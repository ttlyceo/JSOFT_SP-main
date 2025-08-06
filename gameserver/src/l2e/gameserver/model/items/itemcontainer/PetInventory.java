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
package l2e.gameserver.model.items.itemcontainer;

import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;

public class PetInventory extends Inventory
{
	private final PetInstance _owner;

	public PetInventory(PetInstance owner)
	{
		_owner = owner;
	}

	@Override
	public PetInstance getOwner()
	{
		return _owner;
	}

	@Override
	public int getOwnerId()
	{
		int id;
		try
		{
			id = _owner.getOwner().getObjectId();
		}
		catch (final NullPointerException e)
		{
			return 0;
		}
		return id;
	}

	@Override
	protected void refreshWeight()
	{
		super.refreshWeight();
		getOwner().updateAndBroadcastStatus(1);
	}

	public boolean validateCapacity(ItemInstance item)
	{
		int slots = 0;

		if (!(item.isStackable() && (getItemByItemId(item.getId()) != null)) && (!item.getItem().isHerb()))
		{
			slots++;
		}

		return validateCapacity(slots);
	}

	@Override
	public boolean validateCapacity(long slots)
	{
		return ((_items.size() + slots) <= _owner.getInventoryLimit());
	}

	public boolean validateWeight(ItemInstance item, long count)
	{
		int weight = 0;
		final Item template = ItemsParser.getInstance().getTemplate(item.getId());
		if (template == null)
		{
			return false;
		}
		weight += count * template.getWeight();
		return validateWeight(weight);
	}

	@Override
	public boolean validateWeight(long weight)
	{
		return ((_totalWeight + weight) <= _owner.getMaxLoad());
	}

	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.PET;
	}

	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PET_EQUIP;
	}

	@Override
	public void restore()
	{
		super.restore();
		for (final ItemInstance item : _items)
		{
			if (item.isEquipped())
			{
				if (!item.getItem().checkCondition(getOwner(), getOwner(), false))
				{
					unEquipItemInSlot(item.getLocationSlot());
				}
			}
		}
	}

	public void transferItemsToOwner()
	{
		for (final ItemInstance item : _items)
		{
			getOwner().transferItem("return", item.getObjectId(), item.getCount(), getOwner().getOwner().getInventory(), getOwner().getOwner(), getOwner());
		}
	}
}