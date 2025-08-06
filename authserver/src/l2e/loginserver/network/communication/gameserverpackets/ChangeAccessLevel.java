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
package l2e.loginserver.network.communication.gameserverpackets;

import l2e.loginserver.accounts.Account;
import l2e.loginserver.network.communication.ReceivablePacket;

public class ChangeAccessLevel extends ReceivablePacket
{
	private String _account;
	private int _level;
	private int _banExpire;
	
	@Override
	protected void readImpl()
	{
		_account = readS();
		_level = readD();
		_banExpire = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Account acc = new Account(_account);
		acc.restore();
		acc.setAccessLevel(_level);
		acc.setBanExpire(_banExpire);
		acc.update();
	}
}
