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

import org.HostInfo;

import l2e.loginserver.GameServerManager;
import l2e.loginserver.network.communication.GameServer;
import l2e.loginserver.network.communication.ReceivablePacket;
import l2e.loginserver.network.communication.loginserverpackets.AuthResponse;
import l2e.loginserver.network.communication.loginserverpackets.LoginServerFail;

public class AuthRequest extends ReceivablePacket
{
	private int _protocolVersion;
	private HostInfo[] _hosts;
	private int _serverType;
	private int _ageLimit;
	private boolean _gmOnly;
	private boolean _brackets;
	private boolean _pvp;
	private int _maxOnline;
	
	@Override
	protected void readImpl()
	{
		try
		{
			_protocolVersion = readD();
			_serverType = readD();
			_ageLimit = readD();
			_gmOnly = readC() == 1;
			_brackets = readC() == 1;
			_pvp = readC() == 1;
			_maxOnline = readD();
			
			final int hostsCount = readC();
			_hosts = new HostInfo[hostsCount];
			for (int i = 0; i < hostsCount; i++)
			{
				final int id = readC();
				final int allowProxy = readD();
				final String address = readS();
				final int port = readH();
				final String key = readS();
				final int maskCount = readC();
				final HostInfo host = new HostInfo(id, address, port, key, allowProxy == 1);
				for (int m = 0; m < maskCount; m++)
				{
					final String subAddress = readS();
					final byte[] subnetAddress = new byte[readD()];
					readB(subnetAddress);
					final byte[] subnetMask = new byte[readD()];
					readB(subnetMask);
					host.addSubnet(subAddress, subnetAddress, subnetMask);
				}
				_hosts[i] = host;
			}
		}
		catch (final Exception e)
		{}
	}
	
	@Override
	protected void runImpl()
	{
		final GameServer gs = getGameServer();
		
		_log.info("Trying to register gameserver: IP[" + gs.getConnection().getIpAddress() + "]");
		
		for (final HostInfo host : _hosts)
		{
			final int registerResult = GameServerManager.getInstance().registerGameServer(host, gs);
			if (registerResult == GameServerManager.SUCCESS_GS_REGISTER)
			{
				gs.addHost(host);
			}
			else
			{
				if (registerResult == GameServerManager.FAIL_GS_REGISTER_DIFF_KEYS)
				{
					sendPacket(new LoginServerFail("Gameserver registration on ID[" + host.getId() + "] failed. Registered different keys!", false));
					sendPacket(new LoginServerFail("Set the same keys in authserver and gameserver, and restart them!", false));
				}
				else if (registerResult == GameServerManager.FAIL_GS_REGISTER_ID_ALREADY_USE)
				{
					sendPacket(new LoginServerFail("Gameserver registration on ID[" + host.getId() + "] failed. ID[" + host.getId() + "] is already in use!", false));
					sendPacket(new LoginServerFail("Free ID[" + host.getId() + "] or change to another ID, and restart your authserver or gameserver!", false));
				}
				else if (registerResult == GameServerManager.FAIL_GS_REGISTER_ERROR)
				{
					sendPacket(new LoginServerFail("Gameserver registration on ID[" + host.getId() + "] failed. You have some errors!", false));
					sendPacket(new LoginServerFail("To solve the problem, contact the developer!", false));
				}
			}
		}
		
		if (gs.getHosts().length > 0)
		{
			gs.setProtocol(_protocolVersion);
			gs.setServerType(_serverType);
			gs.setAgeLimit(_ageLimit);
			gs.setGmOnly(_gmOnly);
			gs.setShowingBrackets(_brackets);
			gs.setPvp(_pvp);
			gs.setMaxPlayers(_maxOnline);
			gs.store();
			gs.setAuthed(true);
			gs.getConnection().startPingTask();
		}
		else
		{
			sendPacket(new LoginServerFail("Gameserver registration failed. All ID's is already in use!", true));
			_log.info("Gameserver registration failed.");
			return;
		}
		_log.info("Gameserver registration successful.");
		sendPacket(new AuthResponse(gs));
	}
}