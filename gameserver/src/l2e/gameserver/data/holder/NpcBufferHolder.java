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
package l2e.gameserver.data.holder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.holders.SkillHolder;

public class NpcBufferHolder extends LoggerObject
{
	private final Map<Integer, NpcBufferSkills> _buffers = new HashMap<>();
	
	public static class NpcBufferData
	{
		private final SkillHolder _skill;
		private final ItemHolder _fee;
		
		protected NpcBufferData(int skillId, int skillLevel, int feeId, int feeAmount)
		{
			_skill = new SkillHolder(skillId, skillLevel);
			_fee = new ItemHolder(feeId, feeAmount);
		}
		
		public SkillHolder getSkill()
		{
			return _skill;
		}
		
		public ItemHolder getFee()
		{
			return _fee;
		}
	}
	
	protected static class NpcBufferSkills
	{
		private final int _npcId;
		private final Map<Integer, NpcBufferData> _skills = new HashMap<>();
		
		protected NpcBufferSkills(int npcId)
		{
			_npcId = npcId;
		}
		
		public void addSkill(int skillId, int skillLevel, int skillFeeId, int skillFeeAmount, int buffGroup)
		{
			_skills.put(buffGroup, new NpcBufferData(skillId, skillLevel, skillFeeId, skillFeeAmount));
		}
		
		public NpcBufferData getSkillGroupInfo(int buffGroup)
		{
			return _skills.get(buffGroup);
		}

		public int getId()
		{
			return _npcId;
		}
	}
	
	protected NpcBufferHolder()
	{
		int skillCount = 0;
		Connection con = null;
		Statement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery("SELECT `npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group` FROM `npc_buffer` ORDER BY `npc_id` ASC");

			var lastNpcId = 0;
			NpcBufferSkills skills = null;
			
			while (rset.next())
			{
				final var npcId = rset.getInt("npc_id");
				final var skillId = rset.getInt("skill_id");
				final var skillLevel = rset.getInt("skill_level");
				final var skillFeeId = rset.getInt("skill_fee_id");
				final var skillFeeAmount = rset.getInt("skill_fee_amount");
				final var buffGroup = rset.getInt("buff_group");
				
				if (npcId != lastNpcId)
				{
					if (lastNpcId != 0)
					{
						_buffers.put(lastNpcId, skills);
					}
					
					skills = new NpcBufferSkills(npcId);
					skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
				}
				else if (skills != null)
				{
					skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
				}
				
				lastNpcId = npcId;
				skillCount++;
			}
			
			if (lastNpcId != 0)
			{
				_buffers.put(lastNpcId, skills);
			}
		}
		catch (final SQLException e)
		{
			warn("Error reading npc_buffer table: " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		info("Loaded " + _buffers.size() + " buffers and " + skillCount + " skills.");
	}
	
	public NpcBufferData getSkillInfo(int npcId, int buffGroup)
	{
		if (_buffers.containsKey(npcId))
		{
			final var skills = _buffers.get(npcId);
			if (skills != null)
			{
				return skills.getSkillGroupInfo(buffGroup);
			}
		}
		return null;
	}
	
	public static NpcBufferHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcBufferHolder _instance = new NpcBufferHolder();
	}
}