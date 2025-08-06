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
package l2e.gameserver.model.entity.events.cleft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.instancemanager.SoDManager;
import l2e.gameserver.instancemanager.SoIManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExCleftList;
import l2e.gameserver.network.serverpackets.ExCleftList.CleftType;
import l2e.gameserver.network.serverpackets.ExCleftState;
import l2e.gameserver.network.serverpackets.ExCleftState.CleftState;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Created by LordWinter 03.12.2018
 */
public class AerialCleftEvent
{
	protected static final Logger _log = LoggerFactory.getLogger(AerialCleftEvent.class);

	public AerialCleftTeam[] _teams = new AerialCleftTeam[2];
	AerialCleftTeam _winteam = null;
	AerialCleftTeam _loseteam = null;
	
	protected Future<?> _regTask;
	protected Future<?> _eventTask;
	protected Future<?> _collectTask;
	
	protected int _eventTime = 0;
	
	protected Location[] startRed =
	{
	        new Location(-222704, 247803, 1744), new Location(-222649, 247945, 1728), new Location(-222652, 247666, 1728)
	};
	
	protected Location[] exitRed =
	{
	        new Location(-223701, 247795, 1744), new Location(-223780, 247661, 1744), new Location(-223776, 247914, 1744)
	};

	protected Location[] startBlue =
	{
	        new Location(-205312, 242144, 1744), new Location(-205376, 241997, 1744), new Location(-205376, 242276, 1744)
	};

	protected Location[] exitBlue =
	{
	        new Location(-204350, 242148, 1744), new Location(-204284, 242288, 1744), new Location(-204288, 242026, 1728)
	};

	private EventState _state = EventState.INACTIVE;
	
	enum EventState
	{
		INACTIVE, INACTIVATING, PARTICIPATING, STARTING, STARTED, REWARDING
	}

	public AerialCleftEvent()
	{
		_teams[0] = new AerialCleftTeam("Blue Team", 0, startBlue, exitBlue);
		_teams[1] = new AerialCleftTeam("Red Team", 1, startRed, exitRed);
		if (checkRegistration())
		{
			startRegistration();
		}
	}
	
	public void startRegistration()
	{
		if (_regTask == null)
		{
			if (isInactive() && checkRegistration())
			{
				setState(EventState.PARTICIPATING);
				DoubleSessionManager.getInstance().registerEvent(DoubleSessionManager.AERIAL_CLEFT_ID);
				_log.info("Aerial Cleft: Registration period begin.");
			}
		}
	}
	
	public boolean openRegistration()
	{
		if (isInactive())
		{
			cancelRegTask();
			setState(EventState.PARTICIPATING);
			DoubleSessionManager.getInstance().registerEvent(DoubleSessionManager.AERIAL_CLEFT_ID);
			_log.info("Aerial Cleft: Open registration for Aerial Cleft Event.");
			return true;
		}
		return false;
	}
	
	public boolean cleanUpTime()
	{
		if (isInactive())
		{
			cancelRegTask();
			return true;
		}
		return false;
	}

	public boolean forcedEventStart()
	{
		if (isParticipating())
		{
			if (_teams[0].getParticipatedPlayerCount() > 0 && _teams[1].getParticipatedPlayerCount() > 0)
			{
				startEvent();
				return true;
			}
		}
		return false;
	}
	
	public boolean forcedEventStop()
	{
		if (isStarted() || isRewarding())
		{
			if (isStarted())
			{
				stopEvent();
			}
			else
			{
				cancelCollectTask();
				collectEndTime();
			}
			return true;
		}
		return false;
	}
	
	public boolean checkRegistration()
	{
		if (Config.CLEFT_WITHOUT_SEEDS)
		{
			return true;
		}
		return SoDManager.getInstance().isOpened() || SoIManager.getInstance().isSeedOpen();
	}

	protected void cancelRegTask()
	{
		if (_regTask != null)
		{
			_regTask.cancel(false);
			_regTask = null;
		}
	}
	
