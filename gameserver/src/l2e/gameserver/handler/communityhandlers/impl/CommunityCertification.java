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
package l2e.gameserver.handler.communityhandlers.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.RequestAcquireSkill;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Created by LordWinter
 */
public class CommunityCertification extends AbstractCommunity implements ICommunityBoardHandler
{
	private static final String[] QUEST_VAR_NAMES_65 =
	{
	        "EmergentAbility65-"
	};
	
	private static final String[] QUEST_VAR_NAMES_75 =
	{
	        "EmergentAbility70-", "ClassAbility75-"
	};
	
	private static final String[] QUEST_VAR_NAMES_80 =
	{
	        "ClassAbility80-"
	};
	
	private final Map<Integer, List<Integer>> _skills65List = new HashMap<>();
	private final Map<Integer, List<Integer>> _skills75List = new HashMap<>();
	private final Map<Integer, List<Integer>> _skills80List = new HashMap<>();
	
	public CommunityCertification()
	{
		load();
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}

	protected void load()
	{
		for (final var classId : ClassId.values())
		{
			if (classId.getRace() == null || classId.level() < 3)
			{
				continue;
			}
			
			final List<Integer> skillList65 = new ArrayList<>();
			final List<Integer> skillList75 = new ArrayList<>();
			final List<Integer> skillList80 = new ArrayList<>();
			for (final var sl : SkillTreesParser.getInstance().getSubClassSkillTree().values())
			{
				if (sl != null)
				{
					if (sl.getGetLevel() == 65)
					{
						if (skillList65.contains(sl.getId()))
						{
							continue;
						}
						skillList65.add(sl.getId());
					}
					else if (sl.getGetLevel() == 75)
					{
						if (skillList75.contains(sl.getId()))
						{
							continue;
						}
						skillList75.add(sl.getId());
					}
					else if (sl.getGetLevel() == 80)
					{
						if (skillList80.contains(sl.getId()))
						{
							continue;
						}
						skillList80.add(sl.getId());
					}
				}
			}
			_skills65List.put(classId.getId(), skillList65);
			_skills75List.put(classId.getId(), skillList75);
			_skills80List.put(classId.getId(), skillList80);
		}
		
		if (Config.CERT_BLOCK_SKILL_LIST != null && !Config.CERT_BLOCK_SKILL_LIST.isEmpty())
		{
			final String[] allInfo = Config.CERT_BLOCK_SKILL_LIST.split(";");
			for (final String allClasses : allInfo)
			{
				final String[] classes = allClasses.split(":");
				final int classId = Integer.parseInt(classes[0]);
				final String[] skillList = classes[1].split(",");
				final List<Integer> removeSkills = new ArrayList<>();
				for (final String skillId : skillList)
				{
					removeSkills.add(Integer.parseInt(skillId));
				}
				
				if (_skills65List.containsKey(classId))
				{
					for (final int sk : removeSkills)
					{
						_skills65List.get(classId).remove(Integer.valueOf(sk));
					}
				}
				
				if (_skills75List.containsKey(classId))
				{
					for (final int sk : removeSkills)
					{
						_skills75List.get(classId).remove(Integer.valueOf(sk));
					}
				}
				
				if (_skills80List.containsKey(classId))
				{
					for (final int sk : removeSkills)
					{
						_skills80List.get(classId).remove(Integer.valueOf(sk));
					}
				}
				removeSkills.clear();
			}
		}
	}

	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbscertification", "_bbscertup", "_bbscertdown", "_bbscert75learn", "_bbscert75skill", "_bbscert75reset", "_bbscert80learn", "_bbscert80skill", "_bbscert80reset"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		if (!checkUseCondition(player))
		{
			return;
		}

		final StringTokenizer st = new StringTokenizer(command, "_");
		final String cmd = st.nextToken();
		
		if (!checkCondition(player, cmd, false, false))
		{
			return;
		}
		
