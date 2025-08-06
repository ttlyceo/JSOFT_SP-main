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
package l2e.gameserver.model.actor.templates.items;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.handler.skillhandlers.SkillHandler;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.type.WeaponType;
import l2e.gameserver.model.quest.QuestEventType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.conditions.Condition;
import l2e.gameserver.model.skills.conditions.ConditionGameChance;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.Formulas;
import l2e.gameserver.model.stats.StatsSet;

public final class Weapon extends Item
{
	private final WeaponType _type;
	private final boolean _isMagicWeapon;
	private final int _rndDam;
	private final int _soulShotCount;
	private final int _spiritShotCount;
	private final int _mpConsume;
	private SkillHolder _enchant4Skill = null;
	private final int _changeWeaponId;
	private final SkillHolder _unequipSkill = null;

	private SkillHolder _skillsOnMagic;
	private Condition _skillsOnMagicCondition = null;
	private SkillHolder _skillsOnCrit;
	private Condition _skillsOnCritCondition = null;
	
	private final int _reducedSoulshot;
	private final int _reducedSoulshotChance;
	
	private final int _reducedMpConsume;
	private final int _reducedMpConsumeChance;
	
	private final boolean _isForceEquip;
	private final boolean _isAttackWeapon;
	private final boolean _useWeaponSkillsOnly;
	
	private final int _baseAttackRange;
	private int[] _damageRange;
	
	public Weapon(StatsSet set)
	{
		super(set);
		_type = WeaponType.valueOf(set.getString("weapon_type", "none").toUpperCase());
		_type1 = Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
		_type2 = Item.TYPE2_WEAPON;
		_isMagicWeapon = set.getBool("is_magic_weapon", false);
		_soulShotCount = set.getInteger("soulshots", 0);
		_spiritShotCount = set.getInteger("spiritshots", 0);
		_rndDam = set.getInteger("random_damage", 0);
		_mpConsume = set.getInteger("mp_consume", 0);
		_baseAttackRange = set.getInteger("attack_range", 40);
		
		_damageRange = null;
		final var damgeRange = set.getString("damage_range", "").split(";");
		if ((damgeRange.length > 1) && Util.isDigit(damgeRange[3]))
		{
			_damageRange = new int[4];
			_damageRange[0] = Integer.parseInt(damgeRange[0]);
			_damageRange[1] = Integer.parseInt(damgeRange[1]);
			_damageRange[2] = Integer.parseInt(damgeRange[2]);
			_damageRange[3] = Integer.parseInt(damgeRange[3]);
		}
		
		final var reduced_soulshots = set.getString("reduced_soulshot", "").split(",");
		_reducedSoulshotChance = (reduced_soulshots.length == 2) ? Integer.parseInt(reduced_soulshots[0]) : 0;
		_reducedSoulshot = (reduced_soulshots.length == 2) ? Integer.parseInt(reduced_soulshots[1]) : 0;
		
		final var reduced_mpconsume = set.getString("reduced_mp_consume", "").split(",");
		_reducedMpConsumeChance = (reduced_mpconsume.length == 2) ? Integer.parseInt(reduced_mpconsume[0]) : 0;
		_reducedMpConsume = (reduced_mpconsume.length == 2) ? Integer.parseInt(reduced_mpconsume[1]) : 0;
		
		var skill = set.getString("enchant4_skill", null);
		if (skill != null)
		{
			final var info = skill.split("-");
			if ((info != null) && (info.length == 2))
			{
				var id = 0;
				var level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (final Exception nfe)
				{
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon enchant skills! item ", toString()));
				}
				if ((id > 0) && (level > 0))
				{
					_enchant4Skill = new SkillHolder(id, level);
				}
			}
		}
		
		skill = set.getString("onmagic_skill", null);
		if (skill != null)
		{
			final var info = skill.split("-");
			final var chance = set.getInteger("onmagic_chance", 100);
			if ((info != null) && (info.length == 2))
			{
				var id = 0;
				var level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (final Exception nfe)
				{
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon onmagic skills! item ", toString()));
				}
				if ((id > 0) && (level > 0) && (chance > 0))
				{
					_skillsOnMagic = new SkillHolder(id, level);
					_skillsOnMagicCondition = new ConditionGameChance(chance);
				}
			}
		}
		
		skill = set.getString("oncrit_skill", null);
		if (skill != null)
		{
			final String[] info = skill.split("-");
			final var chance = set.getInteger("oncrit_chance", 100);
			if ((info != null) && (info.length == 2))
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (final Exception nfe)
				{
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon oncrit skills! item ", toString()));
				}
				if ((id > 0) && (level > 0) && (chance > 0))
				{
					_skillsOnCrit = new SkillHolder(id, level);
					_skillsOnCritCondition = new ConditionGameChance(chance);
				}
			}
		}
		
		skill = set.getString("unequip_skill", null);
		if (skill != null)
		{
			final var info = skill.split("-");
			if ((info != null) && (info.length == 2))
			{
				var id = 0;
				var level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (final Exception nfe)
				{
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon unequip skills! item ", toString()));
				}
				if ((id > 0) && (level > 0))
				{
					setUnequipSkills(new SkillHolder(id, level));
				}
			}
		}
		
