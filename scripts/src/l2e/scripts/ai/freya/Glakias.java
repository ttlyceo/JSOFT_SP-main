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
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

public class Glakias extends Fighter
{
	private final ZoneType _zone = ZoneManager.getInstance().getZoneById(90578);
	
	public Glakias(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		
		if (attacker != null && actor.getCurrentHp() < (actor.getMaxHp() * 0.2))
		{
			if (actor.isScriptValue(0))
			{
				NpcStringId stringId = null;
				switch (Rnd.get(4))
				{
					case 0 :
					{
						stringId = NpcStringId.ARCHER_GIVE_YOUR_BREATH_FOR_THE_INTRUDER;
						break;
					}
					case 1 :
					{
						stringId = NpcStringId.MY_KNIGHTS_SHOW_YOUR_LOYALTY;
						break;
					}
					case 2 :
					{
						stringId = NpcStringId.I_CAN_TAKE_IT_NO_LONGER;
						break;
					}
					case 3 :
					{
						stringId = NpcStringId.ARCHER_HEED_MY_CALL;
						break;
					}
				}
				actor.broadcastPacketToOthers(2000, new NpcSay(actor.getObjectId(), Say2.SHOUT, actor.getId(), stringId));
				actor.setScriptValue(1);
			}
		}
		super.onEvtAttacked(attacker, damage);
	}
	
	@Override
	protected boolean thinkActive()
	{
		if (Rnd.chance(10))
		{
			aggroPlayers(true);
		}
		return super.thinkActive();
	}
	
	@Override
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();
		aggroPlayers(true);
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
						actor.addDamageHate(activeChar, 0, Rnd.get(100, 300));
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
							actor.addDamageHate(activeChar, 0, Rnd.get(100, 300));
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
