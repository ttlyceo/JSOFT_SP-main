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
package l2e.gameserver.handler.usercommandhandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.handler.usercommandhandlers.IUserCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class OlympiadStat implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
	        109
	};

	@Override
	public boolean useUserCommand(int id, Player activeChar)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		if (!activeChar.isNoble())
		{
			activeChar.sendPacket(SystemMessageId.NOBLESSE_ONLY);
			return false;
		}
		
		int nobleObjId = activeChar.getObjectId();
		if (!Config.ALLOW_HIDE_OLY_POINTS)
		{
			final var target = activeChar.getTarget();
			if (target != null)
			{
				final var isPlayer = target.isPlayer() && target.getActingPlayer().isNoble();
				if (!isPlayer)
				{
					activeChar.sendPacket(SystemMessageId.NOBLESSE_ONLY);
					return false;
				}
				nobleObjId = target.getObjectId();
			}
		}
		
		final var sm = SystemMessage.getSystemMessage(SystemMessageId.THE_CURRENT_RECORD_FOR_THIS_OLYMPIAD_SESSION_IS_S1_MATCHES_S2_WINS_S3_DEFEATS_YOU_HAVE_EARNED_S4_OLYMPIAD_POINTS);
		sm.addNumber(Olympiad.getInstance().getCompetitionDone(nobleObjId));
		sm.addNumber(Olympiad.getInstance().getCompetitionWon(nobleObjId));
		sm.addNumber(Olympiad.getInstance().getCompetitionLost(nobleObjId));
		sm.addNumber(Olympiad.getInstance().getNoblePoints(nobleObjId));
		activeChar.sendPacket(sm);
		
		final var sm2 = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_S1_MATCHES_REMAINING_THAT_YOU_CAN_PARTECIPATE_IN_THIS_WEEK_S2_CLASSED_S3_NON_CLASSED_S4_TEAM);
		sm2.addNumber(Olympiad.getInstance().getRemainingWeeklyMatches(nobleObjId));
		sm2.addNumber(Olympiad.getInstance().getRemainingWeeklyMatchesClassed(nobleObjId));
		sm2.addNumber(Olympiad.getInstance().getRemainingWeeklyMatchesNonClassed(nobleObjId));
		sm2.addNumber(Olympiad.getInstance().getRemainingWeeklyMatchesTeam(nobleObjId));
		activeChar.sendPacket(sm2);
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}