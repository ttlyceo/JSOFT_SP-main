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

import java.util.ArrayList;
import java.util.List;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DominationInstance;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.entity.events.model.template.FightEventTeam;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public class DominationEvent extends AbstractFightEvent
{
	private final List<DominationBase> _bases = new ArrayList<>();
	
	public DominationEvent(MultiValueSet<String> set)
	{
		super(set);
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
				actor.getActingPlayer().sendUserInfo();
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

		final Player player = attacker.getActingPlayer();
		if (player == null)
		{
			return true;
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

	@Override
	public boolean canUseMagic(Creature target, Creature attacker, Skill skill)
	{
		if (_state != EVENT_STATE.STARTED)
		{
			return false;
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
	
	@Override
	public void startEvent(boolean checkReflection)
	{
		try
		{
			super.startEvent(checkReflection);
			for (final var loc : getMap().getKeyLocations())
			{
				final DominationBase base = new DominationBase();
				base._base = (DominationInstance) spawnNpc(53017, loc, 0, false);
				if (base._base != null)
				{
					base._base.setIsInEvent(this);
					if (base._base.getAI() != null)
					{
						base._base.getAI().enableAI();
					}
					_bases.add(base);
				}
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
			super.stopEvent();
			for (final DominationBase base : _bases)
			{
				if (base._base != null)
				{
					base._base.deleteMe();
				}
			}
			_bases.clear();
		}
		catch (final Exception e)
		{
			warn("Error on CaptureBase stopEvent!", e);
		}
	}
	
	private class DominationBase
	{
		private DominationInstance _base;
	}
	
	public void setTeamPoint(FightEventTeam team)
	{
		team.incScore(1);
		updateScreenScores();
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
}