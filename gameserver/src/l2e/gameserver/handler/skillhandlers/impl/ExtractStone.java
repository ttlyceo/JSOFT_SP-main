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
package l2e.gameserver.handler.skillhandlers.impl;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.handler.skillhandlers.ISkillHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.network.SystemMessageId;

public class ExtractStone implements ISkillHandler
{
	private final static int EXTRACT_SCROLL_SKILL = 2630;
	private final static int EXTRACTED_COARSE_RED_STAR_STONE = 13858;
	private final static int EXTRACTED_COARSE_BLUE_STAR_STONE = 13859;
	private final static int EXTRACTED_COARSE_GREEN_STAR_STONE = 13860;
	
	private final static int EXTRACTED_RED_STAR_STONE = 14009;
	private final static int EXTRACTED_BLUE_STAR_STONE = 14010;
	private final static int EXTRACTED_GREEN_STAR_STONE = 14011;
	
	private final static int RED_STAR_STONE1 = 18684;
	private final static int RED_STAR_STONE2 = 18685;
	private final static int RED_STAR_STONE3 = 18686;
	
	private final static int BLUE_STAR_STONE1 = 18687;
	private final static int BLUE_STAR_STONE2 = 18688;
	private final static int BLUE_STAR_STONE3 = 18689;
	
	private final static int GREEN_STAR_STONE1 = 18690;
	private final static int GREEN_STAR_STONE2 = 18691;
	private final static int GREEN_STAR_STONE3 = 18692;
	
	private final static int FIRE_ENERGY_COMPRESSION_STONE = 14015;
	private final static int WATER_ENERGY_COMPRESSION_STONE = 14016;
	private final static int WIND_ENERGY_COMPRESSION_STONE = 14017;
	private final static int EARTH_ENERGY_COMPRESSION_STONE = 14018;
	private final static int DARKNESS_ENERGY_COMPRESSION_STONE = 14019;
	private final static int SACRED_ENERGY_COMPRESSION_STONE = 14020;
	
	private final static int SEED_FIRE = 18679;
	private final static int SEED_WATER = 18678;
	private final static int SEED_WIND = 18680;
	private final static int SEED_EARTH = 18681;
	private final static int SEED_DARKNESS = 18683;
	private final static int SEED_DIVINITY = 18682;
	
	private static final SkillType[] SKILL_IDS =
	{
	        SkillType.EXTRACT_STONE
	};

	private int getItemId(int npcId, int skillId)
	{
		switch (npcId)
		{
			case RED_STAR_STONE1 :
			case RED_STAR_STONE2 :
			case RED_STAR_STONE3 :
				if (skillId == EXTRACT_SCROLL_SKILL)
				{
					return EXTRACTED_COARSE_RED_STAR_STONE;
				}
				return EXTRACTED_RED_STAR_STONE;
			case BLUE_STAR_STONE1 :
			case BLUE_STAR_STONE2 :
			case BLUE_STAR_STONE3 :
				if (skillId == EXTRACT_SCROLL_SKILL)
				{
					return EXTRACTED_COARSE_BLUE_STAR_STONE;
				}
				return EXTRACTED_BLUE_STAR_STONE;
			case GREEN_STAR_STONE1 :
			case GREEN_STAR_STONE2 :
			case GREEN_STAR_STONE3 :
				if (skillId == EXTRACT_SCROLL_SKILL)
				{
					return EXTRACTED_COARSE_GREEN_STAR_STONE;
				}
				return EXTRACTED_GREEN_STAR_STONE;
			case SEED_FIRE :
				return FIRE_ENERGY_COMPRESSION_STONE;
			case SEED_WATER :
				return WATER_ENERGY_COMPRESSION_STONE;
			case SEED_WIND :
				return WIND_ENERGY_COMPRESSION_STONE;
			case SEED_EARTH :
				return EARTH_ENERGY_COMPRESSION_STONE;
			case SEED_DARKNESS :
				return DARKNESS_ENERGY_COMPRESSION_STONE;
			case SEED_DIVINITY :
				return SACRED_ENERGY_COMPRESSION_STONE;
			default :
				return 0;
		}
	}
	
	@Override
	public void useSkill(Creature activeChar, Skill skill, GameObject[] targets)
	{
		final Player player = activeChar.getActingPlayer();
		if (player == null)
		{
			return;
		}
		
		for (final Creature target : (Creature[]) targets)
		{
			if (target != null && getItemId(target.getId(), skill.getId()) != 0)
			{
				final double rate = Config.RATE_QUEST_DROP * (player.isInParty() && Config.PREMIUM_PARTY_RATE ? player.getParty().getQuestRewardRate() : player.getPremiumBonus().getQuestRewardRate());
				final long count = skill.getId() == EXTRACT_SCROLL_SKILL ? 1 : Math.min(10, Rnd.get((int) (skill.getLevel() * rate + 1)));
				final int itemId = getItemId(target.getId(), skill.getId());
				
				if (count > 0)
				{
					player.addItem("StarStone", itemId, count, null, true);
					player.sendPacket(SystemMessageId.THE_COLLECTION_HAS_SUCCEEDED);
				}
				else
				{
					player.sendPacket(SystemMessageId.THE_COLLECTION_HAS_FAILED);
				}
				target.doDie(player);
			}
		}
	}

	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}