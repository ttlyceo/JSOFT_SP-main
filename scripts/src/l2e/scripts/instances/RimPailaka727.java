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
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.scripts.quests._727_HopeWithinTheDarkness;

/**
 * Created by LordWinter 17.10.2020
 */
public final class RimPailaka727 extends AbstractReflection
{
	private final Map<Integer, Integer> _castleReflections = new HashMap<>(9);
	
	private class Pailaka727World extends ReflectionWorld
	{
		public ScheduledFuture<?> firstStageSpawn;
		public ScheduledFuture<?> secondStageSpawn;
		public ScheduledFuture<?> thirdStageSpawn;
		public ScheduledFuture<?> firstCommonSpawn;
		public ScheduledFuture<?> secondCommonSpawn;
		public ScheduledFuture<?> thirdCommonSpawn;
		public boolean underAttack = false;
	}
	
	private RimPailaka727()
	{
		super(RimPailaka727.class.getSimpleName(), "instances");

		_castleReflections.put(36403, 101);
		_castleReflections.put(36404, 102);
		_castleReflections.put(36405, 103);
		_castleReflections.put(36406, 104);
		_castleReflections.put(36407, 105);
		_castleReflections.put(36408, 106);
		_castleReflections.put(36409, 107);
		_castleReflections.put(36410, 108);
		_castleReflections.put(36411, 109);
		
		for (final int i : _castleReflections.keySet())
		{
			addStartNpc(i);
			addTalkId(i);
		}
		
		addStartNpc(36562, 36563, 36564, 36565);
		addTalkId(36562, 36563, 36564, 36565);
		addFirstTalkId(36562, 36563, 36564, 36565);
		
		addSpawnId(25653, 25654, 25655, 36562, 36563, 36564, 36565);
		addKillId(25653, 25654, 25655, 25656, 25657, 25658, 36562, 36563, 36564, 36565);
		addAttackId(25653, 25654, 25655, 25656, 25657, 25658, 36562, 36563, 36564, 36565);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc, int reflectionId)
	{
		if (enterInstance(player, npc, new Pailaka727World(), reflectionId))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			if (inst != null)
			{
				final long delay = inst.getParams().getLong("firstWaveDelay");
				((Pailaka727World) world).firstStageSpawn = ThreadPoolManager.getInstance().schedule(new FirstStage((Pailaka727World) world), delay);
			}
		}
	}
	
	@Override
	protected boolean checkSoloType(Player player, Npc npc, ReflectionTemplate template)
	{
		final Castle castle = npc.getCastle();
		final boolean checkConds = template.getParams().getBool("checkContractConditions");
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		if ((player == null) || (castle == null))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/CastleWarden-03.htm");
			player.sendPacket(html);
			return false;
		}
		
		if (checkConds)
		{
			boolean haveContract = false;
			for (final Fort fort : FortManager.getInstance().getForts())
			{
				if (fort.getContractedCastleId() == castle.getId())
				{
					haveContract = true;
					break;
				}
			}
			
			if (!haveContract)
			{
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/CastleWarden-11.htm");
				player.sendPacket(html);
				return false;
			}
		}
		
		final QuestState st = player.getQuestState(_727_HopeWithinTheDarkness.class.getSimpleName());
		if ((st == null) || (st.getCond() < 1))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/CastleWarden-07.htm");
			player.sendPacket(html);
			return false;
		}
		
		if ((player.getClan() == null) || (player.getClan().getCastleId() != castle.getId()))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/CastleWarden-08.htm");
			player.sendPacket(html);
			return false;
		}
		return super.checkSoloType(player, npc, template);
	}
	
	@Override
	protected boolean checkPartyType(Player player, Npc npc, ReflectionTemplate template)
	{
		final Castle castle = npc.getCastle();
		final boolean checkConds = template.getParams().getBool("checkContractConditions");
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		
		if ((player == null) || (castle == null))
		{
			html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/CastleWarden-03.htm");
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
		
		if (checkConds)
		{
			boolean haveContract = false;
			for (final Fort fort : FortManager.getInstance().getForts())
			{
				if (fort.getContractedCastleId() == castle.getId())
				{
					haveContract = true;
					break;
				}
			}
			
			if (!haveContract)
			{
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/CastleWarden-11.htm");
				player.sendPacket(html);
				return false;
			}
		}
		
		for (final Player partyMember : party.getMembers())
		{
			if (partyMember != null)
			{
				final QuestState st = partyMember.getQuestState(_727_HopeWithinTheDarkness.class.getSimpleName());
				if ((st == null) || (st.getCond() < 1))
				{
					html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/CastleWarden-07.htm");
					player.sendPacket(html);
					return false;
				}
				
				if ((partyMember.getClan() == null) || (partyMember.getClan().getCastleId() != castle.getId()))
				{
					html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/CastleWarden-08.htm");
					player.sendPacket(html);
					return false;
				}
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
	public String onFirstTalk(Npc npc, Player player)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(player.getReflectionId());
		if (tmpworld != null && tmpworld instanceof Pailaka727World)
		{
			final Pailaka727World world = (Pailaka727World) tmpworld;
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			if (world.underAttack)
			{
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/Victim-02.htm");
			}
			else if (world.isStatus(4))
			{
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/Victim-03.htm");
			}
			else
			{
				html.setFile(player, player.getLang(), "data/html/scripts/quests/" + _727_HopeWithinTheDarkness.class.getSimpleName() + "/Victim-01.htm");
			}
			player.sendPacket(html);
		}
		return null;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("enter"))
		{
			enterInstance(player, npc, _castleReflections.get(npc.getId()));
		}
		else if (event.equalsIgnoreCase("leave"))
		{
			if ((npc.getId() >= 36562) && (npc.getId() <= 36565))
			{
				final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(player.getReflectionId());
				if ((tmpworld != null) && (tmpworld instanceof Pailaka727World))
				{
					final Pailaka727World world = (Pailaka727World) tmpworld;
					world.removeAllowed(player.getObjectId());
					final Reflection ref = ReflectionManager.getInstance().getReflection(npc.getReflectionId());
					if (ref != null)
					{
						player.teleToLocation(ref.getReturnLoc(), true, ReflectionManager.DEFAULT);
						if (ref.getPlayers().isEmpty())
						{
							ref.collapse();
						}
					}
					return null;
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if ((npc.getId() >= 36562) && (npc.getId() <= 36565))
		{
			if (npc.getCurrentHp() <= (npc.getMaxHp() * 0.1))
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.YOUR_MIND_IS_GOING_BLANK));
			}
			else if (npc.getCurrentHp() <= (npc.getMaxHp() * 0.4))
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.YOUR_MIND_IS_GOING_BLANK));
			}
			return null;
		}
		
		if (player != null)
		{
			final Playable attacker = (isSummon ? player.getSummon() : player);
			if ((attacker.getLevel() - npc.getLevel()) >= 9)
			{
				if ((attacker.getBuffCount() > 0) || (attacker.getDanceCount() > 0))
				{
					npc.setTarget(attacker);
					npc.doSimultaneousCast(new SkillHolder(5456, 1).getSkill());
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
		}
		return super.onAttack(npc, player, damage, isSummon);
	}
	
	@Override
	public final String onSpawn(Npc npc)
	{
		if (npc.getId() == 36562 || npc.getId() == 36563 || npc.getId() == 36564 || npc.getId() == 36565)
		{
			npc.setIsHasNoChatWindow(false);
			if (npc.getId() == 36562)
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.WARRIORS_HAVE_YOU_COME_TO_HELP_THOSE_WHO_ARE_IMPRISONED_HERE));
			}
		}
		else if (npc.getId() == 25653)
		{
			npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.ILL_RIP_THE_FLESH_FROM_YOUR_BONES));
		}
		else if (npc.getId() == 25654)
		{
			npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.ILL_RIP_THE_FLESH_FROM_YOUR_BONES));
		}
		else if (npc.getId() == 25655)
		{
			npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.YOULL_FLOUNDER_IN_DELUSION_FOR_THE_REST_OF_YOUR_LIFE));
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onKill(Npc npc, Player player, boolean isPet)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof Pailaka727World)
		{
			final Pailaka727World world = (Pailaka727World) tmpworld;
			
			if (npc.getId() == 25653 || npc.getId() == 25654 || npc.getId() == 25655)
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.HOW_DARE_YOU));
			}
	
			switch (npc.getId())
			{
				case 36562 :
				case 36563 :
				case 36564 :
				case 36565 :
					world.setStatus(5);
					doCleanup(world);
					if (npc.getId() == 36562)
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.I_CANT_STAND_IT_ANYMORE_AAH));
					}
					else if (npc.getId() == 36563)
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.I_CANT_STAND_IT_ANYMORE_AAH));
					}
					else if (npc.getId() == 36564)
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.KYAAAK));
					}
					else if (npc.getId() == 36565)
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.ALL, npc.getId(), NpcStringId.GASP_HOW_CAN_THIS_BE));
					}
					final Reflection ref = ReflectionManager.getInstance().getReflection(npc.getReflectionId());
					if (ref != null)
					{
						for (final Npc npcs : ref.getNpcs())
						{
							if ((npcs != null) && npcs.getReflectionId() == world.getReflectionId() && ((npcs.getId() >= 36562) && (npcs.getId() <= 36565)))
							{
								if (!npcs.isDead())
								{
									npcs.doDie(null);
								}
							}
						}
					}
					finishInstance(world, false);
					break;
				case 25655 :
				case 25657 :
				case 25658 :
					if (world.isStatus(3))
					{
						if (checkAliveNpc(world))
						{
							world.incStatus();
							world.underAttack = false;
							finishInstance(world, false);
							final Reflection reflection = ReflectionManager.getInstance().getReflection(world.getReflectionId());
							if (reflection != null)
							{
								for (final Npc knight : reflection.getNpcs())
								{
									if (knight != null && knight.getReflectionId() == world.getReflectionId() && knight.getId() == 36562)
									{
										knight.broadcastPacketToOthers(2000, new NpcSay(knight.getObjectId(), Say2.SHOUT, knight.getId(), NpcStringId.YOUVE_DONE_IT_WE_BELIEVED_IN_YOU_WARRIOR_WE_WANT_TO_SHOW_OUR_SINCERITY_THOUGH_IT_IS_SMALL_PLEASE_GIVE_ME_SOME_OF_YOUR_TIME));
									}
								}
							}
							
							if (player != null)
							{
								final Party party = player.getParty();
								if (party == null || party.getMemberCount() < 2)
								{
									final QuestState st = player.getQuestState(_727_HopeWithinTheDarkness.class.getSimpleName());
									if ((st != null) && (st.isCond(2)))
									{
										st.setCond(3, true);
									}
								}
								else
								{
									for (final Player partyMember : party.getMembers())
									{
										if ((partyMember != null) && (partyMember.getReflectionId() == player.getReflectionId()))
										{
											final QuestState st = partyMember.getQuestState(_727_HopeWithinTheDarkness.class.getSimpleName());
											if ((st != null) && (st.isCond(2)))
											{
												st.setCond(3, true);
											}
										}
									}
								}
							}
						}
					}
					break;
			}
		}
		return super.onKill(npc, player, isPet);
	}
	
	private boolean checkAliveNpc(Pailaka727World world)
	{
		boolean check = false;
		if (world != null && world.isStatus(3))
		{
			final Reflection reflection = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			if (reflection != null)
			{
				check = true;
				for (final Npc npc : reflection.getNpcs())
				{
					if (npc != null && npc.getReflectionId() == world.getReflectionId())
					{
						if (npc.getId() == 36562 || npc.getId() == 36563 || npc.getId() == 36564 || npc.getId() == 36565)
						{
							continue;
						}
						
						if (!npc.isDead())
						{
							check = false;
							break;
						}
					}
				}
			}
		}
		return check;
	}
	
	private class FirstStage implements Runnable
	{
		private final Pailaka727World _world;
		
		public FirstStage(Pailaka727World world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (_world == null)
			{
				return;
			}
			
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				addSpawn(36565, 49093, -12077, -9395, 0, false, 0, false, _world.getReflection());
				addSpawn(36563, 49094, -12238, -9386, 0, false, 0, false, _world.getReflection());
				addSpawn(36564, 49093, -12401, -9388, 0, false, 0, false, _world.getReflection());
				addSpawn(36562, 49232, -12239, -9386, 0, false, 0, false, _world.getReflection());
				addSpawn(25653, 50943, -12224, -9321, 32768, false, 0, false, _world.getReflection());
				_world.incStatus();
				final long stageDelay = inst.getParams().getLong("secondWaveDelay");
				final long spawnDelay = inst.getParams().getLong("firstSpawnDelay");
				_world.firstCommonSpawn = ThreadPoolManager.getInstance().schedule(new FirstCommonSpawn(_world), spawnDelay);
				_world.secondStageSpawn = ThreadPoolManager.getInstance().schedule(new SecondStage(_world), stageDelay);
			}
		}
	}
	
	private class FirstCommonSpawn implements Runnable
	{
		private final Pailaka727World _world;
		
		public FirstCommonSpawn(Pailaka727World world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (_world == null)
			{
				return;
			}
			
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				addSpawn(25656, 50343, -12552, -9388, 32768, false, 0, false, _world.getReflection());
				addSpawn(25656, 50344, -12340, -9380, 32768, false, 0, false, _world.getReflection());
				addSpawn(25656, 50341, -12134, -9381, 32768, false, 0, false, _world.getReflection());
				addSpawn(25656, 50342, -11917, -9389, 32768, false, 0, false, _world.getReflection());
				addSpawn(25656, 50476, -12461, -9392, 32768, false, 0, false, _world.getReflection());
				addSpawn(25656, 50481, -12021, -9390, 32768, false, 0, false, _world.getReflection());
				addSpawn(25656, 50605, -12407, -9392, 32768, false, 0, false, _world.getReflection());
				addSpawn(25656, 50602, -12239, -9380, 32768, false, 0, false, _world.getReflection());
				addSpawn(25656, 50606, -12054, -9390, 32768, false, 0, false, _world.getReflection());
				_world.underAttack = true;
			}
		}
	}
	
	private class SecondStage implements Runnable
	{
		private final Pailaka727World _world;
		
		public SecondStage(Pailaka727World world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (_world == null)
			{
				return;
			}
			
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				addSpawn(25654, 50943, -12224, -9321, 32768, false, 0, false, _world.getReflection());
				_world.incStatus();
				final long stageDelay = inst.getParams().getLong("thirdWaveDelay");
				final long spawnDelay = inst.getParams().getLong("secondSpawnDelay");
				_world.secondCommonSpawn = ThreadPoolManager.getInstance().schedule(new SecondCommonSpawn(_world), spawnDelay);
				_world.thirdStageSpawn = ThreadPoolManager.getInstance().schedule(new ThirdStage(_world), stageDelay);
			}
		}
	}
	
	private class SecondCommonSpawn implements Runnable
	{
		private final Pailaka727World _world;
		
		public SecondCommonSpawn(Pailaka727World world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (_world == null)
			{
				return;
			}
			
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				addSpawn(25657, 50343, -12552, -9388, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50344, -12340, -9380, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50341, -12134, -9381, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50342, -11917, -9389, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50476, -12461, -9392, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50481, -12021, -9390, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50605, -12407, -9392, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50602, -12239, -9380, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50606, -12054, -9390, 32768, false, 0, false, _world.getReflection());
			}
		}
	}
	
	private class ThirdStage implements Runnable
	{
		private final Pailaka727World _world;
		
		public ThirdStage(Pailaka727World world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (_world == null)
			{
				return;
			}
			
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				addSpawn(25655, 50943, -12004, -9321, 32768, false, 0, false, _world.getReflection());
				addSpawn(25655, 50943, -12475, -9321, 32768, false, 0, false, _world.getReflection());
				_world.incStatus();
				final long spawnDelay = inst.getParams().getLong("thirdSpawnDelay");
				_world.secondCommonSpawn = ThreadPoolManager.getInstance().schedule(new ThirdCommonSpawn(_world), spawnDelay);
			}
		}
	}
	
	private class ThirdCommonSpawn implements Runnable
	{
		private final Pailaka727World _world;
		
		public ThirdCommonSpawn(Pailaka727World world)
		{
			_world = world;
		}
		
		@Override
		public void run()
		{
			if (_world == null)
			{
				return;
			}
			
			if (ReflectionManager.getInstance().getWorld(_world.getReflectionId()) != _world)
			{
				return;
			}
			
			final Reflection inst = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (inst != null)
			{
				addSpawn(25657, 50343, -12552, -9388, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50344, -12340, -9380, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50341, -12134, -9381, 32768, false, 0, false, _world.getReflection());
				addSpawn(25657, 50342, -11917, -9389, 32768, false, 0, false, _world.getReflection());
				addSpawn(25658, 50476, -12461, -9392, 32768, false, 0, false, _world.getReflection());
				addSpawn(25658, 50481, -12021, -9390, 32768, false, 0, false, _world.getReflection());
				addSpawn(25658, 50605, -12407, -9392, 32768, false, 0, false, _world.getReflection());
				addSpawn(25658, 50602, -12239, -9380, 32768, false, 0, false, _world.getReflection());
				addSpawn(25658, 50606, -12054, -9390, 32768, false, 0, false, _world.getReflection());
			}
		}
	}
	
	protected void doCleanup(Pailaka727World world)
	{
		if (world != null)
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
			if (world.firstCommonSpawn != null)
			{
				world.firstCommonSpawn.cancel(true);
			}
			if (world.secondCommonSpawn != null)
			{
				world.secondCommonSpawn.cancel(true);
			}
			if (world.thirdCommonSpawn != null)
			{
				world.thirdCommonSpawn.cancel(true);
			}
		}
	}

	public static void main(String[] args)
	{
		new RimPailaka727();
	}
}
