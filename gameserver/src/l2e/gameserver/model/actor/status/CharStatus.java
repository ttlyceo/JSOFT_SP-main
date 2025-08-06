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
package l2e.gameserver.model.actor.status;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.taskmanager.RegenTaskManager;

public class CharStatus
{
	protected static final Logger _log = LoggerFactory.getLogger(CharStatus.class);
	
	private final Creature _activeChar;
	
	private double _currentHp = 0;
	private double _currentMp = 0;
	
	private Set<Creature> _statusListener;
	private Future<?> _regTask;
	
	protected byte _flagsRegenActive = 0;
	
	protected static final byte REGEN_FLAG_CP = 4;
	private static final byte REGEN_FLAG_HP = 1;
	private static final byte REGEN_FLAG_MP = 2;
	
	public CharStatus(Creature activeChar)
	{
		_activeChar = activeChar;
	}
	
	public final void addStatusListener(Creature object)
	{
		if (object == getActiveChar())
		{
			return;
		}
		
		getStatusListener().add(object);
	}
	
	public final void removeStatusListener(Creature object)
	{
		getStatusListener().remove(object);
	}
	
	public final Set<Creature> getStatusListener()
	{
		if (_statusListener == null)
		{
			_statusListener = ConcurrentHashMap.newKeySet();
		}
		return _statusListener;
	}
	
	public void reduceCp(int value)
	{
	}
	
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false, true);
	}
	
	public void reduceHp(double value, Creature attacker, boolean isHpConsumption)
	{
		reduceHp(value, attacker, true, false, isHpConsumption, true);
	}
	
	public void reduceHp(double value, Creature attacker, boolean isHpConsumption, boolean broadCastPacket)
	{
		reduceHp(value, attacker, true, false, isHpConsumption, broadCastPacket);
	}
	
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean broadCastPacket)
	{
		if (getActiveChar().isDead())
		{
			return;
		}
		
		if (getActiveChar().isInvul() && !(isDOT || isHPConsumption))
		{
			return;
		}
		
		if (attacker != null)
		{
			final Player attackerPlayer = attacker.getActingPlayer();
			if ((attackerPlayer != null) && attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
			{
				return;
			}
		}
		
		if (!isDOT && !isHPConsumption)
		{
			getActiveChar().stopEffectsOnDamage(awake);
		}
		
		if (value > 0)
		{
			setCurrentHp(Math.max(getCurrentHp() - value, getActiveChar().isUndying() ? 1 : 0), broadCastPacket);
		}
		
		if ((getActiveChar().getCurrentHp() < 0.5) && getActiveChar().isMortal())
		{
			getActiveChar().abortAttack();
			getActiveChar().abortCast();
			getActiveChar().stopHpMpRegeneration();
			getActiveChar().doDie(attacker);
		}
	}
	
	public void reduceMp(double value)
	{
		setCurrentMp(Math.max(getCurrentMp() - value, 0));
	}
	
	public void reduceMp(double value, boolean broadCastPacket)
	{
		setCurrentMp(Math.max(getCurrentMp() - value, 0), broadCastPacket);
	}
	
	public final synchronized void startHpMpRegeneration()
	{
		if ((_regTask == null) && !getActiveChar().isDead())
		{
			if (Config.DEBUG && getActiveChar().isPlayer())
			{
				_log.info("HP/MP regen start!");
			}
			final long period = Formulas.getRegeneratePeriod(getActiveChar());
			_regTask = RegenTaskManager.getInstance().scheduleAtFixedRate(this::doRegeneration, period, period);
		}
	}
	
	public final synchronized void stopHpMpRegeneration()
	{
		if (_regTask != null)
		{
			if (Config.DEBUG && getActiveChar().isPlayer())
			{
				_log.info("HP/MP regen stop!");
			}
			_regTask.cancel(false);
			_regTask = null;
			_flagsRegenActive = 0;
		}
	}
	
	public double getCurrentCp()
	{
		return 0;
	}
	
	public void setCurrentCp(double newCp)
	{
	}
	
	public final double getCurrentHp()
	{
		return _currentHp;
	}
	
	public final void setCurrentHp(double newHp)
	{
		setCurrentHp(newHp, true, false);
	}
	
	public final void setCurrentHp(double newHp, boolean broadcastPacket)
	{
		setCurrentHp(newHp, broadcastPacket, false);
	}
	
	public boolean setCurrentHp(double newHp, boolean broadcastPacket, boolean isDead)
	{
		final double currentHp = getCurrentHp();
		final double maxHp = getActiveChar().getStat().getMaxHp();
		
		synchronized (this)
		{
			if (getActiveChar().isDead())
			{
				return false;
			}
			
			if (newHp >= maxHp)
			{
				_currentHp = maxHp;
				_flagsRegenActive &= ~REGEN_FLAG_HP;
				
				if (!getActiveChar().getToggleList().hasEffects() && _flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				_currentHp = newHp;
				_flagsRegenActive |= REGEN_FLAG_HP;
				if (!isDead)
				{
					startHpMpRegeneration();
				}
			}
		}
		
		final boolean hpWasChanged = currentHp != _currentHp;
		
		if (hpWasChanged && broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
		getActiveChar().getListeners().onChangeCurrentHp(currentHp, newHp);
		return hpWasChanged;
	}
	
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		boolean hpOrMpWasChanged = setCurrentHp(newHp, false, false);
		hpOrMpWasChanged |= setCurrentMp(newMp, false);
		if (hpOrMpWasChanged)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	public final double getCurrentMp()
	{
		return _currentMp;
	}
	
	public final void setCurrentMp(double newMp)
	{
		setCurrentMp(newMp, true);
	}
	
	public final boolean setCurrentMp(double newMp, boolean broadcastPacket)
	{
		final double currentMp = getCurrentMp();
		final double maxMp = getActiveChar().getStat().getMaxMp();
		
		synchronized (this)
		{
			if (getActiveChar().isDead())
			{
				return false;
			}
			
			if (newMp >= maxMp)
			{
				_currentMp = maxMp;
				_flagsRegenActive &= ~REGEN_FLAG_MP;
				
				if (!getActiveChar().getToggleList().hasEffects() && _flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				_currentMp = newMp;
				_flagsRegenActive |= REGEN_FLAG_MP;
				startHpMpRegeneration();
			}
		}
		
		final boolean mpWasChanged = currentMp != _currentMp;
		
		if (mpWasChanged && broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
		getActiveChar().getListeners().onChangeCurrentHp(currentMp, newMp);
		return mpWasChanged;
	}
	
	protected void doRegeneration()
	{
		if (!getActiveChar().isDead() && ((_currentHp < getActiveChar().getMaxRecoverableHp()) || (_currentMp < getActiveChar().getMaxRecoverableMp())))
		{
			final double newHp = getCurrentHp() + Formulas.calcHpRegen(getActiveChar());
			final double newMp = getCurrentMp() + Formulas.calcMpRegen(getActiveChar());
			setCurrentHpMp(newHp, newMp);
		}
		else
		{
			stopHpMpRegeneration();
		}
	}
	
	public Creature getActiveChar()
	{
		return _activeChar;
	}
}