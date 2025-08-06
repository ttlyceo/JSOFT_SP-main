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
import static l2e.gameserver.ai.model.CtrlIntention.CAST;
import static l2e.gameserver.ai.model.CtrlIntention.IDLE;
import static l2e.gameserver.ai.model.CtrlIntention.INTERACT;
import static l2e.gameserver.ai.model.CtrlIntention.MOVING;
import static l2e.gameserver.ai.model.CtrlIntention.PICK_UP;
import static l2e.gameserver.ai.model.CtrlIntention.REST;

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.model.NextAction;
import l2e.gameserver.ai.model.NextAction.NextActionCallback;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.StaticObjectInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.network.SystemMessageId;

public class PlayerAI extends PlayableAI
{
	private boolean _thinking;
	
	IntentionCommand _nextIntention = null;
	
	public PlayerAI(Player accessor)
	{
		super(accessor);
	}
	
	void saveNextIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_nextIntention = new IntentionCommand(intention, arg0, arg1);
	}
	
	@Override
	public IntentionCommand getNextIntention()
	{
		return _nextIntention;
	}

	@Override
	protected synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if ((intention != CAST) || ((arg0 != null) && !((Skill) arg0).isToggle()))
		{
			_nextIntention = null;
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		if ((intention == _intention) && (arg0 == _intentionArg0) && (arg1 == _intentionArg1))
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		saveNextIntention(_intention, _intentionArg0, _intentionArg1);
		super.changeIntention(intention, arg0, arg1);
	}
	
	@Override
	protected void onEvtReadyToAct()
	{
		final var next = _nextIntention;
		if (next != null)
		{
			setIntention(next._crtlIntention, next._arg0, next._arg1);
			_nextIntention = null;
		}
		super.onEvtReadyToAct();
	}
	
	@Override
	protected void onEvtCancel()
	{
		_nextIntention = null;
		super.onEvtCancel();
	}
	
	@Override
	protected void onEvtFinishCasting()
	{
		if (getIntention() == CAST)
		{
			final var nextIntention = _nextIntention;
			if (nextIntention != null)
			{
				if (nextIntention._crtlIntention != CAST)
				{
					setIntention(nextIntention._crtlIntention, nextIntention._arg0, nextIntention._arg1);
				}
				else
				{
					setIntention(IDLE);
				}
			}
			else
			{
				setIntention(IDLE);
			}
		}
	}
	
	@Override
	protected void onIntentionRest()
	{
		if (getIntention() != REST)
		{
			changeIntention(REST, null, null);
			setTarget(null);
			if (getAttackTarget() != null)
			{
				setAttackTarget(null);
			}
			clientStopMoving(null);
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		setIntention(IDLE);
	}
	
	@Override
	protected void onIntentionMoveTo(Location loc, int offset)
	{
		if (getIntention() == REST)
		{
			clientActionFailed();
			final var nextAction = new NextAction(CtrlEvent.EVT_STAND_UP, CtrlIntention.MOVING, new NextActionCallback()
			{
				@Override
				public void doWork()
				{
					_actor.getAI().setIntention(CtrlIntention.MOVING, loc, offset);
				}
			});
			_actor.getAI().setNextAction(nextAction);
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isActionsDisabled())
		{
			clientActionFailed();
			saveNextIntention(MOVING, loc, offset);
			return;
		}
		
		changeIntention(MOVING, loc, offset);
		
		clientStopAutoAttack();
		
		_actor.abortAttack();
		
		moveTo(loc, offset);
	}
	
	private void thinkAttack()
	{
		final var target = getAttackTarget();
		if (target == null)
		{
			return;
		}
		
		if (_actor.isActionsDisabled())
		{
			_actor.sendActionFailed();
			return;
		}
		
		var range = _actor.getPhysicalAttackRange();
		final var canSee = GeoEngine.getInstance().canSeeTarget(_actor, target);
		
		final var isBehind = _actor.isBehind(target) && target.isMoving();
		if (isBehind || _actor.isMovementDisabled())
		{
			range += 50;
		}
		
		if (!canSee && ((range > 200) || (Math.abs(_actor.getZ() - target.getZ()) > 200)))
		{
			clientStopMoving(null);
			_actor.getAI().setIntention(CtrlIntention.ACTIVE);
			_actor.sendActionFailed();
			return;
		}
		
		if (_actor.isInRangeZ(_actor, target, range))
		{
			if (!canSee)
			{
				_actor.sendPacket(SystemMessageId.CANT_SEE_TARGET);
				_actor.getAI().setIntention(CtrlIntention.ACTIVE);
				_actor.sendActionFailed();
				return;
			}
			
			if (checkTargetLostOrDead(target))
			{
				setAttackTarget(null);
				return;
			}
			if (maybeMoveToPawn(target, range, isBehind))
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
			
			if (maybeMoveToPawn(target, range, isBehind))
			{
				return;
			}
			clientStopMoving(null);
			_actor.doAttack(target);
		}
	}
	
	@Override
	protected void clientNotifyDead()
	{
		_clientMovingToPawnOffset = 0;
		_clientMoving = false;
		
		super.clientNotifyDead();
	}
	
	private void thinkCast()
	{
		final var target = getCastTarget();
		
		var range = _actor.getMagicalAttackRange(_skill);
		final var canSee = (_skill.isDisableGeoCheck() || GeoEngine.getInstance().canSeeTarget(_actor, target));
		
		final var isBehind = _actor.isBehind(target) && target.isMoving();
		if (isBehind)
		{
			range += 50;
		}
		
		if ((_skill.getCastRange() > 0 || _skill.getEffectRange() > 0) && (_actor.isInRangeZ(_actor, target, range) || !canSee))
		{
			if (!canSee)
			{
				_actor.setIsCastingNow(false);
				_actor.getAI().setIntention(CtrlIntention.ACTIVE);
				_actor.sendActionFailed();
				return;
			}
			
			if ((_skill.getTargetType() == TargetType.GROUND) && (_actor instanceof Player))
			{
				if (maybeMoveToPosition(((Player) _actor).getCurrentSkillWorldPosition(), range))
				{
					_actor.setIsCastingNow(false);
					return;
				}
			}
			else
			{
				if (checkTargetLost(target))
				{
					if (_skill.isOffensive() && (getAttackTarget() != null))
					{
						setCastTarget(null);
					}
					_actor.setIsCastingNow(false);
					return;
				}
				
				if ((target != null) && maybeMoveToPawn(target, range, isBehind))
				{
					_actor.setIsCastingNow(false);
					return;
				}
			}

			final var oldTarget = _actor.getTarget();
			if ((oldTarget != null) && (target != null) && (oldTarget != target))
			{
				_actor.getActingPlayer().setFastTarget(getCastTarget());
				_actor.doCast(_skill);
				_actor.setTarget(oldTarget);
			}
			else
			{
				_actor.doCast(_skill);
			}
		}
		else
		{
			
			if ((_skill.getTargetType() == TargetType.GROUND) && (_actor instanceof Player))
			{
				if (maybeMoveToPosition(((Player) _actor).getCurrentSkillWorldPosition(), range))
				{
					_actor.setIsCastingNow(false);
					return;
				}
			}
			else
			{
				if (checkTargetLost(target))
				{
					if (_skill.isOffensive() && (getAttackTarget() != null))
					{
						setCastTarget(null);
					}
					_actor.setIsCastingNow(false);
					return;
				}

				if ((target != null) && maybeMoveToPawn(target, range, isBehind))
				{
					_actor.setIsCastingNow(false);
					return;
				}
			}

			final var oldTarget = _actor.getTarget();
			if ((oldTarget != null) && (target != null) && (oldTarget != target))
			{
				_actor.getActingPlayer().setFastTarget(getCastTarget());
				_actor.doCast(_skill);
				_actor.setTarget(oldTarget);
			}
			else
			{
				_actor.doCast(_skill);
			}
		}
	}
	
	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isActionsDisabled())
		{
			return;
		}
		final var target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		if (maybeMoveToPawn(target, 36, false))
		{
			return;
		}
		setIntention(IDLE);
		_actor.getActingPlayer().doPickupItem(target);
	}
	
	private void thinkInteract()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isActionsDisabled())
		{
			return;
		}
		
		final var target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		if (maybeMoveToPawn(target, 36, false))
		{
			return;
		}
		if (!(target instanceof StaticObjectInstance))
		{
			((Player) _actor).doInteract((Creature) target);
		}
		setIntention(IDLE);
	}
	
	@Override
	protected void onEvtThink()
	{
		if (_thinking && (getIntention() != CAST))
		{
			return;
		}
		
		_thinking = true;
		try
		{
			if (getIntention() == ATTACK)
			{
				thinkAttack();
			}
			else if (getIntention() == CAST)
			{
				thinkCast();
			}
			else if (getIntention() == PICK_UP)
			{
				thinkPickUp();
			}
			else if (getIntention() == INTERACT)
			{
				thinkInteract();
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	public Player getActor()
	{
		return (Player) super.getActor();
	}
}