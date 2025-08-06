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
package l2e.gameserver.model.entity.events.cleft;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.entity.Duel;

/**
 * Created by LordWinter 03.12.2018
 */
public class AerialCleftTeleporter implements Runnable
{
	private Player _player = null;
	private Location[] _coordinates = new Location[3];
	private boolean _exitEvent = false;

	public AerialCleftTeleporter(Player player, Location[] coordinates, boolean fastSchedule, boolean exitEvent)
	{
		_player = player;
		_coordinates = coordinates;
		_exitEvent = exitEvent;
		
		final long delay = (AerialCleftEvent.getInstance().isStarted() ? Config.CLEFT_RESPAWN_DELAY : Config.CLEFT_LEAVE_DELAY) * 1000;
		
		ThreadPoolManager.getInstance().schedule(this, fastSchedule ? 0 : delay);
	}

	@Override
	public void run()
	{
		if (_player == null)
		{
			return;
		}
		
		final Summon summon = _player.getSummon();
		
		if (summon != null)
		{
			summon.unSummon(_player);
		}
		
		if ((_player.getTeam() == 0 || (_player.isInDuel() && _player.getDuelState() != Duel.DUELSTATE_INTERRUPTED)))
		{
			_player.stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		
		if (_player.isInDuel())
		{
			_player.setDuelState(Duel.DUELSTATE_INTERRUPTED);
		}
		
		_player.doRevive();
		
		final Location rndLoc = _coordinates[Rnd.get(3)];
		_player.teleToLocation(rndLoc.getX() + Rnd.get(101) - 50, rndLoc.getY() + Rnd.get(101) - 50, rndLoc.getZ(), false, ReflectionManager.DEFAULT);
		
		if ((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && !_exitEvent)
		{
			_player.setTeam(AerialCleftEvent.getInstance().getParticipantTeamId(_player.getObjectId()) + 1);
			_player.setCanRevive(false);
		}
		else
		{
			_player.setTeam(0);
			_player.cleanCleftStats();
			_player.cleanBlockSkills(false);
			_player.sendSkillList(false);
			_player.setCanRevive(true);
		}
		
		_player.setCurrentCp(_player.getMaxCp());
		_player.setCurrentHp(_player.getMaxHp());
		_player.setCurrentMp(_player.getMaxMp());
		_player.broadcastStatusUpdate();
		_player.broadcastUserInfo(true);
	}
}