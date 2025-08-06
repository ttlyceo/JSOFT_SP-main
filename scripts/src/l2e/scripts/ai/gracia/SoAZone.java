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
package l2e.scripts.ai.gracia;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.time.cron.SchedulingPattern;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.model.zone.type.EffectZone;
import l2e.scripts.ai.AbstractNpcAI;

public class SoAZone extends AbstractNpcAI
{
	private static final Map<Integer, Location> _teleportZones = new HashMap<>();
	
	private static final int[] ZONE_BUFFS =
	{
	        0, 6443, 6444, 6442
	};

	private static final int[][] ZONE_BUFFS_LIST =
	{
	        {
	                1, 2, 3
			},
	        {
	                2, 3, 1
			},
	        {
	                3, 1, 2
			}
	};
	
	private final SeedRegion[] _regionsData = new SeedRegion[3];
	static
	{
		_teleportZones.put(60002, new Location(-213175, 182648, -10992));
		_teleportZones.put(60003, new Location(-181217, 186711, -10528));
		_teleportZones.put(60004, new Location(-180211, 182984, -15152));
		_teleportZones.put(60005, new Location(-179275, 186802, -10720));
	}
	
	private ScheduledFuture<?> _changeTask = null;
	
	private SoAZone(String name, String descr)
	{
		super(name, descr);
		
		loadSeedRegionData();
		for (final int i : _teleportZones.keySet())
		{
			addEnterZoneId(i);
		}

		addStartNpc(32739);
		addTalkId(32739);
		startEffectZonesControl();
	}
	
	public void loadSeedRegionData()
	{
		_regionsData[0] = new SeedRegion(60006, new int[][]
		{
		        {
		                -180450, 185507, -10544, 11632
				},
				{
				        -180005, 185489, -10544, 11632
				}
		});
		
		_regionsData[1] = new SeedRegion(60007, new int[][]
		{
		        {
		                -179600, 186998, -10704, 11632
				},
				{
				        -179295, 186444, -10704, 11632
				}
		});
		
		_regionsData[2] = new SeedRegion(60008, new int[][]
		{
		        {
		                -180971, 186361, -10528, 11632
				},
				{
				        -180758, 186739, -10528, 11632
				}
		});
		
		final var oldTask = _changeTask;
		if (oldTask != null)
		{
			_changeTask = null;
			oldTask.cancel(false);
		}
		
		int buffsNow;
		final long lastUpdate = ServerVariables.getLong("SOAStatusChange", 0);
		if (lastUpdate < System.currentTimeMillis())
		{
			final var oldBuff = ServerVariables.getInt("SOABuffList", 0);
			buffsNow = getRandomList(oldBuff);
			final long newTime = new SchedulingPattern(Config.SOA_CHANGE_ZONE_TIME).next(System.currentTimeMillis());
			ServerVariables.set("SOAStatusChange", newTime);
			ServerVariables.set("SOABuffList", buffsNow);
			_changeTask = ThreadPoolManager.getInstance().schedule(() -> changeStatus(buffsNow), (newTime - System.currentTimeMillis()));
		}
		else
		{
			buffsNow = ServerVariables.getInt("SOABuffList", 0);
			_changeTask = ThreadPoolManager.getInstance().schedule(() -> changeStatus(buffsNow), (lastUpdate - System.currentTimeMillis()));
		}
		for (int i = 0; i < _regionsData.length; i++)
		{
			_regionsData[i].activeBuff = ZONE_BUFFS_LIST[buffsNow][i];
		}
	}
	
	private int getRandomList(int val)
	{
		final int buffsNow = getRandom(ZONE_BUFFS_LIST.length);
		if (buffsNow != val)
		{
			return buffsNow;
		}
		return getRandomList(val);
	}
	
	private void startEffectZonesControl()
	{
		for (int i = 0; i < _regionsData.length; i++)
		{
			for (int j = 0; j < _regionsData[i].af_spawns.length; j++)
			{
				_regionsData[i].af_npcs[j] = addSpawn(18928, _regionsData[i].af_spawns[j][0], _regionsData[i].af_spawns[j][1], _regionsData[i].af_spawns[j][2], _regionsData[i].af_spawns[j][3], false, 0);
				_regionsData[i].af_npcs[j].setDisplayEffect(_regionsData[i].activeBuff);
			}
			final var zone = ZoneManager.getInstance().getZoneById(_regionsData[i].buff_zone, EffectZone.class);
			if (zone != null)
			{
				zone.addSkill(ZONE_BUFFS[_regionsData[i].activeBuff], 1);
			}
		}
	}
	
	private void changeStatus(int oldBuff)
	{
		final var oldTask = _changeTask;
		if (oldTask != null)
		{
			_changeTask = null;
			oldTask.cancel(false);
		}
		
		final int buffsNow = getRandomList(oldBuff);
		ServerVariables.set("SOABuffList", buffsNow);
		final long newTime = new SchedulingPattern(Config.SOA_CHANGE_ZONE_TIME).next(System.currentTimeMillis());
		ServerVariables.set("SOAStatusChange", newTime);
		for (int i = 0; i < _regionsData.length; i++)
		{
			_regionsData[i].activeBuff = ZONE_BUFFS_LIST[buffsNow][i];
			
			for (final Npc af : _regionsData[i].af_npcs)
			{
				af.setDisplayEffect(_regionsData[i].activeBuff);
			}
			
			final EffectZone zone = ZoneManager.getInstance().getZoneById(_regionsData[i].buff_zone, EffectZone.class);
			if (zone != null)
			{
				zone.clearSkills();
				zone.addSkill(ZONE_BUFFS[_regionsData[i].activeBuff], 1);
			}
		}
		_changeTask = ThreadPoolManager.getInstance().schedule(() -> changeStatus(buffsNow), (newTime - System.currentTimeMillis()));
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("transform"))
		{
			if (player.getFirstEffect(6408) != null)
			{
				npc.showChatWindow(player, 2);
			}
			else
			{
				npc.setTarget(player);
				npc.doCast(SkillsParser.getInstance().getInfo(6408, 1));
				npc.doCast(SkillsParser.getInstance().getInfo(6649, 1));
				npc.showChatWindow(player, 1);
			}
		}
		return null;
	}
	
	@Override
	public String onEnterZone(Creature character, ZoneType zone)
	{
		if (_teleportZones.containsKey(zone.getId()))
		{
			final var teleLoc = _teleportZones.get(zone.getId());
			if (teleLoc != null)
			{
				character.teleToLocation(teleLoc, false, character.getReflection());
			}
		}
		return super.onEnterZone(character, zone);
	}
	
	private class SeedRegion
	{
		public int buff_zone;
		public int[][] af_spawns;
		public Npc[] af_npcs = new Npc[2];
		public int activeBuff = 0;
		
		public SeedRegion(int bz, int[][] as)
		{
			buff_zone = bz;
			af_spawns = as;
		}
	}
	
	public static void main(String[] args)
	{
		new SoAZone(SoAZone.class.getSimpleName(), "ai");
	}
}
