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
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;

/**
 * Created by LordWinter 21.09.2018
 */
public class FairyTree extends Fighter
{
	public FairyTree(Attackable actor)
	{
		super(actor);
		actor.setIsImmobilized(true);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (attacker != null)
		{
			if (attacker.isPlayer())
			{
				final Skill skill = SkillsParser.getInstance().getInfo(5423, 12);
				skill.getEffects(actor, attacker, false);
				return;
			}
			else if (attacker.isPet())
			{
				super.onEvtAttacked(attacker, damage);
				return;
			}
			
		}
	}

	@Override
	protected void onEvtAggression(Creature attacker, int aggro)
	{
		final Attackable actor = getActiveChar();
		if ((attacker != null) && attacker.isPlayer())
		{
			final Skill skill = SkillsParser.getInstance().getInfo(5423, 12);
			skill.getEffects(actor, attacker, false);
			return;
		}
	}
}
