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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.commons.collections.JoinedIterator;
import l2e.gameserver.Config;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.interfaces.IL2Procedure;
import l2e.gameserver.model.matching.MatchingRoom;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.ExCloseMPCC;
import l2e.gameserver.network.serverpackets.ExMPCCPartyInfoUpdate;
import l2e.gameserver.network.serverpackets.ExOpenMPCC;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class CommandChannel implements PlayerGroup
{
	public static final int STRATEGY_GUIDE_ID = 8871;
	public static final int CLAN_IMPERIUM_ID = 391;
	
	private final List<Party> _commandChannelParties = new CopyOnWriteArrayList<>();
	private Player _commandChannelLeader;
	private int _commandChannelLvl;
	private Reflection _reflection = ReflectionManager.DEFAULT;
	
	private MatchingRoom _matchingRoom;
	
	public CommandChannel(Player leader)
	{
		_commandChannelLeader = leader;
		_commandChannelParties.add(leader.getParty());
		_commandChannelLvl = leader.getParty().getLevel();
		leader.getParty().setCommandChannel(this);
		broadCast(ExOpenMPCC.STATIC);
	}

	public void addParty(Party party)
	{
		if (party == null || party.getMemberCount() < 2)
		{
			return;
		}
		
		broadCast(new ExMPCCPartyInfoUpdate(party, 1));
		_commandChannelParties.add(party);
		refreshLevel();
		party.setCommandChannel(this);
		
		for (final Player member : party.getMembers())
		{
			member.sendPacket(ExOpenMPCC.STATIC);
			if (_matchingRoom != null && member == party.getLeader())
			{
				_matchingRoom.addMember(party.getLeader());
				party.getLeader().setMatchingRoomWindowOpened(true);
				party.getLeader().sendPacket(_matchingRoom.infoRoomPacket(), _matchingRoom.membersPacket(party.getLeader()));
				_matchingRoom.broadcastPlayerUpdate(party.getLeader());
			}
		}
		
		if (Config.PARTY_RANK_AUTO_OPEN)
		{
			for (final Player member : getMembers())
			{
				if (member != null)
				{
					final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler("rank");
					if (vch != null)
					{
						vch.useVoicedCommand("rank", member, null);
					}
				}
			}
		}
	}

	public void removeParty(Party party)
	{
		_commandChannelParties.remove(party);
		refreshLevel();
		party.setCommandChannel(null);
		party.broadCast(ExCloseMPCC.STATIC);
		if (!_reflection.isDefault())
		{
			for (final Player player : party.getMembers())
			{
				if (player != null && !player.getReflection().isDefault())
				{
					player.teleToLocation(_reflection.getReturnLoc(), true, ReflectionManager.DEFAULT);
				}
			}
		}
		
		if (_commandChannelParties.size() < 2)
		{
			disbandChannel();
		}
		else
		{
			broadCast(new ExMPCCPartyInfoUpdate(party, 0));
			if (_matchingRoom != null)
			{
				_matchingRoom.removeMember(party.getLeader(), false);
				_matchingRoom.broadcastPlayerUpdate(party.getLeader());
			}
		}
	}
	
	public void disbandChannel()
	{
		broadCast(SystemMessage.getSystemMessage(SystemMessageId.COMMAND_CHANNEL_DISBANDED));
		for (final Party party : _commandChannelParties)
		{
			party.setCommandChannel(null);
			party.broadCast(ExCloseMPCC.STATIC);
		}
		
		if (!_reflection.isDefault())
		{
			_reflection.setDuration(60000);
			_reflection.setEmptyDestroyTime(0);
			setReflection(ReflectionManager.DEFAULT);
		}
		
		if (_matchingRoom != null)
		{
			_matchingRoom.disband();
		}
		_commandChannelParties.clear();
		_commandChannelLeader = null;
	}
	
	@Override
	public int getMemberCount()
	{
		int count = 0;
		for (final Party party : _commandChannelParties)
		{
			count += party.getMemberCount();
		}
		return count;
	}
	
	@Override
	public void broadCast(GameServerPacket... gsp)
	{
		for (final Party party : _commandChannelParties)
		{
			party.broadCast(gsp);
		}
	}
	
	@Override
	public void broadCastMessage(ServerMessage msg)
	{
		for (final Party party : _commandChannelParties)
		{
			party.broadCastMessage(msg);
		}
	}
	
	public void broadcastToChannelPartyLeaders(GameServerPacket gsp)
	{
		for (final Party party : _commandChannelParties)
		{
			final Player leader = party.getLeader();
			if (leader != null)
			{
				leader.sendPacket(gsp);
			}
		}
	}
	
	public List<Party> getPartys()
	{
		return _commandChannelParties;
	}
	
	@Override
	public Player getGroupLeader()
	{
		return getLeader();
	}
	
	@Override
	public Iterator<Player> iterator()
	{
		final List<Iterator<Player>> iterators = new ArrayList<>(_commandChannelParties.size());
		for (final Party p : getPartys())
		{
			iterators.add(p.getMembers().iterator());
		}
		return new JoinedIterator<>(iterators);
	}
	
	public int getLevel()
	{
		return _commandChannelLvl;
	}
	
	public void setLeader(Player newLeader)
	{
		_commandChannelLeader = newLeader;
		broadCast(SystemMessage.getSystemMessage(SystemMessageId.COMMAND_CHANNEL_LEADER_NOW_C1).addPcName(newLeader));
	}
	
	public Player getLeader()
	{
		return _commandChannelLeader;
	}
	
	public int getLeaderObjectId()
	{
		return _commandChannelLeader.getObjectId();
	}
	
	private void refreshLevel()
	{
		_commandChannelLvl = 0;
		for (final Party pty : _commandChannelParties)
		{
			if (pty.getLevel() > _commandChannelLvl)
			{
				_commandChannelLvl = pty.getLevel();
			}
		}
	}
	
	public void setReflection(Reflection r)
	{
		_reflection = r;
	}
	
	public Reflection getReflection()
	{
		return _reflection;
	}
	
	public MatchingRoom getMatchingRoom()
	{
		return _matchingRoom;
	}
	
	public void setMatchingRoom(MatchingRoom matchingRoom)
	{
		_matchingRoom = matchingRoom;
	}
	
	public static Player checkAndAskToCreateChannel(Player player, Player target)
	{
		if (player.isOutOfControl())
		{
			player.sendActionFailed();
			return null;
		}
		
		if (player.isProcessingRequest())
		{
			player.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return null;
		}
		
		if (!player.isInParty() || player.getParty().getLeader() != player)
		{
			player.sendPacket(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
			return null;
		}
		
		if (target == null || player == target || !target.isInParty() || player.getParty() == target.getParty())
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return null;
		}
		
		if (target.isInParty() && !target.getParty().isLeader(target))
		{
			target = target.getParty().getLeader();
		}
		
		if (target == null)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return null;
		}
		
		if (target.getParty().isInCommandChannel())
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_ALREADY_MEMBER_OF_COMMAND_CHANNEL).addPcName(target));
			return null;
		}
		
		if (target.isProcessingRequest())
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER).addPcName(target));
			return null;
		}
		if (!checkCreationByClanCondition(player))
		{
			return null;
		}
		return target;
	}
	
	public static boolean checkCreationByClanCondition(Player creator)
	{
		if (creator == null || !creator.isInParty() || !creator.getParty().isLeader(creator) || creator.getPledgeClass() < Config.RANK_CLASS_FOR_CC)
		{
			creator.sendPacket(SystemMessageId.COMMAND_CHANNEL_ONLY_BY_LEVEL_5_CLAN_LEADER_PARTY_LEADER);
			return false;
		}
		
		final boolean haveSkill = creator.getSkillLevel(CLAN_IMPERIUM_ID) > 0;
		final boolean haveItem = creator.getInventory().getItemByItemId(STRATEGY_GUIDE_ID) != null;
		
		if (!haveSkill && !haveItem)
		{
			creator.sendPacket(SystemMessageId.COMMAND_CHANNEL_ONLY_BY_LEVEL_5_CLAN_LEADER_PARTY_LEADER);
			return false;
		}
		return true;
	}
	
	public List<Player> getMembers()
	{
		final List<Player> members = new LinkedList<>();
		for (final Party party : getPartys())
		{
			members.addAll(party.getMembers());
		}
		return members;
	}
	
	public boolean forEachMember(IL2Procedure<Player> procedure)
	{
		if ((_commandChannelParties != null) && !_commandChannelParties.isEmpty())
		{
			for (final Party party : _commandChannelParties)
			{
				if (!party.forEachMember(procedure))
				{
					return false;
				}
			}
		}
		return true;
	}

	public void broadcastCreatureSay(final CreatureSay msg, final Player broadcaster)
	{
		forEachMember(new IL2Procedure<Player>()
		{
			
			@Override
			public boolean execute(Player member)
			{
				if ((member != null) && !member.getBlockList().isBlocked(broadcaster))
				{
					member.sendPacket(msg);
				}
				return true;
			}
		});
	}

	public boolean isLeader(Player player)
	{
		return (getLeaderObjectId() == player.getObjectId());
	}
}