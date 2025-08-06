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
package l2e.gameserver.handler.itemhandlers.impl;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;

public class ItemSkills extends ItemSkillsTemplate
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		final var activeChar = playable.getActingPlayer();
		if ((activeChar != null) && activeChar.isInOlympiadMode() && item.isOlyRestrictedItem())
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return false;
		}
		
		if ((activeChar != null) && (item.isPotion() || item.isElixir()))
		{
			for (final var e : playable.getFightEvents())
			{
				if (e != null && !e.canUsePotion(playable))
				{
					playable.sendActionFailed();
					return false;
				}
			}

			var e = playable.getPartyTournament();
			if (e != null && !e.canUsePotion(playable))
			{
				playable.sendActionFailed();
				return false;
			}
		}
		return super.useItem(playable, item, forceUse);
	}
}