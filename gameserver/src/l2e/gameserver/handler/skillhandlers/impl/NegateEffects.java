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
package l2e.gameserver.handler.skillhandlers.impl;

import l2e.commons.util.Rnd;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;


public class NegateEffects implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.NEGATE_EFFECTS
	};

	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		byte shld = 0;
		final boolean ss = skill.useSoulShot() && activeChar.isChargedShot(ShotType.SOULSHOTS);
		final boolean sps = skill.useSpiritShot() && activeChar.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.useSpiritShot() && activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);

		for (final var obj : targets)
		{
			if (!(obj instanceof Creature))
			{
				continue;
			}
			
			var target = (Creature) obj;
			if (target.isDead() || (target.isInvul() && !target.isParalyzed()))
			{
				continue;
			}
			
			if (skill.getNegateAbnormalTypes() != null && !skill.getNegateAbnormalTypes().isEmpty())
			{
				negateEffects(activeChar, target, skill);
			}
			
			shld = Formulas.calcShldUse(activeChar, target, skill);
			
			if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
			{
				target = activeChar;
			}
			
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
			{
				skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss), true);
			}
		}
		
		if (skill.hasSelfEffects())
		{
			final var effect = activeChar.getFirstEffect(skill.getId());
			if ((effect != null) && effect.isSelfEffect())
			{
				effect.exit();
			}
			skill.getEffectsSelf(activeChar);
		}
		activeChar.setChargedShot(bss ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
	}
	
	private void negateEffects(Creature activeChar, Creature target, Skill skill)
	{
		for (final var value : skill.getNegateAbnormalTypes().entrySet())
		{
			final var stackType = value.getKey();
			final var stackOrder = value.getValue();
			final var skillCast = skill.getId();
			
			for (final var e : target.getAllEffects())
			{
				if (!e.getSkill().canBeDispeled())
				{
					continue;
				}
				
				if (stackType.equalsIgnoreCase(e.getAbnormalType()) && (e.getSkill().getId() != skillCast) && Rnd.chance(skill.getNegateRate()))
				{
					if (e.getSkill() != null)
					{
						if (e.triggersChanceSkill())
						{
							target.removeChanceEffect(e);
						}
						
						if (stackOrder == -1)
						{
							target.stopSkillEffects(e.getSkill().getId());
						}
						else if (stackOrder >= e.getAbnormalLvl())
						{
							target.stopSkillEffects(e.getSkill().getId());
						}
					}
				}
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}