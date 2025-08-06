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
package l2e.scripts.ai.freya;


import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;

public class FreyaThrone extends Fighter
{
	private final ZoneType _zone = ZoneManager.getInstance().getZoneById(20503);
	private static final int ETERNAL_BLIZZARD = 6274;
	private static final int ICE_BALL = 6278;
	private static final int SUMMON_ELEMENTAL = 6277;
	private static final int SELF_NOVA = 6279;
	private static final int DEATH_SENTENCE = 6280;
	private static final int ANGER = 6285;

	private long _blizzardReuseTimer = 0;
	private long _iceballReuseTimer = 0;
	private long _summonReuseTimer = 0;
	private long _selfnovaReuseTimer = 0;
	private long _deathsentenceReuseTimer = 0;
	private long _angerReuseTimer = 0;

	private final int _blizzardReuseDelay = 60;
	private final int _iceballReuseDelay = 20;
	private final int _summonReuseDelay = 60;
	private final int _selfnovaReuseDelay = 70;
	private final int _deathsentenceReuseDelay = 50;
	private final int _angerReuseDelay = 50;

	private final int _summonChance = 70;
	private final int _iceballChance = 60;
	private final int _deathsentenceChance = 60;
	private final int _angerChance = 60;

	public FreyaThrone(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();
		final Creature mostHated = actor.getAggroList().getMostHated();

		if (!actor.isCastingNow() && (_blizzardReuseTimer < System.currentTimeMillis()))
		{
			actor.doCast(SkillsParser.getInstance().getInfo(ETERNAL_BLIZZARD, 1));
			final GameServerPacket packet = new ExShowScreenMessage(NpcStringId.STRONG_MAGIC_POWER_CAN_BE_FELT_FROM_SOMEWHERE, 2, 3000);
			final ReflectionWorld instance = ReflectionManager.getInstance().getWorld(actor.getReflectionId());
			if ((instance != null) && (instance.getAllowed() != null))
			{
				for (final int objectId : instance.getAllowed())
				{
					final Player activeChar = GameObjectsStorage.getPlayer(objectId);
					if (activeChar != null)
					{
						activeChar.sendPacket(packet);
					}

				}
			}
			else
			{
				if (actor.getReflectionId() == 0)
				{
					final var players = _zone.getPlayersInside(ReflectionManager.DEFAULT);
					if (!players.isEmpty())
					{
						for (final var activeChar : players)
						{
							activeChar.sendPacket(packet);
						}
					}
				}
			}
			_blizzardReuseTimer = System.currentTimeMillis() + (_blizzardReuseDelay * 1000L);
		}

		if (!actor.isCastingNow() && !actor.isMoving() && (_iceballReuseTimer < System.currentTimeMillis()) && Rnd.chance(_iceballChance))
		{
			if ((mostHated != null) && !mostHated.isDead() && mostHated.isInRange(actor, 1000))
			{
				actor.setTarget(mostHated);
				actor.doCast(SkillsParser.getInstance().getInfo(ICE_BALL, 1));
				_iceballReuseTimer = System.currentTimeMillis() + (_iceballReuseDelay * 1000L);
			}
		}

		if (!actor.isCastingNow() && (_summonReuseTimer < System.currentTimeMillis()) && Rnd.chance(_summonChance))
		{
			actor.doCast(SkillsParser.getInstance().getInfo(SUMMON_ELEMENTAL, 1));
			for (final Npc npc : World.getInstance().getAroundNpc(actor, 800, 200))
			{
				if (npc != null && npc.isMonster() && !npc.isDead())
				{
					npc.makeTriggerCast(SkillsParser.getInstance().getInfo(SUMMON_ELEMENTAL, 1), npc);
				}
			}
			_summonReuseTimer = System.currentTimeMillis() + (_summonReuseDelay * 1000L);
		}

		if (!actor.isCastingNow() && (_selfnovaReuseTimer < System.currentTimeMillis()))
		{
			actor.doCast(SkillsParser.getInstance().getInfo(SELF_NOVA, 1));
			_selfnovaReuseTimer = System.currentTimeMillis() + (_selfnovaReuseDelay * 1000L);
		}

		if (!actor.isCastingNow() && !actor.isMoving() && (_deathsentenceReuseTimer < System.currentTimeMillis()) && Rnd.chance(_deathsentenceChance))
		{
			if ((mostHated != null) && !mostHated.isDead() && mostHated.isInRange(actor, 1000))
			{
				actor.setTarget(mostHated);
				actor.doCast(SkillsParser.getInstance().getInfo(DEATH_SENTENCE, 1));
				_deathsentenceReuseTimer = System.currentTimeMillis() + (_deathsentenceReuseDelay * 1000L);
			}
		}

		if (!actor.isCastingNow() && !actor.isMoving() && (_angerReuseTimer < System.currentTimeMillis()) && Rnd.chance(_angerChance))
		{
			actor.setTarget(actor);
			actor.doCast(SkillsParser.getInstance().getInfo(ANGER, 1));
			_angerReuseTimer = System.currentTimeMillis() + (_angerReuseDelay * 1000L);
		}
		super.thinkAttack();
	}

