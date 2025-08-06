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
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.service.autofarm.FarmSettings;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.network.SystemMessageId;

/**
 * Created by LordWinter
 */
public class AutoSummonFarmTask extends LoggerObject implements Runnable
{
	private final Player _player;
	private Attackable _committedTarget = null;
	private Player _committedOwner = null;
	private Summon _committedSummon = null;
	private long _extraDelay = 0L;
	private long _extraSummonDelay = 0L;
	private final int _farmInterval;
    
	public AutoSummonFarmTask(Player player)
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
		
		if (!_player.hasSummon())
		{
			_committedSummon = null;
		}
		tryAttack(selectRandomTarget(), _player.getFarmSystem().getSummonFarmType() > 0);
    }

	private void tryAttack(boolean selected, boolean isMagic)
	{
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
				if (_player.getFarmSystem().isAbortTarget())
				{
					_committedTarget = null;
					_player.getFarmSystem().setAbortTarget(false);
					_player.setTarget(null);
				}
				
				if (selected && _committedTarget != null)
				{
					physicalSummonAttack(isMagic);
				}
				
				if (!tryUseSpell(selected))
				{
					if (selected && _committedTarget != null)
					{
						physicalSummonAttack(isMagic);
					}
				}
				
				if (selected && _committedSummon != null && _player.getFarmSystem().isUseSummonSkills())
				{
					tryUseSummonSpell(selected);
				}
			}
		}
		finally
		{
			_player.getFarmSystem()._farmTask = ThreadPoolManager.getInstance().schedule(this, _farmInterval);
		}
    }
	
	private boolean tryUseSpell(boolean isMagic)
	{
		if (_player.isDead())
		{
			return false;
		}
		
		final boolean canAttack = selectRandomTarget();
		if (canAttack && _committedTarget != null)
		{
			final var chanceSkill = _player.getFarmSystem().nextChanceSkill(_committedTarget, _extraDelay);
			if (chanceSkill != null)
			{
				useMagicSkill(chanceSkill, false, true);
				return true;
			}
		}
		
		final var lowLifeSkill = _player.getFarmSystem().nextHealSkill(_committedTarget, _committedSummon);
		if (lowLifeSkill != null)
		{
			useMagicSkill(lowLifeSkill, !lowLifeSkill.isOffensive(), lowLifeSkill.isOffensive());
			return true;
		}
		
		final var selfSkills = _player.getFarmSystem().nextSelfSkill(null);
		if (selfSkills != null)
		{
			useMagicSkill(selfSkills, true, false);
			return true;
		}
		
		if (canAttack && _committedTarget != null)
		{
			final var attackSkill = _player.getFarmSystem().nextAttackSkill(_committedTarget, _extraDelay);
			if (attackSkill != null)
			{
				useMagicSkill(attackSkill, false, true);
				return true;
			}
		}
		return false;
	}
	
	private void tryUseSummonSpell(boolean selected)
	{
		if (_committedSummon.isDead())
		{
			return;
		}
		
		final var lowLifeSkill = _player.getFarmSystem().nextSummonHealSkill(_committedTarget, _committedSummon, _player);
		if (lowLifeSkill != null)
		{
			useSummonMagicSkill(lowLifeSkill, lowLifeSkill.getTargetType() == TargetType.SELF, lowLifeSkill.isOffensive());
			return;
		}
		
		final var selfSkills = _player.getFarmSystem().nextSummonSelfSkill(_committedSummon, _player);
		if (selfSkills != null)
		{
			useSummonMagicSkill(selfSkills, selfSkills.getTargetType() == TargetType.SELF, false);
			return;
		}
		
		if (selected && _player.getFarmSystem().isAllowSummonPhysAttack())
		{
			final var attackSkill = _player.getFarmSystem().nextSummonAttackSkill(_committedTarget, _committedSummon, _extraSummonDelay);
			if (attackSkill != null)
			{
				useSummonMagicSkill(attackSkill, false, true);
				return;
			}
		}
	}

	private boolean canUseSweep()
	{
		final var sweeper = _player.getKnownSkill(42);
		final var masSweeper = _player.getKnownSkill(444);
		if (sweeper == null && masSweeper == null)
		{
			return false;
		}
		
		if (canBeSweepedByMe())
		{
			useMagicSkill(masSweeper != null ? masSweeper : sweeper, false, false);
			return true;
        }
		return false;
    }
	
	private boolean canBeSweepedByMe()
	{
		if (_committedTarget == null)
		{
			return false;
		}
		return _committedTarget.isVisible() && _committedTarget.isDead() && _committedTarget.isSweepActive() && !_committedTarget.isOldCorpse(_player, (Config.MAX_SWEEPER_TIME * 1000), false) && _committedTarget.checkSpoilOwner(_player, false);
    }

	private void physicalSummonAttack(boolean isMagic)
	{
		if (_committedTarget.isAutoAttackable(_player, false) && !_committedTarget.isAlikeDead())
		{
			if (_committedSummon != null && _player.getFarmSystem().isAllowSummonPhysAttack() && validateSummon(_committedSummon, _player.hasPet()) && !_committedSummon.isDead())
			{
				_committedSummon.getAI().setIntention(CtrlIntention.ATTACK, _committedTarget, false);
				if (!isMagic)
				{
					_player.getAI().setIntention(CtrlIntention.ATTACK, _committedTarget, false);
				}
			}
			else
			{
				if (_committedSummon != null && _player.getFarmSystem().isAllowSummonPhysAttack() && validateSummon(_committedSummon, _player.hasPet()) && !_committedSummon.isDead())
				{
					_committedSummon.getAI().setIntention(CtrlIntention.MOVING, _committedTarget.getLocation(), 0);
				}
				_player.getAI().setIntention(CtrlIntention.MOVING, _committedTarget.getLocation(), 0);
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
		
		if (_committedTarget != null)
		{
			if (_committedTarget.isDead() && _committedTarget.isSpoil() && canUseSweep())
			{
				return false;
			}
			
			boolean activePlayer = !_committedTarget.isDead() && _committedTarget.isVisible();
			if (_player.isMovementDisabled() && _player.getDistanceSq(_committedTarget) > _player.getPhysicalAttackRange())
			{
				activePlayer = false;
			}
			
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
						if (_committedSummon != null)
						{
							_committedSummon.getAI().setIntention(CtrlIntention.MOVING, _player.getFarmSystem().getKeepLocation(), 0);
						}
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
		
		if (_committedSummon == null)
		{
			_committedSummon = _player.hasSummon() ? _player.getSummon() : null;
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
					final var loc = Location.findPointToStay(_committedOwner.getLocation(), 100, false);
					if (loc != null)
					{
						_player.getAI().setIntention(CtrlIntention.MOVING, loc, 0);
						if (_committedSummon != null)
						{
							_committedSummon.getAI().setIntention(CtrlIntention.MOVING, loc, 0);
						}
						_player.getFarmSystem().addWaitTime(System.currentTimeMillis() + 2000L);
					}
					return false;
				}
			}
		}
		else
		{
			if (_player.getFarmSystem().isNeedToReturn())
			{
				if (_committedSummon != null)
				{
					_committedSummon.getAI().setIntention(CtrlIntention.MOVING, _player.getFarmSystem().getKeepLocation(), 0);
				}
				return false;
			}
			
			final var target = _player.getFarmSystem().getAroundNpc(creature -> GeoEngine.getInstance().canSeeTarget(_player, creature) && !creature.isDead());
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
				if (_committedSummon != null && _player.getFarmSystem().isAllowSummonPhysAttack() && validateSummon(_committedSummon, _player.hasPet()) && !_committedSummon.isDead())
				{
					_committedSummon.getAI().setIntention(CtrlIntention.ATTACK, _committedTarget, false);
				}
				
				if (_player.getFarmSystem().getSummonFarmType() == 0)
				{
					_player.getAI().setIntention(CtrlIntention.ATTACK, _committedTarget, false);
				}
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
		
		if (_player.getFarmSystem().isExtraDelaySkill())
		{
			_extraDelay = System.currentTimeMillis() + FarmSettings.SKILLS_EXTRA_DELAY;
		}
		_player.getFarmSystem().tryUseMagic(skill, forSelf);
    }
	
	private void useSummonMagicSkill(Skill skill, boolean forSelf, boolean isAttack)
	{
		if (skill == null || _committedSummon == null)
		{
			return;
		}
		
		if (_committedSummon.isOutOfControl())
		{
			return;
		}
		
		if (!forSelf && isAttack && !skill.isAura() && _committedTarget != null && _committedSummon.getDistance(_committedTarget) > skill.getCastRange())
		{
			if (!_committedSummon.isMoving())
			{
				_committedSummon.getAI().setIntention(CtrlIntention.MOVING, _committedTarget.getLocation(), 0);
			}
			return;
		}
		
		if (_player.getFarmSystem().isExtraSummonDelaySkill())
		{
			_extraSummonDelay = System.currentTimeMillis() + FarmSettings.SKILLS_EXTRA_DELAY;
		}
		trySummonUseMagic(skill, forSelf);
	}
	
	public void trySummonUseMagic(Skill skill, boolean forceOnSelf)
	{
		if (forceOnSelf)
		{
			final GameObject oldTarget = _committedSummon.getTarget();
			_committedSummon.setTarget(_committedSummon);
			_committedSummon.useMagic(skill, false, false, false);
			_committedSummon.setTarget(oldTarget);
			return;
		}
		_committedSummon.useMagic(skill, false, false, false);
	}
	
	private boolean validateSummon(Summon summon, boolean checkPet)
	{
		if ((summon != null) && ((checkPet && summon.isPet()) || summon.isServitor()))
		{
			if (summon.isBetrayed())
			{
				_player.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
				return false;
			}
			return true;
		}
		
		if (checkPet)
		{
			_player.sendPacket(SystemMessageId.DONT_HAVE_PET);
		}
		else
		{
			_player.sendPacket(SystemMessageId.DONT_HAVE_SERVITOR);
		}
		return false;
	}
}