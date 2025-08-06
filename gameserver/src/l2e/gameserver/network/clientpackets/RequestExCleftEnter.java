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
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;

/**
 * Created by LordWinter 03.12.2018
 */
public class RequestExCleftEnter extends GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding())
		{
			AerialCleftEvent.getInstance().removePlayer(activeChar.getObjectId(), true);
		}
		else
		{
			AerialCleftEvent.getInstance().removePlayer(activeChar.getObjectId(), false);
		}
	}
}