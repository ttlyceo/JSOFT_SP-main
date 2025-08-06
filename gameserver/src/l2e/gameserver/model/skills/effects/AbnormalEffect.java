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
package l2e.gameserver.model.skills.effects;

import java.util.NoSuchElementException;

public enum AbnormalEffect
{
	/* 0 */NONE("null", 0x0),
	/* 1 */BLEEDING("bleed", 0x000001),
	/* 2 */POISON("poison", 0x000002),
	/* 3 */REDCIRCLE("redcircle", 0x000004),
	/* 4 */ICE("ice", 0x000008),
	/* 5 */WIND("wind", 0x000010),
	/* 6 */FEAR("fear", 0x000020),
	/* 7 */STUN("stun", 0x000040),
	/* 8 */SLEEP("sleep", 0x000080),
	/* 9 */MUTED("mute", 0x000100),
	/* 10 */ROOT("root", 0x000200),
	/* 11 */HOLD_1("hold1", 0x000400),
	/* 12 */HOLD_2("hold2", 0x000800),
	/* 13 */UNKNOWN_13("unk13", 0x001000),
	/* 14 */BIG_HEAD("bighead", 0x002000),
	/* 15 */FLAME("flame", 0x004000),
	/* 16 */UNKNOWN_16("unknown16", 0x008000),
	/* 17 */GROW("grow", 0x010000),
	/* 18 */FLOATING_ROOT("floatroot", 0x020000),
	/* 19 */DANCE_STUNNED("dancestun", 0x040000),
	/* 20 */FIREROOT_STUN("firerootstun", 0x080000),
	/* 21 */STEALTH("stealth", 0x100000),
	/* 22 */IMPRISIONING_1("imprison1", 0x200000),
	/* 23 */IMPRISIONING_2("imprison2", 0x400000),
	/* 24 */MAGIC_CIRCLE("magiccircle", 0x800000),
	/* 25 */ICE2("ice2", 0x1000000),
	/* 26 */EARTHQUAKE("earthquake", 0x2000000),
	/* 27 */UNKNOWN_27("unknown27", 0x4000000),
	/* 28 */INVULNERABLE("invulnerable", 0x8000000),
	/* 29 */VITALITY("vitality", 0x10000000),
	/* 30 */REAL_TARGET("realtarget", 0x20000000),
	/* 31 */DEATH_MARK("deathmark", 0x40000000),
	/* 32 */SKULL_FEAR("skull_fear", 0x80000000),
	/* 33 */S_INVINCIBLE("invincible", 0x000001, true),
	/* 34 */S_AIR_STUN("airstun", 0x000002, true),
	/* 35 */S_AIR_ROOT("airroot", 0x000004, true),
	/* 36 */S_BAGUETTE_SWORD("baguettesword", 0x000008, true),
	/* 37 */S_YELLOW_AFFRO("yellowafro", 0x000010, true),
	/* 38 */S_PINK_AFFRO("pinkafro", 0x000020, true),
	/* 39 */S_BLACK_AFFRO("blackafro", 0x000040, true),
	/* 40 */S_UNKNOWN8("unknown8", 0x000080, true),
	/* 41 */S_STIGMA_SHILIEN("stigmashilien", 0x000100, true),
	/* 42 */S_STAKATOROOT("stakatoroot", 0x000200, true),
	/* 43 */S_FREEZING("freezing", 0x000400, true),
	/* 44 */S_VESPER_S("vesper_s", 0x000800, true),
	/* 45 */S_VESPER_C("vesper_c", 0x001000, true),
	/* 46 */S_VESPER_D("vesper_d", 0x002000, true),
	/* 47 */TIME_BOMB("soa_respawn", 0x004000, true),
	/* 48 */ARCANE_SHIELD("arcane_shield", 0x008000, true),
	/* 49 */AIRBIND("airbird", 0x010000, true),
	/* 50 */CHANGEBODY("changebody", 0x020000, true),
	/* 51 */KNOCKDOWN("knockdown", 0x040000, true),
	/* 52 */NAVIT_ADVENT("ave_advent_blessing", 0x080000, true),
	/* 53 */KNOCKBACK("knockback", 0x100000, true),
	/* 54 */CHANGE_7ANNIVERSARY("7anniversary", 0x200000, true),
	/* 55 */ON_SPOT_MOVEMENT,
	/* 56 */DEPORT,
	/* 57 */AURA_BUFF,
	/* 58 */AURA_BUFF_SELF, 
	/* 59 */AURA_DEBUFF,
	/* 60 */AURA_DEBUFF_SELF,
	/* 61 */HURRICANE,
	/* 62 */HURRICANE_SELF,
	/* 63 */BLACK_MARK,
	/* 64 */BR_SOUL_AVATAR,
	/* 65 */CHANGE_GRADE_B,
	/* 66 */BR_BEAM_SWORD_ONEHAND,
	/* 67 */BR_BEAM_SWORD_DUAL,
	/* 68 */D_NOCHAT,
	/* 69 */D_HERB_POWER,
	/* 70 */D_HERB_MAGIC,
	/* 71 */D_TALI_DECO_P,
	/* 72 */UNK_72,
	/* 73 */D_TALI_DECO_C,
	/* 74 */D_TALI_DECO_D,
	/* 75 */D_TALI_DECO_E,
	/* 76 */D_TALI_DECO_F,
	/* 77 */D_TALI_DECO_G,
	/* 78 */D_CHANGESHAPE_TRANSFORM_1,
	/* 79 */D_CHANGESHAPE_TRANSFORM_2,
	/* 80 */D_CHANGESHAPE_TRANSFORM_3,
	/* 81 */D_CHANGESHAPE_TRANSFORM_4,
	/* 82 */D_CHANGESHAPE_TRANSFORM_5,
	/* 83 */UNK_83,
	/* 84 */UNK_84,
	/* 85 */SANTA_SUIT,
	/* 86 */UNK_86,
	/* 87 */UNK_87,
	/* 88 */UNK_88,
	/* 89 */UNK_89,
	/* 90 */UNK_90,
	/* 91 */UNK_91,
	/* 92 */EMPTY_STARS,
	/* 93 */ONE_STAR,
	/* 94 */TWO_STARS,
	/* 95 */THREE_STARS,
	/* 96 */FOUR_STARS,
	/* 97 */FIVE_STARS,
	/* 98 */FACEOFF,
	/* 99 */UNK_99,
	/* 100 */UNK_100,
	/* 101 */UNK_101,
	/* 102 */UNK_102,
	/* 103 */UNK_103,
	/* 104 */UNK_104,
	/* 105 */UNK_105,
	/* 106 */STOCKING_FAIRY,
	/* 107 */TREE_FAIRY,
	/* 108 */SNOWMAN_FAIRY,
	/* 109 */UNK_109,
	/* 110 */UNK_110,
	/* 111 */UNK_111,
	/* 112 */UNK_112,
	/* 113 */UNK_113,
	/* 114 */STIGMA_STORM,
	/* 115 */GREEN_SPEED_UP,
	/* 116 */RED_SPEED_UP,
	/* 117 */WIND_PROTECTION,
	/* 118 */LOVE,
	/* 119 */PERFECT_STORM,
	/* 120 */UNK_120,
	/* 121 */UNK_121,
	/* 122 */UNK_122,
	/* 123 */GREAT_GRAVITY,
	/* 124 */STEEL_MIND,
	/* 125 */UNK_125,
	/* 126 */OBLATE,
	/* 127 */SPALLATION,
	/* 128 */U_HE_ASPECT_AVE,
	/* 129 */UNK_129,
	/* 130 */UNK_130,
	/* 131 */UNK_131,
	/* 132 */UNK_132,
	/* 133 */UNK_133,
	/* 134 */UNK_134,
	/* 135 */UNK_135,
	/* 136 */UNK_136,
	/* 137 */UNK_137,
	/* 138 */UNK_138,
	/* 139 */UNK_139,
	/* 140 */UNK_140,
	/* 141 */U_AVE_PALADIN_DEF,
	/* 142 */U_AVE_GUARDIAN_DEF,
	/* 143 */U_REALTAR2_AVE,
	/* 144 */U_AVE_DIVINITY,
	/* 145 */U_AVE_SHILPROTECTION,
	/* 146 */U_EVENT_STAR_CA,
	/* 147 */U_EVENT_STAR1_TA,
	/* 148 */U_EVENT_STAR2_TA,
	/* 149 */U_EVENT_STAR3_TA,
	/* 150 */U_EVENT_STAR4_TA,
	/* 151 */U_EVENT_STAR5_TA,
	/* 152 */U_AVE_ABSORB_SHIELD,
	/* 153 */U_KN_PHOENIX_AURA,
	/* 154 */U_KN_REVENGE_AURA,
	/* 155 */U_KN_EVAS_AURA,
	/* 156 */U_KN_REMPLA_AURA,
	/* 157 */U_AVE_LONGBOW,
	/* 158 */U_AVE_WIDESWORD,
	/* 159 */U_AVE_BIGFIST,
	/* 160 */U_AVE_SHADOWSTEP,
	/* 161 */U_TORNADO_AVE,
	/* 162 */U_AVE_SNOW_SLOW,
	/* 163 */U_AVE_SNOW_HOLD,
	/* 164 */UNK_164,
	/* 165 */U_AVE_TORNADO_SLOW,
	/* 166 */U_AVE_ASTATINE_WATER,
	/* 167 */U_BIGBD_CAT_NPC,
	/* 168 */U_BIGBD_UNICORN_NPC,
	/* 169 */U_BIGBD_DEMON_NPC,
	/* 170 */U_BIGBD_CAT_PC,
	/* 171 */U_BIGBD_UNICORN_PC,
	/* 172 */U_BIGBD_DEMON_PC,
	/* 173 */U_AVE_DRAGON_ULTIMATE(700),
	/* 174 */BR_POWER_OF_EVA(0),
	/* 175 */VP_KEEP(29),
	/* 176 */UNK_176,
	/* 177 */UNK_177,
	/* 178 */UNK_178,
	/* 179 */UNK_179,
	/* 180 */UNK_180,
	/* 181 */UNK_181,
	/* 182 */UNK_182,
	/* 183 */UNK_183,
	/* 184 */E_AFRO_1(37, "afrobaguette1", 0x000001, false, true),
	/* 185 */E_AFRO_2(38, "afrobaguette2", 0x000002, false, true),
	/* 186 */E_AFRO_3(39, "afrobaguette3", 0x000004, false, true),
	/* 187 */E_EVASWRATH(0, "evaswrath", 0x000008, false, true),
	/* 188 */E_HEADPHONE(0, "headphone", 0x000010, false, true),
	/* 189 */E_VESPER_1(44, "vesper1", 0x000020, false, true),
	/* 190 */E_VESPER_2(45, "vesper2", 0x000040, false, true),
	/* 191 */E_VESPER_3(46, "vesper3", 0x000080, false, true);

