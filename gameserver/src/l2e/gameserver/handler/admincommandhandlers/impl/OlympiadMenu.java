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

import java.text.SimpleDateFormat;
import java.util.Date;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SocialAction;

public class OlympiadMenu implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_olyinfo", "admin_olysave", "admin_olystart", "admin_olyend", "admin_sethero"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_olyinfo"))
		{
			showMenu(activeChar);
		}
		else if (command.startsWith("admin_olysave"))
		{
			Olympiad.getInstance().saveOlympiadStatus();
			ThreadPoolManager.getInstance().schedule(new RefreshMenu(activeChar), 100);
			activeChar.sendMessage("olympiad system saved.");
		}
		else if (command.startsWith("admin_olyend"))
		{
			try
			{
				Olympiad.getInstance().manualSelectHeroes();
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Problem while ending olympiad...");
			}
			ThreadPoolManager.getInstance().schedule(new RefreshMenu(activeChar), 100);
			activeChar.sendMessage("Heroes formed");
		}
		else if (command.startsWith("admin_olystart"))
		{
			try
			{
				Olympiad.getInstance().manualStartNewOlympiad();
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Problem while starting olympiad...");
			}
			ThreadPoolManager.getInstance().schedule(new RefreshMenu(activeChar), 100);
		}
		else if (command.startsWith("admin_sethero"))
		{
			final GameObject target = activeChar.getTarget();
			if (target instanceof Player)
			{
				final Player targetPlayer = (Player) target;
				final boolean isHero = targetPlayer.isHero();
				if (isHero)
				{
					targetPlayer.setHero(false, true);
					targetPlayer.sendMessage("You are not hero now!");
					if (targetPlayer.getClan() != null)
					{
						targetPlayer.setPledgeClass(ClanMember.calculatePledgeClass(targetPlayer));
					}
					else
					{
						targetPlayer.setPledgeClass(targetPlayer.isNoble() ? 5 : 1);
					}
				}
				else
				{
					targetPlayer.setHero(true, true);
					if (targetPlayer.getClan() != null)
					{
						targetPlayer.setPledgeClass(ClanMember.calculatePledgeClass(targetPlayer));
					}
					else
					{
						targetPlayer.setPledgeClass(8);
					}
					targetPlayer.broadcastPacket(new SocialAction(targetPlayer.getObjectId(), 16));
					targetPlayer.sendMessage("You are hero now!");
				}
				targetPlayer.broadcastUserInfo(true);
			}
			showMenu(activeChar);
		}
		return true;
	}
	
	private void showMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/olympiad.htm");
		if (Olympiad.getInstance().isOlympiadEnd())
		{
			html.replace("%endDate%", "<font color=\"b02e31\">Olympiad End!</font>");
			html.replace("%validDate%", "<font color=\"LEVEL\">" + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(Olympiad.getInstance().getValidationEndDate())) + "</font>");
		}
		else
		{
			html.replace("%endDate%", "<font color=\"LEVEL\">" + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(Olympiad.getInstance().getOlympiadEndDate())) + "</font>");
			html.replace("%validDate%", "<font color=\"b02e31\">Olympiad in Progress!</font>");
		}
		activeChar.sendPacket(html);
	}
	
	protected class RefreshMenu implements Runnable
	{
		Player _player;
		
		private RefreshMenu(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			showMenu(_player);
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}