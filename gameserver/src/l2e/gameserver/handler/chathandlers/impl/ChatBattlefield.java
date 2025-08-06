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
package l2e.gameserver.handler.chathandlers.impl;

import java.util.List;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.Config;
import l2e.gameserver.handler.chathandlers.IChatHandler;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CreatureSay;

public class ChatBattlefield implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
	        20
	};

	@Override
	public void handleChat(int type, Player activeChar, String target, String text, boolean blockBroadCast)
	{
		if (blockBroadCast)
		{
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(null), text));
			return;
		}
		
		if (activeChar.isInFightEvent())
		{
			final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(null), text);
			final List<Player> list = activeChar.getFightEvent().getMyTeamFightingPlayers(activeChar);
			for (final Player player : list)
			{
				player.sendPacket(cs);
			}
			return;
		}

		if (TerritoryWarManager.getInstance().isTWChannelOpen() && activeChar.getSiegeSide() > 0)
		{
			if (activeChar.isChatBanned() && ArrayUtils.contains(Config.BAN_CHAT_CHANNELS, type))
			{
				activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
				return;
			}
			
			final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(null), text);
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (player.getSiegeSide() == activeChar.getSiegeSide())
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}