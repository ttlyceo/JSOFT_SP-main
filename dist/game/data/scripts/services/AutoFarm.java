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
 * this program. If not, see <>.
 */
package services;

import l2e.commons.time.cron.SchedulingPattern;
import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.AutoFarmManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.service.autofarm.FarmSettings;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by LordWinter
 */
public class AutoFarm implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "autofarm", "autosummonfarm", "farmstart", "farmstop", "buyfarm", "buyTimefarm", "tryFreeTime", "expendLimit", "changeSkillType", "refreshSkills", "removeSkill", "addSkill", "addNewSkill", "editFarmOption", "editSummonSkills", "removeSummonSkill", "addSummonSkill", "addNewSummonSkill", "editSummonFarmOption"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		final int farmType = activeChar.getVarInt("farmType", FarmSettings.FARM_TYPE);
		
		if (command.startsWith("farmstart"))
		{
			final String[] params = command.split(" ");
			String skillType = "attack";
			String skillPage = "1";
			try
			{
				skillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[2];
			}
			catch (final Exception e)
			{}
			
			if (activeChar.getFarmSystem().isActiveAutofarm())
			{
				activeChar.getFarmSystem().startFarmTask();
			}
			else
			{
				activeChar.sendMessage("Cant activate auto farm. You have to purchase it!");
			}
			showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			return true;
		}
		else if (command.startsWith("editSummonSkills"))
		{
			final String[] params = command.split(" ");
			String skillType = "attack";
			String summonSkillType = "attack";
			String skillPage = "1";
			try
			{
				summonSkillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillType = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[3];
			}
			catch (final Exception e)
			{}
			
			if (!activeChar.getFarmSystem().isUseSummonSkills())
			{
				activeChar.sendMessage("You can't edit settings the option is disabled!");
				showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
				return false;
			}
			
			if (!activeChar.hasSummon())
			{
				activeChar.sendMessage("You can't use this option!");
				showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
				return false;
			}
			
			showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
			return true;
		}
		else if (command.startsWith("changeSkillType"))
		{
			final String[] params = command.split(" ");
			String skillType = "attack";
			String skillPage = "1";
			try
			{
				skillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[2];
			}
			catch (final Exception e)
			{}
			showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			return true;
		}
		else if (command.startsWith("refreshSkills"))
		{
			final String[] params = command.split(" ");
			String skillType = "attack";
			String shortCut = "1";
			String skillPage = "1";
			try
			{
				skillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				shortCut = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[3];
			}
			catch (final Exception e)
			{}
			activeChar.getFarmSystem().setShortcutPageValue(Integer.parseInt(shortCut));
			activeChar.getFarmSystem().checkAllSlots();
			showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			return true;
		}
		else if (command.startsWith("removeSkill"))
		{
			final String[] params = command.split(" ");
			String skillType = null;
			String skillId = null;
			String skillPage = "1";
			try
			{
				skillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillId = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[3];
			}
			catch (final Exception e)
			{}
			
			if (skillType != null && skillId != null)
			{
				List<Integer> skillList = null;
				switch (skillType)
				{
					case "attack" :
						skillList = activeChar.getFarmSystem().getAttackSpells();
						break;
					case "chance" :
						skillList = activeChar.getFarmSystem().getChanceSpells();
						break;
					case "self" :
						skillList = activeChar.getFarmSystem().getSelfSpells();
						break;
					case "heal" :
						skillList = activeChar.getFarmSystem().getLowLifeSpells();
						break;
				}
				
				if (skillList != null && skillList.contains(Integer.parseInt(skillId)))
				{
					skillList.remove(Integer.valueOf(Integer.parseInt(skillId)));
					switch (skillType)
					{
						case "attack" :
							activeChar.getFarmSystem().saveSkills("farmAttackSkills");
							break;
						case "chance" :
							activeChar.getFarmSystem().saveSkills("farmChanceSkills");
							break;
						case "self" :
							activeChar.getFarmSystem().saveSkills("farmSelfSkills");
							break;
						case "heal" :
							activeChar.getFarmSystem().saveSkills("farmHealSkills");
							break;
					}
				}
			}
			showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			return true;
		}
		else if (command.startsWith("removeSummonSkill"))
		{
			final String[] params = command.split(" ");
			String summonSkillType = null;
			String skillId = null;
			String skillType = null;
			try
			{
				summonSkillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillId = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillType = params[3];
			}
			catch (final Exception e)
			{}
			
			if (summonSkillType != null && skillId != null && skillType != null)
			{
				List<Integer> skillList = null;
				switch (summonSkillType)
				{
					case "attack" :
						skillList = activeChar.getFarmSystem().getSummonAttackSpells();
						break;
					case "self" :
						skillList = activeChar.getFarmSystem().getSummonSelfSpells();
						break;
					case "heal" :
						skillList = activeChar.getFarmSystem().getSummonHealSpells();
						break;
				}
				
				if (skillList != null && skillList.contains(Integer.parseInt(skillId)))
				{
					skillList.remove(Integer.valueOf(Integer.parseInt(skillId)));
					switch (skillType)
					{
						case "attack" :
							activeChar.getFarmSystem().saveSkills("farmAttackSummonSkills");
							break;
						case "self" :
							activeChar.getFarmSystem().saveSkills("farmSelfSummonSkills");
							break;
						case "heal" :
							activeChar.getFarmSystem().saveSkills("farmHealSummonSkills");
							break;
					}
				}
			}
			showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
			return true;
		}
		else if (command.startsWith("addSkill"))
		{
			final String[] params = command.split(" ");
			String skillType = null;
			String page = "1";
			String skillPage = "1";
			try
			{
				skillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				page = params[3];
			}
			catch (final Exception e)
			{}
			
			if (skillType != null)
			{
				showSkillList(activeChar, skillType, Integer.parseInt(page), Integer.parseInt(skillPage));
			}
			return true;
		}
		else if (command.startsWith("addSummonSkill"))
		{
			final String[] params = command.split(" ");
			String summonSkillType = null;
			String skillType = null;
			String page = "1";
			try
			{
				summonSkillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillType = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				page = params[3];
			}
			catch (final Exception e)
			{}
			
			if (summonSkillType != null && skillType != null)
			{
				showSummonSkillList(activeChar, farmType, summonSkillType, skillType, Integer.parseInt(page));
			}
			return true;
		}
		else if (command.startsWith("addNewSkill"))
		{
			final String[] params = command.split(" ");
			String skillId = null;
			String skillType = null;
			String skillPage = "1";
			try
			{
				skillId = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillType = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[3];
			}
			catch (final Exception e)
			{}
			
			if (skillId != null && skillType != null)
			{
				final Skill skill = activeChar.getKnownSkill(Integer.parseInt(skillId));
				if (skill != null)
				{
					switch (skillType)
					{
						case "attack" :
							if (activeChar.getFarmSystem().getAttackSpells().size() >= FarmSettings.MAX_SKILLS)
							{
								showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
								return false;
							}
							
							if (!skill.isSpoilSkill() && !skill.isSweepSkill() && skill.getId() != 1263 && skill.isAttackSkill())
							{
								activeChar.getFarmSystem().getAttackSpells().add(skill.getId());
								activeChar.getFarmSystem().saveSkills("farmAttackSkills");
							}
							break;
						case "chance" :
							if (activeChar.getFarmSystem().getChanceSpells().size() >= FarmSettings.MAX_SKILLS)
							{
								showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
								return false;
							}
							
							if (skill.isChanceSkill())
							{
								activeChar.getFarmSystem().getChanceSpells().add(skill.getId());
								activeChar.getFarmSystem().saveSkills("farmChanceSkills");
							}
							break;
						case "self" :
							if (activeChar.getFarmSystem().getSelfSpells().size() >= FarmSettings.MAX_SKILLS)
							{
								showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
								return false;
							}
							
							if (skill.isNotSelfSkill())
							{
								return false;
							}
							activeChar.getFarmSystem().getSelfSpells().add(skill.getId());
							activeChar.getFarmSystem().saveSkills("farmSelfSkills");
							break;
						case "heal" :
							if (activeChar.getFarmSystem().getLowLifeSpells().size() >= FarmSettings.MAX_SKILLS)
							{
								showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
								return false;
							}
							
							if (skill.isNotNotHealSkill())
							{
								return false;
							}
							activeChar.getFarmSystem().getLowLifeSpells().add(skill.getId());
							activeChar.getFarmSystem().saveSkills("farmHealSkills");
							break;
					}
				}
			}
			showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			return true;
		}
		else if (command.startsWith("addNewSummonSkill"))
		{
			final String[] params = command.split(" ");
			String skillId = null;
			String summonSkillType = null;
			String skillType = null;
			try
			{
				skillId = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				summonSkillType = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillType = params[3];
			}
			catch (final Exception e)
			{}
			
			if (skillId != null && summonSkillType != null && skillType != null)
			{
				if (!activeChar.hasSummon())
				{
					activeChar.sendMessage("You can't use this option!");
					showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
					return false;
				}
				
				final Summon summon = activeChar.getSummon();
				if (summon.isPet() && (summon.getLevel() - activeChar.getLevel()) > 20)
				{
					activeChar.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
					showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
					return false;
				}
				
				Skill skill = null;
				if (summon.isPet())
				{
					skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(Integer.parseInt(skillId), summon.getLevel());
				}
				else
				{
					skill = summon.getTemplate().getSkill(Integer.parseInt(skillId));
				}
				
				if (skill != null)
				{
					if (skill != null)
					{
						switch (summonSkillType)
						{
							case "attack" :
								if (activeChar.getFarmSystem().getSummonAttackSpells().size() >= 8)
								{
									showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
									return false;
								}
								
								if (skill.isAttackSkill())
								{
									activeChar.getFarmSystem().getSummonAttackSpells().add(skill.getId());
									activeChar.getFarmSystem().saveSkills("farmAttackSummonSkills");
								}
								break;
							case "self" :
								if (activeChar.getFarmSystem().getSummonSelfSpells().size() >= 8)
								{
									showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
									return false;
								}
								
								if (skill.isNotSelfSkill())
								{
									return false;
								}
								activeChar.getFarmSystem().getSummonSelfSpells().add(skill.getId());
								activeChar.getFarmSystem().saveSkills("farmSelfSummonSkills");
								break;
							case "heal" :
								if (activeChar.getFarmSystem().getSummonHealSpells().size() >= 8)
								{
									showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
									return false;
								}
								
								if (skill.isNotNotHealSkill())
								{
									return false;
								}
								activeChar.getFarmSystem().getSummonHealSpells().add(skill.getId());
								activeChar.getFarmSystem().saveSkills("farmHealSummonSkills");
								break;
						}
					}
				}
			}
			showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
			return true;
		}
		else if (command.startsWith("editFarmOption"))
		{
			final String[] params = command.split(" ");
			String skillType = "attack";
			String skillPage = "1";
			String option = null;
			try
			{
				skillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				option = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[3];
			}
			catch (final Exception e)
			{}
			
			if (skillType != null && option != null)
			{
				boolean isFound = false;
				switch (farmType)
				{
					case 0 :
						if (option.equalsIgnoreCase("farmLeaderAssist"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isLeaderAssist() ? false : true;
							activeChar.getFarmSystem().setLeaderAssist(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmKeepLocation"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isKeepLocation() ? false : true;
							activeChar.getFarmSystem().setKeepLocation(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmDelaySkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isExtraDelaySkill() ? false : true;
							activeChar.getFarmSystem().setExDelaySkill(changeValue, false);
							isFound = true;
						}
						break;
					case 1 :
						if (option.equalsIgnoreCase("farmLeaderAssist"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isLeaderAssist() ? false : true;
							activeChar.getFarmSystem().setLeaderAssist(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmKeepLocation"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isKeepLocation() ? false : true;
							activeChar.getFarmSystem().setKeepLocation(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmDelaySkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isExtraDelaySkill() ? false : true;
							activeChar.getFarmSystem().setExDelaySkill(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmRunTargetCloseUp"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isRunTargetCloseUp() ? false : true;
							activeChar.getFarmSystem().setRunTargetCloseUp(changeValue, false);
							isFound = true;
						}
						break;
					case 2 :
						if (option.equalsIgnoreCase("farmLeaderAssist"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isLeaderAssist() ? false : true;
							activeChar.getFarmSystem().setLeaderAssist(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmKeepLocation"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isKeepLocation() ? false : true;
							activeChar.getFarmSystem().setKeepLocation(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmRunTargetCloseUp"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isRunTargetCloseUp() ? false : true;
							activeChar.getFarmSystem().setRunTargetCloseUp(changeValue, false);
							isFound = true;
						}
						break;
					case 3 :
						if (option.equalsIgnoreCase("farmLeaderAssist"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isLeaderAssist() ? false : true;
							activeChar.getFarmSystem().setLeaderAssist(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmAssistMonsterAttack"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isAssistMonsterAttack() ? false : true;
							activeChar.getFarmSystem().setAssistMonsterAttack(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmTargetRestoreMp"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isTargetRestoreMp() ? false : true;
							activeChar.getFarmSystem().setTargetRestoreMp(changeValue, false);
							isFound = true;
						}
						break;
					case 4 :
						if (option.equalsIgnoreCase("farmLeaderAssist"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isLeaderAssist() ? false : true;
							activeChar.getFarmSystem().setLeaderAssist(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmKeepLocation"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isKeepLocation() ? false : true;
							activeChar.getFarmSystem().setKeepLocation(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmUseSummonSkills"))
						{
							if (!activeChar.hasSummon())
							{
								activeChar.sendMessage("You can't use this option!");
								showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
								return true;
							}
							final boolean changeValue = activeChar.getFarmSystem().isUseSummonSkills() ? false : true;
							activeChar.getFarmSystem().setUseSummonSkills(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmDelaySkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isExtraDelaySkill() ? false : true;
							activeChar.getFarmSystem().setExDelaySkill(changeValue, false);
							isFound = true;
						}
						else if (option.equalsIgnoreCase("farmTargetRestoreMp"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isTargetRestoreMp() ? false : true;
							activeChar.getFarmSystem().setTargetRestoreMp(changeValue, false);
							isFound = true;
						}
						break;
				}
				
				if (isFound)
				{
					showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
					return true;
				}
				
				switch (skillType)
				{
					case "attack" :
						if (option.equalsIgnoreCase("farmRndAttackSkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isRndAttackSkills() ? false : true;
							activeChar.getFarmSystem().setRndAttackSkills(changeValue, false);
						}
						showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
						return true;
					case "chance" :
						if (option.equalsIgnoreCase("farmRndChanceSkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isRndChanceSkills() ? false : true;
							activeChar.getFarmSystem().setRndChanceSkills(changeValue, false);
						}
						showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
						return true;
					case "self" :
						if (option.equalsIgnoreCase("farmRndSelfSkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isRndSelfSkills() ? false : true;
							activeChar.getFarmSystem().setRndSelfSkills(changeValue, false);
						}
						showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
						return true;
					case "heal" :
						if (option.equalsIgnoreCase("farmRndLifeSkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isRndLifeSkills() ? false : true;
							activeChar.getFarmSystem().setRndLifeSkills(changeValue, false);
						}
						showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
						return true;
				}
			}
			showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			return true;
		}
		else if (command.startsWith("editSummonFarmOption"))
		{
			final String[] params = command.split(" ");
			String summonSkillType = "attack";
			String skillType = "attack";
			String option = null;
			try
			{
				summonSkillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				option = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillType = params[3];
			}
			catch (final Exception e)
			{}
			
			if (summonSkillType != null && option != null)
			{
				if (option.equalsIgnoreCase("farmSummonDelaySkills"))
				{
					final boolean changeValue = activeChar.getFarmSystem().isExtraSummonDelaySkill() ? false : true;
					activeChar.getFarmSystem().setExSummonDelaySkill(changeValue, false);
					showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
					return true;
				}
				else if (option.equalsIgnoreCase("farmSummonPhysAtk"))
				{
					final boolean changeValue = activeChar.getFarmSystem().isAllowSummonPhysAttack() ? false : true;
					activeChar.getFarmSystem().setSummonPhysAttack(changeValue, false);
					showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
					return true;
				}
				
				switch (summonSkillType)
				{
					case "attack" :
						if (option.equalsIgnoreCase("farmRndSummonAttackSkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isRndSummonAttackSkills() ? false : true;
							activeChar.getFarmSystem().setRndSummonAttackSkills(changeValue, false);
						}
						showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
						return true;
					case "self" :
						if (option.equalsIgnoreCase("farmRndSummonSelfSkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isRndSummonSelfSkills() ? false : true;
							activeChar.getFarmSystem().setRndSummonSelfSkills(changeValue, false);
						}
						showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
						return true;
					case "heal" :
						if (option.equalsIgnoreCase("farmRndSummonLifeSkills"))
						{
							final boolean changeValue = activeChar.getFarmSystem().isRndSummonLifeSkills() ? false : true;
							activeChar.getFarmSystem().setRndSummonLifeSkills(changeValue, false);
						}
						showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
						return true;
				}
			}
			showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
			return true;
		}
		else if (command.startsWith("farmstop"))
		{
			final String[] params = command.split(" ");
			final String skillType = "attack";
			String skillPage = "1";
			try
			{
				skillPage = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[2];
			}
			catch (final Exception e)
			{}
			activeChar.getFarmSystem().stopFarmTask(false);
			showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
		}
		else if (command.startsWith("expendLimit"))
		{
			final String[] params = command.split(" ");
			String skillPage = "1";
			try
			{
				skillPage = params[1];
			}
			catch (final Exception e)
			{}
			
			if (activeChar.getFarmSystem().isAutofarming())
			{
				showMenu(activeChar, null, farmType, "attack", Integer.parseInt(skillPage));
				return false;
			}
			
			if (AutoFarmManager.getInstance().isNonCheckPlayer(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You have already used this service!");
				showMenu(activeChar, null, farmType, "attack", Integer.parseInt(skillPage));
				return false;
			}
			
			if (FarmSettings.FARM_EXPEND_LIMIT_PRICE[0] != 0)
			{
				if (activeChar.getInventory().getItemByItemId(FarmSettings.FARM_EXPEND_LIMIT_PRICE[0]) == null || activeChar.getInventory().getItemByItemId(FarmSettings.FARM_EXPEND_LIMIT_PRICE[0]).getCount() < FarmSettings.FARM_EXPEND_LIMIT_PRICE[1])
				{
					final Item template = ItemsParser.getInstance().getTemplate(FarmSettings.FARM_EXPEND_LIMIT_PRICE[0]);
					if (template != null)
					{
						activeChar.sendMessage("To use the service, you must have " + FarmSettings.FARM_EXPEND_LIMIT_PRICE[1] + " " + template.getName(activeChar.getLang()) + "");
					}
					showMenu(activeChar, null, farmType, "attack", Integer.parseInt(skillPage));
					return false;
				}
				activeChar.destroyItemByItemId("AutoFarmService", FarmSettings.FARM_EXPEND_LIMIT_PRICE[0], FarmSettings.FARM_EXPEND_LIMIT_PRICE[1], activeChar, false);
			}
			AutoFarmManager.getInstance().addNonCheckPlayer(activeChar.getObjectId());
			activeChar.sendMessage("Your character is excluded from limits list!");
			showMenu(activeChar, null, farmType, "attack", Integer.parseInt(skillPage));
		}
		else if (command.startsWith("buyfarm"))
		{
			final String[] params = command.split(" ");
			String skillType = "attack";
			String skillPage = "1";
			try
			{
				skillType = params[1];
			}
			catch (final Exception e)
			{}
			try
			{
				skillPage = params[2];
			}
			catch (final Exception e)
			{}
			
			if (activeChar.getFarmSystem().isActiveAutofarm() && !FarmSettings.ALLOW_ADD_FARM_TIME)
			{
				activeChar.sendMessage("You already have active auto farm time!");
				showMenu(activeChar, null, farmType, "attack", Integer.parseInt(skillPage));
				return false;
			}
			showBuyMenu(activeChar, skillType, Integer.parseInt(skillPage));
			return true;
		}
		else if (command.startsWith("tryFreeTime"))
		{
			final String[] params = command.split(" ");
			String skillType = "attack";
			String skillPage = "1";
			try
			{
				skillType = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[2];
			}
			catch (final Exception e)
			{}
			
			final long farmFreeTime = activeChar.getVarLong("farmFreeTime", 0);
			final boolean allowFreeTime = FarmSettings.REFRESH_FARM_TIME ? farmFreeTime < System.currentTimeMillis() : farmFreeTime <= 0;
			if (!FarmSettings.ALLOW_FARM_FREE_TIME || !allowFreeTime)
			{
				activeChar.sendMessage("Function is not available!");
				showBuyMenu(activeChar, skillType, Integer.parseInt(skillPage));
				return false;
			}
			
			final long expireTime = System.currentTimeMillis() + (FarmSettings.FARM_FREE_TIME * 3600000L);
			if (FarmSettings.FARM_ONLINE_TYPE)
			{
				activeChar.setVar("activeFarmOnlineTask", (expireTime - System.currentTimeMillis()));
				activeChar.setVar("activeFarmOnlineTime", 0);
				activeChar.getFarmSystem().refreshFarmOnlineTime();
			}
			else
			{
				activeChar.setVar("activeFarmTask", expireTime);
				activeChar.getFarmSystem().setAutoFarmEndTask(expireTime);
			}
			
			final long freeTime = new SchedulingPattern("30 6 * * *").next(System.currentTimeMillis());
			activeChar.setVar("farmFreeTime", freeTime);
			activeChar.getFarmSystem().checkFarmTask();
			showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			activeChar.sendMessage("You have successfully activate auto farm free time service!");
		}
		else if (command.startsWith("buyTimefarm"))
		{
			final String[] params = command.split(" ");
			
			String hours = null;
			String skillType = "attack";
			String skillPage = "1";
			try
			{
				hours = params[1];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillType = params[2];
			}
			catch (final Exception e)
			{}
			
			try
			{
				skillPage = params[3];
			}
			catch (final Exception e)
			{}
				
			if (hours != null)
			{
				final int next = Integer.parseInt(hours);
				boolean found = false;
				int itemId = 0;
				long amount = 0;
				final long expireTime = System.currentTimeMillis() + (next * 3600000L);
				for (final int day : FarmSettings.AUTO_FARM_PRICES.keySet())
				{
					if (day == next)
					{
						found = true;
						final String[] price = FarmSettings.AUTO_FARM_PRICES.get(day).split(":");
						if (price != null && price.length == 2)
						{
							itemId = Integer.parseInt(price[0]);
							amount = Long.parseLong(price[1]);
						}
						break;
					}
				}
					
				if (found)
				{
					if (itemId != 0)
					{
						if (activeChar.getInventory().getItemByItemId(itemId) == null || activeChar.getInventory().getItemByItemId(itemId).getCount() < amount)
						{
							final Item template = ItemsParser.getInstance().getTemplate(itemId);
							if (template != null)
							{
								activeChar.sendMessage("To use the service, you must have " + amount + " " + template.getName(activeChar.getLang()) + "");
							}
							showBuyMenu(activeChar, skillType, Integer.parseInt(skillPage));
							return false;
						}
						activeChar.destroyItemByItemId("AutoFarmService", itemId, amount, activeChar, false);
						Util.addServiceLog(activeChar.getName(null) + " buy auto farm service!");
					}
					
					if (FarmSettings.ALLOW_ADD_FARM_TIME && activeChar.getFarmSystem().isActiveAutofarm())
					{
						if (FarmSettings.FARM_ONLINE_TYPE)
						{
							final long endTask = activeChar.getVarLong("activeFarmOnlineTask", 0L);
							activeChar.setVar("activeFarmOnlineTask", (endTask + (next * 3600000L)));
							activeChar.getFarmSystem().stopFarmTask(true);
						}
						else
						{
							final long endTask = activeChar.getVarLong("activeFarmTask", 0L);
							activeChar.setVar("activeFarmTask", (endTask + (next * 3600000L)));
							activeChar.getFarmSystem().setAutoFarmEndTask(0);
							activeChar.getFarmSystem().checkFarmTask();
						}
					}
					else
					{
						if (FarmSettings.FARM_ONLINE_TYPE)
						{
							activeChar.setVar("activeFarmOnlineTask", (expireTime - System.currentTimeMillis()));
							activeChar.setVar("activeFarmOnlineTime", 0);
							activeChar.getFarmSystem().refreshFarmOnlineTime();
						}
						else
						{
							activeChar.setVar("activeFarmTask", expireTime);
							activeChar.getFarmSystem().setAutoFarmEndTask(expireTime);
						}
					}
					activeChar.getFarmSystem().checkFarmTask();
					showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
					activeChar.sendMessage("You have successfully purchased the auto farm service!");
				}
			}
			return true;
		}
		else if (command.startsWith("autofarm"))
		{
			final String[] params = command.split(" ");
			String skillType = "attack";
			String skillPage = "1";
			
			if ((params.length >= 3) && params[1].equalsIgnoreCase("edit_farm"))
			{
				try
				{
					skillType = params[3];
				}
				catch (final Exception e)
				{}
				try
				{
					skillPage = params[4];
				}
				catch (final Exception e)
				{}
				showMenu(activeChar, params[2], farmType, skillType, Integer.parseInt(skillPage));
			}
			else if ((params.length >= 3) && params[1].equals("edit_summonFarmType"))
			{
				String type = null;
				try
				{
					type = params[2];
				}
				catch (final Exception e)
				{}
				
				try
				{
					skillType = params[3];
				}
				catch (final Exception e)
				{}
				
				try
				{
					skillPage = params[4];
				}
				catch (final Exception e)
				{}
				
				if (type != null)
				{
					int next = Integer.parseInt(type);
					if (next > 1)
					{
						next = 1;
					}
					else if (next < 0)
					{
						next = 0;
					}
					activeChar.setVar("summonFarmType", next);
					activeChar.getFarmSystem().setSummonFarmTypeValue(next);
					showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
					return true;
				}
				showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			}
			else if ((params.length >= 3) && params[1].equals("edit_farmType"))
			{
				String type = null;
				try
				{
					type = params[2];
				}
				catch (final Exception e)
				{}
				
				try
				{
					skillType = params[3];
				}
				catch (final Exception e)
				{}
				
				try
				{
					skillPage = params[4];
				}
				catch (final Exception e)
				{}
				
				if (type != null)
				{
					int next = Integer.parseInt(type);
					if (next > 4)
					{
						next = 4;
					}
					else if (next < 0)
					{
						next = 0;
					}
					activeChar.setVar("farmType", next);
					activeChar.getFarmSystem().setFarmTypeValue(next);
					showMenu(activeChar, null, next, skillType, Integer.parseInt(skillPage));
					return true;
				}
				showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			}
			else if ((params.length >= 4))
			{
				String value = null;
				try
				{
					value = params[2];
				}
				catch (final Exception e)
				{}

				if ((value == null || !Util.isNumber(value)) && params.length == 4)
				{
					skillType = value;
					skillPage = params[3];
					showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
					return false;
				}

				try
				{
					skillType = params[3];
				}
				catch (final Exception e)
				{}
				
				try
				{
					skillPage = params[4];
				}
				catch (final Exception e)
				{}

				if (!Util.isNumber(value))
				{
					showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
					return false;
				}
				
				if (params[1].equals("set_attackSkills"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_chanceSkills"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_selfSkills"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_healSkills"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_attackSkillsPercent"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_chanceSkillsPercent"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_selfSkillsPercent"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_healSkillsPercent"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_distance"))
				{
					getChangeParams(activeChar, params, FarmSettings.SEARCH_DISTANCE, 1);
				}
				else if (params[1].equals("set_shortcutPage"))
				{
					getChangeParams(activeChar, params, FarmSettings.SHORTCUT_PAGE, 1);
				}
				showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			}
			else
			{
				try
				{
					skillType = params[1];
				}
				catch (final Exception e)
				{}
				
				try
				{
					skillPage = params[2];
				}
				catch (final Exception e)
				{}
				showMenu(activeChar, null, farmType, skillType, Integer.parseInt(skillPage));
			}
			return true;
		}
		else if (command.startsWith("autosummonfarm"))
		{
			final String[] params = command.split(" ");
			String summonSkillType = "attack";
			final String skillType = "attack";
			
			if ((params.length >= 3) && params[1].equalsIgnoreCase("edit_farm"))
			{
				try
				{
					summonSkillType = params[3];
				}
				catch (final Exception e)
				{}
				showSummonSkillInfo(activeChar, params[2], farmType, summonSkillType, skillType);
			}
			else if ((params.length > 4))
			{
				try
				{
					summonSkillType = params[3];
				}
				catch (final Exception e)
				{}
				
				if (params[1].equals("set_attackSummonSkills"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_selfSummonSkills"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_healSummonSkills"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_attackSummonSkillsPercent"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_selfSummonSkillsPercent"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				else if (params[1].equals("set_healSummonSkillsPercent"))
				{
					getChangeParams(activeChar, params, 100, 1);
				}
				showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
			}
			else
			{
				try
				{
					final String param3 = params[3];
					if (!Util.isDigit(param3))
					{
						summonSkillType = params[3];
					}
				}
				catch (final Exception e)
				{}
				showSummonSkillInfo(activeChar, null, farmType, summonSkillType, skillType);
			}
			return true;
		}
		return true;
	}
	
	private void getChangeParams(Player player, String[] params, int defaultMax, int defaultMin)
	{
		String percent = null;
		try
		{
			percent = params[2];
		}
		catch (final Exception e)
		{}
		
		if (percent != null)
		{
			int per = 0;
			try
			{
				per = Integer.parseInt(percent);
				
				if (per > defaultMax)
				{
					per = defaultMax;
				}
				
				if (per < defaultMin)
				{
					per = defaultMin;
				}
			}
			catch (final NumberFormatException nfe)
			{
				if (params[1].equals("set_attackSkills"))
				{
					per = player.getVarInt("attackChanceSkills", FarmSettings.ATTACK_SKILL_CHANCE);
				}
				else if (params[1].equals("set_chanceSkills"))
				{
					per = player.getVarInt("chanceChanceSkills", FarmSettings.CHANCE_SKILL_CHANCE);
				}
				else if (params[1].equals("set_selfSkills"))
				{
					per = player.getVarInt("selfChanceSkills", FarmSettings.SELF_SKILL_CHANCE);
				}
				else if (params[1].equals("set_healSkills"))
				{
					per = player.getVarInt("healChanceSkills", FarmSettings.HEAL_SKILL_CHANCE);
				}
				else if (params[1].equals("set_attackSkillsPercent"))
				{
					per = player.getVarInt("attackSkillsPercent", FarmSettings.ATTACK_SKILL_PERCENT);
				}
				else if (params[1].equals("set_chanceSkillsPercent"))
				{
					per = player.getVarInt("chanceSkillsPercent", FarmSettings.CHANCE_SKILL_PERCENT);
				}
				else if (params[1].equals("set_selfSkillsPercent"))
				{
					per = player.getVarInt("selfSkillsPercent", FarmSettings.SELF_SKILL_PERCENT);
				}
				else if (params[1].equals("set_healSkillsPercent"))
				{
					per = player.getVarInt("healSkillsPercent", FarmSettings.HEAL_SKILL_PERCENT);
				}
				else if (params[1].equals("set_distance"))
				{
					per = player.getVarInt("farmDistance", FarmSettings.SEARCH_DISTANCE);
				}
				else if (params[1].equals("set_shortcutPage"))
				{
					per = player.getVarInt("shortcutPage", FarmSettings.SHORTCUT_PAGE);
				}
				else if (params[1].equals("set_attackSummonSkills"))
				{
					per = player.getVarInt("attackSummonChanceSkills", FarmSettings.SUMMON_ATTACK_SKILL_CHANCE);
				}
				else if (params[1].equals("set_selfSummonSkills"))
				{
					per = player.getVarInt("selfSummonChanceSkills", FarmSettings.SUMMON_SELF_SKILL_CHANCE);
				}
				else if (params[1].equals("set_healSummonSkills"))
				{
					per = player.getVarInt("healSummonChanceSkills", FarmSettings.SUMMON_HEAL_SKILL_CHANCE);
				}
				else if (params[1].equals("set_attackSummonSkillsPercent"))
				{
					per = player.getVarInt("attackSummonSkillsPercent", FarmSettings.SUMMON_ATTACK_SKILL_PERCENT);
				}
				else if (params[1].equals("set_selfSummonSkillsPercent"))
				{
					per = player.getVarInt("selfSummonSkillsPercent", FarmSettings.SUMMON_SELF_SKILL_PERCENT);
				}
				else if (params[1].equals("set_healSummonSkillsPercent"))
				{
					per = player.getVarInt("healSummonSkillsPercent", FarmSettings.SUMMON_HEAL_SKILL_PERCENT);
				}
			}
			
			if (params[1].equals("set_attackSkills"))
			{
				player.setVar("attackChanceSkills", per);
				player.getFarmSystem().setAttackSkillValue(false, per);
			}
			else if (params[1].equals("set_chanceSkills"))
			{
				player.setVar("chanceChanceSkills", per);
				player.getFarmSystem().setChanceSkillValue(false, per);
			}
			else if (params[1].equals("set_selfSkills"))
			{
				player.setVar("selfChanceSkills", per);
				player.getFarmSystem().setSelfSkillValue(false, per);
			}
			else if (params[1].equals("set_healSkills"))
			{
				player.setVar("healChanceSkills", per);
				player.getFarmSystem().setLifeSkillValue(false, per);
			}
			else if (params[1].equals("set_attackSkillsPercent"))
			{
				player.setVar("attackSkillsPercent", per);
				player.getFarmSystem().setAttackSkillValue(true, per);
			}
			else if (params[1].equals("set_chanceSkillsPercent"))
			{
				player.setVar("chanceSkillsPercent", per);
				player.getFarmSystem().setChanceSkillValue(true, per);
			}
			else if (params[1].equals("set_selfSkillsPercent"))
			{
				player.setVar("selfSkillsPercent", per);
				player.getFarmSystem().setSelfSkillValue(true, per);
			}
			else if (params[1].equals("set_healSkillsPercent"))
			{
				player.setVar("healSkillsPercent", per);
				player.getFarmSystem().setLifeSkillValue(true, per);
			}
			else if (params[1].equals("set_distance"))
			{
				player.setVar("farmDistance", per);
				player.getFarmSystem().setRadiusValue(per);
			}
			else if (params[1].equals("set_shortcutPage"))
			{
				player.setVar("shortcutPage", per);
				player.getFarmSystem().setShortcutPageValue(per);
			}
			else if (params[1].equals("set_attackSummonSkills"))
			{
				player.setVar("attackSummonChanceSkills", per);
				player.getFarmSystem().setSummonAttackSkillValue(false, per);
			}
			else if (params[1].equals("set_selfSummonSkills"))
			{
				player.setVar("selfSummonChanceSkills", per);
				player.getFarmSystem().setSummonSelfSkillValue(false, per);
			}
			else if (params[1].equals("set_healSummonSkills"))
			{
				player.setVar("healSummonChanceSkills", per);
				player.getFarmSystem().setSummonLifeSkillValue(false, per);
			}
			else if (params[1].equals("set_attackSummonSkillsPercent"))
			{
				player.setVar("attackSummonSkillsPercent", per);
				player.getFarmSystem().setSummonAttackSkillValue(true, per);
			}
			else if (params[1].equals("set_selfSummonSkillsPercent"))
			{
				player.setVar("selfSummonSkillsPercent", per);
				player.getFarmSystem().setSummonSelfSkillValue(true, per);
			}
			else if (params[1].equals("set_healSummonSkillsPercent"))
			{
				player.setVar("healSummonSkillsPercent", per);
				player.getFarmSystem().setSummonLifeSkillValue(true, per);
			}
		}
	}
	
	private final static String _ONText = "<font color=\"00FF00\">ON</font>";
	private final static String _OFFText = "<font color=\"FF0000\">OFF</font>";
	
	private void showMenu(Player player, String editCmd, int type, String skillType, int skillPage)
	{
		String html = null;
		switch (type)
		{
			case 0 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-fighter.htm");
				final boolean farmLeaderAssist = player.getFarmSystem().isLeaderAssist();
				if (farmLeaderAssist)
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox");
				}
				html = html.replace("%assist_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmLeaderAssist " + skillPage + "");
				
				final boolean farmKeepLocation = player.getFarmSystem().isKeepLocation();
				if (farmKeepLocation)
				{
					html = html.replace("%keepLoc_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%keepLoc_img%", "L2UI.CheckBox");
				}
				html = html.replace("%keepLoc_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmKeepLocation " + skillPage + "");
				
				final boolean farmDelaySkills = player.getFarmSystem().isExtraDelaySkill();
				if (farmDelaySkills)
				{
					html = html.replace("%delaySk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%delaySk_img%", "L2UI.CheckBox");
				}
				html = html.replace("%delaySk_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmDelaySkills " + skillPage + "");
				break;
			case 1 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-archer.htm");
				final boolean farmLeaderAssist1 = player.getFarmSystem().isLeaderAssist();
				if (farmLeaderAssist1)
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox");
				}
				html = html.replace("%assist_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmLeaderAssist " + skillPage + "");
				
				final boolean farmKeepLocation1 = player.getFarmSystem().isKeepLocation();
				if (farmKeepLocation1)
				{
					html = html.replace("%keepLoc_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%keepLoc_img%", "L2UI.CheckBox");
				}
				html = html.replace("%keepLoc_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmKeepLocation " + skillPage + "");
				
				final boolean farmDelaySkills1 = player.getFarmSystem().isExtraDelaySkill();
				if (farmDelaySkills1)
				{
					html = html.replace("%delaySk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%delaySk_img%", "L2UI.CheckBox");
				}
				html = html.replace("%delaySk_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmDelaySkills " + skillPage + "");
				
				final boolean runTargetCloseUp1 = player.getFarmSystem().isRunTargetCloseUp();
				if (runTargetCloseUp1)
				{
					html = html.replace("%runCloseUp_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%runCloseUp_img%", "L2UI.CheckBox");
				}
				html = html.replace("%runCloseUp_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmRunTargetCloseUp " + skillPage + "");
				break;
			case 2 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-mage.htm");
				final boolean farmLeaderAssist2 = player.getFarmSystem().isLeaderAssist();
				if (farmLeaderAssist2)
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox");
				}
				html = html.replace("%assist_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmLeaderAssist " + skillPage + "");
				
				final boolean farmKeepLocation2 = player.getFarmSystem().isKeepLocation();
				if (farmKeepLocation2)
				{
					html = html.replace("%keepLoc_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%keepLoc_img%", "L2UI.CheckBox");
				}
				html = html.replace("%keepLoc_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmKeepLocation " + skillPage + "");
				
				final boolean runTargetCloseUp2 = player.getFarmSystem().isRunTargetCloseUp();
				if (runTargetCloseUp2)
				{
					html = html.replace("%runCloseUp_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%runCloseUp_img%", "L2UI.CheckBox");
				}
				html = html.replace("%runCloseUp_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmRunTargetCloseUp " + skillPage + "");
				break;
			case 3 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-heal.htm");
				final boolean farmLeaderAssist3 = player.getFarmSystem().isLeaderAssist();
				if (farmLeaderAssist3)
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox");
				}
				html = html.replace("%assist_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmLeaderAssist " + skillPage + "");
				
				final boolean farmAssistMonsterAttack = player.getFarmSystem().isAssistMonsterAttack();
				if (farmAssistMonsterAttack)
				{
					html = html.replace("%assistMAttack_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%assistMAttack_img%", "L2UI.CheckBox");
				}
				html = html.replace("%assistMAttack_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmAssistMonsterAttack " + skillPage + "");
				
				final boolean farmTargetRestoreMp = player.getFarmSystem().isTargetRestoreMp();
				if (farmTargetRestoreMp)
				{
					html = html.replace("%tgRestoreMp_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%tgRestoreMp_img%", "L2UI.CheckBox");
				}
				html = html.replace("%tgRestoreMp_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmTargetRestoreMp " + skillPage + "");
				break;
			case 4 :
				html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-summon.htm");
				final boolean farmLeaderAssist4 = player.getFarmSystem().isLeaderAssist();
				if (farmLeaderAssist4)
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%assist_img%", "L2UI.CheckBox");
				}
				html = html.replace("%assist_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmLeaderAssist " + skillPage + "");
				
				final boolean farmKeepLocation4 = player.getFarmSystem().isKeepLocation();
				if (farmKeepLocation4)
				{
					html = html.replace("%keepLoc_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%keepLoc_img%", "L2UI.CheckBox");
				}
				html = html.replace("%keepLoc_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmKeepLocation " + skillPage + "");
				
				final boolean farmUseSummonSkills = player.getFarmSystem().isUseSummonSkills();
				if (farmUseSummonSkills)
				{
					html = html.replace("%useSummonSk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%useSummonSk_img%", "L2UI.CheckBox");
				}
				html = html.replace("%useSummonSk_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmUseSummonSkills " + skillPage + "");
				
				final boolean farmDelaySkills4 = player.getFarmSystem().isExtraDelaySkill();
				if (farmDelaySkills4)
				{
					html = html.replace("%delaySk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%delaySk_img%", "L2UI.CheckBox");
				}
				html = html.replace("%delaySk_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmDelaySkills " + skillPage + "");

				final boolean farmTargetRestoreMp4 = player.getFarmSystem().isTargetRestoreMp();
				if (farmTargetRestoreMp4)
				{
					html = html.replace("%tgRestoreMp_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					html = html.replace("%tgRestoreMp_img%", "L2UI.CheckBox");
				}
				html = html.replace("%tgRestoreMp_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmTargetRestoreMp " + skillPage + "");
				break;
		}
		
		String chanceAttack = "", percentAttack = "", chanceChance = "", percentChance = "", chanceSelf = "", percentSelf = "", chanceLowHeal = "", percentLowHeal = "";
		List<Integer> skillList = null;
		String htm = null;
		switch (skillType)
		{
			case "attack" :
				skillList = player.getFarmSystem().getAttackSpells();
				htm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-skill_attack.htm");
				chanceAttack = getBooleanFrame(player, (editCmd != null && editCmd.equals("editAttackSkills")) ? editCmd : null, "editAttackSkills", "attackChanceSkills", FarmSettings.ATTACK_SKILL_CHANCE, skillType, skillPage);
				percentAttack = getBooleanFrame(player, (editCmd != null && editCmd.equals("editAttackPercent")) ? editCmd : null, "editAttackPercent", "attackSkillsPercent", FarmSettings.ATTACK_SKILL_PERCENT, skillType, skillPage);
				final boolean isRndAttackSkills = player.getFarmSystem().isRndAttackSkills();
				if (isRndAttackSkills)
				{
					htm = htm.replace("%rndAttackSk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					htm = htm.replace("%rndAttackSk_img%", "L2UI.CheckBox");
				}
				htm = htm.replace("%rndAttackSk_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmRndAttackSkills " + skillPage + "");
				break;
			case "chance" :
				skillList = player.getFarmSystem().getChanceSpells();
				htm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-skill_chance.htm");
				chanceChance = getBooleanFrame(player, (editCmd != null && editCmd.equals("editChanceSkills")) ? editCmd : null, "editChanceSkills", "chanceChanceSkills", FarmSettings.CHANCE_SKILL_CHANCE, skillType, skillPage);
				percentChance = getBooleanFrame(player, (editCmd != null && editCmd.equals("editChancePercent")) ? editCmd : null, "editChancePercent", "chanceSkillsPercent", FarmSettings.CHANCE_SKILL_PERCENT, skillType, skillPage);
				final boolean isRndChanceSkills = player.getFarmSystem().isRndChanceSkills();
				if (isRndChanceSkills)
				{
					htm = htm.replace("%rndChanceSk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					htm = htm.replace("%rndChanceSk_img%", "L2UI.CheckBox");
				}
				htm = htm.replace("%rndChanceSk_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmRndChanceSkills " + skillPage + "");
				break;
			case "self" :
				skillList = player.getFarmSystem().getSelfSpells();
				htm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-skill_self.htm");
				chanceSelf = getBooleanFrame(player, (editCmd != null && editCmd.equals("editSelfSkills")) ? editCmd : null, "editSelfSkills", "selfChanceSkills", FarmSettings.SELF_SKILL_CHANCE, skillType, skillPage);
				percentSelf = getBooleanFrame(player, (editCmd != null && editCmd.equals("editSelfPercent")) ? editCmd : null, "editSelfPercent", "selfSkillsPercent", FarmSettings.SELF_SKILL_PERCENT, skillType, skillPage);
				final boolean isRndSelfSkills = player.getFarmSystem().isRndSelfSkills();
				if (isRndSelfSkills)
				{
					htm = htm.replace("%rndSelfSk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					htm = htm.replace("%rndSelfSk_img%", "L2UI.CheckBox");
				}
				htm = htm.replace("%rndSelfSk_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmRndSelfSkills " + skillPage + "");
				break;
			case "heal" :
				skillList = player.getFarmSystem().getLowLifeSpells();
				htm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-skill_heal.htm");
				chanceLowHeal = getBooleanFrame(player, (editCmd != null && editCmd.equals("editHealSkills")) ? editCmd : null, "editHealSkills", "healChanceSkills", FarmSettings.HEAL_SKILL_CHANCE, skillType, skillPage);
				percentLowHeal = getBooleanFrame(player, (editCmd != null && editCmd.equals("editLowHealPercent")) ? editCmd : null, "editLowHealPercent", "healSkillsPercent", FarmSettings.HEAL_SKILL_PERCENT, skillType, skillPage);
				final boolean isRndLifeSkills = player.getFarmSystem().isRndLifeSkills();
				if (isRndLifeSkills)
				{
					htm = htm.replace("%rndLifeSk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					htm = htm.replace("%rndLifeSk_img%", "L2UI.CheckBox");
				}
				htm = htm.replace("%rndLifeSk_bypass%", "bypass -h voiced_editFarmOption " + skillType + " farmRndLifeSkills " + skillPage + "");
				break;
		}
		
		String block = "";
		String list = "";
		
		final int perpage = FarmSettings.MAX_SKILLS > 8 ? 7 : 8;
		int counter = 0;
		
		final int totalSize = FarmSettings.MAX_SKILLS;
		final boolean isThereNextPage = totalSize > perpage;
		
		if (skillList != null)
		{
			final List<Integer> removeSkills = new ArrayList<>();
			for (final int skillId : skillList)
			{
				if (skillId > 0)
				{
					final Skill sk = player.getKnownSkill(skillId);
					if (sk == null)
					{
						removeSkills.add(skillId);
					}
				}
			}
			
			if (!removeSkills.isEmpty())
			{
				for (final int sk : removeSkills)
				{
					skillList.remove(Integer.valueOf(sk));
				}
				removeSkills.clear();
			}
			
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/skill-template.htm");
			for (int i = (skillPage - 1) * perpage; i < FarmSettings.MAX_SKILLS; i++)
			{
				block = template;
				if (skillList.size() > 0 && i < skillList.size())
				{
					final int skillId = skillList.get(i);
					if (skillId > 0)
					{
						final Skill sk = player.getKnownSkill(skillId);
						if (sk != null)
						{
							block = block.replace("{icon}", sk.getIcon());
							block = block.replace("{background}", "");
							block = block.replace("{bypass}", "bypass -h voiced_removeSkill " + skillType + " " + sk.getId() + " " + skillPage + "");
						}
						else
						{
							block = block.replace("{icon}", "icon.high_tab");
							block = block.replace("{background}", "background=\"l2ui_ch3.multisell_plusicon\"");
							block = block.replace("{bypass}", "bypass -h voiced_addSkill " + skillType + " " + skillPage + "");
						}
					}
					else
					{
						block = block.replace("{icon}", "icon.high_tab");
						block = block.replace("{background}", "background=\"l2ui_ch3.multisell_plusicon\"");
						block = block.replace("{bypass}", "bypass -h voiced_addSkill " + skillType + " " + skillPage + "");
					}
				}
				else
				{
					block = block.replace("{icon}", "icon.high_tab");
					block = block.replace("{background}", "background=\"l2ui_ch3.multisell_plusicon\"");
					block = block.replace("{bypass}", "bypass -h voiced_addSkill " + skillType + " " + skillPage + "");
				}
				list += block;
				
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
		}
		
		final String distance = getBooleanBigFrame(player, (editCmd != null && editCmd.equals("editDistance")) ? editCmd : null, "editDistance", "farmDistance", FarmSettings.SEARCH_DISTANCE, skillType, skillPage);
		final String shortcutPage = getBooleanFrame(player, (editCmd != null && editCmd.equals("editShortcutPage")) ? editCmd : null, "editShortcutPage", "shortcutPage", FarmSettings.SHORTCUT_PAGE, skillType, skillPage);
		
		final int count = (int) Math.ceil((double) totalSize / perpage);
		html = html.replace("%activate%", isActivate(player, skillType, skillPage));
		html = html.replace("%skillType%", skillType);
		html = html.replace("%skillList%", list);
		html = html.replace("%skillsParam%", htm);
		html = html.replace("%activeHwids%", getActivateHwids(player, skillPage));
		html = html.replace("%refreshSkills%", "bypass -h voiced_refreshSkills " + skillType + " " + skillPage + "");
		html = html.replace("%status%", player.getFarmSystem().isAutofarming() ? _ONText : _OFFText);
		html = html.replace("%button%", player.getFarmSystem().isAutofarming() ? "<button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_farmstop " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.STRING_OFF") + "\">" : "<button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_farmstart " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.STRING_ON") + "\">");
		html = html.replace("%chanceAttack%", chanceAttack.equals("") ? "" : chanceAttack);
		html = html.replace("%chanceChance%", chanceChance.equals("") ? "" : chanceChance);
		html = html.replace("%chanceSelf%", chanceSelf.equals("") ? "" : chanceSelf);
		html = html.replace("%chanceLowHeal%", chanceLowHeal.equals("") ? "" : chanceLowHeal);
		html = html.replace("%percentAttack%", percentAttack.equals("") ? "" : percentAttack);
		html = html.replace("%percentChance%", percentChance.equals("") ? "" : percentChance);
		html = html.replace("%percentSelf%", percentSelf.equals("") ? "" : percentSelf);
		html = html.replace("%percentLowHeal%", percentLowHeal.equals("") ? "" : percentLowHeal);
		html = html.replace("%distance%", distance.equals("") ? "" : distance);
		html = html.replace("%shortcutPage%", shortcutPage.equals("") ? "" : shortcutPage);
		html = html.replace("%summonType%", getSummonFarmType(player, player.getVarInt("summonFarmType", FarmSettings.FARM_TYPE), skillType));
		html = html.replace("%farmType%", getFarmType(player, type, skillType));
		html = html.replace("%navigation%", Util.getVerticalNavigationBlock(count, skillPage, totalSize, perpage, isThereNextPage, "voiced_autofarm " + skillType + " %s"));
		Util.setHtml(html, false, player);
	}
	
	public String getActivateHwids(Player player, int skillPage)
	{
		if (FarmSettings.FARM_ACTIVE_LIMITS < 0)
		{
			return "<font color=\"LEVEL\">-<font>";
		}
		
		final int lastHwids = AutoFarmManager.getInstance().getActiveFarms(player);
		if (lastHwids > 0)
		{
			return "<font color=\"LEVEL\">" + lastHwids + "<font>";
		}
		else if (lastHwids <= 0 && AutoFarmManager.getInstance().isNonCheckPlayer(player.getObjectId()))
		{
			return "<font color=\"00FF00\">" + lastHwids + "<font>";
		}
		return "<a action=\"bypass -h voiced_expendLimit " + skillPage + "\"><font color=\"FF0000\">" + lastHwids + "<font></a>";
	}
	
	private String isActivate(Player player, String skillType, int skillPage)
	{
		if (FarmSettings.AUTO_FARM_FREE || (FarmSettings.PREMIUM_FARM_FREE && player.hasPremiumBonus()))
		{
			return "<font color=FF6600>No Limit Time</font>";
		}
		
		if (player.getFarmSystem().isActiveAutofarm())
		{
			if (FarmSettings.FARM_ONLINE_TYPE)
			{
				long lastTime = 0L;
				if (!player.getFarmSystem().isActiveFarmTask())
				{
					lastTime = ((player.getVarLong("activeFarmOnlineTask", 0) - (player.getFarmSystem().getLastFarmOnlineTime())) / 1000L);
				}
				else
				{
					lastTime = ((player.getVarLong("activeFarmOnlineTask", 0) - (player.getFarmSystem().getLastFarmOnlineTime() + (System.currentTimeMillis() - player.getFarmSystem().getFarmOnlineTime()))) / 1000L);
				}
				
				if (FarmSettings.ALLOW_ADD_FARM_TIME)
				{
					return "<a action=\"bypass -h voiced_buyfarm " + skillType + " " + skillPage + "\"><font color=\"E6D0AE\">" + TimeUtils.formatTime(player, (int) lastTime, false) + "</a></font>";
				}
				return "<font color=\"E6D0AE\">" + TimeUtils.formatTime(player, (int) lastTime, false) + "</font>";
			}
			else
			{
				final long lastTime = (player.getFarmSystem().getAutoFarmEnd() - System.currentTimeMillis()) / 1000L;
				if (FarmSettings.ALLOW_ADD_FARM_TIME)
				{
					return "<a action=\"bypass -h voiced_buyfarm " + skillType + " " + skillPage + "\"><font color=\"E6D0AE\">" + TimeUtils.formatTime(player, (int) lastTime, false) + "</a></font>";
				}
				return "<font color=\"E6D0AE\">" + TimeUtils.formatTime(player, (int) lastTime, false) + "</font>";
			}
		}
		return "<button value=\"Buy Auto Farm Time\" action=\"bypass -h voiced_buyfarm " + skillType + " " + skillPage + "\" width=130 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
	}
	
	private void showBuyMenu(Player player, String skillType, int skillPage)
	{
		final NpcHtmlMessage htm = new NpcHtmlMessage(6);
		htm.setFile(player, player.getLang(), "data/html/mods/autofarm/buy.htm");
		
		String days = "";
		final List<Integer> list = new ArrayList<>();
		for (final int day : FarmSettings.AUTO_FARM_PRICES.keySet())
		{
			list.add(day);
		}
		
		final Comparator<Integer> statsComparator = new SortTimeInfo();
		Collections.sort(list, statsComparator);
		
		int count = 0;
		for (final int day : list)
		{
			if (count > 0)
			{
				days += ";";
			}
			days += "" + day + "";
			count++;
		}
		htm.replace("%time%", days);
		htm.replace("%freeUse%", getFreeUse(player, skillType, skillPage));
		htm.replace("%skillType%", skillType);
		htm.replace("%skillPage%", String.valueOf(skillPage));
		list.clear();
		player.sendPacket(htm);
	}
	
	private String getFreeUse(Player player, String skillType, int skillPage)
	{
		if (player.getFarmSystem().isActiveAutofarm() || !FarmSettings.ALLOW_FARM_FREE_TIME)
		{
			return "";
		}
		
		final long farmFreeTime = player.getVarLong("farmFreeTime", 0);
		final boolean allowFreeTime = FarmSettings.REFRESH_FARM_TIME ? farmFreeTime < System.currentTimeMillis() : farmFreeTime <= 0;
		if (allowFreeTime)
		{
			return "<button value=\"Try free for " + FarmSettings.FARM_FREE_TIME + " hour(s)\" action=\"bypass -h voiced_tryFreeTime " + skillType + " " + skillPage + "\" width=140 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
		}
		return "";
	}
	
	private static class SortTimeInfo implements Comparator<Integer>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(Integer o1, Integer o2)
		{
			return Double.compare(o1, o2);
		}
	}
	
	private String getSummonFarmType(Player player, int farmType, String skillType)
	{
		String msg = "<td aling=center width=20>";
		int nextType = 0;
		switch (farmType)
		{
			case 0 :
				msg += player.isHFClient() ? "<img src=\"L2UI_CH3.party_styleicon1_3\" width=16 height=16></td>" : "<img src=\"L2UI_CH3.party_styleicon3_1\" width=16 height=16></td>";
				nextType++;
				break;
			case 1 :
				msg += player.isHFClient() ? "<img src=\"L2UI_CH3.party_styleicon5_3\" width=16 height=16></td>" : "<img src=\"L2UI_CH3.party_styleicon3_5\" width=16 height=16></td>";
				break;
		}
		msg += "<td width=30 valign=center><button width=16 height=16 back=\"L2UI_CH3.shortcut_rotate_down\" fore=\"L2UI_CH3.shortcut_rotate\" action=\"bypass -h voiced_autofarm edit_summonFarmType " + nextType + " " + skillType + "\" value=\" \"></td>";
		return msg;
	}
	
	private String getFarmType(Player player, int farmType, String skillType)
	{
		String msg = "<td aling=center width=20>";
		int nextType = 0;
		String type = "";
		switch (farmType)
		{
			case 0 :
				msg += player.isHFClient() ? "<img src=\"L2UI_CH3.party_styleicon1_3\" width=16 height=16></td>" : "<img src=\"L2UI_CH3.party_styleicon3_1\" width=16 height=16></td>";
				type = "Fighter";
				nextType++;
				break;
			case 1 :
				msg += player.isHFClient() ? "<img src=\"L2UI_CH3.party_styleicon2_3\" width=16 height=16></td>" : "<img src=\"L2UI_CH3.party_styleicon3_2\" width=16 height=16></td>";
				type = "Archer";
				nextType = 2;
				break;
			case 2 :
				msg += player.isHFClient() ? "<img src=\"L2UI_CH3.party_styleicon5_3\" width=16 height=16></td>" : "<img src=\"L2UI_CH3.party_styleicon3_5\" width=16 height=16></td>";
				type = "Magic";
				nextType = 3;
				break;
			case 3 :
				msg += player.isHFClient() ? "<img src=\"L2UI_CH3.party_styleicon6_3\" width=16 height=16></td>" : "<img src=\"L2UI_CH3.party_styleicon3_6\" width=16 height=16></td>";
				type = "Healer";
				nextType = 4;
				break;
			case 4 :
				msg += player.isHFClient() ? "<img src=\"L2UI_CH3.party_styleicon7_3\" width=16 height=16></td>" : "<img src=\"L2UI_CH3.party_styleicon3_7\" width=16 height=16></td>";
				type = "Summon";
				break;
		}
		msg += "<td width=85 valign=center>" + type + "</td><td width=30><button width=16 height=16 back=\"L2UI_CH3.shortcut_rotate_down\" fore=\"L2UI_CH3.shortcut_rotate\" action=\"bypass -h voiced_autofarm edit_farmType " + nextType + " " + skillType + "\" value=\" \"></td>";
		return msg;
	}
	
	private static String getBooleanBigFrame(Player player, String editCmd, String editeVar, String playerEditeVar, int defaultVar, String skillType, int skillPage)
	{
		String info = "";
		if (editCmd != null && !editCmd.isEmpty())
		{
			if (editCmd.equals("editDistance"))
			{
				info += "<td width=50><edit var=\"" + editCmd + "\" width=40 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_distance $editDistance " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
		}
		else
		{
			info += "<td aling=center width=50><font color=c1b33a>" + player.getVarInt(playerEditeVar, defaultVar) + "</font></td>";
			info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm edit_farm " + editeVar + " " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.EDIT") + "\"></td>";
		}
		return info;
	}

	private static String getBooleanFrame(Player player, String editCmd, String editeVar, String playerEditeVar, int defaultVar, String skillType, int skillPage)
	{
		String info = "";
		if (editCmd != null && !editCmd.isEmpty())
		{
			if (editCmd.equals("editAttackSkills"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_attackSkills $editAttackSkills " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editChanceSkills"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_chanceSkills $editChanceSkills " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editSelfSkills"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_selfSkills $editSelfSkills " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editHealSkills"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_healSkills $editHealSkills " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editAttackPercent"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_attackSkillsPercent $editAttackPercent " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editChancePercent"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_chanceSkillsPercent $editChancePercent " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editSelfPercent"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_selfSkillsPercent $editSelfPercent " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editLowHealPercent"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_healSkillsPercent $editLowHealPercent " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editShortcutPage"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm set_shortcutPage $editShortcutPage " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
		}
		else
		{
			if (playerEditeVar.equals("shortcutPage"))
			{
				info += "<td aling=center width=45><font color=c1b33a>" + player.getVarInt(playerEditeVar, defaultVar) + "</font></td>";
			}
			else
			{
				info += "<td aling=center width=45><font color=c1b33a>" + player.getVarInt(playerEditeVar, defaultVar) + "%</font></td>";
			}
			info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autofarm edit_farm " + editeVar + " " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.EDIT") + "\"></td>";
		}
		return info;
	}
	
	private static String getSummonBooleanFrame(Player player, String editCmd, String editeVar, String playerEditeVar, int defaultVar, String skillType, int skillPage)
	{
		String info = "";
		if (editCmd != null && !editCmd.isEmpty())
		{
			if (editCmd.equals("editSummonAttackSkills"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autosummonfarm set_attackSummonSkills $editSummonAttackSkills " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editSummonSelfSkills"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autosummonfarm set_selfSummonSkills $editSummonSelfSkills " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editSummonHealSkills"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autosummonfarm set_healSummonSkills $editSummonHealSkills " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editSummonAttackPercent"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autosummonfarm set_attackSummonSkillsPercent $editSummonAttackPercent " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editSummonSelfPercent"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autosummonfarm set_selfSummonSkillsPercent $editSummonSelfPercent " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
			else if (editCmd.equals("editSummonLowHealPercent"))
			{
				info += "<td width=43><edit var=\"" + editCmd + "\" width=34 height=12></td>";
				info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autosummonfarm set_healSummonSkillsPercent $editSummonLowHealPercent " + skillType + " " + skillPage + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE") + "\"></td>";
			}
		}
		else
		{
			info += "<td aling=center width=45><font color=c1b33a>" + player.getVarInt(playerEditeVar, defaultVar) + "%</font></td>";
			info += "<td width=40><button width=40 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h voiced_autosummonfarm edit_farm " + editeVar + " " + skillType + "\" value=\"" + ServerStorage.getInstance().getString(player.getLang(), "Menu.EDIT") + "\"></td>";
		}
		return info;
	}
	
	private void showSkillList(Player player, String skillType, int page, int skillPage)
	{
		List<Integer> skillList = null;
		final List<Skill> plSkillList = new ArrayList<>();
		switch (skillType)
		{
			case "attack" :
				skillList = player.getFarmSystem().getAttackSpells();
				for (final Skill skill : player.getAllSkills())
				{
					if (skill == null)
					{
						continue;
					}
					
					if ((player.getTransformation() != null) && (!player.hasTransformSkill(skill.getId()) && !skill.allowOnTransform()))
					{
						continue;
					}
					
					if (!skill.isSpoilSkill() && !skill.isSweepSkill() && skill.getId() != 1263 && skill.isAttackSkill())
					{
						plSkillList.add(skill);
					}
				}
				break;
			case "chance" :
				skillList = player.getFarmSystem().getChanceSpells();
				for (final Skill skill : player.getAllSkills())
				{
					if (skill == null)
					{
						continue;
					}
					
					if ((player.getTransformation() != null) && (!player.hasTransformSkill(skill.getId()) && !skill.allowOnTransform()))
					{
						continue;
					}
					
					if (skill.isChanceSkill())
					{
						plSkillList.add(skill);
					}
				}
				break;
			case "self" :
				skillList = player.getFarmSystem().getSelfSpells();
				for (final Skill skill : player.getAllSkills())
				{
					if (skill == null)
					{
						continue;
					}
					
					if ((player.getTransformation() != null) && (!player.hasTransformSkill(skill.getId()) && !skill.allowOnTransform()))
					{
						continue;
					}
					
					if (!skill.isNotSelfSkill())
					{
						plSkillList.add(skill);
					}
				}
				break;
			case "heal" :
				skillList = player.getFarmSystem().getLowLifeSpells();
				for (final Skill skill : player.getAllSkills())
				{
					if (skill == null)
					{
						continue;
					}
					
					if ((player.getTransformation() != null) && (!player.hasTransformSkill(skill.getId()) && !skill.allowOnTransform()))
					{
						continue;
					}
					
					if (!skill.isNotNotHealSkill())
					{
						plSkillList.add(skill);
					}
				}
				break;
		}
		
		if (plSkillList.isEmpty())
		{
			player.sendMessage("You have no valid skills!");
			showMenu(player, null, player.getVarInt("farmType", FarmSettings.FARM_TYPE), skillType, skillPage);
			return;
		}
		
		if (skillList.size() > 0)
		{
			final List<Skill> removeSkills = new ArrayList<>();
			for (final Skill skill : plSkillList)
			{
				if (skillList.contains(skill.getId()))
				{
					removeSkills.add(skill);
				}
			}
			
			if (!removeSkills.isEmpty())
			{
				for (final Skill sk : removeSkills)
				{
					plSkillList.remove(sk);
				}
				removeSkills.clear();
			}
		}
		
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/player_skills.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/player_skills_template.htm");
		String block = "";
		String list = "";
		
		final int perpage = 5;
		int counter = 0;
		final int totalSize = plSkillList.size();
		final boolean isThereNextPage = totalSize > perpage;
		
		for (int i = (page - 1) * perpage; i < totalSize; i++)
		{
			final Skill data = plSkillList.get(i);
			if (data != null)
			{
				block = template;
				
				block = block.replace("{name}", data.getName(player.getLang()));
				block = block.replace("{icon}", data.getIcon());
				block = block.replace("{bypass}", "bypass -h voiced_addNewSkill " + data.getId() + " " + skillType + " " + skillPage + "");
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
		html = html.replace("{skillType}", skillType);
		html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "voiced_addSkill " + skillType + " " + skillPage + " %s"));
		Util.setHtml(html, false, player);
		plSkillList.clear();
	}
	
	private void showSummonSkillInfo(Player player, String editCmd, int farmType, String summonSkillType, String skillType)
	{
		if (!player.hasSummon())
		{
			player.sendMessage("You can't use this option!");
			showSummonSkillInfo(player, null, farmType, summonSkillType, skillType);
			return;
		}
		
		final Summon summon = player.getSummon();
		if (summon.isPet() && (summon.getLevel() - player.getLevel()) > 20)
		{
			player.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
			showSummonSkillInfo(player, null, farmType, summonSkillType, skillType);
			return;
		}
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-summonSkills.htm");
		List<Integer> skillList = null;
		String htm = null;
		
		String chanceAttack = "", percentAttack = "", chanceSelf = "", percentSelf = "", chanceLowHeal = "", percentLowHeal = "";
		switch (summonSkillType)
		{
			case "attack" :
				skillList = player.getFarmSystem().getSummonAttackSpells();
				htm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-summon_skill_attack.htm");
				chanceAttack = getSummonBooleanFrame(player, (editCmd != null && editCmd.equals("editSummonAttackSkills")) ? editCmd : null, "editSummonAttackSkills", "attackSummonChanceSkills", FarmSettings.SUMMON_ATTACK_SKILL_CHANCE, summonSkillType, 1);
				percentAttack = getSummonBooleanFrame(player, (editCmd != null && editCmd.equals("editSummonAttackPercent")) ? editCmd : null, "editSummonAttackPercent", "attackSummonSkillsPercent", FarmSettings.SUMMON_ATTACK_SKILL_PERCENT, summonSkillType, 1);
				final boolean isRndAttackSkills = player.getFarmSystem().isRndSummonAttackSkills();
				if (isRndAttackSkills)
				{
					htm = htm.replace("%rndAttackSk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					htm = htm.replace("%rndAttackSk_img%", "L2UI.CheckBox");
				}
				htm = htm.replace("%rndAttackSk_bypass%", "bypass -h voiced_editSummonFarmOption " + summonSkillType + " farmRndSummonAttackSkills " + skillType);
				break;
			case "self" :
				skillList = player.getFarmSystem().getSummonSelfSpells();
				htm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-summon_skill_self.htm");
				chanceSelf = getSummonBooleanFrame(player, (editCmd != null && editCmd.equals("editSummonSelfSkills")) ? editCmd : null, "editSummonSelfSkills", "selfSummonChanceSkills", FarmSettings.SUMMON_SELF_SKILL_CHANCE, summonSkillType, 1);
				percentSelf = getSummonBooleanFrame(player, (editCmd != null && editCmd.equals("editSummonSelfPercent")) ? editCmd : null, "editSummonSelfPercent", "selfSummonSkillsPercent", FarmSettings.SUMMON_SELF_SKILL_PERCENT, summonSkillType, 1);
				final boolean isRndSelfSkills = player.getFarmSystem().isRndSummonSelfSkills();
				if (isRndSelfSkills)
				{
					htm = htm.replace("%rndSelfSk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					htm = htm.replace("%rndSelfSk_img%", "L2UI.CheckBox");
				}
				htm = htm.replace("%rndSelfSk_bypass%", "bypass -h voiced_editSummonFarmOption " + summonSkillType + " farmRndSummonSelfSkills " + skillType);
				break;
			case "heal" :
				skillList = player.getFarmSystem().getSummonHealSpells();
				htm = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/index-summon_skill_heal.htm");
				chanceLowHeal = getSummonBooleanFrame(player, (editCmd != null && editCmd.equals("editSummonHealSkills")) ? editCmd : null, "editSummonHealSkills", "healSummonChanceSkills", FarmSettings.SUMMON_HEAL_SKILL_CHANCE, summonSkillType, 1);
				percentLowHeal = getSummonBooleanFrame(player, (editCmd != null && editCmd.equals("editSummonLowHealPercent")) ? editCmd : null, "editSummonLowHealPercent", "healSummonSkillsPercent", FarmSettings.SUMMON_HEAL_SKILL_PERCENT, summonSkillType, 1);
				final boolean isRndLifeSkills = player.getFarmSystem().isRndSummonLifeSkills();
				if (isRndLifeSkills)
				{
					htm = htm.replace("%rndLifeSk_img%", "L2UI.CheckBox_checked");
				}
				else
				{
					htm = htm.replace("%rndLifeSk_img%", "L2UI.CheckBox");
				}
				htm = htm.replace("%rndLifeSk_bypass%", "bypass -h voiced_editSummonFarmOption " + summonSkillType + " farmRndSummonLifeSkills " + skillType);
				break;
		}
		
		String block = "";
		String list = "";
		
		if (skillList != null && !skillList.isEmpty())
		{
			final List<Integer> removeSkills = new ArrayList<>();
			for (final int skillId : skillList)
			{
				if (skillId > 0)
				{
					Skill skill = null;
					if (summon.isPet())
					{
						skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
					}
					else
					{
						skill = summon.getTemplate().getSkill(skillId);
					}
					
					if (skill == null)
					{
						removeSkills.add(skillId);
					}
				}
			}
			
			if (!removeSkills.isEmpty())
			{
				for (final int skillId : removeSkills)
				{
					skillList.remove(Integer.valueOf(skillId));
				}
				removeSkills.clear();
			}
		}
		
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/summon_skill-template.htm");
		for (int i = 0; i < 8; i++)
		{
			block = template;
			if (skillList != null && skillList.size() > 0 && i < skillList.size())
			{
				final int skillId = skillList.get(i);
				if (skillId > 0)
				{
					Skill skill = null;
					if (summon.isPet())
					{
						skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
					}
					else
					{
						skill = summon.getTemplate().getSkill(skillId);
					}
					
					if (skill != null)
					{
						block = block.replace("{icon}", skill.getIcon());
						block = block.replace("{background}", "");
						block = block.replace("{bypass}", "bypass -h voiced_removeSummonSkill " + summonSkillType + " " + skillId + " " + skillType);
					}
					else
					{
						block = block.replace("{icon}", "icon.high_tab");
						block = block.replace("{background}", "background=\"l2ui_ch3.multisell_plusicon\"");
						block = block.replace("{bypass}", "bypass -h voiced_addSummonSkill " + summonSkillType + " " + skillType);
					}
				}
				else
				{
					block = block.replace("{icon}", "icon.high_tab");
					block = block.replace("{background}", "background=\"l2ui_ch3.multisell_plusicon\"");
					block = block.replace("{bypass}", "bypass -h voiced_addSummonSkill " + summonSkillType + " " + skillType);
				}
			}
			else
			{
				block = block.replace("{icon}", "icon.high_tab");
				block = block.replace("{background}", "background=\"l2ui_ch3.multisell_plusicon\"");
				block = block.replace("{bypass}", "bypass -h voiced_addSummonSkill " + summonSkillType + " " + skillType);
			}
			list += block;
		}
		
		final boolean farmSummonDelaySkills = player.getFarmSystem().isExtraSummonDelaySkill();
		if (farmSummonDelaySkills)
		{
			html = html.replace("%delaySk_img%", "L2UI.CheckBox_checked");
		}
		else
		{
			html = html.replace("%delaySk_img%", "L2UI.CheckBox");
		}
		
		final boolean farmSummonPhysAttack = player.getFarmSystem().isAllowSummonPhysAttack();
		if (farmSummonPhysAttack)
		{
			html = html.replace("%physAtk_img%", "L2UI.CheckBox_checked");
		}
		else
		{
			html = html.replace("%physAtk_img%", "L2UI.CheckBox");
		}
		
		html = html.replace("%delaySk_bypass%", "bypass -h voiced_editSummonFarmOption " + summonSkillType + " farmSummonDelaySkills " + skillType);
		html = html.replace("%physAtk_bypass%", "bypass -h voiced_editSummonFarmOption " + summonSkillType + " farmSummonPhysAtk " + skillType);
		html = html.replace("%skillType%", skillType);
		html = html.replace("%skillsParam%", htm);
		html = html.replace("%summonSkillType%", summonSkillType);
		html = html.replace("%skillList%", list);
		html = html.replace("%chanceAttack%", chanceAttack.equals("") ? "" : chanceAttack);
		html = html.replace("%chanceSelf%", chanceSelf.equals("") ? "" : chanceSelf);
		html = html.replace("%chanceLowHeal%", chanceLowHeal.equals("") ? "" : chanceLowHeal);
		html = html.replace("%percentAttack%", percentAttack.equals("") ? "" : percentAttack);
		html = html.replace("%percentSelf%", percentSelf.equals("") ? "" : percentSelf);
		html = html.replace("%percentLowHeal%", percentLowHeal.equals("") ? "" : percentLowHeal);
		Util.setHtml(html, false, player);
	}
	
	private void showSummonSkillList(Player player, int farmType, String summonSkillType, String skillType, int page)
	{
		if (!player.hasSummon())
		{
			player.sendMessage("You can't use this option!");
			showSummonSkillInfo(player, null, farmType, summonSkillType, skillType);
			return;
		}
		
		final Summon summon = player.getSummon();
		if (summon.isPet() && (summon.getLevel() - player.getLevel()) > 20)
		{
			player.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
			showSummonSkillInfo(player, null, farmType, summonSkillType, skillType);
			return;
		}
		
		List<Integer> skillList = null;
		final List<Skill> summonSkillList = new ArrayList<>();
		final List<Integer> summonSkills = summon.isPet() ? PetsParser.getInstance().getPetData(summon.getId()).getAllSkills() : summon.getTemplate().getAllSkills();
		switch (summonSkillType)
		{
			case "attack" :
				skillList = player.getFarmSystem().getSummonAttackSpells();
				for (final int skillId : summonSkills)
				{
					Skill skill = null;
					if (summon.isPet())
					{
						skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
					}
					else
					{
						skill = summon.getTemplate().getSkill(skillId);
					}
					
					if (skill != null && skill.isAttackSkill())
					{
						summonSkillList.add(skill);
					}
				}
				break;
			case "self" :
				skillList = player.getFarmSystem().getSummonSelfSpells();
				for (final int skillId : summonSkills)
				{
					Skill skill = null;
					if (summon.isPet())
					{
						skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
					}
					else
					{
						skill = summon.getTemplate().getSkill(skillId);
					}
					
					if (skill != null && !skill.isNotSelfSkill())
					{
						summonSkillList.add(skill);
					}
				}
				break;
			case "heal" :
				skillList = player.getFarmSystem().getSummonHealSpells();
				for (final int skillId : summonSkills)
				{
					Skill skill = null;
					if (summon.isPet())
					{
						skill = PetsParser.getInstance().getPetData(summon.getId()).getAvailableSkill(skillId, summon.getLevel());
					}
					else
					{
						skill = summon.getTemplate().getSkill(skillId);
					}
					
					if (skill != null && !skill.isNotNotHealSkill())
					{
						summonSkillList.add(skill);
					}
				}
				break;
		}
		
		if (skillList.size() > 0)
		{
			final List<Skill> removeSkills = new ArrayList<>();
			for (final Skill skill : summonSkillList)
			{
				if (skillList.contains(skill.getId()))
				{
					removeSkills.add(skill);
				}
			}
			
			if (!removeSkills.isEmpty())
			{
				for (final Skill sk : removeSkills)
				{
					summonSkillList.remove(sk);
				}
				removeSkills.clear();
			}
		}
		
		if (summonSkillList.isEmpty())
		{
			player.sendMessage("Your summon has no valid skills!");
			showSummonSkillInfo(player, null, farmType, summonSkillType, skillType);
			return;
		}
		
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/summon_skills.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/autofarm/summon_skills_template.htm");
		String block = "";
		String list = "";
		
		final int perpage = 5;
		int counter = 0;
		final int totalSize = summonSkillList.size();
		final boolean isThereNextPage = totalSize > perpage;
		
		for (int i = (page - 1) * perpage; i < totalSize; i++)
		{
			final Skill data = summonSkillList.get(i);
			if (data != null)
			{
				block = template;
				
				block = block.replace("{name}", data.getName(player.getLang()));
				block = block.replace("{icon}", data.getIcon());
				block = block.replace("{bypass}", "bypass -h voiced_addNewSummonSkill " + data.getId() + " " + summonSkillType + " " + skillType + "");
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
		html = html.replace("{summonSkillType}", summonSkillType);
		html = html.replace("{skillType}", skillType);
		html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "voiced_addSummonSkill " + summonSkillType + " " + skillType + " %s"));
		Util.setHtml(html, false, player);
		summonSkillList.clear();
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}

	public static void main(String[] args)
	{
		if(VoicedCommandHandler.getInstance().getHandler("autofarm") == null)
			VoicedCommandHandler.getInstance().registerHandler(new AutoFarm());
	}
}