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

import l2e.commons.log.Log;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.handler.chathandlers.ChatHandler;
import l2e.gameserver.handler.chathandlers.IChatHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.CreatureSay;

public final class Say2 extends GameClientPacket
{
	public static final int ALL = 0;
	public static final int SHOUT = 1;
	public static final int TELL = 2;
	public static final int PARTY = 3;
	public static final int CLAN = 4;
	public static final int GM = 5;
	public static final int PETITION_PLAYER = 6;
	public static final int PETITION_GM = 7;
	public static final int TRADE = 8;
	public static final int ALLIANCE = 9;
	public static final int ANNOUNCEMENT = 10;
	public static final int BOAT = 11;
	public static final int L2FRIEND = 12;
	public static final int MSNCHAT = 13;
	public static final int PARTYMATCH_ROOM = 14;
	public static final int PARTYROOM_COMMANDER = 15;
	public static final int PARTYROOM_ALL = 16;
	public static final int HERO_VOICE = 17;
	public static final int CRITICAL_ANNOUNCE = 18;
	public static final int SCREEN_ANNOUNCE = 19;
	public static final int BATTLEFIELD = 20;
	public static final int MPCC_ROOM = 21;
	public static final int NPC_ALL = 22;
	public static final int NPC_SHOUT = 23;
	
	private static final String[] CHAT_NAMES =
	{
	        "ALL", "SHOUT", "TELL", "PARTY", "CLAN", "GM", "PETITION_PLAYER", "PETITION_GM", "TRADE", "ALLIANCE", "ANNOUNCEMENT", "BOAT", "L2FRIEND", "MSNCHAT", "PARTYMATCH_ROOM", "PARTYROOM_COMMANDER", "PARTYROOM_ALL", "HERO_VOICE", "CRITICAL_ANNOUNCE", "SCREEN_ANNOUNCE", "BATTLEFIELD", "MPCC_ROOM"
	};
	
	private static final String[] WALKER_COMMAND_LIST =
	{
	        "USESKILL", "USEITEM", "BUYITEM", "SELLITEM", "SAVEITEM", "LOADITEM", "MSG", "DELAY", "LABEL", "JMP", "CALL", "RETURN", "MOVETO", "NPCSEL", "NPCDLG", "DLGSEL", "CHARSTATUS", "POSOUTRANGE", "POSINRANGE", "GOHOME", "SAY", "EXIT", "PAUSE", "STRINDLG", "STRNOTINDLG", "CHANGEWAITTYPE", "FORCEATTACK", "ISMEMBER", "REQUESTJOINPARTY", "REQUESTOUTPARTY", "QUITPARTY", "MEMBERSTATUS", "CHARBUFFS", "ITEMCOUNT", "FOLLOWTELEPORT"
	};
	
	private String _text;
	private int _type;
	private String _target;
	
	@Override
	protected void readImpl()
	{
		_text = readS();
		_type = readD();
		_target = (_type == TELL) ? readS() : null;
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.DEBUG)
		{
			_log.info("Say2: Msg Type = '" + _type + "' Text = '" + _text + "'.");
		}
		
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		activeChar.isntAfk();
		
		if ((_type < 0) || (_type >= CHAT_NAMES.length))
		{
			_log.warn("Say2: Invalid type: " + _type + " Player : " + activeChar.getName(null) + " text: " + String.valueOf(_text));
			activeChar.sendActionFailed();
			activeChar.logout();
			return;
		}
		
		if (_text.isEmpty())
		{
			_log.warn(activeChar.getName(null) + ": sending empty text. Possible packet hack!");
			activeChar.sendActionFailed();
			activeChar.logout();
			return;
		}
		
		if (!activeChar.isGM() && (((_text.indexOf(8) >= 0) && (_text.length() > 500)) || ((_text.indexOf(8) < 0) && (_text.length() > 105))))
		{
			activeChar.sendPacket(SystemMessageId.DONT_SPAM);
			return;
		}
		
		if ((_type == TELL) && checkBot(_text))
		{
			Util.handleIllegalPlayerAction(activeChar, "Client Emulator Detect: " + activeChar.getName(null) + " using l2walker.");
			return;
		}
		
		if (activeChar.isCursedWeaponEquipped() && ((_type == TRADE) || (_type == SHOUT)))
		{
			activeChar.sendPacket(SystemMessageId.SHOUT_AND_TRADE_CHAT_CANNOT_BE_USED_WHILE_POSSESSING_CURSED_WEAPON);
			return;
		}
		
