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
package l2e.gameserver.ai.npc;

import static l2e.gameserver.ai.model.CtrlIntention.ACTIVE;
import static l2e.gameserver.ai.model.CtrlIntention.ATTACK;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.MobGroup;
import l2e.gameserver.model.MobGroupData;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ControllableMobInstance;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.model.actor.instance.NpcInstance;
import l2e.gameserver.model.items.type.WeaponType;

public class Minions extends DefaultAI
{
	public static final int AI_IDLE = 1;
	public static final int AI_NORMAL = 2;
	public static final int AI_FORCEATTACK = 3;
	public static final int AI_FOLLOW = 4;
	public static final int AI_CAST = 5;
	public static final int AI_ATTACK_GROUP = 6;
	
	private int _alternateAI;

	private boolean _isThinking;
	private boolean _isNotMoving;
	
	private Creature _forcedTarget;
	private MobGroup _targetGroup;

	public Minions(ControllableMobInstance controllableMob)
	{
		super(controllableMob);
		setAlternateAI(AI_IDLE);
	}

	protected void thinkFollow()
	{
		final var me = (Attackable) _actor;

		if (!Util.checkIfInRange(MobGroupData.FOLLOW_RANGE, me, getForcedTarget(), true))
		{
			final var signX = (Rnd.nextInt(2) == 0) ? -1 : 1;
			final var signY = (Rnd.nextInt(2) == 0) ? -1 : 1;
			final var randX = Rnd.nextInt(MobGroupData.FOLLOW_RANGE);
			final var randY = Rnd.nextInt(MobGroupData.FOLLOW_RANGE);

			moveTo(getForcedTarget().getX() + (signX * randX), getForcedTarget().getY() + (signY * randY), getForcedTarget().getZ(), 0);
		}
	}

	@Override
	protected void onEvtThink()
	{
		if (isThinking())
		{
			return;
		}

		setThinking(true);

		try
		{
			switch (getAlternateAI())
			{
				case AI_IDLE :
					if (getIntention() != CtrlIntention.ACTIVE)
					{
						setIntention(CtrlIntention.ACTIVE);
					}
					break;
				case AI_FOLLOW :
					thinkFollow();
					break;
				case AI_CAST :
					thinkCast();
					break;
				case AI_FORCEATTACK :
					thinkForceAttack();
					break;
				case AI_ATTACK_GROUP :
					thinkAttackGroup();
					break;
				default :
					if (getIntention() == ACTIVE)
					{
						thinkActive();
					}
					else if (getIntention() == ATTACK)
					{
						thinkAttack();
					}
					break;
			}
		}
		finally
		{
			setThinking(false);
		}
	}

