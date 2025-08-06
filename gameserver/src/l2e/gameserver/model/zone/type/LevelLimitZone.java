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
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;

/**
 * Created by LordWinter
 */
public class LevelLimitZone extends ZoneType
{
	private int _minLvl = 1;
	private int _maxLvl = 85;
	private Location _exitLoc = null;
	
	public LevelLimitZone(int id)
	{
		super(id);
		addZoneId(ZoneId.LEVEL_LIMIT);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("avaliableLvls"))
		{
			final String[] propertySplit = value.split(",");
			if (propertySplit.length != 0)
			{
				_minLvl = Integer.parseInt(propertySplit[0]);
				_maxLvl = Integer.parseInt(propertySplit[1]);
			}
		}
		else if (name.equals("exitLocation"))
		{
			final String[] propertySplit = value.split(",");
			if (propertySplit.length == 3)
			{
				_exitLoc = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]), Integer.parseInt(propertySplit[2]));
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
		if (character.getLevel() < _minLvl || character.getLevel() > _maxLvl)
		{
			if (character.isSummon())
			{
				((Summon) character).unSummon(character.getActingPlayer());
			}
			else if (character.isPlayer())
			{
				if (!character.getActingPlayer().isGM())
				{
					if (_exitLoc != null)
					{
						character.getActingPlayer().teleToLocation(_exitLoc.getX(), _exitLoc.getY(), _exitLoc.getZ(), true, ReflectionManager.DEFAULT);
					}
				}
			}
		}
	}
	
	@Override
	protected void onExit(final Creature character)
	{
	}
}