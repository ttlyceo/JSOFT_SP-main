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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.StringUtil;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ArmorSetsParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.handler.skillhandlers.SkillHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.ArmorSetTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.Stats;

public abstract class Inventory extends ItemContainer
{
	public interface PaperdollListener
	{
		public void notifyEquiped(int slot, ItemInstance inst, Inventory inventory);

		public void notifyUnequiped(int slot, ItemInstance inst, Inventory inventory);
	}
	
	public static final int PAPERDOLL_UNDER = 0;
	public static final int PAPERDOLL_HEAD = 1;
	public static final int PAPERDOLL_HAIR = 2;
	public static final int PAPERDOLL_HAIR2 = 3;
	public static final int PAPERDOLL_NECK = 4;
	public static final int PAPERDOLL_RHAND = 5;
	public static final int PAPERDOLL_CHEST = 6;
	public static final int PAPERDOLL_LHAND = 7;
	public static final int PAPERDOLL_REAR = 8;
	public static final int PAPERDOLL_LEAR = 9;
	public static final int PAPERDOLL_GLOVES = 10;
	public static final int PAPERDOLL_LEGS = 11;
	public static final int PAPERDOLL_FEET = 12;
	public static final int PAPERDOLL_RFINGER = 13;
	public static final int PAPERDOLL_LFINGER = 14;
	public static final int PAPERDOLL_LBRACELET = 15;
	public static final int PAPERDOLL_RBRACELET = 16;
	public static final int PAPERDOLL_DECO1 = 17;
	public static final int PAPERDOLL_DECO2 = 18;
	public static final int PAPERDOLL_DECO3 = 19;
	public static final int PAPERDOLL_DECO4 = 20;
	public static final int PAPERDOLL_DECO5 = 21;
	public static final int PAPERDOLL_DECO6 = 22;
	public static final int PAPERDOLL_CLOAK = 23;
	public static final int PAPERDOLL_BELT = 24;
	public static final int PAPERDOLL_TOTALSLOTS = 25;

	public static final double MAX_ARMOR_WEIGHT = 12000;

	protected final ItemInstance[] _paperdoll;
	private final List<PaperdollListener> _paperdollListeners;

	protected int _totalWeight;

	private int _wearedMask;

	private static final class ChangeRecorder implements PaperdollListener
	{
		private final Inventory _inventory;
		private final List<ItemInstance> _changed;

		ChangeRecorder(Inventory inventory)
		{
			_inventory = inventory;
			_changed = new ArrayList<>();
			_inventory.addPaperdollListener(this);
		}

		@Override
		public void notifyEquiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (!_changed.contains(item))
			{
				_changed.add(item);
			}
		}

