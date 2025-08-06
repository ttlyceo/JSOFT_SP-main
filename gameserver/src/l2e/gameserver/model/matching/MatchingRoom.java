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
package l2e.gameserver.model.matching;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.MatchingRoomManager;
import l2e.gameserver.listener.player.OnPlayerPartyInviteListener;
import l2e.gameserver.listener.player.OnPlayerPartyLeaveListener;
import l2e.gameserver.model.PlayerGroup;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;

public abstract class MatchingRoom implements PlayerGroup
{
	private class PartyListenerImpl implements OnPlayerPartyInviteListener, OnPlayerPartyLeaveListener
	{
		@Override
		public void onPartyInvite(Player player)
		{
			broadcastPlayerUpdate(player);
		}
		
		@Override
		public void onPartyLeave(Player player)
		{
			broadcastPlayerUpdate(player);
		}
	}
	
	public static int PARTY_MATCHING = 0;
	public static int CC_MATCHING = 1;

	public static int WAIT_PLAYER = 0;
	public static int ROOM_MASTER = 1;
	public static int PARTY_MEMBER = 2;
	public static int UNION_LEADER = 3;
	public static int UNION_PARTY = 4;
	public static int WAIT_PARTY = 5;
	public static int WAIT_NORMAL = 6;

	private final int _id;
	private int _minLevel;
	private int _maxLevel;
	private int _maxMemberSize;
	private int _lootType;
	private String _topic;

	private final PartyListenerImpl _listener = new PartyListenerImpl();
	protected Player _leader;
	protected Set<Player> _members = new CopyOnWriteArraySet<>();

	public MatchingRoom(Player leader, int minLevel, int maxLevel, int maxMemberSize, int lootType, String topic)
	{
		_leader = leader;
		_id = MatchingRoomManager.getInstance().addMatchingRoom(this);
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_maxMemberSize = maxMemberSize;
		_lootType = lootType;
		_topic = topic;

		addMember0(leader, null, true);
	}

	public boolean addMember(Player player)
	{
		if (_members.contains(player))
		{
			return true;
		}

		if (player.getLevel() < getMinLevel() || player.getLevel() > getMaxLevel() || getPlayers().size() >= getMaxMembersSize())
		{
			player.sendPacket(notValidMessage());
			return false;
		}
		return addMember0(player, SystemMessage.getSystemMessage(enterMessage()).addPcName(player), true);
	}
	
	public boolean addMemberForce(Player player)
	{
		if (_members.contains(player))
		{
			return true;
		}

		if (getPlayers().size() >= getMaxMembersSize())
		{
			player.sendPacket(notValidMessage());
			return false;
		}

		return addMember0(player, SystemMessage.getSystemMessage(enterMessage()).addPcName(player), false);
	}

	private boolean addMember0(Player player, GameServerPacket p, boolean sendInfo)
	{
		if (!_members.isEmpty())
		{
			player.addListener(_listener);
		}
		_members.add(player);
		player.setMatchingRoom(this);
		for (final Player member : this)
		{
			if (member != player)
			{
				member.sendPacket(p, addMemberPacket(member, player));
			}
		}

		MatchingRoomManager.getInstance().removeFromWaitingList(player);
		if (sendInfo)
		{
			player.setMatchingRoomWindowOpened(true);
			player.sendPacket(infoRoomPacket(), membersPacket(player));
		}
		return true;
	}

	public void removeMember(Player member, boolean oust)
	{
		if (member == null)
		{
			return;
		}
		
		if (!_members.remove(member))
		{
			return;
		}
		member.removeListener(_listener);
		member.setMatchingRoom(null);
		if (_members.isEmpty())
		{
			disband();
		}
		else
		{
			final GameServerPacket infoPacket = infoRoomPacket();
			final SystemMessageId exitMessage0 = exitMessage(true, oust);
			final GameServerPacket exitMessage = exitMessage0 != null ? SystemMessage.getSystemMessage(exitMessage0).addPcName(member) : null;
			for (final Player player : this)
			{
				player.sendPacket(infoPacket, removeMemberPacket(player, member), exitMessage);
			}
		}

		member.sendPacket(closeRoomPacket(), exitMessage(false, oust));
		member.setMatchingRoomWindowOpened(false);
		member.broadcastCharInfo();
	}

