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

import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public final class TerritoryWardInstance extends Attackable
{
	public TerritoryWardInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);

		disableCoreAI(true);
	}

	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		if (isInvul() || (getCastle() == null) || !getCastle().getZone().isActive())
		{
			return false;
		}

		final var actingPlayer = attacker.getActingPlayer();
		if (actingPlayer == null || actingPlayer.getSiegeSide() == 0)
		{
			return false;
		}

		if (TerritoryWarManager.getInstance().isAllyField(actingPlayer, getCastle().getId()) || !TerritoryWarManager.getInstance().canStealWard(actingPlayer))
		{
			return false;
		}
		return true;
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		if (getCastle() == null)
		{
			_log.warn("TerritoryWardInstance(" + getName(null) + ") spawned outside Castle Zone!");
		}
	}

	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if ((skill != null) || !TerritoryWarManager.getInstance().isTWInProgress())
		{
			return;
		}

		final var actingPlayer = attacker.getActingPlayer();
		if (actingPlayer == null || actingPlayer.isCombatFlagEquipped() || actingPlayer.getSiegeSide() == 0 || getCastle() == null)
		{
			return;
		}
		if (TerritoryWarManager.getInstance().isAllyField(actingPlayer, getCastle().getId()))
		{
			return;
		}
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}

	@Override
	public void reduceCurrentHpByDOT(double i, Creature attacker, Skill skill)
	{
	}

	@Override
	protected void onDeath(Creature killer)
	{
		if ((getCastle() == null) || !TerritoryWarManager.getInstance().isTWInProgress())
		{
			super.onDeath(killer);
			return;
		}

		final var player = killer.getActingPlayer();
		if (player != null)
		{
			if (player.getSiegeSide() > 0 && !player.isCombatFlagEquipped())
			{
				player.addItem("Pickup", getId() - 23012, 1, null, false);
			}
			else
			{
				TerritoryWarManager.getInstance().getTerritoryWard(getId() - 36491).spawnMe();
			}
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_S1_WARD_HAS_BEEN_DESTROYED_C2_HAS_THE_WARD);
			sm.addString(getName("en").replaceAll(" Ward", ""));
			sm.addPcName(player);
			TerritoryWarManager.getInstance().announceToParticipants(sm, 0, 0);
		}
		else
		{
			TerritoryWarManager.getInstance().getTerritoryWard(getId() - 36491).spawnMe();
		}
		decayMe();
		super.onDeath(killer);
	}

	@Override
	public void onForcedAttack(Player player, boolean shift)
	{
		onAction(player, false, shift);
	}

	@Override
	public void onAction(Player player, boolean interact, boolean shift)
	{
		if ((player == null) || !canTarget(player))
		{
			return;
		}

		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else if (interact)
		{
			if (isAutoAttackable(player, false) && (Math.abs(player.getZ() - getZ()) < 100))
			{
				player.getAI().setIntention(CtrlIntention.ATTACK, this);
			}
			else
			{
				player.sendActionFailed();
			}
		}
	}
}