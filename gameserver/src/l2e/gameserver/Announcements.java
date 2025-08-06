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
package l2e.gameserver;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.StringUtil;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Announcements extends LoggerObject
{
	private final List<String> _announcements = new ArrayList<>();
	private final List<String> _critAnnouncements = new ArrayList<>();

	protected Announcements()
	{
		loadAnnouncements();
	}

	public static Announcements getInstance()
	{
		return SingletonHolder._instance;
	}

	public void loadAnnouncements()
	{
		_announcements.clear();
		_critAnnouncements.clear();
		readFromDisk("data/announcements.txt", _announcements);
		readFromDisk("data/critannouncements.txt", _critAnnouncements);

		if (Config.DEBUG)
		{
			info("Loaded " + (_announcements.size() + _critAnnouncements.size()) + " announcements.");
		}
	}
	public void showAnnouncements(Player activeChar)
	{
		for (final var announce : _announcements)
		{
			final CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, activeChar.getName(null), announce);
			activeChar.sendPacket(cs);
		}

		for (final var critAnnounce : _critAnnouncements)
		{
			final CreatureSay cs = new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, activeChar.getName(null), critAnnounce);
			activeChar.sendPacket(cs);
		}
	}

	public void listAnnouncements(Player activeChar)
	{
		final var content = HtmCache.getInstance().getHtmForce(activeChar, activeChar.getLang(), "data/html/admin/announce.htm");
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(activeChar, content);
		final StringBuilder replyMSG = StringUtil.startAppend(500, "<br>");
		for (int i = 0; i < _announcements.size(); i++)
		{
			StringUtil.append(replyMSG, "<table width=260><tr><td width=220>", _announcements.get(i), "</td><td width=40>" + "<button value=\"Delete\" action=\"bypass -h admin_del_announcement ", String.valueOf(i), "\" width=60 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		}
		adminReply.replace("%announces%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public void listCritAnnouncements(Player activeChar)
	{
		final var content = HtmCache.getInstance().getHtmForce(activeChar, activeChar.getLang(), "data/html/admin/critannounce.htm");
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(activeChar, content);
		final StringBuilder replyMSG = StringUtil.startAppend(500, "<br>");
		for (int i = 0; i < _critAnnouncements.size(); i++)
		{
			StringUtil.append(replyMSG, "<table width=260><tr><td width=220>", _critAnnouncements.get(i), "</td><td width=40>" + "<button value=\"Delete\" action=\"bypass -h admin_del_critannouncement ", String.valueOf(i), "\" width=60 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		}
		adminReply.replace("%critannounces%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public void addAnnouncement(String text)
	{
		_announcements.add(text);
		saveToDisk(false);
	}

	public void delAnnouncement(int line)
	{
		_announcements.remove(line);
		saveToDisk(false);
	}

	public void addCritAnnouncement(String text)
	{
		_critAnnouncements.add(text);
		saveToDisk(true);
	}

	public void delCritAnnouncement(int line)
	{
		_critAnnouncements.remove(line);
		saveToDisk(true);
	}

	private void readFromDisk(String path, List<String> list)
	{
		final File file = new File(Config.DATAPACK_ROOT, path);
		if (file.exists())
		{
			
			try (
			    LineNumberReader lnr = new LineNumberReader(new FileReader(file)))
			{
				String line = null;
				while ((line = lnr.readLine()) != null)
				{
					final StringTokenizer st = new StringTokenizer(line, Config.EOL);
					if (st.hasMoreTokens())
					{
						list.add(st.nextToken());
					}
				}
			}
			catch (final IOException e1)
			{
				warn("Error reading announcements: ", e1);
			}
		}
		else
		{
			warn(file.getAbsolutePath() + " doesn't exist");
		}
	}

	private void saveToDisk(boolean isCritical)
	{
		String path;
		List<String> list;

		if (isCritical)
		{
			path = "data/critannouncements.txt";
			list = _critAnnouncements;
		}
		else
		{
			path = "data/announcements.txt";
			list = _announcements;
		}

		final File file = new File(path);
		try (
		    FileWriter save = new FileWriter(file))
		{
			for (final var announce : list)
			{
				save.write(announce);
				save.write(Config.EOL);
			}
		}
		catch (final IOException e)
		{
			warn("Saving to the announcements file has failed: ", e);
		}
	}

	public void announceToAll(ServerMessage msg)
	{
		for (final var onlinePlayer : GameObjectsStorage.getPlayers())
		{
			if (onlinePlayer.isOnline())
			{
				onlinePlayer.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, "", msg.toString(onlinePlayer.getLang())));
			}
		}
	}

	public void announceToAll(String text)
	{
		announceToAll(text, false);
	}

	public void gameAnnounceToAll(String text)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", "Announcements: " + text);
		for (final var player : GameObjectsStorage.getPlayers())
		{
			if (player != null)
			{
				if (player.isOnline())
				{
					player.sendPacket(cs);
				}
			}
		}
		cs = null;
	}

	public void announceToAll(String text, boolean isCritical)
	{
		final var cs = new CreatureSay(0, isCritical ? Say2.CRITICAL_ANNOUNCE : Say2.ANNOUNCEMENT, "", text);
		GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(cs));
	}

	public void announceToAll(SystemMessage sm)
	{
		GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(sm));
	}

	public void announceToInstance(SystemMessage sm, int instanceId)
	{
		GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline() && p.getReflectionId() == instanceId).forEach(p -> p.sendPacket(sm));
	}

	public void handleAnnounce(String command, int lengthToTrim, boolean isCritical)
	{
		try
		{
			final var text = command.substring(lengthToTrim);
			SingletonHolder._instance.announceToAll(text, isCritical);
		}

		catch (final StringIndexOutOfBoundsException e)
		{}
	}

	private static class SingletonHolder
	{
		protected static final Announcements _instance = new Announcements();
	}
}