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
import l2e.gameserver.model.actor.Npc;

public final class ServerObjectInfo extends GameServerPacket
{
	private final Npc _activeChar;
	private final int _x, _y, _z, _heading;
	private final int _idTemplate;
	private final boolean _isAttackable;
	private final double _collisionHeight, _collisionRadius;
	private final String _name;
	
	public ServerObjectInfo(Npc activeChar, Creature actor)
	{
		_activeChar = activeChar;
		_idTemplate = _activeChar.getTemplate().getIdTemplate();
		_isAttackable = _activeChar.isAutoAttackable(actor, false);
		_collisionHeight = _activeChar.getColHeight();
		_collisionRadius = _activeChar.getColRadius();
		_x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_heading = _activeChar.getHeading();
		_name = _activeChar.getTemplate().getName(null);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_activeChar.getObjectId());
		writeD(_idTemplate + 1000000);
		writeS(_name);
		writeD(_isAttackable ? 0x01 : 0x00);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
		writeF(1.0);
		writeF(1.0);
		writeF(_collisionRadius);
		writeF(_collisionHeight);
		writeD((int) (_isAttackable ? _activeChar.getCurrentHp() : 0));
		writeD(_isAttackable ? (int) _activeChar.getMaxHp() : 0);
		writeD(0x01);
		writeD(0x00);
	}
}