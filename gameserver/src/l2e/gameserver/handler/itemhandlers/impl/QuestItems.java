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

import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.SystemMessageId;

public class QuestItems implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceuse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}
		
		final Player player = playable.getActingPlayer();

		if (!player.destroyItem("Item Handler - QuestItems", item, player, true))
		{
			return false;
		}

		final Item itm = item.getItem();
		if (itm.getQuestEvents() == null)
		{
			_log.warn(QuestItems.class.getSimpleName() + ": Null list for item handler QuestItems!");
			return false;
		}
		
		for (final Quest quest : itm.getQuestEvents())
		{
			final QuestState state = player.getQuestState(quest.getName());
			if (state == null || !state.isStarted())
			{
				continue;
			}

			quest.notifyItemUse(itm, player);
		}
		return true;
	}
}