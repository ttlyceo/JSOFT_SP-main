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
package l2e.gameserver.model;

public enum PcCondOverride
{
	MAX_STATS_VALUE(0, "PcCondOverride.MAX_STATS_VALUE"), ITEM_CONDITIONS(1, "PcCondOverride.ITEM_CONDITIONS"), SKILL_CONDITIONS(2, "PcCondOverride.SKILL_CONDITIONS"), ZONE_CONDITIONS(3, "PcCondOverride.ZONE_CONDITIONS"), CASTLE_CONDITIONS(4, "PcCondOverride.CASTLE_CONDITIONS"), FORTRESS_CONDITIONS(5, "PcCondOverride.FORTRESS_CONDITIONS"), CLANHALL_CONDITIONS(6, "PcCondOverride.CLANHALL_CONDITIONS"), FLOOD_CONDITIONS(7, "PcCondOverride.FLOOD_CONDITIONS"), CHAT_CONDITIONS(8, "PcCondOverride.CHAT_CONDITIONS"), INSTANCE_CONDITIONS(9, "PcCondOverride.INSTANCE_CONDITIONS"), QUEST_CONDITIONS(10, "PcCondOverride.QUEST_CONDITIONS"), DEATH_PENALTY(11, "PcCondOverride.DEATH_PENALTY"), DESTROY_ALL_ITEMS(12, "PcCondOverride.DESTROY_ALL_ITEMS"), SEE_ALL_PLAYERS(13, "PcCondOverride.SEE_ALL_PLAYERS"), TARGET_ALL(14, "PcCondOverride.TARGET_ALL"), DROP_ALL_ITEMS(15, "PcCondOverride.DROP_ALL_ITEMS");
	
	private final int _mask;
	private final String _descr;
	
	private PcCondOverride(int id, String descr)
	{
		_mask = 1 << id;
		_descr = descr;
	}
	
	public int getMask()
	{
		return _mask;
	}
	
	public String getDescription()
	{
		return _descr;
	}
	
	public static PcCondOverride getCondOverride(int ordinal)
	{
		try
		{
			return values()[ordinal];
		}
		catch (final Exception e)
		{
			return null;
		}
	}
	
	public static long getAllExceptionsMask()
	{
		long result = 0L;
		for (final PcCondOverride ex : values())
		{
			result |= ex.getMask();
		}
		return result;
	}
}