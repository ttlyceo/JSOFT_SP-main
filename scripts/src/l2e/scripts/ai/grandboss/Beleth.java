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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.SpecialCamera;
import l2e.scripts.ai.AbstractNpcAI;

/**
 * Updated by LordWinter 17.02.2020
 */
public class Beleth extends AbstractNpcAI
{
	private Npc _camera1, _camera2, _camera3, _camera4;
	private Npc _beleth, _vortex, _priest;
	private final ZoneType _zone = ZoneManager.getInstance().getZoneById(12018);
	private Player _killer;
	private Npc _stone;
	private final List<Spawner> _minions = new ArrayList<>();
	private final List<Location> _spawnBelethLocs = new ArrayList<>();
	private ScheduledFuture<?> _spawnTask = null;
	private ScheduledFuture<?> _activityCheckTask = null;
	private final Map<Npc, Location> _clonesLoc = new ConcurrentHashMap<>();
	private final Location[] _cloneLoc = new Location[56];
	private long _lastAction = 0;
	private ScheduledFuture<?> _intervalEndTask = null;
	private static long _enterTime = 0;
	
	private Beleth(String name, String descr)
	{
		super(name, descr);

		registerMobs(29118, 29119);
		addSpawnId(29118, 29119);
		addStartNpc(32470);
		addTalkId(32470);
		addFirstTalkId(29128);
		final StatsSet info = EpicBossManager.getInstance().getStatsSet(29118);
		final int status = EpicBossManager.getInstance().getBossStatus(29118);
		if (status == 3)
		{
			final long temp = (info.getLong("respawnTime") - System.currentTimeMillis());
			if (temp > 0)
			{
				if (Config.EPICBOSS_PRE_ANNOUNCE_LIST.containsKey(29118))
				{
					EpicBossManager.getInstance().addAnounceTask(29118, info.getLong("respawnTime"), (Config.EPICBOSS_PRE_ANNOUNCE_LIST.get(29118) * 60000L));
				}
				
				if (_intervalEndTask != null)
				{
					_intervalEndTask.cancel(false);
					_intervalEndTask = null;
				}
				_intervalEndTask = ThreadPoolManager.getInstance().schedule(new Unlock(), temp);
				DoorParser.getInstance().getDoor(20240001).closeMe();
			}
			else
			{
				EpicBossManager.getInstance().setBossStatus(29118, 0, true);
				DoorParser.getInstance().getDoor(20240001).openMe();
			}
		}
		else
		{
			EpicBossManager.getInstance().setBossStatus(29118, 0, false);
			DoorParser.getInstance().getDoor(20240001).openMe();
		}
	}

	private Npc spawn(int npcId, Location loc)
	{
		Npc result = null;
		try
		{
			final NpcTemplate template = NpcsParser.getInstance().getTemplate(npcId);
			if (template != null)
			{
				final Spawner spawn = new Spawner(template);
				spawn.setLocation(loc);
				spawn.setHeading(loc.getHeading());
				spawn.setAmount(1);
				if (npcId == 29119)
				{
					spawn.setRespawnDelay(Config.BELETH_CLONES_RESPAWN);
					spawn.startRespawn();
				}
				else
				{
					spawn.stopRespawn();
				}
				
				result = spawn.spawnOne(false);
				return result;
			}
		}
		catch (final Exception ignored)
		{}
		return null;
	}

