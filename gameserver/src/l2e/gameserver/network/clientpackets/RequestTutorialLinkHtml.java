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

import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.model.actor.instance.ClassMasterInstance;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;
import l2e.gameserver.model.quest.QuestState;

public class RequestTutorialLinkHtml extends GameClientPacket
{
	private String _bypass;
	
	@Override
	protected void readImpl()
	{
		_bypass = readS();
	}

	@Override
	protected void runImpl()
	{
		final var player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final var isTutorial = _bypass.startsWith("CO") || _bypass.startsWith("UC") || _bypass.startsWith("QT") || _bypass.startsWith("TE") || _bypass.startsWith("CE") || _bypass.startsWith("QM");
		if (!isTutorial)
		{
			_bypass = player.isDecodedBypass(_bypass);
			if (_bypass == null)
			{
				return;
			}
		}
		
		ClassMasterInstance.onTutorialLink(player, _bypass);
		if (!Config.DISABLE_TUTORIAL)
		{
			final QuestState qs = player.getQuestState("_255_Tutorial");
			if (qs != null)
			{
				qs.getQuest().notifyEvent(_bypass, null, player);
			}
		}
		
		if (_bypass.startsWith("_community"))
		{
			final StringTokenizer st = new StringTokenizer(_bypass, " ");
			st.nextToken();
			if (st.hasMoreTokens())
			{
				final String bypass = st.nextToken();
				final ICommunityBoardHandler handler = CommunityBoardHandler.getInstance().getHandler(bypass);
				if (handler != null)
				{
					handler.onBypassCommand(bypass, player);
				}
			}
		}
		else if (AchievementManager.getInstance().isActive() && _bypass.startsWith("_bbs_achievements"))
		{
			_bypass = _bypass.replaceAll("%", " ");
			
			if (_bypass.length() < 5)
			{
				return;
			}
			AchievementManager.getInstance().onBypass(player, _bypass, null);
		}
	}
}