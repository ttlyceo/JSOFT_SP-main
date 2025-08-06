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
package l2e.gameserver.data.parser;

import static l2e.gameserver.model.items.itemcontainer.PcInventory.ADENA_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import l2e.commons.apache.StringUtils;
import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.Log;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.GMAudit;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.actor.templates.items.EtcItem;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.model.items.type.ArmorType;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.reward.CalculateRewardChances;
import l2e.gameserver.model.skills.engines.DocumentEngine;
import l2e.gameserver.model.skills.engines.items.ItemTemplate;

public class ItemsParser extends LoggerObject
{
	public static final Map<String, Integer> _materials = new HashMap<>();
	public static final Map<String, Integer> _crystalTypes = new HashMap<>();
	public static final Map<String, Integer> _slots = new HashMap<>();
	public static final Map<String, WeaponType> _weaponTypes = new HashMap<>();
	public static final Map<String, ArmorType> _armorTypes = new HashMap<>();

	private Item[] _allTemplates;
	private Item[] _droppableTemplates;
	private final Map<Integer, EtcItem> _etcItems = new HashMap<>();
	private final Map<Integer, Armor> _armors = new HashMap<>();
	private final Map<Integer, Weapon> _weapons = new HashMap<>();
	
	static
	{
		_materials.put("adamantaite", Item.MATERIAL_ADAMANTAITE);
		_materials.put("blood_steel", Item.MATERIAL_BLOOD_STEEL);
		_materials.put("bone", Item.MATERIAL_BONE);
		_materials.put("bronze", Item.MATERIAL_BRONZE);
		_materials.put("cloth", Item.MATERIAL_CLOTH);
		_materials.put("chrysolite", Item.MATERIAL_CHRYSOLITE);
		_materials.put("cobweb", Item.MATERIAL_COBWEB);
		_materials.put("cotton", Item.MATERIAL_FINE_STEEL);
		_materials.put("crystal", Item.MATERIAL_CRYSTAL);
		_materials.put("damascus", Item.MATERIAL_DAMASCUS);
		_materials.put("dyestuff", Item.MATERIAL_DYESTUFF);
		_materials.put("fine_steel", Item.MATERIAL_FINE_STEEL);
		_materials.put("fish", Item.MATERIAL_FISH);
		_materials.put("gold", Item.MATERIAL_GOLD);
		_materials.put("horn", Item.MATERIAL_HORN);
		_materials.put("leather", Item.MATERIAL_LEATHER);
		_materials.put("liquid", Item.MATERIAL_LIQUID);
		_materials.put("mithril", Item.MATERIAL_MITHRIL);
		_materials.put("oriharukon", Item.MATERIAL_ORIHARUKON);
		_materials.put("paper", Item.MATERIAL_PAPER);
		_materials.put("rune_xp", Item.MATERIAL_RUNE_XP);
		_materials.put("rune_sp", Item.MATERIAL_RUNE_SP);
		_materials.put("rune_remove_penalty", Item.MATERIAL_RUNE_PENALTY);
		_materials.put("scale_of_dragon", Item.MATERIAL_SCALE_OF_DRAGON);
		_materials.put("seed", Item.MATERIAL_SEED);
		_materials.put("silver", Item.MATERIAL_SILVER);
		_materials.put("steel", Item.MATERIAL_STEEL);
		_materials.put("wood", Item.MATERIAL_WOOD);
		
		_crystalTypes.put("s84", Item.CRYSTAL_S84);
		_crystalTypes.put("s80", Item.CRYSTAL_S80);
		_crystalTypes.put("s", Item.CRYSTAL_S);
		_crystalTypes.put("a", Item.CRYSTAL_A);
		_crystalTypes.put("b", Item.CRYSTAL_B);
		_crystalTypes.put("c", Item.CRYSTAL_C);
		_crystalTypes.put("d", Item.CRYSTAL_D);
		_crystalTypes.put("none", Item.CRYSTAL_NONE);
		
		for (final WeaponType type : WeaponType.values())
		{
			_weaponTypes.put(type.toString(), type);
		}
		
		for (final ArmorType type : ArmorType.values())
		{
			_armorTypes.put(type.toString(), type);
		}
		
		_slots.put("shirt", Item.SLOT_UNDERWEAR);
		_slots.put("lbracelet", Item.SLOT_L_BRACELET);
		_slots.put("rbracelet", Item.SLOT_R_BRACELET);
		_slots.put("talisman", Item.SLOT_DECO);
		_slots.put("chest", Item.SLOT_CHEST);
		_slots.put("fullarmor", Item.SLOT_FULL_ARMOR);
		_slots.put("head", Item.SLOT_HEAD);
		_slots.put("hair", Item.SLOT_HAIR);
		_slots.put("hairall", Item.SLOT_HAIRALL);
		_slots.put("underwear", Item.SLOT_UNDERWEAR);
		_slots.put("back", Item.SLOT_BACK);
		_slots.put("neck", Item.SLOT_NECK);
		_slots.put("legs", Item.SLOT_LEGS);
		_slots.put("feet", Item.SLOT_FEET);
		_slots.put("gloves", Item.SLOT_GLOVES);
		_slots.put("chest,legs", Item.SLOT_CHEST | Item.SLOT_LEGS);
		_slots.put("belt", Item.SLOT_BELT);
		_slots.put("rhand", Item.SLOT_R_HAND);
		_slots.put("lhand", Item.SLOT_L_HAND);
		_slots.put("lrhand", Item.SLOT_LR_HAND);
		_slots.put("rear;lear", Item.SLOT_R_EAR | Item.SLOT_L_EAR);
		_slots.put("rfinger;lfinger", Item.SLOT_R_FINGER | Item.SLOT_L_FINGER);
		_slots.put("wolf", Item.SLOT_WOLF);
		_slots.put("greatwolf", Item.SLOT_GREATWOLF);
		_slots.put("hatchling", Item.SLOT_HATCHLING);
		_slots.put("strider", Item.SLOT_STRIDER);
		_slots.put("babypet", Item.SLOT_BABYPET);
		_slots.put("none", Item.SLOT_NONE);
		
		_slots.put("onepiece", Item.SLOT_FULL_ARMOR);
		_slots.put("hair2", Item.SLOT_HAIR2);
		_slots.put("dhair", Item.SLOT_HAIRALL);
		_slots.put("alldress", Item.SLOT_ALLDRESS);
		_slots.put("deco1", Item.SLOT_DECO);
		_slots.put("waist", Item.SLOT_BELT);
		
	}
	
