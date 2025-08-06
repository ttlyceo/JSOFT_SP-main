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

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.model.PlayerGroup;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 05.01.2022
 */
public class _254_LegendaryTales extends Quest
{
	public enum Bosses
	{
		EMERALD_HORN(25718), DUST_RIDER(25719), BLEEDING_FLY(25720), BLACK_DAGGER(25721), SHADOW_SUMMONER(25722), SPIKE_SLASHER(25723), MUSCLE_BOMBER(25724);

		private final int _bossId;
		private final int _mask;

		private Bosses(int bossId)
		{
			_bossId = bossId;
			_mask = 1 << ordinal();
		}

		public int getId()
		{
			return _bossId;
		}

		public int getMask()
		{
			return _mask;
		}

		public static Bosses valueOf(int npcId)
		{
			for (final Bosses val : values())
			{
				if (val.getId() == npcId)
				{
					return val;
				}
			}
			return null;
		}
	}
	
	public _254_LegendaryTales(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30754);
		addTalkId(30754);
		
		addKillId(25718, 25719, 25720, 25721, 25722, 25723, 25724);
		
		questItemIds = new int[]
		{
		        17249
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return htmltext;
		}
		
		if (npc.getId() == 30754)
		{
			if (event.equalsIgnoreCase("accept"))
			{
				if (st.isCreated())
				{
					st.startQuest();
					htmltext = "30754-07.htm";
				}
			}
			else if (event.equalsIgnoreCase("emerald"))
			{
				htmltext = (checkMask(st, Bosses.EMERALD_HORN) ? "30754-22.htm" : "30754-16.htm");
			}
			else if (event.equalsIgnoreCase("dust"))
			{
				htmltext = (checkMask(st, Bosses.DUST_RIDER) ? "30754-23.htm" : "30754-17.htm");
			}
			else if (event.equalsIgnoreCase("bleeding"))
			{
				htmltext = (checkMask(st, Bosses.BLEEDING_FLY) ? "30754-24.htm" : "30754-18.htm");
			}
			else if (event.equalsIgnoreCase("daggerwyrm"))
			{
				htmltext = (checkMask(st, Bosses.BLACK_DAGGER) ? "30754-25.htm" : "30754-19.htm");
			}
			else if (event.equalsIgnoreCase("shadowsummoner"))
			{
				htmltext = (checkMask(st, Bosses.SHADOW_SUMMONER) ? "30754-26.htm" : "30754-16.htm");
			}
			else if (event.equalsIgnoreCase("spikeslasher"))
			{
				htmltext = (checkMask(st, Bosses.SPIKE_SLASHER) ? "30754-27.htm" : "30754-17.htm");
			}
			else if (event.equalsIgnoreCase("muclebomber"))
			{
				htmltext = (checkMask(st, Bosses.MUSCLE_BOMBER) ? "30754-28.htm" : "30754-18.htm");
			}
			else if (Util.isDigit(event))
			{
				if (st.getQuestItemsCount(17249) >= 7 && st.isCond(2))
				{
					st.takeItems(17249, -1);
					st.calcReward(getId(), Integer.parseInt(event));
					htmltext = "30754-13.htm";
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "30754-12.htm";
				}
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
		
		switch (st.getState())
		{
			case State.CREATED :
				htmltext = player.getLevel() < getMinLvl(getId()) ? "30754-03.htm" : "30754-01.htm";
				break;
			case State.STARTED :
				htmltext = st.isCond(2) ? "30754-10.htm" : "30754-09.htm";
				break;
			case State.COMPLETED :
				htmltext = "30754-02.htm";
				break;
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final PlayerGroup group = player.getPlayerGroup();
		if (group != null)
		{
			QuestState st;
			for (final Player pl : group)
			{
				if (pl != null && !pl.isDead() && (npc.isInRangeZ(pl, Config.ALT_PARTY_RANGE) || npc.isInRangeZ(player, Config.ALT_PARTY_RANGE)))
				{
					st = pl.getQuestState(getName());
					if (st != null && st.isCond(1))
					{
						final int raids = st.getInt("raids");
						final Bosses boss = Bosses.valueOf(npc.getId());
						if (!checkMask(st, boss))
						{
							st.set("raids", raids | boss.getMask());
							if (st.calcDropItems(getId(), 17249, npc.getId(), 7))
							{
								st.setCond(2, true);
							}
						}
					}
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	private static boolean checkMask(QuestState st, Bosses boss)
	{
		final int pos = boss.getMask();
		return ((st.getInt("raids") & pos) == pos);
	}
	
	public static void main(String[] args)
	{
		new _254_LegendaryTales(254, _254_LegendaryTales.class.getSimpleName(), "");
	}
}