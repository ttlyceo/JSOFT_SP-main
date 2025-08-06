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

import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.taskmanager.ItemsAutoDestroy;

/**
 * Created by LordWinter 19.09.2018
 */
public class FrightenedOrc extends Fighter
{
	protected ScheduledFuture<?> _rewardTask;
	protected ScheduledFuture<?> _despawnTask;

	private static final NpcStringId[] ATTACK_MSG =
	{
	                NpcStringId.I_DONT_WANT_TO_FIGHT, NpcStringId.IS_THIS_REALLY_NECESSARY
	};

	private static final NpcStringId[] REWARD_MSG =
	{
	                NpcStringId.TH_THANKS_I_COULD_HAVE_BECOME_GOOD_FRIENDS_WITH_YOU, NpcStringId.ILL_GIVE_YOU_10000000_ADENA_LIKE_I_PROMISED_I_MIGHT_BE_AN_ORC_WHO_KEEPS_MY_PROMISES
	};

	private static final NpcStringId[] REWARD_MSG1 =
	{
	                NpcStringId.TH_THANKS_I_COULD_HAVE_BECOME_GOOD_FRIENDS_WITH_YOU, NpcStringId.SORRY_BUT_THIS_IS_ALL_I_HAVE_GIVE_ME_A_BREAK
	};

	private static final NpcStringId[] REWARD_MSG2 =
	{
	                NpcStringId.THANKS_BUT_THAT_THING_ABOUT_10000000_ADENA_WAS_A_LIE_SEE_YA, NpcStringId.YOURE_PRETTY_DUMB_TO_BELIEVE_ME
	};

	public FrightenedOrc(Attackable actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();

		if ((attacker != null) && Rnd.chance(10) && actor.isScriptValue(0))
		{
			actor.setScriptValue(1);
			actor.broadcastPacketToOthers(1000, new NpcSay(actor, Say2.NPC_ALL, ATTACK_MSG[Rnd.get(2)]));
		}
		else if ((actor.getCurrentHp() < (actor.getMaxHp() * 0.2)) && actor.isScriptValue(1))
		{
			_rewardTask = ThreadPoolManager.getInstance().schedule(new checkReward(attacker, actor), 10000L);
			actor.broadcastPacketToOthers(1000, new NpcSay(actor, Say2.NPC_ALL, NpcStringId.WAIT_WAIT_STOP_SAVE_ME_AND_ILL_GIVE_YOU_10000000_ADENA));
			actor.setScriptValue(2);
		}

		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		if (_rewardTask != null)
		{
			_rewardTask.cancel(false);
		}
		if (_despawnTask != null)
		{
			_despawnTask.cancel(false);
		}
		super.onEvtDead(killer);
	}

	protected class checkReward implements Runnable
	{
		Creature _attacker;
		Attackable _npc;

		public checkReward(Creature attacker, Attackable npc)
		{
			_attacker = attacker;
			_npc = npc;
		}

		@Override
		public void run()
		{
			if (!_npc.isDead() && _npc.isScriptValue(2))
			{
				if (Rnd.get(100000) < 10)
				{
					_npc.broadcastPacketToOthers(1000, new NpcSay(_npc, Say2.NPC_ALL, REWARD_MSG[Rnd.get(2)]));
					_npc.setScriptValue(3);
					_npc.doCast(new SkillHolder(6234, 1).getSkill());
					for (int i = 0; i < 10; i++)
					{
						_npc.dropItem(_attacker.getActingPlayer(), PcInventory.ADENA_ID, 1000000);
					}
					ItemsAutoDestroy.getInstance().tryRecalcTime();
				}
				else if (Rnd.get(100000) < 1000)
				{
					_npc.broadcastPacketToOthers(1000, new NpcSay(_npc, Say2.NPC_ALL, REWARD_MSG1[Rnd.get(2)]));
					_npc.setScriptValue(3);
					_npc.doCast(new SkillHolder(6234, 1).getSkill());
					for (int i = 0; i < 10; i++)
					{
						_npc.dropItem(_attacker.getActingPlayer(), PcInventory.ADENA_ID, 10000);
					}
					ItemsAutoDestroy.getInstance().tryRecalcTime();
				}
				else
				{
					_npc.broadcastPacketToOthers(1000, new NpcSay(_npc, Say2.NPC_ALL, REWARD_MSG2[Rnd.get(2)]));
				}
				_despawnTask = ThreadPoolManager.getInstance().schedule(new checkDespawn(_npc), 1000L);
			}
		}
	}

	protected class checkDespawn implements Runnable
	{
		Attackable _npc;

		public checkDespawn(Attackable npc)
		{
			_npc = npc;
		}

		@Override
		public void run()
		{
			if (!_npc.isDead())
			{
				_npc.setRunning();
				_npc.getAI().setIntention(CtrlIntention.MOVING, new Location((_npc.getX() + Rnd.get(-800, 800)), (_npc.getY() + Rnd.get(-800, 800)), _npc.getZ(), _npc.getHeading()), 0);
				_npc.deleteMe();
			}
		}
	}
}
