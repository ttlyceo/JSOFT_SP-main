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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;

/**
 * Rework by LordWinter 09.08.2023
 */
public class _607_ProveYourCourage extends Quest
{
	private static boolean _killForAll;
	
	public _607_ProveYourCourage(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31370);
		addTalkId(31370);
		
		addKillId(25309);
		
		_killForAll = getQuestParams(questId).getBool("killForAll");
		
		questItemIds = new int[]
		{
		        7235
		};
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("31370-2.htm"))
		{
			if (st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("31370-4.htm"))
		{
			if (st.getQuestItemsCount(7235) >= 1 && st.isCond(2))
			{
				st.takeItems(7235, -1);
				st.calcExpAndSp(getId());
				st.calcReward(getId());
				st.exitQuest(true, true);
			}
			else
			{
				htmltext = "31370-2r.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final var st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		final int cond = st.getCond();
		if (cond == 0)
		{
			if (player.getLevel() >= getMinLvl(getId()))
			{
				if ((st.getQuestItemsCount(7213) == 1) || (st.getQuestItemsCount(7214) == 1) || (st.getQuestItemsCount(7215) == 1))
				{
					htmltext = "31370-1.htm";
				}
				else
				{
					htmltext = "31370-00.htm";
					st.exitQuest(true);
				}
			}
			else
			{
				htmltext = "31370-0.htm";
				st.exitQuest(true);
			}
		}
		else if ((cond == 1) && (st.getQuestItemsCount(7235) == 0))
		{
			htmltext = "31370-2r.htm";
		}
		else if ((cond == 2) && (st.getQuestItemsCount(7235) >= 1))
		{
			htmltext = "31370-3.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (_killForAll && player != null)
		{
			final var isInRange = npc.isInRangeZ(player, Config.ALT_PARTY_RANGE);
			for (final var creature : ((Attackable) npc).getAggroList().getCharMap().keySet())
			{
				if (creature != null && creature.isPlayer())
				{
					final Player pl = creature.getActingPlayer();
					if (pl != null && !pl.isDead() && (npc.isInRangeZ(pl, Config.ALT_PARTY_RANGE) || isInRange))
					{
						final var st = pl.getQuestState(getName());
						if (st != null && st.isCond(1) && st.calcDropItems(getId(), 7235, npc.getId(), 1))
						{
							st.setCond(2, true);
						}
					}
				}
			}
		}
		else
		{
			final var st = player.getQuestState(getName());
			if (st != null)
			{
				final var party = player.getParty();
				if (party != null)
				{
					for (final var plr : party.getMembers())
					{
						if (plr == null)
						{
							continue;
						}
						
						final var qs = plr.getQuestState(getName());
						if (qs != null && qs.isCond(1) && st.calcDropItems(getId(), 7235, npc.getId(), 1))
						{
							qs.setCond(2, true);
						}
					}
				}
				else
				{
					if (st.isCond(1) && st.calcDropItems(getId(), 7235, npc.getId(), 1))
					{
						st.setCond(2, true);
					}
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _607_ProveYourCourage(607, _607_ProveYourCourage.class.getSimpleName(), "");
	}
}