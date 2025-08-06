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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.QuestState.QuestType;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 27.12.2019
 */
public final class _456_DontKnowDontCare extends Quest
{
	public _456_DontKnowDontCare(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32864, 32865, 32866, 32867, 32868, 32869, 32870, 32891);
		addTalkId(32864, 32865, 32866, 32867, 32868, 32869, 32870, 32891, 32884, 32885, 32886);
		addFirstTalkId(32884, 32885, 32886);
		
		questItemIds = new int[]
		{
		        17251, 17252, 17253
		};
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || !st.isCond(1) || st.get("RaidKilled") == null)
		{
			return npc.getId() + "-02.htm";
		}
		
		String htmltext = null;
		
		if (st.isCond(1))
		{
			switch (npc.getId())
			{
				case 32884 :
					if (st.getQuestItemsCount(17251) < 1)
					{
						st.giveItems(17251, 1);
						st.playSound("ItemSound.quest_itemget");
						st.unset("RaidKilled");
						htmltext = npc.getId() + "-01.htm";
					}
					else
					{
						htmltext = npc.getId() + "-03.htm";
					}
					break;
				case 32885 :
					if (st.getQuestItemsCount(17252) < 1)
					{
						st.giveItems(17252, 1);
						st.playSound("ItemSound.quest_itemget");
						st.unset("RaidKilled");
						htmltext = npc.getId() + "-01.htm";
					}
					else
					{
						htmltext = npc.getId() + "-03.htm";
					}
					break;
				case 32886 :
					if (st.getQuestItemsCount(17253) < 1)
					{
						st.giveItems(17253, 1);
						st.playSound("ItemSound.quest_itemget");
						st.unset("RaidKilled");
						htmltext = npc.getId() + "-01.htm";
					}
					else
					{
						htmltext = npc.getId() + "-03.htm";
					}
					break;
				default :
					break;
			}
			
			if (st.getQuestItemsCount(17251) > 0 && st.getQuestItemsCount(17252) > 0 && st.getQuestItemsCount(17253) > 0)
			{
				st.setCond(2, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		String htmltext = getNoQuestMsg(player);
		if (st == null)
		{
			return htmltext;
		}
		
		if (npc.getId() == 32864 || npc.getId() == 32865 || npc.getId() == 32866 || npc.getId() == 32867 || npc.getId() == 32868 || npc.getId() == 32869 || npc.getId() == 32870 || npc.getId() == 32891)
		{
			switch (st.getState())
			{
				case State.COMPLETED :
					if (!st.isNowAvailable())
					{
						htmltext = "32864-02.htm";
						break;
					}
					st.setState(State.CREATED);
				case State.CREATED :
					htmltext = ((player.getLevel() >= getMinLvl(getId())) ? "32864-01.htm" : "32864-03.htm");
					break;
				case State.STARTED :
					switch (st.getCond())
					{
						case 1 :
						{
							htmltext = (hasAtLeastOneQuestItem(player, getRegisteredItemIds()) ? "32864-09.htm" : "32864-08.htm");
							break;
						}
						case 2 :
						{
							if (hasQuestItems(player, getRegisteredItemIds()))
							{
								if (Rnd.chance(1))
								{
									st.calcReward(getId(), 1, true);
								}
								else if (Rnd.chance(5))
								{
									st.calcReward(getId(), 2, true);
								}
								else if (Rnd.chance(10))
								{
									st.calcReward(getId(), 3, true);
								}
								else if (Rnd.chance(15))
								{
									st.calcReward(getId(), 4, true);
								}
								else
								{
									st.calcReward(getId(), 5, true);
								}
								st.calcReward(getId(), 6);
								st.exitQuest(QuestType.DAILY, true);
								htmltext = "32864-10.htm";
							}
							break;
						}
					}
					break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState qs;
		String htmltext = null;
		
		switch (event)
		{
			case "32864-04.htm" :
			case "32864-05.htm" :
			case "32864-06.htm" :
				qs = player.getQuestState(getName());
				if ((qs != null) && qs.isCreated())
				{
					htmltext = event;
				}
				break;
			case "32864-07.htm" :
				qs = player.getQuestState(getName());
				if (qs != null && (qs.isCreated() || (qs.isCompleted() && qs.isNowAvailable())))
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _456_DontKnowDontCare(456, _456_DontKnowDontCare.class.getSimpleName(), "");
	}
}