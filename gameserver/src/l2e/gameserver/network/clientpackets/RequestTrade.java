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

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.BotReportParser;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.TradeRequest;

public final class RequestTrade extends GameClientPacket
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
		final Player player = getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.isInFightEvent() && !player.getFightEvent().canOpenStore(player))
		{
			return;
		}

		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your current Access Level.");
			sendActionFailed();
			return;
		}
		
		Effect ef = null;
		if (((ef = player.getFirstEffect(EffectType.ACTION_BLOCK)) != null) && !ef.checkCondition(BotReportParser.TRADE_ACTION_BLOCK_ID))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_SO_ACTIONS_NOT_ALLOWED);
			player.sendActionFailed();
			return;
		}
		
		final Player partner = GameObjectsStorage.getPlayer(_objectId);
		if ((partner == null) || !World.getInstance().getAroundPlayers(player).contains(partner) || ((partner.getReflectionId() != player.getReflectionId())))
		{
			return;
		}
		
		if (partner.getObjectId() == player.getObjectId())
		{
			player.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return;
		}
		
		if (partner.isInOlympiadMode() || player.isInOlympiadMode())
		{
			player.sendMessage("A user currently participating in the Olympiad cannot accept or request a trade.");
			return;
		}
		
		if (((ef = partner.getFirstEffect(EffectType.ACTION_BLOCK)) != null) && !ef.checkCondition(BotReportParser.TRADE_ACTION_BLOCK_ID))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_REPORTED_AND_IS_BEING_INVESTIGATED);
			sm.addCharName(partner);
			player.sendPacket(sm);
			player.sendActionFailed();
			return;
		}
		
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TRADE && (player.getKarma() > 0))
		{
			player.sendMessage("You cannot trade while you are in a chaotic state.");
			return;
		}
		
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TRADE && (partner.getKarma() > 0))
		{
			player.sendMessage("You cannot request a trade while your target is in a chaotic state.");
			return;
		}
		
		if (player.isJailed() || partner.isJailed())
		{
			player.sendMessage("You cannot trade while you are in in Jail.");
			return;
		}
		
		if ((player.getPrivateStoreType() != Player.STORE_PRIVATE_NONE) || (partner.getPrivateStoreType() != Player.STORE_PRIVATE_NONE))
		{
			player.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}
		
		if (player.isProcessingTransaction())
		{
			if (Config.DEBUG)
			{
				_log.info("Already trading with someone else.");
			}
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}
		
		SystemMessage sm;
		if (partner.isProcessingRequest() || partner.isProcessingTransaction())
		{
			if (Config.DEBUG)
			{
				_log.info("Transaction already in progress.");
			}
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
			sm.addString(partner.getName(null));
			player.sendPacket(sm);
			return;
		}
		
		if (partner.getTradeRefusal())
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER).addString(partner.getName(null)));
			return;
		}
		
		if (partner.getBlockList().isBlocked(player))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
			sm.addCharName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (Util.calculateDistance(player, partner, true) > 150)
		{
			player.sendPacket(SystemMessageId.TARGET_TOO_FAR);
			return;
		}
		
		if (player.isSameAddress(partner) && (player.isUseAutoTrade() && partner.isUseAutoTrade()))
		{
			player.startTrade(partner);
		}
		else
		{
			player.onTransactionRequest(partner);
			partner.sendPacket(new TradeRequest(player.getObjectId()));
			sm = SystemMessage.getSystemMessage(SystemMessageId.REQUEST_C1_FOR_TRADE);
			sm.addString(partner.getName(null));
			player.sendPacket(sm);
		}
	}
}