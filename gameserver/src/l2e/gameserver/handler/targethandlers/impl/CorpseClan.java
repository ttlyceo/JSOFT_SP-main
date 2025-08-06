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

import l2e.commons.util.Util;
import l2e.gameserver.handler.targethandlers.ITargetTypeHandler;
import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.ClanMember;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.SkillType;
import l2e.gameserver.model.skills.targets.TargetType;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;

public class CorpseClan implements ITargetTypeHandler
{
	@Override
	public GameObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		if (activeChar.isPlayable())
		{
			final Player player = activeChar.getActingPlayer();
			
			if (player == null)
			{
				return EMPTY_TARGET_LIST;
			}
			
			if (player.isInOlympiadMode())
			{
				return new Creature[]
				{
				        player
				};
			}
			
			final int radius = skill.getAffectRange();
			final Clan clan = player.getClan();
			
			if (Skill.addSummon(activeChar, player, radius, true, true))
			{
				targetList.add(player.getSummon());
			}
			
			boolean condGood = true;
			SystemMessageId msgId = null;
			
			if (clan != null)
			{
				Player obj;
				final int maxTargets = skill.getAffectLimit();
				for (final ClanMember member : clan.getMembers())
				{
					obj = member.getPlayerInstance();
					
					if ((obj == null) || (obj == player))
					{
						continue;
					}
					
					if (player.isInDuel())
					{
						if (player.getDuelId() != obj.getDuelId())
						{
							continue;
						}
						if (player.isInParty() && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))
						{
							continue;
						}
					}
					
					if (!player.checkPvpSkill(obj, skill))
					{
						continue;
					}
					
					for (final AbstractFightEvent e : player.getFightEvents())
					{
						if (e != null && !e.canUseMagic(player, obj, skill))
						{
							continue;
						}
					}

					var e = player.getPartyTournament();
					if (e != null && !e.canUseMagic(player, obj, skill))
					{
						continue;
					}

					if (skill.getSkillType() == SkillType.RESURRECT)
					{
						if (player.isInsideZone(ZoneId.SIEGE) && obj.isInsideZone(ZoneId.SIEGE))
						{
							final Siege siege = SiegeManager.getInstance().getSiege(activeChar);
							final boolean twWar = TerritoryWarManager.getInstance().isTWInProgress();
							if ((siege != null) && siege.getIsInProgress())
							{
								if (siege.checkIsDefender(clan) && (siege.getControlTowerCount() == 0))
								{
									condGood = false;
									if (activeChar.isPlayer())
									{
										msgId = SystemMessageId.TOWER_DESTROYED_NO_RESURRECTION;
									}
									continue;
								}
								else if (siege.checkIsAttacker(clan) && (siege.getAttackerClan(clan).getNumFlags() == 0))
								{
									condGood = false;
									if (activeChar.isPlayer())
									{
										msgId = SystemMessageId.NO_RESURRECTION_WITHOUT_BASE_CAMP;
									}
									continue;
								}
								else
								{
									condGood = false;
									if (activeChar.isPlayer())
									{
										msgId = SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE;
									}
									continue;
								}
							}
							else if (twWar)
							{
								if (TerritoryWarManager.getInstance().getHQForClan(clan) == null)
								{
									condGood = false;
									if (activeChar.isPlayer())
									{
										activeChar.sendPacket(SystemMessageId.NO_RESURRECTION_WITHOUT_BASE_CAMP);
									}
									continue;
								}
								else
								{
									condGood = false;
									if (activeChar.isPlayer())
									{
										activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
									}
									continue;
								}
							}
							
							if (obj.getSummon() != null)
							{
								final Siege ownerSiege = SiegeManager.getInstance().getSiege(obj.getSummon().getOwner().getX(), obj.getSummon().getOwner().getY(), obj.getSummon().getOwner().getZ());
								if ((ownerSiege != null) && ownerSiege.getIsInProgress())
								{
									condGood = false;
									msgId = SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE;
									continue;
								}
							}
						}
					}

					if (!onlyFirst && Skill.addSummon(activeChar, obj, radius, true, true))
					{
						targetList.add(obj.getSummon());
					}
					
					if (!Skill.addCharacter(activeChar, obj, radius, true))
					{
						continue;
					}
					
					if (onlyFirst)
					{
						return new Creature[]
						{
						        obj
						};
					}
					
					if ((maxTargets > 0) && (targetList.size() >= maxTargets))
					{
						break;
					}
					
					targetList.add(obj);
				}
				
				if (!condGood && activeChar.isPlayer() && msgId != null && (targetList.isEmpty() || targetList.size() == 0))
				{
					activeChar.sendPacket(msgId);
				}
			}
		}
		else if (activeChar.isNpc())
		{
			final Npc npc = (Npc) activeChar;
			
			final var isSocial = npc.isMinion() || npc.hasMinions();
			if (!isSocial && npc.getFaction().isNone())
			{
				return new Creature[]
				{
				        activeChar
				};
			}
			
			final var minions = npc.isMinion() && npc.getLeader() != null ? npc.getLeader().getMinionList() : npc.hasMinions() ? npc.getMinionList() : null;
			targetList.add(activeChar);
			
			final int maxTargets = skill.getAffectLimit();
			for (final Npc newTarget : World.getInstance().getAroundNpc(activeChar))
			{
				if (newTarget.isAttackable() && (!npc.getFaction().isNone() && npc.isInFaction((newTarget)) || minions != null && minions.hasNpcId(newTarget.getId())))
				{
					if (!Util.checkIfInRange(skill.getCastRange(), activeChar, newTarget, true))
					{
						continue;
					}
					
					if (targetList.size() >= maxTargets)
					{
						break;
					}
					targetList.add(newTarget);
				}
			}
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.CORPSE_CLAN;
	}
}
