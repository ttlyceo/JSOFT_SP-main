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
package l2e.scripts.custom;

import java.util.concurrent.ScheduledFuture;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.zone.type.NoRestartZone;
import l2e.gameserver.network.serverpackets.SpecialCamera;
import l2e.scripts.ai.AbstractNpcAI;

/**
 * Updated by LordWinter 19.06.2021
 */
public final class Sailren extends AbstractNpcAI
{
	private final NoRestartZone zone = ZoneManager.getInstance().getZoneById(70049, NoRestartZone.class);

	private Status _status = Status.ALIVE;
	private int _killCount = 0;
	private long _lastAttack = 0;
	private ScheduledFuture<?> _monsterSpawnTask = null;
	private ScheduledFuture<?> _timeOutTask = null;
	private ScheduledFuture<?> _lastActionTask = null;
	private ScheduledFuture<?> _unlockTask = null;

	private static enum Status
	{
		ALIVE, IN_FIGHT, DEAD
	}

	private Sailren()
	{
		super(Sailren.class.getSimpleName(), "custom");

		addStartNpc(32109, 32107);
		addTalkId(32109, 32107);
		addFirstTalkId(32109);
		addKillId(22218, 22199, 22217, 29065);
		addAttackId(22218, 22199, 22217, 29065);

		final long remain = ServerVariables.getLong("SailrenRespawn", 0L) - System.currentTimeMillis();
		if (remain > 0)
		{
			_status = Status.DEAD;
			_unlockTask = ThreadPoolManager.getInstance().schedule(new UnlockSailren(), remain);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (npc == null)
		{
			return htmltext;
		}
		switch (event)
		{
			case "32109-01.htm":
			{
				htmltext = "32109-01.htm";
				break;
			}
			case "32109-01a.htm":
			{
				htmltext = "32109-01a.htm";
				break;
			}
			case "32109-02a.htm":
			{
				htmltext = "32109-02a.htm";
				break;
			}
			case "32109-03a.htm":
			{
				htmltext = "32109-03a.htm";
				break;
			}
			case "enter":
			{
				if (!player.isInParty())
				{
					htmltext = "32109-01.htm";
				}
				else if (_status == Status.DEAD)
				{
					htmltext = "32109-04.htm";
				}
				else if (_status == Status.IN_FIGHT)
				{
					htmltext = "32109-05.htm";
				}
				else if (!player.getParty().isLeader(player))
				{
					htmltext = "32109-03.htm";
				}
				else if (!hasQuestItems(player, 8784))
				{
					htmltext = "32109-02.htm";
				}
				else
				{
					takeItems(player, 1, 8784);
					_status = Status.IN_FIGHT;
					_lastAttack = System.currentTimeMillis();
					for (final Player member : player.getParty().getMembers())
					{
						if (member.isInsideRadius(npc, 1000, true, false))
						{
							member.teleToLocation(27549, -6638, -2008, true, member.getReflection());
						}
					}
					cleanUpStatus();
					_monsterSpawnTask = ThreadPoolManager.getInstance().schedule(new SpawnTask(1), 60000);
					_timeOutTask = ThreadPoolManager.getInstance().schedule(new TimeOut(), 3200000);
					_lastActionTask = ThreadPoolManager.getInstance().schedule(new LastAttack(), 120000);
				}
				break;
			}
			case "teleportOut":
			{
				player.teleToLocation(TeleportWhereType.TOWN, true, player.getReflection());
				break;
			}
			case "SPAWN_SAILREN":
			{
				final GrandBossInstance sailren = (GrandBossInstance) addSpawn(29065, 27549, -6638, -2008, 0, false, 0);
				final Npc movieNpc = addSpawn(32110, sailren.getX(), sailren.getY(), sailren.getZ() + 30, 0, false, 26000);
				sailren.setIsInvul(true);
				sailren.setIsImmobilized(true);
				zone.broadcastPacket(new SpecialCamera(movieNpc, 60, 110, 30, 4000, 1500, 20000, 0, 65, 1, 0, 0));

				startQuestTimer("ATTACK", 24600, sailren, null);
				startQuestTimer("ANIMATION", 2000, movieNpc, null);
				startQuestTimer("CAMERA_1", 4100, movieNpc, null);
				break;
			}
			case "ANIMATION":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(new SkillHolder(5090, 1).getSkill());
					startQuestTimer("ANIMATION", 2000, npc, null);
				}
				break;
			}
			case "CAMERA_1":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 100, 180, 30, 3000, 1500, 20000, 0, 50, 1, 0, 0));
				startQuestTimer("CAMERA_2", 3000, npc, null);
				break;
			}
			case "CAMERA_2":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 150, 270, 25, 3000, 1500, 20000, 0, 30, 1, 0, 0));
				startQuestTimer("CAMERA_3", 3000, npc, null);
				break;
			}
			case "CAMERA_3":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 160, 360, 20, 3000, 1500, 20000, 10, 15, 1, 0, 0));
				startQuestTimer("CAMERA_4", 3000, npc, null);
				break;
			}
			case "CAMERA_4":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 160, 450, 10, 3000, 1500, 20000, 0, 10, 1, 0, 0));
				startQuestTimer("CAMERA_5", 3000, npc, null);
				break;
			}
			case "CAMERA_5":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 160, 560, 0, 3000, 1500, 20000, 0, 10, 1, 0, 0));
				startQuestTimer("CAMERA_6", 7000, npc, null);
				break;
			}
			case "CAMERA_6":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 70, 560, 0, 500, 1500, 7000, -15, 20, 1, 0, 0));
				break;
			}
			case "ATTACK":
			{
				npc.setIsInvul(false);
				npc.setIsImmobilized(false);
				break;
			}
		}
		return htmltext;
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (zone.isCharacterInZone(attacker))
		{
			_lastAttack = System.currentTimeMillis();
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (zone.isCharacterInZone(killer))
		{
			switch (npc.getId())
			{
				case 29065 :
				{
					_status = Status.DEAD;
					addSpawn(32107, 27644, -6638, -2008, 0, false, 300000);
					final long respawnTime = EpicBossManager.getInstance().setRespawnTime(29065, Config.SAILREN_RESPAWN_PATTERN);
					ServerVariables.set("SailrenRespawn", String.valueOf(respawnTime));
					cleanUpStatus();
					_unlockTask = ThreadPoolManager.getInstance().schedule(new UnlockSailren(), respawnTime);
					_timeOutTask = ThreadPoolManager.getInstance().schedule(new TimeOut(), 300000);
					break;
				}
				case 22218 :
				{
					_killCount++;
					if (_killCount == 3)
					{
						final Attackable pterosaur = (Attackable) addSpawn(22199, 27313, -6766, -1975, 0, false, 0);
						attackPlayer(pterosaur, killer);
						_killCount = 0;
					}
					break;
				}
				case 22199 :
				{
					final Attackable trex = (Attackable) addSpawn(22217, 27313, -6766, -1975, 0, false, 0);
					attackPlayer(trex, killer);
					break;
				}
				case 22217 :
				{
					if (_monsterSpawnTask != null)
					{
						_monsterSpawnTask.cancel(true);
						_monsterSpawnTask = null;
					}
					_monsterSpawnTask = ThreadPoolManager.getInstance().schedule(new SpawnTask(2), 180000);
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}

	@Override
	public boolean unload(boolean removeFromList)
	{
		cleanUpStatus();
		if (_status == Status.IN_FIGHT)
		{
			_log.info(getClass().getSimpleName() + ": Script is being unloaded while Sailren is active, clearing zone.");
			_timeOutTask = ThreadPoolManager.getInstance().schedule(new TimeOut(), 100);
		}
		return super.unload(removeFromList);
	}
	
	private class UnlockSailren implements Runnable
	{
		@Override
		public void run()
		{
			_status = Status.ALIVE;
		}
	}
	
	private class SpawnTask implements Runnable
	{
		private int _taskId = 0;
		
		public SpawnTask(int taskId)
		{
			_taskId = taskId;
		}
		
		@Override
		public void run()
		{
			switch (_taskId)
			{
				case 1 :
					for (int i = 0; i < 3; i++)
					{
						addSpawn(22218, 27313 + getRandom(150), -6766 + getRandom(150), -1975, 0, false, 0);
					}
					break;
				case 2 :
					final GrandBossInstance sailren = (GrandBossInstance) addSpawn(29065, 27549, -6638, -2008, 0, false, 0);
					final Npc movieNpc = addSpawn(32110, sailren.getX(), sailren.getY(), sailren.getZ() + 30, 0, false, 26000);
					sailren.setIsInvul(true);
					sailren.setIsImmobilized(true);
					zone.broadcastPacket(new SpecialCamera(movieNpc, 60, 110, 30, 4000, 1500, 20000, 0, 65, 1, 0, 0));
					startQuestTimer("ATTACK", 24600, sailren, null);
					startQuestTimer("ANIMATION", 2000, movieNpc, null);
					startQuestTimer("CAMERA_1", 4100, movieNpc, null);
					break;
			}
		}
	}
	
	private class TimeOut implements Runnable
	{
		@Override
		public void run()
		{
			if (_status == Status.IN_FIGHT)
			{
				_status = Status.ALIVE;
			}
			
			for (final Creature character : zone.getCharactersInside())
			{
				if (character != null)
				{
					if (character.isPlayer())
					{
						character.teleToLocation(TeleportWhereType.TOWN, true, character.getReflection());
					}
					else if (character.isNpc())
					{
						character.deleteMe();
					}
				}
			}
		}
	}
	
	private class LastAttack implements Runnable
	{
		@Override
		public void run()
		{
			if (!zone.getPlayersInside().isEmpty() && ((_lastAttack + 600000) < System.currentTimeMillis()))
			{
				if (_timeOutTask != null)
				{
					_timeOutTask.cancel(true);
					_timeOutTask = null;
				}
				_timeOutTask = ThreadPoolManager.getInstance().schedule(new TimeOut(), 100);
			}
			else
			{
				if (_lastActionTask != null)
				{
					_lastActionTask.cancel(true);
					_lastActionTask = null;
				}
				_lastActionTask = ThreadPoolManager.getInstance().schedule(new LastAttack(), 120000);
			}
		}
	}
	
	private void cleanUpStatus()
	{
		_killCount = 0;
		if (_unlockTask != null)
		{
			_unlockTask.cancel(true);
			_unlockTask = null;
		}
		if (_monsterSpawnTask != null)
		{
			_monsterSpawnTask.cancel(true);
			_monsterSpawnTask = null;
		}
		
		if (_timeOutTask != null)
		{
			_timeOutTask.cancel(true);
			_timeOutTask = null;
		}
		
		if (_lastActionTask != null)
		{
			_lastActionTask.cancel(true);
			_lastActionTask = null;
		}
	}

	public static void main(String[] args)
	{
		new Sailren();
	}
}
