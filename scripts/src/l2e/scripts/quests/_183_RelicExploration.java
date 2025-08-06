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

import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;

/**
 * Rework by LordWinter 28.07.2020
 */
public final class _183_RelicExploration extends Quest
{
	public _183_RelicExploration(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30512);
		addTalkId(30512, 30673, 30621);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "30512-04.htm" :
			{
				qs.startQuest();
				qs.setMemoState(1);
				htmltext = event;
				break;
			}
			case "30512-02.htm" :
			{
				htmltext = event;
				break;
			}
			case "30621-02.htm" :
			{
				if (qs.isMemoState(2))
				{
					qs.calcReward(getId());
					if (player.getLevel() < 46)
					{
						qs.calcExpAndSp(getId());
					}
					qs.exitQuest(false, true);
					htmltext = event;
				}
				break;
			}
			case "30673-02.htm" :
			case "30673-03.htm" :
			{
				if (qs.isMemoState(1))
				{
					htmltext = event;
				}
				break;
			}
			case "30673-04.htm" :
			{
				if (qs.isMemoState(1))
				{
					qs.setMemoState(2);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "Contract" :
			{
				final QuestState qs184 = player.getQuestState(_184_NikolasCooperationContract.class.getSimpleName());
				final QuestState qs185 = player.getQuestState(_185_NikolasCooperationConsideration.class.getSimpleName());
				final Quest quest = QuestManager.getInstance().getQuest(_184_NikolasCooperationContract.class.getSimpleName());
				if ((quest != null) && (qs184 == null) && (qs185 == null))
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						quest.notifyEvent("30621-03.htm", npc, player);
					}
					else
					{
						quest.notifyEvent("30621-03a.htm", npc, player);
					}
				}
				break;
			}
			case "Consideration" :
			{
				final QuestState qs184 = player.getQuestState(_184_NikolasCooperationContract.class.getSimpleName());
				final QuestState qs185 = player.getQuestState(_185_NikolasCooperationConsideration.class.getSimpleName());
				final Quest quest = QuestManager.getInstance().getQuest(_185_NikolasCooperationConsideration.class.getSimpleName());
				if ((quest != null) && (qs184 == null) && (qs185 == null))
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						quest.notifyEvent("30621-03.htm", npc, player);
					}
					else
					{
						quest.notifyEvent("30621-03a.htm", npc, player);
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			if (npc.getId() == 30512)
			{
				htmltext = (player.getLevel() >= getMinLvl(getId())) ? "30512-01.htm" : "30512-03.htm";
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case 30512 :
				{
					htmltext = "30512-05.htm";
					break;
				}
				case 30621 :
				{
					if (qs.isMemoState(2))
					{
						htmltext = "30621-01.htm";
					}
					break;
				}
				case 30673 :
				{
					if (qs.isMemoState(1))
					{
						htmltext = "30673-01.htm";
					}
					else if (qs.isMemoState(2))
					{
						htmltext = "30673-05.htm";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _183_RelicExploration(183, _183_RelicExploration.class.getSimpleName(), "");
	}
}