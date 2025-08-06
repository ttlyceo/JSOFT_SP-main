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

public class RecipeStatTemplate
{
	private StatType _type;
	private int _value;

	public static enum StatType
	{
		HP,
		MP,
		XP,
		SP,
		GIM
	}
	
	public RecipeStatTemplate(String type, int value)
	{
		_type = Enum.valueOf(StatType.class, type);
		_value = value;
	}
	
	public StatType getType()
	{
		return _type;
	}
	
	public int getValue()
	{
		return _value;
	}	
}