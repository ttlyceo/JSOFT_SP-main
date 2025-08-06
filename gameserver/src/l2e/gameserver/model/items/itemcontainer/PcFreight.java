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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.model.stats.Stats;

public class PcFreight extends ItemContainer
{
	private final Player _owner;
	private final int _ownerId;

	public PcFreight(int object_id)
	{
		_owner = null;
		_ownerId = object_id;
		restore();
	}
	
	public PcFreight(Player owner)
	{
		_owner = owner;
		_ownerId = owner.getObjectId();
	}
	
	@Override
	public int getOwnerId()
	{
		return _ownerId;
	}

	@Override
	public Player getOwner()
	{
		return _owner;
	}

	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.FREIGHT;
	}

	@Override
	public String getName()
	{
		return "Freight";
	}

	@Override
	public boolean validateCapacity(long slots)
	{
		final int curSlots = _owner == null ? Config.ALT_FREIGHT_SLOTS : Config.ALT_FREIGHT_SLOTS + (int) _owner.getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
		return ((getSize() + slots) <= curSlots);
	}

	@Override
	public void refreshWeight()
	{
	}
}