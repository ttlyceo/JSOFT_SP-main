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
package l2e.gameserver.ai.guard;

import static l2e.gameserver.ai.model.CtrlIntention.ACTIVE;
import static l2e.gameserver.ai.model.CtrlIntention.ATTACK;
import static l2e.gameserver.ai.model.CtrlIntention.IDLE;

import java.util.concurrent.Future;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DefenderInstance;
import l2e.gameserver.model.actor.instance.FortCommanderInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.taskmanager.AiTaskManager;

public class FortGuardAI extends CharacterAI implements Runnable
{
	private Future<?> _aiTask;
	private long _attackTimeout;
	private int _globalAggro;
	private boolean _thinking;
	private final int _attackRange;

	protected final Skill[] _damSkills, _debuffSkills, _healSkills, _buffSkills, _stunSkills;
	
	public FortGuardAI(Creature accessor)
	{
		super(accessor);
		
		_attackTimeout = Long.MAX_VALUE;
		_globalAggro = -10;
		_attackRange = ((Attackable) _actor).getPhysicalAttackRange();

		_damSkills = ((Npc) _actor).getTemplate().getDamageSkills();
		_debuffSkills = ((Npc) _actor).getTemplate().getDebuffSkills();
		_buffSkills = ((Npc) _actor).getTemplate().getBuffSkills();
		_stunSkills = ((Npc) _actor).getTemplate().getStunSkills();
		_healSkills = ((Npc) _actor).getTemplate().getHealSkills();
	}

	@Override
	public void run()
	{
		onEvtThink();
	}

	private boolean checkAggression(Creature target)
	{
		if ((target == null) || target.isAlikeDead())
		{
			return false;
		}
		
		final var player = target.getActingPlayer();
		if (player == null || (player.getClan() != null && (player.getClan().getFortId() == ((Npc) _actor).getFort().getId())))
		{
			return false;
		}
		
		if (target.isSummon() && _actor.isInsideRadius(player, 1000, true, false))
		{
			target = player;
		}

		if (target instanceof Playable)
		{
			if (((Playable) target).isSilentMoving() && Rnd.chance(90))
			{
				return false;
			}
		}
		return (_actor.isAutoAttackable(target, false) && GeoEngine.getInstance().canSeeTarget(_actor, target));
	}

