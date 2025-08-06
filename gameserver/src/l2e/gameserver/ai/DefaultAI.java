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

import static l2e.gameserver.ai.model.CtrlIntention.ACTIVE;
import static l2e.gameserver.ai.model.CtrlIntention.ATTACK;
import static l2e.gameserver.ai.model.CtrlIntention.IDLE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.collections.LazyArrayList;
import l2e.commons.math.random.RndSelector;
import l2e.commons.util.NpcUtils;
import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Corpse;
import l2e.gameserver.ai.npc.Mystic;
import l2e.gameserver.ai.npc.Priest;
import l2e.gameserver.ai.npc.Ranger;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.FestivalMonsterInstance;
import l2e.gameserver.model.actor.instance.FriendlyMobInstance;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.actor.instance.GuardInstance;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.instance.QuestGuardInstance;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.actor.instance.RiftInvaderInstance;
import l2e.gameserver.model.actor.templates.npc.champion.ChampionTemplate;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.taskmanager.AiTaskManager;

public class DefaultAI extends CharacterAI implements Runnable
{
	private Future<?> _aiTask;
	protected long _attackTimeout;
	private int _globalAggro;
	private boolean _thinking = false;
	private boolean _canNotifyFriend;
	private int _waitTimeOut;
	private int _pathfindFails = -1;
	private long _waitTimeOutTime = 0;
	private final long _waitTimeOutLimit = 300000;
	private long _lastFactionNotifyTime = 0;
	protected long _minFactionNotifyInterval = Config.NPC_AI_FACTION_TASK;
	
	private final Skill _defSkill = SkillsParser.getInstance().getInfo(5044, 3);
	private volatile int _defFlag = 0;
	private final int _defRate;
	private long _defInterval = 0;
	private long _runInterval = 0;
	private long _moveInterval = 0;
	private long _lastBuffInterval = 0;
	
	private long _madnessTask = 0L;
	protected int MAX_PURSUE_RANGE;
	private final boolean _isGlobalAggro;
	private final boolean _isGuard;
	private long _randomAnimationEnd;
	private long _lifeTime = 0;
	
	protected Skill[] _damSkills, _dotSkills, _debuffSkills, _healSkills, _buffSkills, _stunSkills, _suicideSkills, _resSkills;
	
	public DefaultAI(Attackable actor)
	{
		super(actor);
		
		_attackTimeout = Long.MAX_VALUE;
		_waitTimeOut = -1;
		_canNotifyFriend = true;
		MAX_PURSUE_RANGE = actor.getTemplate().getParameter("maxPursueRange", actor.isRaid() ? Config.MAX_PURSUE_RANGE_RAID : Config.MAX_PURSUE_RANGE);
		_defRate = actor.getTemplate().getParameter("defenceChance", 0);
		_isGlobalAggro = actor.getTemplate().getParameter("isGlobalAggro", true);
		final Attackable npc = getActiveChar();
		_damSkills = npc.getTemplate().getDamageSkills();
		_dotSkills = npc.getTemplate().getDotSkills();
		_debuffSkills = npc.getTemplate().getDebuffSkills();
		_buffSkills = npc.getTemplate().getBuffSkills();
		_stunSkills = npc.getTemplate().getStunSkills();
		_healSkills = npc.getTemplate().getHealSkills();
		_suicideSkills = npc.getTemplate().getSuicideSkills();
		_resSkills = npc.getTemplate().getResSkills();
		_isGuard = npc instanceof GuardInstance;
		final ChampionTemplate ct = npc.getChampionTemplate();
		if (ct != null)
		{
			ct.skills.forEach(this::addSkill);
		}
	}

	@Override
	public void run()
	{
		onEvtThink();
	}
	
	protected boolean checkAggression(Creature target)
	{
		if ((target == null) || (getActiveChar() == null))
		{
			return false;
		}
		final var me = getActiveChar();

		if (target.isDoor() || target.isAlikeDead() || me.isInBlockList(target))
		{
			return false;
		}
		
		final var haveHideAggro = me.getHideAggroRange() > 0;
		if (haveHideAggro)
		{
			if (target.isPlayable())
			{
				if (target.isInvisible() || (((Playable) target).isSilentMoving()))
				{
					if (!me.isInsideRadius(target, me.getHideAggroRange(), true, false))
					{
						return false;
					}
				}
				else
				{
					if (!me.isInsideRadius(target, me.getAggroRange(), true, false))
					{
						return false;
					}
				}
			}
		}
		else
		{
			if (target.isInvisible() || (target.isPlayable() && !me.isInsideRadius(target, me.getAggroRange(), true, false)))
			{
				return false;
			}
		}
		
		if (target.isPlayable() && !haveHideAggro)
		{
			if ((!(me instanceof GrandBossInstance) || !(me.isRaid())) && !(me.canSeeThroughSilentMove()) && ((Playable) target).isSilentMoving())
			{
				return false;
			}
		}

		final var player = target.getActingPlayer();
		if (player != null)
		{
			if (player.isGM() && !player.getAccessLevel().canTakeAggro())
			{
				return false;
			}

			if (player.isFakeDeath())
			{
				return false;
			}

			if (player.isInParty() && player.getParty().isInDimensionalRift())
			{
				final var riftRoom = player.getParty().getDimensionalRift().getCurrentRoom();
				if (riftRoom != null && (me instanceof RiftInvaderInstance) && !riftRoom.isInside(player))
				{
					return false;
				}
			}
		}

		if (_isGuard)
		{
			if ((player != null) && (player.getKarma() > 0) && me.getId() != 4328)
			{
				return GeoEngine.getInstance().canSeeTarget(me, player);
			}

			if ((target.isMonster()) && Config.GUARD_ATTACK_AGGRO_MOB)
			{
				if (!me.isInsideRadius(target, 600, true, false))
				{
					return false;
				}
				return (((MonsterInstance) target).isAggressive() && GeoEngine.getInstance().canSeeTarget(me, target));
			}
			return false;
		}
		else if (me instanceof FriendlyMobInstance)
		{
			if (target instanceof Npc)
			{
				return false;
			}

			if ((target instanceof Player) && (((Player) target).getKarma() > 0))
			{
				return GeoEngine.getInstance().canSeeTarget(me, target);
			}
			return false;
		}
		else
		{
			if (target instanceof Npc)
			{
				return false;
			}

			if (!Config.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE))
			{
				return false;
			}

			if (me.getChampionTemplate() != null && me.getChampionTemplate().isPassive)
			{
				return false;
			}

			if (me.getSpawn() != null)
			{
				final var ai = me.getAggroList().get(target);
				if ((ai != null) && (ai.getHate() > 0))
				{
					if (!target.isInRangeZ(me.getSpawnedLoc(), MAX_PURSUE_RANGE))
					{
						return false;
					}
				}
			}
			return (me.isAggressive() && GeoEngine.getInstance().canSeeTarget(me, target));
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
	public synchronized void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		super.stopAITask();
	}
	
