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
package l2e.gameserver.handler.effecthandlers.impl;

import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Heal extends Effect
{
	private final boolean _isStaticHeal;
	
	public Heal(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_isStaticHeal = template.getParameters().getBool("isStaticHeal", false);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HEAL;
	}
	
	@Override
	public boolean onStart()
	{
		final var target = getEffected();
		final var activeChar = getEffector();
		if (target == null || target.isDead() || target.isHealBlocked() || target.isInvul())
		{
			return false;
		}
		
		if (activeChar.isPlayer() && activeChar.getActingPlayer().isInFightEvent())
		{
			if (!activeChar.getActingPlayer().getFightEvent().canUsePositiveMagic(activeChar, target))
			{
				return false;
			}
		}
		
		double amount = calc();
		double staticShotBonus = 0;
		int mAtkMul = 1;
		final boolean sps = getSkill().isMagic() && activeChar.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = getSkill().isMagic() && activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		
		if ((sps || bss) && (activeChar.isPlayer() && activeChar.getActingPlayer().isMageClass()) || activeChar.isSummon())
		{
			staticShotBonus = getSkill().getMpConsume();
			mAtkMul = bss ? 4 : 2;
			staticShotBonus *= bss ? 2.4 : 1.0;
		}
		else if ((sps || bss) && activeChar.isNpc())
		{
			staticShotBonus = 2.4 * getSkill().getMpConsume();
			mAtkMul = 4;
		}
		else
		{
			final var weaponInst = activeChar.getActiveWeaponInstance();
			if (weaponInst != null)
			{
				mAtkMul = weaponInst.getItem().getItemGrade() == Item.CRYSTAL_S84 ? 4 : weaponInst.getItem().getItemGrade() == Item.CRYSTAL_S80 ? 2 : 1;
			}
			mAtkMul = bss ? mAtkMul * 4 : mAtkMul + 1;
		}
		
		if (!getSkill().isStatic())
		{
			amount += staticShotBonus + Math.sqrt(mAtkMul * (_isStaticHeal ? 1 : activeChar.getMAtk(activeChar, null)));
			amount = target.calcStat(Stats.HEAL_EFFECT, amount, null, null);
			if (!getSkill().isIgnoreCritDamage() && getSkill().isMagic() && Formulas.calcMCrit(activeChar.getMCriticalHit(target, getSkill())))
			{
				amount *= 3;
			}
		}
		
		amount = Math.max(Math.min(amount, target.getMaxRecoverableHp() - target.getCurrentHp()), 0);
		
		if (amount != 0)
		{
			target.setCurrentHp(amount + target.getCurrentHp());
			final var su = target.makeStatusUpdate(StatusUpdate.CUR_HP);
			target.sendPacket(su);
		}
		
		if (target.isPlayer())
		{
			if (getSkill().getId() == 4051)
			{
				target.sendPacket(SystemMessageId.REJUVENATING_HP);
			}
			else
			{
				if (activeChar.isPlayer() && activeChar != target)
				{
					final var sm = SystemMessage.getSystemMessage(SystemMessageId.S2_HP_RESTORED_BY_C1);
					sm.addString(activeChar.getName(null));
					sm.addNumber((int) amount);
					target.sendPacket(sm);
				}
				else
				{
					final var sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HP_RESTORED);
					sm.addNumber((int) amount);
					target.sendPacket(sm);
				}
			}
		}
		return true;
	}
}