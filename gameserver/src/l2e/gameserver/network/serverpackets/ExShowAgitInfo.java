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

import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.model.entity.clanhall.AuctionableHall;

public class ExShowAgitInfo extends GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		final Map<Integer, AuctionableHall> clannhalls = ClanHallManager.getInstance().getAllAuctionableClanHalls();
		writeD(clannhalls.size());
		for (final AuctionableHall ch : clannhalls.values())
		{
			writeD(ch.getId());
			writeS(ch.getOwnerId() <= 0 ? "" : ClanHolder.getInstance().getClan(ch.getOwnerId()).getName());
			writeS(ch.getOwnerId() <= 0 ? "" : ClanHolder.getInstance().getClan(ch.getOwnerId()).getLeaderName());
			writeD(ch.getGrade() > 0 ? 0x00 : 0x01);
		}
	}
}