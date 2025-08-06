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
package l2e.gameserver.instancemanager;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.zone.AbstractZoneSettings;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneRespawn;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.model.zone.form.ZoneCuboid;
import l2e.gameserver.model.zone.form.ZoneCylinder;
import l2e.gameserver.model.zone.form.ZoneNPoly;
import l2e.gameserver.model.zone.type.ArenaZone;
import l2e.gameserver.model.zone.type.OlympiadStadiumZone;
import l2e.gameserver.model.zone.type.RespawnZone;

public class ZoneManager extends DocumentParser
{
	private static final Map<String, AbstractZoneSettings> _settings = new HashMap<>();
	
	private final Map<Class<? extends ZoneType>, Map<Integer, ? extends ZoneType>> _classZones = new HashMap<>();
	private final List<ZoneType> _reflectionZones = new ArrayList<>();
	private int _lastDynamicId = 300000;
	private List<ItemInstance> _debugItems;
	private ZoneType[][][] _zones;
	
	protected ZoneManager()
	{
		load();
	}
	
	public void reload()
	{
		_zones = null;
		for (final Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			for (final ZoneType zone : map.values())
			{
				if (zone != null)
				{
					zone.clearTask();
					if (zone.getSettings() != null)
					{
						_settings.put(zone.getName(), zone.getSettings());
					}
				}
			}
		}
		
		EpicBossManager.getInstance().getZones().clear();
		
		load();
		for (final GameObject obj : GameObjectsStorage.getObjects())
		{
			if (obj instanceof Creature)
			{
				((Creature) obj).revalidateZone(true);
			}
		}
		_settings.clear();
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		NamedNodeMap attrs;
		Node attribute;
		String zoneName;
		int[][] coords;
		int zoneId, minZ, maxZ;
		String zoneType, zoneShape;
		final List<int[]> rs = new ArrayList<>();
		
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				attribute = attrs.getNamedItem("enabled");
				if ((attribute != null) && !Boolean.parseBoolean(attribute.getNodeValue()))
				{
					continue;
				}
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("zone".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						
						attribute = attrs.getNamedItem("id");
						if (attribute != null)
						{
							zoneId = Integer.parseInt(attribute.getNodeValue());
						}
						else
						{
							zoneId = _lastDynamicId++;
						}
						
						attribute = attrs.getNamedItem("name");
						if (attribute != null)
						{
							zoneName = attribute.getNodeValue();
						}
						else
						{
							zoneName = null;
						}
						
						minZ = parseInt(attrs, "minZ");
						maxZ = parseInt(attrs, "maxZ");
						
						zoneType = attrs.getNamedItem("type").getNodeValue();
						zoneShape = attrs.getNamedItem("shape").getNodeValue();
						
						Class<?> newZone = null;
						Constructor<?> zoneConstructor = null;
						ZoneType temp;
						try
						{
							newZone = Class.forName("l2e.gameserver.model.zone.type." + zoneType);
							zoneConstructor = newZone.getConstructor(int.class);
							temp = (ZoneType) zoneConstructor.newInstance(zoneId);
						}
						catch (final Exception e)
						{
							warn("No such zone type: " + zoneType + " in file: " + getCurrentFile().getName());
							continue;
						}
						
						try
						{
							coords = null;
							int[] point;
							rs.clear();
							
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("node".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									point = new int[2];
									point[0] = parseInt(attrs, "X");
									point[1] = parseInt(attrs, "Y");
									rs.add(point);
								}
							}
							
							coords = rs.toArray(new int[rs.size()][2]);
							
							if ((coords == null) || (coords.length == 0))
							{
								warn("missing data for zone: " + zoneId + " XML file: " + getCurrentFile().getName());
								continue;
							}
							
							if (zoneShape.equalsIgnoreCase("Cuboid"))
							{
								if (coords.length == 2)
								{
									temp.setZone(new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ));
								}
								else
								{
									warn("Missing cuboid vertex in sql data for zone: " + zoneId + " in file: " + getCurrentFile().getName());
									continue;
								}
							}
							else if (zoneShape.equalsIgnoreCase("NPoly"))
							{
								if (coords.length > 2)
								{
									final int[] aX = new int[coords.length];
									final int[] aY = new int[coords.length];
									for (int i = 0; i < coords.length; i++)
									{
										aX[i] = coords[i][0];
										aY[i] = coords[i][1];
									}
									temp.setZone(new ZoneNPoly(aX, aY, minZ, maxZ));
								}
								else
								{
									warn("Bad data for zone: " + zoneId + " in file: " + getCurrentFile().getName());
									continue;
								}
							}
							else if (zoneShape.equalsIgnoreCase("Cylinder"))
							{
								attrs = d.getAttributes();
								final int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
								if ((coords.length == 1) && (zoneRad > 0))
								{
									temp.setZone(new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad));
								}
								else
								{
									warn("Bad data for zone: " + zoneId + " in file: " + getCurrentFile().getName());
									continue;
								}
							}
						}
						catch (final Exception e)
						{
							warn("Failed to load zone " + zoneId + " coordinates: " + e.getMessage(), e);
						}
						
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								final String name = attrs.getNamedItem("name").getNodeValue();
								final String val = attrs.getNamedItem("val").getNodeValue();
								
								temp.setParameter(name, val);
							}
							else if ("spawn".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof ZoneRespawn))
							{
								attrs = cd.getAttributes();
								final int spawnX = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
								final int spawnY = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
								final int spawnZ = Integer.parseInt(attrs.getNamedItem("Z").getNodeValue());
								final Node val = attrs.getNamedItem("type");
								((ZoneRespawn) temp).parseLoc(spawnX, spawnY, spawnZ, val == null ? null : val.getNodeValue());
							}
							else if ("race".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof RespawnZone))
							{
								attrs = cd.getAttributes();
								final String race = attrs.getNamedItem("name").getNodeValue();
								final String point = attrs.getNamedItem("point").getNodeValue();
								
								((RespawnZone) temp).addRaceRespawnPoint(race, point);
							}
						}
						if (checkId(zoneId))
						{
							info("Zone (" + zoneId + ") from file: " + getCurrentFile().getName() + " overrides previos definition.");
						}
						
						if ((zoneName != null) && !zoneName.isEmpty())
						{
							temp.setName(zoneName);
						}
						temp.setType(zoneType);
						
						addZone(zoneId, temp);
						if (temp.getReflectionTemplateId() > 0)
						{
							_reflectionZones.add(temp);
						}
						int ax, ay, bx, by;
						for (int x = 0; x < _zones.length; x++)
						{
							for (int y = 0; y < _zones[x].length; y++)
							{
								ax = 11 + x - 20 << 15;
								ay = 10 + y - 18 << 15;
								bx = ax + 32767;
								by = ay + 32767;
								if (temp.getZone().intersectsRectangle(ax, bx, ay, by))
								{
									if (_zones[x][y] == null)
									{
										_zones[x][y] = new ZoneType[]
										{
										        temp
										};
									}
									else
									{
										final ZoneType[] za = new ZoneType[_zones[x][y].length + 1];
										System.arraycopy(_zones[x][y], 0, za, 0, _zones[x][y].length);
										za[za.length - 1] = temp;
										_zones[x][y] = za;
									}
								}
							}
						}
						
						if (temp.getSchedulePattern() != null)
						{
							temp.calcActivationTime();
						}
					}
				}
			}
		}
	}
	
	@Override
	public final void load()
	{
		_classZones.clear();
		_reflectionZones.clear();
		long started = System.currentTimeMillis();
		_zones = new ZoneType[World.WORLD_SIZE_X][World.WORLD_SIZE_Y][];
		parseDirectory("data/stats/regions/zones", false);
		
		started = System.currentTimeMillis() - started;
		info("Loaded " + _classZones.size() + " zone classes and " + getSize() + " zones in " + (started / 1000) + " seconds.");
	}
	
	public int getSize()
	{
		int i = 0;
		for (final Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			i += map.size();
		}
		return i;
	}
	
	public boolean checkId(int id)
	{
		for (final Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
			{
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> void addZone(Integer id, T zone)
	{
		Map<Integer, T> map = (Map<Integer, T>) _classZones.get(zone.getClass());
		if (map == null)
		{
			map = new HashMap<>();
			map.put(id, zone);
			_classZones.put(zone.getClass(), map);
		}
		else
		{
			map.put(id, zone);
		}
	}
	
	public Collection<ZoneType> getAllZones()
	{
		final List<ZoneType> zones = new ArrayList<>();
		for (final Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			if (map != null)
			{
				zones.addAll(map.values());
			}
		}
		return zones;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> Collection<T> getAllZones(Class<T> zoneType)
	{
		return (Collection<T>) _classZones.get(zoneType).values();
	}
	
	public ZoneType getZoneById(int id)
	{
		for (final Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
			{
				return map.get(id);
			}
		}
		return null;
	}
	
	public ZoneType getZoneByZoneId(Player player, ZoneId id)
	{
		final ZoneType[] za = getAllZones(player.getX(), player.getY());
		if (za == null)
		{
			return null;
		}
		
		for (final ZoneType zone : za)
		{
			if (zone != null && zone.isEnabled() && zone.getZoneId().contains(id))
			{
				if (zone.isInsideZone(player.getX(), player.getY(), player.getZ()))
				{
					return zone;
				}
			}
		}
		return null;
	}
	
	public boolean isInsideZone(int id, GameObject object)
	{
		final ZoneType zone = getZoneById(id);
		if (zone != null && zone.isEnabled() && zone.isInsideZone(object.getX(), object.getY(), object.getZ()))
		{
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZoneById(int id, Class<T> zoneType)
	{
		return (T) _classZones.get(zoneType).get(id);
	}
	
	public List<ZoneType> getZones(GameObject object)
	{
		return getZones(object.getX(), object.getY(), object.getZ());
	}
	
	public <T extends ZoneType> T getZone(GameObject object, Class<T> type)
	{
		if (object == null)
		{
			return null;
		}
		final var zone = getZone(object.getX(), object.getY(), object.getZ(), type);
		return zone != null && zone.isEnabled() ? zone : null;
	}
	
	public ZoneType[] getAllZones(int x, int y)
	{
		final int gx = (x - World.MAP_MIN_X) >> 15;
		final int gy = (y - World.MAP_MIN_Y) >> 15;
		if (gx < 0 || gx >= _zones.length || gy < 0 || gy >= _zones[gx].length)
		{
			warn("Wrong world region: " + gx + " " + gy + " (" + x + "," + y + ")");
			return null;
		}
		return _zones[gx][gy];
	}
	
	public List<ZoneType> getZones(int x, int y, int z)
	{
		final ZoneType[] za = getAllZones(x, y);
		if (za == null)
		{
			return null;
		}
		
		final List<ZoneType> temp = new ArrayList<>();
		for (final ZoneType zone : za)
		{
			if (zone != null && zone.isInsideZone(x, y, z))
			{
				temp.add(zone);
			}
		}
		return temp;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZone(int x, int y, int z, Class<T> type)
	{
		final ZoneType[] za = getAllZones(x, y);
		if (za == null)
		{
			return null;
		}
		
		for (final ZoneType zone : za)
		{
			if (zone != null && zone.isEnabled() && zone.isInsideZone(x, y, z) && type.isInstance(zone))
			{
				return (T) zone;
			}
		}
		return null;
	}
	
	public final ArenaZone getArena(Creature creature)
	{
		if (creature == null)
		{
			return null;
		}
		
		final List<ZoneType> zones = getZones(creature.getX(), creature.getY(), creature.getZ());
		if (zones != null && !zones.isEmpty())
		{
			for (final ZoneType temp : zones)
			{
				if (temp != null && temp.isEnabled() && (temp instanceof ArenaZone) && temp.isCharacterInZone(creature))
				{
					return ((ArenaZone) temp);
				}
			}
		}
		return null;
	}
	
	public final OlympiadStadiumZone getOlympiadStadium(Creature creature)
	{
		if (creature == null)
		{
			return null;
		}
		
		final List<ZoneType> zones = getZones(creature.getX(), creature.getY(), creature.getZ());
		if (zones != null && !zones.isEmpty())
		{
			for (final ZoneType temp : zones)
			{
				if (temp != null && temp.isEnabled() && (temp instanceof OlympiadStadiumZone) && temp.isCharacterInZone(creature))
				{
					return ((OlympiadStadiumZone) temp);
				}
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getClosestZone(GameObject obj, Class<T> type)
	{
		T zone = getZone(obj, type);
		if (zone == null)
		{
			double closestdis = Double.MAX_VALUE;
			for (final T temp : (Collection<T>) _classZones.get(type).values())
			{
				final double distance = temp.getDistanceToZone(obj);
				if (distance < closestdis)
				{
					closestdis = distance;
					zone = temp;
				}
			}
		}
		return zone;
	}
	
	public final List<ZoneType> isInsideZone(int x, int y)
	{
		final List<ZoneType> zones = new ArrayList<>();
		for (final ZoneType temp : getAllZones())
		{
			if (temp != null && temp.isEnabled() && temp.isInsideZone(x, y))
			{
				zones.add(temp);
			}
		}
		return zones;
	}
	
	public List<ItemInstance> getDebugItems()
	{
		if (_debugItems == null)
		{
			_debugItems = new ArrayList<>();
		}
		return _debugItems;
	}
	
	public void clearDebugItems()
	{
		if (_debugItems != null)
		{
			final Iterator<ItemInstance> it = _debugItems.iterator();
			while (it.hasNext())
			{
				final ItemInstance item = it.next();
				if (item != null)
				{
					item.decayMe();
				}
				it.remove();
			}
		}
	}
	
	public static AbstractZoneSettings getSettings(String name)
	{
		return _settings.get(name);
	}
	
	public void createZoneReflections()
	{
		int i = 0;
		if (_reflectionZones != null && !_reflectionZones.isEmpty())
		{
			for (final ZoneType z : _reflectionZones)
			{
				if (z != null)
				{
					z.generateReflection();
					i++;
				}
			}
		}
		info("Generate " + i + " reflections for zones.");
	}
	
	public static final ZoneManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ZoneManager _instance = new ZoneManager();
	}
}