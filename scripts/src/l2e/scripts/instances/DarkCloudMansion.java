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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 02.10.2020
 */
public class DarkCloudMansion extends AbstractReflection
{
	protected class DMCWorld extends ReflectionWorld
	{
		public Map<String, DMCRoom> rooms = new ConcurrentHashMap<>();
	}
	
	protected static class DMCNpc
	{
		public Npc npc;
		public boolean isDead = false;
		public Npc golem = null;
		public int status = 0;
		public int order = 0;
		public int count = 0;
	}
	
	protected static class DMCRoom
	{
		public List<DMCNpc> npcList = new ArrayList<>();
		public boolean isInCalc = false;
		public boolean isEnd = false;
		public int counter = 0;
		public int reset = 0;
		public int founded = 0;
		public int[] Order;
	}
	
	private static int[] BM =
	{
	        22272, 22273, 22274
	};
	
	private static int[] BS =
	{
	        18371, 18372, 18373, 18374, 18375, 18376, 18377
	};
	
	private static NpcStringId[] _spawnChat =
	{
	        NpcStringId.IM_THE_REAL_ONE, NpcStringId.PICK_ME, NpcStringId.TRUST_ME, NpcStringId.NOT_THAT_DUDE_IM_THE_REAL_ONE, NpcStringId.DONT_BE_FOOLED_DONT_BE_FOOLED_IM_THE_REAL_ONE
	};
	
	private static NpcStringId[] _decayChat =
	{
	        NpcStringId.IM_THE_REAL_ONE_PHEW, NpcStringId.CANT_YOU_EVEN_FIND_OUT, NpcStringId.FIND_ME
	};
	
	private static NpcStringId[] _successChat =
	{
	        NpcStringId.HUH_HOW_DID_YOU_KNOW_IT_WAS_ME, NpcStringId.EXCELLENT_CHOICE_TEEHEE, NpcStringId.YOUVE_DONE_WELL, NpcStringId.OH_VERY_SENSIBLE
	};
	
	private static NpcStringId[] _faildChat =
	{
	        NpcStringId.YOUVE_BEEN_FOOLED, NpcStringId.SORRY_BUT_IM_THE_FAKE_ONE
	};
	
	private static int[][] MonolithOrder = new int[][]
	{
	        {
	                1, 2, 3, 4, 5, 6
			},
	        {
	                6, 5, 4, 3, 2, 1
			},
	        {
	                4, 5, 6, 3, 2, 1
			},
	        {
	                2, 6, 3, 5, 1, 4
			},
	        {
	                4, 1, 5, 6, 2, 3
			},
	        {
	                3, 5, 1, 6, 2, 4
			},
	        {
	                6, 1, 3, 4, 5, 2
			},
	        {
	                5, 6, 1, 2, 4, 3
			},
	        {
	                5, 2, 6, 3, 4, 1
			},
	        {
	                1, 5, 2, 6, 3, 4
			},
	        {
	                1, 2, 3, 6, 5, 4
			},
	        {
	                6, 4, 3, 1, 5, 2
			},
	        {
	                3, 5, 2, 4, 1, 6
			},
	        {
	                3, 2, 4, 5, 1, 6
			},
	        {
	                5, 4, 3, 1, 6, 2
			},
	};
	
	private static int[][] GolemSpawn = new int[][]
	{
	        {
	                18369, 148060, 181389
			},
	        {
	                18370, 147910, 181173
			},
	        {
	                18369, 147810, 181334
			},
	        {
	                18370, 147713, 181179
			},
	        {
	                18369, 147569, 181410
			},
	        {
	                18370, 147810, 181517
			},
	        {
	                18369, 147805, 181281
			}
	};
	
	private static int[][] ColumnRows = new int[][]
	{
	        {
	                1, 1, 0, 1, 0
			},
	        {
	                0, 1, 1, 0, 1
			},
	        {
	                1, 0, 1, 1, 0
			},
	        {
	                0, 1, 0, 1, 1
			},
	        {
	                1, 0, 1, 0, 1
			}
	};
	
