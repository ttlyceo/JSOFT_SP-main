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

/**
 * Rework by LordWinter 22.12.2019
 */
public class _10283_RequestOfIceMerchant extends Quest
{
	private boolean _isBusy = false;
	private int _talker = 0;

	public _10283_RequestOfIceMerchant(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32020);
		addTalkId(32020, 32022, 32760);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if ((npc.getId() == 32760) && "DESPAWN".equals(event))
		{
			_isBusy = false;
			_talker = 0;
			npc.deleteMe();
			return super.onAdvEvent(event, npc, player);
		}

		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return null;
		}

		String htmltext = null;
		switch (event)
		{
			case "32020-03.htm":
			{
				htmltext = event;
				break;
			}
			case "32020-04.htm":
			{
				if (st.isCreated() && npc.getId() == 32020)
				{
					st.startQuest();
					st.setMemoState(1);
					htmltext = event;
				}
				break;
			}
			case "32020-05.htm":
			case "32020-06.htm":
			{
				if (st.isMemoState(1) && npc.getId() == 32020)
				{
					htmltext = event;
				}
				break;
			}
			case "32020-07.htm":
			{
				if (st.isMemoState(1) && npc.getId() == 32020)
				{
					st.setMemoState(2);
					st.setCond(2);
					htmltext = event;
				}
				break;
			}
			case "32022-02.htm":
			{
				if (st.isMemoState(2) && npc.getId() == 32022)
				{
					if (!_isBusy)
					{
						_isBusy = true;
						_talker = player.getObjectId();
						st.setCond(3);
						addSpawn(32760, 104476, -107535, -3688, 44954, false, 0, false);
					}
					else
					{
						htmltext = (_talker == player.getObjectId() ? event : "32022-03.htm");
					}
				}
				break;
			}
			case "32760-02.htm":
			case "32760-03.htm":
			{
				if (st.isMemoState(2) && npc.getId() == 32760)
				{
					htmltext = event;
				}
				break;
			}
			case "32760-04.htm":
			{
				if (st.isMemoState(2) && npc.getId() == 32760)
				{
					st.calcExpAndSp(getId());
					st.calcReward(getId());
					st.exitQuest(false, true);
					htmltext = event;
					startQuestTimer("DESPAWN", 2000, npc, null);
				}
				break;
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		String htmltext = getNoQuestMsg(player);
		if (st.isCompleted())
		{
			if (npc.getId() == 32020)
			{
				htmltext = "32020-02.htm";
			}
			else if (npc.getId() == 32760)
			{
				htmltext = "32760-06.htm";
			}
		}
		else if (st.isCreated())
		{
			final QuestState st1 = player.getQuestState("_115_TheOtherSideOfTruth");
			htmltext = ((player.getLevel() >= getMinLvl(getId())) && (st1 != null) && (st1.isCompleted())) ? "32020-01.htm" : "32020-08.htm";
		}
		else if (st.isStarted())
		{
			switch (npc.getId())
			{
				case 32020 :
				{
					if (st.isMemoState(1))
					{
						htmltext = "32020-09.htm";
					}
					else if (st.isMemoState(2))
					{
						htmltext = "32020-10.htm";
					}
					break;
				}
				case 32022 :
				{
					if (st.isMemoState(2))
					{
						htmltext = "32022-01.htm";
					}
					break;
				}
				case 32760 :
				{
					if (st.isMemoState(2))
					{
						htmltext = (_talker == player.getObjectId() ? "32760-01.htm" : "32760-05.htm");
					}
					break;
				}
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _10283_RequestOfIceMerchant(10283, _10283_RequestOfIceMerchant.class.getSimpleName(), "");
	}
}