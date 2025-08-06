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
package l2e.gameserver.model.entity;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.data.parser.SpawnParser;
import l2e.gameserver.instancemanager.HandysBlockCheckerManager;
import l2e.gameserver.model.ArenaParticipantsHolder;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.BlockInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExBasicActionList;
import l2e.gameserver.network.serverpackets.ExBlockUpSetList;
import l2e.gameserver.network.serverpackets.ExBlockUpSetState;
import l2e.gameserver.network.serverpackets.RelationChanged;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class BlockCheckerEngine
{
	protected static final Logger _log = LoggerFactory.getLogger(BlockCheckerEngine.class);
	
	protected ArenaParticipantsHolder _holder;
	protected Map<Player, Integer> _redTeamPoints = new ConcurrentHashMap<>();
	protected Map<Player, Integer> _blueTeamPoints = new ConcurrentHashMap<>();
	protected int _redPoints = 15;
	protected int _bluePoints = 15;
	protected int _arena = -1;
	protected List<Spawner> _spawns = new CopyOnWriteArrayList<>();
	protected boolean _isRedWinner;
	protected long _startedTime;

	protected static final int[][] _arenaCoordinates =
	{
	        {
	                -58368, -62745, -57751, -62131, -58053, -62417
			},
			{
			        -58350, -63853, -57756, -63266, -58053, -63551
			},
			{
			        -57194, -63861, -56580, -63249, -56886, -63551
			},
			{
			        -57200, -62727, -56584, -62115, -56850, -62391
			}
	};

	private static final int _zCoord = -2405;
	protected List<ItemInstance> _drops = new CopyOnWriteArrayList<>();
	private static final byte DEFAULT_ARENA = -1;
	protected boolean _isStarted = false;
	protected ScheduledFuture<?> _task;
	protected boolean _abnormalEnd = false;

	public BlockCheckerEngine(ArenaParticipantsHolder holder, int arena)
	{
		_holder = holder;
		if ((arena > -1) && (arena < 4))
		{
			_arena = arena;
		}

		for (final Player player : holder.getRedPlayers())
		{
			_redTeamPoints.put(player, 0);
		}
		for (final Player player : holder.getBluePlayers())
		{
			_blueTeamPoints.put(player, 0);
		}
	}

	public void updatePlayersOnStart(ArenaParticipantsHolder holder)
	{
		_holder = holder;
	}

	public ArenaParticipantsHolder getHolder()
	{
		return _holder;
	}

	public int getArena()
	{
		return _arena;
	}

	public long getStarterTime()
	{
		return _startedTime;
	}

	public int getRedPoints()
	{
		synchronized (this)
		{
			return _redPoints;
		}
	}

	public int getBluePoints()
	{
		synchronized (this)
		{
			return _bluePoints;
		}
	}

	public int getPlayerPoints(Player player, boolean isRed)
	{
		if (!_redTeamPoints.containsKey(player) && !_blueTeamPoints.containsKey(player))
		{
			return 0;
		}

		if (isRed)
		{
			return _redTeamPoints.get(player);
		}
		return _blueTeamPoints.get(player);
	}

	public synchronized void increasePlayerPoints(Player player, int team)
	{
		if (player == null)
		{
			return;
		}

		if (team == 0)
		{
			final int points = _redTeamPoints.get(player) + 1;
			_redTeamPoints.put(player, points);
			_redPoints++;
			_bluePoints--;
		}
		else
		{
			final int points = _blueTeamPoints.get(player) + 1;
			_blueTeamPoints.put(player, points);
			_bluePoints++;
			_redPoints--;
		}
	}

	public void addNewDrop(ItemInstance item)
	{
		if (item != null)
		{
			_drops.add(item);
		}
	}

	public boolean isStarted()
	{
		return _isStarted;
	}

	protected void broadcastRelationChanged(Player plr)
	{
		for (final Player p : _holder.getAllPlayers())
		{
			p.sendPacket(RelationChanged.update(plr, p, plr));
		}
	}

	public void endEventAbnormally()
	{
		try
		{
			synchronized (this)
			{
				_isStarted = false;

				if (_task != null)
				{
					_task.cancel(true);
				}

				_abnormalEnd = true;

				ThreadPoolManager.getInstance().execute(new EndEvent());

				if (Config.DEBUG)
				{
					_log.info("Handys Block Checker Event at arena " + _arena + " ended due lack of players!");
				}
			}
		}
		catch (final Exception e)
		{
			_log.warn("Couldnt end Block Checker event at " + _arena, e);
		}
	}

	public class StartEvent implements Runnable
	{
		private final Skill _freeze, _transformationRed, _transformationBlue;

		public StartEvent()
		{
			_freeze = SkillsParser.getInstance().getInfo(6034, 1);
			_transformationRed = SkillsParser.getInstance().getInfo(6035, 1);
			_transformationBlue = SkillsParser.getInstance().getInfo(6036, 1);
		}

		private void setUpPlayers()
		{
			HandysBlockCheckerManager.getInstance().setArenaBeingUsed(_arena);

			_redPoints = _spawns.size() / 2;
			_bluePoints = _spawns.size() / 2;
			final ExBlockUpSetState initialPoints = new ExBlockUpSetState(300, _bluePoints, _redPoints);
			ExBlockUpSetState clientSetUp;

			for (final Player player : _holder.getAllPlayers())
			{
				if (player == null)
				{
					continue;
				}

				final boolean isRed = _holder.getRedPlayers().contains(player);

				clientSetUp = new ExBlockUpSetState(300, _bluePoints, _redPoints, isRed, player, 0);
				player.sendPacket(clientSetUp);

				player.sendActionFailed();

				final int tc = _holder.getPlayerTeam(player) * 2;
				final int x = _arenaCoordinates[_arena][tc];
				final int y = _arenaCoordinates[_arena][tc + 1];
				player.teleToLocation(x, y, _zCoord, true, player.getReflection());

				if (isRed)
				{
					_redTeamPoints.put(player, 0);
					player.setTeam(2);
				}
				else
				{
					_blueTeamPoints.put(player, 0);
					player.setTeam(1);
				}
				player.stopAllEffects();

				if (player.hasSummon())
				{
					player.getSummon().unSummon(player);
				}

				_freeze.getEffects(player, player, false);

				if (_holder.getPlayerTeam(player) == 0)
				{
					_transformationRed.getEffects(player, player, false);
				}
				else
				{
					_transformationBlue.getEffects(player, player, false);
				}

				player.setBlockCheckerArena((byte) _arena);
				player.sendPacket(initialPoints);
				player.sendPacket(new ExBlockUpSetList(true));
				player.sendPacket(ExBasicActionList.STATIC_PACKET);
				broadcastRelationChanged(player);
			}
		}

		@Override
		public void run()
		{
			if (_arena == -1)
			{
				_log.error("Couldnt set up the arena Id for the Block Checker event, cancelling event...");
				return;
			}
			_isStarted = true;
			ThreadPoolManager.getInstance().execute(new SpawnRound(16, 1));
			setUpPlayers();
			_startedTime = System.currentTimeMillis() + 300000;
		}
	}

	private class SpawnRound implements Runnable
	{
		int _numOfBoxes;
		int _round;

		SpawnRound(int numberOfBoxes, int round)
		{
			_numOfBoxes = numberOfBoxes;
			_round = round;
		}

		@Override
		public void run()
		{
			if (!_isStarted)
			{
				return;
			}

			switch (_round)
			{
				case 1 :
					_task = ThreadPoolManager.getInstance().schedule(new SpawnRound(20, 2), 60000);
					break;
				case 2 :
					_task = ThreadPoolManager.getInstance().schedule(new SpawnRound(14, 3), 60000);
					break;
				case 3 :
					_task = ThreadPoolManager.getInstance().schedule(new EndEvent(), 180000);
					break;
			}

			byte random = 2;
			final NpcTemplate template = NpcsParser.getInstance().getTemplate(18672);
			try
			{
				for (int i = 0; i < _numOfBoxes; i++)
				{
					final Spawner spawn = new Spawner(template);
					spawn.setX(_arenaCoordinates[_arena][4] + Rnd.get(-400, 400));
					spawn.setY(_arenaCoordinates[_arena][5] + Rnd.get(-400, 400));
					spawn.setZ(_zCoord);
					spawn.setAmount(1);
					spawn.setHeading(1);
					spawn.setRespawnDelay(1);
					SpawnParser.getInstance().addNewSpawn(spawn);
					spawn.init();
					final BlockInstance block = (BlockInstance) spawn.getLastSpawn();

					if ((random % 2) == 0)
					{
						block.setRed(true);
					}
					else
					{
						block.setRed(false);
					}

					block.disableCoreAI(true);
					_spawns.add(spawn);
					random++;
				}
			}
			catch (final Exception e)
			{
				_log.warn(getClass().getSimpleName() + ": " + e.getMessage());
			}

			if ((_round == 1) || (_round == 2))
			{
				final NpcTemplate girl = NpcsParser.getInstance().getTemplate(18676);
				try
				{
					final Spawner girlSpawn = new Spawner(girl);
					girlSpawn.setX(_arenaCoordinates[_arena][4] + Rnd.get(-400, 400));
					girlSpawn.setY(_arenaCoordinates[_arena][5] + Rnd.get(-400, 400));
					girlSpawn.setZ(_zCoord);
					girlSpawn.setAmount(1);
					girlSpawn.setHeading(1);
					girlSpawn.setRespawnDelay(1);
					SpawnParser.getInstance().addNewSpawn(girlSpawn);
					girlSpawn.init();
					ThreadPoolManager.getInstance().schedule(new CarryingGirlUnspawn(girlSpawn), 9000);
				}
				catch (final Exception e)
				{
					_log.warn("Couldnt Spawn Block Checker NPCs! Wrong instance type at npc table?");
					_log.warn(getClass().getSimpleName() + ": " + e.getMessage());
				}
			}
			_redPoints += _numOfBoxes / 2;
			_bluePoints += _numOfBoxes / 2;

			final int timeLeft = (int) ((getStarterTime() - System.currentTimeMillis()) / 1000);
			final ExBlockUpSetState changePoints = new ExBlockUpSetState(timeLeft, getBluePoints(), getRedPoints());
			getHolder().broadCastPacketToTeam(changePoints);
		}
	}

	private class CarryingGirlUnspawn implements Runnable
	{
		private final Spawner _spawn;

		protected CarryingGirlUnspawn(Spawner spawn)
		{
			_spawn = spawn;
		}

		@Override
		public void run()
		{
			if (_spawn == null)
			{
				_log.warn("HBCE: Block Carrying Girl is null");
				return;
			}
			SpawnParser.getInstance().deleteSpawn(_spawn);
			_spawn.stopRespawn();
			_spawn.getLastSpawn().deleteMe();
		}
	}

	protected class EndEvent implements Runnable
	{
		private void clearMe()
		{
			HandysBlockCheckerManager.getInstance().clearPaticipantQueueByArenaId(_arena);
			_holder.clearPlayers();
			_blueTeamPoints.clear();
			_redTeamPoints.clear();
			HandysBlockCheckerManager.getInstance().setArenaFree(_arena);

			for (Spawner spawn : _spawns)
			{
				spawn.stopRespawn();
				spawn.getLastSpawn().deleteMe();
				SpawnParser.getInstance().deleteSpawn(spawn);
				spawn = null;
			}
			_spawns.clear();

			for (final ItemInstance item : _drops)
			{
				if (item == null)
				{
					continue;
				}

				if (!item.isVisible() || (item.getOwnerId() != 0))
				{
					continue;
				}
				item.decayMe();
			}
			_drops.clear();
		}

		private void rewardPlayers()
		{
			if (_redPoints == _bluePoints)
			{
				return;
			}

			_isRedWinner = _redPoints > _bluePoints ? true : false;

			if (_isRedWinner)
			{
				rewardAsWinner(true);
				rewardAsLooser(false);
				final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.TEAM_C1_WON);
				msg.addString("Red Team");
				_holder.broadCastPacketToTeam(msg);
			}
			else if (_bluePoints > _redPoints)
			{
				rewardAsWinner(false);
				rewardAsLooser(true);
				final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.TEAM_C1_WON);
				msg.addString("Blue Team");
				_holder.broadCastPacketToTeam(msg);
			}
			else
			{
				rewardAsLooser(true);
				rewardAsLooser(false);
			}
		}

		private void rewardAsWinner(boolean isRed)
		{
			final Map<Player, Integer> tempPoints = isRed ? _redTeamPoints : _blueTeamPoints;

			for (final Entry<Player, Integer> points : tempPoints.entrySet())
			{
				if (points.getKey() == null)
				{
					continue;
				}

				if (points.getValue() >= 10)
				{
					points.getKey().addItem("Block Checker", 13067, 2, points.getKey(), true);
				}
				else
				{
					tempPoints.remove(points.getKey());
				}
			}

			int first = 0, second = 0;
			Player winner1 = null, winner2 = null;
			for (final Entry<Player, Integer> entry : tempPoints.entrySet())
			{
				final Player pc = entry.getKey();
				final int pcPoints = entry.getValue();
				if (pcPoints > first)
				{
					second = first;
					winner2 = winner1;
					first = pcPoints;
					winner1 = pc;
				}
				else if (pcPoints > second)
				{
					second = pcPoints;
					winner2 = pc;
				}
			}
			if (winner1 != null)
			{
				winner1.addItem("Block Checker", 13067, 8, winner1, true);
			}
			if (winner2 != null)
			{
				winner2.addItem("Block Checker", 13067, 5, winner2, true);
			}
		}

		private void rewardAsLooser(boolean isRed)
		{
			final Map<Player, Integer> tempPoints = isRed ? _redTeamPoints : _blueTeamPoints;

			for (final Entry<Player, Integer> entry : tempPoints.entrySet())
			{
				final Player player = entry.getKey();
				if ((player != null) && (entry.getValue() >= 10))
				{
					player.addItem("Block Checker", 13067, 2, player, true);
				}
			}
		}

		private void setPlayersBack()
		{
			final ExBlockUpSetState end = new ExBlockUpSetState(_isRedWinner);

			for (final Player player : _holder.getAllPlayers())
			{
				if (player == null)
				{
					continue;
				}

				player.stopAllEffects();
				player.setTeam(0);
				player.setBlockCheckerArena(DEFAULT_ARENA);
				final PcInventory inv = player.getInventory();
				if (inv.getItemByItemId(13787) != null)
				{
					final long count = inv.getInventoryItemCount(13787, 0);
					inv.destroyItemByItemId("Handys Block Checker", 13787, count, player, player);
				}
				if (inv.getItemByItemId(13788) != null)
				{
					final long count = inv.getInventoryItemCount(13788, 0);
					inv.destroyItemByItemId("Handys Block Checker", 13788, count, player, player);
				}
				broadcastRelationChanged(player);
				player.teleToLocation(-57478, -60367, -2370, true, player.getReflection());
				player.sendPacket(end);
				player.broadcastUserInfo(true);
			}
		}

		@Override
		public void run()
		{
			if (!_abnormalEnd)
			{
				rewardPlayers();
			}
			setPlayersBack();
			clearMe();
			_isStarted = false;
			_abnormalEnd = false;
		}
	}
}