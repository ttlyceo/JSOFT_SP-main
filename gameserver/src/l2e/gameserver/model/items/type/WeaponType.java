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
package l2e.gameserver.model.items.type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum WeaponType implements ItemType
{
	SWORD("Sword"),
	BLUNT("Blunt"),
	DAGGER("Dagger"),
	BOW("Bow"),
	POLE("Pole"),
	NONE("None"),
	DUAL("Dual Sword"),
	ETC("Etc"),
	FIST("Fist"),
	DUALFIST("Dual Fist"),
	FISHINGROD("Rod"),
	RAPIER("Rapier"),
	ANCIENTSWORD("Ancient"),
	CROSSBOW("Crossbow"),
	FLAG("Flag"),
	OWNTHING("Ownthing"),
	DUALDAGGER("Dual Dagger"),
	BIGBLUNT("Big Blunt"),
	BIGSWORD("Big Sword");
	
	private static final Logger _log = LoggerFactory.getLogger(WeaponType.class);
	private final int _mask;
	private final String _name;
	
	private WeaponType(String name)
	{
		_mask = 1 << ordinal();
		_name = name;
	}
	
	@Override
	public int mask()
	{
		return _mask;
	}
	
	@Override
	public String toString()
	{
		return _name;
	}

	public static WeaponType findByName(String name)
	{
		if (name.equalsIgnoreCase("DUAL"))
		{
			name = "Dual Sword";
		}
		else if (name.equalsIgnoreCase("DUALFIST"))
		{
			name = "Dual Fist";
		}
		for (final WeaponType type : values())
		{
			if (type.toString().equalsIgnoreCase(name))
			{
				return type;
			}
		}
		_log.warn(WeaponType.class.getSimpleName() + ": Requested unexistent enum member: " + name, new IllegalStateException());
		return FIST;
	}
	
	public boolean isRanged()
	{
		return (this == BOW) || (this == CROSSBOW);
	}
	
	public boolean isBow()
	{
		return (this == BOW);
	}
	
	public boolean isCrossbow()
	{
		return (this == CROSSBOW);
	}
}