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

import l2e.gameserver.GameTimeController;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Rework by LordWinter 06.02.2020
 */
public class ZakenNight extends AbstractReflection
{
	private class ZakenNightWorld extends ReflectionWorld
	{
		public ZakenNightWorld()
		{
		}
	}

	private boolean _teleported = false;

	private final int[][] SPAWNS =
	{
	        // Floor 1
	        {
	                54240, 220133, -3498
			},
			{
			        54240, 218073, -3498
			},
			{
			        55265, 219095, -3498
			},
			{
			        56289, 220133, -3498
			},
			{
			        56289, 218073, -3498
			},

			// Floor 2
			{
			        54240, 220133, -3226
			},
			{
			        54240, 218073, -3226
			},
			{
			        55265, 219095, -3226
			},
			{
			        56289, 220133, -3226
			},
			{
			        56289, 218073, -3226
			},

			// Floor 3
			{
			        54240, 220133, -2954
			},
			{
			        54240, 218073, -2954
			},
			{
			        55265, 219095, -2954
			},
			{
			        56289, 220133, -2954
			},
			{
			        56289, 218073, -2954
			}
	};

	public ZakenNight(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32713);
		addTalkId(32713);
		addKillId(29022);
		addAttackId(29022);
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
	
	@Override
	protected boolean checkConditions(Player player, Npc npc, ReflectionTemplate template)
	{
		final boolean checkTime = template.getParams().getBool("checkValidTime");
		if (checkTime)
		{
			if ((getTimeHour() > 4) && (getTimeHour() < 24))
			{
				player.sendMessage((new ServerMessage("Zaken.INVALID_TIME", player.getLang())).toString());
				return false;
			}
		}
		return super.checkConditions(player, npc, template);
	}

	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new ZakenNightWorld(), 114))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			addSpawn(29022, 55312, 219168, -3223, 0, false, 0, false, world.getReflection());
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enter"))
		{
			enterInstance(player, npc);
			return null;
		}
		else
		{
			final int i = getRandom(SPAWNS.length);
			if ((npc.getId() == 29022) && (!npc.isDead()))
			{
				if (event.equalsIgnoreCase("teleport"))
				{
					((Attackable) npc).getAggroList().reduceHate(player, 9999, false);
					((Attackable) npc).abortAttack();
					((Attackable) npc).abortCast();
					npc.broadcastPacketToOthers(2000, new MagicSkillUse(npc, 4222, 1, 1000, 0));
					startQuestTimer("finish_teleport", 1500, npc, player);
				}
				else if (event.equalsIgnoreCase("finish_teleport"))
				{
					npc.teleToLocation(SPAWNS[i][0], SPAWNS[i][1], SPAWNS[i][2], true, npc.getReflection());
					npc.getSpawn().setX(SPAWNS[i][0]);
					npc.getSpawn().setY(SPAWNS[i][1]);
					npc.getSpawn().setZ(SPAWNS[i][2]);
					_teleported = false;
				}
			}
		}
		return event;
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == 29022)
		{
			if (!_teleported)
			{
				startQuestTimer("teleport", 300000, npc, attacker);
				_teleported = true;
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final ReflectionWorld tmpWorld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpWorld instanceof ZakenNightWorld)
		{
			final ZakenNightWorld world = (ZakenNightWorld) tmpWorld;
			final int npcId = npc.getId();
			if (npcId == 29022)
			{
				finishInstance(world, true);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		final int npcId = npc.getId();
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		if (npcId == 32713)
		{
			enterInstance(player, npc);
		}
		return "";
	}

	private int getTimeHour()
	{
		return (GameTimeController.getInstance().getGameTime() / 60) % 24;
	}
	
	public static void main(String[] args)
	{
		new ZakenNight(ZakenNight.class.getSimpleName(), "instances");
	}
}
