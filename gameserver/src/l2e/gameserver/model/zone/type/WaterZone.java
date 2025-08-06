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

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;

public class WaterZone extends ZoneType
{
	public WaterZone(int id)
	{
		super(id);
		addZoneId(ZoneId.WATER);
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (character.isPlayer())
		{
			final Player player = character.getActingPlayer();
			if (player.isTransformed() && !player.getTransformation().canSwim())
			{
				character.stopTransformation(true);
			}
			else
			{
				player.broadcastUserInfo(true);
			}
		}
		else if (character.isNpc())
		{
			character.broadcastInfo();
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		if (character.isPlayer())
		{
			character.getActingPlayer().broadcastUserInfo(true);
		}
		else if (character.isNpc())
		{
			character.broadcastInfo();
		}
	}
	
	public int getWaterMinZ()
	{
		return getZone().getLowZ();
	}
	
	public int getWaterZ()
	{
		return getZone().getHighZ();
	}
	
	public boolean canUseWaterTask()
	{
		return Math.abs(getWaterMinZ() - getWaterZ()) > 100;
	}
}