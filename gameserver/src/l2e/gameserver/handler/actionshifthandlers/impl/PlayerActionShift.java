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

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ClassListParser;
import l2e.gameserver.handler.actionhandlers.IActionHandler;
import l2e.gameserver.handler.admincommandhandlers.AdminCommandHandler;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObject.InstanceType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class PlayerActionShift implements IActionHandler
{
	@Override
	public boolean action(Player activeChar, GameObject target, boolean interact, boolean shift)
	{
		if (activeChar.getTarget() != target)
		{
			activeChar.setTarget(target);
		}
		
		if (activeChar.getAccessLevel().allowPlayerActionShift())
		{
			final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler("admin_character_info");
			if (ach != null)
			{
				ach.useAdminCommand("admin_character_info " + target.getName(null), activeChar);
			}
		}
		else if (Config.ALT_GAME_VIEWPLAYER)
		{
			final NpcHtmlMessage selectInfo = new NpcHtmlMessage(0);
			selectInfo.setFile(activeChar, activeChar.getLang(), "data/html/playerinfo.htm");
			selectInfo.replace("%name%", ((Player) target).getName(activeChar.getLang()));
			selectInfo.replace("%level%", String.valueOf(((Player) target).getLevel()));
			selectInfo.replace("%clan%", String.valueOf(((Player) target).getClan() != null ? ((Player) target).getClan().getName() : null));
			selectInfo.replace("%xp%", String.valueOf(((Player) target).getExp()));
			selectInfo.replace("%sp%", String.valueOf(((Player) target).getSp()));
			selectInfo.replace("%class%", ClassListParser.getInstance().getClass(((Player) target).getClassId()).getClientCode());
			selectInfo.replace("%classid%", String.valueOf(((Player) target).getClassId()));
			selectInfo.replace("%currenthp%", String.valueOf((int) ((Player) target).getCurrentHp()));
			selectInfo.replace("%karma%", String.valueOf(((Player) target).getKarma()));
			selectInfo.replace("%currentmp%", String.valueOf((int) ((Player) target).getCurrentMp()));
			selectInfo.replace("%pvpflag%", String.valueOf(((Player) target).getPvpFlag()));
			selectInfo.replace("%currentcp%", String.valueOf((int) ((Player) target).getCurrentCp()));
			selectInfo.replace("%pvpkills%", String.valueOf(((Player) target).getPvpKills()));
			selectInfo.replace("%pkkills%", String.valueOf(((Player) target).getPkKills()));
			selectInfo.replace("%currentload%", String.valueOf(((Player) target).getCurrentLoad()));
			selectInfo.replace("%patk%", String.valueOf(((Player) target).getPAtk(null)));
			selectInfo.replace("%matk%", String.valueOf(((Player) target).getMAtk(null, null)));
			selectInfo.replace("%pdef%", String.valueOf(((Player) target).getPDef(null)));
			selectInfo.replace("%mdef%", String.valueOf(((Player) target).getMDef(null, null)));
			selectInfo.replace("%accuracy%", String.valueOf(((Player) target).getAccuracy()));
			selectInfo.replace("%evasion%", String.valueOf(((Player) target).getEvasionRate(null)));
			selectInfo.replace("%critical%", String.valueOf(((Player) target).getCriticalHit(null, null)));
			selectInfo.replace("%runspeed%", String.valueOf(((Player) target).getRunSpeed()));
			selectInfo.replace("%patkspd%", String.valueOf(((Player) target).getPAtkSpd()));
			selectInfo.replace("%matkspd%", String.valueOf(((Player) target).getMAtkSpd()));
			selectInfo.replace("%noblesse%", ((Player) target).isNoble() ? "Yes" : "No");
			activeChar.sendPacket(selectInfo);
		}
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Player;
	}
}