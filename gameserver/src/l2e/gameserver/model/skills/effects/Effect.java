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
package l2e.gameserver.model.skills.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.ChanceCondition;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.interfaces.IChanceSkillTrigger;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.funcs.Func;
import l2e.gameserver.model.skills.funcs.FuncTemplate;
import l2e.gameserver.model.skills.funcs.Lambda;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillLaunched;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.taskmanager.EffectTaskManager;

public abstract class Effect implements IChanceSkillTrigger
{
	protected static final Logger _log = LoggerFactory.getLogger(Effect.class);

	private static final Func[] _emptyFunctionSet = new Func[0];
	
	private final Creature _effector;
	private final Creature _effected;
	private final Skill _skill;
	private final Lambda _lambda;
	private EffectState _state;
	protected long _periodStartTime;
	protected int _periodFirstTime;
	private final EffectTemplate _template;
	private final FuncTemplate[] _funcTemplates;
	private int _tickCount;
	private final int _totalTickCount;
	private int _abnormalTime;
	private boolean _isSelfEffect = false;
	private boolean _isPassiveEffect = false;
	public boolean _preventExitUpdate;
	private volatile ScheduledFuture<?> _currentFuture;
	private boolean _inUse = false;
	private boolean _startConditionsCorrect = true;
	private boolean _isRemoved = false;
	private boolean _isReflectable = true;
	private final String _abnormalType;
	private final byte _abnormalLvl;

