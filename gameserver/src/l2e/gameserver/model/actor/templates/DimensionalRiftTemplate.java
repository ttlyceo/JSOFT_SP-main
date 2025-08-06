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
package l2e.gameserver.model.actor.templates;

import l2e.gameserver.model.stats.StatsSet;

public final class DimensionalRiftTemplate
{
	private final byte _type;
	private final int _minPlayers;
	private final long _fragmentCount;
	private final double _bossRoomChance;
	private final int _maxJumps;
	private final StatsSet _params;
	private final boolean _customTeleFunction;
	private final int _hwidsLimit;
	private final int _ipsLimit;

	public DimensionalRiftTemplate(byte type, int minPlayers, long fragmentCount, double bossRoomChance, int maxJumps, StatsSet params, boolean customTeleFunction, int hwidsLimit, int ipsLimit)
	{
		_type = type;
		_minPlayers = minPlayers;
		_fragmentCount = fragmentCount;
		_bossRoomChance = bossRoomChance;
		_maxJumps = maxJumps;
		_params = params;
		_customTeleFunction = customTeleFunction;
		_hwidsLimit = hwidsLimit;
		_ipsLimit = ipsLimit;
	}

	public byte getType()
	{
		return _type;
	}

	public int getMinPlayers()
	{
		return _minPlayers;
	}

	public long getFragmentCount()
	{
		return _fragmentCount;
	}

	public double getBossRoomChance()
	{
		return _bossRoomChance;
	}
	
	public int getMaxJumps()
	{
		return _maxJumps;
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
	
	public boolean isCustomTeleFunction()
	{
		return _customTeleFunction;
	}
	
	public int getHwidsLimit()
	{
		return _hwidsLimit;
	}
	
	public int getIpsLimit()
	{
		return _ipsLimit;
	}
}