	public static ItemsParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public ItemTemplate newItem()
	{
		return new ItemTemplate();
	}
	
	protected ItemsParser()
	{
		load();
	}
	
	private void load()
	{
		var highest = 0;
		_armors.clear();
		_etcItems.clear();
		_weapons.clear();
		for (final var item : DocumentEngine.getInstance().loadItems())
		{
			if (highest < item.getId())
			{
				highest = item.getId();
			}
			if (item instanceof EtcItem)
			{
				_etcItems.put(item.getId(), (EtcItem) item);
			}
			else if (item instanceof Armor)
			{
				_armors.put(item.getId(), (Armor) item);
			}
			else
			{
				_weapons.put(item.getId(), (Weapon) item);
			}
		}
		buildFastLookupTable(highest);
		if (Config.DEBUG)
		{
			info("Loaded: " + _etcItems.size() + " etc items");
			info("Loaded: " + _armors.size() + " armor items");
			info("Loaded: " + _weapons.size() + " weapon items");
		}
		info("Loaded: " + (_etcItems.size() + _armors.size() + _weapons.size()) + " items template.");
	}
	
	private void buildFastLookupTable(int size)
	{
		if (Config.DEBUG)
		{
			info("highest item id used:" + size);
		}
		_allTemplates = new Item[size + 1];
		
		for (final var item : _armors.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		for (final var item : _weapons.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		for (final var item : _etcItems.values())
		{
			_allTemplates[item.getId()] = item;
		}
	}
	
	public Item getTemplate(int id)
	{
		if ((id >= _allTemplates.length) || (id < 0))
		{
			return null;
		}
		
		return _allTemplates[id];
	}
	
	public ItemInstance createItem(String process, int itemId, long count, Player actor, Object reference)
	{
		final var item = new ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		if (Config.DEBUG)
		{
			info("Item created  oid:" + item.getObjectId() + " itemid:" + itemId);
		}
		
		GameObjectsStorage.addItem(item);
		
		if (item.isStackable() && (count > 1))
		{
			item.setCount(count);
		}
		
		if (Config.LOG_ITEMS && !process.equals("Reset"))
		{
			if (!Config.LOG_ITEMS_SMALL_LOG || (Config.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || (item.getId() == ADENA_ID))))
			{
				Log.addLogItem(actor, "CREATE", item);
			}
		}
		
		if (actor != null)
		{
			if (actor.isGM())
			{
				var referenceName = "no-reference";
				if (reference instanceof GameObject)
				{
					referenceName = (((GameObject) reference).getName(null) != null ? ((GameObject) reference).getName(null) : "no-name");
				}
				else if (reference instanceof String)
				{
					referenceName = (String) reference;
				}
				final var targetName = (actor.getTarget() != null ? actor.getTarget().getName(null) : "no-target");
				if (Config.GMAUDIT)
				{
					GMAudit.auditGMAction(actor.getName(null) + " [" + actor.getObjectId() + "]", process + "(id: " + itemId + " count: " + count + " name: " + item.getItem().getName(null) + " objId: " + item.getObjectId() + ")", targetName, "GameObject referencing this action is: " + referenceName);
				}
			}
		}
		
		return item;
	}

	public ItemInstance createItem(int itemId)
	{
		final var item = new ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		GameObjectsStorage.addItem(item);
		item.setItemLocation(ItemLocation.VOID);
		item.setCount(1L);
		return item;
	}
	
	public ItemInstance createItem(String process, int itemId, int count, Player actor)
	{
		return createItem(process, itemId, count, actor, null);
	}
	
	public ItemInstance createDummyItem(int itemId)
	{
		final var item = getTemplate(itemId);
		if (item == null)
		{
			return null;
		}
		final var temp = new ItemInstance(0, item);
		return temp;
	}
	
	public void destroyItem(String process, ItemInstance item, Player actor, Object reference)
	{
		synchronized (item)
		{
			item.getCount();
			item.setCount(0);
			item.setOwnerId(0);
			item.setItemLocation(ItemLocation.VOID);
			item.setLastChange(ItemInstance.REMOVED);
			GameObjectsStorage.removeItem(item);
			IdFactory.getInstance().releaseId(item.getObjectId());
			
			if (Config.LOG_ITEMS)
			{
				if (!Config.LOG_ITEMS_SMALL_LOG || (Config.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || (item.getId() == ADENA_ID))))
				{
					Log.addLogItem(actor, "DELETE", item);
				}
			}
			
			if (actor != null)
			{
				if (actor.isGM())
				{
					var referenceName = "no-reference";
					if (reference instanceof GameObject)
					{
						referenceName = (((GameObject) reference).getName(null) != null ? ((GameObject) reference).getName(null) : "no-name");
					}
					else if (reference instanceof String)
					{
						referenceName = (String) reference;
					}
					final var targetName = (actor.getTarget() != null ? actor.getTarget().getName(null) : "no-target");
					if (Config.GMAUDIT)
					{
						GMAudit.auditGMAction(actor.getName(null) + " [" + actor.getObjectId() + "]", process + "(id: " + item.getId() + " count: " + item.getCount() + " itemObjId: " + item.getObjectId() + ")", targetName, "GameObject referencing this action is: " + referenceName);
					}
				}
			}
			
			if (item.getItem().isPetItem())
			{
				Connection con = null;
				PreparedStatement statement = null;
				try
				{
					con = DatabaseFactory.getInstance().getConnection();
					statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
					statement.setInt(1, item.getObjectId());
					statement.execute();
				}
				catch (final Exception e)
				{
					warn("could not delete pet objectid:", e);
				}
				finally
				{
					DbUtils.closeQuietly(con, statement);
				}
			}
		}
	}
	
