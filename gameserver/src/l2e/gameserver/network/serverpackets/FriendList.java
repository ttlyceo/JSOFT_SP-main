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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;

public class FriendList extends GameServerPacket
{
	private final List<FriendInfo> _info;
	
	private static class FriendInfo
	{
		int _objId;
		String _name;
		boolean _online;
		int _classid;
		int _level;
		
		public FriendInfo(int objId, String name, boolean online, int classid, int level)
		{
			_objId = objId;
			_name = name;
			_online = online;
			_classid = classid;
			_level = level;
		}
	}

	public FriendList(Player player)
	{
		_info = new ArrayList<>(player.getFriendList().size());
		for (final int objId : player.getFriendList())
		{
			final String name = CharNameHolder.getInstance().getNameById(objId);
			final Player player1 = GameObjectsStorage.getPlayer(objId);

			boolean online = false;
			int classid = 0;
			int level = 0;

			if (player1 == null)
			{
				Connection con = null;
				PreparedStatement statement = null;
				ResultSet rset = null;
				try
				{
					con = DatabaseFactory.getInstance().getConnection();
					statement = con.prepareStatement("SELECT char_name, online, classid, level FROM characters WHERE charId = ?");
					statement.setInt(1, objId);
					rset = statement.executeQuery();
					if (rset.next())
					{
						_info.add(new FriendInfo(objId, rset.getString(1), rset.getInt(2) == 1, rset.getInt(3), rset.getInt(4)));
					}
				}
				catch (final Exception e)
				{}
				finally
				{
					DbUtils.closeQuietly(con, statement, rset);
				}
				continue;
			}

			if (player1.isOnline())
			{
				online = true;
			}

			classid = player1.getClassId().getId();
			level = player1.getLevel();

			_info.add(new FriendInfo(objId, name, online, classid, level));
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
			writeD(info._classid);
			writeD(info._level);
		}
	}
}