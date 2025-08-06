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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.GameClient.GameClientState;

public class RequestSaveKeyMapping extends GameClientPacket
{
	private static final String SPLIT_VAR = "	";
	private byte[] _uiKeyMapping;

	@Override
	protected void readImpl()
	{
		final int dataSize = readD();
		if (dataSize > 0)
		{
			_uiKeyMapping = new byte[dataSize];
			readB(_uiKeyMapping);
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getActiveChar();
		if (!Config.STORE_UI_SETTINGS || (player == null) || _uiKeyMapping == null || (getClient().getState() != GameClientState.IN_GAME))
		{
			return;
		}
		
		String uiKeyMapping = "";
		for (final Byte b : _uiKeyMapping)
		{
			uiKeyMapping += b + SPLIT_VAR;
		}
		player.setVar("UI_KEY_MAPPING", uiKeyMapping);
	}
}