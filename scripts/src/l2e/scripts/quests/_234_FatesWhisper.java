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
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Updated by LordWinter 30.07.2020
 */
public class _234_FatesWhisper extends Quest
{
	public _234_FatesWhisper(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31002);
		addTalkId(30178, 30182, 30833, 30847, 31002);
		addTalkId(31027, 31028, 31030, 31029);
		
		addAttackId(29020);
		
		addKillId(20823, 20826, 20827, 20828, 20829, 20830, 20831, 20860);
		
		questItemIds = new int[]
		{
		        14361, 14362, 4665, 4666, 4667, 4668, 4669, 4670, 4671, 4672, 4673
		};
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		final String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case 30178 :
			{
				switch (qs.getMemoState())
				{
					case 6 :
						return "30178-01.htm";
					case 7 :
						return "30178-03.htm";
					case 8 :
						return "30178-04.htm";
				}
				break;
			}
			case 30182 :
			{
				if (qs.isMemoState(4) && !qs.hasQuestItems(4672))
				{
					return "30182-01.htm";
				}
				if (qs.isMemoState(4) && qs.hasQuestItems(4672))
				{
					return "30182-05.htm";
				}
				if (qs.getMemoState() >= 5)
				{
					return "30182-06.htm";
				}
			}
			case 30833 :
			{
				if (qs.isMemoState(7))
				{
					return "30833-01.htm";
				}
				
				final long bloodyFabricCount = qs.getQuestItemsCount(14361);
				final long whiteFabricCount = qs.getQuestItemsCount(14362);
				final long whiteBloodyFabricCount = bloodyFabricCount + whiteFabricCount;
				if (qs.isMemoState(8) && !qs.hasQuestItems(4673) && (whiteBloodyFabricCount <= 0))
				{
					return "30833-03.htm";
				}
				if (qs.isMemoState(8) && qs.hasQuestItems(4673) && (whiteBloodyFabricCount <= 0))
				{
					qs.giveItems(4671, 1);
					qs.takeItems(4673, 1);
					qs.setMemoState(9);
					qs.setCond(10, true);
					qs.showQuestionMark(getId(), 1);
					return "30833-04.htm";
				}
				if (qs.isMemoState(8) && !qs.hasQuestItems(4673) && (bloodyFabricCount < 30) && (whiteBloodyFabricCount >= 30))
				{
					return "30833-03c.htm";
				}
				if (qs.isMemoState(8) && !qs.hasQuestItems(4673) && (bloodyFabricCount >= 30) && (whiteBloodyFabricCount >= 30))
				{
					qs.giveItems(4671, 1);
					qs.takeItems(14361, -1);
					qs.setMemoState(9);
					qs.setCond(10, true);
					qs.showQuestionMark(getId(), 1);
					return "30833-03d.htm";
				}
				if (qs.isMemoState(8) && !qs.hasQuestItems(4673) && (whiteBloodyFabricCount < 30) && (whiteBloodyFabricCount > 0))
				{
					qs.giveItems(14362, 30 - whiteFabricCount);
					qs.takeItems(14361, -1);
					return "30833-03e.htm";
				}
				if (qs.getMemoState() >= 9)
				{
					return "30833-05.htm";
				}
				break;
			}
			case 30847 :
			{
				if (qs.isMemoState(5))
				{
					if (qs.hasQuestItems(4670))
					{
						return "30847-02.htm";
					}
					qs.giveItems(4670, 1);
					return "30847-01.htm";
				}
				if (qs.getMemoState() >= 6)
				{
					return "30847-03.htm";
				}
				break;
			}
			case 31002 :
			{
				if (qs.isCreated() && (player.getLevel() >= 75))
				{
					return "31002-01.htm";
				}
				if (qs.isCreated() && (player.getLevel() < 75))
				{
					return "31002-01a.htm";
				}
				if (qs.isCompleted())
				{
					return getAlreadyCompletedMsg(player);
				}
				if (qs.isMemoState(1) && !qs.hasQuestItems(4666))
				{
					return "31002-09.htm";
				}
				if (qs.isMemoState(1) && qs.hasQuestItems(4666))
				{
					return "31002-10.htm";
				}
				if (qs.isMemoState(2) && !qs.hasQuestItems(4667, 4668, 4669))
				{
					return "31002-12.htm";
				}
				if (qs.isMemoState(2) && qs.hasQuestItems(4667, 4668, 4669))
				{
					return "31002-13.htm";
				}
				if (qs.isMemoState(4) && !qs.hasQuestItems(4672))
				{
					return "31002-15.htm";
				}
				if (qs.isMemoState(4) && qs.hasQuestItems(4672))
				{
					return "31002-16.htm";
				}
				if (qs.isMemoState(5) && !qs.hasQuestItems(4670))
				{
					return "31002-18.htm";
				}
				if (qs.isMemoState(5) && qs.hasQuestItems(4670))
				{
					return "31002-19.htm";
				}
				if ((qs.getMemoState() < 9) && (qs.getMemoState() >= 6))
				{
					return "31002-21.htm";
				}
				if (qs.isMemoState(9) && qs.hasQuestItems(4671))
				{
					return "31002-22.htm";
				}
				if (qs.isMemoState(10) && (qs.getQuestItemsCount(1460) < 984))
				{
					return "31002-24.htm";
				}
				if (qs.isMemoState(10) && (qs.getQuestItemsCount(1460) >= 984))
				{
					return "31002-25.htm";
				}
				switch (qs.getMemoState())
				{
					case 11 :
						if (hasAtLeastOneQuestItem(player, 79, 4717, 4718, 4719))
						{
							return "31002-35.htm";
						}
						return "31002-35a.htm";
					case 12 :
						if (hasAtLeastOneQuestItem(player, 4828, 4829, 4830, 287))
						{
							return "31002-36.htm";
						}
						return "31002-36a.htm";
					case 13 :
						if (hasAtLeastOneQuestItem(player, 4858, 4859, 4860, 97))
						{
							return "31002-37.htm";
						}
						return "31002-37a.htm";
					case 14 :
						if (hasAtLeastOneQuestItem(player, 4753, 4754, 4755, 175))
						{
							return "31002-38.htm";
						}
						return "31002-38a.htm";
					case 15 :
						if (hasAtLeastOneQuestItem(player, 4900, 4901, 4902, 210))
						{
							return "31002-39.htm";
						}
						return "31002-39a.htm";
					case 16 :
						if (hasAtLeastOneQuestItem(player, 4780, 4781, 4782, 234))
						{
							return "31002-40.htm";
						}
						return "31002-40a.htm";
					case 17 :
						if (hasAtLeastOneQuestItem(player, 4804, 4805, 4806, 268))
						{
							return "31002-41.htm";
						}
						return "31002-41a.htm";
					case 18 :
						if (hasAtLeastOneQuestItem(player, 4750, 4751, 4752, 171))
						{
							return "31002-42.htm";
						}
						return "31002-42a.htm";
					case 19 :
						if (hasAtLeastOneQuestItem(player, 2626))
						{
							return "31002-43.htm";
						}
						return "31002-43a.htm";
					case 41 :
						if (hasAtLeastOneQuestItem(player, 7883, 8105, 8106, 8107))
						{
							return "31002-43b.htm";
						}
						return "31002-43c.htm";
					case 42 :
						if (hasAtLeastOneQuestItem(player, 7889, 8117, 8118, 8119))
						{
							return "31002-43d.htm";
						}
						return "31002-43e.htm";
					case 43 :
						if (hasAtLeastOneQuestItem(player, 7901, 8132, 8133, 8134))
						{
							return "31002-43f.htm";
						}
						return "31002-43g.htm";
					case 44 :
						if (hasAtLeastOneQuestItem(player, 7893, 8144, 8145, 8146))
						{
							return "31002-43h.htm";
						}
						return "31002-43i.htm";
				}
				break;
			}
			case 31027 :
			{
				if (qs.isMemoState(1) && !qs.hasQuestItems(4666))
				{
					qs.giveItems(4666, 1);
					qs.playSound(QuestSound.ITEMSOUND_QUEST_ITEMGET);
					return "31027-01.htm";
				}
				if ((qs.getMemoState() > 1) || qs.hasQuestItems(4666))
				{
					return "31027-02.htm";
				}
				break;
			}
			case 31028 :
			{
				if (qs.isMemoState(2) && !qs.hasQuestItems(4667))
				{
					qs.giveItems(4667, 1);
					qs.playSound(QuestSound.ITEMSOUND_QUEST_ITEMGET);
					return "31028-01.htm";
				}
				if (!qs.isMemoState(2) || qs.hasQuestItems(4667))
				{
					return "31028-02.htm";
				}
				break;
			}
			case 31029 :
			{
				if (qs.isMemoState(2) && !qs.hasQuestItems(4668))
				{
					qs.giveItems(4668, 1);
					qs.playSound(QuestSound.ITEMSOUND_QUEST_ITEMGET);
					return "31029-01.htm";
				}
				if (!qs.isMemoState(2) || qs.hasQuestItems(4668))
				{
					return "31029-02.htm";
				}
				break;
			}
			case 31030 :
			{
				if (qs.isMemoState(2) && !qs.hasQuestItems(4669))
				{
					qs.giveItems(4669, 1);
					qs.playSound(QuestSound.ITEMSOUND_QUEST_ITEMGET);
					return "31030-01.htm";
				}
				if (!qs.isMemoState(2) || qs.hasQuestItems(4669))
				{
					return "31030-02.htm";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (player == null)
		{
			if (event.equals("23401") || event.equals("23402") || event.equals("23403") || event.equals("23404"))
			{
				npc.decayMe();
			}
			return super.onAdvEvent(event, npc, player);
		}
		
		final QuestState qs = getQuestState(player, false);
		if (qs == null || qs.isCompleted())
		{
			return null;
		}
		
		final String htmltext = null;
		
		if (event.equals("QUEST_ACCEPTED"))
		{
			qs.setMemoState(1);
			qs.startQuest();
			qs.showQuestionMark(getId(), 1);
			qs.playSound(QuestSound.ITEMSOUND_QUEST_ACCEPT);
			return "31002-06.htm";
		}
		if (event.contains(".htm"))
		{
			return event;
		}
		
		final int npcId = npc.getId();
		final int eventID = Integer.parseInt(event);
		
		switch (npcId)
		{
			case 30178 :
			{
				switch (eventID)
				{
					case 1 :
					{
						qs.setMemoState(7);
						qs.setCond(6);
						qs.showQuestionMark(getId(), 1);
						qs.playSound(QuestSound.ITEMSOUND_QUEST_MIDDLE);
						return "30178-02.htm";
					}
				}
			}
			case 30182 :
			{
				switch (eventID)
				{
					case 1 :
					{
						return "30182-02.htm";
					}
					case 2 :
					{
						return "30182-03.htm";
					}
					case 3 :
					{
						if (qs.isMemoState(4) && !qs.hasQuestItems(4672))
						{
							qs.giveItems(4672, 1);
							return "30182-04.htm";
						}
					}
				}
				break;
			}
			case 30833 :
			{
				switch (eventID)
				{
					case 1 :
					{
						if (qs.isMemoState(7))
						{
							return "30833-02.htm";
						}
						break;
					}
					case 2 :
					{
						if (qs.isMemoState(7))
						{
							qs.giveItems(4665, 1);
							qs.setMemoState(8);
							qs.setCond(7, true);
							qs.showQuestionMark(getId(), 1);
							return "30833-03a.htm";
						}
						break;
					}
					case 3 :
					{
						if (qs.isMemoState(7))
						{
							qs.giveItems(14362, 30);
							qs.setMemoState(8);
							qs.setCond(8, true);
							qs.showQuestionMark(getId(), 1);
							return "30833-03b.htm";
						}
						break;
					}
				}
				break;
			}
			case 31002 :
			{
				switch (eventID)
				{
					case 1 :
						return "31002-02.htm";
					case 2 :
						return "31002-03.htm";
					case 3 :
						return "31002-04.htm";
					case 4 :
					{
						if (!qs.isCompleted() && (player.getLevel() >= 75))
						{
							return "31002-05.htm";
						}
						break;
					}
					case 5 :
					{
						if (qs.isMemoState(1) && qs.hasQuestItems(4666))
						{
							qs.takeItems(4666, 1);
							qs.setMemoState(2);
							qs.setCond(2, true);
							qs.showQuestionMark(getId(), 1);
							return "31002-11.htm";
						}
						break;
					}
					case 6 :
					{
						if (qs.isMemoState(2) && qs.hasQuestItems(4667, 4668, 4669))
						{
							qs.takeItems(4667, -1);
							qs.takeItems(4668, -1);
							qs.takeItems(4669, -1);
							qs.setMemoState(4);
							qs.setCond(3, true);
							qs.showQuestionMark(getId(), 1);
							return "31002-14.htm";
						}
						break;
					}
					case 7 :
					{
						if (qs.isMemoState(4) && qs.hasQuestItems(4672))
						{
							qs.takeItems(4672, 1);
							qs.setMemoState(5);
							qs.setCond(4, true);
							qs.showQuestionMark(getId(), 1);
							return "31002-17.htm";
						}
						break;
					}
					case 8 :
					{
						if (qs.isMemoState(5) && qs.hasQuestItems(4670))
						{
							qs.takeItems(4670, 1);
							qs.setMemoState(6);
							qs.setCond(5, true);
							qs.showQuestionMark(getId(), 1);
							return "31002-20.htm";
						}
						break;
					}
					case 9 :
					{
						if (qs.isMemoState(9) && qs.hasQuestItems(4671))
						{
							qs.takeItems(4671, 1);
							qs.setMemoState(10);
							qs.setCond(11, true);
							qs.showQuestionMark(getId(), 1);
							return "31002-23.htm";
						}
						break;
					}
					case 10 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(11);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-26.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 11 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(19);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-26a.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 12 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(12);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-27.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 13 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(13);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-28.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 14 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(14);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-29.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 15 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(15);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-30.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 16 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(16);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-31.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 17 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(17);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-32.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 18 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(18);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-33.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 41 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(41);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-33a.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 42 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(42);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-33b.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 43 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(43);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-33c.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 44 :
					{
						if (qs.isMemoState(10))
						{
							if (qs.getQuestItemsCount(1460) >= 984)
							{
								qs.takeItems(1460, 984);
								qs.setMemoState(44);
								qs.setCond(12, true);
								qs.showQuestionMark(getId(), 1);
								return "31002-33d.htm";
							}
							return "31002-34.htm";
						}
						break;
					}
					case 21 :
					{
						if (calculateReward(qs, player, 80))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 22 :
					{
						if (calculateReward(qs, player, 288))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 23 :
					{
						if (calculateReward(qs, player, 98))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 24 :
					{
						if (calculateReward(qs, player, 150))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 25 :
					{
						if (calculateReward(qs, player, 212))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 26 :
					{
						if (calculateReward(qs, player, 235))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 27 :
					{
						if (calculateReward(qs, player, 269))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 28 :
					{
						if (calculateReward(qs, player, 2504))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 29 :
					{
						if (calculateReward(qs, player, 5233))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 30 :
					{
						if (calculateReward(qs, player, 7884))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 31 :
					{
						if (calculateReward(qs, player, 7894))
						{
							return "31002-44.htm";
						}
						break;
					}
					case 32 :
					{
						if (calculateReward(qs, player, 7899))
						{
							return "31002-44.htm";
						}
						break;
					}
				}
			}
		}
		return htmltext;
	}
	
	private boolean calculateReward(QuestState qs, Player player, int reward)
	{
		switch (qs.getMemoState())
		{
			case 11 :
				return getReward(qs, player, 79, 4717, 4718, 4719, reward);
			case 12 :
				return getReward(qs, player, 287, 4828, 4829, 4830, reward);
			case 13 :
				return getReward(qs, player, 97, 4858, 4859, 4860, reward);
			case 14 :
				return getReward(qs, player, 175, 4753, 4754, 4755, reward);
			case 15 :
				return getReward(qs, player, 210, 4900, 4901, 4902, reward);
			case 16 :
				return getReward(qs, player, 234, 4780, 4781, 4782, reward);
			case 17 :
				return getReward(qs, player, 268, 4804, 4805, 4806, reward);
			case 18 :
				return getReward(qs, player, 171, 4750, 4751, 4752, reward);
			case 19 :
				return getReward(qs, player, 2626, 0, 0, 0, reward);
			case 41 :
				return getReward(qs, player, 7883, 8105, 8106, 8107, reward);
			case 42 :
				return getReward(qs, player, 7889, 8117, 8118, 8119, reward);
			case 43 :
				return getReward(qs, player, 7901, 8132, 8133, 8134, reward);
			case 44 :
				return getReward(qs, player, 7893, 8144, 8145, 8146, reward);
		}
		return false;
	}
	
	private boolean getReward(QuestState qs, Player player, int item1, int item2, int item3, int item4, int reward)
	{
		if (hasAtLeastOneQuestItem(player, item1, item2, item3, item4))
		{
			qs.giveItems(reward, 1);
			qs.giveItems(5011, 1);
			if (qs.hasQuestItems(item1))
			{
				qs.takeItems(item1, 1);
			}
			else if (qs.hasQuestItems(item2))
			{
				qs.takeItems(item2, 1);
			}
			else if (qs.hasQuestItems(item3))
			{
				qs.takeItems(item3, 1);
			}
			else if (qs.hasQuestItems(item4))
			{
				qs.takeItems(item4, 1);
			}
			qs.exitQuest(false, true);
			player.broadcastSocialAction(3);
			return true;
		}
		return false;
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 2, npc);
		if (qs != null)
		{
			switch (npc.getId())
			{
				case 20823 :
				case 20826 :
				case 20827 :
				case 20828 :
				case 20829 :
				case 20830 :
				case 20831 :
				case 20860 :
				{
					giveItemRandomly(qs.getPlayer(), npc, 14361, 1, 0, 1, false);
					qs.takeItems(14362, 1);
					if (qs.getQuestItemsCount(14361) >= 29)
					{
						qs.setCond(9, true);
						qs.showQuestionMark(getId(), 1);
					}
					else
					{
						qs.playSound(QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
			}
			
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final QuestState qs = getQuestState(attacker, false);
		if ((qs != null) && (npc.getId() == 29020))
		{
			if ((attacker.getActiveWeaponItem() != null) && (attacker.getActiveWeaponItem().getId() == 4665))
			{
				qs.takeItems(4665, 1);
				qs.giveItems(4673, 1);
				qs.playSound(QuestSound.ITEMSOUND_QUEST_ITEMGET);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.WHO_DARES_TO_TRY_AND_STEAL_MY_NOBLE_BLOOD));
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public boolean checkPartyMember(QuestState qs, Npc npc)
	{
		return qs.hasQuestItems(14362) && qs.isMemoState(8);
	}
	
	public static void main(String[] args)
	{
		new _234_FatesWhisper(234, _234_FatesWhisper.class.getSimpleName(), "");
	}
}
