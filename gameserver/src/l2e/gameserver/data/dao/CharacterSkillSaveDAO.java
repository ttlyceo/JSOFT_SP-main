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
import java.util.HashMap;
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.TimeStamp;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.stats.Env;

public class CharacterSkillSaveDAO extends LoggerObject
{
	private static final String ADD_SKILL_SAVE = "REPLACE INTO character_skills_save (charId,skill_id,skill_level,display_icon,effect_count,effect_cur_time,effect_total_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,display_icon,effect_count,effect_cur_time,effect_total_time,reuse_delay,systime,restore_type FROM character_skills_save WHERE charId=? AND class_index=? ORDER BY buff_index ASC";
	private static final String RESTORE_SKILL_SAVE_MODIFER = "SELECT skill_id,skill_level,display_icon,effect_count,effect_cur_time,effect_total_time,reuse_delay,systime,restore_type FROM character_skills_save WHERE charId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";
	private static final String DELETE_SKILL_SAVE_MODIFER = "DELETE FROM character_skills_save WHERE charId=?";
	
	public void restore(Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(Config.SUBCLASS_STORE_SKILL ? RESTORE_SKILL_SAVE_MODIFER : RESTORE_SKILL_SAVE);
			statement.setInt(1, player.getObjectId());
			if (!Config.SUBCLASS_STORE_SKILL)
			{
				statement.setInt(2, player.getClassIndex());
			}
			
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var isDisplay = rset.getInt("display_icon") == 0;
				final var effectCount = rset.getInt("effect_count");
				final var effectCurTime = rset.getInt("effect_cur_time");
				final var effectTotalTime = rset.getInt("effect_total_time");
				final var reuseDelay = rset.getLong("reuse_delay");
				final var systime = rset.getLong("systime");
				final var restoreType = rset.getInt("restore_type");
				
				final var skill = SkillsParser.getInstance().getInfo(rset.getInt("skill_id"), rset.getInt("skill_level"));
				if (skill == null)
				{
					continue;
				}
				
				final var remainingTime = systime - System.currentTimeMillis();
				if (remainingTime > 10)
				{
					player.disableSkill(skill, remainingTime);
					player.addTimeStamp(skill, reuseDelay, systime);
				}
				
				if (restoreType > 0)
				{
					continue;
				}
				
				if (skill.hasEffects())
				{
					final var env = new Env();
					env.setCharacter(player);
					env.setTarget(player);
					env.setSkill(skill);
					
					Effect ef;
					for (final var et : skill.getEffectTemplates())
					{
						ef = et.getEffect(env);
						if (ef != null)
						{
							if (ef.isIconDisplay() && !isDisplay)
							{
								continue;
							}
							
							switch (ef.getEffectType())
							{
								case CANCEL :
								case CANCEL_ALL :
								case CANCEL_BY_SLOT :
									continue;
							}
							ef.setCount(effectCount);
							ef.setAbnormalTime(effectTotalTime);
							ef.setFirstTime(effectCurTime);
							ef.scheduleEffect(true);
						}
					}
				}
			}
			statement.close();
			
			statement = con.prepareStatement(Config.SUBCLASS_STORE_SKILL ? DELETE_SKILL_SAVE_MODIFER : DELETE_SKILL_SAVE);
			statement.setInt(1, player.getObjectId());
			if (!Config.SUBCLASS_STORE_SKILL)
			{
				statement.setInt(2, player.getClassIndex());
			}
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			warn("Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void store(Player player, boolean isAcumulative, boolean storeEffects)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(isAcumulative ? DELETE_SKILL_SAVE_MODIFER : DELETE_SKILL_SAVE);
			
			statement.setInt(1, player.getObjectId());
			if (!isAcumulative)
			{
				statement.setInt(2, player.getClassIndex());
			}
			statement.execute();
			statement.close();
			
			var buff_index = 0;
			
			final Map<Integer, Integer> storedSkills = new HashMap<>();
			
			statement = con.prepareStatement(ADD_SKILL_SAVE);
			
			if (storeEffects)
			{
				for (final var effect : player.getAllEffects())
				{
					if (effect == null || effect.getSkill().isToggle())
					{
						continue;
					}
					
					switch (effect.getEffectType())
					{
						case HEAL_OVER_TIME :
						case CPHEAL_OVER_TIME :
						case HIDE :
						case CANCEL :
						case CANCEL_ALL :
						case CANCEL_BY_SLOT :
						case SIGNET_GROUND :
						case SIGNET_EFFECT :
							continue;
					}
					
					if (effect.getAbnormalType().equalsIgnoreCase("SEED_OF_KNIGHT") || effect.getAbnormalType().equalsIgnoreCase("LIFE_FORCE_OTHERS"))
					{
						continue;
					}
					
					final var skill = effect.getSkill();
					if (storedSkills.containsKey(skill.getReuseHashCode()))
					{
						final var displayIcon = storedSkills.get(skill.getReuseHashCode());
						if (displayIcon == 0 && !effect.isIconDisplay())
						{
							continue;
						}
					}
					
					if (skill.isDance() && !Config.ALT_STORE_DANCES)
					{
						continue;
					}
					
					storedSkills.put(skill.getReuseHashCode(), effect.isIconDisplay() ? 0 : 1);
					
					if (effect.isInUse() && !skill.isToggle())
					{
						statement.setInt(1, player.getObjectId());
						statement.setInt(2, skill.getId());
						statement.setInt(3, skill.getLevel());
						statement.setInt(4, effect.isIconDisplay() ? 0 : 1);
						statement.setInt(5, effect.getTickCount());
						statement.setInt(6, effect.getTime());
						statement.setInt(7, effect.getAbnormalTime());
						
						if (player.getSkillReuseTimeStamps().containsKey(skill.getReuseHashCode()))
						{
							final var t = player.getSkillReuseTimeStamps().get(skill.getReuseHashCode());
							statement.setLong(8, t.hasNotPassed() ? t.getReuse() : 0);
							statement.setDouble(9, t.hasNotPassed() ? t.getStamp() : 0);
						}
						else
						{
							statement.setLong(8, 0);
							statement.setDouble(9, 0);
						}
						statement.setInt(10, 0);
						statement.setInt(11, player.getClassIndex());
						statement.setInt(12, ++buff_index);
						statement.execute();
					}
				}
			}
			int hash;
			TimeStamp t;
			for (final var ts : player.getSkillReuseTimeStamps().entrySet())
			{
				hash = ts.getKey();
				if (storedSkills.containsKey(hash))
				{
					continue;
				}
				t = ts.getValue();
				if ((t != null) && t.hasNotPassed())
				{
					storedSkills.put(hash, 0);
					statement.setInt(1, player.getObjectId());
					statement.setInt(2, t.getSkillId());
					statement.setInt(3, t.getSkillLvl());
					statement.setInt(4, 0);
					statement.setInt(5, -1);
					statement.setInt(6, -1);
					statement.setInt(7, -1);
					statement.setLong(8, t.getReuse());
					statement.setDouble(9, t.getStamp());
					statement.setInt(10, 1);
					statement.setInt(11, player.getClassIndex());
					statement.setInt(12, ++buff_index);
					statement.execute();
				}
			}
			statement.close();
		}
		catch (final Exception e)
		{
			warn("Could not store char effect data: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public static CharacterSkillSaveDAO getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharacterSkillSaveDAO _instance = new CharacterSkillSaveDAO();
	}
}