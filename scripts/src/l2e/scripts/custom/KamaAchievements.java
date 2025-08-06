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
package l2e.scripts.custom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


import l2e.commons.time.cron.SchedulingPattern;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class KamaAchievements extends Quest
{
	public KamaAchievements(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(32484);
		addTalkId(32484);

		long resetDelay;
		final long lastUpdate = ServerVariables.getLong("KamalokaResults", 1000);
		if (lastUpdate > System.currentTimeMillis())
		{
			resetDelay = lastUpdate - System.currentTimeMillis();
		}
		else
		{
			resetDelay = lastUpdate;
		}
		startQuestTimer("cleanKamalokaResults", resetDelay, null, null, true);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("cleanKamalokaResults"))
		{
			try (
			    Connection con = DatabaseFactory.getInstance().getConnection())
			{
				final PreparedStatement statement = con.prepareStatement("DELETE FROM kamaloka_results");
				statement.execute();
			}
			catch (final Exception e)
			{
				_log.warn("KamaAchievments: Could not empty kamaloka_results table: " + e);
			}
			final long newTime = new SchedulingPattern("30 6 * * *").next(System.currentTimeMillis());
			ServerVariables.set("KamalokaResults", newTime);
			startQuestTimer("cleanKamalokaResults", (newTime - System.currentTimeMillis()), null, null, true);
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String SCRIPT_PATH = "data/html/scripts/custom/KamaAchievements/";

		if (npc.getId() == 32484)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			if (npc.isInsideRadius(18228, 146030, -3088, 500, true, false))
			{
				html.setFile(player, player.getLang(), SCRIPT_PATH + "dion-list.htm");
				html.replace("%REPLACE%", getRimKamalokaPlayerList(2030, 2535, 3040, 1));
			}
			else if (npc.isInsideRadius(-13948, 123819, -3112, 500, true, false))
			{
				html.setFile(player, player.getLang(), SCRIPT_PATH + "gludio-list.htm");
				html.replace("%REPLACE%", getRimKamalokaPlayerList(2030, 2535, 1, 1));
			}
			else if (npc.isInsideRadius(108384, 221614, -3592, 500, true, false))
			{
				html.setFile(player, player.getLang(), SCRIPT_PATH + "heine-list.htm");
				html.replace("%REPLACE%", getRimKamalokaPlayerList(3040, 3545, 4050, 1));
			}
			else if (npc.isInsideRadius(80960, 56455, -1552, 500, true, false))
			{
				html.setFile(player, player.getLang(), SCRIPT_PATH + "oren-list.htm");
				html.replace("%REPLACE%", getRimKamalokaPlayerList(3545, 4050, 4555, 5060));
			}
			else if (npc.isInsideRadius(42674, -47909, -797, 500, true, false))
			{
				html.setFile(player, player.getLang(), SCRIPT_PATH + "rune-list.htm");
				html.replace("%REPLACE%", getRimKamalokaPlayerList(5565, 6070, 6575, 7080));
			}
			else if (npc.isInsideRadius(85894, -142108, -1336, 500, true, false))
			{
				html.setFile(player, player.getLang(), SCRIPT_PATH + "schuttgart-list.htm");
				html.replace("%REPLACE%", getRimKamalokaPlayerList(4555, 5060, 5565, 6070));
			}
			else
			{
				return null;
			}
			player.sendPacket(html);
		}
		return null;
	}

	private String getRimKamalokaPlayerList(int a, int b, int c, int d)
	{
		String list = "";
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("SELECT char_name FROM kamaloka_results WHERE Level IN (?, ?, ?, ?) ORDER BY Grade DESC, Count DESC");
			statement.setInt(1, a);
			statement.setInt(2, b);
			statement.setInt(3, c);
			statement.setInt(4, d);
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				list = list + "---" + rset.getString("char_name") + "---<br>";
			}
		}
		catch (final Exception e)
		{
			_log.warn("KamaAchievments: Could not empty kamaloka_results table: " + e);
		}
		return list;
	}

	public static void main(String[] args)
	{
		new KamaAchievements(-1, "KamaAchievements", "custom");
	}
}