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
import l2e.gameserver.model.actor.templates.SeedTemplate;
import l2e.gameserver.model.entity.Castle;

public class RequestSetSeed extends GameClientPacket
{
	private static final int BATCH_LENGTH = 20;

	private int _manorId;
	private List<SeedTemplate> _items;
	
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
			if ((itemId < 1) || (sales < 0) || (price < 0))
			{
				_items.clear();
				return;
			}
			
			if (sales > 0)
			{
				_items.add(new SeedTemplate(itemId, sales, price, sales));
			}
		}
	}

	@Override
	protected void runImpl()
	{
		if (_items == null || _items.isEmpty())
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

		final List<SeedTemplate> list = new ArrayList<>(_items.size());
		for (final SeedTemplate sp : _items)
		{
			final Seed s = CastleManorManager.getInstance().getSeed(sp.getId());
			if ((s != null) && (sp.getStartAmount() <= s.getSeedLimit()) && (sp.getPrice() >= s.getSeedMinPrice()) && (sp.getPrice() <= s.getSeedMaxPrice()))
			{
				list.add(sp);
			}
		}
		CastleManorManager.getInstance().setNextSeedProduction(list, _manorId);
	}
}