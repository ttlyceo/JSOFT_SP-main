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
package l2e.gameserver.model.entity.events.model.impl;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.object.CTFCombatFlagObject;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.entity.events.model.template.FightEventTeam;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public class CaptureTheFlagEvent extends AbstractFightEvent
{
	private CaptureFlagTeam[] _flagTeams;
	private final int[][] _rewardByCaptureFlag;
	private final boolean _rewardByCaptureFlagAlone;
	private final int _captureFlagLimit;
	
	public CaptureTheFlagEvent(MultiValueSet<String> set)
	{
		super(set);
		
		_rewardByCaptureFlag = parseItemsList(set.getString("rewardByCaptureFlag", null));
		_rewardByCaptureFlagAlone = set.getBool("rewardByCaptureFlagAlone", false);
		_captureFlagLimit = set.getInteger("captureFlagLimit", -1);
	}
	
	@Override
	public void onKilled(Creature actor, Creature victim)
	{
		try
		{
			if (actor != null && actor.isPlayer())
			{
				final FightEventPlayer realActor = getFightEventPlayer(actor.getActingPlayer());
				if (victim.isPlayer() && realActor != null)
				{
					increaseKills(realActor);
					final ServerMessage msg = new ServerMessage("FightEvents.YOU_HAVE_KILL", realActor.getPlayer().getLang());
					msg.add(victim.getName(null));
					sendMessageToPlayer(realActor.getPlayer(), MESSAGE_TYPES.GM, msg);
				}
			}
			
			if (victim.isPlayer())
			{
				final FightEventPlayer realVictim = getFightEventPlayer(victim);
				realVictim.increaseDeaths();
				if (actor != null)
				{
					final ServerMessage msg = new ServerMessage("FightEvents.YOU_KILLED", realVictim.getPlayer().getLang());
					msg.add(actor.getName(null));
					sendMessageToPlayer(realVictim.getPlayer(), MESSAGE_TYPES.GM, msg);
				}
				victim.getActingPlayer().broadcastCharInfo();
				respawnFlag(realVictim);
			}
			super.onKilled(actor, victim);
		}
		catch (final Exception e)
		{
			warn("Error on CaptureTheFlag OnKilled!", e);
		}
	}
	
	@Override
	public void startEvent(boolean checkReflection)
	{
		try
		{
			super.startEvent(checkReflection);
			_flagTeams = new CaptureFlagTeam[getTeams().size()];
			int i = 0;
			for (final FightEventTeam team : getTeams())
			{
				final CaptureFlagTeam flagTeam = new CaptureFlagTeam();
				flagTeam._team = team;
				flagTeam._holder = spawnNpc(53003, getFlagHolderSpawnLocation(team), 0, false);
				spawnFlag(flagTeam);
				_flagTeams[i] = flagTeam;
				i++;
			}
		}
		catch (final Exception e)
		{
			warn("Error on CaptureTheFlag startEvent!", e);
		}
	}
	
	@Override
	public void stopEvent()
	{
		try
		{
			super.stopEvent();
			for (final CaptureFlagTeam iFlagTeam : _flagTeams)
			{
				if (iFlagTeam._flag != null)
				{
					iFlagTeam._flag.deleteMe();
				}
				if (iFlagTeam._holder != null)
				{
					iFlagTeam._holder.deleteMe();
				}
				if (iFlagTeam._thisTeamHolder != null && iFlagTeam._thisTeamHolder._enemyFlagHoldByPlayer != null)
				{
					iFlagTeam._thisTeamHolder._enemyFlagHoldByPlayer.despawnObject();
				}
			}
			_flagTeams = null;
		}
		catch (final Exception e)
		{
			warn("Error on CaptureTheFlag stopEvent!", e);
		}
	}
	
	private class CaptureFlagTeam
	{
		private FightEventTeam _team;
		private Npc _holder;
		private Npc _flag;
		private CaptureFlagHolder _thisTeamHolder;
	}
	
	private class CaptureFlagHolder
	{
		private FightEventPlayer _playerHolding;
		private CTFCombatFlagObject _enemyFlagHoldByPlayer;
		private FightEventTeam _teamFlagOwner;
	}

	public boolean tryToTakeFlag(Player player, Npc flag)
	{
		try
		{
			final FightEventPlayer fPlayer = getFightEventPlayer(player);
			if (fPlayer == null)
			{
				return false;
			}
			if (getState() != EVENT_STATE.STARTED)
			{
				return false;
			}
			
			CaptureFlagTeam flagTeam = null;
			for (final CaptureFlagTeam iFlagTeam : _flagTeams)
			{
				if (iFlagTeam._flag != null && iFlagTeam._flag.equals(flag))
				{
					flagTeam = iFlagTeam;
				}
			}
			
			if (fPlayer.getTeam().equals(flagTeam._team))
			{
				giveFlagBack(fPlayer, flagTeam);
				return false;
			}
			else
			{
				return getEnemyFlag(fPlayer, flagTeam);
			}
		}
		catch (final Exception e)
		{
			warn("Error on CaptureTheFlag tryToTakeFlag!", e);
			return false;
		}
	}
	
	public void talkedWithFlagHolder(Player player, Npc holder)
	{
		try
		{
			final FightEventPlayer fPlayer = getFightEventPlayer(player);
			if (fPlayer == null)
			{
				return;
			}
			if (getState() != EVENT_STATE.STARTED)
			{
				return;
			}
			
			CaptureFlagTeam flagTeam = null;
			for (final CaptureFlagTeam iFlagTeam : _flagTeams)
			{
				if (iFlagTeam._holder != null && iFlagTeam._holder.equals(holder))
				{
					flagTeam = iFlagTeam;
				}
			}
			
			if (fPlayer.getTeam().equals(flagTeam._team))
			{
				giveFlagBack(fPlayer, flagTeam);
			}
		}
		catch (final Exception e)
		{
			warn("Error on CaptureTheFlag talkedWithFlagHolder!", e);
		}
	}
	
	private boolean getEnemyFlag(FightEventPlayer fPlayer, CaptureFlagTeam enemyFlagTeam)
	{
		try
		{
			final CaptureFlagTeam goodTeam = getTeam(fPlayer.getTeam());
			final Player player = fPlayer.getPlayer();
			
			if (enemyFlagTeam._flag != null)
			{
				enemyFlagTeam._flag.deleteMe();
				enemyFlagTeam._flag = null;
				
				final CTFCombatFlagObject flag = new CTFCombatFlagObject();
				flag.spawnObject(player);
				
				final CaptureFlagHolder holder = new CaptureFlagHolder();
				holder._enemyFlagHoldByPlayer = flag;
				holder._playerHolding = fPlayer;
				holder._teamFlagOwner = enemyFlagTeam._team;
				goodTeam._thisTeamHolder = holder;
				
				sendMessageToTeam(enemyFlagTeam._team, MESSAGE_TYPES.CRITICAL, "FightEvents.STOLEN_FLAG");
				
				for (final FightEventPlayer iFPlayer : goodTeam._team.getPlayers())
				{
					final ServerMessage msg = new ServerMessage("FightEvents.PLAYER_STOLEN", iFPlayer.getPlayer().getLang());
					msg.add(new ServerMessage("FightEvents." + enemyFlagTeam._team.getName() + "", iFPlayer.getPlayer().getLang()).toString());
					sendMessageToPlayer(iFPlayer.getPlayer(), MESSAGE_TYPES.CRITICAL, msg);
				}
				return true;
			}
			return false;
		}
		catch (final Exception e)
		{
			warn("Error on CaptureTheFlag talkedWithFlagHolder!", e);
			return false;
		}
	}
	
	private CaptureFlagTeam getTeam(FightEventTeam team)
	{
		if (team == null)
		{
			return null;
		}
		try
		{
			for (final CaptureFlagTeam iFlagTeam : _flagTeams)
			{
				if (iFlagTeam._team != null && iFlagTeam._team.equals(team))
				{
					return iFlagTeam;
				}
			}
			return null;
		}
		catch (final Exception e)
		{
			warn("Error on CaptureTheFlag getTeam!", e);
			return null;
		}
	}

	private void giveFlagBack(FightEventPlayer fPlayer, CaptureFlagTeam flagTeam)
	{
		try
		{
			final CaptureFlagHolder holdingTeam = flagTeam._thisTeamHolder;
			if (holdingTeam != null && fPlayer.equals(holdingTeam._playerHolding))
			{
				holdingTeam._enemyFlagHoldByPlayer.despawnObject();
				
				spawnFlag(getTeam(holdingTeam._teamFlagOwner));
				
				flagTeam._thisTeamHolder = null;
				flagTeam._team.incScore(1);
				updateScreenScores();

				for (final FightEventTeam team : getTeams())
				{
					if (!team.equals(flagTeam._team))
					{
						sendMessageToTeam(holdingTeam._teamFlagOwner, MESSAGE_TYPES.CRITICAL, "FightEvents.GAIN_STORE", flagTeam._team);
					}
				}
				sendMessageToTeam(flagTeam._team, MESSAGE_TYPES.CRITICAL, "FightEvents.YOU_GAIN");
				fPlayer.increaseEventSpecificScore("capture");
				if (_captureFlagLimit > 0 && flagTeam._team.getScore() >= _captureFlagLimit)
				{
					stopEvent();
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error on CaptureTheFlag giveFlagBack!", e);
		}
	}
	
	private Location getFlagHolderSpawnLocation(FightEventTeam team)
	{
		return getMap().getKeyLocations()[team.getIndex() - 1];
	}
	
	private void spawnFlag(CaptureFlagTeam flagTeam)
	{
		try
		{
			final Npc flag = spawnNpc((53000 + flagTeam._team.getIndex()), getFlagHolderSpawnLocation(flagTeam._team), 0, false);
			flagTeam._flag = flag;
		}
		catch (final Exception e)
		{
			warn("Error on CaptureTheFlag spawnFlag!", e);
		}
	}
	
	@Override
	public String getVisibleTitle(Player player, Player viewer, String currentTitle, boolean toMe)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);
		
		if (fPlayer == null)
		{
			return currentTitle;
		}
		final ServerMessage msg = new ServerMessage("FightEvents.TITLE_INFO", viewer.getLang());
		msg.add(fPlayer.getKills());
		msg.add(fPlayer.getDeaths());
		return msg.toString();
	}
	
	@Override
	public boolean leaveEvent(Player player, boolean teleportTown, boolean isDestroy, boolean isLogOut)
	{
		respawnFlag(getFightEventPlayer(player));
		return super.leaveEvent(player, teleportTown, isDestroy, isLogOut);
	}
	
	@Override
	public void loggedOut(Player player)
	{
		respawnFlag(getFightEventPlayer(player));
		super.loggedOut(player);
	}
	
	private void respawnFlag(FightEventPlayer fPlayer)
	{
		if (fPlayer != null && getState() == EVENT_STATE.STARTED)
		{
			final CaptureFlagTeam flagTeam = getTeam(fPlayer.getTeam());
			if (flagTeam != null && flagTeam._thisTeamHolder != null && flagTeam._thisTeamHolder._playerHolding.equals(fPlayer))
			{
				final CaptureFlagHolder holdingTeam = flagTeam._thisTeamHolder;
				if (holdingTeam != null && fPlayer.equals(holdingTeam._playerHolding))
				{
					holdingTeam._enemyFlagHoldByPlayer.despawnObject();
				}
				spawnFlag(getTeam(flagTeam._thisTeamHolder._teamFlagOwner));
				flagTeam._thisTeamHolder = null;
			}
		}
	}
	
	@Override
	protected void giveItemRewardsForPlayer(FightEventPlayer fPlayer, Map<Integer, Long> rewards, boolean isTopKiller, boolean isLogOut)
	{
		if (fPlayer == null)
		{
			return;
		}
		
		if (rewards == null)
		{
			rewards = new HashMap<>();
		}
		
		if (_rewardByCaptureFlag != null && _rewardByCaptureFlag.length > 0)
		{
			for (final int[] item : _rewardByCaptureFlag)
			{
				if ((item == null) || (item.length != 2))
				{
					continue;
				}
				
				if (rewards.containsKey(item[0]))
				{
					final long amount = rewards.get(item[0]) + (item[1] * (!_rewardByCaptureFlagAlone ? fPlayer.getTeam().getScore() : fPlayer.getEventSpecificScore("capture")));
					rewards.put(item[0], amount);
				}
				else
				{
					rewards.put(item[0], (long) (item[1] * (!_rewardByCaptureFlagAlone ? fPlayer.getTeam().getScore() : fPlayer.getEventSpecificScore("capture"))));
				}
			}
		}
		super.giveItemRewardsForPlayer(fPlayer, rewards, isTopKiller, isLogOut);
	}
}