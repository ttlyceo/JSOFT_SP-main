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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.loginserver.ThreadPoolManager;

public class GameServerCommunication extends Thread
{
	private static final Logger _log = LoggerFactory.getLogger(GameServerCommunication.class);
	
	private static final GameServerCommunication instance = new GameServerCommunication();
	
	private final ByteBuffer _writeBuffer = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);
	private Selector _selector;
	private boolean _shutdown;
	
	public static GameServerCommunication getInstance()
	{
		return instance;
	}
	
	private GameServerCommunication()
	{
		
	}
	
	public void openServerSocket(InetAddress address, int tcpPort) throws IOException
	{
		_selector = Selector.open();
		
		final ServerSocketChannel selectable = ServerSocketChannel.open();
		selectable.configureBlocking(false);
		
		selectable.socket().bind(address == null ? new InetSocketAddress(tcpPort) : new InetSocketAddress(address, tcpPort));
		selectable.register(_selector, selectable.validOps());
	}
	
	@Override
	public void run()
	{
		Set<SelectionKey> keys;
		Iterator<SelectionKey> iterator;
		SelectionKey key = null;
		int opts;
		
		while (!isShutdown())
		{
			try
			{
				_selector.select();
				keys = _selector.selectedKeys();
				iterator = keys.iterator();
				
				while (iterator.hasNext())
				{
					key = iterator.next();
					iterator.remove();
					
					if (!key.isValid())
					{
						close(key);
						continue;
					}
					
					opts = key.readyOps();
					
					switch (opts)
					{
						case SelectionKey.OP_CONNECT :
							close(key);
							break;
						case SelectionKey.OP_ACCEPT :
							accept(key);
							break;
						case SelectionKey.OP_WRITE :
							write(key);
							break;
						case SelectionKey.OP_READ :
							read(key);
							break;
						case SelectionKey.OP_READ | SelectionKey.OP_WRITE :
							write(key);
							read(key);
							break;
					}
				}
			}
			catch (final ClosedSelectorException e)
			{
				_log.warn("Selector " + _selector + " closed!");
				return;
			}
			catch (final IOException e)
			{
				_log.warn("Gameserver disconnected...");
				close(key);
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}
	
	public void accept(SelectionKey key) throws IOException
	{
		final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc;
		SelectionKey clientKey;
		
		sc = ssc.accept();
		sc.configureBlocking(false);
		clientKey = sc.register(_selector, SelectionKey.OP_READ);
		
		GameServerConnection conn;
		clientKey.attach(conn = new GameServerConnection(clientKey));
		conn.setGameServer(new GameServer(conn));
	}
	
	public void read(SelectionKey key) throws IOException
	{
		final SocketChannel channel = (SocketChannel) key.channel();
		final GameServerConnection conn = (GameServerConnection) key.attachment();
		final GameServer gs = conn.getGameServer();
		final ByteBuffer buf = conn.getReadBuffer();
		
		int count;
		
		count = channel.read(buf);
		
		if (count == -1)
		{
			close(key);
			return;
		}
		else if (count == 0)
		{
			return;
		}
		
		buf.flip();
		
		while (tryReadPacket(key, gs, buf))
		{
			;
		}
	}
	
	protected boolean tryReadPacket(SelectionKey key, GameServer gs, ByteBuffer buf) throws IOException
	{
		final int pos = buf.position();
		if (buf.remaining() > 2)
		{
			int size = buf.getShort() & 0xffff;
			if (size <= 2)
			{
				throw new IOException("Incorrect packet size: <= 2");
			}
			
			size -= 2;
			if (size <= buf.remaining())
			{
				final int limit = buf.limit();
				buf.limit(pos + size + 2);
				
				final ReceivablePacket rp = PacketHandler.handlePacket(gs, buf);
				
				if (rp != null)
				{
					rp.setByteBuffer(buf);
					rp.setClient(gs);
					
					if (rp.read())
					{
						ThreadPoolManager.getInstance().execute(rp);
					}
					
					rp.setByteBuffer(null);
				}
				
				buf.limit(limit);
				buf.position(pos + size + 2);
				if (!buf.hasRemaining())
				{
					buf.clear();
					return false;
				}
				
				return true;
			}
			buf.position(pos);
		}
		buf.compact();
		return false;
	}
	
	public void write(SelectionKey key) throws IOException
	{
		final GameServerConnection conn = (GameServerConnection) key.attachment();
		final GameServer gs = conn.getGameServer();
		final SocketChannel channel = (SocketChannel) key.channel();
		final ByteBuffer buf = getWriteBuffer();
		
		conn.disableWriteInterest();
		
		final Queue<SendablePacket> sendQueue = conn._sendQueue;
		final Lock sendLock = conn._sendLock;
		
		boolean done;
		
		sendLock.lock();
		try
		{
			int i = 0;
			SendablePacket sp;
			while (i++ < 64 && (sp = sendQueue.poll()) != null)
			{
				final int headerPos = buf.position();
				buf.position(headerPos + 2);
				sp.setByteBuffer(buf);
				sp.setClient(gs);
				sp.write();
				
				final int dataSize = buf.position() - headerPos - 2;
				if (dataSize == 0)
				{
					buf.position(headerPos);
					continue;
				}
				buf.position(headerPos);
				buf.putShort((short) (dataSize + 2));
				buf.position(headerPos + dataSize + 2);
			}
			
			done = sendQueue.isEmpty();
			if (done)
			{
				conn.disableWriteInterest();
			}
		}
		finally
		{
			sendLock.unlock();
		}
		
		buf.flip();
		
		channel.write(buf);
		
		if (buf.remaining() > 0)
		{
			buf.compact();
			done = false;
		}
		else
		{
			buf.clear();
		}
		
		if (!done)
		{
			if (conn.enableWriteInterest())
			{
				_selector.wakeup();
			}
		}
	}
	
	private ByteBuffer getWriteBuffer()
	{
		return _writeBuffer;
	}
	
	public void close(SelectionKey key)
	{
		if (key == null)
		{
			return;
		}
		
		try
		{
			try
			{
				final GameServerConnection conn = (GameServerConnection) key.attachment();
				if (conn != null)
				{
					conn.onDisconnection();
				}
			}
			finally
			{
				key.channel().close();
				key.cancel();
			}
		}
		catch (final IOException e)
		{
			_log.warn("", e);
		}
	}
	
	public boolean isShutdown()
	{
		return _shutdown;
	}
	
	public void setShutdown(boolean shutdown)
	{
		_shutdown = shutdown;
	}
}