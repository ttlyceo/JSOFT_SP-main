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
package l2e.scripts.custom;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.scripts.ai.AbstractNpcAI;

public class MonumentOfHeroes extends AbstractNpcAI
{
	private static final int[] MONUMENTS =
	{
	        31690, 31769, 31770, 31771, 31772
	};
	
	private static final int WINGS_OF_DESTINY_CIRCLET = 6842;
	private static final int[] WEAPONS =
	{
	        6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390,
	};

	private MonumentOfHeroes(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(MONUMENTS);
		addTalkId(MONUMENTS);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "HeroWeapon" :
			{
				if (player.isTimeHero())
				{
					return "no_hero_weapon.htm";
				}
				
				if (player.isHero())
				{
					return hasAtLeastOneQuestItem(player, WEAPONS) ? "already_have_weapon.htm" : "weapon_list.htm";
				}
				return "no_hero_weapon.htm";
			}
			case "HeroCirclet" :
			{
				if (player.isTimeHero())
				{
					return "no_hero_circlet.htm";
				}
				
				if (player.isHero())
				{
					if (!hasQuestItems(player, WINGS_OF_DESTINY_CIRCLET))
					{
						giveItems(player, WINGS_OF_DESTINY_CIRCLET, 1);
					}
					else
					{
						return "already_have_circlet.htm";
					}
				}
				else
				{
					return "no_hero_circlet.htm";
				}
				break;
			}
			default :
			{
				if (player.isTimeHero())
				{
					return "no_hero_weapon.htm";
				}
				
				final int weaponId = Integer.parseInt(event);
				if (ArrayUtils.contains(WEAPONS, weaponId))
				{
					giveItems(player, weaponId, 1);
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}

	public static void main(String[] args)
	{
		new MonumentOfHeroes(MonumentOfHeroes.class.getSimpleName(), "custom");
	}
}