	@Override
	protected synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if ((intention == IDLE) || (intention == ACTIVE))
		{
			if (!_actor.isAlikeDead())
			{
				intention = _actor.isInActiveRegion() ? ACTIVE : IDLE;
			}

			if (intention == IDLE)
			{
				super.changeIntention(IDLE, null, null);
				stopAITask();
			}
			else
			{
				super.changeIntention(intention, arg0, arg1);
				startAITask();
			}
			return;
		}
		super.changeIntention(intention, arg0, arg1);
	}

	@Override
	protected void onIntentionAttack(Creature target, boolean shift)
	{
		_attackTimeout = System.currentTimeMillis() + 30000L;
		super.onIntentionAttack(target, shift);
	}

	private void thinkActive()
	{
		final var npc = (Attackable) _actor;

		int aggroRange = 0;
		if (npc.getFaction().isNone())
		{
			aggroRange = _attackRange;
		}
		else
		{
			aggroRange = npc.getFaction().getRange();
		}

		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}

		if (_globalAggro >= 0)
		{
			for (final var target : World.getInstance().getAroundPlayables(npc, aggroRange, 300))
			{
				if (target == null)
				{
					continue;
				}
				
				if (checkAggression(target))
				{
					final int hating = npc.getAggroList().getHating(target);
					if (hating == 0)
					{
						npc.addDamageHate(target, 0, 1);
					}
				}
			}

			Creature hated;
			if (_actor.isConfused())
			{
				hated = getAttackTarget();
			}
			else
			{
				hated = npc.getAggroList().getMostHated();
			}

			if (hated != null)
			{
				final var aggro = npc.getAggroList().getHating(hated);

				if ((aggro + _globalAggro) > 0)
				{
					if (!_actor.isRunning())
					{
						_actor.setRunning();
					}
					setIntention(CtrlIntention.ATTACK, hated, null);
				}
				return;
			}

		}
		
		if (_actor instanceof DefenderInstance)
		{
			((DefenderInstance) _actor).returnHome();
		}
		else
		{
			((FortCommanderInstance) _actor).returnHome();
		}
	}

	private void thinkAttack()
	{
		if (_attackTimeout < System.currentTimeMillis())
		{
			if (_actor.isRunning())
			{
				_actor.setWalking();
				_attackTimeout = System.currentTimeMillis() + 30000L;
			}
		}

		final var attackTarget = getAttackTarget();
		if ((attackTarget == null) || attackTarget.isAlikeDead() || (_attackTimeout < System.currentTimeMillis()))
		{
			if (attackTarget != null)
			{
				final var npc = (Attackable) _actor;
				npc.getAggroList().stopHating(attackTarget);
			}
			_attackTimeout = Long.MAX_VALUE;
			setAttackTarget(null);
			setIntention(ACTIVE, null, null);
			_actor.setWalking();
			return;
		}
		factionNotifyAndSupport();
		attackPrepare();
	}

	private final void factionNotifyAndSupport()
	{
		final var target = getAttackTarget();
		if ((((Npc) _actor).getFaction().isNone()) || (target == null))
		{
			return;
		}

		if (target.isInvul())
		{
			return;
		}
		
		final var faction_id = ((Npc) _actor).getFaction().getName();
		for (final var cha : World.getInstance().getAroundCharacters(_actor, 1000, 200))
		{
			if (cha == null)
			{
				continue;
			}

			if (!(cha instanceof Npc))
			{
				if (_healSkills.length != 0 && (cha instanceof Player) && ((Npc) _actor).getFort().getSiege().checkIsDefender(((Player) cha).getClan()))
				{
					if (!_actor.isAttackingDisabled() && (cha.getCurrentHp() < (cha.getMaxHp() * 0.6)) && (_actor.getCurrentHp() > (_actor.getMaxHp() / 2)) && (_actor.getCurrentMp() > (_actor.getMaxMp() / 2)) && cha.isInCombat())
					{
						for (final var sk : _healSkills)
						{
							if (_actor.getCurrentMp() < sk.getMpConsume())
							{
								continue;
							}
							if (_actor.isSkillDisabled(sk))
							{
								continue;
							}
							if (!Util.checkIfInRange(sk.getCastRange(), _actor, cha, true))
							{
								continue;
							}

							final var chance = 5;
							if (chance >= Rnd.get(100))
							{
								continue;
							}
							if (!GeoEngine.getInstance().canSeeTarget(_actor, cha))
							{
								break;
							}

							final var OldTarget = _actor.getTarget();
							_actor.setTarget(cha);
							clientStopMoving(null);
							_actor.doCast(sk);
							_actor.setTarget(OldTarget);
							return;
						}
					}
				}
				continue;
			}

			final var npc = (Npc) cha;
			if (!faction_id.equals(npc.getFaction().getName()))
			{
				continue;
			}
			
			if (npc.getAI() != null)
			{
				if (!npc.isDead() && (Math.abs(target.getZ() - npc.getZ()) < 600) && ((npc.getAI().getIntention() == CtrlIntention.IDLE) || (npc.getAI().getIntention() == CtrlIntention.ACTIVE)) && target.isInsideRadius(npc, 1500, true, false) && GeoEngine.getInstance().canSeeTarget(npc, target))
				{
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getAttackTarget(), 1);
					return;
				}

				if (_healSkills.length != 0 && !_actor.isAttackingDisabled() && (npc.getCurrentHp() < (npc.getMaxHp() * 0.6)) && (_actor.getCurrentHp() > (_actor.getMaxHp() / 2)) && (_actor.getCurrentMp() > (_actor.getMaxMp() / 2)) && npc.isInCombat())
				{
					for (final var sk : _healSkills)
					{
						if (_actor.getCurrentMp() < sk.getMpConsume())
						{
							continue;
						}
						if (_actor.isSkillDisabled(sk))
						{
							continue;
						}
						if (!Util.checkIfInRange(sk.getCastRange(), _actor, npc, true))
						{
							continue;
						}

						final var chance = 4;
						if (chance >= Rnd.get(100))
						{
							continue;
						}
						if (!GeoEngine.getInstance().canSeeTarget(_actor, npc))
						{
							break;
						}

						final var OldTarget = _actor.getTarget();
						_actor.setTarget(npc);
						clientStopMoving(null);
						_actor.doCast(sk);
						_actor.setTarget(OldTarget);
						return;
					}
				}
			}
		}
	}

	private void attackPrepare()
	{
		var dist_2 = 0.;
		var range = 0;
		DefenderInstance sGuard;
		if (_actor instanceof FortCommanderInstance)
		{
			sGuard = (FortCommanderInstance) _actor;
		}
		else
		{
			sGuard = (DefenderInstance) _actor;
		}
		var attackTarget = getAttackTarget();

		try
		{
			_actor.setTarget(attackTarget);
			final var dist = Math.sqrt(_actor.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY()));
			dist_2 = (int) dist - _actor.getColRadius();
			range = _attackRange;
			if (attackTarget.isMoving())
			{
				range += 50;
			}
		}
		catch (final NullPointerException e)
		{
			_actor.setTarget(null);
			setIntention(IDLE, null, null);
			return;
		}

		if ((attackTarget instanceof Player) && sGuard.getFort().getSiege().checkIsDefender(((Player) attackTarget).getClan()))
		{
			sGuard.getAggroList().stopHating(attackTarget);
			_actor.setTarget(null);
			setIntention(IDLE, null, null);
			return;
		}

		if (!GeoEngine.getInstance().canSeeTarget(_actor, attackTarget))
		{
			sGuard.getAggroList().stopHating(attackTarget);
			_actor.setTarget(null);
			setIntention(IDLE, null, null);
			return;
		}

		if (!_actor.isMuted() && (dist_2 > range))
		{
			if (checkSkills(dist_2))
			{
				return;
			}

			if (!(_actor.isAttackingNow()) && (_actor.getRunSpeed() == 0) && (World.getInstance().getAroundCharacters(_actor).contains(attackTarget)))
			{
				_actor.setTarget(null);
				setIntention(IDLE, null, null);
			}
			else
			{
				final var dx = _actor.getX() - attackTarget.getX();
				final var dy = _actor.getY() - attackTarget.getY();
				final var dz = _actor.getZ() - attackTarget.getZ();
				final var homeX = attackTarget.getX() - sGuard.getSpawn().getX();
				final var homeY = attackTarget.getY() - sGuard.getSpawn().getY();

				if ((((dx * dx) + (dy * dy)) > 10000) && (((homeX * homeX) + (homeY * homeY)) > 3240000) && (World.getInstance().getAroundCharacters(_actor).contains(attackTarget)))
				{
					_actor.setTarget(null);
					setIntention(IDLE, null, null);
				}
				else
				{
					if ((dz * dz) < (170 * 170))
					{
						final var template = (NpcTemplate) _actor.getTemplate();
						if (template.getAI().equalsIgnoreCase("Mystic") || template.getAI().equalsIgnoreCase("Priest"))
						{
							range -= 50;
						}

						if (attackTarget.isMoving())
						{
							moveTo(attackTarget.getLocation(), ((range - 70) / 2));
						}
						else
						{
							moveTo(attackTarget.getLocation(), (range / 2));
						}
					}
				}
			}
			return;

		}
		else if (_actor.isMuted() && (dist_2 > range))
		{
			final var dz = _actor.getZ() - attackTarget.getZ();
			if ((dz * dz) < (170 * 170))
			{
				final var template = (NpcTemplate) _actor.getTemplate();
				if (template.getAI().equalsIgnoreCase("Mystic") || template.getAI().equalsIgnoreCase("Priest"))
				{
					range -= 50;
				}

				if (attackTarget.isMoving())
				{
					moveTo(attackTarget.getLocation(), ((range - 70) / 2));
				}
				else
				{
					moveTo(attackTarget.getLocation(), (range / 2));
				}
			}
			return;
		}
		else if (dist_2 <= range)
		{
			Creature hated = null;
			if (_actor.isConfused())
			{
				hated = attackTarget;
			}
			else
			{
				hated = ((Attackable) _actor).getAggroList().getMostHated();
			}

			if (hated == null)
			{
				setIntention(ACTIVE, null, null);
				return;
			}
			if (hated != attackTarget)
			{
				attackTarget = hated;
			}

			_attackTimeout = System.currentTimeMillis() + 30000L;

			if (!_actor.isMuted() && (Rnd.nextInt(100) <= 5))
			{
				if (checkSkills(dist_2))
				{
					return;
				}
			}

			if (!((NpcTemplate) _actor.getTemplate()).getAI().equalsIgnoreCase("Priest"))
			{
				_actor.doAttack(attackTarget);
			}
		}
	}
	
	protected boolean checkSkills(double distance)
	{
		if (_debuffSkills.length != 0 && Rnd.chance(20))
		{
			final var skill = _debuffSkills[Rnd.get(_debuffSkills.length)];
			final var castRange = skill.getCastRange();
			final var mpConsume = _actor.getStat().getMpConsume(skill)[0];
			if ((distance <= castRange) && !_actor.isSkillDisabled(skill) && (_actor.getCurrentMp() >= mpConsume) && !skill.isPassive())
			{
				if (getAttackTarget().getFirstEffect(skill) == null)
				{
					clientStopMoving(null);
					_actor.setTarget(_actor.getTarget());
					_actor.doCast(skill);
					return true;
				}
			}
		}
		
		if (_damSkills.length != 0)
		{
			final var skill = _damSkills[Rnd.get(_damSkills.length)];
			final var castRange = skill.getCastRange();
			final var mpConsume = _actor.getStat().getMpConsume(skill)[0];
			if ((distance <= castRange) && !_actor.isSkillDisabled(skill) && (_actor.getCurrentMp() >= mpConsume) && !skill.isPassive())
			{
				clientStopMoving(null);
				_actor.setTarget(_actor.getTarget());
				_actor.doCast(skill);
				return true;
			}
		}

		if (_stunSkills.length != 0 && Rnd.chance(20))
		{
			final var skill = _stunSkills[Rnd.get(_stunSkills.length)];
			final var castRange = (int) (getAttackTarget().getColRadius() + _actor.getColRadius());
			final var mpConsume = _actor.getStat().getMpConsume(skill)[0];
			if ((distance <= castRange) && !_actor.isSkillDisabled(skill) && (_actor.getCurrentMp() >= mpConsume) && !skill.isPassive())
			{
				clientStopMoving(null);
				_actor.setTarget(_actor.getTarget());
				_actor.doCast(skill);
				return true;
			}
		}
		
		if (_buffSkills.length != 0)
		{
			final var OldTarget = _actor.getTarget();
			for (final var skill : _buffSkills)
			{
				var useSkillSelf = true;
				final Effect[] effects = _actor.getAllEffects();
				for (int i = 0; (effects != null) && (i < effects.length); i++)
				{
					final var effect = effects[i];
					if (effect.getSkill() == skill)
					{
						useSkillSelf = false;
					}
				}
				
				if (useSkillSelf)
				{
					_actor.setTarget(_actor);
					clientStopMoving(null);
					_actor.doCast(skill);
					_actor.setTarget(OldTarget);
					return true;
				}
			}
		}

		if (_healSkills.length != 0)
		{
			if (_actor.getCurrentHp() < (_actor.getMaxHp() / 2))
			{
				final var skill = _healSkills[Rnd.get(_healSkills.length)];
				final var mpConsume = _actor.getStat().getMpConsume(skill)[0];
				if (!_actor.isSkillDisabled(skill) && (_actor.getCurrentMp() >= mpConsume) && !skill.isPassive())
				{
					final var OldTarget = _actor.getTarget();

					_actor.setTarget(_actor);
					clientStopMoving(null);
					_actor.doCast(skill);
					_actor.setTarget(OldTarget);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void onEvtThink()
	{
		if (_thinking || _actor == null || _actor.isActionsDisabled() || _actor.isAfraid() || _actor.isAllSkillsDisabled())
		{
			return;
		}

		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case ACTIVE :
					thinkActive();
					break;
				case ATTACK :
					thinkAttack();
					break;
			}
		}
		finally
		{
			_thinking = false;
		}
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		_attackTimeout = System.currentTimeMillis() + 30000L;
		
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}

		((Attackable) _actor).addDamageHate(attacker, 0, 1);

		if (!_actor.isRunning())
		{
			_actor.setRunning();
		}

		if (getIntention() != ATTACK)
		{
			setIntention(CtrlIntention.ATTACK, attacker, null);
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		if (_actor == null)
		{
			return;
		}
		final var me = (Attackable) _actor;

		if (target != null)
		{
			me.addDamageHate(target, 0, aggro);

			aggro = me.getAggroList().getHating(target);

			if (aggro <= 0)
			{
				if (me.getAggroList().getMostHated() == null)
				{
					_globalAggro = -25;
					me.clearAggroList(false);
					setIntention(IDLE, null, null);
				}
				return;
			}

			if (getIntention() != CtrlIntention.ATTACK)
			{
				if (!_actor.isRunning())
				{
					_actor.setRunning();
				}

				DefenderInstance sGuard;
				if (_actor instanceof FortCommanderInstance)
				{
					sGuard = (FortCommanderInstance) _actor;
				}
				else
				{
					sGuard = (DefenderInstance) _actor;
				}
				final var homeX = target.getX() - sGuard.getSpawn().getX();
				final var homeY = target.getY() - sGuard.getSpawn().getY();

				if (((homeX * homeX) + (homeY * homeY)) < 3240000)
				{
					setIntention(CtrlIntention.ATTACK, target, null);
				}
			}
		}
		else
		{
			if (aggro >= 0)
			{
				return;
			}

			final var mostHated = me.getAggroList().getMostHated();
			if (mostHated == null)
			{
				_globalAggro = -25;
				return;
			}

			for (final var aggroed : me.getAggroList().getCharMap().keySet())
			{
				me.addDamageHate(aggroed, 0, aggro);
			}

			aggro = me.getAggroList().getHating(mostHated);
			if (aggro <= 0)
			{
				_globalAggro = -25;
				me.clearAggroList(false);
				setIntention(IDLE, null, null);
			}
		}
	}
	
	@Override
	public synchronized void startAITask()
	{
		if (_aiTask == null)
		{
			_aiTask = Config.AI_TASK_MANAGER_COUNT > 0 ? AiTaskManager.getInstance().scheduleAtFixedRate(this, 0L, Config.NPC_AI_TIME_TASK) : ThreadPoolManager.getInstance().scheduleAtFixedRate(this, 0L, Config.NPC_AI_TIME_TASK);
		}
	}

	@Override
	public void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		_actor.detachAI();
		super.stopAITask();
	}
	
	@Override
	public void enableAI()
	{
		if (_actor.getAI().getIntention() == CtrlIntention.IDLE)
		{
			changeIntention(CtrlIntention.ACTIVE, null, null);
		}
	}
}