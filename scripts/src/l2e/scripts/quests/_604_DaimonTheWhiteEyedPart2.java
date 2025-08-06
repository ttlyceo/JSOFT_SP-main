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
 * Rework by LordWinter 13.06.2020
 */
public class _604_DaimonTheWhiteEyedPart2 extends Quest
{
	public static Npc _npc = null;

	public _604_DaimonTheWhiteEyedPart2(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31683);
		addTalkId(31683);
		addTalkId(31541);

		addKillId(25290);

		questItemIds = new int[]
		{
		        7192, 7193, 7194
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

		if (event.equalsIgnoreCase("31683-02.htm"))
		{
			if (player.getLevel() < getMinLvl(getId()) || st.getQuestItemsCount(7192) < 1)
			{
				st.exitQuest(true);
				htmltext = "31683-00b.htm";
			}
			else
			{
				if (st.isCreated())
				{
					st.takeItems(7192, 1);
					st.giveItems(7193, 1);
					st.startQuest();
				}
			}
		}
		else if (event.equalsIgnoreCase("31541-02.htm"))
		{
			if (st.getQuestItemsCount(7193) == 0)
			{
				htmltext = "31541-04.htm";
			}
			else if (_npc != null)
			{
				htmltext = "31541-03.htm";
			}
			else if ((ServerVariables.getLong(getName(), 0) + (3 * 60 * 60 * 1000)) > System.currentTimeMillis())
			{
				htmltext = "31541-05.htm";
			}
			else
			{
				st.takeItems(7193, 1);
				_npc = st.addSpawn(25290, 186320, -43904, -3175);
				if (_npc != null)
				{
					_npc.broadcastPacketToOthers(2000, new NpcSay(_npc.getObjectId(), Say2.ALL, _npc.getId(), NpcStringId.OH_WHERE_I_BE_WHO_CALL_ME));
				}
				st.setCond(2, true);
				st.startQuestTimer("DAIMON_Fail", 12000000);
			}
		}
		else if (event.equalsIgnoreCase("31683-04.htm"))
		{
			if (st.getQuestItemsCount(7194) >= 1)
			{
				htmltext = "list.htm";
			}
			else
			{
				st.exitQuest(true);
				htmltext = "31683-05.htm";
			}
		}
		else if (event.equalsIgnoreCase("INT_MEN"))
		{
			if (st.isCond(3))
			{
				st.takeItems(7194, 1);
				st.calcReward(getId(), 1);
				st.exitQuest(true, true);
			}
			return null;
		}
		else if (event.equalsIgnoreCase("INT_WIT"))
		{
			if (st.isCond(3))
			{
				st.takeItems(7194, 1);
				st.calcReward(getId(), 2);
				st.exitQuest(true, true);
			}
			return null;
		}
		else if (event.equalsIgnoreCase("MEN_INT"))
		{
			if (st.isCond(3))
			{
				st.takeItems(7194, 1);
				st.calcReward(getId(), 3);
				st.exitQuest(true, true);
			}
			return null;
		}
		else if (event.equalsIgnoreCase("MEN_WIT"))
		{
			if (st.isCond(3))
			{
				st.takeItems(7194, 1);
				st.calcReward(getId(), 4);
				st.exitQuest(true, true);
			}
			return null;
		}
		else if (event.equalsIgnoreCase("WIT_INT"))
		{
			if (st.isCond(3))
			{
				st.takeItems(7194, 1);
				st.calcReward(getId(), 5);
				st.exitQuest(true, true);
			}
			return null;
		}
		else if (event.equalsIgnoreCase("WIT_MEN"))
		{
			if (st.isCond(3))
			{
				st.takeItems(7194, 1);
				st.calcReward(getId(), 6);
				st.exitQuest(true, true);
			}
			return null;
		}
		else if (event.equalsIgnoreCase("DAIMON_Fail") && (_npc != null))
		{
			_npc.broadcastPacketToOthers(2000, new NpcSay(_npc.getObjectId(), Say2.ALL, _npc.getId(), NpcStringId.I_CARRY_THE_POWER_OF_DARKNESS_AND_HAVE_RETURNED_FROM_THE_ABYSS));
			_npc.deleteMe();
			_npc = null;
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

		final int cond = st.getCond();
		switch (st.getState())
		{
			case State.CREATED:
				if (npc.getId() == 31683)
				{
					if (cond == 0)
					{
						if (st.getQuestItemsCount(7192) >= 1)
						{
							htmltext = "31683-01.htm";
						}
						else
						{
							htmltext = "31683-00a.htm";
						}
					}
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 31683 :
						if (cond == 1)
						{
							htmltext = "31683-02a.htm";
						}
						else if (cond == 3)
						{
							if (st.getQuestItemsCount(7194) >= 1)
							{
								htmltext = "31683-03.htm";
							}
							else
							{
								htmltext = "31683-06.htm";
							}
						}
						break;
					case 31541 :
						if (cond == 1)
						{
							if ((ServerVariables.getLong(getName(), 0) + (3 * 60 * 60 * 1000)) > System.currentTimeMillis())
							{
								htmltext = "31541-05.htm";
							}
							else
							{
								htmltext = "31541-01.htm";
							}
						}
						else if (cond == 2)
						{
							if (_npc != null)
							{
								htmltext = "31541-03.htm";
							}
							else if ((ServerVariables.getLong(getName(), 0) + (3 * 60 * 60 * 1000)) > System.currentTimeMillis())
							{
								htmltext = "31541-05.htm";
							}
							else
							{
								_npc = st.addSpawn(25290, 186320, -43904, -3175);
								_npc.broadcastPacketToOthers(2000, new NpcSay(_npc.getObjectId(), Say2.ALL, _npc.getId(), NpcStringId.OH_WHERE_I_BE_WHO_CALL_ME));
								st.startQuestTimer("DAIMON_Fail", 12000000);
							}
						}
						else if (cond == 3)
						{
							htmltext = "31541-05.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() == 25290)
		{
			for (final Player partyMember : getMembersCond(player, npc, "cond"))
			{
				if (partyMember != null)
				{
					final QuestState st = partyMember.getQuestState(getName());
					if (st != null)
					{
						if (st.getQuestItemsCount(7193) > 0)
						{
							st.takeItems(7193, 1);
						}
						st.giveItems(7194, 1);
						st.setCond(3, true);
					}
				}
			}
			
			if (_npc != null)
			{
				_npc = null;
			}
		}
		return null;
	}

	public static void main(String[] args)
	{
		new _604_DaimonTheWhiteEyedPart2(604, _604_DaimonTheWhiteEyedPart2.class.getSimpleName(), "");
	}
}