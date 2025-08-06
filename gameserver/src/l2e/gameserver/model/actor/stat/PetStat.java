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

import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class PetStat extends SummonStat
{
	private double _oldMaxHp;
	private double _oldMaxMp;
	
	public PetStat(PetInstance activeChar)
	{
		super(activeChar);
	}
	
	public boolean addExp(int value)
	{
		if (getActiveChar().isUncontrollable() || !super.addExp(value))
		{
			return false;
		}

		getActiveChar().updateAndBroadcastStatus(1);
		getActiveChar().updateEffectIcons(true);
		
		return true;
	}
	
	@Override
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		if (getActiveChar().isUncontrollable() || !addExp(addToExp))
		{
			return false;
		}
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PET_EARNED_S1_EXP);
		sm.addLong(addToExp);
		getActiveChar().updateAndBroadcastStatus(1);
		getActiveChar().sendPacket(sm);
		
		return true;
	}
	
	@Override
	public final boolean addLevel(int value, boolean canLower)
	{
		if (((getLevel() + value) > (getMaxLevel() - 1)) || (!canLower && value < 0))
		{
			return false;
		}
		
		final boolean levelIncreased = super.addLevel(value, canLower);
		
		final var su = getActiveChar().makeStatusUpdate(StatusUpdate.LEVEL, StatusUpdate.MAX_HP, StatusUpdate.MAX_MP);
		getActiveChar().broadcastPacket(su);
		if (levelIncreased)
		{
			getActiveChar().broadcastPacket(new SocialAction(getActiveChar().getObjectId(), SocialAction.LEVEL_UP));
		}
		getActiveChar().updateAndBroadcastStatus(1);
		
		if (getActiveChar().getControlItem() != null)
		{
			getActiveChar().getControlItem().setEnchantLevel(getLevel());
		}
		
		return levelIncreased;
	}
	
	@Override
	public final long getExpForLevel(int level)
	{
		try
		{
			return PetsParser.getInstance().getPetLevelData(getActiveChar().getId(), level).getPetMaxExp();
		}
		catch (final NullPointerException e)
		{
			if (getActiveChar() != null)
			{
				_log.warn("Pet objectId:" + getActiveChar().getObjectId() + ", NpcId:" + getActiveChar().getId() + ", level:" + level + " is missing data from pets_stats table!");
			}
			throw e;
		}
	}
	
	@Override
	public PetInstance getActiveChar()
	{
		return (PetInstance) super.getActiveChar();
	}
	
	public final int getFeedBattle()
	{
		return getActiveChar().getPetLevelData().getPetFeedBattle();
	}
	
	public final int getFeedNormal()
	{
		return getActiveChar().getPetLevelData().getPetFeedNormal();
	}
	
	@Override
	public void setLevel(int value)
	{
		getActiveChar().setPetData(PetsParser.getInstance().getPetLevelData(getActiveChar().getTemplate().getId(), value));
		if (getActiveChar().getPetLevelData() == null)
		{
			throw new IllegalArgumentException("No pet data for npc: " + getActiveChar().getTemplate().getId() + " level: " + value);
		}
		getActiveChar().stopFeed();
		super.setLevel(value);
		
		getActiveChar().startFeed();
		
		if (getActiveChar().getControlItem() != null)
		{
			getActiveChar().getControlItem().setEnchantLevel(getLevel());
		}
	}
	
	public final int getMaxFeed()
	{
		return getActiveChar().getPetLevelData().getPetMaxFeed();
	}
	
	@Override
	public double getMaxHp()
	{
		final double val = calcStat(Stats.MAX_HP, getActiveChar().getPetLevelData().getPetMaxHP(), null, null);
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
	public double getMaxMp()
	{
		final double val = calcStat(Stats.MAX_MP, getActiveChar().getPetLevelData().getPetMaxMP(), null, null);
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
	public double getMAtk(Creature target, Skill skill)
	{
		return calcStat(Stats.MAGIC_ATTACK, getActiveChar().getPetLevelData().getPetMAtk(), target, skill);
	}
	
	@Override
	public double getMDef(Creature target, Skill skill)
	{
		return calcStat(Stats.MAGIC_DEFENCE, getActiveChar().getPetLevelData().getPetMDef(), target, skill);
	}
	
	@Override
	public double getPAtk(Creature target)
	{
		return calcStat(Stats.POWER_ATTACK, getActiveChar().getPetLevelData().getPetPAtk(), target, null);
	}
	
	@Override
	public double getPDef(Creature target)
	{
		return calcStat(Stats.POWER_DEFENCE, getActiveChar().getPetLevelData().getPetPDef(), target, null);
	}
	
	@Override
	public double getPAtkSpd()
	{
		double val = super.getPAtkSpd();
		if (getActiveChar().isHungry())
		{
			val = val / 2;
		}
		return val;
	}
	
	@Override
	public double getMAtkSpd()
	{
		double val = super.getMAtkSpd();
		if (getActiveChar().isHungry())
		{
			val = val / 2;
		}
		return val;
	}
	
	@Override
	public int getMaxLevel()
	{
		return ExperienceParser.getInstance().getMaxPetLevel();
	}
	
	@Override
	public double getPvePhysDamage()
	{
		return getActiveChar().getPetLevelData().getPvePhysDamage();
	}
	
	@Override
	public double getPvePhysDefence()
	{
		return getActiveChar().getPetLevelData().getPvePhysDefence();
	}
	
	@Override
	public double getPveMagicDamage()
	{
		return getActiveChar().getPetLevelData().getPveMagicDamage();
	}
	
	@Override
	public double getPveMagicDefence()
	{
		return getActiveChar().getPetLevelData().getPveMagicDefence();
	}
	
	@Override
	public double getPvpPhysDamage()
	{
		return getActiveChar().getPetLevelData().getPvpPhysDamage();
	}
	
	@Override
	public double getPvpPhysDefence()
	{
		return getActiveChar().getPetLevelData().getPvpPhysDefence();
	}
	
	@Override
	public double getPvpMagicDamage()
	{
		return getActiveChar().getPetLevelData().getPvpMagicDamage();
	}
	
	@Override
	public double getPvpMagicDefence()
	{
		return getActiveChar().getPetLevelData().getPvpMagicDefence();
	}
}