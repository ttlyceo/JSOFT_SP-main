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
package l2e.gameserver.handler.admincommandhandlers.impl;

import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ClassListParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class ExpSp implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_add_exp_sp_to_character", "admin_add_exp_sp", "admin_remove_exp_sp"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_add_exp_sp"))
		{
			try
			{
				final String val = command.substring(16);
				if (!adminAddExpSp(activeChar, val))
				{
					activeChar.sendMessage("Usage: //add_exp_sp exp sp");
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //add_exp_sp exp sp");
			}
		}
		else if (command.startsWith("admin_remove_exp_sp"))
		{
			try
			{
				final String val = command.substring(19);
				if (!adminRemoveExpSP(activeChar, val))
				{
					activeChar.sendMessage("Usage: //remove_exp_sp exp sp");
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //remove_exp_sp exp sp");
			}
		}
		addExpSp(activeChar);
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void addExpSp(Player activeChar)
	{
		final GameObject target = activeChar.getTarget();
		Player player = null;
		if (target instanceof Player)
		{
			player = (Player) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/expsp.htm");
		adminReply.replace("%name%", player.getName(null));
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%xp%", String.valueOf(player.getExp()));
		adminReply.replace("%sp%", String.valueOf(player.getSp()));
		adminReply.replace("%class%", ClassListParser.getInstance().getClass(player.getClassId()).getClientCode());
		activeChar.sendPacket(adminReply);
	}
	
	private boolean adminAddExpSp(Player activeChar, String ExpSp)
	{
		final GameObject target = activeChar.getTarget();
		Player player = null;
		if (target instanceof Player)
		{
			player = (Player) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return false;
		}
		final StringTokenizer st = new StringTokenizer(ExpSp);
		if (st.countTokens() != 2)
		{
			return false;
		}
		
		final String exp = st.nextToken();
		final String sp = st.nextToken();
		long expval = 0;
		int spval = 0;
		try
		{
			expval = Long.parseLong(exp);
			spval = Integer.parseInt(sp);
		}
		catch (final Exception e)
		{
			return false;
		}
		if ((expval != 0) || (spval != 0))
		{
			player.sendMessage("Admin is adding you " + expval + " xp and " + spval + " sp.");
			player.addExpAndSp(expval, spval);
			activeChar.sendMessage("Added " + expval + " xp and " + spval + " sp to " + player.getName(null) + ".");
			if (Config.DEBUG)
			{
				_log.info("GM: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ") added " + expval + " xp and " + spval + " sp to " + player.getObjectId() + ".");
			}
		}
		return true;
	}
	
	private boolean adminRemoveExpSP(Player activeChar, String ExpSp)
	{
		final GameObject target = activeChar.getTarget();
		Player player = null;
		if (target instanceof Player)
		{
			player = (Player) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return false;
		}
		final StringTokenizer st = new StringTokenizer(ExpSp);
		if (st.countTokens() != 2)
		{
			return false;
		}
		
		final String exp = st.nextToken();
		final String sp = st.nextToken();
		long expval = 0;
		int spval = 0;
		try
		{
			expval = Long.parseLong(exp);
			spval = Integer.parseInt(sp);
		}
		catch (final Exception e)
		{
			return false;
		}
		if ((expval != 0) || (spval != 0))
		{
			player.sendMessage("Admin is removing you " + expval + " xp and " + spval + " sp.");
			player.removeExpAndSp(expval, spval);
			activeChar.sendMessage("Removed " + expval + " xp and " + spval + " sp from " + player.getName(null) + ".");
			if (Config.DEBUG)
			{
				_log.info("GM: " + activeChar.getName(null) + "(" + activeChar.getObjectId() + ") removed " + expval + " xp and " + spval + " sp from " + player.getObjectId() + ".");
			}
		}
		return true;
	}
}