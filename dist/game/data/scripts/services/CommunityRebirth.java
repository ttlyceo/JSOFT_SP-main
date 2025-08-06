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

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.AbstractCommunity;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by LordWinter
 */
public class CommunityRebirth extends AbstractCommunity implements ICommunityBoardHandler
{
	// Maximum number of rebirths
	private static int MAX_REBIRT_AMOUNT = 1500;
	
	private static int REBIRT_PER_DAY = 25;
	
	// Item necessary for rebirth
	public static int[] REBIRTH_PRICE =
	{
	        57, 500000000
	};
	
	// SP necessary for rebirth
	public static int REBIRTH_SP = 2000000000;
	
	// List of skills that are given to choose from during rebirth
	public static int[] REBIRTH_SKILL_LIST =
	{
	        10100, 10101, 10102, 10103, 10104, 10105, 10106, 10107, 10108, 10109, 10110, 10111, 10112, 10113, 10114, 10115, 10116, 10117
	};
	
	// price list for 3 buttons
	private static final int[][] _variant1 =
	{
	        {
	                4037, 1
			}
	};
	
	private static final int[][] _variant2 =
	{
	        {
	                1888, 2000
			},
			{
			        1890, 2000
			},
			{
			        5550, 2000
			}
	};
	
	private static final int[][] _variant3 =
	{
	        {
	                9599, 7000
			}
	};
	
	private static final List<Integer> _skillList = new ArrayList<>();
	
