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
package l2e.gameserver.model.actor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.stat.PlayableStat;
import l2e.gameserver.model.actor.status.PlayableStatus;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.EffectFlag;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.zone.ZoneId;

public abstract class Playable extends Creature
{
	private Creature _lockedTarget = null;
	private Player transferDmgTo = null;
	private final List<Integer> _hitmanTargets = new CopyOnWriteArrayList<>();
	
	public Playable(int objectId, CharTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Playable);
		setIsInvul(false);
	}
	
	@Override
	public PlayableStat getStat()
	{
		return (PlayableStat) super.getStat();
	}

	@Override
	public void initCharStat()
	{
		setStat(new PlayableStat(this));
	}

	@Override
	public PlayableStatus getStatus()
	{
		return (PlayableStatus) super.getStatus();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new PlayableStatus(this));
	}

	@Override
	protected void onDeath(Creature killer)
	{
		super.onDeath(killer);

		final boolean fightEventKeepBuffs = isPlayer() && isInFightEvent() && !getFightEvent().loseBuffsOnDeath(getActingPlayer());
		if (isPhoenixBlessed())
		{
			if (isCharmOfLuckAffected())
			{
				stopEffects(EffectType.CHARM_OF_LUCK);
			}
			if (isNoblesseBlessed())
			{
				stopEffects(EffectType.NOBLESSE_BLESSING);
			}
		}
		else if (fightEventKeepBuffs)
		{}
		else if (isNoblesseBlessed())
		{
			stopEffects(EffectType.NOBLESSE_BLESSING);
			if (isCharmOfLuckAffected())
			{
				stopEffects(EffectType.CHARM_OF_LUCK);
			}
		}
		else
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}

		if (isPlayer())
		{
			if (getActingPlayer().getAgathionId() != 0)
			{
				getActingPlayer().setAgathionId(0);
			}
			
			if (!getActingPlayer().isNotifyQuestOfDeathEmpty())
			{
				for (final QuestState qs : getActingPlayer().getNotifyQuestOfDeath())
				{
					qs.getQuest().notifyDeath((killer == null ? this : killer), this, qs);
				}
			}
		}
		else if (isPet() && isPhoenixBlessed())
		{
			if (getActingPlayer() != null)
			{
				getActingPlayer().reviveRequest(getActingPlayer(), null, 0, true, getActingPlayer().getFarmSystem().isAutofarming());
			}
		}
		
		if (getReflectionId() > 0)
		{
			final Reflection instance = ReflectionManager.getInstance().getReflection(getReflectionId());
			if (instance != null)
			{
				instance.notifyDeath(killer, this);
			}
		}

		if (killer != null && killer.isPlayable())
		{
			final Player player = killer.getActingPlayer();
			if (player != null)
			{
				player.onKillUpdatePvPKarma(this);
			}
		}
		
		if (isPlayer())
		{
			if (!(isInsideZone(ZoneId.PVP) && !getActingPlayer().isInSiege()))
			{
				getActingPlayer().onDieUpdateKarma();
			}
		}
		getAI().notifyEvent(CtrlEvent.EVT_DEAD);
	}

	public boolean checkIfPvP(Creature target)
	{
		if (target == null)
		{
			return false;
		}
		if (target == this)
		{
			return false;
		}
		if (!target.isPlayable())
		{
			return false;
		}

		final Player player = getActingPlayer();
		if (player == null)
		{
			return false;
		}
		if (player.getKarma() != 0)
		{
			return false;
		}

		final Player targetPlayer = target.getActingPlayer();
		if (targetPlayer == null)
		{
			return false;
		}
		if (targetPlayer == this)
		{
			return false;
		}
		if (targetPlayer.getKarma() != 0)
		{
			return false;
		}
		if (targetPlayer.getPvpFlag() == 0)
		{
			return false;
		}

		return true;
	}
	
	public final boolean isNoblesseBlessed()
	{
		return isAffected(EffectFlag.NOBLESS_BLESSING);
	}

	public final boolean isPhoenixBlessed()
	{
		return isAffected(EffectFlag.PHOENIX_BLESSING);
	}

	public boolean isSilentMoving()
	{
		return isAffected(EffectFlag.SILENT_MOVE);
	}

	public final boolean isProtectionBlessingAffected()
	{
		return isAffected(EffectFlag.PROTECTION_BLESSING);
	}

	public final boolean isCharmOfLuckAffected()
	{
		return isAffected(EffectFlag.CHARM_OF_LUCK);
	}

	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		_effects.updateEffectIcons(partyOnly, true);
	}

	public boolean isLockedTarget()
	{
		return _lockedTarget != null;
	}

	public Creature getLockedTarget()
	{
		return _lockedTarget;
	}

	public void setLockedTarget(Creature cha)
	{
		_lockedTarget = cha;
	}

	public void setTransferDamageTo(Player val)
	{
		transferDmgTo = val;
	}

	public Player getTransferingDamageTo()
	{
		return transferDmgTo;
	}

	public abstract int getKarma();

	public abstract byte getPvpFlag();

	public abstract boolean useMagic(Skill skill, boolean forceUse, boolean dontMove, boolean msg);

	public abstract void store();

	public abstract void storeEffect(boolean storeEffects);

	public abstract void restoreEffects();

	@Override
	public boolean isPlayable()
	{
		return true;
	}

	public void addHitmanTarget(int hitmanTarget)
	{
		_hitmanTargets.add(hitmanTarget);
	}
	
	public void removeHitmanTarget(int hitmanTarget)
	{
		String line = "";
		int amount = 0;
		for (final int charId : _hitmanTargets)
		{
			if (charId == hitmanTarget)
			{
				continue;
			}
			amount++;
			line += "" + charId + "";
			if (amount < (_hitmanTargets.size() - 1))
			{
				line += ";";
			}
			
		}
		_hitmanTargets.clear();
		
		if (!line.isEmpty())
		{
			final String[] targets = line.split(";");
			for (final String charId : targets)
			{
				_hitmanTargets.add(Integer.parseInt(charId));
			}
		}
	}

	public List<Integer> getHitmanTargets()
	{
		return _hitmanTargets;
	}
	
	public String saveHitmanTargets()
	{
		if (_hitmanTargets != null && !_hitmanTargets.isEmpty())
		{
			String line = "";
			int amount = 0;
			for (final int charId : _hitmanTargets)
			{
				amount++;
				line += "" + charId + "";
				if (amount < _hitmanTargets.size())
				{
					line += ";";
				}
			}
			return line;
		}
		return null;
	}
	
	public void loadHitmanTargets(String line)
	{
		if (line != null && !line.isEmpty())
		{
			final String[] targets = line.split(";");
			for (final String charId : targets)
			{
				_hitmanTargets.add(Integer.parseInt(charId));
			}
		}
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
}