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
package l2e.gameserver.model.actor.tasks.character;

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;

public final class MagicUseTask implements Runnable
{
	private final Creature _character;
	private GameObject[] _targets;
	private final Skill _skill;
	private int _count;
	private int _hitTime;
	private int _coolTime;
	private int _phase;
	private int _delay;
	private final boolean _simultaneously;
	
	public MagicUseTask(Creature character, GameObject[] tgts, Skill s, int hit, int cool, boolean simultaneous)
	{
		_character = character;
		_targets = tgts;
		_skill = s;
		_count = 0;
		_phase = 1;
		_hitTime = hit;
		_coolTime = cool;
		_simultaneously = simultaneous;
	}
	
	@Override
	public void run()
	{
		if (_character == null)
		{
			return;
		}
		switch (_phase)
		{
			case 1:
			{
				_character.onMagicLaunchedTimer(this);
				break;
			}
			case 2:
			{
				_character.onMagicHitTimer(this);
				break;
			}
			case 3:
			{
				_character.onMagicFinalizer(this);
				break;
			}
		}
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public int getPhase()
	{
		return _phase;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	public int getHitTime()
	{
		return _hitTime;
	}
	
	public int getCoolTime()
	{
		return _coolTime;
	}
	
	public GameObject[] getTargets()
	{
		return _targets;
	}
	
	public boolean isSimultaneous()
	{
		return _simultaneously;
	}
	
	public void setCount(int count)
	{
		_count = count;
	}
	
	public void setPhase(int phase)
	{
		_phase = phase;
	}
	
	public void setHitTime(int skillTime)
	{
		_hitTime = skillTime;
	}
	
	public void setCoolTime(int cool)
	{
		_coolTime = cool;
	}
	
	public void setTargets(GameObject[] targets)
	{
		_targets = targets;
	}
	
	public void setDelay(int val)
	{
		_delay = val;
	}
	
	public int getDelay()
	{
		return _delay;
	}
}