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
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ActionFail;
import l2e.gameserver.network.serverpackets.StatusUpdate;

public class EnchantByAttributeTask implements Runnable
{
	private final Player _player;
    private int stoneId = -1;

	public EnchantByAttributeTask(Player player)
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

        boolean isNeedUpdate = false;
        boolean isNeedEquip = false;
        int stones = 0;
        int crystals = 0;
        int success = 0;
		final PcInventory inventory = _player.getInventory();
		final ItemInstance itemToEnchant = _player.getEnchantParams().targetItem;
		ItemInstance stone = _player.getEnchantParams().upgradeItem;
		stoneId = stone.getId();
		try
		{
			for (int i = 0; i < _player.getEnchantParams().upgradeItemLimit && checkAttributeLvl(_player); i++)
			{
				if (!isValidPlayer(_player))
				{
					_player.sendMessage((new ServerMessage("Enchant.NOT_VALID", _player.getLang())).toString());
                    return;
                }
				
				if (itemToEnchant == null)
				{
					_player.sendActionFailed();
					_player.sendMessage((new ServerMessage("Enchant.SELECT_SCROLL", _player.getLang())).toString());
                    return;
                }
				if (stone == null)
				{
					_player.sendActionFailed();
					_player.sendMessage((new ServerMessage("Enchant.SELECT_ATT", _player.getLang())).toString());
                    return;
                }

				final Item item = itemToEnchant.getItem();
				if (item.isCommonItem() || !item.isElementable() || item.getCrystalType() < Item.CRYSTAL_S || item.getBodyPart() == Item.SLOT_L_HAND)
				{
					_player.sendPacket(ActionFail.STATIC_PACKET, SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
                    return;
                }

				if (itemToEnchant.isEnchantable() == 0)
				{
					_player.sendPacket(ActionFail.STATIC_PACKET, SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
                    return;
                }

				if (itemToEnchant.isStackable() || (stone = inventory.getItemByObjectId(stone.getObjectId())) == null)
				{
					_player.sendMessage((new ServerMessage("Enchant.MISS_ITEMS", _player.getLang())).toString());
                    return;
                }
				
				final byte stoneElement = Elementals.getItemElement(stoneId);
				if (stoneElement == Elementals.NONE)
				{
					_player.sendPacket(ActionFail.STATIC_PACKET, SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
                    return;
                }
				final byte element = itemToEnchant.isArmor() ? Elementals.getReverseElement(stoneElement) : stoneElement;
				if (itemToEnchant.isArmor())
				{
					if (itemToEnchant.getElemental(Elementals.getReverseElement(element)) != null)
					{
						_player.sendPacket(SystemMessageId.ANOTHER_ELEMENTAL_POWER_ALREADY_ADDED);
                        return;
                    }
				}
				else if (itemToEnchant.isWeapon())
				{
					if (itemToEnchant.getAttackElementType() != -2 && itemToEnchant.getAttackElementType() != Elementals.NONE && itemToEnchant.getAttackElementType() != element)
					{
						_player.sendPacket(SystemMessageId.ANOTHER_ELEMENTAL_POWER_ALREADY_ADDED);
                        return;
                    }
				}
				else
				{
					_player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
                    return;
                }

				if (item.isUnderwear() || item.isCloak() || item.isBracelet() || item.isBelt())
				{
					_player.sendPacket(ActionFail.STATIC_PACKET, SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
                    return;
                }
				final int maxValue = getLimit(itemToEnchant, stoneId);
				
				final int currentValue = itemToEnchant.getElemental(element) != null ? itemToEnchant.getElemental(element).getValue() : 0;
				if (currentValue >= maxValue)
				{
					_player.sendPacket(ActionFail.STATIC_PACKET, SystemMessageId.ELEMENTAL_ENHANCE_CANCELED);
                    return;
                }

				if (itemToEnchant.getOwnerId() != _player.getObjectId())
				{
					_player.sendPacket(ActionFail.STATIC_PACKET, SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
                    return;
                }

				if (inventory.destroyItem("[AutoEnchant]", stone.getObjectId(), 1, _player, null) == null)
				{
					_player.sendMessage((new ServerMessage("Enchant.MISS_ITEMS", _player.getLang())).toString());
                    return;
                }
				
				boolean boolka = false;
				switch (Elementals.getItemElemental(stoneId)._type)
				{
					case Stone :
					case Roughore :
						boolka = Rnd.get(100) < (Config.ENCHANT_CHANCE_ELEMENT_STONE + _player.getPremiumBonus().getEnchantChance());
						break;
					case Crystal :
						boolka = Rnd.get(100) < (Config.ENCHANT_CHANCE_ELEMENT_CRYSTAL + _player.getPremiumBonus().getEnchantChance());
						break;
					case Jewel :
						boolka = Rnd.get(100) < (Config.ENCHANT_CHANCE_ELEMENT_JEWEL + _player.getPremiumBonus().getEnchantChance());
						break;
					case Energy :
						boolka = Rnd.get(100) < (Config.ENCHANT_CHANCE_ELEMENT_ENERGY + _player.getPremiumBonus().getEnchantChance());
						break;
				}
				
				if (boolka)
				{
                    success++;
					int value = itemToEnchant.isWeapon() ? Elementals.NEXT_WEAPON_BONUS : Elementals.ARMOR_BONUS;
					if (itemToEnchant.getAttributeElementValue(element) == 0 && itemToEnchant.isWeapon())
					{
						value = Elementals.FIRST_WEAPON_BONUS;
					}

					if (itemToEnchant.isEquipped())
					{
						_player.getInventory().unEquipItem(itemToEnchant);
                        isNeedEquip = true;
                    }
					itemToEnchant.setElementAttr(element, itemToEnchant.getAttributeElementValue(element) + value);
                }

                if (EnchantUtils.getInstance().isAttributeStone(stone))
				{
					stones++;
				}
				else if (EnchantUtils.getInstance().isAttributeCrystal(stone))
				{
					crystals++;
				}
                isNeedUpdate = true;
            }
		}
		finally
		{
            if (stone == null || stone.getCount() <= 0)
			{
				_player.getEnchantParams().upgradeItem = null;
			}
			
			if (isNeedUpdate)
			{
				if (Config.ENCHANT_CONSUME_ITEM != 0)
				{
					_player.getInventory().destroyItemByItemId(Config.ENCHANT_CONSUME_ITEM, Config.ENCHANT_CONSUME_ITEM_COUNT, "[AutoEnchant]");
					final Item template = ItemsParser.getInstance().getTemplate(Config.ENCHANT_CONSUME_ITEM);
					
					final ServerMessage msg = new ServerMessage("Enchant.SPET_ITEMS", _player.getLang());
					msg.add(Config.ENCHANT_CONSUME_ITEM_COUNT);
					msg.add(template.getName(_player.getLang()));
					_player.sendMessage(msg.toString());
				}
				_player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
				_player.sendItemList(false);
				_player.broadcastCharInfo();
				
                final Map<String, Integer> result = new HashMap<>();
                result.put("enchant", getAttValue());
                result.put("stones", stones);
                result.put("crystals", crystals);
                int sum = stones + crystals;
                if (sum == 0)
				{
					sum++;
				}
                result.put("chance", (int) ((success / (sum / 100.)) * 100));
				result.put("success", itemToEnchant == null ? 0 : getAttValue() >= _player.getEnchantParams().maxEnchantAtt ? 1 : 0);
				EnchantManager.getInstance().showResultPage(_player, EnchantType.ATTRIBUTE, result);
                if (isNeedEquip)
				{
					_player.getInventory().equipItem(itemToEnchant);
				}
            }
        }
    }

	private boolean checkAttributeLvl(Player player)
	{
        if (player == null)
		{
			return false;
		}
		return getAttValue() < player.getEnchantParams().maxEnchantAtt;
    }

	private int getAttValue()
	{
		final ItemInstance targetItem = _player.getEnchantParams().targetItem;
		final ItemInstance enchantItem = _player.getEnchantParams().upgradeItem;
        int usedStoneId;
        if (targetItem == null)
		{
			return 0;
		}
		
        if (enchantItem == null)
		{
			usedStoneId = stoneId;
		}
		else
		{
			usedStoneId = enchantItem.getId();
		}
		
        if (targetItem.isWeapon())
		{
			return targetItem.getAttackElementPower();
		}
		else
		{
			final Elementals element = targetItem.getElemental(Elementals.getReverseElement(Elementals.getElementById(usedStoneId)));
			return element == null ? 0 : element.getValue();
		}
    }
	
	public int getLimit(ItemInstance item, int sotneId)
	{
		final Elementals.ElementalItems elementItem = Elementals.getItemElemental(sotneId);
		if (elementItem == null)
		{
			return 0;
		}
		
		if (item.isWeapon())
		{
			return Elementals.WEAPON_VALUES[elementItem._type._maxLevel];
		}
		return Elementals.ARMOR_VALUES[elementItem._type._maxLevel];
	}
}