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

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.DefenderInstance;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.skills.Skill;

public class DoorAI extends CharacterAI
{
	public DoorAI(DoorInstance accessor)
	{
		super(accessor);
	}

	@Override
	protected void onIntentionIdle()
	{
	}

	@Override
	protected void onIntentionActive()
	{
	}

	@Override
	protected void onIntentionRest()
	{
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
	protected void onIntentionMoveTo(Location destination, int offset)
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
	protected void onEvtThink()
	{
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final var me = (DoorInstance) _actor;
		ThreadPoolManager.getInstance().execute(new onEventAttackedDoorTask(me, attacker));
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
	protected void onEvtReadyToAct()
	{
	}

	@Override
	protected void onEvtUserCmd(Object arg0, Object arg1)
	{
	}

	@Override
	protected void onEvtArrived()
	{
	}

	@Override
	protected void onEvtArrivedTarget()
	{
	}

	@Override
	protected void onEvtArrivedBlocked(Location blocked_at_loc)
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
	
	private class onEventAttackedDoorTask implements Runnable
	{
		private final DoorInstance _door;
		private final Creature _attacker;

		public onEventAttackedDoorTask(DoorInstance door, Creature attacker)
		{
			_door = door;
			_attacker = attacker;
		}

		@Override
		public void run()
		{
			for (final DefenderInstance guard : _door.getKnownDefenders())
			{
				if (_actor.isInsideRadius(guard, guard.getFaction().getRange(), false, true) && (Math.abs(_attacker.getZ() - guard.getZ()) < 200))
				{
					if (Rnd.chance(20))
					{
						guard.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 10000);
					}
					else
					{
						guard.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 2000);
					}
				}
			}
		}
	}
}