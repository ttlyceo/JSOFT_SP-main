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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class EnchantManager
{
    private static EnchantManager instance;
    private final String mainColor = "BCBCBC";
    private final String fieldColor = "B59A75";
    private final String unknown = setColor("?", fieldColor);

	public static EnchantManager getInstance()
	{
        return instance == null ? instance = new EnchantManager() : instance;
    }

	private String setColor(String text, String color)
	{
        return "<font color=" + color + ">" + text + "</font>";
    }

	private String setColor(int value, String color)
	{
        return setColor("" + value, color);
    }

	private String setCenter(String text)
	{
        return "<center>" + text + "</center>";
    }

	private String setButton(String bypass, String name, int width, int height)
	{
        return "<button width=" + width + " height=" + height + " back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h ." + bypass + "\" value=\"" + name + "\">";
    }

	private String setPressButton(String bypass, String name, int width, int height)
	{
        return "<button width=" + width + " height=" + height + " back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h ." + bypass + "\" value=\"" + name + "\">";
    }

	private String setTextBox(String name, int width, int height)
	{
        return "<edit var=\"" + name + "\" width=" + width + " height=" + height + ">";
    }

	private String setIcon(String src)
	{
        return setIcon(src, 32, 32);
    }

	private String setIcon(String src, int width, int height)
	{
        return "<img src=\"" + src + "\" width=" + width + " height=" + height + ">";
    }

	public void showMainPage(Player player)
	{
		String page = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autoenchant/enchant.htm");
		page = setTargetItem(player, player.getEnchantParams().targetItem, page);
		page = setEnchantItem(player, player.getEnchantParams().upgradeItem, page);
        page = setConfiguration(player, page);
		show(page, player);
    }

	public void showItemChoosePage(Player player, int item_type, int sort_type, int page_number)
	{
		String page = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autoenchant/enchant_item_choose.htm");
        page = parseItemChoosePage(player, page, item_type, sort_type, page_number);
		show(page, player);
    }

	public void showResultPage(Player player, EnchantType type, Map<String, Integer> result)
	{
        String page;
		switch (type)
		{
			case SCROLL :
				page = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autoenchant/enchant_scroll_result.htm");
				page = page.replaceFirst("%crystallized%", result.get("crystallized") == 1 ? "" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.YES1") + "" : "" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.NO1") + "");
                page = page.replaceFirst("%enchant%", "" + result.get("enchant"));
                page = page.replaceFirst("%max_enchant%", "" + result.get("maxenchant"));
                page = page.replaceFirst("%scrolls%", "" + result.get("scrolls"));
                page = page.replaceFirst("%common_scrolls%", "" + result.get("commonscrolls"));
                page = page.replaceFirst("%chance%", "" + ((double) result.get("chance") / 100));
				page = page.replaceFirst("%success%", result.get("success") == 1 ? "<font name=\"hs12\" color=\"00c500\">" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.SUCCESS") + "</font>" : "<font name=\"hs12\" color=\"c50000\">" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.FAIL") + "</font>");
				show(page, player);
                break;
			case ATTRIBUTE :
				page = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autoenchant/enchant_attribute_result.htm");
                page = page.replaceFirst("%enchant%", "" + result.get("enchant"));
                page = page.replaceFirst("%stones%", "" + result.get("stones"));
                page = page.replaceFirst("%crystals%", "" + result.get("crystals"));
                page = page.replaceFirst("%chance%", "" + ((double) result.get("chance") / 100));
				page = page.replaceFirst("%success%", result.get("success") == 1 ? "<font name=\"hs12\" color=\"00c500\">" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.SUCCESS") + "</font>" : "<font name=\"hs12\" color=\"c50000\">" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.FAIL") + "</font>");
				show(page, player);
                break;
        }
    }
	
	private void show(String text, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(player, text);
		player.sendPacket(html);
	}

	public void showHelpPage(Player player)
	{
		final String page = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autoenchant/enchant_help.htm");
		show(page, player);
    }

	private String setTargetItem(Player player, ItemInstance targetItem, String page)
	{
		if (targetItem == null)
		{
            page = page.replaceFirst("%item_icon%", setIcon("icon.weapon_long_sword_i00"));
			page = page.replaceFirst("%item_name%", setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.SELECT_ITEM"), mainColor));
            page = page.replaceFirst("%item_enchant_lvl%", unknown);
            page = page.replaceFirst("%item_att%", unknown);
            page = page.replaceFirst("%item_att_lvl%", unknown);
			page = page.replaceFirst("%item_button%", setButton("", ServerStorage.getInstance().getString(player.getLang(), "Enchant.SELECTED"), 55, 32));
            return page;
        }

		page = page.replaceFirst("%item_icon%", setIcon(targetItem.getItem().getIcon()));
		page = page.replaceFirst("%item_name%", setColor(targetItem.getItem().getName(player.getLang()), mainColor));
        page = page.replaceFirst("%item_enchant_lvl%", "" + setColor("+" + targetItem.getEnchantLevel(), fieldColor));
		page = page.replaceFirst("%item_button%", setButton("item_change 0-" + targetItem.getObjectId(), ServerStorage.getInstance().getString(player.getLang(), "Enchant.SELECTED"), 55, 32));
		page = setAttribute(player, targetItem, page);
        return page;
    }

	private String setEnchantItem(Player player, ItemInstance enchantItem, String page)
	{
		if (enchantItem == null)
		{
            page = page.replaceFirst("%ench_icon%", setIcon("icon.etc_scroll_of_enchant_weapon_i01"));
			page = page.replaceFirst("%ench_name%", setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.SELECT_ITEMS"), mainColor));
			page = page.replaceFirst("%ench_blessed_field%", setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.BLESSED"), mainColor));
            page = page.replaceFirst("%ench_blessed%", unknown);
            page = page.replaceFirst("%ench_type%", unknown);
            page = page.replaceFirst("%ench_count%", unknown);
			page = page.replaceFirst("%ench_button%", setButton("", ServerStorage.getInstance().getString(player.getLang(), "Enchant.SELECTED"), 55, 32));
            return page;
        }

		page = page.replaceFirst("%ench_icon%", setIcon(enchantItem.getItem().getIcon()));
		page = page.replaceFirst("%ench_name%", setColor(enchantItem.getItem().getName(player.getLang()), mainColor));
		if (EnchantUtils.getInstance().isAttribute(enchantItem))
		{
			page = page.replaceFirst("%ench_blessed_field%", setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ELEMENT"), mainColor));
			page = page.replaceFirst("%ench_blessed%", setAttributeById(player, enchantItem.getId()));
			page = page.replaceFirst("%ench_type%", setColor(EnchantUtils.getInstance().isAttributeCrystal(enchantItem) ? ServerStorage.getInstance().getString(player.getLang(), "Enchant.CRYSTAL") : ServerStorage.getInstance().getString(player.getLang(), "Enchant.STONE"), fieldColor));
        }
		page = page.replaceFirst("%ench_blessed_field%", setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.BLESSED"), mainColor));
		page = page.replaceFirst("%ench_blessed%", EnchantUtils.getInstance().isBlessed(enchantItem) ? "" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.YES") + "" : "" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.NO") + "");
		page = page.replaceFirst("%ench_type%", setColor(EnchantUtils.getInstance().isArmorScroll(enchantItem) ? ServerStorage.getInstance().getString(player.getLang(), "Enchant.ARMOR") : ServerStorage.getInstance().getString(player.getLang(), "Enchant.WEAPON"), mainColor));
        page = page.replaceFirst("%ench_count%", setColor("" + enchantItem.getCount(), fieldColor));
		page = page.replaceFirst("%ench_button%", setButton("item_change 1-" + enchantItem.getObjectId(), ServerStorage.getInstance().getString(player.getLang(), "Enchant.SELECTED"), 55, 32));
        return page;
    }

	private String setConfiguration(Player player, String page)
	{
		page = page.replaceFirst("%common_scrolls_for_safe%", player.getEnchantParams().isUseCommonScrollWhenSafe ? "" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.YES") + "" : "" + ServerStorage.getInstance().getString(player.getLang(), "Enchant.NO") + "");
        if (player.getEnchantParams().isUseCommonScrollWhenSafe)
		{
			page = page.replaceFirst("%common_scrolls_for_safe_button%", setButton("common_for_safe 0", "Off", 30, 22));
		}
		else
		{
			page = page.replaceFirst("%common_scrolls_for_safe_button%", setButton("common_for_safe 1", "On", 30, 22));
		}

        final ItemInstance enchantItem = player.getEnchantParams().upgradeItem;
		if (enchantItem == null)
		{
            page = page.replaceFirst("%max_enchant%", unknown);
            page = page.replaceFirst("%upgrade_item_limit%", unknown);
			page = page.replaceFirst("%item_limit_button%", setButton("", ServerStorage.getInstance().getString(player.getLang(), "Enchant.CHANGE"), 62, 22));
			page = page.replaceFirst("%max_enchant_button%", setButton("", ServerStorage.getInstance().getString(player.getLang(), "Enchant.CHANGE"), 62, 22));
            return page;
        }
		
		if (!player.getEnchantParams().isChangingMaxEnchant)
		{
			page = page.replaceFirst("%max_enchant_button%", setButton("max_enchant", ServerStorage.getInstance().getString(player.getLang(), "Enchant.CHANGE"), 62, 22));
            if (EnchantUtils.getInstance().isAttribute(enchantItem))
			{
				page = page.replaceFirst("%max_enchant%", setColor(player.getEnchantParams().maxEnchantAtt, fieldColor));
			}
			else
			{
				page = page.replaceFirst("%max_enchant%", setColor("+" + player.getEnchantParams().maxEnchant, fieldColor));
			}
		}
		else
		{
            page = page.replaceFirst("%max_enchant%", setTextBox("max_enchant", 38, 12));
			page = page.replaceFirst("%max_enchant_button%", Matcher.quoteReplacement(setButton("max_enchant $max_enchant", ServerStorage.getInstance().getString(player.getLang(), "Enchant.TODO"), 62, 22)));
        }

		if (!player.getEnchantParams().isChangingUpgradeItemLimit)
		{
            page = page.replaceFirst("%upgrade_item_limit%", setColor(player.getEnchantParams().upgradeItemLimit, fieldColor));
			page = page.replaceFirst("%item_limit_button%", setButton("item_limit", ServerStorage.getInstance().getString(player.getLang(), "Enchant.CHANGE"), 62, 22));
		}
		else
		{
            page = page.replaceFirst("%upgrade_item_limit%", setTextBox("upgrade_item_limit", 38, 12));
			page = page.replaceFirst("%item_limit_button%", Matcher.quoteReplacement(setButton("item_limit $upgrade_item_limit", ServerStorage.getInstance().getString(player.getLang(), "Enchant.TODO"), 62, 22)));
        }
        return page;
    }

	private String setAttribute(Player player, ItemInstance item, String page)
	{
        String attr = "";
        int power = 0;
		if (item.isWeapon() || item.isArmor())
		{
			if (item.getElementals() != null)
			{
				if (item.isWeapon())
				{
					if (item.getElementals()[0].getElement() == item.getElementals()[0].getFire())
					{
						attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "f72c31");
						power += item.getElementals()[0].getValue();
					}
					if (item.getElementals()[0].getElement() == item.getElementals()[0].getWater())
					{
						attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "1892ef");
						power += item.getElementals()[0].getValue();
					}
					if (item.getElementals()[0].getElement() == item.getElementals()[0].getWind())
					{
						attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "7bbebd");
						power += item.getElementals()[0].getValue();
					}
					if (item.getElementals()[0].getElement() == item.getElementals()[0].getEarth())
					{
						attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "298a08");
						power += item.getElementals()[0].getValue();
					}
					if (item.getElementals()[0].getElement() == item.getElementals()[0].getHoly())
					{
						attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "dedfde");;
						power += item.getElementals()[0].getValue();
					}
					if (item.getElementals()[0].getElement() == item.getElementals()[0].getUnholy())
					{
						attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "9533b1");;
						power += item.getElementals()[0].getValue();
					}
				}
				else if (item.isArmor())
				{
					for (final Elementals elm : item.getElementals())
					{
						if (elm.getElement() == elm.getFire())
						{
							attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "f72c31");
							power += elm.getValue();
						}
						
						if (elm.getElement() == elm.getWater())
						{
							attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "1892ef");
							power += elm.getValue();
						}
						if (elm.getElement() == elm.getWind())
						{
							attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "7bbebd");
							power += elm.getValue();
						}
						if (elm.getElement() == elm.getEarth())
						{
							attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "298a08");
							power += elm.getValue();
						}
						if (elm.getElement() == elm.getHoly())
						{
							attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "dedfde");;
							power += elm.getValue();
						}
						if (elm.getElement() == elm.getUnholy())
						{
							attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "9533b1");;
							power += elm.getValue();
						}
					}
				}
			}
        }
		else
		{
			if (item.getItem().getElemental(Elementals.FIRE) != null)
			{
				attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "f72c31");
				power += item.getItem().getElemental(Elementals.FIRE).getValue();
			}
			else if (item.getItem().getElemental(Elementals.WATER) != null)
			{
				attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "1892ef");
				power += item.getItem().getElemental(Elementals.WATER).getValue();
			}
			
			if (item.getItem().getElemental(Elementals.WIND) != null)
			{
				attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "7bbebd");
				power += item.getItem().getElemental(Elementals.WIND).getValue();
			}
			else if (item.getItem().getElemental(Elementals.EARTH) != null)
			{
				attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "298a08");
				power += item.getItem().getElemental(Elementals.EARTH).getValue();
			}
			
			if (item.getItem().getElemental(Elementals.HOLY) != null)
			{
				attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "dedfde");;
				power += item.getItem().getElemental(Elementals.HOLY).getValue();
			}
			else if (item.getItem().getElemental(Elementals.DARK) != null)
			{
				attr += setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "9533b1");;
				power += item.getItem().getElemental(Elementals.DARK).getValue();
			}
        }

        if (attr.equals(""))
		{
			attr = setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.NO1"), fieldColor);
		}

        page = page.replaceFirst("%item_att%", attr);
        page = page.replaceFirst("%item_att_lvl%", "" + power);
        return page;
    }

	private String setAttributeById(Player player, int id)
	{
        if (id == 9546 || id == 9552)
		{
			return setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "f72c31");
		}
        if (id == 9547 || id == 9553)
		{
			return setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "1892ef");
		}
        if (id == 9548 || id == 9554)
		{
			return setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "298a08");
		}
        if (id == 9549 || id == 9555)
		{
			return setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "7bbebd");
		}
        if (id == 9550 || id == 9556)
		{
			return setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "9533b1");
		}
        if (id == 9551 || id == 9557)
		{
			return setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.ICON"), "dedfde");
		}
        return "";
    }

	private String getPageButtons(int count, int type, int sort_type, int activePage)
	{
        String buttons = "<br><center><table><tr>";
		for (int i = 1; i <= count; i++)
		{
            if (i == activePage)
			{
				buttons += "<td>" + setPressButton("item_choose " + type + "-" + sort_type + "-" + i, "" + i, 22, 22) + "</td>";
			}
			else
			{
				buttons += "<td>" + setButton("item_choose " + type + "-" + sort_type + "-" + i, "" + i, 22, 22) + "</td>";
			}
        }
        buttons += "</tr></table></center>";
        return buttons;
    }

	private String parseItemChoosePage(Player player, String page, int itemType, int sort_type, int page_index)
	{
		final int itemsOnPage = 3;
        String content = "";
		final StringBuilder template = new StringBuilder(HtmCache.getInstance().getHtm(player, player.getLang(), itemType == 0 ? "data/html/mods/autoenchant/enchant_item_obj.htm" : "data/html/mods/autoenchant/enchant_upgrade_item_obj.htm"));
        List<ItemInstance> items = EnchantUtils.getInstance().getWeapon(player);
		switch (itemType)
		{
            case 0:
				switch (sort_type)
				{
                    case 0:
                        items = EnchantUtils.getInstance().getWeapon(player);
                        break;
                    case 1:
                        items = EnchantUtils.getInstance().getArmor(player);
                        break;
                    case 2:
                        items = EnchantUtils.getInstance().getJewelry(player);
                        break;
                }
                break;
            case 1:
				if (sort_type == 0 && !Config.ENCHANT_ALLOW_SCROLLS)
				{
					sort_type++;
				}
				if (sort_type == 1 && !Config.ENCHANT_ALLOW_ATTRIBUTE)
				{
					if (Config.ENCHANT_ALLOW_SCROLLS)
					{
						sort_type--;
					}
					else
					{
						sort_type++;
					}
                }
				switch (sort_type)
				{
                    case 0:
                        items = EnchantUtils.getInstance().getScrolls(player);
                        break;
                    case 1:
                        items = EnchantUtils.getInstance().getAtributes(player);
                        break;
                }
                break;
        }
		content += getMenuButtons(player, sort_type, itemType);
        final StringBuilder parsed_items = new StringBuilder();
        final int page_count = (items.size() + itemsOnPage) / itemsOnPage;
		if (items.size() > itemsOnPage)
		{
            if (page_index > page_count)
			{
				page_index = page_count;
			}
			for (int i = page_index * itemsOnPage - itemsOnPage, startIdx = i; i < items.size() && i < startIdx + itemsOnPage; i++)
			{
                if (itemType == 0)
				{
					parsed_items.append(setTargetItem(player, items.get(i), template.toString()));
				}
				else
				{
					parsed_items.append(setEnchantItem(player, items.get(i), template.toString()));
				}
            }
            parsed_items.append(new StringBuilder(getPageButtons(page_count, itemType, sort_type, page_index)));
        }
		else
		{
			for (final ItemInstance item : items)
			{
                if (itemType == 0)
				{
					parsed_items.append(new StringBuilder(setTargetItem(player, item, template.toString())));
				}
				else
				{
					parsed_items.append(new StringBuilder(setEnchantItem(player, item, template.toString())));
				}
            }
		}
        if (parsed_items.toString().equals(""))
		{
			parsed_items.append(new StringBuilder(setCenter(setColor(ServerStorage.getInstance().getString(player.getLang(), "Enchant.NO_AVALIABLE"), mainColor))));
		}
        content += parsed_items;
        return page.replaceFirst("%content%", content);
    }

	private String getMenuButtons(Player player, int activeButton, int item_type)
	{
        String buttons = "<center><table border=0><tr>";
        final int summaryWidth = 240;
        final int height = 25;
		final String[][] itemButtons =
		{
		        {
		                ServerStorage.getInstance().getString(player.getLang(), "Enchant.WEAPON"), ServerStorage.getInstance().getString(player.getLang(), "Enchant.ARMOR"), ServerStorage.getInstance().getString(player.getLang(), "Enchant.JEWEL")
				},
				{
				        Config.ENCHANT_ALLOW_SCROLLS ? ServerStorage.getInstance().getString(player.getLang(), "Enchant.ENCHANT_SCROLL") : "unallowed", Config.ENCHANT_ALLOW_ATTRIBUTE ? ServerStorage.getInstance().getString(player.getLang(), "Enchant.ENCHANT_ATT") : "unallowed"
				}
		};
		
		for (int i = 0; i < itemButtons[item_type].length; i++)
		{
            if (itemButtons[item_type][i].equals("unallowed"))
			{
				continue;
			}
            if (i == activeButton)
			{
				buttons += "<td>" + setPressButton("item_choose " + item_type + "-" + i + "-1", itemButtons[item_type][i], summaryWidth / itemButtons[item_type].length, height) + "</td>";
			}
			else
			{
				buttons += "<td>" + setButton("item_choose " + item_type + "-" + i + "-1", itemButtons[item_type][i], summaryWidth / itemButtons[item_type].length, height) + "</td>";
			}
        }
        buttons += "</tr></table></center>";
        return buttons;
    }
}