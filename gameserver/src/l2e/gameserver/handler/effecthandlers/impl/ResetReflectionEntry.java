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
package l2e.gameserver.handler.effecthandlers.impl;

import java.util.Calendar;

import l2e.gameserver.data.parser.ReflectionParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

/**
 * Create by LordWinter 14.02.2020
 */
public class ResetReflectionEntry extends Effect
{
	private final int _sharedReuseGroup;
	private final int _attempts;
	
	public ResetReflectionEntry(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_sharedReuseGroup = template.getParameters().getInteger("sharedReuseGroup");
		_attempts = template.getParameters().getInteger("attempts");
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public boolean onStart()
	{
		if ((getEffected() == null) || !getEffected().isPlayer())
		{
			return false;
		}
		
		final var player = getEffected().getActingPlayer();
		for (final int i : ReflectionParser.getInstance().getSharedReuseInstanceIdsByGroup(_sharedReuseGroup))
		{
			if (System.currentTimeMillis() < ReflectionManager.getInstance().getReflectionTime(player, i))
			{
				ReflectionManager.getInstance().deleteReflectionTime(player, i);
			}
		}
		
		final var calendar = Calendar.getInstance();
		calendar.set(Calendar.AM_PM, 0);
		calendar.set(Calendar.HOUR, 6);
		calendar.set(Calendar.MINUTE, 30);
		calendar.set(Calendar.SECOND, 0);
		if (calendar.getTimeInMillis() <= System.currentTimeMillis())
		{
			calendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		final String varName = "reflectionEntry_" + _sharedReuseGroup;
		final var var = player.getVariable(varName);
		if (var != null)
		{
			if (_attempts > 1)
			{
				final int attempts = Integer.parseInt(var.getValue());
				final long time = var.getExpireTime();
				player.unsetVar(varName);
				if ((attempts < _attempts) && !var.isExpired())
				{
					player.setVar(varName, String.valueOf((attempts + 1)), time);
				}
				else
				{
					player.setVar(varName, String.valueOf(1), calendar.getTimeInMillis());
				}
			}
			else
			{
				player.unsetVar(varName);
				player.setVar(varName, String.valueOf(1), calendar.getTimeInMillis());
			}
		}
		else
		{
			player.setVar(varName, String.valueOf(1), calendar.getTimeInMillis());
		}
		return true;
	}
}