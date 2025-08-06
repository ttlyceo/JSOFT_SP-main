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

import l2e.commons.util.PositionUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.quest.Quest;

/**
 * Created by LordWinter 16.09.2018
 */
public class Kanabion extends Fighter
{
	public Kanabion(Attackable actor)
	{
		super(actor);
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable actor = getActiveChar();
		boolean isOverhit = false;
		if (actor instanceof MonsterInstance)
		{
			isOverhit = ((MonsterInstance) actor).getOverhitDamage() > 0;
		}
		final int npcId = actor.getId();
		int nextId = 0;
		if ((npcId != getNextDoppler(npcId)) && (npcId != getNextVoid(npcId)))
		{
			if (isOverhit)
			{
				if (Rnd.chance(70))
				{
					nextId = getNextDoppler(npcId);
				}
				else if (Rnd.chance(80))
				{
					nextId = getNextVoid(npcId);
				}
			}
			else if (Rnd.chance(65))
			{
				nextId = getNextDoppler(npcId);
			}
		}
		else if (npcId == getNextDoppler(npcId))
		{
			if (isOverhit)
			{
				if (Rnd.chance(60))
				{
					nextId = getNextDoppler(npcId);
				}
				else if (Rnd.chance(90))
				{
					nextId = getNextVoid(npcId);
				}
			}
			else if (Rnd.chance(40))
			{
				nextId = getNextDoppler(npcId);
			}
			else if (Rnd.chance(50))
			{
				nextId = getNextVoid(npcId);
			}
		}
		else if (npcId == getNextVoid(npcId))
		{
			if (isOverhit)
			{
				if (Rnd.chance(80))
				{
					nextId = getNextVoid(npcId);
				}
			}
			else if (Rnd.chance(50))
			{
				nextId = getNextVoid(npcId);
			}
		}
		
		if (nextId > 0)
		{
			Creature player = null;
			if (!killer.isPlayer())
			{
				final ReflectionWorld instance = ReflectionManager.getInstance().getWorld(actor.getReflectionId());
				if ((instance != null) && (instance.getAllowed() != null))
				{
					for (final int objectId : instance.getAllowed())
					{
						final Player activeChar = GameObjectsStorage.getPlayer(objectId);
						if (activeChar != null)
						{
							player = activeChar;
						}
						break;
					}
				}
			}
			if (player == null)
			{
				player = killer;
			}
			ThreadPoolManager.getInstance().schedule(new SpawnNext(actor, player, nextId), 5000);
		}
		super.onEvtDead(killer);
	}
	
	public static class SpawnNext implements Runnable
	{
		private final Attackable _actor;
		private final Creature _player;
		private final int _nextId;
		
		public SpawnNext(Attackable actor, Creature player, int nextId)
		{
			_actor = actor;
			_player = player;
			_nextId = nextId;
		}
		
		@Override
		public void run()
		{
			final Attackable npc = (Attackable) Quest.addSpawn(_nextId, _actor);
			npc.setReflection(_actor.getReflection());
			npc.setHeading(PositionUtils.calculateHeadingFrom(npc, _player));
			npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _player, 1000);
		}
	}
	
	private int getNextDoppler(int npcId)
	{
		switch (npcId)
		{
			case 22452 :
			case 22453 :
			case 22454 :
				return 22453;
			case 22455 :
			case 22456 :
			case 22457 :
				return 22456;
			case 22458 :
			case 22459 :
			case 22460 :
				return 22459;
			case 22461 :
			case 22462 :
			case 22463 :
				return 22462;
			case 22464 :
			case 22465 :
			case 22466 :
				return 22465;
			case 22467 :
			case 22468 :
			case 22469 :
				return 22468;
			case 22470 :
			case 22471 :
			case 22472 :
				return 22471;
			case 22473 :
			case 22474 :
			case 22475 :
				return 22474;
			case 22476 :
			case 22477 :
			case 22478 :
				return 22477;
			case 22479 :
			case 22480 :
			case 22481 :
				return 22480;
			case 22482 :
			case 22483 :
			case 22484 :
				return 22483;
			default :
				return 0;
		}
	}
	
	private int getNextVoid(int npcId)
	{
		switch (npcId)
		{
			case 22452 :
			case 22453 :
			case 22454 :
				return 22454;
			case 22455 :
			case 22456 :
			case 22457 :
				return 22457;
			case 22458 :
			case 22459 :
			case 22460 :
				return 22460;
			case 22461 :
			case 22462 :
			case 22463 :
				return 22463;
			case 22464 :
			case 22465 :
			case 22466 :
				return 22466;
			case 22467 :
			case 22468 :
			case 22469 :
				return 22469;
			case 22470 :
			case 22471 :
			case 22472 :
				return 22472;
			case 22473 :
			case 22474 :
			case 22475 :
				return 22475;
			case 22476 :
			case 22477 :
			case 22478 :
				return 22478;
			case 22479 :
			case 22480 :
			case 22481 :
				return 22481;
			case 22482 :
			case 22483 :
			case 22484 :
				return 22484;
			default :
				return 0;
		}
	}
}
