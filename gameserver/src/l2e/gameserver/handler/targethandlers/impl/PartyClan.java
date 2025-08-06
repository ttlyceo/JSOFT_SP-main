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
package l2e.gameserver.handler.targethandlers.impl;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.handler.targethandlers.ITargetTypeHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;

public class PartyClan implements ITargetTypeHandler
{
	@Override
	public GameObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		if (onlyFirst)
		{
			return new Creature[]
			{
			        activeChar
			};
		}
		final Player player = activeChar.getActingPlayer();

		if (player == null)
		{
			return EMPTY_TARGET_LIST;
		}

		targetList.add(player);

		final int radius = skill.getAffectRange();
		final boolean hasClan = player.getClan() != null;
		final boolean hasParty = player.isInParty();

		if (Skill.addSummon(activeChar, player, radius, false, false))
		{
			targetList.add(player.getSummon());
		}

		if (!(hasClan || hasParty))
		{
			return targetList.toArray(new Creature[targetList.size()]);
		}

		final int maxTargets = skill.getAffectLimit();
		for (final Player obj : World.getInstance().getAroundPlayers(activeChar, radius, 200))
		{
			if (obj == null)
			{
				continue;
			}

			if (player.isInOlympiadMode())
			{
				if (!obj.isInOlympiadMode())
				{
					continue;
				}
				if (player.getOlympiadGameId() != obj.getOlympiadGameId())
				{
					continue;
				}
				if (player.getOlympiadSide() != obj.getOlympiadSide())
				{
					continue;
				}
			}

			if (player.isInDuel())
			{
				if (player.getDuelId() != obj.getDuelId())
				{
					continue;
				}

				if (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))
				{
					continue;
				}
			}

			if (!((hasClan && (obj.getClanId() == player.getClanId())) || (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() == obj.getParty().getLeaderObjectId()))))
			{
				continue;
			}

			if (!player.checkPvpSkill(obj, skill))
			{
				continue;
			}

			for (final AbstractFightEvent e : player.getFightEvents())
			{
				if (e != null && !e.canUseMagic(player, obj, skill))
				{
					continue;
				}
			}

			var e = player.getPartyTournament();
			if (e != null && !e.canUseMagic(player, obj, skill))
			{
				continue;
			}

			if (!onlyFirst && Skill.addSummon(activeChar, obj, radius, false, false))
			{
				targetList.add(obj.getSummon());
			}

			if (!Skill.addCharacter(activeChar, obj, radius, false))
			{
				continue;
			}

			if (onlyFirst)
			{
				return new Creature[]
				{
				        obj
				};
			}

			if ((maxTargets > 0) && (targetList.size() >= maxTargets))
			{
				break;
			}
			targetList.add(obj);
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}

	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.PARTY_CLAN;
	}
}