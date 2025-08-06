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

import l2e.gameserver.model.actor.templates.NpcWalkerTemplate;

public class WalkRoute
{
	private final String _name;
	private final List<NpcWalkerTemplate> _nodeList;
	private final boolean _repeatWalk;
	private boolean _stopAfterCycle;
	private final byte _repeatType;
	
	public WalkRoute(String name, List<NpcWalkerTemplate> route, boolean repeat, boolean once, byte repeatType)
	{
		_name = name;
		_nodeList = route;
		_repeatType = repeatType;
		_repeatWalk = ((_repeatType >= 0) && (_repeatType <= 2)) ? repeat : false;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public List<NpcWalkerTemplate> getNodeList()
	{
		return _nodeList;
	}
	
	public NpcWalkerTemplate getLastNode()
	{
		return _nodeList.get(_nodeList.size() - 1);
	}
	
	public boolean repeatWalk()
	{
		return _repeatWalk;
	}
	
	public boolean doOnce()
	{
		return _stopAfterCycle;
	}
	
	public byte getRepeatType()
	{
		return _repeatType;
	}
	
	public int getNodesCount()
	{
		return _nodeList.size();
	}
}