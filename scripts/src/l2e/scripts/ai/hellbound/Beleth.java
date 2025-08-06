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
package l2e.scripts.ai.hellbound;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.holders.SkillHolder;

public class Beleth extends Mystic
{
	private long _lastFactioTime = 0;

	public Beleth(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead())
		{
			return;
		}
		
		if (attacker != null && (System.currentTimeMillis() - _lastFactioTime) > _minFactionNotifyInterval)
		{
			if (actor.getDistance(attacker) < 200 && Rnd.chance(1) && !actor.isCastingNow())
			{
				actor.setTarget(attacker);
				actor.doCast(new SkillHolder(5495, 1).getSkill());
			}
			
			_lastFactioTime = System.currentTimeMillis();
			for (final Npc npc : World.getInstance().getAroundNpc(actor, 1000, 400))
			{
				if (npc != null && npc.getId() == 29119)
				{
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100));
				}
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
}