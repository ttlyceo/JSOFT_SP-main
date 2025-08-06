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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.DoorParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.tasks.FourSepulchersChangeAttackTimeTask;
import l2e.gameserver.instancemanager.tasks.FourSepulchersChangeCoolDownTimeTask;
import l2e.gameserver.instancemanager.tasks.FourSepulchersChangeEntryTimeTask;
import l2e.gameserver.instancemanager.tasks.FourSepulchersChangeWarmUpTimeTask;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.SepulcherMonsterInstance;
import l2e.gameserver.model.actor.instance.SepulcherNpcInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public final class FourSepulchersManager extends LoggerObject
{
	private static final int QUEST_ID = 620;

	private static final int ENTRANCE_PASS = 7075;
	private static final int USED_PASS = 7261;
	private static final int CHAPEL_KEY = 7260;
	private static final int ANTIQUE_BROOCH = 7262;
	
	private boolean _firstTimeRun;
	private boolean _inEntryTime = false;
	private boolean _inWarmUpTime = false;
	private boolean _inAttackTime = false;
	private boolean _inCoolDownTime = false;
	
	private ScheduledFuture<?> _changeCoolDownTimeTask = null;
	private ScheduledFuture<?> _changeEntryTimeTask = null;
	private ScheduledFuture<?> _changeWarmUpTimeTask = null;
	private ScheduledFuture<?> _changeAttackTimeTask = null;

	private final int[][] _startHallSpawn =
	{
	        {
	                181632, -85587, -7218
			},
			{
			        179963, -88978, -7218
			},
			{
			        173217, -86132, -7218
			},
			{
			        175608, -82296, -7218
			}
	};

	private final int[][][] _shadowSpawnLoc =
	{
	        {
	                {
	                        25339, 191231, -85574, -7216, 33380
					},
					{
					        25349, 189534, -88969, -7216, 32768
					},
					{
					        25346, 173195, -76560, -7215, 49277
					},
					{
					        25342, 175591, -72744, -7215, 49317
					}
			},
			{
			        {
			                25342, 191231, -85574, -7216, 33380
					},
					{
					        25339, 189534, -88969, -7216, 32768
					},
					{
					        25349, 173195, -76560, -7215, 49277
					},
					{
					        25346, 175591, -72744, -7215, 49317
					}
			},
			{
			        {
			                25346, 191231, -85574, -7216, 33380
					},
					{
					        25342, 189534, -88969, -7216, 32768
					},
					{
					        25339, 173195, -76560, -7215, 49277
					},
					{
					        25349, 175591, -72744, -7215, 49317
					}
			},
			{
			        {
			                25349, 191231, -85574, -7216, 33380
					},
					{
					        25346, 189534, -88969, -7216, 32768
					},
					{
					        25342, 173195, -76560, -7215, 49277
					},
					{
					        25339, 175591, -72744, -7215, 49317
					}
			},
	};

	protected Map<Integer, Boolean> _archonSpawned = new ConcurrentHashMap<>();
	protected Map<Integer, Boolean> _hallInUse = new ConcurrentHashMap<>();
	protected Map<Integer, Player> _challengers = new ConcurrentHashMap<>();
	protected Map<Integer, int[]> _startHallSpawns = new HashMap<>();
	protected Map<Integer, Integer> _hallGateKeepers = new HashMap<>();
	protected Map<Integer, Integer> _keyBoxNpc = new HashMap<>();
	protected Map<Integer, Integer> _victim = new HashMap<>();
	protected Map<Integer, Spawner> _executionerSpawns = new HashMap<>();
	protected Map<Integer, Spawner> _keyBoxSpawns = new HashMap<>();
	protected Map<Integer, Spawner> _mysteriousBoxSpawns = new HashMap<>();
	protected Map<Integer, Spawner> _shadowSpawns = new HashMap<>();
	protected Map<Integer, List<Spawner>> _dukeFinalMobs = new HashMap<>();
	protected Map<Integer, List<SepulcherMonsterInstance>> _dukeMobs = new HashMap<>();
	protected Map<Integer, List<Spawner>> _emperorsGraveNpcs = new HashMap<>();
	protected Map<Integer, List<Spawner>> _magicalMonsters = new HashMap<>();
	protected Map<Integer, List<Spawner>> _physicalMonsters = new HashMap<>();
	protected Map<Integer, List<SepulcherMonsterInstance>> _viscountMobs = new HashMap<>();

	protected List<Spawner> _physicalSpawns;
	protected List<Spawner> _magicalSpawns;
	protected List<Spawner> _managers = new CopyOnWriteArrayList<>();
	protected List<Spawner> _dukeFinalSpawns;
	protected List<Spawner> _emperorsGraveSpawns;
	protected List<Npc> _allMobs = new CopyOnWriteArrayList<>();

	private long _attackTimeEnd = 0;
	private long _coolDownTimeEnd = 0;
	private long _entryTimeEnd = 0;
	private long _warmUpTimeEnd = 0;

	private final byte _newCycleMin = 55;

	public void init()
	{
		if (_changeCoolDownTimeTask != null)
		{
			_changeCoolDownTimeTask.cancel(true);
		}
		if (_changeEntryTimeTask != null)
		{
			_changeEntryTimeTask.cancel(true);
		}
		if (_changeWarmUpTimeTask != null)
		{
			_changeWarmUpTimeTask.cancel(true);
		}
		if (_changeAttackTimeTask != null)
		{
			_changeAttackTimeTask.cancel(true);
		}

		_changeCoolDownTimeTask = null;
		_changeEntryTimeTask = null;
		_changeWarmUpTimeTask = null;
		_changeAttackTimeTask = null;

		_inEntryTime = false;
		_inWarmUpTime = false;
		_inAttackTime = false;
		_inCoolDownTime = false;

		_firstTimeRun = true;
		initFixedInfo();
		loadMysteriousBox();
		initKeyBoxSpawns();
		loadPhysicalMonsters();
		loadMagicalMonsters();
		initLocationShadowSpawns();
		initExecutionerSpawns();
		loadDukeMonsters();
		loadEmperorsGraveMonsters();
		timeSelector();
		spawnManagers();
		info("Loaded all functions.");
	}

	protected void timeSelector()
	{
		timeCalculator();
		final var currentTime = Calendar.getInstance().getTimeInMillis();

		if ((currentTime >= _coolDownTimeEnd) && (currentTime < _entryTimeEnd))
		{
			clean();
			_changeEntryTimeTask = ThreadPoolManager.getInstance().schedule(new FourSepulchersChangeEntryTimeTask(), 0);
			if (Config.DEBUG)
			{
				info("Beginning in Entry time");
			}
		}
		else if ((currentTime >= _entryTimeEnd) && (currentTime < _warmUpTimeEnd))
		{
			clean();
			_changeWarmUpTimeTask = ThreadPoolManager.getInstance().schedule(new FourSepulchersChangeWarmUpTimeTask(), 0);
			if (Config.DEBUG)
			{
				info("Beginning in WarmUp time");
			}
		}
		else if ((currentTime >= _warmUpTimeEnd) && (currentTime < _attackTimeEnd))
		{
			clean();
			_changeAttackTimeTask = ThreadPoolManager.getInstance().schedule(new FourSepulchersChangeAttackTimeTask(), 0);
			if (Config.DEBUG)
			{
				info("Beginning in Attack time");
			}
		}
		else
		{
			_changeCoolDownTimeTask = ThreadPoolManager.getInstance().schedule(new FourSepulchersChangeCoolDownTimeTask(), 0);
			if (Config.DEBUG)
			{
				info("Beginning in Cooldown time");
			}
		}
	}

	protected void timeCalculator()
	{
		final var tmp = Calendar.getInstance();
		if (tmp.get(Calendar.MINUTE) < _newCycleMin)
		{
			tmp.set(Calendar.HOUR, Calendar.getInstance().get(Calendar.HOUR) - 1);
		}
		tmp.set(Calendar.MINUTE, _newCycleMin);
		_coolDownTimeEnd = tmp.getTimeInMillis();
		_entryTimeEnd = _coolDownTimeEnd + (Config.FS_TIME_ENTRY * 60000L);
		_warmUpTimeEnd = _entryTimeEnd + (Config.FS_TIME_WARMUP * 60000L);
		_attackTimeEnd = _warmUpTimeEnd + (Config.FS_TIME_ATTACK * 60000L);
	}

	public void clean()
	{
		for (var i = 31921; i < 31925; i++)
		{
			final var Location = _startHallSpawns.get(i);
			final var zone = EpicBossManager.getInstance().getZone(Location[0], Location[1], Location[2]);
			if (zone != null)
			{
				zone.oustAllPlayers();
			}
		}

		deleteAllMobs();

		closeAllDoors();

		_hallInUse.clear();
		_hallInUse.put(31921, false);
		_hallInUse.put(31922, false);
		_hallInUse.put(31923, false);
		_hallInUse.put(31924, false);

		if (_archonSpawned.size() != 0)
		{
			final Set<Integer> npcIdSet = _archonSpawned.keySet();
			for (final int npcId : npcIdSet)
			{
				_archonSpawned.put(npcId, false);
			}
		}
	}

	protected void spawnManagers()
	{
		int i = 31921;
		for (Spawner spawnDat; i <= 31924; i++)
		{
			if ((i < 31921) || (i > 31924))
			{
				continue;
			}
			final var template1 = NpcsParser.getInstance().getTemplate(i);
			if (template1 == null)
			{
				continue;
			}
			try
			{
				spawnDat = new Spawner(template1);

				spawnDat.setAmount(1);
				spawnDat.setRespawnDelay(60);
				switch (i)
				{
					case 31921 :
						spawnDat.setX(181061);
						spawnDat.setY(-85595);
						spawnDat.setZ(-7200);
						spawnDat.setHeading(-32584);
						break;
					case 31922 :
						spawnDat.setX(179292);
						spawnDat.setY(-88981);
						spawnDat.setZ(-7200);
						spawnDat.setHeading(-33272);
						break;
					case 31923 :
						spawnDat.setX(173202);
						spawnDat.setY(-87004);
						spawnDat.setZ(-7200);
						spawnDat.setHeading(-16248);
						break;
					case 31924 :
						spawnDat.setX(175606);
						spawnDat.setY(-82853);
						spawnDat.setZ(-7200);
						spawnDat.setHeading(-16248);
						break;
				}
				_managers.add(spawnDat);
				SpawnParser.getInstance().addNewSpawn(spawnDat);
				spawnDat.doSpawn();
				spawnDat.startRespawn();
				if (Config.DEBUG)
				{
					info("Spawned " + spawnDat.getTemplate().getName(null));
				}
			}
			catch (final Exception e)
			{
				warn("Error while spawning managers: " + e.getMessage(), e);
			}
		}
	}

	protected void initFixedInfo()
	{
		_startHallSpawns.put(31921, _startHallSpawn[0]);
		_startHallSpawns.put(31922, _startHallSpawn[1]);
		_startHallSpawns.put(31923, _startHallSpawn[2]);
		_startHallSpawns.put(31924, _startHallSpawn[3]);

		_hallInUse.put(31921, false);
		_hallInUse.put(31922, false);
		_hallInUse.put(31923, false);
		_hallInUse.put(31924, false);

		_hallGateKeepers.put(31925, 25150012);
		_hallGateKeepers.put(31926, 25150013);
		_hallGateKeepers.put(31927, 25150014);
		_hallGateKeepers.put(31928, 25150015);
		_hallGateKeepers.put(31929, 25150016);
		_hallGateKeepers.put(31930, 25150002);
		_hallGateKeepers.put(31931, 25150003);
		_hallGateKeepers.put(31932, 25150004);
		_hallGateKeepers.put(31933, 25150005);
		_hallGateKeepers.put(31934, 25150006);
		_hallGateKeepers.put(31935, 25150032);
		_hallGateKeepers.put(31936, 25150033);
		_hallGateKeepers.put(31937, 25150034);
		_hallGateKeepers.put(31938, 25150035);
		_hallGateKeepers.put(31939, 25150036);
		_hallGateKeepers.put(31940, 25150022);
		_hallGateKeepers.put(31941, 25150023);
		_hallGateKeepers.put(31942, 25150024);
		_hallGateKeepers.put(31943, 25150025);
		_hallGateKeepers.put(31944, 25150026);

		_keyBoxNpc.put(18120, 31455);
		_keyBoxNpc.put(18121, 31455);
		_keyBoxNpc.put(18122, 31455);
		_keyBoxNpc.put(18123, 31455);
		_keyBoxNpc.put(18124, 31456);
		_keyBoxNpc.put(18125, 31456);
		_keyBoxNpc.put(18126, 31456);
		_keyBoxNpc.put(18127, 31456);
		_keyBoxNpc.put(18128, 31457);
		_keyBoxNpc.put(18129, 31457);
		_keyBoxNpc.put(18130, 31457);
		_keyBoxNpc.put(18131, 31457);
		_keyBoxNpc.put(18149, 31458);
		_keyBoxNpc.put(18150, 31459);
		_keyBoxNpc.put(18151, 31459);
		_keyBoxNpc.put(18152, 31459);
		_keyBoxNpc.put(18153, 31459);
		_keyBoxNpc.put(18154, 31460);
		_keyBoxNpc.put(18155, 31460);
		_keyBoxNpc.put(18156, 31460);
		_keyBoxNpc.put(18157, 31460);
		_keyBoxNpc.put(18158, 31461);
		_keyBoxNpc.put(18159, 31461);
		_keyBoxNpc.put(18160, 31461);
		_keyBoxNpc.put(18161, 31461);
		_keyBoxNpc.put(18162, 31462);
		_keyBoxNpc.put(18163, 31462);
		_keyBoxNpc.put(18164, 31462);
		_keyBoxNpc.put(18165, 31462);
		_keyBoxNpc.put(18183, 31463);
		_keyBoxNpc.put(18184, 31464);
		_keyBoxNpc.put(18212, 31465);
		_keyBoxNpc.put(18213, 31465);
		_keyBoxNpc.put(18214, 31465);
		_keyBoxNpc.put(18215, 31465);
		_keyBoxNpc.put(18216, 31466);
		_keyBoxNpc.put(18217, 31466);
		_keyBoxNpc.put(18218, 31466);
		_keyBoxNpc.put(18219, 31466);

		_victim.put(18150, 18158);
		_victim.put(18151, 18159);
		_victim.put(18152, 18160);
		_victim.put(18153, 18161);
		_victim.put(18154, 18162);
		_victim.put(18155, 18163);
		_victim.put(18156, 18164);
		_victim.put(18157, 18165);
	}

	private void loadMysteriousBox()
	{
		_mysteriousBoxSpawns.clear();

		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, key_npc_id FROM four_sepulchers_spawnlist Where spawntype = ? ORDER BY id");
			statement.setInt(1, 0);
			final var rset = statement.executeQuery();

			Spawner spawnDat;
			NpcTemplate template1;

			while (rset.next())
			{
				template1 = NpcsParser.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					spawnDat = new Spawner(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setX(rset.getInt("locx"));
					spawnDat.setY(rset.getInt("locy"));
					spawnDat.setZ(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnParser.getInstance().addNewSpawn(spawnDat);
					final int keyNpcId = rset.getInt("key_npc_id");
					_mysteriousBoxSpawns.put(keyNpcId, spawnDat);
				}
				else
				{
					warn("Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}

			rset.close();
			statement.close();
			if (Config.DEBUG)
			{
				info("Loaded " + _mysteriousBoxSpawns.size() + " Mysterious-Box spawns.");
			}
		}
		catch (final Exception e)
		{
			warn("Spawn could not be initialized: " + e.getMessage(), e);
		}
	}

	private void initKeyBoxSpawns()
	{
		Spawner spawnDat;
		NpcTemplate template;
		for (final Entry<Integer, Integer> keyNpc : _keyBoxNpc.entrySet())
		{
			try
			{
				template = NpcsParser.getInstance().getTemplate(keyNpc.getValue());
				if (template != null)
				{
					spawnDat = new Spawner(template);
					spawnDat.setAmount(1);
					spawnDat.setX(0);
					spawnDat.setY(0);
					spawnDat.setZ(0);
					spawnDat.setHeading(0);
					spawnDat.setRespawnDelay(3600);
					SpawnParser.getInstance().addNewSpawn(spawnDat);
					_keyBoxSpawns.put(keyNpc.getKey(), spawnDat);
				}
				else
				{
					warn("Data missing in NPC table for ID: " + keyNpc.getValue() + ".");
				}
			}
			catch (final Exception e)
			{
				warn("Spawn could not be initialized: " + e.getMessage(), e);
			}
		}
	}

	private void loadPhysicalMonsters()
	{
		_physicalMonsters.clear();

		int loaded = 0;
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement1 = con.prepareStatement("SELECT Distinct key_npc_id FROM four_sepulchers_spawnlist Where spawntype = ? ORDER BY key_npc_id");
			statement1.setInt(1, 1);
			final var rset1 = statement1.executeQuery();

			final var statement2 = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, key_npc_id FROM four_sepulchers_spawnlist Where key_npc_id = ? and spawntype = ? ORDER BY id");
			while (rset1.next())
			{
				final int keyNpcId = rset1.getInt("key_npc_id");

				statement2.setInt(1, keyNpcId);
				statement2.setInt(2, 1);
				final var rset2 = statement2.executeQuery();
				statement2.clearParameters();

				Spawner spawnDat;
				NpcTemplate template1;

				_physicalSpawns = new ArrayList<>();

				while (rset2.next())
				{
					template1 = NpcsParser.getInstance().getTemplate(rset2.getInt("npc_templateid"));
					if (template1 != null)
					{
						spawnDat = new Spawner(template1);
						spawnDat.setAmount(rset2.getInt("count"));
						spawnDat.setX(rset2.getInt("locx"));
						spawnDat.setY(rset2.getInt("locy"));
						spawnDat.setZ(rset2.getInt("locz"));
						spawnDat.setHeading(rset2.getInt("heading"));
						spawnDat.setRespawnDelay(rset2.getInt("respawn_delay"));
						SpawnParser.getInstance().addNewSpawn(spawnDat);
						_physicalSpawns.add(spawnDat);
						loaded++;
					}
					else
					{
						warn("Data missing in NPC table for ID: " + rset2.getInt("npc_templateid") + ".");
					}
				}

				rset2.close();
				_physicalMonsters.put(keyNpcId, _physicalSpawns);
			}

			rset1.close();
			statement1.close();
			statement2.close();
			if (Config.DEBUG)
			{
				info("Loaded " + loaded + " Physical type monsters spawns.");
			}
		}
		catch (final Exception e)
		{
			warn("Spawn could not be initialized: " + e.getMessage(), e);
		}
	}

	private void loadMagicalMonsters()
	{
		
		_magicalMonsters.clear();

		int loaded = 0;
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement1 = con.prepareStatement("SELECT Distinct key_npc_id FROM four_sepulchers_spawnlist Where spawntype = ? ORDER BY key_npc_id");
			statement1.setInt(1, 2);
			final var rset1 = statement1.executeQuery();

			final var statement2 = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, key_npc_id FROM four_sepulchers_spawnlist WHERE key_npc_id = ? AND spawntype = ? ORDER BY id");
			while (rset1.next())
			{
				final int keyNpcId = rset1.getInt("key_npc_id");

				statement2.setInt(1, keyNpcId);
				statement2.setInt(2, 2);
				final var rset2 = statement2.executeQuery();
				statement2.clearParameters();

				Spawner spawnDat;
				NpcTemplate template1;

				_magicalSpawns = new ArrayList<>();

				while (rset2.next())
				{
					template1 = NpcsParser.getInstance().getTemplate(rset2.getInt("npc_templateid"));
					if (template1 != null)
					{
						spawnDat = new Spawner(template1);
						spawnDat.setAmount(rset2.getInt("count"));
						spawnDat.setX(rset2.getInt("locx"));
						spawnDat.setY(rset2.getInt("locy"));
						spawnDat.setZ(rset2.getInt("locz"));
						spawnDat.setHeading(rset2.getInt("heading"));
						spawnDat.setRespawnDelay(rset2.getInt("respawn_delay"));
						SpawnParser.getInstance().addNewSpawn(spawnDat);
						_magicalSpawns.add(spawnDat);
						loaded++;
					}
					else
					{
						warn("Data missing in NPC table for ID: " + rset2.getInt("npc_templateid") + ".");
					}
				}

				rset2.close();
				_magicalMonsters.put(keyNpcId, _magicalSpawns);
			}

			rset1.close();
			statement1.close();
			statement2.close();
			if (Config.DEBUG)
			{
				info("Loaded " + loaded + " Magical type monsters spawns.");
			}
		}
		catch (final Exception e)
		{
			warn("Spawn could not be initialized: " + e.getMessage(), e);
		}
	}

	private void loadDukeMonsters()
	{
		_dukeFinalMobs.clear();
		_archonSpawned.clear();

		int loaded = 0;
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement1 = con.prepareStatement("SELECT Distinct key_npc_id FROM four_sepulchers_spawnlist Where spawntype = ? ORDER BY key_npc_id");
			statement1.setInt(1, 5);
			final var rset1 = statement1.executeQuery();

			final var statement2 = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, key_npc_id FROM four_sepulchers_spawnlist WHERE key_npc_id = ? AND spawntype = ? ORDER BY id");
			while (rset1.next())
			{
				final int keyNpcId = rset1.getInt("key_npc_id");

				statement2.setInt(1, keyNpcId);
				statement2.setInt(2, 5);
				final var rset2 = statement2.executeQuery();
				statement2.clearParameters();

				Spawner spawnDat;
				NpcTemplate template1;

				_dukeFinalSpawns = new ArrayList<>();

				while (rset2.next())
				{
					template1 = NpcsParser.getInstance().getTemplate(rset2.getInt("npc_templateid"));
					if (template1 != null)
					{
						spawnDat = new Spawner(template1);
						spawnDat.setAmount(rset2.getInt("count"));
						spawnDat.setX(rset2.getInt("locx"));
						spawnDat.setY(rset2.getInt("locy"));
						spawnDat.setZ(rset2.getInt("locz"));
						spawnDat.setHeading(rset2.getInt("heading"));
						spawnDat.setRespawnDelay(rset2.getInt("respawn_delay"));
						SpawnParser.getInstance().addNewSpawn(spawnDat);
						_dukeFinalSpawns.add(spawnDat);
						loaded++;
					}
					else
					{
						warn("Data missing in NPC table for ID: " + rset2.getInt("npc_templateid") + ".");
					}
				}
				rset2.close();
				_dukeFinalMobs.put(keyNpcId, _dukeFinalSpawns);
				_archonSpawned.put(keyNpcId, false);
			}
			rset1.close();
			statement1.close();
			statement2.close();
			if (Config.DEBUG)
			{
				info("Loaded " + loaded + " Church of duke monsters spawns.");
			}
		}
		catch (final Exception e)
		{
			warn("Spawn could not be initialized: " + e.getMessage(), e);
		}
	}

	private void loadEmperorsGraveMonsters()
	{
		
		_emperorsGraveNpcs.clear();

		int loaded = 0;
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement1 = con.prepareStatement("SELECT Distinct key_npc_id FROM four_sepulchers_spawnlist Where spawntype = ? ORDER BY key_npc_id");
			statement1.setInt(1, 6);
			final var rset1 = statement1.executeQuery();

			final var statement2 = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, key_npc_id FROM four_sepulchers_spawnlist WHERE key_npc_id = ? and spawntype = ? ORDER BY id");
			while (rset1.next())
			{
				final int keyNpcId = rset1.getInt("key_npc_id");

				statement2.setInt(1, keyNpcId);
				statement2.setInt(2, 6);
				final var rset2 = statement2.executeQuery();
				statement2.clearParameters();

				Spawner spawnDat;
				NpcTemplate template1;

				_emperorsGraveSpawns = new ArrayList<>();

				while (rset2.next())
				{
					template1 = NpcsParser.getInstance().getTemplate(rset2.getInt("npc_templateid"));
					if (template1 != null)
					{
						spawnDat = new Spawner(template1);
						spawnDat.setAmount(rset2.getInt("count"));
						spawnDat.setX(rset2.getInt("locx"));
						spawnDat.setY(rset2.getInt("locy"));
						spawnDat.setZ(rset2.getInt("locz"));
						spawnDat.setHeading(rset2.getInt("heading"));
						spawnDat.setRespawnDelay(rset2.getInt("respawn_delay"));
						SpawnParser.getInstance().addNewSpawn(spawnDat);
						_emperorsGraveSpawns.add(spawnDat);
						loaded++;
					}
					else
					{
						warn("Data missing in NPC table for ID: " + rset2.getInt("npc_templateid") + ".");
					}
				}

				rset2.close();
				_emperorsGraveNpcs.put(keyNpcId, _emperorsGraveSpawns);
			}

			rset1.close();
			statement1.close();
			statement2.close();
			if (Config.DEBUG)
			{
				info("Loaded " + loaded + " Emperor's grave NPC spawns.");
			}
		}
		catch (final Exception e)
		{
			warn("Spawn could not be initialized: " + e.getMessage(), e);
		}
	}

	protected void initLocationShadowSpawns()
	{
		final int locNo = Rnd.get(4);
		final int[] gateKeeper =
		{
		        31929, 31934, 31939, 31944
		};

		Spawner spawnDat;
		NpcTemplate template;

		_shadowSpawns.clear();

		for (var i = 0; i <= 3; i++)
		{
			template = NpcsParser.getInstance().getTemplate(_shadowSpawnLoc[locNo][i][0]);
			if (template != null)
			{
				try
				{
					spawnDat = new Spawner(template);
					spawnDat.setAmount(1);
					spawnDat.setX(_shadowSpawnLoc[locNo][i][1]);
					spawnDat.setY(_shadowSpawnLoc[locNo][i][2]);
					spawnDat.setZ(_shadowSpawnLoc[locNo][i][3]);
					spawnDat.setHeading(_shadowSpawnLoc[locNo][i][4]);
					SpawnParser.getInstance().addNewSpawn(spawnDat);
					final int keyNpcId = gateKeeper[i];
					_shadowSpawns.put(keyNpcId, spawnDat);
				}
				catch (final Exception e)
				{
					warn("Error on InitLocationShadowSpawns", e);
				}
			}
			else
			{
				warn("Data missing in NPC table for ID: " + _shadowSpawnLoc[locNo][i][0] + ".");
			}
		}
	}

	protected void initExecutionerSpawns()
	{
		Spawner spawnDat;
		NpcTemplate template;

		for (final var keyNpcId : _victim.keySet())
		{
			try
			{
				template = NpcsParser.getInstance().getTemplate(_victim.get(keyNpcId));
				if (template != null)
				{
					spawnDat = new Spawner(template);
					spawnDat.setAmount(1);
					spawnDat.setX(0);
					spawnDat.setY(0);
					spawnDat.setZ(0);
					spawnDat.setHeading(0);
					spawnDat.setRespawnDelay(3600);
					SpawnParser.getInstance().addNewSpawn(spawnDat);
					_executionerSpawns.put(keyNpcId, spawnDat);
				}
				else
				{
					warn("Data missing in NPC table for ID: " + _victim.get(keyNpcId) + ".");
				}
			}
			catch (final Exception e)
			{
				warn("Spawn could not be initialized: " + e.getMessage(), e);
			}
		}
	}

	public ScheduledFuture<?> getChangeAttackTimeTask()
	{
		return _changeAttackTimeTask;
	}

	public void setChangeAttackTimeTask(ScheduledFuture<?> task)
	{
		_changeAttackTimeTask = task;
	}

	public ScheduledFuture<?> getChangeCoolDownTimeTask()
	{
		return _changeCoolDownTimeTask;
	}

	public void setChangeCoolDownTimeTask(ScheduledFuture<?> task)
	{
		_changeCoolDownTimeTask = task;
	}

	public ScheduledFuture<?> getChangeEntryTimeTask()
	{
		return _changeEntryTimeTask;
	}

	public void setChangeEntryTimeTask(ScheduledFuture<?> task)
	{
		_changeEntryTimeTask = task;
	}

	public ScheduledFuture<?> getChangeWarmUpTimeTask()
	{
		return _changeWarmUpTimeTask;
	}

	public void setChangeWarmUpTimeTask(ScheduledFuture<?> task)
	{
		_changeWarmUpTimeTask = task;
	}

	public long getAttackTimeEnd()
	{
		return _attackTimeEnd;
	}

	public void setAttackTimeEnd(long attackTimeEnd)
	{
		_attackTimeEnd = attackTimeEnd;
	}

	public byte getCycleMin()
	{
		return _newCycleMin;
	}

	public long getEntrytTimeEnd()
	{
		return _entryTimeEnd;
	}

	public void setEntryTimeEnd(long entryTimeEnd)
	{
		_entryTimeEnd = entryTimeEnd;
	}

	public long getWarmUpTimeEnd()
	{
		return _warmUpTimeEnd;
	}

	public void setWarmUpTimeEnd(long warmUpTimeEnd)
	{
		_warmUpTimeEnd = warmUpTimeEnd;
	}

	public boolean isAttackTime()
	{
		return _inAttackTime;
	}

	public void setIsAttackTime(boolean attackTime)
	{
		_inAttackTime = attackTime;
	}

	public boolean isCoolDownTime()
	{
		return _inCoolDownTime;
	}

	public void setIsCoolDownTime(boolean isCoolDownTime)
	{
		_inCoolDownTime = isCoolDownTime;
	}

	public boolean isEntryTime()
	{
		return _inEntryTime;
	}

	public void setIsEntryTime(boolean entryTime)
	{
		_inEntryTime = entryTime;
	}

	public boolean isFirstTimeRun()
	{
		return _firstTimeRun;
	}

	public void setIsFirstTimeRun(boolean isFirstTimeRun)
	{
		_firstTimeRun = isFirstTimeRun;
	}

	public boolean isWarmUpTime()
	{
		return _inWarmUpTime;
	}

	public void setIsWarmUpTime(boolean isWarmUpTime)
	{
		_inWarmUpTime = isWarmUpTime;
	}

	public synchronized void tryEntry(Npc npc, Player player)
	{
		final var hostQuest = QuestManager.getInstance().getQuest(QUEST_ID);
		if (hostQuest == null)
		{
			warn("Couldn't find quest: " + QUEST_ID);
			return;
		}
		final var npcId = npc.getId();
		switch (npcId)
		{
			case 31921 :
			case 31922 :
			case 31923 :
			case 31924 :
				break;
			default :
				if (!player.isGM())
				{
					Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " tried to enter four sepulchers with invalid npc id.");
				}
				return;
		}

		if (_hallInUse.get(npcId).booleanValue())
		{
			showHtmlFile(player, npcId + "-FULL.htm", npc, null);
			return;
		}

		if (Config.FS_PARTY_MEMBER_COUNT > 1)
		{
			if (!player.isInParty() || (player.getParty().getMemberCount() < Config.FS_PARTY_MEMBER_COUNT))
			{
				showHtmlFile(player, npcId + "-SP.htm", npc, null);
				return;
			}

			if (!player.getParty().isLeader(player))
			{
				showHtmlFile(player, npcId + "-NL.htm", npc, null);
				return;
			}

			for (final var mem : player.getParty().getMembers())
			{
				final var qs = mem.getQuestState(hostQuest.getName());
				if ((qs == null) || (!qs.isStarted() && !qs.isCompleted()))
				{
					showHtmlFile(player, npcId + "-NS.htm", npc, mem);
					return;
				}
				if (mem.getInventory().getItemByItemId(ENTRANCE_PASS) == null)
				{
					showHtmlFile(player, npcId + "-SE.htm", npc, mem);
					return;
				}

				if (player.getWeightPenalty() >= 3)
				{
					mem.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
					return;
				}
			}
		}
		else if ((Config.FS_PARTY_MEMBER_COUNT <= 1) && player.isInParty())
		{
			if (!player.getParty().isLeader(player))
			{
				showHtmlFile(player, npcId + "-NL.htm", npc, null);
				return;
			}
			for (final var mem : player.getParty().getMembers())
			{
				final var qs = mem.getQuestState(hostQuest.getName());
				if ((qs == null) || (!qs.isStarted() && !qs.isCompleted()))
				{
					showHtmlFile(player, npcId + "-NS.htm", npc, mem);
					return;
				}
				if (mem.getInventory().getItemByItemId(ENTRANCE_PASS) == null)
				{
					showHtmlFile(player, npcId + "-SE.htm", npc, mem);
					return;
				}

				if (player.getWeightPenalty() >= 3)
				{
					mem.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
					return;
				}
			}
		}
		else
		{
			final var qs = player.getQuestState(hostQuest.getName());
			if ((qs == null) || (!qs.isStarted() && !qs.isCompleted()))
			{
				showHtmlFile(player, npcId + "-NS.htm", npc, player);
				return;
			}
			if (player.getInventory().getItemByItemId(ENTRANCE_PASS) == null)
			{
				showHtmlFile(player, npcId + "-SE.htm", npc, player);
				return;
			}

			if (player.getWeightPenalty() >= 3)
			{
				player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return;
			}
		}

		if (!isEntryTime())
		{
			showHtmlFile(player, npcId + "-NE.htm", npc, null);
			return;
		}

		showHtmlFile(player, npcId + "-OK.htm", npc, null);

		entry(npcId, player);
	}

	private void entry(int npcId, Player player)
	{
		final var Location = _startHallSpawns.get(npcId);
		int driftx;
		int drifty;

		if (Config.FS_PARTY_MEMBER_COUNT > 1)
		{
			final List<Player> members = new LinkedList<>();
			for (final var mem : player.getParty().getMembers())
			{
				if (!mem.isDead() && Util.checkIfInRange(700, player, mem, true))
				{
					members.add(mem);
				}
			}

			final var zone = EpicBossManager.getInstance().getZone(Location[0], Location[1], Location[2]);
			for (final var mem : members)
			{
				if (zone != null)
				{
					zone.allowPlayerEntry(mem, 30);
				}
				driftx = Rnd.get(-80, 80);
				drifty = Rnd.get(-80, 80);
				mem.teleToLocation(Location[0] + driftx, Location[1] + drifty, Location[2], true, mem.getReflection());
				mem.destroyItemByItemId("Quest", ENTRANCE_PASS, 1, mem, true);
				if (mem.getInventory().getItemByItemId(ANTIQUE_BROOCH) == null)
				{
					mem.addItem("Quest", USED_PASS, 1, mem, true);
				}

				final ItemInstance hallsKey = mem.getInventory().getItemByItemId(CHAPEL_KEY);
				if (hallsKey != null)
				{
					mem.destroyItemByItemId("Quest", CHAPEL_KEY, hallsKey.getCount(), mem, true);
				}
			}
			_challengers.remove(npcId);
			_challengers.put(npcId, player);

			_hallInUse.remove(npcId);
			_hallInUse.put(npcId, true);
		}
		if ((Config.FS_PARTY_MEMBER_COUNT <= 1) && player.isInParty())
		{
			final List<Player> members = new LinkedList<>();
			for (final var mem : player.getParty().getMembers())
			{
				if (!mem.isDead() && Util.checkIfInRange(700, player, mem, true))
				{
					members.add(mem);
				}
			}

			final var zone = EpicBossManager.getInstance().getZone(Location[0], Location[1], Location[2]);
			for (final var mem : members)
			{
				if (zone != null)
				{
					zone.allowPlayerEntry(mem, 30);
				}
				driftx = Rnd.get(-80, 80);
				drifty = Rnd.get(-80, 80);
				mem.teleToLocation(Location[0] + driftx, Location[1] + drifty, Location[2], true, mem.getReflection());
				mem.destroyItemByItemId("Quest", ENTRANCE_PASS, 1, mem, true);
				if (mem.getInventory().getItemByItemId(ANTIQUE_BROOCH) == null)
				{
					mem.addItem("Quest", USED_PASS, 1, mem, true);
				}

				final var hallsKey = mem.getInventory().getItemByItemId(CHAPEL_KEY);
				if (hallsKey != null)
				{
					mem.destroyItemByItemId("Quest", CHAPEL_KEY, hallsKey.getCount(), mem, true);
				}
			}

			_challengers.remove(npcId);
			_challengers.put(npcId, player);

			_hallInUse.remove(npcId);
			_hallInUse.put(npcId, true);
		}
		else
		{
			final var zone = EpicBossManager.getInstance().getZone(Location[0], Location[1], Location[2]);
			if (zone != null)
			{
				zone.allowPlayerEntry(player, 30);
			}
			driftx = Rnd.get(-80, 80);
			drifty = Rnd.get(-80, 80);
			player.teleToLocation(Location[0] + driftx, Location[1] + drifty, Location[2], true, player.getReflection());
			player.destroyItemByItemId("Quest", ENTRANCE_PASS, 1, player, true);
			if (player.getInventory().getItemByItemId(ANTIQUE_BROOCH) == null)
			{
				player.addItem("Quest", USED_PASS, 1, player, true);
			}

			final var hallsKey = player.getInventory().getItemByItemId(CHAPEL_KEY);
			if (hallsKey != null)
			{
				player.destroyItemByItemId("Quest", CHAPEL_KEY, hallsKey.getCount(), player, true);
			}

			_challengers.remove(npcId);
			_challengers.put(npcId, player);

			_hallInUse.remove(npcId);
			_hallInUse.put(npcId, true);
		}
	}

	public void spawnMysteriousBox(int npcId)
	{
		if (!isAttackTime())
		{
			return;
		}

		final var spawnDat = _mysteriousBoxSpawns.get(npcId);
		if (spawnDat != null)
		{
			_allMobs.add(spawnDat.doSpawn());
			spawnDat.stopRespawn();
		}
	}

	public void spawnMonster(int npcId)
	{
		if (!isAttackTime())
		{
			return;
		}

		List<Spawner> monsterList;
		final List<SepulcherMonsterInstance> mobs = new CopyOnWriteArrayList<>();
		Spawner keyBoxMobSpawn;

		if (Rnd.get(2) == 0)
		{
			monsterList = _physicalMonsters.get(npcId);
		}
		else
		{
			monsterList = _magicalMonsters.get(npcId);
		}

		if (monsterList != null)
		{
			boolean spawnKeyBoxMob = false;
			boolean spawnedKeyBoxMob = false;

			for (final var spawnDat : monsterList)
			{
				if (spawnedKeyBoxMob)
				{
					spawnKeyBoxMob = false;
				}
				else
				{
					switch (npcId)
					{
						case 31469 :
						case 31474 :
						case 31479 :
						case 31484 :
							if (Rnd.get(48) == 0)
							{
								spawnKeyBoxMob = true;
							}
							break;
						default :
							spawnKeyBoxMob = false;
					}
				}

				SepulcherMonsterInstance mob = null;

				if (spawnKeyBoxMob)
				{
					try
					{
						final var template = NpcsParser.getInstance().getTemplate(18149);
						if (template != null)
						{
							keyBoxMobSpawn = new Spawner(template);
							keyBoxMobSpawn.setAmount(1);
							keyBoxMobSpawn.setX(spawnDat.getX());
							keyBoxMobSpawn.setY(spawnDat.getY());
							keyBoxMobSpawn.setZ(spawnDat.getZ());
							keyBoxMobSpawn.setHeading(spawnDat.getHeading());
							keyBoxMobSpawn.setRespawnDelay(3600);
							SpawnParser.getInstance().addNewSpawn(keyBoxMobSpawn);
							mob = (SepulcherMonsterInstance) keyBoxMobSpawn.doSpawn();
							keyBoxMobSpawn.stopRespawn();
						}
						else
						{
							warn("Data missing in NPC table for ID: 18149");
						}
					}
					catch (final Exception e)
					{
						warn("Spawn could not be initialized: " + e.getMessage(), e);
					}

					spawnedKeyBoxMob = true;
				}
				else
				{
					mob = (SepulcherMonsterInstance) spawnDat.doSpawn();
					spawnDat.stopRespawn();
				}

				if (mob != null)
				{
					mob.mysteriousBoxId = npcId;
					switch (npcId)
					{
						case 31469 :
						case 31474 :
						case 31479 :
						case 31484 :
						case 31472 :
						case 31477 :
						case 31482 :
						case 31487 :
							mobs.add(mob);
					}
					_allMobs.add(mob);
				}
			}

			switch (npcId)
			{
				case 31469 :
				case 31474 :
				case 31479 :
				case 31484 :
					_viscountMobs.put(npcId, mobs);
					break;
				
				case 31472 :
				case 31477 :
				case 31482 :
				case 31487 :
					_dukeMobs.put(npcId, mobs);
					break;
			}
		}
	}

	public synchronized boolean isViscountMobsAnnihilated(int npcId)
	{
		final List<SepulcherMonsterInstance> mobs = _viscountMobs.get(npcId);

		if (mobs == null)
		{
			return true;
		}

		for (final var mob : mobs)
		{
			if (!mob.isDead())
			{
				return false;
			}
		}

		return true;
	}

	public synchronized boolean isDukeMobsAnnihilated(int npcId)
	{
		final List<SepulcherMonsterInstance> mobs = _dukeMobs.get(npcId);

		if (mobs == null)
		{
			return true;
		}

		for (final var mob : mobs)
		{
			if (!mob.isDead())
			{
				return false;
			}
		}

		return true;
	}

	public void spawnKeyBox(Npc activeChar)
	{
		if (!isAttackTime())
		{
			return;
		}

		final var spawnDat = _keyBoxSpawns.get(activeChar.getId());
		if (spawnDat != null)
		{
			spawnDat.setAmount(1);
			spawnDat.setX(activeChar.getX());
			spawnDat.setY(activeChar.getY());
			spawnDat.setZ(activeChar.getZ());
			spawnDat.setHeading(activeChar.getHeading());
			spawnDat.setRespawnDelay(3600);
			_allMobs.add(spawnDat.doSpawn());
			spawnDat.stopRespawn();
		}
	}

	public void spawnExecutionerOfHalisha(Npc activeChar)
	{
		if (!isAttackTime())
		{
			return;
		}

		final var spawnDat = _executionerSpawns.get(activeChar.getId());
		if (spawnDat != null)
		{
			spawnDat.setAmount(1);
			spawnDat.setX(activeChar.getX());
			spawnDat.setY(activeChar.getY());
			spawnDat.setZ(activeChar.getZ());
			spawnDat.setHeading(activeChar.getHeading());
			spawnDat.setRespawnDelay(3600);
			_allMobs.add(spawnDat.doSpawn());
			spawnDat.stopRespawn();
		}
	}

	public void spawnArchonOfHalisha(int npcId)
	{
		if (!isAttackTime())
		{
			return;
		}

		if (_archonSpawned.get(npcId))
		{
			return;
		}

		final List<Spawner> monsterList = _dukeFinalMobs.get(npcId);

		if (monsterList != null)
		{
			for (final var spawnDat : monsterList)
			{
				final var mob = (SepulcherMonsterInstance) spawnDat.doSpawn();
				spawnDat.stopRespawn();

				if (mob != null)
				{
					mob.mysteriousBoxId = npcId;
					_allMobs.add(mob);
				}
			}
			_archonSpawned.put(npcId, true);
		}
	}

	public void spawnEmperorsGraveNpc(int npcId)
	{
		if (!isAttackTime())
		{
			return;
		}

		final List<Spawner> monsterList = _emperorsGraveNpcs.get(npcId);

		if (monsterList != null)
		{
			for (final var spawnDat : monsterList)
			{
				_allMobs.add(spawnDat.doSpawn());
				spawnDat.stopRespawn();
			}
		}
	}

	public void locationShadowSpawns()
	{
		final int locNo = Rnd.get(4);
		final int[] gateKeeper =
		{
		        31929, 31934, 31939, 31944
		};

		Spawner spawnDat;

		for (int i = 0; i <= 3; i++)
		{
			final var keyNpcId = gateKeeper[i];
			spawnDat = _shadowSpawns.get(keyNpcId);
			spawnDat.setX(_shadowSpawnLoc[locNo][i][1]);
			spawnDat.setY(_shadowSpawnLoc[locNo][i][2]);
			spawnDat.setZ(_shadowSpawnLoc[locNo][i][3]);
			spawnDat.setHeading(_shadowSpawnLoc[locNo][i][4]);
			_shadowSpawns.put(keyNpcId, spawnDat);
		}
	}

	public void spawnShadow(int npcId)
	{
		if (!isAttackTime())
		{
			return;
		}

		final var spawnDat = _shadowSpawns.get(npcId);
		if (spawnDat != null)
		{
			final var mob = (SepulcherMonsterInstance) spawnDat.doSpawn();
			spawnDat.stopRespawn();

			if (mob != null)
			{
				mob.mysteriousBoxId = npcId;
				_allMobs.add(mob);
			}
		}
	}

	public void deleteAllMobs()
	{
		for (final var mob : _allMobs)
		{
			if (mob == null)
			{
				continue;
			}

			try
			{
				if (mob.getSpawn() != null)
				{
					mob.getSpawn().stopRespawn();
				}
				mob.deleteMe();
			}
			catch (final Exception e)
			{
				warn("Failed deleting mob.", e);
			}
		}
		_allMobs.clear();
	}

	protected void closeAllDoors()
	{
		for (final var doorId : _hallGateKeepers.values())
		{
			try
			{
				final var door = DoorParser.getInstance().getDoor(doorId);
				if (door != null)
				{
					door.closeMe();
				}
				else
				{
					warn("Attempted to close undefined door. doorId: " + doorId);
				}
			}
			catch (final Exception e)
			{
				warn("Failed closing door", e);
			}
		}
	}

	protected byte minuteSelect(byte min)
	{
		if (((double) min % 5) != 0)
		{
			switch (min)
			{
				case 6 :
				case 7 :
					min = 5;
					break;
				case 8 :
				case 9 :
				case 11 :
				case 12 :
					min = 10;
					break;
				case 13 :
				case 14 :
				case 16 :
				case 17 :
					min = 15;
					break;
				case 18 :
				case 19 :
				case 21 :
				case 22 :
					min = 20;
					break;
				case 23 :
				case 24 :
				case 26 :
				case 27 :
					min = 25;
					break;
				case 28 :
				case 29 :
				case 31 :
				case 32 :
					min = 30;
					break;
				case 33 :
				case 34 :
				case 36 :
				case 37 :
					min = 35;
					break;
				case 38 :
				case 39 :
				case 41 :
				case 42 :
					min = 40;
					break;
				case 43 :
				case 44 :
				case 46 :
				case 47 :
					min = 45;
					break;
				case 48 :
				case 49 :
				case 51 :
				case 52 :
					min = 50;
					break;
				case 53 :
				case 54 :
				case 56 :
				case 57 :
					min = 55;
					break;
			}
		}
		return min;
	}

	public void managerSay(byte min)
	{
		if (_inAttackTime)
		{
			if (min < 5)
			{
				return;
			}

			min = minuteSelect(min);

			var msg = NpcStringId.MINUTES_HAVE_PASSED;
			if (min == 90)
			{
				msg = NpcStringId.GAME_OVER_THE_TELEPORT_WILL_APPEAR_MOMENTARILY;
			}

			if (_managers != null && !_managers.isEmpty())
			{
				for (final var temp : _managers)
				{
					if (temp == null)
					{
						warn("managerSay(): manager is null");
						continue;
					}
					if (!(temp.getLastSpawn() instanceof SepulcherNpcInstance))
					{
						warn("managerSay(): manager is not Sepulcher instance");
						continue;
					}
					if (!_hallInUse.get(temp.getId()).booleanValue())
					{
						continue;
					}
					
					((SepulcherNpcInstance) temp.getLastSpawn()).sayInShout(msg, min);
				}
			}
		}

		else if (_inEntryTime)
		{
			final var msg1 = NpcStringId.YOU_MAY_NOW_ENTER_THE_SEPULCHER;
			final var msg2 = NpcStringId.IF_YOU_PLACE_YOUR_HAND_ON_THE_STONE_STATUE_IN_FRONT_OF_EACH_SEPULCHER_YOU_WILL_BE_ABLE_TO_ENTER;
			if (_managers != null && !_managers.isEmpty())
			{
				for (final var temp : _managers)
				{
					if (temp == null)
					{
						warn("Something goes wrong in managerSay()...");
						continue;
					}
					if (!(temp.getLastSpawn() instanceof SepulcherNpcInstance))
					{
						warn("Something goes wrong in managerSay()...");
						continue;
					}
					((SepulcherNpcInstance) temp.getLastSpawn()).sayInShout(msg1, 0);
					((SepulcherNpcInstance) temp.getLastSpawn()).sayInShout(msg2, 0);
				}
			}
		}
	}

	public Map<Integer, Integer> getHallGateKeepers()
	{
		return _hallGateKeepers;
	}

	public void showHtmlFile(Player player, String file, Npc npc, Player member)
	{
		final var html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, player.getLang(), "data/html/SepulcherNpc/" + file);
		if (member != null)
		{
			html.replace("%member%", member.getName(null));
		}
		player.sendPacket(html);
	}

	public static final FourSepulchersManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final FourSepulchersManager _instance = new FourSepulchersManager();
	}
}