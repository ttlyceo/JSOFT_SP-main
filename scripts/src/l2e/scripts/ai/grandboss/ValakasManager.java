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
import java.util.concurrent.Future;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.MountType;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.type.NoRestartZone;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.EarthQuake;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.scripts.ai.AbstractNpcAI;

public class ValakasManager extends AbstractNpcAI
{
	private static final Location TELEPORT_CUBE_LOCATIONS[] =
	{
	        new Location(214880, -116144, -1644), new Location(213696, -116592, -1644), new Location(212112, -116688, -1644), new Location(211184, -115472, -1664), new Location(210336, -114592, -1644), new Location(211360, -113904, -1644), new Location(213152, -112352, -1644), new Location(214032, -113232, -1644), new Location(214752, -114592, -1644), new Location(209824, -115568, -1421), new Location(210528, -112192, -1403), new Location(213120, -111136, -1408), new Location(215184, -111504, -1392), new Location(215456, -117328, -1392), new Location(213200, -118160, -1424)
	};
	
	private final NoRestartZone _zone = ZoneManager.getInstance().getZoneById(70052, NoRestartZone.class);
	private GrandBossInstance _valakas;
	private long _lastAttackTime = 0;
	
	private Future<?> _valakasSpawnTask = null;
	private Future<?> _intervalEndTask = null;
	private Future<?> _sleepCheckTask = null;
	
