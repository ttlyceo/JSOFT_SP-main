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
package l2e.gameserver.model.actor.templates.sieges;

import java.util.List;

import l2e.commons.util.Rnd;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.stats.StatsSet;

public class CastleTemplate
{
	private final int _id;
	private final StatsSet _params;
	private final List<SiegeSpawn> _controlTowers;
	private final List<SiegeSpawn> _flameTowers;
	private boolean _allowCastlePvpReward = false;
	private boolean _allowCaptureCastleReward = false;
	private boolean _allowDefenceCastleReward = false;
	private boolean _allowCastlePvpRewardForParty = false;
	private boolean _allowCaptureCastleRewardForLeader = false;
	private boolean _allowDefenceCastleRewardForLeader = false;
	private final List<ItemHolder> _pvpRewards;
	private final List<ItemHolder> _castleCaptureRewards;
	private final List<ItemHolder> _castleDefenceRewards;
	private int _bloodAllianceCount = 0;
	
	public CastleTemplate(int id, StatsSet params, List<SiegeSpawn> controlTowers, List<SiegeSpawn> flameTowers, boolean allowCastlePvpReward, boolean allowCaptureCastleReward, boolean allowDefenceCastleReward, boolean allowCastlePvpRewardForParty, boolean allowCaptureCastleRewardForLeader, boolean allowDefenceCastleRewardForLeader, List<ItemHolder> pvpRewards, List<ItemHolder> castleCaptureRewards, List<ItemHolder> castleDefenceRewards)
	{
		_id = id;
		_params = params;
		_controlTowers = controlTowers;
		_flameTowers = flameTowers;
		_allowCastlePvpReward = allowCastlePvpReward;
		_allowCaptureCastleReward = allowCaptureCastleReward;
		_allowDefenceCastleReward = allowDefenceCastleReward;
		_allowCastlePvpRewardForParty = allowCastlePvpRewardForParty;
		_allowCaptureCastleRewardForLeader = allowCaptureCastleRewardForLeader;
		_allowDefenceCastleRewardForLeader = allowDefenceCastleRewardForLeader;
		_pvpRewards = pvpRewards;
		_castleCaptureRewards = castleCaptureRewards;
		_castleDefenceRewards = castleDefenceRewards;
		for (final var holder : _castleDefenceRewards)
		{
			if (holder != null && holder.getId() == 9911)
			{
				_bloodAllianceCount = (int) holder.getCount();
			}
		}
	}

	public int getId()
	{
		return _id;
	}
	
	public String getName(String lang)
	{
		try
		{
			return _params.getString(lang != null ? "name" + lang.substring(0, 1).toUpperCase() + lang.substring(1) : "name" + Config.MULTILANG_DEFAULT.substring(0, 1).toUpperCase() + Config.MULTILANG_DEFAULT.substring(1));
		}
		catch (final IllegalArgumentException e)
		{
			return "";
		}
	}
	
	public List<SiegeSpawn> getControlTowers()
	{
		return _controlTowers;
	}
	
	public List<SiegeSpawn> getFlameTowers()
	{
		return _flameTowers;
	}
	
	public StatsSet getParams()
	{
		return _params;
	}
	
	public boolean isEnableSiege()
	{
		return _params.getBool("enableSiege");
	}
	
	public int getCloseChangeDataTime()
	{
		return _params.getInteger("siegeCloseChangeData");
	}
	
	public int getCloseRegistrationTime()
	{
		return _params.getInteger("siegeCloseRegistration");
	}
	
	public int getMaxFlags()
	{
		return _params.getInteger("maxFlags");
	}
	
	public int getBloodAllianceCount()
	{
		return _bloodAllianceCount;
	}
	
	public int getMinSiegeClanLevel()
	{
		return _params.getInteger("siegeClanLevel");
	}
	
	public int getAttackerClansLimit()
	{
		return _params.getInteger("attackerClans");
	}
	
	public int getDefenderClansLimit()
	{
		return _params.getInteger("defenderClans");
	}
	
	public int getSiegeLimitPlayers()
	{
		return _params.getInteger("siegeLimitPlayers");
	}
	
	public String getSiegeDate()
	{
		return _params.getString("siegeDate");
	}
	
	public int getSiegeTime()
	{
		return _params.getInteger("siegeTime");
	}
	
	public boolean canAttackSameSiegeSide()
	{
		return _params.getBool("attackSameSiegeSide");
	}
	
	public int getHpRegenFunction(int level)
	{
		switch (level)
		{
			case 2 :
				return _params.getInteger("hpRegenPriceLvl2");
			case 3 :
				return _params.getInteger("hpRegenPriceLvl3");
			case 4 :
				return _params.getInteger("hpRegenPriceLvl4");
			case 5 :
				return _params.getInteger("hpRegenPriceLvl5");
			default :
				return _params.getInteger("hpRegenPriceLvl1");
		}
	}
	
	public int getHpRegenFunctionTime()
	{
		return _params.getInteger("hpRegenFunctionTime");
	}
	
