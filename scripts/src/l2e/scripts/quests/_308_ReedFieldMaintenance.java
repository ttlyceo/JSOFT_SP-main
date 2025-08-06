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
import l2e.gameserver.network.serverpackets.RadarControl;

/**
 * Rework by LordWinter 14.10.2021
 */
public class _308_ReedFieldMaintenance extends Quest
{
	public _308_ReedFieldMaintenance(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(32646);
		addTalkId(32646);
		
		addKillId(22650, 22651, 22652, 22653, 22654, 22655);

		questItemIds = new int[]
		{
		        14871, 14872
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "32646-02.htm" :
			case "32646-03.htm" :
			case "32646-06.htm" :
			case "32646-07.htm" :
			case "32646-08.htm" :
			case "32646-10.htm" :
				htmltext = event;
				break;
			case "32646-04.htm" :
				if (st.isCreated())
				{
					st.startQuest();
					player.sendPacket(new RadarControl(0, 2, 77325, 205773, -3432));
				}
				htmltext = event;
				break;
			case "claimreward" :
				final QuestState q238 = player.getQuestState(_238_SuccesFailureOfBusiness.class.getSimpleName());
				htmltext = ((q238 != null) && q238.isCompleted()) ? "32646-09.htm" : "32646-12.htm";
				break;
			case "DYNASTY_EARRING" :
				htmltext = onItemExchangeRequest(st, 1, 230, 115, false);
				break;
			case "DYNASTY_NECKLACE" :
				htmltext = onItemExchangeRequest(st, 2, 308, 154, false);
				break;
			case "DYNASTY_RING" :
				htmltext = onItemExchangeRequest(st, 3, 154, 77, false);
				break;
			case "DYNASTY_SIGIL" :
				htmltext = onItemExchangeRequest(st, 4, 248, 124, false);
				break;
			case "MOIRAI_RECIPES" :
				htmltext = onItemExchangeRequest(st, 5, 216, 108, true);
				break;
			case "32646-11.htm" :
				st.exitQuest(true, true);
				htmltext = event;
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		final QuestState q309 = player.getQuestState(_309_ForAGoodCause.class.getSimpleName());
		if ((q309 != null) && q309.isStarted())
		{
			htmltext = "32646-15.htm";
		}
		else if (st.isStarted())
		{
			htmltext = (st.hasQuestItems(14871) || st.hasQuestItems(14872)) ? "32646-06.htm" : "32646-05.htm";
		}
		else
		{
			htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32646-01.htm" : "32646-00.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 1);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			st.calcDropItems(getId(), npc.getId() == 22655 ? 14872 : 14871, npc.getId(), Integer.MAX_VALUE);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	private String onItemExchangeRequest(QuestState st, int id, int amount1, int amount2, boolean isRandom)
	{
		String htmltext;
		final long mucrokian = st.getQuestItemsCount(14871);
		final long awakened = st.getQuestItemsCount(14872);
		if (mucrokian >= amount1 || awakened >= amount2)
		{
			if (mucrokian >= amount1)
			{
				st.takeItems(14871, amount1);
			}
			else
			{
				st.takeItems(14872, amount2);
			}
			
			if (isRandom)
			{
				st.calcReward(getId(), id, true);
			}
			else
			{
				st.calcReward(getId(), id);
			}
			st.playSound(QuestSound.ITEMSOUND_QUEST_FINISH);
			htmltext = "32646-14.htm";
		}
		else
		{
			htmltext = "32646-13.htm";
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _308_ReedFieldMaintenance(308, _308_ReedFieldMaintenance.class.getSimpleName(), "");
	}
}