	@Override
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();

		final long generalReuse = System.currentTimeMillis() + 40000L;
		_blizzardReuseTimer += generalReuse + (Rnd.get(1, 20) * 1000L);
		_iceballReuseTimer += generalReuse + (Rnd.get(1, 20) * 1000L);
		_summonReuseTimer += generalReuse + (Rnd.get(1, 20) * 1000L);
		_selfnovaReuseTimer += generalReuse + (Rnd.get(1, 20) * 1000L);
		_deathsentenceReuseTimer += generalReuse + (Rnd.get(1, 20) * 1000L);
		_angerReuseTimer += generalReuse + (Rnd.get(1, 20) * 1000L);

		aggroPlayers(false);
	}

	@Override
	protected boolean thinkActive()
	{
		aggroPlayers(true);
		return super.thinkActive();
	}
	
	private void aggroPlayers(boolean searchTarget)
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead())
		{
			return;
		}
		
		final ReflectionWorld instance = ReflectionManager.getInstance().getWorld(getActiveChar().getReflectionId());
		if ((instance != null) && (instance.getAllowed() != null))
		{
			final List<Player> activeList = new ArrayList<>();
			for (final int objectId : instance.getAllowed())
			{
				final Player activeChar = GameObjectsStorage.getPlayer(objectId);
				if (activeChar != null)
				{
					if (_zone != null && _zone.isInsideZone(activeChar) || activeChar.isDead())
					{
						continue;
					}
					
					if (activeChar.getReflectionId() == actor.getReflectionId())
					{
						actor.addDamageHate(activeChar, 0, 2);
						if (searchTarget)
						{
							activeList.add(activeChar);
						}
					}
				}
			}
			
			if (!activeList.isEmpty())
			{
				final var attacked = activeList.get(Rnd.get(activeList.size()));
				if (attacked != null)
				{
					actor.getAI().setIntention(CtrlIntention.ATTACK, attacked);
				}
			}
			activeList.clear();
		}
		else
		{
			if (actor.getReflectionId() == 0)
			{
				final List<Player> activeList = new ArrayList<>();
				final var players = _zone.getPlayersInside(ReflectionManager.DEFAULT);
				if (!players.isEmpty())
				{
    				for (final var activeChar : players)
    				{
    					if (activeChar != null && !activeChar.isDead())
    					{
    						actor.addDamageHate(activeChar, 0, 2);
    						if (searchTarget)
    						{
    							activeList.add(activeChar);
    						}
    					}
    				}
				}
				
				if (!activeList.isEmpty())
				{
					final var attacked = activeList.get(Rnd.get(activeList.size()));
					if (attacked != null)
					{
						actor.getAI().setIntention(CtrlIntention.ATTACK, attacked);
					}
				}
				activeList.clear();
			}
		}
	}
}
