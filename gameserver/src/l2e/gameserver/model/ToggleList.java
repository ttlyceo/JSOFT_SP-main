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
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.network.SystemMessageId;

/**
 * Created by LordWinter
 */
public class ToggleList
{
	private final Creature _actor;
	private final List<Effect> _effectList = new ArrayList<>();
	private final ReadWriteLock _lock = new ReentrantReadWriteLock();
	private final Lock _writeLock = _lock.writeLock();
	
	public ToggleList(Creature actor)
	{
		_actor = actor;
	}
	
	public void addToggleEffect(Effect effect)
	{
		if (_actor == null || effect == null)
		{
			return;
		}
		
		_writeLock.lock();
		try
		{
			if (effect.getHpReduce() > 0 || effect.getMpReduce() > 0)
			{
				if (!_effectList.contains(effect))
				{
					_effectList.add(effect);
				}
			}
		}
		finally
		{
			_writeLock.unlock();
		}
	}
	
	public boolean calcToggleDam()
	{
		if (_effectList == null || _effectList.isEmpty() || _actor == null)
		{
			return false;
		}
		
		double mpReduce = 0;
		double hpReduce = 0;
		boolean isRelax = false;
		final List<Effect> effectList = new ArrayList<>();
		for (final Effect effect : _effectList)
		{
			if (effect != null)
			{
				final double mpDam = effect.getMpReduce();
				final double hpDam = effect.getHpReduce();
				if (mpDam < 1 && hpDam < 1)
				{
					effectList.add(effect);
					continue;
				}
				if (hpDam > 0)
				{
					if (((hpDam * Config.TOGGLE_MOD_MP * 1.3) + hpReduce) <= (_actor.getCurrentHp() - 1))
					{
						hpReduce += (hpDam * Config.TOGGLE_MOD_MP * 1.3);
						if (_actor.getCurrentHp() <= 1)
						{
							effectList.add(effect);
						}
					}
					else
					{
						effectList.add(effect);
					}
				}
				
				if (mpDam > 0)
				{
					if (_actor.isPlayer() && (effect.getSkill().getId() == 226 || effect.getSkill().getId() == 296))
					{
						if ((((_actor.getCurrentHp() + 1) > _actor.getMaxRecoverableHp()) && effect.getSkill().getId() != 296) || !_actor.getActingPlayer().isSitting())
						{
							effectList.add(effect);
							if (effect.getSkill().getId() != 296)
							{
								isRelax = true;
							}
							continue;
						}
					}
					
					if (((mpDam * Config.TOGGLE_MOD_MP * 1.3) + mpReduce) <= (_actor.getCurrentMp() - 1))
					{
						mpReduce += (mpDam * Config.TOGGLE_MOD_MP * 1.3);
					}
					else
					{
						effectList.add(effect);
					}
				}
			}
		}
		
		if (mpReduce > 0)
		{
			_actor.getStatus().reduceMp(mpReduce, false);
		}
		
		if (hpReduce > 0)
		{
			_actor.getStatus().reduceHp(hpReduce, _actor, false, true, false, false);
		}
		
		if (!effectList.isEmpty())
		{
			final int size = effectList.size();
			if (isRelax)
			{
				_actor.sendPacket(SystemMessageId.SKILL_DEACTIVATED_HP_FULL);
			}
			
			if (isRelax && size > 1 || !isRelax && size >= 1)
			{
				_actor.sendPacket(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
			}
			
			for (final Effect effect : effectList)
			{
				if (effect != null)
				{
					_actor.stopSkillEffects(effect.getSkill().getId());
				}
			}
			effectList.clear();
		}
		return true;
	}
	
	public void removeEffect(Effect effect)
	{
		if (_effectList.isEmpty())
		{
			return;
		}
		
		_writeLock.lock();
		try
		{
			if (_effectList.contains(effect))
			{
				_effectList.remove(effect);
			}
		}
		finally
		{
			_writeLock.unlock();
		}
	}
	
	public List<Effect> getEffects()
	{
		return _effectList;
	}
	
	public void clear()
	{
		_effectList.clear();
	}
	
	public boolean hasEffects()
	{
		return _effectList.size() > 0;
	}
}