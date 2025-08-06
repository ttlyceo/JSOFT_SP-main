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

import l2e.loginserver.network.LoginClient;

public final class Init extends LoginServerPacket
{
	private final int _sessionId;
	private final byte[] _publicKey;
	private final byte[] _blowfishKey;
	private final int _protocol;

	public Init(LoginClient client)
	{
		this(client.getScrambledModulus(), client.getBlowfishKey(), client.getSessionId(), client.getProtocol());
	}

	public Init(byte[] publickey, byte[] blowfishkey, int sessionId, int protocol)
	{
		_sessionId = sessionId;
		_publicKey = publickey;
		_blowfishKey = blowfishkey;
		_protocol = protocol;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x00);
		writeD(_sessionId);
		writeD(_protocol);
		writeB(_publicKey);
		writeB(new byte[16]);
		writeB(_blowfishKey);
		writeD(0x00);
	}
}