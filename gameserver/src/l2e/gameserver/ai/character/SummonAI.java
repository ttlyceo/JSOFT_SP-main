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
package l2e.gameserver.ai.character;

import static l2e.gameserver.ai.model.CtrlIntention.ATTACK;
import static l2e.gameserver.ai.model.CtrlIntention.FOLLOW;
import static l2e.gameserver.ai.model.CtrlIntention.IDLE;

import java.util.concurrent.Future;

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;

public class SummonAI extends PlayableAI implements Runnable
{
	private static final int AVOID_RADIUS = 70;

	private volatile boolean _thinking;
	private volatile boolean _startFollow = ((Summon) _actor).isInFollowStatus();
	private Creature _lastAttack = null;

	private volatile boolean _startAvoid;
	private Future<?> _avoidTask = null;

	public SummonAI(Summon summon)
	{
		super(summon);
	}
	
	@Override
	protected void onIntentionIdle()
	{
		stopFollow();
		_startFollow = false;
		onIntentionActive();
	}

	@Override
	protected void onIntentionActive()
	{
		final var summon = (Summon) _actor;
		if (_startFollow)
		{
			setIntention(FOLLOW, summon.getOwner());
		}
		else
		{
			super.onIntentionActive();
		}
	}

	@Override
	protected synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		switch (intention)
		{
			case ACTIVE :
			case FOLLOW :
			case ATTACK :
				startAvoidTask();
				break;
			default :
				stopAvoidTask();
		}
		super.changeIntention(intention, arg0, arg1);
	}

	private void thinkAttack()
	{
		final var target = getAttackTarget();
		if (target == null)
		{
			return;
		}
		
		if (target.isDead() || !target.isVisibleFor(_actor))
		{
			((Summon) _actor).setFollowStatus(true);
			_actor.getAI().setIntention(CtrlIntention.ACTIVE);
			return;
		}
		
		final var range = _actor.getPhysicalAttackRange();
		final var canSee = GeoEngine.getInstance().canSeeTarget(_actor, target);
		
		if (!canSee && ((range > 200) || (Math.abs(_actor.getZ() - target.getZ()) > 200)))
		{
			clientStopMoving(null);
			_actor.getAI().setIntention(CtrlIntention.ACTIVE);
			return;
		}
		
		if (_actor.isInRangeZ(_actor, target, range))
		{
			if (!canSee)
			{
				_actor.sendPacket(SystemMessageId.CANT_SEE_TARGET);
				_actor.getAI().setIntention(CtrlIntention.ACTIVE);
				return;
			}
			
			if (checkTargetLostOrDead(target))
			{
				setAttackTarget(null);
				return;
			}
			if (maybeMoveToPawn(target, range, false))
			{
				return;
			}
			clientStopMoving(null);
			_actor.doAttack(target);
		}
		else
		{
			if (checkTargetLostOrDead(target))
			{
				setAttackTarget(null);
				return;
			}
			if (maybeMoveToPawn(target, range, false))
			{
				return;
			}
			clientStopMoving(null);
			_actor.doAttack(target);
		}
	}

	private void thinkCast()
	{
		final var summon = (Summon) _actor;
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		final var val = _startFollow;
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill), false))
		{
			return;
		}
		clientStopMoving(null);
		summon.setFollowStatus(false);
		setIntention(IDLE);
		_startFollow = val;
		_actor.doCast(_skill);
	}

	private void thinkPickUp()
	{
		if (checkTargetLost(getTarget()))
		{
			return;
		}
		if (maybeMoveToPawn(getTarget(), 36, false))
		{
			return;
		}
		setIntention(IDLE);
		getActor().doPickupItem(getTarget());
	}

	private void thinkInteract()
	{
		if (checkTargetLost(getTarget()))
		{
			return;
		}
		if (maybeMoveToPawn(getTarget(), 36, false))
		{
			return;
		}
		setIntention(IDLE);
	}

	@Override
	protected void onEvtThink()
	{
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
		{
			return;
		}
		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case ATTACK :
					thinkAttack();
					break;
				case CAST :
					thinkCast();
					break;
				case PICK_UP :
					thinkPickUp();
					break;
				case INTERACT :
					thinkInteract();
					break;
			}
		}
		finally
		{
			_thinking = false;
		}
	}

	@Override
	protected void onEvtFinishCasting()
	{
		if (_lastAttack == null)
		{
			((Summon) _actor).setFollowStatus(_startFollow);
		}
		else
		{
			setIntention(CtrlIntention.ATTACK, _lastAttack);
			_lastAttack = null;
		}
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		super.onEvtAttacked(attacker, damage);

		avoidAttack(attacker);
	}

	@Override
	protected void onEvtEvaded(Creature attacker)
	{
		super.onEvtEvaded(attacker);
		avoidAttack(attacker);
	}

	private void avoidAttack(Creature attacker)
	{
		if (_actor.isCastingNow())
		{
			return;
		}
		
		final var owner = getActor().getOwner();
		if ((owner != null) && (owner != attacker) && owner.isInsideRadius(_actor, 2 * AVOID_RADIUS, true, false))
		{
			_startAvoid = true;
		}
	}
	
	@Override
	public void run()
	{
		if (_startAvoid)
		{
			_startAvoid = false;
			if (!_actor.isMoving() && !_actor.isAttackingNow() && !_actor.isDead() && !_actor.isMovementDisabled() && !_actor.isCastingNow())
			{
				final var ownerX = ((Summon) _actor).getOwner().getX();
				final var ownerY = ((Summon) _actor).getOwner().getY();
				final var angle = Math.toRadians(Rnd.get(-90, 90)) + Math.atan2(ownerY - _actor.getY(), ownerX - _actor.getX());

				final var targetX = ownerX + (int) (AVOID_RADIUS * Math.cos(angle));
				final var targetY = ownerY + (int) (AVOID_RADIUS * Math.sin(angle));
				moveTo(targetX, targetY, _actor.getZ(), 0);
			}
		}
	}

	public void notifyFollowStatusChange()
	{
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case ACTIVE :
			case FOLLOW :
			case IDLE :
			case MOVING :
			case PICK_UP :
				((Summon) _actor).setFollowStatus(_startFollow);
		}
	}

	public void setStartFollowController(boolean val)
	{
		_startFollow = val;
	}

	@Override
	protected void onIntentionCast(Skill skill, GameObject target)
	{
		if (getIntention() == ATTACK)
		{
			_lastAttack = getAttackTarget();
		}
		else
		{
			_lastAttack = null;
		}
		super.onIntentionCast(skill, target);
	}

	private synchronized void startAvoidTask()
	{
		if (_avoidTask == null)
		{
			_avoidTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(this, 100, 500);
		}
	}

	private synchronized void stopAvoidTask()
	{
		if (_avoidTask != null)
		{
			_avoidTask.cancel(false);
			_avoidTask = null;
		}
	}

	@Override
	public void stopAITask()
	{
		stopAvoidTask();
		super.stopAITask();
	}
	
	@Override
	public Summon getActor()
	{
		return (Summon) super.getActor();
	}
}