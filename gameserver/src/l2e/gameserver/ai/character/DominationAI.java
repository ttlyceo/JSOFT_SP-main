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
package l2e.gameserver.ai.character;

import static l2e.gameserver.ai.model.CtrlIntention.ACTIVE;
import static l2e.gameserver.ai.model.CtrlIntention.IDLE;

import java.util.concurrent.Future;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.instance.DominationInstance;
import l2e.gameserver.model.entity.events.AbstractFightEvent.EVENT_STATE;
import l2e.gameserver.model.entity.events.model.template.FightEventTeam;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.taskmanager.AiTaskManager;

public class DominationAI extends CharacterAI implements Runnable
{
	private Future<?> _aiTask;
	private boolean _thinking = false;
	private int _tick = 0;
	
	public DominationAI(DominationInstance character)
	{
		super(character);
	}
	
	@Override
	protected synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if (intention == IDLE || intention == ACTIVE)
		{
			super.changeIntention(intention, arg0, arg1);
			startAITask();
		}
	}
	
	@Override
	public void run()
	{
		onEvtThink();
	}
	
	@Override
	public synchronized void startAITask()
	{
		if (_aiTask == null)
		{
			_aiTask = Config.AI_TASK_MANAGER_COUNT > 0 ? AiTaskManager.getInstance().scheduleAtFixedRate(this, 0L, Config.NPC_AI_TIME_TASK) : ThreadPoolManager.getInstance().scheduleAtFixedRate(this, 1000, 1000);
		}
	}
	
	@Override
	public synchronized void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		super.stopAITask();
	}
	
	@Override
	protected void onEvtThink()
	{
		final var actor = getActiveChar();
		if (_thinking || actor == null)
		{
			return;
		}
		
		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case ACTIVE :
				case IDLE :
					thinkActive();
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	public void enableAI()
	{
		if (getActiveChar().getAI().getIntention() == CtrlIntention.IDLE)
		{
			changeIntention(CtrlIntention.ACTIVE, null, null);
		}
	}
	
	private DominationInstance getActiveChar()
	{
		return (DominationInstance) _actor;
	}
	
	protected boolean thinkActive()
	{
		final var actor = getActiveChar();
		final var event = actor.getEvent();
		if (event == null || event.getState() != EVENT_STATE.STARTED)
		{
			return true;
		}
		
		final var team = actor.getTeam();
		var controversial = false;
		int charIndex = 0;
		FightEventTeam fTeam = null;
		
		for (final var cha : World.getInstance().getAroundCharacters(actor, 300, 300))
		{
			if (cha == null || cha.isAlikeDead())
			{
				continue;
			}
			
			if (cha.isPlayer())
			{
				final var realActor = event.getFightEventPlayer(cha.getActingPlayer());
				if (realActor != null)
				{
					final var index = realActor.getTeam().getIndex();
					if (charIndex == 0)
					{
						charIndex = index;
						fTeam = realActor.getTeam();
					}
					else
					{
						if (charIndex != index)
						{
							fTeam = null;
							controversial = true;
							_tick = 0;
							break;
						}
					}
				}
			}
		}
		
		if (!controversial)
		{
			final var eventTeam = actor.getEventTeam();
			if ((charIndex == team && charIndex > 0) || (eventTeam != null && eventTeam.getIndex() == team && charIndex == 0))
			{
				_tick++;
				if (_tick >= 5)
				{
					event.setTeamPoint(eventTeam);
					_tick = 0;
				}
			}
			else
			{
				if (charIndex > 0)
				{
					_tick++;
				}
				
				if (_tick >= 20)
				{
					actor.setTeam(charIndex);
					actor.setEventTeam(fTeam);
					actor.broadcastInfo();
					_tick = 0;
				}
			}
		}
		return true;
	}

	@Override
	protected void onIntentionRest()
	{
	}

	@Override
	protected void onIntentionAttack(Creature target, boolean shift)
	{
	}

	@Override
	protected void onIntentionCast(Skill skill, GameObject target)
	{
	}

	@Override
	protected void onIntentionMoveTo(Location destination, int offset)
	{
	}

	@Override
	protected void onIntentionFollow(Creature target)
	{
	}

	@Override
	protected void onIntentionPickUp(GameObject item)
	{
	}

	@Override
	protected void onIntentionInteract(GameObject object)
	{
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
	}

	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
	}

	@Override
	protected void onEvtStunned(Creature attacker)
	{
	}

	@Override
	protected void onEvtSleeping(Creature attacker)
	{
	}

	@Override
	protected void onEvtRooted(Creature attacker)
	{
	}

	@Override
	protected void onEvtReadyToAct()
	{
	}

	@Override
	protected void onEvtUserCmd(Object arg0, Object arg1)
	{
	}

	@Override
	protected void onEvtArrived()
	{
	}

	@Override
	protected void onEvtArrivedTarget()
	{
	}

	@Override
	protected void onEvtArrivedBlocked(Location blocked_at_loc)
	{
	}

	@Override
	protected void onEvtForgetObject(GameObject object)
	{
	}

	@Override
	protected void onEvtCancel()
	{
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
	}
}