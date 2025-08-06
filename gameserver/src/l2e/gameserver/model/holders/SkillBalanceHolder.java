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
package l2e.gameserver.model.holders;

import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.base.SkillChangeType;

public class SkillBalanceHolder
{
	private final int _skillId;
	private final int _targetId;
	
	private final Map<SkillChangeType, Double> _list = new ConcurrentHashMap<>();
	private final Map<SkillChangeType, Double> _olyList = new ConcurrentHashMap<>();
	
	public SkillBalanceHolder(int SkillId, int target)
	{
		_skillId = SkillId;
		_targetId = target;
	}
	public int getSkillId()
	{
		return _skillId;
	}
	
	public String getSkillIcon()
	{
		return SkillsParser.getInstance().getInfo(_skillId, 1).getIcon();
	}
	
	public int getTarget()
	{
		return _targetId;
	}
	
	public Map<SkillChangeType, Double> getNormalBalance()
	{
		final Map<SkillChangeType, Double> map = new TreeMap<>(new AttackTypeComparator());
		map.putAll(_list);
		
		return map;
	}
	
	public Map<SkillChangeType, Double> getOlyBalance()
	{
		final Map<SkillChangeType, Double> map = new TreeMap<>(new AttackTypeComparator());
		map.putAll(_olyList);
		
		return map;
	}
	
	private class AttackTypeComparator implements Comparator<SkillChangeType>
	{
		public AttackTypeComparator()
		{
		}
		
		@Override
		public int compare(SkillChangeType l, SkillChangeType r)
		{
			final int left = l.getId();
			final int right = r.getId();
			if (left > right)
			{
				return 1;
			}
			
			if (left < right)
			{
				return -1;
			}
			
			final Random rnd = new Random();
			
			return rnd.nextInt(2) == 1 ? 1 : 1;
		}
	}
	
	public void remove(SkillChangeType sct)
	{
		if (_list.containsKey(sct))
		{
			_list.remove(sct);
		}
	}
	
	public void addSkillBalance(SkillChangeType sct, double value)
	{
		_list.put(sct, value);
	}
	
	public double getValue(SkillChangeType sct)
	{
		if (_list.containsKey(sct))
		{
			return _list.get(sct);
		}
		return 1.0D;
	}
	
	public void removeOly(SkillChangeType sct)
	{
		if (_olyList.containsKey(sct))
		{
			_olyList.remove(sct);
		}
	}
	
	public void addOlySkillBalance(SkillChangeType sct, double value)
	{
		_olyList.put(sct, value);
	}
	
	public double getOlyBalanceValue(SkillChangeType sct)
	{
		if (_olyList.containsKey(sct))
		{
			return _olyList.get(sct);
		}
		return 1.0D;
	}
}