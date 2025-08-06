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

import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Announcements;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.taskmanager.AutoAnnounceTaskManager;
import l2e.gameserver.taskmanager.AutoAnnounceTaskManager.AutoAnnouncement;

public class Announcement implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_list_announcements", "admin_list_critannouncements", "admin_reload_announcements", "admin_announce_announcements", "admin_add_announcement", "admin_del_announcement", "admin_add_critannouncement", "admin_del_critannouncement", "admin_announce", "admin_critannounce", "admin_announce_menu", "admin_critannounce_menu", "admin_list_autoann", "admin_reload_autoann", "admin_add_autoann", "admin_del_autoann"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		
		if (command.equals("admin_list_announcements"))
		{
			Announcements.getInstance().listAnnouncements(activeChar);
		}
		else if (command.equals("admin_list_critannouncements"))
		{
			Announcements.getInstance().listCritAnnouncements(activeChar);
		}
		else if (command.equals("admin_reload_announcements"))
		{
			Announcements.getInstance().loadAnnouncements();
			Announcements.getInstance().listAnnouncements(activeChar);
		}
		else if (command.startsWith("admin_announce_menu"))
		{
			if (activeChar.getAccessLevel().allowShowAnnouncerName() && command.length() > 20)
			{
				command += " (" + activeChar.getName(null) + ")";
			}
			Announcements.getInstance().handleAnnounce(command, 20, false);
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_critannounce_menu"))
		{
			try
			{
				command = command.substring(24);

				if (activeChar.getAccessLevel().allowShowCritAnnouncerName() && command.length() > 0)
				{
					command = activeChar.getName(null) + ": " + command;
				}
				Announcements.getInstance().handleAnnounce(command, 0, true);
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.equals("admin_announce_announcements"))
		{
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				Announcements.getInstance().showAnnouncements(player);
			}
			Announcements.getInstance().listAnnouncements(activeChar);
		}
		else if (command.startsWith("admin_add_announcement"))
		{
			if (!command.equals("admin_add_announcement"))
			{
				try
				{
					final String val = command.substring(23);
					Announcements.getInstance().addAnnouncement(val);
					Announcements.getInstance().listAnnouncements(activeChar);
				}
				catch (final StringIndexOutOfBoundsException e)
				{}
			}
		}
		else if (command.startsWith("admin_add_critannouncement"))
		{
			if (!command.equals("admin_add_critannouncement"))
			{
				try
				{
					final String val = command.substring(27);
					Announcements.getInstance().addCritAnnouncement(val);
					Announcements.getInstance().listCritAnnouncements(activeChar);
				}
				catch (final StringIndexOutOfBoundsException e)
				{}
			}
		}
		else if (command.startsWith("admin_del_announcement"))
		{
			try
			{
				final int val = Integer.parseInt(command.substring(23));
				Announcements.getInstance().delAnnouncement(val);
				Announcements.getInstance().listAnnouncements(activeChar);
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}
		else if (command.startsWith("admin_del_critannouncement"))
		{
			try
			{
				final int val = Integer.parseInt(command.substring(27));
				Announcements.getInstance().delCritAnnouncement(val);
				Announcements.getInstance().listCritAnnouncements(activeChar);
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}
		else if (command.startsWith("admin_announce"))
		{
			if (activeChar.getAccessLevel().allowShowAnnouncerName() && command.length() > 15)
			{
				command += " (" + activeChar.getName(null) + ")";
			}
			
			Announcements.getInstance().handleAnnounce(command, 15, false);
		}
		else if (command.startsWith("admin_critannounce"))
		{
			try
			{
				command = command.substring(19);

				if (activeChar.getAccessLevel().allowShowCritAnnouncerName() && command.length() > 0)
				{
					command = activeChar.getName(null) + ": " + command;
				}
				Announcements.getInstance().handleAnnounce(command, 0, true);
			}
			catch (final StringIndexOutOfBoundsException e)
			{}
		}
		else if (command.startsWith("admin_list_autoann"))
		{
			listAutoAnnouncements(activeChar);
		}
		else if (command.startsWith("admin_reload_autoann"))
		{
			AutoAnnounceTaskManager.getInstance().restore();
			activeChar.sendMessage("AutoAnnouncement Reloaded.");
			listAutoAnnouncements(activeChar);
		}
		else if (command.startsWith("admin_add_autoann"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			
			String initial = null;
			try
			{
				initial = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			String delay = null;
			try
			{
				delay = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			String repeat = null;
			try
			{
				repeat = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			String isCritical = null;
			try
			{
				isCritical = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			if (initial != null && delay != null && repeat != null && isCritical != null)
			{
				if ((!repeat.equals("-1") && !Util.isDigit(repeat)) || !Util.isDigit(initial) || !Util.isDigit(delay))
				{
					activeChar.sendMessage("Not a valid initial delay or repeat value!");
					return false;
				}
				
				if (!st.hasMoreTokens())
				{
					activeChar.sendMessage("Not enough parameters for adding autoannounce!");
					return false;
				}
				
				final StringBuilder memo = new StringBuilder();
				while (st.hasMoreTokens())
				{
					memo.append(st.nextToken());
					memo.append(" ");
				}
				
				AutoAnnounceTaskManager.getInstance().addAutoAnnounce(Long.parseLong(initial) * 1000, Long.parseLong(delay) * 1000, Integer.parseInt(repeat), memo.toString().trim(), Boolean.valueOf(isCritical));
				listAutoAnnouncements(activeChar);
			}
			else
			{
				activeChar.sendMessage("Not enough parameters for adding autoannounce!");
				return false;
			}
		}

		else if (command.startsWith("admin_del_autoann"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();

			if (!st.hasMoreTokens())
			{
				activeChar.sendMessage("Not enough parameters for deleting autoannounce!");
				return false;
			}
			final String token = st.nextToken();
			if (!Util.isDigit(token))
			{
				activeChar.sendMessage("Not a valid auto announce Id value!");
				return false;
			}
			AutoAnnounceTaskManager.getInstance().deleteAutoAnnounce(Integer.parseInt(token));
			listAutoAnnouncements(activeChar);
		}
		return true;
	}

	private void listAutoAnnouncements(Player activeChar)
	{
		final String content = HtmCache.getInstance().getHtmForce(activeChar, activeChar.getLang(), "data/html/admin/autoannounce.htm");
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(activeChar, content);

		final StringBuilder replyMSG = StringUtil.startAppend(500, "<br>");
		final List<AutoAnnouncement> autoannouncements = AutoAnnounceTaskManager.getInstance().getAutoAnnouncements();
		for (int i = 0; i < autoannouncements.size(); i++)
		{
			final AutoAnnouncement autoann = autoannouncements.get(i);
			final StringBuilder memo2 = new StringBuilder();
			for (final String memo0 : autoann.getMemo())
			{
				memo2.append(memo0);
				memo2.append("/n");
			}
			replyMSG.append("<table width=260><tr><td width=220><font color=\"" + (autoann.isCritical() ? "00FCFC" : "7FFCFC") + "\">");
			replyMSG.append(memo2.toString().trim());
			replyMSG.append("</font></td><td width=40><button value=\"Delete\" action=\"bypass -h admin_del_autoann ");
			replyMSG.append(i);
			replyMSG.append("\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		}
		adminReply.replace("%announces%", replyMSG.toString());

		activeChar.sendPacket(adminReply);
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
