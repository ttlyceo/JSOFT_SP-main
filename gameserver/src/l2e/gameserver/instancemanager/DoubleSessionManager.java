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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.net.IPSettings;
import l2e.commons.util.Functions;
import l2e.gameserver.Config;
import l2e.gameserver.data.dao.HardwareLimitsDAO;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;

public final class DoubleSessionManager
{
	private static final Logger _log = LoggerFactory.getLogger(DoubleSessionManager.class);
	
	public static final int GAME_ID = 0;
	public static final int OLYMPIAD_ID = 100;
	public static final int AERIAL_CLEFT_ID = 101;
	public static final int AUTO_FARM_ID = 102;
	public static final int CASTLE_ID = 200;
	public static final int FORT_ID = 300;
	public static final int CLANHALL_ID = 400;
	public static final int FUNZONE_ID = 500;

	private final Map<String, long[]> _hardWareList = new HashMap<>();
	private final Map<Integer, Map<String, List<Player>>> _protectedList = new ConcurrentHashMap<>();
	private final Lock _lock = new ReentrantLock();
	
	protected DoubleSessionManager()
	{
		_hardWareList.clear();
		_protectedList.clear();
		HardwareLimitsDAO.getInstance().restore(_hardWareList);
		if (!Functions.isValidKey(Config.USER_KEY))
		{
			IPSettings.getInstance().autoIpConfig();
		}
	}
	
	public final long[] getHardWareInfo(String hwid)
	{
		if (_hardWareList.containsKey(hwid))
		{
			final var data = _hardWareList.get(hwid);
			if (data[1] > System.currentTimeMillis())
			{
				return data;
			}
		}
		return null;
	}
	
	public final int getHardWareLimit(String hwid)
	{
		var limit = Config.DOUBLE_SESSIONS_CHECK_MAX_PLAYERS;
		if (!Config.DOUBLE_SESSIONS_ENABLE || limit < 1)
		{
			return 0;
		}
		
		if (_hardWareList.containsKey(hwid))
		{
			final var data = _hardWareList.get(hwid);
			if (data[1] > System.currentTimeMillis())
			{
				limit += data[0];
			}
		}
		return limit;
	}
	
	public boolean addHardWareLimit(String hwid, int activeWindows, long expireTime)
	{
		if (HardwareLimitsDAO.getInstance().insert(hwid, activeWindows, expireTime))
		{
			_hardWareList.put(hwid, new long[]
			{
			        activeWindows, expireTime
			});
			return true;
		}
		return false;
	}

	public final boolean check(Creature attacker, Creature target)
	{
		if (!Config.DOUBLE_SESSIONS_ENABLE)
		{
			return true;
		}

		if (target == null)
		{
			return false;
		}

		final Player targetPlayer = target.getActingPlayer();
		if (targetPlayer == null)
		{
			return false;
		}

		if (attacker != null)
		{
			final Player attackerPlayer = attacker.getActingPlayer();
			if (attackerPlayer == null)
			{
				return false;
			}

			if ((targetPlayer.getClient() == null) || (attackerPlayer.getClient() == null) || (targetPlayer.getClient() != null && targetPlayer.getClient().isDetached()) || (attackerPlayer.getClient() != null && attackerPlayer.getClient().isDetached()))
			{
				return !Config.DOUBLE_SESSIONS_DISCONNECTED;
			}
			
			final String attackerInfo = Config.DOUBLE_SESSIONS_HWIDS ? attackerPlayer.getHWID() : attackerPlayer.getIPAddress();
			final String targetInfo = Config.DOUBLE_SESSIONS_HWIDS ? targetPlayer.getHWID() : targetPlayer.getIPAddress();
			
			final boolean notValid = attackerInfo.equalsIgnoreCase(targetInfo);
			if (Config.DEVELOPER)
			{
				_log.info(getClass().getSimpleName() + ": attacker - " + attackerInfo);
				_log.info(getClass().getSimpleName() + ": target - " + targetInfo);
				if (notValid)
				{
					_log.info(getClass().getSimpleName() + ": Not valid conditions!");
				}
				else
				{
					_log.info(getClass().getSimpleName() + ": Valid conditions!");
				}
			}
			return notValid ? false : true;
		}
		return true;
	}

	public final void registerEvent(int eventId)
	{
		_protectedList.putIfAbsent(eventId, new ConcurrentHashMap<>());
	}

