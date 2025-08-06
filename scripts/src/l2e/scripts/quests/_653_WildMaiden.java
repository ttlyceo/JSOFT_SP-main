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
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Rework by LordWinter 13.03.2022
 */
public class _653_WildMaiden extends Quest
{
	private static final int[][] spawns =
	{
		{
			66578,
			72351,
			-3731,
			0
		},
		{
			77189,
			73610,
			-3708,
			2555
		},
		{
			71809,
			67377,
			-3675,
			29130
		},
		{
			69166,
			88825,
			-3447,
			43886
		}
	};
	
	public _653_WildMaiden(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32013);
		addTalkId(32013, 30181);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("apparition_npc"))
		{
			if (npc != null)
			{
				npc.deleteMe();
				startQuestTimer("spawn_suki", 3600000L, null, null);
			}
			return null;
		}
		else if (event.equalsIgnoreCase("spawn_suki"))
		{
			final Npc suki = GameObjectsStorage.getByNpcId(32013);
			if (suki == null)
			{
				final int chance = Rnd.get(0, 3);
				addSpawn(32013, spawns[chance][0], spawns[chance][1], spawns[chance][2], spawns[chance][3], false, 0L);
			}
			return null;
		}
		
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32013-03.htm") && npc.getId() == 32013)
		{
			if (st.getQuestItemsCount(736) >= 1L && player.getLevel() >= getMinLvl(getId()))
			{
				st.startQuest();
				st.takeItems(736, 1L);
				npc.broadcastPacketToOthers(2000, new MagicSkillUse(npc, npc, 2013, 1, 3500, 0));
				startQuestTimer("apparition_npc", 3500L, npc, null);
			}
			else
			{
				htmltext = "32013-03a.htm";
				st.exitQuest(true);
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
		
		switch (st.getState())
		{
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "32013-02.htm";
				}
				else
				{
					htmltext = "32013-01.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 30181:
						htmltext = "30181-01.htm";
						st.calcReward(getId());
						st.exitQuest(true, true);
						break;
					case 32013:
						htmltext = "32013-04a.htm";
				}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new _653_WildMaiden(653, _653_WildMaiden.class.getSimpleName(), "");
	}
}