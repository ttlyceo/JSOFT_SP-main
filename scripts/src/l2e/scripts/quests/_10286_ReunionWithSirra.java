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
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 22.12.2019
 */
public final class _10286_ReunionWithSirra extends Quest
{
	public _10286_ReunionWithSirra(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32020);
		addTalkId(32020, 32760, 32762, 32781);
		
		questItemIds = new int[]
		{
		        15470
		};
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
			case "32020-03.htm":
			case "32760-02.htm":
			case "32760-03.htm":
			case "32760-04.htm":
			{
				if (qs.isMemoState(1))
				{
					htmltext = event;
				}
				break;
			}
			case "32760-05.htm":
			{
				if (qs.isMemoState(1))
				{
					final Npc sirra = addSpawn(32762, -23905, -8790, -5384, 56238, false, 0, false, npc.getReflection());
					sirra.broadcastPacketToOthers(2000, new NpcSay(sirra.getObjectId(), Say2.NPC_ALL, sirra.getId(), NpcStringId.YOU_ADVANCED_BRAVELY_BUT_GOT_SUCH_A_TINY_RESULT_HOHOHO));
					qs.set("ex", 1);
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "32760-07.htm":
			{
				if (qs.isMemoState(1) && (qs.getInt("ex") == 2))
				{
					qs.unset("ex");
					qs.setMemoState(2);
					final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
					if (world != null)
					{
						world.removeAllowed(player.getObjectId());
					}
					player.setReflection(ReflectionManager.DEFAULT);
					htmltext = event;
				}
				break;
			}
			case "32760-08.htm":
			{
				if (qs.isMemoState(2))
				{
					qs.setCond(5, true);
					player.teleToLocation(new Location(113793, -109342, -845, 0), 0, true, player.getReflection());
					htmltext = event;
				}
				break;
			}
			case "32762-02.htm":
			case "32762-03.htm":
			{
				if (qs.isMemoState(1) && (qs.getInt("ex") == 1))
				{
					htmltext = event;
				}
				break;
			}
			case "32762-04.htm":
			{
				if (qs.isMemoState(1) && (qs.getInt("ex") == 1))
				{
					if (!hasQuestItems(player, 15470))
					{
						giveItems(player, 15470, 5);
					}
					qs.set("ex", 2);
					qs.setCond(4, true);
					htmltext = event;
				}
				break;
			}
			case "32781-02.htm":
			case "32781-03.htm":
			{
				if (qs.isMemoState(2))
				{
					htmltext = event;
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
					htmltext = "32020-05.htm";
				}
				break;
			}
			case State.CREATED:
			{
				if (npc.getId() == 32020)
				{
					qs = player.getQuestState("_10285_MeetingSirra");
					htmltext = ((player.getLevel() >= getMinLvl(getId())) && (qs != null) && qs.isCompleted()) ? "32020-01.htm" : "32020-04.htm";
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
							htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32020-06.htm" : "32020-08.htm";
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
							switch (qs.getInt("ex"))
							{
								case 0:
								{
									htmltext = "32760-01.htm";
									break;
								}
								case 1:
								{
									htmltext = "32760-05.htm";
									break;
								}
								case 2:
								{
									htmltext = "32760-06.htm";
									break;
								}
							}
						}
						break;
					}
					case 32762 :
					{
						if (qs.isMemoState(1))
						{
							final int state = qs.getInt("ex");
							if (state == 1)
							{
								htmltext = "32762-01.htm";
							}
							else if (state == 2)
							{
								htmltext = "32762-05.htm";
							}
						}
						break;
					}
					case 32781 :
					{
						if (qs.isMemoState(10))
						{
							qs.calcExpAndSp(getId());
							qs.exitQuest(false, true);
							htmltext = "32781-01.htm";
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
		new _10286_ReunionWithSirra(10286, _10286_ReunionWithSirra.class.getSimpleName(), "");
	}
}