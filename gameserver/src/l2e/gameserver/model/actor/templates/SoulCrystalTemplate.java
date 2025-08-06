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

public class SoulCrystalTemplate
{
	private final int _itemId;
	private final int _level;
	private final int _nextItemId;
	private final int _cursedNextItemId;
	
	public SoulCrystalTemplate(int itemId, int level, int nextItemId, int cursedNextItemId)
	{
		_itemId = itemId;
		_level = level;
		_nextItemId = nextItemId;
		_cursedNextItemId = cursedNextItemId;
	}
	
	public int getId()
	{
		return _itemId;
	}
	
	public int getLvl()
	{
		return _level;
	}
	
	public int getNextId()
	{
		return _nextItemId;
	}
	
	public int getCursedNextId()
	{
		return _cursedNextItemId;
	}
}