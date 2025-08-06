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
import java.nio.ByteOrder;
import java.util.Arrays;

import l2e.gameserver.geodata.loader.IBlock;

public class MultilayerBlock implements IBlock
{
	private final byte[] _data;

	public MultilayerBlock(ByteBuffer bb, boolean isL2Off)
	{
		final var temp = ByteBuffer.allocate(IBlock.BLOCK_CELLS * Byte.MAX_VALUE * 2);
		temp.order(ByteOrder.LITTLE_ENDIAN);

		for (int blockCellOffset = 0; blockCellOffset < IBlock.BLOCK_CELLS; blockCellOffset++)
		{
			byte nLayers;
			if (isL2Off)
			{
				nLayers = (byte) bb.getShort();
			}
			else
			{
				nLayers = bb.get();
			}

			if ((nLayers <= 0) || (nLayers > 125))
			{
				throw new RuntimeException("GeoEngine: l2j geo file corrupted! Invalid layers count!");
			}
			temp.put(nLayers);
			for (byte layer = 0; layer < nLayers; layer++)
			{
				temp.putShort(bb.getShort());
			}
		}
		_data = Arrays.copyOf(temp.array(), temp.position());
		temp.clear();
	}

	private short _getNearestLayer(int geoX, int geoY, int worldZ)
	{
		final int startOffset = _getCellDataOffset(geoX, geoY);
		final byte nLayers = _data[startOffset];
		final int endOffset = startOffset + 1 + (nLayers * 2);

		int nearestDZ = 0;
		short nearestData = 0;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = _extractLayerData(offset);
			final int layerZ = _extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				return layerData;
			}

			final int layerDZ = Math.abs(layerZ - worldZ);
			if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ))
			{
				nearestDZ = layerDZ;
				nearestData = layerData;
			}
		}

		return nearestData;
	}

	private int _getCellDataOffset(int geoX, int geoY)
	{
		final int cellLocalOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
		int cellDataOffset = 0;
		for (int i = 0; i < cellLocalOffset; i++)
		{
			cellDataOffset += 1 + (_data[cellDataOffset] * 2);
		}
		return cellDataOffset;
	}

	private short _extractLayerData(int dataOffset)
	{
		return (short) ((_data[dataOffset] & 0xFF) | (_data[dataOffset + 1] << 8));
	}

	private int _getNearestNSWE(int geoX, int geoY, int worldZ)
	{
		return _extractLayerNswe(_getNearestLayer(geoX, geoY, worldZ));
	}

	private int _extractLayerNswe(short layer)
	{
		return (byte) (layer & 0x000F);
	}

	private int _extractLayerHeight(short layer)
	{
		layer = (short) (layer & 0x0fff0);
		return layer >> 1;
	}
	
	@Override
	public int getNearestNswe(int geoX, int geoY, int worldZ)
	{
		return _getNearestNSWE(geoX, geoY, worldZ);
	}

	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return (_getNearestNSWE(geoX, geoY, worldZ) & nswe) == nswe;
	}

	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return _extractLayerHeight(_getNearestLayer(geoX, geoY, worldZ));
	}

	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		final int startOffset = _getCellDataOffset(geoX, geoY);
		final byte nLayers = _data[startOffset];
		final int endOffset = startOffset + 1 + (nLayers * 2);

		int lowerZ = Integer.MIN_VALUE;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = _extractLayerData(offset);

			final int layerZ = _extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				return layerZ;
			}

			if ((layerZ < worldZ) && (layerZ > lowerZ))
			{
				lowerZ = layerZ;
			}
		}

		return lowerZ == Integer.MIN_VALUE ? worldZ : lowerZ;
	}

	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		final int startOffset = _getCellDataOffset(geoX, geoY);
		final byte nLayers = _data[startOffset];
		final int endOffset = startOffset + 1 + (nLayers * 2);

		int higherZ = Integer.MAX_VALUE;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = _extractLayerData(offset);

			final int layerZ = _extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				return layerZ;
			}

			if ((layerZ > worldZ) && (layerZ < higherZ))
			{
				higherZ = layerZ;
			}
		}
		return higherZ == Integer.MAX_VALUE ? worldZ : higherZ;
	}
}
