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

import java.util.HashMap;
import java.util.Map;

import l2e.commons.util.Rnd;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Updated by LordWinter 06.04.2022
 */
public class _501_ProofOfClanAlliance extends Quest
{
	private static final int CHESTS[] =
	{
	        27173, 27178
	};

	private static final int CHEST_LOCS[][] =
	{
	        {
	                102273, 103433, -3512
			},
			{
			        102190, 103379, -3524
			},
			{
			        102107, 103325, -3533
			},
			{
			        102024, 103271, -3500
			},
			{
			        102327, 103350, -3511
			},
			{
			        102244, 103296, -3518
			},
			{
			        102161, 103242, -3529
			},
			{
			        102078, 103188, -3500
			},
			{
			        102381, 103267, -3538
			},
			{
			        102298, 103213, -3532
			},
			{
			        102215, 103159, -3520
			},
			{
			        102132, 103105, -3513
			},
			{
			        102435, 103184, -3515
			},
			{
			        102352, 103130, -3522
			},
			{
			        102269, 103076, -3533
			},
			{
			        102186, 103022, -3541
			}
	};

	private static Map<Integer, Integer> _mobs = new HashMap<>();

	private static boolean _isArthea = false;

	public _501_ProofOfClanAlliance(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30756, 30757);
		addTalkId(30756, 30757, 30758, 30759);

		addKillId(27173, 27178, 20685, 20644, 20576);

		_mobs.putIfAbsent(20685, 3833);
		_mobs.putIfAbsent(20644, 3832);
		_mobs.putIfAbsent(20576, 3834);

		_isArthea = false;

