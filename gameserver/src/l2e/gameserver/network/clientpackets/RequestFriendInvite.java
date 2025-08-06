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

import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.FriendAddRequest;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestFriendInvite extends GameClientPacket
{
	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		activeChar.isntAfk();

		final Player friend = GameObjectsStorage.getPlayer(_name);

		SystemMessage sm;

		if ((friend == null) || !friend.isOnline() || friend.isInvisible())
		{
			activeChar.sendPacket(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
			return;
		}
		else if (friend == activeChar)
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST);
			return;
		}
		else if (activeChar.getBlockList().isBlocked(friend))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.BLOCKED_C1);
			sm.addCharName(friend);
			activeChar.sendPacket(sm);
			return;
		}
		else if (friend.getBlockList().isBlocked(activeChar))
		{
			activeChar.sendMessage("You are in target's block list.");
			return;
		}
		else if (activeChar.isInOlympiadMode() || friend.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
			return;
		}

		if (friend.getFriendInviteRefusal())
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER).addString(friend.getName(null)));
			return;
		}

		if (activeChar.getFriendList().contains(friend.getObjectId()))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			return;
		}

		if (!friend.isProcessingRequest())
		{
			activeChar.onTransactionRequest(friend);
			sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_REQUESTED_C1_TO_BE_FRIEND);
			sm.addString(_name);
			final FriendAddRequest ajf = new FriendAddRequest(activeChar.getName(null));
			friend.sendPacket(ajf);
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
			sm.addString(_name);
		}
		activeChar.sendPacket(sm);
	}
}