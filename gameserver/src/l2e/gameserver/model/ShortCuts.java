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
package l2e.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.ShortCutTemplate;
import l2e.gameserver.model.base.ShortcutType;
import l2e.gameserver.model.interfaces.IRestorable;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.network.serverpackets.ExAutoSoulShot;
import l2e.gameserver.network.serverpackets.ShortCutInit;
import l2e.gameserver.network.serverpackets.ShortCutRegister;

public class ShortCuts implements IRestorable
{
	private static final Logger _log = LoggerFactory.getLogger(ShortCuts.class);
	private static final int MAX_SHORTCUTS_PER_BAR = 12;
	private final Player _owner;
	private final Map<Integer, ShortCutTemplate> _shortCuts = new TreeMap<>();
	
	public ShortCuts(Player owner)
	{
		_owner = owner;
	}
	
	public ShortCutTemplate[] getAllShortCuts()
	{
		return _shortCuts.values().toArray(new ShortCutTemplate[_shortCuts.values().size()]);
	}

	public ShortCutTemplate getShortCut(int slot, int page)
	{
		ShortCutTemplate sc = _shortCuts.get(slot + (page * MAX_SHORTCUTS_PER_BAR));

		if ((sc != null) && (sc.getType() == ShortcutType.ITEM))
		{
			if (_owner.getInventory().getItemByObjectId(sc.getId()) == null)
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
				sc = null;
			}
		}
		return sc;
	}

	public synchronized void registerShortCut(ShortCutTemplate shortcut)
	{
		if (shortcut.getType() == ShortcutType.ITEM)
		{
			final ItemInstance item = _owner.getInventory().getItemByObjectId(shortcut.getId());
			if (item == null)
			{
				return;
			}
			shortcut.setSharedReuseGroup(item.getSharedReuseGroup());
			if (item.getSharedReuseGroup() > 0)
			{
				final TimeStamp timeStamp = _owner.getSharedItemReuse(item.getObjectId());
				if (timeStamp != null)
				{
					shortcut.setCurrenReuse((int) (timeStamp.getRemaining() / 1000L));
					shortcut.setReuse((int) (timeStamp.getReuseBasic() / 1000L));
				}
			}

			if (item.getAugmentation() != null)
			{
				shortcut.setAugmentationId(item.getAugmentation().getAugmentationId());
			}
			else
			{
				shortcut.setAugmentationId(0);
			}
		}
		
		final ShortCutTemplate oldShortCut = _shortCuts.put(shortcut.getSlot() + (shortcut.getPage() * MAX_SHORTCUTS_PER_BAR), shortcut);
		registerShortCutInDb(shortcut, oldShortCut);
	}

	public synchronized void registerShortCut(ShortCutTemplate shortcut, boolean storeToDb)
	{
		if (shortcut.getType() == ShortcutType.ITEM)
		{
			final ItemInstance item = _owner.getInventory().getItemByObjectId(shortcut.getId());
			if (item == null)
			{
				return;
			}
			if (item.isEtcItem())
			{
				shortcut.setSharedReuseGroup(item.getEtcItem().getSharedReuseGroup());
				if (item.getEtcItem().getSharedReuseGroup() > 0)
				{
					final TimeStamp timeStamp = _owner.getSharedItemReuse(item.getObjectId());
					if (timeStamp != null)
					{
						shortcut.setCurrenReuse((int) (timeStamp.getRemaining() / 1000L));
						shortcut.setReuse((int) (timeStamp.getReuseBasic() / 1000L));
					}
				}
				if (item.getAugmentation() != null)
				{
					shortcut.setAugmentationId(item.getAugmentation().getAugmentationId());
				}
				else
				{
					shortcut.setAugmentationId(0);
				}
			}
		}
		
		final ShortCutTemplate oldShortCut = _shortCuts.put(shortcut.getSlot() + (12 * shortcut.getPage()), shortcut);
		if (storeToDb)
		{
			registerShortCutInDb(shortcut, oldShortCut);
		}
	}

	private void registerShortCutInDb(ShortCutTemplate shortcut, ShortCutTemplate oldShortCut)
	{
		if (oldShortCut != null)
		{
			deleteShortCutFromDb(oldShortCut);
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO character_shortcuts (charId,slot,page,type,shortcut_id,level,class_index) values(?,?,?,?,?,?,?)");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.getSlot());
			statement.setInt(3, shortcut.getPage());
			statement.setInt(4, shortcut.getType().ordinal());
			statement.setInt(5, shortcut.getId());
			statement.setInt(6, shortcut.getLevel());
			statement.setInt(7, _owner.getClassIndex());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Could not store character shortcut: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public synchronized void deleteShortCut(int slot, int page, boolean fromDb)
	{
		final ShortCutTemplate old = _shortCuts.remove(slot + (page * 12));

		if ((old == null) || (_owner == null))
		{
			return;
		}
		
		if (fromDb)
		{
			deleteShortCutFromDb(old);
		}

		if (old.getType() == ShortcutType.ITEM)
		{
			final ItemInstance item = _owner.getInventory().getItemByObjectId(old.getId());

			if ((item != null) && (item.getItemType() == EtcItemType.SHOT))
			{
				if (_owner.removeAutoSoulShot(item.getId()))
				{
					_owner.sendPacket(new ExAutoSoulShot(item.getId(), 0));
				}
			}
		}
		_owner.sendPacket(new ShortCutInit(_owner));

		for (final int shotId : _owner.getAutoSoulShot())
		{
			_owner.sendPacket(new ExAutoSoulShot(shotId, 1));
		}
	}

	public synchronized void deleteShortCut(int slot, int page)
	{
		final ShortCutTemplate old = _shortCuts.remove(slot + (page * MAX_SHORTCUTS_PER_BAR));
		if ((old == null) || (_owner == null))
		{
			return;
		}
		
		deleteShortCutFromDb(old);
		if (old.getType() == ShortcutType.ITEM)
		{
			final ItemInstance item = _owner.getInventory().getItemByObjectId(old.getId());

			if ((item != null) && (item.getItemType() == EtcItemType.SHOT))
			{
				if (_owner.removeAutoSoulShot(item.getId()))
				{
					_owner.sendPacket(new ExAutoSoulShot(item.getId(), 0));
				}
			}
		}

		_owner.sendPacket(new ShortCutInit(_owner));

		for (final int shotId : _owner.getAutoSoulShot())
		{
			_owner.sendPacket(new ExAutoSoulShot(shotId, 1));
		}
	}

	public synchronized void deleteShortCutByObjectId(int objectId)
	{
		for (final ShortCutTemplate shortcut : _shortCuts.values())
		{
			if ((shortcut.getType() == ShortcutType.ITEM) && (shortcut.getId() == objectId))
			{
				deleteShortCut(shortcut.getSlot(), shortcut.getPage());
				break;
			}
		}
	}

	private void deleteShortCutFromDb(ShortCutTemplate shortcut)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=? AND slot=? AND page=? AND class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.getSlot());
			statement.setInt(3, shortcut.getPage());
			statement.setInt(4, _owner.getClassIndex());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Could not delete character shortcut: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	@Override
	public boolean restoreMe()
	{
		_shortCuts.clear();
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT charId, slot, page, type, shortcut_id, level FROM character_shortcuts WHERE charId=? AND class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, _owner.getClassIndex());

			rset = statement.executeQuery();
			while (rset.next())
			{
				final int slot = rset.getInt("slot");
				final int page = rset.getInt("page");
				final int type = rset.getInt("type");
				final int id = rset.getInt("shortcut_id");
				final int level = rset.getInt("level");

				final ShortCutTemplate sc = new ShortCutTemplate(slot, page, ShortcutType.values()[type], id, level, 1);
				_shortCuts.put(slot + (page * MAX_SHORTCUTS_PER_BAR), sc);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore character shortcuts: " + e.getMessage(), e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		for (final ShortCutTemplate sc : getAllShortCuts())
		{
			if (sc.getType() == ShortcutType.ITEM)
			{
				final ItemInstance item = _owner.getInventory().getItemByObjectId(sc.getId());
				if (item == null)
				{
					deleteShortCut(sc.getSlot(), sc.getPage());
				}
				else if (item.isEtcItem())
				{
					sc.setSharedReuseGroup(item.getEtcItem().getSharedReuseGroup());
					if (item.getEtcItem().getSharedReuseGroup() > 0)
					{
						final TimeStamp timeStamp = _owner.getSharedItemReuse(item.getObjectId());
						if (timeStamp != null)
						{
							sc.setCurrenReuse((int) (timeStamp.getRemaining() / 1000L));
							sc.setReuse((int) (timeStamp.getReuseBasic() / 1000L));
						}
					}

					if (item.getAugmentation() != null)
					{
						sc.setAugmentationId(item.getAugmentation().getAugmentationId());
					}
					else
					{
						sc.setAugmentationId(0);
					}
				}
			}
		}
		return true;
	}

	public synchronized void updateShortCuts(int skillId, int skillLevel)
	{
		for (final ShortCutTemplate sc : _shortCuts.values())
		{
			if ((sc.getId() == skillId) && (sc.getType() == ShortcutType.SKILL))
			{
				final ShortCutTemplate newsc = new ShortCutTemplate(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), skillLevel, 1);
				_owner.sendPacket(new ShortCutRegister(newsc));
				_owner.registerShortCut(newsc);
			}
		}
	}
	
	public synchronized void updateShortCuts(int objId, ShortcutType type)
	{
		for (final ShortCutTemplate sc : _shortCuts.values())
		{
			if ((sc.getId() == objId) && (sc.getType() == type))
			{
				final ShortCutTemplate newsc = new ShortCutTemplate(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), sc.getLevel(), sc.getCharacterType());
				_owner.sendPacket(new ShortCutRegister(newsc));
				_owner.registerShortCut(newsc);
			}
		}
	}

	public synchronized void tempRemoveAll()
	{
		_shortCuts.clear();
	}
}