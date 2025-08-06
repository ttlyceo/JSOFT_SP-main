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

import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.model.PetData;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.holders.PetItemHolder;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;

public class SummonItems extends ItemSkillsTemplate
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}
		
		for (final AbstractFightEvent e : playable.getFightEvents())
		{
			if (e != null && !e.canUseItemSummon(playable))
			{
				return false;
			}
		}

		var e = playable.getPartyTournament();
		if (e != null && !e.canUseItemSummon(playable))
		{
			return false;
		}

		final Player activeChar = playable.getActingPlayer();
		if ((activeChar.getBlockCheckerArena() != -1) || activeChar.inObserverMode() || activeChar.isAllSkillsDisabled() || activeChar.isCastingNow())
		{
			return false;
		}

		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return false;
		}

		if (activeChar.hasSummon() || activeChar.isMounted())
		{
			activeChar.sendPacket(SystemMessageId.YOU_ALREADY_HAVE_A_PET);
			return false;
		}

		if (activeChar.isAttackingNow() || activeChar.isInCombat())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT);
			return false;
		}

		final PetData petData = PetsParser.getInstance().getPetDataByItemId(item.getId());
		if ((petData == null) || (petData.getNpcId() == -1))
		{
			return false;
		}

		activeChar.addScript(new PetItemHolder(item));
		return super.useItem(playable, item, forceUse);
	}
}