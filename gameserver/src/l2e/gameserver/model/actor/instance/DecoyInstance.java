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
package l2e.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Decoy;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.taskmanager.DecayTaskManager;

public class DecoyInstance extends Decoy
{
	private int _totalLifeTime;
	private int _timeRemaining;
	private Future<?> _DecoyLifeTask;
	private Future<?> _HateSpam;
	
	public DecoyInstance(int objectId, NpcTemplate template, Player owner, int totalLifeTime)
	{
		super(objectId, template, owner);
		
		setInstanceType(InstanceType.DecoyInstance);
		_totalLifeTime = totalLifeTime;
		_timeRemaining = _totalLifeTime;
		final int skilllevel = getTemplate().getIdTemplate() - 13070;
		_DecoyLifeTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new DecoyLifetime(getOwner(), this), 1000, 1000);
		_HateSpam = ThreadPoolManager.getInstance().scheduleAtFixedRate(new HateSpam(this, SkillsParser.getInstance().getInfo(5272, skilllevel)), 2000, 5000);
	}

	@Override
	protected void onDeath(Creature killer)
	{
		if (_HateSpam != null)
		{
			_HateSpam.cancel(true);
			_HateSpam = null;
		}
		_totalLifeTime = 0;
		DecayTaskManager.getInstance().addDecayTask(this, (Config.NPC_DECAY_TIME * 1000L), true);
		super.onDeath(killer);
	}
	
	static class DecoyLifetime implements Runnable
	{
		private final Player _activeChar;
		
		private final DecoyInstance _Decoy;
		
		DecoyLifetime(Player activeChar, DecoyInstance Decoy)
		{
			_activeChar = activeChar;
			_Decoy = Decoy;
		}
		
		@Override
		public void run()
		{
			try
			{
				_Decoy.decTimeRemaining(1000);
				final double newTimeRemaining = _Decoy.getTimeRemaining();
				if (newTimeRemaining < 0)
				{
					_Decoy.unSummon(_activeChar);
				}
			}
			catch (final Exception e)
			{
				_log.warn("Decoy Error: ", e);
			}
		}
	}
	
	private static class HateSpam implements Runnable
	{
		private final DecoyInstance _activeChar;
		
		private final Skill _skill;
		
		HateSpam(DecoyInstance activeChar, Skill Hate)
		{
			_activeChar = activeChar;
			_skill = Hate;
		}
		
		@Override
		public void run()
		{
			try
			{
				_activeChar.setTarget(_activeChar);
				_activeChar.doCast(_skill);
			}
			catch (final Throwable e)
			{
				_log.warn("Decoy Error: ", e);
			}
		}
	}
	
	@Override
	public void unSummon(Player owner)
	{
		if (_DecoyLifeTask != null)
		{
			_DecoyLifeTask.cancel(true);
			_DecoyLifeTask = null;
		}
		if (_HateSpam != null)
		{
			_HateSpam.cancel(true);
			_HateSpam = null;
		}
		super.unSummon(owner);
	}
	
	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}
	
	public int getTimeRemaining()
	{
		return _timeRemaining;
	}
	
	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}
	
	@Override
	public double getColRadius()
	{
		final Player player = getActingPlayer();
		if (player == null)
		{
			return 0;
		}
		
		if (player.isTransformed())
		{
			return player.getTransformation().getCollisionRadius(player);
		}
		return player.getAppearance().getSex() ? player.getBaseTemplate().getFCollisionRadiusFemale() : player.getBaseTemplate().getfCollisionRadius();
	}
	
	@Override
	public double getColHeight()
	{
		final Player player = getActingPlayer();
		if (player == null)
		{
			return 0;
		}
		
		if (player.isTransformed())
		{
			return player.getTransformation().getCollisionHeight(player);
		}
		return player.getAppearance().getSex() ? player.getBaseTemplate().getFCollisionHeightFemale() : player.getBaseTemplate().getfCollisionHeight();
	}
}