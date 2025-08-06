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
package l2e.gameserver.model.actor.tasks.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;

/**
 * Created by LordWinter
 */
public class AutoHealFarmTask extends LoggerObject implements Runnable
{
	public static final Logger _log = LoggerFactory.getLogger(AutoHealFarmTask.class);
	private final Player _player;
	private Attackable _committedTarget = null;
	private Player _committedOwner = null;
	private boolean _lock = false;
	private final int _farmInterval;
    
	public AutoHealFarmTask(Player player)
	{
		_player = player;
		_farmInterval = player.getFarmSystem().getTaskInterval();
    }
    
	@Override
	public void run()
	{
		if (_player == null)
		{
			return;
		}
		tryAttack();
    }

	private void tryAttack()
	{
		if (!_lock)
		{
			_lock = true;
			try
			{
				if (!_player.getFarmSystem().isInWaitStatus(_committedTarget))
				{
					if (_player.getFarmSystem().isAbortTarget())
					{
						_committedTarget = null;
						_player.getFarmSystem().setAbortTarget(false);
					}
					
					final boolean canAttack = selectRandomTarget();
					if (canAttack)
					{
						physicalAttack();
					}
					
					if (!tryUseSpell(canAttack))
					{
						if (canAttack)
						{
							physicalAttack();
						}
					}
				}
			}
			finally
			{
				_lock = false;
				_player.getFarmSystem()._farmTask = ThreadPoolManager.getInstance().schedule(this, _farmInterval);
			}
		}
    }

	private boolean tryUseSpell(boolean selected)
	{
		final var isPartySupport = _player.getFarmSystem().isPartySupport();
		if (selected)
		{
			final var chanceSkill = _player.getFarmSystem().nextChanceSkill(_committedTarget, 0);
			if (chanceSkill != null)
			{
				useMagicSkill(chanceSkill, false, true);
				return true;
			}
		}

		final var lowLifeSkill = _player.getFarmSystem().nextHealSkill(_committedTarget, _committedOwner);
		if (lowLifeSkill != null)
		{
			useMagicSkill(lowLifeSkill, lowLifeSkill.getTargetType() == TargetType.SELF, lowLifeSkill.isOffensive());
			return true;
        }

		if (isPartySupport)
		{
			final var selfSkills = _player.getFarmSystem().nextSelfPartySkill(_committedOwner, _player.getParty());
			if (selfSkills != null)
			{
				useMagicSkill(selfSkills, selfSkills.getTargetType() == TargetType.SELF, false);
				return true;
			}
		}
		else
		{
			final var selfSkills = _player.getFarmSystem().nextSelfSkill(_committedOwner);
			if (selfSkills != null)
			{
				useMagicSkill(selfSkills, selfSkills.getTargetType() == TargetType.SELF, false);
				return true;
			}
		}
		
		if (selected)
		{
			final var attackSkill = _player.getFarmSystem().nextAttackSkill(_committedTarget, 0);
			if (attackSkill != null)
			{
				useMagicSkill(attackSkill, false, true);
				return true;
			}
		}
		return false;
    }

	private void physicalAttack()
	{
		if (_committedOwner != null)
		{
			if (Math.sqrt(_player.getDistanceSq(_committedOwner)) > 200)
			{
				_player.setTarget(null);
				final var loc = Location.findPointToStay(_committedOwner.getLocation(), 100, false);
				if (loc != null)
				{
					_player.getAI().setIntention(CtrlIntention.MOVING, loc, 0);
				}
				return;
			}
		}
		
		if (_committedTarget != null && (_player.getFarmSystem().isAssistMonsterAttack() || !_player.getFarmSystem().isLeaderAssist()))
		{
			if (_committedTarget.isAutoAttackable(_player, false) && !_committedTarget.isAlikeDead())
			{
				_player.getAI().setIntention(CtrlIntention.ATTACK, _committedTarget, false);
			}
		}
    }

