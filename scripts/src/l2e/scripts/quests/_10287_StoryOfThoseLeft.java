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

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 22.12.2019
 */
public final class _10287_StoryOfThoseLeft extends Quest
{
	public _10287_StoryOfThoseLeft(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32020);
		addTalkId(32020, 32760, 32761);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = player.getQuestState(getName());
		if (qs == null || qs.isCompleted())
		{
			return null;
		}

		String htmltext = null;
		switch (event)
		{
			case "32020-02.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					qs.setMemoState(1);
					htmltext = event;
				}
				break;
			}
			case "32020-08.htm":
			{
				if (qs.isMemoState(2))
				{
					htmltext = event;
				}
				break;
			}
			case "32760-02.htm":
			{
				if (qs.isMemoState(1))
				{
					htmltext = event;
				}
				break;
			}
			case "32760-03.htm":
			{
				if (qs.isMemoState(1))
				{
					qs.set("ex1", 1);
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "32760-06.htm":
			{
				if (qs.isMemoState(2))
				{
					qs.setCond(5, true);
					player.teleToLocation(new Location(113793, -109342, -845, 0), 0, true, player.getReflection());
					htmltext = event;
				}
				break;
			}
			case "32761-02.htm":
			{
				if (qs.isMemoState(1) && (qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 0))
				{
					htmltext = event;
				}
				break;
			}
			case "32761-03.htm":
			{
				if (qs.isMemoState(1) && (qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 0))
				{
					qs.set("ex2", 1);
					qs.setCond(4, true);
					htmltext = event;
				}
				break;
			}
			case "10549":
			case "10550":
			case "10551":
			case "10552":
			case "10553":
			case "14219":
			{
				if (qs.isMemoState(2))
				{
					qs.calcReward(getId(), Integer.valueOf(event));
					htmltext = "32020-09.htm";
					qs.exitQuest(false, true);
				}
				break;
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState qs = player.getQuestState(getName());
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.COMPLETED:
			{
				if (npc.getId() == 32020)
				{
					htmltext = "32020-04.htm";
				}
				break;
			}
			case State.CREATED:
			{
				if (npc.getId() == 32020)
				{
					qs = player.getQuestState("_10286_ReunionWithSirra");
					htmltext = ((player.getLevel() >= getMinLvl(getId())) && (qs != null) && qs.isCompleted()) ? "32020-01.htm" : "32020-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case 32020 :
					{
						if (qs.isMemoState(1))
						{
							htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32020-05.htm" : "32020-06.htm";
						}
						else if (qs.isMemoState(2))
						{
							htmltext = "32020-07.htm";
						}
						break;
					}
					case 32760 :
					{
						if (qs.isMemoState(1))
						{
							final int state1 = qs.getInt("ex1");
							final int state2 = qs.getInt("ex2");
							if ((state1 == 0) && (state2 == 0))
							{
								htmltext = "32760-01.htm";
							}
							else if ((state1 == 1) && (state2 == 0))
							{
								htmltext = "32760-04.htm";
							}
							else if ((state1 == 1) && (state2 == 1))
							{
								qs.setCond(5, true);
								qs.setMemoState(2);
								qs.unset("ex1");
								qs.unset("ex2");
								final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
								if (world != null)
								{
									world.removeAllowed(player.getObjectId());
								}
								player.setReflection(ReflectionManager.DEFAULT);
								htmltext = "32760-05.htm";
							}
						}
						break;
					}
					case 32761 :
					{
						if (qs.isMemoState(1))
						{
							final int state1 = qs.getInt("ex1");
							final int state2 = qs.getInt("ex2");
							if ((state1 == 1) && (state2 == 0))
							{
								htmltext = "32761-01.htm";
							}
							else if ((state1 == 0) && (state2 == 0))
							{
								htmltext = "32761-04.htm";
							}
							else if ((state1 == 1) && (state2 == 1))
							{
								htmltext = "32761-05.htm";
							}
						}
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _10287_StoryOfThoseLeft(10287, _10287_StoryOfThoseLeft.class.getSimpleName(), "");
	}
}