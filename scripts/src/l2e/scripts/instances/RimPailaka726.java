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

import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.scripts.quests._726_LightwithintheDarkness;

/**
 * Created by LordWinter 17.10.2020
 */
public final class RimPailaka726 extends AbstractReflection
{
	private final Map<Integer, Integer> _fortReflections = new HashMap<>(21);
	
	private class Pailaka726World extends ReflectionWorld
	{
		public ScheduledFuture<?> firstStageSpawn;
		public ScheduledFuture<?> secondStageSpawn;
		public ScheduledFuture<?> thirdStageSpawn;
		public int checkKills = 0;
	}
	
	private RimPailaka726()
	{
		super(RimPailaka726.class.getSimpleName(), "instances");

		_fortReflections.put(35666, 80);
		_fortReflections.put(35698, 81);
		_fortReflections.put(35735, 82);
		_fortReflections.put(35767, 83);
		_fortReflections.put(35804, 84);
		_fortReflections.put(35835, 85);
		_fortReflections.put(35867, 86);
		_fortReflections.put(35904, 87);
		_fortReflections.put(35936, 88);
		_fortReflections.put(35974, 89);
		_fortReflections.put(36011, 90);
		_fortReflections.put(36043, 91);
		_fortReflections.put(36081, 92);
		_fortReflections.put(36118, 93);
		_fortReflections.put(36149, 94);
		_fortReflections.put(36181, 95);
		_fortReflections.put(36219, 96);
		_fortReflections.put(36257, 97);
		_fortReflections.put(36294, 98);
		_fortReflections.put(36326, 99);
		_fortReflections.put(36364, 100);
		
		for (final int i : _fortReflections.keySet())
		{
			addStartNpc(i);
			addTalkId(i);
		}
		addKillId(25661);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc, int reflectionId)
	{
		if (enterInstance(player, npc, new Pailaka726World(), reflectionId))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			if (inst != null)
			{
				final long delay = inst.getParams().getLong("firstWaveDelay");
				((Pailaka726World) world).firstStageSpawn = ThreadPoolManager.getInstance().schedule(new FirstStage((Pailaka726World) world), delay);
			}
		}
	}
	
	@Override
	protected boolean checkSoloType(Player player, Npc npc, ReflectionTemplate template)
	{
		final Fort fort = npc.getFort();
		final boolean checkConds = template.getParams().getBool("checkFortConditions");
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		if ((player == null) || (fort == null))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _726_LightwithintheDarkness.class.getSimpleName() + "/FortWarden-01a.htm");
			player.sendPacket(html);
			return false;
		}
		if ((player.getClan() == null) || (player.getClan().getFortId() != fort.getId()))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _726_LightwithintheDarkness.class.getSimpleName() + "/FortWarden-01a.htm");
			player.sendPacket(html);
			return false;
		}
		else if (fort.getFortState() == 0 && checkConds)
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _726_LightwithintheDarkness.class.getSimpleName() + "/FortWarden-07.htm");
			player.sendPacket(html);
			return false;
		}
		else if (fort.getFortState() == 2 && checkConds)
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _726_LightwithintheDarkness.class.getSimpleName() + "/FortWarden-08.htm");
			player.sendPacket(html);
			return false;
		}
		return super.checkSoloType(player, npc, template);
	}
	
	@Override
	protected boolean checkPartyType(Player player, Npc npc, ReflectionTemplate template)
	{
		final Fort fort = npc.getFort();
		final boolean checkConds = template.getParams().getBool("checkFortConditions");
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		
		if ((player == null) || (fort == null))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _726_LightwithintheDarkness.class.getSimpleName() + "/FortWarden-01a.htm");
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
		
		if ((player.getClan() == null) || (player.getClan().getFortId() != fort.getId()))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _726_LightwithintheDarkness.class.getSimpleName() + "/FortWarden-01a.htm");
			player.sendPacket(html);
			return false;
		}
		else if (fort.getFortState() == 0 && checkConds)
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _726_LightwithintheDarkness.class.getSimpleName() + "/FortWarden-07.htm");
			player.sendPacket(html);
			return false;
		}
		else if (fort.getFortState() == 2 && checkConds)
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _726_LightwithintheDarkness.class.getSimpleName() + "/FortWarden-08.htm");
			player.sendPacket(html);
			return false;
		}
		
		for (final Player partyMember : party.getMembers())
		{
			if ((partyMember.getClan() == null) || (partyMember.getClan().getFortId() == 0) || (partyMember.getClan().getFortId() != fort.getId()))
			{
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _726_LightwithintheDarkness.class.getSimpleName() + "/FortWarden-09.htm");
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
	public String onKill(Npc npc, Player player, boolean isPet)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof Pailaka726World)
		{
			final Pailaka726World world = (Pailaka726World) tmpworld;
			
			switch (npc.getId())
			{
				case 25661 :
				{
					world.checkKills++;
					if (world.checkKills > 1)
					{
						if (player.isInParty())
						{
							for (final Player partymember : player.getParty().getMembers())
							{
								if (partymember != null && !partymember.isDead())
								{
									final QuestState st = partymember.getQuestState(_726_LightwithintheDarkness.class.getSimpleName());
									if (st != null && st.isCond(1) && partymember.isInsideRadius(npc, 1000, true, false))
									{
										st.setCond(2, true);
									}
								}
							}
						}
						else
						{
							final QuestState st = player.getQuestState(_726_LightwithintheDarkness.class.getSimpleName());
							if (st != null && st.isCond(1) && player.isInsideRadius(npc, 1000, true, false))
							{
								st.setCond(2, true);
							}
						}
						doCleanup(world);
						final Reflection reflection = world.getReflection();
						if (reflection != null)
						{
							reflection.cleanupNpcs();
						}
						finishInstance(world, false);
					}
				}
			}
		}
		return super.onKill(npc, player, isPet);
	}
	
	private class FirstStage implements Runnable
	{
		private final Pailaka726World _world;
		
		public FirstStage(Pailaka726World world)
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
				addSpawn(36562, 49384, -12232, -9384, 0, false, 0, false, _world.getReflection());
				addSpawn(36563, 49192, -12232, -9384, 0, false, 0, false, _world.getReflection());
				addSpawn(36564, 49192, -12456, -9392, 0, false, 0, false, _world.getReflection());
				addSpawn(36565, 49192, -11992, -9392, 0, false, 0, false, _world.getReflection());
				addSpawn(25659, 50536, -12232, -9384, 32768, false, 0, false, _world.getReflection());
				broadCastPacket(_world, new ExShowScreenMessage(NpcStringId.BEGIN_STAGE_1, 2, 3000));
				for (int i = 0; i < 10; i++)
				{
					addSpawn(25662, 50536, -12232, -9384, 32768, false, 0, false, _world.getReflection());
				}
				final long delay = inst.getParams().getLong("secondWaveDelay");
				_world.secondStageSpawn = ThreadPoolManager.getInstance().schedule(new SecondStage(_world), delay);
			}
		}
	}
	
	private class SecondStage implements Runnable
	{
		private final Pailaka726World _world;
		
		public SecondStage(Pailaka726World world)
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
				broadCastPacket(_world, new ExShowScreenMessage(NpcStringId.BEGIN_STAGE_2, 2, 3000));
				addSpawn(25660, 50536, -12232, -9384, 32768, false, 0, false, _world.getReflection());
				for (int i = 0; i < 10; i++)
				{
					addSpawn(25663, 50536, -12232, -9384, 32768, false, 0, false, _world.getReflection());
				}
				final long delay = inst.getParams().getLong("thirdWaveDelay");
				_world.thirdStageSpawn = ThreadPoolManager.getInstance().schedule(new ThirdStage(_world), delay);
			}
		}
	}
	
	private class ThirdStage implements Runnable
	{
		private final Pailaka726World _world;
		
		public ThirdStage(Pailaka726World world)
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
				broadCastPacket(_world, new ExShowScreenMessage(NpcStringId.BEGIN_STAGE_3, 2, 3000));
				addSpawn(25661, 50536, -12232, -9384, 32768, false, 0, false, _world.getReflection());
				addSpawn(25661, 50536, -12232, -9384, 32768, false, 0, false, _world.getReflection());
				for (int i = 0; i < 10; i++)
				{
					addSpawn(25664, 50536, -12232, -9384, 32768, false, 0, false, _world.getReflection());
				}
			}
		}
	}
	
	protected void broadCastPacket(Pailaka726World world, GameServerPacket packet)
	{
		for (final int objId : world.getAllowed())
		{
			final Player player = GameObjectsStorage.getPlayer(objId);
			if ((player != null) && player.isOnline() && (player.getReflectionId() == world.getReflectionId()))
			{
				player.sendPacket(packet);
			}
		}
	}
	
	protected void doCleanup(Pailaka726World world)
	{
		if (world.firstStageSpawn != null)
		{
			world.firstStageSpawn.cancel(true);
		}
		if (world.secondStageSpawn != null)
		{
			world.secondStageSpawn.cancel(true);
		}
		if (world.thirdStageSpawn != null)
		{
			world.thirdStageSpawn.cancel(true);
		}
	}

	public static void main(String[] args)
	{
		new RimPailaka726();
	}
}
