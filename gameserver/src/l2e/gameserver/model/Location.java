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
package l2e.gameserver.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.PositionUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.interfaces.ILocational;
import l2e.gameserver.model.interfaces.IPositionable;
import l2e.gameserver.model.spawn.SpawnRange;

public class Location implements IPositionable, SpawnRange
{
	private static final Logger _log = LoggerFactory.getLogger(Location.class);
	
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	
	public Location()
	{
		this(0, 0, 0, -1);
	}

	public Location(int x, int y, int z)
	{
		this(x, y, z, -1);
	}
	
	public Location(GameObject obj)
	{
		this(obj.getX(), obj.getY(), obj.getZ(), obj.getHeading());
	}
	
	public Location(int x, int y, int z, int heading)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
	}

	@Override
	public int getX()
	{
		return _x;
	}

	@Override
	public void setX(int x)
	{
		_x = x;
	}

	@Override
	public int getY()
	{
		return _y;
	}

	@Override
	public void setY(int y)
	{
		_y = y;
	}

	@Override
	public int getZ()
	{
		return _z;
	}

	@Override
	public void setZ(int z)
	{
		_z = z;
	}

	@Override
	public boolean setXYZ(int x, int y, int z)
	{
		setX(x);
		setY(y);
		setZ(z);
		return true;
	}
	
	@Override
	public boolean setXYZ(ILocational loc)
	{
		return setXYZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	@Override
	public int getHeading()
	{
		return _heading;
	}

	@Override
	public void setHeading(int heading)
	{
		_heading = heading;
	}

	@Override
	public IPositionable getLocation()
	{
		return this;
	}

	@Override
	public boolean setLocation(Location loc)
	{
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
		_heading = loc.getHeading();
		return true;
	}

	public void set(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	
	public Location set(int x, int y, int z, int h)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = h;
		return this;
	}

	public Location set(Location loc)
	{
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
		_heading = loc.getHeading();
		return this;
	}

	public Location setH(int h)
	{
		_heading = h;
		return this;
	}

	public Location geo2world()
	{
		_x = (_x << 4) + World.MAP_MIN_X + 8;
		_y = (_y << 4) + World.MAP_MIN_Y + 8;
		return this;
	}

	public Location world2geo()
	{
		_x = (_x - World.MAP_MIN_X) >> 4;
		_y = (_y - World.MAP_MIN_Y) >> 4;
		return this;
	}

	public Location(String s) throws IllegalArgumentException
	{
		_heading = 0;
		final String xyzh[] = s.replaceAll(",", " ").replaceAll(";", " ").replaceAll("  ", " ").trim().split(" ");
		if (xyzh.length < 3)
		{
			throw new IllegalArgumentException((new StringBuilder()).append("Can't parse location from string: ").append(s).toString());
		}
		_x = Integer.parseInt(xyzh[0]);
		_y = Integer.parseInt(xyzh[1]);
		_z = Integer.parseInt(xyzh[2]);
		_heading = xyzh.length >= 4 ? Integer.parseInt(xyzh[3]) : 0;
		return;
	}

	public Location(int locX, int locY, int locZ, boolean geo2world)
	{
		if (geo2world)
		{
			_x = (locX << 4) + World.MAP_MIN_X + 8;
			_y = (locY << 4) + World.MAP_MIN_Y + 8;
		}
		else
		{
			_x = locX;
			_y = locY;
		}
		_z = locZ;
		_heading = 0;
	}
	
	public static Location coordsRandomize(Location loc, int radiusmin, int radiusmax)
	{
		return coordsRandomize(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), radiusmin, radiusmax);
	}

	public static Location coordsRandomize(int x, int y, int z, int heading, int radiusmin, int radiusmax)
	{
		if ((radiusmax == 0) || (radiusmax < radiusmin))
		{
			return new Location(x, y, z, heading);
		}
		final int radius = Rnd.get(radiusmin, radiusmax);
		final double angle = Rnd.nextDouble() * 2 * Math.PI;
		return new Location((int) (x + (radius * Math.cos(angle))), (int) (y + (radius * Math.sin(angle))), z, heading);
	}

	public Location rnd(Creature creature, int min, int max, boolean change)
	{
		Location loc = coordsRandomize(this, min, max);
		if (Config.GEODATA)
		{
			loc = GeoEngine.getInstance().moveCheck(creature, _x, _y, _z, loc.getX(), loc.getY(), _z, ReflectionManager.DEFAULT);
		}

		if (change)
		{
			_x = loc.getX();
			_y = loc.getY();
			_z = loc.getZ();
			return this;
		}
		return loc;
	}

	public Location correctGeoZ()
	{
		_z = GeoEngine.getInstance().getSpawnHeight(_x, _y, _z);
		return this;
	}
	
	public Location correctZ()
	{
		_z = GeoEngine.getInstance().getHeight(_x, _y, _z);
		return this;
	}
	
	@Override
	public Location clone()
	{
		return new Location(_x, _y, _z, _heading);
	}
	
	@Override
	public Location getRandomLoc(boolean fly)
	{
		return this;
	}
	
	public static Location parseLoc(String s) throws IllegalArgumentException
	{
		final String[] xyzh = s.split("[\\s,;]+");
		if (xyzh.length < 3)
		{
			throw new IllegalArgumentException("Can't parse location from string: " + s);
		}
		final int x = Integer.parseInt(xyzh[0]);
		final int y = Integer.parseInt(xyzh[1]);
		final int z = Integer.parseInt(xyzh[2]);
		final int h = xyzh.length < 4 ? -1 : Integer.parseInt(xyzh[3]);
		return new Location(x, y, z, h);
	}
	
	public static Location findPointToStay(Location loc, int radius, boolean applyDefault)
	{
		return findPointToStay(loc.getX(), loc.getY(), loc.getZ(), 0, radius, applyDefault);
	}
	
	public static Location findPointToStay(Location loc, int radiusmin, int radiusmax, boolean applyDefault)
	{
		return findPointToStay(loc.getX(), loc.getY(), loc.getZ(), radiusmin, radiusmax, applyDefault);
	}

	public static Location findPointToStay(GameObject obj, Location loc, int radiusmin, int radiusmax, boolean applyDefault)
	{
		return findPointToStay(loc.getX(), loc.getY(), loc.getZ(), radiusmin, radiusmax, applyDefault);
	}
	
	public static Location findPointToStay(GameObject obj, int radiusmin, int radiusmax, boolean applyDefault)
	{
		return findPointToStay(obj, obj.getLocation(), radiusmin, radiusmax, applyDefault);
	}
	
	public static Location findPointToStay(GameObject obj, int radius, boolean applyDefault)
	{
		return findPointToStay(obj, 0, radius, applyDefault);
	}

	public static Location findPointToStay(int x, int y, int z, int radiusmin, int radiusmax, boolean applyDefault)
	{
		Location pos;
		int tempz;
		for (int i = 0; i < 100; i++)
		{
			pos = Location.coordsRandomize(x, y, z, 0, radiusmin, radiusmax);
			tempz = GeoEngine.getInstance().getHeight(pos.getX(), pos.getY(), pos.getZ());
			if (Math.abs(pos.getZ() - tempz) < 200 && GeoEngine.getInstance().isNSWEAll(pos.getX(), pos.getY(), tempz))
			{
				return new Location(pos.getX(), pos.getY(), tempz);
			}
		}
		
		if (applyDefault)
		{
			if (Config.DEBUG_SPAWN)
			{
				_log.warn("Location: Problem to found correct position for npc. Final location: " + x + " " + y + " " + z);
			}
			return new Location(x, y, z);
		}
		return null;
	}
	
	public static Location findPointToStayPet(Player activeChar, int radiusmin, int radiusmax)
	{
		for (int i = 0; i < radiusmax; ++i)
		{
			if (radiusmin > i)
			{
				radiusmin = radiusmin - i;
			}
			else
			{
				radiusmin = 0;
			}
			
			final Location pos = Location.coordsRandomize(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 0, radiusmin, radiusmax - i);
			final int tempz = GeoEngine.getInstance().getHeight(pos.getX(), pos.getY(), pos.getZ());
			if (Math.abs(pos.getZ() - tempz) >= 50 || !GeoEngine.getInstance().isNSWEAll(pos.getX(), pos.getY(), tempz))
			{
				continue;
			}
			if (!GeoEngine.getInstance().canSeeTarget(activeChar, pos) || !GeoEngine.getInstance().canMoveToCoord(activeChar, activeChar.getX(), activeChar.getY(), activeChar.getZ(), pos.getX(), pos.getY(), pos.getZ(), activeChar.getReflection(), false))
			{
				continue;
			}
			pos.setZ(tempz);
			return pos;
		}
		return activeChar.getLocation();
	}
	
	public static Location findNearest(Creature creature, Location[] locs)
	{
		Location defloc = null;
		for (final Location loc : locs)
		{
			if (defloc == null)
			{
				defloc = loc;
			}
			else if (creature.getDistance(loc) < creature.getDistance(defloc))
			{
				defloc = loc;
			}
		}
		return defloc;
	}

	public static Location findAroundPosition(GameObject obj, int radius)
	{
		return findAroundPosition(obj, 0, radius);
	}

	public static Location findAroundPosition(GameObject obj, int radiusmin, int radiusmax)
	{
		return findAroundPosition(obj, obj.getLocation(), radiusmin, radiusmax);
	}

	public static Location findAroundPosition(GameObject obj, Location loc, int radiusmin, int radiusmax)
	{
		return findAroundPosition(loc.getX(), loc.getY(), loc.getZ(), radiusmin, radiusmax, obj.getReflection());
	}

	public static Location findAroundPosition(int x, int y, int z, int radiusmin, int radiusmax, Reflection ref)
	{
		Location pos;
		int tempz;
		for (int i = 0; i < 100; i++)
		{
			pos = Location.coordsRandomize(x, y, z, 0, radiusmin, radiusmax);
			tempz = GeoEngine.getInstance().getHeight(pos.getX(), pos.getY(), pos.getZ());
			if (GeoEngine.getInstance().canMoveToCoord(null, x, y, z, pos.getX(), pos.getY(), tempz, ref, false) && GeoEngine.getInstance().canMoveToCoord(null, pos.getX(), pos.getY(), tempz, x, y, z, ref, false))
			{
				pos._z = tempz;
				return pos;
			}
		}
		return new Location(x, y, z);
	}
	
	public static Location findFrontPosition(GameObject obj, GameObject obj2, int radiusmin, int radiusmax)
	{
		if (radiusmax == 0 || radiusmax < radiusmin)
		{
			return new Location(obj);
		}
		
		final double collision = obj.getColRadius() + obj2.getColRadius();
		int randomRadius, randomAngle, tempz;
		int minangle = 0;
		int maxangle = 360;
		
		if (!obj.equals(obj2))
		{
			final double angle = PositionUtils.calculateAngleFrom(obj, obj2);
			minangle = (int) angle - 45;
			maxangle = (int) angle + 45;
		}
		
		final Location pos = new Location();
		for (int i = 0; i < 100; i++)
		{
			randomRadius = Rnd.get(radiusmin, radiusmax);
			randomAngle = Rnd.get(minangle, maxangle);
			pos._x = obj.getX() + (int) ((collision + randomRadius) * Math.cos(Math.toRadians(randomAngle)));
			pos._y = obj.getY() + (int) ((collision + randomRadius) * Math.sin(Math.toRadians(randomAngle)));
			pos._z = obj.getZ();
			tempz = GeoEngine.getInstance().getHeight(pos._x, pos._y, pos._z);
			if (Math.abs(pos._z - tempz) < 200 && GeoEngine.getInstance().isNSWEAll(pos._x, pos._y, tempz))
			{
				pos._z = tempz;
				if (!obj.equals(obj2))
				{
					pos._heading = PositionUtils.getHeadingTo(pos, obj2.getLocation());
				}
				else
				{
					pos._heading = obj.getHeading();
				}
				return pos;
			}
		}
		return new Location(obj);
	}
	
	public double distance(Location loc)
	{
		return distance(loc.getX(), loc.getY());
	}
	
	public double distance(int x, int y)
	{
		final long dx = _x - x;
		final long dy = _y - y;
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public double distance3D(Location loc)
	{
		return distance3D(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public double distance3D(int x, int y, int z)
	{
		final long dx = _x - x;
		final long dy = _y - y;
		final long dz = _z - z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	public static double calculateAngleFrom(GameObject obj1, GameObject obj2)
	{
		return calculateAngleFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}
	
	public static double calculateAngleFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
		{
			angleTarget = 360 + angleTarget;
		}
		return angleTarget;
	}
	
	public long getXYDeltaSq(int x, int y)
	{
		final long dx = x - getX();
		final long dy = y - getY();
		return dx * dx + dy * dy;
	}
	
	public long getXYDeltaSq(Location loc)
	{
		return getXYDeltaSq(loc.getX(), loc.getY());
	}
	
	public long getZDeltaSq(int z)
	{
		final long dz = z - getZ();
		return dz * dz;
	}
	
	public long getZDeltaSq(Location loc)
	{
		return getZDeltaSq(loc.getZ());
	}
	
	public long getXYZDeltaSq(int x, int y, int z)
	{
		return getXYDeltaSq(x, y) + getZDeltaSq(z);
	}
	
	public long getXYZDeltaSq(Location loc)
	{
		return getXYZDeltaSq(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public int getDistance(int x, int y)
	{
		return (int) Math.sqrt(getXYDeltaSq(x, y));
	}
	
	public int getDistance(int x, int y, int z)
	{
		return (int) Math.sqrt(getXYZDeltaSq(x, y, z));
	}
	
	public int getDistance(Location loc)
	{
		return getDistance(loc.getX(), loc.getY());
	}
	
	public int getDistance3D(Location loc)
	{
		return getDistance(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public boolean isInRangeSq(Location loc, long range)
	{
		return getXYDeltaSq(loc) <= range;
	}
	
	public boolean isInRange(Location loc, int range)
	{
		return isInRangeSq(loc, (long) range * range);
	}
	
	public boolean isInRangeZ(Location loc, int range)
	{
		return isInRangeZSq(loc, (long) range * range);
	}
	
	public boolean isInRangeZSq(Location loc, long range)
	{
		return getXYZDeltaSq(loc) <= range;
	}
	
	public long getSqDistance(int x, int y)
	{
		return getXYDeltaSq(x, y);
	}
	
	public long getSqDistance(Location loc)
	{
		return getXYDeltaSq(loc);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if ((obj != null) && (obj instanceof Location))
		{
			final Location loc = (Location) obj;
			return (getX() == loc.getX()) && (getY() == loc.getY()) && (getZ() == loc.getZ()) && (getHeading() == loc.getHeading());
		}
		return false;
	}
	
	public boolean equals(int x, int y, int z)
	{
		return (getX() == x) && (getY() == y) && (getZ() == z);
	}
	
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] X: " + getX() + " Y: " + getY() + " Z: " + getZ() + " Heading: " + _heading;
	}
}