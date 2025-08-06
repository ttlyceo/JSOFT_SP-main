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

import static l2e.gameserver.model.actor.Npc.INTERACTION_DISTANCE;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ManorManagerInstance;
import l2e.gameserver.model.actor.templates.CropProcureTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class RequestProcureCropList extends GameClientPacket
{
	private static final int BATCH_LENGTH = 20;
	private List<CropHolder> _items = null;

	@Override
	protected void readImpl()
	{
		final int count = readD();
		
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int objId = readD();
			final int itemId = readD();
			final int manorId = readD();
			final long cnt = readQ();
			if ((objId < 1) || (itemId < 1) || (manorId < 0) || (cnt < 0))
			{
				_items = null;
				return;
			}
			_items.add(new CropHolder(objId, itemId, cnt, manorId));
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
		{
			return;
		}
		
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.isActionsDisabled() || CastleManorManager.getInstance().isUnderMaintenance())
		{
			sendActionFailed();
			return;
		}
		
		GameObject manager = player.getTarget();
		if (manager == null)
		{
			return;
		}
		
		if (!(manager instanceof ManorManagerInstance))
		{
			manager = player.getLastFolkNPC();
		}
		
		if (!(manager instanceof ManorManagerInstance))
		{
			return;
		}
		
		if (!player.isInsideRadius(manager, INTERACTION_DISTANCE, false, false))
		{
			return;
		}
		
		final int castleId = ((ManorManagerInstance) manager).getCastle().getId();
		
		int slots = 0, weight = 0;
		for (final CropHolder i : _items)
		{
			final ItemInstance item = player.getInventory().getItemByObjectId(i.getObjectId());
			if ((item == null) || (item.getCount() < i.getCount()) || (item.getId() != i.getId()))
			{
				sendActionFailed();
				return;
			}
			
			final CropProcureTemplate cp = i.getCropProcure();
			if ((cp == null) || (cp.getAmount() < i.getCount()))
			{
				sendActionFailed();
				return;
			}
			
			final Item template = ItemsParser.getInstance().getTemplate(i.getRewardId());
			weight += (i.getCount() * template.getWeight());
			
			if (!template.isStackable())
			{
				slots += i.getCount();
			}
			else if (player.getInventory().getItemByItemId(i.getRewardId()) == null)
			{
				slots++;
			}
		}
		
		if (!player.getInventory().validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}
		
		if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}
		
		final int updateListSize = Config.ALT_MANOR_SAVE_ALL_ACTIONS ? _items.size() : 0;
		final List<CropProcureTemplate> updateList = new ArrayList<>(updateListSize);
		
		for (final CropHolder i : _items)
		{
			final long rewardPrice = ItemsParser.getInstance().getTemplate(i.getRewardId()).getReferencePrice();
			if (rewardPrice == 0)
			{
				continue;
			}
			
			final long rewardItemCount = i.getPrice() / rewardPrice;
			if (rewardItemCount < 1)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(i.getId());
				sm.addLong(i.getCount());
				player.sendPacket(sm);
				continue;
			}
			
			final long fee = (castleId == i.getManorId()) ? 0 : ((long) (i.getPrice() * 0.05));
			if ((fee != 0) && (player.getAdena() < fee))
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(i.getId());
				sm.addLong(i.getCount());
				player.sendPacket(sm);
				
				sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				player.sendPacket(sm);
				continue;
			}
			
			final CropProcureTemplate cp = i.getCropProcure();
			if (!cp.decreaseAmount(i.getCount()) || ((fee > 0) && !player.reduceAdena("Manor", fee, manager, true)) || !player.destroyItem("Manor", i.getObjectId(), i.getCount(), manager, true))
			{
				continue;
			}
			player.addItem("Manor", i.getRewardId(), rewardItemCount, manager, true);
			
			if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				updateList.add(cp);
			}
		}
		
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			CastleManorManager.getInstance().updateCurrentProcure(castleId, updateList);
		}
	}
	
	private static class CropHolder extends ItemHolder
	{
		private final int _manorId;
		private CropProcureTemplate _cp;
		private int _rewardId = 0;
		
		public CropHolder(int objectId, int id, long count, int manorId)
		{
			super(id, objectId, count);
			_manorId = manorId;
		}
		
		public final int getManorId()
		{
			return _manorId;
		}
		
		public final long getPrice()
		{
			return getCount() * _cp.getPrice();
		}
		
		public final CropProcureTemplate getCropProcure()
		{
			if (_cp == null)
			{
				_cp = CastleManorManager.getInstance().getCropProcure(_manorId, getId(), false);
			}
			return _cp;
		}
		
		public final int getRewardId()
		{
			if (_rewardId == 0)
			{
				_rewardId = CastleManorManager.getInstance().getSeedByCrop(_cp.getId()).getReward(_cp.getReward());
			}
			return _rewardId;
		}
	}
}