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

import java.util.Collection;

import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.clanhall.SiegableHall;

public final class CastleSiegeAttackerList extends GameServerPacket
{
	private Castle _castle;
	private SiegableHall _hall;
	
	public CastleSiegeAttackerList(Castle castle)
	{
		_castle = castle;
	}
	
	public CastleSiegeAttackerList(SiegableHall hall)
	{
		_hall = hall;
	}

	@Override
	protected final void writeImpl()
	{
		if (_castle != null)
		{
			writeD(_castle.getId());
			writeD(0x00);
			writeD(0x01);
			writeD(0x00);
			final int size = _castle.getSiege().getAttackerClans().size();
			if (size > 0)
			{
				Clan clan;
				writeD(size);
				writeD(size);
				for (final SiegeClan siegeclan : _castle.getSiege().getAttackerClans())
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
		else
		{
			writeD(_hall.getId());
			writeD(0x00);
			writeD(0x01);
			writeD(0x00);
			final Collection<SiegeClan> attackers = _hall.getSiege().getAttackerClans();
			final int size = attackers.size();
			if (size > 0)
			{
				writeD(size);
				writeD(size);
				for (final SiegeClan sClan : attackers)
				{
					final Clan clan = ClanHolder.getInstance().getClan(sClan.getClanId());
					if (clan == null)
					{
						continue;
					}
					writeD(clan.getId());
					writeS(clan.getName());
					writeS(clan.getLeaderName());
					writeD(clan.getCrestId());
					writeD(0x00);
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
}