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
public class _712_PathToBecomingALordOren extends Quest
{
	private static final String qn = "_712_PathToBecomingALordOren";
	
	private static final int Brasseur = 35226;
	private static final int Croop = 30676;
	private static final int Marty = 30169;
	private static final int Valleria = 30176;
	
	private static final int NebuliteOrb = 13851;
	
	private static final int[] OelMahims =
	{
		20575,
		20576
	};
	
	private static final int OrenCastle = 4;
	
	public _712_PathToBecomingALordOren(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(new int[]
		{
			Brasseur,
			Marty
		});
		
		addTalkId(Brasseur);
		addTalkId(Croop);
		addTalkId(Marty);
		addTalkId(Valleria);
		
		questItemIds = new int[]
		{
			NebuliteOrb
		};
		
		addKillId(OelMahims);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(qn);
		final Castle castle = CastleManager.getInstance().getCastleById(OrenCastle);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		final Player castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if (event.equals("brasseur_q712_03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("croop_q712_03.htm"))
		{
			st.setCond(3);
		}
		else if (event.equals("marty_q712_02.htm"))
		{
			if (isLordAvailable(3, st))
			{
				castleOwner.getQuestState(qn).setCond(4);
				st.setState(State.STARTED);
			}
		}
		else if (event.equals("valleria_q712_02.htm"))
		{
			if (isLordAvailable(4, st))
			{
				castleOwner.getQuestState(qn).setCond(5);
				st.exitQuest(true);
			}
		}
		else if (event.equals("croop_q712_05.htm"))
		{
			st.setCond(6);
		}
		else if (event.equals("croop_q712_07.htm"))
		{
			takeItems(player, NebuliteOrb, -1);
			st.setCond(8);
		}
		else if (event.equals("brasseur_q712_06.htm"))
		{
			if (castleOwner != null)
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_OREN_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_OREN);
				packet.addStringParameter(player.getName(null));
				npc.broadcastPacketToOthers(2000, packet);
				castle.getTerritory().changeOwner(castle.getOwner());
				st.exitQuest(true, true);
			}
		}
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final Castle castle = CastleManager.getInstance().getCastleById(OrenCastle);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		final Player castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case Brasseur:
			{
				if (st.isCond(0))
				{
					if (castleOwner == player)
					{
						if (!hasFort() && castle.getTerritory().getLordObjectId() != player.getObjectId())
						{
							htmltext = "brasseur_q712_01.htm";
						}
						else
						{
							htmltext = "brasseur_q712_00.htm";
							st.exitQuest(true);
						}
					}
					else
					{
						htmltext = "brasseur_q712_00a.htm";
						st.exitQuest(true);
					}
				}
				else if (st.isCond(1))
				{
					st.setCond(2);
					htmltext = "brasseur_q712_04.htm";
				}
				else if (st.isCond(2))
				{
					htmltext = "brasseur_q712_04.htm";
				}
				else if (st.isCond(8))
				{
					htmltext = "brasseur_q712_05.htm";
				}
				break;
			}
			case Croop:
			{
				if (st.isCond(2))
				{
					htmltext = "croop_q712_01.htm";
				}
				else if (st.isCond(3) || st.isCond(4))
				{
					htmltext = "croop_q712_03.htm";
				}
				else if (st.isCond(5))
				{
					htmltext = "croop_q712_04.htm";
				}
				else if (st.isCond(6))
				{
					htmltext = "croop_q712_05.htm";
				}
				else if (st.isCond(7))
				{
					htmltext = "croop_q712_06.htm";
				}
				else if (st.isCond(8))
				{
					htmltext = "croop_q712_08.htm";
				}
				break;
			}
			case Marty:
			{
				if (st.isCond(0))
				{
					if (isLordAvailable(3, st))
					{
						htmltext = "marty_q712_01.htm";
					}
					else
					{
						htmltext = "marty_q712_00.htm";
					}
				}
				break;
			}
			case Valleria:
			{
				if ((st.getState() == State.STARTED) && isLordAvailable(4, st))
				{
					htmltext = "valleria_q712_01.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public final String onKill(Npc npc, Player killer, boolean isPet)
	{
		final QuestState st = killer.getQuestState(qn);
		if ((st != null) && st.isCond(6))
		{
			if (getQuestItemsCount(killer, NebuliteOrb) < 300)
			{
				giveItems(killer, NebuliteOrb, 1);
			}
			if (getQuestItemsCount(killer, NebuliteOrb) >= 300)
			{
				st.setCond(7);
			}
		}
		return null;
	}
	
	private boolean isLordAvailable(int cond, QuestState st)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(OrenCastle);
		final Clan owner = castle.getOwner();
		final Player castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if (owner != null)
		{
			if ((castleOwner != null) && (castleOwner != st.getPlayer()) && (owner == st.getPlayer().getClan()) && (castleOwner.getQuestState(qn) != null) && (castleOwner.getQuestState(qn).isCond(cond)))
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
			if (fortress.getContractedCastleId() == OrenCastle)
			{
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args)
	{
		new _712_PathToBecomingALordOren(712, qn, "");
	}
}
