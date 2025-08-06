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
import l2e.gameserver.model.quest.State;

/**
 * Created by LordWinter 18.06.2012
 * Based on L2J Eternity-World
 */
public final class _998_FallenAngelSelect extends Quest
{
	private static final String qn = "_998_FallenAngelSelect";
	
	private static int NATOOLS = 30894;
	
	public _998_FallenAngelSelect(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(NATOOLS);
		addTalkId(NATOOLS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		final Quest q1 = QuestManager.getInstance().getQuest("_142_FallenAngelRequestOfDawn");
		final Quest q2 = QuestManager.getInstance().getQuest("_143_FallenAngelRequestOfDusk");
		var qs1 = player.getQuestState("_142_FallenAngelRequestOfDawn");
		var qs2 = player.getQuestState("_143_FallenAngelRequestOfDusk");
		
		if (event.equalsIgnoreCase("dawn"))
		{
			if (q1 != null)
			{
				if (qs2 != null && (qs2.isStarted() || qs2.isCompleted()))
				{
					return getNoQuestMsg(player);
				}
				qs1 = q1.newQuestState(player);
				qs1.setState(State.STARTED);
				q1.notifyEvent("30894-01.htm", npc, player);
				// st.setState(State.COMPLETED);
			}
			return null;
		}
		else if (event.equalsIgnoreCase("dusk"))
		{
			if (q2 != null)
			{
				if (qs1 != null && (qs1.isStarted() || qs1.isCompleted()))
				{
					return getNoQuestMsg(player);
				}
				qs2 = q2.newQuestState(player);
				qs2.setState(State.STARTED);
				q2.notifyEvent("30894-01.htm", npc, player);
				// st.setState(State.COMPLETED);
			}
			return null;
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		final var qs1 = player.getQuestState("_142_FallenAngelRequestOfDawn");
		final var qs2 = player.getQuestState("_143_FallenAngelRequestOfDusk");
		final var isActive = (qs1 != null && (qs1.isStarted() || qs1.isCompleted())) || (qs2 != null && (qs2.isStarted() || qs2.isCompleted()));
		if (st.isStarted() && !isActive)
		{
			htmltext = "30894-01.htm";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _998_FallenAngelSelect(998, qn, "");
	}
}