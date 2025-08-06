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
package l2e.gameserver.model.actor.templates.reflection;

import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate.ReflectionRemoveType;

public class ReflectionItemTemplate
{
	private final int _id;
	private final long _count;
	private final boolean _isNecessary;
	private final ReflectionRemoveType _type;
	
	public ReflectionItemTemplate(int id, long count, boolean isNecessary, ReflectionRemoveType type)
	{
		_id = id;
		_count = count;
		_isNecessary = isNecessary;
		_type = type;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public long getCount()
	{
		return _count;
	}
	
	public boolean isNecessary()
	{
		return _isNecessary;
	}
	
	public ReflectionRemoveType getType()
	{
		return _type;
	}
}