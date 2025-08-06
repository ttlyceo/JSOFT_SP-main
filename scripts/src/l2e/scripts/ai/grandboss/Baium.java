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
package l2e.scripts.ai.grandboss;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.geodata.GeoEngine;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.MountType;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.model.zone.type.NoRestartZone;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.EarthQuake;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.gameserver.network.serverpackets.SocialAction;
import l2e.scripts.ai.AbstractNpcAI;

public class Baium extends AbstractNpcAI
{
	private static final Location[] TELEPORT_OUT_LOC =
	{
	        new Location(108784, 16000, -4928), new Location(113824, 10448, -5164), new Location(115488, 22096, -5168),
	};
	
	private static final Location[] ARCHANGEL_LOC =
	{
	        new Location(115792, 16608, 10136, 0), new Location(115168, 17200, 10136, 0), new Location(115780, 15564, 10136, 13620), new Location(114880, 16236, 10136, 5400), new Location(114239, 17168, 10136, -1992)
	};
	
	private static final NoRestartZone _zone = ZoneManager.getInstance().getZoneById(70051, NoRestartZone.class);
	
	private GrandBossInstance _baium = null;
	private Npc _statue = null;
	private long _lastAttack = 0L;
	
	private Baium(String name, String descr)
	{
		super(name, descr);
		
		addTalkId(31862, 31842, 29025);
		addStartNpc(31862, 31842, 29025);
		
		addAttackId(29020, 29021);
		addKillId(29020);
		
		addSpellFinishedId(29020);
		
		final StatsSet info = EpicBossManager.getInstance().getStatsSet(29020);
		final double curr_hp = info.getDouble("currentHP");
		final double curr_mp = info.getDouble("currentMP");
		final int loc_x = info.getInteger("loc_x");
		final int loc_y = info.getInteger("loc_y");
		final int loc_z = info.getInteger("loc_z");
		final int heading = info.getInteger("heading");
		final long respawnTime = info.getLong("respawnTime");
		
		switch (getStatus())
		{
			case 1 :
			{
				setStatus(0);
			}
			case 0 :
			{
				_statue = addSpawn(29025, new Location(116024, 17436, 10104, -25348), false, 0);
				break;
			}
			case 2 :
			{
				_baium = (GrandBossInstance) addSpawn(29020, loc_x, loc_y, loc_z, heading, false, 0);
				_baium.setCurrentHpMp(curr_hp, curr_mp);
				_lastAttack = System.currentTimeMillis();
				addBoss(_baium);
				
				for (final Location loc : ARCHANGEL_LOC)
				{
					addSpawn(29021, loc, false, 0, true);
				}
				startQuestTimer("CHECK_ATTACK", 60000, _baium, null);
				break;
			}
			case 3 :
			{
				final long remain = respawnTime - System.currentTimeMillis();
				if (remain > 0)
				{
					startQuestTimer("CLEAR_STATUS", remain, null, null);
				}
				else
				{
					notifyEvent("CLEAR_STATUS", null, null);
				}
				break;
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "teleportOut" :
			{
				final Location loc = TELEPORT_OUT_LOC[getRandom(TELEPORT_OUT_LOC.length)];
				if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
				{
					if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
					{
						for (final Player member : player.getParty().getMembers())
						{
							if (member != null)
							{
								if (member.getObjectId() == player.getObjectId())
								{
									continue;
								}
								
								if (!Util.checkIfInRange(1000, player, member, true))
								{
									continue;
								}
								
								if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
								{
									continue;
								}
								member.teleToLocation(loc.getX() + getRandom(100), loc.getY() + getRandom(100), loc.getZ(), true, true, member.getReflection());
							}
						}
					}
				}
				player.teleToLocation(loc.getX() + getRandom(100), loc.getY() + getRandom(100), loc.getZ(), true, true, player.getReflection());
				break;
			}
			case "wakeUp" :
			{
				if (getStatus() == 0)
				{
					setStatus(1);
					startQuestTimer("finalWakeUp", Config.BAIUM_SPAWN_DELAY > 0 ? (Config.BAIUM_SPAWN_DELAY * 60000L) : 1000, npc, player);
				}
				else
				{
					player.sendMessage((new ServerMessage("Baium.AWAKENS", player.getLang())).toString());
				}
				break;
			}
			case "finalWakeUp" :
			{
				if (getStatus() == 1)
				{
					npc.deleteMe();
					_statue = null;
					setStatus(2);
					_baium = (GrandBossInstance) addSpawn(29020, new Location(116024, 17436, 10104, -25348), false, 0);
					_baium.disableCoreAI(true);
					addBoss(_baium);
					_lastAttack = System.currentTimeMillis();
					startQuestTimer("WAKEUP_ACTION", 50, _baium, null);
					startQuestTimer("MANAGE_EARTHQUAKE", 2000, _baium, null);
					startQuestTimer("SOCIAL_ACTION", 10000, _baium, player);
					startQuestTimer("CHECK_ATTACK", 60000, _baium, null);
				}
				break;
			}
			case "WAKEUP_ACTION" :
			{
				if (npc != null)
				{
					_zone.broadcastPacket(new SocialAction(_baium.getObjectId(), 2));
				}
				break;
			}
			case "MANAGE_EARTHQUAKE" :
			{
				if (npc != null)
				{
					_zone.broadcastPacket(new EarthQuake(npc.getX(), npc.getY(), npc.getZ(), 40, 10));
					_zone.broadcastPacket(new PlaySound("BS02_A"));
				}
				break;
			}
			case "SOCIAL_ACTION" :
			{
				if (npc != null)
				{
					_zone.broadcastPacket(new SocialAction(npc.getObjectId(), 3));
					startQuestTimer("PLAYER_PORT", 6000, npc, player);
				}
				break;
			}
			case "PLAYER_PORT" :
			{
				if (npc != null)
				{
					if ((player != null) && player.isInsideRadius(npc, 16000, true, false))
					{
						player.teleToLocation(new Location(115910, 17337, 10105), false, player.getReflection());
						startQuestTimer("PLAYER_KILL", 3000, npc, player);
					}
					else
					{
						final Player randomPlayer = getRandomPlayer(npc);
						if (randomPlayer != null)
						{
							randomPlayer.teleToLocation(new Location(115910, 17337, 10105), false, randomPlayer.getReflection());
							startQuestTimer("PLAYER_KILL", 3000, npc, randomPlayer);
						}
						else
						{
							startQuestTimer("PLAYER_KILL", 3000, npc, null);
						}
					}
				}
				break;
			}
			case "PLAYER_KILL" :
			{
				if ((player != null) && player.isInsideRadius(npc, 16000, true, false))
				{
					_zone.broadcastPacket(new SocialAction(npc.getObjectId(), 1));
					npc.broadcastSay(Say2.NPC_ALL, NpcStringId.HOW_DARE_YOU_WAKE_ME_NOW_YOU_SHALL_DIE, player.getName(null));
					npc.setTarget(player);
					npc.doCast(new SkillHolder(4136, 1).getSkill());
				}
				
				for (final Player insidePlayer : _zone.getPlayersInside())
				{
					if (insidePlayer.isHero())
					{
						_zone.broadcastPacket(new ExShowScreenMessage(NpcStringId.NOT_EVEN_THE_GODS_THEMSELVES_COULD_TOUCH_ME_BUT_YOU_S1_YOU_DARE_CHALLENGE_ME_IGNORANT_MORTAL, 2, 4000, insidePlayer.getName(null)));
						break;
					}
				}
				startQuestTimer("SPAWN_ARCHANGEL", 8000, npc, player);
				break;
			}
			case "SPAWN_ARCHANGEL" :
			{
				_baium.disableCoreAI(false);
				
				for (final Location loc : ARCHANGEL_LOC)
				{
					addSpawn(29021, loc, false, 0, true);
				}
				
				if ((player != null) && !player.isDead())
				{
					attackPlayer((Attackable) npc, player);
				}
				else
				{
					final Player randomPlayer = getRandomPlayer(npc);
					if (randomPlayer != null)
					{
						attackPlayer((Attackable) npc, randomPlayer);
					}
				}
				break;
			}
			case "CHECK_ATTACK" :
			{
				if ((npc != null) && ((_lastAttack + 1800000L) < System.currentTimeMillis()))
				{
					notifyEvent("CLEAR_ZONE", null, null);
					if (_statue != null)
					{
						_statue.deleteMe();
						_statue = null;
					}
					_statue = addSpawn(29025, new Location(116024, 17436, 10104, -25348), false, 0);
					setStatus(0);
				}
				else if (npc != null)
				{
					if (((_lastAttack + 300000L) < System.currentTimeMillis()) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.75)))
					{
						npc.setTarget(npc);
						npc.doCast(new SkillHolder(4135, 1).getSkill());
					}
					startQuestTimer("CHECK_ATTACK", 60000, npc, null);
				}
				break;
			}
			case "CLEAR_STATUS" :
			{
				setStatus(0);
				if (_statue != null)
				{
					_statue.deleteMe();
					_statue = null;
				}
				_statue = addSpawn(29025, new Location(116024, 17436, 10104, -25348), false, 0);
				break;
			}
			case "CLEAR_ZONE" :
			{
				for (final Creature charInside : _zone.getCharactersInside())
				{
					if (charInside != null && charInside.getReflectionId() == 0)
					{
						if (charInside.isNpc())
						{
							charInside.deleteMe();
						}
						else if (charInside.isPlayer())
						{
							notifyEvent("teleportOut", null, (Player) charInside);
						}
					}
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String htmltext = "";
		
		switch (npc.getId())
		{
			case 31862 :
			{
				if (player.isFlying())
				{
					return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "Baium.PLAYER_FLY") + "</body></html>";
				}
				else if (getStatus() == 3)
				{
					return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "Baium.DEAD") + "</body></html>";
				}
				else if (getStatus() == 2)
				{
					return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "Baium.IN_FIGHT") + "</body></html>";
				}
				else if (!hasQuestItems(player, 4295))
				{
					return "<html><body>" + ServerStorage.getInstance().getString(player.getLang(), "Baium.NOT_ITEM") + "</body></html>";
				}
				else
				{
					if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
					{
						if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
						{
							for (final Player member : player.getParty().getMembers())
							{
								if (member != null)
								{
									if (member.getObjectId() == player.getObjectId())
									{
										continue;
									}
									
									if (!Util.checkIfInRange(1000, player, member, true) || member.getInventory().getItemByItemId(4295) == null)
									{
										continue;
									}
									
									if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
									{
										continue;
									}
									takeItems(member, 4295, 1);
									member.teleToLocation(114077, 15882, 10078, true, member.getReflection());
								}
							}
						}
					}
					takeItems(player, 4295, 1);
					player.teleToLocation(new Location(114077, 15882, 10078), true, player.getReflection());
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (!_zone.isCharacterInZone(npc) && (_baium != null))
		{
			_baium.teleToLocation(new Location(116024, 17436, 10104, -25348), true, _baium.getReflection());
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (attacker != null)
		{
			if (attacker.isPlayer() || attacker.isSummon())
			{
				_lastAttack = System.currentTimeMillis();
				
				if (npc.getId() == 29020)
				{
					if ((attacker.getMountType() == MountType.STRIDER) && (attacker.getFirstEffect(new SkillHolder(4258, 1).getId()) == null) && !npc.isSkillDisabled(new SkillHolder(4258, 1).getSkill()))
					{
						npc.setTarget(attacker);
						npc.doCast(new SkillHolder(4258, 1).getSkill());
					}
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (_zone.isCharacterInZone(killer))
		{
			addSpawn(31842, new Location(115017, 15549, 10090), false, 900000);
			_zone.broadcastPacket(new PlaySound("BS01_D"));
			final long respawnTime = EpicBossManager.getInstance().setRespawnTime(29020, Config.BAIUM_RESPAWN_PATTERN);
			startQuestTimer("CLEAR_STATUS", (respawnTime - System.currentTimeMillis()), null, null);
			startQuestTimer("CLEAR_ZONE", 900000, null, null);
			cancelQuestTimer("CHECK_ATTACK", npc, null);
			
			for (final Npc obj : World.getInstance().getAroundNpc(npc, 6000, 200))
			{
				if (obj.getId() == 29021)
				{
					obj.deleteMe();
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private int getStatus()
	{
		return EpicBossManager.getInstance().getBossStatus(29020);
	}
	
	private void addBoss(GrandBossInstance grandboss)
	{
		EpicBossManager.getInstance().addBoss(grandboss);
	}
	
	private void setStatus(int status)
	{
		EpicBossManager.getInstance().setBossStatus(29020, status, true);
	}
	
	private Player getRandomPlayer(Npc npc)
	{
		final List<Player> result = new ArrayList<>();
		for (final Player player : World.getInstance().getAroundPlayers(npc, 6000, 200))
		{
			if (player != null)
			{
				if (!player.isDead() && !player.isInvisible() && GeoEngine.getInstance().canSeeTarget(npc, player))
				{
					result.add(player);
				}
			}
		}
		return (result.isEmpty()) ? null : result.get(getRandom(result.size()));
	}
	
	@Override
	public boolean unload(boolean removeFromList)
	{
		if (_statue != null)
		{
			_statue.deleteMe();
			_statue = null;
		}
		notifyEvent("CLEAR_ZONE", null, null);
		return super.unload(removeFromList);
	}
	
	public static void main(String[] args)
	{
		new Baium(Baium.class.getSimpleName(), "ai");
	}
}
