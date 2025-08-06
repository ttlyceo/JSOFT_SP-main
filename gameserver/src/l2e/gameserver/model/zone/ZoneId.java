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
package l2e.gameserver.model.zone;

public enum ZoneId
{
	PVP(0), PEACE(1), SIEGE(2), MOTHER_TREE(3), CLAN_HALL(4), LANDING(5), NO_LANDING(6), WATER(7), JAIL(8), MONSTER_TRACK(9), CASTLE(10), SWAMP(11), NO_SUMMON_FRIEND(12), FORT(13), TOWN(14), SCRIPT(15), HQ(16), DANGER_AREA(17), ALTERED(18), NO_BOOKMARK(19), NO_ITEM_DROP(20), NO_RESTART(21), FUN_PVP(22), LEVEL_LIMIT(23), NO_GEO(24), REFLECTION(25), HP_LIMIT(26), MP_LIMIT(27), CP_LIMIT(28), P_ATK_LIMIT(29), P_DEF_LIMIT(30), ATK_SPEED_LIMIT(31), M_ATK_LIMIT(32), M_DEF_LIMIT(33), M_ATK_SPEED_LIMIT(34), CRIT_DMG_LIMIT(35), RUN_SPEED_LIMIT(36), WALK_SPEED_LIMIT(37), ACCURACY_LIMIT(38), CRIT_HIT_LIMIT(39), MCRIT_HIT_LIMIT(40), EVASION_LIMIT(41), PVP_PHYS_SKILL_DMG_LIMIT(42), PVP_PHYS_SKILL_DEF_LIMIT(43), PVP_PHYS_DEF_LIMIT(44), PVP_PHYS_DMG_LIMIT(45), PVP_MAGIC_DMG_LIMIT(46), PVP_MAGIC_DEF_LIMIT(47), NO_TRADE(48);
	
	private final int _id;
	
	private ZoneId(int id)
	{
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public static int getZoneCount()
	{
		return values().length;
	}
}