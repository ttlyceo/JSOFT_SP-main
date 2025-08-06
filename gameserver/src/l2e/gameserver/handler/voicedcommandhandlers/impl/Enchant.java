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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.service.autoenchant.EnchantManager;
import l2e.gameserver.model.service.autoenchant.EnchantUtils;
import l2e.gameserver.model.strings.server.ServerMessage;

public class Enchant implements IVoicedCommandHandler
{
	private static String[] commands =
	{
	        "enchant", "max_enchant", "item_limit", "common_for_safe", "begin_enchant", "item_choose", "item_change", "enchant_help"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if (player == null || !Config.ALLOW_ENCHANT_SERVICE)
		{
			return false;
		}
		
		if (command.equals("enchant"))
		{
			EnchantManager.getInstance().showMainPage(player);
		}
		else if (command.equals("max_enchant"))
		{
			if (params == null || params.isEmpty())
			{
				player.getEnchantParams().isChangingMaxEnchant = true;
			}
			else
			{
				try
				{
					int userMax = Integer.parseInt(params.split("-")[0]);
					final ItemInstance enchant = player.getEnchantParams().upgradeItem;
					final ItemInstance targetItem = player.getEnchantParams().targetItem;
					if (!EnchantUtils.getInstance().isAttribute(enchant))
					{
						int configMax;
						if (targetItem.isWeapon())
						{
							configMax = Config.ENCHANT_MAX_WEAPON;
						}
						else if (targetItem.isArmor())
						{
							configMax = Config.ENCHANT_MAX_ARMOR;
						}
						else if (targetItem.isJewel())
						{
							configMax = Config.ENCHANT_MAX_JEWELRY;
						}
						else
						{
							return false;
						}
						if (userMax < 1)
						{
							userMax = 1;
						}
						if (userMax > configMax)
						{
							userMax = configMax;
						}
						player.getEnchantParams().maxEnchant = userMax;
						player.getEnchantParams().isChangingMaxEnchant = false;
					}
					else
					{
						if (userMax < 1)
						{
							userMax = 1;
						}
						if (targetItem.isJewel())
						{
							return false;
						}
						if (targetItem.isArmor())
						{
							if (userMax > 120)
							{
								userMax = 120;
							}
						}
						if (targetItem.isWeapon())
						{
							if (userMax > 300)
							{
								userMax = 300;
							}
						}
						player.getEnchantParams().maxEnchantAtt = userMax;
						player.getEnchantParams().isChangingMaxEnchant = false;
					}
				}
				catch (final Exception e)
				{
				}
			}
			EnchantManager.getInstance().showMainPage(player);
		}
		else if (command.equals("item_limit"))
		{
			if (params == null || params.isEmpty())
			{
				player.getEnchantParams().isChangingUpgradeItemLimit = true;
			}
			else
			{
				try
				{
					int userLimit = Integer.parseInt(params.split("-")[0]);
					final ItemInstance enchant = player.getEnchantParams().upgradeItem;
					if (userLimit < 1)
					{
						userLimit = 1;
					}
					if (userLimit > enchant.getCount())
					{
						userLimit = (int) enchant.getCount();
					}
					if (userLimit > Config.ENCHANT_MAX_ITEM_LIMIT)
					{
						userLimit = Config.ENCHANT_MAX_ITEM_LIMIT;
					}
					player.getEnchantParams().upgradeItemLimit = userLimit;
					player.getEnchantParams().isChangingUpgradeItemLimit = false;
				}
				catch (final Exception e)
				{
				}
			}
			EnchantManager.getInstance().showMainPage(player);
		}
		else if (command.equals("common_for_safe"))
		{
			if (params == null || params.isEmpty())
			{
				return false;
			}
			else
			{
				final int safe = Integer.parseInt(params.split("-")[0]);
				if (safe == 0)
				{
					player.getEnchantParams().isUseCommonScrollWhenSafe = false;
				}
				else if (safe == 1)
				{
					player.getEnchantParams().isUseCommonScrollWhenSafe = true;
				}
				else
				{
					return false;
				}
				EnchantManager.getInstance().showMainPage(player);
			}
		}
		else if (command.equals("item_choose"))
		{
			if (params == null || params.isEmpty())
			{
				return false;
			}
			else
			{
				final String[] arr = params.split("-");
				if (arr.length < 3)
				{
					return false;
				}
				EnchantManager.getInstance().showItemChoosePage(player, Integer.parseInt(arr[0]), Integer.parseInt(arr[1]), Integer.parseInt(arr[2]));
			}
		}
		else if (command.equals("item_change"))
		{
			if (params == null || params.isEmpty())
			{
				return false;
			}
			else
			{
				final String[] arr = params.split("-");
				if (arr.length < 2)
				{
					return false;
				}
				if (arr[0].equals("0"))
				{
					player.getEnchantParams().targetItem = player.getInventory().getItemByObjectId(Integer.parseInt(arr[1]));
				}
				else
				{
					player.getEnchantParams().upgradeItem = player.getInventory().getItemByObjectId(Integer.parseInt(arr[1]));
				}
			}
			EnchantManager.getInstance().showMainPage(player);
		}
		else if (command.equals("begin_enchant"))
		{
			if (Config.ENCHANT_SERVICE_ONLY_FOR_PREMIUM && !player.hasPremiumBonus())
			{
				player.sendMessage((new ServerMessage("Enchant.ONLY_PREMIUM", player.getLang())).toString());
				return false;
			}
			final ItemInstance consumeItem = player.getInventory().getItemByItemId(Config.ENCHANT_CONSUME_ITEM);
			if (Config.ENCHANT_CONSUME_ITEM != 0 && (consumeItem == null || consumeItem.getCount() < Config.ENCHANT_CONSUME_ITEM_COUNT))
			{
				final Item template = ItemsParser.getInstance().getTemplate(Config.ENCHANT_CONSUME_ITEM);
				final ServerMessage msg = new ServerMessage("Enchant.NEED_ITEMS", player.getLang());
				msg.add(Config.ENCHANT_CONSUME_ITEM_COUNT);
				msg.add(template.getName(player.getLang()));
				player.sendMessage(msg.toString());
				return false;
			}
			EnchantUtils.getInstance().enchant(player);
		}
		else if (command.equals("enchant_help"))
		{
			EnchantManager.getInstance().showHelpPage(player);
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return commands;
	}
}