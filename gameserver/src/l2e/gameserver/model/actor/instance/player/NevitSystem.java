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
package l2e.gameserver.model.actor.instance.player;

import java.util.Calendar;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.player.impl.AdventTask;
import l2e.gameserver.model.actor.instance.player.impl.NevitEffectTask;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExNavitAdventPointInfo;
import l2e.gameserver.network.serverpackets.ExNevitAdventEffect;
import l2e.gameserver.network.serverpackets.ExNevitAdventTimeChange;

public class NevitSystem
{
	public static final int ADVENT_TIME = Config.NEVIT_ADVENT_TIME * 60;
	private static final int MAX_POINTS = Config.NEVIT_MAX_POINTS;
	private static final int BONUS_EFFECT_TIME = Config.NEVIT_BONUS_EFFECT_TIME;
	
	private final Player _player;
	private int _points = 0;
	private int _time;
	private int _percent;
	private boolean _active;
	
	public NevitSystem(Player player)
	{
		_player = player;
	}
	
	public void setPoints(int points, int time)
	{
		if (!Config.ALLOW_NEVIT_SYSTEM)
		{
			return;
		}
		_points = points;
		_active = false;
		_percent = getPercent(_points);
		
		final Calendar temp = Calendar.getInstance();
		temp.set(Calendar.HOUR_OF_DAY, 6);
		temp.set(Calendar.MINUTE, 30);
		temp.set(Calendar.SECOND, 0);
		temp.set(Calendar.MILLISECOND, 0);
		if (_player.getLastAccess() < temp.getTimeInMillis() && System.currentTimeMillis() > temp.getTimeInMillis())
		{
			_time = ADVENT_TIME;
		}
		else
		{
			_time = time;
		}
	}
	
	public void restartSystem()
	{
		if (!Config.ALLOW_NEVIT_SYSTEM)
		{
			return;
		}
		_time = ADVENT_TIME;
		_player.sendPacket(new ExNevitAdventTimeChange(_active, _time));
	}
	
	public void onEnterWorld()
	{
		if (!Config.ALLOW_NEVIT_SYSTEM)
		{
			return;
		}
		_player.sendPacket(new ExNavitAdventPointInfo(_points));
		_player.sendPacket(new ExNevitAdventTimeChange(_active, _time));
		startNevitEffect(_player.getVarInt("nevit", 0));
		if (_percent >= 45 && _percent < 50)
		{
			_player.sendPacket(SystemMessageId.YOU_ARE_STARTING_TO_FEEL_THE_EFFECTS_OF_NEVITS_ADVENT_BLESSING);
		}
		else if (_percent >= 50 && _percent < 75)
		{
			_player.sendPacket(SystemMessageId.YOU_ARE_FURTHER_INFUSED_WITH_THE_BLESSINGS_OF_NEVIT);
		}
		else if (_percent >= 75)
		{
			_player.sendPacket(SystemMessageId.NEVITS_ADVENT_BLESSING_SHINES_STRONGLY_FROM_ABOVE);
		}
	}
	
	public void startAdventTask()
	{
		if (!Config.ALLOW_NEVIT_SYSTEM)
		{
			return;
		}
		
		if (!_active)
		{
			_active = true;
			if (_time > 0)
			{
				_player.getPersonalTasks().addTask(new AdventTask(30000L));
			}
			_player.sendPacket(new ExNevitAdventTimeChange(_active, _time));
		}
	}
	
	private void startNevitEffect(int time)
	{
		if (!Config.ALLOW_NEVIT_SYSTEM)
		{
			return;
		}
		
		final var effectTime = getEffectTime();
		if (effectTime > 0)
		{
			_player.getPersonalTasks().removeTask(29, false);
			time += effectTime;
		}
		if (time > 0)
		{
			_player.setVar("nevit", time);
			_player.sendPacket(new ExNevitAdventEffect(time));
			_player.sendPacket(SystemMessageId.FROM_NOW_ON_ANGEL_NEVIT_ABIDE_WITH_YOU);
			_player.startAbnormalEffect(AbnormalEffect.NAVIT_ADVENT);
			_player.updateVitalityPoints(Config.VITALITY_NEVIT_UP_POINT, true, false);
			_player.getPersonalTasks().addTask(new NevitEffectTask(time * 1000L));
		}
	}
	
	public void stopAdventTask(boolean isPause)
	{
		if (!Config.ALLOW_NEVIT_SYSTEM)
		{
			return;
		}
		
		if (isPause)
		{
			_player.getPersonalTasks().removeTask(28, true);
		}
		_active = false;
		_player.sendPacket(new ExNevitAdventTimeChange(_active, _time));
	}
	
	public void saveTime()
	{
		if (!Config.ALLOW_NEVIT_SYSTEM)
		{
			return;
		}
		
		final int time = getEffectTime();
		if (time > 0)
		{
			_player.setVar("nevit", time);
		}
		else
		{
			_player.unsetVar("nevit");
		}
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	public int getTime()
	{
		return _time;
	}
	
	public void changeTime(int val)
	{
		_time -= val;
	}
	
	public int getPoints()
	{
		return _points;
	}
	
	public void addPoints(int val)
	{
		if (!Config.ALLOW_NEVIT_SYSTEM)
		{
			return;
		}
		
		_points += val;
		if (_points < 0)
		{
			_points = 0;
		}
		
		final int percent = getPercent(_points);
		if (_percent != percent)
		{
			_percent = percent;
			if (_percent == 45)
			{
				_player.sendPacket(SystemMessageId.YOU_ARE_STARTING_TO_FEEL_THE_EFFECTS_OF_NEVITS_ADVENT_BLESSING);
			}
			else if (_percent == 50)
			{
				_player.sendPacket(SystemMessageId.YOU_ARE_FURTHER_INFUSED_WITH_THE_BLESSINGS_OF_NEVIT);
			}
			else if (_percent == 75)
			{
				_player.sendPacket(SystemMessageId.NEVITS_ADVENT_BLESSING_SHINES_STRONGLY_FROM_ABOVE);
			}
		}
		if (_points > MAX_POINTS)
		{
			_percent = 0;
			_points = 0;
			if (!isBlessingActive())
			{
				startNevitEffect(BONUS_EFFECT_TIME);
			}
		}
		_player.sendPacket(new ExNavitAdventPointInfo(_points));
	}
	
	public int getPercent(int points)
	{
		return (int) (100.0D / MAX_POINTS * points);
	}
	
	public void setTime(int time)
	{
		_time = time;
	}
	
	public boolean isBlessingActive()
	{
		return getEffectTime() > 0;
	}
	
	private int getEffectTime()
	{
		final var delay = _player.getPersonalTasks().isTaskDelay(29);
		return (int) (delay > 0 && delay > System.currentTimeMillis() ? (int) Math.max(0, (delay - System.currentTimeMillis()) / 1000L) : delay);
	}
}