	protected void cancelEventTask()
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
	}

	protected void cancelCollectTask()
	{
		if (_collectTask != null)
		{
			_collectTask.cancel(false);
			_collectTask = null;
		}
	}

	protected void updateTeams(Player newPlayer, int teamId)
	{
		for (final AerialCleftTeam team : _teams)
		{
			for (final Player player : team.getParticipatedPlayers().values())
			{
				if (player != null && player != newPlayer)
				{
					player.sendPacket(new ExCleftList(CleftType.ADD, newPlayer, teamId));
				}
			}
		}
	}
	
	protected void updateChangeTeams(int playerObjectId, int oldTeamId, int newTeamId)
	{
		for (final AerialCleftTeam team : _teams)
		{
			for (final Player player : team.getParticipatedPlayers().values())
			{
				if (player != null)
				{
					player.sendPacket(new ExCleftList(CleftType.TEAM_CHANGE, playerObjectId, oldTeamId, newTeamId));
				}
			}
		}
	}

	public void registerPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}

		byte teamId = 0;

		teamId = (byte) (_teams[0].getParticipatedPlayerCount() > _teams[1].getParticipatedPlayerCount() ? 1 : 0);
		_teams[teamId].addPlayer(player);
		
		if (isStarted())
		{
			new AerialCleftTeleporter(player, _teams[teamId].getLocations(), true, false);
			player.untransform();
			_teams[teamId].startEventTime(player);

			for (final Skill skill : player.getAllSkills())
			{
				if (skill != null && !skill.isPassive() && skill.getId() != 932)
				{
					if (skill.getId() == 840 || skill.getId() == 841 || skill.getId() == 842)
					{
						player.disableSkill(skill, 40000);
					}
					else
					{
						player.addBlockSkill(skill, false);
					}
				}
			}
			player.sendSkillList(true);
			player.sendPacket(SystemMessageId.THE_AERIAL_CLEFT_HAS_BEEN_ACTIVATED);
			player.sendPacket(new ExCleftState(CleftState.TOTAL, this, _teams[0], _teams[1]));
		}

		if (isParticipating())
		{
			player.sendPacket(new ExCleftList(CleftType.TOTAL, _teams[1], _teams[0]));
			updateTeams(player, _teams[teamId].getId());
			if (Config.CLEFT_BALANCER)
			{
				if ((_teams[0].getParticipatedPlayerCount() >= 2 && _teams[1].getParticipatedPlayerCount() >= 2) && (_teams[0].getParticipatedPlayerCount() == _teams[1].getParticipatedPlayerCount()))
				{
					checkPlayersBalance();
				}
			}

			if (_teams[0].getParticipatedPlayerCount() == Config.CLEFT_MIN_TEAM_PLAYERS && _teams[1].getParticipatedPlayerCount() == Config.CLEFT_MIN_TEAM_PLAYERS)
			{
				startEvent();
			}
		}
	}
	
	public void removePlayer(int objectId, boolean exitEvent)
	{
		final byte teamId = getParticipantTeamId(objectId);

		if (teamId != -1)
		{
			_teams[teamId].removePlayer(objectId);
			_teams[teamId].removePlayerTime(objectId);

			final Player listener = GameObjectsStorage.getPlayer(objectId);
			if (listener != null)
			{
				_teams[teamId].removePlayerFromList(listener);
				if ((isStarted() || isRewarding()) && exitEvent)
				{
					if (listener.isCleftCat())
					{
						listener.setCleftCat(false);
						_teams[teamId].selectTeamCat();
					}
					new AerialCleftTeleporter(listener, _teams[teamId].getExitLocations(), true, true);
				}
			}
			
			if (isParticipating())
			{
				for (final AerialCleftTeam team : _teams)
				{
					for (final Player player : team.getParticipatedPlayers().values())
					{
						if (player != null)
						{
							player.sendPacket(new ExCleftList(CleftType.REMOVE, objectId, teamId));
						}
					}
				}
			}
		}
	}
	
	private int highestLevelPlayer(Map<Integer, Player> players)
	{
		int maxLevel = Integer.MIN_VALUE, maxLevelId = -1;
		for (final Player player : players.values())
		{
			if (player.getLevel() >= maxLevel)
			{
				maxLevel = player.getLevel();
				maxLevelId = player.getObjectId();
			}
		}
		return maxLevelId;
	}
	
	protected void checkPlayersBalance()
	{
		final Map<Integer, Player> allParticipants = new HashMap<>();
		allParticipants.putAll(_teams[0].getParticipatedPlayers());
		allParticipants.putAll(_teams[1].getParticipatedPlayers());
		
		Player player;

		final Iterator<Player> iter = allParticipants.values().iterator();
		while (iter.hasNext())
		{
			player = iter.next();
			if (!checkPlayer(player))
			{
				iter.remove();
			}
		}

		final int balance[] =
		{
		        0, 0
		};
		int priority = 0, highestLevelPlayerId, oldTeam;
		Player highestLevelPlayer;

		while (!allParticipants.isEmpty())
		{
			highestLevelPlayerId = highestLevelPlayer(allParticipants);
			highestLevelPlayer = allParticipants.get(highestLevelPlayerId);
			allParticipants.remove(highestLevelPlayerId);

			oldTeam = getParticipantTeamId(highestLevelPlayer.getObjectId());
			if (oldTeam != _teams[priority].getId())
			{
				_teams[oldTeam].removePlayer(highestLevelPlayer.getObjectId());
				_teams[priority].addPlayer(highestLevelPlayer);
				updateChangeTeams(highestLevelPlayer.getObjectId(), oldTeam, priority);
			}
			balance[priority] += highestLevelPlayer.getLevel();
			if (allParticipants.isEmpty())
			{
				break;
			}
			priority = 1 - priority;
			highestLevelPlayerId = highestLevelPlayer(allParticipants);
			highestLevelPlayer = allParticipants.get(highestLevelPlayerId);
			allParticipants.remove(highestLevelPlayerId);
			
			oldTeam = getParticipantTeamId(highestLevelPlayer.getObjectId());
			if (oldTeam != _teams[priority].getId())
			{
				_teams[oldTeam].removePlayer(highestLevelPlayer.getObjectId());
				_teams[priority].addPlayer(highestLevelPlayer);
				updateChangeTeams(highestLevelPlayer.getObjectId(), oldTeam, priority);
			}
			balance[priority] += highestLevelPlayer.getLevel();
			priority = balance[0] > balance[1] ? 1 : 0;
		}
	}
	
	public void startEvent()
	{
		if (_teams[0].getParticipatedPlayerCount() == 0 || _teams[1].getParticipatedPlayerCount() == 0)
		{
			return;
		}
		setState(EventState.STARTING);

		SpawnParser.getInstance().spawnGroup("cleft_fight_spawn");

		setState(EventState.STARTED);
		
		_eventTime = (int) (System.currentTimeMillis() + (60000 * Config.CLEFT_WAR_TIME));
		if (_eventTask == null)
		{
			_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					calculateRewards();
				}
			}, Config.CLEFT_WAR_TIME * 60000);
		}
		for (final AerialCleftTeam team : _teams)
		{
			team.selectTeamCat();
		}

		for (final AerialCleftTeam team : _teams)
		{
			for (final Player player : team.getParticipatedPlayers().values())
			{
				if (player != null)
				{
					player.sendPacket(new ExCleftList(CleftType.CLOSE));
					new AerialCleftTeleporter(player, team.getLocations(), true, false);
					player.untransform();
					team.startEventTime(player);
					for (final Skill skill : player.getAllSkills())
					{
						if (skill != null && !skill.isPassive() && skill.getId() != 932)
						{
							if (skill.getId() == 840 || skill.getId() == 841 || skill.getId() == 842)
							{
								player.disableSkill(skill, 40000);
							}
							else
							{
								player.addBlockSkill(skill, false);
							}
						}
					}
					player.setCanRevive(false);
					player.sendSkillList(false);
					player.sendPacket(SystemMessageId.THE_AERIAL_CLEFT_HAS_BEEN_ACTIVATED);
					player.sendPacket(new ExCleftState(CleftState.TOTAL, this, _teams[0], _teams[1]));
				}
			}
		}
	}
	
	public void stopEvent()
	{
		setState(EventState.INACTIVATING);
		for (final AerialCleftTeam team : _teams)
		{
			for (final Player player : team.getParticipatedPlayers().values())
			{
				if (player != null)
				{
					player.sendPacket(new ExCleftState(CleftState.RESULT, _winteam, _loseteam));
					new AerialCleftTeleporter(player, _loseteam.getExitLocations(), true, true);
				}
			}
		}
		_teams[0].cleanMe();
		_teams[1].cleanMe();
		setState(EventState.INACTIVE);
		SpawnParser.getInstance().despawnGroup("cleft_fight_spawn");
		cancelEventTask();
		cancelCollectTask();
		DoubleSessionManager.getInstance().clear(DoubleSessionManager.AERIAL_CLEFT_ID);

		if (_regTask == null)
		{
			_regTask = ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					startRegistration();
				}
			}, Config.CLEFT_TIME_RELOAD_REG * 60000);
		}
	}
	
	public int getTotalEventPlayers()
	{
		return (_teams[0].getParticipatedPlayerCount() + _teams[1].getParticipatedPlayerCount());
	}
	
	public void calculateRewards()
	{
		setState(EventState.REWARDING);
		
		if (_teams[0].getPoints() > _teams[1].getPoints())
		{
			_winteam = _teams[0];
			_loseteam = _teams[1];
		}
		else
		{
			_winteam = _teams[1];
			_loseteam = _teams[0];
		}

		if (_teams[0].getPoints() == 0 && _teams[1].getPoints() == 0)
		{
			stopEvent();
			return;
		}

		SpawnParser.getInstance().despawnGroup("cleft_fight_spawn");
		SpawnParser.getInstance().spawnGroup("cleft_reward_spawn");
		
		for (final AerialCleftTeam team : _teams)
		{
			for (final Player player : team.getParticipatedPlayers().values())
			{
				player.sendPacket(new ExCleftState(CleftState.RESULT, _winteam, _loseteam));
				if (team == _loseteam)
				{
					new AerialCleftTeleporter(player, _loseteam.getExitLocations(), true, true);
				}
			}
			
			if (team == _loseteam)
			{
				rewardTeam(team, false);
			}
			else
			{
				rewardTeam(team, true);
			}
		}

		_collectTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				collectEndTime();
			}
		}, Config.CLEFT_COLLECT_TIME * 60000);
	}

	private void collectEndTime()
	{
		setState(EventState.INACTIVATING);
		
		for (final Player player : _winteam.getParticipatedPlayers().values())
		{
			if (player != null)
			{
				new AerialCleftTeleporter(player, _winteam.getExitLocations(), true, true);
			}
		}

		SpawnParser.getInstance().despawnGroup("cleft_reward_spawn");
		
		_teams[0].cleanMe();
		_teams[1].cleanMe();
		_winteam = null;
		_loseteam = null;
		setState(EventState.INACTIVE);
		
		cancelEventTask();
		cancelCollectTask();
		
		DoubleSessionManager.getInstance().clear(DoubleSessionManager.AERIAL_CLEFT_ID);

		if (_regTask == null)
		{
			_regTask = ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					startRegistration();
				}
			}, Config.CLEFT_TIME_RELOAD_REG * 60000);
		}
	}

	private void rewardTeam(AerialCleftTeam team, boolean winner)
	{
		for (final Player player : team.getParticipatedPlayers().values())
		{
			if (player == null)
			{
				continue;
			}

			if (checkPlayerTime(team, player.getObjectId()))
			{
				SystemMessage systemMessage = null;
				
				final int itemId = Config.CLEFT_REWARD_ID;
				final int count = winner ? Config.CLEFT_REWARD_COUNT_WINNER : Config.CLEFT_REWARD_COUNT_LOOSER;
				
				final PcInventory inv = player.getInventory();
				
				if (ItemsParser.getInstance().createDummyItem(itemId).isStackable())
				{
					inv.addItem("Aerial Cleft Event", itemId, count, player, player);
					
					if (count > 1)
					{
						systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
						systemMessage.addItemName(itemId);
						systemMessage.addItemNumber(count);
					}
					else
					{
						systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
						systemMessage.addItemName(itemId);
					}
					player.sendPacket(systemMessage);
				}
				else
				{
					for (int i = 0; i < count; ++i)
					{
						inv.addItem("Aerial Cleft Event", itemId, 1, player, player);
						systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
						systemMessage.addItemName(itemId);
						player.sendPacket(systemMessage);
					}
				}
				player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
				if (team == _winteam)
				{
					player.addExpAndSp(0, team.getPoints());
				}
			}
		}
	}
	
	private boolean checkPlayer(final Player player)
	{
		if (player.isInOfflineMode() || player.isJailed())
		{
			return false;
		}
		
		if (player.isCursedWeaponEquipped())
		{
			return false;
		}
		
		if (player.getLevel() < Config.CLEFT_MIN_LEVEL)
		{
			return false;
		}
		
		if (player.isMounted() || player.isDead() || player.inObserverMode())
		{
			return false;
		}
		
		if (player.isInDuel())
		{
			return false;
		}
		
		if (player.getFightEventGameRoom() != null || player.isInFightEvent() || player.getTeam() != 0 || player.checkInTournament())
		{
			return false;
		}
		
		if (player.isInOlympiadMode())
		{
			return false;
		}
		
		if (player.isInParty() && player.getParty().isInDimensionalRift())
		{
			return false;
		}
		
		if (player.isTeleporting())
		{
			return false;
		}
		
		if (player.isInSiege())
		{
			return false;
		}
		
		if (player.getReflectionId() != 0)
		{
			return false;
		}
		return true;
	}

	public byte getParticipantTeamId(int playerObjectId)
	{
		return (byte) (_teams[0].containsPlayer(playerObjectId) ? 0 : (_teams[1].containsPlayer(playerObjectId) ? 1 : -1));
	}

	private void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}

	public boolean isInactive()
	{
		boolean isInactive;
		
		synchronized (_state)
		{
			isInactive = _state == EventState.INACTIVE;
		}
		return isInactive;
	}
	
	public boolean isInactivating()
	{
		boolean isInactivating;
		
		synchronized (_state)
		{
			isInactivating = _state == EventState.INACTIVATING;
		}
		return isInactivating;
	}
	
	public boolean isParticipating()
	{
		boolean isParticipating;
		
		synchronized (_state)
		{
			isParticipating = _state == EventState.PARTICIPATING;
		}
		return isParticipating;
	}
	
	public boolean isStarting()
	{
		boolean isStarting;
		
		synchronized (_state)
		{
			isStarting = _state == EventState.STARTING;
		}
		return isStarting;
	}
	
	public boolean isStarted()
	{
		boolean isStarted;
		
		synchronized (_state)
		{
			isStarted = _state == EventState.STARTED;
		}
		return isStarted;
	}
	
	public boolean isRewarding()
	{
		boolean isRewarding;
		
		synchronized (_state)
		{
			isRewarding = _state == EventState.REWARDING;
		}
		return isRewarding;
	}

	public boolean isValidRegistration()
	{
		return isParticipating() || isStarting() || isStarted();
	}

	public boolean isPlayerParticipant(int objectId)
	{
		if (!isParticipating() && !isStarting() && !isStarted())
		{
			return false;
		}
		return _teams[0].containsPlayer(objectId) || _teams[1].containsPlayer(objectId);
	}

	protected void updateTeamCat(AerialCleftTeam killedTeam)
	{
		for (final AerialCleftTeam team : _teams)
		{
			for (final Player player : team.getParticipatedPlayers().values())
			{
				if (player != null)
				{
					player.sendPacket(new ExCleftState(CleftState.CAT_UPDATE, this, killedTeam));
				}
			}
		}
	}
	
	public boolean onEscapeUse(int playerObjectId)
	{
		if ((isStarted() || isRewarding()) && isPlayerParticipant(playerObjectId))
		{
			return false;
		}
		return true;
	}

	public void onLogout(Player player)
	{
		if ((player != null) && (isStarting() || isStarted() || isParticipating()))
		{
			removePlayer(player.getObjectId(), true);
			player.cleanBlockSkills(false);
		}
	}
	
	public boolean onAction(Player player, int targetedPlayerObjectId)
	{
		if ((player == null) || (!isStarted() && !isRewarding()))
		{
			return true;
		}

		if (player.isGM())
		{
			return true;
		}

		final byte playerTeamId = getParticipantTeamId(player.getObjectId());
		final byte targetedPlayerTeamId = getParticipantTeamId(targetedPlayerObjectId);

		if (((playerTeamId != -1) && (targetedPlayerTeamId == -1)) || ((playerTeamId == -1) && (targetedPlayerTeamId != -1)))
		{
			return false;
		}

		if ((playerTeamId != -1) && (targetedPlayerTeamId != -1) && (playerTeamId == targetedPlayerTeamId) && (player.getObjectId() != targetedPlayerObjectId))
		{
			return false;
		}
		return true;
	}

	public boolean onScrollUse(int playerObjectId)
	{
		if (!isStarted() && !isRewarding())
		{
			return true;
		}
		
		if (isPlayerParticipant(playerObjectId))
		{
			return false;
		}
		return true;
	}

	public void onKill(Creature killerCharacter, Player killedPlayer)
	{
		if ((killedPlayer == null) || !isStarted())
		{
			return;
		}

		final byte killedTeamId = getParticipantTeamId(killedPlayer.getObjectId());

		if (killedTeamId == -1)
		{
			return;
		}

		killedPlayer.setCleftDeath(1);
		new AerialCleftTeleporter(killedPlayer, _teams[killedTeamId].getLocations(), false, false);

		if (killerCharacter == null)
		{
			return;
		}

		Player killerPlayerInstance = null;

		if (killerCharacter.isPlayer())
		{
			killerPlayerInstance = (Player) killerCharacter;
		}
		else
		{
			return;
		}

		final byte killerTeamId = getParticipantTeamId(killerPlayerInstance.getObjectId());

		if ((killerTeamId != -1) && (killedTeamId != -1) && (killerTeamId != killedTeamId))
		{
			final AerialCleftTeam killerTeam = _teams[killerTeamId];

			if (killedPlayer.isCleftCat())
			{
				killerTeam.addPoints(Config.TEAM_CAT_POINT);
			}
			else
			{
				killerTeam.addPoints(Config.TEAM_PLAYER_POINT);
			}
			killerPlayerInstance.setCleftKill(1);
			updatePvpKills(killerPlayerInstance, killedPlayer, _teams[killerTeamId], _teams[killedTeamId]);
		}

		if (killedPlayer.isCleftCat())
		{
			killedPlayer.setCleftCat(false);
			_teams[killedTeamId].selectTeamCat();
			updateTeamCat(_teams[killedTeamId]);
		}
	}

	protected void updatePvpKills(Player killer, Player killed, AerialCleftTeam killerTeam, AerialCleftTeam killedTeam)
	{
		for (final AerialCleftTeam team : _teams)
		{
			for (final Player player : team.getParticipatedPlayers().values())
			{
				if (player != null)
				{
					player.sendPacket(new ExCleftState(CleftState.PVP_KILL, this, _teams[0], _teams[1], killer, killed, killerTeam.getId(), killedTeam.getId()));
				}
			}
		}
	}
	
	public void checkNpcPoints(Attackable npc, Player killer)
	{
		if ((killer == null) || !isStarted())
		{
			return;
		}
		
		final byte killerTeamId = getParticipantTeamId(killer.getObjectId());
		
		if (killerTeamId != -1)
		{
			final AerialCleftTeam killerTeam = _teams[killerTeamId];
			killer.setCleftKillTower(1);
			switch (npc.getId())
			{
				case 22553 :
					killerTeam.addPoints(Config.LARGE_COMPRESSOR_POINT);
					break;
				case 22554 :
				case 22555 :
				case 22556 :
					killerTeam.addPoints(Config.SMALL_COMPRESSOR_POINT);
					break;
			}
			updateTowerCount(killer, killerTeam, npc.getId());
		}
	}

	protected void updateTowerCount(Player killer, AerialCleftTeam killerTeam, int npcId)
	{
		for (final AerialCleftTeam team : _teams)
		{
			for (final Player player : team.getParticipatedPlayers().values())
			{
				if (player != null)
				{
					player.sendPacket(new ExCleftState(CleftState.TOWER_DESTROY, this, killerTeam, _teams[0], _teams[1], npcId, killer));
				}
			}
		}
	}

	public int getEventTime()
	{
		return _eventTime;
	}
	
	public int getEventTimeEnd()
	{
		return ((int) (_eventTime - System.currentTimeMillis()) / 1000);
	}
	
	public boolean getPlayerTime(long time)
	{
		return ((int) (_eventTime - time) / 60000) >= Config.CLEFT_MIN_PLAYR_EVENT_TIME;
	}
	
	protected boolean checkPlayerTime(AerialCleftTeam team, int playerObjectId)
	{
		if (team.containsTime(playerObjectId))
		{
			return getPlayerTime(team.getParticipatedTimes().get(playerObjectId));
		}
		return false;
	}
	
	public static AerialCleftEvent getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final AerialCleftEvent _instance = new AerialCleftEvent();
	}
}