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

import java.util.StringTokenizer;

import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.PremiumAccountsParser;
import l2e.gameserver.data.parser.PromoCodeParser;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.promocode.PromoCodeTemplate;
import l2e.gameserver.model.actor.templates.promocode.impl.AbstractCodeReward;
import l2e.gameserver.model.actor.templates.promocode.impl.AddLevelCodeReward;
import l2e.gameserver.model.actor.templates.promocode.impl.ExpCodeReward;
import l2e.gameserver.model.actor.templates.promocode.impl.FameCodeReward;
import l2e.gameserver.model.actor.templates.promocode.impl.ItemCodeReward;
import l2e.gameserver.model.actor.templates.promocode.impl.PcPointCodeReward;
import l2e.gameserver.model.actor.templates.promocode.impl.PremiumCodeReward;
import l2e.gameserver.model.actor.templates.promocode.impl.ReputationCodeReward;
import l2e.gameserver.model.actor.templates.promocode.impl.SetLevelCodeReward;
import l2e.gameserver.model.actor.templates.promocode.impl.SpCodeReward;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class PromoCode implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "promo", "promocode", "code", "bonus", "giveCode"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!Config.ALLOW_PROMOCODES_COMMAND)
		{
			return false;
		}
		
		if ("promo".equalsIgnoreCase(command) || "promocode".equalsIgnoreCase(command) || "code".equalsIgnoreCase(command) || "bonus".equalsIgnoreCase(command))
		{
			if (target != null && !(target.isEmpty()))
			{
				final StringTokenizer st = new StringTokenizer(target);
				String code = null;
				if (st.hasMoreTokens())
				{
					code = st.nextToken();
				}
				
				if (!player.checkFloodProtection("PROMOCODE", "promocode_delay"))
				{
					return false;
				}
				
				if (!PromoCodeParser.getInstance().isValidCheckTime(player, false))
				{
					showMainMenu(player);
					return false;
				}

				if (code != null)
				{
					final PromoCodeTemplate tpl = PromoCodeParser.getInstance().getPromoCode(code);
					if (tpl != null)
					{
						if (!PromoCodeParser.getInstance().isActivePromoCode(tpl, player, false))
						{
							showMainMenu(player);
							return false;
						}
						
						String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/promocodes/info.htm");
						final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/promocodes/template.htm");
						String block = "";
						String list = "";
						
						for (final AbstractCodeReward reward : tpl.getRewards())
						{
							block = template;
							block = block.replace("%icon%", reward.getIcon());
							if (reward instanceof AddLevelCodeReward)
							{
								final ServerMessage msg = new ServerMessage("PromoCode.ADD_LEVEL", player.getLang());
								msg.add(((AddLevelCodeReward) reward).getLevel());
								block = block.replace("%data%", msg.toString());
							}
							else if (reward instanceof ExpCodeReward)
							{
								final ServerMessage msg = new ServerMessage("PromoCode.ADD_EXP", player.getLang());
								msg.add(((ExpCodeReward) reward).getExp());
								block = block.replace("%data%", msg.toString());
							}
							else if (reward instanceof ItemCodeReward)
							{
								final ServerMessage msg = new ServerMessage("PromoCode.ADD_ITEM", player.getLang());
								msg.add(ItemsParser.getInstance().getTemplate(((ItemCodeReward) reward).getItemId()).getName(player.getLang()));
								msg.add(((ItemCodeReward) reward).getCount());
								block = block.replace("%data%", msg.toString());
							}
							else if (reward instanceof PremiumCodeReward)
							{
								final ServerMessage msg = new ServerMessage("PromoCode.ADD_PREMIUM", player.getLang());
								msg.add(TimeUtils.formatTime(player, (int) PremiumAccountsParser.getInstance().getPremiumTemplate(((PremiumCodeReward) reward).getPremiumId()).getTime()));
								block = block.replace("%data%", msg.toString());
							}
							else if (reward instanceof SetLevelCodeReward)
							{
								final ServerMessage msg = new ServerMessage("PromoCode.CHANGE_LEVEL", player.getLang());
								msg.add(((SetLevelCodeReward) reward).getLevel());
								block = block.replace("%data%", msg.toString());
							}
							else if (reward instanceof SpCodeReward)
							{
								final ServerMessage msg = new ServerMessage("PromoCode.ADD_SP", player.getLang());
								msg.add(((SpCodeReward) reward).getSp());
								block = block.replace("%data%", msg.toString());
							}
							else if (reward instanceof FameCodeReward)
							{
								final ServerMessage msg = new ServerMessage("PromoCode.ADD_FAME", player.getLang());
								msg.add(((FameCodeReward) reward).getFame());
								block = block.replace("%data%", msg.toString());
							}
							else if (reward instanceof PcPointCodeReward)
							{
								final ServerMessage msg = new ServerMessage("PromoCode.ADD_PC_POINTS", player.getLang());
								msg.add(((PcPointCodeReward) reward).getPcPoints());
								block = block.replace("%data%", msg.toString());
							}
							else if (reward instanceof ReputationCodeReward)
							{
								final ServerMessage msg = new ServerMessage("PromoCode.ADD_REPUTATION", player.getLang());
								msg.add(((ReputationCodeReward) reward).getReputation());
								block = block.replace("%data%", msg.toString());
							}
							list += block;
						}
						
						html = html.replace("%name%", tpl.getName());
						html = html.replace("%list%", list);
						Util.setHtml(html, player);
					}
					else
					{
						showMainMenu(player);
						player.sendMessage((new ServerMessage("PromoCode.WRONG_PROMO", player.getLang())).toString());
						return false;
					}
				}
				else
				{
					showMainMenu(player);
					player.sendMessage((new ServerMessage("PromoCode.WRONG_PROMO", player.getLang())).toString());
					return false;
				}
			}
			else
			{
				showMainMenu(player);
				return true;
			}
		}
		else if ("giveCode".equalsIgnoreCase(command))
		{
			if (target != null && !(target.isEmpty()))
			{
				final StringTokenizer st = new StringTokenizer(target);
				String code = null;
				if (st.hasMoreTokens())
				{
					code = st.nextToken();
				}
				
				if (!player.checkFloodProtection("PROMOCODE", "promocode_delay"))
				{
					return false;
				}
				
				if (!PromoCodeParser.getInstance().isValidCheckTime(player, true))
				{
					return false;
				}
				
				if (code != null)
				{
					final PromoCodeTemplate tpl = PromoCodeParser.getInstance().getPromoCode(code);
					if (tpl != null)
					{
						if (PromoCodeParser.getInstance().isActivePromoCode(tpl, player, true))
						{
							for (final AbstractCodeReward reward : tpl.getRewards())
							{
								reward.giveReward(player);
							}
							player.broadcastPacket(new MagicSkillUse(player, player, 6234, 1, 1000, 0));
						}
					}
				}
			}
		}
		return true;
	}
	
	private void showMainMenu(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile(player, player.getLang(), "data/html/mods/promocodes/index.htm");
		player.sendPacket(html);
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}