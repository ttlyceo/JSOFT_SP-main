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
import l2e.gameserver.Config;
import l2e.gameserver.ai.DefaultAI;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.LakfiManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.MagicSkillUse;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Created by LordWinter 22.04.2020
 */
public class LafiLakfi extends DefaultAI
{
	private static final int MAX_RADIUS = 500;
	private static final Skill s_display_bug_of_fortune1 = SkillsParser.getInstance().getInfo(6045, 1);
	private static final Skill s_display_jackpot_firework = SkillsParser.getInstance().getInfo(5778, 1);

	private long _nextEat;
	private int i_ai2, actor_lvl, prev_st;
	private boolean _firstSaid;
	
	public LafiLakfi(Attackable actor)
	{
		super(actor);
		
		_nextEat = 0L;
	}

	@Override
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();
		
		final Attackable actor = getActiveChar();
		
		addTimer(7778, 1000);
		
		if(getFirstSpawned(actor))
		{
			i_ai2 = 0;
			prev_st = 0;
		}
		else
		{
			i_ai2 = 3;
			prev_st = 3;
		}
		_firstSaid = false;

		actor_lvl = actor.getLevel();
	}

	@Override
	protected void onEvtArrived()
	{
		super.onEvtArrived();
		
		final Attackable actor = getActiveChar();
		if(actor == null)
		{
			return;
		}
			
		if(i_ai2 > 9)
		{
			if(!_firstSaid)
			{
				actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.IM_FULL_NOW_I_DONT_WANT_TO_EAT_ANYMORE));
				_firstSaid = true;
			}
			return;
		}
		
		ItemInstance closestItem = null;
		if(_nextEat < System.currentTimeMillis())
		{
			for (final GameObject object : World.getInstance().getAroundObjects(actor, 20, 200))
			{
				if (((object instanceof ItemInstance)) && (((ItemInstance) object).getId() == 57))
				{
					closestItem = (ItemInstance) object;
				}
			}

			if(closestItem != null && closestItem.getCount() >= Config.MIN_ADENA_TO_EAT)
			{
				closestItem.decayMe();
				actor.setTarget(actor);
				actor.doCast(s_display_bug_of_fortune1);
				actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.YUM_YUM_YUM_YUM));
				_firstSaid = false;
				
				if(i_ai2 == 2 && getFirstSpawned(actor))
				{
					final MonsterInstance npc = new MonsterInstance(IdFactory.getInstance().getNextId(), NpcsParser.getInstance().getTemplate(getCurrActor()));
					final Location loc = actor.getLocation();
					npc.getTemplate().setLevel((byte) actor.getLevel());
					npc.setReflection(actor.getReflection());
					npc.spawnMe(loc.getX(), loc.getY(), loc.getZ());
					npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
					actor.doDie(actor);
					actor.deleteMe();
				}
					
				i_ai2++;
				_nextEat = System.currentTimeMillis() + Config.INTERVAL_EATING * 1000;
			}
		}
	}

	private boolean getFirstSpawned(Attackable actor)
	{
		if (actor.getId() == 2503 || actor.getId() == 2502)
		{
			return false;
		}
		return true;
	}
	
	private int getCurrActor()
	{
		if(Rnd.chance(20))
		{
			return 2503;
		}
		return 2502;
	}
	
	@Override
	protected boolean thinkActive()
	{
		final Attackable actor = getActiveChar();
		if(actor == null || actor.isDead())
		{
			return true;
		}
		
		if (!actor.isMoving() && _nextEat < System.currentTimeMillis())
		{
			ItemInstance closestItem = null;
			for (final GameObject object : World.getInstance().getAroundObjects(actor, MAX_RADIUS, 200))
			{
				if (((object instanceof ItemInstance)) && (((ItemInstance) object).getId() == 57))
				{
					closestItem = (ItemInstance) object;
				}
			}

			if(closestItem != null)
			{
				actor.setWalking();
				moveTo(closestItem.getLocation());
			}
		}
		return false;
	}
	
	public int getChance(int stage)
	{
		switch(stage)
		{
			case 4:
				return 10;
			case 5:
				return 20;
			case 6:
				return 40;
			case 7:
				return 60;
			case 8:
				return 70;
			case 9:
				return 80;
			case 10:
				return 100;
			default:
				return 0;
		}
	}
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		super.onEvtDead(killer);
		
		final Attackable actor = getActiveChar();
		if(actor == null)
		{
			return;
		}

		if (killer != null && killer.isPlayer())
		{
			if(i_ai2 >= 0 && i_ai2 < 3)
			{
				actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.I_HAVENT_EATEN_ANYTHING_IM_SO_WEAK));
				return;
			}
			else
			{
				actor.broadcastPacketToOthers(1500, new MagicSkillUse(actor, s_display_jackpot_firework.getId(), 1, s_display_jackpot_firework.getHitTime(), 0));
			}
				
			if (Rnd.chance(getChance(i_ai2)))
			{
				LakfiManager.getInstance().getLakfiRewards(actor_lvl, actor, killer.getActingPlayer(), Rnd.get(0, 100));
			}
		}
	}

	@Override
	protected void onEvtTimer(int timerId, Object arg1)
	{
		final Attackable actor = getActiveChar();
		if(actor == null)
		{
			return;
		}

		if(timerId == 7778)
		{
			switch(i_ai2)
			{
				case 0:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.IF_YOU_HAVE_ITEMS_PLEASE_GIVE_THEM_TO_ME));
					break;
				case 1:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.MY_STOMACH_IS_EMPTY));
					break;
				case 2:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.IM_HUNGRY_IM_HUNGRY));
					break;
				case 3:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.I_FEEL_A_LITTLE_WOOZY));
					break;
				case 4:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.IM_STILL_NOT_FULL));
					break;
				case 5:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.IM_STILL_HUNGRY));
					break;
				case 6:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.NOW_ITS_TIME_TO_EAT));
					break;
				case 7:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.GIVE_ME_SOMETHING_TO_EAT));
					break;
				case 8:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.IM_STILL_HUNGRY_));
					break;
				case 9:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.I_ALSO_NEED_A_DESSERT));
					break;
				case 10:
					actor.broadcastPacketToOthers(1000, new NpcSay(actor.getObjectId(), Say2.ALL, actor.getId(), NpcStringId.IM_FULL_NOW_I_DONT_WANT_TO_EAT_ANYMORE));
					break;
			}
			addTimer(7778, 10000 + Rnd.get(10) * 1000);
		}
		
		if(timerId == 1500)
		{
			if(prev_st == i_ai2 && prev_st != 0 && i_ai2 != 10)
			{
				actor.doDie(actor);
			}
			else
			{
				prev_st = i_ai2;
				addTimer(1500, Config.TIME_IF_NOT_FEED * 60000);
			}
		}
		else
		{
			super.onEvtTimer(timerId, arg1);
		}
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{}
}