	public CommunityRebirth()
	{
		for (final int skillId : REBIRTH_SKILL_LIST)
		{
			_skillList.add(skillId);
		}
		
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Loading all functions.");
		}
	}

	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
		        "_bbsrebirth", "_bbsgetrebirth", "_bbsrebirthSkills", "_bbsrebirthAddSkills", "_bbsresetrebirth"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command, "_");
		final String cmd = st.nextToken();
		
		final int rebirth = player.getVarInt("rebirth", 0);

		if ("bbsrebirth".equals(cmd))
		{
			final int rebirthSkill = player.getVarInt("rebirthSkill", 0);
			if (rebirthSkill > 0)
			{
				onBypassCommand("_bbsrebirthSkills", player);
				return;
			}
			
			final long rebirthTime = player.getVar("rebirthTime") != null ? Long.parseLong(player.getVar("rebirthTime")) : 0;
			int rebirthPerDay = player.getVar("rebirthPerDay") != null ? Integer.parseInt(player.getVar("rebirthPerDay")) : 0;
			if (rebirthTime <= System.currentTimeMillis() && rebirthPerDay > 0)
			{
				player.setVar("rebirthPerDay", 0);
				rebirthPerDay = 0;
			}
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/rebirth/index.htm");
			html = html.replace("%rebirth%", String.valueOf(rebirth));
			html = html.replace("%maxRebirth%", String.valueOf(MAX_REBIRT_AMOUNT));
			if (rebirth < MAX_REBIRT_AMOUNT && player.getLevel() >= 85 && rebirthPerDay < REBIRT_PER_DAY)
			{
				html = html.replace("%button%", "<button value=\"Reincarnate\" action=\"bypass _bbsgetrebirth\" back=\"L2UI_CT1.OlympiadWnd_DF_Fight3None_Down\" width=200 height=30 fore=\"L2UI_CT1.OlympiadWnd_DF_Fight3None\">");
			}
			else
			{
				html = html.replace("%button%", "");
			}
			separateAndSend(html, player);
		}
		else if ("bbsgetrebirth".equals(cmd))
		{
			if (!checkUseCondition(player))
			{
				onBypassCommand("_bbsrebirth", player);
				return;
			}
			
			if (REBIRTH_PRICE[0] > 0)
			{
				player.destroyItemByItemId("Rebirth", REBIRTH_PRICE[0], REBIRTH_PRICE[1], player, true);
			}
			
			if (REBIRTH_SP > 0)
			{
				player.removeExpAndSp(0, REBIRTH_SP);
			}
			
			final long pXp = player.getExp();
			final long tXp = ExperienceParser.getInstance().getExpForLevel(1);
			if (pXp > tXp)
			{
				player.removeExpAndSp(pXp - tXp, 0);
			}
			else if (pXp < tXp)
			{
				player.addExpAndSp(tXp - pXp, 0);
			}
			final int nextRebirth = rebirth + 1;
			player.setVar("rebirth", nextRebirth);
			
			final int rebirthSkill = player.getVarInt("rebirthSkill", 0);
			player.setVar("rebirthSkill", (rebirthSkill + 1));
			
			final int rebirthPerDay = player.getVarInt("rebirthPerDay", 0);
			player.setVar("rebirthPerDay", (rebirthPerDay + 1));
			
			final long rebirthTime = player.getVar("rebirthTime") != null ? Long.parseLong(player.getVar("rebirthTime")) : 0;
			if (rebirthTime <= System.currentTimeMillis())
			{
				final Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.HOUR_OF_DAY, 6);
				cal.set(Calendar.MINUTE, 30);
				player.setVar("rebirthTime", cal.getTimeInMillis());
			}
			
			player.sendMessage("You have successfully reincarnated! Your current reincarnation level is - " + nextRebirth);
			onBypassCommand("_bbsrebirth", player);
		}
		else if ("bbsrebirthSkills".equals(cmd))
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/rebirth/skillList.htm");
			for (int i = 0; i < _skillList.size(); i++)
			{
				final Skill skill = SkillsParser.getInstance().getInfo(_skillList.get(i), SkillsParser.getInstance().getMaxLevel(_skillList.get(i)));
				if (skill != null)
				{
					html = html.replace("%skillName-" + i + "%", skill.getName(player.getLang()));
					html = html.replace("%skillIcon-" + i + "%", skill.getIcon());
					if (player.getKnownSkill(_skillList.get(i)) != null)
					{
						html = html.replace("%curLvl-" + i + "%", String.valueOf(player.getSkillLevel(skill.getId())));
						if (player.getSkillLevel(skill.getId()) < SkillsParser.getInstance().getMaxLevel(skill.getId()))
						{
							html = html.replace("%button-" + i + "%", "<button value=\"" + skill.getName(player.getLang()) + " + 1\" action=\"bypass -h _bbsrebirthAddSkills_" + skill.getId() + "_" + (player.getSkillLevel(skill.getId()) + 1) + "\" width=86 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
						}
						else
						{
							html = html.replace("%button-" + i + "%", "<font color=\"FF0000\">Макс. Ур</font>");
						}
					}
					else
					{
						html = html.replace("%curLvl-" + i + "%", String.valueOf(0));
						html = html.replace("%button-" + i + "%", "<button value=\"" + skill.getName(player.getLang()) + " +1\" action=\"bypass -h _bbsrebirthAddSkills_" + skill.getId() + "_1\" width=86 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					}
					
				}
			}
			separateAndSend(html, player);
		}
		else if ("bbsrebirthAddSkills".equals(cmd))
		{
			final int rebirthSkill = player.getVarInt("rebirthSkill", 0);
			if (rebirthSkill <= 0)
			{
				player.sendMessage("Attempts are over! You need to reincarnate first.");
				onBypassCommand("_bbsrebirth", player);
				return;
			}
			
			final int skillId = Integer.parseInt(st.nextToken());
			final int skillLevel = Integer.parseInt(st.nextToken());
			
			if (!checkSkill(player, skillId, skillLevel))
			{
				Util.handleIllegalPlayerAction(player, "" + player.getName(null) + " try to cheat with learn Rebirth Skill: " + skillId);
				return;
			}
			
			final Skill sk = SkillsParser.getInstance().getInfo(skillId, skillLevel);
			if (sk != null)
			{
				player.setVar("rebirthSkill", (rebirthSkill - 1));
				player.addSkill(sk, true);
				player.sendSkillList(false);
			}
			onBypassCommand("_bbsrebirth", player);
		}
		else if ("bbsresetrebirth".equals(cmd))
		{
			final int varianrId = Integer.parseInt(st.nextToken());
			
			final int[][] requestItems = varianrId == 1 ? _variant1 : varianrId == 2 ? _variant2 : _variant3;
			if (requestItems == null)
			{
				onBypassCommand("_bbsrebirth", player);
				return;
			}
			
			final int rebirthPerDay = player.getVarInt("rebirthPerDay", 0);
			if (rebirthPerDay < REBIRT_PER_DAY)
			{
				player.sendMessage("Function is unavailable, you haven't reached the daily reincarnation limit!");
				return;
			}
			
			for (final int[] itemInfo : requestItems)
			{
				if (player.getInventory().getItemByItemId(itemInfo[0]) == null)
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					onBypassCommand("_bbsrebirth", player);
					return;
				}
				
				if (player.getInventory().getItemByItemId(itemInfo[0]).getCount() < (itemInfo[1]))
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					onBypassCommand("_bbsrebirth", player);
					return;
				}
			}
			
			for (final int[] itemInfo : requestItems)
			{
				player.destroyItemByItemId("Refresh", itemInfo[0], itemInfo[1], player, true);
			}
			
			player.setVar("rebirthPerDay", (rebirthPerDay - 1));
			player.sendMessage("Now you have 1 more reincarnation attempt available.!");
			onBypassCommand("_bbsrebirth", player);
		}
	}
	
	private static boolean checkUseCondition(Player player)
	{
		if (player == null)
		{
			return false;
		}

		if (player.getLevel() < 85)
		{
			player.sendMessage("Your level must be 85! For reincarnation.");
			return false;
		}
		
		if (REBIRTH_SP > 0 && player.getSp() < REBIRTH_SP)
		{
			player.sendMessage("You don't have enough SP for reincarnation.!");
			return false;
		}
		
		if (REBIRTH_PRICE[0] > 0)
		{
			if (player.getInventory().getItemByItemId(REBIRTH_PRICE[0]) == null || player.getInventory().getItemByItemId(REBIRTH_PRICE[0]).getCount() < REBIRTH_PRICE[1])
			{
				player.sendMessage("You need to " + REBIRTH_PRICE[1] + " " + Util.getItemName(player, REBIRTH_PRICE[0]) + " for reincarnation!");
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void onWriteCommand(String command, String s, String s1, String s2, String s3, String s4, Player Player)
	{
	}

	private static boolean checkSkill(Player player, int id, int level)
	{
		for (final int skillId : _skillList)
		{
			if (skillId == id)
			{
				final Skill skill = player.getKnownSkill(id);
				if (skill != null)
				{
					return (skill.getLevel() + 1) == level;
				}
				else
				{
					return level == 1;
				}
			}
		}
		return false;
	}
	
	private static CommunityRebirth _instance = new CommunityRebirth();
	
	public static CommunityRebirth getInstance()
	{
		if (_instance == null)
		{
			_instance = new CommunityRebirth();
		}
		return _instance;
	}

	public static void main(String[] args)
	{
		if(CommunityBoardHandler.getInstance().getHandler("_bbsrebirth") == null)
			CommunityBoardHandler.getInstance().registerHandler(new CommunityRebirth());
	}
}