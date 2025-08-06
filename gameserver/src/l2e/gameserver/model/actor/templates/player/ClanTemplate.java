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
package l2e.gameserver.model.actor.templates.player;

import java.util.List;

import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.skills.Skill;

/**
 * Created by LordWinter
 */
public class ClanTemplate
{
	private final int _level;
	private final int _sp;
	private final List<ItemHolder> _requestItems;
	private final List<ItemHolder> _rewardItems;
	private final List<Skill> _rewardSkills;
	private final int _requestMembers;
	private final int _membersLimit;
	private final int _academyLimit;
	private final int _royalLimit;
	private final int _knightLimit;
	private final boolean _haveTerritory;
	
	public ClanTemplate(int level, int sp, List<ItemHolder> requestItems, List<ItemHolder> rewardItems, List<Skill> rewardSkills, int requestMembers, int membersLimit, int academyLimit, int royalLimit, int knightLimit, boolean haveTerritory)
	{
		_level = level;
		_sp = sp;
		_requestItems = requestItems;
		_rewardItems = rewardItems;
		_rewardSkills = rewardSkills;
		_requestMembers = requestMembers;
		_membersLimit = membersLimit;
		_academyLimit = academyLimit;
		_royalLimit = royalLimit;
		_knightLimit = knightLimit;
		_haveTerritory = haveTerritory;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getSp()
	{
		return _sp;
	}
	
	public List<ItemHolder> getRequestItems()
	{
		return _requestItems;
	}
	
	public List<ItemHolder> getRewardItems()
	{
		return _rewardItems;
	}
	
	public List<Skill> getRewardSkills()
	{
		return _rewardSkills;
	}
	
	public int getRequestMembers()
	{
		return _requestMembers;
	}
	
	public int getMembersLimit()
	{
		return _membersLimit;
	}
	
	public int getAcademyLimit()
	{
		return _academyLimit;
	}
	
	public int getRoyalsLimit()
	{
		return _royalLimit;
	}
	
	public int getKnightsLimit()
	{
		return _knightLimit;
	}
	
	public boolean hasTerritory()
	{
		return _haveTerritory;
	}
}