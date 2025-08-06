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
package l2e.gameserver.model.actor.templates.daily;

import java.util.Map;

/**
 * Created by LordWinter
 */
public class DailyRewardTemplate
{
	private int _day;
	private Map<Integer, Integer> _rewards;
	private String _displayImage;
	
	public DailyRewardTemplate(int day, Map<Integer, Integer> rewards)
	{
		_day = day;
		_rewards = rewards;
	}

	public int getDay()
	{
		return _day;
	}

	public Map<Integer, Integer> getRewards()
	{
		return _rewards;
	}
	
	public String getDisplayImage()
	{
		return _displayImage;
	}
	
	public void setDisplayImage(String image)
	{
		_displayImage = image;
	}
}