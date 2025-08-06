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

import l2e.gameserver.model.actor.Player;

public class ExpCodeReward extends AbstractCodeReward
{
	private final long _value;
	private final String _icon;
	
	public ExpCodeReward(NamedNodeMap attr)
	{
		_value = Long.parseLong(attr.getNamedItem("val").getNodeValue());
		_icon = attr.getNamedItem("icon") != null ? attr.getNamedItem("icon").getNodeValue() : "";
	}
	
	@Override
	public void giveReward(Player player)
	{
		player.addExpAndSp(_value, 0);
	}
	
	@Override
	public String getIcon()
	{
		return _icon;
	}
	
	public long getExp()
	{
		return _value;
	}
}