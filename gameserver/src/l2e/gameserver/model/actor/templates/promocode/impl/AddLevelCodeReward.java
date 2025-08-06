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
package l2e.gameserver.model.actor.templates.promocode.impl;

import org.w3c.dom.NamedNodeMap;

import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.model.actor.Player;

public class AddLevelCodeReward extends AbstractCodeReward
{
	private final int _level;
	private final String _icon;
	
	public AddLevelCodeReward(NamedNodeMap attr)
	{
		_level = Integer.parseInt(attr.getNamedItem("val").getNodeValue());
		_icon = attr.getNamedItem("icon") != null ? attr.getNamedItem("icon").getNodeValue() : "";
	}
	
	@Override
	public void giveReward(Player player)
	{
		if (player.getLevel() >= 85)
		{
			return;
		}
		
		int nextLevel = player.getLevel() + _level;
		if (nextLevel > 85)
		{
			nextLevel = 85;
		}
		final long pXp = player.getExp();
		final long tXp = ExperienceParser.getInstance().getExpForLevel(nextLevel);
		player.addExpAndSp(tXp - pXp, 0);
	}
	
	@Override
	public String getIcon()
	{
		return _icon;
	}
	
	public int getLevel()
	{
		return _level;
	}
}