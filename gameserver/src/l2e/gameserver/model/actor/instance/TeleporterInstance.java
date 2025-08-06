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

import java.util.Calendar;
import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.data.parser.TeleLocationParser;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.TownManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.TeleportTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.zone.type.TownZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class TeleporterInstance extends NpcInstance
{
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_OWNER = 2;
	private static final int COND_REGULAR = 3;

	public TeleporterInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.TeleporterInstance);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		player.sendActionFailed();
		
		final int condition = validateCondition(player);

		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		if ((player.getFirstEffect(6201) != null) || (player.getFirstEffect(6202) != null) || (player.getFirstEffect(6203) != null))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			final String filename = "data/html/teleporter/epictransformed.htm";
			
			html.setFile(player, player.getLang(), filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%npcname%", getName(player.getLang()));
			player.sendPacket(html);
			return;
		}
		else if (actualCommand.equalsIgnoreCase("goto"))
		{
			final int npcId = getId();
			
			switch (npcId)
			{
				case 32534 :
				case 32539 :
					if (player.isFlyingMounted())
					{
						player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_SEED_IN_FLYING_TRANSFORM);
						return;
					}
					break;
			}
			
			if (st.countTokens() <= 0)
			{
				return;
			}
			
			final int whereTo = Integer.parseInt(st.nextToken());
			if (condition == COND_REGULAR)
			{
				doTeleport(player, whereTo);
				return;
			}
			else if (condition == COND_OWNER)
			{
				int minPrivilegeLevel = 0;
				if (st.countTokens() >= 1)
				{
					minPrivilegeLevel = Integer.parseInt(st.nextToken());
				}
				
				if (10 >= minPrivilegeLevel)
				{
					doTeleport(player, whereTo);
				}
				else
				{
					player.sendMessage("You don't have the sufficient access level to teleport there.");
				}
				return;
			}
		}
		else if (command.startsWith("Chat"))
		{
			final Calendar cal = Calendar.getInstance();
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (final IndexOutOfBoundsException ioobe)
			{}
			catch (final NumberFormatException nfe)
			{}
			
			if ((val == 1) && (player.getLevel() < 41))
			{
				showNewbieHtml(player);
				return;
			}
			else if ((val == 1) && (cal.get(Calendar.HOUR_OF_DAY) >= 20) && (cal.get(Calendar.HOUR_OF_DAY) <= 23) && ((cal.get(Calendar.DAY_OF_WEEK) == 1) || (cal.get(Calendar.DAY_OF_WEEK) == 7)))
			{
				showHalfPriceHtml(player);
				return;
			}
			showChatWindow(player, val);
		}
		
		super.onBypassFeedback(player, command);
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
		
		return "data/html/teleporter/" + pom + ".htm";
	}
	
	private void showNewbieHtml(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		String filename = "data/html/teleporter/free/" + getTemplate().getId() + ".htm";
		if (!HtmCache.getInstance().isLoadable(filename))
		{
			filename = "data/html/teleporter/" + getTemplate().getId() + "-1.htm";
		}
		
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName(player.getLang()));
		player.sendPacket(html);
	}
	
	private void showHalfPriceHtml(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		String filename = "data/html/teleporter/half/" + getId() + ".htm";
		if (!HtmCache.getInstance().isLoadable(filename))
		{
			filename = "data/html/teleporter/" + getId() + "-1.htm";
		}
		
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName(player.getLang()));
		player.sendPacket(html);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		String filename = "data/html/teleporter/castleteleporter-no.htm";
		
		final int condition = validateCondition(player);
		if (condition == COND_REGULAR)
		{
			super.showChatWindow(player);
			return;
		}
		else if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				filename = "data/html/teleporter/castleteleporter-busy.htm";
			}
			else if (condition == COND_OWNER)
			{
				filename = getHtmlPath(getId(), 0);
			}
		}
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, player.getLang(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName(player.getLang()));
		player.sendPacket(html);
	}
	
	private void doTeleport(Player player, int val)
	{
		final TeleportTemplate list = TeleLocationParser.getInstance().getTemplate(val);
		if (list != null)
		{
			final var loc = list.getLocation();
			final boolean notNeedCheck = (val == 122) || (val == 123) || (val == 200916);
			final TownZone town = TownManager.getTown(loc.getX(), loc.getY(), loc.getZ());
			if (town != null && TownManager.townHasCastleInSiege(loc.getX(), loc.getY()) && !notNeedCheck)
			{
				player.sendPacket(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE);
				return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (player.getKarma() > 0))
			{
				player.sendMessage("Go away, you're not welcome here.");
				return;
			}
			else if(player.checkInTournament())
			{
				player.sendMessage("You can't use Gatekeeper when you are registered to Tournament.");
				return;
			}
			else if (player.isCombatFlagEquipped())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_IN_POSSESSION_OF_A_WARD);
				return;
			}
			else if (list.isForNoble() && !player.isNoble())
			{
				final String filename = "data/html/teleporter/nobleteleporter-no.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName(player.getLang()));
				player.sendPacket(html);
				return;
			}
			else if (player.isAlikeDead())
			{
				return;
			}
			
			final Calendar cal = Calendar.getInstance();
			long price = list.getPrice();
			
			if (player.getLevel() < list.getMinLevel() || player.getLevel() > list.getMaxLevel())
			{
				final var sm = SystemMessage.getSystemMessage(SystemMessageId.C1_LEVEL_REQUIREMENT_NOT_SUFFICIENT);
				sm.addPcName(player);
				player.sendPacket(sm);
				return;
			}
			
			if (player.getLevel() < 41)
			{
				price = 0;
			}
			else if (!list.isForNoble())
			{
				if ((cal.get(Calendar.HOUR_OF_DAY) >= 20) && (cal.get(Calendar.HOUR_OF_DAY) <= 23) && ((cal.get(Calendar.DAY_OF_WEEK) == 1) || (cal.get(Calendar.DAY_OF_WEEK) == 7)))
				{
					price /= 2;
				}
			}
			
			if ((Config.ALT_GAME_FREE_TELEPORT || price <= 0) || (price > 0 && player.destroyItemByItemId("Teleport " + (list.isForNoble() ? " nobless" : ""), list.getId(), price, this, true)))
			{
				if (Config.DEBUG)
				{
					_log.info("Teleporting player " + player.getName(null) + " to new location: " + loc.toString());
				}
				
				if (BotFunctions.getInstance().isAutoTpGotoEnable(player))
				{
					BotFunctions.getInstance().getAutoGotoTeleport(player, player.getLocation(), loc);
					return;
				}
				player.teleToLocation(loc, true, ReflectionManager.DEFAULT);
			}
		}
		else
		{
			_log.warn("No teleport destination with id:" + val);
		}
		player.sendActionFailed();
	}
	
	private int validateCondition(Player player)
	{
		if (CastleManager.getInstance().getCastleIndex(this) < 0)
		{
			return COND_REGULAR;
		}
		else if (getCastle().getSiege().getIsInProgress())
		{
			return COND_BUSY_BECAUSE_OF_SIEGE;
		}
		else if (player.getClan() != null)
		{
			if (getCastle().getOwnerId() == player.getClanId())
			{
				return COND_OWNER;
			}
		}
		return COND_ALL_FALSE;
	}
}