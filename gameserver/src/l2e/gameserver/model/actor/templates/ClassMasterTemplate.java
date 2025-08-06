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

public class ClassMasterTemplate
{
	private final List<ItemHolder> _requestItems;
	private final Map<Integer, List<ItemHolder>> _rewardItems;
	private final boolean _allowedClassChange;
	
	public ClassMasterTemplate(List<ItemHolder> requestItems, Map<Integer, List<ItemHolder>> rewardItems, boolean allowedClassChange)
	{
		_requestItems = requestItems;
		_rewardItems = rewardItems;
		_allowedClassChange = allowedClassChange;
	}

	public List<ItemHolder> getRequestItems()
	{
		return _requestItems;
	}
	
	public Map<Integer, List<ItemHolder>> getRewardItems()
	{
		return _rewardItems;
	}
	
	public boolean isAllowedChangeClass()
	{
		return _allowedClassChange;
	}
}