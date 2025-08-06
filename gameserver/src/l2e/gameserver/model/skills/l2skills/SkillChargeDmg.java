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
package l2e.gameserver.model.skills.l2skills;

import l2e.commons.log.Log;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ClassBalanceParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.base.AttackType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.BaseStats;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class SkillChargeDmg extends Skill
{
	public SkillChargeDmg(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(Creature caster, GameObject[] targets)
	{
		final var attacker = caster.getActingPlayer();
		if (attacker == null)
		{
			return;
		}
		
		if (caster.isAlikeDead())
		{
			return;
		}

		final boolean ss = useSoulShot() && caster.isChargedShot(ShotType.SOULSHOTS);
		final boolean sps = useSpiritShot() && attacker.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = useSpiritShot() && attacker.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final double ssBoost = ss ? 2 : 1.0;
		
		for (final Creature target : (Creature[]) targets)
		{
			if (target.isAlikeDead())
			{
				continue;
			}
			
			final boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(caster, target, this);
			if (skillIsEvaded)
			{
				continue;
			}
			
			final boolean isPvP = attacker.isPlayable() && target.isPlayable();
			final boolean isPvE = attacker.isPlayable() && target.isAttackable();
			final boolean isPvEAtt = attacker.isAttackable() && target.isPlayable();
			double attack = attacker.getPAtk(target);
			double defence = target.getPDef(attacker);
			
			if (!ignoreShield())
			{
				final byte shield = Formulas.calcShldUse(attacker, target, this, true);
				switch (shield)
				{
					case Formulas.SHIELD_DEFENSE_FAILED :
					{
						break;
					}
					case Formulas.SHIELD_DEFENSE_SUCCEED :
					{
						defence += target.getShldDef();
						break;
					}
					case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK :
					{
						defence = -1;
						break;
					}
				}
			}
			
			double damage = 1;
			boolean critical = false;
			
			if (defence != -1)
			{
				final double damageMultiplier = Formulas.calcValakasTrait(attacker, target, this) * Formulas.calcWeaponTraitBonus(attacker, target) * Formulas.calcElemental(attacker, target, this);
				
				double weaponTypeBoost = 77;
				final var weapon = attacker.getActiveWeaponItem();
				if ((weapon != null) && weapon.getItemType().isRanged())
				{
					weaponTypeBoost = 70;
				}
				final double energyChargesBoost = (((attacker.getCharges() + getChargeConsume()) - 1) * 0.2) + 1;
				
				attack += getPower(attacker, target, isPvP, isPvE);
				attack *= ssBoost;
				attack *= energyChargesBoost;
				attack *= weaponTypeBoost;
				
				damage = attack / defence;
				damage *= damageMultiplier;
				if (target.isPlayer())
				{
					damage *= attacker.getStat().calcStat(Stats.PVP_PHYS_SKILL_DMG, 1.0);
					damage /= target.getStat().calcStat(Stats.PVP_PHYS_SKILL_DEF, 1.0);
					damage = attacker.getStat().calcStat(Stats.PHYSICAL_SKILL_POWER, damage);
				}
				critical = (BaseStats.STR.calcBonus(attacker) * getBaseCritRate()) > (Rnd.nextDouble() * 100);
				if (critical)
				{
					damage *= 2;
				}
				
				if (isPvE)
				{
					damage *= attacker.calcStat(Stats.PVE_PHYS_SKILL_DMG, 1, null, null);
				}
				
				if (isPvEAtt)
				{
					damage /= target.calcStat(Stats.PVE_PHYS_SKILL_DEF, 1, null, null);
				}
			}
			
			damage *= ClassBalanceParser.getInstance().getBalancedClass(AttackType.PSkillDamage, attacker, target);
			if (damage > 0)
			{
				target.reduceCurrentHp(damage, attacker, this);
				if (Config.LOG_GAME_DAMAGE && damage > Config.LOG_GAME_DAMAGE_THRESHOLD)
				{
					Log.addLogDamage((int) damage, "PDAM", this, attacker, target);
				}
				attacker.sendDamageMessage(target, (int) damage, this, false, critical, false);
				Formulas.calcDamageReflected(attacker, target, this, damage, critical);
				
				if (hasEffects())
				{
					final byte shld = Formulas.calcShldUse(attacker, target, this);
					final byte reflect = Formulas.calcSkillReflect(target, this);
					if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0)
					{
						attacker.stopSkillEffects(getId());
						getEffects(target, attacker, true);
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(this);
						attacker.sendPacket(sm);
					}
					else
					{
						getEffects(attacker, target, new Env(shld, ss, sps, bss), true);
					}
				}
			}
		}
		
		if (hasSelfEffects())
		{
			final var effect = caster.getFirstEffect(getId());
			if ((effect != null) && effect.isSelfEffect())
			{
				effect.exit();
			}
			getEffectsSelf(caster);
		}
		caster.setChargedShot(ShotType.SOULSHOTS, false);
	}
}