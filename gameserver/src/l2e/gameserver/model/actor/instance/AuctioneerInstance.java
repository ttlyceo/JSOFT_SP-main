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
package l2e.gameserver.model.actor.instance;

import static l2e.gameserver.model.items.itemcontainer.PcInventory.MAX_ADENA;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.instancemanager.AuctionManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.Auction;
import l2e.gameserver.model.entity.Auction.Bidder;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public final class AuctioneerInstance extends NpcInstance
{
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_REGULAR = 3;
	
	private final Map<Integer, Auction> _pendingAuctions = new ConcurrentHashMap<>();
	
	public AuctioneerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.AuctioneerInstance);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
		{
			player.sendMessage("Wrong conditions.");
			return;
		}
		else if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			final String filename = "data/html/auction/auction-busy.htm";
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, player.getLang(), filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			return;
		}
		else if (condition == COND_REGULAR)
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			final String actualCommand = st.nextToken();
			
			String val = "";
			if (st.countTokens() >= 1)
			{
				val = st.nextToken();
			}
			
			if (actualCommand.equalsIgnoreCase("auction"))
			{
				if (val.isEmpty())
				{
					return;
				}
				
				try
				{
					final int days = Integer.parseInt(val);
					try
					{
						final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
						long bid = 0;
						if (st.countTokens() >= 1)
						{
							bid = Math.min(Long.parseLong(st.nextToken()), MAX_ADENA);
						}
						
						final Auction a = new Auction(player.getClan().getHideoutId(), player.getClan(), days * 86400000L, bid);
						if (_pendingAuctions.get(a.getId()) != null)
						{
							_pendingAuctions.remove(a.getId());
						}
						
						_pendingAuctions.put(a.getId(), a);
						
						final String filename = "data/html/auction/AgitSale3.htm";
						final NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(player, player.getLang(), filename);
						html.replace("%x%", val);
						html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_MIN%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_DESC%", Util.clanHallDescription(player, ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getId()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale2");
						html.replace("%objectId%", String.valueOf((getObjectId())));
						player.sendPacket(html);
					}
					catch (final Exception e)
					{
						player.sendMessage("Invalid bid!");
					}
				}
				catch (final Exception e)
				{
					player.sendMessage("Invalid auction duration!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("confirmAuction"))
			{
				try
				{
					final Auction a = _pendingAuctions.get(player.getClan().getHideoutId());
					a.confirmAuction();
					_pendingAuctions.remove(player.getClan().getHideoutId());
				}
				catch (final Exception e)
				{
					player.sendMessage("Invalid auction");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("bidding"))
			{
				if (val.isEmpty())
				{
					return;
				}
				
				if (Config.DEBUG)
				{
					_log.warn("bidding show successful");
				}
				
				try
				{
					final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					final int auctionId = Integer.parseInt(val);
					
					if (Config.DEBUG)
					{
						_log.warn("auction test started");
					}
					
					final String filename = "data/html/auction/AgitAuctionInfo.htm";
					final Auction a = AuctionManager.getInstance().getAuction(auctionId);
					
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), filename);
					if (a != null)
					{
						html.replace("%AGIT_NAME%", Util.clanHallName(player, a.getId()));
						html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getLease()));
						html.replace("%AGIT_LOCATION%", Util.clanHallLocation(player, ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getId()));
						html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
						html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + String.valueOf((((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60)) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_COUNT%", String.valueOf(a.getBidders().size()));
						html.replace("%AGIT_AUCTION_DESC%", Util.clanHallDescription(player, ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getId()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_list");
						html.replace("%AGIT_LINK_BIDLIST%", "bypass -h npc_" + getObjectId() + "_bidlist " + a.getId());
						html.replace("%AGIT_LINK_RE%", "bypass -h npc_" + getObjectId() + "_bid1 " + a.getId());
					}
					else
					{
						_log.warn("Auctioneer Auction null for AuctionId : " + auctionId);
					}
					
					player.sendPacket(html);
				}
				catch (final Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("bid"))
			{
				if (val.isEmpty())
				{
					return;
				}
				
				try
				{
					final int auctionId = Integer.parseInt(val);
					try
					{
						long bid = 0;
						if (st.countTokens() >= 1)
						{
							bid = Math.min(Long.parseLong(st.nextToken()), MAX_ADENA);
						}
						
						AuctionManager.getInstance().getAuction(auctionId).setBid(player, bid);
					}
					catch (final Exception e)
					{
						player.sendMessage("Invalid bid!");
					}
				}
				catch (final Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("bid1"))
			{
				if ((player.getClan() == null) || (player.getClan().getLevel() < 2))
				{
					player.sendPacket(SystemMessageId.AUCTION_ONLY_CLAN_LEVEL_2_HIGHER);
					return;
				}
				
				if (val.isEmpty())
				{
					return;
				}
				
				if (((player.getClan().getAuctionBiddedAt() > 0) && (player.getClan().getAuctionBiddedAt() != Integer.parseInt(val))) || (player.getClan().getHideoutId() > 0))
				{
					player.sendPacket(SystemMessageId.ALREADY_SUBMITTED_BID);
					return;
				}
				
				try
				{
					final String filename = "data/html/auction/AgitBid1.htm";
					
					long minimumBid = AuctionManager.getInstance().getAuction(Integer.parseInt(val)).getHighestBidderMaxBid();
					if (minimumBid == 0)
					{
						minimumBid = AuctionManager.getInstance().getAuction(Integer.parseInt(val)).getStartingBid();
					}
					
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), filename);
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_bidding " + val);
					html.replace("%PLEDGE_ADENA%", String.valueOf(player.getClan().getWarehouse().getAdena()));
					html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(minimumBid));
					html.replace("npc_%objectId%_bid", "npc_" + getObjectId() + "_bid " + val);
					player.sendPacket(html);
					return;
				}
				catch (final Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("list"))
			{
				final List<Auction> auctions = AuctionManager.getInstance().getAuctions();
				final SimpleDateFormat format = new SimpleDateFormat("yy/MM/dd");
				int limit = 15;
				int start;
				int i = 1;
				final double npage = Math.ceil((float) auctions.size() / limit);
				int curPage = 0;
				if (val.isEmpty())
				{
					start = 1;
					curPage = 1;
				}
				else
				{
					curPage = Integer.parseInt(val);
					start = (limit * (curPage - 1)) + 1;
					limit *= curPage;
				}
				
				final StringBuilder items = new StringBuilder();
				items.append("<table width=280 border=0>");
				for (final Auction a : auctions)
				{
					if (a == null)
					{
						continue;
					}
					
					if (i > limit)
					{
						break;
					}
					else if (i < start)
					{
						i++;
						continue;
					}
					else
					{
						i++;
					}
					
					items.append("<tr>");
					items.append("<td fixwidth=75><font color=4169E1>");
					items.append(Util.clanHallLocation(player, ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getId()));
					items.append("</font></td>");
					items.append("<td fixwidth=180><a action=\"bypass -h npc_");
					items.append(getObjectId());
					items.append("_bidding ");
					items.append(a.getId());
					items.append("\"><font color=EEE8AA>");
					items.append(Util.clanHallName(player, a.getId()));
					items.append("[" + String.valueOf(a.getBidders().size()) + "]</font></a></td>");
					items.append("<td fixwidth=80>" + format.format(a.getEndDate()));
					items.append("</td>");
					items.append("<td fixwidth=80><font color=00FFFF>");
					items.append(a.getStartingBid());
					items.append("</font></td>");
					items.append("</tr>");
				}
				
				items.append("</table>");
				final String filename = "data/html/auction/AgitAuctionList.htm";
				
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(player, player.getLang(), filename);
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				html.replace("%itemsField%", items.toString());
				html.replace("%prev%", curPage > 1 && curPage <= npage ? "<button action=\"bypass -h npc_" + getObjectId() + "_list " + (curPage - 1) + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.PREVIOUS") + "\" width=80 height=27 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "");
				html.replace("%next%", curPage < npage ? "<button action=\"bypass -h npc_" + getObjectId() + "_list " + (curPage + 1) + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NEXT") + "\" width=80 height=27 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "");
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("bidlist"))
			{
				int auctionId = 0;
				if (val.isEmpty())
				{
					if (player.getClan().getAuctionBiddedAt() <= 0)
					{
						return;
					}
					auctionId = player.getClan().getAuctionBiddedAt();
				}
				else
				{
					auctionId = Integer.parseInt(val);
				}
				
				if (Config.DEBUG)
				{
					_log.warn("cmd bidlist: auction test started");
				}
				
				String biders = "";
				final Auction auc = AuctionManager.getInstance().getAuction(auctionId);
				final Map<Integer, Bidder> bidders = auc.getBidders();
				for (final Bidder b : bidders.values())
				{
					final var clan = ClanHolder.getInstance().getClan(b.getClanId());
					if (clan == null)
					{
						continue;
					}
					biders += "<tr>" + "<td fixwidth=160><font color=4169E1>" + Util.clanHallName(player, auc.getId()) + "</font></td><td fixwidth=100><font color=EEE8AA>" + clan.getName() + "</font></td><td fixwidth=80>" + b.getTimeBid().get(Calendar.YEAR) + "/" + (b.getTimeBid().get(Calendar.MONTH) + 1) + "/" + b.getTimeBid().get(Calendar.DATE) + "</td>" + "</tr>";
				}
				final String filename = "data/html/auction/AgitBidderList.htm";
				
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(player, player.getLang(), filename);
				html.replace("%AGIT_LIST%", biders);
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%x%", val);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("selectedItems"))
			{
				if ((player.getClan() != null) && (player.getClan().getHideoutId() == 0) && (player.getClan().getAuctionBiddedAt() > 0))
				{
					final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					final String filename = "data/html/auction/AgitBidInfo.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), filename);
					final Auction a = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt());
					if (a != null)
					{
						html.replace("%AGIT_NAME%", Util.clanHallName(player, a.getId()));
						html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getLease()));
						html.replace("%AGIT_LOCATION%", Util.clanHallLocation(player, ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getId()));
						html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
						html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + String.valueOf((((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60)) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_MYBID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
						html.replace("%AGIT_AUCTION_DESC%", Util.clanHallDescription(player, ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getId()));
						html.replace("%objectId%", String.valueOf(getObjectId()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
					}
					else
					{
						_log.warn("Auctioneer Auction null for AuctionBiddedAt : " + player.getClan().getAuctionBiddedAt());
					}
					
					player.sendPacket(html);
					return;
				}
				else if ((player.getClan() != null) && (AuctionManager.getInstance().getAuction(player.getClan().getHideoutId()) != null))
				{
					final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					final String filename = "data/html/auction/AgitSaleInfo.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), filename);
					final Auction a = AuctionManager.getInstance().getAuction(player.getClan().getHideoutId());
					if (a != null)
					{
						html.replace("%AGIT_NAME%", Util.clanHallName(player, a.getId()));
						html.replace("%AGIT_OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getLease()));
						html.replace("%AGIT_LOCATION%", Util.clanHallLocation(player, ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getId()));
						html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
						html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + String.valueOf((((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60)) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_BIDCOUNT%", String.valueOf(a.getBidders().size()));
						html.replace("%AGIT_AUCTION_DESC%", Util.clanHallDescription(player, ClanHallManager.getInstance().getAuctionableHallById(a.getId()).getId()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
						html.replace("%id%", String.valueOf(a.getId()));
						html.replace("%objectId%", String.valueOf(getObjectId()));
					}
					else
					{
						_log.warn("Auctioneer Auction null for getHasHideout : " + player.getClan().getHideoutId());
					}
					
					player.sendPacket(html);
					return;
				}
				else if ((player.getClan() != null) && (player.getClan().getHideoutId() != 0))
				{
					final int ItemId = player.getClan().getHideoutId();
					final String filename = "data/html/auction/AgitInfo.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), filename);
					if (ClanHallManager.getInstance().getAuctionableHallById(ItemId) != null)
					{
						html.replace("%AGIT_NAME%", Util.clanHallName(player, ClanHallManager.getInstance().getAuctionableHallById(ItemId).getId()));
						html.replace("%AGIT_OWNER_PLEDGE_NAME%", player.getClan().getName());
						html.replace("%OWNER_PLEDGE_MASTER%", player.getClan().getLeaderName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(ItemId).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(ItemId).getLease()));
						html.replace("%AGIT_LOCATION%", Util.clanHallLocation(player, ClanHallManager.getInstance().getAuctionableHallById(ItemId).getId()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
						html.replace("%objectId%", String.valueOf(getObjectId()));
					}
					else
					{
						_log.warn("Clan Hall ID NULL : " + ItemId + " Can be caused by concurent write in ClanHallManager");
					}
					
					player.sendPacket(html);
					return;
				}
				else if ((player.getClan() != null) && (player.getClan().getHideoutId() == 0))
				{
					player.sendPacket(SystemMessageId.NO_OFFERINGS_OWN_OR_MADE_BID_FOR);
					return;
				}
				else if (player.getClan() == null)
				{
					player.sendPacket(SystemMessageId.CANNOT_PARTICIPATE_IN_AN_AUCTION);
					return;
				}
			}
			else if (actualCommand.equalsIgnoreCase("cancelBid"))
			{
				final long bid = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).getBidders().get(player.getClanId()).getBid();
				final String filename = "data/html/auction/AgitBidCancel.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(player, player.getLang(), filename);
				html.replace("%AGIT_BID%", String.valueOf(bid));
				html.replace("%AGIT_BID_REMAIN%", String.valueOf((long) (bid * 0.9)));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("doCancelBid"))
			{
				if (AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()) != null)
				{
					AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).cancelBid(player.getClanId());
					player.sendPacket(SystemMessageId.CANCELED_BID);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("cancelAuction"))
			{
				if (!((player.getClanPrivileges() & Clan.CP_CH_AUCTION) == Clan.CP_CH_AUCTION))
				{
					final String filename = "data/html/auction/not_authorized.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				final String filename = "data/html/auction/AgitSaleCancel.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(player, player.getLang(), filename);
				html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("doCancelAuction"))
			{
				if (AuctionManager.getInstance().getAuction(player.getClan().getHideoutId()) != null)
				{
					AuctionManager.getInstance().getAuction(player.getClan().getHideoutId()).cancelAuction();
					player.sendMessage("Your auction has been canceled");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("sale2"))
			{
				final String filename = "data/html/auction/AgitSale2.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(player, player.getLang(), filename);
				html.replace("%AGIT_LAST_PRICE%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("sale"))
			{
				if (!((player.getClanPrivileges() & Clan.CP_CH_AUCTION) == Clan.CP_CH_AUCTION))
				{
					final String filename = "data/html/auction/not_authorized.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				final String filename = "data/html/auction/AgitSale1.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(player, player.getLang(), filename);
				html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_PLEDGE_ADENA%", String.valueOf(player.getClan().getWarehouse().getAdena()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("rebid"))
			{
				final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
				if (!((player.getClanPrivileges() & Clan.CP_CH_AUCTION) == Clan.CP_CH_AUCTION))
				{
					final String filename = "data/html/auction/not_authorized.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				try
				{
					final String filename = "data/html/auction/AgitBid2.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(player, player.getLang(), filename);
					final Auction a = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt());
					if (a != null)
					{
						html.replace("%AGIT_AUCTION_BID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
						html.replace("npc_%objectId%_bid1", "npc_" + getObjectId() + "_bid1 " + a.getId());
					}
					else
					{
						_log.warn("Auctioneer Auction null for AuctionBiddedAt : " + player.getClan().getAuctionBiddedAt());
					}
					
					player.sendPacket(html);
				}
				catch (final Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("location"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(player, player.getLang(), "data/html/auction/location.htm");
				html.replace("%location%", MapRegionManager.getInstance().getClosestTownName(player));
				html.replace("%LOCATION%", getPictureName(player));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("start"))
			{
				showChatWindow(player);
				return;
			}
		}
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		String filename = "data/html/auction/auction-no.htm";
		
		final int condition = validateCondition(player);
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			filename = "data/html/auction/auction-busy.htm";
		}
		else
		{
			filename = "data/html/auction/auction.htm";
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		html.replace("%npcname%", getName(player.getLang()));
		player.sendPacket(html);
	}
	
	private int validateCondition(Player player)
	{
		if ((getCastle() != null) && (getCastle().getId() > 0))
		{
			if (getCastle().getSiege().getIsInProgress())
			{
				return COND_BUSY_BECAUSE_OF_SIEGE;
			}
			return COND_REGULAR;
		}
		return COND_ALL_FALSE;
	}
	
	private String getPictureName(Player plyr)
	{
		final int nearestTownId = MapRegionManager.getInstance().getMapRegionLocId(plyr);
		String nearestTown;
		
		switch (nearestTownId)
		{
			case 911 :
				nearestTown = "GLUDIN";
				break;
			case 912 :
				nearestTown = "GLUDIO";
				break;
			case 916 :
				nearestTown = "DION";
				break;
			case 918 :
				nearestTown = "GIRAN";
				break;
			case 1537 :
				nearestTown = "RUNE";
				break;
			case 1538 :
				nearestTown = "GODARD";
				break;
			case 1714 :
				nearestTown = "SCHUTTGART";
				break;
			default :
				nearestTown = "ADEN";
				break;
		}
		return nearestTown;
	}
}