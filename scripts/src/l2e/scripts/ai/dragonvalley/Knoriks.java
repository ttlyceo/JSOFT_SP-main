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
 * Created by LordWinter 28.05.2019
 */
public class Knoriks extends Fighter
{
	private Location[] _points = null;
	
	private final Location[] _points1 = new Location[]
	{
	        new Location(141848, 121592, -3912), new Location(140440, 120264, -3912), new Location(140664, 118328, -3912), new Location(142104, 117400, -3912), new Location(142968, 117816, -3912), new Location(142648, 119672, -3912), new Location(143864, 121016, -3896), new Location(144504, 119320, -3896), new Location(145448, 117624, -3912), new Location(146824, 118328, -3984), new Location(147080, 119320, -4288), new Location(147432, 121224, -4768), new Location(148568, 120936, -4864), new Location(149640, 119480, -4864), new Location(150616, 118312, -4936), new Location(152936, 116664, -5256), new Location(153208, 115224, -5256), new Location(151656, 115080, -5472), new Location(148824, 114888, -5472), new Location(151128, 114520, -5464), new Location(152072, 114152, -5520), new Location(153320, 112728, -5520), new Location(153096, 111800, -5520), new Location(150504, 111256, -5520), new Location(149512, 111080, -5488), new Location(149304, 109672, -5216), new Location(151864, 109368, -5152), new Location(153320, 109032, -5152), new Location(153048, 108040, -5152), new Location(150888, 107320, -4800), new Location(149320, 108456, -4424), new Location(147704, 107256, -4048), new Location(146648, 108376, -3664), new Location(146408, 110200, -3472), new Location(146568, 111784, -3552), new Location(147896, 112584, -3720), new Location(148904, 113208, -3720), new Location(149256, 114824, -3720), new Location(149688, 116344, -3704), new Location(150680, 117880, -3688), new Location(152056, 118968, -3808), new Location(152696, 120040, -3808), new Location(151928, 121352, -3808), new Location(152856, 121752, -3808), new Location(154440, 121208, -3808)
	};
	
	private final Location[] _points2 = new Location[]
	{
	        new Location(145452, 115969, -3760), new Location(144630, 115316, -3760), new Location(145136, 114851, -3760), new Location(146549, 116126, -3760), new Location(146421, 116429, -3760)
	};
	
	private final Location[] _points3 = new Location[]
	{
	        new Location(140456, 117832, -3942), new Location(142632, 117336, -3942), new Location(142680, 118680, -3942), new Location(141864, 119240, -3942), new Location(140856, 118904, -3942)
	};
	
	private final Location[] _points4 = new Location[]
	{
	        new Location(140904, 108856, -3764), new Location(140648, 112360, -3750), new Location(142856, 111768, -3974), new Location(142216, 109432, -3966)
	};
	
	private final Location[] _points5 = new Location[]
	{
	        new Location(147960, 110216, -3974), new Location(146072, 109400, -3974), new Location(145576, 110856, -3974), new Location(144504, 107768, -3974), new Location(145864, 109224, -3974)
	};
	
	private final Location[] _points6 = new Location[]
	{
	        new Location(154040, 118696, -3834), new Location(152600, 119992, -3834), new Location(151816, 121480, -3834), new Location(152808, 121960, -3834), new Location(153768, 121480, -3834), new Location(152136, 121672, -3834), new Location(152248, 120200, -3834)
	};
	
	private int _lastPoint = 0;
	private boolean _firstThought = true;
	private boolean _isRecycle = false;
	private long _moveInterval = 0L;
	private long _checkInterval = 0L;
	private final String _npcsZones = "22_21,23_21,24_21";
	
	public Knoriks(Attackable actor)
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
		final Attackable npc = getActiveChar();
		if (npc.getSpawnedLoc().getX() == 141848 && npc.getSpawnedLoc().getY() == 121592)
		{
			_points = _points1;
		}
		else if (npc.getSpawnedLoc().getX() == 145452 && npc.getSpawnedLoc().getY() == 115969)
		{
			_points = _points2;
		}
		else if (npc.getSpawnedLoc().getX() == 140456 && npc.getSpawnedLoc().getY() == 117832)
		{
			_points = _points3;
		}
		else if (npc.getSpawnedLoc().getX() == 140904 && npc.getSpawnedLoc().getY() == 108856)
		{
			_points = _points4;
		}
		else if (npc.getSpawnedLoc().getX() == 147960 && npc.getSpawnedLoc().getY() == 110216)
		{
			_points = _points5;
		}
		else if (npc.getSpawnedLoc().getX() == 154040 && npc.getSpawnedLoc().getY() == 118696)
		{
			_points = _points6;
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
				moveTo(Location.findPointToStay(loc, 40, true));
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
		if (actor.isDead())
		{
			return;
		}
		
		if (actor.isScriptValue(0) && _lastPoint > 0)
		{
			actor.setScriptValue(1);
			if (_isRecycle)
			{
				_lastPoint++;
			}
			else
			{
				_lastPoint--;
			}
		}
		
		if (Rnd.chance(5) && attacker != null && !actor.isOutOfControl() && !actor.isActionsDisabled())
		{
			actor.setTarget(actor);
			actor.doCast(SkillsParser.getInstance().getInfo(6744, 1));
			return;
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
			if (_isRecycle)
			{
				_lastPoint--;
			}
			else
			{
				_lastPoint++;
			}
		}
		
		if (_isRecycle && _lastPoint <= 0)
		{
			_lastPoint = 0;
			_isRecycle = false;
		}
		
		if (_lastPoint >= _points.length && !_isRecycle)
		{
			_isRecycle = true;
			_lastPoint--;
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
		
		Location loc = null;
		try
		{
			loc = _points[_lastPoint];
		}
		catch (final Exception e)
		{}
		
		if (loc == null)
		{
			if (_isRecycle)
			{
				_lastPoint = _points.length - 1;
			}
			else
			{
				_lastPoint = 0;
			}
			loc = _points[_lastPoint];
		}
		
		if (loc != null)
		{
			_moveInterval = System.currentTimeMillis() + 1000L;
			moveTo(Location.findPointToStay(loc, 40, true));
		}
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
		final Attackable actor = getActiveChar();
		if (actor.isDead())
		{
			return;
		}
		super.returnHome(clearAggro, teleport);
		_lastPoint = getIndex(Location.findNearest(actor, _points));
		_firstThought = false;
	}
}
