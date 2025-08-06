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
package l2e.scripts.instances;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 05.11.2020
 */
public class ToTheMonastery extends AbstractReflection
{
	private class TMWorld extends ReflectionWorld
	{
		public Npc support = null;
		public List<Npc> firstgroup;
		public List<Npc> secondgroup;
		public List<Npc> thirdgroup;
		public List<Npc> fourthgroup;

		public TMWorld()
		{
		}
	}

	private static final int[][] minions_1 =
	{
	        {
	                56504, -252840, -6760, 0
			},
			{
			        56504, -252728, -6760, 0
			},
			{
			        56392, -252728, -6760, 0
			},
			{
			        56408, -252840, -6760, 0
			}
	};

	private static final int[][] minions_2 =
	{
	        {
	                55672, -252728, -6760, 0
			},
			{
			        55752, -252840, -6760, 0
			},
			{
			        55768, -252840, -6760, 0
			},
			{
			        55752, -252712, -6760, 0
			}
	};

	private static final int[][] minions_3 =
	{
	        {
	                55672, -252120, -6760, 0
			},
			{
			        55752, -252120, -6760, 0
			},
			{
			        55656, -252216, -6760, 0
			},
			{
			        55736, -252216, -6760, 0
			}
	};

	private static final int[][] minions_4 =
	{
	        {
	                56520, -252232, -6760, 0
			},
			{
			        56520, -252104, -6760, 0
			},
			{
			        56424, -252104, -6760, 0
			},
			{
			        56440, -252216, -6760, 0
			}
	};

	private static final int[][] TELEPORTS =
	{
	        {
	                120664, -86968, -3392
			},
			{
			        116324, -84994, -3397
			},
			{
			        85937, -249618, -8320
			},
			{
			        120727, -86868, -3392
			},
			{
			        85937, -249618, -8320
			},
			{
			        82434, -249546, -8320
			},
			{
			        85691, -252426, -8320
			},
			{
			        88573, -249556, -8320
			},
			{
			        85675, -246630, -8320
			},
			{
			        45512, -249832, -6760
			},
			{
			        120664, -86968, -3392
			},
			{
			        56033, -252944, -6760
			},
			{
			        56081, -250391, -6760
			},
			{
			        76736, -241021, -10832
			},
			{
			        76736, -241021, -10832
			}
	};

	public ToTheMonastery(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32815);
		addTalkId(32815, 32792, 32803, 32804, 32805, 32806, 32807, 32816, 32817, 32818, 32819, 32793, 32820, 32837, 32842, 32843, 32838, 32839, 32840, 32841);

		addSpawnId(18956, 18957, 18958, 18959);
		
		addKillId(18949, 27403, 27404, 18956, 18957, 18958, 18959);

