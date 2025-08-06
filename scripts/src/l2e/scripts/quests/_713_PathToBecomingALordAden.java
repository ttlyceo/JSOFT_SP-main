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
import l2e.gameserver.model.ClanMember;
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
public class _713_PathToBecomingALordAden extends Quest
{
	private static final String qn = "_713_PathToBecomingALordAden";
	
	// NPCs
	private static final int LOGAN = 35274;
	private static final int ORVEN = 30857;
	
	// Monsters
	private static final int TAIK_SEEKER = 20666;
	private static final int TAIK_LEADER = 20669;
	
	private static final int CASTLE_ID = 5;
	private static final int REQUIRED_CLAN_MEMBERS = 5;
	
	public _713_PathToBecomingALordAden(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(LOGAN);
		addTalkId(LOGAN, ORVEN);
		
		addKillId(TAIK_SEEKER, TAIK_LEADER);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(CASTLE_ID);
		
		switch (event)
		{
			case "30857-02.htm" :
			{
				htmltext = event;
				break;
			}
			case "35274-03.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "30857-03.htm" :
			{
				if (qs.isCond(1))
				{
					qs.setMemoState(0);
					qs.setCond(2);
					htmltext = event;
				}
				break;
			}
			case "35274-06.htm" :
			{
				if (qs.isCond(7))
				{
					if (castle.getSiege().getIsInProgress())
					{
						return "35274-05a.htm";
					}
					
					for (final Fort fort : FortManager.getInstance().getForts())
					{
						if (!fort.isBorderFortress() && fort.getSiege().getIsInProgress())
						{
							return "35274-05a.htm";
						}
						else if (!fort.isBorderFortress() && (fort.getContractedCastleId() != CASTLE_ID))
						{
							return "35274-05b.htm";
						}
					}
					
					final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_ADEN_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_ADEN);
					packet.addStringParameter(player.getName(null));
					npc.broadcastPacketToOthers(2000, packet);
					castle.getTerritory().changeOwner(castle.getOwner());
					qs.exitQuest(true, true);
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
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(CASTLE_ID);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		switch (npc.getId())
		{
			case LOGAN:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (talker.getObjectId() == talker.getClan().getLeaderId() && castle.getTerritory().getLordObjectId() != talker.getObjectId()) ? "35274-01.htm" : "35274-02.htm";
						break;
					}
					case State.STARTED:
					{
						switch (qs.getCond())
						{
							case 1:
							case 2:
							case 3:
							case 5:
							{
								htmltext = "35274-04.htm";
								break;
							}
							case 7:
							{
								htmltext = "35274-05.htm";
								break;
							}
						}
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(talker);
						break;
					}
				}
				break;
			}
			case ORVEN:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30857-01.htm";
						break;
					}
					case 2:
					{
						htmltext = "30857-04.htm";
						break;
					}
					case 5:
					{
						int clanMemberCount = 0;
						for (final ClanMember clanMember : talker.getClan().getMembers())
						{
							final Player member = clanMember.getPlayerInstance();
							if ((member != null) && member.isOnline() && (member.getObjectId() != talker.getObjectId()))
							{
								QuestState st = getQuestState(member, false);
								st = member.getQuestState(_359_ForSleeplessDeadmen.class.getSimpleName());
								if (st != null && st.isCompleted())
								{
									clanMemberCount++;
								}
							}
						}
						if (clanMemberCount >= REQUIRED_CLAN_MEMBERS)
						{
							qs.setCond(7);
							htmltext = "30857-06.htm";
						}
						else
						{
							htmltext = "30857-05.htm";
						}
						break;
					}
					case 7:
					{
						htmltext = "30857-07.htm";
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
		final QuestState qs = killer.getQuestState(qn);
		if ((qs != null) && qs.isCond(2))
		{
			if (qs.getMemoState() < 100)
			{
				qs.setMemoState(qs.getMemoState() + 1);
			}
			else
			{
				qs.setCond(5);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _713_PathToBecomingALordAden(713, qn, "");
	}
}