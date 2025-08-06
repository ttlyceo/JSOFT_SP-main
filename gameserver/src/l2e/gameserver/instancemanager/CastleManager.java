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
package l2e.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.InstanceListManager;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.items.instance.ItemInstance;

public final class CastleManager extends LoggerObject implements InstanceListManager
{
	public static final CastleManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private final List<Castle> _castles = new ArrayList<>();

	private static final int _castleCirclets[] =
	{
	        0, 6838, 6835, 6839, 6837, 6840, 6834, 6836, 8182, 8183
	};

	protected CastleManager()
	{
	}

	public final int findNearestCastleIndex(GameObject obj)
	{
		return findNearestCastleIndex(obj, Long.MAX_VALUE);
	}

	public final int findNearestCastleIndex(GameObject obj, long maxDistance)
	{
		int index = getCastleIndex(obj);
		if (index < 0)
		{
			double distance;
			Castle castle;
			for (int i = 0; i < getCastles().size(); i++)
			{
				castle = getCastles().get(i);
				if (castle == null)
				{
					continue;
				}
				distance = castle.getDistance(obj);
				if (maxDistance > distance)
				{
					maxDistance = (long) distance;
					index = i;
				}
			}
		}
		return index;
	}

	public final Castle getCastleById(int castleId)
	{
		for (final Castle temp : getCastles())
		{
			if (temp.getId() == castleId)
			{
				return temp;
			}
		}
		return null;
	}

	public final Castle getCastleByOwner(Clan clan)
	{
		for (final Castle temp : getCastles())
		{
			if (temp.getOwnerId() == clan.getId())
			{
				return temp;
			}
		}
		return null;
	}

	public final Castle getCastle(int x, int y, int z)
	{
		for (final Castle temp : getCastles())
		{
			if (temp.checkIfInZone(x, y, z))
			{
				return temp;
			}
		}
		return null;
	}

	public final Castle getCastle(GameObject activeObject)
	{
		return getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	public final int getCastleIndex(int castleId)
	{
		Castle castle;
		for (int i = 0; i < getCastles().size(); i++)
		{
			castle = getCastles().get(i);
			if ((castle != null) && (castle.getId() == castleId))
			{
				return i;
			}
		}
		return -1;
	}

	public final int getCastleIndex(GameObject activeObject)
	{
		return getCastleIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	public final int getCastleIndex(int x, int y, int z)
	{
		Castle castle;
		for (int i = 0; i < getCastles().size(); i++)
		{
			castle = getCastles().get(i);
			if ((castle != null) && castle.checkIfInZone(x, y, z))
			{
				return i;
			}
		}
		return -1;
	}

	public final List<Castle> getCastles()
	{
		return _castles;
	}

	public final void validateTaxes(int sealStrifeOwner)
	{
		int maxTax;
		switch (sealStrifeOwner)
		{
			case SevenSigns.CABAL_DUSK :
				maxTax = 5;
				break;
			case SevenSigns.CABAL_DAWN :
				maxTax = 25;
				break;
			default :
				maxTax = 15;
				break;
		}
		for (final Castle castle : _castles)
		{
			if (castle.getTaxPercent() > maxTax)
			{
				castle.setTaxPercent(maxTax);
			}
		}
	}

	public int getCirclet()
	{
		return getCircletByCastleId(1);
	}

	public int getCircletByCastleId(int castleId)
	{
		if ((castleId > 0) && (castleId < 10))
		{
			return _castleCirclets[castleId];
		}

		return 0;
	}

	public void removeCirclet(Clan clan, int castleId)
	{
		for (final ClanMember member : clan.getMembers())
		{
			removeCirclet(member, castleId);
		}
	}

	public void removeCirclet(ClanMember member, int castleId)
	{
		if (member == null)
		{
			return;
		}
		final Player player = member.getPlayerInstance();
		final int circletId = getCircletByCastleId(castleId);

		if (circletId != 0)
		{
			if (player != null)
			{
				try
				{
					final ItemInstance circlet = player.getInventory().getItemByItemId(circletId);
					if (circlet != null)
					{
						if (circlet.isEquipped())
						{
							player.getInventory().unEquipItemInSlot(circlet.getLocationSlot());
						}
						player.destroyItemByItemId("CastleCircletRemoval", circletId, 1, player, true);
					}
					return;
				}
				catch (final NullPointerException e)
				{}
			}

			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? and item_id = ?");
				statement.setInt(1, member.getObjectId());
				statement.setInt(2, circletId);
				statement.execute();
			}
			catch (final Exception e)
			{
				warn("Failed to remove castle circlets offline for player " + member.getName() + ": " + e.getMessage(), e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	@Override
	public void loadInstances()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT id FROM castle ORDER BY id");
			rs = statement.executeQuery();
			while (rs.next())
			{
				getCastles().add(new Castle(rs.getInt("id")));
			}
			info("Loaded: " + getCastles().size() + " castles");
		}
		catch (final Exception e)
		{
			warn("Failed loadCastleData(): " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	@Override
	public void updateReferences()
	{
	}

	@Override
	public void activateInstances()
	{
		for (final Castle castle : _castles)
		{
			castle.activateInstance();
		}
	}
	
	private static class SingletonHolder
	{
		protected static final CastleManager _instance = new CastleManager();
	}
}