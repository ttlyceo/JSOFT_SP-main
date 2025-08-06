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
package l2e.gameserver.model.entity.events.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.collections.MultiValueSet;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.FightEventParser;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventGameRoom;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.olympiad.OlympiadManager;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.ShowTutorialMark;
import l2e.gameserver.network.serverpackets.TutorialCloseHtml;
import l2e.gameserver.network.serverpackets.TutorialShowHtml;
import l2e.gameserver.taskmanager.EventTaskManager;

/**
 * Created by LordWinter
 */
public class FightEventManager extends LoggerObject
{
	public enum CLASSES
	{
		FIGHTERS(0, ClassId.fighter, ClassId.warrior, ClassId.gladiator, ClassId.warlord, ClassId.knight, ClassId.rogue, ClassId.elvenFighter, ClassId.elvenKnight, ClassId.elvenScout, ClassId.darkFighter, ClassId.palusKnight, ClassId.assassin, ClassId.orcFighter, ClassId.orcRaider, ClassId.destroyer, ClassId.orcMonk, ClassId.tyrant, ClassId.dwarvenFighter, ClassId.scavenger, ClassId.bountyHunter, ClassId.artisan, ClassId.warsmith, ClassId.maleSoldier, ClassId.femaleSoldier, ClassId.trooper, ClassId.warder, ClassId.berserker, ClassId.maleSoulbreaker, ClassId.femaleSoulbreaker, ClassId.inspector, ClassId.duelist, ClassId.dreadnought, ClassId.titan, ClassId.grandKhavatari, ClassId.maestro, ClassId.doombringer, ClassId.maleSoulhound, ClassId.femaleSoulhound), TANKS(1, ClassId.paladin, ClassId.darkAvenger, ClassId.templeKnight, ClassId.shillienKnight, ClassId.phoenixKnight, ClassId.hellKnight, ClassId.evaTemplar, ClassId.shillienTemplar, ClassId.trickster), ARCHERS(2, ClassId.hawkeye, ClassId.silverRanger, ClassId.phantomRanger, ClassId.arbalester, ClassId.sagittarius, ClassId.moonlightSentinel, ClassId.ghostSentinel, ClassId.fortuneSeeker), DAGGERS(3, ClassId.treasureHunter, ClassId.plainsWalker, ClassId.abyssWalker, ClassId.adventurer, ClassId.windRider, ClassId.ghostHunter), MAGES(4, ClassId.mage, ClassId.wizard, ClassId.sorceror, ClassId.necromancer, ClassId.elvenMage, ClassId.elvenWizard, ClassId.spellsinger, ClassId.darkMage, ClassId.darkWizard, ClassId.spellhowler, ClassId.orcMage, ClassId.orcShaman, ClassId.archmage, ClassId.soultaker, ClassId.mysticMuse, ClassId.stormScreamer), SUMMONERS(5, ClassId.warlock, ClassId.elementalSummoner, ClassId.phantomSummoner, ClassId.arcanaLord, ClassId.elementalMaster, ClassId.spectralMaster), HEALERS(6, ClassId.bishop, ClassId.elder, ClassId.shillenElder, ClassId.cardinal, ClassId.evaSaint, ClassId.shillienSaint, ClassId.dominator), SUPPORTS(7, ClassId.cleric, ClassId.prophet, ClassId.swordSinger, ClassId.oracle, ClassId.bladedancer, ClassId.shillienOracle, ClassId.overlord, ClassId.warcryer, ClassId.hierophant, ClassId.swordMuse, ClassId.spectralDancer, ClassId.doomcryer, ClassId.judicator);

		private final int _transformIndex;
		private final ClassId[] _classes;

		private CLASSES(int transformId, ClassId... ids)
		{
			_transformIndex = transformId;
			_classes = ids;
		}

		public ClassId[] getClasses()
		{
			return _classes;
		}

		public int getTransformIndex()
		{
			return _transformIndex;
		}
	}

	public static final String BYPASS = "_fightEvent";

	private final Map<Integer, AbstractFightEvent> _activeEvents = new ConcurrentHashMap<>();
	private final Map<Integer, AbstractFightEvent> _activeGlobalEvents = new ConcurrentHashMap<>();
	private final Map<Integer, ScheduledFuture<?>> _eventTasks = new HashMap<>();
	private final List<FightEventGameRoom> _rooms = new CopyOnWriteArrayList<>();
	private AbstractFightEvent _nextEvent = null;

