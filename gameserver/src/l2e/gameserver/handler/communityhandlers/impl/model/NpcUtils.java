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
package l2e.gameserver.handler.communityhandlers.impl.model;

import java.text.NumberFormat;
import java.util.Locale;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public abstract class NpcUtils
{
	private static final NumberFormat pf = NumberFormat.getPercentInstance(Locale.ENGLISH);
	private static final NumberFormat df = NumberFormat.getInstance(Locale.ENGLISH);

	static
	{
		pf.setMaximumFractionDigits(4);
		df.setMinimumFractionDigits(2);
	}

	public static void showNpcSkillList(Player player, NpcTemplate npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(5, 1);
		html.setFile(player, player.getLang(), "data/html/npc_skills.htm");
		html.replace("%npc_name%", npc.getName(player.getLang()));
		
		final StringBuilder sb = new StringBuilder(100);
		for (final Skill skill : npc.getSkills().values())
		{
			sb.append("<table width=260 height=35 cellspacing=0 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\">");
			sb.append("<tr><td fixwidth=34 valign=top><img src=\"" + skill.getIcon() + "\" width=\"32\" height=\"32\"></td>");
			sb.append("<td fixwidth=180>");
			sb.append(skill.getName(player.getLang()));
			sb.append("</td>");
			sb.append("<td fixwidth=65>");
			sb.append(skill.getLevel());
			sb.append(" Lvl</td></tr></table>");
		}
		html.replace("%skills%", sb.toString());
		player.sendPacket(html);
	}
}