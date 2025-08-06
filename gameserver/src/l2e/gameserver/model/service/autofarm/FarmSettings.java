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
package l2e.gameserver.model.service.autofarm;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2e.commons.util.GameSettings;
import l2e.gameserver.Config;

public class FarmSettings
{
	public static boolean ALLOW_ADD_FARM_TIME;
	public static boolean FARM_ONLINE_TYPE;
	public static boolean PREMIUM_FARM_FREE;
	public static boolean ALLOW_AUTO_FARM;
	public static boolean AUTO_FARM_FOR_PREMIUM;
	public static boolean AUTO_FARM_FREE;
	public static Map<Integer, String> AUTO_FARM_PRICES;
	public static int ATTACK_SKILL_CHANCE;
	public static int ATTACK_SKILL_PERCENT;
	public static int CHANCE_SKILL_CHANCE;
	public static int CHANCE_SKILL_PERCENT;
	public static int SELF_SKILL_CHANCE;
	public static int SELF_SKILL_PERCENT;
	public static int HEAL_SKILL_CHANCE;
	public static int HEAL_SKILL_PERCENT;
	public static int SUMMON_ATTACK_SKILL_CHANCE;
	public static int SUMMON_ATTACK_SKILL_PERCENT;
	public static int SUMMON_SELF_SKILL_CHANCE;
	public static int SUMMON_SELF_SKILL_PERCENT;
	public static int SUMMON_HEAL_SKILL_CHANCE;
	public static int SUMMON_HEAL_SKILL_PERCENT;
	public static long SKILLS_EXTRA_DELAY;
	public static long KEEP_LOCATION_DELAY;
	public static long RUN_CLOSE_UP_DELAY;
	public static int RUN_CLOSE_UP_DISTANCE;
	public static int SHORTCUT_PAGE;
	public static int SEARCH_DISTANCE;
	public static int FARM_TYPE;
	public static int FARM_INTERVAL_TASK;
	public static int MAX_SKILLS;
	public static List<Integer> RESURRECTION_ITEMS;
	public static boolean ALLOW_FARM_FREE_TIME;
	public static boolean REFRESH_FARM_TIME;
	public static int FARM_FREE_TIME;
	public static boolean ALLOW_CHECK_HWID_LIMIT;
	public static int FARM_ACTIVE_LIMITS;
	public static int[] FARM_EXPEND_LIMIT_PRICE = new int[2];
	public static int WAIT_TIME;
	public static boolean ALLOW_RESURRECTION;
	public static Map<String, Integer> REGIONS_SEARCH;
	
	protected FarmSettings()
	{
	}
	
