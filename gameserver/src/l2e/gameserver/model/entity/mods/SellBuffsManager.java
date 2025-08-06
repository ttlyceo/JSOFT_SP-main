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
package l2e.gameserver.model.entity.mods;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.HtmlUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.dao.CharacterSellBuffsDAO;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.SellBuffsParser;
import l2e.gameserver.data.parser.SellBuffsParser.SellBuffTemplate;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SellBuffHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.ExPrivateStorePackageMsg;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public final class SellBuffsManager
{
	protected SellBuffsManager()
	{
	}

	public static void sendSellMenu(Player player)
	{
		final String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/sellBuffs/" + (player.isSellingBuffs() ? "already.htm" : "buffmenu.htm"));
		final NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setHtml(player, html.toString());
		player.sendPacket(htm);
	}
	
	public static void sendBuffChoiceMenu(Player player, int page)
	{
		final List<Skill> skillList = new ArrayList<>();
		for (final Skill skill : player.getAllSkills())
		{
			if (skill != null)
			{
				final SellBuffTemplate tpl = SellBuffsParser.getInstance().getSellBuff(skill.getId());
				if (tpl != null && !isInSellList(player, skill))
				{
					skillList.add(skill);
				}
			}
		}
		
		if (skillList.isEmpty())
		{
			player.sendMessage((new ServerMessage("SellBuff.NO_BUFFS", player.getLang())).toString());
			return;
		}
		
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/sellBuffs/choice.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/sellBuffs/choice-template.htm");
		
		String block = "";
		String list = "";
		
		final int perpage = 5;
		int counter = 0;
		int curPage = page;
		final int totalSize = skillList.size();
		final boolean isThereNextPage = totalSize > perpage;
		
		String currency = "";
		for (final String itemName : Config.SELLBUFF_CURRECY_LIST.keySet())
		{
			if (itemName != null && !itemName.isEmpty())
			{
				currency += itemName + ";";
			}
		}
		
		if (isThereNextPage && ((perpage * page) - totalSize) >= perpage)
		{
			curPage = page - 1;
		}
		
		for (int i = (curPage - 1) * perpage; i < totalSize; i++)
		{
			final Skill skill = skillList.get(i);
			if (skill != null)
			{
				block = template;
				block = block.replace("%name%", skill.getName(player.getLang()));
				block = block.replace("%icon%", skill.getIcon());
				block = block.replace("%level%", skill.getLevel() > 100 ? "<font color=\"LEVEL\"> + " + (skill.getLevel() % 100) + "</font>" : "<font color=\"ae9977\">" + skill.getLevel() + "</font>");
				block = block.replace("%currency%", "<combobox width=80 var=\"currency_" + skill.getId() + "\" list=\"" + currency + "\">");
				block = block.replace("%editVar%", "<edit var=\"price_" + skill.getId() + "\" width=60 height=10 type=\"number\">");
				block = block.replace("%bypass%", "sellbuffaddskill " + skill.getId() + " $price_" + skill.getId() + " $currency_" + skill.getId() + " " + curPage + "");
				list += block;
			}
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		final int count = (int) Math.ceil((double) totalSize / perpage);
		html = html.replace("{list}", list);
		html = html.replace("{navigation}", Util.getNavigationBlock(count, curPage, totalSize, perpage, isThereNextPage, "sellbuffadd %s"));
		Util.setHtml(html, player);
	}

	public static void sendBuffEditMenu(Player player, int page)
	{
		if (player.getSellingBuffs() == null || player.getSellingBuffs().isEmpty())
		{
			player.sendMessage((new ServerMessage("SellBuff.NO_BUFFS", player.getLang())).toString());
			return;
		}
		
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/sellBuffs/choice.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/sellBuffs/choice-editTemplate.htm");
		
		String block = "";
		String list = "";
		
		final List<Skill> sellList = new ArrayList<>();
		for (final SellBuffHolder holder : player.getSellingBuffs())
		{
			final Skill skill = player.getKnownSkill(holder.getId());
			if (skill != null)
			{
				sellList.add(skill);
			}
		}
		
		if (sellList == null || sellList.isEmpty())
		{
			player.sendMessage((new ServerMessage("SellBuff.NO_BUFFS", player.getLang())).toString());
			return;
		}
		
		final int perpage = 5;
		int counter = 0;
		int curPage = page;
		final int totalSize = sellList.size();
		final boolean isThereNextPage = totalSize > perpage;
		
		String currency = "";
		for (final String itemName : Config.SELLBUFF_CURRECY_LIST.keySet())
		{
			if (itemName != null && !itemName.isEmpty())
			{
				currency += itemName + ";";
			}
		}
		
		if (isThereNextPage && ((perpage * page) - totalSize) >= perpage)
		{
			curPage = page - 1;
		}
		
		for (int i = (curPage - 1) * perpage; i < totalSize; i++)
		{
			final Skill skill = sellList.get(i);
			if (skill != null)
			{
				block = template;
				block = block.replace("%name%", skill.getName(player.getLang()));
				block = block.replace("%icon%", skill.getIcon());
				block = block.replace("%level%", skill.getLevel() > 100 ? "<font color=\"LEVEL\"> + " + (skill.getLevel() % 100) + "</font>" : "<font color=\"ae9977\">" + skill.getLevel() + "</font>");
				block = block.replace("%itemId%", buffItemId(player, player, skill.getId()));
				block = block.replace("%price%", Util.formatAdena(buffPrice(player, skill.getId())));
				block = block.replace("%editVar%", "<edit var=\"price_" + skill.getId() + "\" width=60 height=10 type=\"number\">");
				block = block.replace("%currency%", "<combobox width=80 var=\"currency_" + skill.getId() + "\" list=\"" + currency + "\">");
				block = block.replace("%bypass%", "sellbuffchangeprice " + skill.getId() + " $price_" + skill.getId() + " $currency_" + skill.getId() + " " + curPage + "");
				block = block.replace("%delBypass%", "sellbuffremove " + skill.getId() + " " + curPage + "");
				list += block;
			}
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		final int count = (int) Math.ceil((double) totalSize / perpage);
		html = html.replace("{list}", list);
		html = html.replace("{navigation}", Util.getNavigationBlock(count, curPage, totalSize, perpage, isThereNextPage, "sellbuffedit %s"));
		Util.setHtml(html, player);
	}

	public static void sendBuffMenu(Player player, Player seller, int page)
	{
		if (!seller.isSellingBuffs() || seller.getSellingBuffs().isEmpty())
		{
			return;
		}

		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/sellBuffs/buymenu.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/sellBuffs/template.htm");
		
		String block = "";
		String list = "";
		
		final List<Skill> sellList = new ArrayList<>();
		for (final SellBuffHolder holder : seller.getSellingBuffs())
		{
			final Skill skill = seller.getKnownSkill(holder.getId());
			if (skill != null)
			{
				sellList.add(skill);
			}
		}
		
		final int perpage = 6;
		int counter = 0;
		final int totalSize = sellList.size();
		final boolean isThereNextPage = totalSize > perpage;
		
		for (int i = (page - 1) * perpage; i < totalSize; i++)
		{
			final Skill skill = sellList.get(i);
			if (skill != null)
			{
				block = template;
				block = block.replace("%name%", skill.getName(player.getLang()));
				block = block.replace("%icon%", skill.getIcon());
				block = block.replace("%level%", skill.getLevel() > 100 ? "<font color=\"LEVEL\"> + " + (skill.getLevel() % 100) + "</font>" : "<font color=\"ae9977\">" + skill.getLevel() + "</font>");
				block = block.replace("%item%", buffItemId(player, seller, skill.getId()));
				block = block.replace("%amount%", Util.formatAdena(buffPrice(seller, skill.getId())));
				block = block.replace("%mpAmount%", Config.SELLBUFF_USED_MP ? "(<font color=\"1E90FF\">" + (skill.getMpConsume()) + " MP</font>)" : "");
				block = block.replace("%bypass%", "sellbuffbuyskill " + seller.getObjectId() + " " + skill.getId() + " " + page + "");
				list += block;
			}
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		final int count = (int) Math.ceil((double) totalSize / perpage);
		if (Config.SELLBUFF_USED_MP)
		{
			html = html.replace("%mp%", "<br>" + HtmlUtil.getMpGauge(250, (long) seller.getCurrentMp(), (int) seller.getMaxMp(), false, true) + " <br>");
		}
		else
		{
			html = html.replace("%mp%", "");
		}
		html = html.replace("{list}", list);
		html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "sellbuffbuymenu " + seller.getObjectId() + " %s"));
		Util.setHtml(html, player);
	}
	
	public static int buffPrice(Player seller, int skillId)
	{
		for (final SellBuffHolder holder : seller.getSellingBuffs())
		{
			if (holder.getId() == skillId)
			{
				return (int) holder.getPrice();
			}
		}
		return 0;
	}
	
	public static String buffItemId(Player player, Player seller, int skillId)
	{
		for (final SellBuffHolder holder : seller.getSellingBuffs())
		{
			if (holder.getId() == skillId)
			{

				return Util.getItemName(player, holder.getItemId());
			}
		}
		return "";
	}

	public static void startSellBuffs(Player player, String title)
	{
		player.sitDown();
		player.setIsSellingBuffs(true);
		CharacterSellBuffsDAO.getInstance().saveSellBuffList(player);
		player.setPrivateStoreType(Player.STORE_PRIVATE_PACKAGE_SELL);
		player.setIsInStoreNow(true);
		player.getSellList().setTitle(title);
		player.setVar("sellstorename", player.getSellList().getTitle(), -1);
		player.getSellList().setPackaged(true);
		player.broadcastCharInfo();
		player.broadcastPacket(new ExPrivateStorePackageMsg(player));
		sendSellMenu(player);
	}

	public static void stopSellBuffs(Player player)
	{
		player.setIsSellingBuffs(false);
		player.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
		CharacterSellBuffsDAO.getInstance().cleanSellBuffList(player);
		player.standUp();
		player.broadcastCharInfo();
		sendSellMenu(player);
	}

	public static boolean isInSellList(Player player, Skill skill)
	{
		for (final SellBuffHolder holder : player.getSellingBuffs())
		{
			if (holder != null && holder.getId() == skill.getId())
			{
				return true;
			}
		}
		return false;
	}
}