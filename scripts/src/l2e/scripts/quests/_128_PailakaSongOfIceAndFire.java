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
package l2e.scripts.quests;

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 12.12.2020
 */
public class _128_PailakaSongOfIceAndFire extends Quest
{
	private static final int[][] DROPLIST =
	{
	        {
	                18616, 13032, 30
			},
			{
			        18616, 13033, 80
			},
			{
			        32492, 13032, 10
			},
			{
			        32492, 13041, 40
			},
			{
			        32492, 13033, 80
			},
			{
			        32493, 13032, 10
			},
			{
			        32493, 13040, 40
			},
			{
			        32493, 13033, 80
			}
	};
	
	public _128_PailakaSongOfIceAndFire(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32497);
		addFirstTalkId(32497, 32510, 32500, 32507);
		addTalkId(32497, 32510, 32500, 32507);
		
		addAttackId(32492, 32493);
		addSeeCreatureId(18607);
		
		addKillId(18610, 18609, 18608, 18607, 18620, 18616, 32492, 32493, 18611, 18612, 18613, 18614, 18615);
		
		questItemIds = new int[]
		{
		        13034, 13035, 13036, 13130, 13131, 13132, 13133, 13134, 13135, 13136, 13038, 13039, 13032, 13033, 13040, 13041
		};
	}
	
	private static final void dropItem(Npc mob, Player player)
	{
		final int npcId = mob.getId();
		final int chance = getRandom(100);
		for (final int[] drop : DROPLIST)
		{
			if (npcId == drop[0])
			{
				if (chance < drop[2])
				{
					((MonsterInstance) mob).dropSingleItem(player, drop[1], getRandom(1, 6));
					return;
				}
			}
			if (npcId < drop[0])
			{
				return;
			}
		}
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return getNoQuestMsg(player);
		}
		
		final int cond = st.getCond();
		if (event.equalsIgnoreCase("32497-03.htm"))
		{
			if (cond == 0)
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32500-06.htm"))
		{
			if (cond == 1)
			{
				st.setCond(2, true);
				st.giveItems(13034, 1);
				st.giveItems(13130, 1);
			}
		}
		else if (event.equalsIgnoreCase("32507-04.htm"))
		{
			if (cond == 3)
			{
				st.setCond(4, true);
				st.takeItems(13034, -1);
				st.takeItems(13038, -1);
				st.takeItems(13131, -1);
				st.giveItems(13132, 1);
				st.giveItems(13035, 1);
				addSpawn(18609, -53903, 181484, -4555, 30456, false, 0, false, npc.getReflection());
			}
		}
		else if (event.equalsIgnoreCase("32507-08.htm"))
		{
			if (cond == 6)
			{
				st.setCond(7, true);
				st.takeItems(13035, -1);
				st.takeItems(13134, -1);
				st.takeItems(13039, -1);
				st.giveItems(13036, 1);
				st.giveItems(13135, 1);
				addSpawn(18607, -61354, 183624, -4821, 63613, false, 0, false, npc.getReflection());
			}
		}
		else if (event.equalsIgnoreCase("32510-02.htm"))
		{
			if (cond == 9)
			{
				st.exitQuest(false, true);
				final Reflection inst = ReflectionManager.getInstance().getReflection(npc.getReflectionId());
				if (inst != null)
				{
					inst.setDuration(300000);
					inst.setEmptyDestroyTime(0);
					
					if (inst.containsPlayer(player.getObjectId()))
					{
						player.setVitalityPoints(20000, true);
						st.calcExpAndSp(getId());
						st.calcReward(getId());
					}
				}
			}
		}
		else if (event.equalsIgnoreCase("GARGOS_LAUGH"))
		{
			npc.broadcastPacketToOthers(new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getTemplate().getIdTemplate(), NpcStringId.OHHOHOH));
		}
		return event;
	}
	
	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		final int cond = st.getCond();
		switch (npc.getId())
		{
			case 32497 :
				switch (st.getState())
				{
					case State.CREATED :
						if (player.getLevel() < getMinLvl(getId()))
						{
							return "32497-05.htm";
						}
						if (player.getLevel() > getMaxLvl(getId()))
						{
							return "32497-06.htm";
						}
						return "32497-01.htm";
					case State.STARTED :
						if (cond > 1)
						{
							return "32497-00.htm";
						}
						
						return "32497-03.htm";
					case State.COMPLETED :
						return "32497-07.htm";
					default :
						return "32497-01.htm";
				}
			case 32500 :
				if (cond > 1)
				{
					return "32500-00.htm";
				}
				return "32500-01.htm";
			case 32507 :
				switch (st.getCond())
				{
					case 1 :
						return "32507-01.htm";
					case 2 :
						return "32507-02.htm";
					case 3 :
						return "32507-03.htm";
					case 4 :
					case 5 :
						return "32507-05.htm";
					case 6 :
						return "32507-06.htm";
					default :
						return "32507-09.htm";
				}
			case 32510 :
				if (st.getState() == State.COMPLETED)
				{
					return "32510-00.htm";
				}
				else if (cond == 9)
				{
					return "32510-01.htm";
				}
		}
		return getNoQuestMsg(player);
	}
	
	@Override
	public final String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (!npc.isDead())
		{
			npc.doDie(attacker);
		}
		
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if ((st == null) || (st.getState() != State.STARTED))
		{
			return null;
		}
		
		final int cond = st.getCond();
		switch (npc.getId())
		{
			case 18610 :
				if (cond == 2)
				{
					st.set("cond", "3");
					st.playSound("ItemSound.quest_itemget");
					st.takeItems(13130, -1);
					st.giveItems(13131, 1);
					st.giveItems(13038, 1);
				}
				break;
			case 18609 :
				if (cond == 4)
				{
					st.takeItems(13132, -1);
					st.giveItems(13133, 1);
					st.set("cond", "5");
					st.playSound("ItemSound.quest_itemget");
				}
				addSpawn(18608, -61415, 181418, -4818, 63852, false, 0, false, npc.getReflection());
				break;
			case 18608 :
				if (cond == 5)
				{
					st.set("cond", "6");
					st.playSound("ItemSound.quest_itemget");
					st.takeItems(13133, -1);
					st.giveItems(13134, 1);
					st.giveItems(13039, 1);
				}
				break;
			case 18607 :
				if (cond == 7)
				{
					st.set("cond", "8");
					st.playSound("ItemSound.quest_itemget");
					st.takeItems(13135, -1);
					st.giveItems(13136, 1);
				}
				addSpawn(18620, -53297, 185027, -4617, 1512, false, 0, false, npc.getReflection());
				break;
			case 18620 :
				if (cond == 8)
				{
					st.set("cond", "9");
					st.playSound("ItemSound.quest_middle");
					st.takeItems(13136, -1);
					addSpawn(32510, -53297, 185027, -4617, 33486, false, 0, false, npc.getReflection());
				}
				break;
			case 32492 :
			case 32493 :
			case 18616 :
				dropItem(npc, player);
				break;
			default :
				break;
		}
		return super.onKill(npc, player, isSummon);
	}
	
	@Override
	public String onSeeCreature(Npc npc, Creature creature, boolean isSummon)
	{
		if (npc.isScriptValue(0) && creature.isPlayer())
		{
			npc.setScriptValue(1);
			startQuestTimer("GARGOS_LAUGH", 1000, npc, creature.getActingPlayer());
		}
		return super.onSeeCreature(npc, creature, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _128_PailakaSongOfIceAndFire(128, _128_PailakaSongOfIceAndFire.class.getSimpleName(), "");
	}
}