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
package l2e.scripts.ai.grandboss;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.scripts.ai.AbstractNpcAI;

public class Core extends AbstractNpcAI
{
	private final List<Attackable> _minions = new CopyOnWriteArrayList<>();
	private GrandBossInstance _boss = null;
	
	private Core(String name, String descr)
	{
		super(name, descr);
		registerMobs(29006, 29007, 29008, 29011);
		
		final StatsSet info = EpicBossManager.getInstance().getStatsSet(29006);
		final int status = EpicBossManager.getInstance().getBossStatus(29006);
		if (status == 3)
		{
			final long temp = (info.getLong("respawnTime") - System.currentTimeMillis());
			
			if (temp > 0)
			{
				startQuestTimer("core_unlock", temp, null, null);
			}
			else
			{
				_boss = (GrandBossInstance) addSpawn(29006, 17726, 108915, -6480, 0, false, 0);
				EpicBossManager.getInstance().setBossStatus(29006, 0, false);
				spawnBoss(_boss);
			}
		}
		else
		{
			final int loc_x = info.getInteger("loc_x");
			final int loc_y = info.getInteger("loc_y");
			final int loc_z = info.getInteger("loc_z");
			final int heading = info.getInteger("heading");
			final int hp = info.getInteger("currentHP");
			final int mp = info.getInteger("currentMP");
			_boss = (GrandBossInstance) addSpawn(29006, loc_x, loc_y, loc_z, heading, false, 0);
			_boss.setCurrentHpMp(hp, mp);
			spawnBoss(_boss);
		}
	}
	
	public void spawnBoss(GrandBossInstance npc)
	{
		EpicBossManager.getInstance().addBoss(npc);
		npc.broadcastPacketToOthers(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		Attackable mob;
		for (int i = 0; i < 5; i++)
		{
			final int x = 16800 + (i * 360);
			mob = (Attackable) addSpawn(29007, x, 110000, npc.getZ(), 280 + getRandom(40), false, 0);
			mob.setIsRaidMinion(true);
			_minions.add(mob);
			mob = (Attackable) addSpawn(29007, x, 109000, npc.getZ(), 280 + getRandom(40), false, 0);
			mob.setIsRaidMinion(true);
			_minions.add(mob);
			final int x2 = 16800 + (i * 600);
			mob = (Attackable) addSpawn(29008, x2, 109300, npc.getZ(), 280 + getRandom(40), false, 0);
			mob.setIsRaidMinion(true);
			_minions.add(mob);
		}
		for (int i = 0; i < 4; i++)
		{
			final int x = 16800 + (i * 450);
			mob = (Attackable) addSpawn(29011, x, 110300, npc.getZ(), 280 + getRandom(40), false, 0);
			mob.setIsRaidMinion(true);
			_minions.add(mob);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("core_unlock"))
		{
			_boss = (GrandBossInstance) addSpawn(29006, 17726, 108915, -6480, 0, false, 0);
			EpicBossManager.getInstance().setBossStatus(29006, 0, true);
			spawnBoss(_boss);
		}
		else if (event.equalsIgnoreCase("spawn_minion"))
		{
			final Attackable mob = (Attackable) addSpawn(npc.getId(), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
			mob.setIsRaidMinion(true);
			_minions.add(mob);
		}
		else if (event.equalsIgnoreCase("despawn_minions"))
		{
			for (int i = 0; i < _minions.size(); i++)
			{
				final Attackable mob = _minions.get(i);
				if (mob != null)
				{
					mob.decayMe();
				}
			}
			_minions.clear();
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == 29006)
		{
			if (npc.isScriptValue(1))
			{
				if (getRandom(100) == 0)
				{
					npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.REMOVING_INTRUDERS));
				}
			}
			else
			{
				npc.setScriptValue(1);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.A_NON_PERMITTED_TARGET_HAS_BEEN_DISCOVERED));
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.INTRUDER_REMOVAL_SYSTEM_INITIATED));
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == 29006)
		{
			final int objId = npc.getObjectId();
			npc.broadcastPacketToOthers(2000, new PlaySound(1, "BS02_D", 1, objId, npc.getX(), npc.getY(), npc.getZ()));
			npc.broadcastPacketToOthers(2000, new NpcSay(objId, Say2.NPC_ALL, npcId, NpcStringId.A_FATAL_ERROR_HAS_OCCURRED));
			npc.broadcastPacketToOthers(2000, new NpcSay(objId, Say2.NPC_ALL, npcId, NpcStringId.SYSTEM_IS_BEING_SHUT_DOWN));
			npc.broadcastPacketToOthers(2000, new NpcSay(objId, Say2.NPC_ALL, npcId, NpcStringId.DOT_DOT_DOT_DOT_DOT_DOT));
			final long respawnTime = EpicBossManager.getInstance().setRespawnTime(29006, Config.CORE_RESPAWN_PATTERN);
			startQuestTimer("core_unlock", (respawnTime - System.currentTimeMillis()), null, null);
			startQuestTimer("despawn_minions", 20000, null, null);
			cancelQuestTimers("spawn_minion");
			_boss = null;
		}
		else if ((EpicBossManager.getInstance().getBossStatus(29006) == 0) && (_minions != null) && _minions.contains(npc))
		{
			_minions.remove(npc);
			startQuestTimer("spawn_minion", 60000, npc, null);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public boolean unload(boolean removeFromList)
	{
		if (_boss != null)
		{
			_boss.deleteMe();
			_boss = null;
		}
		if (_minions != null)
		{
			for (int i = 0; i < _minions.size(); i++)
			{
				final Attackable mob = _minions.get(i);
				if (mob != null)
				{
					mob.decayMe();
				}
			}
		}
		_minions.clear();
		cancelQuestTimers("core_unlock");
		return super.unload(removeFromList);
	}
	
	public static void main(String[] args)
	{
		new Core(Core.class.getSimpleName(), "ai");
	}
}