		@Override
		public void notifyUnequiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (!_changed.contains(item))
			{
				_changed.add(item);
			}
		}

		public ItemInstance[] getChangedItems()
		{
			return _changed.toArray(new ItemInstance[_changed.size()]);
		}
	}

	private static final class BowCrossRodListener implements PaperdollListener
	{
		private static BowCrossRodListener instance = new BowCrossRodListener();

		public static BowCrossRodListener getInstance()
		{
			return instance;
		}

		@Override
		public void notifyUnequiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (slot != PAPERDOLL_RHAND)
			{
				return;
			}

			if (item.getItemType() == WeaponType.BOW)
			{
				final ItemInstance arrow = inventory.getPaperdollItem(PAPERDOLL_LHAND);

				if (arrow != null)
				{
					inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
				}
			}
			else if (item.getItemType() == WeaponType.CROSSBOW)
			{
				final ItemInstance bolts = inventory.getPaperdollItem(PAPERDOLL_LHAND);

				if (bolts != null)
				{
					inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
				}
			}
			else if (item.getItemType() == WeaponType.FISHINGROD)
			{
				final ItemInstance lure = inventory.getPaperdollItem(PAPERDOLL_LHAND);

				if (lure != null)
				{
					inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
				}
			}
		}

		@Override
		public void notifyEquiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (slot != PAPERDOLL_RHAND)
			{
				return;
			}

			if (item.getItemType() == WeaponType.BOW)
			{
				final ItemInstance arrow = inventory.findArrowForBow(item.getItem());

				if (arrow != null)
				{
					inventory.setPaperdollItem(PAPERDOLL_LHAND, arrow);
				}
			}
			else if (item.getItemType() == WeaponType.CROSSBOW)
			{
				final ItemInstance bolts = inventory.findBoltForCrossBow(item.getItem());

				if (bolts != null)
				{
					inventory.setPaperdollItem(PAPERDOLL_LHAND, bolts);
				}
			}
		}
	}

	private static final class StatsListener implements PaperdollListener
	{
		private static StatsListener instance = new StatsListener();

		public static StatsListener getInstance()
		{
			return instance;
		}

		@Override
		public void notifyUnequiped(int slot, ItemInstance item, Inventory inventory)
		{
			inventory.getOwner().removeStatsOwner(item);
		}

		@Override
		public void notifyEquiped(int slot, ItemInstance item, Inventory inventory)
		{
			inventory.getOwner().addStatFuncs(item.getStatFuncs(inventory.getOwner()));
		}
	}

	private static final class ItemSkillsListener implements PaperdollListener
	{
		private static ItemSkillsListener instance = new ItemSkillsListener();

		public static ItemSkillsListener getInstance()
		{
			return instance;
		}

		@Override
		public void notifyUnequiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (!(inventory.getOwner() instanceof Player))
			{
				return;
			}
			
			final Player player = (Player) inventory.getOwner();
			
			Skill enchant4Skill, itemSkill;
			final Item it = item.getItem();
			boolean update = false;
			boolean updateTimeStamp = false;
			
			if (item.isAugmented())
			{
				item.getAugmentation().removeBonus(player);
			}

			item.unChargeAllShots();
			item.removeElementAttrBonus(player);

			if (item.getEnchantLevel() >= 4)
			{
				enchant4Skill = it.getEnchant4Skill();

				if (enchant4Skill != null)
				{
					player.removeSkill(enchant4Skill, false, enchant4Skill.isPassive());
					update = true;
				}
			}

			item.clearEnchantStats();

			final SkillHolder[] skills = it.getSkills();

			if (skills != null)
			{
				for (final SkillHolder skillInfo : skills)
				{
					if (skillInfo == null)
					{
						continue;
					}

					itemSkill = skillInfo.getSkill();

					if (itemSkill != null)
					{
						player.removeSkill(itemSkill, false, itemSkill.isPassive());
						update = true;
					}
					else
					{
						_log.warn("Inventory.ItemSkillsListener.Weapon: Incorrect skill: " + skillInfo + ".");
					}
				}
			}

			if (item.isArmor())
			{
				for (final ItemInstance itm : inventory.getItems())
				{
					if (itm == null)
					{
						continue;
					}
					
					if (!itm.isEquipped() || (itm.getItem().getSkills() == null))
					{
						continue;
					}
					
					for (final SkillHolder sk : itm.getItem().getSkills())
					{
						if (player.getSkillLevel(sk.getId()) != -1)
						{
							continue;
						}

						itemSkill = sk.getSkill();

						if (itemSkill != null)
						{
							player.addSkill(itemSkill, false);
							itemSkill.setItemSkill(true);
							
							if (itemSkill.isActive())
							{
								if (!player.hasSkillReuse(itemSkill.getReuseHashCode()))
								{
									final int equipDelay = item.getEquipReuseDelay();
									if (equipDelay > 0)
									{
										player.addTimeStamp(itemSkill, equipDelay);
										player.disableSkill(itemSkill, equipDelay);
									}
								}
								updateTimeStamp = true;
							}
							update = true;
						}
					}
				}
			}
			
			if (item.isShadowItem())
			{
				item.stopManaConsumeTask();
			}
			
			final Skill unequipSkill = it.getUnequipSkill();
			if (unequipSkill != null)
			{
				final ISkillHandler handler = SkillHandler.getInstance().getHandler(unequipSkill.getSkillType());
				final Player[] targets =
				{
				        player
				};

				if (handler != null)
				{
					handler.useSkill(player, unequipSkill, targets);
				}
				else
				{
					unequipSkill.useSkill(player, targets);
				}
			}

			if (update)
			{
				player.sendSkillList(updateTimeStamp);
			}
		}

		@Override
		public void notifyEquiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (!(inventory.getOwner() instanceof Player))
			{
				return;
			}

			final Player player = (Player) inventory.getOwner();

			Skill enchant4Skill, itemSkill;
			final Item it = item.getItem();
			boolean update = false;
			boolean updateTimeStamp = false;
			
			if (item.isAugmented())
			{
				item.getAugmentation().applyBonus(player);
			}

			item.rechargeShots(true, true);
			item.updateElementAttrBonus(player);

			if (item.getEnchantLevel() >= 4)
			{
				enchant4Skill = it.getEnchant4Skill();

				if (enchant4Skill != null)
				{
					player.addSkill(enchant4Skill, false);
					enchant4Skill.setItemSkill(true);
					update = true;
				}
			}
			item.applyEnchantStats();

			final SkillHolder[] skills = it.getSkills();

			if (skills != null)
			{
				for (final SkillHolder skillInfo : skills)
				{
					if (skillInfo == null)
					{
						continue;
					}

					itemSkill = skillInfo.getSkill();

					if (itemSkill != null)
					{
						player.addSkill(itemSkill, false);
						itemSkill.setItemSkill(true);

						if (itemSkill.isActive())
						{
							if (!player.hasSkillReuse(itemSkill.getReuseHashCode()))
							{
								final int equipDelay = item.getEquipReuseDelay();
								if (equipDelay > 0)
								{
									player.addTimeStamp(itemSkill, equipDelay);
									player.disableSkill(itemSkill, equipDelay);
								}
							}
							updateTimeStamp = true;
						}
						update = true;
					}
					else
					{
						_log.warn("Inventory.ItemSkillsListener.Weapon: Incorrect skill: " + skillInfo + ".");
					}
				}
			}

			if (update)
			{
				player.sendSkillList(updateTimeStamp);
			}
		}
	}

	private static final class ArmorSetListener implements PaperdollListener
	{
		private static ArmorSetListener instance = new ArmorSetListener();

		public static ArmorSetListener getInstance()
		{
			return instance;
		}

		@Override
		public void notifyEquiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (!(inventory.getOwner().isPlayer()))
			{
				return;
			}

			final Player player = (Player) inventory.getOwner();

			final ItemInstance chestItem = inventory.getPaperdollItem(PAPERDOLL_CHEST);
			if (chestItem == null)
			{
				return;
			}
			
			if (player.getInventory().hasAllDressMeItemsEquipped())
			{
				player.getInventory().setMustShowDressMe(true);
			}

			if (!ArmorSetsParser.getInstance().isArmorSet(chestItem.getId()))
			{
				return;
			}
			final ArmorSetTemplate armorSet = ArmorSetsParser.getInstance().getSet(chestItem.getId());
			boolean update = false;
			boolean updateTimeStamp = false;

			if (armorSet.containItem(slot, item.getId()))
			{
				if (armorSet.containAll(player))
				{
					Skill itemSkill;
					final List<SkillHolder> skills = armorSet.getSkills();

					if (skills != null)
					{
						for (final SkillHolder holder : skills)
						{
							itemSkill = holder.getSkill();
							if (itemSkill != null)
							{
								player.addSkill(itemSkill, false);
								itemSkill.setItemSkill(true);

								if (itemSkill.isActive())
								{
									if (!player.hasSkillReuse(itemSkill.getReuseHashCode()))
									{
										final int equipDelay = item.getEquipReuseDelay();
										if (equipDelay > 0)
										{
											player.addTimeStamp(itemSkill, equipDelay);
											player.disableSkill(itemSkill, equipDelay);
										}
									}
									updateTimeStamp = true;
								}
								update = true;
							}
							else
							{
								_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
							}
						}
					}

					if (armorSet.containShield(player))
					{
						for (final SkillHolder holder : armorSet.getShieldSkillId())
						{
							if (holder.getSkill() != null)
							{
								player.addSkill(holder.getSkill(), false);
								holder.getSkill().setItemSkill(true);
								update = true;
							}
							else
							{
								_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
							}
						}
					}

					if (armorSet.isEnchanted6(player))
					{
						for (final SkillHolder holder : armorSet.getEnchant6skillId())
						{
							if (holder.getSkill() != null)
							{
								player.addSkill(holder.getSkill(), false);
								holder.getSkill().setItemSkill(true);
								update = true;
							}
							else
							{
								_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
							}
						}
					}
					
					if (!armorSet.getEnchantByLevel().isEmpty())
					{
						for (final int enchLvl : armorSet.getEnchantByLevel().keySet())
						{
							if (armorSet.isEnchantedByLevel(player, enchLvl))
							{
								final SkillHolder holder = armorSet.getEnchantByLevel().get(enchLvl);
								if (holder.getSkill() != null)
								{
									player.addSkill(holder.getSkill(), false);
									holder.getSkill().setItemSkill(true);
									update = true;
								}
								else
								{
									_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
								}
							}
						}
					}
				}
			}
			else if (armorSet.containShield(item.getId()))
			{
				for (final SkillHolder holder : armorSet.getShieldSkillId())
				{
					if (holder.getSkill() != null)
					{
						player.addSkill(holder.getSkill(), false);
						holder.getSkill().setItemSkill(true);
						update = true;
					}
					else
					{
						_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
					}
				}
			}

			if (update)
			{
				player.sendSkillList(updateTimeStamp);
			}
			player.broadcastUserInfo(true);
		}

		@Override
		public void notifyUnequiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (!(inventory.getOwner().isPlayer()))
			{
				return;
			}

			final Player player = (Player) inventory.getOwner();

			boolean remove = false;
			Skill itemSkill;
			List<SkillHolder> skills = null;
			List<SkillHolder> shieldSkill = null;
			List<SkillHolder> skillId6 = null;
			Map<Integer, SkillHolder> skillIdByLevel = null;
			
			if (player.getInventory().mustShowDressMe() && !player.getInventory().hasAllDressMeItemsEquipped())
			{
				player.getInventory().setMustShowDressMe(false);
			}

			if (slot == PAPERDOLL_CHEST)
			{
				if (!ArmorSetsParser.getInstance().isArmorSet(item.getId()))
				{
					player.broadcastUserInfo(true);
					return;
				}
				final ArmorSetTemplate armorSet = ArmorSetsParser.getInstance().getSet(item.getId());
				remove = true;
				skills = armorSet.getSkills();
				shieldSkill = armorSet.getShieldSkillId();
				skillId6 = armorSet.getEnchant6skillId();
				skillIdByLevel = armorSet.getEnchantByLevel();
			}
			else
			{
				final ItemInstance chestItem = inventory.getPaperdollItem(PAPERDOLL_CHEST);
				if (chestItem == null)
				{
					player.broadcastUserInfo(true);
					return;
				}

				final ArmorSetTemplate armorSet = ArmorSetsParser.getInstance().getSet(chestItem.getId());
				if (armorSet == null)
				{
					player.broadcastUserInfo(true);
					return;
				}

				if (armorSet.containItem(slot, item.getId()))
				{
					remove = true;
					skills = armorSet.getSkills();
					shieldSkill = armorSet.getShieldSkillId();
					skillId6 = armorSet.getEnchant6skillId();
					skillIdByLevel = armorSet.getEnchantByLevel();
				}
				else if (armorSet.containShield(item.getId()))
				{
					remove = true;
					shieldSkill = armorSet.getShieldSkillId();
				}
			}

			if (remove)
			{
				if (skills != null)
				{
					for (final SkillHolder holder : skills)
					{
						itemSkill = holder.getSkill();
						if (itemSkill != null)
						{
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						}
						else
						{
							_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
						}
					}
				}

				if (shieldSkill != null)
				{
					for (final SkillHolder holder : shieldSkill)
					{
						itemSkill = holder.getSkill();
						if (itemSkill != null)
						{
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						}
						else
						{
							_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
						}
					}
				}

				if (skillId6 != null)
				{
					for (final SkillHolder holder : skillId6)
					{
						itemSkill = holder.getSkill();
						if (itemSkill != null)
						{
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						}
						else
						{
							_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
						}
					}
				}
				
				if ((skillIdByLevel != null) && !skillIdByLevel.isEmpty())
				{
					for (final int enchLvl : skillIdByLevel.keySet())
					{
						itemSkill = skillIdByLevel.get(enchLvl).getSkill();
						if (itemSkill != null)
						{
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						}
						else
						{
							_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + skillIdByLevel.get(enchLvl) + ".");
						}
					}
				}

				player.checkItemRestriction();
				player.sendSkillList(false);
			}
			player.broadcastUserInfo(true);
		}
	}

	private static final class BraceletListener implements PaperdollListener
	{
		private static BraceletListener instance = new BraceletListener();

		public static BraceletListener getInstance()
		{
			return instance;
		}

		@Override
		public void notifyUnequiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (item.getItem().getBodyPart() == Item.SLOT_R_BRACELET)
			{
				inventory.unEquipItemInSlot(PAPERDOLL_DECO1);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO2);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO3);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO4);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO5);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO6);
			}
		}

		@Override
		public void notifyEquiped(int slot, ItemInstance item, Inventory inventory)
		{
			if (!(inventory.getOwner() instanceof Player))
			{
				return;
			}
			
			final Player player = (Player) inventory.getOwner();
			if (item.getItem().isAccessory() || item.getItem().isTalisman() || item.getItem().isBracelet())
			{
				player.sendUserInfo(true);
			}
			else
			{
				player.broadcastCharInfo();
			}
		}
	}

	protected Inventory()
	{
		_paperdoll = new ItemInstance[PAPERDOLL_TOTALSLOTS];
		_paperdollListeners = new ArrayList<>();

		if (this instanceof PcInventory)
		{
			addPaperdollListener(ArmorSetListener.getInstance());
			addPaperdollListener(BowCrossRodListener.getInstance());
			addPaperdollListener(ItemSkillsListener.getInstance());
			addPaperdollListener(BraceletListener.getInstance());
		}
		addPaperdollListener(StatsListener.getInstance());

	}

	protected abstract ItemLocation getEquipLocation();

	public ChangeRecorder newRecorder()
	{
		return new ChangeRecorder(this);
	}

	public ItemInstance dropItem(String process, ItemInstance item, Player actor, Object reference)
	{
		if (item == null)
		{
			return null;
		}

		synchronized (item)
		{
			if (!_items.contains(item))
			{
				return null;
			}

			removeItem(item);
			item.setOwnerId(process, 0, actor, reference);
			item.setItemLocation(ItemLocation.VOID);
			item.setLastChange(ItemInstance.REMOVED);

			item.updateDatabase();
			refreshWeight();
		}
		return item;
	}

	public ItemInstance dropItem(String process, int objectId, long count, Player actor, Object reference)
	{
		ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
		{
			return null;
		}

		synchronized (item)
		{
			if (!_items.contains(item))
			{
				return null;
			}

			if (item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(ItemInstance.MODIFIED);
				item.updateDatabase();

				item = ItemsParser.getInstance().createItem(process, item.getId(), count, actor, reference);
				item.updateDatabase();
				refreshWeight();
				return item;
			}
		}
		return dropItem(process, item, actor, reference);
	}

	@Override
	protected void addItem(ItemInstance item)
	{
		super.addItem(item);
		if (item.isEquipped())
		{
			equipItem(item);
		}
	}

	@Override
	public boolean removeItem(ItemInstance item)
	{
		for (int i = 0; i < _paperdoll.length; i++)
		{
			if (_paperdoll[i] == item)
			{
				unEquipItemInSlot(i);
			}
		}
		return super.removeItem(item);
	}

	public ItemInstance getPaperdollItem(int slot)
	{
		return _paperdoll[slot];
	}
	
	public int getPaperdollItem(ItemInstance item)
	{
		for (int i = 0; i < _paperdoll.length; i++)
		{
			if (_paperdoll[i] == item)
			{
				return i;
			}
		}
		return -1;
	}

	public ItemInstance[] getPaperdollItems()
	{
		return _paperdoll;
	}

	public boolean isPaperdollSlotEmpty(int slot)
	{
		return _paperdoll[slot] == null;
	}

	public static int getPaperdollIndex(int slot)
	{
		switch (slot)
		{
			case Item.SLOT_UNDERWEAR :
				return PAPERDOLL_UNDER;
			case Item.SLOT_R_EAR :
				return PAPERDOLL_REAR;
			case Item.SLOT_LR_EAR :
			case Item.SLOT_L_EAR :
				return PAPERDOLL_LEAR;
			case Item.SLOT_NECK :
				return PAPERDOLL_NECK;
			case Item.SLOT_R_FINGER :
			case Item.SLOT_LR_FINGER :
				return PAPERDOLL_RFINGER;
			case Item.SLOT_L_FINGER :
				return PAPERDOLL_LFINGER;
			case Item.SLOT_HEAD :
				return PAPERDOLL_HEAD;
			case Item.SLOT_R_HAND :
			case Item.SLOT_LR_HAND :
				return PAPERDOLL_RHAND;
			case Item.SLOT_L_HAND :
				return PAPERDOLL_LHAND;
			case Item.SLOT_GLOVES :
				return PAPERDOLL_GLOVES;
			case Item.SLOT_CHEST :
			case Item.SLOT_FULL_ARMOR :
			case Item.SLOT_ALLDRESS :
				return PAPERDOLL_CHEST;
			case Item.SLOT_LEGS :
				return PAPERDOLL_LEGS;
			case Item.SLOT_FEET :
				return PAPERDOLL_FEET;
			case Item.SLOT_BACK :
				return PAPERDOLL_CLOAK;
			case Item.SLOT_HAIR :
			case Item.SLOT_HAIRALL :
				return PAPERDOLL_HAIR;
			case Item.SLOT_HAIR2 :
				return PAPERDOLL_HAIR2;
			case Item.SLOT_R_BRACELET :
				return PAPERDOLL_RBRACELET;
			case Item.SLOT_L_BRACELET :
				return PAPERDOLL_LBRACELET;
			case Item.SLOT_DECO :
				return PAPERDOLL_DECO1;
			case Item.SLOT_BELT :
				return PAPERDOLL_BELT;
		}
		return -1;
	}

	public ItemInstance getPaperdollItemByL2ItemId(int slot)
	{
		final int index = getPaperdollIndex(slot);
		if (index == -1)
		{
			return null;
		}
		return _paperdoll[index];
	}

	public int getPaperdollItemId(int slot)
	{
		final ItemInstance item = _paperdoll[slot];
		if (item != null)
		{
			return item.getId();
		}
		return 0;
	}
	
	public int getPaperdollVisualItemId(int slot)
	{
		ItemInstance item = _paperdoll[slot];
		if (item != null)
		{
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

	public int getPaperdollItemDisplayId(int slot)
	{
		final ItemInstance item = _paperdoll[slot];
		return (item != null) ? item.getDisplayId() : 0;
	}

	public int getPaperdollAugmentationId(int slot)
	{
		final ItemInstance item = _paperdoll[slot];
		return ((item != null) && (item.getAugmentation() != null)) ? item.getAugmentation().getAugmentationId() : 0;
	}

	public int getPaperdollObjectId(int slot)
	{
		final ItemInstance item = _paperdoll[slot];
		return (item != null) ? item.getObjectId() : 0;
	}

	public synchronized void addPaperdollListener(PaperdollListener listener)
	{
		assert !_paperdollListeners.contains(listener);
		_paperdollListeners.add(listener);
	}

	public synchronized void removePaperdollListener(PaperdollListener listener)
	{
		_paperdollListeners.remove(listener);
	}

	public synchronized ItemInstance setPaperdollItem(int slot, ItemInstance item)
	{
		final ItemInstance old = _paperdoll[slot];
		if (old != item)
		{
			if (old != null)
			{
				if (getOwner().isPlayer())
				{
					if (old.isEventItem() && getOwner().getActingPlayer().isInFightEvent())
					{
						return old;
					}
				}
				_paperdoll[slot] = null;

				old.setItemLocation(getBaseLocation());
				old.setLastChange(ItemInstance.MODIFIED);

				int mask = 0;
				for (int i = 0; i < PAPERDOLL_TOTALSLOTS; i++)
				{
					final ItemInstance pi = _paperdoll[i];
					if (pi != null)
					{
						mask |= pi.getItem().getItemMask();
					}
				}
				_wearedMask = mask;

				for (final PaperdollListener listener : _paperdollListeners)
				{
					if (listener == null)
					{
						continue;
					}

					listener.notifyUnequiped(slot, old, this);
				}
				old.updateDatabase();
				
				if (getOwner() != null && getOwner().isPlayer())
				{
					getOwner().getActingPlayer().getListeners().onItemUnEquipListener(old);
				}
			}

			if (item != null)
			{
				_paperdoll[slot] = item;
				item.setItemLocation(getEquipLocation(), slot);
				item.setLastChange(ItemInstance.MODIFIED);
				_wearedMask |= item.getItem().getItemMask();
				for (final PaperdollListener listener : _paperdollListeners)
				{
					if (listener == null)
					{
						continue;
					}

					listener.notifyEquiped(slot, item, this);
				}
				item.updateDatabase();
				if (getOwner() != null && getOwner().isPlayer())
				{
					getOwner().getActingPlayer().getListeners().onItemEquipListener(item);
				}
			}
		}
		return old;
	}

	public int getWearedMask()
	{
		return _wearedMask;
	}

	public int getSlotFromItem(ItemInstance item)
	{
		int slot = -1;
		final int location = item.getLocationSlot();
		switch (location)
		{
			case PAPERDOLL_UNDER :
				slot = Item.SLOT_UNDERWEAR;
				break;
			case PAPERDOLL_LEAR :
				slot = Item.SLOT_L_EAR;
				break;
			case PAPERDOLL_REAR :
				slot = Item.SLOT_R_EAR;
				break;
			case PAPERDOLL_NECK :
				slot = Item.SLOT_NECK;
				break;
			case PAPERDOLL_RFINGER :
				slot = Item.SLOT_R_FINGER;
				break;
			case PAPERDOLL_LFINGER :
				slot = Item.SLOT_L_FINGER;
				break;
			case PAPERDOLL_HAIR :
				slot = Item.SLOT_HAIR;
				break;
			case PAPERDOLL_HAIR2 :
				slot = Item.SLOT_HAIR2;
				break;
			case PAPERDOLL_HEAD :
				slot = Item.SLOT_HEAD;
				break;
			case PAPERDOLL_RHAND :
				slot = Item.SLOT_R_HAND;
				break;
			case PAPERDOLL_LHAND :
				slot = Item.SLOT_L_HAND;
				break;
			case PAPERDOLL_GLOVES :
				slot = Item.SLOT_GLOVES;
				break;
			case PAPERDOLL_CHEST :
				slot = item.getItem().getBodyPart();
				break;
			case PAPERDOLL_LEGS :
				slot = Item.SLOT_LEGS;
				break;
			case PAPERDOLL_CLOAK :
				slot = Item.SLOT_BACK;
				break;
			case PAPERDOLL_FEET :
				slot = Item.SLOT_FEET;
				break;
			case PAPERDOLL_LBRACELET :
				slot = Item.SLOT_L_BRACELET;
				break;
			case PAPERDOLL_RBRACELET :
				slot = Item.SLOT_R_BRACELET;
				break;
			case PAPERDOLL_DECO1 :
			case PAPERDOLL_DECO2 :
			case PAPERDOLL_DECO3 :
			case PAPERDOLL_DECO4 :
			case PAPERDOLL_DECO5 :
			case PAPERDOLL_DECO6 :
				slot = Item.SLOT_DECO;
				break;
			case PAPERDOLL_BELT :
				slot = Item.SLOT_BELT;
				break;
		}
		return slot;
	}

	public void unEquipItem(ItemInstance item)
	{
		if (getOwner().isPlayer())
		{
			if (item.isEventItem() && getOwner().getActingPlayer().isInFightEvent())
			{
				return;
			}
		}
		
		if (item.isEquipped())
		{
			unEquipItemInBodySlot(item.getItem().getBodyPart());
		}
	}
	
	public void unEquipEventItem(ItemInstance item)
	{
		if (item.isEquipped())
		{
			unEquipItemInBodySlot(item.getItem().getBodyPart());
		}
	}

	public ItemInstance[] unEquipItemInBodySlotAndRecord(int slot)
	{
		final Inventory.ChangeRecorder recorder = newRecorder();

		try
		{
			unEquipItemInBodySlot(slot);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}

	public ItemInstance unEquipItemInSlot(int pdollSlot)
	{
		return setPaperdollItem(pdollSlot, null);
	}

	public ItemInstance[] unEquipItemInSlotAndRecord(int slot)
	{
		final Inventory.ChangeRecorder recorder = newRecorder();

		try
		{
			unEquipItemInSlot(slot);
			if (getOwner().isPlayer())
			{
				((Player) getOwner()).refreshExpertisePenalty();
			}
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}

	public ItemInstance unEquipItemInBodySlot(int slot)
	{
		if (Config.DEBUG)
		{
			_log.info(Inventory.class.getSimpleName() + ": Unequip body slot:" + slot);
		}

		int pdollSlot = -1;

		switch (slot)
		{
			case Item.SLOT_L_EAR :
				pdollSlot = PAPERDOLL_LEAR;
				break;
			case Item.SLOT_R_EAR :
				pdollSlot = PAPERDOLL_REAR;
				break;
			case Item.SLOT_NECK :
				pdollSlot = PAPERDOLL_NECK;
				break;
			case Item.SLOT_R_FINGER :
				pdollSlot = PAPERDOLL_RFINGER;
				break;
			case Item.SLOT_L_FINGER :
				pdollSlot = PAPERDOLL_LFINGER;
				break;
			case Item.SLOT_HAIR :
				pdollSlot = PAPERDOLL_HAIR;
				break;
			case Item.SLOT_HAIR2 :
				pdollSlot = PAPERDOLL_HAIR2;
				break;
			case Item.SLOT_HAIRALL :
				setPaperdollItem(PAPERDOLL_HAIR, null);
				pdollSlot = PAPERDOLL_HAIR;
				break;
			case Item.SLOT_HEAD :
				pdollSlot = PAPERDOLL_HEAD;
				break;
			case Item.SLOT_R_HAND :
			case Item.SLOT_LR_HAND :
				pdollSlot = PAPERDOLL_RHAND;
				break;
			case Item.SLOT_L_HAND :
				pdollSlot = PAPERDOLL_LHAND;
				break;
			case Item.SLOT_GLOVES :
				pdollSlot = PAPERDOLL_GLOVES;
				break;
			case Item.SLOT_CHEST :
			case Item.SLOT_ALLDRESS :
			case Item.SLOT_FULL_ARMOR :
				pdollSlot = PAPERDOLL_CHEST;
				break;
			case Item.SLOT_LEGS :
				pdollSlot = PAPERDOLL_LEGS;
				break;
			case Item.SLOT_BACK :
				pdollSlot = PAPERDOLL_CLOAK;
				break;
			case Item.SLOT_FEET :
				pdollSlot = PAPERDOLL_FEET;
				break;
			case Item.SLOT_UNDERWEAR :
				pdollSlot = PAPERDOLL_UNDER;
				break;
			case Item.SLOT_L_BRACELET :
				pdollSlot = PAPERDOLL_LBRACELET;
				break;
			case Item.SLOT_R_BRACELET :
				pdollSlot = PAPERDOLL_RBRACELET;
				break;
			case Item.SLOT_DECO :
				pdollSlot = PAPERDOLL_DECO1;
				break;
			case Item.SLOT_BELT :
				pdollSlot = PAPERDOLL_BELT;
				break;
			default :
				if (Config.DEBUG)
				{
					_log.info("Unhandled slot type: " + slot);
					_log.info(StringUtil.getTraceString(Thread.currentThread().getStackTrace()));
				}
		}
		if (pdollSlot >= 0)
		{
			final ItemInstance old = setPaperdollItem(pdollSlot, null);
			if (old != null)
			{
				if (getOwner().isPlayer())
				{
					((Player) getOwner()).refreshExpertisePenalty();
				}
			}
			return old;
		}
		return null;
	}

	public ItemInstance[] equipItemAndRecord(ItemInstance item)
	{
		final Inventory.ChangeRecorder recorder = newRecorder();

		try
		{
			equipItem(item);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}

	public void equipItem(ItemInstance item)
	{
		if ((getOwner().isPlayer()) && (((Player) getOwner()).getPrivateStoreType() != Player.STORE_PRIVATE_NONE))
		{
			return;
		}

		if (getOwner().isPlayer())
		{
			final Player player = (Player) getOwner();
			if (item.isEventItem() && !player.isInFightEvent())
			{
				return;
			}
			
			if (!player.canOverrideCond(PcCondOverride.ITEM_CONDITIONS) && !player.isHero() && item.isHeroItem())
			{
				return;
			}
		}

		final int targetSlot = item.getItem().getBodyPart();

		final ItemInstance formal = getPaperdollItem(PAPERDOLL_CHEST);
		if ((item.getId() != 21163) && (formal != null) && (formal.getItem().getBodyPart() == Item.SLOT_ALLDRESS))
		{
			if (formal.getItem().isCostume())
			{
				switch (targetSlot)
				{
					case Item.SLOT_LEGS :
					case Item.SLOT_FEET :
					case Item.SLOT_GLOVES :
					case Item.SLOT_HEAD :
						return;
				}
			}
			else
			{
				switch (targetSlot)
				{
					case Item.SLOT_LR_HAND :
					case Item.SLOT_L_HAND :
					case Item.SLOT_R_HAND :
					case Item.SLOT_LEGS :
					case Item.SLOT_FEET :
					case Item.SLOT_GLOVES :
					case Item.SLOT_HEAD :
						return;
				}
			}
		}

		switch (targetSlot)
		{
			case Item.SLOT_LR_HAND :
			{
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case Item.SLOT_L_HAND :
			{
				final ItemInstance rh = getPaperdollItem(PAPERDOLL_RHAND);
				if ((rh != null) && (rh.getItem().getBodyPart() == Item.SLOT_LR_HAND) && !(((rh.getItemType() == WeaponType.BOW) && (item.getItemType() == EtcItemType.ARROW)) || ((rh.getItemType() == WeaponType.CROSSBOW) && (item.getItemType() == EtcItemType.BOLT)) || ((rh.getItemType() == WeaponType.FISHINGROD) && (item.getItemType() == EtcItemType.LURE))))
				{
					setPaperdollItem(PAPERDOLL_RHAND, null);
				}

				setPaperdollItem(PAPERDOLL_LHAND, item);
				break;
			}
			case Item.SLOT_R_HAND :
			{
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case Item.SLOT_L_EAR :
			case Item.SLOT_R_EAR :
			case Item.SLOT_LR_EAR :
			{
				if (_paperdoll[PAPERDOLL_LEAR] == null)
				{
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				else if (_paperdoll[PAPERDOLL_REAR] == null)
				{
					setPaperdollItem(PAPERDOLL_REAR, item);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				break;
			}
			case Item.SLOT_L_FINGER :
			case Item.SLOT_R_FINGER :
			case Item.SLOT_LR_FINGER :
			{
				if (_paperdoll[PAPERDOLL_LFINGER] == null)
				{
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				else if (_paperdoll[PAPERDOLL_RFINGER] == null)
				{
					setPaperdollItem(PAPERDOLL_RFINGER, item);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				break;
			}
			case Item.SLOT_NECK :
				setPaperdollItem(PAPERDOLL_NECK, item);
				break;
			case Item.SLOT_FULL_ARMOR :
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			case Item.SLOT_CHEST :
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			case Item.SLOT_LEGS :
			{
				final ItemInstance chest = getPaperdollItem(PAPERDOLL_CHEST);
				if ((chest != null) && (chest.getItem().getBodyPart() == Item.SLOT_FULL_ARMOR))
				{
					setPaperdollItem(PAPERDOLL_CHEST, null);
				}

				setPaperdollItem(PAPERDOLL_LEGS, item);
				break;
			}
			case Item.SLOT_FEET :
				setPaperdollItem(PAPERDOLL_FEET, item);
				break;
			case Item.SLOT_GLOVES :
				setPaperdollItem(PAPERDOLL_GLOVES, item);
				break;
			case Item.SLOT_HEAD :
				setPaperdollItem(PAPERDOLL_HEAD, item);
				break;
			case Item.SLOT_HAIR :
				final ItemInstance hair = getPaperdollItem(PAPERDOLL_HAIR);
				if ((hair != null) && (hair.getItem().getBodyPart() == Item.SLOT_HAIRALL))
				{
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
				}

				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			case Item.SLOT_HAIR2 :
				final ItemInstance hair2 = getPaperdollItem(PAPERDOLL_HAIR);
				if ((hair2 != null) && (hair2.getItem().getBodyPart() == Item.SLOT_HAIRALL))
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}

				setPaperdollItem(PAPERDOLL_HAIR2, item);
				break;
			case Item.SLOT_HAIRALL :
				setPaperdollItem(PAPERDOLL_HAIR2, null);
				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			case Item.SLOT_UNDERWEAR :
				setPaperdollItem(PAPERDOLL_UNDER, item);
				break;
			case Item.SLOT_BACK :
				setPaperdollItem(PAPERDOLL_CLOAK, item);
				break;
			case Item.SLOT_L_BRACELET :
				setPaperdollItem(PAPERDOLL_LBRACELET, item);
				break;
			case Item.SLOT_R_BRACELET :
				setPaperdollItem(PAPERDOLL_RBRACELET, item);
				break;
			case Item.SLOT_DECO :
				equipTalisman(item);
				break;
			case Item.SLOT_BELT :
				setPaperdollItem(PAPERDOLL_BELT, item);
				break;
			case Item.SLOT_ALLDRESS :
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null);
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_HEAD, null);
				setPaperdollItem(PAPERDOLL_FEET, null);
				setPaperdollItem(PAPERDOLL_GLOVES, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			default :
				_log.warn("Unknown body slot " + targetSlot + " for Item ID:" + item.getId());
		}
	}

	@Override
	protected void refreshWeight()
	{
		long weight = 0;

		for (final ItemInstance item : _items)
		{
			if ((item != null) && (item.getItem() != null))
			{
				weight += item.getItem().getWeight() * item.getCount();
			}
		}
		_totalWeight = Math.max((int) Math.min(weight - getOwner().getBonusWeightPenalty(), Integer.MAX_VALUE), 0);
	}

	public int getTotalWeight()
	{
		return _totalWeight;
	}
	
	public void reduceAmmunitionCount(EtcItemType type)
	{
	}
	
	public boolean reduceShortsCount(ItemInstance item, int count)
	{
		return false;
	}

	public ItemInstance findArrowForBow(Item bow)
	{
		if (bow == null)
		{
			return null;
		}

		ItemInstance arrow = null;

		for (final ItemInstance item : getItems())
		{
			if (item.isEtcItem() && (item.getItem().getItemGradeSPlus() == bow.getItemGradeSPlus()) && (item.getEtcItem().getItemType() == EtcItemType.ARROW))
			{
				arrow = item;
				break;
			}
		}
		return arrow;
	}

	public ItemInstance findBoltForCrossBow(Item crossbow)
	{
		ItemInstance bolt = null;

		for (final ItemInstance item : getItems())
		{
			if (item.isEtcItem() && (item.getItem().getItemGradeSPlus() == crossbow.getItemGradeSPlus()) && (item.getEtcItem().getItemType() == EtcItemType.BOLT))
			{
				bolt = item;
				break;
			}
		}
		return bolt;
	}

	@Override
	public void restore()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet inv = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time, visual_itemId, agathion_energy, is_event FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY loc_data");
			statement.setInt(1, getOwnerId());
			statement.setString(2, getBaseLocation().name());
			statement.setString(3, getEquipLocation().name());
			inv = statement.executeQuery();

			ItemInstance item;
			while (inv.next())
			{
				item = ItemInstance.restoreFromDb(getOwnerId(), inv);
				if (item == null)
				{
					continue;
				}

				if (getOwner().isPlayer())
				{
					final Player player = (Player) getOwner();

					if (!player.canOverrideCond(PcCondOverride.ITEM_CONDITIONS) && !player.isHero() && item.isHeroItem())
					{
						item.setItemLocation(ItemLocation.INVENTORY);
					}
				}

				GameObjectsStorage.addItem(item);
				
				if (item.isStackable() && (getItemByItemId(item.getId()) != null))
				{
					addItem("Restore", item, getOwner().getActingPlayer(), null);
				}
				else
				{
					addItem(item);
				}
			}
			refreshWeight();
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore inventory: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, inv);
		}
	}

	public int getMaxTalismanCount()
	{
		return (int) getOwner().getStat().calcStat(Stats.TALISMAN_SLOTS, 0, null, null);
	}

	private void equipTalisman(ItemInstance item)
	{
		if (getMaxTalismanCount() == 0)
		{
			return;
		}

		for (int i = PAPERDOLL_DECO1; i < (PAPERDOLL_DECO1 + getMaxTalismanCount()); i++)
		{
			if (_paperdoll[i] != null)
			{
				if (getPaperdollItemId(i) == item.getId())
				{
					setPaperdollItem(i, item);
					return;
				}
			}
		}

		for (int i = PAPERDOLL_DECO1; i < (PAPERDOLL_DECO1 + getMaxTalismanCount()); i++)
		{
			if (_paperdoll[i] == null)
			{
				setPaperdollItem(i, item);
				return;
			}
		}
		setPaperdollItem(PAPERDOLL_DECO1, item);
	}

	public int getCloakStatus()
	{
		if (Config.ALLOW_OPEN_CLOAK_SLOT)
		{
			return 1;
		}
		return (int) getOwner().getStat().calcStat(Stats.CLOAK_SLOT, 0, null, null);
	}
	
	public int getHeroStatus()
	{
		return (int) getOwner().getStat().calcStat(Stats.HERO_STATUS, 0, null, null);
	}

	public void reloadEquippedItems()
	{
		int slot;
		for (final ItemInstance item : _paperdoll)
		{
			if (item == null)
			{
				continue;
			}

			slot = item.getLocationSlot();
			for (final PaperdollListener listener : _paperdollListeners)
			{
				if (listener == null)
				{
					continue;
				}

				listener.notifyUnequiped(slot, item, this);
				listener.notifyEquiped(slot, item, this);
			}
		}
	}
}