	public FightEventManager()
	{
		startAutoEventsTasks();
	}
	
	public void reload()
	{
		startAutoEventsTasks();
	}

	public void signForEvent(Player player, AbstractFightEvent event)
	{
		if (!event.isGlobal())
		{
			FightEventGameRoom room = getEventRooms(event);
			if (room != null && room.getSlotsLeft() <= 0)
			{
				player.sendMessage(new ServerMessage("FightEvents.EVENT_FULL", player.getLang()).toString());
				return;
			}
			
			if (room == null)
			{
				final AbstractFightEvent duplicatedEvent = prepareNewEvent(event);
				room = createRoom(duplicatedEvent, true);
			}
			
			room.addAlonePlayer(player);
			
			final ServerMessage msg = new ServerMessage("FightEvents.JUST_PARTICIPATE", player.getLang());
			msg.add(player.getEventName(event.getId()));
			player.sendMessage(msg.toString());
		}
		else
		{
			event.addToGlobalEvent(player);
		}
	}

	public void trySignForEvent(Player player, AbstractFightEvent event, boolean checkConditions)
	{
		if (checkConditions && !canPlayerParticipate(player, event, true, false, true))
		{
			return;
		}

		if (!isRegistrationOpened(event))
		{
			final ServerMessage msg = new ServerMessage("FightEvents.CANT_PARTICIPATE", player.getLang());
			msg.add(player.getEventName(event.getId()));
			player.sendMessage(msg.toString());
		}
		else if (isPlayerRegistered(player, event.getId()))
		{
			player.sendMessage(new ServerMessage("FightEvents.ALREADY_REG", player.getLang()).toString());
		}
		else if ((Config.DOUBLE_SESSIONS_CHECK_MAX_EVENT_PARTICIPANTS > 0) && !DoubleSessionManager.getInstance().tryAddPlayer(event.getId(), player, Config.DOUBLE_SESSIONS_CHECK_MAX_EVENT_PARTICIPANTS))
		{
			final ServerMessage msg = new ServerMessage("FightEvents.MAX_IP", player.getLang());
			msg.add(Config.DOUBLE_SESSIONS_CHECK_MAX_EVENT_PARTICIPANTS);
			player.sendMessage(msg.toString());
		}
		else
		{
			if (event.isGlobal() && (event.getEventRoom().getPlayersCount() >= event.getEventRoom().getMaxPlayers()))
			{
				player.sendMessage(new ServerMessage("FightEvents.EVENT_FULL", player.getLang()).toString());
				return;
			}
			signForEvent(player, event);
		}
	}

	public void unsignFromEvent(Player player, int eventId)
	{
		for (final FightEventGameRoom room : _rooms)
		{
			if (room != null && room.containsPlayer(player) && room.getGame().getId() == eventId)
			{
				room.leaveRoom(player);
				if (Config.DOUBLE_SESSIONS_CHECK_MAX_EVENT_PARTICIPANTS > 0)
				{
					DoubleSessionManager.getInstance().removePlayer(room.getGame().getId(), player);
				}
				player.sendMessage(new ServerMessage("FightEvents.UNREGISTER", player.getLang()).toString());
			}
		}
	}

	public void unsignFromAllEvents(Player player)
	{
		for (final FightEventGameRoom room : _rooms)
		{
			if (room != null && room.containsPlayer(player))
			{
				room.leaveRoom(player);
				if (Config.DOUBLE_SESSIONS_CHECK_MAX_EVENT_PARTICIPANTS > 0)
				{
					DoubleSessionManager.getInstance().removePlayer(room.getGame().getId(), player);
				}
				player.sendMessage(new ServerMessage("FightEvents.UNREGISTER", player.getLang()).toString());
			}
		}
	}

	public boolean isRegistrationOpened(AbstractFightEvent event)
	{
		if (event != null && event.isGlobal() && event.isInProgress())
		{
			return true;
		}
		
		for (final FightEventGameRoom room : _rooms)
		{
			if (room.getGame() != null && room.getGame().getId() == event.getId())
			{
				return true;
			}
		}
		return false;
	}

