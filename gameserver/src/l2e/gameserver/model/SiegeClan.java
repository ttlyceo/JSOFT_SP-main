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
package l2e.gameserver.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2e.gameserver.model.actor.Npc;

public class SiegeClan
{
	private int _clanId = 0;
	private final List<Npc> _flag = new CopyOnWriteArrayList<>();
	private int _numFlagsAdded = 0;
	private SiegeClanType _type;
	
	public enum SiegeClanType
	{
		OWNER,
		DEFENDER,
		ATTACKER,
		DEFENDER_PENDING
	}
	
	public SiegeClan(int clanId, SiegeClanType type)
	{
		_clanId = clanId;
		_type = type;
	}
	
	public int getNumFlags()
	{
		return _numFlagsAdded;
	}
	
	public void addFlag(Npc flag)
	{
		_numFlagsAdded++;
		getFlag().add(flag);
	}
	
	public boolean removeFlag(Npc flag)
	{
		if (flag == null)
		{
			return false;
		}
		final boolean ret = getFlag().remove(flag);

		if (ret)
		{
			while (getFlag().remove(flag))
			{
			}
		}
		flag.deleteMe();
		_numFlagsAdded--;
		return ret;
	}
	
	public void removeFlags()
	{
		for (final Npc flag : getFlag())
		{
			removeFlag(flag);
		}
	}
	
	public final int getClanId()
	{
		return _clanId;
	}
	
	public final List<Npc> getFlag()
	{
		return _flag;
	}
	
	public SiegeClanType getType()
	{
		return _type;
	}
	
	public void setType(SiegeClanType setType)
	{
		_type = setType;
	}
}