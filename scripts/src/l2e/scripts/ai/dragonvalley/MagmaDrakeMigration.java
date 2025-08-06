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

import l2e.commons.util.Util;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Created by LordWinter 11.03.2022
 */
public class MagmaDrakeMigration extends Fighter
{
	private Location[] _points;
	
	private final Location[] _route1 = new Location[]
	{
	        new Location(125128, 111416, -3152), new Location(125496, 112152, -3232), new Location(125800, 112520, -3320), new Location(126264, 112776, -3448), new Location(126712, 112920, -3552), new Location(127464, 113032, -3664), new Location(128312, 113400, -3680), new Location(129128, 113992, -3712), new Location(129352, 114760, -3792), new Location(128984, 115352, -3808), new Location(128264, 115624, -3792), new Location(127368, 115512, -3744), new Location(125960, 115128, -3728), new Location(124936, 115112, -3728), new Location(124216, 115192, -3680)
	};
	
	private final Location[] _route2 = new Location[]
	{
	        new Location(118120, 116664, -3728), new Location(117320, 116792, -3728), new Location(116632, 117208, -3728), new Location(116408, 117928, -3728), new Location(116392, 118536, -3728), new Location(116136, 119304, -3728), new Location(115720, 119816, -3680), new Location(115128, 120152, -3680), new Location(114104, 120680, -3808), new Location(113496, 121608, -3728), new Location(112728, 122136, -3728), new Location(111960, 122552, -3728)
	};
	
	private final Location[] _route3 = new Location[]
	{
	        new Location(109640, 112328, -3040), new Location(110648, 111096, -3120), new Location(111608, 110472, -3056), new Location(112632, 110264, -2976), new Location(114040, 110184, -3024), new Location(115128, 110024, -3040), new Location(116232, 110008, -3040), new Location(117352, 109960, -2960), new Location(119000, 109768, -2976), new Location(120360, 109336, -2992), new Location(121944, 108648, -2976)
	};
	
	private final Location[] _route4 = new Location[]
	{
	        new Location(109256, 122808, -3664), new Location(108472, 122408, -3664), new Location(107576, 122344, -3696), new Location(106728, 122952, -3696), new Location(105800, 123432, -3648), new Location(104760, 123816, -3728), new Location(103752, 123688, -3728), new Location(103160, 123384, -3696), new Location(102664, 122680, -3648), new Location(102888, 121848, -3696), new Location(102984, 121016, -3728)
	};

	private int _lastPoint = 0;
	private boolean _firstThought = true;
	private long _moveInterval = 0L;

	public MagmaDrakeMigration(Attackable actor)
	{
		super(actor);
		
		MAX_PURSUE_RANGE = Integer.MAX_VALUE - 10;
		actor.setIsRunner(true);
		actor.setCanReturnToSpawnPoint(false);
		actor.setIsGlobalAI(true);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}
	
	@Override
	protected void onEvtSpawn()
	{
		final Attackable actor = getActiveChar();
		
		final Location location1 = new Location(124760, 109608, -3088);
		final Location location2 = new Location(119224, 116264, -3760);
		final Location location3 = new Location(109464, 113688, -3072);
		final Location location4 = new Location(110024, 123480, -3616);
		
		if (actor.getDistance(location1) < 2000)
		{
			_points = _route1;
		}
		if (actor.getDistance(location2) < 2000)
		{
			_points = _route2;
		}
		if (actor.getDistance(location3) < 2000)
		{
			_points = _route3;
		}
		if (actor.getDistance(location4) < 2000)
		{
			_points = _route4;
		}
		getActiveChar().getAI().enableAI();
		super.onEvtSpawn();
	}

	@Override
	protected boolean thinkActive()
	{
		final Attackable npc = getActiveChar();
		if (npc.isDead())
		{
			return true;
		}
		
		if (super.thinkActive() || _points == null || _moveInterval > System.currentTimeMillis())
		{
			return true;
		}

		if (_firstThought || _lastPoint >= _points.length || _lastPoint < 0)
		{
			startMoveTask();
		}
		else
		{
			final Location loc = _points[_lastPoint];
			if (Util.checkIfInRange(80, loc.getX(), loc.getY(), loc.getZ(), npc, false))
			{
				startMoveTask();
			}
			else
			{
				_moveInterval = System.currentTimeMillis() + 1000L;
				if (!npc.isRunning())
				{
					npc.setRunning();
				}
				moveTo(Location.findPointToStay(loc, 40, true));
				if (!npc.isMoving())
				{
					_lastPoint++;
				}
			}
		}
		return true;
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead())
		{
			return;
		}
		
		if (actor.isScriptValue(0) && _lastPoint > 0)
		{
			actor.setScriptValue(1);
			_lastPoint--;
		}
		super.onEvtAttacked(attacker, damage);
	}

	private void startMoveTask()
	{
		final Attackable npc = getActiveChar();
		if(_firstThought)
		{
			_lastPoint = getIndex(Location.findNearest(npc, _points));
			_firstThought = false;
		}
		else
		{
			_lastPoint++;
		}
		
		if(_lastPoint >= _points.length)
		{
			_lastPoint = 0;
			npc.getAI().stopAITask();
			npc.deleteMe();
			return;
		}
		
		if (npc.getScriptValue() > 0)
		{
			npc.setScriptValue(0);
		}
		
		npc.setRunning();
		Location loc = _points[_lastPoint];
		if (loc == null)
		{
			_lastPoint = 0;
			loc = _points[_lastPoint];
		}
		_moveInterval = System.currentTimeMillis() + 1000L;
		moveTo(Location.findPointToStay(loc, 40, true));
	}

	private int getIndex(Location loc)
	{
		for(int i = 0; i < _points.length; i++)
		{
			if(_points[i] == loc)
			{
				return i;
			}
		}
		return 0;
	}

	protected boolean randomWalk()
	{
		return false;
	}

	@Override
	protected boolean maybeMoveToHome()
	{
		return false;
	}

	@Override
	protected void teleportHome()
	{
	}
	
	@Override
	protected void returnHome(boolean clearAggro, boolean teleport)
	{
	}
}
