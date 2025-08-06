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
public final class _10284_AcquisitionOfDivineSword extends Quest
{
	public _10284_AcquisitionOfDivineSword(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32020);
		addTalkId(32020, 32760, 32654, 32653);
		
		questItemIds = new int[]
		{
		        15514
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
				if (qs.isCreated() && npc.getId() == 32020)
				{
					qs.startQuest();
					qs.setMemoState(1);
					htmltext = event;
				}
				break;
			}
			case "32020-03.htm":
			case "32760-02a.htm":
			case "32760-02b.htm":
			case "32760-03a.htm":
			case "32760-03b.htm":
			case "32760-04a.htm":
			case "32760-04b.htm":
			{
				if (qs.isMemoState(1) && npc.getId() == 32760)
				{
					htmltext = event;
				}
				break;
			}
			case "32760-02c.htm":
			{
				if (qs.isMemoState(1) && npc.getId() == 32760)
				{
					qs.set("ex1", 1);
					htmltext = event;
				}
				break;
			}
			case "another_story":
			{
				if (qs.isMemoState(1) && npc.getId() == 32760)
				{
					if ((qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 0) && (qs.getInt("ex3") == 0))
					{
						htmltext = "32760-05a.htm";
					}
					else if ((qs.getInt("ex1") == 0) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 0))
					{
						htmltext = "32760-05b.htm";
					}
					else if ((qs.getInt("ex1") == 0) && (qs.getInt("ex2") == 0) && (qs.getInt("ex3") == 1))
					{
						htmltext = "32760-05c.htm";
					}
					else if ((qs.getInt("ex1") == 0) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 1))
					{
						htmltext = "32760-05d.htm";
					}
					else if ((qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 0) && (qs.getInt("ex3") == 1))
					{
						htmltext = "32760-05e.htm";
					}
					else if ((qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 0))
					{
						htmltext = "32760-05f.htm";
					}
					else if ((qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 1))
					{
						htmltext = "32760-05g.htm";
					}
				}
				break;
			}
			case "32760-03c.htm":
			{
				if (qs.isMemoState(1) && npc.getId() == 32760)
				{
					qs.set("ex2", 1);
					htmltext = event;
				}
				break;
			}
			case "32760-04c.htm":
			{
				if (qs.isMemoState(1) && npc.getId() == 32760)
				{
					qs.set("ex3", 1);
					htmltext = event;
				}
				break;
			}
			case "32760-06.htm":
			{
				if (qs.isMemoState(1) && (qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 1) && npc.getId() == 32760)
				{
					htmltext = event;
				}
				break;
			}
			case "32760-07.htm":
			{
				if (qs.isMemoState(1) && (qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 1) && npc.getId() == 32760)
				{
					qs.unset("ex1");
					qs.unset("ex2");
					qs.unset("ex3");
					qs.setCond(3, true);
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
			case "exit_instance":
			{
				if (qs.isMemoState(2))
				{
					player.teleToLocation(new Location(113793, -109342, -845, 0), 0, true, player.getReflection());
				}
				break;
			}
			case "32654-02.htm":
			case "32654-03.htm":
			case "32653-02.htm":
			case "32653-03.htm":
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
					qs = player.getQuestState("_10283_RequestOfIceMerchant");
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
						switch (qs.getMemoState())
						{
							case 1:
							{
								htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32020-06.htm" : "32020-08.htm";
								break;
							}
							case 2:
							{
								htmltext = "32020-07.htm";
								break;
							}
						}
						break;
					}
					case 32760 :
					{
						if (qs.isMemoState(1))
						{
							if ((qs.getInt("ex1") == 0) && (qs.getInt("ex2") == 0) && (qs.getInt("ex3") == 0))
							{
								htmltext = "32760-01.htm";
							}
							else if ((qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 0) && (qs.getInt("ex3") == 0))
							{
								htmltext = "32760-01a.htm";
							}
							else if ((qs.getInt("ex1") == 0) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 0))
							{
								htmltext = "32760-01b.htm";
							}
							else if ((qs.getInt("ex1") == 0) && (qs.getInt("ex2") == 0) && (qs.getInt("ex3") == 1))
							{
								htmltext = "32760-01c.htm";
							}
							else if ((qs.getInt("ex1") == 0) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 1))
							{
								htmltext = "32760-01d.htm";
							}
							else if ((qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 0) && (qs.getInt("ex3") == 1))
							{
								htmltext = "32760-01e.htm";
							}
							else if ((qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 0))
							{
								htmltext = "32760-01f.htm";
							}
							else if ((qs.getInt("ex1") == 1) && (qs.getInt("ex2") == 1) && (qs.getInt("ex3") == 1))
							{
								htmltext = "32760-01g.htm";
							}
						}
						break;
					}
					case 32654 :
					{
						switch (qs.getMemoState())
						{
							case 2:
							{
								htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32654-01.htm" : "32654-05.htm";
								break;
							}
							case 3:
							{
								qs.calcExpAndSp(getId());
								qs.calcReward(getId());
								qs.exitQuest(false, true);
								htmltext = "32654-04.htm";
								break;
							}
						}
						break;
					}
					case 32653 :
					{
						switch (qs.getMemoState())
						{
							case 2:
							{
								htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32653-01.htm" : "32653-05.htm";
								break;
							}
							case 3:
							{
								qs.calcExpAndSp(getId());
								qs.calcReward(getId());
								qs.exitQuest(false, true);
								htmltext = "32653-04.htm";
								break;
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
		new _10284_AcquisitionOfDivineSword(10284, _10284_AcquisitionOfDivineSword.class.getSimpleName(), "");
	}
}