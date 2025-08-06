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

import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.quest.Quest;

public class Aenkinel extends Fighter
{
	public Aenkinel(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable actor = getActiveChar();
		if (actor.getId() == 25694)
		{
			for (int i = 0; i < 4; i++)
			{
				Quest.addSpawn(18820, actor.getLocation(), actor.getReflection(), 250);
			}
		}
		else if (actor.getId() == 25695)
		{
			for (int i = 0; i < 4; i++)
			{
				Quest.addSpawn(18823, actor.getLocation(), actor.getReflection(), 250);
			}
		}
		super.onEvtDead(killer);
	}
}