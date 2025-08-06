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
package l2e.gameserver.model.entity;

import static l2e.gameserver.model.items.itemcontainer.PcInventory.ADENA_ID;
import static l2e.gameserver.model.items.itemcontainer.PcInventory.MAX_ADENA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.AuctionManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;

public class Auction
{
	protected static final Logger _log = LoggerFactory.getLogger(Auction.class);
	private int _id = 0;
	private long _endDate;
	private int _highestBidderId = 0;
	private String _highestBidderName = "";
	private long _highestBidderMaxBid = 0;
	private int _itemId = 0;
	private int _itemObjectId = 0;
	private final long _itemQuantity = 0;
	private int _sellerId = 0;
	private int _sellerClanId = 0;
	private String _sellerName = "";
	private long _currentBid = 0;
	private long _startingBid = 0;
	
	private final Map<Integer, Bidder> _bidders = new ConcurrentHashMap<>();
	
	private static final String[] ItemTypeName =
	{
	        "ClanHall"
	};
	
	public static enum ItemTypeEnum
	{
		ClanHall
	}
	
	public static class Bidder
	{
		private final String _name;
		private final int _clanId;
		private long _bid;
		private final Calendar _timeBid;
		
		public Bidder(String name, int clanId, long bid, long timeBid)
		{
			_name = name;
			_clanId = clanId;
			_bid = bid;
			_timeBid = Calendar.getInstance();
			_timeBid.setTimeInMillis(timeBid);
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int getClanId()
		{
			return _clanId;
		}
		
		public long getBid()
		{
			return _bid;
		}
		
		public Calendar getTimeBid()
		{
			return _timeBid;
		}
		
		public void setTimeBid(long timeBid)
		{
			_timeBid.setTimeInMillis(timeBid);
		}
		
		public void setBid(long bid)
		{
			_bid = bid;
		}
	}
	
	public class AutoEndTask implements Runnable
	{
		public AutoEndTask()
		{
		}
		
		@Override
		public void run()
		{
			try
			{
				endAuction();
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}
	
	public Auction(int auctionId)
	{
		_id = auctionId;
		load();
		startAutoTask();
	}
	
	public Auction(int itemId, Clan Clan, long delay, long bid)
	{
		_id = itemId;
		_endDate = System.currentTimeMillis() + delay;
		_itemId = itemId;
		_sellerId = Clan.getLeaderId();
		_sellerName = Clan.getLeaderName();
		_sellerClanId = Clan.getId();
		_startingBid = bid;
	}
	
	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Select * from auction where id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				_currentBid = rs.getLong("currentBid");
				_endDate = rs.getLong("endDate");
				_itemId = rs.getInt("itemId");
				_itemObjectId = rs.getInt("itemObjectId");
				_sellerId = rs.getInt("sellerId");
				_sellerClanId = rs.getInt("sellerClanId");
				_sellerName = rs.getString("sellerName");
				_startingBid = rs.getLong("startingBid");
			}
			loadBid();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: Auction.load(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}
	
