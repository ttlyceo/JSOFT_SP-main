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
package l2e.gameserver.ai.character;

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Vehicle;
import l2e.gameserver.model.skills.Skill;

public abstract class VehicleAI extends CharacterAI
{
	public VehicleAI(Vehicle actor)
	{
		super(actor);
	}
	
	@Override
	protected void onIntentionAttack(Creature target, boolean shift)
	{
	}

	@Override
	protected void onIntentionCast(Skill skill, GameObject target)
	{
	}

	@Override
	protected void onIntentionFollow(Creature target)
	{
	}

	@Override
	protected void onIntentionPickUp(GameObject item)
	{
	}

	@Override
	protected void onIntentionInteract(GameObject object)
	{
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
	}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
	}

	@Override
	protected void onEvtStunned(Creature attacker)
	{
	}

	@Override
	protected void onEvtSleeping(Creature attacker)
	{
	}

	@Override
	protected void onEvtRooted(Creature attacker)
	{
	}

	@Override
	protected void onEvtForgetObject(GameObject object)
	{
	}

	@Override
	protected void onEvtCancel()
	{
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
	}

	@Override
	protected void onEvtFakeDeath()
	{
	}

	@Override
	protected void onEvtFinishCasting()
	{
	}

	@Override
	protected void clientActionFailed()
	{
	}

	@Override
	protected void moveToPawn(GameObject pawn, int offset)
	{
	}
	
	@Override
	protected void clientStoppedMoving()
	{
	}
}