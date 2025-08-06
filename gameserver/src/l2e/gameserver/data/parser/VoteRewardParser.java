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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.data.DocumentParser;
import l2e.gameserver.model.entity.mods.votereward.VoteRewardSite;
import l2e.gameserver.model.reward.RewardList;
import l2e.gameserver.model.reward.RewardType;

public final class VoteRewardParser extends DocumentParser
{
	private final Map<String, VoteRewardSite> _voteRewardSites = new HashMap<>();
	
	private int _minLevel;
	private boolean _isFullCheckSites;
	
	protected VoteRewardParser()
	{
		_voteRewardSites.clear();
		load();
		info("Loaded " + _voteRewardSites.size() + " vote reward sites templates.");
		callInit();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("config/mods/votereward.xml");
	}
	
	@Override
	protected void reloadDocument()
	{
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void parseDocument()
	{
		for (Node c = getCurrentDocument().getFirstChild(); c != null; c = c.getNextSibling())
		{
			if ("list".equalsIgnoreCase(c.getNodeName()))
			{
				_minLevel = Integer.parseInt(c.getAttributes().getNamedItem("minLevel").getNodeValue());
				_isFullCheckSites = Boolean.parseBoolean(c.getAttributes().getNamedItem("isFullCheckSites").getNodeValue());
				for (Node npc = c.getFirstChild(); npc != null; npc = npc.getNextSibling())
				{
					if ("vote".equalsIgnoreCase(npc.getNodeName()))
					{
						NamedNodeMap attrs = npc.getAttributes();

						final String impl = attrs.getNamedItem("impl").getNodeValue();
						Class<VoteRewardSite> voteRewardSiteClass = null;
						try
						{
							voteRewardSiteClass = (Class<VoteRewardSite>) Class.forName("l2e.gameserver.model.entity.mods.votereward.impl." + impl + "Site");
						}
						catch (final ClassNotFoundException e)
						{
							try
							{
								voteRewardSiteClass = (Class<VoteRewardSite>) Class.forName("l2e.scripts.votereward." + impl + "Site");
							}
							catch (final ClassNotFoundException e1)
							{
								e1.printStackTrace();
							}
						}
						
						if (voteRewardSiteClass == null)
						{
							info("Not found impl class: " + impl);
							continue;
						}
						
						final boolean enabled = Boolean.parseBoolean(attrs.getNamedItem("enabled").getNodeValue());
						
						Constructor<VoteRewardSite> constructor = null;
						try
						{
							constructor = voteRewardSiteClass.getConstructor(MultiValueSet.class);
						}
						catch (
						    IllegalArgumentException | NoSuchMethodException | SecurityException e)
						{
							warn("Unable to create voteRewardSiteClass!");
							e.printStackTrace();
						}
						
						final MultiValueSet<String> parameters = new MultiValueSet<>();
						parameters.set("name", impl);
						parameters.set("enabled", enabled);
						parameters.set("run_delay", attrs.getNamedItem("run_delay") != null ? Integer.parseInt(attrs.getNamedItem("run_delay").getNodeValue()) : 0);
						for (Node cat = npc.getFirstChild(); cat != null; cat = cat.getNextSibling())
						{
							if ("parameter".equalsIgnoreCase(cat.getNodeName()))
							{
								attrs = cat.getAttributes();
								parameters.set(attrs.getNamedItem("name").getNodeValue(), attrs.getNamedItem("value").getNodeValue());
							}
						}
						
						VoteRewardSite voteRewardSite = null;
						try
						{
							voteRewardSite = constructor.newInstance(parameters);
						}
						catch (
						    IllegalAccessException | InvocationTargetException | InstantiationException | IllegalArgumentException e)
						{
							warn("Unable to create voteRewardSite!");
							e.printStackTrace();
						}
						
						for (Node cat = npc.getFirstChild(); cat != null; cat = cat.getNextSibling())
						{
							if ("rewards".equalsIgnoreCase(cat.getNodeName()))
							{
								voteRewardSite.addRewardList(RewardList.parseRewardList(_log, cat, cat.getAttributes(), RewardType.NOT_RATED_GROUPED, false, impl));
							}
						}
						
						if (enabled)
						{
							addVoteRewardSite(voteRewardSite);
						}
					}
				}
			}
		}
	}
	
	public void addVoteRewardSite(VoteRewardSite site)
	{
		if (_voteRewardSites.containsKey(site.getName()))
		{
			warn(String.format("Dublicate %s Vote Site registered!", site.getName()));
		}
		_voteRewardSites.put(site.getName(), site);
	}

	public Map<String, VoteRewardSite> getVoteRewardSites()
	{
		return _voteRewardSites;
	}

	private void callInit()
	{
		for (final VoteRewardSite site : _voteRewardSites.values())
		{
			site.init();
		}
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public boolean isFullCheckSites()
	{
		return _isFullCheckSites;
	}
	
	public static VoteRewardParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final VoteRewardParser _instance = new VoteRewardParser();
	}
}