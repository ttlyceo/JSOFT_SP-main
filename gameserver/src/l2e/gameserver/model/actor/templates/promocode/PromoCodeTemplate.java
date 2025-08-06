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
package l2e.gameserver.model.actor.templates.promocode;

import java.util.List;

import l2e.gameserver.model.actor.templates.promocode.impl.AbstractCodeReward;

/**
 * Created by LordWinter
 */
public class PromoCodeTemplate
{
	private final String _name;
	private final int _minLvl;
	private final int _maxLvl;
	private final boolean _canUseSubClass;
	private final long _fromDate;
	private final long _toDate;
	private final int _limit;
	private int _curLimit = 0;
	private final List<AbstractCodeReward> _rewards;
	private final boolean _limitByAccount;
	private final boolean _limitHWID;
	
	public PromoCodeTemplate(String name, int minLvl, int maxLvl, boolean canUseSubClass, long fromDate, long toDate, int limit, List<AbstractCodeReward> rewards, boolean limitByAccount, boolean limitHWID)
	{
		_name = name;
		_minLvl = minLvl;
		_maxLvl = maxLvl;
		_canUseSubClass = canUseSubClass;
		_fromDate = fromDate;
		_toDate = toDate;
		_limit = limit;
		_rewards = rewards;
		_limitByAccount = limitByAccount;
		_limitHWID = limitHWID;
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
	
	public boolean canUseSubClass()
	{
		return _canUseSubClass;
	}
	
	public long getStartDate()
	{
		return _fromDate;
	}
	
	public long getEndDate()
	{
		return _toDate;
	}
	
	public int getLimit()
	{
		return _limit;
	}
	
	public int getCurLimit()
	{
		return _curLimit;
	}
	
	public void setCurLimit(int limit)
	{
		_curLimit = limit;
	}
	
	public List<AbstractCodeReward> getRewards()
	{
		return _rewards;
	}
	
	public boolean isLimitByAccount()
	{
		return _limitByAccount;
	}
	
	public boolean isLimitHWID()
	{
		return _limitHWID;
	}
}