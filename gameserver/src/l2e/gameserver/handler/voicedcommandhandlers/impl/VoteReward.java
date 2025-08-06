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

import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.VoteRewardParser;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.Dummy_7D;
import l2e.gameserver.network.serverpackets.Dummy_7D.ServerRequest;
import l2e.gameserver.network.serverpackets.Dummy_8D;

public class VoteReward implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "vote", "voteReward"
	};

	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		final var instance = VoteRewardParser.getInstance();
		if (command.equalsIgnoreCase("vote"))
		{
			final var sites = instance.getVoteRewardSites();
			if (sites == null || sites.isEmpty())
			{
				return false;
			}
			
			if (player.getLevel() < instance.getMinLevel())
			{
				player.sendMessage(new ServerMessage("VoteReward.LOW_LEVEL", player.getLang()).toString());
				return false;
			}
			
			if (instance.isFullCheckSites())
			{
				if (!player.checkFloodProtection("VOTE", "vote_delay"))
				{
					return false;
				}
				boolean received = false;
				for (final var site : sites.values())
				{
					if (site.isEnabled())
					{
						if (site.tryGiveRewards(player))
						{
							received = true;
						}
					}
				}
				
				if (!received)
				{
					player.sendMessage(new ServerMessage("VoteReward.NOT_HAVE_VOTES", player.getLang()).toString());
				}
			}
			else
			{
				String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/vote/index.htm");
				final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/vote/template.htm");
				String block = "";
				String list = "";
				
				for (final var site : sites.values())
				{
					final var record = site.getRecord(player);
					final long lastVoteTime = (record.getLastVoteTime() * 1000L);
					final long nextVote = lastVoteTime + site.getVoteDelay();
					
					block = template;
					block = block.replace("%name%", site.getName(player.getLang()));
					block = block.replace("%link%", "bypass -h .voteReward " + site.getName() + "");
					block = block.replace("%voteTime%", nextVote > System.currentTimeMillis() ? TimeUtils.formatTime(player, (int) ((nextVote - System.currentTimeMillis()) / 1000), false) : "-");
					list += block;
				}
				html = html.replace("%list%", list);
				Util.setHtml(html, player);
			}
		}
		else if (command.startsWith("voteReward"))
		{
			final String name = target.split(" ")[0];
			if (name == null || !player.checkFloodProtection("VOTE", "vote_delay"))
			{
				return false;
			}
			final var voteSite = instance.getVoteRewardSites().get(name);
			if (voteSite != null && voteSite.isEnabled() && voteSite.getVoteDelay() > 0)
			{
				if (!voteSite.isValidAttempt(player))
				{
					final var msg = new ServerMessage("VoteReward.DELAY_VOTE", player.getLang());
					msg.add(voteSite.getDelay() / 1000);
					player.sendMessage(msg.toString());
					return false;
				}
				
				final var record = voteSite.getRecord(player);
				final long lastVoteTime = (record.getLastVoteTime() * 1000L);
				final long nextVote = lastVoteTime + voteSite.getVoteDelay();
				if (nextVote > System.currentTimeMillis())
				{
					player.sendMessage(new ServerMessage("VoteReward.ALREADY_VOTE", player.getLang()).toString());
					return false;
				}
				
				if (!voteSite.tryGiveRewards(player))
				{
					if (voteSite.getVoteLink() != null && !voteSite.getVoteLink().isEmpty())
					{
						if (Config.PROTECTION.equalsIgnoreCase("STRIX"))
						{
							player.sendPacket(new Dummy_7D(voteSite.getVoteLink(), ServerRequest.SC_SERVER_REQUEST_OPEN_URL));
						}
						else
						{
							player.sendPacket(new Dummy_8D(voteSite.getVoteLink()));
						}
					}
					player.sendMessage(new ServerMessage("VoteReward.NOT_VOTE", player.getLang()).toString());
				}
			}
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}
