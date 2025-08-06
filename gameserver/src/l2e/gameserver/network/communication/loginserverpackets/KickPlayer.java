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
package l2e.gameserver.network.communication.loginserverpackets;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.ReceivablePacket;
import l2e.gameserver.network.serverpackets.ServerClose;

public class KickPlayer extends ReceivablePacket
{
	private String _account;
  
	@Override
	public void readImpl()
	{
		_account = readS();
	}
  
	@Override
	protected void runImpl()
	{
		GameClient client = AuthServerCommunication.getInstance().removeWaitingClient(_account);
		if (client == null)
		{
			client = AuthServerCommunication.getInstance().removeAuthedClient(_account);
		}
		
		if (client == null)
		{
			return;
		}
		
		final Player activeChar = client.getActiveChar();
		if (activeChar != null)
		{
			activeChar.sendPacket(SystemMessageId.ANOTHER_LOGIN_WITH_ACCOUNT);
			activeChar.kick();
		}
		else
		{
			client.close(ServerClose.STATIC_PACKET);
		}
	}
}
