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
package l2e.scripts.instances;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.geometry.Polygon;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.SoDManager;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.MinionList;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.instance.TrapInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.SpawnTerritory;
import l2e.gameserver.network.NpcStringId;

/**
 * Rework by LordWinter 07.02.2020
 */
public class Stage1 extends AbstractReflection
{
	private class SOD1World extends ReflectionWorld
	{
		public List<Npc> npcList = new CopyOnWriteArrayList<>();
		public int deviceSpawnedMobCount = 0;
		public final Lock lock = new ReentrantLock();
		public MonsterInstance tiat;
		public long lastFactionNotifyTime = 0;
		
		public SOD1World()
		{
		}
	}
	
	protected static class SODSpawn
	{
		public boolean isZone = false;
		public boolean isNeededNextFlag = false;
		public int npcId;
		public int x = 0;
		public int y = 0;
		public int z = 0;
		public int h = 0;
		public int zone = 0;
		public int count = 0;
	}
	
	private final Map<Integer, SpawnTerritory> _spawnZoneList = new HashMap<>();
	private final Map<Integer, List<SODSpawn>> _spawnList = new HashMap<>();
	private final List<Integer> _mustKillMobsId = new ArrayList<>();
	
	private static final int[] TRAP_18771_NPCS =
	{
	        22541, 22544, 22541, 22544
	};
	
	private static final int[] TRAP_OTHER_NPCS =
	{
	        22546, 22546, 22538, 22537
	};
	
	private static final int[] SPAWN_MOB_IDS =
	{
	        22536, 22537, 22538, 22539, 22540, 22541, 22542, 22543, 22544, 22547, 22550, 22551, 22552, 22596
	};
	
	private static final int[] TIAT_MINION_IDS =
	{
	        29162, 22538, 22540, 22547, 22542, 22548
	};
	
	private static final int[] ATTACKABLE_DOORS =
	{
	        12240005, 12240006, 12240007, 12240008, 12240009, 12240010, 12240013, 12240014, 12240015, 12240016, 12240017, 12240018, 12240021, 12240022, 12240023, 12240024, 12240025, 12240026, 12240028, 12240029, 12240030
	};
	
	private static final int[] ENTRANCE_ROOM_DOORS =
	{
	        12240001, 12240002
	};
	
	private static final int[] SQUARE_DOORS =
	{
	        12240003, 12240004, 12240011, 12240012, 12240019, 12240020
	};
	
	private static final NpcStringId[] TIAT_TEXT =
	{
	        NpcStringId.YOULL_REGRET_CHALLENGING_ME, NpcStringId.HA_HA_YES_DIE_SLOWLY_WRITHING_IN_PAIN_AND_AGONY
	};
	
	public Stage1(String name, String descr)
	{
		super(name, descr);
		
		load();
		addStartNpc(32526);
		addTalkId(32526);
		addStartNpc(32601);
		addTalkId(32601);
		addAttackId(18776);
		addKillId(18776);
		addSpawnId(18776, 18777, 29162, 18778);
		addKillId(18777);
		addKillId(18778);
		addAttackId(29163);
		addKillId(29163);
		addKillId(18696);
		addKillId(29162);
		addAggroRangeEnterId(29169);
		
		for (int i = 18771; i <= 18774; i++)
		{
			addTrapActionId(i);
		}
		
		for (final int mobId : _mustKillMobsId)
		{
			addKillId(mobId);
		}
	}
	
