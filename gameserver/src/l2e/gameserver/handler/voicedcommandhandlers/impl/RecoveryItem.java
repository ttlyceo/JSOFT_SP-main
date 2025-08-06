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
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.instancemanager.ItemRecoveryManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.ItemRecovery;

public class RecoveryItem implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
	        "recovery", "itemRecovery"
	};

	@Override
	public boolean useVoicedCommand(String command, Player player, String args)
	{
		if (!Config.ALLOW_RECOVERY_ITEMS)
		{
			return false;
		}
		
		if (command.equals("recovery"))
		{
			final var itemList = ItemRecoveryManager.getInstance().getAllRemoveItems(player.getObjectId());
			if (itemList == null || itemList.isEmpty())
			{
				player.sendMessage("Your delete item list if empty!");
				return false;
			}
			
			String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/recovetyItem/index.htm");
			final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/mods/recovetyItem/template.htm");
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			
			final String[] param = args.split(" ");
			
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 7;
			int counter = 0;
			
			final boolean isThereNextPage = itemList.size() > perpage;
			
			for (int i = (page - 1) * perpage; i < itemList.size(); i++)
			{
				final ItemRecovery itemRec = itemList.get(i);
				if (itemRec != null)
				{
					block = template;
					
					String name = Util.getItemName(player, itemRec.getItemId());
					if (name.length() > 30)
					{
						name = name.substring(0, 30) + ".";
					}
					
					block = block.replace("{bypass}", "bypass -h .itemRecovery " + itemRec.getObjectId() + "_" + page);
					block = block.replace("{name}", name);
					block = block.replace("{enchant}", itemRec.getEnchantLevel() > 0 ? "+" + itemRec.getEnchantLevel() + "" : "");
					block = block.replace("{count}", String.valueOf(itemRec.getCount()));
					block = block.replace("{time}", String.valueOf(TimeUtils.formatTime(player, (int) ((itemRec.getTime() - System.currentTimeMillis()) / 1000), false)));
					block = block.replace("{icon}", Util.getItemIcon(itemRec.getItemId()));
					list += block;
				}
				
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
			
			final int count = (int) Math.ceil((double) itemList.size() / perpage);
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", Util.getNavigationBlock(count, page, itemList.size(), perpage, isThereNextPage, ".recovery %s"));
			Util.setHtml(html, player);
		}
		else if (command.equals("itemRecovery"))
		{
			if (args == null)
			{
				return false;
			}
			final String[] subStr = args.split(" ")[0].split("_");
			final int objId = Integer.parseInt(subStr[0]);
			final int page = Integer.parseInt(subStr[1]);
			
			if (!ItemRecoveryManager.getInstance().recoveryItem(objId, player))
			{
				player.sendMessage("Item does not belong to you or time has expired!");
			}
			useVoicedCommand("recovery", player, String.valueOf(page));
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}