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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.commons.util.ValueSortMap;
import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.listener.player.impl.AcademyAnswerListener;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.service.academy.AcademyList;
import l2e.gameserver.model.service.academy.AcademyRewards;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.clientpackets.pledge.RequestJoinPledge;
import l2e.gameserver.network.serverpackets.CreatureSay;

public class CommunityAcademy extends AbstractCommunity implements ICommunityBoardHandler
{
	private static Map<Integer, SortBy> _playerSortBy = new HashMap<>();
	private static Map<Integer, String> _playerSearch = new HashMap<>();

	public CommunityAcademy()
	{
		AcademyList.restore();
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
		        "_bbsShowAcademyList", "_bbsRegAcademyChar", "_bbsShowInvitePage", "_bbsInviteToAcademy", "_bbsUnregisterFromAcademy", "_bbsAcademySearch", "_bbsAcademySort", "_bbsAcademyReset"
		};
	}

	@Override
	public void onBypassCommand(final String bypass, Player player)
	{
		final StringTokenizer str = new StringTokenizer(bypass, " ");
		final String cmd = str.nextToken();
		
		if (!checkCondition(player, new StringTokenizer(bypass, "_").nextToken(), false, false))
		{
			return;
		}
		
		if (!checkConditions(player))
		{
			return;
		}

		if (cmd.equalsIgnoreCase("_bbsRegAcademyChar"))
		{
			academyButton(player, false);
		}
		else if (cmd.equalsIgnoreCase("_bbsShowAcademyList"))
		{
			final StringTokenizer st = new StringTokenizer(bypass, " ");
			st.nextToken();
			final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			showAcademList(player, page);
		}
		else if (cmd.equalsIgnoreCase("_bbsShowInvitePage"))
		{
			final StringTokenizer st = new StringTokenizer(bypass, " ");
			st.nextToken();
			final String name = st.nextToken();
			final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			showAcademyChar(player, name, page);
		}
		else if (cmd.equalsIgnoreCase("_bbsAcademySort"))
		{
			final StringTokenizer st = new StringTokenizer(bypass, " ");
			st.nextToken();
			final String sorname = st.hasMoreTokens() ? st.nextToken() : "Level";
			_playerSortBy.put(player.getObjectId(), SortBy.getEnum(sorname));
			showAcademList(player, 1);
		}
		else if (cmd.equalsIgnoreCase("_bbsAcademySearch"))
		{
			final StringTokenizer st = new StringTokenizer(bypass, " ");
			st.nextToken();
			final String search = st.hasMoreTokens() ? st.nextToken() : "";
			_playerSearch.put(player.getObjectId(), search);
			showAcademList(player, 1);
		}
		else if (cmd.equalsIgnoreCase("_bbsAcademyReset"))
		{
			_playerSearch.remove(player.getObjectId());
			showAcademList(player, 1);
		}
		else if (cmd.equalsIgnoreCase("_bbsInviteToAcademy"))
		{
			final StringTokenizer st = new StringTokenizer(bypass, " ");
			st.nextToken();
			if (st.countTokens() != 3)
			{
				return;
			}

			final String charName = st.nextToken();
			final String item = st.nextToken();
			final long price = Long.valueOf(st.nextToken());

			final int itemId = AcademyRewards.getInstance().getItemId(item, player);
			if (itemId == -1)
			{
				player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.MSG1")));
				return;
			}

			final Player acadChar = GameObjectsStorage.getPlayer(charName);
			if ((acadChar == null) || !acadChar.isOnline())
			{
				player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.MSG2")));
				showAcademList(player, 1);
				return;
			}

			if (!player.checkFloodProtection("ACADEMY_ACTION", "academy_action"))
			{
				player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.MSG3")));
				showAcademList(player, 1);
				return;
			}
			inviteToAcademy(player, acadChar, itemId, price);
		}
		else if (cmd.equalsIgnoreCase("_bbsUnregisterFromAcademy"))
		{
			academyButton(player, true);
		}
	}

	private void academyButton(final Player activeChar, final boolean unregisterToAcademy)
	{
		if (!activeChar.checkFloodProtection("ACADEMYACTION", "academy_action"))
		{
			activeChar.sendMessage("Do not spam the button, please wait 5 seconds and try again.");
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.MSG4")));
			return;
		}

		if (unregisterToAcademy)
		{
			AcademyList.deleteFromAcdemyList(activeChar);
			activeChar.sendMessage("You have unregistered from Academy Search Board.");
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.MSG5")));
			onBypassCommand("_bbsShowAcademyList 1", activeChar);
			return;
		}

		if ((activeChar.getLevel() < 5) || (activeChar.getLevel() > 39))
		{
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.MSG6")));
			return;
		}

		if (activeChar.getClan() != null)
		{
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.MSG7")));
			return;
		}

		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.MSG8")));
			return;
		}
		
		for (final Player plr : AcademyList.getAcademyList())
		{
			if (plr == null)
			{
				continue;
			}

			if (plr == activeChar)
			{
				activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.MSG9")));
				return;
			}
		}
		activeChar.setSearchforAcademy(true);
		AcademyList.addToAcademy(activeChar);
		activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.MSG10")));
		activeChar.sendMessage("Successfuly registered into Academy Search Board.");
		onBypassCommand("_bbsShowAcademyList 1", activeChar);
	}

	private static List<Player> getFilteredAcademy(String filter)
	{
		if (filter == null)
		{
			filter = "";
		}

		final List<Player> filteredList = new ArrayList<>();
		for (final Player plr : AcademyList.getAcademyList())
		{
			if (plr.getName(null).toLowerCase().contains(filter.toLowerCase()) || plr.getClassId().getName(plr.getLang()).toLowerCase().contains(filter.toLowerCase()))
			{
				filteredList.add(plr);
			}
		}

		return filteredList;
	}

	private void showAcademList(final Player player, final int page)
	{
		String htmltosend = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/academy/academyList.htm");
		String buttonHtml = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/academy/academyList-button.htm");
		final List<Player> academy = getFilteredAcademy(_playerSearch.get(player.getObjectId()));
		final List<Player> _academyList = AcademyList.getAcademyList();

		String searchfor = _playerSearch.get(player.getObjectId());
		if (searchfor == null)
		{
			searchfor = "";
		}

		final StringBuilder sb = new StringBuilder();
		SortBy sortBy = _playerSortBy.get(player.getObjectId());

		if (sortBy == null)
		{
			sortBy = SortBy.LEVEL;
		}

		final String nameOfCurSortBy = sortBy.toString() + ";";
		sb.append(nameOfCurSortBy);

		for (final SortBy s : SortBy.values())
		{
			final String str = s + ";";
			if (!str.toString().equalsIgnoreCase(nameOfCurSortBy))
			{
				sb.append(str);
			}
		}

		htmltosend = htmltosend.replaceAll("%sortbylist%", sb.toString());

		if (!_academyList.contains(player) && (player.getLevel() < 40) && (player.getLevel() > 4) && (player.getClan() == null))
		{
			buttonHtml = buttonHtml.replace("%btnName%", ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.REGISTER"));
			buttonHtml = buttonHtml.replace("%btnBypass%", "_bbsRegAcademyChar");
			htmltosend = htmltosend.replace("%mainbutton%", buttonHtml);
		}
		if (_academyList.contains(player))
		{
			buttonHtml = buttonHtml.replace("%btnName%", ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.UNREGISTER"));
			buttonHtml = buttonHtml.replace("%btnBypass%", "_bbsUnregisterFromAcademy");
			htmltosend = htmltosend.replace("%mainbutton%", buttonHtml);
		}
		else
		{
			htmltosend = htmltosend.replace("%mainbutton%", "");
		}

		int all = 0;
		int clansvisual = 0;
		final boolean pagereached = false;
		final int totalpages = (_academyList.size() / 16) + 1;

		if (page == 1)
		{
			if (totalpages == 1)
			{
				htmltosend = htmltosend.replaceAll("%more%", "&nbsp;");
			}
			else
			{
				htmltosend = htmltosend.replaceAll("%more%", "<button value=\"\" action=\"bypass -h _bbsShowAcademyList " + (page + 1) + " \" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\">");
			}
			htmltosend = htmltosend.replaceAll("%back%", "&nbsp;");
		}
		else if (page > 1)
		{
			if (totalpages <= page)
			{
				htmltosend = htmltosend.replaceAll("%back%", "<button value=\"\" action=\"bypass -h _bbsShowAcademyList " + (page - 1) + "\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\">");
				htmltosend = htmltosend.replaceAll("%more%", "&nbsp;");
			}
			else
			{
				htmltosend = htmltosend.replaceAll("%more%", "<button value=\"\" action=\"bypass -h _bbsShowAcademyList " + (page + 1) + "\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\">");
				htmltosend = htmltosend.replaceAll("%back%", "<button value=\"\" action=\"bypass -h _bbsShowAcademyList " + (page - 1) + "\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\">");
			}
		}

		for (final Player plr : getSorttedacademys(_playerSortBy.get(player.getObjectId()), academy))
		{
			if (plr.isInOfflineMode())
			{
				AcademyList.deleteFromAcdemyList(plr);
				continue;
			}

			if (!plr.isOnline())
			{
				AcademyList.deleteFromAcdemyList(plr);
				continue;
			}

			if (plr.getClanJoinExpiryTime() > System.currentTimeMillis() || (plr.getLevel() > 40) || (plr.getClassId().level() > 2))
			{
				AcademyList.deleteFromAcdemyList(plr);
				continue;
			}

			all++;
			if ((page == 1) && (clansvisual > 16))
			{
				continue;
			}
			if (!pagereached && (all > (page * 16)))
			{
				continue;
			}
			if (!pagereached && (all <= ((page - 1) * 16)))
			{
				continue;
			}

			clansvisual++;

			htmltosend = htmltosend.replaceAll("%icon" + clansvisual + "%", getIconByRace(plr.getRace()));
			htmltosend = htmltosend.replaceAll("%classname" + clansvisual + "%", "<font color=B59A75>" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.CLASS") + "</font> <font color=01A9DB>" + Util.className(player, plr.getClassId().getId()) + "</font>");
			htmltosend = htmltosend.replaceAll("%name" + clansvisual + "%", "<font color=B59A75>" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.NAME") + "</font> <font color=\"CB4646\">" + plr.getName(null) + "</font>");
			htmltosend = htmltosend.replaceAll("%level" + clansvisual + "%", " [" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.LVL") + " <font color=\"ad9d46\">" + plr.getLevel() + "</font>]");
			htmltosend = htmltosend.replaceAll("%onlinetime" + clansvisual + "%", "<font color=B59A75>" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.ONLINE") + "</font> <font color=\"848484\">" + TimeUtils.formatTime(player, (int) plr.getTotalOnlineTime(), false) + "</font>");
			
			if ((player.getClan() != null) && (player.getClan().getAvailablePledgeTypes(-1) == 0) && (player.getClan().getLevel() > 4) && ((player.getClanPrivileges() & Clan.CP_CL_JOIN_CLAN) == Clan.CP_CL_JOIN_CLAN) && !plr.getBlockList().isInBlockList(player))
			{
				htmltosend = htmltosend.replaceAll("%request" + clansvisual + "%", "<button value=\"\" action=\"bypass -h _bbsShowInvitePage " + plr.getName(null) + " 1\" width=32 height=32 back=\"L2UI_CT1.MiniMap_DF_PlusBtn_Red\" fore=\"L2UI_CT1.MiniMap_DF_PlusBtn_Red\">");
			}
			else
			{
				htmltosend = htmltosend.replaceAll("%request" + clansvisual + "%", "<button width=32 height=0>");
			}

			htmltosend = htmltosend.replaceAll("%width" + clansvisual + "%", "180");
		}

		if (clansvisual < 16)
		{
			for (int d = clansvisual + 1; d != 17; d++)
			{
				htmltosend = htmltosend.replaceAll("%icon" + d + "%", "L2UI_CT1.Inventory_DF_CloakSlot_Disable");
				htmltosend = htmltosend.replaceAll("%classname" + d + "%", "&nbsp;");
				htmltosend = htmltosend.replaceAll("%name" + d + "%", "&nbsp;");
				htmltosend = htmltosend.replaceAll("%level" + d + "%", "&nbsp;");
				htmltosend = htmltosend.replaceAll("%onlinetime" + d + "%", "&nbsp;");
				htmltosend = htmltosend.replaceAll("%request" + d + "%", "&nbsp;");
				htmltosend = htmltosend.replaceAll("%width" + d + "%", "395");
			}
		}

		htmltosend = htmltosend.replaceAll("%searchfor%", searchfor.isEmpty() ? "&nbsp;" : "" + ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.SEARCH_RESULT") + ": <font color=LEVEL>" + searchfor + "</font>");
		htmltosend = htmltosend.replaceAll("%totalresults%", "" + _academyList.size());
		separateAndSend(htmltosend, player);
	}

	private void showAcademyChar(final Player player, final String academyChar, final int pageNum)
	{
		final Player plr = GameObjectsStorage.getPlayer(academyChar);
		if ((plr == null) || !plr.isOnline())
		{
			player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.MSG11")));
			showAcademList(player, 1);
			return;
		}

		String htmltosend = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/academy/academyRequest.htm");
		String infoHtml = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/academy/academyRequest-info.htm");
		for (final Player academy : AcademyList.getAcademyList())
		{
			if (!academy.getName(null).equalsIgnoreCase(academyChar))
			{
				continue;
			}
			infoHtml = infoHtml.replace("%items%", AcademyRewards.getInstance().toList(player));
			infoHtml = infoHtml.replace("%min%", Util.formatAdena(Config.ACADEMY_MIN_ADENA_AMOUNT));
			infoHtml = infoHtml.replace("%max%", Util.formatAdena(Config.ACADEMY_MAX_ADENA_AMOUNT));
			infoHtml = infoHtml.replace("%name%", academy.getName(null));
			infoHtml = infoHtml.replace("%page%", String.valueOf(pageNum));
			break;
		}

		htmltosend = htmltosend.replace("%playerName%", plr.getName(null) + " [" + plr.getLevel() + "]");
		htmltosend = htmltosend.replace("%body%", infoHtml);

		separateAndSend(htmltosend, player);
	}

	private void inviteToAcademy(final Player activeChar, final Player academyChar, final int itemId, long price)
	{
		if ((activeChar == null) || (academyChar == null))
		{
			return;
		}

		if (!checkConditions(activeChar))
		{
			return;
		}

		price = Math.max(price, 0);

		if ((price < Config.ACADEMY_MIN_ADENA_AMOUNT) || (price > Config.ACADEMY_MAX_ADENA_AMOUNT))
		{
			final ServerMessage msg = new ServerMessage("CommunityAcademy.INVALID_AMOUNT", activeChar.getLang());
			msg.add(Util.formatAdena(Config.ACADEMY_MIN_ADENA_AMOUNT));
			msg.add(Util.formatAdena(Config.ACADEMY_MAX_ADENA_AMOUNT));
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), msg.toString()));
			showAcademyChar(activeChar, academyChar.getName(null), 1);
			return;
		}

		final Clan clan = activeChar.getClan();
		if ((clan == null) || !clan.checkClanJoinCondition(activeChar, academyChar, -1))
		{
			return;
		}

		if (activeChar.getInventory().getItemByItemId(itemId) == null || activeChar.getInventory().getItemByItemId(itemId).getCount() < price)
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT);
			return;
		}

		if (!activeChar.getRequest().setRequest(academyChar, new RequestJoinPledge()))
		{
			return;
		}
		
		academyChar.setPledgeItemId(itemId);
		academyChar.setPledgePrice(price);

		final ServerMessage msg = new ServerMessage("CommunityAcademy.TRY_INVITE", academyChar.getLang());
		msg.add(activeChar.getName(null));
		msg.add(activeChar.getClan().getName());
		msg.add(Util.formatAdena(price));
		msg.add(Util.getItemName(academyChar, itemId));
		academyChar.sendConfirmDlg(new AcademyAnswerListener(activeChar, academyChar), 15000, msg.toString());
		onBypassCommand("_bbsShowAcademyList 1", activeChar);
		
		final ServerMessage msg1 = new ServerMessage("CommunityAcademy.SEND", activeChar.getLang());
		msg1.add(academyChar.getName(null));
		activeChar.sendMessage(msg1.toString());
		activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(activeChar.getLang(), "CommunityAcademy.ACADEMY"), msg1.toString()));
	}

	private boolean checkConditions(final Player player)
	{
		if ((player == null) || player.isDead())
		{
			return false;
		}

		if (player.isCursedWeaponEquipped() || player.isJailed() || player.isDead() || player.isAlikeDead() || player.isCastingNow() || player.isInCombat() || player.isAttackingNow() || player.isInOlympiadMode() || player.isFlying() || player.isCombatFlagEquipped())
		{
			player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.MSG12")));
			return false;
		}

		if ((player.getReflectionId() != 0) || player.isInsideZone(ZoneId.NO_RESTART))
		{
			player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.MSG13")));
			return false;
		}

		if (player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendPacket(new CreatureSay(player.getObjectId(), Say2.BATTLEFIELD, ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.ACADEMY"), ServerStorage.getInstance().getString(player.getLang(), "CommunityAcademy.MSG14")));
			return false;
		}

		if (player.isCombatFlagEquipped())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_IN_POSSESSION_OF_A_WARD);
			return false;
		}
		return true;
	}

	private static String getIconByRace(final Race race)
	{
		switch (race)
		{
			case Human :
				return "icon.skill4416_human";
			case Elf :
				return "icon.skill4416_elf";
			case DarkElf :
				return "icon.skill4416_darkelf";
			case Orc :
				return "icon.skill4416_orc";
			case Dwarf :
				return "icon.skill4416_dwarf";
			case Kamael :
				return "icon.skill4416_kamael";
		}
		return "icon.skill4416_etc";
	}

	private enum SortBy
	{
		LEVEL("Level"), NAME_ASC("Name(Ascending)"), NAME_DSC("Name(Descending)"), ONLINE_ASC("Online(Ascending)"), ONLINE_DSC("Online(Descending)"), CLASS_ASC("Class(Ascending)"), CLASS_DSC("Class(Descending)");

		private final String _sortName;

		private SortBy(final String sortName)
		{
			_sortName = sortName;
		}

		@Override
		public String toString()
		{
			return _sortName;
		}

		public static SortBy getEnum(final String sortName)
		{
			for (final SortBy sb : values())
			{
				if (sb.toString().equals(sortName))
				{
					return sb;
				}
			}

			return LEVEL;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Player> getSorttedacademys(SortBy sort, final List<Player> academys)
	{
		if (sort == null)
		{
			sort = SortBy.LEVEL;
		}

		final List<Player> sorted = new ArrayList<>();

		switch (sort)
		{
			default:
			case LEVEL:
				final List<Player> notSortedValues = new ArrayList<>();
				notSortedValues.addAll(academys);
				Player storedid = null;
				int lastpoints = 0;

				while (notSortedValues.size() > 0)
				{
					if (sorted.size() == academys.size())
					{
						break;
					}

					for (final Player cplayer : notSortedValues)
					{
						if (cplayer.getLevel() >= lastpoints)
						{
							storedid = cplayer;
							lastpoints = cplayer.getLevel();
						}
					}

					if (storedid != null)
					{
						notSortedValues.remove(storedid);
						sorted.add(storedid);
						storedid = null;
						lastpoints = 0;
					}
				}

				return sorted;
			case NAME_ASC:
				final Map<Player, String> tmp = new HashMap<>();
				for (final Player academy : academys)
				{
					tmp.put(academy, academy.getName(null));
				}

				sorted.addAll(ValueSortMap.sortMapByValue(tmp, true).keySet());
				return sorted;
			case NAME_DSC:
				final Map<Player, String> tmp2 = new HashMap<>();
				for (final Player academy : academys)
				{
					tmp2.put(academy, academy.getName(null));
				}

				sorted.addAll(ValueSortMap.sortMapByValue(tmp2, false).keySet());
				return sorted;
			case ONLINE_ASC:
				final Map<Player, Long> tmp3 = new HashMap<>();
				for (final Player academy : academys)
				{
					tmp3.put(academy, academy.getTotalOnlineTime());
				}

				sorted.addAll(ValueSortMap.sortMapByValue(tmp3, true).keySet());
				return sorted;
			case ONLINE_DSC:
				final Map<Player, Long> tmp4 = new HashMap<>();
				for (final Player academy : academys)
				{
					tmp4.put(academy, academy.getTotalOnlineTime());
				}

				sorted.addAll(ValueSortMap.sortMapByValue(tmp4, false).keySet());
				return sorted;
			case CLASS_ASC:
				final Map<Player, String> tmp5 = new HashMap<>();
				for (final Player academy : academys)
				{
					tmp5.put(academy, academy.getClassId().getName(academy.getLang()));
				}

				sorted.addAll(ValueSortMap.sortMapByValue(tmp5, true).keySet());
				return sorted;
			case CLASS_DSC:
				final Map<Player, String> tmp6 = new HashMap<>();
				for (final Player academy : academys)
				{
					tmp6.put(academy, academy.getClassId().getName(academy.getLang()));
				}

				sorted.addAll(ValueSortMap.sortMapByValue(tmp6, false).keySet());
				return sorted;
		}
	}

	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
	}
	
	public static CommunityAcademy getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityAcademy _instance = new CommunityAcademy();
	}
}
