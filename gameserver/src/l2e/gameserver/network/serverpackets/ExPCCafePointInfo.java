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
package l2e.gameserver.network.serverpackets;

public class ExPCCafePointInfo extends GameServerPacket
{
	private final int _points;
	private final int _mAddPoint;
	private int _mPeriodType;
	private int _remainTime;
	private int _pointType = 0;
	
	public ExPCCafePointInfo()
	{
		_points = 0;
		_mAddPoint = 0;
		_remainTime = 0;
		_mPeriodType = 0;
		_pointType = 0; 
	}
	public ExPCCafePointInfo(final int points, final int modify_points, final boolean mod, final boolean _double, final int hours_left)
	{
		_points = points;
		_mAddPoint = modify_points;
		_remainTime = hours_left;
		if (mod && _double)
		{
			_mPeriodType = 1;
			_pointType = 0;
		}
		else if (mod)
		{
			_mPeriodType = 1;
			_pointType = 1;
		}
		else
		{
			_mPeriodType = 2;
			_pointType = 2;
		}
	}
	@Override
	protected void writeImpl()
	{
		writeD(_points);
		writeD(_mAddPoint);
		writeC(_mPeriodType);
		writeD(_remainTime);
		writeC(_pointType);
	}	
}