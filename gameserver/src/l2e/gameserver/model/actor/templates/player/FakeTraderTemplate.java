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
package l2e.gameserver.model.actor.templates.player;

import java.util.List;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.spawn.SpawnTerritory;

public class FakeTraderTemplate
{
	private final int _id;
	private final int _classId;
	private final String _type;
	private final int _minLvl;
	private final int _maxLvl;
	private final SpawnTerritory _territory;
	private final Location _location;
	private final List<ItemHolder> _addItems;
	private final List<ItemHolder> _tradeList;
	private final String _message;

	public FakeTraderTemplate(int id, int classId, String type, int minLvl, int maxLvl, SpawnTerritory territory, Location loc, List<ItemHolder> addItems, List<ItemHolder> tradeList, String message)
	{
		_id = id;
		_classId = classId;
		_territory = territory;
		_type = type;
		_minLvl = minLvl;
		_maxLvl = maxLvl;
		_location = loc;
		_addItems = addItems;
		_tradeList = tradeList;
		_message = message;
	}

	public int getId()
	{
		return _id;
	}
	
	public int getClassId()
	{
		return _classId;
	}
	
	public SpawnTerritory getTerritory()
	{
		return _territory;
	}
	
	public Location getLocation()
	{
		return _location;
	}
	
	public int getMinLvl()
	{
		return _minLvl;
	}
	
	public int getMaxLvl()
	{
		return _maxLvl;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public String getMessage()
	{
		return _message;
	}
	
	public List<ItemHolder> getAddItems()
	{
		return _addItems;
	}
	
	public List<ItemHolder> getTradeList()
	{
		return _tradeList;
	}
}