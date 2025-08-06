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
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Rework by LordWinter 10.11.2021
 */
public class _228_TestOfMagus extends Quest
{
	public _228_TestOfMagus(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30629);
		addTalkId(30629, 30391, 30409, 30411, 30412, 30413, 30612);
		
		addKillId(20145, 20157, 20176, 20230, 20231, 20232, 20234, 20553, 20564, 20565, 20566, 27095, 27096, 27097, 27098);
		
		questItemIds = new int[]
		{
		        2841, 2842, 2843, 2844, 2845, 2846, 2847, 2848, 2849, 2850, 2851, 2852, 2853, 2854, 2855, 2856, 2857, 2858, 2859, 2860, 2861, 2862, 2863
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
					giveItems(player, 2841, 1);
					if (player.getVarInt("2ND_CLASS_DIAMOND_REWARD", 0) == 0)
					{
						qs.calcReward(getId(), 1);
						player.setVar("2ND_CLASS_DIAMOND_REWARD", 1);
						htmltext = "30629-04a.htm";
					}
					else
					{
						htmltext = "30629-04.htm";
					}
				}
				break;
			}
			case "30629-09.htm" :
			case "30409-02.htm" :
			{
				htmltext = event;
				break;
			}
			case "30629-10.htm" :
			{
				if (hasQuestItems(player, 2846))
				{
					takeItems(player, 2843, 1);
					takeItems(player, 2844, 1);
					takeItems(player, 2845, 1);
					takeItems(player, 2846, 1);
					giveItems(player, 2847, 1);
					qs.setCond(5, true);
					htmltext = event;
				}
				break;
			}
			case "30391-02.htm" :
			{
				if (hasQuestItems(player, 2841))
				{
					takeItems(player, 2841, 1);
					giveItems(player, 2842, 1);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "30409-03.htm" :
			{
				giveItems(player, 2863, 1);
				htmltext = event;
				break;
			}
			case "30412-02.htm" :
			{
				giveItems(player, 2861, 1);
				htmltext = event;
				break;
			}
			case "30612-02.htm" :
			{
				takeItems(player, 2842, 1);
				giveItems(player, 2843, 1);
				qs.setCond(3, true);
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			if (npc.getId() == 30629)
			{
				if ((player.getClassId() == ClassId.wizard) || (player.getClassId() == ClassId.elvenWizard) || ((player.getClassId() == ClassId.darkWizard)))
				{
					if (player.getLevel() < 39)
					{
						htmltext = "30629-02.htm";
					}
					else
					{
						htmltext = "30629-03.htm";
					}
				}
				else
				{
					htmltext = "30629-01.htm";
				}
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case 30629 :
				{
					if (hasQuestItems(player, 2841))
					{
						htmltext = "30629-05.htm";
					}
					else if (hasQuestItems(player, 2842))
					{
						htmltext = "30629-06.htm";
					}
					else if (hasQuestItems(player, 2843))
					{
						if (hasQuestItems(player, 2844, 2845, 2846))
						{
							htmltext = "30629-08.htm";
						}
						else
						{
							htmltext = "30629-07.htm";
						}
					}
					else if (hasQuestItems(player, 2847))
					{
						if (hasQuestItems(player, 2856, 2857, 2858, 2859))
						{
							qs.calcExpAndSp(getId());
							qs.calcReward(getId(), 2);
							qs.exitQuest(false, true);
							player.sendPacket(new SocialAction(player.getObjectId(), 3));
							htmltext = "30629-12.htm";
						}
						else
						{
							htmltext = "30629-11.htm";
						}
					}
					break;
				}
				case 30391 :
				{
					if (hasQuestItems(player, 2841))
					{
						htmltext = "30391-01.htm";
					}
					else if (hasQuestItems(player, 2842))
					{
						htmltext = "30391-03.htm";
					}
					else if (hasQuestItems(player, 2843))
					{
						htmltext = "30391-04.htm";
					}
					else if (hasQuestItems(player, 2847))
					{
						htmltext = "30391-05.htm";
					}
					break;
				}
				case 30409 :
				{
					if (hasQuestItems(player, 2847))
					{
						if (!hasAtLeastOneQuestItem(player, 2859, 2863))
						{
							htmltext = "30409-01.htm";
						}
						else if (hasQuestItems(player, 2863))
						{
							if ((getQuestItemsCount(player, 2853) >= 10) && (getQuestItemsCount(player, 2854) >= 10) && (getQuestItemsCount(player, 2855) >= 10))
							{
								takeItems(player, 2853, -1);
								takeItems(player, 2854, -1);
								takeItems(player, 2855, -1);
								giveItems(player, 2859, 1);
								takeItems(player, 2863, 1);
								if (hasQuestItems(player, 2857, 2856, 2858))
								{
									qs.setCond(6, true);
								}
								htmltext = "30409-05.htm";
							}
							else
							{
								htmltext = "30409-04.htm";
							}
						}
						else if (hasQuestItems(player, 2859) && !hasQuestItems(player, 2863))
						{
							htmltext = "30409-06.htm";
						}
					}
					break;
				}
				case 30411 :
				{
					if (hasQuestItems(player, 2847))
					{
						if (!hasAtLeastOneQuestItem(player, 2857, 2860))
						{
							htmltext = "30411-01.htm";
							giveItems(player, 2860, 1);
						}
						else if (hasQuestItems(player, 2860))
						{
							if (getQuestItemsCount(player, 2849) < 5)
							{
								htmltext = "30411-02.htm";
							}
							else
							{
								takeItems(player, 2849, -1);
								giveItems(player, 2857, 1);
								takeItems(player, 2860, 1);
								if (hasQuestItems(player, 2856, 2858, 2859))
								{
									qs.setCond(6, true);
								}
								htmltext = "30411-03.htm";
							}
						}
						else if (hasQuestItems(player, 2857) && !hasQuestItems(player, 2860))
						{
							htmltext = "30411-04.htm";
						}
					}
					break;
				}
				case 30412 :
				{
					if (hasQuestItems(player, 2847))
					{
						if (!hasAtLeastOneQuestItem(player, 2858, 2861))
						{
							htmltext = "30412-01.htm";
						}
						else if (hasQuestItems(player, 2861))
						{
							if ((getQuestItemsCount(player, 2850) >= 20) && (getQuestItemsCount(player, 2851) >= 10) && (getQuestItemsCount(player, 2852) >= 10))
							{
								takeItems(player, 2850, -1);
								takeItems(player, 2851, -1);
								takeItems(player, 2852, -1);
								giveItems(player, 2858, 1);
								takeItems(player, 2861, 1);
								if (hasQuestItems(player, 2856, 2857, 2859))
								{
									qs.setCond(6, true);
								}
								htmltext = "30412-04.htm";
							}
							else
							{
								htmltext = "30412-03.htm";
							}
						}
						else if (hasQuestItems(player, 2858) && !hasQuestItems(player, 2861))
						{
							htmltext = "30412-05.htm";
						}
					}
					break;
				}
				case 30413 :
				{
					if (hasQuestItems(player, 2847))
					{
						if (!hasAtLeastOneQuestItem(player, 2856, 2862))
						{
							htmltext = "30413-01.htm";
							giveItems(player, 2862, 1);
						}
						else if (hasQuestItems(player, 2862))
						{
							if (getQuestItemsCount(player, 2848) < 20)
							{
								htmltext = "30413-02.htm";
							}
							else
							{
								takeItems(player, 2848, -1);
								giveItems(player, 2856, 1);
								takeItems(player, 2862, 1);
								if (hasQuestItems(player, 2857, 2858, 2859))
								{
									qs.setCond(6, true);
								}
								htmltext = "30413-03.htm";
							}
						}
						else if (hasQuestItems(player, 2856) && !hasQuestItems(player, 2862))
						{
							htmltext = "30413-04.htm";
						}
					}
					break;
				}
				case 30612 :
				{
					if (hasQuestItems(player, 2842))
					{
						htmltext = "30612-01.htm";
					}
					else if (hasQuestItems(player, 2843))
					{
						if (hasQuestItems(player, 2844, 2845, 2846))
						{
							htmltext = "30612-04.htm";
						}
						else
						{
							htmltext = "30612-03.htm";
						}
					}
					else if (hasQuestItems(player, 2847))
					{
						htmltext = "30612-05.htm";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == 30629)
			{
				htmltext = getAlreadyCompletedMsg(player);
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
				case 20145 :
				{
					if (hasQuestItems(killer, 2847, 2861))
					{
						qs.calcDropItems(getId(), 2850, npc.getId(), 20);
					}
					break;
				}
				case 20157 :
				case 20230 :
				case 20231 :
				case 20232 :
				case 20234 :
				{
					if (hasQuestItems(killer, 2847, 2862))
					{
						qs.calcDropItems(getId(), 2848, npc.getId(), 20);
					}
					break;
				}
				case 20176 :
				{
					if (hasQuestItems(killer, 2847, 2861))
					{
						qs.calcDropItems(getId(), 2851, npc.getId(), 10);
					}
					break;
				}
				case 20553 :
				{
					if (hasQuestItems(killer, 2847, 2861))
					{
						qs.calcDropItems(getId(), 2852, npc.getId(), 10);
					}
					break;
				}
				case 20564 :
				{
					if (hasQuestItems(killer, 2847, 2863))
					{
						qs.calcDropItems(getId(), 2853, npc.getId(), 10);
					}
					break;
				}
				case 20565 :
				{
					if (hasQuestItems(killer, 2847, 2863))
					{
						qs.calcDropItems(getId(), 2854, npc.getId(), 10);
					}
					break;
				}
				case 20566 :
				{
					if (hasQuestItems(killer, 2847, 2863))
					{
						qs.calcDropItems(getId(), 2855, npc.getId(), 10);
					}
					break;
				}
				case 27095 :
				{
					if (hasQuestItems(killer, 2843) && !hasQuestItems(killer, 2844))
					{
						giveItems(killer, 2844, 1);
						npc.broadcastPacketToOthers(1000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.I_AM_A_TREE_OF_NOTHING_A_TREE_THAT_KNOWS_WHERE_TO_RETURN));
						playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						if (hasQuestItems(killer, 2845, 2846))
						{
							qs.setCond(4);
						}
					}
					break;
				}
				case 27096 :
				{
					if (hasQuestItems(killer, 2843) && !hasQuestItems(killer, 2845))
					{
						giveItems(killer, 2845, 1);
						npc.broadcastPacketToOthers(1000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.I_AM_A_CREATURE_THAT_SHOWS_THE_TRUTH_OF_THE_PLACE_DEEP_IN_MY_HEART));
						playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						if (hasQuestItems(killer, 2844, 2846))
						{
							qs.setCond(4);
						}
					}
					break;
				}
				case 27097 :
				{
					if (hasQuestItems(killer, 2843) && !hasQuestItems(killer, 2846))
					{
						giveItems(killer, 2846, 1);
						npc.broadcastPacketToOthers(1000, new NpcSay(npc, Say2.NPC_ALL, NpcStringId.I_AM_A_MIRROR_OF_DARKNESS_A_VIRTUAL_IMAGE_OF_DARKNESS));
						playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						if (hasQuestItems(killer, 2844, 2845))
						{
							qs.setCond(4);
						}
					}
					break;
				}
				case 27098 :
				{
					if (hasQuestItems(killer, 2847, 2860))
					{
						qs.calcDropItems(getId(), 2849, npc.getId(), 5);
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _228_TestOfMagus(228, _228_TestOfMagus.class.getSimpleName(), "");
	}
}