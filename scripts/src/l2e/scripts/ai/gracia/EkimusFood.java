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
package l2e.scripts.ai.gracia;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;

/**
 * Created by LordWinter 30.09.2020
 */
public class EkimusFood extends DefaultAI
{
	private static final Location[] _route1 =
	{
	        new Location(-179544, 207400, -15496),
			new Location(-178856, 207464, -15496),
			new Location(-178168, 207864, -15496),
			new Location(-177512, 208728, -15496),
			new Location(-177336, 209528, -15496),
			new Location(-177448, 210328, -15496),
			new Location(-177864, 211048, -15496),
			new Location(-178584, 211608, -15496),
			new Location(-179304, 211848, -15496),
			new Location(-179512, 211864, -15496),
	        new Location(-179528, 211448, -15472)
	};

	private static final Location[] _route2 =
	{
	        new Location(-179576, 207352, -15496),
			new Location(-180440, 207544, -15496),
			new Location(-181256, 208152, -15496),
			new Location(-181752, 209112, -15496),
			new Location(-181720, 210264, -15496),
			new Location(-181096, 211224, -15496),
			new Location(-180264, 211720, -15496),
			new Location(-179528, 211848, -15496),
	        new Location(-179528, 211400, -15472)
	};

	private final Location[] _points;
	private boolean _firstThought = true;
	private int _lastPoint = 0;
	private long _moveInterval = 0L;

	public EkimusFood(Attackable actor)
	{
		super(actor);
		
		actor.setIsEkimusFood(true);
		((MonsterInstance) actor).setPassive(true);
		MAX_PURSUE_RANGE = Integer.MAX_VALUE - 10;
		_points = Rnd.chance(50) ? _route1 : _route2;
	}

	@Override
	public boolean checkAggression(Creature target)
	{
		return false;
	}

	@Override
	protected boolean thinkActive()
	{
		final Attackable npc = getActiveChar();
		if (npc.isDead())
		{
			return true;
		}
		
		if (_moveInterval > System.currentTimeMillis())
		{
			return true;
		}
		
		if (_firstThought)
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
				moveTo(Location.findPointToStay(loc, 40, true));
				if (!npc.isMoving())
				{
					_lastPoint++;
				}
			}
		}
		return true;
	}

	private void startMoveTask()
	{
		final Attackable npc = getActiveChar();
		
		if (_firstThought)
		{
			_lastPoint = getIndex(Location.findNearest(npc, _points));
			_firstThought = false;
		}
		else
		{
			_lastPoint++;
		}
		
		if (_lastPoint >= _points.length)
		{
			_lastPoint = _points.length - 1;
		}
		
		Location loc = _points[_lastPoint];
		if (loc == null)
		{
			_lastPoint = _points.length - 1;
			loc = _points[_lastPoint];
		}
		_moveInterval = System.currentTimeMillis() + 1000L;
		moveTo(Location.findPointToStay(loc, 40, true));
	}
	
	private int getIndex(Location loc)
	{
		for (int i = 0; i < _points.length; i++)
		{
			if (_points[i] == loc)
			{
				return i;
			}
		}
		return 0;
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
	}

	@Override
	protected void returnHome(boolean clearAggro, boolean teleport)
	{
	}

	@Override
	protected void teleportHome()
	{
	}
}
