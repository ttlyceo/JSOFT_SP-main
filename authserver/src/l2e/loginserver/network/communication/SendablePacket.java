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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SendablePacket extends org.nio.SendablePacket<GameServer>
{
	public static final Logger _log = LoggerFactory.getLogger(SendablePacket.class);
	
	protected GameServer _gs;
	protected ByteBuffer _buf;
	
	protected void setByteBuffer(ByteBuffer buf)
	{
		_buf = buf;
	}
	
	@Override
	protected ByteBuffer getByteBuffer()
	{
		return _buf;
	}
	
	protected void setClient(GameServer gs)
	{
		_gs = gs;
	}
	
	@Override
	public GameServer getClient()
	{
		return _gs;
	}
	
	public GameServer getGameServer()
	{
		return getClient();
	}

	@Override
	public boolean write()
	{
		try
		{
			writeImpl();
		}
		catch (final Exception e)
		{
			_log.warn("", e);
		}
		return true;
	}

	protected abstract void writeImpl();
}