	private class Unlock implements Runnable
	{
		@Override
		public void run()
		{
			EpicBossManager.getInstance().setBossStatus(29118, 0, true);
			DoorParser.getInstance().getDoor(20240001).openMe();
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
			try
			{
				switch (_taskId)
				{
					case 1 :
						_spawnTask.cancel(false);
						_spawnTask = null;
						for (final Creature npc : _zone.getCharactersInside())
						{
							if (npc != null && npc.isNpc())
							{
								npc.deleteMe();
							}
						}
						_camera1 = spawn(29120, new Location(16323, 213142, -9357, 0));
						_camera1.setIsSpecialCamera(true);
						_camera2 = spawn(29121, new Location(16323, 210741, -9357, 0));
						_camera2.setIsSpecialCamera(true);
						_camera3 = spawn(29122, new Location(16323, 213170, -9357, 0));
						_camera3.setIsSpecialCamera(true);
						_camera4 = spawn(29123, new Location(16323, 214917, -9356, 0));
						_camera4.setIsSpecialCamera(true);
						_zone.broadcastPacket(new PlaySound(1, "BS07_A", 1, _camera1.getObjectId(), _camera1.getX(), _camera1.getY(), _camera1.getZ()));
						showSocialActionMovie(_camera1, 1700, 110, 50, 0, 2600, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						showSocialActionMovie(_camera1, 1700, 100, 50, 0, 2600, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 300);
						break;
					case 2 :
						showSocialActionMovie(_camera1, 1800, -65, 30, 6000, 5000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 4900);
						break;
					case 3 :
						showSocialActionMovie(_camera1, 2200, -120, 30, 6000, 5000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 4900);
						break;
					case 4 :
						showSocialActionMovie(_camera2, 2200, 130, 20, 1000, 1500, -20, 10, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 1400);
						break;
					case 5 :
						showSocialActionMovie(_camera2, 2300, 100, 10, 2000, 4500, 0, 10, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 2500);
						break;
					case 6 :
						DoorParser.getInstance().getDoor(20240001).closeMe();
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 1700);
						break;
					case 7 :
						showSocialActionMovie(_camera4, 1500, 210, 5, 0, 1500, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						showSocialActionMovie(_camera4, 900, 255, 5, 5000, 6500, 0, 10, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 6000);
						break;
					case 8 :
						_vortex = spawn(29125, new Location(16323, 214917, -9356, 0));
						_vortex.setIsSpecialCamera(true);
						showSocialActionMovie(_camera4, 900, 255, 5, 0, 1500, 0, 5, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						_beleth = spawn(29118, new Location(16321, 214211, -9352, 49369));
						_beleth.setIsSpecialCamera(true);
						_beleth.setShowSummonAnimation(true);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 1000);
						break;
					case 9 :
						showSocialActionMovie(_camera4, 1100, 255, 10, 7000, 19000, 0, 20, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						_zone.broadcastPacket(new SocialAction(_beleth.getObjectId(), 1));
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 4000);
						break;
					case 10 :
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 200);
						break;
					case 11 :
						for (int i = 0; i < 6; i++)
						{
							final int x = (int) ((150 * Math.cos(i * 1.046666667)) + 16323);
							final int y = (int) ((150 * Math.sin(i * 1.046666667)) + 213059);
							final Npc minion = spawn(29119, new Location(x, y, -9357, 49152));
							minion.setShowSummonAnimation(true);
							minion.setIsSpecialCamera(true);
							minion.decayMe();
							_minions.add(minion.getSpawn());
						}
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 6800);
						break;
					case 12 :
						ThreadPoolManager.getInstance().schedule(new DoActionBeleth(0, new SkillHolder(5531, 1).getSkill()), 1000);
						showSocialActionMovie(_beleth, 0, 270, 5, 0, 6000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 5500);
						break;
					case 13 :
						showSocialActionMovie(_beleth, 800, 270, 10, 3000, 6000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 5000);
						break;
					case 14 :
						showSocialActionMovie(_camera3, 100, 270, 15, 0, 5000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						showSocialActionMovie(_camera3, 100, 270, 15, 0, 5000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 100);
						break;
					case 15 :
						showSocialActionMovie(_camera3, 100, 270, 15, 3000, 6000, 0, 5, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 1400);
						break;
					case 16 :
						_beleth.teleToLocation(16323, 213059, -9357, 49152, false, _beleth.getReflection());
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 200);
						break;
					case 17 :
						ThreadPoolManager.getInstance().schedule(new DoActionBeleth(0, new SkillHolder(5532, 1).getSkill()), 100);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 2000);
						break;
					case 18 :
						showSocialActionMovie(_camera3, 700, 270, 20, 1500, 8000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 6900);
						break;
					case 19 :
						showSocialActionMovie(_camera3, 40, 260, 15, 0, 4000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						for (final Spawner blth : _minions)
						{
							blth.spawnOne(false);
						}
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 3000);
						break;
					case 20 :
						showSocialActionMovie(_camera3, 40, 280, 15, 0, 4000, 5, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 3000);
						break;
					case 21 :
						showSocialActionMovie(_camera3, 5, 250, 15, 0, 13300, 20, 15, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 1000);
						break;
					case 22 :
						_zone.broadcastPacket(new SocialAction(_beleth.getObjectId(), 3));
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 4000);
						break;
					case 23 :
						ThreadPoolManager.getInstance().schedule(new DoActionBeleth(0, new SkillHolder(5533, 1).getSkill()), 100);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 6800);
						break;
					case 24 :
						if (_beleth != null)
						{
							_beleth.deleteMe();
						}
						
