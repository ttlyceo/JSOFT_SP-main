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

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.petition.PetitionMainGroup;
import l2e.gameserver.model.petition.PetitionSection;
import l2e.gameserver.network.serverpackets.ExResponseShowContents;

public class RequestExShowStepThree extends GameClientPacket
{
	private int _subId;

	@Override
	protected void readImpl()
	{
		_subId = readC();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}

		final PetitionMainGroup group = player.getPetitionGroup();
		if (group == null)
		{
			return;
		}
		
		final PetitionSection section = group.getSubGroup(_subId);
		if (section == null)
		{
			return;
		}
		
		player.sendPacket(new ExResponseShowContents(section.getDescription(player.getLang())));
	}
}