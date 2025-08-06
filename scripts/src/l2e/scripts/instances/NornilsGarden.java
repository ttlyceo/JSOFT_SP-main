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

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Util;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneType;

/**
 * Rework by LordWinter 02.10.2020
 */
public class NornilsGarden extends AbstractReflection
{
	private class NornilsWorld extends ReflectionWorld
	{
		public Npc first_npc = null;
		public boolean spawned_1 = false;
		public boolean spawned_2 = false;
		public boolean spawned_3 = false;
		public boolean spawned_4 = false;
		
		public NornilsWorld()
		{
		}
	}
	
	private static final int[] _final_gates =
	{
	        32260, 32261, 32262
	};
	
	private static final int[][] _auto_gates =
	{
	        {
	                20110, 16200001
			},
			{
			        20111, 16200004
			},
			{
			        20112, 16200013
			}
	};
	
	private static final Skill skill1 = SkillsParser.getInstance().getInfo(4322, 1);
	private static final Skill skill2 = SkillsParser.getInstance().getInfo(4327, 1);
	private static final Skill skill3 = SkillsParser.getInstance().getInfo(4329, 1);
	private static final Skill skill4 = SkillsParser.getInstance().getInfo(4324, 1);
	
	private static final int[][] _gatekeepers =
	{
	        {
	                18352, 9703, 0
			},
			{
			        18353, 9704, 0
			},
			{
			        18354, 9705, 0
			},
			{
			        18355, 9706, 0
			},
			{
			        18356, 9707, 16200024
			},
			{
			        18357, 9708, 16200025
			},
			{
			        18358, 9713, 0
			},
			{
			        18359, 9709, 16200023
			},
			{
			        18360, 9710, 0
			},
			{
			        18361, 9711, 0
			},
			{
			        25528, 9712, 0
			}
	};
	
	private static final int[][] _group_1 =
	{
	        {
	                18363, -109899, 74431, -12528, 16488
			},
			{
			        18483, -109701, 74501, -12528, 24576
			},
			{
			        18483, -109892, 74886, -12528, 0
			},
			{
			        18363, -109703, 74879, -12528, 49336
			}
			
	};
	
	private static final int[][] _group_2 =
	{
	        {
	                18363, -110393, 78276, -12848, 49152
			},
			{
			        18363, -110561, 78276, -12848, 49152
			},
			{
			        18362, -110414, 78495, -12905, 48112
			},
			{
			        18362, -110545, 78489, -12903, 48939
			},
			{
			        18483, -110474, 78601, -12915, 49488
			},
			{
			        18362, -110474, 78884, -12915, 49338
			},
			{
			        18483, -110389, 79131, -12915, 48539
			},
			{
			        18483, -110551, 79134, -12915, 49151
			}
	};
	
	private static final int[][] _group_3 =
	{
	        {
	                18483, -107798, 80721, -12912, 0
			},
			{
			        18483, -107798, 80546, -12912, 0
			},
			{
			        18347, -108033, 80644, -12912, 0
			},
			{
			        18363, -108520, 80647, -12912, 0
			},
			{
			        18483, -108740, 80752, -12912, 0
			},
			{
			        18363, -109016, 80642, -12912, 0
			},
			{
			        18483, -108740, 80546, -12912, 0
			}
	};
	
	private static final int[][] _group_4 =
	{
	        {
	                18362, -110082, 83998, -12928, 0
			},
			{
			        18362, -110082, 84210, -12928, 0
			},
			{
			        18363, -109963, 84102, -12896, 0
			},
			{
			        18347, -109322, 84102, -12880, 0
			},
			{
			        18362, -109131, 84097, -12880, 0
			},
			{
			        18483, -108932, 84101, -12880, 0
			},
			{
			        18483, -109313, 84488, -12880, 0
			},
			{
			        18362, -109122, 84490, -12880, 0
			},
			{
			        18347, -108939, 84489, -12880, 0
			}
	};
	
