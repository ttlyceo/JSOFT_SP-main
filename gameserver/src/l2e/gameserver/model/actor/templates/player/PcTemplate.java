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
package l2e.gameserver.model.actor.templates.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.commons.util.Rnd;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.InitialBuffParser;
import l2e.gameserver.data.parser.InitialEquipmentParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.templates.character.CharTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.items.PcItemTemplate;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.skills.PcBuffTemplate;
import l2e.gameserver.model.stats.StatsSet;

public class PcTemplate extends CharTemplate
{
	private final ClassId _classId;
	
	private final float[] _baseHp;
	private final float[] _baseMp;
	private final float[] _baseCp;
	
	private final double[] _baseHpReg;
	private final double[] _baseMpReg;
	private final double[] _baseCpReg;
	
	private final double _fCollisionHeightFemale;
	private final double _fCollisionRadiusFemale;
	
	private final int _baseSafeFallHeight;
	private final int _baseRndDamage;
	private final List<PcItemTemplate> _initialEquipment;
	private final List<PcBuffTemplate> initialBuff;
	private final List<Location> _creationPoints;
	private final Map<Integer, Integer> _baseSlotDef;
	
	public PcTemplate(StatsSet set, List<Location> creationPoints)
	{
		super(set);

		_classId = ClassId.getClassId(set.getInteger("classId"));
		
		_baseHp = new float[ExperienceParser.getInstance().getMaxLevel()];
		_baseMp = new float[ExperienceParser.getInstance().getMaxLevel()];
		_baseCp = new float[ExperienceParser.getInstance().getMaxLevel()];
		_baseHpReg = new double[ExperienceParser.getInstance().getMaxLevel()];
		_baseMpReg = new double[ExperienceParser.getInstance().getMaxLevel()];
		_baseCpReg = new double[ExperienceParser.getInstance().getMaxLevel()];
		
		_baseSlotDef = new HashMap<>(12);
		_baseSlotDef.put(Inventory.PAPERDOLL_CHEST, set.getInteger("basePDefchest", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_LEGS, set.getInteger("basePDeflegs", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_HEAD, set.getInteger("basePDefhead", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_FEET, set.getInteger("basePDeffeet", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_GLOVES, set.getInteger("basePDefgloves", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_UNDER, set.getInteger("basePDefunderwear", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_CLOAK, set.getInteger("basePDefcloak", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_REAR, set.getInteger("baseMDefrear", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_LEAR, set.getInteger("baseMDeflear", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_RFINGER, set.getInteger("baseMDefrfinger", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_LFINGER, set.getInteger("baseMDefrfinger", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_NECK, set.getInteger("baseMDefneck", 0));
		
		_fCollisionRadiusFemale = set.getDouble("collisionFemaleradius");
		_fCollisionHeightFemale = set.getDouble("collisionFemaleheight");
		
		_baseSafeFallHeight = set.getInteger("baseSafeFall", 333);
		_baseRndDamage = set.getInteger("baseRndDam", 0);
		_initialEquipment = InitialEquipmentParser.getInstance().getEquipmentList(_classId);
		initialBuff = InitialBuffParser.getInstance().getBuffList(_classId);
		_creationPoints = creationPoints;
	}

	public ClassId getClassId()
	{
		return _classId;
	}
	
	public Race getRace()
	{
		return _classId.getRace();
	}
	
	public Location getCreationPoint()
	{
		return _creationPoints.get(Rnd.get(_creationPoints.size()));
	}
	
	public void setUpgainValue(String paramName, int level, double val)
	{
		switch (paramName)
		{
			case "hp" :
			{
				_baseHp[level] = (float) val;
				break;
			}
			case "mp" :
			{
				_baseMp[level] = (float) val;
				break;
			}
			case "cp" :
			{
				_baseCp[level] = (float) val;
				break;
			}
			case "hpRegen" :
			{
				_baseHpReg[level] = val;
				break;
			}
			case "mpRegen" :
			{
				_baseMpReg[level] = val;
				break;
			}
			case "cpRegen" :
			{
				_baseCpReg[level] = val;
				break;
			}
		}
	}
	
	public float getBaseHpMax(int level)
	{
		return _baseHp[level];
	}
	
	public float getBaseMpMax(int level)
	{
		return _baseMp[level];
	}
	
	public float getBaseCpMax(int level)
	{
		return _baseCp[level];
	}
	
	public double getBaseHpRegen(int level)
	{
		return _baseHpReg[level];
	}
	
	public double getBaseMpRegen(int level)
	{
		return _baseMpReg[level];
	}
	
	public double getBaseCpRegen(int level)
	{
		return _baseCpReg[level];
	}
	
	public int getBaseDefBySlot(int slotId)
	{
		return _baseSlotDef.containsKey(slotId) ? _baseSlotDef.get(slotId) : 0;
	}
	
	public double getFCollisionHeightFemale()
	{
		return _fCollisionHeightFemale;
	}
	
	public double getFCollisionRadiusFemale()
	{
		return _fCollisionRadiusFemale;
	}
	
	public int getSafeFallHeight()
	{
		return _baseSafeFallHeight;
	}
	
	public int getBaseRndDamage()
	{
		return _baseRndDamage;
	}
	
	public List<PcItemTemplate> getInitialEquipment()
	{
		return _initialEquipment;
	}
	
	public boolean hasInitialEquipment()
	{
		return _initialEquipment != null;
	}
	public List<PcBuffTemplate> getInitialBuff()
	{
		return initialBuff;
	}

	public boolean hasInitialBuff()
	{
		return initialBuff != null;
	}
}