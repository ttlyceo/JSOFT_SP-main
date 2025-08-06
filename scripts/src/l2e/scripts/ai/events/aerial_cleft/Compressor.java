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
package l2e.scripts.ai.events.aerial_cleft;

import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.instance.FlyMonsterInstance;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;

public class Compressor extends Fighter
{
	public Compressor(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtSpawn()
	{
		final Attackable actor = getActiveChar();
		actor.setIsImmobilized(true);
		switch (actor.getId())
		{
			case 22553 :
				actor.broadcastPacketToOthers(new ExShowScreenMessage(NpcStringId.THE_CENTRAL_STRONGHOLDS_COMPRESSOR_IS_WORKING, 2, 6000));
				break;
			case 22554 :
				actor.broadcastPacketToOthers(new ExShowScreenMessage(NpcStringId.STRONGHOLD_IS_COMPRESSOR_IS_WORKING, 2, 6000));
				break;
			case 22555 :
				actor.broadcastPacketToOthers(new ExShowScreenMessage(NpcStringId.STRONGHOLD_IIS_COMPRESSOR_IS_WORKING, 2, 6000));
				break;
			case 22556 :
				actor.broadcastPacketToOthers(new ExShowScreenMessage(NpcStringId.STRONGHOLD_IIIS_COMPRESSOR_IS_WORKING, 2, 6000));
				break;
		}
		super.onEvtSpawn();
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();

		if (attacker != null && actor.isScriptValue(0))
		{
			for (final Npc npc : World.getInstance().getAroundNpc(actor, 2000, 400))
			{
				if (npc instanceof FlyMonsterInstance)
				{
					if (((npc.getId() == 22557) || (npc.getId() == 22558)) && !npc.isAttackingNow() && !npc.isDead() && GeoEngine.getInstance().canSeeTarget(actor, npc))
					{
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 500);
					}
				}
			}
			actor.setScriptValue(1);
		}
		super.onEvtAttacked(attacker, damage);
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable actor = getActiveChar();
		switch (actor.getId())
		{
			case 22553 :
				actor.broadcastPacketToOthers(new ExShowScreenMessage(NpcStringId.THE_CENTRAL_STRONGHOLDS_COMPRESSOR_HAS_BEEN_DESTROYED, 2, 6000));
				break;
			case 22554 :
				actor.broadcastPacketToOthers(new ExShowScreenMessage(NpcStringId.STRONGHOLD_IS_COMPRESSOR_HAS_BEEN_DESTROYED, 2, 6000));
				break;
			case 22555 :
				actor.broadcastPacketToOthers(new ExShowScreenMessage(NpcStringId.STRONGHOLD_IIS_COMPRESSOR_HAS_BEEN_DESTROYED, 2, 6000));
				break;
			case 22556 :
				actor.broadcastPacketToOthers(new ExShowScreenMessage(NpcStringId.STRONGHOLD_IIIS_COMPRESSOR_HAS_BEEN_DESTROYED, 2, 6000));
				break;
		}
		AerialCleftEvent.getInstance().checkNpcPoints(getActiveChar(), killer.getActingPlayer());
		super.onEvtDead(killer);
	}
}