	@Override
	protected synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if ((intention == IDLE) || (intention == ACTIVE))
		{
			final var npc = getActiveChar();
			if (!npc.isAlikeDead())
			{
				if (npc.isInActiveRegion() || isGlobalAI())
				{
					intention = ACTIVE;
				}
				else
				{
					if (npc.getSpawn() != null)
					{
						final var range = Config.MAX_DRIFT_RANGE;
						if (!npc.isInsideRadius(npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ(), range + range, true, false))
						{
							intention = ACTIVE;
						}
					}
				}
				
				if (isGlobalAI() || intention != IDLE)
				{
					super.changeIntention(intention, arg0, arg1);
					startAITask();
				}
			}
			
			if (intention == IDLE)
			{
				super.changeIntention(IDLE, null, null);
				stopAITask();
			}
			return;
		}
		super.changeIntention(intention, arg0, arg1);
	}
	
	@Override
	protected void changeIntentionToCast(Skill skill, GameObject target)
	{
		setTarget(target);
		super.changeIntentionToCast(skill, target);
	}

	@Override
	protected void onIntentionAttack(Creature target, boolean shift)
	{
		_attackTimeout = System.currentTimeMillis() + 120000L;
		super.onIntentionAttack(target, shift);
	}
	
	protected void thinkCast()
	{
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill), false))
		{
			return;
		}
		clientStopMoving(null);
		setIntention(ACTIVE);
		_actor.doCast(_skill);
	}
	
	protected boolean thinkActive()
	{
		final var npc = getActiveChar();
		if (npc.isActionsDisabled())
		{
			return true;
		}
		
		if (npc.getFindTargetDelay() > System.currentTimeMillis())
		{
			clientStartAutoAttack();
			return false;
		}
		
		if (_globalAggro < 0)
		{
			_globalAggro++;
		}
		
		if (_globalAggro >= 0 && (npc.isAggressive() || _isGuard))
		{
			final var targets = (_isGuard && Config.GUARD_ATTACK_AGGRO_MOB) ? World.getInstance().getAroundCharacters(npc, 600, 300) : World.getInstance().getAroundPlayables(npc, npc.getAggroRange(), 300);
			for (final var target : targets)
			{
				if (checkAggression(target))
				{
					final var hating = npc.getAggroList().getHating(target);
					if (hating == 0)
					{
						npc.addDamageHate(target, 0, 0);
					}
					break;
				}
			}
			targets.clear();
			
			Creature hated;
			if (npc.isConfused())
			{
				hated = getAttackTarget();
			}
			else
			{
				hated = npc.getAggroList().getMostHated();
			}
			
			if ((hated != null) && !npc.isCoreAIDisabled())
			{
				final var aggro = npc.getAggroList().getHating(hated);
				
				if ((aggro + _globalAggro) >= 0)
				{
					if (!npc.isRunning())
					{
						if (npc.isEkimusFood())
						{
							npc.setWalking();
						}
						else
						{
							npc.setRunning();
						}
					}
					setIntention(CtrlIntention.ATTACK, hated, false);
				}
				return true;
			}
		}
		
		if (_lifeTime > 0 && System.currentTimeMillis() > _lifeTime)
		{
			_lifeTime = 0;
			npc.deleteMe();
			return false;
		}
		
		if (!npc.getAttackByList().isEmpty() && Rnd.chance(1) && npc.isCurrentHpFull())
		{
			npc.clearAggroList(true);
			npc.getAttackByList().clear();
		}
		
		if (_isGuard && !npc.isWalker() && !npc.isRunner() && !npc.isSpecialCamera() && !npc.isEkimusFood())
		{
			if (!(npc instanceof QuestGuardInstance))
			{
				((GuardInstance) npc).returnHome();
			}
		}
		
		if (npc instanceof FestivalMonsterInstance)
		{
			return false;
		}
		
		if (!npc.canReturnToSpawnPoint())
		{
			return false;
		}
		
		if (_waitTimeOut > 0)
		{
			_waitTimeOut--;
			if (_waitTimeOut == 0 && npc.getSpawn() != null && !npc.isRunner())
			{
				returnHome();
			}
			return false;
		}
		
		if (randomAnimation())
		{
			return true;
		}
		
		final var leader = npc.getLeader();
		if (leader != null)
		{
			if (leader.isDead() && !leader.isVisible())
			{
				npc.deleteMe();
				return false;
			}
			
			final var distance = npc.getDistance(leader);
			if (distance > 1000 || !GeoEngine.getInstance().canSeeTarget(npc, leader))
			{
				final var loc = leader.getMinionPosition();
				if (loc != null)
				{
					npc.teleToLocation(loc, true, npc.getReflection());
				}
				else
				{
					_log.warn("Problem to found minion position for npcId[" + npc.getId() + "] Current loc: " + npc.getX() + " " + npc.getY() + " " + npc.getZ());
					npc.doDie(null);
				}
				return true;
			}
			else if (distance > 200)
			{
				final var loc = leader.getMinionPosition();
				if (loc != null)
				{
					if (leader.isRunning())
					{
						npc.setRunning();
					}
					else
					{
						npc.setWalking();
					}
					moveTo(loc);
				}
				else
				{
					_log.warn("Problem to found minion position for npcId[" + npc.getId() + "] Current loc: " + npc.getX() + " " + npc.getY() + " " + npc.getZ());
					npc.doDie(null);
				}
				return true;
			}
		}
		else if ((npc.getSpawn() != null) && !npc.isNoRndWalk() && npc.canReturnToSpawnPoint())
		{
			if (maybeMoveToHome())
			{
				return true;
			}
		}
		else if ((npc.getSpawn() != null) && npc.isNoRndWalk() && npc.canReturnToSpawnPoint())
		{
			final var isInRange = npc.isInRangeZ(npc.getSpawnedLoc(), Config.MAX_DRIFT_RANGE);
			if (isInRange)
			{
				return false;
			}

			if (maybeMoveToHome())
			{
				return true;
			}
		}
		return false;
	}
	
	protected void thinkAttack()
	{
		final var npc = getActiveChar();
		if (npc.isCastingNow() || npc.isAttackingNow() || npc.isCoreAIDisabled() || _runInterval > System.currentTimeMillis() || npc.getFindTargetDelay() > System.currentTimeMillis())
		{
			clientStartAutoAttack();
			return;
		}

		if (npc.getSpawn() != null && !npc.isInRange(npc.getSpawnedLoc(), MAX_PURSUE_RANGE))
		{
			returnHome();
			return;
		}
		
		final var leader = npc.getLeader();
		if (leader != null)
		{
			if (!leader.isDead() && leader.isVisible())
			{
				final var distance = npc.getDistance(leader);
				if (distance > 2000)
				{
					final var loc = leader.getMinionPosition();
					if (loc != null)
					{
						npc.clearAggroList(true);
						npc.teleToLocation(loc, true, npc.getReflection());
					}
					else
					{
						_log.warn("Problem to found minion position for npcId[" + npc.getId() + "] Current loc: " + npc.getX() + " " + npc.getY() + " " + npc.getZ());
						npc.doDie(null);
					}
					return;
				}
			}
		}

		final var target = npc.getAggroList().getMostHated();
		if (target == null)
		{
			_waitTimeOut = 10;
			setIntention(ACTIVE);
			return;
		}
		
		setAttackTarget(target);
		npc.setTarget(target);

		final var attackTarget = getAttackTarget();
		
		if (attackTarget == null || attackTarget.isAlikeDead() || (getAttackTimeout() < System.currentTimeMillis()))
		{
			if (attackTarget != null)
			{
				npc.getAggroList().stopHating(attackTarget);
			}
			
			_waitTimeOut = 10;
			setIntention(ACTIVE);
			return;
		}
		
		if (!npc.isInRangeZ(attackTarget, MAX_PURSUE_RANGE))
		{
			npc.getAggroList().stopHating(attackTarget);
			if (npc.getAggroList().isEmpty())
			{
				_waitTimeOut = 10;
				setIntention(ACTIVE);
			}
			return;
		}

		final var collision = (int) npc.getColRadius();
		final var combinedCollision = (int) (collision + target.getColRadius());

		if (!npc.isMovementDisabled() && (Rnd.nextInt(100) <= 3))
		{
			for (final var nearby : World.getInstance().getAroundNpc(npc))
			{
				if ((nearby instanceof Attackable) && npc.isInsideRadius(nearby, collision, false, false) && (nearby != target))
				{
					final var newX = (Rnd.nextBoolean() ? target.getX() + combinedCollision + Rnd.get(40) : (target.getX() - combinedCollision) + Rnd.get(40));
					final var newY = (Rnd.nextBoolean() ? target.getY() + combinedCollision + Rnd.get(40) : (target.getY() - combinedCollision) + Rnd.get(40));
					
					if (!npc.isInsideRadius(newX, newY, 0, collision, false, false))
					{
						final var newZ = npc.getZ() + 30;
						if (GeoEngine.getInstance().canMoveToCoord(npc, npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ, npc.getReflection(), false))
						{
							moveTo(newX, newY, newZ, 0);
						}
					}
					return;
				}
			}
		}

		if (!npc.isMovementDisabled() && (npc.getAI() instanceof Ranger))
		{
			if (Rnd.get(100) <= getRateDodge())
			{
				final var distance2 = npc.getPlanDistanceSq(target.getX(), target.getY());
				if (Math.sqrt(distance2) <= (60 + combinedCollision))
				{
					final var newX = (Rnd.nextBoolean() ? target.getX() + combinedCollision + Rnd.get(300) : (target.getX() - combinedCollision) + Rnd.get(300));
					final var newY = (Rnd.nextBoolean() ? target.getY() + combinedCollision + Rnd.get(300) : (target.getY() - combinedCollision) + Rnd.get(300));
					final var loc = GeoEngine.getInstance().moveCheck(npc, npc.getX(), npc.getY(), npc.getZ(), newX, newY, npc.getZ(), npc.getReflection());
					if (loc != null)
					{
						_runInterval = System.currentTimeMillis() + 1500L;
						moveTo(loc, 0);
						return;
					}
				}
			}
		}

		if (npc.isRaid() || npc.isRaidMinion())
		{
			if (_madnessTask > 0 && _madnessTask < System.currentTimeMillis())
			{
				if (npc.isConfused())
				{
					npc.stopConfused();
				}
				_madnessTask = 0L;
			}
			
			if (Rnd.chance(npc.getTemplate().getParameter("isMadness", 0)) && !npc.isConfused())
			{
				if (npc instanceof RaidBossInstance)
				{
					if (!((MonsterInstance) npc).hasMinions())
					{
						if (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 100) / npc.getMaxHp())))
						{
							aggroReconsider();
							return;
						}
					}
					else
					{
						if (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp())))
						{
							aggroReconsider();
							return;
						}
					}
				}
				else if (npc instanceof GrandBossInstance)
				{
					final var chaosRate = 100 - ((npc.getCurrentHp() * 300) / npc.getMaxHp());
					if (((chaosRate <= 10) && (Rnd.get(100) <= 10)) || ((chaosRate > 10) && (Rnd.get(100) <= chaosRate)))
					{
						aggroReconsider();
						return;
					}
				}
				else
				{
					if (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp())))
					{
						aggroReconsider();
						return;
					}
				}
			}
		}

		final var dist = Math.sqrt(npc.getPlanDistanceSq(target.getX(), target.getY()));
		final var dist2 = (int) dist - collision;
		var range = npc.getPhysicalAttackRange();
		final var tgt = getAttackTarget();
		final var isBehind = tgt != null && npc.isBehind(tgt);
		range += tgt != null && tgt.isMoving() ? isBehind ? 50 : 25 : 25;
		
		if (npc.isMovementDisabled())
		{
			movementDisable();
			return;
		}

		if (createNewTask())
		{
			return;
		}
		
		final var canSee = GeoEngine.getInstance().canSeeTarget(npc, target);
		if ((dist2 > range) || !canSee)
		{
			if (npc.isMovementDisabled())
			{
				targetReconsider();
			}
			else
			{
				if (tgt != null)
				{
					if (!npc.isRunning())
					{
						npc.setRunning();
					}
					moveTo(tgt.getLocation(), !canSee ? 0 : ((tgt.isMoving() && isBehind) ? npc.getPhysicalAttackRange() : range));
					if (!npc.isMoving() && !npc.isEkimusFood() && tgt.isPlayable() && _isGlobalAggro)
					{
						if (npc.getDistance(tgt) < 2000 && _moveInterval < System.currentTimeMillis() && !GeoEngine.getInstance().canMoveToCoord(npc, npc.getX(), npc.getY(), npc.getZ(), tgt.getX(), tgt.getY(), tgt.getZ(), npc.getReflection(), false))
						{
							_moveInterval = System.currentTimeMillis() + 2000L;
							_pathfindFails++;
							if (_pathfindFails >= 5)
							{
								npc.broadcastPacket(new MagicSkillUse(npc, npc, 2036, 1, 500, 0));
								npc.teleToLocation(tgt.getLocation(), true, npc.getReflection());
								_pathfindFails = 0;
								npc.setTarget(tgt);
								setAttackTarget(tgt);
								npc.addDamageHate(target, 0, 999);
								setIntention(CtrlIntention.ATTACK, tgt, false);
								if (npc.hasMinions())
								{
									final var minionList = npc.getMinionList();
									if (minionList != null)
									{
										for (final var minion : minionList.getAliveMinions())
										{
											if (minion != null && minion != npc)
											{
												minion.getAI().setIntention(CtrlIntention.ATTACK, tgt, false);
											}
										}
									}
								}
							}
							else
							{
								final var sloc = npc.getSpawnedLoc();
								if (sloc != null)
								{
									if (!npc.isRunning())
									{
										npc.setRunning();
									}
									moveTo(sloc, 0);
								}
							}
						}
					}
					return;
				}
			}
			return;
		}
		clientStopMoving(null);
		_actor.doAttack(getAttackTarget());
	}
	
	protected boolean cast(Skill sk)
	{
		if (sk == null)
		{
			return false;
		}
		
		final var caster = getActiveChar();
		if (caster.isCastingNow())
		{
			return false;
		}
		
		if (!checkSkillCastConditions(sk))
		{
			return false;
		}
		if (getAttackTarget() == null)
		{
			final var mostHate = caster.getAggroList().getMostHated();
			if (mostHate != null)
			{
				setAttackTarget(mostHate);
			}
		}
		
		final var attackTarget = getAttackTarget();
		final var dist = attackTarget != null ? Math.sqrt(caster.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY())) : 0;
		var dist2 = attackTarget != null ? dist - attackTarget.getColRadius() : 0;
		final var range = attackTarget != null ? caster.getPhysicalAttackRange() + caster.getColRadius() + attackTarget.getColRadius() : 0;
		final var srange = attackTarget != null ? sk.getCastRange() + caster.getColRadius() : 0;
		if (attackTarget != null && attackTarget.isMoving())
		{
			dist2 = dist2 - 30;
		}
		
		switch (sk.getSkillType())
		{
			case BUFF :
			{
				if (caster.getFirstEffect(sk) == null)
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					return true;
				}
				
				if (sk.getTargetType() == TargetType.SELF)
				{
					return false;
				}
				
				if (sk.getTargetType() == TargetType.ONE)
				{
					final var target = effectTargetReconsider(sk, true);
					if (target != null)
					{
						clientStopMoving(null);
						final GameObject targets = attackTarget;
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(targets);
						return true;
					}
				}
				
				if (canParty(sk))
				{
					clientStopMoving(null);
					final GameObject targets = attackTarget;
					caster.setTarget(caster);
					caster.doCast(sk);
					caster.setTarget(targets);
					return true;
				}
				return false;
			}
			case RESURRECT :
			{
				if (!isParty(sk))
				{
					if (caster.isMinion() && (sk.getTargetType() != TargetType.SELF))
					{
						final var leader = caster.getLeader();
						if ((leader != null) && leader.isDead())
						{
							if (!Util.checkIfInRange((int) (sk.getCastRange() + caster.getColRadius() + leader.getColRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
							{
								moveToPawn(leader, (int) (sk.getCastRange() + caster.getColRadius() + leader.getColRadius()));
							}
						}
						if (GeoEngine.getInstance().canSeeTarget(caster, leader))
						{
							clientStopMoving(null);
							caster.setTarget(leader);
							caster.doCast(sk);
							return true;
						}
					}
					
					for (final var obj : World.getInstance().getAroundCharacters(caster, (int) (sk.getCastRange() + caster.getColRadius()), 200))
					{
						if (!(obj instanceof Attackable) || !obj.isDead())
						{
							continue;
						}
						
						final var targets = ((Attackable) obj);
						if ((!caster.getFaction().isNone()) && !caster.isInFaction(targets))
						{
							continue;
						}
						if (Rnd.get(100) < 10)
						{
							if (GeoEngine.getInstance().canSeeTarget(caster, targets))
							{
								clientStopMoving(null);
								caster.setTarget(obj);
								caster.doCast(sk);
								return true;
							}
						}
					}
				}
				else if (isParty(sk))
				{
					for (final var obj : World.getInstance().getAroundCharacters(caster, (int) (sk.getAffectRange() + caster.getColRadius()), 200))
					{
						if (!(obj instanceof Attackable))
						{
							continue;
						}
						final var targets = ((Npc) obj);
						if ((!caster.getFaction().isNone()) && caster.isInFaction(targets))
						{
							if ((obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
							{
								clientStopMoving(null);
								caster.setTarget(caster);
								caster.doCast(sk);
								return true;
							}
						}
					}
				}
				return false;
			}
			case DEBUFF :
			case POISON :
			case DOT :
			case MDOT :
			case BLEED :
			{
				if (attackTarget == null)
				{
					return false;
				}
				
				if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
				{
					if (attackTarget.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if (sk.isAura() || sk.isArea())
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == TargetType.ONE)
				{
					final var target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				return false;
			}
			case SLEEP :
			{
				if (attackTarget == null)
				{
					return false;
				}
				
				if (sk.getTargetType() == TargetType.ONE)
				{
					if (!attackTarget.isDead() && (dist2 <= srange))
					{
						if ((dist2 > range) || attackTarget.isMoving())
						{
							if (attackTarget.getFirstEffect(sk) == null)
							{
								clientStopMoving(null);
								caster.doCast(sk);
								return true;
							}
						}
					}
					
					final var target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				return false;
			}
			case ROOT :
			case STUN :
			case PARALYZE :
			case MUTE :
			case FEAR :
			{
				if (attackTarget == null)
				{
					return false;
				}
				
				if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && (dist2 <= srange))
				{
					if (attackTarget.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == TargetType.ONE)
				{
					final var target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				return false;
			}
			
			case PDAM :
			case MDAM :
			case BLOW :
			case DRAIN :
			case CHARGEDAM :
			case FATAL :
			case DEATHLINK :
			case MANADAM :
			case CPDAMPERCENT :
			{
				if (attackTarget == null)
				{
					return false;
				}
				
				if (!canAura(sk))
				{
					if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					
					final var target = skillTargetReconsider(sk);
					if (target != null)
					{
						clientStopMoving(null);
						final GameObject targets = attackTarget;
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(targets);
						return true;
					}
				}
				else
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				return false;
			}
			default :
			{
				if (sk.hasEffectType(EffectType.CANCEL, EffectType.CANCEL_ALL, EffectType.CANCEL_BY_SLOT, EffectType.NEGATE))
				{
					if (Rnd.get(50) != 0)
					{
						return true;
					}
					
					if (sk.getTargetType() == TargetType.ONE)
					{
						if (attackTarget == null)
						{
							return false;
						}
						
						if ((attackTarget.getFirstEffect(EffectType.BUFF) != null) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
						{
							clientStopMoving(null);
							caster.doCast(sk);
							return true;
						}
						final var target = effectTargetReconsider(sk, false);
						if (target != null)
						{
							clientStopMoving(null);
							final GameObject targets = attackTarget;
							caster.setTarget(target);
							caster.doCast(sk);
							caster.setTarget(targets);
							return true;
						}
					}
					else if (canAOE(sk))
					{
						if (((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget))
						{
							clientStopMoving(null);
							caster.doCast(sk);
							return true;
						}
						else if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
						{
							clientStopMoving(null);
							caster.doCast(sk);
							return true;
						}
					}
				}
				
				if (sk.hasEffectType(EffectType.HEAL, EffectType.HEAL_PERCENT))
				{
					var percentage = (caster.getCurrentHp() / caster.getMaxHp()) * 100;
					if (caster.isMinion() && (sk.getTargetType() != TargetType.SELF))
					{
						final var leader = caster.getLeader();
						if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
						{
							if (!Util.checkIfInRange((int) (sk.getCastRange() + caster.getColRadius() + leader.getColRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
							{
								moveToPawn(leader, (int) (sk.getCastRange() + caster.getColRadius() + leader.getColRadius()));
							}
							if (GeoEngine.getInstance().canSeeTarget(caster, leader))
							{
								clientStopMoving(null);
								caster.setTarget(leader);
								caster.doCast(sk);
								return true;
							}
						}
					}
					if (Rnd.get(100) < ((100 - percentage) / 3))
					{
						clientStopMoving(null);
						caster.setTarget(caster);
						caster.doCast(sk);
						return true;
					}
					
					if (sk.getTargetType() == TargetType.ONE)
					{
						for (final var obj : World.getInstance().getAroundCharacters(caster, (int) (sk.getCastRange() + caster.getColRadius()), 200))
						{
							if (!(obj instanceof Attackable) || obj.isDead())
							{
								continue;
							}
							
							final var targets = ((Attackable) obj);
							if ((!caster.getFaction().isNone()) && !caster.isInFaction(targets))
							{
								continue;
							}
							percentage = (targets.getCurrentHp() / targets.getMaxHp()) * 100;
							if (Rnd.get(100) < ((100 - percentage) / 10))
							{
								if (GeoEngine.getInstance().canSeeTarget(caster, targets))
								{
									clientStopMoving(null);
									caster.setTarget(obj);
									caster.doCast(sk);
									return true;
								}
							}
						}
					}
					if (isParty(sk))
					{
						for (final var obj : World.getInstance().getAroundCharacters(caster, (int) (sk.getAffectRange() + caster.getColRadius()), 200))
						{
							if (!(obj instanceof Attackable))
							{
								continue;
							}
							final var targets = ((Npc) obj);
							if ((!caster.getFaction().isNone()) && targets.isInFaction(caster))
							{
								if ((obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
								{
									clientStopMoving(null);
									caster.setTarget(caster);
									caster.doCast(sk);
									return true;
								}
							}
						}
					}
				}
				if (!canAura(sk))
				{
					if (attackTarget == null)
					{
						return false;
					}
					
					if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					
					final var target = skillTargetReconsider(sk);
					if (target != null)
					{
						clientStopMoving(null);
						final GameObject targets = attackTarget;
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(targets);
						return true;
					}
				}
				else
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				return false;
				
			}
		}
	}
	
	protected void movementDisable()
	{
		final var npc = getActiveChar();
		final var target = getAttackTarget();
		if (target == null)
		{
			return;
		}
		
		if (npc.getTarget() == null)
		{
			npc.setTarget(target);
		}
		
		final double dist = npc.getDistance(target);
		final int range = (int) (npc.getPhysicalAttackRange() + target.getColRadius());
		final int random = Rnd.get(100);
		final var canSee = GeoEngine.getInstance().canSeeTarget(npc, target);
		if ((dist <= range) && canSee && random > 50)
		{
			_actor.doAttack(target);
			return;
		}
		
		if (random < 5)
		{
			for (final var sk : npc.getTemplate().getDotSkills())
			{
				if (sk != null)
				{
					if (!canSee || !checkSkillCastConditions(sk) || (((sk.getCastRange() + npc.getColRadius() + target.getColRadius()) <= dist) && !canAura(sk)))
					{
						continue;
					}
					
					if (target.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}
		}

		if (random < 8)
		{
			for (final var sk : npc.getTemplate().getDebuffSkills())
			{
				if (sk != null)
				{
					if (!canSee || !checkSkillCastConditions(sk) || (((sk.getCastRange() + npc.getColRadius() + target.getColRadius()) <= dist) && !canAura(sk)))
					{
						continue;
					}
					
					if (target.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}
		}
		
		if ((npc.isMovementDisabled() || (npc.getAI() instanceof Mystic)) || (npc.getAI() instanceof Priest))
		{
			for (final var sk : npc.getTemplate().getDamageSkills())
			{
				if (sk != null)
				{
					if (!canSee || !checkSkillCastConditions(sk) || (((sk.getCastRange() + npc.getColRadius() + target.getColRadius()) <= dist) && !canAura(sk)))
					{
						continue;
					}
					clientStopMoving(null);
					npc.doCast(sk);
					return;
				}
			}
		}
		targetReconsider();
	}

	private boolean checkSkillCastConditions(Skill skill)
	{
		if (skill == null)
		{
			return false;
		}
		
		final var npc = getActiveChar();
		if (npc.isSkillDisabled(skill) || npc.isSkillBlocked(skill) || (skill.getMpConsume() >= npc.getCurrentMp()) || (!skill.isStatic() && ((skill.isMagic() && npc.isMuted()) || npc.isPhysicalMuted())))
		{
			return false;
		}
		return true;
	}
	
	private Creature effectTargetReconsider(Skill sk, boolean positive)
	{
		final var target = getAttackTarget();
		if (sk == null || target == null)
		{
			return null;
		}
		
		final var actor = getActiveChar();
		if (!sk.hasEffectType(EffectType.CANCEL, EffectType.CANCEL_ALL, EffectType.CANCEL_BY_SLOT, EffectType.NEGATE))
		{
			if (!positive)
			{
				var dist = 0.;
				var dist2 = 0.;
				var range = 0;
				
				for (final var obj : actor.getAttackByList())
				{
					if ((obj == null) || obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj) || (obj == target))
					{
						continue;
					}
					try
					{
						actor.setTarget(target);
						dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
						dist2 = dist - actor.getColRadius();
						range = (int) (sk.getCastRange() + actor.getColRadius() + obj.getColRadius());
						if (obj.isMoving())
						{
							dist2 = dist2 - 70;
						}
					}
					catch (final NullPointerException e)
					{
						continue;
					}
					if (dist2 <= range)
					{
						if (target.getFirstEffect(sk) == null)
						{
							return obj;
						}
					}
				}
				
				for (final var obj : World.getInstance().getAroundCharacters(actor, range, 200))
				{
					if (obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj))
					{
						continue;
					}
					try
					{
						actor.setTarget(target);
						dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
						dist2 = dist;
						range = (int) (sk.getCastRange() + actor.getColRadius() + obj.getColRadius());
						if (obj.isMoving())
						{
							dist2 = dist2 - 70;
						}
					}
					catch (final NullPointerException e)
					{
						continue;
					}
					
					if ((obj instanceof Player) || (obj instanceof Summon))
					{
						if (dist2 <= range && target != null)
						{
							if (target.getFirstEffect(sk) == null)
							{
								return obj;
							}
						}
					}
				}
			}
			else if (positive)
			{
				var dist = 0.;
				var dist2 = 0.;
				var range = 0;
				
				final var minions = actor.isMinion() && actor.getLeader() != null ? actor.getLeader().getMinionList() : actor.hasMinions() ? actor.getMinionList() : null;
				for (final var obj : World.getInstance().getAroundCharacters(actor, range, 200))
				{
					if (!(obj instanceof Attackable) || obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj))
					{
						continue;
					}
					
					final var targets = ((Attackable) obj);
					
					final var checkTarget = (!actor.getFaction().isNone() && actor.isInFaction(targets)) || (minions != null && minions.hasNpcId(targets.getId()));
					if (!checkTarget)
					{
						continue;
					}
					
					try
					{
						actor.setTarget(target);
						dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
						dist2 = dist - actor.getColRadius();
						range = (int) (sk.getCastRange() + actor.getColRadius() + obj.getColRadius());
						if (obj.isMoving())
						{
							dist2 = dist2 - 70;
						}
					}
					catch (final NullPointerException e)
					{
						continue;
					}
					if (dist2 <= range)
					{
						if (obj.getFirstEffect(sk) == null)
						{
							return obj;
						}
					}
				}
			}
		}
		else
		{
			var dist = 0.;
			var dist2 = 0.;
			var range = 0;
			range = (int) (sk.getCastRange() + actor.getColRadius() + target.getColRadius());
			for (final var obj : World.getInstance().getAroundCharacters(actor, range, 200))
			{
				if ((obj == null) || obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj))
				{
					continue;
				}
				try
				{
					actor.setTarget(target);
					dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
					dist2 = dist - actor.getColRadius();
					range = (int) (sk.getCastRange() + actor.getColRadius() + obj.getColRadius());
					if (obj.isMoving())
					{
						dist2 = dist2 - 70;
					}
				}
				catch (final NullPointerException e)
				{
					continue;
				}
				
				if ((obj instanceof Player) || (obj instanceof Summon))
				{
					if (dist2 <= range)
					{
						if (getAttackTarget().getFirstEffect(EffectType.BUFF) != null)
						{
							return obj;
						}
					}
				}
			}
		}
		return null;
	}
	
	private Creature skillTargetReconsider(Skill sk)
	{
		var dist = 0.;
		var dist2 = 0.;
		var range = 0;
		final var actor = getActiveChar();
		final List<Creature> hateList = actor.getAggroList().getHateList();
		if (hateList != null && !hateList.isEmpty())
		{
			for (final var obj : hateList)
			{
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead())
				{
					continue;
				}
				try
				{
					actor.setTarget(getAttackTarget());
					dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
					dist2 = dist - actor.getColRadius();
					range = (int) (sk.getCastRange() + actor.getColRadius() + getAttackTarget().getColRadius());
				}
				catch (final NullPointerException e)
				{
					continue;
				}
				if (dist2 <= range)
				{
					return obj;
				}
			}
		}
		
		if (!_isGuard)
		{
			for (final GameObject target : World.getInstance().getAroundObjects(actor))
			{
				try
				{
					actor.setTarget(getAttackTarget());
					dist = Math.sqrt(actor.getPlanDistanceSq(target.getX(), target.getY()));
					dist2 = dist;
					range = (int) (sk.getCastRange() + actor.getColRadius() + getAttackTarget().getColRadius());
				}
				catch (final NullPointerException e)
				{
					continue;
				}
				Creature obj = null;
				if (target instanceof Creature)
				{
					obj = (Creature) target;
				}
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || (dist2 > range))
				{
					continue;
				}
				if (obj instanceof Player)
				{
					return obj;
					
				}
				if (obj instanceof Summon)
				{
					return obj;
				}
			}
		}
		return null;
	}
	
	private void targetReconsider()
	{
		var dist = 0.;
		var dist2 = 0.;
		var range = 0;
		final var actor = getActiveChar();
		final var MostHate = actor.getAggroList().getMostHated();
		final List<Creature> hateList = actor.getAggroList().getHateList();
		if (hateList != null && !hateList.isEmpty())
		{
			for (final var obj : hateList)
			{
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != MostHate) || (obj == actor))
				{
					continue;
				}
				try
				{
					dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
					dist2 = dist - actor.getColRadius();
					range = (int) (actor.getPhysicalAttackRange() + actor.getColRadius() + obj.getColRadius());
					if (obj.isMoving())
					{
						dist2 = dist2 - 70;
					}
				}
				catch (final NullPointerException e)
				{
					continue;
				}
				
				if (dist2 <= range)
				{
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getAggroList().getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
					return;
				}
			}
		}
		if (!_isGuard)
		{
			for (final var obj : World.getInstance().getAroundCharacters(actor))
			{
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != MostHate) || (obj == actor) || (obj == getAttackTarget()))
				{
					continue;
				}
				if (obj instanceof Player)
				{
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getAggroList().getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
					
				}
				else if (obj instanceof Attackable)
				{
					if ((!((Attackable) obj).getFaction().isNone()) && ((Attackable) obj).isInFaction(actor))
					{
						continue;
					}
					
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getAggroList().getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj instanceof Summon)
				{
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getAggroList().getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			}
		}
	}
	
	protected void aggroReconsider()
	{
		final var actor = getActiveChar();
		final var MostHate = actor.getAggroList().getMostHated();
		final List<Creature> hateList = actor.getAggroList().getHateList();
		if (hateList != null && !hateList.isEmpty())
		{
			final int rand = Rnd.get(hateList.size());
			int count = 0;
			for (final var obj : hateList)
			{
				if (count < rand)
				{
					count++;
					continue;
				}
				
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj == getAttackTarget()) || (obj == actor))
				{
					continue;
				}
				
				try
				{
					actor.setTarget(getAttackTarget());
				}
				catch (final NullPointerException e)
				{
					continue;
				}
				if (MostHate != null)
				{
					actor.addDamageHate(obj, 0, actor.getAggroList().getHating(MostHate));
				}
				else
				{
					actor.addDamageHate(obj, 0, 2000);
				}
				actor.setTarget(obj);
				setAttackTarget(obj);
				if ((_madnessTask <= 0) && !actor.isConfused())
				{
					_madnessTask = System.currentTimeMillis() + 10000L;
					actor.startConfused();
				}
				return;
			}
		}
		
		if (!_isGuard)
		{
			for (final var obj : World.getInstance().getAroundCharacters(actor))
			{
				if (!GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != MostHate) || (obj == actor))
				{
					continue;
				}
				if (obj instanceof Player)
				{
					if ((MostHate != null) && !MostHate.isDead())
					{
						actor.addDamageHate(obj, 0, actor.getAggroList().getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj instanceof Attackable)
				{
					if ((!((Attackable) obj).getFaction().isNone()) && ((Attackable) obj).isInFaction(actor))
					{
						continue;
					}

					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getAggroList().getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj instanceof Summon)
				{
					if (MostHate != null)
					{
						actor.addDamageHate(obj, 0, actor.getAggroList().getHating(MostHate));
					}
					else
					{
						actor.addDamageHate(obj, 0, 2000);
					}
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				
				if ((_madnessTask <= 0) && !actor.isConfused())
				{
					_madnessTask = System.currentTimeMillis() + 10000L;
					actor.startConfused();
				}
			}
		}
	}
	
	@Override
	protected void onEvtThink()
	{
		final var actor = getActiveChar();
		if (_thinking || actor == null || actor.isActionsDisabled() || actor.isAfraid())
		{
			return;
		}
		
		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case ACTIVE :
				case MOVING :
					thinkActive();
					break;
				case ATTACK :
					thinkAttack();
					break;
				case CAST :
					thinkCast();
					break;
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	protected void onEvtClanAttacked(Creature attacker, int aggro)
	{
		final var me = getActiveChar();
		if (me == null || me.isDead())
		{
			return;
		}
		
		if (getIntention() != CtrlIntention.ACTIVE || attacker == null)
		{
			return;
		}
		me.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 2);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final var me = getActiveChar();
		
		if (attacker == null || me.isDead())
		{
			return;
		}
		
		_attackTimeout = System.currentTimeMillis() + 120000L;
		
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		final var transformer = me.getTemplate().getParameter("transformOnUnderAttack", 0);
		if (transformer > 0)
		{
			final var chance = me.getTemplate().getParameter("transformChance", 5);
			if ((chance == 100) || ((me.getCurrentHpPercents() > 50) && Rnd.chance(chance)))
			{
				me.onDecay();
				final MonsterInstance npc = NpcUtils.spawnSingle(transformer, me.getLocation(), me.getReflectionId());
				npc.setRunning();
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
				attacker.setTarget(npc);
				return;
			}
		}
		
		if (!Config.ALLOW_UNLIM_ENTER_CATACOMBS && me.isSevenSignsMonster())
		{
			final var player = attacker.getActingPlayer();
			if (player != null)
			{
				if ((SevenSigns.getInstance().isSealValidationPeriod()) || (SevenSigns.getInstance().isCompResultsPeriod()))
				{
					final var pcabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
					final var wcabal = SevenSigns.getInstance().getCabalHighestScore();
					if ((pcabal != wcabal) && (wcabal != SevenSigns.CABAL_NULL))
					{
						player.sendMessage("You have been teleported to the nearest town because you not signed for winning cabal.");
						player.teleToClosestTown();
						return;
					}
				}
			}
		}
		
		if (!me.isInvul())
		{
			me.addDamageHate(attacker, damage, (attacker.isSummon() || attacker.isPet()) ? (int) (damage * Config.PET_HATE_MOD) : 0);
		}
		if (!me.isRunning())
		{
			if (me.isEkimusFood())
			{
				me.setWalking();
			}
			else
			{
				me.setRunning();
			}
		}
		
		if ((me.getAggroList().getHating(attacker) > 0) && (attacker.isSummon() || attacker.isPet()))
		{
			me.addDamageHate(attacker.getActingPlayer(), 0, me.getTemplate().getParameter("searchingMaster", false) ? me.getAggroList().getHating(attacker) : 1);
		}
		
		if (getIntention() != ATTACK)
		{
			setIntention(CtrlIntention.ATTACK, attacker, false);
		}
		else if (me.getAggroList().getMostHated() != getAttackTarget())
		{
			setIntention(CtrlIntention.ATTACK, attacker, false);
		}
		notifyFriends(attacker, damage);
		checkUD(attacker);
		super.onEvtAttacked(attacker, damage);
	}
	
	protected void notifyFriends(Creature attacker, int damage)
	{
		if (attacker.isInvisible())
		{
			return;
		}
		
		final var actor = getActiveChar();
		if (actor == null || actor.isDead() || actor.isInBlockList(attacker))
		{
			return;
		}
		
		if (!_canNotifyFriend)
		{
			_lastFactionNotifyTime = System.currentTimeMillis();
			_canNotifyFriend = true;
			return;
		}
		
		if ((System.currentTimeMillis() - _lastFactionNotifyTime) > _minFactionNotifyInterval)
		{
			_lastFactionNotifyTime = System.currentTimeMillis();

			if (actor.isMinion())
			{
				final var master = actor.getLeader();
				if (master != null)
				{
					if (!master.isDead() && master.isVisible())
					{
						master.getAI().notifyEvent(CtrlEvent.EVT_CLAN_ATTACKED, attacker, damage);
					}
					
					final var minionList = master.getMinionList();
					if (minionList != null)
					{
						for (final var minion : minionList.getAliveMinions())
						{
							if (minion != actor && !minion.isDead() && minion.isVisible())
							{
								minion.getAI().notifyEvent(CtrlEvent.EVT_CLAN_ATTACKED, attacker, damage);
							}
						}
					}
				}
			}
			
			final var minionList = actor.getMinionList();
			if ((minionList != null) && minionList.hasAliveMinions())
			{
				for (final var minion : minionList.getAliveMinions())
				{
					if (minion != null && !minion.isDead() && minion.isVisible())
					{
						minion.getAI().notifyEvent(CtrlEvent.EVT_CLAN_ATTACKED, attacker, damage);
					}
				}
			}
			
			for (final var npc : activeFactionTargets(true))
			{
				if (npc == null || npc.isDead())
				{
					continue;
				}
				npc.getAI().notifyEvent(CtrlEvent.EVT_CLAN_ATTACKED, attacker, damage);
				if (attacker.isPlayable())
				{
					final List<Quest> quests = npc.getTemplate().getEventQuests(QuestEventType.ON_FACTION_CALL);
					if ((quests != null) && !quests.isEmpty())
					{
						final var player = attacker.getActingPlayer();
						final var isSummon = attacker.isSummon();
						for (final var quest : quests)
						{
							quest.notifyFactionCall(npc, getActiveChar(), player, isSummon);
						}
					}
				}
			}
		}
	}
	
	protected List<Npc> activeFactionTargets(boolean checkChampion)
	{
		final var actor = getActiveChar();
		if (actor == null || actor.isDead())
		{
			return Collections.emptyList();
		}
		
		final List<Npc> npcFriends = new LazyArrayList<>();
		for (final var npc : World.getInstance().getAroundNpc(actor))
		{
			if (npc != null && npc.isMonster())
			{
				if (!npc.isDead() && !npc.isAttackingNow())
				{
					final var champion = npc.getChampionTemplate();
					final var isInFaction = npc.isInFaction(actor) && !actor.getFaction().isNone();
					final var isChampFaction = checkChampion && (champion != null && npc.getChampionTemplate().isSocialAggro && !npc.getChampionTemplate().isPassive && actor.getId() == npc.getId());
					if (isInFaction || isChampFaction)
					{
						if (champion != null && npc.getChampionTemplate().isPassive)
						{
							continue;
						}
						
						if (((isInFaction && npc.isInRange(actor, (long) (npc.getFaction().getRange() + actor.getColRadius()))) || isChampFaction) && GeoEngine.getInstance().canSeeTarget(npc, actor))
						{
							npcFriends.add(npc);
						}
					}
				}
			}
		}
		return npcFriends;
	}
	
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		final var me = getActiveChar();
		
		if (target != null)
		{
			me.addDamageHate(target, 0, aggro);
			
			if ((aggro > 0) && (target.isSummon() || target.isPet()))
			{
				me.addDamageHate(target.getActingPlayer(), 0, me.getTemplate().getParameter("searchingMaster", false) ? aggro : 1);
			}

			if (getIntention() != CtrlIntention.ATTACK)
			{
				if (!me.isRunning())
				{
					if (me.isEkimusFood())
					{
						me.setWalking();
					}
					else
					{
						me.setRunning();
					}
				}
				setIntention(CtrlIntention.ATTACK, target, false);
			}
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		_attackTimeout = Long.MAX_VALUE;
		super.onIntentionActive();
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final var actor = getActiveChar();

		final var transformer = actor.getTemplate().getParameter("transformOnDead", 0);
		final var chance = actor.getTemplate().getParameter("transformChance", 100);
		if ((transformer > 0) && Rnd.chance(chance))
		{
			final var npc = NpcUtils.spawnSingle(transformer, actor.getLocation(), actor.getReflectionId());
			if ((killer != null) && killer.isPlayable())
			{
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, killer, 100);
				killer.setTarget(npc);
			}
		}
		_lifeTime = 0;
		super.onEvtDead(killer);
	}

	@Override
	protected void onEvtSpawn()
	{
		_defFlag = 0;
		setGlobalAggro(-20);
	}
	
	protected boolean createNewTask()
	{
		return false;
	}
	
	protected boolean defaultFightTask()
	{
		final var actor = getActiveChar();
		if (actor.isDead() || actor.isMuted())
		{
			return false;
		}

		final var target = actor.getAggroList().getMostHated();
		if (target == null)
		{
			return false;
		}
		
		if (applyUD())
		{
			return true;
		}
		
		final var distance = actor.getDistance(target);
		final var targetHp = target.getCurrentHpPercents();
		final var actorHp = actor.getCurrentHpPercents();

		final Skill[] dam = Rnd.chance(getRateDAM()) ? selectUsableSkills(target, distance, _damSkills) : null;
		final Skill[] dot = Rnd.chance(getRateDOT()) ? selectUsableSkills(target, distance, _dotSkills) : null;
		final Skill[] debuff = targetHp > 10 ? Rnd.chance(getRateDEBUFF()) ? selectUsableSkills(target, distance, _debuffSkills) : null : null;
		final Skill[] stun = Rnd.chance(getRateSTUN()) ? selectUsableSkills(target, distance, _stunSkills) : null;
		final Skill[] heal = actorHp < 50 ? Rnd.chance(getRateHEAL()) ? selectUsableSkills(actor, 0, _healSkills) : null : null;
		final Skill[] buff = Rnd.chance(getRateBUFF()) ? selectUsableSkills(actor, 0, _buffSkills) : null;
		final Skill[] suicide = actorHp < 30 ? Rnd.chance(getRateSuicide()) ? selectUsableSkills(actor, 0, _suicideSkills) : null : null;
		final Skill[] res = Rnd.chance(getRateHEAL()) ? selectUsableSkills(target, distance, _resSkills) : null;

		final RndSelector<Skill[]> rnd = new RndSelector<>();
		if (!actor.isMuted())
		{
			rnd.add(null, getRatePHYS());
		}
		rnd.add(dam, getRateDAM());
		rnd.add(dot, getRateDOT());
		rnd.add(debuff, getRateDEBUFF());
		rnd.add(heal, getRateHEAL());
		rnd.add(buff, getRateBUFF());
		rnd.add(stun, getRateSTUN());
		rnd.add(suicide, getRateSuicide());
		rnd.add(res, getRateRes());

		final Skill[] selected = rnd.select();
		if (selected != null)
		{
			if ((selected == dam) || (selected == dot))
			{
				return checkOtherSkills(actor, target, selectTopSkillByDamage(actor, target, distance, selected));
			}
			
			if (selected == debuff || selected == stun)
			{
				return checkOtherSkills(actor, target, selectTopSkillByDebuff(actor, target, distance, selected));
			}

			if (selected == buff)
			{
				return checkBuffSkills(actor, target, selectTopSkillByBuff(actor, selected));
			}

			if (selected == heal)
			{
				return checkHealSkills(actor, target, selectTopSkillByHeal(actor, selected));
			}

			if (selected == suicide)
			{
				return checkSuisideSkills(actor, target, selectTopSkillByDamage(actor, target, distance, selected));
			}

			if (selected == res)
			{
				return checkResSkills(actor, target, selectTopSkillByDamage(actor, target, distance, selected));
			}
		}
		return false;
	}
	
	protected Skill[] selectUsableSkills(Creature target, double distance, Skill[] skills)
	{
		if (skills == null || skills.length == 0 || target == null)
		{
			return null;
		}

		Skill[] ret = null;
		int usable = 0;
		
		for (final var skill : skills)
		{
			if (canUseSkill(skill, target, distance))
			{
				if (ret == null)
				{
					ret = new Skill[skills.length];
				}
				ret[usable++] = skill;
			}
		}
		
		if (ret == null || usable == skills.length)
		{
			return ret;
		}
		
		if (usable == 0)
		{
			return null;
		}
		
		ret = Arrays.copyOf(ret, usable);
		return ret;
	}
	
	protected boolean checkResSkills(Attackable npc, Creature target, Skill skill)
	{
		if (npc.isMinion())
		{
			final var leader = npc.getLeader();
			if ((leader != null) && leader.isDead())
			{
				if (skill.getTargetType() == TargetType.SELF)
				{
					return false;
				}
				if (!checkSkillCastConditions(skill))
				{
					return false;
				}
				if (!Util.checkIfInRange((int) (skill.getCastRange() + leader.getColRadius()), npc, leader, false) && !isParty(skill) && !npc.isMovementDisabled())
				{
					moveToPawn(leader, (int) (skill.getCastRange() + leader.getColRadius()));
					return true;
				}
				if (GeoEngine.getInstance().canSeeTarget(npc, leader))
				{
					clientStopMoving(null);
					npc.setTarget(leader);
					npc.doCast(skill);
					return true;
				}
			}
		}
		
		if (!checkSkillCastConditions(skill))
		{
			return false;
		}
		if (skill.getTargetType() == TargetType.ONE)
		{
			for (final var obj : World.getInstance().getAroundCharacters(npc, (int) (skill.getCastRange() + npc.getColRadius()), 200))
			{
				if (!(obj instanceof Attackable) || !obj.isDead())
				{
					continue;
				}
					
				final var targets = ((Attackable) obj);
				if ((!npc.getFaction().isNone()) && !npc.isInFaction(targets))
				{
					continue;
				}
					
				if (Rnd.get(100) < 10)
				{
					if (GeoEngine.getInstance().canSeeTarget(npc, targets))
					{
						clientStopMoving(null);
						npc.setTarget(obj);
						npc.doCast(skill);
						return true;
					}
				}
			}
		}
			
		if (isParty(skill))
		{
			clientStopMoving(null);
			final GameObject newTarget = getAttackTarget();
			npc.setTarget(npc);
			npc.doCast(skill);
			npc.setTarget(newTarget);
			return true;
		}
		return false;
	}

	protected boolean checkOtherSkills(Attackable npc, Creature target, Skill skill)
	{
		return cast(skill);
	}

	protected boolean checkSuisideSkills(Attackable npc, Creature target, Skill skill)
	{
		if (Util.checkIfInRange(skill.getAffectRange(), getActiveChar(), target, false))
		{
			return cast(skill);
		}
		return false;
	}
	
	protected boolean checkBuffSkills(Attackable npc, Creature target, Skill skill)
	{
		if (_lastBuffInterval < System.currentTimeMillis())
		{
			if (cast(skill))
			{
				_lastBuffInterval = System.currentTimeMillis() + 10000L;
				return true;
			}
		}
		return false;
	}

	protected boolean checkHealSkills(Attackable npc, Creature target, Skill skill)
	{
		var percentage = (npc.getCurrentHp() / npc.getMaxHp()) * 100;
		if (npc.isMinion())
		{
			final var leader = npc.getLeader();
			if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
			{
				if (skill.getTargetType() == TargetType.SELF)
				{
					return false;
				}
					
				if (!checkSkillCastConditions(skill))
				{
					return false;
				}
					
				if (!Util.checkIfInRange((int) (skill.getCastRange() + leader.getColRadius()), npc, leader, false) && !isParty(skill) && !npc.isMovementDisabled())
				{
					moveToPawn(leader, (int) (skill.getCastRange() + leader.getColRadius()));
					return true;
				}
					
				if (GeoEngine.getInstance().canSeeTarget(npc, leader))
				{
					clientStopMoving(null);
					npc.setTarget(leader);
					npc.doCast(skill);
					return true;
				}
			}
		}
		if (Rnd.get(100) < ((100 - percentage) / 3))
		{
			if (!checkSkillCastConditions(skill))
			{
				return false;
			}
			clientStopMoving(null);
			npc.setTarget(npc);
			npc.doCast(skill);
			return true;
		}
		
		if (!checkSkillCastConditions(skill))
		{
			return false;
		}
			
		if (skill.getTargetType() == TargetType.ONE)
		{
			for (final var obj : World.getInstance().getAroundCharacters(npc, (int) (skill.getCastRange() + npc.getColRadius()), 200))
			{
				if (!(obj instanceof Attackable) || obj.isDead())
				{
					continue;
				}
					
				final var targets = ((Attackable) obj);
				if ((!npc.getFaction().isNone()) && !npc.isInFaction(targets))
				{
					continue;
				}
				percentage = (targets.getCurrentHp() / targets.getMaxHp()) * 100;
				if (Rnd.get(100) < ((100 - percentage) / 10))
				{
					if (GeoEngine.getInstance().canSeeTarget(npc, targets))
					{
						clientStopMoving(null);
						npc.setTarget(obj);
						npc.doCast(skill);
						return true;
					}
				}
			}
		}
		
		if (isParty(skill))
		{
			clientStopMoving(null);
			npc.doCast(skill);
			return true;
		}
		return false;
	}

	protected boolean defaultThinkBuff(int rateSelf)
	{
		return defaultThinkBuff(rateSelf, 0);
	}

	protected boolean defaultThinkBuff(int rateSelf, int rateFriends)
	{
		final var actor = getActiveChar();
		if (actor.isDead() || _lastBuffInterval > System.currentTimeMillis())
		{
			return false;
		}
		
		if (Rnd.chance(rateSelf))
		{
			final var actorHp = actor.getCurrentHpPercents();
			
			final Skill[] skills = actorHp < 50 ? selectUsableSkills(actor, 0, _healSkills) : selectUsableSkills(actor, 0, _buffSkills);
			if ((skills == null) || (skills.length == 0))
			{
				return false;
			}
			
			final var skill = skills[Rnd.get(skills.length)];
			if (cast(skill))
			{
				_lastBuffInterval = System.currentTimeMillis() + 10000L;
				return true;
			}
		}
		
		if (Rnd.chance(rateFriends))
		{
			for (final var npc : activeFactionTargets(false))
			{
				final double targetHp = npc.getCurrentHpPercents();
				final Skill[] skills = targetHp < 50 ? selectUsableSkills(actor, 0, _healSkills) : selectUsableSkills(actor, 0, _buffSkills);
				if ((skills == null) || (skills.length == 0))
				{
					continue;
				}
				
				final var skill = skills[Rnd.get(skills.length)];
				if (cast(skill))
				{
					_lastBuffInterval = System.currentTimeMillis() + 10000L;
					return true;
				}
			}
		}
		return false;
	}

	public Attackable getActiveChar()
	{
		return (Attackable) _actor;
	}

	protected int getRatePHYS()
	{
		return 100;
	}
	
	protected int getRateDOT()
	{
		return 0;
	}
	
	protected int getRateDEBUFF()
	{
		return 0;
	}
	
	protected int getRateDAM()
	{
		return 0;
	}
	
	protected int getRateSTUN()
	{
		return 0;
	}
	
	protected int getRateBUFF()
	{
		return 0;
	}
	
	protected int getRateHEAL()
	{
		return 0;
	}

	protected int getRateSuicide()
	{
		return 0;
	}

	protected int getRateRes()
	{
		return 0;
	}
	
	protected int getRateDodge()
	{
		return 0;
	}
	
	protected void returnHome()
	{
		returnHome(true, Config.ALWAYS_TELEPORT_HOME);
	}
	
	protected void teleportHome()
	{
		returnHome(true, true);
	}
	
	protected void returnHome(boolean clearAggro, boolean teleport)
	{
		final var actor = getActiveChar();
		if (actor.isDead() || actor.isMovementDisabled())
		{
			return;
		}
		
		if (actor.isMinion())
		{
			final var leader = (MonsterInstance) actor.getLeader();
			if (leader != null)
			{
				if (leader.getSpawnedLoc() == null)
				{
					actor.deleteMe();
				}
				else
				{
					actor.onDecay();
				}
				return;
			}
		}
		
		final var isDestructionRaid = actor.getTemplate().getParameter("isDestructionBoss", false);
		final var sloc = actor.getSpawnedLoc();
		
		clientStopMoving(null);
		if (clearAggro)
		{
			actor.clearAggroList(true);
		}
		_pathfindFails = -1;
		_attackTimeout = Long.MAX_VALUE;
		setAttackTarget(null);
		
		changeIntention(CtrlIntention.ACTIVE, null, null);
		
		final var teleDistance = actor.getDistance(sloc.getX(), sloc.getY()) > 1000;
		if (teleport && teleDistance && !isDestructionRaid)
		{
			actor.broadcastPacket(new MagicSkillUse(actor, actor, 2036, 1, 500, 0));
			actor.teleToLocation(sloc.getX(), sloc.getY(), GeoEngine.getInstance().getHeight(sloc), true, actor.getReflection());
		}
		else
		{
			if (!clearAggro)
			{
				actor.setRunning();
			}
			else
			{
				actor.setWalking();
			}
			moveTo(sloc, 0);
		}

		if (isDestructionRaid)
		{
			actor.setCurrentHp(actor.getMaxHp());
			actor.setCurrentMp(actor.getMaxMp());
		}
	}
	
	protected long getAttackTimeout()
	{
		return _attackTimeout;
	}
	
	protected static Skill selectTopSkillByDamage(Creature actor, Creature target, double distance, Skill[] skills)
	{
		if (skills == null || skills.length == 0)
		{
			return null;
		}
		
		if (skills.length == 1)
		{
			return skills[0];
		}
		
		Skill oneTargetSkill = null;
		for (final Skill skill : skills)
		{
			if (skill.oneTarget())
			{
				if (oneTargetSkill == null || skill.getCastRange() >= distance && (distance / oneTargetSkill.getCastRange()) < (distance / skill.getCastRange()))
				{
					oneTargetSkill = skill;
				}
			}
		}
		
		final RndSelector<Skill> rnd = new RndSelector<>(skills.length);
		
		double weight;
		for (final var skill : skills)
		{
			if (!skill.oneTarget())
			{
				weight = skill.getSimpleDamage(actor, target) / 10 + (distance / skill.getCastRange() * 100);
				if (weight < 1.)
				{
					weight = 1.;
				}
				rnd.add(skill, (int) weight);
			}
		}
		
		final var aoeSkill = rnd.select();
		
		if (aoeSkill == null)
		{
			return oneTargetSkill;
		}
		
		if (oneTargetSkill == null)
		{
			return aoeSkill;
		}
		
		if (Rnd.chance(90))
		{
			return oneTargetSkill;
		}
		else
		{
			return aoeSkill;
		}
	}
	
	protected static Skill selectTopSkillByDebuff(Creature actor, Creature target, double distance, Skill[] skills)
	{
		if (skills == null || skills.length == 0)
		{
			return null;
		}
		
		if (skills.length == 1)
		{
			return skills[0];
		}
		
		final RndSelector<Skill> rnd = new RndSelector<>(skills.length);
		double weight;
		for (final var skill : skills)
		{
			if (target.getFirstEffect(skill.getId()) != null)
			{
				continue;
			}
			if ((weight = 100. * skill.getAOECastRange() / distance) <= 0)
			{
				weight = 1;
			}
			rnd.add(skill, (int) weight);
		}
		return rnd.select();
	}
	
	protected static Skill selectTopSkillByBuff(Creature target, Skill[] skills)
	{
		if (skills == null || skills.length == 0)
		{
			return null;
		}
		
		if (skills.length == 1)
		{
			return skills[0];
		}
		
		final RndSelector<Skill> rnd = new RndSelector<>(skills.length);
		double weight;
		for (final var skill : skills)
		{
			if (target.getFirstEffect(skill.getId()) != null)
			{
				continue;
			}
			if ((weight = skill.getPower()) <= 0)
			{
				weight = 1;
			}
			rnd.add(skill, (int) weight);
		}
		return rnd.select();
	}
	
	protected static Skill selectTopSkillByHeal(Creature target, Skill[] skills)
	{
		if (skills == null || skills.length == 0)
		{
			return null;
		}
		
		final var hpReduced = target.getMaxHp() - target.getCurrentHp();
		if (hpReduced < 1)
		{
			return null;
		}
		
		if (skills.length == 1)
		{
			return skills[0];
		}
		
		final RndSelector<Skill> rnd = new RndSelector<>(skills.length);
		double weight;
		for (final var skill : skills)
		{
			if ((weight = Math.abs(skill.getPower() - hpReduced)) <= 0)
			{
				weight = 1;
			}
			rnd.add(skill, (int) weight);
		}
		return rnd.select();
	}
	
	protected void addDesiredSkill(Map<Skill, Integer> skillMap, Creature target, double distance, Skill[] skills)
	{
		if (skills == null || skills.length == 0 || target == null)
		{
			return;
		}
		
		for (final var sk : skills)
		{
			addDesiredSkill(skillMap, target, distance, sk);
		}
	}
	
	protected void addDesiredSkill(Map<Skill, Integer> skillMap, Creature target, double distance, Skill skill)
	{
		if (skill == null || target == null || !canUseSkill(skill, target))
		{
			return;
		}
		int weight = (int) -Math.abs(skill.getCastRange() - distance);
		if (skill.getCastRange() >= distance)
		{
			weight += 1000000;
		}
		else if (skill.isNotTargetAoE() && skill.getTargetList(getActor(), false, target).length == 0)
		{
			return;
		}
		skillMap.put(skill, weight);
	}
	
	protected void addDesiredHeal(Map<Skill, Integer> skillMap, Skill[] skills)
	{
		if (skills == null || skills.length == 0)
		{
			return;
		}
		
		final var actor = getActiveChar();
		if (actor != null)
		{
			final var hpReduced = actor.getMaxHp() - actor.getCurrentHp();
			final var hpPercent = actor.getCurrentHpPercents();
			if (hpReduced < 1)
			{
				return;
			}
			int weight;
			for (final var sk : skills)
			{
				if (canUseSkill(sk, actor) && sk.getPower() <= hpReduced)
				{
					weight = (int) sk.getPower();
					if (hpPercent < 50)
					{
						weight += 1000000;
					}
					skillMap.put(sk, weight);
				}
			}
		}
	}
	
	protected void addDesiredBuff(Map<Skill, Integer> skillMap, Skill[] skills)
	{
		if (skills == null || skills.length == 0)
		{
			return;
		}
		
		final var actor = getActiveChar();
		if (actor != null)
		{
			for (final var sk : skills)
			{
				if (canUseSkill(sk, actor))
				{
					skillMap.put(sk, 1000000);
				}
			}
		}
	}

	public void addSkill(Skill skill)
	{
		if (skill.getTargetType() == TargetType.NONE || skill.getSkillType() == SkillType.NOTDONE || !skill.isActive())
		{
			return;
		}

		if (skill.isSuicideAttack())
		{
			_suicideSkills = ArrayUtils.add(_suicideSkills, skill);
		}
		else
		{
			switch (skill.getSkillType())
			{
				case PDAM :
				case MANADAM :
				case MDAM :
				case DRAIN :
				case CHARGEDAM :
				case FATAL :
				case DEATHLINK :
				case CPDAMPERCENT :
					_damSkills = ArrayUtils.add(_damSkills, skill);
					break;
				case DOT :
				case MDOT :
				case POISON :
				case BLEED :
					_dotSkills = ArrayUtils.add(_dotSkills, skill);
					break;
				case DEBUFF :
				case SLEEP :
				case ROOT :
				case PARALYZE :
				case MUTE :
					_debuffSkills = ArrayUtils.add(_debuffSkills, skill);
					break;
				case BUFF :
					_buffSkills = ArrayUtils.add(_buffSkills, skill);
					break;
				case RESURRECT :
					_resSkills = ArrayUtils.add(_resSkills, skill);
					break;
				case STUN :
					_stunSkills = ArrayUtils.add(_stunSkills, skill);
					break;
				default :
					if (skill.hasEffectType(EffectType.CANCEL, EffectType.CANCEL_ALL, EffectType.CANCEL_BY_SLOT, EffectType.MUTE, EffectType.FEAR, EffectType.SLEEP, EffectType.ROOT, EffectType.PARALYZE, EffectType.NEGATE))
					{
						_debuffSkills = ArrayUtils.add(_debuffSkills, skill);
					}
					else if (skill.hasEffectType(EffectType.HEAL, EffectType.HEAL_OVER_TIME, EffectType.HEAL_PERCENT))
					{
						_healSkills = ArrayUtils.add(_healSkills, skill);
					}
					else if (skill.hasEffectType(EffectType.STUN))
					{
						_stunSkills = ArrayUtils.add(_stunSkills, skill);
					}
					else if (skill.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT))
					{
						_dotSkills = ArrayUtils.add(_dotSkills, skill);
					}
					break;
			}
		}
	}
	
	protected Skill selectTopSkill(Map<Skill, Integer> skillMap)
	{
		if (skillMap == null || skillMap.isEmpty())
		{
			return null;
		}
		
		int nWeight, topWeight = Integer.MIN_VALUE;
		for (final var next : skillMap.keySet())
		{
			if ((nWeight = skillMap.get(next)) > topWeight)
			{
				topWeight = nWeight;
			}
		}
		
		if (topWeight == Integer.MIN_VALUE)
		{
			return null;
		}
		
		final Skill[] skills = new Skill[skillMap.size()];
		nWeight = 0;
		for (final Map.Entry<Skill, Integer> e : skillMap.entrySet())
		{
			if (e.getValue() < topWeight)
			{
				continue;
			}
			skills[nWeight++] = e.getKey();
		}
		return skills[Rnd.get(nWeight)];
	}
	
	protected boolean canUseSkill(Skill sk, Creature target)
	{
		return canUseSkill(sk, target, 0);
	}
	
	protected boolean canUseSkill(Skill skill, Creature target, double distance)
	{
		final var actor = getActiveChar();
		if (skill == null)
		{
			return false;
		}
		
		if (skill.getTargetType() == TargetType.SELF && target != actor)
		{
			return false;
		}
		
		final var castRange = skill.getCastRange();
		if (castRange <= 200 && distance > 200)
		{
			return false;
		}
		
		if (actor.isSkillDisabled(skill))
		{
			return false;
		}
		
		if (target.getFirstEffect(skill.getId()) != null)
		{
			return false;
		}
		return true;
	}
	
	protected boolean maybeMoveToHome()
	{
		final var actor = getActiveChar();
		if (actor.isDead())
		{
			return false;
		}
		
		if (actor.isMinion())
		{
			final var master = actor.getLeader();
			if (master != null && master.isAlikeDead())
			{
				if (master.getSpawnedLoc() == null)
				{
					actor.deleteMe();
				}
				else
				{
					actor.onDecay();
				}
				return false;
			}
		}
		
		final var isTimeOut = _waitTimeOut == 0;
		
		if (!Rnd.chance(isTimeOut ? 100 : _waitTimeOutTime > 0 ? 50 : Config.NPC_AI_RNDWALK_CHANCE))
		{
			return false;
		}
		
		final var sloc = actor.getSpawnedLoc();
		if (sloc == null)
		{
			return false;
		}
		
		final var isInRange = actor.isInRangeZ(sloc, Config.MAX_DRIFT_RANGE);
		
		if (_waitTimeOutTime > 0)
		{
			if (_waitTimeOutTime < System.currentTimeMillis() && !actor.isInsideRadius(sloc.getX(), sloc.getY(), sloc.getZ(), (Config.MAX_DRIFT_RANGE * 2), true, false))
			{
				_waitTimeOutTime = 0;
				teleportHome();
				return true;
			}
			
			if (actor.isInsideRadius(sloc.getX(), sloc.getY(), sloc.getZ(), Config.MAX_DRIFT_RANGE, true, false))
			{
				_waitTimeOutTime = 0;
			}
			else
			{
				if (!actor.isMoving())
				{
					final var pos = Location.findPointToStay(actor, sloc, 0, (Config.MAX_DRIFT_RANGE / 2), false);
					if (pos != null)
					{
						actor.setWalking();
						moveTo(pos);
					}
				}
			}
			return true;
		}
		
		if ((actor.getDistance(sloc.getX(), sloc.getY()) > 2000) && !isInRange && !isTimeOut)
		{
			teleportHome();
		}
		else
		{
			final var pos = Location.findPointToStay(actor, sloc, 0, Config.MAX_DRIFT_RANGE, false);
			if (pos != null)
			{
				if (!actor.isMoving())
				{
					actor.setWalking();
					moveTo(pos);
				}
				
				if (isTimeOut)
				{
					_waitTimeOut = -1;
					_waitTimeOutTime = System.currentTimeMillis() + _waitTimeOutLimit;
				}
			}
			else
			{
				teleportHome();
			}
		}
		return true;
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
	
	public void setNotifyFriend(boolean value)
	{
		_canNotifyFriend = value;
	}
	
	public boolean isGlobalAI()
	{
		return getActiveChar().isGlobalAI();
	}
	
	@Override
	public void enableAI()
	{
		if (getActiveChar().getAI().getIntention() == CtrlIntention.IDLE)
		{
			getActiveChar().getAI().changeIntention(CtrlIntention.ACTIVE, null, null);
		}
	}
	
	public void checkUD(Creature attacker)
	{
		if (_defRate == 0 || getActiveChar() == null || attacker == null || _defInterval > System.currentTimeMillis())
		{
			return;
		}
		
		_defInterval = System.currentTimeMillis() + 5000L;
		
		if (getActiveChar().getDistance(attacker) > 150)
		{
			if (_defFlag == 0 || _defFlag == 2)
			{
				if (Rnd.chance(_defRate) && canUseSkill(_defSkill, getActiveChar(), 0))
				{
					_defFlag = 1;
				}
			}
		}
		else
		{
			if (_defFlag == 2 || _defFlag == 1)
			{
				getActiveChar().stopSkillEffects(_defSkill.getId());
				_defFlag = 0;
			}
		}
	}
	
	private boolean applyUD()
	{
		if (_defRate == 0 || _defFlag == 0 || getActiveChar() == null)
		{
			return false;
		}
		
		if (_defFlag == 1)
		{
			clientStopMoving(null);
			_defSkill.getEffects(getActiveChar(), getActiveChar(), false);
			_defFlag = 2;
			return true;
		}
		return false;
	}
	
	private void setIsInRandomAnimation(long time)
	{
		_randomAnimationEnd = System.currentTimeMillis() + time;
	}
	
	private boolean randomAnimation()
	{
		final var actor = getActiveChar();
		
		if (actor.isAlikeDead() || !actor.hasRandomAnimation() || !actor.isRandomAnimationEnabled() || (actor.getAI() instanceof Corpse) || _randomAnimationEnd > System.currentTimeMillis())
		{
			return false;
		}
		
		if (!actor.isActionsDisabled() && actor.isVisible() && !actor.isMoving() && !actor.isInCombat() && Rnd.chance(5))
		{
			setIsInRandomAnimation(Rnd.get(Config.MIN_NPC_ANIMATION, Config.MAX_NPC_ANIMATION) * 1000L);
			actor.onRandomAnimation();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isWalkingToHome()
	{
		return _waitTimeOutTime > 0;
	}
	
	@Override
	public void setLifeTime(int lifeTime)
	{
		if (lifeTime > 0)
		{
			_lifeTime = System.currentTimeMillis() + (lifeTime * 60000L);
		}
	}
	
	@Override
	protected void setAttackTarget(Creature target)
	{
		final var actor = getActiveChar();
		if (target != null && target.isPlayer() && !actor.isDead())
		{
			if (getAttackTarget() != target)
			{
				actor.addToTargetList(target.getActingPlayer());
			}
		}
		super.setAttackTarget(target);
	}
}
