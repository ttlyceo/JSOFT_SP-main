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

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.Clan;
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
public class _709_PathToBecomingALordDion extends Quest
{
	private static final int[] OL_MAHUMS =
	{
	        20208, 20209, 20210, 20211, 27392
	};
	
	private static final int[] MANRAGORAS =
	{
	        20154, 20155, 20156
	};
	
	public _709_PathToBecomingALordDion(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(35142);
		addTalkId(35142, 31418, 30735);
		
		addKillId(OL_MAHUMS);
		addKillId(MANRAGORAS);
		
		questItemIds = new int[]
		{
		        13849, 13850
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(2);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final Player castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if (event.equals("35142-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("35142-12.htm"))
		{
			if (isLordAvailable(2, st))
			{
				castleOwner.getQuestState(getName()).setCond(3);
				castleOwner.getQuestState(getName()).set("confidant", String.valueOf(player.getObjectId()));
				st.setState(State.STARTED);
			}
			else
			{
				htmltext = "35142-09b.htm";
			}
		}
		else if (event.equals("31418-05.htm"))
		{
			if (isLordAvailable(3, st))
			{
				castleOwner.getQuestState(getName()).setCond(4);
			}
			else
			{
				htmltext = "31418-06.htm";
			}
		}
		else if (event.equals("30735-02.htm"))
		{
			st.setCond(6, true);
		}
		else if (event.equals("30735-05.htm"))
		{
			takeItems(player, 13850, 1);
			st.setCond(8, true);
		}
		else if (event.equals("31418-09.htm"))
		{
			if (isLordAvailable(8, st))
			{
				takeItems(player, 13849, -1);
				castleOwner.getQuestState(getName()).setCond(9);
			}
		}
		else if (event.equals("35142-23.htm"))
		{
			if (castle.getSiege().getIsInProgress() || TerritoryWarManager.getInstance().isTWInProgress())
			{
				return "35142-22a.htm";
			}
			
			if (hasFort())
			{
				return "35142-22b.htm";
			}
			
			final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_LORD_OF_THE_TOWN_OF_DION_LONG_MAY_HE_REIGN);
			packet.addStringParameter(player.getName(null));
			npc.broadcastPacketToOthers(2000, packet);
			castle.getTerritory().changeOwner(castle.getOwner());
			st.exitQuest(true, true);
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final Castle castle = CastleManager.getInstance().getCastleById(2);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		final Player castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case 35142 :
			{
				if (st.isCond(0))
				{
					if (castleOwner == player)
					{
						if (!hasFort() && castle.getTerritory().getLordObjectId() != player.getObjectId())
						{
							htmltext = "35142-01.htm";
						}
						else
						{
							htmltext = "35142-03.htm";
							st.exitQuest(true);
						}
					}
					else if (isLordAvailable(2, st))
					{
						if (castleOwner.calculateDistance(npc, false, false) <= 200)
						{
							if (castleOwner.getQuestState(getName()).get("confidant") == null)
							{
								htmltext = "35142-11.htm";
							}
							else
							{
								htmltext = "35142-09a.htm";
								st.exitQuest(true);
							}
						}
						else
						{
							htmltext = "35142-10.htm";
						}
					}
					else if (isLordAvailable(3, st))
					{
						if (castleOwner.getQuestState(getName()).get("confidant") != null && castleOwner.getQuestState(getName()).getInt("confidant") == player.getObjectId())
						{
							htmltext = "35142-12.htm";
						}
						else
						{
							htmltext = "35142-09a.htm";
							st.exitQuest(true);
						}
					}
					else
					{
						htmltext = "35142-09a.htm";
						st.exitQuest(true);
					}
				}
				else if (st.isCond(1))
				{
					st.setCond(2, true);
					htmltext = "35142-08.htm";
				}
				else if (st.isCond(2))
				{
					htmltext = "35142-14.htm";
				}
				else if (st.isCond(3))
				{
					htmltext = "35142-15.htm";
				}
				else if (st.isCond(4))
				{
					st.setCond(5, true);
					htmltext = "35142-16.htm";
				}
				else if (st.isCond(5))
				{
					htmltext = "35142-16.htm";
				}
				else if ((st.getCond() > 5) && (st.getCond() < 9))
				{
					htmltext = "35142-15.htm";
				}
				else if (st.isCond(9))
				{
					htmltext = "35142-22.htm";
				}
				break;
			}
			case 31418 :
			{
				if ((st.getState() == State.STARTED) && st.isCond(0) && isLordAvailable(3, st))
				{
					if (castleOwner.getQuestState(getName()).getInt("confidant") == player.getObjectId())
					{
						htmltext = "31418-03.htm";
					}
				}
				else if ((st.getState() == State.STARTED) && st.isCond(0) && isLordAvailable(8, st))
				{
					if (getQuestItemsCount(player, 13849) >= 100)
					{
						htmltext = "31418-08.htm";
					}
					else
					{
						htmltext = "31418-07.htm";
					}
				}
				else if ((st.getState() == State.STARTED) && st.isCond(0) && isLordAvailable(9, st))
				{
					htmltext = "31418-12.htm";
				}
				break;
			}
			case 30735 :
			{
				if (st.isCond(5))
				{
					htmltext = "30735-01.htm";
				}
				else if (st.isCond(6))
				{
					htmltext = "30735-03.htm";
				}
				else if (st.isCond(7))
				{
					htmltext = "30735-04.htm";
				}
				else if (st.isCond(8))
				{
					htmltext = "30735-07.htm";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState st = killer.getQuestState(getName());
		
		if ((st != null) && st.isCond(6) && ArrayUtils.contains(OL_MAHUMS, npc.getId()))
		{
			if ((npc.getId() != 27392) && (Rnd.get(9) == 0))
			{
				addSpawn(27392, npc.getLocation(), true, 300000);
			}
			else if (npc.getId() == 27392)
			{
				giveItems(killer, 13850, 1);
				st.setCond(7, true);
			}
		}
		if ((st != null) && (st.getState() == State.STARTED) && st.isCond(0) && isLordAvailable(8, st) && ArrayUtils.contains(MANRAGORAS, npc.getId()))
		{
			st.calcDoDropItems(getId(), 13849, npc.getId(), 100);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private boolean isLordAvailable(int cond, QuestState st)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(2);
		final Clan owner = castle.getOwner();
		final Player castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if (owner != null)
		{
			if ((castleOwner != null) && (castleOwner != st.getPlayer()) && (owner == st.getPlayer().getClan()) && (castleOwner.getQuestState(getName()) != null) && castleOwner.getQuestState(getName()).isCond(cond))
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean hasFort()
	{
		for (final Fort fortress : FortManager.getInstance().getForts())
		{
			if (fortress.getContractedCastleId() == 2)
			{
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args)
	{
		new _709_PathToBecomingALordDion(709, _709_PathToBecomingALordDion.class.getSimpleName(), "");
	}
}