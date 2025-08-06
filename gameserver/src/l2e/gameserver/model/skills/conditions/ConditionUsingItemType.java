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

import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.type.ArmorType;
import l2e.gameserver.model.stats.Env;

public final class ConditionUsingItemType extends Condition
{
	private final boolean _armor;
	private final int _mask;

	public ConditionUsingItemType(int mask)
	{
		_mask = mask;
		_armor = (_mask & (ArmorType.MAGIC.mask() | ArmorType.LIGHT.mask() | ArmorType.HEAVY.mask())) != 0;
	}

	@Override
	public boolean testImpl(Env env)
	{
		final var character = env.getCharacter();
		if (character == null || !character.isPlayable())
		{
			return false;
		}
		
		if (character.isSummon())
		{
			return true;
		}

		final Inventory inv = character.getInventory();
		
		if (_armor)
		{
			final var chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if (chest == null)
			{
				return false;
			}
			final int chestMask = chest.getItem().getItemMask();

			if ((_mask & chestMask) == 0)
			{
				return false;
			}

			final int chestBodyPart = chest.getItem().getBodyPart();
			
			if (chestBodyPart == Item.SLOT_FULL_ARMOR)
			{
				return true;
			}
			final var legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			if (legs == null)
			{
				return false;
			}
			final int legMask = legs.getItem().getItemMask();
			return (_mask & legMask) != 0;
		}
		return (_mask & inv.getWearedMask()) != 0;
	}
}