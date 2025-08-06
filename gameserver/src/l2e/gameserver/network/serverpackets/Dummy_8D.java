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
package l2e.gameserver.network.serverpackets;

/**
 * Created by LordWinter (Open URL for SmartGuard)
 */
public class Dummy_8D extends GameServerPacket
{
	private final String _url;

	public Dummy_8D(final String url)
	{
		_url = url;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x03);
		writeS(_url);
	}
}