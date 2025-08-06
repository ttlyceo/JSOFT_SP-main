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
package l2e.gameserver.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.status.CharStatus;
import l2e.gameserver.model.entity.events.tournaments.Tournament;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.olympiad.OlympiadGameManager;
import l2e.gameserver.model.olympiad.OlympiadGameTask;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.*;
import l2e.gameserver.network.serverpackets.Interface.ExAbnormalStatusUpdateFromTarget;

public class CharEffectList
{
	private static final Effect[] EMPTY_EFFECTS = new Effect[0];
	
	private List<Effect> _buffs;
	private List<Effect> _debuffs;
	private List<Effect> _passives;

	private Map<String, List<Effect>> _stackedEffects;

	private volatile boolean _hasBuffsRemovedOnAnyAction = false;
	private volatile boolean _hasBuffsRemovedOnDamage = false;
	private volatile boolean _hasDebuffsRemovedOnDamage = false;

	private boolean _queuesInitialized = false;
	private LinkedBlockingQueue<Effect> _addQueue;
	private LinkedBlockingQueue<Effect> _removeQueue;
	private final AtomicBoolean queueLock = new AtomicBoolean();
	private int _effectFlags;

	private boolean _partyOnly = false;
	private final Creature _owner;

	private Effect[] _effectCache;
	private volatile boolean _rebuildCache = true;
	private final Object _buildEffectLock = new Object();
	private Future<?> _effectIconsUpdate;
	private volatile Set<String> _blockedBuffSlots = null;
	private Effect _shortBuff = null;
	
	public CharEffectList(Creature owner)
	{
		_owner = owner;
	}

	public final Effect[] getAllEffects()
	{
		if (isEmpty())
		{
			return EMPTY_EFFECTS;
		}

		synchronized (_buildEffectLock)
		{
			if (!_rebuildCache)
			{
				return _effectCache;
			}

			_rebuildCache = false;

			final List<Effect> temp = new ArrayList<>();

			if (hasBuffs())
			{
				temp.addAll(getBuffs());
			}
			if (hasDebuffs())
			{
				temp.addAll(getDebuffs());
			}

			final Effect[] tempArray = new Effect[temp.size()];
			temp.toArray(tempArray);
			return (_effectCache = tempArray);
		}
	}

	public final Effect getFirstEffect(EffectType tp)
	{
		Effect effectNotInUse = null;

		if (hasBuffs())
		{
			for (final Effect e : getBuffs())
			{
				if (e == null)
				{
					continue;
				}

				if (e.getEffectType() == tp)
				{
					if (e.isInUse())
					{
						return e;
					}

					effectNotInUse = e;
				}
			}
		}

		if (effectNotInUse == null && hasDebuffs())
		{
			for (final Effect e : getDebuffs())
			{
				if (e == null)
				{
					continue;
				}

				if (e.getEffectType() == tp)
				{
					if (e.isInUse())
					{
						return e;
					}
					effectNotInUse = e;
				}
			}
		}
		return effectNotInUse;
	}
	
	public int getEffectTypeAmount(EffectType tp)
	{
		int i = 0;
		if (hasBuffs())
		{
			for (final Effect e : getBuffs())
			{
				if (e == null)
				{
					continue;
				}
				
				if (e.getEffectType() == tp)
				{
					i++;
				}
			}
		}
		return i;
	}

	public final Effect getFirstEffect(Skill skill)
	{
		Effect effectNotInUse = null;

		if (skill.isDebuff())
		{
			if (hasDebuffs())
			{
				for (final Effect e : getDebuffs())
				{
					if (e == null)
					{
						continue;
					}

					if (e.getSkill() == skill)
					{
						if (e.isInUse())
						{
							return e;
						}
						effectNotInUse = e;
					}
				}
			}
		}
		else
		{
			if (hasBuffs())
			{
				for (final Effect e : getBuffs())
				{
					if (e == null)
					{
						continue;
					}

					if (e.getSkill() == skill)
					{
						if (e.isInUse())
						{
							return e;
						}
						effectNotInUse = e;
					}
				}
			}
		}
		return effectNotInUse;
	}
	
	public final Effect getFirstAbnormalType(Skill skill)
	{
		if ((skill._effectTemplates == null) || (skill._effectTemplates.length < 1) || (skill._effectTemplates[0].abnormalType == null) || "none".equals(skill._effectTemplates[0].abnormalType))
		{
			return null;
		}
		
		final String stackType = skill._effectTemplates[0].abnormalType;
		
		if (hasBuffs())
		{
			for (final Effect e : getBuffs())
			{
				if (e == null)
				{
					continue;
				}
				
				final String abnormal = e.getAbnormalType();
				if (abnormal == null || "none".equals(abnormal) || !stackType.equalsIgnoreCase(abnormal))
				{
					continue;
				}
				
				if (e.getAbnormalLvl() > skill.getAbnormalLvl())
				{
					return e;
				}
			}
		}
		return null;
	}

