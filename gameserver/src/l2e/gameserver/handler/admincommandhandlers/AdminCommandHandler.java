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
package l2e.gameserver.handler.admincommandhandlers;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.handler.admincommandhandlers.impl.Admin;
import l2e.gameserver.handler.admincommandhandlers.impl.AerialCleft;
import l2e.gameserver.handler.admincommandhandlers.impl.Announcement;
import l2e.gameserver.handler.admincommandhandlers.impl.Balancer;
import l2e.gameserver.handler.admincommandhandlers.impl.BloodAltars;
import l2e.gameserver.handler.admincommandhandlers.impl.Buffs;
import l2e.gameserver.handler.admincommandhandlers.impl.CHSiege;
import l2e.gameserver.handler.admincommandhandlers.impl.Cache;
import l2e.gameserver.handler.admincommandhandlers.impl.Camera;
import l2e.gameserver.handler.admincommandhandlers.impl.ChangeAccessLevel;
import l2e.gameserver.handler.admincommandhandlers.impl.Clans;
import l2e.gameserver.handler.admincommandhandlers.impl.CreateItem;
import l2e.gameserver.handler.admincommandhandlers.impl.CursedWeapons;
import l2e.gameserver.handler.admincommandhandlers.impl.DailyTasks;
import l2e.gameserver.handler.admincommandhandlers.impl.Debug;
import l2e.gameserver.handler.admincommandhandlers.impl.Delete;
import l2e.gameserver.handler.admincommandhandlers.impl.Disconnect;
import l2e.gameserver.handler.admincommandhandlers.impl.DoorControl;
import l2e.gameserver.handler.admincommandhandlers.impl.EditChar;
import l2e.gameserver.handler.admincommandhandlers.impl.EditNpc;
import l2e.gameserver.handler.admincommandhandlers.impl.Effects;
import l2e.gameserver.handler.admincommandhandlers.impl.Element;
import l2e.gameserver.handler.admincommandhandlers.impl.Enchant;
import l2e.gameserver.handler.admincommandhandlers.impl.Events;
import l2e.gameserver.handler.admincommandhandlers.impl.ExpSp;
import l2e.gameserver.handler.admincommandhandlers.impl.FakePlayers;
import l2e.gameserver.handler.admincommandhandlers.impl.FightCalculator;
import l2e.gameserver.handler.admincommandhandlers.impl.FortSiege;
import l2e.gameserver.handler.admincommandhandlers.impl.Geodata;
import l2e.gameserver.handler.admincommandhandlers.impl.Gm;
import l2e.gameserver.handler.admincommandhandlers.impl.GmChat;
import l2e.gameserver.handler.admincommandhandlers.impl.GraciaSeeds;
import l2e.gameserver.handler.admincommandhandlers.impl.Heal;
import l2e.gameserver.handler.admincommandhandlers.impl.Hellbound;
import l2e.gameserver.handler.admincommandhandlers.impl.HelpPage;
import l2e.gameserver.handler.admincommandhandlers.impl.InstanceZone;
import l2e.gameserver.handler.admincommandhandlers.impl.Instances;
import l2e.gameserver.handler.admincommandhandlers.impl.Invul;
import l2e.gameserver.handler.admincommandhandlers.impl.Kick;
import l2e.gameserver.handler.admincommandhandlers.impl.Kill;
import l2e.gameserver.handler.admincommandhandlers.impl.KrateisCube;
import l2e.gameserver.handler.admincommandhandlers.impl.Level;
import l2e.gameserver.handler.admincommandhandlers.impl.LogsViewer;
import l2e.gameserver.handler.admincommandhandlers.impl.Mammon;
import l2e.gameserver.handler.admincommandhandlers.impl.Manor;
import l2e.gameserver.handler.admincommandhandlers.impl.Menu;
import l2e.gameserver.handler.admincommandhandlers.impl.Messages;
import l2e.gameserver.handler.admincommandhandlers.impl.MobGroups;
import l2e.gameserver.handler.admincommandhandlers.impl.OlympiadMenu;
import l2e.gameserver.handler.admincommandhandlers.impl.OnlineReward;
import l2e.gameserver.handler.admincommandhandlers.impl.Packets;
import l2e.gameserver.handler.admincommandhandlers.impl.PcCondOverrides;
import l2e.gameserver.handler.admincommandhandlers.impl.Petition;
import l2e.gameserver.handler.admincommandhandlers.impl.Pledge;
import l2e.gameserver.handler.admincommandhandlers.impl.Polymorph;
import l2e.gameserver.handler.admincommandhandlers.impl.Premium;
import l2e.gameserver.handler.admincommandhandlers.impl.Punishment;
import l2e.gameserver.handler.admincommandhandlers.impl.Quests;
import l2e.gameserver.handler.admincommandhandlers.impl.RepairChar;
import l2e.gameserver.handler.admincommandhandlers.impl.Res;
import l2e.gameserver.handler.admincommandhandlers.impl.Ride;
import l2e.gameserver.handler.admincommandhandlers.impl.Shop;
import l2e.gameserver.handler.admincommandhandlers.impl.ShowQuests;
import l2e.gameserver.handler.admincommandhandlers.impl.ShutdownMenu;
import l2e.gameserver.handler.admincommandhandlers.impl.Siege;
import l2e.gameserver.handler.admincommandhandlers.impl.Skills;
import l2e.gameserver.handler.admincommandhandlers.impl.Spawn;
import l2e.gameserver.handler.admincommandhandlers.impl.SpecialRates;
import l2e.gameserver.handler.admincommandhandlers.impl.Summons;
import l2e.gameserver.handler.admincommandhandlers.impl.Targets;
import l2e.gameserver.handler.admincommandhandlers.impl.Teleports;
import l2e.gameserver.handler.admincommandhandlers.impl.TerritoryWar;
import l2e.gameserver.handler.admincommandhandlers.impl.Test;
import l2e.gameserver.handler.admincommandhandlers.impl.UnblockIp;
import l2e.gameserver.handler.admincommandhandlers.impl.Vitality;
import l2e.gameserver.handler.admincommandhandlers.impl.Zones;

