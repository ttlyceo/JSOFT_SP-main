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
package l2e.scripts.instances;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.GameTimeController;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.NpcStringId;

/**
 * Rework by LordWinter 06.02.2020
 */
public class ZakenDay extends AbstractReflection
{
	private class ZakenDayWorld extends ReflectionWorld
	{
		private final List<Player> playersInside = new ArrayList<>();
		private long startTime = 0;
		private boolean _is83;
		private int zakenRoom;
		private int _blueFounded;
		private boolean _isSpawned;
	}
	
	private static final int[][] ROOM_SPAWN =
	{
	        {
	                54240, 220133, -3498, 1, 3, 4, 6
			},
			{
			        54240, 218073, -3498, 2, 5, 4, 7
			},
			{
			        55265, 219095, -3498, 4, 9, 6, 7
			},
			{
			        56289, 220133, -3498, 8, 11, 6, 9
			},
			{
			        56289, 218073, -3498, 10, 12, 7, 9
			},
			{
			        54240, 220133, -3226, 13, 15, 16, 18
			},
			{
			        54240, 218073, -3226, 14, 17, 16, 19
			},
			{
			        55265, 219095, -3226, 21, 16, 19, 18
			},
			{
			        56289, 220133, -3226, 20, 23, 21, 18
			},
			{
			        56289, 218073, -3226, 22, 24, 19, 21
			},
			{
			        54240, 220133, -2954, 25, 27, 28, 30
			},
			{
			        54240, 218073, -2954, 26, 29, 28, 31
			},
			{
			        55265, 219095, -2954, 33, 28, 31, 30
			},
			{
			        56289, 220133, -2954, 32, 35, 30, 33
			},
			{
			        56289, 218073, -2954, 34, 36, 31, 33
			}
	};
	
	private static final int[][] CANDLE_SPAWN =
	{
	        {
	                53313, 220133, -3498
			},
			{
			        53313, 218079, -3498
			},
			{
			        54240, 221045, -3498
			},
			{
			        54325, 219095, -3498
			},
			{
			        54240, 217155, -3498
			},
			{
			        55257, 220028, -3498
			},
			{
			        55257, 218172, -3498
			},
			{
			        56280, 221045, -3498
			},
			{
			        56195, 219095, -3498
			},
			{
			        56280, 217155, -3498
			},
			{
			        57215, 220133, -3498
			},
			{
			        57215, 218079, -3498
			},
			{
			        53313, 220133, -3226
			},
			{
			        53313, 218079, -3226
			},
			{
			        54240, 221045, -3226
			},
			{
			        54325, 219095, -3226
			},
			{
			        54240, 217155, -3226
			},
			{
			        55257, 220028, -3226
			},
			{
			        55257, 218172, -3226
			},
			{
			        56280, 221045, -3226
			},
			{
			        56195, 219095, -3226
			},
			{
			        56280, 217155, -3226
			},
			{
			        57215, 220133, -3226
			},
			{
			        57215, 218079, -3226
			},
			{
			        53313, 220133, -2954
			},
			{
			        53313, 218079, -2954
			},
			{
			        54240, 221045, -2954
			},
			{
			        54325, 219095, -2954
			},
			{
			        54240, 217155, -2954
			},
			{
			        55257, 220028, -2954
			},
			{
			        55257, 218172, -2954
			},
			{
			        56280, 221045, -2954
			},
			{
			        56195, 219095, -2954
			},
			{
			        56280, 217155, -2954
			},
			{
			        57215, 220133, -2954
			},
			{
			        57215, 218079, -2954
			},
	};
	
	public ZakenDay(String name, String descr)
	{
		super(name, descr);
		
		addStartNpc(32713);
		addTalkId(32713);
		addFirstTalkId(32705);
		addKillId(29176, 29181);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc, boolean is83)
	{
		if (enterInstance(player, npc, new ZakenDayWorld(), is83 ? 135 : 133))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((ZakenDayWorld) world).startTime = System.currentTimeMillis();
			((ZakenDayWorld) world)._is83 = world.getTemplateId() == 135;
			spawnCandles(world);
		}
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			((ZakenDayWorld) world).playersInside.add(player);
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
	
