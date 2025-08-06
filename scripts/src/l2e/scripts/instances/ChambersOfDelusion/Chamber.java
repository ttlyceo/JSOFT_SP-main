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
package l2e.scripts.instances.ChambersOfDelusion;

import java.util.concurrent.ScheduledFuture;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.ReflectionParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate.ReflectionEntryType;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.holders.SkillHolder;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.EarthQuake;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.scripts.instances.AbstractReflection;

/**
 * Rework by LordWinter 14.11.2020
 */
public abstract class Chamber extends AbstractReflection
{
	protected class CDWorld extends ReflectionWorld
	{
		protected int _currentRoom;
		protected final Party _partyInside;
		protected ScheduledFuture<?> _banishTask;
		protected ScheduledFuture<?> _roomChangeTask;
		protected boolean _isSoloEnter = false;
		
		protected CDWorld(Party party)
		{
			_currentRoom = 0;
			_partyInside = party;
		}

		protected Party getPartyInside()
		{
			return _partyInside;
		}
	}

	private final int _enterNpc;
	private final int _gkFirst;
	private final int _gkLast;
	private final int _raid;
	private final int _box;
	private final int _reflectionId;
	protected Location[] _coords;
	protected String _boxGroup;

	protected Chamber(String name, String descr, int reflectionId, int enterNpc, int gkFirst, int gkLast, int raid, int box, String boxGroup)
	{
		super(name, descr);

		_reflectionId = reflectionId;
		_enterNpc = enterNpc;
		_gkFirst = gkFirst;
		_gkLast = gkLast;
		_raid = raid;
		_box = box;
		_boxGroup = boxGroup;
		
		addStartNpc(_enterNpc);
		addTalkId(_enterNpc);
		
		for (int i = _gkFirst; i <= _gkLast; i++)
		{
			addStartNpc(i);
			addTalkId(i);
		}
		
		addAttackId(_box);
		addSpellFinishedId(_box);
		addEventReceivedId(_box);
		addKillId(_raid);
	}

	private boolean isBigChamber()
	{
		return ((_reflectionId == 131) || (_reflectionId == 132));
	}

	private boolean isBossRoom(CDWorld world)
	{
		return (world._currentRoom == (_coords.length - 1));
	}

	protected void changeRoom(CDWorld world)
	{
		final Party party = world.getPartyInside();
		final Reflection ref = ReflectionManager.getInstance().getReflection(world.getReflectionId());
		
		if (((party == null || party.getMemberCount() < 2) && !world._isSoloEnter) || (ref == null))
		{
			return;
		}
		
		final int bossRoomChance = ref.getParams().getInteger("bossRoomChance");

		int newRoom = world._currentRoom;

		if (isBigChamber() && isBossRoom(world))
		{
			return;
		}
		else if (isBigChamber() && ((ref.getInstanceEndTime() - System.currentTimeMillis()) < 600000))
		{
			newRoom = _coords.length - 1;
		}
		else if (!isBossRoom(world) && (bossRoomChance > 0 && Rnd.chance(bossRoomChance)))
		{
			newRoom = _coords.length - 1;
		}
		else
		{
			while (newRoom == world._currentRoom)
			{
				newRoom = getRandom(_coords.length - 1);
			}
		}

		if (!world._isSoloEnter)
		{
			for (final Player partyMember : party.getMembers())
			{
				if (world.getReflectionId() == partyMember.getReflectionId())
				{
					partyMember.getAI().setIntention(CtrlIntention.IDLE);
					teleportPlayer(partyMember, _coords[newRoom], world.getReflection());
				}
			}
		}
		else
		{
			if (world.getAllowed() != null)
			{
				for (final int charId : world.getAllowed())
				{
					final Player player = GameObjectsStorage.getPlayer(charId);
					if (player != null)
					{
						player.getAI().setIntention(CtrlIntention.IDLE);
						teleportPlayer(player, _coords[newRoom], world.getReflection());
					}
				}
			}
		}

		world._currentRoom = newRoom;

		if (isBigChamber() && isBossRoom(world))
		{
			ref.setDuration((int) ((ref.getInstanceEndTime() - System.currentTimeMillis()) + 1200000));
			for (final Npc npc : ref.getNpcs())
			{
				if (npc.getId() == _gkLast)
				{
					npc.broadcastPacketToOthers(2000, new NpcSay(npc.getObjectId(), Say2.NPC_ALL, npc.getId(), NpcStringId.N21_MINUTES_ARE_ADDED_TO_THE_REMAINING_TIME_IN_THE_INSTANT_ZONE));
				}
			}
		}
		else
		{
			scheduleRoomChange(world, false);
		}
	}

	protected void earthQuake(CDWorld world)
	{
		final Party party = world.getPartyInside();

		if (party == null || party.getMemberCount() < 2)
		{
			return;
		}

		for (final Player partyMember : party.getMembers())
		{
			if (world.getReflectionId() == partyMember.getReflectionId())
			{
				partyMember.sendPacket(new EarthQuake(partyMember.getX(), partyMember.getY(), partyMember.getZ(), 20, 10));
			}
		}
	}
	
