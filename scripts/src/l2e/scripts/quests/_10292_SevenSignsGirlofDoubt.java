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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 25.12.2019
 */
public class _10292_SevenSignsGirlofDoubt extends Quest
{
	private final Map<Integer, InstanceHolder> ReflectionWorlds = new HashMap<>();
	
	private static class InstanceHolder
	{
		List<Npc> mobs = new ArrayList<>();
		boolean spawned = false;
	}
	
	public _10292_SevenSignsGirlofDoubt(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32593);
		addTalkId(32593, 32597, 30832, 32784, 32862, 32617);
		
		addKillId(27422, 22801, 22802, 22804, 22805);
		
		questItemIds = new int[]
		{
		        17226
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
		
		final int instanceId = npc.getReflectionId();
		InstanceHolder holder = ReflectionWorlds.get(instanceId);
		if (holder == null)
		{
			holder = new InstanceHolder();
			ReflectionWorlds.put(instanceId, holder);
		}
		
		if (event.equalsIgnoreCase("evil_despawn"))
		{
			holder.spawned = false;
			for (final Npc h : holder.mobs)
			{
				if (h != null)
				{
					h.deleteMe();
				}
			}
			holder.mobs.clear();
			ReflectionWorlds.remove(instanceId);
			return null;
		}
		else if (npc.getId() == 32593)
		{
			if (event.equalsIgnoreCase("32593-05.htm") && st.isCreated())
			{
				st.startQuest();
			}
		}
		else if (npc.getId() == 32597)
		{
			if (event.equalsIgnoreCase("32597-08.htm"))
			{
				if (st.isCond(1))
				{
					st.setCond(2, true);
				}
			}
		}
		else if (npc.getId() == 30832)
		{
			if (event.equalsIgnoreCase("30832-02.htm"))
			{
				if (st.isCond(7))
				{
					st.setCond(8, true);
				}
			}
		}
		else if (npc.getId() == 32784)
		{
			if (event.equalsIgnoreCase("32784-03.htm"))
			{
				if (st.isCond(2))
				{
					st.setCond(3, true);
				}
			}
			else if (event.equalsIgnoreCase("32784-14.htm"))
			{
				if (st.isCond(6))
				{
					st.setCond(7, true);
				}
			}
			else if (event.equalsIgnoreCase("spawn"))
			{
				if (!holder.spawned)
				{
					st.takeItems(17226, -1);
					holder.spawned = true;
					final Npc evil = addSpawn(27422, 89440, -238016, -9632, 335, false, 0, false, player.getReflection());
					evil.setIsNoRndWalk(true);
					holder.mobs.add(evil);
					final Npc evil1 = addSpawn(27424, 89524, -238131, -9632, 56, false, 0, false, player.getReflection());
					evil1.setIsNoRndWalk(true);
					holder.mobs.add(evil1);
					startQuestTimer("evil_despawn", 60000, evil, player);
					return null;
				}
				htmltext = "32593-02.htm";
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
		else if (npc.getId() == 32593)
		{
			if (st.getState() == State.COMPLETED)
			{
				htmltext = "32593-02.htm";
			}
			else if (player.getLevel() < getMinLvl(getId()))
			{
				htmltext = "32593-03.htm";
			}
			else if ((player.getQuestState(_198_SevenSignEmbryo.class.getSimpleName()) == null) || (player.getQuestState(_198_SevenSignEmbryo.class.getSimpleName()).getState() != State.COMPLETED))
			{
				htmltext = "32593-03.htm";
			}
			else if (st.getState() == State.CREATED)
			{
				htmltext = "32593-01.htm";
			}
			else if (st.getCond() >= 1)
			{
				htmltext = "32593-07.htm";
			}
		}
		else if (npc.getId() == 32597)
		{
			if (st.isCond(1))
			{
				htmltext = "32597-01.htm";
			}
			else if (st.isCond(2))
			{
				htmltext = "32597-03.htm";
			}
		}
		else if (npc.getId() == 32784)
		{
			if (st.isCond(2))
			{
				htmltext = "32784-01.htm";
			}
			else if (st.isCond(3))
			{
				htmltext = "32784-04.htm";
			}
			else if (st.isCond(4))
			{
				st.setCond(5, true);
				htmltext = "32784-05.htm";
			}
			else if (st.isCond(5))
			{
				st.playSound("ItemSound.quest_middle");
				htmltext = "32784-05.htm";
			}
			else if (st.isCond(6))
			{
				st.playSound("ItemSound.quest_middle");
				htmltext = "32784-11.htm";
			}
			else if (st.isCond(8))
			{
				if (player.isSubClassActive())
				{
					htmltext = "32784-18.htm";
				}
				else
				{
					st.calcExpAndSp(getId());
					st.exitQuest(false, true);
					htmltext = "32784-16.htm";
				}
			}
		}
		else if (npc.getId() == 30832)
		{
			if (st.isCond(7))
			{
				htmltext = "30832-01.htm";
			}
			else if (st.isCond(8))
			{
				htmltext = "30832-04.htm";
			}
		}
		else if (npc.getId() == 32617)
		{
			if (st.getState() == State.STARTED)
			{
				if (st.getCond() >= 1)
				{
					htmltext = "32617-01.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		if (npc.getId() == 22801 || npc.getId() == 22802 || npc.getId() == 22804 || npc.getId() == 22805)
		{
			if (st != null && st.isCond(3) && st.calcDropItems(getId(), 17226, npc.getId(), 10))
			{
				st.setCond(4);
			}
		}
		
		if (st != null && st.isCond(5) && npc.getId() == 27422)
		{
			final int instanceid = npc.getReflectionId();
			final InstanceHolder holder = ReflectionWorlds.get(instanceid);
			if (holder == null)
			{
				return null;
			}
			for (final Npc h : holder.mobs)
			{
				if (h != null)
				{
					h.deleteMe();
				}
			}
			holder.spawned = false;
			holder.mobs.clear();
			ReflectionWorlds.remove(instanceid);
			st.setCond(6, true);
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _10292_SevenSignsGirlofDoubt(10292, _10292_SevenSignsGirlofDoubt.class.getSimpleName(), "");
	}
}