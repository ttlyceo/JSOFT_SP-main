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
package l2e.gameserver.model.stats;

import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.CubicInstance;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;

public final class Env
{
	private double _baseValue;
	public boolean _blessedSpiritShot = false;
	public Creature _character;
	private CubicInstance _cubic;
	private Effect _effect;
	private ItemInstance _item;
	public byte _shield = 0;
	public Skill _skill;
	private boolean _skillMastery = false;
	public boolean _soulShot = false;
	public boolean _spiritShot = false;
	public Creature _target;
	public double _value;

	public Env()
	{
	}
	
	public Env(byte shield, boolean soulShot, boolean spiritShot, boolean blessedSpiritShot)
	{
		_shield = shield;
		_soulShot = soulShot;
		_spiritShot = spiritShot;
		_blessedSpiritShot = blessedSpiritShot;
	}
	
	public Env(Creature character, Creature target, Skill skill)
	{
		_character = character;
		_target = target;
		_skill = skill;
	}

	public double getBaseValue()
	{
		return _baseValue;
	}

	public Creature getCharacter()
	{
		return _character;
	}

	public CubicInstance getCubic()
	{
		return _cubic;
	}

	public Effect getEffect()
	{
		return _effect;
	}

	public ItemInstance getItem()
	{
		return _item;
	}

	public Player getPlayer()
	{
		return _character == null ? null : _character.getActingPlayer();
	}
	
	public byte getShield()
	{
		return _shield;
	}

	public Skill getSkill()
	{
		return _skill;
	}

	public Creature getTarget()
	{
		return _target;
	}

	public double getValue()
	{
		return _value;
	}

	public boolean isBlessedSpiritShot()
	{
		return _blessedSpiritShot;
	}

	public boolean isSkillMastery()
	{
		return _skillMastery;
	}

	public boolean isSoulShot()
	{
		return _soulShot;
	}

	public boolean isSpiritShot()
	{
		return _spiritShot;
	}

	public void setBaseValue(double baseValue)
	{
		_baseValue = baseValue;
	}
	
	public void setBlessedSpiritShot(boolean blessedSpiritShot)
	{
		_blessedSpiritShot = blessedSpiritShot;
	}
	
	public void setCharacter(Creature character)
	{
		_character = character;
	}
	
	public void setCubic(CubicInstance cubic)
	{
		_cubic = cubic;
	}
	
	public void setEffect(Effect effect)
	{
		_effect = effect;
	}
	
	public void setItem(ItemInstance item)
	{
		_item = item;
	}
	
	public void setShield(byte shield)
	{
		_shield = shield;
	}
	
	public void setSkill(Skill skill)
	{
		_skill = skill;
	}
	
	public void setSkillMastery(boolean skillMastery)
	{
		_skillMastery = skillMastery;
	}
	
	public void setSoulShot(boolean soulShot)
	{
		_soulShot = soulShot;
	}
	
	public void setSpiritShot(boolean spiritShot)
	{
		_spiritShot = spiritShot;
	}
	
	public void setTarget(Creature target)
	{
		_target = target;
	}
	
	public void setValue(double value)
	{
		_value = value;
	}

	public void addValue(double value)
	{
		_value += value;
	}

	public void subValue(double value)
	{
		_value -= value;
	}

	public void mulValue(double value)
	{
		_value *= value;
	}

	public void divValue(double value)
	{
		_value /= value;
	}
}