		if (activeChar.isChatBanned() && (_text.charAt(0) != '.'))
		{
			if (activeChar.getFirstEffect(EffectType.CHAT_BLOCK) != null)
			{
				activeChar.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_SO_CHATTING_NOT_ALLOWED);
				return;
			}
			else
			{
				for (final int chatId : Config.BAN_CHAT_CHANNELS)
				{
					if (_type == chatId)
					{
						activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
						return;
					}
				}
			}
		}
		
		if (activeChar.isJailed() && Config.JAIL_DISABLE_CHAT)
		{
			if ((_type == TELL) || (_type == SHOUT) || (_type == TRADE) || (_type == HERO_VOICE))
			{
				activeChar.sendMessage("You can not chat with players outside of the jail.");
				return;
			}
		}
		
		if ((_type == PETITION_PLAYER) && activeChar.isGM())
		{
			_type = PETITION_GM;
		}
		
		if (Config.LOG_CHAT)
		{
			Log.AddLogChat(_type, activeChar.getName(null), _target, _text);
		}
		
		if (_text.indexOf(8) >= 0)
		{
			if (!parseAndPublishItem(activeChar))
			{
				return;
			}
		}
		
		final boolean blockBroadCast = checkBroadCastText();
		if (Config.USE_SAY_FILTER && !blockBroadCast)
		{
			checkText();
		}
		
		if (_text.charAt(0) == '-' && Config.ALLOW_CUSTOM_CHAT && _type == ALL)
		{
			if (activeChar.getCustomChatStatus() < Config.CHECK_CHAT_VALID)
			{
				activeChar.sendMessage((new ServerMessage("CustomChat.CANT_USE", activeChar.getLang())).toString());
				return;
			}
			
			if (activeChar.getChatMsg() == 0)
			{
				activeChar.sendMessage((new ServerMessage("CustomChat.LIMIT", activeChar.getLang())).toString());
				return;
			}
			
			final String text = _text.substring(1);
			if (blockBroadCast)
			{
				activeChar.sendPacket(new CreatureSay(0, Say2.BATTLEFIELD, activeChar.getName(null), text));
			}
			else
			{
				for (final Player player : GameObjectsStorage.getPlayers())
				{
					player.sendPacket(new CreatureSay(0, Say2.BATTLEFIELD, activeChar.getName(null), text));
				}
			}
			activeChar.setChatMsg(activeChar.getChatMsg() - 1);
			activeChar.getListeners().onChatMessageReceive(_type, activeChar.getName(null), _target, _text);
			return;
		}
		
		final IChatHandler handler = ChatHandler.getInstance().getHandler(_type);
		if (handler != null)
		{
			handler.handleChat(_type, activeChar, _target, _text, blockBroadCast);
			activeChar.getListeners().onChatMessageReceive(_type, activeChar.getName(null), _target, _text);
		}
		else
		{
			_log.info("No handler registered for ChatType: " + _type + " Player: " + getClient());
		}
	}
	
	private boolean checkBot(String text)
	{
		for (final String botCommand : WALKER_COMMAND_LIST)
		{
			if (text.startsWith(botCommand))
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean checkBroadCastText()
	{
		if (!Config.USE_BROADCAST_SAY_FILTER)
		{
			return false;
		}
		
		for (final String pattern : Config.BROADCAST_FILTER_LIST)
		{
			final int index = _text.indexOf(pattern);
			if (index != -1)
			{
				return true;
			}
		}
		return false;
	}
	
	private void checkText()
	{
		String filteredText = _text;
		for (final String pattern : Config.FILTER_LIST)
		{
			filteredText = filteredText.replaceAll("(?i)" + pattern, Config.CHAT_FILTER_CHARS);
		}
		_text = filteredText;
	}
	
	private boolean parseAndPublishItem(Player owner)
	{
		int pos1 = -1;
		while ((pos1 = _text.indexOf(8, pos1)) > -1)
		{
			int pos = _text.indexOf("ID=", pos1);
			if (pos == -1)
			{
				return false;
			}
			final StringBuilder result = new StringBuilder(9);
			pos += 3;
			while (Character.isDigit(_text.charAt(pos)))
			{
				result.append(_text.charAt(pos++));
			}
			final int id = Integer.parseInt(result.toString());
			final ItemInstance item = GameObjectsStorage.getItem(id);
			if (item != null)
			{
				if (owner.getInventory().getItemByObjectId(id) == null)
				{
					_log.info(getClient() + " trying publish item which doesnt own! ID:" + id);
					return false;
				}
				item.publish();
			}
			else
			{
				_log.info(getClient() + " trying publish object which is not item! Object:" + item);
				return false;
			}
			pos1 = _text.indexOf(8, pos) + 1;
			if (pos1 == 0)
			{
				_log.info(getClient() + " sent invalid publish item msg! ID:" + id);
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}