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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Announcements;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.RewardManager;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.type.OlympiadStadiumZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExOlympiadUserInfo;
import l2e.gameserver.network.serverpackets.ExReceiveOlympiadList;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;

public abstract class OlympiadGameNormal extends AbstractOlympiadGame
{
	protected int _damageP1 = 0;
	protected int _damageP2 = 0;
	protected Participant _playerOne;
	protected Participant _playerTwo;
	protected int _confirmInvites;
	protected int _currentInvites;
	protected boolean _isTraining;
	
	protected OlympiadGameNormal(int id, Participant[] opponents, boolean isTraining)
	{
		super(id);
		
		_playerOne = opponents[0];
		_playerTwo = opponents[1];
		_playerOne.getPlayer().setOlympiadGameId(id);
		_playerTwo.getPlayer().setOlympiadGameId(id);
		_currentInvites = 0;
		_confirmInvites = 2;
		_isTraining = isTraining;
	}

	protected static final Participant[] createListOfParticipants(Set<Integer> list)
	{
		if ((list == null) || list.isEmpty() || (list.size() < 2))
		{
			return null;
		}

		int playerOneObjectId = 0;
		int playerTwoObjectId = 0;
		Player playerOne = null;
		Player playerTwo = null;

		while (list.size() > 1)
		{
			int random = Rnd.get(list.size());
			Iterator<Integer> iter = list.iterator();
			while (iter.hasNext())
			{
				playerOneObjectId = iter.next();
				if (--random < 0)
				{
					iter.remove();
					break;
				}
			}
			
			playerOne = GameObjectsStorage.getPlayer(playerOneObjectId);
			if ((playerOne == null) || !playerOne.isOnline())
			{
				continue;
			}
			
			random = Rnd.get(list.size());
			iter = list.iterator();
			while (iter.hasNext())
			{
				playerTwoObjectId = iter.next();
				if (--random < 0)
				{
					iter.remove();
					break;
				}
			}
			
			playerTwo = GameObjectsStorage.getPlayer(playerTwoObjectId);
			if ((playerTwo == null) || !playerTwo.isOnline())
			{
				list.add(playerOneObjectId);
				continue;
			}
			
			final Participant[] result = new Participant[2];
			result[0] = new Participant(playerOne, 1);
			result[1] = new Participant(playerTwo, 2);
			
			return result;
		}
		return null;
	}

	@Override
	public final boolean containsParticipant(int playerId)
	{
		return ((_playerOne != null) && (_playerOne.getObjectId() == playerId)) || ((_playerTwo != null) && (_playerTwo.getObjectId() == playerId));
	}
	
	@Override
	public final boolean containsHwidParticipant(Player player)
	{
		return (player != null && ((_playerOne.getPlayer() != null) && (_playerOne.getPlayer().getHWID().equalsIgnoreCase(player.getHWID()))) || ((_playerTwo.getPlayer() != null) && (_playerTwo.getPlayer().getHWID().equalsIgnoreCase(player.getHWID()))));
	}

	@Override
	public final void sendOlympiadInfo(Creature player)
	{
		player.sendPacket(new ExOlympiadUserInfo(_playerOne));
		player.sendPacket(new ExOlympiadUserInfo(_playerTwo));
	}

