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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ReceivablePacket extends org.nio.ReceivablePacket<GameServer>
{
	public static final Logger _log = LoggerFactory.getLogger(ReceivablePacket.class);
	
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
	public final boolean read()
	{
		try
		{
			readImpl();
		}
		catch (final BufferUnderflowException e)
		{}
		catch (final Exception e)
		{
		}
		return true;
	}
	
	@Override
	public final void run()
	{
		try
		{
			runImpl();
		}
		catch (final Exception e)
		{
		}
	}
	
	protected abstract void readImpl();

	protected abstract void runImpl();

	public void sendPacket(SendablePacket packet)
	{
		getGameServer().sendPacket(packet);
	}
}