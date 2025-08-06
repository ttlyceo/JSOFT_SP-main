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
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.CreatureSay;

/**
 * Created by LordWinter 15.11.2018
 */
public class AncientHerbSlayer extends Fighter
{
	public AncientHerbSlayer(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final var actor = getActiveChar();
		if (attacker != null && attacker.getFirstEffect(2900) != null && ((actor.getLevel() - attacker.getLevel()) < 6) && damage > 0)
		{
			final var skill = attacker.getCastingSkill();
			if (skill == null)
			{
				actor.doDie(attacker);
				return;
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
	
	@Override
	protected boolean checkAggression(Creature target)
	{
		final var actor = getActiveChar();
		if (actor.getId() == 22659)
		{
			if ((target == null) || (target.getActingPlayer() == null))
			{
				return false;
			}
			
			if (actor.isScriptValue(0))
			{
				actor.setScriptValue(1);
				actor.broadcastPacketToOthers(1500, new CreatureSay(actor.getObjectId(), 0, actor.getName(null), NpcStringId.EVEN_THE_MAGIC_FORCE_BINDS_YOU_YOU_WILL_NEVER_BE_FORGIVEN));
			}
		}
		return super.checkAggression(target);
	}
}
