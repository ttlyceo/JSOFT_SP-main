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
package l2e.gameserver.model.actor.templates;

import l2e.gameserver.model.stats.MoveType;
import l2e.gameserver.model.stats.StatsSet;

public class PetLevelTemplate
{
	private final int _ownerExpTaken;
	private final int _petFeedBattle;
	private final int _petFeedNormal;
	private final float _petMAtk;
	private final long _petMaxExp;
	private final int _petMaxFeed;
	private final float _petMaxHP;
	private final float _petMaxMP;
	private final float _petMDef;
	private final float _petPAtk;
	private final float _petPDef;
	private final float _petRegenHP;
	private final float _petRegenMP;
	private final short _petSoulShot;
	private final short _petSpiritShot;
	private final double _walkSpeedOnRide;
	private final double _runSpeedOnRide;
	private final double _slowSwimSpeedOnRide;
	private final double _fastSwimSpeedOnRide;
	private final double _slowFlySpeedOnRide;
	private final double _fastFlySpeedOnRide;
	private final double _pvePhysDamage;
	private final double _pvePhysDefence;
	private final double _pveMagicDamage;
	private final double _pveMagicDefence;
	private final double _pvpPhysDamage;
	private final double _pvpPhysDefence;
	private final double _pvpMagicDamage;
	private final double _pvpMagicDefence;

	public PetLevelTemplate(StatsSet set)
	{
		_ownerExpTaken = set.getInteger("get_exp_type");
		_petMaxExp = (long) set.getDouble("exp");
		_petMaxHP = set.getFloat("org_hp");
		_petMaxMP = set.getFloat("org_mp");
		_petPAtk = set.getFloat("org_pattack");
		_petPDef = set.getFloat("org_pdefend");
		_petMAtk = set.getFloat("org_mattack");
		_petMDef = set.getFloat("org_mdefend");
		_petMaxFeed = set.getInteger("max_meal");
		_petFeedBattle = set.getInteger("consume_meal_in_battle");
		_petFeedNormal = set.getInteger("consume_meal_in_normal");
		_petRegenHP = set.getFloat("org_hp_regen");
		_petRegenMP = set.getFloat("org_mp_regen");
		_petSoulShot = set.getShort("soulshot_count");
		_petSpiritShot = set.getShort("spiritshot_count");
		_walkSpeedOnRide = set.getDouble("walkSpeedOnRide", 0);
		_runSpeedOnRide = set.getDouble("runSpeedOnRide", 0);
		_slowSwimSpeedOnRide = set.getDouble("slowSwimSpeedOnRide", 0);
		_fastSwimSpeedOnRide = set.getDouble("fastSwimSpeedOnRide", 0);
		_slowFlySpeedOnRide = set.getDouble("slowFlySpeedOnRide", 0);
		_fastFlySpeedOnRide = set.getDouble("fastFlySpeedOnRide", 0);
		_pvePhysDamage = set.getDouble("pvePhysDamage", 1);
		_pvePhysDefence = set.getDouble("pvePhysDefence", 1);
		_pveMagicDamage = set.getDouble("pveMagicDamage", 1);
		_pveMagicDefence = set.getDouble("pveMagicDefence", 1);
		_pvpPhysDamage = set.getDouble("pvpPhysDamage", 1);
		_pvpPhysDefence = set.getDouble("pvpPhysDefence", 1);
		_pvpMagicDamage = set.getDouble("pvpMagicDamage", 1);
		_pvpMagicDefence = set.getDouble("pvpMagicDefence", 1);
	}

	public int getOwnerExpTaken()
	{
		return _ownerExpTaken;
	}

	public int getPetFeedBattle()
	{
		return _petFeedBattle;
	}

	public int getPetFeedNormal()
	{
		return _petFeedNormal;
	}

	public float getPetMAtk()
	{
		return _petMAtk;
	}

	public long getPetMaxExp()
	{
		return _petMaxExp;
	}

	public int getPetMaxFeed()
	{
		return _petMaxFeed;
	}

	public float getPetMaxHP()
	{
		return _petMaxHP;
	}

	public float getPetMaxMP()
	{
		return _petMaxMP;
	}

	public float getPetMDef()
	{
		return _petMDef;
	}

	public float getPetPAtk()
	{
		return _petPAtk;
	}

	public float getPetPDef()
	{
		return _petPDef;
	}

	public float getPetRegenHP()
	{
		return _petRegenHP;
	}

	public float getPetRegenMP()
	{
		return _petRegenMP;
	}

	public short getPetSoulShot()
	{
		return _petSoulShot;
	}

	public short getPetSpiritShot()
	{
		return _petSpiritShot;
	}
	
	public double getSpeedOnRide(MoveType mt)
	{
		switch (mt)
		{
			case WALK :
				return _walkSpeedOnRide;
			case RUN :
				return _runSpeedOnRide;
			case SLOW_SWIM :
				return _slowSwimSpeedOnRide;
			case FAST_SWIM :
				return _fastSwimSpeedOnRide;
			case SLOW_FLY :
				return _slowFlySpeedOnRide;
			case FAST_FLY :
				return _fastFlySpeedOnRide;
		}
		return 0;
	}
	
	public double getPvePhysDamage()
	{
		return _pvePhysDamage;
	}
	
	public double getPvePhysDefence()
	{
		return _pvePhysDefence;
	}
	
	public double getPveMagicDamage()
	{
		return _pveMagicDamage;
	}
	
	public double getPveMagicDefence()
	{
		return _pveMagicDefence;
	}
	
	public double getPvpPhysDamage()
	{
		return _pvpPhysDamage;
	}
	
	public double getPvpPhysDefence()
	{
		return _pvpPhysDefence;
	}
	
	public double getPvpMagicDamage()
	{
		return _pvpMagicDamage;
	}
	
	public double getPvpMagicDefence()
	{
		return _pvpMagicDefence;
	}
}