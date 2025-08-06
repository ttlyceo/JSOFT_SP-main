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
package l2e.gameserver.model.items.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.DelayedItemsManager;
import l2e.gameserver.model.TradeItem;
import l2e.gameserver.model.TradeList;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.StatusUpdate;

public class PcInventory extends Inventory
{
	public static final int ADENA_ID = 57;
	public static final int ANCIENT_ADENA_ID = 5575;
	public static final long MAX_ADENA = Config.MAX_ADENA;
	
	private final Player _owner;
	private ItemInstance _adena;
	private ItemInstance _ancientAdena;
	private boolean _mustShowDressMe = false;
	
	private Collection<Integer> _blockItems = null;
	
	private int _questSlots;

	private final Object _lock;
	private int _blockMode = -1;
	
	public PcInventory(Player owner)
	{
		_owner = owner;
		_lock = new Object();
	}
	
	@Override
	public Player getOwner()
	{
		return _owner;
	}
	
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
	
	public ItemInstance getAdenaInstance()
	{
		return _adena;
	}
	
	@Override
	public long getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}
	
	public ItemInstance getAncientAdenaInstance()
	{
		return _ancientAdena;
	}
	
	public long getAncientAdena()
	{
		return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
	}
	
	public ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItems(allowAdena, allowAncientAdena, true);
	}
	
	public ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		final List<ItemInstance> list = new LinkedList<>();
		for (final ItemInstance item : _items)
		{
			if (item == null || item.isTimeLimitedItem())
			{
				continue;
			}
			
			if ((!allowAdena && (item.getId() == ADENA_ID)))
			{
				continue;
			}
			if ((!allowAncientAdena && (item.getId() == ANCIENT_ADENA_ID)))
			{
				continue;
			}
			
			boolean isDuplicate = false;
			for (final ItemInstance litem : list)
			{
				if (litem.getId() == item.getId())
				{
					isDuplicate = true;
					break;
				}
			}
			if (!isDuplicate && (!onlyAvailable || ((item != null) && item.isSellable() && item.isAvailable(getOwner(), false, false))))
			{
				list.add(item);
			}
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	public ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItemsByEnchantLevel(allowAdena, allowAncientAdena, true);
	}
	
	public ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		final List<ItemInstance> list = new LinkedList<>();
		for (final ItemInstance item : _items)
		{
			if (item == null || item.isTimeLimitedItem())
			{
				continue;
			}
			if ((!allowAdena && (item.getId() == ADENA_ID)))
			{
				continue;
			}
			if ((!allowAncientAdena && (item.getId() == ANCIENT_ADENA_ID)))
			{
				continue;
			}
			
			boolean isDuplicate = false;
			for (final ItemInstance litem : list)
			{
				if ((litem.getId() == item.getId()) && (litem.getEnchantLevel() == item.getEnchantLevel()))
				{
					isDuplicate = true;
					break;
				}
			}
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(getOwner(), false, false))))
			{
				list.add(item);
			}
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	public ItemInstance[] getAllItemsByItemId(int itemId)
	{
		return getAllItemsByItemId(itemId, true);
	}
	
	public ItemInstance[] getAllItemsByItemId(int itemId, boolean includeEquipped)
	{
		final List<ItemInstance> list = new LinkedList<>();
		for (final ItemInstance item : _items)
		{
			if (item == null)
			{
				continue;
			}
			
			if ((item.getId() == itemId) && (includeEquipped || !item.isEquipped()))
			{
				list.add(item);
			}
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	public ItemInstance[] getAllItemsByItemId(int itemId, int enchantment)
	{
		return getAllItemsByItemId(itemId, enchantment, true);
	}
	
	public ItemInstance[] getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped)
	{
		final List<ItemInstance> list = new LinkedList<>();
		for (final ItemInstance item : _items)
		{
			if (item == null)
			{
				continue;
			}
			
			if ((item.getId() == itemId) && (item.getEnchantLevel() == enchantment) && (includeEquipped || !item.isEquipped()))
			{
				list.add(item);
			}
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	public ItemInstance[] getAvailableItems(boolean allowAdena, boolean allowNonTradeable, boolean feightable)
	{
		final List<ItemInstance> list = new LinkedList<>();
		for (final ItemInstance item : _items)
		{
			if ((item == null) || !item.isAvailable(getOwner(), allowAdena, allowNonTradeable) || !canManipulateWithItemId(item.getId()))
			{
				continue;
			}
			else if (feightable)
			{
				if ((item.getItemLocation() == ItemLocation.INVENTORY) && item.isFreightable())
				{
					list.add(item);
				}
			}
			else
			{
				list.add(item);
			}
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	public ItemInstance[] getAugmentedItems()
	{
		final List<ItemInstance> list = new LinkedList<>();
		for (final ItemInstance item : _items)
		{
			if ((item != null) && item.isAugmented())
			{
				list.add(item);
			}
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	public ItemInstance[] getElementItems()
	{
		final List<ItemInstance> list = new LinkedList<>();
		for (final ItemInstance item : _items)
		{
			if ((item != null) && (item.getElementals() != null))
			{
				list.add(item);
			}
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	public TradeItem[] getAvailableItems(TradeList tradeList)
	{
		final List<TradeItem> list = new LinkedList<>();
		for (final ItemInstance item : _items)
		{
			if ((item != null) && item.isAvailable(getOwner(), false, false))
			{
				final TradeItem adjItem = tradeList.adjustAvailableItem(item);
				if (adjItem != null)
				{
					boolean found = false;
					if (!adjItem.getItem().isStackable())
					{
						for (final TradeItem temp : getOwner().getSellList().getItems())
						{
							if (temp.getObjectId() == adjItem.getObjectId())
							{
								found = true;
								break;
							}
						}
					}
					
					if (found)
					{
						continue;
					}
					
					list.add(adjItem);
				}
			}
		}
		return list.toArray(new TradeItem[list.size()]);
	}
	
	public void adjustAvailableItem(TradeItem item)
	{
		boolean notAllEquipped = false;
		for (final ItemInstance adjItem : getItemsByItemId(item.getItem().getId()))
		{
			if (adjItem.isEquipable())
			{
				if (!adjItem.isEquipped() && (adjItem.getEnchantLevel() == item.getEnchant()) && (adjItem.getAttackElementType() == item.getAttackElementType()) && (adjItem.getAttackElementPower() == item.getAttackElementPower()))
				{
					boolean checkAtt = true;
					for (byte i = 0; i < 6; i++)
					{
						if (adjItem.getElementDefAttr(i) != item.getElementDefAttr(i))
						{
							checkAtt = false;
							break;
						}
					}
					
					if (checkAtt)
					{
						notAllEquipped |= true;
					}
				}
			}
			else
			{
				notAllEquipped |= true;
				break;
			}
		}
		if (notAllEquipped)
		{
			final ItemInstance adjItem = getItemByItemId(item.getItem().getId());
			item.setObjectId(adjItem.getObjectId());
			item.setEnchant(adjItem.getEnchantLevel());
			item.setAttackElementType(adjItem.getAttackElementType());
			item.setAttackElementPower(adjItem.getAttackElementPower());
			for (byte i = 0; i < 6; i++)
			{
				item.setElemDefAttr(i, adjItem.getElementDefAttr(i));
			}
			
			if (adjItem.getCount() < item.getCount())
			{
				item.setCount(adjItem.getCount());
			}
			
			return;
		}
		
		item.setCount(0);
	}
	
	public void addAdena(String process, long count, Player actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, ADENA_ID, count, actor, reference);
		}
	}
	
	public boolean reduceAdena(String process, long count, Player actor, Object reference)
	{
		if (count > 0)
		{
			return destroyItemByItemId(process, ADENA_ID, count, actor, reference) != null;
		}
		return false;
	}
	
	public void addAncientAdena(String process, long count, Player actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
		}
	}
	
	public boolean reduceAncientAdena(String process, long count, Player actor, Object reference)
	{
		if (count > 0)
		{
			return destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference) != null;
		}
		return false;
	}
	
	@Override
	public ItemInstance addItem(String process, ItemInstance item, Player actor, Object reference)
	{
		final ItemInstance addedItem = super.addItem(process, item, actor, reference);
		if (addedItem != null)
		{
			if ((addedItem.getId() == ADENA_ID) && !addedItem.equals(_adena))
			{
				_adena = addedItem;
			}
			else if ((addedItem.getId() == ANCIENT_ADENA_ID) && !addedItem.equals(_ancientAdena))
			{
				_ancientAdena = addedItem;
			}
			
			if (actor != null)
			{
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					final InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(addedItem);
					actor.sendInventoryUpdate(playerIU);
				}
				else
				{
					actor.sendItemList(false);
				}
				
				if (item.getId() != ADENA_ID)
				{
					actor.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
				}
			}
		}
		return item;
	}
	
	@Override
	public ItemInstance addItem(String process, int itemId, long count, Player actor, Object reference)
	{
		final ItemInstance item = super.addItem(process, itemId, count, actor, reference);
		if (item != null)
		{
			if ((item.getId() == ADENA_ID) && !item.equals(_adena))
			{
				_adena = item;
			}
			else if ((item.getId() == ANCIENT_ADENA_ID) && !item.equals(_ancientAdena))
			{
				_ancientAdena = item;
			}

			if (actor != null)
			{
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					final InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(item);
					actor.sendInventoryUpdate(playerIU);
				}
				else
				{
					actor.sendItemList(false);
				}
				
				if (item.getId() != ADENA_ID)
				{
					actor.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
				}
			}
		}
		return item;
	}
	
	@Override
	public ItemInstance transferItem(String process, int objectId, long count, ItemContainer target, Player actor, Object reference)
	{
		final ItemInstance item = super.transferItem(process, objectId, count, target, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		return item;
	}
	
	@Override
	public ItemInstance destroyItem(String process, ItemInstance item, long count, Player actor, Object reference)
	{
		final var destroyedItem = super.destroyItem(process, item, count, actor, reference);
		
		if ((_adena != null) && (_adena.getCount() <= 0))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && (_ancientAdena.getCount() <= 0))
		{
			_ancientAdena = null;
		}
		
		if (actor != null)
		{
			if (item.getId() != ADENA_ID)
			{
				actor.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
			}
		}
		return destroyedItem;
	}
	
	@Override
	public ItemInstance destroyItem(String process, int objectId, long count, Player actor, Object reference)
	{
		final var item = getItemByObjectId(objectId);
		return item == null ? null : destroyItem(process, item, count, actor, reference);
	}
	
	@Override
	public ItemInstance destroyItemByItemId(String process, int itemId, long count, Player actor, Object reference)
	{
		ItemInstance destroyItem = null;
		for (final var item : getAllItemsByItemId(itemId))
		{
			destroyItem = item;
			if (!destroyItem.isEquipped())
			{
				break;
			}
		}
		return destroyItem == null ? null : destroyItem(process, destroyItem, count, actor, reference);
	}
	
	@Override
	public ItemInstance dropItem(String process, ItemInstance item, Player actor, Object reference)
	{
		final var droppedItem = super.dropItem(process, item, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		return droppedItem;
	}
	
	@Override
	public ItemInstance dropItem(String process, int objectId, long count, Player actor, Object reference)
	{
		final ItemInstance item = super.dropItem(process, objectId, count, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		return item;
	}
	
	@Override
	public boolean removeItem(ItemInstance item)
	{
		getOwner().removeItemFromShortCut(item.getObjectId());
		
		if (item.getObjectId() == getOwner().getActiveEnchantItemId())
		{
			getOwner().setActiveEnchantItemId(Player.ID_NONE);
		}
		
		if (item.getId() == ADENA_ID)
		{
			_adena = null;
		}
		else if (item.getId() == ANCIENT_ADENA_ID)
		{
			_ancientAdena = null;
		}
		
		if (item.isQuestItem())
		{
			synchronized (_lock)
			{
				_questSlots--;
				if (_questSlots < 0)
				{
					_questSlots = 0;
				}
			}
		}
		return super.removeItem(item);
	}
	
	@Override
	public void refreshWeight()
	{
		super.refreshWeight();
		getOwner().refreshOverloaded();
	}
	
	@Override
	public void restore()
	{
		super.restore();
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
		DelayedItemsManager.getInstance().loadDelayed(getOwner(), false);
	}
	
	public static int[][] restoreVisibleInventory(int objectId)
	{
		final int[][] paperdoll = new int[31][3];
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'");
			statement.setInt(1, objectId);
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int slot = rset.getInt("loc_data");
				paperdoll[slot][0] = rset.getInt("object_id");
				paperdoll[slot][1] = rset.getInt("item_id");
				paperdoll[slot][2] = rset.getInt("enchant_level");
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore inventory: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return paperdoll;
	}
	
	public boolean checkInventorySlotsAndWeight(List<Item> itemList, boolean sendMessage, boolean sendSkillMessage)
	{
		int lootWeight = 0;
		int requiredSlots = 0;
		if (itemList != null)
		{
			for (final Item item : itemList)
			{
				if (!item.isStackable() || (getInventoryItemCount(item.getId(), -1) <= 0))
				{
					requiredSlots++;
				}
				lootWeight += item.getWeight();
			}
		}
		
		final boolean inventoryStatusOK = validateCapacity(requiredSlots) && validateWeight(lootWeight);
		if (!inventoryStatusOK && sendMessage)
		{
			_owner.sendPacket(SystemMessageId.SLOTS_FULL);
			if (sendSkillMessage)
			{
				_owner.sendPacket(SystemMessageId.WEIGHT_EXCEEDED_SKILL_UNAVAILABLE);
			}
		}
		return inventoryStatusOK;
	}
	
	public boolean validateCapacity(ItemInstance item)
	{
		if ((item.isStackable() && (getItemByItemId(item.getId()) != null)) || item.getItem().isHerb())
		{
			return true;
		}
		return validateCapacity(1, item.isQuestItem());
	}
	
	public boolean validateCapacityByItemId(int itemId)
	{
		int slots = 0;
		final ItemInstance invItem = getItemByItemId(itemId);
		if ((invItem == null) || !invItem.isStackable())
		{
			slots++;
		}
		return validateCapacity(slots, ItemsParser.getInstance().getTemplate(itemId).isQuestItem());
	}
	
	@Override
	public boolean validateCapacity(long slots)
	{
		return validateCapacity(slots, false);
	}
	
	public boolean validateCapacity(long slots, boolean questItem)
	{
		if (!questItem)
		{
			return (((_items.size() - _questSlots) + slots) <= _owner.getInventoryLimit());
		}
		return (_questSlots + slots) <= _owner.getQuestInventoryLimit();
	}
	
	@Override
	public boolean validateWeight(long weight)
	{
		if (_owner.isGM() && _owner.getDietMode() && _owner.getAccessLevel().allowTransaction())
		{
			return true;
		}
		return ((_totalWeight + weight) <= _owner.getMaxLoad());
	}
	
	public void setInventoryBlock(Collection<Integer> items, int mode)
	{
		_blockMode = mode;
		_blockItems = items;
		_owner.sendItemList(false);
	}
	
	public void unblock()
	{
		_blockMode = -1;
		_blockItems = null;
		_owner.sendItemList(false);
	}
	
	public boolean hasInventoryBlock()
	{
		return ((_blockMode > -1) && (_blockItems != null) && !_blockItems.isEmpty());
	}
	
	public void blockAllItems()
	{
		setInventoryBlock(Arrays.asList(ADENA_ID), 1);
	}
	
	public int getBlockMode()
	{
		return _blockMode;
	}
	
	public Collection<Integer> getBlockItems()
	{
		return _blockItems;
	}
	
	public boolean canManipulateWithItemId(int itemId)
	{
		final Collection<Integer> blockedItems = _blockItems;
		if (blockedItems != null)
		{
			switch (_blockMode)
			{
				case -1 :
				{
					return true;
				}
				case 1 :
				{
					return blockedItems.stream().anyMatch(id -> id == itemId);
				}
				case 0 :
				{
					return blockedItems.stream().noneMatch(id -> id == itemId);
				}
			}
		}
		return true;
	}
	
	@Override
	public void addItem(ItemInstance item)
	{
		if (item.isQuestItem())
		{
			synchronized (_lock)
			{
				_questSlots++;
			}
		}
		super.addItem(item);
	}
	
	public int getSize(boolean quest)
	{
		if (quest)
		{
			return _questSlots;
		}
		return getSize() - _questSlots;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _owner + "]";
	}
	
	public void applyItemSkills()
	{
		_items.stream().filter(i -> i != null).forEach(it -> it.giveSkillsToOwner());
	}
	
	public void checkRuneSkills()
	{
		for (final ItemInstance item : _items)
		{
			if (item != null && item.hasPassiveSkills())
			{
				item.giveSkillsToOwner();
			}
		}
	}
	
	@Override
	public int getPaperdollVisualItemId(int slot)
	{
		ItemInstance item = getPaperdollItem(slot);
		if (item != null)
		{
			switch (slot)
			{
				case PAPERDOLL_CHEST :
				case PAPERDOLL_LEGS :
				case PAPERDOLL_GLOVES :
				case PAPERDOLL_FEET :
				{
					if (mustShowDressMe())
					{
						final int visualItemId = item.getVisualItemId();
						
						if (visualItemId == -1)
						{
							return 0;
						}
						if (visualItemId != 0)
						{
							return visualItemId;
						}
					}
					break;
				}
				default :
				{
					final int visualItemId = item.getVisualItemId();
					
					if (visualItemId == -1)
					{
						return 0;
					}
					if (visualItemId != 0)
					{
						return visualItemId;
					}
					break;
				}
			}
			return item.getId();
		}
		else if (slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_HAIR2];
			if (item != null)
			{
				return item.getId();
			}
		}
		return 0;
	}
	
	public void setMustShowDressMe(boolean val)
	{
		_mustShowDressMe = val;
	}
	
	public boolean mustShowDressMe()
	{
		return _mustShowDressMe;
	}
	
	public boolean hasAllDressMeItemsEquipped()
	{
		final ItemInstance chestItem = getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		final ItemInstance legsItem = getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final ItemInstance glovesItem = getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final ItemInstance feetItem = getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		if (chestItem == null || glovesItem == null || feetItem == null)
		{
			return false;
		}
		
		if (legsItem == null && chestItem.getItem().getBodyPart() != Item.SLOT_FULL_ARMOR)
		{
			return false;
		}
		return true;
	}
	
	public ItemInstance addItem(ItemInstance item, String log)
	{
		return addItem(item, _owner.toString(), log);
	}
	
	@Override
	public boolean destroyItemByItemId(int itemId, long count, String log)
	{
		return destroyItemByItemId(itemId, count, _owner, log);
	}
	
	@Override
	public void reduceAmmunitionCount(EtcItemType type)
	{
		if ((type != EtcItemType.ARROW) && (type != EtcItemType.BOLT))
		{
			_log.warn(type.toString(), " which is not ammo type.");
			return;
		}
		
		final var weapon = _owner.getActiveWeaponItem();
		if (weapon == null)
		{
			return;
		}
		
		ItemInstance ammunition = null;
		switch (weapon.getItemType())
		{
			case BOW :
			{
				ammunition = findArrowForBow(weapon);
				break;
			}
			case CROSSBOW :
			{
				ammunition = findBoltForCrossBow(weapon);
				break;
			}
			default :
			{
				return;
			}
		}
		
		if ((ammunition == null) || (ammunition.getItemType() != type))
		{
			return;
		}
		updateItemCountNoDbUpdate(null, ammunition, -1, _owner, null);
	}
	
	@Override
	public boolean reduceShortsCount(ItemInstance item, int count)
	{
		if (item.getCount() < count)
		{
			return false;
		}
		return updateItemCountNoDbUpdate(null, item, -count, _owner, null);
	}
	
	public boolean updateItemCountNoDbUpdate(String process, ItemInstance item, long countDelta, Player creator, Object reference)
	{
		final InventoryUpdate iu = new InventoryUpdate();
		final long left = item.getCount() + countDelta;
		try
		{
			if (left > 0)
			{
				synchronized (item)
				{
					if ((process != null) && (process.length() > 0))
					{
						item.changeCount(process, countDelta, creator, reference);
					}
					else
					{
						item.changeCountWithoutTrace(-1, creator, reference);
					}
					item.setLastChange(ItemInstance.MODIFIED);
					refreshWeight();
					iu.addModifiedItem(item);
					return true;
				}
			}
			else if (left == 0)
			{
				iu.addRemovedItem(item);
				destroyItem(process, item, _owner, null);
				return true;
			}
			else
			{
				return false;
			}
		}
		finally
		{
			if (Config.FORCE_INVENTORY_UPDATE)
			{
				_owner.sendItemList(false);
			}
			else
			{
				_owner.sendInventoryUpdate(iu);
			}
		}
	}
	
	public boolean updateItemCount(String process, ItemInstance item, long countDelta, Player creator, Object reference)
	{
		if (item != null)
		{
			try
			{
				return updateItemCountNoDbUpdate(process, item, countDelta, creator, reference);
			}
			finally
			{
				if (item.getCount() > 0)
				{
					item.updateDatabase();
				}
			}
		}
		return false;
	}
}