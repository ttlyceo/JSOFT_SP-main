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
package l2e.gameserver.model.olympiad;

import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.stats.StatsSet;

public final class Participant
{
	private final int objectId;
	private Player player;
	private final String name;
	private final int side;
	private final int baseClass;
	private boolean disconnected = false;
	private boolean defaulted = false;
	private final StatsSet stats;
	public String clanName;
	public int clanId;

	public Participant(Player plr, int olympiadSide)
	{
		objectId = plr.getObjectId();
		player = plr;
		name = plr.getName(null);
		side = olympiadSide;
		baseClass = plr.getBaseClass();
		stats = Olympiad.getNobleStats(getObjectId());
		clanName = plr.getClan() != null ? plr.getClan().getName() : "";
		clanId = plr.getClanId();
	}
	
	public Participant(int objId, int olympiadSide)
	{
		objectId = objId;
		player = null;
		name = "-";
		side = olympiadSide;
		baseClass = 0;
		stats = null;
		clanName = "";
		clanId = 0;
	}
	
	public final boolean updatePlayer()
	{
		if ((player == null) || !player.isOnline())
		{
			player = GameObjectsStorage.getPlayer(getObjectId());
		}
		return (player != null);
	}
	
	public final void updateStat(String statName, int increment)
	{
		stats.set(statName, Math.max(stats.getInteger(statName) + increment, 0));
	}

	public String getName()
	{
		return name;
	}
	
	public Player getPlayer()
	{
		return player;
	}

	public int getObjectId()
	{
		return objectId;
	}
	public StatsSet getStats()
	{
		return stats;
	}

	public void setPlayer(Player noble)
	{
		player = noble;
	}

	public int getSide()
	{
		return side;
	}
	
	public int getBaseClass()
	{
		return baseClass;
	}
	
	public boolean isDisconnected()
	{
		return disconnected;
	}
	
	public void setDisconnected(boolean val)
	{
		disconnected = val;
	}
	
	public boolean isDefaulted()
	{
		return defaulted;
	}
	
	public void setDefaulted(boolean val)
	{
		defaulted = val;
	}

	public String getClanName()
	{
		return clanName;
	}
	
	public int getClanId()
	{
		return clanId;
	}
}