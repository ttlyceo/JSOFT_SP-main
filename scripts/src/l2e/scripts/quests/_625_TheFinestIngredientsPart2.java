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


import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 27.04.2020
 */
public class _625_TheFinestIngredientsPart2 extends Quest
{
	public static Npc _npc = null;

	public _625_TheFinestIngredientsPart2(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31521);
		addTalkId(31521, 31542);
		
		addKillId(25296);
		
		questItemIds = new int[]
		{
		        7209, 7210
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

		String htmltext = event;

		final int _state = st.getState();
		final int cond = st.getCond();
		if (event.equalsIgnoreCase("jeremy_q0625_0104.htm") && (_state == State.CREATED))
		{
			if (st.getQuestItemsCount(7205) == 0)
			{
				st.exitQuest(true);
				htmltext = "jeremy_q0625_0102.htm";
			}
			else
			{
				st.startQuest();
				st.takeItems(7205, 1);
				st.giveItems(7209, 1);
			}
		}
		else if (event.equalsIgnoreCase("jeremy_q0625_0301.htm") && (_state == State.STARTED) && (cond == 3))
		{
			if (st.getQuestItemsCount(7210) == 0)
			{
				htmltext = "jeremy_q0625_0302.htm";
			}
			else
			{
				st.takeItems(7210, 1);
				st.calcReward(getId(), Rnd.get(1, 6));
			}
			st.exitQuest(true, true);
		}
		else if (event.equalsIgnoreCase("yetis_table_q0625_0201.htm") && (_state == State.STARTED) && (cond == 1))
		{
			if ((ServerVariables.getLong(getName(), 0) + (3 * 60 * 60 * 1000)) > System.currentTimeMillis())
			{
				htmltext = "yetis_table_q0625_0204.htm";
			}
			else if (st.getQuestItemsCount(7209) == 0)
			{
				htmltext = "yetis_table_q0625_0203.htm";
			}
			else if (_npc != null)
			{
				htmltext = "yetis_table_q0625_0202.htm";
			}
			else
			{
				st.takeItems(7209, 1);
				st.setCond(2, true);
				ThreadPoolManager.getInstance().schedule(new BumbalumpSpawner(st), 1000);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		final int cond = st.getCond();

		switch (st.getState())
		{
			case State.CREATED:
				if (npcId == 31521)
				{
					if (player.getLevel() < getMinLvl(getId()))
					{
						st.exitQuest(true);
						htmltext = "jeremy_q0625_0103.htm";
					}
					else if (st.getQuestItemsCount(7205) == 0)
					{
						st.exitQuest(true);
						htmltext = "jeremy_q0625_0102.htm";
					}
					else
					{
						st.set("cond", "0");
						htmltext = "jeremy_q0625_0101.htm";
					}
				}
				break;
			case State.STARTED:
				if (npcId == 31521)
				{
					if (cond == 1)
					{
						htmltext = "jeremy_q0625_0105.htm";
					}
					else if (cond == 2)
					{
						htmltext = "jeremy_q0625_0202.htm";
					}
					else if (cond == 3)
					{
						htmltext = "jeremy_q0625_0201.htm";
					}
				}
				else if (npcId == 31542)
				{
					if ((ServerVariables.getLong(getName(), 0) + (3 * 60 * 60 * 1000)) > System.currentTimeMillis())
					{
						htmltext = "yetis_table_q0625_0204.htm";
					}
					else if (cond == 1)
					{
						htmltext = "yetis_table_q0625_0101.htm";
					}
					else if (cond == 2)
					{
						if (_npc != null)
						{
							htmltext = "yetis_table_q0625_0202.htm";
						}
						else
						{
							ThreadPoolManager.getInstance().schedule(new BumbalumpSpawner(st), 1000);
							htmltext = "yetis_table_q0625_0201.htm";
						}
					}
					else if (cond == 3)
					{
						htmltext = "yetis_table_q0625_0204.htm";
					}
				}
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		for (final Player partyMember : getMembersCond(player, npc, "cond"))
		{
			if (partyMember != null)
			{
				final QuestState st = partyMember.getQuestState(getName());
				if (st != null)
				{
					if ((st.getCond() == 1) || (st.getCond() == 2))
					{
						if (st.getQuestItemsCount(7209) > 0)
						{
							st.takeItems(7209, 1);
						}
						st.giveItems(7210, 1);
						st.setCond(3, true);
					}
				}
			}
		}
		if (_npc != null)
		{
			_npc = null;
		}
		return null;
	}

	public class BumbalumpSpawner implements Runnable
	{
		private int tiks = 0;

		public BumbalumpSpawner(QuestState st)
		{
			if (_npc != null)
			{
				return;
			}
			_npc = st.addSpawn(25296, 158240, -121536, -2253);
		}

		@Override
		public void run()
		{
			if (_npc == null)
			{
				return;
			}
			if (tiks == 0)
			{
				_npc.broadcastPacketToOthers(2000, new NpcSay(_npc.getObjectId(), Say2.ALL, _npc.getId(), NpcStringId.I_WILL_TASTE_YOUR_BLOOD));
			}
			if ((tiks < 1200) && (_npc != null))
			{
				tiks++;
				if (tiks == 1200)
				{
					_npc.broadcastPacketToOthers(2000, new NpcSay(_npc.getObjectId(), Say2.ALL, _npc.getId(), NpcStringId.CURSE_THOSE_WHO_DEFY_THE_GODS));
				}
				ThreadPoolManager.getInstance().schedule(this, 1000);
				return;
			}
			else
			{
				_npc.deleteMe();
				_npc = null;
			}
		}
	}

	public static void main(String[] args)
	{
		new _625_TheFinestIngredientsPart2(625, _625_TheFinestIngredientsPart2.class.getSimpleName(), "");
	}
}