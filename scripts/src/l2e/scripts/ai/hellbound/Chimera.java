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

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;

/**
 * Created by LordWinter 21.09.2018
 */
public class Chimera extends Fighter
{
	public Chimera(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtSeeSpell(Skill skill, Creature caster)
	{
		if (skill.getId() != 2359)
		{
			return;
		}
		final Attackable actor = getActiveChar();
		
		if (!actor.isDead() && (actor.getCurrentHpPercents() > 10))
		{
			return;
		}
		if (HellboundManager.getInstance().getLevel() == 7)
		{
			HellboundManager.getInstance().updateTrust(3, true);
		}
		
		switch (actor.getId())
		{
			case 22353 :
				actor.dropSingleItem(caster.getActingPlayer(), 9682, 1);
				break;
			case 22349 :
			case 22350 :
			case 22351 :
			case 22352 :
				if (Rnd.chance(70))
				{
					actor.dropSingleItem(caster.getActingPlayer(), Rnd.chance(30) ? 9681 : 9680, 1);
				}
				break;
		}
		actor.doDie(null);
		actor.endDecayTask();
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		if (attacker != null && HellboundManager.getInstance().getLevel() < 7)
		{
			attacker.teleToLocation(-11272, 236464, -3248, true, attacker.getReflection());
			return;
		}
		super.onEvtAttacked(attacker, damage);
	}
}
