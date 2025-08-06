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
package l2e.gameserver.instancemanager.mods;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.EnchantSkillGroupsParser;
import l2e.gameserver.model.actor.templates.items.EnchantItemTemplate;

/**
 * Created by LordWinter
 */
public class EnchantSkillManager extends LoggerObject
{
	private final Map<Integer, EnchantItemTemplate> _templates = new HashMap<>();
	
	public EnchantSkillManager()
	{
		_templates.clear();
		parseSettings();
		if (_templates.size() > 0)
		{
			info("Loaded " + _templates.size() + " enchant item templates for skills.");
		}
	}
	
	private void parseSettings()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/services/enchantSkillItems.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);

			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					for (Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("skill".equalsIgnoreCase(d1.getNodeName()))
						{
							final Map<Integer, int[]> types = new HashMap<>();
							final int id = Integer.parseInt(d1.getAttributes().getNamedItem("id").getNodeValue());
							parseItemsList(d1.getAttributes().getNamedItem("normalEnchant").getNodeValue(), 0, types);
							parseItemsList(d1.getAttributes().getNamedItem("saveEnchant").getNodeValue(), 1, types);
							parseItemsList(d1.getAttributes().getNamedItem("untrainEnchant").getNodeValue(), 2, types);
							parseItemsList(d1.getAttributes().getNamedItem("changeEnchant").getNodeValue(), 3, types);
							_templates.put(id, new EnchantItemTemplate(types));
						}
					}
				}
			}
		}
		catch (NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("EenchantSkillItems.xml could not be initialized.", e);
		}
		catch (IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	private void parseItemsList(String line, int type, Map<Integer, int[]> types)
	{
		final String[] value = line.split(",");
		if (value.length != 2)
		{
			warn("invalid parseItemsList entry -> " + value[0] + ", should be itemId,itemNumber");
			return;
		}
		final int[] info = new int[2];
		info[0] = Integer.parseInt(value[0]);
		info[1] = Integer.parseInt(value[1]);
		types.put(type, info);
	}
	
	public int[] getEnchantItems(int skillId, int type, int skillLevel)
	{
		if (_templates.containsKey(skillId))
		{
			final int[] info = _templates.get(skillId).getType(type);
			if (info != null)
			{
				if (type == 0)
				{
					if ((skillLevel % 100) > 1)
					{
						return new int[]
						{
						        info[0], 0
						};
					}
					else
					{
						return info;
					}
				}
				else
				{
					return info;
				}
			}
		}
		
		switch (type)
		{
			case 0 :
				return new int[]
				{
				        EnchantSkillGroupsParser.NORMAL_ENCHANT_BOOK, 1
				};
			case 1 :
				return new int[]
				{
				        EnchantSkillGroupsParser.SAFE_ENCHANT_BOOK, 1
				};
			case 2 :
				return new int[]
				{
				        EnchantSkillGroupsParser.UNTRAIN_ENCHANT_BOOK, 1
				};
			case 3 :
				return new int[]
				{
				        EnchantSkillGroupsParser.CHANGE_ENCHANT_BOOK, 1
				};
		}
		
		return new int[]
		{
		        0, 0
		};
	}
	
	public static final EnchantSkillManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantSkillManager _instance = new EnchantSkillManager();
	}
}