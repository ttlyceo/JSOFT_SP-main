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
package l2e.gameserver.handler.usercommandhandlers;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.handler.usercommandhandlers.impl.ChannelDelete;
import l2e.gameserver.handler.usercommandhandlers.impl.ChannelInfo;
import l2e.gameserver.handler.usercommandhandlers.impl.ChannelLeave;
import l2e.gameserver.handler.usercommandhandlers.impl.ClanPenalty;
import l2e.gameserver.handler.usercommandhandlers.impl.ClanWarsList;
import l2e.gameserver.handler.usercommandhandlers.impl.DisMount;
import l2e.gameserver.handler.usercommandhandlers.impl.InstanceZone;
import l2e.gameserver.handler.usercommandhandlers.impl.Loc;
import l2e.gameserver.handler.usercommandhandlers.impl.Mount;
import l2e.gameserver.handler.usercommandhandlers.impl.MyBirthday;
import l2e.gameserver.handler.usercommandhandlers.impl.OlympiadStat;
import l2e.gameserver.handler.usercommandhandlers.impl.PartyInfo;
import l2e.gameserver.handler.usercommandhandlers.impl.SiegeStatus;
import l2e.gameserver.handler.usercommandhandlers.impl.Time;
import l2e.gameserver.handler.usercommandhandlers.impl.Unstuck;

public class UserCommandHandler extends LoggerObject
{
	private final Map<Integer, IUserCommandHandler> _handlers;
	
	public static UserCommandHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected UserCommandHandler()
	{
		_handlers = new HashMap<>();

		registerHandler(new ChannelDelete());
		registerHandler(new ChannelInfo());
		registerHandler(new ChannelLeave());
		registerHandler(new ClanPenalty());
		registerHandler(new ClanWarsList());
		registerHandler(new DisMount());
		registerHandler(new InstanceZone());
		registerHandler(new Loc());
		registerHandler(new Mount());
		registerHandler(new MyBirthday());
		registerHandler(new OlympiadStat());
		registerHandler(new PartyInfo());
		registerHandler(new SiegeStatus());
		registerHandler(new Time());
		registerHandler(new Unstuck());

		info("Loaded " + _handlers.size() + " UserHandlers.");
	}
	
	public void registerHandler(IUserCommandHandler handler)
	{
		final int[] ids = handler.getUserCommandList();
		for (int i = 0; i < ids.length; i++)
		{
			if (_handlers.containsKey(ids[i]))
			{
				info("dublicate bypass registered! First handler: " + _handlers.get(ids[i]).getClass().getSimpleName() + " second: " + handler.getClass().getSimpleName());
				_handlers.remove(ids[i]);
			}
			_handlers.put(ids[i], handler);
		}
	}
	
	public synchronized void removeHandler(IUserCommandHandler handler)
	{
		final int[] ids = handler.getUserCommandList();
		for (final int id : ids)
		{
			_handlers.remove(id);
		}
	}
	
	public IUserCommandHandler getHandler(Integer userCommand)
	{
		if (Config.DEBUG)
		{
			_log.info("getting handler for user command: " + userCommand);
		}
		return _handlers.get(userCommand);
	}
	
	public int size()
	{
		return _handlers.size();
	}
	
	private static class SingletonHolder
	{
		protected static final UserCommandHandler _instance = new UserCommandHandler();
	}
}