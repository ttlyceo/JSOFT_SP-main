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

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.Config;
import l2e.gameserver.handler.chathandlers.IChatHandler;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CreatureSay;

public class ChatShout implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
	        1
	};

	@Override
	public void handleChat(int type, Player activeChar, String target, String text, boolean blockBroadCast)
	{
		if (activeChar.isChatBanned() && ArrayUtils.contains(Config.BAN_CHAT_CHANNELS, type))
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
			return;
		}
		
		if (activeChar.getLevel() < Config.MIN_LVL_GLOBAL_CHAT)
		{
			final ServerMessage msg = new ServerMessage("GlobatChat.LOW_LEVEL", activeChar.getLang());
			msg.add(Config.MIN_LVL_GLOBAL_CHAT);
			activeChar.sendMessage(msg.toString());
			return;
		}
		
		final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(null), text);
		if (blockBroadCast)
		{
			activeChar.sendPacket(cs);
			return;
		}
		
		if (Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("on") || (Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("gm") && activeChar.canOverrideCond(PcCondOverride.CHAT_CONDITIONS)))
		{
			final int region = MapRegionManager.getInstance().getMapRegionLocId(activeChar);
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (region == MapRegionManager.getInstance().getMapRegionLocId(player) && !player.getBlockList().isBlocked(activeChar) && player.getReflectionId() == activeChar.getReflectionId())
				{
					player.sendPacket(cs);
				}
			}
		}
		else if (Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("global"))
		{
			if (!activeChar.canOverrideCond(PcCondOverride.CHAT_CONDITIONS) && !activeChar.checkFloodProtection("GLOBALCHAT", "global_chat"))
			{
				activeChar.sendMessage("Do not spam shout channel.");
				return;
			}
			
			for (final Player player : GameObjectsStorage.getPlayers())
			{
				if (!player.getBlockList().isBlocked(activeChar))
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