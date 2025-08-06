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

import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.actor.templates.CropProcureTemplate;
import l2e.gameserver.model.entity.Castle;

public class ExShowProcureCropDetail extends GameServerPacket
{
	private final int _cropId;
	private final Map<Integer, CropProcureTemplate> _castleCrops = new HashMap<>();

	public ExShowProcureCropDetail(int cropId)
	{
		_cropId = cropId;
		
		for (final Castle c : CastleManager.getInstance().getCastles())
		{
			final CropProcureTemplate cropItem = CastleManorManager.getInstance().getCropProcure(c.getId(), cropId, false);
			if ((cropItem != null) && (cropItem.getAmount() > 0))
			{
				_castleCrops.put(c.getId(), cropItem);
			}
		}
	}

	@Override
	public void writeImpl()
	{
		writeD(_cropId);
		writeD(_castleCrops.size());
		for (final Map.Entry<Integer, CropProcureTemplate> entry : _castleCrops.entrySet())
		{
			final CropProcureTemplate crop = entry.getValue();
			writeD(entry.getKey());
			writeQ(crop.getAmount());
			writeQ(crop.getPrice());
			writeC(crop.getReward());
		}
	}
}