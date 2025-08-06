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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 05.12.2019
 */
public class _022_TragedyInVonHellmannForest extends Quest
{
	private Npc _ghost = null;
	private Npc _soul = null;
	
	public _022_TragedyInVonHellmannForest(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31334, 31328);
		addTalkId(31328, 31334, 31528, 31529, 31527);
		
		addAttackId(27217);
		addKillId(27217, 21553, 21554, 21555, 21556, 21561);
		
		questItemIds = new int[]
		{
		        7142, 7147, 7146, 7143, 7145, 7144
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		final QuestState st2 = player.getQuestState("_021_HiddenTruth");
		
		if (event.equalsIgnoreCase("31334-03.htm"))
		{
			if (st2 != null && st2.isCompleted() && player.getLevel() >= getMinLvl(getId()))
			{
				htmltext = "31334-02.htm";
			}
		}
		else if (event.equalsIgnoreCase("31334-04.htm"))
		{
			if (st2 != null && st2.isCompleted() && player.getLevel() >= getMinLvl(getId()))
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31334-07.htm"))
		{
			if (!st.hasQuestItems(7141))
			{
				st.setCond(2, false);
			}
			else
			{
				htmltext = "31334-06.htm";
			}
		}
		else if (event.equalsIgnoreCase("31334-08.htm"))
		{
			if (st.hasQuestItems(7141))
			{
				st.setCond(4, true);
				st.takeItems(7141, 1);
			}
			else
			{
				st.setCond(2, false);
				htmltext = "31334-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("31334-13.htm"))
		{
			if (_ghost != null)
			{
				st.set("cond", "6");
				htmltext = "31334-14.htm";
			}
			else
			{
				st.setCond(7, true);
				st.takeItems(7142, 1);
				_ghost = addSpawn(31528, 38418, -49894, -1104, 0, false, 120000, true);
				_ghost.broadcastPacketToOthers(2000, new NpcSay(_ghost.getObjectId(), Say2.NPC_ALL, _ghost.getId(), NpcStringId.DID_YOU_CALL_ME_S1).addStringParameter(player.getName(null)));
				startQuestTimer("ghost_cleanup", 118000, null, player, false);
			}
		}
		else if (event.equalsIgnoreCase("31528-08.htm"))
		{
			st.setCond(8, true);
			cancelQuestTimer("ghost_cleanup", null, player);
			if (_ghost != null)
			{
				_ghost.deleteMe();
				_ghost = null;
			}
		}
		else if (event.equalsIgnoreCase("31328-10.htm"))
		{
			st.setCond(9, true);
			st.giveItems(7143, 1);
		}
		else if (event.equalsIgnoreCase("31529-12.htm"))
		{
			if (st.isCond(9))
			{
				st.setCond(10, true);
				st.takeItems(7143, 1);
				st.giveItems(7144, 1);
			}
		}
		else if (event.equalsIgnoreCase("31527-02.htm"))
		{
			if (_soul == null)
			{
				_soul = addSpawn(27217, 34860, -54542, -2048, 0, false, 0, true);
				((Attackable) _soul).addDamageHate(player, 0, 99999);
				_soul.getAI().setIntention(CtrlIntention.ATTACK, player, true);
			}
		}
		else if (event.equalsIgnoreCase("attack_timer"))
		{
			if (st.isCond(10))
			{
				st.setCond(11, true);
				st.takeItems(7144, 1);
				st.giveItems(7145, 1);
			}
		}
		else if (event.equalsIgnoreCase("31328-13.htm"))
		{
			if (st.isCond(14))
			{
				st.setCond(15, true);
				st.takeItems(7147, 1);
			}
		}
		else if (event.equalsIgnoreCase("31328-21.htm"))
		{
			if (st.isCond(15))
			{
				st.setCond(16, true);
			}
		}
		else if (event.equalsIgnoreCase("ghost_cleanup"))
		{
			_ghost.broadcastPacketToOthers(2000, new NpcSay(_ghost.getObjectId(), Say2.NPC_ALL, _ghost.getId(), NpcStringId.IM_CONFUSED_MAYBE_ITS_TIME_TO_GO_BACK));
			_ghost = null;
			return null;
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED :
				switch (npc.getId())
				{
					case 31328 :
						final QuestState st2 = player.getQuestState("_021_HiddenTruth");
						if (st2 != null && st2.isCompleted())
						{
							if (!st.hasQuestItems(7141))
							{
								htmltext = "31328-01.htm";
								st.giveItems(7141, 1);
								st.playSound("ItemSound.quest_itemget");
							}
							else
							{
								htmltext = "31328-01b.htm";
							}
						}
						break;
					case 31334 :
						htmltext = "31334-01.htm";
						break;
				}
				break;
			
			case State.STARTED :
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case 31334 :
						if (cond == 1 || cond == 2 || cond == 3)
						{
							htmltext = "31334-05.htm";
						}
						else if (cond == 4)
						{
							htmltext = "31334-09.htm";
						}
						else if (cond == 5 || cond == 6)
						{
							if (st.hasQuestItems(7142))
							{
								htmltext = (_ghost == null) ? "31334-10.htm" : "31334-11.htm";
							}
							else
							{
								htmltext = "31334-09.htm";
								st.set("cond", "4");
							}
						}
						else if (cond == 7)
						{
							htmltext = (_ghost != null) ? "31334-15.htm" : "31334-17.htm";
						}
						else if (cond > 7)
						{
							htmltext = "31334-18.htm";
						}
						break;
					case 31328 :
						if (cond < 3)
						{
							if (!st.hasQuestItems(7141))
							{
								htmltext = "31328-01.htm";
								st.setCond(3);
								st.playSound("ItemSound.quest_itemget");
								st.giveItems(7141, 1);
							}
							else
							{
								htmltext = "31328-01b.htm";
							}
						}
						else if (cond == 3)
						{
							htmltext = "31328-02.htm";
						}
						else if (cond == 8)
						{
							htmltext = "31328-03.htm";
						}
						else if (cond == 9)
						{
							htmltext = "31328-11.htm";
						}
						else if (cond == 14)
						{
							if (st.hasQuestItems(7147))
							{
								htmltext = "31328-12.htm";
							}
							else
							{
								st.set("cond", "13");
							}
						}
						else if (cond == 15)
						{
							htmltext = "31328-14.htm";
						}
						else if (cond == 16)
						{
							htmltext = player.getLevel() >= 64 ? "31328-22.htm" : "31328-23.htm";
							st.calcExpAndSp(getId());
							st.exitQuest(false, true);
						}
						break;
					case 31528 :
						if (cond == 7)
						{
							htmltext = "31528-01.htm";
						}
						else if (cond == 8)
						{
							htmltext = "31528-08.htm";
						}
						break;
					case 31529 :
						if (cond == 9)
						{
							if (st.hasQuestItems(7143))
							{
								htmltext = "31529-01.htm";
							}
							else
							{
								htmltext = "31529-10.htm";
								st.set("cond", "8");
							}
						}
						else if (cond == 10)
						{
							htmltext = "31529-16.htm";
						}
						else if (cond == 11)
						{
							if (st.hasQuestItems(7145))
							{
								htmltext = "31529-17.htm";
								st.setCond(12, true);
								st.takeItems(7145, 1);
							}
							else
							{
								htmltext = "31529-09.htm";
								st.set("cond", "10");
							}
						}
						else if (cond == 12)
						{
							htmltext = "31529-17.htm";
						}
						else if (cond == 13)
						{
							if (st.hasQuestItems(7146))
							{
								htmltext = "31529-18.htm";
								st.setCond(14, true);
								st.takeItems(7146, 1);
								st.giveItems(7147, 1);
							}
							else
							{
								htmltext = "31529-10.htm";
								st.set("cond", "12");
							}
						}
						else if (cond > 13)
						{
							htmltext = "31529-19.htm";
						}
						break;
					case 31527 :
						if (cond == 10)
						{
							htmltext = "31527-01.htm";
						}
						else if (cond == 11)
						{
							htmltext = "31527-03.htm";
						}
						else if (cond == 12)
						{
							htmltext = "31527-04.htm";
							st.setCond(13, true);
							st.giveItems(7146, 1);
						}
						else if (cond > 12)
						{
							htmltext = "31527-05.htm";
						}
						break;
				}
				break;
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
		}
		
		return htmltext;
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isPet, Skill skill)
	{
		final QuestState st = attacker.getQuestState(getName());
		if (st == null || !st.isStarted() || isPet)
		{
			return null;
		}
		
		if (getQuestTimer("attack_timer", null, attacker) != null)
		{
			return null;
		}
		
		if (st.getInt("cond") == 10)
		{
			startQuestTimer("attack_timer", 20000, null, attacker, false);
		}
		return null;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		if (npc.getId() != 27217)
		{
			if (st.getCond() == 4 && st.calcDropItems(getId(), 7142, npc.getId(), 1))
			{
				st.setCond(5);
			}
		}
		else
		{
			cancelQuestTimer("attack_timer", null, player);
			_soul = null;
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new _022_TragedyInVonHellmannForest(22, _022_TragedyInVonHellmannForest.class.getSimpleName(), "");
	}
}