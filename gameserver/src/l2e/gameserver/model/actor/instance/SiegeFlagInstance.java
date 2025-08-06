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
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.status.SiegeFlagStatus;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.Siegable;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class SiegeFlagInstance extends Npc
{
	private Clan _clan;
	private Player _player;
	private Siegable _siege;
	private final boolean _isAdvanced;
	private long _messageInterval = 0;
	
	public SiegeFlagInstance(Player player, int objectId, NpcTemplate template, boolean advanced, boolean outPost)
	{
		super(objectId, template);
		setInstanceType(InstanceType.SiegeFlagInstance);
		
		if (TerritoryWarManager.getInstance().isTWInProgress())
		{
			_clan = player.getClan();
			_player = player;
			_messageInterval = System.currentTimeMillis() + 7200000L;
			if (_clan == null)
			{
				deleteMe();
			}
			if (outPost)
			{
				_isAdvanced = false;
				setIsInvul(true);
			}
			else
			{
				_isAdvanced = advanced;
				setIsInvul(false);
			}
			getStatus();
			return;
		}
		_clan = player.getClan();
		_player = player;
		_siege = SiegeManager.getInstance().getSiege(_player.getX(), _player.getY(), _player.getZ());
		if (_siege == null)
		{
			_siege = FortSiegeManager.getInstance().getSiege(_player.getX(), _player.getY(), _player.getZ());
		}
		if (_siege == null)
		{
			_siege = CHSiegeManager.getInstance().getSiege(player);
		}
		if (_clan == null || _siege == null)
		{
			throw new NullPointerException(getClass().getSimpleName() + ": Initialization failed.");
		}

		final SiegeClan sc = _siege.getAttackerClan(_clan);
		if (sc == null)
		{
			throw new NullPointerException(getClass().getSimpleName() + ": Cannot find siege clan.");
		}

		sc.addFlag(this);
		_isAdvanced = advanced;
		getStatus();
		setIsInvul(false);
	}
	
	public SiegeFlagInstance(Player player, int objectId, NpcTemplate template)
	{
		super(objectId, template);
		_isAdvanced = false;
	}

	@Override
	public boolean isAutoAttackable(Creature attacker, boolean isPoleAttack)
	{
		return !isInvul();
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return !(isInvul() || isHealBlocked());
	}

	@Override
	protected void onDeath(Creature killer)
	{
		if (_siege != null && _clan != null)
		{
			final SiegeClan sc = _siege.getAttackerClan(_clan);
			if (sc != null)
			{
				sc.removeFlag(this);
			}
		}
		else if (_clan != null)
		{
			TerritoryWarManager.getInstance().removeClanFlag(_clan);
		}
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
		if (player == null || !canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else if (interact)
		{
			if (isAutoAttackable(player, false) && Math.abs(player.getZ() - getZ()) < 100)
			{
				player.getAI().setIntention(CtrlIntention.ATTACK, this);
			}
			else
			{
				player.sendActionFailed();
			}
		}
	}

	public boolean isAdvancedHeadquarter()
	{
		return _isAdvanced;
	}

	@Override
	public SiegeFlagStatus getStatus()
	{
		return (SiegeFlagStatus) super.getStatus();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new SiegeFlagStatus(this));
	}

	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		if (_messageInterval < System.currentTimeMillis())
		{
			_messageInterval = System.currentTimeMillis() + 20000L;
			if ((getCastle() != null && getCastle().getSiege().getIsInProgress()) || (getFort() != null && getFort().getSiege().getIsInProgress()) || (getConquerableHall() != null && getConquerableHall().isInSiege()))
			{
				if (_clan != null)
				{
					_clan.broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.BASE_UNDER_ATTACK));
				}
			}
		}
	}

	@Override
	public boolean isHealBlocked()
	{
		return true;
	}
	
}