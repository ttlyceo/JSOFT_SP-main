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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.apache.StringUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.Elementals.Elemental;
import l2e.gameserver.model.TradeItem;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.entity.auction.AccessoryItemType;
import l2e.gameserver.model.entity.auction.ArmorItemType;
import l2e.gameserver.model.entity.auction.Auction;
import l2e.gameserver.model.entity.auction.AuctionItemTypes;
import l2e.gameserver.model.entity.auction.AuctionsManager;
import l2e.gameserver.model.entity.auction.EtcAuctionItemType;
import l2e.gameserver.model.entity.auction.PetItemType;
import l2e.gameserver.model.entity.auction.SuppliesItemType;
import l2e.gameserver.model.entity.auction.WeaponItemType;
import l2e.gameserver.model.items.ItemAuction;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.skills.funcs.FuncTemplate;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;

public class CommunityAuction extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityAuction()
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
		        "_maillist_0_1_0_", "_bbsAuction", "_bbsNewAuction"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		if (!checkCondition(player, new StringTokenizer(command, "_").nextToken(), false, false))
		{
			return;
		}
		
		String html = "";
		
		if (command.equals("_maillist_0_1_0_"))
		{
			if (Config.ENABLE_MULTI_AUCTION_SYSTEM)
			{
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/auction/multi_auction_list.htm");
			}
			else
			{
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/auction/auction_list.htm");
				html = fillAuctionListPage(player, html, 1, 57, new int[]
				{
				        -1, -1
				}, "All", null, 1, 0, 0, 0);
			}
		}
		else if (command.startsWith("_bbsAuction"))
		{
			final StringTokenizer st = new StringTokenizer(command, "_");
			st.nextToken();

			try
			{
				final int page = Integer.parseInt(st.nextToken().trim());
				final int priceItemId = Integer.parseInt(st.nextToken().trim());
				
				final int[] itemTypes = new int[2];
				int i = 0;
				for (final String type : st.nextToken().trim().split(" "))
				{
					itemTypes[i] = Integer.parseInt(type);
					i++;
				}
				final String grade = st.nextToken().trim();
				final String search = st.nextToken().trim();
				final int itemSort = Integer.parseInt(st.nextToken().trim());
				final int gradeSort = Integer.parseInt(st.nextToken().trim());
				final int quantitySort = Integer.parseInt(st.nextToken().trim());
				final int priceSort = Integer.parseInt(st.nextToken().trim());
				
				if (st.hasMoreTokens())
				{
					final int action = Integer.parseInt(st.nextToken().trim());
					final int auctionId = Integer.parseInt(st.nextToken().trim());
					
					if (action == 1)
					{
						if (!st.hasMoreTokens())
						{
							player.sendMessage((new ServerMessage("CommunityAuction.FILL_ALL", player.getLang())).toString());
						}
						else
						{
							final String quantity = st.nextToken().trim();
							final Auction auction = AuctionsManager.getInstance().getAuction(auctionId);
							if (auction == null || auction.getItem() == null)
							{
								player.sendMessage((new ServerMessage("CommunityAuction.ALREADY_SOLD", player.getLang())).toString());
							}
							else
							{
								long realPrice;
								try
								{
									realPrice = auction.getPricePerItem() * Long.parseLong(quantity);
								}
								catch (final NumberFormatException e)
								{
									player.sendMessage((new ServerMessage("CommunityAuction.INVALID", player.getLang())).toString());
									return;
								}
								final ItemInstance item = auction.getItem();
								final ServerMessage msg = new ServerMessage("CommunityAuction.WANT_TO_BUY", player.getLang());
								msg.add(quantity);
								msg.add(item.getItem().getName(player.getLang()));
								msg.add(Util.getNumberWithCommas(realPrice));
								msg.add(Util.getItemName(player, priceItemId));
								player.sendConfirmDlg(new ButtonClick(player, priceItemId, item, Buttons.Buy_Item, quantity), 60000, msg.toString());
							}
						}
					}
					html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/auction/buy_item.htm");
					html = fillPurchasePage(player, html, page, priceItemId, itemTypes, grade, search, itemSort, gradeSort, quantitySort, priceSort, auctionId);
				}
				else
				{
					html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/auction/auction_list.htm");
					html = fillAuctionListPage(player, html, page, priceItemId, itemTypes, grade, search, itemSort, gradeSort, quantitySort, priceSort);
				}
			}
			catch (final NumberFormatException e)
			{}
		}
		else if (command.startsWith("_bbsNewAuction"))
		{
			final StringTokenizer st = new StringTokenizer(command, "_");
			st.nextToken();

			if (player.isInStoreMode())
			{
				player.sendMessage((new ServerMessage("CommunityAuction.CANT_OPEN", player.getLang())).toString());
				return;
			}
			final String priceItemId = (st.hasMoreTokens() ? st.nextToken().trim() : "57");
			String currentItem = (st.hasMoreTokens() ? st.nextToken().trim() : "c0");
			final int currentObjectId = Integer.parseInt(currentItem.substring(1));
			currentItem = currentItem.substring(0, 1);
			final int line = Integer.parseInt((st.hasMoreTokens() ? st.nextToken().trim() : "0"));
			final String buttonClicked = st.hasMoreTokens() ? st.nextToken().trim() : null;
			if (buttonClicked != null)
			{
				if (buttonClicked.equals("0"))
				{
					final ItemInstance item = player.getInventory().getItemByObjectId(currentObjectId);
					boolean error = false;
					
					final String[] vars = new String[2];
					
					for (int i = 0; i < 2; i++)
					{
						if (st.hasMoreTokens())
						{
							vars[i] = st.nextToken().trim();
							if (vars[i].isEmpty())
							{
								error = true;
							}
						}
						else
						{
							error = true;
						}
					}
					
					if (error)
					{
						player.sendMessage((new ServerMessage("CommunityAuction.FILL_FIELDS", player.getLang())).toString());
					}
					else if (item == null)
					{
						player.sendMessage((new ServerMessage("CommunityAuction.DONT_EXIST", player.getLang())).toString());
					}
					else
					{
						final ServerMessage msg = new ServerMessage("CommunityAuction.WANT_TO_SELL", player.getLang());
						msg.add(item.getItem().getName(player.getLang()));
						player.sendConfirmDlg(new ButtonClick(player, Integer.parseInt(priceItemId), item, Buttons.New_Auction, vars[0], vars[1]), 60000, msg.toString());
					}
				}
				else if (buttonClicked.equals("1"))
				{
					final ItemInstance item = ItemAuction.getInstance().getItemByObjectId(currentObjectId);
					if (item == null)
					{
						player.sendMessage((new ServerMessage("CommunityAuction.ALREADY_SOLD", player.getLang())).toString());
						final Collection<Auction> auctions = AuctionsManager.getInstance().getMyAuctions(player.getObjectId(), Integer.parseInt(priceItemId));
						for (final Auction a : auctions)
						{
							if (a.getItem() == null)
							{
								_log.warn("Auction bugged! Item:null itemId:" + currentObjectId + " auctionId:" + a.getAuctionId() + " Count:" + a.getCountToSell() + " Price:" + a.getPricePerItem() + " Seller:" + a.getSellerName() + "[" + a.getSellerObjectId() + "] store:" + a.isPrivateStore());
							}
							else
							{
								_log.warn("Auction bugged! Item:" + a.getItem().getName(null) + " itemId:" + currentObjectId + " playerInv:" + player.getInventory().getItemByObjectId(player.getObjectId()) + " auctionId:" + a.getAuctionId() + " Count:" + a.getCountToSell() + " Price:" + a.getPricePerItem() + " Seller:" + a.getSellerName() + "[" + a.getSellerObjectId() + "] store:" + a.isPrivateStore());
							}
							AuctionsManager.getInstance().removeStore(player, a.getAuctionId());
						}
					}
					else
					{
						if (!player.hasDialogAskActive())
						{
							final ServerMessage msg = new ServerMessage("CommunityAuction.WANT_TO_CANCEL", player.getLang());
							msg.add(item.getItem().getName(player.getLang()));
							player.sendConfirmDlg(new ButtonClick(player, Integer.parseInt(priceItemId), item, Buttons.Cancel_Auction), 60000, msg.toString());
						}
					}
				}
			}
			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/auction/new_auction.htm");
			html = fillNewAuctionPage(player, html, Integer.parseInt(priceItemId), currentItem.equals("n"), currentObjectId, line);
		}
		separateAndSend(html, player);
	}
	
	private String fillAuctionListPage(Player player, String html, int page, int priceItemId, int[] itemTypes, String itemGrade, String search, int itemSort, int gradeSort, int quantitySort, int priceSort)
	{
		int heightToBeUsed = 220;
		for (int i = 1; i <= 6; i++)
		{
			if (itemTypes[0] == i)
			{
				final AuctionItemTypes[] types = getGroupsInType(itemTypes[0]);
				html = html.replace("%plusMinusBtn" + i + "%", "<button value=\"\" action=\"bypass -h _bbsAuction_ 1 _ %priceItemId% _ -1 -1 _ %grade% _ %search% _ %itemSort% _ %gradeSort% _ %quantitySort% _ %priceSort%\" width=15 height=15 back=\"L2UI_CH3.QuestWndMinusBtn\" fore=\"L2UI_CH3.QuestWndMinusBtn\">");
				html = html.replace("%itemListHeight" + i + "%", String.valueOf(types.length * 5));
				heightToBeUsed -= types.length * 15;
				
				final StringBuilder builder = new StringBuilder();
				builder.append("<table>");
				int count = 0;
				for (final AuctionItemTypes itemType : types)
				{
					builder.append("<tr><td><table width=150 bgcolor=").append(count % 2 == 1 ? "22211d" : "1b1a15").append(">");
					builder.append("<tr><td width=150 height=17><font color=93886c>");
					builder.append("<button value=\"").append(ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction." + itemType.toString() + "")).append("\" action=\"bypass -h _bbsAuction_ 1 _ %priceItemId% _ ").append(itemTypes[0]).append(" ").append(count).append(" _ %grade% _ %search% _ %itemSort% _ %gradeSort% _ %quantitySort% _ %priceSort%\" width=150 height=17 back=\"L2UI_CT1.emptyBtn\" fore=\"L2UI_CT1.emptyBtn\">");
					builder.append("</font></td></tr></table></td></tr>");
					count++;
				}
				builder.append("</table>");
				html = html.replace("%itemList" + i + "%", builder.toString());
			}
			else
			{
				html = html.replace("%plusMinusBtn" + i + "%", "<button value=\"\" action=\"bypass -h _bbsAuction_ 1 _ %priceItemId% _ " + (i) + " -1 _ %grade% _ %search% _ %itemSort% _ %gradeSort% _ %quantitySort% _ %priceSort%\" width=15 height=15 back=\"L2UI_CH3.QuestWndPlusBtn\" fore=\"L2UI_CH3.QuestWndPlusBtn\">");
				html = html.replace("%itemListHeight" + i + "%", "0");
				html = html.replace("%itemList" + i + "%", "");
			}
		}
		html = html.replace("%lastItemHeight%", String.valueOf(heightToBeUsed - 40));
		
		final StringBuilder builder = new StringBuilder();
		final Collection<Auction> allAuctions = AuctionsManager.getInstance().getAllAuctionsPerItemId(priceItemId);
		List<Auction> auctions = getRightAuctions(allAuctions, itemTypes, itemGrade, search);
		auctions = sortAuctions(player, auctions, itemSort, gradeSort, quantitySort, priceSort);
		
		final int maxPage = (int) Math.ceil((double) auctions.size() / 10);
		for (int i = 10 * (page - 1); i < Math.min(auctions.size(), 10 * (page)); i++)
		{
			Auction auction;
			try
			{
				auction = auctions.get(i);
			}
			catch (final RuntimeException e)
			{
				break;
			}
			final ItemInstance item = auction.getItem();
			
			builder.append("<table border=0 cellspacing=1 cellpadding=0 width=558 height=30 bgcolor=").append(i % 2 == 1 ? "1a1914" : "23221d").append(">");
			builder.append("<tr><td fixwidth=280 height=25><table border=0 width=280 height=30><tr>");
			builder.append("<td width=32 background=" + item.getItem().getIcon() + "><button value=\"\" action=\"bypass -h _bbsAuction_ %page% _ %priceItemId% _ %type% _ %grade% _ %search% _ %itemSort% _ %gradeSort% _ %quantitySort% _ %priceSort% _ 0 _ ").append(auction.getAuctionId()).append("\" width=32 height=32 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"></td>");
			builder.append(getItemName(player, item, 248, 25, auction.isPrivateStore()));
			builder.append("</tr></table></td><td width=50 height=30><center>");
			if (item.getItem().getCrystalType() != Item.CRYSTAL_NONE)
			{
				builder.append("<img src=").append(getGradeIcon(item.getItem().getItemsGrade(item.getItem().getCrystalType()))).append(" width=15 height=15>");
			}
			else
			{
				builder.append("None");
			}
			builder.append("</center></td><td width=75 height=30>");
			builder.append("<center>").append(auction.getCountToSell()).append("</center>");
			builder.append("</td><td width=150 height=30 valign=top align=right>");
			builder.append(Util.getNumberWithCommas(auction.getPricePerItem()) + "<br1>");
			builder.append("<font color=A18C70 name=CREDITTEXTSMALL>(" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.TOTAL") + ": " + Util.getNumberWithCommas(auction.getCountToSell() * auction.getPricePerItem()) + ")</font>");
			builder.append("</td></tr></table>");
		}
		html = html.replace("%auctionItems%", builder.toString());
		html = html.replace("%type%", itemTypes[0] + " " + itemTypes[1]);
		html = html.replace("%grade%", itemGrade);
		html = html.replace("%search%", search == null ? "" : search);
		html = html.replace("%totalItems%", String.valueOf(auctions.size()));
		html = html.replace("%itemSort%", String.valueOf(itemSort));
		html = html.replace("%gradeSort%", String.valueOf(gradeSort));
		html = html.replace("%quantitySort%", String.valueOf(quantitySort));
		html = html.replace("%priceSort%", String.valueOf(priceSort));
		html = html.replace("%changeItemSort%", "" + (itemSort <= 0 ? 1 : -1));
		html = html.replace("%changeGradeSort%", "" + (gradeSort <= 0 ? 1 : -1));
		html = html.replace("%changeQuantitySort%", "" + (quantitySort <= 0 ? 1 : -1));
		html = html.replace("%changePriceSort%", "" + (priceSort <= 0 ? 1 : -1));
		html = html.replace("%page%", "" + page);
		html = html.replace("%priceItemId%", String.valueOf(priceItemId));
		html = html.replace("%prevPage%", String.valueOf(Math.max(1, page - 1)));
		html = html.replace("%nextPage%", String.valueOf(Math.min(maxPage, page + 1)));
		html = html.replace("%lastPage%", String.valueOf(maxPage));
		html = html.replace("%priceItemName%", Util.getItemName(player, priceItemId));
		html = html.replace("%priceItemCount%", Util.getNumberWithCommas(player.getInventory().getItemByItemId(priceItemId) == null ? 0 : player.getInventory().getItemByItemId(priceItemId).getCount()));
		return html;
	}
	
	private String fillPurchasePage(Player player, String html, int page, int priceItemId, int[] itemTypes, String itemGrade, String search, int itemSort, int gradeSort, int quantitySort, int priceSort, int auctionId)
	{
		
		final Auction auction = AuctionsManager.getInstance().getAuction(auctionId);
		if (auction == null || auction.getItem() == null)
		{
			return "";
		}
		final ItemInstance choosenItem = auction.getItem();

		final StringBuilder builder = new StringBuilder();
		if (choosenItem.getEnchantLevel() > 0)
		{
			builder.append("<center><font color=b3a683>+").append(choosenItem.getEnchantLevel()).append(" </font>");
		}
		builder.append("<center>" + choosenItem.getItem().getName(player.getLang()));
		builder.append("<br><center><img src=").append(getGradeIcon(choosenItem.getItem().getItemsGrade(choosenItem.getItem().getCrystalType()))).append(" width=15 height=15>");
		builder.append("<br><br><br><font color=827d78><br><br>" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.SELLER") + ":</font> <font color=94775b>" + auction.getSellerName() + "</font>");
		if (choosenItem.isEquipable())
		{
			final int pAtk = getFunc(choosenItem, Stats.POWER_ATTACK);
			if (pAtk > 0)
			{
				builder.append("<br><br><font color=827d78>" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.P_ATK") + ":</font> <font color=94775b>").append(pAtk).append(" </font>");
			}
			final int mAtk = getFunc(choosenItem, Stats.MAGIC_ATTACK);
			if (mAtk > 0)
			{
				builder.append("<br><font color=827d78>" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.M_ATK") + ":</font> <font color=94775b>").append(mAtk).append(" </font>");
			}
			final int pDef = getFunc(choosenItem, Stats.POWER_DEFENCE);
			if (pDef > 0)
			{
				builder.append("<br><font color=827d78>" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.P_DEF") + ":</font> <font color=94775b>").append(pDef).append(" </font>");
			}
			final int mDef = getFunc(choosenItem, Stats.MAGIC_DEFENCE);
			if (mDef > 0)
			{
				builder.append("<br><font color=827d78>" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.M_DEF") + ":</font> <font color=94775b>").append(mDef).append(" </font>");
			}
			
			if (choosenItem.isWeapon() && choosenItem.getElementals() != null)
			{
				builder.append("<br><br><br><font color=827d78>").append(getElementName(choosenItem.getAttackElementType())).append(" " + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.ATK") + " ").append(choosenItem.getAttackElementPower());
				builder.append("</font><br><img src=L2UI_CT1.Gauge_DF_Attribute_").append(getElementName(choosenItem.getAttackElementType())).append(" width=100 height=10>");
			}
			if (choosenItem.isArmor() && choosenItem.getElementals() != null)
			{
				for (final Elementals elm : choosenItem.getElementals())
				{
					if (elm.getValue() > 0)
					{
						builder.append("<br><font color=827d78>").append(getElementName(Elementals.getReverseElement(elm.getElement()))).append(" (").append(getElementName(elm.getElement())).append(" " + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.DEF") + " ").append(elm.getValue()).append(") </font><img src=L2UI_CT1.Gauge_DF_Attribute_").append(getElementName(elm.getElement())).append(" width=100 height=10>");
					}
				}
			}
		}
		builder.append("</center>");
		
		html = html.replace("%page%", String.valueOf(page));
		html = html.replace("%priceItemId%", String.valueOf(priceItemId));
		html = html.replace("%type%", itemTypes[0] + " " + itemTypes[1]);
		html = html.replace("%grade%", itemGrade);
		html = html.replace("%search%", search == null ? "" : search);
		html = html.replace("%itemSort%", String.valueOf(itemSort));
		html = html.replace("%gradeSort%", String.valueOf(gradeSort));
		html = html.replace("%quantitySort%", String.valueOf(quantitySort));
		html = html.replace("%priceSort%", String.valueOf(priceSort));
		html = html.replace("%auctionId%", String.valueOf(auctionId));
		html = html.replace("%icon%", "<img src=" + choosenItem.getItem().getIcon() + " width=32 height=32>");
		html = html.replace("%fullName%", "<table width=240 height=50><tr>" + getItemName(player, choosenItem, 240, 50, auction.isPrivateStore(), (auction.getCountToSell() > 1 ? " x" + auction.getCountToSell() : "")) + "</tr></table>");
		html = html.replace("%quantity%", (auction.getCountToSell() > 1 ? "<edit var=\"quantity\" type=number value=\"\" width=140 height=12>" : "<center><font color=94775b>1</font></center>"));
		if (auction.getCountToSell() <= 1)
		{
			html = html.replace("$quantity", "1");
		}
		html = html.replace("%pricePerItem%", "<font color=94775b>" + String.valueOf(Util.getNumberWithCommas(auction.getPricePerItem())) + "</font>");
		html = html.replace("%totalPrice%", "<font color=94775b>" + String.valueOf(Util.getNumberWithCommas(auction.getCountToSell() * auction.getPricePerItem())) + "</font>");
		html = html.replace("%priceItemName%", Util.getItemName(player, priceItemId));
		html = html.replace("%priceItemCount%", Util.getNumberWithCommas(player.getInventory().getItemByItemId(priceItemId) == null ? 0 : player.getInventory().getItemByItemId(priceItemId).getCount()));
		html = html.replace("%fullAuctionDescription%", builder.toString());
		
		return html;
	}

	private String fillNewAuctionPage(Player player, String html, int priceItemId, boolean newItem, int currentItem, int line)
	{
		final List<ItemInstance> itemsToAuction = getItemsToAuction(player, priceItemId);
		final int maxLine = (int) Math.ceil((double) itemsToAuction.size() / 6);
		StringBuilder builder = new StringBuilder();
		int index = 0;
		boolean added = false;
		for (int i = 6 * (line); i < 6 * (line + 3); i++)
		{
			final ItemInstance item = i >= 0 && itemsToAuction.size() > i ? itemsToAuction.get(i) : null;
			if (index % 6 == 0)
			{
				builder.append("<tr>");
				if (added)
				{
					added = false;
				}
			}
			
			builder.append("<td width=32 align=center valign=top background=\"L2UI_CT1.ItemWindow_DF_SlotBox\">");
			if (item != null)
			{
				builder.append("<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=" + item.getItem().getIcon() + ">");
			}
			else
			{
				builder.append("<table border=0 cellspacing=0 cellpadding=0 width=32 height=32>");
			}
			builder.append("<tr>");
			builder.append("<td width=32 height=32 align=center valign=top>");
			if (item != null)
			{
				builder.append("<button value=\"\" action=\"bypass -h _bbsNewAuction_ " + priceItemId + " _ n").append(item.getObjectId()).append(" _ ").append(line).append("\" width=32 height=32 back=L2UI_CT1.ItemWindow_DF_Frame_Down fore=L2UI_CT1.ItemWindow_DF_Frame />");
			}
			else
			{
				builder.append("<br>");
			}
			builder.append("</td>");
			builder.append("</tr>");
			builder.append("</table>");
			builder.append("</td>");
			if (index % 6 == 5)
			{
				builder.append("</tr>");
				if (!added)
				{
					added = true;
				}
			}
			index++;
		}
		
		if (!added)
		{
			builder.append("</tr>");
		}
		
		html = html.replace("%auctionableItems%", builder.toString());
		
		builder = new StringBuilder();
		
		final Collection<Auction> myAuctions = AuctionsManager.getInstance().getMyAuctions(player, priceItemId);
		Auction[] auctions = myAuctions.toArray(new Auction[myAuctions.size()]);
		final boolean pakage = player.getPrivateStoreType() == Player.STORE_PRIVATE_PACKAGE_SELL;
		if (pakage || player.getPrivateStoreType() == Player.STORE_PRIVATE_SELL)
		{
			for (final TradeItem ti : player.getSellList().getItems())
			{
				for (final Auction auction : auctions)
				{
					if (auction.getItem() != null && auction.getItem().getObjectId() == ti.getObjectId())
					{
						myAuctions.remove(auction);
					}
				}
			}
		}
		
		auctions = myAuctions.toArray(new Auction[myAuctions.size()]);
		
		int i = 0;
		for (; i < 10; i++)
		{
			
			if (auctions.length <= i)
			{
				break;
			}
			final Auction auction = auctions[i];
			final ItemInstance item = auction.getItem();
			builder.append("<table border=0 cellspacing=0 cellpadding=0 width=470 bgcolor=").append(i % 2 == 1 ? "1a1914" : "23221d").append(">");
			builder.append("<tr><td fixwidth=240><table border=0 width=240><tr><td width=32 height=32 background=" + item.getItem().getIcon() + ">");
			if (!player.hasDialogAskActive())
			{
				builder.append("<button value=\"\" action=\"bypass -h _bbsNewAuction_ " + priceItemId + " _ c").append(item.getObjectId()).append(" _ ").append(line).append(" _ 1\" width=32 height=32 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
			}
			else
			{
				builder.append("<button width=32 height=32 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
			}
			builder.append("</td>");
			builder.append(getItemName(player, item, 228, 25, auction.isPrivateStore()));
			builder.append("</tr></table></td><td width=55><center>");
			if (item.getItem().getCrystalType() != Item.CRYSTAL_NONE)
			{
				builder.append("<img src=").append(getGradeIcon(item.getItem().getItemsGrade(item.getItem().getCrystalType()))).append(" width=15 height=15>");
			}
			else
			{
				builder.append("None");
			}
			builder.append("</center></td><td width=75>");
			builder.append("<center>").append(auction.getCountToSell()).append("</center>");
			builder.append("</td><td width=100><center>");
			builder.append(Util.getNumberWithCommas(auction.getPricePerItem()));
			builder.append("</center></td></tr></table>");
		}
		if (i < 10)
		{
			builder.append("<table border=0 cellspacing=0 cellpadding=0 width=470 height=").append((10 - i) * 35).append("><tr><td width=260><br></td><td width=55></td><td width=55></td><td width=100></td></tr></table>");
		}

		html = html.replace("%priceItemId%", String.valueOf(priceItemId));
		html = html.replace("%priceItemName%", Util.getItemName(player, priceItemId));
		html = html.replace("%priceItemCount%", Util.getNumberWithCommas(player.getInventory().getItemByItemId(priceItemId) == null ? 0 : player.getInventory().getItemByItemId(priceItemId).getCount()));
		html = html.replace("%auctionItems%", builder.toString());
		html = html.replace("%auctioned%", "" + auctions.length);
		html = html.replace("%totalPrice%", Util.getNumberWithCommas(0));
		html = html.replace("%saleFee%", Util.getNumberWithCommas(Config.AUCTION_FEE));
		html = html.replace("%currentItem%", (newItem ? "n" : "c") + currentItem);
		html = html.replace("%prevLine%", String.valueOf(Math.max(0, line - 1)));
		html = html.replace("%curLine%", String.valueOf(line));
		html = html.replace("%nextLine%", String.valueOf(Math.min(maxLine - 3, line + 1)));
		html = html.replace("%lastLine%", String.valueOf(Math.max(1, maxLine - 3)));
		
		final ItemInstance choosenItem = (currentItem > 0 ? player.getInventory().getItemByObjectId(currentItem) : null);
		
		html = html.replace("%choosenImage%", (choosenItem != null ? "<img src=" + choosenItem.getItem().getIcon() + " width=32 height=32>" : ""));
		html = html.replace("%choosenItem%", (choosenItem != null ? (getItemName(player, choosenItem, 180, 45, false, (choosenItem.getCount() > 1 ? " x" + choosenItem.getCount() : ""))) : ""));
		html = html.replace("%quantity%", (choosenItem == null || choosenItem.getCount() > 1 ? "<edit var=\"quantity\" type=number value=\"\" width=140 height=12>" : "<center>1</center>"));
		html = html.replace("%NewAuctionButton%", (choosenItem != null ? "<center><button value=\"" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.NEW_AUCTION") + "\" action=\"bypass -h _bbsNewAuction_ " + priceItemId + " _ " + (newItem ? "n" : "c") + currentItem + " _ " + line + " _ 0 _ " + (choosenItem == null || choosenItem.getCount() > 1 ? "$quantity" : "1") + " _ $sale_price\" width=120 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center>" : ""));
		
		return html;
	}

	private List<Auction> getRightAuctions(Collection<Auction> allAuctions, int[] splitedTypes, String itemGrade, String search)
	{
		final List<Auction> auctions = new ArrayList<>();
		for (final Auction auction : allAuctions)
		{
			if (splitedTypes != null && splitedTypes[0] >= 0)
			{
				boolean found = false;
				final AuctionItemTypes realItemType = auction.getItemType();
				AuctionItemTypes[] lookedTypes = getGroupsInType(splitedTypes[0]);
				
				if (splitedTypes[1] >= 0)
				{
					final AuctionItemTypes lookedType = lookedTypes[splitedTypes[1]];
					lookedTypes = new AuctionItemTypes[1];
					lookedTypes[0] = lookedType;
				}
				for (final AuctionItemTypes itemType : lookedTypes)
				{
					if (realItemType == itemType)
					{
						found = true;
						break;
					}
				}
				if (!found)
				{
					continue;
				}
			}
			if (!itemGrade.equals("All"))
			{
				if (!auction.getItem().getItem().getItemsGrade(auction.getItem().getItem().getCrystalType()).equalsIgnoreCase(itemGrade))
				{
					continue;
				}
			}
			if (search != null)
			{
				boolean found = false;
				for (final String lang : Config.MULTILANG_ALLOWED)
				{
					if (StringUtils.containsIgnoreCase(auction.getItem().getName(lang), search))
					{
						found = true;
					}
				}
				if (!found)
				{
					continue;
				}
			}
			auctions.add(auction);
		}
		return auctions;
	}

	protected String getElementName(int elementId)
	{
		String name = null;
		for (final Elemental att : Elemental.VALUES)
		{
			if (att.getId() == elementId)
			{
				name = att.name();
				if (name.equalsIgnoreCase("UNHOLY"))
				{
					name = "Dark";
				}
				if (name.equalsIgnoreCase("HOLY"))
				{
					name = "Divine";
				}
				name = name.substring(0, 1) + name.substring(1).toLowerCase();
			}
		}
		return name;
	}
	
	private List<ItemInstance> getItemsToAuction(Player player, int priceItemId)
	{
		final PcInventory inventory = player.getInventory();
		final List<ItemInstance> items = new ArrayList<>();
		if (player.isInStoreMode())
		{
			return items;
		}
		for (final ItemInstance item : inventory.getItems())
		{
			if (item.getItem().isAdena())
			{
				continue;
			}
			if (!item.isTradeable())
			{
				continue;
			}
			if (item.getItemLocation() == ItemLocation.AUCTION)
			{
				continue;
			}
			if (item.isQuestItem())
			{
				continue;
			}
			if (item.isAugmented())
			{
				continue;
			}
			if (item.isStackable())
			{
				for (final Auction playerAuction : AuctionsManager.getInstance().getMyAuctions(player, priceItemId))
				{
					if (playerAuction.getItem().getId() == item.getId())
					{
						continue;
					}
				}
			}
			if (item.isEquipped())
			{
				continue;
			}
			items.add(item);
		}
		return items;
	}

	private int getFunc(ItemInstance item, Stats stat)
	{
		for (final FuncTemplate func : item.getItem().getAttachedFuncs())
		{
			if (func.stat == stat)
			{
				return calc(item, (int) func.lambda.calc(null), stat);
			}
		}
		return 0;
	}
	
	protected int calc(ItemInstance item, int baseStat, Stats stat)
	{
		int value = baseStat;
		
		int enchant = item.getEnchantLevel();
		
		if (enchant <= 0)
		{
			return value;
		}
		
		int overenchant = 0;
		
		if (enchant > 3)
		{
			overenchant = enchant - 3;
			enchant = 3;
		}
		
		if ((stat == Stats.MAGIC_DEFENCE) || (stat == Stats.POWER_DEFENCE))
		{
			value = value + (enchant + (3 * overenchant));
			return value;
		}
		
		if (stat == Stats.MAGIC_ATTACK)
		{
			switch (item.getItem().getItemGradeSPlus())
			{
				case Item.CRYSTAL_S :
					value = value + ((4 * enchant) + (8 * overenchant));
					break;
				case Item.CRYSTAL_A :
				case Item.CRYSTAL_B :
				case Item.CRYSTAL_C :
					value = value + ((3 * enchant) + (6 * overenchant));
					break;
				case Item.CRYSTAL_D :
				case Item.CRYSTAL_NONE :
					value = value + ((2 * enchant) + (4 * overenchant));
					break;
			}
			return value;
		}
		
		if (item.isWeapon())
		{
			final WeaponType type = (WeaponType) item.getItemType();
			switch (item.getItem().getItemGradeSPlus())
			{
				case Item.CRYSTAL_S :
					switch (type)
					{
						case BOW :
						case CROSSBOW :
							value = value + ((10 * enchant) + (20 * overenchant));
							break;
						case BIGSWORD :
						case BIGBLUNT :
						case DUAL :
						case DUALFIST :
						case ANCIENTSWORD :
						case DUALDAGGER :
							value = value + ((6 * enchant) + (12 * overenchant));
							break;
						default :
							value = value + ((5 * enchant) + (10 * overenchant));
							break;
					}
					break;
				case Item.CRYSTAL_A :
					switch (type)
					{
						case BOW :
						case CROSSBOW :
							value = value + ((8 * enchant) + (16 * overenchant));
							break;
						case BIGSWORD :
						case BIGBLUNT :
						case DUAL :
						case DUALFIST :
						case ANCIENTSWORD :
						case DUALDAGGER :
							value = value + ((5 * enchant) + (10 * overenchant));
							break;
						default :
							value = value + ((4 * enchant) + (8 * overenchant));
							break;
					}
					break;
				case Item.CRYSTAL_B :
				case Item.CRYSTAL_C :
					switch (type)
					{
						case BOW :
						case CROSSBOW :
							value = value + ((6 * enchant) + (12 * overenchant));
							break;
						case BIGSWORD :
						case BIGBLUNT :
						case DUAL :
						case DUALFIST :
						case ANCIENTSWORD :
						case DUALDAGGER :
							value = value + ((4 * enchant) + (8 * overenchant));
							break;
						default :
							value = value + ((3 * enchant) + (6 * overenchant));
							break;
					}
					break;
				case Item.CRYSTAL_D :
				case Item.CRYSTAL_NONE :
					switch (type)
					{
						case BOW :
						case CROSSBOW :
						{
							value = value + ((4 * enchant) + (8 * overenchant));
							break;
						}
						default :
							value = value + ((2 * enchant) + (4 * overenchant));
							break;
					}
					break;
			}
		}
		return value;
	}
	
	private String getItemName(Player player, ItemInstance item, int windowWidth, int windowHeight, boolean isPrivStore, String... addToItemName)
	{
		final StringBuilder builder = new StringBuilder();
		
		if (item.getEnchantLevel() > 0)
		{
			builder.append("<font color=b3a683>+").append(item.getEnchantLevel()).append(" </font>");
		}
		
		final String[] parts = item.getItem().getName(player.getLang()).split(" - ");
		String itemName = item.getItem().getName(player.getLang());
		itemName = itemName.replace("<", "&lt;").replace(">", "&gt;");
		if (parts.length > 1 || ((item.isArmor() || item.getItem().isAccessory()) && item.getName("en").endsWith("of Chaos")))
		{
			builder.append("<font color=d4ce25>").append(itemName).append("</font>");
		}
		else
		{
			builder.append(itemName);
		}
		
		if (item.isWeapon() && item.getElementals() != null)
		{
			builder.append(" <font color=").append(getElementColor(item.getAttackElementType())).append(">").append(getElementName(item.getAttackElementType())).append("+").append(item.getAttackElementPower()).append("</font>");
		}
		if (item.isArmor() && item.getElementals() != null)
		{
			for (final Elementals elm : item.getElementals())
			{
				if (elm.getValue() > 0)
				{
					builder.append(" <font color=").append(getElementColor(Elementals.getReverseElement(elm.getElement()))).append(">").append(getElementName(Elementals.getReverseElement(elm.getElement()))).append("</font>");
				}
			}
		}
		if (isPrivStore)
		{
			builder.append(" <font color=DE9DE8>(" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAuction.PRIVATE_STORE") + ")</font>");
		}
		return "<td align=left width=228 height=25>" + builder.toString() + (addToItemName.length > 0 ? addToItemName[0] : "") + "</td>";
	}
	
	protected String getElementColor(int attId)
	{
		switch (attId)
		{
			case 0 :
				return "b36464";
			case 1 :
				return "528596";
			case 2 :
				return "768f91";
			case 3 :
				return "94775b";
			case 4 :
				return "8c8787";
			case 5 :
				return "4c558f";
			default :
				return "768f91";
		}
	}
	
	protected String getGradeIcon(String grade)
	{
		if (!grade.equalsIgnoreCase("NONE"))
		{
			return "L2UI_CT1.Icon_DF_ItemGrade_" + grade.replace("S8", "8");
		}
		else
		{
			return "";
		}
	}
	
	private static final AuctionItemTypes[][] ALL_AUCTION_ITEM_TYPES =
	{
	        AccessoryItemType.values(), ArmorItemType.values(), EtcAuctionItemType.values(), PetItemType.values(), SuppliesItemType.values(), WeaponItemType.values()
	};
	
	private AuctionItemTypes[] getGroupsInType(int type)
	{
		if (type > 0 && type < 7)
		{
			return ALL_AUCTION_ITEM_TYPES[type - 1];
		}
		return null;
	}
	
	private List<Auction> sortAuctions(Player player, List<Auction> auctionsToSort, int itemSort, int gradeSort, int quantitySort, int priceSort)
	{
		if (itemSort != 0)
		{
			Collections.sort(auctionsToSort, new ItemNameComparator(player, itemSort == 1 ? true : false));
		}
		else if (gradeSort != 0)
		{
			Collections.sort(auctionsToSort, new GradeComparator(gradeSort == 1 ? true : false));
		}
		else if (quantitySort != 0)
		{
			Collections.sort(auctionsToSort, new QuantityComparator(quantitySort == 1 ? true : false));
		}
		else if (priceSort != 0)
		{
			Collections.sort(auctionsToSort, new PriceComparator(priceSort == 1 ? true : false));
		}
		return auctionsToSort;
	}
	
	private static enum Buttons
	{
		New_Auction, Cancel_Auction, Buy_Item
	}
	
	private class ButtonClick implements OnAnswerListener
	{
		private final Player _player;
		private final int _priceItemId;
		private final ItemInstance _item;
		private final Buttons _button;
		private final String[] _args;
		
		private ButtonClick(Player player, int priceItemId, ItemInstance item, Buttons button, String... args)
		{
			_player = player;
			_priceItemId = priceItemId;
			_item = item;
			_button = button;
			_args = args;
		}
		
		@Override
		public void sayYes()
		{
			switch (_button)
			{
				case New_Auction :
					final String sQuantity = _args[0].replace(",", "").replace(".", "");
					final String sPricePerItem = _args[1].replace(",", "").replace(".", "");
					long quantity;
					long pricePerItem;
					try
					{
						quantity = Long.parseLong(sQuantity);
						pricePerItem = Long.parseLong(sPricePerItem);
					}
					catch (final NumberFormatException e)
					{
						onBypassCommand("_bbsNewAuction_ " + _priceItemId + " _ c0 _ 0", _player);
						return;
					}
					AuctionsManager.getInstance().checkAndAddNewAuction(_player, _item, quantity, _priceItemId, pricePerItem);
					onBypassCommand("_bbsNewAuction_ " + _priceItemId + " _ c0 _ 0", _player);
					break;
				case Cancel_Auction :
					AuctionsManager.getInstance().deleteAuction(_player, _item, _priceItemId);
					onBypassCommand("_bbsNewAuction_ " + _priceItemId + " _ c0 _ 0", _player);
					break;
				case Buy_Item :
					AuctionsManager.getInstance().buyItem(_player, _item, Long.parseLong(_args[0]));
					onBypassCommand("_bbsAuction_ 1 _ " + _priceItemId + " _ -1 _ All _  _ 1 _ 0 _ 0 _ 0", _player);
					break;
			}
		}
		
		@Override
		public void sayNo()
		{
			switch (_button)
			{
				case New_Auction :
				case Cancel_Auction :
					onBypassCommand("_bbsNewAuction_ " + _priceItemId + " _ c0 _ 0", _player);
					break;
				case Buy_Item :
					onBypassCommand("_bbsAuction_ 1 _ " + _priceItemId + " _ -1 _ All _  _ 1 _ 0 _ 0 _ 0", _player);
					break;
			}
		}
	}
	
	private static class ItemNameComparator implements Comparator<Auction>, Serializable
	{
		private static final long serialVersionUID = 7850753246573158288L;
		private final boolean _rightOrder;
		private final Player _player;
		
		private ItemNameComparator(Player player, boolean rightOrder)
		{
			_player = player;
			_rightOrder = rightOrder;
		}
		
		@Override
		public int compare(Auction o1, Auction o2)
		{
			if (_rightOrder)
			{
				return (o1.getItem().getName(_player.getLang()).compareTo(o2.getItem().getName(_player.getLang())));
			}
			else
			{
				return (o2.getItem().getName(_player.getLang()).compareTo(o1.getItem().getName(_player.getLang())));
			}
		}
	}
	
	private static class GradeComparator implements Comparator<Auction>, Serializable
	{
		private static final long serialVersionUID = 4096813325789557518L;
		private final boolean _rightOrder;
		
		private GradeComparator(boolean rightOrder)
		{
			_rightOrder = rightOrder;
		}
		@Override
		public int compare(Auction o1, Auction o2)
		{
			final int grade1 = o1.getItem().getItem().getCrystalType();
			final int grade2 = o2.getItem().getItem().getCrystalType();
			
			if (_rightOrder)
			{
				return Integer.compare(grade1, grade2);
			}
			else
			{
				return Integer.compare(grade2, grade1);
			}
		}
	}
	
	private static class QuantityComparator implements Comparator<Auction>, Serializable
	{
		private static final long serialVersionUID = 1572294088027593791L;
		private final boolean _rightOrder;
		
		private QuantityComparator(boolean rightOrder)
		{
			_rightOrder = rightOrder;
		}
		@Override
		public int compare(Auction o1, Auction o2)
		{
			if (_rightOrder)
			{
				return Long.compare(o1.getCountToSell(), o2.getCountToSell());
			}
			else
			{
				return Long.compare(o2.getCountToSell(), o1.getCountToSell());
			}
		}
	}
	
	private static class PriceComparator implements Comparator<Auction>, Serializable
	{
		private static final long serialVersionUID = 7065225580068613464L;
		private final boolean _rightOrder;
		
		private PriceComparator(boolean rightOrder)
		{
			_rightOrder = rightOrder;
		}
		
		@Override
		public int compare(Auction o1, Auction o2)
		{
			if (_rightOrder)
			{
				return Long.compare(o1.getPricePerItem(), o2.getPricePerItem());
			}
			else
			{
				return Long.compare(o2.getPricePerItem(), o1.getPricePerItem());
			}
		}
	}
	
	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}

	public static CommunityAuction getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final CommunityAuction _instance = new CommunityAuction();
	}
}