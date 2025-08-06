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
package l2e.scripts.ai.kamaloka;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Attackable;

/**
 * Created by LordWinter 13.02.2020
 */
public class WarlordTamuzeFollower extends Fighter
{
	public WarlordTamuzeFollower(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();
		
		if (getAttackTarget() != null)
		{
			if (actor.getCurrentHp() < actor.getMaxHp() * 0.5)
			{
				if (actor.getDistance(getAttackTarget()) < 80 && Rnd.calcChance(20))
				{
					actor.setTarget(getAttackTarget());
					actor.doCast(SkillsParser.getInstance().getInfo(4139, 6));
					actor.doDie(null);
				}
			}
		}
		super.thinkAttack();
	}
}