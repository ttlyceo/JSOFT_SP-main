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
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter
 */
public class _710_PathToBecomingALordGiran extends Quest
{
	private static final String qn = "_710_PathToBecomingALordGiran";
	
	private static final int SAUL = 35184;
	private static final int GESTO = 30511;
	private static final int FELTON = 30879;
	private static final int CARGO_BOX = 32243;
	
	private static final int FREIGHT_CHESTS_SEAL = 13014;
	private static final int GESTOS_BOX = 13013;
	
	private static final int[] MOBS =
	{
	        20832, 20833, 20835, 21602, 21603, 21604, 21605, 21606, 21607, 21608, 21609
	};
	
	private static final int GIRAN_CASTLE = 3;
	
	public _710_PathToBecomingALordGiran(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(SAUL);
		addTalkId(SAUL, GESTO, FELTON, CARGO_BOX);
		addKillId(MOBS);
		
		questItemIds = new int[]
		{
			FREIGHT_CHESTS_SEAL,
			GESTOS_BOX
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(qn);
		final Castle castle = CastleManager.getInstance().getCastleById(GIRAN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		if (event.equals("35184-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30511-03.htm"))
		{
			st.setCond(3);
		}
		else if (event.equals("30879-02.htm"))
		{
			st.setCond(4);
		}
		else if (event.equals("35184-07.htm"))
		{
			if (castle.getOwner().getLeader().getPlayerInstance() != null)
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_GIRAN_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_GIRAN);
				packet.addStringParameter(player.getName(null));
				npc.broadcastPacketToOthers(2000, packet);
				castle.getTerritory().changeOwner(castle.getOwner());
				st.exitQuest(true, true);
			}
		}
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState st = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(GIRAN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final Player castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case SAUL:
			{
				if (st.isCond(0))
				{
					if (castleOwner == talker)
					{
						if (!hasFort() && castle.getTerritory().getLordObjectId() != talker.getObjectId())
						{
							htmltext = "35184-01.htm";
						}
						else
						{
							htmltext = "35184-00.htm";
							st.exitQuest(true);
						}
					}
					else
					{
						htmltext = "35184-00a.htm";
						st.exitQuest(true);
					}
				}
				else if (st.isCond(1))
				{
					st.setCond(2);
					htmltext = "35184-04.htm";
				}
				else if (st.isCond(2))
				{
					htmltext = "35184-05.htm";
				}
				else if (st.isCond(9))
				{
					htmltext = "35184-06.htm";
				}
				break;
			}
			case GESTO:
			{
				if (st.isCond(2))
				{
					htmltext = "30511-01.htm";
				}
				else if (st.isCond(3) || st.isCond(4))
				{
					htmltext = "30511-04.htm";
				}
				else if (st.isCond(5))
				{
					takeItems(talker, FREIGHT_CHESTS_SEAL, -1);
					st.setCond(7);
					htmltext = "30511-05.htm";
				}
				else if (st.isCond(7))
				{
					htmltext = "30511-06.htm";
				}
				else if (st.isCond(8))
				{
					takeItems(talker, GESTOS_BOX, -1);
					st.setCond(9);
					htmltext = "30511-07.htm";
				}
				else if (st.isCond(9))
				{
					htmltext = "30511-07.htm";
				}
				break;
			}
			case FELTON:
			{
				if (st.isCond(3))
				{
					htmltext = "30879-01.htm";
				}
				else if (st.isCond(4))
				{
					htmltext = "30879-03.htm";
				}
				break;
			}
			case CARGO_BOX:
			{
				if (st.isCond(4))
				{
					st.setCond(5);
					giveItems(talker, FREIGHT_CHESTS_SEAL, 1);
					htmltext = "32243-01.htm";
				}
				else if (st.isCond(5))
				{
					htmltext = "32243-02.htm";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState st = killer.getQuestState(qn);
		
		if ((st != null) && st.isCond(7))
		{
			if (getQuestItemsCount(killer, GESTOS_BOX) < 300)
			{
				giveItems(killer, GESTOS_BOX, 1);
			}
			if (getQuestItemsCount(killer, GESTOS_BOX) >= 300)
			{
				st.setCond(8);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private boolean hasFort()
	{
		for (final Fort fortress : FortManager.getInstance().getForts())
		{
			if (fortress.getContractedCastleId() == GIRAN_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args)
	{
		new _710_PathToBecomingALordGiran(710, qn, "");
	}
}