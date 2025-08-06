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

import l2e.commons.util.Util;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExStartScenePlayer;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.scripts.quests._195_SevenSignSecretRitualOfThePriests;

/**
 * Rework by LordWinter 27.10.2020
 */
public final class SecretRitualOfThePriests extends AbstractReflection
{
	private class SROPWorld extends ReflectionWorld
	{
		public SROPWorld()
		{
		}
	}
	
	protected static final int[][] MOVING_GUARDS =
	{
	        {
	                18835, -75048, 212116, -7312, -74842, 212116, -7312
			},
			{
			        18835, -75371, 212116, -7312, -75628, 212116, -7312
			},
			{
			        18835, -74480, 212116, -7312, -74253, 212116, -7312
			},
			{
			        18835, -74703, 211466, -7312, -74703, 211172, -7312
			},
			{
			        18835, -75197, 211466, -7312, -75197, 211172, -7312
			},
			{
			        18834, -75245, 210148, -7415, -74677, 210148, -7415
			},
			{
			        18834, -74683, 209819, -7415, -75241, 209819, -7415
			},
			{
			        18834, -74224, 208285, -7511, -74498, 208285, -7511
			},
			{
			        18834, -74202, 207063, -7509, -74508, 207063, -7509
			},
			{
			        18834, -74954, 206671, -7511, -74954, 206356, -7511
			},
			{
			        18834, -74270, 206518, -7511, -75654, 206518, -7511
			},
			{
			        18834, -75412, 206894, -7504, -75699, 206894, -7504
			},
			{
			        18834, -75553, 208838, -7511, -75553, 207660, -7511
			},
			{
			        18834, -76390, 207855, -7607, -76623, 207855, -7607
			},
			{
			        18834, -76610, 208182, -7606, -76392, 208182, -7606
			},
			{
			        18834, -76384, 208832, -7606, -76620, 208832, -7606
			},
			{
			        18834, -76914, 209443, -7610, -76914, 209195, -7610
			},
			{
			        18834, -77188, 209191, -7607, -77188, 209440, -7607
			},
			{
			        18835, -78039, 208472, -7703, -77369, 208472, -7703
			},
			{
			        18835, -77703, 208231, -7701, -77703, 207284, -7701
			},
			{
			        18835, -77304, 208027, -7701, -76979, 208027, -7703
			},
			{
			        18835, -77044, 207796, -7701, -78350, 207796, -7704
			},
			{
			        18835, -78085, 208038, -7701, -78454, 208038, -7703
			},
			{
			        18835, -77336, 207413, -7702, -77032, 207112, -7703
			},
			{
			        18834, -78894, 206130, -7893, -78729, 206298, -7893
			},
			{
			        18834, -79050, 206272, -7893, -78874, 206442, -7893
			},
			{
			        18834, -79360, 206372, -7893, -79360, 206718, -7893
			},
			{
			        18834, -78910, 205582, -7893, -78748, 205416, -7893
			},
			{
			        18834, -79057, 205436, -7893, -78899, 205275, -7893
			},
			{
			        18834, -79361, 205336, -7893, -79363, 204998, -7893
			},
			{
			        18834, -79655, 205440, -7893, -79820, 205273, -7893
			},
			{
			        18834, -79802, 205579, -7893, -79964, 205415, -7893
			},
			{
			        18834, -79792, 206111, -7893, -79964, 206295, -7893
			},
			{
			        18834, -79648, 206258, -7893, -79814, 206430, -7893
			},
			{
			        27351, -81963, 205857, -7989, -81085, 205857, -7989
			}
	};

	private SecretRitualOfThePriests()
	{
		super(SecretRitualOfThePriests.class.getSimpleName(), "instances");

		addStartNpc(32575, 32577, 32578);
		addTalkId(32575, 32577, 32578);
		
		addEnterZoneId(20500, 20501, 20502);
		
		addAggroRangeEnterId(18834, 18835, 27351);
		addAttackId(18834, 18835, 27351);
	}
	
