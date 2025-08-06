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

import java.util.concurrent.Future;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.EarthQuake;
import l2e.gameserver.network.serverpackets.GameServerPacket;

/**
 * Created by LordWinter
 */
public class DragonValleyManager
{
	private boolean _wasSpawned = false;
	private Future<?> _spawnTask;
	
	public DragonValleyManager()
	{
		if (_spawnTask != null)
		{
			_spawnTask.cancel(false);
			_spawnTask = null;
		}
		
		if (Config.DRAGON_MIGRATION_PERIOD < 1)
		{
			return;
		}
		final long period = Config.DRAGON_MIGRATION_PERIOD * 60000L;
		_spawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new MigrationTask(), period, period);
	}
	
	private class MigrationTask implements Runnable
	{
		@Override
		public void run()
		{
			if (Rnd.chance(Config.DRAGON_MIGRATION_CHANCE))
			{
				if (_wasSpawned)
				{
					SpawnParser.getInstance().despawnGroup("dragon_valley_migration");
				}
				
				SpawnParser.getInstance().spawnGroup("dragon_valley_migration");
				_wasSpawned = true;
				
				final Location loc = new Location(101400, 117064, -3696);
				final GameServerPacket eq = new EarthQuake(loc.getX(), loc.getY(), loc.getZ(), 30, 12);
				
				final int rx = regionX(loc.getX());
				final int ry = regionY(loc.getY());
				for (final Player player : GameObjectsStorage.getPlayers())
				{
					if (player != null && player.isOnline())
					{
						if (player.getReflectionId() != 0 || player.checkInTournament() || player.isInFightEvent() || player.isInOfflineMode() || player.isFakePlayer())
						{
							continue;
						}
						
						final int tx = regionX(player.getX());
						final int ty = regionY(player.getY());
						
						if (tx >= rx && tx <= rx && ty >= ry && ty <= ry)
						{
							player.sendPacket(eq);
						}
					}
				}
			}
		}
	}
	
	private static int regionX(int x)
	{
		return (x - World.MAP_MIN_X >> 15) + 11;
	}
	
	private static int regionY(int y)
	{
		return (y - World.MAP_MIN_Y >> 15) + 10;
	}
	
	public static final DragonValleyManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DragonValleyManager _instance = new DragonValleyManager();
	}
}