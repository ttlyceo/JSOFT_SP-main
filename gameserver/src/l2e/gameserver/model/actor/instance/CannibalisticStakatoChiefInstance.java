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
import l2e.gameserver.Config;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;

public class CannibalisticStakatoChiefInstance extends RaidBossInstance
{
	private static final int ITEMS[] = { 14833, 14834 };

	public CannibalisticStakatoChiefInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	protected void onDeath(Creature killer)
	{
		if(killer == null)
		{
			super.onDeath(killer);
			return;
		}
		
		Creature topdam = getAggroList().getMostHated();
		if(topdam == null)
		{
			topdam = killer;
		}
		
		final Player player = topdam.getActingPlayer();
		if (player == null)
		{
			super.onDeath(killer);
			return;
		}
		
		final Party party = player.getParty();
		int itemId;
		
		if(party != null)
		{
			for (final Player member : party.getMembers())
			{
				if (member != null && player.isInRange(member, Config.ALT_PARTY_RANGE))
				{
					itemId = ITEMS[Rnd.get(ITEMS.length)];
					member.addItem("Reward", itemId, 1, member, true);
				}
			}
		}
		else
		{
			itemId = ITEMS[Rnd.get(ITEMS.length)];
			player.addItem("Reward", itemId, 1, player, true);
		}
		super.onDeath(killer);
	}
}