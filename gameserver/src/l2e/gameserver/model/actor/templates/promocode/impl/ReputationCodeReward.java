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

public class ReputationCodeReward extends AbstractCodeReward
{
	private final int _value;
	private final String _icon;
	
	public ReputationCodeReward(NamedNodeMap attr)
	{
		_value = Integer.parseInt(attr.getNamedItem("val").getNodeValue());
		_icon = attr.getNamedItem("icon") != null ? attr.getNamedItem("icon").getNodeValue() : "icon.skill0390";
	}
	
	@Override
	public void giveReward(Player player)
	{
		if (player.getClan() != null)
		{
			player.getClan().addReputationScore(_value, true);
		}
	}
	
	@Override
	public String getIcon()
	{
		return _icon;
	}
	
	public int getReputation()
	{
		return _value;
	}
}