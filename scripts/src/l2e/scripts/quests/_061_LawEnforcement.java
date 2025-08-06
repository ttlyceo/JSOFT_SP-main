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
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.serverpackets.updatetype.UserInfoType;

/**
 * Rework by LordWinter 13.12.2019
 */
public final class _061_LawEnforcement extends Quest
{
	private _061_LawEnforcement(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32222);
		addTalkId(32222, 32138, 32469);
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("32222-05.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("32138-09.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equalsIgnoreCase("32469-08.htm") || event.equals("32469-09.htm"))
		{
			if (st.isCond(2))
			{
				player.setClassId(ClassId.judicator.getId());
				player.broadcastCharInfo(UserInfoType.BASIC_INFO, UserInfoType.BASE_STATS, UserInfoType.MAX_HPCPMP, UserInfoType.STATS, UserInfoType.SPEED);
				st.calcReward(getId());
				st.exitQuest(false, true);
			}
		}
		return htmltext;
	}

	@Override
	public final String onTalk(Npc npc, Player player)
	{
		final String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		final int cond = st.getCond();

		if (st.isCompleted())
		{
			return getAlreadyCompletedMsg(player);
		}

		if (npcId == 32222)
		{
			if (cond == 0)
			{
				if (player.getRace() == Race.Kamael)
				{
					if ((player.getClassId() == ClassId.inspector) && (player.getLevel() >= getMinLvl(getId())))
					{
						return "32222-01.htm";
					}
					return "32222-02.htm";
				}
				return "32222-03.htm";
			}
			else if (cond == 1)
			{
				return "32222-06.htm";
			}
		}
		else if (npcId == 32138)
		{
			if (cond == 1)
			{
				return "32138-01.htm";
			}
			else if (cond == 2)
			{
				return "32138-10.htm";
			}
		}
		else if ((npcId == 32469) && (cond == 2))
		{
			return "32469-01.htm";
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _061_LawEnforcement(61, _061_LawEnforcement.class.getSimpleName(), "");
	}
}