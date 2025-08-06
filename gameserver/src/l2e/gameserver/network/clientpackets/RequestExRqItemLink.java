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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.Config;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.serverpackets.ExRpItemLink;

public class RequestExRqItemLink extends GameClientPacket
{
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		if (client != null)
		{
			final ItemInstance item = GameObjectsStorage.getItem(_objectId);
			if (item != null)
			{
				if (item.isPublished())
				{
					client.sendPacket(new ExRpItemLink(item));
				}
				else
				{
					if (Config.DEBUG)
					{
						_log.info(getClient() + " requested item link for item which wasnt published! ID:" + _objectId);
					}
				}
			}
			else
			{
				client.getActiveChar().getListeners().onQuestionMarkClicked(_objectId);
			}
		}
	}
}