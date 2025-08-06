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
package l2e.gameserver.network.serverpackets;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;

public class QuestList extends GameServerPacket
{
	private final Quest[] _quests;
	private final Player _activeChar;
	
	public QuestList(Player player)
	{
		_activeChar = player;
		_quests = player.getAllActiveQuests();
	}

	@Override
	protected final void writeImpl()
	{
		if (_quests != null)
		{
			writeH(_quests.length);
			for (final Quest q : _quests)
			{
				writeD(q.getId());
				final QuestState qs = _activeChar.getQuestState(q.getName());
				if (qs == null)
				{
					writeD(0);
					continue;
				}
				final int states = qs.getInt("__compltdStateFlags");
				if (states != 0)
				{
					writeD(states);
				}
				else
				{
					writeD(qs.getInt("cond"));
				}
			}
		}
		else
		{
			writeH(0x00);
		}
		writeB(new byte[128]);
	}
}