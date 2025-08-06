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
package l2e.gameserver.model.actor.templates.player.online;

import java.util.List;

import l2e.gameserver.model.holders.ItemHolder;

/**
 * Created by LordWinter
 */
public class OnlineRewardTemplate
{
	private final int _id;
	private final int _minutes;
	private final List<ItemHolder> _items;
	private final boolean _printItem;
	
	public OnlineRewardTemplate(int id, int minutes, List<ItemHolder> items, boolean printItem)
	{
		_id = id;
		_minutes = minutes;
		_items = items;
		_printItem = printItem;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getMinutes()
	{
		return _minutes;
	}
	
	public boolean haveRewards()
	{
		return _items != null && !_items.isEmpty();
	}
	
	public List<ItemHolder> getRewards()
	{
		return _items;
	}
	
	public boolean isPrintItem()
	{
		return _printItem;
	}
}