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
import java.util.ArrayList;
import java.util.List;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.InstanceListManager;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.entity.Fort;

public class FortManager extends LoggerObject implements InstanceListManager
{
	public static final FortManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private final List<Fort> _forts = new ArrayList<>();
	
	protected FortManager()
	{
	}
	
	public final int findNearestFortIndex(GameObject obj)
	{
		return findNearestFortIndex(obj, Long.MAX_VALUE);
	}
	
	public final int findNearestFortIndex(GameObject obj, long maxDistance)
	{
		int index = getFortIndex(obj);
		if (index < 0)
		{
			double distance;
			Fort fort;
			for (int i = 0; i < getForts().size(); i++)
			{
				fort = getForts().get(i);
				if (fort == null)
				{
					continue;
				}
				distance = fort.getDistance(obj);
				if (maxDistance > distance)
				{
					maxDistance = (long) distance;
					index = i;
				}
			}
		}
		return index;
	}
	
	public final Fort getFortById(int fortId)
	{
		for (final Fort f : getForts())
		{
			if (f.getId() == fortId)
			{
				return f;
			}
		}
		return null;
	}
	
	public final Fort getFortByOwner(Clan clan)
	{
		for (final Fort f : getForts())
		{
			if (f.getOwnerClan() == clan)
			{
				return f;
			}
		}
		return null;
	}
	
	public final Fort getFort(String name)
	{
		for (final Fort f : getForts())
		{
			if (f.getName().equalsIgnoreCase(name.trim()))
			{
				return f;
			}
		}
		return null;
	}
	
	public final Fort getFort(int x, int y, int z)
	{
		for (final Fort f : getForts())
		{
			if (f.checkIfInZone(x, y, z))
			{
				return f;
			}
		}
		return null;
	}
	
	public final Fort getFort(GameObject activeObject)
	{
		return getFort(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public final int getFortIndex(int fortId)
	{
		Fort fort;
		for (int i = 0; i < getForts().size(); i++)
		{
			fort = getForts().get(i);
			if ((fort != null) && (fort.getId() == fortId))
			{
				return i;
			}
		}
		return -1;
	}
	
	public final int getFortIndex(GameObject activeObject)
	{
		return getFortIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public final int getFortIndex(int x, int y, int z)
	{
		Fort fort;
		for (int i = 0; i < getForts().size(); i++)
		{
			fort = getForts().get(i);
			if ((fort != null) && fort.checkIfInZone(x, y, z))
			{
				return i;
			}
		}
		return -1;
	}
	
	public final List<Fort> getForts()
	{
		return _forts;
	}
	
	@Override
	public void loadInstances()
	{
		try (var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT id FROM fort ORDER BY id");
			final ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				getForts().add(new Fort(rs.getInt("id")));
			}
			
			rs.close();
			statement.close();
			
			info("Loaded: " + getForts().size() + " fortress");
			for (final Fort fort : getForts())
			{
				fort.getSiege().getSiegeGuardManager().loadSiegeGuard();
			}
		}
		catch (final Exception e)
		{
			warn("loadFortData(): " + e.getMessage(), e);
		}
	}
	
	@Override
	public void updateReferences()
	{
	}
	
	@Override
	public void activateInstances()
	{
		for (final Fort fort : _forts)
		{
			fort.activateInstance();
		}
	}
	
	private static class SingletonHolder
	{
		protected static final FortManager _instance = new FortManager();
	}
}