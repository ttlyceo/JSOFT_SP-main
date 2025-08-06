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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.ClanHallManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.MapRegionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.SiegeFlagInstance;
import l2e.gameserver.model.entity.Castle;
import l2e.gameserver.model.entity.ClanHall;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.Reflection;
import l2e.gameserver.model.entity.clanhall.SiegableHall;
import l2e.gameserver.model.items.instance.ItemInstance;

public final class RequestRestartPoint extends GameClientPacket
{
	protected int _requestedPointType;
	protected int _requestedPointItemId = 0;
	protected boolean _continuation;

	@Override
	protected void readImpl()
	{
		_requestedPointType = readD();
		if (_buf.hasRemaining())
		{
			_requestedPointItemId = readD();
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		if (!activeChar.canRevive())
		{
			return;
		}
		
		if (activeChar.isFakeDeathNow())
		{
			activeChar.stopFakeDeath(true);
			return;
		}
		else if (!activeChar.isDead())
		{
			if (Config.DEBUG)
			{
				_log.warn("Living player [" + activeChar.getName(null) + "] called RestartPointPacket! Ban this player!");
			}
			return;
		}
		portPlayer(activeChar);
	}
	
	protected final void portPlayer(final Player activeChar)
	{
		Location loc = null;
		Castle castle = null;
		Fort fort = null;
		SiegableHall hall = null;
		final boolean isInDefense = false;
		Reflection ref = ReflectionManager.DEFAULT;
		
		if (activeChar.isJailed())
		{
			_requestedPointType = 27;
		}
		else if (activeChar.isFestivalParticipant())
		{
			_requestedPointType = 5;
		}
		switch (_requestedPointType)
		{
			case 1 :
				if (((activeChar.getClan() == null) || (activeChar.getClan().getHideoutId() == 0)))
				{
					if (Config.DEBUG)
					{
						_log.warn("Player [" + activeChar.getName(null) + "] called RestartPointPacket - To Clanhall and he doesn't have Clanhall!");
					}
					return;
				}
				loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.CLANHALL);
				
				if ((ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()) != null) && (ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null))
				{
					activeChar.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
				}
				break;
			
			case 2 :
				castle = CastleManager.getInstance().getCastle(activeChar);
				
				if ((castle != null) && castle.getSiege().getIsInProgress())
				{
					if (castle.getSiege().checkIsDefender(activeChar.getClan()))
					{
						loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.CASTLE);
					}
					else if (castle.getSiege().checkIsAttacker(activeChar.getClan()))
					{
						loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.TOWN);
					}
					else
					{
						if (Config.DEBUG)
						{
							_log.warn("Player [" + activeChar.getName(null) + "] called RestartPointPacket - To Castle and he doesn't have Castle!");
						}
						return;
					}
				}
				else
				{
					if ((activeChar.getClan() == null) || (activeChar.getClan().getCastleId() == 0))
					{
						return;
					}
					loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.CASTLE);
				}
				if ((CastleManager.getInstance().getCastleByOwner(activeChar.getClan()) != null) && (CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).getFunction(Castle.FUNC_RESTORE_EXP) != null))
				{
					activeChar.restoreExp(CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).getFunction(Castle.FUNC_RESTORE_EXP).getLvl());
				}
				break;
			
			case 3 :
				if (((activeChar.getClan() == null) || (activeChar.getClan().getFortId() == 0)) && !isInDefense)
				{
					if (Config.DEBUG)
					{
						_log.warn("Player [" + activeChar.getName(null) + "] called RestartPointPacket - To Fortress and he doesn't have Fortress!");
					}
					return;
				}
				loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.FORTRESS);
				if ((FortManager.getInstance().getFortByOwner(activeChar.getClan()) != null) && (FortManager.getInstance().getFortByOwner(activeChar.getClan()).getFunction(Fort.FUNC_RESTORE_EXP) != null))
				{
					activeChar.restoreExp(FortManager.getInstance().getFortByOwner(activeChar.getClan()).getFunction(Fort.FUNC_RESTORE_EXP).getLvl());
				}
				break;
			
			case 4 :
				SiegeClan siegeClan = null;
				castle = CastleManager.getInstance().getCastle(activeChar);
				fort = FortManager.getInstance().getFort(activeChar);
				hall = CHSiegeManager.getInstance().getNearbyClanHall(activeChar);
				SiegeFlagInstance flag = TerritoryWarManager.getInstance().getHQForClan(activeChar.getClan());
				if (flag == null)
				{
					flag = TerritoryWarManager.getInstance().getFlagForClan(activeChar.getClan());
				}
				
				if ((castle != null) && castle.getSiege().getIsInProgress())
				{
					siegeClan = castle.getSiege().getAttackerClan(activeChar.getClan());
				}
				else if ((fort != null) && fort.getSiege().getIsInProgress())
				{
					siegeClan = fort.getSiege().getAttackerClan(activeChar.getClan());
				}
				else if ((hall != null) && hall.isInSiege())
				{
					siegeClan = hall.getSiege().getAttackerClan(activeChar.getClan());
				}
				if (((siegeClan == null) || siegeClan.getFlag().isEmpty()) && (flag == null))
				{
					if ((hall != null) && ((loc = hall.getSiege().getInnerSpawnLoc(activeChar)) != null))
					{
						break;
					}
					
					if (Config.DEBUG)
					{
						_log.warn("Player [" + activeChar.getName(null) + "] called RestartPointPacket - To Siege HQ and he doesn't have Siege HQ!");
					}
					return;
				}
				loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.SIEGEFLAG);
				break;
			
			case 5 :
				if (activeChar.getInventory().hasSelfResurrection())
				{
					for (final ItemInstance item : activeChar.getInventory().getItems())
					{
						if ((item != null) && (item.getItem().isSelfResurrection()))
						{
							activeChar.getInventory().destroyItemByItemId("RequestRestartPoint", item.getId(), 1, null, null);
							activeChar.doRevive();
							return;
						}
					}
				}
				
				if (!activeChar.isGM() && !activeChar.isFestivalParticipant())
				{
					if (Config.DEBUG)
					{
						_log.warn("Player [" + activeChar.getName(null) + "] called RestartPointPacket - Fixed and he isn't festival participant!");
					}
					return;
				}
				
				if (activeChar.isGM())
				{
					activeChar.doRevive(100.00);
				}
				else
				{
					ref = activeChar.getReflection();
					loc = new Location(activeChar);
				}
				break;
			case 6 :
				break;
			case 27 :
				if (!activeChar.isJailed())
				{
					return;
				}
				loc = new Location(-114356, -249645, -2984);
				break;
			default :
				loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.TOWN);
				if (activeChar.isInFightEvent())
				{
					activeChar.getFightEvent().requestRespawn(activeChar);
					return;
				}
				break;
		}
		
		if (loc != null)
		{
			activeChar.setIsIn7sDungeon(false);
			activeChar.setIsPendingRevive(true);
			activeChar.teleToLocation(loc, true, ref);
		}
	}
}