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

import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.holder.PetNameHolder;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.network.SystemMessageId;

public final class RequestChangePetName extends GameClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final Summon pet = activeChar.getSummon();
		if (pet == null)
		{
			return;
		}
		
		if (!pet.isPet())
		{
			activeChar.sendPacket(SystemMessageId.DONT_HAVE_PET);
			return;
		}
		
		final String name = pet.getName(activeChar.getLang());
		if (name != null && !name.isEmpty())
		{
			activeChar.sendPacket(SystemMessageId.NAMING_YOU_CANNOT_SET_NAME_OF_THE_PET);
			return;
		}
		
		if (PetNameHolder.getInstance().doesPetNameExist(_name, pet.getTemplate().getId()) || CharNameHolder.getInstance().doesCharNameExist(_name))
		{
			activeChar.sendPacket(SystemMessageId.NAMING_ALREADY_IN_USE_BY_ANOTHER_PET);
			return;
		}
		
		if ((_name.length() < 3) || (_name.length() > 16))
		{
			activeChar.sendMessage("Your pet's name can be up to 16 characters in length.");
			return;
		}
		
		if (!PetNameHolder.getInstance().isValidPetName(_name))
		{
			activeChar.sendPacket(SystemMessageId.NAMING_PETNAME_CONTAINS_INVALID_CHARS);
			return;
		}
		pet.setGlobalName(_name);
		pet.updateAndBroadcastStatus(1);
	}
}