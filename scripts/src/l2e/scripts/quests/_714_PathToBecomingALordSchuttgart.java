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

import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter
 */
public class _714_PathToBecomingALordSchuttgart extends Quest
{
	private static final int[] GOLEMS =
	{
	        22809, 22810, 22811, 22812
	};
	
	public _714_PathToBecomingALordSchuttgart(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(35555);
		addTalkId(35555, 31961, 31958);
		
		addKillId(GOLEMS);
		
		questItemIds = new int[]
		{
		        17162
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, false);
		String htmltext = null;
		if (st == null)
		{
			return htmltext;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(9);
		
		switch (event)
		{
			case "35555-02.htm":
			case "35555-06.htm" :
			case "31961-03.htm" :
			{
				htmltext = event;
				break;
			}
			case "35555-04.htm" :
			{
				if (st.isCreated())
				{
					st.startQuest();
					htmltext = event;
				}
				break;
			}
			case "35555-08.htm" :
			{
				if (st.isCond(1))
				{
					st.setCond(2);
					htmltext = event;
				}
				break;
			}
			case "31961-04.htm" :
			{
				if (st.isCond(2))
				{
					st.setCond(3);
					htmltext = event;
				}
				break;
			}
			case "31958-02.htm" :
			{
				if (st.isCond(4))
				{
					st.setCond(5);
					htmltext = event;
				}
				break;
			}
			case "35555-13.htm" :
			{
				if (st.isCond(7))
				{
					if (castle.getSiege().getIsInProgress())
					{
						return "35555-12a.htm";
					}
					
					for (final Fort fort : FortManager.getInstance().getForts())
					{
						if (!fort.isBorderFortress() && fort.getSiege().getIsInProgress())
						{
							return "35555-12a.htm";
						}
						else if (!fort.isBorderFortress() && (fort.getContractedCastleId() != 9))
						{
							return "35555-12b.htm";
						}
					}
					
					final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_SCHUTTGART_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_SCHUTTGART);
					packet.addStringParameter(player.getName(null));
					npc.broadcastPacketToOthers(2000, packet);
					castle.getTerritory().changeOwner(castle.getOwner());
					st.exitQuest(true, true);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState st = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(9);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		switch (npc.getId())
		{
			case 35555 :
			{
				switch (st.getState())
				{
					case State.CREATED:
					{
						htmltext = (talker.getObjectId() == talker.getClan().getLeaderId() && castle.getTerritory().getLordObjectId() != talker.getObjectId()) ? "35555-01.htm" : "35555-03.htm";
						break;
					}
					case State.STARTED:
					{
						switch (st.getCond())
						{
							case 1:
							{
								htmltext = "35555-06.htm";
								break;
							}
							case 2:
							case 3:
							{
								htmltext = "35555-09.htm";
								break;
							}
							case 4:
							{
								htmltext = "35555-10.htm";
								break;
							}
							case 5:
							case 6:
							{
								htmltext = "35555-11.htm";
								break;
							}
							case 7:
							{
								htmltext = "35555-12.htm";
								break;
							}
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(talker);
						break;
					}
				}
				break;
			}
			case 31961 :
			{
				switch (st.getCond())
				{
					case 2:
					{
						htmltext = "31961-02.htm";
						break;
					}
					case 3:
					{
						QuestState qs1 = getQuestState(talker, false);
						qs1 = talker.getQuestState(_114_ResurrectionOfAnOldManager.class.getSimpleName());
						QuestState qs2 = getQuestState(talker, false);
						qs2 = talker.getQuestState(_120_PavelsResearch.class.getSimpleName());
						QuestState qs3 = getQuestState(talker, false);
						qs3 = talker.getQuestState(_121_PavelTheGiant.class.getSimpleName());
						
						if ((qs3 != null) && qs3.isCompleted())
						{
							if ((qs1 != null) && qs1.isCompleted())
							{
								if ((qs2 != null) && qs2.isCompleted())
								{
									st.setCond(4);
									htmltext = "31961-01.htm";
								}
								else
								{
									htmltext = "31961-06.htm";
								}
							}
							else
							{
								htmltext = "31961-05.htm";
							}
						}
						else
						{
							htmltext = "31961-07.htm";
						}
						break;
					}
					case 4:
					{
						htmltext = "31961-01.htm";
						break;
					}
				}
				break;
			}
			case 31958 :
			{
				switch (st.getCond())
				{
					case 4:
					{
						htmltext = "31958-01.htm";
						break;
					}
					case 5:
					{
						htmltext = "31958-03.htm";
						break;
					}
					case 6:
					{
						if (getQuestItemsCount(talker, 17162) >= 300)
						{
							takeItems(talker, 17162, -1);
							st.setCond(7);
							htmltext = "31958-04.htm";
						}
						break;
					}
					case 7:
					{
						htmltext = "31958-05.htm";
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
		final QuestState st = getRandomPartyMemberState(killer, 5, 3, npc);
		if (st != null)
		{
			if (giveItemRandomly(killer, npc, 17162, 1, 300, 1, true))
			{
				st.setCond(6);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _714_PathToBecomingALordSchuttgart(714, _714_PathToBecomingALordSchuttgart.class.getSimpleName(), "");
	}
}