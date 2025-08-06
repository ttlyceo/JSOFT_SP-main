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

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 16.11.2020
 */
public class _196_SevenSignSealOfTheEmperor extends Quest
{
	public _196_SevenSignSealOfTheEmperor(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30969);
		addTalkId(30969, 32593, 32584, 32598, 32586, 32587, 32657);

		questItemIds = new int[]
		{
		        15310, 13808, 13846, 13809
		};
	}

	protected void exitInstance(Player player)
	{
		player.teleToLocation(171782, -17612, -4901, true, ReflectionManager.DEFAULT);
		final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
		if (world != null)
		{
			final Reflection inst = world.getReflection();
			inst.setDuration(5 * 60000);
			inst.setEmptyDestroyTime(0);
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("30969-05.htm"))
		{
			final QuestState qs = player.getQuestState(_195_SevenSignSecretRitualOfThePriests.class.getSimpleName());
			if ((qs != null && qs.isCompleted()) && player.getLevel() >= getMinLvl(getId()))
			{
				if (st.isCreated())
				{
					st.startQuest();
				}
			}
		}
		else if (event.equalsIgnoreCase("32598-02.htm"))
		{
			st.giveItems(13809, 1);
			st.playSound("ItemSound.quest_middle");
		}
		else if (event.equalsIgnoreCase("30969-11.htm") && st.isCond(5))
		{
			st.setCond(6, true);
		}
		else if (event.equalsIgnoreCase("32584-05.htm") && st.isCond(1))
		{
			st.setCond(2, true);
			npc.deleteMe();
		}
		else if (event.equalsIgnoreCase("32586-06.htm") && st.isCond(3))
		{
			st.setCond(4, true);
			st.giveItems(15310, 1);
			st.giveItems(13808, 1);
		}
		else if (event.equalsIgnoreCase("32586-12.htm") && st.isCond(4))
		{
			st.setCond(5, true);
			st.takeItems(13846, 4);
			st.takeItems(15310, 1);
			st.takeItems(13808, 1);
			st.takeItems(13809, 1);
		}
		else if (event.equalsIgnoreCase("32593-02.htm") && st.isCond(6))
		{
			st.calcExpAndSp(getId());
			st.exitQuest(false, true);
		}
		else if (event.equalsIgnoreCase("30969-06.htm"))
		{
			if (GameObjectsStorage.getByNpcId(32584) == null)
			{
				final Npc mammon = addSpawn(32584, 109742, 219978, -3520, 0, false, 120000, true);
				mammon.broadcastPacketToOthers(2000, new NpcSay(mammon.getObjectId(), 0, mammon.getId(), NpcStringId.WHO_DARES_SUMMON_THE_MERCHANT_OF_MAMMON));
			}
			else
			{
				return "30969-06a.htm";
			}
		}
		return event;
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
		switch (npc.getId())
		{
			case 30969 :
				if (player.getLevel() < getMinLvl(getId()))
				{
					st.exitQuest(true);
					htmltext = "30969-00.htm";
				}
				
				final QuestState qs = player.getQuestState(_195_SevenSignSecretRitualOfThePriests.class.getSimpleName());
				if (qs == null)
				{
					return htmltext;
				}
				
				if (qs.isCompleted() && (st.getState() == State.CREATED))
				{
					htmltext = "30969-01.htm";
				}
				else
				{
					switch (cond)
					{
						case 0 :
							st.exitQuest(true);
							htmltext = "30969-00.htm";
							break;
						case 1 :
							htmltext = "30969-05.htm";
							break;
						case 2 :
							st.set("cond", "3");
							htmltext = "30969-08.htm";
							break;
						case 5 :
							htmltext = "30969-09.htm";
							break;
						case 6 :
							htmltext = "30969-11.htm";
							break;
					}
				}
				break;
			case 32593 :
				if (cond == 6)
				{
					htmltext = "32593-01.htm";
				}
				else if (st.getState() == State.COMPLETED)
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			case 32584 :
				switch (cond)
				{
					case 1 :
						htmltext = "32584-01.htm";
						break;
				}
				break;
			case 32598 :
				switch (cond)
				{
					case 4 :
						if (st.getQuestItemsCount(13809) == 0)
						{
							htmltext = "32598-01.htm";
						}
						if (st.getQuestItemsCount(13809) >= 1)
						{
							htmltext = "32598-03.htm";
						}
						break;
				}
				break;
			case 32586 :
				switch (cond)
				{
					case 3 :
						htmltext = "32586-01.htm";
						break;
					case 4 :
						if (st.getQuestItemsCount(15310) == 0)
						{
							st.giveItems(15310, 1);
							htmltext = "32586-14.htm";
						}
						if (st.getQuestItemsCount(13808) == 0)
						{
							st.giveItems(13808, 1);
							htmltext = "32586-14.htm";
						}
						if (st.getQuestItemsCount(13846) <= 3)
						{
							htmltext = "32586-07.htm";
						}
						if (st.getQuestItemsCount(13846) >= 4)
						{
							htmltext = "32586-08.htm";
						}
						break;
					case 5 :
						htmltext = "32586-13.htm";
						break;
				}
				break;
			case 32657 :
				switch (cond)
				{
					case 4 :
						htmltext = "32657-01.htm";
						break;
				}
				break;
			case 32587 :
				if (st.getCond() >= 3)
				{
						exitInstance(player);
						htmltext = "32587-02.htm";
						break;
				}
				break;
		}
		return htmltext;
	}

	public static void main(String[] args)
	{
		new _196_SevenSignSealOfTheEmperor(196, _196_SevenSignSealOfTheEmperor.class.getSimpleName(), "");
	}
}
