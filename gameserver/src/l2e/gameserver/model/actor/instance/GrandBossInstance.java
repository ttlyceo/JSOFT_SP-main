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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.instancemanager.RaidBossSpawnManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.actor.templates.npc.aggro.AggroInfo;
import l2e.gameserver.model.actor.templates.npc.aggro.GroupInfo;
import l2e.gameserver.model.entity.Hero;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.strings.server.ServerStorage;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.taskmanager.RaidBossTaskManager;

public final class GrandBossInstance extends MonsterInstance
{
	private boolean _useRaidCurse = true;
	private final Map<String, Integer> _damageInfo = new HashMap<>();
	private long _infoUpdateTime = 0L;
	private long _infoTotalTime = 0L;

	public GrandBossInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.GrandBossInstance);
		setIsRaid(true);
		setIsEpicRaid(true);
	}
	
	@Override
	protected int getMaintenanceInterval()
	{
		return 10000;
	}
	
	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (Config.ALLOW_DAMAGE_INFO)
		{
			if (attacker != null && damage > 0)
			{
				final Player player = attacker.getActingPlayer();
				if (player != null && player.getClan() != null)
				{
					if (_infoTotalTime != 0 && (_infoTotalTime + (Config.DAMAGE_INFO_LIMIT_TIME * 3600000) < System.currentTimeMillis()))
					{
						_infoTotalTime = 0;
						_infoUpdateTime = 0;
						_damageInfo.clear();
					}
					checkInfoDamage(player.getClan(), damage);
				}
			}
		}
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	@Override
	public int getKilledInterval(MonsterInstance minion)
	{
		final int respawnTime = Config.MINIONS_RESPAWN_TIME.containsKey(minion.getId()) ? Config.MINIONS_RESPAWN_TIME.get(minion.getId()) * 1000 : -1;
		return respawnTime < 0 ? minion.getLeader().isRaid() ? (int) Config.RAID_MINION_RESPAWN_TIMER : 0 : respawnTime;
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
	
	@Override
	protected void onDeath(Creature killer)
	{
		super.onDeath(killer);
		
		RaidBossTaskManager.getInstance().removeMinions(this);
		
		if (Config.ALLOW_DAMAGE_INFO)
		{
			_infoTotalTime = 0;
			_infoUpdateTime = 0;
			_damageInfo.clear();
		}
		
		final int points = getTemplate().getRewardRp();
		if (points > 0)
		{
			calcRaidPointsReward(points);
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
				player.getCounters().addAchivementInfo("epicKiller", getId(), -1, false, true, false);
			}
		}
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
	
	private void checkInfoDamage(Clan clan, double damage)
	{
		if (_infoUpdateTime == 0)
		{
			_infoUpdateTime = System.currentTimeMillis();
			_infoTotalTime = System.currentTimeMillis();
		}
		
		if (_damageInfo.containsKey(clan.getName()))
		{
			final double totalDamage = _damageInfo.get(clan.getName());
			_damageInfo.put(clan.getName(), (int) (totalDamage + damage));
		}
		else
		{
			_damageInfo.put(clan.getName(), (int) damage);
		}
		
		if ((_infoUpdateTime + (Config.DAMAGE_INFO_UPDATE * 1000L)) < System.currentTimeMillis())
		{
			_infoUpdateTime = System.currentTimeMillis();
			
			if (_damageInfo != null)
			{
				final List<DamageInfo> damageList = new ArrayList<>();
				
				final StringBuilder builderEn = new StringBuilder();
				final StringBuilder builderRu = new StringBuilder();
				for (final String clanName : _damageInfo.keySet())
				{
					damageList.add(new DamageInfo(clanName, _damageInfo.get(clanName)));
				}
				
				final Comparator<DamageInfo> statsComparator = new SortDamageInfo();
				Collections.sort(damageList, statsComparator);
				
				for (final DamageInfo info : damageList)
				{
					if (info != null)
					{
						builderEn.append("" + ServerStorage.getInstance().getString("en", "EpicDamageInfo.CLAN") + "").append(' ').append(info.getClanName()).append(": ").append(getDamageFormat(info.getDamage(), "en")).append('\n');
						builderRu.append("" + ServerStorage.getInstance().getString("ru", "EpicDamageInfo.CLAN") + "").append(' ').append(info.getClanName()).append(": ").append(getDamageFormat(info.getDamage(), "ru")).append('\n');
					}
				}
				
				final ExShowScreenMessage msgEn = new ExShowScreenMessage(builderEn.toString(), (Config.DAMAGE_INFO_UPDATE * 1000), ExShowScreenMessage.TOP_LEFT, false);
				final ExShowScreenMessage msgRu = new ExShowScreenMessage(builderRu.toString(), (Config.DAMAGE_INFO_UPDATE * 1000), ExShowScreenMessage.TOP_LEFT, false);
				for (final Player player : World.getInstance().getAroundPlayers(this, 2000, 200))
				{
					player.sendPacket(player.getLang().equalsIgnoreCase("ru") ? msgRu : msgEn);
				}
			}
		}
	}
	
	private static class DamageInfo
	{
		private final String _clan;
		private final int _damage;
		
		public DamageInfo(String clan, int damage)
		{
			_clan = clan;
			_damage = damage;
		}
		
		public final String getClanName()
		{
			return _clan;
		}
		
		public final int getDamage()
		{
			return _damage;
		}
	}
	
	private static class SortDamageInfo implements Comparator<DamageInfo>, Serializable
	{
		private static final long serialVersionUID = 7691414259610932752L;
		
		@Override
		public int compare(DamageInfo o1, DamageInfo o2)
		{
			return Integer.compare(o2.getDamage(), o1.getDamage());
		}
	}
	
	private static String getDamageFormat(int damage, String lang)
	{
		final String scount = Integer.toString(damage);
		if (damage < 1000)
		{
			return scount;
		}
		if ((damage > 999) && (damage < 1000000))
		{
			return scount.substring(0, scount.length() - 3) + "" + ServerStorage.getInstance().getString(lang, "EpicDamageInfo.K");
		}
		if ((damage > 999999) && (damage < 1000000000))
		{
			return scount.substring(0, scount.length() - 6) + "" + ServerStorage.getInstance().getString(lang, "EpicDamageInfo.KK");
		}
		if (damage > 999999999)
		{
			return scount.substring(0, scount.length() - 9) + "" + ServerStorage.getInstance().getString(lang, "EpicDamageInfo.KKK");
		}
		return "0";
	}
}