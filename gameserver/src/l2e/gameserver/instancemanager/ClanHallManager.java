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

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.entity.Auction;
import l2e.gameserver.model.entity.ClanHall;
import l2e.gameserver.model.entity.clanhall.AuctionableHall;
import l2e.gameserver.model.entity.clanhall.SiegableHall;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.zone.type.ClanHallZone;

public final class ClanHallManager extends LoggerObject
{
	private final Map<Integer, AuctionableHall> _clanHall = new ConcurrentHashMap<>();
	private final Map<Integer, AuctionableHall> _freeClanHall = new ConcurrentHashMap<>();
	private final Map<Integer, AuctionableHall> _allAuctionableClanHalls = new HashMap<>();
	private final Map<Integer, ClanHall> _allClanHalls = new HashMap<>();
	private boolean _loaded = false;
	
	public static ClanHallManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public boolean loaded()
	{
		return _loaded;
	}
	
	protected ClanHallManager()
	{
		load();
	}
	
	private final void load()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			int id, ownerId, lease;
			final var statement = con.prepareStatement("SELECT * FROM clanhall ORDER BY id");
			final ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				final StatsSet set = new StatsSet();
				
				id = rs.getInt("id");
				ownerId = rs.getInt("ownerId");
				lease = rs.getInt("lease");
				
				set.set("id", id);
				set.set("ownerId", ownerId);
				set.set("lease", lease);
				set.set("paidUntil", rs.getLong("paidUntil"));
				set.set("grade", rs.getInt("Grade"));
				set.set("paid", rs.getBoolean("paid"));
				final AuctionableHall ch = new AuctionableHall(set);
				_allAuctionableClanHalls.put(id, ch);
				addClanHall(ch);
				
				if (ch.getOwnerId() > 0)
				{
					_clanHall.put(id, ch);
					continue;
				}
				_freeClanHall.put(id, ch);
				
				final Auction auc = AuctionManager.getInstance().getAuction(id);
				if ((auc == null) && (lease > 0))
				{
					AuctionManager.getInstance().initNPC(id);
				}
			}
			
			rs.close();
			statement.close();
			info("Loaded: " + getClanHalls().size() + " occupy and " + getFreeClanHalls().size() + " free clan halls.");
			_loaded = true;
		}
		catch (final Exception e)
		{
			warn("ClanHallManager.load(): " + e.getMessage(), e);
		}
	}
	
	public final Map<Integer, ClanHall> getAllClanHalls()
	{
		return _allClanHalls;
	}
	
	public final Map<Integer, AuctionableHall> getFreeClanHalls()
	{
		return _freeClanHall;
	}
	
	public final Map<Integer, AuctionableHall> getClanHalls()
	{
		return _clanHall;
	}
	
	public final Map<Integer, AuctionableHall> getAllAuctionableClanHalls()
	{
		return _allAuctionableClanHalls;
	}
	
	public final void addClanHall(ClanHall hall)
	{
		_allClanHalls.put(hall.getId(), hall);
	}
	
	public final boolean isFree(int chId)
	{
		if (_freeClanHall.containsKey(chId))
		{
			return true;
		}
		return false;
	}
	
	public final synchronized void setFree(int chId)
	{
		_freeClanHall.put(chId, _clanHall.get(chId));
		ClanHolder.getInstance().getClan(_freeClanHall.get(chId).getOwnerId()).setHideoutId(0);
		_freeClanHall.get(chId).free();
		_clanHall.remove(chId);
	}
	
	public final synchronized void setOwner(int chId, Clan clan)
	{
		if (clan == null)
		{
			return;
		}
		
		if (!_clanHall.containsKey(chId))
		{
			_clanHall.put(chId, _freeClanHall.get(chId));
			_freeClanHall.remove(chId);
		}
		else
		{
			_clanHall.get(chId).free();
		}
		ClanHolder.getInstance().getClan(clan.getId()).setHideoutId(chId);
		_clanHall.get(chId).setOwner(clan);
	}
	
	public final ClanHall getClanHallById(int clanHallId)
	{
		return _allClanHalls.get(clanHallId);
	}
	
	public final AuctionableHall getAuctionableHallById(int clanHallId)
	{
		return _allAuctionableClanHalls.get(clanHallId);
	}
	
	public final ClanHall getClanHall(int x, int y, int z)
	{
		for (final ClanHall temp : getAllClanHalls().values())
		{
			if (temp.checkIfInZone(x, y, z))
			{
				return temp;
			}
		}
		return null;
	}
	
	public final ClanHall getClanHall(GameObject activeObject)
	{
		return getClanHall(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public final AuctionableHall getNearbyClanHall(int x, int y, int maxDist)
	{
		ClanHallZone zone = null;
		
		for (final Map.Entry<Integer, AuctionableHall> ch : _clanHall.entrySet())
		{
			zone = ch.getValue().getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch.getValue();
			}
		}
		for (final Map.Entry<Integer, AuctionableHall> ch : _freeClanHall.entrySet())
		{
			zone = ch.getValue().getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	public final ClanHall getNearbyAbstractHall(int x, int y, int maxDist)
	{
		ClanHallZone zone = null;
		for (final Map.Entry<Integer, ClanHall> ch : _allClanHalls.entrySet())
		{
			zone = ch.getValue().getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	public final AuctionableHall getClanHallByOwner(Clan clan)
	{
		for (final Map.Entry<Integer, AuctionableHall> ch : _clanHall.entrySet())
		{
			if (clan.getId() == ch.getValue().getOwnerId())
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	public final ClanHall getAbstractHallByOwner(Clan clan)
	{
		for (final Map.Entry<Integer, AuctionableHall> ch : _clanHall.entrySet())
		{
			if (clan.getId() == ch.getValue().getOwnerId())
			{
				return ch.getValue();
			}
		}
		for (final Map.Entry<Integer, SiegableHall> ch : CHSiegeManager.getInstance().getConquerableHalls().entrySet())
		{
			if (clan.getId() == ch.getValue().getOwnerId())
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanHallManager _instance = new ClanHallManager();
	}
}