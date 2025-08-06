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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import l2e.commons.util.Rnd;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.npc.MinionData;
import l2e.gameserver.model.actor.templates.npc.MinionTemplate;
import l2e.gameserver.model.spawn.Spawner;

public class MinionList
{
	private final List<MinionData> _minionData = new ArrayList<>();
	private final List<MonsterInstance> _minions = new ArrayList<>();
	private final Npc _master;
	private boolean _isRandomMinons = false;
	private final Lock _lock = new ReentrantLock();

	public MinionList(Npc master)
	{
		_master = master;
		_isRandomMinons = _master.getTemplate().isRandomMinons();
		_minionData.addAll(_master.getTemplate().getMinionData());
	}
	
	public boolean isRandomMinons()
	{
		return _isRandomMinons;
	}

	public boolean addMinion(MinionData m, boolean spawn)
	{
		_lock.lock();
		try
		{
			if (_minionData.add(m))
			{
				if (spawn)
				{
					spawnMinion(m);
				}
				return true;
			}
			return false;
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public boolean addMinion(MonsterInstance m)
	{
		_lock.lock();
		try
		{
			return _minions.add(m);
		}
		finally
		{
			_lock.unlock();
		}
	}

	public boolean hasAliveMinions()
	{
		_lock.lock();
		try
		{
			for (final MonsterInstance m : _minions)
			{
				if (m.isVisible() && !m.isDead())
				{
					return true;
				}
			}
			return false;
		}
		finally
		{
			_lock.unlock();
		}
	}

	public boolean hasMinions()
	{
		_lock.lock();
		try
		{
			return _minionData.size() > 0;
		}
		finally
		{
			_lock.unlock();
		}
	}
	

	public List<MonsterInstance> getAliveMinions()
	{
		if (_minions == null || _minions.isEmpty())
		{
			return Collections.emptyList();
		}
		
		final List<MonsterInstance> result = new ArrayList<>(_minions.size());
		
		_lock.lock();
		try
		{
			for (final MonsterInstance m : _minions)
			{
				if (m.isVisible() && !m.isDead())
				{
					result.add(m);
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
		return result;
	}
	
	public void spawnMinion(MinionData minions)
	{
		_lock.lock();
		try
		{
			final boolean isCanSupportMinions = _master.getSpawn() != null && _master.getSpawn().getMinionList() != null;
			for (final MinionTemplate minion : minions.getMinions())
			{
				final int minionId = minion.getMinionId();
				final int minionCount = minion.getAmount();
				
				for (int i = 0; i < minionCount; i++)
				{
					final MonsterInstance m = new MonsterInstance(IdFactory.getInstance().getNextId(), NpcsParser.getInstance().getTemplate(minionId));
					m.setLeader(_master);
					m.setIsNoRndWalk(true);
					if (isCanSupportMinions)
					{
						m.isCanSupportMinion(false);
					}
					m.setIsRaidMinion(_master.isRaid());
					_master.spawnMinion(m);
					_minions.add(m);
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
	}

	public void spawnMinions()
	{
		_lock.lock();
		try
		{
			int minionCount;
			int minionId;
			final boolean isCanSupportMinions = _master.getSpawn() != null && _master.getSpawn().getMinionList() != null;
			for (final MinionData minions : _minionData)
			{
				for (final MinionTemplate minion : minions.getMinions())
				{
					minionId = minion.getMinionId();
					minionCount = minion.getAmount();
					
					for (final MonsterInstance m : _minions)
					{
						if (m.getId() == minionId)
						{
							minionCount--;
						}
						if (m.isDead() || !m.isVisible())
						{
							m.refreshID();
							_master.spawnMinion(m);
						}
					}
					
					for (int i = 0; i < minionCount; i++)
					{
						final MonsterInstance m = new MonsterInstance(IdFactory.getInstance().getNextId(), NpcsParser.getInstance().getTemplate(minionId));
						m.setLeader(_master);
						m.setIsNoRndWalk(true);
						if (isCanSupportMinions)
						{
							m.isCanSupportMinion(false);
						}
						m.setIsRaidMinion(_master.isRaid());
						_master.spawnMinion(m);
						_minions.add(m);
					}
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public void spawnRndMinions()
	{
		final MinionData data = _minionData.size() > 1 ? _minionData.get(Rnd.get(_minionData.size())) : _minionData.get(0);
		if (data == null)
		{
			return;
		}
		
		_lock.lock();
		try
		{
			final boolean isCanSupportMinions = _master.getSpawn() != null && _master.getSpawn().getMinionList() != null;
			for (final MinionTemplate minions : data.getMinions())
			{
				final int minionId = minions.getMinionId();
				int minionCount = minions.getAmount();
				
				for (final MonsterInstance m : _minions)
				{
					if (m.getId() == minionId)
					{
						minionCount--;
					}
					if (m.isDead() || !m.isVisible())
					{
						m.refreshID();
						_master.spawnMinion(m);
					}
				}
				
				for (int i = 0; i < minionCount; i++)
				{
					final MonsterInstance m = new MonsterInstance(IdFactory.getInstance().getNextId(), NpcsParser.getInstance().getTemplate(minionId));
					m.setLeader(_master);
					m.setIsNoRndWalk(true);
					if (isCanSupportMinions)
					{
						m.isCanSupportMinion(false);
					}
					m.setIsRaidMinion(_master.isRaid());
					_master.spawnMinion(m);
					_minions.add(m);
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public void despawnMinions()
	{
		if (_minions == null || _minions.isEmpty())
		{
			return;
		}
		
		_lock.lock();
		try
		{
			for (final MonsterInstance m : _minions)
			{
				if (m != null)
				{
					m.deleteMe();
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public void onMasterDeath()
	{
		_lock.lock();
		try
		{
			if (_master.isRaid())
			{
				despawnMinions();
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public void onMasterDelete()
	{
		_lock.lock();
		try
		{
			despawnMinions();
			_minions.clear();
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public void onMinionDelete()
	{
		_lock.lock();
		try
		{
			if (!_master.isVisible() && !hasAliveMinions())
			{
				final Spawner spawn = _master.getSpawn();
				if (spawn != null)
				{
					spawn.decreaseCount(_master);
				}
				else
				{
					_master.deleteMe();
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public void clearMinions()
	{
		_lock.lock();
		try
		{
			_minionData.clear();
			_minions.clear();
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public boolean hasNpcId(int npcId)
	{
		if (_master != null && _master.getId() == npcId)
		{
			return true;
		}
		
		if (_minions == null || _minions.isEmpty())
		{
			return false;
		}
		
		_lock.lock();
		try
		{
			for (final MonsterInstance m : _minions)
			{
				if (m != null && m.getId() == npcId)
				{
					return true;
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
		return false;
	}
}