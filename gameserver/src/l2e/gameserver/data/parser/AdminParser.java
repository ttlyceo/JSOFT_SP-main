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
package l2e.gameserver.data.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.AccessLevel;
import l2e.gameserver.model.AdminCommandAccessRight;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class AdminParser extends DocumentParser
{
	private final Map<Integer, AccessLevel> _accessLevels = new HashMap<>();
	private final Map<String, AdminCommandAccessRight> _adminCommandAccessRights = new HashMap<>();
	private final Map<Player, Boolean> _gmList = new ConcurrentHashMap<>();
	private int _highestLevel = 0;
	
	protected AdminParser()
	{
		load();
	}

	@Override
	public synchronized void load()
	{
		_accessLevels.clear();
		_adminCommandAccessRights.clear();
		parseDatapackFile("data/stats/admin/accessLevels.xml");
		parseDatapackFile("data/stats/admin/adminCommands.xml");
		info("Loaded: " + _accessLevels.size() + " access levels and " + _adminCommandAccessRights.size() + " access commands.");
	}

	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		Node attr;
		for (var n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (var d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("access".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final StatsSet set = new StatsSet();
						set.set("level", Integer.parseInt(attrs.getNamedItem("level").getNodeValue()));
						set.set("name", attrs.getNamedItem("name").getNodeValue());
						set.set("nameColor", attrs.getNamedItem("nameColor").getNodeValue());
						set.set("titleColor", attrs.getNamedItem("titleColor").getNodeValue());
						set.set("childAccess", attrs.getNamedItem("childAccess") != null ? Integer.parseInt(attrs.getNamedItem("childAccess").getNodeValue()) : 0);
						set.set("isGM", Boolean.parseBoolean(attrs.getNamedItem("isGM").getNodeValue()));
						for (var s = d.getFirstChild(); s != null; s = s.getNextSibling())
						{
							if ("set".equalsIgnoreCase(s.getNodeName()))
							{
								set.set(s.getAttributes().getNamedItem("name").getNodeValue(), s.getAttributes().getNamedItem("val").getNodeValue());
							}
						}
						final AccessLevel level = new AccessLevel(set);
						if (level.getLevel() > _highestLevel)
						{
							_highestLevel = level.getLevel();
						}
						_accessLevels.put(level.getLevel(), level);
					}
					else if ("admin".equalsIgnoreCase(d.getNodeName()))
					{
						final StatsSet set = new StatsSet();
						final NamedNodeMap attrs = d.getAttributes();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							attr = attrs.item(i);
							set.set(attr.getNodeName(), attr.getNodeValue());
						}
						final AdminCommandAccessRight command = new AdminCommandAccessRight(set);
						_adminCommandAccessRights.put(command.getAdminCommand(), command);
					}
				}
			}
		}
	}

	public AccessLevel getAccessLevel(int accessLevelNum)
	{
		if (accessLevelNum < 0)
		{
			return _accessLevels.get(-1);
		}
		else if (!_accessLevels.containsKey(accessLevelNum))
		{
			_accessLevels.put(accessLevelNum, new AccessLevel());
		}
		return _accessLevels.get(accessLevelNum);
	}

	public AccessLevel getMasterAccessLevel()
	{
		return _accessLevels.get(_highestLevel);
	}

	public boolean hasAccessLevel(int id)
	{
		return _accessLevels.containsKey(id);
	}

	public boolean hasAccess(String adminCommand, AccessLevel accessLevel)
	{
		var acar = _adminCommandAccessRights.get(adminCommand);
		if (acar == null)
		{
			if ((accessLevel.getLevel() > 0) && (accessLevel.getLevel() == _highestLevel))
			{
				acar = new AdminCommandAccessRight(adminCommand, true, accessLevel.getLevel());
				_adminCommandAccessRights.put(adminCommand, acar);
				info("No rights defined for admin command " + adminCommand + " auto setting accesslevel: " + accessLevel.getLevel() + " !");
			}
			else
			{
				info("No rights defined for admin command " + adminCommand + " !");
				return false;
			}
		}
		return acar.hasAccess(accessLevel);
	}

	public boolean requireConfirm(String command)
	{
		final var acar = _adminCommandAccessRights.get(command);
		if (acar == null)
		{
			info("No rights defined for admin command " + command + ".");
			return false;
		}
		return acar.getRequireConfirm();
	}

	public List<Player> getAllGms(boolean includeHidden)
	{
		final List<Player> tmpGmList = new ArrayList<>();

		for (final Entry<Player, Boolean> entry : _gmList.entrySet())
		{
			if (includeHidden || !entry.getValue())
			{
				tmpGmList.add(entry.getKey());
			}
		}

		return tmpGmList;
	}

	public List<String> getAllGmNames(boolean includeHidden)
	{
		final List<String> tmpGmList = new ArrayList<>();

		for (final Entry<Player, Boolean> entry : _gmList.entrySet())
		{
			if (!entry.getValue())
			{
				tmpGmList.add(entry.getKey().getName(null));
			}
			else if (includeHidden)
			{
				tmpGmList.add(entry.getKey().getName(null) + " (invis)");
			}
		}

		return tmpGmList;
	}

	public void addGm(Player player, boolean hidden)
	{
		_gmList.put(player, hidden);
	}

	public void deleteGm(Player player)
	{
		_gmList.remove(player);
	}

	public void showGm(Player player)
	{
		if (_gmList.containsKey(player))
		{
			_gmList.put(player, false);
		}
	}

	public void hideGm(Player player)
	{
		if (_gmList.containsKey(player))
		{
			_gmList.put(player, true);
		}
	}

	public boolean isGmOnline(boolean includeHidden)
	{
		for (final Entry<Player, Boolean> entry : _gmList.entrySet())
		{
			if (includeHidden || !entry.getValue())
			{
				return true;
			}
		}

		return false;
	}

	public void sendListToPlayer(Player player)
	{
		if (isGmOnline(player.isGM()))
		{
			player.sendPacket(SystemMessageId.GM_LIST);

			for (final var name : getAllGmNames(player.isGM()))
			{
				final var sm = SystemMessage.getSystemMessage(SystemMessageId.GM_C1);
				sm.addString(name);
				player.sendPacket(sm);
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.NO_GM_PROVIDING_SERVICE_NOW);
		}
	}

	public void broadcastToGMs(GameServerPacket packet)
	{
		for (final var gm : getAllGms(true))
		{
			gm.sendPacket(packet);
		}
	}

	public void broadcastMessageToGMs(String message)
	{
		for (final var gm : getAllGms(true))
		{
			gm.sendMessage(message);
		}
	}

	public static AdminParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final AdminParser _instance = new AdminParser();
	}
}