	public final Effect getFirstEffect(int skillId)
	{
		Effect effectNotInUse = null;

		if (hasBuffs())
		{
			for (final Effect e : getBuffs())
			{
				if (e == null)
				{
					continue;
				}

				if (e.getSkill().getId() == skillId)
				{
					if (e.isInUse())
					{
						return e;
					}
					effectNotInUse = e;
				}
			}
		}

		if (effectNotInUse == null && hasDebuffs())
		{
			for (final Effect e : getDebuffs())
			{
				if (e == null)
				{
					continue;
				}
				if (e.getSkill().getId() == skillId)
				{
					if (e.isInUse())
					{
						return e;
					}
					effectNotInUse = e;
				}
			}
		}
		return effectNotInUse;
	}

	public final Effect getFirstPassiveEffect(EffectType type)
	{
		if (hasPassives())
		{
			for (final Effect e : getPassives())
			{
				if ((e != null) && (e.getEffectType() == type))
				{
					if (e.isInUse())
					{
						return e;
					}
				}
			}
		}
		return null;
	}

	private boolean doesStack(Skill checkSkill)
	{
		if ((!hasBuffs()) || (checkSkill._effectTemplates == null) || (checkSkill._effectTemplates.length < 1) || (checkSkill._effectTemplates[0].abnormalType == null) || "none".equals(checkSkill._effectTemplates[0].abnormalType))
		{
			return false;
		}

		final String stackType = checkSkill._effectTemplates[0].abnormalType;

		for (final Effect e : getBuffs())
		{
			if ((e.getAbnormalType() != null) && e.getAbnormalType().equalsIgnoreCase(stackType))
			{
				return true;
			}
		}
		return false;
	}

	public int getBuffCount()
	{
		if (!hasBuffs())
		{
			return 0;
		}

		final List<Integer> buffIds = new LinkedList<>();
		for (final Effect e : getBuffs())
		{
			if ((e != null) && e.isIconDisplay() && !e.getSkill().isDance() && !e.getSkill().isTriggeredSkill() && !e.getSkill().is7Signs())
			{
				if (!e.getSkill().isPassive() && !e.getSkill().isToggle() && !e.getSkill().isDebuff() && !e.getSkill().isHealingPotionSkill())
				{
					final int skillId = e.getSkill().getId();
					if (!buffIds.contains(skillId))
					{
						buffIds.add(skillId);
					}
				}
			}
		}
		return buffIds.size();
	}

	public int getDanceCount()
	{
		if (!hasBuffs())
		{
			return 0;
		}

		final List<Integer> buffIds = new LinkedList<>();
		for (final Effect e : getBuffs())
		{
			if ((e != null) && e.getSkill().isDance() && e.isInUse() && !e.isInstant())
			{
				switch (e.getEffectType())
				{
					case CANCEL :
					case CANCEL_ALL :
					case CANCEL_BY_SLOT :
						continue;
				}
				
				final int skillId = e.getSkill().getId();
				if (!buffIds.contains(skillId))
				{
					buffIds.add(skillId);
				}
			}
		}
		return buffIds.size();
	}
	
	public List<Effect> getEffects()
	{
		if (isEmpty())
		{
			return Collections.<Effect> emptyList();
		}
		
		final List<Effect> buffs = new ArrayList<>();
		if (hasBuffs())
		{
			buffs.addAll(getBuffs());
		}
		
		if (hasDebuffs())
		{
			buffs.addAll(getDebuffs());
		}
		return buffs;
	}

	public int getTriggeredBuffCount()
	{
		if (!hasBuffs())
		{
			return 0;
		}
		
		final List<Integer> buffIds = new LinkedList<>();
		for (final Effect e : getBuffs())
		{
			if ((e != null) && e.getSkill().isTriggeredSkill() && e.isInUse())
			{
				final int skillId = e.getSkill().getId();
				if (!buffIds.contains(skillId))
				{
					buffIds.add(skillId);
				}
			}
		}
		return buffIds.size();
	}

	public final void stopAllEffects()
	{
		stopAllEffects(true);
	}
	
	public final void stopAllEffects(boolean all)
	{
		if (all)
		{
			getEffects().stream().filter(e -> e != null).forEach(e -> e.exit(true, Config.DISPLAY_MESSAGE));
		}
		else
		{
			getEffects().stream().filter(e -> (e != null) && e.canBeStolen()).forEach(e -> e.exit(true, Config.DISPLAY_MESSAGE));
		}
	}
	
