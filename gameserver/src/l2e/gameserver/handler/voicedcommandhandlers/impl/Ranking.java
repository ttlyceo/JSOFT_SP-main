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
package l2e.gameserver.handler.voicedcommandhandlers.impl;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.ranking.PartyTemplate;

public class Ranking implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
	        "rank", "rk", "partyRank"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (!activeChar.isInParty() || !Config.ALLOW_PARTY_RANK_COMMAND)
		{
			return false;
		}
		
		if (activeChar.getParty().getCommandChannel() == null && Config.ALLOW_PARTY_RANK_ONLY_FOR_CC)
		{
			return false;
		}
		
		if (command.equals("rank") || command.equals("rk"))
		{
			if (activeChar.getParty().getCommandChannel() != null)
			{
				showCCInfo(activeChar);
			}
			else
			{
				showPartyInfo(activeChar, activeChar);
			}
		}
		else if (command.startsWith("partyRank"))
		{
			final String[] param = command.split(" ");
			if ((param.length == 2) && param[1] != null)
			{
				final int objectId = Integer.parseInt(param[1]);
				final Player player = GameObjectsStorage.getPlayer(objectId);
				if (player != null)
				{
					showPartyInfo(activeChar, player);
					return true;
				}
				else
				{
					useVoicedCommand("rank", activeChar, params);
					return true;
				}
			}
			else
			{
				useVoicedCommand("rank", activeChar, params);
				return true;
			}
		}
		return true;
	}
	
	public void showCCInfo(Player activeChar)
	{
		final CommandChannel channel = activeChar.getParty().getCommandChannel();
		if (channel == null)
		{
			return;
		}
		
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/ranking/channel_info.htm");
		final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/ranking/channel_template.htm");
		String block = "";
		String list = "";
		
		int totalKills = 0;
		int totalDeaths = 0;
		int count = 0;
		for (final Party pt : channel.getPartys())
		{
			if (pt != null)
			{
				int kills = 0;
				int deaths = 0;
				for (final Player pl : pt.getMembers())
				{
					final PartyTemplate tpl = pt.getMemberRank(pl);
					if (tpl != null)
					{
						kills += tpl.getKills();
						deaths += tpl.getDeaths();
					}
				}
				
				totalKills += kills;
				totalDeaths += deaths;
				
				block = template;
				block = block.replace("{color}", count % 2 == 1 ? "22211d" : "1b1a15");
				block = block.replace("{name}", pt.getLeader().getName(null));
				block = block.replace("{party}", String.valueOf(pt.getMemberCount()));
				block = block.replace("{kills}", String.valueOf(kills));
				block = block.replace("{deaths}", String.valueOf(deaths));
				block = block.replace("{bypass}", "bypass -h voiced_partyRank " + pt.getLeader().getObjectId() + "");
				count++;
				list += block;
			}
		}
		html = html.replace("{list}", list);
		html = html.replace("{totalKills}", String.valueOf(totalKills));
		html = html.replace("{totalDeaths}", String.valueOf(totalDeaths));
		html = html.replace("{leader}", channel.getLeader().getName(null));
		html = html.replace("{partyes}", String.valueOf(channel.getPartys().size()));
		html = html.replace("{members}", String.valueOf(channel.getMembers().size()));
		Util.setHtml(html, activeChar);
	}
	
	public void showPartyInfo(Player player, Player activeChar)
	{
		final Party party = activeChar.getParty();
		if (party == null || party.getMemberCount() < 2)
		{
			useVoicedCommand("rank", player, null);
			return;
		}
		
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/ranking/party_info.htm");
		final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/ranking/party_template.htm");
		String block = "";
		String list = "";
		
		int totalKills = 0;
		int totalDeaths = 0;
		
		int count = 0;
		for (final Player pl : party.getMembers())
		{
			if (pl != null)
			{
				final PartyTemplate tpl = party.getMemberRank(pl);
				if (tpl != null)
				{
					totalKills += tpl.getKills();
					totalDeaths += tpl.getDeaths();
					
					block = template;
					block = block.replace("{color}", count % 2 == 1 ? "22211d" : "1b1a15");
					block = block.replace("{name}", pl.getName(null));
					block = block.replace("{kills}", String.valueOf(tpl.getKills()));
					block = block.replace("{deaths}", String.valueOf(tpl.getDeaths()));
					count++;
					list += block;
				}
			}
		}
		html = html.replace("{list}", list);
		html = html.replace("{totalKills}", String.valueOf(totalKills));
		html = html.replace("{totalDeaths}", String.valueOf(totalDeaths));
		html = html.replace("{leader}", party.getLeader().getName(null));
		html = html.replace("{bypass}", party.getCommandChannel() != null ? "<button value=\"Back\" action=\"bypass -h voiced_rank\" width=\"75\" height=\"26\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" : "&nbsp;");
		html = html.replace("{refresh}", "bypass -h voiced_partyRank " + party.getLeader().getObjectId() + "");
		html = html.replace("{members}", String.valueOf(party.getMemberCount()));
		Util.setHtml(html, player);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}