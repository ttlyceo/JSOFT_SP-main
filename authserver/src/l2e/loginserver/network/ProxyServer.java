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
package l2e.loginserver.network;

import java.net.InetAddress;

import l2e.loginserver.GameServerManager;
import l2e.loginserver.network.communication.GameServer;

public class ProxyServer
{
	private final int _origServerId;
	private final int _proxyServerId;
	private InetAddress _proxyAddr;
	private int _proxyPort;
	private final int _minAccessLevel;
	private final int _maxAccessLevel;
	private final boolean _hideMain;
	
	public ProxyServer(int origServerId, int proxyServerId, int minAccessLevel, int maxAccessLevel, boolean hideMain)
	{
		_origServerId = origServerId;
		_proxyServerId = proxyServerId;
		_minAccessLevel = minAccessLevel;
		_maxAccessLevel = maxAccessLevel;
		_hideMain = hideMain;
	}
	
	public int getOrigServerId()
	{
		return _origServerId;
	}
	
	public int getProxyServerId()
	{
		return _proxyServerId;
	}
	
	public InetAddress getProxyAddr()
	{
		return _proxyAddr;
	}
	
	public void setProxyAddr(InetAddress proxyAddr)
	{
		_proxyAddr = proxyAddr;
	}
	
	public int getProxyPort()
	{
		return _proxyPort;
	}
	
	public void setProxyPort(int proxyPort)
	{
		_proxyPort = proxyPort;
	}
	
	public GameServer getGameServer()
	{
		return GameServerManager.getInstance().getGameServerById(getOrigServerId());
	}
	
	public int getMinAccessLevel()
	{
		return _minAccessLevel;
	}
	
	public int getMaxAccessLevel()
	{
		return _maxAccessLevel;
	}
	
	public boolean isHideMain()
	{
		return _hideMain;
	}
}