	@Override
	protected void thinkCast()
	{
		final var npc = (Attackable) _actor;

		if ((getAttackTarget() == null) || getAttackTarget().isAlikeDead())
		{
			setAttackTarget(findNextRndTarget());
			clientStopMoving(null);
		}

		if (getAttackTarget() == null)
		{
			return;
		}
		npc.setTarget(getAttackTarget());

		if (!_actor.isMuted())
		{
			var max_range = 0;

			for (final var sk : _actor.getAllSkills())
			{
				final var mpConsume = _actor.getStat().getMpConsume(sk)[0];
				if (Util.checkIfInRange(sk.getCastRange(), _actor, getAttackTarget(), true) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > mpConsume))
				{
					_actor.doCast(sk);
					return;
				}

				max_range = Math.max(max_range, sk.getCastRange());
			}

			if (!isNotMoving())
			{
				moveToPawn(getAttackTarget(), max_range);
			}

			return;
		}
	}

	protected void thinkAttackGroup()
	{
		final var target = getForcedTarget();
		if ((target == null) || target.isAlikeDead())
		{
			setForcedTarget(findNextGroupTarget());
			clientStopMoving(null);
		}

		if (target == null)
		{
			return;
		}

		_actor.setTarget(target);
		final var theTarget = (ControllableMobInstance) target;
		final var ctrlAi = (Minions) theTarget.getAI();
		ctrlAi.forceAttack(_actor);

		final var dist2 = _actor.getPlanDistanceSq(target.getX(), target.getY());
		final var range = (int) (_actor.getPhysicalAttackRange() + _actor.getColRadius() + target.getColRadius());
		int max_range = range;

		if (!_actor.isMuted() && (dist2 > ((range + 20) * (range + 20))))
		{
			for (final var sk : _actor.getAllSkills())
			{
				final var castRange = sk.getCastRange();
				final var mpConsume = _actor.getStat().getMpConsume(sk)[0];
				if (((castRange * castRange) >= dist2) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > mpConsume))
				{
					_actor.doCast(sk);
					return;
				}

				max_range = Math.max(max_range, castRange);
			}

			if (!isNotMoving())
			{
				moveToPawn(target, range);
			}

			return;
		}
		_actor.doAttack(target);
	}

	protected void thinkForceAttack()
	{
		if ((getForcedTarget() == null) || getForcedTarget().isAlikeDead())
		{
			clientStopMoving(null);
			setIntention(ACTIVE);
			setAlternateAI(AI_IDLE);
		}

		_actor.setTarget(getForcedTarget());
		final var dist2 = _actor.getPlanDistanceSq(getForcedTarget().getX(), getForcedTarget().getY());
		final var range = (int) (_actor.getPhysicalAttackRange() + _actor.getColRadius() + getForcedTarget().getColRadius());
		var max_range = range;

		if (!_actor.isMuted() && (dist2 > ((range + 20) * (range + 20))))
		{
			for (final var sk : _actor.getAllSkills())
			{
				final var castRange = sk.getCastRange();
				final var mpConsume = _actor.getStat().getMpConsume(sk)[0];
				if (((castRange * castRange) >= dist2) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > mpConsume))
				{
					_actor.doCast(sk);
					return;
				}

				max_range = Math.max(max_range, castRange);
			}

			if (!isNotMoving())
			{
				moveToPawn(getForcedTarget(), _actor.getPhysicalAttackRange()/* range */);
			}

			return;
		}
		_actor.doAttack(getForcedTarget());
	}

	@Override
	protected void thinkAttack()
	{
		if ((getAttackTarget() == null) || getAttackTarget().isAlikeDead())
		{
			if (getAttackTarget() != null)
			{
				final var npc = (Attackable) _actor;
				npc.getAggroList().stopHating(getAttackTarget());
			}

			setIntention(ACTIVE);
		}
		else
		{
			if (!((Npc) _actor).getFaction().isNone())
			{
				for (final var npc : World.getInstance().getAroundNpc(_actor))
				{
					if (!npc.getFaction().isNone() && !((Npc) _actor).isInFaction(npc))
					{
						continue;
					}

					if (_actor.isInsideRadius(npc, npc.getFaction().getRange(), false, true) && (Math.abs(getAttackTarget().getZ() - npc.getZ()) < 200))
					{
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getAttackTarget(), 1);
					}
				}
			}

			_actor.setTarget(getAttackTarget());
			final var dist2 = _actor.getPlanDistanceSq(getAttackTarget().getX(), getAttackTarget().getY());
			final var range = (int) (_actor.getPhysicalAttackRange() + _actor.getColRadius() + getAttackTarget().getColRadius());
			int max_range = range;

			if (!_actor.isMuted() && (dist2 > ((range + 20) * (range + 20))))
			{
				for (final var sk : _actor.getAllSkills())
				{
					final var castRange = sk.getCastRange();
					final var mpConsume = _actor.getStat().getMpConsume(sk)[0];
					if (((castRange * castRange) >= dist2) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > mpConsume))
					{
						_actor.doCast(sk);
						return;
					}

					max_range = Math.max(max_range, castRange);
				}

				moveToPawn(getAttackTarget(), range);
				return;
			}
			Creature hated;

			if (_actor.isConfused())
			{
				hated = findNextRndTarget();
			}
			else
			{
				hated = getAttackTarget();
			}

			if (hated == null)
			{
				setIntention(ACTIVE);
				return;
			}

			if (hated != getAttackTarget())
			{
				setAttackTarget(hated);
			}

			if (!_actor.isMuted() && (Rnd.nextInt(5) == 3))
			{
				for (final var sk : _actor.getAllSkills())
				{
					final var castRange = sk.getCastRange();
					final var mpConsume = _actor.getStat().getMpConsume(sk)[0];
					if (((castRange * castRange) >= dist2) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > mpConsume))
					{
						_actor.doCast(sk);
						return;
					}
				}
			}
			_actor.doAttack(getAttackTarget());
		}
	}

	@Override
	protected boolean thinkActive()
	{
		setAttackTarget(findNextRndTarget());
		Creature hated;

		if (_actor.isConfused())
		{
			hated = findNextRndTarget();
		}
		else
		{
			hated = getAttackTarget();
		}

		if (hated != null)
		{
			_actor.setRunning();
			setIntention(CtrlIntention.ATTACK, hated);
		}
		return true;
	}

	@Override
	protected boolean checkAggression(Creature target)
	{
		if ((target == null) || !(_actor instanceof Attackable))
		{
			return false;
		}
		final var me = (Attackable) _actor;

		if ((target instanceof NpcInstance) || (target instanceof DoorInstance))
		{
			return false;
		}

		if (target.isAlikeDead() || !me.isInsideRadius(target, me.getAggroRange(), false, false) || (Math.abs(_actor.getZ() - target.getZ()) > 100))
		{
			return false;
		}

		if (target.isInvul())
		{
			return false;
		}

		if ((target instanceof Player) && ((Player) target).isSpawnProtected())
		{
			return false;
		}

		if (target.isPlayable())
		{
			if (((Playable) target).isSilentMoving())
			{
				return false;
			}
		}

		if (target instanceof Npc)
		{
			return false;
		}

		return me.isAggressive();
	}

	private Creature findNextRndTarget()
	{
		final var aggroRange = ((Attackable) _actor).getAggroRange();
		final var npc = (Attackable) _actor;
		int npcX, npcY, targetX, targetY;
		double dy, dx;
		final var dblAggroRange = aggroRange * aggroRange;

		final List<Creature> potentialTarget = new ArrayList<>();

		for (final var obj : World.getInstance().getAroundCharacters(npc))
		{
			npcX = npc.getX();
			npcY = npc.getY();
			targetX = obj.getX();
			targetY = obj.getY();

			dx = npcX - targetX;
			dy = npcY - targetY;

			if (((dx * dx) + (dy * dy)) > dblAggroRange)
			{
				continue;
			}

			final var target = obj;

			if (checkAggression(target))
			{
				potentialTarget.add(target);
			}
		}

		if (potentialTarget.isEmpty())
		{
			return null;
		}

		final var choice = Rnd.nextInt(potentialTarget.size());
		final var target = potentialTarget.get(choice);

		return target;
	}

	private ControllableMobInstance findNextGroupTarget()
	{
		return getGroupTarget().getRandomMob();
	}
	
	public int getAlternateAI()
	{
		return _alternateAI;
	}

	public void setAlternateAI(int _alternateai)
	{
		_alternateAI = _alternateai;
	}

	public void forceAttack(Creature target)
	{
		setAlternateAI(AI_FORCEATTACK);
		setForcedTarget(target);
	}

	public void forceAttackGroup(MobGroup group)
	{
		setForcedTarget(null);
		setGroupTarget(group);
		setAlternateAI(AI_ATTACK_GROUP);
	}

	public void stop()
	{
		setAlternateAI(AI_IDLE);
		clientStopMoving(null);
	}

	public void move(int x, int y, int z)
	{
		moveTo(x, y, z, 0);
	}

	public void follow(Creature target)
	{
		setAlternateAI(AI_FOLLOW);
		setForcedTarget(target);
	}

	public boolean isThinking()
	{
		return _isThinking;
	}

	public boolean isNotMoving()
	{
		return _isNotMoving;
	}

	public void setNotMoving(boolean isNotMoving)
	{
		_isNotMoving = isNotMoving;
	}

	public void setThinking(boolean isThinking)
	{
		_isThinking = isThinking;
	}

	private Creature getForcedTarget()
	{
		return _forcedTarget;
	}

	private MobGroup getGroupTarget()
	{
		return _targetGroup;
	}

	private void setForcedTarget(Creature forcedTarget)
	{
		_forcedTarget = forcedTarget;
	}

	private void setGroupTarget(MobGroup targetGroup)
	{
		_targetGroup = targetGroup;
	}
	
	@Override
	protected boolean createNewTask()
	{
		return defaultFightTask();
	}

	@Override
	protected int getRatePHYS()
	{
		return 10;
	}

	@Override
	protected int getRateDOT()
	{
		return 8;
	}

	@Override
	protected int getRateDEBUFF()
	{
		return 5;
	}

	@Override
	protected int getRateDAM()
	{
		return 5;
	}

	@Override
	protected int getRateSTUN()
	{
		return 8;
	}

	@Override
	protected int getRateBUFF()
	{
		return 5;
	}

	@Override
	protected int getRateHEAL()
	{
		return 5;
	}
	
	@Override
	protected int getRateSuicide()
	{
		return 3;
	}

	@Override
	protected int getRateRes()
	{
		return 2;
	}
	
	@Override
	protected int getRateDodge()
	{
		final var weaponItem = getActiveChar().getActiveWeaponItem();
		if (weaponItem.getItemType() == WeaponType.BOW)
		{
			return 15;
		}
		return 0;
	}
}