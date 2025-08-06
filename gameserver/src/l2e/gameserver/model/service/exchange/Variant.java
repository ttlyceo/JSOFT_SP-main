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
package l2e.gameserver.model.service.exchange;

public class Variant
{
	final int _number;
	final int _id;
	final String _name;
	final String _icon;
	
	public Variant(int number, int id, String name, String icon)
	{
		_number = number;
		_id = id;
		_name = name;
		_icon = icon;
	}
	
	public int getNumber()
	{
		return _number;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getIcon()
	{
		return _icon;
	}
}