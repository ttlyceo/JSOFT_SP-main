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
package l2e.scripts.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.Rnd;
import l2e.commons.util.TimeUtils;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.WorldEventParser;
import l2e.gameserver.instancemanager.CoupleManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Couple;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.entity.events.AbstractWorldEvent;
import l2e.gameserver.model.entity.events.model.template.WorldEventSpawn;
import l2e.gameserver.model.entity.events.model.template.WorldEventTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.SpecialCamera;

/**
 * Updated by LordWinter 13.07.2020
 */
public class GrandWedding extends AbstractWorldEvent
{
	private boolean _isActive = false;
	private WorldEventTemplate _template = null;
	private ScheduledFuture<?> _eventTask = null;
	
	private final List<Npc> _npcList = new ArrayList<>();

	private static Reflection _instance = ReflectionManager.DEFAULT;

	private static int PIXY_ID = 102500;
	private static int MANAGER = 102510;
	private static int ANAKIM = 102509;
	private static int GIFT = 102502;

	private static int[] Gourd =
	{
	        102504, 102513
	};

	private static int[] entertainmentId =
	{
	        102501, 102511, 102512
	};

	private static int[] specialGuests =
	{
	        102517, 102518, 102519, 102520, 102521, 102522
	};
	private static int[] NPCS =
	{
	        GIFT, ANAKIM, MANAGER
	};

	private static int[] numberGuards =
	{
	        1, 1, 1, 1, 1, 1, 1, 1, 1, 1
	};

	private static boolean HUSBAND_OK = false;
	private static boolean WIFE_OK = false;

	private static int _weddingLocked = 0;
	private static int _weddingStep = 0;
	private static int _husbandCoupleId = 0;
	private static int _wifeCoupleId = 0;

	private static Player _husband;
	private static Player _wife;
	private static Npc _giftBox;
	private static Npc _anakim;
	private static Collection<Player> _players = null;
	private static List<Npc> guards = new ArrayList<>();
	private static List<Player> _weddingList = new ArrayList<>();
	private static List<Npc> _guests = new ArrayList<>();
	private static List<Npc> _pixies = new ArrayList<>();
	private static List<Npc> _entertainment = new ArrayList<>();
	private static List<Npc> _entertainment2 = new ArrayList<>();
	private static List<Npc> _gourds = new ArrayList<>();
	private static Npc _pet1;
	private static Npc _pet2;

	public GrandWedding(String name, String descr)
	{
		super(name, descr);

		addStartNpc(NPCS[2]);
		for (final int i : NPCS)
		{
			addFirstTalkId(i);
			addTalkId(i);
		}
		
		_template = WorldEventParser.getInstance().getEvent(8);
		if (_template != null && !_isActive)
		{
			if (_template.isNonStop())
			{
				eventStart(-1, false);
			}
			else
			{
				final long startTime = calcEventStartTime(_template, false);
				final long expireTime = calcEventStopTime(_template, false);
				if (startTime <= System.currentTimeMillis() && expireTime > System.currentTimeMillis() || (expireTime < startTime && expireTime > System.currentTimeMillis()))
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
				else
				{
					checkTimerTask(startTime);
				}
			}
		}
	}

	@Override
	public boolean isEventActive()
	{
		return _isActive;
	}

	@Override
	public WorldEventTemplate getEventTemplate()
	{
		return _template;
	}
	
