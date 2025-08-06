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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;

public class L2FriendList extends GameServerPacket
{
	private final List<FriendInfo> _info;

	private static class FriendInfo
	{
		int _objId;
		String _name;
		boolean _online;
		
		public FriendInfo(int objId, String name, boolean online)
		{
			_objId = objId;
			_name = name;
			_online = online;
		}
	}
	
	public L2FriendList(Player player)
	{
		_info = new ArrayList<>(player.getFriendList().size());
		for (final int objId : player.getFriendList())
		{
			final String name = CharNameHolder.getInstance().getNameById(objId);
			final Player player1 = GameObjectsStorage.getPlayer(objId);
			boolean online = false;
			if ((player1 != null) && player1.isOnline())
			{
				online = true;
			}
			_info.add(new FriendInfo(objId, name, online));
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_info.size());
		for (final FriendInfo info : _info)
		{
			writeD(info._objId);
			writeS(info._name);
			writeD(info._online ? 0x01 : 0x00);
			writeD(info._online ? info._objId : 0x00);
		}
	}
}