	private void loadBid()
	{
		_highestBidderId = 0;
		_highestBidderName = "";
		_highestBidderMaxBid = 0;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT bidderId, bidderName, maxBid, clanId, time_bid FROM auction_bid WHERE auctionId = ? ORDER BY maxBid DESC");
			statement.setInt(1, getId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				if (rs.isFirst())
				{
					_highestBidderId = rs.getInt("bidderId");
					_highestBidderName = rs.getString("bidderName");
					_highestBidderMaxBid = rs.getLong("maxBid");
				}
				_bidders.put(rs.getInt("bidderId"), new Bidder(rs.getString("bidderName"), rs.getInt("clanId"), rs.getLong("maxBid"), rs.getLong("time_bid")));
			}
		}
		catch (final Exception e)
		{
			_log.warn("Exception: Auction.loadBid(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}
	
	private void startAutoTask()
	{
		final long currentTime = System.currentTimeMillis();
		long taskDelay = 0;
		if (_endDate <= currentTime)
		{
			_endDate = currentTime + (7 * 24 * 3600000);
			saveAuctionDate();
		}
		else
		{
			taskDelay = _endDate - currentTime;
		}
		ThreadPoolManager.getInstance().schedule(new AutoEndTask(), taskDelay);
	}
	
	public static String getItemTypeName(ItemTypeEnum value)
	{
		return ItemTypeName[value.ordinal()];
	}
	
	private void saveAuctionDate()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Update auction set endDate = ? where id = ?");
			statement.setLong(1, _endDate);
			statement.setInt(2, _id);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: saveAuctionDate(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public synchronized void setBid(Player bidder, long bid)
	{
		long requiredAdena = bid;
		if (getHighestBidderName().equals(bidder.getClan().getLeaderName()))
		{
			requiredAdena = bid - getHighestBidderMaxBid();
		}
		if (((getHighestBidderId() > 0) && (bid > getHighestBidderMaxBid())) || ((getHighestBidderId() == 0) && (bid >= getStartingBid())))
		{
			if (takeItem(bidder, requiredAdena))
			{
				updateInDB(bidder, bid);
				bidder.getClan().setAuctionBiddedAt(_id, true);
				return;
			}
		}
		if ((bid < getStartingBid()) || (bid <= getHighestBidderMaxBid()))
		{
			bidder.sendPacket(SystemMessageId.BID_PRICE_MUST_BE_HIGHER);
		}
	}
	
	private void returnItem(int clanId, long quantity, boolean penalty)
	{
		if (penalty)
		{
			quantity *= 0.9;
		}
		
		final var clan = ClanHolder.getInstance().getClan(clanId);
		if (clan == null)
		{
			return;
		}
		final long limit = MAX_ADENA - clan.getWarehouse().getAdena();
		quantity = Math.min(quantity, limit);
		clan.getWarehouse().addItem("Outbidded", ADENA_ID, quantity, null, null);
	}
	
	private boolean takeItem(Player bidder, long quantity)
	{
		if ((bidder.getClan() != null) && (bidder.getClan().getWarehouse().getAdena() >= quantity))
		{
			bidder.getClan().getWarehouse().destroyItemByItemId("Buy", ADENA_ID, quantity, bidder, bidder);
			return true;
		}
		bidder.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_IN_CWH);
		return false;
	}
	
	private void updateInDB(Player bidder, long bid)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			if (getBidders().get(bidder.getClanId()) != null)
			{
				statement = con.prepareStatement("UPDATE auction_bid SET bidderId=?, bidderName=?, maxBid=?, time_bid=? WHERE auctionId=? AND bidderId=?");
				statement.setInt(1, bidder.getClanId());
				statement.setString(2, bidder.getClan().getLeaderName());
				statement.setLong(3, bid);
				statement.setLong(4, System.currentTimeMillis());
				statement.setInt(5, getId());
				statement.setInt(6, bidder.getClanId());
				statement.execute();
			}
			else
			{
				statement = con.prepareStatement("INSERT INTO auction_bid (id, auctionId, bidderId, bidderName, maxBid, clanId, time_bid) VALUES (?, ?, ?, ?, ?, ?, ?)");
				statement.setInt(1, IdFactory.getInstance().getNextId());
				statement.setInt(2, getId());
				statement.setInt(3, bidder.getClanId());
				statement.setString(4, bidder.getName(null));
				statement.setLong(5, bid);
				statement.setInt(6, bidder.getClan().getId());
				statement.setLong(7, System.currentTimeMillis());
				statement.execute();
				
				final var bidd = GameObjectsStorage.getPlayer(_highestBidderName);
				if (bidd != null)
				{
					bidd.sendMessage("You have been out bidded");
				}
			}
			
			_highestBidderId = bidder.getClanId();
			_highestBidderMaxBid = bid;
			_highestBidderName = bidder.getClan().getLeaderName();
			if (_bidders.get(_highestBidderId) == null)
			{
				_bidders.put(_highestBidderId, new Bidder(_highestBidderName, bidder.getClan().getId(), bid, Calendar.getInstance().getTimeInMillis()));
			}
			else
			{
				_bidders.get(_highestBidderId).setBid(bid);
				_bidders.get(_highestBidderId).setTimeBid(Calendar.getInstance().getTimeInMillis());
			}
			bidder.sendPacket(SystemMessageId.BID_IN_CLANHALL_AUCTION);
		}
		catch (final Exception e)
		{
			_log.warn("Exception: Auction.updateInDB(Player bidder, int bid): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	private void removeBids()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=?");
			statement.setInt(1, getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: Auction.deleteFromDB(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		for (final Bidder b : _bidders.values())
		{
			final var clan = ClanHolder.getInstance().getClan(b.getClanId());
			if (clan == null)
			{
				continue;
			}
			
			if (clan.getHideoutId() == 0)
			{
				returnItem(b.getClanId(), b.getBid(), true);
			}
			else
			{
				final var bidd = GameObjectsStorage.getPlayer(b.getName());
				if (bidd != null)
				{
					bidd.sendMessage("Congratulation you have won ClanHall!");
				}
			}
			clan.setAuctionBiddedAt(0, true);
		}
		_bidders.clear();
	}
	
	public void deleteAuctionFromDB()
	{
		AuctionManager.getInstance().getAuctions().remove(this);
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auction WHERE itemId=?");
			statement.setInt(1, _itemId);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: Auction.deleteFromDB(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void endAuction()
	{
		if (ClanHallManager.getInstance().loaded())
		{
			if ((_highestBidderId == 0) && (_sellerId == 0))
			{
				startAutoTask();
				return;
			}
			if ((_highestBidderId == 0) && (_sellerId > 0))
			{
				final int aucId = AuctionManager.getInstance().getAuctionIndex(_id);
				AuctionManager.getInstance().getAuctions().remove(aucId);
				return;
			}
			if (_sellerId > 0)
			{
				returnItem(_sellerClanId, _highestBidderMaxBid, true);
				returnItem(_sellerClanId, ClanHallManager.getInstance().getAuctionableHallById(_itemId).getLease(), false);
			}
			deleteAuctionFromDB();
			final Clan clan = ClanHolder.getInstance().getClan(_bidders.get(_highestBidderId).getClanId());
			_bidders.remove(_highestBidderId);
			clan.setAuctionBiddedAt(0, true);
			removeBids();
			ClanHallManager.getInstance().setOwner(_itemId, clan);
		}
		else
		{
			ThreadPoolManager.getInstance().schedule(new AutoEndTask(), 3000);
		}
	}
	
	public synchronized void cancelBid(int bidder)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=? AND bidderId=?");
			statement.setInt(1, getId());
			statement.setInt(2, bidder);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: Auction.cancelBid(String bidder): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		returnItem(_bidders.get(bidder).getClanId(), _bidders.get(bidder).getBid(), true);
		ClanHolder.getInstance().getClan(_bidders.get(bidder).getClanId()).setAuctionBiddedAt(0, true);
		_bidders.clear();
		loadBid();
	}
	
	public void cancelAuction()
	{
		deleteAuctionFromDB();
		removeBids();
	}
	
	public void confirmAuction()
	{
		AuctionManager.getInstance().getAuctions().add(this);
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO auction (id, sellerId, sellerName, sellerClanId, itemId, itemObjectId, itemQuantity, startingBid, currentBid, endDate) VALUES (?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, getId());
			statement.setInt(2, _sellerId);
			statement.setString(3, _sellerName);
			statement.setInt(4, _sellerClanId);
			statement.setInt(5, _itemId);
			statement.setInt(6, _itemObjectId);
			statement.setLong(7, _itemQuantity);
			statement.setLong(8, _startingBid);
			statement.setLong(9, _currentBid);
			statement.setLong(10, _endDate);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Exception: Auction.load(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public final int getId()
	{
		return _id;
	}
	
	public final long getCurrentBid()
	{
		return _currentBid;
	}
	
	public final long getEndDate()
	{
		return _endDate;
	}
	
	public final int getHighestBidderId()
	{
		return _highestBidderId;
	}
	
	public final String getHighestBidderName()
	{
		return _highestBidderName;
	}
	
	public final long getHighestBidderMaxBid()
	{
		return _highestBidderMaxBid;
	}
	
	public final int getItemId()
	{
		return _itemId;
	}
	
	public final int getObjectId()
	{
		return _itemObjectId;
	}
	
	public final long getItemQuantity()
	{
		return _itemQuantity;
	}
	
	public final int getSellerId()
	{
		return _sellerId;
	}
	
	public final String getSellerName()
	{
		return _sellerName;
	}
	
	public final int getSellerClanId()
	{
		return _sellerClanId;
	}
	
	public final long getStartingBid()
	{
		return _startingBid;
	}
	
	public final Map<Integer, Bidder> getBidders()
	{
		return _bidders;
	}
	
	public String getSellerClanName()
	{
		if (_sellerClanId > 0)
		{
			final var clan = ClanHolder.getInstance().getClan(_sellerClanId);
			if (clan != null)
			{
				return clan.getName();
			}
		}
		return "NPC CLAN";
	}
}