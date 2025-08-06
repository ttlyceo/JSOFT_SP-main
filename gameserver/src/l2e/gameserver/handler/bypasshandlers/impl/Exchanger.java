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
package l2e.gameserver.handler.bypasshandlers.impl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.AugmentationParser;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.listener.player.impl.ExchangerAnswerListener;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public class Exchanger implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "exchanger", "selectitem", "selectoption", "transferitem", "putselectitem", "puttransferitem", "changeabilities"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		final int selectObject = player.getQuickVarI("selectObject", 0);
		final int transferObject = player.getQuickVarI("transferObject", 0);
		final int transferOption = player.getQuickVarI("transferOption", 0);
		
		switch (actualCommand.toLowerCase())
		{
			case "exchanger" :
				show(player);
				break;
			case "selectitem" :
				final List<ItemInstance> items = new ArrayList<>();
				for (final ItemInstance item : player.getInventory().getItems())
				{
					if (item != null)
					{
						if (transferOption == 0)
						{
							if (item.isAugmented())
							{
								items.add(item);
							}
						}
						else
						{
							if (item.getElementals() != null)
							{
								items.add(item);
							}
						}
					}
				}
				
				if (!items.isEmpty())
				{
					String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/exchanger/items.htm");
					final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/exchanger/template.htm");
					String block = "";
					String list = "";
					
					for (final ItemInstance item : items)
					{
						if (item != null)
						{
							block = template;
							block = block.replace("%bypass%", "bypass -h putSelectItem " + item.getObjectId() + "");
							block = block.replace("%name%", item.getName(player.getLang()));
							block = block.replace("%icon%", item.getItem().getIcon());
							list += block;
						}
					}
					html = html.replace("%list%", list);
					items.clear();
					Util.setHtml(html, player);
				}
				else
				{
					player.sendMessage((new ServerMessage("Exchanger.NO_ITEMS", player.getLang())).toString());
					show(player);
				}
				break;
			case "transferitem" :
				if (selectObject <= 0)
				{
					player.sendMessage((new ServerMessage("Exchanger.NOT_VALID", player.getLang())).toString());
					show(player);
					return false;
				}
				
				final var select = player.getInventory().getItemByObjectId(selectObject);
				
				final List<ItemInstance> itemss = new ArrayList<>();
				for (final ItemInstance item : player.getInventory().getItems())
				{
					if (item != null)
					{
						if (transferOption == 0)
						{
							if (isValid(player, item, select))
							{
								itemss.add(item);
							}
						}
						else
						{
							if (isElementValid(player, item, select))
							{
								itemss.add(item);
							}
						}
					}
				}
				
				if (!itemss.isEmpty())
				{
					String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/exchanger/items.htm");
					final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/exchanger/template.htm");
					String block = "";
					String list = "";
					
					for (final ItemInstance item : itemss)
					{
						if (item != null)
						{
							block = template;
							block = block.replace("%bypass%", "bypass -h putTransferItem " + item.getObjectId() + "");
							block = block.replace("%name%", item.getName(player.getLang()));
							block = block.replace("%icon%", item.getItem().getIcon());
							list += block;
						}
					}
					html = html.replace("%list%", list);
					itemss.clear();
					Util.setHtml(html, player);
				}
				else
				{
					player.sendMessage((new ServerMessage("Exchanger.NO_ITEMS", player.getLang())).toString());
					show(player);
				}
				break;
			case "selectoption" :
				final int selectt = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
				player.addQuickVar("transferOption", selectt);
				player.addQuickVar("selectObject", 0);
				player.addQuickVar("transferObject", 0);
				show(player);
				break;
			case "putselectitem" :
				final int selectItem = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
				if (selectItem > 0)
				{
					final var selecttt = player.getInventory().getItemByObjectId(selectItem);
					if (selecttt != null)
					{
						if (transferOption == 0)
						{
							if (selecttt.isAugmented())
							{
								player.addQuickVar("selectObject", selecttt.getObjectId());
							}
						}
						else
						{
							if (selecttt.getElementals() != null)
							{
								player.addQuickVar("selectObject", selecttt.getObjectId());
							}
						}
					}
					
					if (transferObject > 0)
					{
						player.addQuickVar("transferObject", selecttt.getObjectId());
					}
				}
				show(player);
				break;
			case "puttransferitem" :
				if (selectObject <= 0)
				{
					player.sendMessage((new ServerMessage("Exchanger.NOT_VALID", player.getLang())).toString());
					show(player);
					return false;
				}
				
				final var selecttt = player.getInventory().getItemByObjectId(selectObject);
				
				final int transferItem = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
				if (transferItem > 0)
				{
					final var transfer = player.getInventory().getItemByObjectId(transferItem);
					if (transfer != null)
					{
						if (transferOption == 0)
						{
							if (isValid(player, transfer, selecttt))
							{
								player.addQuickVar("transferObject", transfer.getObjectId());
							}
						}
						else
						{
							if (isElementValid(player, transfer, selecttt))
							{
								player.addQuickVar("transferObject", transfer.getObjectId());
							}
						}
					}
				}
				show(player);
				break;
			case "changeabilities" :
				if (selectObject <= 0 || transferObject <= 0)
				{
					player.sendMessage((new ServerMessage("Exchanger.NOT_VALID", player.getLang())).toString());
					show(player);
					return false;
				}
				
				final var msg = new ServerMessage(transferOption == 0 ? "Exchanger.EXCHANGE_AUGMENT" : "Exchanger.EXCHANGE_ELEMENTALS", player.getLang());
				player.sendConfirmDlg(new ExchangerAnswerListener(player), 15000, msg.toString());
				break;
		}
		return false;
	}
	
	private void show(Player player)
	{
		final int selectObject = player.getQuickVarI("selectObject", 0);
		final int transferObject = player.getQuickVarI("transferObject", 0);
		final int transferOption = player.getQuickVarI("transferOption", 0);
		
		String htm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/exchanger/index.htm");
		
		if (transferOption == 0)
		{
			htm = htm.replace("%augment_select%", "L2UI.CheckBox_checked");
			htm = htm.replace("%element_select%", "L2UI.CheckBox");
			htm = htm.replace("%augment_bypass%", "");
			htm = htm.replace("%element_bypass%", "bypass -h selectOption 1");
		}
		else
		{
			htm = htm.replace("%augment_select%", "L2UI.CheckBox");
			htm = htm.replace("%element_select%", "L2UI.CheckBox_checked");
			htm = htm.replace("%augment_bypass%", "bypass -h selectOption 0");
			htm = htm.replace("%element_bypass%", "");
		}
		
		if (selectObject > 0)
		{
			final var select = player.getInventory().getItemByObjectId(selectObject);
			if (select != null)
			{
				htm = htm.replace("%selectIcon%", select.getItem().getIcon());
			}
			else
			{
				htm = htm.replace("%selectIcon%", "icon.etc_question_mark_i00");
			}
		}
		else
		{
			htm = htm.replace("%selectIcon%", "icon.etc_question_mark_i00");
			player.addQuickVar("selectObject", 0);
		}
		
		if (transferObject > 0)
		{
			final var transfer = player.getInventory().getItemByObjectId(transferObject);
			if (transfer != null)
			{
				htm = htm.replace("%transferIcon%", transfer.getItem().getIcon());
			}
			else
			{
				htm = htm.replace("%transferIcon%", "icon.etc_question_mark_i00");
			}
		}
		else
		{
			htm = htm.replace("%transferIcon%", "icon.etc_question_mark_i00");
			player.addQuickVar("transferObject", 0);
		}
		
		final int[] price = transferOption == 0 ? Config.SERVICE_EXCHANGE_AUGMENT : Config.SERVICE_EXCHANGE_ELEMENTS;
		htm = htm.replace("%priceIcon%", Util.getItemIcon(price[0]));
		htm = htm.replace("%priceName%", Util.getItemName(player, price[0]));
		htm = htm.replace("%color%", player.getInventory().getItemByItemId(price[0]) == null || player.getInventory().getItemByItemId(price[0]).getCount() < price[1] ? "FF0000" : "00FF00");
		htm = htm.replace("%priceCount%", String.valueOf(NumberFormat.getNumberInstance(Locale.GERMAN).format(price[1])));
		
		Util.setHtml(htm, player);
	}
	
	private final boolean isValid(Player player, ItemInstance item, ItemInstance select)
	{
		if (!isValid(player))
		{
			return false;
		}
		
		if (item.getOwnerId() != player.getObjectId() || item.isAugmented() || item.isHeroItem() || item.isShadowItem() || item.isCommonItem() || item.isEtcItem() || item.isTimeLimitedItem())
		{
			return false;
		}
		
		if (item.isPvp() && !AugmentationParser.getInstance().getParams().getBool("allowAugmentationPvpItems"))
		{
			return false;
		}
		if (item.getItem().getCrystalType() < Item.CRYSTAL_C && !AugmentationParser.getInstance().getParams().getBool("allowAugmentationAllItemsGrade"))
		{
			return false;
		}
		
		switch (item.getItemLocation())
		{
			case INVENTORY :
			case PAPERDOLL :
				break;
			default :
				return false;
		}
		
		if (item.getItem() instanceof Weapon)
		{
			switch (((Weapon) item.getItem()).getItemType())
			{
				case NONE :
				case FISHINGROD :
					return false;
				default :
					break;
			}
			
			if (select != null && !select.isWeapon())
			{
				return false;
			}
		}
		else if (item.getItem() instanceof Armor)
		{
			switch (item.getItem().getBodyPart())
			{
				case Item.SLOT_LR_FINGER :
				case Item.SLOT_LR_EAR :
				case Item.SLOT_NECK :
					if (select != null && !select.getItem().isAccessory())
					{
						return false;
					}
					break;
				default :
					if (item.getItem().isAugmentable() && AugmentationParser.getInstance().isAllowArmorAugmentation())
					{
						if (select != null && !select.isArmor())
						{
							return false;
						}
						break;
					}
					return false;
			}
		}
		else
		{
			return false;
		}
		
		if (Arrays.binarySearch(AugmentationParser.getInstance().getForbiddenList(), item.getId()) >= 0)
		{
			return false;
		}
		return true;
	}
	
	private final boolean isValid(Player player)
	{
		if (player.getPrivateStoreType() != Player.STORE_PRIVATE_NONE || player.getActiveTradeList() != null || player.isDead() || player.isParalyzed() || player.isFishing() || player.isCursedWeaponEquipped() || player.isActionsDisabled() || player.isEnchanting() || player.isProcessingTransaction())
		{
			return false;
		}
		return true;
	}
	
	private final boolean isElementValid(Player player, ItemInstance item, ItemInstance select)
	{
		if (!isValid(player))
		{
			return false;
		}
		
		if (!item.isElementable() || item.getElementals() != null)
		{
			return false;
		}
		
		switch (item.getItemLocation())
		{
			case INVENTORY :
			case PAPERDOLL :
				break;
			default :
				return false;
		}
		
		if (item.getItem() instanceof Weapon)
		{
			switch (((Weapon) item.getItem()).getItemType())
			{
				case NONE :
				case FISHINGROD :
					return false;
				default :
					break;
			}
			
			if (select != null && !select.isWeapon())
			{
				return false;
			}
		}
		else if (item.getItem() instanceof Armor)
		{
			switch (item.getItem().getBodyPart())
			{
				case Item.SLOT_LR_FINGER :
				case Item.SLOT_LR_EAR :
				case Item.SLOT_NECK :
					if (select != null && !select.getItem().isAccessory())
					{
						return false;
					}
					break;
				default :
					if (select != null && !select.isArmor())
					{
						return false;
					}
					break;
			}
		}
		else
		{
			return false;
		}
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
