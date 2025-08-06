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
package l2e.gameserver.model.actor.templates;

import java.util.List;

import l2e.gameserver.Config;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public class DressArmorTemplate
{
	private final StatsSet _params;
	private final int _priceId;
	private final long _priceCount;
	private final List<Skill> _skills;
	private final double _removeModifier;

	public DressArmorTemplate(StatsSet params, int priceId, long priceCount, List<Skill> skills, double removeModifier)
	{
		_params = params;
		_priceId = priceId;
		_priceCount = priceCount;
		_skills = skills;
		_removeModifier = removeModifier;
	}

	public int getId()
	{
		return _params.getInteger("id");
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

	public int getChest()
	{
		return _params.getInteger("chest");
	}

	public int getLegs()
	{
		return _params.getInteger("legs");
	}

	public int getGloves()
	{
		return _params.getInteger("gloves");
	}

	public int getFeet()
	{
		return _params.getInteger("feet");
	}
	
	public int getShieldId()
	{
		return _params.getInteger("shield");
	}
	
	public int getCloakId()
	{
		return _params.getInteger("cloak");
	}
	
	public int getHatId()
	{
		return _params.getInteger("hat");
	}
	
	public int getSlot()
	{
		return _params.getInteger("slot");
	}

	public int getPriceId()
	{
		return _priceId;
	}

	public long getPriceCount()
	{
		return _priceCount;
	}
	
	public boolean isForKamael()
	{
		return _params.getBool("isForKamael");
	}
	
	public boolean isCheckEquip()
	{
		return _params.getBool("checkEquip");
	}
	
	public List<Skill> getSkills()
	{
		return _skills;
	}
	
	public double getRemoveModifier()
	{
		return _removeModifier;
	}
}