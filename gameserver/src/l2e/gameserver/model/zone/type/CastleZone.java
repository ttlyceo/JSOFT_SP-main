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

import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneRespawn;

public class CastleZone extends ZoneRespawn
{
	private int _castleId;
	private Castle _castle = null;

	public CastleZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
			_castle = CastleManager.getInstance().getCastleById(_castleId);
			if (_castle != null)
			{
				addZoneId(ZoneId.CASTLE);
			}
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

	@Override
	public void onDieInside(Creature character)
	{
	}

	@Override
	public void onReviveInside(Creature character)
	{
	}

	public void banishForeigners(int owningClanId)
	{
		final TeleportWhereType type = TeleportWhereType.TOWN;
		for (final Player temp : getPlayersInside())
		{
			if (temp.getClanId() == owningClanId && owningClanId != 0)
			{
				continue;
			}

			temp.teleToLocation(type, true, ReflectionManager.DEFAULT);
		}
	}

	public int getCastleId()
	{
		return _castleId;
	}
}