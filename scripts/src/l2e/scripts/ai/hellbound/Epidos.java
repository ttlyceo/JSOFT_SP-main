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
package l2e.scripts.ai.hellbound;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.MinionList;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.MinionData;
import l2e.gameserver.model.actor.templates.npc.MinionTemplate;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.scripts.ai.AbstractNpcAI;

public class Epidos extends AbstractNpcAI
{
	private static final int[] EPIDOSES =
	{
	        25609, 25610, 25611, 25612
	};
	
	private static final int[] MINIONS =
	{
	        25605, 25606, 25607, 25608
	};
	
	private static final int[] MINIONS_COUNT =
	{
	        3, 6, 11
	};
	
	private static final int NAIA_CUBE = 32376;
	private final Map<Integer, Double> _lastHp = new ConcurrentHashMap<>();
	
	private Epidos(String name, String descr)
	{
		super(name, descr);
		
		addKillId(EPIDOSES);
		addSpawnId(EPIDOSES);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("check_minions"))
		{
			if ((getRandom(1000) > 250) && _lastHp.containsKey(npc.getObjectId()))
			{
				final int hpDecreasePercent = (int) (((_lastHp.get(npc.getObjectId()) - npc.getCurrentHp()) * 100) / npc.getMaxHp());
				int minionsCount = 0;
				int spawnedMinions = 0;
				final MinionList ml = npc.getMinionList();
				if (ml != null)
				{
					spawnedMinions = npc.getMinionList().getAliveMinions().size();
				}
				
				if ((hpDecreasePercent > 5) && (hpDecreasePercent <= 15) && (spawnedMinions <= 9))
				{
					minionsCount = MINIONS_COUNT[0];
				}
				else if ((((hpDecreasePercent > 1) && (hpDecreasePercent <= 5)) || ((hpDecreasePercent > 15) && (hpDecreasePercent <= 30))) && (spawnedMinions <= 6))
				{
					minionsCount = MINIONS_COUNT[1];
				}
				else if (spawnedMinions == 0)
				{
					minionsCount = MINIONS_COUNT[2];
				}
				
				for (int i = 0; i < minionsCount; i++)
				{
					npc.getMinionList().addMinion(new MinionData(new MinionTemplate(MINIONS[Arrays.binarySearch(EPIDOSES, npc.getId())], 1)), true);
				}
				_lastHp.put(npc.getObjectId(), npc.getCurrentHp());
			}
			startQuestTimer("check_minions", 10000, npc, null);
		}
		else if (event.equalsIgnoreCase("check_idle"))
		{
			if (npc.getAI().getIntention() == CtrlIntention.ACTIVE)
			{
				npc.deleteMe();
			}
			else
			{
				startQuestTimer("check_idle", 600000, npc, null);
			}
		}
		return null;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.isInsideRadius(-45474, 247450, -13994, 2000, true, false))
		{
			final Npc teleCube = addSpawn(NAIA_CUBE, -45482, 246277, -14184, 0, false, 0, false);
			teleCube.broadcastPacketToOthers(2000, new NpcSay(teleCube.getObjectId(), Say2.NPC_ALL, teleCube.getObjectId(), "Teleportation to Beleth Throne Room is available for 2 minutes."));
		}
		_lastHp.remove(npc.getObjectId());
		final MinionList ml = npc.getMinionList();
		if (ml != null)
		{
			ml.onMasterDelete();
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		startQuestTimer("check_minions", 10000, npc, null);
		startQuestTimer("check_idle", 600000, npc, null);
		_lastHp.put(npc.getObjectId(), npc.getMaxHp());
		
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new Epidos(Epidos.class.getSimpleName(), "ai");
	}
}
