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
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Update by LordWinter 13.06.2019
 */
public final class _692_HowtoOpposeEvil extends Quest
{
	private static final String qn = "_692_HowtoOpposeEvil";

	private static final int DILIOS = 32549;
	private static final int KUTRAN = 32550;
	private static final int LEKON = 32557;
	
	private static final int FREED_SOUL_FRAGMENT = 13863;
	private static final int DRAGONKIN_CHARM_FRAGMENT = 13865;
	private static final int RESTLESS_SOUL = 13866;
	private static final int TIAT_CHARM = 13867;

	private static final int CONCENTRATED_SPIRIT_ENERGY = 15535;
	private static final int SPIRIT_STONE_DUST = 15536;

	private static final int FREED_SOUL = 13796;
	private static final int DRAGONKIN_CHARM = 13841;
	private static final int SPIRIT_STONE_FRAGMENT = 15486;

	private static final int[] SOD =
	{
	        22552, 22541, 22550, 22551, 22596, 22544, 22540, 22547, 22542, 22543, 22539, 22546, 22548, 22536, 22538, 22537
	};
	private static final int[] SOI =
	{
	        22509, 22510, 22511, 22512, 22513, 22514, 22515, 22520, 22522, 22527, 22531, 22535, 22516, 22517, 22518, 22519, 22521, 22524, 22528, 22532, 22530, 22535
	};

	private static final int[] SOA =
	{
	        22746, 22747, 22748, 22749, 22750, 22751, 22752, 22753, 22754, 22755, 22756, 22757, 22758, 22759, 22760, 22761, 22762, 22763, 22764, 22765
	};

