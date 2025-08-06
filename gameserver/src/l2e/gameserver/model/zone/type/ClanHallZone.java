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
package l2e.gameserver.model.zone.type;

import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.ClanHall;
import l2e.gameserver.model.entity.clanhall.AuctionableHall;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneRespawn;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AgitDecoInfo;

public class ClanHallZone extends ZoneRespawn
{
	private int _clanHallId;

	public ClanHallZone(int id)
	{
		super(id);
		addZoneId(ZoneId.CLAN_HALL);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("clanHallId"))
		{
			_clanHallId = Integer.parseInt(value);
			final ClanHall hall = ClanHallManager.getInstance().getClanHallById(_clanHallId);
			if (hall == null)
			{
				_log.warn("ClanHallZone: Clan hall with id " + _clanHallId + " does not exist!");
			}
			else
			{
				hall.setZone(this);
			}
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (character.isPlayer())
		{
			final AuctionableHall clanHall = ClanHallManager.getInstance().getAuctionableHallById(_clanHallId);
			if (clanHall == null)
			{
				return;
			}

			final AgitDecoInfo deco = new AgitDecoInfo(clanHall);
			character.sendPacket(deco);

		}
	}

	@Override
	protected void onExit(Creature character)
	{
	}

	@Override
	public void onDieInside(Creature character)
	{
	}

	@Override
	public void onReviveInside(Creature character)
	{
	}

	public void banishForeigners(int owningClanId)
	{
		final TeleportWhereType type = TeleportWhereType.CLANHALL_BANISH;
		for (final Player temp : getPlayersInside())
		{
			if (temp.getClanId() == owningClanId && owningClanId != 0)
			{
				continue;
			}

			temp.teleToLocation(type, true, ReflectionManager.DEFAULT);
		}
	}

	public int getClanHallId()
	{
		return _clanHallId;
	}
	
	public void updateSiegeStatus()
	{
		if (_clanHallId == 35)
		{
			for (final Creature character : _characterList.values())
			{
				try
				{
					onEnter(character);
				}
				catch (final Exception e)
				{}
			}
		}
		else
		{
			getZoneId().clear();
			for (final Creature character : _characterList.values())
			{
				try
				{
					if (character.isPlayer())
					{
						character.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
					}
				}
				catch (final Exception e)
				{}
			}
		}
	}
}