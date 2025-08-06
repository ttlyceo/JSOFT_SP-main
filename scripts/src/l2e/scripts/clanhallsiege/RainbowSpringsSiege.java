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
package l2e.scripts.clanhallsiege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.SiegeClan.SiegeClanType;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ChestInstance;
import l2e.gameserver.model.entity.clanhall.ClanHallSiegeEngine;
import l2e.gameserver.model.entity.clanhall.SiegeStatus;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Updated by LordWinter
 */
public final class RainbowSpringsSiege extends ClanHallSiegeEngine
{
	private static boolean _isDebug = false;
	private static final int WAR_DECREES = 8034;
	private static final int RAINBOW_NECTAR = 8030;
	private static final int RAINBOW_MWATER = 8031;
	private static final int RAINBOW_WATER = 8032;
	private static final int RAINBOW_SULFUR = 8033;

	private static final int MESSENGER = 35604;
	private static final int CARETAKER = 35603;
	private static final int CHEST = 35593;
	private static final int ENRAGED_YETI = 35592;
	
	private static Map<Integer, Long> _warDecreesCount = new HashMap<>();
	private static List<Clan> _acceptedClans = new ArrayList<>();
	private final List<Player> _playersOnArena = new ArrayList<>();
	private final List<Npc> chests = new ArrayList<>();
	
	private static final int ItemA = 8035;
	private static final int ItemB = 8036;
	private static final int ItemC = 8037;
	private static final int ItemD = 8038;
	private static final int ItemE = 8039;
	private static final int ItemF = 8040;
	private static final int ItemG = 8041;
	private static final int ItemH = 8042;
	private static final int ItemI = 8043;
	private static final int ItemK = 8045;
	private static final int ItemL = 8046;
	private static final int ItemN = 8047;
	private static final int ItemO = 8048;
	private static final int ItemP = 8049;
	private static final int ItemR = 8050;
	private static final int ItemS = 8051;
	private static final int ItemT = 8052;
	private static final int ItemU = 8053;
	private static final int ItemW = 8054;
	private static final int ItemY = 8055;
	
	protected static int _generated;
	protected Future<?> _task = null;
	protected Future<?> _chesttask = null;
	private Clan _winner;
	
	protected static final Word[] WORLD_LIST = new Word[8];
	
	private static class Word
	{
		private final String _name;
		private final int[][] _items;
		
		public Word(String name, int[]... items)
		{
			_name = name;
			_items = items;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int[][] getItems()
		{
			return _items;
		}
	}
	
	private static final int[] GOURDS =
	{
	        35588, 35589, 35590, 35591
	};
	
	private static Spawner[] _gourds = new Spawner[4];
	private static Npc[] _yetis = new Npc[4];
	protected Npc _chest1;
	protected Npc _chest2;
	protected Npc _chest3;
	protected Npc _chest4;
	
	private static final int[] YETIS =
	{
	        35596, 35597, 35598, 35599
	};
	
	private static final int[][] ARENAS =
	{
	        {
	                151562, -127080, -2214
			},
			{
			        153141, -125335, -2214
			},
			{
			        153892, -127530, -2214
			},
			{
			        155657, -125752, -2214
			},
	};
	
	private static final int[][] YETIS_SPAWN =
	{
	        {
	                151560, -127075, -2221
			},
			{
			        153129, -125337, -2221
			},
			{
			        153884, -127534, -2221
			},
			{
			        156657, -125753, -2221
			},
	};
	
	protected static final int[][] CHESTS_SPAWN =
	{
	        {
	                151560, -127075, -2221
			},
			{
			        153129, -125337, -2221
			},
			{
			        153884, -127534, -2221
			},
			{
			        155657, -125753, -2221
			},
			
	};
	
	protected final int[] arenaChestsCnt =
	{
	        0, 0, 0, 0
	};
	
	private static final Skill[] DEBUFFS =
	{
	        SkillsParser.getInstance().getInfo(4991, 1)
	};
	
