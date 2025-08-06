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

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.model.strings.server.ServerMessage;

public class EnchantUtils
{
    private static EnchantUtils instance;

	public static EnchantUtils getInstance()
	{
        return instance == null ? instance = new EnchantUtils() : instance;
    }

	public boolean isAttributeStone(ItemInstance item)
	{
		final int[] stones =
		{
		        9547, 9548, 9549, 9550, 9551,
                9546
        };
		
        for (final int id : stones)
		{
			if (id == item.getId())
			{
				return true;
			}
		}
        return false;
    }

	public boolean isAttributeCrystal(ItemInstance item)
	{
		final int[] crystals =
		{
		        9552, 9553, 9554, 9555, 9556, 9557,
        };
		
        for (final int id : crystals)
		{
			if (id == item.getId())
			{
				return true;
			}
		}
        return false;
    }

	public boolean isAttribute(ItemInstance item)
	{
        return isAttributeCrystal(item) || isAttributeStone(item);
    }

	public boolean isEnchantScroll(ItemInstance item)
	{
		if (item.getEtcItem() == null)
		{
			return false;
		}
		if (item.getEtcItem().getHandlerName() == null)
		{
			return false;
		}
		return item.getEtcItem().getHandlerName().equals("EnchantScrolls");
    }

	public boolean isBlessed(ItemInstance item)
	{
		return item.getItem().getItemType() == EtcItemType.BLESS_SCRL_ENCHANT_WP || item.getItem().getItemType() == EtcItemType.BLESS_SCRL_ENCHANT_AM;
    }

	public boolean isArmorScroll(ItemInstance item)
	{
		return item.getItem().getItemType() == EtcItemType.SCRL_ENCHANT_AM || item.getItem().getItemType() == EtcItemType.BLESS_SCRL_ENCHANT_AM;
    }

	public List<ItemInstance> getWeapon(Player player)
	{
        final List<ItemInstance> weapon = new ArrayList<>();
        for (final ItemInstance item : player.getInventory().getItems())
		{
			if (item.isWeapon())
			{
                if (item.getItem().getCrystalType() == 0)
				{
					continue;
				}
				if (item.isEnchantable() == 0)
				{
					continue;
				}
                weapon.add(item);
            }
		}
        return weapon;
    }

	public List<ItemInstance> getArmor(Player player)
	{
        final List<ItemInstance> armor = new ArrayList<>();
        for (final ItemInstance item : player.getInventory().getItems())
		{
			if (item.isArmor() || (item.getItem().getBodyPart() == 25 && Config.ENCHANT_ALLOW_BELTS))
			{
                if (item.getItem().getCrystalType() == 0)
				{
					continue;
				}
				if (item.isEnchantable() == 0)
				{
					continue;
				}
                armor.add(item);
            }
		}
        return armor;
    }

	public List<ItemInstance> getJewelry(Player player)
	{
        final List<ItemInstance> jewelry = new ArrayList<>();
        for (final ItemInstance item : player.getInventory().getItems())
		{
			if (item.isJewel())
			{
                if (item.getItem().getCrystalType() == 0)
				{
					continue;
				}
				if (item.isEnchantable() == 0)
				{
					continue;
				}
                jewelry.add(item);
            }
		}
        return jewelry;
    }

	public List<ItemInstance> getAtributes(Player player)
	{
        final List<ItemInstance> stones = new ArrayList<>();
        for (final ItemInstance item : player.getInventory().getItems())
		{
			if (isAttribute(item))
			{
				stones.add(item);
			}
		}
        return stones;
    }

	public List<ItemInstance> getScrolls(Player player)
	{
        final List<ItemInstance> scrolls = new ArrayList<>();
        for (final ItemInstance item : player.getInventory().getItems())
		{
			if (isEnchantScroll(item))
			{
				scrolls.add(item);
			}
		}
        return scrolls;
    }

	public void enchant(Player player)
	{
        if (player == null)
		{
			return;
		}
        final ItemInstance upgradeItem = player.getEnchantParams().upgradeItem;
		if (upgradeItem == null)
		{
			player.sendMessage((new ServerMessage("Enchant.SELECT_SCROLL", player.getLang())).toString());
            return;
        }
		if (player.getEnchantParams().targetItem == null)
		{
			player.sendMessage((new ServerMessage("Enchant.SELECT_SCROLL", player.getLang())).toString());
            return;
        }

        if (isEnchantScroll(upgradeItem))
		{
			ThreadPoolManager.getInstance().execute(new EnchantByScrollTask(player));
		}
		else if (isAttribute(upgradeItem))
		{
			ThreadPoolManager.getInstance().execute(new EnchantByAttributeTask(player));
		}
    }

	public ItemInstance getUnsafeEnchantScroll(Player player, ItemInstance item)
	{
        int scrollId = 0;
        ItemInstance scroll = null;
		final int type = item.getItem().getCrystalType();
		if (item.isWeapon())
		{
			if (type == Item.CRYSTAL_D)
			{
				scrollId = 955;
			}
			else if (type == Item.CRYSTAL_C)
			{
				scrollId = 951;
			}
			else if (type == Item.CRYSTAL_B)
			{
				scrollId = 947;
			}
			else if (type == Item.CRYSTAL_A)
			{
				scrollId = 729;
			}
			else if (type == Item.CRYSTAL_S || type == Item.CRYSTAL_S80 || type == Item.CRYSTAL_S84)
			{
				scrollId = 959;
			}
		}
		else if (item.isArmor() || item.isJewel())
		{
			if (type == Item.CRYSTAL_D)
			{
				scrollId = 956;
			}
			else if (type == Item.CRYSTAL_C)
			{
				scrollId = 952;
			}
			else if (type == Item.CRYSTAL_B)
			{
				scrollId = 948;
			}
			else if (type == Item.CRYSTAL_A)
			{
				scrollId = 730;
			}
			else if (type == Item.CRYSTAL_S || type == Item.CRYSTAL_S80 || type == Item.CRYSTAL_S84)
			{
				scrollId = 960;
			}
        }
        if (scrollId != 0)
		{
			scroll = player.getInventory().getItemByItemId(scrollId);
		}
        return scroll;
    }
}
