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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.holder.CharSummonHolder;
import l2e.gameserver.data.holder.SummonEffectsHolder;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.tournaments.data.template.TournamentsEventsTemplate;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.l2skills.SkillSummon;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.network.serverpackets.SetSummonRemainTime;

public class ServitorInstance extends Summon
{
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_summon_skills_save (ownerId,ownerClassIndex,summonSkillId,skill_id,skill_level,effect_count,effect_cur_time,effect_total_time,buff_index) VALUES (?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time,effect_total_time,buff_index FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=?";

	private float _expPenalty = 0;
	private int _itemConsumeId;
	private int _itemConsumeCount;
	private int _itemConsumeSteps;
	private final int _totalLifeTime;
	private final int _timeLostIdle;
	private final int _timeLostActive;
	private int _timeRemaining;
	private int _nextItemConsumeTime;
	public int lastShowntimeRemaining;
	
	protected Future<?> _summonLifeTask;
	
	private int _referenceSkill;
	
	private boolean _shareElementals = false;
	private double _sharedElementalsPercent = 1;

	public ServitorInstance(int objectId, NpcTemplate template, Player owner, Skill skill)
	{
		super(objectId, template, owner);
		setInstanceType(InstanceType.ServitorInstance);
		setShowSummonAnimation(true);
		
		if (skill != null)
		{
			final SkillSummon summonSkill = (SkillSummon) skill;
			_itemConsumeId = summonSkill.getItemConsumeIdOT();
			_itemConsumeCount = summonSkill.getItemConsumeOT();
			_itemConsumeSteps = summonSkill.getItemConsumeSteps();
			_totalLifeTime = summonSkill.getTotalLifeTime();
			_timeLostIdle = summonSkill.getTimeLostIdle();
			_timeLostActive = summonSkill.getTimeLostActive();
			_referenceSkill = summonSkill.getId();
		}
		else
		{
			_itemConsumeId = 0;
			_itemConsumeCount = 0;
			_itemConsumeSteps = 0;
			_totalLifeTime = 1200000;
			_timeLostIdle = 1000;
			_timeLostActive = 1000;
		}
		_timeRemaining = _totalLifeTime;
		lastShowntimeRemaining = _totalLifeTime;
		
		if (_itemConsumeId == 0)
		{
			_nextItemConsumeTime = -1;
		}
		else if (_itemConsumeSteps == 0)
		{
			_nextItemConsumeTime = -1;
		}
		else
		{
			_nextItemConsumeTime = _totalLifeTime - (_totalLifeTime / (_itemConsumeSteps + 1));
		}
		
		final int delay = 1000;
		
		if (Config.DEBUG && (_itemConsumeCount != 0))
		{
			_log.warn(getClass().getSimpleName() + ": Item Consume ID: " + _itemConsumeId + ", Count: " + _itemConsumeCount + ", Rate: " + _itemConsumeSteps + " times.");
		}
		if (Config.DEBUG)
		{
			_log.warn(getClass().getSimpleName() + ": Task Delay " + (delay / 1000) + " seconds.");
		}
		
		_summonLifeTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new SummonLifetime(getOwner(), this), delay, delay);
	}
	
	@Override
	public final int getLevel()
	{
		return (getTemplate() != null ? getTemplate().getLevel() : 0);
	}
	
	@Override
	public int getSummonType()
	{
		return 1;
	}
	
	public void setExpPenalty(float expPenalty)
	{
		_expPenalty = expPenalty;
	}
	
	public float getExpPenalty()
	{
		return _expPenalty;
	}
	
	public void setSharedElementals(final boolean val)
	{
		_shareElementals = val;
	}
	
	public boolean isSharingElementals()
	{
		return _shareElementals;
	}
	
	public void setSharedElementalsValue(final double val)
	{
		_sharedElementalsPercent = val;
	}
	
	public double sharedElementalsPercent()
	{
		return _sharedElementalsPercent;
	}
	
