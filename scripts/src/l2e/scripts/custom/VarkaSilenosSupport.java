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
package l2e.scripts.custom;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.util.Util;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.serverpackets.WareHouseWithdrawList;

public class VarkaSilenosSupport extends Quest
{
	private static final String qn = "VarkaSilenosSupport";

	private static final int ASHAS = 31377; // Hierarch
	private static final int NARAN = 31378; // Messenger
	private static final int UDAN = 31379; // Buffer
	private static final int DIYABU = 31380; // Grocer
	private static final int HAGOS = 31381; // Warehouse Keeper
	private static final int SHIKON = 31382; // Trader
	private static final int TERANU = 31383; // Teleporter

	private static final int[] NPCS =
	{
	        ASHAS, NARAN, UDAN, DIYABU, HAGOS, SHIKON, TERANU
	};

	private static final int[] VARKA_MARKS =
	{
	        7221, 7222, 7223, 7224, 7225
	};

	private static final int SEED = 7187;

	private static final Map<Integer, BuffsData> BUFF = new HashMap<>();

	private class BuffsData
	{
		private final int _skill;
		private final int _cost;

		public BuffsData(int skill, int cost)
		{
			super();
			_skill = skill;
			_cost = cost;
		}

		public Skill getSkill()
		{
			return SkillsParser.getInstance().getInfo(_skill, 1);
		}

		public int getCost()
		{
			return _cost;
		}
	}

	private int getAllianceLevel(Player player)
	{
		for (int i = 0; i < VARKA_MARKS.length; i++)
		{
			if (hasQuestItems(player, VARKA_MARKS[i]))
			{
				return -(i + 1);
			}
		}
		return 0;
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return htmltext;
		}

		final int Alevel = getAllianceLevel(player);
		if (Util.isDigit(event) && BUFF.containsKey(Integer.parseInt(event)))
		{
			final BuffsData buff = BUFF.get(Integer.parseInt(event));
			if (st.getQuestItemsCount(SEED) >= buff.getCost())
			{
				st.takeItems(SEED, buff.getCost());
				npc.setTarget(player);
				npc.doCast(buff.getSkill());
				npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
				htmltext = "31379-4.htm";
			}
		}
		else if (event.equals("Withdraw"))
		{
			if (player.getWarehouse().getSize() == 0)
			{
				htmltext = "31381-0.htm";
			}
			else
			{
				player.sendActionFailed();
				player.setActiveWarehouse(player.getWarehouse());
				player.sendPacket(new WareHouseWithdrawList(1, player, 1));
				player.sendPacket(new WareHouseWithdrawList(2, player, 1));
			}
		}
		else if (event.equals("Teleport"))
		{
			if (Alevel == -4)
			{
				htmltext = "31383-4.htm";
			}
			else if (Alevel == -5)
			{
				htmltext = "31383-5.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			st = newQuestState(player);
		}
		final int npcId = npc.getId();
		final int Alevel = getAllianceLevel(player);
		if (npcId == ASHAS)
		{
			if (Alevel < 0)
			{
				htmltext = "31377-friend.htm";
			}
			else
			{
				htmltext = "31377-no.htm";
			}
		}
		else if (npcId == NARAN)
		{
			if (Alevel < 0)
			{
				htmltext = "31378-friend.htm";
			}
			else
			{
				htmltext = "31378-no.htm";
			}
		}
		else if (npcId == UDAN)
		{
			st.setState(State.STARTED);
			if (Alevel > 0)
			{
				htmltext = "31379-3.htm";
			}
			else if (Alevel > -3)
			{
				htmltext = "31379-1.htm";
			}
			else if (Alevel < -2)
			{
				if (st.hasQuestItems(SEED))
				{
					htmltext = "31379-4.htm";
				}
				else
				{
					htmltext = "31379-2.htm";
				}
			}
		}
		else if (npcId == DIYABU)
		{
			if (player.getKarma() >= 1)
			{
				htmltext = "31380-pk.htm";
			}
			else if (Alevel >= 0)
			{
				htmltext = "31380-no.htm";
			}
			else if ((Alevel == -1) || (Alevel == -2))
			{
				htmltext = "31380-1.htm";
			}
			else
			{
				htmltext = "31380-2.htm";
			}
		}
		else if (npcId == HAGOS)
		{
			if (Alevel >= 0)
			{
				htmltext = "31381-no.htm";
			}
			else if (Alevel == -1)
			{
				htmltext = "31381-1.htm";
			}
			else if (player.getWarehouse().getSize() == 0)
			{
				htmltext = "31381-3.htm";
			}
			else if ((Alevel == -2) || (Alevel == -3))
			{
				htmltext = "31381-2.htm";
			}
			else
			{
				htmltext = "31381-4.htm";
			}
		}
		else if (npcId == SHIKON)
		{
			if (Alevel == -2)
			{
				htmltext = "31382-1.htm";
			}
			else if ((Alevel == -3) || (Alevel == -4))
			{
				htmltext = "31382-2.htm";
			}
			else if (Alevel == -5)
			{
				htmltext = "31382-3.htm";
			}
			else
			{
				htmltext = "31382-no.htm";
			}
		}
		else if (npcId == TERANU)
		{
			if (Alevel >= 0)
			{
				htmltext = "31383-no.htm";
			}
			else if ((Alevel < 0) && (Alevel > -4))
			{
				htmltext = "31383-1.htm";
			}
			else if (Alevel == -4)
			{
				htmltext = "31383-2.htm";
			}
			else
			{
				htmltext = "31383-3.htm";
			}
		}
		return htmltext;
	}

	public VarkaSilenosSupport(int id, String name, String descr)
	{
		super(id, name, descr);

		for (final int i : NPCS)
		{
			addFirstTalkId(i);
		}
		addTalkId(UDAN);
		addTalkId(HAGOS);
		addTalkId(TERANU);
		addStartNpc(HAGOS);
		addStartNpc(TERANU);

		BUFF.put(1, new BuffsData(4359, 2)); // Focus: Requires 2
		                                     // Nepenthese Seeds
		BUFF.put(2, new BuffsData(4360, 2)); // Death Whisper: Requires
		                                     // 2 Nepenthese Seeds
		BUFF.put(3, new BuffsData(4345, 3)); // Might: Requires 3
		                                     // Nepenthese Seeds
		BUFF.put(4, new BuffsData(4355, 3)); // Acumen: Requires 3
		                                     // Nepenthese Seeds
		BUFF.put(5, new BuffsData(4352, 3)); // Berserker: Requires 3
		                                     // Nepenthese Seeds
		BUFF.put(6, new BuffsData(4354, 3)); // Vampiric Rage: Requires
		                                     // 3 Nepenthese Seeds
		BUFF.put(7, new BuffsData(4356, 6)); // Empower: Requires 6
		                                     // Nepenthese Seeds
		BUFF.put(8, new BuffsData(4357, 6)); // Haste: Requires 6
		                                     // Nepenthese Seeds
	}

	public static void main(String args[])
	{
		new VarkaSilenosSupport(-1, qn, "custom");
	}
}
