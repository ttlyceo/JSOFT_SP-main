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
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 22.11.2018
 */
public class TimakOrcTroopLeader extends Fighter
{
	private static final NpcStringId[] ATTACK_LEADER_MSG =
	{
	        NpcStringId.FORCES_OF_DARKNESS_FOLLOW_ME, NpcStringId.DESTROY_THE_ENEMY_MY_BROTHERS, NpcStringId.SHOW_YOURSELVES, NpcStringId.COME_OUT_YOU_CHILDREN_OF_DARKNESS
	};
	
	private static final int[] BROTHERS =
	{
	        20768, 20769, 20770
	};
	
	public TimakOrcTroopLeader(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		
		if (attacker != null && !actor.isDead() && actor.isScriptValue(0))
		{
			actor.setScriptValue(1);
			actor.broadcastPacketToOthers(1000, new NpcSay(actor, Say2.NPC_ALL, ATTACK_LEADER_MSG[Rnd.get(ATTACK_LEADER_MSG.length)]));
			for (final int bro : BROTHERS)
			{
				try
				{
					final MonsterInstance npc = new MonsterInstance(IdFactory.getInstance().getNextId(), NpcsParser.getInstance().getTemplate(bro));
					final Location loc = ((MonsterInstance) actor).getMinionPosition();
					npc.setReflection(actor.getReflection());
					npc.setHeading(actor.getHeading());
					npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
					npc.spawnMe(loc.getX(), loc.getY(), loc.getZ());
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100));
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
}
