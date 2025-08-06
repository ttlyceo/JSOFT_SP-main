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
package l2e.gameserver.handler.targethandlers.impl;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.handler.targethandlers.ITargetTypeHandler;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.PetInstance;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;

public class CorpsePlayer implements ITargetTypeHandler
{
	@Override
	public GameObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		if ((target != null) && target.isDead())
		{
			final Player player;
			if (activeChar.isPlayer())
			{
				player = activeChar.getActingPlayer();
			}
			else
			{
				player = null;
			}
			
			final Player targetPlayer;
			if (target.isPlayer())
			{
				targetPlayer = target.getActingPlayer();
			}
			else
			{
				targetPlayer = null;
			}

			final PetInstance targetPet;
			if (target.isPet())
			{
				targetPet = (PetInstance) target;
			}
			else
			{
				targetPet = null;
			}

			if ((player != null) && ((targetPlayer != null) || (targetPet != null)))
			{
				boolean condGood = true;

				if (skill.getSkillType() == SkillType.RESURRECT)
				{
					if (player.isInsideZone(ZoneId.SIEGE))
					{
						final Siege siege = SiegeManager.getInstance().getSiege(activeChar);
						final boolean twWar = TerritoryWarManager.getInstance().isTWInProgress();
						if ((siege != null) && siege.getIsInProgress())
						{
							if (targetPlayer != null && targetPlayer.isInsideZone(ZoneId.SIEGE))
							{
								final Clan clan = player.getClan();
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
						else if (twWar)
						{
							final Clan clan = player.getClan();
							if (clan == null)
							{
								condGood = false;
								if (activeChar.isPlayer())
								{
									activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
								}
							}
							else if (TerritoryWarManager.getInstance().getHQForClan(player.getClan()) == null)
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
							final Siege ownerSiege = SiegeManager.getInstance().getSiege(targetPet.getOwner().getX(), targetPet.getOwner().getY(), targetPet.getOwner().getZ());
							if ((ownerSiege != null) && ownerSiege.getIsInProgress())
							{
								condGood = false;
								activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
							}
						}
					}
					
					if (targetPlayer != null)
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
								player.sendPacket(SystemMessageId.MASTER_CANNOT_RES);
							}
							else
							{
								player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED);
							}
							condGood = false;
						}
					}
					else if (targetPet != null)
					{
						if (targetPet.getOwner() != player)
						{
							if (targetPet.getOwner().isReviveRequested())
							{
								if (targetPet.getOwner().isRevivingPet())
								{
									player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED);
								}
								else
								{
									player.sendPacket(SystemMessageId.CANNOT_RES_PET2);
								}
								condGood = false;
							}
						}
					}
				}

				if (condGood)
				{
					if (!onlyFirst)
					{
						targetList.add(target);
						return targetList.toArray(new GameObject[targetList.size()]);
					}
					return new Creature[]
					{
					        target
					};
				}
			}
		}
		activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
		return EMPTY_TARGET_LIST;
	}

	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.CORPSE_PLAYER;
	}
}