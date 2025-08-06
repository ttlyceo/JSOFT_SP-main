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
package l2e.gameserver.handler.actionhandlers.impl;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.actionhandlers.IActionHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.StaticObjectInstance;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class StaticObjectAction implements IActionHandler
{
	@Override
	public boolean action(final Player activeChar, final GameObject target, final boolean interact, boolean shift)
	{
		final StaticObjectInstance staticObject = (StaticObjectInstance) target;
		if (staticObject.getType() < 0)
		{
			_log.info("StaticObjectInstance: StaticObject with invalid type! StaticObjectId: " + staticObject.getId());
		}

		if (activeChar.getTarget() != staticObject)
		{
			activeChar.setTarget(staticObject);
		}
		else if (interact)
		{
			if (!activeChar.isInsideRadius(staticObject, Npc.INTERACTION_DISTANCE, false, false))
			{
				activeChar.getAI().setIntention(CtrlIntention.INTERACT, staticObject);
			}
			else
			{
				if (staticObject.getType() == 2)
				{
					final String filename = (staticObject.getId() == 24230101) ? "data/html/signboards/tomb_of_crystalgolem.htm" : "data/html/signboards/pvp_signboard.htm";
					final String content = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), filename);
					final NpcHtmlMessage html = new NpcHtmlMessage(staticObject.getObjectId());
					
					if (content == null)
					{
						html.setHtml(activeChar, "<html><body>Signboard is missing:<br>" + filename + "</body></html>");
					}
					else
					{
						html.setHtml(activeChar, content);
					}
					activeChar.sendPacket(html);
				}
				else if (staticObject.getType() == 0)
				{
					activeChar.sendPacket(staticObject.getMap());
				}
			}
		}
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.StaticObjectInstance;
	}
}