	public void reload()
	{
		load();
		EnchantItemHPBonusParser.getInstance().load();
	}
	
	public Set<Integer> getAllArmorsId()
	{
		return _armors.keySet();
	}
	
	public Set<Integer> getAllWeaponsId()
	{
		return _weapons.keySet();
	}
	
	public int getArraySize()
	{
		return _allTemplates.length;
	}

	public List<Item> getItemsByNameContainingString(CharSequence name, boolean onlyDroppable)
	{
		final Item[] toChooseFrom = onlyDroppable ? getDroppableTemplates() : _allTemplates;
		final List<Item> templates = new ArrayList<>();
		for (final var template : toChooseFrom)
		{
			if (template != null)
			{
				for (final var lang : Config.MULTILANG_ALLOWED)
				{
					if (lang != null && StringUtils.containsIgnoreCase(template.getName(lang), name))
					{
						templates.add(template);
					}
				}
			}
		}
		return templates;
	}

	public Item[] getDroppableTemplates()
	{
		if (_droppableTemplates == null)
		{
			final List<Item> templates = CalculateRewardChances.getDroppableItems();
			_droppableTemplates = templates.toArray(new Item[templates.size()]);
		}
		return _droppableTemplates;
	}
	
	public Item[] getAllItems()
	{
		return _allTemplates;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemsParser _instance = new ItemsParser();
	}
}