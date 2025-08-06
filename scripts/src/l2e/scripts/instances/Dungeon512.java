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
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.scripts.quests._512_BladeUnderFoot;

/**
 * Created by LordWinter 19.10.2020
 */
public final class Dungeon512 extends AbstractReflection
{
	private final Map<Integer, Integer> _castleReflections = new HashMap<>(9);
	
	private class Dungeon512World extends ReflectionWorld
	{
		public ScheduledFuture<?> stageSpawnDelay;
	}
	
	private static final int[] RAIDS1 =
	{
	        25546, 25549, 25552
	};
	
	private static final int[] RAIDS2 =
	{
	        25553, 25554, 25557, 25560
	};
	
	private static final int[] RAIDS3 =
	{
	        25563, 25566, 25569
	};
	
	private Dungeon512()
	{
		super(Dungeon512.class.getSimpleName(), "instances");

		_castleReflections.put(36403, 13);
		_castleReflections.put(36404, 14);
		_castleReflections.put(36405, 15);
		_castleReflections.put(36406, 16);
		_castleReflections.put(36407, 17);
		_castleReflections.put(36408, 18);
		_castleReflections.put(36409, 19);
		_castleReflections.put(36410, 20);
		_castleReflections.put(36411, 21);
		
		for (final int i : _castleReflections.keySet())
		{
			addStartNpc(i);
			addTalkId(i);
		}
		
		for (int i = 25546; i <= 25571; i++)
		{
			addAttackId(i);
		}
		
		addKillId(25546, 25549, 25552, 25553, 25554, 25557, 25560, 25563, 25566, 25569);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc, int reflectionId)
	{
		if (enterInstance(player, npc, new Dungeon512World(), reflectionId))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			final Reflection ref = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			if (ref != null)
			{
				final long delay = ref.getParams().getLong("stageSpawnDelay");
				((Dungeon512World) world).stageSpawnDelay = ThreadPoolManager.getInstance().schedule(new StageSpawn((Dungeon512World) world), delay);
			}
		}
	}
	
	@Override
	protected boolean checkSoloType(Player player, Npc npc, ReflectionTemplate template)
	{
		final Castle castle = npc.getCastle();
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		
		if ((player == null) || (castle == null))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _512_BladeUnderFoot.class.getSimpleName() + "/CastleWarden-01.htm");
			player.sendPacket(html);
			return false;
		}
		
		if ((player.getClan() == null) || (player.getClan().getCastleId() != castle.getId()))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _512_BladeUnderFoot.class.getSimpleName() + "/CastleWarden-01.htm");
			player.sendPacket(html);
			return false;
		}
		
		final QuestState st = player.getQuestState(_512_BladeUnderFoot.class.getSimpleName());
		if ((st == null) || (st.getCond() < 1))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _512_BladeUnderFoot.class.getSimpleName() + "/CastleWarden-02.htm");
			html.replace("%player%", player.getName(null));
			player.sendPacket(html);
			return false;
		}
		return super.checkSoloType(player, npc, template);
	}
	
	@Override
	protected boolean checkPartyType(Player player, Npc npc, ReflectionTemplate template)
	{
		final Castle castle = npc.getCastle();
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		if ((player == null) || (castle == null))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _512_BladeUnderFoot.class.getSimpleName() + "/CastleWarden-01.htm");
			player.sendPacket(html);
			return false;
		}
		
		if ((player.getClan() == null) || (player.getClan().getCastleId() != castle.getId()))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _512_BladeUnderFoot.class.getSimpleName() + "/CastleWarden-01.htm");
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
			final QuestState st = partyMember.getQuestState(_512_BladeUnderFoot.class.getSimpleName());
			if (st == null || st.getCond() < 1)
			{
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _512_BladeUnderFoot.class.getSimpleName() + "/CastleWarden-02.htm");
				html.replace("%player%", partyMember.getName(null));
				player.sendPacket(html);
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
			enterInstance(player, npc, _castleReflections.get(npc.getId()));
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
		if (tmpworld != null && tmpworld instanceof Dungeon512World)
		{
			final Dungeon512World world = (Dungeon512World) tmpworld;
			if (ArrayUtils.contains(RAIDS3, npc.getId()))
			{
				final int allowed = world.getAllowed().size();
				if (player.getParty() != null)
				{
					for (final Player pl : player.getParty().getMembers())
					{
						if (pl != null && pl.getReflectionId() == world.getReflectionId())
						{
							final QuestState st = pl.getQuestState(_512_BladeUnderFoot.class.getSimpleName());
							if (st != null && st.isCond(1))
							{
								st.calcReward(512, allowed);
								st.playSound("ItemSound.quest_itemget");
							}
						}
					}
				}
				else
				{
					final QuestState st = player.getQuestState(_512_BladeUnderFoot.class.getSimpleName());
					if (st != null && st.isCond(1))
					{
						st.calcReward(512, allowed);
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
		private final Dungeon512World _world;
		
		public StageSpawn(Dungeon512World world)
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
				switch (_world.getStatus())
				{
					case 0 :
						spawnId = RAIDS1[getRandom(RAIDS1.length)];
						break;
					case 1 :
						spawnId = RAIDS2[getRandom(RAIDS2.length)];
						break;
					default :
						spawnId = RAIDS3[getRandom(RAIDS3.length)];
						break;
				}
				
				final Npc raid = addSpawn(spawnId, 12161, -49144, -3000, 0, false, 0, false, _world.getReflection());
				if (raid instanceof RaidBossInstance)
				{
					((RaidBossInstance) raid).setUseRaidCurse(false);
				}
			}
		}
	}

	public static void main(String[] args)
	{
		new Dungeon512();
	}
}