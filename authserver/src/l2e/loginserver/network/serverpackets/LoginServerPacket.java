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
package l2e.loginserver.network.serverpackets;

import org.nio.impl.SendablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.loginserver.network.LoginClient;

public abstract class LoginServerPacket extends SendablePacket<LoginClient>
{
	private static final Logger _log = LoggerFactory.getLogger(LoginServerPacket.class);

	@Override
	public final boolean write()
	{
		try
		{
			writeImpl();
			return true;
		}
		catch(final Exception e)
		{
			_log.warn("Client: " + getClient() + " - Failed writing: " + getClass().getSimpleName() + "!", e);
		}
		return false;
	}

	protected abstract void writeImpl();
}
