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
package l2e.scripts.custom;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.QuestState;
import l2e.scripts.ai.AbstractNpcAI;

public final class Jinia extends AbstractNpcAI
{
	private Jinia()
	{
		super(Jinia.class.getSimpleName(), "custom");

		addStartNpc(32781);
		addFirstTalkId(32781);
		addTalkId(32781);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		switch (event)
		{
			case "32781-10.htm":
			case "32781-11.htm":
			{
				htmltext = event;
				break;
			}
			case "check":
			{
				if (hasAtLeastOneQuestItem(player, 15469, 15470))
				{
					htmltext = "32781-03.htm";
				}
				else
				{
					final QuestState qs = player.getQuestState("_10286_ReunionWithSirra");
					if ((qs != null) && qs.isCompleted())
					{
						giveItems(player, 15469, 1);
					}
					else
					{
						giveItems(player, 15470, 1);
					}
					htmltext = "32781-04.htm";
				}
				break;
			}
		}
		return htmltext;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState qs = player.getQuestState("_10286_ReunionWithSirra");
		if ((qs != null) && (player.getLevel() >= 82))
		{
			if (qs.isCompleted())
			{
				return "32781-02.htm";
			}
			else if (qs.isCond(5) || qs.isCond(6))
			{
				return "32781-09.htm";
			}
		}
		return "32781-01.htm";
	}

	public static void main(String[] args)
	{
		new Jinia();
	}
}
