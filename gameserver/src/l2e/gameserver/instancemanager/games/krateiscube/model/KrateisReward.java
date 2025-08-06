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
package l2e.gameserver.instancemanager.games.krateiscube.model;

/**
 * Created by LordWinter
 */
public class KrateisReward
{
	private final int _itemId;
	private final long _amount;
	private final boolean _allowMidifier;
	
	public KrateisReward(int itemId, long amount, boolean allowMidifier)
	{
		_itemId = itemId;
		_amount = amount;
		_allowMidifier = allowMidifier;
	}
	
	public int getId()
	{
		return _itemId;
	}
	
	public long getAmount()
	{
		return _amount;
	}
	
	public boolean isAllowMidifier()
	{
		return _allowMidifier;
	}
}