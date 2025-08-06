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
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.SocialAction;

/**
 * Rework by LordWinter 29.12.2021
 */
public final class _064_CertifiedBerserker extends Quest
{
	public _064_CertifiedBerserker(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32207);
		addTalkId(32207, 32200, 32215, 32252, 32253);
		
		addKillId(20202, 20234, 20267, 20268, 20269, 20270, 20271, 20551, 27323);
		
		questItemIds = new int[]
		{
		        9754, 9755, 9756, 9757, 9758, 9759
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
					if (player.getVarInt("2ND_CLASS_DIAMOND_REWARD", 0) == 0)
					{
						giveItems(player, 7562, 48);
						player.setVar("2ND_CLASS_DIAMOND_REWARD", 1);
						htmltext = "32207-06.htm";
					}
					else
					{
						htmltext = "32207-06a.htm";
					}
				}
				break;
			}
			case "32207-10.htm" :
			{
				if (qs.isMemoState(11))
				{
					htmltext = event;
				}
				break;
			}
			case "32207-11.htm" :
			{
				if (qs.isMemoState(11))
				{
					qs.calcExpAndSp(getId());
					qs.calcReward(getId());
					qs.exitQuest(false, true);
					player.sendPacket(new SocialAction(player.getObjectId(), 3));
					htmltext = event;
				}
				break;
			}
			case "32215-02.htm" :
			{
				if (qs.isMemoState(1))
				{
					qs.setMemoState(2);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "32215-07.htm" :
			case "32215-08.htm" :
			case "32215-09.htm" :
			{
				if (qs.isMemoState(5))
				{
					htmltext = event;
				}
				break;
			}
			case "32215-10.htm" :
			{
				if (qs.isMemoState(5))
				{
					takeItems(player, 9755, 1);
					qs.setMemoState(6);
					qs.setCond(8, true);
					htmltext = event;
				}
				break;
			}
			case "32215-15.htm" :
			{
				if (qs.isMemoState(10))
				{
					takeItems(player, 9758, 1);
					giveItems(player, 9759, 1);
					qs.setMemoState(11);
					qs.setCond(14, true);
					htmltext = event;
				}
				break;
			}
			case "32252-02.htm" :
			{
				if (qs.isMemoState(3))
				{
					qs.setMemoState(4);
					qs.setCond(5, true);
					htmltext = event;
				}
				break;
			}
			case "32253-02.htm" :
			{
				if (qs.isMemoState(9))
				{
					giveItems(player, 9758, 1);
					qs.setMemoState(10);
					qs.setCond(13, true);
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
			if (npc.getId() == 32207)
			{
				if (player.getRace() == Race.Kamael)
				{
					if (player.getClassId() == ClassId.trooper)
					{
						if (player.getLevel() >= getMinLvl(getId()))
						{
							htmltext = "32207-01.htm";
						}
						else
						{
							htmltext = "32207-02.htm";
						}
					}
					else
					{
						htmltext = "32207-03.htm";
					}
				}
				else
				{
					htmltext = "32207-04.htm";
				}
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case 32207 :
				{
					if (memoState == 1)
					{
						htmltext = "32207-07.htm";
					}
					else if ((memoState >= 2) && (memoState < 11))
					{
						htmltext = "32207-08.htm";
					}
					else if (memoState == 11)
					{
						htmltext = "32207-09.htm";
					}
					break;
				}
				case 32200 :
				{
					if (memoState == 6)
					{
						qs.setMemoState(7);
						qs.setCond(9, true);
						player.getRadar().addMarker(27956, 106003, -3831);
						player.getRadar().addMarker(50568, 152408, -2656);
						htmltext = "32200-01.htm";
					}
					else if (memoState == 7)
					{
						if (!hasQuestItems(player, 9756, 9757))
						{
							htmltext = "32200-02.htm";
						}
						else
						{
							takeItems(player, 9756, 1);
							takeItems(player, 9757, 1);
							qs.setMemoState(8);
							qs.setCond(11, true);
							htmltext = "32200-03.htm";
						}
					}
					else if (memoState == 8)
					{
						htmltext = "32200-04.htm";
					}
					break;
				}
				case 32215 :
				{
					if (memoState == 1)
					{
						htmltext = "32215-01.htm";
					}
					else if (memoState == 2)
					{
						if (getQuestItemsCount(player, 9754) < 20)
						{
							htmltext = "32215-03.htm";
						}
						else
						{
							takeItems(player, 9754, -1);
							qs.setMemoState(3);
							qs.setCond(4, true);
							htmltext = "32215-04.htm";
						}
					}
					else if (memoState == 3)
					{
						htmltext = "32215-05.htm";
					}
					else if (memoState == 5)
					{
						htmltext = "32215-06.htm";
					}
					else if (memoState == 6)
					{
						htmltext = "32215-11.htm";
					}
					else if (memoState == 8)
					{
						qs.setMemoState(9);
						qs.setCond(12, true);
						htmltext = "32215-12.htm";
					}
					else if (memoState == 9)
					{
						htmltext = "32215-13.htm";
					}
					else if (memoState == 10)
					{
						htmltext = "32215-14.htm";
					}
					else if (memoState == 11)
					{
						htmltext = "32215-16.htm";
					}
					break;
				}
				case 32252 :
				{
					if (memoState == 3)
					{
						htmltext = "32252-01.htm";
					}
					else if (memoState == 4)
					{
						if (!hasQuestItems(player, 9755))
						{
							htmltext = "32252-03.htm";
						}
						else
						{
							qs.setMemoState(5);
							qs.setCond(7, true);
							htmltext = "32252-04.htm";
						}
					}
					else if (memoState == 5)
					{
						htmltext = "32252-05.htm";
					}
					break;
				}
				case 32253 :
				{
					if (memoState == 9)
					{
						htmltext = "32253-01.htm";
					}
					else if (memoState == 10)
					{
						htmltext = "32253-03.htm";
					}
					break;
				}
			}
		}
		else if (qs.isCompleted())
		{
			if (npc.getId() == 32207)
			{
				htmltext = "32207-05.htm";
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
				case 20202 :
				{
					if (qs.isMemoState(7) && !hasQuestItems(killer, 9756))
					{
						qs.calcDropItems(getId(), 9756, npc.getId(), 1);
						if (hasQuestItems(killer, 9757))
						{
							qs.setCond(10, true);
						}
					}
					break;
				}
				case 20234 :
				{
					if (qs.isMemoState(7) && !hasQuestItems(killer, 9757))
					{
						qs.calcDropItems(getId(), 9757, npc.getId(), 1);
						if (hasQuestItems(killer, 9756))
						{
							qs.setCond(10, true);
						}
					}
					break;
				}
				case 20267 :
				case 20268 :
				case 20269 :
				case 20270 :
				case 20271 :
				{
					if (qs.isMemoState(2))
					{
						if (qs.calcDropItems(getId(), 9754, npc.getId(), 20))
						{
							qs.setCond(3, true);
						}
					}
					break;
				}
				case 20551 :
				{
					if (qs.isMemoState(4) && !hasQuestItems(killer, 9755))
					{
						if (qs.calcDropItems(getId(), 9755, npc.getId(), 1))
						{
							qs.setCond(6, true);
						}
					}
					break;
				}
				case 27323 :
				{
					if (qs.isMemoState(9))
					{
						if (getRandom(100) < 20)
						{
							final Npc kamael = addSpawn(32253, npc.getLocation(), true, 60000);
							kamael.broadcastPacketToOthers(2000, new NpcSay(kamael, Say2.NPC_ALL, NpcStringId.S1_DID_YOU_COME_TO_HELP_ME).addStringParameter(killer.getAppearance().getVisibleName()));
							playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						}
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _064_CertifiedBerserker(64, _064_CertifiedBerserker.class.getSimpleName(), "");
	}
}