	public final void load()
	{
		if (!isAllowSystem())
		{
			return;
		}
		
		final var farmSettings = new GameSettings(false);
		final var file = new File(Config.AUTO_FARM_FILE);
		try (
		    var is = new FileInputStream(file))
		{
			farmSettings.load(is);
		}
		catch (final Exception e)
		{
		}
		
		ALLOW_AUTO_FARM = Boolean.parseBoolean(farmSettings.getProperty("AllowAutoFarm", "False"));
		ALLOW_ADD_FARM_TIME = Boolean.parseBoolean(farmSettings.getProperty("AllowIncreaseFarmTime", "False"));
		FARM_ONLINE_TYPE = Boolean.parseBoolean(farmSettings.getProperty("AutoFarmOnlineType", "False"));
		AUTO_FARM_FOR_PREMIUM = Boolean.parseBoolean(farmSettings.getProperty("AutoFarmOnlyForPremium", "False"));
		AUTO_FARM_FREE = Boolean.parseBoolean(farmSettings.getProperty("AutoFarmIsFree", "False"));
		PREMIUM_FARM_FREE = Boolean.parseBoolean(farmSettings.getProperty("AutoFarmIsFreeForPremium", "False"));
		
		final String[] priceSplits = farmSettings.getProperty("AutoFarmPriceList", "").split(";");
		AUTO_FARM_PRICES = new HashMap<>(priceSplits.length);
		for (final String price : priceSplits)
		{
			final String[] priceSplit = price.split(",");
			if (priceSplit.length == 2)
			{
				try
				{
					AUTO_FARM_PRICES.put(Integer.parseInt(priceSplit[0]), priceSplit[1]);
				}
				catch (final NumberFormatException nfe)
				{}
			}
		}
		ATTACK_SKILL_CHANCE = Integer.parseInt(farmSettings.getProperty("AttackSkillChance", "100"));
		ATTACK_SKILL_PERCENT = Integer.parseInt(farmSettings.getProperty("AttackSkillPercent", "5"));
		CHANCE_SKILL_CHANCE = Integer.parseInt(farmSettings.getProperty("ChanceSkillChance", "100"));
		CHANCE_SKILL_PERCENT = Integer.parseInt(farmSettings.getProperty("ChanceSkillPercent", "5"));
		SELF_SKILL_CHANCE = Integer.parseInt(farmSettings.getProperty("SelfSkillChance", "100"));
		SELF_SKILL_PERCENT = Integer.parseInt(farmSettings.getProperty("SelfSkillPercent", "5"));
		HEAL_SKILL_CHANCE = Integer.parseInt(farmSettings.getProperty("HealSkillChance", "100"));
		HEAL_SKILL_PERCENT = Integer.parseInt(farmSettings.getProperty("HealSkillPercent", "30"));
		SUMMON_ATTACK_SKILL_CHANCE = Integer.parseInt(farmSettings.getProperty("SummonAttackSkillChance", "100"));
		SUMMON_ATTACK_SKILL_PERCENT = Integer.parseInt(farmSettings.getProperty("SummonAttackSkillPercent", "5"));
		SUMMON_SELF_SKILL_CHANCE = Integer.parseInt(farmSettings.getProperty("SummonSelfSkillChance", "100"));
		SUMMON_SELF_SKILL_PERCENT = Integer.parseInt(farmSettings.getProperty("SummonSelfSkillPercent", "5"));
		SUMMON_HEAL_SKILL_CHANCE = Integer.parseInt(farmSettings.getProperty("SummonHealSkillChance", "100"));
		SUMMON_HEAL_SKILL_PERCENT = Integer.parseInt(farmSettings.getProperty("SummonHealSkillPercent", "30"));
		SHORTCUT_PAGE = Integer.parseInt(farmSettings.getProperty("ShortCutPage", "10"));
		SEARCH_DISTANCE = Integer.parseInt(farmSettings.getProperty("SearchDistance", "2000"));
		FARM_TYPE = Integer.parseInt(farmSettings.getProperty("AutoFarmType", "0"));
		FARM_INTERVAL_TASK = Integer.parseInt(farmSettings.getProperty("AutoFarmIntervalTask", "500"));
		SKILLS_EXTRA_DELAY = Long.parseLong(farmSettings.getProperty("SkillsExtraDelay", "5")) * 1000L;
		KEEP_LOCATION_DELAY = Long.parseLong(farmSettings.getProperty("KeepLocationDelay", "5")) * 1000L;
		RUN_CLOSE_UP_DELAY = Long.parseLong(farmSettings.getProperty("RunCloseUpDelay", "2")) * 1000L;
		RUN_CLOSE_UP_DISTANCE = Integer.parseInt(farmSettings.getProperty("RunCloseUpDistance", "100"));
		ALLOW_FARM_FREE_TIME = Boolean.parseBoolean(farmSettings.getProperty("AllowFarmFreeTime", "False"));
		REFRESH_FARM_TIME = Boolean.parseBoolean(farmSettings.getProperty("AllowRefreshFarmTime", "False"));
		FARM_FREE_TIME = Integer.parseInt(farmSettings.getProperty("FarmFreeTime", "3"));
		ALLOW_CHECK_HWID_LIMIT = Boolean.parseBoolean(farmSettings.getProperty("AllowCheckHwidLimits", "False"));
		FARM_ACTIVE_LIMITS = Integer.parseInt(farmSettings.getProperty("FarmActiveLimits", "3"));
		final String[] propertyPrice = farmSettings.getProperty("FarmExpendLimitPrice", "4037,1").split(",");
		try
		{
			FARM_EXPEND_LIMIT_PRICE[0] = Integer.parseInt(propertyPrice[0]);
			FARM_EXPEND_LIMIT_PRICE[1] = Integer.parseInt(propertyPrice[1]);
		}
		catch (final NumberFormatException nfe)
		{}
		MAX_SKILLS = Integer.parseInt(farmSettings.getProperty("FarmMaxSkills", "14"));
		final String[] items = farmSettings.getProperty("RessurectionItemList", "737,3936").split(",");
		RESURRECTION_ITEMS = new ArrayList<>(items.length);
		for (final String item : items)
		{
			Integer itm = 0;
			try
			{
				itm = Integer.parseInt(item);
			}
			catch (final NumberFormatException nfe)
			{}
			
			if (itm != 0)
			{
				RESURRECTION_ITEMS.add(itm);
			}
		}
		WAIT_TIME = Integer.parseInt(farmSettings.getProperty("WaitDelayTime", "15"));
		ALLOW_RESURRECTION = farmSettings.getProperty("AllowRessurection", true);
		final String[] regions = farmSettings.getProperty("AutoFarmRegionSearch", "").split(";");
		REGIONS_SEARCH = new HashMap<>(regions.length);
		for (final String region : regions)
		{
			final String[] regionSplit = region.split(",");
			if (regionSplit.length == 2)
			{
				try
				{
					REGIONS_SEARCH.put(regionSplit[0], Integer.parseInt(regionSplit[1]));
				}
				catch (final NumberFormatException nfe)
				{
				}
			}
		}
	}
	
	private static boolean isAllowSystem()
	{
		return new File(Config.DATAPACK_ROOT + "/data/scripts/services/AutoFarm.java").exists();
	}

	public static final FarmSettings getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final FarmSettings _instance = new FarmSettings();
	}
}