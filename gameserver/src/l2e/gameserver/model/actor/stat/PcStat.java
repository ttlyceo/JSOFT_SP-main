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
package l2e.gameserver.model.actor.stat;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ClassMasterInstance;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.actor.templates.PetLevelTemplate;
import l2e.gameserver.model.actor.transform.TransformTemplate;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.MoveType;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExVitalityPointInfo;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListUpdate;

public class PcStat extends PlayableStat
{
	private double _oldMaxHp;
	private double _oldMaxMp;
	private double _oldMaxCp;
	private double _vitalityPoints = 1;
	private byte _vitalityLevel = 0;
	private long _startingXp;
	
	public static final int VITALITY_LEVELS[] =
	{
	        240, 2000, 13000, 17000, 20000
	};
	public static final int MAX_VITALITY_POINTS = VITALITY_LEVELS[4];
	public static final int MIN_VITALITY_POINTS = 1;
	
	public PcStat(Player activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addExp(long value)
	{
		final Player activeChar = getActiveChar();
		if (!getActiveChar().getAccessLevel().canGainExp())
		{
			return false;
		}
		
		if (!super.addExp(value))
		{
			return false;
		}
		
		getActiveChar().getListeners().onExperienceReceived(value);
		
		if (!activeChar.isCursedWeaponEquipped() && (activeChar.getKarma() > 0) && (activeChar.isGM() || !activeChar.isInsideZone(ZoneId.PVP)))
		{
			final int karmaLost = activeChar.calculateKarmaLost(value);
			if (karmaLost > 0)
			{
				activeChar.setKarma(activeChar.getKarma() - karmaLost);
			}
		}
		activeChar.sendUserInfo();
		return true;
	}
	
	@Override
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		return addExpAndSp(addToExp, addToSp, false);
	}
	
