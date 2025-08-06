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

import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.model.GameObjectsStorage;

public class FriendStatus extends GameServerPacket
{
	private final boolean _online;
	private final int _objid;
	private final String _name;

	public FriendStatus(int objId)
	{
		_objid = objId;
		_name = CharNameHolder.getInstance().getNameById(objId);
		_online = GameObjectsStorage.getPlayer(objId) != null;
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_online ? 0x01 : 0x00);
		writeS(_name);
		writeD(_objid);
	}
}