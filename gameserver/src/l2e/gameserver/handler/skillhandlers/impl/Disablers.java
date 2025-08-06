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
package l2e.gameserver.handler.skillhandlers.impl;

import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.ShotType;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.SiegeSummonInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class Disablers implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.STUN, SkillType.ROOT, SkillType.SLEEP, SkillType.CONFUSION, SkillType.AGGDAMAGE, SkillType.AGGREDUCE, SkillType.AGGREDUCE_CHAR, SkillType.AGGREMOVE, SkillType.MUTE, SkillType.CONFUSE_MOB_ONLY, SkillType.PARALYZE, SkillType.ERASE, SkillType.BETRAY, SkillType.DISARM
	};

	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		final SkillType type = skill.getSkillType();

		byte shld = 0;
		final boolean ss = skill.useSoulShot() && activeChar.isChargedShot(ShotType.SOULSHOTS);
		final boolean sps = skill.useSpiritShot() && activeChar.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.useSpiritShot() && activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);

		for (final GameObject obj : targets)
		{
			if (!(obj instanceof Creature))
			{
				continue;
			}
			Creature target = (Creature) obj;
			if (target.isDead() || (target.isInvul() && !target.isParalyzed()))
			{
				continue;
			}

			shld = Formulas.calcShldUse(activeChar, target, skill);

			switch (type)
			{
				case BETRAY :
				{
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
					{
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss), true);
					}
					else
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
					break;
				}
				case ROOT :
				case DISARM :
				case STUN :
				case SLEEP :
				case PARALYZE :
				{
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
					{
						target = activeChar;
					}

					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
					{
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss), true);
					}
					else
					{
						if (activeChar.isPlayer())
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case CONFUSION :
				case MUTE :
				{
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
					{
						target = activeChar;
					}

					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
					{
						final Effect[] effects = target.getAllEffects();
						for (final Effect e : effects)
						{
							if ((e != null) && (e.getSkill() != null) && (e.getSkill().getSkillType() == type))
							{
								e.exit();
							}
						}
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss), true);
					}
					else
					{
						if (activeChar.isPlayer())
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case CONFUSE_MOB_ONLY :
				{
					if (target.isAttackable())
					{
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
						{
							final Effect[] effects = target.getAllEffects();
							for (final Effect e : effects)
							{
								if (e.getSkill().getSkillType() == type)
								{
									e.exit();
								}
							}
							skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss), true);
						}
						else
						{
							if (activeChar.isPlayer())
							{
								final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
								sm.addCharName(target);
								sm.addSkillName(skill);
								activeChar.sendPacket(sm);
							}
						}
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					}
					break;
				}
				case AGGDAMAGE :
				{
					if (!target.isAutoAttackable(activeChar, false))
					{
						continue;
					}
					
					if (target.hasAI() && target.isNpc() && skill.getId() == 51)
					{
						((DefaultAI) target.getAI()).setNotifyFriend(false);
					}
					skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss), true);
					Formulas.calcLethalHit(activeChar, target, skill);
					break;
				}
				case AGGREDUCE :
				{
					if (target.isAttackable())
					{
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss), true);

						final double aggdiff = ((Attackable) target).getAggroList().getHating(activeChar) - target.calcStat(Stats.AGGRESSION, ((Attackable) target).getAggroList().getHating(activeChar), target, skill);

						if (skill.getPower() > 0)
						{
							((Attackable) target).getAggroList().reduceHate(null, (int) skill.getPower(), true);
						}
						else if (aggdiff > 0)
						{
							((Attackable) target).getAggroList().reduceHate(null, (int) aggdiff, true);
						}
					}
					break;
				}
				case AGGREDUCE_CHAR :
				{
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
					{
						if (target.isAttackable())
						{
							final Attackable targ = (Attackable) target;
							targ.getAggroList().stopHating(activeChar);
							if ((targ.getAggroList().getMostHated() == null) && targ.hasAI() && (targ.getAI() instanceof DefaultAI))
							{
								((DefaultAI) targ.getAI()).setGlobalAggro(-25);
								targ.clearAggroList(false);
								targ.getAI().setIntention(CtrlIntention.ACTIVE);
								targ.setWalking();
							}
						}
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss), true);
					}
					else
					{
						if (activeChar.isPlayer())
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar, 0);
					}
					break;
				}
				case AGGREMOVE :
				{
					final var isAddToBlockList = skill.hasUnAggroEffects();
					if (skill.getTargetType() == TargetType.SELF)
					{
						final int maxTargets = skill.getAffectLimit();
						int targetList = 0;
						for (final var npc : World.getInstance().getAroundNpc(activeChar, skill.getAffectRange(), 200))
						{
							if (npc.isAttackable() && !npc.isRaid())
							{
								if ((maxTargets > 0) && (targetList >= maxTargets))
								{
									break;
								}
								targetList++;
								final var tgt = ((Attackable) npc);
								if (Formulas.calcSkillSuccess(activeChar, tgt, skill, shld, ss, sps, bss))
								{
									if (skill.getTargetType() == TargetType.UNDEAD || skill.getId() == 1034)
									{
										if (tgt.isUndead())
										{
											tgt.getAggroList().reduceHate(null, tgt.getAggroList().getHating(tgt.getAggroList().getMostHated()), !isAddToBlockList);
											if (skill.hasEffects())
											{
												skill.getEffects(activeChar, tgt, true);
											}
										}
									}
									else
									{
										tgt.getAggroList().reduceHate(null, tgt.getAggroList().getHating(tgt.getAggroList().getMostHated()), !isAddToBlockList);
										if (skill.hasEffects())
										{
											skill.getEffects(activeChar, tgt, true);
										}
									}
								}
								else
								{
									if (activeChar.isPlayer())
									{
										final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
										sm.addCharName(tgt);
										sm.addSkillName(skill);
										activeChar.sendPacket(sm);
									}
									
									if (!tgt.isInBlockList(activeChar))
									{
										tgt.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar, 0);
									}
								}
							}
							else
							{
								npc.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar, 0);
							}
						}
					}
					else
					{
						if (target.isAttackable() && !target.isRaid())
						{
							final var tgt = ((Attackable) target);
							if (skill.getId() == 1049 && !tgt.isInBlockList(activeChar))
							{
								continue;
							}
							
							if (Formulas.calcSkillSuccess(activeChar, tgt, skill, shld, ss, sps, bss))
							{
								if (skill.getTargetType() == TargetType.UNDEAD)
								{
									if (tgt.isUndead())
									{
										tgt.getAggroList().reduceHate(null, tgt.getAggroList().getHating(tgt.getAggroList().getMostHated()), !isAddToBlockList);
										if (skill.hasEffects())
										{
											skill.getEffects(activeChar, tgt, true);
										}
									}
								}
								else
								{
									tgt.getAggroList().reduceHate(null, tgt.getAggroList().getHating(tgt.getAggroList().getMostHated()), !isAddToBlockList);
									if (skill.hasEffects())
									{
										skill.getEffects(activeChar, tgt, true);
									}
								}
							}
							else
							{
								if (activeChar.isPlayer())
								{
									final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
									sm.addCharName(tgt);
									sm.addSkillName(skill);
									activeChar.sendPacket(sm);
								}
								
								if (!tgt.isInBlockList(activeChar))
								{
									tgt.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar, 0);
								}
							}
						}
						else
						{
							target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar, 0);
						}
					}
					break;
				}
				case ERASE :
				{
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss) && !(target instanceof SiegeSummonInstance))
					{
						final Player summonOwner = ((Summon) target).getOwner();
						final Summon summon = summonOwner.getSummon();
						if (summon != null)
						{
							if (summon.isPhoenixBlessed())
							{
								if (summon.isNoblesseBlessed())
								{
									summon.stopEffects(EffectType.NOBLESSE_BLESSING);
								}
							}
							else if (summon.isNoblesseBlessed())
							{
								summon.stopEffects(EffectType.NOBLESSE_BLESSING);
							}
							else
							{
								summon.stopAllEffectsExceptThoseThatLastThroughDeath();
							}
							summon.abortAttack();
							summon.abortCast();
							summon.unSummon(summonOwner);
							summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
						}
					}
					else
					{
						if (activeChar.isPlayer())
						{
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
			}
		}

		if (skill.hasSelfEffects())
		{
			final Effect effect = activeChar.getFirstEffect(skill.getId());
			if ((effect != null) && effect.isSelfEffect())
			{
				effect.exit();
			}
			skill.getEffectsSelf(activeChar);
		}

		activeChar.setChargedShot(bss ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
	}

	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}