	static
	{
		WORLD_LIST[0] = new Word("BABYDUCK", new int[]
		{
		        ItemB, 2
		}, new int[]
		{
		        ItemA, 1
		}, new int[]
		{
		        ItemY, 1
		}, new int[]
		{
		        ItemD, 1
		}, new int[]
		{
		        ItemU, 1
		}, new int[]
		{
		        ItemC, 1
		}, new int[]
		{
		        ItemK, 1
		});
		WORLD_LIST[1] = new Word("ALBATROS", new int[]
		{
		        ItemA, 2
		}, new int[]
		{
		        ItemL, 1
		}, new int[]
		{
		        ItemB, 1
		}, new int[]
		{
		        ItemT, 1
		}, new int[]
		{
		        ItemR, 1
		}, new int[]
		{
		        ItemO, 1
		}, new int[]
		{
		        ItemS, 1
		});
		WORLD_LIST[2] = new Word("PELICAN", new int[]
		{
		        ItemP, 1
		}, new int[]
		{
		        ItemE, 1
		}, new int[]
		{
		        ItemL, 1
		}, new int[]
		{
		        ItemI, 1
		}, new int[]
		{
		        ItemC, 1
		}, new int[]
		{
		        ItemA, 1
		}, new int[]
		{
		        ItemN, 1
		});
		WORLD_LIST[3] = new Word("KINGFISHER", new int[]
		{
		        ItemK, 1
		}, new int[]
		{
		        ItemI, 1
		}, new int[]
		{
		        ItemN, 1
		}, new int[]
		{
		        ItemG, 1
		}, new int[]
		{
		        ItemF, 1
		}, new int[]
		{
		        ItemI, 1
		}, new int[]
		{
		        ItemS, 1
		}, new int[]
		{
		        ItemH, 1
		}, new int[]
		{
		        ItemE, 1
		}, new int[]
		{
		        ItemR, 1
		});
		WORLD_LIST[4] = new Word("CYGNUS", new int[]
		{
		        ItemC, 1
		}, new int[]
		{
		        ItemY, 1
		}, new int[]
		{
		        ItemG, 1
		}, new int[]
		{
		        ItemN, 1
		}, new int[]
		{
		        ItemU, 1
		}, new int[]
		{
		        ItemS, 1
		});
		WORLD_LIST[5] = new Word("TRITON", new int[]
		{
		        ItemT, 2
		}, new int[]
		{
		        ItemR, 1
		}, new int[]
		{
		        ItemI, 1
		}, new int[]
		{
		        ItemN, 1
		});
		WORLD_LIST[6] = new Word("RAINBOW", new int[]
		{
		        ItemR, 1
		}, new int[]
		{
		        ItemA, 1
		}, new int[]
		{
		        ItemI, 1
		}, new int[]
		{
		        ItemN, 1
		}, new int[]
		{
		        ItemB, 1
		}, new int[]
		{
		        ItemO, 1
		}, new int[]
		{
		        ItemW, 1
		});
		WORLD_LIST[7] = new Word("SPRING", new int[]
		{
		        ItemS, 1
		}, new int[]
		{
		        ItemP, 1
		}, new int[]
		{
		        ItemR, 1
		}, new int[]
		{
		        ItemI, 1
		}, new int[]
		{
		        ItemN, 1
		}, new int[]
		{
		        ItemG, 1
		});
	}
	
