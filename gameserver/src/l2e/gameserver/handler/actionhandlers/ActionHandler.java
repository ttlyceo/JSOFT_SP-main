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
package l2e.gameserver.handler.actionhandlers;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.handler.actionhandlers.impl.ArtefactAction;
import l2e.gameserver.handler.actionhandlers.impl.DecoyAction;
import l2e.gameserver.handler.actionhandlers.impl.DoorAction;
import l2e.gameserver.handler.actionhandlers.impl.ItemAction;
import l2e.gameserver.handler.actionhandlers.impl.NpcAction;
import l2e.gameserver.handler.actionhandlers.impl.PetAction;
import l2e.gameserver.handler.actionhandlers.impl.PlayerAction;
import l2e.gameserver.handler.actionhandlers.impl.StaticObjectAction;
import l2e.gameserver.handler.actionhandlers.impl.SummonAction;
import l2e.gameserver.handler.actionhandlers.impl.TrapAction;
import l2e.gameserver.model.GameObject.InstanceType;

public class ActionHandler extends LoggerObject
{
	private final Map<InstanceType, IActionHandler> _handlers;

	public static ActionHandler getInstance()
	{
		return SingletonHolder._instance;
	}

	protected ActionHandler()
	{
		_handlers = new HashMap<>();
		
		registerHandler(new ArtefactAction());
		registerHandler(new DecoyAction());
		registerHandler(new DoorAction());
		registerHandler(new ItemAction());
		registerHandler(new NpcAction());
		registerHandler(new PlayerAction());
		registerHandler(new PetAction());
		registerHandler(new StaticObjectAction());
		registerHandler(new SummonAction());
		registerHandler(new TrapAction());
		
		info("Loaded " + _handlers.size() + " ActionHandlers");
	}

	public void registerHandler(IActionHandler handler)
	{
		_handlers.put(handler.getInstanceType(), handler);
	}
	
	public synchronized void removeHandler(IActionHandler handler)
	{
		_handlers.remove(handler.getInstanceType());
	}

	public IActionHandler getHandler(InstanceType iType)
	{
		IActionHandler result = null;
		for (InstanceType t = iType; t != null; t = t.getParent())
		{
			result = _handlers.get(t);
			if (result != null)
			{
				break;
			}
		}
		return result;
	}

	public int size()
	{
		return _handlers.size();
	}

	private static class SingletonHolder
	{
		protected static final ActionHandler _instance = new ActionHandler();
	}
}