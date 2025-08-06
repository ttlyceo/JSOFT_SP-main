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

import l2e.gameserver.model.actor.Player;

public class ExReceiveShowPostFriend extends GameServerPacket
{
	private final List<String> _contacts;
	
	public ExReceiveShowPostFriend(Player player)
	{
		_contacts = player.getContactList().getAllContacts();
	}

	@Override
	protected void writeImpl()
	{
		writeD(_contacts.size());
		for (final String name : _contacts)
		{
			writeS(name);
		}
	}
}