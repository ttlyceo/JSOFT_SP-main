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
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;

public final class ClanWarehouse extends Warehouse
{
	private final Clan _clan;
	
	public ClanWarehouse(Clan clan)
	{
		_clan = clan;
	}

	@Override
	public String getName()
	{
		return "ClanWarehouse";
	}

	@Override
	public int getOwnerId()
	{
		return _clan.getId();
	}
	
	@Override
	public Player getOwner()
	{
		return _clan.getLeader().getPlayerInstance();
	}
	
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.CLANWH;
	}
	
	public String getLocationId()
	{
		return "0";
	}
	
	public int getLocationId(boolean dummy)
	{
		return 0;
	}
	
	public void setLocationId(Player dummy)
	{
	}
	
	@Override
	public boolean validateCapacity(long slots)
	{
		return ((_items.size() + slots) <= Config.WAREHOUSE_SLOTS_CLAN);
	}
	
	@Override
	public ItemInstance addItem(String process, int itemId, long count, Player actor, Object reference)
	{
		return super.addWareHouseItem(process, itemId, count, actor, reference);
	}
	
	@Override
	public ItemInstance addItem(String process, ItemInstance item, Player actor, Object reference)
	{
		return super.addWaheHouseItem(process, item, actor, reference);
	}
	
	@Override
	public ItemInstance destroyItem(String process, ItemInstance item, long count, Player actor, Object reference)
	{
		return super.destroyItem(process, item, count, actor, reference);
	}
	
	@Override
	public ItemInstance transferItem(String process, int objectId, long count, ItemContainer target, Player actor, Object reference)
	{
		return super.transferItem(process, objectId, count, target, actor, reference);
	}
}