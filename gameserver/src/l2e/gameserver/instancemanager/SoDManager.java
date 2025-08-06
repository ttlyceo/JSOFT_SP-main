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
package l2e.gameserver.instancemanager;

import java.util.concurrent.ScheduledFuture;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.quest.Quest;

public class SoDManager extends LoggerObject
{
	private static long SOD_OPEN_TIME = 12 * 3600000L;
	private static long SOD_DEFENCE_TIME = 12 * 3600000L;
	private static final int _zone = 60009;
	private static boolean _isOpened = false;
	private ScheduledFuture<?> _openTimeTask = null;
	
	public SoDManager()
	{
		info("Loaded. Current stage is: " + getStage());
		if (!isAttackStage())
		{
			if (isDefenceStage())
			{
				startDefenceStage(false, false);
			}
			else
			{
				openSeed(getOpenedTimeLimit(), false);
			}
		}
	}
	
	private String getStage()
	{
		return isAttackStage() ? "1 (Attack)" : isDefenceStage() ? "3 (Defence)" : "2 (Open)";
	}
	
	public boolean isAttackStage()
	{
		return getOpenedTimeLimit() <= 0 && getDefenceStageTimeLimit() <= 0;
	}
	
	public boolean isDefenceStage()
	{
		return getDefenceStageTimeLimit() > 0;
	}
	
	public void addTiatKill()
	{
		if (!isAttackStage())
		{
			return;
		}
		if (getTiatKills() < Config.SOD_TIAT_KILL_COUNT)
		{
			ServerVariables.set("Tial_kills", getTiatKills() + 1);
		}
		else
		{
			openSeed(SOD_OPEN_TIME, true);
		}
	}
	
	public int getTiatKills()
	{
		return ServerVariables.getInt("Tial_kills", 0);
	}
	
	public long getDefenceStageTimeLimit()
	{
		return ServerVariables.getLong("SoD_defence", 0) * 1000L - System.currentTimeMillis();
	}
	
	public long getOpenedTimeLimit()
	{
		return ServerVariables.getLong("SoD_opened", 0) * 1000L - System.currentTimeMillis();
	}
	
	public void teleportIntoSeed(Player p)
	{
		p.teleToLocation(new Location(-245800, 220488, -12112), true, ReflectionManager.DEFAULT);
	}
	
	public void handleDoors(boolean doOpen)
	{
		for (int i = 12240003; i <= 12240031; i++)
		{
			final DoorInstance door = DoorParser.getInstance().getDoor(i);
			if (door != null)
			{
				if (doOpen)
				{
					door.openMe();
				}
				else
				{
					door.closeMe();
				}
			}
		}
	}
	
	public void openSeed(long timelimit, boolean isOpenCleft)
	{
		if (_isOpened)
		{
			return;
		}

		_isOpened = true;
		ServerVariables.unset("Tial_kills");
		ServerVariables.set("SoD_opened", (System.currentTimeMillis() + timelimit) / 1000L);
		info("Opening the seed for " + Util.formatTime((int) timelimit / 1000));
		SpawnParser.getInstance().spawnGroup("sod_free");
		handleDoors(true);
		
		_openTimeTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				startDefenceStage(true, true);
			}
		}, timelimit);
		
		if (isOpenCleft)
		{
			ThreadPoolManager.getInstance().schedule(() -> cleftActivate(), 3000);
		}
	}
	
	private void cleftActivate()
	{
		AerialCleftEvent.getInstance().openRegistration();
	}
	
	public void startDefenceStage(boolean refreshTime, boolean isOpenCleft)
	{
		if (_openTimeTask != null)
		{
			_openTimeTask.cancel(false);
			_openTimeTask = null;
		}
		
		if (!_isOpened)
		{
			ServerVariables.unset("Tial_kills");
			SpawnParser.getInstance().spawnGroup("sod_free");
			handleDoors(true);
		}
		_isOpened = true;
		if (isOpenCleft)
		{
			ThreadPoolManager.getInstance().schedule(() -> cleftActivate(), 3000);
		}
		
		final Quest qs = QuestManager.getInstance().getQuest("SoDDefenceStage");
		if (qs != null)
		{
			if (refreshTime)
			{
				ServerVariables.set("SoD_defence", (System.currentTimeMillis() + SOD_DEFENCE_TIME) / 1000L);
			}
			info("Seed in defence stage for " + Util.formatTime((int) getDefenceStageTimeLimit() / 1000));
			qs.notifyEvent("StartDefence", null, null);
		}
		else
		{
			closeSeed();
		}
	}
	
	public void closeSeed()
	{
		if (!_isOpened)
		{
			return;
		}
		
		if (_openTimeTask != null)
		{
			_openTimeTask.cancel(false);
			_openTimeTask = null;
		}
		
		_isOpened = false;
		info("Closing the seed.");
		ServerVariables.unset("SoD_opened");
		ServerVariables.unset("SoD_defence");
		SpawnParser.getInstance().despawnGroup("sod_free");
		for (final Player p : ZoneManager.getInstance().getZoneById(_zone).getPlayersInside())
		{
			if (p != null)
			{
				p.teleToLocation(-248717, 250260, 4337, true, ReflectionManager.DEFAULT);
			}
		}
		handleDoors(false);
	}

	public boolean isOpened()
	{
		return _isOpened;
	}
	
	public static final SoDManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SoDManager _instance = new SoDManager();
	}
}