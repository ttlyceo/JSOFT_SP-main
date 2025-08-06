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
package l2e.scripts.ai;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Priest;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.scripts.ai.grandboss.QueenAnt;

public class QueenAntNurse extends Priest
{
	public QueenAntNurse(Attackable actor)
	{
		super(actor);
		
		MAX_PURSUE_RANGE = 10000;
		actor.setIsGlobalAI(true);
	}
	
	@Override
	protected void onEvtSpawn()
	{
		getActiveChar().getAI().enableAI();
		super.onEvtSpawn();
	}

	@Override
	protected boolean thinkActive()
	{
		final var actor = getActiveChar();
		if (actor == null || actor.isDead())
		{
			return false;
		}

		final var target = getTopDesireTarget();
		if (target == null)
		{
			return false;
		}

		final var distance = actor.getDistance(target) - target.getColRadius() - actor.getColRadius();
		if (distance > 200)
		{
			moveOrTeleportToLocation(Location.findFrontPosition(target, actor, 100, 150), distance > 2000);
			return false;
		}

		if (!target.isCurrentHpFull())
		{
			return createNewTask();
		}
		return false;
	}

	@Override
	protected boolean createNewTask()
	{
		final var actor = getActiveChar();
		final var target = getTopDesireTarget();
		if (actor == null || actor.isDead() || target == null)
		{
			return false;
		}

		if (!target.isCurrentHpFull())
		{
			final var distance = actor.getDistance(target);
			final var canSee = GeoEngine.getInstance().canSeeTarget(actor, target);
			if (!canSee)
			{
				moveOrTeleportToLocation(target.getLocation(), distance > 2000);
				return true;
			}
			
			final var skill = _healSkills[Rnd.get(_healSkills.length)];
			if (skill.getAOECastRange() < distance)
			{
				moveOrTeleportToLocation(Location.findFrontPosition(target, actor, skill.getAOECastRange() - 30, skill.getAOECastRange() - 10), distance > 2000);
			}
			else
			{
				actor.setTarget(target);
				actor.useMagic(skill);
			}
			return true;
		}
		return false;
	}

	private void moveOrTeleportToLocation(Location loc, boolean teleport)
	{
		final var actor = getActiveChar();
		if (actor == null || actor.isDead())
		{
			return;
		}
		
		actor.setRunning();
		moveTo(loc);
		if (teleport)
		{
			if (!actor.isMovementDisabled() && actor.isMoving())
			{
				return;
			}
			actor.broadcastPacketToOthers(2000, new MagicSkillUse(actor, actor, 2036, 1, 500, 0));
			actor.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), true, actor.getReflection());
		}
	}

	private Npc getTopDesireTarget()
	{
		final var actor = getActiveChar();
		if (actor == null || actor.isDead())
		{
			return null;
		}
		
		final var queenAnt = ((MonsterInstance) actor).getLeader();
		if (queenAnt == null)
		{
			return null;
		}
		
		if (queenAnt.isDead())
		{
			return null;
		}
		
		final var larva = QueenAnt.getLarva();
		if (larva != null && larva.getCurrentHpPercents() < 5)
		{
			return larva;
		}
		return queenAnt;
	}

	@Override
	protected void onIntentionAttack(Creature target, boolean shift)
	{
	}
}