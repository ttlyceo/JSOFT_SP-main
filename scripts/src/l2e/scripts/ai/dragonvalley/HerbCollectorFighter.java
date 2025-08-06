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
package l2e.scripts.ai.dragonvalley;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.holders.SkillHolder;

/**
 * Created by LordWinter 18.04.2023
 */
public class HerbCollectorFighter extends Fighter
{
	private static SkillHolder _vitalityBuff = new SkillHolder(6883, 1);
	
	public HerbCollectorFighter(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final var actor = getActiveChar();
		
		super.onEvtDead(killer);
		
		if (killer != null && killer.isPlayer() && actor.isSweepActive())
		{
			final var player = killer.getActingPlayer();
			if (Rnd.get(1000) < 5 && _vitalityBuff.getSkill() != null)
			{
				if (player.isInParty())
				{
					player.getParty().getMembers().stream().filter(m -> m != null && !m.isDead() && actor.isInRangeZ(m, Config.ALT_PARTY_RANGE)).forEach(i -> _vitalityBuff.getSkill().getEffects(i, i, false));
				}
				else
				{
					_vitalityBuff.getSkill().getEffects(player, player, false);
				}
			}
			actor.dropSingleItem(player, Rnd.get(100) <= 50 ? 8604 : 8605, 1);
		}
	}
}