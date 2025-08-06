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

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.scripts.ai.AbstractNpcAI;

public class CastleBlacksmith extends AbstractNpcAI
{
	private static final int[] NPCS =
	{
		35098,
		35140,
		35182,
		35224,
		35272,
		35314,
		35361,
		35507,
		35553,
	};
	
	private CastleBlacksmith(String name, String descr)
	{
		super(name, descr);

		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	private boolean hasRights(Player player, Npc npc)
	{
		return player.canOverrideCond(PcCondOverride.CASTLE_CONDITIONS) || npc.isMyLord(player) || ((player.getClanId() == npc.getCastle().getOwnerId()) && ((player.getClanPrivileges() & Clan.CP_CS_MANOR_ADMIN) == Clan.CP_CS_MANOR_ADMIN));
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		return (event.equalsIgnoreCase(npc.getId() + "-02.htm") && hasRights(player, npc)) ? event : null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return (hasRights(player, npc)) ? npc.getId() + "-01.htm" : "no.htm";
	}
	
	public static void main(String[] args)
	{
		new CastleBlacksmith(CastleBlacksmith.class.getSimpleName(), "custom");
	}
}