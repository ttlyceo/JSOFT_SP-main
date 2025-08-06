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

import java.util.concurrent.TimeUnit;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.model.NextAction;
import l2e.gameserver.ai.model.NextAction.NextActionCallback;
import l2e.gameserver.handler.itemhandlers.ItemHandler;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.entity.events.tournaments.data.template.TournamentsEventsTemplate;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExGMViewQuestItemList;
import l2e.gameserver.network.serverpackets.ExUseSharedGroupItem;
import l2e.gameserver.network.serverpackets.GMHennaInfo;
import l2e.gameserver.network.serverpackets.GMViewItemList;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class RequestUseItem extends GameClientPacket
{
	private int _objectId;
	private boolean _ctrlPressed;
	private int _itemId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_ctrlPressed = readD() != 0;
	}
	
	@Override
	protected void runImpl()
	{
		final var activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		activeChar._useItemLock.writeLock().lock();
		try
		{
			if (activeChar.getActiveTradeList() != null)
			{
				activeChar.cancelActiveTrade();
			}

			var item = activeChar.getInventory().getItemByObjectId(_objectId);
			if (item == null)
			{
				if (activeChar.isGM() && activeChar.getTarget() != null && activeChar.getTarget().isPlayer())
				{
					item = activeChar.getTarget().getActingPlayer().getInventory().getItemByObjectId(_objectId);
					if (item == null)
					{
						activeChar.sendActionFailed();
						return;
					}
				}
				else
				{
					activeChar.sendActionFailed();
					return;
				}
			}

			if (activeChar.getPrivateStoreType() != Player.STORE_PRIVATE_NONE)
			{
				final var isInSellBuff = item.isPotion() && (activeChar.isSellingBuffs() || activeChar.isInCraftMode() || activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_MANUFACTURE);
				if (!isInSellBuff)
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
					activeChar.sendActionFailed();
					return;
				}
			}
			
			if (activeChar.isInsideZone(ZoneId.FUN_PVP))
			{
				final var zone = ZoneManager.getInstance().getZone(activeChar, FunPvpZone.class);
				if (zone != null && !zone.checkItem(item))
				{
					activeChar.sendMessage("You cannot use " + item.getName(null) + " inside this zone.");
					return;
				}
			}
			
			if (item.getItem().getType2() == Item.TYPE2_QUEST)
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_USE_QUEST_ITEMS);
				return;
			}
		
			if (activeChar.isStunned() || activeChar.isParalyzed() || activeChar.isSleeping() || activeChar.isAfraid() || activeChar.isAlikeDead())
			{
				return;
			}
		
			if (activeChar.isDead() || !activeChar.getInventory().canManipulateWithItemId(item.getId()))
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addItemName(item);
				activeChar.sendPacket(sm);
				return;
			}
		
			if (!activeChar.isGM() && !activeChar.isHero() && item.isHeroItem())
			{
				activeChar.sendMessage("Cannot use this item.");
				return;
			}

			var e = activeChar.getPartyTournament();
			if(e != null && !e.canUseItem(item))
			{
				activeChar.sendMessage("You can't use this item in Tournament.");
				return;
			}

			if (!item.isEquipped() && !item.getItem().checkCondition(activeChar, activeChar, true))
			{
				return;
			}
		
			_itemId = item.getId();
		
			if (activeChar.isFishing() && ((_itemId < 6535) || (_itemId > 6540)))
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
				return;
			}
		
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && (activeChar.getKarma() > 0))
			{
				final var skills = item.getItem().getSkills();
				if (skills != null)
				{
					for (final var sHolder : skills)
					{
						final var skill = sHolder.getSkill();
						if ((skill != null) && skill.hasEffectType(EffectType.TELEPORT))
						{
							return;
						}
					}
				}
			}
		
			final int reuseDelay = item.getReuseDelay();
			final int sharedReuseGroup = item.getSharedReuseGroup();
			if (reuseDelay > 0)
			{
				final long reuse = activeChar.getItemRemainingReuseTime(item.getObjectId());
				if (reuse > 0)
				{
					reuseData(activeChar, item);
					sendSharedGroupUpdate(activeChar, sharedReuseGroup, reuse, reuseDelay);
					return;
				}
			
				final long reuseOnGroup = activeChar.getReuseDelayOnGroup(sharedReuseGroup);
				if (reuseOnGroup > 0)
				{
					reuseData(activeChar, item);
					sendSharedGroupUpdate(activeChar, sharedReuseGroup, reuseOnGroup, reuseDelay);
					return;
				}
			}
		
			if (activeChar.getObjectId() == item.getOwnerId())
			{
				if (item.isEquipable())
				{
					if (activeChar.isCursedWeaponEquipped() && (_itemId == 6408))
					{
						return;
					}
					
					if (FortSiegeManager.getInstance().isCombat(_itemId))
					{
						return;
					}
					
					if (activeChar.isCombatFlagEquipped())
					{
						return;
					}
					
					switch (item.getItem().getBodyPart())
					{
						case Item.SLOT_LR_HAND :
						case Item.SLOT_L_HAND :
						case Item.SLOT_R_HAND :
						{
							if ((activeChar.getActiveWeaponItem() != null) && (activeChar.getActiveWeaponItem().getId() == 9819))
							{
								activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
								return;
							}
							
							if (activeChar.isMounted())
							{
								activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
								return;
							}
							if (activeChar.isDisarmed())
							{
								activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
								return;
							}
							
							if (activeChar.isCursedWeaponEquipped())
							{
								return;
							}
							break;
						}
						case Item.SLOT_DECO :
						{
							if (!item.isEquipped() && (activeChar.getInventory().getMaxTalismanCount() == 0))
							{
								activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
								return;
							}
						}
					}
					
					if (activeChar.isCastingNow() || activeChar.isCastingSimultaneouslyNow())
					{
						if (activeChar.getAI().getNextAction() != null)
						{
							activeChar.getAI().getNextAction().addCallback(new NextActionCallback()
							{
								@Override
								public void doWork()
								{
									activeChar.useEquippableItem(_objectId, true);
								}
							});
						}
						else
						{
							final var nextAction = new NextAction(CtrlEvent.EVT_FINISH_CASTING, CtrlIntention.CAST, new NextActionCallback()
							{
								@Override
								public void doWork()
								{
									activeChar.useEquippableItem(_objectId, true);
								}
							});
							activeChar.getAI().setNextAction(nextAction);
						}
					}
					else if (activeChar.isAttackingNow())
					{
						ThreadPoolManager.getInstance().schedule(() ->
						{
							activeChar.useEquippableItem(_objectId, false);
						}, (TimeUnit.MILLISECONDS.convert(activeChar.getAttackEndTime() - System.nanoTime(), TimeUnit.NANOSECONDS)));
					}
					else
					{
						activeChar.useEquippableItem(_objectId, true);
					}
				}
				else
				{
					if (activeChar.isCastingNow() && !(item.isPotion() || item.isElixir()))
					{
						return;
					}
					
					final var weaponItem = activeChar.getActiveWeaponItem();
					if (((weaponItem != null) && (weaponItem.getItemType() == WeaponType.FISHINGROD)) && (((_itemId >= 6519) && (_itemId <= 6527)) || ((_itemId >= 7610) && (_itemId <= 7613)) || ((_itemId >= 7807) && (_itemId <= 7809)) || ((_itemId >= 8484) && (_itemId <= 8486)) || (((_itemId >= 8505) && (_itemId <= 8513)) || (_itemId == 8548))))
					{
						activeChar.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
						activeChar.broadcastUserInfo(true);
						if (!Config.FORCE_INVENTORY_UPDATE)
						{
							final InventoryUpdate iu = new InventoryUpdate();
							iu.addModifiedItem(item);
							activeChar.sendInventoryUpdate(iu);
						}
						else
						{
							activeChar.sendItemList(false);
						}
						return;
					}
					
					final var etcItem = item.getEtcItem();
					final var handler = ItemHandler.getInstance().getHandler(etcItem);
					if (handler == null)
					{
						if ((etcItem != null) && (etcItem.getHandlerName() != null))
						{
							_log.warn("Unmanaged Item handler: " + etcItem.getHandlerName() + " for Item Id: " + _itemId + "!");
						}
						else if (Config.DEBUG)
						{
							_log.warn("No Item handler registered for Item Id: " + _itemId + "!");
						}
						return;
					}
					
					if (handler.useItem(activeChar, item, _ctrlPressed))
					{
						if (reuseDelay > 0)
						{
							activeChar.addTimeStampItem(item, reuseDelay, item.isReuseByCron());
							sendSharedGroupUpdate(activeChar, sharedReuseGroup, reuseDelay, reuseDelay);
						}
					}
				}
			}
			else
			{
				if (activeChar.isGM())
				{
					final var owner = GameObjectsStorage.getPlayer(item.getOwnerId());
					if (owner == null)
					{
						return;
					}
					
					final long count = owner.getInventory().getInventoryItemCount(item.getId(), -1, true);
					if (count > 0)
					{
						if (item.isEquipped())
						{
							final var unequiped = owner.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
							final var iu = new InventoryUpdate();
							for (final ItemInstance itm : unequiped)
							{
								iu.addModifiedItem(itm);
							}
							owner.sendInventoryUpdate(iu);
						}
						
						owner.destroyItem("Removed by GM", item.getObjectId(), count, owner, false);
						
						final var items = owner.getInventory().getItems();
						int questSize = 0;
						for (final var it : items)
						{
							if (it.isQuestItem())
							{
								questSize++;
							}
						}
						activeChar.sendPacket(new GMViewItemList(owner, items, items.length - questSize));
						activeChar.sendPacket(new ExGMViewQuestItemList(owner, items, questSize));
						activeChar.sendPacket(new GMHennaInfo(owner));
					}
				}
			}
		}
		finally
		{
			activeChar._useItemLock.writeLock().unlock();
		}
	}
	
	private void reuseData(Player activeChar, ItemInstance item)
	{
		SystemMessage sm = null;
		final long remainingTime = activeChar.getItemRemainingReuseTime(item.getObjectId());
		final int hours = (int) (remainingTime / 3600000L);
		final int minutes = (int) (remainingTime % 3600000L) / 60000;
		final int seconds = (int) ((remainingTime / 1000) % 60);
		if (hours > 0)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S2_HOURS_S3_MINUTES_S4_SECONDS_REMAINING_FOR_REUSE_S1);
			sm.addItemName(item);
			sm.addNumber(hours);
			sm.addNumber(minutes);
		}
		else if (minutes > 0)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S2_MINUTES_S3_SECONDS_REMAINING_FOR_REUSE_S1);
			sm.addItemName(item);
			sm.addNumber(minutes);
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S2_SECONDS_REMAINING_FOR_REUSE_S1);
			sm.addItemName(item);
		}
		sm.addNumber(seconds);
		activeChar.sendPacket(sm);
	}
	
	private void sendSharedGroupUpdate(Player activeChar, int group, long remaining, int reuse)
	{
		if (group > 0)
		{
			activeChar.sendPacket(new ExUseSharedGroupItem(_itemId, group, remaining, reuse));
		}
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return !Config.SPAWN_PROTECTION_ALLOWED_ITEMS.contains(_itemId);
	}
}