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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.FinishRotatings;

public final class FinishRotating extends GameClientPacket
{
	private int _degree;
	protected int _unknown;

	@Override
	protected void readImpl()
	{
		_degree = readD();
		_unknown = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		FinishRotatings sr;
		if (activeChar.isInAirShip() && activeChar.getAirShip().isCaptain(activeChar))
		{
			activeChar.getAirShip().setHeading(_degree);
			sr = new FinishRotatings(activeChar.getAirShip().getObjectId(), _degree, 0);
			activeChar.getAirShip().broadcastPacket(sr);
		}
		else
		{
			activeChar.setHeading(_degree);
			sr = new FinishRotatings(activeChar.getObjectId(), _degree, 0);
			activeChar.broadcastPacket(sr);
		}
	}
}