	public static final AbnormalEffect[] VALUES = values();

	private final int _id;
	private final int _mask;
	private final String _name;
	private final boolean _special;
	private final boolean _event;
	
	private AbnormalEffect()
	{
		_id = ordinal();
		_name = toString();
		_mask = 0x0;
		_special = false;
		_event = false;
	}
	
	private AbnormalEffect(int id)
	{
		_id = id;
		_name = toString();
		_mask = 0x0;
		_special = false;
		_event = false;
	}

	private AbnormalEffect(String name, int mask)
	{
		_id = ordinal();
		_name = name;
		_mask = mask;
		_special = false;
		_event = false;
	}
	
	private AbnormalEffect(String name, int mask, boolean special)
	{
		_id = ordinal();
		_name = name;
		_mask = mask;
		_special = special;
		_event = false;
	}
	
	private AbnormalEffect(int id, String name, int mask, boolean special, boolean event)
	{
		_id = id;
		_name = name;
		_mask = mask;
		_special = special;
		_event = event;
	}
	
	public final int getId()
	{
		return _id;
	}

	public final int getMask()
	{
		return _mask;
	}

	public final String getName()
	{
		return _name;
	}

	public final boolean isSpecial()
	{
		return _special;
	}
	
	public final boolean isEvent()
	{
		return _event;
	}

	public static AbnormalEffect getById(int id)
	{
		if (id > 0)
		{
			for(AbnormalEffect eff : AbnormalEffect.VALUES)
			{
				if(eff.getId() == id)
					return eff;
			}
			throw new NoSuchElementException("AbnormalEffect not found for id: '" + id + "'.\n Please check " + AbnormalEffect.class.getCanonicalName());
		}
		return NONE;
	}
	
	public static AbnormalEffect getByName(String name)
	{
		if ((name != null) && !name.isEmpty())
		{
			for(AbnormalEffect eff : AbnormalEffect.VALUES)
			{
				if(eff.getName().equalsIgnoreCase(name))
					return eff;
			}

			for(AbnormalEffect eff : AbnormalEffect.VALUES)
			{
				if(eff.toString().equalsIgnoreCase(name))
					return eff;
			}
			throw new NoSuchElementException("AbnormalEffect not found for name: '" + name + "'.\n Please check " + AbnormalEffect.class.getCanonicalName());
		}
		return NONE;
	}
}