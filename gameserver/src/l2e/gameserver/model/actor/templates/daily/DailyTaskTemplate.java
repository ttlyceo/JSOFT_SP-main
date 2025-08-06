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
package l2e.gameserver.model.actor.templates.daily;

import java.util.Map;

/**
 * Created by LordWinter
 */
public class DailyTaskTemplate
{
	private final int _id;
	private final String _type;
	private final String _sort;
	private final String _name;
	private final String _image;
	private final String _descr;
	private final boolean _isForAll;
	private Map<Integer, Long> _rewards;
	private int _npcId;
	private int _npcCount;
	private int _questId;
	private int _reflectionId;
	private int _pvpCount;
	private int _pkCount;
	private int _olyMatchCount;
	private int _eventsCount;
	private boolean _isSiegeFort;
	private boolean _isSiegeCastle;
	private boolean _isAttack;
	
	public DailyTaskTemplate(int id, String type, String sort, String name, String descr, String image, boolean isForAll)
	{
		_id = id;
		_type = type;
		_sort = sort;
		_name = name;
		_descr = descr;
		_image = image;
		_isForAll = isForAll;
	}
	
	public int getId()
	{
		return _id;
	}

	public String getType()
	{
		return _type;
	}
	
	public String getSort()
	{
		return _sort;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getImage()
	{
		return _image;
	}
	
	public boolean isForAll()
	{
		return _isForAll;
	}
	
	public String getDescr()
	{
		return _descr;
	}
	
	public void setRewards(Map<Integer, Long> rewards)
	{
		_rewards = rewards;
	}
	
	public Map<Integer, Long> getRewards()
	{
		return _rewards;
	}
	
	public void setNpcId(int npcId)
	{
		_npcId = npcId;
	}

	public int getNpcId()
	{
		return _npcId;
	}
	
	public void setNpcCount(int count)
	{
		_npcCount = count;
	}

	public int getNpcCount()
	{
		return _npcCount;
	}
	
	public void setQuestId(int questId)
	{
		_questId = questId;
	}

	public int getQuestId()
	{
		return _questId;
	}
	
	public void setReflectionId(int reflectionId)
	{
		_reflectionId = reflectionId;
	}

	public int getReflectionId()
	{
		return _reflectionId;
	}
	
	public void setPvpCount(int pvpCount)
	{
		_pvpCount = pvpCount;
	}

	public int getPvpCount()
	{
		return _pvpCount;
	}
	
	public void setPkCount(int pkCount)
	{
		_pkCount = pkCount;
	}

	public int getPkCount()
	{
		return _pkCount;
	}
	
	public void setOlyMatchCount(int count)
	{
		_olyMatchCount = count;
	}

	public int getOlyMatchCount()
	{
		return _olyMatchCount;
	}
	
	public void setEventsCount(int count)
	{
		_eventsCount = count;
	}

	public int getEventsCount()
	{
		return _eventsCount;
	}
	
	public void setSiegeFort(boolean fort)
	{
		_isSiegeFort = fort;
	}

	public boolean getSiegeFort()
	{
		return _isSiegeFort;
	}
	
	public void setSiegeCastle(boolean castle)
	{
		_isSiegeCastle = castle;
	}

	public boolean getSiegeCastle()
	{
		return _isSiegeCastle;
	}

	public void isAttack(boolean isAttack)
	{
		_isAttack = isAttack;
	}
	
	public boolean isAttackSiege()
	{
		return _isAttack;
	}
}