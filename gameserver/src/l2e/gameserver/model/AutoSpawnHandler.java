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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.spawn.Spawner;

public class AutoSpawnHandler extends LoggerObject
{
	private final Map<Integer, AutoSpawnInstance> _registeredSpawns = new ConcurrentHashMap<>();
	private final Map<Integer, ScheduledFuture<?>> _runningSpawns = new ConcurrentHashMap<>();

	protected boolean _activeState = true;
	
	protected AutoSpawnHandler()
	{
		restoreSpawnData();
		info("Loaded " + _registeredSpawns.size() + " AutoSpawnHandlers.");
	}

	public static AutoSpawnHandler getInstance()
	{
		return SingletonHolder._instance;
	}

	public void reload()
	{
		_runningSpawns.values().stream().filter(sf -> (sf != null)).forEach(sf -> sf.cancel(true));
		_registeredSpawns.values().stream().filter(a -> (a != null)).forEach(a -> removeSpawn(a));
		_registeredSpawns.clear();
		_runningSpawns.clear();
		restoreSpawnData();
	}

	private void restoreSpawnData()
	{
		Connection con = null;
		Statement statement = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rs = statement.executeQuery("SELECT * FROM random_spawn ORDER BY groupId ASC");
			ps = con.prepareStatement("SELECT * FROM random_spawn_loc WHERE groupId=?");
			while (rs.next())
			{
				final var spawnInst = registerSpawn(rs.getInt("npcId"), rs.getInt("count"), rs.getInt("initialDelay"), rs.getInt("respawnDelay"), rs.getInt("despawnDelay"), rs.getBoolean("broadcastSpawn"), rs.getBoolean("randomSpawn"));
				ps.setInt(1, rs.getInt("groupId"));
				final var rs2 = ps.executeQuery();
				ps.clearParameters();
				
				while (rs2.next())
				{
					spawnInst.addSpawnLocation(rs2.getInt("x"), rs2.getInt("y"), rs2.getInt("z"), rs2.getInt("heading"));
				}
				rs2.close();
			}
			ps.close();
		}
		catch (final Exception e)
		{
			warn("Could not restore spawn data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	private AutoSpawnInstance registerSpawn(int npcId, int count, int[][] spawnPoints, int initialDelay, int respawnDelay, int despawnDelay, boolean broadcastSpawn, boolean randomSpawn)
	{
		initialDelay = initialDelay < 0 ? 30000 : initialDelay;
		respawnDelay = respawnDelay < 0 ? 3600000 : respawnDelay;
		despawnDelay = despawnDelay < 0 ? 3600000 : despawnDelay;
		final var newSpawn = new AutoSpawnInstance(npcId, count, initialDelay, respawnDelay, despawnDelay, broadcastSpawn, randomSpawn);
		if (spawnPoints != null)
		{
			for (final var spawnPoint : spawnPoints)
			{
				newSpawn.addSpawnLocation(spawnPoint);
			}
		}
		final int newId = IdFactory.getInstance().getNextId();
		newSpawn._objectId = newId;
		_registeredSpawns.put(newId, newSpawn);
		setSpawnActive(newSpawn, true);
		return newSpawn;
	}

	private AutoSpawnInstance registerSpawn(int npcId, int count, int initialDelay, int respawnDelay, int despawnDelay, boolean broadcastSpawn, boolean randomSpawn)
	{
		return registerSpawn(npcId, count, null, initialDelay, respawnDelay, despawnDelay, broadcastSpawn, randomSpawn);
	}

	public boolean removeSpawn(AutoSpawnInstance spawnInst)
	{
		if (!isSpawnRegistered(spawnInst))
		{
			return false;
		}

		try
		{
			_registeredSpawns.remove(spawnInst.getId());
			final var respawnTask = _runningSpawns.remove(spawnInst._objectId);
			respawnTask.cancel(false);
		}
		catch (final Exception e)
		{
			if (Config.DEBUG)
			{
				warn("Could not auto spawn for NPC ID " + spawnInst._npcId + " (Object ID = " + spawnInst._objectId + "): " + e.getMessage(), e);
			}
			return false;
		}

		return true;
	}

	public void removeSpawn(int objectId)
	{
		removeSpawn(_registeredSpawns.get(objectId));
	}

	public void setSpawnActive(AutoSpawnInstance spawnInst, boolean isActive)
	{
		if (spawnInst == null)
		{
			return;
		}

		final int objectId = spawnInst._objectId;

		if (isSpawnRegistered(objectId))
		{
			ScheduledFuture<?> spawnTask = null;
			if (isActive)
			{
				final AutoSpawner rs = new AutoSpawner(objectId);

				if (spawnInst._desDelay > 0)
				{
					spawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(rs, spawnInst._initDelay, spawnInst._resDelay);
				}
				else
				{
					spawnTask = ThreadPoolManager.getInstance().schedule(rs, spawnInst._initDelay);
				}

				_runningSpawns.put(objectId, spawnTask);
			}
			else
			{
				final AutoDespawner rd = new AutoDespawner(objectId);
				spawnTask = _runningSpawns.remove(objectId);
				if (spawnTask != null)
				{
					spawnTask.cancel(false);
				}
				ThreadPoolManager.getInstance().schedule(rd, 0);
			}
			spawnInst.setSpawnActive(isActive);
		}
	}

	public void setAllActive(boolean isActive)
	{
		if (_activeState == isActive)
		{
			return;
		}
		_registeredSpawns.values().stream().filter(s -> (s != null)).forEach(s -> setSpawnActive(s, isActive));
		_activeState = isActive;
	}

	public final long getTimeToNextSpawn(AutoSpawnInstance spawnInst)
	{
		final int objectId = spawnInst.getObjectId();
		if (!isSpawnRegistered(objectId))
		{
			return -1;
		}
		return _runningSpawns.get(objectId).getDelay(TimeUnit.MILLISECONDS);
	}

	public final AutoSpawnInstance getAutoSpawnInstance(int id, boolean isObjectId)
	{
		if (isObjectId)
		{
			if (isSpawnRegistered(id))
			{
				return _registeredSpawns.get(id);
			}
		}
		else
		{
			for (final var spawnInst : _registeredSpawns.values())
			{
				if (spawnInst.getId() == id)
				{
					return spawnInst;
				}
			}
		}
		return null;
	}

	public List<AutoSpawnInstance> getAutoSpawnInstances(int npcId)
	{
		final List<AutoSpawnInstance> result = new LinkedList<>();
		_registeredSpawns.values().stream().filter(s -> (s != null && s.getId() == npcId)).forEach(s -> result.add(s));
		return result;
	}

	public final boolean isSpawnRegistered(int objectId)
	{
		return _registeredSpawns.containsKey(objectId);
	}

	public final boolean isSpawnRegistered(AutoSpawnInstance spawnInst)
	{
		return _registeredSpawns.containsValue(spawnInst);
	}

	private class AutoSpawner implements Runnable
	{
		private final int _objectId;

		protected AutoSpawner(int objectId)
		{
			_objectId = objectId;
		}

		@Override
		public void run()
		{
			try
			{
				final var spawnInst = _registeredSpawns.get(_objectId);
				if (!spawnInst.isSpawnActive())
				{
					return;
				}

				final var locationList = spawnInst.getLocationList();

				if (locationList.length == 0)
				{
					info("No location co-ords specified for spawn instance (Object ID = " + _objectId + ").");
					return;
				}

				final int locationCount = locationList.length;
				int locationIndex = Rnd.nextInt(locationCount);

				if (!spawnInst.isRandomSpawn())
				{
					locationIndex = spawnInst._lastLocIndex + 1;

					if (locationIndex == locationCount)
					{
						locationIndex = 0;
					}

					spawnInst._lastLocIndex = locationIndex;
				}

				final int x = locationList[locationIndex].getX();
				final int y = locationList[locationIndex].getY();
				final int z = locationList[locationIndex].getZ();
				final int heading = locationList[locationIndex].getHeading();

				final var npcTemp = NpcsParser.getInstance().getTemplate(spawnInst.getId());
				if (npcTemp == null)
				{
					warn("Couldnt find NPC id" + spawnInst.getId() + " Try to update your DP");
					return;
				}
				final Spawner newSpawn = new Spawner(npcTemp);

				newSpawn.setX(x);
				newSpawn.setY(y);
				newSpawn.setZ(z);
				if (heading != -1)
				{
					newSpawn.setHeading(heading);
				}
				newSpawn.setAmount(spawnInst.getSpawnCount());
				if (spawnInst._desDelay == 0)
				{
					newSpawn.setRespawnDelay(spawnInst._resDelay);
				}

				SpawnParser.getInstance().addNewSpawn(newSpawn);
				Npc npcInst = null;

				if (spawnInst._spawnCount == 1)
				{
					npcInst = newSpawn.doSpawn();
					npcInst.setXYZ(npcInst.getX(), npcInst.getY(), npcInst.getZ());
					spawnInst.addNpcInstance(npcInst);
				}
				else
				{
					for (int i = 0; i < spawnInst._spawnCount; i++)
					{
						npcInst = newSpawn.doSpawn();
						npcInst.setXYZ(npcInst.getX() + Rnd.nextInt(50), npcInst.getY() + Rnd.nextInt(50), npcInst.getZ());
						spawnInst.addNpcInstance(npcInst);
					}
				}

				if (spawnInst.isBroadcasting() && (npcInst != null))
				{
					final String nearestTown = MapRegionManager.getInstance().getClosestTownName(npcInst);
					Announcements.getInstance().announceToAll("The " + npcInst.getName(null) + " has spawned near " + nearestTown + "!");
				}

				if (spawnInst.getDespawnDelay() > 0)
				{
					final AutoDespawner rd = new AutoDespawner(_objectId);
					ThreadPoolManager.getInstance().schedule(rd, spawnInst.getDespawnDelay() - 1000);
				}
			}
			catch (final Exception e)
			{
				warn("An error occurred while initializing spawn instance (Object ID = " + _objectId + "): " + e.getMessage(), e);
			}
		}
	}

	private class AutoDespawner implements Runnable
	{
		private final int _objectId;

		protected AutoDespawner(int objectId)
		{
			_objectId = objectId;
		}

		@Override
		public void run()
		{
			try
			{
				final var spawnInst = _registeredSpawns.get(_objectId);
				if (spawnInst == null)
				{
					info("No spawn registered for object ID = " + _objectId + ".");
					return;
				}

				for (final var npcInst : spawnInst.getNPCInstanceList())
				{
					if (npcInst == null)
					{
						continue;
					}

					npcInst.deleteMe();
					SpawnParser.getInstance().deleteSpawn(npcInst.getSpawn());
					spawnInst.removeNpcInstance(npcInst);
				}
			}
			catch (final Exception e)
			{
				warn("An error occurred while despawning spawn (Object ID = " + _objectId + "): " + e.getMessage(), e);
			}
		}
	}

	public static class AutoSpawnInstance implements IIdentifiable
	{
		protected int _objectId;
		protected int _spawnIndex;
		protected int _npcId;
		protected int _initDelay;
		protected int _resDelay;
		protected int _desDelay;
		protected int _spawnCount;
		protected int _lastLocIndex = -1;
		private final Queue<Npc> _npcList = new ConcurrentLinkedQueue<>();
		private final List<Location> _locList = new CopyOnWriteArrayList<>();
		private boolean _spawnActive;
		private final boolean _randomSpawn;
		private boolean _broadcastAnnouncement;

		protected AutoSpawnInstance(int npcId, int spawnCount, int initDelay, int respawnDelay, int despawnDelay, boolean broadcastAnnouncement, boolean randomSpawn)
		{
			_npcId = npcId;
			_spawnCount = spawnCount;
			_initDelay = initDelay;
			_resDelay = respawnDelay;
			_desDelay = despawnDelay;
			_broadcastAnnouncement = broadcastAnnouncement;
			_randomSpawn = randomSpawn;
		}

		protected void setSpawnActive(boolean activeValue)
		{
			_spawnActive = activeValue;
		}

		protected boolean addNpcInstance(Npc npcInst)
		{
			return _npcList.add(npcInst);
		}

		protected boolean removeNpcInstance(Npc npcInst)
		{
			return _npcList.remove(npcInst);
		}

		public int getObjectId()
		{
			return _objectId;
		}

		public int getInitialDelay()
		{
			return _initDelay;
		}

		public int getRespawnDelay()
		{
			return _resDelay;
		}

		public int getDespawnDelay()
		{
			return _desDelay;
		}

		@Override
		public int getId()
		{
			return _npcId;
		}

		public int getSpawnCount()
		{
			return _spawnCount;
		}

		public Location[] getLocationList()
		{
			return _locList.toArray(new Location[_locList.size()]);
		}

		public Queue<Npc> getNPCInstanceList()
		{
			return _npcList;
		}

		public List<Spawner> getSpawns()
		{
			final List<Spawner> npcSpawns = new ArrayList<>();
			_npcList.stream().filter(n -> (n != null)).forEach(n -> npcSpawns.add(n.getSpawn()));
			return npcSpawns;
		}

		public boolean isSpawnActive()
		{
			return _spawnActive;
		}

		public boolean isRandomSpawn()
		{
			return _randomSpawn;
		}
		
		public void setBroadcast(boolean val)
		{
			_broadcastAnnouncement = val;
		}

		public boolean isBroadcasting()
		{
			return _broadcastAnnouncement;
		}

		public boolean addSpawnLocation(int x, int y, int z, int heading)
		{
			return _locList.add(new Location(x, y, z, heading));
		}

		public boolean addSpawnLocation(int[] spawnLoc)
		{
			if (spawnLoc.length != 3)
			{
				return false;
			}
			return addSpawnLocation(spawnLoc[0], spawnLoc[1], spawnLoc[2], -1);
		}

		public Location removeSpawnLocation(int locIndex)
		{
			try
			{
				return _locList.remove(locIndex);
			}
			catch (final IndexOutOfBoundsException e)
			{
				return null;
			}
		}
	}

	private static class SingletonHolder
	{
		protected static final AutoSpawnHandler _instance = new AutoSpawnHandler();
	}
}