						for (final Spawner spawn : _minions)
						{
							if (spawn != null)
							{
								spawn.stopRespawn();
								final Npc bel = spawn.getLastSpawn();
								if (bel != null)
								{
									bel.deleteMe();
								}
							}
						}
						_minions.clear();
						ThreadPoolManager.getInstance().schedule(new SpawnTask(26), 10);
						ThreadPoolManager.getInstance().schedule(new ShowBeleth(), Config.BELETH_SPAWN_DELAY * 60000);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 2000);
						break;
					case 25 :
						_camera1.deleteMe();
						_camera2.deleteMe();
						_camera3.deleteMe();
						_camera4.deleteMe();
						for (final Npc clones : _clonesLoc.keySet())
						{
							_zone.broadcastPacket(new SocialAction(clones.getObjectId(), 0));
						}
						break;
					case 26 :
						for (int i = 0; i < 56; i++)
						{
							spawnClone(i);
						}
						break;
					case 27 :
						_beleth = spawn(29118, new Location(16323, 213170, -9357, 49152));
						_beleth.setIsInvul(true);
						_beleth.setIsSpecialCamera(true);
						_beleth.setIsImmobilized(true);
						_beleth.disableAllSkills();
						_priest = spawn(29128, new Location(_beleth));
						_priest.setIsSpecialCamera(true);
						_priest.setShowSummonAnimation(true);
						_priest.decayMe();
						break;
					case 28 :
						_beleth.doDie(null);
						_camera1 = spawn(29122, new Location(16323, 213170, -9357, 0));
						_camera1.setIsSpecialCamera(true);
						_camera1.broadcastPacket(new PlaySound(1, "BS07_D", 1, _camera1.getObjectId(), _camera1.getX(), _camera1.getY(), _camera1.getZ()));
						showSocialActionMovie(_camera1, 400, 290, 25, 0, 10000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						showSocialActionMovie(_camera1, 400, 290, 25, 0, 10000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						showSocialActionMovie(_camera1, 400, 110, 25, 4000, 10000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						_zone.broadcastPacket(new SocialAction(_beleth.getObjectId(), 5));
						for (final Creature npc : _zone.getCharactersInside())
						{
							if (npc != null && npc.getId() == 29119)
							{
								npc.deleteMe();
							}
						}
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 4000);
						break;
					case 29 :
						showSocialActionMovie(_camera1, 400, 295, 25, 4000, 5000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 4500);
						break;
					case 30 :
						showSocialActionMovie(_camera1, 400, 295, 10, 4000, 11000, 0, 25, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 9000);
						break;
					case 31 :
						_vortex.deleteMe();
						showSocialActionMovie(_camera1, 250, 90, 25, 0, 1000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						showSocialActionMovie(_camera1, 250, 90, 35, 0, 10000, 0, 0, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 2000);
						break;
					case 32 :
						_priest.spawnMe();
						if (_beleth != null)
						{
							_beleth.deleteMe();
						}
						_camera2 = spawn(29121, new Location(14056, 213170, -9357, 0));
						_camera2.setIsSpecialCamera(true);
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 3500);
						break;
					case 33 :
						showSocialActionMovie(_camera2, 800, 180, 0, 0, 4000, 0, 10, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						showSocialActionMovie(_camera2, 800, 180, 0, 0, 4000, 0, 10, 1, 0, 0, Config.ALLOW_BELETH_MOVIE);
						DoorParser.getInstance().getDoor(20240002).openMe();
						DoorParser.getInstance().getDoor(20240003).openMe();
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), 4000);
						break;
					case 34 :
						_camera1.deleteMe();
						_camera2.deleteMe();
						deleteAllClones();
						deleteBeleth();
						ThreadPoolManager.getInstance().schedule(new SpawnTask(_taskId + 1), Config.BELETH_ZONE_CLEAN_DELAY * 60 * 1000);
						break;
					case 35 :
						if (_spawnTask != null)
						{
							_spawnTask.cancel(true);
							_spawnTask = null;
						}
						
						if (_stone != null)
						{
							_stone.deleteMe();
						}
						DoorParser.getInstance().getDoor(20240002).closeMe();
						EpicBossManager.getInstance().getZone(12018).oustAllPlayers();
						break;
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
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
				setBelethSpawnTask();
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	public void setBelethSpawnTask()
	{
		if (_spawnTask == null)
		{
			synchronized (this)
			{
				if (_spawnTask == null)
				{
					EpicBossManager.getInstance().setBossStatus(29118, 2, true);
					_enterTime = System.currentTimeMillis() + (600 * 1000L);
					_spawnTask = ThreadPoolManager.getInstance().schedule(new SpawnTask(1), (Config.BELETH_SPAWN_DELAY * 60000));
					initSpawnLocs();
				}
			}
		}
	}
	
	public static boolean canTeleport()
	{
		return _enterTime > System.currentTimeMillis();
	}

	@Override
	public String onKill(final Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == 29118)
		{
			if (killer != null)
			{
				setBelethKiller(killer);
			}
			
			if (Config.ALLOW_BELETH_DROP_RING)
			{
				((Attackable) npc).dropSingleItem(_killer, 10314, 1);
				_killer = null;
			}
			
			final long respawnTime = EpicBossManager.getInstance().setRespawnTime(29118, Config.BELETH_RESPAWN_PATTERN);
			if (_intervalEndTask != null)
			{
				_intervalEndTask.cancel(false);
				_intervalEndTask = null;
			}
			_intervalEndTask = ThreadPoolManager.getInstance().schedule(new Unlock(), (respawnTime - System.currentTimeMillis()));
			deleteAllClones();
			if (_beleth != null)
			{
				_beleth.deleteMe();
			}
			_enterTime = 0L;
			ThreadPoolManager.getInstance().schedule(new SpawnTask(27), 1000);
			_stone = addSpawn(32470, new Location(12470, 215607, -9381, 49152));
			ThreadPoolManager.getInstance().schedule(new SpawnTask(28), 1500);
		}
		return super.onKill(npc, killer, isSummon);
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == 29118)
		{
			_lastAction = System.currentTimeMillis();
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}

	@Override
	public String onSpawn(Npc npc)
	{
		npc.setIsNoRndWalk(true);
		npc.setIsSpecialCamera(true);
		return super.onSpawn(npc);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String html;
		if ((_killer != null) && (player.getObjectId() == _killer.getObjectId()))
		{
			_killer = null;
			player.addItem("Kill Beleth", 10314, 1, null, true);
			html = "32470a.htm";
		}
		else
		{
			html = "32470b.htm";
		}
		return HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/default/" + html);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == 29128)
		{
			player.teleToLocation(-24095, 251617, -3374, true, player.getReflection());
			if (player.getSummon() != null)
			{
				player.getSummon().teleToLocation(-24095, 251617, -3374, true, player.getReflection());
			}
		}
		return null;
	}

	private void setBelethKiller(Player killer)
	{
		_killer = killer.getParty() != null ? killer.getParty().getCommandChannel() != null ? killer.getParty().getCommandChannel().getLeader() : killer.getParty().getLeader() : killer;
	}

	private void deleteAllClones()
	{
		if (_activityCheckTask != null)
		{
			_activityCheckTask.cancel(true);
			_activityCheckTask = null;
		}
		
		if (!_minions.isEmpty())
		{
			for (final Spawner clone : _minions)
			{
				if (clone != null)
				{
					clone.stopRespawn();
					final Npc bel = clone.getLastSpawn();
					if (bel != null)
					{
						bel.deleteMe();
					}
				}
			}
		}
		_minions.clear();
		_clonesLoc.clear();
	}

	private void deleteBeleth()
	{
		if (_beleth != null)
		{
			_beleth.abortCast();
			_beleth.setTarget(null);
			_beleth.getAI().setIntention(CtrlIntention.IDLE);
			_beleth.deleteMe();
			_beleth = null;
		}

		if (_vortex != null)
		{
			_vortex.deleteMe();
		}

		if (_camera1 != null)
		{
			_camera1.deleteMe();
		}

		if (_camera2 != null)
		{
			_camera2.deleteMe();
		}

		if (_camera3 != null)
		{
			_camera3.deleteMe();
		}

		if (_camera4 != null)
		{
			_camera4.deleteMe();
		}
		_spawnBelethLocs.clear();
	}

	private class ShowBeleth implements Runnable
	{
		@Override
		public void run()
		{
			Location spawn = null;
			if (_spawnBelethLocs != null && !_spawnBelethLocs.isEmpty())
			{
				spawn = _spawnBelethLocs.get(Rnd.get(_spawnBelethLocs.size())).rnd(null, 50, 100, true);
			}
			else
			{
				spawn = new Location(16323, 213059, -9357, 49152);
			}
			_beleth = spawn(29118, new Location(spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getHeading()));
			_lastAction = System.currentTimeMillis();
			_activityCheckTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new CheckActivity(), 60000, 60000);
		}
	}
	
	private class CheckActivity implements Runnable
	{
		@Override
		public void run()
		{
			final Long temp = (System.currentTimeMillis() - _lastAction);
			if (temp > 3600000 && _beleth != null)
			{
				EpicBossManager.getInstance().setBossStatus(_beleth.getId(), 0, true);
				setUnspawn();
			}
		}
	}
	
	private void setUnspawn()
	{
		_enterTime = 0L;
		deleteAllClones();
		deleteBeleth();
		if (_spawnTask != null)
		{
			_spawnTask.cancel(true);
			_spawnTask = null;
		}
		
		if (_activityCheckTask != null)
		{
			_activityCheckTask.cancel(true);
			_activityCheckTask = null;
		}
		DoorParser.getInstance().getDoor(20240002).closeMe();
		DoorParser.getInstance().getDoor(20240001).openMe();
		EpicBossManager.getInstance().getZone(12018).oustAllPlayers();
	}

	private class DoActionBeleth implements Runnable
	{
		private final int _socialAction;
		private final Skill _skill;

		public DoActionBeleth(final int socialAction, final Skill skill)
		{
			_socialAction = socialAction;
			_skill = skill;
		}

		@Override
		public void run()
		{
			if (_socialAction > 0)
			{
				_zone.broadcastPacket(new SocialAction(_beleth.getObjectId(), _socialAction));
			}
			if (_beleth != null && _skill != null)
			{
				_zone.broadcastPacket(new MagicSkillUse(_beleth, _beleth, _skill.getId(), 1, _skill.getHitTime(), 1));
			}
		}
	}

	private void spawnClone(int id)
	{
		final Npc clone = spawn(29119, new Location(_cloneLoc[id].getX(), _cloneLoc[id].getY(), -9353, 49152));
		if (clone != null)
		{
			_zone.broadcastPacket(new SocialAction(clone.getObjectId(), 0));
			_clonesLoc.put(clone, clone.getLocation());
			_spawnBelethLocs.add(clone.getLocation());
			_minions.add(clone.getSpawn());
		}
	}

	private void initSpawnLocs()
	{
		double angle = Math.toRadians(22.5);
		int radius = 700;

		for (int i = 0; i < 16; i++)
		{
			if ((i % 2) == 0)
			{
				radius -= 50;
			}
			else
			{
				radius += 50;
			}
			_cloneLoc[i] = new Location(16325 + (int) (radius * Math.sin(i * angle)), 213135 + (int) (radius * Math.cos(i * angle)), convertDegreeToClientHeading(270 - (i * 22.5)));
		}

		radius = 1340;
		angle = Math.asin(1 / Math.sqrt(3));
		int mulX = 1, mulY = 1, addH = 3;
		double decX = 1.0, decY = 1.0;
		for (int i = 0; i < 16; i++)
		{
			if ((i % 8) == 0)
			{
				mulX = 0;
			}
			else if (i < 8)
			{
				mulX = -1;
			}
			else
			{
				mulX = 1;
			}
			if ((i == 4) || (i == 12))
			{
				mulY = 0;
			}
			else if ((i > 4) && (i < 12))
			{
				mulY = -1;
			}
			else
			{
				mulY = 1;
			}
			if (((i % 8) == 1) || (i == 7) || (i == 15))
			{
				decX = 0.5;
			}
			else
			{
				decX = 1.0;
			}
			if (((i % 10) == 3) || (i == 5) || (i == 11))
			{
				decY = 0.5;
			}
			else
			{
				decY = 1.0;
			}
			if (((i + 2) % 4) == 0)
			{
				addH++;
			}
			_cloneLoc[i + 16] = new Location(16325 + (int) (radius * decX * mulX), 213135 + (int) (radius * decY * mulY), convertDegreeToClientHeading(180 + (addH * 90)));
		}

		angle = Math.toRadians(22.5);
		radius = 1000;
		for (int i = 0; i < 16; i++)
		{
			if ((i % 2) == 0)
			{
				radius -= 70;
			}
			else
			{
				radius += 70;
			}
			_cloneLoc[i + 32] = new Location(16325 + (int) (radius * Math.sin(i * angle)), 213135 + (int) (radius * Math.cos(i * angle)), _cloneLoc[i].getHeading());
		}

		int order = 48;
		radius = 650;
		for (int i = 1; i < 16; i += 2)
		{
			if ((i == 1) || (i == 15))
			{
				_cloneLoc[order] = new Location(_cloneLoc[i].getX(), _cloneLoc[i].getY() + radius, _cloneLoc[i + 16].getHeading());
			}
			else if ((i == 3) || (i == 5))
			{
				_cloneLoc[order] = new Location(_cloneLoc[i].getX() + radius, _cloneLoc[i].getY(), _cloneLoc[i].getHeading());
			}
			else if ((i == 7) || (i == 9))
			{
				_cloneLoc[order] = new Location(_cloneLoc[i].getX(), _cloneLoc[i].getY() - radius, _cloneLoc[i + 16].getHeading());
			}
			else if ((i == 11) || (i == 13))
			{
				_cloneLoc[order] = new Location(_cloneLoc[i].getX() - radius, _cloneLoc[i].getY(), _cloneLoc[i].getHeading());
			}
			order++;
		}
	}

	private void showSocialActionMovie(final Creature target, final int dist, final int yaw, final int pitch, final int time, final int duration, final int turn, final int rise, final int widescreen, int relAngle, final int unk, boolean isAllowMovie)
	{
		if (target == null || !isAllowMovie)
		{
			return;
		}
		_zone.broadcastPacket(new SpecialCamera(target, dist, yaw, pitch, time, duration, turn, rise, widescreen, relAngle, unk));
	}

	private static int convertDegreeToClientHeading(double degree)
	{
		if (degree < 0)
		{
			degree = 360 + degree;
		}
		return (int) (degree * 182.044444444);
	}
	
	@Override
	public boolean unload(boolean removeFromList)
	{
		setUnspawn();
		if (_intervalEndTask != null)
		{
			_intervalEndTask.cancel(false);
			_intervalEndTask = null;
		}
		final int status = EpicBossManager.getInstance().getBossStatus(29118);
		if (status > 0 && status < 3)
		{
			EpicBossManager.getInstance().setBossStatus(29118, 0, true);
		}
		return super.unload(removeFromList);
	}

	public static void main(String[] args)
	{
		new Beleth(Beleth.class.getSimpleName(), "ai");
	}
}
