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

import static l2e.gameserver.model.items.itemcontainer.PcInventory.MAX_ADENA;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.StringUtil;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.parser.AdminParser;
import l2e.gameserver.instancemanager.MailManager;
import l2e.gameserver.model.AccessLevel;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Message;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Mail;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExNoticePostSent;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestSendPost extends GameClientPacket
{
	private static final int BATCH_LENGTH = 12;
	
	private static final int MAX_RECV_LENGTH = 16;
	private static final int MAX_SUBJ_LENGTH = 128;
	private static final int MAX_TEXT_LENGTH = 512;
	private static final int MAX_ATTACHMENTS = 8;
	private static final int INBOX_SIZE = 240;
	private static final int OUTBOX_SIZE = 240;

	private String _receiver;
	private boolean _isCod;
	private String _subject;
	private String _text;
	private int _count;
	private int[] _items;
	private long[] _itemQ;
	private long _reqAdena;

	public RequestSendPost()
	{
	}

	@Override
	protected void readImpl()
	{
		_receiver = readS();
		_isCod = readD() == 0 ? false : true;
		_subject = readS();
		_text = readS();

		_count = readD();
		if ((_count < 1) || (_count > Config.MAX_ITEM_IN_PACKET) || (((_count * BATCH_LENGTH) + 8) != _buf.remaining()))
		{
			_count = 0;
			return;
		}
		_items = new int[_count];
		_itemQ = new long[_count];

		for (int i = 0; i < _count; i++)
		{
			_items[i] = readD();
			_itemQ[i] = readQ();
			if (_itemQ[i] < 1 || ArrayUtils.indexOf(_items, _items[i]) < i)
			{
				_count = 0;
				return;
			}
		}
		
		_reqAdena = readQ();
		if (_reqAdena < 0)
		{
			_count = 0;
			_reqAdena = 0;
		}
	}

	@Override
	public void runImpl()
	{
		if (!Config.ALLOW_MAIL)
		{
			return;
		}

		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		if (!Config.ALLOW_ATTACHMENTS)
		{
			_items = null;
			_isCod = false;
			_reqAdena = 0;
		}
		
		if (activeChar.getLevel() < Config.MAIL_MIN_LEVEL)
		{
			final var msg = new ServerMessage("Mail.WRONG_LEVEL", activeChar.getLang());
			msg.add(Config.MAIL_MIN_LEVEL);
			activeChar.sendMessage(msg.toString());
			return;
		}

		if (!activeChar.getAccessLevel().allowTransaction())
		{
			activeChar.sendMessage("Transactions are disabled for your Access Level.");
			return;
		}
		
		if (activeChar.isActionsDisabled())
		{
			activeChar.sendActionFailed();
			return;
		}

		if (!activeChar.isInZonePeace() && (_items != null))
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_NOT_IN_PEACE_ZONE);
			return;
		}

		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_DURING_EXCHANGE);
			return;
		}

		if (activeChar.isEnchanting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_DURING_ENCHANT);
			return;
		}

		if (activeChar.getPrivateStoreType() > Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_PRIVATE_STORE);
			return;
		}

		if (_receiver.length() > MAX_RECV_LENGTH)
		{
			activeChar.sendPacket(SystemMessageId.ALLOWED_LENGTH_FOR_RECIPIENT_EXCEEDED);
			return;
		}

		if (_subject.length() > MAX_SUBJ_LENGTH)
		{
			activeChar.sendPacket(SystemMessageId.ALLOWED_LENGTH_FOR_TITLE_EXCEEDED);
			return;
		}

		if (_text.length() > MAX_TEXT_LENGTH)
		{
			activeChar.sendPacket(SystemMessageId.ALLOWED_LENGTH_FOR_TITLE_EXCEEDED);
			return;
		}

		if ((_items != null) && (_items.length > MAX_ATTACHMENTS))
		{
			activeChar.sendPacket(SystemMessageId.ITEM_SELECTION_POSSIBLE_UP_TO_8);
			return;
		}

		if ((_reqAdena < 0) || (_reqAdena > MAX_ADENA))
		{
			return;
		}

		if (_isCod)
		{
			if (_reqAdena == 0)
			{
				activeChar.sendPacket(SystemMessageId.PAYMENT_AMOUNT_NOT_ENTERED);
				return;
			}
			if ((_items == null) || (_items.length == 0))
			{
				activeChar.sendPacket(SystemMessageId.PAYMENT_REQUEST_NO_ITEM);
				return;
			}
		}

		final int receiverId = CharNameHolder.getInstance().getIdByName(_receiver);
		if (receiverId <= 0)
		{
			activeChar.sendPacket(SystemMessageId.RECIPIENT_NOT_EXIST);
			return;
		}

		if (receiverId == activeChar.getObjectId())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANT_SEND_MAIL_TO_YOURSELF);
			return;
		}

		final int level = CharNameHolder.getInstance().getAccessLevelById(receiverId);
		final AccessLevel accessLevel = AdminParser.getInstance().getAccessLevel(level);

		if (accessLevel.isGm() && !activeChar.getAccessLevel().isGm())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CANNOT_MAIL_GM_C1);
			sm.addString(_receiver);
			activeChar.sendPacket(sm);
			return;
		}

		if (activeChar.isJailed() && ((_items != null) || Config.JAIL_DISABLE_CHAT))
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_NOT_IN_PEACE_ZONE);
			return;
		}

		if (activeChar.getBlockList().isInBlockList(receiverId, activeChar.getObjectId()))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_BLOCKED_YOU_CANNOT_MAIL);
			sm.addString(_receiver);
			activeChar.sendPacket(sm);
			return;
		}

		if (MailManager.getInstance().getOutboxSize(activeChar.getObjectId()) >= OUTBOX_SIZE)
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_MAIL_LIMIT_EXCEEDED);
			return;
		}

		if (MailManager.getInstance().getInboxSize(receiverId) >= INBOX_SIZE)
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_MAIL_LIMIT_EXCEEDED);
			return;
		}

		final Message msg = new Message(activeChar.getObjectId(), receiverId, _isCod, _subject, _text, _reqAdena);
		if (removeItems(activeChar, msg))
		{
			MailManager.getInstance().sendMessage(msg);
			activeChar.sendPacket(ExNoticePostSent.valueOf(true));
			activeChar.sendPacket(SystemMessageId.MAIL_SUCCESSFULLY_SENT);
		}
	}

	private final boolean removeItems(Player player, Message msg)
	{
		final long serviceCost = 100 + _count * 1000;
		if (_count > 0)
		{
			for (int i = 0; i < _count; i++)
			{
				final ItemInstance item = player.getInventory().getItemByObjectId(_items[i]);
				if (item == null || item.getCount() < _itemQ[i] || (item.getId() == 57 && item.getCount() < _itemQ[i] + serviceCost) || !item.isTradeable() || item.isEquipped())
				{
					player.sendPacket(SystemMessageId.CANT_FORWARD_BAD_ITEM);
					return false;
				}
			}
		}
		
		if (!player.reduceAdena("MailFee", serviceCost, null, false))
		{
			player.sendPacket(SystemMessageId.CANT_FORWARD_NO_ADENA);
			return false;
		}

		if (_count == 0)
		{
			return true;
		}

		final Mail attachments = msg.createAttachments();

		if (attachments == null)
		{
			return false;
		}

		final StringBuilder recv = new StringBuilder(32);
		StringUtil.append(recv, msg.getReceiverName(), "[", String.valueOf(msg.getReceiverId()), "]");
		final String receiver = recv.toString();

		final InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (int i = 0; i < _count; i++)
		{
			final ItemInstance oldItem = player.checkItemManipulation(_items[i], _itemQ[i], "attach");
			if ((oldItem == null) || !oldItem.isTradeable() || oldItem.isEquipped())
			{
				_log.warn("Error adding attachment for char " + player.getName(null) + " (olditem == null) ID: " + _items[i] + " count: " + _itemQ[i]);
				continue;
			}

			final ItemInstance newItem = player.getInventory().transferItem("SendMail", _items[i], _itemQ[i], attachments, player, receiver);
			if (newItem == null)
			{
				_log.warn("Error adding attachment for char " + player.getName(null) + " (newitem == null) ID: " + _items[i] + " count: " + _itemQ[i]);
				continue;
			}
			newItem.setItemLocation(newItem.getItemLocation(), msg.getId());

			if (playerIU != null)
			{
				if ((oldItem.getCount() > 0) && (oldItem != newItem))
				{
					playerIU.addModifiedItem(oldItem);
				}
				else
				{
					playerIU.addRemovedItem(oldItem);
				}
			}
		}

		if (playerIU != null)
		{
			player.sendPacket(playerIU);
		}
		else
		{
			player.sendItemList(false);
		}
		player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
		return true;
	}

	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}