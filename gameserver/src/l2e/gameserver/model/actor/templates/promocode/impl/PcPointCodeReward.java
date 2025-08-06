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

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class PcPointCodeReward extends AbstractCodeReward
{
	private int _value;
	private final String _icon;
	
	public PcPointCodeReward(NamedNodeMap attr)
	{
		_value = Integer.parseInt(attr.getNamedItem("val").getNodeValue());
		_icon = attr.getNamedItem("icon") != null ? attr.getNamedItem("icon").getNodeValue() : "icon.etc_pccafe_point_i00";
	}
	
	@Override
	public void giveReward(Player player)
	{
		if ((player.getPcBangPoints() + _value) > Config.MAX_PC_BANG_POINTS)
		{
			_value = Config.MAX_PC_BANG_POINTS - player.getPcBangPoints();
		}
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
		sm.addNumber(_value);
		player.sendPacket(sm);
		player.setPcBangPoints(player.getPcBangPoints() + _value);
		player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), _value, true, false, 1));
	}
	
	@Override
	public String getIcon()
	{
		return _icon;
	}
	
	public int getPcPoints()
	{
		return _value;
	}
}