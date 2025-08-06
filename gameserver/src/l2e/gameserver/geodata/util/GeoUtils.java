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
package l2e.gameserver.geodata.util;

import java.awt.Color;

import l2e.commons.geometry.LinePointIterator;
import l2e.commons.geometry.LinePointIterator3D;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.geodata.loader.Cell;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.ExServerPrimitive;

public final class GeoUtils
{
	public static void debug2DLine(Player player, int x, int y, int tx, int ty, int z)
	{
		final int gx = GeoEngine.getInstance().getGeoX(x);
		final int gy = GeoEngine.getInstance().getGeoY(y);

		final int tgx = GeoEngine.getInstance().getGeoX(tx);
		final int tgy = GeoEngine.getInstance().getGeoY(ty);

		final ExServerPrimitive prim = new ExServerPrimitive("Debug2DLine", x, y, z);
		prim.addLine(Color.BLUE, GeoEngine.getInstance().getWorldX(gx), GeoEngine.getInstance().getWorldY(gy), z, GeoEngine.getInstance().getWorldX(tgx), GeoEngine.getInstance().getWorldY(tgy), z);

		final LinePointIterator iter = new LinePointIterator(gx, gy, tgx, tgy);

		while (iter.next())
		{
			final int wx = GeoEngine.getInstance().getWorldX(iter.x());
			final int wy = GeoEngine.getInstance().getWorldY(iter.y());

			prim.addPoint(Color.RED, wx, wy, z);
		}
		player.sendPacket(prim);
	}

	public static void debug3DLine(Player player, int x, int y, int z, int tx, int ty, int tz)
	{
		final int gx = GeoEngine.getInstance().getGeoX(x);
		final int gy = GeoEngine.getInstance().getGeoY(y);

		final int tgx = GeoEngine.getInstance().getGeoX(tx);
		final int tgy = GeoEngine.getInstance().getGeoY(ty);

		final ExServerPrimitive prim = new ExServerPrimitive("Debug3DLine", x, y, z);
		prim.addLine(Color.BLUE, GeoEngine.getInstance().getWorldX(gx), GeoEngine.getInstance().getWorldY(gy), z, GeoEngine.getInstance().getWorldX(tgx), GeoEngine.getInstance().getWorldY(tgy), tz);

		final LinePointIterator3D iter = new LinePointIterator3D(gx, gy, z, tgx, tgy, tz);
		iter.next();
		int prevX = iter.x();
		int prevY = iter.y();
		int wx = GeoEngine.getInstance().getWorldX(prevX);
		int wy = GeoEngine.getInstance().getWorldY(prevY);
		int wz = iter.z();
		prim.addPoint(Color.RED, wx, wy, wz);

		while (iter.next())
		{
			final int curX = iter.x();
			final int curY = iter.y();

			if ((curX != prevX) || (curY != prevY))
			{
				wx = GeoEngine.getInstance().getWorldX(curX);
				wy = GeoEngine.getInstance().getWorldY(curY);
				wz = iter.z();

				prim.addPoint(Color.RED, wx, wy, wz);

				prevX = curX;
				prevY = curY;
			}
		}
		player.sendPacket(prim);
	}

	private static Color getDirectionColor(int x, int y, double z, int nswe)
	{
		if (GeoEngine.getInstance().checkNearestNswe(x, y, z, nswe))
		{
			return Color.GREEN;
		}
		return Color.RED;
	}

	public static void debugGrid(Player player, int radius)
	{
		final int blocksPerPacket = 40;
		if (radius < 0)
		{
			throw new IllegalArgumentException("geoRadius < 0");
		}

		int iBlock = blocksPerPacket;
		int iPacket = 0;

		ExServerPrimitive exsp = null;
		final GeoEngine gd = GeoEngine.getInstance();
		final int playerGx = gd.getGeoX(player.getX());
		final int playerGy = gd.getGeoY(player.getY());
		for (int dx = -radius; dx <= radius; ++dx)
		{
			for (int dy = -radius; dy <= radius; ++dy)
			{
				if (iBlock >= blocksPerPacket)
				{
					iBlock = 0;
					if (exsp != null)
					{
						++iPacket;
						player.sendPacket(exsp);
					}
					exsp = new ExServerPrimitive("DebugGrid_" + iPacket, player.getX(), player.getY(), -16000);
				}

				if (exsp == null)
				{
					throw new IllegalStateException();
				}

				final int gx = playerGx + dx;
				final int gy = playerGy + dy;

				final int x = gd.getWorldX(gx);
				final int y = gd.getWorldY(gy);
				final int z = gd.getNearestZ(gx, gy, player.getZ());

				Color col = getDirectionColor(gx, gy, z, Cell.NSWE_NORTH);
				exsp.addLine(col, x - 1, y - 7, z, x + 1, y - 7, z);
				exsp.addLine(col, x - 2, y - 6, z, x + 2, y - 6, z);
				exsp.addLine(col, x - 3, y - 5, z, x + 3, y - 5, z);
				exsp.addLine(col, x - 4, y - 4, z, x + 4, y - 4, z);

				col = getDirectionColor(gx, gy, z, Cell.NSWE_EAST);
				exsp.addLine(col, x + 7, y - 1, z, x + 7, y + 1, z);
				exsp.addLine(col, x + 6, y - 2, z, x + 6, y + 2, z);
				exsp.addLine(col, x + 5, y - 3, z, x + 5, y + 3, z);
				exsp.addLine(col, x + 4, y - 4, z, x + 4, y + 4, z);

				col = getDirectionColor(gx, gy, z, Cell.NSWE_SOUTH);
				exsp.addLine(col, x - 1, y + 7, z, x + 1, y + 7, z);
				exsp.addLine(col, x - 2, y + 6, z, x + 2, y + 6, z);
				exsp.addLine(col, x - 3, y + 5, z, x + 3, y + 5, z);
				exsp.addLine(col, x - 4, y + 4, z, x + 4, y + 4, z);

				col = getDirectionColor(gx, gy, z, Cell.NSWE_WEST);
				exsp.addLine(col, x - 7, y - 1, z, x - 7, y + 1, z);
				exsp.addLine(col, x - 6, y - 2, z, x - 6, y + 2, z);
				exsp.addLine(col, x - 5, y - 3, z, x - 5, y + 3, z);
				exsp.addLine(col, x - 4, y - 4, z, x - 4, y + 4, z);

				++iBlock;
			}
		}

		player.sendPacket(exsp);
	}

	public static int computeNswe(int lastX, int lastY, int x, int y)
	{
		if (x > lastX)
		{
			if (y > lastY)
			{
				return Cell.NSWE_SOUTH_EAST;
			}
			else if (y < lastY)
			{
				return Cell.NSWE_NORTH_EAST;
			}
			else
			{
				return Cell.NSWE_EAST;
			}
		}
		else if (x < lastX)
		{
			if (y > lastY)
			{
				return Cell.NSWE_SOUTH_WEST;
			}
			else if (y < lastY)
			{
				return Cell.NSWE_NORTH_WEST;
			}
			else
			{
				return Cell.NSWE_WEST;
			}
		}
		else
		{
			if (y > lastY)
			{
				return Cell.NSWE_SOUTH;
			}
			else if (y < lastY)
			{
				return Cell.NSWE_NORTH;
			}
			else
			{
				throw new RuntimeException();
			}
		}
	}
}