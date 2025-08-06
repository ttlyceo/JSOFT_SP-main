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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExDuelAskStart;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestDuelStart extends GameClientPacket
{
	private String _player;
	private int _partyDuel;

	@Override
	protected void readImpl()
	{
		_player = readS();
		_partyDuel = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		final Player targetChar = GameObjectsStorage.getPlayer(_player);
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.isActionsDisabled())
		{
			activeChar.sendActionFailed();
			return;
		}

		if (targetChar == null)
		{
			activeChar.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}
		
		if (activeChar == targetChar)
		{
			activeChar.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}
		
		if (targetChar.isInFightEvent())
		{
			return;
		}
		
		if (!activeChar.canDuel())
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
			return;
		}
		else if (!targetChar.canDuel())
		{
			activeChar.sendPacket(targetChar.getNoDuelReason());
			return;
		}
		else if (!activeChar.isInsideRadius(targetChar, 250, false, false))
		{
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_RECEIVE_A_DUEL_CHALLENGE_BECAUSE_C1_IS_TOO_FAR_AWAY);
			msg.addString(targetChar.getName(null));
			activeChar.sendPacket(msg);
			return;
		}
		
		if (_partyDuel == 1)
		{
			if (!activeChar.isInParty() || !(activeChar.isInParty() && activeChar.getParty().isLeader(activeChar)))
			{
				activeChar.sendMessage("You have to be the leader of a party in order to request a party duel.");
				return;
			}
			else if (!targetChar.isInParty())
			{
				activeChar.sendPacket(SystemMessageId.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY);
				return;
			}
			else if (activeChar.getParty().containsPlayer(targetChar))
			{
				activeChar.sendMessage("This player is a member of your own party.");
				return;
			}
			
			for (final Player temp : activeChar.getParty().getMembers())
			{
				if (!temp.canDuel())
				{
					activeChar.sendMessage("Not all the members of your party are ready for a duel.");
					return;
				}
			}
			Player partyLeader = null;
			for (final Player temp : targetChar.getParty().getMembers())
			{
				if (partyLeader == null)
				{
					partyLeader = temp;
				}
				if (!temp.canDuel())
				{
					activeChar.sendPacket(SystemMessageId.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
					return;
				}
			}
			
			if (partyLeader != null)
			{
				if (!partyLeader.isProcessingRequest())
				{
					activeChar.onTransactionRequest(partyLeader);
					partyLeader.sendPacket(new ExDuelAskStart(activeChar.getName(null), _partyDuel));
					
					if (Config.DEBUG)
					{
						_log.info(activeChar.getName(null) + " requested a duel with " + partyLeader.getName(null));
					}
					
					SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL);
					msg.addString(partyLeader.getName(null));
					activeChar.sendPacket(msg);
					
					msg = SystemMessage.getSystemMessage(SystemMessageId.C1_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL);
					msg.addString(activeChar.getName(null));
					targetChar.sendPacket(msg);
				}
				else
				{
					final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
					msg.addString(partyLeader.getName(null));
					activeChar.sendPacket(msg);
				}
			}
		}
		else
		{
			if (!targetChar.isProcessingRequest())
			{
				activeChar.onTransactionRequest(targetChar);
				targetChar.sendPacket(new ExDuelAskStart(activeChar.getName(null), _partyDuel));
				
				if (Config.DEBUG)
				{
					_log.info(activeChar.getName(null) + " requested a duel with " + targetChar.getName(null));
				}
				
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_BEEN_CHALLENGED_TO_A_DUEL);
				msg.addString(targetChar.getName(null));
				activeChar.sendPacket(msg);
				
				msg = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_CHALLENGED_YOU_TO_A_DUEL);
				msg.addString(activeChar.getName(null));
				targetChar.sendPacket(msg);
			}
			else
			{
				final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
				msg.addString(targetChar.getName(null));
				activeChar.sendPacket(msg);
			}
		}
	}
}