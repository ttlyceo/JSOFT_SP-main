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

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Menu implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "menu", "cfg"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!Config.ALLOW_MENU_COMMAND)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("menu") || command.equalsIgnoreCase("cfg"))
		{
			showConfigMenu(activeChar, null);
		}
		else if (command.startsWith("menu") || command.startsWith("cfg"))
		{
			final String[] params = command.split(" ");
			if ((params.length == 4) && params[1].equalsIgnoreCase("set_var"))
			{
				if (params[2].equalsIgnoreCase("lang@"))
				{
					activeChar.setLang(params[3]);
				}
				else
				{
					if (((params[2].equalsIgnoreCase("visualBlock") && params[3].equalsIgnoreCase("0")) || ((params[2].equalsIgnoreCase("useAutoParty@") || params[2].equalsIgnoreCase("useAutoTrade@")) && params[3].equalsIgnoreCase("1"))))
					{
						if (params[2].equalsIgnoreCase("visualBlock"))
						{
							activeChar.sendConfirmDlg(new AnswerVisualBlock(activeChar), 30000, new ServerMessage("Menu.VISUAL_BLOCK_INFO", activeChar.getLang()).toString());
							return true;
						}
						else
						{
							activeChar.setVar(params[2], params[3]);
							activeChar.sendMessage(Config.PROTECTION.equalsIgnoreCase("NONE") ? new ServerMessage("Menu.SAME_IPS", activeChar.getLang()).toString() : new ServerMessage("Menu.SAME_HWIDS", activeChar.getLang()).toString());
						}
					}
					else
					{
						activeChar.setVar(params[2], params[3]);
						if (params[2].equalsIgnoreCase("useHideTraders@"))
						{
							if (params[3].equalsIgnoreCase("1"))
							{
								activeChar.hidePrivateStores();
							}
							else
							{
								activeChar.restorePrivateStores();
							}
						}
						else if (params[2].equalsIgnoreCase("visualBlock") || params[2].equalsIgnoreCase("useHideTitle@"))
						{
							activeChar.broadcastCharInfoAround();
						}
					}
				}
				showConfigMenu(activeChar, null);
			}
			else if ((params.length == 3) && params[1].equalsIgnoreCase("edit_var"))
			{
				showConfigMenu(activeChar, params[2]);
			}
			else if ((params.length == 3) && (params[1].equalsIgnoreCase("set_logout")))
			{
				String percent = null;
				try
				{
					percent = params[2];
				}
				catch (final Exception e)
				{}
				
				if (percent != null)
				{
					int per = 0;
					try
					{
						per = Integer.parseInt(percent);
						
						if (per > Config.DISCONNECT_TIMEOUT)
						{
							per = Config.DISCONNECT_TIMEOUT;
						}
						
						if (per < 0)
						{
							per = 0;
						}
					}
					catch (final NumberFormatException nfe)
					{
						if (params[1].equalsIgnoreCase("set_logout"))
						{
							per = activeChar.getVarInt("logoutTime", Config.DISCONNECT_TIMEOUT);
						}
					}
					
					if (params[1].equalsIgnoreCase("set_logout"))
					{
						activeChar.setVar("logoutTime", per);
					}
				}
				showConfigMenu(activeChar, null);
			}
			else
			{
				showConfigMenu(activeChar, null);
			}
		}
		return true;
	}
	
	private void showConfigMenu(Player activeChar, String editCmd)
	{
		final var lang = activeChar.getLang();
		String language = "";
		String autoloot = "";
		String autolootHerbs = "";
		String blockExp = "";
		String blockBuffs = "";
		String hideTrades = "";
		String hideBuffs = "";
		String blockTrades = "";
		String blockPartys = "";
		String blockFriends = "";
		String useBlockPartyRecall = "";
		String noCarrier = "";
		String blockVisual = "";
		String shiftclick = "";
		String privateInventory = "";
		String busy = "";
		String skillChance = "";
		String autoParty = "";
		String autoTrade = "";
		String hideTitles = "";
		
		if (Config.MULTILANG_ENABLE && (Config.MULTILANG_ALLOWED.size() > 1))
		{
			String tpl = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/menu/menu-lang-template.htm");
			final String button = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/mods/menu/menu-lang-button.htm");

			var block = "";
			var list = "";

			tpl = tpl.replace("%lang%", lang.toUpperCase());
			if (Config.MULTILANG_VOICED_ALLOW)
			{
				for (final var lng : Config.MULTILANG_ALLOWED)
				{
					if (!lang.equalsIgnoreCase(lng.toString()))
					{
						block = button;
						block = block.replace("%bypass%", "bypass -h voiced_menu set_var lang@ " + lng + "");
						block = block.replace("%lang%", lng.toUpperCase());
						list += block;
					}
				}
				tpl = tpl.replace("%langs%", list);
			}
			language = tpl;
		}
		
		if (Config.ALLOW_AUTOLOOT_COMMAND)
		{
			autoloot += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_AUTOLOOT"), "useAutoLoot@", Config.ALLOW_AUTOLOOT_COMMAND);
		}
		
		if (Config.ALLOW_AUTOLOOT_COMMAND)
		{
			autolootHerbs += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_AUTOLOOT_HERBS"), "useAutoLootHerbs@", Config.ALLOW_AUTOLOOT_COMMAND);
		}
		
		if (Config.DISCONNECT_SYSTEM_ENABLED)
		{
			noCarrier += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_DISCONNECT"), (editCmd != null && editCmd.equals("editLogout")) ? editCmd : null, "editLogout", "logoutTime", Config.DISCONNECT_TIMEOUT);
		}
		shiftclick += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_SHIFTCLICK"), "shiftclick@", true);
		
		if (Config.ALLOW_EXP_GAIN_COMMAND)
		{
			blockExp += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_BLOCK_XP"), "blockedEXP@", Config.ALLOW_EXP_GAIN_COMMAND);
		}
		
		if (Config.ALLOW_BLOCKBUFFS_COMMAND)
		{
			blockBuffs += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_BLOCK_BUFFS"), "useBlockBuffs@", Config.ALLOW_BLOCKBUFFS_COMMAND);
		}
		
		if (Config.ALLOW_HIDE_TRADERS_COMMAND)
		{
			hideTrades += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_HIDE_TRADES"), "useHideTraders@", Config.ALLOW_HIDE_TRADERS_COMMAND);
		}
		
		if (Config.ALLOW_HIDE_BUFFS_ANIMATION_COMMAND)
		{
			hideBuffs += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_HIDE_BUFFS"), "useHideBuffs@", Config.ALLOW_HIDE_BUFFS_ANIMATION_COMMAND);
		}
		
		if (Config.ALLOW_BLOCK_TRADERS_COMMAND)
		{
			blockTrades += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_BLOCK_TRADES"), "useBlockTrade@", Config.ALLOW_BLOCK_TRADERS_COMMAND);
		}
		
		if (Config.ALLOW_BLOCK_PARTY_COMMAND)
		{
			blockPartys += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_BLOCK_PARTYS"), "useBlockParty@", Config.ALLOW_BLOCK_PARTY_COMMAND);
		}
		
		if (Config.ALLOW_BLOCK_FRIEND_COMMAND)
		{
			blockFriends += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_BLOCK_FRIENDS"), "useBlockFriend@", Config.ALLOW_BLOCK_FRIEND_COMMAND);
		}
		blockVisual += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_BLOCK_VISUAL"), "visualBlock", true);
		useBlockPartyRecall += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_PARTY_RECALL"), "useBlockPartyRecall@", true);
		privateInventory += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_PRIVATEINVENTORY"), "privateInv@", true);
		busy += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_BUSY"), "busy@", true);
		
		if (Config.SKILL_CHANCE_SHOW)
		{
			skillChance += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_CHANCE_SKILLS"), "showSkillChance@", Config.SKILL_CHANCE_SHOW);
		}
		
		autoParty += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_AUTO_PARTY"), "useAutoParty@", true);
		autoTrade += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_AUTO_TRADE"), "useAutoTrade@", true);
		hideTitles += getBooleanFrame(activeChar, ServerStorage.getInstance().getString(lang, "Menu.STRING_HIDE_TITLE"), "useHideTitle@", true);
		
		final var htm = new NpcHtmlMessage(6);
		htm.setFile(activeChar, activeChar.getLang(), "data/html/mods/menu/menu.htm");
		htm.replace("%lang%", language.equals("") ? "<tr><td>&nbsp;</td></tr>" : language);
		htm.replace("%autoLoot%", autoloot.equals("") ? "<tr><td>&nbsp;</td></tr>" : autoloot);
		htm.replace("%autoHerbs%", autolootHerbs.equals("") ? "<tr><td>&nbsp;</td></tr>" : autolootHerbs);
		htm.replace("%blockExp%", blockExp.equals("") ? "<tr><td>&nbsp;</td></tr>" : blockExp);
		htm.replace("%blockbuffs%", blockBuffs.equals("") ? "<tr><td>&nbsp;</td></tr>" : blockBuffs);
		htm.replace("%hidetraders%", hideTrades.equals("") ? "<tr><td>&nbsp;</td></tr>" : hideTrades);
		htm.replace("%hidebuffs%", hideBuffs.equals("") ? "<tr><td>&nbsp;</td></tr>" : hideBuffs);
		htm.replace("%blocktrades%", blockTrades.equals("") ? "<tr><td>&nbsp;</td></tr>" : blockTrades);
		htm.replace("%blockpartys%", blockPartys.equals("") ? "<tr><td>&nbsp;</td></tr>" : blockPartys);
		htm.replace("%blockfriends%", blockFriends.equals("") ? "<tr><td>&nbsp;</td></tr>" : blockFriends);
		htm.replace("%blockPartyRecall%", useBlockPartyRecall.equals("") ? "<tr><td>&nbsp;</td></tr>" : useBlockPartyRecall);
		htm.replace("%blockVisual%", blockVisual.equals("") ? "<tr><td>&nbsp;</td></tr>" : blockVisual);
		htm.replace("%noCarrier%", noCarrier.equals("") ? "<tr><td>&nbsp;</td></tr>" : noCarrier);
		htm.replace("%shiftclick%", shiftclick.equals("") ? "<tr><td>&nbsp;</td></tr>" : shiftclick);
		htm.replace("%privateInventory%", privateInventory.equals("") ? "<tr><td>&nbsp;</td></tr>" : privateInventory);
		htm.replace("%busy%", busy.equals("") ? "<tr><td>&nbsp;</td></tr>" : busy);
		htm.replace("%skillChance%", skillChance.equals("") ? "<tr><td>&nbsp;</td></tr>" : skillChance);
		htm.replace("%autoParty%", busy.equals("") ? "<tr><td>&nbsp;</td></tr>" : autoParty);
		htm.replace("%autoTrade%", busy.equals("") ? "<tr><td>&nbsp;</td></tr>" : autoTrade);
		htm.replace("%hideTitles%", busy.equals("") ? "<tr><td>&nbsp;</td></tr>" : hideTitles);
		activeChar.sendPacket(htm);
	}
	
	private static String getBooleanFrame(Player player, String configTitle, String editCmd, String editeVar, String playerEditeVar, int defaultVar)
	{
		String tpl = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/menu/menu-cmd-logout.htm");
		String edit = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/menu/menu-cmd-logout-edit.htm");
		String info = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/menu/menu-cmd-logout-info.htm");
		tpl = tpl.replace("%title%", configTitle);
		if (editCmd != null && !editCmd.isEmpty())
		{
			if (editCmd.equals("editLogout"))
			{
				tpl = tpl.replace("%bypass%", "bypass -h voiced_menu set_logout $editLogout");
				tpl = tpl.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "Menu.SAVE"));
				edit = edit.replace("%editCmd%", editCmd);
				tpl = tpl.replace("%edit%", edit);
			}
		}
		else
		{
			tpl = tpl.replace("%bypass%", "bypass -h voiced_menu edit_var " + editeVar + "");
			tpl = tpl.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "Menu.EDIT"));
			info = info.replace("%info%", "" + player.getVarInt(playerEditeVar, defaultVar) + " " + ServerStorage.getInstance().getString(player.getLang(), "Menu.STRING_SEC") + "");
			tpl = tpl.replace("%edit%", info);
		}
		return tpl;
	}
	
	private String getBooleanFrame(Player activeChar, String configTitle, String configName, boolean allowtomod)
	{
		String tpl = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), configName.equals("privateInv@") ? "data/html/mods/menu/menu-cmd-inv.htm" : "data/html/mods/menu/menu-cmd-template.htm");
		tpl = tpl.replace("%title%", configTitle);
		tpl = tpl.replace("%color%", activeChar.getVarB(configName) ? "00FF00" : "FF5155");
		if (allowtomod)
		{
			if (activeChar.getVarB(configName))
			{
				tpl = tpl.replace("%bypass%", "bypass -h voiced_menu set_var " + configName + " 0");
				tpl = tpl.replace("%name%", ServerStorage.getInstance().getString(activeChar.getLang(), "Menu.STRING_OFF"));
			}
			else
			{
				tpl = tpl.replace("%bypass%", "bypass -h voiced_menu set_var " + configName + " 1");
				tpl = tpl.replace("%name%", ServerStorage.getInstance().getString(activeChar.getLang(), "Menu.STRING_ON"));
			}
		}
		return tpl;
	}
	
	private class AnswerVisualBlock implements OnAnswerListener
	{
		private final Player _player;
		
		private AnswerVisualBlock(Player player)
		{
			_player = player;
		}
		
		@Override
		public void sayYes()
		{
			if (_player != null)
			{
				_player.setVar("visualBlock", 0);
				_player.broadcastCharInfoAround();
				showConfigMenu(_player, null);
			}
		}
		
		@Override
		public void sayNo()
		{
			if (_player != null)
			{
				showConfigMenu(_player, null);
			}
		}
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}