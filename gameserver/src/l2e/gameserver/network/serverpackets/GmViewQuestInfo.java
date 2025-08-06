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

public class GmViewQuestInfo extends GameServerPacket
{
	private final Player _activeChar;
	
	public GmViewQuestInfo(Player cha)
	{
		_activeChar = cha;
	}

	@Override
	protected final void writeImpl()
	{
		writeS(_activeChar.getName(null));
		final Quest[] questList = _activeChar.getAllActiveQuests();
		if (questList.length == 0)
		{
			writeC(0x00);
			writeH(0x00);
			writeH(0x00);
			return;
		}
		writeH(questList.length);
		for (final Quest q : questList)
		{
			writeD(q.getId());
			final QuestState qs = _activeChar.getQuestState(q.getName());
			if (qs == null)
			{
				writeD(0x00);
				continue;
			}
			writeD(qs.getInt("cond"));
		}
	}
}