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
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;

/**
 * Created by LordWinter 13.08.2020
 */
public class Gordon extends Fighter
{
	private static final Location[] points =
	{
			new Location(146268, -64651, -3412),
			new Location(143678, -64045, -3434),
			new Location(141620, -62316, -3210),
			new Location(139466, -60839, -2994),
			new Location(138429, -57679, -3548),
			new Location(139402, -55879, -3334),
			new Location(139660, -52780, -2908),
			new Location(139516, -50343, -2591),
			new Location(140059, -48657, -2271),
			new Location(140319, -46063, -2408),
			new Location(142462, -45540, -2432),
			new Location(144290, -43543, -2380),
			new Location(146494, -43234, -2325),
			new Location(148416, -43186, -2329),
			new Location(151135, -44084, -2746),
			new Location(153040, -42240, -2920),
			new Location(154871, -39193, -3294),
			new Location(156725, -41827, -3569),
			new Location(157788, -45071, -3598),
			new Location(159433, -45943, -3547),
			new Location(160327, -47404, -3681),
			new Location(159106, -48215, -3691),
			new Location(159541, -50908, -3563),
			new Location(159576, -53782, -3226),
			new Location(160918, -56899, -2790),
			new Location(160785, -59505, -2662),
			new Location(158252, -60098, -2680),
			new Location(155962, -59751, -2656),
			new Location(154649, -60214, -2701),
			new Location(153121, -63319, -2969),
			new Location(151511, -64366, -3174),
	        new Location(149161, -64576, -3316), new Location(147316, -64797, -3440)
	};

	private int current_point = -1;
	private long wait_timeout = 0;
	private boolean wait = false;
	private long _moveInterval = 0L;

	public Gordon(Attackable actor)
	{
		super(actor);
		
		actor.setIsGlobalAI(true);
	}

	@Override
	protected void onEvtSpawn()
	{
		getActiveChar().setCanReturnToSpawnPoint(false);
		current_point = -1;
		wait_timeout = 0;
		wait = false;
		getActiveChar().getAI().enableAI();
		super.onEvtSpawn();
	}
	
	@Override
	protected boolean checkAggression(Creature target)
	{
		if (target.isPlayer() && !((Player) target).isCursedWeaponEquipped())
		{
			return false;
		}
		
		if (getIntention() == CtrlIntention.ATTACK)
		{
			if (getActiveChar().isScriptValue(0) && current_point > -1)
			{
				getActiveChar().setScriptValue(1);
				current_point--;
			}
		}
		return super.checkAggression(target);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (actor.isScriptValue(0) && current_point > -1)
		{
			actor.setScriptValue(1);
			current_point--;
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead())
		{
			return true;
		}
		
		if (super.thinkActive() || _moveInterval > System.currentTimeMillis())
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
			if (!wait && current_point == 31)
			{
				wait_timeout = System.currentTimeMillis() + 60000;
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
			
			if (actor.getScriptValue() > 0)
			{
				actor.setScriptValue(0);
			}

			_moveInterval = System.currentTimeMillis() + 1000L;
			actor.setWalking();
			moveTo(loc);
		}
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