	public final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new SROPWorld(), 111))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _195_SevenSignSecretRitualOfThePriests.class.getSimpleName() + "/32575-2.htm");
			player.sendPacket(html);
			
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			spawnMovingGuards((SROPWorld) world);
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
	protected boolean checkSoloType(Player player, Npc npc, ReflectionTemplate template)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		if (!player.isTransformed() || (player.isTransformed() && player.getTransformationId() != 113))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _195_SevenSignSecretRitualOfThePriests.class.getSimpleName() + "/32575-1.htm");
			player.sendPacket(html);
			return false;
		}
		return super.checkSoloType(player, npc, template);
	}
	
	@Override
	public final String onEnterZone(Creature character, ZoneType zone)
	{
		if (character.isPlayer())
		{
			final ReflectionWorld rWorld = ReflectionManager.getInstance().getPlayerWorld(character.getActingPlayer());
			if ((rWorld != null) && rWorld instanceof SROPWorld)
			{
				final SROPWorld world = (SROPWorld) rWorld;
				switch (zone.getId())
				{
					case 20500 :
						if (world.isStatus(0))
						{
							world.incStatus();
						}
						break;
					case 20501 :
						if (world.isStatus(1))
						{
							world.incStatus();
						}
						break;
					case 20502 :
						if (world.isStatus(2))
						{
							world.incStatus();
						}
						break;
				}
			}
		}
		return null;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("openDoor"))
		{
			final ReflectionWorld rWorld = ReflectionManager.getInstance().getPlayerWorld(player);
			if ((rWorld != null) && rWorld instanceof SROPWorld)
			{
				openDoor(player, (SROPWorld) rWorld);
				
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _195_SevenSignSecretRitualOfThePriests.class.getSimpleName() + "/32577-1.htm");
				player.sendPacket(html);
			}
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		if (npc.getId() == 32578)
		{
			final ReflectionWorld rWorld = ReflectionManager.getInstance().getPlayerWorld(player);
			if ((rWorld != null) && rWorld instanceof SROPWorld)
			{
				openDoor(player, (SROPWorld) rWorld);
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _195_SevenSignSecretRitualOfThePriests.class.getSimpleName() + "/32578-0.htm");
				player.sendPacket(html);
			}
		}
		else if (npc.getId() == 32575)
		{
			enterInstance(player, npc);
		}
		return null;
	}
	
	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		handleReturnMagic(player, npc);
		return super.onAggroRangeEnter(npc, player, isSummon);
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		handleReturnMagic(attacker, npc);
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	private void handleReturnMagic(Player player, Npc npc)
	{
		final ReflectionWorld rWorld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if ((rWorld != null) && rWorld instanceof SROPWorld)
		{
			final SROPWorld world = (SROPWorld) rWorld;
			if (npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				switch (npc.getId())
				{
					case 18834 :
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), 0, npc.getId(), NpcStringId.HOW_DARE_YOU_INTRUDE_WITH_THAT_TRANSFORMATION_GET_LOST));
						break;
					case 18835 :
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), 0, npc.getId(), NpcStringId.INTRUDER_PROTECT_THE_PRIESTS_OF_DAWN));
						break;
					case 27351 :
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), 0, npc.getId(), NpcStringId.WHO_ARE_YOU_A_NEW_FACE_LIKE_YOU_CANT_APPROACH_THIS_PLACE));
						break;
				}
			
				if (!npc.isCastingNow())
				{
					npc.broadcastPacketToOthers(2000, new MagicSkillUse(npc, player, 5978, 1, 2400, 0));
				}
				
				Location loc = null;
				switch (world.getStatus())
				{
					case 0 :
						loc = new Location(-76158, 213412, -7120);
						break;
					case 1 :
						loc = new Location(-74959, 209240, -7472);
						break;
					case 2 :
						loc = new Location(-77706, 208994, -7616);
						break;
					case 3 :
						loc = new Location(-80176, 205855, -7893);
						break;
				}
				ThreadPoolManager.getInstance().schedule(new Teleport(player, loc, world), 1000);
				ThreadPoolManager.getInstance().schedule(new ReturnTask(npc, world), 5000);
			}
		}
	}
	
	private void spawnMovingGuards(SROPWorld world)
	{
		if (world != null)
		{
			for (final int[] element : MOVING_GUARDS)
			{
				ThreadPoolManager.getInstance().schedule(new MoveTask(addSpawn(element[0], element[1], element[2], element[3], 0, false, 0, false, world.getReflection()), world, element[1], element[2], element[3], element[4], element[5], element[6], getMoveDelay(element)), getRandom(1000, 5000));
			}
		}
	}
	
	private class MoveTask implements Runnable
	{
		private final Npc _npc;
		private final SROPWorld _world;
		private final int _x1;
		private final int _y1;
		private final int _z1;
		private final int _x2;
		private final int _y2;
		private final int _z2;
		private final int _moveDelay;
		
		protected MoveTask(Npc npc, SROPWorld world, int x1, int y1, int z1, int x2, int y2, int z2, int moveDelay)
		{
			_npc = npc;
			_world = world;
			_x1 = x1;
			_y1 = y1;
			_z1 = z1;
			_x2 = x2;
			_y2 = y2;
			_z2 = z2;
			_moveDelay = moveDelay;
		}
		
		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null && _npc != null)
			{
				if (_npc.getDistance(_x1, _y1) <= 40)
				{
					_npc.getAI().setIntention(CtrlIntention.MOVING, new Location(_x2, _y2, _z2, 0), 0);
				}
				else if (_npc.getDistance(_x2, _y2) <= 40)
				{
					_npc.getAI().setIntention(CtrlIntention.MOVING, new Location(_x1, _y1, _z1, 0), 0);
				}
				else
				{
					_npc.getAI().setIntention(CtrlIntention.MOVING, new Location(_x1, _y1, _z1, 0), 0);
				}
				ThreadPoolManager.getInstance().schedule(new MoveTask(_npc, _world, _x1, _y1, _z1, _x2, _y2, _z2, _moveDelay), _moveDelay);
			}
		}
	}
	
	private class ReturnTask implements Runnable
	{
		private final Npc _npc;
		private final SROPWorld _world;

		protected ReturnTask(Npc npc, SROPWorld world)
		{
			_npc = npc;
			_world = world;
		}

		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}

			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null && _npc != null)
			{
				_npc.setWalking();
				_npc.setScriptValue(0);
				_npc.getAI().setIntention(CtrlIntention.MOVING, new Location(_npc.getSpawn().getX(), _npc.getSpawn().getY(), _npc.getSpawn().getZ(), _npc.getSpawn().getHeading()), 0);
			}
		}
	}
	
	private final class Teleport implements Runnable
	{
		private final Player _player;
		private final Location _loc;
		private final SROPWorld _world;
		
		public Teleport(Player player, Location loc, SROPWorld world)
		{
			_player = player;
			_loc = loc;
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				if (_loc != null)
				{
					_player.teleToLocation(_loc, false, _player.getReflection());
				}
			}
		}
	}
	
	private synchronized void openDoor(Player player, SROPWorld world)
	{
		for (final DoorInstance door : ReflectionManager.getInstance().getReflection(world.getReflectionId()).getDoors())
		{
			switch (door.getDoorId())
			{
				case 17240001 :
					for (final Player pc : World.getInstance().getAroundPlayers(door, 500, 200))
					{
						if ((pc == player) && door.isClosed())
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.USING_INVISIBLE_SKILL_SNEAK_IN));
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.MALE_GUARDS_CAN_DETECT_FEMALE_CANT));
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FEMALE_GUARDS_NOTICE_FROM_FAR_AWAY_BEWARE));
							door.openMe();
						}
					}
					break;
				case 17240003 :
					for (final Player pc : World.getInstance().getAroundPlayers(door, 500, 200))
					{
						if ((pc == player) && door.isClosed())
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DOOR_IS_ENTRANCE_APPROACH_DEVICE));
							door.openMe();
							player.showQuestMovie(ExStartScenePlayer.SSQ_RITUAL_OF_PRIEST);
						}
					}
					break;
				case 17240005 :
					for (final Player pc : World.getInstance().getAroundPlayers(door, 500, 200))
					{
						if ((pc == player) && door.isClosed())
						{
							door.openMe();
						}
					}
					break;
			}
		}
	}
	
	private static int getMoveDelay(int[] array)
	{
		return (int) ((Util.calculateDistance(array[1], array[2], array[4], array[5]) / 50) * 1000) + 200;
	}

	public static void main(String[] args)
	{
		new SecretRitualOfThePriests();
	}
}