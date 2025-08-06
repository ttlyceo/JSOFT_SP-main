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
package l2e.gameserver.model.skills.conditions;

import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.stats.Env;

public class ConditionChangeWeapon extends Condition
{
	private final boolean _required;

	public ConditionChangeWeapon(boolean required)
	{
		_required = required;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		final var player = env.getPlayer();
		if (player == null || player.isAlikeDead())
		{
			return false;
		}

		if (_required)
		{
			final var wpnItem = env.getPlayer().getActiveWeaponItem();
			if (wpnItem == null || wpnItem.getChangeWeaponId() == 0)
			{
				return false;
			}

			if (player.isEnchanting())
			{
				return false;
			}
			
			var wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (wpn == null)
			{
				wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			}
			
			if ((wpn == null) || wpn.isAugmented())
			{
				return false;
			}
		}
		return true;
	}
}