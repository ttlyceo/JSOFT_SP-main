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
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.communityhandlers.impl.AbstractCommunity;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.handler.voicedcommandhandlers.VoicedCommandHandler;
import l2e.gameserver.instancemanager.DailyRewardManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.daily.DailyRewardTemplate;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.ShowBoard;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by LordWinter
 */
public class DailyReward implements IVoicedCommandHandler
{
	private static final SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy");

	private final String[] _commandList = new String[]
	{
	        "daily", "dailyreward", "getReward", "infoReward"
	};

	@Override
	public boolean useVoicedCommand(final String command, final Player player, final String args)
	{
		if (!Config.ALLOW_DAILY_REWARD)
		{
			return true;
		}

		if (command.equals("daily") || command.equals("dailyreward"))
		{
			showInfo(player);
			return true;
		}
		else if (command.startsWith("getReward"))
		{
			final int number = Integer.parseInt(args.split(" ")[0]);

			final Calendar calendar = Calendar.getInstance();
			if (number > calendar.get(Calendar.DAY_OF_MONTH))
			{
				player.sendMessage("You're trying to get a reward for the next day!");
				return false;
			}

			final DailyRewardTemplate template = DailyRewardManager.getInstance().getDailyReward(number);
			if ((template != null) && !DailyRewardManager.getInstance().isRewardedToday(player))
			{
				DailyRewardManager.getInstance().setRewarded(player);
				for (final int i : template.getRewards().keySet())
				{
					player.addItem("Daily Reward", i, template.getRewards().get(i), null, true);
				}
			}
			player.sendPacket(new ShowBoard());
			checkTime(player);
			return true;
		}
		else if (command.equals("infoReward"))
		{
			final int number = Integer.parseInt(args.split(" ")[0]);
			final DailyRewardTemplate template = DailyRewardManager.getInstance().getDailyReward(number);
			if (template != null)
			{
				showRewards(player, template);
			}
			return true;
		}
		return false;
	}

	public static void showInfo(final Player player)
	{
		final Calendar calendar = Calendar.getInstance();

		final int curMonth = calendar.get(Calendar.MONTH);
		final int curYear = calendar.get(Calendar.YEAR);

		final Calendar dailyCal = (Calendar) Calendar.getInstance().clone();
		dailyCal.set(curYear, curMonth, 1);
		final int curDays = dailyCal.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		if (DailyRewardManager.getInstance().isRewardedToday(player))
		{
			player.sendMessage("You already get a daily reward today!");
			checkTime(player);
			return;
		}

		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyRewards/index.htm");
		html = html.replace("%date%", dateFmt.format(new Date(System.currentTimeMillis())));

		String list = "";
		String block = "";

		int count = 0;
		for (int i = 1; i <= curDays; i++)
		{
			String template = null;
			final DailyRewardTemplate rewardTemplate = DailyRewardManager.getInstance().getDailyReward(i);
			if (rewardTemplate != null)
			{
				template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyRewards/template.htm");

				block = template;
				block = block.replace("%number%", String.valueOf(rewardTemplate.getDay()));

				if (i < DailyRewardManager.getInstance().getRewardDay(player))
				{
					block = block.replace("{background}", "L2UI_CT1_CN.Gray");
					block = block.replace("%image%", "<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"" + getRewardIcon(rewardTemplate) + "\"><tr><td width=32 align=center valign=top><img src=\"L2UI.PETITEM_CLICK\" width=32 height=32></td></tr></table>");
				}
				else if ((i == DailyRewardManager.getInstance().getRewardDay(player)) && (i <= calendar.get(Calendar.DAY_OF_MONTH)))
				{
					block = block.replace("{background}", "L2UI_CT1_CN.Green");
					block = block.replace("%image%", "<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"" + getRewardIcon(rewardTemplate) + "\"><tr><td><button value=\" \" action=\"bypass -h .getReward " + i + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"></td></tr></table>");
				}
				else
				{
					block = block.replace("{background}", "L2UI_CT1_CN.Red");
					block = block.replace("%image%", "<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"" + getRewardIcon(rewardTemplate) + "\"><tr><td><button value=\" \" action=\"bypass -h .infoReward " + i + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"></td></tr></table>");
				}

				count++;
				if ((count == 7) || (count == 14) || (count == 21) || (count == 28))
				{
					if (count == 28 && curDays == count)
					{
						block += "</tr>";
					}
					else
					{
						block += "</tr><tr><td><br></td></tr><tr>";
					}
				}
				list += block;
			}
		}
		html = html.replace("{list}", list);

		checkTime(player);
		AbstractCommunity.separateAndSend(html, player);
	}
	
	private static void checkTime(Player player)
	{
		final long nextReward = DailyRewardManager.getInstance().getRewardTime(player);
		if (DailyRewardManager.getInstance().isRewardedToday(player))
		{
			final long delay = (nextReward - System.currentTimeMillis()) / TimeUnit.SECONDS.toMillis(1L);

			final int hours = (int) (delay / 60 / 60);
			final int mins = (int) ((delay - (hours * 60 * 60)) / 60);
			final int secs = (int) ((delay - ((hours * 60 * 60) + (mins * 60))));

			final String Strhours = hours < 10 ? "0" + hours : "" + hours;
			final String Strmins = mins < 10 ? "0" + mins : "" + mins;
			final String Strsecs = secs < 10 ? "0" + secs : "" + secs;

			player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, "DailyReward", "Next Daily Reward in: " + Strhours + ":" + Strmins + ":" + Strsecs));
			player.sendMessage("You can get next Daily Reward in: " + Strhours + ":" + Strmins + ":" + Strsecs + "");
		}
	}

	public static void showRewards(final Player player, final DailyRewardTemplate rewardTemplate)
	{
		if (rewardTemplate != null)
		{
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyRewards/showRewards.htm");

			String list = "";
			String block = "";

			for (final int itemId : rewardTemplate.getRewards().keySet())
			{
				String template = null;
				template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/dailyRewards/rewards-template.htm");

				block = template;
				block = block.replace("%itemIcon%", ItemsParser.getInstance().getTemplate(itemId).getIcon());
				block = block.replace("%itemName%", String.valueOf(ItemsParser.getInstance().getTemplate(itemId).getName(player.getLang())));
				block = block.replace("%itemCount%", String.valueOf(rewardTemplate.getRewards().get(itemId)));
				list += block;
			}
			html = html.replace("{list}", list);
			Util.setHtml(html, player);
		}
	}

	private static CharSequence getRewardIcon(final DailyRewardTemplate template)
	{
		if (template.getDisplayImage() != "")
		{
			return template.getDisplayImage();
		}
		return "icon.etc_question_mark_i00";
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _commandList;
	}

	public static void main(String[] args)
	{
		if(VoicedCommandHandler.getInstance().getHandler("daily") == null)
			VoicedCommandHandler.getInstance().registerHandler(new DailyReward());
	}
}