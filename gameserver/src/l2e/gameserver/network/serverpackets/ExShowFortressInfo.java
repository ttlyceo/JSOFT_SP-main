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

import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.entity.Fort;

public class ExShowFortressInfo extends GameServerPacket
{
	public static final ExShowFortressInfo STATIC_PACKET = new ExShowFortressInfo();

	private ExShowFortressInfo()
	{
	}

	@Override
	protected void writeImpl()
	{
		final List<Fort> forts = FortManager.getInstance().getForts();
		writeD(forts.size());
		for (final Fort fort : forts)
		{
			final Clan clan = fort.getOwnerClan();
			writeD(fort.getId());
			writeS(clan != null ? clan.getName() : "");
			writeD(fort.getSiege().getIsInProgress() ? 0x01 : 0x00);
			writeD(fort.getOwnedTime());
		}
	}
}