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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.zone.type.OlympiadStadiumZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class OlympiadGameTask implements Runnable
{
	protected static final Logger _log = LoggerFactory.getLogger(OlympiadGameTask.class);
	
	public static final int[] TELEPORT_TO_ARENA =
	{
	        120, 119, 118, 117, 116, 115, 114, 113, 112, 111, 110, 109, 60, 59, 58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 15, 10, 5, 4, 3, 2, 1, 0
	};
	
	public static final int[] BATTLE_START_TIME_FIRST =
	{
	        60, 55, 50, 40, 30, 20, 10, 0
	};
	
	public static final int[] BATTLE_START_TIME_SECOND =
	{
	        10, 5, 4, 3, 2, 1, 0
	};
	
	public static final int[] TELEPORT_TO_TOWN =
	{
	        40, 30, 20, 10, 5, 4, 3, 2, 1, 0
	};
	
	private static final List<Integer> BATTLE_END_TIME_SECOND = Arrays.asList(120, 60, 30, 10, 5, 4, 3, 2, 1, 0);
	private final OlympiadStadiumZone _zone;
	private AbstractOlympiadGame _game;
	private final int _id;
	private GameState _state = GameState.IDLE;
	private boolean _needAnnounce = false;
	private int _countDown = 0;
	
	private static enum GameState
	{
		BEGIN, TELEPORT_TO_ARENA, GAME_STARTED, BATTLE_COUNTDOWN_FIRST, BATTLE_COUNTDOWN_SECOND, BATTLE_STARTED, BATTLE_IN_PROGRESS, GAME_STOPPED, TELEPORT_TO_TOWN, CLEANUP, IDLE
	}
	
	public OlympiadGameTask(int id, OlympiadStadiumZone zone)
	{
		_id = id;
		_zone = zone;
		zone.registerTask(this);
	}
	
	public final boolean isRunning()
	{
		return _state != GameState.IDLE;
	}
	
	public final boolean isPrepareTeleport()
	{
		return _state == GameState.TELEPORT_TO_ARENA;
	}
	
	public final boolean isGameStarted()
	{
		return _state.ordinal() >= GameState.GAME_STARTED.ordinal() && _state.ordinal() <= GameState.CLEANUP.ordinal();
	}
	
	public final boolean isBattleStarted()
	{
		return _state == GameState.BATTLE_IN_PROGRESS;
	}
	
	public final boolean isBattleFinished()
	{
		return _state == GameState.TELEPORT_TO_TOWN;
	}
	
	public final boolean needAnnounce()
	{
		if (_needAnnounce)
		{
			_needAnnounce = false;
			return true;
		}
		return false;
	}
	
	public final int getId()
	{
		return _id;
	}
	
	public final OlympiadStadiumZone getZone()
	{
		return _zone;
	}
	
	public final AbstractOlympiadGame getGame()
	{
		return _game;
	}
	
	public final void attachGame(AbstractOlympiadGame game)
	{
		if (game != null && _state != GameState.IDLE)
		{
			_log.warn("Attempt to overwrite non-finished game in state " + _state);
			return;
		}
		
		_game = game;
		_game.setZone(_zone);
		_state = GameState.BEGIN;
		_needAnnounce = false;
		ThreadPoolManager.getInstance().execute(this);
	}
	
	public final void quickTeleport()
	{
		_countDown = 0;
	}
	
	@Override
	public final void run()
	{
		try
		{
			int delay = 1;
			switch (_state)
			{
				case BEGIN :
				{
					_state = GameState.TELEPORT_TO_ARENA;
					_countDown = Config.ALT_OLY_WAIT_TIME;
					break;
				}
				case TELEPORT_TO_ARENA :
				{
					if (_countDown > 0)
					{
						if (Config.ALLOW_OLY_FAST_INVITE && (_countDown == 120 || _countDown == 60 || _countDown == 30))
						{
							_game.confirmDlgInvite();
						}
						
						if (_countDown == 120 || _countDown == 60 || _countDown == 30 || _countDown == 15 || _countDown == 10 || _countDown == 5 || _countDown == 4 || _countDown == 3 || _countDown == 2 || _countDown == 1)
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
							sm.addNumber(_countDown);
							_game.broadcastPacket(sm);
						}
					}
					
					delay = getDelay(TELEPORT_TO_ARENA);
					if (_countDown <= 0)
					{
						_state = GameState.GAME_STARTED;
					}
					break;
				}
				case GAME_STARTED :
				{
					if (!startGame())
					{
						_state = GameState.GAME_STOPPED;
						break;
					}
					_state = GameState.BATTLE_COUNTDOWN_FIRST;
					_countDown = BATTLE_START_TIME_FIRST[0];
					delay = 5;
					break;
				}
				case BATTLE_COUNTDOWN_FIRST :
				{
					if (_countDown > 0)
					{
						if (_countDown == 55)
						{
							_game.healPlayers();
						}
						else
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S);
							sm.addNumber(_countDown);
							_zone.broadcastPacket(sm);
						}
					}
					
					delay = getDelay(BATTLE_START_TIME_FIRST);
					if (_countDown <= 0)
					{
						openDoors();
						
						_state = GameState.BATTLE_COUNTDOWN_SECOND;
						_countDown = BATTLE_START_TIME_SECOND[0];
						delay = getDelay(BATTLE_START_TIME_SECOND);
					}
					
					break;
				}
				case BATTLE_COUNTDOWN_SECOND :
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S);
						sm.addNumber(_countDown);
						_zone.broadcastPacket(sm);
					}
					
					delay = getDelay(BATTLE_START_TIME_SECOND);
					if (_countDown <= 0)
					{
						_state = GameState.BATTLE_STARTED;
					}
					
					break;
				}
				case BATTLE_STARTED :
				{
					_countDown = 0;
					_state = GameState.BATTLE_IN_PROGRESS;
					if (!startBattle())
					{
						_state = GameState.GAME_STOPPED;
					}
					
					break;
				}
				case BATTLE_IN_PROGRESS :
				{
					final int remaining = (int) (((Config.ALT_OLY_BATTLE * 60000) - _countDown) / 1000);
					if (BATTLE_END_TIME_SECOND.contains(remaining))
					{
						_game.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.GAME_ENDS_IN_S1_SECONDS).addInt(remaining));
					}
					
					_countDown += 1000;
					if (checkBattle() || _countDown > (Config.ALT_OLY_BATTLE * 60000))
					{
						_state = GameState.GAME_STOPPED;
					}
					
					break;
				}
				case GAME_STOPPED :
				{
					_state = GameState.TELEPORT_TO_TOWN;
					_countDown = Config.ALT_OLY_TELE_TO_TOWN;
					stopGame();
					delay = getDelay(TELEPORT_TO_TOWN);
					break;
				}
				case TELEPORT_TO_TOWN :
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
						sm.addNumber(_countDown);
						_game.broadcastPacket(sm);
					}
					
					delay = getDelay(TELEPORT_TO_TOWN);
					if (_countDown <= 0)
					{
						_state = GameState.CLEANUP;
						cleanupGame();
						delay = 1;
					}
					break;
				}
				case CLEANUP :
				{
					_state = GameState.IDLE;
					_game = null;
					return;
				}
			}
			ThreadPoolManager.getInstance().schedule(this, delay * 1000);
		}
		catch (final Exception e)
		{
			switch (_state)
			{
				case GAME_STOPPED :
				case TELEPORT_TO_TOWN :
				case CLEANUP :
				case IDLE :
				{
					_log.warn("Unable to return players back in town, exception: " + e.getMessage());
					_state = GameState.IDLE;
					_game = null;
					return;
				}
			}
			
			_log.warn("Exception in " + _state + ", trying to port players back: " + e.getMessage(), e);
			_state = GameState.GAME_STOPPED;
			ThreadPoolManager.getInstance().schedule(this, 1000);
		}
	}
	
	private final int getDelay(int[] times)
	{
		int time;
		for (int i = 0; i < times.length - 1; i++)
		{
			time = times[i];
			if (time >= _countDown)
			{
				continue;
			}
			
			final int delay = _countDown - time;
			_countDown = time;
			return delay;
		}
		_countDown = -1;
		return 1;
	}
	
	private final boolean startGame()
	{
		try
		{
			if (_game.checkDefaulted())
			{
				return false;
			}
			
			_zone.closeDoors();
			if (_game.needBuffers())
			{
				_zone.spawnBuffers();
			}

			if (!_game.portPlayersToArena(_zone.getSpawns()))
			{
				return false;
			}
			
			_game.removals();
			_needAnnounce = true;
			OlympiadGameManager.getInstance().startBattle();
			return true;
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		return false;
	}
	
	private final void openDoors()
	{
		try
		{
			_game.resetDamage();
			_zone.openDoors();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
	}
	
	private final boolean startBattle()
	{
		try
		{
			if (_game.needBuffers())
			{
				_zone.deleteBuffers();
			}
			
			if (_game.checkBattleStatus() && _game.makeCompetitionStart())
			{
				_game.broadcastOlympiadInfo(_zone);
				_zone.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.STARTS_THE_GAME));
				_zone.updateZoneStatusForCharactersInside(false);
				return true;
			}
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		return false;
	}
	
	private final boolean checkBattle()
	{
		if (_game._hasEnded)
		{
			return true;
		}
		
		try
		{
			return _game.haveWinner();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		return true;
	}
	
	private final void stopGame()
	{
		if (_game._hasEnded)
		{
			return;
		}
		
		_game._hasEnded = true;
		
		try
		{
			_zone.updateZoneStatusForCharactersInside(false);
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		
		try
		{
			_game.cleanEffects();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		
		try
		{
			_game.validateWinner(_zone);
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
	}
	
	private final void cleanupGame()
	{
		try
		{
			_game.playersStatusBack();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		
		try
		{
			_game.portPlayersBack();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}

		try
		{
			_game.clearPlayers();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
		
		try
		{
			_zone.closeDoors();
		}
		catch (final Exception e)
		{
			_log.warn(e.getMessage(), e);
		}
	}
}