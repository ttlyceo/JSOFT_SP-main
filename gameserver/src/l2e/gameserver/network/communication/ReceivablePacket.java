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
package l2e.gameserver.network.communication;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ReceivablePacket extends org.nio.ReceivablePacket<AuthServerCommunication>
{
	public static final Logger _log = LoggerFactory.getLogger(ReceivablePacket.class);
	
	@Override
	public AuthServerCommunication getClient()
	{
		return AuthServerCommunication.getInstance();
	}
	
	@Override
	protected ByteBuffer getByteBuffer()
	{
		return getClient().getReadBuffer();
	}
	
	@Override
	public final boolean read()
	{
		try
		{
			readImpl();
		}
		catch (final Exception e)
		{
			_log.warn("", e);
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
			_log.warn("", e);
		}
	}
	
	protected abstract void readImpl();
	
	protected abstract void runImpl();
	
	protected void sendPacket(SendablePacket sp)
	{
		getClient().sendPacket(sp);
	}
}