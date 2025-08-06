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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.TargetUnselected;

public final class RequestTargetCancel extends GameClientPacket
{
	private int _unselect;

	@Override
	protected void readImpl()
	{
		_unselect = readH();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.isLockedTarget())
		{
			activeChar.sendPacket(SystemMessageId.FAILED_DISABLE_TARGET);
			return;
		}
		
		if (_unselect == 0)
		{
			if (activeChar.isCastingNow())
			{
				final Skill skill = activeChar.getCastingSkill();
				if (skill != null && skill.getHitTime() > 1000)
				{
					activeChar.abortCast();
				}
			}
			else if (activeChar.getTarget() != null)
			{
				activeChar.setTarget(null);
			}
		}
		else if (activeChar.getTarget() != null)
		{
			activeChar.setTarget(null);
		}
		else if (activeChar.isInAirShip())
		{
			activeChar.broadcastPacket(new TargetUnselected(activeChar));
		}
	}
}