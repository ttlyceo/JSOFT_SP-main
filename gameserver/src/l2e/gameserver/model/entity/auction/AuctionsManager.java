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
package l2e.gameserver.model.entity.auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.HennaParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.ItemAuction;
import l2e.gameserver.model.items.ItemRequest;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.items.type.ArmorType;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;

public class AuctionsManager
{
	private static final Logger _log = LoggerFactory.getLogger(AuctionsManager.class);
	
	private final Map<Integer, Auction> _auctions = new ConcurrentHashMap<>();
	private final List<Integer> _deadAuctions = new ArrayList<>();
	private final Map<Integer, Long> _lastMadeAuction = new ConcurrentHashMap<>();
	private int _lastId = -1;
	
	private AuctionsManager()
	{
		loadAuctions();
	}

	public Auction getAuction(int auctionId)
	{
		return _auctions.get(auctionId);
	}

	public Auction getAuction(ItemInstance item)
	{
		for (final Auction auction : getAllAuctions())
		{
			if (auction.getItem().equals(item))
			{
				return auction;
			}
		}
		return null;
	}

	public Collection<Auction> getAllAuctions()
	{
		return _auctions.values();
	}
	
	public Collection<Auction> getAllAuctionsPerItemId(int itemId)
	{
		final Collection<Auction> coll = new ArrayList<>();
		for (final Auction auction : getAllAuctions())
		{
			if (auction != null && auction.getPriceItemId() == itemId)
			{
				coll.add(auction);
			}
		}
		return coll;
	}

	public Collection<Auction> getMyAuctions(Player player, int priceItemId)
	{
		return getMyAuctions(player.getObjectId(), priceItemId);
	}

	public Collection<Auction> getMyAuctions(int playerObjectId, int priceItemId)
	{
		final Collection<Auction> coll = new ArrayList<>();
		for (final Auction auction : getAllAuctions())
		{
			if (auction != null && auction.getSellerObjectId() == playerObjectId && auction.getPriceItemId() == priceItemId)
			{
				coll.add(auction);
			}
		}
		return coll;
	}

