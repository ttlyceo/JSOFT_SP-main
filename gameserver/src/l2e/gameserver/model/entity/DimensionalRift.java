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
package l2e.gameserver.model.entity;

import java.util.concurrent.Future;

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.DimensionalRiftManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.DimensionalRiftRoomTemplate;
import l2e.gameserver.model.actor.templates.DimensionalRiftTemplate;

public class DimensionalRift
{
	private final Party _party;
	private final DimensionalRiftTemplate _template;
	private DimensionalRiftRoomTemplate _currentRoom;
	private Future<?> _teleportTask;
	private Future<?> _earthquakeTask;
	private boolean _isBossRoom = false;
	private byte _currentJump = 0;
	private boolean _isJumped = false;
	private boolean _manualTeleport = false;
	
	public DimensionalRift(Party party, DimensionalRiftRoomTemplate room, DimensionalRiftTemplate template)
	{
		party.setDimensionalRift(this);
		_party = party;
		_currentRoom = room;
		_isBossRoom = room.isBossRoom();
		_template = template;
		final var coords = room.getTeleportLocation();
		for (final var p : party.getMembers())
		{
			final var riftQuest = QuestManager.getInstance().getQuest(635);
			if (riftQuest != null)
			{
				var qs = p.getQuestState(riftQuest.getName());
				if (qs == null)
				{
					qs = riftQuest.newQuestState(p);
				}
				if (!qs.isStarted())
				{
					qs.startQuest();
				}
			}
			p.teleToLocation(coords, true, p.getReflection());
		}
		
		final var jumpTime = calcTimeToNextJump();
		room.start(_template);
		_teleportTask = ThreadPoolManager.getInstance().schedule(new RiftTeleportTask(), jumpTime);
		_earthquakeTask = ThreadPoolManager.getInstance().schedule(new EarthquakeTask(), jumpTime - 3000);
	}

	public Party getParty()
	{
		return _party;
	}

	public boolean isJumped()
	{
		return _isJumped;
	}

	private class RiftTeleportTask implements Runnable
	{
		@Override
		public void run()
		{
			_currentJump++;

			if (_party == null || _party.getMemberCount() < 1)
			{
				_currentRoom.telePlayersToOut(_party);
				_currentRoom.stop();
				DimensionalRiftManager.getInstance().removeRift(DimensionalRift.this);
				return;
			}

			boolean isDead = true;
			for (final var member : _party.getMembers())
			{
				if (member != null && _currentRoom.isInside(member) && !member.isDead())
				{
					isDead = false;
					break;
				}
			}

			if (isDead || _isBossRoom || _currentJump > _template.getMaxJumps() || _currentRoom.getPlayersCount(_party) < 2)
			{
				_currentRoom.telePlayersToOut(_party);
				_currentRoom.stop();
				_party.setDimensionalRift(null);
				DimensionalRiftManager.getInstance().removeRift(DimensionalRift.this);
				return;
			}

			final var isBossRoom = _template.isCustomTeleFunction() && Rnd.chance(_template.getBossRoomChance());
			final var rooms = DimensionalRiftManager.getInstance().getFreeRooms(_currentRoom.getRoomType(), _currentJump != _template.getMaxJumps() && !_manualTeleport, isBossRoom);
			_manualTeleport = false;
			if (rooms.size() > 0)
			{
				final var nextRoom = rooms.get(Rnd.get(rooms.size()));
				nextRoom.start(_template);
				_currentRoom.telePlayersToNext(_party, _currentRoom, nextRoom);
				_currentRoom.stop();
				_currentRoom = nextRoom;
				_isBossRoom = nextRoom.isBossRoom();
				final var jumpTime = calcTimeToNextJump();
				_teleportTask = ThreadPoolManager.getInstance().schedule(new RiftTeleportTask(), jumpTime);
				_earthquakeTask = ThreadPoolManager.getInstance().schedule(new EarthquakeTask(), jumpTime - 3000);
			}
			else
			{
				_currentRoom.telePlayersToOut(_party);
				_currentRoom.stop();
				_party.setDimensionalRift(null);
				DimensionalRiftManager.getInstance().removeRift(DimensionalRift.this);
			}
		}
	}

	private class EarthquakeTask implements Runnable
	{
		@Override
		public void run()
		{
			_currentRoom.earthquake(_party);
		}
	}

	public void manualTeleport()
	{
		_isJumped = true;
		if (_teleportTask != null)
		{
			_teleportTask.cancel(true);
			_teleportTask = null;
		}

		if (_earthquakeTask != null)
		{
			_earthquakeTask.cancel(true);
			_earthquakeTask = null;
		}
		_manualTeleport = true;
		new RiftTeleportTask().run();
	}

	public void manualExit()
	{
		if (_teleportTask != null)
		{
			_teleportTask.cancel(true);
		}

		if (_earthquakeTask != null)
		{
			_earthquakeTask.cancel(true);
		}
		_currentRoom.telePlayersToOut(_party);
		_currentRoom.stop();
		DimensionalRiftManager.getInstance().removeRift(this);
		_party.setDimensionalRift(null);
	}

	public void checkDeath()
	{
		boolean isDead = true;
		
		if (_party != null)
		{
			for (final var member : _party.getMembers())
			{
				if (member != null && _currentRoom.isInside(member) && !member.isDead())
				{
					isDead = false;
					break;
				}
			}
		}

		if (isDead)
		{
			manualExit();
		}
	}

	public void oustMember(String name)
	{
		if (_party != null)
		{
			for (final var player : _party.getMembers())
			{
				if (player != null && player.getName(null).equalsIgnoreCase(name) && _currentRoom.isInside(player))
				{
					teleportToWaitingRoom(player);
					break;
				}
			}
		}
	}
	
	private void teleportToWaitingRoom(Player player)
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

	private long calcTimeToNextJump()
	{
		final long time = Rnd.get(_template.getParams().getInteger("jumpDelayMin"), _template.getParams().getInteger("jumpDelayMax")) * 60000L;
		if (_currentRoom.isBossRoom())
		{
			return (long) (time * _template.getParams().getDouble("bossTimeModifier"));
		}
		return time;
	}
	
	public DimensionalRiftRoomTemplate getCurrentRoom()
	{
		return _currentRoom;
	}
}