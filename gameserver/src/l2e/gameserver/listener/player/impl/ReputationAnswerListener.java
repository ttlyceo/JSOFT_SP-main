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
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class ReputationAnswerListener implements OnAnswerListener
{
	private final Player _player;
		
	public ReputationAnswerListener(Player player)
	{
		_player = player;
	}
		
	@Override
	public void sayYes()
	{
		if (_player != null && _player.isOnline())
		{
			if (_player.getClan() != null)
			{
				if (_player.getInventory().getItemByItemId(Config.SERVICES_GIVEREP_ITEM[0]) == null)
				{
					_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (_player.getInventory().getItemByItemId(Config.SERVICES_GIVEREP_ITEM[0]).getCount() < Config.SERVICES_GIVEREP_ITEM[1])
				{
					_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				_player.destroyItemByItemId("GiveRepBBS", Config.SERVICES_GIVEREP_ITEM[0], Config.SERVICES_GIVEREP_ITEM[1], _player, true);
				Util.addServiceLog(_player.getName(null) + " buy reputation service!");
				_player.getClan().addReputationScore(Config.SERVICES_REP_COUNT, true);
				final ServerMessage msg = new ServerMessage("ServiceBBS.ADD_REP", _player.getLang());
				msg.add(String.valueOf(Config.SERVICES_REP_COUNT));
				_player.sendMessage(msg.toString());
			}
		}
	}
	
	@Override
	public void sayNo()
	{
	}
}