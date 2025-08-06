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

import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.service.BotFunctions;
import l2e.gameserver.model.zone.ZoneType;

public class Warpgate extends Quest
{
	public Warpgate(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(32314, 32315, 32316, 32317, 32318, 32319);
		addFirstTalkId(32314, 32315, 32316, 32317, 32318, 32319);
		addTalkId(32314, 32315, 32316, 32317, 32318, 32319);
		
		addEnterZoneId(40101);
	}
	
	private static final boolean canEnter(Player player)
	{
		if (player.isFlying())
		{
			return false;
		}

		if (Config.ENTER_HELLBOUND_WITHOUT_QUEST || player.isInFightEvent() || player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS) || player.isInVehicle())
		{
			return true;
		}

		QuestState st;
		if (!HellboundManager.getInstance().isLocked())
		{
			st = player.getQuestState("_130_PathToHellbound");
			if ((st != null) && st.isCompleted())
			{
				return true;
			}
		}
		
		st = player.getQuestState("_133_ThatsBloodyHot");
		return ((st != null) && st.isCompleted());
	}
	
	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		if (!canEnter(player))
		{
			if (HellboundManager.getInstance().isLocked())
			{
				return "warpgate-locked.htm";
			}
		}
		
		return npc.getId() + ".htm";
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		if (!canEnter(player))
		{
			return "warpgate-no.htm";
		}
		
		if (BotFunctions.getInstance().isAllowLicence() && player.isInParty())
		{
			if (player.getParty().isLeader(player) && player.getVarB("autoTeleport@", false))
			{
				for (final Player member : player.getParty().getMembers())
				{
					if (member != null)
					{
						if (member.getObjectId() == player.getObjectId())
						{
							continue;
						}
						
						if (!Util.checkIfInRange(1000, player, member, true) || !canEnter(member))
						{
							continue;
						}
						
						if (!BotFunctions.checkCondition(member, false) || !member.getIPAddress().equalsIgnoreCase(player.getIPAddress()))
						{
							continue;
						}
						member.teleToLocation(-11272, 236464, -3248, true, member.getReflection());
					}
				}
			}
		}
		player.teleToLocation(-11272, 236464, -3248, true, player.getReflection());
		HellboundManager.getInstance().unlock();
		return null;
	}
	
	@Override
	public final String onEnterZone(Creature character, ZoneType zone)
	{
		if (character.isPlayer())
		{
			if (!canEnter(character.getActingPlayer()))
			{
				ThreadPoolManager.getInstance().schedule(new Teleport(character), 1000);
			}
			else if (!((Player) character).isMinimapAllowed())
			{
				if (character.getInventory().getItemByItemId(9994) != null)
				{
					((Player) character).setMinimapAllowed(true);
				}
			}
		}
		return null;
	}
	
	private static final class Teleport implements Runnable
	{
		private final Creature _char;
		
		public Teleport(Creature c)
		{
			_char = c;
		}
		
		@Override
		public void run()
		{
			try
			{
				_char.teleToLocation(-16555, 209375, -3670, true, _char.getReflection());
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args)
	{
		new Warpgate(-1, Warpgate.class.getSimpleName(), "teleports");
	}
}