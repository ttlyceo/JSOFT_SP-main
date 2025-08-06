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
package l2e.gameserver.model.zone.type;

import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.zone.AbstractZoneSettings;
import l2e.gameserver.model.zone.TaskZoneSettings;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.taskmanager.EffectTaskManager;

public class DamageZone extends ZoneType
{
	private int _damageHPPerSec;
	private int _damageMPPerSec;
	
	private int _castleId;
	private Castle _castle;

	private int _startTask;
	private int _reuseTask;

	public DamageZone(int id)
	{
		super(id);

		_damageHPPerSec = 200;
		_damageMPPerSec = 0;
		_startTask = 10;
		_reuseTask = 5000;
		_castleId = 0;
		_castle = null;

		setTargetType(InstanceType.Playable);
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new TaskZoneSettings();
		}
		setSettings(settings);
	}

	@Override
	public TaskZoneSettings getSettings()
	{
		return (TaskZoneSettings) super.getSettings();
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("dmgHPSec"))
		{
			_damageHPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("dmgMPSec"))
		{
			_damageMPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("initialDelay"))
		{
			_startTask = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("reuse"))
		{
			_reuseTask = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(Creature character)
	{
		if ((getSettings().getTask() == null) && ((_damageHPPerSec != 0) || (_damageMPPerSec != 0)))
		{
			final Player player = character.getActingPlayer();
			if (getCastle() != null)
			{
				if (!(getCastle().getSiege().getIsInProgress() && (player != null) && (player.getSiegeState() != 2)))
				{
					return;
				}
			}

			synchronized (this)
			{
				if (getSettings().getTask() == null)
				{
					getSettings().setTask(EffectTaskManager.getInstance().scheduleAtFixedRate(new ApplyDamage(this), _startTask, _reuseTask));
				}
			}
		}
	}

	@Override
	protected void onExit(Creature character)
	{
		if (_characterList.isEmpty() && (getSettings().getTask() != null))
		{
			stopTask();
		}
	}

	protected int getHPDamagePerSecond()
	{
		return _damageHPPerSec;
	}

	protected int getMPDamagePerSecond()
	{
		return _damageMPPerSec;
	}

	protected void stopTask()
	{
		if (getSettings().getTask() != null)
		{
			getSettings().getTask().cancel(false);
		}
	}

	@Override
	public void clearTask()
	{
		stopTask();
		super.clearTask();
	}
	
	protected Castle getCastle()
	{
		if ((_castleId > 0) && (_castle == null))
		{
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		}

		return _castle;
	}

	private final class ApplyDamage implements Runnable
	{
		private final DamageZone _dmgZone;
		private final Castle _castle;

		protected ApplyDamage(DamageZone zone)
		{
			_dmgZone = zone;
			_castle = zone.getCastle();
		}

		@Override
		public void run()
		{
			if (!isEnabled())
			{
				return;
			}

			boolean siege = false;

			if (_castle != null)
			{
				siege = _castle.getSiege().getIsInProgress();
				if (!siege)
				{
					_dmgZone.stopTask();
					return;
				}
			}

			for (final Creature temp : _dmgZone.getCharactersInside())
			{
				if ((temp != null) && !temp.isDead())
				{
					if (siege)
					{
						final Player player = temp.getActingPlayer();
						if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
						{
							continue;
						}
					}

					final double multiplier = 1 + (temp.calcStat(Stats.DAMAGE_ZONE_VULN, 0, null, null) / 100);

					if (getHPDamagePerSecond() != 0)
					{
						temp.reduceCurrentHp(_dmgZone.getHPDamagePerSecond() * multiplier, null, null);
					}
					if (getMPDamagePerSecond() != 0)
					{
						temp.reduceCurrentMp(_dmgZone.getMPDamagePerSecond() * multiplier);
					}
				}
			}
		}
	}
}