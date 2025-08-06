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

import l2e.gameserver.data.parser.HennaParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Henna;
import l2e.gameserver.network.serverpackets.HennaUnequipInfo;

public final class RequestHennaUnequipInfo extends GameClientPacket
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
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		final Henna henna = HennaParser.getInstance().getHenna(_symbolId);
		if (henna == null)
		{
			_log.warn(getClass().getName() + ": Invalid Henna Id: " + _symbolId + " from player " + activeChar);
			sendActionFailed();
			return;
		}
		activeChar.sendPacket(new HennaUnequipInfo(henna, activeChar));
	}
}