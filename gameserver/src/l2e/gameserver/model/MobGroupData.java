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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.model.actor.instance.ControllableMobInstance;

public class MobGroupData
{
	private final Map<Integer, MobGroup> _groupMap;
	
	public static final int FOLLOW_RANGE = 300;
	public static final int RANDOM_RANGE = 300;
	
	protected MobGroupData()
	{
		_groupMap = new ConcurrentHashMap<>();
	}
	
	public static MobGroupData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void addGroup(int groupKey, MobGroup group)
	{
		_groupMap.put(groupKey, group);
	}
	
	public MobGroup getGroup(int groupKey)
	{
		return _groupMap.get(groupKey);
	}
	
	public int getGroupCount()
	{
		return _groupMap.size();
	}
	
	public MobGroup getGroupForMob(ControllableMobInstance mobInst)
	{
		for (final MobGroup mobGroup : _groupMap.values())
		{
			if (mobGroup.isGroupMember(mobInst))
			{
				return mobGroup;
			}
		}
		
		return null;
	}
	
	public MobGroup[] getGroups()
	{
		return _groupMap.values().toArray(new MobGroup[getGroupCount()]);
	}
	
	public boolean removeGroup(int groupKey)
	{
		return (_groupMap.remove(groupKey) != null);
	}
	
	private static class SingletonHolder
	{
		protected static final MobGroupData _instance = new MobGroupData();
	}
}