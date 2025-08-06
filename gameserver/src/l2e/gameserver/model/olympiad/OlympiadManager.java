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
package l2e.gameserver.model.olympiad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExOlympiadMode;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class OlympiadManager
{
	private final Set<Integer> _nonClassBasedRegisters;
	private final Map<Integer, Set<Integer>> _classBasedRegisters;
	private final List<List<Integer>> _teamsBasedRegisters;

	protected OlympiadManager()
	{
		_nonClassBasedRegisters = ConcurrentHashMap.newKeySet();
		_classBasedRegisters = new ConcurrentHashMap<>();
		_teamsBasedRegisters = new CopyOnWriteArrayList<>();
	}
	
	public static final OlympiadManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public final Set<Integer> getRegisteredNonClassBased()
	{
		return _nonClassBasedRegisters;
	}

	public final Map<Integer, Set<Integer>> getRegisteredClassBased()
	{
		return _classBasedRegisters;
	}

	public final List<List<Integer>> getRegisteredTeamsBased()
	{
		return _teamsBasedRegisters;
	}

	protected final List<Set<Integer>> hasEnoughRegisteredClassed()
	{
		List<Set<Integer>> result = null;
		for (final var classList : _classBasedRegisters.entrySet())
		{
			if ((classList.getValue() != null) && (classList.getValue().size() >= Config.ALT_OLY_CLASSED))
			{
				if (result == null)
				{
					result = new CopyOnWriteArrayList<>();
				}

				result.add(classList.getValue());
			}
		}
		return result;
	}

	protected final boolean hasEnoughRegisteredNonClassed()
	{
		return _nonClassBasedRegisters.size() >= Config.ALT_OLY_NONCLASSED;
	}

	protected final boolean hasEnoughRegisteredTeams()
	{
		return _teamsBasedRegisters.size() >= Config.ALT_OLY_TEAMS;
	}

	protected final void clearRegistered()
	{
		_nonClassBasedRegisters.clear();
		_classBasedRegisters.clear();
		_teamsBasedRegisters.clear();
		DoubleSessionManager.getInstance().clear(DoubleSessionManager.OLYMPIAD_ID);
	}

	public final boolean isRegistered(Player noble)
	{
		return isRegistered(noble, noble, false);
	}

	private final boolean isRegistered(Player noble, Player player, boolean showMessage)
	{
		final var objId = noble.getObjectId();
		for (final var team : _teamsBasedRegisters)
		{
			if ((team != null) && team.contains(objId))
			{
				if (showMessage)
				{
					final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_NON_CLASS_LIMITED_EVENT_TEAMS);
					sm.addPcName(noble);
					player.sendPacket(sm);
				}
				return true;
			}
		}

		if (_nonClassBasedRegisters.contains(objId))
		{
			if (showMessage)
			{
				final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_NON_CLASS_LIMITED_MATCH_WAITING_LIST);
				sm.addPcName(noble);
				player.sendPacket(sm);
			}
			return true;
		}

		final var classed = _classBasedRegisters.get(noble.getBaseClass());
		if ((classed != null) && classed.contains(objId))
		{
			if (showMessage)
			{
				final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
				sm.addPcName(noble);
				player.sendPacket(sm);
			}
			return true;
		}
		return false;
	}

	public final boolean isRegisteredInComp(Player noble)
	{
		return isRegistered(noble, noble, false) || isInCompetition(noble, noble, false);
	}

	private final boolean isInCompetition(Player noble, Player player, boolean showMessage)
	{
		if (!Olympiad._inCompPeriod)
		{
			return false;
		}

		AbstractOlympiadGame game;
		OlympiadGameTask task;
		for (int i = OlympiadGameManager.getInstance().getNumberOfStadiums(); --i >= 0;)
		{
			task = OlympiadGameManager.getInstance().getOlympiadTask(i);
			if (task == null)
			{
				continue;
			}
			
			game = task.getGame();
			if (game == null)
			{
				continue;
			}

			if (game.containsParticipant(noble.getObjectId()))
			{
				if (!showMessage)
				{
					return true;
				}

				switch (game.getType())
				{
					case CLASSED :
					{
						final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
					case NON_CLASSED :
					{
						final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_NON_CLASS_LIMITED_MATCH_WAITING_LIST);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
					case TEAMS :
					{
						final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_NON_CLASS_LIMITED_EVENT_TEAMS);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
				}
				return true;
			}
		}
		return false;
	}

	public final boolean registerNoble(Player player, CompetitionType type)
	{
		final var isTraining = Olympiad._inTrainingPeriod;
		if (!Olympiad._inCompPeriod)
		{
			player.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}

		if (Olympiad.getInstance().getMillisToCompEnd() < 600000)
		{
			player.sendPacket(SystemMessageId.GAME_REQUEST_CANNOT_BE_MADE);
			return false;
		}

		if(player.isInPartyTournament() || player.isRegisteredInFightEvent())
		{
			player.sendMessage("You registered another Event.");
			return false;
		}

		final var charId = player.getObjectId();
		if (Olympiad.getInstance().getRemainingWeeklyMatches(charId) < 1 && !isTraining)
		{
			player.sendPacket(SystemMessageId.MAX_OLY_WEEKLY_MATCHES_REACHED);
			return false;
		}

		switch (type)
		{
			case CLASSED :
			{
				if (!checkNoble(player, player))
				{
					return false;
				}

				if (Olympiad.getInstance().getRemainingWeeklyMatchesClassed(charId) < 1 && !isTraining)
				{
					player.sendPacket(SystemMessageId.MAX_OLY_WEEKLY_MATCHES_REACHED_60_NON_CLASSED_30_CLASSED_10_TEAM);
					return false;
				}
				_classBasedRegisters.computeIfAbsent(player.getBaseClass(), k -> ConcurrentHashMap.newKeySet()).add(charId);
				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
				break;
			}
			case NON_CLASSED :
			{
				if (!checkNoble(player, player))
				{
					return false;
				}

				if (Olympiad.getInstance().getRemainingWeeklyMatchesNonClassed(charId) < 1 && !isTraining)
				{
					player.sendPacket(SystemMessageId.MAX_OLY_WEEKLY_MATCHES_REACHED_60_NON_CLASSED_30_CLASSED_10_TEAM);
					return false;
				}

				_nonClassBasedRegisters.add(charId);
				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
				break;
			}
			case TEAMS :
			{
				final var party = player.getParty();
				if ((party == null) || (party.getMemberCount() != 3))
				{
					player.sendPacket(SystemMessageId.PARTY_REQUIREMENTS_NOT_MET);
					return false;
				}
				if (!party.isLeader(player))
				{
					player.sendPacket(SystemMessageId.ONLY_PARTY_LEADER_CAN_REQUEST_TEAM_MATCH);
					return false;
				}

				int teamPoints = 0;
				final List<Integer> team = new ArrayList<>(party.getMemberCount());
				for (final var noble : party.getMembers())
				{
					if (!checkNoble(noble, player))
					{
						if (Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS > 0)
						{
							for (final var unreg : party.getMembers())
							{
								if (unreg == noble)
								{
									break;
								}
								DoubleSessionManager.getInstance().removePlayer(DoubleSessionManager.OLYMPIAD_ID, unreg);
							}
						}
						return false;
					}

					if (Olympiad.getInstance().getRemainingWeeklyMatchesTeam(noble.getObjectId()) < 1 && !isTraining)
					{
						player.sendPacket(SystemMessageId.MAX_OLY_WEEKLY_MATCHES_REACHED_60_NON_CLASSED_30_CLASSED_10_TEAM);
						return false;
					}
					team.add(noble.getObjectId());
					teamPoints += Olympiad.getInstance().getNoblePoints(noble.getObjectId());
				}
				if (teamPoints < 10 && !isTraining)
				{
					player.sendMessage("Your team must have at least 10 points in total.");
					if (Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS > 0)
					{
						for (final var unreg : party.getMembers())
						{
							DoubleSessionManager.getInstance().removePlayer(DoubleSessionManager.OLYMPIAD_ID, unreg);
						}
					}
					return false;
				}

				party.broadCast(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_REGISTERED_IN_A_WAITING_LIST_OF_TEAM_GAMES));
				_teamsBasedRegisters.add(team);
				break;
			}
		}
		return true;
	}

	public final boolean unRegisterNoble(Player noble)
	{
		if (!Olympiad._inCompPeriod)
		{
			noble.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}

		if (!noble.isNoble())
		{
			final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addString(noble.getName(null));
			noble.sendPacket(sm);
			return false;
		}

		if (!isRegistered(noble, noble, false))
		{
			noble.sendPacket(SystemMessageId.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
			return false;
		}

		if (isInCompetition(noble, noble, false))
		{
			return false;
		}
		
		final var objId = noble.getObjectId();
		if (_nonClassBasedRegisters.remove(objId))
		{
			if (Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS > 0)
			{
				DoubleSessionManager.getInstance().removePlayer(DoubleSessionManager.OLYMPIAD_ID, noble);
			}

			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			return true;
		}

		final var classed = _classBasedRegisters.get(noble.getBaseClass());
		if ((classed != null) && classed.remove(objId))
		{
			if (Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS > 0)
			{
				DoubleSessionManager.getInstance().removePlayer(DoubleSessionManager.OLYMPIAD_ID, noble);
			}

			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			return true;
		}

		for (final var team : _teamsBasedRegisters)
		{
			if ((team != null) && team.contains(objId))
			{
				_teamsBasedRegisters.remove(team);
				ThreadPoolManager.getInstance().execute(new AnnounceUnregToTeam(team));
				return true;
			}
		}
		return false;
	}

	public final void removeDisconnectedCompetitor(Player player)
	{
		final var task = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
		if ((task != null) && task.isGameStarted())
		{
			task.getGame().handleDisconnect(player);
			if ((player.getLastX() != 0) && (player.getLastY() != 0))
			{
				player.setIsInOlympiadMode(false);
				player.setIsOlympiadStart(false);
				player.setOlympiadSide(-1);
				player.setOlympiadGameId(-1);
				player.setOlympiadGame(null);
				player.sendPacket(new ExOlympiadMode(0));
				player.teleToLocation(player.getLastX(), player.getLastY(), player.getLastZ(), true, ReflectionManager.DEFAULT);
				player.setLastCords(0, 0, 0);
			}
		}

		final var objId = player.getObjectId();
		if (_nonClassBasedRegisters.remove(objId))
		{
			if (Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS > 0)
			{
				DoubleSessionManager.getInstance().removePlayer(DoubleSessionManager.OLYMPIAD_ID, player);
			}
			return;
		}
		
		final var classed = _classBasedRegisters.get(player.getBaseClass());
		if ((classed != null) && classed.remove(objId))
		{
			if (Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS > 0)
			{
				DoubleSessionManager.getInstance().removePlayer(DoubleSessionManager.OLYMPIAD_ID, player);
			}
			return;
		}

		for (final var team : _teamsBasedRegisters)
		{
			if ((team != null) && team.contains(objId))
			{
				_teamsBasedRegisters.remove(team);
				ThreadPoolManager.getInstance().execute(new AnnounceUnregToTeam(team));
				return;
			}
		}
	}

	private final boolean checkNoble(Player noble, Player player)
	{
		SystemMessage sm;
		if (!noble.isNoble())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addPcName(noble);
			player.sendPacket(sm);
			return false;
		}

		if (noble.isSubClassActive())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_CLASS_CHARACTER);
			sm.addPcName(noble);
			player.sendPacket(sm);
			return false;
		}

		if (noble.isCursedWeaponEquipped())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_JOIN_OLYMPIAD_POSSESSING_S2);
			sm.addPcName(noble);
			sm.addItemName(noble.getCursedWeaponEquippedId());
			player.sendPacket(sm);
			return false;
		}

		if (!noble.isInventoryUnder90(true))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_IN_OLYMPIAD_INVENTORY_SLOT_EXCEEDS_80_PERCENT);
			sm.addPcName(noble);
			player.sendPacket(sm);
			return false;
		}

		final var charId = noble.getObjectId();
		if (isRegistered(noble, player, true))
		{
			return false;
		}

		if (isInCompetition(noble, player, true))
		{
			return false;
		}

		Olympiad.addNoble(noble);
		
		final var points = Olympiad.getInstance().getNoblePoints(charId);
		if (points <= 0 && !Olympiad._inTrainingPeriod)
		{
			final var message = new NpcHtmlMessage(0);
			message.setFile(player, player.getLang(), "data/html/olympiad/noble_nopoints1.htm");
			message.replace("%objectId%", String.valueOf(noble.getTargetId()));
			player.sendPacket(message);
			return false;
		}

		if ((Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS > 0) && !DoubleSessionManager.getInstance().tryAddPlayer(DoubleSessionManager.OLYMPIAD_ID, noble, Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS))
		{
			final var message = new NpcHtmlMessage(0);
			message.setFile(player, player.getLang(), "data/html/mods/OlympiadIPRestriction.htm");
			message.replace("%max%", String.valueOf(Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS));
			player.sendPacket(message);
			return false;
		}

		if (player.getClassId().level() != 3)
		{
			final var message = new NpcHtmlMessage(0);
			message.setFile(player, player.getLang(), "data/html/olympiad/invalid_class.htm");
			player.sendPacket(message);
			return false;
		}
		return true;
	}

	private static final class AnnounceUnregToTeam implements Runnable
	{
		private final List<Integer> _team;

		public AnnounceUnregToTeam(List<Integer> t)
		{
			_team = t;
		}

		@Override
		public final void run()
		{
			Player teamMember;
			final var sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			for (final var objectId : _team)
			{
				teamMember = GameObjectsStorage.getPlayer(objectId);
				if (teamMember != null)
				{
					teamMember.sendPacket(sm);
					if (Config.DOUBLE_SESSIONS_CHECK_MAX_OLYMPIAD_PARTICIPANTS > 0)
					{
						DoubleSessionManager.getInstance().removePlayer(DoubleSessionManager.OLYMPIAD_ID, teamMember);
					}
				}
			}
			teamMember = null;
		}
	}
	
	public int getCountOpponents()
	{
		return _nonClassBasedRegisters.size() + _classBasedRegisters.size() + _teamsBasedRegisters.size();
	}

	private static class SingletonHolder
	{
		protected static final OlympiadManager _instance = new OlympiadManager();
	}
}