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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.PvpColorTempate;

/**
 * Created by LordWinter
 */
public class PvpColorManager extends LoggerObject
{
	private final List<PvpColorTempate> _templates = new ArrayList<>();
	private boolean _isActive = false;
	private int _minPvp = 0;
	
	public PvpColorManager()
	{
		_templates.clear();
		parseSettings();
		if (isActive() && _templates.size() > 0)
		{
			checkTemplates();
		}
		info("Loaded " + _templates.size() + " templates.");
	}
	
	private void parseSettings()
	{
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/stats/services/pvpColor.xml");
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final Document doc1 = factory.newDocumentBuilder().parse(file);

			for (Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					_isActive = Boolean.parseBoolean(n1.getAttributes().getNamedItem("allowSystem").getNodeValue());
					for (Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("pvp".equalsIgnoreCase(d1.getNodeName()))
						{
							final int id = Integer.parseInt(d1.getAttributes().getNamedItem("id").getNodeValue());
							final int request = Integer.parseInt(d1.getAttributes().getNamedItem("request").getNodeValue());
							final int color = Integer.decode("0x" + d1.getAttributes().getNamedItem("color").getNodeValue());
							final int titleColor = Integer.decode("0x" + d1.getAttributes().getNamedItem("titleColor").getNodeValue());
							final String title = d1.getAttributes().getNamedItem("title").getNodeValue();
							_templates.add(new PvpColorTempate(id, request, color, titleColor, title));
						}
					}
				}
			}
		}
		catch (NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("PpvpColor.xml could not be initialized.", e);
		}
		catch (IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	private void checkTemplates()
	{
		for (int i = 0; i < _templates.size(); i++)
		{
			final PvpColorTempate template = _templates.get(i);
			if (template != null)
			{
				if (template.getId() == 1)
				{
					_minPvp = template.getRequest();
				}
				
				final PvpColorTempate tpl = getTemplate(template.getId() + 1);
				if (tpl != null)
				{
					template.setNextCheck(tpl.getRequest());
				}
				else
				{
					template.setLast();
				}
			}
		}
	}
	
	private PvpColorTempate getTemplate(int id)
	{
		for (final PvpColorTempate tpl : _templates)
		{
			if (tpl != null && tpl.getId() == id)
			{
				return tpl;
			}
		}
		return null;
	}
	
	public int getPlayerTitleColor(Player player)
	{
		if (!isActive() || player == null || player.getPvpKills() < _minPvp)
		{
			return 0xFFFF77;
		}
		
		final PvpColorTempate tpl = checkColorTemplate(player);
		if (tpl != null)
		{
			return tpl.getTitleColor();
		}
		return 0xFFFF77;
	}
	
	public int getPlayerNameColor(Player player)
	{
		if (!isActive() || player == null || player.getPvpKills() < _minPvp)
		{
			return 0xFFFFFF;
		}
		
		final PvpColorTempate tpl = checkColorTemplate(player);
		if (tpl != null)
		{
			return tpl.getColor();
		}
		return 0xFFFFFF;
	}
	
	public String getPlayerTitle(Player player)
	{
		if (!isActive() || player == null || player.getPvpKills() < _minPvp)
		{
			return "";
		}
		
		final PvpColorTempate tpl = checkColorTemplate(player);
		if (tpl != null)
		{
			return tpl.getTitle();
		}
		return "";
	}
	
	private PvpColorTempate checkColorTemplate(Player player)
	{
		for (final PvpColorTempate tpl : _templates)
		{
			if (tpl != null)
			{
				if (!tpl.isLast() && (player.getPvpKills() >= tpl.getRequest() && (player.getPvpKills() < tpl.getLimit())))
				{
					return tpl;
				}
				else if (tpl.isLast() && player.getPvpKills() >= tpl.getRequest())
				{
					return tpl;
				}
			}
		}
		return null;
	}
	
	public boolean isActive()
	{
		return _isActive;
	}
	
	public static final PvpColorManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PvpColorManager _instance = new PvpColorManager();
	}
}