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
package l2e.gameserver.model.actor.templates.quest;

public class QuestExperience
{
	private final long _exp;
	private final long _sp;
	private final double _expRate;
	private final double _spRate;
	private final boolean _rateableExp;
	private final boolean _rateableSp;
	
	public QuestExperience(long exp, long sp, double expRate, double spRate, boolean rateableExp, boolean rateableSp)
	{
		_exp = exp;
		_sp = sp;
		_expRate = expRate;
		_spRate = spRate;
		_rateableExp = rateableExp;
		_rateableSp = rateableSp;
	}
	
	public long getExp()
	{
		return _exp;
	}

	public long getSp()
	{
		return _sp;
	}
	
	public double getExpRate()
	{
		return _expRate;
	}
	
	public double getSpRate()
	{
		return _spRate;
	}

	public boolean isExpRateable()
	{
		return _rateableExp;
	}

	public boolean isSpRateable()
	{
		return _rateableSp;
	}
}