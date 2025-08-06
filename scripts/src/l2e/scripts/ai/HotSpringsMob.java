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

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;

/**
 * Created by LordWinter 16.09.2018
 */
public class HotSpringsMob extends Mystic
{
	public HotSpringsMob(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (attacker != null)
		{
			if (Rnd.chance(5))
			{
				chekDebuff(actor, attacker, 4554);
			}

			if (Rnd.chance(5) && ((actor.getId() == 21317) || (actor.getId() == 21322)))
			{
				chekDebuff(actor, attacker, 4553);
			}

			if (Rnd.chance(5) && ((actor.getId() == 21316) || (actor.getId() == 21319)))
			{
				chekDebuff(actor, attacker, 4552);
			}

			if (Rnd.chance(5) && ((actor.getId() == 21314) || (actor.getId() == 21321)))
			{
				chekDebuff(actor, attacker, 4551);
			}
		}
		super.onEvtAttacked(attacker, damage);
	}

	protected void chekDebuff(Attackable actor, Creature attacker, int debuff)
	{
		final Effect effect = attacker.getFirstEffect(debuff);
		if (effect != null)
		{
			final int level = effect.getSkill().getLevel();
			if (level < 10)
			{
				effect.exit();
				final Skill skill = SkillsParser.getInstance().getInfo(debuff, level + 1);
				skill.getEffects(actor, attacker, false);
			}
		}
		else
		{
			final Skill skill = SkillsParser.getInstance().getInfo(debuff, 1);
			if (skill != null)
			{
				skill.getEffects(actor, attacker, false);
			}
			else
			{
				System.out.println("Skill id " + debuff + " is null, fix it.");
			}
		}
	}
}
