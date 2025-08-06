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
package l2e.gameserver.handler.admincommandhandlers.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.util.GMAudit;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.network.SystemMessageId;

public class Buffs implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_getbuffs", "admin_stopbuff", "admin_stopallbuffs", "admin_areacancel", "admin_removereuse", "admin_switch_gm_buffs"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		
		if (command.startsWith("admin_getbuffs"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			command = st.nextToken();
			
			int page = 1;
			if (st.hasMoreTokens())
			{
				page = Integer.parseInt(st.nextToken());
			}
			showBuffs(activeChar, page);
			return true;
		}
		else if (command.startsWith("admin_stopbuff"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				final int skillId = Integer.parseInt(st.nextToken());
				
				removeBuff(activeChar, objectId, skillId);
				return true;
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Failed removing effect: " + e.getMessage());
				activeChar.sendMessage("Usage: //stopbuff <objectId> <skillId>");
				return false;
			}
		}
		else if (command.startsWith("admin_stopallbuffs"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				removeAllBuffs(activeChar, objectId);
				return true;
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Failed removing all effects: " + e.getMessage());
				activeChar.sendMessage("Usage: //stopallbuffs <objectId>");
				return false;
			}
		}
		else if (command.startsWith("admin_areacancel"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			final String val = st.nextToken();
			try
			{
				final int radius = Integer.parseInt(val);
				
				for (final Player knownChar : World.getInstance().getAroundPlayers(activeChar, radius, 200))
				{
					knownChar.stopAllEffects();
				}
				
				activeChar.sendMessage("All effects canceled within raidus " + radius);
				return true;
			}
			catch (final NumberFormatException e)
			{
				activeChar.sendMessage("Usage: //areacancel <radius>");
				return false;
			}
		}
		else if (command.startsWith("admin_removereuse"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			command = st.nextToken();
			
			Player player = null;
			if (st.hasMoreTokens())
			{
				final String playername = st.nextToken();
				
				try
				{
					player = GameObjectsStorage.getPlayer(playername);
				}
				catch (final Exception e)
				{}
				
				if (player == null)
				{
					activeChar.sendMessage("The player " + playername + " is not online.");
					return false;
				}
			}
			else if (activeChar.getTarget().isPlayer())
			{
				player = activeChar.getTarget().getActingPlayer();
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return false;
			}
			
			try
			{
				player.resetReuse();
				player.sendSkillList(true);
				activeChar.sendMessage("Skill reuse was removed from " + player.getName(null) + ".");
				return true;
			}
			catch (final NullPointerException e)
			{
				return false;
			}
		}
		else if (command.startsWith("admin_switch_gm_buffs"))
		{
			if (activeChar.getAccessLevel().allowGiveSpecialSkills() != activeChar.getAccessLevel().allowGiveSpecialAuraSkills())
			{
				final boolean toAuraSkills = activeChar.getKnownSkill(7041) != null;
				switchSkills(activeChar, toAuraSkills);
				activeChar.sendSkillList(false);
				activeChar.sendMessage("You have succefully changed to target " + (toAuraSkills ? "aura" : "one") + " special skills.");
				return true;
			}
			activeChar.sendMessage("There is nothing to switch.");
			return false;
		}
		else
		{
			return true;
		}
	}
	
	public static void switchSkills(Player gmchar, boolean toAuraSkills)
	{
		final Collection<Skill> skills = toAuraSkills ? SkillTreesParser.getInstance().getGMSkillTree().values() : SkillTreesParser.getInstance().getGMAuraSkillTree().values();
		for (final Skill skill : skills)
		{
			gmchar.removeSkill(skill, false);
		}
		SkillTreesParser.getInstance().addSkills(gmchar, toAuraSkills);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	public static void showBuffs(Player activeChar, int page)
	{
		final Creature target = (Creature) activeChar.getTarget();
		if (target == null)
		{
			return;
		}
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/admin/effects_info.htm");
		final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/admin/effects_template.htm");
		String block = "";
		String list = "";
		
		final List<Effect> effList = new ArrayList<>();
		for (final Effect ef : target.getAllEffects())
		{
			if (ef != null && ef.isIconDisplay())
			{
				effList.add(ef);
			}
		}
		
		if (effList.isEmpty() || effList.size() == 0)
		{
			html = html.replace("{list}", "<tr><td align=center>Empty Effects List!</td></tr>");
			html = html.replace("{page}", String.valueOf(page));
			html = html.replace("{navigation}", "<td>&nbsp;</td>");
			html = html.replace("{npc_name}", target.getName(activeChar.getLang()));
			Util.setHtml(html, activeChar);
			return;
		}
		
		final int perpage = 5;
		int counter = 0;
		final int totalSize = effList.size();
		final boolean isThereNextPage = totalSize > perpage;
		
		for (int i = (page - 1) * perpage; i < totalSize; i++)
		{
			final Effect data = effList.get(i);
			if (data != null)
			{
				block = template;
				
				block = block.replace("{name}", data.getSkill().getName(activeChar.getLang()));
				block = block.replace("{icon}", data.getSkill().getIcon());
				block = block.replace("{time}", data.getSkill().isToggle() ? "<font color=\"b02e31\">-1</font>" : getTimeLeft(data.getTimeLeft()));
				block = block.replace("{type}", data.getSkill().isToggle() ? "Toogle" : data.getSkill().isPassive() ? "Passive" : "Active");
				block = block.replace("{bypass}", "bypass -h admin_stopbuff " + Integer.toString(target.getObjectId()) + " " + String.valueOf(data.getSkill().getId()) + "");
				list += block;
			}
		
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		final double pages = (double) totalSize / perpage;
		final int count = (int) Math.ceil(pages);
		html = html.replace("{list}", list);
		html = html.replace("{page}", String.valueOf(page));
		html = html.replace("{objId}", String.valueOf(target.getObjectId()));
		html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "admin_getbuffs %s"));
		html = html.replace("{npc_name}", target.getName(activeChar.getLang()));
		Util.setHtml(html, activeChar);
		
		if (Config.GMAUDIT)
		{
			GMAudit.auditGMAction(activeChar.getName(null) + " [" + activeChar.getObjectId() + "]", "getbuffs", "(" + Integer.toString(target.getObjectId()) + ")", "");
		}
	}
	
	private void removeBuff(Player activeChar, int objId, int skillId)
	{
		Creature target = null;
		try
		{
			target = (Creature) GameObjectsStorage.findObject(objId);
		}
		catch (final Exception e)
		{}
		
		if ((target != null) && (skillId > 0))
		{
			final Effect[] effects = target.getAllEffects();
			
			for (final Effect e : effects)
			{
				if ((e != null) && (e.getSkill().getId() == skillId))
				{
					e.exit();
					activeChar.sendMessage("Removed " + e.getSkill().getName(activeChar.getLang()) + " level " + e.getSkill().getLevel() + " from " + target.getName(activeChar.getLang()) + " (" + objId + ")");
				}
			}
			showBuffs(activeChar, 1);
			if (Config.GMAUDIT)
			{
				GMAudit.auditGMAction(activeChar.getName(null) + " [" + activeChar.getObjectId() + "]", "stopbuff", target.getName(null) + " (" + objId + ")", Integer.toString(skillId));
			}
		}
	}
	
	private static String getTimeLeft(long time)
	{
		final int hours = (int) (time / 60 / 60);
		final int mins = (int) ((time - (hours * 60 * 60)) / 60);
		final int secs = (int) ((time - ((hours * 60 * 60) + (mins * 60))));
		
		final String Strhours = hours < 10 ? "0" + hours : "" + hours;
		final String Strmins = mins < 10 ? "0" + mins : "" + mins;
		final String Strsecs = secs < 10 ? "0" + secs : "" + secs;
		if (hours > 0)
		{
			return "<font color=\"b02e31\">" + Strhours + ":" + Strmins + ":" + Strsecs + "</font>";
		}
		else if (hours <= 0 && mins > 0)
		{
			return "<font color=\"b02e31\">" + Strmins + ":" + Strsecs + "</font>";
		}
		return "<font color=\"b02e31\">" + Strsecs + "</font>";
	}
	
	private static void removeAllBuffs(Player activeChar, int objId)
	{
		Creature target = null;
		try
		{
			target = (Creature) GameObjectsStorage.findObject(objId);
		}
		catch (final Exception e)
		{}
		
		if (target != null)
		{
			target.stopAllEffects();
			activeChar.sendMessage("Removed all effects from " + target.getName(activeChar.getLang()) + " (" + objId + ")");
			showBuffs(activeChar, 1);
			if (Config.GMAUDIT)
			{
				GMAudit.auditGMAction(activeChar.getName(null) + " [" + activeChar.getObjectId() + "]", "stopallbuffs", target.getName(null) + " (" + objId + ")", "");
			}
		}
	}
}