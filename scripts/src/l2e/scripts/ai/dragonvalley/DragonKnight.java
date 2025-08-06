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
package l2e.scripts.ai.dragonvalley;

import l2e.commons.util.NpcUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;

/**
 * Created by LordWinter 13.07.2020
 */
public class DragonKnight extends Fighter
{
	public DragonKnight(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		super.onEvtDead(killer);
		if (killer != null)
		{
			final int chance = getActiveChar().getTemplate().getParameter("helpersSpawnChance", 0);
			switch (getActiveChar().getId())
			{
				case 22844 :
					if (Rnd.chance(chance))
					{
						final MonsterInstance n = NpcUtils.spawnSingle(22845, getActiveChar().getLocation());
						n.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, killer, 2);
					}
					break;
				case 22845 :
					if (Rnd.chance(chance))
					{
						final MonsterInstance n = NpcUtils.spawnSingle(22846, getActiveChar().getLocation());
						n.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, killer, 2);
					}
					break;
			}
		}
	}
}