	private boolean selectRandomTarget()
	{
		if (_player.isDead())
		{
			_player.getFarmSystem().checkEmptyTime();
			return false;
		}
		
		final var isLeaderAssist = _player.getFarmSystem().isLeaderAssist();
		final var isPartySupport = _player.getFarmSystem().isPartySupport();
		final var isAssistMonster = _player.getFarmSystem().isAssistMonsterAttack();
		
		if (_committedTarget != null && (isAssistMonster || isLeaderAssist))
		{
			final boolean activePlayer = !_committedTarget.isDead() && _committedTarget.isVisible();
			if (!activePlayer)
			{
				_committedTarget = null;
				_player.setTarget(null);
			}
			else
			{
				if (GeoEngine.getInstance().canSeeTarget(_player, _committedTarget))
				{
					if (_player.getTarget() != _committedTarget)
					{
						_player.setTarget(_committedTarget);
					}
					return true;
				}
				else
				{
					_committedTarget = null;
					_player.setTarget(null);
				}
			}
        }
		
		if (_player.getFarmSystem().getResTarget() != null)
		{
			if (_player.getFarmSystem().tryResTarget())
			{
				return false;
			}
		}
		
		if (isLeaderAssist || isPartySupport)
		{
			if (_player.getParty() == null)
			{
				_committedOwner = null;
				_player.getFarmSystem().setLeaderAssist(false, false);
			}
			else
			{
				_committedOwner = _player.getFarmSystem().getSelectPartyHealPlayer(_player.getParty());
			}
			
			if (_committedOwner != null && Math.sqrt(_player.getDistanceSq(_committedOwner)) > 200 && isPartySupport)
			{
				final var loc = Location.findPointToStay(_committedOwner.getLocation(), 100, false);
				if (loc != null)
				{
					_player.getAI().setIntention(CtrlIntention.MOVING, loc, 0);
					_player.getFarmSystem().addWaitTime(System.currentTimeMillis() + 2000L);
				}
				return false;
			}
		}
		
		if (_committedOwner != null && !_committedOwner.isDead() && (isAssistMonster || isLeaderAssist))
		{
			_committedTarget = _player.getFarmSystem().getLeaderTarget(_committedOwner);
			if (_committedTarget != null)
			{
				return true;
			}
			else
			{
				if (Math.sqrt(_player.getDistanceSq(_committedOwner)) > 200)
				{
					final var loc = Location.findPointToStay(_committedOwner.getLocation(), 100, false);
					if (loc != null)
					{
						_player.getAI().setIntention(CtrlIntention.MOVING, loc, 0);
						_player.getFarmSystem().addWaitTime(System.currentTimeMillis() + 2000L);
					}
					return false;
				}
			}
		}
		else
		{
			if (isLeaderAssist || isPartySupport)
			{
				return true;
			}
			
			final var target = _player.getFarmSystem().getAroundNpc(creature -> GeoEngine.getInstance().canSeeTarget(_player, creature) && !creature.isDead());
			if (target == null)
			{
				_player.getFarmSystem().checkEmptyTime();
				return false;
			}
			_committedTarget = target;
			_player.setTarget(target);
			_player.getFarmSystem().clearEmptyTime();
			return true;
		}
		return false;
    }
    
	private void useMagicSkill(Skill skill, boolean forSelf, boolean isAttack)
	{
		if (skill == null)
		{
			return;
		}
		
		if (skill.isToggle() && _player.isMounted())
        {
            return;
        }

		if (_player.isOutOfControl())
        {
            return;
        }
		
		if (!forSelf && isAttack && !skill.isAura() && _committedTarget != null && _player.getDistance(_committedTarget) > skill.getCastRange())
		{
			if (!_player.isMoving())
			{
				_player.getAI().setIntention(CtrlIntention.MOVING, _committedTarget.getLocation(), 0);
			}
			return;
		}
		_player.getFarmSystem().tryUseMagic(skill, forSelf);
    }
}