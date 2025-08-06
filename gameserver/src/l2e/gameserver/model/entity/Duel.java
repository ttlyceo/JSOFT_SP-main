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
package l2e.gameserver.model.entity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.ReflectionParser;
import l2e.gameserver.instancemanager.DuelManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ActionFail;
import l2e.gameserver.network.serverpackets.ExDuelEnd;
import l2e.gameserver.network.serverpackets.ExDuelReady;
import l2e.gameserver.network.serverpackets.ExDuelStart;
import l2e.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Duel
{
	protected static final Logger _log = LoggerFactory.getLogger(Duel.class);

	public static final int DUELSTATE_NODUEL = 0;
	public static final int DUELSTATE_DUELLING = 1;
	public static final int DUELSTATE_DEAD = 2;
	public static final int DUELSTATE_WINNER = 3;
	public static final int DUELSTATE_INTERRUPTED = 4;
	public static final int DUELSTATE_PREPARING = 5;

	private final int _duelId;
	private Player _playerA;
	private Player _playerB;
	private final boolean _partyDuel;
	private final Calendar _duelEndTime;
	private int _surrenderRequest = 0;
	private int _countdown = 4;
	private boolean _finished = false;
	private Reflection _duelRef;

	private List<PlayerCondition> _playerConditions = new CopyOnWriteArrayList<>();

	public static enum DuelResultEnum
	{
		Continue, Team1Win, Team2Win, Team1Surrender, Team2Surrender, Canceled, Timeout
	}

	public Duel(Player playerA, Player playerB, int partyDuel, int duelId)
	{
		_duelId = duelId;
		_playerA = playerA;
		_playerB = playerB;
		_partyDuel = partyDuel == 1 ? true : false;
		prepareStatus(_playerA, _playerB, _partyDuel);
		_duelEndTime = Calendar.getInstance();
		if (_partyDuel)
		{
			_duelEndTime.add(Calendar.SECOND, 300);
		}
		else
		{
			_duelEndTime.add(Calendar.SECOND, 120);
		}

		setFinished(false);

		if (_partyDuel)
		{
			_countdown++;
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
		}
		ThreadPoolManager.getInstance().schedule(new ScheduleStartDuelTask(this), 3000);
	}

	public static class PlayerCondition
	{
		private Player _player;
		private double _hp;
		private double _mp;
		private double _cp;
		private boolean _paDuel;
		private int _x, _y, _z;
		private long _effectRecordTime;
		private List<Effect> _effects;

		public PlayerCondition(Player player, boolean partyDuel)
		{
			if (player == null)
			{
				return;
			}
			_player = player;
			_hp = _player.getCurrentHp();
			_mp = _player.getCurrentMp();
			_cp = _player.getCurrentCp();
			_paDuel = partyDuel;

			final Effect[] effectList = player.getAllEffects();
			_effects = new ArrayList<>(effectList.length);
			_effectRecordTime = System.currentTimeMillis();
			for (final Effect effect : effectList)
			{
				final Effect ef = effect.getEffectTemplate().getEffect(new Env(effect.getEffector(), effect.getEffected(), effect.getSkill()));
				ef.setCount(effect.getTickCount());
				ef.setAbnormalTime(effect.getAbnormalTime());
				ef.setFirstTime(effect.getTime());
				_effects.add(ef);
			}
			
			if (_paDuel)
			{
				_x = _player.getX();
				_y = _player.getY();
				_z = _player.getZ();
			}
		}

		public void restoreCondition()
		{
			if (_player == null)
			{
				return;
			}
			
			_player.getEffectList().stopAllEffects();
			if (_effects != null && !_effects.isEmpty())
			{
				for (final Effect e : _effects)
				{
					e.setCount(e.getTickCount() + (int) ((System.currentTimeMillis() - _effectRecordTime) / 1000));
					e.scheduleEffect(true);
				}
			}
			
			_player.setCurrentHp(_hp);
			_player.setCurrentMp(_mp);
			_player.setCurrentCp(_cp);

			if (_paDuel)
			{
				teleportBack();
			}
		}

		public void teleportBack()
		{
			if (_paDuel)
			{
				_player.teleToLocation(_x, _y, _z, true, ReflectionManager.DEFAULT);
			}
		}

		public Player getPlayer()
		{
			return _player;
		}
	}

	public class ScheduleDuelTask implements Runnable
	{
		private final Duel _duel;

		public ScheduleDuelTask(Duel duel)
		{
			_duel = duel;
		}

		@Override
		public void run()
		{
			try
			{
				final DuelResultEnum status = _duel.checkEndDuelCondition();

				if (status == DuelResultEnum.Canceled)
				{
					setFinished(true);
					_duel.endDuel(status);
				}
				else if (status != DuelResultEnum.Continue)
				{
					setFinished(true);
					playKneelAnimation();
					ThreadPoolManager.getInstance().schedule(new ScheduleEndDuelTask(_duel, status), 5000);
				}
				else
				{
					ThreadPoolManager.getInstance().schedule(this, 1000);
				}
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}

	private class ScheduleStartDuelTask implements Runnable
	{
		private final Duel _duel;

		public ScheduleStartDuelTask(Duel duel)
		{
			_duel = duel;
		}

		@Override
		public void run()
		{
			try
			{
				final int count = _duel.countdown();
				if (count == 4)
				{
					_duel.teleportPlayers();
					ThreadPoolManager.getInstance().schedule(this, 20000);
				}
				else if (count > 0)
				{
					ThreadPoolManager.getInstance().schedule(this, 1000);
				}
				else
				{
					_duel.startDuel();
				}
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}

	private class ScheduleEndDuelTask implements Runnable
	{
		private final Duel _duel;
		private final DuelResultEnum _result;

		public ScheduleEndDuelTask(Duel duel, DuelResultEnum result)
		{
			_duel = duel;
			_result = result;
		}

		@Override
		public void run()
		{
			try
			{
				_duel.endDuel(_result);
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}

	private void stopFighting()
	{
		final ActionFail af = ActionFail.STATIC_PACKET;
		if (_partyDuel)
		{
			for (final Player temp : _playerA.getParty().getMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(CtrlIntention.ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
			}
			for (final Player temp : _playerB.getParty().getMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(CtrlIntention.ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
			}
		}
		else
		{
			_playerA.abortCast();
			_playerB.abortCast();
			_playerA.getAI().setIntention(CtrlIntention.ACTIVE);
			_playerA.setTarget(null);
			_playerB.getAI().setIntention(CtrlIntention.ACTIVE);
			_playerB.setTarget(null);
			_playerA.sendPacket(af);
			_playerB.sendPacket(af);
		}
	}

	public boolean isDuelistInPvp(boolean sendMessage)
	{
		if (_partyDuel)
		{
			return false;
		}
		else if (_playerA.getPvpFlag() != 0 || _playerB.getPvpFlag() != 0)
		{
			if (sendMessage)
			{
				final String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
				_playerA.sendMessage(engagedInPvP);
				_playerB.sendMessage(engagedInPvP);
			}
			return true;
		}
		return false;
	}

	public void startDuel()
	{
		savePlayerConditions();

		if (_playerA == null || _playerB == null || _playerA.isInDuel() || _playerB.isInDuel())
		{
			_playerConditions.clear();
			_playerConditions = null;
			DuelManager.getInstance().removeDuel(this);
			return;
		}

		if (_partyDuel)
		{
			for (final Player temp : _playerA.getParty().getMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(1);
				temp.broadcastUserInfo(true);
				broadcastToTeam2(new ExDuelUpdateUserInfo(temp));
			}
			for (final Player temp : _playerB.getParty().getMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(2);
				temp.broadcastUserInfo(true);
				broadcastToTeam1(new ExDuelUpdateUserInfo(temp));
			}

			if (_partyDuel)
			{
				for (final DoorInstance door : ReflectionManager.getInstance().getReflection(_duelRef.getId()).getDoors())
				{
					if ((door != null) && door.isOpened())
					{
						door.closeMe();
					}
				}
			}
			
			final ExDuelReady ready = new ExDuelReady(1);
			final ExDuelStart start = new ExDuelStart(1);

			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);
		}
		else
		{
			_playerA.setIsInDuel(_duelId);
			_playerA.setTeam(1);
			_playerB.setIsInDuel(_duelId);
			_playerB.setTeam(2);
			
			final ExDuelReady ready = new ExDuelReady(0);
			final ExDuelStart start = new ExDuelStart(0);

			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);

			broadcastToTeam1(new ExDuelUpdateUserInfo(_playerB));
			broadcastToTeam2(new ExDuelUpdateUserInfo(_playerA));

			_playerA.broadcastUserInfo(true);
			_playerB.broadcastUserInfo(true);
		}

		final PlaySound ps = new PlaySound(1, "B04_S01", 0, 0, 0, 0, 0);
		broadcastToTeam1(ps);
		broadcastToTeam2(ps);

		ThreadPoolManager.getInstance().schedule(new ScheduleDuelTask(this), 1000);
	}

	public void savePlayerConditions()
	{
		if (_partyDuel)
		{
			for (final Player temp : _playerA.getParty().getMembers())
			{
				_playerConditions.add(new PlayerCondition(temp, _partyDuel));
			}
			for (final Player temp : _playerB.getParty().getMembers())
			{
				_playerConditions.add(new PlayerCondition(temp, _partyDuel));
			}
		}
		else
		{
			_playerConditions.add(new PlayerCondition(_playerA, _partyDuel));
			_playerConditions.add(new PlayerCondition(_playerB, _partyDuel));
		}
	}

	public void restorePlayerConditions(boolean abnormalDuelEnd)
	{
		if (_partyDuel)
		{
			for (final Player temp : _playerA.getParty().getMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(0);
				temp.broadcastUserInfo(true);
			}
			for (final Player temp : _playerB.getParty().getMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(0);
				temp.broadcastUserInfo(true);
			}
		}
		else
		{
			_playerA.setIsInDuel(0);
			_playerA.setTeam(0);
			_playerA.broadcastUserInfo(true);
			_playerB.setIsInDuel(0);
			_playerB.setTeam(0);
			_playerB.broadcastUserInfo(true);
		}

		if (abnormalDuelEnd)
		{
			return;
		}

		for (final PlayerCondition e : _playerConditions)
		{
			e.restoreCondition();
		}
	}

	public int getId()
	{
		return _duelId;
	}

	public int getRemainingTime()
	{
		return (int) (_duelEndTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
	}

	public Player getPlayerA()
	{
		return _playerA;
	}

	public Player getPlayerB()
	{
		return _playerB;
	}

	public boolean isPartyDuel()
	{
		return _partyDuel;
	}
	
	public void setFinished(boolean mode)
	{
		_finished = mode;
	}

	public boolean getFinished()
	{
		return _finished;
	}

	public void teleportPlayers()
	{
		if (!_partyDuel)
		{
			return;
		}

		final ReflectionTemplate template = ReflectionParser.getInstance().getReflectionId(1);
		if (template != null)
		{
			_duelRef = ReflectionManager.getInstance().createDynamicReflection(template);
			if (_duelRef != null)
			{
				int i = 0;
				for (final Player player : _playerA.getParty().getMembers())
				{
					final Location loc = template.getTeleportCoords().get(i++);
					player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), 0, 0, true, true, _duelRef);
				}
				
				i = 9;
				for (final Player player : _playerB.getParty().getMembers())
				{
					final Location loc = template.getTeleportCoords().get(i++);
					player.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), 0, 0, true, true, _duelRef);
				}
			}
		}
	}

	public void broadcastToTeam1(GameServerPacket packet)
	{
		if (_playerA == null)
		{
			return;
		}

		if (_partyDuel && _playerA.getParty() != null)
		{
			for (final Player temp : _playerA.getParty().getMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerA.sendPacket(packet);
		}
	}

	public void broadcastToTeam2(GameServerPacket packet)
	{
		if (_playerB == null)
		{
			return;
		}

		if (_partyDuel && _playerB.getParty() != null)
		{
			for (final Player temp : _playerB.getParty().getMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerB.sendPacket(packet);
		}
	}

	public Player getWinner()
	{
		if (!getFinished() || _playerA == null || _playerB == null)
		{
			return null;
		}
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		return null;
	}

	public Player getLooser()
	{
		if (!getFinished() || _playerA == null || _playerB == null)
		{
			return null;
		}
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		return null;
	}

	public void playKneelAnimation()
	{
		final Player looser = getLooser();

		if (looser == null)
		{
			return;
		}

		if (_partyDuel && looser.getParty() != null)
		{
			for (final Player temp : looser.getParty().getMembers())
			{
				temp.broadcastPacket(new SocialAction(temp.getObjectId(), 7));
			}
		}
		else
		{
			looser.broadcastPacket(new SocialAction(looser.getObjectId(), 7));
		}
	}

	public int countdown()
	{
		_countdown--;

		if (_countdown > 3)
		{
			return _countdown;
		}

		SystemMessage sm = null;
		if (_countdown > 0)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS);
			sm.addNumber(_countdown);
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN);
		}

		broadcastToTeam1(sm);
		broadcastToTeam2(sm);

		return _countdown;
	}

	public void endDuel(DuelResultEnum result)
	{
		if (_playerA == null || _playerB == null)
		{
			_playerConditions.clear();
			_playerConditions = null;
			DuelManager.getInstance().removeDuel(this);
			return;
		}

		SystemMessage sm = null;
		switch (result)
		{
			case Team1Win :
			case Team2Surrender :
				restorePlayerConditions(false);
				
				if (_partyDuel)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_PARTY_HAS_WON_THE_DUEL);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_DUEL);
				}
				sm.addString(_playerA.getName(null));
				_playerA.getCounters().addAchivementInfo("duelsWon", 0, -1, false, _partyDuel, false);
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Team1Surrender :
			case Team2Win :
				restorePlayerConditions(false);
				
				if (_partyDuel)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_PARTY_HAS_WON_THE_DUEL);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_DUEL);
				}
				sm.addString(_playerB.getName(null));
				_playerB.getCounters().addAchivementInfo("duelsWon", 0, -1, false, _partyDuel, false);
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Canceled :
				stopFighting();
				restorePlayerConditions(true);
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Timeout :
				stopFighting();
				restorePlayerConditions(false);
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
		}
		
		ExDuelEnd duelEnd = null;
		if (_partyDuel)
		{
			duelEnd = new ExDuelEnd(1);
		}
		else
		{
			duelEnd = new ExDuelEnd(0);
		}

		broadcastToTeam1(duelEnd);
		broadcastToTeam2(duelEnd);
		
		_playerConditions.clear();
		_playerConditions = null;
		if (_partyDuel && _duelRef != null)
		{
			_duelRef.collapse();
		}
		DuelManager.getInstance().removeDuel(this);
	}

	public DuelResultEnum checkEndDuelCondition()
	{
		if (_playerA == null || _playerB == null)
		{
			return DuelResultEnum.Canceled;
		}

		if (_surrenderRequest != 0)
		{
			if (_surrenderRequest == 1)
			{
				return DuelResultEnum.Team1Surrender;
			}
			return DuelResultEnum.Team2Surrender;
		}
		else if (getRemainingTime() <= 0)
		{
			return DuelResultEnum.Timeout;
		}
		else if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			stopFighting();
			return DuelResultEnum.Team1Win;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			stopFighting();
			return DuelResultEnum.Team2Win;
		}
		else if (!_partyDuel)
		{
			if (_playerA.getDuelState() == DUELSTATE_INTERRUPTED || _playerB.getDuelState() == DUELSTATE_INTERRUPTED)
			{
				return DuelResultEnum.Canceled;
			}

			if (!_playerA.isInsideRadius(_playerB, 1600, false, false))
			{
				return DuelResultEnum.Canceled;
			}

			if (isDuelistInPvp(true))
			{
				return DuelResultEnum.Canceled;
			}

			if (_playerA.isInsideZone(ZoneId.PEACE) || _playerB.isInsideZone(ZoneId.PEACE) || _playerA.isInsideZone(ZoneId.SIEGE) || _playerB.isInsideZone(ZoneId.SIEGE) || _playerA.isInsideZone(ZoneId.PVP) || _playerB.isInsideZone(ZoneId.PVP))
			{
				return DuelResultEnum.Canceled;
			}
		}
		return DuelResultEnum.Continue;
	}

	public void doSurrender(Player player)
	{
		if (_surrenderRequest != 0)
		{
			return;
		}

		stopFighting();

		if (_partyDuel)
		{
			if (_playerA.getParty().getMembers().contains(player))
			{
				_surrenderRequest = 1;
				for (final Player temp : _playerA.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				for (final Player temp : _playerB.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
			else if (_playerB.getParty().getMembers().contains(player))
			{
				_surrenderRequest = 2;
				for (final Player temp : _playerB.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				for (final Player temp : _playerA.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}

			}
		}
		else
		{
			if (player == _playerA)
			{
				_surrenderRequest = 1;
				_playerA.setDuelState(DUELSTATE_DEAD);
				_playerB.setDuelState(DUELSTATE_WINNER);
			}
			else if (player == _playerB)
			{
				_surrenderRequest = 2;
				_playerB.setDuelState(DUELSTATE_DEAD);
				_playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}

	public void onPlayerDefeat(Player player)
	{
		player.setDuelState(DUELSTATE_DEAD);

		if (_partyDuel)
		{
			boolean teamdefeated = true;
			for (final Player temp : player.getParty().getMembers())
			{
				if (temp.getDuelState() == DUELSTATE_DUELLING)
				{
					teamdefeated = false;
					break;
				}
			}

			if (teamdefeated)
			{
				Player winner = _playerA;
				if (_playerA.getParty().getMembers().contains(player))
				{
					winner = _playerB;
				}

				for (final Player temp : winner.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
		}
		else
		{
			if (player != _playerA && player != _playerB)
			{
				_log.warn("Error in onPlayerDefeat(): player is not part of this 1vs1 duel");
			}

			if (_playerA == player)
			{
				_playerB.setDuelState(DUELSTATE_WINNER);
			}
			else
			{
				_playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}

	public void onRemoveFromParty(Player player)
	{
		if (!_partyDuel)
		{
			return;
		}

		if (player == _playerA || player == _playerB)
		{
			for (final PlayerCondition e : _playerConditions)
			{
				e.teleportBack();
				e.getPlayer().setIsInDuel(0);
			}
			_playerA = null;
			_playerB = null;
		}
		else
		{
			for (final PlayerCondition e : _playerConditions)
			{
				if (e.getPlayer() == player)
				{
					e.teleportBack();
					_playerConditions.remove(e);
					break;
				}
			}
			player.setIsInDuel(0);
		}
	}
	
	private void prepareStatus(Player playerA, Player playerB, boolean isPartyDuel)
	{
		if (playerA == null || playerB == null)
		{
			return;
		}
		
		if (isPartyDuel)
		{
			for (final Player temp : playerA.getParty().getMembers())
			{
				temp.setDuelState(DUELSTATE_PREPARING);
			}
			for (final Player temp : playerB.getParty().getMembers())
			{
				temp.setDuelState(DUELSTATE_PREPARING);
			}
		}
		else
		{
			playerA.setDuelState(DUELSTATE_PREPARING);
			playerB.setDuelState(DUELSTATE_PREPARING);
		}
	}
}