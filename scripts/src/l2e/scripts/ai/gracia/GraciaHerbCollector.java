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
package l2e.scripts.ai.gracia;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.CategoryParser;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.zone.type.EffectZone;

/**
 * Created by LordWinter 18.04.2023
 */
public class GraciaHerbCollector extends Fighter
{
	private static final int[] ZONE_BUFFS =
	{
	        60006, 60007, 60008
	};
	
	public GraciaHerbCollector(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable npc = getActiveChar();
		if (npc.isScriptValue(0) && attacker != null && attacker.isPlayer())
		{
			final var player = attacker.getActingPlayer();
			if (player != null && CategoryParser.getInstance().isInCategory(CategoryType.WIZARD_GROUP, player.getClassId().getId()))
			{
				npc.setScriptValue(1);
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable npc = getActiveChar();
		super.onEvtDead(killer);
		if (npc.isScriptValue(1) && killer != null && killer.isPlayer())
		{
			final int zoneId = ServerVariables.getInt("SOABuffList", 0);
			final var zone = ZoneManager.getInstance().getZoneById(ZONE_BUFFS[zoneId], EffectZone.class);
			if (zone != null && zone.isInsideZone(npc.getLocation()))
			{
				if (Rnd.get(100) < 70)
				{
					npc.dropSingleItem(killer.getActingPlayer(), 8603, 1);
				}
				
				if (Rnd.get(100) < 70)
				{
					npc.dropSingleItem(killer.getActingPlayer(), 8603, 1);
				}
				
				if (Rnd.get(100) > 70)
				{
					npc.dropSingleItem(killer.getActingPlayer(), 8604, 1);
				}
			}
		}
	}
}