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
package l2e.gameserver.model.actor.status;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Duel;

public class NpcStatus extends CharStatus
{
	public NpcStatus(Npc activeChar)
	{
		super(activeChar);
	}

	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false, true);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption, boolean broadcastPacket)
	{
		if (getActiveChar().isDead())
		{
			return;
		}

		if (attacker != null)
		{
			final Player attackerPlayer = attacker.getActingPlayer();
			if (attackerPlayer != null && attackerPlayer.isInDuel())
			{
				attackerPlayer.setDuelState(Duel.DUELSTATE_INTERRUPTED);
			}

			getActiveChar().addAttackerToAttackByList(attacker);
		}
		super.reduceHp(value, attacker, awake, isDOT, isHpConsumption, broadcastPacket);
	}

	@Override
	public Npc getActiveChar()
	{
		return (Npc) super.getActiveChar();
	}
}