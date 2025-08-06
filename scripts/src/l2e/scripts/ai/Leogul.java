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

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 22.11.2018
 */
public class Leogul extends Fighter
{
	public Leogul(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	public boolean checkAggression(Creature killer)
	{
		if (super.checkAggression(killer))
		{
			if (getActiveChar().isScriptValue(0))
			{
				getActiveChar().setScriptValue(1);
				getActiveChar().broadcastPacketToOthers(2000, new NpcSay(getActiveChar().getObjectId(), Say2.NPC_SHOUT, getActiveChar().getId(), NpcStringId._INTRUDER_DETECTED));
				
				for (final Npc npc : World.getInstance().getAroundNpc(getActiveChar(), 800, 200))
				{
					if (npc.isMonster() && (npc.getId() >= 22660) && (npc.getId() <= 22677) && !npc.isDead())
					{
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, killer, 5000);
					}
				}
			}
			return true;
		}
		return false;
	}
}