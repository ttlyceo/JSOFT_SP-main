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
package l2e.gameserver.handler.communityhandlers.impl;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerStorage;

/**
 * Created by LordWinter 03.03.2019
 */
public class CommunityRanking extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityRanking()
	{
		selectRankingPK();
		selectRankingPVP();
		selectRankingPcBang();
		selectRankingHero();
		selectRankingClan();
		selectRankingAdena();
		selectRankingCastle();
		selectRankingOnline();
		selectRankingRebirth();
		selectRankingRaidPoints();
		selectRankingClanRaidPoints();
		
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}
	
	private static class RankingManager
	{
		private final String[] RankingPvPName = new String[10];
		private final String[] RankingPvPClan = new String[10];
		private final int[] RankingPvPClass = new int[10];
		private final int[] RankingPvPOn = new int[10];
		private final int[] RankingPvP = new int[10];

		private final String[] RankingPkName = new String[10];
		private final String[] RankingPkClan = new String[10];
		private final int[] RankingPkClass = new int[10];
		private final int[] RankingPkOn = new int[10];
		private final int[] RankingPk = new int[10];

		private final String[] RankingPcbangName = new String[10];
		private final String[] RankingPcbangClan = new String[10];
		private final int[] RankingPcbangClass = new int[10];
		private final int[] RankingPcbangOn = new int[10];
		private final int[] RankingPcbang = new int[10];

		private final String[] RankingHeroName = new String[10];
		private final String[] RankingHeroClan = new String[10];
		private final int[] RankingHeroClass = new int[10];
		private final int[] RankingHeroOn = new int[10];
		private final int[] RankingHero = new int[10];

		private final String[] RankingClanName = new String[10];
		private final String[] RankingClanAlly = new String[10];
		private final int[] RankingClanReputation = new int[10];
		private final int[] RankingClanLvl = new int[10];
		private final String[] RankingClanLeader = new String[10];

		private final String[] RankingAdenaName = new String[10];
		private final String[] RankingAdenaClan = new String[10];
		private final int[] RankingAdenaClass = new int[10];
		private final int[] RankingAdenaOn = new int[10];
		private final long[] RankingAdena = new long[10];
		
		private final String[] RankingCastleName = new String[10];
		private final String[] RankingCastleClan = new String[10];
		private final int[] RankingCastleClanLvl = new int[10];
		private final int[] RankingCastleTax = new int[10];
		private final long[] RankingCastleDate = new long[10];
		
		private final String[] RankingOnlineName = new String[10];
		private final String[] RankingOnlineClan = new String[10];
		private final int[] RankingOnlineClass = new int[10];
		private final long[] RankingOnline = new long[10];
		
		private final String[] RankingRebirthName = new String[10];
		private final String[] RankingRebirthClan = new String[10];
		private final int[] RankingRebirthClass = new int[10];
		private final long[] RankingRebirthAmount = new long[10];
		
		private final String[] RankingRpName = new String[10];
		private final String[] RankingRpClan = new String[10];
		private final int[] RankingRpClass = new int[10];
		private final int[] RankingRpOn = new int[10];
		private final int[] RankingRp = new int[10];
		
		private final String[] RankingCRpName = new String[10];
		private final int[] RankingCRpLvl = new int[10];
		private final String[] RankingCRpAlly = new String[10];
		private final int[] RankingCRp = new int[10];
	}

	private static RankingManager RankingManagerStats = new RankingManager();
	private long update = 0;
	private final int time_update = Config.INTERVAL_STATS_UPDATE;

	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbsranking", "_bbsloc"
		};
	}
	
	@Override
	public void onBypassCommand(String bypass, Player player)
	{
		if (!checkCondition(player, new StringTokenizer(bypass, "_").nextToken(), false, false))
		{
			return;
		}
		
		if ((update + (time_update * 60 * 1000L)) < Calendar.getInstance().getTimeInMillis())
		{
			selectRankingPK();
			selectRankingPVP();
			selectRankingPcBang();
			selectRankingHero();
			selectRankingClan();
			selectRankingAdena();
			selectRankingCastle();
			selectRankingOnline();
			selectRankingRebirth();
			selectRankingRaidPoints();
			selectRankingClanRaidPoints();
			update = Calendar.getInstance().getTimeInMillis();
			_log.info("Ranking in the commynity board has been updated.");
		}

		if (bypass.equals("_bbsloc"))
		{
			onBypassCommand("_bbsranking:pvp", player);
		}
		else if (bypass.equals("_bbsranking:pk"))
		{
			show(player, 1);
		}
		else if (bypass.equals("_bbsranking:pvp"))
		{
			show(player, 2);
		}
		else if (bypass.equals("_bbsranking:pcbang"))
		{
			show(player, 3);
		}
		else if (bypass.equals("_bbsranking:hero"))
		{
			show(player, 4);
		}
		else if (bypass.equals("_bbsranking:clan"))
		{
			show(player, 5);
		}
		else if (bypass.equals("_bbsranking:adena"))
		{
			show(player, 6);
		}
		else if (bypass.equals("_bbsranking:castle"))
		{
			show(player, 7);
		}
		else if (bypass.equals("_bbsranking:online"))
		{
			show(player, 8);
		}
		else if (bypass.equals("_bbsranking:rebirth"))
		{
			show(player, 9);
		}
		else if (bypass.equals("_bbsranking:raidPoints"))
		{
			show(player, 10);
		}
		else if (bypass.equals("_bbsranking:clanRaidPoints"))
		{
			show(player, 11);
		}
	}

	private void show(Player player, int page)
	{
		int number = 0;
		String html = null;

		switch (page)
		{
			case 1 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/pk.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingPkName[number] != null)
					{
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingPkName[number]);
						html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingPkClan[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN") + "</font>" : RankingManagerStats.RankingPkClan[number]);
						html = html.replace("<?class_" + number + "?>", Util.className(player, RankingManagerStats.RankingPkClass[number]));
						html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingPkOn[number] == 1 ? "<font color=\"66FF33\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") + "</font>" : "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
						html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingPk[number]));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clan_" + number + "?>", "...");
						html = html.replace("<?class_" + number + "?>", "...");
						html = html.replace("<?on_" + number + "?>", "...");
						html = html.replace("<?count_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 2 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/pvp.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingPvPName[number] != null)
					{
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingPvPName[number]);
						html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingPvPClan[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN") + "</font>" : RankingManagerStats.RankingPvPClan[number]);
						html = html.replace("<?class_" + number + "?>", Util.className(player, RankingManagerStats.RankingPvPClass[number]));
						html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingPvPOn[number] == 1 ? "<font color=\"66FF33\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") + "</font>" : "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
						html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingPvP[number]));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clan_" + number + "?>", "...");
						html = html.replace("<?class_" + number + "?>", "...");
						html = html.replace("<?on_" + number + "?>", "...");
						html = html.replace("<?count_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 3 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/pcbang.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingPcbangName[number] != null)
					{
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingPcbangName[number]);
						html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingPcbangClan[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN") + "</font>" : RankingManagerStats.RankingPcbangClan[number]);
						html = html.replace("<?class_" + number + "?>", Util.className(player, RankingManagerStats.RankingPcbangClass[number]));
						html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingPcbangOn[number] == 1 ? "<font color=\"66FF33\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") + "</font>" : "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
						html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingPcbang[number]));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clan_" + number + "?>", "...");
						html = html.replace("<?class_" + number + "?>", "...");
						html = html.replace("<?on_" + number + "?>", "...");
						html = html.replace("<?count_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 4 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/hero.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingHeroName[number] != null)
					{
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingHeroName[number]);
						html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingHeroClan[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN") + "</font>" : RankingManagerStats.RankingHeroClan[number]);
						html = html.replace("<?class_" + number + "?>", Util.className(player, RankingManagerStats.RankingHeroClass[number]));
						html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingHeroOn[number] == 1 ? "<font color=\"66FF33\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") + "</font>" : "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
						html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingHero[number]));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clan_" + number + "?>", "...");
						html = html.replace("<?class_" + number + "?>", "...");
						html = html.replace("<?on_" + number + "?>", "...");
						html = html.replace("<?count_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 5 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/clan.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingClanName[number] != null)
					{
						html = html.replace("<?clanName_" + number + "?>", RankingManagerStats.RankingClanName[number]);
						html = html.replace("<?clanAlly_" + number + "?>", RankingManagerStats.RankingClanAlly[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_ALLY") + "</font>" : RankingManagerStats.RankingClanAlly[number]);
						html = html.replace("<?clanRep_" + number + "?>", Integer.toString(RankingManagerStats.RankingClanReputation[number]));
						html = html.replace("<?clanLvl_" + number + "?>", Integer.toString(RankingManagerStats.RankingClanLvl[number]));
						html = html.replace("<?clanLeader_" + number + "?>", RankingManagerStats.RankingClanLeader[number]);
					}
					else
					{
						html = html.replace("<?clanName_" + number + "?>", "...");
						html = html.replace("<?clanAlly_" + number + "?>", "...");
						html = html.replace("<?clanRep_" + number + "?>", "...");
						html = html.replace("<?clanLvl_" + number + "?>", "...");
						html = html.replace("<?clanLeader_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 6 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/adena.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingAdenaName[number] != null)
					{
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingAdenaName[number]);
						html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingAdenaClan[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN") + "</font>" : RankingManagerStats.RankingAdenaClan[number]);
						html = html.replace("<?class_" + number + "?>", Util.className(player, RankingManagerStats.RankingAdenaClass[number]));
						html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingAdenaOn[number] == 1 ? "<font color=\"66FF33\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") + "</font>" : "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
						html = html.replace("<?count_" + number + "?>", Long.toString(RankingManagerStats.RankingAdena[number]));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clan_" + number + "?>", "...");
						html = html.replace("<?class_" + number + "?>", "...");
						html = html.replace("<?on_" + number + "?>", "...");
						html = html.replace("<?count_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 7 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/castle.htm");
				while (number < 9)
				{
					if (RankingManagerStats.RankingCastleName[number] != null)
					{
						final Date nextDate = new Date(RankingManagerStats.RankingCastleDate[number]);
						final String DATE_FORMAT = "dd-MMM-yyyy HH:mm";
						final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingCastleName[number]);
						html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingCastleClan[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN") + "</font>" : RankingManagerStats.RankingCastleClan[number]);
						html = html.replace("<?level_" + number + "?>", Integer.toString(RankingManagerStats.RankingCastleClanLvl[number]));
						html = html.replace("<?tax_" + number + "?>", "" + RankingManagerStats.RankingCastleTax[number] + " %");
						html = html.replace("<?date_" + number + "?>", String.valueOf(sdf.format(nextDate)));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clan_" + number + "?>", "...");
						html = html.replace("<?level_" + number + "?>", "...");
						html = html.replace("<?tax_" + number + "?>", "...");
						html = html.replace("<?date_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 8 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/online.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingOnlineName[number] != null)
					{
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingOnlineName[number]);
						html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingOnlineClan[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN") + "</font>" : RankingManagerStats.RankingOnlineClan[number]);
						html = html.replace("<?class_" + number + "?>", Util.className(player, RankingManagerStats.RankingOnlineClass[number]));
						html = html.replace("<?count_" + number + "?>", TimeUtils.formatTime(player, (int) RankingManagerStats.RankingOnline[number], false));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clan_" + number + "?>", "...");
						html = html.replace("<?class_" + number + "?>", "...");
						html = html.replace("<?count_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 9 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/rebirth.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingRebirthName[number] != null)
					{
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingRebirthName[number]);
						html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingRebirthClan[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN") + "</font>" : RankingManagerStats.RankingRebirthClan[number]);
						html = html.replace("<?class_" + number + "?>", Util.className(player, RankingManagerStats.RankingRebirthClass[number]));
						html = html.replace("<?count_" + number + "?>", String.valueOf(RankingManagerStats.RankingRebirthAmount[number]));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clan_" + number + "?>", "...");
						html = html.replace("<?class_" + number + "?>", "...");
						html = html.replace("<?count_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 10 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/raidPoints.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingRpName[number] != null)
					{
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingRpName[number]);
						html = html.replace("<?clan_" + number + "?>", RankingManagerStats.RankingRpClan[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN") + "</font>" : RankingManagerStats.RankingRpClan[number]);
						html = html.replace("<?class_" + number + "?>", Util.className(player, RankingManagerStats.RankingRpClass[number]));
						html = html.replace("<?on_" + number + "?>", RankingManagerStats.RankingRpOn[number] == 1 ? "<font color=\"66FF33\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") + "</font>" : "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
						html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingRp[number]));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clan_" + number + "?>", "...");
						html = html.replace("<?class_" + number + "?>", "...");
						html = html.replace("<?on_" + number + "?>", "...");
						html = html.replace("<?count_" + number + "?>", "...");
					}
					number++;
				}
				break;
			case 11 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/clanRaidPoints.htm");
				while (number < 10)
				{
					if (RankingManagerStats.RankingCRpName[number] != null)
					{
						html = html.replace("<?name_" + number + "?>", RankingManagerStats.RankingCRpName[number]);
						html = html.replace("<?clanAlly_" + number + "?>", RankingManagerStats.RankingCRpAlly[number] == null ? "<font color=\"B59A75\">" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_ALLY") + "</font>" : RankingManagerStats.RankingCRpAlly[number]);
						html = html.replace("<?clanLvl_" + number + "?>", Integer.toString(RankingManagerStats.RankingCRpLvl[number]));
						html = html.replace("<?count_" + number + "?>", Integer.toString(RankingManagerStats.RankingCRp[number]));
					}
					else
					{
						html = html.replace("<?name_" + number + "?>", "...");
						html = html.replace("<?clanAlly_" + number + "?>", "...");
						html = html.replace("<?clanLvl_" + number + "?>", "...");
						html = html.replace("<?count_" + number + "?>", "...");
					}
					number++;
				}
				break;
			default :
				_log.warn("Unknown page: " + page + " - " + player.getName(null));
				break;
		}

		html = html.replace("<?update?>", String.valueOf(time_update));
		html = html.replace("<?last_update?>", String.valueOf(time(update)));
		html = html.replace("<?ranking_menu?>", HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/ranking/menu.htm"));
		separateAndSend(html, player);
	}

	private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

	private static String time(long time)
	{
		return TIME_FORMAT.format(new Date(time));
	}

	private void selectRankingPVP()
	{
		int number = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, base_class, clanid, online, pvpkills FROM characters WHERE accesslevel = 0 AND pvpkills > 0 ORDER BY pvpkills DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while (rset.next())
			{
				if (!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingPvPName[number] = rset.getString("char_name");
					final int clan_id = rset.getInt("clanid");
					final Clan clan = clan_id == 0 ? null : ClanHolder.getInstance().getClan(clan_id);
					RankingManagerStats.RankingPvPClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingPvPClass[number] = rset.getInt("base_class");
					RankingManagerStats.RankingPvPOn[number] = rset.getInt("online");
					RankingManagerStats.RankingPvP[number] = rset.getInt("pvpkills");
				}
				else
				{
					RankingManagerStats.RankingPvPName[number] = null;
					RankingManagerStats.RankingPvPClan[number] = null;
					RankingManagerStats.RankingPvPClass[number] = 0;
					RankingManagerStats.RankingPvPOn[number] = 0;
					RankingManagerStats.RankingPvP[number] = 0;
				}
				number++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void selectRankingPK()
	{
		int number = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, base_class, clanid, online, pkkills FROM characters WHERE accesslevel = 0 AND pkkills > 0 ORDER BY pkkills DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while (rset.next())
			{
				if (!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingPkName[number] = rset.getString("char_name");
					final int clan_id = rset.getInt("clanid");
					final Clan clan = clan_id == 0 ? null : ClanHolder.getInstance().getClan(clan_id);
					RankingManagerStats.RankingPkClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingPkClass[number] = rset.getInt("base_class");
					RankingManagerStats.RankingPkOn[number] = rset.getInt("online");
					RankingManagerStats.RankingPk[number] = rset.getInt("pkkills");
				}
				else
				{
					RankingManagerStats.RankingPkName[number] = null;
					RankingManagerStats.RankingPkClan[number] = null;
					RankingManagerStats.RankingPkClass[number] = 0;
					RankingManagerStats.RankingPkOn[number] = 0;
					RankingManagerStats.RankingPk[number] = 0;
				}
				number++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void selectRankingPcBang()
	{
		int number = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, base_class, clanid, online, pccafe_points FROM characters WHERE accesslevel = 0 AND pccafe_points > 0 ORDER BY pccafe_points DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while (rset.next())
			{
				if (!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingPcbangName[number] = rset.getString("char_name");
					final int clan_id = rset.getInt("clanid");
					final Clan clan = clan_id == 0 ? null : ClanHolder.getInstance().getClan(clan_id);
					RankingManagerStats.RankingPcbangClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingPcbangClass[number] = rset.getInt("base_class");
					RankingManagerStats.RankingPcbangOn[number] = rset.getInt("online");
					RankingManagerStats.RankingPcbang[number] = rset.getInt("pccafe_points");
				}
				else
				{
					RankingManagerStats.RankingPcbangName[number] = null;
					RankingManagerStats.RankingPcbangClan[number] = null;
					RankingManagerStats.RankingPcbangClass[number] = 0;
					RankingManagerStats.RankingPcbangOn[number] = 0;
					RankingManagerStats.RankingPcbang[number] = 0;
				}
				number++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void selectRankingHero()
	{
		int number = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT h.count, ch.char_name, ch.base_class, ch.online, ch.clanid FROM heroes h LEFT JOIN characters ch ON ch.charId=h.charId ORDER BY h.count DESC, ch.char_name ASC LIMIT " + 10);
			rset = statement.executeQuery();
			while (rset.next())
			{
				if (!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingHeroName[number] = rset.getString("char_name");
					final int clan_id = rset.getInt("clanid");
					final Clan clan = clan_id == 0 ? null : ClanHolder.getInstance().getClan(clan_id);
					RankingManagerStats.RankingHeroClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingHeroClass[number] = rset.getInt("base_class");
					RankingManagerStats.RankingHeroOn[number] = rset.getInt("online");
					RankingManagerStats.RankingHero[number] = rset.getInt("count");
				}
				else
				{
					RankingManagerStats.RankingHeroName[number] = null;
					RankingManagerStats.RankingHeroClan[number] = null;
					RankingManagerStats.RankingHeroClass[number] = 0;
					RankingManagerStats.RankingHeroOn[number] = 0;
					RankingManagerStats.RankingHero[number] = 0;
				}
				number++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void selectRankingClan()
	{
		int number = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clan_name, clan_level, reputation_score, ally_name, leader_id FROM clan_data WHERE clan_level > 0 ORDER BY clan_level DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while (rset.next())
			{
				if (!rset.getString("clan_name").isEmpty())
				{
					RankingManagerStats.RankingClanName[number] = rset.getString("clan_name");
					final String ally = rset.getString("ally_name");
					RankingManagerStats.RankingClanAlly[number] = ally == null ? null : ally;
					RankingManagerStats.RankingClanReputation[number] = rset.getInt("reputation_score");
					RankingManagerStats.RankingClanLvl[number] = rset.getInt("clan_level");
					RankingManagerStats.RankingClanLeader[number] = CharNameHolder.getInstance().getNameById(rset.getInt("leader_id"));
				}
				else
				{
					RankingManagerStats.RankingClanName[number] = null;
					RankingManagerStats.RankingClanAlly[number] = null;
					RankingManagerStats.RankingClanReputation[number] = 0;
					RankingManagerStats.RankingClanLvl[number] = 0;
					RankingManagerStats.RankingClanLeader[number] = null;
				}
				number++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void selectRankingAdena()
	{
		int number = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, base_class, clanid, online, it.count FROM characters AS c LEFT JOIN items AS it ON (c.charId=it.owner_id) WHERE it.item_id=57 AND accesslevel = 0 ORDER BY it.count DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while (rset.next())
			{
				if (!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingAdenaName[number] = rset.getString("char_name");
					final int clan_id = rset.getInt("clanid");
					final Clan clan = clan_id == 0 ? null : ClanHolder.getInstance().getClan(clan_id);
					RankingManagerStats.RankingAdenaClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingAdenaClass[number] = rset.getInt("base_class");
					RankingManagerStats.RankingAdenaOn[number] = rset.getInt("online");
					RankingManagerStats.RankingAdena[number] = rset.getLong("count");
				}
				else
				{
					RankingManagerStats.RankingAdenaName[number] = null;
					RankingManagerStats.RankingAdenaClan[number] = null;
					RankingManagerStats.RankingAdenaClass[number] = 0;
					RankingManagerStats.RankingAdenaOn[number] = 0;
					RankingManagerStats.RankingAdena[number] = 0;
				}
				number++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private void selectRankingCastle()
	{
		int number = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			for (int i = 1; i <= 9; i++)
			{
				statement = con.prepareStatement("SELECT clan_name, clan_level FROM clan_data WHERE hasCastle=" + i + ";");
				result = statement.executeQuery();
				
				final PreparedStatement statement2 = con.prepareStatement("SELECT name, siegeDate, taxPercent FROM castle WHERE id=" + i + ";");
				final ResultSet result2 = statement2.executeQuery();
				
				while (result.next())
				{
					if (!result.getString("clan_name").isEmpty())
					{
						while (result2.next())
						{
							RankingManagerStats.RankingCastleName[number] = result2.getString("name");
							RankingManagerStats.RankingCastleClan[number] = result.getString("clan_name");
							RankingManagerStats.RankingCastleClanLvl[number] = result.getInt("clan_level");
							RankingManagerStats.RankingCastleTax[number] = result2.getInt("taxPercent");
							RankingManagerStats.RankingCastleDate[number] = result2.getLong("siegeDate");
						}
					}
					else
					{
						RankingManagerStats.RankingCastleName[number] = null;
						RankingManagerStats.RankingCastleClan[number] = null;
						RankingManagerStats.RankingCastleClanLvl[number] = 0;
						RankingManagerStats.RankingCastleTax[number] = 0;
						RankingManagerStats.RankingCastleDate[number] = 0;
					}
					number++;
				}
				statement.close();
				result.close();
				statement2.close();
				result2.close();
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, result);
		}
	}
	
	private void selectRankingOnline()
	{
		int number = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, base_class, clanid, onlinetime FROM characters WHERE accesslevel = 0 ORDER BY onlinetime DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while (rset.next())
			{
				if (!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingOnlineName[number] = rset.getString("char_name");
					final int clan_id = rset.getInt("clanid");
					final Clan clan = clan_id == 0 ? null : ClanHolder.getInstance().getClan(clan_id);
					RankingManagerStats.RankingOnlineClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingOnlineClass[number] = rset.getInt("base_class");
					RankingManagerStats.RankingOnline[number] = rset.getLong("onlinetime");
				}
				else
				{
					RankingManagerStats.RankingOnlineName[number] = null;
					RankingManagerStats.RankingOnlineClan[number] = null;
					RankingManagerStats.RankingOnlineClass[number] = 0;
					RankingManagerStats.RankingOnline[number] = 0;
				}
				number++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private void selectRankingRebirth()
	{
		int number = 0;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name, base_class, clanid, cv.value FROM characters AS c LEFT JOIN character_variables AS cv ON (c.charId=cv.obj_id) WHERE cv.name=\"rebirth\" AND accesslevel = 0 ORDER BY convert(cv.value, decimal) DESC LIMIT " + 10);
			rset = statement.executeQuery();
			while (rset.next())
			{
				if (!rset.getString("char_name").isEmpty())
				{
					RankingManagerStats.RankingRebirthName[number] = rset.getString("char_name");
					final int clan_id = rset.getInt("clanid");
					final Clan clan = clan_id == 0 ? null : ClanHolder.getInstance().getClan(clan_id);
					RankingManagerStats.RankingRebirthClan[number] = clan == null ? null : clan.getName();
					RankingManagerStats.RankingRebirthClass[number] = rset.getInt("base_class");
					RankingManagerStats.RankingRebirthAmount[number] = rset.getLong("value");
				}
				else
				{
					RankingManagerStats.RankingRebirthName[number] = null;
					RankingManagerStats.RankingRebirthClan[number] = null;
					RankingManagerStats.RankingRebirthClass[number] = 0;
					RankingManagerStats.RankingRebirthAmount[number] = 0;
				}
				number++;
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	private void selectRankingRaidPoints()
	{
		final List<CharPointsInfo> charPointList = new ArrayList<>();
		
		for (final Map.Entry<Integer, Map<Integer, Integer>> charPoints : RaidBossSpawnManager.getInstance().getPoints().entrySet())
		{
			final Map<Integer, Integer> tmpPoint = charPoints.getValue();
			
			int totalPoints = 0;
			for (final Entry<Integer, Integer> e : tmpPoint.entrySet())
			{
				switch (e.getKey())
				{
					case 0 :
						totalPoints += e.getValue();
						break;
				}
			}
			
			if (totalPoints != 0)
			{
				charPointList.add(new CharPointsInfo(charPoints.getKey(), totalPoints));
			}
		}
		
		if (charPointList == null || charPointList.isEmpty())
		{
			return;
		}
		
		final Comparator<CharPointsInfo> statsComparator = new SortCharPointsInfo();
		Collections.sort(charPointList, statsComparator);
		
		int count = 0;
		for (int i = 0; i < charPointList.size(); i++)
		{
			final CharPointsInfo data = charPointList.get(i);
			if (data != null)
			{
				final Player player = GameObjectsStorage.getPlayer(data.getCharId());
				if (player != null)
				{
					RankingManagerStats.RankingRpName[i] = player.getName(null);
					RankingManagerStats.RankingRpClan[i] = player.getClan() == null ? null : player.getClan().getName();
					RankingManagerStats.RankingRpClass[i] = player.getBaseClass();
					RankingManagerStats.RankingRpOn[i] = 1;
					RankingManagerStats.RankingRp[i] = data.getPoints();
				}
				else
				{
					Connection con = null;
					PreparedStatement statement = null;
					ResultSet rset = null;
					try
					{
						con = DatabaseFactory.getInstance().getConnection();
						statement = con.prepareStatement("SELECT char_name, base_class, clanid FROM characters WHERE charId = '" + data.getCharId() + "'");
						rset = statement.executeQuery();
						if (rset.next())
						{
							RankingManagerStats.RankingRpName[i] = rset.getString("char_name");
							final int clan_id = rset.getInt("clanid");
							final Clan clan = clan_id == 0 ? null : ClanHolder.getInstance().getClan(clan_id);
							RankingManagerStats.RankingRpClan[i] = clan == null ? null : clan.getName();
							RankingManagerStats.RankingRpClass[i] = rset.getInt("base_class");
							RankingManagerStats.RankingRpOn[i] = 0;
							RankingManagerStats.RankingRp[i] = data.getPoints();
						}
					}
					catch (final Exception e)
					{
						_log.warn("Error restore char data:", e);
					}
					finally
					{
						DbUtils.closeQuietly(con, statement, rset);
					}
				}
			}
			else
			{
				RankingManagerStats.RankingRpName[i] = null;
				RankingManagerStats.RankingRpClan[i] = null;
				RankingManagerStats.RankingRpClass[i] = 0;
				RankingManagerStats.RankingRpOn[i] = 0;
				RankingManagerStats.RankingRp[i] = 0;
			}
			count++;
			if (count >= 10)
			{
				break;
			}
		}
	}
	
	private void selectRankingClanRaidPoints()
	{
		final List<ClanPointsInfo> clanPointList = new ArrayList<>();
		
		for (final Integer clanId : RaidBossSpawnManager.getInstance().getClanPoints().keySet())
		{
			final int points = RaidBossSpawnManager.getInstance().getClanPoints().get(clanId);
			if (points != 0)
			{
				clanPointList.add(new ClanPointsInfo(clanId, points));
			}
		}
		
		if (clanPointList == null || clanPointList.isEmpty())
		{
			return;
		}
		
		final Comparator<ClanPointsInfo> statsComparator = new SortClanPointsInfo();
		Collections.sort(clanPointList, statsComparator);
		
		int count = 0;
		for (int i = 0; i < clanPointList.size(); i++)
		{
			final ClanPointsInfo data = clanPointList.get(i);
			if (data != null)
			{
				final Clan clan = ClanHolder.getInstance().getClan(data.getClanId());
				if (clan != null)
				{
					RankingManagerStats.RankingCRpName[i] = clan.getName();
					RankingManagerStats.RankingCRpLvl[i] = clan.getLevel();
					final String ally = clan.getAllyName();
					RankingManagerStats.RankingCRpAlly[i] = ally == null ? null : ally;
					RankingManagerStats.RankingCRp[i] = data.getPoints();
				}
				else
				{
					RankingManagerStats.RankingCRpName[i] = null;
					RankingManagerStats.RankingCRpLvl[i] = 0;
					RankingManagerStats.RankingCRpAlly[i] = null;
					RankingManagerStats.RankingCRp[i] = 0;
				}
			}
			else
			{
				RankingManagerStats.RankingCRpName[i] = null;
				RankingManagerStats.RankingCRpLvl[i] = 0;
				RankingManagerStats.RankingCRpAlly[i] = null;
				RankingManagerStats.RankingCRp[i] = 0;
			}
			count++;
			if (count >= 10)
			{
				break;
			}
		}
	}
	
	protected static class CharPointsInfo
	{
		private final int _charId;
		private final int _points;
		
		public CharPointsInfo(int charId, int points)
		{
			_charId = charId;
			_points = points;
		}
		
		public int getCharId()
		{
			return _charId;
		}
		
		public int getPoints()
		{
			return _points;
		}
	}
	
	protected static class ClanPointsInfo
	{
		private final int _clanId;
		private final int _points;
		
		public ClanPointsInfo(int charId, int points)
		{
			_clanId = charId;
			_points = points;
		}
		
		public int getClanId()
		{
			return _clanId;
		}
		
		public int getPoints()
		{
			return _points;
		}
	}
	
	private static class SortCharPointsInfo implements Comparator<CharPointsInfo>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(CharPointsInfo o1, CharPointsInfo o2)
		{
			return Integer.compare(o2.getPoints(), o1.getPoints());
		}
	}
	
	private static class SortClanPointsInfo implements Comparator<ClanPointsInfo>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(ClanPointsInfo o1, ClanPointsInfo o2)
		{
			return Integer.compare(o2.getPoints(), o1.getPoints());
		}
	}

	public static CommunityRanking getInstance()
	{
		return SingletonHolder._instance;
	}

	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
	}

	private static class SingletonHolder
	{
		protected static final CommunityRanking _instance = new CommunityRanking();
	}
}
