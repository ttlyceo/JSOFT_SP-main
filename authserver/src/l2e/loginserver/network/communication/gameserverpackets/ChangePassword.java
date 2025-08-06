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
package l2e.loginserver.network.communication.gameserverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2e.commons.dbutils.DbUtils;
import l2e.loginserver.Config;
import l2e.loginserver.database.DatabaseFactory;
import l2e.loginserver.network.communication.ReceivablePacket;
import l2e.loginserver.network.communication.loginserverpackets.ChangePasswordResponse;

public class ChangePassword extends ReceivablePacket
{
	public String _accname;
	public String _oldPass;
	public String _newPass;
	public String _hwid;
  
	@Override
	protected void readImpl()
	{
		_accname = readS();
		_oldPass = readS();
		_newPass = readS();
		_hwid = readS();
	}
	
	@Override
	protected void runImpl()
	{
		String dbPassword = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			try
			{
				statement = con.prepareStatement("SELECT * FROM accounts WHERE login = ?");
				statement.setString(1, _accname);
				rs = statement.executeQuery();
				if (rs.next())
				{
					dbPassword = rs.getString("password");
				}
			}
			catch (final Exception e)
			{
				_log.warn("Can't recive old password for account " + _accname + ", exciption :" + e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, rs);
			}
			
			try
			{
				if (((!Config.DEFAULT_CRYPT.compare(_oldPass, dbPassword)) && (Config.ALLOW_ENCODE_PASSWORD)) || ((!Config.DEFAULT_CRYPT.compare(_oldPass, dbPassword)) && (!dbPassword.equals(_oldPass)) && (!Config.ALLOW_ENCODE_PASSWORD)))
				{
					sendPacket(new ChangePasswordResponse(_accname, false));
				}
				else
				{
					statement = con.prepareStatement("UPDATE accounts SET password = ? WHERE login = ?");
					statement.setString(1, Config.ALLOW_ENCODE_PASSWORD ? Config.DEFAULT_CRYPT.encrypt(_newPass) : _newPass);
					statement.setString(2, _accname);
					final int result = statement.executeUpdate();
					sendPacket(new ChangePasswordResponse(_accname, result != 0));
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
			finally
			{}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}
}
