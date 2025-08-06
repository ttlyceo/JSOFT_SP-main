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
package l2e.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.handler.skillhandlers.SkillHandler;
import l2e.gameserver.instancemanager.DuelManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.l2skills.SkillDrain;
import l2e.gameserver.model.stats.BaseStats;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.taskmanager.AttackStanceTaskManager;

public final class CubicInstance implements IIdentifiable
{
	protected static final Logger _log = LoggerFactory.getLogger(CubicInstance.class);

	public static final int STORM_CUBIC = 1;
	public static final int VAMPIRIC_CUBIC = 2;
	public static final int LIFE_CUBIC = 3;
	public static final int VIPER_CUBIC = 4;
	public static final int POLTERGEIST_CUBIC = 5;
	public static final int BINDING_CUBIC = 6;
	public static final int AQUA_CUBIC = 7;
	public static final int SPARK_CUBIC = 8;
	public static final int ATTRACT_CUBIC = 9;
	public static final int SMART_CUBIC_EVATEMPLAR = 10;
	public static final int SMART_CUBIC_SHILLIENTEMPLAR = 11;
	public static final int SMART_CUBIC_ARCANALORD = 12;
	public static final int SMART_CUBIC_ELEMENTALMASTER = 13;
	public static final int SMART_CUBIC_SPECTRALMASTER = 14;
	
	public static final int MAX_MAGIC_RANGE = 900;

	public static final int SKILL_CUBIC_HEAL = 4051;
	public static final int SKILL_CUBIC_CURE = 5579;

	protected Player _owner;
	protected Creature _target;

	protected int _id;
	protected int _cubicPower;
	protected int _cubicDuration;
	protected int _cubicDelay;
	protected int _cubicSkillChance;
	protected int _cubicMaxCount;
	protected int _currentcount;
	protected boolean _active;
	private final boolean _givenByOther;

	protected List<Skill> _skills = new ArrayList<>();

	private Future<?> _disappearTask;
	private Future<?> _actionTask;

	public CubicInstance(Player owner, int id, int level, int cubicPower, int cubicDelay, int cubicSkillChance, int cubicMaxCount, int cubicDuration, boolean givenByOther)
	{
		_owner = owner;
		_id = id;
		_cubicPower = cubicPower;
		_cubicDuration = cubicDuration * 1000;
		_cubicDelay = cubicDelay * 1000;
		_cubicSkillChance = cubicSkillChance;
		_cubicMaxCount = cubicMaxCount;
		_currentcount = 0;
		_active = false;
		_givenByOther = givenByOther;

		switch (_id)
		{
			case STORM_CUBIC :
				_skills.add(SkillsParser.getInstance().getInfo(4049, level));
				break;
			case VAMPIRIC_CUBIC :
				_skills.add(SkillsParser.getInstance().getInfo(4050, level));
				break;
			case LIFE_CUBIC :
				_skills.add(SkillsParser.getInstance().getInfo(4051, level));
				doAction();
				break;
			case VIPER_CUBIC :
				_skills.add(SkillsParser.getInstance().getInfo(4052, level));
				break;
			case POLTERGEIST_CUBIC :
				_skills.add(SkillsParser.getInstance().getInfo(4053, level));
				_skills.add(SkillsParser.getInstance().getInfo(4054, level));
				_skills.add(SkillsParser.getInstance().getInfo(4055, level));
				break;
			case BINDING_CUBIC :
				_skills.add(SkillsParser.getInstance().getInfo(4164, level));
				break;
			case AQUA_CUBIC :
				_skills.add(SkillsParser.getInstance().getInfo(4165, level));
				break;
			case SPARK_CUBIC :
				_skills.add(SkillsParser.getInstance().getInfo(4166, level));
				break;
			case ATTRACT_CUBIC :
				_skills.add(SkillsParser.getInstance().getInfo(5115, level));
				_skills.add(SkillsParser.getInstance().getInfo(5116, level));
				break;
			case SMART_CUBIC_ARCANALORD :
				_skills.add(SkillsParser.getInstance().getInfo(4051, 7));
				_skills.add(SkillsParser.getInstance().getInfo(4165, 9));
				break;
			case SMART_CUBIC_ELEMENTALMASTER :
				_skills.add(SkillsParser.getInstance().getInfo(4049, 8));
				_skills.add(SkillsParser.getInstance().getInfo(4166, 9));
				break;
			case SMART_CUBIC_SPECTRALMASTER :
				_skills.add(SkillsParser.getInstance().getInfo(4049, 8));
				_skills.add(SkillsParser.getInstance().getInfo(4052, 6));
				break;
			case SMART_CUBIC_EVATEMPLAR :
				_skills.add(SkillsParser.getInstance().getInfo(4053, 8));
				_skills.add(SkillsParser.getInstance().getInfo(4165, 9));
				break;
			case SMART_CUBIC_SHILLIENTEMPLAR :
				_skills.add(SkillsParser.getInstance().getInfo(4049, 8));
				_skills.add(SkillsParser.getInstance().getInfo(5115, 4));
				break;
		}
		_disappearTask = ThreadPoolManager.getInstance().schedule(new Disappear(this), _cubicDuration);
	}

