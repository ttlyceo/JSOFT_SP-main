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

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.templates.reflection.ReflectionTemplate;
import l2e.gameserver.model.actor.templates.reflection.ReflectionWorld;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.quest.QuestState;
import l2e.gameserver.model.quest.State;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Rework by LordWinter 13.12.2020
 */
public class PailakaDevilsLegacy extends AbstractReflection
{
	private class PDLWorld extends ReflectionWorld
	{
		public boolean _isTeleportScheduled = false;
		public boolean _isOnShip = false;
		public Npc _lematanNpc = null;
		public List<Npc> _followerslist;
	}

	public PailakaDevilsLegacy(String name, String descr)
	{
		super(name, descr);

		addStartNpc(32498);
		addTalkId(32498);
		
		addSpawnId(18634);
		addAttackId(18622, 18633, 32495);
		addKillId(18633, 18634, 32495, 18622);

		addEnterZoneId(20109);
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
	
	private final synchronized void enterInstance(Player player, Npc npc)
	{
		if (enterInstance(player, npc, new PDLWorld(), 44))
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getPlayerWorld(player);
			((PDLWorld) world)._lematanNpc = addSpawn(18633, 88108, -209252, -3744, 64255, false, 0, false, world.getReflection());
		}
	}

	@Override
	protected void attackPlayer(Attackable npc, Playable attacker)
	{
		npc.setIsRunning(true);
		npc.addDamageHate(attacker, 0, 999);
		npc.getAI().setIntention(CtrlIntention.ATTACK, attacker);
	}

