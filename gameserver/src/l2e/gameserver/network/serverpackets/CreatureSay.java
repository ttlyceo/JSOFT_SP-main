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

import java.util.ArrayList;
import java.util.List;

import l2e.commons.util.ArabicUtilities;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;

public final class CreatureSay extends GameServerPacket
{
	private final int _objectId;
	private final int _textType;
	private String _charName = null;
	private int _charId = 0;
	private String _text = null;
	private int _npcString = -1;
	private List<String> _parameters;
	
	public CreatureSay(int objectId, int messageType, String charName, String text)
	{
		_objectId = objectId;
		_textType = messageType;
		_charName = charName;
		_text = text;
	}

	public CreatureSay(int objectId, int messageType, int charId, NpcStringId npcString)
	{
		_objectId = objectId;
		_textType = messageType;
		_charId = charId;
		_npcString = npcString.getId();
	}

	public CreatureSay(int objectId, int messageType, String charName, NpcStringId npcString)
	{
		_objectId = objectId;
		_textType = messageType;
		_charName = charName;
		_npcString = npcString.getId();
	}

	public CreatureSay(int objectId, int messageType, int charId, SystemMessageId sysString)
	{
		_objectId = objectId;
		_textType = messageType;
		_charId = charId;
		_npcString = sysString.getId();
	}

	public void addStringParameter(String text)
	{
		if (_parameters == null)
		{
			_parameters = new ArrayList<>();
		}
		_parameters.add(text);
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_objectId);
		writeD(_textType);
		if (_charName != null)
		{
			writeS(_charName);
		}
		else
		{
			writeD(_charId);
		}
		writeD(_npcString);
		if (_text != null)
		{
			if (ArabicUtilities.hasArabicLetters(_text))
			{
				final String ttext = ArabicUtilities.reshapeSentence(_text);
				writeS(ttext);
			}
			else
			{
				writeS(_text);
			}
		}
		else if (_parameters != null)
		{
			for (final String s : _parameters)
			{
				writeS(s);
			}
		}
		
		final Player player = getClient().getActiveChar();
		if (player != null)
		{
			player.broadcastSnoop(_textType, _charName, _text);
		}
	}
}