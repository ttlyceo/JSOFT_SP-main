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

import java.util.concurrent.Future;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.MountType;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.zone.type.NoRestartZone;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.EarthQuake;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SpecialCamera;
import l2e.scripts.ai.AbstractNpcAI;

public class AntharasManager extends AbstractNpcAI
{
	private static final SkillHolder ANTH_ANTI_STRIDER = new SkillHolder(4258, 1);
	
	private GrandBossInstance _antharas = null;
	private Npc _cube = null;
	private Future<?> _cubeSpawnTask = null;
	private Future<?> _monsterSpawnTask = null;
	private Future<?> _activityCheckTask = null;
	private Future<?> _unlockTask = null;
	
	private long _LastAction = 0;
	
	private final NoRestartZone _zone = ZoneManager.getInstance().getZoneById(70050, NoRestartZone.class);
	
	private AntharasManager(String name, String descr)
	{
		super(name, descr);
		
		addAttackId(29068);
		addKillId(29068);
		
		int status = EpicBossManager.getInstance().getBossStatus(29068);
		if (status == 2)
		{
			final StatsSet info = EpicBossManager.getInstance().getStatsSet(29068);
			final int loc_x = info.getInteger("loc_x");
			final int loc_y = info.getInteger("loc_y");
			final int loc_z = info.getInteger("loc_z");
			final int heading = info.getInteger("heading");
			final int hp = info.getInteger("currentHP");
			final int mp = info.getInteger("currentMP");
			_antharas = (GrandBossInstance) addSpawn(29068, loc_x, loc_y, loc_z, heading, false, 0);
			EpicBossManager.getInstance().addBoss(_antharas);
			_antharas.setCurrentHpMp(hp, mp);
			_LastAction = System.currentTimeMillis();
			_activityCheckTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new CheckActivity(), 60000, 60000);
		}
		else if (status == 1)
		{
			setAntharasSpawnTask();
		}
		else if (status == 3)
		{
			final var info = EpicBossManager.getInstance().getStatsSet(29068);
			final var respawnTime = info.getLong("respawnTime");
			if (respawnTime <= System.currentTimeMillis())
			{
				EpicBossManager.getInstance().setBossStatus(29068, 0, false);
				status = 0;
			}
			else
			{
				if (_unlockTask != null)
				{
					_unlockTask.cancel(true);
					_unlockTask = null;
				}
				_unlockTask = ThreadPoolManager.getInstance().schedule(new UnlockAntharas(), respawnTime - System.currentTimeMillis());
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "waiting" :
			{
				setAntharasSpawnTask();
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	public void setAntharasSpawnTask()
	{
		if (_monsterSpawnTask == null)
		{
			synchronized (this)
			{
				if (_monsterSpawnTask == null)
				{
					EpicBossManager.getInstance().setBossStatus(29068, 1, true);
					_monsterSpawnTask = ThreadPoolManager.getInstance().schedule(new AntharasSpawn(1), (Config.ANTHARAS_WAIT_TIME * 60000));
				}
			}
		}
	}
	
	private class AntharasSpawn implements Runnable
	{
		private int _taskId = 0;
		
		public AntharasSpawn(int taskId)
		{
			_taskId = taskId;
		}
		
		@Override
		public void run()
		{
			switch (_taskId)
			{
				case 1 :
					_monsterSpawnTask.cancel(false);
					_monsterSpawnTask = null;
					_antharas = (GrandBossInstance) addSpawn(29068, 181323, 114850, -7623, 32542, false, 0, false, ReflectionManager.DEFAULT);
					EpicBossManager.getInstance().addBoss(_antharas);
					_antharas.block();
					
					EpicBossManager.getInstance().setBossStatus(29068, 2, true);
					_LastAction = System.currentTimeMillis();
					_activityCheckTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new CheckActivity(), 60000, 60000);
					ThreadPoolManager.getInstance().schedule(new AntharasSpawn(2), 16);
					break;
				case 2 :
					_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.enterMovieMode());
					broadcastPacket(new SpecialCamera(_antharas, 700, 13, -19, 0, 10000, 20000, 0, 0, 0, 0, 0), Config.ALLOW_ANTHARAS_MOVIE);
					ThreadPoolManager.getInstance().schedule(new AntharasSpawn(3), 3000);
					break;
				case 3 :
					broadcastPacket(new SpecialCamera(_antharas, 700, 13, 0, 6000, 10000, 20000, 0, 0, 0, 0, 0), Config.ALLOW_ANTHARAS_MOVIE);
					ThreadPoolManager.getInstance().schedule(new AntharasSpawn(4), 10000);
					break;
				case 4 :
					broadcastPacket(new SpecialCamera(_antharas, 3700, 0, -3, 0, 10000, 10000, 0, 0, 0, 0, 0), Config.ALLOW_ANTHARAS_MOVIE);
					ThreadPoolManager.getInstance().schedule(new AntharasSpawn(5), 200);
					break;
				
				case 5 :
					broadcastPacket(new SpecialCamera(_antharas, 1100, 0, -3, 22000, 10000, 30000, 0, 0, 0, 0, 0), Config.ALLOW_ANTHARAS_MOVIE);
					ThreadPoolManager.getInstance().schedule(new AntharasSpawn(6), 10800);
					break;
				case 6 :
					broadcastPacket(new SpecialCamera(_antharas, 1100, 0, -3, 300, 10000, 7000, 0, 0, 0, 0, 0), Config.ALLOW_ANTHARAS_MOVIE);
					ThreadPoolManager.getInstance().schedule(new AntharasSpawn(7), 1900);
					break;
				case 7 :
					_antharas.unblock();
					final var pos = new Location(179269, 114905, -7707, 0);
					_antharas.getAI().setIntention(CtrlIntention.MOVING, pos, 0);
					_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.leaveMovieMode());
					break;
				case 8 :
					if (_antharas != null)
					{
						_antharas.deleteMe();
						_antharas = null;
					}
					_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.leaveMovieMode());
					break;
			}
		}
	}
	
	protected void broadcastPacket(GameServerPacket mov, boolean isAllowMovie)
	{
		if (_zone != null && isAllowMovie)
		{
			_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.sendPacket(mov));
		}
	}
	
	protected class CheckActivity implements Runnable
	{
		@Override
		public void run()
		{
			if (_antharas == null)
			{
				return;
			}
			
			final var temp = (System.currentTimeMillis() - _LastAction);
			if (temp > 900000)
			{
				EpicBossManager.getInstance().setBossStatus(_antharas.getId(), 0, true);
				setUnspawn();
			}
		}
	}
	
	public void setUnspawn()
	{
		_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.teleToLocation(79800 + getRandom(600), 151200 + getRandom(1100), -3534, true, p.getReflection()));
		if (_cubeSpawnTask != null)
		{
			_cubeSpawnTask.cancel(true);
			_cubeSpawnTask = null;
		}
		if (_monsterSpawnTask != null)
		{
			_monsterSpawnTask.cancel(true);
			_monsterSpawnTask = null;
		}
		if (_activityCheckTask != null)
		{
			_activityCheckTask.cancel(false);
			_activityCheckTask = null;
		}
		if (_cube != null)
		{
			_cube.deleteMe();
			_cube = null;
		}
		
		if (_antharas != null)
		{
			_antharas.deleteMe();
			_antharas = null;
		}
	}
	
	private class CubeSpawn implements Runnable
	{
		private final int _type;
		
		public CubeSpawn(int type)
		{
			_type = type;
		}
		
		@Override
		public void run()
		{
			if (_type == 0)
			{
				_cube = addSpawn(31859, 177615, 114941, -7709, 32542, false, 0, false, ReflectionManager.DEFAULT);
				_cubeSpawnTask = ThreadPoolManager.getInstance().schedule(new CubeSpawn(1), 1800000);
			}
			else
			{
				setUnspawn();
			}
		}
	}
	
	private static class UnlockAntharas implements Runnable
	{
		@Override
		public void run()
		{
			EpicBossManager.getInstance().setBossStatus(29068, 0, true);
			GameObjectsStorage.getPlayers().stream().filter(p -> (p != null)).forEach(p -> p.sendPacket(new EarthQuake(185708, 114298, -8221, 20, 10)));
		}
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.getReflectionId() != 0)
		{
			return super.onAttack(npc, attacker, damage, isSummon, skill);
		}
		
		_LastAction = System.currentTimeMillis();
		if ((attacker.getMountType() == MountType.STRIDER) && (attacker.getFirstEffect(ANTH_ANTI_STRIDER.getId()) == null))
		{
			if (npc.checkDoCastConditions(ANTH_ANTI_STRIDER.getSkill(), false))
			{
				npc.setTarget(attacker);
				npc.doCast(ANTH_ANTI_STRIDER.getSkill());
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getReflectionId() != 0)
		{
			return super.onKill(npc, killer, isSummon);
		}
		_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.enterMovieMode());
		broadcastPacket(new SpecialCamera(_antharas, 1200, 20, -10, 0, 10000, 13000, 0, 0, 0, 0, 0), Config.ALLOW_ANTHARAS_MOVIE);
		npc.broadcastPacketToOthers(new PlaySound(1, "BS01_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		if (_activityCheckTask != null)
		{
			_activityCheckTask.cancel(false);
			_activityCheckTask = null;
		}
		_cubeSpawnTask = ThreadPoolManager.getInstance().schedule(new CubeSpawn(0), 10000);
		final long respawnTime = EpicBossManager.getInstance().setRespawnTime(29068, Config.ANTHARAS_RESPAWN_PATTERN);
		if (_unlockTask != null)
		{
			_unlockTask.cancel(true);
			_unlockTask = null;
		}
		_unlockTask = ThreadPoolManager.getInstance().schedule(new UnlockAntharas(), (respawnTime - System.currentTimeMillis()));
		broadcastPacket(new ExShowScreenMessage(NpcStringId.THE_EVIL_LAND_DRAGON_ANTHARAS_HAS_BEEN_DEFEATED, ExShowScreenMessage.TOP_CENTER, 30000), true);
		ThreadPoolManager.getInstance().schedule(new AntharasSpawn(8), 10000);
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public boolean unload(boolean removeFromList)
	{
		if (_unlockTask != null)
		{
			_unlockTask.cancel(true);
			_unlockTask = null;
		}
		setUnspawn();
		final int status = EpicBossManager.getInstance().getBossStatus(29068);
		if (status > 0 && status < 3)
		{
			EpicBossManager.getInstance().setBossStatus(29068, 0, true);
		}
		return super.unload(removeFromList);
	}
	
	public static void main(String[] args)
	{
		new AntharasManager(AntharasManager.class.getSimpleName(), "ai");
	}
}
