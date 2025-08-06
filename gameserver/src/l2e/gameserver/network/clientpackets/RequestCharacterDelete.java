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
import l2e.gameserver.network.serverpackets.CharacterDeleteFail;
import l2e.gameserver.network.serverpackets.CharacterDeleteSuccess;
import l2e.gameserver.network.serverpackets.CharacterSelectionInfo;

public final class RequestCharacterDelete extends GameClientPacket
{
	private int _charSlot;
	
	@Override
	protected void readImpl()
	{
		_charSlot = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.DEBUG)
		{
			_log.info("deleting slot:" + _charSlot);
		}
		
		try
		{
			final byte answer = getClient().markToDeleteChar(_charSlot);
			
			switch (answer)
			{
				default :
				case -1 :
					break;
				case 0 :
					sendPacket(new CharacterDeleteSuccess());
					break;
				case 1 :
					sendPacket(new CharacterDeleteFail(CharacterDeleteFail.REASON_YOU_MAY_NOT_DELETE_CLAN_MEMBER));
					break;
				case 2 :
					sendPacket(new CharacterDeleteFail(CharacterDeleteFail.REASON_CLAN_LEADERS_MAY_NOT_BE_DELETED));
					break;
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error:", e);
		}
		final CharacterSelectionInfo cl = new CharacterSelectionInfo(getClient().getLogin(), getClient().getSessionId().playOkID1, 0);
		getClient().sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}
}