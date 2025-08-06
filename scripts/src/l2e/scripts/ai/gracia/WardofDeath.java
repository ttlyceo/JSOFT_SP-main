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
package l2e.scripts.ai.gracia;

import l2e.commons.util.NpcUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;

/**
 * Created by LordWinter 27.01.2020
 */
public class WardofDeath extends DefaultAI
{
	private static final int[] mobs =
	{
	        22516, 22520, 22522, 22524
	};

	public WardofDeath(Attackable actor)
	{
		super(actor);
		
		actor.setIsImmobilized(true);
	}

	@Override
	protected boolean checkAggression(Creature target)
	{
		final Attackable actor = getActiveChar();
		if (actor == null)
		{
			return false;
		}
		
		if (super.checkAggression(target))
		{
			if (actor.getId() == 18667)
			{
				actor.doCast(SkillsParser.getInstance().getInfo(Rnd.get(5423, 5424), 9));
				actor.doDie(null);
			}
			else if (actor.getId() == 18668)
			{
				for(int i = 0; i < Rnd.get(1, 4); i++)
				{
					final MonsterInstance n = NpcUtils.spawnSingle(mobs[Rnd.get(mobs.length)], Location.findAroundPosition(actor, 60, 100), actor.getReflection(), 0);
					if (target != null)
					{
						n.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, target, 100);
					}
				}
				actor.doDie(null);
			}
			return true;
		}
		return false;
	}
}