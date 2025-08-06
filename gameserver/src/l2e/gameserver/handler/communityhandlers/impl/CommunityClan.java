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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.TimeUtils;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.htm.ImagesCache;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.Clan.SinglePetition;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.base.SubClass;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.ClanHall;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowInfoUpdate;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListAdd;
import l2e.gameserver.network.serverpackets.pledge.PledgeShowMemberListAll;

public class CommunityClan extends AbstractCommunity implements ICommunityBoardHandler
{
	private static final int CLANS_PER_PAGE = Config.CLANS_PER_PAGE;
	private static final int MEMBERS_PER_PAGE = Config.MEMBERS_PER_PAGE;
	private static final int PETITION_PER_PAGE = Config.PETITIONS_PER_PAGE;
	private static final int SKILLS_PER_PAGE = Config.SKILLS_PER_PAGE;
	private static final int MAX_QUESTION_LEN = Config.CLAN_PETITION_QUESTION_LEN;
	private static final int MAX_ANSWER_LEN = Config.CLAN_PETITION_ANSWER_LEN;
	private static final int MAX_COMMENT_LEN = Config.CLAN_PETITION_COMMENT_LEN;
	
	private static final String[] ALL_CLASSES =
	{
	        "Duelist", "Dreadnought", "PhoenixKnight", "HellKnight", "Adventurer", "Sagittarius", "Archmage", "Soultaker", "ArcanaLord", "Cardinal", "Hierophant", "EvaTemplar", "SwordMuse", "WindRider", "MoonlightSentinel", "MysticMuse", "ElementalMaster", "EvaSaint", "ShillienTemplar", "SpectralDancer", "GhostHunter", "GhostSentinel", "StormScreamer", "SpectralMaster", "ShillienSaint", "Titan", "GrandKhavatari", "Dominator", "Doomcryer", "FortuneSeeker", "Maestro"
	};
	private static final int[] SLOTS =
	{
	        Inventory.PAPERDOLL_RHAND, Inventory.PAPERDOLL_LHAND, Inventory.PAPERDOLL_HEAD, Inventory.PAPERDOLL_CHEST, Inventory.PAPERDOLL_LEGS, Inventory.PAPERDOLL_GLOVES, Inventory.PAPERDOLL_FEET, Inventory.PAPERDOLL_CLOAK, Inventory.PAPERDOLL_UNDER, Inventory.PAPERDOLL_BELT, Inventory.PAPERDOLL_LFINGER, Inventory.PAPERDOLL_RFINGER, Inventory.PAPERDOLL_LEAR, Inventory.PAPERDOLL_REAR, Inventory.PAPERDOLL_NECK, Inventory.PAPERDOLL_LBRACELET
	};
	private static final String[] NAMES =
	{
	        "Weapon", "Shield", "Helmet", "Chest", "Legs", "Gloves", "Boots", "Cloak", "Shirt", "Belt", "Ring", " Ring", "Earring", "Earring", "Necklace", "Bracelet"
	};
	
