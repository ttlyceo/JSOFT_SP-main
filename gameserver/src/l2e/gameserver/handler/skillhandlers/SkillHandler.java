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
package l2e.gameserver.handler.skillhandlers;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.handler.skillhandlers.impl.BallistaBomb;
import l2e.gameserver.handler.skillhandlers.impl.Continuous;
import l2e.gameserver.handler.skillhandlers.impl.ConvertItem;
import l2e.gameserver.handler.skillhandlers.impl.DeluxeKey;
import l2e.gameserver.handler.skillhandlers.impl.Detection;
import l2e.gameserver.handler.skillhandlers.impl.Disablers;
import l2e.gameserver.handler.skillhandlers.impl.Dummy;
import l2e.gameserver.handler.skillhandlers.impl.EnergyReplenish;
import l2e.gameserver.handler.skillhandlers.impl.EnergySpend;
import l2e.gameserver.handler.skillhandlers.impl.ExtractStone;
import l2e.gameserver.handler.skillhandlers.impl.Fishing;
import l2e.gameserver.handler.skillhandlers.impl.FishingSkill;
import l2e.gameserver.handler.skillhandlers.impl.GetPlayer;
import l2e.gameserver.handler.skillhandlers.impl.Manadam;
import l2e.gameserver.handler.skillhandlers.impl.Mdam;
import l2e.gameserver.handler.skillhandlers.impl.NegateEffects;
import l2e.gameserver.handler.skillhandlers.impl.NornilsPower;
import l2e.gameserver.handler.skillhandlers.impl.Pdam;
import l2e.gameserver.handler.skillhandlers.impl.Resurrect;
import l2e.gameserver.handler.skillhandlers.impl.Sow;
import l2e.gameserver.handler.skillhandlers.impl.TakeFort;
import l2e.gameserver.handler.skillhandlers.impl.Trap;
import l2e.gameserver.handler.skillhandlers.impl.Unlock;
import l2e.gameserver.model.skills.SkillType;

public class SkillHandler extends LoggerObject
{
	private final Map<Integer, ISkillHandler> _handlers;
	
	public static SkillHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected SkillHandler()
	{
		_handlers = new HashMap<>();
		
		registerHandler(new BallistaBomb());
		registerHandler(new Continuous());
		registerHandler(new ConvertItem());
		registerHandler(new DeluxeKey());
		registerHandler(new Detection());
		registerHandler(new Disablers());
		registerHandler(new Dummy());
		registerHandler(new EnergyReplenish());
		registerHandler(new EnergySpend());
		registerHandler(new ExtractStone());
		registerHandler(new Fishing());
		registerHandler(new FishingSkill());
		registerHandler(new GetPlayer());
		registerHandler(new Manadam());
		registerHandler(new Mdam());
		registerHandler(new NegateEffects());
		registerHandler(new NornilsPower());
		registerHandler(new Pdam());
		registerHandler(new Resurrect());
		registerHandler(new Sow());
		registerHandler(new TakeFort());
		registerHandler(new Trap());
		registerHandler(new Unlock());
		
		info("Loaded " + _handlers.size() + " SkillHandlers");
	}
	
	public void registerHandler(ISkillHandler handler)
	{
		final SkillType[] types = handler.getSkillIds();
		for (final SkillType t : types)
		{
			if (_handlers.containsKey(t.ordinal()))
			{
				info("dublicate bypass registered! First handler: " + _handlers.get(t.ordinal()).getClass().getSimpleName() + " second: " + handler.getClass().getSimpleName());
				_handlers.remove(t.ordinal());
			}
			_handlers.put(t.ordinal(), handler);
		}
	}
	
	public synchronized void removeHandler(ISkillHandler handler)
	{
		final SkillType[] types = handler.getSkillIds();
		for (final SkillType t : types)
		{
			_handlers.remove(t.ordinal());
		}
	}
	
	public ISkillHandler getHandler(SkillType skillType)
	{
		return _handlers.get(skillType.ordinal());
	}
	
	public int size()
	{
		return _handlers.size();
	}
	
	private static class SingletonHolder
	{
		protected static final SkillHandler _instance = new SkillHandler();
	}
}