	private static int[][] Beleths = new int[][]
	{
	        {
	                1, 0, 1, 0, 1, 0, 0
			},
	        {
	                0, 0, 1, 0, 1, 1, 0
			},
	        {
	                0, 0, 0, 1, 0, 1, 1
			},
	        {
	                1, 0, 1, 1, 0, 0, 0
			},
	        {
	                1, 1, 0, 0, 0, 1, 0
			},
	        {
	                0, 1, 0, 1, 0, 1, 0
			},
	        {
	                0, 0, 0, 1, 1, 1, 0
			},
	        {
	                1, 0, 1, 0, 0, 1, 0
			},
	        {
	                0, 1, 1, 0, 0, 0, 1
			}
	};
	
	public DarkCloudMansion(String name, String descr)
	{
		super(name, descr);
		
		addFirstTalkId(32291, 32324);
		addStartNpc(32282);
		addTalkId(32282, 32291);
		addAttackId(18369, 18370, 18371, 18372, 18373, 18374, 18375, 18376, 18377, 22402);
		addKillId(18371, 18372, 18373, 18374, 18375, 18376, 18377, 22318, 22319, 22272, 22273, 22274, 18369, 18370, 22402, 22264);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new DMCWorld(), 9))
		{
			final var world = ReflectionManager.getInstance().getPlayerWorld(player);
			runStartRoom((DMCWorld) world);
		}
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			player.getAI().setIntention(CtrlIntention.IDLE);
			final var teleLoc = template.getTeleportCoord();
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
			final var teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
	}
	
	protected void runStartRoom(DMCWorld world)
	{
		world.setStatus(0);
		final var StartRoom = new DMCRoom();
		DMCNpc thisnpc;
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22272, 146817, 180335, -6117, 0, false, 0, false, world.getReflection());
		StartRoom.npcList.add(thisnpc);
		thisnpc.npc.setIsNoRndWalk(true);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22272, 146741, 180589, -6117, 0, false, 0, false, world.getReflection());
		StartRoom.npcList.add(thisnpc);
		thisnpc.npc.setIsNoRndWalk(true);
		world.rooms.put("StartRoom", StartRoom);
	}
	
	protected void spawnHall(DMCWorld world)
	{
		final var Hall = new DMCRoom();
		DMCNpc thisnpc;
		world.rooms.remove("Hall");
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22273, 147217, 180112, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		Hall.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22274, 147217, 180209, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		Hall.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22273, 148521, 180112, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		Hall.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22272, 148521, 180209, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		Hall.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22273, 148525, 180910, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		Hall.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22274, 148435, 180910, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		Hall.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22273, 147242, 180910, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		Hall.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22274, 147242, 180819, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		Hall.npcList.add(thisnpc);
		
		world.rooms.put("Hall", Hall);
	}
	
	protected void runHall(DMCWorld world)
	{
		spawnHall(world);
		world.setStatus(1);
		world.getReflection().openDoor(24230001);
	}
	
	protected void runFirstRoom(DMCWorld world)
	{
		final var FirstRoom = new DMCRoom();
		DMCNpc thisnpc;
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22264, 147842, 179837, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		FirstRoom.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22264, 147711, 179708, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		FirstRoom.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22264, 147842, 179552, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		FirstRoom.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(22264, 147964, 179708, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		FirstRoom.npcList.add(thisnpc);
		
		world.rooms.put("FirstRoom", FirstRoom);
		world.setStatus(2);
		world.getReflection().openDoor(24230002);
	}
	
	protected void runHall2(DMCWorld world)
	{
		addSpawn(32288, 147818, 179643, -6117, 0, false, 0, false, world.getReflection());
		spawnHall(world);
		world.setStatus(3);
	}
	
	protected void runSecondRoom(DMCWorld world)
	{
		final var SecondRoom = new DMCRoom();
		DMCNpc thisnpc;
		
		SecondRoom.Order = new int[7];
		SecondRoom.Order[0] = 1;
		for (int i = 1; i < 7; i++)
		{
			SecondRoom.Order[i] = 0;
		}
		
		final int i = getRandom(MonolithOrder.length);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(32324, 147800, 181150, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.order = MonolithOrder[i][0];
		SecondRoom.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(32324, 147900, 181215, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.order = MonolithOrder[i][1];
		SecondRoom.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(32324, 147900, 181345, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.order = MonolithOrder[i][2];
		SecondRoom.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(32324, 147800, 181410, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.order = MonolithOrder[i][3];
		SecondRoom.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(32324, 147700, 181345, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.order = MonolithOrder[i][4];
		SecondRoom.npcList.add(thisnpc);
		
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(32324, 147700, 181215, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.order = MonolithOrder[i][5];
		SecondRoom.npcList.add(thisnpc);
		
		world.rooms.put("SecondRoom", SecondRoom);
		world.setStatus(4);
		world.getReflection().openDoor(24230005);
	}
	
	protected void runHall3(DMCWorld world)
	{
		addSpawn(32289, 147808, 181281, -6117, 16383, false, 0, false, world.getReflection());
		spawnHall(world);
		world.setStatus(5);
	}
	
	protected void runThirdRoom(DMCWorld world)
	{
		final var ThirdRoom = new DMCRoom();
		final var thisnpc = new DMCNpc();
		thisnpc.isDead = false;
		thisnpc.npc = addSpawn(22273, 148765, 180450, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22274, 148865, 180190, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22273, 148995, 180190, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22272, 149090, 180450, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22273, 148995, 180705, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22274, 148865, 180705, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		world.rooms.put("ThirdRoom", ThirdRoom);
		world.setStatus(6);
		world.getReflection().openDoor(24230003);
	}
	
	protected void runThirdRoom2(DMCWorld world)
	{
		addSpawn(32290, 148910, 178397, -6117, 16383, false, 0, false, world.getReflection());
		final var ThirdRoom = new DMCRoom();
		final var thisnpc = new DMCNpc();
		thisnpc.isDead = false;
		thisnpc.npc = addSpawn(22273, 148765, 180450, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22274, 148865, 180190, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22273, 148995, 180190, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22272, 149090, 180450, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22273, 148995, 180705, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		thisnpc.npc = addSpawn(22274, 148865, 180705, -6117, 0, false, 0, false, world.getReflection());
		thisnpc.npc.setIsNoRndWalk(true);
		ThirdRoom.npcList.add(thisnpc);
		world.rooms.put("ThirdRoom2", ThirdRoom);
		world.setStatus(8);
	}
	
	protected void runForthRoom(DMCWorld world)
	{
		final DMCRoom ForthRoom = new DMCRoom();
		ForthRoom.counter = 0;
		DMCNpc thisnpc;
		final int temp[] = new int[7];
		final int templist[][] = new int[7][5];
		int xx = 0;
		
		for (int i = 0; i < 7; i++)
		{
			temp[i] = getRandom(ColumnRows.length);
		}
		
		for (int i = 0; i < 7; i++)
		{
			templist[i] = ColumnRows[temp[i]];
		}
		
		for (int x = 148660; x < 149285; x += 125)
		{
			int yy = 0;
			for (int y = 179280; y > 178405; y -= 125)
			{
				thisnpc = new DMCNpc();
				thisnpc.npc = addSpawn(22402, x, y, -6115, 16215, false, 0, false, world.getReflection());
				thisnpc.status = templist[yy][xx];
				thisnpc.order = yy;
				ForthRoom.npcList.add(thisnpc);
				yy++;
			}
			xx++;
		}
		
		for (final var npc : ForthRoom.npcList)
		{
			if (npc.status == 0)
			{
				npc.npc.setIsInvul(true);
			}
		}
		
		world.rooms.put("ForthRoom", ForthRoom);
		world.setStatus(7);
		world.getReflection().openDoor(24230004);
	}
	
	protected void runFifthRoom(DMCWorld world)
	{
		spawnFifthRoom(world);
		world.setStatus(9);
		world.getReflection().openDoor(24230006);
	}
	
	private void spawnFifthRoom(DMCWorld world)
	{
		int idx = 0;
		int temp[] = new int[6];
		final var FifthRoom = new DMCRoom();
		DMCNpc thisnpc;
		
		temp = Beleths[getRandom(Beleths.length)];
		
		FifthRoom.reset = 0;
		FifthRoom.founded = 0;
		FifthRoom.isInCalc = false;
		
		for (int x = 148720; x < 149175; x += 65)
		{
			thisnpc = new DMCNpc();
			thisnpc.npc = addSpawn(BS[idx], x, 182145, -6117, 48810, false, 0, false, world.getReflection());
			thisnpc.npc.setIsNoRndWalk(true);
			thisnpc.order = idx;
			thisnpc.status = temp[idx];
			thisnpc.count = 0;
			FifthRoom.npcList.add(thisnpc);
			if (temp[idx] == 1 && getRandom(100) < 95)
			{
				thisnpc.npc.broadcastPacketToOthers(2000, new NpcSay(thisnpc.npc.getObjectId(), 0, thisnpc.npc.getId(), _spawnChat[getRandom(_spawnChat.length)]));
			}
			else if (temp[idx] != 1 && getRandom(100) < 67)
			{
				thisnpc.npc.broadcastPacketToOthers(2000, new NpcSay(thisnpc.npc.getObjectId(), 0, thisnpc.npc.getId(), _spawnChat[getRandom(_spawnChat.length)]));
			}
			idx++;
		}
		world.rooms.put("FifthRoom", FifthRoom);
	}
	
	protected boolean checkKillProgress(Npc npc, DMCRoom room)
	{
		boolean cont = true;
		for (final var npcobj : room.npcList)
		{
			if (npcobj.npc == npc)
			{
				npcobj.isDead = true;
			}
			if (npcobj.isDead == false)
			{
				cont = false;
			}
		}
		return cont;
	}
	
	protected void spawnRndGolem(DMCWorld world, DMCNpc npc)
	{
		if (npc.golem != null)
		{
			return;
		}
		
		final int i = getRandom(GolemSpawn.length);
		final int mobId = GolemSpawn[i][0];
		final int x = GolemSpawn[i][1];
		final int y = GolemSpawn[i][2];
		
		npc.golem = addSpawn(mobId, x, y, -6117, 0, false, 0, false, world.getReflection());
		npc.golem.setIsNoRndWalk(true);
	}
	
	protected void checkStone(Npc npc, int order[], DMCNpc npcObj, DMCWorld world)
	{
		for (int i = 1; i < 7; i++)
		{
			if (order[i] == 0 && order[i - 1] != 0)
			{
				if (npcObj.order == i && npcObj.status == 0)
				{
					order[i] = 1;
					npcObj.status = 1;
					npcObj.isDead = true;
					npc.broadcastPacketToOthers(new MagicSkillUse(npc, npc, 5441, 1, 1, 0));
					return;
				}
			}
		}
		spawnRndGolem(world, npcObj);
	}
	
	protected void endInstance(DMCWorld world)
	{
		final var FifthRoom = world.rooms.get("FifthRoom");
		for (final var mob : FifthRoom.npcList)
		{
			mob.npc.decayMe();
		}
		world.setStatus(10);
		addSpawn(32291, 148911, 181940, -6117, 16383, false, 0, false, world.getReflection());
		world.rooms.clear();
	}
	
	protected void checkBelethSample(DMCWorld world, Npc npc, Player player)
	{
		final var FifthRoom = world.rooms.get("FifthRoom");
		if (FifthRoom.isInCalc && FifthRoom.reset == 0)
		{
			if (npc.isAttackable())
			{
				npc.setTarget(null);
				npc.stopMove(null);
				((Attackable) npc).clearAggroList(false);
				npc.getAttackByList().clear();
			}
			return;
		}
		
		if (FifthRoom.reset == 1)
		{
			return;
		}
		
		FifthRoom.isInCalc = true;
		
		for (final var mob : FifthRoom.npcList)
		{
			if (mob.npc == npc)
			{
				if (mob.count == 0)
				{
					mob.count = 1;
					if (mob.status == 1)
					{
						mob.npc.broadcastPacketToOthers(2000, new NpcSay(mob.npc.getObjectId(), Say2.NPC_ALL, mob.npc.getId(), _successChat[getRandom(_successChat.length)]));
						FifthRoom.founded += 1;
						mob.count = 2;
						mob.npc.decayMe();
						if (FifthRoom.founded == 3)
						{
							if (!FifthRoom.isEnd)
							{
								FifthRoom.isEnd = true;
								endInstance(world);
							}
						}
					}
					else
					{
						FifthRoom.reset = 1;
						mob.npc.broadcastPacketToOthers(2000, new NpcSay(mob.npc.getObjectId(), Say2.NPC_ALL, mob.npc.getId(), _faildChat[getRandom(_faildChat.length)]));
						startQuestTimer("decayChatBelethSamples", 100, npc, player);
						startQuestTimer("decayBelethSamples", 1000, npc, player);
					}
				}
			}
		}
		
		if (FifthRoom.reset < 1)
		{
			FifthRoom.isInCalc = false;
		}
	}
	
	protected void killedBelethSample(DMCWorld world, Npc npc)
	{
		final var FifthRoom = world.rooms.get("FifthRoom");
		if (FifthRoom.reset == 1)
		{
			npc.decayMe();
			spawnFifthRoom(world);
		}
		else
		{
			if (FifthRoom.reset == 0 && FifthRoom.founded == 3)
			{
				for (final var mob : FifthRoom.npcList)
				{
					mob.npc.decayMe();
				}
				
				if (!FifthRoom.isEnd)
				{
					FifthRoom.isEnd = true;
					endInstance(world);
				}
			}
		}
	}
	
	protected boolean allStonesDone(DMCWorld world)
	{
		final var SecondRoom = world.rooms.get("SecondRoom");
		for (final var mob : SecondRoom.npcList)
		{
			if (mob.isDead)
			{
				continue;
			}
			return false;
		}
		return true;
	}
	
	protected void removeMonoliths(DMCWorld world)
	{
		final var SecondRoom = world.rooms.get("SecondRoom");
		for (final var mob : SecondRoom.npcList)
		{
			mob.npc.decayMe();
		}
	}
	
	protected void chkShadowColumn(DMCWorld world, Npc npc)
	{
		final var ForthRoom = world.rooms.get("ForthRoom");
		for (final var mob : ForthRoom.npcList)
		{
			if (mob.npc == npc)
			{
				for (int i = 0; i < 7; i++)
				{
					if (mob.order == i && ForthRoom.counter == i)
					{
						world.getReflection().openDoor(24230007 + i);
						ForthRoom.counter += 1;
						if (ForthRoom.counter == 7)
						{
							runThirdRoom2(world);
						}
					}
				}
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (npc == null)
		{
			return "";
		}
		
		final var tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof DMCWorld)
		{
			final var world = (DMCWorld) tmpworld;
			if (world.rooms.containsKey("FifthRoom"))
			{
				final DMCRoom FifthRoom = world.rooms.get("FifthRoom");
				if (event.equalsIgnoreCase("decayBelethSamples"))
				{
					for (final var mob : FifthRoom.npcList)
					{
						if (mob.count == 0)
						{
							mob.npc.decayMe();
							mob.count = 2;
						}
					}
				}
				else if (event.equalsIgnoreCase("decayChatBelethSamples"))
				{
					for (final var mob : FifthRoom.npcList)
					{
						if (mob.status == 1)
						{
							mob.npc.broadcastPacketToOthers(2000, new NpcSay(mob.npc.getObjectId(), Say2.NPC_ALL, mob.npc.getId(), _decayChat[getRandom(_decayChat.length)]));
						}
					}
				}
				else if (event.equalsIgnoreCase("respawnFifth"))
				{
					spawnFifthRoom(world);
				}
			}
		}
		return "";
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof DMCWorld)
		{
			final var world = (DMCWorld) tmpworld;
			if (world.isStatus(0))
			{
				if (checkKillProgress(npc, world.rooms.get("StartRoom")))
				{
					runHall(world);
				}
			}
			if (world.isStatus(1))
			{
				if (checkKillProgress(npc, world.rooms.get("Hall")))
				{
					runFirstRoom(world);
				}
			}
			if (world.isStatus(2))
			{
				if (checkKillProgress(npc, world.rooms.get("FirstRoom")))
				{
					runHall2(world);
				}
			}
			if (world.isStatus(3))
			{
				if (checkKillProgress(npc, world.rooms.get("Hall")))
				{
					runSecondRoom(world);
				}
			}
			if (world.isStatus(4))
			{
				final var SecondRoom = world.rooms.get("SecondRoom");
				for (final var mob : SecondRoom.npcList)
				{
					if (mob.golem == npc)
					{
						mob.golem = null;
					}
				}
			}
			if (world.isStatus(5))
			{
				if (checkKillProgress(npc, world.rooms.get("Hall")))
				{
					runThirdRoom(world);
				}
			}
			if (world.isStatus(6))
			{
				if (checkKillProgress(npc, world.rooms.get("ThirdRoom")))
				{
					runForthRoom(world);
				}
			}
			if (world.isStatus(7))
			{
				chkShadowColumn(world, npc);
			}
			if (world.isStatus(8))
			{
				if (checkKillProgress(npc, world.rooms.get("ThirdRoom2")))
				{
					runFifthRoom(world);
				}
			}
			if (world.isStatus(9))
			{
				killedBelethSample(world, npc);
			}
		}
		return "";
	}
	
	@Override
	public String onAttack(Npc npc, Player player, int damage, boolean isSummon, Skill skill)
	{
		final var tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof DMCWorld)
		{
			final var world = (DMCWorld) tmpworld;
			if (world.isStatus(7))
			{
				final var ForthRoom = world.rooms.get("ForthRoom");
				for (final var mob : ForthRoom.npcList)
				{
					if (mob.npc == npc)
					{
						if (mob.npc.isInvul() && getRandom(100) < 12)
						{
							addSpawn(BM[getRandom(BM.length)], player.getX(), player.getY(), player.getZ(), 0, false, 0, false, world.getReflection());
						}
					}
				}
			}
			if (world.isStatus(9))
			{
				checkBelethSample(world, npc, player);
			}
		}
		return "";
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final var tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof DMCWorld)
		{
			final var world = (DMCWorld) tmpworld;
			if (world.isStatus(4))
			{
				final var SecondRoom = world.rooms.get("SecondRoom");
				for (final var mob : SecondRoom.npcList)
				{
					if (mob.npc == npc)
					{
						checkStone(npc, SecondRoom.Order, mob, world);
					}
				}
				
				if (allStonesDone(world))
				{
					removeMonoliths(world);
					runHall3(world);
				}
			}
			
			if (npc.getId() == 32291 && world.isStatus(10))
			{
				npc.showChatWindow(player);
				QuestState st = player.getQuestState(getName());
				if (st == null)
				{
					st = newQuestState(player);
				}
				
				if (!st.hasQuestItems(9690))
				{
					st.giveItems(9690, 1);
				}
			}
		}
		return "";
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final int npcId = npc.getId();
		if (npcId == 32282)
		{
			enterInstance(player, npc);
		}
		else
		{
			final var tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if (tmpworld != null && tmpworld instanceof DMCWorld)
			{
				final var world = (DMCWorld) tmpworld;
				if (npcId == 32291)
				{
					if (world.isAllowed(player.getObjectId()))
					{
						world.removeAllowed(player.getObjectId());
					}
					teleportPlayer(player, new Location(139968, 150367, -3111), ReflectionManager.DEFAULT);
					final var instance = ReflectionManager.getInstance().getReflection(npc.getReflectionId());
					if (instance != null && instance.getPlayers().isEmpty())
					{
						instance.collapse();
					}
					return "";
				}
			}
		}
		return "";
	}
	
	public static void main(String[] args)
	{
		new DarkCloudMansion(DarkCloudMansion.class.getSimpleName(), "instances");
	}
}