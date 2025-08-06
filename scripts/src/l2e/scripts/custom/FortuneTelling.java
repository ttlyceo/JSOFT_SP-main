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

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Based on L2J Eternity-World
 */
public class FortuneTelling extends Quest
{
	private static final String qn = "FortuneTelling";
	
	private static final int NPC_ID = 32616;
	private static final int COST = 1000;
	
	public FortuneTelling(int id, String name, String desc)
	{
		super(id, name, desc);
		
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		
		if (player.getAdena() < COST)
		{
			html.setFile(player, player.getLang(), "data/html/scripts/custom/FortuneTelling/lowadena.htm");
			player.sendPacket(html);
		}
		else
		{
			takeItems(player, 57, COST);
			html.setFile(player, player.getLang(), "data/html/scripts/custom/FortuneTelling/fortune.htm");
			html.replace("%fortune%", "<fstring>" + (1800309 + getRandom(386)) + "</fstring>");
			player.sendPacket(html);
		}
		return "";
	}
	
	public static void main(String[] args)
	{
		new FortuneTelling(-1, qn, "custom");
	}
}