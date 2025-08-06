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
package l2e.gameserver.listener.actor;

import l2e.commons.listener.Listener;
import l2e.commons.listener.ListenerList;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;

public class CharListenerList extends ListenerList<Creature>
{
	public final static ListenerList<Creature> _global = new ListenerList<>();

	protected final Creature _actor;

	public CharListenerList(Creature actor)
	{
		_actor = actor;
	}

	public Creature getActor()
	{
		return _actor;
	}

	public final static boolean addGlobal(Listener<Creature> listener)
	{
		return _global.add(listener);
	}

	public final static boolean removeGlobal(Listener<Creature> listener)
	{
		return _global.remove(listener);
	}
	
	public void onAttack(Creature target)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnAttackListener.class.isInstance(listener))
				{
					((OnAttackListener) listener).onAttack(getActor(), target);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnAttackListener.class.isInstance(listener))
				{
					((OnAttackListener) listener).onAttack(getActor(), target);
				}
			}
		}
	}
	
	public void onAttackHit(Creature attacker)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnAttackHitListener.class.isInstance(listener))
				{
					((OnAttackHitListener) listener).onAttackHit(getActor(), attacker);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnAttackHitListener.class.isInstance(listener))
				{
					((OnAttackHitListener) listener).onAttackHit(getActor(), attacker);
				}
			}
		}
	}
	
	public void onMagicUse(Skill skill, GameObject[] targets, boolean alt)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnMagicUseListener.class.isInstance(listener))
				{
					((OnMagicUseListener) listener).onMagicUse(getActor(), skill, targets, alt);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnMagicUseListener.class.isInstance(listener))
				{
					((OnMagicUseListener) listener).onMagicUse(getActor(), skill, targets, alt);
				}
			}
		}
	}
	
	public void onMagicHit(Skill skill, Creature caster)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnMagicHitListener.class.isInstance(listener))
				{
					((OnMagicHitListener) listener).onMagicHit(getActor(), skill, caster);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnMagicHitListener.class.isInstance(listener))
				{
					((OnMagicHitListener) listener).onMagicHit(getActor(), skill, caster);
				}
			}
		}
	}
	
	public void onDeath(Creature killer)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnDeathListener.class.isInstance(listener))
				{
					((OnDeathListener) listener).onDeath(getActor(), killer);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnDeathListener.class.isInstance(listener))
				{
					((OnDeathListener) listener).onDeath(getActor(), killer);
				}
			}
		}
	}
	
	public void onKill(Creature victim)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnKillListener.class.isInstance(listener) && !((OnKillListener) listener).ignorePetOrSummon())
				{
					((OnKillListener) listener).onKill(getActor(), victim);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnKillListener.class.isInstance(listener) && !((OnKillListener) listener).ignorePetOrSummon())
				{
					((OnKillListener) listener).onKill(getActor(), victim);
				}
			}
		}
	}
	
	public void onKillIgnorePetOrSummon(Creature victim)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnKillListener.class.isInstance(listener) && ((OnKillListener) listener).ignorePetOrSummon())
				{
					((OnKillListener) listener).onKill(getActor(), victim);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnKillListener.class.isInstance(listener) && ((OnKillListener) listener).ignorePetOrSummon())
				{
					((OnKillListener) listener).onKill(getActor(), victim);
				}
			}
		}
	}
	
	public void onCurrentHpDamage(double damage, Creature attacker, Skill skill)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnCurrentHpDamageListener.class.isInstance(listener))
				{
					((OnCurrentHpDamageListener) listener).onCurrentHpDamage(getActor(), damage, attacker, skill);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnCurrentHpDamageListener.class.isInstance(listener))
				{
					((OnCurrentHpDamageListener) listener).onCurrentHpDamage(getActor(), damage, attacker, skill);
				}
			}
		}
	}
	
	public void onChangeCurrentCp(double oldCp, double newCp)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnChangeCurrentCpListener.class.isInstance(listener))
				{
					((OnChangeCurrentCpListener) listener).onChangeCurrentCp(getActor(), oldCp, newCp);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnChangeCurrentCpListener.class.isInstance(listener))
				{
					((OnChangeCurrentCpListener) listener).onChangeCurrentCp(getActor(), oldCp, newCp);
				}
			}
		}
	}
	
	public void onChangeCurrentHp(double oldHp, double newHp)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnChangeCurrentHpListener.class.isInstance(listener))
				{
					((OnChangeCurrentHpListener) listener).onChangeCurrentHp(getActor(), oldHp, newHp);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnChangeCurrentHpListener.class.isInstance(listener))
				{
					((OnChangeCurrentHpListener) listener).onChangeCurrentHp(getActor(), oldHp, newHp);
				}
			}
		}
	}
	
	public void onChangeCurrentMp(double oldMp, double newMp)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnChangeCurrentMpListener.class.isInstance(listener))
				{
					((OnChangeCurrentMpListener) listener).onChangeCurrentMp(getActor(), oldMp, newMp);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnChangeCurrentMpListener.class.isInstance(listener))
				{
					((OnChangeCurrentMpListener) listener).onChangeCurrentMp(getActor(), oldMp, newMp);
				}
			}
		}
	}
	
	public void onAct(final String act, final Object... args)
	{
		if (!_global.getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : _global.getListeners())
			{
				if (OnActorAct.class.isInstance(listener))
				{
					((OnActorAct) listener).onAct(getActor(), act, args);
				}
			}
		}
		
		if (!getListeners().isEmpty())
		{
			for (final Listener<Creature> listener : getListeners())
			{
				if (OnActorAct.class.isInstance(listener))
				{
					((OnActorAct) listener).onAct(getActor(), act, args);
				}
			}
		}
	}
}