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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 13.12.2019
 */
public class _060_GoodWorkReward extends Quest
{
	private static final Map<Integer, String> _profs = new HashMap<>();
	private static final Map<String, int[]> _classes = new HashMap<>();
	
	public Npc _pursuer;

	public _060_GoodWorkReward(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(31435);
		addTalkId(31435, 30081, 31092, 32487);
		
		addKillId(27340);

		questItemIds = new int[]
		{
		        10867, 10868
		};
	}

	static
	{
		_profs.put(1, "classId-1");
		_profs.put(4, "classId-4");
		_profs.put(7, "classId-7");
		_profs.put(11, "classId-11");
		_profs.put(15, "classId-15");
		_profs.put(19, "classId-19");
		_profs.put(22, "classId-22");
		_profs.put(26, "classId-26");
		_profs.put(29, "classId-29");
		_profs.put(32, "classId-32");
		_profs.put(35, "classId-35");
		_profs.put(39, "classId-39");
		_profs.put(42, "classId-42");
		_profs.put(45, "classId-45");
		_profs.put(47, "classId-47");
		_profs.put(50, "classId-50");
		_profs.put(54, "classId-54");
		_profs.put(56, "classId-56");

		_classes.put("AW", new int[]
		{
		                2673,
		                3172,
		                2809
		});
		_classes.put("BD", new int[]
		{
		                2627,
		                3172,
		                2762
		});
		_classes.put("BH", new int[]
		{
		                2809,
		                3119,
		                3238
		});
		_classes.put("BS", new int[]
		{
		                2721,
		                2734,
		                2820
		});
		_classes.put("DA", new int[]
		{
		                2633,
		                2734,
		                3307
		});
		_classes.put("DT", new int[]
		{
		                2627,
		                3203,
		                3276
		});
		_classes.put("EE", new int[]
		{
		                2721,
		                3140,
		                2820
		});
		_classes.put("ES", new int[]
		{
		                2674,
		                3140,
		                3336
		});
		_classes.put("GL", new int[]
		{
		                2627,
		                2734,
		                2762
		});
		_classes.put("HK", new int[]
		{
		                2673,
		                2734,
		                3293
		});
		_classes.put("NM", new int[]
		{
		                2674,
		                2734,
		                3307
		});
		_classes.put("OL", new int[]
		{
		                2721,
		                3203,
		                3390
		});
		_classes.put("PA", new int[]
		{
		                2633,
		                2734,
		                2820
		});
		_classes.put("PP", new int[]
		{
		                2721,
		                2734,
		                2821
		});
		_classes.put("PR", new int[]
		{
		                2673,
		                3172,
		                3293
		});
		_classes.put("PS", new int[]
		{
		                2674,
		                3172,
		                3336
		});
		_classes.put("PW", new int[]
		{
		                2673,
		                3140,
		                2809
		});
		_classes.put("SC", new int[]
		{
		                2674,
		                2734,
		                2840
		});
		_classes.put("SE", new int[]
		{
		                2721,
		                3172,
		                2821
		});
		_classes.put("SH", new int[]
		{
		                2674,
		                3172,
		                2840
		});
		_classes.put("SK", new int[]
		{
		                2633,
		                3172,
		                3307
		});
		_classes.put("SP", new int[]
		{
		                2674,
		                3140,
		                2840
		});
		_classes.put("SR", new int[]
		{
		                2673,
		                3140,
		                3293
		});
		_classes.put("SS", new int[]
		{
		                2627,
		                3140,
		                2762
		});
		_classes.put("TH", new int[]
		{
		                2673,
		                2734,
		                2809
		});
		_classes.put("TK", new int[]
		{
		                2633,
		                3140,
		                2820
		});
		_classes.put("TR", new int[]
		{
		                2627,
		                3203,
		                2762
		});
		_classes.put("WA", new int[]
		{
		                2674,
		                2734,
		                3336
		});
		_classes.put("WC", new int[]
		{
		                2721,
		                3203,
		                2879
		});
		_classes.put("WL", new int[]
		{
		                2627,
		                2734,
		                3276
		});
		_classes.put("WS", new int[]
		{
		                2867,
		                3119,
		                3238
		});
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

		final int cond = st.getCond();

		if (event.equalsIgnoreCase("31435-03.htm") && npc.getId() == 31435)
		{
			if (st.getState() == State.CREATED)
			{
				st.startQuest();
			}
		}
		else if (event.equalsIgnoreCase("32487-02.htm") && npc.getId() == 32487)
		{
			if (cond == 1)
			{
				despawnPursuer(st);
				spawnPursuer(st);
				_pursuer.setRunning();
				((Attackable) _pursuer).addDamageHate(player, 0, 999);
				_pursuer.getAI().setIntention(CtrlIntention.ATTACK, player);
			}
		}
		else if (event.equalsIgnoreCase("31435-05.htm") && npc.getId() == 31435)
		{
			if (cond == 3)
			{
				st.setCond(4, true);
			}
		}
		else if (event.equalsIgnoreCase("30081-03.htm") && npc.getId() == 30081)
		{
			if (cond == 4)
			{
				if (st.getQuestItemsCount(10867) < 1)
				{
					return "30081-03a.htm";
				}
				st.takeItems(10867, -1);
				st.setCond(5, true);
			}
		}
		else if (event.equalsIgnoreCase("30081-05.htm") && npc.getId() == 30081)
		{
			if (cond == 5)
			{
				st.setCond(6, true);
			}
		}
		else if (event.equalsIgnoreCase("30081-08.htm") && npc.getId() == 30081)
		{
			if ((cond == 5) || (cond == 6))
			{
				if (st.getQuestItemsCount(57) < 3000000)
				{
					st.setCond(6, true);
					return "30081-07.htm";
				}
				st.takeItems(57, 3000000);
				st.giveItems(10868, 1);
				st.setCond(7, true);
			}
		}
		else if (event.equalsIgnoreCase("32487-06.htm") && npc.getId() == 32487)
		{
			if (cond == 7)
			{
				if (st.getQuestItemsCount(10868) < 1)
				{
					return "32487-06a.htm";
				}
				st.takeItems(10868, -1);
				st.setCond(8, true);
			}
		}
		else if (event.equalsIgnoreCase("31435-08.htm") && npc.getId() == 31435)
		{
			if (cond == 8)
			{
				st.setCond(9, true);
			}
		}
		else if (event.equalsIgnoreCase("31092-05.htm") && npc.getId() == 31092)
		{
			if ((cond == 10) && _profs.containsKey(player.getClassId().getId()))
			{
				return htmltext = "" + _profs.get(player.getClassId().getId()) + ".htm";
			}
		}
		else if (event.startsWith("classes-") && npc.getId() == 31092)
		{
			if (cond == 10)
			{
				final String occupation = event.replaceAll("classes-", "");
				final int[] classes = _classes.get(occupation);
				if (classes == null)
				{
					return "Error id: " + occupation;
				}

				int adena = 0;
				for (final int mark : classes)
				{
					if (st.getQuestItemsCount(mark) > 0)
					{
						adena++;
					}
					else
					{
						st.giveItems(mark, 1);
					}
				}

				if (adena > 0)
				{
					st.calcRewardPerItem(getId(), 1, adena);
				}
				st.exitQuest(false, true);
				return "31092-06.htm";
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

		final int cond = st.getCond();
		final int npcId = npc.getId();
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == 31435)
				{
					if ((player.getLevel() < getMinLvl(getId())) || (player.getRace() == Race.Kamael) || (player.getClassId().level() != 1))
					{
						st.exitQuest(true);
						htmltext = "31435-00.htm";
					}
					else
					{
						htmltext = "31435-01.htm";
					}
				}
				break;
			case State.STARTED:
				switch (npcId)
				{
					case 31435 :
						switch (cond)
						{
							case 1:
							case 2:
								htmltext = "31435-03.htm";
								break;
							case 3:
								htmltext = "31435-04.htm";
								break;
							case 4:
							case 5:
							case 6:
							case 7:
								htmltext = "31435-06.htm";
								break;
							case 8:
								htmltext = "31435-07.htm";
								break;
							case 9:
								st.setCond(10, true);
								htmltext = "31435-09.htm";
								break;
							default:
								if (cond > 9)
								{
									htmltext = "31435-10.htm";
								}
								break;
						}
						break;
					case 30081 :
						switch (cond)
						{
							case 4:
								htmltext = "30081-01.htm";
								break;
							case 5:
								htmltext = "30081-04.htm";
								break;
							case 6:
								htmltext = "30081-06.htm";
								break;
							default:
								if (cond > 6)
								{
									htmltext = "30081-09.htm";
								}
								break;
						}
						break;
					case 31092 :
						switch (cond)
						{
							case 10:
								htmltext = "31092-01.htm";
								break;
						}
						break;
					case 32487 :
						switch (cond)
						{
							case 1:
								htmltext = "32487-01.htm";
								break;
							case 2:
								st.setCond(3, true);
								htmltext = "32487-03.htm";
								break;
							case 3:
								htmltext = "32487-04.htm";
								break;
							case 7:
								htmltext = "32487-05.htm";
								break;
							default:
								if (cond > 7)
								{
									htmltext = "32487-06.htm";
								}
								break;
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
		final QuestState st = player.getQuestState(getName());
		if (st != null)
		{
			if (st.isCond(1))
			{
				if (st.calcDropItems(getId(), 10867, npc.getId(), 1))
				{
					npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), 0, npc.getId(), NpcStringId.YOU_HAVE_GOOD_LUCK_I_SHALL_RETURN));
					st.setCond(2);
				}
				else
				{
					npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), 0, npc.getId(), NpcStringId.YOU_ARE_STRONG_THIS_WAS_A_MISTAKE));
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	private void spawnPursuer(QuestState st)
	{
		_pursuer = st.addSpawn(27340, 72590, 148100, -3312, getRandom(0, 20), true, 1800000);
		_pursuer.broadcastPacketToOthers(2000, new NpcSay(_pursuer.getObjectId(), 0, _pursuer.getId(), NpcStringId.S1_I_MUST_KILL_YOU_BLAME_YOUR_OWN_CURIOSITY));
		_pursuer.getAI().setIntention(CtrlIntention.ATTACK, st.getPlayer());
	}

	private void despawnPursuer(QuestState st)
	{
		if (_pursuer != null)
		{
			_pursuer.deleteMe();
		}
		_pursuer = null;
	}

	public static void main(String[] args)
	{
		new _060_GoodWorkReward(60, _060_GoodWorkReward.class.getSimpleName(), "");
	}
}