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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.SevenSignsFestival;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.DuelManager;
import l2e.gameserver.instancemanager.MatchingRoomManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.ServitorInstance;
import l2e.gameserver.model.actor.templates.player.ranking.PartyTemplate;
import l2e.gameserver.model.entity.DimensionalRift;
import l2e.gameserver.model.entity.underground_coliseum.UCTeam;
import l2e.gameserver.model.entity.underground_coliseum.UCWaiting;
import l2e.gameserver.model.interfaces.IL2Procedure;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.matching.MatchingRoom;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.ExAskModifyPartyLooting;
import l2e.gameserver.network.serverpackets.ExCloseMPCC;
import l2e.gameserver.network.serverpackets.ExOpenMPCC;
import l2e.gameserver.network.serverpackets.ExPVPMatchRecord;
import l2e.gameserver.network.serverpackets.ExPartyPetWindowAdd;
import l2e.gameserver.network.serverpackets.ExPartyPetWindowDelete;
import l2e.gameserver.network.serverpackets.ExReplyHandOverPartyMaster;
import l2e.gameserver.network.serverpackets.ExSetPartyLooting;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.PartyMemberPosition;
import l2e.gameserver.network.serverpackets.PartySmallWindowAdd;
import l2e.gameserver.network.serverpackets.PartySmallWindowAll;
import l2e.gameserver.network.serverpackets.PartySmallWindowDelete;
import l2e.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import l2e.gameserver.network.serverpackets.SetDismissParty;
import l2e.gameserver.network.serverpackets.SetOustPartyMember;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Party implements PlayerGroup
{
	private static final Logger _log = LoggerFactory.getLogger(Party.class);

	private static final double[] BONUS_EXP_SP =
	{
	        1, 1.10, 1.20, 1.30, 1.40, 1.50, 2.0, 2.10, 2.20
	};

	private static final int[] LOOT_SYSSTRINGS =
	{
	        487, 488, 798, 799, 800
	};

	private static final int PARTY_POSITION_BROADCAST_DELAY = 12000;

	public static final byte ITEM_LOOTER = 0;
	public static final byte ITEM_RANDOM = 1;
	public static final byte ITEM_RANDOM_SPOIL = 2;
	public static final byte ITEM_ORDER = 3;
	public static final byte ITEM_ORDER_SPOIL = 4;

	private final List<Player> _members;
	private final Map<Integer, PartyTemplate> _ranking;
	private final Map<Integer, Map<Skill, Long>> _partyEffects;
	private boolean _pendingInvitation = false;
	private long _pendingInviteTimeout;
	private int _partyLvl = 0;
	private int _itemDistribution = 0;
	private int _itemLastLoot = 0;
	private CommandChannel _commandChannel = null;
	private DimensionalRift _dr;
	private byte _requestChangeLoot = -1;
	private List<Integer> _changeLootAnswers = null;
	protected long _requestChangeLootTimer = 0;
	private Future<?> _checkTask = null;
	private Future<?> _positionBroadcastTask = null;
	protected PartyMemberPosition _positionPacket;
	private boolean _disbanding = false;
	private Object _ucState = null;
	
	private double _rateXp;
	private double _rateSp;
	private double _rateFishing;
	private double _dropSiege;
	private double _dropElementStones;
	private double _dropSealStones;
	private double _questRewardRate;
	private double _questDropRate;
	private double _dropAdena;
	private double _dropItems;
	private double _dropRaids;
	private double _dropEpics;
	private double _dropSpoil;

	public enum messageType
	{
		Expelled, Left, None, Disconnected
	}
	
	public void recalculatePartyData()
	{
		double rateXp = 0.;
		double rateSp = 0.;
		double rateFishing = 0.;
		double dropSiege = 0.;
		double dropElementStones = 0.;
		double dropSealStones = 0.;
		double questRewardRate = 0.;
		double questDropRate = 0.;
		double dropAdena = 0.;
		double dropItems = 0.;
		double dropRaids = 0.;
		double dropEpics = 0.;
		double dropSpoil = 0.;
		
		int count = 0;
		
		for (final Player member : _members)
		{
			if (member == null)
			{
				continue;
			}
			
			count++;
			
			rateXp += member.getPremiumBonus().getRateXp();
			rateSp += member.getPremiumBonus().getRateSp();
			rateFishing += member.getPremiumBonus().getFishingRate();
			dropSiege += member.getPremiumBonus().getDropSiege();
			dropElementStones += member.getPremiumBonus().getDropElementStones();
			dropSealStones += member.getPremiumBonus().getDropSealStones();
			questRewardRate += member.getPremiumBonus().getQuestRewardRate();
			questDropRate += member.getPremiumBonus().getQuestDropRate();
			dropAdena += member.getPremiumBonus().getDropAdena();
			dropItems += member.getPremiumBonus().getDropItems();
			dropRaids += member.getPremiumBonus().getDropRaids();
			dropEpics += member.getPremiumBonus().getDropEpics();
			dropSpoil += member.getPremiumBonus().getDropSpoil();
		}
		
		_rateXp = Math.max(1., rateXp / count);
		_rateSp = Math.max(1., rateSp / count);
		_rateFishing = Math.max(1., rateFishing / count);
		_dropSiege = Math.max(1., dropSiege / count);
		_dropElementStones = Math.max(1., dropElementStones / count);
		_dropSealStones = Math.max(1., dropSealStones / count);
		_questRewardRate = Math.max(1., questRewardRate / count);
		_questDropRate = Math.max(1., questDropRate / count);
		_dropAdena = Math.max(1., dropAdena / count);
		_dropItems = Math.max(1., dropItems / count);
		_dropRaids = Math.max(1., dropRaids / count);
		_dropEpics = Math.max(1., dropEpics / count);
		_dropSpoil = Math.max(1., dropSpoil / count);
	}

	public Party(Player leader, int itemDistribution)
	{
		_members = new CopyOnWriteArrayList<>();
		_members.add(leader);
		_partyLvl = leader.getLevel();
		_rateXp = leader.getPremiumBonus().getRateXp();
		_rateSp = leader.getPremiumBonus().getRateSp();
		_rateFishing = leader.getPremiumBonus().getFishingRate();
		_dropSiege = leader.getPremiumBonus().getDropSiege();
		_dropElementStones = leader.getPremiumBonus().getDropElementStones();
		_dropSealStones = leader.getPremiumBonus().getDropSealStones();
		_questRewardRate = leader.getPremiumBonus().getQuestRewardRate();
		_questDropRate = leader.getPremiumBonus().getQuestDropRate();
		_dropAdena = leader.getPremiumBonus().getDropAdena();
		_dropItems = leader.getPremiumBonus().getDropItems();
		_dropRaids = leader.getPremiumBonus().getDropRaids();
		_dropEpics = leader.getPremiumBonus().getDropEpics();
		_dropSpoil = leader.getPremiumBonus().getDropSpoil();
		_itemDistribution = itemDistribution;
		
		_ranking = new ConcurrentHashMap<>();
		_partyEffects = new ConcurrentHashMap<>();
		if (Config.ALLOW_PARTY_RANK_COMMAND)
		{
			_ranking.put(leader.getObjectId(), new PartyTemplate());
		}
	}

	public boolean getPendingInvitation()
	{
		return _pendingInvitation;
	}

	public void setPendingInvitation(boolean val)
	{
		_pendingInvitation = val;
		_pendingInviteTimeout = GameTimeController.getInstance().getGameTicks() + (Player.REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND);
	}

	public boolean isInvitationRequestExpired()
	{
		return (_pendingInviteTimeout <= GameTimeController.getInstance().getGameTicks());
	}

	private Player getCheckedRandomMember(int itemId, Creature target)
	{
		final List<Player> availableMembers = new ArrayList<>();
		for (final Player member : getMembers())
		{
			if (member.isDead())
			{
				continue;
			}
			
			if (member.getInventory().validateCapacityByItemId(itemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true))
			{
				availableMembers.add(member);
			}
		}
		if (!availableMembers.isEmpty())
		{
			return availableMembers.get(Rnd.get(availableMembers.size()));
		}
		return null;
	}

	private Player getCheckedNextLooter(int ItemId, Creature target)
	{
		for (int i = 0; i < getMemberCount(); i++)
		{
			if (++_itemLastLoot >= getMemberCount())
			{
				_itemLastLoot = 0;
			}
			Player member;
			try
			{
				member = getMembers().get(_itemLastLoot);
				if (member != null && !member.isDead() && member.getInventory().validateCapacityByItemId(ItemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true))
				{
					return member;
				}
			}
			catch (final Exception e)
			{}
		}
		return null;
	}

	private Player getActualLooter(Player player, int ItemId, boolean spoil, Creature target)
	{
		Player looter = player;

		switch (_itemDistribution)
		{
			case ITEM_RANDOM :
				if (!spoil)
				{
					looter = getCheckedRandomMember(ItemId, target);
				}
				break;
			case ITEM_RANDOM_SPOIL :
				looter = getCheckedRandomMember(ItemId, target);
				break;
			case ITEM_ORDER :
				if (!spoil)
				{
					looter = getCheckedNextLooter(ItemId, target);
				}
				break;
			case ITEM_ORDER_SPOIL :
				looter = getCheckedNextLooter(ItemId, target);
				break;
		}

		if (looter == null)
		{
			looter = player;
		}
		return looter;
	}

	@Deprecated
	public void broadcastToPartyMembers(GameServerPacket packet)
	{
		broadCast(packet);
	}

	public void broadcastToPartyMembersNewLeader()
	{
		for (final Player member : getMembers())
		{
			if (member != null)
			{
				member.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
				member.sendPacket(new PartySmallWindowAll(member, this));
				member.broadcastUserInfo(true);
			}
		}
	}

	public void broadcastToPartyMembers(Player player, GameServerPacket msg)
	{
		for (final Player member : getMembers())
		{
			if ((member != null) && (member.getObjectId() != player.getObjectId()))
			{
				member.sendPacket(msg);
			}
		}
	}

	public void addPartyMember(Player player)
	{
		if (getMembers().contains(player))
		{
			return;
		}

		if (_requestChangeLoot != -1)
		{
			finishLootRequest(false);
		}

		player.sendPacket(new PartySmallWindowAll(player, this));

		for (final Player pMember : getMembers())
		{
			if ((pMember != null) && pMember.hasSummon())
			{
				player.sendPacket(new ExPartyPetWindowAdd(pMember.getSummon()));
			}
		}

		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOU_JOINED_S1_PARTY);
		msg.addString(getLeader().getName(null));
		player.sendPacket(msg);

		msg = SystemMessage.getSystemMessage(SystemMessageId.C1_JOINED_PARTY);
		msg.addString(player.getName(null));
		broadCast(msg);
		broadCast(new PartySmallWindowAdd(player, this));

		if (player.hasSummon())
		{
			broadCast(new ExPartyPetWindowAdd(player.getSummon()));
		}

		getMembers().add(player);
		if (player.getLevel() > _partyLvl)
		{
			_partyLvl = player.getLevel();
		}
		recalculatePartyData();
		
		if (Config.ALLOW_PARTY_RANK_COMMAND)
		{
			_ranking.put(player.getObjectId(), new PartyTemplate());
			if (Config.PARTY_RANK_AUTO_OPEN)
			{
				final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler("rank");
				if (vch != null)
				{
					vch.useVoicedCommand("rank", player, null);
					if (getMemberCount() == 2)
					{
						vch.useVoicedCommand("rank", getLeader(), null);
					}
				}
			}
		}

		Summon summon;
		for (final Player member : getMembers())
		{
			if (member != null)
			{
				member.updateEffectIcons(true);
				summon = member.getSummon();
				member.broadcastUserInfo(true);
				if (summon != null)
				{
					summon.updateEffectIcons();
				}
			}
		}

		if (isInCommandChannel())
		{
			player.sendPacket(ExOpenMPCC.STATIC);
		}
		
		final MatchingRoom currentRoom = player.getMatchingRoom();
		final MatchingRoom room = getLeader().getMatchingRoom();
		if (currentRoom != null && currentRoom != room)
		{
			currentRoom.removeMember(player, false);
		}
		if (room != null && room.getType() == MatchingRoom.PARTY_MATCHING)
		{
			room.addMemberForce(player);
		}
		else
		{
			MatchingRoomManager.getInstance().removeFromWaitingList(player);
		}
		
		BotFunctions.getInstance().checkAutoDropFunction(player);

		if (_positionBroadcastTask == null)
		{
			_positionBroadcastTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new PositionBroadcast(), PARTY_POSITION_BROADCAST_DELAY / 2, PARTY_POSITION_BROADCAST_DELAY);
		}
	}

	public void removePartyMember(String name, messageType type)
	{
		removePartyMember(getPlayerByName(name), type);
	}

	public void removePartyMember(Player player, messageType type)
	{
		if (getMembers().contains(player))
		{
			final boolean isLeader = isLeader(player);
			if (!_disbanding)
			{
				if ((getMembers().size() == 2) || (isLeader && !Config.ALT_LEAVE_PARTY_LEADER && (type != messageType.Disconnected)))
				{
					disbandParty();
					return;
				}
				else
				{
					if (player.getUCState() != Player.UC_STATE_NONE)
					{
						player.sendPacket(new ExPVPMatchRecord(2, 0));
					}
				}
			}

			getMembers().remove(player);
			checkPatryEffecs(player);
			player.getListeners().onPartyLeave();
			
			if (player.getUCState() != Player.UC_STATE_NONE)
			{
				player.setTeam(0);
				player.cleanUCStats();
				player.setUCState(Player.UC_STATE_NONE);
				if (player.isDead())
				{
					UCTeam.resPlayer(player);
				}
				
				if (player.getSaveLoc() != null)
				{
					player.teleToLocation(player.getSaveLoc(), true, ReflectionManager.DEFAULT);
				}
				else
				{
					player.teleToLocation(TeleportWhereType.TOWN, true, ReflectionManager.DEFAULT);
				}
			}
			
			if (Config.ALLOW_PARTY_RANK_COMMAND)
			{
				_ranking.remove(player.getObjectId());
			}
			recalculatePartyLevel();
			recalculatePartyData();

			if (player.isFestivalParticipant())
			{
				SevenSignsFestival.getInstance().updateParticipants(player, this);
			}

			if (player.isInDuel())
			{
				DuelManager.getInstance().onRemoveFromParty(player);
			}

			try
			{
				if (player.getFusionSkill() != null)
				{
					player.abortCast();
				}

				for (final Creature character : World.getInstance().getAroundCharacters(player))
				{
					if ((character.getFusionSkill() != null) && (character.getFusionSkill().getTarget() == player))
					{
						character.abortCast();
					}
				}
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}

			SystemMessage msg;
			if (type == messageType.Expelled)
			{
				player.sendPacket(SetOustPartyMember.STATIC_PACKET);
				msg = SystemMessage.getSystemMessage(SystemMessageId.C1_WAS_EXPELLED_FROM_PARTY);
				msg.addString(player.getName(null));
				broadCast(msg);
			}
			else if ((type == messageType.Left) || (type == messageType.Disconnected))
			{
				player.sendPacket(SystemMessageId.YOU_LEFT_PARTY);
				msg = SystemMessage.getSystemMessage(SystemMessageId.C1_LEFT_PARTY);
				msg.addString(player.getName(null));
				broadCast(msg);
			}

			player.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
			player.setParty(null);
			broadCast(new PartySmallWindowDelete(player));
			if (player.hasSummon())
			{
				broadCast(new ExPartyPetWindowDelete(player.getSummon()));
			}
			player.broadcastUserInfo(true);

			if (isInCommandChannel())
			{
				player.sendPacket(new ExCloseMPCC());
			}

			final MatchingRoom room = getLeader() != null ? getLeader().getMatchingRoom() : null;
			if (room != null)
			{
				if (room.getType() == MatchingRoom.PARTY_MATCHING)
				{
					if (isLeader)
					{
						room.disband();
					}
					else
					{
						room.removeMember(player, false);
					}
				}
			}

			if (isLeader && (getMembers().size() > 1) && (Config.ALT_LEAVE_PARTY_LEADER || (type == messageType.Disconnected)))
			{
				msg = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_BECOME_A_PARTY_LEADER);
				msg.addString(getLeader().getName(null));
				broadCast(msg);
				broadcastToPartyMembersNewLeader();
				if (isInCommandChannel() && _commandChannel.getLeader() == player)
				{
					_commandChannel.setLeader(getLeader());
				}
			}
			else if (getMembers().size() == 1)
			{
				if (isInCommandChannel())
				{
					if (getCommandChannel().getLeader().getObjectId() == getLeader().getObjectId())
					{
						getCommandChannel().disbandChannel();
					}
					else
					{
						getCommandChannel().removeParty(this);
					}
				}
				
				if (_dr != null)
				{
					_dr.manualExit();
				}

				final Player pl = getLeader();
				if (pl != null)
				{
					pl.setParty(null);
					if (pl.isInDuel())
					{
						DuelManager.getInstance().onRemoveFromParty(pl);
					}
				}
				
				try
				{
					if (_checkTask != null)
					{
						_checkTask.cancel(true);
						_checkTask = null;
					}
					if (_positionBroadcastTask != null)
					{
						_positionBroadcastTask.cancel(false);
						_positionBroadcastTask = null;
					}
				}
				catch (final Exception e)
				{
				}
				_members.clear();
				_ranking.clear();
				_partyEffects.clear();
			}
		}
	}

	public void disbandParty()
	{
		_disbanding = true;
		checkUCStatus();
		if (_members != null)
		{
			broadCast(SetDismissParty.STATIC_PACKET);
			for (final Player member : _members)
			{
				if (member != null)
				{
					removePartyMember(member, messageType.None);
				}
			}
		}
	}

	public void changePartyLeader(String name)
	{
		setLeader(getPlayerByName(name));
	}
	
	public boolean isLeader(Player player)
	{
		return getLeader() == player;
	}

	public void setLeader(Player player)
	{
		if ((player != null) && !player.isInDuel())
		{
			if (getMembers().contains(player))
			{
				if (isLeader(player))
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF);
				}
				else
				{
					final Player temp = getLeader();
					final int p1 = getMembers().indexOf(player);
					getMembers().set(0, player);
					getMembers().set(p1, temp);
					
					temp.sendPacket(ExReplyHandOverPartyMaster.FALSE);
					player.sendPacket(ExReplyHandOverPartyMaster.TRUE);

					final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_BECOME_A_PARTY_LEADER);
					msg.addString(getLeader().getName(null));
					broadCast(msg);
					broadcastToPartyMembersNewLeader();
					if (isInCommandChannel() && _commandChannel.getLeader() == temp)
					{
						_commandChannel.setLeader(getLeader());
					}

					final MatchingRoom room = getLeader().getMatchingRoom();
					if (room != null && room.getType() == MatchingRoom.PARTY_MATCHING)
					{
						room.setLeader(getLeader());
					}
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER);
			}
		}
	}

	private Player getPlayerByName(String name)
	{
		for (final Player member : getMembers())
		{
			if (member.getName(null).equalsIgnoreCase(name))
			{
				return member;
			}
		}
		return null;
	}

	public void distributeItem(Player player, ItemInstance item)
	{
		if (item.getId() == PcInventory.ADENA_ID)
		{
			distributeAdena(player, item.getCount(), player);
			ItemsParser.getInstance().destroyItem("Party", item, player, null);
			return;
		}
		
		if (BotFunctions.getInstance().isAutoDropEnable(player))
		{
			player.getParty().getLeader().addItem("Party", item, player.getParty().getLeader(), true);
			return;
		}

		final Player target = getActualLooter(player, item.getId(), false, player);
		target.addItem("Party", item, player, true);

		if (item.getCount() > 1)
		{
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_OBTAINED_S3_S2);
			msg.addString(target.getName(null));
			msg.addItemName(item);
			msg.addItemNumber(item.getCount());
			broadcastToPartyMembers(target, msg);
		}
		else
		{
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_OBTAINED_S2);
			msg.addString(target.getName(null));
			msg.addItemName(item);
			broadcastToPartyMembers(target, msg);
		}
	}

	public void distributeItem(Player player, int itemId, long itemCount, boolean spoil, Attackable target)
	{
		if (itemId == PcInventory.ADENA_ID)
		{
			distributeAdena(player, itemCount, target);
			return;
		}
		
		if (BotFunctions.getInstance().isAutoDropEnable(player) && !spoil)
		{
			player.getParty().getLeader().addItem("Party", itemId, itemCount, player.getParty().getLeader(), true);
			return;
		}
		
		if (BotFunctions.getInstance().isAutoSpoilEnable(player) && spoil)
		{
			player.getParty().getLeader().addItem("Party", itemId, itemCount, player.getParty().getLeader(), true);
			return;
		}

		final Player looter = getActualLooter(player, itemId, spoil, target);
		looter.addItem(spoil ? "Sweeper Party" : "Party", itemId, itemCount, target, true);
		
		if (itemCount > 1)
		{
			final SystemMessage msg = spoil ? SystemMessage.getSystemMessage(SystemMessageId.C1_SWEEPED_UP_S3_S2) : SystemMessage.getSystemMessage(SystemMessageId.C1_OBTAINED_S3_S2);
			msg.addString(looter.getName(null));
			msg.addItemName(itemId);
			msg.addItemNumber(itemCount);
			broadcastToPartyMembers(looter, msg);
		}
		else
		{
			final SystemMessage msg = spoil ? SystemMessage.getSystemMessage(SystemMessageId.C1_SWEEPED_UP_S2) : SystemMessage.getSystemMessage(SystemMessageId.C1_OBTAINED_S2);
			msg.addString(looter.getName(null));
			msg.addItemName(itemId);
			broadcastToPartyMembers(looter, msg);
		}
	}

	public void distributeAdena(Player player, long adena, Creature target)
	{
		if (BotFunctions.getInstance().isAutoDropEnable(player))
		{
			player.getParty().getLeader().addAdena("Party", adena, player.getParty().getLeader(), true);
			return;
		}
		
		final Map<Player, AtomicLong> toReward = new HashMap<>(9);
		
		for (final Player member : getMembers())
		{
			if (Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true))
			{
				toReward.put(member, new AtomicLong());
			}
		}
		
		if (!toReward.isEmpty())
		{
			long leftOver = adena % toReward.size();
			final long count = adena / toReward.size();
			
			if (count > 0)
			{
				for (final AtomicLong member : toReward.values())
				{
					member.addAndGet(count);
				}
			}
			
			if (leftOver > 0)
			{
				final List<Player> keys = new ArrayList<>(toReward.keySet());
				
				while (leftOver-- > 0)
				{
					Collections.shuffle(keys);
					toReward.get(keys.get(0)).incrementAndGet();
				}
			}
			
			for (final Entry<Player, AtomicLong> member : toReward.entrySet())
			{
				if (member.getValue().get() > 0)
				{
					if (member.getKey().getInventory().getAdenaInstance() != null)
					{
						member.getKey().addAdena("Party", member.getValue().get(), player, true);
					}
					else
					{
						member.getKey().addItem("Party", 57, member.getValue().get(), player, true);
					}
				}
			}
		}
	}

	public void distributeXpAndSp(long xpReward_pr, int spReward_pr, long xpReward, int spReward, List<Player> rewardedMembers, int topLvl, int partyDmg, Attackable target)
	{
		final List<Player> validMembers = getValidMembers(rewardedMembers, topLvl);

		xpReward *= getExpBonus(validMembers.size());
		spReward *= getSpBonus(validMembers.size());
		xpReward_pr *= getExpBonus(validMembers.size());
		spReward_pr *= getSpBonus(validMembers.size());
		
		int sqLevelSum = 0;
		for (final Player member : validMembers)
		{
			sqLevelSum += (member.getLevel() * member.getLevel());
		}

		final double vitalityPoints = (target.getVitalityPoints(partyDmg) * Config.RATE_PARTY_XP) / validMembers.size();
		final boolean useVitalityRate = target.useVitalityRate();

		for (final Player member : rewardedMembers)
		{
			if (member.isDead())
			{
				continue;
			}

			long addexp;
			int addsp;

			if (validMembers.contains(member))
			{
				final float penalty = member.hasServitor() ? ((ServitorInstance) member.getSummon()).getExpPenalty() : 0;

				final double sqLevel = member.getLevel() * member.getLevel();
				final double preCalculation = (sqLevel / sqLevelSum) * (1 - penalty);

				if (member.hasPremiumBonus())
				{
					addexp = Math.round(member.calcStat(Stats.EXPSP_RATE, xpReward_pr * preCalculation, null, null));
					addsp = (int) member.calcStat(Stats.EXPSP_RATE, spReward_pr * preCalculation, null, null);
				}
				else
				{
					addexp = Math.round(member.calcStat(Stats.EXPSP_RATE, xpReward * preCalculation, null, null));
					addsp = (int) member.calcStat(Stats.EXPSP_RATE, spReward * preCalculation, null, null);
				}
				
				addexp = calculateExpSpPartyCutoff(member.getActingPlayer(), topLvl, addexp, addsp, useVitalityRate);

				if (addexp > 0)
				{
					member.updateVitalityPoints(vitalityPoints, true, false);
				}
			}
			else
			{
				member.addExpAndSp(0, 0);
			}
		}
	}

	private final long calculateExpSpPartyCutoff(Player player, int topLvl, long addExp, int addSp, boolean vit)
	{
		double xp = addExp;
		double sp = addSp;
		
		if (player.getPremiumBonus().isPersonal())
		{
			xp = (xp * player.getPremiumBonus().getRateXp());
			sp = (sp * player.getPremiumBonus().getRateSp());
		}
		
		xp *= player.getRExp();
		sp *= player.getRSp();
		
		if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("highfive"))
		{
			int i = 0;
			final int lvlDiff = topLvl - player.getLevel();
			for (final int[] gap : Config.PARTY_XP_CUTOFF_GAPS)
			{
				if ((lvlDiff >= gap[0]) && (lvlDiff <= gap[1]))
				{
					xp = (xp * Config.PARTY_XP_CUTOFF_GAP_PERCENTS[i]) / 100;
					sp = (sp * Config.PARTY_XP_CUTOFF_GAP_PERCENTS[i]) / 100;
					
					xp = xp > Long.MAX_VALUE ? Long.MAX_VALUE : xp;
					sp = sp > Integer.MAX_VALUE ? Integer.MAX_VALUE : sp;
					if (xp < 0)
					{
						xp = 0;
					}
					if (sp < 0)
					{
						sp = 0;
					}
					player.addExpAndSp((long) xp, (int) sp, vit);
					break;
				}
				i++;
			}
		}
		else
		{
			xp = xp > Long.MAX_VALUE ? Long.MAX_VALUE : xp;
			sp = sp > Integer.MAX_VALUE ? Integer.MAX_VALUE : sp;
			if (xp < 0)
			{
				xp = 0;
			}
			if (sp < 0)
			{
				sp = 0;
			}
			player.addExpAndSp((long) xp, (int) sp, vit);
		}
		return (long) xp;
	}

	public void recalculatePartyLevel()
	{
		int newLevel = 0;
		for (final Player member : getMembers())
		{
			if (member == null)
			{
				getMembers().remove(member);
				continue;
			}

			if (member.getLevel() > newLevel)
			{
				newLevel = member.getLevel();
			}
		}
		_partyLvl = newLevel;
	}

	private List<Player> getValidMembers(List<Player> members, int topLvl)
	{
		final List<Player> validMembers = new ArrayList<>();

		if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("level"))
		{
			for (final Player member : members)
			{
				if ((topLvl - member.getLevel()) <= Config.PARTY_XP_CUTOFF_LEVEL)
				{
					validMembers.add(member);
				}
			}
		}
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("percentage"))
		{
			int sqLevelSum = 0;
			for (final Player member : members)
			{
				sqLevelSum += (member.getLevel() * member.getLevel());
			}

			for (final Player member : members)
			{
				final int sqLevel = member.getLevel() * member.getLevel();
				if ((sqLevel * 100) >= (sqLevelSum * Config.PARTY_XP_CUTOFF_PERCENT))
				{
					validMembers.add(member);
				}
			}
		}
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("auto"))
		{
			int sqLevelSum = 0;
			for (final Player member : members)
			{
				sqLevelSum += (member.getLevel() * member.getLevel());
			}

			int i = members.size() - 1;
			if (i < 1)
			{
				return members;
			}
			if (i >= BONUS_EXP_SP.length)
			{
				i = BONUS_EXP_SP.length - 1;
			}

			for (final Player member : members)
			{
				final int sqLevel = member.getLevel() * member.getLevel();
				if (sqLevel >= (sqLevelSum / (members.size() * members.size())))
				{
					validMembers.add(member);
				}
			}
		}
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("highfive"))
		{
			validMembers.addAll(members);
		}
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("none"))
		{
			validMembers.addAll(members);
		}
		return validMembers;
	}

	private double getBaseExpSpBonus(int membersCount)
	{
		int i = membersCount - 1;
		if (i < 1)
		{
			return 1;
		}
		if (i >= BONUS_EXP_SP.length)
		{
			i = BONUS_EXP_SP.length - 1;
		}

		return BONUS_EXP_SP[i];
	}

	private double getExpBonus(int membersCount)
	{
		return (membersCount < 2) ? (getBaseExpSpBonus(membersCount)) : (getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_XP);
	}

	private double getSpBonus(int membersCount)
	{
		return (membersCount < 2) ? (getBaseExpSpBonus(membersCount)) : (getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_SP);
	}
	
	public int getLevel()
	{
		return _partyLvl;
	}

	public int getLootDistribution()
	{
		return _itemDistribution;
	}

	public boolean isInCommandChannel()
	{
		return _commandChannel != null;
	}

	public CommandChannel getCommandChannel()
	{
		return _commandChannel;
	}

	public void setCommandChannel(CommandChannel channel)
	{
		_commandChannel = channel;
	}

	public boolean isInDimensionalRift()
	{
		return _dr != null;
	}

	public void setDimensionalRift(DimensionalRift dr)
	{
		_dr = dr;
	}

	public DimensionalRift getDimensionalRift()
	{
		return _dr;
	}

	public Player getLeader()
	{
		return (_members != null && !_members.isEmpty()) ? _members.get(0) : null;
	}

	public void requestLootChange(byte type)
	{
		if (_requestChangeLoot != -1)
		{
			if (System.currentTimeMillis() > _requestChangeLootTimer)
			{
				finishLootRequest(false);
			}
			else
			{
				return;
			}
		}
		_requestChangeLoot = type;
		final int additionalTime = Player.REQUEST_TIMEOUT * 3000;
		_requestChangeLootTimer = System.currentTimeMillis() + additionalTime;
		_changeLootAnswers = new ArrayList<>();
		_checkTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new ChangeLootCheck(), additionalTime + 1000, 5000);
		broadcastToPartyMembers(getLeader(), new ExAskModifyPartyLooting(getLeader().getName(null), type));
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.REQUESTING_APPROVAL_CHANGE_PARTY_LOOT_S1);
		sm.addSystemString(LOOT_SYSSTRINGS[type]);
		getLeader().sendPacket(sm);
	}

	public synchronized void answerLootChangeRequest(Player member, boolean answer)
	{
		if (_requestChangeLoot == -1)
		{
			return;
		}
		if (_changeLootAnswers.contains(member.getObjectId()))
		{
			return;
		}
		if (!answer)
		{
			finishLootRequest(false);
			return;
		}
		_changeLootAnswers.add(member.getObjectId());
		if (_changeLootAnswers.size() >= (getMemberCount() - 1))
		{
			finishLootRequest(true);
		}
	}

	protected synchronized void finishLootRequest(boolean success)
	{
		if (_requestChangeLoot == -1)
		{
			return;
		}
		if (_checkTask != null)
		{
			_checkTask.cancel(false);
			_checkTask = null;
		}
		if (success)
		{
			broadCast(new ExSetPartyLooting(1, _requestChangeLoot));
			_itemDistribution = _requestChangeLoot;
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PARTY_LOOT_CHANGED_S1);
			sm.addSystemString(LOOT_SYSSTRINGS[_requestChangeLoot]);
			broadCast(sm);
		}
		else
		{
			broadCast(new ExSetPartyLooting(0, (byte) 0));
			broadCast(SystemMessage.getSystemMessage(SystemMessageId.PARTY_LOOT_CHANGE_CANCELLED));
		}
		_requestChangeLoot = -1;
		_changeLootAnswers.clear();
		_requestChangeLootTimer = 0;
	}

	protected class ChangeLootCheck implements Runnable
	{
		@Override
		public void run()
		{
			if (System.currentTimeMillis() > _requestChangeLootTimer)
			{
				finishLootRequest(false);
			}
		}
	}

	protected class PositionBroadcast implements Runnable
	{
		@Override
		public void run()
		{
			if (_positionPacket == null)
			{
				_positionPacket = new PartyMemberPosition(Party.this);
			}
			else
			{
				_positionPacket.reuse(Party.this);
			}
			broadCast(_positionPacket);
		}
	}

	public List<Player> getMembers()
	{
		return _members;
	}

	public Object getUCState()
	{
		return _ucState;
	}

	public void setUCState(Object uc)
	{
		_ucState = uc;
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

	@Override
	public void broadCast(GameServerPacket... msg)
	{
		if (_members != null && !_members.isEmpty())
		{
			for (final Player member : _members)
			{
				if (member != null)
				{
					member.sendPacket(msg);
				}
			}
		}
	}
	
	@Override
	public void broadCastMessage(ServerMessage msg)
	{
		if (_members != null && !_members.isEmpty())
		{
			for (final Player member : _members)
			{
				if (member != null)
				{
					member.sendMessage(msg.toString(member.getLang()));
				}
			}
		}
	}

	@Override
	public int getMemberCount()
	{
		return _members.size();
	}

	public boolean forEachMember(IL2Procedure<Player> procedure)
	{
		for (final Player player : getMembers())
		{
			if (!procedure.execute(player))
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean containsPlayer(Player player)
	{
		return getMembers().contains(player);
	}

	public int getLeaderObjectId()
	{
		return getLeader() != null ? getLeader().getObjectId() : 0;
	}
	
	public void broadcastString(String text)
	{
		broadCast(SystemMessage.sendString(text));
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
	
	public Map<Integer, PartyTemplate> getRanking()
	{
		return _ranking;
	}
	
	public PartyTemplate getMemberRank(Player player)
	{
		return _ranking.get(player.getObjectId());
	}
	
	public double getRateXp()
	{
		return _rateXp;
	}
	
	public double getRateSp()
	{
		return _rateSp;
	}
	
	public double getQuestRewardRate()
	{
		return _questRewardRate;
	}
	
	public double getQuestDropRate()
	{
		return _questDropRate;
	}
	
	public double getDropAdena()
	{
		return _dropAdena;
	}
	
	public double getDropItems()
	{
		return _dropItems;
	}
	
	public double getDropSpoil()
	{
		return _dropSpoil;
	}
	
	public double getDropSiege()
	{
		return _dropSiege;
	}
	
	public double getDropElementStones()
	{
		return _dropElementStones;
	}
	
	public double getFishingRate()
	{
		return _rateFishing;
	}
	
	public double getDropRaids()
	{
		return _dropRaids;
	}
	
	public double getDropEpics()
	{
		return _dropEpics;
	}
	
	public double getDropSealStones()
	{
		return _dropSealStones;
	}
	
	private void checkUCStatus()
	{
		if (getUCState() != null)
		{
			if (getUCState() instanceof UCWaiting)
			{
				final UCWaiting waiting = (UCWaiting) getUCState();
				waiting.setParty(false);
				waiting.clean();
			}
			else if (getUCState() instanceof UCTeam)
			{
				final UCTeam team = (UCTeam) getUCState();
				final UCTeam otherTeam = team.getOtherTeam();
				
				team.setStatus(UCTeam.FAIL);
				otherTeam.setStatus(UCTeam.WIN);
				team.getBaseArena().runTaskNow();
			}
		}
	}
	
	public void addPartyEffect(Skill sk, Player player)
	{
		if (!_partyEffects.containsKey(player.getObjectId()))
		{
			_partyEffects.put(player.getObjectId(), new ConcurrentHashMap<>());
		}
		_partyEffects.get(player.getObjectId()).put(sk, (System.currentTimeMillis() + (sk.getAbnormalTime() * 1000L)));
	}
	
	private void checkPatryEffecs(Player player)
	{
		final var now = System.currentTimeMillis();
		if (_partyEffects.containsKey(player.getObjectId()))
		{
			for (final var entry : _partyEffects.get(player.getObjectId()).entrySet())
			{
				final var sk = entry.getKey();
				final var time = entry.getValue();
				
				if (time < now)
				{
					_partyEffects.get(player.getObjectId()).remove(sk);
					continue;
				}
				_partyEffects.get(player.getObjectId()).remove(sk);
				getMembers().stream().filter(p -> p != null).forEach(pl -> pl.stopSkillEffects(sk.getId()));
			}
		}
		else
		{
			for (final var id : _partyEffects.keySet())
			{
				for (final var entry : _partyEffects.get(id).entrySet())
				{
					final var sk = entry.getKey();
					final var time = entry.getValue();
					
					if (time < now)
					{
						_partyEffects.get(id).remove(sk);
						continue;
					}
					player.stopSkillEffects(sk.getId());
				}
			}
		}
	}
}