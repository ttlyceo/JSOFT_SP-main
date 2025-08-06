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

import java.util.List;

import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.model.entity.Castle;

public class ExShowCastleInfo extends GameServerPacket
{
	public ExShowCastleInfo()
	{
	}
	
	@Override
	protected void writeImpl()
	{
		final List<Castle> castles = CastleManager.getInstance().getCastles();
		writeD(castles.size());
		for (final Castle castle : castles)
		{
			writeD(castle.getId());
			if (castle.getOwnerId() > 0)
			{
				if (ClanHolder.getInstance().getClan(castle.getOwnerId()) != null)
				{
					writeS(ClanHolder.getInstance().getClan(castle.getOwnerId()).getName());
				}
				else
				{
					writeS("");
				}
			}
			else
			{
				writeS("");
			}
			writeD(castle.getTaxPercent());
			writeD((int) (castle.getSiege().getSiegeStartTime() / 1000));
		}
	}
}