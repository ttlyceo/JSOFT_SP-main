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
package l2e.gameserver.handler.bypasshandlers;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.handler.bypasshandlers.impl.AgressionInfo;
import l2e.gameserver.handler.bypasshandlers.impl.Augment;
import l2e.gameserver.handler.bypasshandlers.impl.Buy;
import l2e.gameserver.handler.bypasshandlers.impl.BuyShadowItem;
import l2e.gameserver.handler.bypasshandlers.impl.ChatLink;
import l2e.gameserver.handler.bypasshandlers.impl.ClanWarehouse;
import l2e.gameserver.handler.bypasshandlers.impl.DropInfo;
import l2e.gameserver.handler.bypasshandlers.impl.EffectInfo;
import l2e.gameserver.handler.bypasshandlers.impl.ElcardiaBuff;
import l2e.gameserver.handler.bypasshandlers.impl.Exchanger;
import l2e.gameserver.handler.bypasshandlers.impl.Festival;
import l2e.gameserver.handler.bypasshandlers.impl.FortSiege;
import l2e.gameserver.handler.bypasshandlers.impl.Freight;
import l2e.gameserver.handler.bypasshandlers.impl.Hennas;
import l2e.gameserver.handler.bypasshandlers.impl.ItemAuctionLink;
import l2e.gameserver.handler.bypasshandlers.impl.Link;
import l2e.gameserver.handler.bypasshandlers.impl.Loto;
import l2e.gameserver.handler.bypasshandlers.impl.ManorManager;
import l2e.gameserver.handler.bypasshandlers.impl.Multisell;
import l2e.gameserver.handler.bypasshandlers.impl.Observation;
import l2e.gameserver.handler.bypasshandlers.impl.OlympiadManagerLink;
import l2e.gameserver.handler.bypasshandlers.impl.OlympiadObservation;
import l2e.gameserver.handler.bypasshandlers.impl.PlayerHelp;
import l2e.gameserver.handler.bypasshandlers.impl.PrivateWarehouse;
import l2e.gameserver.handler.bypasshandlers.impl.QuestLink;
import l2e.gameserver.handler.bypasshandlers.impl.QuestList;
import l2e.gameserver.handler.bypasshandlers.impl.ReleaseAttribute;
import l2e.gameserver.handler.bypasshandlers.impl.RentPet;
import l2e.gameserver.handler.bypasshandlers.impl.Rift;
import l2e.gameserver.handler.bypasshandlers.impl.SkillList;
import l2e.gameserver.handler.bypasshandlers.impl.SupportBlessing;
import l2e.gameserver.handler.bypasshandlers.impl.SupportMagic;
import l2e.gameserver.handler.bypasshandlers.impl.TerritoryStatus;
import l2e.gameserver.handler.bypasshandlers.impl.VoiceCommand;
import l2e.gameserver.handler.bypasshandlers.impl.Wear;
import l2e.gameserver.handler.voicedcommandhandlers.impl.SellBuff;

public class BypassHandler extends LoggerObject
{
	private final Map<String, IBypassHandler> _handlers;
	
	public static BypassHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected BypassHandler()
	{
		_handlers = new HashMap<>();

		registerHandler(new AgressionInfo());
		registerHandler(new Augment());
		registerHandler(new Buy());
		registerHandler(new BuyShadowItem());
		registerHandler(new ChatLink());
		registerHandler(new ClanWarehouse());
		registerHandler(new DropInfo());
		registerHandler(new EffectInfo());
		registerHandler(new ElcardiaBuff());
		registerHandler(new Exchanger());
		registerHandler(new Festival());
		registerHandler(new FortSiege());
		registerHandler(new Freight());
		registerHandler(new Hennas());
		registerHandler(new ItemAuctionLink());
		registerHandler(new Link());
		registerHandler(new Loto());
		registerHandler(new ManorManager());
		registerHandler(new Multisell());
		registerHandler(new Observation());
		registerHandler(new OlympiadManagerLink());
		registerHandler(new OlympiadObservation());
		registerHandler(new PlayerHelp());
		registerHandler(new PrivateWarehouse());
		registerHandler(new QuestLink());
		registerHandler(new QuestList());
		registerHandler(new ReleaseAttribute());
		registerHandler(new RentPet());
		registerHandler(new Rift());
		registerHandler(new SkillList());
		registerHandler(new SupportBlessing());
		registerHandler(new SupportMagic());
		registerHandler(new TerritoryStatus());
		registerHandler(new VoiceCommand());
		registerHandler(new Wear());
		registerHandler(new SellBuff());

		info("Loaded " + _handlers.size() + " BypassHandlers");
	}
	
	public void registerHandler(IBypassHandler handler)
	{
		for (final String element : handler.getBypassList())
		{
			if (_handlers.containsKey(element))
			{
				info("dublicate bypass registered! First handler: " + _handlers.get(element).getClass().getSimpleName() + " second: " + handler.getClass().getSimpleName());
				_handlers.remove(element);
			}
			_handlers.put(element.toLowerCase(), handler);
		}
	}
	
	public synchronized void removeHandler(IBypassHandler handler)
	{
		for (final String element : handler.getBypassList())
		{
			_handlers.remove(element.toLowerCase());
		}
	}
	
	public IBypassHandler getHandler(String BypassCommand)
	{
		String command = BypassCommand;
		
		if (BypassCommand.indexOf(" ") != -1)
		{
			command = BypassCommand.substring(0, BypassCommand.indexOf(" "));
		}
		
		if (Config.DEBUG)
		{
			_log.info("getting handler for command: " + command + " -> " + (_handlers.get(command.hashCode()) != null));
		}
		
		return _handlers.get(command.toLowerCase());
	}
	
	public int size()
	{
		return _handlers.size();
	}
	
	private static class SingletonHolder
	{
		protected static final BypassHandler _instance = new BypassHandler();
	}
}