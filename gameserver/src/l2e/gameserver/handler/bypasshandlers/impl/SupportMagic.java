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
package l2e.gameserver.handler.bypasshandlers.impl;

import l2e.gameserver.data.parser.CategoryParser;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;

public class SupportMagic implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
	        "supportmagicservitor", "supportmagic"
	};

	private static final SkillHolder HASTE_1 = new SkillHolder(4327, 1);
	private static final SkillHolder HASTE_2 = new SkillHolder(5632, 1);
	private static final SkillHolder CUBIC = new SkillHolder(4338, 1);

	private static final SkillHolder[] FIGHTER_BUFFS =
	{
	        new SkillHolder(4322, 1), new SkillHolder(4323, 1), new SkillHolder(5637, 1), new SkillHolder(4324, 1), new SkillHolder(4325, 1), new SkillHolder(4326, 1),
	};
	private static final SkillHolder[] MAGE_BUFFS =
	{
	        new SkillHolder(4322, 1), new SkillHolder(4323, 1), new SkillHolder(5637, 1), new SkillHolder(4328, 1), new SkillHolder(4329, 1), new SkillHolder(4330, 1), new SkillHolder(4331, 1),
	};
	private static final SkillHolder[] SUMMON_BUFFS =
	{
	        new SkillHolder(4322, 1), new SkillHolder(4323, 1), new SkillHolder(5637, 1), new SkillHolder(4324, 1), new SkillHolder(4325, 1), new SkillHolder(4326, 1), new SkillHolder(4328, 1), new SkillHolder(4329, 1), new SkillHolder(4330, 1), new SkillHolder(4331, 1),
	};

	private static final int LOWEST_LEVEL = 6;
	private static final int HIGHEST_LEVEL = 75;
	private static final int CUBIC_LOWEST = 16;
	private static final int CUBIC_HIGHEST = 34;
	private static final int HASTE_LEVEL_2 = 40;

	@Override
	public boolean useBypass(String command, Player activeChar, Creature target)
	{
		if (!target.isNpc() || activeChar.isCursedWeaponEquipped())
		{
			return false;
		}
		
		if (!activeChar.checkFloodProtection("BUFFSDELAY", "buffs_delay"))
		{
			return false;
		}

		if (command.equalsIgnoreCase(COMMANDS[0]))
		{
			makeSupportMagic(activeChar, (Npc) target, true);
		}
		else if (command.equalsIgnoreCase(COMMANDS[1]))
		{
			makeSupportMagic(activeChar, (Npc) target, false);
		}
		return true;
	}

	private static void makeSupportMagic(Player player, Npc npc, boolean isSummon)
	{
		final int level = player.getLevel();
		if (isSummon && !player.hasServitor())
		{
			npc.showChatWindow(player, "data/html/default/SupportMagicNoSummon.htm");
			return;
		}
		else if (level > HIGHEST_LEVEL)
		{
			npc.showChatWindow(player, "data/html/default/SupportMagicHighLevel.htm");
			return;
		}
		else if (level < LOWEST_LEVEL)
		{
			npc.showChatWindow(player, "data/html/default/SupportMagicLowLevel.htm");
			return;
		}
		else if (player.getClassId().level() == 3)
		{
			player.sendMessage("Only adventurers who have not completed their 3rd class transfer may receive these buffs.");
			return;
		}

		if (isSummon)
		{
			npc.setTarget(player.getSummon());
			for (final SkillHolder skill : SUMMON_BUFFS)
			{
				npc.doCast(skill.getSkill());
			}

			if (level >= HASTE_LEVEL_2)
			{
				npc.doCast(HASTE_2.getSkill());
			}
			else
			{
				npc.doCast(HASTE_1.getSkill());
			}
		}
		else
		{
			npc.setTarget(player);
			if (CategoryParser.getInstance().isInCategory(CategoryType.BEGINNER_MAGE, player.getClassId().getId()))
			{
				for (final SkillHolder skill : MAGE_BUFFS)
				{
					npc.doCast(skill.getSkill());
				}
			}
			else
			{
				for (final SkillHolder skill : FIGHTER_BUFFS)
				{
					npc.doCast(skill.getSkill());
				}

				if (level >= HASTE_LEVEL_2)
				{
					npc.doCast(HASTE_2.getSkill());
				}
				else
				{
					npc.doCast(HASTE_1.getSkill());
				}
			}

			if ((level >= CUBIC_LOWEST) && (level <= CUBIC_HIGHEST))
			{
				player.doSimultaneousCast(CUBIC.getSkill());
			}
		}
	}

	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}