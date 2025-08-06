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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.ExConfirmAddingPostFriend;

public class RequestExAddPostFriendForPostBox extends GameClientPacket
{
	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		if (!Config.ALLOW_MAIL)
		{
			return;
		}

		if (_name == null)
		{
			return;
		}

		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		final boolean charAdded = activeChar.getContactList().add(_name);
		activeChar.sendPacket(new ExConfirmAddingPostFriend(_name, charAdded));
	}
}