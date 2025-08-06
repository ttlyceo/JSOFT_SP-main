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
import l2e.gameserver.model.actor.instance.BaseToCaptureInstance;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.entity.events.model.template.FightEventTeam;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public class BaseCaptureEvent extends AbstractFightEvent
{
	private CaptureBaseTeam[] _baseTeams;
	private final int[][] _rewardByCaptureBase;
	private final boolean _calcWinnerByDamage;
	private boolean _isEnd = false;
	
	public BaseCaptureEvent(MultiValueSet<String> set)
	{
		super(set);
		
		_rewardByCaptureBase = parseItemsList(set.getString("rewardByCaptureBase", null));
		_calcWinnerByDamage = set.getBool("calcWinnerByDamage", false);
	}
	
	@Override
	public void onKilled(Creature actor, Creature victim)
	{
		try
		{
			if (actor != null && actor.isPlayer())
			{
				final var realActor = getFightEventPlayer(actor.getActingPlayer());
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
				final var realVictim = getFightEventPlayer(victim);
				realVictim.increaseDeaths();
				if (actor != null)
				{
					final ServerMessage msg = new ServerMessage("FightEvents.YOU_KILLED", realVictim.getPlayer().getLang());
					msg.add(actor.getName(null));
					sendMessageToPlayer(realVictim.getPlayer(), MESSAGE_TYPES.GM, msg);
				}
				victim.getActingPlayer().broadcastCharInfo();
			}
			super.onKilled(actor, victim);
		}
		catch (final Exception e)
		{
			warn("Error on CaptureBase OnKilled!", e);
		}
	}

	@Override
	public boolean canAttack(Creature target, Creature attacker)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
		}

		final var player = attacker.getActingPlayer();
		if (player == null)
		{
			return true;
		}
		
		if (target instanceof BaseToCaptureInstance)
		{
			for (final CaptureBaseTeam iBaseTeam : _baseTeams)
			{
				if (iBaseTeam._base != null && iBaseTeam._base.equals(target))
				{
					final var fPlayer = getFightEventPlayer(player);
					if (fPlayer != null)
					{
						if (fPlayer.getTeam().equals(iBaseTeam._team))
						{
							return false;
						}
						return true;
					}
				}
			}
		}

		if (isTeamed())
		{
			final var targetFPlayer = getFightEventPlayer(target);
			final var attackerFPlayer = getFightEventPlayer(attacker);

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

	@Override
	public boolean canUseMagic(Creature target, Creature attacker, Skill skill)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
		}

		if (target instanceof BaseToCaptureInstance)
		{
			for (final var iBaseTeam : _baseTeams)
			{
				if (iBaseTeam._base != null && iBaseTeam._base.equals(target))
				{
					final FightEventPlayer fPlayer = getFightEventPlayer(attacker);
					if (fPlayer != null)
					{
						if (fPlayer.getTeam().equals(iBaseTeam._team))
						{
							return false;
						}
						return true;
					}
				}
			}
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
		
		if (isTeamed())
		{
			final var targetFPlayer = getFightEventPlayer(target);
			final var attackerFPlayer = getFightEventPlayer(attacker);
			
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
	
	@Override
	public void startEvent(boolean checkReflection)
	{
		try
		{
			super.startEvent(checkReflection);
			_baseTeams = new CaptureBaseTeam[getTeams().size()];
			int i = 0;
			for (final FightEventTeam team : getTeams())
			{
				final CaptureBaseTeam baseTeam = new CaptureBaseTeam();
				baseTeam._team = team;
				spawnBase(baseTeam);
				_baseTeams[i] = baseTeam;
				if (baseTeam._base != null)
				{
					baseTeam._base.setIsInEvent(this);
					baseTeam._base.setEventTeam(team);
				}
				i++;
			}
		}
		catch (final Exception e)
		{
			warn("Error on CaptureBase startEvent!", e);
		}
	}
	
	@Override
	public void stopEvent()
	{
		try
		{
			if (_calcWinnerByDamage && !_isEnd)
			{
				CaptureBaseTeam baseTeam = null;
				final double[] hp = new double[2];
				final double[] hpPercent = new double[2];
				boolean isValid = true;
				int i = 0;
				for (final var iBaseTeam : _baseTeams)
				{
					if (iBaseTeam._base == null)
					{
						isValid = false;
						break;
					}
					
					hpPercent[i] = iBaseTeam._base.getCurrentHpPercents();
					hp[i] = iBaseTeam._base.getCurrentHp();
					i++;
				}
				
				if (isValid)
				{
					baseTeam = hpPercent[0] == hpPercent[1] ? hp[0] > hp[1] ? _baseTeams[0] : _baseTeams[1] : hpPercent[0] > hpPercent[1] ? _baseTeams[0] : _baseTeams[1];
				}
				
				if (baseTeam != null)
				{
					final var winner = baseTeam._team;
					if (winner != null)
					{
						winner.incScore(1);
						updateScreenScores();
						
						for (final var team : getTeams())
    					{
    						if (!team.equals(baseTeam._team))
    						{
    							sendMessageToTeam(winner, MESSAGE_TYPES.CRITICAL, "FightEvents.TEAM_WON_EVENT", winner);
    						}
    					}
					}
				}
			}

			super.stopEvent();
			for (final var iBaseTeam : _baseTeams)
			{
				if (iBaseTeam._base != null)
				{
					iBaseTeam._base.deleteMe();
				}
			}
			_isEnd = false;
			_baseTeams = null;
		}
		catch (final Exception e)
		{
			warn("Error on CaptureBase stopEvent!", e);
		}
	}
	
	private class CaptureBaseTeam
	{
		private FightEventTeam _team;
		private BaseToCaptureInstance _base;
	}
	
	public void destroyBase(Player player, Npc holder)
	{
		try
		{
			final var fPlayer = getFightEventPlayer(player);
			if (fPlayer == null)
			{
				return;
			}
			if (getState() != EVENT_STATE.STARTED)
			{
				return;
			}
			
			CaptureBaseTeam baseTeam = null;
			for (final var iBaseTeam : _baseTeams)
			{
				if (iBaseTeam._base != null && iBaseTeam._base.equals(holder))
				{
					baseTeam = iBaseTeam;
				}
			}
			
			if (!fPlayer.getTeam().equals(baseTeam._team))
			{
				fPlayer.getTeam().incScore(1);
				updateScreenScores();

				for (final var team : getTeams())
				{
					if (!team.equals(baseTeam._team))
					{
						sendMessageToTeam(fPlayer.getTeam(), MESSAGE_TYPES.CRITICAL, "FightEvents.TEAM_WON_EVENT", fPlayer.getTeam());
					}
				}
				sendMessageToTeam(fPlayer.getTeam(), MESSAGE_TYPES.CRITICAL, "FightEvents.CAPTURE_BASE");
				
				fPlayer.increaseEventSpecificScore("capture");
				_isEnd = true;
				stopEvent();
			}
		}
		catch (final Exception e)
		{
			warn("Error on CaptureBase destroyBase!", e);
		}
	}
	
	private Location getBaseSpawnLocation(FightEventTeam team)
	{
		return getMap().getKeyLocations()[team.getIndex() - 1];
	}
	
	private void spawnBase(CaptureBaseTeam baseTeam)
	{
		try
		{
			final BaseToCaptureInstance base = (BaseToCaptureInstance) spawnNpc((53003 + baseTeam._team.getIndex()), getBaseSpawnLocation(baseTeam._team), 0, false);
			baseTeam._base = base;
		}
		catch (final Exception e)
		{
			warn("Error on CaptureBase spawnBase!", e);
		}
	}
	
	@Override
	public String getVisibleTitle(Player player, Player viewer, String currentTitle, boolean toMe)
	{
		final var fPlayer = getFightEventPlayer(player);
		if (fPlayer == null)
		{
			return currentTitle;
		}
		final var msg = new ServerMessage("FightEvents.TITLE_INFO", viewer.getLang());
		msg.add(fPlayer.getKills());
		msg.add(fPlayer.getDeaths());
		return msg.toString();
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
		
		if (_rewardByCaptureBase != null && _rewardByCaptureBase.length > 0)
		{
			for (final int[] item : _rewardByCaptureBase)
			{
				if ((item == null) || (item.length != 2))
				{
					continue;
				}
				
				if (rewards.containsKey(item[0]))
				{
					final long amount = rewards.get(item[0]) + (item[1] * fPlayer.getTeam().getScore());
					rewards.put(item[0], amount);
				}
				else
				{
					rewards.put(item[0], (long) (item[1] * fPlayer.getTeam().getScore()));
				}
			}
		}
		super.giveItemRewardsForPlayer(fPlayer, rewards, isTopKiller, isLogOut);
	}
}