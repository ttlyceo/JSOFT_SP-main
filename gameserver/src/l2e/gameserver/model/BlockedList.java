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
package l2e.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class BlockedList extends LoggerObject
{
	private final Map<Integer, List<Integer>> _offlineList = new ConcurrentHashMap<>();
	private final Player _owner;
	private List<Integer> _blockList;

	public BlockedList(Player owner)
	{
		_owner = owner;
		_blockList = _offlineList.get(owner.getObjectId());
		if (_blockList == null)
		{
			_blockList = loadList(_owner.getObjectId());
		}
	}

	private void addToBlockList(int target)
	{
		_blockList.add(target);
		updateInDB(target, true);
	}

	private void removeFromBlockList(int target)
	{
		_blockList.remove(Integer.valueOf(target));
		updateInDB(target, false);
	}

	public void playerLogout()
	{
		_offlineList.put(_owner.getObjectId(), _blockList);
	}

	private List<Integer> loadList(int ObjId)
	{
		final List<Integer> list = new ArrayList<>();
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=1");
			statement.setInt(1, ObjId);
			rset = statement.executeQuery();
			
			int friendId;
			
			while (rset.next())
			{
				friendId = rset.getInt("friendId");
				if (friendId == ObjId)
				{
					continue;
				}
				list.add(friendId);
			}
		}
		catch (final Exception e)
		{
			warn("Error found in " + ObjId + " FriendList while loading BlockList: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return list;
	}

	private void updateInDB(int targetId, boolean state)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			if (state)
			{
				statement = con.prepareStatement("INSERT INTO character_friends (charId, friendId, relation) VALUES (?, ?, 1)");
				statement.setInt(1, _owner.getObjectId());
				statement.setInt(2, targetId);
				statement.execute();
			}
			else
			{
				statement = con.prepareStatement("DELETE FROM character_friends WHERE charId=? AND friendId=? AND relation=1");
				statement.setInt(1, _owner.getObjectId());
				statement.setInt(2, targetId);
				statement.execute();
			}
		}
		catch (final Exception e)
		{
			warn("Could not add block player: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public boolean isInBlockList(Player target)
	{
		return _blockList.contains(target.getObjectId());
	}

	public boolean isInBlockList(int targetId)
	{
		return _blockList.contains(targetId);
	}

	private boolean isBlockAll()
	{
		return _owner.getMessageRefusal();
	}

	public boolean isBlocked(Player target)
	{
		return isBlockAll() || isInBlockList(target);
	}

	public boolean isBlocked(int targetId)
	{
		return isBlockAll() || isInBlockList(targetId);
	}

	public void setBlockAll(boolean state)
	{
		_owner.setMessageRefusal(state);
	}

	public List<Integer> getBlockList()
	{
		return _blockList;
	}

	public void addTargetToBlockList(int targetId)
	{
		final String charName = CharNameHolder.getInstance().getNameById(targetId);
		if (_owner.getFriendList().contains(targetId))
		{
			final var sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST);
			sm.addString(charName);
			_owner.sendPacket(sm);
			return;
		}

		if (getBlockList().contains(targetId))
		{
			_owner.sendMessage("Already in ignore list.");
			return;
		}

		addToBlockList(targetId);

		var sm = SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_ADDED_TO_YOUR_IGNORE_LIST);
		sm.addString(charName);
		_owner.sendPacket(sm);

		final var player = GameObjectsStorage.getPlayer(targetId);
		if (player != null)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
			sm.addString(_owner.getName(null));
			player.sendPacket(sm);
		}
	}

	public void removeTargetFromBlockList(int targetId)
	{
		SystemMessage sm;
		final String charName = CharNameHolder.getInstance().getNameById(targetId);
		if (!getBlockList().contains(targetId))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT);
			_owner.sendPacket(sm);
			return;
		}

		removeFromBlockList(targetId);

		sm = SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_REMOVED_FROM_YOUR_IGNORE_LIST);
		sm.addString(charName);
		_owner.sendPacket(sm);
	}

	public void sendListToOwner()
	{
		int i = 1;
		_owner.sendPacket(SystemMessageId.BLOCK_LIST_HEADER);
		for (final int playerId : getBlockList())
		{
			_owner.sendMessage((i++) + ". " + CharNameHolder.getInstance().getNameById(playerId));
		}
		_owner.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
	}

	public boolean isInBlockList(int ownerId, int targetId)
	{
		final var player = GameObjectsStorage.getPlayer(ownerId);
		if (player != null)
		{
			return player.getBlockList().isBlocked(targetId);
		}
		if (!_offlineList.containsKey(ownerId))
		{
			_offlineList.put(ownerId, loadList(ownerId));
		}
		return _offlineList.get(ownerId).contains(targetId);
	}
}