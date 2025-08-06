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
package l2e.gameserver.handler.itemhandlers;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.handler.itemhandlers.impl.BeastSoulShot;
import l2e.gameserver.handler.itemhandlers.impl.BeastSpice;
import l2e.gameserver.handler.itemhandlers.impl.BeastSpiritShot;
import l2e.gameserver.handler.itemhandlers.impl.BlessedSpiritShot;
import l2e.gameserver.handler.itemhandlers.impl.Book;
import l2e.gameserver.handler.itemhandlers.impl.Bypass;
import l2e.gameserver.handler.itemhandlers.impl.Calculator;
import l2e.gameserver.handler.itemhandlers.impl.ChristmasTree;
import l2e.gameserver.handler.itemhandlers.impl.Community;
import l2e.gameserver.handler.itemhandlers.impl.Disguise;
import l2e.gameserver.handler.itemhandlers.impl.Elixir;
import l2e.gameserver.handler.itemhandlers.impl.EnchantAttribute;
import l2e.gameserver.handler.itemhandlers.impl.EnchantScrolls;
import l2e.gameserver.handler.itemhandlers.impl.EventItem;
import l2e.gameserver.handler.itemhandlers.impl.ExtractableItems;
import l2e.gameserver.handler.itemhandlers.impl.FishShots;
import l2e.gameserver.handler.itemhandlers.impl.Harvester;
import l2e.gameserver.handler.itemhandlers.impl.ItemSkills;
import l2e.gameserver.handler.itemhandlers.impl.ItemSkillsTemplate;
import l2e.gameserver.handler.itemhandlers.impl.ManaPotion;
import l2e.gameserver.handler.itemhandlers.impl.Maps;
import l2e.gameserver.handler.itemhandlers.impl.MercTicket;
import l2e.gameserver.handler.itemhandlers.impl.NevitHourglass;
import l2e.gameserver.handler.itemhandlers.impl.NicknameColor;
import l2e.gameserver.handler.itemhandlers.impl.Nobless;
import l2e.gameserver.handler.itemhandlers.impl.PetFood;
import l2e.gameserver.handler.itemhandlers.impl.Premium;
import l2e.gameserver.handler.itemhandlers.impl.QuestItems;
import l2e.gameserver.handler.itemhandlers.impl.Recipes;
import l2e.gameserver.handler.itemhandlers.impl.RollingDice;
import l2e.gameserver.handler.itemhandlers.impl.ScrollOfResurrection;
import l2e.gameserver.handler.itemhandlers.impl.Seed;
import l2e.gameserver.handler.itemhandlers.impl.SevenSignsRecord;
import l2e.gameserver.handler.itemhandlers.impl.SoulShots;
import l2e.gameserver.handler.itemhandlers.impl.SpecialXMas;
import l2e.gameserver.handler.itemhandlers.impl.SpiritShot;
import l2e.gameserver.handler.itemhandlers.impl.SummonItems;
import l2e.gameserver.handler.itemhandlers.impl.TeleportBookmark;
import l2e.gameserver.handler.itemhandlers.impl.TempHero;
import l2e.gameserver.handler.itemhandlers.impl.VisualItems;
import l2e.gameserver.model.actor.templates.items.EtcItem;

public class ItemHandler extends LoggerObject
{
	private final Map<String, IItemHandler> _handlers;
	
	public static ItemHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public int size()
	{
		return _handlers.size();
	}
	
	protected ItemHandler()
	{
		_handlers = new HashMap<>();
		
		registerHandler(new BeastSoulShot());
		registerHandler(new BeastSpice());
		registerHandler(new BeastSpiritShot());
		registerHandler(new BlessedSpiritShot());
		registerHandler(new Bypass());
		registerHandler(new Book());
		registerHandler(new Calculator());
		registerHandler(new ChristmasTree());
		registerHandler(new Community());
		registerHandler(new Disguise());
		registerHandler(new Elixir());
		registerHandler(new EnchantAttribute());
		registerHandler(new EnchantScrolls());
		registerHandler(new EventItem());
		registerHandler(new ExtractableItems());
		registerHandler(new FishShots());
		registerHandler(new Harvester());
		registerHandler(new ItemSkills());
		registerHandler(new ItemSkillsTemplate());
		registerHandler(new ManaPotion());
		registerHandler(new Maps());
		registerHandler(new MercTicket());
		registerHandler(new NevitHourglass());
		registerHandler(new NicknameColor());
		registerHandler(new Nobless());
		registerHandler(new PetFood());
		registerHandler(new Premium());
		registerHandler(new QuestItems());
		registerHandler(new Recipes());
		registerHandler(new RollingDice());
		registerHandler(new ScrollOfResurrection());
		registerHandler(new Seed());
		registerHandler(new SevenSignsRecord());
		registerHandler(new SoulShots());
		registerHandler(new SpecialXMas());
		registerHandler(new SpiritShot());
		registerHandler(new SummonItems());
		registerHandler(new TempHero());
		registerHandler(new TeleportBookmark());
		registerHandler(new VisualItems());
		
		info("Loaded " + _handlers.size() + " ItemHandlers.");
	}
	
	public void registerHandler(IItemHandler handler)
	{
		_handlers.put(handler.getClass().getSimpleName(), handler);
	}
	
	public synchronized void removeHandler(IItemHandler handler)
	{
		_handlers.remove(handler.getClass().getSimpleName());
	}
	
	public IItemHandler getHandler(EtcItem item)
	{
		if ((item == null) || (item.getHandlerName() == null))
		{
			return null;
		}
		return _handlers.get(item.getHandlerName());
	}
	
	private static class SingletonHolder
	{
		protected static final ItemHandler _instance = new ItemHandler();
	}
}