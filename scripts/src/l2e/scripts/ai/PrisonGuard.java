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
package l2e.scripts.ai;

import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

public class PrisonGuard extends Fighter
{
	public PrisonGuard(Attackable actor)
	{
		super(actor);
		
		actor.setIsNoRndWalk(true);
	}

	@Override
	public boolean checkAggression(Creature target)
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead() || actor.getId() == 18367)
		{
			return false;
		}

		if (target.getFirstEffect(5239) == null)
		{
			return false;
		}
		return super.checkAggression(target);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead() || attacker == null)
		{
			return;
		}
		
		if(attacker.isSummon() || attacker.isPet())
		{
			attacker = attacker.getActingPlayer();
		}
		
		if (attacker.getFirstEffect(5239) == null)
		{
			final NpcStringId npcString = (actor.getId() == 18367 ? NpcStringId.ITS_NOT_EASY_TO_OBTAIN : NpcStringId.YOURE_OUT_OF_YOUR_MIND_COMING_HERE);
			actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.NPC_ALL, actor.getId(), npcString));

			final Skill petrification = SkillsParser.getInstance().getInfo(4578, 1);
			if (petrification != null)
			{
				actor.setTarget(attacker);
				actor.doCast(petrification);
			}
			return;
		}

		if (actor.getId() == 18367)
		{
			notifyFriends(attacker, damage);
			return;
		}
		super.onEvtAttacked(attacker, damage);
	}
}