public class AdminCommandHandler extends LoggerObject
{
	private final Map<String, IAdminCommandHandler> _handlers;

	public static AdminCommandHandler getInstance()
	{
		return SingletonHolder._instance;
	}

	protected AdminCommandHandler()
	{
		_handlers = new HashMap<>();

		registerHandler(new Admin());
		registerHandler(new AerialCleft());
		registerHandler(new Announcement());
		registerHandler(new Balancer());
		registerHandler(new BloodAltars());
		registerHandler(new Buffs());
		registerHandler(new Cache());
		registerHandler(new Camera());
		registerHandler(new ChangeAccessLevel());
		registerHandler(new CHSiege());
		registerHandler(new Clans());
		registerHandler(new CreateItem());
		registerHandler(new CursedWeapons());
		registerHandler(new Debug());
		registerHandler(new Delete());
		registerHandler(new Disconnect());
		registerHandler(new DoorControl());
		registerHandler(new EditChar());
		registerHandler(new EditNpc());
		registerHandler(new Effects());
		registerHandler(new Element());
		registerHandler(new Enchant());
		registerHandler(new Events());
		registerHandler(new ExpSp());
		if (Config.ALLOW_FAKE_PLAYERS)
		{
			registerHandler(new FakePlayers());
		}
		registerHandler(new FightCalculator());
		registerHandler(new FortSiege());
		registerHandler(new Hellbound());
		registerHandler(new Geodata());
		registerHandler(new Gm());
		registerHandler(new GmChat());
		registerHandler(new GraciaSeeds());
		registerHandler(new Heal());
		registerHandler(new HelpPage());
		registerHandler(new Instances());
		registerHandler(new InstanceZone());
		registerHandler(new Invul());
		registerHandler(new Kick());
		registerHandler(new Kill());
		registerHandler(new KrateisCube());
		registerHandler(new Level());
		registerHandler(new LogsViewer());
		registerHandler(new Mammon());
		registerHandler(new Manor());
		registerHandler(new Menu());
		registerHandler(new Messages());
		registerHandler(new MobGroups());
		registerHandler(new OlympiadMenu());
		registerHandler(new OnlineReward());
		registerHandler(new Packets());
		registerHandler(new PcCondOverrides());
		registerHandler(new Petition());
		registerHandler(new Pledge());
		registerHandler(new Polymorph());
		registerHandler(new Premium());
		registerHandler(new Punishment());
		registerHandler(new Quests());
		registerHandler(new RepairChar());
		registerHandler(new Res());
		registerHandler(new Ride());
		registerHandler(new Shop());
		registerHandler(new ShowQuests());
		registerHandler(new ShutdownMenu());
		registerHandler(new Siege());
		registerHandler(new Skills());
		registerHandler(new Spawn());
		registerHandler(new SpecialRates());
		registerHandler(new Summons());
		registerHandler(new Targets());
		registerHandler(new Teleports());
		registerHandler(new TerritoryWar());
		registerHandler(new Test());
		registerHandler(new UnblockIp());
		registerHandler(new Vitality());
		registerHandler(new Zones());
		if (Config.ALLOW_DAILY_TASKS)
		{
			registerHandler(new DailyTasks());
		}
		
		info("Loaded " + _handlers.size() + " AdminCommandHandlers");
	}
	
	public void registerHandler(IAdminCommandHandler handler)
	{
		final String[] ids = handler.getAdminCommandList();
		for (final String id : ids)
		{
			if (_handlers.containsKey(id))
			{
				_handlers.remove(id);
			}
			_handlers.put(id, handler);
		}
	}
	
	public IAdminCommandHandler getHandler(String adminCommand)
	{
		String command = adminCommand;
		if (adminCommand.indexOf(" ") != -1)
		{
			command = adminCommand.substring(0, adminCommand.indexOf(" "));
		}
		return _handlers.get(command);
	}
	
	public int size()
	{
		return _handlers.size();
	}

	private static class SingletonHolder
	{
		protected static final AdminCommandHandler _instance = new AdminCommandHandler();
	}
}