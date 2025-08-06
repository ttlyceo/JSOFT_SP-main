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
package l2e.gameserver.model;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.network.serverpackets.AllianceCrest;
import l2e.gameserver.network.serverpackets.ExPledgeEmblem;
import l2e.gameserver.network.serverpackets.pledge.PledgeCrest;

public final class Crest implements IIdentifiable
{
	private final int _id;
	private final byte[] _data;
	private final CrestType _type;
	
	public Crest(int id, byte[] data, CrestType type)
	{
		_id = id;
		_data = data;
		_type = type;
	}
	
	@Override
	public int getId()
	{
		return _id;
	}
	
	public byte[] getData()
	{
		return _data;
	}
	
	public CrestType getType()
	{
		return _type;
	}
	
	public String getClientPath(Player activeChar)
	{
		String path = null;
		switch (getType())
		{
			case PLEDGE:
			{
				activeChar.sendPacket(new PledgeCrest(getId(), getData()));
				path = "Crest.crest_" + Config.REQUEST_ID + "_" + getId();
				break;
			}
			case PLEDGE_LARGE:
			{
				activeChar.sendPacket(new ExPledgeEmblem(getId(), getData()));
				path = "Crest.crest_" + Config.REQUEST_ID + "_" + getId() + "_l";
				break;
			}
			case ALLY:
			{
				activeChar.sendPacket(new AllianceCrest(getId(), getData()));
				path = "Crest.crest_" + Config.REQUEST_ID + "_" + getId();
				break;
			}
		}
		return path;
	}
}