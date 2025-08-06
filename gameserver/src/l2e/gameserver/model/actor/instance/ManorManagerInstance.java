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

import l2e.gameserver.Config;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;

public class ManorManagerInstance extends MerchantInstance
{
	public ManorManagerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.ManorManagerInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/manormanager/manager.htm";
	}

	@Override
	public void showChatWindow(Player player)
	{
		if (!Config.ALLOW_MANOR)
		{
			showChatWindow(player, "data/html/npcdefault.htm");
			return;
		}

		if (!player.canOverrideCond(PcCondOverride.CASTLE_CONDITIONS) && (getCastle() != null) && (getCastle().getId() > 0) && player.isClanLeader() && (getCastle().getOwnerId() == player.getClanId()))
		{
			showChatWindow(player, "data/html/manormanager/manager-lord.htm");
		}
		else
		{
			showChatWindow(player, "data/html/manormanager/manager.htm");
		}
	}
}