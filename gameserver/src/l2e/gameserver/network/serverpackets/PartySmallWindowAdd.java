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

public final class PartySmallWindowAdd extends GameServerPacket
{
	private final Player _member;
	private final int _leaderId;
	private final int _distribution;
	
	public PartySmallWindowAdd(Player member, Party party)
	{
		_member = member;
		_leaderId = party.getLeaderObjectId();
		_distribution = party.getLootDistribution();
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_leaderId);
		writeD(_distribution);
		writeD(_member.getObjectId());
		writeS(_member.getName(null));
		writeD((int) _member.getCurrentCp());
		writeD((int) _member.getMaxCp());
		writeD((int) _member.getCurrentHp());
		writeD((int) _member.getMaxHp());
		writeD((int) _member.getCurrentMp());
		writeD((int) _member.getMaxMp());
		writeD(_member.getLevel());
		writeD(_member.getClassId().getId());
		writeD(0x00);
		writeD(0x00);
	}
}