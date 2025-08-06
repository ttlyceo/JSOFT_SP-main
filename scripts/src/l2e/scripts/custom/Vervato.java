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

import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.zone.ZoneType;
import l2e.scripts.ai.AbstractNpcAI;

public final class Vervato extends AbstractNpcAI
{
	private final static long REVIVE_PRICE = 200000;
	private final static int PRIMEVAL_ISLE_ZONE_ID = 40001;
    private static ZoneType _zone = null;
	
    private Vervato()
    {
        super(Vervato.class.getSimpleName(), "custom");

        addStartNpc(32104);
        addFirstTalkId(32104);
        addTalkId(32104);

        _zone = ZoneManager.getInstance().getZoneById(PRIMEVAL_ISLE_ZONE_ID);
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player)
    {
		if (!("1".equals(event)) || _zone == null)
		{
			return "32104.htm";
		}

		if (player.getInventory().getInventoryItemCount(57, 0) < REVIVE_PRICE)
		{
			return "32104-2.htm";
		}

		final var party = player.getParty();
		if (party == null || party.getMemberCount() < 2)
		{
			return "32104-1.htm";
		}
		
		Player pMember = null;
		for (final var member : party.getMembers())
        {
			if (member == null || member == player)
			{
				continue;
			}

            if (member.isDead() && _zone.isCharacterInZone(member))
			{
				pMember = member;
				break;
			}
        }

		if (pMember == null)
		{
			return "32104-1.htm";
		}
		
		player.destroyItemByItemId("Vervato", 57, REVIVE_PRICE, player, true);
		pMember.teleToLocation(player.getLocation(), true, pMember.getReflection());
		pMember.doRevive(100);
		return "32104-3.htm";
    }

    @Override
    public String onFirstTalk(Npc npc, Player player)
    {
        return "32104.htm";
    }

    public static void main(String[] args)
    {
        new Vervato();
    }
}
