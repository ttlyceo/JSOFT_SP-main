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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.aggro.AggroInfo;
import l2e.gameserver.model.actor.templates.npc.aggro.GroupInfo;
import l2e.gameserver.model.entity.Hero;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.taskmanager.RaidBossTaskManager;

public class RaidBossInstance extends MonsterInstance
{
	private RaidBossSpawnManager.StatusEnum _raidStatus;
	private boolean _useRaidCurse = true;
	
	public RaidBossInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.RaidBossInstance);
		setIsRaid(true);
	}
	
	@Override
	protected int getMaintenanceInterval()
	{
		return 30000;
	}
	
	@Override
	public int getKilledInterval(MonsterInstance minion)
	{
		final int respawnTime = Config.MINIONS_RESPAWN_TIME.containsKey(minion.getId()) ? Config.MINIONS_RESPAWN_TIME.get(minion.getId()) * 1000 : -1;
		return respawnTime < 0 ? minion.getLeader().isRaid() ? (int) Config.RAID_MINION_RESPAWN_TIMER : 0 : respawnTime;
	}

	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();
	}

	@Override
	protected void onDeath(Creature killer)
	{
		super.onDeath(killer);
		
		RaidBossTaskManager.getInstance().removeMinions(this);

		final int points = getTemplate().getRewardRp();
		if (points > 0)
		{
			calcRaidPointsReward(points);
		}
		
		if (this instanceof LostCaptainInstance)
		{
			return;
		}
		
		if (killer != null && killer.isPlayable())
		{
			final Player player = killer.getActingPlayer();
			if (player != null)
			{
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
				if (player.getParty() != null)
				{
					for (final Player member : player.getParty().getMembers())
					{
						if (member.isNoble() && getDistance(member) <= Config.ALT_PARTY_RANGE)
						{
							Hero.getInstance().setRBkilled(member.getObjectId(), getId());
						}
					}
				}
				else
				{
					if (player.isNoble())
					{
						Hero.getInstance().setRBkilled(player.getObjectId(), getId());
					}
				}
				player.getCounters().addAchivementInfo("raidKiller", getId(), -1, false, true, false);
			}
		}
	}
	
	@Override
	public void onDecay()
	{
		super.onDecay();
		RaidBossSpawnManager.getInstance().updateStatus(this, true);
	}
	
	private void calcRaidPointsReward(int totalPoints)
	{
		final Map<Object, GroupInfo> groupsInfo = new HashMap<>();
		final double totalHp = getMaxHp();

		for (final AggroInfo ai : getAggroList().getCharMap().values())
		{
			final Player player = ai.getAttacker().getActingPlayer();
			if (player != null)
			{
				final Object key = player.getParty() != null ? player.getParty().getCommandChannel() != null ? player.getParty().getCommandChannel() : player.getParty() : player.getActingPlayer();
				GroupInfo info = groupsInfo.get(key);
				if (info == null)
				{
					info = new GroupInfo();
					groupsInfo.put(key, info);
				}

				if (key instanceof CommandChannel)
				{
					for (final Player p : ((CommandChannel) key))
					{
						if (p.isInRangeZ(this, Config.ALT_PARTY_RANGE2))
						{
							info.getPlayer().add(p);
						}
					}
				}
				else if (key instanceof Party)
				{
					for (final Player p : ((Party) key).getMembers())
					{
						if (p.isInRangeZ(this, Config.ALT_PARTY_RANGE2))
						{
							info.getPlayer().add(p);
						}
					}
				}
				else
				{
					info.getPlayer().add(player);
				}
				info.addReward(ai.getDamage());
			}
		}

		for (final GroupInfo groupInfo : groupsInfo.values())
		{
			final HashSet<Player> players = groupInfo.getPlayer();
			final int perPlayer = (int) Math.round(totalPoints * groupInfo.getReward() / (totalHp * players.size()));
			for (final Player player : players)
			{
				if (player != null)
				{
					int playerReward = perPlayer;
					playerReward = (int) Math.round(playerReward * ExperienceParser.getInstance().penaltyModifier(calculateLevelDiffForDrop(player.getLevel()), 9));
					if (playerReward == 0)
					{
						continue;
					}
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_RAID_POINTS).addNumber(playerReward));
					RaidBossSpawnManager.getInstance().addPoints(player, getId(), playerReward);
				}
			}
		}
		RaidBossSpawnManager.getInstance().calculateRanking();
	}
	
	@Override
	public void notifyMinionDied(MonsterInstance minion)
	{
		final int respawnTime = getKilledInterval(minion);
		if (respawnTime > 0)
		{
			RaidBossTaskManager.getInstance().addToMinionList(minion, (System.currentTimeMillis() + respawnTime));
		}
		super.notifyMinionDied(minion);
	}
	
	public void setRaidStatus(RaidBossSpawnManager.StatusEnum status)
	{
		_raidStatus = status;
	}

	public RaidBossSpawnManager.StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}

	@Override
	public double getVitalityPoints(int damage)
	{
		return -super.getVitalityPoints(damage) / 100 + Config.VITALITY_RAID_BONUS;
	}

	@Override
	public boolean useVitalityRate()
	{
		return false;
	}

	public void setUseRaidCurse(boolean val)
	{
		_useRaidCurse = val;
	}

	@Override
	public boolean giveRaidCurse()
	{
		return _useRaidCurse;
	}
	
	@Override
	public boolean isLethalImmune()
	{
		return true;
	}
}