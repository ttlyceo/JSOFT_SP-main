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
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.StaticObjectInstance;

public class StaticObject extends GameServerPacket
{
	private final int _staticObjectId;
	private final int _objectId;
	private final int _type;
	private final int _isTargetable;
	private final int _meshIndex;
	private final int _isClosed;
	private final int _isEnemy;
	private final double _maxHp;
	private final int _currentHp;
	private final int _showHp;
	private final int _damageGrade;
	
	public StaticObject(StaticObjectInstance staticObject)
	{
		_staticObjectId = staticObject.getId();
		_objectId = staticObject.getObjectId();
		_type = 0;
		_isTargetable = 1;
		_meshIndex = staticObject.getMeshIndex();
		_isClosed = 0;
		_isEnemy = 0;
		_maxHp = 0;
		_currentHp = 0;
		_showHp = 0;
		_damageGrade = 0;
	}
	
	public StaticObject(DoorInstance door, boolean targetable)
	{
		_staticObjectId = door.getDoorId();
		_objectId = door.getObjectId();
		_type = 1;
		_isTargetable = door.isTargetable() || targetable ? 1 : 0;
		_meshIndex = door.getMeshIndex();
		_isClosed = door.isOpen() ? 1 : 0;
		_isEnemy = door.isEnemy() ? 1 : 0;
		_maxHp = door.getMaxHp();
		_currentHp = (int) door.getCurrentHp();
		_showHp = door.getIsShowHp() ? 1 : 0;
		_damageGrade = door.getDamage();
	}
	
	public StaticObject(DoorInstance door, Player player)
	{
		_staticObjectId = door.getDoorId();
		_objectId = door.getObjectId();
		_type = 1;
		_isTargetable = door.isTargetable() ? 1 : player.isGM() ? 1 : 0;
		_meshIndex = 1;
		_isClosed = door.isOpen() ? 0 : 1;
		_isEnemy = door.isEnemy() ? 1 : 0;
		_currentHp = (int) door.getCurrentHp();
		_maxHp = door.getMaxHp();
		_showHp = door.getIsShowHp() ? 1 : 0;
		_damageGrade = door.getDamage();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeD(_staticObjectId);
		writeD(_objectId);
		writeD(_type);
		writeD(_isTargetable);
		writeD(_meshIndex);
		writeD(_isClosed);
		writeD(_isEnemy);
		writeD(_currentHp);
		writeD((int) _maxHp);
		writeD(_showHp);
		writeD(_damageGrade);
	}
}