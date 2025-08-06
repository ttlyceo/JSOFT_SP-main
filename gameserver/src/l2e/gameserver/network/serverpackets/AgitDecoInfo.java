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

import l2e.gameserver.model.entity.ClanHall;
import l2e.gameserver.model.entity.ClanHall.ClanHallFunction;
import l2e.gameserver.model.entity.clanhall.AuctionableHall;

public class AgitDecoInfo extends GameServerPacket
{
	private final AuctionableHall _clanHall;
	private ClanHallFunction _function;
	
	public AgitDecoInfo(AuctionableHall ClanHall)
	{
		_clanHall = ClanHall;
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_clanHall.getId());
		_function = _clanHall.getFunction(ClanHall.FUNC_RESTORE_HP);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0x00);
		}
		else if ((_clanHall.getGrade() == 0 && _function.getLvl() < 220) || (_clanHall.getGrade() == 1 && _function.getLvl() < 160) || (_clanHall.getGrade() == 2 && _function.getLvl() < 260) || (_clanHall.getGrade() == 3 && _function.getLvl() < 300))
		{
			writeC(0x01);
		}
		else
		{
			writeC(0x02);
		}
		_function = _clanHall.getFunction(ClanHall.FUNC_RESTORE_MP);

		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0x00);
			writeC(0x00);
		}
		else if (((_clanHall.getGrade() == 0 || _clanHall.getGrade() == 1) && _function.getLvl() < 25) || (_clanHall.getGrade() == 2 && _function.getLvl() < 30) || (_clanHall.getGrade() == 3 && _function.getLvl() < 40))
		{
			writeC(0x01);
			writeC(0x01);
		}
		else
		{
			writeC(0x02);
			writeC(0x02);
		}
		_function = _clanHall.getFunction(ClanHall.FUNC_RESTORE_EXP);

		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0x00);
		}
		else if ((_clanHall.getGrade() == 0 && _function.getLvl() < 25) || (_clanHall.getGrade() == 1 && _function.getLvl() < 30) || (_clanHall.getGrade() == 2 && _function.getLvl() < 40) || (_clanHall.getGrade() == 3 && _function.getLvl() < 50))
		{
			writeC(0x01);
		}
		else
		{
			writeC(0x02);
		}
		_function = _clanHall.getFunction(ClanHall.FUNC_TELEPORT);

		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0x00);
		}
		else if (_function.getLvl() < 2)
		{
			writeC(0x01);
		}
		else
		{
			writeC(0x02);
		}
		writeC(0x00);
		_function = _clanHall.getFunction(ClanHall.FUNC_DECO_CURTAINS);

		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0x00);
		}
		else if (_function.getLvl() <= 1)
		{
			writeC(0x01);
		}
		else
		{
			writeC(0x02);
		}
		_function = _clanHall.getFunction(ClanHall.FUNC_ITEM_CREATE);

		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0x00);
		}
		else if ((_clanHall.getGrade() == 0 && _function.getLvl() < 2) || _function.getLvl() < 3)
		{
			writeC(0x01);
		}
		else
		{
			writeC(0x02);
		}
		_function = _clanHall.getFunction(ClanHall.FUNC_SUPPORT);

		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0x00);
			writeC(0x00);
		}
		else if ((_clanHall.getGrade() == 0 && _function.getLvl() < 2) || (_clanHall.getGrade() == 1 && _function.getLvl() < 4) || (_clanHall.getGrade() == 2 && _function.getLvl() < 5) || (_clanHall.getGrade() == 3 && _function.getLvl() < 8))
		{
			writeC(0x01);
			writeC(0x01);
		}
		else
		{
			writeC(0x02);
			writeC(0x02);
		}
		_function = _clanHall.getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM);

		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0x00);
		}
		else if (_function.getLvl() <= 1)
		{
			writeC(0x01);
		}
		else
		{
			writeC(0x02);
		}
		_function = _clanHall.getFunction(ClanHall.FUNC_ITEM_CREATE);

		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0x00);
		}
		else if ((_clanHall.getGrade() == 0 && _function.getLvl() < 2) || _function.getLvl() < 3)
		{
			writeC(0x01);
		}
		else
		{
			writeC(0x02);
		}
		writeD(0x00);
		writeD(0x00);
	}
}