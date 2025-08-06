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

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.CursedWeapon;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DefenderInstance;
import l2e.gameserver.model.actor.instance.FeedableBeastInstance;
import l2e.gameserver.model.actor.instance.FestivalMonsterInstance;
import l2e.gameserver.model.actor.instance.FortCommanderInstance;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.instance.GuardInstance;
import l2e.gameserver.model.actor.instance.RiftInvaderInstance;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class CursedWeaponsManager extends LoggerObject
{
	private Map<Integer, CursedWeapon> _cursedWeapons;
	
	protected CursedWeaponsManager()
	{
		init();
	}
	
	private void init()
	{
		_cursedWeapons = new HashMap<>();

		if (!Config.ALLOW_CURSED_WEAPONS)
		{
			return;
		}

		load();
		restore();
		controlPlayers();
		info("Loaded " + _cursedWeapons.size() + " cursed weapon(s).");
	}

	public final void reload()
	{
		init();
	}

	private final void load()
	{
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/items/cursedWeapons.xml");
			if (!file.exists())
			{
				warn("Couldn't find data/stats/items/" + file.getName());
				return;
			}

			final Document doc = factory.newDocumentBuilder().parse(file);

			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							final int skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
							final String name = attrs.getNamedItem("name").getNodeValue();

							final CursedWeapon cw = new CursedWeapon(id, skillId, name);

							int val;
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("dropRate".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDropRate(val);
								}
								else if ("duration".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDuration(val);
								}
								else if ("durationLost".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDurationLost(val);
								}
								else if ("disapearChance".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDisapearChance(val);
								}
								else if ("stageKills".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setStageKills(val);
								}
							}
							_cursedWeapons.put(id, cw);
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			warn("Error parsing cursed weapons file.", e);
			return;
		}
	}

	private final void restore()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("SELECT itemId, charId, playerKarma, playerPkKills, nbKills, endTime FROM cursed_weapons");
			final var rset = statement.executeQuery();

			while (rset.next())
			{
				final int itemId = rset.getInt("itemId");
				final int playerId = rset.getInt("charId");
				final int playerKarma = rset.getInt("playerKarma");
				final int playerPkKills = rset.getInt("playerPkKills");
				final int nbKills = rset.getInt("nbKills");
				final long endTime = rset.getLong("endTime");

				final CursedWeapon cw = _cursedWeapons.get(itemId);
				cw.setPlayerId(playerId);
				cw.setPlayerKarma(playerKarma);
				cw.setPlayerPkKills(playerPkKills);
				cw.setNbKills(nbKills);
				cw.setEndTime(endTime);
				cw.reActivate();
			}
			rset.close();
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not restore CursedWeapons data: " + e.getMessage(), e);
			return;
		}
	}

	private final void controlPlayers()
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			try (
			    var statement = con.prepareStatement("SELECT owner_id FROM items WHERE item_id=?"))
			{
				for (final CursedWeapon cw : _cursedWeapons.values())
				{
					if (cw.isActivated())
					{
						continue;
					}

					final int itemId = cw.getItemId();
					statement.setInt(1, itemId);
					try (
					    var rset = statement.executeQuery())
					{
						if (rset.next())
						{
							final int playerId = rset.getInt("owner_id");
							info("Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");

							try (
							    PreparedStatement delete = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?"))
							{
								delete.setInt(1, playerId);
								delete.setInt(2, itemId);
								if (delete.executeUpdate() != 1)
								{
									warn("Error while deleting cursed weapon " + itemId + " from userId " + playerId);
								}
							}

							try (
							    PreparedStatement update = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE charId=?"))
							{
								update.setInt(1, cw.getPlayerKarma());
								update.setInt(2, cw.getPlayerPkKills());
								update.setInt(3, playerId);
								if (update.executeUpdate() != 1)
								{
									warn("Error while updating karma & pkkills for userId " + cw.getPlayerId());
								}
							}
							removeFromDb(itemId);
						}
					}
					statement.clearParameters();
				}
			}
		}
		catch (final Exception e)
		{
			warn("Could not check CursedWeapons data: " + e.getMessage(), e);
		}
	}

	public synchronized void checkDrop(Attackable attackable, Player player)
	{
		if ((attackable instanceof DefenderInstance) || (attackable instanceof RiftInvaderInstance) || (attackable instanceof FestivalMonsterInstance) || (attackable instanceof GuardInstance) || (attackable instanceof GrandBossInstance) || (attackable instanceof FeedableBeastInstance) || (attackable instanceof FortCommanderInstance))
		{
			return;
		}

		for (final CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActive())
			{
				continue;
			}

			if (cw.checkDrop(attackable, player))
			{
				break;
			}
		}
	}

	public void activate(Player player, ItemInstance item)
	{
		final CursedWeapon cw = _cursedWeapons.get(item.getId());

		if (player.isCursedWeaponEquipped())
		{
			final CursedWeapon cw2 = _cursedWeapons.get(player.getCursedWeaponEquippedId());
			cw2.setNbKills(cw2.getStageKills() - 1);
			cw2.increaseKills();
			cw.setPlayer(player);
			cw.endOfLife();
		}
		else
		{
			cw.activate(player, item);
		}
	}

	public void drop(int itemId, Creature killer)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);

		cw.dropIt(killer);
	}

	public void increaseKills(int itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);

		cw.increaseKills();
	}

	public int getLevel(int itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);

		return cw.getLevel();
	}

	public static void announce(SystemMessage sm)
	{
		GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(sm));
	}

	public void checkPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}

		for (final CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActivated() && (player.getObjectId() == cw.getPlayerId()))
			{
				cw.setPlayer(player);
				cw.setItem(player.getInventory().getItemByItemId(cw.getItemId()));
				cw.giveSkill();
				player.setCursedWeaponEquippedId(cw.getItemId());

				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
				sm.addString(cw.getName());
				sm.addNumber((int) ((cw.getEndTime() - System.currentTimeMillis()) / 60000));
				player.sendPacket(sm);
			}
		}
	}

	public int checkOwnsWeaponId(int ownerId)
	{
		for (final CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActivated() && (ownerId == cw.getPlayerId()))
			{
				return cw.getItemId();
			}
		}
		return -1;
	}

	public void removeFromDb(int itemId)
	{
		try (
		    var con = DatabaseFactory.getInstance().getConnection())
		{
			final var statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
			statement.setInt(1, itemId);
			statement.executeUpdate();
			statement.close();
		}
		catch (final SQLException e)
		{
			warn("CursedWeaponsManager: Failed to remove data: " + e.getMessage(), e);
		}
	}

	public void saveData()
	{
		for (final CursedWeapon cw : _cursedWeapons.values())
		{
			cw.saveData();
		}
	}

	public boolean isCursed(int itemId)
	{
		return _cursedWeapons.containsKey(itemId);
	}

	public Collection<CursedWeapon> getCursedWeapons()
	{
		return _cursedWeapons.values();
	}

	public Set<Integer> getCursedWeaponsIds()
	{
		return _cursedWeapons.keySet();
	}

	public CursedWeapon getCursedWeapon(int itemId)
	{
		return _cursedWeapons.get(itemId);
	}

	public void givePassive(int itemId)
	{
		try
		{
			_cursedWeapons.get(itemId).giveSkill();
		}
		catch (final Exception e)
		{}
	}

	public static final CursedWeaponsManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final CursedWeaponsManager _instance = new CursedWeaponsManager();
	}
}