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
package l2e.scripts.ai.groups;

import l2e.commons.apache.ArrayUtils;
import l2e.commons.util.NpcUtils;
import l2e.commons.util.PositionUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.model.CtrlEvent;
import l2e.gameserver.ai.npc.Fighter;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.MinionList;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.instance.CannibalisticStakatoChiefInstance;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.type.EffectZone;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Created by LordWinter 22.11.2018
 */
public class StakatoNest extends Fighter
{
	private static final int[] BIZARRE_COCOON =
	{
	        18793, 18794, 18795, 18796, 18797, 18798
	};

	private static final int CANNIBALISTIC_STAKATO_LEADER = 22625;
	private static final int SPIKE_STAKATO_NURSE = 22630;
	private static final int SPIKE_STAKATO_NURSE_CHANGED = 22631;
	private static final int SPIKED_STAKATO_BABY = 22632;
	private static final int SPIKED_STAKATO_CAPTAIN = 22629;
	private static final int FEMALE_SPIKED_STAKATO = 22620;
	private static final int MALE_SPIKED_STAKATO = 22621;
	private static final int MALE_SPIKED_STAKATO_2 = 22622;
	private static final int SPIKED_STAKATO_GUARD = 22619;
	private static final int SKILL_GROWTH_ACCELERATOR = 2905;
	private static final int CANNIBALISTIC_STAKATO_CHIEF = 25667;
	private static final int QUEEN_SHYEED = 25671;

	private static final EffectZone _zone_mob_buff = ZoneManager.getInstance().getZoneById(200103, EffectZone.class);
	private static final EffectZone _zone_mob_buff_pc_display = ZoneManager.getInstance().getZoneById(200104, EffectZone.class);
	private static final EffectZone _zone_pc_buff = ZoneManager.getInstance().getZoneById(200105, EffectZone.class);

	private static boolean _debuffed = false;

	public StakatoNest(Attackable actor)
	{
		super(actor);
		if (ArrayUtils.contains(BIZARRE_COCOON, actor.getId()))
		{
			actor.setIsInvul(true);
			actor.setIsImmobilized(true);
		}
	}

