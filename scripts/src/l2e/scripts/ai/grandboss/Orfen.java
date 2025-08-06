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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.zone.type.BossZone;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.network.serverpackets.PlaySound;
import l2e.scripts.ai.AbstractNpcAI;

public class Orfen extends AbstractNpcAI
{
	private static final Location[] _pos =
	{
	                new Location(43728, 17220, -4342), new Location(55024, 17368, -5412), new Location(53504, 21248, -5486), new Location(53248, 24576, -5262)
	};

	private static final NpcStringId[] _text =
	{
	                NpcStringId.S1_STOP_KIDDING_YOURSELF_ABOUT_YOUR_OWN_POWERLESSNESS, NpcStringId.S1_ILL_MAKE_YOU_FEEL_WHAT_TRUE_FEAR_IS, NpcStringId.YOURE_REALLY_STUPID_TO_HAVE_CHALLENGED_ME_S1_GET_READY, NpcStringId.S1_DO_YOU_THINK_THATS_GOING_TO_WORK
	};
	
	private GrandBossInstance _boss = null;
	private static List<Attackable> _minions = new CopyOnWriteArrayList<>();
	private static BossZone _zone = (BossZone) ZoneManager.getInstance().getZoneById(12013);;
	private long _lastMsg = 0L;

	private Orfen(String name, String descr)
	{
		super(name, descr);
		final int[] mobs =
		{
		        29014, 29016, 29018
		};
		registerMobs(mobs);
		final StatsSet info = EpicBossManager.getInstance().getStatsSet(29014);
		final int status = EpicBossManager.getInstance().getBossStatus(29014);
		if (status == 3)
		{
			final long temp = info.getLong("respawnTime") - System.currentTimeMillis();

			if (temp > 0)
			{
				startQuestTimer("orfen_unlock", temp, null, null);
			}
			else
			{
				final int i = getRandom(10);
				Location loc;
				if (i < 4)
				{
					loc = _pos[1];
				}
				else if (i < 7)
				{
					loc = _pos[2];
				}
				else
				{
					loc = _pos[3];
				}
				_boss = (GrandBossInstance) addSpawn(29014, loc, false, 0);
				EpicBossManager.getInstance().setBossStatus(29014, 0, false);
				spawnBoss(_boss);
			}
		}
		else
		{
			final int loc_x = info.getInteger("loc_x");
			final int loc_y = info.getInteger("loc_y");
			final int loc_z = info.getInteger("loc_z");
			final int heading = info.getInteger("heading");
			final int hp = info.getInteger("currentHP");
			final int mp = info.getInteger("currentMP");
			_boss = (GrandBossInstance) addSpawn(29014, loc_x, loc_y, loc_z, heading, false, 0);
			_boss.setCurrentHpMp(hp, mp);
			spawnBoss(_boss);
		}
	}

	public void setSpawnPoint(Npc npc, int index)
	{
		((Attackable) npc).clearAggroList(false);
		npc.getAI().setIntention(CtrlIntention.IDLE, null, null);
		final Spawner spawn = npc.getSpawn();
		spawn.setLocation(_pos[index]);
		npc.teleToLocation(_pos[index], false, npc.getReflection());
	}

