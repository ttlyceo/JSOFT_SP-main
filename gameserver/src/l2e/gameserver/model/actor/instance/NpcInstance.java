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

import java.util.List;
import java.util.Map;

import l2e.commons.util.StringUtil;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.status.FolkStatus;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.AcquireSkillDone;
import l2e.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class NpcInstance extends Npc
{
	public NpcInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.NpcInstance);
		setIsInvul(false);
		setUndying(true);
	}

	@Override
	public FolkStatus getStatus()
	{
		return (FolkStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new FolkStatus(this));
	}
	
	public List<ClassId> getClassesToTeach()
	{
		return getTemplate().getTeachInfo();
	}
	
	public static void showCustomSkillList(Player activeChar, int groupId)
	{
		final var skills = SkillTreesParser.getInstance().getAvailableCustomSkills(activeChar, groupId);
		final var asl = new ExAcquirableSkillListByClass(AcquireSkillType.CUSTOM);
		if (skills == null || skills.isEmpty())
		{
			return;
		}
		
		int count = 0;
		for (final var s : skills)
		{
			final var sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
			if (sk == null)
			{
				continue;
			}
			count++;
			asl.addSkill(s.getId(), s.getGetLevel(), s.getLvl(), s.getLvl(), s.getLevelUpSp(), 1);
		}
		
		if (count == 0)
		{
			final var allMap = SkillTreesParser.getInstance().getCustomSkills(groupId);
			if (allMap != null)
			{
				final int minlLevel = SkillTreesParser.getInstance().getMinLevelForNewSkill(activeChar, allMap);
				if (minlLevel > 0)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
					sm.addNumber(minlLevel);
					activeChar.sendPacket(sm);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
				}
				activeChar.sendPacket(AcquireSkillDone.STATIC);
			}
		}
		else
		{
			activeChar.setLearningGroupId(groupId);
			activeChar.sendPacket(asl);
		}
	}

	public static void showSkillList(Player player, Npc npc, ClassId classId)
	{
		if (Config.DEBUG)
		{
			_log.info("SkillList activated on: " + npc.getObjectId());
		}
		
		if ((player.getWeightPenalty() >= 3) || !player.isInventoryUnder90(true))
		{
			player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
			return;
		}
		
		final int npcId = npc.getTemplate().getId();
		if (npcId == 32611)
		{
			final List<SkillLearn> skills = SkillTreesParser.getInstance().getAvailableCollectSkills(player);
			final ExAcquirableSkillListByClass asl = new ExAcquirableSkillListByClass(AcquireSkillType.COLLECT);
			
			int counts = 0;
			for (final SkillLearn s : skills)
			{
				final Skill sk = SkillsParser.getInstance().getInfo(s.getId(), s.getLvl());
				
				if (sk != null)
				{
					counts++;
					asl.addSkill(s.getId(), s.getGetLevel(), s.getLvl(), s.getLvl(), 0, 1);
				}
			}
			
			if (counts == 0)
			{
				final int minLevel = SkillTreesParser.getInstance().getMinLevelForNewSkill(player, SkillTreesParser.getInstance().getCollectSkillTree());
				if (minLevel > 0)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
					sm.addNumber(minLevel);
					player.sendPacket(sm);
				}
				else
				{
					player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
				}
				player.sendPacket(AcquireSkillDone.STATIC);
			}
			else
			{
				player.sendPacket(asl);
			}
			return;
		}
		
		if (!npc.getTemplate().canTeach(classId))
		{
			npc.showNoTeachHtml(player);
			return;
		}
		
		if (((NpcInstance) npc).getClassesToTeach().isEmpty())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			final String sb = StringUtil.concat("<html><body>" + "I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:", String.valueOf(npcId), ", Your classId:", String.valueOf(player.getClassId().getId()), "<br>" + "</body></html>");
			html.setHtml(player, sb);
			player.sendPacket(html);
			return;
		}
		final List<SkillLearn> skills = SkillTreesParser.getInstance().getAvailableSkills(player, classId, false, false);
		final ExAcquirableSkillListByClass asl = new ExAcquirableSkillListByClass(AcquireSkillType.CLASS);
		int count = 0;
		player.setLearningClass(classId);
		for (final SkillLearn s : skills)
		{
			if (SkillsParser.getInstance().getInfo(s.getId(), s.getLvl()) != null)
			{
				asl.addSkill(s.getId(), s.getGetLevel(), s.getLvl(), s.getLvl(), s.getCalculatedLevelUpSp(player.getClassId(), classId), 0);
				count++;
			}
		}
		
		if (count == 0)
		{
			final Map<Integer, SkillLearn> skillTree = SkillTreesParser.getInstance().getCompleteClassSkillTree(classId);
			final int minLevel = SkillTreesParser.getInstance().getMinLevelForNewSkill(player, skillTree);
			if (minLevel > 0)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
				sm.addNumber(minLevel);
				player.sendPacket(sm);
			}
			else
			{
				if (player.getClassId().level() == 1)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.NO_SKILLS_TO_LEARN_RETURN_AFTER_S1_CLASS_CHANGE);
					sm.addNumber(2);
					player.sendPacket(sm);
				}
				else
				{
					player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
				}
			}
		}
		else
		{
			player.sendPacket(asl);
		}
	}
}