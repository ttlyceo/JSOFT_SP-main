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
package l2e.gameserver.geodata.loader.blocks;

import java.nio.ByteBuffer;

import l2e.gameserver.geodata.loader.Cell;
import l2e.gameserver.geodata.loader.IBlock;

public class FlatBlock implements IBlock
{
	private final short _height;

	public FlatBlock(ByteBuffer bb, boolean isL2Off)
	{
		_height = bb.getShort();
		if (isL2Off)
		{
			bb.getShort();
		}
	}

	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return true;
	}
	
	@Override
	public int getNearestNswe(int geoX, int geoY, int worldZ)
	{
		return Cell.NSWE_ALL;
	}

	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return _height;
	}

	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return _height <= worldZ ? _height : worldZ;
	}

	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return _height >= worldZ ? _height : worldZ;
	}
}
