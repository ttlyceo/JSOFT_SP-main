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

import l2e.gameserver.instancemanager.CoupleManager;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;

public class EngageAnswerListener implements OnAnswerListener
{
	private final Player _player;
		
	public EngageAnswerListener(Player player)
	{
		_player = player;
	}
		
	@Override
	public void sayYes()
	{
		if (_player != null && _player.isOnline())
		{
			if (!_player.isEngageRequest() || _player.getEngageId() == 0)
			{
				return;
			}
			final Player partner = GameObjectsStorage.getPlayer(_player.getEngageId());
			if (partner != null)
			{
				CoupleManager.getInstance().createCouple(partner, _player);
				partner.sendMessage("Request to Engage has been >ACCEPTED<");
			}
			_player.setEngageRequest(false, 0);
		}
	}
	@Override
	public void sayNo()
	{
		if (_player != null && _player.isOnline())
		{
			if (!_player.isEngageRequest() || _player.getEngageId() == 0)
			{
				return;
			}
			final Player partner = GameObjectsStorage.getPlayer(_player.getEngageId());
			if (partner != null)
			{
				partner.sendMessage("Request to Engage has been >DENIED<!");
			}
			_player.setEngageRequest(false, 0);
		}
	}
}