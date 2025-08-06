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

import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.instancemanager.SoDManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.QuestGuardInstance;
import l2e.gameserver.network.NpcStringId;
import l2e.scripts.ai.AbstractNpcAI;

/**
 * Created by LordWinter 25.12.2020
 */
public class SoDDefenceStage extends AbstractNpcAI
{
	private int controllerKills = 0;
	private int portalKills = 0;
	private static int defenceStage = 0;
	private long timeLimit = 0L;
	private ScheduledFuture<?> _respawnTime = null;
	private ScheduledFuture<?> _timeLimit = null;
	
	private SoDDefenceStage(String name, String descr)
	{
		super(name, descr);

		addKillId(18702, 18775);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("StartDefence"))
		{
			if (SoDManager.getInstance().isOpened())
			{
				timeLimit = ServerVariables.getLong("SoD_defence", 0) * 1000L;
				final int lastHours = (int) ((timeLimit - System.currentTimeMillis()) / 3600000);
				if (lastHours >= 10 && lastHours < 11)
				{
					defenceStage = 2;
				}
				else if (lastHours < 10)
				{
					defenceStage = 3;
				}
				else
				{
					defenceStage = 1;
				}
				SpawnParser.getInstance().spawnGroup("sod_defence_controller");
				printScreenMsg(NpcStringId.TIATS_FOLLOWERS_ARE_COMING_TO_RETAKE_THE_SEED_OF_DESTRUCTION_GET_READY_TO_STOP_THE_ENEMIES);
				checkStageStatus();
				if (_timeLimit != null)
				{
					_timeLimit.cancel(false);
					_timeLimit = null;
				}
				_timeLimit = ThreadPoolManager.getInstance().schedule(new ChangeToHardStatus(), (timeLimit - System.currentTimeMillis()));
			}
			return null;
		}
		else if (event.equalsIgnoreCase("EndDefence"))
		{
			controllerKills = 0;
			defenceStage = 0;
			
			if (_respawnTime != null)
			{
				_respawnTime.cancel(false);
				_respawnTime = null;
			}
			
			if (_timeLimit != null)
			{
				_timeLimit.cancel(false);
				_timeLimit = null;
			}
			SpawnParser.getInstance().despawnGroup("sod_defence_controller");
			SpawnParser.getInstance().despawnGroup("sod_defence_portal_1stage");
			SpawnParser.getInstance().despawnGroup("sod_defence_portal_2stage");
			SpawnParser.getInstance().despawnGroup("sod_defence_tiat");
			SoDManager.getInstance().closeSeed();
			return null;
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case 18775 :
				((QuestGuardInstance) npc).setPassive(false);
				break;
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() == 18702)
		{
			portalKills++;
			if (portalKills >= 3)
			{
				switch (defenceStage)
				{
					case 1 :
						defenceStage++;
						if (Rnd.chance(40))
						{
							defenceStage++;
						}
						break;
					case 2 :
						defenceStage++;
						break;
					case 3 :
					case 4 :
						break;
				}
				
				if (_respawnTime != null)
				{
					_respawnTime.cancel(false);
					_respawnTime = null;
				}
				_respawnTime = ThreadPoolManager.getInstance().schedule(new CheckPortalStatus(), calcRespawnTime());
			}
		}
		else if (npc.getId() == 18775)
		{
			controllerKills++;
			if (controllerKills >= 2)
			{
				notifyEvent("EndDefence", null, null);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public String onKillByMob(Npc npc, Npc killer)
	{
		if (npc.getId() == 18775)
		{
			controllerKills++;
			if (controllerKills >= 2)
			{
				notifyEvent("EndDefence", null, null);
			}
		}
		return super.onKillByMob(npc, killer);
	}
	
	private class ChangeToHardStatus implements Runnable
	{
		@Override
		public void run()
		{
			defenceStage = 4;
			if (_respawnTime != null)
			{
				_respawnTime.cancel(false);
				_respawnTime = null;
			}
			_respawnTime = ThreadPoolManager.getInstance().schedule(new CheckPortalStatus(), calcRespawnTime());
		}
	}
	
	private class CheckPortalStatus implements Runnable
	{
		@Override
		public void run()
		{
			portalKills = 0;
			printScreenMsg(NpcStringId.TIATS_FOLLOWERS_ARE_COMING_TO_RETAKE_THE_SEED_OF_DESTRUCTION_GET_READY_TO_STOP_THE_ENEMIES);
			checkStageStatus();
		}
	}
	
	private long calcRespawnTime()
	{
		switch (defenceStage)
		{
			case 1 :
				return Rnd.get(20, 40) * 60000;
			case 2 :
				return Rnd.get(30, 60) * 60000;
			case 3 :
				return Rnd.get(30, 180) * 60000;
			case 4 :
				return Rnd.get(2, 10) * 60000;
		}
		return 0;
	}
	
	private void checkStageStatus()
	{
		switch (defenceStage)
		{
			case 1 :
				SpawnParser.getInstance().spawnGroup("sod_defence_portal_1stage");
				SpawnParser.getInstance().spawnGroup("sod_defence_tiat");
				break;
			case 2 :
				SpawnParser.getInstance().spawnGroup("sod_defence_portal_1stage");
				break;
			case 3 :
			case 4 :
				SpawnParser.getInstance().despawnGroup("sod_defence_portal_2stage");
				SpawnParser.getInstance().spawnGroup("sod_defence_portal_2stage");
				break;
		}
	}
	
	private void printScreenMsg(NpcStringId stringId)
	{
		for (final Player player : ZoneManager.getInstance().getZoneById(60009).getPlayersInside())
		{
			if (player != null)
			{
				showOnScreenMsg(player, stringId, 2, 5000);
			}
		}
	}
	
	public static int getDefenceStage()
	{
		return defenceStage;
	}
	
	public static void main(String[] args)
	{
		new SoDDefenceStage(SoDDefenceStage.class.getSimpleName(), "ai");
	}
}