		_changeWeaponId = set.getInteger("change_weaponId", 0);
		_isForceEquip = set.getBool("isForceEquip", false);
		_isAttackWeapon = set.getBool("isAttackWeapon", true);
		_useWeaponSkillsOnly = set.getBool("useWeaponSkillsOnly", false);
	}
	
	@Override
	public WeaponType getItemType()
	{
		return _type;
	}
	
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}
	
	@Override
	public boolean isMagicWeapon()
	{
		return _isMagicWeapon;
	}
	
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}
	
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}
	
	public int getReducedSoulShot()
	{
		return _reducedSoulshot;
	}
	
	public int getReducedSoulShotChance()
	{
		return _reducedSoulshotChance;
	}
	
	public int getRandomDamage()
	{
		return _rndDam;
	}
	
	public int getMpConsume()
	{
		return _mpConsume;
	}
	
	public int getReducedMpConsume()
	{
		return _reducedMpConsume;
	}
	
	public int getReducedMpConsumeChance()
	{
		return _reducedMpConsumeChance;
	}
	
	@Override
	public Skill getEnchant4Skill()
	{
		if (_enchant4Skill == null)
		{
			return null;
		}
		return _enchant4Skill.getSkill();
	}
	
	public int getChangeWeaponId()
	{
		return _changeWeaponId;
	}
	
	public boolean isForceEquip()
	{
		return _isForceEquip;
	}
	
	public boolean isAttackWeapon()
	{
		return _isAttackWeapon;
	}
	
	public boolean useWeaponSkillsOnly()
	{
		return _useWeaponSkillsOnly;
	}
	
	public Effect[] getSkillEffects(Creature caster, Creature target, boolean crit)
	{
		if ((_skillsOnCrit == null) || !crit)
		{
			return _emptyEffectSet;
		}
		
		final List<Effect> effects = new ArrayList<>();
		final var onCritSkill = _skillsOnCrit.getSkill();
		if (_skillsOnCritCondition != null)
		{
			final var env = new Env();
			env.setCharacter(caster);
			env.setTarget(target);
			env.setSkill(onCritSkill);
			if (!_skillsOnCritCondition.test(env))
			{
				return _emptyEffectSet;
			}
		}
		
		if (onCritSkill == null)
		{
			return _emptyEffectSet;
		}
		
		if (!onCritSkill.checkCondition(caster, target, false, true) || (target != null && target.isPlayer() && onCritSkill.getSkillType() == SkillType.CONFUSE_MOB_ONLY))
		{
			return _emptyEffectSet;
		}
		
		final var shld = Formulas.calcShldUse(caster, target, onCritSkill);
		if (!Formulas.calcSkillSuccess(caster, target, onCritSkill, shld, false, false, false))
		{
			return _emptyEffectSet;
		}
		
		if (target != null)
		{
			final var sk = target.getFirstEffect(onCritSkill.getId());
			if (sk != null)
			{
				sk.exit();
			}
		}
		
		for (final var e : onCritSkill.getEffects(caster, target, new Env(shld, false, false, false), true))
		{
			effects.add(e);
		}
		if (effects.isEmpty())
		{
			return _emptyEffectSet;
		}
		return effects.toArray(new Effect[effects.size()]);
	}
	
	public Effect[] getSkillEffects(Creature caster, Creature target, Skill trigger)
	{
		if (_skillsOnMagic == null)
		{
			return _emptyEffectSet;
		}
		
		final var onMagicSkill = _skillsOnMagic.getSkill();
		if (onMagicSkill == null || trigger == null)
		{
			return _emptyEffectSet;
		}
		
		if (trigger.isOffensive() != onMagicSkill.isOffensive() || trigger.isMagic() != onMagicSkill.isMagic())
		{
			return _emptyEffectSet;
		}
		
		final var player = caster.getActingPlayer();
		final var playerTarget = caster.getActingPlayer();
		if ((player != null && playerTarget != null && onMagicSkill.isDebuff() && player.isFriend(playerTarget)) || trigger.isToggle() || (onMagicSkill.isDebuff() && caster == target))
		{
			return _emptyEffectSet;
		}
		
		if (_skillsOnMagicCondition != null)
		{
			final Env env = new Env();
			env.setCharacter(caster);
			env.setTarget(target);
			env.setSkill(onMagicSkill);
			if (!_skillsOnMagicCondition.test(env))
			{
				return _emptyEffectSet;
			}
		}
		
		if (!onMagicSkill.checkCondition(caster, target, false, true))
		{
			return _emptyEffectSet;
		}
		
		final var shld = Formulas.calcShldUse(caster, target, onMagicSkill);
		if (onMagicSkill.isOffensive() && !Formulas.calcSkillSuccess(caster, target, onMagicSkill, shld, false, false, false))
		{
			return _emptyEffectSet;
		}
		
		final Creature[] targets =
		{
		        target
		};
		
		final var handler = SkillHandler.getInstance().getHandler(onMagicSkill.getSkillType());
		if (handler != null)
		{
			handler.useSkill(caster, onMagicSkill, targets);
		}
		else
		{
			onMagicSkill.useSkill(caster, targets);
		}
		
		if (caster.isPlayer())
		{
			for (final var npcMob : World.getInstance().getAroundNpc(caster))
			{
				if (npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE) != null)
				{
					for (final var quest : npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE))
					{
						quest.notifySkillSee(npcMob, caster.getActingPlayer(), onMagicSkill, targets, false);
					}
				}
			}
		}
		return _emptyEffectSet;
	}
	
	public SkillHolder getUnequipSkills()
	{
		return _unequipSkill;
	}
	
	public void setUnequipSkills(SkillHolder unequipSkill)
	{
		unequipSkill = _unequipSkill;
	}
	
	public int getBaseAttackRange()
	{
		return _baseAttackRange;
	}
	
	public int[] getDamageRange()
	{
		return _damageRange;
	}
}