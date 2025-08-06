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

import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Created by LordWinter 10.12.2018
 */
public class KaimAbigoreFollower extends Fighter
{
	public KaimAbigoreFollower(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();
		final Creature attacker = getAttackTarget();
		if (attacker != null && actor.getDistance(attacker) < 50)
		{
			actor.setTarget(attacker);
			actor.doCast(SkillsParser.getInstance().getInfo(4614, 6));
			actor.doDie(null);
		}
		super.thinkAttack();
	}
}