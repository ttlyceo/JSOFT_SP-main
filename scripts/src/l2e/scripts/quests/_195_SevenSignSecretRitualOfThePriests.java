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

import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 27.10.2020
 */
public class _195_SevenSignSecretRitualOfThePriests extends Quest
{
	public _195_SevenSignSecretRitualOfThePriests(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31001);
		addTalkId(31001, 32576, 30289, 30969, 32579, 32577, 32581);

		questItemIds = new int[]
		{
		        13822, 13823
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return getNoQuestMsg(player);
		}
		
		if (event.equalsIgnoreCase("31001-4.htm"))
		{
			final QuestState qs = player.getQuestState("_194_SevenSignContractOfMammon");
			if (qs != null && qs.isCompleted() && player.getLevel() >= getMinLvl(getId()))
			{
				if (st.isCreated())
				{
					st.startQuest();
				}
			}
		}
		else if (event.equalsIgnoreCase("32576-1.htm") && st.isCond(1))
		{
			st.setCond(2, true);
			st.giveItems(13822, 1);
		}
		else if (event.equalsIgnoreCase("30289-3.htm") && st.isCond(2))
		{
			st.setCond(3, true);
			player.doCast(SkillsParser.getInstance().getInfo(6204, 1));
		}
		else if (event.equalsIgnoreCase("30289-6.htm"))
		{
			if (player.isTransformed())
			{
				player.untransform();
			}
			player.doCast(SkillsParser.getInstance().getInfo(6204, 1));
		}
		else if (event.equalsIgnoreCase("30289-7.htm"))
		{
			if (player.isTransformed())
			{
				player.untransform();
			}
		}
		else if (event.equalsIgnoreCase("30289-10.htm") && st.isCond(3))
		{
			if (player.isTransformed())
			{
				player.untransform();
			}
			st.setCond(4, true);
		}
		else if (event.equalsIgnoreCase("32581-3.htm"))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			if (world != null)
			{
				final Reflection inst = world.getReflection();
				inst.setDuration(5 * 60000);
				inst.setEmptyDestroyTime(0);
			}
			player.teleToLocation(-12532, 122329, -2984, true, ReflectionManager.DEFAULT);
		}
		else if (event.equalsIgnoreCase("30969-2.htm") && st.isCond(4))
		{
			st.calcExpAndSp(getId());
			st.exitQuest(false, true);
		}
		else if (event.equalsIgnoreCase("wrong"))
		{
			player.teleToLocation(-78240, 205858, -7856, false, player.getReflection());
			htmltext = "32577-2.htm";
		}
		else if (event.equalsIgnoreCase("empty"))
		{
			return null;
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

		final int cond = st.getCond();
		final int npcId = npc.getId();

		switch (st.getState())
		{
			case State.COMPLETED :
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED :
				if (npcId == 31001)
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						final QuestState qs = player.getQuestState("_194_SevenSignContractOfMammon");
						if (qs != null)
						{
							if (qs.isCompleted())
							{
								htmltext = "31001-0.htm";
							}
							else
							{
								htmltext = "31001-0b.htm";
							}
						}
					}
					else
					{
						htmltext = "31001-0a.htm";
						st.exitQuest(true);
					}
				}
				break;
			case State.STARTED :
				switch (npcId)
				{
					case 31001 :
						if (cond == 1)
						{
							htmltext = "31001-5.htm";
						}
						break;
					case 32576 :
						switch (cond)
						{
							case 1 :
								htmltext = "32576-0.htm";
								break;
							case 2 :
								htmltext = "32576-2.htm";
								break;
						}
						break;
					case 30289 :
						switch (cond)
						{
							case 2 :
								htmltext = "30289-0.htm";
								break;
							case 3 :
								if (player.getInventory().getItemByItemId(13823) != null)
								{
									htmltext = "30289-8.htm";
								}
								else
								{
									htmltext = "30289-5.htm";
								}
								break;
							case 4 :
								htmltext = "30289-11.htm";
								break;
						}
						break;
					case 30969 :
						if (cond == 4)
						{
							htmltext = "30969-0.htm";
						}
						break;
					case 32579 :
						final var ref = player.getReflection();
						if (!ref.isDefault())
						{
							ref.collapse();
						}
						player.teleToLocation(-12532, 122329, -2984, true, ReflectionManager.DEFAULT);
						htmltext = "32579-0.htm";
						break;
					case 32577 :
						htmltext = "32577-0.htm";
						break;
					case 32581 :
						htmltext = "32581-0.htm";
						if ((npc.getSpawn().getX() == -81393) && (npc.getSpawn().getY() == 205565))
						{
							if (player.getInventory().getItemByItemId(13823) == null)
							{
								st.giveItems(13823, 1);
							}
							htmltext = "32581-1.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _195_SevenSignSecretRitualOfThePriests(195, _195_SevenSignSecretRitualOfThePriests.class.getSimpleName(), "");
	}
}