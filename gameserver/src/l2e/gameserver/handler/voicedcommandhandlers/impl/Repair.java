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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Repair implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "repair", "startrepair"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!Config.ALLOW_REPAIR_COMMAND || activeChar == null)
		{
			return false;
		}

		String repairChar = null;

		try
		{
			if (target != null)
			{
				if (target.length() > 1)
				{
					final String[] cmdParams = target.split(" ");
					repairChar = cmdParams[0];
				}
			}
		}
		catch (final Exception e)
		{
			repairChar = null;
		}

		if (command.startsWith("repair"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
			html.setFile(activeChar, activeChar.getLang(), "data/html/mods/repair/repair.htm");
			html.replace("%acc_chars%", getCharList(activeChar));
			activeChar.sendPacket(html);
			return true;
		}

		if (command.startsWith("startrepair") && (repairChar != null))
		{
			if (!activeChar.checkFloodProtection("REPAIR", "repair_delay"))
			{
				return false;
			}
			
			if (checkAcc(activeChar, repairChar))
			{
				if (checkChar(activeChar, repairChar))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
					html.setFile(activeChar, activeChar.getLang(), "data/html/mods/repair/repair-self.htm");
					activeChar.sendPacket(html);
					return false;
				}
				repairBadCharacter(repairChar);
				final NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
				html.setFile(activeChar, activeChar.getLang(), "data/html/mods/repair/repair-done.htm");
				activeChar.sendPacket(html);
				return true;
			}
			final NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
			html.setFile(activeChar, activeChar.getLang(), "data/html/mods/repair/repair-error.htm");
			activeChar.sendPacket(html);
			return false;
		}
		return false;
	}

	private String getCharList(Player activeChar)
	{
		String result = "";
		final String repCharAcc = activeChar.getAccountName();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name FROM characters WHERE account_name=?");
			statement.setString(1, repCharAcc);
			rset = statement.executeQuery();
			while (rset.next())
			{
				if (activeChar.getName(null).compareTo(rset.getString(1)) != 0)
				{
					result += rset.getString(1) + ";";
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			return result;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}

	private boolean checkAcc(Player activeChar, String repairChar)
	{
		boolean result = false;
		String repCharAcc = "";

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=?");
			statement.setString(1, repairChar);
			rset = statement.executeQuery();
			if (rset.next())
			{
				repCharAcc = rset.getString(1);
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			return result;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		if (activeChar.getAccountName().compareTo(repCharAcc) == 0)
		{
			result = true;
		}
		return result;
	}

	private boolean checkChar(Player activeChar, String repairChar)
	{
		boolean result = false;
		if (activeChar.getName(null).compareTo(repairChar) == 0)
		{
			result = true;
		}
		return result;
	}

	private void repairBadCharacter(String charName)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT charId FROM characters WHERE char_name=?");
			statement.setString(1, charName);
			rset = statement.executeQuery();

			int objId = 0;
			if (rset.next())
			{
				objId = rset.getInt(1);
			}
			statement.close();

			if (objId == 0)
			{
				return;
			}

			statement = con.prepareStatement("UPDATE characters SET x=17867, y=170259, z=-3503 WHERE charId=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("UPDATE items SET loc=\"WAREHOUSE\" WHERE owner_id=? AND loc=\"PAPERDOLL\"");
			statement.setInt(1, objId);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("GameServer: could not repair character:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}