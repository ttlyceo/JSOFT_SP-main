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
package l2e.gameserver.model.skills.funcs;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Stats;

public class FuncEnchant extends Func
{
	public FuncEnchant(Stats pStat, int pOrder, Object owner, Lambda lambda)
	{
		super(pStat, pOrder, owner);
	}

	@Override
	public void calc(Env env)
	{
		if ((cond != null) && !cond.test(env))
		{
			return;
		}
		final ItemInstance item = (ItemInstance) funcOwner;
		
		int enchant = item.getEnchantLevel();
		
		if (enchant <= 0)
		{
			return;
		}
		
		int overenchant = 0;
		
		if (enchant > 3)
		{
			overenchant = enchant - 3;
			enchant = 3;
		}
		
		if (env.getPlayer() != null)
		{
			final Player player = env.getPlayer();
			if (player.isInOlympiadMode())
			{
				final var enchantLimit = item.getItem().isWeapon() ? Config.ALT_OLY_WEAPON_ENCHANT_LIMIT : item.getItem().isAccessory() ? Config.ALT_OLY_ACCESSORY_ENCHANT_LIMIT : Config.ALT_OLY_ARMOR_ENCHANT_LIMIT;
				if (enchantLimit >= 0 && ((enchant + overenchant) > enchantLimit))
				{
					if (enchantLimit > 3)
					{
						overenchant = enchantLimit - 3;
					}
					else
					{
						overenchant = 0;
						enchant = enchantLimit;
					}
				}
			}
		}
		
		if ((stat == Stats.MAGIC_DEFENCE) || (stat == Stats.POWER_DEFENCE))
		{
			env.addValue(enchant + (3 * overenchant));
			return;
		}
		
		if (stat == Stats.MAGIC_ATTACK)
		{
			switch (item.getItem().getItemGradeSPlus())
			{
				case Item.CRYSTAL_S :
					env.addValue((4 * enchant) + (8 * overenchant));
					break;
				case Item.CRYSTAL_A :
				case Item.CRYSTAL_B :
				case Item.CRYSTAL_C :
					env.addValue((3 * enchant) + (6 * overenchant));
					break;
				case Item.CRYSTAL_D :
				case Item.CRYSTAL_NONE :
					env.addValue((2 * enchant) + (4 * overenchant));
					break;
			}
			return;
		}
		
		if (item.isWeapon())
		{
			final WeaponType type = (WeaponType) item.getItemType();
			switch (item.getItem().getItemGradeSPlus())
			{
				case Item.CRYSTAL_S :
					switch (type)
					{
						case BOW :
						case CROSSBOW :
							env.addValue((10 * enchant) + (20 * overenchant));
							break;
						case BIGSWORD :
						case BIGBLUNT :
						case DUAL :
						case DUALFIST :
						case ANCIENTSWORD :
						case DUALDAGGER :
							env.addValue((6 * enchant) + (12 * overenchant));
							break;
						default :
							env.addValue((5 * enchant) + (10 * overenchant));
							break;
					}
					break;
				case Item.CRYSTAL_A :
					switch (type)
					{
						case BOW :
						case CROSSBOW :
							env.addValue((8 * enchant) + (16 * overenchant));
							break;
						case BIGSWORD :
						case BIGBLUNT :
						case DUAL :
						case DUALFIST :
						case ANCIENTSWORD :
						case DUALDAGGER :
							env.addValue((5 * enchant) + (10 * overenchant));
							break;
						default :
							env.addValue((4 * enchant) + (8 * overenchant));
							break;
					}
					break;
				case Item.CRYSTAL_B :
				case Item.CRYSTAL_C :
					switch (type)
					{
						case BOW :
						case CROSSBOW :
							env.addValue((6 * enchant) + (12 * overenchant));
							break;
						case BIGSWORD :
						case BIGBLUNT :
						case DUAL :
						case DUALFIST :
						case ANCIENTSWORD :
						case DUALDAGGER :
							env.addValue((4 * enchant) + (8 * overenchant));
							break;
						default :
							env.addValue((3 * enchant) + (6 * overenchant));
							break;
					}
					break;
				case Item.CRYSTAL_D :
				case Item.CRYSTAL_NONE :
					switch (type)
					{
						case BOW :
						case CROSSBOW :
						{
							env.addValue((4 * enchant) + (8 * overenchant));
							break;
						}
						default :
							env.addValue((2 * enchant) + (4 * overenchant));
							break;
					}
					break;
			}
		}
	}
}