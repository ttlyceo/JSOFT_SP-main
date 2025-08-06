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
package l2e.gameserver.handler.chathandlers;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.handler.chathandlers.impl.ChatAll;
import l2e.gameserver.handler.chathandlers.impl.ChatAlliance;
import l2e.gameserver.handler.chathandlers.impl.ChatBattlefield;
import l2e.gameserver.handler.chathandlers.impl.ChatClan;
import l2e.gameserver.handler.chathandlers.impl.ChatHeroVoice;
import l2e.gameserver.handler.chathandlers.impl.ChatMpccRoom;
import l2e.gameserver.handler.chathandlers.impl.ChatParty;
import l2e.gameserver.handler.chathandlers.impl.ChatPartyMatchRoom;
import l2e.gameserver.handler.chathandlers.impl.ChatPartyRoomAll;
import l2e.gameserver.handler.chathandlers.impl.ChatPartyRoomCommander;
import l2e.gameserver.handler.chathandlers.impl.ChatPetition;
import l2e.gameserver.handler.chathandlers.impl.ChatShout;
import l2e.gameserver.handler.chathandlers.impl.ChatTell;
import l2e.gameserver.handler.chathandlers.impl.ChatTrade;

public class ChatHandler extends LoggerObject
{
	private final Map<Integer, IChatHandler> _handlers;
	
	protected ChatHandler()
	{
		_handlers = new HashMap<>();

		registerHandler(new ChatAll());
		registerHandler(new ChatAlliance());
		registerHandler(new ChatBattlefield());
		registerHandler(new ChatClan());
		registerHandler(new ChatHeroVoice());
		registerHandler(new ChatMpccRoom());
		registerHandler(new ChatParty());
		registerHandler(new ChatPartyMatchRoom());
		registerHandler(new ChatPartyRoomAll());
		registerHandler(new ChatPartyRoomCommander());
		registerHandler(new ChatPetition());
		registerHandler(new ChatShout());
		registerHandler(new ChatTell());
		registerHandler(new ChatTrade());

		info("Loaded " + _handlers.size() + " ChatHandlers.");
	}
	
	public void registerHandler(IChatHandler handler)
	{
		final int[] ids = handler.getChatTypeList();
		for (final int id : ids)
		{
			if (_handlers.containsKey(id))
			{
				info("dublicate bypass registered! First handler: " + _handlers.get(id).getClass().getSimpleName() + " second: " + handler.getClass().getSimpleName());
				_handlers.remove(id);
			}
			_handlers.put(id, handler);
		}
	}
	
	public synchronized void removeHandler(IChatHandler handler)
	{
		final int[] ids = handler.getChatTypeList();
		for (final int id : ids)
		{
			_handlers.remove(id);
		}
	}
	
	public IChatHandler getHandler(Integer chatType)
	{
		return _handlers.get(chatType);
	}
	
	public int size()
	{
		return _handlers.size();
	}
	
	public static ChatHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ChatHandler _instance = new ChatHandler();
	}
}