	@Override
	public final void broadcastOlympiadInfo(OlympiadStadiumZone stadium)
	{
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerOne));
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerTwo));
	}

	@Override
	protected final void broadcastPacket(GameServerPacket packet)
	{
		if (_playerOne.updatePlayer())
		{
			_playerOne.getPlayer().sendPacket(packet);
		}

		if (_playerTwo.updatePlayer())
		{
			_playerTwo.getPlayer().sendPacket(packet);
		}
	}
	
	private class AnswerClick implements OnAnswerListener
	{
		private final Player _player;
		
		private AnswerClick(Player player)
		{
			_player = player;
		}
		
		@Override
		public void sayYes()
		{
			_currentInvites++;
			checkAnswers(_player);
		}
		
		@Override
		public void sayNo()
		{
		}
	}
	
	private void checkAnswers(Player player)
	{
		if (_currentInvites >= _confirmInvites)
		{
			final OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
			if ((task != null) && task.isPrepareTeleport())
			{
				task.quickTeleport();
			}
		}
	}
	
	@Override
	protected final void confirmDlgInvite()
	{
		_currentInvites = 0;
		if (_playerOne.getPlayer() != null)
		{
			_playerOne.getPlayer().sendConfirmDlg(new AnswerClick(_playerOne.getPlayer()), 10000, new ServerMessage("Olympiad.INVITE", _playerOne.getPlayer().getLang()).toString());
		}
		
		if (_playerTwo.getPlayer() != null)
		{
			_playerTwo.getPlayer().sendConfirmDlg(new AnswerClick(_playerTwo.getPlayer()), 10000, new ServerMessage("Olympiad.INVITE", _playerTwo.getPlayer().getLang()).toString());
		}
	}

	@Override
	protected final boolean portPlayersToArena(List<Location> spawns)
	{
		boolean result = true;
		try
		{
			setOlympiadGame(_playerOne, this);
			setOlympiadGame(_playerTwo, this);
			result &= portPlayerToArena(_playerOne, spawns.get(0), _stadiumID);
			result &= portPlayerToArena(_playerTwo, spawns.get(spawns.size() / 2), _stadiumID);
		}
		catch (final Exception e)
		{
			_log.warn("", e);
			return false;
		}
		return result;
	}

	@Override
	protected boolean needBuffers()
	{
		return true;
	}

	@Override
	protected final void removals()
	{
		if (_aborted)
		{
			return;
		}

		removals(_playerOne.getPlayer(), true);
		removals(_playerTwo.getPlayer(), true);
	}

	@Override
	protected final boolean makeCompetitionStart()
	{
		if (!super.makeCompetitionStart())
		{
			return false;
		}

		if ((_playerOne.getPlayer() == null) || (_playerTwo.getPlayer() == null))
		{
			return false;
		}

		_playerOne.getPlayer().setIsOlympiadStart(true);
		_playerOne.getPlayer().updateEffectIcons();
		_playerTwo.getPlayer().setIsOlympiadStart(true);
		_playerTwo.getPlayer().updateEffectIcons();
		return true;
	}
	
	@Override
	protected final void restorePlayers()
	{
		if ((_playerOne.getPlayer() != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected() && (_playerOne.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			restorePlayer(_playerOne.getPlayer());
		}
		
		if ((_playerTwo.getPlayer() != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected() && (_playerTwo.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			restorePlayer(_playerTwo.getPlayer());
		}
	}

	@Override
	protected final void cleanEffects()
	{
		if ((_playerOne.getPlayer() != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected() && (_playerOne.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			cleanEffects(_playerOne.getPlayer());
		}

		if ((_playerTwo.getPlayer() != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected() && (_playerTwo.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			cleanEffects(_playerTwo.getPlayer());
		}
	}

	@Override
	protected final void portPlayersBack()
	{
		if ((_playerOne.getPlayer() != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected())
		{
			portPlayerBack(_playerOne.getPlayer());
		}
		if ((_playerTwo.getPlayer() != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected())
		{
			portPlayerBack(_playerTwo.getPlayer());
		}
	}

	@Override
	protected final void playersStatusBack()
	{
		if ((_playerOne.getPlayer() != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected() && (_playerOne.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			playerStatusBack(_playerOne.getPlayer());
		}

		if ((_playerTwo.getPlayer() != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected() && (_playerTwo.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			playerStatusBack(_playerTwo.getPlayer());
		}
	}

	@Override
	protected final void clearPlayers()
	{
		_playerOne.setPlayer(null);
		_playerOne = null;
		_playerTwo.setPlayer(null);
		_playerTwo = null;
	}

	@Override
	public void healPlayers()
	{
		final Player playerOne = _playerOne.getPlayer();
		if (playerOne != null)
		{
			playerOne.setCurrentCp(playerOne.getMaxCp());
			playerOne.setCurrentHp(playerOne.getMaxHp());
			playerOne.setCurrentMp(playerOne.getMaxMp());
			if (Config.OLY_PRINT_CLASS_OPPONENT && _playerTwo.getPlayer() != null)
			{
				final ServerMessage msg = new ServerMessage("Olympiad.OPPONENT", playerOne.getLang());
				msg.add(Util.className(playerOne, _playerTwo.getPlayer().getClassId().getId()));
				playerOne.sendPacket(new ExShowScreenMessage(msg.toString(), 5000, ExShowScreenMessage.TOP_CENTER, true));
				playerOne.sendMessage(msg.toString());
			}
		}

		final Player playerTwo = _playerTwo.getPlayer();
		if (playerTwo != null)
		{
			playerTwo.setCurrentCp(playerTwo.getMaxCp());
			playerTwo.setCurrentHp(playerTwo.getMaxHp());
			playerTwo.setCurrentMp(playerTwo.getMaxMp());
			if (Config.OLY_PRINT_CLASS_OPPONENT && _playerOne.getPlayer() != null)
			{
				final ServerMessage msg = new ServerMessage("Olympiad.OPPONENT", playerTwo.getLang());
				msg.add(Util.className(playerTwo, _playerOne.getPlayer().getClassId().getId()));
				playerTwo.sendPacket(new ExShowScreenMessage(msg.toString(), 5000, ExShowScreenMessage.TOP_CENTER, true));
				playerTwo.sendMessage(msg.toString());
			}
		}
	}

	@Override
	protected final void handleDisconnect(Player player)
	{
		if (player.getObjectId() == _playerOne.getObjectId())
		{
			_playerOne.setDisconnected(true);
		}
		else if (player.getObjectId() == _playerTwo.getObjectId())
		{
			_playerTwo.setDisconnected(true);
		}
	}

	@Override
	protected final boolean checkBattleStatus()
	{
		if (_aborted)
		{
			return false;
		}

		if ((_playerOne.getPlayer() == null) || _playerOne.isDisconnected())
		{
			return false;
		}

		if ((_playerTwo.getPlayer() == null) || _playerTwo.isDisconnected())
		{
			return false;
		}

		return true;
	}

	@Override
	protected final boolean haveWinner()
	{
		if (!checkBattleStatus())
		{
			return true;
		}

		boolean playerOneLost = true;
		try
		{
			if (_playerOne.getPlayer().getOlympiadGameId() == _stadiumID)
			{
				playerOneLost = _playerOne.getPlayer().isDead();
			}
		}
		catch (final Exception e)
		{
			playerOneLost = true;
		}

		boolean playerTwoLost = true;
		try
		{
			if (_playerTwo.getPlayer().getOlympiadGameId() == _stadiumID)
			{
				playerTwoLost = _playerTwo.getPlayer().isDead();
			}
		}
		catch (final Exception e)
		{
			playerTwoLost = true;
		}

		return playerOneLost || playerTwoLost;
	}

	@Override
	protected void validateWinner(OlympiadStadiumZone stadium)
	{
		if (_aborted)
		{
			return;
		}
		
		ExReceiveOlympiadList.OlympiadResult result = null;

		boolean tie = false;
		int winside = 0;

		final List<OlympiadInfo> list1 = new ArrayList<>(1);
		final List<OlympiadInfo> list2 = new ArrayList<>(1);

		final boolean _pOneCrash = ((_playerOne.getPlayer() == null) || _playerOne.isDisconnected());
		final boolean _pTwoCrash = ((_playerTwo.getPlayer() == null) || _playerTwo.isDisconnected());

		final int playerOnePoints = _playerOne.getStats().getInteger(POINTS);
		final int playerTwoPoints = _playerTwo.getStats().getInteger(POINTS);
		int pointDiff = Math.min(playerOnePoints, playerTwoPoints) / getDivider();
		if (pointDiff <= 0)
		{
			pointDiff = 1;
		}
		else if (pointDiff > Config.ALT_OLY_MAX_POINTS)
		{
			pointDiff = Config.ALT_OLY_MAX_POINTS;
		}

		int points;
		SystemMessage sm;

		if (_playerOne.isDefaulted() || _playerTwo.isDefaulted())
		{
			try
			{
				if (_playerOne.isDefaulted())
				{
					try
					{
						if (_isTraining)
						{
							list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, 0, 0));
						}
						else
						{
							points = Math.min(playerOnePoints / 3, Config.ALT_OLY_MAX_POINTS);
							removePointsFromParticipant(_playerOne, points);
							list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - points, -points));
						}
						winside = 2;
					}
					catch (final Exception e)
					{
						_log.warn("Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				
				if (_playerTwo.isDefaulted())
				{
					try
					{
						if (_isTraining)
						{
							list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, 0, 0));
						}
						else
						{
							points = Math.min(playerTwoPoints / 3, Config.ALT_OLY_MAX_POINTS);
							removePointsFromParticipant(_playerTwo, points);
							list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - points, -points));
						}
						
						if (winside == 2)
						{
							tie = true;
						}
						else
						{
							winside = 1;
						}
					}
					catch (final Exception e)
					{
						_log.warn("Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				if (winside == 1)
				{
					result = new ExReceiveOlympiadList.OlympiadResult(tie, winside, list1, list2);
				}
				else
				{
					result = new ExReceiveOlympiadList.OlympiadResult(tie, winside, list2, list1);
				}
				stadium.broadcastPacket(result);
				return;
			}
			catch (final Exception e)
			{
				_log.warn("Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}
		}

		if (_pOneCrash || _pTwoCrash)
		{
			try
			{
				if (_pTwoCrash && !_pOneCrash)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
					sm.addString(_playerOne.getName());
					stadium.broadcastPacket(sm);

					if (_isTraining)
					{
						list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, 0, 0));
						list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, 0, 0));
					}
					else
					{
						_playerOne.updateStat(COMP_WON, 1);
						addPointsToParticipant(_playerOne, pointDiff);
						list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints + pointDiff, pointDiff));
						
						_playerTwo.updateStat(COMP_LOST, 1);
						removePointsFromParticipant(_playerTwo, pointDiff);
						list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - pointDiff, -pointDiff));
						rewardParticipant(_playerOne.getPlayer(), getReward());
					}
					winside = 1;
					
					if (Config.ALLOW_WINNER_ANNOUNCE)
					{
						final var msg = new ServerMessage("Olympiad.SINGLE_ANNOUNCE", true);
						msg.add(_playerOne.getName());
						msg.add(_playerTwo.getName());
						msg.add(_playerOne.getName());
						Announcements.getInstance().announceToAll(msg);
					}
				}
				else if (_pOneCrash && !_pTwoCrash)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
					sm.addString(_playerTwo.getName());
					stadium.broadcastPacket(sm);

					if (_isTraining)
					{
						list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, 0, 0));
						list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, 0, 0));
					}
					else
					{
						_playerTwo.updateStat(COMP_WON, 1);
						addPointsToParticipant(_playerTwo, pointDiff);
						list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints + pointDiff, pointDiff));
						
						_playerOne.updateStat(COMP_LOST, 1);
						removePointsFromParticipant(_playerOne, pointDiff);
						list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - pointDiff, -pointDiff));
						rewardParticipant(_playerTwo.getPlayer(), getReward());
					}
					winside = 2;
					
					if (Config.ALLOW_WINNER_ANNOUNCE)
					{
						final var msg = new ServerMessage("Olympiad.SINGLE_ANNOUNCE", true);
						msg.add(_playerOne.getName());
						msg.add(_playerTwo.getName());
						msg.add(_playerTwo.getName());
						Announcements.getInstance().announceToAll(msg);
					}
				}
				else if (_pOneCrash && _pTwoCrash)
				{
					stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE));
					if (_isTraining)
					{
						list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, 0, 0));
						list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, 0, 0));
					}
					else
					{
						_playerOne.updateStat(COMP_LOST, 1);
						removePointsFromParticipant(_playerOne, pointDiff);
						list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - pointDiff, -pointDiff));
						
						_playerTwo.updateStat(COMP_LOST, 1);
						removePointsFromParticipant(_playerTwo, pointDiff);
						list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - pointDiff, -pointDiff));
					}
					tie = true;
				}

				if (!_isTraining)
				{
					_playerOne.updateStat(COMP_DONE, 1);
					_playerTwo.updateStat(COMP_DONE, 1);
					_playerOne.updateStat(COMP_DONE_WEEK, 1);
					_playerTwo.updateStat(COMP_DONE_WEEK, 1);
					_playerOne.updateStat(getWeeklyMatchType(), 1);
					_playerTwo.updateStat(getWeeklyMatchType(), 1);
				}
				
				if (winside == 1)
				{
					result = new ExReceiveOlympiadList.OlympiadResult(tie, winside, list1, list2);
				}
				else
				{
					result = new ExReceiveOlympiadList.OlympiadResult(tie, winside, list2, list1);
				}
				stadium.broadcastPacket(result);
				return;
			}
			catch (final Exception e)
			{
				_log.warn("Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}
		}

		try
		{
			final long _fightTime = (System.currentTimeMillis() - _startTime);

			double playerOneHp = 0;
			if ((_playerOne.getPlayer() != null) && !_playerOne.getPlayer().isDead())
			{
				playerOneHp = _playerOne.getPlayer().getCurrentHp() + _playerOne.getPlayer().getCurrentCp();
				if (playerOneHp < 0.5)
				{
					playerOneHp = 0;
				}
			}

			double playerTwoHp = 0;
			if ((_playerTwo.getPlayer() != null) && !_playerTwo.getPlayer().isDead())
			{
				playerTwoHp = _playerTwo.getPlayer().getCurrentHp() + _playerTwo.getPlayer().getCurrentCp();
				if (playerTwoHp < 0.5)
				{
					playerTwoHp = 0;
				}
			}
			_playerOne.updatePlayer();
			_playerTwo.updatePlayer();

			if (((_playerOne.getPlayer() == null) || !_playerOne.getPlayer().isOnline()) && ((_playerTwo.getPlayer() == null) || !_playerTwo.getPlayer().isOnline()))
			{
				if (!_isTraining)
				{
					_playerOne.updateStat(COMP_DRAWN, 1);
					_playerTwo.updateStat(COMP_DRAWN, 1);
				}
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
				stadium.broadcastPacket(sm);
			}
			else if ((_playerTwo.getPlayer() == null) || !_playerTwo.getPlayer().isOnline() || ((playerTwoHp == 0) && (playerOneHp != 0)) || ((_damageP1 < _damageP2) && (playerTwoHp != 0) && (playerOneHp != 0)))
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
				sm.addString(_playerOne.getName());
				stadium.broadcastPacket(sm);
				if (_isTraining)
				{
					list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, 0, 0));
					list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, 0, 0));
					winside = 1;
				}
				else
				{
					_playerOne.updateStat(COMP_WON, 1);
					_playerTwo.updateStat(COMP_LOST, 1);
				
					if (_playerOne.getPlayer() != null)
					{
						_playerOne.getPlayer().getCounters().addAchivementInfo("olympiadWon", 0, -1, false, false, false);
						_playerOne.getPlayer().getCounters().addAchivementInfo("olympiadPoints", 0, pointDiff, false, false, false);
					}
					
					if (_playerTwo.getPlayer() != null)
					{
						_playerTwo.getPlayer().getCounters().addAchivementInfo("olympiadLose", 0, -1, false, false, false);
					}
					
					addPointsToParticipant(_playerOne, pointDiff);
					list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints + pointDiff, pointDiff));
					
					removePointsFromParticipant(_playerTwo, pointDiff);
					list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - pointDiff, -pointDiff));
					winside = 1;
					
					saveResults(_playerOne, _playerTwo, 1, _startTime, _fightTime, getType());
					rewardParticipant(_playerOne.getPlayer(), getReward());
					if (getLoseReward() != null)
					{
						rewardParticipant(_playerTwo.getPlayer(), getLoseReward());
					}
				}
				
				if (Config.ALLOW_WINNER_ANNOUNCE)
				{
					final var msg = new ServerMessage("Olympiad.SINGLE_ANNOUNCE", true);
					msg.add(_playerOne.getName());
					msg.add(_playerTwo.getName());
					msg.add(_playerOne.getName());
					Announcements.getInstance().announceToAll(msg);
				}
				
				if (_playerOne.getPlayer() != null && _playerTwo.getPlayer() != null && !_isTraining)
				{
					RewardManager.getInstance().checkOlyRewards(_playerOne.getPlayer(), _playerTwo.getPlayer());
				}
			}
			else if ((_playerOne.getPlayer() == null) || !_playerOne.getPlayer().isOnline() || ((playerOneHp == 0) && (playerTwoHp != 0)) || ((_damageP2 < _damageP1) && (playerOneHp != 0) && (playerTwoHp != 0)))
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
				sm.addString(_playerTwo.getName());
				stadium.broadcastPacket(sm);
				if (_isTraining)
				{
					list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, 0, 0));
					list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, 0, 0));
					winside = 2;
				}
				else
				{
					_playerTwo.updateStat(COMP_WON, 1);
					_playerOne.updateStat(COMP_LOST, 1);
					
					if (_playerTwo.getPlayer() != null)
					{
						_playerTwo.getPlayer().getCounters().addAchivementInfo("olympiadWon", 0, -1, false, false, false);
						_playerTwo.getPlayer().getCounters().addAchivementInfo("olympiadPoints", 0, pointDiff, false, false, false);
					}
					
					if (_playerOne.getPlayer() != null)
					{
						_playerOne.getPlayer().getCounters().addAchivementInfo("olympiadLose", 0, -1, false, false, false);
					}
					
					addPointsToParticipant(_playerTwo, pointDiff);
					list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints + pointDiff, pointDiff));
					
					removePointsFromParticipant(_playerOne, pointDiff);
					list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - pointDiff, -pointDiff));
					winside = 2;
					
					saveResults(_playerOne, _playerTwo, 2, _startTime, _fightTime, getType());
					rewardParticipant(_playerTwo.getPlayer(), getReward());
					if (getLoseReward() != null)
					{
						rewardParticipant(_playerOne.getPlayer(), getLoseReward());
					}
				}
				
				if (Config.ALLOW_WINNER_ANNOUNCE)
				{
					final var msg = new ServerMessage("Olympiad.SINGLE_ANNOUNCE", true);
					msg.add(_playerOne.getName());
					msg.add(_playerTwo.getName());
					msg.add(_playerTwo.getName());
					Announcements.getInstance().announceToAll(msg);
				}
				
				if (_playerOne.getPlayer() != null && _playerTwo.getPlayer() != null && !_isTraining)
				{
					RewardManager.getInstance().checkOlyRewards(_playerTwo.getPlayer(), _playerOne.getPlayer());
				}
			}
			else
			{
				if (_isTraining)
				{
					list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, 0, 0));
					list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, 0, 0));
				}
				else
				{
					saveResults(_playerOne, _playerTwo, 0, _startTime, _fightTime, getType());
					
					sm = SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
					stadium.broadcastPacket(sm);
					
					int value = Math.min(playerOnePoints / getDivider(), Config.ALT_OLY_MAX_POINTS);
					
					removePointsFromParticipant(_playerOne, value);
					list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - value, -value));
					
					value = Math.min(playerTwoPoints / getDivider(), Config.ALT_OLY_MAX_POINTS);
					removePointsFromParticipant(_playerTwo, value);
					list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - value, -value));
				}
				tie = true;
			}

			if (!_isTraining)
			{
				_playerOne.updateStat(COMP_DONE, 1);
				_playerTwo.updateStat(COMP_DONE, 1);
				_playerOne.updateStat(COMP_DONE_WEEK, 1);
				_playerTwo.updateStat(COMP_DONE_WEEK, 1);
				_playerOne.updateStat(getWeeklyMatchType(), 1);
				_playerTwo.updateStat(getWeeklyMatchType(), 1);
			}
			
			if (winside == 1)
			{
				result = new ExReceiveOlympiadList.OlympiadResult(tie, winside, list1, list2);
			}
			else
			{
				result = new ExReceiveOlympiadList.OlympiadResult(tie, winside, list2, list1);
			}
			stadium.broadcastPacket(result);
		}
		catch (final Exception e)
		{
			_log.warn("Exception on validateWinner(): " + e.getMessage(), e);
		}
	}

	@Override
	protected final void addDamage(Player player, int damage)
	{
		if ((_playerOne.getPlayer() == null) || (_playerTwo.getPlayer() == null))
		{
			return;
		}
		if (player == _playerOne.getPlayer())
		{
			_damageP1 += damage;
		}
		else if (player == _playerTwo.getPlayer())
		{
			_damageP2 += damage;
		}
	}

	@Override
	public final String[] getPlayerNames()
	{
		return new String[]
		{
		        _playerOne.getName(), _playerTwo.getName()
		};
	}

	@Override
	public boolean checkDefaulted()
	{
		SystemMessage reason;
		_playerOne.updatePlayer();
		_playerTwo.updatePlayer();

		reason = checkDefaulted(_playerOne.getPlayer());
		if (reason != null)
		{
			_playerOne.setDefaulted(true);
			if (_playerTwo.getPlayer() != null)
			{
				_playerTwo.getPlayer().sendPacket(reason);
			}
		}

		reason = checkDefaulted(_playerTwo.getPlayer());
		if (reason != null)
		{
			_playerTwo.setDefaulted(true);
			if (_playerOne.getPlayer() != null)
			{
				_playerOne.getPlayer().sendPacket(reason);
			}
		}

		return _playerOne.isDefaulted() || _playerTwo.isDefaulted();
	}

	@Override
	public final void resetDamage()
	{
		_damageP1 = 0;
		_damageP2 = 0;
	}

	protected static final void saveResults(Participant one, Participant two, int _winner, long _startTime, long _fightTime, CompetitionType type)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO olympiad_fights (charOneId, charTwoId, charOneClass, charTwoClass, winner, start, time, classed) values(?,?,?,?,?,?,?,?)");
			statement.setInt(1, one.getObjectId());
			statement.setInt(2, two.getObjectId());
			statement.setInt(3, one.getBaseClass());
			statement.setInt(4, two.getBaseClass());
			statement.setInt(5, _winner);
			statement.setLong(6, _startTime);
			statement.setLong(7, _fightTime);
			statement.setInt(8, (type == CompetitionType.CLASSED ? 1 : 0));
			statement.execute();
		}
		catch (final SQLException e)
		{
			_log.error("SQL exception while saving olympiad fight.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}