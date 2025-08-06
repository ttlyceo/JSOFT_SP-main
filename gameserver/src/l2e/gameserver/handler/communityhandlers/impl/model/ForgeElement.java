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
package l2e.gameserver.handler.communityhandlers.impl.model;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.FoundationParser;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.strings.server.ServerStorage;

public class ForgeElement
{
	public static String[] generateAttribution(ItemInstance item, int slot, Player player, boolean hasBonus)
	{
		final String[] data = new String[4];

		final String noicon = "icon.NOIMAGE";
		final String slotclose = "L2UI_CT1.ItemWindow_DF_SlotBox_Disable";
		final String dot = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.EMPTY") + "";
		final String immposible = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_POSIBLE") + "";
		final String maxenchant = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.CANT_ATTR") + "";
		final String heronot = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.CANT_HERO_ITEM_ATTR") + "";
		final String picenchant = "l2ui_ch3.multisell_plusicon";

		if (item != null)
		{
			String name = item.getItem().getName(player.getLang());
			if (name.length() > 24)
			{
				name = name.substring(0, 24) + "...";
			}
			
			data[0] = item.getItem().getIcon();
			data[1] = new StringBuilder().append(name).append(" ").append(item.getEnchantLevel() > 0 ? new StringBuilder().append("+").append(item.getEnchantLevel()).toString() : "").toString();
			if (itemCheckGrade(hasBonus, item))
			{
				if (item.isHeroItem())
				{
					data[2] = heronot;
					data[3] = slotclose;
				}
				else if ((item.isArmor() && item.getElementals() != null && item.getElementals().length >= 3 && item.getElementals()[0].getValue() >= Config.BBS_FORGE_ARMOR_ATTRIBUTE_MAX) || (item.isWeapon() && item.getElementals() != null && item.getElementals()[0].getValue() >= Config.BBS_FORGE_WEAPON_ATTRIBUTE_MAX) || item.getItem().isAccessory() || item.getItem().isShield())
				{
					data[2] = maxenchant;
					data[3] = slotclose;
				}
				else
				{
					data[2] = new StringBuilder().append("<button action=\"bypass -h _bbsforge:attribute:item:").append(slot).append("\" value=\"").append("" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.INSERT_ATTRIBUTE") + "").append("\" width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">").toString();
					data[3] = picenchant;
				}
			}
			else
			{
				data[2] = immposible;
				data[3] = slotclose;
			}
		}
		else
		{
			data[0] = noicon;
			data[1] = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.SLOT_NOT_CLOSED_" + slot + "") + "";
			data[2] = dot;
			data[3] = slotclose;
		}

		return data;
	}
	
	public static String[] generateEnchant(ItemInstance item, int max, int slot, Player player)
	{
		final String[] data = new String[4];
		
		final String noicon = "icon.NOIMAGE";
		final String slotclose = "L2UI_CT1.ItemWindow_DF_SlotBox_Disable";
		final String dot = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.EMPTY") + "";
		final String maxenchant = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ENCHANT_MAX") + "";
		final String picenchant = "l2ui_ch3.multisell_plusicon";
		
		if (item != null)
		{
			String name = item.getItem().getName(player.getLang());
			if (name.length() > 24)
			{
				name = name.substring(0, 24) + "...";
			}
			
			data[0] = item.getItem().getIcon();
			data[1] = new StringBuilder().append(name).append(" ").append(item.getEnchantLevel() > 0 ? new StringBuilder().append("+").append(item.getEnchantLevel()).toString() : "").toString();
			if (!item.getItem().isArrow())
			{
				if (item.getEnchantLevel() >= max || item.isEnchantable() == 0)
				{
					data[2] = maxenchant;
					data[3] = slotclose;
				}
				else
				{
					data[2] = new StringBuilder().append("<button action=\"bypass -h _bbsforge:enchant:item:").append(slot).append("\" value=\"").append("" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ENCHANT") + "").append("\"width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">").toString();
					data[3] = picenchant;
				}
			}
			else
			{
				data[2] = dot;
				data[3] = slotclose;
			}
		}
		else
		{
			data[0] = noicon;
			data[1] = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.SLOT_NOT_CLOSED_" + slot + "") + "";
			data[2] = dot;
			data[3] = slotclose;
		}
		
		return data;
	}
	
	public static String[] generateFoundation(ItemInstance item, int slot, Player player)
	{
		final String[] data = new String[4];
		
		final String noicon = "icon.NOIMAGE";
		final String slotclose = "L2UI_CT1.ItemWindow_DF_SlotBox_Disable";
		final String dot = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.EMPTY") + "";
		final String no = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.CANT_CHANGE") + "";
		final String picenchant = "l2ui_ch3.multisell_plusicon";
		
		if (item != null)
		{
			String name = item.getItem().getName(player.getLang());
			if (name.length() > 24)
			{
				name = name.substring(0, 24) + "...";
			}
			
			data[0] = item.getItem().getIcon();
			data[1] = new StringBuilder().append(name).append(" ").append(item.getEnchantLevel() > 0 ? new StringBuilder().append("+").append(item.getEnchantLevel()).toString() : "").toString();
			if (!item.getItem().isArrow())
			{
				final int found = FoundationParser.getInstance().getFoundation(item.getId());
				if (found == -1)
				{
					data[2] = no;
					data[3] = slotclose;
				}
				else
				{
					data[2] = new StringBuilder().append("<button action=\"bypass -h _bbsforge:foundation:item:").append(slot).append("\" value=\"").append("" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.EXCHANGE") + "").append("\"width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">").toString();
					data[3] = picenchant;
				}
			}
			else
			{
				data[2] = dot;
				data[3] = slotclose;
			}
		}
		else
		{
			data[0] = noicon;
			data[1] = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.SLOT_NOT_CLOSED_" + slot + "") + "";
			data[2] = dot;
			data[3] = slotclose;
		}
		
		return data;
	}
	
	public static String page(Player player)
	{
		return HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/page_template.htm");
	}
	
	public static boolean itemCheckGrade(boolean hasBonus, ItemInstance item)
	{
		final int grade = item.getItem().getCrystalType();
		
		switch (grade)
		{
			case Item.CRYSTAL_S :
				return hasBonus;
			case Item.CRYSTAL_S80 :
				return hasBonus;
			case Item.CRYSTAL_S84 :
				return hasBonus;
		}
		return false;
	}
	
	public static boolean canEnchantArmorAttribute(int attr, ItemInstance item)
	{
		final byte opositeElement = Elementals.getOppositeElement((byte) attr);

		if (item.getElementals() != null)
		{
			for (final Elementals elm : item.getElementals())
			{
				if (elm.getElement() == opositeElement)
				{
					return false;
				}
			}
		}
		return true;
	}
}