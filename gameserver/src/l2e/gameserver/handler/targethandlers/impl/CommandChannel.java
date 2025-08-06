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
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.targets.TargetType;

public class CommandChannel implements ITargetTypeHandler
{
	@Override
	public GameObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		final Player player = activeChar.getActingPlayer();
		if (player == null)
		{
			return EMPTY_TARGET_LIST;
		}

		targetList.add(player);
		
		final int radius = skill.getAffectRange();
		final Party party = player.getParty();
		final boolean hasChannel = (party != null) && party.isInCommandChannel();
		
		if (Skill.addSummon(activeChar, player, radius, false, false))
		{
			targetList.add(player.getSummon());
		}
		
		if (party == null || party.getMemberCount() < 2)
		{
			return targetList.toArray(new Creature[targetList.size()]);
		}
		
		final int maxTargets = skill.getAffectLimit();
		final List<Player> members = hasChannel ? party.getCommandChannel().getMembers() : party.getMembers();
		
		for (final Player member : members)
		{
			if (activeChar == member)
			{
				continue;
			}
			
			if (Skill.addCharacter(activeChar, member, radius, false))
			{
				targetList.add(member);
				if (targetList.size() >= maxTargets)
				{
					break;
				}
			}
		}
		
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.COMMAND_CHANNEL;
	}
}
