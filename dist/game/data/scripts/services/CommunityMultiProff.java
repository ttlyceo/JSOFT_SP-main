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
 * this program. If not, see <>.
 */
package services;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.CategoryParser;
import l2e.gameserver.data.parser.SkillTreesParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.AbstractCommunity;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.SkillLearn;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.AcquireSkillType;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

import java.util.*;

/**
 * Created by LordWinter
 */
public class CommunityMultiProff extends AbstractCommunity implements ICommunityBoardHandler
{
	// List of block skills to learn
	private static final int[] MULTIPROFF_BLOCK_SKILLS =
	{
	        842, 841, 840, 14225, 14215, 14213, 14209, 14206, 14202, 14201, 14200, 287, 445, 931, 776, 763, 762, 761, 1440, 1321, 172, 248, 462, 1436, 458, 454, 1557, 783, 782, 781, 780, 779, 181, 334, 1539, 813, 812, 811, 810, 1457, 14228, 14229, 14230, 14231, 14232, 14233, 1016, 113, 118, 134, 163, 194, 438, 1418, 1410, 214, 462, 486, 1177, 1402, 1216, 1322, 2096, 2789, 1320, 1396, 1335, 1258, 1219, 1336, 1337, 455, 1422, 1358, 1359, 1360, 1361, 1554, 1056, 1350, 1351, 1344, 1345, 532, 533, 534, 535, 818, 819, 820, 771, 772, 773, 777, 918, 790, 947, 949, 1468, 786, 914, 788, 948, 1493, 1494, 1506, 1507, 764, 765, 766, 930, 923, 1540, 1543, 919, 924, 1467, 784, 913, 917, 1492, 1496, 760, 774, 929, 946, 945, 755, 756, 757, 758, 759, 1497, 1498, 766, 767, 1405, 239, 1409, 1426, 1305, 1254, 1213, 485, 1427, 794, 1246, 1248, 3865, 1448, 922, 3870, 1505, 1515, 1411, 1413, 1414, 1542, 139, 528, 1357, 1553, 912, 915, 785, 787, 768, 769, 770, 791, 789, 1495, 406, 1303, 536, 1564, 1441, 420, 1444, 176, 1469, 1476, 1477, 1478, 1479, 1355, 1356, 337, 338, 1363, 1365, 341, 349, 94, 483, 1509, 1514, 365, 622, 1532, 1533, 1536, 1537, 1538, 1499, 1500, 1501, 1502, 1503, 1504, 1517, 1518, 1519, 778, 1469, 1470
	};
	
	// Fee for learning skills 1 professions
	private static final int[] MULTIPROFF_1PROFF_PRICE =
	{
	        57, 1
	};
	
	// Fee for learning skills 2 professions
	private static final int[] MULTIPROFF_2PROFF_PRICE =
	{
	        57, 1
	};
	
	// Fee for learning skills 3 professions
	private static final int[] MULTIPROFF_3PROFF_PRICE =
	{
	        57, 1
	};
	
	// Required SP for learning skills 1 professions modifier
	private static final double MULTIPROFF_1PROFF_SP = 10.0;
	private static final double MULTIPROFF_1PROFF_ITEMS = 10.0;
	
	// Required SP for learning skills 2 professions modifier
	private static final double MULTIPROFF_2PROFF_SP = 10.0;
	private static final double MULTIPROFF_2PROFF_ITEMS = 10.0;
	
	// Required SP for learning skills 3 professions modifier
	private static final double MULTIPROFF_3PROFF_SP = 10.0;
	private static final double MULTIPROFF_3PROFF_ITEMS = 10.0;
	
	// List of block skills to remove
	private static final int[] MULTIPROFF_BLOCK_DEL_SKILLS =
	{
			
	};
	
	// Fee for remove skills
	private static final int[] MULTIPROFF_PRICE_DEL_SKILLS =
	{
	        57, 100000
	};
	
