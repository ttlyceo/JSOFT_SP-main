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
 * this program. If not, see <>.
 */
package services;

import l2e.commons.util.Util;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.*;
import l2e.gameserver.handler.communityhandlers.impl.AbstractCommunity;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.*;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.type.ItemType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.SystemMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Create by LordWinter
 */
public class VisualMe implements IVoicedCommandHandler
{
	private final boolean _allowColorDesign = true;
	private final boolean _allowBuyVisual = true;
	
	private static final String[] VOICED_COMMANDS =
	{
		"visualme",
		"visualme-armor",
		"visualmy-armor",
		"visual-myarmorpage",
		"visualbuy-armor",
		"visual-tryarmor",
		"visualme-weapon",
		"visualmy-weapon",
		"visual-myweaponpage",
		"visualbuy-weapon",
		"visual-tryweapon",
		"visualme-shield",
		"visualmy-shield",
		"visual-myshieldpage",
		"visualbuy-shield",
		"visual-tryshield",
		"visualme-cloak",
		"visualmy-cloak",
		"visual-mycloakpage",
		"visualbuy-cloak",
		"visual-trycloak",
	        "visualme-hat",
		"visualmy-hat",
		"visual-myhatpage",
		"visualbuy-hat",
		"visual-tryhat",
		"unvisualmy-armor",
		"unvisualmy-weapon",
		"unvisualmy-shield",
		"unvisualmy-cloak",
	        "unvisualmy-hair",
		"unvisualmy-face",
		"visualme-selectarmor",
		"visualme-selectweapon",
		"visualme-selectshield",
	        "visualme-selectcloak",
		"visualme-selecthat"
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
		if (command.equals("visualme"))
		{
			useVoicedCommand("visualme-armor", player, null);
		}
		else if (command.equals("visualme-armor"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-armor.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-armor.htm");
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = DressArmorParser.getInstance().size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			for (int i = (page - 1) * perpage; i < DressArmorParser.getInstance().size(); i++)
			{
				final DressArmorTemplate visual = DressArmorParser.getInstance().getArmor(i + 1);
				if (visual != null)
				{
					boolean haveSkin = false;
					if (player.getArmorSkins().contains(i + 1))
					{
						haveSkin = true;
					}
					block = template;
					
					String visual_name = visual.getName(null);
					
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (haveSkin)
					{
						if (player.getActiveArmorSkin() != null && (i + 1) == player.getActiveArmorSkin().getId())
						{
							block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-armor 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						else
						{
							block = block.replace("{button}", "<button action=\"bypass -h .visualmy-armor " + (i + 1) + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
					}
					else
					{
						block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selectarmor " + visual.getChest() + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-armor " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					
					block = block.replace("{bypassTry}", "bypass -h .visual-tryarmor " + (i + 1) + "_0_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{price}", Util.formatPay(player, visual.getPriceCount(), visual.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(visual.getChest()));
					
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-armor %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualme-selectarmor"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-selectarmor.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-selectarmor.htm");
			String block = "";
			String list = "";
			
			final String[] subStr = args.split(" ")[0].split("_");
			final int skinId = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = DressArmorParser.getInstance().size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			
			int priceId = 0;
			long priceCount = 0;
			
			for (int i = (page - 1) * perpage; i < DressArmorParser.getInstance().size(); i++)
			{
				final DressArmorTemplate visual = DressArmorParser.getInstance().getArmor(i + 1);
				if (visual != null)
				{
					boolean haveSkin = false;
					if (player.getArmorSkins().contains(i + 1))
					{
						haveSkin = true;
					}
					block = template;
					
					String visual_name = visual.getName(null);
					
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (visual.getChest() == skinId)
					{
						block = block.replace("{select}", "<img src=\"L2UI.PETITEM_CLICK\" width=32 height=32>");
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						priceId = visual.getPriceId();
						priceCount = visual.getPriceCount();
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-armor " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					else
					{
						if (haveSkin)
						{
							if ((i + 1) == player.getActiveArmorSkin().getId())
							{
								block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-armor 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							else
							{
								block = block.replace("{button}", "<button action=\"bypass -h .visualmy-armor " + (i + 1) + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						}
						else
						{
							block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selectarmor " + visual.getChest() + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
							block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-armor " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
						}
					}
					
					block = block.replace("{bypassTry}", "bypass -h .visual-tryarmor " + (i + 1) + "_0_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{icon}", Util.getItemIcon(visual.getChest()));
					
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-armor %s"));
			html = html.replace("{priceItem}", Util.getItemName(player, priceId));
			html = html.replace("{priceAmount}", String.valueOf(priceCount));
			html = html.replace("{priceIcon}", Util.getItemIcon(priceId));
			
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visual-myarmorpage"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-myarmor.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-myarmor.htm");
			String block = "";
			String list = "";
			
			final String[] param = args.split(" ");
			int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = player.getArmorSkins().size();
			final boolean isThereNextPage = totalSize > perpage;
			
			final int lastPage = (totalSize / perpage) + 1;
			if (page > lastPage)
			{
				page = lastPage;
			}
			
			int countt = 0;
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final DressArmorTemplate visual = DressArmorParser.getInstance().getArmor(player.getArmorSkins().get(i));
				if (visual != null)
				{
					block = template;
					
					String visual_name = visual.getName(null);
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (player.getActiveArmorSkin() != null && visual.getId() == player.getActiveArmorSkin().getId())
					{
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-armor 1_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					else
					{
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .visualmy-armor " + visual.getId() + "_1_" + page + "\" value=\"Enable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					block = block.replace("{bypassTry}", "bypass -h .visual-tryarmor " + visual.getId() + "_1_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{icon}", Util.getItemIcon(visual.getChest()));
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visual-myarmorpage %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualbuy-armor"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int set = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final DressArmorTemplate visual = DressArmorParser.getInstance().getArmor(set);
			
			if (player.getInventory().getItemByItemId(visual.getPriceId()) == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			if (player.getInventory().getItemByItemId(visual.getPriceId()).getCount() < visual.getPriceCount())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			
			if (!visual.isForKamael() && player.getRace() == Race.Kamael)
			{
				player.sendMessage("Error: This dress doesn't suit you.");
				return false;
			}
			
			player.destroyItemByItemId("Visual", visual.getPriceId(), visual.getPriceCount(), player, true);
			player.sendMessage("You successfully bought visual skin " + visual.getName(null));
			
			buyVisuality(player, "Armor", set);
			useVoicedCommand("visual-myarmorpage", player, String.valueOf(page));
		}
		else if (command.equals("visualmy-armor"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int set = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final String page = subStr[2];
			
			final DressArmorTemplate visual = DressArmorParser.getInstance().getArmor(set);
			
			final ItemInstance chest = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if (chest == null)
			{
				player.sendMessage("Error: Chest must be equiped.");
				if (type == 1)
				{
					useVoicedCommand("visual-myarmorpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-armor", player, page);
				}
				return false;
			}
			
			final ItemInstance legs = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			if ((legs == null) && (chest.getItem().getBodyPart() != Item.SLOT_FULL_ARMOR))
			{
				player.sendMessage("Error: Legs must be equiped.");
				if (type == 1)
				{
					useVoicedCommand("visual-myarmorpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-armor", player, page);
				}
				return false;
			}
			
			final ItemInstance gloves = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
			if (gloves == null)
			{
				player.sendMessage("Error: Gloves must be equiped.");
				if (type == 1)
				{
					useVoicedCommand("visual-myarmorpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-armor", player, page);
				}
				return false;
			}
			
			final ItemInstance feet = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);
			if (feet == null)
			{
				player.sendMessage("Error: Feet must be equiped.");
				if (type == 1)
				{
					useVoicedCommand("visual-myarmorpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-armor", player, page);
				}
				return false;
			}
			
			if (visual.getShieldId() > 0 && DressShieldParser.getInstance().getShieldId(visual.getShieldId()) != -1)
			{
				final ItemInstance shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
				if (shield == null)
				{
					player.sendMessage("Error: Shield must be equiped.");
					if (type == 1)
					{
						useVoicedCommand("visual-myarmorpage", player, page);
					}
					else
					{
						useVoicedCommand("visualme-armor", player, page);
					}
					return false;
				}
			}
			
			if (visual.getCloakId() > 0 && DressCloakParser.getInstance().getCloakId(visual.getCloakId()) != -1)
			{
				final ItemInstance cloak = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
				if (cloak == null)
				{
					player.sendMessage("Error: Cloak must be equiped.");
					if (type == 1)
					{
						useVoicedCommand("visual-myarmorpage", player, page);
					}
					else
					{
						useVoicedCommand("visualme-armor", player, page);
					}
					return false;
				}
			}
			
			if (visual.getHatId() > 0 && DressHatParser.getInstance().getHatId(visual.getHatId()) != -1)
			{
				final Item template = ItemsParser.getInstance().getTemplate(visual.getHatId());
				final int paperdoll = Inventory.getPaperdollIndex(template.getBodyPart());
				
				final ItemInstance item = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
				if (item != null)
				{
					final Item itemTemp = ItemsParser.getInstance().getTemplate(item.getId());
					if (itemTemp.getBodyPart() == Item.SLOT_HAIRALL)
					{
						if (itemTemp.getBodyPart() != template.getBodyPart())
						{
							player.sendMessage("Error: Must be equiped correct item for skin.");
							if (type == 1)
							{
								useVoicedCommand("visual-myarmorpage", player, page);
							}
							else
							{
								useVoicedCommand("visualme-armor", player, page);
							}
							return false;
						}
					}
					else if (itemTemp.getBodyPart() == Item.SLOT_HAIR)
					{
						if (template.getBodyPart() == Item.SLOT_HAIRALL)
						{
							player.sendMessage("Error: Must be equiped correct item for skin.");
							if (type == 1)
							{
								useVoicedCommand("visual-myarmorpage", player, page);
							}
							else
							{
								useVoicedCommand("visualme-armor", player, page);
							}
							return false;
						}
					}
				}
				
				final ItemInstance hair = player.getInventory().getPaperdollItem(paperdoll);
				if (hair == null)
				{
					player.sendMessage("Error: Must be equiped slot for skin.");
					if (type == 1)
					{
						useVoicedCommand("visual-myarmorpage", player, page);
					}
					else
					{
						useVoicedCommand("visualme-armor", player, page);
					}
					return false;
				}
			}
			
			if (!visual.isForKamael() && player.getRace() == Race.Kamael)
			{
				player.sendMessage("Error: This dress doesn't suit you.");
				if (type == 1)
				{
					useVoicedCommand("visual-myarmorpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-armor", player, page);
				}
				return false;
			}
			
			if ((visual.getShieldId() > 0 && DressShieldParser.getInstance().getShieldId(visual.getShieldId()) != -1) || (visual.getCloakId() > 0 && DressCloakParser.getInstance().getCloakId(visual.getCloakId()) != -1) || (visual.getHatId() > 0 && DressHatParser.getInstance().getHatId(visual.getHatId()) != -1))
			{
				visuality(player, "Armor", set, false);
			}
			else
			{
				visuality(player, "Armor", set, true);
			}
			
			if (type == 1)
			{
				useVoicedCommand("visual-myarmorpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-armor", player, page);
			}
			player.broadcastCharInfo();
		}
		else if (command.equals("visual-tryarmor"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int set = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final int page = Integer.parseInt(subStr[2]);
			
			final DressArmorTemplate visual = DressArmorParser.getInstance().getArmor(set);
			if (visual == null)
			{
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(0, visual.getShieldId(), visual.getGloves(), visual.getChest(), visual.getLegs(), visual.getFeet(), visual.getCloakId(), 0, 0);
				player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
				player.setRemovePreviewTask();
			}
			
			if (type == 1)
			{
				useVoicedCommand("visual-myarmorpage", player, String.valueOf(page));
			}
			else
			{
				useVoicedCommand("visualme-armor", player, String.valueOf(page));
			}
		}
		else if (command.equals("unvisualmy-armor"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int type = Integer.parseInt(subStr[0]);
			final String page = subStr[1];

			final DressArmorTemplate visual = DressArmorParser.getInstance().getArmor(player.getActiveArmorSkin().getId());
			player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
			player.unVisualArmorSkin(true);
			if (visual != null)
			{
				if (visual.getShieldId() > 0)
				{
					player.unVisualShieldSkin(false);
				}

				if (visual.getCloakId() > 0)
				{
					player.unVisualCloakSkin(false);
				}

				if (visual.getHatId() > 0)
				{
					if (visual.getSlot() == 3)
					{
						player.unVisualFaceSkin(false);
					}
					else
					{
						player.unVisualHairSkin(false);
					}
				}
			}
			if (type == 1)
			{
				useVoicedCommand("visual-myarmorpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-armor", player, page);
			}
			player.broadcastUserInfo(true);
			player.sendUserInfo();
		}
		else if (command.startsWith("visualme-weapon"))
		{
			final ItemInstance slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (slot == null)
			{
				player.sendMessage("Error: Weapon must be equiped!");
				return false;
			}
			
			final ItemType type = slot.getItemType();
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-weapon.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-weapon.htm");
			String block = "";
			String list = "";
			
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			Map<Integer, DressWeaponTemplate> map = new HashMap<>();
			
			map = initMap(type.toString(), map);
			
			if (map == null)
			{
				_log.warn("Visual system: Weapon Map is null.");
				return false;
			}
			
			final int totalSize = map.size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			for (int i = (page - 1) * perpage; i < map.size(); i++)
			{
				final DressWeaponTemplate weapon = map.get(i + 1);
				if (weapon != null)
				{
					boolean haveSkin = false;
					if (player.getWeaponSkins().contains(weapon.getId()))
					{
						haveSkin = true;
					}
					block = template;
					
					String weapon_name = weapon.getName(null);
					
					if (weapon_name.length() > 14)
					{
						weapon_name = weapon_name.substring(0, 14) + ".";
					}
					
					if (haveSkin)
					{
						if (weapon.getId() == player.getActiveWeaponSkin().getId())
						{
							block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-weapon 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						else
						{
							block = block.replace("{button}", "<button action=\"bypass -h .visualmy-weapon " + weapon.getId() + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
					}
					else
					{
						block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selectweapon " + weapon.getId() + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-weapon " + weapon.getId() + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					
					block = block.replace("{bypassTry}", "bypass -h .visual-tryweapon " + weapon.getId() + "_0_" + page);
					block = block.replace("{name}", weapon_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{price}", Util.formatPay(player, weapon.getPriceCount(), weapon.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(weapon.getId()));
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-weapon %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualme-selectweapon"))
		{
			final ItemInstance slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (slot == null)
			{
				player.sendMessage("Error: Weapon must be equiped!");
				return false;
			}
			
			final ItemType type = slot.getItemType();
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-selectweapon.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-selectweapon.htm");
			String block = "";
			String list = "";
			
			final String[] subStr = args.split(" ")[0].split("_");
			final int skinId = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final int perpage = 12;
			int counter = 0;
			Map<Integer, DressWeaponTemplate> map = new HashMap<>();
			
			map = initMap(type.toString(), map);
			
			if (map == null)
			{
				_log.warn("Visual system: Weapon Map is null.");
				return false;
			}
			
			final int totalSize = map.size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			
			int priceId = 0;
			long priceCount = 0;
			
			for (int i = (page - 1) * perpage; i < map.size(); i++)
			{
				final DressWeaponTemplate weapon = map.get(i + 1);
				if (weapon != null)
				{
					boolean haveSkin = false;
					if (player.getWeaponSkins().contains(weapon.getId()))
					{
						haveSkin = true;
					}
					block = template;
					
					String weapon_name = weapon.getName(null);
					
					if (weapon_name.length() > 14)
					{
						weapon_name = weapon_name.substring(0, 14) + ".";
					}
					
					if (weapon.getId() == skinId)
					{
						block = block.replace("{select}", "<img src=\"L2UI.PETITEM_CLICK\" width=32 height=32>");
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						priceId = weapon.getPriceId();
						priceCount = weapon.getPriceCount();
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-weapon " + weapon.getId() + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					else
					{
						if (haveSkin)
						{
							if (weapon.getId() == player.getActiveWeaponSkin().getId())
							{
								block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-weapon 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							else
							{
								block = block.replace("{button}", "<button action=\"bypass -h .visualmy-weapon " + weapon.getId() + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						}
						else
						{
							block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selectweapon " + weapon.getId() + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
							block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-weapon " + weapon.getId() + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
						}
					}
					
					block = block.replace("{bypassTry}", "bypass -h .visual-tryweapon " + weapon.getId() + "_0_" + page);
					block = block.replace("{name}", weapon_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{icon}", Util.getItemIcon(weapon.getId()));
					
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-weapon %s"));
			html = html.replace("{priceItem}", Util.getItemName(player, priceId));
			html = html.replace("{priceAmount}", String.valueOf(priceCount));
			html = html.replace("{priceIcon}", Util.getItemIcon(priceId));
			
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visual-myweaponpage"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-myweapon.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-myweapon.htm");
			String block = "";
			String list = "";
			
			final String[] param = args.split(" ");
			int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = player.getWeaponSkins().size();
			final boolean isThereNextPage = totalSize > perpage;
			
			final int lastPage = (totalSize / perpage) + 1;
			if (page > lastPage)
			{
				page = lastPage;
			}
			
			int countt = 0;
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final DressWeaponTemplate visual = DressWeaponParser.getInstance().getWeapon(player.getWeaponSkins().get(i));
				if (visual != null)
				{
					block = template;
					
					String visual_name = visual.getName(null);
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (visual.getId() == player.getActiveWeaponSkin().getId())
					{
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-weapon 1_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					else
					{
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .visualmy-weapon " + visual.getId() + "_1_" + page + "\" value=\"Enable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					block = block.replace("{bypassTry}", "bypass -h .visual-tryweapon " + visual.getId() + "_1_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{icon}", Util.getItemIcon(visual.getId()));
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visual-myweaponpage %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualbuy-weapon"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int weapon = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final DressWeaponTemplate weapon_data = DressWeaponParser.getInstance().getWeapon(weapon);
			
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
			player.destroyItemByItemId("Visual", weapon_data.getPriceId(), weapon_data.getPriceCount(), player, true);
			player.sendMessage("You successfully bought visual skin " + weapon_data.getName(null));
			
			buyVisuality(player, "Weapon", weapon);
			useVoicedCommand("visual-myweaponpage", player, String.valueOf(page));
		}
		else if (command.equals("visualmy-weapon"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int weaponId = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final String page = subStr[2];
			
			final DressWeaponTemplate weapon_data = DressWeaponParser.getInstance().getWeapon(weaponId);
			
			final ItemInstance weapon = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (weapon == null)
			{
				player.sendMessage("Error: Weapon must be equiped.");
				if (type == 1)
				{
					useVoicedCommand("visual-myweaponpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-weapon", player, page);
				}
				return false;
			}
			
			if (!weapon.getItemType().toString().equals(weapon_data.getType()))
			{
				player.sendMessage("Error: Weapon must be equals type.");
				if (type == 1)
				{
					useVoicedCommand("visual-myweaponpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-weapon", player, page);
				}
				return false;
			}
			
			visuality(player, "Weapon", weaponId, true);
			if (type == 1)
			{
				useVoicedCommand("visual-myweaponpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-weapon", player, page);
			}
			player.broadcastCharInfo();
		}
		else if (command.equals("visual-tryweapon"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int weapon = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final int page = Integer.parseInt(subStr[2]);
			
			final DressWeaponTemplate weapon_data = DressWeaponParser.getInstance().getWeapon(weapon);
			if (weapon_data == null)
			{
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(weapon_data.getId(), 0, 0, 0, 0, 0, 0, 0, 0);
				player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
				player.setRemovePreviewTask();
			}
			
			if (type == 1)
			{
				useVoicedCommand("visual-myweaponpage", player, String.valueOf(page));
			}
			else
			{
				useVoicedCommand("visualme-weapon", player, String.valueOf(page));
			}
			return false;
		}
		else if (command.equals("unvisualmy-weapon"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int type = Integer.parseInt(subStr[0]);
			final String page = subStr[1];
			player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
			player.unVisualWeaponSkin(true);
			if (type == 1)
			{
				useVoicedCommand("visual-myweaponpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-weapon", player, page);
			}
			player.broadcastUserInfo(true);
			player.sendUserInfo();
		}
		else if (command.equals("visualme-shield"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-shield.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-shield.htm");
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = DressShieldParser.getInstance().size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			for (int i = (page - 1) * perpage; i < DressShieldParser.getInstance().size(); i++)
			{
				final DressShieldTemplate visual = DressShieldParser.getInstance().getShield(i + 1);
				if (visual != null)
				{
					boolean haveSkin = false;
					if (player.getShieldSkins().contains(i + 1))
					{
						haveSkin = true;
					}
					block = template;
					
					String visual_name = visual.getName(null);
					
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (haveSkin)
					{
						if ((i + 1) == player.getActiveShieldSkin().getId())
						{
							block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-shield 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						else
						{
							block = block.replace("{button}", "<button action=\"bypass -h .visualmy-shield " + (i + 1) + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
					}
					else
					{
						block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selectshield " + visual.getShieldId() + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-shield " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					
					block = block.replace("{bypassTry}", "bypass -h .visual-tryshield " + (i + 1) + "_0_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{price}", Util.formatPay(player, visual.getPriceCount(), visual.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(visual.getShieldId()));
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-shield %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualme-selectshield"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-selectshield.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-selectshield.htm");
			String block = "";
			String list = "";
			
			final String[] subStr = args.split(" ")[0].split("_");
			final int skinId = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = DressShieldParser.getInstance().size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			
			int priceId = 0;
			long priceCount = 0;
			
			for (int i = (page - 1) * perpage; i < DressShieldParser.getInstance().size(); i++)
			{
				final DressShieldTemplate visual = DressShieldParser.getInstance().getShield(i + 1);
				if (visual != null)
				{
					boolean haveSkin = false;
					if (player.getShieldSkins().contains(i + 1))
					{
						haveSkin = true;
					}
					block = template;
					
					String visual_name = visual.getName(null);
					
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (visual.getShieldId() == skinId)
					{
						block = block.replace("{select}", "<img src=\"L2UI.PETITEM_CLICK\" width=32 height=32>");
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						priceId = visual.getPriceId();
						priceCount = visual.getPriceCount();
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-shield " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					else
					{
						if (haveSkin)
						{
							if ((i + 1) == player.getActiveShieldSkin().getId())
							{
								block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-shield 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							else
							{
								block = block.replace("{button}", "<button action=\"bypass -h .visualmy-shield " + (i + 1) + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						}
						else
						{
							block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selectshield " + visual.getShieldId() + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
							block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-shield " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
						}
					}
					
					block = block.replace("{bypassTry}", "bypass -h .visual-tryshield " + (i + 1) + "_0_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{icon}", Util.getItemIcon(visual.getShieldId()));
					
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-shield %s"));
			html = html.replace("{priceItem}", Util.getItemName(player, priceId));
			html = html.replace("{priceAmount}", String.valueOf(priceCount));
			html = html.replace("{priceIcon}", Util.getItemIcon(priceId));
			
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visual-myshieldpage"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-myshield.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-myshield.htm");
			String block = "";
			String list = "";
			
			final String[] param = args.split(" ");
			int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = player.getShieldSkins().size();
			final boolean isThereNextPage = totalSize > perpage;
			
			final int lastPage = (totalSize / perpage) + 1;
			if (page > lastPage)
			{
				page = lastPage;
			}
			
			int countt = 0;
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final DressShieldTemplate visual = DressShieldParser.getInstance().getShield(player.getShieldSkins().get(i));
				if (visual != null)
				{
					block = template;
					
					String visual_name = visual.getName(null);
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (visual.getId() == player.getActiveShieldSkin().getId())
					{
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-shield 1_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					else
					{
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .visualmy-shield " + visual.getId() + "_1_" + page + "\" value=\"Enable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					block = block.replace("{bypassTry}", "bypass -h .visual-tryshield " + visual.getId() + "_1_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{icon}", Util.getItemIcon(visual.getShieldId()));
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visual-myshieldpage %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualbuy-shield"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int shield = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final DressShieldTemplate visual = DressShieldParser.getInstance().getShield(shield);
			
			if (player.getInventory().getItemByItemId(visual.getPriceId()) == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			if (player.getInventory().getItemByItemId(visual.getPriceId()).getCount() < visual.getPriceCount())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			
			player.destroyItemByItemId("Visual", visual.getPriceId(), visual.getPriceCount(), player, true);
			player.sendMessage("You successfully bought visual skin " + visual.getName(null));
			
			buyVisuality(player, "Shield", shield);
			useVoicedCommand("visual-myshieldpage", player, String.valueOf(page));
		}
		else if (command.equals("visualmy-shield"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int shieldId = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final String page = subStr[2];
			
			final ItemInstance shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (shield == null)
			{
				player.sendMessage("Error: Shield must be equiped.");
				if (type == 1)
				{
					useVoicedCommand("visual-myshieldpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-shield", player, page);
				}
				return false;
			}
			
			visuality(player, "Shield", shieldId, true);
			if (type == 1)
			{
				useVoicedCommand("visual-myshieldpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-shield", player, page);
			}
			player.broadcastCharInfo();
		}
		else if (command.equals("visual-tryshield"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int shield = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final int page = Integer.parseInt(subStr[2]);
			
			final DressShieldTemplate visual = DressShieldParser.getInstance().getShield(shield);
			if (visual == null)
			{
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(0, visual.getShieldId(), 0, 0, 0, 0, 0, 0, 0);
				player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
				player.setRemovePreviewTask();
			}
			
			if (type == 1)
			{
				useVoicedCommand("visual-myshieldpage", player, String.valueOf(page));
			}
			else
			{
				useVoicedCommand("visualme-shield", player, String.valueOf(page));
			}
		}
		else if (command.equals("unvisualmy-shield"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int type = Integer.parseInt(subStr[0]);
			final String page = subStr[1];
			player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
			player.unVisualShieldSkin(true);
			if (type == 1)
			{
				useVoicedCommand("visual-myshieldpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-shield", player, page);
			}
			player.broadcastUserInfo(true);
			player.sendUserInfo();
		}
		else if (command.equals("visualme-cloak"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-cloak.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-cloak.htm");
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = DressCloakParser.getInstance().size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			for (int i = (page - 1) * perpage; i < DressCloakParser.getInstance().size(); i++)
			{
				final DressCloakTemplate visual = DressCloakParser.getInstance().getCloak(i + 1);
				if (visual != null)
				{
					boolean haveSkin = false;
					if (player.getCloakSkins().contains(i + 1))
					{
						haveSkin = true;
					}
					block = template;
					
					String visual_name = visual.getName(null);
					
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (haveSkin)
					{
						if ((i + 1) == player.getActiveCloakSkin().getId())
						{
							block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-cloak 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						else
						{
							block = block.replace("{button}", "<button action=\"bypass -h .visualmy-cloak " + (i + 1) + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
					}
					else
					{
						block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selectcloak " + (i + 1) + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-cloak " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					
					block = block.replace("{bypassTry}", "bypass -h .visual-trycloak " + (i + 1) + "_0_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{price}", Util.formatPay(player, visual.getPriceCount(), visual.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(visual.getCloakId()));
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-cloak %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualme-selectcloak"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-selectcloak.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-selectcloak.htm");
			String block = "";
			String list = "";
			
			final String[] subStr = args.split(" ")[0].split("_");
			final int skinId = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = DressCloakParser.getInstance().size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			
			int priceId = 0;
			long priceCount = 0;
			
			for (int i = (page - 1) * perpage; i < DressCloakParser.getInstance().size(); i++)
			{
				final DressCloakTemplate visual = DressCloakParser.getInstance().getCloak(i + 1);
				if (visual != null)
				{
					boolean haveSkin = false;
					if (player.getCloakSkins().contains(i + 1))
					{
						haveSkin = true;
					}
					block = template;
					
					String visual_name = visual.getName(null);
					
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (visual.getId() == skinId)
					{
						block = block.replace("{select}", "<img src=\"L2UI.PETITEM_CLICK\" width=32 height=32>");
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						priceId = visual.getPriceId();
						priceCount = visual.getPriceCount();
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-cloak " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					else
					{
						if (haveSkin)
						{
							if ((i + 1) == player.getActiveCloakSkin().getId())
							{
								block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-cloak 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							else
							{
								block = block.replace("{button}", "<button action=\"bypass -h .visualmy-cloak " + (i + 1) + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						}
						else
						{
							block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selectcloak " + visual.getId() + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
							block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-cloak " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
						}
					}
					
					block = block.replace("{bypassTry}", "bypass -h .visual-trycloak " + visual.getId() + "_0_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{icon}", Util.getItemIcon(visual.getCloakId()));
					
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-cloak %s"));
			html = html.replace("{priceItem}", Util.getItemName(player, priceId));
			html = html.replace("{priceAmount}", String.valueOf(priceCount));
			html = html.replace("{priceIcon}", Util.getItemIcon(priceId));
			
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visual-mycloakpage"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-mycloak.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-mycloak.htm");
			String block = "";
			String list = "";
			
			final String[] param = args.split(" ");
			int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = player.getCloakSkins().size();
			final boolean isThereNextPage = totalSize > perpage;
			
			final int lastPage = (totalSize / perpage) + 1;
			if (page > lastPage)
			{
				page = lastPage;
			}
			
			int countt = 0;
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final DressCloakTemplate visual = DressCloakParser.getInstance().getCloak(player.getCloakSkins().get(i));
				if (visual != null)
				{
					block = template;
					
					String visual_name = visual.getName(null);
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (visual.getId() == player.getActiveCloakSkin().getId())
					{
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-cloak 1_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					else
					{
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .visualmy-cloak " + visual.getId() + "_1_" + page + "\" value=\"Enable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					block = block.replace("{bypassTry}", "bypass -h .visual-trycloak " + visual.getId() + "_1_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{icon}", Util.getItemIcon(visual.getCloakId()));
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visual-mycloakpage %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualbuy-cloak"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int cloak = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final DressCloakTemplate visual = DressCloakParser.getInstance().getCloak(cloak);
			
			if (player.getInventory().getItemByItemId(visual.getPriceId()) == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			if (player.getInventory().getItemByItemId(visual.getPriceId()).getCount() < visual.getPriceCount())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			
			player.destroyItemByItemId("Visual", visual.getPriceId(), visual.getPriceCount(), player, true);
			player.sendMessage("You successfully bought visual skin " + visual.getName(null));
			
			buyVisuality(player, "Cloak", cloak);
			useVoicedCommand("visual-mycloakpage", player, String.valueOf(page));
		}
		else if (command.equals("visualmy-cloak"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int cloakId = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final String page = subStr[2];
			
			final ItemInstance cloak = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
			if (cloak == null)
			{
				player.sendMessage("Error: Cloak must be equiped.");
				if (type == 1)
				{
					useVoicedCommand("visual-mycloakpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-cloak", player, page);
				}
				return false;
			}
			
			visuality(player, "Cloak", cloakId, true);
			if (type == 1)
			{
				useVoicedCommand("visual-mycloakpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-cloak", player, page);
			}
			player.broadcastCharInfo();
		}
		else if (command.equals("visual-trycloak"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int cloak = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final int page = Integer.parseInt(subStr[2]);
			
			final DressCloakTemplate visual = DressCloakParser.getInstance().getCloak(cloak);
			if (visual == null)
			{
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(0, 0, 0, 0, 0, 0, visual.getCloakId(), 0, 0);
				player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
				player.setRemovePreviewTask();
			}
			
			if (type == 1)
			{
				useVoicedCommand("visual-mycloakpage", player, String.valueOf(page));
			}
			else
			{
				useVoicedCommand("visualme-cloak", player, String.valueOf(page));
			}
		}
		else if (command.equals("unvisualmy-cloak"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int type = Integer.parseInt(subStr[0]);
			final String page = subStr[1];
			player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
			player.unVisualCloakSkin(true);
			if (type == 1)
			{
				useVoicedCommand("visual-mycloakpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-cloak", player, page);
			}
			player.broadcastUserInfo(true);
			player.sendUserInfo();
		}
		else if (command.equals("visualme-hat"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-hat.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-hat.htm");
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = DressHatParser.getInstance().size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			for (int i = (page - 1) * perpage; i < DressHatParser.getInstance().size(); i++)
			{
				final DressHatTemplate visual = DressHatParser.getInstance().getHat(i + 1);
				if (visual != null)
				{
					boolean haveSkin = false;
					if (player.getHairSkins().contains(i + 1))
					{
						haveSkin = true;
					}
					block = template;
					
					String visual_name = visual.getName(null);
					
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (haveSkin)
					{
						if ((i + 1) == player.getActiveHairSkin().getId())
						{
							block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-hair 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						else if ((i + 1) == player.getActiveMaskSkin().getId())
						{
							block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-face 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						else
						{
							block = block.replace("{button}", "<button action=\"bypass -h .visualmy-hat " + (i + 1) + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						
						block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
					}
					else
					{
						block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selecthat " + visual.getHatId() + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-hat " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					block = block.replace("{bypassTry}", "bypass -h .visual-tryhat " + (i + 1) + "_0_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{price}", Util.formatPay(player, visual.getPriceCount(), visual.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(visual.getHatId()));
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-hat %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualme-selecthat"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-selecthat.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-selecthat.htm");
			String block = "";
			String list = "";
			
			final String[] subStr = args.split(" ")[0].split("_");
			final int skinId = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = DressHatParser.getInstance().size();
			final boolean isThereNextPage = totalSize > perpage;
			int countt = 0;
			
			int priceId = 0;
			long priceCount = 0;
			
			for (int i = (page - 1) * perpage; i < DressHatParser.getInstance().size(); i++)
			{
				final DressHatTemplate visual = DressHatParser.getInstance().getHat(i + 1);
				if (visual != null)
				{
					boolean haveSkin = false;
					if (player.getHairSkins().contains(i + 1))
					{
						haveSkin = true;
					}
					block = template;
					
					String visual_name = visual.getName(null);
					
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (visual.getHatId() == skinId)
					{
						block = block.replace("{select}", "<img src=\"L2UI.PETITEM_CLICK\" width=32 height=32>");
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						priceId = visual.getPriceId();
						priceCount = visual.getPriceCount();
						block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-hat " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
					}
					else
					{
						if (haveSkin)
						{
							if ((i + 1) == player.getActiveHairSkin().getId())
							{
								block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-hair 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							else if ((i + 1) == player.getActiveMaskSkin().getId())
							{
								block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-face 0_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							else
							{
								block = block.replace("{button}", "<button action=\"bypass -h .visualmy-hat " + (i + 1) + "_0_" + page + "\" value=\"Use\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
							}
							block = block.replace("{select}", "<button value=\" \" action=\"\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						}
						else
						{
							block = block.replace("{select}", "<button value=\" \" action=\"bypass -h .visualme-selecthat " + visual.getHatId() + "_" + page + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
							block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Red" : "L2UI_CT1_CN.Gray");
							block = block.replace("{button}", _allowBuyVisual ? "<button action=\"bypass -h .visualbuy-hat " + (i + 1) + "_" + page + "\" value=\"Buy\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "<br><br><br>");
						}
					}
					block = block.replace("{bypassTry}", "bypass -h .visual-tryhat " + visual.getId() + "_0_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{lock}", haveSkin ? "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">" : "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
					block = block.replace("{icon}", Util.getItemIcon(visual.getHatId()));
					
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visualme-hat %s"));
			html = html.replace("{priceItem}", Util.getItemName(player, priceId));
			html = html.replace("{priceAmount}", String.valueOf(priceCount));
			html = html.replace("{priceIcon}", Util.getItemIcon(priceId));
			
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visual-myhatpage"))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/index-myhat.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/visual/template-myhat.htm");
			String block = "";
			String list = "";
			
			final String[] param = args.split(" ");
			int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 12;
			int counter = 0;
			
			final int totalSize = player.getHairSkins().size();
			final boolean isThereNextPage = totalSize > perpage;
			
			final int lastPage = (totalSize / perpage) + 1;
			if (page > lastPage)
			{
				page = lastPage;
			}
			
			int countt = 0;
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final DressHatTemplate visual = DressHatParser.getInstance().getHat(player.getHairSkins().get(i));
				if (visual != null)
				{
					block = template;
					
					String visual_name = visual.getName(null);
					if (visual_name.length() > 14)
					{
						visual_name = visual_name.substring(0, 14) + ".";
					}
					
					if (visual.getId() == player.getActiveHairSkin().getId())
					{
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-hair 1_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					else if (visual.getId() == player.getActiveMaskSkin().getId())
					{
						block = block.replace("{tableColor}", _allowColorDesign ? "L2UI_CT1_CN.Green" : "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_unlock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .unvisualmy-face 1_" + page + "\" value=\"Disable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					else
					{
						block = block.replace("{tableColor}", "L2UI_CT1_CN.Gray");
						block = block.replace("{lock}", "<img src=\"L2UI_CH3.l2ui_ch3.joypad_lock\" width=\"16\" height=\"16\">");
						block = block.replace("{button}", "<button action=\"bypass -h .visualmy-hat " + visual.getId() + "_1_" + page + "\" value=\"Enable\" width=60 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					block = block.replace("{bypassTry}", "bypass -h .visual-tryhat " + visual.getId() + "_1_" + page);
					block = block.replace("{name}", visual_name);
					block = block.replace("{icon}", Util.getItemIcon(visual.getHatId()));
					countt++;
					if (countt == 4 || countt == 8 || countt == 12)
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
					list += block;
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			if (totalSize == 0)
			{
				list = "<td width=130 align=center>Empty List!</td>";
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, ".visual-myhatpage %s"));
			AbstractCommunity.separateAndSend(html, player);
		}
		else if (command.equals("visualbuy-hat"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int hat = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			final DressHatTemplate visual = DressHatParser.getInstance().getHat(hat);
			
			if (player.getInventory().getItemByItemId(visual.getPriceId()) == null)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			if (player.getInventory().getItemByItemId(visual.getPriceId()).getCount() < visual.getPriceCount())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				return false;
			}
			
			player.destroyItemByItemId("Visual", visual.getPriceId(), visual.getPriceCount(), player, true);
			player.sendMessage("You successfully bought visual skin " + visual.getName(null));
			
			buyVisuality(player, "Hair", hat);
			useVoicedCommand("visual-myhatpage", player, String.valueOf(page));
		}
		else if (command.equals("visualmy-hat"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int hatId = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final String page = subStr[2];
			
			final Item template = ItemsParser.getInstance().getTemplate(DressHatParser.getInstance().getHat(hatId).getHatId());
			final int paperdoll = Inventory.getPaperdollIndex(template.getBodyPart());
			
			final ItemInstance item = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
			if (item != null)
			{
				final Item itemTemp = ItemsParser.getInstance().getTemplate(item.getId());
				if (itemTemp.getBodyPart() == Item.SLOT_HAIRALL)
				{
					if (itemTemp.getBodyPart() != template.getBodyPart())
					{
						player.sendMessage("Error: Must be equiped correct item for skin.");
						if (type == 1)
						{
							useVoicedCommand("visual-myhatpage", player, page);
						}
						else
						{
							useVoicedCommand("visualme-hat", player, page);
						}
						return false;
					}
				}
				else if (itemTemp.getBodyPart() == Item.SLOT_HAIR)
				{
					if (template.getBodyPart() == Item.SLOT_HAIRALL)
					{
						player.sendMessage("Error: Must be equiped correct item for skin.");
						if (type == 1)
						{
							useVoicedCommand("visual-myhatpage", player, page);
						}
						else
						{
							useVoicedCommand("visualme-hat", player, page);
						}
						return false;
					}
				}
			}
			
			final ItemInstance hair = player.getInventory().getPaperdollItem(paperdoll);
			if (hair == null)
			{
				player.sendMessage("Error: Must be equiped slot for skin.");
				if (type == 1)
				{
					useVoicedCommand("visual-myhatpage", player, page);
				}
				else
				{
					useVoicedCommand("visualme-hat", player, page);
				}
				return false;
			}
			
			visuality(player, "Hair", hatId, true);
			if (type == 1)
			{
				useVoicedCommand("visual-myhatpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-hat", player, page);
			}
			player.broadcastCharInfo();
		}
		else if (command.equals("visual-tryhat"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int hat = Integer.parseInt(subStr[0]);
			final int type = Integer.parseInt(subStr[1]);
			final int page = Integer.parseInt(subStr[2]);
			
			final DressHatTemplate visual = DressHatParser.getInstance().getHat(hat);
			if (visual == null)
			{
				return false;
			}
			
			if (player.canUsePreviewTask())
			{
				player.sendPreviewUserInfo(0, 0, 0, 0, 0, 0, 0, visual.getSlot() == 2 ? visual.getHatId() : 0, visual.getSlot() == 2 ? 0 : visual.getHatId());
				player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
				player.setRemovePreviewTask();
			}
			
			if (type == 1)
			{
				useVoicedCommand("visual-myhatpage", player, String.valueOf(page));
			}
			else
			{
				useVoicedCommand("visualme-hat", player, String.valueOf(page));
			}
		}
		else if (command.equals("unvisualmy-hair"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int type = Integer.parseInt(subStr[0]);
			final String page = subStr[1];
			player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
			player.unVisualHairSkin(true);
			if (type == 1)
			{
				useVoicedCommand("visual-myhatpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-hat", player, page);
			}
			player.broadcastUserInfo(true);
			player.sendUserInfo();
		}
		else if (command.equals("unvisualmy-face"))
		{
			final String[] subStr = args.split(" ")[0].split("_");
			final int type = Integer.parseInt(subStr[0]);
			final String page = subStr[1];
			player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
			player.unVisualFaceSkin(true);
			if (type == 1)
			{
				useVoicedCommand("visual-myhatpage", player, page);
			}
			else
			{
				useVoicedCommand("visualme-hat", player, page);
			}
			player.broadcastUserInfo(true);
			player.sendUserInfo();
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
		int Sword = 1, Blunt = 1, Dagger = 1, Bow = 1, Pole = 1, Fist = 1, DualSword = 1, DualFist = 1, BigSword = 1, Rod = 1, BigBlunt = 1, Crossbow = 1, Rapier = 1, AncientSword = 1, DualDagger = 1;
		
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
	
	private void buyVisuality(Player player, String type, int visual)
	{
		if (visual > 0)
		{
			player.addVisual(type, visual);
		}
	}
	
	private void visuality(Player player, String type, int visual, boolean separate)
	{
		if (visual > 0)
		{
			player.broadcastPacket(new MagicSkillUse(player, player, 22217, 1, 0, 0));
			switch (type)
			{
				case "Weapon":
					player.setActiveWeaponSkin(visual, true);
					break;
				case "Armor":
					if (separate)
					{
						player.setActiveArmorSkin(visual, true);
					}
					else
					{
						final DressArmorTemplate set = DressArmorParser.getInstance().getArmor(visual);
						if (set != null)
						{
							player.setActiveArmorSkin(visual, true);
							if (set.getShieldId() > 0 && DressShieldParser.getInstance().getShieldId(set.getShieldId()) != -1)
							{
								player.setActiveShieldSkin(DressShieldParser.getInstance().getShieldId(set.getShieldId()), false);
							}
							
							if (set.getCloakId() > 0 && DressCloakParser.getInstance().getCloakId(set.getCloakId()) != -1)
							{
								player.setActiveCloakSkin(DressCloakParser.getInstance().getCloakId(set.getCloakId()), false);
							}
							
							if (set.getHatId() > 0 && DressHatParser.getInstance().getHatId(set.getHatId()) != -1)
							{
								if (set.getSlot() == 3)
								{
									player.setActiveMaskSkin(DressHatParser.getInstance().getHatId(set.getHatId()), false);
								}
								else
								{
									player.setActiveHairSkin(DressHatParser.getInstance().getHatId(set.getHatId()), false);
								}
							}
						}
					}
					break;
				case "Shield":
					player.setActiveShieldSkin(visual, separate);
					break;
				case "Cloak":
					player.setActiveCloakSkin(visual, separate);
					break;
				case "Hair" :
					final DressHatTemplate hair = DressHatParser.getInstance().getHat(visual);
					if (hair.getSlot() == 3)
					{
						player.setActiveMaskSkin(visual, true);
					}
					else
					{
						player.setActiveHairSkin(visual, true);
					}
					break;
			}
		}
		player.broadcastUserInfo(true);
		player.sendUserInfo();
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	
	public static void main(String[] args)
	{
		if(VoicedCommandHandler.getInstance().getHandler("visualme") == null) {
			VoicedCommandHandler.getInstance().registerHandler(new VisualMe());
			parseWeapon();
		}
	}
}