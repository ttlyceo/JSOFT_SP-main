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

public class Corpse extends DefaultAI
{
	public Corpse(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected boolean thinkActive()
	{
		return super.thinkActive();
	}
	
	@Override
	public int getRatePHYS()
	{
		return 0;
	}
	
	@Override
	public int getRateDOT()
	{
		return 0;
	}
	
	@Override
	public int getRateDEBUFF()
	{
		return 0;
	}
	
	@Override
	public int getRateDAM()
	{
		return 0;
	}
	
	@Override
	public int getRateSTUN()
	{
		return 0;
	}
	
	@Override
	public int getRateBUFF()
	{
		return 0;
	}
	
	@Override
	public int getRateHEAL()
	{
		return 0;
	}

	@Override
	protected int getRateSuicide()
	{
		return 0;
	}
	
	@Override
	protected int getRateRes()
	{
		return 0;
	}
	
	@Override
	protected int getRateDodge()
	{
		return 0;
	}
}