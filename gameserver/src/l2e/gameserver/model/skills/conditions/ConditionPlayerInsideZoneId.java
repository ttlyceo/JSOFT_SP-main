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
package l2e.gameserver.model.skills.conditions;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.zone.ZoneType;

public class ConditionPlayerInsideZoneId extends Condition
{
	private final List<Integer> _zones;

	public ConditionPlayerInsideZoneId(ArrayList<Integer> zones)
	{
		_zones = zones;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if (env.getPlayer() == null || _zones.isEmpty())
		{
			return false;
		}
		
		final List<ZoneType> zones = ZoneManager.getInstance().getZones(env.getCharacter());
		if (zones != null && !zones.isEmpty())
		{
			for (final ZoneType zone : zones)
			{
				if (zone != null && _zones.contains(zone.getId()))
				{
					return true;
				}
			}
		}
		return false;
	}
}