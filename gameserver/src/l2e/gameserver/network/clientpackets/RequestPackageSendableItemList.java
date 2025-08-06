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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.serverpackets.PackageSendableList;

public class RequestPackageSendableItemList extends GameClientPacket
{
	private int _objectID;
	
	@Override
	protected void readImpl()
	{
		_objectID = readD();
	}
	
	@Override
	public void runImpl()
	{
		ItemInstance[] items = getClient().getActiveChar().getInventory().getAvailableItems(true, true, true);
		sendPacket(new PackageSendableList(items, _objectID));
	}
}