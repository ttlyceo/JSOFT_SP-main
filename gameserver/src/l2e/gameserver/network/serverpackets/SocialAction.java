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

public class SocialAction extends GameServerPacket
{
	public static final int LEVEL_UP = 2122;

	private final int _charObjId;
	private final int _actionId;
	
	public SocialAction(int objectId, int actionId)
	{
		_charObjId = objectId;
		_actionId = actionId;
	}
	
	public SocialAction(Creature cha, int actionId)
	{
		this(cha.getObjectId(), actionId);
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_charObjId);
		writeD(_actionId);
	}
}