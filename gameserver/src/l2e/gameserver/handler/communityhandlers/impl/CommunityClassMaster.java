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
package l2e.gameserver.handler.communityhandlers.impl;

import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ClassMasterParser;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.Augmentation;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.ClassMasterTemplate;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.items.PcItemTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Updated by LordWinter 28.06.2021
 */
public class CommunityClassMaster extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityClassMaster()
	{
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
		        "_bbsclass"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player activeChar)
	{
		if (!checkCondition(activeChar, new StringTokenizer(command, "_").nextToken(), false, false))
		{
			return;
		}
		
		if (!ClassMasterParser.getInstance().isAllowCommunityClassMaster())
		{
			return;
		}
		
		final ClassId classId = activeChar.getClassId();
		final int jobLevel = classId.level();
		final int level = activeChar.getLevel();
		
		final ClassMasterTemplate tpl1 = ClassMasterParser.getInstance().getClassTemplate(1);
		final ClassMasterTemplate tpl2 = ClassMasterParser.getInstance().getClassTemplate(2);
		final ClassMasterTemplate tpl3 = ClassMasterParser.getInstance().getClassTemplate(3);
		
		final StringBuilder html = new StringBuilder("");
		html.append("<br>");
		html.append("<center>");
		
		if ((((level >= 20) && (jobLevel == 0) && tpl1 != null && tpl1.isAllowedChangeClass()) || ((level >= 40) && (jobLevel == 1) && tpl2 != null && tpl2.isAllowedChangeClass()) || ((level >= 76) && (jobLevel == 2 && tpl3 != null && tpl3.isAllowedChangeClass()))))
		{
			final ClassMasterTemplate tpl = ClassMasterParser.getInstance().getClassTemplate((jobLevel + 1));
			if ((tpl.getRequestItems() != null) && !tpl.getRequestItems().isEmpty())
			{
				html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.MUST_PAY") + " <br>");
				
				for (final ItemHolder holder : tpl.getRequestItems())
				{
					html.append("<center><font color=\"LEVEL\">" + holder.getCount() + "</font> " + ItemsParser.getInstance().getTemplate(holder.getId()).getName(activeChar.getLang()) + "</center><br>");
				}
			}
			
			int val = -1;
			for (final ClassId cid : ClassId.values())
			{
				if (cid == ClassId.inspector)
				{
					continue;
				}
				
				if (cid.childOf(classId) && (cid.level() == (classId.level() + 1)))
				{
					val = cid.getId();
					html.append("<br><center><button value=\"").append(Util.className(activeChar, cid.getId())).append("\" action=\"bypass -h _bbsclass;change_class;").append(cid.getId()).append(";").append((jobLevel + 1)).append("\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_Apply_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Apply\"></center>");
				}
			}
			html.append("</center>");
			if ((tpl.getRewardItems() != null) && !tpl.getRewardItems().isEmpty() && val >= 0)
			{
				final List<ItemHolder> allReward = tpl.getRewardItems().get(-1);
				final List<ItemHolder> classReward = tpl.getRewardItems().get(Integer.valueOf(val));
				if (allReward != null && !allReward.isEmpty() || classReward != null && !classReward.isEmpty())
				{
					html.append("<br>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.REWARDS") + " <br>");
					
					if (allReward != null && !allReward.isEmpty())
					{
						for (final ItemHolder holder : allReward)
						{
							if (holder != null)
							{
								final var chance = holder.getChance() < 100 ? "<font color=LEVEL>" + holder.getChance() + "%</font>" : "";
								html.append("<center><font color=LEVEL>x" + holder.getCount() + "</font> " + ItemsParser.getInstance().getTemplate(holder.getId()).getName(activeChar.getLang()) + " " + chance + "</center><br>");
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
								html.append("<center><font color=LEVEL>x" + holder.getCount() + "</font> " + ItemsParser.getInstance().getTemplate(holder.getId()).getName(activeChar.getLang()) + " " + chance + "</center><br>");
							}
						}
					}
				}
			}
		}
		else
		{
			switch (jobLevel)
			{
				case 0 :
					html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.WELCOME") + " " + activeChar.getName(null) + "! " + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.CURRENT_PROF") + " <font color=F2C202>" + Util.className(activeChar, activeChar.getClassId().getId()) + "</font>.<br>");
					if (tpl1 != null && tpl1.isAllowedChangeClass())
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.FOR_CHANGE_PROF") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.20_LVL") + "</font><br>");
					}
					else if (tpl2 != null && tpl2.isAllowedChangeClass())
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.FOR_CHANGE_PROF") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.40_LVL_1") + "</font><br>");
					}
					else if (tpl3 != null && tpl3.isAllowedChangeClass())
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.FOR_CHANGE_PROF") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL_2") + "</font><br>");
					}
					else
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.NO_CHANGE_PROF") + "<br>");
					}
					html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.SUBCLASS_CHANGE") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL") + ".</font><br>");
					html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.NOOBLESS_CHANGE") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL") + ".</font><br>");
					break;
				case 1 :
					html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.WELCOME") + " " + activeChar.getName(null) + "! " + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.CURRENT_PROF") + " <font color=F2C202>" + Util.className(activeChar, activeChar.getClassId().getId()) + "</font>.<br>");
					if (tpl2 != null && tpl2.isAllowedChangeClass())
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.FOR_CHANGE_PROF") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.40_LVL") + "</font><br>");
					}
					else if (tpl3 != null && tpl3.isAllowedChangeClass())
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.FOR_CHANGE_PROF") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL_2") + "</font><br>");
					}
					else
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.NO_CHANGE_PROF") + "<br>");
					}
					html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.SUBCLASS_CHANGE") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL") + ".</font><br>");
					html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.NOOBLESS_CHANGE") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL") + ".</font><br>");
					break;
				case 2 :
					html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.WELCOME") + " " + activeChar.getName(null) + "! " + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.CURRENT_PROF") + " <font color=F2C202>" + Util.className(activeChar, activeChar.getClassId().getId()) + "</font>.<br>");
					if (tpl3 != null && tpl3.isAllowedChangeClass())
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.FOR_CHANGE_PROF") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL") + ".</font><br>");
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.SUBCLASS_CHANGE") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL") + ".</font><br>");
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.NOOBLESS_CHANGE") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL") + ".</font><br>");
					}
					else
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.NO_CHANGE_PROF") + "<br>");
					}
					break;
				case 3 :
					html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.WELCOME") + " " + activeChar.getName(null) + "! " + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.CURRENT_PROF") + " <font color=F2C202>" + Util.className(activeChar, activeChar.getClassId().getId()) + "</font>.<br>");
					html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.NO_CHANGE_PROF") + "<br>");
					if (level >= 76)
					{
						html.append("" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.YOU_REACH") + " <font color=F2C202>" + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.76_LVL") + "</font>! " + ServerStorage.getInstance().getString(activeChar.getLang(), "ClassBBS.ACTIVE_SUBCLASS") + "<br>");
					}
					break;
			}
		}
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/community/classmaster.htm");
		adminReply.replace("%classmaster%", html.toString());
		separateAndSend(adminReply.getHtm(), activeChar);

		if (command.startsWith("_bbsclass;change_class;"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final short val = Short.parseShort(st.nextToken());
			final int id = Integer.parseInt(st.nextToken());
			final ClassMasterTemplate tpl = ClassMasterParser.getInstance().getClassTemplate(id);
			if (tpl == null)
			{
				onBypassCommand("_bbsclass;", activeChar);
				return;
			}
			
			if (!validateClassId(activeChar.getClassId(), val))
			{
				onBypassCommand("_bbsclass;", activeChar);
				return;
			}
			
			if (!activeChar.isInventoryUnder90(false))
			{
				activeChar.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return;
			}
			
			if (!tpl.getRequestItems().isEmpty())
			{
				final List<ItemHolder> requestList = tpl.getRequestItems();
				for (final ItemHolder holder : requestList)
				{
					if (holder != null)
					{
						if (activeChar.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getCount())
    					{
    						activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
    						onBypassCommand("_bbsclass;", activeChar);
    						return;
    					}
					}
				}
				
				for (final ItemHolder holder : requestList)
				{
					if (holder != null)
					{
						if (!activeChar.destroyItemByItemId("ClassMaster", holder.getId(), holder.getCount(), activeChar, true))
						{
							onBypassCommand("_bbsclass;", activeChar);
							return;
						}
					}
				}
			}
			changeClass(activeChar, val, tpl);
			onBypassCommand("_bbsclass;", activeChar);
		}
	}

	private void changeClass(Player activeChar, short val, ClassMasterTemplate tpl)
	{
		if (activeChar.getClassId().level() == ClassId.values()[val].level())
		{
			return;
		}

		if (activeChar.getClassId().level() == 3)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER));
		}
		else
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLASS_TRANSFER));
		}
		activeChar.setClassId(val);

		if (activeChar.isSubClassActive())
		{
			activeChar.getSubClasses().get(Integer.valueOf(activeChar.getClassIndex())).setClassId(activeChar.getActiveClass());
		}
		else
		{
			if (!tpl.getRewardItems().isEmpty())
			{
				final List<ItemHolder> allReward = tpl.getRewardItems().get(-1);
				final List<ItemHolder> classReward = tpl.getRewardItems().get(Integer.valueOf(val));
				if (allReward != null && !allReward.isEmpty())
				{
					for (final ItemHolder holder : allReward)
					{
						if (holder != null && Rnd.chance(holder.getChance()))
						{
							activeChar.addItem("ClassMaster", holder.getId(), holder.getCount(), activeChar, true);
						}
					}
				}
				if (classReward != null && !classReward.isEmpty())
				{
					for (final ItemHolder holder : classReward)
					{
						if (holder != null && Rnd.chance(holder.getChance()))
						{
							activeChar.addItem("ClassMaster", holder.getId(), holder.getCount(), activeChar, true);
						}
					}
				}
			}
			activeChar.setBaseClass(activeChar.getActiveClass());
			
			if (activeChar.getTemplate().hasInitialEquipment())
			{
				ItemInstance item;
				for (final PcItemTemplate ie : activeChar.getTemplate().getInitialEquipment())
				{
					item = new ItemInstance(IdFactory.getInstance().getNextId(), ie.getId());
					if (ie.getEnchant() != 0)
					{
						item.setEnchantLevel(ie.getEnchant());
					}
					
					if (ie.getAugmentId() != -1)
					{
						item.setAugmentation(new Augmentation(ie.getAugmentId()));
					}
					
					if (ie.getElementals() != null && !ie.getElementals().isEmpty())
					{
						final String[] elements = ie.getElementals().split(";");
						for (final String el : elements)
						{
							final String[] element = el.split(":");
							if (element != null)
							{
								item.setElementAttr(Byte.parseByte(element[0]), Integer.parseInt(element[1]));
							}
						}
					}
					item.setCount(ie.getCount());
					if (ie.getDurability() > 0)
					{
						item.setMana(ie.getDurability());
					}
					activeChar.getInventory().addItem("Code Item", item, activeChar, null);
					
					if (item.isEquipable() && ie.isEquipped())
					{
						activeChar.getInventory().equipItem(item);
					}
				}
			}
		}
		activeChar.broadcastUserInfo(true);
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

	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}

	public static CommunityClassMaster getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final CommunityClassMaster _instance = new CommunityClassMaster();
	}
}