	public void spawnBoss(GrandBossInstance npc)
	{
		EpicBossManager.getInstance().addBoss(npc);
		npc.broadcastPacketToOthers(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		startQuestTimer("check_orfen_pos", 10000, npc, null, true);

		final int x = npc.getX();
		final int y = npc.getY();
		Attackable mob;
		mob = (Attackable) addSpawn(29016, x + 100, y + 100, npc.getZ(), 0, false, 0);
		mob.setIsRaidMinion(true);
		_minions.add(mob);
		mob = (Attackable) addSpawn(29016, x + 100, y - 100, npc.getZ(), 0, false, 0);
		mob.setIsRaidMinion(true);
		_minions.add(mob);
		mob = (Attackable) addSpawn(29016, x - 100, y + 100, npc.getZ(), 0, false, 0);
		mob.setIsRaidMinion(true);
		_minions.add(mob);
		mob = (Attackable) addSpawn(29016, x - 100, y - 100, npc.getZ(), 0, false, 0);
		mob.setIsRaidMinion(true);
		_minions.add(mob);
		startQuestTimer("check_minion_loc", 10000, npc, null, true);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("orfen_unlock"))
		{
			final int i = getRandom(10);
			Location loc;
			if (i < 4)
			{
				loc = _pos[1];
			}
			else if (i < 7)
			{
				loc = _pos[2];
			}
			else
			{
				loc = _pos[3];
			}
			_boss = (GrandBossInstance) addSpawn(29014, loc, false, 0);
			EpicBossManager.getInstance().setBossStatus(29014, 0, true);
			spawnBoss(_boss);
		}
		else if (event.equalsIgnoreCase("check_orfen_pos"))
		{
			if ((npc.isScriptValue(1) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.95))) || (!_zone.isInsideZone(npc) && npc.isScriptValue(0)))
			{
				setSpawnPoint(npc, getRandom(3) + 1);
				npc.setScriptValue(0);
			}
			else if (npc.isScriptValue(1) && !_zone.isInsideZone(npc))
			{
				setSpawnPoint(npc, 0);
			}
		}
		else if (event.equalsIgnoreCase("check_minion_loc"))
		{
			for (int i = 0; i < _minions.size(); i++)
			{
				final Attackable mob = _minions.get(i);
				if (mob != null)
				{
					if (!npc.isInsideRadius(mob, 3000, false, false))
					{
						mob.teleToLocation(npc.getX(), npc.getY(), npc.getZ(), true, mob.getReflection());
						((Attackable) npc).clearAggroList(false);
						npc.getAI().setIntention(CtrlIntention.IDLE, null, null);
					}
				}
			}
		}
		else if (event.equalsIgnoreCase("despawn_minions"))
		{
			for (int i = 0; i < _minions.size(); i++)
			{
				final Attackable mob = _minions.get(i);
				if (mob != null)
				{
					mob.decayMe();
				}
			}
			_minions.clear();
		}
		else if (event.equalsIgnoreCase("spawn_minion"))
		{
			final Attackable mob = (Attackable) addSpawn(29016, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0);
			mob.setIsRaidMinion(true);
			_minions.add(mob);
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onSkillSee(Npc npc, Player caster, Skill skill, GameObject[] targets, boolean isSummon)
	{
		if (npc.getId() == 29014)
		{
			final Creature originalCaster = isSummon ? caster.getSummon() : caster;
			if ((originalCaster != null && skill.getAggroPoints() > 0) && Rnd.chance(5) && npc.isInsideRadius(originalCaster, 1000, false, false))
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), _text[getRandom(4)]);
				packet.addStringParameter(caster.getName(null).toString());
				npc.broadcastPacketToOthers(2000, packet);
				originalCaster.teleToLocation(npc.getX(), npc.getY(), npc.getZ(), true, originalCaster.getReflection());
				npc.setTarget(originalCaster);
				npc.doCast(SkillsParser.getInstance().getInfo(4064, 1));
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}

	@Override
	public String onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		if ((caller == null) || (npc == null) || npc.isCastingNow())
		{
			return super.onFactionCall(npc, caller, attacker, isSummon);
		}
		final int npcId = npc.getId();
		final int callerId = caller.getId();
		if ((npcId == 29016) && (getRandom(20) == 0))
		{
			npc.setTarget(attacker);
			npc.doCast(SkillsParser.getInstance().getInfo(4067, 4));
		}
		else if (npcId == 29018)
		{
			int chance = 1;
			if (callerId == 29014)
			{
				chance = 9;
			}
			if ((callerId != 29018) && (caller.getCurrentHp() < (caller.getMaxHp() / 2.0)) && (getRandom(10) < chance))
			{
				npc.getAI().setIntention(CtrlIntention.IDLE, null, null);
				npc.setTarget(caller);
				npc.doCast(SkillsParser.getInstance().getInfo(4516, 1));
			}
		}
		return super.onFactionCall(npc, caller, attacker, isSummon);
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == 29014)
		{
			if (npc.isScriptValue(0) && ((npc.getCurrentHp() - damage) < (npc.getMaxHp() / 2)))
			{
				npc.setScriptValue(1);
				setSpawnPoint(npc, 0);
			}
			else if (npc.isInsideRadius(attacker, 1000, false, false) && !npc.isInsideRadius(attacker, 300, false, false) && Rnd.chance(1) && (_lastMsg + 120000L) < System.currentTimeMillis())
			{
				_lastMsg = System.currentTimeMillis();
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npcId, _text[getRandom(3)]);
				packet.addStringParameter(attacker.getName(null).toString());
				npc.broadcastPacketToOthers(2000, packet);
				attacker.teleToLocation(npc.getX(), npc.getY(), npc.getZ(), true, attacker.getReflection());
				npc.setTarget(attacker);
				npc.doCast(SkillsParser.getInstance().getInfo(4064, 1));
			}
		}
		else if (npcId == 29018)
		{
			if (!npc.isCastingNow() && ((npc.getCurrentHp() - damage) < (npc.getMaxHp() / 2.0)))
			{
				npc.setTarget(attacker);
				npc.doCast(SkillsParser.getInstance().getInfo(4516, 1));
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == 29014)
		{
			_lastMsg = 0;
			npc.broadcastPacketToOthers(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			final long respawnTime = EpicBossManager.getInstance().setRespawnTime(29014, Config.ORFEN_RESPAWN_PATTERN);
			startQuestTimer("orfen_unlock", (respawnTime - System.currentTimeMillis()), null, null);
			cancelQuestTimer("check_minion_loc", npc, null);
			cancelQuestTimer("check_orfen_pos", npc, null);
			startQuestTimer("despawn_minions", 20000, null, null);
			cancelQuestTimers("spawn_minion");
			_boss = null;
		}
		else if ((EpicBossManager.getInstance().getBossStatus(29014) == 0) && (npc.getId() == 29016))
		{
			_minions.remove(npc);
			startQuestTimer("spawn_minion", 360000, npc, null);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public boolean unload(boolean removeFromList)
	{
		if (_boss != null)
		{
			_boss.deleteMe();
			_boss = null;
		}
		if (_minions != null)
		{
			for (int i = 0; i < _minions.size(); i++)
			{
				final Attackable mob = _minions.get(i);
				if (mob != null)
				{
					mob.decayMe();
				}
			}
		}
		_minions.clear();
		cancelQuestTimers("orfen_unlock");
		return super.unload(removeFromList);
	}

	public static void main(String[] args)
	{
		new Orfen(Orfen.class.getSimpleName(), "ai");
	}
}
