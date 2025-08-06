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

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Ranger;
import l2e.gameserver.geodata.GeoEngine;
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
public class DeluLizardmanRanger extends Ranger
{
	private static final int MONSTERS[] =
	{
	        21104, 21105, 21107
	};
	
	private static NpcStringId[] MONSTERS_MSG =
	{
	        NpcStringId.S1_HOW_DARE_YOU_INTERRUPT_OUR_FIGHT_HEY_GUYS_HELP, NpcStringId.S1_HEY_WERE_HAVING_A_DUEL_HERE, NpcStringId.THE_DUEL_IS_OVER_ATTACK, NpcStringId.FOUL_KILL_THE_COWARD, NpcStringId.HOW_DARE_YOU_INTERRUPT_A_SACRED_DUEL_YOU_MUST_BE_TAUGHT_A_LESSON
	};

	private static NpcStringId[] MONSTERS_ASSIST_MSG =
	{
	        NpcStringId.DIE_YOU_COWARD, NpcStringId.KILL_THE_COWARD, NpcStringId.WHAT_ARE_YOU_LOOKING_AT
	};
	
	public DeluLizardmanRanger(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		
		if (attacker != null && actor.isScriptValue(0))
		{
			final int i = Rnd.get(5);
			if (i < 2)
			{
				final NpcSay packet = new NpcSay(actor.getObjectId(), Say2.NPC_ALL, actor.getId(), MONSTERS_MSG[i]);
				packet.addStringParameter(attacker.getName(null).toString());
				actor.broadcastPacketToOthers(2000, packet);
			}
			else
			{
				actor.broadcastPacketToOthers(2000, new NpcSay(actor.getObjectId(), Say2.NPC_ALL, actor.getId(), MONSTERS_MSG[i]));
			}
			
			for (final Npc npc : World.getInstance().getAroundNpc(actor, 500, 200))
			{
				if (npc.isMonster())
				{
					if (ArrayUtils.contains(MONSTERS, npc.getId()) && !npc.isAttackingNow() && !npc.isDead() && GeoEngine.getInstance().canSeeTarget(actor, npc))
					{
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 500);
						npc.broadcastPacketToOthers(1000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), MONSTERS_ASSIST_MSG[Rnd.get(3)]));
					}
				}
			}
			actor.setScriptValue(1);
		}
		super.onEvtAttacked(attacker, damage);
	}
}
