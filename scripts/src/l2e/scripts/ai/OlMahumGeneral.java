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
 * Created by LordWinter 16.09.2018
 */
public class OlMahumGeneral extends Fighter
{
	public OlMahumGeneral(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (attacker != null)
		{
			if (actor.isScriptValue(0))
			{
				actor.setScriptValue(1);
				if (Rnd.chance(25))
				{
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), 0, actor.getId(), NpcStringId.WE_SHALL_SEE_ABOUT_THAT));
				}
			}
			else if (Rnd.chance(10))
			{
				actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), 0, actor.getId(), NpcStringId.I_WILL_DEFINITELY_REPAY_THIS_HUMILIATION));
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
}
