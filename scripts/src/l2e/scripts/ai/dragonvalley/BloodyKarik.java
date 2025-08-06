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
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;

/**
 * Created by LordWinter 03.09.2020
 */
public class BloodyKarik extends Fighter
{
	public BloodyKarik(Attackable actor)
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
			if (Rnd.chance(chance) && getActiveChar().isScriptValue(0))
			{
				final String[] amount = getActiveChar().getTemplate().getParameter("helpersRndAmount", "4;4").split(";");
				final int rnd = Rnd.get(Integer.parseInt(amount[0]), Integer.parseInt(amount[1]));
				for (int x = 0; x < rnd; x++)
				{
					final MonsterInstance npc = NpcUtils.spawnSingle(22854, Location.findAroundPosition(getActiveChar(), 60, 100));
					npc.setScriptValue(1);
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, killer, 2);
				}
            		}
        	}
    	}
}
