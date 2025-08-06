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

import l2e.gameserver.model.actor.Creature;

public class NickNameChanged extends GameServerPacket
{
	private final String _title;
	private final int _objectId;

	public NickNameChanged(Creature cha)
	{
		_objectId = cha.getObjectId();
		_title = cha.getTitle(null);
	}

	@Override
	protected void writeImpl()
	{
		writeD(_objectId);
		writeS(_title);
	}
}