	public int getItemConsumeCount()
	{
		return _itemConsumeCount;
	}
	
	public int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	public int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}
	
	public int getNextItemConsumeTime()
	{
		return _nextItemConsumeTime;
	}
	
	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}
	
	public int getTimeLostIdle()
	{
		return _timeLostIdle;
	}
	
	public int getTimeLostActive()
	{
		return _timeLostActive;
	}
	
	public int getTimeRemaining()
	{
		return _timeRemaining;
	}
	
	public void setNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime = value;
	}
	
	public void decNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime -= value;
	}
	
	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}
	
	public void addExpAndSp(int addToExp, int addToSp)
	{
		getOwner().addExpAndSp(addToExp, addToSp);
	}
	
	@Override
	protected void onDeath(Creature killer)
	{
		if (Config.DEBUG)
		{
			_log.warn(getClass().getSimpleName() + ": " + getTemplate().getName(null) + " (" + getOwner().getName(null) + ") has been killed.");
		}
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
			_summonLifeTask = null;
		}

		CharSummonHolder.getInstance().removeServitor(getOwner());

		super.onDeath(killer);
	}
	
	@Override
	public void setRestoreSummon(boolean val)
	{
		_restoreSummon = val;
	}
	
	@Override
	public final void stopSkillEffects(int skillId)
	{
		super.stopSkillEffects(skillId);
		SummonEffectsHolder.getInstance().removeServitorEffects(getOwner(), getReferenceSkill(), skillId);
	}
	
	@Override
	public void store()
	{
		super.store();
		if ((_referenceSkill == 0) || isDead())
		{
			return;
		}
		
		if (Config.RESTORE_SERVITOR_ON_RECONNECT)
		{
			CharSummonHolder.getInstance().saveSummon(this);
		}
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!Config.SUMMON_STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		if ((getOwner() == null) || getOwner().isInOlympiadMode() || getOwner().isInFightEvent())
		{
			return;
		}
		
		SummonEffectsHolder.getInstance().clearServitorEffects(getOwner(), getReferenceSkill());
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			con.setAutoCommit(false);
			statement.setInt(1, getOwner().getObjectId());
			statement.setInt(2, getOwner().getClassIndex());
			statement.setInt(3, getReferenceSkill());
			statement.execute();
			statement.close();
			int buff_index = 0;
			
			final List<Integer> storedSkills = new LinkedList<>();
			
			if (storeEffects)
			{
				statement = con.prepareStatement(ADD_SKILL_SAVE);
				for (final Effect effect : getAllEffects())
				{
					if (effect == null)
					{
						continue;
					}
					
					switch (effect.getEffectType())
					{
						case HEAL_OVER_TIME :
						case CPHEAL_OVER_TIME :
						case HIDE :
							continue;
					}
					
					if (effect.getAbnormalType().equalsIgnoreCase("LIFE_FORCE_OTHERS"))
					{
						continue;
					}
					
					final Skill skill = effect.getSkill();
					
					if (skill.isToggle() || skill.isReflectionBuff())
					{
						continue;
					}
					
					if (skill.isDance() && !Config.ALT_STORE_DANCES)
					{
						continue;
					}
					
					if (storedSkills.contains(skill.getReuseHashCode()))
					{
						continue;
					}
					
					storedSkills.add(skill.getReuseHashCode());
					
					statement.setInt(1, getOwner().getObjectId());
					statement.setInt(2, getOwner().getClassIndex());
					statement.setInt(3, getReferenceSkill());
					statement.setInt(4, skill.getId());
					statement.setInt(5, skill.getLevel());
					statement.setInt(6, effect.getTickCount());
					statement.setInt(7, effect.getTime());
					statement.setInt(8, effect.getAbnormalTime());
					statement.setInt(9, ++buff_index);
					statement.addBatch();
					
					SummonEffectsHolder.getInstance().addServitorEffect(getOwner(), getReferenceSkill(), skill, effect.getTickCount(), effect.getTime(), effect.getAbnormalTime());
				}
				statement.executeBatch();
			}
			con.commit();
		}
		catch (final Exception e)
		{
			_log.warn("Could not store summon effect data: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
	
	@Override
	public void restoreEffects()
	{
		if (getOwner().isInOlympiadMode())
		{
			return;
		}
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			if (!SummonEffectsHolder.getInstance().containsSkill(getOwner(), getReferenceSkill()))
			{
				statement = con.prepareStatement(RESTORE_SKILL_SAVE);
				statement.setInt(1, getOwner().getObjectId());
				statement.setInt(2, getOwner().getClassIndex());
				statement.setInt(3, getReferenceSkill());
				rset = statement.executeQuery();
				while (rset.next())
				{
					final int effectCount = rset.getInt("effect_count");
					final int effectCurTime = rset.getInt("effect_cur_time");
					final int effectTotalTime = rset.getInt("effect_total_time");
					
					final Skill skill = SkillsParser.getInstance().getInfo(rset.getInt("skill_id"), rset.getInt("skill_level"));
					if (skill == null || skill.isReflectionBuff())
					{
						continue;
					}
					
					if (skill.hasEffects())
					{
						SummonEffectsHolder.getInstance().addServitorEffect(getOwner(), getReferenceSkill(), skill, effectCount, effectCurTime, effectTotalTime);
					}
				}
				statement.close();
			}
			
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getOwner().getObjectId());
			statement.setInt(2, getOwner().getClassIndex());
			statement.setInt(3, getReferenceSkill());
			statement.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			SummonEffectsHolder.getInstance().applyServitorEffects(this, getOwner(), getReferenceSkill());
			DbUtils.closeQuietly(con, statement, rset);
		}
	}
	
	static class SummonLifetime implements Runnable
	{
		private final Player _activeChar;
		private final ServitorInstance _summon;
		
		SummonLifetime(Player activeChar, ServitorInstance newpet)
		{
			_activeChar = activeChar;
			_summon = newpet;
		}
		
		@Override
		public void run()
		{
			if (Config.DEBUG)
			{
				_log.warn(getClass().getSimpleName() + ": " + _summon.getTemplate().getName(null) + " (" + _activeChar.getName(null) + ") run task.");
			}
			
			try
			{
				final double oldTimeRemaining = _summon.getTimeRemaining();
				final int maxTime = _summon.getTotalLifeTime();
				double newTimeRemaining;
				
				if (_summon.isAttackingNow())
				{
					_summon.decTimeRemaining(_summon.getTimeLostActive());
				}
				else
				{
					_summon.decTimeRemaining(_summon.getTimeLostIdle());
				}
				newTimeRemaining = _summon.getTimeRemaining();
				
				if (newTimeRemaining < 0)
				{
					_summon.unSummon(_activeChar);
				}
				else if ((newTimeRemaining <= _summon.getNextItemConsumeTime()) && (oldTimeRemaining > _summon.getNextItemConsumeTime()))
				{
					_summon.decNextItemConsumeTime(maxTime / (_summon.getItemConsumeSteps() + 1));
					
					if ((_summon.getItemConsumeCount() > 0) && (_summon.getItemConsumeId() != 0) && !_summon.isDead() && !_summon.destroyItemByItemId("Consume", _summon.getItemConsumeId(), _summon.getItemConsumeCount(), _activeChar, true))
					{
						_summon.unSummon(_activeChar);
					}
				}
				
				if ((_summon.lastShowntimeRemaining - newTimeRemaining) > (maxTime / 352))
				{
					_summon.sendPacket(new SetSummonRemainTime(maxTime, (int) newTimeRemaining));
					_summon.lastShowntimeRemaining = (int) newTimeRemaining;
					_summon.updateEffectIcons();
				}
			}
			catch (final Exception e)
			{
				_log.error("Error on player [" + _activeChar.getName(null) + "] summon item consume task.", e);
			}
		}
	}
	
	@Override
	public void unSummon(Player owner)
	{
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": " + getTemplate().getName(null) + " (" + owner.getName(null) + ") unsummoned.");
		}
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
			_summonLifeTask = null;
		}
		
		super.unSummon(owner);

		if(!_restoreSummon)
			CharSummonHolder.getInstance().removeServitor(getOwner());
	}
	
	@Override
	public boolean destroyItem(String process, int objectId, long count, GameObject reference, boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}
	
	@Override
	public boolean destroyItemByItemId(String process, int itemId, long count, GameObject reference, boolean sendMessage)
	{
		if (Config.DEBUG)
		{
			_log.warn(getClass().getSimpleName() + ": " + getTemplate().getName(null) + " (" + getOwner().getName(null) + ") consume.");
		}
		
		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}
	
	public void setTimeRemaining(int time)
	{
		_timeRemaining = time;
	}
	
	public int getReferenceSkill()
	{
		return _referenceSkill;
	}
	
	@Override
	public byte getAttackElement()
	{
		if (isSharingElementals() && (getOwner() != null))
		{
			return getOwner().getAttackElement();
		}
		return super.getAttackElement();
	}
	
	@Override
	public int getAttackElementValue(byte attackAttribute)
	{
		if (isSharingElementals() && (getOwner() != null))
		{
			return (int) (getOwner().getAttackElementValue(attackAttribute) * sharedElementalsPercent());
		}
		return super.getAttackElementValue(attackAttribute);
	}
	
	@Override
	public int getDefenseElementValue(byte defenseAttribute)
	{
		if (isSharingElementals() && (getOwner() != null))
		{
			return (int) (getOwner().getDefenseElementValue(defenseAttribute) * sharedElementalsPercent());
		}
		return super.getDefenseElementValue(defenseAttribute);
	}
	
	@Override
	public boolean isServitor()
	{
		return true;
	}
	
	@Override
	public double getMAtk(Creature target, Skill skill)
	{
		return super.getMAtk(target, skill) + (getOwner().getMAtk(target, skill) * (getOwner().getServitorShareBonus(Stats.MAGIC_ATTACK) - 1.0));
	}
	
	@Override
	public double getMDef(Creature target, Skill skill)
	{
		return super.getMDef(target, skill) + (getOwner().getMDef(target, skill) * (getOwner().getServitorShareBonus(Stats.MAGIC_DEFENCE) - 1.0));
	}
	
	@Override
	public double getPAtk(Creature target)
	{
		return super.getPAtk(target) + (getOwner().getPAtk(target) * (getOwner().getServitorShareBonus(Stats.POWER_ATTACK) - 1.0));
	}
	
	@Override
	public double getPDef(Creature target)
	{
		return super.getPDef(target) + (getOwner().getPDef(target) * (getOwner().getServitorShareBonus(Stats.POWER_DEFENCE) - 1.0));
	}
	
	@Override
	public double getMAtkSpd()
	{
		return super.getMAtkSpd() + (getOwner().getMAtkSpd() * (getOwner().getServitorShareBonus(Stats.MAGIC_ATTACK_SPEED) - 1.0));
	}
	
	@Override
	public double getCriticalHit(Creature target, Skill skill)
	{
		return super.getCriticalHit(target, skill) + ((getOwner().getCriticalHit(target, skill)) * (getOwner().getServitorShareBonus(Stats.CRITICAL_RATE) - 1.0));
	}
	
	@Override
	public double getPAtkSpd()
	{
		return super.getPAtkSpd() + (getOwner().getPAtkSpd() * (getOwner().getServitorShareBonus(Stats.POWER_ATTACK_SPEED) - 1.0));
	}
	
	@Override
	public double getLevelMod()
	{
		return Config.ALLOW_SUMMON_LVL_MOD ? ((getLevel() + 89) / 100.0) : 1.0;
	}
}