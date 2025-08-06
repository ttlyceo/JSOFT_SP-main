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
package l2e.gameserver.handler.itemhandlers.impl;

import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Playable;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;

public class ScrollOfResurrection implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return false;
		}

		for (final var e : playable.getFightEvents())
		{
			if (e != null && !e.canUseScroll(playable))
			{
				playable.sendActionFailed();
				return false;
			}
		}

		var e = playable.getPartyTournament();
		if (e != null && !e.canUseScroll(playable))
		{
			playable.sendActionFailed();
			return false;
		}

		if (!AerialCleftEvent.getInstance().onScrollUse(playable.getObjectId()))
		{
			playable.sendActionFailed();
			return false;
		}
		
		final var activeChar = playable.getActingPlayer();
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return false;
		}

		if (activeChar.isMovementDisabled())
		{
			return false;
		}

		final var itemId = item.getId();
		final var petScroll = (itemId == 6387);
		final var skills = item.getItem().getSkills();

		if (skills == null)
		{
			_log.warn(getClass().getSimpleName() + ": is missing skills!");
			return false;
		}

		final var target = (Creature) activeChar.getTarget();
		if ((target == null) || !target.isDead())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return false;
		}

		Player targetPlayer = null;
		if (target.isPlayer())
		{
			targetPlayer = (Player) target;
		}

		PetInstance targetPet = null;
		if (target instanceof PetInstance)
		{
			targetPet = (PetInstance) target;
		}

		if ((targetPlayer != null) || (targetPet != null))
		{
			boolean condGood = true;
			if (activeChar.isInsideZone(ZoneId.SIEGE))
			{
				final var siege = SiegeManager.getInstance().getSiege(activeChar);
				final var fortSiege = FortSiegeManager.getInstance().getSiege(activeChar);
				final boolean twWar = TerritoryWarManager.getInstance().isTWInProgress();
				if ((siege != null) && siege.getIsInProgress())
				{
					if (targetPlayer != null && targetPlayer.isInsideZone(ZoneId.SIEGE))
					{
						final var clan = activeChar.getClan();
						if (clan == null)
						{
							condGood = false;
							if (activeChar.isPlayer())
							{
								activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
							}
						}
						else if (siege.checkIsDefender(clan) && (siege.getControlTowerCount() == 0))
						{
							condGood = false;
							if (activeChar.isPlayer())
							{
								activeChar.sendPacket(SystemMessageId.TOWER_DESTROYED_NO_RESURRECTION);
							}
						}
						else if (siege.checkIsAttacker(clan) && (siege.getAttackerClan(clan).getNumFlags() == 0))
						{
							condGood = false;
							if (activeChar.isPlayer())
							{
								activeChar.sendPacket(SystemMessageId.NO_RESURRECTION_WITHOUT_BASE_CAMP);
							}
						}
						else
						{
							condGood = false;
							if (activeChar.isPlayer())
							{
								activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
							}
						}
					}
				}
				else if ((fortSiege != null) && fortSiege.getIsInProgress())
				{
					if (targetPlayer != null && targetPlayer.isInsideZone(ZoneId.SIEGE))
					{
						final var clan = activeChar.getClan();
						if (clan == null)
						{
							condGood = false;
							if (activeChar.isPlayer())
							{
								activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
							}
						}
						else if (fortSiege.checkIsAttacker(clan) && (fortSiege.getAttackerClan(clan).getNumFlags() == 0))
						{
							condGood = false;
							if (activeChar.isPlayer())
							{
								activeChar.sendPacket(SystemMessageId.NO_RESURRECTION_WITHOUT_BASE_CAMP);
							}
						}
						else
						{
							condGood = false;
							if (activeChar.isPlayer())
							{
								activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
							}
						}
					}
				}
				else if (twWar)
				{
					final var clan = activeChar.getClan();
					if (clan == null)
					{
						condGood = false;
						if (activeChar.isPlayer())
						{
							activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
						}
					}
					else if (TerritoryWarManager.getInstance().getHQForClan(activeChar.getClan()) == null)
					{
						condGood = false;
						if (activeChar.isPlayer())
						{
							activeChar.sendPacket(SystemMessageId.NO_RESURRECTION_WITHOUT_BASE_CAMP);
						}
					}
					else
					{
						condGood = false;
						if (activeChar.isPlayer())
						{
							activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
						}
					}
				}
				
				if (targetPet != null)
				{
					final var ownerSiege = SiegeManager.getInstance().getSiege(targetPet.getOwner().getX(), targetPet.getOwner().getY(), targetPet.getOwner().getZ());
					final var ownerFortSiege = FortSiegeManager.getInstance().getSiege(targetPet.getOwner().getX(), targetPet.getOwner().getY(), targetPet.getOwner().getZ());
					if ((ownerSiege != null) && ownerSiege.getIsInProgress())
					{
						condGood = false;
						activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
					}
					else if ((ownerFortSiege != null) && ownerFortSiege.getIsInProgress())
					{
						condGood = false;
						activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
					}
				}
			}

			if (targetPet != null)
			{
				if (targetPet.getOwner() != activeChar)
				{
					if (targetPet.getOwner().isReviveRequested())
					{
						if (targetPet.getOwner().isRevivingPet())
						{
							activeChar.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED);
						}
						else
						{
							activeChar.sendPacket(SystemMessageId.CANNOT_RES_PET2);
						}
						condGood = false;
					}
				}
			}
			else if (targetPlayer != null)
			{
				if (targetPlayer.isFestivalParticipant())
				{
					condGood = false;
					activeChar.sendMessage("You may not resurrect participants in a festival.");
				}
				if (targetPlayer.isReviveRequested())
				{
					if (targetPlayer.isRevivingPet())
					{
						activeChar.sendPacket(SystemMessageId.MASTER_CANNOT_RES);
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED);
					}
					condGood = false;
				}
				else if (petScroll)
				{
					condGood = false;
					activeChar.sendMessage("You do not have the correct scroll");
				}
			}

			if (condGood)
			{
				for (final var sk : skills)
				{
					activeChar.useMagic(sk.getSkill(), true, true, true);
				}
				return true;
			}
		}
		return false;
	}
}