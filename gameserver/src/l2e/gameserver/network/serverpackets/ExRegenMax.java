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

public class ExRegenMax extends GameServerPacket
{
	private final double _max;
	private final int _count;
	private final int _time;

	public ExRegenMax(double max, int count, int time)
	{
		_max = max;
		_count = count;
		_time = time;
	}

	@Override
	protected void writeImpl()
	{
		writeD(0x01);
		writeD(_count);
		writeD(_time);
		writeF(_max);
	}
}