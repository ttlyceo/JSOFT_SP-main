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

import l2e.gameserver.Config;
import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 15.12.2019
 */
public class _10270_BirthOfTheSeed extends Quest
{
	public _10270_BirthOfTheSeed(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32563);
		addTalkId(32563, 32567, 32566, 32559);

		addKillId(25666, 25665, 25634);

		questItemIds = new int[]
		{
		        13868, 13869, 13870
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

		if (event.equalsIgnoreCase("32563-05.htm") && npc.getId() == 32563)
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
		else if (event.equalsIgnoreCase("32559-09.htm") && npc.getId() == 32559)
		{
			if (st.isCond(3))
			{
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("32559-13.htm") && npc.getId() == 32559)
		{
			if (st.isCond(5))
			{
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		else if (event.equalsIgnoreCase("32566-05.htm") && npc.getId() == 32566)
		{
			if (st.getQuestItemsCount(57) < 10000)
			{
				htmltext = "32566-04a.htm";
			}
			else
			{
				st.takeItems(57, 10000);
				st.set("pay", "1");
			}
		}
		else if (event.equalsIgnoreCase("32567-05.htm") && npc.getId() == 32567)
		{
			if (st.isCond(4))
			{
				st.setCond(5, true);
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
		
		switch(npcId)
		{
			case 32563 :
				switch (st.getState())
				{
					case State.CREATED:
						if (player.getLevel() < getMinLvl(getId()))
						{
							htmltext = "32563-02.htm";
						}
						else
						{
							htmltext = "32563-01.htm";
						}
						break;
					case State.STARTED:
						if (cond == 1)
						{
							htmltext = "32563-06.htm";
						}
						break;
					case State.COMPLETED:
						htmltext = "32563-03.htm";
						break;
				}
				break;
			case 32559 :
				if (cond == 1)
				{
					htmltext = "32559-01.htm";
				}
				else if (cond == 2)
				{
					if (st.getQuestItemsCount(13868) < 1 && st.getQuestItemsCount(13869) < 1 && st.getQuestItemsCount(13870) < 1)
					{
						htmltext = "32559-04.htm";
					}
					else if (st.getQuestItemsCount(13868) + st.getQuestItemsCount(13869) + st.getQuestItemsCount(13870) < 3)
					{
						htmltext = "32559-05.htm";
					}
					else if (st.getQuestItemsCount(13868) == 1 && st.getQuestItemsCount(13869) == 1 && st.getQuestItemsCount(13870) == 1)
					{
						htmltext = "32559-06.htm";
						st.takeItems(13868, 1);
						st.takeItems(13869, 1);
						st.takeItems(13870, 1);
						st.setCond(3, true);
					}
				}
				else if (cond == 3 || cond == 4)
				{
					htmltext = "32559-07.htm";
				}
				else if (cond == 5)
				{
					htmltext = "32559-12.htm";
				}
				if (st.getState() == State.COMPLETED)
				{
					htmltext = "32559-02.htm";
				}
				break;
			case 32566 :
				if (cond < 4)
				{
					htmltext = "32566-02.htm";
				}
				else if (cond == 4)
				{
					if (st.getInt("pay") == 1)
					{
						htmltext = "32566-10.htm";
					}
					else
					{
						htmltext = "32566-04.htm";
					}
				}
				else if (cond > 4)
				{
					htmltext = "32566-12.htm";
				}
				break;
			case 32567 :
				if (cond == 4)
				{
					htmltext = "32567-01.htm";
				}
				else if (cond == 5)
				{
					htmltext = "32567-07.htm";
				}
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() == 25666)
		{
			if (player.getParty() != null)
			{
				final Party party = player.getParty();
				if (party.getCommandChannel() != null)
				{
					final CommandChannel cc = party.getCommandChannel();
					for (final Player partyMember : cc.getMembers())
					{
						if (partyMember == null || !partyMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2))
						{
							continue;
						}
						
						final QuestState st = partyMember.getQuestState(getName());
						if (st != null && st.isCond(2))
						{
							st.calcDoDropItems(getId(), 13869, npc.getId(), 1);
						}
					}
				}
				else
				{
					for (final Player partyMember : party.getMembers())
					{
						if (partyMember == null || !partyMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2))
						{
							continue;
						}
						
						final QuestState st = partyMember.getQuestState(getName());
						if (st != null && st.isCond(2))
						{
							st.calcDoDropItems(getId(), 13869, npc.getId(), 1);
						}
					}
				}
			}
			else
			{
				final QuestState st = player.getQuestState(getName());
				if (st != null && st.isCond(2))
				{
					st.calcDoDropItems(getId(), 13869, npc.getId(), 1);
				}
			}
		}
		else if (npc.getId() == 25665)
		{
			if (player.getParty() != null)
			{
				final Party party = player.getParty();
				if (party.getCommandChannel() != null)
				{
					final CommandChannel cc =party.getCommandChannel();
					for (final Player partyMember : cc.getMembers())
					{
						if (partyMember == null || !partyMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2))
						{
							continue;
						}
						
						final QuestState st = partyMember.getQuestState(getName());
						if (st != null && st.isCond(2))
						{
							st.calcDoDropItems(getId(), 13868, npc.getId(), 1);
						}
					}
				}
				else
				{
					for (final Player partyMember : party.getMembers())
					{
						if (partyMember == null || !partyMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2))
						{
							continue;
						}
						
						final QuestState st = partyMember.getQuestState(getName());
						if (st != null && st.isCond(2))
						{
							st.calcDoDropItems(getId(), 13868, npc.getId(), 1);
						}
					}
				}
			}
			else
			{
				final QuestState st = player.getQuestState(getName());
				if (st != null && st.isCond(2))
				{
					st.calcDoDropItems(getId(), 13868, npc.getId(), 1);
				}
			}
		}
		else if (npc.getId() == 25634)
		{
			if (player.getParty() != null)
			{
				final Party party = player.getParty();
				if (party.getCommandChannel() != null)
				{
					final CommandChannel cc =party.getCommandChannel();
					for (final Player partyMember : cc.getMembers())
					{
						if (partyMember == null || !partyMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2))
						{
							continue;
						}
						
						final QuestState st = partyMember.getQuestState(getName());
						if (st != null && st.isCond(2))
						{
							st.calcDoDropItems(getId(), 13870, npc.getId(), 1);
						}
					}
				}
				else
				{
					for (final Player partyMember : party.getMembers())
					{
						if (partyMember == null || !partyMember.isInRangeZ(npc, Config.ALT_PARTY_RANGE2))
						{
							continue;
						}
						
						final QuestState st = partyMember.getQuestState(getName());
						if (st != null && st.isCond(2))
						{
							st.calcDoDropItems(getId(), 13870, npc.getId(), 1);
						}
					}
				}
			}
			else
			{
				final QuestState st = player.getQuestState(getName());
				if (st != null && st.isCond(2))
				{
					st.calcDoDropItems(getId(), 13870, npc.getId(), 1);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _10270_BirthOfTheSeed(10270, _10270_BirthOfTheSeed.class.getSimpleName(), "");
	}
}