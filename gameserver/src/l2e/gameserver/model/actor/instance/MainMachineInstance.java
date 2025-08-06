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
package l2e.gameserver.model.actor.instance;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter
 */
public class MainMachineInstance extends NpcInstance
{
	private int _powerUnits = 3;

	public MainMachineInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if(_powerUnits != 0)
		{
			return;
		}

		broadcastPacket(new NpcSay(getObjectId(), Say2.NPC_SHOUT, getId(), NpcStringId.FORTRESS_POWER_DISABLED));

		if (!getFort().getSiege().getIsInProgress())
		{
			return;
		}

		onDecay();
		
		getFort().getSiege().disablePower(true);
		getFort().getSiege().checkCommanders();
	}

	public void powerOff(PowerControlUnitInstance powerUnit)
	{
		final int totalSize = getFort().getSiege().getPowerUnits().size();
		final int machineNumber = 3 - totalSize;

		NpcStringId msg = null;
		switch(machineNumber)
		{
			case 1:
				msg = NpcStringId.MACHINE_NO_1_POWER_OFF;
				break;
			case 2:
				msg = NpcStringId.MACHINE_NO_2_POWER_OFF;
				break;
			case 3 :
				msg = NpcStringId.MACHINE_NO_3_POWER_OFF;
				break;
			default:
				throw new IllegalArgumentException("Wrong spawn at fortress: " + getFort().getName());
		}
		_powerUnits --;
		broadcastPacket(new NpcSay(getObjectId(), Say2.NPC_SHOUT, getId(), msg));
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		final NpcHtmlMessage message = new NpcHtmlMessage(getObjectId());
		if(_powerUnits != 0)
		{
			message.setFile(player, player.getLang(), "data/html/fortress/fortress_mainpower002.htm");
		}
		else
		{
			message.setFile(player, player.getLang(), "data/html/fortress/fortress_mainpower001.htm");
		}
		message.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(message);
	}
}
