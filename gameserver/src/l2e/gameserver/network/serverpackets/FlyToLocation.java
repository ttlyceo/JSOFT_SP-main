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

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;

public final class FlyToLocation extends GameServerPacket
{
	private final int _chaObjId;
	private final FlyType _type;
	private final Location _loc;
	private final Location _destLoc;

	public enum FlyType
	{
		THROW_UP, THROW_HORIZONTAL, DUMMY, CHARGE, NONE
	}

	public FlyToLocation(Creature cha, Location destLoc, FlyType type)
	{
		_destLoc = destLoc;
		_type = type;
		_chaObjId = cha.getObjectId();
		_loc = cha.getLocation();
	}

	public FlyToLocation(Creature cha, int destX, int destY, int destZ, FlyType type)
	{
		_chaObjId = cha.getObjectId();
		_destLoc = new Location(destX, destY, destZ);
		_type = type;
		_loc = cha.getLocation();
	}
	
	public FlyToLocation(Creature cha, int destX, int destY, int destZ, FlyType type, int flySpeed, int flyDelay, int animationSpeed)
	{
		_chaObjId = cha.getObjectId();
		_loc = cha.getLocation();
		_destLoc = new Location(destX, destY, destZ);
		_type = type;
	}

	public FlyToLocation(Creature cha, GameObject dest, FlyType type)
	{
		this(cha, dest.getX(), dest.getY(), dest.getZ(), type);
	}

	@Override
	protected void writeImpl()
	{
		writeD(_chaObjId);
		writeD(_destLoc.getX());
		writeD(_destLoc.getY());
		writeD(_destLoc.getZ());
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(_type.ordinal());
	}
}