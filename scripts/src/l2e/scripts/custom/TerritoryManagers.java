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

import l2e.gameserver.data.parser.MultiSellParser;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.QuestManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.Race;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.olympiad.Olympiad;
import l2e.gameserver.model.quest.Quest;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.updatetype.UserInfoType;
import l2e.scripts.ai.AbstractNpcAI;

public class TerritoryManagers extends AbstractNpcAI
{
	private static final int[] preciousSoul1ItemIds =
	{
	        7587, 7588, 7589, 7597, 7598, 7599
	};
	
	private static final int[] preciousSoul2ItemIds =
	{
	        7595
	};
	
	private static final int[] preciousSoul3ItemIds =
	{
	        7678, 7591, 7592, 7593
	};
	
	public TerritoryManagers()
	{
		super(TerritoryManagers.class.getSimpleName(), "custom");
		
		for (int i = 0; i < 9; i++)
		{
			addFirstTalkId(36490 + i);
			addTalkId(36490 + i);
			addStartNpc(36490 + i);
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if ((player.getClassId().level() < 2) || (player.getLevel() < 40))
		{
			return "36490-08.htm";
		}
		return npc.getId() + ".htm";
	}
	
	@Override
	@Deprecated
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		
		String htmltext = null;
		final int npcId = npc.getId();
		final int itemId = 13757 + (npcId - 36490);
		final int territoryId = 81 + (npcId - 36490);
		
		switch (event)
		{
			case "36490-04.htm" :
			{
				html.setFile(player, player.getLang(), "data/html/scripts/custom/TerritoryManagers/36490-04.htm");
				html.replace("%badge%", String.valueOf(TerritoryWarManager.MINTWBADGEFORNOBLESS));
				player.sendPacket(html);
				break;
			}
			case "BuyProducts" :
			{
				if (player.getInventory().getItemByItemId(itemId) != null)
				{
					final int multiSellId = 364900001 + ((npcId - 36490) * 10000);
					MultiSellParser.getInstance().separateAndSend(multiSellId, player, npc, false);
				}
				else
				{
					htmltext = "36490-02.htm";
				}
				break;
			}
			case "MakeMeNoble" :
			{
				if (player.getInventory().getInventoryItemCount(itemId, -1) < TerritoryWarManager.MINTWBADGEFORNOBLESS)
				{
					htmltext = "36490-02.htm";
				}
				else if (player.isNoble())
				{
					htmltext = "36490-05.htm";
				}
				else if (player.getLevel() < 75)
				{
					htmltext = "36490-06.htm";
				}
				else
				{
					processNoblesseQuest(player, 241, preciousSoul1ItemIds);
					processNoblesseQuest(player, 242, preciousSoul2ItemIds);
					processNoblesseQuest(player, 246, preciousSoul3ItemIds);
					processNoblesseQuest(player, 247, null);
					
					player.destroyItemByItemId(event, itemId, TerritoryWarManager.MINTWBADGEFORNOBLESS, npc, true);
					player.addItem(event, 7694, 1, npc, true);
					Olympiad.addNoble(player);
					player.setNoble(true);
					if (player.getClan() != null)
					{
						player.setPledgeClass(ClanMember.calculatePledgeClass(player));
					}
					else
					{
						player.setPledgeClass(5);
					}
					player.sendUserInfo(UserInfoType.SOCIAL, UserInfoType.CLAN);
					final Quest q = QuestManager.getInstance().getQuest((player.getRace() == Race.Kamael) ? 236 : 235);
					if (q != null)
					{
						QuestState qs = player.getQuestState(q.getName());
						if (qs == null)
						{
							qs = q.newQuestState(player);
							qs.setState(State.STARTED);
						}
						qs.exitQuest(false);
					}
					deleteIfExist(player, 7678, event, npc);
					deleteIfExist(player, 7679, event, npc);
					deleteIfExist(player, 5011, event, npc);
					deleteIfExist(player, 1239, event, npc);
					deleteIfExist(player, 1246, event, npc);
				}
				break;
			}
			case "CalcRewards" :
			{
				final int[] reward = TerritoryWarManager.getInstance().calcReward(player);
				if (TerritoryWarManager.getInstance().isTWInProgress() || (reward[0] == 0))
				{
					html.setFile(player, player.getLang(), "data/html/scripts/custom/TerritoryManagers/reward-0a.htm");
				}
				else if (reward[0] != territoryId)
				{
					html.setFile(player, player.getLang(), "data/html/scripts/custom/TerritoryManagers/reward-0b.htm");
					html.replace("%castle%", CastleManager.getInstance().getCastleById(reward[0] - 80).getName(player.getLang()));
				}
				else if (reward[1] == 0)
				{
					html.setFile(player, player.getLang(), "data/html/scripts/custom/TerritoryManagers/reward-0a.htm");
				}
				else
				{
					html.setFile(player, player.getLang(), "data/html/scripts/custom/TerritoryManagers/reward-1.htm");
					html.replace("%castle%", CastleManager.getInstance().getCastleById(reward[0] - 80).getName(player.getLang()));
					html.replace("%badge%", String.valueOf(reward[1]));
					html.replace("%adena%", String.valueOf(reward[1] * 5000));
				}
				html.replace("%territoryId%", String.valueOf(territoryId));
				html.replace("%objectId%", String.valueOf(npc.getObjectId()));
				player.sendPacket(html);
				player.sendActionFailed();
				break;
			}
			case "ReceiveRewards" :
			{
				int badgeId = 57;
				if (TerritoryWarManager.TERRITORY_ITEM_IDS.containsKey(territoryId))
				{
					badgeId = TerritoryWarManager.TERRITORY_ITEM_IDS.get(territoryId);
				}
				final int[] reward = TerritoryWarManager.getInstance().calcReward(player);
				if (TerritoryWarManager.getInstance().isTWInProgress() || (reward[0] == 0))
				{
					html.setFile(player, player.getLang(), "data/html/scripts/custom/TerritoryManagers/reward-0a.htm");
				}
				else if (reward[0] != territoryId)
				{
					html.setFile(player, player.getLang(), "data/html/scripts/custom/TerritoryManagers/reward-0b.htm");
					html.replace("%castle%", CastleManager.getInstance().getCastleById(reward[0] - 80).getName(player.getLang()));
				}
				else if (reward[1] == 0)
				{
					html.setFile(player, player.getLang(), "data/html/scripts/custom/TerritoryManagers/reward-0a.htm");
				}
				else
				{
					html.setFile(player, player.getLang(), "data/html/scripts/custom/TerritoryManagers/reward-2.htm");
					player.addItem("ReceiveRewards", badgeId, reward[1], npc, true);
					player.addAdena("ReceiveRewards", reward[1] * 5000, npc, true);
					TerritoryWarManager.getInstance().resetReward(player);
				}
				html.replace("%objectId%", String.valueOf(npc.getObjectId()));
				player.sendPacket(html);
				player.sendActionFailed();
				break;
			}
			default :
			{
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	private static void processNoblesseQuest(Player player, int questId, int[] itemIds)
	{
		final Quest q = QuestManager.getInstance().getQuest(questId);
		if (q == null)
		{
			return;
		}
		
		QuestState qs = player.getQuestState(q.getName());
		if (qs == null)
		{
			qs = q.newQuestState(player);
			qs.setState(State.STARTED);
		}
		
		if (!qs.isCompleted())
		{
			if (itemIds != null)
			{
				for (final int itemId : itemIds)
				{
					qs.takeItems(itemId, -1);
				}
			}
			qs.exitQuest(false);
		}
	}
	
	private static void deleteIfExist(Player player, int itemId, String event, Npc npc)
	{
		final ItemInstance item = player.getInventory().getItemByItemId(itemId);
		if (item != null)
		{
			player.destroyItem(event, item, npc, true);
		}
	}
	
	public static void main(String[] args)
	{
		new TerritoryManagers();
	}
}
