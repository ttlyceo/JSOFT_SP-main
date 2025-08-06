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

import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.Util;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.HellboundManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.instance.QuestGuardInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;

/**
 * Rework by LordWinter 02.10.2020
 */
public class HellboundTown extends AbstractReflection
{
	private class TownWorld extends ReflectionWorld
	{
		protected MonsterInstance spawnedAmaskari;
		protected ScheduledFuture<?> activeAmaskariCall = null;
		public boolean isAmaskariDead = false;

		public TownWorld()
		{
		}
	}

	private static final NpcStringId[] NPCSTRING_ID =
	{
	        NpcStringId.INVADER, NpcStringId.YOU_HAVE_DONE_WELL_IN_FINDING_ME_BUT_I_CANNOT_JUST_HAND_YOU_THE_KEY
	};

	private static final NpcStringId[] NATIVES_NPCSTRING_ID =
	{
	        NpcStringId.THANK_YOU_FOR_SAVING_ME, NpcStringId.GUARDS_ARE_COMING_RUN, NpcStringId.NOW_I_CAN_ESCAPE_ON_MY_OWN
	};

	public HellboundTown(String name, String descr)
	{
		super(name, descr);

		addFirstTalkId(32358);
		addStartNpc(32346, 32358);
		addTalkId(32346, 32358);
		addAttackId(22359, 22361);
		addAggroRangeEnterId(22359);
		addKillId(22449, 32358, 22359, 22360, 22361);
	}
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new TownWorld(), 2))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((TownWorld) world).spawnedAmaskari = (MonsterInstance) addSpawn(22449, new Location(19424, 253360, -2032, 16860), false, 0, false, world.getReflection());
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
	public final String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getFirstEffect(4616) == null)
		{
			return "32358-02.htm";
		}
		return "32358-01.htm";
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;

		if (npc.getId() == 32346)
		{
			htmltext = checkConditions(player);
			if (htmltext == null)
			{
				enterInstance(player, npc);
			}
		}
		else if (npc.getId() == 32343)
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if ((tmpworld != null) && (tmpworld instanceof TownWorld))
			{
				final TownWorld world = (TownWorld) tmpworld;

				final Party party = player.getParty();
				if (party == null || party.getMemberCount() < 2)
				{
					htmltext = "32343-02.htm";
				}
				else if (npc.isBusy())
				{
					htmltext = "32343-02c.htm";
				}
				else if (player.getInventory().getInventoryItemCount(9714, -1, false) >= 1)
				{
					for (final Player partyMember : party.getMembers())
					{
						if (!Util.checkIfInRange(300, npc, partyMember, true))
						{
							return "32343-02b.htm";
						}
					}

					if (player.destroyItemByItemId("Quest", 9714, 1, npc, true))
					{
						npc.setBusy(true);
						final Reflection inst = ReflectionManager.getInstance().getReflection(world.getReflectionId());
						inst.setDuration(5 * 60000);
						inst.setEmptyDestroyTime(0);
						ThreadPoolManager.getInstance().schedule(new ExitInstance(party, world), 285000);
						htmltext = "32343-02d.htm";
					}
				}
				else
				{
					htmltext = "32343-02a.htm";
				}
			}
		}
		return htmltext;
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if ((tmpworld != null) && (tmpworld instanceof TownWorld))
		{
			final TownWorld world = (TownWorld) tmpworld;

			if (npc.getId() == 32358)
			{
				if (event.equalsIgnoreCase("rebuff") && !world.isAmaskariDead)
				{
					new SkillHolder(4616, 1).getSkill().getEffects(npc, npc, false);
				}
				else if (event.equalsIgnoreCase("break_chains"))
				{
					if ((npc.getFirstEffect(4611) == null) || world.isAmaskariDead)
					{
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NATIVES_NPCSTRING_ID[0]));
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NATIVES_NPCSTRING_ID[2]));
					}
					else
					{
						cancelQuestTimer("rebuff", npc, null);
						for (final Effect e : npc.getAllEffects())
						{
							if (e.getSkill() == new SkillHolder(4616, 1).getSkill())
							{
								e.exit();
							}
						}
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NATIVES_NPCSTRING_ID[0]));
						npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NATIVES_NPCSTRING_ID[1]));
						HellboundManager.getInstance().updateTrust(10, true);
						npc.scheduleDespawn(3000);

						if ((world.spawnedAmaskari != null) && !world.spawnedAmaskari.isDead() && (getRandom(1000) < 25) && Util.checkIfInRange(5000, npc, world.spawnedAmaskari, false))
						{
							if (world.activeAmaskariCall != null)
							{
								world.activeAmaskariCall.cancel(true);
							}

							world.activeAmaskariCall = ThreadPoolManager.getInstance().schedule(new CallAmaskari(npc), 25000);
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public final String onSpawn(Npc npc)
	{
		if (npc.getId() == 32358)
		{
			((QuestGuardInstance) npc).setPassive(true);
			((QuestGuardInstance) npc).setAutoAttackable(false);
			new SkillHolder(4616, 1).getSkill().getEffects(npc, npc, false);
			startQuestTimer("rebuff", 357000, npc, null);
		}
		else if ((npc.getId() == 22359) || (npc.getId() == 22361))
		{
			npc.setBusy(false);
			npc.setBusyMessage("");
		}
		return super.onSpawn(npc);
	}

	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if ((tmpworld != null) && (tmpworld instanceof TownWorld))
		{
			final TownWorld world = (TownWorld) tmpworld;

			if (!npc.isBusy())
			{
				npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NPCSTRING_ID[0]));
				npc.setBusy(true);

				if ((world.spawnedAmaskari != null) && !world.spawnedAmaskari.isDead() && (getRandom(1000) < 25) && Util.checkIfInRange(1000, npc, world.spawnedAmaskari, false))
				{
					if (world.activeAmaskariCall != null)
					{
						world.activeAmaskariCall.cancel(true);
					}

					world.activeAmaskariCall = ThreadPoolManager.getInstance().schedule(new CallAmaskari(npc), 25000);
				}
			}
		}
		return super.onAggroRangeEnter(npc, player, isSummon);
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if ((tmpworld != null) && (tmpworld instanceof TownWorld))
		{
			final TownWorld world = (TownWorld) tmpworld;

			if (!world.isAmaskariDead && !(npc.getBusyMessage().equalsIgnoreCase("atk") || npc.isBusy()))
			{
				int msgId;
				int range;
				switch (npc.getId())
				{
					case 22359 :
						msgId = 0;
						range = 1000;
						break;
					case 22361 :
						msgId = 1;
						range = 5000;
						break;
					default :
						msgId = -1;
						range = 0;
				}
				if (msgId >= 0)
				{
					npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NPCSTRING_ID[msgId]));
				}
				npc.setBusy(true);
				npc.setBusyMessage("atk");

				if ((world.spawnedAmaskari != null) && !world.spawnedAmaskari.isDead() && (getRandom(1000) < 25) && Util.checkIfInRange(range, npc, world.spawnedAmaskari, false))
				{
					if (world.activeAmaskariCall != null)
					{
						world.activeAmaskariCall.cancel(true);
					}

					world.activeAmaskariCall = ThreadPoolManager.getInstance().schedule(new CallAmaskari(npc), 25000);
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon, skill);
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if ((tmpworld != null) && (tmpworld instanceof TownWorld))
		{
			final TownWorld world = (TownWorld) tmpworld;
			world.isAmaskariDead = true;
		}
		return super.onKill(npc, killer, isSummon);
	}

	private String checkConditions(Player player)
	{
		if (HellboundManager.getInstance().getLevel() < 10)
		{
			return "32346-lvl.htm";
		}
		return null;
	}

	private static class CallAmaskari implements Runnable
	{
		private final Npc _caller;

		public CallAmaskari(Npc caller)
		{
			_caller = caller;
		}

		@Override
		public void run()
		{
			if ((_caller != null) && !_caller.isDead())
			{
				final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(_caller.getReflectionId());
				if ((tmpworld != null) && (tmpworld instanceof TownWorld))
				{
					final TownWorld world = (TownWorld) tmpworld;

					if ((world.spawnedAmaskari != null) && !world.spawnedAmaskari.isDead())
					{
						world.spawnedAmaskari.teleToLocation(_caller.getX(), _caller.getY(), _caller.getZ(), true, world.getReflection());
						world.spawnedAmaskari.broadcastPacketToOthers(2000, new NpcSay(world.spawnedAmaskari.getObjectId(), Say2.NPC_ALL, world.spawnedAmaskari.getId(), NpcStringId.ILL_MAKE_YOU_FEEL_SUFFERING_LIKE_A_FLAME_THAT_IS_NEVER_EXTINGUISHED));
					}
				}
			}
		}
	}

	private class ExitInstance implements Runnable
	{
		private final Party _party;
		private final TownWorld _world;

		public ExitInstance(Party party, TownWorld world)
		{
			_party = party;
			_world = world;
		}

		@Override
		public void run()
		{
			if ((_party != null) && (_world != null))
			{
				for (final Player partyMember : _party.getMembers())
				{
					if ((partyMember != null) && !partyMember.isDead())
					{
						_world.removeAllowed(partyMember.getObjectId());
						teleportPlayer(partyMember, new Location(16262, 283651, -9700), ReflectionManager.DEFAULT);
					}
				}
			}
		}
	}

	public static void main(String[] args)
	{
		new HellboundTown(HellboundTown.class.getSimpleName(), "instances");
	}
}