	public int getMpRegenFunction(int level)
	{
		switch (level)
		{
			case 2 :
				return _params.getInteger("mpRegenPriceLvl2");
			case 3 :
				return _params.getInteger("mpRegenPriceLvl3");
			case 4 :
				return _params.getInteger("mpRegenPriceLvl4");
			default :
				return _params.getInteger("mpRegenPriceLvl1");
		}
	}
	
	public int getMpRegenFunctionTime()
	{
		return _params.getInteger("mpRegenFunctionTime");
	}
	
	public int getExpRegenFunction(int level)
	{
		switch (level)
		{
			case 2 :
				return _params.getInteger("expRegenPriceLvl2");
			case 3 :
				return _params.getInteger("expRegenPriceLvl3");
			case 4 :
				return _params.getInteger("expRegenPriceLvl4");
			default :
				return _params.getInteger("expRegenPriceLvl1");
		}
	}
	
	public int getExpRegenFunctionTime()
	{
		return _params.getInteger("expRegenFunctionTime");
	}
	
	public int getSupportFunction(int level)
	{
		switch (level)
		{
			case 2 :
				return _params.getInteger("supportPriceLvl2");
			case 3 :
				return _params.getInteger("supportPriceLvl3");
			case 4 :
				return _params.getInteger("supportPriceLvl4");
			default :
				return _params.getInteger("supportPriceLvl1");
		}
	}
	
	public int getSupportFunctionTime()
	{
		return _params.getInteger("supportFunctionTime");
	}
	
	public int getTeleportFunction(int level)
	{
		switch (level)
		{
			case 2 :
				return _params.getInteger("teleportPriceLvl2");
			default :
				return _params.getInteger("teleportPriceLvl1");
		}
	}
	
	public int getTeleportFunctionTime()
	{
		return _params.getInteger("teleportFunctionTime");
	}
	
	public String getCorrectSiegeTime()
	{
		return _params.getString("correctSiegeHoursTime");
	}
	
	public void checkCastlePvpReward(Player killer, Player target)
	{
		if (!_allowCastlePvpReward || _pvpRewards.isEmpty())
		{
			return;
		}
		
		if (_allowCastlePvpRewardForParty && killer.isInParty())
		{
			if (target.isInParty() && (target.getParty() == killer.getParty()))
			{
				return;
			}
			
			if (killer != null && killer.getParty() != null)
			{
				killer.getParty().getMembers().stream().filter(p -> p != null).forEach(pm -> _pvpRewards.stream().filter(r -> (r != null && r.getId() != 9911) && Rnd.chance(r.getChance())).forEach(i -> Util.getReward(pm, i.getId(), i.getCount())));
			}
		}
		else
		{
			if (killer != null)
			{
				_pvpRewards.stream().filter(r -> (r != null && r.getId() != 9911) && Rnd.chance(r.getChance())).forEach(i -> Util.getReward(killer, i.getId(), i.getCount()));
			}
		}
	}
	
	public void checkCastleCaptureReward(Clan clan, int castleId)
	{
		if (!_allowCaptureCastleReward || _castleCaptureRewards.isEmpty() || clan == null)
		{
			return;
		}
		
		if (_allowCaptureCastleRewardForLeader)
		{
			final var leader = GameObjectsStorage.getPlayer(clan.getLeaderId());
			if (leader != null)
			{
				_castleCaptureRewards.stream().filter(r -> (r != null) && r.getId() != 9911 && Rnd.chance(r.getChance())).forEach(i -> Util.getReward(leader, i.getId(), i.getCount()));
			}
		}
		else
		{
			for (final var member : clan.getMembers())
			{
				if (member != null && member.isOnline())
				{
					for (final var tr : _castleDefenceRewards)
					{
						if (tr != null && tr.getId() != 9911 && Rnd.chance(tr.getChance()))
						{
							Util.getReward(member.getPlayerInstance(), tr.getId(), tr.getCount());
						}
					}
				}
			}
		}
	}
	
	public void checkCastleDefenceReward(Clan clan, int castleId)
	{
		if (!_allowDefenceCastleReward || _castleDefenceRewards.isEmpty() || clan == null)
		{
			return;
		}
		
		if (_allowDefenceCastleRewardForLeader)
		{
			final var leader = GameObjectsStorage.getPlayer(clan.getLeaderId());
			if (leader != null)
			{
				_castleDefenceRewards.stream().filter(r -> (r != null) && r.getId() != 9911 && Rnd.chance(r.getChance())).forEach(i -> Util.getReward(leader, i.getId(), i.getCount()));
			}
		}
		else
		{
			for (final var member : clan.getMembers())
			{
				if (member != null && member.isOnline())
				{
					for (final var tr : _castleDefenceRewards)
					{
						if (tr != null && tr.getId() != 9911 && Rnd.chance(tr.getChance()))
						{
							Util.getReward(member.getPlayerInstance(), tr.getId(), tr.getCount());
						}
					}
				}
			}
		}
	}
}