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

import l2e.gameserver.model.actor.Creature;

public class ChangeMoveType extends GameServerPacket
{
	public static final int WALK = 0x00;
	public static final int RUN = 0x01;

	private final int _charObjId;
	private final boolean _running;

	public ChangeMoveType(Creature character)
	{
		_charObjId = character.getObjectId();
		_running = character.isRunning();
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_charObjId);
		writeD(_running ? RUN : WALK);
		writeD(0x00);
	}
}