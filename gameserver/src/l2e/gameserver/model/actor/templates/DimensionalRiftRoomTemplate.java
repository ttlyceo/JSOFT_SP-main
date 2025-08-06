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
package l2e.gameserver.model.actor.templates;

import java.awt.Polygon;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.DimensionalRiftManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.serverpackets.EarthQuake;

public final class DimensionalRiftRoomTemplate
{
	private final List<Spawner> _roomSpawns;
	private boolean _isBusy;
	private final Shape _s;
	private final int _xMin;
	private final int _xMax;
	private final int _yMin;
	private final int _yMax;
	private final int _zMin;
	private final int _zMax;
	private boolean _isBossRoom = false;
	private final byte _roomType;
	private final byte _roomId;
	private final Location _teleportLoc;
	private Future<?> _spawnTask;

	public DimensionalRiftRoomTemplate(int xMin, int xMax, int yMin, int yMax, int zMin, int zMax, byte roomId, byte roomType, Location teleportLoc, boolean isBossRoom)
	{
		_xMin = (xMin + 128);
		_xMax = (xMax - 128);
		_yMin = (yMin + 128);
		_yMax = (yMax - 128);
		_zMin = zMin;
		_zMax = zMax;
		_s = new Polygon(new int[]
		{
		        xMin, xMax, xMax, xMin
		}, new int[]
		{
		        yMin, yMin, yMax, yMax
		}, 4);
		_roomId = roomId;
		_roomType = roomType;
		_teleportLoc = teleportLoc;
		_isBossRoom = isBossRoom;
		_roomSpawns = new ArrayList<>();
		_isBusy = false;
	}

	public byte getRoomId()
	{
		return _roomId;
	}

	public byte getRoomType()
	{
		return _roomType;
	}
	
	public boolean isBusy()
	{
		return _isBusy;
	}
	
	public void setIsBusy(boolean busy)
	{
		_isBusy = busy;
	}

	public boolean isBossRoom()
	{
		return _isBossRoom;
	}

	public List<Spawner> getSpawns()
	{
		return _roomSpawns;
	}

	public void addSpawn(Spawner spawn)
	{
		_roomSpawns.add(spawn);
	}

	public void telePlayersToOut(Party party)
	{
		if (party == null)
		{
			return;
		}
		
		for (final var player : party.getMembers())
		{
			if (player != null && isInside(player))
			{
				DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
				final var riftQuest = QuestManager.getInstance().getQuest(635);
				if (riftQuest != null)
				{
					final var qs = player.getQuestState(riftQuest.getName());
					if ((qs != null) && qs.isCond(1))
					{
						qs.exitQuest(true, true);
					}
				}
			}
		}
	}

	public void telePlayersToNext(Party party, DimensionalRiftRoomTemplate currentRoom, DimensionalRiftRoomTemplate nextRoom)
	{
		if (party == null)
		{
			return;
		}
		
		final var coords = nextRoom.getTeleportLocation();
		if (coords != null)
		{
			party.getMembers().stream().filter(p -> (p != null && currentRoom.isInside(p))).forEach(p -> p.teleToLocation(coords, true, p.getReflection()));
		}
	}
	
	public void earthquake(Party party)
	{
		if (party == null)
		{
			return;
		}
		party.getMembers().stream().filter(p -> (p != null && isInside(p))).forEach(p -> p.sendPacket(new EarthQuake(p.getX(), p.getY(), p.getZ(), 65, 9)));
	}
	
	public void start(DimensionalRiftTemplate template)
	{
		_isBusy = true;
		final var delay = template.getParams().getInteger("spawnDelay");
		if (_spawnTask != null)
		{
			_spawnTask.cancel(true);
			_spawnTask = null;
		}
		_spawnTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				spawn();
			}
		}, delay);
	}

	public void stop()
	{
		if (_spawnTask != null)
		{
			_spawnTask.cancel(true);
			_spawnTask = null;
		}
		unspawn();
		_isBusy = false;
	}
	
	private void spawn()
	{
		for (final var spawn : _roomSpawns)
		{
			spawn.doSpawn();
			if (!_isBossRoom)
			{
				spawn.startRespawn();
			}
		}
	}

	private void unspawn()
	{
		for (final var spawn : _roomSpawns)
		{
			spawn.stopRespawn();
			final var npc = spawn.getLastSpawn();
			if (npc != null)
			{
				npc.deleteMe();
			}
		}
	}

	public boolean isInside(Player player)
	{
		return _s.contains(player.getX(), player.getY()) && (player.getZ() >= _zMin) && (player.getZ() <= _zMax);
	}
	
	public boolean isInside(int x, int y, int z)
	{
		return _s.contains(x, y) && (z >= _zMin) && (z <= _zMax);
	}

	public int getPlayersCount(Party party)
	{
		int ret = 0;
		if (party == null)
		{
			return ret;
		}
		
		for (final var p : party.getMembers())
		{
			if (p != null && isInside(p))
			{
				ret++;
			}
		}
		return ret;
	}
	
	public int getRandomX()
	{
		return Rnd.get(_xMin, _xMax);
	}
	
	public int getRandomY()
	{
		return Rnd.get(_yMin, _yMax);
	}
	
	public Location getTeleportLocation()
	{
		return _teleportLoc;
	}
}