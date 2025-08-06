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
package l2e.gameserver.model.actor.instance.player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2e.commons.util.Util;
import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.player.AchiveTemplate;
import l2e.gameserver.model.entity.events.custom.achievements.AchievementManager;
import l2e.gameserver.network.serverpackets.ShowTutorialMark;

public class AchievementCounters
{
	public final Map<Integer, Long> _achievements = new ConcurrentHashMap<>();
	
	private Player _player = null;
	protected int _objId = 0;

	public AchievementCounters(Player activeChar)
	{
		_player = activeChar;
		_objId = activeChar == null ? 0 : activeChar.getObjectId();
		_achievements.clear();
	}
	
	public Map<Integer, Long> getAchievements()
	{
		return _achievements;
	}
	
	public long getAchievementInfo(int id)
	{
		if (!hasAchievementInfo(id))
		{
			_achievements.put(id, 0L);
		}
		return _achievements.get(id);
	}
	
	public void setAchievementInfo(int id, long points, boolean addSum)
	{
		if (_player.getAchievements() == null || _player.getAchievements().isEmpty())
		{
			return;
		}
		
		if (hasAchievementInfo(id))
		{
			final long nextPoints = addSum ? points : getAchievementInfo(id) + points;
			_achievements.put(id, nextPoints);
		}
		else
		{
			_achievements.put(id, points);
		}
		checkProgress(id);
	}
	
	public void refreshAchievementInfo(int id)
	{
		_achievements.put(id, 0L);
		checkProgress(id);
	}
	
	public boolean hasAchievementInfo(int id)
	{
		return _achievements.containsKey(id);
	}

	public void delAchievementInfo(int id)
	{
		_achievements.remove(id);
	}
	
	protected Player getPlayer()
	{
		return _player;
	}
	
	public void checkProgress(int id)
	{
		if (_player == null || !AchievementManager.getInstance().isActive())
		{
			return;
		}
		
		if (AchievementManager.getInstance().isActive())
		{
			final AchiveTemplate arc = AchievementManager.getInstance().getAchievement(id);
			if (arc != null)
			{
				final int achievementId = arc.getId();
				int achievementLevel = _player.getAchievements().get(achievementId);
				if (AchievementManager.getInstance().getMaxLevel(achievementId) <= achievementLevel)
				{
					return;
				}
				
				final AchiveTemplate nextLevelAchievement = AchievementManager.getInstance().getAchievement(achievementId, ++achievementLevel);
				if (nextLevelAchievement != null && nextLevelAchievement.isDone(getAchievementInfo(nextLevelAchievement.getId())))
				{
					_player.sendPacket(new ShowTutorialMark(_player.getObjectId(), 0));
				}
			}
		}
	}
	
	public void addAchivementInfo(String type, int select, long points, boolean addSum, boolean isForParty, boolean isForClan)
	{
		if (!AchievementManager.getInstance().isActive())
		{
			return;
		}
		
		final long addPoints = points > 0 ? points : 1;
		
		AchiveTemplate arc = null;
		if (type.equals("killbyId"))
		{
			arc = AchievementManager.getInstance().getAchievementKillById(select);
		}
		else if (type.equals("reflectionById"))
		{
			arc = AchievementManager.getInstance().getAchievementRefById(select);
		}
		else if (type.equals("questById"))
		{
			arc = AchievementManager.getInstance().getAchievementQuestById(select);
		}
		else if (type.equals("enchantWeaponByLvl"))
		{
			arc = AchievementManager.getInstance().getAchievementWeaponEnchantByLvl(select);
		}
		else if (type.equals("enchantArmorByLvl"))
		{
			arc = AchievementManager.getInstance().getAchievementArmorEnchantByLvl(select);
		}
		else if (type.equals("enchantJewerlyByLvl"))
		{
			arc = AchievementManager.getInstance().getAchievementJewerlyEnchantByLvl(select);
		}
		else
		{
			arc = AchievementManager.getInstance().getAchievementType(type);
		}
		
		if (arc != null)
		{
			if (isForParty)
			{
				if (_player.getParty() != null)
				{
					final CommandChannel channel = _player.getParty().getCommandChannel();
					if (channel != null)
					{
						for (final Player ccMember : channel.getMembers())
						{
							if (ccMember != null && Util.checkIfInRange(3000, _player, ccMember, false))
							{
								ccMember.getCounters().setAchievementInfo(arc.getId(), addPoints, addSum);
							}
						}
					}
					else
					{
						for (final Player pMember : _player.getParty().getMembers())
						{
							if (pMember != null && Util.checkIfInRange(3000, _player, pMember, false))
							{
								pMember.getCounters().setAchievementInfo(arc.getId(), addPoints, addSum);
							}
						}
					}
				}
				else
				{
					setAchievementInfo(arc.getId(), addPoints, addSum);
				}
			}
			else if (isForClan && _player.getClan() != null)
			{
				for (final Player member : _player.getClan().getOnlineMembers())
				{
					if (member != null)
					{
						member.getCounters().setAchievementInfo(arc.getId(), addPoints, addSum);
					}
				}
			}
			else
			{
				setAchievementInfo(arc.getId(), addPoints, addSum);
			}
		}
	}
}