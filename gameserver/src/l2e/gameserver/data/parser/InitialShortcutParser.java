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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.Macro;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.MacroTemplate;
import l2e.gameserver.model.actor.templates.ShortCutTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.MacroType;
import l2e.gameserver.model.base.ShortcutType;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.serverpackets.ShortCutRegister;

public final class InitialShortcutParser extends DocumentParser
{
	private final Map<ClassId, List<ShortCutTemplate>> _initialShortcutData = new HashMap<>();
	private final List<ShortCutTemplate> _initialGlobalShortcutList = new ArrayList<>();
	private final Map<Integer, Macro> _macroPresets = new HashMap<>();
	
	protected InitialShortcutParser()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_initialShortcutData.clear();
		_initialGlobalShortcutList.clear();
		_macroPresets.clear();
		
		parseDatapackFile("data/stats/chars/initialShortcuts.xml");
		
		info("Loaded " + _initialGlobalShortcutList.size() + " initial global shortcuts data.");
		info("Loaded " + _initialShortcutData.size() + " initial shortcuts data.");
		info("Loaded " + _macroPresets.size() + " macros presets.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					switch (d.getNodeName())
					{
						case "shortcuts":
						{
							parseShortcuts(d);
							break;
						}
						case "macros":
						{
							parseMacros(d);
							break;
						}
					}
				}
			}
		}
	}
	
	private void parseShortcuts(Node d)
	{
		NamedNodeMap attrs = d.getAttributes();
		final Node classIdNode = attrs.getNamedItem("classId");
		final List<ShortCutTemplate> list = new ArrayList<>();
		for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("page".equals(c.getNodeName()))
			{
				attrs = c.getAttributes();
				final int pageId = parseInteger(attrs, "pageId");
				for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("slot".equals(b.getNodeName()))
					{
						list.add(createShortcut(pageId, b));
					}
				}
			}
		}
		
		if (classIdNode != null)
		{
			_initialShortcutData.put(ClassId.getClassId(Integer.parseInt(classIdNode.getNodeValue())), list);
		}
		else
		{
			_initialGlobalShortcutList.addAll(list);
		}
	}
	
	private void parseMacros(Node d)
	{
		for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("macro".equals(c.getNodeName()))
			{
				NamedNodeMap attrs = c.getAttributes();
				if (!parseBoolean(attrs, "enabled", true))
				{
					continue;
				}
				
				final int macroId = parseInteger(attrs, "macroId");
				final int icon = parseInteger(attrs, "icon");
				final String name = parseString(attrs, "name");
				final String description = parseString(attrs, "description");
				final String acronym = parseString(attrs, "acronym");
				final List<MacroTemplate> commands = new ArrayList<>(1);
				int entry = 0;
				for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("command".equals(b.getNodeName()))
					{
						attrs = b.getAttributes();
						final MacroType type = parseEnum(attrs, MacroType.class, "type");
						int d1 = 0;
						int d2 = 0;
						final String cmd = b.getTextContent();
						switch (type)
						{
							case SKILL:
							{
								d1 = parseInteger(attrs, "skillId");
								d2 = parseInteger(attrs, "skillLvl", 0);
								break;
							}
							case ACTION:
							{
								d1 = parseInteger(attrs, "actionId");
								break;
							}
							case TEXT:
							{
								break;
							}
							case SHORTCUT:
							{
								d1 = parseInteger(attrs, "page");
								d2 = parseInteger(attrs, "slot", 0);
								break;
							}
							case ITEM:
							{
								d1 = parseInteger(attrs, "itemId");
								break;
							}
							case DELAY:
							{
								d1 = parseInteger(attrs, "delay");
								break;
							}
						}
						commands.add(new MacroTemplate(entry++, type, d1, d2, cmd));
					}
				}
				_macroPresets.put(macroId, new Macro(macroId, icon, name, description, acronym, commands));
			}
		}
	}
	
	private ShortCutTemplate createShortcut(int pageId, Node b)
	{
		final NamedNodeMap attrs = b.getAttributes();
		final int slotId = parseInteger(attrs, "slotId");
		final ShortcutType shortcutType = parseEnum(attrs, ShortcutType.class, "shortcutType");
		final int shortcutId = parseInteger(attrs, "shortcutId");
		final int shortcutLevel = parseInteger(attrs, "shortcutLevel", 0);
		final int characterType = parseInteger(attrs, "characterType", 0);
		return new ShortCutTemplate(slotId, pageId, shortcutType, shortcutId, shortcutLevel, characterType);
	}
	
	public List<ShortCutTemplate> getShortcutList(ClassId cId)
	{
		return _initialShortcutData.get(cId);
	}
	
	public List<ShortCutTemplate> getShortcutList(int cId)
	{
		return _initialShortcutData.get(ClassId.getClassId(cId));
	}
	
	public List<ShortCutTemplate> getGlobalMacroList()
	{
		return _initialGlobalShortcutList;
	}
	
	public void registerAllShortcuts(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		for (final ShortCutTemplate shortcut : _initialGlobalShortcutList)
		{
			int shortcutId = shortcut.getId();
			switch (shortcut.getType())
			{
				case ITEM:
				{
					final ItemInstance item = player.getInventory().getItemByItemId(shortcutId);
					if (item == null)
					{
						continue;
					}
					shortcutId = item.getObjectId();
					break;
				}
				case SKILL:
				{
					if (!player.getSkills().containsKey(shortcutId))
					{
						continue;
					}
					break;
				}
				case MACRO:
				{
					final Macro macro = _macroPresets.get(shortcutId);
					if (macro == null)
					{
						continue;
					}
					player.registerMacro(macro);
					break;
				}
			}
			
			final ShortCutTemplate newShortcut = new ShortCutTemplate(shortcut.getSlot(), shortcut.getPage(), shortcut.getType(), shortcutId, shortcut.getLevel(), shortcut.getCharacterType());
			player.sendPacket(new ShortCutRegister(newShortcut));
			player.registerShortCut(newShortcut);
		}
		
		if (_initialShortcutData.containsKey(player.getClassId()))
		{
			for (final ShortCutTemplate shortcut : _initialShortcutData.get(player.getClassId()))
			{
				int shortcutId = shortcut.getId();
				switch (shortcut.getType())
				{
					case ITEM:
					{
						final ItemInstance item = player.getInventory().getItemByItemId(shortcutId);
						if (item == null)
						{
							continue;
						}
						shortcutId = item.getObjectId();
						break;
					}
					case SKILL:
					{
						if (!player.getSkills().containsKey(shortcut.getId()))
						{
							continue;
						}
						break;
					}
					case MACRO:
					{
						final Macro macro = _macroPresets.get(shortcutId);
						if (macro == null)
						{
							continue;
						}
						player.registerMacro(macro);
						break;
					}
				}
				final ShortCutTemplate newShortcut = new ShortCutTemplate(shortcut.getSlot(), shortcut.getPage(), shortcut.getType(), shortcutId, shortcut.getLevel(), shortcut.getCharacterType());
				player.sendPacket(new ShortCutRegister(newShortcut));
				player.registerShortCut(newShortcut);
			}
		}
	}
	
	public static InitialShortcutParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final InitialShortcutParser _instance = new InitialShortcutParser();
	}
}