	public final void stopAllBuffs()
	{
		getBuffs().stream().filter(e -> e != null && !e.getSkill().isStayAfterDeath()).forEach(e -> e.exit());
	}
	
	public final void stopAllReflectionBuffs()
	{
		getBuffs().stream().filter(e -> e != null && e.getSkill().isReflectionBuff()).forEach(e -> e.exit());
	}

	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		getEffects().stream().filter(e -> (e != null) && !e.getSkill().isStayAfterDeath()).forEach(e -> e.exit(true, Config.DISPLAY_MESSAGE));
	}

	public void stopAllToggles()
	{
		getBuffs().stream().filter(e -> (e != null) && e.getSkill().isToggle()).forEach(e -> e.exit());
	}
	
	public void stopAllDances()
	{
		getBuffs().stream().filter(e -> (e != null) && e.getSkill().isDance()).forEach(e -> e.exit());
	}
	
	public final void stopAllDebuffs()
	{
		getDebuffs().stream().filter(e -> e != null).forEach(e -> e.exit());
		getBuffs().stream().filter(e -> e != null && !e.getSkill().isStayAfterDeath() && e.getSkill().hasDebuffEffects()).forEach(e -> e.exit());
	}

	public final void stopEffects(EffectType type)
	{
		getBuffs().stream().filter(e -> (e != null) && (e.getEffectType() == type)).forEach(e -> stopSkillEffects(e.getSkill().getId()));
		getDebuffs().stream().filter(e -> (e != null) && (e.getEffectType() == type)).forEach(e -> stopSkillEffects(e.getSkill().getId()));
	}

	public final void stopSkillEffects(int skillId)
	{
		getBuffs().stream().filter(e -> (e != null) && (e.getSkill().getId() == skillId)).forEach(e -> e.exit());
		getDebuffs().stream().filter(e -> (e != null) && (e.getSkill().getId() == skillId)).forEach(e -> e.exit());
	}
	
	public final void stopSkillEffect(Effect newEffect)
	{
		if (hasDebuffs())
		{
			for (final Effect e : getDebuffs())
			{
				if ((e != null) && (e.getClass().getSimpleName().equalsIgnoreCase(newEffect.getClass().getSimpleName())) && e.getSkill().getId() == newEffect.getSkill().getId())
				{
					if ((e.getEffectType() == newEffect.getEffectType()) && (e.getAbnormalLvl() <= newEffect.getAbnormalLvl()) && e.getAbnormalType().equalsIgnoreCase(newEffect.getAbnormalType()))
					{
						e.exit();
					}
				}
			}
		}
	}

	public void stopEffectsOnAction()
	{
		if (_hasBuffsRemovedOnAnyAction)
		{
			getBuffs().stream().filter(e -> (e != null) && e.getSkill().isRemovedOnAnyActionExceptMove()).forEach(e -> e.exit(true, true));
		}
	}

	public void stopEffectsOnDamage(boolean awake)
	{
		if (_hasBuffsRemovedOnDamage)
		{
			if (hasBuffs())
			{
				for (final Effect e : getBuffs())
				{
					if ((e != null) && e.getSkill().isRemovedOnDamage() && (awake || (e.getSkill().getSkillType() != SkillType.SLEEP)))
					{
						e.exit(true, true);
					}
				}
			}
		}
		if (_hasDebuffsRemovedOnDamage)
		{
			if (hasDebuffs())
			{
				for (final Effect e : getDebuffs())
				{
					if ((e != null) && e.getSkill().isRemovedOnDamage() && (awake || (e.getSkill().getSkillType() != SkillType.SLEEP)))
					{
						e.exit(true, true);
					}
				}
			}
		}
	}

	public void updateEffectIcons(boolean partyOnly, boolean printMessage)
	{
		if (!hasBuffs() && !hasDebuffs())
		{
			return;
		}

		if (partyOnly)
		{
			_partyOnly = true;
		}
		queueRunner(printMessage);
	}

	public void queueEffect(Effect effect, boolean remove, boolean printMessage)
	{
		if (effect == null)
		{
			return;
		}

		if (!_queuesInitialized)
		{
			init();
		}

		if (remove)
		{
			_removeQueue.offer(effect);
		}
		else
		{
			_addQueue.offer(effect);
		}
		queueRunner(printMessage);
	}

	private synchronized void init()
	{
		if (_queuesInitialized)
		{
			return;
		}
		_addQueue = new LinkedBlockingQueue<>();
		_removeQueue = new LinkedBlockingQueue<>();
		_queuesInitialized = true;
	}

	private void queueRunner(boolean printMessage)
	{
		if (!queueLock.compareAndSet(false, true))
		{
			return;
		}

		try
		{
			Effect effect;
			do
			{
				while ((effect = _removeQueue.poll()) != null)
				{
					removeEffectFromQueue(effect, printMessage);
					_partyOnly = false;
				}

				if ((effect = _addQueue.poll()) != null)
				{
					addEffectFromQueue(effect);
					_partyOnly = false;
				}
			}
			while (!_addQueue.isEmpty() || !_removeQueue.isEmpty());

			computeEffectFlags();
			updateEffectIcons();
		}
		finally
		{
			queueLock.set(false);
		}
	}

	protected void removeEffectFromQueue(Effect effect, boolean printMessage)
	{
		if (effect == null)
		{
			return;
		}

		if (effect.getSkill().isPassive())
		{
			if (effect.setInUse(false))
			{
				_owner.removeStatsOwner(effect.getStatFuncs());
				if (_passives != null)
				{
					_passives.remove(effect);
				}
			}
		}

		List<Effect> effectList;

		_rebuildCache = true;

		if (effect.getSkill().isDebuff())
		{
			if (!hasDebuffs())
			{
				return;
			}
			effectList = getDebuffs();
		}
		else
		{
			if (!hasBuffs())
			{
				return;
			}
			effectList = getBuffs();
		}

		if ("none".equals(effect.getAbnormalType()))
		{
			_owner.removeStatsOwner(effect);
		}
		else
		{
			if (_stackedEffects == null)
			{
				return;
			}

			final List<Effect> stackQueue = _stackedEffects.get(effect.getAbnormalType());

			if ((stackQueue == null) || stackQueue.isEmpty())
			{
				return;
			}

			final int index = stackQueue.indexOf(effect);

			if (index >= 0)
			{
				stackQueue.remove(effect);

				if (index == 0)
				{
					_owner.removeStatsOwner(effect);

					if (!stackQueue.isEmpty())
					{
						final Effect newStackedEffect = listsContains(stackQueue.get(0));
						if (newStackedEffect != null)
						{
							if (newStackedEffect.setInUse(true))
							{
								_owner.addStatFuncs(newStackedEffect.getStatFuncs());
							}
						}
					}
				}
				if (stackQueue.isEmpty())
				{
					_stackedEffects.remove(effect.getAbnormalType());
				}
				else
				{
					_stackedEffects.put(effect.getAbnormalType(), stackQueue);
				}
			}
		}

		if (effectList.remove(effect) && _owner.isPlayer() && effect.isIconDisplay() && !effect.isInstant())
		{
			if (printMessage)
			{
				final SystemMessage sm;
				if ((effect.getTickCount() >= (effect.getEffectTemplate().getTotalTickCount() - 1)) && effect.isIconDisplay() && !effect.isInstant())
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WORN_OFF);
				}
				else
				{
					sm = effect.getSkill().isToggle() ? SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ABORTED) : SystemMessage.getSystemMessage(SystemMessageId.EFFECT_S1_DISAPPEARED);
				}
				sm.addSkillName(effect);
				_owner.sendPacket(sm);
			}
		}
		
		if (effect == _owner.getEffectList().getShortBuff())
		{
			_owner.getEffectList().shortBuffStatusUpdate(null);
		}
	}

	protected void addEffectFromQueue(Effect newEffect)
	{
		if (newEffect == null)
		{
			return;
		}

		final Skill newSkill = newEffect.getSkill();
		if ((_blockedBuffSlots != null) && _blockedBuffSlots.contains(newEffect.getAbnormalType()))
		{
			return;
		}
		
		if (newEffect.getSkill().isPassive())
		{
			if ("none".equals(newEffect.getAbnormalType()))
			{
				if (newEffect.setInUse(true))
				{
					for (final Effect eff : getPassives())
					{
						if (eff == null)
						{
							continue;
						}

						if (eff.getEffectTemplate().equals(newEffect.getEffectTemplate()))
						{
							eff.exit();
						}

					}
					_owner.addStatFuncs(newEffect.getStatFuncs());
					getPassives().add(newEffect);
				}
			}
			return;
		}
		_rebuildCache = true;

		if (newSkill.isDebuff())
		{
			for (final Effect e : getDebuffs())
			{
				if ((e != null) && (!e.getAbnormalType().equals("none")) && (e.getSkill().getId() == newEffect.getSkill().getId()) && (e.getEffectType() == newEffect.getEffectType()) && (e.getAbnormalLvl() == newEffect.getAbnormalLvl()) && e.getAbnormalType().equalsIgnoreCase(newEffect.getAbnormalType()))
				{
					newEffect.stopEffectTask(true);
					return;
				}
			}
			
			int effectsToRemove;
			effectsToRemove = getDebuffCount() - _owner.getMaxDebuffCount();
			if (effectsToRemove >= 0)
			{
				for (final Effect e : getDebuffs())
				{
					if ((e == null) || !e.getSkill().isDebuff())
					{
						continue;
					}
					
					e.exit();
					effectsToRemove--;
					if (effectsToRemove < 0)
					{
						break;
					}
				}
			}
			
			int pos = 0;
			for (final Effect e : getDebuffs())
			{
				if (e == null)
				{
					continue;
				}
				pos++;
			}
			getDebuffs().add(pos, newEffect);
		}
		else
		{
			for (final Effect e : getBuffs())
			{
				if ((e != null) && (!e.getAbnormalType().equals("none")) && (e.getSkill().getId() == newEffect.getSkill().getId()) && (e.getEffectType() == newEffect.getEffectType()) && (e.getAbnormalLvl() == newEffect.getAbnormalLvl()) && e.getAbnormalType().equalsIgnoreCase(newEffect.getAbnormalType()))
				{
					e.exit();
				}
			}

			if (!doesStack(newSkill) && !newSkill.is7Signs())
			{
				int effectsToRemove;
				if (newSkill.isDance())
				{
					effectsToRemove = getDanceCount() - Config.DANCES_MAX_AMOUNT;
					if (effectsToRemove >= 0)
					{
						for (final Effect e : getBuffs())
						{
							if ((e == null) || !e.getSkill().isDance() || !e.getSkill().isReplaceLimit())
							{
								continue;
							}

							e.exit();
							effectsToRemove--;
							if (effectsToRemove < 0)
							{
								break;
							}
						}
					}
				}
				else if (newSkill.isTriggeredSkill())
				{
					effectsToRemove = getTriggeredBuffCount() - Config.TRIGGERED_BUFFS_MAX_AMOUNT;
					if (effectsToRemove >= 0)
					{
						for (final Effect e : getBuffs())
						{
							if ((e == null) || !e.getSkill().isTriggeredSkill() || !e.getSkill().isReplaceLimit())
							{
								continue;
							}

							e.exit();
							effectsToRemove--;
							if (effectsToRemove < 0)
							{
								break;
							}
						}
					}
				}
				else if (!newSkill.isHealingPotionSkill() && !newEffect.isInstant())
				{
					effectsToRemove = getBuffCount() - _owner.getMaxBuffCount();
					if (effectsToRemove >= 0)
					{
						if (newSkill.getSkillType() == SkillType.BUFF)
						{
							for (final Effect e : getBuffs())
							{
								if ((e == null) || e.getSkill().isDance() || !e.getSkill().isReplaceLimit() || e.getSkill().isTriggeredSkill() || e.getEffectType() == EffectType.TRANSFORMATION)
								{
									continue;
								}

								if (e.getSkill().getSkillType() == SkillType.BUFF)
								{
									e.exit();
									effectsToRemove--;
								}
								else
								{
									continue;
								}

								if (effectsToRemove < 0)
								{
									break;
								}
							}
						}
					}
				}
			}

			if (newSkill.isTriggeredSkill() || !newSkill.isReplaceLimit())
			{
				getBuffs().add(newEffect);
			}
			else
			{
				int pos = 0;
				if (newSkill.isToggle())
				{
					for (final Effect e : getBuffs())
					{
						if (e == null)
						{
							continue;
						}
						if (e.getSkill().isDance())
						{
							break;
						}
						pos++;
					}
				}
				else if (newSkill.isDance())
				{
					for (final Effect e : getBuffs())
					{
						if (e == null)
						{
							continue;
						}
						if (e.getSkill().isTriggeredSkill())
						{
							break;
						}
						pos++;
					}
				}
				else
				{
					for (final Effect e : getBuffs())
					{
						if (e == null)
						{
							continue;
						}
						if (e.getSkill().isToggle() || e.getSkill().is7Signs() || e.getSkill().isDance() || e.getSkill().isTriggeredSkill() || !e.getSkill().isReplaceLimit())
						{
							break;
						}
						pos++;
					}
				}
				getBuffs().add(pos, newEffect);
			}
		}

		if ("none".equals(newEffect.getAbnormalType()))
		{
			if (newEffect.setInUse(true))
			{
				_owner.addStatFuncs(newEffect.getStatFuncs());
			}
			return;
		}
		Effect effectToAdd = null;
		Effect effectToRemove = null;
		if (_stackedEffects == null)
		{
			_stackedEffects = new ConcurrentHashMap<>();
		}

		List<Effect> stackQueue = _stackedEffects.get(newEffect.getAbnormalType());
		if (stackQueue == null)
		{
			stackQueue = new ArrayList<>();
		}
		
		if (!stackQueue.isEmpty())
		{
			int pos = 0;
			if (!stackQueue.isEmpty())
			{
				effectToRemove = listsContains(stackQueue.get(0));

				final Iterator<Effect> queueIterator = stackQueue.iterator();

				while (queueIterator.hasNext())
				{
					if (newEffect.getAbnormalLvl() < queueIterator.next().getAbnormalLvl())
					{
						pos++;
					}
					else
					{
						break;
					}
				}
				stackQueue.add(pos, newEffect);

				final var isValid = newEffect.getSkill().isStatic() && !newEffect.getSkill().isPotion() || newEffect.getSkill().isAbnormalInstant();
				if (Config.EFFECT_CANCELING && !isValid && (stackQueue.size() > 1))
				{
					if (newSkill.isDebuff())
					{
						getDebuffs().remove(stackQueue.remove(1));
					}
					else
					{
						getBuffs().remove(stackQueue.remove(1));
					}
				}
			}
			else
			{
				stackQueue.add(0, newEffect);
			}
		}
		else
		{
			stackQueue.add(0, newEffect);
		}
		_stackedEffects.put(newEffect.getAbnormalType(), stackQueue);

		if (!stackQueue.isEmpty())
		{
			effectToAdd = listsContains(stackQueue.get(0));
		}

		if (effectToRemove != effectToAdd)
		{
			if (effectToRemove != null)
			{
				_owner.removeStatsOwner(effectToRemove);
				effectToRemove.setInUse(false);
			}
			if (effectToAdd != null)
			{
				if (effectToAdd.setInUse(true))
				{
					_owner.addStatFuncs(effectToAdd.getStatFuncs());
				}
			}
		}
	}

	public void removePassiveEffects(int skillId)
	{
		if (!hasPassives())
		{
			return;
		}

		for (final Effect eff : getPassives())
		{
			if (eff == null)
			{
				continue;
			}

			if (eff.getSkill().getId() == skillId)
			{
				eff.exit();
				getPassives().remove(eff);
			}
		}
	}
	
	protected void updateEffectIcons()
	{
		if (_owner == null)
		{
			return;
		}
		
		if ((_effectIconsUpdate != null) && !_effectIconsUpdate.isDone())
		{
			return;
		}
		_effectIconsUpdate = ThreadPoolManager.getInstance().schedule(new UpdateEffectIconsTask(), Config.USER_ABNORMAL_EFFECTS_INTERVAL);
	}

	private class UpdateEffectIconsTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_owner == null)
			{
				return;
			}
			
			if (_owner.isPlayer() && _owner.getActingPlayer()._entering)
			{
				return;
			}
			
			if (!Config.ALLOW_CUSTOM_INTERFACE && !_owner.isPlayable())
			{
				updateEffectFlags();
				return;
			}

			AbnormalStatusUpdate asu = null;
			PartySpelled ps = null;
			PartySpelled psSummon = null;
			ExOlympiadSpelledInfo os = null;
			boolean isSummon = false;
			final List<Effect> effectList = new ArrayList<>();
			
			if (_owner.isPlayer())
			{
				if (_partyOnly)
				{
					_partyOnly = false;
				}
				else
				{
					asu = new AbnormalStatusUpdate();
				}
				
				if (_owner.isInParty())
				{
					ps = new PartySpelled(_owner);
				}

				if (_owner.getActingPlayer().isInOlympiadMode() && _owner.getActingPlayer().isOlympiadStart())
				{
					os = new ExOlympiadSpelledInfo(_owner.getActingPlayer());
				}

				if(_owner.getActingPlayer().isInPartyTournament() && _owner.getActingPlayer().getPartyTournament().isRunning())
					os = new ExOlympiadSpelledInfo(_owner.getActingPlayer());
			}
			else if (_owner.isSummon())
			{
				isSummon = true;
				ps = new PartySpelled(_owner);
				psSummon = new PartySpelled(_owner);
			}
			
			boolean foundRemovedOnAction = false;
			boolean foundRemovedOnDamage = false;

			if (hasBuffs())
			{
				for (final Effect e : getBuffs())
				{
					if (e == null)
					{
						continue;
					}
					
					if (e.getSkill().isRemovedOnAnyActionExceptMove())
					{
						foundRemovedOnAction = true;
					}
					if (e.getSkill().isRemovedOnDamage())
					{
						foundRemovedOnDamage = true;
					}
					
					if (!e.isIconDisplay() || e.isInstant() || (e.getEffectType() == EffectType.SIGNET_GROUND))
					{
						continue;
					}

					if (e.isInUse())
					{
						if (e.getSkill().isHealingPotionSkill())
						{
							shortBuffStatusUpdate(e);
						}
						else
						{
							addIcon(e, asu, ps, psSummon, os, isSummon, effectList);
						}
					}
				}

			}
			
			_hasBuffsRemovedOnAnyAction = foundRemovedOnAction;
			_hasBuffsRemovedOnDamage = foundRemovedOnDamage;
			foundRemovedOnDamage = false;

			if (hasDebuffs())
			{
				for (final Effect e : getDebuffs())
				{
					if (e == null)
					{
						continue;
					}
					
					if (e.getSkill().isRemovedOnAnyActionExceptMove())
					{
						foundRemovedOnAction = true;
					}
					if (e.getSkill().isRemovedOnDamage())
					{
						foundRemovedOnDamage = true;
					}
					
					if (!e.isIconDisplay() || e.isInstant() || (e.getEffectType() == EffectType.SIGNET_GROUND))
					{
						continue;
					}

					if (e.isInUse())
					{
						addIcon(e, asu, ps, psSummon, os, isSummon, effectList);
					}
				}
			}

			_hasDebuffsRemovedOnDamage = foundRemovedOnDamage;
			
			if (asu != null)
			{
				_owner.sendPacket(asu);
			}

			if (ps != null)
			{
				if (_owner.isSummon())
				{
					final Player summonOwner = ((Summon) _owner).getOwner();

					if (summonOwner != null)
					{
						if (summonOwner.isInParty())
						{
							summonOwner.getParty().broadcastToPartyMembers(summonOwner, psSummon);
						}
						summonOwner.sendPacket(ps);
					}
				}
				else if (_owner.isPlayer() && _owner.isInParty())
				{
					_owner.getParty().broadCast(ps);
				}
			}

			if (os != null)
			{
				final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(_owner.getActingPlayer().getOlympiadGameId());
				if ((game != null) && game.isBattleStarted())
				{
					game.getZone().broadcastPacketToObservers(os);
				}

				Tournament tournament = TournamentData.getInstance().getTournament(_owner.getActingPlayer().getTournamentGameId());
				if(tournament != null && tournament.isRunning())
				{
					tournament.broadcastPacketToObservers(os);
				}
			}
			
			if (Config.ALLOW_CUSTOM_INTERFACE)
			{
				final var canSend = _owner.isPlayable() || _owner.isNpc();
				final var upd = new ExAbnormalStatusUpdateFromTarget(_owner, asu != null ? asu.getEffects() : effectList);
				for (final var creature : _owner.getStatus().getStatusListener())
				{
					if ((creature != null) && creature.isPlayer())
					{
						if (canSend)
						{
							upd.sendTo(creature.getActingPlayer());
						}
					}
				}
				
				if (_owner.isPlayer() && (_owner.getTarget() == _owner))
				{
					_owner.sendPacket(upd);
				}
				effectList.clear();
			}
			_effectIconsUpdate = null;
		}
	}

	protected void updateEffectFlags()
	{
		boolean foundRemovedOnAction = false;
		boolean foundRemovedOnDamage = false;
		
		if (hasBuffs())
		{
			for (final Effect e : getBuffs())
			{
				if (e == null)
				{
					continue;
				}

				if (e.getSkill().isRemovedOnAnyActionExceptMove())
				{
					foundRemovedOnAction = true;
				}
				if (e.getSkill().isRemovedOnDamage())
				{
					foundRemovedOnDamage = true;
				}
			}
		}
		
		_hasBuffsRemovedOnAnyAction = foundRemovedOnAction;
		_hasBuffsRemovedOnDamage = foundRemovedOnDamage;
		foundRemovedOnDamage = false;

		if (hasDebuffs())
		{
			for (final Effect e : getDebuffs())
			{
				if (e == null)
				{
					continue;
				}

				if (e.getSkill().isRemovedOnDamage())
				{
					foundRemovedOnDamage = true;
				}
			}
		}
		_hasDebuffsRemovedOnDamage = foundRemovedOnDamage;
	}

	private Effect listsContains(Effect effect)
	{
		if (hasBuffs() && getBuffs().contains(effect))
		{
			return effect;
		}
		if (hasDebuffs() && getDebuffs().contains(effect))
		{
			return effect;
		}
		return null;
	}

	private final void computeEffectFlags()
	{
		int flags = 0;
		StringBuilder debuffMessage = null;
		if(Config.ENABLE_SOMIK_INTERFACE)
			debuffMessage = new StringBuilder("[TargetDebuffList]");

		if (hasBuffs())
		{
			for (final Effect e : getBuffs())
			{
				if (e == null)
				{
					continue;
				}
				flags |= e.getEffectFlags();
			}
		}

		if (hasDebuffs())
		{
			for (final Effect e : getDebuffs())
			{
				if (e == null)
				{
					continue;
				}
				flags |= e.getEffectFlags();

				if (debuffMessage != null && e.getSkill() != null)
					debuffMessage.append(e.getSkill().getIcon()).append(";");
			}
		}
		
		if (hasPassives())
		{
			for (final Effect e : getPassives())
			{
				if (e == null)
				{
					continue;
				}
				flags |= e.getEffectFlags();
			}
		}

		if(debuffMessage != null)
		{
			final var ownerStatus = _owner.getStatus();
			final var listen = ownerStatus.getStatusListener();
			if ((listen != null) && !listen.isEmpty())
			{
				StringBuilder finalDebuffMessage = debuffMessage;
				listen.stream()
						.filter(temp -> temp != null)
						.forEach(temp -> temp.sendPacket(new CreatureSay(0, Say2.MSNCHAT, temp.getName(null), finalDebuffMessage.toString())));
			}
		}

		_effectFlags = flags;
	}
	
	public boolean isEmpty()
	{
		return ((_buffs == null) || _buffs.isEmpty()) && ((_debuffs == null) || _debuffs.isEmpty());
	}
	
	public boolean hasBuffs()
	{
		return (_buffs != null) && !_buffs.isEmpty();
	}
	
	public boolean hasDebuffs()
	{
		return (_debuffs != null) && !_debuffs.isEmpty();
	}
	
	public boolean hasPassives()
	{
		return (_passives != null) && !_passives.isEmpty();
	}
	
	public List<Effect> getBuffs()
	{
		if (_buffs == null)
		{
			_buffs = new CopyOnWriteArrayList<>();
		}
		return _buffs;
	}
	
	public List<Effect> getDebuffs()
	{
		if (_debuffs == null)
		{
			_debuffs = new CopyOnWriteArrayList<>();
		}
		return _debuffs;
	}
	
	public int getDebuffCount()
	{
		if (_debuffs == null)
		{
			_debuffs = new CopyOnWriteArrayList<>();
		}
		return _debuffs.size();
	}
	
	public List<Effect> getPassives()
	{
		if (_passives == null)
		{
			_passives = new CopyOnWriteArrayList<>();
		}
		return _passives;
	}

	public boolean isAffected(EffectFlag flag)
	{
		return (_effectFlags & flag.getMask()) != 0;
	}

	public void clear()
	{
		try
		{
			_addQueue = null;
			_removeQueue = null;
			_buffs = null;
			_debuffs = null;
			_stackedEffects = null;
			_queuesInitialized = false;
			_effectCache = null;
		}
		catch (final Exception e)
		{
		}
	}
	
	public void addBlockedBuffSlots(Set<String> blockedBuffSlots)
	{
		if (_blockedBuffSlots == null)
		{
			synchronized (this)
			{
				if (_blockedBuffSlots == null)
				{
					_blockedBuffSlots = ConcurrentHashMap.newKeySet(blockedBuffSlots.size());
				}
			}
		}
		_blockedBuffSlots.addAll(blockedBuffSlots);
	}
	
	public boolean removeBlockedBuffSlots(Set<String> blockedBuffSlots)
	{
		if (_blockedBuffSlots != null)
		{
			return _blockedBuffSlots.removeAll(blockedBuffSlots);
		}
		return false;
	}
	
	public Set<String> getAllBlockedBuffSlots()
	{
		return _blockedBuffSlots;
	}
	
	public Effect getShortBuff()
	{
		return _shortBuff;
	}
	
	private void addIcon(Effect info, AbnormalStatusUpdate asu, PartySpelled ps, PartySpelled psSummon, ExOlympiadSpelledInfo os, boolean isSummon, List<Effect> effectList)
	{
		if ((info == null) || !info.isInUse())
		{
			return;
		}
		
		final Skill skill = info.getSkill();
		if (asu != null)
		{
			asu.addSkill(info);
		}
		effectList.add(info);
		
		if ((ps != null) && (isSummon || !skill.isToggle()))
		{
			ps.addSkill(info);
		}
		
		if ((psSummon != null) && !skill.isToggle())
		{
			psSummon.addSkill(info);
		}
		
		if (os != null)
		{
			os.addSkill(info);
		}
	}
	
	public void shortBuffStatusUpdate(Effect info)
	{
		if (_owner.isPlayer())
		{
			_shortBuff = info;
			if (info == null)
			{
				_owner.sendPacket(ShortBuffStatusUpdate.RESET_SHORT_BUFF);
			}
			else
			{
				_owner.sendPacket(new ShortBuffStatusUpdate(info.getSkill().getId(), info.getSkill().getLevel(), info.getTimeLeft()));
			}
		}
	}
}