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
package l2e.gameserver.model.actor.templates.npc;

public class DamageLimit
{
	private final int _damage;
	private final int _physicDamage;
	private final int _magicDamage;

	public DamageLimit(int damage, int physicDamage, int magicDamage)
	{
		_damage = damage;
		_physicDamage = physicDamage;
		_magicDamage = magicDamage;
	}
	
	public int getDamage()
	{
		return _damage;
	}

	public int getPhysicDamage()
	{
		return _physicDamage;
	}
	
	public int getMagicDamage()
	{
		return _magicDamage;
	}
}