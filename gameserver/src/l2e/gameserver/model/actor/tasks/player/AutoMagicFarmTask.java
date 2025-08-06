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

import l2e.commons.log.LoggerObject;
import l2e.commons.util.PositionUtils;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.service.autofarm.FarmSettings;
import l2e.gameserver.model.skills.Skill;

/**
 * Created by LordWinter
 */
public class AutoMagicFarmTask extends LoggerObject implements Runnable
{
	private final Player _player;
	private Attackable _committedTarget = null;
	private Player _committedOwner = null;
	private final int _farmInterval;
    
	public AutoMagicFarmTask(Player player)
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
		tryUseSpell();
    }

	private void tryUseSpell()
	{
		if (_player.getFarmSystem().isAbortTarget())
		{
			_committedTarget = null;
			_player.getFarmSystem().setAbortTarget(false);
		}
		
		if (_player.isDead())
		{
			_player.getFarmSystem().checkEmptyTime();
			_player.getFarmSystem()._farmTask = ThreadPoolManager.getInstance().schedule(this, _farmInterval);
			return;
		}

		try
		{
			if (!_player.getFarmSystem().isInWaitStatus(_committedTarget))
			{
				final boolean canAttack = selectRandomTarget();
				if (canAttack)
				{
					final Skill chanceSkill = _player.getFarmSystem().nextChanceSkill(_committedTarget, 0);
					if (chanceSkill != null)
					{
						useMagicSkill(chanceSkill, false, true);
						return;
					}
				}
				
				final Skill lowLifeSkill = _player.getFarmSystem().nextHealSkill(_committedTarget, null);
				if (lowLifeSkill != null)
				{
					useMagicSkill(lowLifeSkill, !lowLifeSkill.isOffensive(), lowLifeSkill.isOffensive());
					return;
				}
				
				final Skill selfSkills = _player.getFarmSystem().nextSelfSkill(null);
				if (selfSkills != null)
				{
					useMagicSkill(selfSkills, true, false);
					return;
				}
				
				if (canAttack)
				{
					final Skill attackSkill = _player.getFarmSystem().nextAttackSkill(_committedTarget, 0);
					if (attackSkill != null)
					{
						useMagicSkill(attackSkill, false, true);
						return;
					}
				}
			}
		}
		finally
		{
			_player.getFarmSystem()._farmTask = ThreadPoolManager.getInstance().schedule(this, _farmInterval);
		}
    }
	
	private boolean selectRandomTarget()
	{
		if (_committedTarget != null)
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
					if (_player.getFarmSystem().isNeedToReturn())
					{
						return false;
					}
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
		
		if (_player.getFarmSystem().isLeaderAssist())
		{
			if (_player.getParty() == null)
			{
				_committedOwner = null;
				_player.getFarmSystem().setLeaderAssist(false, false);
			}
			else
			{
				_committedOwner = _player.getParty().getLeader();
			}
		}
		
		if (_committedOwner != null)
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
					final Location loc = Location.findPointToStay(_committedOwner.getLocation(), 200, false);
					if (loc != null)
					{
						_player.getAI().setIntention(CtrlIntention.MOVING, loc, 0);
					}
					_player.getFarmSystem().addWaitTime(System.currentTimeMillis() + 2000L);
					return false;
				}
			}
		}
		else
		{
			if (_player.getFarmSystem().isNeedToReturn())
			{
				return false;
			}
			
			final Attackable target = _player.getFarmSystem().getAroundNpc(creature -> GeoEngine.getInstance().canSeeTarget(_player, creature) && !creature.isDead());
			if (target == null)
			{
				_player.getFarmSystem().checkEmptyTime();
				return false;
			}
			_committedTarget = target;
			_player.setTarget(target);
			if (_player.isInPathFinding())
			{
				_player.stopMove(null);
			}
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
		
		if (_player.getFarmSystem().isRunTargetCloseUp() && !forSelf)
		{
			if (_committedTarget != null && (Math.sqrt(_player.getDistanceSq(_committedTarget)) < FarmSettings.RUN_CLOSE_UP_DISTANCE))
			{
				final double angle = Math.toRadians(PositionUtils.calculateAngleFrom(_committedTarget, _player));
				final int oldX = _player.getX();
				final int oldY = _player.getY();
				final int x = oldX + (int) (500 * Math.cos(angle));
				final int y = oldY + (int) (500 * Math.sin(angle));
				final Location loc = Location.findPointToStay(new Location(x, y, _player.getZ()), 100, false);
				if (loc != null)
				{
					_player.getAI().setIntention(CtrlIntention.MOVING, loc, 0);
					_player.getFarmSystem().addWaitTime(System.currentTimeMillis() + FarmSettings.RUN_CLOSE_UP_DELAY);
					return;
				}
			}
		}
		_player.getFarmSystem().tryUseMagic(skill, forSelf);
    }
}