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
package l2e.gameserver.model.zone.type;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class MotherTreeZone extends ZoneType
{
	private int _enterMsg;
	private int _leaveMsg;
	private int _mpRegen;
	private int _hpRegen;
	
	public MotherTreeZone(int id)
	{
		super(id);
		addZoneId(ZoneId.MOTHER_TREE);
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("enterMsgId"))
		{
			_enterMsg = Integer.parseInt(value);
		}
		else if (name.equals("leaveMsgId"))
		{
			_leaveMsg = Integer.parseInt(value);
		}
		else if (name.equals("MpRegenBonus"))
		{
			_mpRegen = Integer.parseInt(value);
		}
		else if (name.equals("HpRegenBonus"))
		{
			_hpRegen = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (character.isPlayer())
		{
			final Player player = character.getActingPlayer();
			if (_enterMsg != 0)
			{
				player.sendPacket(SystemMessage.getSystemMessage(_enterMsg));
			}
		}
	}

	@Override
	protected void onExit(Creature character)
	{
		if (character.isPlayer())
		{
			final Player player = character.getActingPlayer();
			if (_leaveMsg != 0)
			{
				player.sendPacket(SystemMessage.getSystemMessage(_leaveMsg));
			}
		}
	}

	public int getMpRegenBonus()
	{
		return _mpRegen;
	}

	public int getHpRegenBonus()
	{
		return _hpRegen;
	}
}