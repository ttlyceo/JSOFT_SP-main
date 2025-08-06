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
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.CastleManorManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Seed;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.CastleChamberlainInstance;
import l2e.gameserver.model.actor.templates.CropProcureTemplate;
import l2e.gameserver.model.entity.Castle;

public class RequestSetCrop extends GameClientPacket
{
	private static final int BATCH_LENGTH = 21;
	
	private int _manorId;
	private List<CropProcureTemplate> _items;
	
	@Override
	protected void readImpl()
	{
		_manorId = readD();
		final int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int itemId = readD();
			final long sales = readQ();
			final long price = readQ();
			final int type = readC();
			if ((itemId < 1) || (sales < 0) || (price < 0))
			{
				_items.clear();
				return;
			}
			
			if (sales > 0)
			{
				_items.add(new CropProcureTemplate(itemId, sales, type, sales, price));
			}
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
		if ((player == null) || (player.getClan() == null) || ((player.getClanPrivileges() & Clan.CP_CS_MANOR_ADMIN) == 0))
		{
			return;
		}
		
		final Castle currentCastle = CastleManager.getInstance().getCastleById(_manorId);
		if (currentCastle.getOwnerId() != player.getClanId())
		{
			return;
		}
		
		GameObject manager = player.getTarget();
		if (manager == null)
		{
			return;
		}
		if (!(manager instanceof CastleChamberlainInstance))
		{
			manager = player.getLastFolkNPC();
		}
		
		if (!(manager instanceof CastleChamberlainInstance))
		{
			return;
		}
		
		if (((CastleChamberlainInstance) manager).getCastle() != currentCastle)
		{
			return;
		}
		
		if (!player.isInsideRadius(manager, INTERACTION_DISTANCE, true, false))
		{
			return;
		}
		
		final List<CropProcureTemplate> list = new ArrayList<>(_items.size());
		for (final CropProcureTemplate cp : _items)
		{
			final Seed s = CastleManorManager.getInstance().getSeedByCrop(cp.getId(), _manorId);
			if ((s != null) && (cp.getStartAmount() <= s.getCropLimit()) && (cp.getPrice() >= s.getCropMinPrice()) && (cp.getPrice() <= s.getCropMaxPrice()))
			{
				list.add(cp);
			}
		}
		CastleManorManager.getInstance().setNextCropProcure(list, _manorId);
	}
}