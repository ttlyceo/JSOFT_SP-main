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
package l2e.gameserver.model.service.autoenchant;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.EnchantItemParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.StatusUpdate;

public class EnchantByScrollTask implements Runnable
{
	private final Player _player;

	public EnchantByScrollTask(Player player)
	{
		_player = player;
	}

	public static boolean isValidPlayer(final Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		if (player.isActionsDisabled())
		{
			return false;
		}
		
		if (player.isProcessingTransaction() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE);
			return false;
		}
		
		if (!player.isOnline() || player.getClient().isDetached())
		{
			return false;
		}
		return true;
	}
	
	@Override
	public void run()
	{
		if (!isValidPlayer(_player))
		{
			_player.sendMessage((new ServerMessage("Enchant.NOT_VALID", _player.getLang())).toString());
			return;
		}
		
		boolean isNeedEquip = false;
		boolean isNeedUpdate = false;

		final int isCrystallized = 0;
		int maxEnchant = 0;
		int commonScrolls = 0;
		int scrolls = 0;
		int success = 0;
		int count = 0;

		var item = _player.getEnchantParams().targetItem;
		var scroll = _player.getEnchantParams().upgradeItem;
		final var inventory = _player.getInventory();
		if (item != null && item.isEquipped())
		{
			inventory.unEquipItem(item);
			isNeedEquip = true;
		}
		try
		{
			for (int i = 0; i < _player.getEnchantParams().upgradeItemLimit && _player.getEnchantParams().targetItem.getEnchantLevel() < _player.getEnchantParams().maxEnchant; i++)
			{
				if (!isValidPlayer(_player))
				{
					_player.sendMessage((new ServerMessage("Enchant.NOT_VALID", _player.getLang())).toString());
					return;
				}
				
				if (item == null)
				{
					_player.sendMessage((new ServerMessage("Enchant.SELECT_SCROLL", _player.getLang())).toString());
					return;
				}
				
				if (item.getEnchantLevel() < 3 && _player.getEnchantParams().isUseCommonScrollWhenSafe)
				{
					scroll = EnchantUtils.getInstance().getUnsafeEnchantScroll(_player, item);
					if (scroll == null)
					{
						_player.sendMessage((new ServerMessage("Enchant.COND_OR_SCROLL", _player.getLang())).toString());
						return;
					}
					commonScrolls++;
				}
				else
				{
					scroll = _player.getEnchantParams().upgradeItem;
					if (scroll == null)
					{
						_player.sendMessage((new ServerMessage("Enchant.NOT_SCROLL", _player.getLang())).toString());
						return;
					}
					scrolls++;
				}
				_player.setActiveEnchantItemId(scroll.getObjectId());
				final var esi = EnchantItemParser.getInstance().getEnchantScroll(scroll);
				if (esi == null)
				{
					_player.setActiveEnchantItemId(Player.ID_NONE);
					_player.sendMessage((new ServerMessage("Enchant.SCROLL_NOT_VALID", _player.getLang())).toString());
					return;
				}
				
				if (!esi.isValid(item))
				{
					_player.setActiveEnchantItemId(Player.ID_NONE);
					_player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
					return;
				}
				
				if (!_player.destroyItem("[AutoEnchant]", scroll.getObjectId(), 1, _player, false))
				{
					_player.setActiveEnchantItemId(Player.ID_NONE);
					_player.sendMessage((new ServerMessage("Enchant.SCROLL_MISS", _player.getLang())).toString());
					return;
				}
				
				if (item.isEquipped())
				{
					inventory.unEquipItem(item);
				}
				
				double chance = esi.getChance(_player, item);
				if (chance == -1.0)
				{
					_player.setActiveEnchantItemId(Player.ID_NONE);
					_player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
					return;
				}
				_player.setActiveEnchantItemId(item.getId());
				chance = Math.min((chance + _player.getPremiumBonus().getEnchantChance()), 100.0);
				if (Rnd.chance(chance + Config.ENCHANT_SCROLL_CHANCE_CORRECT))
				{
					if (chance != 100.0)
					{
						success++;
					}
					item.setEnchantLevel(item.getEnchantLevel() + 1);
				}
				else
				{
					if (esi.isSafe())
					{
					}
					else
					{
						if (esi.isBlessed())
						{
							if (Config.SYSTEM_BLESSED_ENCHANT)
							{
								item.setEnchantLevel(Config.BLESSED_ENCHANT_SAVE);
							}
							else
							{
								item.setEnchantLevel(0);
							}
						}
						else
						{
							final int crystalId = item.getItem().getCrystalItemId();
							int CryCount = item.getCrystalCount() - ((item.getItem().getCrystalCount() + 1) / 2);
							if (CryCount < 1)
							{
								CryCount = 1;
							}
							
							if (!_player.destroyItem("[AutoEnchant]", item, _player, false))
							{
								Util.handleIllegalPlayerAction(_player, "Unable to delete item on enchant failure from player " + _player.getName(null) + ", possible cheater !");
								_player.setActiveEnchantItemId(Player.ID_NONE);
								return;
							}
							
							if (crystalId != 0)
							{
								_player.addItem("[AutoEnchant]", crystalId, CryCount, _player, true);
							}
							isNeedEquip = false;
							item = null;
						}
					}
				}
				final int ench = item != null ? item.getEnchantLevel() : 0;
				if (ench > maxEnchant)
				{
					maxEnchant = ench;
				}
				isNeedUpdate = true;
				if (ench >= 3)
				{
					count++;
				}
			}
		}
		finally
		{
			if (isNeedEquip && item != null)
			{
				inventory.equipItem(item);
			}
			
			if (item != null)
			{
				item.updateDatabase();
			}
			if (isNeedUpdate)
			{
				if (Config.ENCHANT_CONSUME_ITEM != 0)
				{
					_player.getInventory().destroyItemByItemId(Config.ENCHANT_CONSUME_ITEM, Config.ENCHANT_CONSUME_ITEM_COUNT, "[AutoEnchant]");
					final var template = ItemsParser.getInstance().getTemplate(Config.ENCHANT_CONSUME_ITEM);
					final var msg = new ServerMessage("Enchant.SPET_ITEMS", _player.getLang());
					msg.add(Config.ENCHANT_CONSUME_ITEM_COUNT);
					msg.add(template.getName(_player.getLang()));
					_player.sendMessage(msg.toString());
				}
				final Map<String, Integer> result = new HashMap<>();
				result.put("crystallized", isCrystallized);
				result.put("enchant", item == null ? 0 : item.getEnchantLevel());
				result.put("maxenchant", maxEnchant);
				result.put("scrolls", scrolls);
				result.put("commonscrolls", commonScrolls);
				if (count == 0)
				{
					count++;
				}
				result.put("chance", (int) ((success / (count / 100.)) * 100));
				result.put("success", item == null ? 0 : item.getEnchantLevel() == _player.getEnchantParams().maxEnchant ? 1 : 0);
				EnchantManager.getInstance().showResultPage(_player, EnchantType.SCROLL, result);
			}
			_player.setActiveEnchantItemId(Player.ID_NONE);
			_player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
			_player.sendItemList(false);
			_player.broadcastCharInfo();
		}
	}
}