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
package l2e.gameserver.model.service.donate;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.stats.StatsSet;

public class Donation
{
	private static final Logger _log = LoggerFactory.getLogger(Donation.class);
	
	private final int _id;
	private final StatsSet _params;
	private final String _icon;
	private final int _group;
	private final boolean _havefound;
	private SimpleList _simple;
	private FoundList _found;
	private Enchant _enchant;
	private Attribution _attribution;

	public Donation(int id, StatsSet params, String icon, int group, boolean havefound)
	{
		_id = id;
		_params = params;
		_icon = icon;
		_group = group;
		_havefound = havefound;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "name" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public String getIcon()
	{
		return _icon;
	}
	
	public int getGroup()
	{
		return _group;
	}
	
	public boolean haveFound()
	{
		return _havefound;
	}
	
	public void addSimple(SimpleList list)
	{
		_simple = list;
	}
	
	public SimpleList getSimple()
	{
		return _simple;
	}
	
	public void addFound(FoundList list)
	{
		_found = list;
	}
	
	public FoundList getFound()
	{
		return _found;
	}
	
	public Enchant getEnchant()
	{
		return _enchant;
	}
	
	public void setEnchant(Enchant enchant)
	{
		_enchant = enchant;
	}
	
	public Attribution getAttribution()
	{
		return _attribution;
	}
	
	public void setAttribution(Attribution att)
	{
		_attribution = att;
	}
	
	public void print()
	{
		Iterator<DonateItem> i = _simple.getList().iterator();

		_log.info("=========== Donate: " + getName(null) + " (id: " + _id + ") ===========");
		_log.info("=== Icon: " + _icon);
		_log.info("=== Group: " + _group);
		_log.info("=== Have found: " + _havefound);
		_log.info("=== Simple items:");

		DonateItem item;
		while (i.hasNext())
		{
			item = i.next();
			_log.info("====> Item:" + ItemsParser.getInstance().getTemplate(item.getId()).getName(null) + " (id: " + item.getId() + ")");
			_log.info("====> Count: " + item.getCount());
			_log.info("====> Enchant: " + item.getEnchant());
		}
		
		if (_havefound)
		{
			_log.info("=== Foundation items:");
			i = _found.getList().iterator();
			
			while (i.hasNext())
			{
				item = i.next();
				_log.info("====> Item:" + ItemsParser.getInstance().getTemplate(item.getId()).getName(null) + " (id: " + item.getId() + ")");
				_log.info("====> Count: " + item.getCount());
				_log.info("====> Enchant: " + item.getEnchant());
			}
		}
		
		_log.info("=== Enchant: cost -> " + _enchant.getCount() + " " + _enchant.getId() + ", value -> " + _enchant.getEnchant());
		_log.info("=== Attribution: cost -> " + _attribution.getCount() + " " + _attribution.getId() + ", value -> " + _attribution.getValue() + ", size (Element count) -> " + _attribution.getSize());
	}
}