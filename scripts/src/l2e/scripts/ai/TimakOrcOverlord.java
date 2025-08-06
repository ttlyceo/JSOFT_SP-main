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

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 16.11.2018
 */
public class TimakOrcOverlord extends Fighter
{
	public TimakOrcOverlord(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		if (attacker != null && getActiveChar().isScriptValue(0))
		{
			getActiveChar().setScriptValue(1);
			if (Rnd.chance(40))
			{
				getActiveChar().broadcastPacketToOthers(1000, new NpcSay(getActiveChar().getObjectId(), 0, getActiveChar().getId(), NpcStringId.DEAR_ULTIMATE_POWER));
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
}
