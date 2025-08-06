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

import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.actor.instance.EffectPointInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.l2skills.SkillSignetCasttime;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.stats.Env;

public class SignetMDam extends Effect
{
	private EffectPointInstance _actor;

	public SignetMDam(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SIGNET_GROUND;
	}
	
	@Override
	public boolean onStart()
	{
		NpcTemplate template;
		if (getSkill() instanceof SkillSignetCasttime)
		{
			template = NpcsParser.getInstance().getTemplate(getSkill().getNpcId());
		}
		else
		{
			return false;
		}
		
		_actor = new EffectPointInstance(IdFactory.getInstance().getNextId(), template, getEffector(), getSkill());
		_actor.setCurrentHp(_actor.getMaxHp());
		_actor.setCurrentMp(_actor.getMaxMp());
		int x = getEffector().getX();
		int y = getEffector().getY();
		int z = getEffector().getZ();
		if (getEffector().isPlayer() && (getSkill().getTargetType() == TargetType.GROUND))
		{
			final var wordPosition = getEffector().getActingPlayer().getCurrentSkillWorldPosition();
			if (wordPosition != null)
			{
				x = wordPosition.getX();
				y = wordPosition.getY();
				z = wordPosition.getZ();
			}
		}
		_actor.setIsInvul(true);
		_actor.spawnMe(x, y, z);
		if (_actor.getAI() != null)
		{
			_actor.getAI().enableAI();
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if (_actor != null)
		{
			_actor.deleteMe();
		}
	}
}