	public CommunityClan()
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
		        "_bbsclan", "_bbsclclan", "_bbsclanlist", "_bbsclanmanage", "_bbsclanjoin", "_bbsclanpetitions", "_bbsclanplayerpetition", "_bbsclanplayerinventory", "_bbsclanmembers", "_bbsclanmembersingle", "_bbsclanskills", "_bbsclannoticeform", "_bbsclannoticeenable", "_bbsclannoticedisable", "Notice"
		};
	}

	@Override
	public void onBypassCommand(String bypass, Player player)
	{
		final StringTokenizer st = new StringTokenizer(bypass, "_");
		final String cmd = st.nextToken();
		String html = null;
		
		if (!checkCondition(player, cmd, false, false))
		{
			return;
		}
		
		if ("bbsclan".equals(cmd))
		{
			final Clan clan = player.getClan();
			if (clan != null)
			{
				onBypassCommand("_bbsclclan_" + player.getClanId(), player);
			}
			else
			{
				onBypassCommand("_bbsclanlist_0", player);
			}
			return;
		}
		else if ("bbsclanlist".equals(cmd))
		{
			final int page = Integer.parseInt(st.nextToken());
			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_list.htm");
			html = html.replace("%color%", getMainStatsTableColor(0));
			final Clan[] clans = ClanHolder.getInstance().getClans();
			Arrays.sort(clans, _clansComparator);
			final var template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_list_template.htm");
			final var recrut_button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/recruts_button.htm");
			final var list_button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/list_button.htm");
			final int max = Math.min(CLANS_PER_PAGE + CLANS_PER_PAGE * page, clans.length);
			int index = 0;
			
			String block = "";
			String list = "";
			
			for (int i = CLANS_PER_PAGE * page; i < max; i++)
			{
				final Clan clan = clans[i];
				block = template;
				String icon = "";
				block = block.replace("%bcolor%", getMainStatsTableColor(index + 1));
				block = block.replace("%number%", String.valueOf((i + 1)));
				
				if ((clan.getAllyId() > 0 && clan.getAllyCrestId() > 0) || clan.getCrestId() != 0)
				{
					if (clan.getAllyId() > 0 && clan.getAllyCrestId() > 0)
					{
						icon += "<td>";
						icon += "<table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + player.getRequestId() + "_" + clan.getAllyCrestId() + ">";
						icon += "<tr><td fixwidth=8><img height=4 width=8 src=L2UI.SquareBlack>&nbsp;</td></tr>";
						icon += "</table></td>";
						ImagesCache.getInstance().sendImageToPlayer(player, clan.getAllyCrestId());
					}
					
					if (clan.getCrestId() != 0)
					{
						icon += "<td>";
						icon += "<table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + player.getRequestId() + "_" + clan.getCrestId() + ">";
						icon += "<tr><td fixwidth=16><img height=4 width=16 src=L2UI.SquareBlack>&nbsp;</td></tr>";
						icon += "</table></td>";
						ImagesCache.getInstance().sendImageToPlayer(player, clan.getCrestId());
					}
				}
				
				if (icon.isEmpty())
				{
					icon += "<td width=46>&nbsp;</td>";
				}
				
				block = block.replace("%icon%", icon);
				block = block.replace("%bypass%", "_bbsclclan_" + clan.getId());
				block = block.replace("%name%", clan.getName());
				block = block.replace("%leader%", clan.getLeaderName());
				block = block.replace("%ally%", clan.getAllyId() > 0 ? clan.getAllyName() : "<font name=\"__SYSTEMWORLDFONT\" color=\"A18C70\">" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
				block = block.replace("%level%", String.valueOf(clan.getLevel()));
				var info = recrut_button;
				if (!clan.isRecruting() || clan.isFull())
				{
					info = info.replace("%bypass%", "_bbsclanlist_" + page);
					info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.RECRUT_CLOSE"));
					block = block.replace("%recruts%", info);
				}
				else if (player.getClan() != null)
				{
					info = info.replace("%bypass%", "_bbsclanlist_" + page);
					info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.RECRUT_OPEN"));
					block = block.replace("%recruts%", info);
				}
				else
				{
					info = info.replace("%bypass%", " _bbsclanjoin_" + clan.getId() + "_0");
					info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.JOIN_CLAN"));
					block = block.replace("%recruts%", info);
				}
				list += block;
				index++;
			}
			html = html.replace("%rank%", list);
			if (page > 0)
			{
				var info = list_button;
				info = info.replace("%bypass%", "_bbsclanlist_" + (page - 1));
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.PREVIOUS"));
				html = html.replace("%pre%", info);
			}
			else
			{
				html = html.replace("%pre%", "&nbsp;");
			}
			if (clans.length > CLANS_PER_PAGE + CLANS_PER_PAGE * page)
			{
				var info = list_button;
				info = info.replace("%bypass%", "_bbsclanlist_" + (page + 1));
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NEXT"));
				html = html.replace("%next%", info);
			}
			else
			{
				html = html.replace("%next%", "&nbsp;");
			}
			html = html.replace("%myClan%", (player.getClan() != null ? "_bbsclclan_" + player.getClanId() : "_bbsclanlist_0"));
		}
		else if ("bbsclclan".equals(cmd))
		{
			final int clanId = Integer.parseInt(st.nextToken());
			if (clanId == 0)
			{
				player.sendPacket(SystemMessageId.NOT_JOINED_IN_ANY_CLAN);
				onBypassCommand("_bbsclanlist_0", player);
				return;
			}

			final Clan clan = ClanHolder.getInstance().getClan(clanId);
			if (clan == null)
			{
				onBypassCommand("_bbsclanlist_0", player);
				return;
			}

			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_index.htm");
			html = getMainClanPage(player, clan, html);
		}
		else if ("bbsclanmanage".equals(cmd))
		{
			final String actionToken = st.nextToken();
			final int action = Integer.parseInt(actionToken.substring(0, 1));

			if (action != 0)
			{
				final boolean shouldReturn = manageRecrutationWindow(player, action, actionToken);
				if (shouldReturn)
				{
					return;
				}
			}
			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_recruit.htm");
			html = getClanRecruitmentManagePage(player, html);
		}
		else if ("bbsclanjoin".equals(cmd))
		{
			final int clanId = Integer.parseInt(st.nextToken());

			final Clan clan = ClanHolder.getInstance().getClan(clanId);
			if (clan == null)
			{
				player.sendMessage("Such clan cannot be found!");
				onBypassCommand("_bbsclanlist_0", player);
				return;
			}

			if (player.getClanJoinExpiryTime() > System.currentTimeMillis())
			{
				player.sendPacket(SystemMessageId.YOU_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN);
				return;
			}

			final String next = st.nextToken();
			if (Integer.parseInt(next.substring(0, 1)) == 1)
			{
				try
				{
					if (!manageClanJoinWindow(player, clan, next.substring(2)))
					{
						player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.ALREADY_SEND").toString());
						onBypassCommand("_bbsclclan_" + clan.getId(), player);
						return;
					}
				}
				catch (final Exception e)
				{
					player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.INCORRECT").toString());
					onBypassCommand("_bbsclanjoin_" + clan.getId() + "_0", player);
					return;
				}
				player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.SUBMITTED").toString());
				onBypassCommand("_bbsclclan_" + clan.getId(), player);
				return;
			}
			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_join.htm");
			html = getClanJoinPage(player, clan, html);
		}
		else if ("bbsclanpetitions".equals(cmd))
		{
			final int clanId = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			final Clan clan = ClanHolder.getInstance().getClan(clanId);
			if (clan == null)
			{
				player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NOT_FOUND").toString());
				onBypassCommand("_bbsclanlist_0", player);
				return;
			}

			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_petitions.htm");
			final var template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_petition_template.htm");
			final var list_button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/list_button.htm");
			final List<SinglePetition> petitionsToRemove = new ArrayList<>();
			for (final SinglePetition petition : clan.getPetitions())
			{
				final ClanPetitionData data = getClanPetitionsData(player, petition.getSenderId());
				if (data == null)
				{
					petitionsToRemove.add(petition);
					continue;
				}
			}
			
			if (!petitionsToRemove.isEmpty())
			{
				for (final SinglePetition petitionToRemove : petitionsToRemove)
				{
					clan.deletePetition(petitionToRemove);
				}
				petitionsToRemove.clear();
			}
			final List<SinglePetition> petitions = clan.getPetitions();
			final int max = Math.min(PETITION_PER_PAGE + PETITION_PER_PAGE * page, petitions.size());
			int index = 0;
			String block = "";
			String list = "";
			html = html.replace("%clanName%", clan.getName());
			
			for (int i = PETITION_PER_PAGE * page; i < max; i++)
			{
				final SinglePetition petition = petitions.get(i);
				final ClanPetitionData data = getClanPetitionsData(player, petition.getSenderId());
				if (data != null)
				{
					block = template;
					block = block.replace("%number%", String.valueOf((index + 1)));
					block = block.replace("%bypass%", "_bbsclanplayerpetition_" + petition.getSenderId());
					block = block.replace("%name%", data.char_name);
					block = block.replace("%online%", data.online);
					block = block.replace("%pvp%", String.valueOf(data.pvpKills));
					block = block.replace("%onlineTime%", TimeUtils.formatTime(player, (int) data.onlineTime, false));
					block = block.replace("%nooble%", Util.boolToString(player, data.isNoble));
					list += block;
					index++;
				}
			}
			
			html = html.replace("%petitions%", list);
			if (page > 0)
			{
				var info = list_button;
				info = info.replace("%bypass%", "_bbsclanpetitions_" + clanId + "_" + (page - 1));
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.PREVIOUS"));
				html = html.replace("%pre%", info);
			}
			else
			{
				html = html.replace("%pre%", "&nbsp;");
			}
			
			if (petitions.size() > PETITION_PER_PAGE + PETITION_PER_PAGE * page)
			{
				var info = list_button;
				info = info.replace("%bypass%", "_bbsclanpetitions_" + clanId + "_" + (page + 1));
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NEXT"));
				html = html.replace("%next%", info);
			}
			else
			{
				html = html.replace("%next%", "&nbsp;");
			}
			
			String buttons = "";
			final var button_join = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_join.htm");
			final var button_manage = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_manage.htm");
			final var button_petition = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_petition.htm");
			final var button_notice = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_notice.htm");
			if (player.getClan() == null)
			{
				var info = button_join;
				info = info.replace("%bypass%", "_bbsclanjoin_" + clan.getId() + "_0");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.JOIN_CLAN"));
				buttons += info;
			}
			
			if (player.getClan() != null && player.getClan().equals(clan) && player.getClan().getLeaderId() == player.getObjectId())
			{
				var info = button_manage;
				info = info.replace("%bypass%", "_bbsclanmanage_0");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_RECRUTE"));
				buttons += info;
				
				info = button_petition;
				info = info.replace("%bypass%", "_bbsclanpetitions_" + clan.getId() + "_0");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_PETITION"));
				buttons += info;
				
				info = button_notice;
				info = info.replace("%bypass%", "_bbsclannoticeform");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_NOTICE"));
				buttons += info;
			}
			
			if (buttons.isEmpty())
			{
				buttons += "<tr><td>&nbsp;</td></tr>";
			}
			html = html.replace("%buttons%", buttons);
			html = html.replace("%allyInfo%", getAllyInfo(player, clan));
		}
		else if ("bbsclanplayerpetition".equals(cmd))
		{
			final int senderId = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens())
			{
				final int action = Integer.parseInt(st.nextToken());
				managePlayerPetition(player, senderId, action);
				return;
			}
			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_playerpetition.htm");

			final Player sender = GameObjectsStorage.getPlayer(senderId);
			if (sender != null)
			{
				html = getClanSinglePetitionPage(player, sender, html);
			}
			else
			{
				html = getClanSinglePetitionPage(player, senderId, html);
			}
		}
		else if ("bbsclanplayerinventory".equals(cmd))
		{
			final int senderId = Integer.parseInt(st.nextToken());
			final Player sender = GameObjectsStorage.getPlayer(senderId);

			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_playerinventory.htm");

			if (sender != null)
			{
				html = getPlayerInventoryPage(sender, html);
			}
			else
			{
				html = getPlayerInventoryPage(player, senderId, html);
			}
		}
		else if ("bbsclanmembers".equals(cmd))
		{
			final int clanId = Integer.parseInt(st.nextToken());
			if (clanId == 0)
			{
				player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NOT_FOUND").toString());
				onBypassCommand("_bbsclanlist_0", player);
				return;
			}

			final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;

			final Clan clan = ClanHolder.getInstance().getClan(clanId);
			if (clan == null)
			{
				player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NOT_FOUND").toString());
				onBypassCommand("_bbsclanlist_0", player);
				return;
			}

			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_members.htm");
			html = html.replace("%clanName%", clan.getName());
			final List<ClanMember> members = clan.getAllMembers();
			final var template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_member_template.htm");
			final var list_button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/list_button.htm");
			int index = 0;
			String block = "";
			String list = "";
			final int max = Math.min(MEMBERS_PER_PAGE + MEMBERS_PER_PAGE * page, members.size());
			for (int i = MEMBERS_PER_PAGE * page; i < max; i++)
			{
				final ClanMember member = members.get(i);
				block = template;
				block = block.replace("%number%", String.valueOf((index + 1)));
				block = block.replace("%name%", member.getName());
				block = block.replace("%online%", member.isOnline() ? "<font color=6a9b54>" + ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") + "</font>" : "<font color=FF6666>" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
				block = block.replace("%leader%", member == clan.getLeader() ? "<font color=6a9b54>" + ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") + "</font>" : "<font color=FF6666>" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
				block = block.replace("%pledge%", "<font color=\"BBFF44\">" + getUnitName(player, member.getPledgeType()) + "</font>");
				list += block;
				index++;
			}
			html = html.replace("%members%", list);
			if (page > 0)
			{
				var info = list_button;
				info = info.replace("%bypass%", "_bbsclanmembers_" + clan.getId() + "_" + (page - 1));
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.PREVIOUS"));
				html = html.replace("%pre%", info);
			}
			else
			{
				html = html.replace("%pre%", "&nbsp;");
			}
			
			if (members.size() > MEMBERS_PER_PAGE + MEMBERS_PER_PAGE * page)
			{
				var info = list_button;
				info = info.replace("%bypass%", "_bbsclanmembers_" + clan.getId() + "_" + (page + 1));
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NEXT"));
				html = html.replace("%next%", info);
			}
			else
			{
				html = html.replace("%next%", "&nbsp;");
			}
			
			String buttons = "";
			final var button_join = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_join.htm");
			final var button_manage = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_manage.htm");
			final var button_petition = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_petition.htm");
			final var button_notice = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_notice.htm");
			if (player.getClan() == null)
			{
				var info = button_join;
				info = info.replace("%bypass%", "_bbsclanjoin_" + clan.getId() + "_0");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.JOIN_CLAN"));
				buttons += info;
			}
			
			if (player.getClan() != null && player.getClan().equals(clan) && player.getClan().getLeaderId() == player.getObjectId())
			{
				var info = button_manage;
				info = info.replace("%bypass%", "_bbsclanmanage_0");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_RECRUTE"));
				buttons += info;
				
				info = button_petition;
				info = info.replace("%bypass%", "_bbsclanpetitions_" + clan.getId() + "_0");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_PETITION"));
				buttons += info;
				
				info = button_notice;
				info = info.replace("%bypass%", "_bbsclannoticeform");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_NOTICE"));
				buttons += info;
			}
			
			if (buttons.isEmpty())
			{
				buttons += "<tr><td>&nbsp;</td></tr>";
			}
			html = html.replace("%buttons%", buttons);
			html = html.replace("%allyInfo%", getAllyInfo(player, clan));
		}
		else if ("bbsclanskills".equals(cmd))
		{
			final int clanId = Integer.parseInt(st.nextToken());
			final int page = Integer.parseInt(st.nextToken());
			if (clanId == 0)
			{
				player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NOT_FOUND").toString());
				onBypassCommand("_bbsclanlist_0", player);
				return;
			}

			final Clan clan = ClanHolder.getInstance().getClan(clanId);
			if (clan == null)
			{
				player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NOT_FOUND").toString());
				onBypassCommand("_bbsclanlist_0", player);
				return;
			}

			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_skills.htm");
			final var template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_skills_template.htm");
			final var list_button = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/list_button.htm");
			html = html.replace("%clanName%", clan.getName());
			html = html.replace("%clanId%", String.valueOf(clan.getId()));
			
			final Skill[] skills = clan.getAllSkills();
			final int max = Math.min(SKILLS_PER_PAGE + SKILLS_PER_PAGE * page, skills.length);
			String block = "";
			String list = "";
			
			for (int i = SKILLS_PER_PAGE * page; i < max; i++)
			{
				final Skill skill = skills[i];
				
				block = template;
				block = block.replace("%icon%", skill.getIcon());
				block = block.replace("%name%", skill.getName(player.getLang()));
				block = block.replace("%level%", String.valueOf(skill.getLevel()));
				block = block.replace("%descr%", skill.getDescr(player.getLang()));
				list += block;
			}
			
			if (list.isEmpty())
			{
				list += "<tr><td>&nbsp;</td></tr>";
			}
			html = html.replace("%skills%", list);
			if (page > 0)
			{
				var info = list_button;
				info = info.replace("%bypass%", "_bbsclanskills_" + clanId + "_" + (page - 1));
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.PREVIOUS"));
				html = html.replace("%pre%", info);
			}
			else
			{
				html = html.replace("%pre%", "&nbsp;");
			}
			
			if (skills.length > PETITION_PER_PAGE + PETITION_PER_PAGE * page)
			{
				var info = list_button;
				info = info.replace("%bypass%", "_bbsclanskills_" + clanId + "_" + (page + 1));
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NEXT"));
				html = html.replace("%next%", info);
			}
			else
			{
				html = html.replace("%next%", "&nbsp;");
			}
			
			String buttons = "";
			final var button_join = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_join.htm");
			final var button_manage = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_manage.htm");
			final var button_petition = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_petition.htm");
			final var button_notice = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_notice.htm");
			if (player.getClan() == null)
			{
				var info = button_join;
				info = info.replace("%bypass%", "_bbsclanjoin_" + clan.getId() + "_0");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.JOIN_CLAN"));
				buttons += info;
			}
			
			if (player.getClan() != null && player.getClan().equals(clan) && player.getClan().getLeaderId() == player.getObjectId())
			{
				var info = button_manage;
				info = info.replace("%bypass%", "_bbsclanmanage_0");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_RECRUTE"));
				buttons += info;
				
				info = button_petition;
				info = info.replace("%bypass%", "_bbsclanpetitions_" + clan.getId() + "_0");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_PETITION"));
				buttons += info;
				
				info = button_notice;
				info = info.replace("%bypass%", "_bbsclannoticeform");
				info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_NOTICE"));
				buttons += info;
			}
			
			if (buttons.isEmpty())
			{
				buttons += "<tr><td>&nbsp;</td></tr>";
			}
			html = html.replace("%buttons%", buttons);
			html = html.replace("%allyInfo%", getAllyInfo(player, clan));
		}
		else if ("bbsclanmembersingle".equals(cmd))
		{
			final int playerId = Integer.parseInt(st.nextToken());

			html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_singlemember.htm");

			final Player member = GameObjectsStorage.getPlayer(playerId);
			if (member != null)
			{
				html = getClanSingleMemberPage(player, member, html);
			}
			else
			{
				html = getClanSingleMemberPage(player, playerId, html);
			}
		}
		else if ("bbsclannoticeform".equals(cmd))
		{
			if (player.getClan() == null || !player.isClanLeader())
			{
				onBypassCommand("_bbsclclan_" + player.getClanId(), player);
				return;
			}
			clanNotice(player, player.getClanId());
			return;
		}
		else if ("bbsclannoticeenable".equals(cmd))
		{
			if (player.getClan() == null || !player.isClanLeader())
			{
				onBypassCommand("_bbsclclan_" + player.getClanId(), player);
				return;
			}
			
			player.getClan().setNoticeEnabled(true);
			clanNotice(player, player.getClanId());
		}
		else if ("bbsclannoticedisable".equals(cmd))
		{
			if (player.getClan() == null || !player.isClanLeader())
			{
				onBypassCommand("_bbsclclan_" + player.getClanId(), player);
				return;
			}

			player.getClan().setNoticeEnabled(false);
			clanNotice(player, player.getClanId());
		}
		separateAndSend(html, player);
	}

	private String getMainClanPage(Player player, Clan clan, String html)
	{
		html = html.replace("%clanName%", clan.getName());
		html = html.replace("%clanId%", String.valueOf(clan.getId()));
		html = html.replace("%position%", "#" + clan.getRank());
		html = html.replace("%clanLeader%", clan.getLeaderName());
		html = html.replace("%allyName%", (clan.getAllyId() > 0 ? clan.getAllyName() : ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NO_ALLY")));
		html = html.replace("%crp%", Util.formatAdena(clan.getReputationScore()));
		html = html.replace("%membersCount%", String.valueOf(clan.getMembersCount()));
		html = html.replace("%clanLevel%", String.valueOf(clan.getLevel()));
		html = html.replace("%raidsKilled%", String.valueOf(0));
		html = html.replace("%epicsKilled%", String.valueOf(0));

		final ClanHall clanHall = ClanHallManager.getInstance().getAbstractHallByOwner(clan);
		html = html.replace("%clanHall%", (clanHall != null ? Util.clanHallName(player, clanHall.getId()) : ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NO")));
		final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
		html = html.replace("%castle%", (castle != null ? castle.getName(player.getLang()) : ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NO")));
		final Fort fortress = FortManager.getInstance().getFortByOwner(clan);
		html = html.replace("%fortress%", (fortress != null ? fortress.getName() : ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NO")));

		final int[] data = getMainClanPageData(clan);

		html = html.replace("%pvps%", String.valueOf(data[0]));
		html = html.replace("%pks%", String.valueOf(data[1]));
		html = html.replace("%nobleCount%", String.valueOf(data[2]));
		html = html.replace("%heroCount%", String.valueOf(data[3]));
		html = html.replace("%clan_avarage_level%", Util.formatAdena(clan.getAverageLevel()));
		html = html.replace("%clan_online%", Util.formatAdena(clan.getOnlineMembers(0).size()));

		String clanCrest = "";
		if ((clan.getAllyId() > 0 && clan.getAllyCrestId() > 0) || clan.getCrestId() != 0)
		{
			clanCrest += "<td width=46 align=center>";
			clanCrest += "<table fixwidth=24 fixheight=12 cellpadding=0 cellspacing=0>";
			clanCrest += "<tr>";

			if (clan.getAllyId() > 0 && clan.getAllyCrestId() > 0)
			{
				clanCrest += "<td>";
				clanCrest += "<br><table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + player.getRequestId() + "_" + clan.getAllyCrestId() + ">";
				clanCrest += "<tr><td fixwidth=8><img height=4 width=8 src=L2UI.SquareBlack>&nbsp;</td></tr>";
				clanCrest += "</table></td>";
				ImagesCache.getInstance().sendImageToPlayer(player, clan.getAllyCrestId());
			}

			if (clan.getCrestId() != 0)
			{
				clanCrest += "<td>";
				clanCrest += "<br><table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + player.getRequestId() + "_" + clan.getCrestId() + ">";
				clanCrest += "<tr><td fixwidth=16><img height=4 width=16 src=L2UI.SquareBlack>&nbsp;</td></tr>";
				clanCrest += "</table></td>";
				ImagesCache.getInstance().sendImageToPlayer(player, clan.getCrestId());
			}

			clanCrest += "</tr></table></td>";
		}
		else
		{
			clanCrest += "<td width=46>&nbsp;</td>";
		}
		html = html.replace("%clan_crest%", clanCrest);

		String alliances = "";
		if (clan.getAllyId() > 0)
		{
			alliances = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/alliances.htm");
			final var tpl = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/alliances-template.htm");
			var allInfo = "";
			for (final var cl : ClanHolder.getInstance().getClanAllies(clan.getAllyId()))
			{
				var info = tpl;
				info = info.replace("%name%", cl.getName());
				info = info.replace("%bypass%", "_bbsclclan_" + cl.getId());
				allInfo += info;
			}
			alliances = alliances.replace("%info%", allInfo);
		}
		html = html.replace("%alliances%", alliances);

		String wars = "";
		if (clan.getEnemyClans().size() > 0)
		{
			wars += "<tr>";
			int index = 0;
			for (final Clan warClan : clan.getEnemyClans())
			{
				if (index == 4)
				{
					wars += "</tr><tr>";
					index = 0;
				}
				wars += "<td align=center>";
				
				String warCrest = "";
				if ((warClan.getAllyCrestId() > 0) || warClan.getCrestId() != 0)
				{
					warCrest += "<td width=46 align=center>";
					warCrest += "<table fixwidth=24 fixheight=12 cellpadding=0 cellspacing=0>";
					warCrest += "<tr>";
					
					if (warClan.getAllyCrestId() > 0)
					{
						warCrest += "<td>";
						warCrest += "<br><table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + player.getRequestId() + "_" + warClan.getAllyCrestId() + ">";
						warCrest += "<tr><td fixwidth=8><img height=4 width=8 src=L2UI.SquareBlack>&nbsp;</td></tr>";
						warCrest += "</table></td>";
						ImagesCache.getInstance().sendImageToPlayer(player, warClan.getAllyCrestId());
					}
					
					if (warClan.getCrestId() != 0)
					{
						warCrest += "<td>";
						warCrest += "<br><table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + player.getRequestId() + "_" + warClan.getCrestId() + ">";
						warCrest += "<tr><td fixwidth=16><img height=4 width=16 src=L2UI.SquareBlack>&nbsp;</td></tr>";
						warCrest += "</table></td>";
						ImagesCache.getInstance().sendImageToPlayer(player, warClan.getCrestId());
					}
					warCrest += "</tr></table></td>";
				}
				else
				{
					warCrest += "<td width=46>&nbsp;</td>";
				}
				wars += "<table width=130 cellspacing=0 cellpadding=0 height=28><tr>" + warCrest + "<td fixwidth=94 align=left><font color=LEVEL name=\"hs11\"><a action=\"bypass -h _bbsclclan_" + warClan.getId() + "\">" + warClan.getName() + "</a></font></td></tr></table>";
				wars += "</td>";
				index++;
			}
			wars += "</tr>";
		}
		else
		{
			wars += "<tr><td>&nbsp;</td></tr>";
		}

		html = html.replace("%wars%", wars);

		String buttons = "";
		final var button_join = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_join.htm");
		final var button_manage = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_manage.htm");
		final var button_petition = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_petition.htm");
		final var button_notice = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_notice.htm");
		if (player.getClan() == null)
		{
			var info = button_join;
			info = info.replace("%bypass%", "_bbsclanjoin_" + clan.getId() + "_0");
			info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.JOIN_CLAN"));
			buttons += info;
		}

		if (player.getClan() != null && player.getClan().equals(clan) && player.getClan().getLeaderId() == player.getObjectId())
		{
			var info = button_manage;
			info = info.replace("%bypass%", "_bbsclanmanage_0");
			info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_RECRUTE"));
			buttons += info;
			
			info = button_petition;
			info = info.replace("%bypass%", "_bbsclanpetitions_" + clan.getId() + "_0");
			info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_PETITION"));
			buttons += info;
			
			info = button_notice;
			info = info.replace("%bypass%", "_bbsclannoticeform");
			info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_NOTICE"));
			buttons += info;
		}
		
		if (buttons.isEmpty())
		{
			buttons += "<tr><td>&nbsp;</td></tr>";
		}
		html = html.replace("%buttons%", buttons);
		html = html.replace("%allyInfo%", getAllyInfo(player, clan));
		return html;
	}
	
	private String getAllyInfo(Player player, Clan clan)
	{
		String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/ally_info.htm");
		final String template = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/ally_template.htm");
		String block = "";
		String list = "";
		
		if (clan.getAllyId() > 0)
		{
			for (final Clan allyClan : ClanHolder.getInstance().getClanAllies(clan.getAllyId()))
			{
				block = template;
				
				String clanCrest = "";
				if ((allyClan.getAllyCrestId() > 0) || allyClan.getCrestId() != 0)
				{
					clanCrest += "<td width=46 align=center>";
					clanCrest += "<table fixwidth=24 fixheight=12 cellpadding=0 cellspacing=0>";
					clanCrest += "<tr>";
					
					if (allyClan.getAllyCrestId() > 0)
					{
						clanCrest += "<td>";
						clanCrest += "<br><table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + player.getRequestId() + "_" + allyClan.getAllyCrestId() + ">";
						clanCrest += "<tr><td fixwidth=8><img height=4 width=8 src=L2UI.SquareBlack>&nbsp;</td></tr>";
						clanCrest += "</table></td>";
						ImagesCache.getInstance().sendImageToPlayer(player, allyClan.getAllyCrestId());
					}
					
					if (allyClan.getCrestId() != 0)
					{
						clanCrest += "<td>";
						clanCrest += "<br><table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + player.getRequestId() + "_" + allyClan.getCrestId() + ">";
						clanCrest += "<tr><td fixwidth=16><img height=4 width=16 src=L2UI.SquareBlack>&nbsp;</td></tr>";
						clanCrest += "</table></td>";
						ImagesCache.getInstance().sendImageToPlayer(player, allyClan.getCrestId());
					}
					
					clanCrest += "</tr></table></td>";
				}
				else
				{
					clanCrest += "<td width=46>&nbsp;</td>";
				}
				block = block.replace("%clan_crest%", clanCrest);
				block = block.replace("%clan_name%", clan.getId() != allyClan.getId() ? "<a action=\"bypass -h _bbsclclan_" + allyClan.getId() + "\">" + allyClan.getName() + "</a>" : allyClan.getName());
				block = block.replace("%online%", Util.formatAdena(allyClan.getOnlineMembers(0).size()));
				block = block.replace("%total%", Util.formatAdena(allyClan.getMembersCount()));
				list += block;
			}
		}
		else
		{
			list = "&nbsp;";
		}
		html = html.replace("%ally%", clan.getAllyId() > 0 ? "<font color=\"CDB38B\" name=\"hs11\">" + ServerStorage.getInstance().getString(player.getLang(), "ClanBBS.ALLIANCE") + "</font> <font color=\"26bfbf\">" + clan.getAllyName() + "</font>" : "&nbsp;");
		html = html.replace("%list%", list);
		return html;
	}

	private String getClanSingleMemberPage(Player player, Player member, String html)
	{
		html = html.replace("%playerName%", member.getName(null));
		html = html.replace("%playerId%", String.valueOf(member.getObjectId()));
		html = html.replace("%clanName%", member.getClan() != null ? member.getClan().getName() : "");
		html = html.replace("%online%", "<font color=6a9b54>True</font>");
		html = html.replace("%title%", member.getTitle(null));
		html = html.replace("%pvpPoints%", String.valueOf(member.getPvpKills()));
		html = html.replace("%pkPoints%", String.valueOf(member.getPkKills()));
		html = html.replace("%rank%", "Level " + (member.getClan() != null ? member.getClan().getClanMember(member.getObjectId()).getPowerGrade() : 0));
		html = html.replace("%onlineTime%", TimeUtils.formatTime(player, (int) member.getTotalOnlineTime(), false));
		html = html.replace("%leader%", member.getClan().getLeaderId() == member.getObjectId() ? ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") : ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE"));
		html = html.replace("%subpledge%", getUnitName(player, member.getPledgeType()));
		html = html.replace("%nobless%", member.isNoble() ? ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") : ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE"));
		html = html.replace("%hero%", member.isHero() ? ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") : ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE"));
		html = html.replace("%adena%", getConvertedAdena(player, member.getAdena()));
		html = html.replace("%recs%", String.valueOf(member.getRecommendation().getRecomHave()));
		html = html.replace("%sevenSigns%", SevenSigns.getCabalShortName(SevenSigns.getInstance().getPlayerCabal(member.getObjectId())));
		html = html.replace("%fame%", String.valueOf(member.getFame()));

		final Collection<SubClass> classes = member.getSubClasses().values();
		int subIndex = 0;
		for (final SubClass sub : classes)
		{
			String replacement = "";
			if (sub.getClassId() == member.getBaseClass())
			{
				replacement = "mainClass";
			}
			else
			{
				if (subIndex == 0)
				{
					replacement = "firstSub";
				}
				else if (subIndex == 1)
				{
					replacement = "secondSub";
				}
				else
				{
					replacement = "thirdSub";
				}
				subIndex++;
			}

			html = html.replace("%" + replacement + "%", Util.className(player, ClassId.values()[sub.getClassId()].getId()) + "(" + sub.getLevel() + ")");
		}
		html = html.replace("%firstSub%", "");
		html = html.replace("%secondSub%", "");
		html = html.replace("%thirdSub%", "");

		html = html.replace("%clanId%", String.valueOf(member.getClanId()));

		return html;
	}

	private String getClanSingleMemberPage(Player player, int playerId, String html)
	{
		final OfflineSinglePlayerData data = getSinglePlayerData(playerId);

		html = html.replace("%playerName%", data.char_name);
		html = html.replace("%playerId%", String.valueOf(playerId));
		html = html.replace("%clanName%", data.clan_name);
		html = html.replace("%online%", "<font color=9b5454>" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>");
		html = html.replace("%title%", data.title == null ? "" : data.title);
		html = html.replace("%pvpPoints%", "" + data.pvpKills);
		html = html.replace("%pkPoints%", "" + data.pkKills);
		html = html.replace("%onlineTime%", TimeUtils.formatTime(player, (int) data.onlineTime, false));
		html = html.replace("%leader%", Util.boolToString(player, data.isClanLeader));
		html = html.replace("%subpledge%", getUnitName(player, data.pledge_type));
		html = html.replace("%nobless%", Util.boolToString(player, data.isNoble));
		html = html.replace("%hero%", Util.boolToString(player, data.isHero));
		html = html.replace("%adena%", getConvertedAdena(player, data.adenaCount));
		html = html.replace("%recs%", "" + data.rec_have);
		html = html.replace("%sevenSigns%", SevenSigns.getCabalShortName(data.sevenSignsSide));
		html = html.replace("%fame%", "" + data.fame);
		html = html.replace("%clanId%", "" + data.clanId);

		final String[] otherSubs =
		{
		        "%firstSub%", "%secondSub%", "%thirdSub%"
		};
		final int index = 0;
		for (final int[] sub : data.subClassIdLvlBase)
		{
			if (sub[2] == 1)
			{
				html = html.replace("%mainClass%", Util.className(player, ClassId.values()[sub[0]].getId()) + "(" + sub[1] + ")");
			}
			else
			{
				html = html.replace(otherSubs[index], Util.className(player, ClassId.values()[sub[0]].getId()) + "(" + sub[1] + ")");
			}
		}

		for (final String sub : otherSubs)
		{
			html = html.replace(sub, "<br>");
		}

		return html;
	}

	private String getClanSinglePetitionPage(Player leader, Player member, String html)
	{
		html = html.replace("%clanId%", String.valueOf(leader.getClan().getId()));
		html = html.replace("%playerId%", String.valueOf(member.getObjectId()));
		html = html.replace("%playerName%", member.getName(null));
		html = html.replace("%online%", "<font color=6a9b54>" + ServerStorage.getInstance().getString(leader.getLang(), "Util.TRUE") + "</font>");
		html = html.replace("%onlineTime%", TimeUtils.formatTime(leader, (int) member.getTotalOnlineTime(), false));
		html = html.replace("%pvpPoints%", String.valueOf(member.getPvpKills()));
		html = html.replace("%pkPoints%", String.valueOf(member.getPkKills()));
		html = html.replace("%fame%", String.valueOf(member.getFame()));
		html = html.replace("%adena%", getConvertedAdena(leader, member.getAdena()));
		html = html.replace("%mainClass%", Util.className(leader, ClassId.values()[member.getBaseClass()].getId()) + "");
		final Collection<SubClass> classes = member.getSubClasses().values();
		int subIndex = 0;
		for (final SubClass sub : classes)
		{
			String replacement = "";
			if (subIndex == 0)
			{
				replacement = "firstSub";
			}
			else if (subIndex == 1)
			{
				replacement = "secondSub";
			}
			else
			{
				replacement = "thirdSub";
			}
			subIndex++;

			html = html.replace("%" + replacement + "%", Util.className(leader, ClassId.values()[sub.getClassId()].getId()) + "(" + ServerStorage.getInstance().getString(leader.getLang(), "CommunityClan.LEVEL") + ": " + sub.getLevel() + ")");
		}
		html = html.replace("%firstSub%", "");
		html = html.replace("%secondSub%", "");
		html = html.replace("%thirdSub%", "");

		int index = 1;
		for (final String question : leader.getClan().getQuestions())
		{
			html = html.replace("%question" + index + "%", question != null && question.length() > 2 ? question + "?" : "");
			index++;
		}

		final SinglePetition petition = leader.getClan().getPetition(member.getObjectId());
		index = 1;
		for (final String answer : petition.getAnswers())
		{
			html = html.replace("%answer" + index + "%", answer != null && answer.length() > 2 ? answer : "");
			index++;
		}

		html = html.replace("%comment%", petition.getComment());

		return html;
	}

	private String getClanSinglePetitionPage(Player leader, int playerId, String html)
	{
		final PetitionPlayerData data = getSinglePetitionPlayerData(playerId);

		html = html.replace("%clanId%", String.valueOf(leader.getClanId()));
		html = html.replace("%playerId%", String.valueOf(playerId));
		html = html.replace("%online%", "<font color=9b5454>" + ServerStorage.getInstance().getString(leader.getLang(), "Util.FALSE") + "</font>");
		html = html.replace("%playerName%", data.char_name);
		html = html.replace("%onlineTime%", TimeUtils.formatTime(leader, (int) data.onlineTime, false));
		html = html.replace("%pvpPoints%", "" + data.pvpKills);
		html = html.replace("%pkPoints%", "" + data.pkKills);
		html = html.replace("%fame%", "" + data.fame);
		html = html.replace("%adena%", getConvertedAdena(leader, data.adenaCount));

		final String[] otherSubs =
		{
		        "%firstSub%", "%secondSub%", "%thirdSub%"
		};
		int index = 0;
		for (final int[] sub : data.subClassIdLvlBase)
		{
			if (sub[2] == 1)
			{
				html = html.replace("%mainClass%", Util.className(leader, ClassId.values()[sub[0]].getId()) + "(" + sub[1] + ")");
			}
			else
			{
				html = html.replace(otherSubs[index], Util.className(leader, ClassId.values()[sub[0]].getId()) + "(" + sub[1] + ")");
			}
		}

		for (final String sub : otherSubs)
		{
			html = html.replace(sub, "<br>");
		}

		index = 1;
		for (final String question : leader.getClan().getQuestions())
		{
			html = html.replace("%question" + index + "%", question != null && question.length() > 2 ? question : "");
			index++;
		}

		final SinglePetition petition = leader.getClan().getPetition(playerId);
		index = 1;
		for (final String answer : petition.getAnswers())
		{
			html = html.replace("%answer" + index + "%", answer != null && answer.length() > 2 ? answer : "");
			index++;
		}

		html = html.replace("%comment%", petition.getComment());

		return html;
	}

	private String getClanRecruitmentManagePage(Player player, String html)
	{
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return html;
		}

		final var button_class = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_class.htm");
		
		html = html.replace("%clanName%", clan.getName());
		final boolean firstChecked = clan.getClassesNeeded().size() == ALL_CLASSES.length;
		html = html.replace("%checked1%", firstChecked ? "_checked" : "");
		html = html.replace("%checked2%", firstChecked ? "" : "_checked");

		final String[] notChoosenClasses = getNotChosenClasses(player, clan);
		html = html.replace("%firstClassGroup%", notChoosenClasses[0]);
		html = html.replace("%secondClassGroup%", notChoosenClasses[1]);

		String list = "<tr>";
		int index = -1;
		for (final Integer clas : clan.getClassesNeeded())
		{
			if (index % 4 == 3)
			{
				list += "</tr><tr>";
			}
			index++;
			final String className = Util.className(player, (ALL_CLASSES[clas - 88].substring(0, 1).toLowerCase() + ALL_CLASSES[clas - 88].substring(1)));
			final String shortName = className.length() > 15 ? className.substring(0, 15) : className;
			var info = button_class;
			info = info.replace("%bypass%", "_bbsclanmanage_5 " + className);
			info = info.replace("%name%", shortName);
			list += info;
		}
		list += "</tr>";

		html = html.replace("%choosenClasses%", list);

		for (int i = 0; i < 8; i++)
		{
			final String clanQuestion = clan.getQuestions()[i];
			html = html.replace("%question" + (i + 1) + "%", clanQuestion != null && clanQuestion.length() > 0 ? clanQuestion : "Question " + (i + 1) + ":");
		}

		html = html.replace("%recrutation%", clan.isRecruting() ? ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.STOP") : ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.START"));
		return html;
	}

	private String getClanJoinPage(Player player, Clan clan, String html)
	{
		html = html.replace("%clanId%", String.valueOf(clan.getId()));
		html = html.replace("%clanName%", clan.getName());
		for (int i = 0; i < 8; i++)
		{
			final String question = clan.getQuestions()[i];
			if (question != null && question.length() > 2)
			{
				html = html.replace("%question" + (i + 1) + "%", question);
				html = html.replace("%answer" + (i + 1) + "%", "<edit var=\"answer" + (i + 1) + "\" width=100 height=13>");
			}
			else
			{
				html = html.replace("%question" + (i + 1) + "%", "");
				html = html.replace("%answer" + (i + 1) + "%", "");
				html = html.replace("$answer" + (i + 1), " ");
			}
		}

		boolean canJoin = false;

		String classes = "<tr>";
		int index = -1;
		for (final int classNeeded : clan.getClassesNeeded())
		{
			index++;
			if (index == 6)
			{
				classes += "</tr><tr>";
				index = 0;
			}
			
			final boolean goodClass = (player.getBaseClass() == classNeeded) || player.getSubClasses().keySet().contains(classNeeded);
			if (goodClass)
			{
				canJoin = true;
			}

			classes += "<td width=130><font color=\"" + (goodClass ? "00FF00" : "9b5454") + "\">";
			classes += Util.className(player, ClassId.values()[classNeeded].getId());
			classes += "</font></td>";
		}
		classes += "</tr>";

		html = html.replace("%classes%", classes);
		
		final var button_send = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_send.htm");

		if (canJoin)
		{
			var info = button_send;
			info = info.replace("%bypass%", "_bbsclanjoin_" + clan.getId() + "_1 | $answer1 | $answer2 | $answer3 | $answer4 | $answer5 | $answer6 | $answer7 | $answer8 | $comment |");
			info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.SEND"));
			html = html.replace("%joinClanButton%", info);
		}
		else
		{
			html = html.replace("%joinClanButton%", "");
		}
		html = html.replace("%allyInfo%", getAllyInfo(player, clan));
		return html;
	}

	private String getPlayerInventoryPage(Player player, String html)
	{
		html = html.replace("%playerName%", player.getName(null));
		html = html.replace("%playerId%", String.valueOf(player.getObjectId()));
		html = html.replace("%back%", (player.getClan() != null ? "_bbsclanmembersingle_" + player.getObjectId() : "_bbsclanplayerpetition_" + player.getObjectId()));

		final PcInventory pcInv = player.getInventory();
		String inventory = "<tr>";
		for (int i = 0; i < SLOTS.length; i++)
		{
			if (i % 2 == 0)
			{
				inventory += "</tr><tr>";
			}
			inventory += "<td align=center><table><tr><td height=40>";
			inventory += pcInv.getPaperdollItem(SLOTS[i]) != null ? "<img src=" + pcInv.getPaperdollItem(SLOTS[i]).getItem().getIcon() + " width=32 height=32>" : "<img src=\"Icon.low_tab\" width=32 height=32>";
			inventory += "</td><td width=150><font color=\"FFFFFF\">";
			inventory += pcInv.getPaperdollItem(SLOTS[i]) != null ? pcInv.getPaperdollItem(SLOTS[i]).getItem().getName(player.getLang()) + " +" + pcInv.getPaperdollItem(SLOTS[i]).getEnchantLevel() : "" + ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NO") + " " + ServerStorage.getInstance().getString(player.getLang(), "CommunityClan." + NAMES[i] + "");
			inventory += "</font></td></tr></table></td>";
		}
		inventory += "</tr>";

		html = html.replace("%inventory%", inventory);

		return html;
	}

	private String getPlayerInventoryPage(Player player, int playerId, String html)
	{
		final OfflinePlayerInventoryData data = getPlayerInventoryData(playerId);
		html = html.replace("%playerName%", data.char_name);
		html = html.replace("%playerId%", String.valueOf(playerId));
		html = html.replace("%back%", (data.clanId != 0 ? "_bbsclanmembersingle_" + playerId : "_bbsclanplayerpetition_" + playerId));

		String inventory = "<tr>";
		for (int i = 0; i < SLOTS.length; i++)
		{
			if (i % 2 == 0)
			{
				inventory += "</tr><tr>";
			}
			final int[] item = data.itemIdAndEnchantForSlot.get(i);
			Item template = null;
			if (item != null && item[0] > 0)
			{
				template = ItemsParser.getInstance().getTemplate(item[0]);
			}
			inventory += "<td align=center><table><tr><td height=40>";
			inventory += template != null ? "<img src=" + template.getIcon() + " width=32 height=32>" : "<img src=\"Icon.low_tab\" width=32 height=32>";
			inventory += "</td><td width=150><font color=\"bc7420\">";
			inventory += template != null ? template.getName(player.getLang()) + " +" + item[1] : "" + ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.NO") + " " + ServerStorage.getInstance().getString(player.getLang(), "CommunityClan." + NAMES[i] + "");
			inventory += "</font></td></tr></table></td>";
		}
		inventory += "</tr>";

		html = html.replace("%inventory%", inventory);

		return html;
	}

	private class OfflinePlayerInventoryData
	{
		String char_name;
		int clanId;
		Map<Integer, int[]> itemIdAndEnchantForSlot = new HashMap<>();
	}

	private OfflinePlayerInventoryData getPlayerInventoryData(int playerId)
	{
		final OfflinePlayerInventoryData data = new OfflinePlayerInventoryData();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name,clanid FROM characters WHERE charId = '" + playerId + "'");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data.char_name = rset.getString("char_name");
				data.clanId = rset.getInt("clanid");
			}
			statement.close();
			rset.close();

			statement = con.prepareStatement("SELECT item_id, loc_data, enchant_level FROM items WHERE owner_id = '" + playerId + "' AND loc='PAPERDOLL'");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int loc = rset.getInt("loc_data");
				for (int i = 0; i < SLOTS.length; i++)
				{
					if (loc == SLOTS[i])
					{
						final int[] itemData =
						{
						        rset.getInt("item_id"), rset.getInt("enchant_level")
						};
						data.itemIdAndEnchantForSlot.put(i, itemData);
					}
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error in getPlayerInventoryData:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return data;
	}

	private int[] getMainClanPageData(Clan clan)
	{
		final int[] data = new int[5];

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT SUM(pvpkills), SUM(pkkills) FROM characters WHERE characters.clanid = '" + clan.getId() + "'");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data[0] = rset.getInt("SUM(pvpkills)");
				data[1] = rset.getInt("SUM(pkkills)");
			}
			statement.close();
			rset.close();
			
			statement = con.prepareStatement("SELECT count(characters.charId) FROM characters WHERE characters.nobless=1 AND characters.clanid =" + clan.getId() + "");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data[2] = rset.getInt("count(characters.charId)");
			}
			statement.close();
			rset.close();

			statement = con.prepareStatement("SELECT count(characters.charId) FROM characters JOIN heroes on characters.charId = heroes.charId WHERE characters.clanid =" + clan.getId() + "");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data[3] = rset.getInt("count(characters.charId)");
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error in getMainClanPageData:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return data;
	}

	private class OfflineSinglePlayerData
	{
		String char_name;
		String title = "";
		int pvpKills;
		int pkKills;
		long onlineTime;
		int rec_have;
		int sevenSignsSide = 0;
		int fame;
		int clanId;
		String clan_name = "";
		int pledge_type = 0;
		boolean isClanLeader = false;
		boolean isNoble = false;
		boolean isHero = false;
		long adenaCount = 0L;
		List<int[]> subClassIdLvlBase = new ArrayList<>();
	}

	private OfflineSinglePlayerData getSinglePlayerData(int playerId)
	{
		final OfflineSinglePlayerData data = new OfflineSinglePlayerData();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name,base_class,title,pvpkills,pkkills,onlinetime,rec_have,fame,clanid FROM characters WHERE charId = '" + playerId + "'");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data.char_name = rset.getString("char_name");
				data.title = rset.getString("title");
				data.pvpKills = rset.getInt("pvpkills");
				data.pkKills = rset.getInt("pkkills");
				data.onlineTime = rset.getLong("onlinetime");
				data.rec_have = rset.getInt("rec_have");
				data.fame = rset.getInt("fame");
				data.clanId = rset.getInt("clanid");
				
				final int[] sub = new int[3];
				sub[0] = rset.getInt("base_class");
				sub[1] = rset.getInt("level");
				sub[2] = 1;
				data.subClassIdLvlBase.add(sub);
			}
			statement.close();
			rset.close();
			
			statement = con.prepareStatement("SELECT cabal FROM seven_signs WHERE charId='" + playerId + "'");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data.sevenSignsSide = SevenSigns.getCabalNumber(rset.getString("cabal"));
			}
			statement.close();
			rset.close();

			if (data.clanId > 0)
			{
				statement = con.prepareStatement("SELECT type,name,leader_id FROM `clan_subpledges` where `clan_id` = '" + data.clanId + "'");
				rset = statement.executeQuery();
				if (rset.next())
				{
					data.clan_name = rset.getString("name");
					data.pledge_type = rset.getInt("type");
					data.isClanLeader = rset.getInt("leader_id") == playerId;
				}
				statement.close();
				rset.close();
			}

			statement = con.prepareStatement("SELECT olympiad_points FROM `olympiad_nobles` where `char_id` = '" + playerId + "'");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data.isNoble = true;
			}
			statement.close();
			rset.close();

			statement = con.prepareStatement("SELECT count FROM `heroes` where `char_id` = '" + playerId + "'");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data.isHero = true;
			}
			statement.close();
			rset.close();

			statement = con.prepareStatement("SELECT count FROM `items` where `owner_id` = '" + playerId + "' AND item_id=57");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data.adenaCount = rset.getLong("count");
			}
			statement.close();
			rset.close();

			statement = con.prepareStatement("SELECT class_id,level FROM `character_subclasses` where `charId` = '" + playerId + "'");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int[] sub = new int[3];
				sub[0] = rset.getInt("class_id");
				sub[1] = rset.getInt("level");
				sub[2] = 0;
				data.subClassIdLvlBase.add(sub);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error in getSinglePlayerData:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return data;
	}

	private class PetitionPlayerData
	{
		String char_name;
		long onlineTime;
		int pvpKills;
		int pkKills;
		int fame;
		long adenaCount = 0L;
		List<int[]> subClassIdLvlBase = new ArrayList<>();
	}

	private PetitionPlayerData getSinglePetitionPlayerData(int playerId)
	{
		final PetitionPlayerData data = new PetitionPlayerData();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name,base_class,level,onlinetime,pvpkills,pkkills,fame FROM characters WHERE charId = '" + playerId + "'");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data.char_name = rset.getString("char_name");
				data.onlineTime = rset.getLong("onlinetime");
				data.pvpKills = rset.getInt("pvpkills");
				data.pkKills = rset.getInt("pkkills");
				data.fame = rset.getInt("fame");
				
				final int[] sub = new int[3];
				sub[0] = rset.getInt("base_class");
				sub[1] = rset.getInt("level");
				sub[2] = 1;
				data.subClassIdLvlBase.add(sub);
			}
			statement.close();
			rset.close();
			
			statement = con.prepareStatement("SELECT count FROM `items` WHERE `owner_id` = '" + playerId + "' AND item_id=57");
			rset = statement.executeQuery();
			if (rset.next())
			{
				data.adenaCount = rset.getLong("count");
			}
			statement.close();
			rset.close();

			statement = con.prepareStatement("SELECT class_id,level FROM `character_subclasses` WHERE `charId` = '" + playerId + "'");
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int[] sub = new int[3];
				sub[0] = rset.getInt("class_id");
				sub[1] = rset.getInt("level");
				sub[2] = 0;
				data.subClassIdLvlBase.add(sub);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error in getSinglePetitionPlayerData:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return data;
	}

	private class ClanPetitionData
	{
		String char_name;
		String online;
		int pvpKills;
		long onlineTime;
		boolean isNoble;
	}

	private ClanPetitionData getClanPetitionsData(Player player, int senderId)
	{
		final ClanPetitionData data = new ClanPetitionData();
		final Player sender = GameObjectsStorage.getPlayer(senderId);
		boolean haveclan = false;
		if (sender != null)
		{
			data.char_name = sender.getName(null);
			data.online = "<font color=6a9b54>" + ServerStorage.getInstance().getString(player.getLang(), "Util.TRUE") + "</font>";
			data.pvpKills = sender.getPvpKills();
			data.onlineTime = sender.getTotalOnlineTime();
			data.isNoble = sender.isNoble();
			haveclan = sender.getClanId() > 0;
		}
		else
		{
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT char_name,pvpkills,onlinetime,clanid FROM characters WHERE charId = '" + senderId + "'");
				rset = statement.executeQuery();
				if (rset.next())
				{
					data.char_name = rset.getString("char_name");
					data.online = "<font color=9b5454>" + ServerStorage.getInstance().getString(player.getLang(), "Util.FALSE") + "</font>";
					data.pvpKills = rset.getInt("pvpkills");
					data.onlineTime = rset.getLong("onlinetime");
					if (rset.getInt("clanid") > 0)
					{
						haveclan = true;
					}
				}
				statement.close();
				rset.close();
				
				statement = con.prepareStatement("SELECT nobless FROM characters WHERE charId = '" + senderId + "'");
				rset = statement.executeQuery();
				if (rset.next())
				{
					if (rset.getInt("nobless") > 0)
					{
						data.isNoble = true;
					}
				}
			}
			catch (final Exception e)
			{
				_log.warn("Error in getClanPetitionsData:", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}

		if (haveclan)
		{
			return null;
		}
		else
		{
			return data;
		}
	}

	private String getConvertedAdena(Player player, long adena)
	{
		String text = "";
		final String convertedAdena = String.valueOf(adena);
		final int ks = (convertedAdena.length() - 1) / 3;
		final long firstValue = adena / (long) (Math.pow(1000, ks));
		text = firstValue + getKs(player, ks);
		if ((convertedAdena.length() - 2) / 3 < ks)
		{
			adena -= firstValue * (long) (Math.pow(1000, ks));
			if (adena / (long) (Math.pow(1000, (ks - 1))) > 0)
			{
				text += " " + adena / (int) (Math.pow(1000, (ks - 1))) + getKs(player, ks - 1);
			}
		}
		return text;
	}

	private String getKs(Player player, int howMany)
	{
		String x = "";
		for (int i = 0; i < howMany; i++)
		{
			x += ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.K");
		}
		return x;
	}

	public String getUnitName(Player player, int type)
	{
		String subUnitName = "";
		switch (type)
		{
			case 0 :
				subUnitName = ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MAIN_CLAN");
				break;
			case Clan.SUBUNIT_ROYAL1 :
			case Clan.SUBUNIT_ROYAL2 :
				subUnitName = ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.ROYAL_GUARD");
				break;
			default :
				subUnitName = ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.ORDER_KNIGHT");
		}
		return subUnitName;
	}

	private String getMainStatsTableColor(int index)
	{
		return index % 2 == 0 ? "222320" : "191919";
	}

	private boolean manageRecrutationWindow(Player player, int actionToken, String wholeText)
	{
		final Clan clan = player.getClan();

		boolean failedAction = false;
		switch (actionToken)
		{
			case 1 :
				clan.getClassesNeeded().clear();
				for (int i = 88; i <= 118; i++)
				{
					clan.addClassNeeded(i);
				}
				break;
			case 2 :
				clan.getClassesNeeded().clear();
				break;
			case 3 :
				if (wholeText.length() > 2)
				{
					final String clazz = wholeText.substring(2);
					for (int i = 0; i < ALL_CLASSES.length; i++)
					{
						final String className = Util.className(player, (ALL_CLASSES[i].substring(0, 1).toLowerCase() + ALL_CLASSES[i].substring(1)));
						if (className.equals(clazz))
						{
							clan.addClassNeeded(88 + i);
							break;
						}
					}
				}
				break;
			case 5 :
				final String clazz = wholeText.substring(2);
				for (int i = 0; i < ALL_CLASSES.length; i++)
				{
					final String className = Util.className(player, (ALL_CLASSES[i].substring(0, 1).toLowerCase() + ALL_CLASSES[i].substring(1)));
					if (className.equals(clazz))
					{
						clan.deleteClassNeeded(88 + i);
						break;
					}
				}
				break;
			case 6 :
				final String[] questions = clan.getQuestions();
				final StringTokenizer st = new StringTokenizer(wholeText.substring(2), "|");
				for (int i = 0; i < 8; i++)
				{
					final String question = st.nextToken();
					if (question.length() > 3)
					{
						questions[i] = question.substring(0, Math.min(question.length(), MAX_QUESTION_LEN));
					}
					clan.setQuestions(questions);
				}
				break;
			case 7 :
				clan.setRecrutating(!clan.isRecruting());
				break;
			default :
				failedAction = true;
		}

		if (!failedAction)
		{
			clan.updateRecrutationData();
		}
		return false;
	}

	private boolean manageClanJoinWindow(Player player, Clan clan, String text)
	{
		final StringTokenizer st = new StringTokenizer(text, "|");
		final String[] answers = new String[8];
		for (int i = 0; i < 8; i++)
		{
			final String answer = st.nextToken();
			answers[i] = answer.substring(0, Math.min(answer.length(), MAX_ANSWER_LEN));
		}
		String comment = st.nextToken();
		comment = comment.substring(0, Math.min(comment.length(), MAX_COMMENT_LEN));
		return clan.addPetition(player.getObjectId(), answers, comment);
	}

	private void managePlayerPetition(Player player, int senderId, int action)
	{
		final Player sender = GameObjectsStorage.getPlayer(senderId);
		final Clan clan = player.getClan();
		var sucsess = false;
		switch (action)
		{
			case 1 :
				int type = -1;
				for (final ClanMember unit : clan.getMembers())
				{
					if (clan.getSubPledgeMembersCount(unit.getPledgeType()) < clan.getMaxNrOfMembers(unit.getPledgeType()))
					{
						type = unit.getPledgeType();
					}
				}

				if (type == -1)
				{
					player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.CLAN_FULL").toString());
					onBypassCommand("_bbsclanplayerpetition_" + senderId, player);
					return;
				}
				
				if (sender != null && sender.getClanId() <= 0)
				{
					sender.setPledgeType(type);
					if (type == Clan.SUBUNIT_ACADEMY)
					{
						sender.setPowerGrade(9);
						sender.setLvlJoinedAcademy(sender.getLevel());
					}
					else
					{
						sender.setPowerGrade(5);
					}
					clan.addClanMember(sender);
					sender.setClanPrivileges(clan.getRankPrivs(sender.getPowerGrade()));
					
					sender.sendPacket(SystemMessageId.ENTERED_THE_CLAN);
					
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_JOINED_CLAN);
					sm.addString(sender.getName(null));
					clan.broadcastToOnlineMembers(sm);
					sm = null;
					
					if (clan.getCastleId() > 0)
					{
						CastleManager.getInstance().getCastleByOwner(clan).giveResidentialSkills(sender);
					}
					if (clan.getFortId() > 0)
					{
						FortManager.getInstance().getFortByOwner(clan).giveResidentialSkills(sender);
					}
					if (clan.getHideoutId() > 0)
					{
						final var hall = ClanHallManager.getInstance().getAbstractHallByOwner(clan);
						if (hall != null)
						{
							hall.giveResidentialSkills(sender);
						}
					}
					sender.sendSkillList(false);
					
					clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(sender), sender);
					clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
					
					sender.sendPacket(new PledgeShowMemberListAll(clan, sender));
					sender.setClanJoinExpiryTime(0);
					sender.broadcastCharInfo();
					sucsess = true;
				}
				else
				{
					var haveclan = false;
					
					Connection con = null;
					PreparedStatement statement = null;
					ResultSet rset = null;
					try
					{
						con = DatabaseFactory.getInstance().getConnection();
						statement = con.prepareStatement("SELECT clanid FROM characters WHERE charId = '" + senderId + "'");
						rset = statement.executeQuery();
						if (rset.next())
						{
							if (rset.getInt("clanid") > 0)
							{
								haveclan = true;
							}
						}
					}
					catch (final Exception e)
					{
						_log.warn("Error in getClanPetitionsData:", e);
					}
					finally
					{
						DbUtils.closeQuietly(con, statement, rset);
					}
					
					if (!haveclan)
					{
						int powerGrade = 5;
						if (type == Clan.SUBUNIT_ACADEMY)
						{
							powerGrade = 9;
						}
						
						Connection conn = null;
						PreparedStatement statementt = null;
						try
						{
							conn = DatabaseFactory.getInstance().getConnection();
							statementt = conn.prepareStatement("UPDATE characters SET clanid=" + clan.getId() + ", subpledge=" + type + ", power_grade=" + powerGrade + " WHERE charId=" + senderId + " AND clanid=0");
							statementt.execute();
						}
						catch (final Exception e)
						{
							_log.warn("Error in managePlayerPetition:", e);
						}
						finally
						{
							DbUtils.closeQuietly(conn, statementt);
						}
						clan.addClanMember(getSubUnitMember(clan, type, senderId));
						clan.broadcastClanStatus();
						sucsess = true;
					}
				}
				clan.deletePetition(senderId);
				if (sucsess)
				{
					player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.HAS_BEED_ADD").toString());
				}
				onBypassCommand("_bbsclanpetitions_" + clan.getId() + "_0", player);
				break;
			case 2 :
				clan.deletePetition(senderId);
				if (action == 2)
				{
					player.sendMessage(ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.PETITION_DELETE").toString());
					onBypassCommand("_bbsclanpetitions_" + clan.getId() + "_0", player);
				}
				break;
		}
	}

	protected ClanMember getSubUnitMember(Clan clan, int type, int memberId)
	{
		ClanMember member = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_name,level,classid,pvpkills,charId,subpledge,title,power_grade,apprentice,sponsor,sex,race FROM characters WHERE charId=?");
			statement.setInt(1, memberId);
			rset = statement.executeQuery();
			if (rset.next())
			{
				member = new ClanMember(clan, rset);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error in managePlayerPetition:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return member;
	}

	private final ClanComparator _clansComparator = new ClanComparator();

	private class ClanComparator implements Comparator<Clan>
	{
		@Override
		public int compare(Clan o1, Clan o2)
		{
			if (o1.getLevel() > o2.getLevel())
			{
				return -1;
			}
			if (o2.getLevel() > o1.getLevel())
			{
				return 1;
			}
			if (o1.getReputationScore() > o2.getReputationScore())
			{
				return -1;
			}
			if (o2.getReputationScore() > o1.getReputationScore())
			{
				return 1;
			}
			return 0;
		}
	}

	private String[] getNotChosenClasses(Player player, Clan clan)
	{
		final String[] splited =
		{
		        "", ""
		};

		final ArrayList<Integer> classes = clan.getClassesNeeded();

		for (int i = 0; i < ALL_CLASSES.length; i++)
		{
			if (!classes.contains(i + 88))
			{
				int x = 1;
				if (i % 2 == 0)
				{
					x = 0;
				}
				if (!splited[x].equals(""))
				{
					splited[x] += ";";
				}
				splited[x] += Util.className(player, (ALL_CLASSES[i].substring(0, 1).toLowerCase() + ALL_CLASSES[i].substring(1)));
			}
		}
		return splited;
	}

	private void clanNotice(Player player, int clanId)
	{
		final Clan cl = ClanHolder.getInstance().getClan(clanId);
		if (cl != null)
		{
			if (cl.getLevel() < 2)
			{
				player.sendPacket(SystemMessageId.NO_CB_IN_MY_CLAN);
				onBypassCommand("_bbsclclan_" + player.getClanId(), player);
			}
			else
			{
				String html = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/clan_notice.htm");
				if (player.getClan().isNoticeEnabled())
				{
					html = html.replace("%status%", "" + ServerStorage.getInstance().getString(player.getLang(), "ClanBBS.NOTICE_FUNCTION") + ":&nbsp;&nbsp;&nbsp;" + ServerStorage.getInstance().getString(player.getLang(), "ClanBBS.ON") + "&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;<a action=\"bypass -h _bbsclannoticedisable\">" + ServerStorage.getInstance().getString(player.getLang(), "ClanBBS.OFF") + "</a>");
				}
				else
				{
					html = html.replace("%status%", "" + ServerStorage.getInstance().getString(player.getLang(), "ClanBBS.NOTICE_FUNCTION") + ":&nbsp;&nbsp;&nbsp;<a action=\"bypass -h _bbsclannoticeenable\">" + ServerStorage.getInstance().getString(player.getLang(), "ClanBBS.ON") + "</a>&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;" + ServerStorage.getInstance().getString(player.getLang(), "ClanBBS.OFF") + "");
				}
				html = html.replace("%clanId%", String.valueOf(clanId));
				String buttons = "";
				final var button_join = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_join.htm");
				final var button_manage = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_manage.htm");
				final var button_petition = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_petition.htm");
				final var button_notice = HtmCache.getInstance().getHtm(player, player.getLang(), "data/html/community/clan/button_notice.htm");
				if (player.getClan() == null)
				{
					var info = button_join;
					info = info.replace("%bypass%", "_bbsclanjoin_" + cl.getId() + "_0");
					info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.JOIN_CLAN"));
					buttons += info;
				}
				
				if (player.getClan() != null && player.getClan().equals(cl) && player.getClan().getLeaderId() == player.getObjectId())
				{
					var info = button_manage;
					info = info.replace("%bypass%", "_bbsclanmanage_0");
					info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_RECRUTE"));
					buttons += info;
					
					info = button_petition;
					info = info.replace("%bypass%", "_bbsclanpetitions_" + cl.getId() + "_0");
					info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_PETITION"));
					buttons += info;
					
					info = button_notice;
					info = info.replace("%bypass%", "_bbsclannoticeform");
					info = info.replace("%name%", ServerStorage.getInstance().getString(player.getLang(), "CommunityClan.MANAGE_NOTICE"));
					buttons += info;
				}
				
				if (buttons.isEmpty())
				{
					buttons += "<tr><td>&nbsp;</td></tr>";
				}
				html = html.replace("%buttons%", buttons);
				html = html.replace("%allyInfo%", getAllyInfo(player, cl));
				send1001(html, player);
				send1002(player, player.getClan().getNotice(), " ", "0");
			}
		}
	}

	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
		if (command.equals("Notice"))
		{
			if (ar1.equals("Set"))
			{
				player.getClan().setNotice(ar4);
				player.sendPacket(SystemMessageId.CLAN_NOTICE_SAVED);
				onBypassCommand("_bbsclannoticeform", player);
			}
		}
	}
	
	public static CommunityClan getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityClan _instance = new CommunityClan();
	}
}
