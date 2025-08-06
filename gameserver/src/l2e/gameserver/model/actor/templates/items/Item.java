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
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.StringUtil;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.interfaces.IIdentifiable;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.type.ActionType;
import l2e.gameserver.model.items.type.EtcItemType;
import l2e.gameserver.model.items.type.ItemType;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.conditions.Condition;
import l2e.gameserver.model.skills.conditions.ConditionLogicOr;
import l2e.gameserver.model.skills.conditions.ConditionPetType;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectTemplate;
import l2e.gameserver.model.skills.funcs.Func;
import l2e.gameserver.model.skills.funcs.FuncTemplate;
import l2e.gameserver.model.stats.Env;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public abstract class Item implements IIdentifiable
{
	protected static final Logger _log = LoggerFactory.getLogger(Item.class);
	
	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;

	public static final int TYPE2_WEAPON = 0;
	public static final int TYPE2_SHIELD_ARMOR = 1;
	public static final int TYPE2_ACCESSORY = 2;
	public static final int TYPE2_QUEST = 3;
	public static final int TYPE2_MONEY = 4;
	public static final int TYPE2_OTHER = 5;
	
	public static final int STRIDER = 0x1;
	public static final int GROWN_UP_WOLF_GROUP = 0x2;
	public static final int HATCHLING_GROUP = 0x4;
	public static final int ALL_WOLF_GROUP = 0x8;
	public static final int BABY_PET_GROUP = 0x16;
	public static final int UPGRADE_BABY_PET_GROUP = 0x32;
	public static final int ITEM_EQUIP_PET_GROUP = 0x64;
	
	public static final int SLOT_NONE = 0x0000;
	public static final int SLOT_UNDERWEAR = 0x0001;
	public static final int SLOT_R_EAR = 0x0002;
	public static final int SLOT_L_EAR = 0x0004;
	public static final int SLOT_LR_EAR = 0x00006;
	public static final int SLOT_NECK = 0x0008;
	public static final int SLOT_R_FINGER = 0x0010;
	public static final int SLOT_L_FINGER = 0x0020;
	public static final int SLOT_LR_FINGER = 0x0030;
	public static final int SLOT_HEAD = 0x0040;
	public static final int SLOT_R_HAND = 0x0080;
	public static final int SLOT_L_HAND = 0x0100;
	public static final int SLOT_GLOVES = 0x0200;
	public static final int SLOT_CHEST = 0x0400;
	public static final int SLOT_LEGS = 0x0800;
	public static final int SLOT_FEET = 0x1000;
	public static final int SLOT_BACK = 0x2000;
	public static final int SLOT_LR_HAND = 0x4000;
	public static final int SLOT_FULL_ARMOR = 0x8000;
	public static final int SLOT_HAIR = 0x010000;
	public static final int SLOT_ALLDRESS = 0x020000;
	public static final int SLOT_HAIR2 = 0x040000;
	public static final int SLOT_HAIRALL = 0x080000;
	public static final int SLOT_R_BRACELET = 0x100000;
	public static final int SLOT_L_BRACELET = 0x200000;
	public static final int SLOT_DECO = 0x400000;
	public static final int SLOT_BELT = 0x10000000;
	public static final int SLOT_WOLF = -100;
	public static final int SLOT_HATCHLING = -101;
	public static final int SLOT_STRIDER = -102;
	public static final int SLOT_BABYPET = -103;
	public static final int SLOT_GREATWOLF = -104;
	
	public static final int SLOT_MULTI_ALLWEAPON = SLOT_LR_HAND | SLOT_R_HAND;
	
	public static final int MATERIAL_STEEL = 0x00;
	public static final int MATERIAL_FINE_STEEL = 0x01;
	public static final int MATERIAL_BLOOD_STEEL = 0x02;
	public static final int MATERIAL_BRONZE = 0x03;
	public static final int MATERIAL_SILVER = 0x04;
	public static final int MATERIAL_GOLD = 0x05;
	public static final int MATERIAL_MITHRIL = 0x06;
	public static final int MATERIAL_ORIHARUKON = 0x07;
	public static final int MATERIAL_PAPER = 0x08;
	public static final int MATERIAL_WOOD = 0x09;
	public static final int MATERIAL_CLOTH = 0x0a;
	public static final int MATERIAL_LEATHER = 0x0b;
	public static final int MATERIAL_BONE = 0x0c;
	public static final int MATERIAL_HORN = 0x0d;
	public static final int MATERIAL_DAMASCUS = 0x0e;
	public static final int MATERIAL_ADAMANTAITE = 0x0f;
	public static final int MATERIAL_CHRYSOLITE = 0x10;
	public static final int MATERIAL_CRYSTAL = 0x11;
	public static final int MATERIAL_LIQUID = 0x12;
	public static final int MATERIAL_SCALE_OF_DRAGON = 0x13;
	public static final int MATERIAL_DYESTUFF = 0x14;
	public static final int MATERIAL_COBWEB = 0x15;
	public static final int MATERIAL_SEED = 0x16;
	public static final int MATERIAL_FISH = 0x17;
	public static final int MATERIAL_RUNE_XP = 0x18;
	public static final int MATERIAL_RUNE_SP = 0x19;
	public static final int MATERIAL_RUNE_PENALTY = 0x20;
	
	public static final int CRYSTAL_NONE = 0x00;
	public static final int CRYSTAL_D = 0x01;
	public static final int CRYSTAL_C = 0x02;
	public static final int CRYSTAL_B = 0x03;
	public static final int CRYSTAL_A = 0x04;
	public static final int CRYSTAL_S = 0x05;
	public static final int CRYSTAL_S80 = 0x06;
	public static final int CRYSTAL_S84 = 0x07;
	
	public static final int ITEM_ID_ADENA = 57;
	
	public String getItemsGrade(int itemGrade)
	{
		String grade = "";
		switch (itemGrade)
		{
			case 0 :
				grade = "NONE";
				break;
			case 1 :
				grade = "D";
				break;
			case 2 :
				grade = "C";
				break;
			case 3 :
				grade = "B";
				break;
			case 4 :
				grade = "A";
				break;
			case 5 :
				grade = "S";
				break;
			case 6 :
				grade = "S80";
				break;
			case 7 :
				grade = "S84";
				break;
		}
		return grade;
	}

	private static final int[] CRYSTAL_ITEM_ID =
	{
	        0, 1458, 1459, 1460, 1461, 1462, 1462, 1462
	};
	
	private static final int[] CRYSTAL_ENCHANT_BONUS_ARMOR =
	{
	        0, 11, 6, 11, 19, 25, 25, 25
	};
	
	private static final int[] CRYSTAL_ENCHANT_BONUS_WEAPON =
	{
	        0, 90, 45, 67, 144, 250, 250, 250
	};
	
	private final int _itemId;
	private final int _displayId;
	private final StatsSet _params;
	private final String _icon;
	private final int _weight;
	private final boolean _stackable;
	private final int _materialType;
	private final int _crystalType;
	private final int _equipReuseDelay;
	private final int _duration;
	private final int _time;
	private final int _timeLimit;
	private final int _autoDestroyTime;
	private final int _bodyPart;
	private final int _referencePrice;
	private final int _crystalCount;
	private final boolean _sellable;
	private final boolean _dropable;
	private final boolean _destroyable;
	private final boolean _tradeable;
	private final boolean _depositable;
	private final int _enchantable;
	private final boolean _elementable;
	private final boolean _elementBlackList;
	private final boolean _augmentable;
	private final boolean _questItem;
	private final boolean _freightable;
	private final boolean _isOlyRestricted;
	private final boolean _isEventRestricted;
	private final boolean _for_npc;
	private final boolean _common;
	private final boolean _heroItem;
	private final boolean _pvpItem;
	private final boolean _ex_immediate_effect;
	private final boolean _activeRune;
	private final int _defaultEnchantLevel;
	private final ActionType _defaultAction;
	private final String _showBoard;
	protected int _type1;
	protected int _type2;
	protected Elementals[] _elementals = null;
	protected FuncTemplate[] _funcTemplates;
	protected EffectTemplate[] _effectTemplates;
	protected List<Condition> _preConditions;
	private SkillHolder[] _SkillsHolder;
	private SkillHolder _unequipSkill = null;
	
	protected static final Func[] _emptyFunctionSet = new Func[0];
	protected static final Effect[] _emptyEffectSet = new Effect[0];
	
	private List<Quest> _questEvents;
	private List<ItemHolder> _requestItems;
	private List<ItemHolder> _rewardItems;
	
	private final int _useSkillDisTime;
	private final int _reuseDelay;
	private final boolean _isReuseByCron;
	private final boolean _isBlockResetReuse;
	private final int _sharedReuseGroup;
	private final int _agathionMaxEnergy;
	private final int _premiumId;
	private final boolean _isCostume;
	private final int _itemConsumeCount;
	private final boolean _isSelfResurrection;
	private final boolean _isWithHeroSkills;
	
	protected Item(StatsSet set)
	{
		_params = new StatsSet();
		_itemId = set.getInteger("item_id");
		_displayId = set.getInteger("displayId", _itemId);
		for (final String lang : Config.MULTILANG_ALLOWED)
		{
			if (lang != null)
			{
				final String name = "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1);
				_params.set(name, set.getString(name, set.getString("nameEn", "")));
			}
		}
		_icon = set.getString("icon", null);
		_weight = set.getInteger("weight", 0);
		_materialType = ItemsParser._materials.get(set.getString("material", "steel"));
		
		_duration = set.getInteger("duration", -1);
		_time = set.getInteger("time", -1);
		_timeLimit = set.getInteger("timeLimit", 0);
		_autoDestroyTime = set.getInteger("auto_destroy_time", -1) * 1000;
		_bodyPart = ItemsParser._slots.get(set.getString("bodypart", "none"));
		_referencePrice = set.getInteger("price", 0);
		_crystalType = ItemsParser._crystalTypes.get(set.getString("crystal_type", "none"));
		_crystalCount = set.getInteger("crystal_count", 0);
		_equipReuseDelay = set.getInteger("equip_reuse_delay", 0) * 1000;
		_stackable = set.getBool("is_stackable", false);
		_sellable = set.getBool("is_sellable", true);
		_dropable = set.getBool("is_dropable", true);
		_destroyable = set.getBool("is_destroyable", true);
		_tradeable = set.getBool("is_tradable", true);
		_depositable = set.getBool("is_depositable", true);
		_elementable = set.getBool("element_enabled", false);
		_elementBlackList = set.getBool("elementBlackList", false);
		_augmentable = set.getBool("augment_enabled", false);
		_enchantable = set.getInteger("enchant_enabled", 0);
		_questItem = set.getBool("is_questitem", false);
		_freightable = set.getBool("is_freightable", false);
		_isOlyRestricted = set.getBool("is_oly_restricted", false);
		_isEventRestricted = set.getBool("isEventRestricted", false);
		_for_npc = set.getBool("for_npc", false);
		_isCostume = set.getBool("isCostume", false);
		_showBoard = set.getString("showBoard", null);
		_ex_immediate_effect = set.getBool("ex_immediate_effect", false);
		_activeRune = set.getBool("activeRune", false);
		_defaultAction = set.getEnum("default_action", ActionType.class, ActionType.none);
		_useSkillDisTime = set.getInteger("useSkillDisTime", 0);
		_defaultEnchantLevel = set.getInteger("enchanted", 0);
		_reuseDelay = set.getInteger("reuse_delay", 0);
		_isReuseByCron = set.getBool("is_cron_reuse", false);
		_isBlockResetReuse = set.getBool("isBlockResetReuse", false);
		_sharedReuseGroup = set.getInteger("shared_reuse_group", 0);
		_agathionMaxEnergy = set.getInteger("agathion_energy", -1);
		_premiumId = set.getInteger("premiumId", -1);
		final String equip_condition = set.getString("equip_condition", null);
		if (equip_condition != null)
		{
			final ConditionLogicOr cond = new ConditionLogicOr();
			if (equip_condition.contains("strider"))
			{
				cond.add(new ConditionPetType(STRIDER));
			}
			if (equip_condition.contains("grown_up_wolf_group"))
			{
				cond.add(new ConditionPetType(GROWN_UP_WOLF_GROUP));
			}
			if (equip_condition.contains("hatchling_group"))
			{
				cond.add(new ConditionPetType(HATCHLING_GROUP));
			}
			if (equip_condition.contains("all_wolf_group"))
			{
				cond.add(new ConditionPetType(ALL_WOLF_GROUP));
			}
			if (equip_condition.contains("baby_pet_group"))
			{
				cond.add(new ConditionPetType(BABY_PET_GROUP));
			}
			if (equip_condition.contains("upgrade_baby_pet_group"))
			{
				cond.add(new ConditionPetType(UPGRADE_BABY_PET_GROUP));
			}
			if (equip_condition.contains("item_equip_pet_group"))
			{
				cond.add(new ConditionPetType(ITEM_EQUIP_PET_GROUP));
			}
			
			if (cond.conditions.length > 0)
			{
				attach(cond);
			}
		}
		
		String skills = set.getString("item_skill", null);
		if (skills != null)
		{
			final String[] skillsSplit = skills.split(";");
			_SkillsHolder = new SkillHolder[skillsSplit.length];
			int used = 0;
			
			for (final String element : skillsSplit)
			{
				try
				{
					final String[] skillSplit = element.split("-");
					final int id = Integer.parseInt(skillSplit[0]);
					final int level = Integer.parseInt(skillSplit[1]);
					
					if (id == 0)
					{
						_log.info(StringUtil.concat("Ignoring item_skill(", element, ") for item ", toString(), ". Skill id is 0!"));
						continue;
					}
					
					if (level == 0)
					{
						_log.info(StringUtil.concat("Ignoring item_skill(", element, ") for item ", toString(), ". Skill level is 0!"));
						continue;
					}
					
					_SkillsHolder[used] = new SkillHolder(id, level);
					++used;
				}
				catch (final Exception e)
				{
					_log.warn(StringUtil.concat("Failed to parse item_skill(", element, ") for item ", toString(), "! Format: SkillId0-SkillLevel0[;SkillIdN-SkillLevelN]"));
				}
			}
			
			if (used != _SkillsHolder.length)
			{
				final SkillHolder[] SkillsHolder = new SkillHolder[used];
				System.arraycopy(_SkillsHolder, 0, SkillsHolder, 0, used);
				_SkillsHolder = SkillsHolder;
			}
		}
		
		skills = set.getString("unequip_skill", null);
		if (skills != null)
		{
			final String[] info = skills.split("-");
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
					_log.info(StringUtil.concat("Couldnt parse ", skills, " in weapon unequip skills! item ", toString()));
				}
				if ((id > 0) && (level > 0))
				{
					_unequipSkill = new SkillHolder(id, level);
				}
			}
		}
		_common = ((_itemId >= 11605) && (_itemId <= 12361)) || ((_itemId >= 20639) && (_itemId <= 20653));
		_heroItem = ((_itemId >= 6611) && (_itemId <= 6621)) || ((_itemId >= 9388) && (_itemId <= 9390)) || (_itemId == 6842);
		_pvpItem = ((_itemId >= 10667) && (_itemId <= 10835)) || ((_itemId >= 12852) && (_itemId <= 12977)) || ((_itemId >= 14363) && (_itemId <= 14525)) || (_itemId == 14528) || (_itemId == 14529) || (_itemId == 14558) || ((_itemId >= 15913) && (_itemId <= 16024)) || ((_itemId >= 16134) && (_itemId <= 16147)) || (_itemId == 16149) || (_itemId == 16151) || (_itemId == 16153) || (_itemId == 16155) || (_itemId == 16157) || (_itemId == 16159) || ((_itemId >= 16168) && (_itemId <= 16176)) || ((_itemId >= 16179) && (_itemId <= 16220));
		_itemConsumeCount = set.getInteger("itemConsumeCount", 0);
		_isSelfResurrection = set.getBool("isSelfResurrection", false);
		_isWithHeroSkills = set.getBool("isWithHeroSkills", false);
	}
	
	public abstract ItemType getItemType();
	
	public boolean isMagicWeapon()
	{
		return false;
	}
	
	public int getEquipReuseDelay()
	{
		return _equipReuseDelay;
	}
	
	public final int getDuration()
	{
		return _duration;
	}
	
	public final int getTime()
	{
		return _time;
	}
	
	public final int getTimeLimit()
	{
		return _timeLimit;
	}
	
	public final int getAutoDestroyTime()
	{
		return _autoDestroyTime;
	}
	
	@Override
	public final int getId()
	{
		return _itemId;
	}
	
	public final int getDisplayId()
	{
		return _displayId;
	}
	
	public abstract int getItemMask();
	
	public final int getMaterialType()
	{
		return _materialType;
	}
	
	public final int getType2()
	{
		return _type2;
	}
	
	public final int getWeight()
	{
		return _weight;
	}
	
	public final boolean isCrystallizable()
	{
		return (_crystalType != Item.CRYSTAL_NONE) && (_crystalCount > 0);
	}
	
	public final int getCrystalType()
	{
		return _crystalType;
	}
	
	public final int getCrystalItemId()
	{
		return CRYSTAL_ITEM_ID[_crystalType];
	}
	
	public final int getItemGrade()
	{
		return getCrystalType();
	}
	
	public final int getItemGradeSPlus()
	{
		switch (getItemGrade())
		{
			case CRYSTAL_S80 :
			case CRYSTAL_S84 :
				return CRYSTAL_S;
			default :
				return getItemGrade();
		}
	}
	
	public final int getCrystalCount()
	{
		return _crystalCount;
	}
	
	public final int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR :
				case TYPE2_ACCESSORY :
					return _crystalCount + (CRYSTAL_ENCHANT_BONUS_ARMOR[getCrystalType()] * ((3 * enchantLevel) - 6));
				case TYPE2_WEAPON :
					return _crystalCount + (CRYSTAL_ENCHANT_BONUS_WEAPON[getCrystalType()] * ((2 * enchantLevel) - 3));
				default :
					return _crystalCount;
			}
		}
		else if (enchantLevel > 0)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR :
				case TYPE2_ACCESSORY :
					return _crystalCount + (CRYSTAL_ENCHANT_BONUS_ARMOR[getCrystalType()] * enchantLevel);
				case TYPE2_WEAPON :
					return _crystalCount + (CRYSTAL_ENCHANT_BONUS_WEAPON[getCrystalType()] * enchantLevel);
				default :
					return _crystalCount;
			}
		}
		else
		{
			return _crystalCount;
		}
	}
	
	public String getName(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "name" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public final Elementals[] getElementals()
	{
		return _elementals;
	}
	
	public Elementals getElemental(byte attribute)
	{
		if (_elementals == null)
		{
			return null;
		}
		for (final Elementals elm : _elementals)
		{
			if (elm.getElement() == attribute)
			{
				return elm;
			}
		}
		return null;
	}
	
	public void setElementals(Elementals element)
	{
		if (_elementals == null)
		{
			_elementals = new Elementals[1];
			_elementals[0] = element;
		}
		else
		{
			Elementals elm = getElemental(element.getElement());
			if (elm != null)
			{
				elm.setValue(element.getValue());
			}
			else
			{
				elm = element;
				final Elementals[] array = new Elementals[_elementals.length + 1];
				System.arraycopy(_elementals, 0, array, 0, _elementals.length);
				array[_elementals.length] = elm;
				_elementals = array;
			}
		}
	}
	
	public final int getBodyPart()
	{
		return _bodyPart;
	}
	
	public final int getType1()
	{
		return _type1;
	}
	
	public final boolean isStackable()
	{
		return _stackable;
	}
	
	public boolean isConsumable()
	{
		return false;
	}
	
	public boolean isEquipable()
	{
		return (getBodyPart() != 0) && !(getItemType() instanceof EtcItemType);
	}
	
	public boolean isArrow()
	{
		return getItemType() == EtcItemType.ARROW;
	}
	
	public final int getReferencePrice()
	{
		return (isConsumable() ? (int) (_referencePrice * Config.RATE_CONSUMABLE_COST) : _referencePrice);
	}
	
	public final boolean isSellable()
	{
		return _sellable;
	}
	
	public final boolean isDropable()
	{
		return _dropable;
	}
	
	public final boolean isDestroyable()
	{
		return _destroyable;
	}
	
	public final boolean isTradeable()
	{
		return _tradeable;
	}
	
	public final boolean isDepositable()
	{
		return _depositable;
	}
	
	public final int isEnchantable()
	{
		return Arrays.binarySearch(Config.ENCHANT_BLACKLIST, getId()) < 0 ? _enchantable : 0;
	}
	
	public final boolean isElementable()
	{
		return _elementBlackList ? false : Config.ENCHANT_ELEMENT_ALL_ITEMS ? ((isArmor() || isWeapon()) && getCrystalType() >= Item.CRYSTAL_S && getBodyPart() != Item.SLOT_L_HAND) : _elementable;
	}
	
	public final boolean isCommon()
	{
		return _common;
	}
	
	public final boolean isHeroItem()
	{
		return _heroItem;
	}
	
	public final boolean isPvpItem()
	{
		return _pvpItem;
	}
	
	public boolean isPotion()
	{
		return (getItemType() == EtcItemType.POTION);
	}
	
	public boolean isElixir()
	{
		return (getItemType() == EtcItemType.ELIXIR);
	}
	
	public boolean isCapsule()
	{
		return (getDefaultAction() == ActionType.capsule);
	}
	
	public final Func[] getStatFuncs(ItemInstance item, Creature player)
	{
		if ((_funcTemplates == null) || (_funcTemplates.length == 0))
		{
			return _emptyFunctionSet;
		}
		
		final ArrayList<Func> funcs = new ArrayList<>(_funcTemplates.length);
		
		final Env env = new Env();
		env.setCharacter(player);
		env.setTarget(player);
		env.setItem(item);
		
		Func f;
		for (final FuncTemplate t : _funcTemplates)
		{
			f = t.getFunc(env, item);
			if (f != null)
			{
				funcs.add(f);
			}
		}
		
		if (funcs.isEmpty())
		{
			return _emptyFunctionSet;
		}
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	public Effect[] getEffects(ItemInstance item, Creature player)
	{
		if ((_effectTemplates == null) || (_effectTemplates.length == 0))
		{
			return _emptyEffectSet;
		}
		
		final List<Effect> effects = new ArrayList<>(_effectTemplates.length);
		
		final Env env = new Env();
		env.setCharacter(player);
		env.setTarget(player);
		env.setItem(item);
		
		for (final EffectTemplate et : _effectTemplates)
		{
			final Effect e = et.getEffect(env);
			if (e != null)
			{
				e.scheduleEffect(true);
				effects.add(e);
			}
		}
		
		if (effects.isEmpty())
		{
			return _emptyEffectSet;
		}
		return effects.toArray(new Effect[effects.size()]);
	}
	
	public void attach(FuncTemplate f)
	{
		switch (f.stat)
		{
			case FIRE_RES :
			case FIRE_POWER :
				setElementals(new Elementals(Elementals.FIRE, (int) f.lambda.calc(null)));
				break;
			case WATER_RES :
			case WATER_POWER :
				setElementals(new Elementals(Elementals.WATER, (int) f.lambda.calc(null)));
				break;
			case WIND_RES :
			case WIND_POWER :
				setElementals(new Elementals(Elementals.WIND, (int) f.lambda.calc(null)));
				break;
			case EARTH_RES :
			case EARTH_POWER :
				setElementals(new Elementals(Elementals.EARTH, (int) f.lambda.calc(null)));
				break;
			case HOLY_RES :
			case HOLY_POWER :
				setElementals(new Elementals(Elementals.HOLY, (int) f.lambda.calc(null)));
				break;
			case DARK_RES :
			case DARK_POWER :
				setElementals(new Elementals(Elementals.DARK, (int) f.lambda.calc(null)));
				break;
		}
		if (_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[]
			{
			        f
			};
		}
		else
		{
			final int len = _funcTemplates.length;
			final FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}
	
	public void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
		{
			_effectTemplates = new EffectTemplate[]
			{
			        effect
			};
		}
		else
		{
			final int len = _effectTemplates.length;
			final EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
	}
	
	public final void attach(Condition c)
	{
		if (_preConditions == null)
		{
			_preConditions = new ArrayList<>(1);
		}
		if (!_preConditions.contains(c))
		{
			_preConditions.add(c);
		}
	}
	
	public boolean hasSkills()
	{
		return _SkillsHolder != null;
	}
	
	public final SkillHolder[] getSkills()
	{
		return _SkillsHolder;
	}
	
	public final Skill getUnequipSkill()
	{
		return _unequipSkill == null ? null : _unequipSkill.getSkill();
	}
	
	public boolean checkCondition(Creature activeChar, GameObject target, boolean sendMessage)
	{
		if (activeChar.canOverrideCond(PcCondOverride.ITEM_CONDITIONS) && !activeChar.getAccessLevel().allowItemRestriction())
		{
			return true;
		}
		
		if ((isOlyRestrictedItem() || isHeroItem()) && ((activeChar.isPlayer()) && activeChar.getActingPlayer().isInOlympiadMode()))
		{
			if (isEquipable())
			{
				activeChar.sendPacket(SystemMessageId.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT);
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			}
			return false;
		}
		
		if (!isConditionAttached())
		{
			return true;
		}
		
		final Env env = new Env();
		env.setCharacter(activeChar);
		if (target instanceof Creature)
		{
			env.setTarget((Creature) target);
		}
		
		for (final Condition preCondition : _preConditions)
		{
			if (preCondition == null)
			{
				continue;
			}
			
			if (!preCondition.test(env))
			{
				if (activeChar instanceof Summon)
				{
					activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
					return false;
				}
				
				if (sendMessage)
				{
					final String msg = preCondition.getMessage();
					final int msgId = preCondition.getMessageId();
					if (msg != null)
					{
						activeChar.sendMessage(msg);
					}
					else if (msgId != 0)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(msgId);
						if (preCondition.isAddName())
						{
							sm.addItemName(_itemId);
						}
						activeChar.sendPacket(sm);
					}
				}
				return false;
			}
		}
		return true;
	}
	
	public boolean isConditionAttached()
	{
		return (_preConditions != null) && !_preConditions.isEmpty();
	}
	
	public boolean isQuestItem()
	{
		return _questItem;
	}
	
	public boolean isFreightable()
	{
		return _freightable;
	}
	
	public boolean isOlyRestrictedItem()
	{
		return _isOlyRestricted || Config.LIST_OLY_RESTRICTED_ITEMS.contains(_itemId);
	}
	
	public boolean isEventRestrictedItem()
	{
		return _isEventRestricted;
	}
	
	public boolean isForNpc()
	{
		return _for_npc;
	}
	
	@Override
	public String toString()
	{
		return getName(null) + "(" + _itemId + ")";
	}
	
	public boolean is_ex_immediate_effect()
	{
		return _ex_immediate_effect;
	}
	
	public boolean isActiveRune()
	{
		return _activeRune;
	}
	
	public ActionType getDefaultAction()
	{
		return _defaultAction;
	}
	
	public int useSkillDisTime()
	{
		return _useSkillDisTime;
	}
	
	public int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	public boolean isReuseByCron()
	{
		return _isReuseByCron;
	}
	
	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}

	public int getDisplayReuseGroup()
	{
		return _sharedReuseGroup < 0 ? -1 : _sharedReuseGroup;
	}
	
	public int getAgathionMaxEnergy()
	{
		return _agathionMaxEnergy;
	}
	
	public String getIcon()
	{
		return _icon;
	}
	
	public String getShowBoard()
	{
		return _showBoard;
	}
	
	public void addQuestEvent(Quest q)
	{
		if (_questEvents == null)
		{
			_questEvents = new ArrayList<>();
		}
		_questEvents.add(q);
	}
	
	public List<Quest> getQuestEvents()
	{
		return _questEvents;
	}
	
	public int getDefaultEnchantLevel()
	{
		return _defaultEnchantLevel;
	}
	
	public boolean isPetItem()
	{
		return getItemType() == EtcItemType.PET_COLLAR;
	}
	
	public Skill getEnchant4Skill()
	{
		return null;
	}
	
	public boolean isShield()
	{
		return _bodyPart == 256;
	}

	public boolean isAccessory()
	{
		return getType2() == TYPE2_ACCESSORY;
	}
	
	public boolean isCloak()
	{
		return _bodyPart == SLOT_BACK;
	}

	public boolean isUnderwear()
	{
		return _bodyPart == SLOT_UNDERWEAR;
	}
	
	public boolean isBelt()
	{
		return _bodyPart == SLOT_BELT;
	}
	
	public boolean isSoulCrystal()
	{
		return ((_itemId >= 4629) && (_itemId <= 4661)) || ((_itemId >= 5577) && (_itemId <= 5582)) || ((_itemId >= 5908) && (_itemId <= 5914)) || ((_itemId >= 9570) && (_itemId <= 9572)) || ((_itemId >= 10480) && (_itemId <= 10482)) || ((_itemId >= 13071) && (_itemId <= 13073)) || ((_itemId >= 15541) && (_itemId <= 15543)) || ((_itemId >= 15826) && (_itemId <= 15828));
	}

	public boolean isLifeStone()
	{
		return ((_itemId >= 8723) && (_itemId <= 8762)) || ((_itemId >= 9573) && (_itemId <= 9576)) || ((_itemId >= 10483) && (_itemId <= 10486)) || ((_itemId >= 12754) && (_itemId <= 12763)) || (_itemId == 12821) || (_itemId == 12822) || ((_itemId >= 12840) && (_itemId <= 12851)) || (_itemId == 14008) || ((_itemId >= 14166) && (_itemId <= 14169)) || ((_itemId >= 16160) && (_itemId <= 16167)) || (_itemId == 16177) || (_itemId == 16178);
	}

	public boolean isEnchantScroll()
	{
		return ((_itemId >= 6569) && (_itemId <= 6578)) || ((_itemId >= 17255) && (_itemId <= 17264)) || ((_itemId >= 22314) && (_itemId <= 22323)) || ((_itemId >= 949) && (_itemId <= 962)) || ((_itemId >= 729) && (_itemId <= 732));
	}

	public boolean isForgottenScroll()
	{
		return ((_itemId >= 10549) && (_itemId <= 10599)) || ((_itemId >= 12768) && (_itemId <= 12778)) || ((_itemId >= 14170) && (_itemId <= 14227)) || (_itemId == 17030) || ((_itemId >= 17034) && (_itemId <= 17039));
	}

	public boolean isCodexBook()
	{
		return _itemId >= 9625 && _itemId <= 9627 || _itemId == 6622;
	}
	
	public boolean isAttributeStone()
	{
		return _itemId >= 9546 && _itemId <= 9551;
	}
	
	public boolean isAttributeCrystal()
	{
		return _itemId >= 9552 && _itemId <= 9557;
	}
	
	public boolean isAttributeJewel()
	{
		return _itemId >= 9558 && _itemId <= 9563;
	}
	
	public boolean isAttributeEnergy()
	{
		return _itemId >= 9564 && _itemId <= 9569;
	}
	
	public boolean isSealStone()
	{
		return _itemId >= 6360 && _itemId <= 6362;
	}
	
	public boolean isKeyMatherial()
	{
		return getItemType() == EtcItemType.MATERIAL;
	}
	
	public boolean isRecipe()
	{
		return getItemType() == EtcItemType.RECIPE;
	}

	public boolean isAdena()
	{
		return (_itemId == 57) || (_itemId == 6360) || (_itemId == 6361) || (_itemId == 6362);
	}

	public boolean isWeapon()
	{
		return getType2() == Item.TYPE2_WEAPON;
	}
	
	public boolean isArmor()
	{
		return getType2() == Item.TYPE2_SHIELD_ARMOR;
	}
	
	public boolean isJewel()
	{
		return (getBodyPart() == Item.SLOT_NECK || getBodyPart() == (Item.SLOT_L_FINGER | Item.SLOT_R_FINGER) || getBodyPart() == (Item.SLOT_L_EAR | Item.SLOT_R_EAR));
	}
	
	public FuncTemplate[] getAttachedFuncs()
	{
		if ((_funcTemplates == null) || (_funcTemplates.length == 0))
		{
			return new FuncTemplate[0];
		}
		return _funcTemplates;
	}

	public boolean isEquipment()
	{
		return _type1 != TYPE1_ITEM_QUESTITEM_ADENA;
	}

	public boolean isCommonItem()
	{
		return getName("en").startsWith("Common Item - ");
	}
	
	public boolean isHerb()
	{
		return getItemType() == EtcItemType.HERB;
	}

	public boolean isEpolets()
	{
		return _itemId == 9912;
	}
	
	public boolean isExtractableItem()
	{
		return false;
	}
	
	public boolean isBracelet()
	{
		return _bodyPart == SLOT_R_BRACELET || _bodyPart == SLOT_L_BRACELET;
	}
	
	public boolean isCostume()
	{
		return _isCostume;
	}
	
	public boolean isSealedItem()
	{
		return getName("en").startsWith("Sealed");
	}
	
	public int getPremiumId()
	{
		return _premiumId;
	}
	
	public final int getItemConsume()
	{
		return _itemConsumeCount;
	}
	
	public boolean isNobleStone()
	{
		return _itemId == 14052;
	}
	
	public boolean isTalisman()
	{
		return _bodyPart == SLOT_DECO;
	}
	
	public boolean isSelfResurrection()
	{
		return _isSelfResurrection;
	}
	
	public boolean isWithHeroSkills()
	{
		return _isWithHeroSkills;
	}
	
	public boolean isAugmentable()
	{
		return _augmentable;
	}
	
	public void addRequestItem(ItemHolder item)
	{
		if (_requestItems == null)
		{
			_requestItems = new ArrayList<>();
		}
		_requestItems.add(item);
	}
	
	public List<ItemHolder> getRequestItems()
	{
		return _requestItems;
	}
	
	public void addRewardItem(ItemHolder item)
	{
		if (_rewardItems == null)
		{
			_rewardItems = new ArrayList<>();
		}
		_rewardItems.add(item);
	}
	
	public List<ItemHolder> getRewardItems()
	{
		return _rewardItems;
	}
	
	public boolean isBlockResetReuse()
	{
		return _isReuseByCron || _isBlockResetReuse;
	}
}