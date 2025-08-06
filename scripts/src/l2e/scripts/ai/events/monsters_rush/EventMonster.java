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
package l2e.scripts.ai.events.monsters_rush;

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;

public class EventMonster extends Fighter
{
	public EventMonster(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		for (final Npc npc : World.getInstance().getAroundNpc(getActiveChar()))
		{
			if ((npc != null) && (npc.getId() == 53006))
			{
				getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, npc, 100);
				getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, npc, 100);
			}
		}
		return true;
	}

	@Override
	protected boolean checkAggression(Creature target)
	{
		if (target.isPlayable())
		{
			return false;
		}
		for (final Npc npc : World.getInstance().getAroundNpc(getActiveChar()))
		{
			if ((npc != null) && (npc.getId() == 53006))
			{
				getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, npc, 300);
			}
		}
		return super.checkAggression(target);
	}
}
