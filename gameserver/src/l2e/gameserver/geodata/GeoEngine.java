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
package l2e.gameserver.geodata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.geometry.LinePointIterator;
import l2e.commons.geometry.LinePointIterator3D;
import l2e.gameserver.Config;
import l2e.gameserver.geodata.loader.Cell;
import l2e.gameserver.geodata.loader.GeoLoader;
import l2e.gameserver.geodata.pathfinding.PathFinding;
import l2e.gameserver.geodata.util.GeoUtils;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.interfaces.ILocational;

public final class GeoEngine
{
	private static final Logger _log = LoggerFactory.getLogger(GeoEngine.class);
	
	private static final int ELEVATED_SEE_OVER_DISTANCE = 2;
	private static final int MAX_SEE_OVER_HEIGHT = 48;
	private static final int SPAWN_Z_DELTA_LIMIT = 100;
	
	public static float LOW_WEIGHT = 0.5F;
	public static float MEDIUM_WEIGHT = 2F;
	public static float HIGH_WEIGHT = 3F;
	public static float DIAGONAL_WEIGHT = 0.707F;

	private final GeoLoader _geoLoader = new GeoLoader();

	protected GeoEngine()
	{
		load();
	}

	private void load()
	{
		if (!Config.GEODATA)
		{
			_log.info("GeoEngine: Disabled.");
			return;
		}
		
		_log.info("GeoEngine: Loading Geodata...");
		
		final var geoDir = new File(Config.DATAPACK_ROOT, "geodata");
		
		int loadedRegions = 0;
		try
		{
			for (int rx = Config.GEO_X_FIRST; rx <= Config.GEO_X_LAST; rx++)
			{
				for (int ry = Config.GEO_Y_FIRST; ry <= Config.GEO_Y_LAST; ry++)
				{
					File geoFile;
					boolean isL2Off = false;
					if ((geoFile = new File(geoDir, String.format("%2d_%2d.l2j", rx, ry))).exists())
					{
						isL2Off = false;
					}
					else if ((geoFile = new File(geoDir, String.format("%2d_%2d_conv.dat", rx, ry))).exists())
					{
						isL2Off = true;
					}
					else
					{
						continue;
					}
					
					_geoLoader.loadRegion(geoFile, rx, ry, isL2Off);
					loadedRegions++;
				}
			}
		}
		catch (final Exception e)
		{
			_log.info("GeoEngine: Files missing, loading aborted.");
		}

		_log.info("GeoEngine: Loaded " + loadedRegions + " map(s).");
		PathFinding.getInstance().load();
	}

	public boolean hasGeoPos(int geoX, int geoY)
	{
		if (!Config.GEODATA)
		{
			return false;
		}
		return _geoLoader.hasGeoPos(geoX, geoY);
	}

	public boolean checkNearestNswe(int geoX, int geoY, double worldZ, int nswe)
	{
		return _geoLoader.checkNearestNswe(geoX, geoY, (int) worldZ, nswe);
	}
	
	public int getNearestNSWE(ILocational loc)
	{
		return getNearestNSWE(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public int getNearestNSWE(int x, int y, double worldZ)
	{
		return NgetNearestNSWE(getGeoX(x), getGeoY(y), worldZ);
	}
	
	public int NgetNearestNSWE(int geoX, int geoY, double worldZ)
	{
		return _geoLoader.getNearestNswe(geoX, geoY, (int) worldZ);
	}

	public boolean checkNearestNsweAntiCornerCut(int geoX, int geoY, double worldZ, int nswe)
	{
		boolean can = true;
		
		if (!Config.GEODATA)
		{
			return can;
		}
		
		if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST)
		{
			can = checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_EAST) && checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_NORTH);
		}

