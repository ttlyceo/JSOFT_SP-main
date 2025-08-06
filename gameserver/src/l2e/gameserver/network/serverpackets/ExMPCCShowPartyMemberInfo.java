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

import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;

public class ExMPCCShowPartyMemberInfo extends GameServerPacket
{
	private final Party _party;

	public ExMPCCShowPartyMemberInfo(Party party)
	{
		_party = party;
	}

	@Override
	protected void writeImpl()
	{
		writeD(_party.getMemberCount());
		for (final Player pc : _party.getMembers())
		{
			writeS(pc.getName(null));
			writeD(pc.getObjectId());
			writeD(pc.getClassId().getId());
		}
	}
}