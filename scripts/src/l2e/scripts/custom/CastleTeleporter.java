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
package l2e.scripts.custom;

import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.scripts.ai.AbstractNpcAI;

public class CastleTeleporter extends AbstractNpcAI
{
	private static final int[] NPCS =
	{
		35095,
		35137,
		35179,
		35221,
		35266,
		35311,
		35355,
		35502,
		35547
	};
	
	private CastleTeleporter(String name, String descr)
	{
		super(name, descr);

		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("teleporter-03.htm"))
		{
			if (npc.isScriptValue(0))
			{
				final var siege = npc.getCastle().getSiege();
				final int time = (siege.getIsInProgress() && (siege.getControlTowerCount() == 0)) ? 480000 : 30000;
				startQuestTimer("teleport", time, npc, null);
				npc.getVariables().set("teleport", (System.currentTimeMillis() + time));
				npc.setScriptValue(1);
			}
			replaceInfo(npc, player, player.getLang(), "teleporter-03.htm");
			return null;
		}
		else if (event.equalsIgnoreCase("teleport"))
		{
			final int region = MapRegionManager.getInstance().getMapRegionLocId(npc.getX(), npc.getY());
			npc.getCastle().oustAllPlayers();
			npc.setScriptValue(0);
			
			for (final var pl : GameObjectsStorage.getPlayers())
			{
				if (region == MapRegionManager.getInstance().getMapRegionLocId(pl))
				{
					final var msg = new NpcSay(npc, Say2.NPC_SHOUT, NpcStringId.THE_DEFENDERS_OF_S1_CASTLE_WILL_BE_TELEPORTED_TO_THE_INNER_CASTLE);
					msg.addStringParameter(npc.getCastle().getName(pl.getLang()));
					pl.sendPacket(msg);
				}
			}
		}
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final var siege = npc.getCastle().getSiege();
		if (npc.isScriptValue(1))
		{
			replaceInfo(npc, player, player.getLang(), "teleporter-03.htm");
			return null;
		}
		return (siege.getIsInProgress() && (siege.getControlTowerCount() == 0)) ? "teleporter-02.htm" : "teleporter-01.htm";
	}
	
	private void replaceInfo(Npc npc, Player player, String lang, String htmlFile)
	{
		final var html = new NpcHtmlMessage(npc.getObjectId());
		
		final long delay = (npc.getVariables().getLong("teleport") - System.currentTimeMillis()) / 1000L;
		final int mins = (int) (delay / 60);
		final int secs = (int) (delay - (mins * 60));
		
		final String Strmins = mins > 0 ? "" + mins + ":" : "";
		final String Strsecs = "" + secs + "";
		
		html.setFile(player, player.getLang(), "data/html/scripts/custom/CastleTeleporter/" + htmlFile);
		html.replace("%time%", "<font color=\"LEVEL\">" + Strmins + "" + Strsecs + "</font>");
		player.sendPacket(html);
	}
	
	public static void main(String[] args)
	{
		new CastleTeleporter(CastleTeleporter.class.getSimpleName(), "custom");
	}
}