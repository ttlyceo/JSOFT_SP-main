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
package l2e.gameserver.network.clientpackets.pledge;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.clientpackets.GameClientPacket;
import l2e.gameserver.network.serverpackets.pledge.PledgeReceiveWarList;

public final class RequestPledgeWarList extends GameClientPacket
{
	protected int _unk1;
	private int _tab;
	
	@Override
	protected void readImpl()
	{
		_unk1 = readD();
		_tab = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();

		if (activeChar == null)
		{
			return;
		}

		if (activeChar.getClan() == null)
		{
			return;
		}

		activeChar.sendPacket(new PledgeReceiveWarList(activeChar.getClan(), _tab));
	}
}