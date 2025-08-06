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
package l2e.gameserver.model.actor.instance;

import java.util.List;

import l2e.commons.util.Rnd;
import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ClassMasterParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.ClassMasterTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.ShowTutorialMark;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.TutorialCloseHtml;
import l2e.gameserver.network.serverpackets.TutorialShowHtml;

public final class ClassMasterInstance extends MerchantInstance
{
	public ClassMasterInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		
		setInstanceType(InstanceType.ClassMasterInstance);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/classmaster/" + pom + ".htm";
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!ClassMasterParser.getInstance().isAllowClassMaster())
		{
			return;
		}
		
		if (command.startsWith("1stClass"))
		{
			showHtmlMenu(player, getObjectId(), 1);
		}
		else if (command.startsWith("2ndClass"))
		{
			showHtmlMenu(player, getObjectId(), 2);
		}
		else if (command.startsWith("3rdClass"))
		{
			showHtmlMenu(player, getObjectId(), 3);
		}
		else if (command.startsWith("change_class"))
		{
			final int val = Integer.parseInt(command.substring(13));
			
			if (checkAndChangeClass(player, val))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/classmaster/ok.htm");
				html.replace("%name%", Util.className(player, val));
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("become_noble"))
		{
			if (!player.isNoble())
			{
				if (player.getInventory().getItemByItemId(Config.SERVICES_GIVENOOBLESS_ITEM[0]) == null)
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				if (player.getInventory().getItemByItemId(Config.SERVICES_GIVENOOBLESS_ITEM[0]).getCount() < Config.SERVICES_GIVENOOBLESS_ITEM[1])
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
				player.destroyItemByItemId("ShopBBS", Config.SERVICES_GIVENOOBLESS_ITEM[0], Config.SERVICES_GIVENOOBLESS_ITEM[1], player, true);
				Olympiad.addNoble(player);
				player.setNoble(true);
				if (player.getClan() != null)
				{
					player.setPledgeClass(ClanMember.calculatePledgeClass(player));
				}
				else
				{
					player.setPledgeClass(5);
				}
				player.sendUserInfo();
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/classmaster/nobleok.htm");
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("learn_skills"))
		{
			player.giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true);
		}
		else if (command.startsWith("clan_level_up"))
		{
			if (!player.isClanLeader())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/classmaster/noclanleader.htm");
				player.sendPacket(html);
			}
			else if (player.getClan().getLevel() >= 5)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/classmaster/noclanlevel.htm");
				player.sendPacket(html);
			}
			else
			{
				player.getClan().changeLevel(5, true);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public static final void onTutorialLink(Player player, String request)
	{
		if (!Config.ALTERNATE_CLASS_MASTER || (request == null) || !request.startsWith("CO"))
		{
			return;
		}
		
		try
		{
			final int val = Integer.parseInt(request.substring(2));
			checkAndChangeClass(player, val);
		}
		catch (final NumberFormatException e)
		{}
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
	}
	
	public static final void onTutorialQuestionMark(Player player, int number)
	{
		if (!Config.ALTERNATE_CLASS_MASTER || (number != 1001))
		{
			return;
		}
		
		showTutorialHtml(player);
	}
	
	public static final void showQuestionMark(Player player)
	{
		if (!Config.ALTERNATE_CLASS_MASTER)
		{
			return;
		}
		
		final ClassId classId = player.getClassId();
		if (getMinLevel(classId.level()) > player.getLevel())
		{
			return;
		}
		
		if (ClassMasterParser.getInstance().isAllowClassMaster())
		{
			if (!ClassMasterParser.getInstance().isAllowedClassChange(classId.level() + 1))
			{
				return;
			}
		}
		player.sendPacket(new ShowTutorialMark(1001, 0));
	}
	
	private static final void showHtmlMenu(Player player, int objectId, int level)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		int val = -1;
		if (!ClassMasterParser.getInstance().isAllowClassMaster())
		{
			html.setFile(player, player.getLang(), "data/html/classmaster/disabled.htm");
			player.sendPacket(html);
			return;
		}
		
		final ClassMasterTemplate tpl = ClassMasterParser.getInstance().getClassTemplate(level);
		if (tpl == null)
		{
			html.setFile(player, player.getLang(), "data/html/classmaster/disabled.htm");
			player.sendPacket(html);
			return;
		}
		
		if (!tpl.isAllowedChangeClass())
		{
			final int jobLevel = player.getClassId().level();
			final StringBuilder sb = new StringBuilder(100);
			final ClassMasterTemplate tpl1 = ClassMasterParser.getInstance().getClassTemplate(1);
			final ClassMasterTemplate tpl2 = ClassMasterParser.getInstance().getClassTemplate(2);
			final ClassMasterTemplate tpl3 = ClassMasterParser.getInstance().getClassTemplate(3);
			sb.append("<html><body>");
			switch (jobLevel)
			{
				case 0 :
					if (tpl1 != null && tpl1.isAllowedChangeClass())
					{
						sb.append("Come back here when you reached level 20 to change your class.<br>");
					}
					else if (tpl2 != null && tpl2.isAllowedChangeClass())
					{
						sb.append("Come back after your first occupation change.<br>");
					}
					else if (tpl3 != null && tpl3.isAllowedChangeClass())
					{
						sb.append("Come back after your second occupation change.<br>");
					}
					else
					{
						sb.append("I can't change your occupation.<br>");
					}
					break;
				case 1 :
					if (tpl2 != null && tpl2.isAllowedChangeClass())
					{
						sb.append("Come back here when you reached level 40 to change your class.<br>");
					}
					else if (tpl3 != null && tpl3.isAllowedChangeClass())
					{
						sb.append("Come back after your second occupation change.<br>");
					}
					else
					{
						sb.append("I can't change your occupation.<br>");
					}
					break;
				case 2 :
					if (tpl3 != null && tpl3.isAllowedChangeClass())
					{
						sb.append("Come back here when you reached level 76 to change your class.<br>");
					}
					else
					{
						sb.append("I can't change your occupation.<br>");
					}
					break;
				case 3 :
					sb.append("There is no class change available for you anymore.<br>");
					break;
			}
			sb.append("</body></html>");
			html.setHtml(player, sb.toString());
		}
		else
		{
			final ClassId currentClassId = player.getClassId();
			if (currentClassId.level() >= level)
			{
				html.setFile(player, player.getLang(), "data/html/classmaster/nomore.htm");
			}
			else
			{
				final int minLevel = getMinLevel(currentClassId.level());
				if ((player.getLevel() >= minLevel) || Config.ALLOW_ENTIRE_TREE)
				{
					final StringBuilder menu = new StringBuilder(100);
					for (final ClassId cid : ClassId.values())
					{
						if ((cid == ClassId.inspector) && (player.getTotalSubClasses() < 2))
						{
							continue;
						}
						if (validateClassId(currentClassId, cid) && (cid.level() == level))
						{
							val = cid.getId();
							StringUtil.append(menu, "<tr><td width=280 align=center><button value = \"" + Util.className(player, cid.getId()) + "\" action=\"bypass -h npc_%objectId%_change_class ", String.valueOf(cid.getId()), "\" back=\"l2ui_ct1.button.OlympiadWnd_DF_Back_Down\" width=200 height=30 fore=\"l2ui_ct1.button.OlympiadWnd_DF_Back\"></td></tr>");
						}
					}
					
					if (menu.length() > 0)
					{
						html.setFile(player, player.getLang(), "data/html/classmaster/template.htm");
						html.replace("%name%", Util.className(player, currentClassId.getId()));
						html.replace("%menu%", menu.toString());
					}
					else
					{
						html.setFile(player, player.getLang(), "data/html/classmaster/comebacklater.htm");
						html.replace("%level%", String.valueOf(getMinLevel(level - 1)));
					}
				}
				else
				{
					if (minLevel < Integer.MAX_VALUE)
					{
						html.setFile(player, player.getLang(), "data/html/classmaster/comebacklater.htm");
						html.replace("%level%", String.valueOf(minLevel));
					}
					else
					{
						html.setFile(player, player.getLang(), "data/html/classmaster/nomore.htm");
					}
				}
			}
		}
		html.replace("%objectId%", String.valueOf(objectId));
		html.replace("%req_items%", getRequiredItems(player, level));
		html.replace("%rew_items%", getRewardItems(player, level, val));
		player.sendPacket(html);
	}
	
	private static final void showTutorialHtml(Player player)
	{
		final ClassId currentClassId = player.getClassId();
		if ((getMinLevel(currentClassId.level()) > player.getLevel()) && !Config.ALLOW_ENTIRE_TREE)
		{
			return;
		}
		
		String msg = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/classmaster/tutorialtemplate.htm");
		msg = msg.replaceAll("%name%", Util.className(player, currentClassId.getId()));
		int val = -1;
		final StringBuilder menu = new StringBuilder(100);
		for (final ClassId cid : ClassId.values())
		{
			if ((cid == ClassId.inspector) && (player.getTotalSubClasses() < 2))
			{
				continue;
			}
			if (validateClassId(currentClassId, cid))
			{
				if (cid.level() == currentClassId.level() + 1)
				{
					val = cid.getId();
				}
				StringUtil.append(menu, "<tr><td width=280 align=center><button value = \"" + Util.className(player, cid.getId()) + "\" action=\"link CO" + String.valueOf(cid.getId()) + "\" back=\"l2ui_ct1.button.OlympiadWnd_DF_Back_Down\" width=200 height=30 fore=\"l2ui_ct1.button.OlympiadWnd_DF_Back\"></td></tr>");
			}
		}
		msg = msg.replaceAll("%menu%", menu.toString());
		msg = msg.replace("%req_items%", getRequiredItems(player, currentClassId.level() + 1));
		msg = msg.replace("%rew_items%", getRewardItems(player, currentClassId.level() + 1, val));
		player.sendPacket(new TutorialShowHtml(msg));
	}
	
	private static final boolean checkAndChangeClass(Player player, int val)
	{
		final ClassId currentClassId = player.getClassId();
		if ((getMinLevel(currentClassId.level()) > player.getLevel()) && !Config.ALLOW_ENTIRE_TREE)
		{
			return false;
		}
		
		if (!validateClassId(currentClassId, val))
		{
			return false;
		}
		
		final int newJobLevel = currentClassId.level() + 1;
		ClassMasterTemplate tpl = ClassMasterParser.getInstance().getClassTemplate(newJobLevel);
		if (tpl == null)
		{
			return false;
		}
		
		if (!tpl.getRewardItems().isEmpty() && !player.isInventoryUnder90(false))
		{
			player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
			return false;
		}
		
		if (!tpl.getRequestItems().isEmpty())
		{
			final List<ItemHolder> requestList = tpl.getRequestItems();
			for (final ItemHolder holder : requestList)
			{
				if (holder != null)
				{
					if (player.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getCount())
					{
						player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
						return false;
					}
				}
			}
			
			for (final ItemHolder holder : requestList)
			{
				if (holder != null)
				{
					if (!player.destroyItemByItemId("ClassMaster", holder.getId(), holder.getCount(), player, true))
					{
						return false;
					}
				}
			}
		}
		
		if (!tpl.getRewardItems().isEmpty())
		{
			final List<ItemHolder> allReward = tpl.getRewardItems().get(-1);
			final List<ItemHolder> classReward = tpl.getRewardItems().get(val);
			if (allReward != null && !allReward.isEmpty())
			{
				for (final ItemHolder holder : allReward)
				{
					if (holder != null && Rnd.chance(holder.getChance()))
					{
						player.addItem("ClassMaster", holder.getId(), holder.getCount(), player, true);
					}
				}
			}
			if (classReward != null && !classReward.isEmpty())
			{
				for (final ItemHolder holder : classReward)
				{
					if (holder != null && Rnd.chance(holder.getChance()))
					{
						player.addItem("ClassMaster", holder.getId(), holder.getCount(), player, true);
					}
				}
			}
		}
		
		player.setClassId(val);
		
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		}
		else
		{
			player.setBaseClass(player.getActiveClass());
		}
		
		player.broadcastUserInfo(true);
		
		tpl = ClassMasterParser.getInstance().getClassTemplate(player.getClassId().level() + 1);
		if (tpl != null && tpl.isAllowedChangeClass() && Config.ALTERNATE_CLASS_MASTER && (((player.getClassId().level() == 1) && (player.getLevel() >= 40)) || ((player.getClassId().level() == 2) && (player.getLevel() >= 76))))
		{
			showQuestionMark(player);
		}
		return true;
	}
	
	private static final int getMinLevel(int level)
	{
		switch (level)
		{
			case 0 :
				return 20;
			case 1 :
				return 40;
			case 2 :
				return 76;
			default :
				return Integer.MAX_VALUE;
		}
	}
	
	private static final boolean validateClassId(ClassId oldCID, int val)
	{
		try
		{
			return validateClassId(oldCID, ClassId.getClassId(val));
		}
		catch (final Exception e)
		{}
		return false;
	}
	
	private static final boolean validateClassId(ClassId oldCID, ClassId newCID)
	{
		if ((newCID == null) || (newCID.getRace() == null))
		{
			return false;
		}
		
		if (oldCID.equals(newCID.getParent()))
		{
			return true;
		}
		
		if (Config.ALLOW_ENTIRE_TREE && newCID.childOf(oldCID))
		{
			return true;
		}
		
		return false;
	}
	
	private static String getRequiredItems(Player player, int level)
	{
		final ClassMasterTemplate tpl = ClassMasterParser.getInstance().getClassTemplate(level);
		if (tpl == null)
		{
			return "";
		}
		if ((tpl.getRequestItems() == null) || tpl.getRequestItems().isEmpty())
		{
			return "<tr><td width=280 align=center>none</td></tr>";
		}
		final StringBuilder sb = new StringBuilder();
		if (tpl.getRequestItems() != null && !tpl.getRequestItems().isEmpty())
		{
			for (final ItemHolder holder : tpl.getRequestItems())
			{
				if (holder != null)
				{
					sb.append("<tr><td width=280 align=center><font color=\"LEVEL\">" + holder.getCount() + "</font> " + ItemsParser.getInstance().getTemplate(holder.getId()).getName(player.getLang()) + "</td></tr>");
				}
			}
		}
		return sb.toString();
	}
	
	private static String getRewardItems(Player player, int level, int val)
	{
		final ClassMasterTemplate tpl = ClassMasterParser.getInstance().getClassTemplate(level);
		if (tpl == null)
		{
			return "";
		}
		if ((tpl.getRewardItems() == null) || tpl.getRewardItems().isEmpty())
		{
			return "<tr><td width=280 align=center>none</td></tr>";
		}
		final List<ItemHolder> allReward = tpl.getRewardItems().get(-1);
		final List<ItemHolder> classReward = val >= 0 ? tpl.getRewardItems().get(val) : null;
		if (allReward != null && !allReward.isEmpty() || classReward != null && !classReward.isEmpty())
		{
			final StringBuilder sb = new StringBuilder();
			
			if (allReward != null && !allReward.isEmpty())
			{
				for (final ItemHolder holder : allReward)
				{
					if (holder != null)
					{
						final var chance = holder.getChance() < 100 ? "<font color=LEVEL>" + holder.getChance() + "%</font>" : "";
						sb.append("<tr><td width=280 align=center><font color=LEVEL>x" + holder.getCount() + "</font> " + ItemsParser.getInstance().getTemplate(holder.getId()).getName(player.getLang()) + " " + chance + "</td></tr>");
					}
				}
			}
			
			if (classReward != null && !classReward.isEmpty())
			{
				for (final ItemHolder holder : classReward)
				{
					if (holder != null)
					{
						final var chance = holder.getChance() < 100 ? "<font color=LEVEL>" + holder.getChance() + "%</font>" : "";
						sb.append("<tr><td width=280 align=center><font color=LEVEL>x" + holder.getCount() + "</font> " + ItemsParser.getInstance().getTemplate(holder.getId()).getName(player.getLang()) + " " + chance + "</td></tr>");
					}
				}
			}
			return sb.toString();
		}
		return "<tr><td width=280 align=center>none</td></tr>";
	}
}