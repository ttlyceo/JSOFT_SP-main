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
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 09.10.2022
 */
public class _334_TheWishingPotion extends Quest
{
	public _334_TheWishingPotion(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(30738);
		addTalkId(30738, 30557, 30742, 30743);
		
		addSpawnId(27135, 27136, 27138, 27153, 27154, 27155, 30742);
		addKillId(20078, 20087, 20088, 20168, 20192, 20193, 20199, 20227, 20248, 20249, 27135, 20250, 27136, 27138, 27139, 27153, 27154, 27155);
		
		questItemIds = new int[]
		{
		        3679, 3684, 3686, 3687, 3688, 3689, 3690, 3691, 3467, 3680, 3681, 3682, 3678
		};
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final var qs = getQuestState(player, true);
		final var htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case 30557 :
			{
				if (hasQuestItems(player, 3683))
				{
					qs.calcReward(getId(), 8);
					takeItems(player, 3683, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					return "30557-01.htm";
				}
				break;
			}
			case 30738 :
			{
				if (qs.isCreated())
				{
					if (player.getLevel() < getMinLvl(getId()))
					{
						return "30738-01.htm";
					}
					return "30738-02.htm";
				}
				if (!hasQuestItems(player, 3679) && hasQuestItems(player, 3678))
				{
					return "30738-05.htm";
				}
				if (hasQuestItems(player, 3679) && hasQuestItems(player, 3678))
				{
					return "30738-06.htm";
				}
				if (hasQuestItems(player, 3680, 3681) && (!hasQuestItems(player, 3684) || (hasQuestItems(player, 3685) && !hasQuestItems(player, 3686)) || (!hasQuestItems(player, 3687) || !hasQuestItems(player, 3688) || !hasQuestItems(player, 3689) || !hasQuestItems(player, 3690) || !hasQuestItems(player, 3691))))
				{
					return "30738-08.htm";
				}
				if (hasQuestItems(player, 3680, 3681, 3684, 3685, 3686, 3687, 3688, 3689, 3690, 3691))
				{
					return "30738-09.htm";
				}
				if (hasQuestItems(player, 3682) && !hasQuestItems(player, 3680) && !hasQuestItems(player, 3681) && (!hasQuestItems(player, 3684) || (hasQuestItems(player, 3685) && !hasQuestItems(player, 3686)) || !hasQuestItems(player, 3687) || !hasQuestItems(player, 3688) || !hasQuestItems(player, 3689) || !hasQuestItems(player, 3690) || !hasQuestItems(player, 3691)))
				{
					return "30738-12.htm";
				}
				break;
			}
			case 30742 :
			{
				if (qs.getInt("flag") == 1)
				{
					String html = null;
					if ((getRandom(4) < 4))
					{
						qs.calcReward(getId(), 2);
						qs.set("flag", 0);
						html = "30742-01.htm";
					}
					else
					{
						qs.calcReward(getId(), 1, true);
						html = "30742-02.htm";
					}
					qs.set("flag", 0);
					npc.deleteMe();
					return html;
				}
				break;
			}
			case 30743 :
			{
				if (qs.getInt("flag") == 4)
				{
					final int random = getRandom(100);
					String html = null;
					if (random < 10)
					{
						qs.calcReward(getId(), 3);
						html = "30743-02.htm";
					}
					else if ((random >= 10) && (random < 50))
					{
						qs.calcReward(getId(), 1, true);
						html = "30743-03.htm";
					}
					else if ((random >= 50) && (random < 100))
					{
						qs.calcReward(getId(), 4, true);
						html = "30743-04.htm";
					}
					else if ((random >= 85) && (random < 95))
					{
						qs.calcReward(getId(), 5, true);
						html = "30743-05.htm";
					}
					else if (random >= 95)
					{
						qs.calcReward(getId(), 6, true);
						html = "30743-06.htm";
					}
					qs.set("flag", 0);
					npc.deleteMe();
					return html;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case 27135 :
			{
				startQuestTimer("2336002", 1000 * 200, npc, null);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.OH_OH_OH));
				break;
			}
			case 27136 :
			{
				startQuestTimer("2336003", 1000 * 200, npc, null);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.DO_YOU_WANT_US_TO_LOVE_YOU_OH));
				break;
			}
			case 27138 :
			{
				startQuestTimer("2336007", 1000 * 600, npc, null);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.WHO_KILLED_MY_UNDERLING_DEVIL));
				break;
			}
			case 27153 :
			{
				startQuestTimer("2336004", 1000 * 200, npc, null);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.WHO_IS_CALLING_THE_LORD_OF_DARKNESS));
				break;
			}
			case 27154 :
			{
				startQuestTimer("2336005", 1000 * 200, npc, null);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.I_AM_A_GREAT_EMPIRE_BONAPARTERIUS));
				break;
			}
			case 27155 :
			{
				startQuestTimer("2336006", 1000 * 200, npc, null);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.LET_YOUR_HEAD_DOWN_BEFORE_THE_LORD));
				break;
			}
			case 30742 :
			{
				startQuestTimer("2336001", 120 * 1000, npc, null);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.I_WILL_MAKE_YOUR_LOVE_COME_TRUE_LOVE_LOVE_LOVE));
				break;
			}
			case 30743 :
			{
				startQuestTimer("2336007", 120 * 1000, npc, null);
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.I_HAVE_WISDOM_IN_ME_I_AM_THE_BOX_OF_WISDOM));
				break;
			}
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (npc.getId())
		{
			case 30742 :
			case 27135 :
			case 27136 :
			case 27138 :
			{
				npc.deleteMe();
				break;
			}
			case 27153 :
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.OH_ITS_NOT_AN_OPPONENT_OF_MINE_HA_HA_HA));
				npc.deleteMe();
				break;
			}
			case 27154 :
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.OH_ITS_NOT_AN_OPPONENT_OF_MINE_HA_HA_HA));
				npc.deleteMe();
				break;
			}
			case 27155 :
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.OH_ITS_NOT_AN_OPPONENT_OF_MINE_HA_HA_HA));
				npc.deleteMe();
				break;
			}
			case 30743 :
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.DONT_INTERRUPT_MY_REST_AGAIN));
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.YOURE_A_GREAT_DEVIL_NOW));
				npc.deleteMe();
				break;
			}
			case 30738 :
			{
				final var qs = getQuestState(player, false);
				if (event.equals("QUEST_ACCEPTED"))
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ACCEPT);
					qs.startQuest();
					qs.setMemoState(1);
					qs.setCond(1);
					qs.showQuestionMark(334, 1);
					if (!hasQuestItems(player, 3678))
					{
						giveItems(player, 3678, 1);
					}
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					return "30738-04.htm";
				}
				
				switch (Integer.parseInt(event))
				{
					case 1:
					{
						return "30738-03.htm";
					}
					case 2:
					{
						takeItems(player, 3679, -1);
						takeItems(player, 3678, -1);
						giveItems(player, 3680, 1);
						giveItems(player, 3681, 1);
						qs.setMemoState(2);
						qs.setCond(3, true);
						qs.showQuestionMark(334, 1);
						return "30738-07.htm";
					}
					case 3:
					{
						return "30738-10.htm";
					}
					case 4:
					{
						if (hasQuestItems(player, 3684, 3686, 3687, 3688, 3689, 3690, 3691, 3685, 3680, 3681))
						{
							giveItems(player, 3467, 1);
							if (!hasQuestItems(player, 3682))
							{
								giveItems(player, 3682, 1);
							}
							takeItems(player, 3684, 1);
							takeItems(player, 3686, 1);
							takeItems(player, 3687, 1);
							takeItems(player, 3688, 1);
							takeItems(player, 3689, 1);
							takeItems(player, 3690, 1);
							takeItems(player, 3691, 1);
							takeItems(player, 3685, 1);
							takeItems(player, 3680, -1);
							takeItems(player, 3681, -1);
							qs.setMemoState(2);
							playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							qs.setCond(5);
							qs.showQuestionMark(334, 1);
							return "30738-11.htm";
						}
						break;
					}
					case 5:
					{
						if (hasQuestItems(player, 3467))
						{
							if (qs.getInt("i_quest0") != 1)
							{
								qs.set("i_quest0", 0);
							}
							return "30738-13.htm";
						}
						return "30738-14.htm";
					}
					case 6:
					{
						if (hasQuestItems(player, 3467))
						{
							return "30738-15a.htm";
						}
						giveItems(player, 3680, 1);
						giveItems(player, 3681, 1);
						return "30738-15.htm";
					}
					case 7:
					{
						if (hasQuestItems(player, 3467))
						{
							if (qs.getInt("Exchange") == 0)
							{
								takeItems(player, 3467, 1);
								qs.set("i_quest0", 1);
								qs.set("flag", 1);
								startQuestTimer("2336008", 3 * 1000, npc, player);
								return "30738-16.htm";
							}
							return "30738-20.htm";
						}
						return "30738-14.htm";
					}
					case 8:
					{
						if (hasQuestItems(player, 3467))
						{
							if (qs.getInt("Exchange") == 0)
							{
								takeItems(player, 3467, 1);
								qs.set("i_quest0", 2);
								qs.set("flag", 2);
								startQuestTimer("2336008", 3 * 1000, npc, player);
								return "30738-17.htm";
							}
							return "30738-20.htm";
						}
						return "30738-14.htm";
					}
					case 9:
					{
						if (hasQuestItems(player, 3467))
						{
							if (qs.getInt("Exchange") == 0)
							{
								takeItems(player, 3467, 1);
								qs.set("i_quest0", 3);
								qs.set("flag", 3);
								startQuestTimer("2336008", 3 * 1000, npc, player);
								return "30738-18.htm";
							}
							return "30738-20.htm";
						}
						return "30738-14.htm";
					}
					case 10:
					{
						if (hasQuestItems(player, 3467))
						{
							if (qs.getInt("Exchange") == 0)
							{
								takeItems(player, 3467, 1);
								qs.set("i_quest0", 4);
								qs.set("flag", 4);
								startQuestTimer("2336008", 3 * 1000, npc, player);
								return "30738-19.htm";
							}
							return "30738-20.htm";
						}
						return "30738-14.htm";
					}
					case 2336008:
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.OK_EVERYBODY_PRAY_FERVENTLY));
						startQuestTimer("2336009", 4 * 1000, npc, player);
						break;
					}
					case 2336009:
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.BOTH_HANDS_TO_HEAVEN_EVERYBODY_YELL_TOGETHER));
						startQuestTimer("2336010", 4 * 1000, npc, player);
						break;
					}
					case 2336010:
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.ONE_TWO_MAY_YOUR_DREAMS_COME_TRUE));
						int i0 = 0;
						switch (qs.getInt("i_quest0"))
						{
							case 1:
							{
								i0 = getRandom(2);
								break;
							}
							case 2:
							case 3:
							case 4:
							{
								i0 = getRandom(3);
								break;
							}
						}
						switch (i0)
						{
							case 0:
							{
								switch (qs.getInt("i_quest0"))
								{
									case 1:
									{
										addSpawn(30742, npc);
										qs.set("Exchange", 0);
										break;
									}
									case 2:
									{
										addSpawn(27135, npc);
										addSpawn(27135, npc);
										addSpawn(27135, npc);
										qs.set("Exchange", 0);
										break;
									}
									case 3:
									{
										giveItems(player, 3469, 1);
										qs.set("Exchange", 0);
										break;
									}
									case 4:
									{
										addSpawn(30743, npc);
										qs.set("Exchange", 0);
										break;
									}
								}
								break;
							}
							case 1:
							{
								switch (qs.getInt("i_quest0"))
								{
									case 1:
									{
										addSpawn(27136, npc);
										addSpawn(27136, npc);
										addSpawn(27136, npc);
										addSpawn(27136, npc);
										qs.set("Exchange", 0);
										break;
									}
									case 2:
									{
										qs.calcReward(getId(), 7);
										qs.set("Exchange", 0);
										break;
									}
									case 3:
									{
										addSpawn(27153, npc);
										qs.set("Exchange", 0);
										break;
									}
									case 4:
									{
										addSpawn(30743, npc);
										qs.set("Exchange", 0);
										break;
									}
								}
								break;
							}
							case 2:
							{
								switch (qs.getInt("i_quest0"))
								{
									case 2:
									{
										qs.calcReward(getId(), 7);
										qs.set("Exchange", 0);
										break;
									}
									case 3:
									{
										giveItems(player, 3468, 1);
										qs.set("Exchange", 0);
										break;
									}
									case 4:
									{
										addSpawn(30743, npc);
										qs.set("Exchange", 0);
										break;
									}
								}
								break;
							}
						}
						break;
					}
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final var partyMember = getRandomPartyMemberState(killer, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		final var qs = partyMember.getQuestState(getName());
		if (qs != null)
		{
			switch (npc.getId())
			{
				case 20078 :
				{
					if (hasQuestItems(killer, 3680, 3681) && !hasQuestItems(killer, 3685))
					{
						if (qs.calcDropItems(getId(), 3685, npc.getId(), 1))
						{
							if (hasQuestItems(killer, 3684, 3686, 3687, 3688, 3689, 3690, 3691))
							{
								qs.setCond(4, true);
								qs.showQuestionMark(334, 1);
							}
						}
					}
					break;
				}
				case 20087 :
				case 20088 :
				{
					if (hasQuestItems(killer, 3680, 3681) && !hasQuestItems(killer, 3689))
					{
						if (qs.calcDropItems(getId(), 3689, npc.getId(), 1))
						{
							if (hasQuestItems(killer, 3684, 3685, 3686, 3687, 3688, 3689, 3690, 3691))
							{
								qs.setCond(4, true);
								qs.showQuestionMark(334, 1);
							}
						}
					}
					break;
				}
				case 20168 :
				{
					if (hasQuestItems(killer, 3680, 3681) && !hasQuestItems(killer, 3688))
					{
						if (qs.calcDropItems(getId(), 3688, npc.getId(), 1))
						{
							if (hasQuestItems(killer, 3684, 3685, 3686, 3687, 3689, 3690, 3691))
							{
								qs.setCond(4, true);
								qs.showQuestionMark(334, 1);
							}
						}
					}
					break;
				}
				case 20192 :
				case 20193 :
				{
					if (hasQuestItems(killer, 3680, 3681) && !hasQuestItems(killer, 3690))
					{
						if (qs.calcDropItems(getId(), 3690, npc.getId(), 1))
						{
							if (hasQuestItems(killer, 3684, 3685, 3686, 3687, 3688, 3689, 3690, 3691))
							{
								qs.setCond(4, true);
								qs.showQuestionMark(334, 1);
							}
						}
					}
					break;
				}
				case 20199 :
				{
					if (hasQuestItems(killer, 3680, 3681) && !hasQuestItems(killer, 3684))
					{
						if (qs.calcDropItems(getId(), 3684, npc.getId(), 1))
						{
							if (hasQuestItems(killer, 3685, 3686, 3687, 3688, 3689, 3690, 3691))
							{
								qs.setCond(4, true);
								qs.showQuestionMark(334, 1);
							}
						}
					}
					break;
				}
				case 20227 :
				{
					if (hasQuestItems(killer, 3680, 3681) && !hasQuestItems(killer, 3687))
					{
						if (qs.calcDropItems(getId(), 3687, npc.getId(), 1))
						{
							if (hasQuestItems(killer, 3684, 3685, 3686, 3687, 3688, 3689, 3690, 3691))
							{
								qs.setCond(4, true);
								qs.showQuestionMark(334, 1);
							}
						}
					}
					break;
				}
				case 20248 :
				case 20249 :
				{
					if (hasQuestItems(killer, 3680, 3681) && !hasQuestItems(killer, 3691))
					{
						if (qs.calcDropItems(getId(), 3691, npc.getId(), 1))
						{
							if (hasQuestItems(killer, 3684, 3685, 3686, 3687, 3688, 3689, 3690, 3691))
							{
								qs.setCond(4, true);
								qs.showQuestionMark(334, 1);
							}
						}
					}
					break;
				}
				case 20250 :
				{
					if (hasQuestItems(killer, 3680, 3681) && !hasQuestItems(killer, 3686))
					{
						if (qs.calcDropItems(getId(), 3686, npc.getId(), 1))
						{
							if (hasQuestItems(killer, 3684, 3685, 3686, 3687, 3688, 3689, 3690, 3691))
							{
								qs.setCond(4, true);
								qs.showQuestionMark(334, 1);
							}
						}
					}
					break;
				}
				case 27135 :
				{
					if (qs.isMemoState(2) && (qs.getInt("flag") == 2))
					{
						if (qs.calcDropItems(getId(), 57, npc.getId(), Long.MAX_VALUE))
						{
							qs.set("flag", 0);
						}
					}
					break;
				}
				case 27136 :
				{
					if (qs.isMemoState(2) && !hasQuestItems(killer, 3683) && (qs.getInt("flag") == 1))
					{
						if (qs.calcDropItems(getId(), 3683, npc.getId(), 1))
						{
							qs.set("flag", 0);
						}
					}
					break;
				}
				case 27138 :
				{
					if (qs.isMemoState(2) && (qs.getInt("flag") == 3))
					{
						if (qs.calcDropItems(getId(), 57, npc.getId(), Long.MAX_VALUE))
						{
							qs.set("flag", 0);
						}
					}
					break;
				}
				case 27139 :
				{
					if (qs.isMemoState(1) && !hasQuestItems(killer, 3679))
					{
						if (qs.calcDropItems(getId(), 3679, npc.getId(), 1))
						{
							qs.setCond(2, true);
							qs.showQuestionMark(334, 1);
						}
					}
					break;
				}
				case 27153 :
				{
					if (qs.isMemoState(2) && (qs.getInt("flag") == 3))
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.BONAPARTERIUS_ABYSS_KING_WILL_PUNISH_YOU));
						if (getRandom(2) == 0)
						{
							addSpawn(27154, npc);
						}
						else
						{
							qs.calcReward(getId(), 1, true);
						}
					}
					break;
				}
				case 27154 :
				{
					if (qs.isMemoState(2) && (qs.getInt("flag") == 3))
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.REVENGE_IS_OVERLORD_RAMSEBALIUS_OF_THE_EVIL_WORLD));
						if (getRandom(2) == 0)
						{
							addSpawn(27155, npc);
						}
						else
						{
							qs.calcReward(getId(), 1, true);
						}
						break;
					}
					break;
				}
				case 27155 :
				{
					if (qs.isMemoState(2) && (qs.getInt("flag") == 3))
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getId(), NpcStringId.OH_GREAT_DEMON_KING));
						if (getRandom(2) == 0)
						{
							addSpawn(27138, npc);
						}
						else
						{
							qs.calcReward(getId(), 1, true);
						}
						break;
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _334_TheWishingPotion(334, _334_TheWishingPotion.class.getSimpleName(), "");
	}
}
