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
import static l2e.gameserver.ai.model.CtrlIntention.IDLE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.EffectPointInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillLaunched;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.taskmanager.AiTaskManager;

public class EffectPointAI extends CharacterAI implements Runnable
{
	private Future<?> _aiTask;
	private boolean _thinking = false;
	private int _tick = 0;
	
	public EffectPointAI(EffectPointInstance character)
	{
		super(character);
	}
	
	@Override
	protected synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if (intention == IDLE || intention == ACTIVE)
		{
			super.changeIntention(intention, arg0, arg1);
			startAITask();
		}
	}
	
	@Override
	public void run()
	{
		onEvtThink();
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
	protected void onEvtThink()
	{
		final var actor = getActiveChar();
		if (_thinking || actor == null)
		{
			return;
		}
		
		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case ACTIVE :
				case IDLE :
					thinkActive();
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	public void enableAI()
	{
		if (getActiveChar().getAI().getIntention() == CtrlIntention.IDLE)
		{
			changeIntention(CtrlIntention.ACTIVE, null, null);
		}
	}
	
	private EffectPointInstance getActiveChar()
	{
		return (EffectPointInstance) _actor;
	}
	
	protected boolean thinkActive()
	{
		final var actor = getActiveChar();
		var skill = actor.getSkill();
		final var owner = actor.getActingPlayer();
		if (owner == null)
		{
			return true;
		}
		final var isSignetNoise = actor.getMainSkill().getId() == 455;
		final var isAntiSummon = actor.getMainSkill().getId() == 1424;
		final var isCastTime = actor.isCastTime();
		final int mpConsume = actor.getMainSkill().getMpConsume();
		if (mpConsume > owner.getCurrentMp())
		{
			owner.sendPacket(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
			return true;
		}
		
		if (isCastTime || isSignetNoise)
		{
			skill = actor.getMainSkill();
			_tick++;
			if (_tick > 1)
			{
				_tick = 0;
				return true;
			}
		}
		
		if (skill == null)
		{
			return true;
		}
		
		if (isCastTime)
		{
			owner.reduceCurrentMp(mpConsume);
		}
		
		final List<Creature> targets = new ArrayList<>();
		for (final var cha : World.getInstance().getAroundCharacters(actor, actor.getMainSkill().getAffectRange(), 300))
		{
			if (((cha == null) || cha.isAlikeDead()) || ((cha == owner) && skill.isOffensive()) || (!skill.isOffensive() && !cha.isPlayable()))
			{
				continue;
			}
			
			if ((actor.getMainSkill().getAffectLimit() > 0) && (targets.size() >= actor.getMainSkill().getAffectLimit()))
			{
				break;
			}
			
			if (cha.isPlayable())
			{
				final var player = cha.getActingPlayer();
				if (player == null)
				{
					continue;
				}
				
				if (skill.isOffensive() && !Skill.checkForAreaOffensiveSkills(owner, player, skill, actor.isInSrcInArena(), owner))
				{
					continue;
				}
				
				if ((!skill.isOffensive() && !player.isFriend(owner, false)) || (!isCastTime && cha.getFirstEffect(skill.getId()) != null))
				{
					continue;
				}
				
				if (cha.isPlayer() && skill.isOffensive() && owner.isFriend(cha.getActingPlayer()))
				{
					continue;
				}
				
				if (isAntiSummon && player.hasSummon())
				{
					player.getSummon().unSummon(player);
				}
			}
			
			if (!isCastTime && !isSignetNoise)
			{
				actor.broadcastPacket(new MagicSkillUse(actor, cha, skill.getId(), skill.getLevel(), 0, 0));
			}
			targets.add(cha);
		}
		
		if (!targets.isEmpty())
		{
			if (!isCastTime)
			{
				if (isSignetNoise)
				{
					targets.stream().filter(p -> p != null).forEach(pm -> pm.getEffectList().stopAllDances());
				}
				else
				{
					owner.callSkill(skill, targets.toArray(new Creature[targets.size()]));
				}
			}
			else
			{
				owner.rechargeShots(actor.getMainSkill().useSoulShot(), actor.getMainSkill().useSpiritShot());
				final boolean sps = actor.getMainSkill().useSpiritShot() && owner.isChargedShot(ShotType.SPIRITSHOTS);
				final boolean bss = actor.getMainSkill().useSpiritShot() && owner.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
				
				owner.broadcastPacket(new MagicSkillLaunched(owner, actor.getMainSkill().getId(), actor.getMainSkill().getLevel(), targets.toArray(new Creature[targets.size()])));
				for (final var target : targets)
				{
					if (target == owner)
					{
						continue;
					}
					
					final boolean mcrit = Formulas.calcMCrit(owner.getMCriticalHit(target, actor.getMainSkill()));
					final byte shld = Formulas.calcShldUse(owner, target, actor.getMainSkill());
					final int mdam = (int) Formulas.calcMagicDam(owner, target, actor.getMainSkill(), shld, sps, bss, mcrit);
					
					if (target.isPlayable())
					{
						owner.updatePvPStatus(target);
					}
					
					if (target.isSummon())
					{
						target.broadcastStatusUpdate();
					}
					
					if (mdam > 0)
					{
						if (!target.isRaid() && Formulas.calcAtkBreak(target, mcrit))
						{
							target.breakAttack();
							target.breakCast();
						}
						owner.sendDamageMessage(target, mdam, actor.getMainSkill(), mcrit, false, false);
						target.reduceCurrentHp(mdam, owner, actor.getMainSkill());
					}
					target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, owner, mdam);
				}
				owner.setChargedShot(bss ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
			}
		}
		return true;
	}

	@Override
	protected void onIntentionRest()
	{
	}

	@Override
	protected void onIntentionAttack(Creature target, boolean shift)
	{
	}

	@Override
	protected void onIntentionCast(Skill skill, GameObject target)
	{
	}

	@Override
	protected void onIntentionMoveTo(Location destination, int offset)
	{
	}

	@Override
	protected void onIntentionFollow(Creature target)
	{
	}

	@Override
	protected void onIntentionPickUp(GameObject item)
	{
	}

	@Override
	protected void onIntentionInteract(GameObject object)
	{
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
	}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
	}

	@Override
	protected void onEvtStunned(Creature attacker)
	{
	}

	@Override
	protected void onEvtSleeping(Creature attacker)
	{
	}

	@Override
	protected void onEvtRooted(Creature attacker)
	{
	}

	@Override
	protected void onEvtReadyToAct()
	{
	}

	@Override
	protected void onEvtUserCmd(Object arg0, Object arg1)
	{
	}

	@Override
	protected void onEvtArrived()
	{
	}

	@Override
	protected void onEvtArrivedTarget()
	{
	}

	@Override
	protected void onEvtArrivedBlocked(Location blocked_at_loc)
	{
	}

	@Override
	protected void onEvtForgetObject(GameObject object)
	{
	}

	@Override
	protected void onEvtCancel()
	{
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
	}
}