	public RainbowSpringsSiege(int questId, String name, String descr, int hallId)
	{
		super(questId, name, descr, hallId);
		
		addFirstTalkId(MESSENGER);
		addFirstTalkId(CARETAKER);
		addFirstTalkId(YETIS);
		addTalkId(MESSENGER);
		addTalkId(CARETAKER);
		addTalkId(YETIS);
		
		for (final int squashes : GOURDS)
		{
			addSpawnId(squashes);
			addKillId(squashes);
		}
		addSpawnId(ENRAGED_YETI);
		
		addSkillSeeId(YETIS);
		
		addKillId(CHEST);
		
		_generated = -1;
		_winner = ClanHolder.getInstance().getClan(_hall.getOwnerId());
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (player.getQuestState(getName()) == null)
		{
			newQuestState(player);
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		final int npcId = npc.getId();
		
		if (npcId == MESSENGER)
		{
			final String main = (_hall.getOwnerId() > 0) ? "35604-01.htm" : "35604-00.htm";
			html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/" + main);
			html.replace("%nextSiege%", TimeUtils.toSimpleFormat(_hall.getSiegeDate()));
			if (_hall.getOwnerId() > 0)
			{
				html.replace("%owner%", ClanHolder.getInstance().getClan(_hall.getOwnerId()).getName());
			}
			player.sendPacket(html);
		}
		else if (npcId == CARETAKER)
		{
			final String main = (_hall.isInSiege() || !_hall.isWaitingBattle()) ? "35603-00.htm" : "35603-01.htm";
			html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/" + main);
			player.sendPacket(html);
		}
		else if (ArrayUtils.contains(YETIS, npcId))
		{
			final Clan clan = player.getClan();
			if (_acceptedClans.contains(clan))
			{
				final int index = _acceptedClans.indexOf(clan);
				if (npcId == YETIS[index])
				{
					if (!player.isClanLeader())
					{
						html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/35596-00.htm");
					}
					else
					{
						html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/35596-01.htm");
					}
				}
				else
				{
					html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/35596-06.htm");
				}
			}
			else
			{
				html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/35596-06.htm");
			}
			player.sendPacket(html);
		}
		player.setLastQuestNpcObject(npc.getObjectId());
		return "";
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final Clan clan = player.getClan();
		
		switch (npc.getId())
		{
			case MESSENGER :
				switch (event)
				{
					case "Register" :
						if (!player.isClanLeader())
						{
							htmltext = "35604-07.htm";
						}
						else if ((clan.getCastleId() > 0) || (clan.getFortId() > 0) || (clan.getHideoutId() > 0))
						{
							htmltext = "35604-09.htm";
						}
						else if (!_hall.isRegistering())
						{
							htmltext = "35604-11.htm";
						}
						else if (_warDecreesCount.containsKey(clan.getId()))
						{
							htmltext = "35604-10.htm";
						}
						else if (getAttackers().size() >= 4)
						{
							htmltext = "35604-18.htm";
						}
						else if ((clan.getLevel() < 3) || (!_isDebug && clan.getMembersCount() < 5))
						{
							htmltext = "35604-08.htm";
						}
						else
						{
							final ItemInstance warDecrees = player.getInventory().getItemByItemId(WAR_DECREES);
							if (warDecrees == null)
							{
								htmltext = "35604-05.htm";
							}
							else
							{
								final long count = warDecrees.getCount();
								_warDecreesCount.put(clan.getId(), count);
								player.destroyItemByItemId("Rainbow Springs Registration", WAR_DECREES, count, npc, true);
								registerClan(clan, count, true);
								htmltext = "35604-06.htm";
							}
						}
						break;
					case "Cancel" :
						if (!player.isClanLeader())
						{
							htmltext = "35604-08.htm";
						}
						else if (!_warDecreesCount.containsKey(clan.getId()))
						{
							htmltext = "35604-12.htm";
						}
						else if (!_hall.isRegistering())
						{
							htmltext = "35604-13.htm";
						}
						else
						{
							_warDecreesCount.remove(clan.getId());
							registerClan(clan, 0, false);
							htmltext = "35604-17.htm";
						}
						break;
					case "Unregister" :
						if (_hall.isRegistering())
						{
							if (clan == null)
							{
								htmltext = "35604-12.htm";
							}
							else if (_warDecreesCount.containsKey(clan.getId()))
							{
								player.addItem("Rainbow Spring unregister", WAR_DECREES, _warDecreesCount.get(clan.getId()) / 2, npc, true);
								_warDecreesCount.remove(clan.getId());
								registerClan(clan, 0, false);
								htmltext = "35604-14.htm";
							}
							else
							{
								htmltext = "35604-16.htm";
							}
						}
						else if (_hall.isWaitingBattle())
						{
							_acceptedClans.remove(clan);
							htmltext = "35604-16.htm";
						}
						break;
				}
				break;
			case CARETAKER :
				switch (event)
				{
					case "GoToArena" :
						final Party party = player.getParty();
						if (clan == null)
						{
							htmltext = "35603-07.htm";
						}
						else if (!player.isClanLeader())
						{
							htmltext = "35603-02.htm";
						}
						else if (!player.isInParty() && !_isDebug)
						{
							htmltext = "35603-03.htm";
						}
						else if (!_isDebug && (party.getLeaderObjectId() != player.getObjectId()))
						{
							htmltext = "35603-04.htm";
						}
						else
						{
							final int clanId = player.getClanId();
							boolean nonClanMemberInParty = false;
							if (!_isDebug)
							{
								for (final Player member : party.getMembers())
								{
									if (member.getClanId() != clanId)
									{
										nonClanMemberInParty = true;
										break;
									}
								}
							}
							
							if (nonClanMemberInParty)
							{
								htmltext = "35603-05.htm";
							}
							else if (!_isDebug && party.getMemberCount() < 5)
							{
								htmltext = "35603-06.htm";
							}
							if ((clan.getCastleId() > 0) || (clan.getFortId() > 0) || (clan.getHideoutId() > 0))
							{
								htmltext = "35603-08.htm";
							}
							else if (clan.getLevel() < Config.CHS_CLAN_MINLEVEL)
							{
								htmltext = "35603-09.htm";
							}
							else if (!_acceptedClans.contains(clan))
							{
								htmltext = "35603-10.htm";
							}
							else
							{
								portToArena(player, _acceptedClans.indexOf(clan));
							}
							return null;
						}
						break;
				}
				break;
		}
		
		if (event.startsWith("getItem"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			boolean has = true;
			if (_generated == -1)
			{
				has = false;
			}
			else
			{
				final Word word = WORLD_LIST[_generated];
				
				if (_generated == 0)
				{
					if ((player.getInventory().getInventoryItemCount(ItemB, -1) >= 2) && (player.getInventory().getInventoryItemCount(ItemA, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemY, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemD, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemU, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemC, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemK, -1) >= 1))
					{
						has = true;
					}
					else
					{
						has = false;
					}
				}
				
				if (_generated == 1)
				{
					if ((player.getInventory().getInventoryItemCount(ItemA, -1) >= 2) && (player.getInventory().getInventoryItemCount(ItemL, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemB, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemT, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemR, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemO, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemS, -1) >= 1))
					{
						has = true;
					}
					else
					{
						has = false;
					}
				}
				
				if (_generated == 2)
				{
					if ((player.getInventory().getInventoryItemCount(ItemP, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemE, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemL, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemI, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemC, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemA, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemN, -1) >= 1))
					{
						has = true;
					}
					else
					{
						has = false;
					}
				}
				
				if (_generated == 3)
				{
					if ((player.getInventory().getInventoryItemCount(ItemK, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemI, -1) >= 2) && (player.getInventory().getInventoryItemCount(ItemN, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemG, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemF, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemS, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemH, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemE, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemR, -1) >= 1))
					{
						has = true;
					}
					else
					{
						has = false;
					}
				}
				
				if (_generated == 4)
				{
					if ((player.getInventory().getInventoryItemCount(ItemC, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemY, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemG, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemN, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemU, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemS, -1) >= 1))
					{
						has = true;
					}
					else
					{
						has = false;
					}
				}
				
				if (_generated == 5)
				{
					if ((player.getInventory().getInventoryItemCount(ItemT, -1) >= 2) && (player.getInventory().getInventoryItemCount(ItemR, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemI, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemO, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemN, -1) >= 1))
					{
						has = true;
					}
					else
					{
						has = false;
					}
				}
				
				if (_generated == 6)
				{
					if ((player.getInventory().getInventoryItemCount(ItemR, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemA, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemI, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemN, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemB, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemO, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemW, -1) >= 1))
					{
						has = true;
					}
					else
					{
						has = false;
					}
				}
				
				if (_generated == 7)
				{
					if ((player.getInventory().getInventoryItemCount(ItemS, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemP, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemR, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemI, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemN, -1) >= 1) && (player.getInventory().getInventoryItemCount(ItemG, -1) >= 1))
					{
						has = true;
					}
					else
					{
						has = false;
					}
				}
				
				if (has)
				{
					for (final int[] itemInfo : word.getItems())
					{
						player.destroyItemByItemId("Rainbow Item", itemInfo[0], itemInfo[1], player, true);
					}
					
					final int rnd = Rnd.get(100);
					if ((_generated >= 0) && (_generated <= 5))
					{
						if (rnd < 70)
						{
							addItem(player, RAINBOW_NECTAR);
						}
						else if (rnd < 80)
						{
							addItem(player, RAINBOW_MWATER);
						}
						else if (rnd < 90)
						{
							addItem(player, RAINBOW_WATER);
						}
						else
						{
							addItem(player, RAINBOW_SULFUR);
						}
					}
					else
					{
						if (rnd < 10)
						{
							addItem(player, RAINBOW_NECTAR);
						}
						else if (rnd < 40)
						{
							addItem(player, RAINBOW_MWATER);
						}
						else if (rnd < 70)
						{
							addItem(player, RAINBOW_WATER);
						}
						else
						{
							addItem(player, RAINBOW_SULFUR);
						}
					}
				}
				
				if (!has)
				{
					html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/35596-02.htm");
				}
				else
				{
					html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/35596-04.htm");
				}
				player.sendPacket(html);
			}
			return null;
		}
		else if (event.startsWith("seeItem"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, player.getLang(), "data/html/scripts/clanhallsiege/" + getName() + "/35596-05.htm");
			if (_generated == -1)
			{
				html.replace("%word%", "<fstring>" + NpcStringId.UNDECIDED + "</fstring>");
			}
			else
			{
				html.replace("%word%", WORLD_LIST[_generated].getName());
			}
			player.sendPacket(html);
			return null;
		}
		return htmltext;
	}
	
	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		final Clan clan = caster.getClan();
		
		if ((clan == null) || !_acceptedClans.contains(clan))
		{
			return null;
		}
		
		final int index = _acceptedClans.indexOf(clan);
		int warIndex = Integer.MIN_VALUE;
		
		if (ArrayUtils.contains(targets, npc))
		{
			if (npc.isInsideRadius(caster, 60, false, false))
			{
				switch (skill.getId())
				{
					case 2240 :
						if (getRandom(100) < 10)
						{
							addSpawn(ENRAGED_YETI, caster.getX() + 10, caster.getY() + 10, caster.getZ(), 0, false, 0, false);
						}
						reduceGourdHp(index, caster);
						break;
					case 2241 :
						warIndex = rndEx(_acceptedClans.size(), index);
						if (warIndex == Integer.MIN_VALUE)
						{
							return null;
						}
						increaseGourdHp(warIndex);
						break;
					case 2242 :
						warIndex = rndEx(_acceptedClans.size(), index);
						if (warIndex == Integer.MIN_VALUE)
						{
							return null;
						}
						moveGourds(warIndex);
						break;
					case 2243 :
						warIndex = rndEx(_acceptedClans.size(), index);
						if (warIndex == Integer.MIN_VALUE)
						{
							return null;
						}
						castDebuffsOnEnemies(caster, warIndex);
						break;
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, final Player killer, boolean isSummon)
	{
		final Clan clan = killer.getClan();
		final int index = _acceptedClans.indexOf(clan);
		
		if ((clan == null) || !_acceptedClans.contains(clan))
		{
			return null;
		}
		
		if (npc.getId() == CHEST)
		{
			chestDie(npc);
			if (chests.contains(npc))
			{
				chests.remove(npc);
			}
			
			final int chance = Rnd.get(100);
			if (chance <= 5)
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemA, 1);
			}
			else if ((chance > 5) && (chance <= 10))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemB, 1);
			}
			else if ((chance > 10) && (chance <= 15))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemC, 1);
			}
			else if ((chance > 15) && (chance <= 20))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemD, 1);
			}
			else if ((chance > 20) && (chance <= 25))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemE, 1);
			}
			else if ((chance > 25) && (chance <= 30))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemF, 1);
			}
			else if ((chance > 30) && (chance <= 35))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemG, 1);
			}
			else if ((chance > 35) && (chance <= 40))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemH, 1);
			}
			else if ((chance > 40) && (chance <= 45))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemI, 1);
			}
			else if ((chance > 45) && (chance <= 50))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemK, 1);
			}
			else if ((chance > 50) && (chance <= 55))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemL, 1);
			}
			else if ((chance > 55) && (chance <= 60))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemN, 1);
			}
			else if ((chance > 60) && (chance <= 65))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemO, 1);
			}
			else if ((chance > 65) && (chance <= 70))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemP, 1);
			}
			else if ((chance > 70) && (chance <= 75))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemR, 1);
			}
			else if ((chance > 75) && (chance <= 80))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemS, 1);
			}
			else if ((chance > 80) && (chance <= 85))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemT, 1);
			}
			else if ((chance > 85) && (chance <= 90))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemU, 1);
			}
			else if ((chance > 90) && (chance <= 95))
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemW, 1);
			}
			else if (chance > 95)
			{
				((ChestInstance) npc).dropSingleItem(killer, ItemY, 1);
			}
		}
		
		if (npc.getId() == GOURDS[index])
		{
			_missionAccomplished = true;
			_winner = ClanHolder.getInstance().getClan(clan.getId());
			
			synchronized (this)
			{
				cancelSiegeTask();
				endSiege();
				
				ThreadPoolManager.getInstance().schedule(new Runnable()
				{
					@Override
					public void run()
					{
						for (final var pl : _playersOnArena)
						{
							if (pl != null && pl.isOnline())
							{
								pl.teleToLocation(TeleportWhereType.TOWN, true, pl.getReflection());
							}
						}
						_playersOnArena.clear();
					}
				}, 120 * 1000);
			}
		}
		return null;
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		if (npc.getId() == ENRAGED_YETI)
		{
			npc.broadcastPacketToOthers(2000, new CreatureSay(npc.getObjectId(), Say2.SHOUT, npc.getName(null), NpcStringId.OOOH_WHO_POURED_NECTAR_ON_MY_HEAD_WHILE_I_WAS_SLEEPING));
		}
		
		if (ArrayUtils.contains(GOURDS, npc.getId()))
		{
			npc.setIsParalyzed(true);
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public void startSiege()
	{
		if (_acceptedClans == null || _acceptedClans.isEmpty() || _acceptedClans.size() < 2)
		{
			onSiegeEnds();
			_acceptedClans.clear();
			_hall.updateNextSiege();
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
			sm.addString(Util.clanHallName(null, _hall.getId()));
			Announcements.getInstance().announceToAll(sm);
			return;
		}
		spawnGourds();
		spawnYetis();
	}
	
	@Override
	public void prepareOwner()
	{
		if (_hall.getOwnerId() > 0)
		{
			registerClan(ClanHolder.getInstance().getClan(_hall.getOwnerId()), 10000, true);
		}
		_hall.banishForeigners();
		final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.REGISTRATION_TERM_FOR_S1_ENDED);
		msg.addString(Util.clanHallName(null, _hall.getId()));
		Announcements.getInstance().announceToAll(msg);
		_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
		_siegeTask = ThreadPoolManager.getInstance().schedule(new SiegeStarts(), 3600000);
	}
	
	@Override
	public void endSiege()
	{
		if (_hall.getOwnerId() > 0)
		{
			final Clan clan = ClanHolder.getInstance().getClan(_hall.getOwnerId());
			clan.setHideoutId(0);
			_hall.free();
		}
		super.endSiege();
	}
	
	@Override
	public void onSiegeEnds()
	{
		unSpawnGourds();
		unSpawnYetis();
		unSpawnChests();
		clearTables();
	}
	
	protected void portToArena(Player leader, int arena)
	{
		if ((arena < 0) || (arena > 3))
		{
			_log.warn(getName() + ": Wrong arena id passed: " + arena);
			return;
		}
		
		if (_isDebug && leader.getParty() == null)
		{
			leader.stopAllEffects();
			if (leader.hasSummon())
			{
				leader.getSummon().unSummon(leader);
			}
			_playersOnArena.add(leader);
			leader.teleToLocation(ARENAS[arena][0], ARENAS[arena][1], ARENAS[arena][2], true, leader.getReflection());
		}
		else
		{
			for (final Player pc : leader.getParty().getMembers())
			{
				if (pc != null)
				{
					pc.stopAllEffects();
					if (pc.hasSummon())
					{
						pc.getSummon().unSummon(pc);
					}
					_playersOnArena.add(pc);
					pc.teleToLocation(ARENAS[arena][0], ARENAS[arena][1], ARENAS[arena][2], true, pc.getReflection());
				}
			}
		}
	}
	
	protected void spawnYetis()
	{
		if (_acceptedClans == null || _acceptedClans.isEmpty())
		{
			return;
		}
		
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			if (_yetis[i] == null)
			{
				try
				{
					_yetis[i] = addSpawn(YETIS[i], YETIS_SPAWN[i][0], YETIS_SPAWN[i][1], YETIS_SPAWN[i][2], 0, false, 0, false);
					_yetis[i].setHeading(1);
					_task = ThreadPoolManager.getInstance().scheduleAtFixedRate(new GenerateTask(_yetis[i]), 10000, 300000);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	protected void spawnGourds()
	{
		if (_acceptedClans == null || _acceptedClans.isEmpty() || _acceptedClans.size() < 2)
		{
			return;
		}
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			try
			{
				_gourds[i] = new Spawner(NpcsParser.getInstance().getTemplate(GOURDS[i]));
				_gourds[i].setX(ARENAS[i][0] + 150);
				_gourds[i].setY(ARENAS[i][1] + 150);
				_gourds[i].setZ(ARENAS[i][2]);
				_gourds[i].setHeading(1);
				_gourds[i].setAmount(1);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
			SpawnParser.getInstance().addNewSpawn(_gourds[i]);
			_gourds[i].init();
		}
		_chesttask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new ChestsSpawn(), 5000, 5000);
	}
	
	protected void unSpawnYetis()
	{
		if (_acceptedClans == null || _acceptedClans.isEmpty())
		{
			return;
		}
		
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			if (_yetis[i] != null)
			{
				_yetis[i].deleteMe();
			}
			
			if (_task != null)
			{
				_task.cancel(false);
				_task = null;
			}
		}
	}
	
	protected void unSpawnGourds()
	{
		if (_acceptedClans == null || _acceptedClans.isEmpty())
		{
			return;
		}
		
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			if (_gourds[i] != null && _gourds[i].getLastSpawn() != null)
			{
				_gourds[i].getLastSpawn().deleteMe();
				SpawnParser.getInstance().deleteSpawn(_gourds[i]);
			}
		}
	}
	
	protected void unSpawnChests()
	{
		if (!chests.isEmpty())
		{
			for (final Npc chest : chests)
			{
				if (chest != null)
				{
					chest.deleteMe();
					if (_chesttask != null)
					{
						_chesttask.cancel(false);
						_chesttask = null;
					}
				}
			}
		}
	}
	
	private static void moveGourds(int index)
	{
		final Spawner[] tempArray = _gourds;
		for (int i = 0; i < index; i++)
		{
			final Spawner oldSpawn = _gourds[(index - 1) - i];
			final Spawner curSpawn = tempArray[i];
			
			_gourds[(index - 1) - i] = curSpawn;
			
			final int newX = oldSpawn.getX();
			final int newY = oldSpawn.getY();
			final int newZ = oldSpawn.getZ();
			
			curSpawn.getLastSpawn().teleToLocation(newX, newY, newZ, true, curSpawn.getReflection());
		}
	}
	
	private static void reduceGourdHp(int index, Player player)
	{
		final Spawner gourd = _gourds[index];
		if (gourd != null && gourd.getLastSpawn() != null)
		{
			gourd.getLastSpawn().reduceCurrentHp(1000, player, null);
		}
	}
	
	private static void increaseGourdHp(int index)
	{
		final Spawner gourd = _gourds[index];
		if (gourd != null)
		{
			final Npc gourdNpc = gourd.getLastSpawn();
			if (gourdNpc != null)
			{
				gourdNpc.setCurrentHp(gourdNpc.getCurrentHp() + 1000);
			}
		}
	}
	
	private void castDebuffsOnEnemies(Player player, int myArena)
	{
		if (_acceptedClans.contains(player.getClan()))
		{
			final int index = _acceptedClans.indexOf(player.getClan());
			
			if (_playersOnArena.contains(player))
			{
				for (final Player pl : _playersOnArena)
				{
					if (index != myArena)
					{
						continue;
					}
					
					if (pl != null && pl.isOnline())
					{
						for (final Skill sk : DEBUFFS)
						{
							sk.getEffects(pl, pl, false);
						}
					}
				}
			}
		}
	}
	
	private void registerClan(Clan clan, long count, boolean register)
	{
		if (register)
		{
			if (getAttackers().containsKey(clan.getId()))
			{
				return;
			}
			getAttackers().clear();
			final var warDecrees = _warDecreesCount.entrySet().stream().sorted(Map.Entry.<Integer, Long> comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			_acceptedClans.clear();
			if (_hall.getOwnerId() > 0)
			{
				final SiegeClan sc = new SiegeClan(_hall.getOwnerId(), SiegeClanType.ATTACKER);
				getAttackers().put(_hall.getOwnerId(), sc);
			}
			final int spotLeft = _hall.getOwnerId() > 0 ? 3 : 4;
			int i = 0;
			for (final var entry : warDecrees.entrySet())
			{
				final var clanId = entry.getKey();
				final Clan actingClan = ClanHolder.getInstance().getClan(clanId);
				if ((actingClan == null) || (actingClan.getDissolvingExpiryTime() > 0))
				{
					_warDecreesCount.remove(clanId);
					continue;
				}
				if (i >= spotLeft)
				{
					break;
				}
				i++;
				_acceptedClans.add(actingClan);
				final SiegeClan sc = new SiegeClan(actingClan.getId(), SiegeClanType.ATTACKER);
				getAttackers().put(actingClan.getId(), sc);
			}
			updateAttacker(clan.getId(), count, false);
		}
		else
		{
			updateAttacker(clan.getId(), 0, true);
		}
	}
	
	private static void updateAttacker(int clanId, long count, boolean remove)
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement;
			if (remove)
			{
				statement = con.prepareStatement("DELETE FROM rainbowsprings_attacker_list WHERE clanId = ?");
				statement.setInt(1, clanId);
			}
			else
			{
				statement = con.prepareStatement("INSERT INTO rainbowsprings_attacker_list VALUES (?,?)");
				statement.setInt(1, clanId);
				statement.setLong(2, count);
			}
			statement.execute();
			statement.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public final void loadAttackers()
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("SELECT * FROM rainbowsprings_attacker_list");
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				final int clanId = rset.getInt("clanId");
				final long count = rset.getLong("war_decrees_count");
				_warDecreesCount.put(clanId, count);
			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.warn(getName() + ".loadAttackers()->" + e.getMessage());
			e.printStackTrace();
		}
		
		if (!_warDecreesCount.isEmpty())
		{
			final var warDecrees = _warDecreesCount.entrySet().stream().sorted(Map.Entry.<Integer, Long> comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			_acceptedClans.clear();
			if (_hall.getOwnerId() > 0)
			{
				final SiegeClan sc = new SiegeClan(_hall.getOwnerId(), SiegeClanType.ATTACKER);
				getAttackers().put(_hall.getOwnerId(), sc);
			}
			final int spotLeft = _hall.getOwnerId() > 0 ? 3 : 4;
			int i = 0;
			for (final var entry : warDecrees.entrySet())
			{
				final var clanId = entry.getKey();
				final Clan actingClan = ClanHolder.getInstance().getClan(clanId);
				if ((actingClan == null) || (actingClan.getDissolvingExpiryTime() > 0))
				{
					_warDecreesCount.remove(clanId);
					continue;
				}
				if (i >= spotLeft)
				{
					break;
				}
				i++;
				_acceptedClans.add(actingClan);
				final SiegeClan sc = new SiegeClan(actingClan.getId(), SiegeClanType.ATTACKER);
				getAttackers().put(actingClan.getId(), sc);
			}
		}
	}
	
	private void clearTables()
	{
		try (
		    Connection con = DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement stat1 = con.prepareStatement("DELETE FROM rainbowsprings_attacker_list");
			stat1.execute();
			stat1.close();
		}
		catch (final Exception e)
		{
			_log.warn(getName() + ".clearTables()->" + e.getMessage());
		}
	}
	
	protected final class ChestsSpawn implements Runnable
	{
		@Override
		public void run()
		{
			for (int i = 0; i < _acceptedClans.size(); i++)
			{
				if (arenaChestsCnt[i] < 4)
				{
					final Npc chest = addSpawn(CHEST, CHESTS_SPAWN[i][0] + getRandom(-400, 400), CHESTS_SPAWN[i][1] + getRandom(-400, 400), CHESTS_SPAWN[i][2], 0, false, 0, false);
					if (chest != null)
					{
						chests.add(chest);
					}
					arenaChestsCnt[i]++;
				}
				
				if (arenaChestsCnt[i] < 4)
				{
					final Npc chest = addSpawn(CHEST, CHESTS_SPAWN[i][0] + getRandom(-400, 400), CHESTS_SPAWN[i][1] + getRandom(-400, 400), CHESTS_SPAWN[i][2], 0, false, 0, false);
					if (chest != null)
					{
						chests.add(chest);
					}
					arenaChestsCnt[i]++;
				}
				
				if (arenaChestsCnt[i] < 4)
				{
					final Npc chest = addSpawn(CHEST, CHESTS_SPAWN[i][0] + getRandom(-400, 400), CHESTS_SPAWN[i][1] + getRandom(-400, 400), CHESTS_SPAWN[i][2], 0, false, 0, false);
					if (chest != null)
					{
						chests.add(chest);
					}
					arenaChestsCnt[i]++;
				}
				
				if (arenaChestsCnt[i] < 4)
				{
					final Npc chest = addSpawn(CHEST, CHESTS_SPAWN[i][0] + getRandom(-400, 400), CHESTS_SPAWN[i][1] + getRandom(-400, 400), CHESTS_SPAWN[i][2], 0, false, 0, false);
					if (chest != null)
					{
						chests.add(chest);
					}
					arenaChestsCnt[i]++;
				}
			}
		}
	}
	
	private void addItem(Player player, int itemId)
	{
		player.getInventory().addItem("Rainbow Item", itemId, 1, player, null);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
		sm.addItemName(itemId);
		player.sendPacket(sm);
	}
	
	protected final class GenerateTask implements Runnable
	{
		protected final Npc _npc;
		
		protected GenerateTask(Npc npc)
		{
			_npc = npc;
		}
		
		@Override
		public void run()
		{
			_generated = getRandom(WORLD_LIST.length);
			final Word word = WORLD_LIST[_generated];
			
			final ExShowScreenMessage msg = new ExShowScreenMessage(word.getName(), 5000);
			final int region = MapRegionManager.getInstance().getMapRegionLocId(_npc.getX(), _npc.getY());
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (region == MapRegionManager.getInstance().getMapRegionLocId(player.getX(), player.getY()))
				{
					if (Util.checkIfInRange(750, _npc, player, false))
					{
						player.sendPacket(msg);
					}
				}
			}
		}
	}
	
	protected void chestDie(Npc npc)
	{
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			arenaChestsCnt[i]--;
		}
	}
	
	private int rndEx(int size, int ex)
	{
		int rnd = Integer.MIN_VALUE;
		for (int i = 0; i < Byte.MAX_VALUE; i++)
		{
			rnd = Rnd.get(size);
			if (rnd != ex)
			{
				break;
			}
		}
		return rnd;
	}
	
	@Override
	public Clan getWinner()
	{
		return _winner;
	}
	
	public static void main(String[] args)
	{
		new RainbowSpringsSiege(-1, RainbowSpringsSiege.class.getSimpleName(), "clanhallsiege", RAINBOW_SPRINGS);
	}
}
