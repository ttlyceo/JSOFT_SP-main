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

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class BabyPetInstance extends PetInstance
{
	private List<SkillHolder> _buffs = null;
	private SkillHolder _majorHeal = null;
	private SkillHolder _minorHeal = null;
	private SkillHolder _recharge = null;
	private Future<?> _castTask;
	private boolean _bufferMode = true;
	
	public BabyPetInstance(int objectId, NpcTemplate template, Player owner, ItemInstance control)
	{
		super(objectId, template, owner, control);
		setInstanceType(InstanceType.BabyPetInstance);
	}

	public BabyPetInstance(int objectId, NpcTemplate template, Player owner, ItemInstance control, int level)
	{
		super(objectId, template, owner, control, level);
		setInstanceType(InstanceType.BabyPetInstance);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		Skill skill;
		for (final var psl : PetsParser.getInstance().getPetData(getId()).getAvailableSkills())
		{
			skill = PetsParser.getInstance().getPetData(getId()).getAvailableSkill(psl.getId(), getLevel());
			if (skill != null)
			{
				if ((skill.getId() == 5771) || (skill.getId() == 5753))
				{
					continue;
				}

				switch (skill.getSkillType())
				{
					case BUFF :
						if (skill.getId() >= 23167 && skill.getId() <= 23169)
						{
							continue;
						}
						
						if (_buffs == null)
						{
							_buffs = new ArrayList<>();
						}
						_buffs.add(new SkillHolder(skill));
						break;
					case DUMMY :
						if (skill.hasEffectType(EffectType.MANAHEAL_BY_LEVEL))
						{
							_recharge = new SkillHolder(skill);
						}
						else if (skill.hasEffectType(EffectType.HEAL))
						{
							if (skill.isPetMajorHeal())
							{
								_majorHeal = new SkillHolder(skill);
							}
							else
							{
								_minorHeal = new SkillHolder(skill);
							}
						}
						break;
				}
			}
		}
		startCastTask();
	}

	@Override
	protected void onDeath(Creature killer)
	{
		stopCastTask();
		abortCast();
		super.onDeath(killer);
	}

	@Override
	public void unSummon(Player owner)
	{
		stopCastTask();
		super.unSummon(owner);
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
		startCastTask();
	}

	@Override
	public void onDecay()
	{
		super.onDecay();

		if (_buffs != null)
		{
			_buffs.clear();
		}
	}
	
	@Override
	public void onTeleported()
	{
		stopCastTask();
		if (_buffs != null)
		{
			_buffs.clear();
		}
		super.onTeleported();
	}

	private final void startCastTask()
	{
		if ((_majorHeal != null) || (_buffs != null) || ((_recharge != null) && (_castTask == null) && !isDead()))
		{
			_castTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new CastTask(this), 3000, 2000);
		}
	}

	@Override
	public void switchMode()
	{
		_bufferMode = !_bufferMode;
	}

	public boolean isInSupportMode()
	{
		return _bufferMode;
	}

	private final void stopCastTask()
	{
		if (_castTask != null)
		{
			_castTask.cancel(false);
			_castTask = null;
		}
	}

	protected void castSkill(Skill skill)
	{
		final boolean previousFollowStatus = isInFollowStatus();
		if (!previousFollowStatus && !isInsideRadius(getOwner(), skill.getCastRange(), true, true))
		{
			return;
		}
		
		if (!checkDoCastConditions(skill, false) || !GeoEngine.getInstance().canSeeTarget(this, getOwner()))
		{
			return;
		}

		setTarget(getOwner());
		useMagic(skill, false, false, true);

		final var msg = SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1);
		msg.addSkillName(skill);
		sendPacket(msg);

		if (previousFollowStatus != isInFollowStatus())
		{
			setFollowStatus(previousFollowStatus);
		}
	}

	private class CastTask implements Runnable
	{
		private final BabyPetInstance _baby;
		private final List<Skill> _currentBuffs = new ArrayList<>();

		public CastTask(BabyPetInstance baby)
		{
			_baby = baby;
		}

		@Override
		public void run()
		{
			if (_baby == null || _baby.isDead())
			{
				return;
			}
			
			final var owner = _baby.getOwner();
			if ((owner != null) && !owner.isDead() && !owner.isInvul() && !_baby.isCastingNow() && !_baby.isBetrayed() && !_baby.isMuted() && !_baby.isOutOfControl() && (_baby.getAI().getIntention() != CtrlIntention.CAST) && (_baby.getAI().getIntention() != CtrlIntention.ATTACK))
			{
				Skill skill = null;

				if (_majorHeal != null || _minorHeal != null)
				{
					final double hpPercent = owner.getCurrentHp() / owner.getMaxHp();
					double percent = 0;
					if (_majorHeal != null)
					{
						percent = PetsParser.getInstance().getPetData(_baby.getId()).getHpPercent(_majorHeal.getId(), _majorHeal.getLvl());
						skill = _majorHeal.getSkill();
						if (!_baby.isSkillDisabled(skill) && hpPercent < percent && _baby.getCurrentMp() >= skill.getMpConsume())
						{
							castSkill(skill);
							return;
						}
					}
					
					if (_minorHeal != null)
					{
						percent = PetsParser.getInstance().getPetData(_baby.getId()).getHpPercent(_minorHeal.getId(), _minorHeal.getLvl());
						skill = _minorHeal.getSkill();
						if (!_baby.isSkillDisabled(skill) && hpPercent < percent && _baby.getCurrentMp() >= skill.getMpConsume())
						{
							castSkill(skill);
							return;
						}
					}
				}

				if (_baby.getFirstEffect(5771) == null && isInSupportMode())
				{
					if ((_buffs != null) && !_buffs.isEmpty())
					{
						for (final var i : _buffs)
						{
							skill = i.getSkill();
							if (_baby.isSkillDisabled(skill) || skill.getTargetType() == TargetType.SELF)
							{
								continue;
							}
							if (_baby.getCurrentMp() >= skill.getMpConsume())
							{
								_currentBuffs.add(skill);
							}
						}
					}

					if (!_currentBuffs.isEmpty())
					{
						final List<Skill> skillList = new ArrayList<>();
						Skill rndSkill = null;
						Skill currentSkill;
						
						if (owner.getEffectList().hasBuffs())
						{
							for (final var e : owner.getEffectList().getBuffs())
							{
								if (e == null)
								{
									continue;
								}
								
								currentSkill = e.getSkill();
								
								if (currentSkill.isDebuff() || currentSkill.isPassive() || currentSkill.isToggle())
								{
									continue;
								}
								
								for (final Skill sk : _currentBuffs)
								{
									if (sk == null)
									{
										continue;
									}
									
									if ((currentSkill.getId() == sk.getId()) && (currentSkill.getLevel() >= sk.getLevel()))
									{
										if (!skillList.contains(sk))
										{
											skillList.add(sk);
										}
										continue;
									}
									else if ((owner.getEffectList().getAllBlockedBuffSlots() != null) && owner.getEffectList().getAllBlockedBuffSlots().contains(sk.getEffectTemplates()[0].abnormalType))
									{
										if (!skillList.contains(sk))
										{
											skillList.add(sk);
										}
										continue;
									}
									else
									{
										if (sk.hasEffects() && !"none".equals(sk.getEffectTemplates()[0].abnormalType) && e.getAbnormalType().equalsIgnoreCase(sk.getEffectTemplates()[0].abnormalType) && (e.getAbnormalLvl() >= sk.getEffectTemplates()[0].abnormalLvl))
										{
											if (!skillList.contains(sk))
											{
												skillList.add(sk);
											}
											continue;
										}
									}
								}
							}
						}
						
						if (!skillList.isEmpty())
						{
							for (final var sk : skillList)
							{
								_currentBuffs.remove(sk);
							}
						}
						skillList.clear();
						
						if (!_currentBuffs.isEmpty())
						{
							rndSkill = _currentBuffs.get(Rnd.get(_currentBuffs.size()));
							if (rndSkill != null)
							{
								castSkill(rndSkill);
							}
							_currentBuffs.clear();
							return;
						}
					}
				}

				final boolean canCast = ((owner.isInCombat() && Config.ALLOW_PETS_RECHARGE_ONLY_COMBAT) || !Config.ALLOW_PETS_RECHARGE_ONLY_COMBAT);
				if ((_recharge != null) && canCast && ((owner.getCurrentMp() / owner.getMaxMp()) < 0.6) && (Rnd.get(100) <= 60))
				{
					skill = _recharge.getSkill();
					if (!_baby.isSkillDisabled(skill) && (_baby.getCurrentMp() >= skill.getMpConsume()))
					{
						castSkill(skill);
						return;
					}
				}
			}
		}
	}
}