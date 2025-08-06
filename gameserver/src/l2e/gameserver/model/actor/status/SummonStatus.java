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
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.entity.Duel;
import l2e.gameserver.model.stats.Stats;

public class SummonStatus extends PlayableStatus
{
	public SummonStatus(Summon activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false, true);
	}

	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean broadcastPacket)
	{
		if (attacker == null || getActiveChar().isDead())
		{
			return;
		}
		
		final Player attackerPlayer = attacker.getActingPlayer();
		if (attackerPlayer != null && (getActiveChar().getOwner() == null || getActiveChar().getOwner().getDuelId() != attackerPlayer.getDuelId()))
		{
			attackerPlayer.setDuelState(Duel.DUELSTATE_INTERRUPTED);
		}
		
		final Player caster = getActiveChar().getTransferingDamageTo();
		if (getActiveChar().getOwner().getParty() != null)
		{
			if (caster != null && Util.checkIfInRange(1000, getActiveChar(), caster, true) && !caster.isDead() && getActiveChar().getParty().getMembers().contains(caster))
			{
				int transferDmg = 0;

				transferDmg = (int) value * (int) getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_TO_PLAYER, 0, null, null) / 100;
				transferDmg = Math.min((int) caster.getCurrentHp() - 1, transferDmg);
				if (transferDmg > 0)
				{
					int membersInRange = 0;
					for (final Player member : caster.getParty().getMembers())
					{
						if (Util.checkIfInRange(1000, member, caster, false) && member != caster)
						{
							membersInRange++;
						}
					}
					if (attacker instanceof Playable && caster.getCurrentCp() > 0)
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
					}
					
					if (caster == getActiveChar().getOwner() && Util.checkIfInRange(1000, getActiveChar(), caster, true) && !caster.isDead())
					{
						if (caster.getChanceSkills() != null)
						{
							caster.getChanceSkills().onHit(attacker, (int) value, true, false);
						}
					}
				}
			}
		}
		else if (caster != null && caster == getActiveChar().getOwner() && Util.checkIfInRange(1000, getActiveChar(), caster, true) && !caster.isDead())
		{
			int transferDmg = 0;

			transferDmg = (int) value * (int) getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_TO_PLAYER, 0, null, null) / 100;
			transferDmg = Math.min((int) caster.getCurrentHp() - 1, transferDmg);
			if (transferDmg > 0)
			{
				if (attacker instanceof Playable && caster.getCurrentCp() > 0)
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

				caster.reduceCurrentHp(transferDmg, attacker, null);
				value -= transferDmg;
				
				if (caster.getChanceSkills() != null)
				{
					caster.getChanceSkills().onHit(attacker, (int) value, true, false);
				}
			}
		}
		super.reduceHp(value, attacker, awake, isDOT, isHPConsumption, broadcastPacket);
	}
	
	@Override
	public Summon getActiveChar()
	{
		return (Summon) super.getActiveChar();
	}
}