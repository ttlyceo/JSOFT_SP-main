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
package l2e.gameserver.handler.communityhandlers.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.FoundationParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.model.ForgeElement;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class CommunityForge extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityForge()
	{
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}
	
	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbsforge"
		};
	}

	@Override
	public void onBypassCommand(String command, Player player)
	{
		if (!checkCondition(player, new StringTokenizer(command, "_").nextToken(), false, false))
		{
			return;
		}
		
		String content = "";
		if (command.equals("_bbsforge"))
		{
			content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/index.htm");
		}
		else
		{
			if (command.equals("_bbsforge:enchant:list"))
			{
				content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/itemlist.htm");

				final ItemInstance head = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD);
				final ItemInstance chest = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
				final ItemInstance legs = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
				final ItemInstance gloves = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
				final ItemInstance feet = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);

				final ItemInstance lhand = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
				final ItemInstance rhand = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

				final ItemInstance lfinger = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER);
				final ItemInstance rfinger = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER);
				final ItemInstance neck = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK);
				final ItemInstance lear = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR);
				final ItemInstance rear = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR);

				final Map<Integer, String[]> data = new HashMap<>();

				data.put(Integer.valueOf(1), ForgeElement.generateEnchant(head, Config.BBS_FORGE_ENCHANT_MAX[1], 1, player));
				data.put(Integer.valueOf(6), ForgeElement.generateEnchant(chest, Config.BBS_FORGE_ENCHANT_MAX[1], 6, player));
				data.put(Integer.valueOf(11), ForgeElement.generateEnchant(legs, Config.BBS_FORGE_ENCHANT_MAX[1], 11, player));
				data.put(Integer.valueOf(10), ForgeElement.generateEnchant(gloves, Config.BBS_FORGE_ENCHANT_MAX[1], 10, player));
				data.put(Integer.valueOf(12), ForgeElement.generateEnchant(feet, Config.BBS_FORGE_ENCHANT_MAX[1], 12, player));

				data.put(Integer.valueOf(14), ForgeElement.generateEnchant(lfinger, Config.BBS_FORGE_ENCHANT_MAX[2], 14, player));
				data.put(Integer.valueOf(13), ForgeElement.generateEnchant(rfinger, Config.BBS_FORGE_ENCHANT_MAX[2], 13, player));
				data.put(Integer.valueOf(4), ForgeElement.generateEnchant(neck, Config.BBS_FORGE_ENCHANT_MAX[2], 4, player));
				data.put(Integer.valueOf(9), ForgeElement.generateEnchant(lear, Config.BBS_FORGE_ENCHANT_MAX[2], 9, player));
				data.put(Integer.valueOf(8), ForgeElement.generateEnchant(rear, Config.BBS_FORGE_ENCHANT_MAX[2], 8, player));

				data.put(Integer.valueOf(5), ForgeElement.generateEnchant(rhand, Config.BBS_FORGE_ENCHANT_MAX[0], 5, player));
				if (rhand != null && (rhand.getItem().getItemType() == WeaponType.BIGBLUNT || rhand.getItem().getItemType() == WeaponType.BOW || rhand.getItem().getItemType() == WeaponType.DUALDAGGER || rhand.getItem().getItemType() == WeaponType.ANCIENTSWORD || rhand.getItem().getItemType() == WeaponType.CROSSBOW || rhand.getItem().getItemType() == WeaponType.BIGBLUNT || rhand.getItem().getItemType() == WeaponType.BIGSWORD || rhand.getItem().getItemType() == WeaponType.DUALFIST || rhand.getItem().getItemType() == WeaponType.DUAL || rhand.getItem().getItemType() == WeaponType.POLE || rhand.getItem().getItemType() == WeaponType.FIST))
				{
					data.put(Integer.valueOf(7), new String[]
					{
					        rhand.getItem().getIcon(), new StringBuilder().append(rhand.getItem().getName(player.getLang())).append(" ").append(rhand.getEnchantLevel() > 0 ? new StringBuilder().append("+").append(rhand.getEnchantLevel()).toString() : "").toString(), "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.EMPTY") + "", "L2UI_CT1.ItemWindow_DF_SlotBox_Disable"
					});
				}
				else
				{
					data.put(Integer.valueOf(7), ForgeElement.generateEnchant(lhand, Config.BBS_FORGE_ENCHANT_MAX[0], 7, player));
				}
				content = content.replace("<?content?>", ForgeElement.page(player));

				for (final Entry<Integer, String[]> info : data.entrySet())
				{
					final int slot = info.getKey();
					final String[] array = info.getValue();
					content = content.replace(new StringBuilder().append("<?").append(slot).append("_icon?>").toString(), array[0]);
					content = content.replace(new StringBuilder().append("<?").append(slot).append("_name?>").toString(), array[1]);
					content = content.replace(new StringBuilder().append("<?").append(slot).append("_button?>").toString(), array[2]);
					content = content.replace(new StringBuilder().append("<?").append(slot).append("_pic?>").toString(), array[3]);
				}
				data.clear();
			}
			else if (command.startsWith("_bbsforge:enchant:item:"))
			{
				final String[] array = command.split(":");
				final int item = Integer.parseInt(array[3]);

				String name = ItemsParser.getInstance().getTemplate(Config.BBS_FORGE_ENCHANT_ITEM).getName(player.getLang());

				if (name.isEmpty())
				{
					name = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.NO_NAME") + "";
				}
				if ((item < 1) || (item > 14))
				{
					return;
				}
				final ItemInstance _item = player.getInventory().getPaperdollItem(item);
				if (_item == null)
				{
					player.sendMessage((new ServerMessage("ServiceBBS.YOU_REMOVE_ITEM", player.getLang())).toString());
					onBypassCommand("_bbsforge:enchant:list", player);
					return;
				}

				if (_item.isHeroItem())
				{
					player.sendMessage((new ServerMessage("ServiceBBS.CANT_ENCH_HERO_WEAPON", player.getLang())).toString());
					onBypassCommand("_bbsforge:enchant:list", player);
					return;
				}

				if (_item.getItem().isArrow())
				{
					player.sendMessage((new ServerMessage("ServiceBBS.CANT_ENCH_ARROW", player.getLang())).toString());
					onBypassCommand("_bbsforge:enchant:list", player);
					return;
				}

				content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/enchant.htm");

				String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/enchant_template.htm");

				template = template.replace("{icon}", _item.getItem().getIcon());
				String _name = _item.getItem().getName(player.getLang());
				_name = _name.replace(" {PvP}", "");

				if (_name.length() > 30)
				{
					_name = new StringBuilder().append(_name.substring(0, 29)).append("...").toString();
				}
				template = template.replace("{name}", _name);
				template = template.replace("{enchant}", _item.getEnchantLevel() <= 0 ? "" : new StringBuilder().append("+").append(_item.getEnchantLevel()).toString());
				template = template.replace("{msg}", "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.SELECT_ENCH_LEVEL") + "");

				final String button_tm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/enchant_button_template.htm");

				String button = null;
				String block = "";

				final int[] level = _item.isArmor() ? Config.BBS_FORGE_ARMOR_ENCHANT_LVL : _item.isWeapon() ? Config.BBS_FORGE_WEAPON_ENCHANT_LVL : Config.BBS_FORGE_JEWELS_ENCHANT_LVL;
				int index = 0;
				for (int i = 0; i < level.length; i++)
				{
					if (_item.getEnchantLevel() >= level[i])
					{
						continue;
					}
					
					block = button_tm;
					block = block.replace("{link}", new StringBuilder().append("bypass -h _bbsforge:enchant:").append(i * item).append(":").append(item).toString());
					block = block.replace("{value}", new StringBuilder().append("+").append(level[i]).append(" (").append(_item.isArmor() ? Config.BBS_FORGE_ENCHANT_PRICE_ARMOR[i] : _item.isWeapon() ? Config.BBS_FORGE_ENCHANT_PRICE_WEAPON[i] : Config.BBS_FORGE_ENCHANT_PRICE_JEWELS[i]).append(" ").append(name).append(")").toString());
					index++;
					if (index % 2 == 0)
					{
						if (index > 0)
						{
							block += "</tr>";
						}
						block += "<tr>";
					}
					button = new StringBuilder().append(button).append(block).toString();
				}

				template = template.replace("{button}", ((button == null) ? "" : button));

				content = content.replace("<?content?>", template);
			}
			else if (command.equals("_bbsforge:foundation:list"))
			{
				content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/foundationlist.htm");

				final ItemInstance head = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD);
				final ItemInstance chest = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
				final ItemInstance legs = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
				final ItemInstance gloves = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
				final ItemInstance feet = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);

				final ItemInstance lhand = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
				final ItemInstance rhand = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

				final ItemInstance lfinger = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER);
				final ItemInstance rfinger = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER);
				final ItemInstance neck = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK);
				final ItemInstance lear = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR);
				final ItemInstance rear = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR);

				final Map<Integer, String[]> data = new HashMap<>();

				data.put(Integer.valueOf(1), ForgeElement.generateFoundation(head, 1, player));
				data.put(Integer.valueOf(6), ForgeElement.generateFoundation(chest, 6, player));
				data.put(Integer.valueOf(11), ForgeElement.generateFoundation(legs, 11, player));
				data.put(Integer.valueOf(10), ForgeElement.generateFoundation(gloves, 10, player));
				data.put(Integer.valueOf(12), ForgeElement.generateFoundation(feet, 12, player));

				data.put(Integer.valueOf(14), ForgeElement.generateFoundation(lfinger, 14, player));
				data.put(Integer.valueOf(13), ForgeElement.generateFoundation(rfinger, 13, player));
				data.put(Integer.valueOf(4), ForgeElement.generateFoundation(neck, 4, player));
				data.put(Integer.valueOf(9), ForgeElement.generateFoundation(lear, 9, player));
				data.put(Integer.valueOf(8), ForgeElement.generateFoundation(rear, 8, player));

				data.put(Integer.valueOf(5), ForgeElement.generateFoundation(rhand, 5, player));
				if (rhand != null && (rhand.getItem().getItemType() == WeaponType.BIGBLUNT || rhand.getItem().getItemType() == WeaponType.BOW || rhand.getItem().getItemType() == WeaponType.DUALDAGGER || rhand.getItem().getItemType() == WeaponType.ANCIENTSWORD || rhand.getItem().getItemType() == WeaponType.CROSSBOW || rhand.getItem().getItemType() == WeaponType.BIGBLUNT || rhand.getItem().getItemType() == WeaponType.BIGSWORD || rhand.getItem().getItemType() == WeaponType.DUALFIST || rhand.getItem().getItemType() == WeaponType.DUAL || rhand.getItem().getItemType() == WeaponType.POLE || rhand.getItem().getItemType() == WeaponType.FIST))
				{
					data.put(Integer.valueOf(7), new String[]
					{
					        rhand.getItem().getIcon(), new StringBuilder().append(rhand.getItem().getName(player.getLang())).append(" ").append(rhand.getEnchantLevel() > 0 ? new StringBuilder().append("+").append(rhand.getEnchantLevel()).toString() : "").toString(), "<font color=\"FF0000\">...</font>", "L2UI_CT1.ItemWindow_DF_SlotBox_Disable"
					});
				}
				else
				{
					data.put(Integer.valueOf(7), ForgeElement.generateFoundation(lhand, 7, player));
				}
				content = content.replace("<?content?>", ForgeElement.page(player));

				for (final Entry<Integer, String[]> info : data.entrySet())
				{
					final int slot = info.getKey();
					final String[] array = info.getValue();
					content = content.replace(new StringBuilder().append("<?").append(slot).append("_icon?>").toString(), array[0]);
					content = content.replace(new StringBuilder().append("<?").append(slot).append("_name?>").toString(), array[1]);
					content = content.replace(new StringBuilder().append("<?").append(slot).append("_button?>").toString(), array[2]);
					content = content.replace(new StringBuilder().append("<?").append(slot).append("_pic?>").toString(), array[3]);
				}
			}
			else
			{
				if (command.startsWith("_bbsforge:foundation:item:"))
				{
					final String[] array = command.split(":");
					final int item = Integer.parseInt(array[3]);

					if ((item < 1) || (item > 14))
					{
						return;
					}
					final ItemInstance _item = player.getInventory().getPaperdollItem(item);
					if (_item == null)
					{
						player.sendMessage((new ServerMessage("ServiceBBS.YOU_REMOVE_ITEM", player.getLang())).toString());
						onBypassCommand("_bbsforge:foundation:list", player);
						return;
					}

					if (_item.isHeroItem())
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_ENCH_HERO_WEAPON", player.getLang())).toString());
						onBypassCommand("_bbsforge:foundation:list", player);
						return;
					}
					
					if (!player.isInventoryUnder90(false))
					{
						player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
						return;
					}

					final int found = FoundationParser.getInstance().getFoundation(_item.getId());
					if (found == -1)
					{
						player.sendMessage((new ServerMessage("ServiceBBS.YOU_REMOVE_ITEM", player.getLang())).toString());
						onBypassCommand("_bbsforge:foundation:list", player);
						return;
					}

					final int price;
					if (_item.getItem().isAccessory())
					{
						price = Config.BBS_FORGE_FOUNDATION_PRICE_JEWEL[_item.getItem().getCrystalType()];
					}
					else if (_item.isWeapon())
					{
						price = Config.BBS_FORGE_FOUNDATION_PRICE_WEAPON[_item.getItem().getCrystalType()];
					}
					else
					{
						price = Config.BBS_FORGE_FOUNDATION_PRICE_ARMOR[_item.getItem().getCrystalType()];
					}

					if (player.getInventory().getItemByItemId(Config.BBS_FORGE_FOUNDATION_ITEM) == null)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					if (player.getInventory().getItemByItemId(Config.BBS_FORGE_FOUNDATION_ITEM).getCount() < price)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}

					if (player.getInventory().destroyItemByObjectId(_item.getObjectId(), _item.getCount(), player, true) != null)
					{
						player.destroyItemByItemId("ForgeBBS", Config.BBS_FORGE_FOUNDATION_ITEM, price, player, true);
						final ItemInstance _found = player.getInventory().addItem("ForgeBBS", found, 1, player, true);
						_found.setEnchantLevel(_item.getEnchantLevel());
						_found.setAugmentation(_item.getAugmentation());
						if (_item.getElementals() != null)
						{
							for (final Elementals elm : _item.getElementals())
							{
								if (elm.getElement() != -1 && elm.getValue() != -1)
								{
									_found.setElementAttr(elm.getElement(), elm.getValue());
								}
							}
						}
						player.getInventory().equipItem(_found);
						Util.addServiceLog(player.getName(null) + " buy foundation item: " + _found.getName(null) + " +" + _item.getEnchantLevel());
						final InventoryUpdate iu = new InventoryUpdate();
						iu.addItem(_found);
						iu.addModifiedItem(_found);
						player.sendPacket(iu);

						player.sendMessage(new StringBuilder().append("" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.EXCHANGE_ITEM") + " ").append(_item.getItem().getName(player.getLang())).append(" " + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.TO_FOUNDATION") + " ").append(_found.getItem().getName(player.getLang())).toString());
					}
					else
					{
						player.sendMessage((new ServerMessage("ServiceBBS.FOUNDATION_FAIL", player.getLang())).toString());
					}
					onBypassCommand("_bbsforge:foundation:list", player);
					return;
				}
				if (command.startsWith("_bbsforge:enchant:"))
				{
					final String[] array = command.split(":");

					final int val = Integer.parseInt(array[2]);
					final int item = Integer.parseInt(array[3]);

					final int conversion = val / item;

					final ItemInstance _item = player.getInventory().getPaperdollItem(item);
					if (_item == null)
					{
						player.sendMessage((new ServerMessage("ServiceBBS.YOU_REMOVE_ITEM", player.getLang())).toString());
						onBypassCommand("_bbsforge:enchant:list", player);
						return;
					}

					if (_item.isHeroItem())
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_ENCH_HERO_WEAPON", player.getLang())).toString());
						onBypassCommand("_bbsforge:enchant:list", player);
						return;
					}

					final int[] level = _item.isArmor() ? Config.BBS_FORGE_ARMOR_ENCHANT_LVL : _item.isWeapon() ? Config.BBS_FORGE_WEAPON_ENCHANT_LVL : Config.BBS_FORGE_JEWELS_ENCHANT_LVL;
					final int Value = level[conversion];

					final int max = _item.isArmor() ? Config.BBS_FORGE_ENCHANT_MAX[1] : _item.isWeapon() ? Config.BBS_FORGE_ENCHANT_MAX[0] : Config.BBS_FORGE_ENCHANT_MAX[2];
					if (Value > max)
					{
						return;
					}
					if (_item.getItem().isArrow())
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_ENCH_ARROW", player.getLang())).toString());
						onBypassCommand("_bbsforge:enchant:list", player);
						return;
					}

					final int price = _item.isArmor() ? Config.BBS_FORGE_ENCHANT_PRICE_ARMOR[conversion] : _item.isWeapon() ? Config.BBS_FORGE_ENCHANT_PRICE_WEAPON[conversion] : Config.BBS_FORGE_ENCHANT_PRICE_JEWELS[conversion];

					if (player.getInventory().getItemByItemId(Config.BBS_FORGE_ENCHANT_ITEM) == null)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					if (player.getInventory().getItemByItemId(Config.BBS_FORGE_ENCHANT_ITEM).getCount() < price)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}

					player.destroyItemByItemId("ForgeBBS", Config.BBS_FORGE_ENCHANT_ITEM, price, player, true);
					_item.setEnchantLevel(Value);
					_item.updateDatabase();
					if ((_item.getItem() instanceof Armor) && (_item.getEnchantLevel() == 4))
					{
						final Skill enchant4Skill = ((Armor) _item.getItem()).getEnchant4Skill();
						if (enchant4Skill != null)
						{
							player.addSkill(enchant4Skill, false);
							player.sendSkillList(false);
						}
					}
					Util.addServiceLog(player.getName(null) + " buy enchant service for item: " + _item.getName(null) + " +" + _item.getEnchantLevel());
					final InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(_item);
					player.sendPacket(iu);
					player.broadcastCharInfo();

					final ServerMessage msg = new ServerMessage("ServiceBBS.ITEM_ENCHANT", player.getLang());
					msg.add(_item.getItem().getName(player.getLang()));
					msg.add(Value);
					player.sendMessage(msg.toString());

					onBypassCommand("_bbsforge:enchant:list", player);
					return;
				}
				if (command.equals("_bbsforge:attribute:list"))
				{
					content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/attributelist.htm");

					final ItemInstance head = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD);
					final ItemInstance chest = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
					final ItemInstance legs = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
					final ItemInstance gloves = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
					final ItemInstance feet = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);

					final ItemInstance lhand = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
					final ItemInstance rhand = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

					final ItemInstance lfinger = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER);
					final ItemInstance rfinger = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER);
					final ItemInstance neck = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK);
					final ItemInstance lear = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR);
					final ItemInstance rear = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR);

					final Map<Integer, String[]> data = new HashMap<>();

					data.put(Integer.valueOf(1), ForgeElement.generateAttribution(head, 1, player, true));
					data.put(Integer.valueOf(6), ForgeElement.generateAttribution(chest, 6, player, true));
					data.put(Integer.valueOf(11), ForgeElement.generateAttribution(legs, 11, player, true));
					data.put(Integer.valueOf(10), ForgeElement.generateAttribution(gloves, 10, player, true));
					data.put(Integer.valueOf(12), ForgeElement.generateAttribution(feet, 12, player, true));

					data.put(Integer.valueOf(14), ForgeElement.generateAttribution(lfinger, 14, player, true));
					data.put(Integer.valueOf(13), ForgeElement.generateAttribution(rfinger, 13, player, true));
					data.put(Integer.valueOf(4), ForgeElement.generateAttribution(neck, 4, player, true));
					data.put(Integer.valueOf(9), ForgeElement.generateAttribution(lear, 9, player, true));
					data.put(Integer.valueOf(8), ForgeElement.generateAttribution(rear, 8, player, true));

					data.put(Integer.valueOf(5), ForgeElement.generateAttribution(rhand, 5, player, true));
					if (rhand != null && (rhand.getItem().getItemType() == WeaponType.BIGBLUNT || rhand.getItem().getItemType() == WeaponType.BOW || rhand.getItem().getItemType() == WeaponType.DUALDAGGER || rhand.getItem().getItemType() == WeaponType.ANCIENTSWORD || rhand.getItem().getItemType() == WeaponType.CROSSBOW || rhand.getItem().getItemType() == WeaponType.BIGBLUNT || rhand.getItem().getItemType() == WeaponType.BIGSWORD || rhand.getItem().getItemType() == WeaponType.DUALFIST || rhand.getItem().getItemType() == WeaponType.DUAL || rhand.getItem().getItemType() == WeaponType.POLE || rhand.getItem().getItemType() == WeaponType.FIST))
					{
						data.put(Integer.valueOf(7), new String[]
						{
						        rhand.getItem().getIcon(), new StringBuilder().append(rhand.getItem().getName(player.getLang())).append(" ").append(rhand.getEnchantLevel() > 0 ? new StringBuilder().append("+").append(rhand.getEnchantLevel()).toString() : "").toString(), "<font color=\"FF0000\">...</font>", "L2UI_CT1.ItemWindow_DF_SlotBox_Disable"
						});
					}
					else
					{
						data.put(Integer.valueOf(7), ForgeElement.generateAttribution(lhand, 7, player, true));
					}
					content = content.replace("<?content?>", ForgeElement.page(player));

					for (final Entry<Integer, String[]> info : data.entrySet())
					{
						final int slot = info.getKey();
						final String[] array = info.getValue();
						content = content.replace(new StringBuilder().append("<?").append(slot).append("_icon?>").toString(), array[0]);
						content = content.replace(new StringBuilder().append("<?").append(slot).append("_name?>").toString(), array[1]);
						content = content.replace(new StringBuilder().append("<?").append(slot).append("_button?>").toString(), array[2]);
						content = content.replace(new StringBuilder().append("<?").append(slot).append("_pic?>").toString(), array[3]);
					}
				}
				else if (command.startsWith("_bbsforge:attribute:item:"))
				{
					final String[] array = command.split(":");
					final int item = Integer.parseInt(array[3]);

					if ((item < 1) || (item > 14))
					{
						return;
					}
					final ItemInstance _item = player.getInventory().getPaperdollItem(item);
					if (_item == null)
					{
						player.sendMessage((new ServerMessage("ServiceBBS.YOU_REMOVE_ITEM", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					if (!ForgeElement.itemCheckGrade(true, _item))
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_GRADE_ENCHANT", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					if (_item.isHeroItem())
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_ENCH_HERO_WEAPON", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					if (_item.getItem().isAccessory())
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CATN_ENCH_JEVERLY", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					if (_item.getItem().isShield())
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CATN_ENCH_SHIELD", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}
					content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/attribute.htm");

					final String slotclose = "<img src=\"L2UI_CT1.ItemWindow_DF_SlotBox_Disable\" width=\"32\" height=\"32\">";
					String buttonFire = new StringBuilder().append("<button action=\"bypass -h _bbsforge:attribute:element:0:").append(item).append("\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"/>").toString();
					String buttonWater = new StringBuilder().append("<button action=\"bypass -h _bbsforge:attribute:element:1:").append(item).append("\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"/>").toString();
					String buttonWind = new StringBuilder().append("<button action=\"bypass -h _bbsforge:attribute:element:2:").append(item).append("\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"/>").toString();
					String buttonEarth = new StringBuilder().append("<button action=\"bypass -h _bbsforge:attribute:element:3:").append(item).append("\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"/>").toString();
					String buttonHoly = new StringBuilder().append("<button action=\"bypass -h _bbsforge:attribute:element:4:").append(item).append("\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"/>").toString();
					String buttonUnholy = new StringBuilder().append("<button action=\"bypass -h _bbsforge:attribute:element:5:").append(item).append("\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"/>").toString();

					if (_item.isWeapon())
					{
						if (_item.getElementals() != null && (_item.getElementals()[0].getElement() == _item.getElementals()[0].getFire()))
						{
							buttonWater = slotclose;
							buttonWind = slotclose;
							buttonEarth = slotclose;
							buttonHoly = slotclose;
							buttonUnholy = slotclose;
						}
						if (_item.getElementals() != null && (_item.getElementals()[0].getElement() == _item.getElementals()[0].getWater()))
						{
							buttonFire = slotclose;
							buttonWind = slotclose;
							buttonEarth = slotclose;
							buttonHoly = slotclose;
							buttonUnholy = slotclose;
						}
						if (_item.getElementals() != null && (_item.getElementals()[0].getElement() == _item.getElementals()[0].getWind()))
						{
							buttonWater = slotclose;
							buttonFire = slotclose;
							buttonEarth = slotclose;
							buttonHoly = slotclose;
							buttonUnholy = slotclose;
						}
						if (_item.getElementals() != null && (_item.getElementals()[0].getElement() == _item.getElementals()[0].getEarth()))
						{
							buttonWater = slotclose;
							buttonWind = slotclose;
							buttonFire = slotclose;
							buttonHoly = slotclose;
							buttonUnholy = slotclose;
						}
						if (_item.getElementals() != null && (_item.getElementals()[0].getElement() == _item.getElementals()[0].getHoly()))
						{
							buttonWater = slotclose;
							buttonWind = slotclose;
							buttonEarth = slotclose;
							buttonFire = slotclose;
							buttonUnholy = slotclose;
						}
						if (_item.getElementals() != null && (_item.getElementals()[0].getElement() == _item.getElementals()[0].getUnholy()))
						{
							buttonWater = slotclose;
							buttonWind = slotclose;
							buttonEarth = slotclose;
							buttonHoly = slotclose;
							buttonFire = slotclose;
						}
					}

					if (_item.isArmor())
					{
						if (_item.getElementals() != null)
						{
							for (final Elementals elm : _item.getElementals())
							{
								if (elm.getElement() == elm.getFire())
								{
									if (elm.getValue() >= Config.BBS_FORGE_ARMOR_ATTRIBUTE_MAX)
									{
										buttonFire = slotclose;
									}
									buttonWater = slotclose;
								}

								if (elm.getElement() == elm.getWater())
								{
									if (elm.getValue() >= Config.BBS_FORGE_ARMOR_ATTRIBUTE_MAX)
									{
										buttonWater = slotclose;
									}
									buttonFire = slotclose;
								}
								if (elm.getElement() == elm.getWind())
								{
									if (elm.getValue() >= Config.BBS_FORGE_ARMOR_ATTRIBUTE_MAX)
									{
										buttonWind = slotclose;
									}
									buttonEarth = slotclose;
								}
								if (elm.getElement() == elm.getEarth())
								{
									if (elm.getValue() >= Config.BBS_FORGE_ARMOR_ATTRIBUTE_MAX)
									{
										buttonEarth = slotclose;
									}
									buttonWind = slotclose;
								}
								if (elm.getElement() == elm.getHoly())
								{
									if (elm.getValue() >= Config.BBS_FORGE_ARMOR_ATTRIBUTE_MAX)
									{
										buttonHoly = slotclose;
									}
									buttonUnholy = slotclose;
								}
								if (elm.getElement() == elm.getUnholy())
								{
									if (elm.getValue() >= Config.BBS_FORGE_ARMOR_ATTRIBUTE_MAX)
									{
										buttonUnholy = slotclose;
									}
									buttonHoly = slotclose;
								}
							}
						}
					}

					String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/attribute_choice_template.htm");
					html = html.replace("{icon}", _item.getItem().getIcon());
					String _name = _item.getItem().getName(player.getLang());
					_name = _name.replace(" {PvP}", "");

					if (_name.length() > 30)
					{
						_name = new StringBuilder().append(_name.substring(0, 29)).append("...").toString();
					}
					html = html.replace("{name}", _name);
					html = html.replace("{enchant}", _item.getEnchantLevel() <= 0 ? "" : new StringBuilder().append(" +").append(_item.getEnchantLevel()).toString());
					html = html.replace("{msg}", "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.SELECT_ATTR") + "");
					html = html.replace("{fire}", buttonFire);
					html = html.replace("{water}", buttonWater);
					html = html.replace("{earth}", buttonEarth);
					html = html.replace("{wind}", buttonWind);
					html = html.replace("{holy}", buttonHoly);
					html = html.replace("{unholy}", buttonUnholy);

					content = content.replace("<?content?>", html);
				}
				else if (command.startsWith("_bbsforge:attribute:element:"))
				{
					final String[] array = command.split(":");
					final int element = Integer.parseInt(array[3]);

					String elementName = "";
					if (element == 0)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_FIRE") + "";
					}
					else if (element == 1)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_WATER") + "";
					}
					else if (element == 2)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_WIND") + "";
					}
					else if (element == 3)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_EARTH") + "";
					}
					else if (element == 4)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_HOLY") + "";
					}
					else if (element == 5)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_DARK") + "";
					}
					final int item = Integer.parseInt(array[4]);

					String name = ItemsParser.getInstance().getTemplate(Config.BBS_FORGE_ENCHANT_ITEM).getName(player.getLang());

					if (name.isEmpty())
					{
						name = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.NO_NAME") + "";
					}
					final ItemInstance _item = player.getInventory().getPaperdollItem(item);

					if (_item == null)
					{
						player.sendMessage((new ServerMessage("ServiceBBS.YOU_REMOVE_ITEM", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					if (!ForgeElement.itemCheckGrade(true, _item))
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_GRADE_ENCHANT", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					if (_item.isHeroItem())
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_ENCH_HERO_WEAPON", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					content = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/attribute.htm");
					
					String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/enchant_template.htm");
					template = template.replace("{icon}", _item.getItem().getIcon());
					String _name = _item.getItem().getName(player.getLang());
					_name = _name.replace(" {PvP}", "");

					if (_name.length() > 30)
					{
						_name = new StringBuilder().append(_name.substring(0, 29)).append("...").toString();
					}
					template = template.replace("{name}", _name);
					template = template.replace("{enchant}", _item.getEnchantLevel() <= 0 ? "" : new StringBuilder().append("+").append(_item.getEnchantLevel()).toString());
					template = template.replace("{msg}", "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.SELECTED") + " " + elementName + "");

					final String button_tm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/forge/enchant_button_template.htm");
					final StringBuilder button = new StringBuilder();
					String block = null;
					final int[] level = _item.isWeapon() ? Config.BBS_FORGE_ATRIBUTE_LVL_WEAPON : Config.BBS_FORGE_ATRIBUTE_LVL_ARMOR;
					int index = 0;
					for (int i = 0; i < level.length; i++)
					{
						final Elementals elementals = _item.getElementals() == null ? null : _item.getElementals()[0];
						
						if ((elementals != null) && (elementals.getElement() != -2) && (elementals.getElement() == Elementals.getElementById(element)) && elementals.getValue() >= (_item.isWeapon() ? Config.BBS_FORGE_ATRIBUTE_LVL_WEAPON[i] : Config.BBS_FORGE_ATRIBUTE_LVL_ARMOR[i]))
						{
							continue;
						}
						block = button_tm;
						block = block.replace("{link}", String.valueOf(new StringBuilder().append("bypass -h _bbsforge:attribute:").append(i * item).append(":").append(item).append(":").append(element).toString()));
						block = block.replace("{value}", new StringBuilder().append("+").append(_item.isWeapon() ? Config.BBS_FORGE_ATRIBUTE_LVL_WEAPON[i] : Config.BBS_FORGE_ATRIBUTE_LVL_ARMOR[i]).append(" (").append(_item.isWeapon() ? Config.BBS_FORGE_ATRIBUTE_PRICE_WEAPON[i] : Config.BBS_FORGE_ATRIBUTE_PRICE_ARMOR[i]).append(" ").append(name).append(")").toString());
						index++;
						if (index % 2 == 0)
						{
							if (index > 0)
							{
								block += "</tr>";
							}
							block += "<tr>";
						}
						button.append(block);
					}
					template = template.replace("{button}", button.toString());
					content = content.replace("<?content?>", template);
				}
				else if (command.startsWith("_bbsforge:attribute:"))
				{
					final String[] array = command.split(":");
					final int val = Integer.parseInt(array[2]);
					final int item = Integer.parseInt(array[3]);
					final int att = Integer.parseInt(array[4]);

					final ItemInstance _item = player.getInventory().getPaperdollItem(item);

					if (_item == null)
					{
						player.sendMessage((new ServerMessage("ServiceBBS.YOU_REMOVE_ITEM", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					if (!ForgeElement.itemCheckGrade(true, _item))
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_GRADE_ENCHANT", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					if (_item.isHeroItem())
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_ENCH_HERO_WEAPON", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					if ((_item.isArmor()) && (!ForgeElement.canEnchantArmorAttribute(att, _item)))
					{
						player.sendMessage((new ServerMessage("ServiceBBS.CANT_INSERT_ATTR", player.getLang())).toString());
						onBypassCommand("_bbsforge:attribute:list", player);
						return;
					}

					final int conversion = val / item;

					final int Value = _item.isWeapon() ? Config.BBS_FORGE_ATRIBUTE_LVL_WEAPON[conversion] : Config.BBS_FORGE_ATRIBUTE_LVL_ARMOR[conversion];

					if (Value > (_item.isWeapon() ? Config.BBS_FORGE_WEAPON_ATTRIBUTE_MAX : Config.BBS_FORGE_ARMOR_ATTRIBUTE_MAX))
					{
						return;
					}
					final int price = _item.isWeapon() ? Config.BBS_FORGE_ATRIBUTE_PRICE_WEAPON[conversion] : Config.BBS_FORGE_ATRIBUTE_PRICE_ARMOR[conversion];

					if (player.getInventory().getItemByItemId(Config.BBS_FORGE_ENCHANT_ITEM) == null)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					if (player.getInventory().getItemByItemId(Config.BBS_FORGE_ENCHANT_ITEM).getCount() < price)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}

					player.destroyItemByItemId("ForgeBBS", Config.BBS_FORGE_ENCHANT_ITEM, price, player, true);
					player.getInventory().unEquipItem(_item);
					_item.setElementAttr(Elementals.getElementById(att), Value);
					player.getInventory().equipItem(_item);
					Util.addServiceLog(player.getName(null) + " buy attribute service for item: " + _item.getName(null));
					final InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(_item);
					player.sendPacket(iu);
					player.broadcastCharInfo();

					String elementName = "";
					if (att == 0)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_FIRE") + "";
					}
					else if (att == 1)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_WATER") + "";
					}
					else if (att == 2)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_WIND") + "";
					}
					else if (att == 3)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_EARTH") + "";
					}
					else if (att == 4)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_HOLY") + "";
					}
					else if (att == 5)
					{
						elementName = "" + ServerStorage.getInstance().getString(player.getLang(), "ServiceBBS.ATTR_DARK") + "";
					}
					final ServerMessage msg = new ServerMessage("ServiceBBS.ATTR_ADDED", player.getLang());
					msg.add(_item.getItem().getName(player.getLang()));
					msg.add(elementName);
					msg.add(Value);
					player.sendMessage(msg.toString());

					onBypassCommand("_bbsforge:attribute:list", player);
					return;
				}
			}
		}
		separateAndSend(content, player);
	}
	
	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}
	
	public static CommunityForge getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityForge _instance = new CommunityForge();
	}
}