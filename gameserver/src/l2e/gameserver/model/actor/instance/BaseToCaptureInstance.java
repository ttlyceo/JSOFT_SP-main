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
package l2e.gameserver.model.actor.instance;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.model.impl.BaseCaptureEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventTeam;
import l2e.gameserver.model.skills.Skill;

public class BaseToCaptureInstance extends Npc
{
	private BaseCaptureEvent _event;
	private FightEventTeam _team;
	
	public BaseToCaptureInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
	}
	
	@Override
	public boolean isAutoAttackable(Creature creature, boolean isPoleAttack)
	{
		final Player player = creature.getActingPlayer();
		if (player == null)
		{
			return false;
		}
		
		final var realActor = _event.getFightEventPlayer(player);
		if (realActor == null)
		{
			return false;
		}
		return !_team.equals(realActor.getTeam());
	}
	
	public boolean canAttack(Player player)
	{
		final var realActor = _event.getFightEventPlayer(player);
		if (realActor == null)
		{
			return false;
		}
		return !_team.equals(realActor.getTeam());
	}
	
	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		final var realActor = _event.getFightEventPlayer(player);
		if (realActor == null)
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else if (interact)
		{
			if (!isAlikeDead() && !_team.equals(realActor.getTeam()) && Math.abs(player.getZ() - getZ()) < 400)
			{
				player.getAI().setIntention(CtrlIntention.ATTACK, this);
			}
		}
		player.sendActionFailed();
	}
	
	@Override
	public void onForcedAttack(Player player, boolean shift)
	{
		onAction(player, false, shift);
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		final Player player = attacker.getActingPlayer();
		if (player == null)
		{
			return;
		}

		if (!canAttack(player))
		{
			return;
		}

		if (damage < getStatus().getCurrentHp())
		{
			getStatus().setCurrentHp(getStatus().getCurrentHp() - damage);
		}
		else
		{
			doDie(attacker);
		}
	}

	@Override
	protected void onDeath(Creature killer)
	{
		final Player player = killer.getActingPlayer();
		if (player == null)
		{
			return;
		}
		_event.destroyBase(player, this);
		super.onDeath(killer);
	}
	
	public void setIsInEvent(BaseCaptureEvent event)
	{
		_event = event;
	}
	
	public BaseCaptureEvent getEvent()
	{
		return _event;
	}
	
	public void setEventTeam(FightEventTeam team)
	{
		_team = team;
	}
	
	public FightEventTeam getEventTeam()
	{
		return _team;
	}
	
	@Override
	public boolean isHealBlocked()
	{
		return true;
	}
	
	@Override
	public boolean isBuffImmune()
	{
		return true;
	}
	
	@Override
	public boolean isDebuffImmune()
	{
		return true;
	}
}