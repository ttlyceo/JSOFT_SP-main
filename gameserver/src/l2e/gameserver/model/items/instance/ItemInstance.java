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
package l2e.gameserver.model.items.instance;

import static l2e.gameserver.model.items.itemcontainer.PcInventory.ADENA_ID;
import static l2e.gameserver.model.items.itemcontainer.PcInventory.MAX_ADENA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.Log;
import l2e.commons.util.GMAudit;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.AugmentationParser;
import l2e.gameserver.data.parser.EnchantItemOptionsParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.OptionsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.MercTicketManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.DropProtection;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.actor.templates.items.EtcItem;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.model.items.type.ItemType;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.funcs.Func;
import l2e.gameserver.model.skills.options.EnchantOptions;
import l2e.gameserver.model.skills.options.Options;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.DropItem;
import l2e.gameserver.network.serverpackets.GetItem;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SpawnItem;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class ItemInstance extends GameObject
{
	public static enum ItemLocation
	{
		VOID, INVENTORY, PAPERDOLL, WAREHOUSE, CLANWH, PET, PET_EQUIP, LEASE, REFUND, MAIL, FREIGHT, AUCTION, PRIVATEINV
	}

	private int _ownerId;

	private int _dropperObjectId = 0;

	private long _count;
	private long _initCount;
	private long _time;
	private boolean _decrease = false;
	private int _itemId;
	private int _visualItemId = 0;
	private Item _item;
	private ItemLocation _loc;
	private int _locData;
	private int _enchantLevel;
	private boolean _wear;
	private boolean _isEventItem = false;
	
	private Augmentation _augmentation = null;
	
	private int _agathionEnergy = -1;
	private static final int ENERGY_CONSUMPTION_RATE = 60000;
	
	private int _mana = -1;
	private static final int MANA_CONSUMPTION_RATE = 60000;

	private int _type1;
	private int _type2;

	private long _dropTime;

	private boolean _published = false;

	private boolean _protected;

	public static final int UNCHANGED = 0;
	public static final int ADDED = 1;
	public static final int REMOVED = 3;
	public static final int MODIFIED = 2;

	public static final int[] DEFAULT_ENCHANT_OPTIONS = new int[]
	{
	        0, 0, 0
	};

	private int _lastChange = 2;
	private boolean _existsInDb;
	private boolean _storedInDb;

	private final ReentrantLock _dbLock = new ReentrantLock();

	private Elementals[] _elementals = null;

	private Future<?> _consumingMana = null;
	private Future<?> _consumingEnergy = null;
	private Future<?> _lifeTimeTask = null;

	private final DropProtection _dropProtection = new DropProtection();

	private int _shotsMask = 0;

	private final List<Options> _enchantOptions = new ArrayList<>();

	public ItemInstance(int objectId, int itemId)
	{
		super(objectId);
		setInstanceType(InstanceType.ItemInstance);
		_itemId = itemId;
		_item = ItemsParser.getInstance().getTemplate(itemId);
		if ((_itemId == 0) || (_item == null))
		{
			throw new IllegalArgumentException();
		}
		
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			if (lang != null)
			{
				super.setName(lang, _item.getName(lang) != null ? _item.getName(lang) : _item.getName(null));
			}
		}
		setCount(1);
		if (_item.getDefaultEnchantLevel() > 0)
		{
			_enchantLevel = _item.getDefaultEnchantLevel();
		}
		_loc = ItemLocation.VOID;
		_type1 = 0;
		_type2 = 0;
		_dropTime = 0;
		_mana = _item.getDuration();
		_agathionEnergy = _item.getAgathionMaxEnergy();
		_time = _item.getTime() == -1 ? -1 : System.currentTimeMillis() + ((long) _item.getTime() * 60 * 1000);
		scheduleLifeTimeTask();
	}

	public ItemInstance(int objectId, Item item)
	{
		super(objectId);
		setInstanceType(InstanceType.ItemInstance);
		_itemId = item.getId();
		_item = item;
		if (_itemId == 0)
		{
			throw new IllegalArgumentException();
		}
		
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			if (lang != null)
			{
				super.setName(lang, _item.getName(lang) != null ? _item.getName(lang) : _item.getName("en"));
			}
		}
		setCount(1);
		if (_item.getDefaultEnchantLevel() > 0)
		{
			_enchantLevel = _item.getDefaultEnchantLevel();
		}
		_loc = ItemLocation.VOID;
		_mana = _item.getDuration();
		_agathionEnergy = _item.getAgathionMaxEnergy();
		_time = _item.getTime() == -1 ? -1 : System.currentTimeMillis() + ((long) _item.getTime() * 60 * 1000);
		scheduleLifeTimeTask();
	}

	public ItemInstance(int itemId)
	{
		this(IdFactory.getInstance().getNextId(), itemId);
	}

	public final void pickupMe(Creature player)
	{
		player.broadcastPacket(new GetItem(this, player.getObjectId()));

		final int itemId = getId();

		if (MercTicketManager.getInstance().getTicketCastleId(itemId) > 0)
		{
			MercTicketManager.getInstance().removeTicket(this);
		}
		
		if ((getReflectionId() > 0) && (ReflectionManager.getInstance().getReflection(getReflectionId()) != null))
		{
			ReflectionManager.getInstance().getReflection(getReflectionId()).removeItem(this);
		}

		if (!Config.DISABLE_TUTORIAL && ((itemId == PcInventory.ADENA_ID) || (itemId == 6353)))
		{
			final Player actor = player.getActingPlayer();
			if (actor != null)
			{
				final QuestState qs = actor.getQuestState("_255_Tutorial");
				if ((qs != null) && (qs.getQuest() != null))
				{
					qs.getQuest().notifyEvent("CE" + itemId, null, actor);
				}
			}
		}
		decayItem();
	}

	public void setOwnerId(String process, int owner_id, Player creator, Object reference)
	{
		setOwnerId(owner_id);

		if (Config.LOG_ITEMS)
		{
			if (!Config.LOG_ITEMS_SMALL_LOG || (Config.LOG_ITEMS_SMALL_LOG && (getItem().isEquipable() || (getItem().getId() == ADENA_ID))))
			{
				Log.addLogItem(creator, "SET OWNER", this);
			}
		}

		if (creator != null)
		{
			if (creator.isGM())
			{
				String referenceName = "no-reference";
				if (reference instanceof GameObject)
				{
					referenceName = (((GameObject) reference).getName(null) != null ? ((GameObject) reference).getName(null) : "no-name");
				}
				else if (reference instanceof String)
				{
					referenceName = (String) reference;
				}
				final String targetName = (creator.getTarget() != null ? creator.getTarget().getName(null) : "no-target");
				if (Config.GMAUDIT)
				{
					GMAudit.auditGMAction(creator.getName(null) + " [" + creator.getObjectId() + "]", process + "(id: " + getId() + " name: " + getName(null) + ")", targetName, "GameObject referencing this action is: " + referenceName);
				}
			}
		}
	}

	public void setOwnerId(int owner_id)
	{
		if (owner_id == _ownerId)
		{
			return;
		}

		removeSkillsFromOwner();

		_ownerId = owner_id;
		_storedInDb = false;

		giveSkillsToOwner();
	}

	public int getOwnerId()
	{
		return _ownerId;
	}

	public void setItemLocation(ItemLocation loc)
	{
		setItemLocation(loc, 0);
	}

	public void setItemLocation(ItemLocation loc, int loc_data)
	{
		if ((loc == _loc) && (loc_data == _locData))
		{
			return;
		}

		removeSkillsFromOwner();

		_loc = loc;
		_locData = loc_data;
		_storedInDb = false;

		giveSkillsToOwner();
	}

	public ItemLocation getItemLocation()
	{
		return _loc;
	}

	public void setCount(long count)
	{
		if (getCount() == count)
		{
			return;
		}

		_count = count >= -1 ? count : 0;
		_storedInDb = false;
	}

	public long getCount()
	{
		return _count;
	}

	public void changeCount(String process, long count, Player creator, Object reference)
	{
		if (count == 0)
		{
			return;
		}
		final long old = getCount();
		final long max = getId() == ADENA_ID ? MAX_ADENA : Integer.MAX_VALUE;

		if ((count > 0) && (getCount() > (max - count)))
		{
			setCount(max);
		}
		else
		{
			setCount(getCount() + count);
		}

		if (getCount() < 0)
		{
			setCount(0);
		}

		_storedInDb = false;

		if (Config.LOG_ITEMS && (process != null))
		{
			if (!Config.LOG_ITEMS_SMALL_LOG || (Config.LOG_ITEMS_SMALL_LOG && (_item.isEquipable() || (_item.getId() == ADENA_ID))))
			{
				Log.addLogItem(creator, "CHARGE", this, getCount(), "prevAmount(" + old + ")");
			}
		}
		
		if (creator != null)
		{
			if (creator.isGM())
			{
				String referenceName = "no-reference";
				if (reference instanceof GameObject)
				{
					referenceName = (((GameObject) reference).getName(null) != null ? ((GameObject) reference).getName(null) : "no-name");
				}
				else if (reference instanceof String)
				{
					referenceName = (String) reference;
				}
				final String targetName = (creator.getTarget() != null ? creator.getTarget().getName(null) : "no-target");
				if (Config.GMAUDIT)
				{
					GMAudit.auditGMAction(creator.getName(null) + " [" + creator.getObjectId() + "]", process + "(id: " + getId() + " objId: " + getObjectId() + " name: " + getName(null) + " count: " + count + ")", targetName, "GameObject referencing this action is: " + referenceName);
				}
			}
		}
	}

	public void changeCountWithoutTrace(int count, Player creator, Object reference)
	{
		changeCount(null, count, creator, reference);
	}

	public int isEnchantable()
	{
		if ((getItemLocation() == ItemLocation.INVENTORY) || (getItemLocation() == ItemLocation.PAPERDOLL))
		{
			return getItem().isEnchantable();
		}
		return 0;
	}

	public boolean isEquipable()
	{
		return !((_item.getBodyPart() == 0) || (_item.getItemType() == EtcItemType.ARROW) || (_item.getItemType() == EtcItemType.BOLT) || (_item.getItemType() == EtcItemType.LURE));
	}

	public boolean isEquipped()
	{
		return (_loc == ItemLocation.PAPERDOLL) || (_loc == ItemLocation.PET_EQUIP);
	}

	public int getLocationSlot()
	{
		assert (_loc == ItemLocation.PAPERDOLL) || (_loc == ItemLocation.PET_EQUIP) || (_loc == ItemLocation.INVENTORY) || (_loc == ItemLocation.MAIL) || (_loc == ItemLocation.FREIGHT);
		return _locData;
	}

	public Item getItem()
	{
		return _item;
	}

	public int getCustomType1()
	{
		return _type1;
	}

	public int getCustomType2()
	{
		return _type2;
	}

	public void setCustomType1(int newtype)
	{
		_type1 = newtype;
	}

	public void setCustomType2(int newtype)
	{
		_type2 = newtype;
	}

	public void setDropTime(long time)
	{
		_dropTime = time;
	}

	public long getDropTime()
	{
		return _dropTime;
	}

	public ItemType getItemType()
	{
		return _item.getItemType();
	}

	public boolean isWear()
	{
		return _wear;
	}
	
	public void setItemId(int id)
	{
		_itemId = id;
		_item = ItemsParser.getInstance().getTemplate(id);
	}

	@Override
	public int getId()
	{
		return _itemId;
	}

	public int getDisplayId()
	{
		return getItem().getDisplayId();
	}

	public boolean isEtcItem()
	{
		return (_item instanceof EtcItem);
	}

	public boolean isWeapon()
	{
		return (_item instanceof Weapon);
	}

	public boolean isArmor()
	{
		return (_item instanceof Armor);
	}
	
	public boolean isJewel()
	{
		return _item.isJewel();
	}

	public EtcItem getEtcItem()
	{
		if (_item instanceof EtcItem)
		{
			return (EtcItem) _item;
		}
		return null;
	}

	public Weapon getWeaponItem()
	{
		if (_item instanceof Weapon)
		{
			return (Weapon) _item;
		}
		return null;
	}

	public Armor getArmorItem()
	{
		if (_item instanceof Armor)
		{
			return (Armor) _item;
		}
		return null;
	}

	public final int getCrystalCount()
	{
		return _item.getCrystalCount(_enchantLevel);
	}

	public int getReferencePrice()
	{
		return _item.getReferencePrice();
	}

	public int getReuseDelay()
	{
		return _item.getReuseDelay();
	}
	
	public boolean isReuseByCron()
	{
		return _item.isReuseByCron();
	}

	public int getSharedReuseGroup()
	{
		return _item.getSharedReuseGroup();
	}

	public int getLastChange()
	{
		return _lastChange;
	}

	public void setLastChange(int lastChange)
	{
		_lastChange = lastChange;
	}

	public boolean isStackable()
	{
		return _item.isStackable();
	}

	public boolean isDropable()
	{
		return isAugmented() ? AugmentationParser.getInstance().getParams().getBool("allowDropAugmentationItems") ? true : false : isTimeLimitedItem() ? false : _item.isDropable();
	}

	public boolean isDestroyable()
	{
		return _item.isDestroyable();
	}

	public boolean isTradeable()
	{
		return isAugmented() ? AugmentationParser.getInstance().getParams().getBool("allowTradeAugmentationItems") ? true : false : isTimeLimitedItem() ? false : _item.isTradeable();
	}

	public boolean isSellable()
	{
		return isAugmented() ? AugmentationParser.getInstance().getParams().getBool("allowSellAugmentationItems") ? true : false : isTimeLimitedItem() ? false : _item.isSellable();
	}

	public boolean isDepositable(boolean isPrivateWareHouse)
	{
		if (isEquipped() || !_item.isDepositable())
		{
			return false;
		}
		if (!isPrivateWareHouse)
		{
			if (!isTradeable() || isShadowItem())
			{
				return false;
			}
		}

		return true;
	}

	public boolean isConsumable()
	{
		return _item.isConsumable();
	}

	public boolean isPotion()
	{
		return _item.isPotion();
	}

	public boolean isElixir()
	{
		return _item.isElixir();
	}

	public boolean isHeroItem()
	{
		return _item.isHeroItem();
	}

	public boolean isCommonItem()
	{
		return _item.isCommon();
	}

	public boolean isPvp()
	{
		return _item.isPvpItem();
	}

	public boolean isOlyRestrictedItem()
	{
		return getItem().isOlyRestrictedItem();
	}
	
	public boolean isEventRestrictedItem()
	{
		return getItem().isEventRestrictedItem();
	}

	public boolean isAvailable(Player player, boolean allowAdena, boolean allowNonTradeable)
	{
		return ((!isEquipped()) && (getItem().getType2() != Item.TYPE2_QUEST) && ((getItem().getType2() != Item.TYPE2_MONEY) || (getItem().getType1() != Item.TYPE1_SHIELD_ARMOR)) && (!player.hasSummon() || (getObjectId() != player.getSummon().getControlObjectId())) && (player.getActiveEnchantItemId() != getObjectId()) && (player.getActiveEnchantSupportItemId() != getObjectId()) && (player.getActiveEnchantAttrItemId() != getObjectId()) && (allowAdena || (getId() != PcInventory.ADENA_ID)) && ((player.getCurrentSkill() == null) || (player.getCurrentSkill().getSkill().getItemConsumeId() != getId())) && (!player.isCastingSimultaneouslyNow() || (player.getCastingSkill() == null) || (player.getCastingSkill().getItemConsumeId() != getId())) && (allowNonTradeable || (isTradeable() && (!((getItem().getItemType() == EtcItemType.PET_COLLAR) && player.havePetInvItems())))));
	}

	public int getEnchantLevel()
	{
		return _enchantLevel;
	}

	public void setEnchantLevel(int enchantLevel)
	{
		if (_enchantLevel == enchantLevel)
		{
			return;
		}
		clearEnchantStats();
		_enchantLevel = enchantLevel;
		applyEnchantStats();
		_storedInDb = false;
	}

	public boolean isAugmented()
	{
		return _augmentation != null;
	}

	public Augmentation getAugmentation()
	{
		return _augmentation;
	}

	public boolean setAugmentation(Augmentation augmentation)
	{
		if (_augmentation != null)
		{
			_log.info("Warning: Augment set for (" + getObjectId() + ") " + getName(null) + " owner: " + getOwnerId());
			return false;
		}
		_augmentation = augmentation;
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			updateItemAttributes(con);
		}
		catch (final Exception e)
		{
			if (Config.DEBUG)
			{
				_log.warn("Could not update atributes for item: " + this + " from DB:", e);
			}
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
		return true;
	}

	public void removeAugmentation()
	{
		if (_augmentation == null)
		{
			return;
		}
		_augmentation = null;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			if (Config.DEBUG)
			{
				_log.warn("Could not remove augmentation for item: " + this + " from DB:", e);
			}
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void restoreAttributes()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT augAttributes FROM item_attributes WHERE itemId=?");
			statement.setInt(1, getObjectId());
			rs = statement.executeQuery();
			if (rs.next())
			{
				final int aug_attributes = rs.getInt(1);
				if (aug_attributes != -1)
				{
					_augmentation = new Augmentation(rs.getInt("augAttributes"));
				}
			}
			statement.close();
			rs.close();
			
			statement = con.prepareStatement("SELECT elemType,elemValue FROM item_elementals WHERE itemId=?");
			statement.setInt(1, getObjectId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				final byte elem_type = rs.getByte(1);
				final int elem_value = rs.getInt(2);
				if ((elem_type != -1) && (elem_value != -1))
				{
					applyAttribute(elem_type, elem_value);
				}
			}
		}
		catch (final Exception e)
		{
			if (Config.DEBUG)
			{
				_log.warn("Could not restore augmentation and elemental data for item " + this + " from DB: " + e.getMessage(), e);
			}
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	private void updateItemAttributes(Connection con)
	{
		PreparedStatement statement = null;
		try
		{
			statement = con.prepareStatement("REPLACE INTO item_attributes VALUES(?,?)");
			statement.setInt(1, getObjectId());
			statement.setInt(2, _augmentation != null ? _augmentation.getAttributes() : -1);
			statement.executeUpdate();
		}
		catch (final SQLException e)
		{
			if (Config.DEBUG)
			{
				_log.warn("Could not update atributes for item: " + this + " from DB:", e);
			}
		}
		finally
		{
			DbUtils.closeQuietly(statement);
		}
	}

	private void updateItemElements(Connection con)
	{
		PreparedStatement statement = null;
		try
		{
			statement = con.prepareStatement("DELETE FROM item_elementals WHERE itemId = ?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
		}
		finally
		{
			DbUtils.closeQuietly(statement);
		}
		
		if (_elementals == null)
		{
			return;
		}
		
		try
		{
			statement = con.prepareStatement("INSERT INTO item_elementals VALUES(?,?,?)");
			for (final Elementals elm : _elementals)
			{
				statement.setInt(1, getObjectId());
				statement.setByte(2, elm.getElement());
				statement.setInt(3, elm.getValue());
				statement.execute();
				statement.clearParameters();
			}
		}
		catch (final Exception e)
		{
		}
		finally
		{
			DbUtils.closeQuietly(statement);
		}
	}

	public Elementals[] getElementals()
	{
		return _elementals;
	}

	public Elementals getElemental(byte attribute)
	{
		if (_elementals == null)
		{
			return null;
		}
		for (final Elementals elm : _elementals)
		{
			if (elm.getElement() == attribute)
			{
				return elm;
			}
		}
		return null;
	}
	
	public int getAttributeElementValue(byte attribute)
	{
		final Elementals element = getElemental(attribute);
		if (element == null)
		{
			return 0;
		}
		return element.getValue();
	}

	public byte getAttackElementType()
	{
		if (!isWeapon())
		{
			return -2;
		}
		else if (getItem().getElementals() != null)
		{
			return getItem().getElementals()[0].getElement();
		}
		else if (_elementals != null)
		{
			return _elementals[0].getElement();
		}
		return -2;
	}

	public int getAttackElementPower()
	{
		if (!isWeapon())
		{
			return 0;
		}
		else if (getItem().getElementals() != null)
		{
			return getItem().getElementals()[0].getValue();
		}
		else if (_elementals != null)
		{
			return _elementals[0].getValue();
		}
		return 0;
	}

	public int getElementDefAttr(byte element)
	{
		if (!isArmor())
		{
			return 0;
		}
		else if (getItem().getElementals() != null)
		{
			final Elementals elm = getItem().getElemental(element);
			if (elm != null)
			{
				return elm.getValue();
			}
		}
		else if (_elementals != null)
		{
			final Elementals elm = getElemental(element);
			if (elm != null)
			{
				return elm.getValue();
			}
		}
		return 0;
	}

	private void applyAttribute(byte element, int value)
	{
		if (_elementals == null)
		{
			_elementals = new Elementals[1];
			_elementals[0] = new Elementals(element, value);
		}
		else
		{
			Elementals elm = getElemental(element);
			if (elm != null)
			{
				elm.setValue(value);
			}
			else
			{
				elm = new Elementals(element, value);
				final Elementals[] array = new Elementals[_elementals.length + 1];
				System.arraycopy(_elementals, 0, array, 0, _elementals.length);
				array[_elementals.length] = elm;
				_elementals = array;
			}
		}
	}

	public void setElementAttr(byte element, int value)
	{
		applyAttribute(element, value);
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			updateItemElements(con);
		}
		catch (final Exception e)
		{
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}

	public void clearElementAttr(byte element)
	{
		if ((getElemental(element) == null) && (element != -1))
		{
			return;
		}

		Elementals[] array = null;
		if ((element != -1) && (_elementals != null) && (_elementals.length > 1))
		{
			array = new Elementals[_elementals.length - 1];
			int i = 0;
			for (final Elementals elm : _elementals)
			{
				if (elm.getElement() != element)
				{
					array[i++] = elm;
				}
			}
		}
		_elementals = array;

		final String query = (element != -1) ? "DELETE FROM item_elementals WHERE itemId = ? AND elemType = ?" : "DELETE FROM item_elementals WHERE itemId = ?";
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			if (element != -1)
			{
				statement.setInt(2, element);
			}

			statement.setInt(1, getObjectId());
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			if (Config.DEBUG)
			{
				_log.warn("Could not remove elemental enchant for item: " + this + " from DB:", e);
			}
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private class ScheduleConsumeManaTask implements Runnable
	{
		private final ItemInstance _shadowItem;

		public ScheduleConsumeManaTask(ItemInstance item)
		{
			_shadowItem = item;
		}

		@Override
		public void run()
		{
			if (_shadowItem != null)
			{
				if (!_shadowItem.isEquipped())
				{
					_shadowItem.stopManaConsumeTask();
					return;
				}
				_shadowItem.decreaseMana(true);
			}
		}
	}

	public boolean isShadowItem()
	{
		return (_mana >= 0);
	}

	public int getMana()
	{
		return _mana;
	}

	public void decreaseMana(boolean resetConsumingMana)
	{
		decreaseMana(resetConsumingMana, 1);
	}

	public void decreaseMana(boolean resetConsumingMana, int count)
	{
		if (!isShadowItem())
		{
			return;
		}

		if ((_mana - count) >= 0)
		{
			_mana -= count;
		}
		else
		{
			_mana = 0;
		}

		if (_storedInDb)
		{
			_storedInDb = false;
		}
		
		if (resetConsumingMana)
		{
			stopManaConsumeTask();
		}
		
		final Player player = getActingPlayer();
		if (player != null)
		{
			SystemMessage sm;
			switch (_mana)
			{
				case 10 :
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10);
					sm.addItemName(_item);
					player.sendPacket(sm);
					break;
				case 5 :
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5);
					sm.addItemName(_item);
					player.sendPacket(sm);
					break;
				case 1 :
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1);
					sm.addItemName(_item);
					player.sendPacket(sm);
					break;
			}

			if (_mana == 0)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0);
				sm.addItemName(_item);
				player.sendPacket(sm);

				if (isEquipped())
				{
					final ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getLocationSlot());
					final InventoryUpdate iu = new InventoryUpdate();
					for (final ItemInstance item : unequiped)
					{
						item.unChargeAllShots();
						iu.addModifiedItem(item);
					}
					player.sendPacket(iu);
					player.broadcastUserInfo(true);
				}

				if (getItemLocation() != ItemLocation.WAREHOUSE && getItemLocation() != ItemLocation.PRIVATEINV)
				{
					player.getInventory().destroyItem("ItemInstance", this, player, null);

					final InventoryUpdate iu = new InventoryUpdate();
					iu.addRemovedItem(this);
					player.sendPacket(iu);
					player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
				}
				else
				{
					if (getItemLocation() == ItemLocation.PRIVATEINV)
					{
						player.getPrivateInventory().destroyItem("ItemInstance", this, player, null);
					}
					else
					{
						player.getWarehouse().destroyItem("ItemInstance", this, player, null);
					}
				}
				GameObjectsStorage.removeItem(this);
			}
			else
			{
				if ((_consumingMana == null) && isEquipped())
				{
					scheduleConsumeManaTask();
				}
				if (getItemLocation() != ItemLocation.WAREHOUSE && getItemLocation() != ItemLocation.PRIVATEINV)
				{
					final InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(this);
					player.sendPacket(iu);
				}
			}
		}
	}

	public void scheduleConsumeManaTask()
	{
		if (_consumingMana != null)
		{
			return;
		}
		_consumingMana = ThreadPoolManager.getInstance().schedule(new ScheduleConsumeManaTask(this), MANA_CONSUMPTION_RATE);
	}

	public void stopManaConsumeTask()
	{
		if (_consumingMana != null)
		{
			_consumingMana.cancel(true);
			_consumingMana = null;
		}
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return false;
	}

	public Func[] getStatFuncs(Creature player)
	{
		return getItem().getStatFuncs(this, player);
	}

	public void updateDatabase()
	{
		this.updateDatabase(false);
	}

	public void updateDatabase(boolean force)
	{
		_dbLock.lock();

		try
		{
			if (_existsInDb)
			{
				if ((_ownerId == 0) || (_loc == ItemLocation.VOID) || (_loc == ItemLocation.REFUND) || ((getCount() == 0) && (_loc != ItemLocation.LEASE)))
				{
					removeFromDb();
				}
				else if (!Config.LAZY_ITEMS_UPDATE || force)
				{
					updateInDb();
				}
			}
			else
			{
				if ((_ownerId == 0) || (_loc == ItemLocation.VOID) || (_loc == ItemLocation.REFUND) || ((getCount() == 0) && (_loc != ItemLocation.LEASE)))
				{
					return;
				}
				insertIntoDb();
			}
		}
		finally
		{
			_dbLock.unlock();
		}
	}

	public static ItemInstance restoreFromDb(int ownerId, ResultSet rs)
	{
		ItemInstance inst = null;
		int objectId, item_id, loc_data, enchant_level, custom_type1,
		        custom_type2, manaLeft, visual_itemId, agathionEnergy, isEventItem;
		long time, count;
		ItemLocation loc;
		try
		{
			objectId = rs.getInt(1);
			item_id = rs.getInt("item_id");
			count = rs.getLong("count");
			loc = ItemLocation.valueOf(rs.getString("loc"));
			loc_data = rs.getInt("loc_data");
			enchant_level = rs.getInt("enchant_level");
			custom_type1 = rs.getInt("custom_type1");
			custom_type2 = rs.getInt("custom_type2");
			manaLeft = rs.getInt("mana_left");
			time = rs.getLong("time");
			visual_itemId = rs.getInt("visual_itemId");
			agathionEnergy = rs.getInt("agathion_energy");
			isEventItem = rs.getInt("is_event");
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore an item owned by " + ownerId + " from DB:", e);
			return null;
		}
		final Item item = ItemsParser.getInstance().getTemplate(item_id);
		if (item == null)
		{
			if (Config.DEBUG)
			{
				_log.error("Item item_id=" + item_id + " not known, object_id=" + objectId);
			}
			return null;
		}
		inst = new ItemInstance(objectId, item);
		inst._ownerId = ownerId;
		inst.setCount(count);
		inst._enchantLevel = enchant_level;
		inst._type1 = custom_type1;
		inst._type2 = custom_type2;
		inst._loc = loc;
		inst._locData = loc_data;
		inst._existsInDb = true;
		inst._storedInDb = true;
		inst._mana = manaLeft;
		inst._time = time;
		inst._visualItemId = visual_itemId;
		inst._agathionEnergy = agathionEnergy;
		inst._isEventItem = isEventItem == 1;

		if (inst.isEquipable())
		{
			inst.restoreAttributes();
		}

		return inst;
	}

	public class ItemDropTask implements Runnable
	{
		private int _x, _y, _z;
		private final Creature _dropper;
		private final ItemInstance _itm;

		public ItemDropTask(ItemInstance item, Creature dropper, int x, int y, int z, boolean geoCheck)
		{
			_x = x;
			_y = y;
			_z = geoCheck ? GeoEngine.getInstance().getSpawnHeight(x, y, z) : z;
			_dropper = dropper;
			_itm = item;
		}

		@Override
		public final void run()
		{
			assert _itm.getWorldRegion() == null;

			if ((Config.GEODATA) && (_dropper != null))
			{
				if (!GeoEngine.getInstance().canMoveToCoord(_dropper, _dropper.getX(), _dropper.getY(), _dropper.getZ(), _x, _y, _z, _dropper.getReflection(), false) || (_x == 0 && _y == 0 && _z == 0))
				{
					_x = _dropper.getX();
					_y = _dropper.getY();
					_z = _dropper.getZ();
				}
			}
			
			if (_x == 0 && _y == 0 && _z == 0)
			{
				_log.warn("Item " + getName(null) + " has wrong zero coords!" + _dropper != null ? "" + _dropper.getName(null) + " drop this item!" : "");
			}
			
			setReflection(_dropper != null ? _dropper.getReflection() : ReflectionManager.DEFAULT);

			_itm.setDropTime(System.currentTimeMillis());
			_itm.setDropperObjectId(_dropper != null ? _dropper.getObjectId() : 0);
			_itm.spawnMe(_x, _y, _z);
			if ((_itm.getReflectionId() > 0) && (ReflectionManager.getInstance().getReflection(getReflectionId()) != null))
			{
				ReflectionManager.getInstance().getReflection(getReflectionId()).addItem(_itm);
			}
			_itm.setDropperObjectId(0);
		}
	}

	public final void dropMe(Creature dropper, int x, int y, int z, boolean geoCheck)
	{
		ThreadPoolManager.getInstance().execute(new ItemDropTask(this, dropper, x, y, z, geoCheck));
	}

	public final void dropMe(Creature dropper, Location loc, boolean geoCheck)
	{
		ThreadPoolManager.getInstance().execute(new ItemDropTask(this, dropper, loc.getX(), loc.getY(), loc.getZ(), geoCheck));
	}

	private void updateInDb()
	{
		assert _existsInDb;

		if (_wear)
		{
			return;
		}

		if (_storedInDb)
		{
			return;
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE items SET owner_id=?,item_id=?,count=?,loc=?,loc_data=?,enchant_level=?,custom_type1=?,custom_type2=?,mana_left=?,time=?,visual_itemId=?,agathion_energy=?,is_event=? " + "WHERE object_id = ?");
			statement.setInt(1, _ownerId);
			statement.setInt(2, getId());
			statement.setLong(3, getCount());
			statement.setString(4, _loc.name());
			statement.setInt(5, _locData);
			statement.setInt(6, getEnchantLevel());
			statement.setInt(7, getCustomType1());
			statement.setInt(8, getCustomType2());
			statement.setInt(9, getMana());
			statement.setLong(10, getTime());
			statement.setInt(11, getVisualItemId());
			statement.setInt(12, getAgathionEnergy());
			statement.setInt(13, isEventItem() ? 1 : 0);
			statement.setInt(14, getObjectId());
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
		}
		catch (final Exception e)
		{
			if (Config.DEBUG)
			{
				_log.warn("Could not update item " + this + " in DB: Reason: " + e.getMessage(), e);
			}
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void insertIntoDb()
	{
		assert !_existsInDb && (getObjectId() != 0);

		if (_wear)
		{
			return;
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,mana_left,time,visual_itemId,agathion_energy,is_event) " + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _ownerId);
			statement.setInt(2, _itemId);
			statement.setLong(3, getCount());
			statement.setString(4, _loc.name());
			statement.setInt(5, _locData);
			statement.setInt(6, getEnchantLevel());
			statement.setInt(7, getObjectId());
			statement.setInt(8, _type1);
			statement.setInt(9, _type2);
			statement.setInt(10, getMana());
			statement.setLong(11, getTime());
			statement.setInt(12, getVisualItemId());
			statement.setInt(13, getAgathionEnergy());
			statement.setInt(14, isEventItem() ? 1 : 0);
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;

			if (_augmentation != null)
			{
				updateItemAttributes(con);
			}
			if (_elementals != null)
			{
				updateItemElements(con);
			}
		}
		catch (final Exception e)
		{
			if (Config.DEBUG)
			{
				_log.warn("Could not insert item " + this + " into DB: Reason: " + e.getMessage(), e);
			}
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void removeFromDb()
	{
		assert _existsInDb;

		if (_wear)
		{
			return;
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM items WHERE object_id = ?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			_existsInDb = false;
			_storedInDb = false;
			statement.close();

			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			statement.close();

			statement = con.prepareStatement("DELETE FROM item_elementals WHERE itemId = ?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.warn("Could not delete item " + this + " in DB: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	@Override
	public String toString()
	{
		final var enchant = getEnchantLevel() > 0 ? "[+" + getEnchantLevel() + "]" : "";
		return _item + "" + enchant + "" + "[" + getObjectId() + "]";
	}

	public void setProtected(boolean isProtected)
	{
		_protected = isProtected;
	}

	public boolean isProtected()
	{
		return _protected;
	}

	public boolean isNightLure()
	{
		return (((_itemId >= 8505) && (_itemId <= 8513)) || (_itemId == 8485));
	}

	public void setCountDecrease(boolean decrease)
	{
		_decrease = decrease;
	}

	public boolean getCountDecrease()
	{
		return _decrease;
	}

	public void setInitCount(int InitCount)
	{
		_initCount = InitCount;
	}

	public long getInitCount()
	{
		return _initCount;
	}

	public void restoreInitCount()
	{
		if (_decrease)
		{
			setCount(_initCount);
		}
	}

	public boolean isTimeLimitedItem()
	{
		return (_time > 0);
	}

	public long getTime()
	{
		return _time;
	}
	
	public void setTime(long time)
	{
		_time = System.currentTimeMillis() + (time * 60 * 1000L);
		scheduleLifeTimeTask();
	}

	public long getRemainingTime()
	{
		return _time - System.currentTimeMillis();
	}

	public void endOfLife()
	{
		final Player player = getActingPlayer();
		if (player != null)
		{
			if (isEquipped())
			{
				final ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getLocationSlot());
				final InventoryUpdate iu = new InventoryUpdate();
				for (final ItemInstance item : unequiped)
				{
					item.unChargeAllShots();
					iu.addModifiedItem(item);
				}
				player.sendPacket(iu);
				player.broadcastUserInfo(true);
			}

			if (getItemLocation() != ItemLocation.WAREHOUSE && getItemLocation() != ItemLocation.PRIVATEINV)
			{
				player.getInventory().destroyItem("ItemInstance", this, player, null);

				final InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(this);
				player.sendPacket(iu);
				player.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
			}
			else
			{
				if (getItemLocation() == ItemLocation.PRIVATEINV)
				{
					player.getPrivateInventory().destroyItem("ItemInstance", this, player, null);
				}
				else
				{
					player.getWarehouse().destroyItem("ItemInstance", this, player, null);
				}
			}
			player.sendPacket(SystemMessageId.TIME_LIMITED_ITEM_DELETED);
			GameObjectsStorage.removeItem(this);
		}
	}

	public void scheduleLifeTimeTask()
	{
		if (!isTimeLimitedItem())
		{
			return;
		}
		if (getRemainingTime() <= 0)
		{
			endOfLife();
		}
		else
		{
			if (_lifeTimeTask != null)
			{
				_lifeTimeTask.cancel(false);
			}
			_lifeTimeTask = ThreadPoolManager.getInstance().schedule(new ScheduleLifeTimeTask(this), getRemainingTime());
		}
	}

	private class ScheduleLifeTimeTask implements Runnable
	{
		private final ItemInstance _limitedItem;

		public ScheduleLifeTimeTask(ItemInstance item)
		{
			_limitedItem = item;
		}

		@Override
		public void run()
		{
			if (_limitedItem != null)
			{
				_limitedItem.endOfLife();
			}
		}
	}

	public void updateElementAttrBonus(Player player)
	{
		if (_elementals == null)
		{
			return;
		}
		for (final Elementals elm : _elementals)
		{
			elm.updateBonus(player, isArmor());
		}
	}

	public void removeElementAttrBonus(Player player)
	{
		if (_elementals == null)
		{
			return;
		}
		for (final Elementals elm : _elementals)
		{
			elm.removeBonus(player);
		}
	}

	public void setDropperObjectId(int id)
	{
		_dropperObjectId = id;
	}

	@Override
	public void sendInfo(Player activeChar)
	{
		if (_dropperObjectId != 0)
		{
			activeChar.sendPacket(new DropItem(this, _dropperObjectId));
		}
		else
		{
			activeChar.sendPacket(new SpawnItem(this));
		}
	}

	public final DropProtection getDropProtection()
	{
		return _dropProtection;
	}

	public boolean isPublished()
	{
		return _published;
	}

	public void publish()
	{
		_published = true;
	}

	public boolean isQuestItem()
	{
		return getItem().isQuestItem();
	}

	public boolean isElementable()
	{
		if ((getItemLocation() == ItemLocation.INVENTORY) || (getItemLocation() == ItemLocation.PAPERDOLL))
		{
			return getItem().isElementable();
		}
		return false;
	}

	public boolean isFreightable()
	{
		return getItem().isFreightable();
	}

	public int useSkillDisTime()
	{
		return getItem().useSkillDisTime();
	}

	public int getOlyEnchantLevel()
	{
		final Player player = getActingPlayer();
		int enchant = getEnchantLevel();

		if (player == null)
		{
			return enchant;
		}

		if (player.isInOlympiadMode())
		{
			final var enchantLimit = getItem().isWeapon() ? Config.ALT_OLY_WEAPON_ENCHANT_LIMIT : getItem().isAccessory() ? Config.ALT_OLY_ACCESSORY_ENCHANT_LIMIT : Config.ALT_OLY_ARMOR_ENCHANT_LIMIT;
			if (enchantLimit >= 0 && enchant > enchantLimit)
			{
				enchant = enchantLimit;
			}
		}
		return enchant;
	}

	public int getDefaultEnchantLevel()
	{
		return _item.getDefaultEnchantLevel();
	}

	public boolean hasPassiveSkills()
	{
		return (getItemType() == EtcItemType.RUNE) && (getItemLocation() == ItemLocation.INVENTORY) && (getOwnerId() > 0) && getItem().hasSkills();
	}

	public void giveSkillsToOwner()
	{
		if (!hasPassiveSkills())
		{
			return;
		}

		final Player player = getActingPlayer();

		if (player != null)
		{
			for (final SkillHolder sh : getItem().getSkills())
			{
				if (sh.getSkill() != null && (sh.getSkill().isPassive() || (getItem().isActiveRune() && !sh.getSkill().isPassive())))
				{
					if (player.isInFightEvent())
					{
						for (final var e : player.getFightEvents())
						{
							if (e != null && !e.canUseMagic(player, player, sh.getSkill()))
							{
								continue;
							}
						}
					}

					if (player.isInPartyTournament())
					{
						var e = player.getPartyTournament();
						if (e != null && !e.canUseMagic(player, player, sh.getSkill()))
						{
							continue;
						}
					}

					final Skill skill = player.getKnownSkill(sh.getSkill().getId());
					if (skill != null)
					{
						if (skill.getLevel() < sh.getSkill().getLevel())
						{
							player.addSkill(sh.getSkill(), false);
							sh.getSkill().setItemSkill(true);
						}
					}
					else
					{
						player.addSkill(sh.getSkill(), false);
						sh.getSkill().setItemSkill(true);
					}
				}
			}
			player.sendSkillList(false);
		}
	}

	public void removeSkillsFromOwner()
	{
		if (!hasPassiveSkills())
		{
			return;
		}

		final Player player = getActingPlayer();

		if (player != null)
		{
			for (final SkillHolder sh : getItem().getSkills())
			{
				if (sh.getSkill() != null && (sh.getSkill().isPassive() || (getItem().isActiveRune() && !sh.getSkill().isPassive())))
				{
					player.removeSkill(sh.getSkill(), false, true);
				}
			}
			player.getInventory().checkRuneSkills();
			player.sendSkillList(false);
		}
	}

	@Override
	public boolean isItem()
	{
		return true;
	}

	@Override
	public Player getActingPlayer()
	{
		return GameObjectsStorage.getPlayer(getOwnerId());
	}

	public int getEquipReuseDelay()
	{
		return _item.getEquipReuseDelay();
	}

	public void onBypassFeedback(Player activeChar, String command)
	{
		if (command.startsWith("Quest"))
		{
			String questName = command.substring(6);
			String content = null;

			String event = null;
			final int idx = questName.indexOf(' ');
			if (idx > 0)
			{
				event = questName.substring(idx).trim();
				questName = questName.substring(0, idx);
			}

			final Quest q = QuestManager.getInstance().getQuest(questName);
			QuestState qs = activeChar.getQuestState(questName);

			if (q != null)
			{
				if (((q.getId() >= 1) && (q.getId() < 20000)) && ((activeChar.getWeightPenalty() >= 3) || !activeChar.isInventoryUnder90(true)))
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT));
					return;
				}

				if (qs == null)
				{
					if ((q.getId() >= 1) && (q.getId() < 20000))
					{
						if (activeChar.getAllActiveQuests().length > 40)
						{
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TOO_MANY_QUESTS));
							return;
						}
					}
					qs = q.newQuestState(activeChar);
				}
			}
			else
			{
				content = Quest.getNoQuestMsg(activeChar);
			}

			if (qs != null)
			{
				if ((event != null) && !qs.getQuest().notifyItemEvent(this, activeChar, event))
				{
					return;
				}
				else if (!qs.getQuest().notifyItemTalk(this, activeChar))
				{
					return;
				}
				questName = qs.getQuest().getName();
				final String stateId = State.getStateName(qs.getState());
				final String path = "data/html/scripts/quests/" + questName + "/" + stateId + ".htm";
				content = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), path);
			}

			if (content != null)
			{
				showChatWindow(activeChar, content);
			}
			activeChar.sendActionFailed();
		}
	}

	public void showChatWindow(Player activeChar, String content)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, getId());
		html.setHtml(activeChar, content);
		html.replace("%itemId%", String.valueOf(getObjectId()));
		activeChar.sendPacket(html);
	}

	@Override
	public boolean isChargedShot(ShotType type)
	{
		return (_shotsMask & type.getMask()) == type.getMask();
	}

	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		if (charged)
		{
			_shotsMask |= type.getMask();
		}
		else
		{
			_shotsMask &= ~type.getMask();
		}
	}

	public void unChargeAllShots()
	{
		_shotsMask = 0;
	}

	public int[] getEnchantOptions()
	{
		final EnchantOptions op = EnchantItemOptionsParser.getInstance().getOptions(this);
		if (op != null)
		{
			return op.getOptions();
		}
		return DEFAULT_ENCHANT_OPTIONS;
	}

	public void clearEnchantStats()
	{
		final Player player = getActingPlayer();
		if (player == null)
		{
			_enchantOptions.clear();
			return;
		}

		for (final Options op : _enchantOptions)
		{
			op.remove(player);
		}
		_enchantOptions.clear();
	}

	public void applyEnchantStats()
	{
		final Player player = getActingPlayer();
		if (!isEquipped() || (player == null) || (getEnchantOptions() == DEFAULT_ENCHANT_OPTIONS))
		{
			return;
		}

		for (final int id : getEnchantOptions())
		{
			final Options options = OptionsParser.getInstance().getOptions(id);
			if (options != null)
			{
				options.apply(player);
				_enchantOptions.add(options);
			}
			else if (id != 0)
			{
				_log.info("applyEnchantStats: Couldn't find option: " + id);
			}
		}
	}

	@Override
	public void deleteMe()
	{
		stopManaConsumeTask();
		stopEnergyConsumeTask();
		if ((_lifeTimeTask != null) && !_lifeTimeTask.isDone())
		{
			_lifeTimeTask.cancel(false);
			_lifeTimeTask = null;
		}
	}

	public void setMana(int value)
	{
		_mana = value;
	}
	
	public int getVisualItemId()
	{
		return _visualItemId;
	}

	public void setVisualItemId(int visualItemId)
	{
		_visualItemId = visualItemId;
	}
	
	public boolean isEnergyItem()
	{
		return (_agathionEnergy >= 0);
	}
	
	public int getAgathionEnergy()
	{
		return _agathionEnergy;
	}

	public void setAgathionEnergy(int agathionEnergy)
	{
		_agathionEnergy = agathionEnergy;
	}
	
	public void decreaseEnergy(boolean resetConsumingEnergy)
	{
		decreaseEnergy(resetConsumingEnergy, 1);
	}

	public void decreaseEnergy(boolean resetConsumingEnergy, int count)
	{
		if (!isEnergyItem())
		{
			return;
		}

		if ((_agathionEnergy - count) >= 0)
		{
			_agathionEnergy -= count;
		}
		else
		{
			_agathionEnergy = 0;
		}

		if (_storedInDb)
		{
			_storedInDb = false;
		}
		if (resetConsumingEnergy)
		{
			stopEnergyConsumeTask();
		}

		final Player player = getActingPlayer();
		if (player != null)
		{
			if (_agathionEnergy == 0)
			{
				if (isEquipped() && player.getAgathionId() > 0)
				{
					player.setAgathionId(0);
					player.broadcastUserInfo(true);
				}
			}
			else
			{
				if (_consumingEnergy == null && isEquipped())
				{
					scheduleConsumeEnergyTask();
				}
				if (getItemLocation() != ItemLocation.WAREHOUSE && getItemLocation() != ItemLocation.PRIVATEINV)
				{
					final InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(this);
					player.sendPacket(iu);
				}
			}
		}
	}

	public void scheduleConsumeEnergyTask()
	{
		if (_consumingEnergy != null)
		{
			return;
		}
		_consumingEnergy = ThreadPoolManager.getInstance().schedule(new ScheduleConsumeEnergyTask(this), ENERGY_CONSUMPTION_RATE);
	}
	
	private class ScheduleConsumeEnergyTask implements Runnable
	{
		private final ItemInstance _energyItem;

		public ScheduleConsumeEnergyTask(ItemInstance item)
		{
			_energyItem = item;
		}

		@Override
		public void run()
		{
			if (_energyItem != null)
			{
				_energyItem.decreaseEnergy(true);
			}
		}
	}
	
	public void stopEnergyConsumeTask()
	{
		if (_consumingEnergy != null)
		{
			_consumingEnergy.cancel(true);
			_consumingEnergy = null;
		}
	}
	
	public void setIsEventItem(boolean b)
	{
		_isEventItem = b;
	}
	
	public boolean isEventItem()
	{
		return _isEventItem;
	}
	
	public boolean isTalisman()
	{
		return getItem().isTalisman();
	}
	
	public boolean isBlockResetReuse()
	{
		return _item.isBlockResetReuse();
	}
}