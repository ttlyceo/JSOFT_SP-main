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
package l2e.gameserver.model.entity.events.model.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.entity.events.model.template.FightEventPlayer;
import l2e.gameserver.model.strings.server.ServerMessage;

/**
 * Created by LordWinter
 */
public class FFATreasureHuntEvent extends AbstractFightEvent
{
	private static final int CHEST_ID = 53000;
	private final int[][] _rewardByOpenChest;
	private final int _scoreForKilledPlayer;
	private final int _scoreForChest;
	private final long _timeForRespawningChest;
	private final int _numberOfChests;
	private final Collection<Npc> _spawnedChests;
	
	public FFATreasureHuntEvent(MultiValueSet<String> set)
	{
		super(set);
		
		_rewardByOpenChest = parseItemsList(set.getString("rewardByOpenChest", null));
		_scoreForKilledPlayer = set.getInteger("scoreForKilledPlayer");
		_scoreForChest = set.getInteger("scoreForChest");
		_timeForRespawningChest = set.getLong("timeForRespawningChest");
		_numberOfChests = set.getInteger("numberOfChests");
		_spawnedChests = new CopyOnWriteArrayList<>();
	}
	
	@Override
	public void onKilled(Creature actor, Creature victim)
	{
		if (actor != null && actor.isPlayer())
		{
			final FightEventPlayer realActor = getFightEventPlayer(actor.getActingPlayer());
			if (realActor != null)
			{
				if (victim.isPlayer())
				{
					realActor.increaseScore(_scoreForKilledPlayer);
					increaseKills(realActor);
					final ServerMessage msg = new ServerMessage("FightEvents.YOU_HAVE_KILL", realActor.getPlayer().getLang());
					msg.add(victim.getName(null));
					sendMessageToPlayer(realActor.getPlayer(), MESSAGE_TYPES.GM, msg);
				}
			}
		}
		
		if (victim.isPlayer())
		{
			final FightEventPlayer realVictim = getFightEventPlayer(victim);
			if (realVictim != null)
			{
				realVictim.increaseDeaths();
				if (actor != null)
				{
					final ServerMessage msg = new ServerMessage("FightEvents.YOU_KILLED", realVictim.getPlayer().getLang());
					msg.add(actor.getName(null));
					sendMessageToPlayer(realVictim.getPlayer(), MESSAGE_TYPES.GM, msg);
				}
				victim.getActingPlayer().broadcastCharInfo();
			}
		}
		
		super.onKilled(actor, victim);
	}
	
	private void spawnChest()
	{
		_spawnedChests.add(chooseLocAndSpawnNpc(CHEST_ID, getMap().getKeyLocations(), 0, true));
	}
	
	@Override
	public void startRound()
	{
		super.startRound();
		
		for (int i = 0; i < _numberOfChests; i++)
		{
			spawnChest();
		}
	}
	
	@Override
	public void stopEvent()
	{
		super.stopEvent();
		
		for (final Npc chest : _spawnedChests)
		{
			if (chest != null && !chest.isDead())
			{
				chest.deleteMe();
			}
		}
		_spawnedChests.clear();
	}
	
	public boolean openTreasure(Player player, Npc npc)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);
		if (fPlayer == null)
		{
			return false;
		}
		if (getState() != EVENT_STATE.STARTED)
		{
			return false;
		}
		
		fPlayer.increaseEventSpecificScore("chest");
		fPlayer.increaseScore(_scoreForChest);
		updatePlayerScore(fPlayer);
		player.broadcastCharInfo();
		
		ThreadPoolManager.getInstance().schedule(new SpawnChest(this), _timeForRespawningChest * 1000L);
		
		_spawnedChests.remove(npc);
		
		return true;
	}

	private static class SpawnChest implements Runnable
	{
		private final FFATreasureHuntEvent event;
		private SpawnChest(FFATreasureHuntEvent event)
		{
			this.event = event;
		}

		@Override
		public void run()
		{
			if (event.getState() != EVENT_STATE.NOT_ACTIVE)
			{
				event.spawnChest();
			}
		}
	}
	
	@Override
	protected void giveItemRewardsForPlayer(FightEventPlayer fPlayer, Map<Integer, Long> rewards, boolean isTopKiller, boolean isLogOut)
	{
		if (fPlayer == null)
		{
			return;
		}
		
		if (rewards == null)
		{
			rewards = new HashMap<>();
		}
		
		if (_rewardByOpenChest != null && _rewardByOpenChest.length > 0)
		{
			for (final int[] item : _rewardByOpenChest)
			{
				if ((item == null) || (item.length != 2))
				{
					continue;
				}
				
				if (rewards.containsKey(item[0]))
				{
					final long amount = rewards.get(item[0]) + (item[1] * fPlayer.getEventSpecificScore("chest"));
					rewards.put(item[0], amount);
				}
				else
				{
					rewards.put(item[0], (long) (item[1] * fPlayer.getEventSpecificScore("chest")));
				}
			}
		}
		super.giveItemRewardsForPlayer(fPlayer, rewards, isTopKiller, isLogOut);
	}
	
	@Override
	public String getVisibleTitle(Player player, Player viewer, String currentTitle, boolean toMe)
	{
		final FightEventPlayer fPlayer = getFightEventPlayer(player);
		if (fPlayer == null)
		{
			return currentTitle;
		}
		final ServerMessage msg = new ServerMessage("FightEvents.TITLE_INFO1", viewer.getLang());
		msg.add(fPlayer.getEventSpecificScore("chest"));
		msg.add(fPlayer.getKills());
		return msg.toString();
	}
}