	public final boolean tryAddPlayer(int eventId, Player player, int max)
	{
		if (!Config.DOUBLE_SESSIONS_ENABLE)
		{
			return true;
		}
		
		Map<String, List<Player>> hwids = _protectedList.get(eventId);
		if (hwids == null)
		{
			hwids = new ConcurrentHashMap<>();
		}
		
		final String clientInfo = Config.DOUBLE_SESSIONS_HWIDS ? player.getHWID() : player.getIPAddress();
		if (clientInfo == null)
		{
			if (Config.DEVELOPER)
			{
				_log.info("tryAddPlayer: clientInfo null!");
			}
			return false;
		}
		
		List<Player> list = hwids.get(clientInfo);
		if (list == null)
		{
			hwids.put(clientInfo, list = new ArrayList<>());
			list.add(player);
			if (Config.DEVELOPER)
			{
				_log.info("tryAddPlayer: " + player.getName(null) + " added!");
			}
			return true;
		}
		
		_lock.lock();
		try
		{
			final Iterator<Player> iterator = list.iterator();
			while (iterator.hasNext())
			{
				final Player pl = iterator.next();
				if (pl == null || (pl != null && pl.getObjectId() == player.getObjectId()) || (pl != null && !pl.isOnline()))
				{
					iterator.remove();
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
		
		if (list.size() >= max)
		{
			if (Config.DEVELOPER)
			{
				_log.info("tryAddPlayer: limit is exceeded!");
			}
			return false;
		}
		
		list.add(player);
		if (Config.DEVELOPER)
		{
			_log.info("tryAddPlayer: " + player.getName(null) + " added!");
		}
		return true;
	}

	public final boolean removePlayer(int eventId, Player player)
	{
		if (!Config.DOUBLE_SESSIONS_ENABLE)
		{
			return true;
		}
		
		final Map<String, List<Player>> hwids = _protectedList.get(eventId);
		if (hwids == null || player == null)
		{
			if (Config.DEVELOPER)
			{
				_log.info("removePlayer: hwids null!");
			}
			return false;
		}
		
		final String info = Config.DOUBLE_SESSIONS_HWIDS ? player.getHWID() : player.getIPAddress();
		if (info == null || info.isEmpty())
		{
			return false;
		}
			
		final List<Player> list = hwids.get(info);
		if (list == null)
		{
			return false;
		}
		
		_lock.lock();
		try
		{
			final Iterator<Player> iterator = list.iterator();
			while (iterator.hasNext())
			{
				final Player pl = iterator.next();
				if (pl == null || (pl != null && pl.getObjectId() == player.getObjectId()))
				{
					iterator.remove();
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
		
		if (Config.DEVELOPER)
		{
			_log.info("removePlayer: ok!");
		}
		return true;
	}
	
	public final int getActivePlayers(int eventId, Player player)
	{
		if (!Config.DOUBLE_SESSIONS_ENABLE)
		{
			return 0;
		}
		
		final Map<String, List<Player>> hwids = _protectedList.get(eventId);
		if (hwids == null)
		{
			return 0;
		}
		
		final String clientInfo = Config.DOUBLE_SESSIONS_HWIDS ? player.getHWID() : player.getIPAddress();
		if (clientInfo == null)
		{
			return 0;
		}
		
		final List<Player> list = hwids.get(clientInfo);
		if (list == null)
		{
			return 0;
		}
		
		_lock.lock();
		try
		{
			final Iterator<Player> iterator = list.iterator();
			while (iterator.hasNext())
			{
				final Player pl = iterator.next();
				if (pl == null || (pl != null && pl.getObjectId() == player.getObjectId()) || (pl != null && !pl.isOnline()))
				{
					if (eventId == AUTO_FARM_ID && pl != null)
					{
						if (!pl.getFarmSystem().isAutofarming())
						{
							iterator.remove();
						}
					}
					else
					{
						iterator.remove();
					}
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
		return list.size();
	}

	public final void onDisconnect(Player player)
	{
		if (!Config.DOUBLE_SESSIONS_ENABLE)
		{
			return;
		}

		for (final int events : _protectedList.keySet())
		{
			removePlayer(events, player);
		}
	}

	public final void clear(int eventId)
	{
		final Map<String, List<Player>> hwids = _protectedList.get(eventId);
		if (hwids != null)
		{
			hwids.clear();
		}
	}

	public static final DoubleSessionManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final DoubleSessionManager _instance = new DoubleSessionManager();
	}
}