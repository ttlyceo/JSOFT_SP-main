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
package l2e.scripts.ai.hellbound;

import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;

/**
 * Created by LordWinter 22.11.2018
 */
public class Remnant extends Fighter
{
	public Remnant(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtSpawn()
	{
		getActiveChar().setIsMortal(false);
		super.onEvtSpawn();
	}
	
	@Override
	protected void onEvtSeeSpell(Skill skill, Creature caster)
	{
		if (skill.getId() != 2358)
		{
			return;
		}
		
		final Attackable actor = getActiveChar();
		
		if (!actor.isDead() && (actor.getCurrentHpPercents() < 10))
		{
			actor.setIsMortal(true);
			actor.doDie(caster);
		}
		super.onEvtSeeSpell(skill, caster);
	}
}