	public void broadcastPlayerUpdate(Player player)
	{
		for (final Player member : MatchingRoom.this)
		{
			if (member.isMatchingRoomWindowOpened())
			{
				member.sendPacket(updateMemberPacket(member, player));
			}
		}
	}

	public void disband()
	{
		for (final Player player : this)
		{
			player.removeListener(_listener);
			if (player.isMatchingRoomWindowOpened())
			{
				player.sendPacket(closeRoomMessage());
				player.sendPacket(closeRoomPacket());
			}
			player.setMatchingRoom(null);
			player.broadcastCharInfo();
		}
		_members.clear();
		MatchingRoomManager.getInstance().removeMatchingRoom(this);
	}

	public void setLeader(Player leader)
	{
		_leader = leader;
		
		if (!_members.contains(leader))
		{
			addMember0(leader, null, true);
		}
		else
		{
			if (!leader.isMatchingRoomWindowOpened())
			{
				leader.setMatchingRoomWindowOpened(true);
				leader.sendPacket(infoRoomPacket(), membersPacket(leader));
			}
			final SystemMessageId changeLeaderMessage = changeLeaderMessage();
			for (final Player member : this)
			{
				if (member.isMatchingRoomWindowOpened())
				{
					member.sendPacket(updateMemberPacket(member, leader), changeLeaderMessage);
				}
			}
		}
	}
	
	public abstract SystemMessageId notValidMessage();

	public abstract SystemMessageId enterMessage();

	public abstract SystemMessageId exitMessage(boolean toOthers, boolean kick);

	public abstract SystemMessageId closeRoomMessage();

	public abstract SystemMessageId changeLeaderMessage();

	public abstract GameServerPacket closeRoomPacket();

	public abstract GameServerPacket infoRoomPacket();

	public abstract GameServerPacket addMemberPacket(Player member, Player active);

	public abstract GameServerPacket removeMemberPacket(Player member, Player active);

	public abstract GameServerPacket updateMemberPacket(Player member, Player active);

	public abstract GameServerPacket membersPacket(Player active);

	public abstract int getType();

	public abstract int getMemberType(Player member);
	
	@Override
	public void broadCast(GameServerPacket... arg)
	{
		for (final Player player : this)
		{
			player.sendPacket(arg);
		}
	}
	
	@Override
	public void broadCastMessage(ServerMessage msg)
	{
		for (final Player player : this)
		{
			player.sendMessage(msg.toString(player.getLang()));
		}
	}

	public int getId()
	{
		return _id;
	}

	public int getMinLevel()
	{
		return _minLevel;
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}

	public String getTopic()
	{
		return _topic;
	}

	public int getMaxMembersSize()
	{
		return _maxMemberSize;
	}

	public int getLocationId()
	{
		return MapRegionManager.getInstance().getBBs(_leader.getLocation());
	}

	public Player getLeader()
	{
		return _leader;
	}

	public Collection<Player> getPlayers()
	{
		return _members;
	}

	public int getLootType()
	{
		return _lootType;
	}

	@Override
	public int getMemberCount()
	{
		return getPlayers().size();
	}

	@Override
	public Player getGroupLeader()
	{
		return getLeader();
	}

	@Override
	public Iterator<Player> iterator()
	{
		return _members.iterator();
	}

	public void setMinLevel(int minLevel)
	{
		_minLevel = minLevel;
	}

	public void setMaxLevel(int maxLevel)
	{
		_maxLevel = maxLevel;
	}

	public void setTopic(String topic)
	{
		_topic = topic;
	}

	public void setMaxMemberSize(int maxMemberSize)
	{
		_maxMemberSize = maxMemberSize;
	}

	public void setLootType(int lootType)
	{
		_lootType = lootType;
	}
}