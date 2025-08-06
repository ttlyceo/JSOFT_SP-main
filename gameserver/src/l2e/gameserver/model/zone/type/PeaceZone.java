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
package l2e.gameserver.model.zone.type;

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;

public class PeaceZone extends ZoneType
{
	public PeaceZone(int id)
	{
		super(id);
		
		if (Config.PEACE_ZONE_MODE != 2)
		{
			addZoneId(ZoneId.PEACE);
		}
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		final Player player = character.getActingPlayer();

		if (character.isPlayer())
		{
			if (player.isCombatFlagEquipped() && TerritoryWarManager.getInstance().isTWInProgress())
			{
				TerritoryWarManager.getInstance().dropCombatFlag(player, false, true);
			}

			if ((player.getSiegeState() != 0) && (Config.PEACE_ZONE_MODE == 1))
			{
				return;
			}
		}

		if (Config.PEACE_ZONE_MODE != 2)
		{
			if (character.isPlayer())
			{
				if (player != null)
				{
					player.getRecommendation().stopRecBonus();
					if (player.getNevitSystem().isActive())
					{
						player.getNevitSystem().stopAdventTask(true);
					}
				}

				if (Config.SPEED_UP_RUN)
				{
					if (player != null)
					{
						player.broadcastUserInfo(true);
					}
				}
			}
		}
	}

	@Override
	protected void onExit(Creature character)
	{
		if (Config.SPEED_UP_RUN)
		{
			if (character.isPlayer())
			{
				character.getActingPlayer().broadcastUserInfo(true);
			}
		}
	}
}