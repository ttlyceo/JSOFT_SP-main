/*
 *
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.instance.ServitorInstance;
import l2e.gameserver.model.actor.instance.SiegeSummonInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.l2skills.SkillSummon;
import l2e.gameserver.network.serverpackets.PetItemList;

public class CharSummonHolder extends LoggerObject
{
	private static final Map<Integer, Integer> _pets = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> _servitors = new ConcurrentHashMap<>();

	private static final String INIT_PET = "SELECT ownerId, item_obj_id FROM pets WHERE restore = '1'";
	private static final String INIT_SUMMONS = "SELECT ownerId, summonSkillId FROM character_summons";
	private static final String LOAD_SUMMON = "SELECT curHp, curMp, time FROM character_summons WHERE ownerId = ? AND summonSkillId = ?";
	private static final String REMOVE_SUMMON = "DELETE FROM character_summons WHERE ownerId = ?";
	private static final String SAVE_SUMMON = "REPLACE INTO character_summons (ownerId,summonSkillId,curHp,curMp,time) VALUES (?,?,?,?,?)";
	
	public Map<Integer, Integer> getPets()
	{
		return _pets;
	}
	
	public Map<Integer, Integer> getServitors()
	{
		return _servitors;
	}
	
	public void init()
	{
		if (Config.RESTORE_SERVITOR_ON_RECONNECT)
		{
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(INIT_SUMMONS);
				rset = statement.executeQuery();
				while (rset.next())
				{
					_servitors.put(rset.getInt("ownerId"), rset.getInt("summonSkillId"));
				}
			}
			catch (final Exception e)
			{
				warn("Error while loading saved summons");
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
		
		if (Config.RESTORE_PET_ON_RECONNECT)
		{
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(INIT_PET);
				rset = statement.executeQuery();
				while (rset.next())
				{
					_pets.put(rset.getInt("ownerId"), rset.getInt("item_obj_id"));
				}
			}
			catch (final Exception e)
			{
				warn("Error while loading saved summons");
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
	}
	
	public void removeServitor(Player activeChar)
	{
		_servitors.remove(activeChar.getObjectId());
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(REMOVE_SUMMON);
			statement.setInt(1, activeChar.getObjectId());
			statement.execute();
		}
		catch (final SQLException e)
		{
			warn("Summon cannot be removed: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public void restorePet(Player activeChar)
	{
		final var item = activeChar.getInventory().getItemByObjectId(_pets.get(activeChar.getObjectId()));
		if (item == null)
		{
			warn("Null pet summoning item for: " + activeChar);
			return;
		}
		final var petData = PetsParser.getInstance().getPetDataByItemId(item.getId());
		if (petData == null)
		{
			warn("Null pet data for: " + activeChar + " and summoning item: " + item);
			return;
		}
		final var npcTemplate = NpcsParser.getInstance().getTemplate(petData.getNpcId());
		if (npcTemplate == null)
		{
			warn("Null pet NPC template for: " + activeChar + " and pet Id:" + petData.getNpcId());
			return;
		}
		
		final var pet = PetInstance.spawnPet(npcTemplate, activeChar, item);
		if (pet == null)
		{
			warn("Null pet instance for: " + activeChar + " and pet NPC template:" + npcTemplate);
			return;
		}
		pet.setShowSummonAnimation(true);
		pet.setGlobalTitle(activeChar.getName(null));
		
		if (!pet.isRespawned())
		{
			pet.setCurrentHp(pet.getMaxHp());
			pet.setCurrentMp(pet.getMaxMp());
			pet.getStat().setExp(pet.getExpForThisLevel());
			pet.setCurrentFed(pet.getMaxFed());
		}
		
		pet.setRunning();
		
		if (!pet.isRespawned())
		{
			pet.store();
		}
		
		activeChar.setPet(pet);
		
		pet.spawnMe();
		pet.startFeed();
		
		if (pet.getCurrentFed() <= 0)
		{
			pet.unSummon(activeChar);
		}
		else
		{
			pet.startFeed();
		}
		
		pet.setFollowStatus(true);
		
		pet.getOwner().sendPacket(new PetItemList(pet.getInventory().getItems()));
		pet.broadcastStatusUpdate();
	}
	
	public void restoreServitor(Player activeChar)
	{
		final var skillId = _servitors.get(activeChar.getObjectId());
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(LOAD_SUMMON);
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, skillId);
			
			NpcTemplate summonTemplate;
			ServitorInstance summon;
			SkillSummon skill;
			
			rset = statement.executeQuery();
			while (rset.next())
			{
				final var curHp = rset.getInt("curHp");
				final var curMp = rset.getInt("curMp");
				final var time = rset.getInt("time");
				
				skill = (SkillSummon) SkillsParser.getInstance().getInfo(skillId, activeChar.getSkillLevel(skillId));
				if (skill == null)
				{
					removeServitor(activeChar);
					return;
				}
				
				summonTemplate = NpcsParser.getInstance().getTemplate(skill.getNpcId());
				if (summonTemplate == null)
				{
					warn("Summon attemp for nonexisting Skill ID:" + skillId);
					return;
				}
				
				final var id = IdFactory.getInstance().getNextId();
				if (summonTemplate.isType("SiegeSummon"))
				{
					summon = new SiegeSummonInstance(id, summonTemplate, activeChar, skill);
				}
				else if (summonTemplate.isType("MerchantSummon"))
				{
					summon = new ServitorInstance(id, summonTemplate, activeChar, skill);
				}
				else
				{
					summon = new ServitorInstance(id, summonTemplate, activeChar, skill);
				}
				
				for (final var lang : Config.MULTILANG_ALLOWED)
				{
					if (lang != null)
					{
						summon.setName(lang, summonTemplate.getName(lang) != null ? summonTemplate.getName(lang) : summonTemplate.getName(null));
					}
				}
				summon.setGlobalTitle(activeChar.getName(null));
				summon.setExpPenalty(skill.getExpPenalty());
				summon.setSharedElementals(skill.getInheritElementals());
				summon.setSharedElementalsValue(skill.getElementalSharePercent());
				summon.setPhysAttributteMod(summonTemplate.getPhysAttributteMod());
				summon.setMagicAttributteMod(summonTemplate.getMagicAttributteMod());
				
				if (summon.getLevel() >= ExperienceParser.getInstance().getMaxPetLevel())
				{
					summon.getStat().setExp(ExperienceParser.getInstance().getExpForLevel(ExperienceParser.getInstance().getMaxPetLevel() - 1));
					warn("Summon (" + summon.getName(null) + ") NpcID: " + summon.getId() + " has a level above " + ExperienceParser.getInstance().getMaxPetLevel() + ". Please rectify.");
				}
				else
				{
					summon.getStat().setExp(ExperienceParser.getInstance().getExpForLevel(summon.getLevel() % ExperienceParser.getInstance().getMaxPetLevel()));
				}
				summon.setCurrentHp(curHp);
				summon.setCurrentMp(curMp);
				summon.setHeading(activeChar.getHeading());
				summon.setRunning();
				activeChar.setPet(summon);
				summon.setTimeRemaining(time);
				summon.spawnMe(activeChar.getX() + 20, activeChar.getY() + 20, activeChar.getZ());
			}
		}
		catch (final SQLException e)
		{
			_log.warn("Servitor cannot be restored: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	public void saveSummon(ServitorInstance summon)
	{
		if ((summon == null) || (summon.getTimeRemaining() <= 0))
		{
			return;
		}
		
		_servitors.put(summon.getOwner().getObjectId(), summon.getReferenceSkill());
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SAVE_SUMMON);
			statement.setInt(1, summon.getOwner().getObjectId());
			statement.setInt(2, summon.getReferenceSkill());
			statement.setInt(3, (int) Math.round(summon.getCurrentHp()));
			statement.setInt(4, (int) Math.round(summon.getCurrentMp()));
			statement.setInt(5, summon.getTimeRemaining());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Failed to store summon: " + summon + " from " + summon.getOwner() + ", error: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	public static CharSummonHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharSummonHolder _instance = new CharSummonHolder();
	}
}