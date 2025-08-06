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


import l2e.gameserver.data.holder.CrestHolder;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.Crest;
import l2e.gameserver.model.CrestType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;

public final class RequestSetPledgeCrest extends GameClientPacket
{
	private int _length;
	private byte[] _data;

	@Override
	protected void readImpl()
	{
		_length = readD();
		if (_length > 256)
		{
			return;
		}
		
		_data = new byte[_length];
		readB(_data);
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final Clan clan = activeChar.getClan();
		if (clan == null)
		{
			return;
		}
		
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_SET_CREST_WHILE_DISSOLUTION_IN_PROGRESS);
			return;
		}
		
		if (_length < 0)
		{
			activeChar.sendMessage("File transfer error.");
			return;
		}
		if (_length > 256)
		{
			activeChar.sendPacket(SystemMessageId.THE_SIZE_OF_THE_IMAGE_FILE_IS_INAPPROPRIATE);
			return;
		}
		
		if ((activeChar.getClanPrivileges() & Clan.CP_CL_REGISTER_CREST) == Clan.CP_CL_REGISTER_CREST)
		{
			if ((_length == 0) || (_data.length == 0))
			{
				if (clan.getCrestId() == 0)
				{
					return;
				}
				
				if (clan.getCrestId() != 0)
				{
					clan.changeClanCrest(0);
					activeChar.sendPacket(SystemMessageId.CLAN_CREST_HAS_BEEN_DELETED);
				}
			}
			else
			{
				if (clan.getLevel() < 3)
				{
					activeChar.sendPacket(SystemMessageId.CLAN_LVL_3_NEEDED_TO_SET_CREST);
					return;
				}
				
				final Crest crest = CrestHolder.getInstance().createCrest(_data, CrestType.PLEDGE);
				if (crest != null)
				{
					clan.changeClanCrest(crest.getId());
					activeChar.sendPacket(SystemMessageId.CLAN_CREST_WAS_SUCCESSFULLY_REGISTRED);
				}
			}
		}
	}
}