	@Override
	public boolean eventStart(long totalTime, boolean force)
	{
		if (_isActive || totalTime == 0)
		{
			return false;
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_isActive = true;
		
		final List<WorldEventSpawn> spawnList = _template.getSpawnList();
		if (spawnList != null && !spawnList.isEmpty())
		{
			for (final WorldEventSpawn spawn : spawnList)
			{
				_npcList.add(addSpawn(spawn.getNpcId(), spawn.getLocation().getX(), spawn.getLocation().getY(), spawn.getLocation().getZ(), spawn.getLocation().getHeading(), false, 0));
			}
		}

		final ServerMessage msg = new ServerMessage("GrandWedding.START", true);
		Announcements.getInstance().announceToAll(msg);
		if (totalTime > 0)
		{
			_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					eventStop();
				}
			}, totalTime);
			_log.info("Event " + _template.getName(null) + " will end in: " + TimeUtils.toSimpleFormat(System.currentTimeMillis() + totalTime));
		}
		return true;
	}

	@Override
	public boolean eventStop()
	{
		if (!_isActive)
		{
			return false;
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		_isActive = false;

		if (!_npcList.isEmpty())
		{
			for (final Npc _npc : _npcList)
			{
				if (_npc != null)
				{
					_npc.deleteMe();
				}
			}
		}
		_npcList.clear();

		final ServerMessage msg = new ServerMessage("GrandWedding.STOP", true);
		Announcements.getInstance().announceToAll(msg);
		
		checkTimerTask(calcEventStartTime(_template, false));
		
		return true;
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		int xx;
		int yy;
		int zz;
		int x1;
		int y1;
		int x2;
		int y2;

		if (event.equals("EngageRequest"))
		{
			final boolean sex = player.getAppearance().getSex();
			final boolean married = player.isMarried();
			if (!married)
			{
				if (sex == false)
				{
					if (HUSBAND_OK == false)
					{
						if ((_husbandCoupleId == 0) && (_wifeCoupleId == 0))
						{
							if (player.getQuestState(getName()).getQuestItemsCount(57) >= Config.WEDDING_PRICE)
							{
								_weddingList.clear();
								player.getQuestState(getName()).takeItems(57, Config.WEDDING_PRICE);
								_husband = player;
								HUSBAND_OK = true;
								_husbandCoupleId = player.getCoupleId();
								startQuestTimer("WeddingAnswer", 120000, null, null);
								for (final Npc i : _npcList)
								{
									_players = World.getInstance().getAroundPlayers(i);
									if (_players.contains(_husband))
									{
										i.broadcastPacketToOthers(new CreatureSay(i.getId(), 0, "Wedding Manager", new ServerMessage("GrandWedding.MSG_01", _husband.getLang()).toString()));
									}
								}
							}
							else
							{
								return "102510-07.htm";
							}
						}
						if (_wifeCoupleId == player.getCoupleId())
						{
							_husband = player;
							HUSBAND_OK = true;
							_husbandCoupleId = player.getCoupleId();
							_weddingLocked = 1;
							cancelQuestTimer("WeddingAnswer", null, null);
							startQuestTimer("WeddingAnnounce", 10000, null, null);
							for (final Npc i : _npcList)
							{
								_players = World.getInstance().getAroundPlayers(i);
								if (_players.contains(_husband))
								{
									i.broadcastPacketToOthers(new CreatureSay(i.getId(), 0, "Wedding Manager", new ServerMessage("GrandWedding.MSG_02", _husband.getLang()).toString()));
								}
							}
						}
						if (_husband != player)
						{
							return "102510-08.htm";
						}
					}
					else
					{
						return "102510-08.htm";
					}
				}
				else if (sex == true)
				{
					if (WIFE_OK == false)
					{
						if ((_husbandCoupleId == 0) && (_wifeCoupleId == 0))
						{
							if (player.getQuestState(getName()).getQuestItemsCount(57) >= Config.WEDDING_PRICE)
							{
								_weddingList.clear();
								player.getQuestState(getName()).takeItems(57, Config.WEDDING_PRICE);
								_wife = player;
								WIFE_OK = true;
								_wifeCoupleId = player.getCoupleId();
								cancelQuestTimer("WeddingAnswer", null, null);
								startQuestTimer("WeddingAnswer", 120000, null, null);
								for (final Npc i : _npcList)
								{
									_players = World.getInstance().getAroundPlayers(i);
									if (_players.contains(_wife))
									{
										i.broadcastPacketToOthers(new CreatureSay(i.getId(), 0, "Wedding Manager", new ServerMessage("GrandWedding.MSG_03", _wife.getLang()).toString()));
									}
								}
							}
							else
							{
								return "102510-07.htm";
							}
						}
						if (_husbandCoupleId == player.getCoupleId())
						{
							_wife = player;
							WIFE_OK = true;
							_wifeCoupleId = player.getCoupleId();
							_weddingLocked = 1;
							cancelQuestTimer("WeddingAnswer", null, null);
							startQuestTimer("WeddingAnnounce", 10000, null, null);
							for (final Npc i : _npcList)
							{
								_players = World.getInstance().getAroundPlayers(i);
								if (_players.contains(_wife))
								{
									i.broadcastPacketToOthers(new CreatureSay(i.getId(), 0, "Wedding Manager", new ServerMessage("GrandWedding.MSG_02", _wife.getLang()).toString()));
								}
							}
						}
						if (_wife != player)
						{
							return "102510-08.htm";
						}

					}
					else
					{
						return "102510-08.htm";
					}
				}
			}
			else
			{
				return "102510-09.htm";
			}
		}
		if (event.equals("WeddingAnswer"))
		{
			HUSBAND_OK = false;
			WIFE_OK = false;
			_husband = null;
			_wife = null;
			_husbandCoupleId = 0;
			_wifeCoupleId = 0;
			_weddingLocked = 0;
			_weddingStep = 0;
			for (final Npc i : _npcList)
			{
				i.broadcastPacketToOthers(new CreatureSay(i.getId(), 0, "Wedding Manager", new ServerMessage("GrandWedding.MSG_04", _husband.getLang()).toString()));
			}
		}
		if (event.equals("WeddingAnnounce"))
		{
			final ServerMessage msg = new ServerMessage("GrandWedding.MSG_05", true);
			msg.add(_husband.getName(null));
			msg.add(_wife.getName(null));
			Announcements.getInstance().announceToAll(msg);
			startQuestTimer("WeddingTeleportAnnounce", 285000, null, null);
			startQuestTimer("WeddingTeleport", 300000, null, null);
		}
		if (event.equals("WeddingList"))
		{
			if (!_weddingList.contains(player))
			{
				if ((player != _wife) && (player != _husband))
				{
					_weddingList.add(player);
					return "102510-10.htm";
				}
				else
				{
					return "102510-11.htm";
				}
			}
			else
			{
				return "102510-12.htm";
			}
		}
		if (event.equals("WeddingTeleportAnnounce"))
		{
			Announcements.getInstance().announceToAll(new ServerMessage("GrandWedding.MSG_06", true));
		}
		if (event.equals("WeddingTeleport"))
		{
			xx = 0;
			yy = 0;

			_instance = ReflectionManager.getInstance().createRef();
			_instance.setPvPInstance(false);

			final ServerMessage msg = new ServerMessage("GrandWedding.MSG_07", _husband.getLang());

			_weddingLocked = 2;
			_husband.teleToLocation(-51659, -54137, -2820, true, _instance);
			_husband.sendMessage(msg.toString());
			_husband.setIsParalyzed(true);
			_wife.teleToLocation(-51659, -54194, -2819, true, _instance);
			_wife.sendMessage(msg.toString());
			_wife.setIsParalyzed(true);
			if (_weddingList.size() > 0)
			{
				for (final Player i : _weddingList)
				{
					xx = -51848 + (Rnd.get(100) - 50);
					yy = -54165 + (Rnd.get(100) - 50);
					i.teleToLocation(xx, yy, -2826, true, _instance);
					i.setIsParalyzed(true);
				}
			}
			startQuestTimer("WeddingGuardsSpawn", 60000, null, null);
		}
		if (event.equals("WeddingGuardsSpawn"))
		{
			Npc guard = null;
			int val = 1;
			guards.clear();
			y1 = -54091;
			y2 = -54242;
			x1 = -51480;
			x2 = -51480;
			for (int i = 0; i < numberGuards.length; i++)
			{
				x1 = x1 + val;
				x2 = x2 + val;
				guard = addSpawn(102503, x1, y1, -2808, 15308, false, 0, false, getReflection());
				guards.add(guard);
				guard = addSpawn(102503, x2, y2, -2808, 48643, false, 0, false, getReflection());
				guards.add(guard);
				val = 80;
			}
			startQuestTimer("guardsPart2", 6000, null, null);
		}
		if (event.equals("guardsPart2"))
		{
			zz = guards.get(0).getZ();
			for (int i = 0; i < guards.size(); i += 2)
			{
				final int xx1 = guards.get(i).getX();
				final int yy1 = guards.get(i).getY() - 30;
				final int xx2 = guards.get(i + 1).getX();
				final int yy2 = guards.get(i + 1).getY() + 30;
				guards.get(i).getAI().setIntention(CtrlIntention.MOVING, new Location(xx1, yy1, zz, 0), 0);
				guards.get(i + 1).getAI().setIntention(CtrlIntention.MOVING, new Location(xx2, yy2, zz, 0), 0);
			}
			startQuestTimer("guardsPart3", 2500, null, null);
		}
		if (event.equals("guardsPart3"))
		{
			for (final Npc i : guards)
			{
				i.broadcastPacketToOthers(new SocialAction(i.getObjectId(), 2));
			}

			startQuestTimer("AnakimSpawn", 2000, null, null);
		}
		if (event.equals("AnakimSpawn"))
		{
			_anakim = addSpawn(ANAKIM, -52241, -54176, -2827, 0, false, 0, false, getReflection());
			startQuestTimer("AnakimSpeak", 100, null, null);
		}
		if (event.equals("AnakimSpeak"))
		{
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_08", _husband.getLang()).toString()));
			_anakim.broadcastPacketToOthers(new SpecialCamera(_anakim.getObjectId(), 200, 0, 150, 0, 5000));
			startQuestTimer("AnakimAnim", 1000, null, null);
			startQuestTimer("AnakimPets", 8000, null, null);
		}
		if (event.equals("AnakimAnim"))
		{
			_anakim.broadcastPacketToOthers(new SocialAction(_anakim.getObjectId(), 2));
		}
		if (event.equals("AnakimPets"))
		{
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_09", _husband.getLang()).toString()));
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_10", _husband.getLang()).toString()));
			_pet1 = addSpawn(102514, -52241, -54146, -2827, 0, false, 0, false, getReflection());
			_pet2 = addSpawn(102514, -52241, -54206, -2827, 0, false, 0, false, getReflection());
			startQuestTimer("AnakimWalk", 3000, null, null);
		}
		if (event.equals("AnakimWalk"))
		{
			_anakim.getAI().setIntention(CtrlIntention.MOVING, new Location(-49877, -54168, -2688, 0), 0);
			startQuestTimer("petsWalk", 1500, null, null);
		}
		if (event.equals("petsWalk"))
		{
			_pet1.getAI().setIntention(CtrlIntention.MOVING, new Location(-49896, -54116, -2688, 0), 0);
			_pet2.getAI().setIntention(CtrlIntention.MOVING, new Location(-49896, -54220, -2688, 0), 0);
			startQuestTimer("AnakimHeading", 27000, null, null);
			_anakim.broadcastPacketToOthers(new SpecialCamera(_anakim.getObjectId(), 400, 180, 150, 0, 31500));
		}
		if (event.equals("AnakimHeading"))
		{
			_anakim.getAI().setIntention(CtrlIntention.MOVING, new Location(-49984, -54168, -2688, 0), 0);
			_pet2.getAI().setIntention(CtrlIntention.MOVING, new Location(-49976, -54241, -2688, 0), 0);
			startQuestTimer("petsHeading", 100, null, null);
			startQuestTimer("witnessSpawn", 500, null, null);
			startQuestTimer("AnakimSpeak2", 3000, null, null);
		}
		if (event.equals("petsHeading"))
		{
			_pet1.getAI().setIntention(CtrlIntention.MOVING, new Location(-49976, -54104, -2688, 0), 0);
		}
		if (event.equals("witnessSpawn"))
		{
			Npc witness = addSpawn(102508, -50034, -54068, -2688, 48643, false, 0, false, getReflection());
			guards.add(witness);
			witness = addSpawn(102507, -50034, -54268, -2688, 15308, false, 0, false, getReflection());
			guards.add(witness);
		}
		if (event.equals("AnakimSpeak2"))
		{
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_11", _husband.getLang()).toString()));
			startQuestTimer("AnakimSpeak3", 2000, null, null);
		}
		if (event.equals("AnakimSpeak3"))
		{
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_12", _husband.getLang()).toString()));
			startQuestTimer("PixiesSpawn", 1000, null, null);
			startQuestTimer("PixiesCamera", 10500, null, null);
		}
		if (event.equals("PixiesSpawn"))
		{
			for (int i = 0; i < 45; i++)
			{
				xx = -51910 + (Rnd.get(120) - 60);
				yy = -54985 + (Rnd.get(120) - 60);
				final Npc pixy = addSpawn(PIXY_ID, xx, yy, -2824, 0, false, 0, false, getReflection());
				pixy.setRunning();
				_pixies.add(pixy);
			}
			startQuestTimer("pixiesMove1", 9000, null, null);
		}
		if (event.equals("PixiesCamera"))
		{
			_pixies.get(0).broadcastPacketToOthers(new SpecialCamera(_pixies.get(0).getObjectId(), 400, 180, 150, 0, 14000));
		}
		if (event.equals("pixiesMove1"))
		{
			for (final Npc i : _pixies)
			{
				xx = -51433 + (Rnd.get(250) - 125);
				yy = -54725 + (Rnd.get(250) - 125);
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2827, 0), 0);
			}
			startQuestTimer("pixiesMove2", 3000, null, null);
		}
		if (event.equals("pixiesMove2"))
		{
			for (final Npc i : _pixies)
			{
				xx = -51848 + (Rnd.get(60) - 30);
				yy = -54165 + (Rnd.get(60) - 30);
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2826, 0), 0);
			}
			startQuestTimer("pixiesMove3", 2500, null, null);
		}
		if (event.equals("pixiesMove3"))
		{
			for (final Npc i : _pixies)
			{
				xx = -51228 + (Rnd.get(1200) - 600);
				yy = -54178 + (Rnd.get(1200) - 600);
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2809, 0), 0);
			}
			startQuestTimer("AnakimSpeak4", 5000, null, null);
		}
		if (event.equals("AnakimSpeak4"))
		{
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_13", _husband.getLang()).toString()));
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_14", _husband.getLang()).toString()));
			startQuestTimer("entertainmentSpawn", 1000, null, null);
		}
		if (event.equals("entertainmentSpawn"))
		{
			for (int i = 0; i < 24; i++)
			{
				final int rr = Rnd.get(3);
				xx = -53714 + (Rnd.get(150) - 75);
				yy = -54142 + (Rnd.get(150) - 75);
				final Npc show = addSpawn(entertainmentId[rr], xx, yy, -2674, 0, false, 0, false, getReflection());
				show.setRunning();
				_entertainment.add(show);
			}
			startQuestTimer("entertainmentMove", 4000, null, null);
		}
		if (event.equals("showCamera"))
		{
			_entertainment.get(0).broadcastPacketToOthers(new SpecialCamera(_entertainment.get(0).getObjectId(), 400, 180, 150, 0, 20000));
		}
		if (event.equals("entertainmentMove"))
		{
			for (final Npc i : _entertainment)
			{
				xx = -52083 + (Rnd.get(100) - 50);
				yy = -54117 + (Rnd.get(100) - 50);
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2826, 0), 0);
			}
			startQuestTimer("entertainmentMove2", 10500, null, null);
		}
		if (event.equals("entertainmentMove2"))
		{
			for (final Npc i : _entertainment)
			{
				xx = -51770 + (Rnd.get(220) - 110);
				yy = -54863 + (Rnd.get(220) - 110);
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2825, 0), 0);
			}
			for (final Npc show : _entertainment)
			{
				show.setWalking();
			}

			startQuestTimer("showCamera", 100, null, null);
			startQuestTimer("entertainmentMove3", 10500, null, null);
		}
		if (event.equals("entertainmentMove3"))
		{
			for (final Npc i : _entertainment)
			{
				xx = -51150 + (Rnd.get(200) - 100);
				yy = -54511 + (Rnd.get(200) - 100);
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2825, 0), 0);
			}
			startQuestTimer("AnakimSpeak5", 10000, null, null);
		}
		if (event.equals("AnakimSpeak5"))
		{
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_15", _husband.getLang()).toString()));
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_16", _husband.getLang()).toString()));
			startQuestTimer("SpecialGuestsSpawn", 4000, null, null);
		}
		if (event.equals("SpecialGuestsSpawn"))
		{
			for (int i = 0; i < specialGuests.length; i++)
			{
				x1 = -51311 + (Rnd.get(500) - 250);
				y1 = -53695 + (Rnd.get(500) - 250);
				final Npc guard = addSpawn(specialGuests[i], x1, y1, -2808, 58609, false, 0, false, getReflection());
				_guests.add(guard);
			}
			startQuestTimer("GuestCamera", 100, null, null);
			startQuestTimer("AnakimSpeak6", 8500, null, null);
			startQuestTimer("CoupleMarch", 10000, null, null);
		}
		if (event.equals("GuestCamera"))
		{
			_guests.get(0).broadcastPacketToOthers(new SpecialCamera(_guests.get(0).getObjectId(), 1000, 180, 150, 0, 6000));
		}
		if (event.equals("AnakimSpeak6"))
		{
			final ServerMessage msg = new ServerMessage("GrandWedding.MSG_17", _husband.getLang());
			msg.add(_wife.getName(null));
			msg.add(_husband.getName(null));
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", msg.toString()));
			startQuestTimer("AnakimSpeak7", 24000, null, null);
		}
		if (event.equals("CoupleMarch"))
		{
			for (final Player i : _weddingList)
			{
				i.sendPacket(new PlaySound(1, "ns23_f", 0, 0, i.getX(), i.getY(), i.getZ()));
			}

			_wife.sendPacket(new PlaySound(1, "ns23_f", 0, 0, _wife.getX(), _wife.getY(), _wife.getZ()));
			_husband.sendPacket(new PlaySound(1, "ns23_f", 0, 0, _husband.getX(), _husband.getY(), _husband.getZ()));
			_husband.setIsParalyzed(false);
			_wife.setIsParalyzed(false);
			_husband.setWalking();
			_wife.setWalking();
			_husband.broadcastPacketToOthers(new SpecialCamera(_husband.getObjectId(), 700, 180, 140, 0, 20000));
			_wife.getAI().setIntention(CtrlIntention.MOVING, new Location(-50042, -54178, -2688, 0), 0);
			_husband.getAI().setIntention(CtrlIntention.MOVING, new Location(-50042, -54147, -2688, 0), 0);
			for (final Player i : _weddingList)
			{
				i.setIsParalyzed(false);
			}
		}
		if (event.equals("AnakimSpeak7"))
		{
			final ServerMessage msg = new ServerMessage("GrandWedding.MSG_18", _husband.getLang());
			msg.add(_husband.getName(null));
			msg.add(_wife.getName(null));
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", msg.toString()));
			_weddingStep = 1;
		}
		if (event.equals("AnakimSpeak8"))
		{
			final ServerMessage msg = new ServerMessage("GrandWedding.MSG_19", _husband.getLang());
			msg.add(_wife.getName(null));
			msg.add(_husband.getName(null));
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", msg.toString()));
			_weddingStep = 2;
		}
		if (event.equals("AnakimSpeak9"))
		{
			_anakim.broadcastPacketToOthers(new CreatureSay(_anakim.getId(), 0, "Anakim", new ServerMessage("GrandWedding.MSG_20", _husband.getLang()).toString()));
			_weddingStep = 0;
			_husband.setPartnerId(_wife.getObjectId());
			_husband.setMarryAccepted(true);
			_husband.setMarried(true);
			_wife.setPartnerId(_husband.getObjectId());
			_wife.setMarryAccepted(true);
			_wife.setMarried(true);
			_husband.setRunning();
			_wife.setRunning();
			final Couple couple = CoupleManager.getInstance().getCouple(_husbandCoupleId);
			couple.marry();
			if (_husband != null)
			{
				_husband.getCounters().addAchivementInfo("getMarried", 0, -1, false, false, false);
			}
			if (_wife != null)
			{
				_wife.getCounters().addAchivementInfo("getMarried", 0, -1, false, false, false);
			}
			_anakim.doCast(SkillsParser.getInstance().getInfo(2025, 1));
			for (final Npc i : guards)
			{
				final int rr = Rnd.get(1);
				if (rr == 0)
				{
					i.doCast(SkillsParser.getInstance().getInfo(2024, 1));
				}
				if (rr == 1)
				{
					i.doCast(SkillsParser.getInstance().getInfo(2023, 1));
				}
			}
			startQuestTimer("WeddingFinale", 3000, null, null);
		}
		if (event.equals("WeddingFinale"))
		{
			for (final Npc i : _pixies)
			{
				xx = -51228 + (Rnd.get(1200) - 600);
				yy = -54178 + (Rnd.get(1200) - 600);
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2809, 0), 0);
			}
			for (int i = 0; i < 25; i++)
			{
				final int rr = Rnd.get(2);
				xx = -51228 + (Rnd.get(1200) - 600);
				yy = -54178 + (Rnd.get(1200) - 600);
				final Npc gourd = addSpawn(Gourd[rr], xx, yy, -2824, 0, false, 0, false, getReflection());
				_gourds.add(gourd);
			}
			startQuestTimer("WeddingFinale2", 4000, null, null);
		}
		if (event.equals("WeddingFinale2"))
		{
			for (final Npc i : _guests)
			{
				i.deleteMe();
			}

			for (final Npc i : guards)
			{
				i.deleteMe();
			}

			for (final Npc i : _entertainment)
			{
				xx = -51862 + (Rnd.get(50) - 25);
				yy = -54451 + (Rnd.get(50) - 25);
				i.setRunning();
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2825, 0), 0);
			}
			for (int n = 0; n < 25; n++)
			{
				_gourds.get(n).reduceCurrentHp(999999, _gourds.get(n), null);
			}

			startQuestTimer("WeddingFinale3", 6000, null, null);
		}
		if (event.equals("WeddingFinale3"))
		{
			for (int i = 0; i < 12; i++)
			{
				_entertainment2.add(_entertainment.get(i));
				_entertainment.remove(i);
			}

			for (final Npc i : _entertainment)
			{
				xx = -51867;
				yy = -54209;
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2825, 0), 0);
			}
			for (final Npc i : _entertainment2)
			{
				xx = -51867;
				yy = -54120;
				i.getAI().setIntention(CtrlIntention.MOVING, new Location(xx, yy, -2825, 0), 0);
			}
			startQuestTimer("WeddingFinale4", 6000, null, null);
		}
		if (event.equals("WeddingFinale4"))
		{
			int val = 1;
			x1 = -51480 + 30;
			x2 = -51480 - 30;
			for (int i = 0; i < _entertainment.size(); i++)
			{
				x1 = x1 + val;
				x2 = x2 + val;
				yy = -54209;
				_entertainment.get(i).getAI().setIntention(CtrlIntention.MOVING, new Location(x1, yy, -2808, 0), 0);
				_entertainment2.get(i).getAI().setIntention(CtrlIntention.MOVING, new Location(x2, yy, -2808, 0), 0);
				val = 80;
			}
			startQuestTimer("WeddingFinale5", 6000, null, null);
		}
		if (event.equals("WeddingFinale5"))
		{
			_entertainment2.get(0).broadcastPacketToOthers(new CreatureSay(_entertainment2.get(0).getId(), 0, "Ceremony Staff", new ServerMessage("GrandWedding.MSG_21", _husband.getLang()).toString()));
			startQuestTimer("WeddingFinale6", 4000, null, null);
		}
		if (event.equals("WeddingFinale6"))
		{
			_giftBox = addSpawn(GIFT, _husband.getX() + 20, _husband.getY() + 20, _husband.getZ(), 0, false, 0, false, getReflection());
			startQuestTimer("weddingDespawn", 5000, null, null);
		}
		if (event.equals("weddingDespawn"))
		{
			HUSBAND_OK = false;
			WIFE_OK = false;
			_husbandCoupleId = 0;
			_wifeCoupleId = 0;
			_weddingLocked = 0;
			_weddingStep = 0;
			_anakim.deleteMe();
			_pet1.deleteMe();
			_pet2.deleteMe();
			for (final Npc s : _entertainment2)
			{
				s.deleteMe();
			}
			for (final Npc s : _entertainment)
			{
				s.deleteMe();
			}
			for (final Npc n : _pixies)
			{
				n.deleteMe();
			}
			for (final Npc i : guards)
			{
				i.deleteMe();
			}
		}
		if (event.equals("destroyInstance"))
		{
			if (!_instance.isDefault())
			{
				_husband.setReflection(ReflectionManager.DEFAULT);
				_wife.setReflection(ReflectionManager.DEFAULT);
				for (final Player i : _weddingList)
				{
					i.setReflection(ReflectionManager.DEFAULT);
				}
				_instance.collapse();
			}
			_husband = null;
			_wife = null;
			_weddingList.clear();
		}
		return "";
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = "Error";
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}

		final int npcId = npc.getId();
		if (npcId == GIFT)
		{
			if ((player == _husband) || (player == _wife))
			{
				htmltext = "102502-01.htm";
				
				calcReward(_husband, _template, 1);
				calcReward(_wife, _template, 1);
				_giftBox.deleteMe();
				startQuestTimer("destroyInstance", 5000, null, null);
			}
			else
			{
				htmltext = "102502-02.htm";
			}
		}
		else if (npcId == MANAGER)
		{
			final int level = player.getLevel();
			switch (_weddingLocked)
			{
				case 0 :
					if (level >= 60)
					{
						if (player.getCoupleId() == 0)
						{
							htmltext = "102510-03.htm";
						}
						else
						{
							if (wearsFormalWear(player))
							{
								htmltext = "102510-01.htm";
							}
							else
							{
								htmltext = "102510-04.htm";
							}
						}
					}
					else
					{
						htmltext = "102510-05.htm";
					}
					break;
				case 1 :
					htmltext = "102510-02.htm";
					break;
				case 2 :
					player.teleToLocation(-51848, -54165, -2826, true, player.getReflection());
					htmltext = "102510-06.htm";
					break;
			}
		}
		else if (npcId == ANAKIM)
		{
			if ((player == _husband) || player.getName(null).equals(_husband.getName(null)))
			{
				if (_weddingStep == 1)
				{
					htmltext = "102509-01.htm";
				}
				else
				{
					htmltext = "102509-03.htm";
				}
			}
			else if ((player == _wife) || player.getName(null).equals(_wife.getName(null)))
			{
				if (_weddingStep == 2)
				{
					htmltext = "102509-02.htm";
				}
				else
				{
					htmltext = "102509-03.htm";
				}
			}
			else
			{
				htmltext = "102509-04.htm";
			}
		}
		return htmltext;
	}

	protected boolean wearsFormalWear(Player player)
	{
		if (Config.WEDDING_FORMALWEAR)
		{
			final ItemInstance fw1 = player.getChestArmorInstance();

			if ((fw1 == null) || (fw1.getId() != 6408))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void startTimerTask(long time)
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
			_eventTask = null;
		}
		
		_eventTask = ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				final long expireTime = calcEventStopTime(_template, false);
				if (expireTime > System.currentTimeMillis())
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
			}
		}, (time - System.currentTimeMillis()));
		_log.info("Event " + _template.getName(null) + " will start in: " + TimeUtils.toSimpleFormat(time));
	}

	protected Reflection getReflection()
	{
		return _instance;
	}
	
	@Override
	public boolean isReloaded()
	{
		if (isEventActive())
		{
			return false;
		}
		
		if (_template != null)
		{
			if (_template.isNonStop())
			{
				eventStart(-1, false);
			}
			else
			{
				final long startTime = calcEventStartTime(_template, false);
				final long expireTime = calcEventStopTime(_template, false);
				if (startTime <= System.currentTimeMillis() && expireTime > System.currentTimeMillis() || (expireTime < startTime && expireTime > System.currentTimeMillis()))
				{
					eventStart(expireTime - System.currentTimeMillis(), false);
				}
				else
				{
					checkTimerTask(startTime);
				}
			}
			return true;
		}
		return false;
	}

	public static void main(String[] args)
	{
		new GrandWedding(GrandWedding.class.getSimpleName(), "events");
	}
}
