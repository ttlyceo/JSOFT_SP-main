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

import l2e.commons.util.Rnd;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class SummonCubic extends Effect
{
	private final int _npcId;
	private final int _cubicPower;
	private final int _cubicDuration;
	private final int _cubicDelay;
	private final int _cubicMaxCount;
	private final int _cubicSkillChance;

	public SummonCubic(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_npcId = template.getParameters().getInteger("npcId", 0);
		_cubicPower = template.getParameters().getInteger("cubicPower", 0);
		_cubicDuration = template.getParameters().getInteger("cubicDuration", 0);
		_cubicDelay = template.getParameters().getInteger("cubicDelay", 0);
		_cubicMaxCount = template.getParameters().getInteger("cubicMaxCount", -1);
		_cubicSkillChance = template.getParameters().getInteger("cubicSkillChance", 0);
		if (_npcId > 0)
		{
			template.setCubicId(_npcId);
		}
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SUMMON_CUBIC;
	}
	
	@Override
	public boolean onStart()
	{
		if ((getEffected() == null) || !getEffected().isPlayer() || getEffected().isAlikeDead() || getEffected().getActingPlayer().inObserverMode())
		{
			return false;
		}
		
		if (_npcId <= 0)
		{
			_log.warn(SummonCubic.class.getSimpleName() + ": Invalid NPC Id:" + _npcId + " in skill Id: " + getSkill().getId());
			return false;
		}
		
		final Player player = getEffected().getActingPlayer();
		if (player.inObserverMode() || player.isMounted())
		{
			return false;
		}
		
		int _cubicSkillLevel = getSkill().getLevel();
		if (_cubicSkillLevel > 100)
		{
			_cubicSkillLevel = ((getSkill().getLevel() - 100) / 7) + 8;
		}
		
		if (!player.removeCubicById(_npcId))
		{
			final Effect cubicMastery = player.getFirstPassiveEffect(EffectType.CUBIC_MASTERY);
			final int cubicCount = (int) (cubicMastery != null ? (cubicMastery.calc() - 1) : 0);
			final var cubicSize = player.getCubicsSize();
			if (cubicSize > cubicCount)
			{
				final int random = Rnd.get(cubicSize);
				final var removedCubic = player.getCubicByPosition(random);
				if (removedCubic != null)
				{
					player.removeCubicById(removedCubic.getId());
				}
				
			}
		}
		player.addCubic(_npcId, _cubicSkillLevel, _cubicPower, _cubicDelay, _cubicSkillChance, _cubicMaxCount, _cubicDuration, getEffected() != getEffector());
		player.broadcastUserInfo(true);
		return true;
	}
}