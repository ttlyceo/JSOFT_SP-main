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

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 12.12.2021
 */
public abstract class SagasSuperClass extends Quest
{
	protected int[] _npcs;
	protected int[] _items;
	protected int[] _mobs;
	protected int[] _classId;
	protected int[] _prevClass;
	protected Location[] _npcSpawnLoc;
	protected String[] _msgs;
	private static final Map<Npc, Integer> _spawnList = new HashMap<>();

	private static int[] QuestClass[] =
	{
	        {
	                0x7f
			},
			{
			        0x80, 0x81
			},
			{
			        0x82
			},
			{
			        0x05
			},
			{
			        0x14
			},
			{
			        0x15
			},
			{
			        0x02
			},
			{
			        0x03
			},
			{
			        0x2e
			},
			{
			        0x30
			},
			{
			        0x33
			},
			{
			        0x34
			},
			{
			        0x08
			},
			{
			        0x17
			},
			{
			        0x24
			},
			{
			        0x09
			},
			{
			        0x18
			},
			{
			        0x25
			},
			{
			        0x10
			},
			{
			        0x11
			},
			{
			        0x1e
			},
			{
			        0x0c
			},
			{
			        0x1b
			},
			{
			        0x28
			},
			{
			        0x0e
			},
			{
			        0x1c
			},
			{
			        0x29
			},
			{
			        0x0d
			},
			{
			        0x06
			},
			{
			        0x22
			},
			{
			        0x21
			},
			{
			        0x2b
			},
			{
			        0x37
			},
			{
			        0x39
			}
	};

	public SagasSuperClass(int id, String name, String descr)
	{
		super(id, name, descr);
	}

	public void registerNPCs()
	{
		addStartNpc(_npcs[0]);
		for (final int npc : _npcs)
		{
			addTalkId(npc);
		}
		addFirstTalkId(_npcs[4]);
		
		addAttackId(_mobs[2]);
		addAttackId(_mobs[1]);
		
		addSkillSeeId(_mobs[1]);
		
		for (final int mobid : _mobs)
		{
			addKillId(mobid);
		}
		for (int i = 21646; i < 21652; i++)
		{
			addKillId(i);
		}
		for (int i = 27214; i < 27217; i++)
		{
			addKillId(i);
		}
		addKillId(18212, 18214, 18215, 18216, 18218);
		
		questItemIds = _items.clone();
		questItemIds[0] = 0;
		questItemIds[2] = 0;
	}

	private static void cast(Npc npc, Creature target, int skillId, int level)
	{
		target.broadcastPacket(new MagicSkillUse(target, target, skillId, level, 6000, 1));
		target.broadcastPacket(new MagicSkillUse(npc, npc, skillId, level, 6000, 1));
	}

