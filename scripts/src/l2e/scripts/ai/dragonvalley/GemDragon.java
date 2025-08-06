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

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;

/**
 * Created by LordWinter 23.12.2020
 */
public class GemDragon extends HerbCollectorFighter
{
	private boolean _isInvul = false;
	private long _maxTime = 0;
	
	public GemDragon(Attackable actor)
	{
		super(actor);
		
		MAX_PURSUE_RANGE = 3000;
	}
	
	@Override
	protected boolean checkAggression(Creature target)
	{
		if (_isInvul)
		{
			return false;
		}
		return super.checkAggression(target);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		if (_isInvul)
		{
			return;
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
		
		if (_isInvul)
		{
			checkMinions(actor, _isInvul);
			return false;
		}
		return super.thinkActive();
	}
	
	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();
		if (actor == null)
		{
			return;
		}
		
		if (_isInvul)
		{
			checkMinions(actor, _isInvul);
			return;
		}
		super.thinkAttack();
	}
	
	@Override
	public void returnHome()
	{
		final Attackable actor = getActiveChar();
		if (actor == null)
		{
			return;
		}
		super.returnHome();
		if (!actor.isDead() && !_isInvul)
		{
			actor.setIsInvul(true);
			_isInvul = true;
			_maxTime = System.currentTimeMillis() + 60000L;
			checkMinions(actor, true);
		}
	}
	
	private void checkMinions(Attackable actor, boolean isInvul)
	{
		final Location sloc = actor.getSpawnedLoc();
		if (sloc != null)
		{
			if (_maxTime < System.currentTimeMillis() && _isInvul)
			{
				_isInvul = false;
				_maxTime = 0L;
				actor.setIsInvul(false);
				isInvul = _isInvul;
			}
			
			if (actor.isInRangeZ(sloc, Config.MAX_DRIFT_RANGE))
			{
				_isInvul = false;
				_maxTime = 0L;
				actor.setIsInvul(false);
				isInvul = _isInvul;
			}
			
			if (isInvul)
			{
				actor.clearAggroList(false);
				actor.setRunning();
				moveTo(sloc);
			}
			
			for (final MonsterInstance minion : actor.getMinionList().getAliveMinions())
			{
				if (minion != null && !minion.isDead())
				{
					if (isInvul)
					{
						minion.setIsInvul(true);
						minion.clearAggroList(false);
						minion.setRunning();
						minion.getAI().setIntention(CtrlIntention.MOVING, sloc, 0);
					}
					else
					{
						minion.setIsInvul(false);
					}
				}
			}
		}
	}
}