		questItemIds = new int[]
		{
		        3833, 3832, 3834, 3835, 3837, 3872, 3873, 3889
		};
	}

	private QuestState getLeaderQuestState(Player player)
	{
		if (player.isClanLeader())
		{
			return player.getQuestState(getName());
		}

		final Clan clan = player.getClan();
		if (clan == null)
		{
			return null;
		}

		final Player leader = clan.getLeader().getPlayerInstance();
		if (leader == null)
		{
			return null;
		}

		final QuestState leaderst = leader.getQuestState(getName());
		return leaderst;
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		QuestState leaderst = null;
		final String htmltext = event;

		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		if (event.equalsIgnoreCase("chest_timer"))
		{
			_isArthea = false;
			return "";
		}

		if (player.isClanLeader())
		{
			leaderst = st;
		}
		else
		{
			leaderst = getLeaderQuestState(player);
		}

		if (leaderst == null)
		{
			return null;
		}

		if (player.isClanLeader())
		{
			if (event.equalsIgnoreCase("30756-07.htm"))
			{
				st.startQuest();
				st.set("part", "1");
			}
			else if (event.equalsIgnoreCase("30759-03.htm"))
			{
				st.setCond(2, true);
				st.set("part", "2");
				st.set("dead_list", " ");
			}
			else if (event.equalsIgnoreCase("30759-07.htm"))
			{
				st.takeItems(3837, -1);
				st.giveItems(3872, 1);
				st.set("part", "3");
				st.setCond(3, true);
				st.addNotifyOfDeath(player);
				SkillsParser.getInstance().getInfo(4082, 1).getEffects(npc, player, false);
			}
		}
		else if (event.equalsIgnoreCase("30757-05.htm"))
		{
			if (player.isClanLeader())
			{
				return "30757-05a.htm";
			}

			if (getRandom(10) > 5)
			{
				st.giveItems(3837, 1);
				final String[] deadlist = leaderst.get("dead_list").split(" ");
				leaderst.set("dead_list", joinStringArray(setNewValToArray(deadlist, player.getName(null).toLowerCase()), " "));
				return "30757-06.htm";
			}
			final Skill skill = SkillsParser.getInstance().getInfo(4083, 1);
			npc.setTarget(player);
			npc.doCast(skill);
			startQuestTimer(player.getName(null), 4000, npc, player, false);
		}
		else if (event.equalsIgnoreCase(player.getName(null)))
		{
			if (player.isDead())
			{
				st.giveItems(3837, 1);
				final String[] deadlist = leaderst.get("dead_list").split(" ");
				leaderst.set("dead_list", joinStringArray(setNewValToArray(deadlist, player.getName(null).toLowerCase()), " "));
			}
		}
		else if (event.equalsIgnoreCase("30758-03.htm"))
		{
			if (_isArthea)
			{
				return "30758-04.htm";
			}

			_isArthea = true;
			leaderst.set("part", "4");
			for (final int[] element : CHEST_LOCS)
			{
				final int rand = getRandom(5);
				addSpawn(CHESTS[0] + rand, element[0], element[1], element[2], 0, false, 300000);
				startQuestTimer("chest_timer", 60000, npc, player, false);
			}
		}
		else if (event.equalsIgnoreCase("30758-07.htm"))
		{
			if ((st.getQuestItemsCount(57) >= 10000) && !_isArthea)
			{
				st.takeItems(57, 10000);
				return "30758-08.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final String htmltext = getNoQuestMsg(talker);

		final QuestState st = talker.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		final byte id = st.getState();
		final Clan clan = talker.getClan();
		final int part = st.getInt("part");

		switch (npcId)
		{
			case 30756 :
				if (id == State.CREATED)
				{
					if (!talker.isClanLeader() || (clan == null))
					{
						return returningString("05", npcId);
					}

					final int level = clan.getLevel();
					if (level <= 2)
					{
						return returningString("01", npcId);
					}
					else if (level >= 4)
					{
						return returningString("02", npcId);
					}
					else if (level == 3)
					{
						if (st.hasQuestItems(3874))
						{
							return returningString("03", npcId);
						}
						return returningString("04", npcId);
					}
				}
				else if (id == State.STARTED)
				{
					if (!st.hasQuestItems(3873) || (part != 6))
					{
						return returningString("10", npcId);
					}
					st.takeItems(3873, 1);
					st.giveItems(3874, 1);
					st.addExpAndSp(0, 120000);
					st.exitQuest(false, true);
					return returningString("09", npcId);
				}
				break;
			case 30759 :
				if (id == State.CREATED)
				{
					final QuestState leaderst = getLeaderQuestState(talker);
					if (leaderst == null)
					{
						return "";
					}

					if (talker.isClanLeader() || (leaderst == st))
					{
						return "30759-13.htm";
					}
					else if (leaderst.getState() == State.STARTED)
					{
						return returningString("12", npcId);
					}
				}
				else if (id == State.STARTED)
				{
					final long symbol = st.getQuestItemsCount(3837);
					if (part == 1)
					{
						return returningString("01", npcId);
					}
					else if ((part == 2) && (symbol < 3))
					{
						return returningString("05", npcId);
					}
					else if (symbol == 3)
					{
						return returningString("06", npcId);
					}
					else if ((part == 5) && st.hasQuestItems(3832) && st.hasQuestItems(3833) && st.hasQuestItems(3834) && st.hasQuestItems(3835) && isAffected(talker, 4082))
					{
						st.giveItems(3873, 1);
						st.giveItems(3889, 1);

						st.takeItems(3832, -1);
						st.takeItems(3833, -1);
						st.takeItems(3834, -1);
						st.takeItems(3835, -1);

						st.set("part", "6");
						st.set("cond", "4");
						return returningString("08", npcId);
					}
					else if ((part == 3) || (part == 4) || (part == 5))
					{
						if (!isAffected(talker, 4082))
						{
							st.set("part", "1");
							st.takeItems(3872, -1);
							return returningString("09", npcId);
						}
						return returningString("10", npcId);
					}
					else if (part == 6)
					{
						return returningString("11", npcId);
					}
				}
				break;
			case 30757 :
				final QuestState leaderst = getLeaderQuestState(talker);
				if (leaderst == null)
				{
					return "";
				}

				final byte sId = leaderst.getState();
				switch (sId)
				{
					case State.STARTED:
						if (leaderst.getInt("part") != 2)
						{
							return "";
						}

						if (talker.isClanLeader() || (leaderst == st))
						{
							return returningString("02", npcId);
						}

						if (talker.getLevel() >= 40)
						{
							final String[] dlist = leaderst.get("dead_list").split(" ");
							if (dlist.length < 3)
							{
								for (final String str : dlist)
								{
									if (talker.getName(null).equalsIgnoreCase(str))
									{
										return returningString("03", npcId);
									}
								}
								return returningString("01", npcId);
							}
							return returningString("03", npcId);
						}
						return returningString("04", npcId);
					default:
						return returningString("08", npcId);
				}
			case 30758 :
				final QuestState leader_st = getLeaderQuestState(talker);
				if (leader_st == null)
				{
					return "";
				}

				final byte s_Id = leader_st.getState();
				switch (s_Id)
				{
					case State.STARTED:
						final int partA = leader_st.getInt("part");
						if ((partA == 3) && leader_st.hasQuestItems(3872) && !leader_st.hasQuestItems(3835))
						{
							return returningString("01", npcId);
						}
						else if (partA == 5)
						{
							return returningString("10", npcId);
						}
						else if (partA == 4)
						{
							if (leader_st.getInt("chest_wins") >= 4)
							{
								st.giveItems(3835, 1);
								leader_st.set("part", "5");
								return returningString("09", npcId);
							}
							return returningString("06", npcId);
						}
						break;
					default:
						break;
				}
				break;
			default:
				break;
		}
		return null;
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState leaderst = getLeaderQuestState(killer);
		if (leaderst == null || !leaderst.isStarted())
		{
			return null;
		}

		final int part = leaderst.getInt("part");
		final int npcId = npc.getId();

		if (_mobs.containsKey(npcId))
		{
			QuestState st = killer.getQuestState(getName());
			if (st == null)
			{
				st = newQuestState(killer);
			}

			if (st == leaderst)
			{
				return null;
			}

			if ((part >= 3) && (part < 6))
			{
				if (getRandom(10) == 0)
				{
					st.giveItems(_mobs.get(npcId), 1);
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}

		for (final int i : CHESTS)
		{
			QuestState st = killer.getQuestState(getName());
			if (st == null)
			{
				st = newQuestState(killer);
			}

			if (npcId == i)
			{
				if (Rnd.chance(25))
				{
					npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), 0, npc.getId(), NpcStringId.BINGO));
					int wins = leaderst.getInt("chest_wins");
					if (wins < 4)
					{
						wins += 1;
						leaderst.set("chest_wins", String.valueOf(wins));
					}
					if (wins >= 4)
					{
						st.playSound("ItemSound.quest_middle");
					}
					else
					{
						st.playSound("ItemSound.quest_itemget");
					}
				}
				return null;
			}
		}
		return null;
	}

	@Override
	public String onDeath(Creature killer, Creature victim, QuestState qs)
	{
		if (qs.getPlayer().equals(victim))
		{
			qs.exitQuest(true);
		}
		return null;
	}

	private boolean isAffected(Player player, int skillId)
	{
		return player.getFirstEffect(skillId) != null;
	}

	private static String joinStringArray(String[] s, String sep)
	{
		String ts = "";
		for (int i = 0; i < s.length; i++)
		{
			if (i == (s.length - 1))
			{
				ts += s[i];
			}
			else
			{
				ts += s[i] + sep;
			}
		}
		return ts;
	}

	public static String[] setNewValToArray(String[] s, String s1)
	{
		final String[] ts = new String[s.length + 1];
		for (int i = 0; i < s.length; i++)
		{
			ts[i] = s[i];
		}
		ts[s.length] = s1;
		return ts;
	}

	private static String returningString(String s, int npcId)
	{
		return String.valueOf(npcId) + "-" + s + ".htm";
	}

	public static void main(String[] args)
	{
		new _501_ProofOfClanAlliance(501, _501_ProofOfClanAlliance.class.getSimpleName(), "");
	}
}
