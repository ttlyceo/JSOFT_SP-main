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
package l2e.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.entity.Auction;

public final class AuctionManager extends LoggerObject
{
	private final List<Auction> _auctions = new ArrayList<>();

	private static final String[] ITEM_INIT_DATA =
	{
	        "(22, 0, 'NPC', '0', 22, 0, 'Moonstone Hall', 1, 20000000, 0, 1073037600000)", "(23, 0, 'NPC', '0', 23, 0, 'Onyx Hall', 1, 20000000, 0, 1073037600000)", "(24, 0, 'NPC', '0', 24, 0, 'Topaz Hall', 1, 20000000, 0, 1073037600000)", "(25, 0, 'NPC', '0', 25, 0, 'Ruby Hall', 1, 20000000, 0, 1073037600000)", "(26, 0, 'NPC', '0', 26, 0, 'Crystal Hall', 1, 20000000, 0, 1073037600000)", "(27, 0, 'NPC', '0', 27, 0, 'Onyx Hall', 1, 20000000, 0, 1073037600000)", "(28, 0, 'NPC', '0', 28, 0, 'Sapphire Hall', 1, 20000000, 0, 1073037600000)", "(29, 0, 'NPC', '0', 29, 0, 'Moonstone Hall', 1, 20000000, 0, 1073037600000)", "(30, 0, 'NPC', '0', 30, 0, 'Emerald Hall', 1, 20000000, 0, 1073037600000)", "(31, 0, 'NPC', '0', 31, 0, 'The Atramental Barracks', 1, 8000000, 0, 1073037600000)", "(32, 0, 'NPC', '0', 32, 0, 'The Scarlet Barracks', 1, 8000000, 0, 1073037600000)", "(33, 0, 'NPC', '0', 33, 0, 'The Viridian Barracks', 1, 8000000, 0, 1073037600000)", "(36, 0, 'NPC', '0', 36, 0, 'The Golden Chamber', 1, 50000000, 0, 1106827200000)", "(37, 0, 'NPC', '0', 37, 0, 'The Silver Chamber', 1, 50000000, 0, 1106827200000)", "(38, 0, 'NPC', '0', 38, 0, 'The Mithril Chamber', 1, 50000000, 0, 1106827200000)", "(39, 0, 'NPC', '0', 39, 0, 'Silver Manor', 1, 50000000, 0, 1106827200000)", "(40, 0, 'NPC', '0', 40, 0, 'Gold Manor', 1, 50000000, 0, 1106827200000)", "(41, 0, 'NPC', '0', 41, 0, 'The Bronze Chamber', 1, 50000000, 0, 1106827200000)", "(42, 0, 'NPC', '0', 42, 0, 'The Golden Chamber', 1, 50000000, 0, 1106827200000)", "(43, 0, 'NPC', '0', 43, 0, 'The Silver Chamber', 1, 50000000, 0, 1106827200000)", "(44, 0, 'NPC', '0', 44, 0, 'The Mithril Chamber', 1, 50000000, 0, 1106827200000)", "(45, 0, 'NPC', '0', 45, 0, 'The Bronze Chamber', 1, 50000000, 0, 1106827200000)", "(46, 0, 'NPC', '0', 46, 0, 'Silver Manor', 1, 50000000, 0, 1106827200000)", "(47, 0, 'NPC', '0', 47, 0, 'Moonstone Hall', 1, 50000000, 0, 1106827200000)", "(48, 0, 'NPC', '0', 48, 0, 'Onyx Hall', 1, 50000000, 0, 1106827200000)", "(49, 0, 'NPC', '0', 49, 0, 'Emerald Hall', 1, 50000000, 0, 1106827200000)", "(50, 0, 'NPC', '0', 50, 0, 'Sapphire Hall', 1, 50000000, 0, 1106827200000)", "(51, 0, 'NPC', '0', 51, 0, 'Mont Chamber', 1, 50000000, 0, 1106827200000)", "(52, 0, 'NPC', '0', 52, 0, 'Astaire Chamber', 1, 50000000, 0, 1106827200000)", "(53, 0, 'NPC', '0', 53, 0, 'Aria Chamber', 1, 50000000, 0, 1106827200000)", "(54, 0, 'NPC', '0', 54, 0, 'Yiana Chamber', 1, 50000000, 0, 1106827200000)", "(55, 0, 'NPC', '0', 55, 0, 'Roien Chamber', 1, 50000000, 0, 1106827200000)", "(56, 0, 'NPC', '0', 56, 0, 'Luna Chamber', 1, 50000000, 0, 1106827200000)", "(57, 0, 'NPC', '0', 57, 0, 'Traban Chamber', 1, 50000000, 0, 1106827200000)", "(58, 0, 'NPC', '0', 58, 0, 'Eisen Hall', 1, 20000000, 0, 1106827200000)", "(59, 0, 'NPC', '0', 59, 0, 'Heavy Metal Hall', 1, 20000000, 0, 1106827200000)", "(60, 0, 'NPC', '0', 60, 0, 'Molten Ore Hall', 1, 20000000, 0, 1106827200000)", "(61, 0, 'NPC', '0', 61, 0, 'Titan Hall', 1, 20000000, 0, 1106827200000)"
	};
	
	private static final int[] ItemInitDataId =
	{
		22,
		23,
		24,
		25,
		26,
		27,
		28,
		29,
		30,
		31,
		32,
		33,
		36,
		37,
		38,
		39,
		40,
		41,
		42,
		43,
		44,
		45,
		46,
		47,
		48,
		49,
		50,
		51,
		52,
		53,
		54,
		55,
		56,
		57,
		58,
		59,
		60,
		61
	};
	
	public static final AuctionManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected AuctionManager()
	{
		load();
	}
	
	public void reload()
	{
		_auctions.clear();
		load();
	}
	
	private final void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT id FROM auction ORDER BY id");
			rs = statement.executeQuery();
			while (rs.next())
			{
				_auctions.add(new Auction(rs.getInt("id")));
			}
			info("Loaded: " + getAuctions().size() + " auction(s)");
		}
		catch (final Exception e)
		{
			warn("Exception: AuctionManager.load(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}
	
	public final Auction getAuction(int auctionId)
	{
		final int index = getAuctionIndex(auctionId);
		if (index >= 0)
		{
			return getAuctions().get(index);
		}
		return null;
	}
	
	public final int getAuctionIndex(int auctionId)
	{
		Auction auction;
		for (int i = 0; i < getAuctions().size(); i++)
		{
			auction = getAuctions().get(i);
			if ((auction != null) && (auction.getId() == auctionId))
			{
				return i;
			}
		}
		return -1;
	}
	
	public final List<Auction> getAuctions()
	{
		return _auctions;
	}
	
	public void initNPC(int id)
	{
		int i;
		for (i = 0; i < ItemInitDataId.length; i++)
		{
			if (ItemInitDataId[i] == id)
			{
				break;
			}
		}
		if ((i >= ItemInitDataId.length) || (ItemInitDataId[i] != id))
		{
			warn("Clan Hall auction not found for Id :" + id);
			return;
		}
		
		Connection con = null;
		Statement s = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			s = con.createStatement();
			s.executeUpdate("INSERT INTO `auction` VALUES " + ITEM_INIT_DATA[i]);
			_auctions.add(new Auction(id));
			info("Created auction for ClanHall: " + id);
		}
		catch (final Exception e)
		{
			warn("Exception: Auction.initNPC(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, s);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AuctionManager _instance = new AuctionManager();
	}
}