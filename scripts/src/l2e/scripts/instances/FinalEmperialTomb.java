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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.geometry.Polygon;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.SpawnTerritory;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.EarthQuake;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.MagicSkillCanceled;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcInfo.Info;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.SpecialCamera;

/**
 * Rework by LordWinter 07.02.2020
 */
public class FinalEmperialTomb extends AbstractReflection
{
	private class FETWorld extends ReflectionWorld
	{
		public Lock lock = new ReentrantLock();
		public List<Npc> npcList = new CopyOnWriteArrayList<>();
		public int npcSize = 0;
		public int darkChoirPlayerCount = 0;
		public FrintezzaSong OnSong = null;
		public ScheduledFuture<?> songTask = null;
		public ScheduledFuture<?> songEffectTask = null;
		public boolean isVideo = false;
		public boolean isScarletSecondStage = false;
		public Npc frintezzaDummy = null;
		public Npc overheadDummy = null;
		public Npc portraitDummy1 = null;
		public Npc portraitDummy3 = null;
		public Npc scarletDummy = null;
		public GrandBossInstance frintezza = null;
		public GrandBossInstance activeScarlet = null;
		public List<MonsterInstance> demons = new ArrayList<>();
		public Map<MonsterInstance, Integer> portraits = new ConcurrentHashMap<>();
		public int scarlet_x = 0;
		public int scarlet_y = 0;
		public int scarlet_z = 0;
		public int scarlet_h = 0;
		public int scarlet_a = 0;
		protected Future<?> demonsSpawnTask;

		public FETWorld()
		{
		}
	}

	protected static class FETSpawn
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

	private static class FrintezzaSong
	{
		public SkillHolder skill;
		public SkillHolder effectSkill;
		public NpcStringId songName;
		public int chance;

		public FrintezzaSong(SkillHolder sk, SkillHolder esk, NpcStringId sn, int ch)
		{
			skill = sk;
			effectSkill = esk;
			songName = sn;
			chance = ch;
		}
	}

	private final Map<Integer, SpawnTerritory> _spawnZoneList = new HashMap<>();
	private final Map<Integer, List<FETSpawn>> _spawnList = new HashMap<>();
	private final List<Integer> _mustKillMobsId = new ArrayList<>();

	protected static final FrintezzaSong[] FRINTEZZASONGLIST =
	{
	        new FrintezzaSong(new SkillHolder(5007, 1), new SkillHolder(5008, 1), NpcStringId.REQUIEM_OF_HATRED, 5), new FrintezzaSong(new SkillHolder(5007, 2), new SkillHolder(5008, 2), NpcStringId.RONDO_OF_SOLITUDE, 50), new FrintezzaSong(new SkillHolder(5007, 3), new SkillHolder(5008, 3), NpcStringId.FRENETIC_TOCCATA, 70), new FrintezzaSong(new SkillHolder(5007, 4), new SkillHolder(5008, 4), NpcStringId.FUGUE_OF_JUBILATION, 90), new FrintezzaSong(new SkillHolder(5007, 5), new SkillHolder(5008, 5), NpcStringId.HYPNOTIC_MAZURKA, 100),
	};

	protected static final int[] FIRST_ROOM_DOORS =
	{
	        17130051, 17130052, 17130053, 17130054, 17130055, 17130056, 17130057, 17130058
	};

	protected static final int[] SECOND_ROOM_DOORS =
	{
	        17130061, 17130062, 17130063, 17130064, 17130065, 17130066, 17130067, 17130068, 17130069, 17130070
	};

	protected static final int[] FIRST_ROUTE_DOORS =
	{
	        17130042, 17130043
	};

	protected static final int[] SECOND_ROUTE_DOORS =
	{
	        17130045, 17130046
	};

	protected static final int[][] PORTRAIT_SPAWNS =
	{
	        {
	                29048, -89381, -153981, -9168, 3368, -89378, -153968, -9168, 3368
			},
			{
			        29048, -86234, -152467, -9168, 37656, -86261, -152492, -9168, 37656
			},
			{
			        29049, -89342, -152479, -9168, -5152, -89311, -152491, -9168, -5152
			},
			{
			        29049, -86189, -153968, -9168, 29456, -86217, -153956, -9168, 29456
			}
	};

	protected int spawnCount;