	public boolean isPlayerRegistered(Player player, int eventId)
	{
		if (player == null)
		{
			return false;
		}

		if (player.isInFightEvent())
		{
			return true;
		}

		for (final FightEventGameRoom room : _rooms)
		{
			if (room != null && room.getGame().getId() == eventId)
			{
				for (final Player iPlayer : room.getAllPlayers())
				{
					if (iPlayer.equals(player))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isPlayerRegistered(Player player)
	{
		if (player == null)
		{
			return false;
		}

		if (player.isInFightEvent())
		{
			return true;
		}

		for (final FightEventGameRoom room : _rooms)
		{
			if (room != null)
			{
				for (final Player iPlayer : room.getAllPlayers())
				{
					if (iPlayer.equals(player))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public void startEventCountdown(AbstractFightEvent event)
	{
		if (!Config.ALLOW_FIGHT_EVENTS || getEventById(event.getId()) != null)
		{
			return;
		}
		
		if (FightEventParser.getInstance().getDisabledEvents().contains(event.getId()))
		{
			return;
		}

		if (!event.isGlobal())
		{
			_nextEvent = event;
		}
		
		EventTaskManager.getInstance().removeEventTask(event, true);

		final AbstractFightEvent duplicatedEvent = prepareNewEvent(event);
		final FightEventGameRoom room = createRoom(duplicatedEvent, !duplicatedEvent.isGlobal());

		DoubleSessionManager.getInstance().registerEvent(duplicatedEvent.getId());

		if (Config.ALLOW_REG_CONFIRM_DLG)
		{
			sendEventInvitations(duplicatedEvent);
		}
		if (duplicatedEvent.isGlobal())
		{
			sendToAllMsg(duplicatedEvent, "FightEvents.STARTED");
			duplicatedEvent.prepareEvent(room, room.getAllPlayers(), true);
			FightEventNpcManager.getInstance().tryGlobalSpawnRegNpc();
		}
		else
		{
			sendToAllMsg(duplicatedEvent, "FightEvents.OPEN_REG");
			ServerMessage message = null;
			switch (Config.FIGHT_EVENTS_REG_TIME)
			{
				case 10:
				case 9:
				case 8:
				case 7:
				case 6:
				case 5:
					message = new ServerMessage("FightEvents.LAST_5MIN", true);
					break;
				case 4:
				case 3:
				case 2:
					message = new ServerMessage("FightEvents.LAST_3MIN", true);
					break;
				case 1:
					message = new ServerMessage("FightEvents.LAST_1MIN", true);
					break;
			}
			if (message != null)
			{
				for (final String lang : Config.MULTILANG_ALLOWED)
				{
					if (lang != null)
					{
						message.add(lang, duplicatedEvent.getName(lang));
						message.add(lang, Config.FIGHT_EVENTS_REG_TIME);
					}
				}
				Announcements.getInstance().announceToAll(message);
			}
			FightEventNpcManager.getInstance().trySpawnRegNpc();
			setEventTask(duplicatedEvent, (Config.FIGHT_EVENTS_REG_TIME * 60000L));
		}
	}

	public void setEventTask(AbstractFightEvent event, long time)
	{
		ScheduledFuture<?> task = _eventTasks.get(event.getId());
		if (task != null && !task.isDone())
		{
			task.cancel(false);
			task = null;
		}
		task = ThreadPoolManager.getInstance().schedule(new EventTask(event), time);
		_eventTasks.put(event.getId(), task);
	}
	
	private class EventTask implements Runnable
	{
		AbstractFightEvent _event;
		
		private EventTask(AbstractFightEvent event)
		{
			_event = event;
		}
		
		@Override
		public void run()
		{
			startEvent(_event);
		}
	}

	private void startEvent(AbstractFightEvent event)
	{
		final FightEventGameRoom room = getEventRooms(event);
		if (room == null)
		{
			return;
		}
		
		clearEventIdTask(event.getId());

		_rooms.remove(room);
		FightEventNpcManager.getInstance().tryUnspawnRegNpc();
		if (room.getPlayersCount() < 2)
		{
			info(event.getName(null) + ": Removing room because it doesnt have enough players");
			info(event.getName(null) + ": Player Counts: " + room.getPlayersCount());
			sendToAllMsg(event, "FightEvents.CANCEL");
			room.getAllPlayers().stream().filter(p -> p != null).forEach(pl -> room.leaveRoom(pl));
			DoubleSessionManager.getInstance().clear(event.getId());
			removeEventId(event.getId());
			calcNewEventTime(event);
			return;
		}
		sendToAllMsg(event, "FightEvents.STARTED");
		room.getGame().prepareEvent(room, room.getAllPlayers(), true);
	}

	private FightEventGameRoom getEventRooms(AbstractFightEvent event)
	{
		for (final FightEventGameRoom room : _rooms)
		{
			if (room.getGame() != null && room.getGame().getId() == event.getId())
			{
				return room;
			}
		}
		return null;
	}

	public void sendEventInvitations(AbstractFightEvent event)
	{
		for (final Player player : GameObjectsStorage.getPlayers())
		{
			if (canPlayerParticipate(player, event, false, true, true) && (player.getEvent(AbstractFightEvent.class) == null))
			{
				final ServerMessage msg = new ServerMessage("FightEvents.WANT_JOIN", player.getLang());
				msg.add(player.getEventName(event.getId()));
				player.sendConfirmDlg(new AnswerEventInvitation(player, event), 60000, msg.toString());
			}
		}
	}
	
	private class AnswerEventInvitation implements OnAnswerListener
	{
		private final Player _player;
		private final AbstractFightEvent _event;

		private AnswerEventInvitation(Player player, AbstractFightEvent event)
		{
			_player = player;
			_event = event;
		}

		@Override
		public void sayYes()
		{
			trySignForEvent(_player, _event, false);
		}

		@Override
		public void sayNo()
		{
		}
	}

	public FightEventGameRoom createRoom(AbstractFightEvent event, boolean toAdd)
	{
		final FightEventGameRoom newRoom = new FightEventGameRoom(event);
		if (toAdd)
		{
			_rooms.add(newRoom);
		}
		return newRoom;
	}

	public AbstractFightEvent getNextEvent()
	{
		return _nextEvent;
	}

	private void sendErrorMessageToPlayer(Player player, String msg)
	{
		player.sendPacket(new CreatureSay(player.getObjectId(), Say2.PARTYROOM_COMMANDER, new ServerMessage("FightEvents.ERROR", player.getLang()).toString(), msg));
		player.sendMessage(msg);
	}

	public void sendToAllMsg(AbstractFightEvent event, String msg)
	{
		for (final Player player : GameObjectsStorage.getPlayers())
		{
			if (player == null || player.isInOfflineMode() || player.isInFightEvent())
			{
				continue;
			}
			
			final ServerMessage message = new ServerMessage(msg, player.getLang());
			message.add(player.getEventName(event.getId()));
			player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, player.getEventName(event.getId()), message.toString()));
		}
	}

	private AbstractFightEvent prepareNewEvent(AbstractFightEvent event)
	{
		final MultiValueSet<String> set = event.getSet();
		AbstractFightEvent duplicatedEvent = null;
		try
		{
			@SuppressWarnings("unchecked")
			final Class<AbstractFightEvent> eventClass = (Class<AbstractFightEvent>) Class.forName(set.getString("eventClass"));
			final Constructor<AbstractFightEvent> constructor = eventClass.getConstructor(MultiValueSet.class);
			duplicatedEvent = constructor.newInstance(set);

			if (event.isGlobal())
			{
				_activeGlobalEvents.put(duplicatedEvent.getId(), duplicatedEvent);
			}
			else
			{
				_activeEvents.put(duplicatedEvent.getId(), duplicatedEvent);
			}

		}
		catch (
		    ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
		{
			e.printStackTrace();
		}
		return duplicatedEvent;
	}

	private void startAutoEventsTasks()
	{
		AbstractFightEvent closestEvent = null;
		long closestEventTime = Long.MAX_VALUE;

		for (final AbstractFightEvent event : FightEventParser.getInstance().getEvents().valueCollection())
		{
			if (event != null && event.isAutoTimed() && !FightEventParser.getInstance().getDisabledEvents().contains(event.getId()))
			{
				long nextEventTime = 0L;
				if (event.getAutoStartTimes() != null)
				{
					nextEventTime = event.getAutoStartTimes().next(System.currentTimeMillis());
				}
				else
				{
					nextEventTime = (System.currentTimeMillis() + (Rnd.get(1, 5) * 60000L));
				}
				EventTaskManager.getInstance().addEventTask(event, nextEventTime, false);
				if (closestEventTime > nextEventTime)
				{
					closestEvent = event;
					closestEventTime = nextEventTime;
				}
			}
		}
		_nextEvent = closestEvent;
		EventTaskManager.getInstance().recalcTime();
	}
	
	public void calcNewEventTime(AbstractFightEvent event)
	{
		if (!event.isAutoTimed())
		{
			return;
		}
		
		if (!event.isWithoutTime())
		{
			long nextEventTime = 0L;
			if (event.getAutoStartTimes() != null)
			{
				nextEventTime = event.getAutoStartTimes().next(System.currentTimeMillis());
			}
			else
			{
				nextEventTime = (System.currentTimeMillis() + (Rnd.get(1, 5) * 60000L));
			}
			EventTaskManager.getInstance().addEventTask(event, nextEventTime, true);
		}
	}

	public boolean canPlayerParticipate(Player player, AbstractFightEvent event, boolean sendMessage, boolean justMostImportant, boolean checkReflection)
	{
		if (player == null)
		{
			return false;
		}
		
		if (event.getValidLevels() != null)
		{
			if (player.getLevel() < event.getValidLevels()[0] || player.getLevel() > event.getValidLevels()[1])
			{
				final ServerMessage msg = new ServerMessage("FightEvents.WRONG_LEVEL", player.getLang());
				sendErrorMessageToPlayer(player, msg.toString());
				return false;
			}
		}
		
		if (event.getValidProffs() != null)
		{
			if (!event.getValidProffs().contains(player.getClassId().level()))
			{
				final ServerMessage msg = new ServerMessage("FightEvents.WRONG_PROFF", player.getLang());
				sendErrorMessageToPlayer(player, msg.toString());
				return false;
			}
		}
		
		if (event.getExcludedClasses() != null)
		{
			final CLASSES clazz = FightEventGameRoom.getPlayerClassGroup(player);
			if (clazz != null)
			{
				final CLASSES[] classes = event.getExcludedClasses();
				for (int i = 0; i < event.getExcludedClasses().length; i++)
				{
					if (classes[i].name().equals(clazz.name()))
					{
						final ServerMessage msg = new ServerMessage("FightEvents.WRONG_PROFF", player.getLang());
						sendErrorMessageToPlayer(player, msg.toString());
						return false;
					}
				}
			}
			else
			{
				final ServerMessage msg = new ServerMessage("FightEvents.WRONG_PROFF", player.getLang());
				sendErrorMessageToPlayer(player, msg.toString());
				return false;
			}
		}
		
		if (player.isDead() || player.isAlikeDead())
		{
			sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_DEAD", player.getLang()).toString());
			return false;
		}

		if (player.isBlocked() || player.isInKrateisCube() || player.getUCState() > 0)
		{
			return false;
		}

		if (player.getCursedWeaponEquippedId() > 0)
		{
			if (sendMessage)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_CURSE", player.getLang()).toString());
			}
			return false;
		}

		if (OlympiadManager.getInstance().isRegistered(player))
		{
			if (sendMessage)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_OLY", player.getLang()).toString());
			}
			return false;
		}

		if (player.isInOlympiadMode())
		{
			if (sendMessage)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_OLY1", player.getLang()).toString());
			}
			return false;
		}

		if (player.inObserverMode())
		{
			if (sendMessage)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_OBSERVE", player.getLang()).toString());
			}
			return false;
		}

		if (player.isJailed())
		{
			if (sendMessage)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_JAIL", player.getLang()).toString());
			}
			return false;
		}

		if (player.isInOfflineMode())
		{
			if (sendMessage)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_OFFLINE", player.getLang()).toString());
			}
			return false;
		}

		if (player.isInStoreMode())
		{
			if (sendMessage)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_STORE", player.getLang()).toString());
			}
			return false;
		}

		if (player.getReflectionId() > 0 && checkReflection)
		{
			if (sendMessage)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_REF", player.getLang()).toString());
			}
			return false;
		}

