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
package l2e.scripts.ai.hellbound;

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.MinionList;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.taskmanager.DecayTaskManager;

public class JuniorFighter extends Fighter
{
	public JuniorFighter(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtSpawn()
	{
		final Attackable npc = getActiveChar();
		
		((MonsterInstance) npc).enableMinions(HellboundManager.getInstance().getLevel() < 5);
		((MonsterInstance) npc).setOnKillDelay(1000);
		
		super.onEvtSpawn();
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable npc = getActiveChar();
		
		final MinionList ml = npc.getMinionList();
		if (ml != null && ml.hasAliveMinions())
		{
			for (final MonsterInstance slave : ml.getAliveMinions())
			{
				if ((slave != null) && !slave.isDead())
				{
					slave.clearAggroList(false);
					slave.abortAttack();
					slave.abortCast();
					slave.broadcastPacketToOthers(2000, new NpcSay(slave.getObjectId(), Say2.NPC_ALL, slave.getId(), NpcStringId.THANK_YOU_FOR_SAVING_ME_FROM_THE_CLUTCHES_OF_EVIL));
					
					if ((HellboundManager.getInstance().getLevel() >= 1) && (HellboundManager.getInstance().getLevel() <= 2))
					{
						HellboundManager.getInstance().updateTrust(10, false);
					}
					slave.getAI().setIntention(CtrlIntention.MOVING, new Location(-25451, 252291, -3252, 3500), 0);
					DecayTaskManager.getInstance().addDecayTask(slave, (Config.NPC_DECAY_TIME * 1000L), true);
				}
			}
		}
		super.onEvtDead(killer);
	}
}
