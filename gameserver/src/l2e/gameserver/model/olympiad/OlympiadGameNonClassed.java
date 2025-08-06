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
package l2e.gameserver.model.olympiad;

import java.util.Set;

import l2e.gameserver.Config;

public class OlympiadGameNonClassed extends OlympiadGameNormal
{
	private OlympiadGameNonClassed(int id, Participant[] opponents, boolean isTraining)
	{
		super(id, opponents, isTraining);
	}

	@Override
	public final CompetitionType getType()
	{
		return CompetitionType.NON_CLASSED;
	}

	@Override
	protected final int getDivider()
	{
		return Config.ALT_OLY_DIVIDER_NON_CLASSED;
	}

	@Override
	protected final int[][] getReward()
	{
		return Config.ALT_OLY_NONCLASSED_REWARD;
	}

	@Override
	protected final int[][] getLoseReward()
	{
		return Config.ALT_OLY_NONCLASSED_LOSE_REWARD;
	}
	
	@Override
	protected final String getWeeklyMatchType()
	{
		return COMP_DONE_WEEK_NON_CLASSED;
	}
	
	protected static final OlympiadGameNonClassed createGame(int id, Set<Integer> list)
	{
		final Participant[] opponents = OlympiadGameNormal.createListOfParticipants(list);
		if (opponents == null)
		{
			return null;
		}

		return new OlympiadGameNonClassed(id, opponents, Olympiad.getInstance().isTrainingPeriod());
	}
}