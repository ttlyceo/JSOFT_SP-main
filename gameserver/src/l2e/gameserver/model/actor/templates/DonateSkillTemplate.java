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
import java.util.Map;

import l2e.gameserver.model.holders.ItemHolder;

/**
 * Created by LordWinter
 */
public class DonateSkillTemplate
{
	private final int _skillId;
	private final int _skillLevel;
	private final boolean _isClanSkill;
	private final Map<Long, List<ItemHolder>> _requestItems;
	private final List<Long> _times;
	
	public DonateSkillTemplate(int skillId, int skillLevel, boolean isClanSkill, List<Long> times, Map<Long, List<ItemHolder>> requestItems)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
		_isClanSkill = isClanSkill;
		_times = times;
		_requestItems = requestItems;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public int getSkillLevel()
	{
		return _skillLevel;
	}
	
	public boolean isClanSkill()
	{
		return _isClanSkill;
	}
	
	public List<Long> getTimes()
	{
		return _times;
	}
	
	public Map<Long, List<ItemHolder>> getRequestItems()
	{
		return _requestItems;
	}
}