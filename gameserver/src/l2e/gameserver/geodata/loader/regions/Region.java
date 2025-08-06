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
package l2e.gameserver.geodata.loader.regions;

import java.nio.ByteBuffer;

import l2e.gameserver.geodata.loader.IBlock;
import l2e.gameserver.geodata.loader.IRegion;
import l2e.gameserver.geodata.loader.blocks.ComplexBlock;
import l2e.gameserver.geodata.loader.blocks.FlatBlock;
import l2e.gameserver.geodata.loader.blocks.MultilayerBlock;

public final class Region implements IRegion
{
	private final IBlock[] _blocks = new IBlock[IRegion.REGION_BLOCKS];

	public Region(ByteBuffer bb, boolean isL2Off)
	{
		for (int blockOffset = 0; blockOffset < IRegion.REGION_BLOCKS; blockOffset++)
		{
			if (!isL2Off)
			{
				final int blockType = bb.get();
				switch (blockType)
				{
					case IBlock.TYPE_FLAT -> _blocks[blockOffset] = new FlatBlock(bb, isL2Off);
					case IBlock.TYPE_COMPLEX -> _blocks[blockOffset] = new ComplexBlock(bb);
					case IBlock.TYPE_MULTILAYER -> _blocks[blockOffset] = new MultilayerBlock(bb, isL2Off);
					default -> throw new RuntimeException("Invalid block type " + blockType + "!");
				}
			}
			else
			{
				final short blockType = bb.getShort();
				switch (blockType)
				{
					case IBlock.TYPE_FLAT -> _blocks[blockOffset] = new FlatBlock(bb, isL2Off);
					case IBlock.TYPE_COMPLEX_L2OFF -> _blocks[blockOffset] = new ComplexBlock(bb);
					default -> _blocks[blockOffset] = new MultilayerBlock(bb, isL2Off);
				}
			}
		}
	}

	private IBlock getBlock(int geoX, int geoY)
	{
		return _blocks[(((geoX / IBlock.BLOCK_CELLS_X) % IRegion.REGION_BLOCKS_X) * IRegion.REGION_BLOCKS_Y) + ((geoY / IBlock.BLOCK_CELLS_Y) % IRegion.REGION_BLOCKS_Y)];
	}

	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return getBlock(geoX, geoY).checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	@Override
	public int getNearestNswe(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getNearestNswe(geoX, geoY, worldZ);
	}

	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getNearestZ(geoX, geoY, worldZ);
	}

	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getNextLowerZ(geoX, geoY, worldZ);
	}

	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return getBlock(geoX, geoY).getNextHigherZ(geoX, geoY, worldZ);
	}

	@Override
	public boolean hasGeo()
	{
		return true;
	}
}
