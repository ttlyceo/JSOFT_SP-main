/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 *
 */
package l2e.gameserver.data.holder;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2e.commons.checksum.util.Base64;
import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.gameserverpackets.ChangeAccessLevel;
import l2e.gameserver.network.serverpackets.Ex2ndPasswordAck;
import l2e.gameserver.network.serverpackets.Ex2ndPasswordCheck;
import l2e.gameserver.network.serverpackets.Ex2ndPasswordVerify;

public class SecPasswordHolder extends LoggerObject
{
	private final GameClient _activeClient;

	private String _password;
	private int _wrongAttempts;
	private boolean _authed;

	private static final String VAR_PWD = "secauth_pwd";
	private static final String VAR_WTE = "secauth_wte";

	private static final String SELECT_PASSWORD = "SELECT var, value FROM character_secondary_password WHERE account_name=? AND var LIKE 'secauth_%'";
	private static final String INSERT_PASSWORD = "INSERT INTO character_secondary_password VALUES (?, ?, ?)";
	private static final String UPDATE_PASSWORD = "UPDATE character_secondary_password SET value=? WHERE account_name=? AND var=?";

	private static final String INSERT_ATTEMPT = "INSERT INTO character_secondary_password VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value=?";

	public SecPasswordHolder(GameClient activeClient)
	{
		_activeClient = activeClient;
		_password = null;
		_wrongAttempts = 0;
		_authed = false;
		loadPassword();
	}

	private void loadPassword()
	{
		String var, value = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_PASSWORD);
			statement.setString(1, _activeClient.getLogin());
			rset = statement.executeQuery();
			while (rset.next())
			{
				var = rset.getString("var");
				value = rset.getString("value");
				
				if (var.equals(VAR_PWD))
				{
					_password = value;
				}
				else if (var.equals(VAR_WTE))
				{
					_wrongAttempts = Integer.parseInt(value);
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error while reading password.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public boolean savePassword(String password)
	{
		if (passwordExist())
		{
			warn("" + _activeClient.getLogin() + " forced savePassword");
			_activeClient.closeNow(false);
			return false;
		}

		if (!validatePassword(password))
		{
			_activeClient.sendPacket(new Ex2ndPasswordAck(Ex2ndPasswordAck.WRONG_PATTERN));
			return false;
		}

		password = cryptPassword(password);

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_PASSWORD);
			statement.setString(1, _activeClient.getLogin());
			statement.setString(2, VAR_PWD);
			statement.setString(3, password);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Error while writing password.", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		_password = password;
		return true;
	}

	public boolean insertWrongAttempt(int attempts)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(INSERT_ATTEMPT);
			statement.setString(1, _activeClient.getLogin());
			statement.setString(2, VAR_WTE);
			statement.setString(3, Integer.toString(attempts));
			statement.setString(4, Integer.toString(attempts));
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Error while writing wrong attempts.", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}

	public boolean changePassword(String oldPassword, String newPassword)
	{
		if (!passwordExist())
		{
			warn("" + _activeClient.getLogin() + " forced changePassword");
			_activeClient.closeNow(false);
			return false;
		}

		if (!checkPassword(oldPassword, true))
		{
			return false;
		}

		if (!validatePassword(newPassword))
		{
			_activeClient.sendPacket(new Ex2ndPasswordAck(Ex2ndPasswordAck.WRONG_PATTERN));
			return false;
		}

		newPassword = cryptPassword(newPassword);

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_PASSWORD);
			statement.setString(1, newPassword);
			statement.setString(2, _activeClient.getLogin());
			statement.setString(3, VAR_PWD);
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Error while reading password.", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		_password = newPassword;
		_authed = false;
		return true;
	}

	public boolean checkPassword(String password, boolean skipAuth)
	{
		password = cryptPassword(password);

		if (!password.equals(_password))
		{
			_wrongAttempts++;
			if (_wrongAttempts < Config.SECOND_AUTH_MAX_ATTEMPTS)
			{
				_activeClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_WRONG, _wrongAttempts));
				insertWrongAttempt(_wrongAttempts);
			}
			else
			{
				final var banExpire = (int) ((System.currentTimeMillis() / 1000L) + Config.SECOND_AUTH_BAN_TIME * 60);
				final var accessLvl = Config.SECOND_AUTH_BAN_TIME > 0 ? 0 : -1;
				AuthServerCommunication.getInstance().sendPacket(new ChangeAccessLevel(_activeClient.getLogin(), accessLvl, banExpire));
				warn(_activeClient.getLogin() + " - (" + _activeClient.getIPAddress() + ") has inputted the wrong password " + _wrongAttempts + " times in row.");
				insertWrongAttempt(0);
				_activeClient.close(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_BAN, Config.SECOND_AUTH_MAX_ATTEMPTS));
			}
			return false;
		}
		if (!skipAuth)
		{
			_authed = true;
			_activeClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_OK, _wrongAttempts));
		}
		insertWrongAttempt(0);
		return true;
	}

	public boolean passwordExist()
	{
		return _password == null ? false : true;
	}

	public void openDialog()
	{
		if (passwordExist())
		{
			_activeClient.sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_PROMPT));
		}
		else
		{
			_activeClient.sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_NEW));
		}
	}

	public boolean isAuthed()
	{
		return _authed;
	}

	private String cryptPassword(String password)
	{
		try
		{
			final var md = MessageDigest.getInstance("SHA");
			final byte[] raw = password.getBytes("UTF-8");
			final byte[] hash = md.digest(raw);
			return Base64.encodeBytes(hash);
		}
		catch (final NoSuchAlgorithmException e)
		{
			error("Unsupported Algorythm");
		}
		catch (final UnsupportedEncodingException e)
		{
			error("Unsupported Encoding");
		}
		return null;
	}

	private boolean validatePassword(String password)
	{
		if (!Util.isDigit(password))
		{
			return false;
		}

		if ((password.length() < 6) || (password.length() > 8))
		{
			return false;
		}

		if (Config.SECOND_AUTH_STRONG_PASS)
		{
			for (int i = 0; i < (password.length() - 1); i++)
			{
				final var curCh = password.charAt(i);
				final var nxtCh = password.charAt(i + 1);

				if ((curCh + 1) == nxtCh)
				{
					return false;
				}
				else if ((curCh - 1) == nxtCh)
				{
					return false;
				}
				else if (curCh == nxtCh)
				{
					return false;
				}
			}
			
			for (int i = 0; i < (password.length() - 2); i++)
			{
				final var toChk = password.substring(i + 1);
				final var chkEr = new StringBuffer(password.substring(i, i + 2));

				if (toChk.contains(chkEr))
				{
					return false;
				}
				else if (toChk.contains(chkEr.reverse()))
				{
					return false;
				}
			}
		}
		_wrongAttempts = 0;
		return true;
	}
}