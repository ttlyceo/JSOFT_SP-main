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
package l2e.gameserver.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ClassListParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;

public class CharacterSkillsDAO extends LoggerObject
{
	private static final String RESTORE_SKILLS_FOR_CHAR_DEFAULT = "SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?";
	private static final String RESTORE_SKILLS_FOR_CHAR_MODIFER = "SELECT skill_id,skill_level FROM character_skills WHERE charId=? ORDER BY skill_id , skill_level ASC";
	private static final String ADD_NEW_SKILL = "INSERT INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String ADD_NEW_SKILLS = "REPLACE INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
	
	public void remove(Player player, int skillId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);
			statement.setInt(1, skillId);
			statement.setInt(2, player.getObjectId());
			statement.setInt(3, player.getClassIndex());
			statement.execute();
		}
		catch (final Exception e)
		{
			warn("Error could not delete skill: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void storeSkills(Player player, List<Skill> newSkills, int newClassIndex)
	{
		if (newSkills.isEmpty())
		{
			return;
		}
		
		final var classIndex = (newClassIndex > -1) ? newClassIndex : player.getClassIndex();
		Connection con = null;
		PreparedStatement ps = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement(ADD_NEW_SKILLS);
			con.setAutoCommit(false);
			for (final var addSkill : newSkills)
			{
				
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, addSkill.getId());
				ps.setInt(3, addSkill.getLevel());
				ps.setInt(4, classIndex);
				ps.addBatch();
			}
			ps.executeBatch();
			con.commit();
		}
		catch (final SQLException e)
		{
			warn("Error could not store char skills: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, ps);
		}
	}
	
	public void store(Player player, Skill newSkill, Skill oldSkill, int newClassIndex)
	{
		var classIndex = player.getClassIndex();
		
		if (newClassIndex > -1)
		{
			classIndex = newClassIndex;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			if ((oldSkill != null) && (newSkill != null))
			{
				statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, player.getObjectId());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else if (newSkill != null)
			{
				statement = con.prepareStatement(ADD_NEW_SKILL);
				statement.setInt(1, player.getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else
			{
				warn("could not store new skill. its NULL");
			}
		}
		catch (final Exception e)
		{
			warn("Error could not store char skills: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void restoreSkills(Player player)
	{
		final var isAcumulative = Config.SUBCLASS_STORE_SKILL;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(isAcumulative ? RESTORE_SKILLS_FOR_CHAR_MODIFER : RESTORE_SKILLS_FOR_CHAR_DEFAULT);
			statement.setInt(1, player.getObjectId());
			if (!isAcumulative)
			{
				statement.setInt(2, player.getClassIndex());
			}
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var id = rset.getInt("skill_id");
				final var level = rset.getInt("skill_level");
				if ((id > 9000) && (id < 9007))
				{
					continue;
				}
				
				final var skill = SkillsParser.getInstance().getInfo(id, level);
				if (skill == null)
				{
					continue;
				}
				
				final var isBlockRemove = skill.isBlockRemove();
				if (isBlockRemove)
				{
					final var sk = player.getKnownSkill(skill.getId());
					if (sk != null && sk.getLevel() > skill.getLevel())
					{
						continue;
					}
					player.addSkill(skill);
				}
				else
				{
					player.addSkill(skill);
				}
				
				if (Config.SECURITY_SKILL_CHECK && !player.canOverrideCond(PcCondOverride.SKILL_CONDITIONS))
				{
					if (isBlockRemove)
					{
						continue;
					}
					if (!SkillTreesParser.getInstance().isSkillAllowed(player, skill))
					{
						Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " has invalid skill " + skill.getName(null) + " (" + skill.getId() + "/" + skill.getLevel() + "), class:" + ClassListParser.getInstance().getClass(player.getClassId()).getClassName());
						if (Config.SECURITY_SKILL_CHECK_CLEAR)
						{
							player.removeSkill(skill);
						}
					}
				}
			}
			rset.close();
		}
		catch (final Exception e)
		{
			warn("Could not restore character " + this + " skills: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public static CharacterSkillsDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharacterSkillsDAO _instance = new CharacterSkillsDAO();
	}
}