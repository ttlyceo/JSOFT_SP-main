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

import l2e.gameserver.model.base.ShortcutType;

public class ShortCutTemplate
{
	private final int _slot;
	private final int _page;
	private final ShortcutType _type;
	private final int _id;
	private final int _level;
	private final int _characterType;
	private int _sharedReuseGroup = -1;
	private int _currentReuse = 0;
	private int _reuse = 0;
	private int _augmentationId = 0;
	
	public ShortCutTemplate(int slotId, int pageId, ShortcutType type, int shortcutId, int shortcutLevel, int characterType)
	{
		_slot = slotId;
		_page = pageId;
		_type = type;
		_id = shortcutId;
		_level = shortcutLevel;
		_characterType = characterType;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getPage()
	{
		return _page;
	}
	
	public int getSlot()
	{
		return _slot;
	}
	
	public ShortcutType getType()
	{
		return _type;
	}
	
	public int getCharacterType()
	{
		return _characterType;
	}
	
	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}
	
	public void setSharedReuseGroup(int g)
	{
		_sharedReuseGroup = g;
	}
	
	public void setCurrenReuse(int reuse)
	{
		_currentReuse = reuse;
	}
	
	public int getCurrenReuse()
	{
		return _currentReuse;
	}
	
	public void setReuse(int reuse)
	{
		_reuse = reuse;
	}
	
	public int getReuse()
	{
		return _reuse;
	}

	public void setAugmentationId(int augmentationId)
	{
		_augmentationId = augmentationId;
	}
	
	public int getAugmentationId()
	{
		return _augmentationId;
	}
}