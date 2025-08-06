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

import java.util.Map;

import l2e.gameserver.Config;
import l2e.gameserver.model.base.BonusType;
import l2e.gameserver.model.stats.StatsSet;

/**
 * Created by LordWinter
 */
public class DonateRateTempate
{
	private final int _id;
	private final StatsSet _params;
	private final Map<Integer, Long> _price;
	private final Map<BonusType, Double> _bonusList;
	private final Map<BonusType, String> _bonusIcon;
	
	public DonateRateTempate(int id, StatsSet params, Map<Integer, Long> price, Map<BonusType, Double> bonusList, Map<BonusType, String> bonusIcon)
	{
		_id = id;
		_params = params;
		_price = price;
		_bonusList = bonusList;
		_bonusIcon = bonusIcon;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "name" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public String getIcon()
	{
		return _params.getString("icon");
	}
	
	public long getTime()
	{
		return _params.getLong("time");
	}
	
	public Map<Integer, Long> getPrice()
	{
		return _price;
	}
	
	public Map<BonusType, Double> getBonusList()
	{
		return _bonusList;
	}
	
	public Map<BonusType, String> getBonusIcon()
	{
		return _bonusIcon;
	}
}