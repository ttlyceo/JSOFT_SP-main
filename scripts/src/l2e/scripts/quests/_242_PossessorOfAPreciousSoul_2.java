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
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 25.05.2021
 */
public class _242_PossessorOfAPreciousSoul_2 extends Quest
{
	private static boolean _isSubActive;
	
	public _242_PossessorOfAPreciousSoul_2(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31742);
		addTalkId(31742, 31743, 31744, 31751, 31752, 30759, 30738, 31746, 31748, 31747);

		addKillId(27317);

		_isSubActive = getQuestParams(questId).getBool("isSubActive");
		
		questItemIds = new int[]
		{
		        7590, 7595, 7596
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return getNoQuestMsg(player);
		}

		if ((player.isSubClassActive() && _isSubActive) || !_isSubActive)
		{
			switch (event)
			{
				case "31742-02.htm" :
					final QuestState qs = player.getQuestState(_241_PossessorOfAPreciousSoul_1.class.getSimpleName());
					if ((qs != null && qs.isCompleted()) || st.getQuestItemsCount(7677) > 0)
					{
						if (st.isCreated() && player.getLevel() >= getMinLvl(getId()))
						{
							st.startQuest();
							st.takeItems(7677, -1);
						}
					}
					break;
				case "31743-05.htm" :
					if (st.isCond(1))
					{
						st.setCond(2, true);
					}
					break;
				case "31744-02.htm" :
					if (st.isCond(2))
					{
						st.setCond(3, true);
					}
					break;
				case "31751-02.htm" :
					if (st.isCond(3))
					{
						st.setCond(4, true);
					}
					break;
				case "30759-02.htm" :
					if (st.isCond(6))
					{
						st.setCond(7, true);
					}
					break;
				case "30738-02.htm" :
					if (st.isCond(7))
					{
						st.setCond(8, true);
						st.giveItems(7596, 1);
					}
					break;
				case "30759-05.htm" :
					if (st.isCond(8))
					{
						st.takeItems(7590, -1);
						st.takeItems(7596, -1);
						st.set("awaitsDrops", "1");
						st.setCond(9, true);
					}
					break;
				case "PURE_UNICORN" :
					final var now = System.currentTimeMillis();
					if (st.getLong("spawnDelay") < now)
					{
						st.set("spawnDelay", (now + 30000L));
						npc.doDie(null);
						st.addSpawn(31747, 85884, -76588, -3470, 30000);
					}
					return null;
			}
		}
		else
		{
			return "sub.htm";
		}
		return event;
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

		if (st.isStarted() && !player.isSubClassActive() && _isSubActive)
		{
			return "sub.htm";
		}

		switch (npc.getId())
		{
			case 31742 :
				switch (st.getState())
				{
					case State.CREATED:
						final QuestState qs = player.getQuestState(_241_PossessorOfAPreciousSoul_1.class.getSimpleName());
						if ((qs != null && qs.isCompleted()) || st.getQuestItemsCount(7677) > 0)
						{
							htmltext = (((player.isSubClassActive() && _isSubActive) || !_isSubActive) && (player.getLevel() >= getMinLvl(getId()))) ? "31742-01.htm" : "31742-00.htm";
						}
						break;
					case State.STARTED:
						switch (st.getCond())
						{
							case 1:
								htmltext = "31742-03.htm";
								break;
							case 11:
								htmltext = "31742-04.htm";
								st.calcExpAndSp(getId());
								st.calcReward(getId());
								st.exitQuest(false, true);
								break;
						}
						break;
					case State.COMPLETED:
						htmltext = getAlreadyCompletedMsg(player);
						break;
				}
				break;
			case 31743 :
				switch (st.getCond())
				{
					case 1:
						htmltext = "31743-01.htm";
						break;
					case 2:
						htmltext = "31743-06.htm";
						break;
					case 11:
						htmltext = "31743-07.htm";
						break;
				}
				break;
			case 31744 :
				switch (st.getCond())
				{
					case 2:
						htmltext = "31744-01.htm";
						break;
					case 3:
						htmltext = "31744-03.htm";
						break;
				}
				break;
			case 31751 :
				switch (st.getCond())
				{
					case 3:
						htmltext = "31751-01.htm";
						break;
					case 4:
						htmltext = "31751-03.htm";
						break;
					case 5:
						if (st.hasQuestItems(7590))
						{
							st.setCond(6, true);
							htmltext = "31751-04.htm";
						}
						break;
					case 6:
						htmltext = "31751-05.htm";
						break;
				}
				break;
			case 31752 :
				switch (st.getCond())
				{
					case 4:
						npc.doDie(npc);
						if (Rnd.chance(30))
						{
							st.giveItems(7590, 1);
							st.setCond(5, true);
							htmltext = "31752-01.htm";
						}
						else
						{
							htmltext = "31752-02.htm";
						}
						break;
					case 5:
						htmltext = "31752-02.htm";
						break;
				}
				break;
			case 30759 :
				switch (st.getCond())
				{
					case 6:
						htmltext = "30759-01.htm";
						break;
					case 7:
						htmltext = "30759-03.htm";
						break;
					case 8:
						if (st.hasQuestItems(7596))
						{
							htmltext = "30759-04.htm";
						}
						break;
					case 9:
						htmltext = "30759-06.htm";
						break;
				}
				break;
			case 30738 :
				switch (st.getCond())
				{
					case 7:
						htmltext = "30738-01.htm";
						break;
					case 8:
						htmltext = "30738-03.htm";
						break;
				}
				break;
			case 31748 :
				if (st.isCond(9))
				{
					if (st.hasQuestItems(7595))
					{
						htmltext = "31748-02.htm";
						st.takeItems(7595, 1);
						npc.doDie(npc);

						st.set("cornerstones", Integer.toString(st.getInt("cornerstones") + 1));
						if (st.getInt("cornerstones") == 4)
						{
							st.setCond(10);
						}
						st.playSound("ItemSound.quest_middle");
						npc.setTarget(player);
						npc.doCast(SkillsParser.getInstance().getInfo(4546, 1));
					}
					else
					{
						htmltext = "31748-01.htm";
					}
				}
				break;
			case 31746 :
				switch (st.getCond())
				{
					case 9:
						htmltext = "31746-01.htm";
						break;
					case 10:
						htmltext = "31746-02.htm";
						startQuestTimer("PURE_UNICORN", 3000, npc, player);
						break;
				}
				break;
			case 31747 :
				switch (st.getCond())
				{
					case 10:
						st.setCond(11, true);
						htmltext = "31747-01.htm";
						break;
					case 11:
						htmltext = "31747-02.htm";
						break;
				}
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, "awaitsDrops", "1");
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}

		final QuestState st = partyMember.getQuestState(getName());
		if (st != null && st.isCond(9) && st.calcDropItems(getId(), 7595, npc.getId(), 4))
		{
			st.unset("awaitsDrops");
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _242_PossessorOfAPreciousSoul_2(242, _242_PossessorOfAPreciousSoul_2.class.getSimpleName(), "");
	}
}
