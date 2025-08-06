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

import l2e.commons.log.Log;
import l2e.gameserver.Config;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.L2FriendSay;

public final class RequestSendL2FriendSay extends GameClientPacket
{
	private String _message;
	private String _reciever;

	@Override
	protected void readImpl()
	{
		_message = readS();
		_reciever = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if ((_message == null) || _message.isEmpty() || (_message.length() > 300))
		{
			return;
		}
		
		final Player targetPlayer = GameObjectsStorage.getPlayer(_reciever);
		if ((targetPlayer == null) || !targetPlayer.getFriendList().contains(activeChar.getObjectId()))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			return;
		}
		
		if (Config.LOG_CHAT)
		{
			Log.AddLogChat(Say2.L2FRIEND, activeChar.getName(null), _reciever, _message);
		}
		targetPlayer.sendPacket(new L2FriendSay(activeChar.getName(null), _reciever, _message));
		targetPlayer.getListeners().onChatMessageReceive(Say2.L2FRIEND, targetPlayer.getName(null), activeChar.getName(null), _message);
	}
}