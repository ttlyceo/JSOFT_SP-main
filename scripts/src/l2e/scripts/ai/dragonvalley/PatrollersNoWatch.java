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

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;

/**
 * Created by LordWinter 29.05.2019
 */
public class PatrollersNoWatch extends Fighter
{
	protected Location[] _points = null;
	private int _lastPoint = 0;
	private boolean _firstThought = true;
	private long _moveInterval = 0L;
	private long _checkInterval = 0L;
	private final String _npcsZones = "22_21,23_21,24_21";

	public PatrollersNoWatch(Attackable actor)
	{
		super(actor);
		MAX_PURSUE_RANGE = Integer.MAX_VALUE - 10;
		actor.setIsRunner(true);
		actor.setCanReturnToSpawnPoint(false);
		actor.setIsGlobalAI(true);
		_checkInterval = System.currentTimeMillis() + 60000;
	}
	
	@Override
	protected void onEvtSpawn()
	{
		_lastPoint = 0;
		_firstThought = true;
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
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();
		if (actor == null || actor.isDead())
		{
			return;
		}
		
		if (_checkInterval < System.currentTimeMillis())
		{
			final int geoX = GeoEngine.getInstance().getMapX(actor.getX());
			final int geoY = GeoEngine.getInstance().getMapY(actor.getY());
			final String reg = "" + geoX + "_" + geoY + "";
			boolean valid = false;
			for (final String region : _npcsZones.split(","))
			{
				if (region.equals(reg))
				{
					valid = true;
					break;
				}
			}
			
			if (!valid)
			{
				returnHome(true, true);
			}
			_checkInterval = System.currentTimeMillis() + 60000;
			return;
		}
		super.thinkAttack();
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
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
			_lastPoint = 0;
		}
		
		if (npc.getScriptValue() > 0)
		{
			npc.setScriptValue(0);
		}
		
		npc.setRunning();
		if (Rnd.chance(5))
		{
			npc.makeTriggerCast(SkillsParser.getInstance().getInfo(6757, 1), npc);
		}
		
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
	protected void teleportHome()
	{
	}
	
	@Override
	protected void returnHome(boolean clearAggro, boolean teleport)
	{
		super.returnHome(clearAggro, teleport);
		_firstThought = true;
		_lastPoint = 0;
		startMoveTask();
	}
}
