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

import java.util.Collection;

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.HashIntObjectMap;
import org.w3c.dom.Node;

import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.petition.PetitionMainGroup;
import l2e.gameserver.model.petition.PetitionSection;

public class PetitionGroupParser extends DocumentParser
{
	private final IntObjectMap<PetitionMainGroup> _petitionGroups = new HashIntObjectMap<>();
	
	protected PetitionGroupParser()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_petitionGroups.clear();
		parseDatapackFile("data/stats/admin/petitions.xml");
		info("Loaded: " + _petitionGroups.size() + " petition groups.");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node c = getCurrentDocument().getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("list".equalsIgnoreCase(c.getNodeName()))
			{
				for (Node d = c.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("petition".equalsIgnoreCase(d.getNodeName()))
					{
						final PetitionMainGroup group = new PetitionMainGroup(Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue()));
						_petitionGroups.put(group.getId(), group);

						for (Node n = d.getFirstChild(); n != null; n = n.getNextSibling())
						{
							if ("name".equalsIgnoreCase(n.getNodeName()))
							{
								group.setName(n.getAttributes().getNamedItem("lang").getNodeValue(), n.getAttributes().getNamedItem("val").getNodeValue());
							}
							else if ("descr".equals(n.getNodeName()))
							{
								group.setDescription(n.getAttributes().getNamedItem("lang").getNodeValue(), n.getAttributes().getNamedItem("val").getNodeValue());
							}
							else if ("section".equals(n.getNodeName()))
							{
								final PetitionSection subGroup = new PetitionSection(Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue()));
								group.addSubGroup(subGroup);
								for (Node e = n.getFirstChild(); e != null; e = e.getNextSibling())
								{
									if ("name".equalsIgnoreCase(e.getNodeName()))
									{
										subGroup.setName(e.getAttributes().getNamedItem("lang").getNodeValue(), e.getAttributes().getNamedItem("val").getNodeValue());
									}
									else if ("descr".equals(e.getNodeName()))
									{
										subGroup.setDescription(e.getAttributes().getNamedItem("lang").getNodeValue(), e.getAttributes().getNamedItem("val").getNodeValue());
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public PetitionMainGroup getPetitionGroup(int val)
	{
		return _petitionGroups.get(val);
	}

	public Collection<PetitionMainGroup> getPetitionGroups()
	{
		return _petitionGroups.valueCollection();
	}
	
	public static PetitionGroupParser getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PetitionGroupParser _instance = new PetitionGroupParser();
	}
}
