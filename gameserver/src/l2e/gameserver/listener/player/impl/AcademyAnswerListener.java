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

import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.service.academy.AcademyList;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;

public class AcademyAnswerListener implements OnAnswerListener
{
	private final Player _activeChar;
	private final Player _academyChar;
	
	public AcademyAnswerListener(Player activeChar, Player academyChar)
	{
		_activeChar = activeChar;
		_academyChar = academyChar;
	}
	
	@Override
	public void sayYes()
	{
		final Player activeChar = _activeChar;
		final Player academyChar = _academyChar;
		if (activeChar == null || academyChar == null)
		{
			return;
		}
		
		AcademyList.inviteInAcademy(activeChar, academyChar);
	}
	
	@Override
	public void sayNo()
	{
		final Player activeChar = _activeChar;
		final Player academyChar = _academyChar;
		if (activeChar == null || academyChar == null)
		{
			return;
		}
		
		final ServerMessage msg = new ServerMessage("CommunityAcademy.REFUSED", activeChar.getLang());
		msg.add(academyChar.getName(null));
		activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), msg.toString()));
	}
}