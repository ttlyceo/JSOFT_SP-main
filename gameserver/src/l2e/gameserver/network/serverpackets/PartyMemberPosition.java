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

import java.util.HashMap;
import java.util.Map;

import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;

public class PartyMemberPosition extends GameServerPacket
{
	private final Map<Integer, Location> _locations = new HashMap<>();

	public PartyMemberPosition(Party party)
	{
		reuse(party);
	}
	
	public void reuse(Party party)
	{
		_locations.clear();
		for (final Player member : party.getMembers())
		{
			if (member == null)
			{
				continue;
			}
			_locations.put(member.getObjectId(), member.getLocation());
		}
	}

	@Override
	protected void writeImpl()
	{
		writeD(_locations.size());
		for (final Map.Entry<Integer, Location> entry : _locations.entrySet())
		{
			final Location loc = entry.getValue();
			writeD(entry.getKey());
			writeD(loc.getX());
			writeD(loc.getY());
			writeD(loc.getZ());
		}
	}
}