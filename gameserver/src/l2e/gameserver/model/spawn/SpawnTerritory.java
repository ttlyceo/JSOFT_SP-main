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
package l2e.gameserver.model.spawn;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.geometry.Point2D;
import l2e.commons.geometry.Point3D;
import l2e.commons.geometry.Shape;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.ExShowTerritory;

public class SpawnTerritory implements Shape, SpawnRange
{
	protected static final Logger _log = LoggerFactory.getLogger(SpawnTerritory.class);
	
	protected final Point3D max = new Point3D();
	protected final Point3D min = new Point3D();
	
	private final List<Shape> include = new ArrayList<>(1);
	private final List<Shape> exclude = new ArrayList<>(1);
	
	public SpawnTerritory()
	{
	}

	public SpawnTerritory add(Shape shape)
	{
		if (include.isEmpty())
		{
			max._x = shape.getXmax();
			max._y = shape.getYmax();
			max._z = shape.getZmax();
			min._x = shape.getXmin();
			min._y = shape.getYmin();
			min._z = shape.getZmin();
		}
		else
		{
			max._x = Math.max(max.getX(), shape.getXmax());
			max._y = Math.max(max.getY(), shape.getYmax());
			max._z = Math.max(max.getZ(), shape.getZmax());
			min._x = Math.min(min.getX(), shape.getXmin());
			min._y = Math.min(min.getY(), shape.getYmin());
			min._z = Math.min(min.getZ(), shape.getZmin());
		}
		
		include.add(shape);
		return this;
	}
	
	public SpawnTerritory addBanned(Shape shape)
	{
		exclude.add(shape);
		return this;
	}
	
	public List<Shape> getTerritories()
	{
		return include;
	}

	public List<Shape> getBannedTerritories()
	{
		return exclude;
	}

	@Override
	public boolean isInside(int x, int y)
	{
		for (final Shape shape : include)
		{
			if (shape.isInside(x, y))
			{
				return !isExcluded(x, y);
			}
		}
		return false;
	}
	
	@Override
	public boolean isInside(int x, int y, int z)
	{
		if (x < min.getX() || x > max.getX() || y < min.getY() || y > max.getY() || z < min.getZ() || z > max.getZ())
		{
			return false;
		}
		
		for (final Shape shape : include)
		{
			if (shape.isInside(x, y, z))
			{
				return !isExcluded(x, y, z);
			}
		}
		return false;
	}
	
	public boolean isExcluded(int x, int y)
	{
		for (final Shape shape : exclude)
		{
			if (shape.isInside(x, y))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isExcluded(int x, int y, int z)
	{
		for (final Shape shape : exclude)
		{
			if (shape.isInside(x, y, z))
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int getXmax()
	{
		return max.getX();
	}
	
	@Override
	public int getXmin()
	{
		return min.getX();
	}
	
	@Override
	public int getYmax()
	{
		return max.getY();
	}
	
	@Override
	public int getYmin()
	{
		return min.getY();
	}
	
	@Override
	public int getZmax()
	{
		return max.getZ();
	}
	
	@Override
	public int getZmin()
	{
		return min.getZ();
	}
	
	public static Location getRandomLoc(SpawnTerritory territory, boolean fly)
	{
		final Location pos = new Location();
		final var territories = territory.getTerritories();

		for (int i = 0; i <= 100; i++)
		{
			final var shape = territories.get(Rnd.get(territories.size()));
			
			pos.setX(Rnd.get(shape.getXmin(), shape.getXmax()));
			pos.setY(Rnd.get(shape.getYmin(), shape.getYmax()));
			pos.setZ(shape.getZmin() + (shape.getZmax() - shape.getZmin()) / 2);
			int minZ = Math.min(shape.getZmin(), shape.getZmax());
			int maxZ = Math.max(shape.getZmin(), shape.getZmax());
			if (territory.isInside(pos.getX(), pos.getY()))
			{
				if (fly)
				{
					pos.setZ(Rnd.get(minZ, maxZ));
					break;
				}
				
				if (minZ == maxZ)
				{
					minZ -= 50;
					maxZ += 50;
				}
				
				if (!Config.GEODATA)
				{
					break;
				}
				
				final int tempz = GeoEngine.getInstance().getHeight(pos.getX(), pos.getY(), pos.getZ());
				if (tempz < shape.getZmin() || tempz >= shape.getZmax())
				{
					continue;
				}
				pos.setZ(tempz);
				
				if (!GeoEngine.getInstance().isNSWEAll(pos.getX(), pos.getY(), tempz))
				{
					continue;
				}
				pos.setHeading(-1);
				return pos;
			}
			
			if (i == 100 && Config.DEBUG_SPAWN)
			{
				pos.setZ(GeoEngine.getInstance().getHeight(pos.getX(), pos.getY(), maxZ));
				_log.warn("SpawnTerritory: Problem to found correct position for npc. Final location: " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
			}
		}
		pos.setHeading(-1);
		return pos;
	}
	
	@Override
	public Location getRandomLoc(boolean fly)
	{
		return getRandomLoc(this, fly);
	}
	
	public void setVisuality(Player player)
	{
		for (final var shape : include)
		{
			player.sendPacket(new ExShowTerritory(shape));
		}
		for (final var shape : exclude)
		{
			player.sendPacket(new ExShowTerritory(shape));
		}
	}
	
	@Override
	public Point2D[] getPoints()
	{
		final List<Point2D> points = new ArrayList<>();
		for (final var shape : include)
		{
			for (final var point : shape.getPoints())
			{
				points.add(point);
			}
		}
		for (final var shape : exclude)
		{
			for (final var point : shape.getPoints())
			{
				points.add(point);
			}
		}
		return points.toArray(new Point2D[points.size()]);
	}
}