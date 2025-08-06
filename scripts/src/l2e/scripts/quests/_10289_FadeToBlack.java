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
 * Rework by LordWinter 22.12.2019
 */
public class _10289_FadeToBlack extends Quest
{
	public _10289_FadeToBlack(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32757);
		addTalkId(32757);
		
		addKillId(25701);
		
		questItemIds = new int[]
		{
		        15527, 15528
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (npc.getId() == 32757)
		{
			if (event.equalsIgnoreCase("32757-04.htm"))
			{
				if (st.isCreated())
				{
					st.startQuest();
				}
			}
			else if (isNumber(event) && st.getQuestItemsCount(15527) > 0 && st.isCond(3))
			{
				st.takeItems(15527, 1);
				st.calcReward(getId(), Integer.parseInt(event));
				st.exitQuest(true, true);
				htmltext = "32757-08.htm";
			}
		}
		return htmltext;
	}
	
	private boolean isNumber(String str)
	{
		if (str == null || str.length() == 0)
		{
			return false;
		}
		
		for (int i = 0; i < str.length(); i++)
		{
			if (!Character.isDigit(str.charAt(i)))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		final QuestState secretMission = player.getQuestState("_10288_SecretMission");
		if (st == null)
		{
			return htmltext;
		}
		
		if (npc.getId() == 32757)
		{
			switch(st.getState())
			{
				case State.CREATED :
					if (player.getLevel() >= getMinLvl(getId()) && secretMission != null && secretMission.getState() == State.COMPLETED)
					{
						htmltext = "32757-02.htm";
					}
					else if (player.getLevel() < 82)
					{
						htmltext = "32757-00.htm";
					}
					else
					{
						htmltext = "32757-01.htm";
					}
					break;
				case State.STARTED :
					if (st.isCond(1))
					{
						htmltext = "32757-04b.htm";
					}
					else if (st.isCond(2))
					{
						htmltext = "32757-05.htm";
						st.calcExpAndSp(getId());
						st.setCond(1, true);
					}
					else if (st.isCond(3))
					{
						htmltext = "32757-06.htm";
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
		if (st == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		if (player.isInParty())
		{
			final Player member = getRandomPartyMember(player, 1);
			final QuestState st1 = member.getQuestState(getName());
			if (st1 != null && st1.calcDropItems(getId(), 15527, npc.getId(), 1))
			{
				st1.setCond(3, true);
			}
			
			final int rnd = getRandom(member.getParty().getMemberCount());
			int idx = 0;
			
			for (final Player pl : member.getParty().getMembers())
			{
				final QuestState st2 = pl.getQuestState(getName());
				if (st2 != null && pl.getObjectId() != member.getObjectId() && st2.calcDropItems(getId(), idx == rnd ? 15527 : 15528, npc.getId(), 1))
				{
					st2.setCond(idx == rnd ? 3 : 2, true);
				}
				idx++;
			}
		}
		else
		{
			if (st.calcDropItems(getId(), 15527, npc.getId(), 1))
			{
				st.setCond(3, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	
	public static void main(String[] args)
	{
		new _10289_FadeToBlack(10289, _10289_FadeToBlack.class.getSimpleName(), "");
	}
}