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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.StringUtil;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.MacroTemplate;
import l2e.gameserver.model.actor.templates.ShortCutTemplate;
import l2e.gameserver.model.base.MacroType;
import l2e.gameserver.model.base.ShortcutType;
import l2e.gameserver.model.interfaces.IRestorable;
import l2e.gameserver.network.serverpackets.MacrosList;

public class MacroList implements IRestorable
{
	private static final Logger _log = LoggerFactory.getLogger(MacroList.class);

	private final Player _owner;
	private int _revision;
	private int _macroId;
	private final Map<Integer, Macro> _macroses = Collections.synchronizedMap(new LinkedHashMap<>());
	
	public MacroList(Player owner)
	{
		_owner = owner;
		_revision = 1;
		_macroId = 1000;
	}

	public int getRevision()
	{
		return _revision;
	}

	public Map<Integer, Macro> getAllMacroses()
	{
		return _macroses;
	}

	public void registerMacro(Macro macro)
	{
		if (macro.getId() == 0)
		{
			macro.setId(_macroId++);
			while (_macroses.containsKey(macro.getId()))
			{
				macro.setId(_macroId++);
			}
			_macroses.put(macro.getId(), macro);
			registerMacroInDb(macro);
		}
		else
		{
			final Macro old = _macroses.put(macro.getId(), macro);
			if (old != null)
			{
				deleteMacroFromDb(old);
			}
			registerMacroInDb(macro);
		}
		sendUpdate();
	}

	public void deleteMacro(int id)
	{
		final Macro removed = _macroses.remove(id);
		if (removed != null)
		{
			deleteMacroFromDb(removed);
		}

		final ShortCutTemplate[] allShortCuts = _owner.getAllShortCuts();
		for (final ShortCutTemplate sc : allShortCuts)
		{
			if ((sc.getId() == id) && (sc.getType() == ShortcutType.MACRO))
			{
				_owner.deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}

		sendUpdate();
	}

	public void sendUpdate()
	{
		_revision++;
		final Collection<Macro> allMacros = _macroses.values();
		synchronized (_macroses)
		{
			if (allMacros.isEmpty())
			{
				_owner.sendPacket(new MacrosList(_revision, 0, null));
			}
			else
			{
				for (final Macro m : allMacros)
				{
					_owner.sendPacket(new MacrosList(_revision, allMacros.size(), m));
				}
			}
		}
	}

	private void registerMacroInDb(Macro macro)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO character_macroses (charId,id,icon,name,descr,acronym,commands) values(?,?,?,?,?,?,?)");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, macro.getId());
			statement.setInt(3, macro.getIcon());
			statement.setString(4, macro.getName());
			statement.setString(5, macro.getDescr());
			statement.setString(6, macro.getAcronym());
			final StringBuilder sb = new StringBuilder(300);
			for (final MacroTemplate cmd : macro.getCommands())
			{
				StringUtil.append(sb, String.valueOf(cmd.getType().ordinal()), ",", String.valueOf(cmd.getD1()), ",", String.valueOf(cmd.getD2()));
				if ((cmd.getCmd() != null) && (cmd.getCmd().length() > 0))
				{
					StringUtil.append(sb, ",", cmd.getCmd());
				}
				sb.append(';');
			}

			if (sb.length() > 255)
			{
				sb.setLength(255);
			}

			statement.setString(7, sb.toString());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("could not store macro:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void deleteMacroFromDb(Macro macro)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_macroses WHERE charId=? AND id=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, macro.getId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("could not delete macro:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	@Override
	public boolean restoreMe()
	{
		_macroses.clear();
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT charId, id, icon, name, descr, acronym, commands FROM character_macroses WHERE charId=?");
			statement.setInt(1, _owner.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int id = rset.getInt("id");
				final int icon = rset.getInt("icon");
				final String name = rset.getString("name");
				final String descr = rset.getString("descr");
				final String acronym = rset.getString("acronym");
				final List<MacroTemplate> commands = new ArrayList<>();
				final StringTokenizer st1 = new StringTokenizer(rset.getString("commands"), ";");
				while (st1.hasMoreTokens())
				{
					final StringTokenizer st = new StringTokenizer(st1.nextToken(), ",");
					if (st.countTokens() < 3)
					{
						continue;
					}
					final MacroType type = MacroType.values()[Integer.parseInt(st.nextToken())];
					final int d1 = Integer.parseInt(st.nextToken());
					final int d2 = Integer.parseInt(st.nextToken());
					String cmd = "";
					if (st.hasMoreTokens())
					{
						cmd = st.nextToken();
					}
					commands.add(new MacroTemplate(commands.size(), type, d1, d2, cmd));
				}
				_macroses.put(id, new Macro(id, icon, name, descr, acronym, commands));
			}
		}
		catch (final Exception e)
		{
			_log.warn("could not store shortcuts:", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return true;
	}
}