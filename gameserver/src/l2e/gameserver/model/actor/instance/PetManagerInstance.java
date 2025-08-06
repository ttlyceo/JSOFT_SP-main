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
package l2e.gameserver.model.actor.instance;

import l2e.commons.util.Evolve;
import l2e.gameserver.Config;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class PetManagerInstance extends MerchantInstance
{
	public PetManagerInstance(int objectID, NpcTemplate template)
	{
		super(objectID, template);
		setInstanceType(InstanceType.PetManagerInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}

		return "data/html/petmanager/" + pom + ".htm";
	}

	@Override
	public void showChatWindow(Player player)
	{
		String filename = "data/html/petmanager/" + getId() + ".htm";

		if ((getId() == 36478) && player.hasSummon())
		{
			filename = "data/html/petmanager/restore-unsummonpet.htm";
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(player, player.getLang(), filename);
		if (Config.ALLOW_RENTPET && Config.LIST_PET_RENT_NPC.contains(getId()))
		{
			html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h npc_%objectId%_Quest");
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName(player.getLang()));
		player.sendPacket(html);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("exchange"))
		{
			final String[] params = command.split(" ");
			final int val = Integer.parseInt(params[1]);
			switch (val)
			{
				case 1 :
					exchange(player, 7585, 6650);
					break;
				case 2 :
					exchange(player, 7583, 6648);
					break;
				case 3 :
					exchange(player, 7584, 6649);
					break;
			}
			return;
		}
		else if (command.startsWith("evolve"))
		{
			final String[] params = command.split(" ");
			final int val = Integer.parseInt(params[1]);
			boolean ok = false;
			switch (val)
			{
				case 1 :
					ok = Evolve.doEvolve(player, this, 2375, 9882, 55);
					break;
				case 2 :
					ok = Evolve.doEvolve(player, this, 9882, 10426, 70);
					break;
				case 3 :
					ok = Evolve.doEvolve(player, this, 6648, 10311, 55);
					break;
				case 4 :
					ok = Evolve.doEvolve(player, this, 6650, 10313, 55);
					break;
				case 5 :
					ok = Evolve.doEvolve(player, this, 6649, 10312, 55);
					break;
			}
			if (!ok)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/petmanager/evolve_no.htm");
				player.sendPacket(html);
			}
			return;
		}
		else if (command.startsWith("restore"))
		{
			final String[] params = command.split(" ");
			final int val = Integer.parseInt(params[1]);
			boolean ok = false;
			switch (val)
			{
				case 1 :
					ok = Evolve.doRestore(player, this, 10307, 9882, 55);
					break;
				case 2 :
					ok = Evolve.doRestore(player, this, 10611, 10426, 70);
					break;
				case 3 :
					ok = Evolve.doRestore(player, this, 10308, 4422, 55);
					break;
				case 4 :
					ok = Evolve.doRestore(player, this, 10309, 4423, 55);
					break;
				case 5 :
					ok = Evolve.doRestore(player, this, 10310, 4424, 55);
					break;
			}
			if (!ok)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, player.getLang(), "data/html/petmanager/restore_no.htm");
				player.sendPacket(html);
			}
			return;
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	public final void exchange(Player player, int itemIdtake, int itemIdgive)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (player.destroyItemByItemId("Consume", itemIdtake, 1, this, true))
		{
			player.addItem("", itemIdgive, 1, this, true);
			html.setFile(player, player.getLang(), "data/html/petmanager/" + getId() + ".htm");
			player.sendPacket(html);
		}
		else
		{
			html.setFile(player, player.getLang(), "data/html/petmanager/exchange_no.htm");
			player.sendPacket(html);
		}
	}
}