	@Override
	protected boolean checkConditions(Player player, Npc npc, ReflectionTemplate template)
	{
		final boolean checkTime = template.getParams().getBool("checkValidTime");
		if (checkTime)
		{
			if (getTimeHour() <= 4)
			{
				player.sendMessage((new ServerMessage("Zaken.INVALID_TIME", player.getLang())).toString());
				return false;
			}
		}
		return super.checkConditions(player, npc, template);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("60"))
		{
			enterInstance(player, npc, false);
		}
		else if (event.equalsIgnoreCase("83"))
		{
			enterInstance(player, npc, true);
		}
		else
		{
			final ReflectionWorld tmpWorld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if (tmpWorld != null && tmpWorld instanceof ZakenDayWorld)
			{
				final ZakenDayWorld world = (ZakenDayWorld) tmpWorld;
				
				if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
				{
					return null;
				}
				
				final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
				if (inst != null)
				{
					final long burnDelay = inst.getParams().getLong("burnCandleDelay") * 1000L;
					final long zakenDelay = inst.getParams().getLong("zakenSpawnDelay") * 1000L;
					switch (event)
					{
						case "burn_good_candle" :
							if (npc.getRightHandItem() == 0)
							{
								npc.setRHandId(15280);
								npc.setDisplayEffect(1);
								startQuestTimer("burn_blue_candle", burnDelay, npc, player);
								if (world._blueFounded == 4)
								{
									startQuestTimer("spawn_zaken", zakenDelay, npc, player);
								}
							}
							break;
						case "burn_blue_candle" :
							if (npc.getRightHandItem() == 15280)
							{
								npc.setRHandId(15302);
								npc.setDisplayEffect(3);
							}
							break;
						case "burn_bad_candle" :
							if (npc.getRightHandItem() == 0)
							{
								npc.setRHandId(15280);
								npc.setDisplayEffect(1);
								startQuestTimer("burn_red_candle", burnDelay, npc, player);
							}
							break;
						case "burn_red_candle" :
							if (npc.getRightHandItem() == 15280)
							{
								npc.setRHandId(15281);
								final int room = getRoomByCandle(world, npc);
								npc.setDisplayEffect(2);
								manageScreenMsg(world, NpcStringId.THE_CANDLES_CAN_LEAD_YOU_TO_ZAKEN_DESTROY_HIM);
								spawnInRoom(world._is83 ? 29182 : 29023, room, player, world);
								spawnInRoom(world._is83 ? 29183 : 29024, room, player, world);
								spawnInRoom(world._is83 ? 29185 : 29027, room, player, world);
								spawnInRoom(world._is83 ? 29184 : 29026, room, player, world);
							}
							break;
						case "spawn_zaken" :
							if (!world._isSpawned)
							{
								world._isSpawned = true;
								if (world._is83)
								{
									manageScreenMsg(world, NpcStringId.WHO_DARES_AWKAWEN_THE_MIGHTY_ZAKEN);
								}
								spawnInRoom(world._is83 ? 29181 : 29176, world.zakenRoom, player, world);
								spawnInRoom(world._is83 ? 29182 : 29023, world.zakenRoom, player, world);
								spawnInRoom(world._is83 ? 29185 : 29027, world.zakenRoom, player, world);
								spawnInRoom(world._is83 ? 29184 : 29026, world.zakenRoom, player, world);
								spawnInRoom(world._is83 ? 29183 : 29024, world.zakenRoom, player, world);
							}
							break;
					}
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final ReflectionWorld tmpWorld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpWorld != null && tmpWorld instanceof ZakenDayWorld)
		{
			final ZakenDayWorld world = (ZakenDayWorld) tmpWorld;
			if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
			{
				return null;
			}
			final long finishDiff = System.currentTimeMillis() - world.startTime;
			final int npcId = npc.getId();
			if (npcId == 29176)
			{
				finishInstance(world, true);
			}
			else if (npcId == 29181)
			{
				finishInstance(world, true);
			}
			
			if (finishDiff <= 900000)
			{
				if ((npc.getId() == 29181) && killer.isInParty() && (killer.getParty().getCommandChannel() != null))
				{
					for (final Player player : killer.getParty().getCommandChannel().getMembers())
					{
						timebonus(world, npc, player);
					}
				}
				else if ((npc.getId() == 29181) && killer.isInParty())
				{
					for (final Player player : killer.getParty().getMembers())
					{
						timebonus(world, npc, player);
					}
				}
				else if ((npc.getId() == 29181) && !killer.isInParty())
				{
					timebonus(world, npc, killer);
				}
			}
		}
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if ((tmpworld != null) && (tmpworld instanceof ZakenDayWorld))
		{
			final ZakenDayWorld world = (ZakenDayWorld) tmpworld;
			if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
			{
				return null;
			}
			
			final boolean isBlue = npc.getVariables().getInteger("isBlue", 0) == 1;
			if (npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				if (isBlue)
				{
					world._blueFounded++;
					startQuestTimer("burn_good_candle", 500, npc, player);
				}
				else
				{
					startQuestTimer("burn_bad_candle", 500, npc, player);
				}
			}
		}
		return null;
	}
	
	private void timebonus(ZakenDayWorld world, Npc npc, Player player)
	{
		final long finishDiff = System.currentTimeMillis() - world.startTime;
		if (player.isInsideRadius(npc, 2000, false, false))
		{
			final int rand = getRandom(100);
			if (finishDiff <= 300000)
			{
				if (rand < 50)
				{
					player.addItem("Zaken", 15763, 1, npc, true);
				}
			}
			else if (finishDiff <= 600000)
			{
				if (rand < 30)
				{
					player.addItem("Zaken", 15764, 1, npc, true);
				}
			}
			else if (finishDiff <= 900000)
			{
				if (rand < 25)
				{
					player.addItem("Zaken", 15763, 1, npc, true);
				}
			}
		}
	}
	
	private void spawnCandles(ReflectionWorld world)
	{
		if ((world != null) && (world instanceof ZakenDayWorld))
		{
			if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
			{
				return;
			}
			
			final ZakenDayWorld tmpworld = (ZakenDayWorld) world;
			final List<Npc> candles = new ArrayList<>();
			tmpworld.zakenRoom = getRandom(1, 15);
			for (int i = 0; i < 36; i++)
			{
				final Npc candle = addSpawn(32705, CANDLE_SPAWN[i][0], CANDLE_SPAWN[i][1], CANDLE_SPAWN[i][2], 0, false, 0, false);
				candle.getVariables().set("candleId", i + 1);
				candle.setReflection(world.getReflection());
				candles.add(candle);
			}
		
			for (int i = 3; i < 7; i++)
			{
				candles.get(ROOM_SPAWN[tmpworld.zakenRoom - 1][i] - 1).getVariables().set("isBlue", 1);
			}
		}
	}
	
	private int getRoomByCandle(ZakenDayWorld world, Npc npc)
	{
		if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
		{
			return 0;
		}
		
		final int candleId = npc.getVariables().getInteger("candleId", 0);
		for (int i = 0; i < 15; i++)
		{
			if ((ROOM_SPAWN[i][3] == candleId) || (ROOM_SPAWN[i][4] == candleId))
			{
				return i + 1;
			}
		}
		
		if ((candleId == 6) || (candleId == 7))
		{
			return 3;
		}
		else if ((candleId == 18) || (candleId == 19))
		{
			return 8;
		}
		else if ((candleId == 30) || (candleId == 31))
		{
			return 13;
		}
		return 0;
	}
	
	private void spawnInRoom(int npcId, int roomId, Player player, ZakenDayWorld world)
	{
		if (ReflectionManager.getInstance().getWorld(world.getReflectionId()) != world)
		{
			return;
		}
		
		if ((player != null) && (npcId != 29176) && (npcId != 29181))
		{
			final Npc mob = addSpawn(npcId, ROOM_SPAWN[roomId - 1][0] + getRandom(350), ROOM_SPAWN[roomId - 1][1] + getRandom(350), ROOM_SPAWN[roomId - 1][2], 0, false, 0, false, world.getReflection());
			mob.setRunning();
			mob.setTarget(player);
			((Attackable) mob).addDamageHate(player, 0, 999);
			mob.getAI().setIntention(CtrlIntention.ATTACK, player);
		}
		else
		{
			addSpawn(npcId, ROOM_SPAWN[roomId - 1][0], ROOM_SPAWN[roomId - 1][1], ROOM_SPAWN[roomId - 1][2], 0, false, 0, false, world.getReflection());
		}
	}
	
	private void manageScreenMsg(ZakenDayWorld world, NpcStringId stringId)
	{
		for (final Player players : world.playersInside)
		{
			if ((players != null) && (players.getReflectionId() == world.getReflectionId()))
			{
				showOnScreenMsg(players, stringId, 2, 6000);
			}
		}
	}
	
	private int getTimeHour()
	{
		return (GameTimeController.getInstance().getGameTime() / 60) % 24;
	}
	
	public static void main(String[] args)
	{
		new ZakenDay(ZakenDay.class.getSimpleName(), "instances");
	}
}