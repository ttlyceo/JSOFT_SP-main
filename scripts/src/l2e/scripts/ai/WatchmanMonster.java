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


import l2e.commons.util.Rnd;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 05.07.2021
 */
public class WatchmanMonster extends Fighter
{
	private long _lastSearch = 0;
	private boolean _isSearching = false;
	private Creature _attacker = null;

	public WatchmanMonster(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(final Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead())
		{
			return;
		}
		
		if(attacker != null && !actor.getFaction().isNone() && actor.getCurrentHpPercents() < 50 && _lastSearch < System.currentTimeMillis() - 15000)
		{
			_lastSearch = System.currentTimeMillis();
			_attacker = attacker;
			actor.broadcastPacketToOthers(1000, new NpcSay(actor, Say2.NPC_ALL, NpcStringId.getNpcStringId(Rnd.get(1000007, 1000027))));
			if(findHelp())
			{
				return;
			}
		}
		super.onEvtAttacked(attacker, damage);
	}

	private boolean findHelp()
	{
		_isSearching = false;
		final Attackable actor = getActiveChar();
		final Creature attacker = _attacker;
		if (actor.isDead() || attacker == null)
		{
			return false;
		}

		for (final Npc npc : World.getInstance().getAroundNpc(actor, 1000, 150))
		{
			if (actor != null && !actor.isDead() && npc.isInFaction(actor) && !npc.isInCombat())
			{
				_isSearching = true;
				npc.setIsRunning(true);
				npc.getAI().setIntention(CtrlIntention.MOVING, actor.getLocation(), 0);
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		_lastSearch = 0;
		_attacker = null;
		_isSearching = false;
		super.onEvtDead(killer);
	}

	@Override
	protected void onEvtArrived()
	{
		final Attackable actor = getActiveChar();
		if (actor.isDead())
		{
			return;
		}
		
		if (_isSearching)
		{
			final Creature attacker = _attacker;
			if(attacker != null)
			{
				notifyFriends(attacker, 100);
			}
			_isSearching = false;
			actor.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
		}
		else
		{
			super.onEvtArrived();
		}
	}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		if (!_isSearching)
		{
			super.onEvtAggression(target, aggro);
		}
	}
}