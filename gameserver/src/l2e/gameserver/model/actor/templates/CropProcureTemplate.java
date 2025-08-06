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

public final class CropProcureTemplate extends SeedTemplate
{
	private final int _rewardType;
	
	public CropProcureTemplate(int id, long amount, int type, long startAmount, long price)
	{
		super(id, amount, price, startAmount);
		_rewardType = type;
	}
	
	public final int getReward()
	{
		return _rewardType;
	}
}