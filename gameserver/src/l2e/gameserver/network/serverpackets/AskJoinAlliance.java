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

public class AskJoinAlliance extends GameServerPacket
{
	private final String _requestorName;
	private final String _requestorAllyName;
	private final int _requestorObjId;

	public AskJoinAlliance(int requestorObjId, String requestorName, String requestorAllyName)
	{
		_requestorName = requestorName;
		_requestorAllyName = requestorAllyName;
		_requestorObjId = requestorObjId;
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_requestorObjId);
		writeS(_requestorName);
		writeS("");
		writeS(_requestorAllyName);
	}
}