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
package l2e.gameserver.model.stats;

import l2e.gameserver.model.interfaces.IRestorable;
import l2e.gameserver.model.interfaces.IStorable;

public class NpcStats extends StatsSet implements IRestorable, IStorable
{
	private static final long serialVersionUID = -3001662130903838717L;
	private volatile boolean _changes = false;
	
	public NpcStats()
	{
		super();
	}

	@Override
	public final void set(String name, boolean value)
	{
		super.set(name, value);
		_changes = true;
	}

	@Override
	public final void set(String name, double value)
	{
		super.set(name, value);
		_changes = true;
	}

	@Override
	public final void set(String name, Enum<?> value)
	{
		super.set(name, value);
		_changes = true;
	}

	@Override
	public final void set(String name, int value)
	{
		super.set(name, value);
		_changes = true;
	}

	@Override
	public final void set(String name, long value)
	{
		super.set(name, value);
		_changes = true;
	}

	@Override
	public final void set(String name, String value)
	{
		super.set(name, value);
		_changes = true;
	}

	@Override
	public int getInteger(String key)
	{
		return super.getInteger(key, 0);
	}

	@Override
	public boolean restoreMe()
	{
		return true;
	}

	@Override
	public boolean storeMe()
	{
		return true;
	}

	public boolean hasVariable(String name)
	{
		return containsKey(name);
	}

	public final boolean getAndResetChanges()
	{
		final boolean changes = _changes;
		if (changes)
		{
			_changes = false;
		}
		return changes;
	}

	public final void remove(String name)
	{
		unset(name);
		_changes = true;
	}
}