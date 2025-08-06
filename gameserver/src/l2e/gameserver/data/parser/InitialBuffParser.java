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

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.items.PcItemTemplate;
import l2e.gameserver.model.skills.PcBuffTemplate;
import l2e.gameserver.model.stats.StatsSet;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InitialBuffParser extends DocumentParser
{
	private boolean activated;
	private final Map<ClassId, List<PcBuffTemplate>> initialBuffList = new HashMap<>();

	protected InitialBuffParser()
	{
		load();
	}

	@Override
	public void load()
	{
		initialBuffList.clear();
		parseDatapackFile("data/stats/chars/initialBuff.xml");
		info("Loaded " + initialBuffList.size() + " initial buff data.");
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
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("newbieBuff".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
                        this.activated = Boolean.parseBoolean(attrs.getNamedItem("activated").getNodeValue());
					}
					else if ("class".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						Node attr;
						final ClassId classId = ClassId.getClassId(Integer.parseInt(attrs.getNamedItem("classId").getNodeValue()));
						final List<PcBuffTemplate> equipList = new ArrayList<>();
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("buff".equalsIgnoreCase(c.getNodeName()))
							{
								final StatsSet set = new StatsSet();
								attrs = c.getAttributes();
								for (int i = 0; i < attrs.getLength(); i++)
								{
									attr = attrs.item(i);
									set.set(attr.getNodeName(), attr.getNodeValue());
								}
								equipList.add(new PcBuffTemplate(set));
							}
						}
						initialBuffList.put(classId, equipList);
					}
				}
			}
		}
	}

	public List<PcBuffTemplate> getBuffList(ClassId cId)
	{
		return initialBuffList.get(cId);
	}

	public List<PcBuffTemplate> getBuffList(int cId)
	{
		return initialBuffList.get(ClassId.getClassId(cId));
	}

	public static InitialBuffParser getInstance()
	{
		return SingletonHolder._instance;
	}

	public boolean isActivated() {
		return activated;
	}

	private static class SingletonHolder
	{
		protected static final InitialBuffParser _instance = new InitialBuffParser();
	}
}