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

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.ChestInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.EarthQuake;
import l2e.gameserver.network.serverpackets.ExRedSky;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SSQInfo;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.gameserver.network.serverpackets.SunRise;
import l2e.gameserver.network.serverpackets.SunSet;

public class Effects implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
	        "admin_invis", "admin_invisible", "admin_setinvis", "admin_vis", "admin_visible", "admin_invis_menu", "admin_earthquake", "admin_earthquake_menu", "admin_bighead", "admin_shrinkhead", "admin_gmspeed", "admin_gmspeed_menu", "admin_unpara_all", "admin_para_all", "admin_unpara", "admin_para", "admin_unpara_all_menu", "admin_para_all_menu", "admin_unpara_menu", "admin_para_menu", "admin_polyself", "admin_unpolyself", "admin_polyself_menu", "admin_unpolyself_menu", "admin_clearteams", "admin_setteam_close", "admin_setteam", "admin_social", "admin_effect", "admin_social_menu", "admin_special", "admin_special_menu", "admin_effect_menu", "admin_abnormal", "admin_abnormal_menu", "admin_play_sounds", "admin_play_sound", "admin_atmosphere", "admin_atmosphere_menu", "admin_set_displayeffect", "admin_set_displayeffect_menu"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();

		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);

		if (command.equals("admin_invis_menu"))
		{
			if (!activeChar.isInvisible())
			{
				activeChar.setInvisible(true);
				activeChar.broadcastUserInfo(true);
			}
			else
			{
				activeChar.setInvisible(false);
				activeChar.broadcastUserInfo(true);
			}
			command = "";
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_invis"))
		{
			if (!activeChar.isInvisible())
			{
				activeChar.setInvisible(true);
				activeChar.broadcastUserInfo(true);
			}
			else
			{
				activeChar.setInvisible(false);
				activeChar.broadcastUserInfo(true);
			}
		}
		else if (command.startsWith("admin_setinvis"))
		{
			if ((activeChar.getTarget() == null) || !activeChar.getTarget().isCreature())
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return false;
			}
			final Creature target = (Creature) activeChar.getTarget();
			target.setInvisible(!target.isInvisible());
			activeChar.sendMessage("You've made " + target.getName(activeChar.getLang()) + " " + (target.isInvisible() ? "invisible" : "visible") + ".");
		}
		else if (command.startsWith("admin_vis"))
		{
			activeChar.setInvisible(false);
			activeChar.broadcastUserInfo(true);
		}
		else if (command.startsWith("admin_earthquake"))
		{
			try
			{
				final String val1 = st.nextToken();
				final int intensity = Integer.parseInt(val1);
				final String val2 = st.nextToken();
				final int duration = Integer.parseInt(val2);
				final EarthQuake eq = new EarthQuake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), intensity, duration);
				activeChar.broadcastPacket(eq);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //earthquake <intensity> <duration>");
			}
		}
		else if (command.startsWith("admin_atmosphere"))
		{
			try
			{
				final String type = st.nextToken();
				final String state = st.nextToken();
				final int duration = Integer.parseInt(st.nextToken());
				adminAtmosphere(type, state, duration, activeChar);
			}
			catch (final Exception ex)
			{
				activeChar.sendMessage("Usage: //atmosphere <signsky dawn|dusk>|<sky day|night|red> <duration>");
			}
		}
		else if (command.equals("admin_play_sounds"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/songs/songs.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.startsWith("admin_play_sounds"))
		{
			try
			{
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/songs/songs" + command.substring(18) + ".htm");
				activeChar.sendPacket(adminhtm);
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //play_sounds <pagenumber>");
			}
		}
		else if (command.startsWith("admin_play_sound"))
		{
			try
			{
				playAdminSound(activeChar, command.substring(17));
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //play_sound <soundname>");
			}
		}
		else if (command.equals("admin_para_all"))
		{
			try
			{
				for (final Player player : World.getInstance().getAroundPlayers(activeChar))
				{
					if (!player.isGM())
					{
						player.startAbnormalEffect(AbnormalEffect.HOLD_1);
						player.setIsParalyzed(true);
						player.startParalyze();
					}
				}
			}
			catch (final Exception e)
			{}
		}
		else if (command.equals("admin_unpara_all"))
		{
			try
			{
				for (final Player player : World.getInstance().getAroundPlayers(activeChar))
				{
					player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
					player.setIsParalyzed(false);
				}
			}
			catch (final Exception e)
			{}
		}
		else if (command.startsWith("admin_para"))
		{
			String type = "1";
			try
			{
				type = st.nextToken();
			}
			catch (final Exception e)
			{}
			try
			{
				final GameObject target = activeChar.getTarget();
				Creature player = null;
				if (target instanceof Creature)
				{
					player = (Creature) target;
					if (type.equals("1"))
					{
						player.startAbnormalEffect(AbnormalEffect.HOLD_1);
					}
					else
					{
						player.startAbnormalEffect(AbnormalEffect.HOLD_2);
					}
					player.setIsParalyzed(true);
					player.startParalyze();
				}
			}
			catch (final Exception e)
			{}
		}
		else if (command.startsWith("admin_unpara"))
		{
			String type = "1";
			try
			{
				type = st.nextToken();
			}
			catch (final Exception e)
			{}
			try
			{
				final GameObject target = activeChar.getTarget();
				Creature player = null;
				if (target instanceof Creature)
				{
					player = (Creature) target;
					if (type.equals("1"))
					{
						player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
					}
					else
					{
						player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
					}
					player.setIsParalyzed(false);
				}
			}
			catch (final Exception e)
			{}
		}
		else if (command.startsWith("admin_bighead"))
		{
			try
			{
				final GameObject target = activeChar.getTarget();
				Creature player = null;
				if (target instanceof Creature)
				{
					player = (Creature) target;
					player.startAbnormalEffect(AbnormalEffect.BIG_HEAD);
				}
			}
			catch (final Exception e)
			{}
		}
		else if (command.startsWith("admin_shrinkhead"))
		{
			try
			{
				final GameObject target = activeChar.getTarget();
				Creature player = null;
				if (target instanceof Creature)
				{
					player = (Creature) target;
					player.stopAbnormalEffect(AbnormalEffect.BIG_HEAD);
				}
			}
			catch (final Exception e)
			{}
		}
		else if (command.startsWith("admin_gmspeed"))
		{
			if (!activeChar.getAccessLevel().isGmSpeedAccess())
			{
				return false;
			}
			try
			{
				final int val = Integer.parseInt(st.nextToken());
				activeChar.stopSkillEffects(7029);
				if ((val >= 1) && (val <= 4))
				{
					final Skill gmSpeedSkill = SkillsParser.getInstance().getInfo(7029, val);
					activeChar.doSimultaneousCast(gmSpeedSkill);
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //gmspeed <value> (0=off...4=max)");
			}
			if (command.contains("_menu"))
			{
				command = "";
				adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/gm_menu.htm");
				activeChar.sendPacket(adminhtm);
			}
		}
		else if (command.startsWith("admin_polyself"))
		{
			try
			{
				final String id = st.nextToken();
				activeChar.setPolyId(Integer.parseInt(id));
				activeChar.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false, activeChar.getReflection());
				activeChar.broadcastCharInfo();
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //polyself <npcId>");
			}
		}
		else if (command.startsWith("admin_unpolyself"))
		{
			activeChar.setPolyId(0);
			activeChar.decayMe();
			activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
			activeChar.broadcastCharInfo();
		}
		else if (command.equals("admin_clearteams"))
		{
			try
			{
				for (final Player player : World.getInstance().getAroundPlayers(activeChar))
				{
					player.setTeam(0);
					player.broadcastUserInfo(true);
				}
			}
			catch (final Exception e)
			{}
		}
		else if (command.startsWith("admin_setteam_close"))
		{
			try
			{
				final String val = st.nextToken();
				int radius = 400;
				if (st.hasMoreTokens())
				{
					radius = Integer.parseInt(st.nextToken());
				}
				final int teamVal = Integer.parseInt(val);
				for (final Creature player : World.getInstance().getAroundCharacters(activeChar, radius, 200))
				{
					player.setTeam(teamVal);
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //setteam_close <teamId>");
			}
		}
		else if (command.startsWith("admin_setteam"))
		{
			try
			{
				final String val = st.nextToken();
				final int teamVal = Integer.parseInt(val);
				Creature target = null;
				if (activeChar.getTarget() instanceof Creature)
				{
					target = (Creature) activeChar.getTarget();
				}
				else
				{
					return false;
				}
				target.setTeam(teamVal);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //setteam <teamId>");
			}
		}
		else if (command.startsWith("admin_social"))
		{
			try
			{
				String target = null;
				GameObject obj = activeChar.getTarget();
				if (st.countTokens() == 2)
				{
					final int social = Integer.parseInt(st.nextToken());
					target = st.nextToken();
					if (target != null)
					{
						final Player player = GameObjectsStorage.getPlayer(target);
						if (player != null)
						{
							if (performSocial(social, player, activeChar))
							{
								activeChar.sendMessage(player.getName(null) + " was affected by your request.");
							}
						}
						else
						{
							try
							{
								final int radius = Integer.parseInt(target);
								for (final GameObject object : World.getInstance().getAroundObjects(activeChar))
								{
									if (activeChar.isInsideRadius(object, radius, false, false))
									{
										performSocial(social, object, activeChar);
									}
								}
								activeChar.sendMessage(radius + " units radius affected by your request.");
							}
							catch (final NumberFormatException nbe)
							{
								activeChar.sendMessage("Incorrect parameter");
							}
						}
					}
				}
				else if (st.countTokens() == 1)
				{
					final int social = Integer.parseInt(st.nextToken());
					if (obj == null)
					{
						obj = activeChar;
					}

					if (performSocial(social, obj, activeChar))
					{
						activeChar.sendMessage(obj.getName(activeChar.getLang()) + " was affected by your request.");
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					}
				}
				else if (!command.contains("menu"))
				{
					activeChar.sendMessage("Usage: //social <social_id> [player_name|radius]");
				}
			}
			catch (final Exception e)
			{
				if (Config.DEBUG)
				{
					e.printStackTrace();
				}
			}
		}
		else if (command.startsWith("admin_abnormal"))
		{
			try
			{
				String target = null;
				GameObject obj = activeChar.getTarget();
				if (st.countTokens() == 2)
				{
					final String parm = st.nextToken();
					final int abnormal = Integer.decode("0x" + parm);
					target = st.nextToken();
					if (target != null)
					{
						final Player player = GameObjectsStorage.getPlayer(target);
						if (player != null)
						{
							if (performAbnormal(abnormal, player))
							{
								activeChar.sendMessage(player.getName(null) + "'s abnormal status was affected by your request.");
							}
							else
							{
								activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
							}
						}
						else
						{
							try
							{
								final int radius = Integer.parseInt(target);
								for (final GameObject object : World.getInstance().getAroundObjects(activeChar))
								{
									if (activeChar.isInsideRadius(object, radius, false, false))
									{
										performAbnormal(abnormal, object);
									}
								}
								activeChar.sendMessage(radius + " units radius affected by your request.");
							}
							catch (final NumberFormatException nbe)
							{
								activeChar.sendMessage("Usage: //abnormal <hex_abnormal_mask> [player|radius]");
							}
						}
					}
				}
				else if (st.countTokens() == 1)
				{
					final int abnormal = Integer.decode("0x" + st.nextToken());
					if (obj == null)
					{
						obj = activeChar;
					}

					if (performAbnormal(abnormal, obj))
					{
						activeChar.sendMessage(obj.getName(activeChar.getLang()) + "'s abnormal status was affected by your request.");
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					}
				}
				else if (!command.contains("menu"))
				{
					activeChar.sendMessage("Usage: //abnormal <abnormal_mask> [player_name|radius]");
				}
			}
			catch (final Exception e)
			{
				if (Config.DEBUG)
				{
					e.printStackTrace();
				}
			}
		}
		else if (command.startsWith("admin_special"))
		{
			try
			{
				String target = null;
				GameObject obj = activeChar.getTarget();
				if (st.countTokens() == 2)
				{
					final String parm = st.nextToken();
					final int special = Integer.decode("0x" + parm);
					target = st.nextToken();
					if (target != null)
					{
						final Player player = GameObjectsStorage.getPlayer(target);
						if (player != null)
						{
							if (performSpecial(special, player))
							{
								activeChar.sendMessage(player.getName(null) + "'s special status was affected by your request.");
							}
							else
							{
								activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
							}
						}
						else
						{
							try
							{
								final int radius = Integer.parseInt(target);
								for (final GameObject object : World.getInstance().getAroundObjects(activeChar))
								{
									if (activeChar.isInsideRadius(object, radius, false, false))
									{
										performSpecial(special, object);
									}
								}
								activeChar.sendMessage(radius + " units radius affected by your request.");
							}
							catch (final NumberFormatException nbe)
							{
								activeChar.sendMessage("Usage: //special <hex_special_mask> [player|radius]");
							}
						}
					}
				}
				else if (st.countTokens() == 1)
				{
					final int special = Integer.decode("0x" + st.nextToken());
					if (obj == null)
					{
						obj = activeChar;
					}

					if (performSpecial(special, obj))
					{
						activeChar.sendMessage(obj.getName(activeChar.getLang()) + "'s special status was affected by your request.");
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					}
				}
				else if (!command.contains("menu"))
				{
					activeChar.sendMessage("Usage: //special <special_mask> [player_name|radius]");
				}
			}
			catch (final Exception e)
			{
				if (Config.DEBUG)
				{
					e.printStackTrace();
				}
			}
		}
		else if (command.startsWith("admin_effect"))
		{
			try
			{
				GameObject obj = activeChar.getTarget();
				int level = 1, hittime = 1;
				final int skill = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					level = Integer.parseInt(st.nextToken());
				}
				if (st.hasMoreTokens())
				{
					hittime = Integer.parseInt(st.nextToken());
				}
				if (obj == null)
				{
					obj = activeChar;
				}
				if (!(obj instanceof Creature))
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
				else
				{
					final Creature target = (Creature) obj;
					target.broadcastPacket(new MagicSkillUse(target, activeChar, skill, level, hittime, 0));
					activeChar.sendMessage(obj.getName(activeChar.getLang()) + " performs MSU " + skill + "/" + level + " by your request.");
				}

			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //effect skill [level | level hittime]");
			}
		}
		else if (command.startsWith("admin_set_displayeffect"))
		{
			final GameObject target = activeChar.getTarget();
			if (!(target instanceof Npc))
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return false;
			}
			final Npc npc = (Npc) target;
			try
			{
				final String type = st.nextToken();
				final int diplayeffect = Integer.parseInt(type);
				npc.setDisplayEffect(diplayeffect);
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //set_displayeffect <id>");
			}
		}
		if (command.contains("menu"))
		{
			showMainPage(activeChar, command);
		}
		return true;
	}

	private boolean performAbnormal(int action, GameObject target)
	{
		if (target instanceof Creature)
		{
			final Creature character = (Creature) target;
			final AbnormalEffect eff = AbnormalEffect.getById(action);
			if (eff != null)
			{
				for (final AbnormalEffect ef : character.getAbnormalEffects())
				{
					if (ef != null && ef.getId() == eff.getId())
					{
						character.stopAbnormalEffect(ef);
						return true;
					}
				}
				character.startAbnormalEffect(eff);
				return true;
			}
		}
		return false;
	}

	private boolean performSpecial(int action, GameObject target)
	{
		if (target instanceof Player)
		{
			final Creature character = (Creature) target;
			final AbnormalEffect eff = AbnormalEffect.getById(action);
			if (eff != null)
			{
				for (final AbnormalEffect ef : character.getAbnormalEffects())
				{
					if (ef != null && ef.getId() == eff.getId())
					{
						character.stopAbnormalEffect(ef);
						return true;
					}
				}
				character.startAbnormalEffect(eff);
				return true;
			}
		}
		return false;
	}

	private boolean performSocial(int action, GameObject target, Player activeChar)
	{
		try
		{
			if (target instanceof Creature)
			{
				if (target instanceof ChestInstance)
				{
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return false;
				}
				if ((target instanceof Npc) && ((action < 1) || (action > 3)))
				{
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return false;
				}
				if ((target instanceof Player) && ((action < 2) || ((action > 18) && (action != SocialAction.LEVEL_UP))))
				{
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return false;
				}
				final Creature character = (Creature) target;
				character.broadcastPacket(new SocialAction(character.getObjectId(), action));
			}
			else
			{
				return false;
			}
		}
		catch (final Exception e)
		{}
		return true;
	}

	private void adminAtmosphere(String type, String state, int duration, Player activeChar)
	{
		GameServerPacket packet = null;
		if (type.equals("signsky"))
		{
			if (state.equals("dawn"))
			{
				packet = new SSQInfo(2);
			}
			else if (state.equals("dusk"))
			{
				packet = new SSQInfo(1);
			}
		}
		else if (type.equals("sky"))
		{
			if (state.equals("night"))
			{
				packet = SunSet.STATIC_PACKET;
			}
			else if (state.equals("day"))
			{
				packet = SunRise.STATIC_PACKET;
			}
			else if (state.equals("red"))
			{
				if (duration != 0)
				{
					packet = new ExRedSky(duration);
				}
				else
				{
					packet = new ExRedSky(10);
				}
			}
		}
		else
		{
			activeChar.sendMessage("Usage: //atmosphere <signsky dawn|dusk>|<sky day|night|red> <duration>");
		}
		
		if (packet != null)
		{
			final var pc = packet;
			GameObjectsStorage.getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendPacket(pc));
		}
	}

	private void playAdminSound(Player activeChar, String sound)
	{
		final PlaySound _snd = new PlaySound(1, sound, 0, 0, 0, 0, 0);
		activeChar.sendPacket(_snd);
		activeChar.broadcastPacket(_snd);
		activeChar.sendMessage("Playing " + sound + ".");
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void showMainPage(Player activeChar, String command)
	{
		final NpcHtmlMessage adminhtm = new NpcHtmlMessage(5);
		adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/effects_menu.htm");
		activeChar.sendPacket(adminhtm);

		if (command.contains("abnormal"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/abnormal.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.contains("special"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/special.htm");
			activeChar.sendPacket(adminhtm);
		}
		else if (command.contains("social"))
		{
			adminhtm.setFile(activeChar, activeChar.getLang(), "data/html/admin/social.htm");
			activeChar.sendPacket(adminhtm);
		}
	}
}