	@Override
	public final String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("enter"))
		{
			enterInstance(player, npc);
			final QuestState qs = player.getQuestState("_129_PailakaDevilsLegacy");
			if ((qs != null) && qs.isCond(1))
			{
				qs.setCond(2, true);
			}
			return null;
		}
		else
		{
			final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
			if (tmpworld != null && tmpworld instanceof PDLWorld)
			{
				final PDLWorld world = (PDLWorld) tmpworld;
				
				if ((npc.getId() == 18634) && event.equals("follower_cast"))
				{
					if (!npc.isCastingNow() && !npc.isDead() && !world._lematanNpc.isDead())
					{
						npc.setTarget(world._lematanNpc);
						npc.doCast(SkillsParser.getInstance().getInfo(5712, 1));
					}
					startQuestTimer("follower_cast", 2000 + getRandom(100, 1000), npc, null);
					return null;
				}
				else if (event.equalsIgnoreCase("first_anim"))
				{
					if (!npc.isCastingNow() && !npc.isDead() && !world._lematanNpc.isDead())
					{
						npc.broadcastPacketToOthers(2000, new MagicSkillUse(npc, npc, 5756, 1, 2500, 0));
					}
					return null;
				}
				else if (event.equalsIgnoreCase("respawnMinions"))
				{
					if (world._lematanNpc != null && !world._lematanNpc.isDead())
					{
						final int radius = 260;
						final int rnd = Rnd.get(6);
						final int x = (int) (radius * Math.cos(rnd * 0.918));
						final int y = (int) (radius * Math.sin(rnd * 0.918));
						final Npc mob = addSpawn(18634, world._lematanNpc.getX() + x, world._lematanNpc.getY() + y, world._lematanNpc.getZ(), 0, false, 0, true, world._lematanNpc.getReflection());
						if (mob != null)
						{
							world._followerslist.add(mob);
						}
					}
					return null;
				}
				else if ((npc.getId() == 18622) && event.equalsIgnoreCase("keg_trigger"))
				{
					onAttack(npc, player, 600, false);
					return null;
				}
				else if (event.equalsIgnoreCase("lematan_teleport"))
				{
					if ((npc.getId() == 18633) && !npc.isMovementDisabled() && !world._isOnShip)
					{
						((Attackable) npc).getAggroList().reduceHate(player, 9999, false);
						((Attackable) npc).abortAttack();
						((Attackable) npc).abortCast();
						npc.broadcastPacketToOthers(2000, new MagicSkillUse(npc, 2100, 1, 1000, 0));
						startQuestTimer("lematan_finish_teleport", 1500, npc, player);
						return null;
					}
					else
					{
						world._isTeleportScheduled = false;
						return null;
					}
				}
				else if ((npc.getId() == 18633) && event.equalsIgnoreCase("lematan_finish_teleport") && !world._isOnShip)
				{
					npc.teleToLocation(84982, -208690, -3337, true, npc.getReflection());
					world._isOnShip = true;
					npc.getSpawn().setX(84982);
					npc.getSpawn().setY(-208690);
					npc.getSpawn().setZ(-3337);
					((Attackable) npc).getAggroList().reduceHate(player, 9999, false);
					world._followerslist = new ArrayList<>();
					for (int i = 0; i < 6; i++)
					{
						final int radius = 260;
						final int x = (int) (radius * Math.cos(i * 0.918));
						final int y = (int) (radius * Math.sin(i * 0.918));
						final Npc mob = addSpawn(18634, 84982 + x, -208690 + y, -3337, 0, false, 0, true, player.getReflection());
						if (mob != null)
						{
							world._followerslist.add(mob);
						}
					}
					return null;
				}
			}
		}
		return event;
	}

	@Override
	public final String onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof PDLWorld)
		{
			final PDLWorld world = (PDLWorld) tmpworld;
			if ((npc.getId() == 18622) && !npc.isDead())
			{
				npc.doCast(SkillsParser.getInstance().getInfo(5714, 1));
				for (final Creature target : World.getInstance().getAroundCharacters(npc, 900, 200))
				{
					target.reduceCurrentHp(500 + getRandom(0, 200), npc, SkillsParser.getInstance().getInfo(5714, 1));
					if (target instanceof MonsterInstance)
					{
						if (((MonsterInstance) target).getId() == 18622)
						{
							startQuestTimer("keg_trigger", 500, (Npc) target, attacker);
						}
						else
						{
							if (isSummon)
							{
								attackPlayer((Attackable) npc, attacker.getSummon());
							}
							else
							{
								attackPlayer((Attackable) npc, attacker);
							}
						}
					}
				}
				
				if (!npc.isDead())
				{
					npc.doDie(attacker);
				}
			}
			else if ((npc.getId() == 18633) && (npc.getCurrentHp() < (npc.getMaxHp() / 2)) && !world._isTeleportScheduled)
			{
				startQuestTimer("lematan_teleport", 1000, npc, attacker);
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}

	@Override
	public final String onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState("_129_PailakaDevilsLegacy");
		if ((st == null) || (st.getState() != State.STARTED))
		{
			return null;
		}
		
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof PDLWorld)
		{
			final PDLWorld world = (PDLWorld) tmpworld;
			switch (npc.getId())
			{
				case 18633 :
					if ((world._followerslist != null) && !world._followerslist.isEmpty())
					{
						for (final Npc _follower : world._followerslist)
						{
							_follower.deleteMe();
						}
						world._followerslist.clear();
					}
					st.setCond(4, true);
					addSpawn(32511, 84983, -208736, -3336, 49915, false, 0, false, npc.getReflection());
					break;
				case 18622 :
				case 32495 :
				case 18634 :
					if (world._isOnShip)
					{
						if (world._followerslist.contains(npc))
						{
							world._followerslist.remove(npc);
						}
						startQuestTimer("respawnMinions", 10000, npc, null);
					}
					break;
				default :
					break;
			}
		}
		return super.onKill(npc, player, isSummon);
	}

	@Override
	public final String onSpawn(Npc npc)
	{
		final ReflectionWorld tmpworld = ReflectionManager.getInstance().getWorld(npc.getReflectionId());
		if (tmpworld != null && tmpworld instanceof PDLWorld)
		{
			startQuestTimer("first_anim", 500, npc, null);
			startQuestTimer("follower_cast", 1000 + getRandom(100, 1000), npc, null);
			npc.disableCoreAI(true);
		}
		return null;
	}

	@Override
	public String onEnterZone(Creature character, ZoneType zone)
	{
		if ((character.isPlayer()) && !character.isDead() && !character.isTeleporting() && ((Player) character).isOnline())
		{
			final ReflectionWorld world = ReflectionManager.getInstance().getWorld(character.getReflectionId());
			if ((world != null) && (world.getTemplateId() == 44))
			{
				ThreadPoolManager.getInstance().schedule(new Teleport(character, world.getReflection()), 1000);
			}
		}
		return super.onEnterZone(character, zone);
	}

	private static final class Teleport implements Runnable
	{
		private final Creature _player;
		private final Reflection _r;

		public Teleport(Creature c, Reflection r)
		{
			_player = c;
			_r = r;
		}

		@Override
		public void run()
		{
			if (_player != null && _r != null)
			{
				_player.getAI().setIntention(CtrlIntention.IDLE);
				_player.teleToLocation(76428, -219038, -3752, true, _r);
			}
		}
	}

	public static void main(String[] args)
	{
		new PailakaDevilsLegacy(PailakaDevilsLegacy.class.getSimpleName(), "instances");
	}
}
