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

import java.util.StringTokenizer;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.QuestState.QuestType;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 13.01.2020
 */
public class _455_WingsOfSand extends Quest
{
	public _455_WingsOfSand(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32864, 32865, 32866, 32867, 32868, 32869, 32870);
		addTalkId(32864, 32865, 32866, 32867, 32868, 32869, 32870);
		
		addKillId(25718, 25719, 25720, 25721, 25722, 25723, 25724);
		
		questItemIds = new int[]
		{
		        17250
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || (st.isCompleted() && !st.isNowAvailable()))
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("sepsoul_q455_05.htm"))
		{
			st.startQuest();
		}
		else if (event.startsWith("sepsoul_q455_08.htm"))
		{
			if (st.isCond(2))
			{
				st.takeItems(17250, -1);
				final StringTokenizer tokenizer = new StringTokenizer(event);
				tokenizer.nextToken();
				switch (Integer.parseInt(tokenizer.nextToken()))
				{
					case 1 :
						st.calcReward(getId(), 1, true);
						break;
					case 2 :
						st.calcReward(getId(), 2, true);
						break;
					case 3 :
						st.calcReward(getId(), 3, true);
						break;
					case 4 :
						st.calcReward(getId(), 4, true);
						break;
					default :
						break;
				}
				htmltext = "sepsoul_q455_08.htm";
				st.exitQuest(QuestType.DAILY, true);
			}
		}
		else if (event.startsWith("sepsoul_q455_11.htm"))
		{
			if (st.isCond(3))
			{
				st.takeItems(17250, -1);
				final StringTokenizer tokenizer = new StringTokenizer(event);
				tokenizer.nextToken();
				switch (Integer.parseInt(tokenizer.nextToken()))
				{
					case 1 :
						st.calcReward(getId(), 5, true);
						break;
					case 2 :
						st.calcReward(getId(), 6, true);
						break;
					case 3 :
						st.calcReward(getId(), 7, true);
						break;
					case 4 :
						st.calcReward(getId(), 8, true);
						break;
					default :
						break;
				}
				
				if (Rnd.chance(25))
				{
					st.calcReward(getId(), 9, true);
				}
				htmltext = "sepsoul_q455_11.htm";
				st.exitQuest(QuestType.DAILY, true);
			}
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
		
		if (ArrayUtils.contains(new int[]
		{
		        32864, 32865, 32866, 32867, 32868, 32869, 32870
		}, npc.getId()))
		{
			switch (st.getState())
			{
				case State.CREATED :
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "sepsoul_q455_01.htm";
					}
					else
					{
						htmltext = "sepsoul_q455_00.htm";
					}
					break;
				case State.STARTED :
					if (st.isCond(1))
					{
						htmltext = "sepsoul_q455_06.htm";
					}
					else if (st.isCond(2))
					{
						htmltext = "sepsoul_q455_07.htm";
					}
					else if (st.isCond(3))
					{
						htmltext = "sepsoul_q455_10.htm";
					}
					break;
				case State.COMPLETED :
					if (st.isNowAvailable())
					{
						if (player.getLevel() >= getMinLvl(getId()))
						{
							htmltext = "sepsoul_q455_01.htm";
						}
						else
						{
							htmltext = "sepsoul_q455_00.htm";
						}
						st.setState(State.CREATED);
					}
					else
					{
						htmltext = "sepsoul_q455_00a.htm";
					}
					break;
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st != null)
		{
			final int npcId = npc.getId();
			
			if (npcId == 25718 || npcId == 25719 || npcId == 25720 || npcId == 25721 || npcId == 25722 || npcId == 25723 || npcId == 25724)
			{
				if (player.getParty() != null)
				{
					for (final Player partyMember : player.getParty().getMembers())
					{
						final QuestState qs = partyMember.getQuestState(getName());
						if (qs != null && qs.isCond(1) && qs.calcDropItems(getId(), 17250, npc.getId(), 1))
						{
							qs.setCond(2);
						}
						else if (qs != null && qs.isCond(2) && qs.calcDropItems(getId(), 17250, npc.getId(), 2))
						{
							qs.setCond(3);
						}
					}
				}
				else
				{
					if (st != null && st.isCond(1) && st.calcDropItems(getId(), 17250, npc.getId(), 1))
					{
						st.setCond(2);
					}
					else if (st != null && st.isCond(2) && st.calcDropItems(getId(), 17250, npc.getId(), 2))
					{
						st.setCond(3);
					}
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _455_WingsOfSand(455, _455_WingsOfSand.class.getSimpleName(), "");
	}
}