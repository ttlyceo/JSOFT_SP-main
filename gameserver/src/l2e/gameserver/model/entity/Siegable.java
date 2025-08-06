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
package l2e.gameserver.model.entity;

import java.util.List;

import l2e.gameserver.model.Clan;
import l2e.gameserver.model.SiegeClan;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;

public interface Siegable
{
	public void startSiege();

	public void endSiege();

	public SiegeClan getAttackerClan(int clanId);

	public SiegeClan getAttackerClan(Clan clan);

	public List<SiegeClan> getAttackerClans();
	
	public List<Player> getAttackersInZone();

	public boolean checkIsAttacker(Clan clan);

	public SiegeClan getDefenderClan(int clanId);

	public SiegeClan getDefenderClan(Clan clan);

	public List<SiegeClan> getDefenderClans();

	public boolean checkIsDefender(Clan clan);

	public List<Npc> getFlag(Clan clan);

	public long getSiegeStartTime();

	public boolean giveFame();
	public int getFameFrequency();
	public int getFameAmount();

	public void updateSiege();
}