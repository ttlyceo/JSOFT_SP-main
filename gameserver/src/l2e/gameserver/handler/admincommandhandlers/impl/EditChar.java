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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.StringUtil;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.parser.ClassListParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.network.GameClient;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.GMViewItemList;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.PartySmallWindowAll;
import l2e.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import l2e.gameserver.network.serverpackets.SetSummonRemainTime;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class EditChar implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_edit_character", "admin_hp_percent", "admin_mp_percent", "admin_cp_percent", "admin_current_player", "admin_nokarma", "admin_setkarma", "admin_setfame", "admin_character_list", "admin_offline_list", "admin_total_list", "admin_fake_list", "admin_character_info", "admin_show_characters", "admin_find_character", "admin_find_ip", "admin_find_account", "admin_find_dualbox", "admin_strict_find_dualbox", "admin_save_modifications", "admin_rec", "admin_settitle", "admin_changename", "admin_setsex", "admin_setcolor", "admin_settcolor", "admin_setclass", "admin_setpk", "admin_setpvp", "admin_fullfood", "admin_remove_clan_penalty", "admin_summon_info", "admin_unsummon", "admin_summon_setlvl", "admin_show_pet_inv", "admin_partyinfo", "admin_setnoble", "admin_game_points", "admin_resetrec", "admin_charfarm"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		
		if (command.equals("admin_current_player"))
		{
			showCharacterInfo(activeChar, activeChar);
		}
		else if (command.startsWith("admin_character_info"))
		{
			final String[] data = command.split(" ");
			if ((data.length > 1))
			{
				showCharacterInfo(activeChar, GameObjectsStorage.getPlayer(data[1]));
			}
			else if (activeChar.getTarget() instanceof Player)
			{
				showCharacterInfo(activeChar, activeChar.getTarget().getActingPlayer());
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			}
		}
		else if (command.startsWith("admin_total_list"))
		{
			listCharacters(activeChar, 0, 1);
		}
		else if (command.startsWith("admin_fake_list"))
		{
			listCharacters(activeChar, 2, 1);
		}
		else if (command.startsWith("admin_character_list"))
		{
			listCharacters(activeChar, 1, 1);
		}
		else if (command.startsWith("admin_offline_list"))
		{
			listCharacters(activeChar, 3, 1);
		}
		else if (command.startsWith("admin_show_characters"))
		{
			try
			{
				final var val = command.substring(22);
				final String[] param = val.split(" ");
				final var type = Integer.parseInt(param[0]);
				final var page = Integer.parseInt(param[1]);
				listCharacters(activeChar, type, page);
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //show_characters <page_number>");
			}
		}
		else if (command.startsWith("admin_find_character"))
		{
			try
			{
				final var val = command.substring(21);
				findCharacter(activeChar, val);
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //find_character <character_name>");
				listCharacters(activeChar, 0, 1);
			}
		}
		else if (command.startsWith("admin_find_ip"))
		{
			try
			{
				final var val = command.substring(14);
				findCharactersPerIp(activeChar, val);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //find_ip <www.xxx.yyy.zzz>");
				listCharacters(activeChar, 0, 1);
			}
		}
		else if (command.startsWith("admin_find_account"))
		{
			try
			{
				final var val = command.substring(19);
				findCharactersPerAccount(activeChar, val);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //find_account <player_name>");
				listCharacters(activeChar, 0, 1);
			}
		}
		else if (command.startsWith("admin_edit_character"))
		{
			final String[] data = command.split(" ");
			if ((data.length > 1))
			{
				editCharacter(activeChar, data[1]);
			}
			else if (activeChar.getTarget() instanceof Player)
			{
				editCharacter(activeChar, null);
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			}
		}
		else if (command.equals("admin_nokarma"))
		{
			setTargetKarma(activeChar, 0);
		}
		else if (command.startsWith("admin_setkarma"))
		{
			try
			{
				final var val = command.substring(15);
				final var karma = Integer.parseInt(val);
				setTargetKarma(activeChar, karma);
			}
			catch (final Exception e)
			{
				if (Config.DEVELOPER)
				{
					_log.warn("Set karma error: " + e);
				}
				activeChar.sendMessage("Usage: //setkarma <new_karma_value>");
			}
		}
		else if (command.startsWith("admin_setpk"))
		{
			try
			{
				final var val = command.substring(12);
				final var pk = Integer.parseInt(val);
				final var target = activeChar.getTarget();
				if (target instanceof Player)
				{
					final var player = (Player) target;
					player.setPkKills(pk);
					player.broadcastUserInfo(true);
					player.sendMessage("A GM changed your PK count to " + pk);
					activeChar.sendMessage(player.getName(null) + "'s PK count changed to " + pk);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				if (Config.DEVELOPER)
				{
					_log.warn("Set pk error: " + e);
				}
				activeChar.sendMessage("Usage: //setpk <pk_count>");
			}
		}
		else if (command.startsWith("admin_setpvp"))
		{
			try
			{
				final var val = command.substring(13);
				final var pvp = Integer.parseInt(val);
				final var target = activeChar.getTarget();
				if (target instanceof Player)
				{
					final var player = (Player) target;
					player.setPvpKills(pvp);
					player.broadcastUserInfo(true);
					player.sendMessage("A GM changed your PVP count to " + pvp);
					activeChar.sendMessage(player.getName(null) + "'s PVP count changed to " + pvp);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				if (Config.DEVELOPER)
				{
					_log.warn("Set pvp error: " + e);
				}
				activeChar.sendMessage("Usage: //setpvp <pvp_count>");
			}
		}
		else if (command.startsWith("admin_setfame"))
		{
			try
			{
				final var val = command.substring(14);
				final var fame = Integer.parseInt(val);
				final var target = activeChar.getTarget();
				if (target instanceof Player)
				{
					final var player = (Player) target;
					player.setFame(fame);
					player.broadcastUserInfo(true);
					player.sendMessage("A GM changed your Reputation points to " + fame);
					activeChar.sendMessage(player.getName(null) + "'s Fame changed to " + fame);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				if (Config.DEVELOPER)
				{
					_log.warn("Set Fame error: " + e);
				}
				activeChar.sendMessage("Usage: //setfame <new_fame_value>");
			}
		}
		else if (command.startsWith("admin_save_modifications"))
		{
			try
			{
				final var val = command.substring(24);
				adminModifyCharacter(activeChar, val);
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Error while modifying character.");
				listCharacters(activeChar, 0, 1);
			}
		}
		else if (command.startsWith("admin_rec"))
		{
			try
			{
				final var val = command.substring(10);
				final var recVal = Integer.parseInt(val);
				final var target = activeChar.getTarget();
				if (target instanceof Player)
				{
					final var player = (Player) target;
					player.getRecommendation().setRecomHave(recVal);
					player.sendVoteSystemInfo();
					player.broadcastUserInfo(true);
					player.sendMessage("A GM changed your Recommend points to " + recVal);
					activeChar.sendMessage(player.getName(null) + "'s Recommend changed to " + recVal);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //rec number");
			}
		}
		else if (command.startsWith("admin_setclass"))
		{
			try
			{
				final var val = command.substring(15).trim();
				final var classidval = Integer.parseInt(val);
				final var target = activeChar.getTarget();
				Player player = null;
				if (target instanceof Player)
				{
					player = (Player) target;
				}
				else
				{
					return false;
				}
				var valid = false;
				for (final var classid : ClassId.values())
				{
					if (classidval == classid.getId())
					{
						valid = true;
					}
				}
				if (valid && (player.getClassId().getId() != classidval))
				{
					player.setClassId(classidval);
					if (!player.isSubClassActive())
					{
						player.setBaseClass(classidval);
					}
					final var newclass = ClassListParser.getInstance().getClass(player.getClassId()).getClassName();
					player.store();
					player.sendMessage("A GM changed your class to " + newclass + ".");
					player.broadcastUserInfo(true);
					activeChar.sendMessage(player.getName(null) + " is a " + newclass + ".");
				}
				else
				{
					activeChar.sendMessage("Usage: //setclass <valid_new_classid>");
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/setclass/human_fighter.htm");
				activeChar.sendPacket(adminhtm);
			}
			catch (final NumberFormatException e)
			{
				activeChar.sendMessage("Usage: //setclass <valid_new_classid>");
			}
		}
		else if (command.startsWith("admin_settitle"))
		{
			try
			{
				final var val = command.substring(15);
				final var target = activeChar.getTarget();
				Player player = null;
				if (target instanceof Player)
				{
					player = (Player) target;
				}
				else
				{
					return false;
				}
				player.setGlobalTitle(val);
				player.sendMessage("Your title has been changed by a GM");
				player.broadcastTitleInfo();
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You need to specify the new title.");
			}
		}
		else if (command.startsWith("admin_changename"))
		{
			try
			{
				final var val = command.substring(17);
				final var target = activeChar.getTarget();
				Player player = null;
				if (target instanceof Player)
				{
					player = (Player) target;
				}
				else
				{
					return false;
				}
				if (CharNameHolder.getInstance().getIdByName(val) > 0)
				{
					activeChar.sendMessage("Warning, player " + val + " already exists");
					return false;
				}
				player.setGlobalName(val);
				player.store();
				
				activeChar.sendMessage("Changed name to " + val);
				player.sendMessage("Your name has been changed by a GM.");
				player.broadcastUserInfo(true);
				
				if (player.isInParty())
				{
					player.getParty().broadcastToPartyMembers(player, PartySmallWindowDeleteAll.STATIC_PACKET);
					for (final var member : player.getParty().getMembers())
					{
						if (member != player)
						{
							member.sendPacket(new PartySmallWindowAll(member, player.getParty()));
						}
					}
				}
				if (player.getClan() != null)
				{
					player.getClan().broadcastClanStatus();
				}
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //setname new_name_for_target");
			}
		}
		else if (command.startsWith("admin_setsex"))
		{
			final var target = activeChar.getTarget();
			Player player = null;
			if (target instanceof Player)
			{
				player = (Player) target;
			}
			else
			{
				return false;
			}
			player.getAppearance().setSex(player.getAppearance().getSex() ? false : true);
			player.sendMessage("Your gender has been changed by a GM");
			player.broadcastUserInfo(true);
		}
		else if (command.startsWith("admin_setcolor"))
		{
			final var target = activeChar.getTarget();
			if (target == null)
			{
				activeChar.sendMessage("You have to select a player!");
				return false;
			}
			if (!(target instanceof Player))
			{
				activeChar.sendMessage("Your target is not a player!");
				return false;
			}
			final String[] params = command.split(" ");
			
			if (params.length < 3)
			{
				activeChar.sendMessage("Usage: //setcolor <colorHex> <timeInDays>");
				return false;
			}
			final var player = (Player) target;
			final var color = Util.decodeColor(params[1]);
			final var time = Long.valueOf(params[2]);
			
			if (params.length == 3)
			{
				player.getAppearance().setNameColor(color);
				player.setVar("namecolor", Integer.toString(color), (System.currentTimeMillis() + time * 24 * 60 * 60 * 1000L));
				player.sendMessage("Your name color has been changed by a GM!");
				player.broadcastUserInfo(true);
				return true;
			}
		}
		else if (command.startsWith("admin_settcolor"))
		{
			final var target = activeChar.getTarget();
			if (target == null)
			{
				activeChar.sendMessage("You have to select a player!");
				return false;
			}
			if (!(target instanceof Player))
			{
				activeChar.sendMessage("Your target is not a player!");
				return false;
			}
			
			final String[] params = command.split(" ");
			if (params.length < 3)
			{
				activeChar.sendMessage("Usage: //settcolor <colorHex> <timeInDays>");
				return false;
			}
			
			final var player = (Player) target;
			final var color = Util.decodeColor(params[1]);
			final var time = Long.valueOf(params[2]);
			
			if (params.length == 3)
			{
				player.getAppearance().setTitleColor(color);
				player.setVar("titlecolor", Integer.toString(color), (System.currentTimeMillis() + time * 24 * 60 * 60 * 1000L));
				player.sendMessage("Your title color has been changed by a GM");
				player.broadcastUserInfo(true);
				return true;
			}
		}
		else if (command.startsWith("admin_fullfood"))
		{
			final var target = activeChar.getTarget();
			if (target instanceof PetInstance)
			{
				final var targetPet = (PetInstance) target;
				targetPet.setCurrentFed(targetPet.getMaxFed());
				targetPet.sendPacket(new SetSummonRemainTime(targetPet.getMaxFed(), targetPet.getCurrentFed()));
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			}
		}
		else if (command.startsWith("admin_remove_clan_penalty"))
		{
			try
			{
				final var st = new StringTokenizer(command, " ");
				if (st.countTokens() != 3)
				{
					activeChar.sendMessage("Usage: //remove_clan_penalty join|create charname");
					return false;
				}
				
				st.nextToken();
				
				final var changeCreateExpiryTime = st.nextToken().equalsIgnoreCase("create");
				
				final var playerName = st.nextToken();
				final var player = GameObjectsStorage.getPlayer(playerName);
				if (player == null)
				{
					Connection con = null;
					PreparedStatement statement = null;
					try
					{
						con = DatabaseFactory.getInstance().getConnection();
						statement = con.prepareStatement("UPDATE characters SET " + (changeCreateExpiryTime ? "clan_create_expiry_time" : "clan_join_expiry_time") + " WHERE char_name=? LIMIT 1");
						statement.setString(1, playerName);
						statement.execute();
					}
					catch (final Exception e)
					{
					}
					finally
					{
						DbUtils.closeQuietly(con, statement);
					}
				}
				else
				{
					if (changeCreateExpiryTime)
					{
						player.setClanCreateExpiryTime(0);
					}
					else
					{
						player.setClanJoinExpiryTime(0);
					}
				}
				
				activeChar.sendMessage("Clan penalty successfully removed to character: " + playerName);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (command.startsWith("admin_find_dualbox"))
		{
			int multibox = 2;
			try
			{
				final var val = command.substring(19);
				multibox = Integer.parseInt(val);
				if (multibox < 1)
				{
					activeChar.sendMessage("Usage: //find_dualbox [number > 0]");
					return false;
				}
			}
			catch (final Exception e)
			{}
			findDualbox(activeChar, multibox);
		}
		else if (command.startsWith("admin_strict_find_dualbox"))
		{
			int multibox = 2;
			try
			{
				final var val = command.substring(26);
				multibox = Integer.parseInt(val);
				if (multibox < 1)
				{
					activeChar.sendMessage("Usage: //strict_find_dualbox [number > 0]");
					return false;
				}
			}
			catch (final Exception e)
			{}
			findDualboxStrict(activeChar, multibox);
		}
		else if (command.startsWith("admin_summon_info"))
		{
			final var target = activeChar.getTarget();
			if (target instanceof Summon)
			{
				gatherSummonInfo((Summon) target, activeChar);
			}
			else
			{
				activeChar.sendMessage("Invalid target.");
			}
		}
		else if (command.startsWith("admin_unsummon"))
		{
			final var target = activeChar.getTarget();
			if (target instanceof Summon)
			{
				((Summon) target).unSummon(((Summon) target).getOwner());
			}
			else
			{
				activeChar.sendMessage("Usable only with Pets/Summons");
			}
		}
		else if (command.startsWith("admin_summon_setlvl"))
		{
			final var target = activeChar.getTarget();
			if (target instanceof PetInstance)
			{
				final var pet = (PetInstance) target;
				try
				{
					final var val = command.substring(20);
					final var level = Integer.parseInt(val);
					long newexp, oldexp = 0;
					oldexp = pet.getStat().getExp();
					newexp = pet.getStat().getExpForLevel(level);
					if (oldexp > newexp)
					{
						pet.getStat().removeExp(oldexp - newexp);
					}
					else if (oldexp < newexp)
					{
						pet.getStat().addExp(newexp - oldexp);
					}
				}
				catch (final Exception e)
				{}
			}
			else
			{
				activeChar.sendMessage("Usable only with Pets");
			}
		}
		else if (command.startsWith("admin_show_pet_inv"))
		{
			String val;
			int objId;
			GameObject target;
			try
			{
				val = command.substring(19);
				objId = Integer.parseInt(val);
				target = GameObjectsStorage.getSummon(objId);
			}
			catch (final Exception e)
			{
				target = activeChar.getTarget();
			}
			
			if (target instanceof PetInstance)
			{
				final ItemInstance[] items = ((PetInstance) target).getInventory().getItems();
				activeChar.sendPacket(new GMViewItemList((PetInstance) target, items, items.length));
			}
			else
			{
				activeChar.sendMessage("Usable only with Pets");
			}
			
		}
		else if (command.startsWith("admin_partyinfo"))
		{
			String val;
			GameObject target;
			try
			{
				val = command.substring(16);
				target = GameObjectsStorage.getPlayer(val);
				if (target == null)
				{
					target = activeChar.getTarget();
				}
			}
			catch (final Exception e)
			{
				target = activeChar.getTarget();
			}
			
			if (target instanceof Player)
			{
				if (((Player) target).isInParty())
				{
					gatherPartyInfo((Player) target, activeChar);
				}
				else
				{
					activeChar.sendMessage("Not in party.");
				}
			}
			else
			{
				activeChar.sendMessage("Invalid target.");
			}
			
		}
		else if (command.equals("admin_setnoble"))
		{
			Player player = null;
			if (activeChar.getTarget() == null)
			{
				player = activeChar;
			}
			else if ((activeChar.getTarget() != null) && (activeChar.getTarget() instanceof Player))
			{
				player = (Player) activeChar.getTarget();
			}
			
			if (player != null)
			{
				if (!player.isNoble())
				{
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
					if (player.getObjectId() != activeChar.getObjectId())
					{
						activeChar.sendMessage("You've changed nobless status of: " + player.getName(null));
					}
					player.sendMessage("GM changed your nobless status!");
				}
			}
		}
		else if (command.startsWith("admin_game_points"))
		{
			try
			{
				final var val = command.substring(18);
				final var points = Integer.parseInt(val);
				final var target = activeChar.getTarget();
				if (target instanceof Player)
				{
					final var player = (Player) target;
					player.setGamePoints((player.getGamePoints() + points));
					player.sendMessage("GM changed your game points count to " + (player.getGamePoints() + points));
					activeChar.sendMessage(player.getName(null) + "'s game points count changed to " + (player.getGamePoints() + points));
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				if (Config.DEVELOPER)
				{
					_log.warn("Set game points error: " + e);
				}
				activeChar.sendMessage("Usage: //game_points <game_points_count>");
			}
		}
		else if (command.startsWith("admin_hp_percent"))
		{
			try
			{
				final var val = command.substring(17);
				final var percent = Double.parseDouble(val);
				final var target = activeChar.getTarget();
				if (target.isCreature())
				{
					final var creature = (Creature) target;
					if (percent > 0 && percent <= 100)
					{
						final double newHp = creature.getMaxHp() * (percent / 100.);
						creature.setCurrentHp(newHp);
						creature.sendPacket(creature.makeStatusUpdate(StatusUpdate.CUR_HP));
						activeChar.sendMessage("You changed hp percent for " + creature.getName(null) + " by " + percent + "%!");
					}
					else
					{
						activeChar.sendMessage("Usage: //admin_hp_percent <percent 1-100>");
					}
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //admin_hp_percent <percent 1-100>");
			}
		}
		else if (command.startsWith("admin_mp_percent"))
		{
			try
			{
				final var val = command.substring(17);
				final var percent = Double.parseDouble(val);
				final var target = activeChar.getTarget();
				if (target.isCreature())
				{
					final var creature = (Creature) target;
					if (percent > 0 && percent <= 100)
					{
						final double newMp = creature.getMaxMp() * (percent / 100.);
						creature.setCurrentMp(newMp);
						creature.sendPacket(creature.makeStatusUpdate(StatusUpdate.CUR_MP));
						activeChar.sendMessage("You changed mp percent for " + creature.getName(null) + " by " + percent + "%!");
					}
					else
					{
						activeChar.sendMessage("Usage: //admin_mp_percent <percent 1-100>");
					}
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //admin_mp_percent <percent 1-100>");
			}
		}
		else if (command.startsWith("admin_cp_percent"))
		{
			try
			{
				final var val = command.substring(17);
				final var percent = Double.parseDouble(val);
				final var target = activeChar.getTarget();
				if (target.isPlayer())
				{
					final var creature = (Player) target;
					if (percent > 0 && percent <= 100)
					{
						final double newCp = creature.getMaxCp() * (percent / 100.);
						creature.setCurrentCp(newCp);
						creature.sendPacket(creature.makeStatusUpdate(StatusUpdate.CUR_CP));
						activeChar.sendMessage("You changed cp percent for " + creature.getName(null) + " by " + percent + "%!");
					}
					else
					{
						activeChar.sendMessage("Usage: //admin_cp_percent <percent 1-100>");
					}
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //admin_cp_percent <percent 1-100>");
			}
		}
		else if (command.startsWith("admin_resetrec"))
		{
			for (final var player : GameObjectsStorage.getPlayers())
			{
				player.getRecommendation().restartRecom();
			}
			
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE `characters` SET `rec_bonus_time`=3600");
				statement.execute();
			}
			catch (final Exception e)
			{
				_log.warn("Could not update chararacters recommendations!", e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
			_log.info("Recommendation Global Task: completed.");
		}
		else if (command.startsWith("admin_charfarm"))
		{
			try
			{
				final var target = activeChar.getTarget();
				if (target != null && target instanceof Player)
				{
					final var player = (Player) target;
					if (player.getFarmSystem().isAutofarming())
					{
						player.getFarmSystem().stopFarmTask(false);
						activeChar.sendMessage(player.getName(null) + " autofarm stoped!");
					}
					else if (!player.getFarmSystem().isAutofarming() && player.getFarmSystem().isActiveAutofarm())
					{
						player.getFarmSystem().startFarmTask();
						activeChar.sendMessage(player.getName(null) + " autofarm enabled!");
					}
					else
					{
						activeChar.sendMessage(player.getName(null) + " cant use autofarm now!");
					}
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
			}
			catch (final Exception e)
			{
				if (Config.DEVELOPER)
				{
					_log.warn("Set game points error: " + e);
				}
				activeChar.sendMessage("Usage: //game_points <game_points_count>");
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void listCharacters(Player activeChar, int type, int page)
	{
		final List<String> realIpList = new ArrayList<>();
		final List<Player> totalList = new ArrayList<>();
		final List<Player> playerList = new ArrayList<>();
		final List<Player> fakeList = new ArrayList<>();
		final List<Player> offlineList = new ArrayList<>();
		final boolean isForIp = Config.PROTECTION.equalsIgnoreCase("NONE");
		for (final var onlinePlayer : GameObjectsStorage.getPlayers())
		{
			if (onlinePlayer != null)
			{
				if (onlinePlayer.isFakePlayer())
				{
					fakeList.add(onlinePlayer);
				}
				else
				{
					if (onlinePlayer.isInOfflineMode())
					{
						offlineList.add(onlinePlayer);
					}
					else
					{
						playerList.add(onlinePlayer);
					}
				}
				totalList.add(onlinePlayer);
				
				if (isForIp)
				{
					if (!realIpList.contains(onlinePlayer.getIPAddress()) && !onlinePlayer.isFakePlayer() && !onlinePlayer.isInOfflineMode())
					{
						realIpList.add(onlinePlayer.getIPAddress());
					}
				}
				else
				{
					if (!realIpList.contains(onlinePlayer.getHWID()) && !onlinePlayer.isFakePlayer() && !onlinePlayer.isInOfflineMode())
					{
						realIpList.add(onlinePlayer.getHWID());
					}
				}
			}
		}
		
		final int perpage = 6;
		int counter = 0;
		
		final List<Player> curList = new ArrayList<>();
		switch (type)
		{
			case 0 :
				curList.addAll(totalList);
				break;
			case 1 :
				curList.addAll(playerList);
				break;
			case 2 :
				curList.addAll(fakeList);
				break;
			case 3 :
				curList.addAll(offlineList);
				break;
		}
		
		final var isThereNextPage = curList.size() > perpage;
		
		final var adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/charlist.htm");
		var list = "";
		if (curList == null || curList.isEmpty())
		{
			list += "<table width=280><tr><td align=center>Empty List....</td></tr></table>";
		}
		else
		{
			for (int i = (page - 1) * perpage; i < curList.size(); i++)
			{
				final var player = curList.get(i);
				if (player != null)
				{
					list += "<table width=280 height=30><tr><td width=90><a action=\"bypass -h admin_character_info " + player.getName(null) + "\">" + player.getName(null) + "</a></td><td width=120>" + Util.className(activeChar, player.getClassId().getId()) + "</td><td width=50 align=center>" + player.getLevel() + "</td></tr></table><img src=\"L2UI.squaregray\" width=\"280\" height=\"1\">";
				}
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
		}
		
		final int count = (int) Math.ceil((double) curList.size() / perpage);
		adminReply.replace("%total%", String.valueOf(totalList.size()));
		adminReply.replace("%real%", String.valueOf(realIpList.size()));
		adminReply.replace("%players%", String.valueOf(playerList.size()));
		adminReply.replace("%fakes%", String.valueOf(fakeList.size()));
		adminReply.replace("%offline%", String.valueOf(offlineList.size()));
		adminReply.replace("%playerList%", list);
		adminReply.replace("%pages%", Util.getNavigationBlock(count, page, curList.size(), perpage, isThereNextPage, "admin_show_characters " + type + " %s"));
		activeChar.sendPacket(adminReply);
		totalList.clear();
		realIpList.clear();
		playerList.clear();
		fakeList.clear();
		offlineList.clear();
	}
	
	private void showCharacterInfo(Player activeChar, Player player)
	{
		if (player == null)
		{
			final var target = activeChar.getTarget();
			if (target instanceof Player)
			{
				player = (Player) target;
			}
			else
			{
				return;
			}
		}
		else
		{
			activeChar.setTarget(player);
		}
		gatherCharacterInfo(activeChar, player, "charinfo.htm");
	}
	
	private void gatherCharacterInfo(Player activeChar, Player player, String filename)
	{
		var ip = "N/A";
		
		if (player == null)
		{
			activeChar.sendMessage("Player is null.");
			return;
		}
		
		final var client = player.getClient();
		if (client == null)
		{
			activeChar.sendMessage("Client is null.");
		}
		else if (client.isDetached())
		{
			activeChar.sendMessage("Client is detached.");
		}
		else
		{
			ip = player.getIPAddress();
		}
		
		final var adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/" + filename);
		adminReply.replace("%name%", player.getName(null));
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%clan%", String.valueOf(player.getClan() != null ? "<a action=\"bypass -h admin_clan_info " + player.getObjectId() + "\">" + player.getClan().getName() + "</a>" : ""));
		adminReply.replace("%xp%", String.valueOf(player.getExp()));
		adminReply.replace("%sp%", String.valueOf(player.getSp()));
		adminReply.replace("%class%", Util.className(activeChar, player.getClassId().getId()));
		adminReply.replace("%ordinal%", String.valueOf(player.getClassId().ordinal()));
		adminReply.replace("%classid%", String.valueOf(player.getClassId()));
		adminReply.replace("%baseclass%", ClassListParser.getInstance().getClass(player.getBaseClass()).getClientCode());
		adminReply.replace("%x%", String.valueOf(player.getX()));
		adminReply.replace("%y%", String.valueOf(player.getY()));
		adminReply.replace("%z%", String.valueOf(player.getZ()));
		adminReply.replace("%currenthp%", String.valueOf((int) player.getCurrentHp()));
		adminReply.replace("%maxhp%", String.valueOf((int) player.getMaxHp()));
		adminReply.replace("%karma%", String.valueOf(player.getKarma()));
		adminReply.replace("%currentmp%", String.valueOf((int) player.getCurrentMp()));
		adminReply.replace("%maxmp%", String.valueOf((int) player.getMaxMp()));
		adminReply.replace("%pvpflag%", String.valueOf(player.getPvpFlag()));
		adminReply.replace("%currentcp%", String.valueOf((int) player.getCurrentCp()));
		adminReply.replace("%maxcp%", String.valueOf((int) player.getMaxCp()));
		adminReply.replace("%pvpkills%", String.valueOf(player.getPvpKills()));
		adminReply.replace("%pkkills%", String.valueOf(player.getPkKills()));
		adminReply.replace("%currentload%", String.valueOf(player.getCurrentLoad()));
		adminReply.replace("%maxload%", String.valueOf(player.getMaxLoad()));
		adminReply.replace("%percent%", String.valueOf(Util.roundTo(((float) player.getCurrentLoad() / (float) player.getMaxLoad()) * 100, 2)));
		adminReply.replace("%patk%", String.valueOf((int) player.getPAtk(null)));
		adminReply.replace("%matk%", String.valueOf((int) player.getMAtk(null, null)));
		adminReply.replace("%pdef%", String.valueOf((int) player.getPDef(null)));
		adminReply.replace("%mdef%", String.valueOf((int) player.getMDef(null, null)));
		adminReply.replace("%accuracy%", String.valueOf(player.getAccuracy()));
		adminReply.replace("%evasion%", String.valueOf(player.getEvasionRate(null)));
		adminReply.replace("%critical%", String.valueOf((int) player.getCriticalHit(null, null)));
		adminReply.replace("%runspeed%", String.valueOf((int) player.getRunSpeed()));
		adminReply.replace("%patkspd%", String.valueOf((int) player.getPAtkSpd()));
		adminReply.replace("%matkspd%", String.valueOf((int) player.getMAtkSpd()));
		adminReply.replace("%access%", player.getAccessLevel().getLevel() + " (" + player.getAccessLevel().getName() + ")");
		adminReply.replace("%account%", player.getAccountName());
		adminReply.replace("%ip%", ip);
		adminReply.replace("%ai%", String.valueOf(player.getAI().getIntention().name()));
		adminReply.replace("%inst%", player.getReflectionId() > 0 ? "<a action=\"bypass -h admin_instance_spawns " + String.valueOf(player.getReflectionId()) + "\">" + String.valueOf(player.getReflectionId()) + "</a>" : "" + String.valueOf(player.getReflectionId()) + "");
		adminReply.replace("%noblesse%", player.isNoble() ? "Yes" : "No");
		adminReply.replace("%region%", player.getWorldRegion() != null ? player.getWorldRegion().getName() : "");
		adminReply.replace("%farm%", player.getFarmSystem().isAutofarming() ? "<font color=FF0000>Farm Active</font>" : "");
		activeChar.sendPacket(adminReply);
	}
	
	private void setTargetKarma(Player activeChar, int newKarma)
	{
		final var target = activeChar.getTarget();
		Player player = null;
		if (target instanceof Player)
		{
			player = (Player) target;
		}
		else
		{
			return;
		}
		
		if (newKarma >= 0)
		{
			final var oldKarma = player.getKarma();
			player.setKarma(newKarma);
			final var sm = SystemMessage.getSystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO_S1);
			sm.addNumber(newKarma);
			player.sendPacket(sm);
			activeChar.sendMessage("Successfully Changed karma for " + player.getName(null) + " from (" + oldKarma + ") to (" + newKarma + ").");
			if (Config.DEBUG)
			{
				_log.info("[SET KARMA] [GM]" + activeChar.getName(null) + " Changed karma for " + player.getName(null) + " from (" + oldKarma + ") to (" + newKarma + ").");
			}
		}
		else
		{
			activeChar.sendMessage("You must enter a value for karma greater than or equal to 0.");
			if (Config.DEBUG)
			{
				_log.info("[SET KARMA] ERROR: [GM]" + activeChar.getName(null) + " entered an incorrect value for new karma: " + newKarma + " for " + player.getName(null) + ".");
			}
		}
	}
	
	private void adminModifyCharacter(Player activeChar, String modifications)
	{
		final var target = activeChar.getTarget();
		
		if (!(target instanceof Player))
		{
			return;
		}
		
		final var player = (Player) target;
		final var st = new StringTokenizer(modifications);
		
		if (st.countTokens() != 6)
		{
			editCharacter(activeChar, null);
			return;
		}
		
		final var hp = st.nextToken();
		final var mp = st.nextToken();
		final var cp = st.nextToken();
		final var pvpflag = st.nextToken();
		final var pvpkills = st.nextToken();
		final var pkkills = st.nextToken();
		
		final var hpval = Integer.parseInt(hp);
		final var mpval = Integer.parseInt(mp);
		final var cpval = Integer.parseInt(cp);
		final var pvpflagval = Integer.parseInt(pvpflag);
		final var pvpkillsval = Integer.parseInt(pvpkills);
		final var pkkillsval = Integer.parseInt(pkkills);
		
		player.sendMessage("Admin has changed your stats." + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + "  PvP Flag: " + pvpflagval + " PvP/PK " + pvpkillsval + "/" + pkkillsval);
		player.setCurrentHp(hpval);
		player.setCurrentMp(mpval);
		player.setCurrentCp(cpval);
		player.setPvpFlag(pvpflagval);
		player.setPvpKills(pvpkillsval);
		player.setPkKills(pkkillsval);
		player.store();
		
		final var su = player.makeStatusUpdate(StatusUpdate.CUR_HP, StatusUpdate.MAX_HP, StatusUpdate.CUR_MP, StatusUpdate.MAX_MP, StatusUpdate.CUR_CP, StatusUpdate.MAX_CP);
		player.sendPacket(su);
		activeChar.sendMessage("Changed stats of " + player.getName(null) + "." + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + "  PvP: " + pvpflagval + " / " + pvpkillsval);
		
		if (Config.DEBUG)
		{
			_log.info("[GM]" + activeChar.getName(null) + " changed stats of " + player.getName(null) + ". " + " HP: " + hpval + " MP: " + mpval + " CP: " + cpval + " PvP: " + pvpflagval + " / " + pvpkillsval);
		}
		
		showCharacterInfo(activeChar, null);
		
		player.broadcastCharInfo();
		player.getAI().setIntention(CtrlIntention.IDLE);
		player.decayMe();
		player.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	}
	
	private void editCharacter(Player activeChar, String targetName)
	{
		GameObject target = null;
		if (targetName != null)
		{
			target = GameObjectsStorage.getPlayer(targetName);
		}
		else
		{
			target = activeChar.getTarget();
		}
		
		if (target instanceof Player)
		{
			final Player player = (Player) target;
			gatherCharacterInfo(activeChar, player, "charedit.htm");
		}
	}
	
	private void findCharacter(Player activeChar, String CharacterToFind)
	{
		var CharactersFound = 0;
		String name;
		final var adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/charfind.htm");
		
		final var replyMSG = new StringBuilder(1000);
		
		for (final var player : GameObjectsStorage.getPlayers())
		{
			name = player.getName(null);
			if (name.toLowerCase().contains(CharacterToFind.toLowerCase()))
			{
				CharactersFound = CharactersFound + 1;
				StringUtil.append(replyMSG, "<tr><td width=80><a action=\"bypass -h admin_character_info ", name, "\">", name, "</a></td><td width=110>", ClassListParser.getInstance().getClass(player.getClassId()).getClientCode(), "</td><td width=40>", String.valueOf(player.getLevel()), "</td></tr>");
			}
			if (CharactersFound > 20)
			{
				break;
			}
		}
		adminReply.replace("%results%", replyMSG.toString());
		
		final String replyMSG2;
		
		if (CharactersFound == 0)
		{
			replyMSG2 = "s. Please try again.";
		}
		else if (CharactersFound > 20)
		{
			adminReply.replace("%number%", " more than 20");
			replyMSG2 = "s.<br>Please refine your search to see all of the results.";
		}
		else if (CharactersFound == 1)
		{
			replyMSG2 = ".";
		}
		else
		{
			replyMSG2 = "s.";
		}
		
		adminReply.replace("%number%", String.valueOf(CharactersFound));
		adminReply.replace("%end%", replyMSG2);
		activeChar.sendPacket(adminReply);
	}
	
	private void findCharactersPerIp(Player activeChar, String IpAdress) throws IllegalArgumentException
	{
		var findDisconnected = false;
		
		if (IpAdress.equals("disconnected"))
		{
			findDisconnected = true;
		}
		else
		{
			if (!IpAdress.matches("^(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))$"))
			{
				throw new IllegalArgumentException("Malformed IPv4 number");
			}
		}
		var CharactersFound = 0;
		GameClient client;
		String name, ip = "0.0.0.0";
		final var replyMSG = new StringBuilder(1000);
		final var adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/ipfind.htm");
		for (final var player : GameObjectsStorage.getPlayers())
		{
			client = player.getClient();
			if (client == null)
			{
				continue;
			}
			
			if (client.isDetached())
			{
				if (!findDisconnected)
				{
					continue;
				}
			}
			else
			{
				if (findDisconnected)
				{
					continue;
				}
				
				ip = player.getIPAddress();
				if (!ip.equals(IpAdress))
				{
					continue;
				}
			}
			
			name = player.getName(null);
			CharactersFound = CharactersFound + 1;
			StringUtil.append(replyMSG, "<tr><td width=80><a action=\"bypass -h admin_character_info ", name, "\">", name, "</a></td><td width=110>", ClassListParser.getInstance().getClass(player.getClassId()).getClientCode(), "</td><td width=40>", String.valueOf(player.getLevel()), "</td></tr>");
			
			if (CharactersFound > 20)
			{
				break;
			}
		}
		adminReply.replace("%results%", replyMSG.toString());
		
		final String replyMSG2;
		
		if (CharactersFound == 0)
		{
			replyMSG2 = "s. Maybe they got d/c? :)";
		}
		else if (CharactersFound > 20)
		{
			adminReply.replace("%number%", " more than " + String.valueOf(CharactersFound));
			replyMSG2 = "s.<br>In order to avoid you a client crash I won't <br1>display results beyond the 20th character.";
		}
		else if (CharactersFound == 1)
		{
			replyMSG2 = ".";
		}
		else
		{
			replyMSG2 = "s.";
		}
		adminReply.replace("%ip%", IpAdress);
		adminReply.replace("%number%", String.valueOf(CharactersFound));
		adminReply.replace("%end%", replyMSG2);
		activeChar.sendPacket(adminReply);
	}
	
	private void findCharactersPerAccount(Player activeChar, String characterName) throws IllegalArgumentException
	{
		if (characterName.matches(Config.CNAME_TEMPLATE))
		{
			String account = null;
			Map<Integer, String> chars;
			final var player = GameObjectsStorage.getPlayer(characterName);
			if (player == null)
			{
				throw new IllegalArgumentException("Player doesn't exist");
			}
			chars = player.getAccountChars();
			account = player.getAccountName();
			final var replyMSG = new StringBuilder(chars.size() * 20);
			final var adminReply = new NpcHtmlMessage(5);
			adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/accountinfo.htm");
			for (final var charname : chars.values())
			{
				StringUtil.append(replyMSG, charname, "<br1>");
			}
			
			adminReply.replace("%characters%", replyMSG.toString());
			adminReply.replace("%account%", account);
			adminReply.replace("%player%", characterName);
			activeChar.sendPacket(adminReply);
		}
		else
		{
			throw new IllegalArgumentException("Malformed character name");
		}
	}
	
	private void findDualbox(Player activeChar, int multibox)
	{
		final Map<String, List<Player>> ipMap = new HashMap<>();
		
		var ip = "0.0.0.0";
		GameClient client;
		
		final Map<String, Integer> dualboxIPs = new HashMap<>();
		
		for (final var player : GameObjectsStorage.getPlayers())
		{
			client = player.getClient();
			if ((client == null) || client.isDetached())
			{
				continue;
			}
			
			ip = player.getIPAddress();
			if (ipMap.get(ip) == null)
			{
				ipMap.put(ip, new ArrayList<>());
			}
			ipMap.get(ip).add(player);
			
			if (ipMap.get(ip).size() >= multibox)
			{
				final Integer count = dualboxIPs.get(ip);
				if (count == null)
				{
					dualboxIPs.put(ip, multibox);
				}
				else
				{
					dualboxIPs.put(ip, count + 1);
				}
			}
		}
		
		final List<String> keys = new ArrayList<>(dualboxIPs.keySet());
		Collections.sort(keys, new Comparator<String>()
		{
			@Override
			public int compare(String left, String right)
			{
				return dualboxIPs.get(left).compareTo(dualboxIPs.get(right));
			}
		});
		Collections.reverse(keys);
		
		final var results = new StringBuilder();
		for (final var dualboxIP : keys)
		{
			StringUtil.append(results, "<a action=\"bypass -h admin_find_ip " + dualboxIP + "\">" + dualboxIP + " (" + dualboxIPs.get(dualboxIP) + ")</a><br1>");
		}
		
		final var adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/dualbox.htm");
		adminReply.replace("%multibox%", String.valueOf(multibox));
		adminReply.replace("%results%", results.toString());
		adminReply.replace("%strict%", "");
		activeChar.sendPacket(adminReply);
	}
	
	private void findDualboxStrict(Player activeChar, int multibox)
	{
		final Map<String, List<Player>> ipMap = new HashMap<>();
		
		GameClient client;
		
		final Map<String, Integer> dualboxIPs = new HashMap<>();
		
		for (final var player : GameObjectsStorage.getPlayers())
		{
			client = player.getClient();
			if ((client == null) || client.isDetached())
			{
				continue;
			}
			
			final var ipAdress = player.getIPAddress();
			if (ipMap.get(ipAdress) == null)
			{
				ipMap.put(ipAdress, new ArrayList<>());
			}
			ipMap.get(ipAdress).add(player);
			
			if (ipMap.get(ipAdress).size() >= multibox)
			{
				final Integer count = dualboxIPs.get(ipAdress);
				if (count == null)
				{
					dualboxIPs.put(ipAdress, multibox);
				}
				else
				{
					dualboxIPs.put(ipAdress, count + 1);
				}
			}
		}
		
		final List<String> keys = new ArrayList<>(dualboxIPs.keySet());
		Collections.sort(keys, new Comparator<String>()
		{
			@Override
			public int compare(String left, String right)
			{
				return dualboxIPs.get(left).compareTo(dualboxIPs.get(right));
			}
		});
		Collections.reverse(keys);
		
		final var results = new StringBuilder();
		for (final var dualboxIP : keys)
		{
			StringUtil.append(results, "<a action=\"bypass -h admin_find_ip " + dualboxIP + "\">" + dualboxIP + " (" + dualboxIPs.get(dualboxIP) + ")</a><br1>");
		}
		
		final var adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(activeChar, activeChar.getLang(), "data/html/admin/dualbox.htm");
		adminReply.replace("%multibox%", String.valueOf(multibox));
		adminReply.replace("%results%", results.toString());
		adminReply.replace("%strict%", "strict_");
		activeChar.sendPacket(adminReply);
	}
	
	private void gatherSummonInfo(Summon target, Player activeChar)
	{
		final var html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/petinfo.htm");
		final var name = target.getName(activeChar.getLang());
		html.replace("%name%", name == null ? "N/A" : name);
		html.replace("%level%", Integer.toString(target.getLevel()));
		html.replace("%exp%", Long.toString(target.getStat().getExp()));
		final var owner = target.getActingPlayer().getName(null);
		html.replace("%owner%", " <a action=\"bypass -h admin_character_info " + owner + "\">" + owner + "</a>");
		html.replace("%class%", target.getClass().getSimpleName());
		html.replace("%ai%", target.hasAI() ? String.valueOf(target.getAI().getIntention().name()) : "NULL");
		html.replace("%hp%", (int) target.getStatus().getCurrentHp() + "/" + target.getStat().getMaxHp());
		html.replace("%mp%", (int) target.getStatus().getCurrentMp() + "/" + target.getStat().getMaxMp());
		html.replace("%karma%", Integer.toString(target.getKarma()));
		html.replace("%undead%", target.isUndead() ? "yes" : "no");
		if (target instanceof PetInstance)
		{
			final int objId = target.getActingPlayer().getObjectId();
			html.replace("%inv%", " <a action=\"bypass -h admin_show_pet_inv " + objId + "\">view</a>");
		}
		else
		{
			html.replace("%inv%", "none");
		}
		if (target instanceof PetInstance)
		{
			html.replace("%food%", ((PetInstance) target).getCurrentFed() + "/" + ((PetInstance) target).getPetLevelData().getPetMaxFeed());
			html.replace("%load%", ((PetInstance) target).getInventory().getTotalWeight() + "/" + ((PetInstance) target).getMaxLoad());
		}
		else
		{
			html.replace("%food%", "N/A");
			html.replace("%load%", "N/A");
		}
		activeChar.sendPacket(html);
	}
	
	private void gatherPartyInfo(Player target, Player activeChar)
	{
		var color = true;
		final var html = new NpcHtmlMessage(0);
		html.setFile(activeChar, activeChar.getLang(), "data/html/admin/partyinfo.htm");
		final var text = new StringBuilder(400);
		for (final var member : target.getParty().getMembers())
		{
			if (color)
			{
				text.append("<tr><td><table width=270 border=0 bgcolor=131210 cellpadding=2><tr><td width=30 align=right>");
			}
			else
			{
				text.append("<tr><td><table width=270 border=0 cellpadding=2><tr><td width=30 align=right>");
			}
			text.append(member.getLevel() + "</td><td width=130><a action=\"bypass -h admin_character_info " + member.getName(null) + "\">" + member.getName(null) + "</a>");
			text.append("</td><td width=110 align=right>" + member.getClassId().toString() + "</td></tr></table></td></tr>");
			color = !color;
		}
		html.replace("%player%", target.getName(null));
		html.replace("%party%", text.toString());
		activeChar.sendPacket(html);
	}
}