	protected final synchronized void enterInstance(Player player, Npc npc, int templateId)
	{
		final Party party = player.isInParty() ? player.getParty() : null;
		if (enterInstance(player, npc, new CDWorld(party), templateId))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			final ReflectionTemplate template = ReflectionParser.getInstance().getReflectionId(templateId);
			if (template != null)
			{
				if ((party == null || party.getMemberCount() < 2) && (template.getEntryType() == ReflectionEntryType.SOLO || template.getEntryType() == ReflectionEntryType.SOLO_PARTY))
				{
					((CDWorld) world)._isSoloEnter = true;
				}
				
				if (!((CDWorld) world)._isSoloEnter && party != null)
				{
					for (final Player member : party.getMembers())
					{
						if (member != null)
						{
							if (hasQuestItems(member, 15311))
							{
								takeItems(member, 15311, -1);
							}
							
							if (party.isLeader(member))
							{
								giveItems(member, 15311, 1);
							}
						}
					}
					((CDWorld) world)._banishTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new BanishTask(((CDWorld) world)), 60000, 60000);
				}
			}
			changeRoom((CDWorld) world);
		}
	}
	
	@Override
	protected void onTeleportEnter(Player player, ReflectionTemplate template, ReflectionWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			player.setReflection(world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().setReflection(world.getReflection());
			}
		}
		else
		{
			final CDWorld currentWorld = (CDWorld) world;
			final Location loc = _coords[currentWorld._currentRoom];
			teleportPlayer(player, loc, world.getReflection());
			if (player.hasSummon())
			{
				player.getSummon().teleToLocation(loc, true, world.getReflection());
			}
		}
	}

	protected void exitInstance(Player player)
	{
		if ((player == null) || !player.isOnline() || (player.getReflectionId() == 0))
		{
			return;
		}
		final Reflection ref = ReflectionManager.getInstance().getReflection(player.getReflectionId());
		if (ref != null)
		{
			teleportPlayer(player, ref.getReturnLoc(), ReflectionManager.DEFAULT);
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			if (world != null)
			{
				world.removeAllowed(player.getObjectId());
			}
		}
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		final String htmltext = "";
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if ((player != null) && (tmpworld != null) && (tmpworld instanceof CDWorld) && (npc.getId() >= _gkFirst) && (npc.getId() <= _gkLast))
		{
			final CDWorld world = (CDWorld) tmpworld;
			QuestState st = player.getQuestState(getName());
			if (st == null)
			{
				st = newQuestState(player);
			}
			else if (event.equals("next_room"))
			{
				if (!world._isSoloEnter)
				{
					if (player.getParty() == null)
					{
						html.setFile(player, player.getLang(), "data/html/scripts/instances/ChambersOfDelusion/no_party.htm");
						player.sendPacket(html);
						return null;
					}
					else if (player.getParty().getLeaderObjectId() != player.getObjectId())
					{
						html.setFile(player, player.getLang(), "data/html/scripts/instances/ChambersOfDelusion/no_leader.htm");
						player.sendPacket(html);
						return null;
					}
				}
				
				if (hasQuestItems(player, 15311))
				{
					st.takeItems(15311, 1);
					if (world._roomChangeTask != null)
					{
						world._roomChangeTask.cancel(false);
						world._roomChangeTask = null;
					}
					changeRoom(world);
					return null;
				}
				else
				{
					html.setFile(player, player.getLang(), "data/html/scripts/instances/ChambersOfDelusion/no_item.htm");
					player.sendPacket(html);
					return null;
				}
			}
			else if (event.equals("go_out"))
			{
				if (!world._isSoloEnter)
				{
					if (player.getParty() == null)
					{
						html.setFile(player, player.getLang(), "data/html/scripts/instances/ChambersOfDelusion/no_party.htm");
						player.sendPacket(html);
						return null;
					}
					else if (player.getParty().getLeaderObjectId() != player.getObjectId())
					{
						html.setFile(player, player.getLang(), "data/html/scripts/instances/ChambersOfDelusion/no_leader.htm");
						player.sendPacket(html);
						return null;
					}
				}
				
				if (world._banishTask != null)
				{
					world._banishTask.cancel(true);
				}
					
				if (world._roomChangeTask != null)
				{
					world._roomChangeTask.cancel(false);
					world._roomChangeTask = null;
				}

				if (!world._isSoloEnter)
				{
					for (final Player partyMember : player.getParty().getMembers())
					{
						exitInstance(partyMember);
					}
				}
				else
				{
					if (world.getAllowed() != null)
					{
						for (final int charId : world.getAllowed())
						{
							final Player pl = GameObjectsStorage.getPlayer(charId);
							if (pl != null)
							{
								exitInstance(pl);
							}
						}
					}
				}
					
				final Reflection ref = ReflectionManager.getInstance().getReflection(world.getReflectionId());
				if (ref != null)
				{
					ref.setEmptyDestroyTime(0);
				}
				return null;
			}
			else if (event.equals("look_party"))
			{
				if ((player.getParty() != null) && (player.getParty() == world.getPartyInside()))
				{
					teleportPlayer(player, _coords[world._currentRoom], world.getReflection());
				}
				return null;
			}
		}
		return htmltext;
	}

	@Override
	public String onAttack(final Npc npc, final Player attacker, final int damage, final boolean isPet, final Skill skill)
	{
		if (!npc.isBusy() && (npc.getCurrentHp() < (npc.getMaxHp() / 10)))
		{
			npc.setBusy(true);
			final MonsterInstance box = (MonsterInstance) npc;
			if (getRandom(100) < 25)
			{
				if (getRandom(100) < 33)
				{
					box.dropSingleItem(attacker, 4042, (int) (3 * Config.RATE_DROP_ITEMS));
				}
				if (getRandom(100) < 50)
				{
					box.dropSingleItem(attacker, 4044, (int) (4 * Config.RATE_DROP_ITEMS));
				}
				if (getRandom(100) < 50)
				{
					box.dropSingleItem(attacker, 4043, (int) (4 * Config.RATE_DROP_ITEMS));
				}
				if (getRandom(100) < 16)
				{
					box.dropSingleItem(attacker, 9628, (int) (2 * Config.RATE_DROP_ITEMS));
				}
				box.broadcastEvent("SCE_LUCKY", 2000, null);
				box.doCast(new SkillHolder(5758, 1).getSkill());
			}
			else
			{
				box.broadcastEvent("SCE_DREAM_FIRE_IN_THE_HOLE", 2000, null);
			}
		}
		return super.onAttack(npc, attacker, damage, isPet, skill);
	}

	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, GameObject reference)
	{
		switch (eventName)
		{
			case "SCE_LUCKY" :
				receiver.setBusy(true);
				receiver.doCast(new SkillHolder(5758, 1).getSkill());
				break;
			case "SCE_DREAM_FIRE_IN_THE_HOLE" :
				receiver.setBusy(true);
				receiver.doCast(new SkillHolder(5376, 4).getSkill());
				break;
		}
		return null;
	}

	@Override
	public String onKill(Npc npc, Player player, boolean isPet)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getPlayerWorld(player);
		if ((tmpworld != null) && (tmpworld instanceof CDWorld))
		{
			final CDWorld world = (CDWorld) tmpworld;
			final Reflection ref = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			if (ref != null)
			{
				ref.spawnByGroup(_boxGroup);
			}
			
			if (isBigChamber())
			{
				finishInstance(world, true);
			}
			else
			{
				if (world._roomChangeTask != null)
				{
					world._roomChangeTask.cancel(false);
					world._roomChangeTask = null;
				}
				scheduleRoomChange(world, true);
			}
		}
		return super.onKill(npc, player, isPet);
	}
	
	protected class BanishTask implements Runnable
	{
		CDWorld _world;
		
		BanishTask(CDWorld world)
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
			
			final Reflection ref = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (ref != null)
			{
				if ((ref.getInstanceEndTime() - System.currentTimeMillis()) < 60000)
				{
					if (_world._banishTask != null)
					{
						_world._banishTask.cancel(false);
						_world._banishTask = null;
					}
				}
				else
				{
					for (final int objId : ref.getPlayers())
					{
						final Player pl = GameObjectsStorage.getPlayer(objId);
						if ((pl != null) && pl.isOnline())
						{
							if (!_world._isSoloEnter)
							{
								if ((_world._partyInside == null) || !pl.isInParty() || (_world._partyInside != pl.getParty()))
								{
									exitInstance(pl);
								}
							}
						}
					}
				}
			}
		}
	}
	
	protected class ChangeRoomTask implements Runnable
	{
		CDWorld _world;
		
		ChangeRoomTask(CDWorld world)
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
			
			final Reflection ref = ReflectionManager.getInstance().getReflection(_world.getReflectionId());
			if (ref != null)
			{
				earthQuake(_world);
				try
				{
					Thread.sleep(5000);
				}
				catch (final InterruptedException e)
				{}
				changeRoom(_world);
			}
		}
	}
	
	protected void scheduleRoomChange(CDWorld world, boolean bossRoom)
	{
		if (world != null)
		{
			final Reflection ref = ReflectionManager.getInstance().getReflection(world.getReflectionId());
			final long nextInterval = bossRoom ? 60000L : (480 + getRandom(120)) * 1000L;
			if (ref != null && ((ref.getInstanceEndTime() - System.currentTimeMillis()) > nextInterval))
			{
				world._roomChangeTask = ThreadPoolManager.getInstance().schedule(new ChangeRoomTask(world), nextInterval - 5000);
			}
		}
	}

	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if ((npc.getId() == _box) && ((skill.getId() == 5376) || (skill.getId() == 5758)) && !npc.isDead())
		{
			npc.doDie(player);
		}
		return super.onSpellFinished(npc, player, skill);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}

		if (npc.getId() == _enterNpc)
		{
			enterInstance(player, npc, _reflectionId);
		}
		return "";
	}

	public static void main(String[] args)
	{
	}
}
