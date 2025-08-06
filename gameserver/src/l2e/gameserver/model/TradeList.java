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
package l2e.gameserver.model;

import static l2e.gameserver.model.items.itemcontainer.PcInventory.MAX_ADENA;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.math.SafeMath;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.entity.auction.AuctionsManager;
import l2e.gameserver.model.items.ItemRequest;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class TradeList
{
	private static final Logger _log = LoggerFactory.getLogger(TradeList.class);
	
	private final Player _owner;
	private Player _partner;
	private final List<TradeItem> _items = new CopyOnWriteArrayList<>();
	private String _title;
	private boolean _packaged;
	private boolean _confirmed = false;
	private boolean _locked = false;

	public TradeList(Player owner)
	{
		_owner = owner;
	}

	public Player getOwner()
	{
		return _owner;
	}
	
	public void setPartner(Player partner)
	{
		_partner = partner;
	}
	
	public Player getPartner()
	{
		return _partner;
	}
	
	public void setTitle(String title)
	{
		_title = title;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public boolean isLocked()
	{
		return _locked;
	}
	
	public boolean isConfirmed()
	{
		return _confirmed;
	}
	
	public boolean isPackaged()
	{
		return _packaged;
	}
	
	public void setPackaged(boolean value)
	{
		_packaged = value;
	}
	
	public TradeItem[] getItems()
	{
		return _items.toArray(new TradeItem[_items.size()]);
	}
	
	public List<TradeItem> getAvailableItems(PcInventory inventory)
	{
		final List<TradeItem> list = new LinkedList<>();
		for (TradeItem item : _items)
		{
			item = new TradeItem(item, item.getCount(), item.getPrice());
			inventory.adjustAvailableItem(item);
			list.add(item);
		}
		return list;
	}
	
	public int getItemCount()
	{
		return _items.size();
	}
	
	public TradeItem adjustAvailableItem(ItemInstance item)
	{
		if (item.isStackable())
		{
			for (final TradeItem exclItem : _items)
			{
				if (exclItem.getItem().getId() == item.getId())
				{
					if (item.getCount() <= exclItem.getCount())
					{
						return null;
					}
					return new TradeItem(item, item.getCount() - exclItem.getCount(), item.getReferencePrice());
				}
			}
		}
		return new TradeItem(item, item.getCount(), item.getReferencePrice());
	}
	
	public void adjustItemRequest(ItemRequest item)
	{
		for (final TradeItem filtItem : _items)
		{
			if (filtItem.getObjectId() == item.getObjectId())
			{
				if (filtItem.getCount() < item.getCount())
				{
					item.setCount(filtItem.getCount());
				}
				return;
			}
		}
		item.setCount(0);
	}
	
	public TradeItem addItem(int objectId, long count)
	{
		return addItem(objectId, count, 0);
	}
	
	public synchronized TradeItem addItem(int objectId, long count, long price)
	{
		if (isLocked())
		{
			_log.warn(_owner.getName(null) + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		final ItemInstance item = GameObjectsStorage.getItem(objectId);
		if (item == null)
		{
			_log.warn(_owner.getName(null) + ": Trying to add something other than an item!");
			return null;
		}
		
		if (!(item.isTradeable() || (getOwner().isGM() && getOwner().getAccessLevel().allowTradeRestrictedItems())) || item.isQuestItem())
		{
			_log.warn(_owner.getName(null) + ": Attempt to add a restricted item!");
			return null;
		}
		
		if (!getOwner().getInventory().canManipulateWithItemId(item.getId()))
		{
			_log.warn(_owner.getName(null) + ": Attempt to add an item that can't manipualte!");
			return null;
		}
		
		if ((count <= 0) || (count > item.getCount()))
		{
			_log.warn(_owner.getName(null) + ": Attempt to add an item with invalid item count!");
			return null;
		}
		
		if (!item.isStackable() && (count > 1))
		{
			_log.warn(_owner.getName(null) + ": Attempt to add non-stackable item to TradeList with count > 1!");
			return null;
		}
		
		if ((PcInventory.MAX_ADENA / count) < price)
		{
			_log.warn(_owner.getName(null) + ": Attempt to overflow adena !");
			return null;
		}
		
		long amount = Math.min(count, item.getCount());
		try
		{
			for (final TradeItem ti : _items)
			{
				if (ti.getObjectId() == objectId)
				{
					amount = SafeMath.addAndCheck(amount, ti.getCount());
					amount = Math.min(amount, item.getCount());
					ti.setCount(amount);
					return ti;
				}
			}
		}
		catch (final ArithmeticException ae)
		{
			return null;
		}
		
		final TradeItem titem = new TradeItem(item, amount, price);
		_items.add(titem);
		
		invalidateConfirmation();
		return titem;
	}
	
	public synchronized TradeItem addItemByItemId(int itemId, int enchant, long count, long price, Augmentation augmentation, int mana, int time, int elemAtkType, int elemAtkPower, int[] elemDefAttr)
	{
		if (isLocked())
		{
			_log.warn(_owner.getName(null) + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		final Item item = ItemsParser.getInstance().getTemplate(itemId);
		if (item == null)
		{
			_log.warn(_owner.getName(null) + ": Attempt to add invalid item to TradeList!");
			return null;
		}
		
		if (!item.isTradeable() || item.isQuestItem())
		{
			return null;
		}
		
		if (!item.isStackable() && (count > 1))
		{
			_log.warn(_owner.getName(null) + ": Attempt to add non-stackable item to TradeList with count > 1!");
			return null;
		}
		
		if ((PcInventory.MAX_ADENA / count) < price)
		{
			_log.warn(_owner.getName(null) + ": Attempt to overflow adena !");
			return null;
		}
		
		if (elemAtkType == 65534)
		{
			if (item.isWeapon())
			{
				elemAtkType = -1;
			}
			else
			{
				elemAtkType = -2;
			}
			
		}
		
		final TradeItem titem = new TradeItem(item, enchant, count, price, augmentation, mana, time, elemAtkType, elemAtkPower, elemDefAttr);
		_items.add(titem);
		invalidateConfirmation();
		return titem;
	}
	
	public synchronized TradeItem removeItem(int objectId, int itemId, long count)
	{
		if (isLocked())
		{
			_log.warn(_owner.getName(null) + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		for (final TradeItem titem : _items)
		{
			if ((titem.getObjectId() == objectId) || (titem.getItem().getId() == itemId))
			{
				if (_partner != null)
				{
					final TradeList partnerList = _partner.getActiveTradeList();
					if (partnerList == null)
					{
						_log.warn(_partner.getName(null) + ": Trading partner (" + _partner.getName(null) + ") is invalid in this trade!");
						return null;
					}
					partnerList.invalidateConfirmation();
				}
				
				if ((count != -1) && (titem.getCount() > count))
				{
					final int curCount = (int) (titem.getCount() - count);
					titem.setCount(titem.getCount() - count);
					AuctionsManager.getInstance().setNewCount(titem.getAuctionId(), curCount);
				}
				else
				{
					_items.remove(titem);
					AuctionsManager.getInstance().removeStore(_owner, titem.getAuctionId());
				}
				
				return titem;
			}
		}
		return null;
	}
	
	public synchronized void updateItems()
	{
		for (final TradeItem titem : _items)
		{
			final ItemInstance item = _owner.getInventory().getItemByObjectId(titem.getObjectId());
			if ((item == null) || (titem.getCount() < 1))
			{
				removeItem(titem.getObjectId(), -1, -1);
			}
			else if (item.getCount() < titem.getCount())
			{
				titem.setCount(item.getCount());
			}
		}
	}
	
	public void lock()
	{
		_locked = true;
	}
	
	public synchronized void clear()
	{
		_items.clear();
		_locked = false;
	}
	
	public boolean confirm()
	{
		if (_confirmed)
		{
			return true;
		}
		
		if (_partner != null)
		{
			final TradeList partnerList = _partner.getActiveTradeList();
			if (partnerList == null)
			{
				_log.warn(_partner.getName(null) + ": Trading partner (" + _partner.getName(null) + ") is invalid in this trade!");
				return false;
			}
			TradeList sync1, sync2;
			if (getOwner().getObjectId() > partnerList.getOwner().getObjectId())
			{
				sync1 = partnerList;
				sync2 = this;
			}
			else
			{
				sync1 = this;
				sync2 = partnerList;
			}
			
			synchronized (sync1)
			{
				synchronized (sync2)
				{
					_confirmed = true;
					if (partnerList.isConfirmed())
					{
						partnerList.lock();
						lock();
						if (!partnerList.validate())
						{
							return false;
						}
						if (!validate())
						{
							return false;
						}
						
						doExchange(partnerList);
					}
					else
					{
						_partner.onTradeConfirm(_owner);
					}
				}
			}
		}
		else
		{
			_confirmed = true;
		}
		
		return _confirmed;
	}
	
	public void invalidateConfirmation()
	{
		_confirmed = false;
	}
	
	private boolean validate()
	{
		if ((_owner == null) || (GameObjectsStorage.getPlayer(_owner.getObjectId()) == null))
		{
			_log.warn("Invalid owner of TradeList");
			return false;
		}
		
		for (final TradeItem titem : _items)
		{
			final ItemInstance item = _owner.checkItemManipulation(titem.getObjectId(), titem.getCount(), "transfer");
			if ((item == null) || (item.getCount() < 1))
			{
				_log.warn(_owner.getName(null) + ": Invalid Item in TradeList");
				return false;
			}
		}
		return true;
	}
	
	private boolean TransferItems(Player partner, InventoryUpdate ownerIU, InventoryUpdate partnerIU)
	{
		for (final TradeItem titem : _items)
		{
			final ItemInstance oldItem = _owner.getInventory().getItemByObjectId(titem.getObjectId());
			if (oldItem == null)
			{
				return false;
			}
			final ItemInstance newItem = _owner.getInventory().transferItem("Trade", titem.getObjectId(), titem.getCount(), partner.getInventory(), _owner, _partner);
			if (newItem == null)
			{
				return false;
			}
			
			if (ownerIU != null)
			{
				if ((oldItem.getCount() > 0) && (oldItem != newItem))
				{
					ownerIU.addModifiedItem(oldItem);
				}
				else
				{
					ownerIU.addRemovedItem(oldItem);
				}
			}
			
			if (partnerIU != null)
			{
				if (newItem.getCount() > titem.getCount())
				{
					partnerIU.addModifiedItem(newItem);
				}
				else
				{
					partnerIU.addNewItem(newItem);
				}
			}
		}
		return true;
	}
	
	public int countItemsSlots(Player partner)
	{
		int slots = 0;
		
		for (final TradeItem item : _items)
		{
			if (item == null)
			{
				continue;
			}
			final Item template = ItemsParser.getInstance().getTemplate(item.getItem().getId());
			if (template == null)
			{
				continue;
			}
			if (!template.isStackable())
			{
				slots += item.getCount();
			}
			else if (partner.getInventory().getItemByItemId(item.getItem().getId()) == null)
			{
				slots++;
			}
		}
		
		return slots;
	}
	
	public int calcItemsWeight()
	{
		long weight = 0;
		
		for (final TradeItem item : _items)
		{
			if (item == null)
			{
				continue;
			}
			final Item template = ItemsParser.getInstance().getTemplate(item.getItem().getId());
			if (template == null)
			{
				continue;
			}
			weight += item.getCount() * template.getWeight();
		}
		
		return (int) Math.min(weight, Integer.MAX_VALUE);
	}
	
	private void doExchange(TradeList partnerList)
	{
		boolean success = false;
		
		if ((!getOwner().getInventory().validateWeight(partnerList.calcItemsWeight())) || !(partnerList.getOwner().getInventory().validateWeight(calcItemsWeight())))
		{
			partnerList.getOwner().sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			getOwner().sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
		}
		else if ((!getOwner().getInventory().validateCapacity(partnerList.countItemsSlots(getOwner()))) || (!partnerList.getOwner().getInventory().validateCapacity(countItemsSlots(partnerList.getOwner()))))
		{
			partnerList.getOwner().sendPacket(SystemMessageId.SLOTS_FULL);
			getOwner().sendPacket(SystemMessageId.SLOTS_FULL);
		}
		else
		{
			final InventoryUpdate ownerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
			final InventoryUpdate partnerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
			
			partnerList.TransferItems(getOwner(), partnerIU, ownerIU);
			TransferItems(partnerList.getOwner(), ownerIU, partnerIU);
			
			if (ownerIU != null)
			{
				_owner.sendPacket(ownerIU);
			}
			else
			{
				_owner.sendItemList(false);
			}
			
			if (partnerIU != null)
			{
				_partner.sendPacket(partnerIU);
			}
			else
			{
				_partner.sendItemList(false);
			}
			_owner.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
			_partner.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
			success = true;
		}
		partnerList.getOwner().onTradeFinish(success);
		getOwner().onTradeFinish(success);
	}
	
	public synchronized int privateStoreBuy(Player player, Set<ItemRequest> items)
	{
		if (_locked)
		{
			return 1;
		}
		
		if (!validate())
		{
			lock();
			return 1;
		}
		
		if (!_owner.isOnline() || !player.isOnline())
		{
			return 1;
		}
		
		int slots = 0;
		int weight = 0;
		long totalPrice = 0;
		
		final PcInventory ownerInventory = _owner.getInventory();
		final PcInventory playerInventory = player.getInventory();
		
		for (final ItemRequest item : items)
		{
			boolean found = false;
			
			for (final TradeItem ti : _items)
			{
				if (ti.getObjectId() == item.getObjectId())
				{
					if (ti.getPrice() == item.getPrice())
					{
						if (ti.getCount() < item.getCount())
						{
							item.setCount(ti.getCount());
						}
						found = true;
					}
					break;
				}
			}
			
			if (!found)
			{
				if (isPackaged())
				{
					Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " tried to cheat the package sell and buy only a part of the package!");
					return 2;
				}
				
				item.setCount(0);
				continue;
			}
			
			if ((MAX_ADENA / item.getCount()) < item.getPrice())
			{
				lock();
				return 1;
			}
			
			totalPrice += item.getCount() * item.getPrice();
			if ((MAX_ADENA < totalPrice) || (totalPrice < 0))
			{
				lock();
				return 1;
			}
			
			final ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
			if ((oldItem == null) || !oldItem.isTradeable())
			{
				lock();
				return 2;
			}
			
			final Item template = ItemsParser.getInstance().getTemplate(item.getId());
			if (template == null)
			{
				continue;
			}
			weight += item.getCount() * template.getWeight();
			if (!template.isStackable())
			{
				slots += item.getCount();
			}
			else if (playerInventory.getItemByItemId(item.getId()) == null)
			{
				slots++;
			}
		}
		
		if (totalPrice > playerInventory.getAdena())
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return 1;
		}
		
		if (!playerInventory.validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return 1;
		}
		
		if (!playerInventory.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return 1;
		}
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		
		final ItemInstance adenaItem = playerInventory.getAdenaInstance();
		if (!playerInventory.reduceAdena("PrivateStore", totalPrice, player, _owner))
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return 1;
		}
		playerIU.addItem(adenaItem);
		ownerInventory.addAdena("PrivateStore", totalPrice, _owner, player);
		
		boolean ok = true;
		
		for (final ItemRequest item : items)
		{
			if (item.getCount() == 0)
			{
				continue;
			}
			
			final ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
			if (oldItem == null)
			{
				lock();
				ok = false;
				break;
			}
			
			final ItemInstance newItem = ownerInventory.transferItem("PrivateStore", item.getObjectId(), item.getCount(), playerInventory, _owner, player);
			if (newItem == null)
			{
				ok = false;
				break;
			}
			removeItem(item.getObjectId(), -1, item.getCount());
			
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				ownerIU.addModifiedItem(oldItem);
			}
			else
			{
				ownerIU.addRemovedItem(oldItem);
			}
			if (newItem.getCount() > item.getCount())
			{
				playerIU.addModifiedItem(newItem);
			}
			else
			{
				playerIU.addNewItem(newItem);
			}
			
			if (newItem.isStackable())
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_PURCHASED_S3_S2_S);
				msg.addString(player.getName(null));
				msg.addItemName(newItem);
				msg.addItemNumber(item.getCount());
				_owner.sendPacket(msg);
				
				msg = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_C1);
				msg.addString(_owner.getName(null));
				msg.addItemName(newItem);
				msg.addItemNumber(item.getCount());
				player.sendPacket(msg);
			}
			else
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_PURCHASED_S2);
				msg.addString(player.getName(null));
				msg.addItemName(newItem);
				_owner.sendPacket(msg);
				
				msg = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S2_FROM_C1);
				msg.addString(_owner.getName(null));
				msg.addItemName(newItem);
				player.sendPacket(msg);
			}
		}
		_owner.sendPacket(ownerIU);
		_owner.saveTradeList();
		player.sendPacket(playerIU);
		if (ok)
		{
			return 0;
		}
		return 2;
	}
	
	public synchronized boolean privateStoreSell(Player player, ItemRequest[] items)
	{
		if (_locked)
		{
			return false;
		}
		
		if (!_owner.isOnline() || !player.isOnline())
		{
			return false;
		}
		
		boolean ok = false;
		
		final PcInventory ownerInventory = _owner.getInventory();
		final PcInventory playerInventory = player.getInventory();
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		
		long totalPrice = 0;
		
		for (final ItemRequest item : items)
		{
			boolean found = false;
			
			for (final TradeItem ti : _items)
			{
				if (ti.getItem().getId() == item.getId())
				{
					if (ti.getPrice() == item.getPrice())
					{
						if (ti.getCount() < item.getCount())
						{
							item.setCount(ti.getCount());
						}
						found = item.getCount() > 0;
					}
					break;
				}
			}
			
			if (!found)
			{
				continue;
			}
			
			if ((MAX_ADENA / item.getCount()) < item.getPrice())
			{
				lock();
				break;
			}
			
			final long _totalPrice = totalPrice + (item.getCount() * item.getPrice());
			
			if ((MAX_ADENA < _totalPrice) || (_totalPrice < 0))
			{
				lock();
				break;
			}
			
			if (ownerInventory.getAdena() < _totalPrice)
			{
				continue;
			}
			
			int objectId = item.getObjectId();
			ItemInstance oldItem = player.checkItemManipulation(objectId, item.getCount(), "sell");
			
			if (oldItem == null)
			{
				oldItem = playerInventory.getItemByItemId(item.getId());
				if (oldItem == null)
				{
					continue;
				}
				objectId = oldItem.getObjectId();
				oldItem = player.checkItemManipulation(objectId, item.getCount(), "sell");
				if (oldItem == null)
				{
					continue;
				}
			}
			if (oldItem.getId() != item.getId())
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " is cheating with sell items");
				return false;
			}
			
			if (!oldItem.isTradeable())
			{
				continue;
			}
			
			final ItemInstance newItem = playerInventory.transferItem("PrivateStore", objectId, item.getCount(), ownerInventory, player, _owner);
			if (newItem == null)
			{
				continue;
			}
			
			removeItem(-1, item.getId(), item.getCount());
			ok = true;
			
			totalPrice = _totalPrice;
			
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
			if (newItem.getCount() > item.getCount())
			{
				ownerIU.addModifiedItem(newItem);
			}
			else
			{
				ownerIU.addNewItem(newItem);
			}
			
			if (newItem.isStackable())
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_C1);
				msg.addString(player.getName(null));
				msg.addItemName(newItem);
				msg.addItemNumber(item.getCount());
				_owner.sendPacket(msg);
				
				msg = SystemMessage.getSystemMessage(SystemMessageId.C1_PURCHASED_S3_S2_S);
				msg.addString(_owner.getName(null));
				msg.addItemName(newItem);
				msg.addItemNumber(item.getCount());
				player.sendPacket(msg);
			}
			else
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.PURCHASED_S2_FROM_C1);
				msg.addString(player.getName(null));
				msg.addItemName(newItem);
				_owner.sendPacket(msg);
				
				msg = SystemMessage.getSystemMessage(SystemMessageId.C1_PURCHASED_S2);
				msg.addString(_owner.getName(null));
				msg.addItemName(newItem);
				player.sendPacket(msg);
			}
		}
		
		if (totalPrice > 0)
		{
			if (totalPrice > ownerInventory.getAdena())
			{
				return false;
			}
			final ItemInstance adenaItem = ownerInventory.getAdenaInstance();
			ownerInventory.reduceAdena("PrivateStore", totalPrice, _owner, player);
			ownerIU.addItem(adenaItem);
			playerInventory.addAdena("PrivateStore", totalPrice, player, _owner);
			playerIU.addItem(playerInventory.getAdenaInstance());
		}
		
		if (ok)
		{
			_owner.sendPacket(ownerIU);
			_owner.saveTradeList();
			player.sendPacket(playerIU);
		}
		return ok;
	}
}