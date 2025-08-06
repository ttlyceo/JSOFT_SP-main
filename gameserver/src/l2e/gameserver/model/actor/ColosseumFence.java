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
package l2e.gameserver.model.actor;

import java.awt.Rectangle;

import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.network.serverpackets.ExColosseumFenceInfo;

public final class ColosseumFence extends GameObject
{
	public enum FenceState
	{
		HIDDEN, OPEN, CLOSED
	}

	private final int _minZ;
	private final int _maxZ;
	private final FenceState _state;
	private final Rectangle _bounds;

	private ColosseumFence(int objectId, Reflection ref, int x, int y, int z, int minZ, int maxZ, int width, int height, FenceState state)
	{
		super(objectId);
		setReflection(ref);
		setXYZ(x, y, z);
		_minZ = minZ;
		_maxZ = maxZ;
		_state = state;
		_bounds = new Rectangle(x - (width / 2), y - (height / 2), width, height);
		GameObjectsStorage.put(this);
	}

	public ColosseumFence(Reflection ref, int x, int y, int z, int minZ, int maxZ, int width, int height, FenceState state)
	{
		this(IdFactory.getInstance().getNextId(), ref, x, y, z, minZ, maxZ, width, height, state);
	}

	@Override
	public void sendInfo(Player activeChar)
	{
		activeChar.sendPacket(new ExColosseumFenceInfo(this));
	}

	public int getFenceX()
	{
		return _bounds.x;
	}

	public int getFenceY()
	{
		return _bounds.y;
	}

	public int getFenceMinZ()
	{
		return _minZ;
	}

	public int getFenceMaxZ()
	{
		return _maxZ;
	}

	public int getFenceWidth()
	{
		return _bounds.width;
	}

	public int getFenceHeight()
	{
		return _bounds.height;
	}

	public FenceState getFenceState()
	{
		return _state;
	}

	@Override
	public int getId()
	{
		return getObjectId();
	}

	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return false;
	}

	public boolean isInsideFence(int x, int y, int z)
	{
		return (x >= _bounds.x) && (y >= _bounds.y) && (z >= _minZ) && (z <= _maxZ) && (x <= (_bounds.x + _bounds.width)) && (y <= (_bounds.y + _bounds.width));
	}
	
	@Override
	public int getGeoZ(int x, int y, int z)
	{
		return z;
	}
	
	@Override
	protected void onDelete()
	{
		GameObjectsStorage.remove(this);
		super.onDelete();
	}
}