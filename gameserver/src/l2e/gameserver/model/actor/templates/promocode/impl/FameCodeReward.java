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
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class FameCodeReward extends AbstractCodeReward
{
	private final int _value;
	private final String _icon;
	
	public FameCodeReward(NamedNodeMap attr)
	{
		_value = Integer.parseInt(attr.getNamedItem("val").getNodeValue());
		_icon = attr.getNamedItem("icon") != null ? attr.getNamedItem("icon").getNodeValue() : "icon.pvp_point_i00";
	}
	
	@Override
	public void giveReward(Player player)
	{
		player.setFame(player.getFame() + _value);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
		sm.addNumber(_value);
		player.sendPacket(sm);
		player.sendUserInfo();
	}
	
	@Override
	public String getIcon()
	{
		return _icon;
	}
	
	public int getFame()
	{
		return _value;
	}
}