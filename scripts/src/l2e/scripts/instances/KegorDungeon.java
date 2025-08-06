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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;

/**
 * Rework by LordWinter 02.10.2020
 */
public final class KegorDungeon extends AbstractReflection
{
	private class KDWorld extends ReflectionWorld
	{
		private int count;
	}

	private static final Location[] MOB_SPAWNS = new Location[]
	{
	        new Location(185216, -184112, -3308, -15396), new Location(185456, -184240, -3308, -19668), new Location(185712, -184384, -3308, -26696), new Location(185920, -184544, -3308, -32544), new Location(185664, -184720, -3308, 27892)
	};

	private KegorDungeon()
	{
		super(KegorDungeon.class.getSimpleName(), "instances");

		addFirstTalkId(18846);
		addKillId(18846, 22766);
		addStartNpc(32654, 32653);
		addTalkId(32654, 32653, 18846);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		enterInstance(player, npc, new KDWorld(), 138);
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
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final ReflectionWorld world = ReflectionManager.getInstance().getWorld(npc.getReflectionId());

		switch (event)
		{
			case "BUFF":
			{
				if ((player != null) && !player.isDead() && !npc.isDead() && npc.isInsideRadius(player, 1000, true, false) && npc.isScriptValue(1))
				{
					npc.setTarget(player);
					npc.doCast(new SkillHolder(6286, 1).getSkill());
				}
				startQuestTimer("BUFF", 30000, npc, player);
				break;
			}
			case "TIMER":
			{
				if (world != null && world instanceof KDWorld)
				{
					for (final Location loc : MOB_SPAWNS)
					{
						final Attackable spawnedMob = (Attackable) addSpawn(22766, loc, false, 0, false, world.getReflection());
						spawnedMob.setScriptValue(1);
						spawnedMob.setIsRunning(true);
						spawnedMob.getAI().setIntention(CtrlIntention.ATTACK, npc);
						spawnedMob.addDamageHate(npc, 0, 999999);
					}
					((KDWorld) world).count = MOB_SPAWNS.length;
				}
				break;
			}
			case "FINISH":
			{
				if (world != null && world instanceof KDWorld)
				{
					for (final Npc kegor : World.getInstance().getAroundNpc(player))
					{
						if (kegor.getId() == 18846)
						{
							kegor.setScriptValue(2);
							kegor.setWalking();
							kegor.setTarget(player);
							kegor.getAI().setIntention(CtrlIntention.FOLLOW, player);
							broadcastNpcSay(kegor, Say2.NPC_ALL, NpcStringId.I_CAN_FINALLY_TAKE_A_BREATHER_BY_THE_WAY_WHO_ARE_YOU_HMM_I_THINK_I_KNOW_WHO_SENT_YOU);
							break;
						}
					}
					
					final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
					if (inst != null)
					{
						inst.setDuration(3000);
					}
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState qs = player.getQuestState("_10284_AcquisitionOfDivineSword");
		if ((qs != null))
		{
			if (qs.isMemoState(2))
			{
				return npc.isScriptValue(0) ? "18846.htm" : "18846-01.htm";
			}
			else if (qs.isMemoState(3))
			{
				final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
				world.removeAllowed(player.getObjectId());
				player.teleToLocation(new Location(178823, -184303, -347, 0), 0, true, ReflectionManager.DEFAULT);
				qs.rewardItems(57, 296425);
				qs.addExpAndSp(921805, 82230);
				qs.exitQuest(false, true);
				return "18846-03.htm";
			}
		}
		return super.onFirstTalk(npc, player);
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld world = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		final KDWorld mmWorld = ((KDWorld) world);
		if (world == null)
		{
			return null;
		}

		if (npc.getId() == 18846)
		{
			mmWorld.count = 9999;
			broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.HOW_COULD_I_FALL_IN_A_PLACE_LIKE_THIS);
			ReflectionManager.getInstance().getReflection(world.getReflectionId()).setDuration(1000);
		}
		else if (npc.isScriptValue(1))
		{
			final int count;
			synchronized (mmWorld)
			{
				count = --mmWorld.count;
			}
			if (count == 0)
			{
				final QuestState qs = player.getQuestState("_10284_AcquisitionOfDivineSword");
				if ((qs != null) && qs.isMemoState(2))
				{
					qs.setMemoState(3);
					qs.setCond(6, true);
					startQuestTimer("FINISH", 3000, npc, player);
				}
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	@Override
	public String onTalk(Npc npc, Player talker)
	{
		switch (npc.getId())
		{
			case 32654 :
			case 32653 :
			{
				final QuestState qs = talker.getQuestState("_10284_AcquisitionOfDivineSword");
				if ((qs != null) && qs.isMemoState(2))
				{
					if (!qs.hasQuestItems(15514))
					{
						qs.giveItems(15514, 1);
					}
					qs.setCond(4, true);
					enterInstance(talker, npc);
				}
				break;
			}
			case 18846 :
			{
				final QuestState qs = talker.getQuestState("_10284_AcquisitionOfDivineSword");
				if ((qs != null) && qs.isMemoState(2) && qs.hasQuestItems(15514) && npc.isScriptValue(0))
				{
					qs.takeItems(15514, -1);
					qs.setCond(5, true);
					npc.setScriptValue(1);
					startQuestTimer("TIMER", 3000, npc, talker);
					startQuestTimer("BUFF", 3500, npc, talker);
					return "18846-02.htm";
				}
				break;
			}
		}
		return super.onTalk(npc, talker);
	}

	public static void main(String[] args)
	{
		new KegorDungeon();
	}
}
