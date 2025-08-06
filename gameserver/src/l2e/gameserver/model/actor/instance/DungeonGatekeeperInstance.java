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

import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.data.parser.TeleLocationParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.TeleportTemplate;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;

public class DungeonGatekeeperInstance extends NpcInstance
{
	public DungeonGatekeeperInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.DungeonGatekeeperInstance);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		player.sendActionFailed();
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();

		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		final int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		final int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
		final boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
		final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
		
		if (actualCommand.startsWith("necro"))
		{
			boolean canPort = true;
			if (!Config.ALLOW_UNLIM_ENTER_CATACOMBS)
			{
				if (isSealValidationPeriod)
				{
					if ((compWinner == SevenSigns.CABAL_DAWN) && ((playerCabal != SevenSigns.CABAL_DAWN) || (sealAvariceOwner != SevenSigns.CABAL_DAWN)))
					{
						player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
						canPort = false;
					}
					else if ((compWinner == SevenSigns.CABAL_DUSK) && ((playerCabal != SevenSigns.CABAL_DUSK) || (sealAvariceOwner != SevenSigns.CABAL_DUSK)))
					{
						player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
						canPort = false;
					}
					else if ((compWinner == SevenSigns.CABAL_NULL) && (playerCabal != SevenSigns.CABAL_NULL))
					{
						canPort = true;
					}
					else if (playerCabal == SevenSigns.CABAL_NULL)
					{
						canPort = false;
					}
				}
				else
				{
					if (playerCabal == SevenSigns.CABAL_NULL)
					{
						canPort = false;
					}
				}
			}
			
			if (!canPort)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				filename += "necro_no.htm";
				html.setFile(player, player.getLang(), filename);
				player.sendPacket(html);
			}
			else
			{
				doTeleport(player, Integer.parseInt(st.nextToken()));
				player.setIsIn7sDungeon(true);
			}
		}
		else if (actualCommand.startsWith("cata"))
		{
			boolean canPort = true;
			if (!Config.ALLOW_UNLIM_ENTER_CATACOMBS)
			{
				if (isSealValidationPeriod)
				{
					if ((compWinner == SevenSigns.CABAL_DAWN) && ((playerCabal != SevenSigns.CABAL_DAWN) || (sealGnosisOwner != SevenSigns.CABAL_DAWN)))
					{
						player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
						canPort = false;
					}
					else if ((compWinner == SevenSigns.CABAL_DUSK) && ((playerCabal != SevenSigns.CABAL_DUSK) || (sealGnosisOwner != SevenSigns.CABAL_DUSK)))
					{
						player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
						canPort = false;
					}
					else if ((compWinner == SevenSigns.CABAL_NULL) && (playerCabal != SevenSigns.CABAL_NULL))
					{
						canPort = true;
					}
					else if (playerCabal == SevenSigns.CABAL_NULL)
					{
						canPort = false;
					}
				}
				else
				{
					if (playerCabal == SevenSigns.CABAL_NULL)
					{
						canPort = false;
					}
				}
			}
			
			if (!canPort)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				filename += "cata_no.htm";
				html.setFile(player, player.getLang(), filename);
				player.sendPacket(html);
			}
			else
			{
				doTeleport(player, Integer.parseInt(st.nextToken()));
				player.setIsIn7sDungeon(true);
			}
		}
		else if (actualCommand.startsWith("exit"))
		{
			doTeleport(player, Integer.parseInt(st.nextToken()));
			player.setIsIn7sDungeon(false);
		}
		else if (actualCommand.startsWith("goto"))
		{
			doTeleport(player, Integer.parseInt(st.nextToken()));
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	private void doTeleport(Player player, int val)
	{
		final TeleportTemplate list = TeleLocationParser.getInstance().getTemplate(val);
		if (list != null)
		{
			if (player.isAlikeDead())
			{
				return;
			}
			
			player.teleToLocation(list.getLocation(), true, ReflectionManager.DEFAULT);
		}
		else
		{
			_log.warn("No teleport destination with id:" + val);
		}
		
		player.sendActionFailed();
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
		
		return "data/html/teleporter/" + pom + ".htm";
	}
}