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

import java.util.HashMap;
import java.util.Map;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.DressArmorParser;
import l2e.gameserver.data.parser.DressCloakParser;
import l2e.gameserver.data.parser.DressHatParser;
import l2e.gameserver.data.parser.DressShieldParser;
import l2e.gameserver.data.parser.DressWeaponParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.DressArmorTemplate;
import l2e.gameserver.model.actor.templates.DressCloakTemplate;
import l2e.gameserver.model.actor.templates.DressHatTemplate;
import l2e.gameserver.model.actor.templates.DressShieldTemplate;
import l2e.gameserver.model.actor.templates.DressWeaponTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.type.ItemType;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class DressMe implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
	        "dressme", "dress", "dressme-armor", "dressme-cloak", "dressme-shield", "dressme-hat", "dressme-weapon", "dress-armor", "dress-cloak", "dress-shield", "dress-hat", "dress-weapon", "dress-armorpage", "dress-cloakpage", "dress-shieldpage", "dress-hatpage", "dress-weaponpage", "dress-tryarmor", "dress-trycloak", "dress-tryshield", "dress-tryhat", "dress-tryweapon", "dressinfo", "undressme", "undressme-armor", "undressme-cloak", "undressme-shield", "undressme-hat", "undressme-weapon", "showdress", "hidedress"
	};
	
	private static Map<Integer, DressWeaponTemplate> SWORD = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> BLUNT = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> DAGGER = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> BOW = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> POLE = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> FIST = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> DUAL = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> DUALFIST = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> BIGSWORD = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> ROD = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> BIGBLUNT = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> CROSSBOW = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> RAPIER = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> ANCIENTSWORD = new HashMap<>();
	private static Map<Integer, DressWeaponTemplate> DUALDAGGER = new HashMap<>();
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String args)
	{
		if (!Config.ALLOW_VISUAL_ARMOR_COMMAND)
		{
			return false;
		}
		
		if (command.equals("dressme"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/index.htm");
			html = html.replace("<?show_hide?>", player.getVar("showVisualChange") == null ? "Show visual equip on other player!" : "Hide visual equip on other player!");
			html = html.replace("<?show_hide_b?>", player.getVar("showVisualChange") == null ? "showdress" : "hidedress");
			Util.setHtml(html, player);
		}
		else if (command.equals("dressme-armor"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/index-armor.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/template-armor.htm");
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 6;
			int counter = 0;
			
			final boolean isThereNextPage = DressArmorParser.getInstance().size() > perpage;
			
			for (int i = (page - 1) * perpage; i < DressArmorParser.getInstance().size(); i++)
			{
				final DressArmorTemplate dress = DressArmorParser.getInstance().getArmor(i + 1);
				if (dress != null)
				{
					block = template;
					
					String dress_name = dress.getName(player.getLang());
					
					if (dress_name.length() > 25)
					{
						dress_name = dress_name.substring(0, 25) + ".";
					}
					
					block = block.replace("{bypass}", "bypass -h .dress-armorpage " + (i + 1));
					block = block.replace("{name}", dress_name);
					block = block.replace("{price}", Util.formatPay(player, dress.getPriceCount(), dress.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(dress.getChest()));
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			final int count = (int) Math.ceil((double) DressArmorParser.getInstance().size() / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, DressArmorParser.getInstance().size(), perpage, isThereNextPage, ".dressme-armor %s"));
			Util.setHtml(html, player);
		}
		else if (command.equals("dressme-cloak"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/index-cloak.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/template-cloak.htm");
			String block = "";
			String list = "";
			
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 6;
			int counter = 0;

			final boolean isThereNextPage = DressCloakParser.getInstance().size() > perpage;
			
			for (int i = (page - 1) * perpage; i < DressCloakParser.getInstance().size(); i++)
			{
				final DressCloakTemplate cloak = DressCloakParser.getInstance().getCloak(i + 1);
				if (cloak != null)
				{
					block = template;
					
					String cloak_name = cloak.getName(player.getLang());
					
					if (cloak_name.length() > 25)
					{
						cloak_name = cloak_name.substring(0, 25) + ".";
					}
					
					block = block.replace("{bypass}", "bypass -h .dress-cloakpage " + (i + 1));
					block = block.replace("{name}", cloak_name);
					block = block.replace("{price}", Util.formatPay(player, cloak.getPriceCount(), cloak.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(cloak.getCloakId()));
					list += block;
				}
				else
				{
					_log.info("No Cloak!!!");
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			final int count = (int) Math.ceil((double) DressCloakParser.getInstance().size() / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, DressCloakParser.getInstance().size(), perpage, isThereNextPage, ".dressme-cloak %s"));
			Util.setHtml(html, player);
		}
		else if (command.equals("dressme-shield"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/index-shield.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/template-shield.htm");
			String block = "";
			String list = "";
			
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 6;
			int counter = 0;

			final boolean isThereNextPage = DressShieldParser.getInstance().size() > perpage;
			
			for (int i = (page - 1) * perpage; i < DressShieldParser.getInstance().size(); i++)
			{
				final DressShieldTemplate shield = DressShieldParser.getInstance().getShield(i + 1);
				if (shield != null)
				{
					block = template;
					
					String shield_name = shield.getName(player.getLang());
					
					if (shield_name.length() > 25)
					{
						shield_name = shield_name.substring(0, 25) + ".";
					}
					
					block = block.replace("{bypass}", "bypass -h .dress-shieldpage " + (i + 1));
					block = block.replace("{name}", shield_name);
					block = block.replace("{price}", Util.formatPay(player, shield.getPriceCount(), shield.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(shield.getShieldId()));
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			final int count = (int) Math.ceil((double) DressShieldParser.getInstance().size() / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, DressShieldParser.getInstance().size(), perpage, isThereNextPage, ".dressme-shield %s"));
			Util.setHtml(html, player);
		}
		else if (command.equals("dressme-hat"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/index-hat.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/template-hat.htm");
			String block = "";
			String list = "";
			
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 6;
			int counter = 0;
			
			final boolean isThereNextPage = DressHatParser.getInstance().size() > perpage;
			
			for (int i = (page - 1) * perpage; i < DressHatParser.getInstance().size(); i++)
			{
				final DressHatTemplate hat = DressHatParser.getInstance().getHat(i + 1);
				if (hat != null)
				{
					block = template;
					
					String hat_name = hat.getName(player.getLang());
					
					if (hat_name.length() > 25)
					{
						hat_name = hat_name.substring(0, 25) + ".";
					}
					
					block = block.replace("{bypass}", "bypass -h .dress-hatpage " + (i + 1));
					block = block.replace("{name}", hat_name);
					block = block.replace("{price}", Util.formatPay(player, hat.getPriceCount(), hat.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(hat.getHatId()));
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			final int count = (int) Math.ceil((double) DressHatParser.getInstance().size() / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, DressHatParser.getInstance().size(), perpage, isThereNextPage, ".dressme-hat %s"));
			Util.setHtml(html, player);
		}
		else if (command.startsWith("dressme-weapon"))
		{
			final ItemInstance slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (slot == null)
			{
				player.sendMessage((new ServerMessage("DressMe.NO_WEAPON", player.getLang())).toString());
				return false;
			}
			
			final ItemType type = slot.getItemType();
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/index-weapon.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/template-weapon.htm");
			String block = "";
			String list = "";
			
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 6;
			int counter = 0;
			Map<Integer, DressWeaponTemplate> map = new HashMap<>();
			
			map = initMap(type.toString(), map);
			
			if (map == null)
			{
				_log.warn("Dress me system: Weapon Map is null.");
				return false;
			}

			final boolean isThereNextPage = map.size() > perpage;
			
			for (int i = (page - 1) * perpage; i < map.size(); i++)
			{
				final DressWeaponTemplate weapon = map.get(i + 1);
				if (weapon != null)
				{
					block = template;
					
					String weapon_name = weapon.getName(player.getLang());
					
					if (weapon_name.length() > 25)
					{
						weapon_name = weapon_name.substring(0, 25) + ".";
					}
					
					block = block.replace("{bypass}", "bypass -h .dress-weaponpage " + weapon.getId());
					block = block.replace("{name}", weapon_name);
					block = block.replace("{price}", Util.formatPay(player, weapon.getPriceCount(), weapon.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(weapon.getId()));
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			final int count = (int) Math.ceil((double) map.size() / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, map.size(), perpage, isThereNextPage, ".dressme-weapon %s"));
			Util.setHtml(html, player);
		}
		else if (command.equals("dress-armorpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			final DressArmorTemplate dress = DressArmorParser.getInstance().getArmor(set);
			if (dress != null)
			{
				String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/dress-armor.htm");
				
				final ItemInstance my_chest = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
				html = html.replace("{my_chest_icon}", my_chest == null ? "icon.NOIMAGE" : my_chest.getItem().getIcon());
				final ItemInstance my_legs = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
				html = html.replace("{my_legs_icon}", my_legs == null ? "icon.NOIMAGE" : my_legs.getItem().getIcon());
				final ItemInstance my_gloves = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
				html = html.replace("{my_gloves_icon}", my_gloves == null ? "icon.NOIMAGE" : my_gloves.getItem().getIcon());
				final ItemInstance my_feet = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);
				html = html.replace("{my_feet_icon}", my_feet == null ? "icon.NOIMAGE" : my_feet.getItem().getIcon());
				
				html = html.replace("{bypassBuy}", "bypass -h .dress-armor " + set);
				html = html.replace("{bypassTry}", "bypass -h .dress-tryarmor " + set);
				html = html.replace("{name}", dress.getName(player.getLang()));
				html = html.replace("{price}", Util.formatPay(player, dress.getPriceCount(), dress.getPriceId()));
				
				final Item chest = ItemsParser.getInstance().getTemplate(dress.getChest());
				String chest_name = chest.getName(player.getLang());
				if (chest_name.length() > 25)
				{
					chest_name = chest_name.substring(0, 25) + ".";
				}
				html = html.replace("{chest_icon}", chest.getIcon());
				html = html.replace("{chest_name}", chest_name);
				html = html.replace("{chest_grade}", chest.getItemsGrade(chest.getCrystalType()));
				
				if (dress.getLegs() != -1)
				{
					final Item legs = ItemsParser.getInstance().getTemplate(dress.getLegs());
					String legs_name = legs.getName(player.getLang());
					if (legs_name.length() > 25)
					{
						legs_name = legs_name.substring(0, 25) + ".";
					}
					html = html.replace("{legs_icon}", legs.getIcon());
					html = html.replace("{legs_name}", legs_name);
					html = html.replace("{legs_grade}", legs.getItemsGrade(legs.getCrystalType()));
				}
				else
				{
					html = html.replace("{legs_icon}", "icon.NOIMAGE");
					html = html.replace("{legs_name}", "<font color=FF0000>...</font>");
					html = html.replace("{legs_grade}", "NO");
				}
				if (dress.getGloves() != -1)
				{
					final Item gloves = ItemsParser.getInstance().getTemplate(dress.getGloves());
					String gloves_name = gloves.getName(player.getLang());
					if (gloves_name.length() > 25)
					{
						gloves_name = gloves_name.substring(0, 25) + ".";
					}
					html = html.replace("{gloves_icon}", gloves.getIcon());
					html = html.replace("{gloves_name}", gloves_name);
					html = html.replace("{gloves_grade}", gloves.getItemsGrade(gloves.getCrystalType()));
				}
				else
				{
					html = html.replace("{gloves_icon}", "icon.NOIMAGE");
					html = html.replace("{gloves_name}", "<font color=FF0000>...</font>");
					html = html.replace("{gloves_grade}", "NO");
				}
				
				if (dress.getFeet() != -1)
				{
					final Item feet = ItemsParser.getInstance().getTemplate(dress.getFeet());
					String feet_name = feet.getName(player.getLang());
					if (feet_name.length() > 25)
					{
						feet_name = feet_name.substring(0, 25) + ".";
					}
					html = html.replace("{feet_icon}", feet.getIcon());
					html = html.replace("{feet_name}", feet_name);
					html = html.replace("{feet_grade}", feet.getItemsGrade(feet.getCrystalType()));
				}
				else
				{
					html = html.replace("{feet_icon}", "icon.NOIMAGE");
					html = html.replace("{feet_name}", "<font color=FF0000>...</font>");
					html = html.replace("{feet_grade}", "NO");
				}
				Util.setHtml(html, player);
			}
		}
		else if (command.equals("dress-cloakpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			final DressCloakTemplate cloak = DressCloakParser.getInstance().getCloak(set);
			if (cloak != null)
			{
				String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/dress-cloak.htm");
				
				final ItemInstance my_cloak = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
				html = html.replace("{my_cloak_icon}", my_cloak == null ? "icon.NOIMAGE" : my_cloak.getItem().getIcon());
				
				String cloak_name = cloak.getName(player.getLang());
				if (cloak_name.length() > 25)
				{
					cloak_name = cloak_name.substring(0, 25) + ".";
				}
				html = html.replace("{bypassBuy}", "bypass -h .dress-cloak " + cloak.getId());
				html = html.replace("{bypassTry}", "bypass -h .dress-trycloak " + cloak.getId());
				html = html.replace("{name}", cloak_name);
				html = html.replace("{price}", Util.formatPay(player, cloak.getPriceCount(), cloak.getPriceId()));
				
				final Item item = ItemsParser.getInstance().getTemplate(cloak.getCloakId());
				String cloak_name1 = item.getName(player.getLang());
				if (cloak_name1.length() > 25)
				{
					cloak_name1 = cloak_name1.substring(0, 25) + ".";
				}
				html = html.replace("{item_icon}", item.getIcon());
				html = html.replace("{item_name}", cloak_name1);
				html = html.replace("{item_grade}", item.getItemsGrade(item.getCrystalType()));
				Util.setHtml(html, player);
			}
		}
		else if (command.equals("dress-shieldpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			final DressShieldTemplate shield = DressShieldParser.getInstance().getShield(set);
			if (shield != null)
			{
				String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/dress-shield.htm");
				
				final ItemInstance my_shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
				html = html.replace("{my_shield_icon}", my_shield == null ? "icon.NOIMAGE" : my_shield.getItem().getIcon());
				
				String shield_myName = shield.getName(player.getLang());
				if (shield_myName.length() > 25)
				{
					shield_myName = shield_myName.substring(0, 25) + ".";
				}
				html = html.replace("{bypassBuy}", "bypass -h .dress-shield " + shield.getId());
				html = html.replace("{bypassTry}", "bypass -h .dress-tryshield " + shield.getId());
				html = html.replace("{name}", shield_myName);
				html = html.replace("{price}", Util.formatPay(player, shield.getPriceCount(), shield.getPriceId()));
				
				final Item item = ItemsParser.getInstance().getTemplate(shield.getShieldId());
				String shield_name = item.getName(player.getLang());
				if (shield_name.length() > 25)
				{
					shield_name = shield_name.substring(0, 25) + ".";
				}
				html = html.replace("{item_icon}", item.getIcon());
				html = html.replace("{item_name}", shield_name);
				html = html.replace("{item_grade}", item.getItemsGrade(item.getCrystalType()));
				Util.setHtml(html, player);
			}
		}
		else if (command.equals("dress-hatpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			final DressHatTemplate hat = DressHatParser.getInstance().getHat(set);
			if (hat != null)
			{
				String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/dress-hat.htm");
				
				final ItemInstance my_hat = hat.getSlot() == 2 ? player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR) : player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
				html = html.replace("{my_hat_icon}", my_hat == null ? "icon.NOIMAGE" : my_hat.getItem().getIcon());
				
				String my_hatName = hat.getName(player.getLang());
				if (my_hatName.length() > 25)
				{
					my_hatName = my_hatName.substring(0, 25) + ".";
				}
				html = html.replace("{bypassBuy}", "bypass -h .dress-hat " + hat.getId());
				html = html.replace("{bypassTry}", "bypass -h .dress-tryhat " + hat.getId());
				html = html.replace("{name}", my_hatName);
				html = html.replace("{price}", Util.formatPay(player, hat.getPriceCount(), hat.getPriceId()));
				
				final Item item = ItemsParser.getInstance().getTemplate(hat.getHatId());
				String hat_name = item.getName(player.getLang());
				if (hat_name.length() > 25)
				{
					hat_name = hat_name.substring(0, 25) + ".";
				}
				html = html.replace("{item_icon}", item.getIcon());
				html = html.replace("{item_name}", hat_name);
				html = html.replace("{item_grade}", item.getItemsGrade(item.getCrystalType()));
				Util.setHtml(html, player);
			}
		}
		else if (command.equals("dress-weaponpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			final DressWeaponTemplate weapon = DressWeaponParser.getInstance().getWeapon(set);
			if (weapon != null)
			{
				String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/dress-weapon.htm");
				
				final ItemInstance my_weapon = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				
				html = html.replace("{my_weapon_icon}", my_weapon == null ? "icon.NOIMAGE" : my_weapon.getItem().getIcon());
				
				String my_weaponName = weapon.getName(player.getLang());
				if (my_weaponName.length() > 25)
				{
					my_weaponName = my_weaponName.substring(0, 25) + ".";
				}
				html = html.replace("{bypassBuy}", "bypass -h .dress-weapon " + weapon.getId());
				html = html.replace("{bypassTry}", "bypass -h .dress-tryweapon " + weapon.getId());
				html = html.replace("{name}", my_weaponName);
				html = html.replace("{price}", Util.formatPay(player, weapon.getPriceCount(), weapon.getPriceId()));
				
				final Item item = ItemsParser.getInstance().getTemplate(weapon.getId());
				String weapon_name = item.getName(player.getLang());
				if (weapon_name.length() > 25)
				{
					weapon_name = weapon_name.substring(0, 25) + ".";
				}
				html = html.replace("{item_icon}", item.getIcon());
				html = html.replace("{item_name}", weapon_name);
				html = html.replace("{item_grade}", item.getItemsGrade(item.getCrystalType()));
				Util.setHtml(html, player);
			}
		}
		else if (command.equals("dressinfo"))
		{
			final String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/info.htm");
			Util.setHtml(html, player);
		}
		else if (command.equals("dress-armor"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			
			final DressArmorTemplate dress = DressArmorParser.getInstance().getArmor(set);
			final ItemInstance chest = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if (chest == null)
			{
				player.sendMessage((new ServerMessage("DressMe.NO_CHEST", player.getLang())).toString());
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			
			final ItemInstance legs = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			if (legs == null && chest.getItem().getBodyPart() != Item.SLOT_FULL_ARMOR)
			{
				player.sendMessage((new ServerMessage("DressMe.NO_LEGS", player.getLang())).toString());
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			
			final ItemInstance gloves = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
			if (gloves == null)
			{
				player.sendMessage((new ServerMessage("DressMe.NO_GLOVES", player.getLang())).toString());
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			
			final ItemInstance feet = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);
			if (feet == null)
			{
				player.sendMessage((new ServerMessage("DressMe.NO_FEET", player.getLang())).toString());
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			
			if (dress.getShieldId() > 0 && DressShieldParser.getInstance().getShieldId(dress.getShieldId()) != -1)
			{
				final ItemInstance shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
				if (shield == null || (shield != null && shield.getItem().isArrow()))
				{
					player.sendMessage((new ServerMessage("DressMe.NO_SHIELD", player.getLang())).toString());
					useVoicedCommand("dress-armorpage", player, args);
					return false;
				}
			}
			
			if (dress.getCloakId() > 0 && DressCloakParser.getInstance().getCloakId(dress.getCloakId()) != -1)
			{
				final ItemInstance cloak = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
				if (cloak == null)
				{
					player.sendMessage((new ServerMessage("DressMe.NO_CLOAK", player.getLang())).toString());
					useVoicedCommand("dress-armorpage", player, args);
					return false;
				}
			}
			
			if (dress.getHatId() > 0 && DressHatParser.getInstance().getHatId(dress.getHatId()) != -1)
			{
				final ItemInstance hat = dress.getSlot() == 2 ? player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR) : player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
				if (hat == null)
				{
					player.sendMessage((new ServerMessage("DressMe.NO_HAT", player.getLang())).toString());
					useVoicedCommand("dress-armorpage", player, args);
					return false;
				}
			}
			
			if (!dress.isForKamael() && player.getRace() == Race.Kamael)
			{
				player.sendMessage((new ServerMessage("DressMe.NOT_SUIT", player.getLang())).toString());
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			
			if (player.getInventory().getItemByItemId(dress.getPriceId()) == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			if (player.getInventory().getItemByItemId(dress.getPriceId()).getCount() < dress.getPriceCount())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			
			player.destroyItemByItemId("Dress", dress.getPriceId(), dress.getPriceCount(), player, true);

			visuality(player, chest, dress.getChest());
			player.getInventory().unEquipItem(chest);
			player.getInventory().equipItem(chest);
			if (legs != null)
			{
				visuality(player, legs, dress.getLegs());
				player.getInventory().unEquipItem(legs);
				player.getInventory().equipItem(legs);
			}
			visuality(player, gloves, dress.getGloves());
			player.getInventory().unEquipItem(gloves);
			player.getInventory().equipItem(gloves);

			visuality(player, feet, dress.getFeet());
			player.getInventory().unEquipItem(feet);
			player.getInventory().equipItem(feet);
			
			if (dress.getShieldId() > 0 && DressShieldParser.getInstance().getShieldId(dress.getShieldId()) != -1)
			{
				final ItemInstance shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
				visuality(player, shield, DressShieldParser.getInstance().getShieldId(dress.getShieldId()));
				player.getInventory().unEquipItem(shield);
				player.getInventory().equipItem(shield);
			}
			if (dress.getCloakId() > 0 && DressCloakParser.getInstance().getCloakId(dress.getCloakId()) != -1)
			{
				final ItemInstance cloak = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
				visuality(player, cloak, DressCloakParser.getInstance().getCloakId(dress.getCloakId()));
				player.getInventory().unEquipItem(cloak);
				player.getInventory().equipItem(cloak);
			}
			if (dress.getHatId() > 0 && DressHatParser.getInstance().getHatId(dress.getHatId()) != -1)
			{
				final ItemInstance hat = dress.getSlot() == 2 ? player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR) : player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
				visuality(player, dress.getSlot() == 2 ? player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR) : player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2), DressHatParser.getInstance().getHatId(dress.getHatId()));
				player.getInventory().unEquipItem(hat);
				player.getInventory().equipItem(hat);
			}
			player.broadcastUserInfo(true);
		}
		else if (command.equals("dress-tryarmor"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			
			final DressArmorTemplate dress = DressArmorParser.getInstance().getArmor(set);
			if (dress == null)
			{
				return false;
			}
			
			if (!dress.isForKamael() && player.getRace() == Race.Kamael)
			{
				player.sendMessage((new ServerMessage("DressMe.NOT_SUIT", player.getLang())).toString());
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(0, dress.getShieldId(), dress.getGloves(), dress.getChest(), dress.getLegs(), dress.getFeet(), dress.getCloakId(), 0, 0);
				player.setRemovePreviewTask();
			}
			useVoicedCommand("dress-armorpage", player, args);
		}
		else if (command.equals("dress-cloak"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			
			final DressCloakTemplate cloak_data = DressCloakParser.getInstance().getCloak(set);
			
			final ItemInstance cloak = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
			if (cloak == null)
			{
				player.sendMessage((new ServerMessage("DressMe.NO_CLOAK", player.getLang())).toString());
				useVoicedCommand("dress-cloakpage", player, args);
				return false;
			}

			if (player.getInventory().getItemByItemId(cloak_data.getPriceId()) == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			if (player.getInventory().getItemByItemId(cloak_data.getPriceId()).getCount() < cloak_data.getPriceCount())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			player.destroyItemByItemId("Dress", cloak_data.getPriceId(), cloak_data.getPriceCount(), player, true);
			
			visuality(player, cloak, cloak_data.getCloakId());
			player.getInventory().unEquipItem(cloak);
			player.getInventory().equipItem(cloak);
			player.broadcastUserInfo(true);
			return true;
		}
		else if (command.equals("dress-trycloak"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			
			final DressCloakTemplate cloak_data = DressCloakParser.getInstance().getCloak(set);
			if (cloak_data == null)
			{
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(0, 0, 0, 0, 0, 0, cloak_data.getCloakId(), 0, 0);
				player.setRemovePreviewTask();
			}
			useVoicedCommand("dress-cloakpage", player, args);
			return false;
		}
		else if (command.equals("dress-shield"))
		{
			final int shield_id = Integer.parseInt(args.split(" ")[0]);
			
			final DressShieldTemplate shield_data = DressShieldParser.getInstance().getShield(shield_id);
			
			final ItemInstance shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (shield == null || (shield != null && shield.getItem().isArrow()))
			{
				player.sendMessage((new ServerMessage("DressMe.NO_SHIELD", player.getLang())).toString());
				useVoicedCommand("dress-shieldpage", player, args);
				return false;
			}
			
			if (player.getInventory().getItemByItemId(shield_data.getPriceId()) == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			if (player.getInventory().getItemByItemId(shield_data.getPriceId()).getCount() < shield_data.getPriceCount())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			player.destroyItemByItemId("Dress", shield_data.getPriceId(), shield_data.getPriceCount(), player, true);
			
			visuality(player, shield, shield_data.getShieldId());
			player.getInventory().unEquipItem(shield);
			player.getInventory().equipItem(shield);
			player.broadcastUserInfo(true);
		}
		else if (command.equals("dress-tryshield"))
		{
			final int shield_id = Integer.parseInt(args.split(" ")[0]);
			
			final DressShieldTemplate shield_data = DressShieldParser.getInstance().getShield(shield_id);
			if (shield_data == null)
			{
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(0, shield_data.getShieldId(), 0, 0, 0, 0, 0, 0, 0);
				player.setRemovePreviewTask();
			}
			useVoicedCommand("dress-shieldpage", player, args);
		}
		else if (command.equals("dress-hat"))
		{
			final int hat_id = Integer.parseInt(args.split(" ")[0]);
			
			final DressHatTemplate hat_data = DressHatParser.getInstance().getHat(hat_id);
			
			final ItemInstance hat = hat_data.getSlot() == 2 ? player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR) : player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
			if (hat == null)
			{
				player.sendMessage((new ServerMessage("DressMe.NO_HAT", player.getLang())).toString());
				useVoicedCommand("dress-hatpage", player, args);
				return false;
			}
			
			if (player.getInventory().getItemByItemId(hat_data.getPriceId()) == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			if (player.getInventory().getItemByItemId(hat_data.getPriceId()).getCount() < hat_data.getPriceCount())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			player.destroyItemByItemId("Dress", hat_data.getPriceId(), hat_data.getPriceCount(), player, true);
			
			visuality(player, hat, hat_data.getHatId());
			player.getInventory().unEquipItem(hat);
			player.getInventory().equipItem(hat);
			player.broadcastUserInfo(true);
		}
		else if (command.equals("dress-tryhat"))
		{
			final int hat_id = Integer.parseInt(args.split(" ")[0]);
			
			final DressHatTemplate hat_data = DressHatParser.getInstance().getHat(hat_id);
			if (hat_data == null)
			{
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(0, 0, 0, 0, 0, 0, 0, hat_data.getSlot() == 2 ? hat_data.getHatId() : 0, hat_data.getSlot() == 2 ? 0 : hat_data.getHatId());
				player.setRemovePreviewTask();
			}
			useVoicedCommand("dress-hatpage", player, args);
		}
		else if (command.equals("dress-weapon"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			
			final DressWeaponTemplate weapon_data = DressWeaponParser.getInstance().getWeapon(set);
			
			final ItemInstance weapon = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (weapon == null)
			{
				player.sendMessage((new ServerMessage("DressMe.NO_WEAPON", player.getLang())).toString());
				useVoicedCommand("dress-weaponpage", player, args);
				return false;
			}
			
			if (!weapon.getItemType().toString().equals(weapon_data.getType()))
			{
				player.sendMessage((new ServerMessage("DressMe.WRONG_TYPE", player.getLang())).toString());
				useVoicedCommand("dressme-weapon", player, null);
				return false;
			}
			
			if (player.getInventory().getItemByItemId(weapon_data.getPriceId()) == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			if (player.getInventory().getItemByItemId(weapon_data.getPriceId()).getCount() < weapon_data.getPriceCount())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			player.destroyItemByItemId("Dress", weapon_data.getPriceId(), weapon_data.getPriceCount(), player, true);

			visuality(player, weapon, weapon_data.getId());
			player.getInventory().unEquipItem(weapon);
			player.getInventory().equipItem(weapon);
			player.broadcastUserInfo(true);
		}
		else if (command.equals("dress-tryweapon"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			
			final DressWeaponTemplate weapon_data = DressWeaponParser.getInstance().getWeapon(set);
			if (weapon_data == null)
			{
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(weapon_data.getId(), 0, 0, 0, 0, 0, 0, 0, 0);
				player.setRemovePreviewTask();
			}
			useVoicedCommand("dress-weaponpage", player, args);
			return false;
		}
		else if (command.equals("undressme"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dressme/undressme.htm");
			html = html.replace("<?show_hide?>", player.getVar("showVisualChange") == null ? "Show visual equip on other player!" : "Hide visual equip on other player!");
			html = html.replace("<?show_hide_b?>", player.getVar("showVisualChange") == null ? "showdress" : "hidedress");
			Util.setHtml(html, player);
		}
		else if (command.equals("undressme-armor"))
		{
			final ItemInstance chest = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			final ItemInstance legs = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			final ItemInstance gloves = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
			final ItemInstance feet = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);
			if (chest != null)
			{
				visuality(player, chest, 0);
				player.getInventory().unEquipItem(chest);
				player.getInventory().equipItem(chest);
			}
			if (legs != null)
			{
				visuality(player, legs, 0);
				player.getInventory().unEquipItem(legs);
				player.getInventory().equipItem(legs);
			}
			if (gloves != null)
			{
				visuality(player, gloves, 0);
				player.getInventory().unEquipItem(gloves);
				player.getInventory().equipItem(gloves);
			}
			if (feet != null)
			{
				visuality(player, feet, 0);
				player.getInventory().unEquipItem(feet);
				player.getInventory().equipItem(feet);
			}
			player.broadcastUserInfo(true);

			useVoicedCommand("undressme", player, null);
		}
		else if (command.equals("undressme-cloak"))
		{
			final ItemInstance cloak = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
			if (cloak != null)
			{
				visuality(player, cloak, 0);
				player.getInventory().unEquipItem(cloak);
				player.getInventory().equipItem(cloak);
			}
			player.broadcastUserInfo(true);
			
			useVoicedCommand("undressme", player, null);
		}
		else if (command.equals("undressme-shield"))
		{
			final ItemInstance shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (shield != null && !shield.getItem().isArrow())
			{
				visuality(player, shield, 0);
				player.getInventory().unEquipItem(shield);
				player.getInventory().equipItem(shield);
			}
			player.broadcastUserInfo(true);
			
			useVoicedCommand("undressme", player, null);
		}
		else if (command.equals("undressme-hat"))
		{
			final ItemInstance hat = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
			final ItemInstance hat2 = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
			if (hat != null)
			{
				visuality(player, hat, 0);
				player.getInventory().unEquipItem(hat);
				player.getInventory().equipItem(hat);
			}
			if (hat2 != null)
			{
				visuality(player, hat2, 0);
				player.getInventory().unEquipItem(hat2);
				player.getInventory().equipItem(hat2);
			}
			player.broadcastUserInfo(true);
			
			useVoicedCommand("undressme", player, null);
		}
		else if (command.equals("undressme-weapon"))
		{
			final ItemInstance weapon = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (weapon != null)
			{
				visuality(player, weapon, 0);
				player.getInventory().unEquipItem(weapon);
				player.getInventory().equipItem(weapon);
			}
			player.broadcastUserInfo(true);
			
			useVoicedCommand("undressme", player, null);
		}
		else if (command.equals("showdress"))
		{
			if (player.getVar("showVisualChange") == null)
			{
				player.setVar("showVisualChange", "-1");
				player.broadcastCharInfo();
			}
			useVoicedCommand("dressme", player, null);
			return true;
		}
		else if (command.equals("hidedress"))
		{
			if (player.getVar("showVisualChange") != null)
			{
				player.unsetVar("showVisualChange");
				player.broadcastCharInfo();
			}
			useVoicedCommand("dressme", player, null);
		}
		return true;
	}

	private Map<Integer, DressWeaponTemplate> initMap(String type, Map<Integer, DressWeaponTemplate> map)
	{
		if (type.equals("Sword"))
		{
			return map = SWORD;
		}
		else if (type.equals("Blunt"))
		{
			return map = BLUNT;
		}
		else if (type.equals("Dagger"))
		{
			return map = DAGGER;
		}
		else if (type.equals("Bow"))
		{
			return map = BOW;
		}
		else if (type.equals("Pole"))
		{
			return map = POLE;
		}
		else if (type.equals("Fist"))
		{
			return map = FIST;
		}
		else if (type.equals("Dual Sword"))
		{
			return map = DUAL;
		}
		else if (type.equals("Dual Fist"))
		{
			return map = DUALFIST;
		}
		else if (type.equals("Big Sword"))
		{
			return map = BIGSWORD;
		}
		else if (type.equals("Rod"))
		{
			return map = ROD;
		}
		else if (type.equals("Big Blunt"))
		{
			return map = BIGBLUNT;
		}
		else if (type.equals("Crossbow"))
		{
			return map = CROSSBOW;
		}
		else if (type.equals("Rapier"))
		{
			return map = RAPIER;
		}
		else if (type.equals("Ancient"))
		{
			return map = ANCIENTSWORD;
		}
		else if (type.equals("Dual Dagger"))
		{
			return map = DUALDAGGER;
		}
		else
		{
			_log.warn(getClass().getSimpleName() + ": Unknown type: " + type);
			return null;
		}
	}

	public static int parseWeapon()
	{
		int Sword = 1, Blunt = 1, Dagger = 1, Bow = 1, Pole = 1, Fist = 1,
		        DualSword = 1, DualFist = 1, BigSword = 1, Rod = 1,
		        BigBlunt = 1, Crossbow = 1, Rapier = 1, AncientSword = 1,
		        DualDagger = 1;
		
		for (final DressWeaponTemplate weapon : DressWeaponParser.getInstance().getAllWeapons())
		{
			if (weapon.getType().equals("Sword"))
			{
				SWORD.put(Sword, weapon);
				Sword++;
			}
			else if (weapon.getType().equals("Blunt"))
			{
				BLUNT.put(Blunt, weapon);
				Blunt++;
			}
			else if (weapon.getType().equals("Dagger"))
			{
				DAGGER.put(Dagger, weapon);
				Dagger++;
			}
			else if (weapon.getType().equals("Bow"))
			{
				BOW.put(Bow, weapon);
				Bow++;
			}
			else if (weapon.getType().equals("Pole"))
			{
				POLE.put(Pole, weapon);
				Pole++;
			}
			else if (weapon.getType().equals("Fist"))
			{
				FIST.put(Fist, weapon);
				Fist++;
			}
			else if (weapon.getType().equals("Dual Sword"))
			{
				DUAL.put(DualSword, weapon);
				DualSword++;
			}
			else if (weapon.getType().equals("Dual Fist"))
			{
				DUALFIST.put(DualFist, weapon);
				DualFist++;
			}
			else if (weapon.getType().equals("Big Sword"))
			{
				BIGSWORD.put(BigSword, weapon);
				BigSword++;
			}
			else if (weapon.getType().equals("Rod"))
			{
				ROD.put(Rod, weapon);
				Rod++;
			}
			else if (weapon.getType().equals("Big Blunt"))
			{
				BIGBLUNT.put(BigBlunt, weapon);
				BigBlunt++;
			}
			else if (weapon.getType().equals("Crossbow"))
			{
				CROSSBOW.put(Crossbow, weapon);
				Crossbow++;
			}
			else if (weapon.getType().equals("Rapier"))
			{
				RAPIER.put(Rapier, weapon);
				Rapier++;
			}
			else if (weapon.getType().equals("Ancient"))
			{
				ANCIENTSWORD.put(AncientSword, weapon);
				AncientSword++;
			}
			else if (weapon.getType().equals("Dual Dagger"))
			{
				DUALDAGGER.put(DualDagger, weapon);
				DualDagger++;
			}
			else
			{
				_log.warn("DressMe: Can't find type: " + weapon.getType());
			}
		}
		return 0;
	}
	
	private void visuality(Player player, ItemInstance item, int visual)
	{
		item.setVisualItemId(visual);
		item.updateDatabase();
		
		if (visual > 0)
		{
			final ServerMessage msg = new ServerMessage("DressMe.CHANGE", player.getLang());
			msg.add(Util.getItemName(player, item.getId()));
			msg.add(Util.getItemName(player, visual));
			player.sendMessage(msg.toString());
		}
		else
		{
			final ServerMessage msg = new ServerMessage("DressMe.REMOVE", player.getLang());
			msg.add(Util.getItemName(player, item.getId()));
			player.sendMessage(msg.toString());
		}
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}