		if ("bbscertification".equals(cmd))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/certification/certification.htm");
			html = getCertificationInfo(html, player);
			separateAndSend(html, player);
		}
		else if ("bbscertup".equals(cmd))
		{
			final int skillId = Integer.parseInt(st.nextToken());
			final int skillLevel = Integer.parseInt(st.nextToken());

			if (getAttemptCount(player, 65) == 0)
			{
				onBypassCommand("_bbscertification", player);
				return;
			}

			if (skillId != 0)
			{
				if (Config.ALLOW_CERT_DONATE_MODE)
				{
					if (Config.EMERGET_SKILLS_LEARN[0] > 0)
					{
						if (player.getInventory().getItemByItemId(Config.EMERGET_SKILLS_LEARN[0]) == null)
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
						if (player.getInventory().getItemByItemId(Config.EMERGET_SKILLS_LEARN[0]).getCount() < Config.EMERGET_SKILLS_LEARN[1])
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
						player.destroyItemByItemId("EmergetSkill", Config.EMERGET_SKILLS_LEARN[0], Config.EMERGET_SKILLS_LEARN[1], player, true);
						Util.addServiceLog(player.getName(null) + " buy certification skillId: " + skillId + " level: " + skillLevel);
					}
					
					final var sk = SkillsParser.getInstance().getInfo(skillId, skillLevel);
					if (sk != null)
					{
						player.addSkill(sk, true);
						player.sendSkillList(false);
						player.sendUserInfo();
					}
				}
				else
				{
					if (player.isSubClassActive())
					{
						player.sendPacket(SystemMessageId.SKILL_NOT_FOR_SUBCLASS);
						onBypassCommand("_bbscertification", player);
						return;
					}
					
					final var skill = selectSkill(skillId, skillLevel);
					if (skill != null)
					{
						for (final var item : skill.getRequiredItems(AcquireSkillType.SUBCLASS))
						{
							if (player.getInventory().getItemByItemId(item.getId()) == null)
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
							if (player.getInventory().getItemByItemId(item.getId()).getCount() < item.getCount())
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
						}
						
						if (isValidGiveSkill(player, skill, 65))
						{
							final var sk = SkillsParser.getInstance().getInfo(skill.getId(), skill.getLvl());
							if (sk != null)
							{
								player.addSkill(sk, true);
								player.sendSkillList(false);
								player.sendUserInfo();
							}
						}
					}
				}
			}
			onBypassCommand("_bbscertification", player);
		}
		else if ("bbscertdown".equals(cmd))
		{
			final int skillId = Integer.parseInt(st.nextToken());
			final int skillLevel = Integer.parseInt(st.nextToken());

			if (skillLevel < 0)
			{
				onBypassCommand("_bbscertification", player);
				return;
			}
			
			if (!Config.ALLOW_CERT_DONATE_MODE)
			{
				if (player.isSubClassActive())
				{
					player.sendPacket(SystemMessageId.SKILL_NOT_FOR_SUBCLASS);
					onBypassCommand("_bbscertification", player);
					return;
				}
			}
			
			boolean foundSkill = false;
			if (skillLevel == 0)
			{
				final var skill = player.getKnownSkill(skillId);
				if (skill != null)
				{
					foundSkill = true;
					player.removeSkill(skill, true);
				}
			}
			else
			{
				if (skillId != 0)
				{
					final var sk = SkillsParser.getInstance().getInfo(skillId, skillLevel);
					if (sk != null)
					{
						foundSkill = true;
						player.addSkill(sk, true);
					}
				}
			}
			
			if (!Config.ALLOW_CERT_DONATE_MODE && foundSkill)
			{
				final var skill = selectSkill(skillId, skillLevel > 0 ? skillLevel : 1);
				if (skill != null)
				{
					for (final var item : skill.getRequiredItems(AcquireSkillType.SUBCLASS))
					{
						if (item != null)
						{
							player.addItem("Return Book", item.getId(), item.getCount(), null, true);
						}
					}
				}
			}
			player.sendSkillList(false);
			player.sendUserInfo();
			onBypassCommand("_bbscertification", player);
		}
		else if ("bbscert75learn".equals(cmd))
		{
			final int attempt = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			if (getAttemptCount(player, 75) == 0)
			{
				return;
			}
			
			if (!Config.ALLOW_CERT_DONATE_MODE)
			{
				if (player.isSubClassActive())
				{
					player.sendPacket(SystemMessageId.SKILL_NOT_FOR_SUBCLASS);
					return;
				}
			}
			check75Skills(player, page, attempt);
		}
		else if ("bbscert75skill".equals(cmd))
		{
			if (getAttemptCount(player, 75) == 0)
			{
				onBypassCommand("_bbscertification", player);
				return;
			}
			
			final int skillId = Integer.parseInt(st.nextToken());
			final int skillLevel = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			if (!checkValidSkill(player, _skills75List.get(player.getClassId().getId()), skillId, skillLevel))
			{
				onBypassCommand("_bbscertification", player);
				return;
			}
			
			if (skillId != 0)
			{
				if (Config.ALLOW_CERT_DONATE_MODE)
				{
					if (Config.MASTER_SKILLS_LEARN[0] > 0)
					{
						if (player.getInventory().getItemByItemId(Config.MASTER_SKILLS_LEARN[0]) == null)
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
						if (player.getInventory().getItemByItemId(Config.MASTER_SKILLS_LEARN[0]).getCount() < Config.MASTER_SKILLS_LEARN[1])
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
						player.destroyItemByItemId("MasterSkill", Config.MASTER_SKILLS_LEARN[0], Config.MASTER_SKILLS_LEARN[1], player, true);
						Util.addServiceLog(player.getName(null) + " buy certification skillId: " + skillId + " level: " + skillLevel);
					}
					
					final var sk = SkillsParser.getInstance().getInfo(skillId, skillLevel);
					if (sk != null)
					{
						player.addSkill(sk, true);
						player.sendSkillList(false);
						player.sendUserInfo();
					}
				}
				else
				{
					if (player.isSubClassActive())
					{
						player.sendPacket(SystemMessageId.SKILL_NOT_FOR_SUBCLASS);
						onBypassCommand("_bbscertification", player);
						return;
					}
					
					final var skill = selectSkill(skillId, skillLevel);
					if (skill != null)
					{
						for (final var item : skill.getRequiredItems(AcquireSkillType.SUBCLASS))
						{
							if (player.getInventory().getItemByItemId(item.getId()) == null)
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
							if (player.getInventory().getItemByItemId(item.getId()).getCount() < item.getCount())
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
						}
						
						if (isValidGiveSkill(player, skill, 75))
						{
							final var sk = SkillsParser.getInstance().getInfo(skill.getId(), skill.getLvl());
							if (sk != null)
							{
								player.addSkill(sk, true);
								player.sendSkillList(false);
								player.sendUserInfo();
							}
						}
					}
				}
			}
			
			final var attemps = getAttemptCount(player, 75);
			if (attemps > 0)
			{
				check75Skills(player, page, attemps);
			}
			onBypassCommand("_bbscertification", player);
		}
		else if ("bbscert75reset".equals(cmd))
		{
			final var skills75List = _skills75List.get(player.getClassId().getId());
			if (skills75List == null)
			{
				return;
			}
			
			if (Config.ALLOW_CERT_DONATE_MODE)
			{
				if (Config.CLEAN_SKILLS_LEARN[0] > 0)
				{
					if (player.getInventory().getItemByItemId(Config.CLEAN_SKILLS_LEARN[0]) == null)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					if (player.getInventory().getItemByItemId(Config.CLEAN_SKILLS_LEARN[0]).getCount() < Config.CLEAN_SKILLS_LEARN[1])
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					player.destroyItemByItemId("CleanSkills", Config.CLEAN_SKILLS_LEARN[0], Config.CLEAN_SKILLS_LEARN[1], player, true);
					Util.addServiceLog(player.getName(null) + " buy reset certification skills!");
				}
				
				for (final int skillId : skills75List)
				{
					final var skill = player.getKnownSkill(skillId);
					if (skill != null)
					{
						player.removeSkill(skill, true);
					}
				}
			}
			else
			{
				if (player.isSubClassActive())
				{
					player.sendPacket(SystemMessageId.SKILL_NOT_FOR_SUBCLASS);
					onBypassCommand("_bbscertification", player);
					return;
				}
				
				for (final int skillId : skills75List)
				{
					final var skill = player.getKnownSkill(skillId);
					if (skill != null)
					{
						final var sl = selectSkill(skillId, 1);
						if (sl != null)
						{
							isValidRemoveSkill(player, sl, 75);
						}
					}
				}
			}
			player.sendSkillList(false);
			player.sendUserInfo();
			onBypassCommand("_bbscertification", player);
		}
		else if ("bbscert80learn".equals(cmd))
		{
			final int attempt = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			if (getAttemptCount(player, 80) == 0)
			{
				return;
			}
			
			if (!Config.ALLOW_CERT_DONATE_MODE)
			{
				if (!RequestAcquireSkill.canTransform(player))
				{
					return;
				}
				
				if (player.isSubClassActive())
				{
					player.sendPacket(SystemMessageId.SKILL_NOT_FOR_SUBCLASS);
					onBypassCommand("_bbscertification", player);
					return;
				}
			}
			check80Skills(player, page, attempt);
		}
		else if ("bbscert80skill".equals(cmd))
		{
			if (getAttemptCount(player, 80) == 0)
			{
				onBypassCommand("_bbscertification", player);
				return;
			}
			
			final int skillId = Integer.parseInt(st.nextToken());
			final int skillLevel = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			if (!checkValidSkill(player, _skills80List.get(player.getClassId().getId()), skillId, skillLevel))
			{
				onBypassCommand("_bbscertification", player);
				return;
			}
			
			if (skillId != 0)
			{
				if (Config.ALLOW_CERT_DONATE_MODE)
				{
					if (Config.TRANSFORM_SKILLS_LEARN[0] > 0)
					{
						if (player.getInventory().getItemByItemId(Config.TRANSFORM_SKILLS_LEARN[0]) == null)
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
						if (player.getInventory().getItemByItemId(Config.TRANSFORM_SKILLS_LEARN[0]).getCount() < Config.TRANSFORM_SKILLS_LEARN[1])
						{
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
							return;
						}
						player.destroyItemByItemId("CleanSkills", Config.TRANSFORM_SKILLS_LEARN[0], Config.TRANSFORM_SKILLS_LEARN[1], player, true);
						Util.addServiceLog(player.getName(null) + " buy certification trasform skillId: " + skillId + " level: " + skillLevel);
					}
					
					final var sk = SkillsParser.getInstance().getInfo(skillId, skillLevel);
					if (sk != null)
					{
						player.addSkill(sk, true);
						player.sendSkillList(false);
						player.sendUserInfo();
					}
				}
				else
				{
					if (!RequestAcquireSkill.canTransform(player))
					{
						onBypassCommand("_bbscertification", player);
						return;
					}
					
					if (player.isSubClassActive())
					{
						player.sendPacket(SystemMessageId.SKILL_NOT_FOR_SUBCLASS);
						onBypassCommand("_bbscertification", player);
						return;
					}
					
					final var skill = selectSkill(skillId, skillLevel);
					if (skill != null)
					{
						for (final var item : skill.getRequiredItems(AcquireSkillType.SUBCLASS))
						{
							if (player.getInventory().getItemByItemId(item.getId()) == null)
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
							if (player.getInventory().getItemByItemId(item.getId()).getCount() < item.getCount())
							{
								player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
								return;
							}
						}
						
						if (isValidGiveSkill(player, skill, 80))
						{
							final var sk = SkillsParser.getInstance().getInfo(skill.getId(), skill.getLvl());
							if (sk != null)
							{
								player.addSkill(sk, true);
								player.sendSkillList(false);
								player.sendUserInfo();
							}
						}
					}
				}
			}
			
			final var attempts = getAttemptCount(player, 80);
			if (attempts > 0)
			{
				check80Skills(player, page, attempts);
			}
			onBypassCommand("_bbscertification", player);
		}
		else if ("bbscert80reset".equals(cmd))
		{
			final var skills80List = _skills80List.get(player.getClassId().getId());
			if (skills80List == null)
			{
				return;
			}
			
			if (Config.ALLOW_CERT_DONATE_MODE)
			{
				if (Config.CLEAN_SKILLS_LEARN[0] > 0)
				{
					if (player.getInventory().getItemByItemId(Config.CLEAN_SKILLS_LEARN[0]) == null)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					if (player.getInventory().getItemByItemId(Config.CLEAN_SKILLS_LEARN[0]).getCount() < Config.CLEAN_SKILLS_LEARN[1])
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						return;
					}
					player.destroyItemByItemId("CleanSkills", Config.CLEAN_SKILLS_LEARN[0], Config.CLEAN_SKILLS_LEARN[1], player, true);
					Util.addServiceLog(player.getName(null) + " buy reset certification skillIs!");
				}
				
				for (final int skillId : skills80List)
				{
					final var skill = player.getKnownSkill(skillId);
					if (skill != null)
					{
						player.removeSkill(skill, true);
					}
				}
			}
			else
			{
				if (!RequestAcquireSkill.canTransform(player))
				{
					onBypassCommand("_bbscertification", player);
					return;
				}
				
				if (player.isSubClassActive())
				{
					player.sendPacket(SystemMessageId.SKILL_NOT_FOR_SUBCLASS);
					onBypassCommand("_bbscertification", player);
					return;
				}
				
				for (final int skillId : skills80List)
				{
					final var skill = player.getKnownSkill(skillId);
					if (skill != null)
					{
						player.removeSkill(skill, true);
						final var sl = selectSkill(skillId, 3);
						if (sl != null)
						{
							isValidRemoveSkill(player, sl, 80);
						}
					}
				}
			}
			player.sendSkillList(false);
			player.sendUserInfo();
			onBypassCommand("_bbscertification", player);
		}
	}
	
	private int getAttemptCount(Player player, int type)
	{
		var attempts = 0;
		switch (type)
		{
			case 65 :
				attempts = 6;
				final var skills65List = _skills65List.get(player.getClassId().getId());
				for (int i = 0; i < skills65List.size(); i++)
				{
					final var skill = SkillsParser.getInstance().getInfo(skills65List.get(i), 1);
					if (skill != null)
					{
						final var plSkill = player.getKnownSkill(skill.getId());
						if (plSkill != null)
						{
							attempts -= plSkill.getLevel();
						}
					}
				}
				return attempts;
			case 75 :
				attempts = 3;
				final var skills75List = _skills75List.get(player.getClassId().getId());
				int amount75 = 0;
				for (int i = 0; i < skills75List.size(); i++)
				{
					final var plSkill = player.getKnownSkill(skills75List.get(i));
					{
						if (plSkill != null && amount75 < 3)
						{
							amount75++;
							attempts--;
						}
					}
				}
				return attempts;
			case 80 :
				attempts = 3;
				final var skills80List = _skills80List.get(player.getClassId().getId());
				int amount80 = 0;
				for (int i = 0; i < skills80List.size(); i++)
				{
					final var plSkill = player.getKnownSkill(skills80List.get(i));
					{
						if (plSkill != null && amount80 < 3)
						{
							amount80++;
							attempts--;
						}
					}
				}
				return attempts;
		}
		return attempts;
	}
	
	private boolean isValidGiveSkill(Player player, SkillLearn skill, int level)
	{
		var qs = player.getQuestState("SubClassSkills");
		if (qs == null)
		{
			final var subClassSkilllsQuest = QuestManager.getInstance().getQuest("SubClassSkills");
			if (subClassSkilllsQuest != null)
			{
				qs = subClassSkilllsQuest.newQuestState(player);
			}
			else
			{
				_log.warn("Null SubClassSkills quest, for Sub-Class skill Id: " + skill.getId() + " level: " + skill.getLvl() + " for player " + player.getName(null) + "!");
				return false;
			}
		}
		
		final var names = level == 65 ? QUEST_VAR_NAMES_65 : level == 75 ? QUEST_VAR_NAMES_75 : QUEST_VAR_NAMES_80;
		
		for (final String varName : names)
		{
			for (int i = 1; i <= Config.MAX_SUBCLASS; i++)
			{
				final String itemOID = qs.getGlobalQuestVar(varName + i);
				if (!itemOID.isEmpty() && !itemOID.endsWith(";") && !itemOID.equals("0"))
				{
					if (Util.isDigit(itemOID))
					{
						final int itemObjId = Integer.parseInt(itemOID);
						final var item = player.getInventory().getItemByObjectId(itemObjId);
						if (item != null)
						{
							for (final var itemIdCount : skill.getRequiredItems(AcquireSkillType.SUBCLASS))
							{
								if (item.getId() == itemIdCount.getId())
								{
									if (!player.destroyItemByItemId("CertLearn", itemIdCount.getId(), itemIdCount.getCount(), null, true))
									{
										Util.handleIllegalPlayerAction(player, "" + player.getName(null) + ", level " + player.getLevel() + " lose required item Id: " + itemIdCount.getId() + " to learn skill while learning skill Id: " + skill.getId() + " level " + skill.getLvl() + "!");
										return false;
									}
									else
									{
										qs.saveGlobalQuestVar(varName + i, skill.getId() + ";");
										return true;
									}
								}
							}
						}
					}
				}
				else
				{
					if (level == 65)
					{
						for (final var itemIdCount : skill.getRequiredItems(AcquireSkillType.SUBCLASS))
						{
							if (!player.destroyItemByItemId("CertLearn", itemIdCount.getId(), itemIdCount.getCount(), null, true))
							{
								Util.handleIllegalPlayerAction(player, "" + player.getName(null) + ", level " + player.getLevel() + " lose required item Id: " + itemIdCount.getId() + " to learn skill while learning skill Id: " + skill.getId() + " level " + skill.getLvl() + "!");
								return false;
							}
							else
							{
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean isValidRemoveSkill(Player player, SkillLearn skill, int level)
	{
		var qs = player.getQuestState("SubClassSkills");
		if (qs == null)
		{
			final var subClassSkilllsQuest = QuestManager.getInstance().getQuest("SubClassSkills");
			if (subClassSkilllsQuest != null)
			{
				qs = subClassSkilllsQuest.newQuestState(player);
			}
			else
			{
				_log.warn("Null SubClassSkills quest, for Sub-Class skill Id: " + skill.getId() + " level: " + skill.getLvl() + " for player " + player.getName(null) + "!");
				return false;
			}
		}
		
		final var names = level == 75 ? QUEST_VAR_NAMES_75 : QUEST_VAR_NAMES_80;
		
		for (final String varName : names)
		{
			for (int i = 1; i <= Config.MAX_SUBCLASS; i++)
			{
				final String qvarName = varName + i;
				final String qvar = qs.getGlobalQuestVar(qvarName);
				if (qvar.endsWith(";"))
				{
					final String skillIdVar = qvar.replace(";", "");
					if (Util.isDigit(skillIdVar))
					{
						final int skillId = Integer.parseInt(skillIdVar);
						final Skill sk = SkillsParser.getInstance().getInfo(skillId, 1);
						if (sk != null)
						{
							player.removeSkill(sk);
							for (final var item : skill.getRequiredItems(AcquireSkillType.SUBCLASS))
							{
								if (item != null)
								{
									final var it = player.addItem("Return Book", item.getId(), item.getCount(), null, true);
									qs.saveGlobalQuestVar(qvarName, "" + it.getObjectId());
								}
							}
						}
					}
					else
					{
						_log.warn("Invalid Sub-Class Skill Id: " + skillIdVar + " for player " + player.getName(null) + "!");
					}
				}
			}
		}
		return true;
	}

	private String getCertificationInfo(String html, Player player)
	{
		final var attempt75 = getAttemptCount(player, 75);
		final var attempt80 = getAttemptCount(player, 80);

		final var skills65List = _skills65List.get(player.getClassId().getId());
		for (int i = 0; i < skills65List.size(); i++)
		{
			final var skill = SkillsParser.getInstance().getInfo(skills65List.get(i), 1);
			if (skill != null)
			{
				html = html.replace("%65skillName-" + i + "%", checkSkillName(skill.getName("en")));
				html = html.replace("%65skillIcon-" + i + "%", skill.getIcon());
				final var plSkill = player.getKnownSkill(skill.getId());
				int nextLvl;
				if (plSkill != null)
				{
					nextLvl = plSkill.getLevel();
					html = html.replace("%65skillLvl-" + i + "%", String.valueOf(plSkill.getLevel()));
				}
				else
				{
					nextLvl = 0;
					html = html.replace("%65skillLvl-" + i + "%", String.valueOf(0));
				}
				html = html.replace("%65skillUp-" + i + "%", "<button action=\"bypass -h _bbscertup_" + skill.getId() + "_" + (nextLvl + 1) + "\" value=\"+\" width=22 height=16 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">");
				html = html.replace("%65skillDown-" + i + "%", "<button action=\"bypass -h _bbscertdown_" + skill.getId() + "_" + (nextLvl - 1) + "\" value=\"-\" width=22 height=16 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">");
				html = html.replace("%65skillInfo-" + i + "%", getSkillInfo(plSkill != null ? plSkill.getLevel() : 0));
			}
		}
		
		final List<Integer> skills75List = _skills75List.get(player.getClassId().getId());
		int amount75 = 0;
		for (int i = 0; i < skills75List.size(); i++)
		{
			final var plSkill = player.getKnownSkill(skills75List.get(i));
			{
				if (plSkill != null && amount75 < 3)
				{
					amount75++;
					html = html.replace("%75skillIcon-" + amount75 + "%", plSkill.getIcon());
					html = html.replace("%name75skill-" + amount75 + "%", checkSkillName(plSkill.getName("en")));
				}
			}
		}
		
		if (amount75 < 3)
		{
			switch (amount75)
			{
				case 0 :
					html = html.replace("%75skillIcon-1%", "icon.skill00000");
					html = html.replace("%75skillIcon-2%", "icon.skill00000");
					html = html.replace("%75skillIcon-3%", "icon.skill00000");
					html = html.replace("%name75skill-1%", "none");
					html = html.replace("%name75skill-2%", "none");
					html = html.replace("%name75skill-3%", "none");
					break;
				case 1 :
					html = html.replace("%75skillIcon-2%", "icon.skill00000");
					html = html.replace("%75skillIcon-3%", "icon.skill00000");
					html = html.replace("%name75skill-2%", "none");
					html = html.replace("%name75skill-3%", "none");
					break;
				case 2 :
					html = html.replace("%75skillIcon-3%", "icon.skill00000");
					html = html.replace("%name75skill-3%", "none");
					break;
			}
		}
		
		html = html.replace("%75attempt%", String.valueOf(attempt75));
		
		final List<Integer> skills80List = _skills80List.get(player.getClassId().getId());
		int amount80 = 0;
		for (int i = 0; i < skills80List.size(); i++)
		{
			final var plSkill = player.getKnownSkill(skills80List.get(i));
			{
				if (plSkill != null && amount80 < 3)
				{
					amount80++;
					html = html.replace("%80skillIcon-" + amount80 + "%", plSkill.getIcon());
					html = html.replace("%name80skill-" + amount80 + "%", checkSkillName(plSkill.getName("en")));
				}
			}
		}
		
		if (amount80 < 3)
		{
			switch (amount80)
			{
				case 0 :
					html = html.replace("%80skillIcon-1%", "icon.skill00000");
					html = html.replace("%80skillIcon-2%", "icon.skill00000");
					html = html.replace("%80skillIcon-3%", "icon.skill00000");
					html = html.replace("%name80skill-1%", "none");
					html = html.replace("%name80skill-2%", "none");
					html = html.replace("%name80skill-3%", "none");
					break;
				case 1 :
					html = html.replace("%80skillIcon-2%", "icon.skill00000");
					html = html.replace("%80skillIcon-3%", "icon.skill00000");
					html = html.replace("%name80skill-2%", "none");
					html = html.replace("%name80skill-3%", "none");
					break;
				case 2 :
					html = html.replace("%80skillIcon-3%", "icon.skill00000");
					html = html.replace("%name80skill-3%", "none");
					break;
			}
		}
		html = html.replace("%80attempt%", String.valueOf(attempt80));
		return html;
	}
	
	private String checkSkillName(String name)
	{
		String skillName = name;
		skillName = skillName.replace("Emergent Ability - ", "");
		skillName = skillName.replace("Master Ability - ", "");
		skillName = skillName.replace("Warrior Ability - ", "");
		skillName = skillName.replace("Rogue Ability - ", "");
		skillName = skillName.replace("Knight Ability - ", "");
		skillName = skillName.replace("Wizard Ability - ", "");
		skillName = skillName.replace("Healer Ability - ", "");
		skillName = skillName.replace("Summoner Ability - ", "");
		skillName = skillName.replace("Enchanter Ability - ", "");
		skillName = skillName.replace("Transform Divine ", "");
		return skillName;
	}

	private String getSkillInfo(int level)
	{
		switch (level)
		{
			case 1:
				return "+10";
			case 2:
				return "+20";
			case 3:
				return "+30";
			case 4:
				return "+40";
			case 5:
				return "+50";
			case 6:
				return "+60";
		}
		return "0";
	}
	
	private void check75Skills(Player player, int page, int attempt)
	{
		final List<Integer> _skills = new ArrayList<>();
		final var skillLearnList = _skills75List.get(player.getClassId().getId());
		int counts = 0;
		
		for (final int skillId : skillLearnList)
		{
			final var skill = player.getKnownSkill(skillId);
			if (skill != null)
			{
				continue;
			}
			counts++;
			_skills.add(skillId);
		}
		
		final int perpage = 6;
		final boolean isThereNextPage = _skills.size() > perpage;
		
		final var html = new NpcHtmlMessage(5);
		html.setFile(player, player.getLang(), "data/html/community/certification/skills.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/certification/template.htm");
		
		String block = "";
		String list = "";
		
		int countss = 0;
		
		for (int i = (page - 1) * perpage; i < _skills.size(); i++)
		{
			final int skillsId = _skills.get(i);
			if (skillsId != 0)
			{
				block = template;
				
				block = block.replace("%bypass%", "bypass -h _bbscert75skill_" + skillsId + "_1_" + page);
				block = block.replace("%name%", SkillsParser.getInstance().getInfo(skillsId, 1).getName(player.getLang()));
				block = block.replace("%icon%", SkillsParser.getInstance().getInfo(skillsId, 1).getIcon());
				list += block;
				countss++;
				
				if (countss >= perpage)
				{
					break;
				}
			}
		}
		
		final double pages = (double) _skills.size() / perpage;
		final int count = (int) Math.ceil(pages);
		if (counts == 0 || attempt == 0)
		{
			return;
		}
		html.replace("%list%", list);
		html.replace("%navigation%", Util.getNavigationBlock(count, page, _skills.size(), perpage, isThereNextPage, "_bbscert75learn_" + attempt + "_%s"));
		player.sendPacket(html);
	}
	
	private void check80Skills(Player player, int page, int attempt)
	{
		final List<Integer> _skills = new ArrayList<>();
		final var skillLearnList = _skills80List.get(player.getClassId().getId());
		int counts = 0;
		
		for (final int skillId : skillLearnList)
		{
			final var skill = player.getKnownSkill(skillId);
			if (skill != null && skill.getLevel() >= 3)
			{
				continue;
			}
			counts++;
			_skills.add(skillId);
		}
		
		final int perpage = 6;
		final boolean isThereNextPage = _skills.size() > perpage;
		
		final var html = new NpcHtmlMessage(5);
		html.setFile(player, player.getLang(), "data/html/community/certification/skills.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/certification/template.htm");
		
		String block = "";
		String list = "";
		
		int countss = 0;
		
		for (int i = (page - 1) * perpage; i < _skills.size(); i++)
		{
			final int skillsId = _skills.get(i);
			if (skillsId != 0)
			{
				int level = 1;
				final var plSkill = player.getKnownSkill(skillsId);
				if (plSkill != null)
				{
					level = plSkill.getLevel() + 1;
				}
				block = template;
				block = block.replace("%bypass%", "bypass -h _bbscert80skill_" + skillsId + "_" + level + "_" + page);
				block = block.replace("%name%", SkillsParser.getInstance().getInfo(skillsId, 1).getName(player.getLang()));
				block = block.replace("%icon%", SkillsParser.getInstance().getInfo(skillsId, 1).getIcon());
				list += block;
				countss++;
				
				if (countss >= perpage)
				{
					break;
				}
			}
		}
		
		final double pages = (double) _skills.size() / perpage;
		final int count = (int) Math.ceil(pages);
		if (counts == 0 || attempt == 0)
		{
			return;
		}
		html.replace("%list%", list);
		html.replace("%navigation%", Util.getNavigationBlock(count, page, _skills.size(), perpage, isThereNextPage, "_bbscert80learn_" + attempt + "_%s"));
		player.sendPacket(html);
	}

	private static boolean checkUseCondition(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		if (player.getClassId().level() < 3)
		{
			player.sendMessage("Your level of profession is too low!");
			return false;
		}

		if (player.getLevel() < Config.CERT_MIN_LEVEL)
		{
			player.sendMessage("Your level is lower! Need to be " + Config.CERT_MIN_LEVEL + " level.");
			return false;
		}
		return true;
	}
	
	private SkillLearn selectSkill(int skillId, int skillLvl)
	{
		for (final var sl : SkillTreesParser.getInstance().getSubClassSkillTree().values())
		{
			if (sl != null)
			{
				if (sl.getId() == skillId && sl.getLvl() == skillLvl)
				{
					return sl;
				}
			}
		}
		return null;
	}
	
	private boolean checkValidSkill(Player player, List<Integer> skillList, int id, int lvl)
	{
		if (player == null || skillList == null)
		{
			return false;
		}
		
		for (final int skillId : skillList)
		{
			if (skillId == id)
			{
				if (lvl == 1)
				{
					return true;
				}
				else
				{
					final var oldSkill = player.getKnownSkill(id);
					if (oldSkill != null)
					{
						if (lvl == (oldSkill.getLevel() + 1))
						{
							return true;
						}
					}
				}
				return false;
			}
		}
		return false;
	}
	
	@Override
	public void onWriteCommand(String command, String s, String s1, String s2, String s3, String s4, Player Player)
	{
	}
	
	public static CommunityCertification getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityCertification _instance = new CommunityCertification();
	}
}