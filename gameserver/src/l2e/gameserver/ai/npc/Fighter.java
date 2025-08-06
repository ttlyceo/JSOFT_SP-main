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
package l2e.gameserver.ai.npc;

import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.model.actor.Attackable;

public class Fighter extends DefaultAI
{
	public Fighter(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		return super.thinkActive() || defaultThinkBuff(2);
	}
	
	@Override
	protected boolean createNewTask()
	{
		return defaultFightTask();
	}

	@Override
	protected int getRatePHYS()
	{
		return 10;
	}
	
	@Override
	protected int getRateDOT()
	{
		return 8;
	}
	
	@Override
	protected int getRateDEBUFF()
	{
		return 5;
	}
	
	@Override
	protected int getRateDAM()
	{
		return 5;
	}
	
	@Override
	protected int getRateSTUN()
	{
		return 8;
	}
	
	@Override
	protected int getRateBUFF()
	{
		return 5;
	}
	
	@Override
	protected int getRateHEAL()
	{
		return 5;
	}

	@Override
	protected int getRateSuicide()
	{
		return 3;
	}
	
	@Override
	protected int getRateRes()
	{
		return 2;
	}
	
	@Override
	protected int getRateDodge()
	{
		return 0;
	}
}