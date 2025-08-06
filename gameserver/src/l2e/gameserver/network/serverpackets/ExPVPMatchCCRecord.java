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

import java.util.Map;

public class ExPVPMatchCCRecord extends GameServerPacket
{
	private final Map<String, Integer> _scores;
	private final int _state;
	
	public ExPVPMatchCCRecord(Map<String, Integer> scores, int state)
	{
		_scores = scores;
		_state = state;
	}

	@Override
	protected void writeImpl()
	{
		writeD(_state);
		writeD(_scores.size());
		for (final Map.Entry<String, Integer> p : _scores.entrySet())
		{
			writeS(p.getKey());
			writeD(p.getValue().intValue());
		}
	}
}