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

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.instance.NpcInstance;
import l2e.gameserver.model.actor.instance.SiegeSummonInstance;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.network.serverpackets.FinishRotatings;
import l2e.gameserver.network.serverpackets.StartRotation;

public class Bluff extends Effect
{
	private final int _chance;

	public Bluff(Env env, EffectTemplate template)
	{
		super(env, template);
		_chance = template.getParameters().getInteger("chance", 100);
	}

	@Override
	public boolean calcSuccess()
	{
		return Formulas.calcProbability(_chance, getEffector(), getEffected(), getSkill(), false);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
	
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof NpcInstance)
		{
			return false;
		}
		
		if ((getEffected() instanceof Npc) && (((Npc) getEffected()).getId() == 35062))
		{
			return false;
		}
		
		if (getEffected() instanceof SiegeSummonInstance)
		{
			return false;
		}
		
		getEffected().broadcastPacket(new StartRotation(getEffected().getObjectId(), getEffected().getHeading(), 1, 65535));
		getEffected().broadcastPacket(new FinishRotatings(getEffected().getObjectId(), getEffector().getHeading(), 65535));
		getEffected().setHeading(getEffector().getHeading());
		return true;
	}
}