	public FinalEmperialTomb(String name, String descr)
	{
		super(name, descr);

		load();
		addStartNpc(32011, 29061);
		addTalkId(32011, 29061);
		addAttackId(29046, 29045, 29048, 29049);
		addKillId(18328, 18329, 18339, 29047, 29046, 29048, 29049, 29050, 29051);
		addKillId(_mustKillMobsId);
		addSpellFinishedId(18333);
		addSpawnId(29045, 29046, 29047, 29048, 29049, 29050, 29051, 29059);
	}

	protected void load()
	{
		spawnCount = 0;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/npcs/spawnZones/final_emperial_tomb.xml");
			if (!file.exists())
			{
				_log.error("[Final Emperial Tomb] Missing final_emperial_tomb.xml. The quest wont work without it!");
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
									_log.warn("[Final Emperial Tomb] Missing npcId in npc List, skipping");
									continue;
								}
								final int npcId = Integer.parseInt(attrs.getNamedItem("npcId").getNodeValue());

								att = attrs.getNamedItem("flag");
								if (att == null)
								{
									_log.warn("[Final Emperial Tomb] Missing flag in npc List npcId: " + npcId + ", skipping");
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
										final FETSpawn spw = new FETSpawn();
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
										spawnCount++;
									}
									else if ("zone".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										final FETSpawn spw = new FETSpawn();
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
										spawnCount++;
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
									_log.warn("[Final Emperial Tomb] Missing id in spawnZones List, skipping");
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
			_log.warn("[Final Emperial Tomb] Could not parse final_emperial_tomb.xml file: " + e.getMessage(), e);
		}
	}

	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new FETWorld(), 136))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			controlStatus((FETWorld) world);
			if ((player.getParty() == null) || (player.getParty().getCommandChannel() == null))
			{
				if (player.getInventory().getItemByItemId(8556) != null)
				{
					player.destroyItemByItemId(getName(), 8556, player.getInventory().getInventoryItemCount(8556, -1), null, true);
				}
			}
			else
			{
				if (player.getParty().getCommandChannel() != null)
				{
					for (final Player channelMember : player.getParty().getCommandChannel().getMembers())
					{
						if (channelMember != null)
						{
							if (channelMember.getInventory().getItemByItemId(8556) != null)
							{
								channelMember.destroyItemByItemId(getName(), 8556, channelMember.getInventory().getInventoryItemCount(8556, -1), null, true);
							}
						}
					}
				}
				else
				{
					for (final Player member : player.getParty().getMembers())
					{
						if (member != null)
						{
							if (member.getInventory().getItemByItemId(8556) != null)
							{
								member.destroyItemByItemId(getName(), 8556, member.getInventory().getInventoryItemCount(8556, -1), null, true);
							}
						}
					}
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
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("enter"))
		{
			enterInstance(player, npc);
			return null;
		}
		return super.onAdvEvent(event, npc, player);
	}

	protected boolean checkKillProgress(FETWorld world)
	{
		if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
		{
			return false;
		}
		
		if (world.npcSize < 10)
		{
			for (final Npc npc : world.npcList)
			{
				if (npc != null)
				{
					if (!npc.isInRangeZ(npc.getSpawn().getLocation(), Config.MAX_PURSUE_RANGE))
					{
						npc.deleteMe();
						world.npcSize--;
					}
				}
			}
		}
		return world.npcSize <= 0;
	}

	private void spawnFlaggedNPCs(FETWorld world, int flag)
	{
		if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
		{
			return;
		}
		
		if (world.lock.tryLock())
		{
			if (!world.npcList.isEmpty())
			{
				for (final Npc npc : world.npcList)
				{
					if (npc != null)
					{
						npc.deleteMe();
					}
				}
			}
			world.npcList.clear();
			world.npcSize = 0;
			try
			{
				for (final FETSpawn spw : _spawnList.get(flag))
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
				world.npcSize = world.npcList.size();
			}
			finally
			{
				world.lock.unlock();
			}
		}
	}

	protected boolean controlStatus(FETWorld world)
	{
		if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
		{
			return false;
		}
		
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
						for (final int doorId : FIRST_ROUTE_DOORS)
						{
							world.getReflection().openDoor(doorId);
						}
						spawnFlaggedNPCs(world, world.getStatus());
						break;
					case 2 :
						for (final int doorId : SECOND_ROUTE_DOORS)
						{
							world.getReflection().openDoor(doorId);
						}
						ThreadPoolManager.getInstance().schedule(new IntroTask(world, 0), 300000);
						break;
					case 3 :
						if (world.songEffectTask != null)
						{
							world.songEffectTask.cancel(false);
						}
						world.songEffectTask = null;
						world.activeScarlet.setIsInvul(true);
						if (world.activeScarlet.isCastingNow())
						{
							world.activeScarlet.abortCast();
						}
						handleReenterTime(world);
						world.activeScarlet.doCast(new SkillHolder(5017, 1).getSkill());
						ThreadPoolManager.getInstance().schedule(new SongTask(world, 2), 1500);
						break;
					case 4 :
						if (!world.isScarletSecondStage)
						{
							world.isScarletSecondStage = true;
							world.isVideo = true;
							broadCastPacket(world, new MagicSkillCanceled(world.frintezza.getObjectId()));
							if (world.songEffectTask != null)
							{
								world.songEffectTask.cancel(false);
							}
							world.songEffectTask = null;
							ThreadPoolManager.getInstance().schedule(new IntroTask(world, 23), 2000);
							ThreadPoolManager.getInstance().schedule(new IntroTask(world, 24), 2100);
						}
						break;
					case 5 :
						world.isVideo = true;
						broadCastPacket(world, new MagicSkillCanceled(world.frintezza.getObjectId()));
						if (world.songTask != null)
						{
							world.songTask.cancel(true);
						}
						if (world.songEffectTask != null)
						{
							world.songEffectTask.cancel(false);
						}
						world.songTask = null;
						world.songEffectTask = null;
						ThreadPoolManager.getInstance().schedule(new IntroTask(world, 33), 500);
						break;
					case 6 :
						for (final int doorId : FIRST_ROOM_DOORS)
						{
							world.getReflection().openDoor(doorId);
						}
						for (final int doorId : FIRST_ROUTE_DOORS)
						{
							world.getReflection().openDoor(doorId);
						}
						for (final int doorId : SECOND_ROUTE_DOORS)
						{
							world.getReflection().openDoor(doorId);
						}
						for (final int doorId : SECOND_ROOM_DOORS)
						{
							world.getReflection().closeDoor(doorId);
						}
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

	protected void spawn(FETWorld world, int npcId, int x, int y, int z, int h, boolean addToKillTable, boolean isTerrytory, SpawnTerritory ter)
	{
		if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
		{
			return;
		}
		
		Npc npc = null;
		if (isTerrytory)
		{
			npc = addSpawn(npcId, ter, 0L, false, world.getReflection());
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
		
		if (npcId == 18328)
		{
			npc.disableCoreAI(true);
		}
		
		if (npcId == 18339)
		{
			world.darkChoirPlayerCount++;
		}
	}

	private class DemonSpawnTask implements Runnable
	{
		private final FETWorld _world;

		DemonSpawnTask(FETWorld world)
		{
			_world = world;
		}

		@Override
		public void run()
		{
			if (_world != null)
			{
				if ((ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world) || _world.portraits.isEmpty())
				{
					return;
				}
				for (final int i : _world.portraits.values())
				{
					if (_world.demons.size() > 24)
					{
						break;
					}
					final MonsterInstance demon = (MonsterInstance) addSpawn(PORTRAIT_SPAWNS[i][0] + 2, PORTRAIT_SPAWNS[i][5], PORTRAIT_SPAWNS[i][6], PORTRAIT_SPAWNS[i][7], PORTRAIT_SPAWNS[i][8], false, 0, false, _world.getReflection());
					_world.demons.add(demon);
				}
				_world.demonsSpawnTask = ThreadPoolManager.getInstance().schedule(new DemonSpawnTask(_world), 20000);
			}
		}
	}

	private class SoulBreakingArrow implements Runnable
	{
		private final Npc _npc;

		protected SoulBreakingArrow(Npc npc)
		{
			_npc = npc;
		}

		@Override
		public void run()
		{
			_npc.setScriptValue(0);
		}
	}

	private class SongTask implements Runnable
	{
		private final FETWorld _world;
		private final int _status;

		SongTask(FETWorld world, int status)
		{
			_world = world;
			_status = status;
		}

		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			switch (_status)
			{
				case 0 :
					if (_world.isVideo)
					{
						_world.songTask = ThreadPoolManager.getInstance().schedule(new SongTask(_world, 0), 1000);
					}
					else if ((_world.frintezza != null) && !_world.frintezza.isDead())
					{
						if (_world.frintezza.getScriptValue() != 1)
						{
							final int rnd = getRandom(100);
							for (final FrintezzaSong element : FRINTEZZASONGLIST)
							{
								if (rnd < element.chance)
								{
									_world.OnSong = element;
									broadCastPacket(_world, new ExShowScreenMessage(2, -1, 2, 0, 0, 0, 0, true, 4000, false, null, element.songName, null));
									broadCastPacket(_world, new MagicSkillUse(_world.frintezza, _world.frintezza, element.skill.getId(), element.skill.getLvl(), element.skill.getSkill().getHitTime(), 0));
									_world.songEffectTask = ThreadPoolManager.getInstance().schedule(new SongTask(_world, 1), 3000);
									_world.songTask = ThreadPoolManager.getInstance().schedule(new SongTask(_world, 0), element.skill.getSkill().getHitTime());
									break;
								}
							}
						}
						else
						{
							ThreadPoolManager.getInstance().schedule(new SoulBreakingArrow(_world.frintezza), 35000);
						}
					}
					break;
				case 1 :
					_world.songEffectTask = null;
					final Skill skill = _world.OnSong.effectSkill.getSkill();
					if (skill == null)
					{
						return;
					}

					if ((_world.activeScarlet == null) || _world.activeScarlet.isDead() || !_world.activeScarlet.isVisible())
					{
						return;
					}
					if (_world.isVideo)
					{
						return;
					}
					_world.activeScarlet.doCast(SkillsParser.getInstance().getInfo(skill.getId(), skill.getLevel()));
					break;
				case 2 :
					_world.activeScarlet.setRHandId(7903);
					_world.activeScarlet.setIsInvul(false);
					break;
			}
		}
	}

	private class IntroTask implements Runnable
	{
		private final FETWorld _world;
		private final int _status;

		IntroTask(FETWorld world, int status)
		{
			_world = world;
			_status = status;
		}

		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			switch (_status)
			{
				case 0 :
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 1), 27000);
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 2), 30000);
					broadCastPacket(_world, new EarthQuake(-87784, -155083, -9087, 45, 27));
					break;
				case 1 :
					for (final int doorId : FIRST_ROOM_DOORS)
					{
						_world.getReflection().closeDoor(doorId);
					}
					for (final int doorId : FIRST_ROUTE_DOORS)
					{
						_world.getReflection().closeDoor(doorId);
					}
					for (final int doorId : SECOND_ROOM_DOORS)
					{
						_world.getReflection().closeDoor(doorId);
					}
					for (final int doorId : SECOND_ROUTE_DOORS)
					{
						_world.getReflection().closeDoor(doorId);
					}
					addSpawn(29061, -87904, -141296, -9168, 0, false, 0, false, _world.getReflection());
					break;
				case 2 :
					_world.frintezzaDummy = addSpawn(29052, -87784, -155083, -9087, 16048, false, 0, false, _world.getReflection());
					_world.frintezzaDummy.setIsInvul(true);
					_world.frintezzaDummy.setIsImmobilized(true);

					_world.overheadDummy = addSpawn(29052, -87784, -153298, -9175, 16384, false, 0, false, _world.getReflection());
					_world.overheadDummy.setIsInvul(true);
					_world.overheadDummy.setIsImmobilized(true);
					_world.overheadDummy.setCollisionHeight(600);
					broadCastPacket(_world, new Info(_world.overheadDummy, null));

					_world.portraitDummy1 = addSpawn(29052, -89566, -153168, -9165, 16048, false, 0, false, _world.getReflection());
					_world.portraitDummy1.setIsImmobilized(true);
					_world.portraitDummy1.setIsInvul(true);

					_world.portraitDummy3 = addSpawn(29052, -86004, -153168, -9165, 16048, false, 0, false, _world.getReflection());
					_world.portraitDummy3.setIsImmobilized(true);
					_world.portraitDummy3.setIsInvul(true);

					_world.scarletDummy = addSpawn(29053, -87784, -153298, -9175, 16384, false, 0, false, _world.getReflection());
					_world.scarletDummy.setIsInvul(true);
					_world.scarletDummy.setIsImmobilized(true);

					stopPc();
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 3), 1000);
					break;
				case 3 :
					broadCastPacket(_world, new SpecialCamera(_world.overheadDummy, 0, 75, -89, 0, 100, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SpecialCamera(_world.overheadDummy, 0, 75, -89, 0, 100, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SpecialCamera(_world.overheadDummy, 300, 90, -10, 6500, 7000, 0, 0, 1, 0, 0));

					_world.frintezza = (GrandBossInstance) addSpawn(29045, -87780, -155086, -9080, 16384, false, 0, false, _world.getReflection());
					_world.frintezza.setIsImmobilized(true);
					_world.frintezza.setIsRunner(true);
					_world.frintezza.setIsInvul(true);
					_world.frintezza.disableAllSkills();

					for (final int[] element : PORTRAIT_SPAWNS)
					{
						final MonsterInstance demon = (MonsterInstance) addSpawn(element[0] + 2, element[5], element[6], element[7], element[8], false, 0, false, _world.getReflection());
						demon.setIsImmobilized(true);
						demon.disableAllSkills();
						_world.demons.add(demon);
					}
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 4), 6500);
					break;
				case 4 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezzaDummy, 1800, 90, 8, 6500, 7000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 5), 900);
					break;
				case 5 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezzaDummy, 140, 90, 10, 2500, 4500, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 6), 4000);
					break;
				case 6 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 40, 75, -10, 0, 1000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 40, 75, -10, 0, 12000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 7), 1350);
					break;
				case 7 :
					broadCastPacket(_world, new SocialAction(_world.frintezza.getObjectId(), 2));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 8), 7000);
					break;
				case 8 :
					_world.frintezzaDummy.deleteMe();
					_world.frintezzaDummy = null;
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 9), 1000);
					break;
				case 9 :
					if (_world.demons.size() >= 3)
					{
						broadCastPacket(_world, new SocialAction(_world.demons.get(1).getObjectId(), 1));
						broadCastPacket(_world, new SocialAction(_world.demons.get(2).getObjectId(), 1));
					}
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 10), 400);
					break;
				case 10 :
					if (_world.demons.size() > 0)
					{
						broadCastPacket(_world, new SocialAction(_world.demons.get(0).getObjectId(), 1));
						if (_world.demons.size() > 3)
						{
							broadCastPacket(_world, new SocialAction(_world.demons.get(3).getObjectId(), 1));
						}
					}
					sendPacketX(new SpecialCamera(_world.portraitDummy1, 1000, 118, 0, 0, 1000, 0, 0, 1, 0, 0), new SpecialCamera(_world.portraitDummy3, 1000, 62, 0, 0, 1000, 0, 0, 1, 0, 0), -87784);
					sendPacketX(new SpecialCamera(_world.portraitDummy1, 1000, 118, 0, 0, 10000, 0, 0, 1, 0, 0), new SpecialCamera(_world.portraitDummy3, 1000, 62, 0, 0, 10000, 0, 0, 1, 0, 0), -87784);
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 11), 2000);
					break;
				case 11 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 240, 90, 0, 0, 1000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 240, 90, 25, 5500, 10000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SocialAction(_world.frintezza.getObjectId(), 3));
					_world.portraitDummy1.deleteMe();
					_world.portraitDummy3.deleteMe();
					_world.portraitDummy1 = null;
					_world.portraitDummy3 = null;
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 12), 4500);
					break;
				case 12 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 100, 195, 35, 0, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 13), 700);
					break;
				case 13 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 100, 195, 35, 0, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 14), 1300);
					break;
				case 14 :
					broadCastPacket(_world, new ExShowScreenMessage(NpcStringId.MOURNFUL_CHORALE_PRELUDE, 2, 5000));
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 120, 180, 45, 1500, 10000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new MagicSkillUse(_world.frintezza, _world.frintezza, 5006, 1, 34000, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 15), 1500);
					break;
				case 15 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 520, 135, 45, 8000, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 16), 7500);
					break;
				case 16 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 1500, 110, 25, 10000, 13000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 17), 9500);
					break;
				case 17 :
					broadCastPacket(_world, new SpecialCamera(_world.overheadDummy, 930, 160, -20, 0, 1000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SpecialCamera(_world.overheadDummy, 600, 180, -25, 0, 10000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new MagicSkillUse(_world.scarletDummy, _world.overheadDummy, 5004, 1, 5800, 0));

					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 18), 5000);
					break;
				case 18 :
					_world.activeScarlet = (GrandBossInstance) addSpawn(29046, -87789, -153295, -9176, 16384, false, 0, false, _world.getReflection());
					_world.activeScarlet.setRHandId(8204);
					_world.activeScarlet.setIsInvul(true);
					_world.activeScarlet.setIsImmobilized(true);
					_world.activeScarlet.disableAllSkills();
					broadCastPacket(_world, new SocialAction(_world.activeScarlet.getObjectId(), 3));
					broadCastPacket(_world, new SpecialCamera(_world.scarletDummy, 800, 180, 10, 1000, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 19), 2100);
					break;
				case 19 :
					broadCastPacket(_world, new SpecialCamera(_world.activeScarlet, 300, 60, 8, 0, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 20), 2000);
					break;
				case 20 :
					broadCastPacket(_world, new SpecialCamera(_world.activeScarlet, 500, 90, 10, 3000, 5000, 0, 0, 1, 0, 0));
					_world.songTask = ThreadPoolManager.getInstance().schedule(new SongTask(_world, 0), 100);
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 21), 3000);
					break;
				case 21 :
					for (int i = 0; i < PORTRAIT_SPAWNS.length; i++)
					{
						final MonsterInstance portrait = (MonsterInstance) addSpawn(PORTRAIT_SPAWNS[i][0], PORTRAIT_SPAWNS[i][1], PORTRAIT_SPAWNS[i][2], PORTRAIT_SPAWNS[i][3], PORTRAIT_SPAWNS[i][4], false, 0, false, _world.getReflection());
						_world.portraits.put(portrait, i);
					}

					_world.overheadDummy.deleteMe();
					_world.scarletDummy.deleteMe();
					_world.overheadDummy = null;
					_world.scarletDummy = null;

					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 22), 2000);
					break;
				case 22 :
					for (final MonsterInstance demon : _world.demons)
					{
						demon.setIsImmobilized(false);
						demon.enableAllSkills();
					}
					_world.activeScarlet.setIsInvul(false);
					_world.activeScarlet.setIsImmobilized(false);
					_world.activeScarlet.enableAllSkills();
					_world.activeScarlet.setRunning();
					_world.activeScarlet.doCast(new SkillHolder(5004, 1).getSkill());
					_world.frintezza.enableAllSkills();
					_world.frintezza.disableCoreAI(true);
					_world.frintezza.setIsMortal(false);
					startPc();

					_world.demonsSpawnTask = ThreadPoolManager.getInstance().schedule(new DemonSpawnTask(_world), 20000);
					break;
				case 23 :
					broadCastPacket(_world, new SocialAction(_world.frintezza.getObjectId(), 4));
					break;
				case 24 :
					stopPc();
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 250, 120, 15, 0, 1000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 250, 120, 15, 0, 10000, 0, 0, 1, 0, 0));
					_world.activeScarlet.abortAttack();
					_world.activeScarlet.abortCast();
					_world.activeScarlet.setIsInvul(true);
					_world.activeScarlet.setIsImmobilized(true);
					_world.activeScarlet.disableAllSkills();
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 25), 7000);
					break;
				case 25 :
					broadCastPacket(_world, new MagicSkillUse(_world.frintezza, _world.frintezza, 5006, 1, 34000, 0));
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 500, 70, 15, 3000, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 26), 3000);
					break;
				case 26 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 2500, 90, 12, 6000, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 27), 3000);
					break;
				case 27 :
					_world.scarlet_x = _world.activeScarlet.getX();
					_world.scarlet_y = _world.activeScarlet.getY();
					_world.scarlet_z = _world.activeScarlet.getZ();
					_world.scarlet_h = _world.activeScarlet.getHeading();
					if (_world.scarlet_h < 32768)
					{
						_world.scarlet_a = Math.abs(180 - (int) (_world.scarlet_h / 182.044444444));
					}
					else
					{
						_world.scarlet_a = Math.abs(540 - (int) (_world.scarlet_h / 182.044444444));
					}
					broadCastPacket(_world, new SpecialCamera(_world.activeScarlet, 250, _world.scarlet_a, 12, 0, 1000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SpecialCamera(_world.activeScarlet, 250, _world.scarlet_a, 12, 0, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 28), 500);
					break;
				case 28 :
					_world.activeScarlet.doDie(_world.activeScarlet);
					broadCastPacket(_world, new SpecialCamera(_world.activeScarlet, 450, _world.scarlet_a, 14, 8000, 8000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 29), 6250);
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 30), 7200);
					break;
				case 29 :
					_world.activeScarlet.deleteMe();
					_world.activeScarlet = null;
					break;
				case 30 :
					_world.activeScarlet = (GrandBossInstance) addSpawn(29047, _world.scarlet_x, _world.scarlet_y, _world.scarlet_z, _world.scarlet_h, false, 0, false, _world.getReflection());
					_world.activeScarlet.setIsInvul(true);
					_world.activeScarlet.setIsImmobilized(true);
					_world.activeScarlet.disableAllSkills();
					for (final MonsterInstance demon : _world.demons)
					{
						if (demon != null)
						{
							demon.setIsImmobilized(true);
							demon.disableAllSkills();
						}
					}
					broadCastPacket(_world, new SpecialCamera(_world.activeScarlet, 450, _world.scarlet_a, 12, 500, 14000, 0, 0, 1, 0, 0));

					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 31), 8100);
					break;
				case 31 :
					broadCastPacket(_world, new SocialAction(_world.activeScarlet.getObjectId(), 2));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 32), 9000);
					break;
				case 32 :
					startPc();
					_world.activeScarlet.setIsInvul(false);
					_world.activeScarlet.setIsImmobilized(false);
					_world.activeScarlet.enableAllSkills();
					for (final MonsterInstance demon : _world.demons)
					{
						if (demon != null)
						{
							demon.setIsImmobilized(false);
							demon.enableAllSkills();
						}
					}
					_world.isVideo = false;
					break;
				case 33 :
					broadCastPacket(_world, new SpecialCamera(_world.activeScarlet, 300, _world.scarlet_a - 180, 5, 0, 7000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SpecialCamera(_world.activeScarlet, 200, _world.scarlet_a, 85, 4000, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 34), 7400);
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 35), 7500);
					break;
				case 34 :
					_world.frintezza.doDie(_world.frintezza);
					break;
				case 35 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 100, 120, 5, 0, 7000, 0, 0, 1, 0, 0));
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 100, 90, 5, 5000, 15000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 36), 7000);
					break;
				case 36 :
					broadCastPacket(_world, new SpecialCamera(_world.frintezza, 900, 90, 25, 7000, 10000, 0, 0, 1, 0, 0));
					ThreadPoolManager.getInstance().schedule(new IntroTask(_world, 37), 9000);
					break;
				case 37 :
					controlStatus(_world);
					_world.isVideo = false;
					startPc();
					break;
			}
		}

		private void stopPc()
		{
			for (final int objId : _world.getAllowed())
			{
				final Player player = GameObjectsStorage.getPlayer(objId);
				if ((player != null) && player.isOnline() && (player.getReflectionId() == _world.getReflectionId()))
				{
					player.abortAttack();
					player.abortCast();
					player.disableAllSkills();
					player.setTarget(null);
					player.stopMove(null);
					player.setIsImmobilized(true);
					player.getAI().setIntention(CtrlIntention.IDLE);
				}
			}
		}

		private void startPc()
		{
			for (final int objId : _world.getAllowed())
			{
				final Player player = GameObjectsStorage.getPlayer(objId);
				if ((player != null) && player.isOnline() && (player.getReflectionId() == _world.getReflectionId()))
				{
					player.enableAllSkills();
					player.setIsImmobilized(false);
				}
			}
		}

		private void sendPacketX(GameServerPacket packet1, GameServerPacket packet2, int x)
		{
			for (final int objId : _world.getAllowed())
			{
				final Player player = GameObjectsStorage.getPlayer(objId);
				if ((player != null) && player.isOnline() && (player.getReflectionId() == _world.getReflectionId()))
				{
					if (player.getX() < x)
					{
						player.sendPacket(packet1);
					}
					else
					{
						player.sendPacket(packet2);
					}
				}
			}
		}
	}

	private class StatusTask implements Runnable
	{
		private final FETWorld _world;
		private final int _status;

		StatusTask(FETWorld world, int status)
		{
			_world = world;
			_status = status;
		}

		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			switch (_status)
			{
				case 0 :
					ThreadPoolManager.getInstance().schedule(new StatusTask(_world, 1), 2000);
					for (final int doorId : FIRST_ROOM_DOORS)
					{
						_world.getReflection().openDoor(doorId);
					}
					break;
				case 1 :
					addAggroToMobs();
					break;
				case 2 :
					ThreadPoolManager.getInstance().schedule(new StatusTask(_world, 3), 100);
					for (final int doorId : SECOND_ROOM_DOORS)
					{
						_world.getReflection().openDoor(doorId);
					}
					break;
				case 3 :
					addAggroToMobs();
					break;
				case 4 :
					controlStatus(_world);
					break;
			}
		}

		private void addAggroToMobs()
		{
			Player target = GameObjectsStorage.getPlayer(_world.getAllowed().get(getRandom(_world.getAllowed().size())));
			if ((target == null) || (target.getReflectionId() != _world.getReflectionId()) || target.isDead() || target.isFakeDeath())
			{
				for (final int objId : _world.getAllowed())
				{
					target = GameObjectsStorage.getPlayer(objId);
					if ((target != null) && (target.getReflectionId() == _world.getReflectionId()) && !target.isDead() && !target.isFakeDeath())
					{
						break;
					}
					target = null;
				}
			}
			for (final Npc mob : _world.npcList)
			{
				mob.setRunning();
				if (target != null)
				{
					((MonsterInstance) mob).addDamageHate(target, 0, 500);
					mob.getAI().setIntention(CtrlIntention.ATTACK, target);
				}
				else
				{
					mob.getAI().setIntention(CtrlIntention.MOVING, new Location(-87904, -141296, -9168, 0), 0);
				}
			}
		}
	}

	protected void broadCastPacket(FETWorld world, GameServerPacket packet)
	{
		for (final int objId : world.getAllowed())
		{
			final Player player = GameObjectsStorage.getPlayer(objId);
			if ((player != null) && player.isOnline() && (player.getReflectionId() == world.getReflectionId()))
			{
				player.sendPacket(packet);
			}
		}
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof FETWorld)
		{
			final FETWorld world = (FETWorld) tmpworld;
			
			if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
			{
				return null;
			}
			
			if ((npc.getId() == 29046) && (world.isStatus(3)) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.80)))
			{
				controlStatus(world);
			}
			else if ((npc.getId() == 29046) && (world.isStatus(4)) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.20)))
			{
				controlStatus(world);
			}
			if (skill != null)
			{
				if ((npc.getId() == 29048 || npc.getId() == 29049) && (skill.getId() == 2276))
				{
					npc.doDie(attacker);
				}
				else if ((npc.getId() == 29045) && (skill.getId() == 2234))
				{
					npc.setScriptValue(1);
					npc.setTarget(null);
					npc.getAI().setIntention(CtrlIntention.IDLE);
				}
			}
		}
		return null;
	}

	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (skill.isSuicideAttack())
		{
			return onKill(npc, null, false);
		}
		return super.onSpellFinished(npc, player, skill);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof FETWorld)
		{
			final FETWorld world = (FETWorld) tmpworld;
			
			if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
			{
				return null;
			}
			
			if (npc.getId() == 18328)
			{
				ThreadPoolManager.getInstance().schedule(new StatusTask(world, 0), 2000);
			}
			else if (npc.getId() == 18339)
			{
				world.darkChoirPlayerCount--;
				if (world.darkChoirPlayerCount < 1)
				{
					ThreadPoolManager.getInstance().schedule(new StatusTask(world, 2), 2000);
				}
			}
			else if (npc.getId() == 29046)
			{
				if (world.isStatus(3))
				{
					handleReenterTime(world);
					world.setStatus(4);
					controlStatus(world);
				}
				else if (world.isStatus(4) && !world.isScarletSecondStage)
				{
					controlStatus(world);
				}
			}
			else if (npc.getId() == 29047)
			{
				if (world.demonsSpawnTask != null)
				{
					world.demonsSpawnTask.cancel(true);
					world.demonsSpawnTask = null;
				}
				
				for (final Npc demon : world.demons)
				{
					if (demon != null)
					{
						demon.deleteMe();
					}
				}
				
				for (final Npc portrait : world.portraits.keySet())
				{
					if (portrait != null)
					{
						portrait.deleteMe();
					}
				}
				world.demons.clear();
				world.portraits.clear();
				controlStatus(world);
				finishInstance(world, false);
			}
			else if (world.getStatus() <= 2)
			{
				if (world.npcList.contains(npc))
				{
					world.npcList.remove(npc);
					world.npcSize--;
					if (checkKillProgress(world))
					{
						controlStatus(world);
					}
				}
				
				if (npc.getId() == 18329 && getRandom(100) < 5)
				{
					((MonsterInstance) npc).dropSingleItem(player, 8556, 1);
				}
			}
			else if (world.demons.contains(npc))
			{
				world.demons.remove(npc);
			}
			else if (world.portraits.containsKey(npc))
			{
				world.portraits.remove(npc);
			}
		}
		return "";
	}

	@Override
	public String onSpawn(Npc npc)
	{
		if (npc.isAttackable())
		{
			((Attackable) npc).setSeeThroughSilentMove(true);
		}
		return super.onSpawn(npc);
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
		if (npcId == 32011)
		{
			enterInstance(player, npc);
		}
		else if (npc.getId() == 29061)
		{
			final int x = -87534 + getRandom(500);
			final int y = -153048 + getRandom(500);
			if (BotFunctions.getInstance().isAutoTpByIdEnable(player))
			{
				BotFunctions.getInstance().getAutoTeleportById(player, player.getLocation(), new Location(x, y, -9165), 1000);
				return null;
			}
			player.teleToLocation(x, y, -9165, true, player.getReflection());
			return null;
		}
		return "";
	}

	public static void main(String[] args)
	{
		new FinalEmperialTomb(FinalEmperialTomb.class.getSimpleName(), "instances");
	}
}