	private ValakasManager(String name, String descr)
	{
		super(name, descr);
		
		registerMobs(29028);
		
		final var info = EpicBossManager.getInstance().getStatsSet(29028);
		final int status = EpicBossManager.getInstance().getBossStatus(29028);
		
		if (status == 3)
		{
			final long temp = (info.getLong("respawnTime") - System.currentTimeMillis());
			if (temp > 0)
			{
				_intervalEndTask = ThreadPoolManager.getInstance().schedule(new IntervalEnd(), temp);
			}
			else
			{
				EpicBossManager.getInstance().setBossStatus(29028, 0, false);
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
			
			if (status == 2)
			{
				_valakas = (GrandBossInstance) addSpawn(29028, loc_x, loc_y, loc_z, heading, false, 0);
				EpicBossManager.getInstance().addBoss(_valakas);
				_valakas.setCurrentHpMp(hp, mp);
				_valakas.setRunning();
				_lastAttackTime = System.currentTimeMillis();
				_sleepCheckTask = ThreadPoolManager.getInstance().schedule(new CheckLastAttack(), 600000);
			}
			else
			{
				EpicBossManager.getInstance().setBossStatus(29028, 0, false);
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
				setValakasSpawnTask();
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isBlocked() || npc.getReflectionId() != 0)
		{
			return null;
		}
		
		if (attacker.getMountType() == MountType.STRIDER)
		{
			final Skill skill = SkillsParser.getInstance().getInfo(4258, 1);
			if (attacker.getFirstEffect(skill) == null)
			{
				npc.setTarget(attacker);
				npc.doCast(skill);
			}
		}
		_lastAttackTime = System.currentTimeMillis();
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == 29028 && npc.getReflectionId() == 0)
		{
			ThreadPoolManager.getInstance().schedule(new SpawnDespawn(12), 1);
			final long respawnTime = EpicBossManager.getInstance().setRespawnTime(29028, Config.VALAKAS_RESPAWN_PATTERN);
			_intervalEndTask = ThreadPoolManager.getInstance().schedule(new IntervalEnd(), (respawnTime - System.currentTimeMillis()));
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private class SpawnDespawn implements Runnable
	{
		private final int _taskId;
		private final List<Player> _players = _zone.getPlayersInside();
		
		public SpawnDespawn(int taskId)
		{
			_taskId = taskId;
		}
		
		@Override
		public void run()
		{
			switch (_taskId)
			{
				case 1 :
					_valakas = (GrandBossInstance) addSpawn(29028, 212852, -114842, -1632, 833, false, 0);
					EpicBossManager.getInstance().addBoss(_valakas);
					_valakas.block();
					_valakas.sendPacket(new PlaySound(1, "B03_A", 0, 0, 0, 0, 0));
					ThreadPoolManager.getInstance().schedule(new SpawnDespawn(2), 16);
					_lastAttackTime = System.currentTimeMillis();
					if (_valakasSpawnTask != null)
					{
						_valakasSpawnTask.cancel(false);
						_valakasSpawnTask = null;
					}
					break;
				case 2 :
					_valakas.broadcastPacket(new SocialAction(_valakas.getObjectId(), 1));
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						for (final Player pc : _players)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 1800, 180, -1, 1500, 15000, 0, 0, 1, 0);
						}
					}
					ThreadPoolManager.getInstance().schedule(new SpawnDespawn(Config.ALLOW_VALAKAS_MOVIE ? 3 : 11), 1500);
					break;
				case 3 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 1300, 180, -5, 3000, 15000, 0, -5, 1, 0));
						ThreadPoolManager.getInstance().schedule(new SpawnDespawn(4), 3300);
					}
					break;
				case 4 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 500, 180, -8, 600, 15000, 0, 60, 1, 0));
						ThreadPoolManager.getInstance().schedule(new SpawnDespawn(5), 2900);
					}
					break;
				case 5 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 800, 180, -8, 2700, 15000, 0, 30, 1, 0));
						ThreadPoolManager.getInstance().schedule(new SpawnDespawn(6), 2700);
					}
					break;
				case 6 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 200, 250, 70, 0, 15000, 30, 80, 1, 0));
						ThreadPoolManager.getInstance().schedule(new SpawnDespawn(7), 1);
					}
					break;
				case 7 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 1100, 250, 70, 2500, 15000, 30, 80, 1, 0));
						ThreadPoolManager.getInstance().schedule(new SpawnDespawn(8), 3200);
					}
					break;
				case 8 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 700, 150, 30, 0, 15000, -10, 60, 1, 0));
						ThreadPoolManager.getInstance().schedule(new SpawnDespawn(9), 1400);
					}
					break;
				case 9 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 1200, 150, 20, 2900, 15000, -10, 30, 1, 0));
						ThreadPoolManager.getInstance().schedule(new SpawnDespawn(10), 6700);
					}
					break;
				case 10 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 750, 170, -10, 3400, 15000, 10, -15, 1, 0));
						ThreadPoolManager.getInstance().schedule(new SpawnDespawn(11), 5700);
					}
					break;
				case 11 :
					_players.stream().filter(p -> (p != null)).forEach(p -> p.leaveMovieMode());
					_valakas.unblock();
					_lastAttackTime = System.currentTimeMillis();
					_players.stream().filter(p -> (p != null)).forEach(p -> p.sendPacket(new ExShowScreenMessage(NpcStringId.ARROGANT_FOOL_YOU_DARE_TO_CHALLENGE_ME_THE_RULER_OF_FLAMES_HERE_IS_YOUR_REWARD, 2, 8000)));
					EpicBossManager.getInstance().setBossStatus(29028, 2, true);
					if (_valakas.getAI().getIntention() == CtrlIntention.ACTIVE)
					{
						_valakas.getAI().setIntention(CtrlIntention.MOVING, new Location(Rnd.get(211080, 214909), Rnd.get(-115841, -112822), -1662), 0);
					}
					_sleepCheckTask = ThreadPoolManager.getInstance().schedule(new CheckLastAttack(), 600000);
					break;
				case 12 :
					_valakas.sendPacket(new PlaySound(1, "B03_D", 0, 0, 0, 0, 0));
					_players.stream().filter(p -> (p != null)).forEach(p -> p.sendPacket(new ExShowScreenMessage(NpcStringId.THE_EVIL_FIRE_DRAGON_VALAKAS_HAS_BEEN_DEFEATED, 2, 8000)));
					onValakasDie();
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						for (final Player pc : _players)
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 2000, 130, -1, 0, 15000, 0, 0, 1, 1);
						}
					}
					ThreadPoolManager.getInstance().schedule(new SpawnDespawn(13), 500);
					break;
				case 13 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 1100, 210, -5, 3000, 15000, -13, 0, 1, 1));
					}
					ThreadPoolManager.getInstance().schedule(new SpawnDespawn(14), 3500);
					break;
				case 14 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 1300, 200, -8, 3000, 15000, 0, 15, 1, 1));
					}
					ThreadPoolManager.getInstance().schedule(new SpawnDespawn(15), 4500);
					break;
				case 15 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 1000, 190, 0, 500, 15000, 0, 10, 1, 1));
					}
					ThreadPoolManager.getInstance().schedule(new SpawnDespawn(16), 500);
					break;
				case 16 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 1700, 120, 0, 2500, 15000, 12, 40, 1, 1));
					}
					ThreadPoolManager.getInstance().schedule(new SpawnDespawn(17), 4600);
					break;
				case 17 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 1700, 20, 0, 700, 15000, 10, 10, 1, 1));
					}
					ThreadPoolManager.getInstance().schedule(new SpawnDespawn(18), 750);
					break;
				case 18 :
					if (Config.ALLOW_VALAKAS_MOVIE)
					{
						_players.stream().filter(p -> (p != null)).forEach(p -> p.specialCamera(_valakas, 1700, 10, 0, 1000, 15000, 20, 70, 1, 1));
					}
					ThreadPoolManager.getInstance().schedule(new SpawnDespawn(19), 2500);
					break;
				case 19 :
					for (final var pc : _players)
					{
						pc.leaveMovieMode();
						final var buff = SkillsParser.getInstance().getInfo(23312, 1);
						if (buff != null)
						{
							buff.getEffects(pc, pc, false);
						}
					}
					break;
			}
		}
	}
	
	private class CheckLastAttack implements Runnable
	{
		@Override
		public void run()
		{
			if (EpicBossManager.getInstance().getBossStatus(29028) == 2)
			{
				if (_lastAttackTime + (20 * 60000) < System.currentTimeMillis())
				{
					sleep();
				}
				else
				{
					_sleepCheckTask = ThreadPoolManager.getInstance().schedule(new CheckLastAttack(), 60000);
				}
			}
		}
	}
	
	private void sleep()
	{
		setUnspawn();
		if (EpicBossManager.getInstance().getBossStatus(29028) != 0)
		{
			EpicBossManager.getInstance().setBossStatus(29028, 0, true);
		}
	}
	
	private void setUnspawn()
	{
		_zone.getPlayersInside().stream().filter(p -> (p != null)).forEach(p -> p.teleToLocation(150037 + getRandom(500), -57720 + getRandom(500), -2976, true, p.getReflection()));
		
		if (_valakas != null)
		{
			_valakas.deleteMe();
			_valakas = null;
		}
		
		if (_valakasSpawnTask != null)
		{
			_valakasSpawnTask.cancel(false);
			_valakasSpawnTask = null;
		}
		
		if (_sleepCheckTask != null)
		{
			_sleepCheckTask.cancel(false);
			_sleepCheckTask = null;
		}
		
		if (_intervalEndTask != null)
		{
			_intervalEndTask.cancel(false);
			_intervalEndTask = null;
		}
	}
	
	private void onValakasDie()
	{
		for (final var loc : TELEPORT_CUBE_LOCATIONS)
		{
			addSpawn(31759, loc, false, 900000);
		}
	}
	
	private synchronized void setValakasSpawnTask()
	{
		if (_valakasSpawnTask == null)
		{
			EpicBossManager.getInstance().setBossStatus(29028, 1, true);
			_valakasSpawnTask = ThreadPoolManager.getInstance().schedule(new SpawnDespawn(1), Config.VALAKAS_WAIT_TIME * 60000);
		}
	}
	
	private class IntervalEnd implements Runnable
	{
		@Override
		public void run()
		{
			EpicBossManager.getInstance().setBossStatus(29028, 0, true);
			for (final var p : GameObjectsStorage.getPlayers())
			{
				p.broadcastPacket(new EarthQuake(213896, -115436, -1644, 20, 10));
			}
		}
	}
	
	@Override
	public boolean unload(boolean removeFromList)
	{
		setUnspawn();
		final int status = EpicBossManager.getInstance().getBossStatus(29028);
		if (status > 0 && status < 3)
		{
			EpicBossManager.getInstance().setBossStatus(29028, 0, true);
		}
		return super.unload(removeFromList);
	}
	
	public static void main(String[] args)
	{
		new ValakasManager(ValakasManager.class.getSimpleName(), "ai");
	}
}
