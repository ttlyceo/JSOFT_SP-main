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
package l2e.gameserver.handler.itemhandlers.impl;

import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.SystemMessageId;

public class ChristmasTree implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}

		final Player activeChar = playable.getActingPlayer();
		NpcTemplate template1 = null;
		
		switch (item.getId())
		{
			case 5560 :
				template1 = NpcsParser.getInstance().getTemplate(13006);
				break;
			case 5561 :
				template1 = NpcsParser.getInstance().getTemplate(13007);
				break;
		}
		
		if (template1 == null)
		{
			return false;
		}
		
		GameObject target = activeChar.getTarget();
		if (target == null)
		{
			target = activeChar;
		}
		
		try
		{
			final Spawner spawn = new Spawner(template1);
			spawn.setX(target.getX());
			spawn.setY(target.getY());
			spawn.setZ(target.getZ());
			spawn.setReflection(activeChar.getReflection());
			final Npc npc = spawn.spawnOne(false);
			npc.setSummoner(activeChar);
			
			activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
			
			activeChar.sendMessage("Created " + template1.getName(activeChar.getLang()) + " at x: " + spawn.getX() + " y: " + spawn.getY() + " z: " + spawn.getZ());
			return true;
		}
		catch (final Exception e)
		{
			activeChar.sendPacket(SystemMessageId.TARGET_CANT_FOUND);
			return false;
		}
	}
}