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
package l2e.gameserver.model.actor.status;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.DuelManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.stat.PcStat;
import l2e.gameserver.model.entity.Duel;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class PcStatus extends PlayableStatus
{
	private double _currentCp = 0;

	public PcStatus(Player activeChar)
	{
		super(activeChar);
	}

	@Override
	public final void reduceCp(int value)
	{
		if (getCurrentCp() > value)
		{
			setCurrentCp(getCurrentCp() - value);
		}
		else
		{
			setCurrentCp(0);
		}
	}

	@Override
	public final void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false, false, true);
	}

	@Override
	public final void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean broadcastPacket)
	{
		reduceHp(value, attacker, awake, isDOT, isHPConsumption, false, broadcastPacket);
	}

	public final void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean ignoreCP, boolean broadcastPacket)
	{
		if (getActiveChar().isDead())
		{
			return;
		}

		if (Config.OFFLINE_MODE_NO_DAMAGE && (getActiveChar().getClient() != null) && getActiveChar().getClient().isDetached() && ((Config.OFFLINE_TRADE_ENABLE && ((getActiveChar().getPrivateStoreType() == Player.STORE_PRIVATE_SELL) || (getActiveChar().getPrivateStoreType() == Player.STORE_PRIVATE_BUY))) || (Config.OFFLINE_CRAFT_ENABLE && (getActiveChar().isInCraftMode() || (getActiveChar().getPrivateStoreType() == Player.STORE_PRIVATE_MANUFACTURE)))))
		{
			return;
		}

		if (getActiveChar().isInvul() && !(isDOT || isHPConsumption))
		{
			return;
		}

		if (!isHPConsumption)
		{
			getActiveChar().stopEffectsOnDamage(awake);

			if (getActiveChar().isInCraftMode() || getActiveChar().isInStoreMode())
			{
				getActiveChar().setPrivateStoreType(Player.STORE_PRIVATE_NONE);
				getActiveChar().standUp();
				getActiveChar().broadcastUserInfo(true);
			}
			else if (getActiveChar().isSitting())
			{
				getActiveChar().standUp();
			}
		}

		int fullValue = (int) value;
		int tDmg = 0;
		int mpDam = 0;

		if ((attacker != null) && (attacker != getActiveChar()))
		{
			final Player attackerPlayer = attacker.getActingPlayer();

			if (attackerPlayer != null)
			{
				if (attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
				{
					return;
				}

				if (getActiveChar().isInDuel())
				{
					if (getActiveChar().getDuelState() == Duel.DUELSTATE_DEAD)
					{
						return;
					}
					else if (getActiveChar().getDuelState() == Duel.DUELSTATE_WINNER)
					{
						return;
					}

					if (attackerPlayer.getDuelId() != getActiveChar().getDuelId())
					{
						getActiveChar().setDuelState(Duel.DUELSTATE_INTERRUPTED);
					}
				}
			}

			final Summon summon = getActiveChar().getSummon();
			if (getActiveChar().hasServitor() && Util.checkIfInRange(1000, getActiveChar(), summon, true))
			{
				tDmg = ((int) value * (int) getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null)) / 100;

				tDmg = Math.min((int) summon.getCurrentHp() - 1, tDmg);
				if (tDmg > 0)
				{
					summon.reduceCurrentHp(tDmg, attacker, null);
					value -= tDmg;
					fullValue = (int) value;
				}
			}

			mpDam = ((int) value * (int) getActiveChar().getStat().calcStat(Stats.MANA_SHIELD_PERCENT, 0, null, null)) / 100;

			if (mpDam > 0)
			{
				mpDam = (int) (value - mpDam);
				if (mpDam > getActiveChar().getCurrentMp())
				{
					getActiveChar().sendPacket(SystemMessageId.MP_BECAME_0_ARCANE_SHIELD_DISAPPEARING);
					getActiveChar().stopSkillEffects(1556);
					value = mpDam - getActiveChar().getCurrentMp();
					getActiveChar().setCurrentMp(0);
				}
				else
				{
					getActiveChar().reduceCurrentMp(mpDam);
					final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.ARCANE_SHIELD_DECREASED_YOUR_MP_BY_S1_INSTEAD_OF_HP);
					smsg.addNumber(mpDam);
					getActiveChar().sendPacket(smsg);
					return;
				}
			}

			final Player caster = getActiveChar().getTransferingDamageTo();
			if ((caster != null) && (getActiveChar().getParty() != null) && Util.checkIfInRange(1000, getActiveChar(), caster, true) && !caster.isDead() && (getActiveChar() != caster) && getActiveChar().getParty().getMembers().contains(caster))
			{
				int transferDmg = 0;

				transferDmg = ((int) value * (int) getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_TO_PLAYER, 0, null, null)) / 100;
				transferDmg = Math.min((int) caster.getCurrentHp() - 1, transferDmg);
				if (transferDmg > 0)
				{
					int membersInRange = 0;
					if (caster.getParty() != null)
					{
						for (final Player member : caster.getParty().getMembers())
						{
							if (Util.checkIfInRange(1000, member, caster, false) && (member != caster))
							{
								membersInRange++;
							}
						}
					}
					
					if ((attacker instanceof Playable) && (caster.getCurrentCp() > 0))
					{
						if (caster.getCurrentCp() > transferDmg)
						{
							caster.getStatus().reduceCp(transferDmg);
						}
						else
						{
							transferDmg = (int) (transferDmg - caster.getCurrentCp());
							caster.getStatus().reduceCp((int) caster.getCurrentCp());
						}
					}

					if (membersInRange > 0)
					{
						caster.reduceCurrentHp(transferDmg / membersInRange, attacker, null);
						value -= transferDmg;
						fullValue = (int) value;
					}
				}
			}

			if (!ignoreCP && (attacker instanceof Playable))
			{
				if (getCurrentCp() >= value)
				{
					setCurrentCp(getCurrentCp() - value, broadcastPacket);
					value = 0;
				}
				else
				{
					value -= getCurrentCp();
					setCurrentCp(0, false);
				}
			}

			if ((fullValue > 0) && !isDOT)
			{
				SystemMessage smsg;
				smsg = SystemMessage.getSystemMessage(SystemMessageId.C1_RECEIVED_DAMAGE_OF_S3_FROM_C2);
				smsg.addString(getActiveChar().getName(null));
				smsg.addCharName(attacker);
				smsg.addNumber(fullValue);
				getActiveChar().sendPacket(smsg);

				if (tDmg > 0)
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.C1_RECEIVED_DAMAGE_OF_S3_FROM_C2);
					smsg.addString(getActiveChar().getSummon().getName(null));
					smsg.addCharName(attacker);
					smsg.addNumber(tDmg);
					getActiveChar().sendPacket(smsg);

					if (attackerPlayer != null)
					{
						smsg = SystemMessage.getSystemMessage(SystemMessageId.GIVEN_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_SERVITOR);
						smsg.addNumber(fullValue);
						smsg.addNumber(tDmg);
						attackerPlayer.sendPacket(smsg);
					}
				}
			}
		}

		if (value > 0)
		{
			value = getCurrentHp() - value;
			if (isHPConsumption)
			{
				value = Math.max(value, 1);
			}
			
			if (value <= 0)
			{
				if (getActiveChar().isInDuel())
				{
					getActiveChar().disableAllSkills();
					stopHpMpRegeneration();
					if (attacker != null)
					{
						attacker.getAI().setIntention(CtrlIntention.ACTIVE);
						attacker.sendActionFailed();
					}
					DuelManager.getInstance().onPlayerDefeat(getActiveChar());
					value = 1;
				}
				else
				{
					value = 0;
				}
			}
			setCurrentHp(value, broadcastPacket);
		}

		if (getActiveChar().getCurrentHp() < 0.5 && !getActiveChar().isUndying())
		{
			getActiveChar().abortAttack();
			getActiveChar().abortCast();

			if (getActiveChar().isInOlympiadMode())
			{
				stopHpMpRegeneration();
				getActiveChar().setIsDead(true);
				getActiveChar().setIsPendingRevive(true);
				getActiveChar().getOlympiadGame().checkWinner();
				if (getActiveChar().hasSummon())
				{
					getActiveChar().getSummon().cancelAction();
				}
				return;
			}

			getActiveChar().doDie(attacker);
			if (!Config.DISABLE_TUTORIAL)
			{
				final var qs = getActiveChar().getQuestState("_255_Tutorial");
				if (qs != null)
				{
					final var quest = qs.getQuest();
					if (quest != null)
					{
						qs.getQuest().notifyEvent("CE30", null, getActiveChar());
					}
				}
			}
		}
	}

	@Override
	public final boolean setCurrentHp(double newHp, boolean broadcastPacket, boolean isDead)
	{
		final boolean result = super.setCurrentHp(newHp, broadcastPacket, isDead);

		if (!Config.DISABLE_TUTORIAL && (getCurrentHp() <= (getActiveChar().getStat().getMaxHp() * .3)))
		{
			final var qs = getActiveChar().getQuestState("_255_Tutorial");
			if (qs != null)
			{
				final var quest = qs.getQuest();
				if (quest != null)
				{
					quest.notifyEvent("CE45", null, getActiveChar());
				}
			}
		}
		return result;
	}

	@Override
	public final double getCurrentCp()
	{
		return _currentCp;
	}

	@Override
	public final void setCurrentCp(double newCp)
	{
		setCurrentCp(newCp, true);
	}

	public final void setCurrentCp(double newCp, boolean broadcastPacket)
	{
		final int currentCp = (int) getCurrentCp();
		final double maxCp = getActiveChar().getStat().getMaxCp();

		synchronized (this)
		{
			if (getActiveChar().isDead())
			{
				return;
			}

			if (newCp < 0)
			{
				newCp = 0;
			}

			if (newCp >= maxCp)
			{
				_currentCp = maxCp;
				_flagsRegenActive &= ~REGEN_FLAG_CP;
				if (!getActiveChar().getToggleList().hasEffects() && _flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				_currentCp = newCp;
				_flagsRegenActive |= REGEN_FLAG_CP;
				startHpMpRegeneration();
			}
		}

		if ((currentCp != _currentCp) && broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
		getActiveChar().getListeners().onChangeCurrentCp(currentCp, newCp);
	}

	@Override
	protected void doRegeneration()
	{
		if (!getActiveChar().isDead())
		{
			final PcStat charstat = getActiveChar().getStat();
			var isActive = false;
			if (getActiveChar().getToggleList().hasEffects())
			{
				getActiveChar().getToggleList().calcToggleDam();
				isActive = true;
			}
			
			if (getCurrentCp() < charstat.getMaxRecoverableCp())
			{
				setCurrentCp(getCurrentCp() + Formulas.calcCpRegen(getActiveChar()), false);
				isActive = true;
			}
			
			if (getCurrentHp() < charstat.getMaxRecoverableHp())
			{
				setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()), false, false);
				isActive = true;
			}
			
			if (getCurrentMp() < charstat.getMaxRecoverableMp())
			{
				setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()), false);
				isActive = true;
			}
			
			if (isActive)
			{
				getActiveChar().broadcastStatusUpdate();
			}
			else
			{
				stopHpMpRegeneration();
			}
		}
		else
		{
			stopHpMpRegeneration();
		}
	}

	@Override
	public Player getActiveChar()
	{
		return (Player) super.getActiveChar();
	}
}