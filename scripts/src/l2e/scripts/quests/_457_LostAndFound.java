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
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.QuestState.QuestType;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;

/**
 * Rework by LordWinter 22.02.2020
 */
public final class _457_LostAndFound extends Quest
{
	private static Spawner[] _escortCheckers = new Spawner[2];

	private _457_LostAndFound(int id, String name, String descr)
	{
		super(id, name, descr);

		addStartNpc(32759);
		addFirstTalkId(32759);
		addTalkId(32759);
		addKillId(22789, 22790, 22791, 22793);
		
		int i = 0;
		for (final Spawner spawn : SpawnParser.getInstance().getSpawnData())
		{
			if (spawn.getId() == 32764)
			{
				_escortCheckers[i] = spawn;
				i++;
			}
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null || (st.isCompleted() && !st.isNowAvailable()))
		{
			return getNoQuestMsg(player);
		}

		String htmltext = null;
		switch (event)
		{
			case "32759-06.htm":
				npc.setScriptValue(0);
				npc.stopDespawn();
				st.startQuest();
				npc.setTarget(player);
				npc.setWalking();
				npc.getAI().setIntention(CtrlIntention.FOLLOW, player);
				startQuestTimer("check", 1000, npc, player, true);
				startQuestTimer("time_limit", 600000, npc, player);
				startQuestTimer("talk_time", 120000, npc, player);
				startQuestTimer("talk_time2", 30000, npc, player);
				break;
			case "talk_time":
				if (npc != null)
				{
					broadcastNpcSay(npc, player, NpcStringId.AH_I_THINK_I_REMEMBER_THIS_PLACE, false);
				}
				break;
			case "talk_time2":
				if (npc != null)
				{
					broadcastNpcSay(npc, player, NpcStringId.WHAT_WERE_YOU_DOING_HERE, false);
					startQuestTimer("talk_time3", 10 * 1000, npc, player);
				}
				break;
			case "talk_time3":
				if (npc != null)
				{
					broadcastNpcSay(npc, player, NpcStringId.I_GUESS_YOURE_THE_SILENT_TYPE_THEN_ARE_YOU_LOOKING_FOR_TREASURE_LIKE_ME, false);
				}
				break;
			case "time_limit":
				if (npc != null)
				{
					startQuestTimer("stop", 2000, npc, player);
				}
				st.exitQuest(QuestType.DAILY);
				break;
			case "check":
				if (npc != null)
				{
					final double distance = Math.sqrt(npc.getPlanDistanceSq(player.getX(), player.getY()));
					if (distance > 1000)
					{
						if (distance > 5000)
						{
							startQuestTimer("stop", 2000, npc, player);
							st.exitQuest(QuestType.DAILY);
						}
						else if (npc.isScriptValue(0))
						{
							broadcastNpcSay(npc, player, NpcStringId.HEY_DONT_GO_SO_FAST, true);
							npc.setScriptValue(1);
						}
						else if (npc.isScriptValue(1))
						{
							broadcastNpcSay(npc, player, NpcStringId.ITS_HARD_TO_FOLLOW, true);
							npc.setScriptValue(2);
						}
						else if (npc.isScriptValue(2))
						{
							startQuestTimer("stop", 2000, npc, player);
							st.exitQuest(QuestType.DAILY);
						}
					}
					
					for (final Spawner escortSpawn : _escortCheckers)
					{
						final Npc escort = escortSpawn.getLastSpawn();
						if ((escort != null) && npc.isInsideRadius(escort, 1000, false, false))
						{
							startQuestTimer("stop", 1000, npc, player);
							startQuestTimer("bye", 3000, npc, player);
							cancelQuestTimer("check", npc, player);
							npc.broadcastPacketToOthers(2000, new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(null), NpcStringId.AH_FRESH_AIR));
							broadcastNpcSay(npc, player, NpcStringId.AH_FRESH_AIR, false);
							st.calcReward(getId());
							st.exitQuest(QuestType.DAILY, true);
							break;
						}
					}
				}
				break;
			case "stop" :
				if (npc != null)
				{
					npc.setTarget(null);
					npc.getAI().stopFollow();
					npc.getAI().setIntention(CtrlIntention.IDLE);
					cancelQuestTimer("check", npc, player);
					cancelQuestTimer("time_limit", npc, player);
					cancelQuestTimer("talk_time", npc, player);
					cancelQuestTimer("talk_time2", npc, player);
				}
				break;
			case "bye":
				if (npc != null)
				{
					npc.deleteMe();
				}
				break;
			default:
			{
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getTarget() != null)
		{
			return npc.getTarget().equals(player) ? "32759-08.htm" : "32759-01a.htm";
		}
		return "32759.htm";
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		switch (st.getState())
		{
			case State.CREATED:
				htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32759-01.htm" : "32759-03.htm";
				break;
			case State.COMPLETED:
				if (st.isNowAvailable())
				{
					st.setState(State.CREATED);
					htmltext = (player.getLevel() >= getMinLvl(getId())) ? "32759-01.htm" : "32759-03.htm";
				}
				else
				{
					htmltext = "32759-02.htm";
				}
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = getQuestState(player, true);
		if (st.isNowAvailable() && player.getLevel() >= 82)
		{
			if (Rnd.chance(1))
			{
				if (Rnd.chance(10))
				{
					addSpawn(32759, npc, 60000);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	public void broadcastNpcSay(Npc npc, Player player, NpcStringId stringId, boolean whisper)
	{
		((whisper) ? player : npc).sendPacket(new CreatureSay(npc.getObjectId(), ((whisper) ? Say2.TELL : Say2.ALL), npc.getId(), stringId));
	}

	public static void main(String[] args)
	{
		new _457_LostAndFound(457, _457_LostAndFound.class.getSimpleName(), "");
	}
}