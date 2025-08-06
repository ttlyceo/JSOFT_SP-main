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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 05.12.2019
 */
public class _021_HiddenTruth extends Quest
{
	private Npc _ghostPage;
	private Npc _ghost;

	public _021_HiddenTruth(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(31522);
		addTalkId(31522, 31523, 31524, 31524, 31525, 31526, 31348, 31349, 31350, 31328);

		questItemIds = new int[]
		{
		        7140
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

		if (event.equalsIgnoreCase("31522-02.htm"))
		{
			st.startQuest();
		}
		else if (event.equalsIgnoreCase("31328-05.htm"))
		{
			if (st.getQuestItemsCount(7140) != 0)
			{
				htmltext = "31328-05a.htm";
				st.takeItems(7140, 1);
				if (st.getQuestItemsCount(7141) == 0)
				{
					st.giveItems(7141, 1);
				}
				st.calcExpAndSp(getId());
				st.exitQuest(false, true);
			}
		}
		else if (event.equalsIgnoreCase("31523-03.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
				despawnGhost(st);
				spawnGhost(st);
			}
		}
		else if (event.equalsIgnoreCase("31524-06.htm"))
		{
			if (st.isCond(2))
			{
				st.setCond(3, true);
				despawnGhostPage(st);
				spawnGhostPage(st);
				startQuestTimer("1", 4000, _ghostPage, player);
			}
		}
		else if (event.equalsIgnoreCase("31526-03.htm"))
		{
			st.playSound("ItemSound.item_drop_equip_armor_cloth");
		}
		else if (event.equalsIgnoreCase("31526-08.htm"))
		{
			st.playSound("AmdSound.ed_chimes_05");
			st.setCond(5, false);
		}
		else if (event.equalsIgnoreCase("31526-14.htm"))
		{
			st.giveItems(7140, 1);
			st.setCond(6, true);
		}
		else if (event.equalsIgnoreCase("1"))
		{
			if (_ghostPage != null)
			{
				_ghostPage.getAI().setIntention(CtrlIntention.MOVING, new Location(52373, -54296, -3136, 0), 0);
				st.startQuestTimer("2", 5000, _ghostPage);
			}
		}
		else if (event.equalsIgnoreCase("2"))
		{
			if (_ghostPage != null)
			{
				_ghostPage.getAI().setIntention(CtrlIntention.MOVING, new Location(52451, -52921, -3152, 0), 0);
				st.startQuestTimer("3", 12000, _ghostPage);
			}
		}
		else if (event.equalsIgnoreCase("3"))
		{
			if (_ghostPage != null)
			{
				_ghostPage.getAI().setIntention(CtrlIntention.MOVING, new Location(51909, -51725, -3125, 0), 0);
				st.startQuestTimer("4", 15000, _ghostPage);
			}
		}
		else if (event.equalsIgnoreCase("4"))
		{
			if (_ghostPage != null)
			{
				_ghostPage.getAI().setIntention(CtrlIntention.MOVING, new Location(52438, -51240, -3097, 0), 0);
				st.startQuestTimer("5", 5000, _ghostPage);
			}
		}
		else if (event.equalsIgnoreCase("5"))
		{
			if (_ghostPage != null)
			{
				_ghostPage.getAI().setIntention(CtrlIntention.MOVING, new Location(52143, -51418, -3085, 0), 0);
			}
			return null;
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
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (player.getLevel() >= getMinLvl(getId()))
				{
					htmltext = "31522-01.htm";
				}
				else
				{
					htmltext = "31522-03.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case 31522 :
						if (cond == 1)
						{
							htmltext = "31522-05.htm";
						}
						break;
					case 31523 :
						switch (cond)
						{
							case 1:
								htmltext = "31523-01.htm";
								break;
							case 2:
							case 3:
								htmltext = "31523-04.htm";
								st.playSound("SkillSound5.horror_02");
								despawnGhost(st);
								spawnGhost(st);
								break;
						}
						break;
					case 31524 :
						switch (cond)
						{
							case 2:
								htmltext = "31524-01.htm";
								break;
							case 3:
								htmltext = "31524-07b.htm";
								break;
							case 4:
								htmltext = "31524-07c.htm";
								break;
						}
						break;
					case 31525 :
						switch (cond)
						{
							case 3:
							case 4:
								htmltext = "31525-01.htm";

								if (!_ghostPage.isMoving())
								{
									htmltext = "31525-02.htm";
									if (cond == 3)
									{
										st.setCond(4, true);
									}
								}
								else
								{
									return "31525-01.htm";
								}
								break;
						}
						break;
					case 31526 :
						switch (cond)
						{
							case 3:
							case 4:
								htmltext = "31525-01.htm";

								if (!_ghostPage.isMoving())
								{
									despawnGhostPage(st);
									despawnGhost(st);
									st.setCond(5, true);
									htmltext = "31526-01.htm";
								}

								break;
							case 5:
								htmltext = "31526-10.htm";
								st.playSound("AmdSound.ed_chimes_05");
								break;
							case 6:
								htmltext = "31526-15.htm";
								break;
						}
						break;
					case 31348 :
						if (st.getQuestItemsCount(7140) >= 1)
						{
							switch (cond)
							{
								case 6:
									if ((st.getInt("DOMINIC") == 1) && (st.getInt("BENEDICT") == 1))
									{
										htmltext = "31348-02.htm";
										st.setCond(7, true);
									}
									else
									{
										st.set("AGRIPEL", "1");
										htmltext = "31348-0" + getRandom(3) + ".htm";
									}
									break;
								case 7:
									htmltext = "31348-03.htm";
									break;
							}
						}
						break;
					case 31350 :
						if (st.getQuestItemsCount(7140) >= 1)
						{
							switch (cond)
							{
								case 6:
									if ((st.getInt("AGRIPEL") == 1) && (st.getInt("BENEDICT") == 1))
									{
										htmltext = "31350-02.htm";
										st.setCond(7, true);
									}
									else
									{
										st.set("DOMINIC", "1");
										htmltext = "31350-0" + getRandom(3) + ".htm";
									}
									break;
								case 7:
									htmltext = "31350-03.htm";
									break;
							}
						}
						break;
					case 31349 :
						if (st.getQuestItemsCount(7140) >= 1)
						{
							switch (cond)
							{
								case 6:
									if ((st.getInt("AGRIPEL") == 1) && (st.getInt("DOMINIC") == 1))
									{
										htmltext = "31349-02.htm";
										st.setCond(7, true);
									}
									else
									{
										st.set("BENEDICT", "1");
										htmltext = "31349-0" + getRandom(3) + ".htm";
									}
									break;
								case 7:
									htmltext = "31349-03.htm";
									break;
							}
						}
						break;
					case 31328 :
						switch (cond)
						{
							case 0:
								htmltext = "31328-06.htm";
								break;
							case 7:
								if (st.getQuestItemsCount(7140) != 0)
								{
									htmltext = "31328-01.htm";
								}
								break;
						}
						break;
				}
				break;
		}
		return htmltext;
	}
	
	private void spawnGhostPage(QuestState st)
	{
		_ghostPage = st.addSpawn(31525, 51462, -54539, -3176, getRandom(0, 20), true, 0);
		final NpcSay packet = new NpcSay(_ghostPage.getObjectId(), Say2.NPC_ALL, _ghostPage.getId(), NpcStringId.MY_MASTER_HAS_INSTRUCTED_ME_TO_BE_YOUR_GUIDE_S1);
		packet.addStringParameter(st.getPlayer().getName(null).toString());
		_ghostPage.broadcastPacketToOthers(2000, packet);
	}
	
	private void despawnGhostPage(QuestState st)
	{
		if (_ghostPage != null)
		{
			_ghostPage.deleteMe();
		}
		_ghostPage = null;
	}

	private void spawnGhost(QuestState st)
	{
		_ghost = st.addSpawn(31524, 51432, -54570, -3136, getRandom(0, 20), false, 0);
		_ghost.broadcastPacketToOthers(2000, new NpcSay(_ghost.getObjectId(), 0, _ghost.getId(), NpcStringId.WHO_AWOKE_ME));
	}
	
	private void despawnGhost(QuestState st)
	{
		if (_ghost != null)
		{
			_ghost.deleteMe();
		}
		_ghost = null;
	}

	public static void main(String[] args)
	{
		new _021_HiddenTruth(21, _021_HiddenTruth.class.getSimpleName(), "");
	}
}
