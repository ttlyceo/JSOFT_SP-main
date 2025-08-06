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
package l2e.gameserver.model.actor.templates.community;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.holders.ItemHolder;

public class CBTeleportTemplate
{
	private final int _id;
	private final int _minLvl;
	private final int _maxLvl;
	private final int _freeLvl;
	private final String _name;
	private final boolean _canPk;
	private final boolean _isForPremium;
	private final Location _loc;
	private final ItemHolder _price;
	private final ItemHolder _requestItem;
	
	public CBTeleportTemplate(int id, String name, int minLvl, int maxLvl, int freeLvl, boolean canPk, boolean isForPremium, Location loc, ItemHolder price, ItemHolder requestItem)
	{
		_id = id;
		_name = name;
		_minLvl = minLvl;
		_maxLvl = maxLvl;
		_freeLvl = freeLvl;
		_canPk = canPk;
		_isForPremium = isForPremium;
		_loc = loc;
		_price = price;
		_requestItem = requestItem;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getMinLvl()
	{
		return _minLvl;
	}
	
	public int getMaxLvl()
	{
		return _maxLvl;
	}
	
	public int getFreeLvl()
	{
		return _freeLvl;
	}
	
	public boolean canPk()
	{
		return _canPk;
	}
	
	public Location getLocation()
	{
		return _loc;
	}
	
	public ItemHolder getPrice()
	{
		return _price;
	}
	
	public ItemHolder getReguestItem()
	{
		return _requestItem;
	}
	
	public boolean isForPremium()
	{
		return _isForPremium;
	}
}