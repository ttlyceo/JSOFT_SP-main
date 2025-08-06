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

import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.entity.Castle;

public final class CastleSiegeDefenderList extends GameServerPacket
{
	private final Castle _castle;
	
	public CastleSiegeDefenderList(Castle castle)
	{
		_castle = castle;
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_castle.getId());
		writeD(0x00);
		writeD(0x01);
		writeD(0x00);
		final int size = _castle.getSiege().getDefenderClans().size() + _castle.getSiege().getDefenderWaitingClans().size();
		if (size > 0)
		{
			Clan clan;
			writeD(size);
			writeD(size);
			for (final SiegeClan siegeclan : _castle.getSiege().getDefenderClans())
			{
				clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
				if (clan == null)
				{
					continue;
				}
				writeD(clan.getId());
				writeS(clan.getName());
				writeS(clan.getLeaderName());
				writeD(clan.getCrestId());
				writeD(0x00);
				switch (siegeclan.getType())
				{
					case OWNER :
						writeD(0x01);
						break;
					case DEFENDER_PENDING :
						writeD(0x02);
						break;
					case DEFENDER :
						writeD(0x03);
						break;
					default :
						writeD(0x00);
						break;
				}
				writeD(clan.getAllyId());
				writeS(clan.getAllyName());
				writeS("");
				writeD(clan.getAllyCrestId());
			}
			for (final SiegeClan siegeclan : _castle.getSiege().getDefenderWaitingClans())
			{
				clan = ClanHolder.getInstance().getClan(siegeclan.getClanId());
				writeD(clan.getId());
				writeS(clan.getName());
				writeS(clan.getLeaderName());
				writeD(clan.getCrestId());
				writeD(0x00);
				writeD(0x02);
				writeD(clan.getAllyId());
				writeS(clan.getAllyName());
				writeS("");
				writeD(clan.getAllyCrestId());
			}
		}
		else
		{
			writeD(0x00);
			writeD(0x00);
		}
	}
}