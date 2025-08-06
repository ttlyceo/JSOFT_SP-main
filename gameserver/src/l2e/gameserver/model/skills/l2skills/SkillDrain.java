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
import l2e.gameserver.Config;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.CubicInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class SkillDrain extends Skill
{
	private final float _absorbPart;
	private final int _absorbAbs;
	
	public SkillDrain(StatsSet set)
	{
		super(set);

		_absorbPart = set.getFloat("absorbPart", 0.f);
		_absorbAbs = set.getInteger("absorbAbs", 0);
	}

	@Override
	public void useSkill(Creature activeChar, GameObject[] targets)
	{
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		final boolean ss = useSoulShot() && activeChar.isChargedShot(ShotType.SOULSHOTS);
		final boolean sps = useSpiritShot() && activeChar.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = useSpiritShot() && activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);

		for (final Creature target : (Creature[]) targets)
		{
			if (target.isAlikeDead() && (getTargetType() != TargetType.CORPSE_MOB))
			{
				continue;
			}

			if ((activeChar != target) && target.isInvul())
			{
				continue;
			}

			final boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this));
			final byte shld = Formulas.calcShldUse(activeChar, target, this);
			final int damage = isStaticDamage() ? (int) getPower() : (int) Formulas.calcMagicDam(activeChar, target, this, shld, sps, bss, mcrit);

			Formulas.calcDrainDamage(activeChar, target, damage, _absorbAbs, _absorbPart);
			
			if ((damage > 0) && (!target.isDead() || (getTargetType() != TargetType.CORPSE_MOB)))
			{
				if (!target.isRaid() && Formulas.calcAtkBreak(target, mcrit))
				{
					target.breakAttack();
					target.breakCast();
				}

				activeChar.sendDamageMessage(target, damage, this, mcrit, false, false);

				if (Config.LOG_GAME_DAMAGE && activeChar.isPlayable() && damage > Config.LOG_GAME_DAMAGE_THRESHOLD)
				{
					Log.addLogDamage(damage, "PDAM", this, activeChar.getActingPlayer(), target);
				}

				if (hasEffects() && (getTargetType() != TargetType.CORPSE_MOB))
				{
					if ((Formulas.calcSkillReflect(target, this) & Formulas.SKILL_REFLECT_SUCCEED) > 0)
					{
						activeChar.stopSkillEffects(getId());
						getEffects(target, activeChar, true);
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(getId());
						activeChar.sendPacket(sm);
					}
					else
					{
						target.stopSkillEffects(getId());
						if (Formulas.calcSkillSuccess(activeChar, target, this, shld, ss, sps, bss))
						{
							getEffects(activeChar, target, true);
						}
						else
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
							sm.addCharName(target);
							sm.addSkillName(this);
							activeChar.sendPacket(sm);
						}
					}
				}

				target.reduceCurrentHp(damage, activeChar, this);
			}

			if (target.isDead() && getTargetType() == TargetType.CORPSE_MOB && target.isNpc())
			{
				((Npc) target).endDecayTask();
			}
		}
		
		final Effect effect = activeChar.getFirstEffect(getId());
		if ((effect != null) && effect.isSelfEffect())
		{
			effect.exit();
		}
		getEffectsSelf(activeChar);
		activeChar.setChargedShot(bss ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
	}

	public void useCubicSkill(CubicInstance activeCubic, GameObject[] targets)
	{
		if (Config.DEBUG)
		{
			_log.info("SkillDrain: useCubicSkill()");
		}

		for (final Creature target : (Creature[]) targets)
		{
			if (target.isAlikeDead() && (getTargetType() != TargetType.CORPSE_MOB))
			{
				continue;
			}

			final boolean mcrit = Formulas.calcMCrit(activeCubic.getMCriticalHit(target, this));
			final byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, this);

			final int damage = (int) Formulas.calcMagicDam(activeCubic, target, this, mcrit, shld);
			if (Config.DEBUG)
			{
				_log.info("SkillDrain: useCubicSkill() -> damage = " + damage);
			}

			final double hpAdd = _absorbAbs + (_absorbPart * damage);
			final Player owner = activeCubic.getOwner();
			final double hp = ((owner.getCurrentHp() + hpAdd) > owner.getMaxHp() ? owner.getMaxHp() : (owner.getCurrentHp() + hpAdd));

			if (!owner.isHealBlocked())
			{
				owner.setCurrentHp(hp);
				owner.sendStatusUpdate(false, false, StatusUpdate.CUR_HP);
			}
			
			if ((damage > 0) && (!target.isDead() || (getTargetType() != TargetType.CORPSE_MOB)))
			{
				target.reduceCurrentHp(damage, activeCubic.getOwner(), this);

				if (!target.isRaid() && Formulas.calcAtkBreak(target, mcrit))
				{
					target.breakAttack();
					target.breakCast();
				}
				owner.sendDamageMessage(target, damage, this, mcrit, false, false);
			}
		}
	}
}