	public synchronized void doAction()
	{
		if (_active)
		{
			return;
		}
		_active = true;
		switch (_id)
		{
			case AQUA_CUBIC :
			case BINDING_CUBIC :
			case SPARK_CUBIC :
			case STORM_CUBIC :
			case POLTERGEIST_CUBIC :
			case VAMPIRIC_CUBIC :
			case VIPER_CUBIC :
			case ATTRACT_CUBIC :
			case SMART_CUBIC_ARCANALORD :
			case SMART_CUBIC_ELEMENTALMASTER :
			case SMART_CUBIC_SPECTRALMASTER :
			case SMART_CUBIC_EVATEMPLAR :
			case SMART_CUBIC_SHILLIENTEMPLAR :
				_actionTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Action(this, _cubicSkillChance), 0, _cubicDelay);
				break;
			case LIFE_CUBIC :
				_actionTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new Heal(this), 0, _cubicDelay);
				break;
		}
	}

	@Override
	public int getId()
	{
		return _id;
	}

	public Player getOwner()
	{
		return _owner;
	}

	public final int getMCriticalHit(Creature target, Skill skill)
	{
		return (int) (BaseStats.WIT.calcBonus(_owner) * 10);
	}

	public int getCubicPower()
	{
		return _cubicPower;
	}

	public void stopAction()
	{
		_target = null;
		if (_actionTask != null)
		{
			_actionTask.cancel(true);
			_actionTask = null;
		}
		_active = false;
	}

	public void cancelDisappear()
	{
		if (_disappearTask != null)
		{
			_disappearTask.cancel(true);
			_disappearTask = null;
		}
	}

	public void getCubicTarget()
	{
		try
		{
			_target = null;
			final GameObject ownerTarget = _owner.getTarget();
			if (ownerTarget == null)
			{
				return;
			}

			if (_owner.isInFightEvent())
			{
				if (ownerTarget.getActingPlayer() != null)
				{
					for (final AbstractFightEvent e : _owner.getFightEvents())
					{
						if (e != null && e.canAttack((Creature) ownerTarget, _owner) && !ownerTarget.getActingPlayer().isDead())
						{
							_target = (Creature) ownerTarget;
						}
					}
				}
				return;
			}

			if (_owner.isInPartyTournament())
			{
				if (ownerTarget.getActingPlayer() != null)
				{
					var e = _owner.getPartyTournament();
					if (e != null && e.canAttack((Creature) ownerTarget, _owner) && !ownerTarget.getActingPlayer().isDead())
					{
						_target = (Creature) ownerTarget;
					}
				}
				return;
			}

			if (_owner.isInDuel())
			{
				final Player PlayerA = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerA();
				final Player PlayerB = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerB();

				if (DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
				{
					final Party partyA = PlayerA.getParty();
					final Party partyB = PlayerB.getParty();
					Party partyEnemy = null;

					if (partyA != null)
					{
						if (partyA.getMembers().contains(_owner))
						{
							if (partyB != null)
							{
								partyEnemy = partyB;
							}
							else
							{
								_target = PlayerB;
							}
						}
						else
						{
							partyEnemy = partyA;
						}
					}
					else
					{
						if (PlayerA == _owner)
						{
							if (partyB != null)
							{
								partyEnemy = partyB;
							}
							else
							{
								_target = PlayerB;
							}
						}
						else
						{
							_target = PlayerA;
						}
					}
					if ((_target == PlayerA) || (_target == PlayerB))
					{
						if (_target == ownerTarget)
						{
							return;
						}
					}
					if (partyEnemy != null)
					{
						if (partyEnemy.getMembers().contains(ownerTarget))
						{
							_target = (Creature) ownerTarget;
						}
						return;
					}
				}
				if ((PlayerA != _owner) && (ownerTarget == PlayerA))
				{
					_target = PlayerA;
					return;
				}
				if ((PlayerB != _owner) && (ownerTarget == PlayerB))
				{
					_target = PlayerB;
					return;
				}
				_target = null;
				return;
			}

			if (_owner.isInOlympiadMode())
			{
				if (_owner.isOlympiadStart())
				{
					if (ownerTarget instanceof Playable)
					{
						final Player targetPlayer = ownerTarget.getActingPlayer();
						if ((targetPlayer != null) && (targetPlayer.getOlympiadGameId() == _owner.getOlympiadGameId()) && (targetPlayer.getOlympiadSide() != _owner.getOlympiadSide()))
						{
							_target = (Creature) ownerTarget;
						}
					}
				}
				return;
			}

			if ((ownerTarget instanceof Creature) && (ownerTarget != _owner.getSummon()) && (ownerTarget != _owner))
			{
				if (ownerTarget instanceof Attackable)
				{
					if ((((Attackable) ownerTarget).getAggroList().get(_owner) != null) && !((Attackable) ownerTarget).isDead())
					{
						_target = (Creature) ownerTarget;
						return;
					}
					if (_owner.hasSummon())
					{
						if ((((Attackable) ownerTarget).getAggroList().get(_owner.getSummon()) != null) && !((Attackable) ownerTarget).isDead())
						{
							_target = (Creature) ownerTarget;
							return;
						}
					}
				}

				Player enemy = null;

				if (((_owner.getPvpFlag() > 0) && !_owner.isInsideZone(ZoneId.PEACE)) || _owner.isInsideZone(ZoneId.PVP))
				{
					if (!((Creature) ownerTarget).isDead())
					{
						enemy = ownerTarget.getActingPlayer();
					}

					if (enemy != null)
					{
						boolean targetIt = true;

						if (_owner.getParty() != null)
						{
							if (_owner.getParty().getMembers().contains(enemy))
							{
								targetIt = false;
							}
							else if (_owner.getParty().getCommandChannel() != null)
							{
								if (_owner.getParty().getCommandChannel().getMembers().contains(enemy))
								{
									targetIt = false;
								}
							}
						}
						if ((_owner.getClan() != null) && !_owner.isInsideZone(ZoneId.PVP))
						{
							if (_owner.getClan().isMember(enemy.getObjectId()))
							{
								targetIt = false;
							}
							if ((_owner.getAllyId() > 0) && (enemy.getAllyId() > 0))
							{
								if (_owner.getAllyId() == enemy.getAllyId())
								{
									targetIt = false;
								}
							}
						}
						if ((enemy.getPvpFlag() == 0) && !enemy.isInsideZone(ZoneId.PVP))
						{
							targetIt = false;
						}
						if (enemy.isInsideZone(ZoneId.PEACE))
						{
							targetIt = false;
						}
						if ((_owner.getSiegeState() > 0) && (_owner.getSiegeState() == enemy.getSiegeState()))
						{
							targetIt = false;
						}
						if (!enemy.isVisible())
						{
							targetIt = false;
						}

						if (targetIt)
						{
							_target = enemy;
							return;
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("", e);
		}
	}

	private class Action implements Runnable
	{
		private final int _chance;
		private final CubicInstance _cubic;
		
		protected Action(CubicInstance cubic, int chance)
		{
			_cubic = cubic;
			_chance = chance;
		}

		@Override
		public void run()
		{
			try
			{
				if (_owner.isDead() || !_owner.isOnline())
				{
					stopAction();
					_owner.removeCubicById(_cubic.getId());
					_owner.broadcastUserInfo(true);
					cancelDisappear();
					return;
				}
				if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_owner))
				{
					if (_owner.hasSummon())
					{
						if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_owner.getSummon()))
						{
							stopAction();
							return;
						}
					}
					else
					{
						stopAction();
						return;
					}
				}

				if ((_cubicMaxCount > -1) && (_currentcount >= _cubicMaxCount))
				{
					stopAction();
					return;
				}
				boolean UseCubicCure = false;
				Skill skill = null;

				if ((_id >= SMART_CUBIC_EVATEMPLAR) && (_id <= SMART_CUBIC_SPECTRALMASTER))
				{
					final Effect[] effects = _owner.getAllEffects();
					for (final Effect e : effects)
					{
						if ((e != null) && e.getSkill().hasDebuffEffects() && e.getSkill().canBeDispeled())
						{
							UseCubicCure = true;
							if (e.triggersChanceSkill())
							{
								_owner.removeChanceEffect(e);
							}
							e.exit();
						}
					}
				}

				if (UseCubicCure)
				{
					final MagicSkillUse msu = new MagicSkillUse(_owner, _owner, SKILL_CUBIC_CURE, 1, 0, 0);
					_owner.broadcastPacket(msu);
					_currentcount++;
				}
				else if (Rnd.get(1, 100) < _chance)
				{
					skill = _skills.get(Rnd.get(_skills.size()));
					if (skill != null)
					{
						if (skill.getId() == SKILL_CUBIC_HEAL)
						{
							cubicTargetForHeal();
						}
						else
						{
							getCubicTarget();
							if (!isInCubicRange(_owner, _target))
							{
								_target = null;
							}
						}
						final Creature target = _target;
						if ((target != null) && (!target.isDead()))
						{
							if (Config.DEBUG)
							{
								_log.info("CubicInstance: Action.run();");
								_log.info("Cubic Id: " + _id + " Target: " + target.getName(null) + " distance: " + Math.sqrt(target.getDistanceSq(_owner.getX(), _owner.getY(), _owner.getZ())));
							}

							_owner.broadcastPacket(new MagicSkillUse(_owner, target, skill.getId(), skill.getLevel(), 0, 0));

							final SkillType type = skill.getSkillType();
							final ISkillHandler handler = SkillHandler.getInstance().getHandler(skill.getSkillType());
							final Creature[] targets =
							{
							        target
							};

							if ((type == SkillType.PARALYZE) || (type == SkillType.STUN) || (type == SkillType.ROOT) || (type == SkillType.AGGDAMAGE))
							{
								if (Config.DEBUG)
								{
									_log.info("CubicInstance: Action.run() handler " + type);
								}
								useCubicDisabler(type, CubicInstance.this, skill, targets);
							}
							else if (type == SkillType.MDAM)
							{
								if (Config.DEBUG)
								{
									_log.info("CubicInstance: Action.run() handler " + type);
								}
								useCubicMdam(CubicInstance.this, skill, targets);
							}
							else if ((type == SkillType.POISON) || (type == SkillType.DEBUFF) || (type == SkillType.DOT))
							{
								if (Config.DEBUG)
								{
									_log.info("CubicInstance: Action.run() handler " + type);
								}
								useCubicContinuous(CubicInstance.this, skill, targets);
							}
							else if (type == SkillType.DRAIN)
							{
								if (Config.DEBUG)
								{
									_log.info("CubicInstance: Action.run() skill " + type);
								}
								((SkillDrain) skill).useCubicSkill(CubicInstance.this, targets);
							}
							else
							{
								handler.useSkill(_owner, skill, targets);
								if (Config.DEBUG)
								{
									_log.info("CubicInstance: Action.run(); other handler");
								}
							}

							if (skill.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT))
							{
								if (Config.DEBUG)
								{
									_log.info("CubicInstance: Action.run() handler " + type);
								}
								useCubicContinuous(CubicInstance.this, skill, targets);
							}
							_currentcount++;
						}
					}
				}
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}

	public void useCubicContinuous(CubicInstance activeCubic, Skill skill, GameObject[] targets)
	{
		for (final Creature target : (Creature[]) targets)
		{
			if ((target == null) || target.isDead())
			{
				continue;
			}

			if (skill.isOffensive())
			{
				final byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, skill);
				final boolean acted = Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld);
				if (!acted)
				{
					activeCubic.getOwner().sendPacket(SystemMessageId.ATTACK_FAILED);
					continue;
				}

			}
			skill.getEffects(activeCubic, target, null);
		}
	}

	public void useCubicMdam(CubicInstance activeCubic, Skill skill, GameObject[] targets)
	{
		for (final Creature target : (Creature[]) targets)
		{
			if (target == null)
			{
				continue;
			}

			if (target.isAlikeDead())
			{
				if (target.isPlayer())
				{
					target.stopFakeDeath(true);
				}
				else
				{
					continue;
				}
			}

			final boolean mcrit = Formulas.calcMCrit(activeCubic.getOwner().getMCriticalHit(target, skill));
			final byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, skill);
			int damage = (int) Formulas.calcMagicDam(activeCubic, target, skill, mcrit, shld);

			if (Config.DEBUG)
			{
				_log.info("L2SkillMdam: useCubicSkill() -> damage = " + damage);
			}

			if (damage > 0)
			{
				if (!target.isRaid() && Formulas.calcAtkBreak(target, mcrit))
				{
					target.breakAttack();
					target.breakCast();
				}

				if (target.getStat().calcStat(Stats.VENGEANCE_SKILL_MAGIC_DAMAGE, 0, target, skill) > Rnd.get(100))
				{
					damage = 0;
				}
				else
				{
					activeCubic.getOwner().sendDamageMessage(target, damage, skill, mcrit, false, false);
					target.reduceCurrentHp(damage, activeCubic.getOwner(), skill);
				}
			}
		}
	}

	public void useCubicDisabler(SkillType type, CubicInstance activeCubic, Skill skill, GameObject[] targets)
	{
		if (Config.DEBUG)
		{
			_log.info("Disablers: useCubicSkill()");
		}

		for (final Creature target : (Creature[]) targets)
		{
			if ((target == null) || target.isDead())
			{
				continue;
			}

			final byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, skill);

			switch (type)
			{
				case STUN :
				case PARALYZE :
				case ROOT :
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld))
					{
						skill.getEffects(activeCubic, target, null);
						if (Config.DEBUG)
						{
							_log.info("Disablers: useCubicSkill() -> success");
						}
					}
					else
					{
						if (Config.DEBUG)
						{
							_log.info("Disablers: useCubicSkill() -> failed");
						}
					}
					break;
				}
				case AGGDAMAGE :
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld))
					{
						if (target instanceof Attackable)
						{
							target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeCubic.getOwner(), (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
						}
						skill.getEffects(activeCubic, target, null);

						if (Config.DEBUG)
						{
							_log.info("Disablers: useCubicSkill() -> success");
						}
					}
					else
					{
						if (Config.DEBUG)
						{
							_log.info("Disablers: useCubicSkill() -> failed");
						}
					}
					break;
				}
			}
		}
	}

	public boolean isInCubicRange(Creature owner, Creature target)
	{
		if ((owner == null) || (target == null))
		{
			return false;
		}

		int x, y, z;
		final int range = MAX_MAGIC_RANGE;

		x = (owner.getX() - target.getX());
		y = (owner.getY() - target.getY());
		z = (owner.getZ() - target.getZ());

		return (((x * x) + (y * y) + (z * z)) <= (range * range));
	}

	public void cubicTargetForHeal()
	{
		Creature target = null;
		double percentleft = 100.0;
		Party party = _owner.getParty();

		if (_owner.isInDuel())
		{
			if (!DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
			{
				party = null;
			}
		}

		if ((party != null) && !_owner.isInOlympiadMode())
		{
			final List<Player> partyList = party.getMembers();
			for (final Creature partyMember : partyList)
			{
				if (!partyMember.isDead())
				{
					if (isInCubicRange(_owner, partyMember))
					{
						if (partyMember.getCurrentHp() < partyMember.getMaxHp())
						{
							if (percentleft > (partyMember.getCurrentHp() / partyMember.getMaxHp()))
							{
								percentleft = (partyMember.getCurrentHp() / partyMember.getMaxHp());
								target = partyMember;
							}
						}
					}
				}
				if (partyMember.getSummon() != null)
				{
					if (partyMember.getSummon().isDead())
					{
						continue;
					}

					if (!isInCubicRange(_owner, partyMember.getSummon()))
					{
						continue;
					}

					if (partyMember.getSummon().getCurrentHp() < partyMember.getSummon().getMaxHp())
					{
						if (percentleft > (partyMember.getSummon().getCurrentHp() / partyMember.getSummon().getMaxHp()))
						{
							percentleft = (partyMember.getSummon().getCurrentHp() / partyMember.getSummon().getMaxHp());
							target = partyMember.getSummon();
						}
					}
				}
			}
		}
		else
		{
			if (_owner.getCurrentHp() < _owner.getMaxHp())
			{
				target = _owner;
			}
			
			if (_owner.hasSummon() && !_owner.getSummon().isDead())
			{
				if ((_owner.getSummon().getCurrentHp() != _owner.getSummon().getMaxHp()) && (_owner.getCurrentHpPercents() > _owner.getSummon().getCurrentHpPercents()) && isInCubicRange(_owner, _owner.getSummon()))
				{
					target = _owner.getSummon();
				}
			}
		}

		_target = target;
	}

	public boolean givenByOther()
	{
		return _givenByOther;
	}

	protected class Heal implements Runnable
	{
		private final CubicInstance _cubic;
		
		public Heal(CubicInstance cubic)
		{
			_cubic = cubic;
		}
		
		@Override
		public void run()
		{
			if (_owner.isDead() || !_owner.isOnline())
			{
				stopAction();
				_owner.removeCubicById(_cubic.getId());
				_owner.broadcastUserInfo(true);
				cancelDisappear();
				return;
			}
			try
			{
				Skill skill = null;
				for (final Skill sk : _skills)
				{
					if (sk.getId() == SKILL_CUBIC_HEAL)
					{
						skill = sk;
						break;
					}
				}

				if (skill != null)
				{
					cubicTargetForHeal();
					final Creature target = _target;
					if ((target != null) && !target.isDead())
					{
						final int chance = target.getCurrentHpPercents() <= 60 ? 100 : 50;
						if (target.getCurrentHpPercents() < (target.isSummon() ? 95 : 100) && Rnd.getChance(chance))
						{
							final Creature[] targets =
							{
							        target
							};
							final ISkillHandler handler = SkillHandler.getInstance().getHandler(skill.getSkillType());
							if (handler != null)
							{
								handler.useSkill(_owner, skill, targets);
							}
							else
							{
								skill.useSkill(_owner, targets);
							}

							final MagicSkillUse msu = new MagicSkillUse(_owner, target, skill.getId(), skill.getLevel(), 0, 0);
							_owner.broadcastPacket(msu);
						}
					}
				}
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}

	private class Disappear implements Runnable
	{
		private final CubicInstance _cubic;
		
		public Disappear(CubicInstance cubic)
		{
			_cubic = cubic;
		}

		@Override
		public void run()
		{
			stopAction();
			_owner.removeCubicById(_cubic.getId());
			_owner.broadcastUserInfo(true);
		}
	}
}