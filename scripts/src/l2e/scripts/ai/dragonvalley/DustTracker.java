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
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.holders.SkillHolder;

/**
 * Created by LordWinter 29.05.2019
 */
public class DustTracker extends Patrollers
{
	private static SkillHolder _vitalityBuff = new SkillHolder(6883, 1);
	
	public DustTracker(Attackable actor)
	{
		super(actor);
		
		_points = new Location[]
		{
		        new Location(125176, 111896, -3168), new Location(124872, 109736, -3104), new Location(123608, 108712, -3024), new Location(122632, 108008, -2992), new Location(120504, 109000, -2944), new Location(118632, 109944, -2960), new Location(115208, 109928, -3040), new Location(112568, 110296, -2976), new Location(110264, 111320, -3152), new Location(109512, 113432, -3088), new Location(109272, 116104, -3104), new Location(108008, 117912, -3056)
		};
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
