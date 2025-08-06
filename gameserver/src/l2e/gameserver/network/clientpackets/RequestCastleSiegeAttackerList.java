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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.clanhall.SiegableHall;
import l2e.gameserver.network.serverpackets.CastleSiegeAttackerList;

public final class RequestCastleSiegeAttackerList extends GameClientPacket
{
	private int _castleId;
	
	@Override
	protected void readImpl()
	{
		_castleId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if (castle != null)
		{
			final CastleSiegeAttackerList sal = new CastleSiegeAttackerList(castle);
			sendPacket(sal);
		}
		else
		{
			final SiegableHall hall = CHSiegeManager.getInstance().getSiegableHall(_castleId);
			if (hall != null)
			{
				final CastleSiegeAttackerList sal = new CastleSiegeAttackerList(hall);
				sendPacket(sal);
			}
		}
	}
}