	@Override
	protected void onEvtSpawn()
	{
		final Attackable actor = getActiveChar();

		if (actor.getId() == QUEEN_SHYEED)
		{
			if (!_debuffed)
			{
				_debuffed = true;
				_zone_mob_buff.setIsEnabled(true);
				_zone_mob_buff_pc_display.setIsEnabled(true);
				_zone_pc_buff.setIsEnabled(false);
			}
		
			for (final GameObject player : World.getInstance().getAroundPlayers(actor, 1500, 400))
			{
				if (player != null)
				{
					player.sendPacket(SystemMessageId.SHYEED_S_ROAR_FILLED_WITH_WRATH_RINGS_THROUGHOUT_THE_STAKATO_NEST);
				}
			}
		}
		super.onEvtSpawn();
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		final Attackable actor = getActiveChar();
		final MonsterInstance mob = (MonsterInstance) actor;

		if (attacker != null && (mob.getId() == CANNIBALISTIC_STAKATO_LEADER) && Rnd.chance(10) && (mob.getCurrentHpPercents() < 30))
		{
			final Npc follower = getAliveMinion(actor);
			if ((follower != null) && (follower.getCurrentHpPercents() > 30))
			{
				mob.abortAttack();
				mob.abortCast();
				mob.setHeading(PositionUtils.getHeadingTo(mob, follower));
				mob.setTarget(follower);
				mob.doCast(SkillsParser.getInstance().getInfo(4485, 1));
				mob.setCurrentHp(mob.getCurrentHp() + follower.getCurrentHp());
				follower.doDie(follower);
				follower.deleteMe();
			}
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		final Attackable actor = getActiveChar();

		final Npc minion = getAliveMinion(actor);
		Npc leader = null;

		switch (actor.getId())
		{
			case SPIKE_STAKATO_NURSE :
				if (minion == null)
				{
					break;
				}
				actor.broadcastPacketToOthers(2000, new MagicSkillUse(actor, 2046, 1, 1000, 0));
				for (int i = 0; i < 3; i++)
				{
					spawnMonster(minion, killer, SPIKED_STAKATO_CAPTAIN);
				}
				break;
			case SPIKED_STAKATO_BABY :
				leader = actor.getLeader();
				if ((leader != null) && !leader.isDead())
				{
					ThreadPoolManager.getInstance().schedule(new ChangeMonster(SPIKE_STAKATO_NURSE_CHANGED, actor, killer), 3000L);
				}
				break;
			case MALE_SPIKED_STAKATO :
				if (minion == null)
				{
					break;
				}
				actor.broadcastPacketToOthers(2000, new MagicSkillUse(actor, 2046, 1, 1000, 0));
				for (int i = 0; i < 3; i++)
				{
					spawnMonster(minion, killer, SPIKED_STAKATO_GUARD);
				}
				break;
			case FEMALE_SPIKED_STAKATO :
				leader = actor.getLeader();
				if ((leader != null) && !leader.isDead())
				{
					ThreadPoolManager.getInstance().schedule(new ChangeMonster(MALE_SPIKED_STAKATO_2, actor, killer), 3000L);
				}
				break;
			case QUEEN_SHYEED :
				if (_debuffed)
				{
					_debuffed = false;
					_zone_pc_buff.setIsEnabled(true);
					_zone_mob_buff.setIsEnabled(false);
					_zone_mob_buff_pc_display.setIsEnabled(false);
				}
				break;
		}
		super.onEvtDead(killer);
	}

	@Override
	protected void onEvtSeeSpell(Skill skill, Creature caster)
	{
		final Attackable actor = getActiveChar();
		if ((actor == null) || !ArrayUtils.contains(BIZARRE_COCOON, actor.getId()) || (caster == null) || (skill.getId() != SKILL_GROWTH_ACCELERATOR))
		{
			super.onEvtSeeSpell(skill, caster);
			return;
		}
		if (Rnd.chance(8))
		{
			caster.getActingPlayer().sendPacket(SystemMessageId.NOTHING_HAPPENED);
			return;
		}
		actor.doDie(null);
		actor.endDecayTask();
		try
		{
			final CannibalisticStakatoChiefInstance mob = new CannibalisticStakatoChiefInstance(IdFactory.getInstance().getNextId(), NpcsParser.getInstance().getTemplate(CANNIBALISTIC_STAKATO_CHIEF));
			mob.setReflection(actor.getReflection());
			mob.setHeading(actor.getHeading());
			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
			mob.spawnMe(actor.getLocation().getX(), actor.getLocation().getY(), actor.getLocation().getZ());
			mob.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, caster.getActingPlayer(), Rnd.get(1, 100));
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		super.onEvtSeeSpell(skill, caster);
	}

	protected class ChangeMonster implements Runnable
	{
		private final int _monsterId;
		private final Creature _killer;
		private final Attackable _npc;

		public ChangeMonster(int mobId, Attackable npc, Creature killer)
		{
			_monsterId = mobId;
			_npc = npc;
			_killer = killer;
		}

		@Override
		public void run()
		{
			spawnMonster(_npc, _killer, _monsterId);
		}
	}

	private MonsterInstance getAliveMinion(Npc npc)
	{
		final MinionList ml = npc.getMinionList();
		if (ml != null && ml.hasAliveMinions())
		{
			for (final MonsterInstance minion : ml.getAliveMinions())
			{
				return minion;
			}
		}
		return null;
	}

	private void spawnMonster(Npc actor, Creature killer, int mobId)
	{
		final MonsterInstance npc = NpcUtils.spawnSingle(mobId, actor.getLocation());
		if (killer != null)
		{
			npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, killer, Rnd.get(1, 100));
		}
	}
}
