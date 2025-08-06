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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.RevengeManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ClassMasterInstance;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;
import l2e.gameserver.model.entity.events.model.FightEventManager;
import l2e.gameserver.model.quest.QuestState;

public class RequestTutorialQuestionMarkPressed extends GameClientPacket
{
	private int _number = 0;

	@Override
	protected void readImpl()
	{
		_number = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}

		player.isntAfk();

		if (player.isInFightEvent())
		{
			FightEventManager.getInstance().sendEventPlayerMenu(player);
		}
		else if (_number == 1002)
		{
			RevengeManager.getInstance().getRevengeList(player);
		}
		else
		{
			ClassMasterInstance.onTutorialQuestionMark(player, _number);

			if (!Config.DISABLE_TUTORIAL)
			{
				final QuestState qs = player.getQuestState("_255_Tutorial");
				if (qs != null)
				{
					qs.getQuest().notifyEvent("QM" + _number + "", null, player);
				}
			}
			
			if (AchievementManager.getInstance().isActive() && _number == player.getObjectId())
			{
				AchievementManager.getInstance().onBypass(player, "_bbs_achievements", null);
			}
		}
	}
}