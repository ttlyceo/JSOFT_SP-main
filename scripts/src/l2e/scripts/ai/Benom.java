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
package l2e.scripts.ai;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ServerVariables;
import l2e.gameserver.listener.other.OnSiegeStatusListener;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.stats.MoveType;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Based on L2J Eternity-World
 */
public final class Benom extends AbstractNpcAI
{
	private static final int CASTLE = 8;
	
	private static final int VENOM = 29054;
	private static final int TELEPORT_CUBE = 29055;
	private static final int DUNGEON_KEEPER = 35506;
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	
	private static final int HOURS_BEFORE = 24;
	
	private static final Location[] TARGET_TELEPORTS =
	{
	        new Location(12860, -49158, 976), new Location(14878, -51339, 1024), new Location(15674, -49970, 864), new Location(15696, -48326, 864), new Location(14873, -46956, 1024), new Location(12157, -49135, -1088), new Location(12875, -46392, -288), new Location(14087, -46706, -288), new Location(14086, -51593, -288), new Location(12864, -51898, -288), new Location(15538, -49153, -1056), new Location(17001, -49149, -1064)
	};
	
	private static final Location TRHONE = new Location(11025, -49152, -537);
	private static final Location DUNGEON = new Location(11882, -49216, -3008);
	private static final Location TELEPORT = new Location(12589, -49044, -3008);
	private static final Location CUBE = new Location(12047, -49211, -3009);
	
	private static final SkillHolder VENOM_STRIKE = new SkillHolder(4993, 1);
	private static final SkillHolder SONIC_STORM = new SkillHolder(4994, 1);
	private static final SkillHolder VENOM_TELEPORT = new SkillHolder(4995, 1);
	private static final SkillHolder RANGE_TELEPORT = new SkillHolder(4996, 1);
	
	private Npc _venom;
	private Npc _massymore;
	
	private int _venomX;
	private int _venomY;
	private int _venomZ;
	
	private boolean _aggroMode = false;
	private boolean _prisonIsOpen = false;
	
	private static final int[] TARGET_TELEPORTS_OFFSET =
	{
	        650, 100, 100, 100, 100, 650, 200, 200, 200, 200, 200, 650
	};
	
	private static List<Player> _targets = new ArrayList<>();
	private final SiegeListener _listener = new SiegeListener();
	
	private class SiegeListener implements OnSiegeStatusListener
	{
		@Override
		public void onStart(Siege siege)
		{
			_aggroMode = true;
			_prisonIsOpen = false;
			if ((_venom != null) && !_venom.isDead())
			{
				_venom.setCurrentHp(_venom.getMaxHp());
				_venom.setCurrentMp(_venom.getMaxMp());
				_venom.enableSkill(VENOM_TELEPORT.getSkill());
				_venom.enableSkill(RANGE_TELEPORT.getSkill());
				startQuestTimer("tower_check", 30000, _venom, null, true);
			}
		}
		
		@Override
		public void onEnd(Siege siege, Clan winClan, Clan defClan)
		{
			_aggroMode = false;
			if ((_venom != null) && !_venom.isDead())
			{
				changeLocation(MoveTo.PRISON);
				_venom.disableSkill(VENOM_TELEPORT.getSkill(), 0);
				_venom.disableSkill(RANGE_TELEPORT.getSkill(), 0);
			}
			updateStatus(ALIVE);
			cancelQuestTimer("tower_check", _venom, null);
			cancelQuestTimer("raid_check", _venom, null);
		}
	}
	
