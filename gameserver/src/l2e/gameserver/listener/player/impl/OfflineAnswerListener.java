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

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.mods.OfflineTaskManager;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class OfflineAnswerListener implements OnAnswerListener
{
	private final Player _player;
		
	public OfflineAnswerListener(Player player)
	{
		_player = player;
	}
		
	@Override
	public void sayYes()
	{
		if (_player != null && _player.isOnline())
		{
			if (Config.OFFLINE_MODE_PRICE[0] > 0 && !OfflineTaskManager.getInstance().isActivePlayer(_player))
			{
				if (_player.getInventory().getItemByItemId(Config.OFFLINE_MODE_PRICE[0]) == null || _player.getInventory().getItemByItemId(Config.OFFLINE_MODE_PRICE[0]).getCount() < Config.OFFLINE_MODE_PRICE[1])
				{
					_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				_player.destroyItemByItemId("OfflineMode", Config.OFFLINE_MODE_PRICE[0], Config.OFFLINE_MODE_PRICE[1], _player, true);
				OfflineTaskManager.getInstance().addOfflinePlayer(_player, 0, true);
			}
			_player.setInOfflineMode(true);
		}
	}
	
	@Override
	public void sayNo()
	{
		if (_player != null && _player.isOnline())
		{
			_player.setInOfflineMode(false);
		}
	}
}