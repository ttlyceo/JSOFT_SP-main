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
package l2e.scripts.ai.fantasy_isle;

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.HandysBlockCheckerManager;
import l2e.gameserver.model.ArenaParticipantsHolder;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExBlockUpSetList;

public class HandysBlockCheckerEvent extends Quest
{
	private static final String qn = "HandysBlockCheckerEvent";

	private static final int A_MANAGER_1 = 32521;
	private static final int A_MANAGER_2 = 32522;
	private static final int A_MANAGER_3 = 32523;
	private static final int A_MANAGER_4 = 32524;
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if ((npc == null) || (player == null))
		{
			return null;
		}

		final int arena = npc.getId() - A_MANAGER_1;
		if (eventIsFull(arena))
		{
			player.sendPacket(SystemMessageId.CANNOT_REGISTER_CAUSE_QUEUE_FULL);
			return null;
		}

		if (HandysBlockCheckerManager.getInstance().arenaIsBeingUsed(arena))
		{
			player.sendPacket(SystemMessageId.MATCH_BEING_PREPARED_TRY_LATER);
			return null;
		}

		if (HandysBlockCheckerManager.getInstance().addPlayerToArena(player, arena))
		{
			final ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(arena);

			ExBlockUpSetList tl = new ExBlockUpSetList(holder.getRedPlayers(), holder.getBluePlayers(), arena);
			player.sendPacket(tl);

			final int countBlue = holder.getBlueTeamSize();
			final int countRed = holder.getRedTeamSize();
			final int minMembers = Config.MIN_BLOCK_CHECKER_TEAM_MEMBERS;

			if ((countBlue >= minMembers) && (countRed >= minMembers))
			{
				holder.updateEvent();
				holder.broadCastPacketToTeam(new ExBlockUpSetList(false));
				holder.broadCastPacketToTeam(new ExBlockUpSetList(10));
			}
		}
		return null;
	}

	private boolean eventIsFull(int arena)
	{
		if (HandysBlockCheckerManager.getInstance().getHolder(arena).getAllPlayers().size() == 12)
		{
			return true;
		}
		return false;
	}

	public HandysBlockCheckerEvent(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addFirstTalkId(A_MANAGER_1, A_MANAGER_2, A_MANAGER_3, A_MANAGER_4);
	}

	public static void main(String[] args)
	{
		if (!Config.ENABLE_BLOCK_CHECKER_EVENT)
		{
			if (Config.DEBUG)
			{
				_log.info("Handy's Block Checker Event is disabled");
			}
		}
		else
		{
			new HandysBlockCheckerEvent(-1, qn, "Handy's Block Checker Event");
			HandysBlockCheckerManager.getInstance().startUpParticipantsQueue();
			if (Config.DEBUG)
			{
				_log.info("Handy's Block Checker Event is enabled");
			}
		}
	}
}