	// List of block skills to learn
	private static final int[] MULTIPROFF_BLOCK_SKILLS_BY_LEVEL =
	{
	        
	};
	
	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbsmultiproff", "_bbsmultiskills", "_bbsmultilearnskill", "_bbsmultilearnAllskill", "_bbsmultiremoveskills", "_bbsmultiremoveskillId"
		};
	}

	public CommunityMultiProff()
	{
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command, "_");
		final String cmd = st.nextToken();

		int attackSkillLimit = 0;
		int passiveSkillLimit = 0;
		int triggerSkillLimit = 0;
		if (CategoryParser.getInstance().isInCategory(CategoryType.FIGHTER_GROUP, player.getClassId().getId()))
		{
			attackSkillLimit = 999;
			passiveSkillLimit = 999;
			triggerSkillLimit = 999;
		}
		else
		{
			attackSkillLimit = 999;
			passiveSkillLimit = 999;
			triggerSkillLimit = 999;
		}
		
		if ("bbsmultiproff".equals(cmd))
		{
			String race = null;
			String classLvl = null;
			try
			{
				race = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			try
			{
				classLvl = st.nextToken();
			}
			catch (final Exception e)
			{}
			final int raceId = race != null ? Integer.parseInt(race) : 1;
			final int classLevel = classLvl != null ? Integer.parseInt(classLvl) : 1;
			
			if (player.getClassId().level() < classLevel)
			{
				player.sendMessage("Your level of profession is too low!");
				return;
			}
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/multiproff/index.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/multiproff/template.htm");
			
			String block = "";
			String classes = "";
			
			final ClassId[] array = ClassId.values();
			for (int cId = 0; cId < array.length; cId++)
			{
				final ClassId classId = array[cId];
				if (classId.getRace() != null && classId.getRace().ordinal() == raceId && classId.level() == classLevel)
				{
					if (checkPlayerClasses(player, classId))
					{
						continue;
					}
					block = template;
					block = block.replace("%name%", "<button value=\"" + Util.className(player, classId.getId()) + "\" action=\"bypass -h _bbsmultiskills_" + classId.ordinal() + "_1\" width=200 height=25 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
					classes += block;
				}
			}
			html = html.replace("%classes%", classes);
			html = html.replace("%raceId%", String.valueOf(raceId));
			html = html.replace("%classLvl%", String.valueOf(classLvl));
			html = html.replace("%attackLimit%", String.valueOf(attackSkillLimit));
			html = html.replace("%passiveLimit%", String.valueOf(passiveSkillLimit));
			html = html.replace("%triggerLimit%", String.valueOf(triggerSkillLimit));
			separateAndSend(html, player);
		}
		else if ("bbsmultiskills".equals(cmd))
		{
			final int id = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/multiproff/skills.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/multiproff/skill-template.htm");
			
			String block = "";
			String list = "";
			
			final ClassId classId = ClassId.getClassById(id);
			final int classLevel = classId.level();
			
			final List<SkillLearn> skills = checkPlayerSkills(player, classId);
			if (skills.isEmpty())
			{
				player.sendMessage("Нет доступных навыков!");
				return;
			}
			
			double needSp = 0.;
			double needAmount = 0.;
			int[] needItems = null;
			switch (classLevel)
			{
				case 0 :
				case 1 :
					needSp = MULTIPROFF_1PROFF_SP;
					needItems = MULTIPROFF_1PROFF_PRICE;
					needAmount = MULTIPROFF_1PROFF_ITEMS;
					break;
				case 2 :
					needSp = MULTIPROFF_2PROFF_SP;
					needItems = MULTIPROFF_2PROFF_PRICE;
					needAmount = MULTIPROFF_2PROFF_ITEMS;
					break;
				case 3 :
					needSp = MULTIPROFF_3PROFF_SP;
					needItems = MULTIPROFF_3PROFF_PRICE;
					needAmount = MULTIPROFF_3PROFF_ITEMS;
					break;
			}
			
			final int perpage = 16;
			int counter = 0;
			final int totalSize = skills.size();
			final boolean isThereNextPage = totalSize > perpage;

			int countt = 0;
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final SkillLearn skL = skills.get(i);
				if (skL == null)
				{
					continue;
				}
				
				final Skill sk = SkillsParser.getInstance().getInfo(skL.getId(), skL.getLvl());
				if (sk != null)
				{
					block = template;
					block = block.replace("%name%", sk.getName(player.getLang()));
					block = block.replace("%icon%", sk.getIcon());
					block = block.replace("%level%", String.valueOf(sk.getLevel()));
					block = block.replace("%sp%", getAmountFormat((int) (skL.getLevelUpSp() * needSp), player.getLang()));
					block = block.replace("%itemName%", skL.getRequiredItems(AcquireSkillType.CLASS) != null && !skL.getRequiredItems(AcquireSkillType.CLASS).isEmpty() ? Util.getItemName(player, skL.getRequiredItems(AcquireSkillType.CLASS).get(0).getId()) : needItems != null ? Util.getItemName(player, needItems[0]) : "");
					block = block.replace("%itemAmount%", needItems != null ? getAmountFormat((int) (skL.getLevelUpSp() * needItems[1] * needAmount), player.getLang()) : skL.getRequiredItems(AcquireSkillType.CLASS) != null && !skL.getRequiredItems(AcquireSkillType.CLASS).isEmpty() ? getAmountFormat((int) skL.getRequiredItems(AcquireSkillType.CLASS).get(0).getCount(), player.getLang()) : String.valueOf(0));
					block = block.replace("%bypass%", "_bbsmultilearnskill_" + sk.getId() + "_" + sk.getLevel() + "_" + classLevel + "_" + id + "_" + page);
					block = block.replace("%bypassAll%", "_bbsmultilearnAllskill_" + sk.getId() + "_" + classLevel + "_" + id + "_" + page);
					countt++;
					if (countt % 2 == 0)
					{
						if (countt > 0)
						{
							block += "</tr>";
						}
						block += "<tr>";
					}
					list += block;
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("%list%", list);
			html = html.replace("%navigation%", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "_bbsmultiskills_" + id + "_%s"));
			html = html.replace("%raceId%", String.valueOf(classId.getRace().ordinal()));
			html = html.replace("%classLvl%", String.valueOf(classLevel));
			html = html.replace("%attackLimit%", String.valueOf(attackSkillLimit));
			html = html.replace("%passiveLimit%", String.valueOf(passiveSkillLimit));
			html = html.replace("%triggerLimit%", String.valueOf(triggerSkillLimit));
			separateAndSend(html, player);
		}
		else if ("bbsmultilearnskill".equals(cmd))
		{
			final int skillId = Integer.parseInt(st.nextToken());
			final int skLevel = Integer.parseInt(st.nextToken());
			final int classLevel = Integer.parseInt(st.nextToken());
			final int id = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			if (!SkillTreesParser.getInstance().checkValidClassSkills(skillId, ClassId.getClassById(id)))
			{
				_log.warn("Player " + player.getName(null) + " try to cheat with learn multiProff skill: " + skillId + " & invalid classId!");
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " try to cheat with Community MultiProff!");
				return;
			}

			if (!SkillTreesParser.getInstance().checkLimitClassSkills(player, SkillsParser.getInstance().getInfo(skillId, skLevel), attackSkillLimit, passiveSkillLimit, triggerSkillLimit))
			{
				return;
			}

			if (!SkillTreesParser.getInstance().checkClassesSkill(player, skillId, skLevel))
			{
				_log.warn("Player " + player.getName(null) + " try to cheat with learn multiProff skill: " + skillId);
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " try to cheat with Community MultiProff!");
				return;
			}
			
			if (player.getClassId().level() < classLevel)
			{
				player.sendMessage("Ваш уровень профессии слишком низок!");
				return;
			}
			
			if (ArrayUtils.contains(MULTIPROFF_BLOCK_SKILLS_BY_LEVEL, skillId))
			{
				final List<SkillLearn> skills = checkPlayerSkills(player, ClassId.getClassById(id));
				if (!skills.isEmpty())
				{
					for (final SkillLearn skill : skills)
					{
						if (skill != null && skill.getId() == skillId && skill.getLvl() == skLevel)
						{
							if (skill.getGetLevel() > player.getLevel())
							{
								player.sendMessage("Ваш уровень низкий!");
								return;
							}
						}
					}
				}
			}
			
			SkillLearn learnSkill = null;
			final List<SkillLearn> skills = checkPlayerSkills(player, ClassId.getClassById(id));
			if (!skills.isEmpty())
			{
				for (final SkillLearn skill : skills)
				{
					if (skill != null && skill.getId() == skillId && skill.getLvl() == skLevel)
					{
						learnSkill = skill;
					}
				}
			}
			
			if (learnSkill == null)
			{
				return;
			}
			
			double needSp = 0;
			double needAmount = 0;
			int[] needItems = null;
			switch (classLevel)
			{
				case 0 :
				case 1 :
					needSp = MULTIPROFF_1PROFF_SP;
					needItems = MULTIPROFF_1PROFF_PRICE;
					needAmount = MULTIPROFF_1PROFF_ITEMS;
					break;
				case 2 :
					needSp = MULTIPROFF_2PROFF_SP;
					needItems = MULTIPROFF_2PROFF_PRICE;
					needAmount = MULTIPROFF_2PROFF_ITEMS;
					break;
				case 3 :
					needSp = MULTIPROFF_3PROFF_SP;
					needItems = MULTIPROFF_3PROFF_PRICE;
					needAmount = MULTIPROFF_3PROFF_ITEMS;
					break;
			}
			
			if (player.getSp() < (learnSkill.getLevelUpSp() * needSp))
			{
				player.sendMessage("Не хватает СП!");
				return;
			}
			
			final List<ItemHolder> request = learnSkill.getRequiredItems(AcquireSkillType.CLASS);
			if (request != null)
			{
				for (final ItemHolder items : request)
				{
					if (items != null)
					{
						if (player.getInventory().getItemByItemId(items.getId()) == null || player.getInventory().getItemByItemId(items.getId()).getCount() < items.getCount())
						{
							player.sendMessage("Тебе нужно " + items.getCount() + " " + Util.getItemName(player, items.getId()));
							return;
						}
					}
				}
			}
			
			if (needItems[0] > 0)
			{
				if (player.getInventory().getItemByItemId(needItems[0]) == null || player.getInventory().getItemByItemId(needItems[0]).getCount() < (needItems[1] * learnSkill.getLevelUpSp() * needAmount))
				{
					player.sendMessage("Тебе нужно " + (needItems[1] * learnSkill.getLevelUpSp() * needAmount) + " " + Util.getItemName(player, needItems[0]));
					return;
				}
			}
			
			if (request != null)
			{
				for (final ItemHolder items : request)
				{
					if (items != null)
					{
						player.destroyItemByItemId("MultiProff", items.getId(), items.getCount(), player, true);
					}
				}
			}
			
			if (needItems[0] > 0)
			{
				player.destroyItemByItemId("MultiProff", needItems[0], (long) (needItems[1] * learnSkill.getLevelUpSp() * needAmount), player, true);
			}
			
			player.setSp((int) (player.getSp() - (learnSkill.getLevelUpSp() * needSp)));
			final StatusUpdate su = new StatusUpdate(player);
			su.addAttribute(StatusUpdate.SP, player.getSp());
			player.sendPacket(su);
			
			final Skill sk = SkillsParser.getInstance().getInfo(skillId, skLevel);
			if (sk != null)
			{
				player.addSkill(sk, true);
				player.sendSkillList(false);
				player.updateShortCuts(skillId, skLevel);
			}
			onBypassCommand("_bbsmultiskills_" + id + "_" + page + "", player);
		}
		else if ("bbsmultilearnAllskill".equals(cmd))
		{
			final int skillId = Integer.parseInt(st.nextToken());
			final int classLevel = Integer.parseInt(st.nextToken());
			final int id = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			final List<SkillLearn> skills = SkillTreesParser.getInstance().checkMultiSkill(player, ClassId.getClassById(id), skillId);
			if (skills.isEmpty())
			{
				player.sendMessage("Нет доступного навыка....");
				return;
			}
			
			if (!SkillTreesParser.getInstance().checkValidClassSkills(skillId, ClassId.getClassById(id)))
			{
				_log.warn("Player " + player.getName(null) + " try to cheat with learn multiProff skill: " + skillId + " & invalid classId!");
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " try to cheat with Community MultiProff!");
				return;
			}
			
			if (player.getClassId().level() < classLevel)
			{
				player.sendMessage("Ваш уровень профессии слишком низок!");
				return;
			}
			
			final int playerSkillLevel = player.getKnownSkill(skillId) != null ? player.getKnownSkill(skillId).getLevel() : 0;
			final Map<Integer, Long> items = new HashMap<>();
			long needSp = 0;
			int skillLevel = 0;
			for (final SkillLearn sk : skills)
			{
				if (sk != null && (sk.getLvl() > playerSkillLevel && sk.getGetLevel() <= player.getLevel()))
				{
					if (ArrayUtils.contains(MULTIPROFF_BLOCK_SKILLS_BY_LEVEL, skillId) && sk.getLvl() > playerSkillLevel && sk.getGetLevel() > player.getLevel())
					{
						continue;
					}
					_log.info("skill level " + sk.getLvl());
					int[] needItems = null;
					boolean isCancel = false;
					double spMod = 1;
					double amountMod = 1;
					switch (classLevel)
					{
						case 0 :
						case 1 :
							spMod += MULTIPROFF_1PROFF_SP;
							needItems = MULTIPROFF_1PROFF_PRICE;
							amountMod = MULTIPROFF_1PROFF_ITEMS;
							break;
						case 2 :
							spMod += MULTIPROFF_2PROFF_SP;
							needItems = MULTIPROFF_2PROFF_PRICE;
							amountMod = MULTIPROFF_2PROFF_ITEMS;
							break;
						case 3 :
							spMod += MULTIPROFF_3PROFF_SP;
							needItems = MULTIPROFF_3PROFF_PRICE;
							amountMod = MULTIPROFF_3PROFF_ITEMS;
							break;
					}
					
					needSp += (spMod * sk.getLevelUpSp());
					
					if (needSp > player.getSp())
					{
						needSp -= (spMod * sk.getLevelUpSp());
						isCancel = true;
					}
					
					if (!isCancel)
					{
						final List<ItemHolder> request = sk.getRequiredItems(AcquireSkillType.CLASS);
						if (request != null)
						{
							for (final ItemHolder holder : request)
							{
								if (holder != null)
								{
									if (items.containsKey(holder.getId()))
									{
										final long amount = items.get(holder.getId()) + holder.getCount();
										items.put(holder.getId(), amount);
									}
									else
									{
										items.put(holder.getId(), holder.getCount());
									}
									
									for (final int itemId : items.keySet())
									{
										final long reqItemCount = player.getInventory().getInventoryItemCount(itemId, -1);
										if (reqItemCount < items.get(itemId))
										{
											isCancel = true;
										}
									}
									
									if (isCancel)
									{
										if (items.containsKey(holder.getId()))
										{
											final long amount = items.get(holder.getId()) - holder.getCount();
											if (amount <= 0)
											{
												items.remove(holder.getId());
											}
											else
											{
												items.put(holder.getId(), amount);
											}
										}
									}
								}
							}
						}
						
						if (!isCancel && needItems != null)
						{
							if (items.containsKey(needItems[0]))
							{
								final long amount = (long) (items.get(needItems[0]) + (needItems[1] * sk.getLevelUpSp() * amountMod));
								items.put(needItems[0], amount);
							}
							else
							{
								items.put(needItems[0], (long) (needItems[1] * sk.getLevelUpSp() * amountMod));
							}
							
							for (final int itemId : items.keySet())
							{
								final long reqItemCount = player.getInventory().getInventoryItemCount(itemId, -1);
								if (reqItemCount < items.get(itemId))
								{
									isCancel = true;
								}
							}
							
							if (isCancel)
							{
								if (items.containsKey(needItems[0]))
								{
									final long amount = (long) (items.get(needItems[0]) - (needItems[1] * sk.getLevelUpSp() * amountMod));
									if (amount <= 0)
									{
										items.remove(needItems[0]);
									}
									else
									{
										items.put(needItems[0], amount);
									}
								}
							}
						}
						
						if (!isCancel)
						{
							if (skillLevel < sk.getLvl())
							{
								skillLevel = sk.getLvl();
							}
						}
						else
						{
							break;
						}
					}
				}
			}
			
			if (skillLevel <= 0)
			{
				return;
			}


			if (!SkillTreesParser.getInstance().checkLimitClassSkills(player, SkillsParser.getInstance().getInfo(skillId, skillLevel), attackSkillLimit, passiveSkillLimit, triggerSkillLimit))
			{
				return;
			}
			
			final Skill skill = SkillsParser.getInstance().getInfo(skillId, skillLevel);
			if (skill == null)
			{
				return;
			}
			
			if ((needSp > 0) && (needSp > player.getSp()))
			{
				player.sendMessage("Не хватает СП!");
				return;
			}
			
			if (needSp > 0)
			{
				player.setSp((int) (player.getSp() - needSp));
				final StatusUpdate su = new StatusUpdate(player);
				su.addAttribute(StatusUpdate.SP, player.getSp());
				player.sendPacket(su);
			}
			
			if (!items.isEmpty())
			{
				for (final int itemId : items.keySet())
				{
					final long reqItemCount = player.getInventory().getInventoryItemCount(itemId, -1);
					if (reqItemCount < items.get(itemId))
					{
						player.sendMessage("Тебе нужно " + reqItemCount + " " + Util.getItemName(player, itemId));
						return;
					}
				}
				
				for (final int itemId : items.keySet())
				{
					if (!player.destroyItemByItemId("SkillLearn", itemId, items.get(itemId), player, true))
					{
						Util.handleIllegalPlayerAction(player, "Somehow player " + player.getName(null) + ", level " + player.getLevel() + " lose required item Id: " + itemId + " to learn skill while learning skill Id: " + skillId + " level " + skillLevel + "!");
					}
				}
			}
			
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1);
			sm.addSkillName(skill);
			player.sendPacket(sm);
			
			player.addSkill(skill, true);
			player.sendSkillList(false);
			player.updateShortCuts(skillId, skill.getLevel());
			onBypassCommand("_bbsmultiskills_" + id + "_" + page + "", player);
		}
		else if ("bbsmultiremoveskills".equals(cmd))
		{
			final int page = Integer.parseInt(st.nextToken());
			String race = null;
			String classLvl = null;
			try
			{
				race = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			try
			{
				classLvl = st.nextToken();
			}
			catch (final Exception e)
			{}
			final int raceId = race != null ? Integer.parseInt(race) : 1;
			
			if (player.getSkills().isEmpty())
			{
				return;
			}
			
			final List<Integer> classSkills = SkillTreesParser.getInstance().getAllOriginalClassSkillIdTree(player.getClassId());
			final List<Skill> skills = new ArrayList<>();
			
			for (final Skill skill : player.getSkills().values())
			{
				if (skill != null)
				{
					if (ArrayUtils.contains(MULTIPROFF_BLOCK_DEL_SKILLS, skill.getId()) || skill.isCustom() || classSkills.contains(skill.getId()))
					{
						continue;
					}
					skills.add(skill);
				}
			}
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/multiproff/skillList.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/multiproff/skillList-template.htm");
			String block = "";
			String list = "";
			
			final int perpage = 16;
			int counter = 0;
			final int totalSize = skills.size();
			final boolean isThereNextPage = totalSize > perpage;
			
			int countt = 0;
			for (int i = (page - 1) * perpage; i < totalSize; i++)
			{
				final Skill skill = skills.get(i);
				if (skill != null)
				{
					block = template;
					block = block.replace("%name%", skill.getName(player.getLang()));
					block = block.replace("%icon%", skill.getIcon());
					block = block.replace("%level%", String.valueOf(skill.getLevel()));
					block = block.replace("%itemId%", MULTIPROFF_PRICE_DEL_SKILLS[0] != 0 ? Util.getItemName(player, MULTIPROFF_PRICE_DEL_SKILLS[0]) : "");
					block = block.replace("%amount%", MULTIPROFF_PRICE_DEL_SKILLS[1] != 0 ? String.valueOf(MULTIPROFF_PRICE_DEL_SKILLS[1]) : "");
					block = block.replace("%bypass%", "_bbsmultiremoveskillId_" + skill.getId() + "_" + page + "_" + raceId + "_" + classLvl);
					countt++;
					if (countt % 2 == 0)
					{
						if (countt > 0)
						{
							block += "</tr>";
						}
						block += "<tr>";
					}
					list += block;
				}
				counter++;
				
				if (counter >= perpage)
				{
					break;
				}
			}
			final int count = (int) Math.ceil((double) totalSize / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, totalSize, perpage, isThereNextPage, "_bbsmultiremoveskills_%s_" + raceId + "_" + classLvl));
			html = html.replace("%raceId%", String.valueOf(raceId));
			html = html.replace("%classLvl%", String.valueOf(classLvl));
			separateAndSend(html, player);
		}
		else if ("bbsmultiremoveskillId".equals(cmd))
		{
			final int skillId = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			
			String race = null;
			String classLvl = null;
			try
			{
				race = st.nextToken();
			}
			catch (final Exception e)
			{}
			
			try
			{
				classLvl = st.nextToken();
			}
			catch (final Exception e)
			{}
			final int raceId = race != null ? Integer.parseInt(race) : 1;
			
			final List<Integer> classSkills = SkillTreesParser.getInstance().getAllOriginalClassSkillIdTree(player.getClassId());
			if (classSkills.contains(skillId))
			{
				return;
			}
			
			if (MULTIPROFF_PRICE_DEL_SKILLS[0] > 0)
			{
				if (player.getInventory().getItemByItemId(MULTIPROFF_PRICE_DEL_SKILLS[0]) == null || player.getInventory().getItemByItemId(MULTIPROFF_PRICE_DEL_SKILLS[0]).getCount() < MULTIPROFF_PRICE_DEL_SKILLS[1])
				{
					player.sendMessage("Тебе нужно " + MULTIPROFF_PRICE_DEL_SKILLS[1] + " " + Util.getItemName(player, MULTIPROFF_PRICE_DEL_SKILLS[0]));
					return;
				}
			}
			
			final Skill skill = player.getKnownSkill(skillId);
			if (skill != null)
			{
				if (MULTIPROFF_PRICE_DEL_SKILLS[0] > 0)
				{
					player.destroyItemByItemId("MultiProff", MULTIPROFF_PRICE_DEL_SKILLS[0], MULTIPROFF_PRICE_DEL_SKILLS[1], player, true);
				}
				player.removeSkill(skill, true);
			}
			onBypassCommand("_bbsmultiremoveskills_" + page + "_" + raceId + "_" + classLvl + "", player);
		}
	}
	
	private boolean checkPlayerClasses(Player player, ClassId classId)
	{
		ClassId plClass = null;
		if (player.getClassId().level() > 0)
		{
			plClass = player.getClassId().getParent();
		}
		
		if (classId == player.getClassId() || (plClass != null && classId == plClass) || (player.getClassId().level() == 3 && classId == plClass.getParent()))
		{
			return true;
		}
		
		/*for (final SubClass sub : player.getSubClasses().values())
		{
			if (sub != null)
			{
				final ClassId subId = ClassId.getClassById(sub.getClassId());
				if (subId.level() > 0)
				{
					plClass = subId.getParent();
				}
			
				if (classId == subId || (plClass != null && classId == plClass) || (subId.level() == 3 && classId == plClass.getParent()))
				{
					return true;
				}
			}
		}*/
		return false;
	}
	
	private List<SkillLearn> checkPlayerSkills(Player player, ClassId classId)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final Map<Integer, SkillLearn> skills = SkillTreesParser.getInstance().getAllClassSkillTree(classId);
		for (final SkillLearn skill : skills.values())
		{
			if (skill != null)
			{
				if (ArrayUtils.contains(MULTIPROFF_BLOCK_SKILLS, skill.getId()))
				{
					continue;
				}
				
				if (ArrayUtils.contains(MULTIPROFF_BLOCK_SKILLS_BY_LEVEL, skill.getId()))
				{
					if (skill.getGetLevel() > player.getLevel())
					{
						continue;
					}
				}

				final Skill oldSkill = player.getKnownSkill(skill.getId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getLvl() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getLvl() == 1)
				{
					result.add(skill);
				}
			}
		}
		return result;
	}
	
	private static String getAmountFormat(int damage, String lang)
	{
		final String scount = String.valueOf(damage);
		if (scount.length() < 5)
		{
			return scount;
		}
		
		if (scount.length() == 5 || scount.length() == 6)
		{
			final double price = damage / 1000.0;
			return (price + "" + ServerStorage.getInstance().getString(lang, "EpicDamageInfo.K"));
		}
		else if (scount.length() == 7 || scount.length() == 8 || scount.length() == 9)
		{
			final double price = damage / 1000000.0;
			return (price + "" + ServerStorage.getInstance().getString(lang, "EpicDamageInfo.KK"));
		}
		else
		{
			final double price = damage / 1000000000.0;
			return (price + "" + ServerStorage.getInstance().getString(lang, "EpicDamageInfo.KKK"));
		}
	}

	@Override
	public void onWriteCommand(String command, String s, String s1, String s2, String s3, String s4, Player Player)
	{
	}

	private static CommunityMultiProff _instance = new CommunityMultiProff();
	
	public static CommunityMultiProff getInstance()
	{
		if (_instance == null)
		{
			_instance = new CommunityMultiProff();
		}
		return _instance;
	}

	public static void main(String[] args)
	{
		if(CommunityBoardHandler.getInstance().getHandler("_bbsmultiproff") == null)
			CommunityBoardHandler.getInstance().registerHandler(new CommunityMultiProff());
	}
}