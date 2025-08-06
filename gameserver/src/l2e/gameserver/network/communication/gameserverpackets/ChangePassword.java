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
package l2e.gameserver.network.communication.gameserverpackets;

import l2e.gameserver.network.communication.SendablePacket;

public class ChangePassword extends SendablePacket
{
	public String _account;
	public String _oldPass;
	public String _newPass;
	public String _hwid;
	
	public ChangePassword(String account, String oldPass, String newPass, String hwid)
	{
		_account = account;
		_oldPass = oldPass;
		_newPass = newPass;
		_hwid = hwid;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0x08);
		writeS(_account);
		writeS(_oldPass);
		writeS(_newPass);
		writeS(_hwid);
	}
}
