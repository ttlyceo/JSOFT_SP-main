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
package l2e.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.underground_coliseum.UCArena;
import l2e.gameserver.model.entity.underground_coliseum.UCTeam;

public class ExPVPMatchRecord extends GameServerPacket
{
	public static final int START = 0;
	public static final int UPDATE = 1;
	public static final int FINISH = 2;
	private final int _type;
	private final int _winnerTeam;
	private final int _blueKills;
	private final int _redKills;
	private final List<Member> _blueList;
	private final List<Member> _redList;
	
	public ExPVPMatchRecord(int type, int winnerTeam, UCArena arena)
	{
		_type = type;
		_winnerTeam = winnerTeam;
		
		final UCTeam blueTeam = arena.getTeams()[0];
		_blueKills = blueTeam.getKillCount();
		final UCTeam redTeam = arena.getTeams()[1];
		_redKills = redTeam.getKillCount();
		
		_blueList = new ArrayList<>(9);
		
		if (blueTeam.getParty() != null)
		{
			for (final Player memberObject : blueTeam.getParty().getMembers())
			{
				if (memberObject != null)
				{
					_blueList.add(new Member(memberObject.getName(null), memberObject.getUCKills(), memberObject.getUCDeaths()));
				}
			}
		}
		
		_redList = new ArrayList<>(9);
		
		if (redTeam.getParty() != null)
		{
			for (final Player memberObject : redTeam.getParty().getMembers())
			{
				if (memberObject != null)
				{
					_redList.add(new Member(memberObject.getName(null), memberObject.getUCKills(), memberObject.getUCDeaths()));
				}
			}
		}
	}
	
	public ExPVPMatchRecord(int type, int winnerTeam)
	{
		_type = type;
		_winnerTeam = winnerTeam;
		_blueKills = 0;
		_redKills = 0;
		_blueList = new ArrayList<>(9);
		_redList = new ArrayList<>(9);
	}

	@Override
	protected void writeImpl()
	{
		writeD(_type);
		writeD(_winnerTeam);
		writeD(_winnerTeam == 0 ? 0 : _winnerTeam == 1 ? 2 : 1);
		writeD(_blueKills);
		writeD(_redKills);
		writeD(_blueList.size());
		for (final Member member : _blueList)
		{
			writeS(member._name);
			writeD(member._kills);
			writeD(member._deaths);
		}
		writeD(_redList.size());
		for (final Member member : _redList)
		{
			writeS(member._name);
			writeD(member._kills);
			writeD(member._deaths);
		}
	}
	
	public static class Member
	{
		public String _name;
		public int _kills;
		public int _deaths;
		
		public Member(String name, int kills, int deaths)
		{
			_name = name;
			_kills = kills;
			_deaths = deaths;
		}
	}
}