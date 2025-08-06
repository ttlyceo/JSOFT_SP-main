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

import l2e.commons.util.Util;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Rework by LordWinter 29.12.2021
 */
public final class _063_PathOfTheWarder extends Quest
{
	public _063_PathOfTheWarder(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32195);
		addTalkId(32195, 32198, 30297, 30332);
		
		addKillId(20053, 20782, 20919, 20920, 20921, 27337);
		
		questItemIds = new int[]
		{
		        9762, 9763, 9764, 9765, 9766, 9767, 9768, 9769, 9770, 9771
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null || qs.isCompleted())
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "ACCEPT" :
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					qs.setMemoState(1);
					htmltext = "32195-05.htm";
				}
				break;
			}
			case "32195-06.htm" :
			{
				if (qs.isMemoState(1))
				{
					htmltext = event;
				}
				break;
			}
			case "32195-08.htm" :
			{
				if (qs.isMemoState(1))
				{
					qs.setMemoState(2);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "32198-03.htm" :
			{
				if (qs.isMemoState(3))
				{
					takeItems(player, 9764, 1);
					giveItems(player, 9765, 1);
					qs.setMemoState(4);
					qs.setCond(5, true);
					htmltext = event;
				}
				break;
			}
			case "32198-07.htm" :
			{
				if (qs.isMemoState(7))
				{
					htmltext = event;
				}
				break;
			}
			case "32198-08.htm" :
			{
				if (qs.isMemoState(7))
				{
					giveItems(player, 9767, 1);
					qs.setMemoState(8);
					qs.setCond(7, true);
					htmltext = event;
				}
				break;
			}
			case "32198-12.htm" :
			{
				if (qs.isMemoState(12))
				{
					giveItems(player, 9769, 1);
					qs.setMemoState(13);
					qs.setCond(9, true);
					htmltext = event;
				}
				break;
			}
			case "32198-15.htm" :
			{
				if (qs.isMemoState(14))
				{
					qs.setMemoState(15);
					htmltext = event;
				}
				break;
			}
			case "32198-16.htm" :
			{
				if (qs.isMemoState(15))
				{
					giveItems(player, 9770, 1);
					qs.setMemoState(16);
					qs.set("ex", 0);
					qs.setCond(11, true);
					htmltext = event;
				}
				break;
			}
			case "30332-03.htm" :
			{
				if (qs.isMemoState(4))
				{
					takeItems(player, 9765, 1);
					giveItems(player, 9766, 1);
					qs.setMemoState(5);
					htmltext = event;
				}
				break;
			}
			case "30332-05.htm" :
			{
				if (qs.isMemoState(5))
				{
					htmltext = event;
				}
				break;
			}
			case "30332-06.htm" :
			{
				if (qs.isMemoState(5))
				{
					qs.setMemoState(6);
					qs.setCond(6, true);
					htmltext = event;
				}
				break;
			}
			case "30297-03.htm" :
			{
				if (qs.isMemoState(9))
				{
					htmltext = event;
				}
				break;
			}
			case "30297-04.htm" :
			{
				if (qs.isMemoState(9))
				{
					qs.setMemoState(10);
					htmltext = event;
				}
				break;
			}
			case "30297-06.htm" :
			{
				if (qs.isMemoState(10))
				{
					giveItems(player, 9768, 1);
					qs.setMemoState(11);
					qs.setCond(8, true);
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
		final QuestState qs = getQuestState(player, true);
		final int memoState = qs.getMemoState();
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			if (npc.getId() == 32195)
			{
				if ((player.getClassId() == ClassId.femaleSoldier) && !hasQuestItems(player, 9772))
				{
					if (player.getLevel() >= getMinLvl(getId()))
					{
						htmltext = "32195-01.htm";
					}
					else
					{
						htmltext = "32195-02.htm";
					}
				}
				else if (hasQuestItems(player, 9772))
				{
					htmltext = "32195-03.htm";
				}
				else
				{
					htmltext = "32195-04.htm";
				}
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case 32195 :
				{
					if (memoState == 1)
					{
						htmltext = "32195-07.htm";
					}
					else if (memoState == 2)
					{
						if ((getQuestItemsCount(player, 9762) < 10) || (getQuestItemsCount(player, 9763) < 5))
						{
							htmltext = "32195-09.htm";
						}
						else
						{
							takeItems(player, 9762, -1);
							takeItems(player, 9763, -1);
							giveItems(player, 9764, 1);
							qs.setMemoState(3);
							qs.setCond(4, true);
							htmltext = "32195-10.htm";
						}
					}
					else if (memoState == 3)
					{
						htmltext = "32195-11.htm";
					}
					else if ((memoState > 3) && (memoState != 13))
					{
						htmltext = "32195-12.htm";
					}
					else if (memoState == 13)
					{
						takeItems(player, 9769, 1);
						qs.setMemoState(14);
						qs.setCond(10, true);
						htmltext = "32195-13.htm";
					}
					break;
				}
				case 32198 :
				{
					if (memoState < 3)
					{
						htmltext = "32198-01.htm";
					}
					else if (memoState == 3)
					{
						htmltext = "32198-02.htm";
					}
					else if ((memoState == 4) || (memoState == 5))
					{
						htmltext = "32198-04.htm";
					}
					else if (memoState == 6)
					{
						takeItems(player, 9766, 1);
						qs.setMemoState(7);
						htmltext = "32198-05.htm";
					}
					else if (memoState == 7)
					{
						htmltext = "32198-06.htm";
					}
					else if (memoState == 8)
					{
						htmltext = "32198-09.htm";
					}
					else if (memoState == 11)
					{
						takeItems(player, 9768, 1);
						qs.setMemoState(12);
						htmltext = "32198-10.htm";
					}
					else if (memoState == 12)
					{
						giveItems(player, 9769, 1);
						qs.setMemoState(13);
						htmltext = "32198-11.htm";
					}
					else if (memoState == 13)
					{
						htmltext = "32198-13.htm";
					}
					else if (memoState == 14)
					{
						htmltext = "32198-14.htm";
					}
					else if (memoState == 15)
					{
						giveItems(player, 9770, 1);
						qs.setMemoState(16);
						qs.set("ex", 0);
						qs.setCond(11, true);
						htmltext = "32198-17.htm";
					}
					else if (memoState == 16)
					{
						if (!hasQuestItems(player, 9771))
						{
							qs.set("ex", 0);
							htmltext = "32198-18.htm";
						}
						else
						{
							takeItems(player, 9771, 1);
							qs.calcExpAndSp(getId());
							qs.calcReward(getId());
							qs.exitQuest(false, true);
							player.sendPacket(new SocialAction(player.getObjectId(), 3));
							qs.saveGlobalQuestVar("1ClassQuestFinished", "1");
							htmltext = "32198-19.htm";
						}
					}
					break;
				}
				case 30332 :
				{
					if (memoState == 4)
					{
						htmltext = "30332-01.htm";
					}
					else if (memoState < 4)
					{
						htmltext = "30332-02.htm";
					}
					else if (memoState == 5)
					{
						htmltext = "30332-04.htm";
					}
					else if (memoState == 6)
					{
						htmltext = "30332-07.htm";
					}
					else if (memoState > 6)
					{
						htmltext = "30332-08.htm";
					}
					break;
				}
				case 30297 :
				{
					if (memoState == 8)
					{
						takeItems(player, 9767, 1);
						qs.setMemoState(9);
						htmltext = "30297-01.htm";
					}
					else if (memoState == 9)
					{
						htmltext = "30297-02.htm";
					}
					else if (memoState == 10)
					{
						giveItems(player, 9768, 1);
						qs.setMemoState(11);
						qs.setCond(8, true);
						htmltext = "30297-05.htm";
					}
					else if (memoState >= 11)
					{
						htmltext = "30297-07.htm";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == 32198)
			{
				if (hasQuestItems(player, 9772))
				{
					htmltext = "32198-20.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isStarted() && Util.checkIfInRange(1500, npc, killer, true))
		{
			switch (npc.getId())
			{
				case 20053 :
				{
					if (qs.isMemoState(2))
					{
						if (qs.calcDropItems(getId(), 9763, npc.getId(), 5) && (getQuestItemsCount(killer, 9762) >= 10))
						{
							qs.setCond(3, true);
						}
					}
					break;
				}
				case 20782 :
				{
					if (qs.isMemoState(2))
					{
						if (qs.calcDropItems(getId(), 9762, npc.getId(), 10) && (getQuestItemsCount(killer, 9763) >= 5))
						{
							qs.setCond(3, true);
						}
					}
					break;
				}
				case 20919 :
				case 20920 :
				case 20921 :
				{
					if (qs.isMemoState(16) && !hasQuestItems(killer, 9771))
					{
						final int i4 = qs.getInt("ex");
						if (i4 < 4)
						{
							qs.set("ex", i4 + 1);
						}
						else
						{
							qs.set("ex", 0);
							final Attackable monster = (Attackable) addSpawn(27337, npc.getLocation(), true, 0, false);
							if ((monster != null) && (killer != null))
							{
								monster.setIsRunning(true);
								monster.addDamageHate(killer, 0, 999);
								monster.getAI().setIntention(CtrlIntention.ATTACK, killer);
							}
						}
					}
					break;
				}
				case 27337 :
				{
					if (qs.isMemoState(16) && !hasQuestItems(killer, 9771))
					{
						takeItems(killer, 9770, 1);
						giveItems(killer, 9771, 1);
						qs.setCond(12, true);
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _063_PathOfTheWarder(63, _063_PathOfTheWarder.class.getSimpleName(), "");
	}
}