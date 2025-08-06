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

import l2e.gameserver.Config;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;

public class PcPrivateInventory extends Warehouse
{
	private final Player _owner;

	public PcPrivateInventory(Player owner)
	{
		_owner = owner;
	}
	
	@Override
	public String getName()
	{
		return "PrivateInventory";
	}

	@Override
	public Player getOwner()
	{
		return _owner;
	}

	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.PRIVATEINV;
	}

	@Override
	public boolean validateCapacity(long slots)
	{
		return (_items.size() + slots <= _owner.getPrivateInventoryLimit());
	}
	
	@Override
	public ItemInstance addItem(String process, int itemId, long count, Player actor, Object reference)
	{
		ItemInstance item = getItemByItemId(itemId);
		
		if ((item != null) && item.isStackable())
		{
			item.changeCount(process, count, actor, reference);
			item.setLastChange(ItemInstance.MODIFIED);
			final double adenaRate = Config.RATE_DROP_ADENA;
			if (item.getItemLocation() != ItemLocation.PRIVATEINV)
			{
				item.setItemLocation(ItemLocation.PRIVATEINV);
			}
			
			if ((itemId == PcInventory.ADENA_ID) && (count < (10000 * adenaRate)))
			{
				if ((GameTimeController.getInstance().getGameTicks() % 5) == 0)
				{
					item.updateDatabase();
				}
			}
			else
			{
				item.updateDatabase();
			}
		}
		else
		{
			for (int i = 0; i < count; i++)
			{
				final Item template = ItemsParser.getInstance().getTemplate(itemId);
				if (template == null)
				{
					_log.warn((actor != null ? "[" + actor.getName(null) + "] " : "") + "Invalid ItemId requested: ", itemId);
					return null;
				}
				
				item = ItemsParser.getInstance().createItem(process, itemId, template.isStackable() ? count : 1, actor, reference);
				item.setOwnerId(getOwnerId());
				item.setItemLocation(ItemLocation.PRIVATEINV);
				item.setLastChange(ItemInstance.ADDED);
				
				addItem(item);
				item.updateDatabase(true);
				
				if (template.isStackable() || !Config.MULTIPLE_ITEM_DROP)
				{
					break;
				}
			}
		}
		
		if (actor != null && item != null)
		{
			final ItemInstance newItem = actor.getInventory().getItemByItemId(item.getId());
			if (newItem != null)
			{
				if (newItem.isEtcItem())
				{
					actor.checkToEquipArrows(newItem);
				}
				else if (newItem.isTalisman())
				{
					actor.checkCombineTalisman(newItem, false);
				}
			}
		}
		refreshWeight();
		return item;
	}
	
	@Override
	public ItemInstance addItem(String process, ItemInstance item, Player actor, Object reference)
	{
		final ItemInstance olditem = getItemByItemId(item.getId());
		
		if ((olditem != null) && olditem.isStackable())
		{
			final long count = item.getCount();
			olditem.changeCount(process, count, actor, reference);
			olditem.setLastChange(ItemInstance.MODIFIED);
			
			ItemsParser.getInstance().destroyItem(process, item, actor, reference);
			if (item.getItemLocation() != ItemLocation.PRIVATEINV)
			{
				item.setItemLocation(ItemLocation.PRIVATEINV);
			}
			item.updateDatabase();
			item = olditem;
			
			if ((item.getId() == PcInventory.ADENA_ID) && (count < (10000 * Config.RATE_DROP_ADENA)))
			{
				if ((GameTimeController.getInstance().getGameTicks() % 5) == 0)
				{
					item.updateDatabase();
				}
			}
			else
			{
				item.updateDatabase();
			}
		}
		else
		{
			item.setOwnerId(process, getOwnerId(), actor, reference);
			item.setItemLocation(ItemLocation.PRIVATEINV);
			item.setLastChange((ItemInstance.ADDED));
			
			addItem(item);
			item.updateDatabase(true);
		}
		
		if (actor != null && item != null)
		{
			final ItemInstance newItem = actor.getInventory().getItemByItemId(item.getId());
			if (newItem != null)
			{
				if (newItem.isEtcItem())
				{
					actor.checkToEquipArrows(newItem);
				}
				else if (newItem.isTalisman())
				{
					actor.checkCombineTalisman(newItem, false);
				}
			}
		}
		refreshWeight();
		return item;
	}
	
	public int getItemSize()
	{
		return _items.size();
	}
}