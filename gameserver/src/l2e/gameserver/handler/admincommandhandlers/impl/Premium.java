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
package l2e.gameserver.handler.admincommandhandlers.impl;

import java.util.StringTokenizer;

import l2e.gameserver.data.dao.CharacterPremiumDAO;
import l2e.gameserver.data.parser.PremiumAccountsParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.service.premium.PremiumTemplate;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class Premium implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_premium_menu", "admin_premium_give", "admin_clean_premium", "admin_premium_clan_give", "admin_addPremium", "admin_addClanPremium"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_premium_menu"))
		{
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_addPremium"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int premiumId = Integer.parseInt(st.nextToken());
				
				final GameObject targetChar = activeChar.getTarget();
				if (targetChar == null || !(targetChar instanceof Player))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					showMenu(activeChar);
					return false;
				}
				final Player targetPlayer = (Player) targetChar;
				if (givePremium(targetPlayer, premiumId))
				{
					activeChar.sendMessage("Preumium account is successfully added for: " + targetPlayer.getName(null));
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Failed to give premium account...");
			}
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_clean_premium"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				String charName = null;
				try
				{
					charName = st.nextToken();
				}
				catch (final Exception e)
				{
				}
				
				if (charName != null)
				{
					final Player player = GameObjectsStorage.getPlayer(charName);
					if (player != null)
					{
						if (!player.hasPremiumBonus())
						{
							activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
							showMenu(activeChar);
							return false;
						}
						else
						{
							player.getPersonalTasks().removeTask(2, true);
							player.removePremiumAccount(false);
							activeChar.sendMessage("Preumium account removed at : " + player.getName(null));
							showMenu(activeChar);
							return true;
						}
					}
					else
					{
						activeChar.sendMessage("Failed to clean premium account...");
						showMenu(activeChar);
						return false;
					}
				}
				else
				{
					final GameObject targetChar = activeChar.getTarget();
					if (targetChar == null)
					{
						activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
						showMenu(activeChar);
						return false;
					}
				
					if (!targetChar.isPlayer() || !targetChar.getActingPlayer().hasPremiumBonus())
					{
						activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
						showMenu(activeChar);
						return false;
					}
					
					targetChar.getActingPlayer().getPersonalTasks().removeTask(2, true);
					targetChar.getActingPlayer().removePremiumAccount(false);
					activeChar.sendMessage("Preumium account removed at : " + targetChar.getName(null));
					showMenu(activeChar);
					return true;
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Failed to clean premium account...");
			}
		}
		else if (command.startsWith("admin_addclanPremium"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				String premiumId = null;
				try
				{
					premiumId = st.nextToken();
				}
				catch (final Exception e)
				{}
				
				final GameObject targetChar = activeChar.getTarget();
				if (targetChar == null || !(targetChar instanceof Player))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					showMenu(activeChar);
					return false;
				}
				
				if (premiumId == null)
				{
					activeChar.sendMessage("Failed to give premium account...");
					showMenu(activeChar);
					return false;
				}
				
				final Player targetPlayer = (Player) targetChar;
				if (targetPlayer.getClan() == null)
				{
					if (givePremium(targetPlayer, Integer.parseInt(premiumId)))
					{
						activeChar.sendMessage("Preumium account is successfully added for: " + targetPlayer.getName(null));
					}
					showMenu(activeChar);
					return true;
				}
				else
				{
					for (final ClanMember member : targetPlayer.getClan().getMembers())
					{
						if (member != null && member.isOnline())
						{
							if (givePremium(member.getPlayerInstance(), Integer.parseInt(premiumId)))
							{
								activeChar.sendMessage("Preumium account is successfully added for: " + member.getName());
							}
						}
					}
					showMenu(activeChar);
					return true;
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Failed to give premium account...");
			}
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_premium_give"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				String charName = null;
				String premiumId = null;
				try
				{
					charName = st.nextToken();
				}
				catch (final Exception e)
				{}
				try
				{
					premiumId = st.nextToken();
				}
				catch (final Exception e)
				{}
				
				if (charName == null)
				{
					activeChar.sendMessage("Failed to give premium account...");
					showMenu(activeChar);
					return false;
				}
				
				final Player player = GameObjectsStorage.getPlayer(charName);
				if (player != null && premiumId != null)
				{
					if (givePremium(player, Integer.parseInt(premiumId)))
					{
						activeChar.sendMessage("Preumium account is successfully added for: " + player.getName(null));
					}
				}
				showMenu(activeChar);
				return true;
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Failed to give premium account...");
			}
			showMenu(activeChar);
			return true;
		}
		else if (command.startsWith("admin_premium_clan_give"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final String charName = st.nextToken();
				final int premiumId = Integer.parseInt(st.nextToken());
				final Player player = GameObjectsStorage.getPlayer(charName);
				if (player != null)
				{
					if (player.getClan() == null)
					{
						if (givePremium(player, premiumId))
						{
							activeChar.sendMessage("Preumium account is successfully added for: " + player.getName(null));
						}
					}
					else
					{
						for (final ClanMember member : player.getClan().getMembers())
						{
							if (member != null && member.isOnline())
							{
								if (givePremium(member.getPlayerInstance(), premiumId))
								{
									activeChar.sendMessage("Preumium account is successfully added for: " + member.getName());
								}
							}
						}
					}
					showMenu(activeChar);
					return true;
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Failed to give premium account...");
			}
			showMenu(activeChar);
			return true;
		}
		return true;
	}
	
	private void showMenu(Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/premium_menu.htm");
		activeChar.sendPacket(adminhtm);
	}
	
	private boolean givePremium(Player player, int premiumId)
	{
		if (player != null)
		{
			final PremiumTemplate template = PremiumAccountsParser.getInstance().getPremiumTemplate(premiumId);
			if (template != null)
			{
				final long time = !template.isOnlineType() ? (System.currentTimeMillis() + (template.getTime() * 1000)) : 0;
				if (template.isPersonal())
				{
					CharacterPremiumDAO.getInstance().updatePersonal(player, template.getId(), time, template.isOnlineType());
				}
				else
				{
					CharacterPremiumDAO.getInstance().update(player, template.getId(), time, template.isOnlineType());
				}
				
				if (player.isInParty())
				{
					player.getParty().recalculatePartyData();
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}