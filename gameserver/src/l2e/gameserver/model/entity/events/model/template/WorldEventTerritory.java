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
package l2e.gameserver.model.entity.events.model.template;

import l2e.gameserver.model.spawn.SpawnTerritory;

/**
 * Created by LordWinter 13.07.2020
 */
public class WorldEventTerritory
{
	private final String _name;
	private final SpawnTerritory _territory;
	
	public WorldEventTerritory(String name, SpawnTerritory territory)
	{
		_name = name;
		_territory = territory;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public SpawnTerritory getTerritory()
	{
		return _territory;
	}
}