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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.zone.type.OlympiadStadiumZone;

public class OlympiadGameManager implements Runnable
{
	private static final Logger _log = LoggerFactory.getLogger(OlympiadGameManager.class);
	
	private volatile boolean _battleStarted = false;
	private final List<OlympiadGameTask> _tasks;

	protected OlympiadGameManager()
	{
		final Collection<OlympiadStadiumZone> zones = ZoneManager.getInstance().getAllZones(OlympiadStadiumZone.class);
		if ((zones == null) || zones.isEmpty())
		{
			throw new Error("No olympiad stadium zones defined !");
		}
		
		_tasks = new ArrayList<>(zones.size());
		int i = 0;
		for (final OlympiadStadiumZone zone : zones)
		{
			_tasks.add(new OlympiadGameTask(i, zone));
			i++;
		}
		
		_log.info("Olympiad System: Loaded " + _tasks.size() + " stadiums.");
	}

	public static final OlympiadGameManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected final boolean isBattleStarted()
	{
		return _battleStarted;
	}
	
	protected final void startBattle()
	{
		_battleStarted = true;
	}
	
	@Override
	public final void run()
	{
		if (Olympiad.getInstance().isOlympiadEnd())
		{
			return;
		}

		if (Olympiad.getInstance().inCompPeriod())
		{
			OlympiadGameTask task;
			AbstractOlympiadGame newGame;
			List<Set<Integer>> readyClassed = OlympiadManager.getInstance().hasEnoughRegisteredClassed();
			boolean readyNonClassed = OlympiadManager.getInstance().hasEnoughRegisteredNonClassed();
			boolean readyTeams = OlympiadManager.getInstance().hasEnoughRegisteredTeams();
			if (((readyClassed != null) || readyNonClassed || readyTeams) && isValidBattleTime())
			{
				Collections.shuffle(_tasks);
				for (int i = 0; i < _tasks.size(); i++)
				{
					task = _tasks.get(i);
					synchronized (task)
					{
						if (!task.isRunning())
						{
							if ((readyClassed != null || readyTeams) && (task.getId() % 2) == 0)
							{
								if (readyTeams && (task.getId() % 4) == 0)
								{
									newGame = OlympiadGameTeams.createGame(task.getId(), OlympiadManager.getInstance().getRegisteredTeamsBased());
									if (newGame != null)
									{
										task.attachGame(newGame);
										continue;
									}
									readyTeams = false;
								}
								
								if (readyClassed != null)
								{
									newGame = OlympiadGameClassed.createGame(task.getId(), readyClassed);
									if (newGame != null)
									{
										task.attachGame(newGame);
										continue;
									}
									readyClassed = null;
								}
							}
							
							if (readyNonClassed)
							{
								newGame = OlympiadGameNonClassed.createGame(task.getId(), OlympiadManager.getInstance().getRegisteredNonClassBased());
								if (newGame != null)
								{
									task.attachGame(newGame);
									continue;
								}
								readyNonClassed = false;
							}
						}
					}
					
					if (readyClassed == null && !readyNonClassed && !readyTeams)
					{
						break;
					}
				}
			}
		}
		else
		{
			if (isAllTasksFinished() && _battleStarted)
			{
				OlympiadManager.getInstance().clearRegistered();
				_battleStarted = false;
				_log.info("Olympiad System: All current games finished.");
			}
		}
	}
	
	public final boolean isAllTasksFinished()
	{
		for (final OlympiadGameTask task : _tasks)
		{
			if (task.isRunning())
			{
				return false;
			}
		}
		return true;
	}
	
	private boolean isValidBattleTime()
	{
		if (!Config.OLY_PAUSE_BATTLES_AT_SIEGES)
		{
			return true;
		}
		
		if (TerritoryWarManager.getInstance().isTWInProgress())
		{
			return false;
		}
		
		for (final Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (siege != null && siege.getIsInProgress())
			{
				return false;
			}
		}
		return true;
	}
	
	public final OlympiadGameTask getOlympiadTask(int id)
	{
		for (final OlympiadGameTask task : _tasks)
		{
			if (task.getId() == id)
			{
				return task;
			}
		}
		return null;
	}
	
	public final int getNumberOfStadiums()
	{
		return _tasks.size();
	}
	
	public final void notifyCompetitorDamage(Player player, int damage)
	{
		if (player == null)
		{
			return;
		}
		
		final int id = player.getOlympiadGameId();
		
		for (final OlympiadGameTask task : _tasks)
		{
			if (task.getId() == id)
			{
				final AbstractOlympiadGame game = task.getGame();
				if (game != null)
				{
					game.addDamage(player, damage);
				}
			}
		}
	}

	private static class SingletonHolder
	{
		protected static final OlympiadGameManager _instance = new OlympiadGameManager();
	}
}