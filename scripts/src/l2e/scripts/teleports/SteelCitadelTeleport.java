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
package l2e.scripts.teleports;

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.EpicBossManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.CommandChannel;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.GrandBossInstance;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.zone.type.BossZone;
import l2e.scripts.ai.grandboss.Beleth;

public class SteelCitadelTeleport extends Quest
{
	public SteelCitadelTeleport(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(32376);
		addTalkId(32376);
		addSpawnId(32376);
	}
	
	private Quest belethAI()
	{
		return QuestManager.getInstance().getQuest(Beleth.class.getSimpleName());
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		startQuestTimer("despawn", 600000, npc, null);
		return null;
	}
	
	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if (npc == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("despawn") && npc.getId() == 32376)
		{
			npc.deleteMe();
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		switch (npc.getId())
		{
			case 32376 :
				if (belethAI() != null)
				{
					final int status = EpicBossManager.getInstance().getBossStatus(29118);
					
					if (status == 3)
					{
						return "32376-02.htm";
					}
					
					if (Config.BELETH_NO_CC)
					{
						final Party party = player.getParty() == null ? null : player.getParty();
						if ((party == null || party.getMemberCount() < 2) || (party.getLeader().getObjectId() != player.getObjectId()) || (party.getMemberCount() < Config.BELETH_MIN_PLAYERS))
						{
							return "32376-02a.htm";
						}
					}
					else
					{
						final CommandChannel channel = player.getParty() == null ? null : player.getParty().getCommandChannel();
						if ((channel == null) || (channel.getLeader().getObjectId() != player.getObjectId()) || (channel.getMemberCount() < Config.BELETH_MIN_PLAYERS))
						{
							return "32376-02a.htm";
						}
					}
					
					if (status > 0 && !Beleth.canTeleport())
					{
						return "32376-03.htm";
					}
					
					final BossZone zone = (BossZone) ZoneManager.getInstance().getZoneById(12018);
					if (zone != null)
					{
						if (status == 0)
						{
							EpicBossManager.getInstance().setBossStatus(29118, 1, true);
							final GrandBossInstance beleth = EpicBossManager.getInstance().getBoss(29118);
							belethAI().notifyEvent("waiting", beleth, player);
						}
						
						if (Config.BELETH_NO_CC)
						{
							if (player.getParty() != null)
							{
								for (final Player pl : player.getParty().getMembers())
								{
									if (pl.isInsideRadius(npc.getX(), npc.getY(), npc.getZ(), 3000, true, false))
									{
										zone.allowPlayerEntry(pl, 30);
										pl.teleToLocation(16342, 209557, -9352, true, pl.getReflection());
									}
								}
							}
						}
						else
						{
							for (final Party party : player.getParty().getCommandChannel().getPartys())
							{
								if (party == null || party.getMemberCount() < 2)
								{
									continue;
								}
								
								for (final Player pl : party.getMembers())
								{
									if (pl.isInsideRadius(npc.getX(), npc.getY(), npc.getZ(), 3000, true, false))
									{
										zone.allowPlayerEntry(pl, 30);
										pl.teleToLocation(16342, 209557, -9352, true, pl.getReflection());
									}
								}
							}
						}
					}
				}
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new SteelCitadelTeleport(-1, SteelCitadelTeleport.class.getSimpleName(), "teleports");
	}
}