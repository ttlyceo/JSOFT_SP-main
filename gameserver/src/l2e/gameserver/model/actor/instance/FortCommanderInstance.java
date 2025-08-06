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
package l2e.gameserver.model.actor.instance;

import java.util.List;
import java.util.Map;

import l2e.gameserver.Config;
import l2e.gameserver.ai.character.CharacterAI;
import l2e.gameserver.ai.guard.FortGuardAI;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.reward.RewardList;
import l2e.gameserver.model.reward.RewardType;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.spawn.SpawnFortSiege;
import l2e.gameserver.model.spawn.Spawner;
import l2e.gameserver.network.NpcStringId;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.NpcSay;
import l2e.gameserver.taskmanager.ItemsAutoDestroy;

public class FortCommanderInstance extends DefenderInstance
{
	private long _messageInterval = 0;
	
	public FortCommanderInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.FortCommanderInstance);
		setIsSiegeGuard(true);
	}
	
	@Override
	public CharacterAI initAI()
	{
		return new FortGuardAI(this);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		if (isInActiveRegion())
		{
			final var ai = getAI();
			if (ai != null)
			{
				ai.enableAI();
			}
		}
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		if ((attacker == null) || !(attacker.isPlayer()))
		{
			return false;
		}
		
		final boolean isFort = ((getFort() != null) && (getFort().getId() > 0) && getFort().getSiege().getIsInProgress() && !getFort().getSiege().checkIsDefender(((Player) attacker).getClan()));
		
		return (isFort);
	}
	
	@Override
	public void addDamageHate(Creature attacker, int damage, int aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!(attacker instanceof FortCommanderInstance))
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}
	
	@Override
	protected void onDeath(Creature killer)
	{
		super.onDeath(killer);
		if (getFort().getSiege().getIsInProgress())
		{
			getFort().getSiege().killedCommander(this);
			
		}
		
		if (killer != null && (killer.isSummon() || killer.isPlayer()) && Config.EPAULETTE_ONLY_FOR_REG)
		{
			final Player player = killer.isSummon() ? ((Summon) killer).getOwner() : killer.getActingPlayer();
			if (player == null)
			{
				super.onDeath(killer);
				return;
			}
			if (player.getClan() != null && (getFort() != null && getFort().getId() > 0 && getFort().getSiege().checkIsAttacker(player.getClan())))
			{
				final Creature topDamager = getAggroList().getTopDamager(killer);
				for (final Map.Entry<RewardType, RewardList> entry : getTemplate().getRewards().entrySet())
				{
					rollRewards(entry, killer, topDamager != null ? topDamager : killer);
				}
				ItemsAutoDestroy.getInstance().tryRecalcTime();
			}
		}
	}
	
	@Override
	public void returnHome()
	{
		if (!isInsideRadius(getSpawn().getX(), getSpawn().getY(), 200, false))
		{
			if (Config.DEBUG)
			{
				_log.info(getObjectId() + ": moving home");
			}
			setisReturningToSpawnPoint(true);
			clearAggroList(true);
			
			if (hasAI())
			{
				getAI().setIntention(CtrlIntention.MOVING, getSpawn().getLocation(), 0);
			}
		}
	}
	
	@Override
	public final void addDamage(Creature attacker, int damage, Skill skill)
	{
		final Spawner spawn = getSpawn();
		if ((spawn != null) && _messageInterval < System.currentTimeMillis())
		{
			_messageInterval = System.currentTimeMillis() + 10000L;
			final List<SpawnFortSiege> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getId());
			for (final SpawnFortSiege spawn2 : commanders)
			{
				if (spawn2.getId() == spawn.getId())
				{
					NpcStringId npcString = null;
					switch (spawn2.getId())
					{
						case 1 :
							npcString = NpcStringId.ATTACKING_THE_ENEMYS_REINFORCEMENTS_IS_NECESSARY_TIME_TO_DIE;
							break;
						case 2 :
							if (attacker instanceof Summon)
							{
								attacker = ((Summon) attacker).getOwner();
							}
							npcString = NpcStringId.EVERYONE_CONCENTRATE_YOUR_ATTACKS_ON_S1_SHOW_THE_ENEMY_YOUR_RESOLVE;
							break;
						case 3 :
							npcString = NpcStringId.SPIRIT_OF_FIRE_UNLEASH_YOUR_POWER_BURN_THE_ENEMY;
							break;
					}
					if (npcString != null)
					{
						final NpcSay ns = new NpcSay(getObjectId(), Say2.NPC_SHOUT, getId(), npcString);
						if (npcString.getParamCount() == 1)
						{
							ns.addStringParameter(attacker.getName(null));
						}
						broadcastPacket(ns);
					}
				}
			}
		}
		super.addDamage(attacker, damage, skill);
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}