		if (player.isInDuel())
		{
			if (sendMessage)
			{
				sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_DUEL", player.getLang()).toString());
			}
			return false;
		}

		if (!justMostImportant)
		{
			if (player.isDead() || player.isAlikeDead())
			{
				if (sendMessage)
				{
					sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_DEAD", player.getLang()).toString());
				}
				return false;
			}

			if (!player.isInsideZone(ZoneId.PEACE) && player.getPvpFlag() > 0)
			{
				if (sendMessage)
				{
					sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_PVP", player.getLang()).toString());
				}
				return false;
			}

			if (player.isInCombat())
			{
				if (sendMessage)
				{
					sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_COMBAT", player.getLang()).toString());
				}
				return false;
			}

			if (player.getKarma() > 0)
			{
				if (sendMessage)
				{
					sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_PK", player.getLang()).toString());
				}
				return false;
			}

			if (player.checkInTournament())
			{
				if (sendMessage)
				{
					sendErrorMessageToPlayer(player, new ServerMessage("FightEvents.CANT_TOURNAMENT", player.getLang()).toString());
				}
				return false;
			}
		}
		return true;
	}

	public void requestEventPlayerMenuBypass(Player player, String bypass)
	{
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);

		final AbstractFightEvent event = player.getFightEvent();
		if (event == null)
		{
			return;
		}

		final FightEventPlayer fPlayer = event.getFightEventPlayer(player);
		if (fPlayer == null)
		{
			return;
		}

		fPlayer.setShowTutorial(false);

		if (!bypass.startsWith(BYPASS))
		{
			return;
		}

		final StringTokenizer st = new StringTokenizer(bypass, " ");
		st.nextToken();

		final String action = st.nextToken();

		switch (action)
		{
			case "leave" :
				askQuestion(player, new ServerMessage("FightEvents.WANT_TO_LEAVE", player.getLang()).toString());
				break;
			case "buffer" :
				final ICommunityBoardHandler handler = CommunityBoardHandler.getInstance().getHandler("_bbsbuffer");
				if (handler != null)
				{
					handler.onBypassCommand("_bbsbuffer", player);
				}
				break;
		}
	}

	public void sendEventPlayerMenu(Player player)
	{
		final AbstractFightEvent event = player.getFightEvent();
		if (event == null || event.getFightEventPlayer(player) == null)
		{
			return;
		}

		final FightEventPlayer fPlayer = event.getFightEventPlayer(player);

		fPlayer.setShowTutorial(true);

		final StringBuilder builder = new StringBuilder();
		builder.append("<html><head><title>").append(player.getEventName(event.getId())).append("</title></head>");
		builder.append("<body>");
		builder.append("<br1><img src=\"L2UI.squaregray\" width=\"290\" height=\"1\">");
		builder.append("<table height=20 fixwidth=\"290\" bgcolor=29241d>");
		builder.append("	<tr>");
		builder.append("		<td height=20 width=290>");
		builder.append("			<center><font name=\"hs12\" color=913d3d>").append(player.getEventName(event.getId())).append("</font></center>");
		builder.append("		</td>");
		builder.append("	</tr>");
		builder.append("</table>");
		builder.append("<br1><img src=\"L2UI.squaregray\" width=\"290\" height=\"1\">");
		builder.append("<br>");
		builder.append("<table fixwidth=290 bgcolor=29241d>");
		builder.append("	<tr>");
		builder.append("		<td valign=top width=280>");
		builder.append("			<font color=388344>").append(player.getEventDescr(event.getId())).append("<br></font>");
		builder.append("		</td>");
		builder.append("	</tr>");
		builder.append("</table>");
		builder.append("<br1><img src=\"L2UI.squaregray\" width=\"290\" height=\"1\">");
		builder.append("<br>");

		builder.append("<table width=270>");
		if (event.canUseCommunity())
		{
			builder.append("	<tr>");
			builder.append("		<td>");
			builder.append("			<center><button value = \"" + ServerStorage.getInstance().getString(player.getLang(), "FightEvents.BUFFER") + "\" action=\"bypass -h ").append(BYPASS).append(" buffer\" back=\"l2ui_ct1.button.OlympiadWnd_DF_Back_Down\" width=200 height=30 fore=\"l2ui_ct1.button.OlympiadWnd_DF_Back\"></center>");
			builder.append("		</td>");
			builder.append("	</tr>");
		}
		builder.append("	<tr>");
		builder.append("		<td>");
		builder.append("			<center><button value = \"" + ServerStorage.getInstance().getString(player.getLang(), "FightEvents.LEAVE_EVENT") + "\" action=\"bypass -h ").append(BYPASS).append(" leave\" back=\"l2ui_ct1.button.OlympiadWnd_DF_Back_Down\" width=200 height=30 fore=\"l2ui_ct1.button.OlympiadWnd_DF_Back\"></center>");
		builder.append("		</td>");
		builder.append("	</tr>");
		builder.append("	<tr>");
		builder.append("		<td>");
		builder.append("			<center><button value = \"" + ServerStorage.getInstance().getString(player.getLang(), "FightEvents.CLOSE") + "\" action=\"bypass -h ").append(BYPASS).append(" close\" back=\"l2ui_ct1.button.OlympiadWnd_DF_Info_Down\" width=200 height=30 fore=\"l2ui_ct1.button.OlympiadWnd_DF_Info\"></center>");
		builder.append("		</td>");
		builder.append("	</tr>");
		builder.append("</table>");

		builder.append("</body></html>");

		player.sendPacket(new TutorialShowHtml(builder.toString()));
		player.sendPacket(new ShowTutorialMark(100, 0));
	}

	private void leaveEvent(Player player)
	{
		final AbstractFightEvent event = player.getFightEvent();
		if (event == null)
		{
			return;
		}

		if (event.leaveEvent(player, true, true, true))
		{
			player.sendMessage(new ServerMessage("FightEvents.LEFT_EVENT", player.getLang()).toString());
		}
	}

	private void askQuestion(Player player, String question)
	{
		player.sendConfirmDlg(new AskQuestionAnswerListener(player), 0, question);
	}

	private class AskQuestionAnswerListener implements OnAnswerListener
	{
		private final Player _player;

		private AskQuestionAnswerListener(Player player)
		{
			_player = player;
		}

		@Override
		public void sayYes()
		{
			leaveEvent(_player);
		}

		@Override
		public void sayNo()
		{
		}

	}

	public AbstractFightEvent getEventById(int id)
	{
		if (_activeGlobalEvents.containsKey(id))
		{
			return _activeGlobalEvents.get(id);
		}
		return _activeEvents.get(id);
	}

	public Map<Integer, AbstractFightEvent> getActiveEvents()
	{
		return _activeEvents;
	}
	
	public AbstractFightEvent getGlobalEventById(int id)
	{
		return _activeGlobalEvents.get(id);
	}
	
	public Map<Integer, AbstractFightEvent> getGlobalActiveEvents()
	{
		return _activeGlobalEvents;
	}
	
	public boolean getActiveEventTask(int id)
	{
		return _eventTasks.get(id) != null;
	}
	
	public void clearEventIdTask(int eventId)
	{
		ScheduledFuture<?> task = _eventTasks.get(eventId);
		if (task != null && !task.isDone())
		{
			_eventTasks.remove(eventId);
			task.cancel(false);
			task = null;
		}
	}

	public void cleanEventId(int eventId)
	{
		for (final FightEventGameRoom room : _rooms)
		{
			if (room.getGame() != null && room.getGame().getId() == eventId)
			{
				room.cleanUp();
				_rooms.remove(room);
			}
		}
	}

	public void prepareStartEventId(int eventId)
	{
		clearEventIdTask(eventId);
		final AbstractFightEvent event = getEventById(eventId);
		if (event != null)
		{
			startEvent(event);
		}
	}

	public void removeEventId(int eventId)
	{
		if (_activeEvents.containsKey(eventId))
		{
			_activeEvents.remove(eventId);
		}
		
		if (_activeGlobalEvents.containsKey(eventId))
		{
			_activeGlobalEvents.remove(eventId);
		}
	}

	public static final FightEventManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FightEventManager _instance = new FightEventManager();
	}
}