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

import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.instancemanager.HandysBlockCheckerManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.BlockInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.network.SystemMessageId;

public class Dummy implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.DUMMY
	};

	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		switch (skill.getId())
		{
			case 5852 :
			case 5853 :
			{
				final var obj = targets[0];
				if (obj != null)
				{
					useBlockCheckerSkill(activeChar.getActingPlayer(), skill, obj);
				}
				break;
			}
			case 254 :
			case 302 :
				if (skill.hasEffects())
				{
					for (final var tgt : targets)
					{
						if (tgt == null || !tgt.isCreature())
						{
							continue;
						}
						final var target = (Creature) tgt;
						if (target != null)
						{
							skill.getEffects(activeChar, target, true);
						}
					}
				}
				break;
			default :
			{
				if (skill.isDebuff() && !skill.isIgnoreCalcChance())
				{
					final boolean ss = skill.useSoulShot() && activeChar.isChargedShot(ShotType.SOULSHOTS);
					final boolean sps = skill.useSpiritShot() && activeChar.isChargedShot(ShotType.SPIRITSHOTS);
					final boolean bss = skill.useSpiritShot() && activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
					for (final var tgt : targets)
					{
						if (tgt == null || !tgt.isCreature())
						{
							continue;
						}
						var target = (Creature) tgt;
						if (target != null)
						{
							byte shld = 0;
							if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
							{
								target = activeChar;
							}
							
							shld = Formulas.calcShldUse(activeChar, target, skill);
							if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
							{
								final var effects = skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss), true);
								final var summon = target.getSummon();
								if ((summon != null) && (summon != activeChar) && summon.isServitor() && (effects.length > 0) && !skill.isTriggeredSkill())
								{
									if (effects[0].canBeStolen() || skill.isHeroSkill() || skill.isStatic())
									{
										skill.getEffects(activeChar, summon, new Env(shld, ss, sps, bss), true);
									}
								}
							}
							else
							{
								activeChar.sendPacket(SystemMessageId.ATTACK_FAILED);
							}
							Formulas.calcLethalHit(activeChar, target, skill);
						}
					}
				}
				else
				{
					if (skill.hasEffects())
					{
						for (final var tgt : targets)
						{
							if (tgt == null || !tgt.isCreature())
							{
								continue;
							}
							final var target = (Creature) tgt;
							if (target != null)
							{
								skill.getEffects(activeChar, target, true);
							}
						}
					}
				}
				break;
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
		
		if (skill.useSpiritShot())
		{
			activeChar.setChargedShot(activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
	
	private final void useBlockCheckerSkill(Player activeChar, Skill skill, GameObject target)
	{
		if (!(target instanceof BlockInstance))
		{
			return;
		}
		
		final var block = (BlockInstance) target;
		
		final int arena = activeChar.getBlockCheckerArena();
		if (arena != -1)
		{
			final var holder = HandysBlockCheckerManager.getInstance().getHolder(arena);
			if (holder == null)
			{
				return;
			}
			
			final int team = holder.getPlayerTeam(activeChar);
			final int color = block.getColorEffect();
			if ((team == 0) && (color == 0x00))
			{
				block.changeColor(activeChar, holder, team);
			}
			else if ((team == 1) && (color == 0x53))
			{
				block.changeColor(activeChar, holder, team);
			}
		}
	}
}