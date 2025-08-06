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
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;

public class HqZone extends ZoneType
{
	private int _clanHallId = 0;
	private int _fortId = 0;
	private int _castleId = 0;
	private int _territoryId = 0;
	
	public HqZone(final int id)
	{
		super(id);
		addZoneId(ZoneId.HQ);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if ("castleId".equals(name))
		{
			_castleId = Integer.parseInt(value);
		}
		else if ("fortId".equals(name))
		{
			_fortId = Integer.parseInt(value);
		}
		else if ("clanHallId".equals(name))
		{
			_clanHallId = Integer.parseInt(value);
		}
		else if ("territoryId".equals(name))
		{
			_territoryId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(final Creature character)
	{
	}
	
	@Override
	protected void onExit(final Creature character)
	{
	}
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	public int getFortId()
	{
		return _fortId;
	}
	
	public int getClanHallId()
	{
		return _clanHallId;
	}
	
	public int getTerritoryId()
	{
		return _territoryId;
	}
}