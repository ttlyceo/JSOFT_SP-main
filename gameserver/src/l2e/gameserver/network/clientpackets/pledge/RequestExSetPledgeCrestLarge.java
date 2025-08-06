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
package l2e.gameserver.network.clientpackets.pledge;


import l2e.gameserver.data.holder.CrestHolder;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.Crest;
import l2e.gameserver.model.CrestType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.GameClientPacket;

public final class RequestExSetPledgeCrestLarge extends GameClientPacket
{
	private int _length;
	private byte[] _data;
	
	@Override
	protected void readImpl()
	{
		_length = readD();
		if (_length > 2176)
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
		
		if ((_length < 0) || (_length > 2176))
		{
			activeChar.sendPacket(SystemMessageId.WRONG_SIZE_UPLOADED_CREST);
			return;
		}
		
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_SET_CREST_WHILE_DISSOLUTION_IN_PROGRESS);
			return;
		}
		
		if ((activeChar.getClanPrivileges() & Clan.CP_CL_REGISTER_CREST) == Clan.CP_CL_REGISTER_CREST)
		{
			if ((_length == 0) || (_data == null))
			{
				if (clan.getCrestLargeId() == 0)
				{
					return;
				}
				
				if (clan.getCrestLargeId() != 0)
				{
					clan.changeLargeCrest(0);
					activeChar.sendPacket(SystemMessageId.CLAN_CREST_HAS_BEEN_DELETED);
				}
			}
			else
			{
				if ((clan.getCastleId() == 0) && (clan.getHideoutId() == 0))
				{
					activeChar.sendMessage("Only a clan that owns a clan hall or a castle can get their emblem displayed on clan related items");
					return;
				}
				
				final Crest crest = CrestHolder.getInstance().createCrest(_data, CrestType.PLEDGE_LARGE);
				if (crest != null)
				{
					clan.changeLargeCrest(crest.getId());
					activeChar.sendPacket(SystemMessageId.CLAN_EMBLEM_WAS_SUCCESSFULLY_REGISTERED);
				}
			}
		}
	}
}