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
package l2e.gameserver.listener.player.impl;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class NooblesAnswerListener implements OnAnswerListener
{
	private final Player _player;
		
	public NooblesAnswerListener(Player player)
	{
		_player = player;
	}
		
	@Override
	public void sayYes()
	{
		if (_player != null && _player.isOnline())
		{
			if (_player.getInventory().getItemByItemId(Config.SERVICES_GIVENOOBLESS_ITEM[0]) == null)
			{
				_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return;
			}
			if (_player.getInventory().getItemByItemId(Config.SERVICES_GIVENOOBLESS_ITEM[0]).getCount() < Config.SERVICES_GIVENOOBLESS_ITEM[1])
			{
				_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return;
			}
			if (_player.isNoble())
			{
				_player.sendMessage((new ServerMessage("ServiceBBS.NOOBLES_MSG_1", _player.getLang())).toString());
				return;
			}
			
			_player.destroyItemByItemId("NooblesBBS", Config.SERVICES_GIVENOOBLESS_ITEM[0], Config.SERVICES_GIVENOOBLESS_ITEM[1], _player, true);
			Util.addServiceLog(_player.getName(null) + " buy Noobless!");
			Olympiad.addNoble(_player);
			_player.setNoble(true);
			if (_player.getClan() != null)
			{
				_player.setPledgeClass(ClanMember.calculatePledgeClass(_player));
			}
			else
			{
				_player.setPledgeClass(5);
			}
			_player.sendUserInfo();
		}
	}
	
	@Override
	public void sayNo()
	{
	}
}