		questItemIds = new int[]
		{
		        17228, 17229, 17230, 17231
		};
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new TMWorld(), 151))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((TMWorld) world).support = addSpawn(32787, player.getX(), player.getY(), player.getZ(), 0, false, 0, false, player.getReflection());
			startQuestTimer("check_follow", 3000, ((TMWorld) world).support, player);
			startQuestTimer("check_player", 3000, ((TMWorld) world).support, player);
			startQuestTimer("check_voice", 3000, ((TMWorld) world).support, player);
			world.getReflection().openDoor(21100001);
			world.getReflection().openDoor(21100002);
			world.getReflection().openDoor(21100003);
			world.getReflection().openDoor(21100004);
			world.getReflection().openDoor(21100005);
			world.getReflection().openDoor(21100006);
			world.getReflection().openDoor(21100007);
			world.getReflection().openDoor(21100008);
			world.getReflection().openDoor(21100009);
			world.getReflection().openDoor(21100010);
			world.getReflection().openDoor(21100011);
			world.getReflection().openDoor(21100012);
			world.getReflection().openDoor(21100013);
			world.getReflection().openDoor(21100014);
			world.getReflection().openDoor(21100015);
			world.getReflection().openDoor(21100016);
		}
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
		else
		{
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		final int npcId = npc.getId();

		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getPlayerWorld(player);
		if (tmpworld != null && tmpworld instanceof TMWorld)
		{
			final TMWorld world = (TMWorld) tmpworld;
			if (npcId == 32792)
			{
				if (event.equalsIgnoreCase("Enter3"))
				{
					teleportPlayer(npc, player, TELEPORTS[2], player.getReflection());
					startQuestTimer("start_movie", 3000, npc, player);
					return null;
				}
				else if (event.equalsIgnoreCase("teleport_in"))
				{
					teleportPlayer(npc, player, TELEPORTS[9], player.getReflection());
					final QuestState check = player.getQuestState("_10295_SevenSignsSolinasTomb");
					if (check != null && check.getInt("entermovie") == 0)
					{
						ThreadPoolManager.getInstance().schedule(() -> player.showQuestMovie(26), 3000L);
						check.set("entermovie", "1");
					}
					return null;
				}
				else if (event.equalsIgnoreCase("start_scene"))
				{
					final QuestState check = player.getQuestState("_10296_SevenSignsPoweroftheSeal");
					if (check != null)
					{
						check.setCond(2);
					}
					ThreadPoolManager.getInstance().schedule(new Teleport(npc, player, TELEPORTS[13], world.getReflection()), 60500L);
					player.showQuestMovie(29);
					return null;
				}
				else if (event.equalsIgnoreCase("teleport_back"))
				{
					teleportPlayer(npc, player, TELEPORTS[14], player.getReflection());
					return null;
				}
			}
			else if (npcId == 32803)
			{
				if (event.equalsIgnoreCase("ReturnToEris"))
				{
					teleportPlayer(npc, player, TELEPORTS[3], player.getReflection());
					return null;
				}
			}
			else if ((npcId == 32820) || (npcId == 32792))
			{
				if (event.equalsIgnoreCase("teleport_solina"))
				{
					teleportPlayer(npc, player, TELEPORTS[11], player.getReflection());
					if (npcId == 32820)
					{
						final QuestState qs = player.getQuestState("_10295_SevenSignsSolinasTomb");
						if (qs != null)
						{
							if ((qs.getInt("firstgroup") == 1) && (qs.getInt("secondgroup") == 1) && (qs.getInt("thirdgroup") == 1) && (qs.getInt("fourthgroup") == 1))
							{
								world.getReflection().openDoor(21100018);
							}
							else
							{
								final int activity = qs.getInt("activity");
								if (activity == 1)
								{
									world.getReflection().openDoor(21100101);
									world.getReflection().openDoor(21100102);
									world.getReflection().openDoor(21100103);
									world.getReflection().openDoor(21100104);
									if (world.firstgroup == null)
									{
										SpawnFirstGroup(world);
									}
									if (world.secondgroup == null)
									{
										SpawnSecondGroup(world);
									}
									if (world.thirdgroup == null)
									{
										SpawnThirdGroup(world);
									}
									if (world.fourthgroup == null)
									{
										SpawnFourthGroup(world);
									}
								}
							}
						}
					}
					return null;
				}
			}
			else if (event.equalsIgnoreCase("FirstGroupSpawn"))
			{
				SpawnFirstGroup(world);
				return null;
			}
			else if (event.equalsIgnoreCase("SecondGroupSpawn"))
			{
				SpawnSecondGroup(world);
				return null;
			}
			else if (event.equalsIgnoreCase("ThirdGroupSpawn"))
			{
				SpawnThirdGroup(world);
				return null;
			}
			else if (event.equalsIgnoreCase("FourthGroupSpawn"))
			{
				SpawnFourthGroup(world);
				return null;
			}
			else if (event.equalsIgnoreCase("start_movie"))
			{
				player.showQuestMovie(24);
				return null;
			}
			else if (event.equalsIgnoreCase("check_player"))
			{
				cancelQuestTimer("check_player", npc, player);

				if (player.getCurrentHp() < (player.getMaxHp() * 0.8))
				{
					final Skill skill = SkillsParser.getInstance().getInfo(6724, 1);
					if (skill != null)
					{
						npc.setTarget(player);
						npc.doCast(skill);
					}
				}

				if (player.getCurrentMp() < (player.getMaxMp() * 0.5))
				{
					final Skill skill = SkillsParser.getInstance().getInfo(6728, 1);
					if (skill != null)
					{
						npc.setTarget(player);
						npc.doCast(skill);
					}
				}

				if (player.getCurrentHp() < (player.getMaxHp() * 0.1))
				{
					final Skill skill = SkillsParser.getInstance().getInfo(6730, 1);
					if (skill != null)
					{
						npc.setTarget(player);
						npc.doCast(skill);
					}
				}

				if (player.isInCombat())
				{
					final Skill skill = SkillsParser.getInstance().getInfo(6725, 1);
					if (skill != null)
					{
						npc.setTarget(player);
						npc.doCast(skill);
					}
				}
				startQuestTimer("check_player", 3000, npc, player);
				return null;
			}
			else if (event.equalsIgnoreCase("check_voice"))
			{
				cancelQuestTimer("check_voice", npc, player);

				final QuestState qs = player.getQuestState("_10294_SevenSignToTheMonastery");
				if ((qs != null) && !qs.isCompleted())
				{
					if (qs.isCond(2))
					{
						if (Rnd.getChance(5))
						{
							if (Rnd.getChance(10))
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.IT_SEEMS_THAT_YOU_CANNOT_REMEMBER_TO_THE_ROOM_OF_THE_WATCHER_WHO_FOUND_THE_BOOK));
							}
							else
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.REMEMBER_THE_CONTENT_OF_THE_BOOKS_THAT_YOU_FOUND_YOU_CANT_TAKE_THEM_OUT_WITH_YOU));
							}
						}
					}
					else if (qs.isCond(3))
					{
						if (Rnd.getChance(8))
						{
							npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.YOUR_WORK_HERE_IS_DONE_SO_RETURN_TO_THE_CENTRAL_GUARDIAN));
						}
					}
				}

				final QuestState qs2 = player.getQuestState("_10295_SevenSignsSolinasTomb");
				if ((qs2 != null) && !qs2.isCompleted())
				{
					if (qs2.isCond(1))
					{
						if (Rnd.getChance(5))
						{
							if (Rnd.getChance(10))
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.TO_REMOVE_THE_BARRIER_YOU_MUST_FIND_THE_RELICS_THAT_FIT_THE_BARRIER_AND_ACTIVATE_THE_DEVICE));
							}
							else if (Rnd.getChance(15))
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.THE_GUARDIAN_OF_THE_SEAL_DOESNT_SEEM_TO_GET_INJURED_AT_ALL_UNTIL_THE_BARRIER_IS_DESTROYED));
							}
							else
							{
								npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.THE_DEVICE_LOCATED_IN_THE_ROOM_IN_FRONT_OF_THE_GUARDIAN_OF_THE_SEAL_IS_DEFINITELY_THE_BARRIER_THAT_CONTROLS_THE_GUARDIANS_POWER));
							}
						}
					}
				}
				startQuestTimer("check_voice", 100000, npc, player);
				return null;
			}
			else if (event.equalsIgnoreCase("check_follow"))
			{
				cancelQuestTimer("check_follow", npc, player);
				npc.getAI().stopFollow();
				npc.setIsRunning(true);
				npc.getAI().startFollow(player);
				startQuestTimer("check_follow", 5000, npc, player);
				return null;
			}
		}
		return htmltext;
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

		final int npcId = npc.getId();

		if (npcId == 32815)
		{
			if ((player.getQuestState("_10294_SevenSignToTheMonastery") != null) && (player.getQuestState("_10294_SevenSignToTheMonastery").getState() == State.STARTED))
			{
				enterInstance(player, npc);
				return null;
			}
			else if ((player.getQuestState("_10294_SevenSignToTheMonastery") != null) && (player.getQuestState("_10294_SevenSignToTheMonastery").getState() == State.COMPLETED) && (player.getQuestState("_10295_SevenSignsSolinasTomb") == null))
			{
				enterInstance(player, npc);
				return null;
			}
			else if ((player.getQuestState("_10295_SevenSignsSolinasTomb") != null) && (player.getQuestState("_10295_SevenSignsSolinasTomb").getState() != State.COMPLETED))
			{
				enterInstance(player, npc);
				return null;
			}
			else if ((player.getQuestState("_10295_SevenSignsSolinasTomb") != null) && (player.getQuestState("_10295_SevenSignsSolinasTomb").getState() == State.COMPLETED) && (player.getQuestState("_10296_SevenSignsPoweroftheSeal") == null))
			{
				enterInstance(player, npc);
				return null;
			}
			else if ((player.getQuestState("_10296_SevenSignsPoweroftheSeal") != null) && (player.getQuestState("_10296_SevenSignsPoweroftheSeal").getState() != State.COMPLETED))
			{
				enterInstance(player, npc);
				return null;
			}
			else
			{
				htmltext = "32815-00.htm";
			}
		}

		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getPlayerWorld(player);
		if (tmpworld != null && tmpworld instanceof TMWorld)
		{
			final TMWorld world = (TMWorld) tmpworld;
			if (npcId == 32792)
			{
				if (world.support != null)
				{
					cancelQuestTimer("check_follow", world.support, player);
					cancelQuestTimer("check_player", world.support, player);
					cancelQuestTimer("check_voice", world.support, player);
					world.support.deleteMe();
				}
				teleportPlayer(npc, player, TELEPORTS[1], ReflectionManager.DEFAULT);
				return null;
			}
			else if (npcId == 32816)
			{
				teleportPlayer(npc, player, TELEPORTS[5], player.getReflection());
				return null;
			}
			else if (npcId == 32817)
			{
				teleportPlayer(npc, player, TELEPORTS[7], player.getReflection());
				return null;
			}
			else if (npcId == 32818)
			{
				teleportPlayer(npc, player, TELEPORTS[8], player.getReflection());
				return null;
			}
			else if (npcId == 32819)
			{
				teleportPlayer(npc, player, TELEPORTS[6], player.getReflection());
				return null;
			}
			else if ((npcId == 32804) || (npcId == 32805) || (npcId == 32806) || (npcId == 32807))
			{
				teleportPlayer(npc, player, TELEPORTS[4], player.getReflection());
				return null;
			}
			else if ((npcId == 32793) || (npcId == 32820) || (npcId == 32837))
			{
				teleportPlayer(npc, player, TELEPORTS[10], player.getReflection());
				return null;
			}
			else if (npcId == 32842)
			{
				teleportPlayer(npc, player, TELEPORTS[12], player.getReflection());
				player.showQuestMovie(28);
				return null;
			}
			else if (npcId == 32838)
			{
				if (st.getQuestItemsCount(17231) > 0)
				{
					st.takeItems(17231, -1);
					removeInvincibility(player, 18953);
					return null;
				}
				htmltext = "no-item.htm";
			}
			else if (npcId == 32839)
			{
				if (st.getQuestItemsCount(17228) > 0)
				{
					st.takeItems(17228, -1);
					removeInvincibility(player, 18954);
					return null;
				}
				htmltext = "no-item.htm";
			}
			else if (npcId == 32840)
			{
				if (st.getQuestItemsCount(17230) > 0)
				{
					st.takeItems(17230, -1);
					removeInvincibility(player, 18955);
					return null;
				}
				htmltext = "no-item.htm";
			}
			else if (npcId == 32841)
			{
				if (st.getQuestItemsCount(17229) > 0)
				{
					st.takeItems(17229, -1);
					removeInvincibility(player, 18952);
					return null;
				}
				htmltext = "no-item.htm";
			}
			else if (npcId == 32843)
			{
				final QuestState qs = player.getQuestState("_10295_SevenSignsSolinasTomb");
				if (qs != null)
				{
					final int activity = qs.getInt("activity");
					if (activity == 1)
					{
						htmltext = "32843-03.htm";
					}
					else
					{
						world.getReflection().openDoor(21100101);
						world.getReflection().openDoor(21100102);
						world.getReflection().openDoor(21100103);
						world.getReflection().openDoor(21100104);
						SpawnFirstGroup(world);
						SpawnSecondGroup(world);
						SpawnThirdGroup(world);
						SpawnFourthGroup(world);
						qs.set("activity", "1");
						htmltext = "32843-02.htm";
					}
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
			return null;
		}

		final QuestState qs = player.getQuestState("_10295_SevenSignsSolinasTomb");
		if (qs != null)
		{
			final int npcId = npc.getId();
			final int firstgroup = qs.getInt("firstgroup");
			final int secondgroup = qs.getInt("secondgroup");
			final int thirdgroup = qs.getInt("thirdgroup");
			final int fourthgroup = qs.getInt("fourthgroup");

			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if (tmpworld != null && tmpworld instanceof TMWorld)
			{
				final TMWorld world = (TMWorld) tmpworld;
				if (npcId == 27403)
				{
					if (world.firstgroup != null)
					{
						world.firstgroup.remove(npc);
						if (world.firstgroup.isEmpty())
						{
							world.firstgroup = null;

							if (firstgroup == 1)
							{
								cancelQuestTimer("FirstGroupSpawn", npc, player);
							}
							else
							{
								startQuestTimer("FirstGroupSpawn", 10000, npc, player);
							}
						}
					}

					if (world.secondgroup != null)
					{
						world.secondgroup.remove(npc);
						if (world.secondgroup.isEmpty())
						{
							world.secondgroup = null;

							if (secondgroup == 1)
							{
								cancelQuestTimer("SecondGroupSpawn", npc, player);
							}
							else
							{
								startQuestTimer("SecondGroupSpawn", 10000, npc, player);
							}
						}
					}
				}

				if (npcId == 27404)
				{
					if (world.thirdgroup != null)
					{
						world.thirdgroup.remove(npc);
						if (world.thirdgroup.isEmpty())
						{
							world.thirdgroup = null;

							if (thirdgroup == 1)
							{
								cancelQuestTimer("ThirdGroupSpawn", npc, player);
							}
							else
							{
								startQuestTimer("ThirdGroupSpawn", 10000, npc, player);
							}
						}
					}

					if (world.fourthgroup != null)
					{
						world.fourthgroup.remove(npc);
						if (world.fourthgroup.isEmpty())
						{
							world.fourthgroup = null;

							if (fourthgroup == 1)
							{
								cancelQuestTimer("FourthGroupSpawn", npc, player);
							}
							else
							{
								startQuestTimer("FourthGroupSpawn", 10000, npc, player);
							}
						}
					}
				}

				if (npcId == 18949)
				{
					ThreadPoolManager.getInstance().schedule(new Teleport(npc, player, TELEPORTS[0], world.getReflection()), 60500L);
					return null;
				}

				if (npcId == 18956)
				{
					qs.set("firstgroup", "1");
				}

				if (npcId == 18957)
				{
					qs.set("secondgroup", "1");
				}

				if (npcId == 18958)
				{
					qs.set("thirdgroup", "1");
				}

				if (npcId == 18959)
				{
					qs.set("fourthgroup", "1");
				}
				
				if ((qs.getInt("firstgroup") == 1) && (qs.getInt("secondgroup") == 1) && (qs.getInt("thirdgroup") == 1) && (qs.getInt("fourthgroup") == 1))
				{
					world.getReflection().openDoor(21100018);
				}
			}
		}
		return "";
	}

	@Override
	public String onSpawn(Npc npc)
	{
		if (npc instanceof MonsterInstance)
		{
			if (npc.getId() == 18956 || npc.getId() == 18957 || npc.getId() == 18958 || npc.getId() == 18959)
			{
				final MonsterInstance monster = (MonsterInstance) npc;
				monster.setIsImmobilized(true);
			}
		}
		return super.onSpawn(npc);
	}

	protected void teleportPlayer(Npc npc, Player player, int[] coords, Reflection r)
	{
		player.getAI().setIntention(CtrlIntention.IDLE);
		player.teleToLocation(coords[0], coords[1], coords[2], false, r);
		if (!r.isDefault())
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if (tmpworld != null && tmpworld instanceof TMWorld)
			{
				final TMWorld world = (TMWorld) tmpworld;
				if (world.support != null)
				{
					cancelQuestTimer("check_follow", world.support, player);
					cancelQuestTimer("check_player", world.support, player);
					cancelQuestTimer("check_voice", world.support, player);
					world.support.deleteMe();
				}
				world.support = addSpawn(32787, player.getX(), player.getY(), player.getZ(), 0, false, 0, false, r);
				startQuestTimer("check_follow", 3000, world.support, player);
				startQuestTimer("check_player", 3000, world.support, player);
				startQuestTimer("check_voice", 3000, world.support, player);
			}
		}
	}

	protected void SpawnFirstGroup(TMWorld world)
	{
		world.firstgroup = new ArrayList<>();
		for (final int[] spawn : minions_1)
		{
			final Npc spawnedMob = addSpawn(27403, spawn[0], spawn[1], spawn[2], spawn[3], false, 0, false, world.getReflection());
			world.firstgroup.add(spawnedMob);
		}
	}

	protected void SpawnSecondGroup(TMWorld world)
	{
		world.secondgroup = new ArrayList<>();
		for (final int[] spawn : minions_2)
		{
			final Npc spawnedMob = addSpawn(27403, spawn[0], spawn[1], spawn[2], spawn[3], false, 0, false, world.getReflection());
			world.secondgroup.add(spawnedMob);
		}
	}

	protected void SpawnThirdGroup(TMWorld world)
	{
		world.thirdgroup = new ArrayList<>();
		for (final int[] spawn : minions_3)
		{
			final Npc spawnedMob = addSpawn(27404, spawn[0], spawn[1], spawn[2], spawn[3], false, 0, false, world.getReflection());
			world.thirdgroup.add(spawnedMob);
		}
	}

	protected void SpawnFourthGroup(TMWorld world)
	{
		world.fourthgroup = new ArrayList<>();
		for (final int[] spawn : minions_4)
		{
			final Npc spawnedMob = addSpawn(27404, spawn[0], spawn[1], spawn[2], spawn[3], false, 0, false, world.getReflection());
			world.fourthgroup.add(spawnedMob);
		}
	}

	private class Teleport implements Runnable
	{
		private final Npc _npc;
		private final Player _player;
		private final Reflection _r;
		private final int[] _cords;

		public Teleport(Npc npc, Player player, int[] cords, Reflection r)
		{
			_npc = npc;
			_player = player;
			_cords = cords;
			_r = r;
		}

		@Override
		public void run()
		{
			try
			{
				teleportPlayer(_npc, _player, _cords, _r);
				startQuestTimer("check_follow", 3000, _npc, _player);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void removeInvincibility(Player player, int mobId)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getPlayerWorld(player);
		if (tmpworld != null && tmpworld instanceof TMWorld)
		{
			final TMWorld world = (TMWorld) tmpworld;
			final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			{
				for (final Npc n : inst.getNpcs())
				{
					if (n != null && n.getId() == mobId)
					{
						for (final Effect e : n.getEffectList().getAllEffects())
						{
							if (e.getSkill().getId() == 6371)
							{
								e.exit();
							}
						}
					}
				}
			}
		}
	}

	public static void main(String[] args)
	{
		new ToTheMonastery(ToTheMonastery.class.getSimpleName(), "instances");
	}
}