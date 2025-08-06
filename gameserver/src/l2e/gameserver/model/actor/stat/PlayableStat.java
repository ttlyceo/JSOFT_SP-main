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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.type.SwampZone;

public class PlayableStat extends CharStat
{
	protected static final Logger _log = LoggerFactory.getLogger(PlayableStat.class);

	public PlayableStat(Playable activeChar)
	{
		super(activeChar);
	}

	public boolean addExp(long value)
	{
		if (((getExp() + value) < 0) || ((value > 0) && (getExp() == (getExpForLevel(getMaxLevel()) - 1))))
		{
			return true;
		}

		if ((getExp() + value) >= getExpForLevel(getMaxLevel()))
		{
			value = getExpForLevel(getMaxLevel()) - 1 - getExp();
		}

		setExp(getExp() + value);

		int minimumLevel = 1;
		if (getActiveChar() instanceof PetInstance)
		{
			minimumLevel = PetsParser.getInstance().getPetMinLevel(((PetInstance) getActiveChar()).getTemplate().getId());
		}

		int level = minimumLevel;

		for (int tmp = level; tmp <= getMaxLevel(); tmp++)
		{
			if (getExp() >= getExpForLevel(tmp))
			{
				continue;
			}
			level = --tmp;
			break;
		}
		if ((level != getLevel()) && (level >= minimumLevel))
		{
			addLevel((level - getLevel()), false);
		}
		return true;
	}

	public boolean removeExp(long value)
	{
		if ((getExp() - value) < 0)
		{
			value = getExp() - 1;
		}

		setExp(getExp() - value);

		int minimumLevel = 1;
		if (getActiveChar() instanceof PetInstance)
		{
			minimumLevel = PetsParser.getInstance().getPetMinLevel(((PetInstance) getActiveChar()).getTemplate().getId());
		}
		int level = minimumLevel;

		for (int tmp = level; tmp <= getMaxLevel(); tmp++)
		{
			if (getExp() >= getExpForLevel(tmp))
			{
				continue;
			}
			level = --tmp;
			break;
		}
		if ((level != getLevel()) && (level >= minimumLevel))
		{
			addLevel((level - getLevel()), true);
		}
		return true;
	}

	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		boolean expAdded = false;
		boolean spAdded = false;
		if (addToExp >= 0)
		{
			expAdded = addExp(addToExp);
		}
		if (addToSp >= 0)
		{
			spAdded = addSp(addToSp);
		}
		return expAdded || spAdded;
	}

	public boolean removeExpAndSp(long removeExp, int removeSp)
	{
		boolean expRemoved = false;
		boolean spRemoved = false;
		if (removeExp > 0)
		{
			expRemoved = removeExp(removeExp);
		}
		if (removeSp > 0)
		{
			spRemoved = removeSp(removeSp);
		}

		return expRemoved || spRemoved;
	}

	public boolean addLevel(int value, boolean canLower)
	{
		if ((getLevel() + value) > (getMaxLevel() - 1))
		{
			if (getLevel() < (getMaxLevel() - 1))
			{
				value = (getMaxLevel() - 1 - getLevel());
			}
			else
			{
				return false;
			}
		}
		
		if (!canLower && value < 0)
		{
			return false;
		}
		
		final boolean levelIncreased = ((getLevel() + value) > getLevel());
		value += getLevel();
		setLevel(value);

		if ((getExp() >= getExpForLevel(getLevel() + 1)) || (getExpForLevel(getLevel()) > getExp()))
		{
			setExp(getExpForLevel(getLevel()));
		}

		if (!levelIncreased && getActiveChar().isPlayer() && !((Player) (getActiveChar())).isGM() && Config.DECREASE_SKILL_LEVEL)
		{
			((Player) (getActiveChar())).checkPlayerSkills();
		}

		if (!levelIncreased)
		{
			return false;
		}

		getActiveChar().getStatus().setCurrentHp(getActiveChar().getStat().getMaxHp());
		getActiveChar().getStatus().setCurrentMp(getActiveChar().getStat().getMaxMp());

		return true;
	}

	public boolean addSp(int value)
	{
		if (value < 0)
		{
			_log.warn("wrong usage");
			return false;
		}
		final int currentSp = getSp();
		if (currentSp == Integer.MAX_VALUE)
		{
			return false;
		}

		if (currentSp > (Integer.MAX_VALUE - value))
		{
			value = Integer.MAX_VALUE - currentSp;
		}

		setSp(currentSp + value);
		return true;
	}

	public boolean removeSp(int value)
	{
		final int currentSp = getSp();
		if (currentSp < value)
		{
			value = currentSp;
		}
		setSp(getSp() - value);
		return true;
	}

	public long getExpForLevel(int level)
	{
		return level;
	}

	@Override
	public double getRunSpeed()
	{
		if (getActiveChar().isInsideZone(ZoneId.SWAMP))
		{
			final SwampZone zone = ZoneManager.getInstance().getZone(getActiveChar(), SwampZone.class);
			if (zone != null)
			{
				return super.getRunSpeed() * zone.getMoveBonus(getActiveChar());
			}
		}
		return super.getRunSpeed();
	}

	@Override
	public double getWalkSpeed()
	{
		if (getActiveChar().isInsideZone(ZoneId.SWAMP))
		{
			final SwampZone zone = ZoneManager.getInstance().getZone(getActiveChar(), SwampZone.class);
			if (zone != null)
			{
				return super.getWalkSpeed() * zone.getMoveBonus(getActiveChar());
			}
		}
		return super.getWalkSpeed();
	}

	@Override
	public Playable getActiveChar()
	{
		return (Playable) super.getActiveChar();
	}

	public int getMaxLevel()
	{
		return ExperienceParser.getInstance().getMaxLevel();
	}
}