	protected final class EffectTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				_periodFirstTime = 0;
				_periodStartTime = System.currentTimeMillis();
				scheduleEffect(true);
			}
			catch (final Exception e)
			{
				_log.warn("", e);
			}
		}
	}

	protected Effect(Env env, EffectTemplate template)
	{
		_state = EffectState.CREATED;
		_skill = env.getSkill();
		_template = template;
		_effected = env.getTarget();
		_effector = env.getCharacter();
		_lambda = template.getLambda();
		_funcTemplates = template.funcTemplates;
		_totalTickCount = Formulas.calcEffectTickCount(env, template);
		_tickCount = 0;
		_abnormalTime = Formulas.calcEffectAbnormalTime(env, template);
		_periodStartTime = System.currentTimeMillis();
		_periodFirstTime = 0;
		_abnormalType = template.abnormalType;
		_abnormalLvl = template.abnormalLvl;
		_isReflectable = template.isReflectable();
	}

	protected Effect(Env env, Effect effect)
	{
		_template = effect._template;
		_state = EffectState.CREATED;
		_skill = env.getSkill();
		_effected = env.getTarget();
		_effector = env.getCharacter();
		_lambda = _template.getLambda();
		_funcTemplates = _template.funcTemplates;
		_totalTickCount = _template._totalTickCount;
		_tickCount = effect.getTickCount();
		_abnormalTime = effect.getAbnormalTime();
		_periodStartTime = effect.getPeriodStartTicks();
		_periodFirstTime = effect.getTime();
		_abnormalType = _template.abnormalType;
		_abnormalLvl = _template.abnormalLvl;
		_isReflectable = effect.isReflectable();
	}

	public int getTickCount()
	{
		return _tickCount;
	}
	
	public int getTotalTickCount()
	{
		return _totalTickCount;
	}

	public void setCount(int newTickCount)
	{
		_tickCount = Math.min(newTickCount, _totalTickCount);
	}

	public void setFirstTime(int newFirstTime)
	{
		_periodFirstTime = Math.min(newFirstTime, _abnormalTime);
		_periodStartTime = System.currentTimeMillis() - _periodFirstTime * 1000;
	}

	public boolean isIconDisplay()
	{
		return _template.isIconDisplay();
	}
	
	public void setAbnormalTime(int time)
	{
		_abnormalTime = time;
	}

	public int getAbnormalTime()
	{
		return _abnormalTime;
	}

	public int getTime()
	{
		return (int) ((System.currentTimeMillis() - _periodStartTime) / 1000);
	}
	
	public int getTimeLeft()
	{
		if (_totalTickCount > 1 && !getSkill().isToggle())
		{
			return ((_totalTickCount - _tickCount)) - getTime();
		}
		return _abnormalTime - getTime();
	}

	public boolean isInUse()
	{
		return _inUse;
	}
	
	public boolean setInUse(boolean inUse)
	{
		_inUse = inUse;
		if (_inUse)
		{
			_startConditionsCorrect = onStart();
		}
		else
		{
			onExit();
		}
		return _startConditionsCorrect;
	}

	public String getAbnormalType(String ngt)
	{
		return _abnormalType;
	}

	public String getAbnormalType()
	{
		return _abnormalType;
	}

	public byte getAbnormalLvl()
	{
		return _abnormalLvl;
	}

	public final Skill getSkill()
	{
		return _skill;
	}

	public final Creature getEffector()
	{
		return _effector;
	}

	public final Creature getEffected()
	{
		return _effected;
	}

	public boolean isSelfEffect()
	{
		return _isSelfEffect;
	}

	public void setSelfEffect()
	{
		_isSelfEffect = true;
	}

	public boolean isPassiveEffect()
	{
		return _isPassiveEffect;
	}

	public void setPassiveEffect()
	{
		_isPassiveEffect = true;
	}

	public final double calc()
	{
		final Env env = new Env();
		env.setCharacter(_effector);
		env.setTarget(_effected);
		env.setSkill(_skill);
		return _lambda.calc(env);
	}

	public boolean calcSuccess()
	{
		final Env env = new Env();
		env.setSkillMastery(Formulas.calcSkillMastery(getEffector(), getSkill()));
		env.setCharacter(getEffector());
		env.setTarget(getEffected());
		env.setSkill(getSkill());
		env.setEffect(this);
		return Formulas.calcEffectSuccess(env);
	}

	private final synchronized void startEffectTask()
	{
		stopEffectTask(true);

		final int delay = Math.max((_abnormalTime - _periodFirstTime) * 1000, 5);
		if (_totalTickCount > 0 && !getSkill().isToggle())
		{
			_currentFuture = EffectTaskManager.getInstance().scheduleAtFixedRate(new EffectTask(), delay, _abnormalTime * 1000);
		}
		else
		{
			_currentFuture = EffectTaskManager.getInstance().schedule(new EffectTask(), delay);
		}

		if (_state == EffectState.ACTING)
		{
			if (isSelfEffectType())
			{
				_effector.addEffect(this);
			}
			else
			{
				_effected.addEffect(this);
			}
		}
	}

	public final void exit()
	{
		exit(false, true);
	}

	public final void exit(boolean preventExitUpdate, boolean printMessage)
	{
		_preventExitUpdate = preventExitUpdate;
		_state = EffectState.FINISHING;
		scheduleEffect(printMessage);
	}

	public final synchronized void stopEffectTask(boolean printMessage)
	{
		var task = _currentFuture;
		if (task != null || getSkill().isToggle())
		{
			if (task != null)
			{
				task.cancel(false);
				task = null;
			}
			
			if (isSelfEffectType() && (getEffector() != null))
			{
				getEffector().removeEffect(this, printMessage);
			}
			else if (getEffected() != null)
			{
				getEffected().removeEffect(this, printMessage);
			}
			
			if (getEffected() != null && getEffected().hasSummon())
			{
				getEffected().getSummon().removeEffect(this, printMessage);
			}
		}
	}

	public abstract EffectType getEffectType();

	public boolean onStart()
	{
		if (getSkill().isToggle())
		{
			getEffector().getToggleList().addToggleEffect(this);
		}
		
		for (final AbnormalEffect eff : _template.getAbnormalEffect())
		{
			if (eff != null && eff != AbnormalEffect.NONE)
			{
				getEffected().startAbnormalEffect(eff);
			}
		}
		return true;
	}

	public void onExit()
	{
		for (final AbnormalEffect eff : _template.getAbnormalEffect())
		{
			if (eff != null && eff != AbnormalEffect.NONE)
			{
				getEffected().stopAbnormalEffect(eff);
			}
		}
	}

	public boolean onActionTime()
	{
		return getAbnormalTime() < 0;
	}
	
	public double getMpReduce()
	{
		return 0;
	}
	
	public double getHpReduce()
	{
		return 0;
	}

	public final void scheduleEffect(boolean printMessage)
	{
		switch (_state)
		{
			case CREATED :
			{
				_state = EffectState.ACTING;
				
				if (getEffector().isPlayer() && _skill.isDebuff() && Config.ALLOW_DEBUFF_INFO)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SUCCEEDED);
					sm.addSkillName(_skill);
					getEffector().sendPacket(sm);
				}
				
				if (_skill.isOffensive() && isIconDisplay() && getEffected().isPlayer())
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(_skill);
					getEffected().sendPacket(sm);
				}
				
				getEffected().updateAbnormalEffect();
				
				if (_abnormalTime != 0)
				{
					startEffectTask();
					return;
				}
				_startConditionsCorrect = onStart();
			}
			case ACTING :
			{
				if (isInUse())
				{
					_tickCount++;
					if (onActionTime() && _startConditionsCorrect)
					{
						return;
					}
				}
				
				if (_tickCount < _totalTickCount && !isNevativeTime())
				{
					return;
				}
				_state = EffectState.FINISHING;
			}
			case FINISHING :
			{
				if ((_currentFuture == null) && (getEffected() != null) && _abnormalTime > 0)
				{
					getEffected().removeEffect(this, printMessage);
				}

				stopEffectTask(printMessage);
				getEffected().updateAbnormalEffect();
				
				if (isInUse() || !((_tickCount > 1) || (_abnormalTime > 0)) || isNevativeTime())
				{
					if (_startConditionsCorrect || isNevativeTime())
					{
						onExit();
					}
				}
				
				if (getSkill().isToggle())
				{
					getEffector().getToggleList().removeEffect(this);
				}
				
				if (_skill.getAfterEffectId() > 0)
				{
					final Skill skill = SkillsParser.getInstance().getInfo(_skill.getAfterEffectId(), _skill.getAfterEffectLvl());
					if (skill != null)
					{
						getEffected().broadcastPacket(new MagicSkillUse(_effected, skill.getId(), skill.getLevel(), 0, 0));
						getEffected().broadcastPacket(new MagicSkillLaunched(_effected, skill.getId(), skill.getLevel()));
						skill.getEffects(getEffected(), getEffected(), true);
					}
				}
			}
		}
	}
	
	private boolean isNevativeTime()
	{
		return !getSkill().isToggle() && getTimeLeft() < 0;
	}

	public Func[] getStatFuncs()
	{
		if (_funcTemplates == null)
		{
			return _emptyFunctionSet;
		}

		final List<Func> funcs = new ArrayList<>(_funcTemplates.length);

		final Env env = new Env();
		env.setCharacter(_effector);
		env.setTarget(_effected);
		env.setSkill(_skill);

		for (final FuncTemplate t : _funcTemplates)
		{
			final Func f = t.getFunc(env, this);
			if (f != null)
			{
				funcs.add(f);
			}
		}

		if (funcs.isEmpty())
		{
			return _emptyFunctionSet;
		}
		return funcs.toArray(new Func[funcs.size()]);
	}

	public long getPeriodStartTicks()
	{
		return _periodStartTime;
	}

	public EffectTemplate getEffectTemplate()
	{
		return _template;
	}

	public double getEffectPower()
	{
		return _template.getEffectPower();
	}

	public boolean canBeStolen()
	{
		return !getSkill().isPassive() && (getEffectType() != EffectType.TRANSFORMATION) && !getSkill().isToggle() && !getSkill().isDebuff() && !getSkill().isHeroSkill() && !getSkill().isGMSkill() && !(getSkill().isStatic() && (getSkill().getId() != 2341)) && getSkill().canBeDispeled();
	}

	public int getEffectFlags()
	{
		return EffectFlag.NONE.getMask();
	}

	@Override
	public String toString()
	{
		return "Effect " + getClass().getSimpleName() + ", " + _skill + ", State: " + _state + ", Time: " + _abnormalTime + ", Remaining: " + getTimeLeft();
	}

	public boolean isSelfEffectType()
	{
		return false;
	}

	public void decreaseForce()
	{
	}

	public void increaseEffect()
	{
	}

	public boolean checkCondition(Object obj)
	{
		return true;
	}

	@Override
	public boolean triggersChanceSkill()
	{
		return false;
	}

	@Override
	public int getTriggeredChanceId()
	{
		return 0;
	}

	@Override
	public int getTriggeredChanceLevel()
	{
		return 0;
	}

	@Override
	public ChanceCondition getTriggeredChanceCondition()
	{
		return null;
	}

	public boolean isInstant()
	{
		return false;
	}

	public boolean isRemoved()
	{
		return _isRemoved;
	}

	public void setRemoved(boolean val)
	{
		_isRemoved = val;
	}
	
	public boolean isReflectable()
	{
		return _isReflectable;
	}
}