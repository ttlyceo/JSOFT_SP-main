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

import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.TerritoryWarManager.Territory;

public class ExReplyDominionInfo extends GameServerPacket
{
	public static final ExReplyDominionInfo STATIC_PACKET = new ExReplyDominionInfo();

	private ExReplyDominionInfo()
	{
	}
	
	@Override
	protected void writeImpl()
	{
		final List<Territory> territoryList = TerritoryWarManager.getInstance().getAllTerritories();
		writeD(territoryList.size());
		for (final Territory t : territoryList)
		{
			writeD(t.getTerritoryId());
			writeS(CastleManager.getInstance().getCastleById(t.getCastleId()).getName("en").toLowerCase() + "_dominion");
			writeS(t.getOwnerClan().getName());
			writeD(t.getOwnedWardIds().size());
			for (final int i : t.getOwnedWardIds())
			{
				writeD(i);
			}
			writeD((int) (TerritoryWarManager.getInstance().getTWStartTimeInMillis() / 1000));
		}
	}
}