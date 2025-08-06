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
import l2e.gameserver.data.parser.TeleLocationParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.UndergroundColiseumManager;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.TeleportTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.underground_coliseum.UCArena;
import l2e.gameserver.model.entity.underground_coliseum.UCBestTeam;
import l2e.gameserver.model.entity.underground_coliseum.UCWaiting;
import l2e.gameserver.model.olympiad.OlympiadManager;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class UCManagerInstance extends NpcInstance
{
	public UCManagerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.UCManagerInstance);
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
		
		return "data/html/underground_coliseum/" + pom + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		final StringTokenizer token = new StringTokenizer(command, " ");
		final String actualCommand = token.nextToken();
		
		if (actualCommand.equalsIgnoreCase("register"))
		{
			try
			{
				if (!player.isInParty())
				{
					html.setFile(player, player.getLang(), "data/html/underground_coliseum/noTeam.htm");
					player.sendPacket(html);
					return;
				}
				
				if (player.isInKrateisCube() || OlympiadManager.getInstance().isRegistered(player) || player.isInOlympiadMode() || player.isInFightEvent() || player.isRegisteredInFightEvent() || player.getTeam() != 0)
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_BE_SIMULTANEOUSLY_REGISTERED_FOR_PVP_MATCHES_SUCH_AS_THE_OLYMPIAD_UNDERGROUND_COLISEUM_AERIAL_CLEFT_KRATEIS_CUBE_AND_HANDYS_BLOCK_CHECKERS);
					return;
				}
				
				if (player.isCursedWeaponEquipped())
				{
					player.sendPacket(SystemMessageId.CANNOT_REGISTER_PROCESSING_CURSED_WEAPON);
					return;
				}
				
				if (!UndergroundColiseumManager.getInstance().isStarted())
				{
					html.setFile(player, player.getLang(), "data/html/underground_coliseum/notStarted.htm");
					player.sendPacket(html);
					return;
				}
				
				if (!player.getParty().isLeader(player))
				{
					html.setFile(player, player.getLang(), "data/html/underground_coliseum/notPartyLeader.htm");
					player.sendPacket(html);
					return;
				}
				
				if (player.getParty().getUCState() instanceof UCWaiting)
				{
					html.setFile(player, player.getLang(), "data/html/underground_coliseum/alreadyRegistered.htm");
					player.sendPacket(html);
					return;
				}
				
				final int val = Integer.parseInt(token.nextToken());
				final UCArena arena = UndergroundColiseumManager.getInstance().getArena(val);
				if (arena == null)
				{
					player.sendMessage("This arena is temporarly unavailable.");
					return;
				}
				
				if (arena.getTeams()[0].getParty() != null || arena.getTeams()[1].getParty() != null)
				{
					if (arena.getTeams()[0].getParty() == player.getParty() || arena.getTeams()[1].getParty() == player.getParty())
					{
						html.setFile(player, player.getLang(), "data/html/underground_coliseum/alreadyRegistered.htm");
						player.sendPacket(html);
						return;
					}
				}
				
				int realCount = 0;
				
				for (final Player member : player.getParty().getMembers())
				{
					if (member == null)
					{
						continue;
					}
					
					if (member.getClassId().level() < 2)
					{
						final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
						packet.setFile(member, member.getLang(), "data/html/underground_coliseum/wrongLevel.htm");
						packet.replace("%name%", member.getName(null));
						player.sendPacket(packet);
						return;
					}
					
					if (!((member.getLevel() >= arena.getMinLevel()) && (member.getLevel() <= arena.getMaxLevel())))
					{
						final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
						packet.setFile(member, member.getLang(), "data/html/underground_coliseum/wrongLevel.htm");
						packet.replace("%name%", member.getName(null));
						player.sendPacket(packet);
						return;
					}
					realCount++;
				}
				
				if (realCount < Config.UC_PARTY_LIMIT)
				{
					final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
					packet.setFile(player, player.getLang(), "data/html/underground_coliseum/notEnoughMembers.htm");
					player.sendPacket(packet);
					return;
				}
				
				if (arena.getWaitingList().size() >= 5)
				{
					final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
					packet.setFile(player, player.getLang(), "data/html/underground_coliseum/arenaFull.htm");
					player.sendPacket(packet);
					return;
				}
				
				final UCWaiting waiting = new UCWaiting(player.getParty(), arena);
				arena.getWaitingList().add(waiting);
				waiting.setParty(true);
				waiting.hasRegisterdNow();
				html.setFile(player, player.getLang(), "data/html/underground_coliseum/registered.htm");
				player.sendPacket(html);
				if (arena.getWaitingList().size() >= 2 && !arena.isBattleNow())
				{
					arena.runTaskNow();
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (actualCommand.equalsIgnoreCase("cancel"))
		{
			if ((player.getParty() == null) || ((player.getParty() != null) && !player.getParty().isLeader(player)))
			{
				return;
			}
			
			if (player.getParty().getUCState() instanceof UCWaiting)
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
				final UCWaiting waiting = (UCWaiting) player.getParty().getUCState();
				
				waiting.setParty(false);
				waiting.clean();
				waiting.getBaseArena().getWaitingList().remove(waiting);
				packet.setFile(player, player.getLang(), "data/html/underground_coliseum/registrantionCanceled.htm");
				player.sendPacket(packet);
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("bestTeam"))
		{
			final int val = Integer.parseInt(token.nextToken());
			final UCArena arena = UndergroundColiseumManager.getInstance().getArena(val);
			final UCBestTeam bestTeam = UndergroundColiseumManager.getInstance().getBestTeam(arena.getId());
			if (bestTeam != null)
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
				packet.setFile(player, player.getLang(), "data/html/underground_coliseum/bestTeam.htm");
				packet.replace("%name%", bestTeam.getLeaderName());
				packet.replace("%best%", String.valueOf(bestTeam.getWins()));
				player.sendPacket(packet);
			}
			else
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
				packet.setFile(player, player.getLang(), "data/html/underground_coliseum/view-most-wins.htm");
				player.sendPacket(packet);
			}
		}
		else if (actualCommand.equalsIgnoreCase("listTeams"))
		{
			final int val = Integer.parseInt(token.nextToken());
			
			final UCArena arena = UndergroundColiseumManager.getInstance().getArena(val);
			if (arena == null)
			{
				player.sendMessage("This arena is temporarly unavailable.");
				return;
			}
			
			final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
			packet.setFile(player, player.getLang(), "data/html/underground_coliseum/view-participating-teams.htm");
			
			String list = "";
			int i = 0;
			final int currentReg = arena.getWaitingList().size();
			for (i = 1; i <= 5; i++)
			{
				if (i > currentReg)
				{
					list += i + ". (Participating Team: Team)<br>";
				}
				else
				{
					final Party party = arena.getWaitingList().get(i - 1).getParty();
					if (party == null || party.getMemberCount() < 2)
					{
						list += i + ". (Participating Team: Team)<br>";
					}
					else
					{
						String teamList = "";
						for (final Player m : party.getMembers())
						{
							if (m != null)
							{
								teamList += m.getName(null) + ";";
							}
						}
						list += i + ". (Participating Team: <font color=00ffff>" + teamList + "</font>)<br>";
					}
				}
			}
			packet.replace("%list%", list);
			player.sendPacket(packet);
		}
		else if (actualCommand.equalsIgnoreCase("goto"))
		{
			if (token.countTokens() <= 0)
			{
				return;
			}
			
			final int whereTo = Integer.parseInt(token.nextToken());
			doTeleport(player, whereTo);
			return;
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	private void doTeleport(Player player, int val)
	{
		final TeleportTemplate list = TeleLocationParser.getInstance().getTemplate(val);
		if (list != null)
		{
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (player.getKarma() > 0))
			{
				player.sendMessage("Go away, you're not welcome here.");
				return;
			}
			else if (player.isCombatFlagEquipped())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_IN_POSSESSION_OF_A_WARD);
				return;
			}
			else if (player.isAlikeDead())
			{
				return;
			}
			
			final Calendar cal = Calendar.getInstance();
			long price = list.getPrice();
			
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
			
			if (Config.ALT_GAME_FREE_TELEPORT || player.destroyItemByItemId("Teleport", list.getId(), price, this, true))
			{
				if (BotFunctions.getInstance().isAutoTpGotoEnable(player))
				{
					BotFunctions.getInstance().getAutoGotoTeleport(player, player.getLocation(), list.getLocation());
					return;
				}
				player.teleToLocation(list.getLocation(), true, ReflectionManager.DEFAULT);
			}
		}
		else
		{
			_log.warn("No teleport destination with id:" + val);
		}
		player.sendActionFailed();
	}
}