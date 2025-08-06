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
package l2e.gameserver.model.actor.instance;

import l2e.commons.util.Rnd;
import l2e.gameserver.instancemanager.games.krateiscube.KrateisCubeManager;
import l2e.gameserver.instancemanager.games.krateiscube.model.Arena;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPVPMatchCCRetire;

/**
 * Created by LordWinter
 */
public class KrateisMatchManagerInstance extends NpcInstance
{
	private int _arenaId = 0;
	
	public KrateisMatchManagerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final Arena arena = KrateisCubeManager.getInstance().getArenaId(getArenaId());
		if (command.startsWith("TeleToArena"))
		{
			if (arena != null && arena.isRegisterPlayer(player))
			{
				if (arena.isActiveNow())
				{
					player.teleToLocation(arena.getBattleLoc().get(Rnd.get(arena.getBattleLoc().size())), true, player.getReflection());
					arena.addEffects(player);
				}
				else
				{
					showChatWindow(player, "data/html/krateisCube/" + getId() + "-01.htm");
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.CANNOT_ENTER_CAUSE_DONT_MATCH_REQUIREMENTS);
				player.teleToLocation(-70381, -70937, -1428, 0, true, player.getReflection());
			}
		}
		else if (command.startsWith("TeleFromArena"))
		{
			if (arena != null && arena.isRegisterPlayer(player))
			{
				arena.removePlayer(player);
				player.stopAllEffects();
				player.sendPacket(ExPVPMatchCCRetire.STATIC);
				player.broadcastStatusUpdate();
				player.broadcastUserInfo(true);
				final Summon pet = player.getSummon();
				if (pet != null)
				{
					pet.stopAllEffects();
				}
			}
			player.teleToLocation(-70381, -70937, -1428, 0, true, player.getReflection());
			
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-0" + val;
		}

		return "data/html/krateisCube/" + pom + ".htm";
	}
	
	public void setArenaId(int id)
	{
		_arenaId = id;
	}
	
	public int getArenaId()
	{
		return _arenaId;
	}
}