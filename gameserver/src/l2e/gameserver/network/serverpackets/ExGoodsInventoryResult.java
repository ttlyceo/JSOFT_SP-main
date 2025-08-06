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

public class ExGoodsInventoryResult extends GameServerPacket
{
	public static GameServerPacket NOTHING = new ExGoodsInventoryResult(1);
	public static GameServerPacket SUCCESS = new ExGoodsInventoryResult(2);
	public static GameServerPacket ERROR = new ExGoodsInventoryResult(-1);
	public static GameServerPacket TRY_AGAIN_LATER = new ExGoodsInventoryResult(-2);
	public static GameServerPacket INVENTORY_FULL = new ExGoodsInventoryResult(-3);
	public static GameServerPacket NOT_CONNECT_TO_PRODUCT_SERVER = new ExGoodsInventoryResult(-4);
	public static GameServerPacket CANT_USE_AT_TRADE_OR_PRIVATE_SHOP = new ExGoodsInventoryResult(-5);
	public static GameServerPacket NOT_EXISTS = new ExGoodsInventoryResult(-6);
	public static GameServerPacket TO_MANY_USERS_TRY_AGAIN_INVENTORY = new ExGoodsInventoryResult(-101);
	public static GameServerPacket TO_MANY_USERS_TRY_AGAIN = new ExGoodsInventoryResult(-102);
	public static GameServerPacket PREVIOS_REQUEST_IS_NOT_COMPLETE = new ExGoodsInventoryResult(-103);
	public static GameServerPacket NOTHING2 = new ExGoodsInventoryResult(-104);
	public static GameServerPacket ALREADY_RETRACTED = new ExGoodsInventoryResult(-105);
	public static GameServerPacket ALREADY_RECIVED = new ExGoodsInventoryResult(-106);
	public static GameServerPacket PRODUCT_CANNOT_BE_RECEIVED_AT_CURRENT_SERVER = new ExGoodsInventoryResult(-107);
	public static GameServerPacket PRODUCT_CANNOT_BE_RECEIVED_AT_CURRENT_PLAYER = new ExGoodsInventoryResult(-108);

	private final int _result;

	public ExGoodsInventoryResult(int result)
	{
		_result = result;
	}

	@Override
	protected void writeImpl()
	{
		writeD(_result);
	}
}