	public NornilsGarden(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(32330);
		addFirstTalkId(32330);
		addTalkId(32330);
		
		for (final int i[] : _gatekeepers)
		{
			addKillId(i[0]);
		}
		
		for (final int i[] : _auto_gates)
		{
			addEnterZoneId(i[0]);
		}
		
		addTalkId(_final_gates);
		addAttackId(18362);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new NornilsWorld(), 11))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			prepareInstance((NornilsWorld) world);
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
		
		if (skill1 != null)
		{
			skill1.getEffects(player, player, false);
		}
		if (skill2 != null)
		{
			skill2.getEffects(player, player, false);
		}
		if (skill3 != null)
		{
			skill3.getEffects(player, player, false);
		}
		if (skill4 != null)
		{
			skill4.getEffects(player, player, false);
		}
	}
	
	private void prepareInstance(NornilsWorld world)
	{
		world.first_npc = addSpawn(18362, -109702, 74696, -12528, 49568, false, 0, false, world.getReflection());
		final DoorInstance door = ReflectionManager.getInstance().getReflection(world.getReflectionId()).getDoor(16200010);
		if (door != null)
		{
			door.setTargetable(false);
			door.setMeshIndex(2);
		}
	}
	
	private void spawn1(Npc npc)
	{
		final ReflectionWorld inst = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (inst instanceof NornilsWorld)
		{
			final NornilsWorld world = ((NornilsWorld) inst);
			if (npc.equals(world.first_npc) && !world.spawned_1)
			{
				world.spawned_1 = true;
				
				for (final int mob[] : _group_1)
				{
					addSpawn(mob[0], mob[1], mob[2], mob[3], mob[4], false, 0, false, world.getReflection());
				}
			}
		}
	}
	
	private void spawn2(Npc npc)
	{
		final ReflectionWorld inst = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (inst instanceof NornilsWorld)
		{
			final NornilsWorld world = ((NornilsWorld) inst);
			if (!world.spawned_2)
			{
				world.spawned_2 = true;
				
				for (final int mob[] : _group_2)
				{
					addSpawn(mob[0], mob[1], mob[2], mob[3], mob[4], false, 0, false, world.getReflection());
				}
			}
		}
	}
	
	private void spawn3(Creature cha)
	{
		final ReflectionWorld inst = ReflectionManager.getInstance().getWorld(cha.getReflectionId());
		if (inst instanceof NornilsWorld)
		{
			final NornilsWorld world = ((NornilsWorld) inst);
			if (!world.spawned_3)
			{
				world.spawned_3 = true;
				
				for (final int mob[] : _group_3)
				{
					addSpawn(mob[0], mob[1], mob[2], mob[3], mob[4], false, 0, false, world.getReflection());
				}
			}
		}
	}
	
	private void spawn4(Creature cha)
	{
		final ReflectionWorld inst = ReflectionManager.getInstance().getWorld(cha.getReflectionId());
		if (inst instanceof NornilsWorld)
		{
			final NornilsWorld world = ((NornilsWorld) inst);
			if (!world.spawned_4)
			{
				world.spawned_4 = true;
				
				for (final int mob[] : _group_4)
				{
					addSpawn(mob[0], mob[1], mob[2], mob[3], mob[4], false, 0, false, world.getReflection());
				}
			}
		}
	}
	
	public void openDoor(QuestState st, Player player, int doorId)
	{
		st.unset("correct");
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(player.getReflectionId());
		if (tmpworld instanceof NornilsWorld)
		{
			tmpworld.getReflection().openDoor(doorId);
		}
	}
	
	@Override
	public String onEnterZone(Creature character, ZoneType zone)
	{
		if ((character.isPlayer()) && !character.isDead() && !character.isTeleporting() && ((Player) character).isOnline())
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(character.getReflectionId());
			if (tmpworld instanceof NornilsWorld)
			{
				for (final int _auto[] : _auto_gates)
				{
					if (zone.getId() == _auto[0])
					{
						tmpworld.getReflection().openDoor(_auto[1]);
					}
					if (zone.getId() == 20111)
					{
						spawn3(character);
					}
					else if (zone.getId() == 20112)
					{
						spawn4(character);
					}
				}
			}
		}
		return super.onEnterZone(character, zone);
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		if ((npc.getId() == 32330) && event.equalsIgnoreCase("enter_instance"))
		{
			enterInstance(player, npc);
		}
		else if ((npc.getId() == 32258) && event.equalsIgnoreCase("exit"))
		{
			final ReflectionWorld inst = ReflectionManager.getInstance().getWorld(player.getReflectionId());
			if (inst instanceof NornilsWorld)
			{
				final NornilsWorld world = ((NornilsWorld) inst);
				world.removeAllowed(player.getObjectId());
				teleportPlayer(player, new Location(-74058, 52040, -3680), ReflectionManager.DEFAULT);
			}
		}
		else if (ArrayUtils.contains(_final_gates, npc.getId()))
		{
			if (event.equalsIgnoreCase("32260-02.html") || event.equalsIgnoreCase("32261-02.html") || event.equalsIgnoreCase("32262-02.html"))
			{
				st.unset("correct");
			}
			else if (Util.isDigit(event))
			{
				int correct = st.getInt("correct");
				correct++;
				st.set("correct", String.valueOf(correct));
				htmltext = npc.getId() + "-0" + String.valueOf(correct + 2) + ".html";
			}
			else if (event.equalsIgnoreCase("check"))
			{
				final int correct = st.getInt("correct");
				if ((npc.getId() == 32260) && (correct == 3))
				{
					openDoor(st, player, 16200014);
				}
				else if ((npc.getId() == 32261) && (correct == 3))
				{
					openDoor(st, player, 16200015);
				}
				else if ((npc.getId() == 32262) && (correct == 4))
				{
					openDoor(st, player, 16200016);
				}
				else
				{
					return npc.getId() + "-00.html";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		if (ArrayUtils.contains(_final_gates, npc.getId()))
		{
			final QuestState cst = player.getQuestState("_179_IntoTheLargeCavern");
			if ((cst != null) && (cst.getState() == State.STARTED))
			{
				return npc.getId() + "-01.html";
			}
			return getNoQuestMsg(player);
		}
		
		return null;
	}
	
	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		return npc.getId() + ".html";
	}
	
	@Override
	public final String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if ((npc.getId() == 18362) && (npc.getReflectionId() > 0))
		{
			spawn1(npc);
		}
		return null;
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		for (final int _gk[] : _gatekeepers)
		{
			if (npc.getId() == _gk[0])
			{
				((MonsterInstance) npc).dropSingleItem(player, _gk[1], 1);
				if (_gk[2] > 0)
				{
					final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(player.getReflectionId());
					if (tmpworld instanceof NornilsWorld)
					{
						tmpworld.getReflection().openDoor(_gk[2]);
					}
				}
			}
			if (npc.getId() == 18355)
			{
				spawn2(npc);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new NornilsGarden(NornilsGarden.class.getSimpleName(), "instances");
	}
}
