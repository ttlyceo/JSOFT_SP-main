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
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.serverpackets.SocialAction;

public class _401_PathToWarrior extends Quest
{
	// Item
	private static final int AURONSLETTER = 1138;
	private static final int WARRIORGUILDMARK = 1139;
	private static final int RUSTEDBRONZESWORD1 = 1140;
	private static final int RUSTEDBRONZESWORD2 = 1141;
	private static final int RUSTEDBRONZESWORD3 = 1142;
	private static final int SIMPLONSLETTER = 1143;
	private static final int POISONSPIDERLEG = 1144;
	private static final int MEDALLIONOFWARRIOR = 1145;

	// Npc
	private static final int AURON = 30010;
	private static final int SIMPLON = 30253;
	private static final int[] MONSTERS =
	{
	                20035,
	                20038,
	                20042,
	                20043
	};

	public _401_PathToWarrior(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(AURON);
		addTalkId(AURON);
		addTalkId(SIMPLON);
		for (final int i : MONSTERS)
		{
			addKillId(i);
		}

		questItemIds = new int[]
		{
		                AURONSLETTER, WARRIORGUILDMARK, RUSTEDBRONZESWORD1, RUSTEDBRONZESWORD2, RUSTEDBRONZESWORD3, SIMPLONSLETTER, POISONSPIDERLEG
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());

		if (st == null || st.isCompleted())
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("401_1"))
		{
			switch (player.getClassId())
			{
				case fighter:
				{
					if (player.getLevel() >= 18)
					{
						if (st.getQuestItemsCount(MEDALLIONOFWARRIOR) == 1)
						{
							htmltext = "30010-04.htm";
						}
						else
						{
							htmltext = "30010-05.htm";
						}
					}
					else
					{
						htmltext = "30010-02.htm";
					}
					break;
				}
				case warrior:
				{
					htmltext = "30010-03.htm";
					break;
				}
				default:
				{
					htmltext = "30010-02b.htm";
				}
			}
		}
		else if (event.equalsIgnoreCase("401_accept"))
		{
			st.startQuest();
			st.giveItems(AURONSLETTER, 1);
			htmltext = "30010-06.htm";
		}
		else if (event.equalsIgnoreCase("30253_1"))
		{
			st.setCond(2, true);
			st.takeItems(AURONSLETTER, 1);
			st.giveItems(WARRIORGUILDMARK, 1);
			htmltext = "30253-02.html";
		}
		else if (event.equalsIgnoreCase("401_2"))
		{
			htmltext = "30010-10.html";
		}
		else if (event.equalsIgnoreCase("401_3"))
		{
			st.setCond(5, true);
			st.takeItems(RUSTEDBRONZESWORD2, 1);
			st.giveItems(RUSTEDBRONZESWORD3, 1);
			st.takeItems(SIMPLONSLETTER, 1);
			htmltext = "30010-11.html";
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

		switch (npc.getId())
		{
			case AURON:
			{
				switch (st.getCond())
				{
					case 0:
					{
						htmltext = "30010-01.htm";
						break;
					}
					case 1:
					{
						htmltext = "30010-07.html";
						break;
					}
					case 2:
					case 3:
					{
						htmltext = "30010-08.html";
						break;
					}
					case 4:
					{
						htmltext = "30010-09.html";
						break;
					}
					case 5:
					{
						htmltext = "30010-12.html";
						break;
					}
					case 6:
					{
						st.takeItems(RUSTEDBRONZESWORD3, 1);
						st.takeItems(POISONSPIDERLEG, -1);
						final String done = st.getGlobalQuestVar("1ClassQuestFinished");
						if (done == null || done.isEmpty())
						{
							st.addExpAndSp(228064, 16455);
							st.rewardItems(57, 163800);
							st.saveGlobalQuestVar("1ClassQuestFinished", "1");
						}
						st.giveItems(MEDALLIONOFWARRIOR, 1);
						player.sendPacket(new SocialAction(player.getObjectId(), 3));
						st.exitQuest(false, true);
						htmltext = "30010-13.html";
						break;
					}
				}
				break;
			}
			case SIMPLON:
			{
				switch (st.getCond())
				{
					case 1:
					{
						htmltext = "30253-01.html";
						break;
					}
					case 2:
					{
						htmltext = "30253-03.html";
						break;
					}
					case 3:
					{
						st.setCond(4, true);
						st.takeItems(WARRIORGUILDMARK, 1);
						st.takeItems(RUSTEDBRONZESWORD1, 10);
						st.giveItems(RUSTEDBRONZESWORD2, 1);
						st.giveItems(SIMPLONSLETTER, 1);
						htmltext = "30253-04.html";
						break;
					}
					case 4:
					{
						htmltext = "30253-05.html";
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState st = killer.getQuestState(getName());
		if (st == null)
		{
			return null;
		}

		switch (st.getCond())
		{
			case 2:
			{
				if ((npc.getId() == MONSTERS[0]) || (npc.getId() == MONSTERS[2]))
				{
					if (st.getQuestItemsCount(RUSTEDBRONZESWORD1) < 10)
					{
						if (getRandom(10) < 4)
						{
							st.giveItems(RUSTEDBRONZESWORD1, 1);
							st.playSound("ItemSound.quest_itemget");
						}
					}
					if (st.getQuestItemsCount(RUSTEDBRONZESWORD1) == 10)
					{
						st.setCond(3, true);
					}
				}
				break;
			}
			case 5:
			{
				if ((st.getItemEquipped(Inventory.PAPERDOLL_RHAND) == RUSTEDBRONZESWORD3) && ((npc.getId() == MONSTERS[1]) || (npc.getId() == MONSTERS[3])))
				{
					if (st.getQuestItemsCount(POISONSPIDERLEG) < 20)
					{
						st.giveItems(POISONSPIDERLEG, 1);
						st.playSound("ItemSound.quest_itemget");
					}

					if (st.getQuestItemsCount(POISONSPIDERLEG) == 20)
					{
						st.setCond(6, true);
					}
				}
			}
		}

		return super.onKill(npc, killer, isSummon);
	}

	public static void main(String[] args)
	{
		new _401_PathToWarrior(401, _401_PathToWarrior.class.getSimpleName(), "");
	}
}
