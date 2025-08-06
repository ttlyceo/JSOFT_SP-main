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
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;

public class ChangeColorAnswerListener implements OnAnswerListener
{
	private final Player _player;
	private final String _color;
	private final int _days;
	private final int _type;
		
	public ChangeColorAnswerListener(Player player, String color, int days, int type)
	{
		_player = player;
		_color = color;
		_days = days;
		_type = type;
	}
		
	@Override
	public void sayYes()
	{
		if (_player != null && _player.isOnline())
		{
			String colorh = new String("FFFFFF");
			if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.GREEN") + ""))
			{
				colorh = "00FF00";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.YELLOW") + ""))
			{
				colorh = "00FFFF";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.ORANGE") + ""))
			{
				colorh = "0099FF";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.BLUE") + ""))
			{
				colorh = "FF0000";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.BLACK") + ""))
			{
				colorh = "000000";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.BROWN") + ""))
			{
				colorh = "006699";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.LIGHT_PINK") + ""))
			{
				colorh = "FF66FF";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.PINK") + ""))
			{
				colorh = "FF00FF";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.LIGHT_BLUE") + ""))
			{
				colorh = "FFFF66";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.TURQUOSE") + ""))
			{
				colorh = "999900";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.LIME") + ""))
			{
				colorh = "99FF99";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.GRAY") + ""))
			{
				colorh = "999999";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.DARK_GREEN") + ""))
			{
				colorh = "339900";
			}
			else if (_color.equalsIgnoreCase("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.PURPLE") + ""))
			{
				colorh = "FF3399";
			}
			
			int itemId = 0;
			long amount = 0;
			boolean found = false;
			final long expireTime = System.currentTimeMillis() + (_days * 86400000L);
			switch (_type)
			{
				case 1 :
					for (final int day : Config.CHANGE_COLOR_NAME_LIST.keySet())
					{
						if (day == _days)
						{
							found = true;
							final String[] price = Config.CHANGE_COLOR_NAME_LIST.get(day).split(":");
							if (price != null && price.length == 2)
							{
								itemId = Integer.parseInt(price[0]);
								amount = Long.parseLong(price[1]);
							}
							break;
						}
					}
					
					if (found)
					{
						if (itemId != 0)
						{
							if (_player.getInventory().getItemByItemId(itemId) == null)
							{
								haveNoItems(_player, itemId, amount);
								final var cmd = CommunityBoardHandler.getInstance().getHandler("_bbs_service");
								if (cmd != null)
								{
									cmd.onBypassCommand("_bbs_service;nickcolor", _player);
								}
								return;
							}
							if (_player.getInventory().getItemByItemId(itemId).getCount() < amount)
							{
								haveNoItems(_player, itemId, amount);
								final var cmd = CommunityBoardHandler.getInstance().getHandler("_bbs_service");
								if (cmd != null)
								{
									cmd.onBypassCommand("_bbs_service;nickcolor", _player);
								}
								return;
							}
							_player.destroyItemByItemId("BBSColorName", itemId, amount, _player, false);
							Util.addServiceLog(_player.getName(null) + " buy color name service!");
						}
						final int curColor = Integer.decode("0x" + colorh);
						_player.setVar("namecolor", Integer.toString(curColor), expireTime);
						_player.getAppearance().setNameColor(curColor);
						_player.broadcastUserInfo(true);
						_player.sendMessage("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.CHANGE_NAME") + " " + _color);
					}
					break;
				case 2 :
					for (final int day : Config.CHANGE_COLOR_TITLE_LIST.keySet())
					{
						if (day == _days)
						{
							found = true;
							final String[] price = Config.CHANGE_COLOR_TITLE_LIST.get(day).split(":");
							if (price != null && price.length == 2)
							{
								itemId = Integer.parseInt(price[0]);
								amount = Long.parseLong(price[1]);
							}
							break;
						}
					}
					
					if (found)
					{
						if (itemId != 0)
						{
							if (_player.getInventory().getItemByItemId(itemId) == null)
							{
								haveNoItems(_player, itemId, amount);
								final var cmd = CommunityBoardHandler.getInstance().getHandler("_bbs_service");
								if (cmd != null)
								{
									cmd.onBypassCommand("_bbs_service;titlecolor", _player);
								}
								return;
							}
							if (_player.getInventory().getItemByItemId(itemId).getCount() < amount)
							{
								haveNoItems(_player, itemId, amount);
								final var cmd = CommunityBoardHandler.getInstance().getHandler("_bbs_service");
								if (cmd != null)
								{
									cmd.onBypassCommand("_bbs_service;titlecolor", _player);
								}
								return;
							}
							_player.destroyItemByItemId("BBSColorTitle", itemId, amount, _player, false);
							Util.addServiceLog(_player.getName(null) + " buy color title service!");
						}
						final int curColor1 = Integer.decode("0x" + colorh);
						_player.getAppearance().setTitleColor(curColor1);
						_player.setVar("titlecolor", Integer.toString(curColor1), expireTime);
						_player.broadcastUserInfo(true);
						_player.sendMessage("" + ServerStorage.getInstance().getString(_player.getLang(), "ServiceBBS.CHANGE_TITLE_COLOR") + " " + _color);
					}
					break;
			}
		}
	}
	
	@Override
	public void sayNo()
	{
	}
	
	private static void haveNoItems(Player player, int itemId, long amount)
	{
		final Item template = ItemsParser.getInstance().getTemplate(itemId);
		if (template != null)
		{
			final ServerMessage msg = new ServerMessage("Enchant.NEED_ITEMS", player.getLang());
			msg.add(amount);
			msg.add(template.getName(player.getLang()));
			player.sendMessage(msg.toString());
		}
	}
}