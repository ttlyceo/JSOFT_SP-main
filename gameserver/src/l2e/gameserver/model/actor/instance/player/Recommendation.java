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

import java.util.Calendar;

import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.player.impl.RecVoteTask;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.skills.effects.EffectType;

public class Recommendation
{
	public static final int[][] REC_BONUS =
	{
	        {
	                25, 50, 50, 50, 50, 50, 50, 50, 50, 50
			},
			{
			        16, 33, 50, 50, 50, 50, 50, 50, 50, 50
			},
			{
			        12, 25, 37, 50, 50, 50, 50, 50, 50, 50
			},
			{
			        10, 20, 30, 40, 50, 50, 50, 50, 50, 50
			},
			{
			        8, 16, 25, 33, 41, 50, 50, 50, 50, 50
			},
			{
			        7, 14, 21, 28, 35, 42, 50, 50, 50, 50
			},
			{
			        6, 12, 18, 25, 31, 37, 43, 50, 50, 50
			},
			{
			        5, 11, 16, 22, 27, 33, 38, 44, 50, 50
			},
			{
			        5, 10, 15, 20, 25, 30, 35, 40, 45, 50
			},
			{
			        5, 10, 15, 20, 25, 30, 35, 40, 45, 50
			},
			{
			        5, 10, 15, 20, 25, 30, 35, 40, 45, 50
			}
	};
	
	private final Player _owner;
	private int _recomHave;
	private int _recomLeft;
	private int _recomTimeLeft;
	private long _recomBonusStart;

	public Recommendation(Player player)
	{
		_owner = player;
	}

	private Player getPlayer()
	{
		return _owner;
	}

	public void checkRecom()
	{
		final var player = getPlayer();
		if (player == null)
		{
			return;
		}

		final Calendar temp = Calendar.getInstance();
		temp.set(Calendar.HOUR_OF_DAY, 6);
		temp.set(Calendar.MINUTE, 30);
		temp.set(Calendar.SECOND, 0);
		long count = Math.round(((System.currentTimeMillis() - player.getLastAccess()) / 1000) / 86400);

		if ((count == 0) && (player.getLastAccess() < temp.getTimeInMillis()) && (System.currentTimeMillis() > temp.getTimeInMillis()))
		{
			count++;
		}
		int time = 0;

		if (count != 0)
		{
			setRecomLeft(20);
			setRecomTimeLeft(3600);
			int have = getRecomHave();
			for (int i = 0; i < count; i++)
			{
				have -= 20;
			}
			if (have < 0)
			{
				have = 0;
			}
			setRecomHave(have);
			time = 2;
		}
		updateVoteInfo();
		if (Config.ALLOW_RECO_BONUS_SYSTEM)
		{
			player.getPersonalTasks().addTask(new RecVoteTask(3600000L, time));
		}
	}

	public void restartRecom()
	{
		final var player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		setRecomLeft(20);
		setRecomTimeLeft(3600);
		
		_recomHave -= 20;
		if (_recomHave < 0)
		{
			_recomHave = 0;
		}
		
		player.getPersonalTasks().removeTask(27, false);
		updateVoteInfo();
		if (Config.ALLOW_RECO_BONUS_SYSTEM)
		{
			player.getPersonalTasks().addTask(new RecVoteTask(3600000L, 2));
		}
	}

	public void startRecBonus()
	{
		if (isRecBonusActive() || isHourglassBonusActive() > 0 || !Config.ALLOW_RECO_BONUS_SYSTEM)
		{
			return;
		}
		
		Player player;
		if (getRecomTimeLeft() == 0 || ((player = getPlayer()) != null && player.isInZonePeace()))
		{
			stopRecBonus();
			return;
		}
		_recomBonusStart = System.currentTimeMillis();
		updateVoteInfo();
	}

	public void stopRecBonus()
	{
		if (!isRecBonusActive())
		{
			return;
		}
		_recomTimeLeft = getRecomTimeLeft();
		_recomBonusStart = 0;
		updateVoteInfo();
	}

	public boolean isRecBonusActive()
	{
		return _recomBonusStart != 0;
	}

	public void updateVoteInfo()
	{
		final var player = getPlayer();
		if (player == null)
		{
			return;
		}
		player.sendUserInfo(true);
		player.sendVoteSystemInfo();
	}

	public void addRecomHave(int value)
	{
		setRecomHave(getRecomHave() + value);
		updateVoteInfo();
	}

	public void addRecomLeft(int value)
	{
		setRecomLeft(getRecomLeft() + value);
		updateVoteInfo();
	}

	public void giveRecom(Player target)
	{
		final int targetRecom = target.getRecommendation().getRecomHave();
		if (targetRecom < 255)
		{
			target.getRecommendation().setRecomHave(targetRecom + 1);
			target.getCounters().addAchivementInfo("recomHave", 0, -1, false, false, false);
			target.getRecommendation().updateVoteInfo();
		}
		
		if (_recomLeft > 0)
		{
			if (getPlayer() != null)
			{
				getPlayer().getCounters().addAchivementInfo("recomLeft", 0, -1, false, false, false);
			}
			_recomLeft -= 1;
		}
	}

	public int getRecomHave()
	{
		return _recomHave;
	}

	public void setRecomHave(int value)
	{
		_recomHave = Math.max(Math.min(value, 255), 0);
	}

	public int getRecomLeft()
	{
		return _recomLeft;
	}

	public void setRecomLeft(int value)
	{
		_recomLeft = Math.max(Math.min(value, 255), 0);
	}

	public int getRecomTimeLeft()
	{
		return isRecBonusActive() ? Math.max(_recomTimeLeft - (int) (System.currentTimeMillis() - _recomBonusStart) / 1000, 0) : _recomTimeLeft;
	}

	public void setRecomTimeLeft(int value)
	{
		_recomTimeLeft = value;
	}

	public long isHourglassBonusActive()
	{
		final var player = getPlayer();
		if (player == null || !Config.ALLOW_RECO_BONUS_SYSTEM)
		{
			return 0L;
		}
		
		if (player.isOnline())
		{
			final Effect effect = player.getEffectList().getFirstEffect(EffectType.NEVIT_HOURGLASS);
			if (effect != null)
			{
				return effect.getTimeLeft();
			}
		}
		return 0L;
	}

	public int getRecomExpBonus()
	{
		final var player = getPlayer();
		if (player == null || !Config.ALLOW_RECO_BONUS_SYSTEM)
		{
			return 0;
		}
		else if (isHourglassBonusActive() <= 0)
		{
			if (getRecomTimeLeft() <= 0)
			{
				return 0;
			}
			else if (player.getLevel() < 1)
			{
				return 0;
			}
			else if (getRecomHave() < 1)
			{
				return 0;
			}
		}
		
		if (getRecomHave() >= 100)
		{
			return 50;
		}
		
		final int lvl = Math.min((player.getLevel() / 10), (REC_BONUS.length - 1));
		final int exp = (Math.min(100, getRecomHave()) - 1) / 10;
		
		return REC_BONUS[lvl][exp];
	}

	public double getRecoMultiplier()
	{
		double multiplier = 0;
		final double bonus = getRecomExpBonus();
		if (bonus > 0)
		{
			multiplier += (bonus / 100);
		}
		return multiplier;
	}
}