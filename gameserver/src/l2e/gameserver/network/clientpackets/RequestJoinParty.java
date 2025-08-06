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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.Config;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AskJoinParty;
import l2e.gameserver.network.serverpackets.ExManagePartyRoomMember;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestJoinParty extends GameClientPacket
{
	private String _name;
	private int _itemDistribution;

	@Override
	protected void readImpl()
	{
		_name = readS();
		_itemDistribution = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final var requestor = getClient().getActiveChar();
		final var target = GameObjectsStorage.getPlayer(_name);
		
		if (requestor == null)
		{
			return;
		}
		
		if (target == null)
		{
			requestor.sendPacket(SystemMessageId.FIRST_SELECT_USER_TO_INVITE_TO_PARTY);
			return;
		}
		
		if (((target.getClient() == null) || target.getClient().isDetached()) && !target.isFakePlayer())
		{
			requestor.sendMessage("Player is in offline mode.");
			return;
		}
		
		if (requestor.isPartyBanned())
		{
			requestor.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_SO_PARTY_NOT_ALLOWED);
			requestor.sendActionFailed();
			return;
		}
		
		if (target.isPartyBanned())
		{
			final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_REPORTED_AND_CANNOT_PARTY);
			sm.addCharName(target);
			requestor.sendPacket(sm);
			return;
		}

		if ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(target.getObjectId()))
		{
			requestor.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}
		
		if (!target.isVisibleFor(requestor))
		{
			requestor.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}
		
		SystemMessage sm;
		if (target.isInParty() || target.isFakePlayer())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_IN_PARTY);
			sm.addString(target.getName(null));
			requestor.sendPacket(sm);
			return;
		}
		
		if (target.getBlockList().isBlocked(requestor))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
			sm.addCharName(target);
			requestor.sendPacket(sm);
			return;
		}
		
		if (target == requestor)
		{
			requestor.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}

		if (target.getPartyInviteRefusal())
		{
			requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER).addString(target.getName(null)));
			return;
		}
		
		if (target.isCursedWeaponEquipped() || requestor.isCursedWeaponEquipped())
		{
			requestor.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		if (target.isInFightEvent() && !target.getFightEvent().canJoinParty(requestor, target))
		{
			requestor.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		
		if (target.isJailed() || requestor.isJailed())
		{
			requestor.sendMessage("You cannot invite a player while is in Jail.");
			return;
		}
		
		if (target.isInOlympiadMode() || requestor.isInOlympiadMode())
		{
			if ((target.isInOlympiadMode() != requestor.isInOlympiadMode()) || (target.getOlympiadGameId() != requestor.getOlympiadGameId()) || (target.getOlympiadSide() != requestor.getOlympiadSide()))
			{
				requestor.sendPacket(SystemMessageId.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
				return;
			}
		}
		
		sm = SystemMessage.getSystemMessage(SystemMessageId.C1_INVITED_TO_PARTY);
		sm.addCharName(target);
		requestor.sendPacket(sm);
		
		if (!requestor.isInParty())
		{
			if (requestor.isSameAddress(target) && (requestor.isUseAutoParty() && target.isUseAutoParty()))
			{
				requestor.setParty(new Party(requestor, _itemDistribution));
				target.joinParty(requestor.getParty());
				final var requestorRoom = requestor.getMatchingRoom();
				
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
			else
			{
				createNewParty(target, requestor);
			}
		}
		else
		{
			final var party = requestor.getParty();
			if (party == null)
			{
				return;
			}
			
			if (party.isInDimensionalRift())
			{
				requestor.sendMessage("You cannot invite a player when you are in the Dimensional Rift.");
			}
			else
			{
				if (requestor.isSameAddress(target) && party.isLeader(requestor) && party.getMemberCount() < Config.PARTY_LIMIT && (requestor.isUseAutoParty() && target.isUseAutoParty()))
				{
					target.joinParty(party);
					final var requestorRoom = requestor.getMatchingRoom();
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
				else
				{
					addTargetToParty(target, requestor);
				}
			}
		}
	}
	
	private void addTargetToParty(Player target, Player requestor)
	{
		final var party = requestor.getParty();
		if (!party.isLeader(requestor))
		{
			requestor.sendPacket(SystemMessageId.ONLY_LEADER_CAN_INVITE);
			return;
		}
		if (party.getMemberCount() >= Config.PARTY_LIMIT)
		{
			requestor.sendPacket(SystemMessageId.PARTY_FULL);
			return;
		}
		if (party.getPendingInvitation() && !party.isInvitationRequestExpired())
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
		
		if (!target.isProcessingRequest())
		{
			requestor.onTransactionRequest(target);
			target.sendPacket(new AskJoinParty(requestor.getName(null), party.getLootDistribution()));
			party.setPendingInvitation(true);
			
			if (Config.DEBUG)
			{
				_log.info("sent out a party invitation to:" + target.getName(null));
			}
			
		}
		else
		{
			final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
			sm.addString(target.getName(null));
			requestor.sendPacket(sm);
			
			if (Config.DEBUG)
			{
				_log.warn(requestor.getName(null) + " already received a party invitation");
			}
		}
	}
	
	private void createNewParty(Player target, Player requestor)
	{
		if (!target.isProcessingRequest())
		{
			requestor.setParty(new Party(requestor, _itemDistribution));
			
			requestor.onTransactionRequest(target);
			target.sendPacket(new AskJoinParty(requestor.getName(null), _itemDistribution));
			requestor.getParty().setPendingInvitation(true);
			
			if (Config.DEBUG)
			{
				_log.info("sent out a party invitation to:" + target.getName(null));
			}
			
		}
		else
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			
			if (Config.DEBUG)
			{
				_log.warn(requestor.getName(null) + " already received a party invitation");
			}
		}
	}
}