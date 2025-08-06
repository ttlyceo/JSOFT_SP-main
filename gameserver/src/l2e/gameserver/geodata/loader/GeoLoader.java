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
package l2e.gameserver.geodata.loader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReferenceArray;

import l2e.gameserver.geodata.loader.regions.NullRegion;
import l2e.gameserver.geodata.loader.regions.Region;

public final class GeoLoader
{
	private static final int WORLD_MIN_X = -655360;
	private static final int WORLD_MAX_X = 393215;
	private static final int WORLD_MIN_Y = -589824;
	private static final int WORLD_MAX_Y = 458751;
	private static final int WORLD_MIN_Z = -16384;
	private static final int WORLD_MAX_Z = 16384;

	public static final int GEO_REGIONS_X = 32;
	public static final int GEO_REGIONS_Y = 32;
	public static final int GEO_REGIONS = GEO_REGIONS_X * GEO_REGIONS_Y;

	public static final int GEO_BLOCKS_X = GEO_REGIONS_X * IRegion.REGION_BLOCKS_X;
	public static final int GEO_BLOCKS_Y = GEO_REGIONS_Y * IRegion.REGION_BLOCKS_Y;
	public static final int GEO_BLOCKS = GEO_REGIONS * IRegion.REGION_BLOCKS;

	public static final int GEO_CELLS_X = GEO_BLOCKS_X * IBlock.BLOCK_CELLS_X;
	public static final int GEO_CELLS_Y = GEO_BLOCKS_Y * IBlock.BLOCK_CELLS_Y;
	public static final int GEO_CELLS_Z = (Math.abs(WORLD_MIN_Z) + Math.abs(WORLD_MAX_Z)) / 16;

	private final AtomicReferenceArray<IRegion> _regions = new AtomicReferenceArray<>(GEO_REGIONS);

	public GeoLoader()
	{
		for (int i = 0; i < _regions.length(); i++)
		{
			_regions.set(i, NullRegion.INSTANCE);
		}
	}

	private void checkGeoX(int geoX)
	{
		if ((geoX < 0) || (geoX >= GEO_CELLS_X))
		{
			throw new IllegalArgumentException();
		}
	}

	private void checkGeoY(int geoY)
	{
		if ((geoY < 0) || (geoY >= GEO_CELLS_Y))
		{
			throw new IllegalArgumentException();
		}
	}

	private void checkGeoZ(int geoZ)
	{
		if ((geoZ < 0) || (geoZ >= GEO_CELLS_Z))
		{
			throw new IllegalArgumentException();
		}
	}

	private IRegion getRegion(int geoX, int geoY)
	{
		checkGeoX(geoX);
		checkGeoY(geoY);
		return _regions.get(((geoX / IRegion.REGION_CELLS_X) * GEO_REGIONS_Y) + (geoY / IRegion.REGION_CELLS_Y));
	}

	public void loadRegion(File file, int regionX, int regionY, boolean isL2Off) throws IOException
	{
		final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;

		try (var raf = new RandomAccessFile(file, "r"); var fc = raf.getChannel())
		{
			final var buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).load();
			buffer.order(ByteOrder.LITTLE_ENDIAN);

			if (isL2Off)
			{
				for (int i = 0; i < 18; i++)
				{
					buffer.get();
				}
			}
			_regions.set(regionOffset, new Region(buffer, isL2Off));
		}
	}

	public void unloadRegion(int regionX, int regionY)
	{
		_regions.set((regionX * GEO_REGIONS_Y) + regionY, NullRegion.INSTANCE);
	}

	public boolean hasGeoPos(int geoX, int geoY)
	{
		return getRegion(geoX, geoY).hasGeo();
	}

	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return getRegion(geoX, geoY).checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	public int getNearestNswe(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNearestNswe(geoX, geoY, worldZ);
	}

	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNearestZ(geoX, geoY, worldZ);
	}

	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNextLowerZ(geoX, geoY, worldZ);
	}

	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return getRegion(geoX, geoY).getNextHigherZ(geoX, geoY, worldZ);
	}

	public int getGeoX(int worldX)
	{
		if ((worldX < WORLD_MIN_X) || (worldX > WORLD_MAX_X))
		{
			throw new IllegalArgumentException();
		}
		return (worldX - WORLD_MIN_X) / 16;
	}

	public int getGeoY(int worldY)
	{
		if ((worldY < WORLD_MIN_Y) || (worldY > WORLD_MAX_Y))
		{
			throw new IllegalArgumentException();
		}
		return (worldY - WORLD_MIN_Y) / 16;
	}

	public int getGeoZ(int worldZ)
	{
		if ((worldZ < WORLD_MIN_Z) || (worldZ > WORLD_MAX_Z))
		{
			throw new IllegalArgumentException();
		}
		return (worldZ - WORLD_MIN_Z) / 16;
	}

	public int getWorldX(int geoX)
	{
		checkGeoX(geoX);
		return (geoX * 16) + WORLD_MIN_X + 8;
	}

	public int getWorldY(int geoY)
	{
		checkGeoY(geoY);
		return (geoY * 16) + WORLD_MIN_Y + 8;
	}

	public int getWorldZ(int geoZ)
	{
		checkGeoZ(geoZ);
		return (geoZ * 16) + WORLD_MIN_Z + 8;
	}
}
