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

import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneRespawn;

public class FortZone extends ZoneRespawn
{
	private int _fortId;

	public FortZone(int id)
	{
		super(id);
		addZoneId(ZoneId.FORT);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("fortId"))
		{
			_fortId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(Creature character)
	{
	}

	@Override
	protected void onExit(Creature character)
	{
	}

	public void updateZoneStatusForCharactersInside()
	{
	}

	public void banishForeigners(int owningClanId)
	{
		final TeleportWhereType type = TeleportWhereType.FORTRESS_BANISH;
		for (final Player temp : getPlayersInside())
		{
			if ((temp.getClanId() == owningClanId) && (owningClanId != 0))
			{
				continue;
			}

			temp.teleToLocation(type, true, ReflectionManager.DEFAULT);
		}
	}

	public int getFortId()
	{
		return _fortId;
	}
}