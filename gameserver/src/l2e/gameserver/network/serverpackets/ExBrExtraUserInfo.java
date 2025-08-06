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

public class ExBrExtraUserInfo extends GameServerPacket
{
	private final int _charObjId;
	private final int _val;
	private final int _lectureMark;
	
	public ExBrExtraUserInfo(Player player)
	{
		_charObjId = player.getObjectId();
		_val = player.getAbnormalEffectMask3();
		_lectureMark = player.getLectureMark();
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_charObjId);
		writeD(_val);
		writeC(_lectureMark);
	}
}