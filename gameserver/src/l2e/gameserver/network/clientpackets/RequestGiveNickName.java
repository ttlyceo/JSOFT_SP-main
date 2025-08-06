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

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;

public class RequestGiveNickName extends GameClientPacket
{
	private String _target;
	private String _title;

	@Override
	protected void readImpl()
	{
		_target = readS();
		_title = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		if (activeChar.isNoble() && _target.equalsIgnoreCase(activeChar.getName(null)))
		{
			activeChar.setGlobalTitle(_title);
			activeChar.sendPacket(SystemMessageId.TITLE_CHANGED);
			activeChar.broadcastTitleInfo();
		}
		else if ((activeChar.getClanPrivileges() & Clan.CP_CL_GIVE_TITLE) == Clan.CP_CL_GIVE_TITLE)
		{
			if (activeChar.getClan().getLevel() < 3)
			{
				activeChar.sendPacket(SystemMessageId.CLAN_LVL_3_NEEDED_TO_ENDOWE_TITLE);
				return;
			}

			final ClanMember member1 = activeChar.getClan().getClanMember(_target);
			if (member1 != null)
			{
				final Player member = member1.getPlayerInstance();
				if (member != null)
				{
					member.setGlobalTitle(_title);
					member.sendPacket(SystemMessageId.TITLE_CHANGED);
					member.broadcastTitleInfo();
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
				}
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
			}
		}
	}
}