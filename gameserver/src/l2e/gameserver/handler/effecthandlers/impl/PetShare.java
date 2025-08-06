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

import java.util.HashMap;
import java.util.Map;

import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Stats;

public class PetShare extends Effect
{
	private final Map<Stats, Double> stats = new HashMap<>(9);

	public PetShare(Env env, EffectTemplate template)
	{
		super(env, template);
		for (final String key : template.getParameters().keySet())
		{
			stats.put(Stats.valueOfXml(key), template.getParameters().getDouble(key, 1.));
		}
	}

	@Override
	public boolean canBeStolen()
	{
		return false;
	}

	@Override
	public int getEffectFlags()
	{
		return EffectFlag.SERVITOR_SHARE.getMask();
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}
	
	@Override
	public boolean onStart()
	{
		super.onStart();
		if (getEffected() != null && getEffected().isPlayer())
		{
			getEffected().getActingPlayer().setPetShare(stats);
			final var summon = getEffected().getActingPlayer().getSummon();
			if (summon != null && summon.isPet())
			{
				getEffected().getActingPlayer().getSummon().broadcastInfo();
				getEffected().getActingPlayer().getSummon().getStatus().startHpMpRegeneration();
			}
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if (getEffected() != null && getEffected().isPlayer())
		{
			getEffected().getActingPlayer().setPetShare(null);
			final var summon = getEffected().getActingPlayer().getSummon();
			if (summon != null && summon.isPet())
			{
				if (summon.getCurrentHp() > summon.getMaxHp())
				{
					summon.setCurrentHp(getEffected().getSummon().getMaxHp());
				}
				if (summon.getCurrentMp() > summon.getMaxMp())
				{
					summon.setCurrentMp(getEffected().getSummon().getMaxMp());
				}
				summon.broadcastInfo();
			}
		}
	}
}