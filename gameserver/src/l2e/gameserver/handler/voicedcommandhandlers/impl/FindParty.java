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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.listener.player.OnQuestionMarkListener;
import l2e.gameserver.listener.player.PlayerListenerList;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.ExManagePartyRoomMember;
import l2e.gameserver.network.serverpackets.JoinParty;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Rework by LordWinter
 */
public class FindParty implements IVoicedCommandHandler
{
	private static final String[] COMMANDS =
	{
	        "findparty", "fp", "party", "invite", "partylist"
	};

	private static final OnPartyQuestionMarkClicked _listener = new OnPartyQuestionMarkClicked();
	private static final Map<Integer, FindPartyRequest> _requests = new ConcurrentHashMap<>();

	static
	{
		PlayerListenerList.addGlobal(_listener);
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				synchronized (_requests)
				{
					for (final Entry<Integer, FindPartyRequest> entry : _requests.entrySet())
					{
						if ((entry.getValue().getRequestStartTime() + (Config.FIND_PARTY_REFRESH_TIME * 1000)) < System.currentTimeMillis())
						{
							_requests.remove(entry.getKey());
						}
					}
				}
			}
		}, 60000, 60000);
	}

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!Config.ALLOW_FIND_PARTY)
		{
			return false;
		}
		
		if (command.startsWith("partylist"))
		{
			int i = 0;
			final CreatureSay[] packets = new CreatureSay[_requests.size() + 2];
			packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "FindParty.PARTY_REQUEST"), ServerStorage.getInstance().getString(activeChar.getLang(), "FindParty.PARTY_LIST"));
			for (final FindPartyRequest request : _requests.values())
			{
				if (target != null && !target.isEmpty() && !request.getMessage().toLowerCase().contains(target.toLowerCase()))
				{
					continue;
				}

				final Player partyLeader = GameObjectsStorage.getPlayer(request.getObjectId());
				if (partyLeader == null)
				{
					continue;
				}

				int freeSlots = Config.PARTY_LIMIT - 1;
				if (partyLeader.getParty() != null)
				{
					freeSlots = Config.PARTY_LIMIT - partyLeader.getParty().getMembers().size();
				}
				if (freeSlots <= 0)
				{
					continue;
				}

				packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.PARTY, ServerStorage.getInstance().getString(activeChar.getLang(), "FindParty.FIND_PARTY"), "\b\tType=1 \tID=" + partyLeader.getObjectId() + " \tColor=0 \tUnderline=0 \tTitle=\u001B\u001B\b" + partyLeader.getName(null) + " (" + freeSlots + "/" + Config.PARTY_LIMIT + ")" + " " + ServerStorage.getInstance().getString(activeChar.getLang(), "FindParty.FREE_SLOTS") + ". " + request.getMessage());
			}
			packets[i++] = new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "FindParty.PARTY_REQUEST"), ServerStorage.getInstance().getString(activeChar.getLang(), "FindParty.PARTY_LIST_END"));
			activeChar.sendPacket(packets);
			return true;
		}
		else if (command.startsWith("invite"))
		{
			Player playerToInvite = null;
			if (activeChar.isInParty() && !activeChar.getParty().isLeader(activeChar) && activeChar.getParty().getMemberCount() >= Config.PARTY_LIMIT)
			{
				playerToInvite = GameObjectsStorage.getPlayer(target);
			}

			if (playerToInvite != null)
			{
				for (final Player ptMem : activeChar.getParty())
				{
					if (activeChar.getParty().getLeader() == ptMem)
					{
						ptMem.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.PARTY, ServerStorage.getInstance().getString(ptMem.getLang(), "FindParty.PARTY_REQUEST"), "" + ServerStorage.getInstance().getString(ptMem.getLang(), "FindParty.PL_INVITE") + " " + playerToInvite.getName(null) + " " + ServerStorage.getInstance().getString(ptMem.getLang(), "FindParty.TO_PARTY") + ". \b\tType=1 \tID=" + playerToInvite.getObjectId() + " \tColor=0 \tUnderline=0 \tTitle=\u001B\u001B\b"));
					}
					else
					{
						ptMem.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.PARTY, ServerStorage.getInstance().getString(ptMem.getLang(), "FindParty.PARTY_REQUEST"), "" + ServerStorage.getInstance().getString(ptMem.getLang(), "FindParty.PL_INVITE") + " " + playerToInvite.getName(null) + " " + ServerStorage.getInstance().getString(ptMem.getLang(), "FindParty.TO_PARTY") + "."));
					}
				}
			}
		}
		else if (command.startsWith("party") || command.startsWith("fp") || command.startsWith("findparty"))
		{
			if (activeChar.isChatBanned())
			{
				return false;
			}
			
			if (activeChar.isInParty() && !activeChar.getParty().isLeader(activeChar))
			{
				activeChar.sendMessage((new ServerMessage("FindParty.ONLY_LEADER", activeChar.getLang())).toString());
				return true;
			}
			
			if (activeChar.getLevel() < Config.FIND_PARTY_MIN_LEVEL)
			{
				final ServerMessage msg = new ServerMessage("FindParty.MIN_LEVEL", activeChar.getLang());
				msg.add(Config.FIND_PARTY_MIN_LEVEL);
				activeChar.sendMessage(msg.toString());
				return true;
			}
			
			final String[] params = command.split(" ");
			String topic = "";
			if (params.length > 1)
			{
				int i = 0;
				for (final var msg : params)
				{
					if (i == 0)
					{
						i++;
						continue;
					}
					topic += msg;
					topic += " ";
					i++;
				}
			}
			
			int partyRequestObjId = 0;
			for (final Entry<Integer, FindPartyRequest> entry : _requests.entrySet())
			{
				if (entry.getValue().getObjectId() == activeChar.getObjectId())
				{
					partyRequestObjId = entry.getKey();
					break;
				}
			}
			if (partyRequestObjId == 0)
			{
				partyRequestObjId = IdFactory.getInstance().getNextId();
			}

			int freeSlots = Config.PARTY_LIMIT - 1;
			if (activeChar.getParty() != null)
			{
				freeSlots = Config.PARTY_LIMIT - activeChar.getParty().getMembers().size();
			}
			if (freeSlots <= 0)
			{
				activeChar.sendMessage((new ServerMessage("FindParty.PARTY_FULL", activeChar.getLang())).toString());
				return true;
			}

			if (target != null && !target.isEmpty())
			{
				target = String.valueOf(target.charAt(0)).toUpperCase() + target.substring(1);
			}

			FindPartyRequest request = _requests.get(partyRequestObjId);
			if (request == null)
			{
				request = new FindPartyRequest(activeChar, target);
			}
			else
			{
				final long delay = System.currentTimeMillis() - request.getRequestStartTime();
				if (delay < (Config.FIND_PARTY_FLOOD_TIME * 1000))
				{
					final ServerMessage msg = new ServerMessage("FindParty.FLOOD_MSG", activeChar.getLang());
					msg.add(((Config.FIND_PARTY_FLOOD_TIME * 1000) / 1000));
					msg.add((((Config.FIND_PARTY_FLOOD_TIME * 1000) - delay) / 1000));
					activeChar.sendMessage(msg.toString());
					return true;
				}

				if (target == null || target.isEmpty())
				{
					request.update();
				}
				else
				{
					request.update(target);
				}
			}
			_requests.put(partyRequestObjId, request);
			
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (player == null)
				{
					continue;
				}
				
				if (!player.canJoinParty(activeChar) && !(activeChar.isInParty() && activeChar.getParty().getMembers().contains(player)))
				{
					continue;
				}
				player.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.PARTY, !topic.isEmpty() ? topic : ServerStorage.getInstance().getString(player.getLang(), "FindParty.FIND_PARTY"), "\b\tType=1 \tID=" + partyRequestObjId + " \tColor=0 \tUnderline=0 \tTitle=\u001B\u001B\b" + activeChar.getName(null) + " (" + freeSlots + "/" + Config.PARTY_LIMIT + ")" + " " + ServerStorage.getInstance().getString(player.getLang(), "FindParty.FREE_SLOTS") + ". " + request.getMessage()));
			}
		}
		return false;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return COMMANDS;
	}

	private static class OnPartyQuestionMarkClicked implements OnQuestionMarkListener
	{
		@Override
		public void onQuestionMarkClicked(Player player, int targetObjId)
		{
			final int requestorObjId = _requests.containsKey(targetObjId) ? _requests.get(targetObjId).getObjectId() : 0;
			if (requestorObjId > 0)
			{
				if (player.getObjectId() != requestorObjId)
				{
					final Player partyLeader = GameObjectsStorage.getPlayer(requestorObjId);
					if (partyLeader == null)
					{
						player.sendMessage((new ServerMessage("FindParty.LEADER_OFF", player.getLang())).toString());
					}
					else
					{
						final long delay = System.currentTimeMillis() - player.getQuickVarL("partyrequestsent", 0);
						if (delay < (Config.FIND_PARTY_FLOOD_TIME * 1000))
						{
							final ServerMessage msg = new ServerMessage("FindParty.FLOOD_MSG", player.getLang());
							msg.add(((Config.FIND_PARTY_FLOOD_TIME * 1000) / 1000));
							msg.add((((Config.FIND_PARTY_FLOOD_TIME * 1000) - delay) / 1000));
							player.sendMessage(msg.toString());
							return;
						}
						player.addQuickVar("partyrequestsent", System.currentTimeMillis());
						
						final CreatureSay packetLeader = new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, player.getName(null), "" + ServerStorage.getInstance().getString(partyLeader.getLang(), "FindParty.LEVEL") + " " + player.getLevel() + ", " + ServerStorage.getInstance().getString(partyLeader.getLang(), "FindParty.CLASS") + " " + Util.className(partyLeader, player.getClassId().getId()) + ". " + ServerStorage.getInstance().getString(partyLeader.getLang(), "FindParty.INVITE") + " \b\tType=1 \tID=" + player.getObjectId() + " \tColor=0 \tUnderline=0 \tTitle=\u001B\u001B\b");
						partyLeader.sendPacket(packetLeader);
						
						final ServerMessage msg = new ServerMessage("FindParty.REQUEST_SENT", player.getLang());
						msg.add(partyLeader.getName(null));
						player.sendMessage(msg.toString());
					}
				}
			}
			else
			{
				final Player target = GameObjectsStorage.getPlayer(targetObjId);
				if (target != null)
				{
					requestParty(player, target);
				}
				else
				{
					player.sendMessage((new ServerMessage("FindParty.NOT_VALID", player.getLang())).toString());
				}
			}
		}

		private void requestParty(Player partyLeader, Player target)
		{
			if (partyLeader.isOutOfControl())
			{
				partyLeader.sendActionFailed();
				return;
			}

			if (partyLeader.isProcessingRequest())
			{
				partyLeader.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
				return;
			}

			if (target == null)
			{
				partyLeader.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
				return;
			}

			if (target == partyLeader || !target.isVisibleFor(partyLeader))
			{
				partyLeader.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				partyLeader.sendActionFailed();
				return;
			}

			if (target.isProcessingRequest())
			{
				partyLeader.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER).addPcName(target));
				return;
			}
			
			if (partyLeader.isPartyBanned())
			{
				partyLeader.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_SO_PARTY_NOT_ALLOWED);
				partyLeader.sendActionFailed();
				return;
			}
			
			if (target.isPartyBanned())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_REPORTED_AND_CANNOT_PARTY);
				sm.addCharName(target);
				partyLeader.sendPacket(sm);
				return;
			}
			
			if (target.isInKrateisCube() || target.getUCState() > 0 || (AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(target.getObjectId()))
			{
				partyLeader.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return;
			}
			
			SystemMessage sm;
			if (target.isInParty())
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_IN_PARTY);
				sm.addString(target.getName(null));
				partyLeader.sendPacket(sm);
				return;
			}
			
			if (target.getBlockList().isBlocked(partyLeader))
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
				sm.addCharName(target);
				partyLeader.sendPacket(sm);
				return;
			}
			
			if (target.getPartyInviteRefusal())
			{
				partyLeader.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER).addString(target.getName(null)));
				return;
			}
			
			if (target.isCursedWeaponEquipped() || partyLeader.isCursedWeaponEquipped())
			{
				partyLeader.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return;
			}

			if (target.isInFightEvent() && !target.getFightEvent().canJoinParty(partyLeader, target))
			{
				partyLeader.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return;
			}

			if (target.checkInTournament())
			{
				partyLeader.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return;
			}

			if (target.isInPvpFunZone())
			{
				final FunPvpZone zone = ZoneManager.getInstance().getZone(target, FunPvpZone.class);
				if (zone != null && !zone.canJoinParty(partyLeader, target))
				{
					partyLeader.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return;
				}
			}

			if (target.isJailed() || partyLeader.isJailed())
			{
				partyLeader.sendMessage((new ServerMessage("FindParty.JAIL", partyLeader.getLang())).toString());
				return;
			}
			
			if (target.isInOlympiadMode() || partyLeader.isInOlympiadMode())
			{
				if ((target.isInOlympiadMode() != partyLeader.isInOlympiadMode()) || (target.getOlympiadGameId() != partyLeader.getOlympiadGameId()) || (target.getOlympiadSide() != partyLeader.getOlympiadSide()))
				{
					partyLeader.sendPacket(SystemMessageId.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
					return;
				}
			}

			Party party = partyLeader.getParty();
			if (party != null)
			{
				if (party.getMemberCount() >= Config.PARTY_LIMIT)
				{
					partyLeader.sendPacket(SystemMessageId.PARTY_FULL);
					return;
				}

				if (Config.PARTY_LEADER_ONLY_CAN_INVITE && !party.isLeader(partyLeader))
				{
					partyLeader.sendPacket(SystemMessageId.ONLY_LEADER_CAN_INVITE);
					return;
				}

				if (party.isInDimensionalRift())
				{
					partyLeader.sendMessage((new ServerMessage("FindParty.RIFT", partyLeader.getLang())).toString());
					partyLeader.sendActionFailed();
					return;
				}
			}
			else
			{
				party = new Party(partyLeader, 0);
				partyLeader.setParty(party);
			}
			partyLeader.sendPacket(new JoinParty(1));
			target.joinParty(party);
			
			final var requestorRoom = partyLeader.getMatchingRoom();
			if (requestorRoom != null)
			{
				requestorRoom.addMember(target);
				final var packet = new ExManagePartyRoomMember(target, requestorRoom, 1);
				for (final var member : requestorRoom.getPlayers())
				{
					if (member != null)
					{
						member.sendPacket(packet);
					}
				}
				target.broadcastCharInfo();
			}
		}
	}

	private static class FindPartyRequest
	{
		private final int _objectId;
		private long _requestStartTimeMilis;
		private String _message;

		public FindPartyRequest(Player player, String msg)
		{
			_objectId = player.getObjectId();
			_requestStartTimeMilis = System.currentTimeMillis();
			_message = msg == null ? "" : msg;
		}

		public void update()
		{
			_requestStartTimeMilis = System.currentTimeMillis();
		}

		public void update(String newMsg)
		{
			_requestStartTimeMilis = System.currentTimeMillis();
			_message = newMsg;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public long getRequestStartTime()
		{
			return _requestStartTimeMilis;
		}
		
		public String getMessage()
		{
			return _message;
		}
	}
}