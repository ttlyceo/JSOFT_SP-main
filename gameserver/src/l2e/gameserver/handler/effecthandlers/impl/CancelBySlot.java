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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class CancelBySlot extends Effect
{
	private final String _dispel;
	private final Map<String, Byte> _dispelAbnormals;
	
	public CancelBySlot(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_dispel = template.getParameters().getString("dispel", null);
		if ((_dispel != null) && !_dispel.isEmpty())
		{
			_dispelAbnormals = new ConcurrentHashMap<>();
			for (final String ngtStack : _dispel.split(";"))
			{
				final String[] ngt = ngtStack.split(",");
				_dispelAbnormals.put(ngt[0], Byte.parseByte(ngt[1]));
			}
		}
		else
		{
			_dispelAbnormals = Collections.<String, Byte> emptyMap();
		}
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.CANCEL_BY_SLOT;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public boolean onStart()
	{
		if (_dispelAbnormals.isEmpty())
		{
			return false;
		}
		
		final var target = getEffected();
		if ((target == null) || target.isDead())
		{
			return false;
		}
		
		for (final var value : _dispelAbnormals.entrySet())
		{
			final String stackType = value.getKey();
			final float stackOrder = value.getValue();
			final int skillCast = getSkill().getId();
			for (final var e : target.getAllEffects())
			{
				if (!e.getSkill().canBeDispeled() && skillCast != 2060 && skillCast != 2530)
				{
					continue;
				}
				
				if (stackType.equalsIgnoreCase(e.getAbnormalType()) && (e.getSkill().getId() != skillCast))
				{
					if (e.getSkill() != null)
					{
						if (e.triggersChanceSkill())
						{
							target.removeChanceEffect(e);
						}
						
						if (stackOrder == -1)
						{
							target.stopSkillEffects(e.getSkill().getId());
						}
						else if (stackOrder >= e.getAbnormalLvl())
						{
							target.stopSkillEffects(e.getSkill().getId());
						}
					}
				}
			}
		}
		return true;
	}
}