	private static void autoChat(Npc npc, String text)
	{
		npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), 0, npc.getId(), text));
	}

	private static void addSpawn(QuestState st, Npc mob)
	{
		_spawnList.put(mob, st.getPlayer().getObjectId());
	}

	private static Npc FindSpawn(Player player, Npc npc)
	{
		if (_spawnList.containsKey(npc) && (_spawnList.get(npc) == player.getObjectId()))
		{
			return npc;
		}
		return null;
	}

	private static void DeleteSpawn(QuestState st, Npc npc)
	{
		if (_spawnList.containsKey(npc))
		{
			_spawnList.remove(npc);
			npc.deleteMe();
		}
	}

	private QuestState findRightState(Npc npc)
	{
		Player player = null;
		QuestState st = null;
		if (_spawnList.containsKey(npc))
		{
			player = GameObjectsStorage.getPlayer(_spawnList.get(npc));
			if (player != null)
			{
				st = player.getQuestState(getName());
			}
		}
		return st;
	}

	private void giveHalishaMark(QuestState st2, Npc npc)
	{
		if (st2.getInt("spawned") == 0)
		{
			if (st2.getQuestItemsCount(_items[3]) >= 700)
			{
				st2.takeItems(_items[3], 20);
				final Npc archon = st2.addSpawn(_mobs[1], npc.getLocation().getX(), npc.getLocation().getY(), npc.getLocation().getZ());
				addSpawn(st2, archon);
				st2.set("spawned", "1");
				st2.startQuestTimer("Archon Hellisha has despawned", 600000, archon);
				autoChat(archon, _msgs[13].replace("PLAYERNAME", st2.getPlayer().getName(null)));
				((Attackable) archon).addDamageHate(st2.getPlayer(), 0, 99999);
				archon.getAI().setIntention(CtrlIntention.ATTACK, st2.getPlayer());
			}
			else
			{
				st2.calcDropItems(getId(), _items[3], npc.getId(), Integer.MAX_VALUE);
			}
		}
	}

	private QuestState findQuest(Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st != null)
		{
			if (getId() == 68)
			{
				for (int q = 0; q < 2; q++)
				{
					if (player.getClassId().getId() == QuestClass[1][q])
					{
						return st;
					}
				}
			}
			else if (player.getClassId().getId() == QuestClass[getId() - 67][0])
			{
				return st;
			}
		}
		return null;
	}

	public int getClassId(Player player)
	{
		if (player.getClassId().getId() == 0x81)
		{
			return _classId[1];
		}
		return _classId[0];
	}

	public int getPrevClass(Player player)
	{
		if (player.getClassId().getId() == 0x81)
		{
			if (_prevClass.length == 1)
			{
				return -1;
			}
			return _prevClass[1];
		}
		return _prevClass[0];
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		String htmltext = null;
		if (st != null && !st.isCompleted())
		{
			switch (event)
			{
				case "0-011.htm":
				case "0-012.htm":
				case "0-013.htm":
				case "0-014.htm":
				case "0-015.htm":
					htmltext = event;
					break;
				case "accept":
					st.startQuest();
					giveItems(player, _items[10], 1);
					htmltext = "0-03.htm";
					break;
				case "0-1":
					if (player.getLevel() < 76)
					{
						htmltext = "0-02.htm";
						if (st.isCreated())
						{
							st.exitQuest(true);
						}
					}
					else
					{
						htmltext = "0-05.htm";
					}
					break;
				case "0-2":
					if (player.getLevel() < 76)
					{
						takeItems(player, _items[10], -1);
						st.setCond(20, true);
						htmltext = "0-08.htm";
					}
					else
					{
						takeItems(player, _items[10], -1);
						st.calcExpAndSp(getId());
						st.calcReward(getId());
						final int Class = getClassId(player);
						final int prevClass = getPrevClass(player);
						player.setClassId(Class);
						if (!player.isSubClassActive() && (player.getBaseClass() == prevClass))
						{
							player.setBaseClass(Class);
						}
						player.broadcastCharInfo();
						cast(npc, player, 4339, 1);
						st.exitQuest(false);
						htmltext = "0-07.htm";
					}
					break;
				case "1-3":
					st.setCond(3);
					htmltext = "1-05.htm";
					break;
				case "1-4":
					st.setCond(4);
					takeItems(player, _items[0], 1);
					if (_items[11] != 0)
					{
						takeItems(player, _items[11], 1);
					}
					giveItems(player, _items[1], 1);
					htmltext = "1-06.htm";
					break;
				case "2-1":
					st.setCond(2);
					htmltext = "2-05.htm";
					break;
				case "2-2":
					st.setCond(5);
					takeItems(player, _items[1], 1);
					giveItems(player, _items[4], 1);
					htmltext = "2-06.htm";
					break;
				case "3-5":
					htmltext = "3-07.htm";
					break;
				case "3-6":
					st.setCond(11);
					htmltext = "3-02.htm";
					break;
				case "3-7":
					st.setCond(12);
					htmltext = "3-03.htm";
					break;
				case "3-8":
					st.setCond(13);
					takeItems(player, _items[2], 1);
					giveItems(player, _items[7], 1);
					htmltext = "3-08.htm";
					break;
				case "4-1":
					htmltext = "4-010.htm";
					break;
				case "4-2":
					giveItems(player, _items[9], 1);
					st.setCond(18, true);
					htmltext = "4-011.htm";
					break;
				case "4-3":
					giveItems(player, _items[9], 1);
					st.setCond(18, true);
					autoChat(npc, _msgs[13].replace("PLAYERNAME", player.getName(null)));
					st.set("Quest0", "0");
					cancelQuestTimer("Mob_2 has despawned", npc, player);
					DeleteSpawn(st, npc);
					return null;
				case "5-1":
					st.setCond(6, true);
					takeItems(player, _items[4], 1);
					cast(npc, player, 4546, 1);
					htmltext = "5-02.htm";
					break;
				case "6-1":
					st.setCond(8, true);
					takeItems(player, _items[5], 1);
					cast(npc, player, 4546, 1);
					htmltext = "6-03.htm";
					break;
				case "7-1":
					if (st.getInt("spawned") == 1)
					{
						htmltext = "7-03.htm";
					}
					else if (st.getInt("spawned") == 0)
					{
						final Npc Mob_1 = st.addSpawn(_mobs[0], _npcSpawnLoc[0].getX(), _npcSpawnLoc[0].getY(), _npcSpawnLoc[0].getZ());
						st.set("spawned", "1");
						st.startQuestTimer("Mob_1 Timer 1", 500, Mob_1);
						st.startQuestTimer("Mob_1 has despawned", 300000, Mob_1);
						addSpawn(st, Mob_1);
						htmltext = "7-02.htm";
					}
					else
					{
						htmltext = "7-04.htm";
					}
					break;
				case "7-2":
					st.setCond(10, true);
					takeItems(player, _items[6], 1);
					cast(npc, player, 4546, 1);
					htmltext = "7-06.htm";
					break;
				case "8-1":
					st.setCond(14, true);
					takeItems(player, _items[7], 1);
					cast(npc, player, 4546, 1);
					htmltext = "8-02.htm";
					break;
				case "9-1":
					st.setCond(17, true);
					takeItems(player, _items[8], 1);
					cast(npc, player, 4546, 1);
					htmltext = "9-03.htm";
					break;
				case "10-1":
					if (st.getInt("Quest0") == 0)
					{
						final Npc Mob_3 = st.addSpawn(_mobs[2], _npcSpawnLoc[1].getX(), _npcSpawnLoc[1].getY(), _npcSpawnLoc[1].getZ());
						final Npc Mob_2 = st.addSpawn(_npcs[4], _npcSpawnLoc[2].getX(), _npcSpawnLoc[2].getY(), _npcSpawnLoc[2].getZ());
						addSpawn(st, Mob_3);
						addSpawn(st, Mob_2);
						st.set("Mob_2", String.valueOf(Mob_2.getObjectId()));
						st.set("Quest0", "1");
						st.set("Quest1", "45");
						st.startRepeatingQuestTimer("Mob_3 Timer 1", 500, Mob_3);
						st.startQuestTimer("Mob_3 has despawned", 59000, Mob_3);
						st.startQuestTimer("Mob_2 Timer 1", 500, Mob_2);
						st.startQuestTimer("Mob_2 has despawned", 60000, Mob_2);
						htmltext = "10-02.htm";
					}
					else if (st.getInt("Quest1") == 45)
					{
						htmltext = "10-03.htm";
					}
					else
					{
						htmltext = "10-04.htm";
					}
					break;
				case "10-2":
					st.setCond(19, true);
					takeItems(player, _items[9], 1);
					cast(npc, player, 4546, 1);
					htmltext = "10-06.htm";
					break;
				case "11-9":
					st.setCond(15);
					htmltext = "11-03.htm";
					break;
				case "Mob_1 Timer 1":
					autoChat(npc, _msgs[0].replace("PLAYERNAME", player.getName(null)));
					return null;
				case "Mob_1 has despawned":
					autoChat(npc, _msgs[1].replace("PLAYERNAME", player.getName(null)));
					st.set("spawned", "0");
					DeleteSpawn(st, npc);
					return null;
				case "Archon Hellisha has despawned":
					autoChat(npc, _msgs[6].replace("PLAYERNAME", player.getName(null)));
					st.set("spawned", "0");
					DeleteSpawn(st, npc);
					return null;
				case "Mob_3 Timer 1":
					final Npc Mob_2 = FindSpawn(player, GameObjectsStorage.getNpc(st.getInt("Mob_2")));
					if (Mob_2 != null && World.getInstance().getAroundNpc(npc).contains(Mob_2))
					{
						((Attackable) npc).addDamageHate(Mob_2, 0, 99999);
						npc.getAI().setIntention(CtrlIntention.ATTACK, Mob_2, null);
						Mob_2.getAI().setIntention(CtrlIntention.ATTACK, npc, null);
						autoChat(npc, _msgs[14].replace("PLAYERNAME", player.getName(null)));
						cancelQuestTimer("Mob_3 Timer 1", npc, player);
					}
					return null;
				case "Mob_3 has despawned":
					autoChat(npc, _msgs[15].replace("PLAYERNAME", player.getName(null)));
					st.set("Quest0", "2");
					DeleteSpawn(st, npc);
					return null;
				case "Mob_2 Timer 1":
					autoChat(npc, _msgs[7].replace("PLAYERNAME", player.getName(null)));
					st.startQuestTimer("Mob_2 Timer 2", 1500, npc);
					if (st.getInt("Quest1") == 45)
					{
						st.set("Quest1", "0");
					}
					return null;
				case "Mob_2 Timer 2":
					autoChat(npc, _msgs[8].replace("PLAYERNAME", player.getName(null)));
					st.startQuestTimer("Mob_2 Timer 3", 10000, npc);
					return null;
				case "Mob_2 Timer 3":
					if (st.getInt("Quest0") == 0)
					{
						st.startQuestTimer("Mob_2 Timer 3", 13000, npc);
						if (getRandom(2) == 0)
						{
							autoChat(npc, _msgs[9].replace("PLAYERNAME", player.getName(null)));
						}
						else
						{
							autoChat(npc, _msgs[10].replace("PLAYERNAME", player.getName(null)));
						}
					}
					return null;
				case "Mob_2 has despawned":
					st.set("Quest1", String.valueOf(st.getInt("Quest1") + 1));
					if ((st.getInt("Quest0") == 1) || (st.getInt("Quest0") == 2) || (st.getInt("Quest1") > 3))
					{
						st.set("Quest0", "0");
						if (st.getInt("Quest0") == 1)
						{
							autoChat(npc, _msgs[11].replace("PLAYERNAME", player.getName(null)));
						}
						else
						{
							autoChat(npc, _msgs[12].replace("PLAYERNAME", player.getName(null)));
						}
						DeleteSpawn(st, npc);
					}
					else
					{
						st.startQuestTimer("Mob_2 has despawned", 1000, npc);
					}
					return null;
			}
		}
		return htmltext;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st != null)
		{
			final int npcId = npc.getId();
			if ((npcId == _npcs[0]) && st.isCompleted())
			{
				htmltext = getAlreadyCompletedMsg(player);
			}
			else if (player.getClassId().getId() == getPrevClass(player))
			{
				switch (st.getCond())
				{
					case 0:
						if (npcId == _npcs[0])
						{
							htmltext = "0-01.htm";
						}
						break;
					case 1:
						if (npcId == _npcs[0])
						{
							htmltext = "0-04.htm";
						}
						else if (npcId == _npcs[2])
						{
							htmltext = "2-01.htm";
						}
						break;
					case 2:
						if (npcId == _npcs[2])
						{
							htmltext = "2-02.htm";
						}
						else if (npcId == _npcs[1])
						{
							htmltext = "1-01.htm";
						}
						break;
					case 3:
						if ((npcId == _npcs[1]) && hasQuestItems(player, _items[0]))
						{
							if ((_items[11] == 0) || hasQuestItems(player, _items[11]))
							{
								htmltext = "1-03.htm";
							}
							else
							{
								htmltext = "1-02.htm";
							}
						}
						break;
					case 4:
						if (npcId == _npcs[1])
						{
							htmltext = "1-04.htm";
						}
						else if (npcId == _npcs[2])
						{
							htmltext = "2-03.htm";
						}
						break;
					case 5:
						if (npcId == _npcs[2])
						{
							htmltext = "2-04.htm";
						}
						else if (npcId == _npcs[5])
						{
							htmltext = "5-01.htm";
						}
						break;
					case 6:
						if (npcId == _npcs[5])
						{
							htmltext = "5-03.htm";
						}
						else if (npcId == _npcs[6])
						{
							htmltext = "6-01.htm";
						}
						break;
					case 7:
						if (npcId == _npcs[6])
						{
							htmltext = "6-02.htm";
						}
						break;
					case 8:
						if (npcId == _npcs[6])
						{
							htmltext = "6-04.htm";
						}
						else if (npcId == _npcs[7])
						{
							htmltext = "7-01.htm";
						}
						break;
					case 9:
						if (npcId == _npcs[7])
						{
							htmltext = "7-05.htm";
						}
						break;
					case 10:
						if (npcId == _npcs[7])
						{
							htmltext = "7-07.htm";
						}
						else if (npcId == _npcs[3])
						{
							htmltext = "3-01.htm";
						}
						break;
					case 11:
					case 12:
						if (npcId == _npcs[3])
						{
							if (hasQuestItems(player, _items[2]))
							{
								htmltext = "3-05.htm";
							}
							else
							{
								htmltext = "3-04.htm";
							}
						}
						break;
					case 13:
						if (npcId == _npcs[3])
						{
							htmltext = "3-06.htm";
						}
						else if (npcId == _npcs[8])
						{
							htmltext = "8-01.htm";
						}
						break;
					case 14:
						if (npcId == _npcs[8])
						{
							htmltext = "8-03.htm";
						}
						else if (npcId == _npcs[11])
						{
							htmltext = "11-01.htm";
						}
						break;
					case 15:
						if (npcId == _npcs[11])
						{
							htmltext = "11-02.htm";
						}
						else if (npcId == _npcs[9])
						{
							htmltext = "9-01.htm";
						}
						break;
					case 16:
						if (npcId == _npcs[9])
						{
							htmltext = "9-02.htm";
						}
						break;
					case 17:
						if (npcId == _npcs[9])
						{
							htmltext = "9-04.htm";
						}
						else if (npcId == _npcs[10])
						{
							htmltext = "10-01.htm";
						}
						break;
					case 18:
						if (npcId == _npcs[10])
						{
							htmltext = "10-05.htm";
						}
						break;
					case 19:
						if (npcId == _npcs[10])
						{
							htmltext = "10-07.htm";
						}
						else if (npcId == _npcs[0])
						{
							htmltext = "0-06.htm";
						}
						break;
					case 20:
						if (npcId == _npcs[0])
						{
							if (player.getLevel() >= 76)
							{
								htmltext = "0-09.htm";
								if ((getClassId(player) < 131) || (getClassId(player) > 135))
								{
									st.exitQuest(false);
									st.calcExpAndSp(getId());
									st.calcReward(getId());
									final int classId = getClassId(player);
									final int prevClass = getPrevClass(player);
									player.setClassId(classId);
									if (!player.isSubClassActive() && (player.getBaseClass() == prevClass))
									{
										player.setBaseClass(classId);
									}
									player.broadcastCharInfo();
									cast(npc, player, 4339, 1);
								}
							}
							else
							{
								htmltext = "0-010.htm";
							}
						}
						break;
				}
			}
		}
		return htmltext;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState st = player.getQuestState(getName());
		final int npcId = npc.getId();
		if (st != null)
		{
			if (npcId == _npcs[4])
			{
				final int cond = st.getCond();
				if (cond == 17)
				{
					final QuestState st2 = findRightState(npc);
					if (st2 != null)
					{
						player.setLastQuestNpcObject(npc.getObjectId());
						final int tab = st.getInt("Tab");
						final int quest0 = st.getInt("Quest0");

						if (st == st2)
						{
							if (tab == 1)
							{
								if (quest0 == 0)
								{
									htmltext = "4-04.htm";
								}
								else if (quest0 == 1)
								{
									htmltext = "4-06.htm";
								}
							}
							else if (quest0 == 0)
							{
								htmltext = "4-01.htm";
							}
							else if (quest0 == 1)
							{
								htmltext = "4-03.htm";
							}
						}
						else if (tab == 1)
						{
							if (quest0 == 0)
							{
								htmltext = "4-05.htm";
							}
							else if (quest0 == 1)
							{
								htmltext = "4-07.htm";
							}
						}
						else if (quest0 == 0)
						{
							htmltext = "4-02.htm";
						}
					}
				}
				else if (cond == 18)
				{
					htmltext = "4-08.htm";
				}
			}
		}
		if (htmltext.isEmpty())
		{
			npc.showChatWindow(player);
		}
		return htmltext;
	}

	@Override
	public String onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		final QuestState st2 = findRightState(npc);
		if (st2 != null)
		{
			final int cond = st2.getCond();
			final QuestState st = player.getQuestState(getName());
			final int npcId = npc.getId();
			if ((npcId == _mobs[2]) && (st == st2) && (cond == 17))
			{
				int quest0 = st.getInt("Quest0") + 1;
				if (quest0 == 1)
				{
					autoChat(npc, _msgs[16].replace("PLAYERNAME", player.getName(null)));
				}

				if (quest0 > 15)
				{
					quest0 = 1;
					autoChat(npc, _msgs[17].replace("PLAYERNAME", player.getName(null)));
					cancelQuestTimer("Mob_3 has despawned", npc, st2.getPlayer());
					st.set("Tab", "1");
					DeleteSpawn(st, npc);
				}
				st.set("Quest0", Integer.toString(quest0));
			}
			else if ((npcId == _mobs[1]) && (cond == 15))
			{
				if ((st != st2) || ((st == st2) && player.isInParty()))
				{
					autoChat(npc, _msgs[5].replace("PLAYERNAME", player.getName(null)));
					cancelQuestTimer("Archon Hellisha has despawned", npc, st2.getPlayer());
					st2.set("spawned", "0");
					DeleteSpawn(st2, npc);
				}
			}
		}
		return super.onAttack(npc, player, damage, isSummon);
	}

	@Override
	public String onSkillSee(Npc npc, Player player, Skill skill, GameObject[] targets, boolean isSummon)
	{
		if (_spawnList.containsKey(npc) && (_spawnList.get(npc) != player.getObjectId()))
		{
			final Player quest_player = GameObjectsStorage.getPlayer(_spawnList.get(npc));
			if (quest_player == null)
			{
				return null;
			}

			for (final GameObject obj : targets)
			{
				if ((obj == quest_player) || (obj == npc))
				{
					final QuestState st2 = findRightState(npc);
					if (st2 == null)
					{
						return null;
					}
					autoChat(npc, _msgs[5].replace("PLAYERNAME", player.getName(null)));
					cancelQuestTimer("Archon Hellisha has despawned", npc, st2.getPlayer());
					st2.set("spawned", "0");
					DeleteSpawn(st2, npc);
				}
			}
		}
		return super.onSkillSee(npc, player, skill, targets, isSummon);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		if (player == null)
		{
			return super.onKill(npc, player, isSummon);
		}
		
		if (npc.getId() >= 21646 && npc.getId() <= 21651)
		{
			if (player.getParty() != null)
			{
				final List<QuestState> members = new ArrayList<>();
				for (final Player member : player.getParty().getMembers())
				{
					final QuestState st = findQuest(member);
					if ((st != null) && st.isCond(15) && member.isInsideRadius(player, Config.ALT_PARTY_RANGE2, false, false))
					{
						members.add(st);
					}
				}
				
				if (members.size() > 0)
				{
					final QuestState st = members.get(getRandom(members.size()));
					giveHalishaMark(st, npc);
					members.clear();
				}
			}
			else
			{
				final QuestState st = findQuest(player);
				if (st != null && st.isCond(15))
				{
					giveHalishaMark(st, npc);
				}
			}
			return super.onKill(npc, player, isSummon);
		}
		
		if (npc.getId() >= 18212 && npc.getId() <= 18218)
		{
			final QuestState st1 = findQuest(player);
			if (st1 != null && st1.isCond(15))
			{
				autoChat(npc, _msgs[4].replace("PLAYERNAME", st1.getPlayer().getName(null)));
				st1.giveItems(_items[8], 1);
				st1.takeItems(_items[3], -1);
				st1.setCond(16, true);
			}
			return super.onKill(npc, player, isSummon);
		}
		
		if (npc.getId() >= 27214 && npc.getId() <= 27216)
		{
			final QuestState st1 = findQuest(player);
			if ((st1 != null) && st1.isCond(6))
			{
				final int kills = st1.getInt("kills");
				if (kills < 9)
				{
					st1.set("kills", Integer.toString(kills + 1));
				}
				else
				{
					st1.giveItems(_items[5], 1);
					st1.setCond(7, true);
				}
			}
			return super.onKill(npc, player, isSummon);
		}
		
		QuestState st = player.getQuestState(getName());
		if (st != null)
		{
			final QuestState st2 = findRightState(npc);
			if (st2 != null)
			{
				final int cond = st.getCond();
				if ((npc.getId() == _mobs[0]) && (cond == 8))
				{
					if (!player.isInParty())
					{
						if (st == st2)
						{
							autoChat(npc, _msgs[12].replace("PLAYERNAME", player.getName(null)));
							giveItems(player, _items[6], 1);
							st.setCond(9, true);
						}
					}
					cancelQuestTimer("Mob_1 has despawned", npc, st2.getPlayer());
					st2.set("spawned", "0");
					DeleteSpawn(st2, npc);
				}
				else if ((npc.getId() == _mobs[1]) && (cond == 15))
				{
					if (!player.isInParty())
					{
						if (st == st2)
						{
							autoChat(npc, _msgs[4].replace("PLAYERNAME", player.getName(null)));
							giveItems(player, _items[8], 1);
							takeItems(player, _items[3], -1);
							st.setCond(16, true);
						}
						else
						{
							autoChat(npc, _msgs[5].replace("PLAYERNAME", player.getName(null)));
						}
					}
					cancelQuestTimer("Archon Hellisha has despawned", npc, st2.getPlayer());
					st2.set("spawned", "0");
					DeleteSpawn(st2, npc);
				}
				else if ((npc.getId() == _mobs[2]) && (st == st2) && (cond == 17))
				{
					autoChat(npc, _msgs[17].replace("PLAYERNAME", player.getName(null)));
					cancelQuestTimer("Mob_3 has despawned", npc, st2.getPlayer());
					st.set("Tab", "1");
					DeleteSpawn(st, npc);
					st.set("Quest0", "1");
				}
			}
		}
		else if (npc.getId() == _mobs[0])
		{
			st = findRightState(npc);
			if (st != null)
			{
				cancelQuestTimer("Mob_1 has despawned", npc, player);
				st.set("spawned", "0");
				DeleteSpawn(st, npc);
			}
		}
		else if (npc.getId() == _mobs[1])
		{
			st = findRightState(npc);
			if (st != null)
			{
				cancelQuestTimer("Archon Hellisha has despawned", npc, player);
				st.set("spawned", "0");
				DeleteSpawn(st, npc);
			}
		}
		return super.onKill(npc, player, isSummon);
	}
	
	public static void main(String[] args)
	{
	}
}