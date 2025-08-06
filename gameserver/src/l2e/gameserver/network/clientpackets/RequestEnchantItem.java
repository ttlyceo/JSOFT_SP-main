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
package l2e.gameserver.network.clientpackets;

import java.util.Arrays;

import l2e.commons.log.Log;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.EnchantItemParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Armor;
import l2e.gameserver.model.items.enchant.EnchantItem;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.EnchantResult;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestEnchantItem extends GameClientPacket
{
	private int _objectId = 0;
	private int _supportId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_supportId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final var activeChar = getClient().getActiveChar();
		if ((activeChar == null) || (_objectId == 0))
		{
			return;
		}
		
		activeChar.isntAfk();
		
		if (activeChar.isActionsDisabled())
		{
			activeChar.setActiveEnchantItemId(Player.ID_NONE);
			activeChar.sendActionFailed();
			return;
		}

		if (!activeChar.isOnline() || getClient().isDetached())
		{
			activeChar.setActiveEnchantItemId(Player.ID_NONE);
			return;
		}

		if (activeChar.isProcessingTransaction() || activeChar.isInStoreMode())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE);
			activeChar.setActiveEnchantItemId(Player.ID_NONE);
			return;
		}

		activeChar._useItemLock.writeLock().lock();
		try
		{
			final var item = activeChar.getInventory().getItemByObjectId(_objectId);
			var scroll = activeChar.getInventory().getItemByObjectId(activeChar.getActiveEnchantItemId());
			var support = activeChar.getInventory().getItemByObjectId(activeChar.getActiveEnchantSupportItemId());
			
			if ((item == null) || (scroll == null))
			{
				activeChar.setActiveEnchantItemId(Player.ID_NONE);
				return;
			}
			
			final var scrollTemplate = EnchantItemParser.getInstance().getEnchantScroll(scroll);
			if (scrollTemplate == null)
			{
				return;
			}
			
			EnchantItem supportTemplate = null;
			if (support != null)
			{
				if (support.getObjectId() != _supportId)
				{
					activeChar.setActiveEnchantItemId(Player.ID_NONE);
					return;
				}
				supportTemplate = EnchantItemParser.getInstance().getSupportItem(support);
			}
			
			if (!scrollTemplate.isValid(item, supportTemplate))
			{
				activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
				activeChar.setActiveEnchantItemId(Player.ID_NONE);
				activeChar.sendPacket(new EnchantResult(2, 0, 0));
				return;
			}
			
			scroll = activeChar.getInventory().destroyItem("Enchant", scroll.getObjectId(), 1, activeChar, item);
			if (scroll == null)
			{
				activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " tried to enchant with a scroll he doesn't have");
				activeChar.setActiveEnchantItemId(Player.ID_NONE);
				activeChar.sendPacket(new EnchantResult(2, 0, 0));
				return;
			}
			
			if (support != null)
			{
				support = activeChar.getInventory().destroyItem("Enchant", support.getObjectId(), 1, activeChar, item);
				if (support == null)
				{
					activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					Util.handleIllegalPlayerAction(activeChar, "" + activeChar.getName(null) + " tried to enchant with a support item he doesn't have");
					activeChar.setActiveEnchantItemId(Player.ID_NONE);
					activeChar.sendPacket(new EnchantResult(2, 0, 0));
					return;
				}
			}
			
			synchronized (item)
			{
				if ((item.getOwnerId() != activeChar.getObjectId()) || (item.isEnchantable() == 0))
				{
					activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
					activeChar.setActiveEnchantItemId(Player.ID_NONE);
					activeChar.sendPacket(new EnchantResult(2, 0, 0));
					return;
				}
				
				final var resultType = scrollTemplate.calculateSuccess(activeChar, item, supportTemplate);
				switch (resultType)
				{
					case ERROR :
					{
						activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
						activeChar.setActiveEnchantItemId(Player.ID_NONE);
						activeChar.sendPacket(new EnchantResult(2, 0, 0));
						break;
					}
					case SUCCESS :
					{
						final var isEnchantAnnounce = scrollTemplate.isEnchantAnnounce(activeChar, item, supportTemplate);
						Skill enchant4Skill = null;
						final var it = item.getItem();
						final int oldEnchant = item.getEnchantLevel();
						final int maxEnchant = scrollTemplate.getMaxEnchant();
						item.setEnchantLevel(Math.min(maxEnchant, (oldEnchant + scrollTemplate.getIncreaseEnchantValue())));
						item.updateDatabase();
						activeChar.sendPacket(new EnchantResult(0, 0, 0));
						
						if (item.getEnchantLevel() > 3)
						{
							if (scrollTemplate.isBlessed() || scrollTemplate.isSafe())
							{
								activeChar.getCounters().addAchivementInfo("enchantBlessedSucceeded", 0, -1, false, false, false);
							}
							else
							{
								activeChar.getCounters().addAchivementInfo("enchantNormalSucceeded", 0, -1, false, false, false);
							}
						}
						
						if (Config.LOG_ITEM_ENCHANTS)
						{
							Log.addLogEnchantItem("SUCCESS", item, activeChar, scroll);
						}
						
						if (item.isWeapon())
						{
							activeChar.getCounters().addAchivementInfo("enchantWeaponByLvl", item.getEnchantLevel(), -1, false, false, false);
						}
						else if (item.isJewel())
						{
							activeChar.getCounters().addAchivementInfo("enchantJewerlyByLvl", item.getEnchantLevel(), -1, false, false, false);
						}
						else
						{
							activeChar.getCounters().addAchivementInfo("enchantArmorByLvl", item.getEnchantLevel(), -1, false, false, false);
						}
						
						if (isEnchantAnnounce)
						{
							final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_SUCCESSFULY_ENCHANTED_A_S2_S3);
							sm.addCharName(activeChar);
							sm.addNumber(item.getEnchantLevel());
							sm.addItemName(item);
							activeChar.broadcastPacket(sm);
							
							final var skill = SkillsParser.FrequentSkill.FIREWORK.getSkill();
							if (skill != null)
							{
								activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
							}
						}
						
						if ((it instanceof Armor) && (item.getEnchantLevel() == 4) && activeChar.getInventory().getItemByObjectId(item.getObjectId()).isEquipped())
						{
							enchant4Skill = ((Armor) it).getEnchant4Skill();
							if (enchant4Skill != null)
							{
								activeChar.addSkill(enchant4Skill, false);
								activeChar.sendSkillList(false);
							}
						}
						activeChar.getListeners().onEnchantItem(item, oldEnchant, item.getEnchantLevel(), true, false);
						break;
					}
					case FAILURE :
					{
						final int oldEnchant = item.getEnchantLevel();
						if (scrollTemplate.isSafe())
						{
							activeChar.sendPacket(SystemMessageId.SAFE_ENCHANT_FAILED);
							activeChar.sendPacket(new EnchantResult(5, 0, 0));
							
							if (Config.LOG_ITEM_ENCHANTS)
							{
								Log.addLogEnchantItem("SAFE FAIL", item, activeChar, scroll);
							}
							activeChar.getListeners().onEnchantItem(item, oldEnchant, item.getEnchantLevel(), false, false);
						}
						else
						{
							if (item.isEquipped())
							{
								if (item.getEnchantLevel() > 0)
								{
									final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
									sm.addNumber(item.getEnchantLevel());
									sm.addItemName(item);
									activeChar.sendPacket(sm);
								}
								else
								{
									final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
									sm.addItemName(item);
									activeChar.sendPacket(sm);
								}
								
								final ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
								final InventoryUpdate iu = new InventoryUpdate();
								for (final ItemInstance itm : unequiped)
								{
									iu.addModifiedItem(itm);
								}
								
								activeChar.sendPacket(iu);
								activeChar.broadcastCharInfo();
							}
							
							if (scrollTemplate.isBlessed())
							{
								activeChar.sendPacket(SystemMessageId.BLESSED_ENCHANT_FAILED);
								
								if (Config.SYSTEM_BLESSED_ENCHANT && Arrays.binarySearch(Config.SAVE_ENCHANT_BLACKLIST, item.getId()) < 0)
								{
									item.setEnchantLevel(Config.BLESSED_ENCHANT_SAVE);
								}
								else
								{
									item.setEnchantLevel(0);
								}
								item.updateDatabase();
								activeChar.sendPacket(new EnchantResult(3, 0, 0));
								
								if (Config.LOG_ITEM_ENCHANTS)
								{
									Log.addLogEnchantItem("BLESSED FAIL", item, activeChar, scroll);
								}
								activeChar.getListeners().onEnchantItem(item, oldEnchant, item.getEnchantLevel(), false, false);
							}
							else
							{
								final int crystalId = item.getItem().getCrystalItemId();
								int count = item.getCrystalCount() - ((item.getItem().getCrystalCount() + 1) / 2);
								if (count < 1)
								{
									count = 1;
								}
								activeChar.getListeners().onEnchantItem(item, oldEnchant, 0, false, true);
								
								final var destroyItem = activeChar.getInventory().destroyItem("Enchant", item, activeChar, null);
								if (destroyItem == null)
								{
									Util.handleIllegalPlayerAction(activeChar, "Unable to delete item on enchant failure from player " + activeChar.getName(null) + ", possible cheater !");
									activeChar.setActiveEnchantItemId(Player.ID_NONE);
									activeChar.sendPacket(new EnchantResult(2, 0, 0));
									
									if (Config.LOG_ITEM_ENCHANTS)
									{
										Log.addLogEnchantItem("UNABLE DESTROY", item, activeChar, scroll);
									}
									return;
								}
								
								ItemInstance crystals = null;
								if (crystalId != 0)
								{
									crystals = activeChar.getInventory().addItem("Enchant", crystalId, count, activeChar, destroyItem);
									
									final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
									sm.addItemName(crystals);
									sm.addItemNumber(count);
									activeChar.sendPacket(sm);
								}
								
								if (!Config.FORCE_INVENTORY_UPDATE)
								{
									final var iu = new InventoryUpdate();
									if (destroyItem.getCount() == 0)
									{
										iu.addRemovedItem(destroyItem);
									}
									else
									{
										iu.addModifiedItem(destroyItem);
									}
									
									if (crystals != null)
									{
										iu.addItem(crystals);
									}
									
									if (scroll.getCount() == 0)
									{
										iu.addRemovedItem(scroll);
									}
									else
									{
										iu.addModifiedItem(scroll);
									}
									
									activeChar.sendPacket(iu);
								}
								else
								{
									activeChar.sendItemList(true);
								}
								
								if (crystalId == 0)
								{
									activeChar.sendPacket(new EnchantResult(4, 0, 0));
								}
								else
								{
									activeChar.sendPacket(new EnchantResult(1, crystalId, count));
								}
								
								if (Config.LOG_ITEM_ENCHANTS)
								{
									Log.addLogEnchantItem("FAIL", item, activeChar, scroll);
								}
							}
						}
						break;
					}
				}
			}
		}
		finally
		{
			activeChar._useItemLock.writeLock().unlock();
			activeChar.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
		}
		activeChar.sendItemList(false);
		activeChar.setActiveEnchantItemId(Player.ID_NONE);
		activeChar.broadcastCharInfo();
	}
}