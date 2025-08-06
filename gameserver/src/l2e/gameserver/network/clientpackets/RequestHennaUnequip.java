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
import l2e.gameserver.model.actor.templates.items.Henna;
import l2e.gameserver.network.SystemMessageId;

public final class RequestHennaUnequip extends GameClientPacket
{
	private int _symbolId;

	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		Henna henna;
		boolean found = false;
		for (int i = 1; i <= 3; i++)
		{
			henna = activeChar.getHenna(i);
			if ((henna != null) && (henna.getDyeId() == _symbolId))
			{
				if (activeChar.getAdena() >= henna.getCancelFee())
				{
					activeChar.removeHenna(i);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
					sendActionFailed();
				}
				found = true;
				break;
			}
		}
		if (!found)
		{
			_log.warn(getClass().getSimpleName() + ": Player " + activeChar + " requested Henna Draw remove without any henna.");
			sendActionFailed();
		}
	}
}