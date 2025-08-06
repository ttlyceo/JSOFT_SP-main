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
package l2e.gameserver.handler.effecthandlers.impl;

import l2e.commons.util.Util;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.effects.EffectType;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.network.serverpackets.StatusUpdate;

public class RebalanceHP extends Effect
{
	public RebalanceHP(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.REBALANCE_HP;
	}
	
	@Override
	public boolean onStart()
	{
		if (!getEffector().isPlayer() || !getEffector().isInParty())
		{
			return false;
		}

		double fullHP = 0;
		double currentHPs = 0;
		final Party party = getEffector().getParty();
		for (final Player member : party.getMembers())
		{
			if (member.isDead() || !Util.checkIfInRange(getSkill().getAffectRange(), getEffector(), member, true))
			{
				continue;
			}

			fullHP += member.getMaxHp();
			currentHPs += member.getCurrentHp();
			
			if (member.hasSummon())
			{
				final Summon summon = member.getSummon();
				if (summon != null && !summon.isDead() && Util.checkIfInRange(getSkill().getAffectRange(), getEffector(), summon, true))
				{
					fullHP += summon.getMaxHp();
					currentHPs += summon.getCurrentHp();
				}
			}
		}

		final double percentHP = currentHPs / fullHP;
		for (final Player member : party.getMembers())
		{
			if (member.isDead() || !Util.checkIfInRange(getSkill().getAffectRange(), getEffector(), member, true))
			{
				continue;
			}
			
			if (member.hasSummon())
			{
				final Summon summon = member.getSummon();
				if (summon != null && !summon.isDead() && Util.checkIfInRange(getSkill().getAffectRange(), getEffector(), summon, true))
				{
					double newHP = summon.getMaxHp() * percentHP;
					if (newHP > summon.getCurrentHp())
					{
						if (summon.getCurrentHp() > summon.getMaxRecoverableHp())
						{
							newHP = summon.getCurrentHp();
						}
						else if (newHP > summon.getMaxRecoverableHp())
						{
							newHP = summon.getMaxRecoverableHp();
						}
					}
					summon.setCurrentHp(newHP);
				}
			}

			double newHP = member.getMaxHp() * percentHP;
			if (newHP > member.getCurrentHp())
			{
				if (member.getCurrentHp() > member.getMaxRecoverableHp())
				{
					newHP = member.getCurrentHp();
				}
				else if (newHP > member.getMaxRecoverableHp())
				{
					newHP = member.getMaxRecoverableHp();
				}
			}

			member.setCurrentHp(newHP);
			final var su = member.makeStatusUpdate(StatusUpdate.CUR_HP);
			member.sendPacket(su);
		}
		return true;
	}
}