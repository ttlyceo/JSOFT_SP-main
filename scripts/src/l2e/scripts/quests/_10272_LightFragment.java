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

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 15.12.2019
 */
public class _10272_LightFragment extends Quest
{
	public _10272_LightFragment(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32560);
		addTalkId(32560, 32559, 32566, 32567, 32557);

		addKillId(22536, 22537, 22538, 22539, 22540, 22541, 22542, 22543, 22544, 22547, 22550, 22551, 22552, 22596);
		
		questItemIds = new int[]
		{
		        13853, 13854
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

		if (event.equalsIgnoreCase("32560-06.htm") && npc.getId() == 32560)
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32559-03.htm") && npc.getId() == 32559)
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("32559-07.htm") && npc.getId() == 32559)
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
			}
		}
		else if (event.equalsIgnoreCase("pay") && npc.getId() == 32566)
		{
			if (st.getQuestItemsCount(57) >= 10000)
			{
				st.takeItems(57, 10000);
				htmltext = "32566-05.htm";
			}
			else if (st.getQuestItemsCount(57) < 10000)
			{
				htmltext = "32566-04a.htm";
			}
		}
		else if (event.equalsIgnoreCase("32567-04.htm") && npc.getId() == 32567)
		{
			if (st.isCond(3))
			{
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("32559-12.htm") && npc.getId() == 32559)
		{
			if (st.isCond(4))
			{
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("32557-03.htm") && npc.getId() == 32557)
		{
			if (st.getQuestItemsCount(13854) >= 100)
			{
				st.takeItems(13854, 100);
				st.set("wait", "1");
			}
			else
			{
				htmltext = "32557-04.htm";
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

		if (npc.getId() == 32560)
		{
			switch (st.getState())
			{
				case State.CREATED :
					final QuestState _prev = player.getQuestState("_10271_TheEnvelopingDarkness");
					if ((_prev != null) && (_prev.getState() == State.COMPLETED) && (player.getLevel() >= getMinLvl(getId())))
					{
						htmltext = "32560-01.htm";
					}
					else
					{
						htmltext = "32560-02.htm";
					}
					if (player.getLevel() < 75)
					{
						htmltext = "32560-03.htm";
					}
					break;
				case State.STARTED:
					htmltext = "32560-06.htm";
					break;
				case State.COMPLETED :
					htmltext = "32560-04.htm";
					break;
			}
      
			if (st.isCond(2))
			{
				htmltext = "32560-06.htm";
			}
		}
		else if (npc.getId() == 32559)
		{
			switch (st.getState())
			{
				case State.COMPLETED :
					htmltext = "32559-19.htm";
					break;
			}
			
			if (st.isCond(1))
			{
				htmltext = "32559-01.htm";
			}
			else if (st.isCond(2))
			{
				htmltext = "32559-04.htm";
			}
			else if (st.isCond(3))
			{
				htmltext = "32559-08.htm";
			}
			else if (st.isCond(4))
			{
				htmltext = "32559-10.htm";
			}
			else if (st.isCond(5))
			{
				if (st.getQuestItemsCount(13853) >= 100)
				{
					htmltext = "32559-15.htm";
					st.setCond(6, true);
				}
				else if (st.getQuestItemsCount(13853) >= 1)
				{
					htmltext = "32559-14.htm";
				}
				else if (st.getQuestItemsCount(13853) < 1)
				{
					htmltext = "32559-13.htm";
				}
			}
			else if (st.isCond(6))
			{
				if (st.getQuestItemsCount(13854) < 100)
				{
					htmltext = "32559-16.htm";
				}
				else
				{
					htmltext = "32559-17.htm";
					st.setCond(7, true);
				}
			}
			else if (st.isCond(8))
			{
				htmltext = "32559-18.htm";
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		else if (npc.getId() == 32566)
		{
			switch (st.getState())
			{
				case State.COMPLETED :
					htmltext = "32559-19.htm";
					break;
			}
			
			if (st.isCond(1))
			{
				htmltext = "32566-02.htm";
			}
			else if (st.isCond(2))
			{
				htmltext = "32566-02.htm";
			}
			else if (st.isCond(3))
			{
				htmltext = "32566-01.htm";
			}
			else if (st.isCond(4))
			{
				htmltext = "32566-09.htm";
			}
			else if (st.isCond(5))
			{
				htmltext = "32566-10.htm";
			}
			else if (st.isCond(6))
			{
				htmltext = "32566-10.htm";
			}
		}
		else if (npc.getId() == 32567)
		{
			if (st.isCond(3))
			{
				htmltext = "32567-01.htm";
			}
			else if (st.isCond(4))
			{
				htmltext = "32567-05.htm";
			}
		}
		else if (npc.getId() == 32557)
		{
			if (st.isCond(7))
			{
				if (st.getInt("wait") == 1)
				{
					htmltext = "32557-05.htm";
					st.unset("wait");
					st.setCond(8, true);
					st.giveItems(13855, 1);
				}
				else
				{
					htmltext = "32557-01.htm";
				}
			}
			else if (st.isCond(8))
			{
				htmltext = "32557-06.htm";
			}
		}
		return htmltext;
  	}
            
  	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 5);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = player.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), 13853, npc.getId(), 100);
		}
		return super.onKill(npc, player, isSummon);
	}
  
	public static void main(String[] args)
	{
		new _10272_LightFragment(10272, _10272_LightFragment.class.getSimpleName(), "");
	}
}