	private void load()
	{
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/npcs/spawnZones/seed_of_destruction.xml");
			if (!file.exists())
			{
				_log.warn("[Seed of Destruction] Missing seed_of_destruction.xml. The quest wont work without it!");
				return;
			}
			
			final Document doc = factory.newDocumentBuilder().parse(file);
			final Node first = doc.getFirstChild();
			if ((first != null) && "list".equalsIgnoreCase(first.getNodeName()))
			{
				for (Node n = first.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(n.getNodeName()))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("spawn".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								Node att = attrs.getNamedItem("npcId");
								if (att == null)
								{
									_log.warn("[Seed of Destruction] Missing npcId in npc List, skipping");
									continue;
								}
								final int npcId = Integer.parseInt(attrs.getNamedItem("npcId").getNodeValue());
								
								att = attrs.getNamedItem("flag");
								if (att == null)
								{
									_log.warn("[Seed of Destruction] Missing flag in npc List npcId: " + npcId + ", skipping");
									continue;
								}
								final int flag = Integer.parseInt(attrs.getNamedItem("flag").getNodeValue());
								if (!_spawnList.containsKey(flag))
								{
									_spawnList.put(flag, new ArrayList<>());
								}
								
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("loc".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										final SODSpawn spw = new SODSpawn();
										spw.npcId = npcId;
										
										att = attrs.getNamedItem("x");
										if (att != null)
										{
											spw.x = Integer.parseInt(att.getNodeValue());
										}
										else
										{
											continue;
										}
										att = attrs.getNamedItem("y");
										if (att != null)
										{
											spw.y = Integer.parseInt(att.getNodeValue());
										}
										else
										{
											continue;
										}
										att = attrs.getNamedItem("z");
										if (att != null)
										{
											spw.z = Integer.parseInt(att.getNodeValue());
										}
										else
										{
											continue;
										}
										att = attrs.getNamedItem("heading");
										if (att != null)
										{
											spw.h = Integer.parseInt(att.getNodeValue());
										}
										else
										{
											continue;
										}
										att = attrs.getNamedItem("mustKill");
										if (att != null)
										{
											spw.isNeededNextFlag = Boolean.parseBoolean(att.getNodeValue());
										}
										if (spw.isNeededNextFlag)
										{
											_mustKillMobsId.add(npcId);
										}
										_spawnList.get(flag).add(spw);
									}
									else if ("zone".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										final SODSpawn spw = new SODSpawn();
										spw.npcId = npcId;
										spw.isZone = true;
										
										att = attrs.getNamedItem("id");
										if (att != null)
										{
											spw.zone = Integer.parseInt(att.getNodeValue());
										}
										else
										{
											continue;
										}
										att = attrs.getNamedItem("count");
										if (att != null)
										{
											spw.count = Integer.parseInt(att.getNodeValue());
										}
										else
										{
											continue;
										}
										att = attrs.getNamedItem("mustKill");
										if (att != null)
										{
											spw.isNeededNextFlag = Boolean.parseBoolean(att.getNodeValue());
										}
										if (spw.isNeededNextFlag)
										{
											_mustKillMobsId.add(npcId);
										}
										_spawnList.get(flag).add(spw);
									}
								}
							}
						}
					}
					else if ("spawnZones".equalsIgnoreCase(n.getNodeName()))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("zone".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								final Node att = attrs.getNamedItem("id");
								if (att == null)
								{
									_log.warn("[Seed of Destruction] Missing id in spawnZones List, skipping");
									continue;
								}
								final int id = Integer.parseInt(att.getNodeValue());
								
								final SpawnTerritory ter = new SpawnTerritory();
								final Polygon temp = new Polygon();
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("add".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										final int x = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
										final int y = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
										final int zmin = Integer.parseInt(attrs.getNamedItem("zmin").getNodeValue());
										final int zmax = Integer.parseInt(attrs.getNamedItem("zmax").getNodeValue());
										temp.add(x, y).setZmin(zmin).setZmax(zmax);
									}
								}
								ter.add(temp);
								_spawnZoneList.put(id, ter);
							}
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("[Seed of Destruction] Could not parse data.xml file: " + e.getMessage(), e);
		}
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new SOD1World(), 110))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			spawnState((SOD1World) world);
			for (final DoorInstance door : ReflectionManager.getInstance().getReflection(world.getReflectionId()).getDoors())
			{
				if (ArrayUtils.contains(ATTACKABLE_DOORS, door.getDoorId()))
				{
					door.setIsAttackableDoor(true);
				}
			}
		}
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
		else
		{
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
	}
	
	protected boolean checkKillProgress(Npc mob, SOD1World world)
	{
		final boolean done;
		synchronized (world)
		{
			done = world.npcList.remove(mob) && (world.npcList.size() == 0);
		}
		return done;
	}
	
	private void spawnFlaggedNPCs(SOD1World world, int flag)
	{
		if (world.lock.tryLock())
		{
			try
			{
				for (final SODSpawn spw : _spawnList.get(flag))
				{
					if (spw.isZone)
					{
						for (int i = 0; i < spw.count; i++)
						{
							if (_spawnZoneList.containsKey(spw.zone))
							{
								spawn(world, spw.npcId, 0, 0, 0, 0, spw.isNeededNextFlag, true, _spawnZoneList.get(spw.zone));
							}
						}
					}
					else
					{
						spawn(world, spw.npcId, spw.x, spw.y, spw.z, spw.h, spw.isNeededNextFlag, false, null);
					}
				}
			}
			finally
			{
				world.lock.unlock();
			}
		}
	}
	
	protected boolean spawnState(SOD1World world)
	{
		if (world.lock.tryLock())
		{
			try
			{
				world.npcList.clear();
				switch (world.getStatus())
				{
					case 0 :
						spawnFlaggedNPCs(world, 0);
						break;
					case 1 :
						manageScreenMsg(world, NpcStringId.THE_ENEMIES_HAVE_ATTACKED_EVERYONE_COME_OUT_AND_FIGHT_URGH);
						for (final int i : ENTRANCE_ROOM_DOORS)
						{
							world.getReflection().openDoor(i);
						}
						spawnFlaggedNPCs(world, 1);
						break;
					case 2 :
					case 3 :
						return true;
					case 4 :
						manageScreenMsg(world, NpcStringId.OBELISK_HAS_COLLAPSED_DONT_LET_THE_ENEMIES_JUMP_AROUND_WILDLY_ANYMORE);
						for (final int i : SQUARE_DOORS)
						{
							world.getReflection().openDoor(i);
						}
						spawnFlaggedNPCs(world, 4);
						break;
					case 5 :
						world.getReflection().openDoor(12240027);
						spawnFlaggedNPCs(world, 3);
						spawnFlaggedNPCs(world, 5);
						break;
					case 6 :
						world.getReflection().openDoor(12240031);
						break;
					case 7 :
						spawnFlaggedNPCs(world, 7);
						break;
					case 8 :
						manageScreenMsg(world, NpcStringId.COME_OUT_WARRIORS_PROTECT_SEED_OF_DESTRUCTION);
						world.deviceSpawnedMobCount = 0;
						spawnFlaggedNPCs(world, 8);
						break;
					case 9 :
						break;
				}
				world.incStatus();
				return true;
			}
			finally
			{
				world.lock.unlock();
			}
		}
		return false;
	}
	
	protected void spawn(SOD1World world, int npcId, int x, int y, int z, int h, boolean addToKillTable, boolean isTerrytory, SpawnTerritory ter)
	{
		if ((npcId >= 18720) && (npcId <= 18774))
		{
			Skill skill = null;
			if (npcId <= 18728)
			{
				skill = new SkillHolder(4186, 9).getSkill();
			}
			else if (npcId <= 18736)
			{
				skill = new SkillHolder(4072, 10).getSkill();
			}
			else if (npcId <= 18770)
			{
				skill = new SkillHolder(5340, 4).getSkill();
			}
			else
			{
				skill = new SkillHolder(10002, 1).getSkill();
			}
			addTrap(npcId, x, y, z, h, skill, world.getReflection());
			return;
		}
		
		Npc npc = null;
		if (isTerrytory)
		{
			npc = addSpawn(npcId, ter, 0, false, world.getReflection());
		}
		else
		{
			npc = addSpawn(npcId, x, y, z, h, false, 0, false, world.getReflection());
		}
		
		if (addToKillTable)
		{
			world.npcList.add(npc);
		}
		npc.setIsNoRndWalk(true);
		if (npc.isInstanceType(InstanceType.Attackable))
		{
			((Attackable) npc).setSeeThroughSilentMove(true);
		}
		if (npcId == 29169)
		{
			startQuestTimer("DoorCheck", 10000, npc, null);
		}
		else if (npcId == 18696)
		{
			npc.disableCoreAI(true);
			startQuestTimer("Spawn", 5000, npc, null, true);
		}
		else if (npcId == 29163)
		{
			npc.setIsImmobilized(true);
			world.tiat = (MonsterInstance) npc;
			for (int i = 0; i < 5; i++)
			{
				addMinion(world.tiat, 29162);
			}
		}
	}
	
	private void manageScreenMsg(SOD1World world, NpcStringId stringId)
	{
		for (final int objId : world.getAllowed())
		{
			final Player player = GameObjectsStorage.getPlayer(objId);
			if (player != null)
			{
				showOnScreenMsg(player, stringId, 2, 5000);
			}
		}
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		if (npc.getId() == 29162)
		{
			final Player target = selectRndPlayer(npc);
			if (target != null)
			{
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, target, 30000);
			}
			return super.onSpawn(npc);
		}
		else
		{
			npc.disableCoreAI(true);
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if ((isSummon == false) && (player != null))
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(player.getReflectionId());
			if (tmpworld instanceof SOD1World)
			{
				final SOD1World world = (SOD1World) tmpworld;
				if (world.isStatus(7))
				{
					if (spawnState(world))
					{
						for (final int objId : world.getAllowed())
						{
							final Player pl = GameObjectsStorage.getPlayer(objId);
							if (pl != null)
							{
								pl.showQuestMovie(5);
							}
						}
						npc.deleteMe();
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof SOD1World)
		{
			final SOD1World world = (SOD1World) tmpworld;
			
			if (world.isStatus(2) && (npc.getId() == 18776))
			{
				world.setStatus(4);
				spawnFlaggedNPCs(world, 3);
			}
			else if (world.isStatus(3) && (npc.getId() == 18776))
			{
				world.setStatus(4);
				spawnFlaggedNPCs(world, 2);
			}
			else if ((world.getStatus() <= 8) && (npc.getId() == 29163))
			{
				if (npc.getCurrentHp() < (npc.getMaxHp() / 2))
				{
					if (spawnState(world))
					{
						if (npc.isImmobilized())
						{
							npc.setIsImmobilized(false);
						}
						npc.setTarget(npc);
						npc.setIsInvul(true);
						npc.doCast(SkillsParser.getInstance().getInfo(5974, 1));
						handleReenterTime(world);
						ThreadPoolManager.getInstance().schedule(new Runnable()
						{
							@Override
							public void run()
							{
								npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
								npc.setIsInvul(false);
							}
						}, SkillsParser.getInstance().getInfo(5974, 1).getHitTime());
					}
				}
				else
				{
					if (world.lastFactionNotifyTime < System.currentTimeMillis())
					{
						for (final Npc mob : World.getInstance().getAroundNpc(npc, (int) (4000 + npc.getColRadius()), 200))
						{
							if (ArrayUtils.contains(TIAT_MINION_IDS, mob.getId()))
							{
								mob.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 30000);
							}
						}
						
						if (Rnd.chance(5))
						{
							manageScreenMsg(world, TIAT_TEXT[Rnd.get(TIAT_TEXT.length)]);
						}
						world.lastFactionNotifyTime = System.currentTimeMillis() + 10000L;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof SOD1World)
		{
			final SOD1World world = (SOD1World) tmpworld;
			if (event.equalsIgnoreCase("Spawn"))
			{
				if (world.deviceSpawnedMobCount < 100)
				{
					final Attackable mob = (Attackable) addSpawn(SPAWN_MOB_IDS[getRandom(SPAWN_MOB_IDS.length)], npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ(), npc.getSpawn().getHeading(), false, 0, false, world.getReflection());
					world.deviceSpawnedMobCount++;
					mob.setSeeThroughSilentMove(true);
					mob.setRunning();
					if (world.getStatus() < 7)
					{
						mob.getAI().setIntention(CtrlIntention.MOVING, new Location(-251432, 214905, -12088, 16384), 0);
					}
				}
			}
			else if (event.equalsIgnoreCase("DoorCheck"))
			{
				final DoorInstance tmp = world.getReflection().getDoor(12240030);
				if (tmp.getCurrentHp() < tmp.getMaxHp())
				{
					world.deviceSpawnedMobCount = 0;
					spawnFlaggedNPCs(world, 6);
					manageScreenMsg(world, NpcStringId.ENEMIES_ARE_TRYING_TO_DESTROY_THE_FORTRESS_EVERYONE_DEFEND_THE_FORTRESS);
				}
				else
				{
					startQuestTimer("DoorCheck", 10000, npc, null);
				}
			}
		}
		return "";
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() == 18696)
		{
			cancelQuestTimer("Spawn", npc, null);
			return "";
		}
		
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof SOD1World)
		{
			final SOD1World world = (SOD1World) tmpworld;
			if (world.isStatus(1))
			{
				if (checkKillProgress(npc, world))
				{
					spawnState(world);
				}
			}
			else if (world.isStatus(2))
			{
				if (checkKillProgress(npc, world))
				{
					world.incStatus();
				}
			}
			else if ((world.isStatus(4)) && (npc.getId() == 18776))
			{
				spawnState(world);
			}
			else if ((world.isStatus(5)) && (npc.getId() == 18777))
			{
				if (checkKillProgress(npc, world))
				{
					spawnState(world);
				}
			}
			else if ((world.isStatus(6)) && (npc.getId() == 18778))
			{
				if (checkKillProgress(npc, world))
				{
					spawnState(world);
				}
			}
			else if (world.getStatus() >= 7)
			{
				if (npc.getId() == 29163)
				{
					world.incStatus();
					
					ReflectionManager.getInstance().getReflection(world.getReflectionId()).cleanupNpcs();
					
					for (final int objId : world.getAllowed())
					{
						final Player pl = GameObjectsStorage.getPlayer(objId);
						if (pl != null)
						{
							pl.showQuestMovie(6);
						}
					}
					
					final MinionList ml = npc.getMinionList();
					if (ml != null)
					{
						ml.onMasterDelete();
					}
					SoDManager.getInstance().addTiatKill();
					finishInstance(world, 900000, false);
				}
				else if (npc.getId() == 29162)
				{
					addMinion(world.tiat, 29162);
				}
			}
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final int npcId = npc.getId();
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		if (npcId == 32526)
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			if ((SoDManager.getInstance().isAttackStage()) || ((world != null) && (world instanceof SOD1World)))
			{
				enterInstance(player, npc);
			}
			else if (!SoDManager.getInstance().isAttackStage())
			{
				SoDManager.getInstance().teleportIntoSeed(player);
			}
		}
		else if (npcId == 32601)
		{
			teleportPlayer(player, new Location(-245802, 220528, -12104), player.getReflection(), false);
		}
		return "";
	}
	
	@Override
	public String onTrapAction(TrapInstance trap, Creature trigger, TrapAction action)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(trap.getReflectionId());
		if (tmpworld instanceof SOD1World)
		{
			final SOD1World world = (SOD1World) tmpworld;
			switch (action)
			{
				case TRAP_TRIGGERED :
					if (trap.getId() == 18771)
					{
						for (final int npcId : TRAP_18771_NPCS)
						{
							addSpawn(npcId, trap.getX(), trap.getY(), trap.getZ(), trap.getHeading(), true, 0, true, world.getReflection());
						}
					}
					else
					{
						for (final int npcId : TRAP_OTHER_NPCS)
						{
							addSpawn(npcId, trap.getX(), trap.getY(), trap.getZ(), trap.getHeading(), true, 0, true, world.getReflection());
						}
					}
					break;
			}
		}
		return null;
	}
	
	protected Player selectRndPlayer(Npc npc)
	{
		List<Player> selectPlayers = null;
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld instanceof SOD1World)
		{
			final SOD1World world = (SOD1World) tmpworld;
			
			if (world.getAllowed().size() > 0)
			{
				selectPlayers = new ArrayList<>();
				for (final int objId : world.getAllowed())
				{
					final Player player = GameObjectsStorage.getPlayer(objId);
					if (player != null)
					{
						selectPlayers.add(player);
					}
				}
			}
		}
		return selectPlayers != null ? selectPlayers.get(Rnd.get(selectPlayers.size())) : null;
	}
	
	public static void main(String[] args)
	{
		new Stage1(Stage1.class.getSimpleName(), "instances");
	}
}