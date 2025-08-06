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

public class TownZone extends ZoneType
{
	private int _townId;
	private int _taxById;

	public TownZone(int id)
	{
		super(id);

		_taxById = 0;
		addZoneId(ZoneId.TOWN);
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("townId"))
		{
			_townId = Integer.parseInt(value);
		}
		else if (name.equals("taxById"))
		{
			_taxById = Integer.parseInt(value);
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

	public void updateForCharactersInside()
	{
		for (final Creature character : _characterList.values())
		{
			if (character != null)
			{
				onEnter(character);
			}
		}
	}

	public int getTownId()
	{
		return _townId;
	}

	public final int getTaxById()
	{
		return _taxById;
	}
}