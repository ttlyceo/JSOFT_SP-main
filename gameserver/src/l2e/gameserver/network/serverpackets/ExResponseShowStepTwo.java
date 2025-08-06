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

import java.util.Collection;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.petition.PetitionMainGroup;
import l2e.gameserver.model.petition.PetitionSection;

public class ExResponseShowStepTwo extends GameServerPacket
{
	private final String _lang;
	private final PetitionMainGroup _petitionMainGroup;
	
	public ExResponseShowStepTwo(Player player, PetitionMainGroup gr)
	{
		_lang = player.getLang();
		_petitionMainGroup = gr;
	}
	
	@Override
	protected void writeImpl()
	{
		final Collection<PetitionSection> sections = _petitionMainGroup.getSubGroups();
		writeD(sections.size());
		writeS(_petitionMainGroup.getDescription(_lang));
		for (final PetitionSection g : sections)
		{
			writeC(g.getId());
			writeS(g.getName(_lang));
		}
	}
}