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
package l2e.gameserver.model.entity.events;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.annotations.Nullable;
import l2e.commons.apache.reflect.MethodUtils;
import l2e.commons.collections.MultiValueSet;
import l2e.commons.log.LoggerObject;
import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.time.cron.SchedulingPattern.InvalidPatternException;
import l2e.commons.util.Rnd;
import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.FightEventParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.DailyTaskManager;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.listener.other.OnZoneEnterLeaveListener;
import l2e.gameserver.listener.player.OnPlayerExitListener;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.Party.messageType;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.instance.player.impl.InvisibleTask;
import l2e.gameserver.model.actor.instance.player.impl.TeleportTask;
import l2e.gameserver.model.actor.templates.daily.DailyTaskTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.player.PlayerTaskTemplate;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.entity.events.model.FightEventManager;
import l2e.gameserver.model.entity.events.model.FightEventNpcManager;
import l2e.gameserver.model.entity.events.model.FightLastStatsManager;
import l2e.gameserver.model.entity.events.model.FightLastStatsManager.FightEventStatType;
import l2e.gameserver.model.entity.events.model.template.FightEventGameRoom;
import l2e.gameserver.model.entity.events.model.template.FightEventItem;
import l2e.gameserver.model.entity.events.model.template.FightEventMap;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.entity.events.model.template.FightEventTeam;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.EarthQuake;
import l2e.gameserver.network.serverpackets.ExPVPMatchCCRecord;
import l2e.gameserver.network.serverpackets.ExPVPMatchCCRetire;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.RelationChanged;
import l2e.gameserver.network.serverpackets.ShowBoard;
import l2e.gameserver.network.serverpackets.ShowTutorialMark;

/**
 * Created by LordWinter
 */
public abstract class AbstractFightEvent extends LoggerObject
{
	protected final Map<String, List<Serializable>> _objects = new HashMap<>(0);
	protected final int _id;
	
	private class ZoneListener implements OnZoneEnterLeaveListener
	{
		@Override
		public void onZoneEnter(ZoneType zone, Creature actor)
		{
			if (actor.isPlayer())
			{
				final FightEventPlayer fPlayer = getFightEventPlayer(actor);
				if (fPlayer != null)
				{
					actor.sendPacket(new EarthQuake(actor.getX(), actor.getY(), actor.getZ(), 0, 1));
					_leftZone.remove(getFightEventPlayer(actor));
				}
			}
		}
		
		@Override
		public void onZoneLeave(ZoneType zone, Creature actor)
		{
			if (actor.isPlayer() && _state != EVENT_STATE.NOT_ACTIVE)
			{
				final FightEventPlayer fPlayer = getFightEventPlayer(actor);
				if (fPlayer != null)
				{
					_leftZone.put(getFightEventPlayer(actor), zone);
				}
			}
		}
	}
	
	private class ExitListener implements OnPlayerExitListener
	{
		@Override
		public void onPlayerExit(Player player)
		{
			loggedOut(player);
		}
	}
	
	public static enum EVENT_STATE
	{
		NOT_ACTIVE, COUNT_DOWN, PREPARATION, STARTED, OVER
	}
	
	public static final String REGISTERED_PLAYERS = "registered_players";
	public static final String LOGGED_OFF_PLAYERS = "logged_off_players";
	public static final String FIGHTING_PLAYERS = "fighting_players";
	
	private static final int CLOSE_LOCATIONS_VALUE = 150;
	
	protected static final int ITEMS_FOR_MINUTE_OF_AFK = Config.ITEMS_FOR_MINUTE_OF_AFK;
	
	protected static final int TIME_FIRST_TELEPORT = Config.TIME_FIRST_TELEPORT;
	protected static final int TIME_PLAYER_TELEPORTING = Config.TIME_PLAYER_TELEPORTING;
	protected static final int TIME_PREPARATION_BEFORE_FIRST_ROUND = Config.TIME_PREPARATION_BEFORE_FIRST_ROUND;
	protected static final int TIME_PREPARATION_BETWEEN_NEXT_ROUNDS = Config.TIME_PREPARATION_BETWEEN_NEXT_ROUNDS;
	protected static final int TIME_AFTER_ROUND_END_TO_RETURN_SPAWN = Config.TIME_AFTER_ROUND_END_TO_RETURN_SPAWN;
	protected static final int TIME_TELEPORT_BACK_TOWN = Config.TIME_TELEPORT_BACK_TOWN;
	protected static final int TIME_MAX_SECONDS_OUTSIDE_ZONE = Config.TIME_MAX_SECONDS_OUTSIDE_ZONE;
	protected static final int TIME_TO_BE_AFK = Config.TIME_TO_BE_AFK;
	
	private static final String[] ROUND_NUMBER_IN_STRING =
	{
	        "", "FightEvents.ROUND_1", "FightEvents.ROUND_2", "FightEvents.ROUND_3", "FightEvents.ROUND_4", "FightEvents.ROUND_5", "FightEvents.ROUND_6", "FightEvents.ROUND_7", "FightEvents.ROUND_8", "FightEvents.ROUND_9", "FightEvents.ROUND_10"
	};
	
	private final String _icon;
	private final int _roundRunTime;
	private final boolean _isAutoTimed;
	private final boolean _isGlobal;
	private final SchedulingPattern _autoStartTimes;
	private final boolean _teamed;
	private final boolean _teamTargets;
	private final boolean _givePvpPoints;
	private final boolean _attackPlayers;
	private final boolean _useScrolls;
	private final boolean _usePotions;
	private final boolean _useItemSummon;
	private final boolean _useCommunity;
	private final boolean _buffer;
	private final boolean _dispelBuffs;
	private final boolean _loseBuffsOnDeath;
	private final boolean _blockNoClassSkills;
	private final int[][] _fighterBuffs;
	private final int[][] _mageBuffs;
	private final boolean _rootBetweenRounds;
	private final FightEventManager.CLASSES[] _excludedClasses;
	private final int[] _excludedSkills;
	private final int[] _validLevels;
	private final List<Integer> _validProffs;
	private final boolean _roundEvent;
	private final int _rounds;
	private final int _respawnTime;
	private final boolean _ressAllowed;
	private final boolean _instanced;
	private final int[][] _rewardByParticipation;
	private final int[][] _rewardByKillPlayer;
	private final int[][] _rewardByPartPlayer;
	protected final int[][] _rewardByWinner;
	private final int[][] _rewardByTopKiller;
	private final boolean _isHideNames;
	private final boolean _isHideClanInfo;
	private final boolean _isHideByTransform;
	private final String _transformationList;
	private final boolean _getHeroStatus;
	private final boolean _getHeroSkills;
	private final long _heroTime;
	private final boolean _isUseEventItems;
	private final int[] _checkItemSlots;
	private final boolean _isPartyRandomKillPoints;
	
	protected EVENT_STATE _state = EVENT_STATE.NOT_ACTIVE;
	private ZoneListener _zoneListener = null;
	private ExitListener _exitListener = new ExitListener();
	private FightEventMap _map;
	private Reflection _reflection = ReflectionManager.DEFAULT;
	private final List<FightEventTeam> _teams = new CopyOnWriteArrayList<>();
	private final Map<FightEventPlayer, ZoneType> _leftZone = new ConcurrentHashMap<>();
	private final Map<String, ZoneType> _activeZones = new HashMap<>();
	private int _currentRound = 0;
	private FightEventGameRoom _room;
	private final MultiValueSet<String> _set;
	private Map<Integer, List<FightEventItem>> _templates = null;
	
	private final Map<String, Integer> _scores = new ConcurrentHashMap<>();
	private Map<String, Integer> _bestScores = new ConcurrentHashMap<>();
	private boolean _scoredUpdated = true;
	private boolean _isWithoutTime = false;
	private ScheduledFuture<?> _timer;
	private ScheduledFuture<?> _afkTask;
	private ScheduledFuture<?> _zoneTask;
	
	public AbstractFightEvent(MultiValueSet<String> set)
	{
		_id = set.getInteger("id");
		_icon = set.getString("icon");
		_roundRunTime = set.getInteger("roundRunTime", -1);
		_teamed = set.getBool("teamed");
		_teamTargets = set.getBool("canTargetTeam", false);
		_givePvpPoints = set.getBool("givePvpPoints", false);
		_attackPlayers = set.getBool("canAttackPlayers", true);
		_useScrolls = set.getBool("canUseScrolls", false);
		_usePotions = set.getBool("canUsePotions", false);
		_useItemSummon = set.getBool("canItemSummons", false);
		_useCommunity = set.getBool("canUseCommunity", true);
		_dispelBuffs = set.getBool("dispelBuffs", true);
		_loseBuffsOnDeath = set.getBool("loseBuffsOnDeath", false);
		_blockNoClassSkills = set.getBool("blockNoClassSkills", false);
		_buffer = set.getBool("useBuffs", false);
		_fighterBuffs = parseBuffs(set.getString("fighterBuffs", null));
		_mageBuffs = parseBuffs(set.getString("mageBuffs", null));
		_rootBetweenRounds = set.getBool("rootBetweenRounds");
		_excludedClasses = parseExcludedClasses(set.getString("excludedClasses", ""));
		_excludedSkills = parseExcludedSkills(set.getString("excludedSkills", null));
		_validLevels = parseValidLevels(set.getString("validLevels", null));
		_validProffs = parseValidProffs(set.getString("validProffs", null));
		_isAutoTimed = set.getBool("isAutoTimed", false);
		_isGlobal = set.getBool("isGlobal", false);
		_autoStartTimes = parseAutoStartTimes(set.getString("autoTimes", ""));
		_roundEvent = set.getBool("roundEvent");
		_rounds = set.getInteger("rounds", -1);
		_respawnTime = set.getInteger("respawnTime");
		_ressAllowed = set.getBool("ressAllowed");
		_instanced = set.getBool("instanced");
		_rewardByParticipation = parseItemsList(set.getString("rewardByParticipation", null));
		_rewardByKillPlayer = parseItemsList(set.getString("rewardByKillPlayer", null));
		_rewardByPartPlayer = parseItemsList(set.getString("rewardByPartPlayer", null));
		_rewardByWinner = parseItemsList(set.getString("rewardByWinner", null));
		_rewardByTopKiller = parseItemsList(set.getString("rewardByTopKiller", null));
		_isHideNames = set.getBool("isHideNames", false);
		_isHideClanInfo = set.getBool("isHideClanInfo", false);
		_isHideByTransform = set.getBool("isHideByTransform", false);
		_transformationList = set.getString("transformationList", "");
		_getHeroStatus = set.getBool("getHeroStatus", false);
		_getHeroSkills = set.getBool("getHeroSkills", false);
		_heroTime = set.getLong("heroTime", -1);
		_isUseEventItems = set.getBool("isUseEventItems", false);
		_checkItemSlots = parseExcludedSkills(set.getString("checkItemSlots", null));
		_isPartyRandomKillPoints = set.getBool("isPartyRandomKillPoints", false);
		_set = set;
	}
	
	public void prepareEvent(FightEventGameRoom room, Collection<Player> playerList, boolean checkReflection)
	{
		_map = room.getMap();
		_room = room;
		_templates = FightEventParser.getInstance().getEventItems(getId());
		for (final Player player : playerList)
		{
			addObject(REGISTERED_PLAYERS, new FightEventPlayer(player));
			player.addEvent(this);
		}
		
		if (isGlobal())
		{
			_state = EVENT_STATE.PREPARATION;
			startEvent(checkReflection);
		}
		else
		{
			startTeleportTimer(_room);
		}
	}
	
	public void addToGlobalEvent(Player player)
	{
		final FightEventPlayer eventPlayer = new FightEventPlayer(player);
		if (eventPlayer != null)
		{
			addObject(REGISTERED_PLAYERS, eventPlayer);
			player.addEvent(this);
			_room.addAlonePlayer(player);
			if (isTeamed() && _teams.size() > 0)
			{
				int teamIndex = 0;
				int teamPlayers = _teams.get(teamIndex).getPlayers().size();
				for (int i = 1; i < _teams.size(); i++)
				{
					final int curPlayers = _teams.get(i).getPlayers().size();
					if (curPlayers < teamPlayers)
					{
						teamPlayers = curPlayers;
						teamIndex = i;
					}
				}
				
				final FightEventTeam team = _teams.get(teamIndex);
				if (team != null)
				{
					eventPlayer.setTeam(team);
					team.addPlayer(eventPlayer);
				}
			}
			
			if (isHideByTransform())
			{
				calcTransformation(player, FightEventGameRoom.getPlayerClassGroup(player).getTransformIndex());
			}
			teleportSinglePlayer(eventPlayer, true, true, true);
			_scores.put(getScorePlayerName(eventPlayer), eventPlayer.getKills());
			updateScreenScores(player);
		}
	}
	
