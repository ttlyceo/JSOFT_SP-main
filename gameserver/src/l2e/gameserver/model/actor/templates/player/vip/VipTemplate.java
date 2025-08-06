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
package l2e.gameserver.model.actor.templates.player.vip;

import java.util.List;

import l2e.gameserver.model.holders.ItemHolder;

/**
 * Created by LordWinter
 */
public class VipTemplate
{
	private final int _id;
	private final long _points;
	private final double _expRate;
	private final double _spRate;
	private final double _adenaRate;
	private final double _dropRate;
	private final double _dropRaidRate;
	private final double _spoilRate;
	private final double _epRate;
	private final int _enchantChance;
	private final List<ItemHolder> _items;
	private final List<ItemHolder> _requestItems;
	private final List<ItemHolder> _rewardItems;
	
	public VipTemplate(int id, long points, double expRate, double spRate, double adenaRate, double dropRate, double dropRaidRate, double spoilRate, double epRate, int enchantChance, List<ItemHolder> items, List<ItemHolder> rewardItems, List<ItemHolder> requestItems)
	{
		_id = id;
		_points = points;
		_expRate = expRate;
		_spRate = spRate;
		_adenaRate = adenaRate;
		_dropRate = dropRate;
		_dropRaidRate = dropRaidRate;
		_spoilRate = spoilRate;
		_epRate = epRate;
		_enchantChance = enchantChance;
		_items = items;
		_requestItems = requestItems;
		_rewardItems = rewardItems;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public long getPoints()
	{
		return _points;
	}
	
	public double getExpRate()
	{
		return _expRate;
	}
	
	public double getSpRate()
	{
		return _spRate;
	}
	
	public double getAdenaRate()
	{
		return _adenaRate;
	}
	
	public double getDropRate()
	{
		return _dropRate;
	}
	
	public double getDropRaidRate()
	{
		return _dropRaidRate;
	}
	
	public double getSpoilRate()
	{
		return _spoilRate;
	}
	
	public double getEpRate()
	{
		return _epRate;
	}
	
	public int getEnchantChance()
	{
		return _enchantChance;
	}
	
	public boolean haveRewards()
	{
		return _items != null && !_items.isEmpty();
	}
	
	public List<ItemHolder> getDailyItems()
	{
		return _items;
	}
	
	public List<ItemHolder> getRequestItems()
	{
		return _requestItems;
	}
	
	public List<ItemHolder> getRewardItems()
	{
		return _rewardItems;
	}
}