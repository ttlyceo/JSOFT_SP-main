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
package l2e.gameserver.listener.player.impl;

import l2e.gameserver.instancemanager.BotCheckManager;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.strings.server.ServerMessage;

public class BotCheckAnswerListner implements OnAnswerListener
{
	private final Player _player;
	private final int _qId;

	public BotCheckAnswerListner(Player player, int qId)
	{
		_player = player;
		_qId = qId;
	}

	@Override
	public void sayYes()
	{
		if (_player == null)
		{
			return;
		}
		
		final boolean rightAnswer = BotCheckManager.getInstance().checkAnswer(_qId, true);
		if (rightAnswer)
		{
			_player.increaseBotRating();
			sendFeedBack(_player, true);
		}
		else
		{
			sendFeedBack(_player, false);
			_player.decreaseBotRating();
		}
	}

	@Override
	public void sayNo()
	{
		if (_player == null)
		{
			return;
		}
		
		final boolean rightAnswer = BotCheckManager.getInstance().checkAnswer(_qId, false);
		if (rightAnswer)
		{
			_player.increaseBotRating();
			sendFeedBack(_player, true);
		}
		else
		{
			_player.decreaseBotRating();
			sendFeedBack(_player, false);
		}
	}
	
	private void sendFeedBack(Player player, boolean rightAnswer)
	{
		if (rightAnswer)
		{
			player.sendMessage((new ServerMessage("BotCheck.CORRECT_ANSWER", player.getLang())).toString());
		}
		else
		{
			player.sendMessage((new ServerMessage("BotCheck.INCORRECT_ANSWER", player.getLang())).toString());
		}
		
		if (player.isParalyzed())
		{
			player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
			player.setIsParalyzed(false);
		}
		player.setIsInvul(false);
		player.getPersonalTasks().removeTask(4, true);
	}
}
