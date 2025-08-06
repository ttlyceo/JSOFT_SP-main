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

import static l2e.gameserver.ai.model.CtrlIntention.ACTIVE;
import static l2e.gameserver.ai.model.CtrlIntention.ATTACK;
import static l2e.gameserver.ai.model.CtrlIntention.CAST;
import static l2e.gameserver.ai.model.CtrlIntention.FOLLOW;
import static l2e.gameserver.ai.model.CtrlIntention.IDLE;
import static l2e.gameserver.ai.model.CtrlIntention.INTERACT;
import static l2e.gameserver.ai.model.CtrlIntention.MOVING;
import static l2e.gameserver.ai.model.CtrlIntention.PICK_UP;
import static l2e.gameserver.ai.model.CtrlIntention.REST;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.AbstractAI;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.instancemanager.WalkingManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.instance.ItemInstance.ItemLocation;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AutoAttackStop;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public class CharacterAI extends AbstractAI
{
	private final List<ScheduledFuture<?>> _timers = new ArrayList<>();
	
	public CharacterAI(Creature character)
	{
		super(character);
	}

	public static class IntentionCommand
	{
		protected final CtrlIntention _crtlIntention;
		protected final Object _arg0, _arg1;
		
		protected IntentionCommand(CtrlIntention pIntention, Object pArg0, Object pArg1)
		{
			_crtlIntention = pIntention;
			_arg0 = pArg0;
			_arg1 = pArg1;
		}
		
		public CtrlIntention getCtrlIntention()
		{
			return _crtlIntention;
		}
	}

	public static class CastTask implements Runnable
	{
		private final Creature _activeChar;
		private final GameObject _target;
		private final Skill _skill;
		
		public CastTask(Creature actor, Skill skill, GameObject target)
		{
			_activeChar = actor;
			_target = target;
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			if (_activeChar.isAttackingNow())
			{
				_activeChar.abortAttack();
			}
			_activeChar.getAI().changeIntentionToCast(_skill, _target);
		}
	}
	
	public IntentionCommand getNextIntention()
	{
		return null;
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		if ((attacker instanceof Attackable) && !((Attackable) attacker).isCoreAIDisabled())
		{
			if (_actor.isPlayer())
			{
				if (_actor.getTarget() == null)
				{
					_actor.setTarget(attacker);
				}
				
				if (_actor.isInvul())
				{
					return;
				}
			}
			clientStartAutoAttack();
		}
	}
	
	@Override
	protected void onEvtClanAttacked(Creature attacker, int aggro)
	{
	}
	
	@Override
	protected void onIntentionIdle()
	{
		changeIntention(IDLE, null, null);
		
		setCastTarget(null);
		setAttackTarget(null);
		
		clientStopMoving(null);
		clientStopAutoAttack();
		
	}
	
	@Override
	protected void onIntentionActive()
	{
		if (getIntention() != ACTIVE)
		{
			changeIntention(ACTIVE, null, null);
			
			setCastTarget(null);
			setAttackTarget(null);
			
			clientStopMoving(null);
			clientStopAutoAttack();
			
			onEvtThink();
		}
	}
	
	@Override
	protected void onIntentionRest()
	{
		setIntention(IDLE);
	}
	
	@Override
	protected void onIntentionAttack(Creature target, boolean shift)
	{
		if ((target == null) || !target.isTargetable())
		{
			clientActionFailed();
			return;
		}
		
		if (getIntention() == REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAfraid())
		{
			clientActionFailed();
			return;
		}
		
		if (getIntention() == ATTACK)
		{
			if (getAttackTarget() != target)
			{
				setAttackTarget(target);
				setIsShiftClick(shift);
				stopFollow();
				notifyEvent(CtrlEvent.EVT_THINK);
			}
			else
			{
				clientActionFailed();
				if (!_actor.isAttackingNow() && !_actor.isMoving())
				{
					final var haveAction = _actor.getAI().getNextAction() != null && _actor.getAI().getNextAction().isAttackAction();
					if (!haveAction)
					{
						notifyEvent(CtrlEvent.EVT_THINK);
					}
				}
			}
		}
		else
		{
			changeIntention(ATTACK, target, null);
			setAttackTarget(target);
			setIsShiftClick(shift);
			stopFollow();
			notifyEvent(CtrlEvent.EVT_THINK);
		}
	}
	
	@Override
	protected void onIntentionCast(Skill skill, GameObject target)
	{
		if (((getIntention() == REST) && skill.isMagic()) || _actor.isAfraid())
		{
			clientActionFailed();
			_actor.setIsCastingNow(false);
			return;
		}
		
		final var bowAttackDelay = _actor.getBowAttackEndTime() - System.currentTimeMillis();
		final var normalAttackDelay = TimeUnit.MILLISECONDS.convert(_actor.getAttackEndTime() - System.nanoTime(), TimeUnit.NANOSECONDS);
		if ((bowAttackDelay > 0) || (normalAttackDelay > 0))
		{
			ThreadPoolManager.getInstance().schedule(new CastTask(_actor, skill, target), bowAttackDelay > 0 ? (int) (bowAttackDelay * 0.50) : normalAttackDelay);
			return;
		}
		changeIntentionToCast(skill, target);
	}
	
	protected void changeIntentionToCast(Skill skill, GameObject target)
	{
		setCastTarget((Creature) target);
		if (skill.getHitTime() > 50)
		{
			_actor.abortAttack();
		}
		_skill = skill;
		changeIntention(CAST, skill, target);
		notifyEvent(CtrlEvent.EVT_THINK);
	}
	
	@Override
	protected void onIntentionMoveTo(Location loc, int offset)
	{
		if (getIntention() == REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isActionsDisabled())
		{
			clientActionFailed();
			return;
		}
		changeIntention(MOVING, loc, offset);
		
		clientStopAutoAttack();
		
		_actor.abortAttack();
		
		moveTo(loc, offset);
	}
	
	@Override
	protected void onIntentionFollow(Creature target)
	{
		if (getIntention() == REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isActionsDisabled())
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isMovementDisabled())
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isDead())
		{
			clientActionFailed();
			return;
		}
		
		if (_actor == target)
		{
			clientActionFailed();
			return;
		}
		clientStopAutoAttack();
		changeIntention(FOLLOW, target, null);
		startFollow(target);
	}
	
	@Override
	protected void onIntentionPickUp(GameObject object)
	{
		if (getIntention() == REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isActionsDisabled())
		{
			clientActionFailed();
			return;
		}
		clientStopAutoAttack();
		
		if ((object instanceof ItemInstance) && (((ItemInstance) object).getItemLocation() != ItemLocation.VOID))
		{
			return;
		}
		
		changeIntention(PICK_UP, object, null);
		
		setTarget(object);
		if ((object.getX() == 0) && (object.getY() == 0) && (object.getZ() == 0))
		{
			_log.warn("Object in coords 0,0,0 - using a temporary fix");
			object.setXYZ(getActor().getX(), getActor().getY(), getActor().getZ() + 5);
		}
		moveToPawn(object, 20);
	}
	
	@Override
	protected void onIntentionInteract(GameObject object)
	{
		if (getIntention() == REST)
		{
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isActionsDisabled())
		{
			clientActionFailed();
			return;
		}
		clientStopAutoAttack();
		
		if (getIntention() != INTERACT)
		{
			changeIntention(INTERACT, object, null);
			setTarget(object);
			moveTo(object.getLocation(), 40);
		}
	}
	
	@Override
	protected void onEvtThink()
	{
	}
	
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
	}
	
	@Override
	protected void onEvtStunned(Creature attacker)
	{
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		setAutoAttacking(false);
		clientStopMoving(null);
		onEvtAttacked(attacker, 0);
	}
	
	@Override
	protected void onEvtParalyzed(Creature attacker)
	{
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		setAutoAttacking(false);
		clientStopMoving(null);
		onEvtAttacked(attacker, 0);
	}
	
	@Override
	protected void onEvtSleeping(Creature attacker)
	{
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		setAutoAttacking(false);
		clientStopMoving(null);
	}
	
	@Override
	protected void onEvtRooted(Creature attacker)
	{
		clientStopMoving(null);
		onEvtAttacked(attacker, 0);
	}
	
	@Override
	protected void onEvtConfused(Creature attacker)
	{
		clientStopMoving(null);
		onEvtAttacked(attacker, 0);
	}
	
	@Override
	protected void onEvtMuted(Creature attacker)
	{
		onEvtAttacked(attacker, 0);
	}
	
	@Override
	protected void onEvtEvaded(Creature attacker)
	{
	}
	
	@Override
	protected void onEvtReadyToAct()
	{
		onEvtThink();
	}
	
	@Override
	protected void onEvtUserCmd(Object arg0, Object arg1)
	{
	}
	
	@Override
	protected void onEvtArrived()
	{
		_actor.revalidateZone(true);
		
		if (_actor.moveToNextRoutePoint())
		{
			if (_actor.isSummon() && (_actor.getAI().getIntention() == CtrlIntention.IDLE))
			{
				((Summon) _actor).setFollowStatus(true);
			}
			return;
		}
		
		if (_actor.isPlayer() && (_actor.getAI().getIntention() == CtrlIntention.CAST) && getCastTarget() != null && getCastTarget().isDoor())
		{
			int x = 0, y = 0, z = 0;
			if (getCastTarget().isDoor())
			{
				final var dor = (DoorInstance) getCastTarget();
				x = dor.getTemplate().posX;
				y = dor.getTemplate().posY;
				z = dor.getTemplate().posZ + 32;
			}
			
			if (!GeoEngine.getInstance().canMoveToCoord(_actor, _actor.getX(), _actor.getY(), _actor.getZ(), x, y, z, _actor.getReflection(), false))
			{
				setIntention(CtrlIntention.IDLE);
			}
		}
		
		if (_actor instanceof Attackable)
		{
			((Attackable) _actor).setisReturningToSpawnPoint(false);
		}
		
		clientStoppedMoving();
		
		if (_actor instanceof Npc)
		{
			final var npc = (Npc) _actor;
			WalkingManager.getInstance().onArrived(npc);
			
			if (npc.getTemplate().getEventQuests(QuestEventType.ON_MOVE_FINISHED) != null)
			{
				for (final Quest quest : npc.getTemplate().getEventQuests(QuestEventType.ON_MOVE_FINISHED))
				{
					quest.notifyMoveFinished(npc);
				}
			}
		}
		
		if (getIntention() == MOVING)
		{
			setIntention(ACTIVE);
		}
		onEvtThink();
	}
	
	@Override
	protected void onEvtArrivedTarget()
	{
		onEvtThink();
	}
	
	@Override
	protected void onEvtStandUp()
	{
		setIntention(IDLE);
	}
	
	@Override
	protected void onEvtArrivedBlocked(Location loc)
	{
		if ((getIntention() == MOVING) || (getIntention() == CAST))
		{
			setIntention(ACTIVE);
		}
		
		clientStopMoving(loc);
		onEvtThink();
	}
	
	@Override
	protected void onEvtForgetObject(GameObject object)
	{
		if (getTarget() == object)
		{
			setTarget(null);
			
			if ((getIntention() == INTERACT) || (getIntention() == PICK_UP))
			{
				setIntention(ACTIVE);
			}
		}
		
		if (getAttackTarget() == object)
		{
			setAttackTarget(null);
			setIntention(ACTIVE);
		}
		
		if (getCastTarget() == object)
		{
			setCastTarget(null);
			setIntention(ACTIVE);
		}
		
		if (getFollowTarget() == object)
		{
			clientStopMoving(null);
			stopFollow();
			setIntention(ACTIVE);
		}
		
		if (_actor == object)
		{
			setTarget(null);
			setAttackTarget(null);
			setCastTarget(null);
			stopFollow();
			clientStopMoving(null);
			changeIntention(IDLE, null, null);
		}
	}
	
	@Override
	protected void onEvtCancel()
	{
		_actor.abortCast();
		stopFollow();
		
		if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		}
		
		onEvtThink();
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		stopAITask();
		clientNotifyDead();
		
		if (!(_actor instanceof Playable))
		{
			_actor.setWalking();
		}
	}
	
	@Override
	protected void onEvtFakeDeath()
	{
		stopFollow();
		clientStopMoving(null);
		_intention = IDLE;
		setTarget(null);
		setCastTarget(null);
		setAttackTarget(null);
	}
	
	@Override
	protected void onEvtTimer(int timerId, Object arg1)
	{
		final var actor = getActor();
		if (actor == null)
		{
			return;
		}
		actor.onEvtTimer(timerId, arg1);
	}
	
	@Override
	protected void onEvtFinishCasting()
	{
	}

	@Override
	protected void onEvtSeeSpell(Skill skill, Creature caster)
	{
	}
	
	@Override
	protected void onEvtSpawn()
	{
	}
	
	protected boolean maybeMoveToPosition(Location worldPosition, int offset)
	{
		if (worldPosition == null)
		{
			return false;
		}
		
		if (offset < 0)
		{
			return false;
		}
		
		if (!_actor.isInsideRadius(worldPosition.getX(), worldPosition.getY(), (int) (offset + _actor.getColRadius()), false))
		{
			if (_actor.isMovementDisabled())
			{
				return true;
			}
			
			if (!_actor.isRunning() && !(this instanceof PlayableAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			
			var x = _actor.getX();
			var y = _actor.getY();
			
			final var dx = worldPosition.getX() - x;
			final var dy = worldPosition.getY() - y;
			
			var dist = Math.sqrt((dx * dx) + (dy * dy));
			
			final var sin = dy / dist;
			final var cos = dx / dist;
			
			dist -= offset - 5;
			
			x += (int) (dist * cos);
			y += (int) (dist * sin);
			
			moveTo(x, y, worldPosition.getZ(), 0);
			return true;
		}
		
		if (getFollowTarget() != null)
		{
			stopFollow();
		}
		return false;
	}
	
	protected boolean maybeMoveToPawn(GameObject target, int offset, boolean isBehind)
	{
		if (target == null || offset < 0)
		{
			return false;
		}
		
		offset += _actor.getColRadius() / 2;
		if (target instanceof Creature)
		{
			offset += target.isDoor() ? 60 : (target.getColRadius() / 2);
		}
		
		final boolean needToMove;
		var xPoint = 0;
		var yPoint = 0;
		var zPoint = 0;
		
		if (target.isDoor())
		{
			final var dor = (DoorInstance) target;
			xPoint = dor.getTemplate().posX;
			yPoint = dor.getTemplate().posY;
			zPoint = dor.getTemplate().posZ + 32;
			
			needToMove = !_actor.isInsideRadius(xPoint, yPoint, zPoint, offset, false, false);
		}
		else
		{
			if (target instanceof ItemInstance)
			{
				needToMove = !_actor.isInRange(target, offset);
			}
			else
			{
				needToMove = !_actor.isInsideRadius(target, offset, false, false);
			}
		}
		
		if (needToMove)
		{
			if (isShiftClick())
			{
				_actor.getAI().setIntention(CtrlIntention.IDLE);
				setIsShiftClick(false);
				stopFollow();
				return true;
			}
			
			if (getFollowTarget() != null)
			{
				if (!_actor.isInsideRadius(target, isBehind ? 5 : offset + 50, false, false))
				{
					if ((target instanceof Creature) && !(target instanceof DoorInstance))
					{
						startFollow((Creature) target, Math.max(offset, 5));
					}
					return true;
				}
				stopFollow();
				return false;
			}
			
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				if (_actor.getAI().getIntention() == CtrlIntention.ATTACK)
				{
					_actor.getAI().setIntention(CtrlIntention.IDLE);
				}
				return true;
			}
			
			if (_actor.isFlying() && (_actor.getAI().getIntention() == CtrlIntention.CAST) && (_actor.isPlayer()) && _actor.getActingPlayer().isTransformed())
			{
				if (!_actor.getActingPlayer().getTransformation().isCombat())
				{
					_actor.sendPacket(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
					_actor.sendActionFailed();
					return true;
				}
			}
			
			if (!_actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			if ((target instanceof Creature) && !(target instanceof DoorInstance))
			{
				startFollow((Creature) target, isBehind ? 5 : Math.max(offset, 5));
			}
			else if (target instanceof DoorInstance)
			{
				moveTo(xPoint, yPoint, zPoint, Math.max(offset, 5));
			}
			else
			{
				moveTo(target.getLocation(), isBehind ? 5 : Math.max(offset, 5));
			}
			return true;
		}
		
		if (getFollowTarget() != null)
		{
			stopFollow();
		}
		return false;
	}
	
	protected boolean checkTargetLostOrDead(Creature target)
	{
		if ((target == null) || target.isAlikeDead())
		{
			if ((target instanceof Player) && ((Player) target).isFakeDeathNow())
			{
				target.stopFakeDeath(true);
				return false;
			}
			setIntention(ACTIVE);
			return true;
		}
		return false;
	}
	
	protected boolean checkTargetLost(GameObject target)
	{
		if (target == null)
		{
			setIntention(ACTIVE);
			return true;
		}
		
		if ((_actor != null) && (_skill != null) && _skill.isOffensive() && (_skill.getAffectRange() > 0) && (Config.GEODATA) && !GeoEngine.getInstance().canSeeTarget(_actor, target))
		{
			setIntention(ACTIVE);
			return true;
		}
		return false;
	}
	
	public boolean canAura(Skill sk)
	{
		if (sk != null && sk.isAura())
		{
			for (final var target : World.getInstance().getAroundCharacters(_actor, sk.getAffectRange(), 200))
			{
				if (target == getAttackTarget())
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean canAOE(Skill sk)
	{
		if (sk == null)
		{
			return false;
		}
		
		if (sk.hasEffectType(EffectType.CANCEL, EffectType.CANCEL_ALL, EffectType.CANCEL_BY_SLOT, EffectType.NEGATE))
		{
			if (sk.isAura())
			{
				var cancast = true;
				for (final var target : World.getInstance().getAroundCharacters(_actor, sk.getAffectRange(), 200))
				{
					if (target != null)
					{
						if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
						{
							continue;
						}
						if (target instanceof Attackable)
						{
							final var actors = ((Npc) _actor);
							
							if (actors.getFaction().isNone())
							{
								continue;
							}
						}
					
						final Effect[] effects = target.getAllEffects();
						for (int i = 0; (effects != null) && (i < effects.length); i++)
						{
							final Effect effect = effects[i];
							if (effect.getSkill() == sk)
							{
								cancast = false;
								break;
							}
						}
					}
				}
				if (cancast)
				{
					return true;
				}
			}
			else if (sk.isArea())
			{
				var cancast = true;
				for (final var target : World.getInstance().getAroundCharacters(_actor, sk.getAffectRange(), 200))
				{
					if (target != null)
					{
						if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || (target == null))
						{
							continue;
						}
						if (target instanceof Attackable)
						{
							final var actors = ((Npc) _actor);
							if (actors.getFaction().isNone())
							{
								continue;
							}
						}
						final Effect[] effects = target.getAllEffects();
						if (effects.length > 0)
						{
							cancast = true;
						}
					}
				}
				
				if (cancast)
				{
					return true;
				}
			}
		}
		else
		{
			if (sk.isAura())
			{
				var cancast = false;
				for (final var target : World.getInstance().getAroundCharacters(_actor, sk.getAffectRange(), 200))
				{
					if (target != null)
					{
						if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
						{
							continue;
						}
						if (target instanceof Attackable)
						{
							final var actors = ((Npc) _actor);
							if (actors.getFaction().isNone())
							{
								continue;
							}
						}
						final Effect[] effects = target.getAllEffects();
						if (effects.length > 0)
						{
							cancast = true;
						}
					}
				}
				
				if (cancast)
				{
					return true;
				}
			}
			else if (sk.isArea())
			{
				var cancast = true;
				for (final var target : World.getInstance().getAroundCharacters(_actor, sk.getAffectRange(), 200))
				{
					if (target != null)
					{
						if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
						{
							continue;
						}
						if (target instanceof Attackable)
						{
							final var actors = ((Npc) _actor);
							if (actors.getFaction().isNone())
							{
								continue;
							}
						}
					
						final Effect[] effects = target.getAllEffects();
						for (int i = 0; (effects != null) && (i < effects.length); i++)
						{
							final Effect effect = effects[i];
							if (effect.getSkill() == sk)
							{
								cancast = false;
								break;
							}
						}
					}
				}
				
				if (cancast)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean canParty(Skill sk)
	{
		if (sk.getTargetType() == TargetType.PARTY || sk.getTargetType() == TargetType.CLAN)
		{
			int count = 0;
			int ccount = 0;
			
			final var minions = _actor.isNpc() ? _actor.isMinion() && ((Npc) _actor).getLeader() != null ? ((Npc) _actor).getLeader().getMinionList() : ((Npc) _actor).hasMinions() ? ((Npc) _actor).getMinionList() : null : null;
			for (final var target : World.getInstance().getAroundCharacters(_actor, sk.getAffectRange(), 200))
			{
				if (!(target instanceof Attackable) || !GeoEngine.getInstance().canSeeTarget(_actor, target))
				{
					continue;
				}
				final var targets = ((Npc) target);
				final var actors = ((Npc) _actor);
				if ((!actors.getFaction().isNone() && actors.isInFaction((targets)) || minions != null && minions.hasNpcId(targets.getId())))
				{
					count++;
					final Effect[] effects = target.getAllEffects();
					for (int i = 0; (effects != null) && (i < effects.length); i++)
					{
						final Effect effect = effects[i];
						if (effect.getSkill() == sk)
						{
							ccount++;
							break;
						}
					}
				}
			}
			if (ccount < count)
			{
				return true;
			}
			
		}
		return false;
	}
	
	public boolean isParty(Skill sk)
	{
		if (sk.getTargetType() == TargetType.PARTY)
		{
			return true;
		}
		return false;
	}
	
	public void stopAllTaskAndTimers()
	{
		for (final var timer : _timers)
		{
			timer.cancel(false);
		}
		_timers.clear();
	}
	
	public void addTimer(int timerId, long delay)
	{
		addTimer(timerId, null, delay);
	}
	
	public void addTimer(int timerId, Object arg1, long delay)
	{
		final var timer = ThreadPoolManager.getInstance().schedule(new Timer(timerId, arg1), delay);
		if (timer != null)
		{
			_timers.add(timer);
		}
	}
	
	protected class Timer implements Runnable
	{
		private final int _timerId;
		private final Object _arg1;
		
		public Timer(int timerId, Object arg1)
		{
			_timerId = timerId;
			_arg1 = arg1;
		}
		
		@Override
		public void run()
		{
			notifyEvent(CtrlEvent.EVT_TIMER, _timerId, _arg1);
		}
	}
	
	public void setLifeTime(int lifeTime)
	{
	}
}