	private void loadAuctions()
	{
		ItemAuction.getInstance();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM auctions");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int id = rset.getInt("auction_id");
				final int sellerObjectId = rset.getInt("seller_object_id");
				final String sellerName = rset.getString("seller_name");
				final int itemObjectId = rset.getInt("item_object_id");
				final int priceItemId = rset.getInt("price_itemId");
				final long pricePerItem = rset.getLong("price_per_item");
				final ItemInstance item = ItemAuction.getInstance().getItemByObjectId(itemObjectId);
				if (id > _lastId)
				{
					_lastId = id;
				}

				if (item != null)
				{
					final Auction auction = new Auction(id, sellerObjectId, sellerName, item, priceItemId, pricePerItem, item.getCount(), getItemGroup(item), false);
					_auctions.put(id, auction);
				}
				else
				{
					_deadAuctions.add(id);
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error while loading Auctions", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void addAuctionToDatabase(Auction auction)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO auctions VALUES(?,?,?,?,?,?)");
			statement.setInt(1, auction.getAuctionId());
			statement.setInt(2, auction.getSellerObjectId());
			statement.setString(3, auction.getSellerName());
			statement.setInt(4, auction.getItem().getObjectId());
			statement.setInt(5, auction.getPriceItemId());
			statement.setLong(6, auction.getPricePerItem());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Error while adding auction to database:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void addItemIdToSeller(int sellerObjectId, int itemId, long count)
	{
		int objId = -1;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT object_id FROM items WHERE item_id=" + itemId + " AND owner_id=" + sellerObjectId + " AND loc='INVENTORY'");
			rset = statement.executeQuery();
			if (rset.next())
			{
				objId = rset.getInt("object_id");
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error while selecting itemId: " + itemId + "", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		if (objId == -1)
		{
			final ItemInstance item = ItemsParser.getInstance().createItem("Auction", itemId, count, null, null);
			item.setCount(count);
			item.setOwnerId(sellerObjectId);
			item.setItemLocation(ItemLocation.INVENTORY);
			item.updateDatabase();
		}
		else
		{
			Connection conn = null;
			PreparedStatement statement1 = null;
			try
			{
				conn = DatabaseFactory.getInstance().getConnection();
				statement1 = conn.prepareStatement("UPDATE items SET count=count+" + count + " WHERE object_id=" + objId);
				statement1.execute();
			}
			catch (final Exception e)
			{
				_log.warn("Error while selecting itemId: " + itemId + "", e);
			}
			finally
			{
				DbUtils.closeQuietly(conn, statement1);
			}
		}
	}

	private void deleteAuctionFromDatabase(Auction auction)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auctions WHERE auction_id = ?");
			statement.setInt(1, auction.getAuctionId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Error while deleting auction from database:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void deleteAuction(Player seller, ItemInstance item, int priceItemId)
	{
		Auction auction = null;
		for (final Auction anyAuction : getMyAuctions(seller, priceItemId))
		{
			if (anyAuction.getItem().equals(item))
			{
				auction = anyAuction;
				break;
			}
		}
		deleteAuction(seller, auction);
	}

	public void deleteAuction(Player seller, Auction auction)
	{
		if (auction == null)
		{
			sendMessage(seller, "This auction doesnt exist anymore!");
			return;
		}
		
		final ItemInstance item = auction.getItem();
		final long count = item.getCount();
		
		if (!Config.ALLOW_AUCTION_OUTSIDE_TOWN && !seller.isInsideZone(ZoneId.PEACE))
		{
			sendMessage(seller, "You cannot delete auction outside town!");
		}
		_auctions.remove(auction.getAuctionId());
		
		final PcInventory inventory = seller.getInventory();
		final ItemAuction storage = ItemAuction.getInstance();
		
		try
		{
			final ItemInstance createdItem = inventory.addItem("Remove Auction", item.getId(), count, seller, true);
			createdItem.setItemLocation(ItemLocation.INVENTORY);
			createdItem.setEnchantLevel(item.getEnchantLevel());
			createdItem.setCustomType1(item.getCustomType1());
			createdItem.setCustomType2(item.getCustomType2());
			createdItem.setAugmentation(item.getAugmentation());
			createdItem.setVisualItemId(item.getVisualItemId());
			if (item.getElementals() != null)
			{
				for (final Elementals elm : item.getElementals())
				{
					if (elm.getElement() != -1 && elm.getValue() != -1)
					{
						createdItem.setElementAttr(elm.getElement(), elm.getValue());
					}
				}
			}
			createdItem.updateDatabase(true);
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(createdItem);
			seller.sendPacket(iu);

			storage.removeItemFromDb(item.getObjectId());
			storage.removeItem(item);
		}
		finally
		{}
		
		deleteAuctionFromDatabase(auction);
		sendMessage(seller, "Auction has been removed!");
	}

	public Auction addNewStore(Player seller, ItemInstance item, int saleItemId, long salePrice, long count)
	{
		final int id = getNewId();
		final AuctionItemTypes type = getItemGroup(item);
		return addAuction(seller, id, item, saleItemId, salePrice, count, type, true);
	}

	public void removeStore(Player seller, int auctionId)
	{
		if (!Config.AUCTION_PRIVATE_STORE_AUTO_ADDED)
		{
			return;
		}

		if (auctionId <= 0)
		{
			return;
		}
		final Auction a = getAuction(auctionId);
		if (a == null || !a.isPrivateStore() || a.getSellerObjectId() != seller.getObjectId())
		{
			return;
		}
		_auctions.remove(auctionId);
	}

	public synchronized void removePlayerStores(Player player)
	{
		if (!Config.AUCTION_PRIVATE_STORE_AUTO_ADDED)
		{
			return;
		}

		final int playerObjId = player.getObjectId();
		final List<Integer> keysToRemove = new ArrayList<>();
		for (final Entry<Integer, Auction> auction : _auctions.entrySet())
		{
			if (auction.getValue().getSellerObjectId() == playerObjId && auction.getValue().isPrivateStore())
			{
				keysToRemove.add(auction.getKey());
			}
		}
		for (final Integer key : keysToRemove)
		{
			_auctions.remove(key);
		}
	}

	public void setNewCount(int auctionId, long newCount)
	{
		if (auctionId <= 0)
		{
			return;
		}
		_auctions.get(auctionId).setCount(newCount);
	}

	public void buyItem(Player buyer, ItemInstance item, long quantity)
	{
		final Auction auction = getAuction(item);
		if (auction == null)
		{
			sendMessage(buyer, "This auction doesnt exist anymore!");
			return;
		}
		if (buyer.isBlocked())
		{
			sendMessage(buyer, "You cannot buy items while being blocked!");
			return;
		}
		if (auction.getSellerObjectId() == buyer.getObjectId())
		{
			sendMessage(buyer, "You cannot win your own auction!");
			return;
		}
		if (quantity <= 0)
		{
			sendMessage(buyer, "You need to buy at least one item!");
			return;
		}
		if (item.getCount() < quantity)
		{
			sendMessage(buyer, "You are trying to buy too many items!");
			return;
		}
		if (buyer.getInventory().getItemByItemId(auction.getPriceItemId()) == null || auction.getPricePerItem() * quantity > buyer.getInventory().getItemByItemId(auction.getPriceItemId()).getCount())
		{
			sendMessage(buyer, "You don't have enough " + Util.getItemName(buyer, auction.getPriceItemId()) + "!");
			return;
		}

		if (!Config.ALLOW_AUCTION_OUTSIDE_TOWN && !buyer.isInsideZone(ZoneId.PEACE))
		{
			sendMessage(buyer, "You can't use buy that item outside town!");
			return;
		}

		if (auction.isPrivateStore())
		{
			final Player seller = GameObjectsStorage.getPlayer(auction.getSellerObjectId());
			if (seller == null)
			{
				sendMessage(buyer, "This auction doesnt exist anymore !");
				return;
			}
			final Set<ItemRequest> _items = new HashSet<>();
			_items.add(new ItemRequest(item.getObjectId(), quantity, auction.getPricePerItem()));
			seller.getSellList().privateStoreBuy(buyer, _items);

			if (seller.getSellList().getItemCount() == 0)
			{
				seller.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
				seller.broadcastUserInfo(true);
			}
			return;
		}

		buyer.destroyItemByItemId("Auction Online Bought", auction.getPriceItemId(), auction.getPricePerItem() * quantity, null, true);
		boolean wholeItemBought = false;

		final PcInventory inventory = buyer.getInventory();
		final ItemAuction storage = ItemAuction.getInstance();
		try
		{
			if (item.getCount() == quantity)
			{
				final ItemInstance createdItem = inventory.addItem("Auction Part Bought", item.getId(), quantity, buyer, true);
				createdItem.setItemLocation(ItemLocation.INVENTORY);
				createdItem.setEnchantLevel(item.getEnchantLevel());
				createdItem.setCustomType1(item.getCustomType1());
				createdItem.setCustomType2(item.getCustomType2());
				createdItem.setAugmentation(item.getAugmentation());
				createdItem.setVisualItemId(item.getVisualItemId());
				if (item.getElementals() != null)
				{
					for (final Elementals elm : item.getElementals())
					{
						if (elm.getElement() != -1 && elm.getValue() != -1)
						{
							createdItem.setElementAttr(elm.getElement(), elm.getValue());
						}
					}
				}
				createdItem.updateDatabase(true);
				
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(createdItem);
				buyer.sendPacket(iu);
				
				storage.removeItemFromDb(item.getObjectId());
				storage.removeItem(item);
				
				deleteAuctionFromDatabase(auction);
				_auctions.remove(auction.getAuctionId());
				wholeItemBought = true;
			}
			else
			{
				final ItemInstance newItem = copyItem(item, quantity);
				final ItemInstance createdItem = inventory.addItem("Auction Part Bought", newItem.getId(), newItem.getCount(), buyer, true);

				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(createdItem);
				buyer.sendPacket(iu);
				
				storage.changeCount(item, auction.getCountToSell() - quantity);
				auction.setCount(auction.getCountToSell() - quantity);
			}
		}
		finally
		{}

		final Player seller = GameObjectsStorage.getPlayer(auction.getSellerObjectId());
		if (seller != null)
		{
			if (wholeItemBought)
			{
				seller.sendMessage(item.getName(seller.getLang()) + " has been bought by " + buyer.getName(null) + "!");
			}
			else
			{
				seller.sendMessage(quantity + " " + item.getName(seller.getLang()) + (quantity > 1 ? "s" : "") + " has been bought by " + buyer.getName(null) + "!");
			}
			seller.addItem("Auction Online Sold", auction.getPriceItemId(), auction.getPricePerItem() * quantity, null, true);
		}
		else
		{
			addItemIdToSeller(auction.getSellerObjectId(), auction.getPriceItemId(), auction.getPricePerItem() * quantity);
		}
		buyer.sendMessage("You have bought " + item.getName(buyer.getLang()));
	}

	public void checkAndAddNewAuction(Player seller, ItemInstance item, long quantity, int saleItemId, long salePrice)
	{
		if (!checkIfItsOk(seller, item, quantity, saleItemId, salePrice))
		{
			return;
		}
		
		final int id = getNewId();
		if (id < 0)
		{
			sendMessage(seller, "There are currently too many auctions!");
			return;
		}
		
		final AuctionItemTypes type = getItemGroup(item);
		
		final PcInventory inventory = seller.getInventory();
		final ItemAuction storage = ItemAuction.getInstance();
		Auction auction = null;
		
		try
		{
			if (item.getCount() > quantity)
			{
				final ItemInstance newItem = copyItem(item, quantity);
				seller.destroyItem("Create Auction", item, quantity, null, true);
				storage.addItem("Create Auction", newItem, null, null);
				auction = addAuction(seller, id, newItem, saleItemId, salePrice, quantity, type, false);
				
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				seller.sendPacket(iu);
			}
			else
			{
				inventory.removeItem(item);
				item.setCount(quantity);
				storage.addFullItem(item);
				auction = addAuction(seller, id, item, saleItemId, salePrice, quantity, type, false);
				
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(item);
				seller.sendPacket(iu);
			}
		}
		finally
		{}
		
		if (Config.ALLOW_ADDING_AUCTION_DELAY)
		{
			_lastMadeAuction.put(seller.getObjectId(), System.currentTimeMillis() + Config.SECONDS_BETWEEN_ADDING_AUCTIONS * 1000);
		}
		
		seller.getInventory().reduceAdena("Create Auctino Fee", Config.AUCTION_FEE, null, true);
		addAuctionToDatabase(auction);
		sendMessage(seller, "Auction has been created!");
	}

	private Auction addAuction(Player seller, int auctionId, ItemInstance item, int saleItemId, long salePrice, long sellCount, AuctionItemTypes itemType, boolean privateStore)
	{
		final Auction newAuction = new Auction(auctionId, seller.getObjectId(), seller.getName(null), item, saleItemId, salePrice, sellCount, itemType, privateStore);
		_auctions.put(auctionId, newAuction);
		return newAuction;
	}

	public void sendMessage(Player player, String message)
	{
		player.sendMessage(message);
	}

	private ItemInstance copyItem(ItemInstance oldItem, long quantity)
	{
		final ItemInstance item = new ItemInstance(IdFactory.getInstance().getNextId(), oldItem.getId());
		item.setOwnerId(oldItem.getOwnerId());
		item.setCount(quantity);
		item.setEnchantLevel(oldItem.getEnchantLevel());
		item.setItemLocation(ItemLocation.AUCTION);
		item.setCustomType1(oldItem.getCustomType1());
		item.setCustomType2(oldItem.getCustomType2());
		item.setAugmentation(oldItem.getAugmentation());
		item.setVisualItemId(oldItem.getVisualItemId());
		if (oldItem.getElementals() != null)
		{
			for (final Elementals elm : oldItem.getElementals())
			{
				if (elm.getElement() != -1 && elm.getValue() != -1)
				{
					item.setElementAttr(elm.getElement(), elm.getValue());
				}
			}
		}
		return item;
	}

	private synchronized int getNewId()
	{
		return ++_lastId;
	}

	private boolean checkIfItsOk(Player seller, ItemInstance item, long quantity, int priceItemId, long salePrice)
	{
		if (seller == null)
		{
			return false;
		}

		if (item == null)
		{
			sendMessage(seller, "Item you are trying to sell, doesn't exist!");
			return false;
		}

		if (item.getOwnerId() != seller.getObjectId() || seller.getInventory().getItemByObjectId(item.getObjectId()) == null)
		{
			sendMessage(seller, "Item you are trying to sell, doesn't exist!");
			return false;
		}

		if (item.isEquipped())
		{
			sendMessage(seller, "You need to unequip that item first!");
			return false;
		}

		if (item.isAugmented())
		{
			sendMessage(seller, "You cannot sell Augmented weapons!");
			return false;
		}

		if (item.isQuestItem())
		{
			sendMessage(seller, "You can't sell quest items!");
			return false;
		}

		if (!item.isTradeable())
		{
			sendMessage(seller, "You cannot sell this item!");
			return false;
		}

		if (seller.getSummon() != null && item.getItemType() == EtcItemType.PET_COLLAR)
		{
			sendMessage(seller, "Please unsummon your pet before trying to sell this item.");
			return false;
		}

		if (seller.getSummon() != null && item.isSummon() && item.getItem().isPetItem())
		{
			sendMessage(seller, "Please unsummon your pet before trying to sell this item.");
			return false;
		}
		if (quantity < 1)
		{
			sendMessage(seller, "Quantity is too low!");
			return false;
		}
		if (item.getCount() < quantity)
		{
			sendMessage(seller, "You don't have enough items to sell!");
			return false;
		}
		if (seller.getAdena() < Config.AUCTION_FEE)
		{
			sendMessage(seller, "You don't have enough adena, to pay the fee!");
			return false;
		}
		if (salePrice <= 0)
		{
			sendMessage(seller, "Sale price is too low!");
			return false;
		}
		if (salePrice > 999999999999L)
		{
			sendMessage(seller, "Price is too high!");
			return false;
		}
		if (seller.isBlocked())
		{
			sendMessage(seller, "Cannot create auctions while being Blocked!");
			return false;
		}
		if (getMyAuctions(seller, priceItemId).size() >= 10)
		{
			sendMessage(seller, "You can have just 10 auctions at the time!");
			return false;
		}
		if (!Config.ALLOW_AUCTION_OUTSIDE_TOWN && !seller.isInsideZone(ZoneId.PEACE))
		{
			sendMessage(seller, "You cannot add new Auction outside town!");
			return false;
		}
		if (seller.isInStoreMode())
		{
			sendMessage(seller, "Close your store before creating new Auction!");
			return false;
		}
		
		if (Config.ALLOW_ADDING_AUCTION_DELAY)
		{
			if (_lastMadeAuction.containsKey(seller.getObjectId()))
			{
				if (_lastMadeAuction.get(seller.getObjectId()) > System.currentTimeMillis())
				{
					sendMessage(seller, "You cannot do it so often!");
					return false;
				}
			}
		}
		return true;
	}

	private AuctionItemTypes getItemGroup(ItemInstance item)
	{
		if (item.isEquipable())
		{
			if (item.getItem().getBodyPart() == (Item.SLOT_L_EAR | Item.SLOT_R_EAR))
			{
				return AccessoryItemType.EARRING;
			}
			if (item.getItem().getBodyPart() == (Item.SLOT_L_FINGER | Item.SLOT_R_FINGER))
			{
				return AccessoryItemType.RING;
			}
			if (item.getItem().getBodyPart() == Item.SLOT_NECK)
			{
				return AccessoryItemType.NECKLACE;
			}
			if (item.getItem().getBodyPart() == Item.SLOT_L_BRACELET || item.getItem().getBodyPart() == Item.SLOT_R_BRACELET)
			{
				return AccessoryItemType.BRACELET;
			}
			if (item.getItem().getBodyPart() == Item.SLOT_HAIR || item.getItem().getBodyPart() == Item.SLOT_HAIRALL || item.getItem().getBodyPart() == Item.SLOT_HAIR2)
			{
				return AccessoryItemType.ACCESSORY;
			}
		}

		if (item.isArmor())
		{
			if (item.getItem().getBodyPart() == Item.SLOT_HEAD)
			{
				return ArmorItemType.HELMET;
			}
			if (item.getItem().getBodyPart() == Item.SLOT_CHEST)
			{
				return ArmorItemType.CHEST;
			}
			if (item.getItem().getBodyPart() == Item.SLOT_LEGS)
			{
				return ArmorItemType.LEGS;
			}
			if (item.getItem().getBodyPart() == Item.SLOT_GLOVES)
			{
				return ArmorItemType.GLOVES;
			}
			if (item.getItem().getBodyPart() == Item.SLOT_FEET)
			{
				return ArmorItemType.SHOES;
			}
			if (item.getItem().isCloak())
			{
				return ArmorItemType.CLOAK;
			}
			if (item.getItem().isUnderwear())
			{
				return ArmorItemType.SHIRT;
			}
			if (item.getItem().isBelt())
			{
				return ArmorItemType.BELT;
			}
		}
		if (item.getItem().isEnchantScroll())
		{
			return EtcAuctionItemType.ENCHANT;
		}
		if (item.getItem().isLifeStone())
		{
			return EtcAuctionItemType.LIFE_STONE;
		}
		if (item.getItem().isAttributeCrystal() || item.getItem().isAttributeStone())
		{
			return EtcAuctionItemType.ATTRIBUTE;
		}
		if (item.getItem().isCodexBook())
		{
			return EtcAuctionItemType.CODEX;
		}
		if (item.getItem().isForgottenScroll())
		{
			return EtcAuctionItemType.FORGOTTEN_SCROLL;
		}
		if (item.getItem().isSoulCrystal())
		{
			return EtcAuctionItemType.SA_CRYSTAL;
		}
		if (item.isPet())
		{
			return PetItemType.PET;
		}
		if (item.getItemType() == EtcItemType.PET_COLLAR)
		{
			return PetItemType.PET;
		}
		if (item.getItem().isPetItem())
		{
			return PetItemType.GEAR;
		}
		if (isBabyFoodOrShot(item.getId()))
		{
			return PetItemType.OTHER;
		}
		if (item.getItemType() == EtcItemType.POTION)
		{
			return SuppliesItemType.ELIXIR;
		}
		if (HennaParser.getInstance().isHenna(item.getId()))
		{
			return SuppliesItemType.DYE;
		}
		if (item.getItemType() == EtcItemType.SCROLL)
		{
			return SuppliesItemType.SCROLL;
		}
		if (item.getItem().isKeyMatherial())
		{
			return SuppliesItemType.KEY_MATERIAL;
		}
		if (item.getItem().isRecipe())
		{
			return SuppliesItemType.RECIPE;
		}
		if (item.getItemType() == EtcItemType.MATERIAL)
		{
			return SuppliesItemType.MATERIAL;
		}
		if (item.getItemType() instanceof EtcItemType)
		{
			return SuppliesItemType.MISCELLANEOUS;
		}

		if (item.isWeapon())
		{
			if (item.getItemType() == WeaponType.SWORD)
			{
				return WeaponItemType.SWORD;
			}
			if (item.getItemType() == WeaponType.ANCIENTSWORD)
			{
				return WeaponItemType.ANCIENT_SWORD;
			}
			if (item.getItemType() == WeaponType.BIGSWORD)
			{
				return WeaponItemType.BIG_SWORD;
			}
			if (item.getItemType() == WeaponType.BLUNT)
			{
				return WeaponItemType.BLUNT;
			}
			if (item.getItemType() == WeaponType.BIGBLUNT)
			{
				return WeaponItemType.BIG_BLUNT;
			}
			if (item.getItemType() == WeaponType.DAGGER)
			{
				return WeaponItemType.DAGGER;
			}
			if (item.getItemType() == WeaponType.DUALDAGGER)
			{
				return WeaponItemType.DUAL_DAGGER;
			}
			if (item.getItemType() == WeaponType.BOW)
			{
				return WeaponItemType.BOW;
			}
			if (item.getItemType() == WeaponType.CROSSBOW)
			{
				return WeaponItemType.CROSSBOW;
			}
			if (item.getItemType() == WeaponType.POLE)
			{
				return WeaponItemType.POLE;
			}
			if (item.getItemType() == WeaponType.DUALFIST)
			{
				return WeaponItemType.FISTS;
			}
			if (item.getItemType() == WeaponType.RAPIER)
			{
				return WeaponItemType.RAPIER;
			}
			return WeaponItemType.OTHER_W;
		}

		if (item.getItem().getBodyPart() == Item.SLOT_L_HAND)
		{
			if (item.getItemType() == ArmorType.SIGIL)
			{
				return ArmorItemType.SIGIL;
			}
			else
			{
				return ArmorItemType.SHIELD;
			}
		}
		return SuppliesItemType.MISCELLANEOUS;
	}

	private static final int[] PET_FOOD_OR_SHOT =
	{
	        6316, 2515, 4038, 5168, 5169, 7582, 9668, 10425, 6645, 20332, 20329, 20326, 10515, 6647, 6646, 20334, 20333, 20331, 20330, 20329, 20327, 10517, 10516
	};

	private boolean isBabyFoodOrShot(int id)
	{
		for (final int i : PET_FOOD_OR_SHOT)
		{
			if (i == id)
			{
				return true;
			}
		}
		return false;
	}

	public static final AuctionsManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AuctionsManager _instance = new AuctionsManager();
	}
}
