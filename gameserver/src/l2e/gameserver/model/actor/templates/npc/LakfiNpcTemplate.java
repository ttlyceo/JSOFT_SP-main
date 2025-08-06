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
package l2e.gameserver.model.actor.templates.npc;

import java.util.List;

/**
 * Created by LordWinter 12.09.2020
 */
public class LakfiNpcTemplate
{
	private final int _npcId;
	private final List<LakfiRewardTemplate> _rewards;
	
	public LakfiNpcTemplate(int npcId, List<LakfiRewardTemplate> rewards)
	{
		_npcId = npcId;
		_rewards = rewards;
	}
	
	public int getId()
	{
		return _npcId;
	}
	
	public List<LakfiRewardTemplate> getRewards()
	{
		return _rewards;
	}
}