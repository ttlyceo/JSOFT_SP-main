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

import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class Invincible extends Effect
{
	private final boolean _blockBuff, _blockDebuff, _isHealBlock;
	
	public Invincible(Env env, EffectTemplate template)
	{
		super(env, template);
		
		_isHealBlock = template.getParameters().getBool("healBlock", true);
		_blockBuff = template.getParameters().getBool("blockBuff", false);
		_blockDebuff = template.getParameters().getBool("blockDebuff", true);
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.INVUL.getMask();
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.INVINCIBLE;
	}
	
	@Override
	public boolean onStart()
	{
		getEffected().startHealBlocked(_isHealBlock);
		getEffected().setIsInvul(true);
		if (_blockBuff)
		{
			getEffected().startBuffImmunity(true);
		}
		if (_blockDebuff)
		{
			getEffected().startDebuffImmunity(true);
		}
		return super.onStart();
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
	
	@Override
	public void onExit()
	{
		final var amount = getEffected().getEffectList().getEffectTypeAmount(EffectType.INVINCIBLE);
		if (amount < 1)
		{
			getEffected().startHealBlocked(false);
			getEffected().setIsInvul(false);
			getEffected().startBuffImmunity(false);
			getEffected().startDebuffImmunity(false);
		}
		super.onExit();
	}
}