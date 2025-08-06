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
import l2e.gameserver.model.actor.templates.items.Henna;

public class HennaItemInfo extends GameServerPacket
{
	private final Player _activeChar;
	private final Henna _henna;

	public HennaItemInfo(Henna henna, Player player)
	{
		_henna = henna;
		_activeChar = player;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_henna.getDyeId());
		writeD(_henna.getDyeItemId());
		writeQ(_henna.getWearCount());
		writeQ(_henna.getWearFee());
		writeD(_henna.isAllowedClass(_activeChar.getClassId()) ? 0x01 : 0x00);
		writeQ(_activeChar.getAdena());
		writeD(_activeChar.getINT());
		writeC(_activeChar.getINT() + _henna.getStatINT());
		writeD(_activeChar.getSTR());
		writeC(_activeChar.getSTR() + _henna.getStatSTR());
		writeD(_activeChar.getCON());
		writeC(_activeChar.getCON() + _henna.getStatCON());
		writeD(_activeChar.getMEN());
		writeC(_activeChar.getMEN() + _henna.getStatMEN());
		writeD(_activeChar.getDEX());
		writeC(_activeChar.getDEX() + _henna.getStatDEX());
		writeD(_activeChar.getWIT());
		writeC(_activeChar.getWIT() + _henna.getStatWIT());
	}
}