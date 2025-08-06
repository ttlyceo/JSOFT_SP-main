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

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.handler.communityhandlers.impl.CommunityGeneral;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class LevelAnswerListener implements OnAnswerListener
{
	private final Player _player;
	private final int _level;
	
	public LevelAnswerListener(Player player, int level)
	{
		_player = player;
		_level = level;
	}
	
	@Override
	public void sayYes()
	{
		if (_player == null || !_player.isOnline())
		{
			return;
		}
		if (!CommunityGeneral.correct(_level, _player.getBaseClass() == _player.getClassId().getId()))
		{
			_player.sendMessage((new ServerMessage("ServiceBBS.INCORRECT_LVL", _player.getLang())).toString());
			return;
		}
		
		final boolean delevel = _level < _player.getLevel();
		if (delevel && !Config.SERVICES_DELEVEL_ENABLE)
		{
			_player.sendMessage((new ServerMessage("ServiceBBS.CANT_DELEVEL", _player.getLang())).toString());
			return;
		}
		
		if (!delevel && !Config.SERVICES_LEVELUP_ENABLE)
		{
			_player.sendMessage((new ServerMessage("ServiceBBS.CANT_LVLUP", _player.getLang())).toString());
			return;
		}
		final int item = delevel ? Config.SERVICES_DELEVEL_ITEM[0] : Config.SERVICES_LEVELUP_ITEM[0];
		final long count = delevel ? (_player.getLevel() - _level) * Config.SERVICES_DELEVEL_ITEM[1] : (_level - _player.getLevel()) * Config.SERVICES_LEVELUP_ITEM[1];
		if (_player.getInventory().getItemByItemId(item) == null)
		{
			_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			return;
		}
		if (_player.getInventory().getItemByItemId(item).getCount() < count)
		{
			_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			return;
		}
		
		if (!delevel && !_player.getExpOn())
		{
			_player.sendMessage((new ServerMessage("CommunityGeneral.CANT_ADD_LVL", _player.getLang())).toString());
			return;
		}
		
		_player.destroyItemByItemId("Lvlcalc", item, count, _player, true);
		final long pXp = _player.getExp();
		final long tXp = ExperienceParser.getInstance().getExpForLevel(_level);
		if (delevel)
		{
			_player.getStat().removeExpAndSp((_player.getExp() - ExperienceParser.getInstance().getExpForLevel(_player.getStat().getLevel() - (_player.getLevel() - _level))), 0);
		}
		else
		{
			_player.addExpAndSp(tXp - pXp, 0);
		}
		final ServerMessage msg = new ServerMessage("ServiceBBS.LVL_CHANGE", _player.getLang());
		msg.add(_level);
		_player.sendMessage(msg.toString());
	}
	
	@Override
	public void sayNo()
	{
	}
}