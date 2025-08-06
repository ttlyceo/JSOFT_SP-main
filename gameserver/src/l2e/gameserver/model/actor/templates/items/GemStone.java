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
package l2e.gameserver.model.actor.templates.items;

public class GemStone
{
	private final int _grade;
	private final int _gemId;
	private final int _count;
	private final int _accessoryCount;
	
	public GemStone(int grade, int gemId, int count, int accessoryCount)
	{
		_grade = grade;
		_gemId = gemId;
		_count = count;
		_accessoryCount = accessoryCount;
	}
	
	public final int getGrade()
	{
		return _grade;
	}
	
	public final int getGemId()
	{
		return _gemId;
	}
	
	public final int getCount()
	{
		return _count;
	}
	
	public final int getAccessoryCount()
	{
		return _accessoryCount;
	}
}