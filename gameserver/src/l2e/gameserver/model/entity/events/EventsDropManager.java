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
package l2e.gameserver.model.entity.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.model.template.WorldEventDrop;

/**
 * Update by LordWinter 13.07.2020
 */
public class EventsDropManager
{
	private final Map<Integer, DropRule> _dropRules = new ConcurrentHashMap<>();
	
	public EventsDropManager()
	{
		_dropRules.clear();
	}
	
	public void addRule(int eventId, List<WorldEventDrop> dropList, boolean lvlControl)
	{
		final DropRule rule = new DropRule();
		rule._levDifferenceControl = lvlControl;
		for (final WorldEventDrop drop : dropList)
		{
			rule._items.add(drop);
		}
		_dropRules.put(eventId, rule);
	}
	
	public void removeRule(int eventId)
	{
		final DropRule rule = _dropRules.get(eventId);
		if (rule != null)
		{
			_dropRules.remove(eventId);
		}
	}
	
	public int[] calculateRewardItem(NpcTemplate npcTemplate, Creature lastAttacker)
	{
		final int res[] =
		{
		        0, 0
		};
		
		final int lvlDif = lastAttacker.getLevel() - npcTemplate.getLevel();
		final List<WorldEventDrop> rewards = new ArrayList<>();
		
		if (!_dropRules.isEmpty())
		{
			for (final DropRule tmp : _dropRules.values())
			{
				if (tmp != null)
				{
					if (tmp._levDifferenceControl && ((lvlDif > 7) || (lvlDif < -7)))
					{
						continue;
					}
					
					if (tmp._items != null && !tmp._items.isEmpty())
					{
						for (final WorldEventDrop drop : tmp._items)
						{
							if (npcTemplate.getLevel() < drop.getMinLevel() || npcTemplate.getLevel() > drop.getMaxLevel())
							{
								continue;
							}
							
							if (Rnd.chance(drop.getChance()))
							{
								rewards.add(drop);
							}
						}
					}
				}
			}
		}

		if (rewards.size() > 0)
		{
			final int rndRew = Rnd.get(rewards.size());
			res[0] = rewards.get(rndRew).getId();
			res[1] = (int) (rewards.get(rndRew).getMaxCount() > 0L ? Rnd.get(rewards.get(rndRew).getMinCount(), rewards.get(rndRew).getMaxCount()) : rewards.get(rndRew).getMinCount());
		}
		return res;
	}
	
	private static class DropRule
	{
		public boolean _levDifferenceControl;
		public List<WorldEventDrop> _items = new ArrayList<>();
	}
	
	public Map<Integer, DropRule> getEventRules()
	{
		return _dropRules;
	}
	
	public static final EventsDropManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventsDropManager _instance = new EventsDropManager();
	}
}