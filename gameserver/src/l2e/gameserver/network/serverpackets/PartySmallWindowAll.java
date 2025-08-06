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

public final class PartySmallWindowAll extends GameServerPacket
{
	private final Party _party;
	private final Player _exclude;
	private final int _dist, _LeaderOID;

	public PartySmallWindowAll(Player exclude, Party party)
	{
		_exclude = exclude;
		_party = party;
		_LeaderOID = _party.getLeaderObjectId();
		_dist = _party.getLootDistribution();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_LeaderOID);
		writeD(_dist);
		writeD(_party.getMemberCount() - 1);
		
		for (final Player member : _party.getMembers())
		{
			if ((member != null) && (member != _exclude))
			{
				writeD(member.getObjectId());
				writeS(member.getName(null));
				writeD((int) member.getCurrentCp());
				writeD((int) member.getMaxCp());
				writeD((int) member.getCurrentHp());
				writeD((int) member.getMaxHp());
				writeD((int) member.getCurrentMp());
				writeD((int) member.getMaxMp());
				writeD(member.getLevel());
				writeD(member.getClassId().getId());
				writeD(0x00);
				writeD(member.getRace().ordinal());
				writeD(0x00);
				writeD(0x00);
				if (member.hasSummon())
				{
					writeD(member.getSummon().getObjectId());
					writeD(member.getSummon().getId() + 1000000);
					writeD(member.getSummon().getSummonType());
					writeS(member.getSummon().getName(null));
					writeD((int) member.getSummon().getCurrentHp());
					writeD((int) member.getSummon().getMaxHp());
					writeD((int) member.getSummon().getCurrentMp());
					writeD((int) member.getSummon().getMaxMp());
					writeD(member.getSummon().getLevel());
				}
				else
				{
					writeD(0x00);
				}
			}
		}
	}
}