		if (can && ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST))
		{
			can = checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_WEST) && checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_NORTH);
		}

		if (can && ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST))
		{
			can = checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_EAST) && checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_SOUTH);
		}

		if (can && ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST))
		{
			can = checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_WEST) && checkNearestNswe(geoX - 1, geoY, worldZ, Cell.NSWE_SOUTH);
		}

		return can && checkNearestNswe(geoX, geoY, worldZ, nswe);
	}

	public int getNearestZ(int geoX, int geoY, double worldZ)
	{
		return _geoLoader.getNearestZ(geoX, geoY, (int) worldZ);
	}

	public int getNextLowerZ(int geoX, int geoY, double worldZ)
	{
		return _geoLoader.getNextLowerZ(geoX, geoY, (int) worldZ);
	}

	public int getNextHigherZ(int geoX, int geoY, double worldZ)
	{
		return _geoLoader.getNextHigherZ(geoX, geoY, (int) worldZ);
	}

	public int getMapX(int x)
	{
		return ((x - World.MAP_MIN_X) >> 15) + Config.GEO_X_FIRST;
	}
	
	public int getMapY(int y)
	{
		return ((y - World.MAP_MIN_Y) >> 15) + Config.GEO_Y_FIRST;
	}
	
	public int getGeoX(double worldX)
	{
		return _geoLoader.getGeoX((int) worldX);
	}

	public int getGeoY(double worldY)
	{
		return _geoLoader.getGeoY((int) worldY);
	}

	public int getGeoZ(double worldZ)
	{
		return _geoLoader.getGeoZ((int) worldZ);
	}

	public int getWorldX(int geoX)
	{
		return _geoLoader.getWorldX(geoX);
	}

	public int getWorldY(int geoY)
	{
		return _geoLoader.getWorldY(geoY);
	}

	public int getWorldZ(int geoZ)
	{
		return _geoLoader.getWorldZ(geoZ);
	}
	
	public int getHeight(Location location)
	{
		return getHeight(location.getX(), location.getY(), location.getZ());
	}

	public int getHeight(int x, int y, int z)
	{
		return getNearestZ(getGeoX(x), getGeoY(y), z);
	}
	
	public int getSpawnHeight(int x, int y, int z)
	{
		final int geoX = getGeoX(x);
		final int geoY = getGeoY(y);

		if (!hasGeoPos(geoX, geoY))
		{
			return z;
		}

		final int nextLowerZ = getNextLowerZ(geoX, geoY, z + 20);
		return Math.abs(nextLowerZ - z) <= SPAWN_Z_DELTA_LIMIT ? nextLowerZ : z;
	}

	public double getSpawnHeight(Location location)
	{
		return getSpawnHeight(location.getX(), location.getY(), location.getZ());
	}

	public boolean canSeeTarget(GameObject cha, GameObject target)
	{
		if (!Config.GEODATA)
		{
			return true;
		}
		
		if (cha == null || target == null)
		{
			return false;
		}
		
		if (cha.equals(target) || target.isDoor())
		{
			return true;
		}
		return canSeeTarget(cha, cha.getReflection(), target.getX(), target.getY(), target.getZ(), target.getReflection());
	}

	public boolean canSeeTarget(GameObject cha, ILocational worldPosition)
	{
		return canSeeTarget(cha, cha.getReflection(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
	}

	public boolean canSeeTarget(GameObject cha, Reflection ref, int tx, int ty, int tz, Reflection tworld)
	{
		if (!Config.GEODATA)
		{
			return true;
		}
		return ref.getId() == tworld.getId() && canSeeTarget(cha, ref, tx, ty, tz);
	}

	public boolean canSeeTarget(GameObject cha, Reflection ref, int tx, int ty, int tz)
	{
		if (cha != null && cha.isInFrontDoor(tx, ty, tz, ref))
		{
			return false;
		}
		
		if (!Config.GEODATA)
		{
			return true;
		}
		return canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), tx, ty, tz);
	}

	private int getLosGeoZ(int prevX, int prevY, int prevGeoZ, int curX, int curY, int nswe)
	{
		if ((((nswe & Cell.NSWE_NORTH) != 0) && ((nswe & Cell.NSWE_SOUTH) != 0)) || (((nswe & Cell.NSWE_WEST) != 0) && ((nswe & Cell.NSWE_EAST) != 0)))
		{
			throw new RuntimeException("Multiple directions!");
		}

		if (checkNearestNsweAntiCornerCut(prevX, prevY, prevGeoZ, nswe))
		{
			return getNearestZ(curX, curY, prevGeoZ);
		}
		return getNextHigherZ(curX, curY, prevGeoZ);
	}

	public boolean canSeeTarget(double x, double y, double z, double tx, double ty, double tz)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		int tGeoX = getGeoX(tx);
		int tGeoY = getGeoY(ty);

		z = getNearestZ(geoX, geoY, z);
		tz = getNearestZ(tGeoX, tGeoY, tz);

		if ((geoX == tGeoX) && (geoY == tGeoY))
		{
			if (hasGeoPos(tGeoX, tGeoY))
			{
				return z == tz;
			}

			return true;
		}

		if (tz > z)
		{
			double tmp = tx;
			tx = x;
			x = tmp;

			tmp = ty;
			ty = y;
			y = tmp;

			tmp = tz;
			tz = z;
			z = tmp;

			tmp = tGeoX;
			tGeoX = geoX;
			geoX = (int) tmp;

			tmp = tGeoY;
			tGeoY = geoY;
			geoY = (int) tmp;
		}

		final LinePointIterator3D pointIter = new LinePointIterator3D(geoX, geoY, (int) z, tGeoX, tGeoY, (int) tz);
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		final int prevZ = pointIter.z();
		int prevGeoZ = prevZ;
		int ptIndex = 0;
		while (pointIter.next())
		{
			final int curX = pointIter.x();
			final int curY = pointIter.y();

			if ((curX == prevX) && (curY == prevY))
			{
				continue;
			}

			final int beeCurZ = pointIter.z();
			int curGeoZ = prevGeoZ;

			if (hasGeoPos(curX, curY))
			{
				final int beeCurGeoZ = getNearestZ(curX, curY, beeCurZ);
				final int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				curGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, curX, curY, nswe);
				double maxHeight;
				if (ptIndex < ELEVATED_SEE_OVER_DISTANCE)
				{
					maxHeight = z + MAX_SEE_OVER_HEIGHT;
				}
				else
				{
					maxHeight = beeCurZ + MAX_SEE_OVER_HEIGHT;
				}

				boolean canSeeThrough = false;
				if ((curGeoZ <= maxHeight) && (curGeoZ <= beeCurGeoZ))
				{
					if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST)
					{
						final int northGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY - 1, Cell.NSWE_EAST);
						final int eastGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX + 1, prevY, Cell.NSWE_NORTH);
						canSeeThrough = (northGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(prevX, prevY - 1, beeCurZ)) && (eastGeoZ <= getNearestZ(prevX + 1, prevY, beeCurZ));
					}
					else if ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST)
					{
						final int northGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY - 1, Cell.NSWE_WEST);
						final int westGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX - 1, prevY, Cell.NSWE_NORTH);
						canSeeThrough = (northGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(prevX, prevY - 1, beeCurZ)) && (westGeoZ <= getNearestZ(prevX - 1, prevY, beeCurZ));
					}
					else if ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST)
					{
						final int southGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY + 1, Cell.NSWE_EAST);
						final int eastGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX + 1, prevY, Cell.NSWE_SOUTH);
						canSeeThrough = (southGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(prevX, prevY + 1, beeCurZ)) && (eastGeoZ <= getNearestZ(prevX + 1, prevY, beeCurZ));
					}
					else if ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST)
					{
						final int southGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY + 1, Cell.NSWE_WEST);
						final int westGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX - 1, prevY, Cell.NSWE_SOUTH);
						canSeeThrough = (southGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(prevX, prevY + 1, beeCurZ)) && (westGeoZ <= getNearestZ(prevX - 1, prevY, beeCurZ));
					}
					else
					{
						canSeeThrough = true;
					}
				}

				if (!canSeeThrough)
				{
					return false;
				}
			}

			prevX = curX;
			prevY = curY;
			prevGeoZ = curGeoZ;
			++ptIndex;
		}

		return true;
	}

	public Location moveCheck(Creature creature, int x, int y, int z, int tx, int ty, int tz, Reflection ref)
	{
		return moveCheck(creature, x, y, z, tx, ty, tz, ref, true);
	}
	
	public Location moveCheck(Creature creature, int x, int y, int z, int tx, int ty, int tz, Reflection ref, boolean returnPrev)
	{
		if (creature != null && (!Config.ALLOW_DOOR_VALIDATE || !creature.isPlayable()))
		{
			if (creature.isInFrontDoor(tx, ty, tz, ref))
			{
				return moveDoorCheck(creature, tx, ty, tz, ref);
			}
		}
		
		final int geoX = getGeoX(x);
		final int geoY = getGeoY(y);
		z = getNearestZ(geoX, geoY, z);
		final int tGeoX = getGeoX(tx);
		final int tGeoY = getGeoY(ty);
		tz = getNearestZ(tGeoX, tGeoY, tz);

		final List<Location> path = new ArrayList<>();
		
		final var pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = z;

		while (pointIter.next())
		{
			final int curX = pointIter.x();
			final int curY = pointIter.y();
			final int curZ = getNearestZ(curX, curY, prevZ);
			if (hasGeoPos(prevX, prevY))
			{
				final int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, nswe))
				{
					if (path.size() >= 1)
					{
						final var location = path.get(path.size() - 1);
						path.clear();
						return new Location(getWorldX(location.getX()), getWorldY(location.getY()), location.getZ());
					}
					return new Location(x, y, z);
				}
				prevX = curX;
				prevY = curY;
				prevZ = curZ;
				path.add(new Location(prevX, prevY, prevZ));
			}
		}
		path.clear();
		if (returnPrev && hasGeoPos(prevX, prevY) && (prevZ != tz) || (prevZ != tz && tz > prevZ))
		{
			return new Location(x, y, z);
		}
		return new Location(tx, ty, tz);
	}
	
	public Location moveDoorCheck(Creature creature, int tx, int ty, int tz, Reflection ref)
	{
		if (creature == null)
		{
			return new Location(tx, ty, tz);
		}
		final int geoX = getGeoX(creature.getX());
		final int geoY = getGeoY(creature.getY());
		final int z = getNearestZ(geoX, geoY, creature.getZ());
		final int tGeoX = getGeoX(tx);
		final int tGeoY = getGeoY(ty);
		tz = getNearestZ(tGeoX, tGeoY, tz);
		
		final List<Location> path = new ArrayList<>();
		
		final var pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = z;
		
		final var doors = creature.getDoorsAround(ref, Math.max(Math.hypot(tx - creature.getX(), ty - creature.getY()), 0));
		while (pointIter.next())
		{
			final int curX = pointIter.x();
			final int curY = pointIter.y();
			final int curZ = getNearestZ(curX, curY, prevZ);
			if (hasGeoPos(prevX, prevY))
			{
				final int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				final var isBlockDoor = creature.isInFrontDoor(doors, getWorldX(prevX), getWorldY(prevY), prevZ, getWorldX(curX), getWorldY(curY), curZ, ref);
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, nswe) || isBlockDoor)
				{
					if (path.size() >= 2)
					{
						final var location = path.get((path.size() - (isBlockDoor ? 2 : 1)));
						path.clear();
						return new Location(getWorldX(location.getX()), getWorldY(location.getY()), location.getZ());
					}
					return creature.getLocation();
				}
				prevX = curX;
				prevY = curY;
				prevZ = curZ;
				path.add(new Location(prevX, prevY, prevZ));
			}
		}
		path.clear();
		if (hasGeoPos(prevX, prevY) && (prevZ != tz) || (prevZ != tz && tz > prevZ))
		{
			return creature.getLocation();
		}
		return new Location(tx, ty, tz);
	}
	
	public Location moveWaterCheck(Creature actor, int tx, int ty, int tz, Reflection ref, int[] limits)
	{
		return actor != null && actor.isInFrontDoor(tx, ty, tz, ref) ? actor.getLocation() : moveWaterCheck(getGeoX(actor.getX()), getGeoY(actor.getY()), actor.getZ(), getGeoX(tx), getGeoY(ty), tz, ref, limits[0], limits[1]);
	}
	
	private Location moveWaterCheck(int x, int y, int z, int tx, int ty, int tz, Reflection ref, int minZ, int maxZ)
	{
		final List<Location> path = new ArrayList<>();
		int dx = tx - x;
		int dy = ty - y;
		final int dz = tz - z;
		final int inc_x = sign(dx);
		final int inc_y = sign(dy);
		dx = Math.abs(dx);
		dy = Math.abs(dy);
		if (dx + dy == 0)
		{
			return new Location(getWorldX(x), getWorldY(y), z);
		}
		final float inc_z_for_x = dx == 0 ? 0 : dz / dx;
		final float inc_z_for_y = dy == 0 ? 0 : dz / dy;
		float next_x = x;
		float next_y = y;
		float next_z = z;
		if (dx >= dy)
		{
			final int delta_A = 2 * dy;
			int d = delta_A - dx;
			final int delta_B = delta_A - 2 * dx;
			for (int i = 0; i < dx; i++)
			{
				x = (int) next_x;
				y = (int) next_y;
				z = (int) next_z;
				if (d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					next_z += inc_z_for_x;
					next_y += inc_y;
					next_z += inc_z_for_y;
				}
				else
				{
					d += delta_A;
					next_x += inc_x;
					next_z += inc_z_for_x;
				}
				
				final int nswe = GeoUtils.computeNswe(x, y, (int) next_x, (int) next_y);
				final int beeCurGeoZ = getNearestZ(x, y, z);
				if (next_z < minZ || next_z >= maxZ || (!checkNearestNsweAntiCornerCut(x, y, z, nswe) && Math.abs(next_z - beeCurGeoZ) <= 50))
				{
					if (path.size() >= 1)
					{
						final var location = path.get((path.size() - 1));
						path.clear();
						return new Location(getWorldX(location.getX()), getWorldY(location.getY()), location.getZ());
					}
					return new Location(getWorldX(x), getWorldY(y), z);
				}
				path.add(new Location((int) next_x, (int) next_y, (int) next_z));
			}
		}
		else
		{
			final int delta_A = 2 * dx;
			int d = delta_A - dy;
			final int delta_B = delta_A - 2 * dy;
			for (int i = 0; i < dy; i++)
			{
				x = (int) next_x;
				y = (int) next_y;
				z = (int) next_z;
				if (d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					next_z += inc_z_for_x;
					next_y += inc_y;
					next_z += inc_z_for_y;
				}
				else
				{
					d += delta_A;
					next_y += inc_y;
					next_z += inc_z_for_y;
				}
				
				final int nswe = GeoUtils.computeNswe(x, y, (int) next_x, (int) next_y);
				final int beeCurGeoZ = getNearestZ(x, y, z);
				if (next_z < minZ || next_z >= maxZ || (!checkNearestNsweAntiCornerCut(x, y, z, nswe) && Math.abs(next_z - beeCurGeoZ) <= 50))
				{
					if (path.size() >= 1)
					{
						final var location = path.get((path.size() - 1));
						path.clear();
						return new Location(getWorldX(location.getX()), getWorldY(location.getY()), location.getZ());
					}
					return new Location(getWorldX(x), getWorldY(y), z);
				}
				path.add(new Location((int) next_x, (int) next_y, (int) next_z));
			}
		}
		path.clear();
		return new Location(getWorldX((int) next_x), getWorldY((int) next_y), (int) next_z);
	}

	public Location moveCheck(Creature creature, Location startLoc, Location endLoc, Reflection ref, boolean returnPrev)
	{
		return moveCheck(creature, startLoc.getX(), startLoc.getY(), startLoc.getZ(), endLoc.getX(), endLoc.getY(), endLoc.getZ(), ref, false);
	}

	public boolean canMoveToCoord(GameObject creature, int fromX, int fromY, int fromZ, int toX, int toY, int toZ, Reflection ref, boolean checkDoors)
	{
		if (checkDoors && creature != null && (!Config.ALLOW_DOOR_VALIDATE || !creature.isPlayable()))
		{
			if (creature.isInFrontDoor(toX, toY, toZ, ref))
			{
				return false;
			}
		}
		
		final int geoX = getGeoX(fromX);
		final int geoY = getGeoY(fromY);
		fromZ = getNearestZ(geoX, geoY, fromZ);
		final int tGeoX = getGeoX(toX);
		final int tGeoY = getGeoY(toY);
		toZ = getNearestZ(tGeoX, tGeoY, toZ);

		final LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		double prevZ = fromZ;

		while (pointIter.next())
		{
			final int curX = pointIter.x();
			final int curY = pointIter.y();
			final int curZ = getNearestZ(curX, curY, prevZ);
			if (hasGeoPos(prevX, prevY))
			{
				final int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, nswe))
				{
					return false;
				}
			}

			prevX = curX;
			prevY = curY;
			prevZ = curZ;
		}

		if (hasGeoPos(prevX, prevY) && (prevZ != toZ) || (prevZ != toZ && toZ > prevZ))
		{
			return false;
		}
		return true;
	}

	public boolean canMoveToCoord(GameObject from, int toX, int toY, int toZ)
	{
		return canMoveToCoord(from, from.getX(), from.getY(), from.getZ(), toX, toY, toZ, from.getReflection(), false);
	}

	public boolean canMoveToCoord(GameObject from, GameObject to)
	{
		return canMoveToCoord(from, to.getX(), to.getY(), to.getZ());
	}

	public boolean hasGeo(double x, double y)
	{
		return hasGeoPos(getGeoX(x), getGeoY(y));
	}
	
	public boolean isNSWEAll(Location loc)
	{
		return isNSWEAll(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public boolean isNSWEAll(Location loc, int distance)
	{
		return isNSWEAll(loc.getX(), loc.getY(), loc.getZ(), distance);
	}
	
	public boolean isNSWEAll(int worldX, int worldY, int worldZ)
	{
		return isNSWEAll(worldX, worldY, worldZ, 40);
	}
	
	public boolean isNSWEAll(int worldX, int worldY, int worldZ, int distance)
	{
		final int geoX = getGeoX(worldX);
		final int geoY = getGeoY(worldY);
		final int cells = Math.max(0, distance - 8) / 16;
		
		for (int ix = -cells; ix <= cells; ix++)
		{
			for (int iy = -cells; iy <= cells; iy++)
			{
				final int gx = geoX + ix;
				final int gy = geoY + iy;
				if (NgetNearestNSWE(gx, gy, worldZ) != Cell.NSWE_ALL)
				{
					return false;
				}
			}
		}
		return true;
	}
	
	private byte sign(int x)
	{
		if (x >= 0)
		{
			return +1;
		}
		return -1;
	}

	public static GeoEngine getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static final class SingletonHolder
	{
		private final static GeoEngine INSTANCE = new GeoEngine();
	}
}
