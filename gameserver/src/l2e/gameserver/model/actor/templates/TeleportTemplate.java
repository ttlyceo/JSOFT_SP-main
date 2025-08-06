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
package l2e.gameserver.model.actor.templates;

import l2e.gameserver.model.Location;

public class TeleportTemplate
{
	private final int _teleId;
	private final Location _loc;
	private final long _price;
	private final boolean _isForNoble;
	private final int _itemId;
	private final int _minLevel;
	private final int _maxLevel;
	
	public TeleportTemplate(int id, Location loc, boolean isForNoble, int itemId, long price, int minLevel, int maxLevel)
	{
		_teleId = id;
		_loc = loc;
		_isForNoble = isForNoble;
		_itemId = itemId;
		_price = price;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
	}
	
	public int getTeleId()
	{
		return _teleId;
	}
	
	public Location getLocation()
	{
		return _loc;
	}
	
	public long getPrice()
	{
		return _price;
	}
	
	public boolean isForNoble()
	{
		return _isForNoble;
	}
	
	public int getId()
	{
		return _itemId;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public int getMaxLevel()
	{
		return _maxLevel;
	}
}