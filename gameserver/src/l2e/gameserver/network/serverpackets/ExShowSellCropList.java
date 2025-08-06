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
package l2e.gameserver.network.serverpackets;

import java.util.HashMap;
import java.util.Map;

import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.Seed;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.CropProcureTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;

public class ExShowSellCropList extends GameServerPacket
{
	private final int _manorId;
	private final Map<Integer, ItemInstance> _cropsItems = new HashMap<>();
	private final Map<Integer, CropProcureTemplate> _castleCrops = new HashMap<>();
	
	public ExShowSellCropList(Player player, int manorId)
	{
		_manorId = manorId;
		for (final int cropId : CastleManorManager.getInstance().getCropIds())
		{
			final ItemInstance item = player.getInventory().getItemByItemId(cropId);
			if (item != null)
			{
				_cropsItems.put(cropId, item);
			}
		}
		
		for (final CropProcureTemplate crop : CastleManorManager.getInstance().getCropProcure(_manorId, false))
		{
			if (_cropsItems.containsKey(crop.getId()) && (crop.getAmount() > 0))
			{
				_castleCrops.put(crop.getId(), crop);
			}
		}
	}
	
	@Override
	public void writeImpl()
	{
		writeD(_manorId);
		writeD(_cropsItems.size());
		for (final ItemInstance item : _cropsItems.values())
		{
			final Seed seed = CastleManorManager.getInstance().getSeedByCrop(item.getId());
			writeD(item.getObjectId());
			writeD(item.getId());
			writeD(seed.getLevel());
			writeC(0x01);
			writeD(seed.getReward(1));
			writeC(0x01);
			writeD(seed.getReward(2));
			if (_castleCrops.containsKey(item.getId()))
			{
				final CropProcureTemplate crop = _castleCrops.get(item.getId());
				writeD(_manorId);
				writeQ(crop.getAmount());
				writeQ(crop.getPrice());
				writeC(crop.getReward());
			}
			else
			{
				writeD(0xFFFFFFFF);
				writeQ(0x00);
				writeQ(0x00);
				writeC(0x00);
			}
			writeQ(item.getCount());
		}
	}
}