	public _692_HowtoOpposeEvil(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(DILIOS);
		addTalkId(DILIOS);
		addTalkId(KUTRAN);
		addTalkId(LEKON);
		
		for (final int i : SOD)
		{
			addKillId(i);
		}
		
		for (final int i : SOI)
		{
			addKillId(i);
		}
		
		for (final int i : SOA)
		{
			addKillId(i);
		}

		questItemIds = new int[]
		{
		        FREED_SOUL_FRAGMENT, DRAGONKIN_CHARM_FRAGMENT, RESTLESS_SOUL, TIAT_CHARM, CONCENTRATED_SPIRIT_ENERGY, SPIRIT_STONE_DUST
		};
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		final int cond = st.getInt("cond");
		
		if (event.equalsIgnoreCase("take_test") && cond == 0)
		{
			final QuestState _quest = player.getQuestState("_10273_GoodDayToFly");
			if ((_quest != null) && (_quest.getState() == State.COMPLETED))
			{
				st.set("cond", "2");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
				htmltext = "dilios_q692_4.htm";
			}
			else
			{
				st.set("cond", "1");
				st.setState(State.STARTED);
				st.playSound("ItemSound.quest_accept");
				htmltext = "dilios_q692_3.htm";
			}
		}
		else if (event.equalsIgnoreCase("lekon_q692_2.htm") && cond == 1)
		{
			st.exitQuest(true);
			st.playSound("ItemSound.quest_finish");
		}
		else if (event.equalsIgnoreCase("kutran_q692_2.htm") && cond == 2)
		{
			st.set("cond", "3");
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("exchange_sod") && cond == 3)
		{
			if (st.getQuestItemsCount(DRAGONKIN_CHARM_FRAGMENT) < 5)
			{
				htmltext = "kutran_q692_7.htm";
			}
			else
			{
				final int _charmstogive = Math.round(st.getQuestItemsCount(DRAGONKIN_CHARM_FRAGMENT) / 5);
				st.takeItems(DRAGONKIN_CHARM_FRAGMENT, 5 * _charmstogive);
				st.giveItems(DRAGONKIN_CHARM, _charmstogive);
				htmltext = "kutran_q692_4.htm";
			}
		}
		else if (event.equalsIgnoreCase("exchange_soi") && cond == 3)
		{
			if (st.getQuestItemsCount(FREED_SOUL_FRAGMENT) < 5)
			{
				htmltext = "kutran_q692_7.htm";
			}
			else
			{
				final int _soulstogive = Math.round(st.getQuestItemsCount(FREED_SOUL_FRAGMENT) / 5);
				st.takeItems(FREED_SOUL_FRAGMENT, 5 * _soulstogive);
				st.giveItems(FREED_SOUL, _soulstogive);
				htmltext = "kutran_q692_5.htm";
			}
		}
		else if (event.equalsIgnoreCase("exchange_soa") && cond == 3)
		{
			if (st.getQuestItemsCount(SPIRIT_STONE_DUST) < 5)
			{
				htmltext = "kutran_q692_7.htm";
			}
			else
			{
				final int _soulstogive = Math.round(st.getQuestItemsCount(SPIRIT_STONE_DUST) / 5);
				st.takeItems(SPIRIT_STONE_DUST, 5 * _soulstogive);
				st.giveItems(SPIRIT_STONE_FRAGMENT, _soulstogive);
				htmltext = "kutran_q692_5.htm";
			}
		}
		else if (event.equalsIgnoreCase("exchange_breath") && cond == 3)
		{
			if (st.getQuestItemsCount(TIAT_CHARM) == 0)
			{
				htmltext = "kutran_q692_7.htm";
			}
			else
			{
				st.giveItems(57, st.getQuestItemsCount(TIAT_CHARM) * 2500);
				st.takeItems(TIAT_CHARM, -1);
				htmltext = "kutran_q692_5.htm";
			}
		}
		else if (event.equalsIgnoreCase("exchange_portion") && cond == 3)
		{
			if (st.getQuestItemsCount(RESTLESS_SOUL) == 0)
			{
				htmltext = "kutran_q692_7.htm";
			}
			else
			{
				st.giveItems(57, st.getQuestItemsCount(RESTLESS_SOUL) * 2500);
				st.takeItems(RESTLESS_SOUL, -1);
				htmltext = "kutran_q692_5.htm";
			}
		}
		else if (event.equalsIgnoreCase("exchange_energy") && cond == 3)
		{
			if (st.getQuestItemsCount(CONCENTRATED_SPIRIT_ENERGY) == 0)
			{
				htmltext = "kutran_q692_7.htm";
			}
			else
			{
				st.giveItems(57, st.getQuestItemsCount(CONCENTRATED_SPIRIT_ENERGY) * 25000);
				st.takeItems(CONCENTRATED_SPIRIT_ENERGY, -1);
				htmltext = "kutran_q692_5.htm";
			}
		}
		return htmltext;
	}

	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}
		
		final int cond = st.getInt("cond");
		final int npcId = npc.getId();

		if (npcId == DILIOS)
		{
			if (cond == 0)
			{
				if (player.getLevel() >= 75)
				{
					htmltext = "dilios_q692_1.htm";
				}
				else
				{
					htmltext = "dilios_q692_0.htm";
				}
			}
		}
		else if (npcId == KUTRAN)
		{
			if (cond == 2)
			{
				htmltext = "kutran_q692_1.htm";
			}
			else if (cond == 3)
			{
				htmltext = "kutran_q692_3.htm";
			}
		}
		else if (npcId == LEKON)
		{
			if (cond == 1)
			{
				htmltext = "lekon_q692_1.htm";
			}
		}
		return htmltext;
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMember(player, 3);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			final int npcId = npc.getId();
			
			if (ArrayUtils.contains(SOD, npcId))
			{
				st.rollAndGive(DRAGONKIN_CHARM_FRAGMENT, 1, 10);
			}
			else if (ArrayUtils.contains(SOI, npcId))
			{
				st.rollAndGive(FREED_SOUL_FRAGMENT, 1, 10);
			}
			else if (ArrayUtils.contains(SOA, npcId))
			{
				st.rollAndGive(SPIRIT_STONE_DUST, 1, 15);
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public static void main(String[] args)
	{
		new _692_HowtoOpposeEvil(692, qn, "");
	}
}
