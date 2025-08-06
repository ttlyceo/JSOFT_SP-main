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
package l2e.fake.model;

import java.util.List;

import l2e.commons.util.Rnd;
import l2e.fake.FakePlayer;
import l2e.fake.FakePlayerNameManager;
import l2e.fake.ai.FakePlayerAI;
import l2e.fake.ai.FallbackAI;
import l2e.fake.ai.addon.TraderAI;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.parser.CharTemplateParser;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.FakeArmorParser;
import l2e.gameserver.data.parser.FakeClassesParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.appearance.PcAppearance;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.player.FakeLocTemplate;
import l2e.gameserver.model.actor.templates.player.FakePassiveLocTemplate;
import l2e.gameserver.model.actor.templates.player.FakeTraderTemplate;
import l2e.gameserver.model.actor.templates.player.PcTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.base.Sex;
import l2e.gameserver.model.items.PcItemTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;

public class FakeSupport
{
	public static Class<? extends Creature> getFarmTargetClass()
	{
		return MonsterInstance.class;
	}
	
	public static FakePlayer createRandomFakePlayer(FakeLocTemplate loc)
	{
		final String accountName = "FakePlayer";
		final String playerName = FakePlayerNameManager.getInstance().getRandomAvailableName();
		final int rndLevel = Rnd.get(loc.getMinLvl(), loc.getMaxLvl());
		ClassId classId = null;
		if (rndLevel > 75)
		{
			if (loc.getClasses() != null)
			{
				classId = ClassId.values()[loc.getClasses().get(Rnd.get(0, loc.getClasses().size() - 1))];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getThirdClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getThirdClasses().size() - 1));
			}
		}
		else if (rndLevel > 39 && rndLevel < 76)
		{
			if (loc.getClasses() != null)
			{
				classId = ClassId.values()[loc.getClasses().get(Rnd.get(0, loc.getClasses().size() - 1))];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getSecondClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getSecondClasses().size() - 1));
			}
		}
		else if (rndLevel > 19 && rndLevel < 40)
		{
			if (loc.getClasses() != null)
			{
				classId = ClassId.values()[loc.getClasses().get(Rnd.get(0, loc.getClasses().size() - 1))];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getFirstClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getFirstClasses().size() - 1));
			}
		}
		else if (rndLevel < 20)
		{
			if (loc.getClasses() != null)
			{
				classId = ClassId.values()[loc.getClasses().get(Rnd.get(0, loc.getClasses().size() - 1))];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getBaseClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getBaseClasses().size() - 1));
			}
		}

		final PcTemplate template = CharTemplateParser.getInstance().getTemplate(classId);
		final PcAppearance app = getRandomAppearance(template.getRace(), classId);
		final FakePlayer player = new FakePlayer(IdFactory.getInstance().getNextId(), template, accountName, app);

		player.setGlobalName(playerName);
		player.setAccessLevel(0);
		CharNameHolder.getInstance().addName(player);
		player.setBaseClass(player.getClassId());
		player.setFakePlayer(true);
		setLevel(player, rndLevel);
		player.rewardSkills();
		if (Config.ALLOW_NEW_CHARACTER_TITLE)
		{
			player.getAppearance().setTitleColor(0xFFFF77);
			player.setGlobalTitle(Config.NEW_CHARACTER_TITLE);
		}
		else
		{
			player.setGlobalTitle("");
		}
		checkRndItems(player);
		player.heal();
		return player;
	}
	
	public static FakePlayer createRandomPassiveTrader(FakeTraderTemplate tpl)
	{
		final String accountName = "FakePlayer";
		final String playerName = FakePlayerNameManager.getInstance().getRandomAvailableName();
		final int rndLevel = Rnd.get(tpl.getMinLvl(), tpl.getMaxLvl());
		ClassId classId = null;
		if (rndLevel > 75)
		{
			if (tpl.getClassId() >= 0)
			{
				classId = ClassId.values()[tpl.getClassId()];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getThirdClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getThirdClasses().size() - 1));
			}
		}
		else if (rndLevel > 39 && rndLevel < 76)
		{
			if (tpl.getClassId() >= 0)
			{
				classId = ClassId.values()[tpl.getClassId()];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getSecondClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getSecondClasses().size() - 1));
			}
		}
		else if (rndLevel > 19 && rndLevel < 40)
		{
			if (tpl.getClassId() >= 0)
			{
				classId = ClassId.values()[tpl.getClassId()];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getFirstClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getFirstClasses().size() - 1));
			}
		}
		else if (rndLevel < 20)
		{
			if (tpl.getClassId() >= 0)
			{
				classId = ClassId.values()[tpl.getClassId()];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getBaseClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getBaseClasses().size() - 1));
			}
		}
		
		final PcTemplate template = CharTemplateParser.getInstance().getTemplate(classId);
		final PcAppearance app = getRandomAppearance(template.getRace(), classId);
		final FakePlayer player = new FakePlayer(IdFactory.getInstance().getNextId(), template, accountName, app);
		
		player.setGlobalName(playerName);
		player.setAccessLevel(0);
		CharNameHolder.getInstance().addName(player);
		player.setBaseClass(player.getClassId());
		player.setFakePlayer(true);
		setLevel(player, rndLevel);
		player.rewardSkills();
		if (Config.ALLOW_NEW_CHARACTER_TITLE)
		{
			player.getAppearance().setTitleColor(0xFFFF77);
			player.setGlobalTitle(Config.NEW_CHARACTER_TITLE);
		}
		else
		{
			player.setGlobalTitle("");
		}
		checkRndItems(player);
		player.heal();
		
		return player;
	}
	
	public static FakePlayer createRandomPassiveFakePlayer(FakePassiveLocTemplate loc)
	{
		final String accountName = "FakePlayer";
		final String playerName = FakePlayerNameManager.getInstance().getRandomAvailableName();
		final int rndLevel = Rnd.get(loc.getMinLvl(), loc.getMaxLvl());
		ClassId classId = null;
		if (rndLevel > 75)
		{
			if (loc.getClasses() != null)
			{
				classId = ClassId.values()[loc.getClasses().get(Rnd.get(0, loc.getClasses().size() - 1))];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getThirdClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getThirdClasses().size() - 1));
			}
		}
		else if (rndLevel > 39 && rndLevel < 76)
		{
			if (loc.getClasses() != null)
			{
				classId = ClassId.values()[loc.getClasses().get(Rnd.get(0, loc.getClasses().size() - 1))];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getSecondClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getSecondClasses().size() - 1));
			}
		}
		else if (rndLevel > 19 && rndLevel < 40)
		{
			if (loc.getClasses() != null)
			{
				classId = ClassId.values()[loc.getClasses().get(Rnd.get(0, loc.getClasses().size() - 1))];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getFirstClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getFirstClasses().size() - 1));
			}
		}
		else if (rndLevel < 20)
		{
			if (loc.getClasses() != null)
			{
				classId = ClassId.values()[loc.getClasses().get(Rnd.get(0, loc.getClasses().size() - 1))];
			}
			else
			{
				classId = FakeClassesParser.getInstance().getBaseClasses().get(Rnd.get(0, FakeClassesParser.getInstance().getBaseClasses().size() - 1));
			}
		}
		
		final PcTemplate template = CharTemplateParser.getInstance().getTemplate(classId);
		final PcAppearance app = getRandomAppearance(template.getRace(), classId);
		final FakePlayer player = new FakePlayer(IdFactory.getInstance().getNextId(), template, accountName, app);
		
		player.setGlobalName(playerName);
		player.setAccessLevel(0);
		CharNameHolder.getInstance().addName(player);
		player.setBaseClass(player.getClassId());
		player.setFakePlayer(true);
		setLevel(player, rndLevel);
		player.rewardSkills();
		if (Config.ALLOW_NEW_CHARACTER_TITLE)
		{
			player.getAppearance().setTitleColor(0xFFFF77);
			player.setGlobalTitle(Config.NEW_CHARACTER_TITLE);
		}
		else
		{
			player.setGlobalTitle("");
		}
		checkRndItems(player);
		player.heal();
		
		return player;
	}
	
	private static void checkRndItems(FakePlayer player)
	{
		List<PcItemTemplate> items = null;
		
		if (player.getLevel() < 20)
		{
			items = FakeArmorParser.getInstance().getNgGradeList(player.getClassId());
		}
		else if (player.getLevel() >= 20 && player.getLevel() < 40)
		{
			items = FakeArmorParser.getInstance().getDGradeList(player.getClassId());
		}
		else if (player.getLevel() >= 40 && player.getLevel() < 52)
		{
			items = FakeArmorParser.getInstance().getCGradeList(player.getClassId());
		}
		else if (player.getLevel() >= 52 && player.getLevel() < 61)
		{
			items = FakeArmorParser.getInstance().getBGradeList(player.getClassId());
		}
		else if (player.getLevel() >= 60 && player.getLevel() < 76)
		{
			items = FakeArmorParser.getInstance().getAGradeList(player.getClassId());
		}
		else if (player.getLevel() >= 75 && player.getLevel() < 80)
		{
			items = FakeArmorParser.getInstance().getSGradeList(player.getClassId());
		}
		else if (player.getLevel() >= 80 && player.getLevel() < 84)
		{
			items = FakeArmorParser.getInstance().getS80GradeList(player.getClassId());
		}
		else if (player.getLevel() >= 84)
		{
			items = FakeArmorParser.getInstance().getS84GradeList(player.getClassId());
		}
		
		if (items != null)
		{
			for (final PcItemTemplate ie : items)
			{
				final ItemInstance item = player.getInventory().addItem("Items", ie.getId(), ie.getCount(), player, null);
				if (item.isEquipable())
				{
					if (player.getLevel() > 19)
					{
						if (item.isWeapon() && Config.ALLOW_ENCHANT_WEAPONS)
						{
							item.setEnchantLevel(Rnd.get(Config.RND_ENCHANT_WEAPONS[0], Config.RND_ENCHANT_WEAPONS[1]));
						}
						else if (item.isArmor() && Config.ALLOW_ENCHANT_ARMORS)
						{
							item.setEnchantLevel(Rnd.get(Config.RND_ENCHANT_ARMORS[0], Config.RND_ENCHANT_ARMORS[1]));
						}
						else if (item.isJewel() && Config.ALLOW_ENCHANT_JEWERLYS)
						{
							item.setEnchantLevel(Rnd.get(Config.RND_ENCHANT_JEWERLYS[0], Config.RND_ENCHANT_JEWERLYS[1]));
						}
					}
					player.getInventory().equipItem(item);
				}
			}
		}
	}

	public static PcAppearance getRandomAppearance(Race race, ClassId clazz)
	{
		final int randomSex = Rnd.get(1, 2) == 1 ? Sex.MALE.ordinal() : Sex.FEMALE.ordinal();
		final boolean female = randomSex != Sex.MALE.ordinal();
		final int hairStyle = Rnd.get(0, randomSex == Sex.MALE.ordinal() ? 4 : 6);
		final int hairColor = Rnd.get(0, 3);
		final int faceId = Rnd.get(0, 2);
		return new PcAppearance((byte) faceId, (byte) hairColor, (byte) hairStyle, female);
	}

	public static void setLevel(FakePlayer player, int level)
	{
		if (level >= 1 && level <= ExperienceParser.getInstance().getMaxLevel())
		{
			final long pXp = player.getExp();
			final long tXp = ExperienceParser.getInstance().getExpForLevel(level);

			if (pXp > tXp)
			{
				player.removeExpAndSp(pXp - tXp, 0);
			}
			else if (pXp < tXp)
			{
				player.addExpAndSp(tXp - pXp, 0);
			}
		}
	}

	public static Class<? extends FakePlayerAI> getAIbyClassId(ClassId classId, boolean isPassive)
	{
		if (isPassive)
		{
			return FallbackAI.class;
		}
		
		final Class<? extends FakePlayerAI> ai = FakeClassesParser.getInstance().getAllAIs().get(classId);
		if (ai == null)
		{
			return FallbackAI.class;
		}
		return ai;
	}
	
	public static Class<? extends FakePlayerAI> getAIbyTrader()
	{
		return TraderAI.class;
	}
}