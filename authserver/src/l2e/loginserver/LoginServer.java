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
package l2e.loginserver;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.HostInfo;
import org.nio.impl.HaProxySelectorThread;
import org.nio.impl.SelectorConfig;
import org.nio.impl.SelectorThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.net.IPSettings;
import l2e.loginserver.database.DatabaseFactory;
import l2e.loginserver.network.LoginClient;
import l2e.loginserver.network.LoginPacketHandler;
import l2e.loginserver.network.SelectorHelper;
import l2e.loginserver.network.communication.GameServerCommunication;

public class LoginServer
{
	private static final Logger _log = LoggerFactory.getLogger(LoginServer.class);
	
	private final GameServerCommunication _gameServerListener;
	private final SelectorThread<LoginClient> _selectorThread;
	private Thread _restartLoginServer;

	public LoginServer() throws Exception
	{
		final File logFolder = new File(Config.DATAPACK_ROOT, "log");
		logFolder.mkdir();
		
		Config.load();
		final HostInfo host = IPSettings.getInstance().getAuthServerHost();
		if (host == null)
		{
			throw new Exception("Login host is empty!");
		}
		checkFreePorts(host);
		
		Class.forName(Config.DATABASE_DRIVER).getDeclaredConstructor().newInstance();
		DatabaseFactory.getInstance().getConnection().close();
		
		Config.initCrypt();
		GameServerManager.getInstance();
		if (Config.LOGIN_SERVER_SCHEDULE_RESTART)
		{
			_log.info("Scheduled Login Server Restart at: " + Config.LOGIN_SERVER_SCHEDULE_RESTART_TIME + " hour(s).");
			_restartLoginServer = new LoginServerRestart();
			_restartLoginServer.setDaemon(true);
			_restartLoginServer.start();
		}
		final LoginPacketHandler lph = new LoginPacketHandler();
		final SelectorHelper sh = new SelectorHelper();
		final SelectorConfig sc = new SelectorConfig();
		
		if (host.isAllowHaProxy())
		{
			_selectorThread = new HaProxySelectorThread<>(sc, lph, sh, sh, sh);
		}
		else
		{
			_selectorThread = new SelectorThread<>(sc, lph, sh, sh, sh);
		}
		_gameServerListener = GameServerCommunication.getInstance();
		_gameServerListener.openServerSocket(host.getGameAddress().equals("*") ? null : InetAddress.getByName(host.getGameAddress()), host.getGamePort());
		_gameServerListener.start();
		_log.info("Listening for Gameservers on " + host.getGameAddress() + ":" + host.getGamePort());
		_selectorThread.openServerSocket(host.getAddress().equals("*") ? null : InetAddress.getByName(host.getAddress()), host.getPort());
		_selectorThread.start();
		if (!host.isAllowHaProxy())
		{
			_log.info("Listening for clients on " + host.getAddress() + ":" + host.getPort());
		}
		else
		{
			_log.info("Listening for HAProxy clients on " + host.getAddress() + ":" + host.getPort());
		}
	}

	public GameServerCommunication getGameServerListener()
	{
		return _gameServerListener;
	}
	
	private static void checkFreePorts(HostInfo host)
	{
		boolean binded = false;
		while (!binded)
		{
			try
			{
				ServerSocket ss;
				if (host.getAddress().equalsIgnoreCase("*"))
				{
					ss = new ServerSocket(host.getPort());
				}
				else
				{
					ss = new ServerSocket(host.getPort(), 50, InetAddress.getByName(host.getAddress()));
				}
				ss.close();
				binded = true;
			}
			catch (final Exception e)
			{
				_log.warn("Port " + host.getPort() + " is already binded. Please free it and restart server!");
				binded = false;
				try
				{
					Thread.sleep(1000);
				}
				catch (final InterruptedException e2)
				{}
			}
		}
	}
	
	public void shutdown(boolean restart)
	{
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}
	
	class LoginServerRestart extends Thread
	{
		public LoginServerRestart()
		{
			setName("LoginServerRestart");
		}
		
		@Override
		public void run()
		{
			while (!isInterrupted())
			{
				try
				{
					Thread.sleep(Config.LOGIN_SERVER_SCHEDULE_RESTART_TIME * 3600000);
				}
				catch (final InterruptedException e)
				{
					return;
				}
				shutdown(true);
			}
		}
	}
	
	private static LoginServer _loginServer;
	
	public static LoginServer getInstance()
	{
		return _loginServer;
	}

	public static void main(String[] args) throws Exception
	{
		_loginServer = new LoginServer();
	}
}