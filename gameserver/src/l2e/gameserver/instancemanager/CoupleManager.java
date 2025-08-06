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

import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Couple;

public final class CoupleManager extends LoggerObject
{
	protected CoupleManager()
	{
		load();
	}
	
	public static final CoupleManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private final List<Couple> _couples = new CopyOnWriteArrayList<>();

	public void reload()
	{
		_couples.clear();
		load();
	}

	private final void load()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT id FROM mods_wedding ORDER BY id");
			final ResultSet rs = statement.executeQuery();

			while (rs.next())
			{
				getCouples().add(new Couple(rs.getInt("id")));
			}

			rs.close();
			statement.close();

			info("Loaded: " + getCouples().size() + " couples(s)");
		}
		catch (final Exception e)
		{
			warn("CoupleManager.load(): " + e.getMessage(), e);
		}
	}

	public final Couple getCouple(int coupleId)
	{
		final int index = getCoupleIndex(coupleId);
		if (index >= 0)
		{
			return getCouples().get(index);
		}
		return null;
	}

	public void createCouple(Player player1, Player player2)
	{
		if ((player1 != null) && (player2 != null))
		{
			if ((player1.getPartnerId() == 0) && (player2.getPartnerId() == 0))
			{
				final int _player1id = player1.getObjectId();
				final int _player2id = player2.getObjectId();

				final Couple _new = new Couple(player1, player2);
				getCouples().add(_new);
				player1.setPartnerId(_player2id);
				player2.setPartnerId(_player1id);
				player1.setCoupleId(_new.getId());
				player2.setCoupleId(_new.getId());
			}
		}
	}

	public void deleteCouple(int coupleId)
	{
		final int index = getCoupleIndex(coupleId);
		final Couple couple = getCouples().get(index);
		if (couple != null)
		{
			final Player player1 = GameObjectsStorage.getPlayer(couple.getPlayer1Id());
			final Player player2 = GameObjectsStorage.getPlayer(couple.getPlayer2Id());
			if (player1 != null)
			{
				player1.setPartnerId(0);
				player1.setMarried(false);
				player1.setCoupleId(0);

			}
			if (player2 != null)
			{
				player2.setPartnerId(0);
				player2.setMarried(false);
				player2.setCoupleId(0);

			}
			couple.divorce();
			getCouples().remove(index);
		}
	}

	public final int getCoupleIndex(int coupleId)
	{
		int i = 0;
		for (final Couple temp : getCouples())
		{
			if ((temp != null) && (temp.getId() == coupleId))
			{
				return i;
			}
			i++;
		}
		return -1;
	}

	public final List<Couple> getCouples()
	{
		return _couples;
	}

	private static class SingletonHolder
	{
		protected static final CoupleManager _instance = new CoupleManager();
	}
}