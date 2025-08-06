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
package l2e.loginserver.network.communication;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.loginserver.Config;
import l2e.loginserver.ThreadPoolManager;
import l2e.loginserver.network.communication.loginserverpackets.PingRequest;

public class GameServerConnection
{
	private static final Logger _log = LoggerFactory.getLogger(GameServerConnection.class);
	
	final ByteBuffer _readBuffer = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);
	final Queue<SendablePacket> _sendQueue = new ArrayDeque<>();
	final Lock _sendLock = new ReentrantLock();
	final AtomicBoolean _isPengingWrite = new AtomicBoolean();
	private final Selector _selector;
	private final SelectionKey _key;
	private GameServer _gameServer;
	private Future<?> _pingTask;
	private int _pingRetry;
	
	private class PingTask implements Runnable
	{
		@Override
		public void run()
		{
			if (Config.GAME_SERVER_PING_RETRY > 0)
			{
				if (_pingRetry > Config.GAME_SERVER_PING_RETRY)
				{
					_log.warn("Gameserver IP[" + getIpAddress() + "]: ping timeout!");
					closeNow();
					return;
				}
			}
			_pingRetry++;
			sendPacket(new PingRequest());
		}
	}
	
	public GameServerConnection(SelectionKey key)
	{
		_key = key;
		_selector = key.selector();
	}
	
	public void sendPacket(SendablePacket packet)
	{
		boolean wakeUp;
		
		_sendLock.lock();
		try
		{
			_sendQueue.add(packet);
			wakeUp = enableWriteInterest();
		}
		catch (final CancelledKeyException e)
		{
			return;
		}
		finally
		{
			_sendLock.unlock();
		}
		
		if (wakeUp)
		{
			_selector.wakeup();
		}
	}
	
	protected boolean disableWriteInterest() throws CancelledKeyException
	{
		if (_isPengingWrite.compareAndSet(true, false))
		{
			_key.interestOps(_key.interestOps() & ~SelectionKey.OP_WRITE);
			return true;
		}
		return false;
	}
	
	protected boolean enableWriteInterest() throws CancelledKeyException
	{
		if (_isPengingWrite.getAndSet(true) == false)
		{
			_key.interestOps(_key.interestOps() | SelectionKey.OP_WRITE);
			return true;
		}
		return false;
	}
	
	public void closeNow()
	{
		_key.interestOps(SelectionKey.OP_CONNECT);
		_selector.wakeup();
	}
	
	public void onDisconnection()
	{
		try
		{
			stopPingTask();
			_readBuffer.clear();
			_sendLock.lock();
			try
			{
				_sendQueue.clear();
			}
			finally
			{
				_sendLock.unlock();
			}
			
			_isPengingWrite.set(false);
			if (_gameServer != null && _gameServer.isAuthed())
			{
				_log.info("Connection with gameserver IP[" + getIpAddress() + "] lost.");
				_log.info("Setting gameserver down.");
				_gameServer.setDown();
			}
			_gameServer = null;
		}
		catch (final Exception e)
		{
			_log.warn("", e);
		}
	}
	
	ByteBuffer getReadBuffer()
	{
		return _readBuffer;
	}
	
	GameServer getGameServer()
	{
		return _gameServer;
	}
	
	void setGameServer(GameServer gameServer)
	{
		_gameServer = gameServer;
	}
	
	public String getIpAddress()
	{
		return ((SocketChannel) _key.channel()).socket().getInetAddress().getHostAddress();
	}
	
	public void onPingResponse()
	{
		_pingRetry = 0;
	}
	
	public void startPingTask()
	{
		if (Config.GAME_SERVER_PING_DELAY == 0)
		{
			return;
		}
		_pingTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new PingTask(), Config.GAME_SERVER_PING_DELAY, Config.GAME_SERVER_PING_DELAY);
	}

	public void stopPingTask()
	{
		if (_pingTask != null)
		{
			_pingTask.cancel(false);
			_pingTask = null;
		}
	}
}