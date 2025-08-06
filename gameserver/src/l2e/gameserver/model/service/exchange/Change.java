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
package l2e.gameserver.model.service.exchange;

import java.util.List;

public class Change
{
	final int _id;
	final String _name;
	final String _icon;
	final int _cost_id;
	final long _cost_count;
	final boolean _attribute_change;
	final boolean _is_upgrade;
	final List<Variant> _variants;

	public Change(int id, String name, String icon, int cost_id, long cost_count, boolean attribute_change, boolean is_upgrade, List<Variant> variants)
	{
		_id = id;
		_name = name;
		_icon = icon;
		_cost_id = cost_id;
		_cost_count = cost_count;
		_attribute_change = attribute_change;
		_is_upgrade = is_upgrade;
		_variants = variants;
	}

	public int getId()
	{
		return _id;
	}

	public String getName()
	{
		return _name;
	}

	public String getIcon()
	{
		return _icon;
	}

	public int getCostId()
	{
		return _cost_id;
	}

	public long getCostCount()
	{
		return _cost_count;
	}

	public boolean attChange()
	{
		return _attribute_change;
	}

	public boolean isUpgrade()
	{
		return _is_upgrade;
	}

	public List<Variant> getList()
	{
		return _variants;
	}

	public Variant getVariant(int id)
	{
		for (final Variant var : _variants)
		{
			if (var.getNumber() == id)
			{
				return var;
			}
		}
		return null;
	}
}