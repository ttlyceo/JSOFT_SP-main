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
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;

/**
 * Rework by LordWinter 26.04.2021
 */
public class _423_TakeYourBestShot extends Quest
{
	private static int _totalModifier;
	private static int _spawnChance;
	
	private static final int[] _mobs =
	{
	        22768, 22769, 22770, 22771, 22772, 22773, 22774
	};
	
	public _423_TakeYourBestShot(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32740, 32744);
		addTalkId(32740, 32744);
		addFirstTalkId(32740);
		
		addKillId(18862, 22768, 22769, 22770, 22771, 22772, 22773, 22774);
		
		_spawnChance = getQuestParams(questId).getInteger("spawnChance");
		_totalModifier = getQuestParams(questId).getInteger("totalModifier");
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (npc.getId() == 32744)
		{
			if (event.equalsIgnoreCase("32744-04.htm"))
			{
				if (st.isCreated())
				{
					st.startQuest();
				}
			}
			else if (event.equalsIgnoreCase("32744-quit.htm"))
			{
				st.exitQuest(true);
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
		
		if (npc.getId() == 32744)
		{
			switch (st.getState())
			{
				case State.CREATED :
					final QuestState _prev = player.getQuestState("_249_PoisonedPlainsOfTheLizardmen");
					if ((_prev != null) && (_prev.getState() == State.COMPLETED) && (player.getLevel() >= getMinLvl(getId())))
					{
						if (st.hasQuestItems(15496))
						{
							htmltext = "32744-07.htm";
						}
						else
						{
							htmltext = "32744-01.htm";
						}
					}
					else
					{
						htmltext = "32744-00.htm";
					}
					break;
				case State.STARTED :
					if (st.isCond(1))
					{
						htmltext = "32744-05.htm";
					}
					else if (st.isCond(2))
					{
						htmltext = "32744-06.htm";
					}
					break;
			}
		}
		else if (npc.getId() == 32740)
		{
			if (st.getState() == State.CREATED)
			{
				if (st.hasQuestItems(15496))
				{
					htmltext = "32740-05.htm";
				}
				else
				{
					htmltext = "32740-00.htm";
				}
			}
			else if ((st.getState() == State.STARTED) && st.isCond(1))
			{
				htmltext = "32740-02.htm";
			}
			else if ((st.getState() == State.STARTED) && st.isCond(2))
			{
				st.calcReward(getId());
				st.exitQuest(true, true);
				htmltext = "32740-04.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			final Quest q = QuestManager.getInstance().getQuest(getName());
			st = q.newQuestState(player);
		}
		
		if (npc.isInsideRadius(96782, 85918, 100, true))
		{
			return "32740-ugoros.htm";
		}
		return "32740.htm";
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final var partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st != null)
		{
			if (ArrayUtils.contains(_mobs, npc.getId()) && (getRandom(_totalModifier) <= _spawnChance))
			{
				final Npc guard = addSpawn(18862, npc, false);
				if (player != null)
				{
					guard.setIsRunning(true);
					((Attackable) guard).addDamageHate(player, 0, 999);
					guard.getAI().setIntention(CtrlIntention.ATTACK, player);
				}
			}
			else if (npc.getId() == 18862 && st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
		new _423_TakeYourBestShot(423, _423_TakeYourBestShot.class.getSimpleName(), "");
	}
}
