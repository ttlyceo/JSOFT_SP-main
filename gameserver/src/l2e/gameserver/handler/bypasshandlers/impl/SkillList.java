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
package l2e.gameserver.handler.bypasshandlers.impl;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.NpcInstance;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class SkillList implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "SkillList", "CustomSkillList"
	};
	
	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!(target instanceof NpcInstance))
		{
			return false;
		}
		
		if ((activeChar.getWeightPenalty() >= 3) || !activeChar.isInventoryUnder90(true))
		{
			activeChar.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
			return false;
		}

		if (command.startsWith("CustomSkillList"))
		{
			final String id = command.substring(15).trim();
			if (id.length() != 0)
			{
				try
				{
					NpcInstance.showCustomSkillList(activeChar, Integer.parseInt(id));
				}
				catch (final Exception e)
				{
					_log.warn("Exception in " + getClass().getSimpleName(), e);
				}
			}
		}
		else if (command.startsWith("SkillList"))
		{
			if (Config.ALT_GAME_SKILL_LEARN)
			{
				try
				{
					final String id = command.substring(9).trim();
					if (id.length() != 0)
					{
						NpcInstance.showSkillList(activeChar, (Npc) target, ClassId.getClassId(Integer.parseInt(id)));
					}
					else
					{
						boolean found = false;
						final List<ClassId> classesToTeach = ((NpcInstance) target).getClassesToTeach();
						for (final ClassId cid : classesToTeach)
						{
							if (cid.equalsOrChildOf(activeChar.getClassId()))
							{
								found = true;
								break;
							}
						}
						
						final NpcHtmlMessage html = new NpcHtmlMessage(((Npc) target).getObjectId());
						html.setFile(activeChar, activeChar.getLang(), "data/html/trainer/multiclass-skills.htm");
						String text = "";
						int count = 0;
						if (!classesToTeach.isEmpty())
						{
							final List<ClassId> addClasses = new ArrayList<>();
							for (final ClassId cid : classesToTeach)
							{
								if (cid.level() == 2)
								{
									for (final ClassId cd : ClassId.values())
									{
										if (cd == ClassId.inspector)
										{
											continue;
										}
										
										if (cd.childOf(cid) && (cd.level() == (cid.level() + 1)))
										{
											addClasses.add(cd);
										}
									}
								}
							}
							
							addClasses.addAll(classesToTeach);
							if (found && !addClasses.contains(activeChar.getClassId()))
							{
								addClasses.add(activeChar.getClassId());
							}
							
							ClassId classCheck = activeChar.getClassId();
							
							while ((count == 0) && (classCheck != null))
							{
								for (final ClassId cid : addClasses)
								{
									if (cid.level() > classCheck.level() || cid.level() < classCheck.level())
									{
										continue;
									}
									
									if (SkillTreesParser.getInstance().getAvailableSkills(activeChar, cid, false, false).isEmpty())
									{
										continue;
									}
									
									text += "<a action=\"bypass -h npc_%objectId%_SkillList " + cid.getId() + "\">" + Util.className(activeChar, cid.getId()) + "</a><br>\n";
									count++;
								}
								classCheck = classCheck.getParent();
							}
							classCheck = null;
							addClasses.clear();
						}
						else
						{
							text += "No Skills.<br>";
						}
						
						if (!found && count == 0)
						{
							((Npc) target).showNoTeachHtml(activeChar);
							return false;
						}
						html.replace("%classes%", text);
						html.replace("%objectId%", String.valueOf(((Npc) target).getObjectId()));
						activeChar.sendPacket(html);
						activeChar.sendActionFailed();
					}
				}
				catch (final Exception e)
				{
					_log.warn("Exception in " + getClass().getSimpleName(), e);
				}
			}
			else
			{
				NpcInstance.showSkillList(activeChar, (Npc) target, activeChar.getClassId());
			}
		}
		return true;
	}

	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}