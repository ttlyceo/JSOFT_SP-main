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
package l2e.scripts.ai;


import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

public class AirshipGuard1 extends Fighter
{
	static final Location[] points =
	{
	        new Location(-149633, 254016, -180), new Location(-149874, 254224, -184), new Location(-150088, 254429, -184), new Location(-150229, 254603, -184), new Location(-150368, 254822, -184), new Location(-150570, 255125, -184), new Location(-149649, 254189, -180), new Location(-149819, 254291, -184), new Location(-150038, 254487, -184), new Location(-150182, 254654, -184), new Location(-150301, 254855, -184), new Location(-150438, 255133, -181)
	};
	
	private int current_point = -1;
	private long wait_timeout = 0;
	private boolean wait = false;
	private long _moveInterval = 0L;
	
	public AirshipGuard1(Attackable actor)
	{
		super(actor);
		
		actor.setIsGlobalAI(true);
	}

	@Override
	protected void onEvtSpawn()
	{
		getActiveChar().getAI().enableAI();
		super.onEvtSpawn();
	}
	
	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead() || _moveInterval > System.currentTimeMillis())
		{
			return true;
		}
		
		if (current_point < 0 || current_point >= points.length)
		{
			startMoveTask();
		}
		else
		{
			final Location loc = points[current_point];
			if (Util.checkIfInRange(80, loc.getX(), loc.getY(), loc.getZ(), actor, false))
			{
				startMoveTask();
			}
			else
			{
				_moveInterval = System.currentTimeMillis() + 1000L;
				moveTo(Location.findPointToStay(loc, 40, true));
				return true;
			}
		}
		return false;
	}
	
	private void startMoveTask()
	{
		final Attackable actor = getActiveChar();
		if (System.currentTimeMillis() > wait_timeout && (current_point > -1 || Rnd.chance(5)))
		{
			try
			{
				if (!wait && (current_point == 0 || current_point == 8))
				{
					wait_timeout = System.currentTimeMillis() + Rnd.get(0, 30000);
					wait = true;
					return;
				}
				
				wait_timeout = 0;
				wait = false;
				current_point++;
				
				if (current_point >= points.length)
				{
					current_point = 0;
				}
				
				Location loc = points[current_point];
				if (loc == null)
				{
					current_point = 0;
					loc = points[current_point];
				}
				_moveInterval = System.currentTimeMillis() + 1000L;
				actor.setWalking();
				moveTo(Location.findPointToStay(actor, loc, 0, 100, true));
			}
			catch (final Exception e)
			{
			}
		}
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
	}
	
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
	}
}
