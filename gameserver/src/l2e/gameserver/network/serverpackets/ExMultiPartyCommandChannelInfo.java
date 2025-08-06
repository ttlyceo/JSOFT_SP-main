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

import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.Party;

public class ExMultiPartyCommandChannelInfo extends GameServerPacket
{
	private final CommandChannel _channel;
	
	public ExMultiPartyCommandChannelInfo(CommandChannel channel)
	{
		_channel = channel;
	}
	
	@Override
	protected void writeImpl()
	{
		if (_channel == null)
		{
			return;
		}
		writeS(_channel.getLeader().getName(null));
		writeD(0x00);
		writeD(_channel.getMemberCount());
		writeD(_channel.getPartys().size());
		for (final Party p : _channel.getPartys())
		{
			writeS(p.getLeader().getName(null));
			writeD(p.getLeaderObjectId());
			writeD(p.getMemberCount());
		}
	}
}