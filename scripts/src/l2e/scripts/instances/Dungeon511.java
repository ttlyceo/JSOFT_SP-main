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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import l2e.commons.apache.ArrayUtils;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.RaidBossInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.scripts.quests._511_AwlUnderFoot;

/**
 * Created by LordWinter 18.10.2020
 */
public final class Dungeon511 extends AbstractReflection
{
	private final Map<Integer, Integer> _fortReflections = new HashMap<>(21);
	
	private class Dungeon511World extends ReflectionWorld
	{
		public ScheduledFuture<?> stageSpawnDelay;
	}
	
	private static final int[] RAIDS1 =
	{
	        25572, 25575, 25578
	};
	
	private static final int[] RAIDS2 =
	{
	        25579, 25582, 25585, 25588
	};
	
	private static final int[] RAIDS3 =
	{
	        25589, 25592, 25593
	};
	
	private Dungeon511()
	{
		super(Dungeon511.class.getSimpleName(), "instances");

		_fortReflections.put(35666, 22);
		_fortReflections.put(35698, 23);
		_fortReflections.put(35735, 24);
		_fortReflections.put(35767, 25);
		_fortReflections.put(35804, 26);
		_fortReflections.put(35835, 27);
		_fortReflections.put(35867, 28);
		_fortReflections.put(35904, 29);
		_fortReflections.put(35936, 30);
		_fortReflections.put(35974, 31);
		_fortReflections.put(36011, 32);
		_fortReflections.put(36043, 33);
		_fortReflections.put(36081, 34);
		_fortReflections.put(36118, 35);
		_fortReflections.put(36149, 36);
		_fortReflections.put(36181, 37);
		_fortReflections.put(36219, 38);
		_fortReflections.put(36257, 39);
		_fortReflections.put(36294, 40);
		_fortReflections.put(36326, 41);
		_fortReflections.put(36364, 42);
		
		for (final int i : _fortReflections.keySet())
		{
			addStartNpc(i);
			addTalkId(i);
		}
		
		for (int i = 25572; i <= 25595; i++)
		{
			addAttackId(i);
		}
		
		addKillId(25572, 25575, 25578, 25579, 25582, 25585, 25588, 25589, 25592, 25593);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc, int reflectionId)
	{
		if (enterInstance(player, npc, new Dungeon511World(), reflectionId))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			final Reflection ref = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			if (ref != null)
			{
				final long delay = ref.getParams().getLong("stageSpawnDelay");
				((Dungeon511World) world).stageSpawnDelay = ThreadPoolManager.getInstance().schedule(new StageSpawn((Dungeon511World) world), delay);
			}
		}
	}
	
	@Override
	protected boolean checkSoloType(Player player, Npc npc, ReflectionTemplate template)
	{
		final Fort fortress = npc.getFort();
		final boolean checkConds = template.getParams().getBool("checkFortConditions");
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		if ((player == null) || (fortress == null))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-01.htm");
			player.sendPacket(html);
			return false;
		}
		
		if ((player.getClan() == null) || (player.getClan().getFortId() != fortress.getId()))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-01.htm");
			player.sendPacket(html);
			return false;
		}
		else if (fortress.getFortState() == 0 && checkConds)
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-02.htm");
			player.sendPacket(html);
			return false;
		}
		else if (fortress.getFortState() == 2 && checkConds)
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-03.htm");
			player.sendPacket(html);
			return false;
		}
		
		final QuestState st = player.getQuestState(_511_AwlUnderFoot.class.getSimpleName());
		if ((st == null) || (st.getCond() < 1))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-04.htm");
			html.replace("%player%", player.getName(null));
			player.sendPacket(html);
			return false;
		}
		return super.checkSoloType(player, npc, template);
	}
	
	@Override
	protected boolean checkPartyType(Player player, Npc npc, ReflectionTemplate template)
	{
		final Fort fortress = npc.getFort();
		final boolean checkConds = template.getParams().getBool("checkFortConditions");
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		if ((player == null) || (fortress == null))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-01.htm");
			player.sendPacket(html);
			return false;
		}
		
		if ((player.getClan() == null) || (player.getClan().getFortId() != fortress.getId()))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-01.htm");
			player.sendPacket(html);
			return false;
		}
		else if (fortress.getFortState() == 0 && checkConds)
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-02.htm");
			player.sendPacket(html);
			return false;
		}
		else if (fortress.getFortState() == 2 && checkConds)
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-03.htm");
			player.sendPacket(html);
			return false;
		}
		
		final Party party = player.getParty();
		if (party == null || party.getMemberCount() < 2)
		{
			player.sendPacket(SystemMessageId.NOT_IN_PARTY_CANT_ENTER);
			return false;
		}
		
		if (party.getLeader() != player)
		{
			player.sendPacket(SystemMessageId.ONLY_PARTY_LEADER_CAN_ENTER);
			return false;
		}
		
		for (final Player partyMember : party.getMembers())
		{
			final QuestState st = partyMember.getQuestState(_511_AwlUnderFoot.class.getSimpleName());
			if ((st == null || st.getCond() < 1) || (partyMember.getClan() == null) || (partyMember.getClan().getFortId() == 0) || (partyMember.getClan().getFortId() != fortress.getId()))
			{
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _511_AwlUnderFoot.class.getSimpleName() + "/FortressWarden-04.htm");
				html.replace("%player%", partyMember.getName(null));
				player.sendPacket(html);
				return false;
			}
		}
		return super.checkPartyType(player, npc, template);
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
		if (event.equalsIgnoreCase("Enter"))
		{
			enterInstance(player, npc, _fortReflections.get(npc.getId()));
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		final Playable attacker = (isSummon ? player.getSummon() : player);
		if ((attacker.getLevel() - npc.getLevel()) >= 9)
		{
			if (player.getParty() == null)
			{
				if ((attacker.getBuffCount() > 0) || (attacker.getDanceCount() > 0))
				{
					npc.setTarget(attacker);
					npc.doSimultaneousCast(new SkillHolder(5456, 1).getSkill());
				}
			}
			else if (player.getParty() != null)
			{
				for (final Player pmember : player.getParty().getMembers())
				{
					if ((pmember.getBuffCount() > 0) || (pmember.getDanceCount() > 0))
					{
						npc.setTarget(pmember);
						npc.doSimultaneousCast(new SkillHolder(5456, 1).getSkill());
					}
				}
			}
		}
		return super.onAttack(npc, player, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isPet)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof Dungeon511World)
		{
			final Dungeon511World world = (Dungeon511World) tmpworld;
			if (ArrayUtils.contains(RAIDS3, npc.getId()))
			{
				if (player.getParty() != null)
				{
					for (final Player pl : player.getParty().getMembers())
					{
						if (pl != null && pl.getReflectionId() == world.getReflectionId())
						{
							final QuestState st = pl.getQuestState(_511_AwlUnderFoot.class.getSimpleName());
							if (st != null && st.isCond(1))
							{
								st.calcReward(511);
								st.playSound("ItemSound.quest_itemget");
							}
						}
					}
				}
				else
				{
					final QuestState st = player.getQuestState(_511_AwlUnderFoot.class.getSimpleName());
					if (st != null && st.isCond(1))
					{
						st.calcReward(511);
						st.playSound("ItemSound.quest_itemget");
					}
				}
				finishInstance(world, false);
				if (world.stageSpawnDelay != null)
				{
					world.stageSpawnDelay.cancel(true);
				}
			}
			else
			{
				world.incStatus();
				final Reflection ref = ReflectionManager.getInstance().getReflection(world.getReflectionId());
				if (ref != null)
				{
					final long delay = ref.getParams().getLong("stageSpawnDelay");
					world.stageSpawnDelay = ThreadPoolManager.getInstance().schedule(new StageSpawn(world), delay);
				}
			}
		}
		return super.onKill(npc, player, isPet);
	}
	
	private class StageSpawn implements Runnable
	{
		private final Dungeon511World _world;
		
		public StageSpawn(Dungeon511World world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				int spawnId;
				if (_world.isStatus(0))
				{
					spawnId = RAIDS1[getRandom(RAIDS1.length)];
				}
				else if (_world.isStatus(1))
				{
					spawnId = RAIDS2[getRandom(RAIDS2.length)];
				}
				else
				{
					spawnId = RAIDS3[getRandom(RAIDS3.length)];
				}
				final Npc raid = addSpawn(spawnId, 53319, 245814, -6576, 0, false, 0, false, _world.getReflection());
				if (raid instanceof RaidBossInstance)
				{
					((RaidBossInstance) raid).setUseRaidCurse(false);
				}
			}
		}
	}

	public static void main(String[] args)
	{
		new Dungeon511();
	}
}