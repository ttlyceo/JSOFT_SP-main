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
package l2e.gameserver.ai;

import static l2e.gameserver.ai.model.CtrlIntention.ATTACK;
import static l2e.gameserver.ai.model.CtrlIntention.FOLLOW;
import static l2e.gameserver.ai.model.CtrlIntention.IDLE;

import java.util.concurrent.Future;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.GameTimeController;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.Ctrl;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.model.NextAction;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.serverpackets.AutoAttackStart;
import l2e.gameserver.network.serverpackets.AutoAttackStop;
import l2e.gameserver.network.serverpackets.Die;
import l2e.gameserver.network.serverpackets.FinishRotatings;
import l2e.gameserver.network.serverpackets.MoveToLocation;
import l2e.gameserver.network.serverpackets.MoveToPawn;
import l2e.gameserver.network.serverpackets.ValidateLocation;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public abstract class AbstractAI extends LoggerObject implements Ctrl
{
	private NextAction _nextAction;
	
	public NextAction getNextAction()
	{
		return _nextAction;
	}

	public void setNextAction(NextAction nextAction)
	{
		_nextAction = nextAction;
	}
	
	private class FollowTask implements Runnable
	{
		protected int _range = 70;
		
		public FollowTask()
		{
		}
		
		public FollowTask(int range)
		{
			_range = range;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (_followTask == null)
				{
					return;
				}
				
				final Creature followTarget = _followTarget;
				if (followTarget == null)
				{
					if (_actor.isSummon())
					{
						((Summon) _actor).setFollowStatus(false);
					}
					setIntention(IDLE);
					return;
				}
				
				final var iSummon = _actor.isSummon();
				if (!_actor.isInsideRadius(followTarget, _range, true, false))
				{
					if (!_actor.isInsideRadius(followTarget, iSummon ? 6000 : 3000, true, false))
					{
						if (iSummon && !_actor.isMoving() && !followTarget.isTeleporting())
						{
							if (Config.ALLOW_SUMMON_TELE_TO_LEADER)
							{
								((Summon) _actor).teleToLeader(true);
							}
							else
							{
								((Summon) _actor).setFollowStatus(false);
								setIntention(IDLE);
							}
							return;
						}
						else
						{
							setIntention(IDLE);
							return;
						}
					}
					
					if (!_actor.isInPathFinding())
					{
						moveTo(followTarget.getLocation(), followTarget.isMoving() ? (_range + 25) : _range);
						if (iSummon && Config.ALLOW_SUMMON_TELE_TO_LEADER && !_actor.isMoving() && !followTarget.isTeleporting())
						{
							if (Math.sqrt(_actor.getDistanceSq(followTarget)) > 200)
							{
								((Summon) _actor).teleToLeader(true);
								return;
							}
						}
					}
				}
			}
			catch (final Exception e)
			{
				warn("Error: " + e.getMessage());
			}
		}
	}

	protected final Creature _actor;

	protected CtrlIntention _intention = IDLE;
	protected Object _intentionArg0 = null;
	protected Object _intentionArg1 = null;

	protected volatile boolean _clientMoving;
	protected volatile boolean _clientAutoAttacking;
	protected int _clientMovingToPawnOffset;

	private GameObject _target;
	private Creature _castTarget;
	protected Creature _attackTarget;
	protected Creature _followTarget;
	protected boolean _shift = false;

	protected Skill _skill;

	private int _moveToPawnTimeout;
	protected Future<?> _followTask = null;
	
	protected AbstractAI(Creature character)
	{
		_actor = character;
	}

	@Override
	public Creature getActor()
	{
		return _actor;
	}

	@Override
	public CtrlIntention getIntention()
	{
		return _intention;
	}

	protected void setCastTarget(Creature target)
	{
		_castTarget = target;
	}

	public Creature getCastTarget()
	{
		return _castTarget;
	}

	protected void setAttackTarget(Creature target)
	{
		_attackTarget = target;
	}

	@Override
	public Creature getAttackTarget()
	{
		return _attackTarget;
	}
	
	protected void setIsShiftClick(boolean value)
	{
		_shift = value;
	}
	
	public boolean isShiftClick()
	{
		return _shift;
	}

	protected synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_intention = intention;
		_intentionArg0 = arg0;
		_intentionArg1 = arg1;
	}

	@Override
	public final void setIntention(CtrlIntention intention)
	{
		setIntention(intention, null, null);
	}

	@Override
	public final void setIntention(CtrlIntention intention, Object arg0)
	{
		setIntention(intention, arg0, null);
	}

	@Override
	public final void setIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if ((intention != FOLLOW) && (intention != ATTACK))
		{
			stopFollow();
		}

		switch (intention)
		{
			case IDLE :
				onIntentionIdle();
				break;
			case ACTIVE :
				onIntentionActive();
				break;
			case REST :
				onIntentionRest();
				break;
			case ATTACK :
				onIntentionAttack((Creature) arg0, arg1 == null ? false : (boolean) arg1);
				break;
			case CAST :
				onIntentionCast((Skill) arg0, (GameObject) arg1);
				break;
			case MOVING :
				onIntentionMoveTo((Location) arg0, arg1 == null ? 0 : (int) arg1);
				break;
			case FOLLOW :
				onIntentionFollow((Creature) arg0);
				break;
			case PICK_UP :
				onIntentionPickUp((GameObject) arg0);
				break;
			case INTERACT :
				onIntentionInteract((GameObject) arg0);
				break;
		}
	}

	@Override
	public final void notifyEvent(CtrlEvent evt)
	{
		notifyEvent(evt, null, null);
	}

	@Override
	public final void notifyEvent(CtrlEvent evt, Object arg0)
	{
		notifyEvent(evt, arg0, null);
	}

	@Override
	public final void notifyEvent(CtrlEvent evt, Object arg0, Object arg1)
	{
		if ((!_actor.isVisible() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		switch (evt)
		{
			case EVT_THINK :
				onEvtThink();
				break;
			case EVT_ATTACKED :
				onEvtAttacked((Creature) arg0, ((Number) arg1).intValue());
				break;
			case EVT_CLAN_ATTACKED :
				onEvtClanAttacked((Creature) arg0, ((Number) arg1).intValue());
				break;
			case EVT_SPAWN :
				onEvtSpawn();
				break;
			case EVT_AGGRESSION :
				onEvtAggression((Creature) arg0, ((Number) arg1).intValue());
				break;
			case EVT_STUNNED :
				onEvtStunned((Creature) arg0);
				break;
			case EVT_PARALYZED :
				onEvtParalyzed((Creature) arg0);
				break;
			case EVT_SLEEPING :
				onEvtSleeping((Creature) arg0);
				break;
			case EVT_ROOTED :
				onEvtRooted((Creature) arg0);
				break;
			case EVT_CONFUSED :
				onEvtConfused((Creature) arg0);
				break;
			case EVT_MUTED :
				onEvtMuted((Creature) arg0);
				break;
			case EVT_EVADED :
				onEvtEvaded((Creature) arg0);
				break;
			case EVT_READY_TO_ACT :
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
				{
					onEvtReadyToAct();
				}
				break;
			case EVT_USER_CMD :
				onEvtUserCmd(arg0, arg1);
				break;
			case EVT_ARRIVED :
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
				{
					onEvtArrived();
				}
				break;
			case EVT_ARRIVED_ATTACK :
				onEvtArrivedTarget();
				break;
			case EVT_ARRIVED_TARGET :
				if (_actor.isMoving())
				{
					onEvtArrivedTarget();
				}
				break;
			case EVT_ARRIVED_BLOCKED :
				onEvtArrivedBlocked((Location) arg0);
				break;
			case EVT_FORGET_OBJECT :
				onEvtForgetObject((GameObject) arg0);
				break;
			case EVT_CANCEL :
				onEvtCancel();
				break;
			case EVT_DEAD :
				onEvtDead((Creature) arg0);
				break;
			case EVT_FAKE_DEATH :
				onEvtFakeDeath();
				break;
			case EVT_FINISH_CASTING :
				onEvtFinishCasting();
				break;
			case EVT_SEE_SPELL :
				onEvtSeeSpell((Skill) arg0, (Creature) arg1);
				break;
			case EVT_TIMER :
				onEvtTimer(((Number) arg0).intValue(), arg1);
				break;
			case EVT_STAND_UP :
				onEvtStandUp();
				break;
		}

		if ((_nextAction != null) && _nextAction.getEvents().contains(evt))
		{
			_nextAction.doAction();
			_nextAction = null;
		}
	}

	protected abstract void onIntentionIdle();

	protected abstract void onIntentionActive();

	protected abstract void onIntentionRest();

	protected abstract void onIntentionAttack(Creature target, boolean shift);

	protected abstract void onIntentionCast(Skill skill, GameObject target);

	protected abstract void onIntentionMoveTo(Location destination, int offset);

	protected abstract void onIntentionFollow(Creature target);

	protected abstract void onIntentionPickUp(GameObject item);

	protected abstract void onIntentionInteract(GameObject object);

	protected abstract void onEvtThink();

	protected abstract void onEvtAttacked(Creature attacker, int damage);
	
	protected abstract void onEvtClanAttacked(Creature attacker, int aggro);

	protected abstract void onEvtAggression(Creature target, int aggro);

	protected abstract void onEvtStunned(Creature attacker);

	protected abstract void onEvtParalyzed(Creature attacker);

	protected abstract void onEvtSleeping(Creature attacker);

	protected abstract void onEvtRooted(Creature attacker);

	protected abstract void onEvtConfused(Creature attacker);

	protected abstract void onEvtMuted(Creature attacker);

	protected abstract void onEvtEvaded(Creature attacker);

	protected abstract void onEvtReadyToAct();

	protected abstract void onEvtUserCmd(Object arg0, Object arg1);

	protected abstract void onEvtArrived();
	
	protected abstract void onEvtArrivedTarget();

	protected abstract void onEvtArrivedBlocked(Location blocked_at_pos);

	protected abstract void onEvtForgetObject(GameObject object);

	protected abstract void onEvtCancel();

	protected abstract void onEvtDead(Creature killer);

	protected abstract void onEvtSpawn();

	protected abstract void onEvtFakeDeath();

	protected abstract void onEvtFinishCasting();

	protected abstract void onEvtSeeSpell(Skill skill, Creature caster);

	protected abstract void onEvtTimer(int timerId, Object arg1);
	
	protected abstract void onEvtStandUp();
	
	protected void clientActionFailed()
	{
		if (_actor.isPlayer())
		{
			_actor.sendActionFailed();
		}
	}

	protected void moveToPawn(GameObject pawn, int offset)
	{
		if (!_actor.isMovementDisabled())
		{
			if (offset < 10)
			{
				offset = 10;
			}
			
			if (_clientMoving && (_target == pawn))
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (GameTimeController.getInstance().getGameTicks() < _moveToPawnTimeout)
					{
						return;
					}
				}
				else if (_actor.isInPathFinding())
				{
					if (GameTimeController.getInstance().getGameTicks() < (_moveToPawnTimeout + 10))
					{
						return;
					}
				}
			}
			
			_clientMoving = true;
			_clientMovingToPawnOffset = offset;
			_target = pawn;
			_moveToPawnTimeout = GameTimeController.getInstance().getGameTicks();
			_moveToPawnTimeout += 1000 / GameTimeController.MILLIS_IN_TICK;
			
			if (pawn == null)
			{
				return;
			}
			
			_actor.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
			
			if (!_actor.isMoving())
			{
				clientActionFailed();
				return;
			}
			
			if (pawn.isCreature())
			{
				if (_actor.isInPathFinding())
				{
					_actor.broadcastPacket(new MoveToLocation(_actor));
					_clientMovingToPawnOffset = 0;
				}
				else
				{
					_actor.broadcastPacket(new MoveToPawn(_actor, (Creature) pawn, offset));
				}
			}
			else
			{
				_actor.broadcastPacket(new MoveToLocation(_actor));
			}
		}
		else
		{
			clientActionFailed();
		}
	}
	
	protected void moveTo(Location loc)
	{
		moveTo(loc, 0);
	}
	
	protected void moveTo(int x, int y, int z, int offset)
	{
		moveTo(new Location(x, y, z), 0);
	}
	
	public void followTo(Location loc, int offset)
	{
		moveTo(loc, offset);
	}
	
	protected void moveTo(Location loc, int offset)
	{
		if (!_actor.isMovementDisabled())
		{
			_clientMoving = true;
			_clientMovingToPawnOffset = 0;
			
			_actor.moveToLocation(loc.getX(), loc.getY(), loc.getZ(), offset);
			if (!_actor.isMoving())
			{
				if (offset > 0)
				{
					_actor.moveToLocation(loc.getX(), loc.getY(), loc.getZ(), 0);
				}
				
				if (!_actor.isMoving())
				{
					clientActionFailed();
					final var player = _actor.getActingPlayer();
					if (player != null)
					{
						player.getFarmSystem().isLocked();
					}
					return;
				}
			}
			_actor.broadcastPacket(new MoveToLocation(_actor));
			if (_actor.isNpc())
			{
				_actor.broadcastPacket(new ValidateLocation(_actor));
			}
		}
		else
		{
			clientActionFailed();
		}
	}
	
	public void clientStopMoving(Location loc)
	{
		_clientMovingToPawnOffset = 0;
		if (_clientMoving || (loc != null))
		{
			_clientMoving = false;
			_actor.stopMove(loc);
			if (loc != null)
			{
				_actor.broadcastPacket(new FinishRotatings(_actor.getObjectId(), loc.getHeading(), 0));
			}
		}
		else
		{
			_actor.stopMove(false);
		}
	}

	protected void clientStoppedMoving()
	{
		final var isOffset = _clientMovingToPawnOffset > 0;
		if (isOffset)
		{
			_clientMovingToPawnOffset = 0;
		}
		_actor.stopMove(isOffset);
		_clientMoving = false;
	}

	public boolean isAutoAttacking()
	{
		return _clientAutoAttacking;
	}

	public void setAutoAttacking(boolean isAutoAttacking)
	{
		if (_actor.isSummon())
		{
			final Summon summon = (Summon) _actor;
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().setAutoAttacking(isAutoAttacking);
			}
			return;
		}
		_clientAutoAttacking = isAutoAttacking;
	}

	public void clientStartAutoAttack()
	{
		if (_actor.isSummon())
		{
			final Summon summon = (Summon) _actor;
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStartAutoAttack();
			}
			return;
		}
		if (!isAutoAttacking())
		{
			if (_actor.isPlayer() && _actor.hasSummon())
			{
				_actor.getSummon().broadcastPacket(new AutoAttackStart(_actor.getSummon().getObjectId()));
			}
			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
			setAutoAttacking(true);
		}
		AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
	}

	public void clientStopAutoAttack()
	{
		if (_actor.isSummon())
		{
			final Summon summon = (Summon) _actor;
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStopAutoAttack();
			}
			return;
		}
		if (_actor.isPlayer())
		{
			if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor) && isAutoAttacking())
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
			}
		}
		else if (isAutoAttacking())
		{
			stopAutoAttack();
		}
	}

	protected void clientNotifyDead()
	{
		_actor.broadcastPacket(new Die(_actor));
		_intention = IDLE;
		_target = null;
		_castTarget = null;
		_attackTarget = null;

		stopFollow();
	}
	
	public void describeStateToPlayer(Player player)
	{
		if (getActor().isVisibleFor(player))
		{
			if (_clientMoving)
			{
				if ((_clientMovingToPawnOffset != 0) && (_followTarget != null))
				{
					player.sendPacket(new MoveToPawn(_actor, _followTarget, _clientMovingToPawnOffset));
				}
				else
				{
					player.sendPacket(new MoveToLocation(_actor));
					player.sendPacket(new ValidateLocation(_actor));
				}
			}
		}
	}
	
	public void stopAutoAttack()
	{
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		setAutoAttacking(false);
	}

	public synchronized void startFollow(Creature target)
	{
		if (_followTask != null)
		{
			_followTask.cancel(false);
			_followTask = null;
		}
		_followTarget = target;
		_followTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new FollowTask(), 5, 1000);
	}
	
	public synchronized void startFollow(Creature target, int range)
	{
		if (_followTask != null)
		{
			_followTask.cancel(false);
			_followTask = null;
		}
		_followTarget = target;
		_followTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new FollowTask(range), 5, 250);
	}

	public synchronized void stopFollow()
	{
		if (_followTask != null)
		{
			_followTask.cancel(false);
			_followTask = null;
		}
		_followTarget = null;
	}
	
	protected Creature getFollowTarget()
	{
		return _followTarget;
	}

	protected GameObject getTarget()
	{
		return _target;
	}

	protected void setTarget(GameObject target)
	{
		_target = target;
	}

	public void stopAITask()
	{
		stopFollow();
	}
	
	public void startAITask()
	{
	}
	
	public void enableAI()
	{
	}
	
	public boolean isWalkingToHome()
	{
		return false;
	}

	@Override
	public String toString()
	{
		if (_actor == null)
		{
			return "Actor: null";
		}
		return "Actor: " + _actor;
	}
}