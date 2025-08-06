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
package l2e.gameserver.model.actor.templates.items;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;

public class Henna
{
	private final int _dyeId;
	private final String _dyeName;
	private final int _dyeItemId;
	private final int _str;
	private final int _con;
	private final int _dex;
	private final int _int;
	private final int _men;
	private final int _wit;
	private final int _wear_fee;
	private final int _wear_count;
	private final int _cancel_fee;
	private final int _cancel_count;
	private final List<ClassId> _wear_class;
	private final List<Skill> _skillList;

	public Henna(StatsSet set)
	{
		_dyeId = set.getInteger("dyeId");
		_dyeName = set.getString("dyeName");
		_dyeItemId = set.getInteger("dyeItemId");
		_str = set.getInteger("str");
		_con = set.getInteger("con");
		_dex = set.getInteger("dex");
		_int = set.getInteger("int");
		_men = set.getInteger("men");
		_wit = set.getInteger("wit");
		_wear_fee = set.getInteger("wear_fee");
		_wear_count = set.getInteger("wear_count");
		_cancel_fee = set.getInteger("cancel_fee");
		_cancel_count = set.getInteger("cancel_count");
		_wear_class = new ArrayList<>();
		_skillList = new ArrayList<>();
	}

	public int getDyeId()
	{
		return _dyeId;
	}

	public String getDyeName()
	{
		return _dyeName;
	}

	public int getDyeItemId()
	{
		return _dyeItemId;
	}

	public int getStatSTR()
	{
		return _str;
	}

	public int getStatCON()
	{
		return _con;
	}

	public int getStatDEX()
	{
		return _dex;
	}

	public int getStatINT()
	{
		return _int;
	}

	public int getStatMEN()
	{
		return _men;
	}

	public int getStatWIT()
	{
		return _wit;
	}

	public int getWearFee()
	{
		return _wear_fee;
	}

	public int getWearCount()
	{
		return _wear_count;
	}

	public int getCancelFee()
	{
		return _cancel_fee;
	}

	public int getCancelCount()
	{
		return _cancel_count;
	}

	public List<ClassId> getAllowedWearClass()
	{
		return _wear_class;
	}

	public boolean isAllowedClass(ClassId c)
	{
		return _wear_class.contains(c);
	}

	public void setWearClassIds(List<ClassId> wearClassIds)
	{
		_wear_class.addAll(wearClassIds);
	}
	
	public void setSkills(List<Skill> skillList)
	{
		_skillList.addAll(skillList);
	}
	
	public List<Skill> getSkillList()
	{
		return _skillList;
	}
}