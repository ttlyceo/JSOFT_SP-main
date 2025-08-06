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

import l2e.gameserver.model.actor.Player;

public class ExDuelUpdateUserInfo extends GameServerPacket
{
	private final Player _activeChar;

	public ExDuelUpdateUserInfo(Player cha)
	{
		_activeChar = cha;
	}
	
	@Override
	protected void writeImpl()
	{
		writeS(_activeChar.getName(null));
		writeD(_activeChar.getObjectId());
		writeD(_activeChar.getClassId().getId());
		writeD(_activeChar.getLevel());
		writeD((int) _activeChar.getCurrentHp());
		writeD((int) _activeChar.getMaxHp());
		writeD((int) _activeChar.getCurrentMp());
		writeD((int) _activeChar.getMaxMp());
		writeD((int) _activeChar.getCurrentCp());
		writeD((int) _activeChar.getMaxCp());
	}
}