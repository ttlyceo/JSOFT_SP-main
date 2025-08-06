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
package l2e.gameserver.model.actor.templates.npc.aggro;

import l2e.gameserver.model.actor.Creature;

public final class AggroInfo
{
	private final Creature _attacker;
	private int _hate = 0;
	private int _damage = 0;
	
	public AggroInfo(Creature pAttacker)
	{
		_attacker = pAttacker;
	}
	
	public final Creature getAttacker()
	{
		return _attacker;
	}
	
	public final int getHate()
	{
		return _hate;
	}
	
	public final int checkHate()
	{
		return _attacker.isAlikeDead() || !_attacker.isVisible() ? 0 : _hate;
	}
	
	public final void addHate(int value)
	{
		_hate = (int) Math.min(_hate + (long) value, 999999999);
	}
	
	public final void stopHate()
	{
		_hate = 0;
	}
	
	public final int getDamage()
	{
		return _damage;
	}
	
	public final void addDamage(int value)
	{
		_damage = (int) Math.min(_damage + (long) value, 999999999);
	}
	
	@Override
	public final boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if (obj instanceof AggroInfo)
		{
			return (((AggroInfo) obj).getAttacker() == _attacker);
		}
		return false;
	}
	
	@Override
	public final int hashCode()
	{
		return _attacker.getObjectId();
	}
}