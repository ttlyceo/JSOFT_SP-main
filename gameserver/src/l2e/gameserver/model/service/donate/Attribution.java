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
package l2e.gameserver.model.service.donate;

public class Attribution
{
	private final int _id;
	private final long _count;
	private final int _value;
	private final int _size;
	
	public Attribution(int id, long count, int value, int size)
	{
		_id = id;
		_count = count;
		_value = value;
		_size = size;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public long getCount()
	{
		return _count;
	}
	
	public int getValue()
	{
		return _value;
	}
	
	public int getSize()
	{
		return _size;
	}
}