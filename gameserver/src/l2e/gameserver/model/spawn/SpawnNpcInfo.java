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
package l2e.gameserver.model.spawn;

import l2e.commons.collections.MultiValueSet;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;

public class SpawnNpcInfo
{
	private final NpcTemplate _template;
	private final int _max;
	private final MultiValueSet<String> _parameters;
	private final int _npcId;

	public SpawnNpcInfo(int npcId, int max, MultiValueSet<String> set)
	{
		_template = NpcsParser.getInstance().getTemplate(npcId);
		_max = max;
		_parameters = set;
		_npcId = npcId;
	}

	public NpcTemplate getTemplate()
	{
		return _template;
	}
	
	public int getId()
	{
		return _npcId;
	}

	public int getMax()
	{
		return _max;
	}

	public MultiValueSet<String> getParameters()
	{
		return _parameters;
	}
}
