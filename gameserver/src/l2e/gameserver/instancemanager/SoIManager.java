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

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;

/**
 * Created by LordWinter 27.10.2012 Based on L2J Eternity-World
 */
public class SoIManager extends LoggerObject
{
	private static final long SOI_OPEN_TIME = 24 * 60 * 60 * 1000L;
	
	private static final Location[] openSeedTeleportLocs =
	{
	        new Location(-179537, 209551, -15504), new Location(-179779, 212540, -15520), new Location(-177028, 211135, -15520), new Location(-176355, 208043, -15520), new Location(-179284, 205990, -15520), new Location(-182268, 208218, -15520), new Location(-182069, 211140, -15520), new Location(-176036, 210002, -11948), new Location(-176039, 208203, -11949), new Location(-183288, 208205, -11939), new Location(-183290, 210004, -11939), new Location(-187776, 205696, -9536), new Location(-186327, 208286, -9536), new Location(-184429, 211155, -9536), new Location(-182811, 213871, -9504), new Location(-180921, 216789, -9536), new Location(-177264, 217760, -9536), new Location(-173727, 218169, -9536)
	};

	public SoIManager()
	{
		info("Loaded. Current stage is: " + getCurrentStage());
		checkStageAndSpawn();
		if (isSeedOpen())
		{
			openSeed(getOpenedTime(), false);
		}
	}

	public int getCurrentStage()
	{
		return ServerVariables.getInt("SoI_stage", 1);
	}

	public long getOpenedTime()
	{
		if (getCurrentStage() != 3)
		{
			return 0;
		}
		return (ServerVariables.getLong("SoI_opened", 0) * 1000L) - System.currentTimeMillis();
	}

	public void setCurrentStage(int stage)
	{
		if (getCurrentStage() == stage)
		{
			return;
		}
		if (stage == 3)
		{
			openSeed(SOI_OPEN_TIME, true);
		}
		else if (isSeedOpen())
		{
			closeSeed();
		}
		ServerVariables.set("SoI_stage", stage);
		setCohemenesCount(0);
		setEkimusCount(0);
		setHoEDefCount(0);
		checkStageAndSpawn();
		info("Set to stage " + stage);
	}

	public boolean isSeedOpen()
	{
		return getOpenedTime() > 0;
	}

	public void openSeed(long time, boolean isOpenCleft)
	{
		if (time <= 0)
		{
			return;
		}
		ServerVariables.set("SoI_opened", (System.currentTimeMillis() + time) / 1000L);
		info("Opening the seed for " + Util.formatTime((int) time / 1000));
		spawnOpenedSeed();
		DoorParser.getInstance().getDoor(14240102).openMe();

		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				closeSeed();
				setCurrentStage(4);
			}
		}, time);
		
		if (isOpenCleft)
		{
			ThreadPoolManager.getInstance().schedule(() -> cleftActivate(), 3000);
		}
	}
	
	private void cleftActivate()
	{
		AerialCleftEvent.getInstance().openRegistration();
	}
	
	public void closeSeed()
	{
		info("Closing the seed.");
		ServerVariables.unset("SoI_opened");
		
		SpawnParser.getInstance().despawnGroup("soi_hos_middle_seeds");
		SpawnParser.getInstance().despawnGroup("soi_hoe_middle_seeds");
		SpawnParser.getInstance().despawnGroup("soi_hoi_middle_seeds");
		SpawnParser.getInstance().despawnGroup("soi_all_middle_stable_tumor");
		
		DoorParser.getInstance().getDoor(14240102).closeMe();
		for (final Player ch : ZoneManager.getInstance().getZoneById(200033).getPlayersInside())
		{
			if (ch != null)
			{
				ch.teleToLocation(-183285, 205996, -12896, true, ReflectionManager.DEFAULT);
			}
		}
	}

	public void checkStageAndSpawn()
	{
		SpawnParser.getInstance().despawnGroup("soi_world_closedmouths");
		SpawnParser.getInstance().despawnGroup("soi_world_mouths");
		SpawnParser.getInstance().despawnGroup("soi_world_abyssgaze2");
		SpawnParser.getInstance().despawnGroup("soi_world_abyssgaze1");
		
		switch (getCurrentStage())
		{
			case 1 :
			case 4 :
				SpawnParser.getInstance().spawnGroup("soi_world_mouths");
				SpawnParser.getInstance().spawnGroup("soi_world_abyssgaze2");
				break;
			case 5 :
				SpawnParser.getInstance().spawnGroup("soi_world_closedmouths");
				SpawnParser.getInstance().spawnGroup("soi_world_abyssgaze2");
				break;
			default :
				SpawnParser.getInstance().spawnGroup("soi_world_closedmouths");
				SpawnParser.getInstance().spawnGroup("soi_world_abyssgaze1");
				break;
		}
	}

	public void notifyCohemenesKill()
	{
		if (getCurrentStage() == 1)
		{
			if (getCohemenesCount() < 9)
			{
				setCohemenesCount(getCohemenesCount() + 1);
			}
			else
			{
				setCurrentStage(2);
			}
		}
	}

	public void notifyEkimusKill()
	{
		if (getCurrentStage() == 2)
		{
			if (getEkimusCount() < Config.SOI_EKIMUS_KILL_COUNT)
			{
				setEkimusCount(getEkimusCount() + 1);
			}
			else
			{
				setCurrentStage(3);
			}
		}
	}

	public void notifyHoEDefSuccess()
	{
		if (getCurrentStage() == 4)
		{
			if (getHoEDefCount() < 9)
			{
				setHoEDefCount(getHoEDefCount() + 1);
			}
			else
			{
				setCurrentStage(5);
			}
		}
	}

	public void setCohemenesCount(int i)
	{
		ServerVariables.set("SoI_CohemenesCount", i);
	}

	public void setEkimusCount(int i)
	{
		ServerVariables.set("SoI_EkimusCount", i);
	}

	public void setHoEDefCount(int i)
	{
		ServerVariables.set("SoI_hoedefkillcount", i);
	}

	public int getCohemenesCount()
	{
		return ServerVariables.getInt("SoI_CohemenesCount", 0);
	}

	public int getEkimusCount()
	{
		return ServerVariables.getInt("SoI_EkimusCount", 0);
	}

	public int getHoEDefCount()
	{
		return ServerVariables.getInt("SoI_hoedefkillcount", 0);
	}

	private void spawnOpenedSeed()
	{
		SpawnParser.getInstance().spawnGroup("soi_hos_middle_seeds");
		SpawnParser.getInstance().spawnGroup("soi_hoe_middle_seeds");
		SpawnParser.getInstance().spawnGroup("soi_hoi_middle_seeds");
		SpawnParser.getInstance().spawnGroup("soi_all_middle_stable_tumor");
	}

	public void teleportInSeed(Player player)
	{
		player.teleToLocation(openSeedTeleportLocs[Rnd.get(openSeedTeleportLocs.length)], false, ReflectionManager.DEFAULT);
	}
	
	public static final SoIManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SoIManager _instance = new SoIManager();
	}
}