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
import java.util.List;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.service.autofarm.FarmSettings;

public final class AutoFarmManager
{
	private final List<Integer> _nonCheckPlayers = new ArrayList<>();
	
	protected AutoFarmManager()
	{
		_nonCheckPlayers.clear();
		DoubleSessionManager.getInstance().registerEvent(DoubleSessionManager.AUTO_FARM_ID);
	}
	
	public int getActiveFarms(Player player)
	{
		if (FarmSettings.FARM_ACTIVE_LIMITS < 0 || player == null)
		{
			return Integer.MAX_VALUE;
		}
		
		final var activeFarm = DoubleSessionManager.getInstance().getActivePlayers(DoubleSessionManager.AUTO_FARM_ID, player);
		return FarmSettings.FARM_ACTIVE_LIMITS - activeFarm;
	}
	
	public void addActiveFarm(Player player)
	{
		if (FarmSettings.FARM_ACTIVE_LIMITS < 0 || player == null)
		{
			return;
		}
		
		if (isNonCheckPlayer(player.getObjectId()))
		{
			return;
		}
		DoubleSessionManager.getInstance().tryAddPlayer(DoubleSessionManager.AUTO_FARM_ID, player, FarmSettings.FARM_ACTIVE_LIMITS);
	}
	
	public void removeActiveFarm(Player player)
	{
		if (FarmSettings.FARM_ACTIVE_LIMITS < 0 || player == null)
		{
			return;
		}
		
		if (isNonCheckPlayer(player.getObjectId()))
		{
			return;
		}
		DoubleSessionManager.getInstance().removePlayer(DoubleSessionManager.AUTO_FARM_ID, player);
	}
	
	public void addNonCheckPlayer(int charId)
	{
		if (FarmSettings.FARM_ACTIVE_LIMITS < 0)
		{
			return;
		}
		
		if (!_nonCheckPlayers.contains(charId))
		{
			_nonCheckPlayers.add(charId);
		}
	}
	
	public boolean isNonCheckPlayer(int charId)
	{
		return _nonCheckPlayers.contains(charId);
	}
	
	private static class SingletonHolder
	{
		protected static final AutoFarmManager _instance = new AutoFarmManager();
	}
	
	public static final AutoFarmManager getInstance()
	{
		return SingletonHolder._instance;
	}
}