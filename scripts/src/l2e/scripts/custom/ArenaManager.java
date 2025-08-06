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
package l2e.scripts.custom;

import l2e.gameserver.data.parser.CategoryParser;
import l2e.gameserver.model.CategoryType;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.scripts.ai.AbstractNpcAI;

public class ArenaManager extends AbstractNpcAI
{
	private static final int[] ARENA_MANAGER =
	{
	                31226,
	                31225
	};

	private static final int[][] BUFFS = new int[][]
	{
	        {
	                6803, 0
			},
			{
			        6804, 2
			},
			{
			        6805, 1
			},
			{
			        6806, 1
			},
			{
			        6807, 1
			},
			{
			        6808, 2
			},
			{
			        6809, 0
			},
			{
			        6811, 0
			},
			{
			        6812, 2
			}
	};

	private static final SkillHolder CP_RECOVERY = new SkillHolder(4380, 1);
	private static final SkillHolder HP_RECOVERY = new SkillHolder(6817, 1);

	private static final int CP_COST = 1000;
	private static final int HP_COST = 1000;
	private static final int BUFF_COST = 2000;

	private ArenaManager()
	{
		super(ArenaManager.class.getSimpleName(), "custom");

		addStartNpc(ARENA_MANAGER);
		addTalkId(ARENA_MANAGER);
		addFirstTalkId(ARENA_MANAGER);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "CPrecovery":
			{
				if (player.getAdena() >= CP_COST)
				{
					takeItems(player, PcInventory.ADENA_ID, CP_COST);
					startQuestTimer("CPrecovery_delay", 2000, npc, player);
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				}
				break;
			}
			case "CPrecovery_delay":
			{
				if ((player != null) && !player.isInsideZone(ZoneId.PVP))
				{
					npc.setTarget(player);
					npc.doCast(CP_RECOVERY.getSkill());
				}
				break;
			}
			case "HPrecovery":
			{
				if (player.getAdena() >= HP_COST)
				{
					takeItems(player, PcInventory.ADENA_ID, HP_COST);
					startQuestTimer("HPrecovery_delay", 2000, npc, player);
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				}
				break;
			}
			case "HPrecovery_delay":
			{
				if ((player != null) && !player.isInsideZone(ZoneId.PVP))
				{
					npc.setTarget(player);
					npc.doCast(HP_RECOVERY.getSkill());
				}
				break;
			}
			case "Buff":
			{
				if (player.getAdena() >= BUFF_COST)
				{
					takeItems(player, PcInventory.ADENA_ID, BUFF_COST);
					npc.setTarget(player);
					final boolean isMage = CategoryParser.getInstance().isInCategory(CategoryType.MAGE_GROUP, player.getClassId().getId());
					for (final int[] buff : BUFFS)
					{
						if (isMage)
						{
							if (buff[1] == 0)
							{
								continue;
							}
							npc.doCast(new SkillHolder(buff[0], 1).getSkill());
						}
						else
						{
							if (buff[1] == 1)
							{
								continue;
							}
							npc.doCast(new SkillHolder(buff[0], 1).getSkill());
						}
					}
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				}
				break;
			}
		}
		return null;
	}

	public static void main(String[] args)
	{
		new ArenaManager();
	}
}
