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
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;

/**
 * Rework by LordWinter 02.10.2020
 */
public final class FreyaFirstBattle extends AbstractReflection
{
	protected class IQCWorld extends ReflectionWorld
	{
		Player player = null;
	}
	
	private FreyaFirstBattle()
	{
		super(FreyaFirstBattle.class.getSimpleName(), "instances");
		
		addStartNpc(32781);
		addTalkId(32781);
		addSeeCreatureId(18848, 18849, 18926);
		addSpawnId(18847);
		addSpellFinishedId(18847);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new IQCWorld(), 137))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((IQCWorld) world).player = player;
			world.getReflection().openDoor(23140101);
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
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "TIMER_MOVING" :
			{
				if (npc != null)
				{
					npc.getAI().setIntention(CtrlIntention.MOVING, new Location(114730, -114805, -11200, 50), 0);
				}
				break;
			}
			case "TIMER_BLIZZARD" :
			{
				broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.I_CAN_NO_LONGER_STAND_BY);
				npc.stopMove(null);
				npc.setTarget(player);
				npc.doCast(new SkillHolder(6276, 1).getSkill());
				break;
			}
			case "TIMER_SCENE_21" :
			{
				if (npc != null)
				{
					player.showQuestMovie(21);
					npc.deleteMe();
					startQuestTimer("TIMER_PC_LEAVE", 24000, npc, player);
				}
				break;
			}
			case "TIMER_PC_LEAVE" :
			{
				final QuestState qs = player.getQuestState("_10285_MeetingSirra");
				if ((qs != null))
				{
					qs.setMemoState(3);
					qs.setCond(10, true);
					final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
					if (world != null)
					{
						world.removeAllowed(player.getObjectId());
					}
					player.teleToLocation(new Location(113883, -108777, -848, 0), 0, true, ReflectionManager.DEFAULT);
				}
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSeeCreature(Npc npc, Creature creature, boolean isSummon)
	{
		if (creature.isPlayer() && npc.isScriptValue(0))
		{
			broadcastNpcSay(npc, Say2.NPC_ALL, NpcStringId.S1_MAY_THE_PROTECTION_OF_THE_GODS_BE_UPON_YOU, creature.getName(null));
		}
		return super.onSeeCreature(npc, creature, isSummon);
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		startQuestTimer("TIMER_MOVING", 60000, npc, null);
		startQuestTimer("TIMER_BLIZZARD", 180000, npc, null);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		
		if ((tmpworld != null) && (tmpworld instanceof IQCWorld))
		{
			final IQCWorld world = (IQCWorld) tmpworld;
			
			if ((skill == new SkillHolder(6276, 1).getSkill()) && (world.player != null))
			{
				startQuestTimer("TIMER_SCENE_21", 1000, npc, world.player);
			}
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		enterInstance(player, npc);
		return super.onTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new FreyaFirstBattle();
	}
}
