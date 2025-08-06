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

import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.SystemMessageId;

public class EnergyDamOverTime extends Effect
{
	public EnergyDamOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	public EnergyDamOverTime(Env env, Effect effect)
	{
		super(env, effect);
	}

	@Override
	public boolean canBeStolen()
	{
		return !getSkill().isToggle();
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.ENERGY_DAM_OVER_TIME;
	}

	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead() || getEffected().getActingPlayer().getAgathionId() == 0)
		{
			return false;
		}
		
		final ItemInstance item = getEffected().getInventory().getPaperdollItem(Inventory.PAPERDOLL_LBRACELET);
		if (item == null)
		{
			return false;
		}
		
		final double energyDam = calc();
		
		if (energyDam > item.getAgathionEnergy())
		{
			getEffected().sendPacket(SystemMessageId.THE_SKILL_HAS_BEEN_CANCELED_BECAUSE_YOU_HAVE_INSUFFICIENT_ENERGY);
			return false;
		}

		item.setAgathionEnergy((int) (item.getAgathionEnergy() - energyDam));
		return getSkill().isToggle();
	}
}