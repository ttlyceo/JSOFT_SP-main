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

import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;

/**
 * Rework by LordWinter 29.05.2021
 */
public class _417_PathToScavenger extends Quest
{
	private static int _killsAmount;
	
	public _417_PathToScavenger(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(30524);
		addTalkId(30524, 30316, 30517, 30519, 30525, 30538, 30556, 30557);

		addSkillSeeId(20403, 20508, 27058);
		
		addKillId(20403, 27058, 20508, 20777);
		
		_killsAmount = getQuestParams(questId).getInteger("hunterBearAmount");

		questItemIds = new int[]
		{
		        1643, 1644, 1645, 1646, 1647, 1648, 1649, 1650, 1651, 1652, 1653, 1654, 1655, 1656, 1657
		};
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null || st.isCompleted())
		{
			return super.onAdvEvent(event, npc, player);
		}

		final int level = player.getLevel();
		final int classId = player.getClassId().getId();
		if (event.equalsIgnoreCase("1"))
		{
			st.set("id", "0");
			if ((level >= 18) && (classId == 0x35) && (st.getQuestItemsCount(1642) == 0))
			{
				st.giveItems(1643, 1);
				st.startQuest();
				htmltext = "30524-05.htm";
			}
			else if (classId != 0x35)
			{
				htmltext = classId == 0x36 ? "30524-02a.htm" : "30524-08.htm";
			}
			else if ((level < 18) && (classId == 0x35))
			{
				htmltext = "30524-02.htm";
			}
			else if ((level >= 18) && (classId == 0x35) && (st.getQuestItemsCount(1642) == 1))
			{
				htmltext = "30524-04.htm";
			}
		}
		else if (event.equalsIgnoreCase("30519_1"))
		{
			if (st.getQuestItemsCount(1643) > 0)
			{
				st.takeItems(1643, 1);
				st.setCond(2, true);
				final int n = st.getRandom(3);
				if (n == 0)
				{
					st.giveItems(1649, 1);
					htmltext = "30519-02.htm";
				}
				else if (n == 1)
				{
					st.giveItems(1648, 1);
					htmltext = "30519-03.htm";
				}
				else if (n == 2)
				{
					st.giveItems(1647, 1);
					htmltext = "30519-04.htm";
				}
			}
			else
			{
				htmltext = Quest.getNoQuestMsg(player);
			}
		}
		else if (event.equalsIgnoreCase("30519_2"))
		{
			htmltext = "30519-06.htm";
		}
		else if (event.equalsIgnoreCase("30519_3"))
		{
			htmltext = "30519-07.htm";
			st.set("id", String.valueOf(st.getInt("id") + 1));
		}
		else if (event.equalsIgnoreCase("30519_4"))
		{
			htmltext = st.getRandom(2) == 0 ? "30519-06.htm" : "30519-11.htm";
		}
		else if (event.equalsIgnoreCase("30519_5"))
		{
			if ((st.getQuestItemsCount(1649) > 0) || (st.getQuestItemsCount(1648) > 0) || (st.getQuestItemsCount(1647) > 0))
			{
				if ((st.getInt("id") / 10) < 2)
				{
					st.set("id", String.valueOf(st.getInt("id") + 1));
					htmltext = "30519-07.htm";
				}
				else if (((st.getInt("id") / 10) >= 2) && (st.getInt("cond") == 0))
				{
					if ((st.getInt("id") / 10) < 3)
					{
						st.set("id", String.valueOf(st.getInt("id") + 1));
					}
					htmltext = "30519-09.htm";
				}
				else if (((st.getInt("id") / 10) >= 3) && (st.getInt("cond") > 0))
				{
					st.giveItems(1646, 1);
					st.takeItems(1648, 1);
					st.takeItems(1649, 1);
					st.takeItems(1647, 1);
					htmltext = "30519-10.htm";
				}
			}
			else
			{
				htmltext = Quest.getNoQuestMsg(player);
			}
		}
		else if (event.equalsIgnoreCase("30519_6"))
		{
			if ((st.getQuestItemsCount(1652) > 0) || (st.getQuestItemsCount(1651) > 0) || (st.getQuestItemsCount(1650) > 0))
			{
				final int n = st.getRandom(3);
				st.takeItems(1652, 1);
				st.takeItems(1651, 1);
				st.takeItems(1650, 1);
				if (n == 0)
				{
					st.giveItems(1649, 1);
					htmltext = "30519-02.htm";
				}
				else if (n == 1)
				{
					st.giveItems(1648, 1);
					htmltext = "30519-03.htm";
				}
				else if (n == 2)
				{
					st.giveItems(1647, 1);
					htmltext = "30519-04.htm";
				}
			}
			else
			{
				htmltext = Quest.getNoQuestMsg(player);
			}
		}
		else if (event.equalsIgnoreCase("30316_1"))
		{
			if (st.getQuestItemsCount(1657) > 0)
			{
				st.takeItems(1657, 1);
				st.giveItems(1644, 1);
				st.setCond(10, true);
				htmltext = "30316-02.htm";
			}
			else
			{
				htmltext = Quest.getNoQuestMsg(player);
			}
		}
		else if (event.equalsIgnoreCase("30316_2"))
		{
			if (st.getQuestItemsCount(1657) > 0)
			{
				st.takeItems(1657, 1);
				st.giveItems(1644, 1);
				st.setCond(10, true);
				htmltext = "30316-03.htm";
			}
			else
			{
				htmltext = Quest.getNoQuestMsg(player);
			}
		}
		else if (event.equalsIgnoreCase("30557_1"))
		{
			htmltext = "30557-02.htm";
		}
		else if (event.equalsIgnoreCase("30557_2"))
		{
			if (st.getQuestItemsCount(1644) > 0)
			{
				st.takeItems(1644, 1);
				st.giveItems(1645, 1);
				st.setCond(11, true);
				htmltext = "30557-03.htm";
			}
			else
			{
				htmltext = Quest.getNoQuestMsg(player);
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player talker)
	{
		String htmltext = Quest.getNoQuestMsg(talker);
		final QuestState st = talker.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		final int state = st.getState();
		if ((npcId != 30524) && (state != State.STARTED))
		{
			return htmltext;
		}

		final int cond = st.getCond();
		if ((npcId == 30524) && (cond == 0))
		{
			htmltext = "30524-01.htm";
		}
		else if ((npcId == 30524) && (cond > 0) && (st.getQuestItemsCount(1643) > 0))
		{
			htmltext = "30524-06.htm";
		}
		else if ((npcId == 30524) && (cond > 0) && (st.getQuestItemsCount(1643) == 0))
		{
			htmltext = "30524-07.htm";
		}
		else if ((npcId == 30519) && (cond > 0) && (st.getQuestItemsCount(1643) > 0))
		{
			htmltext = "30519-01.htm";
		}
		else if ((npcId == 30519) && (cond > 0) && ((st.getQuestItemsCount(1648) + st.getQuestItemsCount(1647) + st.getQuestItemsCount(1649)) == 1) && ((st.getInt("id") / 10) == 0))
		{
			htmltext = "30519-05.htm";
		}
		else if ((npcId == 30519) && (cond > 0) && ((st.getQuestItemsCount(1648) + st.getQuestItemsCount(1647) + st.getQuestItemsCount(1649)) == 1) && ((st.getInt("id") / 10) > 0))
		{
			htmltext = "30519-08.htm";
		}
		else if ((npcId == 30519) && (cond > 0) && ((st.getQuestItemsCount(1651) + st.getQuestItemsCount(1650) + st.getQuestItemsCount(1652)) == 1) && (st.getInt("id") < 50))
		{
			htmltext = "30519-12.htm";
		}
		else if ((npcId == 30519) && (cond > 0) && ((st.getQuestItemsCount(1651) + st.getQuestItemsCount(1650) + st.getQuestItemsCount(1652)) == 1) && (st.getInt("id") >= 50))
		{
			st.giveItems(1646, 1);
			st.takeItems(1651, 1);
			st.takeItems(1652, 1);
			st.takeItems(1650, 1);
			st.setCond(4, true);
			htmltext = "30519-15.htm";
		}
		else if ((npcId == 30519) && (cond > 0) && (st.getQuestItemsCount(1646) > 0))
		{
			htmltext = "30519-13.htm";
		}
		else if ((npcId == 30519) && (cond > 0) && ((st.getQuestItemsCount(1653) > 0) || (st.getQuestItemsCount(1654) > 0) || (st.getQuestItemsCount(1657) > 0) || (st.getQuestItemsCount(1644) > 0) || (st.getQuestItemsCount(1645) > 0)))
		{
			htmltext = "30519-14.htm";
		}
		else if ((npcId == 30517) && (cond > 0) && (st.getQuestItemsCount(1648) == 1) && (st.getInt("id") < 20))
		{
			st.takeItems(1648, 1);
			st.giveItems(1651, 1);
			if (st.getInt("id") >= 50)
			{
				st.setCond(3, true);
			}
			st.set("id", String.valueOf(st.getInt("id") + 10));
			htmltext = "30517-01.htm";
		}
		else if ((npcId == 30517) && (cond > 0) && (st.getQuestItemsCount(1648) == 1) && (st.getInt("id") >= 20))
		{
			st.takeItems(1648, 1);
			st.giveItems(1651, 1);
			if (st.getInt("id") >= 50)
			{
				st.setCond(3, true);
			}
			st.set("id", String.valueOf(st.getInt("id") + 10));
			htmltext = "30517-02.htm";
		}

		else if ((npcId == 30517) && (cond > 0) && (st.getQuestItemsCount(1651) == 1))
		{
			htmltext = "30517-03.htm";
		}
		else if ((npcId == 30525) && (cond > 0) && (st.getQuestItemsCount(1647) == 1) && (st.getInt("id") < 20))
		{
			st.takeItems(1647, 1);
			st.giveItems(1650, 1);
			if (st.getInt("id") >= 50)
			{
				st.setCond(3, true);
			}
			st.set("id", String.valueOf(st.getInt("id") + 10));
			htmltext = "30525-01.htm";
		}
		else if ((npcId == 30525) && (cond > 0) && (st.getQuestItemsCount(1647) == 1) && (st.getInt("id") >= 20))
		{
			st.takeItems(1647, 1);
			st.giveItems(1650, 1);
			if (st.getInt("id") >= 50)
			{
				st.setCond(3, true);
			}
			st.set("id", String.valueOf(st.getInt("id") + 10));
			htmltext = "30525-02.htm";
		}
		else if ((npcId == 30525) && (cond > 0) && (st.getQuestItemsCount(1650) == 1))
		{
			htmltext = "30525-03.htm";
		}
		else if ((npcId == 30538) && (cond > 0) && (st.getQuestItemsCount(1649) == 1) && (st.getInt("id") < 20))
		{
			st.takeItems(1649, 1);
			st.giveItems(1652, 1);
			if (st.getInt("id") >= 50)
			{
				st.setCond(3, true);
			}
			st.set("id", String.valueOf(st.getInt("id") + 10));
			htmltext = "30538-01.htm";
		}
		else if ((npcId == 30538) && (cond > 0) && (st.getQuestItemsCount(1649) == 1) && (st.getInt("id") >= 20))
		{
			st.takeItems(1649, 1);
			st.giveItems(1652, 1);
			if (st.getInt("id") >= 50)
			{
				st.setCond(3, true);
			}
			st.set("id", String.valueOf(st.getInt("id") + 10));
			htmltext = "30538-02.htm";
		}
		else if ((npcId == 30538) && (cond > 0) && (st.getQuestItemsCount(1652) == 1))
		{
			htmltext = "30538-03.htm";
		}
		else if ((npcId == 30556) && (cond > 0) && (st.getQuestItemsCount(1646) == 1))
		{
			st.takeItems(1646, 1);
			st.giveItems(1653, 1);
			st.setCond(5, true);
			st.set("id", String.valueOf(0));
			htmltext = "30556-01.htm";
		}
		else if ((npcId == 30556) && (cond > 0) && (st.getQuestItemsCount(1653) == 1) && (st.getQuestItemsCount(1655) < 5))
		{
			htmltext = "30556-02.htm";
		}
		else if ((npcId == 30556) && (cond > 0) && (st.getQuestItemsCount(1653) == 1) && (st.getQuestItemsCount(1655) >= 5))
		{
			st.takeItems(1655, st.getQuestItemsCount(1655));
			st.takeItems(1653, 1);
			st.giveItems(1654, 1);
			st.setCond(7, true);
			htmltext = "30556-03.htm";
		}
		else if ((npcId == 30556) && (cond > 0) && (st.getQuestItemsCount(1654) == 1) && (st.getQuestItemsCount(1656) < 20))
		{
			htmltext = "30556-04.htm";
		}
		else if ((npcId == 30556) && (cond > 0) && (st.getQuestItemsCount(1654) == 1) && (st.getQuestItemsCount(1656) >= 20))
		{
			st.takeItems(1656, st.getQuestItemsCount(1656));
			st.takeItems(1654, 1);
			st.giveItems(1657, 1);
			st.setCond(9, true);
			htmltext = "30556-05.htm";
		}
		else if ((npcId == 30556) && (cond > 0) && (st.getQuestItemsCount(1657) > 0))
		{
			htmltext = "30556-06.htm";
		}
		else if ((npcId == 30556) && (cond > 0) && ((st.getQuestItemsCount(1644) > 0) || (st.getQuestItemsCount(1645) > 0)))
		{
			htmltext = "30556-07.htm";
		}
		else if ((npcId == 30316) && (cond > 0) && (st.getQuestItemsCount(1657) == 1))
		{
			htmltext = "30316-01.htm";
		}
		else if ((npcId == 30316) && (cond > 0) && (st.getQuestItemsCount(1644) == 1))
		{
			htmltext = "30316-04.htm";
		}
		else if ((npcId == 30316) && (cond > 0) && (st.getQuestItemsCount(1645) == 1))
		{
			st.takeItems(1645, 1);
			final String done = st.getGlobalQuestVar("1ClassQuestFinished");
			if (done == null || done.isEmpty())
			{
				st.calcExpAndSp(getId());
				st.calcReward(getId(), 1);
				st.saveGlobalQuestVar("1ClassQuestFinished", "1");
			}
			st.calcReward(getId(), 2);
			st.exitQuest(false, true);
			htmltext = "30316-05.htm";
		}
		else if ((npcId == 30557) && (cond > 0) && (st.getQuestItemsCount(1644) == 1))
		{
			htmltext = "30557-01.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		final QuestState st = caster.getQuestState(getName());
		if (st == null)
		{
			return super.onSkillSee(npc, caster, skill, targets, isSummon);
		}
		
		switch (npc.getId())
		{
			case 20403 :
			case 20508 :
			case 27058 :
				if (skill.getId() == 254 && npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
				}
				break;
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState st = killer.getQuestState(getName());
		if (st == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		if (st != null && st.isStarted())
		{
			switch (npc.getId())
			{
				case 20777 :
					if ((st.getQuestItemsCount(1653) == 1) && (st.getQuestItemsCount(1655) < 5))
					{
						if (st.getInt("id") >= _killsAmount)
						{
							st.addSpawn(27058);
							st.set("id", "0");
						}
						else
						{
							st.set("id", String.valueOf(st.getInt("id") + 1));
						}
					}
					break;
				case 27058 :
					if ((st.getQuestItemsCount(1653) == 1) && npc.isScriptValue(1))
					{
						if (st.calcDropItems(getId(), 1655, npc.getId(), 5))
						{
							st.setCond(6, true);
						}
					}
					break;
				case 20403 :
				case 20508 :
					if ((st.getQuestItemsCount(1654) == 1) && npc.isScriptValue(1))
					{
						if (st.calcDropItems(getId(), 1656, npc.getId(), 20))
						{
							st.setCond(8, true);
						}
					}
					break;
			}
		}
		return super.onKill(npc, killer, isSummon);
	}

	public static void main(String[] args)
	{
		new _417_PathToScavenger(417, _417_PathToScavenger.class.getSimpleName(), "");
	}
}
