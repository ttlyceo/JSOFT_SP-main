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
package l2e.gameserver.model.actor.templates.sieges;

import l2e.gameserver.model.entity.Siege;

public class SiegeTemplate
{
	private final Siege _siege;
	private final boolean _isStartSige;
	
	public SiegeTemplate(Siege siege, boolean isStartSige)
	{
		_siege = siege;
		_isStartSige = isStartSige;
	}
	
	public Siege getSiege()
	{
		return _siege;
	}
	
	public boolean isStartSige()
	{
		return _isStartSige;
	}
}