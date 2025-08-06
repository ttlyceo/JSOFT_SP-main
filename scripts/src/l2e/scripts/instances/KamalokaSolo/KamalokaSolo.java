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
package l2e.scripts.instances.KamalokaSolo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import l2e.gameserver.Config;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.ReflectionParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.scripts.instances.AbstractReflection;

/**
 * Rework by LordWinter 13.02.2020
 */
public class KamalokaSolo extends AbstractReflection
{
	protected class KamaPlayer
	{
		public int instance = 0;
		public int killedKanabions = 0;
		public int killedDoplers = 0;
		public int killedVoiders = 0;
		public int grade = 0;
		public boolean rewarded = false;
	}
	
	public class KamaParam
	{
		public String qn = "KamalokaSolo";
		public int instanceId = 0;
		public Location rewPosition = null;
	}
	
	protected class KamaWorld extends ReflectionWorld
	{
		public Map<String, KamaPlayer> KamalokaPlayers = new HashMap<>();
		public KamaParam param = new KamaParam();
		
		public KamaWorld()
		{
		}
	}
	
	public KamalokaSolo(String name, String descr)
	{
		super(name, descr);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc, KamaParam param)
	{
		if (enterInstance(player, npc, new KamaWorld(), param.instanceId))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			final KamaPlayer kp = new KamaPlayer();
			kp.instance = world.getReflectionId();
			((KamaWorld) world).param = param;
			((KamaWorld) world).KamalokaPlayers.put(player.getName(null), kp);
			final Reflection ref = world.getReflection();
			if (ref != null)
			{
				final long time = ref.getParams().getLong("reflectTime") * 60000L;
				startQuestTimer("time", time, null, player);
			}
		}
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
		else
		{
			player.getAI().setIntention(CtrlIntention.IDLE);
			final Location teleLoc = template.getTeleportCoord();
			player.teleToLocation(teleLoc, true, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().getAI().setIntention(CtrlIntention.IDLE);
				player.getSummon().teleToLocation(teleLoc, true, world.getReflection());
			}
		}
	}
	
	public String onAdvEventTo(String event, Npc npc, Player player, String qn)
	{
		final QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			return null;
		}
		
		final var ref = player.getReflection();
		final String playerName = player.getName(null);
		
		Reflection instanceObj = null;
		KamaWorld world = null;
		Location rewPosition = null;
		if (ReflectionManager.getInstance().reflectionExist(ref.getId()))
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(ref.getId());
			if (tmpworld != null && tmpworld instanceof KamaWorld && !ref.isDefault())
			{
				world = (KamaWorld) tmpworld;
				instanceObj = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			}
			
			if (world == null)
			{
				return "";
			}
			rewPosition = world.param.rewPosition;
		}
		else
		{
			return "";
		}
		
		if (event.equalsIgnoreCase("time"))
		{
			if (!player.isOnline())
			{
				return null;
			}
			
			if (instanceObj != null)
			{
				instanceObj.setDuration(600000);
				instanceObj.cleanupNpcs();
			}
			
			addSpawn(32485, rewPosition.getX(), rewPosition.getY(), rewPosition.getZ(), 0, false, 0, false, ref);
			if (world == null || !world.KamalokaPlayers.containsKey(playerName))
			{
				return "";
			}
			
			final KamaPlayer kp = world.KamalokaPlayers.get(playerName);
			if (kp == null)
			{
				return null;
			}
			
			if (world != null && instanceObj != null)
			{
				final int count = kp.killedKanabions * 10 + kp.killedDoplers * 20 + kp.killedVoiders * 50;
				kp.grade = getRank(count, instanceObj.getParams());
				if (kp.grade > 0)
				{
					final ReflectionTemplate template = ReflectionParser.getInstance().getReflectionId(world.param.instanceId);
					if (template != null)
					{
						try (
						    Connection con = DatabaseFactory.getInstance().getConnection())
						{
							final int code = (template.getMinLevel() * 100) + template.getMaxLevel();
							final PreparedStatement statement = con.prepareStatement("INSERT INTO kamaloka_results (char_name,Level,Grade,Count) VALUES (?,?,?,?)");
							statement.setString(1, playerName);
							statement.setInt(2, code);
							statement.setInt(3, kp.grade);
							statement.setInt(4, count);
							statement.executeUpdate();
							statement.close();
						}
						catch (final Exception e)
						{
							_log.warn("Error while inserting Kamaloka data: " + e);
						}
					}
				}
			}
		}
		else if (event.equalsIgnoreCase("Reward"))
		{
			final KamaPlayer kp = world.KamalokaPlayers.get(playerName);
			if (instanceObj != null && kp != null)
			{
				if (!kp.rewarded)
				{
					kp.rewarded = true;
					final int[][] rewards = getRewardList(kp.grade, instanceObj.getParams());
					if (rewards != null)
					{
						for(final int[] item : rewards)
						{
							if(item != null)
							{
								final int id = item[0];
								final int count = item[1];
								if(id > 0 && count > 0)
								{
									st.giveItems(id, count);
								}
							}
						}
					}
					final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
					html.setFile(player, player.getLang(), "data/html/scripts/instances/KamalokaSolo/" + "1.htm");
					html.replace("%kamaloka%", qn);
					player.sendPacket(html);
				}
			}
		}
		else if (event.equalsIgnoreCase("Exit"))
		{
			if (instanceObj != null)
			{
				instanceObj.collapse();
			}
		}
		
		return null;
	}
	
	private static int[][] getRewardList(int rank, StatsSet params)
	{
		if (params == null || params.isEmpty())
		{
			return null;
		}
		
		final String rewards = params.getString("rewardLvl" + rank);
		if (rewards == null)
		{
			return null;
		}
		
		final String[] itemsList = rewards.split(";");
		final int rewardsCount = itemsList.length;
		final int[][] result = new int[rewardsCount][];
		for (int i = 0; i < rewardsCount; i++)
		{
			final String[] item = itemsList[i].split("-");
			if (item.length != 2)
			{
				continue;
			}
			final int[] it = new int[2];
			it[0] = Integer.parseInt(item[0]);
			it[1] = Integer.parseInt(item[1]);
			result[i] = it;
		}
		return result;
	}
	
	private static int getRank(int total, StatsSet params)
	{
		int rank = 0;
		if (params == null || params.isEmpty())
		{
			return rank;
		}
		
		for (int i = 1; i <= 5; i++)
		{
			final int points = params.getInteger("rankLvl" + i);
			if (rank < i && total >= points)
			{
				rank = i;
			}
		}
		return rank;
	}
	
	public String onEnterTo(Npc npc, Player player, KamaParam param)
	{
		QuestState st = player.getQuestState(param.qn);
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		if (Config.ALT_KAMALOKA_SOLO_PREMIUM_ONLY && !player.hasPremiumBonus())
		{
			if (npc != null)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, player.getLang(), "data/html/scripts/instances/KamalokaSolo/32484-no.htm");
				player.sendPacket(html);
			}
			else
			{
				player.sendMessage((new ServerMessage("ServiceBBS.ONLY_FOR_PREMIUM", player.getLang())).toString());
			}
			return null;
		}
		enterInstance(player, npc, param);
		return "";
	}
	
	public String onTalkTo(Npc npc, Player player, String qn)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (!(tmpworld instanceof KamaWorld))
		{
			return "";
		}
		
		final String playerName = player.getName(null);
		final KamaWorld world = (KamaWorld) tmpworld;
		final KamaPlayer kp = world.KamalokaPlayers.get(playerName);
		if (kp == null)
		{
			return "";
		}
		
		if (npc.getId() == 32485)
		{
			if (!world.KamalokaPlayers.containsKey(playerName))
			{
				return "";
			}
			
			String msgReward = "0.htm";
			if (!kp.rewarded)
			{
				switch (kp.grade)
				{
					case 1 :
						msgReward = "D.htm";
						break;
					case 2 :
						msgReward = "C.htm";
						break;
					case 3 :
						msgReward = "B.htm";
						break;
					case 4 :
						msgReward = "A.htm";
						break;
					case 5 :
						msgReward = "S.htm";
						break;
					default :
						msgReward = "1.htm";
						break;
				}
			}
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, player.getLang(), "data/html/scripts/instances/KamalokaSolo/" + msgReward);
			html.replace("%kamaloka%", qn);
			player.sendPacket(html);
		}
		return null;
	}
	
	public String onKillTo(Npc npc, Player player, boolean isPet, String qn, int KANABION, int[] APPEAR)
	{
		if (player == null)
		{
			return "";
		}
		
		QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (!(tmpworld instanceof KamaWorld))
		{
			return "";
		}
		
		final String playerName = player.getName(null);
		final KamaWorld world = (KamaWorld) tmpworld;
		
		if (!world.KamalokaPlayers.containsKey(playerName))
		{
			return "";
		}
		
		final KamaPlayer kp = world.KamalokaPlayers.get(playerName);
		
		final int npcId = npc.getId();
		if (npcId == KANABION)
		{
			kp.killedKanabions++;
		}
		else if (npcId == APPEAR[0])
		{
			kp.killedDoplers++;
		}
		else if (npcId == APPEAR[1])
		{
			kp.killedVoiders++;
		}
		return "";
	}
	
	public static void main(String[] args)
	{
		new KamalokaSolo("qn", "instance");
	}
}
