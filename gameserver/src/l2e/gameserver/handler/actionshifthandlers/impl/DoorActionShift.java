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
package l2e.gameserver.handler.actionshifthandlers.impl;

import l2e.gameserver.handler.actionhandlers.IActionHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.DoorInstance;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.StaticObject;

public class DoorActionShift implements IActionHandler
{
	@Override
	public boolean action(Player activeChar, GameObject target, boolean interact, boolean shift)
	{
		if (activeChar.getAccessLevel().allowDoorActionShift())
		{
			activeChar.setTarget(target);

			final DoorInstance door = (DoorInstance) target;
			activeChar.sendPacket(new StaticObject(door, activeChar));

			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(activeChar, activeChar.getLang(), "data/html/admin/doorinfo.htm");
			html.replace("%class%", target.getClass().getSimpleName());
			html.replace("%hp%", String.valueOf((int) door.getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(door.getMaxHp()));
			html.replace("%objid%", String.valueOf(target.getObjectId()));
			html.replace("%doorid%", String.valueOf(door.getDoorId()));

			html.replace("%minx%", String.valueOf(door.getX(0)));
			html.replace("%miny%", String.valueOf(door.getY(0)));
			html.replace("%minz%", String.valueOf(door.getZMin()));

			html.replace("%maxx%", String.valueOf(door.getX(2)));
			html.replace("%maxy%", String.valueOf(door.getY(2)));
			html.replace("%maxz%", String.valueOf(door.getZMax()));
			html.replace("%unlock%", door.isOpenableBySkill() ? "<font color=00FF00>YES<font>" : "<font color=FF0000>NO</font>");

			activeChar.sendPacket(html);
		}
		return true;
	}

	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.DoorInstance;
	}
}