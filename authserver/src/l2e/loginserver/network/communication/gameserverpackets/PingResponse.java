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
package l2e.loginserver.network.communication.gameserverpackets;


import l2e.loginserver.network.communication.GameServer;
import l2e.loginserver.network.communication.ReceivablePacket;

public class PingResponse extends ReceivablePacket
{
	private long _serverTime;
	
	@Override
	protected void readImpl()
	{
		_serverTime = readQ();
	}
	
	@Override
	protected void runImpl()
	{
		final GameServer gameServer = getGameServer();
		if (!gameServer.isAuthed())
		{
			return;
		}
		
		gameServer.getConnection().onPingResponse();
		
		final long diff = System.currentTimeMillis() - _serverTime;
		
		if (Math.abs(diff) > 999)
		{
			_log.warn("Gameserver IP[" + gameServer.getConnection().getIpAddress() + "]: time offset " + diff + " ms.");
		}
	}
}
