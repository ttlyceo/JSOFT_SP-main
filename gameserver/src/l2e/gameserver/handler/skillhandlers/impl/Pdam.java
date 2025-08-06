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

import l2e.commons.log.Log;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Pdam implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.PDAM, SkillType.FATAL
	};
	
	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		int damage = 0;
		final boolean ss = skill.useSoulShot() && activeChar.isChargedShot(ShotType.SOULSHOTS);
		
		for (final GameObject obj : targets)
		{
			if (!(obj instanceof Creature))
			{
				continue;
			}
			final Creature target = (Creature) obj;
			if (target.isPlayer() && target.getActingPlayer().isFakeDeathNow())
			{
				target.stopFakeDeath(true);
			}
			else if (target.isDead())
			{
				continue;
			}
			
			final byte shld = Formulas.calcShldUse(activeChar, target, skill);
			boolean crit = false;
			
			if (!skill.isStaticDamage() && (skill.getBaseCritRate() > 0))
			{
				crit = Rnd.chance(Formulas.calcCrit(activeChar, target, skill, false));
			}
			
			if (!crit && ((skill.getCondition() & Skill.COND_CRIT) != 0))
			{
				damage = 0;
			}
			else
			{
				damage = skill.isStaticDamage() ? (int) skill.getPower() : (int) Formulas.calcPhysDam(activeChar, target, skill, shld, false, ss);
			}
			if (!skill.isStaticDamage() && (skill.getMaxSoulConsumeCount() > 0) && activeChar.isPlayer())
			{
				final int chargedSouls = (activeChar.getActingPlayer().getChargedSouls() <= skill.getMaxSoulConsumeCount()) ? activeChar.getActingPlayer().getChargedSouls() : skill.getMaxSoulConsumeCount();
				damage *= 1 + (chargedSouls * 0.04);
			}
			if (crit)
			{
				damage *= 2;
			}
			
			final byte reflect = Formulas.calcSkillReflect(target, skill);
			
			if (!Formulas.calcPhysicalSkillEvasion(activeChar, target, skill))
			{
				boolean allowDamage = true;
				if (skill.getId() == 450 && (skill.getLevel() < 300 || skill.getLevel() > 330))
				{
					allowDamage = false;
				}
				
				if (damage > 0 && allowDamage)
				{
					final double drainPercent = activeChar.calcStat(Stats.DRAIN_PERCENT, 0, null, null);
					if (drainPercent != 0)
					{
						Formulas.calcDrainDamage(activeChar, target, damage, 0, drainPercent);
					}
					activeChar.sendDamageMessage(target, damage, skill, false, crit, false);
					target.reduceCurrentHp(damage, activeChar, skill);
					Formulas.calcStunBreak(target, crit);
					if (Config.LOG_GAME_DAMAGE && activeChar.isPlayable() && (damage > Config.LOG_GAME_DAMAGE_THRESHOLD))
					{
						Log.addLogDamage(damage, "PDAM", skill, activeChar.getActingPlayer(), target);
					}
					
					if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
					{
						Formulas.calcReflectDamage(activeChar, target, skill, damage, crit);
					}
					
					if (skill.hasEffects())
					{
						if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0)
						{
							activeChar.stopSkillEffects(skill.getId());
							skill.getEffects(activeChar, target, true);
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
						else
						{
							skill.getEffects(activeChar, target, new Env(shld, false, false, false), true);
						}
					}
				}
				else
				{
					if (allowDamage)
					{
						activeChar.sendPacket(SystemMessageId.ATTACK_FAILED);
					}
				}
			}
			Formulas.calcLethalHit(activeChar, target, skill);
		}
		
		if (skill.hasSelfEffects())
		{
			final Effect effect = activeChar.getFirstEffect(skill.getId());
			if ((effect != null) && effect.isSelfEffect())
			{
				effect.exit();
			}
			skill.getEffectsSelf(activeChar);
		}
		activeChar.setChargedShot(ShotType.SOULSHOTS, false);
		
		if (skill.isSuicideAttack())
		{
			activeChar.doDie(activeChar);
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}