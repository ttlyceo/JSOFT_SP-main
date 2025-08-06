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
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.ReceivablePacket;

public class ChangePasswordResponse extends ReceivablePacket
{
	public String _account;
	public boolean _changed;
  
	@Override
	protected void readImpl()
	{
		_account = readS();
		_changed = (readD() == 1);
	}
  
	@Override
	protected void runImpl()
	{
		final GameClient client = AuthServerCommunication.getInstance().getAuthedClient(_account);
		if (client == null)
		{
			return;
		}
		
		final Player activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (_changed)
		{
			activeChar.sendMessage("Password changed!");
		}
		else
		{
			activeChar.sendMessage("Password not changed!");
		}
	}
}