	public boolean addExpAndSp(long addToExp, int addToSp, boolean useBonuses)
	{
		final Player activeChar = getActiveChar();
		
		if (!activeChar.getAccessLevel().canGainExp())
		{
			return false;
		}
		
		final long baseExp = addToExp;
		final int baseSp = addToSp;
		
		double bonusExp = 1.;
		double bonusSp = 1.;
		
		if (useBonuses)
		{
			bonusExp = getExpBonusMultiplier();
			bonusSp = getSpBonusMultiplier();
			if ((addToExp > 0) && !activeChar.isInsideZone(ZoneId.PEACE))
			{
				activeChar.getRecommendation().startRecBonus();
				activeChar.getNevitSystem().startAdventTask();
			}
		}
		
		addToExp *= bonusExp;
		addToSp *= bonusSp;
		
		float ratioTakenByPlayer = 0;
		
		if (activeChar.hasPet() && Util.checkIfInShortRadius(Config.ALT_PARTY_RANGE, activeChar, activeChar.getSummon(), false))
		{
			final PetInstance pet = (PetInstance) activeChar.getSummon();
			ratioTakenByPlayer = pet.getPetLevelData().getOwnerExpTaken() / 100f;
			if (!pet.getPetData().isSynchLevel())
			{
				if (ratioTakenByPlayer > 1)
				{
					ratioTakenByPlayer = 1;
				}
				
				if (!pet.isDead())
				{
					pet.addExpAndSp((long) (addToExp * (1 - ratioTakenByPlayer)), (int) (addToSp * (1 - ratioTakenByPlayer)));
				}
				
				addToExp = (long) (addToExp * ratioTakenByPlayer);
				addToSp = (int) (addToSp * ratioTakenByPlayer);
			}
		}
		
		addToExp = Math.max(0, addToExp);
		addToSp = Math.max(0, addToSp);
		
		if (!super.addExpAndSp(addToExp, addToSp))
		{
			return false;
		}
		
		SystemMessage sm = null;
		if ((addToExp == 0) && (addToSp != 0))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_SP);
			sm.addInt(addToSp);
		}
		else if ((addToSp == 0) && (addToExp != 0))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_EXPERIENCE);
			sm.addLong(addToExp);
		}
		else
		{
			if ((addToExp - baseExp) > 0 && (addToSp - baseSp) > 0)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_BONUS_S2_AND_S3_SP_BONUS_S4);
				sm.addLong(addToExp);
				sm.addLong(addToExp - baseExp);
				sm.addInt(addToSp);
				sm.addInt(addToSp - baseSp);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP);
				sm.addLong(addToExp);
				sm.addInt(addToSp);
			}
		}
		activeChar.sendPacket(sm);
		return true;
	}
	
	@Override
	public boolean removeExpAndSp(long addToExp, int addToSp)
	{
		return removeExpAndSp(addToExp, addToSp, true);
	}
	
	public boolean removeExpAndSp(long addToExp, int addToSp, boolean sendMessage)
	{
		final int level = getLevel();
		if (!super.removeExpAndSp(addToExp, addToSp))
		{
			return false;
		}
		
		if (sendMessage)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EXP_DECREASED_BY_S1);
			sm.addLong(addToExp);
			getActiveChar().sendPacket(sm);
			sm = SystemMessage.getSystemMessage(SystemMessageId.SP_DECREASED_S1);
			sm.addInt(addToSp);
			getActiveChar().sendPacket(sm);
			if (getLevel() < level)
			{
				getActiveChar().broadcastStatusUpdate();
			}
		}
		return true;
	}
	
	@Override
	public final boolean addLevel(int value, boolean canLower)
	{
		if (((getLevel() + value) > (ExperienceParser.getInstance().getMaxLevel() - 1)) || (!canLower && value < 0))
		{
			return false;
		}
		
		getActiveChar().getListeners().onLevelChange(getLevel(), (value + getLevel()));
		
		final boolean levelIncreased = super.addLevel(value, canLower);
		if (levelIncreased)
		{
			if (!Config.DISABLE_TUTORIAL)
			{
				final QuestState qs = getActiveChar().getQuestState("_255_Tutorial");
				if (qs != null)
				{
					qs.getQuest().notifyEvent("CE40", null, getActiveChar());
				}
			}
			
			getActiveChar().setCurrentCp(getMaxCp());
			getActiveChar().broadcastPacket(new SocialAction(getActiveChar().getObjectId(), SocialAction.LEVEL_UP));
			getActiveChar().sendPacket(SystemMessageId.YOU_INCREASED_YOUR_LEVEL);
			ClassMasterInstance.showQuestionMark(getActiveChar());
		}
		
		getActiveChar().rewardSkills();
		
		if (getActiveChar().getClan() != null)
		{
			getActiveChar().getClan().updateClanMember(getActiveChar());
			getActiveChar().getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(getActiveChar()));
		}
		if (getActiveChar().isInParty())
		{
			getActiveChar().getParty().recalculatePartyLevel();
		}

		if (getActiveChar().getMatchingRoom() != null)
		{
			getActiveChar().getMatchingRoom().broadcastPlayerUpdate(getActiveChar());
		}
		
		if (getActiveChar().isTransformed() || getActiveChar().isInStance())
		{
			getActiveChar().getTransformation().onLevelUp(getActiveChar());
		}
		
		if (getActiveChar().hasPet())
		{
			final PetInstance pet = (PetInstance) getActiveChar().getSummon();
			if (pet.getPetData().isSynchLevel() && (pet.getLevel() != getLevel()))
			{
				final long oldexp = pet.getStat().getExp();
				final long newexp = pet.getStat().getExpForLevel(getActiveChar().getLevel());
				pet.getStat().setLevel(getLevel());
				if (oldexp > newexp)
				{
					pet.getStat().removeExp(oldexp - newexp);
				}
				else if (oldexp < newexp)
				{
					pet.getStat().addExp(newexp - oldexp);
				}
				pet.setCurrentHp(pet.getMaxHp());
				pet.setCurrentMp(pet.getMaxMp());
				pet.broadcastPacket(new SocialAction(getActiveChar().getObjectId(), SocialAction.LEVEL_UP));
				pet.updateAndBroadcastStatus(1);
			}
		}
		
		final var su = getActiveChar().makeStatusUpdate(StatusUpdate.LEVEL, StatusUpdate.MAX_CP, StatusUpdate.MAX_HP, StatusUpdate.MAX_MP);
		getActiveChar().sendPacket(su);
		
		getActiveChar().refreshOverloaded();
		getActiveChar().refreshExpertisePenalty();
		
		getActiveChar().sendUserInfo();
		getActiveChar().sendVoteSystemInfo();
		getActiveChar().getNevitSystem().addPoints(levelIncreased ? 1950 : -1950);
		
		return levelIncreased;
	}
	
	@Override
	public boolean addSp(int value)
	{
		if (!super.addSp(value))
		{
			return false;
		}
		getActiveChar().sendStatusUpdate(false, false, StatusUpdate.SP);
		return true;
	}
	
	@Override
	public final long getExpForLevel(int level)
	{
		return ExperienceParser.getInstance().getExpForLevel(level);
	}
	
	@Override
	public final Player getActiveChar()
	{
		return (Player) super.getActiveChar();
	}
	
	@Override
	public final long getExp()
	{
		if (getActiveChar().isSubClassActive())
		{
			final var sub = getActiveChar().getSubClasses().get(getActiveChar().getClassIndex());
			if (sub != null)
			{
				return sub.getExp();
			}
		}
		return super.getExp();
	}
	
	public final long getBaseExp()
	{
		return super.getExp();
	}
	
	@Override
	public final void setExp(long value)
	{
		if (getActiveChar().isSubClassActive())
		{
			final var sub = getActiveChar().getSubClasses().get(getActiveChar().getClassIndex());
			if (sub != null)
			{
				sub.setExp(value);
			}
		}
		else
		{
			super.setExp(value);
		}
	}
	
	public void setStartingExp(long value)
	{
		if (Config.BOTREPORT_ENABLE)
		{
			_startingXp = value;
		}
	}
	
	public long getStartingExp()
	{
		return _startingXp;
	}
	
	@Override
	public final int getLevel()
	{
		if (getActiveChar().isSubClassActive())
		{
			final var sub = getActiveChar().getSubClasses().get(getActiveChar().getClassIndex());
			if (sub != null)
			{
				return sub.getLevel();
			}
		}
		return super.getLevel();
	}
	
	public final int getBaseLevel()
	{
		return super.getLevel();
	}
	
	@Override
	public final void setLevel(int value)
	{
		if (value > (ExperienceParser.getInstance().getMaxLevel() - 1))
		{
			value = (ExperienceParser.getInstance().getMaxLevel() - 1);
		}
		
		if (getActiveChar().isSubClassActive())
		{
			final var sub = getActiveChar().getSubClasses().get(getActiveChar().getClassIndex());
			if (sub != null)
			{
				sub.setLevel(value);
			}
		}
		else
		{
			super.setLevel(value);
		}
	}
	
	@Override
	public final double getMaxCp()
	{
		double val = (getActiveChar() == null) ? 1 : calcStat(Stats.MAX_CP, getActiveChar().getTemplate().getBaseCpMax(getActiveChar().getLevel()));
		if (Config.ALLOW_ZONES_LIMITS)
		{
			if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.CP_LIMIT))
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.CP_LIMIT);
				if (zone != null && zone.getCpLimit() > 0)
				{
					val = zone.getCpLimit();
				}
			}
		}
		
		if (val != _oldMaxCp)
		{
			_oldMaxCp = val;
			
			if (getActiveChar().getStatus().getCurrentCp() != val)
			{
				getActiveChar().getStatus().setCurrentCp(getActiveChar().getStatus().getCurrentCp());
			}
		}
		return val;
	}
	
	@Override
	public final double getMaxHp()
	{
		double val = (getActiveChar() == null) ? 1 : (int) calcStat(Stats.MAX_HP, getActiveChar().getTemplate().getBaseHpMax(getActiveChar().getLevel()));
		if (Config.ALLOW_ZONES_LIMITS)
		{
			if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.HP_LIMIT))
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.HP_LIMIT);
				if (zone != null && zone.getHpLimit() > 0)
				{
					val = zone.getHpLimit();
				}
			}
		}
		
		if (val != _oldMaxHp)
		{
			_oldMaxHp = val;
			
			if (getActiveChar().getStatus().getCurrentHp() != val)
			{
				getActiveChar().getStatus().setCurrentHp(getActiveChar().getStatus().getCurrentHp());
			}
		}
		return val;
	}
	
	@Override
	public final double getMaxMp()
	{
		double val = (getActiveChar() == null) ? 1 : (int) calcStat(Stats.MAX_MP, getActiveChar().getTemplate().getBaseMpMax(getActiveChar().getLevel()));
		if (Config.ALLOW_ZONES_LIMITS)
		{
			if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.MP_LIMIT))
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.MP_LIMIT);
				if (zone != null && zone.getMpLimit() > 0)
				{
					val = zone.getMpLimit();
				}
			}
		}
		
		if (val != _oldMaxMp)
		{
			_oldMaxMp = val;
			
			if (getActiveChar().getStatus().getCurrentMp() != val)
			{
				getActiveChar().getStatus().setCurrentMp(getActiveChar().getStatus().getCurrentMp());
			}
		}
		return val;
	}
	
	@Override
	public final int getSp()
	{
		if (getActiveChar().isSubClassActive())
		{
			final var sub = getActiveChar().getSubClasses().get(getActiveChar().getClassIndex());
			if (sub != null)
			{
				return sub.getSp();
			}
		}
		return super.getSp();
	}
	
	public final int getBaseSp()
	{
		return super.getSp();
	}
	
	@Override
	public final void setSp(int value)
	{
		if (getActiveChar().isSubClassActive())
		{
			final var sub = getActiveChar().getSubClasses().get(getActiveChar().getClassIndex());
			if (sub != null)
			{
				sub.setSp(value);
			}
		}
		else
		{
			super.setSp(value);
		}
	}
	
	@Override
	public double getBaseMoveSpeed(MoveType type)
	{
		final Player player = getActiveChar();
		if (player.isTransformed())
		{
			final TransformTemplate template = player.getTransformation().getTemplate(player);
			if (template != null)
			{
				return template.getBaseMoveSpeed(type);
			}
		}
		else if (player.isMounted())
		{
			final PetLevelTemplate data = PetsParser.getInstance().getPetLevelData(player.getMountNpcId(), player.getMountLevel());
			if (data != null)
			{
				return data.getSpeedOnRide(type);
			}
		}
		return super.getBaseMoveSpeed(type);
	}
	
	@Override
	public double getRealMoveSpeed(boolean isStillWalking)
	{
		return (getActiveChar().isInWater()) ? getActiveChar().isRunning() ? getSwimRunSpeed() : getSwimWalkSpeed() : ((isStillWalking || !getActiveChar().isRunning()) ? getWalkSpeed() : getRunSpeed());
	}
	
	@Override
	public double getRunSpeed()
	{
		double val = super.getRunSpeed() + Config.RUN_SPD_BOOST;
		
		if (Config.ALLOW_ZONES_LIMITS)
		{
			if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.RUN_SPEED_LIMIT))
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.RUN_SPEED_LIMIT);
				if (zone != null && zone.getRunSpeedLimit() > 0)
				{
					val = zone.getRunSpeedLimit();
				}
			}
		}
		
		if (getActiveChar().isMounted() && ((getActiveChar().getMountLevel() - getActiveChar().getLevel()) >= 10 || getActiveChar().isHungry()))
		{
			val /= 2;
		}
		
		if (Config.SPEED_UP_RUN && getActiveChar().isInsideZone(ZoneId.PEACE))
		{
			val *= 2;
		}
		return val;
	}
	
	@Override
	public double getSwimRunSpeed()
	{
		double val = super.getSwimRunSpeed() + Config.RUN_SPD_BOOST;
		
		if (getActiveChar().isMounted() && ((getActiveChar().getMountLevel() - getActiveChar().getLevel()) >= 10 || getActiveChar().isHungry()))
		{
			val /= 2;
		}
		
		if (Config.SPEED_UP_RUN && getActiveChar().isInsideZone(ZoneId.PEACE))
		{
			val *= 2;
		}
		return val;
	}
	
	@Override
	public double getWalkSpeed()
	{
		double val = super.getWalkSpeed() + Config.RUN_SPD_BOOST;
		
		if (Config.ALLOW_ZONES_LIMITS)
		{
			if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.WALK_SPEED_LIMIT))
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.WALK_SPEED_LIMIT);
				if (zone != null && zone.getWalkSpeedLimit() > 0)
				{
					val = zone.getWalkSpeedLimit();
				}
			}
		}
		
		if (getActiveChar().isMounted() && ((getActiveChar().getMountLevel() - getActiveChar().getLevel()) >= 10 || getActiveChar().isHungry()))
		{
			val /= 2;
		}
		
		if (Config.SPEED_UP_RUN && getActiveChar().isInsideZone(ZoneId.PEACE))
		{
			val *= 2;
		}
		return val;
	}
	
	@Override
	public double getSwimWalkSpeed()
	{
		double val = super.getSwimWalkSpeed() + Config.RUN_SPD_BOOST;
		
		if (Config.ALLOW_ZONES_LIMITS)
		{
			if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.WALK_SPEED_LIMIT))
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.WALK_SPEED_LIMIT);
				if (zone != null && zone.getWalkSpeedLimit() > 0)
				{
					val = zone.getWalkSpeedLimit();
				}
			}
		}
		
		if (getActiveChar().isMounted() && ((getActiveChar().getMountLevel() - getActiveChar().getLevel()) >= 10 || getActiveChar().isHungry()))
		{
			val /= 2;
		}
		
		if (Config.SPEED_UP_RUN && getActiveChar().isInsideZone(ZoneId.PEACE))
		{
			val *= 2;
		}
		return val;
	}
	
	@Override
	public double getPAtk(Creature target)
	{
		double val = super.getPAtk(target);
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.P_ATK_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.P_ATK_LIMIT);
			if (zone != null && zone.getPAtkLimit() > 0)
			{
				val = zone.getPAtkLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getPAtkSpd()
	{
		double val = super.getPAtkSpd();
		if (Config.ALLOW_ZONES_LIMITS)
		{
			if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.ATK_SPEED_LIMIT))
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.ATK_SPEED_LIMIT);
				if (zone != null && zone.getAtkSpeedLimit() > 0)
				{
					val = zone.getAtkSpeedLimit();
				}
			}
		}
		return val;
	}
	
	@Override
	public double getPDef(Creature target)
	{
		double val = super.getPDef(target);
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.P_DEF_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.P_DEF_LIMIT);
			if (zone != null && zone.getPDefLimit() > 0)
			{
				val = zone.getPDefLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getCriticalDmg(Creature target, double init, Skill skill)
	{
		double val = super.getCriticalDmg(target, init, skill);
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.CRIT_DMG_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.CRIT_DMG_LIMIT);
			if (zone != null && zone.getCritDmgLimit() > 0)
			{
				val = zone.getCritDmgLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getMAtk(Creature target, Skill skill)
	{
		double val = super.getMAtk(target, skill);
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.M_ATK_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.M_ATK_LIMIT);
			if (zone != null && zone.getMAtkLimit() > 0)
			{
				val = zone.getMAtkLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getMAtkSpd()
	{
		double val = super.getMAtkSpd();
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.M_ATK_SPEED_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.M_ATK_SPEED_LIMIT);
			if (zone != null && zone.getMAtkSpeedLimit() > 0)
			{
				val = zone.getMAtkSpeedLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getMDef(Creature target, Skill skill)
	{
		double val = super.getMDef(target, skill);
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.M_DEF_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.M_DEF_LIMIT);
			if (zone != null && zone.getMDefLimit() > 0)
			{
				val = zone.getMDefLimit();
			}
		}
		return val;
	}
	
	@Override
	public int getAccuracy()
	{
		int val = super.getAccuracy();
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.ACCURACY_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.ACCURACY_LIMIT);
			if (zone != null && zone.getAccuracyLimit() > 0)
			{
				val = zone.getAccuracyLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getCriticalHit(Creature target, Skill skill)
	{
		double val = super.getCriticalHit(target, skill);
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.CRIT_HIT_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.CRIT_HIT_LIMIT);
			if (zone != null && zone.getCritHitLimit() > 0)
			{
				val = zone.getCritHitLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getMCriticalHit(Creature target, Skill skill)
	{
		double val = super.getMCriticalHit(target, skill);
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.MCRIT_HIT_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.MCRIT_HIT_LIMIT);
			if (zone != null && zone.getMCritHitLimit() > 0)
			{
				val = zone.getMCritHitLimit();
			}
		}
		return val;
	}
	
	@Override
	public int getEvasionRate(Creature target)
	{
		int val = super.getEvasionRate(target);
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.EVASION_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.EVASION_LIMIT);
			if (zone != null && zone.getEvasionLimit() > 0)
			{
				val = zone.getEvasionLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getPvpPhysSkillDmg()
	{
		double val = super.getPvpPhysSkillDmg();
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.PVP_PHYS_SKILL_DMG_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.PVP_PHYS_SKILL_DMG_LIMIT);
			if (zone != null && zone.getPvpPhysSkillDmgLimit() > 0)
			{
				val = zone.getPvpPhysSkillDmgLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getPvpPhysSkillDef()
	{
		double val = super.getPvpPhysSkillDef();
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.PVP_PHYS_SKILL_DEF_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.PVP_PHYS_SKILL_DEF_LIMIT);
			if (zone != null && zone.getPvpPhysSkillDefLimit() > 0)
			{
				val = zone.getPvpPhysSkillDefLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getPvpPhysDef()
	{
		double val = super.getPvpPhysDef();
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.PVP_PHYS_DEF_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.PVP_PHYS_DEF_LIMIT);
			if (zone != null && zone.getPvpPhysDefLimit() > 0)
			{
				val = zone.getPvpPhysDefLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getPvpPhysDmg()
	{
		double val = super.getPvpPhysDmg();
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.PVP_PHYS_DMG_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.PVP_PHYS_DMG_LIMIT);
			if (zone != null && zone.getPvpPhysDmgLimit() > 0)
			{
				val = zone.getPvpPhysDmgLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getPvpMagicDmg()
	{
		double val = super.getPvpMagicDmg();
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.PVP_MAGIC_DMG_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.PVP_MAGIC_DMG_LIMIT);
			if (zone != null && zone.getPvpMagicDmgLimit() > 0)
			{
				val = zone.getPvpMagicDmgLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getPvpMagicDef()
	{
		double val = super.getPvpMagicDef();
		if (!Config.ALLOW_ZONES_LIMITS)
		{
			return val;
		}
		
		if (getActiveChar() != null && getActiveChar().isInsideZone(ZoneId.PVP_MAGIC_DEF_LIMIT))
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByZoneId(getActiveChar(), ZoneId.PVP_MAGIC_DEF_LIMIT);
			if (zone != null && zone.getPvpMagicDefLimit() > 0)
			{
				val = zone.getPvpMagicDefLimit();
			}
		}
		return val;
	}
	
	@Override
	public double getMovementSpeedMultiplier()
	{
		if (getActiveChar().isMounted())
		{
			final PetLevelTemplate data = PetsParser.getInstance().getPetLevelData(getActiveChar().getMountNpcId(), getActiveChar().getMountLevel());
			final double baseSpeed = data != null ? data.getSpeedOnRide(MoveType.RUN) : NpcsParser.getInstance().getTemplate(getActiveChar().getMountNpcId()).getBaseMoveSpeed(MoveType.RUN);
			return (getRunSpeed() / baseSpeed);
		}
		return super.getMovementSpeedMultiplier();
	}
	
	private void updateVitalityLevel(boolean quiet)
	{
		final byte level;
		
		if (_vitalityPoints <= VITALITY_LEVELS[0])
		{
			level = 0;
		}
		else if (_vitalityPoints <= VITALITY_LEVELS[1])
		{
			level = 1;
		}
		else if (_vitalityPoints <= VITALITY_LEVELS[2])
		{
			level = 2;
		}
		else if (_vitalityPoints <= VITALITY_LEVELS[3])
		{
			level = 3;
		}
		else
		{
			level = 4;
		}
		
		if (_vitalityLevel > level)
		{
			getActiveChar().getNevitSystem().addPoints(1500);
		}
		
		if (!quiet && (level != _vitalityLevel))
		{
			if (level < _vitalityLevel)
			{
				getActiveChar().sendPacket(SystemMessageId.VITALITY_HAS_DECREASED);
			}
			else
			{
				getActiveChar().sendPacket(SystemMessageId.VITALITY_HAS_INCREASED);
			}
			if (level == 0)
			{
				getActiveChar().sendPacket(SystemMessageId.VITALITY_IS_EXHAUSTED);
			}
			else if (level == 4)
			{
				getActiveChar().sendPacket(SystemMessageId.VITALITY_IS_AT_MAXIMUM);
			}
		}
		
		_vitalityLevel = level;
	}
	
	public int getVitalityPoints()
	{
		return (int) _vitalityPoints;
	}
	
	public void setVitalityPoints(int points, boolean quiet)
	{
		if (!Config.ENABLE_VITALITY)
		{
			return;
		}
		
		points = Math.min(Math.max(points, MIN_VITALITY_POINTS), MAX_VITALITY_POINTS);
		if (points == _vitalityPoints)
		{
			return;
		}
		
		_vitalityPoints = points;
		updateVitalityLevel(quiet);
		getActiveChar().sendPacket(new ExVitalityPointInfo(getVitalityPoints()));
	}
	
	public synchronized void updateVitalityPoints(double vitalityPoints, boolean useRates, boolean quiet)
	{
		if ((vitalityPoints == 0) || !Config.ENABLE_VITALITY || getVitalityAbundance() > 0)
		{
			return;
		}
		
		if (useRates)
		{
			if (getActiveChar().isLucky())
			{
				return;
			}
			
			if (vitalityPoints < 0)
			{
				int stat = (int) calcStat(Stats.VITALITY_CONSUME_RATE, 1, getActiveChar(), null);
				
				if (getActiveChar().getNevitSystem().isBlessingActive())
				{
					stat -= Config.VITALITY_NEVIT_POINT;
				}
				
				if (stat == 0)
				{
					return;
				}
				if (stat < 0)
				{
					vitalityPoints = -vitalityPoints;
				}
			}
			
			if (vitalityPoints > 0)
			{
				vitalityPoints *= Config.RATE_VITALITY_GAIN;
			}
			else
			{
				vitalityPoints *= Config.RATE_VITALITY_LOST;
			}
		}
		
		if (vitalityPoints > 0)
		{
			vitalityPoints = Math.min(_vitalityPoints + vitalityPoints, MAX_VITALITY_POINTS);
		}
		else
		{
			vitalityPoints = Math.max(_vitalityPoints + vitalityPoints, MIN_VITALITY_POINTS);
		}
		
		if (Math.abs(vitalityPoints - _vitalityPoints) <= 1e-6)
		{
			return;
		}
		
		_vitalityPoints = vitalityPoints;
		updateVitalityLevel(quiet);
	}
	
	public double getVitalityMultiplier()
	{
		double vitality = 1.0;
		if (Config.ENABLE_VITALITY)
		{
			switch (getVitalityLevel())
			{
				case 1 :
					vitality = Config.RATE_VITALITY_LEVEL_1;
					break;
				case 2 :
					vitality = Config.RATE_VITALITY_LEVEL_2;
					break;
				case 3 :
					vitality = Config.RATE_VITALITY_LEVEL_3;
					break;
				case 4 :
					vitality = Config.RATE_VITALITY_LEVEL_4;
					break;
			}
		}
		return vitality;
	}
	
	public byte getVitalityLevel()
	{
		return (byte) (getActiveChar().getNevitSystem().isBlessingActive() ? 4 : getVitalityAbundance() > 0 ? getVitalityAbundance() : _vitalityLevel);
	}
	
	public int getVitalityLevel(boolean blessActive)
	{
		return Config.ENABLE_VITALITY ? (blessActive ? 4 : _vitalityLevel) : 0;
	}
	
	public double getExpBonusMultiplier()
	{
		final double vitality = getVitalityMultiplier();
		final double nevits = getActiveChar().getRecommendation().getRecoMultiplier();
		final double bonusExp = calcStat(Stats.BONUS_EXP, 1);
		
		double bonus = 0.0;
		bonus += Math.max(vitality, 0);
		bonus += Math.max(nevits, 0);
		bonus = Math.max(bonus, 1);
		bonus *= bonusExp;
		bonus = Math.min(bonus, Config.MAX_BONUS_EXP);
		
		return bonus;
	}
	
	public double getSpBonusMultiplier()
	{
		final double vitality = getVitalityMultiplier();
		final double nevits = getActiveChar().getRecommendation().getRecoMultiplier();
		final double bonusSp = calcStat(Stats.BONUS_SP, 1);
		
		double bonus = 0.0;
		bonus += Math.max(vitality, 0);
		bonus += Math.max(nevits, 0);
		bonus = Math.max(bonus, 1);
		bonus *= bonusSp;
		bonus = Math.min(bonus, Config.MAX_BONUS_SP);
		
		return bonus;
	}
	
	private int getVitalityAbundance()
	{
		return (int) calcStat(Stats.VITALITY_ABUNDANCE, 0);
	}
}