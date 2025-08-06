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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.SpawnTemplate;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.scripts.ai.AbstractNpcAI;

public class AnomicFoundry extends AbstractNpcAI
{
	private static int LABORER = 22396;
	private static int FOREMAN = 22397;
	private static int LESSER_EVIL = 22398;
	private static int GREATER_EVIL = 22399;

	protected Future<?> _runTask = null;

	protected static final Map<Integer, AnomicFoundryRoute> _anomicFoundryRoute = new ConcurrentHashMap<>();
	private final List<Npc> spawnedMobs = new ArrayList<>();

	protected class AnomicFoundryRoute
	{
		protected int _id;
		protected Npc _anomicFoundryNpc;
		protected int _currentRoute = 0;
		protected boolean _attackDirection = false;
		protected TreeMap<Integer, Location> _pathRoutes;
		protected boolean _respawn = true;

		protected AnomicFoundryRoute(int id)
		{
			_id = id;
		}
	}

	private int respawnTime = 10000;
	private final int respawnMin = 15000;
	private final int respawnMax = 100000;

	protected final int[] _spawned =
	{
	        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
	};

	private final Map<Integer, Integer> _atkIndex = new ConcurrentHashMap<>();

	public AnomicFoundry(String name, String descr)
	{
		super(name, descr);

		addAggroRangeEnterId(LABORER);
		addAttackId(LESSER_EVIL);
		addAttackId(LABORER);
		addKillId(LABORER);
		addKillId(LESSER_EVIL);
		addKillId(GREATER_EVIL);
		addSpawnId(LABORER);
		addSpawnId(LESSER_EVIL);
		addSpawnId(GREATER_EVIL);

		load();
		startQuestTimer("make_spawn_1", respawnTime, null, null);
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("make_spawn_1"))
		{
			if (HellboundManager.getInstance().getLevel() >= 10)
			{
				if (_runTask == null)
				{
					loadSpawns(1);
					loadSpawns(2);
					loadSpawns(3);
					
					ThreadPoolManager.getInstance().schedule(new Runnable()
					{
						@Override
						public void run()
						{
							loadSpawns(4);
							loadSpawns(5);
							loadSpawns(6);
						}
					}, 25000);
					
					ThreadPoolManager.getInstance().schedule(new Runnable()
					{
						@Override
						public void run()
						{
							loadSpawns(7);
							loadSpawns(8);
							loadSpawns(9);
						}
					}, 45000);
					_runTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new RunTask(), 1000, 1000);
				}
			}
			else
			{
				if (_runTask != null)
				{
					_runTask.cancel(false);
					_runTask = null;
				}

				for (final Npc mob : spawnedMobs)
				{
					if (mob != null)
					{
						mob.getSpawn().stopRespawn();
						mob.deleteMe();
					}
				}
				spawnedMobs.clear();
			}
			startQuestTimer("make_spawn_1", respawnTime, null, null);
		}
		else if (event.equalsIgnoreCase("return_laborer"))
		{
			if ((npc != null) && !npc.isDead())
			{
				((Attackable) npc).returnHome();
			}
		}
		else if (event.equalsIgnoreCase("reset_respawn_time"))
		{
			respawnTime = 60000;
		}
		return null;
	}

	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (getRandom(10000) < 2000)
		{
			requestHelp(npc, player, 500);
		}
		return super.onAggroRangeEnter(npc, player, isSummon);
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.getId() == LESSER_EVIL)
		{
			final AnomicFoundryRoute group = getGroup(npc);
			if (group != null)
			{
				group._attackDirection = true;
			}
		}
		else if (npc.getId() == LABORER)
		{
			int atkIndex = _atkIndex.containsKey(npc.getObjectId()) ? _atkIndex.get(npc.getObjectId()) : 0;
			if (atkIndex == 0)
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.ENEMY_INVASION_HURRY_UP));
				cancelQuestTimer("return_laborer", npc, null);
				startQuestTimer("return_laborer", 60000, npc, null);

				if (respawnTime > respawnMin)
				{
					respawnTime -= 5000;
				}
				else if ((respawnTime <= respawnMin) && (getQuestTimer("reset_respawn_time", null, null) == null))
				{
					startQuestTimer("reset_respawn_time", 600000, null, null);
				}
			}

			if (getRandom(10000) < 2000)
			{
				atkIndex++;
				_atkIndex.put(npc.getObjectId(), atkIndex);
				requestHelp(npc, attacker, 1000 * atkIndex);

				if (getRandom(10) < 1)
				{
					npc.getAI().setIntention(CtrlIntention.MOVING, new Location((npc.getX() + getRandom(-800, 800)), (npc.getY() + getRandom(-800, 800)), npc.getZ(), npc.getHeading()), 0);
				}
			}
		}
		return null;
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if ((npc.getId() == LESSER_EVIL) || (npc.getId() == GREATER_EVIL))
		{
			removeGroupId(npc, true);
		}
		else if (npc.getId() == LABORER)
		{
			if (getRandom(10000) < 8000)
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.PROCESS_SHOULDNT_BE_DELAYED_BECAUSE_OF_ME));
				if (respawnTime < respawnMax)
				{
					respawnTime += 10000;
				}
				else if ((respawnTime >= respawnMax) && (getQuestTimer("reset_respawn_time", null, null) == null))
				{
					startQuestTimer("reset_respawn_time", 600000, null, null);
				}
			}
			_atkIndex.remove(npc.getObjectId());
		}
		return super.onKill(npc, killer, isSummon);
	}

	@Override
	public final String onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case 22398 :
			case 22399 :
				((Attackable) npc).setOnKillDelay(0);
				break;
			case 22396 :
				if (!npc.isTeleporting())
				{
					npc.setIsNoRndWalk(true);
				}
				break;
		}
		return super.onSpawn(npc);
	}

	private static void requestHelp(Npc requester, Player agressor, int range)
	{
		for (final Spawner npcSpawn : SpawnParser.getInstance().getSpawnData())
		{
			if ((npcSpawn.getId() == FOREMAN) || (npcSpawn.getId() == LESSER_EVIL) || (npcSpawn.getId() == GREATER_EVIL))
			{
				final MonsterInstance monster = (MonsterInstance) npcSpawn.getLastSpawn();

				if ((monster != null) && !monster.isDead() && monster.isInsideRadius(requester, range, true, false) && (agressor != null) && !agressor.isDead())
				{
					monster.addDamageHate(agressor, 0, 1000);
				}
			}
		}
	}

	protected void load()
	{
		final File f = new File(Config.DATAPACK_ROOT, "data/stats/npcs/spawnZones/anomicFoundry.xml");
		if (!f.exists())
		{
			_log.error("[Anomic Foundry]: Error! anomicFoundry.xml file is missing!");
			return;
		}

		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			factory.setValidating(true);
			final Document doc = factory.newDocumentBuilder().parse(f);

			for (Node n = doc.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("route".equalsIgnoreCase(n.getNodeName()))
				{
					final int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
					final AnomicFoundryRoute group = new AnomicFoundryRoute(id);
					group._pathRoutes = new TreeMap<>();
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("pathRoute".equalsIgnoreCase(d.getNodeName()))
						{
							final int order = Integer.parseInt(d.getAttributes().getNamedItem("position").getNodeValue());
							final int x = Integer.parseInt(d.getAttributes().getNamedItem("locX").getNodeValue());
							final int y = Integer.parseInt(d.getAttributes().getNamedItem("locY").getNodeValue());
							final int z = Integer.parseInt(d.getAttributes().getNamedItem("locZ").getNodeValue());
							final Location loc = new Location(x, y, z, 0);
							group._pathRoutes.put(order, loc);
						}
					}
					_anomicFoundryRoute.put(id, group);
				}
			}
		}
		catch (final Exception e)
		{
			_log.error("[Anomic Foundry]: Error while loading anomicFoundry.xml file: " + e.getMessage(), e);
		}
	}

	protected void loadSpawns(int groupId)
	{
		if (groupId >= 10)
		{
			if (_spawned[groupId] > 0)
			{
				return;
			}
		}
		int npcId = 0;
		for (final Integer integer : _anomicFoundryRoute.keySet())
		{
			final int _groupId = integer;
			if (groupId == _groupId)
			{
				final AnomicFoundryRoute group = _anomicFoundryRoute.get(_groupId);
				final Location spawn = group._pathRoutes.firstEntry().getValue();
				if (_groupId >= 10)
				{
					if (!group._respawn)
					{
						return;
					}
				}
				if (groupId >= 19)
				{
					npcId = GREATER_EVIL;
				}
				else
				{
					npcId = LESSER_EVIL;
				}
				final SpawnTemplate tpl = new SpawnTemplate("none", 1, 0, 0);
				tpl.addSpawnRange(new Location(spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getHeading()));
				group._anomicFoundryNpc = addSpawn(npcId, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getHeading(), false, 0);
				group._anomicFoundryNpc.getSpawn().setSpawnTemplate(tpl);
				group._anomicFoundryNpc.getSpawn().setAmount(1);
				group._anomicFoundryNpc.getSpawn().startRespawn();
				if (_groupId >= 10)
				{
					group._anomicFoundryNpc.getSpawn().setRespawnDelay(0);
					group._anomicFoundryNpc.getSpawn().stopRespawn();
				}
				else
				{
					group._anomicFoundryNpc.getSpawn().setRespawnDelay(60);
				}
				group._anomicFoundryNpc.setIsRunner(true);
				group._anomicFoundryNpc.setIsTeleporting(false);
				group._anomicFoundryNpc.setRunning();
				((Attackable) group._anomicFoundryNpc).setCanReturnToSpawnPoint(false);
				spawnedMobs.add(group._anomicFoundryNpc);
				_spawned[groupId]++;
				SpawnParser.getInstance().addRandomSpawnByNpc(group._anomicFoundryNpc.getSpawn(), group._anomicFoundryNpc.getTemplate());
			}
		}
	}

	protected class RunTask implements Runnable
	{
		@Override
		public void run()
		{
			for (final int groupId : _anomicFoundryRoute.keySet())
			{
				final AnomicFoundryRoute group = _anomicFoundryRoute.get(groupId);
				if ((group._anomicFoundryNpc == null) || group._anomicFoundryNpc.isInCombat() || group._anomicFoundryNpc.isCastingNow() || group._anomicFoundryNpc.isAttackingNow() || group._anomicFoundryNpc.isDead())
				{
					continue;
				}
				
				final Location oldLoc = group._pathRoutes.get(group._currentRoute);
				if (!Util.checkIfInRange(40, oldLoc.getX(), oldLoc.getY(), oldLoc.getZ(), group._anomicFoundryNpc, false))
				{
					if (!group._anomicFoundryNpc.isRunning())
					{
						group._anomicFoundryNpc.setRunning();
					}
					group._anomicFoundryNpc.getAI().setIntention(CtrlIntention.MOVING, oldLoc, 0);
					continue;
				}
				
				group._currentRoute = getNextRoute(group, group._currentRoute, groupId);
				final Location loc = group._pathRoutes.get(group._currentRoute);
				int nextPathRoute;
				if (group._attackDirection)
				{
					nextPathRoute = getNextRoute(group, group._currentRoute - 1, groupId);
				}
				else
				{
					nextPathRoute = getNextRoute(group, group._currentRoute, groupId);
				}
				loc.setHeading(calculateHeading(loc, group._pathRoutes.get(nextPathRoute)));
				if (!group._anomicFoundryNpc.isRunning())
				{
					group._anomicFoundryNpc.setIsRunning(true);
				}
				group._anomicFoundryNpc.getAI().setIntention(CtrlIntention.MOVING, loc, 0);
			}
		}
	}

	protected int getNextRoute(AnomicFoundryRoute group, int currentRoute, int groupId)
	{
		if (group._pathRoutes.lastKey().intValue() == currentRoute)
		{
			group._currentRoute = 0;
			group._anomicFoundryNpc.doDie(group._anomicFoundryNpc);
			switch (groupId)
			{
				case 1 :
					loadSpawns(10);
					loadSpawns(28);
					break;
				case 2 :
					loadSpawns(11);
					loadSpawns(29);
					break;
				case 3 :
					loadSpawns(12);
					loadSpawns(30);
					break;
				case 4 :
					loadSpawns(13);
					loadSpawns(31);
					break;
				case 5 :
					loadSpawns(14);
					loadSpawns(32);
					break;
				case 6 :
					loadSpawns(15);
					loadSpawns(33);
					break;
				case 7 :
					loadSpawns(16);
					loadSpawns(34);
					break;
				case 8 :
					loadSpawns(17);
					loadSpawns(35);
					break;
				case 9 :
					loadSpawns(18);
					loadSpawns(36);
					break;
				case 10 :
					loadSpawns(19);
					break;
				case 11 :
					loadSpawns(20);
					break;
				case 12 :
					loadSpawns(21);
					break;
				case 13 :
					loadSpawns(22);
					break;
				case 14 :
					loadSpawns(23);
					break;
				case 15 :
					loadSpawns(24);
					break;
				case 16 :
					loadSpawns(25);
					break;
				case 17 :
					loadSpawns(26);
					break;
				case 18 :
					loadSpawns(27);
					break;
			}
			removeGroupId(group._anomicFoundryNpc, false);
			return 0;
		}
		else
		{
			return group._pathRoutes.higherKey(currentRoute);
		}
	}

	protected AnomicFoundryRoute getGroup(Npc npc)
	{
		if ((npc == null) || (npc.getId() != LESSER_EVIL))
		{
			return null;
		}

		for (final AnomicFoundryRoute group : _anomicFoundryRoute.values())
		{
			if ((npc.getId() == LESSER_EVIL) && npc.equals(group._anomicFoundryNpc))
			{
				return group;
			}
		}
		return null;
	}

	protected void removeGroupId(Npc npc, boolean onKill)
	{
		if ((npc.getId() == LESSER_EVIL) || (npc.getId() == GREATER_EVIL))
		{
			for (final AnomicFoundryRoute group : _anomicFoundryRoute.values())
			{
				if (npc.equals(group._anomicFoundryNpc))
				{
					if (group._id >= 10)
					{
						group._currentRoute = 0;
						spawnedMobs.remove(npc);
						_spawned[group._id]--;
						if (!onKill)
						{
							SpawnParser.getInstance().removeRandomSpawnByNpc(npc);
							npc.deleteMe();
							group._respawn = false;
							ThreadPoolManager.getInstance().schedule(new RespawnTask(group._id), 60000);
						}
					}
					else
					{
						group._currentRoute = 0;
					}
				}
			}
		}
		return;
	}

	protected int calculateHeading(Location fromLoc, Location toLoc)
	{
		return Util.calculateHeadingFrom(fromLoc.getX(), fromLoc.getY(), toLoc.getX(), toLoc.getY());
	}

	protected class RespawnTask implements Runnable
	{
		private final int _groupId;

		public RespawnTask(int groupId)
		{
			_groupId = groupId;
		}

		@Override
		public void run()
		{
			for (final AnomicFoundryRoute group : _anomicFoundryRoute.values())
			{
				if (group._id == _groupId)
				{
					group._respawn = true;
				}
			}

		}
	}

	public static void main(String[] args)
	{
		new AnomicFoundry(AnomicFoundry.class.getSimpleName(), "hellbound");
	}
}
