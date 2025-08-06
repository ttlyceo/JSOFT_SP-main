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
import java.util.StringTokenizer;

import l2e.commons.dbutils.DbUtils;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.ClassListParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.CreatureSay;
import l2e.gameserver.network.serverpackets.L2Friend;
import l2e.gameserver.network.serverpackets.ShowBoard;
import l2e.gameserver.network.serverpackets.SystemMessage;

/**
 * Created by LordWinter 05.07.2013 Fixed by L2J Eternity-World
 */
public class CommunityFriend extends AbstractCommunity implements ICommunityBoardHandler
{
	public CommunityFriend()
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
		        "_friendlist_0_", "Friends"
		};
	}
	
	@Override
	public void onBypassCommand(String command, Player activeChar)
	{
		if (!checkCondition(activeChar, new StringTokenizer(command, "_").nextToken(), false, false))
		{
			return;
		}
		
		if (command.equals("_friendlist_0_"))
		{
			showFriendsList(activeChar);
		}
		else if (command.startsWith("_friendlist_0_;playerdelete;"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final String name = st.nextToken();
			deleteFriend(activeChar, name);
			showFriendsList(activeChar);
		}
		else if (command.startsWith("_friendlist_0_;playerinfo;"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final String name = st.nextToken();
			showFriendsInfo(activeChar, name);
		}
		else
		{
			final ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101", activeChar);
			activeChar.sendPacket(sb);
			activeChar.sendPacket(new ShowBoard(null, "102", activeChar));
			activeChar.sendPacket(new ShowBoard(null, "103", activeChar));
		}
	}

	private void deleteFriend(Player activeChar, String name)
	{
		final int id = CharNameHolder.getInstance().getIdByName(name);

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_friends WHERE (charId=? AND friendId=?) OR (charId=? AND friendId=?)");
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, id);
			statement.setInt(3, id);
			statement.setInt(4, activeChar.getObjectId());
			statement.execute();

			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST);
			sm.addString(name);
			activeChar.sendPacket(sm);

			activeChar.getFriendList().remove(Integer.valueOf(id));
			activeChar.sendPacket(new L2Friend(false, id));
		}
		catch (final Exception e)
		{
			_log.warn("could not del friend objectid: " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void showFriendsInfo(Player activeChar, String name)
	{
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/friends/friendinfo.htm");
		final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/friends/friendinfo-template.htm");
		String block = "";
		String list = "";

		final Player player = GameObjectsStorage.getPlayer(name);
		if (player != null)
		{
			block = template;
			block = block.replace("%friendName%", player.getName(null));
			block = block.replace("%sex%", player.getAppearance().getSex() ? ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.FEMALE") : ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.MALE"));
			block = block.replace("%class%", ClassListParser.getInstance().getClass(player.getClassId()).getClientCode());
			block = block.replace("%level%", String.valueOf(player.getLevel()));
			block = block.replace("%clan%", player.getClan() != null ? player.getClan().getName() : ServerStorage.getInstance().getString(player.getLang(), "CommunityRanking.NO_CLAN"));
			list += block;
		}
		else
		{
			list = "<center>" + ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.NOT_PLAYER") + " " + name + "!</center>";
		}
		html = html.replace("%info%", list);
		separateAndSend(html, activeChar);
	}

	private void showFriendsList(Player activeChar)
	{
		String html = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/friends/friendslist.htm");
		final String template = HtmCache.getInstance().getHtm(activeChar, activeChar.getLang(), "data/html/community/friends/friends-template.htm");
		String block = "";
		String list = "";

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=?");
			statement.setInt(1, activeChar.getObjectId());
			rset = statement.executeQuery();
			while (rset.next())
			{
				final int friendId = rset.getInt("friendId");
				final String friendName = CharNameHolder.getInstance().getNameById(friendId);
				final Player friend = friendId != 0 ? GameObjectsStorage.getPlayer(friendId) : GameObjectsStorage.getPlayer(friendName);

				block = template;
				block = block.replace("%friendName%", friend == null ? friendName : "<a action=\"bypass -h _friendlist_0_;playerinfo;" + friendName + "\">" + friendName + "</a>");
				block = block.replace("%status%", friend == null ? "<font color=\"D70000\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.OFF") + "</font>" : "<font color=\"00CC00\">" + ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.ON") + "</font>");
				block = block.replace("%action%", friend == null ? "" : "<button value=\"" + ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.DELETE") + "\" action=\"bypass -h _friendlist_0_;playerdelete;" + friendName + "\" width=60 height=21 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
				list += block;
			}
		}
		catch (final Exception e)
		{
			list = ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.CAN_T_SHOW");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		if (list.isEmpty())
		{
			list = ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.LIST_EMPTY");
		}
		html = html.replace("%list%", list);
		separateAndSend(html, activeChar);
	}

	@Override
	public void onWriteCommand(String command, String ar1, String ar2, String ar3, String ar4, String ar5, Player activeChar)
	{
		if (command.equals("Friends"))
		{
			if (ar1.equals("PM"))
			{
				try
				{
					final Player reciever = GameObjectsStorage.getPlayer(ar2);
					if (reciever == null)
					{
						activeChar.sendMessage(ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.NOT_FOUND"));
						onBypassCommand("_friendlist_0_;playerinfo;" + ar2, activeChar);
						return;
					}
					if (activeChar.isChatBanned())
					{
						activeChar.sendMessage(ServerStorage.getInstance().getString(activeChar.getLang(), "FriendsBBS.BANNED"));
						onBypassCommand("_friendlist_0_;playerinfo;" + reciever.getName(null), activeChar);
						return;
					}
					if (!reciever.getMessageRefusal())
					{
						reciever.sendPacket(new CreatureSay(0, Say2.TELL, activeChar.getName(null), ar3));
						activeChar.sendPacket(new CreatureSay(0, Say2.TELL, activeChar.getName(null), ar3));
						onBypassCommand("_friendlist_0_;playerinfo;" + reciever.getName(null), activeChar);
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
						onBypassCommand("_friendlist_0_;playerinfo;" + reciever.getName(null), activeChar);
					}
				}
				catch (final StringIndexOutOfBoundsException e)
				{}
			}
			else
			{
				final ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + ar1 + " is not implemented yet</center><br><br></body></html>", "101", activeChar);
				activeChar.sendPacket(sb);
				activeChar.sendPacket(new ShowBoard(null, "102", activeChar));
				activeChar.sendPacket(new ShowBoard(null, "103", activeChar));
			}
		}
	}
	
	public static CommunityFriend getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final CommunityFriend _instance = new CommunityFriend();
	}
}