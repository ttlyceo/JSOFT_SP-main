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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.apache.StringUtils;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.htm.ImagesCache;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.handler.bypasshandlers.BypassHandler;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.model.NpcUtils;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.actor.templates.npc.MinionData;
import l2e.gameserver.model.actor.templates.npc.MinionTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.olympiad.OlympiadManager;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.RadarControl;
import l2e.gameserver.network.serverpackets.ShowBoard;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class CommunityRaidBoss extends AbstractCommunity implements ICommunityBoardHandler
{
	private static final int BOSSES_PER_PAGE = 10;
	
	public CommunityRaidBoss()
	{
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}

	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbsboss", "_bbsteleboss", "_bbsepic", "_bbsbosslist", "_bbsepiclist"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		if (!checkCondition(player, new StringTokenizer(command, "_").nextToken(), false, false))
		{
			return;
		}
		
		if (command.startsWith("_bbsbosslist"))
		{
			final StringTokenizer st = new StringTokenizer(command, "_");
			st.nextToken();
			
			final int sort = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "1");
			final int page = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "0");
			final String search = st.hasMoreTokens() ? st.nextToken().trim() : "";

			sendBossListPage(player, getSortByIndex(sort), page, search);
		}
		else if (command.startsWith("_bbsteleboss"))
		{
			final StringTokenizer st = new StringTokenizer(command, "_");
			st.nextToken();
			
			final int bossId = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "25044");
			manageButtons(player, 5, bossId);
		}
		else if (command.startsWith("_bbsepiclist"))
		{
			final StringTokenizer st = new StringTokenizer(command, "_");
			st.nextToken();
			
			final int sort = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "1");
			final int page = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "0");
			final String search = st.hasMoreTokens() ? st.nextToken().trim() : "";
			
			sendEpicBossListPage(player, getSortByIndex(sort), page, search);
		}
		else if (command.startsWith("_bbsboss"))
		{
			final StringTokenizer st = new StringTokenizer(command, "_");
			st.nextToken();
			
			final int sort = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "3");
			final int page = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "0");
			final String search = st.hasMoreTokens() ? st.nextToken().trim() : "";
			final int bossId = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "25044");
			final int buttonClick = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "0");

			manageButtons(player, buttonClick, bossId);

			if (buttonClick != 5)
			{
				sendBossDetails(player, getSortByIndex(sort), page, search, bossId);
			}
		}
		else if (command.startsWith("_bbsepic"))
		{
			final StringTokenizer st = new StringTokenizer(command, "_");
			st.nextToken();
			
			final int sort = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "3");
			final int page = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "0");
			final String search = st.hasMoreTokens() ? st.nextToken().trim() : "";
			final int bossId = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "25044");
			final int buttonClick = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "0");
			
			manageButtons(player, buttonClick, bossId);
			
			if (buttonClick != 5)
			{
				sendEpicBossDetails(player, getSortByIndex(sort), page, search, bossId);
			}
		}
	}

	private static void sendBossListPage(Player player, SortType sort, int page, String search)
	{
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/bosses/boss_list.htm");

		final Map<Integer, StatsSet> allBosses = getSearchedBosses(sort, search, player.getLang());
		final Map<Integer, StatsSet> bossesToShow = getBossesToShow(allBosses, page);
		final boolean isThereNextPage = allBosses.size() > bossesToShow.size();

		html = getBossListReplacements(player, html, page, bossesToShow, isThereNextPage);
		html = getNormalReplacements(player, html, page, sort, search, -1);
		
		separateAndSend(html, player);
	}
	
	private static void sendEpicBossListPage(Player player, SortType sort, int page, String search)
	{
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/bosses/epic_list.htm");
		
		final Map<Integer, StatsSet> allBosses = getSearchedEpicBosses(sort, search, player.getLang());
		final Map<Integer, StatsSet> bossesToShow = getBossesToShow(allBosses, page);
		final boolean isThereNextPage = allBosses.size() > bossesToShow.size();
		
		html = getEpicBossListReplacements(player, html, page, bossesToShow, isThereNextPage);
		html = getNormalReplacements(player, html, page, sort, search, -1);
		
		separateAndSend(html, player);
	}

	private static String getBossListReplacements(Player player, String html, int page, Map<Integer, StatsSet> allBosses, boolean nextPage)
	{
		String newHtml = html;

		int i = 0;

		for (final Entry<Integer, StatsSet> entry : allBosses.entrySet())
		{
			final StatsSet boss = entry.getValue();
			final NpcTemplate temp = NpcsParser.getInstance().getTemplate(entry.getKey().intValue());

			final boolean isAlive = isBossAlive(boss);

			newHtml = newHtml.replace("<?name_" + i + "?>", temp.getName(player.getLang()));
			newHtml = newHtml.replace("<?level_" + i + "?>", String.valueOf(temp.getLevel()));
			newHtml = newHtml.replace("<?points_" + i + "?>", String.valueOf(temp.getRewardRp()));
			newHtml = newHtml.replace("<?status_" + i + "?>", isAlive ? "" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.ALIVE") + "" : Config.ALLOW_BOSS_RESPAWN_TIME ? getRespawnTime(boss) : "" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.DEAD") + "");
			newHtml = newHtml.replace("<?status_color_" + i + "?>", getTextColor(isAlive));
			newHtml = newHtml.replace("<?bp_" + i + "?>", "<button value=\"" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.SHOW") + "\" action=\"bypass -h _bbsboss_<?sort?>_" + page + "_ <?search?> _" + entry.getKey() + "\" width=50 height=16 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
			newHtml = newHtml.replace("<?tp_" + i + "?>", "<button value=\"" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.SHOW") + "\" action=\"bypass -h _bbsteleboss_" + entry.getKey().intValue() + "\" width=50 height=16 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
			i++;
		}

		for (int j = i; j < BOSSES_PER_PAGE; j++)
		{
			newHtml = newHtml.replace("<?name_" + j + "?>", "...");
			newHtml = newHtml.replace("<?level_" + j + "?>", "...");
			newHtml = newHtml.replace("<?points_" + j + "?>", "...");
			newHtml = newHtml.replace("<?status_" + j + "?>", "...");
			newHtml = newHtml.replace("<?status_color_" + j + "?>", "FFFFFF");
			newHtml = newHtml.replace("<?bp_" + j + "?>", "...");
			newHtml = newHtml.replace("<?tp_" + j + "?>", "...");
		}

		newHtml = newHtml.replace("<?previous?>", page > 0 ? "<button action=\"bypass -h _bbsbosslist_<?sort?>_" + (page - 1) + "_<?search?>\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\">" : "<br>");
		newHtml = newHtml.replace("<?next?>", nextPage && i == BOSSES_PER_PAGE ? "<button action=\"bypass -h _bbsbosslist_<?sort?>_" + (page + 1) + "_<?search?>\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\">" : "<br>");
		newHtml = newHtml.replace("<?pages?>", String.valueOf(page + 1));

		return newHtml;
	}
	
	private static String getEpicBossListReplacements(Player player, String html, int page, Map<Integer, StatsSet> allBosses, boolean nextPage)
	{
		String newHtml = html;
		
		int i = 0;
		
		for (final Entry<Integer, StatsSet> entry : allBosses.entrySet())
		{
			final StatsSet boss = entry.getValue();
			final NpcTemplate temp = NpcsParser.getInstance().getTemplate(entry.getKey().intValue());
			
			final var status = EpicBossManager.getInstance().getBossStatus(entry.getKey().intValue());
			final var statusSting = getEpicBossStatus(player, status, entry.getKey().intValue());
			final var statusColor = getEpicBossColor(player, status, entry.getKey().intValue());
			
			newHtml = newHtml.replace("<?name_" + i + "?>", temp.getName(player.getLang()));
			newHtml = newHtml.replace("<?level_" + i + "?>", String.valueOf(temp.getLevel()));
			newHtml = newHtml.replace("<?points_" + i + "?>", String.valueOf(temp.getRewardRp()));
			newHtml = newHtml.replace("<?status_" + i + "?>", status < 3 ? "" + statusSting + "" : Config.ALLOW_BOSS_RESPAWN_TIME ? getRespawnTime(boss) : "" + statusSting + "");
			newHtml = newHtml.replace("<?status_color_" + i + "?>", statusColor);
			newHtml = newHtml.replace("<?bp_" + i + "?>", "<button value=\"" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.SHOW") + "\" action=\"bypass -h _bbsepic_<?sort?>_" + page + "_ <?search?> _" + entry.getKey() + "\" width=50 height=16 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
			i++;
		}
		
		for (int j = i; j < BOSSES_PER_PAGE; j++)
		{
			newHtml = newHtml.replace("<?name_" + j + "?>", "...");
			newHtml = newHtml.replace("<?level_" + j + "?>", "...");
			newHtml = newHtml.replace("<?points_" + j + "?>", "...");
			newHtml = newHtml.replace("<?status_" + j + "?>", "...");
			newHtml = newHtml.replace("<?status_color_" + j + "?>", "FFFFFF");
			newHtml = newHtml.replace("<?bp_" + j + "?>", "...");
			
		}
		
		newHtml = newHtml.replace("<?previous?>", page > 0 ? "<button action=\"bypass -h _bbsepiclist_<?sort?>_" + (page - 1) + "_<?search?>\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\">" : "<br>");
		newHtml = newHtml.replace("<?next?>", nextPage && i == BOSSES_PER_PAGE ? "<button action=\"bypass -h _bbsepiclist_<?sort?>_" + (page + 1) + "_<?search?>\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\">" : "<br>");
		newHtml = newHtml.replace("<?pages?>", String.valueOf(page + 1));
		
		return newHtml;
	}

	private static Map<Integer, StatsSet> getBossesToShow(Map<Integer, StatsSet> allBosses, int page)
	{
		final Map<Integer, StatsSet> bossesToShow = new LinkedHashMap<>();
		int i = 0;
		for (final Entry<Integer, StatsSet> entry : allBosses.entrySet())
		{
			if (i < page * BOSSES_PER_PAGE)
			{
				i++;
			}
			else
			{
				final StatsSet boss = entry.getValue();
				final NpcTemplate temp = NpcsParser.getInstance().getTemplate(entry.getKey().intValue());
				if (boss != null && temp != null)
				{
					i++;
					bossesToShow.put(entry.getKey(), entry.getValue());
					if (i > (page * BOSSES_PER_PAGE + BOSSES_PER_PAGE - 1))
					{
						return bossesToShow;
					}
				}
			}
		}
		return bossesToShow;
	}

	private static void sendBossDetails(Player player, SortType sort, int page, CharSequence search, int bossId)
	{
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/bosses/boss_details.htm");
		final StatsSet bossSet = RaidBossSpawnManager.getInstance().getStoredInfo().get(bossId);

		if (bossSet == null)
		{
			separateAndSend(html, player);
			return;
		}

		final NpcTemplate bossTemplate = NpcsParser.getInstance().getTemplate(bossId);
		final RaidBossInstance bossInstance = getAliveBoss(bossId);

		html = getDetailedBossReplacements(player, html, bossSet, bossTemplate, bossInstance);
		html = getNormalReplacements(player, html, page, sort, search, bossId);
		
		ImagesCache.getInstance().sendImageToPlayer(player, bossId);

		separateAndSend(html, player);
	}
	
	private static void sendEpicBossDetails(Player player, SortType sort, int page, CharSequence search, int bossId)
	{
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/bosses/epic_details.htm");
		final StatsSet bossSet = EpicBossManager.getInstance().getStoredInfo().get(bossId);
		
		if (bossSet == null)
		{
			separateAndSend(html, player);
			return;
		}
		
		final NpcTemplate bossTemplate = NpcsParser.getInstance().getTemplate(bossId);
		final GrandBossInstance bossInstance = getAliveEpicBoss(bossId);
		
		html = getDetailedEpicBossReplacements(player, html, bossSet, bossTemplate, bossInstance);
		html = getNormalReplacements(player, html, page, sort, search, bossId);
		
		ImagesCache.getInstance().sendImageToPlayer(player, bossId);
		
		separateAndSend(html, player);
	}

	private static void manageButtons(Player player, int buttonIndex, int bossId)
	{
		switch (buttonIndex)
		{
			case 1 :
				RaidBossSpawnManager.getInstance().showBossLocation(player, bossId);
				break;
			case 2 :
				final IBypassHandler handler = BypassHandler.getInstance().getHandler("drop");
				if (handler != null)
				{
					handler.useBypass("drop 1 " + bossId + "", player, null);
				}
				break;
			case 3 :
				NpcUtils.showNpcSkillList(player, NpcsParser.getInstance().getTemplate(bossId));
				break;
			case 4 :
				for (final int id : Config.BLOCKED_RAID_LIST)
				{
					if (id == bossId)
					{
						return;
					}
				}
				player.sendPacket(new RadarControl(2, 2, 0, 0, 0));
				break;
			case 5 :
				if (!Config.ALLOW_TELEPORT_TO_RAID || ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
				{
					player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
					return;
				}
				
				if (player.isInKrateisCube() || player.getUCState() > 0 || player.isCursedWeaponEquipped() || player.checkInTournament() || player.isInFightEvent() || player.isInCombat() || player.isInDuel() || player.getReflectionId() > 0 || player.isInStoreMode() || player.isInOfflineMode() || player.isJailed() || player.inObserverMode() || player.isInOlympiadMode() || player.isBlocked() || OlympiadManager.getInstance().isRegistered(player))
				{
					player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
					return;
				}
				
				for (final int id : Config.BLOCKED_RAID_LIST)
				{
					if (id == bossId)
					{
						player.sendMessage((new ServerMessage("CommunityRaidBoss.BLOCK_BOSS", player.getLang())).toString());
						return;
					}
				}
				
				final Spawner spawn = RaidBossSpawnManager.getInstance().getSpawns().get(bossId);
				if (spawn != null)
				{
					final Location loc = Location.findPointToStay(spawn.calcSpawnRangeLoc(spawn.getTemplate()), 100, 150, false);
					if (loc != null)
					{
						if (Config.TELEPORT_TO_RAID_PRICE[0] > 0)
						{
							if (player.getInventory().getItemByItemId(Config.TELEPORT_TO_RAID_PRICE[0]) == null)
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
							if (player.getInventory().getItemByItemId(Config.TELEPORT_TO_RAID_PRICE[0]).getCount() < Config.TELEPORT_TO_RAID_PRICE[1])
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
							player.destroyItemByItemId("TeleToRaid", Config.TELEPORT_TO_RAID_PRICE[0], Config.TELEPORT_TO_RAID_PRICE[1], player, true);
						}
						player.sendPacket(new ShowBoard());
						if (BotFunctions.getInstance().isAutoTpToRaidEnable(player))
						{
							BotFunctions.getInstance().getAutoTeleportToRaid(player, player.getLocation(), new Location(loc.getX(), loc.getY(), loc.getZ()));
							return;
						}
						player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), true, ReflectionManager.DEFAULT);
					}
					else
					{
						player.sendMessage((new ServerMessage("CommunityRaidBoss.EMPTY_LOC", player.getLang())).toString());
					}
				}
				break;
		}
	}

	private static String getDetailedBossReplacements(Player player, String html, StatsSet bossSet, NpcTemplate bossTemplate, RaidBossInstance bossInstance)
	{
		String newHtml = html;
		
		final boolean isAlive = isBossAlive(bossSet);
		
		newHtml = newHtml.replace("<?name?>", bossTemplate.getName(player.getLang()));
		newHtml = newHtml.replace("<?level?>", String.valueOf(bossTemplate.getLevel()));
		newHtml = newHtml.replace("<?status?>", isAlive ? "" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.ALIVE") + "" : Config.ALLOW_BOSS_RESPAWN_TIME ? getRespawnTime(bossSet) : "" + ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.DEAD") + "");
		newHtml = newHtml.replace("<?status_color?>", getTextColor(isAlive));
		newHtml = newHtml.replace("<?minions?>", String.valueOf(getMinionsCount(bossTemplate)));
		newHtml = newHtml.replace("<?currentHp?>", Util.formatAdena(bossInstance != null ? (int) bossInstance.getCurrentHp() : 0));
		newHtml = newHtml.replace("<?maxHp?>", Util.formatAdena((int) bossTemplate.getBaseHpMax()));
		newHtml = newHtml.replace("<?minions?>", String.valueOf(getMinionsCount(bossTemplate)));
		
		return newHtml;
	}
	
	protected static String getDetailedEpicBossReplacements(Player player, String html, StatsSet bossSet, NpcTemplate bossTemplate, GrandBossInstance bossInstance)
	{
		String newHtml = html;
		
		final var status = EpicBossManager.getInstance().getBossStatus(bossTemplate.getId());
		final var statusSting = getEpicBossStatus(player, status, bossTemplate.getId());
		final var statusColor = getEpicBossColor(player, status, bossTemplate.getId());
		
		newHtml = newHtml.replace("<?name?>", bossTemplate.getName(player.getLang()));
		newHtml = newHtml.replace("<?level?>", String.valueOf(bossTemplate.getLevel()));
		newHtml = newHtml.replace("<?status?>", status < 3 ? "" + statusSting + "" : Config.ALLOW_BOSS_RESPAWN_TIME ? getRespawnTime(bossSet) : "" + statusSting + "");
		newHtml = newHtml.replace("<?status_color?>", statusColor);
		newHtml = newHtml.replace("<?minions?>", String.valueOf(getMinionsCount(bossTemplate)));
		newHtml = newHtml.replace("<?currentHp?>", Util.formatAdena(bossInstance != null ? (int) bossInstance.getCurrentHp() : 0));
		newHtml = newHtml.replace("<?maxHp?>", Util.formatAdena((int) bossTemplate.getBaseHpMax()));
		newHtml = newHtml.replace("<?minions?>", String.valueOf(getMinionsCount(bossTemplate)));
		
		return newHtml;
	}

	private static String getNormalReplacements(Player player, String html, int page, SortType sort, CharSequence search, int bossId)
	{
		String newHtml = html;
		newHtml = newHtml.replace("<?page?>", String.valueOf(page));
		newHtml = newHtml.replace("<?sort?>", String.valueOf(sort.index));
		newHtml = newHtml.replace("<?serverId?>", String.valueOf(player.getRequestId()));
		newHtml = newHtml.replace("<?bossId?>", String.valueOf(bossId));
		newHtml = newHtml.replace("<?image?>", "Crest.crest_" + player.getRequestId() + "_" + bossId + "");
		newHtml = newHtml.replace("<?search?>", search);

		for (int i = 1; i <= 6; i++)
		{
			if (Math.abs(sort.index) == i)
			{
				newHtml = newHtml.replace("<?sort" + i + "?>", String.valueOf(-sort.index));
			}
			else
			{
				newHtml = newHtml.replace("<?sort" + i + "?>", String.valueOf(i));
			}
		}

		return newHtml;
	}

	private static boolean isBossAlive(StatsSet set)
	{
		return set.getLong("respawnTime") < System.currentTimeMillis();
	}
	
	private static String getEpicBossStatus(Player player, int id, int raidId)
	{
		switch (id)
		{
			case 0 :
				switch (raidId)
				{
					case 29020 :
					case 29028 :
					case 29068 :
					case 29118 :
						return ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.SLEEP");
					default :
						return ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.ALIVE");
				}
			case 1 :
				return ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.WAKEUP");
			case 2 :
				return ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.ALIVE");
			default :
				return ServerStorage.getInstance().getString(player.getLang(), "CommunityRaidBoss.DEAD");
		}
	}
	
	private static String getEpicBossColor(Player player, int id, int raidId)
	{
		switch (id)
		{
			case 0 :
				switch (raidId)
				{
					case 29020 :
					case 29028 :
					case 29068 :
					case 29118 :
						return "d5e340";
					default :
						return "259a30";
				}
			case 1 :
				return "e3b440";
			case 2 :
				return "259a30";
			default :
				return "b02e31";
		}
	}

	private static String getRespawnTime(StatsSet set)
	{
		if (set.getLong("respawnTime") < System.currentTimeMillis())
		{
			return "isAlive";
		}
		
		final long delay = (set.getLong("respawnTime") - System.currentTimeMillis()) / TimeUnit.SECONDS.toMillis(1L);

		final int hours = (int) (delay / 60 / 60);
		final int mins = (int) ((delay - (hours * 60 * 60)) / 60);
		final int secs = (int) ((delay - ((hours * 60 * 60) + (mins * 60))));
		
		final String Strhours = hours < 10 ? "0" + hours : "" + hours;
		final String Strmins = mins < 10 ? "0" + mins : "" + mins;
		final String Strsecs = secs < 10 ? "0" + secs : "" + secs;

		return "<font color=\"b02e31\">" + Strhours + ":" + Strmins + ":" + Strsecs + "</font>";
	}

	private static RaidBossInstance getAliveBoss(int bossId)
	{
		final RaidBossInstance boss = RaidBossSpawnManager.getInstance().getBossStatus(bossId);
		if (boss != null)
		{
			return (RaidBossInstance) GameObjectsStorage.getNpc(boss.getObjectId());
		}
		return null;
	}
	
	private static GrandBossInstance getAliveEpicBoss(int bossId)
	{
		final GrandBossInstance boss = EpicBossManager.getInstance().getBoss(bossId);
		if (boss != null)
		{
			return (GrandBossInstance) GameObjectsStorage.getNpc(boss.getObjectId());
		}
		return null;
	}

	private static int getMinionsCount(NpcTemplate template)
	{
		int minionsCount = 0;
		if (template.getMinionData().isEmpty())
		{
			return minionsCount;
		}
		
		if (template.isRandomMinons())
		{
			final MinionData data = template.getMinionData().size() > 1 ? template.getMinionData().get(Rnd.get(template.getMinionData().size())) : template.getMinionData().get(0);
			if (data != null)
			{
				for (final MinionTemplate tpl : data.getMinions())
				{
					minionsCount += tpl.getAmount();
				}
			}
		}
		else
		{
			for (final MinionData minion : template.getMinionData())
			{
				for (final MinionTemplate tpl : minion.getMinions())
				{
					minionsCount += tpl.getAmount();
				}
			}
		}
		return minionsCount;
	}

	private static String getTextColor(boolean alive)
	{
		if (alive)
		{
			return "259a30";
		}
		else
		{
			return "b02e31";
		}
	}

	private static Map<Integer, StatsSet> getSearchedBosses(SortType sort, String search, String lang)
	{
		Map<Integer, StatsSet> result = getBossesMapBySearch(search);
		if (Config.BBS_BOSSES_TO_SHOW.length > 1)
		{
			Map<Integer, StatsSet> bosses = new HashMap<>();
			for (final int id : result.keySet())
			{
				if (ArrayUtils.contains(Config.BBS_BOSSES_TO_SHOW, id))
				{
					bosses.put(id, result.get(id));
				}
			}
			bosses = sortResults(bosses, sort, lang);
			return bosses;
		}
		else
		{
			for (final int id : Config.BBS_BOSSES_TO_NOT_SHOW)
			{
				result.remove(id);
			}
			result = sortResults(result, sort, lang);
			return result;
		}
	}
	
	private static Map<Integer, StatsSet> getSearchedEpicBosses(SortType sort, String search, String lang)
	{
		Map<Integer, StatsSet> result = getEpicBossesMapBySearch(search);
		if (Config.BBS_BOSSES_TO_SHOW.length > 1)
		{
			Map<Integer, StatsSet> bosses = new HashMap<>();
			for (final int id : result.keySet())
			{
				if (ArrayUtils.contains(Config.BBS_BOSSES_TO_SHOW, id))
				{
					bosses.put(id, result.get(id));
				}
			}
			bosses = sortResults(bosses, sort, lang);
			return bosses;
		}
		else
		{
			for (final int id : Config.BBS_BOSSES_TO_NOT_SHOW)
			{
				result.remove(id);
			}
			result = sortResults(result, sort, lang);
			return result;
		}
	}

	private static Map<Integer, StatsSet> getBossesMapBySearch(String search)
	{
		Map<Integer, StatsSet> finalResult = new HashMap<>();
		if (search.isEmpty())
		{
			finalResult = RaidBossSpawnManager.getInstance().getStoredInfo();
		}
		else
		{
			for (final Entry<Integer, StatsSet> entry : RaidBossSpawnManager.getInstance().getStoredInfo().entrySet())
			{
				final NpcTemplate temp = NpcsParser.getInstance().getTemplate(entry.getKey().intValue());
				for (final String lang : Config.MULTILANG_ALLOWED)
				{
					if (lang != null)
					{
						if (StringUtils.containsIgnoreCase(temp.getName(lang), search))
						{
							finalResult.put(entry.getKey(), entry.getValue());
						}
					}
				}
			}
		}
		return finalResult;
	}
	
	private static Map<Integer, StatsSet> getEpicBossesMapBySearch(String search)
	{
		Map<Integer, StatsSet> finalResult = new HashMap<>();
		if (search.isEmpty())
		{
			finalResult = EpicBossManager.getInstance().getStoredInfo();
		}
		else
		{
			for (final Entry<Integer, StatsSet> entry : EpicBossManager.getInstance().getStoredInfo().entrySet())
			{
				final NpcTemplate temp = NpcsParser.getInstance().getTemplate(entry.getKey().intValue());
				for (final String lang : Config.MULTILANG_ALLOWED)
				{
					if (lang != null)
					{
						if (StringUtils.containsIgnoreCase(temp.getName(lang), search))
						{
							finalResult.put(entry.getKey(), entry.getValue());
						}
					}
				}
			}
		}
		return finalResult;
	}

	private static Map<Integer, StatsSet> sortResults(Map<Integer, StatsSet> result, SortType sort, String lang)
	{
		final ValueComparator bvc = new ValueComparator(result, sort, lang);
		final Map<Integer, StatsSet> sortedMap = new TreeMap<>(bvc);
		sortedMap.putAll(result);
		return sortedMap;
	}

	private static class ValueComparator implements Comparator<Integer>, Serializable
	{
		private static final long serialVersionUID = 4782405190873267622L;
		private final Map<Integer, StatsSet> _base;
		private final SortType _sortType;
		private final String _lang;

		private ValueComparator(Map<Integer, StatsSet> base, SortType sortType, String lang)
		{
			_base = base;
			_sortType = sortType;
			_lang = lang;
		}

		@Override
		public int compare(Integer o1, Integer o2)
		{
			int sortResult = sortById(o1, o2, _sortType, _lang);
			if (sortResult == 0 && !o1.equals(o2) && Math.abs(_sortType.index) != 1)
			{
				sortResult = sortById(o1, o2, SortType.NAME_ASC, _lang);
			}
			return sortResult;
		}

		private int sortById(Integer a, Integer b, SortType sorting, String lang)
		{
			final NpcTemplate temp1 = NpcsParser.getInstance().getTemplate(a.intValue());
			final NpcTemplate temp2 = NpcsParser.getInstance().getTemplate(b.intValue());
			final StatsSet set1 = _base.get(a);
			final StatsSet set2 = _base.get(b);
			final var isAlive1 = isBossAlive(set1);
			final var isAlive2 = isBossAlive(set2);
			switch (sorting)
			{
				case NAME_ASC :
					return temp1.getName(lang).compareTo(temp2.getName(lang));
				case NAME_DESC :
					return temp2.getName(lang).compareTo(temp1.getName(lang));
				case LEVEL_ASC :
					return Integer.compare(temp1.getLevel(), temp2.getLevel());
				case LEVEL_DESC :
					return Integer.compare(temp2.getLevel(), temp1.getLevel());
				case STATUS_ASC :
					return Integer.compare((int) set1.getLong("respawnTime"), (int) set2.getLong("respawnTime"));
				case STATUS_DESC :
					return Integer.compare((int) set2.getLong("respawnTime"), (int) set1.getLong("respawnTime"));
				case STATUS_ALIVE :
					return Boolean.compare(isAlive1, isAlive2);
				case STATUS_DEATH :
					return Boolean.compare(isAlive2, isAlive1);
			}
			return 0;
		}
	}

	private enum SortType
	{
		NAME_ASC(1), NAME_DESC(-1), LEVEL_ASC(2), LEVEL_DESC(-2), STATUS_ASC(3), STATUS_DESC(-3), STATUS_ALIVE(4), STATUS_DEATH(-4);

		public final int index;

		SortType(int index)
		{
			this.index = index;
		}
	}
	
	private static SortType getSortByIndex(int i)
	{
		for (final SortType type : SortType.values())
		{
			if (type.index == i)
			{
				return type;
			}
		}
		return SortType.NAME_ASC;
	}

	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}

	public static CommunityRaidBoss getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final CommunityRaidBoss _instance = new CommunityRaidBoss();
	}
}