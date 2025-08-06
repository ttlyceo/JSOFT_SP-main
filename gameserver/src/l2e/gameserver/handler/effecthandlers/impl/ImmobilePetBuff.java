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

import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;

public class ImmobilePetBuff extends Effect
{
	private Summon _pet;

	public ImmobilePetBuff(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}

	@Override
	public boolean onStart()
	{
		_pet = null;

		if (getEffected().isSummon() && getEffector().isPlayer() && ((Summon) getEffected()).getOwner() == getEffector())
		{
			_pet = (Summon) getEffected();
			_pet.setIsImmobilized(true);
			return true;
		}
		return false;
	}

	@Override
	public void onExit()
	{
		if (_pet != null)
		{
			_pet.setIsImmobilized(false);
		}
	}
}