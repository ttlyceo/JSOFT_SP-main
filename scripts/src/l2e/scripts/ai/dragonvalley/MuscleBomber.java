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
package l2e.scripts.ai.dragonvalley;


import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;

/**
 * Created by LordWinter 10.12.2018
 */
public class MuscleBomber extends Fighter
{
	private long _lastAttackTime = 0;
	private final long _despawnTime;
	
	public MuscleBomber(Attackable actor)
	{
		super(actor);
		_despawnTime = actor.getTemplate().getParameter("despawnTime", 30);
	}

	@Override
	protected void onEvtSpawn()
	{
		_lastAttackTime = System.currentTimeMillis();
		super.onEvtSpawn();
	}

	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if (_lastAttackTime != 0)
		{
			if ((_lastAttackTime + (5 * 60 * 1000L)) < System.currentTimeMillis())
			{
				if (actor.getAggroRange() == 0)
				{
					actor.getTemplate().setAggroRange(400);
				}
			}
			if (_despawnTime > 0 && (_lastAttackTime + (_despawnTime * 60 * 1000L)) < System.currentTimeMillis())
			{
				actor.deleteMe();
			}
		}
		return super.thinkActive();
	}
	
	@Override
	public int getRateDEBUFF()
	{
		return 5;
	}
	
	@Override
	public int getRateDAM()
	{
		return 80;
	}
}