	private Benom(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(DUNGEON_KEEPER, TELEPORT_CUBE);
		addTalkId(DUNGEON_KEEPER, TELEPORT_CUBE);
		addSpawnId(VENOM);
		addSpellFinishedId(VENOM);
		addAttackId(VENOM);
		addKillId(VENOM);
		addAggroRangeEnterId(VENOM);
		final var castle = CastleManager.getInstance().getCastleById(CASTLE);
		if (castle != null && castle.getTemplate() != null && castle.getSiege() != null)
		{
			final var siege = castle.getSiege();
			if (siege != null)
			{
				if (siege.isRegistrationTimeOver() && !siege.getAttackerClans().isEmpty())
				{
					_prisonIsOpen = true;
					changeLocation(MoveTo.PRISON);
				}
				siege.addListener(_listener);
			}
			
			for (final var spawns : SpawnParser.getInstance().getSpawnData())
			{
				if (spawns != null && spawns.getLastSpawn() != null)
				{
					switch (spawns.getId())
					{
						case VENOM :
							_venom = spawns.getLastSpawn();
							_venomX = _venom.getX();
							_venomY = _venom.getY();
							_venomZ = _venom.getZ();
							_venom.disableSkill(VENOM_TELEPORT.getSkill(), 0);
							_venom.disableSkill(RANGE_TELEPORT.getSkill(), 0);
							_venom.doRevive();
							((Attackable) _venom).setCanReturnToSpawnPoint(false);
							if (checkStatus() == DEAD)
							{
								_venom.deleteMe();
							}
							break;
						case DUNGEON_KEEPER :
							_massymore = spawns.getLastSpawn();
							break;
					}
				}
			}
			
			final long currentTime = System.currentTimeMillis();
			final long startSiegeDate = castle.getSiege().getSiegeStartTime();
			final long openingDungeonDate = startSiegeDate - (HOURS_BEFORE * 360000);
			
			if ((currentTime > openingDungeonDate) && (currentTime < startSiegeDate))
			{
				_prisonIsOpen = true;
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		switch (npc.getId())
		{
			case TELEPORT_CUBE :
			{
				talker.teleToLocation(TeleportWhereType.TOWN, true, talker.getReflection());
				break;
			}
			case DUNGEON_KEEPER :
			{
				if (_prisonIsOpen)
				{
					talker.teleToLocation(TELEPORT, 0, true, talker.getReflection());
				}
				else
				{
					return "<html><body>" + ServerStorage.getInstance().getString(talker.getLang(), "Benom.CLOSED") + "</body></html>";
				}
				break;
			}
		}
		return super.onTalk(npc, talker);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "tower_check" :
				if (CastleManager.getInstance().getCastleById(CASTLE).getSiege().getControlTowerCount() <= 1)
				{
					changeLocation(MoveTo.THRONE);
					broadcastNpcSay(_massymore, Say2.NPC_SHOUT, NpcStringId.OH_NO_THE_DEFENSES_HAVE_FAILED_IT_IS_TOO_DANGEROUS_TO_REMAIN_INSIDE_THE_CASTLE_FLEE_EVERY_MAN_FOR_HIMSELF);
					cancelQuestTimer("tower_check", npc, null);
					startQuestTimer("raid_check", 10000, npc, null, true);
				}
				break;
			case "raid_check" :
				if ((npc.getX() != _venomX && npc.getY() != _venomY && npc.getAI().getIntention() == CtrlIntention.ACTIVE) || (!npc.isInsideRadius(_venomX, _venomY, _venomZ, 2500, true, true)))
				{
					if (!npc.isTeleporting())
					{
						((Attackable) npc).clearAggroList(true);
						npc.broadcastPacketToOthers(new MagicSkillUse(npc, npc, 2036, 1, 500, 0));
						npc.teleToLocation(new Location(_venomX, _venomY, _venomZ), false, npc.getReflection());
					}
				}
				break;
			case "cube_despawn" :
				if (npc != null)
				{
					npc.deleteMe();
				}
				break;
		}
		return event;
	}
	
	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (isSummon)
		{
			return super.onAggroRangeEnter(npc, player, isSummon);
		}
		
		if (_aggroMode && (_targets.size() < 10) && (getRandom(3) < 1) && !player.isDead())
		{
			_targets.add(player);
		}
		return super.onAggroRangeEnter(npc, player, isSummon);
	}
	
	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		switch (skill.getId())
		{
			case 4222 :
				npc.teleToLocation(new Location(_venomX, _venomY, _venomZ), false, npc.getReflection());
				break;
			case 4995 :
				teleportTarget(player);
				((Attackable) npc).getAggroList().stopHating(player);
				break;
			case 4996 :
				teleportTarget(player);
				((Attackable) npc).getAggroList().stopHating(player);
				if ((_targets != null) && (_targets.size() > 0))
				{
					for (final Player target : _targets)
					{
						final long x = player.getX() - target.getX();
						final long y = player.getY() - target.getY();
						final long z = player.getZ() - target.getZ();
						final long range = 250;
						if (((x * x) + (y * y) + (z * z)) <= (range * range))
						{
							teleportTarget(target);
							((Attackable) npc).getAggroList().stopHating(target);
						}
					}
					_targets.clear();
				}
				break;
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		if (!npc.isTeleporting())
		{
			if (checkStatus() == DEAD)
			{
				npc.deleteMe();
			}
			else
			{
				npc.doRevive();
				broadcastNpcSay(npc, Say2.NPC_SHOUT, NpcStringId.WHO_DARES_TO_COVET_THE_THRONE_OF_OUR_CASTLE_LEAVE_IMMEDIATELY_OR_YOU_WILL_PAY_THE_PRICE_OF_YOUR_AUDACITY_WITH_YOUR_VERY_OWN_BLOOD);
			}
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (!npc.isInsideRadius(_venomX, _venomY, _venomZ, 2500, true, true))
		{
			((Attackable) npc).clearAggroList(true);
			npc.broadcastPacketToOthers(new MagicSkillUse(npc, npc, 2036, 1, 500, 0));
			npc.teleToLocation(new Location(_venomX, _venomY, _venomZ), false, npc.getReflection());
		}
		
		final double distance = Math.sqrt(npc.getPlanDistanceSq(attacker.getX(), attacker.getY()));
		if (_aggroMode && (npc.getCurrentHp() < (npc.getMaxHp() / 2)) && (getRandom(100) < 5) && !npc.isCastingNow())
		{
			npc.setTarget(attacker);
			npc.doCast(VENOM_TELEPORT.getSkill());
		}
		else if (_aggroMode && (npc.getCurrentHp() < (npc.getMaxHp() / 3)) && (getRandom(100) < 2) && !npc.isCastingNow())
		{
			npc.setTarget(attacker);
			npc.doCast(RANGE_TELEPORT.getSkill());
		}
		else if ((distance > 300) && (getRandom(100) < 10) && !npc.isCastingNow())
		{
			npc.setTarget(attacker);
			npc.doCast(VENOM_STRIKE.getSkill());
		}
		else if ((getRandom(100) < 10) && !npc.isCastingNow())
		{
			npc.setTarget(attacker);
			npc.doCast(SONIC_STORM.getSkill());
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		updateStatus(DEAD);
		broadcastNpcSay(npc, Say2.NPC_SHOUT, NpcStringId.ITS_NOT_OVER_YET_IT_WONT_BE_OVER_LIKE_THIS_NEVER);
		if (!CastleManager.getInstance().getCastleById(CASTLE).getSiege().getIsInProgress())
		{
			final Npc cube = addSpawn(TELEPORT_CUBE, CUBE, false, 0);
			startQuestTimer("cube_despawn", 120000, cube, null);
		}
		cancelQuestTimer("raid_check", npc, null);
		return super.onKill(npc, killer, isSummon);
	}
	
	private void changeLocation(MoveTo loc)
	{
		switch (loc)
		{
			case THRONE :
				_venom.getTemplate().setAggroRange(1500);
				_venom.getTemplate().setBaseMoveSpeed(MoveType.RUN, 350);
				_venom.teleToLocation(TRHONE, false, _venom.getReflection());
				break;
			case PRISON :
				if ((_venom == null) || _venom.isDead())
				{
					_venom = addSpawn(VENOM, DUNGEON, false, 0);
				}
				else
				{
					_venom.getTemplate().setAggroRange(0);
					_venom.getTemplate().setBaseMoveSpeed(MoveType.RUN, 220);
					_venom.teleToLocation(DUNGEON, false, _venom.getReflection());
				}
				cancelQuestTimer("raid_check", _venom, null);
				cancelQuestTimer("tower_check", _venom, null);
				break;
		}
		_venomX = _venom.getX();
		_venomY = _venom.getY();
		_venomZ = _venom.getZ();
	}
	
	private void teleportTarget(Player player)
	{
		if ((player != null) && !player.isDead())
		{
			final int rnd = getRandom(11);
			player.teleToLocation(TARGET_TELEPORTS[rnd], TARGET_TELEPORTS_OFFSET[rnd], true, player.getReflection());
			player.getAI().setIntention(CtrlIntention.IDLE);
		}
	}
	
	private int checkStatus()
	{
		return ServerVariables.getInt("VenomStatus", ALIVE);
	}
	
	private void updateStatus(int status)
	{
		ServerVariables.set("VenomStatus", status);
	}
	
	private enum MoveTo
	{
		THRONE, PRISON
	}
	
	public static void main(String[] args)
	{
		new Benom(Benom.class.getSimpleName(), "ai");
	}
}
