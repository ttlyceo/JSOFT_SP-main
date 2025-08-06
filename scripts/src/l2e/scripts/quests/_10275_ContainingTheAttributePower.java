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

import l2e.commons.util.Util;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 16.12.2019
 */
public class _10275_ContainingTheAttributePower extends Quest
{
	public _10275_ContainingTheAttributePower(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30839, 31307);
		addTalkId(30839, 31307, 32325, 32326);
		
		addKillId(27381, 27380);
		
		questItemIds = new int[]
		{
		        13845, 13881, 13861, 13862
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
		
		switch (event)
		{
			case "30839-02.htm" :
			case "31307-02.htm" :
				if (st.isCreated())
				{
					st.startQuest();
				}
				break;
			case "30839-05.htm" :
				if (st.isCond(1))
				{
					st.setCond(2, true);
				}
				break;
			case "31307-05.htm" :
				if (st.isCond(6))
				{
					st.setCond(7, true);
				}
				break;
			case "32325-03.htm" :
				if (st.isCond(2))
				{
					st.giveItems(13845, 1, Elementals.FIRE, 10);
					st.setCond(3, true);
				}
				break;
			case "32326-03.htm" :
				if (st.isCond(7))
				{
					st.giveItems(13881, 1, Elementals.EARTH, 10);
					st.setCond(8, true);
				}
				break;
			case "32325-06.htm" :
				if (st.hasQuestItems(13845))
				{
					st.takeItems(13845, 1);
					htmltext = "32325-07.htm";
				}
				st.giveItems(13845, 1, Elementals.FIRE, 10);
				break;
			case "32326-06.htm" :
				if (st.hasQuestItems(13881))
				{
					st.takeItems(13881, 1);
					htmltext = "32326-07.htm";
				}
				st.giveItems(13881, 1, Elementals.EARTH, 10);
				break;
			case "32325-09.htm" :
				if (st.isCond(4))
				{
					SkillsParser.getInstance().getInfo(2635, 1).getEffects(player, player, false);
					st.giveItems(13845, 1, Elementals.FIRE, 10);
					st.setCond(5, true);
				}
				break;
			case "32326-09.htm" :
				if (st.isCond(9))
				{
					SkillsParser.getInstance().getInfo(2636, 1).getEffects(player, player, false);
					st.giveItems(13881, 1, Elementals.EARTH, 10);
					st.setCond(10, true);
				}
				break;
		}
		
		if (Util.isDigit(event))
		{
			if (st.isCond(6) || st.isCond(11))
			{
				st.calcExpAndSp(getId());
				st.calcReward(getId(), Integer.valueOf(event));
				st.exitQuest(false, true);
				htmltext = Integer.toString(npc.getId()) + "-1" + event + ".htm";
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
		
		final int npcId = npc.getId();
		final int cond = st.getCond();
		switch (st.getState())
		{
			case State.COMPLETED :
				if (npcId == 30839)
				{
					htmltext = "30839-0a.htm";
				}
				else if (npcId == 31307)
				{
					htmltext = "31307-0a.htm";
				}
				break;
			case State.CREATED :
				if (player.getLevel() >= getMinLvl(getId()))
				{
					if (npcId == 30839)
					{
						htmltext = "30839-01.htm";
					}
					else
					{
						htmltext = "31307-01.htm";
					}
				}
				else if (npcId == 30839)
				{
					htmltext = "30839-00.htm";
				}
				else
				{
					htmltext = "31307-00.htm";
				}
				break;
			default :
				switch (npcId)
				{
					case 30839 :
						switch (cond)
						{
							case 1 :
								htmltext = "30839-03.htm";
								break;
							case 2 :
								htmltext = "30839-05.htm";
								break;
						}
						break;
					case 31307 :
						switch (cond)
						{
							case 1 :
								htmltext = "31307-03.htm";
								break;
							case 7 :
								htmltext = "31307-05.htm";
								break;
						}
						break;
					case 32325 :
						switch (cond)
						{
							case 2 :
								htmltext = "32325-01.htm";
								break;
							case 3 :
							case 5 :
								htmltext = "32325-04.htm";
								break;
							case 4 :
								htmltext = "32325-08.htm";
								st.takeItems(13845, 1);
								st.takeItems(13861, -1);
								break;
							case 6 :
								htmltext = "32325-10.htm";
								break;
						}
					case 32326 :
						switch (cond)
						{
							case 7 :
								htmltext = "32326-01.htm";
								break;
							case 8 :
							case 10 :
								htmltext = "32326-04.htm";
								break;
							case 9 :
								htmltext = "32326-08.htm";
								st.takeItems(13881, 1);
								st.takeItems(13862, -1);
								break;
							case 11 :
								htmltext = "32326-10.htm";
								break;
						}
						break;
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final int cond = st.getCond();
		switch (npc.getId())
		{
			case 27381 :
				if (st.getItemEquipped(Inventory.PAPERDOLL_RHAND) == 13881 && ((cond == 8) || (cond == 10)))
				{
					if (st.calcDropItems(getId(), 13862, npc.getId(), 6))
					{
						st.setCond(cond + 1);
					}
				}
				break;
			case 27380 :
				if (st.getItemEquipped(Inventory.PAPERDOLL_RHAND) == 13845 && ((cond >= 3) || (cond <= 5)))
				{
					if (st.calcDropItems(getId(), 13861, npc.getId(), 6))
					{
						st.setCond(cond + 1);
					}
				}
				break;
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _10275_ContainingTheAttributePower(10275, _10275_ContainingTheAttributePower.class.getSimpleName(), "");
	}
}