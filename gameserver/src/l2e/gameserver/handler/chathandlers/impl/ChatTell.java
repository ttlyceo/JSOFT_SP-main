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
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CreatureSay;

public class ChatTell implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
	        2
	};

	@Override
	public void handleChat(int type, Player activeChar, String target, String text, boolean blockBroadCast)
	{
		if (activeChar.isChatBanned() && ArrayUtils.contains(Config.BAN_CHAT_CHANNELS, type))
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
			return;
		}
		
		if (Config.JAIL_DISABLE_CHAT && activeChar.isJailed() && !activeChar.canOverrideCond(PcCondOverride.CHAT_CONDITIONS))
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_PROHIBITED);
			return;
		}
		
		if (target == null)
		{
			return;
		}
		
		final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(null), text);
		Player receiver = null;
		
		receiver = GameObjectsStorage.getPlayer(target);
		
		if (receiver != null && !receiver.isSilenceMode(activeChar.getObjectId()))
		{
			if (Config.JAIL_DISABLE_CHAT && receiver.isJailed() && !activeChar.canOverrideCond(PcCondOverride.CHAT_CONDITIONS))
			{
				activeChar.sendMessage("Player is in jail.");
				return;
			}
			if (receiver.isChatBanned())
			{
				activeChar.sendPacket(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
				return;
			}
			if ((receiver.getClient() == null || receiver.getClient().isDetached()))
			{
				if (!receiver.isFakePlayer())
				{
					activeChar.sendMessage("Player is in offline mode.");
					return;
				}
			}
			if (!receiver.getBlockList().isBlocked(activeChar))
			{
				if (Config.SILENCE_MODE_EXCLUDE && activeChar.isSilenceMode())
				{
					activeChar.addSilenceModeExcluded(receiver.getObjectId());
				}
				
				if (!blockBroadCast)
				{
					receiver.sendPacket(cs);
				}
				activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), type, "->" + receiver.getName(activeChar.getLang()), text));
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
		}
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}