	public void restartEvent()
	{
		final FightEventGameRoom room = new FightEventGameRoom(this);
		if (room != null)
		{
			_state = EVENT_STATE.PREPARATION;
			final List<Player> playerList = getAllFightingPlayers();
			if (playerList != null && playerList.size() > 0)
			{
				getObjects().clear();
				for (final Player player : playerList)
				{
					player.removeEvent(this);
					room.addAlonePlayer(player);
				}
			}
			prepareEvent(room, room.getAllPlayers(), false);
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (player == null || player.isInOfflineMode() || player.isInFightEvent())
				{
					continue;
				}
				
				final ServerMessage message = new ServerMessage("FightEvents.RESTART", player.getLang());
				message.add(player.getEventName(getId()));
				
				final ServerMessage message1 = new ServerMessage("FightEvents.MAP_NAME", player.getLang());
				message1.add(_map.getName());
				
				player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, player.getEventName(getId()), message.toString()));
				player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, player.getEventName(getId()), message1.toString()));
			}
			
			if (Config.ALLOW_REG_CONFIRM_DLG)
			{
				FightEventManager.getInstance().sendEventInvitations(this);
			}
			FightEventNpcManager.getInstance().tryGlobalSpawnRegNpc();
		}
	}
	
	public void startEvent(boolean checkReflection)
	{
		_state = EVENT_STATE.PREPARATION;
		_zoneListener = new ZoneListener();
		FightLastStatsManager.getInstance().clearStats(getId());
		
		final List<Integer> doors = new ArrayList<>();
		for (final int door : getMap().getDoors())
		{
			if (door != 0)
			{
				doors.add(door);
			}
		}
		
		for (final Entry<Integer, Map<String, ZoneType>> entry : getMap().getTerritories().entrySet())
		{
			for (final Entry<String, ZoneType> team : entry.getValue().entrySet())
			{
				_activeZones.put(team.getKey(), team.getValue());
			}
		}
		
		if (isInstanced())
		{
			createInstance(doors, _activeZones);
		}
		
		_activeZones.values().stream().filter(z -> (z != null)).forEach(z -> z.addListener(_zoneListener));
		
		final List<FightEventPlayer> playersToRemove = new ArrayList<>();
		for (final FightEventPlayer iFPlayer : getPlayers(REGISTERED_PLAYERS))
		{
			stopInvisibility(iFPlayer.getPlayer());
			if (!checkIfRegisteredPlayerMeetCriteria(iFPlayer, checkReflection))
			{
				playersToRemove.add(iFPlayer);
				continue;
			}
			
			if (isHideByTransform())
			{
				calcTransformation(iFPlayer.getPlayer(), FightEventGameRoom.getPlayerClassGroup(iFPlayer.getPlayer()).getTransformIndex());
			}
		}
		
		playersToRemove.stream().filter(p -> (p != null)).forEach(p -> unregister(p.getPlayer()));
		
		if (isTeamed())
		{
			spreadIntoTeamsAndPartys();
		}
		
		teleportRegisteredPlayers(checkReflection);
		
		updateEveryScore();
		
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS, REGISTERED_PLAYERS))
		{
			iFPlayer.getPlayer().isntAfk();
			iFPlayer.getPlayer().setFightEventGameRoom(null);
		}
		
		startNewTimer(true, TIME_PLAYER_TELEPORTING * 1000, "startRoundTimer", TIME_PREPARATION_BEFORE_FIRST_ROUND);
		
		_zoneTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new LeftZoneThread(), 5000L, 1000L);
	}
	
	public void startRound()
	{
		_state = EVENT_STATE.STARTED;
		
		_currentRound++;
		
		if (isRoundEvent())
		{
			if (_currentRound == _rounds)
			{
				sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.LAST_ROUND_START", true);
			}
			else
			{
				sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.ROUND_START", true, String.valueOf(_currentRound));
			}
		}
		else
		{
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.FIGHT", true);
		}
		
		unrootPlayers();
		
		if (getRoundRuntime() > 0)
		{
			startNewTimer(true, (int) ((double) getRoundRuntime() / 2 * 60000), "endRoundTimer", (int) ((double) getRoundRuntime() / 2 * 60));
		}
		
		if (_currentRound == 1)
		{
			_afkTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new CheckAfkThread(), 1000, 1000);
		}
		
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			hideScores(iFPlayer.getPlayer());
			iFPlayer.getPlayer().broadcastUserInfo(true);
		}
	}
	
	public void endRound()
	{
		_state = EVENT_STATE.OVER;
		
		if (_afkTask != null)
		{
			_afkTask.cancel(false);
		}
		_afkTask = null;
		
		if (!isLastRound())
		{
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.ROUND_OVER", false, String.valueOf(_currentRound));
		}
		else
		{
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.EVENT_OVER", false);
		}
		
		ressAndHealPlayers();
		
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			showScores(iFPlayer.getPlayer());
			handleAfk(iFPlayer, false);
		}
		
		if (!isLastRound())
		{
			if (isTeamed())
			{
				getTeams().stream().filter(t -> (t != null)).forEach(t -> t.setSpawnLoc(null));
			}
			
			ThreadPoolManager.getInstance().schedule(() ->
			{
				getPlayers(FIGHTING_PLAYERS).stream().filter(p -> (p != null)).forEach(p -> teleportSinglePlayer(p, false, true, false));
				startNewTimer(true, 0, "startRoundTimer", TIME_PREPARATION_BETWEEN_NEXT_ROUNDS);
				
			}, TIME_AFTER_ROUND_END_TO_RETURN_SPAWN * 1000);
		}
		else
		{
			ThreadPoolManager.getInstance().schedule(() -> stopEvent(), 5 * 1000);
			
			if (isTeamed())
			{
				announceWinnerTeam(true, null);
			}
			else
			{
				announceWinnerPlayer(true, null);
			}
		}
		getPlayers(FIGHTING_PLAYERS).stream().filter(p -> (p != null)).forEach(p -> p.getPlayer().broadcastUserInfo(true));
	}
	
	public void stopEvent()
	{
		_state = EVENT_STATE.NOT_ACTIVE;
		_room = null;
		
		showLastAFkMessage();
		final FightEventPlayer[] topKillers = getTopKillers();
		announceTopKillers(topKillers);
		giveRewards(topKillers);
		
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			iFPlayer.getPlayer().updateAndBroadcastStatus(1);
			if (!isGlobal())
			{
				iFPlayer.getPlayer().removeListener(_exitListener);
			}
			if (iFPlayer.getPlayer().getSummon() != null)
			{
				iFPlayer.getPlayer().getSummon().updateAndBroadcastStatus(0);
			}
		}
		
		FightLastStatsManager.getInstance().updateEventStats(getId());
		
		if (!isWithoutTime())
		{
			ThreadPoolManager.getInstance().schedule(() ->
			{
				for (final Player player : getAllFightingPlayers())
				{
					leaveEvent(player, true, false, false);
					player.sendPacket(new ExShowScreenMessage("", 10, ExShowScreenMessage.TOP_LEFT, false));
				}
			}, 10 * 1000);
			
			ThreadPoolManager.getInstance().schedule(() ->
			{
				destroyMe();
			}, (15 + TIME_TELEPORT_BACK_TOWN) * 1000);
		}
		else
		{
			startNewTimer(true, TIME_PLAYER_TELEPORTING * 1000, "startChangeMapTimer", TIME_PREPARATION_BEFORE_FIRST_ROUND);
		}
	}
	
	public void destroyMe()
	{
		if (_zoneTask != null)
		{
			_zoneTask.cancel(false);
		}
		_zoneTask = null;
		_activeZones.values().stream().filter(z -> (z != null)).forEach(z -> z.removeListener(_zoneListener));
		
		if (_timer != null)
		{
			_timer.cancel(false);
		}
		_timer = null;
		_bestScores.clear();
		_scores.clear();
		_leftZone.clear();
		_activeZones.clear();
		if (!isGlobal())
		{
			getObjects().clear();
			_exitListener = null;
		}
		_room = null;
		_zoneListener = null;
		if (!isWithoutTime())
		{
			DoubleSessionManager.getInstance().clear(getId());
			FightEventManager.getInstance().removeEventId(getId());
			FightEventManager.getInstance().calcNewEventTime(this);
		}
		
		if (isGlobal() && isWithoutTime())
		{
			restartEvent();
		}
		
		if (!_reflection.isDefault())
		{
			ThreadPoolManager.getInstance().schedule(() ->
			{
				_reflection.collapse();
				if (isGlobal())
				{
					FightEventNpcManager.getInstance().tryUnspawnGlobalRegNpc();
				}
			}, 5000);
		}
	}
	
	public void onKilled(Creature actor, Creature victim)
	{
		if (victim.isPlayer() && getRespawnTime() > 0)
		{
			showScores(victim);
		}
		
		if (actor != null && actor.isPlayer() && getFightEventPlayer(actor) != null)
		{
			FightLastStatsManager.getInstance().updateStat(getId(), actor.getActingPlayer(), FightEventStatType.KILL_PLAYER, getFightEventPlayer(actor).getKills());
		}
		
		if (victim.isPlayer() && getRespawnTime() > 0 && !_ressAllowed && getFightEventPlayer(victim.getActingPlayer()) != null)
		{
			startNewTimer(false, 0, "ressurectionTimer", getRespawnTime(), getFightEventPlayer(victim));
		}
	}
	
	public void onDamage(Creature actor, Creature victim, double damage)
	{
	}
	
	public void requestRespawn(Player activeChar)
	{
		if (getRespawnTime() > 0)
		{
			startNewTimer(false, 0, "ressurectionTimer", getRespawnTime(), getFightEventPlayer(activeChar));
		}
	}
	
	public boolean canAttack(Creature target, Creature attacker)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
		}
		final Player player = attacker.getActingPlayer();
		if (player == null)
		{
			return true;
		}

		if (target != null && target.isMonster())
		{
			return true;
		}
		
		if (player != null && player.isRespawnProtected())
		{
			return false;
		}
		
		if (target != null && target.isPlayer() && target.getActingPlayer().isRespawnProtected())
		{
			return false;
		}
		
		if (isTeamed())
		{
			final FightEventPlayer targetFPlayer = getFightEventPlayer(target);
			final FightEventPlayer attackerFPlayer = getFightEventPlayer(attacker);
			
			if (targetFPlayer == null || attackerFPlayer == null || targetFPlayer.getTeam().equals(attackerFPlayer.getTeam()))
			{
				return false;
			}
		}

		if (!canAttackPlayers())
		{
			return false;
		}
		return true;
	}

	public boolean canAction(Creature target, Creature attacker)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
		}
		final Player player = attacker.getActingPlayer();
		if (player == null)
		{
			return true;
		}

		if (attacker != null && target != null)
		{
			if (attacker.getObjectId() == target.getObjectId())
			{
				return true;
			}
		}

		if (target != null && target.isMonster())
		{
			return true;
		}

		if (isTeamed())
		{
			final FightEventPlayer targetFPlayer = getFightEventPlayer(target);
			final FightEventPlayer attackerFPlayer = getFightEventPlayer(attacker);
			
			if (targetFPlayer == null || attackerFPlayer == null || (targetFPlayer.getTeam().equals(attackerFPlayer.getTeam()) && !canTeamTarget()))
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean canUseMagic(Creature target, Creature attacker, Skill skill)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
		}
		
		if (attacker.isSummon() && skill.isAura())
		{
			return true;
		}
		
		if (attacker != null && target != null)
		{
			if (!canUseSkill(attacker, target, skill))
			{
				return false;
			}
			
			if (attacker.getObjectId() == target.getObjectId())
			{
				return true;
			}
		}
		
		if (target != null && target.isMonster())
		{
			return true;
		}
		
		if (attacker != null && attacker.isPlayer() && attacker.getActingPlayer().isRespawnProtected())
		{
			return false;
		}
		
		if (target != null && target.isPlayer() && target.getActingPlayer().isRespawnProtected())
		{
			return false;
		}
		
		if (isTeamed())
		{
			final FightEventPlayer targetFPlayer = getFightEventPlayer(target);
			final FightEventPlayer attackerFPlayer = getFightEventPlayer(attacker);
			
			if (targetFPlayer == null || attackerFPlayer == null || (targetFPlayer.getTeam().equals(attackerFPlayer.getTeam()) && skill.isOffensive()))
			{
				return false;
			}
		}

		if (!canAttackPlayers())
		{
			return false;
		}
		return true;
	}
	
	public boolean canUseSkill(Creature caster, Creature target, Skill skill)
	{
		if (_excludedSkills != null)
		{
			for (final int id : _excludedSkills)
			{
				if (skill.getId() == id)
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean canUseScroll(Creature caster)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
		}
		
		final FightEventPlayer FPlayer = getFightEventPlayer(caster);
		if (FPlayer != null && !canUseScrolls())
		{
			return false;
		}
		return true;
	}
	
	public boolean canUsePotion(Creature caster)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
		}
		
		final FightEventPlayer FPlayer = getFightEventPlayer(caster);
		if (FPlayer != null && !canUsePotions())
		{
			return false;
		}
		return true;
	}
	
	public boolean canUseEscape(Creature caster)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
		}
		
		final FightEventPlayer FPlayer = getFightEventPlayer(caster);
		if (FPlayer != null)
		{
			return false;
		}
		return true;
	}
	
	public boolean canUseItemSummon(Creature caster)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
		}
		
		final FightEventPlayer FPlayer = getFightEventPlayer(caster);
		if (FPlayer != null && !canUseItemSummons())
		{
			return false;
		}
		return true;
	}
	
	public boolean canRessurect(Player player, Creature creature)
	{
		return _ressAllowed;
	}
	
	public int getMySpeed(Player player)
	{
		return -1;
	}
	
	public int getPAtkSpd(Player player)
	{
		return -1;
	}
	
	public void checkRestartLocs(Player player, Map<TeleportWhereType, Boolean> r)
	{
		r.clear();
		if (isTeamed() && getRespawnTime() > 0 && getFightEventPlayer(player) != null && _ressAllowed)
		{
			r.put(TeleportWhereType.SIEGEFLAG, true);
		}
	}
	
	public boolean canUseBuffer(Player player, boolean heal)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);
		if (!getBuffer())
		{
			return false;
		}
		if (player.isInCombat())
		{
			return false;
		}
		if (heal)
		{
			if (player.isDead())
			{
				return false;
			}
			if (_state != EVENT_STATE.STARTED)
			{
				return true;
			}
			if (fPlayer.isInvisible())
			{
				return true;
			}
			return false;
		}
		return true;
	}
	
	public boolean canUsePositiveMagic(Creature user, Creature target)
	{
		final Player player = user.getActingPlayer();
		if (player == null)
		{
			return true;
		}
		
		return isFriend(user, target);
	}
	
	public int getRelation(Player thisPlayer, Player target, int oldRelation)
	{
		if (_state == EVENT_STATE.STARTED)
		{
			return isFriend(thisPlayer, target) ? getFriendRelation() : getWarRelation();
		}
		else
		{
			return oldRelation;
		}
	}
	
	public boolean canJoinParty(Player sender, Player receiver)
	{
		return isFriend(sender, receiver);
	}
	
	public boolean canReceiveInvitations(Player sender, Player receiver)
	{
		return true;
	}
	
	public boolean canOpenStore(Player player)
	{
		return false;
	}
	
	public boolean canStandUp(Player player)
	{
		return true;
	}
	
	public boolean loseBuffsOnDeath(Player player)
	{
		return _loseBuffsOnDeath;
	}
	
	public boolean isBlockNoClassSkills()
	{
		return _blockNoClassSkills;
	}
	
	public boolean isDispelBuffs()
	{
		return _dispelBuffs;
	}
	
	protected boolean inScreenShowBeScoreNotKills()
	{
		return true;
	}
	
	protected boolean inScreenShowBeTeamNotInvidual()
	{
		return isTeamed();
	}
	
	public boolean isFriend(Creature c1, Creature c2)
	{
		if (c1.equals(c2))
		{
			return true;
		}
		
		if (!c1.isPlayable() || !c2.isPlayable())
		{
			return true;
		}
		
		if (c1.isSummon() && c2.isPlayer() && c2.getActingPlayer().getSummon() != null && c2.getActingPlayer().getSummon().equals(c1))
		{
			return true;
		}
		
		if (c2.isSummon() && c1.isPlayer() && c1.getActingPlayer().getSummon() != null && c1.getActingPlayer().getSummon().equals(c2))
		{
			return true;
		}
		
		final FightEventPlayer fPlayer1 = getFightEventPlayer(c1.getActingPlayer());
		final FightEventPlayer fPlayer2 = getFightEventPlayer(c2.getActingPlayer());
		
		if (isTeamed())
		{
			if (fPlayer1 == null || fPlayer2 == null || !fPlayer1.getTeam().equals(fPlayer2.getTeam()))
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		else
		{
			return false;
		}
	}
	
	public boolean isInvisible(Player actor, Player watcher)
	{
		return actor.isVisible();
	}
	
	public String getVisibleTitle(Player player, Player viewer, String currentTitle, boolean toMe)
	{
		return currentTitle;
	}
	
	public String getVisibleName(Player player, String currentName, boolean toMe)
	{
		if (isHideNames() && !toMe)
		{
			return "Player";
		}
		return currentName;
	}
	
	public int getVisibleNameColor(Player player, int currentNameColor, boolean toMe)
	{
		if (isTeamed())
		{
			final FightEventPlayer fPlayer = getFightEventPlayer(player);
			return fPlayer.getTeam().getNickColor();
		}
		return currentNameColor;
	}
	
	protected void giveItemRewardsForPlayer(FightEventPlayer fPlayer, Map<Integer, Long> rewards, boolean topKiller, boolean isLogOut)
	{
		if (fPlayer == null)
		{
			return;
		}
		
		if (rewards == null)
		{
			rewards = new HashMap<>();
		}
		
		if (!isLogOut)
		{
			rewards = giveRewardByParticipation(fPlayer, rewards);
		}
		rewards = giveRewardByKillPlayer(fPlayer, fPlayer.getKills(), rewards);
		rewards = giveRewardByPartPlayer(fPlayer, fPlayer.getPartPoints(), rewards);
		rewards = giveRewardForWinningTeam(fPlayer, rewards, true, topKiller);
		if (topKiller)
		{
			fPlayer.getPlayer().getCounters().addAchivementInfo("eventTopKiller", 0, -1, false, false, false);
			rewards = giveRewardForTopKiller(fPlayer, rewards);
		}
		
		final double minutesAFK = Math.abs((fPlayer.getTotalAfkSeconds() / 60) * ITEMS_FOR_MINUTE_OF_AFK);
		if (rewards != null && rewards.size() > 0)
		{
			for (final int item : rewards.keySet())
			{
				if (item == 0)
				{
					continue;
				}
				final long totalAmount = (long) ((rewards.get(item) * Config.FIGHT_EVENTS_REWARD_MULTIPLIER * fPlayer.getPlayer().getPremiumBonus().getEventBonus()) - (int) minutesAFK);
				if (totalAmount <= 0)
				{
					continue;
				}
				fPlayer.getPlayer().addItem("Event Reward", item, totalAmount, fPlayer.getPlayer(), true);
			}
			rewards.clear();
		}
	}
	
	private Map<Integer, Long> giveRewardByParticipation(FightEventPlayer fPlayer, Map<Integer, Long> rewards)
	{
		if (_rewardByParticipation == null || _rewardByParticipation.length == 0 || rewards == null)
		{
			return rewards;
		}
		
		for (final int[] item : _rewardByParticipation)
		{
			if ((item == null) || (item.length != 2))
			{
				continue;
			}
			
			if (rewards.containsKey(item[0]))
			{
				final long amount = rewards.get(item[0]) + item[1];
				rewards.put(item[0], amount);
			}
			else
			{
				rewards.put(item[0], (long) item[1]);
			}
		}
		return rewards;
	}
	
	private Map<Integer, Long> giveRewardForTopKiller(FightEventPlayer fPlayer, Map<Integer, Long> rewards)
	{
		if (_rewardByTopKiller == null || _rewardByTopKiller.length == 0)
		{
			return rewards;
		}
		
		for (final int[] item : _rewardByTopKiller)
		{
			if ((item == null) || (item.length != 2))
			{
				continue;
			}
			
			if (rewards.containsKey(item[0]))
			{
				final long amount = rewards.get(item[0]) + item[1];
				rewards.put(item[0], amount);
			}
			else
			{
				rewards.put(item[0], (long) item[1]);
			}
		}
		return rewards;
	}
	
	protected Map<Integer, Long> giveRewardForWinningTeam(FightEventPlayer fPlayer, Map<Integer, Long> rewards, boolean atLeast1Kill, boolean isTopKiller)
	{
		if (!_teamed || _state != EVENT_STATE.OVER && _state != EVENT_STATE.NOT_ACTIVE)
		{
			return rewards;
		}
		
		if ((_rewardByWinner == null || _rewardByWinner.length == 0) && !isGetHeroStatus())
		{
			return rewards;
		}
		
		if (atLeast1Kill && fPlayer.getKills() <= 0 && FightEventGameRoom.getPlayerClassGroup(fPlayer.getPlayer()) != FightEventManager.CLASSES.HEALERS)
		{
			return rewards;
		}
		
		FightEventTeam winner = null;
		int winnerPoints = -1;
		boolean sameAmount = false;
		for (final FightEventTeam team : getTeams())
		{
			if (team.getScore() > winnerPoints)
			{
				winner = team;
				winnerPoints = team.getScore();
				sameAmount = false;
			}
			else if (team.getScore() == winnerPoints)
			{
				sameAmount = true;
			}
		}
		
		if (!sameAmount && fPlayer.getTeam().equals(winner))
		{
			if (isTopKiller && isGetHeroStatus())
			{
				fPlayer.getPlayer().setHero(true, isGetHeroSkills());
				if (getHeroTime() > 0)
				{
					final var endTime = System.currentTimeMillis() + (getHeroTime() * 60000L);
					fPlayer.getPlayer().setVar("tempHero", String.valueOf(endTime));
					if (isGetHeroSkills())
					{
						fPlayer.getPlayer().setVar("tempHeroSkills", "1");
					}
					fPlayer.getPlayer().startTempHeroTask(endTime, isGetHeroSkills() ? 1 : 0);
				}
			}
			
			if (_rewardByWinner != null && _rewardByWinner.length > 0)
			{
				for (final int[] item : _rewardByWinner)
				{
					if ((item == null) || (item.length != 2))
					{
						continue;
					}
					
					if (rewards.containsKey(item[0]))
					{
						final long amount = rewards.get(item[0]) + item[1];
						rewards.put(item[0], amount);
					}
					else
					{
						rewards.put(item[0], (long) item[1]);
					}
				}
			}
		}
		return rewards;
	}
	
	private Map<Integer, Long> giveRewardByKillPlayer(FightEventPlayer fPlayer, int kills, Map<Integer, Long> rewards)
	{
		if (_rewardByKillPlayer != null && _rewardByKillPlayer.length > 0 && kills > 0)
		{
			for (final int[] item : _rewardByKillPlayer)
			{
				if ((item == null) || (item.length != 2))
				{
					continue;
				}
				
				if (rewards.containsKey(item[0]))
				{
					final long amount = rewards.get(item[0]) + (item[1] * kills);
					rewards.put(item[0], amount);
				}
				else
				{
					rewards.put(item[0], ((long) item[1] * kills));
				}
			}
		}
		return rewards;
	}
	
	private Map<Integer, Long> giveRewardByPartPlayer(FightEventPlayer fPlayer, int kills, Map<Integer, Long> rewards)
	{
		if (_isPartyRandomKillPoints && _rewardByPartPlayer != null && _rewardByPartPlayer.length > 0 && kills > 0)
		{
			for (final int[] item : _rewardByPartPlayer)
			{
				if ((item == null) || (item.length != 2))
				{
					continue;
				}
				
				if (rewards.containsKey(item[0]))
				{
					final long amount = rewards.get(item[0]) + (item[1] * kills);
					rewards.put(item[0], amount);
				}
				else
				{
					rewards.put(item[0], ((long) item[1] * kills));
				}
			}
		}
		return rewards;
	}
	
	public void startTeleportTimer(FightEventGameRoom room)
	{
		setState(EVENT_STATE.COUNT_DOWN);
		
		startNewTimer(true, 0, "teleportWholeRoomTimer", TIME_FIRST_TELEPORT);
	}
	
	protected void teleportRegisteredPlayers(boolean addSaveLoc)
	{
		getPlayers(REGISTERED_PLAYERS).stream().filter(p -> (p != null)).forEach(p -> teleportSinglePlayer(p, true, true, addSaveLoc));
	}
	
	protected void teleportSinglePlayer(FightEventPlayer fPlayer, boolean firstTime, boolean healAndRess, boolean addSaveLoc)
	{
		final Player player = fPlayer.getPlayer();
		player.setExpBeforeDeath(0);
		if (healAndRess)
		{
			if (player.isDead())
			{
				player.doRevive(100.);
			}
		}
		
		if (firstTime && addSaveLoc)
		{
			player.setSaveLoc(player.getLocation());
			player.setCanRevive(false);
			if (_excludedSkills != null)
			{
				for (final int id : _excludedSkills)
				{
					final var sk = player.getKnownSkill(id);
					if (sk != null)
					{
						player.addBlockSkill(sk, true);
					}
				}
			}
		}
		
		final Location loc = getSinglePlayerSpawnLocation(fPlayer);
		player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), true, isInstanced() ? _reflection : ReflectionManager.DEFAULT);
		
		if (_state == EVENT_STATE.PREPARATION || _state == EVENT_STATE.OVER)
		{
			rootPlayer(player);
		}
		
		if (player.getSummon() instanceof PetInstance)
		{
			player.getSummon().unSummon(player);
		}
		
		if (firstTime)
		{
			removeObject(REGISTERED_PLAYERS, fPlayer);
			addObject(FIGHTING_PLAYERS, fPlayer);
			if (!canUseCommunity())
			{
				player.sendPacket(new ShowBoard());
			}
			
			if (isDispelBuffs())
			{
				player.stopAllEffects();
				if (player.getSummon() != null)
				{
					player.getSummon().stopAllEffects();
				}
			}
			
			if (isBlockNoClassSkills())
			{
				final List<Integer> classSkills = SkillTreesParser.getInstance().getAllOriginalClassSkillIdTree(player.getClassId());
				player.getAllSkills().stream().filter(r -> (r != null) && !r.isItemSkill() && !classSkills.contains(r.getId())).forEach(i -> player.addBlockSkill(i, true));
			}
			
			player.store(true);
			player.sendPacket(new ShowTutorialMark(100, 0));
			
			for (final ItemInstance o : player.getInventory().getItems())
			{
				if (o != null)
				{
					if (o.isEquipable() && o.isEquipped() && o.isEventRestrictedItem())
					{
						final int slot = player.getInventory().getSlotFromItem(o);
						player.getInventory().unEquipItemInBodySlot(slot);
					}
				}
			}
			
			if (isUseEventItems() && addSaveLoc)
			{
				checkPlayerEventItems(player);
			}
			
			player.sendPacket(new CreatureSay(player.getObjectId(), Say2.ALL, player.getEventName(getId()), ServerStorage.getInstance().getString(player.getLang(), "FightEvents.NCHAT")));
			if (isTeamed())
			{
				player.sendPacket(new CreatureSay(player.getObjectId(), Say2.ALL, player.getEventName(getId()), ServerStorage.getInstance().getString(player.getLang(), "FightEvents.BCHAT")));
				player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, player.getEventName(getId()), ServerStorage.getInstance().getString(player.getLang(), "FightEvents.BCHAT")));
			}
		}
		
		if (!firstTime && Config.ALLOW_RESPAWN_PROTECT_PLAYER)
		{
			final Skill skill = SkillsParser.getInstance().getInfo(5576, 1);
			if (skill != null)
			{
				skill.getEffects(player, player, false);
			}
			player.setRespawnProtect();
			player.getPersonalTasks().addTask(new InvisibleTask(5000));
		}
		
		if (healAndRess)
		{
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			if (player.getSummon() != null && !player.getSummon().isDead())
			{
				final Summon pet = player.getSummon();
				pet.setCurrentHpMp(pet.getMaxHp(), pet.getMaxMp());
				pet.updateAndBroadcastStatus(1);
			}
			player.broadcastStatusUpdate();
			player.updateAndBroadcastStatus(1);
		}
		
		if (player.isMounted())
		{
			player.dismount();
		}
		
		if (player.getTransformationId() > 0)
		{
			player.untransform();
		}
		player.getEffectList().stopAllDebuffs();
		buffPlayer(player);
		player.broadcastUserInfo(true);
	}
	
	protected void checkPlayerEventItems(Player player)
	{
		final List<FightEventItem> itemList = getEventItems(player.getClassId().getId());
		if (itemList != null && !itemList.isEmpty())
		{
			player.getEventItems().clear();
			
			if (_checkItemSlots != null)
			{
				for (final int slot : _checkItemSlots)
				{
					final ItemInstance item = player.getInventory().getPaperdollItem(slot);
					if (item != null)
					{
						player.getInventory().unEquipItemInSlot(slot);
						final InventoryUpdate iu = new InventoryUpdate();
						iu.addModifiedItem(item);
						player.sendInventoryUpdate(iu);
						player.addEventItem(item);
					}
				}
			}
			
			for (final FightEventItem item : itemList)
			{
				if (item != null)
				{
					final ItemInstance createditem = player.getInventory().addItem("EventItem", item.getId(), item.getAmount(), player, null);
					if (item.getEnchantLvl() > 0)
					{
						createditem.setEnchantLevel(item.getEnchantLvl());
					}
					if (item.getAugmentation() != null)
					{
						createditem.setAugmentation(new Augmentation(item.getAugmentation().getAugmentationId()));
					}
					if (item.getElementals() != null)
					{
						for (final Elementals elm : item.getElementals())
						{
							createditem.setElementAttr(elm.getElement(), elm.getValue());
						}
					}
					
					if (createditem.isEquipable())
					{
						createditem.setIsEventItem(true);
						player.useEquippableItem(createditem.getObjectId(), true);
					}
				}
			}
			player.sendItemList(false);
			player.broadcastUserInfo(true);
		}
	}
	
	protected void removePlayerEventItems(Player player)
	{
		final List<FightEventItem> itemList = getEventItems(player.getClassId().getId());
		if (itemList != null && !itemList.isEmpty())
		{
			for (final var item : player.getInventory().getItems())
			{
				if (item != null && item.isEventItem() && item.isEquipable())
				{
					player.getInventory().unEquipEventItem(item);
					player.getInventory().destroyItemByObjectId(item.getObjectId(), item.getCount(), player, null);
				}
			}
		}
		
		if (!player.getEventItems().isEmpty())
		{
			for (final ItemInstance item : player.getEventItems())
			{
				if (item != null && item.isEquipable())
				{
					player.getInventory().equipItem(item);
				}
			}
		}
		player.sendItemList(false);
		player.broadcastUserInfo(true);
	}
	
	protected Location getSinglePlayerSpawnLocation(FightEventPlayer fPlayer)
	{
		Location[] spawns = null;
		Location loc = null;
		
		if (!isTeamed())
		{
			spawns = getMap().getPlayerSpawns();
		}
		else
		{
			loc = getTeamSpawn(fPlayer, true);
		}
		
		if (!isTeamed())
		{
			loc = getSafeLocation(spawns);
		}
		return Location.findPointToStay(loc, 0, CLOSE_LOCATIONS_VALUE / 2, true);
	}
	
	public void unregister(Player player)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player, REGISTERED_PLAYERS);
		player.removeEvent(this);
		removeObject(REGISTERED_PLAYERS, fPlayer);
		player.sendMessage((new ServerMessage("FightEvents.LONG_REGISTER", player.getLang())).toString());
	}
	
	public boolean leaveEvent(Player player, boolean teleportTown, boolean isDestroy, boolean isLogOut)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);
		
		if (fPlayer == null)
		{
			return true;
		}
		
		if (_state == EVENT_STATE.NOT_ACTIVE)
		{
			if (fPlayer.isInvisible())
			{
				stopInvisibility(player);
			}
			removeObject(FIGHTING_PLAYERS, fPlayer);
			if (isTeamed())
			{
				fPlayer.getTeam().removePlayer(fPlayer);
			}
			player.removeEvent(this);
			if (teleportTown)
			{
				teleportBackToTown(player);
			}
			else
			{
				if (player.isDead())
				{
					player.doRevive(100.);
				}
				player.setCanRevive(true);
			}
		}
		else
		{
			rewardPlayer(fPlayer, false, isLogOut);
			if (!isLogOut)
			{
				if (teleportTown)
				{
					setInvisible(player, TIME_TELEPORT_BACK_TOWN, false);
				}
				else
				{
					setInvisible(player, -1, false);
				}
			}
			removeObject(FIGHTING_PLAYERS, fPlayer);
			
			if (teleportTown)
			{
				player.stopAllEffects();
				if (!isLogOut)
				{
					startNewTimer(false, 0, "teleportBackSinglePlayerTimer", TIME_TELEPORT_BACK_TOWN, player);
				}
				else
				{
					teleportBackToTown(player);
				}
				player.sendPacket(new ExShowScreenMessage("", 10, ExShowScreenMessage.TOP_LEFT, false));
			}
			else
			{
				if (player.isDead())
				{
					player.doRevive(100.);
				}
				player.setCanRevive(true);
			}
			player.removeEvent(this);
		}
		
		hideScores(player);
		updateScreenScores();
		
		final List<FightEventPlayer> players = getPlayers(FIGHTING_PLAYERS, REGISTERED_PLAYERS);
		if ((players.isEmpty() || players.size() < 2) && !isGlobal() && isDestroy)
		{
			ThreadPoolManager.getInstance().schedule(() ->
			{
				stopEvent();
			}, (15 + TIME_TELEPORT_BACK_TOWN) * 1000);
		}

		if (player.isRooted())
		{
			player.startRooted(false);
			if (player.hasSummon())
			{
				player.getSummon().startRooted(false);
			}
		}
		
		final Effect eInvis = player.getFirstEffect(EffectType.INVINCIBLE);
		if (eInvis != null)
		{
			eInvis.exit();
		}
		player.startHealBlocked(false);
		player.setIsInvul(false);
		player.getInventory().checkRuneSkills();
		if (player.getParty() != null)
		{
			player.getParty().removePartyMember(player, messageType.Expelled);
		}
		
		if (isUseEventItems())
		{
			removePlayerEventItems(player);
		}
		return true;
	}
	
	protected void loggedOut(Player player)
	{
		leaveEvent(player, true, true, true);
	}
	
	protected void teleportBackToTown(Player player)
	{
		if (player.isPolymorphed())
		{
			player.setPolyId(0);
		}
		
		Location loc = null;
		if (player.getSaveLoc() != null)
		{
			loc = player.getSaveLoc();
		}
		else
		{
			loc = Location.findPointToStay(new Location(83208, 147672, -3494, 0), 0, 100, true);
		}
		
		if (player.isDead())
		{
			player.doRevive(100.);
			player.getPersonalTasks().addTask(new TeleportTask(1000, loc, ReflectionManager.DEFAULT));
		}
		else
		{
			player.teleToLocation(loc, true, ReflectionManager.DEFAULT);
		}
		player.setCanRevive(true);
		player.cleanBlockSkills(true);
		if (isGlobal())
		{
			player.setFightEventGameRoom(null);
		}
		if (isInstanced())
		{
			if (_reflection != null && _reflection.containsPlayer(player.getObjectId()))
			{
				_reflection.removePlayer(player.getObjectId());
			}
		}
	}
	
	protected void rewardPlayer(FightEventPlayer fPlayer, boolean isTopKiller, boolean isLogOut)
	{
		if (fPlayer != null)
		{
			giveItemRewardsForPlayer(fPlayer, null, isTopKiller, isLogOut);
		}
	}
	
	@Nullable
	private FightEventPlayer[] getTopKillers()
	{
		if (_rewardByTopKiller == null || _rewardByTopKiller.length == 0)
		{
			return null;
		}
		
		if (_teamed)
		{
			final FightEventPlayer[] topKillers = new FightEventPlayer[_teams.size()];
			final int[] topKillersKills = new int[_teams.size()];
		
			int teamIndex = 0;
			for (final FightEventTeam team : _teams)
			{
				for (final FightEventPlayer fPlayer : team.getPlayers())
				{
					if (fPlayer != null)
					{
						if (fPlayer.getKills() == topKillersKills[teamIndex])
						{
							topKillers[teamIndex] = null;
						}
						else if (fPlayer.getKills() > topKillersKills[teamIndex])
						{
							topKillers[teamIndex] = fPlayer;
							topKillersKills[teamIndex] = fPlayer.getKills();
						}
					}
				}
				teamIndex++;
			}
			return topKillers;
		}
		else
		{
			final FightEventPlayer[] topKillers = new FightEventPlayer[1];
			int topKillersKills = 0;
			
			for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			{
				if (iFPlayer != null)
				{
					if (iFPlayer.getKills() > topKillersKills)
					{
						topKillers[0] = null;
						topKillers[0] = iFPlayer;
						topKillersKills = iFPlayer.getKills();
					}
				}
			}
			return topKillers;
		}
	}
	
	protected void announceWinnerTeam(boolean wholeEvent, FightEventTeam winnerOfTheRound)
	{
		int bestScore = -1;
		FightEventTeam bestTeam = null;
		boolean draw = false;
		if (wholeEvent)
		{
			for (final FightEventTeam team : getTeams())
			{
				if (team.getScore() > bestScore)
				{
					draw = false;
					bestScore = team.getScore();
					bestTeam = team;
				}
				else if (team.getScore() == bestScore)
				{
					draw = true;
				}
			}
		}
		else
		{
			bestTeam = winnerOfTheRound;
		}
		
		if (!draw)
		{
			for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			{
				final ServerMessage msg = wholeEvent ? new ServerMessage("FightEvents.WE_WON_EVENT", iFPlayer.getPlayer().getLang()) : new ServerMessage("FightEvents.WE_WON_ROUND", iFPlayer.getPlayer().getLang());
				if (wholeEvent)
				{
					msg.add(iFPlayer.getPlayer().getEventName(getId()));
				}
				iFPlayer.getPlayer().sendPacket(new CreatureSay(0, Say2.PARTYROOM_COMMANDER, new ServerMessage("FightEvents." + bestTeam.getName() + "", iFPlayer.getPlayer().getLang()).toString(), msg.toString()));
				iFPlayer.getPlayer().getListeners().onEventFinish(getId(), iFPlayer.getTeam() == bestTeam);
			}
		}
		else
		{
			getPlayers(FIGHTING_PLAYERS).stream().filter(p -> (p != null)).forEach(p -> p.getPlayer().getListeners().onEventFinish(getId(), false));
		}
		updateScreenScores();
	}
	
	protected void announceWinnerPlayer(boolean wholeEvent, FightEventPlayer winnerOfTheRound)
	{
		int bestScore = -1;
		FightEventPlayer bestPlayer = null;
		boolean draw = false;
		if (wholeEvent)
		{
			for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			{
				if (iFPlayer.getPlayer() != null && iFPlayer.getPlayer().isOnline())
				{
					if (iFPlayer.getScore() > bestScore)
					{
						bestScore = iFPlayer.getScore();
						bestPlayer = iFPlayer;
					}
					else if (iFPlayer.getScore() == bestScore)
					{
						draw = true;
					}
				}
			}
		}
		else
		{
			bestPlayer = winnerOfTheRound;
		}
		
		if (!draw && bestPlayer != null)
		{
			for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			{
				final ServerMessage msg = wholeEvent ? new ServerMessage("FightEvents.I_WON_EVENT", iFPlayer.getPlayer().getLang()) : new ServerMessage("FightEvents.I_WON_ROUND", iFPlayer.getPlayer().getLang());
				if (wholeEvent)
				{
					msg.add(iFPlayer.getPlayer().getEventName(getId()));
				}
				iFPlayer.getPlayer().sendPacket(new CreatureSay(0, Say2.PARTYROOM_COMMANDER, bestPlayer.getPlayer().getName(null), msg.toString()));
				iFPlayer.getPlayer().getListeners().onEventFinish(getId(), iFPlayer == bestPlayer);
			}
			
			if (isGetHeroStatus() && getId() != 1)
			{
				bestPlayer.getPlayer().setHero(true, isGetHeroSkills());
			}
		}
		else
		{
			getPlayers(FIGHTING_PLAYERS).stream().filter(p -> (p != null)).forEach(p -> p.getPlayer().getListeners().onEventFinish(getId(), false));
		}
		updateScreenScores();
	}
	
	protected void updateScreenScores()
	{
		getPlayers(FIGHTING_PLAYERS).stream().filter(p -> (p != null)).forEach(p -> p.getPlayer().sendPacket(new ExShowScreenMessage(getScreenScores(p.getPlayer(), inScreenShowBeScoreNotKills(), inScreenShowBeTeamNotInvidual()).toString(), 600000, ExShowScreenMessage.TOP_LEFT, false)));
	}
	
	protected void updateScreenScores(Player player)
	{
		if (getFightEventPlayer(player) != null)
		{
			final String msg = getScreenScores(player, inScreenShowBeScoreNotKills(), inScreenShowBeTeamNotInvidual());
			player.sendPacket(new ExShowScreenMessage(msg.toString(), 600000, ExShowScreenMessage.TOP_LEFT, false));
		}
	}
	
	protected String getScorePlayerName(FightEventPlayer fPlayer)
	{
		return new StringBuilder().append(fPlayer.getPlayer().getName(null)).append(isTeamed() ? new StringBuilder().append(" (").append("" + ServerStorage.getInstance().getString(fPlayer.getPlayer().getLang(), "FightEvents." + fPlayer.getTeam().getName() + "") + "").append(")").toString() : "").toString();
	}
	
	protected void updatePlayerScore(FightEventPlayer fPlayer)
	{
		_scores.put(getScorePlayerName(fPlayer), Integer.valueOf(fPlayer.getKills()));
		_scoredUpdated = true;
		
		if (!isTeamed())
		{
			updateScreenScores();
		}
	}
	
	protected void showScores(Creature c)
	{
		final Map<String, Integer> scores = getBestScores();
		if (scores == null || scores.isEmpty())
		{
			return;
		}
		
		final FightEventPlayer fPlayer = getFightEventPlayer(c);
		if (fPlayer != null)
		{
			fPlayer.setShowRank(true);
		}
		c.sendPacket(new ExPVPMatchCCRecord(scores, 0));
	}
	
	protected void hideScores(Creature c)
	{
		c.sendPacket(ExPVPMatchCCRetire.STATIC);
	}
	
	protected void handleAfk(FightEventPlayer fPlayer, boolean setAsAfk)
	{
		final Player player = fPlayer.getPlayer();
		
		if (setAsAfk)
		{
			fPlayer.setAfk(true);
			fPlayer.setAfkStartTime(player.getLastNotAfkTime());
			
			sendMessageToPlayer(player, MESSAGE_TYPES.CRITICAL, new ServerMessage("FightEvents.YOU_AFK", player.getLang()));
		}
		else if (fPlayer.isAfk())
		{
			int totalAfkTime = (int) ((System.currentTimeMillis() - fPlayer.getAfkStartTime()) / 1000);
			totalAfkTime -= TIME_TO_BE_AFK;
			if (totalAfkTime > 5)
			{
				fPlayer.setAfk(false);
				
				fPlayer.addTotalAfkSeconds(totalAfkTime);
				final ServerMessage msg = new ServerMessage("FightEvents.WAS_AFK_SEC", player.getLang());
				msg.add(totalAfkTime);
				sendMessageToPlayer(player, MESSAGE_TYPES.CRITICAL, msg);
			}
		}
	}
	
	protected void setInvisible(Player player, int seconds, boolean sendMessages)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);
		fPlayer.setInvisible(true);
		player.setInvisible(true);
		player.startAbnormalEffect(AbnormalEffect.STEALTH);
		player.broadcastUserInfo(true);
		if (seconds > 0)
		{
			startNewTimer(false, 0, "setInvisible", seconds, fPlayer, sendMessages);
		}
	}
	
	protected void stopInvisibility(Player player)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);
		
		if (fPlayer != null)
		{
			fPlayer.setInvisible(false);
		}
		
		player.setInvisible(false);
		player.stopAbnormalEffect(AbnormalEffect.STEALTH);
		player.updateAndBroadcastStatus(1);
		if (player.getSummon() != null)
		{
			player.getSummon().updateAndBroadcastStatus(0);
		}
	}
	
	protected void rootPlayer(Player player)
	{
		if (!isRootBetweenRounds())
		{
			return;
		}
		
		player.startRooted(true);
		if (player.hasSummon())
		{
			player.getSummon().startRooted(true);
		}
	}
	
	protected void unrootPlayers()
	{
		if (!isRootBetweenRounds())
		{
			return;
		}
		
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			final Player player = iFPlayer.getPlayer();
			if (player != null)
			{
				player.startRooted(false);
				if (player.hasSummon())
				{
					player.getSummon().startRooted(false);
				}
			}
		}
	}
	
	protected void ressAndHealPlayers()
	{
		for (final FightEventPlayer fPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			final Player player = fPlayer.getPlayer();
			
			if (player.isDead())
			{
				player.doRevive(100.);
			}
			buffPlayer(player);
		}
	}
	
	protected int getWarRelation()
	{
		int result = 0;
		
		result |= RelationChanged.RELATION_CLAN_MEMBER;
		result |= RelationChanged.RELATION_1SIDED_WAR;
		result |= RelationChanged.RELATION_MUTUAL_WAR;
		
		return result;
	}
	
	protected int getFriendRelation()
	{
		int result = 0;
		
		result |= RelationChanged.RELATION_CLAN_MEMBER;
		result |= RelationChanged.RELATION_CLAN_MATE;
		
		return result;
	}
	
	protected Npc chooseLocAndSpawnNpc(int id, Location[] locs, int respawnInSeconds, boolean findPos)
	{
		return spawnNpc(id, getSafeLocation(locs), respawnInSeconds, findPos);
	}
	
	protected Npc spawnNpc(int id, Location loc, int respawnInSeconds, boolean findPos)
	{
		Npc npc = null;
		final NpcTemplate template = NpcsParser.getInstance().getTemplate(id);
		try
		{
			Location location = null;
			if (findPos)
			{
				location = Location.findPointToStay(loc, 0, CLOSE_LOCATIONS_VALUE / 2, true);
			}
			else
			{
				location = loc;
			}
			final Spawner spawn = new Spawner(template);
			spawn.setX(location.getX());
			spawn.setY(location.getY());
			spawn.setZ(location.getZ());
			spawn.setHeading(location.getHeading());
			spawn.setReflection(getReflection());
			spawn.setAmount(1);
			spawn.setRespawnDelay(Math.max(0, respawnInSeconds));
			if (respawnInSeconds <= 0)
			{
				spawn.stopRespawn();
			}
			SpawnParser.getInstance().addNewSpawn(spawn);
			spawn.init();
			npc = spawn.getLastSpawn();
		}
		catch (final Exception e)
		{
			warn(e.getMessage());
		}
		return npc;
	}
	
	protected static ServerMessage getFixedTime(Player player, int seconds)
	{
		final int minutes = seconds / 60;
		final ServerMessage msg = new ServerMessage("FightEvents.FIX_TIME", player.getLang());
		if (seconds >= 60)
		{
			msg.add(minutes);
			msg.add(minutes > 1 ? minutes < 5 ? new ServerMessage("FightEvents.CHECK_MIN2", player.getLang()).toString() : new ServerMessage("FightEvents.CHECK_MIN3", player.getLang()).toString() : new ServerMessage("FightEvents.CHECK_MIN1", player.getLang()).toString());
		}
		else
		{
			msg.add(seconds);
			msg.add(seconds > 1 ? seconds < 5 ? new ServerMessage("FightEvents.CHECK_SEC2", player.getLang()).toString() : new ServerMessage("FightEvents.CHECK_SEC3", player.getLang()).toString() : new ServerMessage("FightEvents.CHECK_SEC1", player.getLang()).toString());
		}
		return msg;
	}
	
	private void buffPlayer(Player player)
	{
		if (getBuffer())
		{
			int[][] buffs;
			if (player.isMageClass())
			{
				buffs = _mageBuffs;
			}
			else
			{
				buffs = _fighterBuffs;
			}
			
			if (buffs != null)
			{
				giveBuffs(player, buffs, false);
				if (player.getSummon() != null)
				{
					giveBuffs(player, _fighterBuffs, true);
				}
			}
		}
	}
	
	private static void giveBuffs(final Player player, int[][] buffs, boolean petbuff)
	{
		Skill buff;
		for (final int[] buff1 : buffs)
		{
			buff = SkillsParser.getInstance().getInfo(buff1[0], buff1[1]);
			if (buff == null)
			{
				continue;
			}
			
			if (!petbuff)
			{
				buff.getEffects(player, player, false);
			}
			else
			{
				if (player.hasSummon())
				{
					buff.getEffects(player, player.getSummon(), false);
				}
			}
		}
		
		ThreadPoolManager.getInstance().schedule(() ->
		{
			if (!petbuff)
			{
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
			}
			else
			{
				if (player.hasSummon())
				{
					player.getSummon().setCurrentHp(player.getSummon().getMaxHp());
					player.getSummon().setCurrentMp(player.getSummon().getMaxMp());
					player.getSummon().setCurrentCp(player.getSummon().getMaxCp());
				}
			}
		}, 1000);
	}
	
	private void announceTopKillers(FightEventPlayer[] topKillers)
	{
		if (topKillers == null)
		{
			return;
		}
		
		for (final FightEventPlayer fPlayer : topKillers)
		{
			if (fPlayer != null)
			{
				for (final Player player : GameObjectsStorage.getPlayers())
				{
					final ServerMessage message = new ServerMessage("FightEvents.MOST_KILL", player.getLang());
					message.add(fPlayer.getPlayer().getName(null));
					message.add(player.getEventName(getId()));
					player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, player.getEventName(getId()), message.toString()));
				}
			}
		}
	}
	
	public enum MESSAGE_TYPES
	{
		GM, NORMAL_MESSAGE, SCREEN_BIG, SCREEN_SMALL, CRITICAL
	}
	
	protected void sendMessageWithCheckRound(AbstractFightEvent event, MESSAGE_TYPES type, int secondsLeft)
	{
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			if (iFPlayer != null)
			{
				ServerMessage msg;
				if (event.isRoundEvent())
				{
					msg = ((event.getCurrentRound() + 1) == event.getTotalRounds() ? new ServerMessage("FightEvents.GOING_START_LAST_ROUND", iFPlayer.getPlayer().getLang()) : new ServerMessage(ROUND_NUMBER_IN_STRING[event.getCurrentRound() + 1], iFPlayer.getPlayer().getLang()));
				}
				else
				{
					msg = new ServerMessage("FightEvents.GOING_START_MATCH", iFPlayer.getPlayer().getLang());
				}
				msg.add(getFixedTime(iFPlayer.getPlayer(), secondsLeft).toString());
				sendMessageToPlayer(iFPlayer.getPlayer(), type, msg);
			}
		}
	}
	
	protected void sendMessageWithCheckDestroy(AbstractFightEvent event, MESSAGE_TYPES type, int secondsLeft)
	{
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			if (iFPlayer != null)
			{
				final ServerMessage msg = new ServerMessage("FightEvents.CHANGE_MAP_MATCH", iFPlayer.getPlayer().getLang());
				msg.add(getFixedTime(iFPlayer.getPlayer(), secondsLeft).toString());
				sendMessageToPlayer(iFPlayer.getPlayer(), type, msg);
			}
		}
	}
	
	protected void sendMessageToTeam(FightEventTeam team, MESSAGE_TYPES type, String msg)
	{
		team.getPlayers().stream().filter(p -> (p != null)).forEach(p -> sendMessageToPlayer(p.getPlayer(), type, new ServerMessage(msg, p.getPlayer().getLang())));
	}
	
	protected void sendMessageToTeam(FightEventTeam team, MESSAGE_TYPES type, String msg, FightEventTeam flagTeam)
	{
		for (final FightEventPlayer iFPlayer : team.getPlayers())
		{
			final ServerMessage message = new ServerMessage(msg, iFPlayer.getPlayer().getLang());
			message.add(new ServerMessage("FightEvents." + flagTeam.getName() + "", iFPlayer.getPlayer().getLang()).toString());
			sendMessageToPlayer(iFPlayer.getPlayer(), type, message);
		}
	}
	
	protected void sendMessageToFighting(MESSAGE_TYPES type, String msg, boolean skipJustTeleported)
	{
		getPlayers(FIGHTING_PLAYERS).stream().filter(p -> (p != null) && (!skipJustTeleported || !p.isInvisible())).forEach(p -> sendMessageToPlayer(p.getPlayer(), type, new ServerMessage(msg, p.getPlayer().getLang())));
	}
	
	protected void sendMessageToFighting(MESSAGE_TYPES type, String msg, boolean skipJustTeleported, String value)
	{
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			if (!skipJustTeleported || !iFPlayer.isInvisible())
			{
				final ServerMessage message = new ServerMessage(msg, iFPlayer.getPlayer().getLang());
				message.add(value);
				sendMessageToPlayer(iFPlayer.getPlayer(), type, message);
			}
		}
	}
	
	protected void sendMessageToFighting(MESSAGE_TYPES type, String msg, boolean skipJustTeleported, int secondsLeft)
	{
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			if (!skipJustTeleported || !iFPlayer.isInvisible())
			{
				final ServerMessage message = new ServerMessage(msg, iFPlayer.getPlayer().getLang());
				message.add(getFixedTime(iFPlayer.getPlayer(), secondsLeft).toString());
				sendMessageToPlayer(iFPlayer.getPlayer(), type, message);
			}
		}
	}
	
	protected void sendMessageToRegistered(MESSAGE_TYPES type, String msg)
	{
		getPlayers(REGISTERED_PLAYERS).stream().filter(p -> (p != null)).forEach(p -> sendMessageToPlayer(p.getPlayer(), type, new ServerMessage(msg, p.getPlayer().getLang())));
	}
	
	protected void sendMessageToRegistered(MESSAGE_TYPES type, String msg, int secondsLeft)
	{
		for (final FightEventPlayer iFPlayer : getPlayers(REGISTERED_PLAYERS))
		{
			final ServerMessage message = new ServerMessage(msg, iFPlayer.getPlayer().getLang());
			message.add(getFixedTime(iFPlayer.getPlayer(), secondsLeft).toString());
			sendMessageToPlayer(iFPlayer.getPlayer(), type, message);
		}
	}
	
	protected void sendMessageToPlayer(Player player, MESSAGE_TYPES type, ServerMessage msg)
	{
		switch (type)
		{
			case GM :
				player.sendPacket(new CreatureSay(player.getObjectId(), Say2.CRITICAL_ANNOUNCE, player.getName(null), msg.toString()));
				updateScreenScores(player);
				break;
			case NORMAL_MESSAGE :
				player.sendMessage(msg.toString());
				break;
			case SCREEN_BIG :
				player.sendPacket(new ExShowScreenMessage(msg.toString(), 3000, ExShowScreenMessage.TOP_CENTER, true));
				updateScreenScores(player);
				break;
			case SCREEN_SMALL :
				player.sendPacket(new ExShowScreenMessage(msg.toString(), 600000, ExShowScreenMessage.TOP_LEFT, false));
				break;
			case CRITICAL :
				player.sendPacket(new CreatureSay(player.getObjectId(), Say2.PARTYROOM_COMMANDER, player.getName(null), msg.toString()));
				updateScreenScores(player);
				break;
		}
	}
	
	public void setState(EVENT_STATE state)
	{
		_state = state;
	}
	
	public EVENT_STATE getState()
	{
		return _state;
	}
	
	public String getIcon()
	{
		return _icon;
	}
	
	public boolean isAutoTimed()
	{
		return _isAutoTimed;
	}
	
	public SchedulingPattern getAutoStartTimes()
	{
		return _autoStartTimes;
	}
	
	public FightEventMap getMap()
	{
		return _map;
	}
	
	public boolean isTeamed()
	{
		return _teamed;
	}

	public boolean canTeamTarget()
	{
		return _teamTargets;
	}
	
	public boolean givePvpPoints()
	{
		return _givePvpPoints;
	}
	
	public boolean canAttackPlayers()
	{
		return _attackPlayers;
	}

	public boolean canUseScrolls()
	{
		return _useScrolls;
	}

	public boolean canUsePotions()
	{
		return _usePotions;
	}

	public boolean canUseItemSummons()
	{
		return _useItemSummon;
	}
	
	public boolean canUseCommunity()
	{
		return _useCommunity;
	}

	protected boolean isInstanced()
	{
		return _instanced;
	}
	
	public Reflection getReflection()
	{
		return _reflection;
	}
	
	public int getRoundRuntime()
	{
		return _roundRunTime;
	}
	
	public int getRespawnTime()
	{
		return _respawnTime;
	}
	
	public boolean isRoundEvent()
	{
		return _roundEvent;
	}
	
	public int getTotalRounds()
	{
		return _rounds;
	}
	
	public int getCurrentRound()
	{
		return _currentRound;
	}
	
	public boolean getBuffer()
	{
		return _buffer;
	}
	
	protected boolean isRootBetweenRounds()
	{
		return _rootBetweenRounds;
	}
	
	public boolean isLastRound()
	{
		return !isRoundEvent() || getCurrentRound() == getTotalRounds();
	}
	
	protected List<FightEventTeam> getTeams()
	{
		return _teams;
	}
	
	public MultiValueSet<String> getSet()
	{
		return _set;
	}
	
	public FightEventManager.CLASSES[] getExcludedClasses()
	{
		return _excludedClasses;
	}
	
	protected int getTeamTotalKills(FightEventTeam team)
	{
		if (!isTeamed())
		{
			return 0;
		}
		int totalKills = 0;
		for (final FightEventPlayer iFPlayer : team.getPlayers())
		{
			totalKills += iFPlayer.getKills();
		}
		
		return totalKills;
	}
	
	public int getPlayersCount(String... groups)
	{
		return getPlayers(groups).size();
	}
	
	public List<FightEventPlayer> getPlayers(String... groups)
	{
		if (groups.length == 1)
		{
			final List<FightEventPlayer> fPlayers = getObjects(groups[0]);
			return fPlayers;
		}
		else
		{
			final List<FightEventPlayer> newList = new ArrayList<>();
			for (final String group : groups)
			{
				final List<FightEventPlayer> fPlayers = getObjects(group);
				newList.addAll(fPlayers);
			}
			return newList;
		}
	}
	
	public List<Player> getAllFightingPlayers()
	{
		final List<FightEventPlayer> fPlayers = getPlayers(FIGHTING_PLAYERS);
		final List<Player> players = new ArrayList<>(fPlayers.size());
		fPlayers.stream().filter(p -> (p != null)).forEach(p -> players.add(p.getPlayer()));
		return players;
	}
	
	public List<Player> getMyTeamFightingPlayers(Player player)
	{
		final FightEventTeam fTeam = getFightEventPlayer(player).getTeam();
		final List<FightEventPlayer> fPlayers = getPlayers(FIGHTING_PLAYERS);
		final List<Player> players = new ArrayList<>(fPlayers.size());
		
		if (!isTeamed())
		{
			player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, player.getEventName(getId()), new ServerMessage("FightEvents.NO_TEAMS", player.getLang()).toString()));
			players.add(player);
		}
		else
		{
			fPlayers.stream().filter(p -> (p != null) && p.getTeam().equals(fTeam)).forEach(p -> players.add(p.getPlayer()));
		}
		return players;
	}
	
	public FightEventPlayer getFightEventPlayer(Creature creature)
	{
		return getFightEventPlayer(creature, FIGHTING_PLAYERS);
	}

	public FightEventPlayer getFightEventPlayer(Creature creature, String... groups)
	{
		if (creature == null || !creature.isPlayable())
		{
			return null;
		}
		
		final int lookedPlayerId = creature.getActingPlayer().getObjectId();
		
		for (final FightEventPlayer iFPlayer : getPlayers(groups))
		{
			if (iFPlayer.getPlayer().getObjectId() == lookedPlayerId)
			{
				return iFPlayer;
			}
		}
		return null;
	}
	
	protected void spreadIntoTeamsAndPartys()
	{
		_teams.clear();
		for (int i = 0; i < _room.getTeamsCount(); i++)
		{
			_teams.add(new FightEventTeam(i + 1));
		}
		
		int index = 0;
		for (final Player player : _room.getAllPlayers())
		{
			final FightEventPlayer fPlayer = getFightEventPlayer(player, REGISTERED_PLAYERS);
			if (fPlayer == null)
			{
				continue;
			}
			
			final FightEventTeam team = _teams.get(index % _room.getTeamsCount());
			fPlayer.setTeam(team);
			team.addPlayer(fPlayer);
			
			index++;
		}
		
		for (final FightEventTeam team : _teams)
		{
			final List<List<Player>> partys = spreadTeamInPartys(team);
			partys.stream().filter(p -> (p != null)).forEach(p -> createParty(p));
		}
	}

	protected List<List<Player>> spreadTeamInPartys(FightEventTeam team)
	{
		final Map<FightEventManager.CLASSES, List<Player>> classesMap = new HashMap<>();
		for (final FightEventManager.CLASSES clazz : FightEventManager.CLASSES.values())
		{
			classesMap.put(clazz, new ArrayList<>());
		}
		
		for (final FightEventPlayer iFPlayer : team.getPlayers())
		{
			final Player player = iFPlayer.getPlayer();
			final FightEventManager.CLASSES clazz = FightEventGameRoom.getPlayerClassGroup(player);
			if (clazz != null)
			{
				classesMap.get(clazz).add(player);
			}
			else
			{
				warn("Problem with add player - " + player.getName(null));
				warn("Class - " + player.getClassId().name() + " null for event!");
			}
		}
		
		final int partyCount = (int) Math.ceil(team.getPlayers().size() / 9);
		
		final List<List<Player>> partys = new ArrayList<>();
		for (int i = 0; i < partyCount; i++)
		{
			partys.add(new ArrayList<>());
		}
		
		if (partyCount == 0)
		{
			return partys;
		}
		
		int finishedOnIndex = 0;
		for (final Entry<FightEventManager.CLASSES, List<Player>> clazzEntry : classesMap.entrySet())
		{
			for (final Player player : clazzEntry.getValue())
			{
				partys.get(finishedOnIndex).add(player);
				finishedOnIndex++;
				if (finishedOnIndex == partyCount)
				{
					finishedOnIndex = 0;
				}
			}
		}
		
		return partys;
	}
	
	protected void createParty(List<Player> listOfPlayers)
	{
		if (listOfPlayers.size() <= 1)
		{
			return;
		}
		
		Party newParty = null;
		for (final Player player : listOfPlayers)
		{
			if (player.getParty() != null)
			{
				player.getParty().removePartyMember(player, messageType.Expelled);
			}
			
			if (newParty == null)
			{
				player.setParty(newParty = new Party(player, Party.ITEM_ORDER_SPOIL));
			}
			else
			{
				player.joinParty(newParty);
			}
		}
	}
	
	private void createInstance(List<Integer> doors, Map<String, ZoneType> zones)
	{
		_reflection = ReflectionManager.getInstance().createRef();
		_reflection.setPvPInstance(true);
		if (doors != null && !doors.isEmpty())
		{
			doors.stream().filter(d -> (d != 0)).forEach(d -> _reflection.addEventDoor(d, new StatsSet()));
		}
	}

	private Location getSafeLocation(Location[] locations)
	{
		Location safeLoc = null;
		int checkedCount = 0;
		boolean isOk = false;
		
		while (!isOk)
		{
			safeLoc = Rnd.get(locations);
			isOk = nobodyIsClose(safeLoc);
			checkedCount++;
			
			if (checkedCount > locations.length * 2)
			{
				isOk = true;
			}
		}
		
		return safeLoc;
	}
	
	protected Location getTeamSpawn(FightEventPlayer fPlayer, boolean randomNotClosestToPt)
	{
		final FightEventTeam team = fPlayer.getTeam();
		final Location[] spawnLocs = getMap().getTeamSpawns().get(team.getIndex());
		
		if (randomNotClosestToPt || _state != EVENT_STATE.STARTED)
		{
			return Rnd.get(spawnLocs);
		}
		else
		{
			List<Player> playersToCheck = new ArrayList<>();
			if (fPlayer.getParty() != null)
			{
				playersToCheck = fPlayer.getParty().getMembers();
			}
			else
			{
				for (final FightEventPlayer iFPlayer : team.getPlayers())
				{
					playersToCheck.add(iFPlayer.getPlayer());
				}
			}
			
			final Map<Location, Integer> spawnLocations = new HashMap<>(spawnLocs.length);
			for (final Location loc : spawnLocs)
			{
				spawnLocations.put(loc, 0);
			}
			
			for (final Player player : playersToCheck)
			{
				if (player != null && player.isOnline() && !player.isDead())
				{
					Location winner = null;
					double winnerDist = -1;
					for (final Location loc : spawnLocs)
					{
						if (winnerDist <= 0 || winnerDist < player.getDistance(loc))
						{
							winner = loc;
							winnerDist = player.getDistance(loc);
						}
					}
					
					if (winner != null)
					{
						spawnLocations.put(winner, spawnLocations.get(winner) + 1);
					}
				}
			}
			
			Location winner = null;
			double points = -1;
			for (final Entry<Location, Integer> spawn : spawnLocations.entrySet())
			{
				if (points < spawn.getValue())
				{
					winner = spawn.getKey();
					points = spawn.getValue();
				}
			}
			
			if (points <= 0)
			{
				return Rnd.get(spawnLocs);
			}
			return winner;
		}
	}
	
	protected boolean isPlayerActive(Player player)
	{
		if (player == null)
		{
			return false;
		}
		if (player.isDead())
		{
			return false;
		}
		if (player.getReflectionId() != _reflection.getId())
		{
			return false;
		}
		if (System.currentTimeMillis() - player.getLastNotAfkTime() > 120000)
		{
			return false;
		}

		boolean insideZone = false;
		for (final ZoneType zone : _activeZones.values())
		{
			if (zone.isInsideZone(player.getX(), player.getY(), player.getZ()))
			{
				insideZone = true;
			}
		}
		if (!insideZone)
		{
			return false;
		}
		return true;
	}
	
	private void giveRewards(FightEventPlayer[] topKillers)
	{
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			if (iFPlayer != null)
			{
				iFPlayer.getPlayer().getCounters().addAchivementInfo("eventsParticipate", 0, -1, false, false, false);
				rewardPlayer(iFPlayer, Util.arrayContains(topKillers, iFPlayer), false);
				if (Config.ALLOW_DAILY_TASKS)
				{
					if (iFPlayer.getPlayer().getActiveDailyTasks() != null)
					{
						for (final PlayerTaskTemplate taskTemplate : iFPlayer.getPlayer().getActiveDailyTasks())
						{
							if (taskTemplate.getType().equalsIgnoreCase("Event") && !taskTemplate.isComplete())
							{
								final DailyTaskTemplate task = DailyTaskManager.getInstance().getDailyTask(taskTemplate.getId());
								if (taskTemplate.getCurrentEventsCount() < task.getEventsCount())
								{
									taskTemplate.setCurrentEventsCount((taskTemplate.getCurrentEventsCount() + 1));
									if (taskTemplate.isComplete())
									{
										final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler("missions");
										if (vch != null)
										{
											iFPlayer.getPlayer().updateDailyStatus(taskTemplate);
											vch.useVoicedCommand("missions", iFPlayer.getPlayer(), null);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void showLastAFkMessage()
	{
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			final int minutesAFK = (int) Math.round((double) iFPlayer.getTotalAfkSeconds() / 60);
			final int badgesDecreased = -minutesAFK * ITEMS_FOR_MINUTE_OF_AFK;
			if (badgesDecreased > 0)
			{
				final ServerMessage msg = new ServerMessage("FightEvents.DECREASED", iFPlayer.getPlayer().getLang());
				msg.add(badgesDecreased);
				sendMessageToPlayer(iFPlayer.getPlayer(), MESSAGE_TYPES.NORMAL_MESSAGE, msg);
			}
		}
	}
	
	private Map<String, Integer> getBestScores()
	{
		if (!_scoredUpdated)
		{
			return _bestScores;
		}
		
		if (_scores.isEmpty())
		{
			return null;
		}

		final List<Integer> points = new ArrayList<>(_scores.values());
		Collections.sort(points);
		Collections.reverse(points);

		int cap;
		if (points.size() <= 26)
		{
			cap = points.get(points.size() - 1).intValue();
		}
		else
		{
			cap = points.get(25).intValue();
		}
		final Map<String, Integer> finalResult = new LinkedHashMap<>();
		final List<Entry<String, Integer>> toAdd = new ArrayList<>();
		for (final Entry<String, Integer> i : _scores.entrySet())
		{
			if (i.getValue().intValue() > cap && finalResult.size() < 25)
			{
				toAdd.add(i);
			}
		}

		if (finalResult.size() < 25)
		{
			for (final Entry<String, Integer> i : _scores.entrySet())
			{
				if (i.getValue().intValue() == cap)
				{
					toAdd.add(i);
					if (finalResult.size() == 25)
					{
						break;
					}
				}
			}
		}

		for (int i = 0; i < toAdd.size(); i++)
		{
			Entry<String, Integer> biggestEntry = null;
			for (final Entry<String, Integer> entry : toAdd)
			{
				if (!finalResult.containsKey(entry.getKey()) && (biggestEntry == null || entry.getValue().intValue() > biggestEntry.getValue().intValue()))
				{
					biggestEntry = entry;
				}
			}
			if (biggestEntry != null)
			{
				finalResult.put(biggestEntry.getKey(), biggestEntry.getValue());
			}
		}

		_bestScores = finalResult;
		_scoredUpdated = false;

		return finalResult;
	}

	private void updateEveryScore()
	{
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			_scores.put(getScorePlayerName(iFPlayer), iFPlayer.getKills());
			_scoredUpdated = true;
		}
	}
	
	private String getScreenScores(Player player, boolean showScoreNotKills, boolean teamPointsNotInvidual)
	{
		String msg = "";
		if (isTeamed() && teamPointsNotInvidual)
		{
			final List<FightEventTeam> teams = getTeams();
			Collections.sort(teams, new BestTeamComparator(showScoreNotKills));
			for (final FightEventTeam team : teams)
			{
				msg = new StringBuilder().append(msg).append(ServerStorage.getInstance().getString(player.getLang(), "FightEvents." + team.getName() + "")).append(": ").append(showScoreNotKills ? team.getScore() : getTeamTotalKills(team)).append(" ").append(showScoreNotKills ? "" + ServerStorage.getInstance().getString(player.getLang(), "FightEvents.POINTS") + "" : "" + ServerStorage.getInstance().getString(player.getLang(), "FightEvents.KILLS") + "").append("\n").toString();
			}
		}
		else
		{
			final List<FightEventPlayer> fPlayers = getPlayers(FIGHTING_PLAYERS);
			final List<FightEventPlayer> changedFPlayers = new ArrayList<>(fPlayers.size());
			changedFPlayers.addAll(fPlayers);
			
			Collections.sort(changedFPlayers, new BestPlayerComparator(showScoreNotKills));
			final int max = Math.min(10, changedFPlayers.size());
			for (int i = 0; i < max; i++)
			{
				msg = new StringBuilder().append(msg).append(changedFPlayers.get(i).getPlayer().getName(null)).append(" ").append(showScoreNotKills ? "" + ServerStorage.getInstance().getString(player.getLang(), "FightEvents.SCORE") + "" : "" + ServerStorage.getInstance().getString(player.getLang(), "FightEvents.KILLS") + "").append(": ").append(showScoreNotKills ? changedFPlayers.get(i).getScore() : changedFPlayers.get(i).getKills()).append("\n").toString();
			}
		}
		return msg;
	}
	
	private boolean nobodyIsClose(Location loc)
	{
		for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			final Location playerLoc = iFPlayer.getPlayer().getLocation();
			if (Math.abs(playerLoc.getX() - loc.getX()) <= CLOSE_LOCATIONS_VALUE)
			{
				return false;
			}
			if (Math.abs(playerLoc.getY() - loc.getY()) <= CLOSE_LOCATIONS_VALUE)
			{
				return false;
			}
		}
		return true;
	}

	private void checkIfRegisteredMeetCriteria(boolean checkReflection)
	{
		getPlayers(REGISTERED_PLAYERS).stream().filter(p -> (p != null)).forEach(p -> checkIfRegisteredPlayerMeetCriteria(p, checkReflection));
	}
	
	private boolean checkIfRegisteredPlayerMeetCriteria(FightEventPlayer fPlayer, boolean checkReflection)
	{
		if (!FightEventManager.getInstance().canPlayerParticipate(fPlayer.getPlayer(), this, true, true, checkReflection))
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	private FightEventManager.CLASSES[] parseExcludedClasses(String classes)
	{
		if (classes.equals(""))
		{
			return null;
		}
		
		final String[] classType = classes.split(";");
		final FightEventManager.CLASSES[] realTypes = new FightEventManager.CLASSES[classType.length];
		for (int i = 0; i < classType.length; i++)
		{
			realTypes[i] = FightEventManager.CLASSES.valueOf(classType[i]);
		}
		return realTypes;
	}
	
	protected int[] parseExcludedSkills(String ids)
	{
		if (ids == null || ids.isEmpty())
		{
			return null;
		}
		final StringTokenizer st = new StringTokenizer(ids, ";");
		final int[] realIds = new int[st.countTokens()];
		int index = 0;
		while (st.hasMoreTokens())
		{
			realIds[index] = Integer.parseInt(st.nextToken());
			index++;
		}
		return realIds;
	}
	
	protected int[] parseValidLevels(String ids)
	{
		if (ids == null || ids.isEmpty())
		{
			return null;
		}
		
		final StringTokenizer st = new StringTokenizer(ids, "-");
		final int[] realIds = new int[st.countTokens()];
		int index = 0;
		while (st.hasMoreTokens())
		{
			realIds[index] = Integer.parseInt(st.nextToken());
			index++;
		}
		return realIds;
	}
	
	protected List<Integer> parseValidProffs(String ids)
	{
		if (ids == null || ids.isEmpty())
		{
			return Collections.emptyList();
		}
		
		final StringTokenizer st = new StringTokenizer(ids, ";");
		final List<Integer> list = new ArrayList<>();
		while (st.hasMoreTokens())
		{
			list.add(Integer.parseInt(st.nextToken()));
		}
		return list;
	}
	
	protected int[][] parseItemsList(String line)
	{
		if (line == null || line.isEmpty())
		{
			return null;
		}
		
		final String[] propertySplit = line.split(";");
		if (propertySplit.length == 0)
		{
			return null;
		}
		
		int i = 0;
		String[] valueSplit;
		final int[][] result = new int[propertySplit.length][];
		for (final String value : propertySplit)
		{
			valueSplit = value.split(",");
			if (valueSplit.length != 2)
			{
				warn(StringUtil.concat("parseItemsList[" + getName(null) + "]: invalid entry -> \"", valueSplit[0], "\", should be itemId,itemNumber"));
				return null;
			}
			
			result[i] = new int[2];
			try
			{
				result[i][0] = Integer.parseInt(valueSplit[0]);
			}
			catch (final NumberFormatException e)
			{
				warn(StringUtil.concat("parseItemsList[" + getName(null) + "]: invalid itemId -> \"", valueSplit[0], "\""));
				return null;
			}
			try
			{
				result[i][1] = Integer.parseInt(valueSplit[1]);
			}
			catch (final NumberFormatException e)
			{
				warn(StringUtil.concat("parseItemsList[" + getName(null) + "]: invalid item number -> \"", valueSplit[1], "\""));
				return null;
			}
			i++;
		}
		return result;
	}
	
	private SchedulingPattern parseAutoStartTimes(String time)
	{
		if (time != null && time.equals("-1"))
		{
			_isWithoutTime = true;
			return null;
		}
		
		SchedulingPattern cronTime = null;
		if (time != null && !time.isEmpty())
		{
			try
			{
				cronTime = new SchedulingPattern(time);
			}
			catch (final InvalidPatternException e)
			{
				cronTime = null;
			}
		}
		return cronTime;
	}
	
	private int[][] parseBuffs(String buffs)
	{
		if (buffs == null || buffs.isEmpty())
		{
			return null;
		}
		
		final StringTokenizer st = new StringTokenizer(buffs, ";");
		final int[][] realBuffs = new int[st.countTokens()][2];
		int index = 0;
		while (st.hasMoreTokens())
		{
			final String[] skillLevel = st.nextToken().split(",");
			final int[] realHourMin =
			{
			        Integer.parseInt(skillLevel[0]), Integer.parseInt(skillLevel[1])
			};
			realBuffs[index] = realHourMin;
			index++;
		}
		return realBuffs;
	}

	private int getTimeToWait(int totalLeftTimeInSeconds)
	{
		int toWait = 1;
		
		final int[] stops =
		{
		        5, 15, 30, 60, 300, 600, 900
		};
		
		for (final int stop : stops)
		{
			if (totalLeftTimeInSeconds > stop)
			{
				toWait = stop;
			}
		}
		
		return toWait;
	}
	
	private class LeftZoneThread implements Runnable
	{
		@Override
		public void run()
		{
			final List<FightEventPlayer> toDelete = new ArrayList<>();
			for (final Entry<FightEventPlayer, ZoneType> entry : _leftZone.entrySet())
			{
				final Player player = entry.getKey().getPlayer();
				if (player == null || !player.isOnline() || _state == EVENT_STATE.NOT_ACTIVE || entry.getValue().isInsideZone(player) || player.isDead() || player.isTeleporting())
				{
					toDelete.add(entry.getKey());
					continue;
				}
				
				final int power = (int) Math.max(400, entry.getValue().getDistanceToZone(player) - 4000);
				
				player.sendPacket(new EarthQuake(player.getX(), player.getY(), player.getZ(), power, 5));
				player.sendPacket(new CreatureSay(0, Say2.PARTYROOM_COMMANDER, new ServerMessage("FightEvents.ERROR", player.getLang()).toString(), new ServerMessage("FightEvents.BACK_TO_ZONE", player.getLang()).toString()));
				entry.getKey().increaseSecondsOutsideZone();
				
				if (entry.getKey().getSecondsOutsideZone() >= TIME_MAX_SECONDS_OUTSIDE_ZONE)
				{
					player.doDie(null);
					toDelete.add(entry.getKey());
					entry.getKey().clearSecondsOutsideZone();
				}
			}
			
			for (final FightEventPlayer playerToDelete : toDelete)
			{
				if (playerToDelete != null)
				{
					_leftZone.remove(playerToDelete);
					playerToDelete.clearSecondsOutsideZone();
				}
			}
		}
	}
	
	protected boolean isAfkTimerStopped(Player player)
	{
		return player.isDead() && !_ressAllowed && _respawnTime <= 0;
	}
	
	private class CheckAfkThread implements Runnable
	{
		@Override
		public void run()
		{
			final long currentTime = System.currentTimeMillis();
			for (final FightEventPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			{
				final Player player = iFPlayer.getPlayer();
				final boolean isAfk = (player.getLastNotAfkTime() + TIME_TO_BE_AFK * 1000) < currentTime;
				
				if (isAfkTimerStopped(player))
				{
					continue;
				}
				
				if (iFPlayer.isAfk())
				{
					if (!isAfk)
					{
						handleAfk(iFPlayer, false);
					}
					else if (_state != EVENT_STATE.OVER)
					{
						sendMessageToPlayer(player, MESSAGE_TYPES.CRITICAL, new ServerMessage("FightEvents.AFK_MODE", player.getLang()));
					}
				}
				else if (_state == EVENT_STATE.NOT_ACTIVE)
				{
					handleAfk(iFPlayer, false);
				}
				else if (isAfk)
				{
					handleAfk(iFPlayer, true);
				}
			}
		}
	}
	
	private class BestTeamComparator implements Comparator<FightEventTeam>, Serializable
	{
		private static final long serialVersionUID = -7744947898101934099L;
		private final boolean _scoreNotKills;
		private BestTeamComparator(boolean scoreNotKills)
		{
			_scoreNotKills = scoreNotKills;
		}
		
		@Override
		public int compare(FightEventTeam o1, FightEventTeam o2)
		{
			if (_scoreNotKills)
			{
				return Integer.compare(o2.getScore(), o1.getScore());
			}
			else
			{
				return Integer.compare(getTeamTotalKills(o2), getTeamTotalKills(o1));
			}
		}
	}
	
	private static class BestPlayerComparator implements Comparator<FightEventPlayer>
	{
		private final boolean _scoreNotKills;

		private BestPlayerComparator(boolean scoreNotKills)
		{
			_scoreNotKills = scoreNotKills;
		}
		
		@Override
		public int compare(FightEventPlayer arg0, FightEventPlayer arg1)
		{
			if (_scoreNotKills)
			{
				return Integer.compare(arg1.getScore(), arg0.getScore());
			}
			return Integer.compare(arg1.getKills(), arg0.getKills());
		}
	}
	
	public static boolean teleportWholeRoomTimer(int eventObjId, int secondsLeft)
	{
		final AbstractFightEvent event = FightEventManager.getInstance().getEventById(eventObjId);
		if (secondsLeft == 0)
		{
			event.startEvent(true);
		}
		else
		{
			event.checkIfRegisteredMeetCriteria(true);
			event.sendMessageToRegistered(MESSAGE_TYPES.SCREEN_BIG, "FightEvents.WILL_TELE", secondsLeft);
		}
		return true;
	}
	
	public static boolean startRoundTimer(int eventObjId, int secondsLeft)
	{
		final AbstractFightEvent event = FightEventManager.getInstance().getEventById(eventObjId);
		if (secondsLeft > 0)
		{
			event.sendMessageWithCheckRound(event, MESSAGE_TYPES.SCREEN_BIG, secondsLeft);
		}
		else
		{
			event.startRound();
		}
		return true;
	}
	
	public static boolean startChangeMapTimer(int eventObjId, int secondsLeft)
	{
		final AbstractFightEvent event = FightEventManager.getInstance().getEventById(eventObjId);
		
		if (secondsLeft > 0)
		{
			event.sendMessageWithCheckDestroy(event, MESSAGE_TYPES.SCREEN_BIG, secondsLeft);
		}
		else
		{
			event.destroyMe();
		}
		
		return true;
	}
	
	public static boolean endRoundTimer(int eventObjId, int secondsLeft)
	{
		final AbstractFightEvent event = FightEventManager.getInstance().getEventById(eventObjId);
		if (secondsLeft > 0)
		{
			final String msg = !event.isLastRound() ? "FightEvents.GOING_OVER_ROUND" : "FightEvents.GOING_OVER_MATCH";
			event.sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, msg, false, secondsLeft);
		}
		else
		{
			event.endRound();
		}
		
		return true;
	}
	
	public static boolean teleportBackSinglePlayerTimer(int eventObjId, int secondsLeft, Player player)
	{
		final AbstractFightEvent event = FightEventManager.getInstance().getEventById(eventObjId);
		
		if (player == null || !player.isOnline())
		{
			return false;
		}
		
		if (secondsLeft > 0)
		{
			final ServerMessage msg = new ServerMessage("FightEvents.TELE_BACK", player.getLang());
			msg.add(getFixedTime(player, secondsLeft).toString());
			event.sendMessageToPlayer(player, MESSAGE_TYPES.SCREEN_BIG, msg);
		}
		else
		{
			event.teleportBackToTown(player);
		}
		return true;
	}
	
	public static boolean ressurectionTimer(int eventObjId, int secondsLeft, FightEventPlayer fPlayer)
	{
		final AbstractFightEvent event = FightEventManager.getInstance().getEventById(eventObjId);
		final Player player = fPlayer.getPlayer();
		
		if (player == null || !player.isOnline() || !player.isDead())
		{
			return false;
		}
		
		if (secondsLeft > 0)
		{
			final ServerMessage msg = new ServerMessage("FightEvents.RESPAWN_IN", player.getLang());
			msg.add(getFixedTime(player, secondsLeft).toString());
			player.sendMessage(msg.toString());
		}
		else
		{
			event.hideScores(player);
			event.teleportSinglePlayer(fPlayer, false, true, false);
		}
		return true;
	}
	
	public static boolean setInvisible(int eventObjId, int secondsLeft, FightEventPlayer fPlayer, boolean sendMessages)
	{
		final AbstractFightEvent event = FightEventManager.getInstance().getEventById(eventObjId);
		if (fPlayer.getPlayer() == null || !fPlayer.getPlayer().isOnline())
		{
			return false;
		}
		
		if (secondsLeft > 0)
		{
			if (sendMessages)
			{
				final ServerMessage msg = new ServerMessage("FightEvents.VISIBLE_IN", fPlayer.getPlayer().getLang());
				msg.add(getFixedTime(fPlayer.getPlayer(), secondsLeft).toString());
				event.sendMessageToPlayer(fPlayer.getPlayer(), MESSAGE_TYPES.SCREEN_BIG, msg);
			}
		}
		else
		{
			if (sendMessages && event.getState() == EVENT_STATE.STARTED)
			{
				event.sendMessageToPlayer(fPlayer.getPlayer(), MESSAGE_TYPES.SCREEN_BIG, new ServerMessage("FightEvents.FIGHT", true));
			}
			event.stopInvisibility(fPlayer.getPlayer());
		}
		return true;
		
	}
	
	public void startNewTimer(boolean saveAsMainTimer, int firstWaitingTimeInMilis, String methodName, Object... args)
	{
		final ScheduledFuture<?> timer = ThreadPoolManager.getInstance().schedule(new SmartTimer(methodName, saveAsMainTimer, args), firstWaitingTimeInMilis);
		if (saveAsMainTimer)
		{
			_timer = timer;
		}
	}
	
	private class SmartTimer implements Runnable
	{
		private final String _methodName;
		private final Object[] _args;
		private final boolean _saveAsMain;
		
		private SmartTimer(String methodName, boolean saveAsMainTimer, Object... args)
		{
			_methodName = methodName;
			
			final Object[] changedArgs = new Object[args.length + 1];
			changedArgs[0] = getId();
			for (int i = 0; i < args.length; i++)
			{
				changedArgs[i + 1] = args[i];
			}
			_args = changedArgs;
			_saveAsMain = saveAsMainTimer;
		}
		
		@Override
		public void run()
		{
			final Class<?>[] parameterTypes = new Class<?>[_args.length];
			for (int i = 0; i < _args.length; i++)
			{
				parameterTypes[i] = _args[i] != null ? _args[i].getClass() : null;
			}
			
			int waitingTime = (int) _args[1];
			
			try
			{
				final Object ret = MethodUtils.invokeMethod(AbstractFightEvent.this, _methodName, _args, parameterTypes);
				if ((boolean) ret == false)
				{
					return;
				}
			}
			catch (
			    NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
			{
				e.printStackTrace();
			}
			
			if (waitingTime > 0)
			{
				final int toWait = getTimeToWait(waitingTime);
				
				waitingTime -= toWait;
				
				_args[1] = waitingTime;
				
				final ScheduledFuture<?> timer = ThreadPoolManager.getInstance().schedule(this, toWait * 1000);
				if (_saveAsMain)
				{
					_timer = timer;
				}
			}
			else
			{
				return;
			}
		}
	}
	
	public void onAddEvent(GameObject o)
	{
		if (o.isPlayer())
		{
			o.getActingPlayer().addListener(_exitListener);
		}
	}
	
	public void onRemoveEvent(GameObject o)
	{
		if (o.isPlayer())
		{
			o.getActingPlayer().removeListener(_exitListener);
		}
	}
	
	public boolean isInProgress()
	{
		return _state != EVENT_STATE.NOT_ACTIVE;
	}

	@SuppressWarnings("unchecked")
	public <O extends Serializable> List<O> getObjects(String name)
	{
		final List<Serializable> objects = _objects.get(name);
		return objects == null ? Collections.<O> emptyList() : (List<O>) objects;
	}

	public <O extends Serializable> O getFirstObject(String name)
	{
		final List<O> objects = getObjects(name);
		return objects.size() > 0 ? (O) objects.get(0) : null;
	}

	public void addObject(String name, Serializable object)
	{
		if (object == null)
		{
			return;
		}

		List<Serializable> list = _objects.get(name);
		if (list != null)
		{
			list.add(object);
		}
		else
		{
			list = new CopyOnWriteArrayList<>();
			list.add(object);
			_objects.put(name, list);
		}
	}

	public void removeObject(String name, Serializable o)
	{
		if (o == null)
		{
			return;
		}

		final List<Serializable> list = _objects.get(name);
		if (list != null)
		{
			list.remove(o);
		}
	}

	@SuppressWarnings("unchecked")
	public <O extends Serializable> List<O> removeObjects(String name)
	{
		final List<Serializable> objects = _objects.remove(name);
		return objects == null ? Collections.<O> emptyList() : (List<O>) objects;
	}

	@SuppressWarnings("unchecked")
	public void addObjects(String name, List<? extends Serializable> objects)
	{
		if (objects.isEmpty())
		{
			return;
		}

		final List<Serializable> list = _objects.get(name);
		if (list != null)
		{
			list.addAll(objects);
		}
		else
		{
			_objects.put(name, (List<Serializable>) objects);
		}
	}

	public Map<String, List<Serializable>> getObjects()
	{
		return _objects;
	}

	public int getId()
	{
		return _id;
	}

	public Map<String, ZoneType> getActiveZones()
	{
		return _activeZones;
	}
	
	public boolean isGlobal()
	{
		return _isGlobal;
	}
	
	public boolean isWithoutTime()
	{
		return _isWithoutTime;
	}
	
	public FightEventGameRoom getEventRoom()
	{
		return _room;
	}
	
	public boolean isHideNames()
	{
		return _isHideNames;
	}
	
	public boolean isHideClanInfo()
	{
		return _isHideClanInfo;
	}
	
	public boolean isHideByTransform()
	{
		return _isHideByTransform;
	}
	
	public String getTransformList()
	{
		return _transformationList;
	}
	
	public void calcTransformation(Player player, int index)
	{
		if (getTransformList() == null || getTransformList().isEmpty())
		{
			return;
		}
		
		int transformId = -1;
		final String[] list = getTransformList().split(";");
		if (list.length >= 7)
		{
			transformId = Integer.parseInt(list[index]);
		}
		
		if (transformId > 0)
		{
			player.setPolyId(transformId);
		}
	}
	
	public boolean isGetHeroStatus()
	{
		return _getHeroStatus;
	}
	
	public boolean isGetHeroSkills()
	{
		return _getHeroSkills;
	}
	
	public long getHeroTime()
	{
		return _heroTime;
	}
	
	public boolean isUseEventItems()
	{
		return _isUseEventItems;
	}
	
	protected List<FightEventItem> getEventItems(final int classId)
	{
		return _templates.get(classId);
	}
	
	public int[] getValidLevels()
	{
		return _validLevels;
	}
	
	public List<Integer> getValidProffs()
	{
		return _validProffs;
	}
	
	public String getName(String lang)
	{
		try
		{
			return _set.getString(lang != null ? "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "name" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public String getDescription(String lang)
	{
		try
		{
			return _set.getString(lang != null ? "desc" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "desc" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public void increaseKills(FightEventPlayer player)
	{
		final var party = player.getPlayer().getParty();
		if (_isPartyRandomKillPoints && party != null)
		{
			final var pl = party.getMembers().get(Rnd.get(party.getMembers().size()));
			if (pl != null)
			{
				final var realActor = getFightEventPlayer(pl);
				if (realActor != null)
				{
					realActor.increasePartPoints();
				}
				else
				{
					player.increasePartPoints();
				}
			}
			else
			{
				player.increasePartPoints();
			}
		}
		else
		{
			player.increasePartPoints();
		}
		
		player.increaseKills();
		updatePlayerScore(player);
		player.getPlayer().sendUserInfo();
	}
}