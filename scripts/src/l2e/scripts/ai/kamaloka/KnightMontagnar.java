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
package l2e.scripts.ai.kamaloka;

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.NpcUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 10.12.2018
 */
public class KnightMontagnar extends Fighter
{
	private long _spawnTimer = 0L;
	private int _spawnCounter = 0;
	private long _orderTimer = 0L;
	private final static long _spawnInterval = 60000L;
	private final static int _spawnLimit = 6;
	private final static long _orderInterval = 24000L;

	public KnightMontagnar(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void thinkAttack()
	{
		final Attackable actor = getActiveChar();
		
		if (_spawnTimer == 0)
		{
			_spawnTimer = System.currentTimeMillis();
		}

		if ((_spawnCounter < _spawnLimit) && ((_spawnTimer + _spawnInterval) < System.currentTimeMillis()))
		{
			final MonsterInstance follower = NpcUtils.spawnSingle(18569, Location.findPointToStay(actor.getLocation(), 200, true), actor.getReflection(), 0);
			final Creature attacker = getAttackTarget();
			if (attacker != null)
			{
				follower.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 1000000);
			}
			else
			{
				for (final Player p : World.getInstance().getAroundPlayers(follower, 1500, 200))
				{
					if (p != null && !p.isDead() && p.getReflectionId() == follower.getReflectionId())
					{
						follower.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 1000000);
						break;
					}
				}
			}
			_spawnTimer = System.currentTimeMillis();
			_spawnCounter++;
		}
		if ((_spawnCounter > 0) && ((_orderTimer + _orderInterval) < System.currentTimeMillis()))
		{
			final List<Player> aggressionList = new ArrayList<>();
			for (final Player p : World.getInstance().getAroundPlayers(actor, 1500, 200))
			{
				if (p != null && !p.isDead() && p.getReflectionId() == actor.getReflectionId())
				{
					aggressionList.add(p.getActingPlayer());
				}
			}

			if (!aggressionList.isEmpty())
			{
				final Player aggressionTarget = aggressionList.get(Rnd.get(aggressionList.size()));
				if (aggressionTarget != null)
				{
					final NpcSay packet = new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.YOU_S1_ATTACK_THEM);
					packet.addStringParameter(aggressionTarget.getName(null).toString());
					actor.broadcastPacketToOthers(2000, packet);
					_orderTimer = System.currentTimeMillis();
					for (final Npc minion : World.getInstance().getAroundNpc(actor))
					{
						if (minion.getId() == 18569)
						{
							((Attackable) minion).clearAggroList(false);
							minion.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, aggressionTarget, 1000000);
						}
					}
				}
			}
		}
		super.thinkAttack();
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable actor = getActiveChar();
		
		final Reflection inst = ReflectionManager.getInstance().getReflection(actor.getReflectionId());
		if (inst != null)
		{
			final ReflectionWorld instance = ReflectionManager.getInstance().getWorld(actor.getReflectionId());
			if ((instance != null) && (instance.getAllowed() != null))
			{
				for (final Npc n : inst.getNpcs())
				{
					if (n != null && n.getReflectionId() == instance.getReflectionId() && n.getId() == 18569)
					{
						if (!n.isDead())
						{
							n.deleteMe();
						}
					}
				}
			}
		}
	}
}