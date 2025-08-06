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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;

public class PcRefund extends ItemContainer
{
	private final Player _owner;

	public PcRefund(Player owner)
	{
		_owner = owner;
	}
	
	@Override
	public String getName()
	{
		return "Refund";
	}

	@Override
	public Player getOwner()
	{
		return _owner;
	}

	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.REFUND;
	}

	@Override
	protected void addItem(ItemInstance item)
	{
		super.addItem(item);
		try
		{
			if (getSize() > 12)
			{
				final ItemInstance removedItem = _items.remove(0);
				if (removedItem != null)
				{
					ItemsParser.getInstance().destroyItem("ClearRefund", removedItem, getOwner(), null);
					removedItem.updateDatabase(true);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("addItem()", e);
		}
	}

	@Override
	public void refreshWeight()
	{
	}

	@Override
	public void deleteMe()
	{
		try
		{
			for (final ItemInstance item : _items)
			{
				if (item != null)
				{
					ItemsParser.getInstance().destroyItem("ClearRefund", item, getOwner(), null);
					item.updateDatabase(true);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("deleteMe()", e);
		}
		_items.clear();
	}

	@Override
	public void restore()
	{
	}
}