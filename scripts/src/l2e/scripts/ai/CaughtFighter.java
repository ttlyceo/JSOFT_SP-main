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
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 22.09.2018
 */
public class CaughtFighter extends Fighter
{
	private final long TIME_TO_DIE = System.currentTimeMillis() + 60000;
	
	private static final NpcStringId[] NPC_STRINGS_ON_SPAWN =
	{
	        NpcStringId.CROAK_CROAK_FOOD_LIKE_S1_IN_THIS_PLACE, NpcStringId.S1_HOW_LUCKY_I_AM, NpcStringId.PRAY_THAT_YOU_CAUGHT_A_WRONG_FISH_S1
	};
	
	private static final NpcStringId[] NPC_STRINGS_ON_KILL =
	{
	        NpcStringId.UGH_NO_CHANCE_HOW_COULD_THIS_ELDER_PASS_AWAY_LIKE_THIS, NpcStringId.CROAK_CROAK_A_FROG_IS_DYING, NpcStringId.A_FROG_TASTES_BAD_YUCK
	};
	
	private static final NpcStringId[] NPC_STRINGS_ON_ATTACK =
	{
	        NpcStringId.DO_YOU_KNOW_WHAT_A_FROG_TASTES_LIKE, NpcStringId.I_WILL_SHOW_YOU_THE_POWER_OF_A_FROG, NpcStringId.I_WILL_SWALLOW_AT_A_MOUTHFUL
	};
	
	public CaughtFighter(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();
		
		final Attackable npc = getActiveChar();
		
		final GameObject target = npc.getTarget();
		
		if ((target != null) && target.isPlayer())
		{
			if (Rnd.chance(75))
			{
				final Player player = target.getActingPlayer();
				final NpcSay say = new NpcSay(npc, Say2.NPC_ALL, NPC_STRINGS_ON_SPAWN[Rnd.get(NPC_STRINGS_ON_SPAWN.length)]);
				say.addStringParameter(player.getName(null));
				npc.broadcastPacketToOthers(1000, say);
			}
		}
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable npc = getActiveChar();
		if (attacker != null && Rnd.chance(10))
		{
			npc.broadcastPacketToOthers(1000, new NpcSay(npc, Say2.NPC_ALL, NPC_STRINGS_ON_ATTACK[Rnd.get(NPC_STRINGS_ON_ATTACK.length)]));
		}
		super.onEvtAttacked(attacker, damage);
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable actor = getActiveChar();
		if (Rnd.chance(75))
		{
			actor.broadcastPacketToOthers(2000, new NpcSay(actor, Say2.NPC_ALL, NPC_STRINGS_ON_KILL[Rnd.get(NPC_STRINGS_ON_KILL.length)]));
		}
		super.onEvtDead(killer);
	}
	
	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if ((actor != null) && (System.currentTimeMillis() >= TIME_TO_DIE))
		{
			actor.deleteMe();
			return false;
		}
		return super.thinkActive();
	}
}
