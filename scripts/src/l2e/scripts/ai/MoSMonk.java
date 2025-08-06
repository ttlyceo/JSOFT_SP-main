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
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 22.11.2018
 */
public class MoSMonk extends Fighter
{
	public MoSMonk(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected boolean checkAggression(Creature target)
	{
		if (target.getActiveWeaponInstance() == null)
		{
			return false;
		}
		
		if (super.checkAggression(target))
		{
			if (getActiveChar().isScriptValue(0))
			{
				getActiveChar().setScriptValue(1);
				getActiveChar().broadcastPacketToOthers(1000, new NpcSay(getActiveChar().getObjectId(), Say2.NPC_ALL, getActiveChar().getId(), NpcStringId.YOU_CANNOT_CARRY_A_WEAPON_WITHOUT_AUTHORIZATION));
			}
			return true;
		}
		return false;
	}
}