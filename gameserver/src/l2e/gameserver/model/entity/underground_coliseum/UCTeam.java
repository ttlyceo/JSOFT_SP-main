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
package l2e.gameserver.model.entity.underground_coliseum;

import java.util.List;

import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.UndergroundColiseumManager;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.UCTowerInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.ExPVPMatchUserDie;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class UCTeam
{
	public final static byte NOT_DECIDED = 0;
	public final static byte WIN = 1;
	public final static byte FAIL = 2;

	private final int _index;
	private final UCArena _baseArena;
	protected final int _x;
	protected final int _y;
	protected final int _z;
	private final int _npcId;
	private UCTowerInstance _tower = null;
	private Party _party;
	private int _killCount;
	private byte _status;
	private Party _lastParty;
	private int _consecutiveWins;
	private long _registerTime;
	
	public UCTeam(int index, UCArena baseArena, int x, int y, int z, int npcId)
	{
		_index = index;
		_baseArena = baseArena;
		_x = x;
		_y = y;
		_z = z;
		_npcId = npcId;
		
		setStatus(NOT_DECIDED);
	}
	
	public long getRegisterTime()
	{
		return _registerTime;
	}
	
	public void setLastParty(Party party)
	{
		_lastParty = party;
	}
	
	public void setRegisterTime(long time)
	{
		_registerTime = time;
	}
	
	public void increaseConsecutiveWins()
	{
		_consecutiveWins++;
		if (_consecutiveWins > 1 && _party != null && _party.getLeader() != null)
		{
			UndergroundColiseumManager.getInstance().updateBestTeam(_baseArena.getId(), _party.getLeader().getName(null), _consecutiveWins);
		}
	}
	
	public int getConsecutiveWins()
	{
		return _consecutiveWins;
	}
	
	public void spawnTower()
	{
		if (_tower != null)
		{
			return;
		}
		
		final NpcTemplate template = NpcsParser.getInstance().getTemplate(_npcId);
		if (template != null)
		{
			_tower = new UCTowerInstance(this, IdFactory.getInstance().getNextId(), template);
			_tower.setIsInvul(false);
			_tower.setCurrentHpMp(_tower.getMaxHp(), _tower.getMaxMp());
			_tower.spawnMe(_x, _y, _z);
		}
	}
	
	public void deleteTower()
	{
		if (_tower != null)
		{
			_tower.deleteMe();
			_tower = null;
		}
	}
	
	public void onKill(final Player player, final Player killer)
	{
		if ((player == null) || (killer == null) || (getParty() == null))
		{
			return;
		}
		
		if (player.isInSameParty(killer))
		{
			return;
		}
		
		final UCTeam otherTeam = getOtherTeam();
		otherTeam.increaseKillCount();
		player.addDeathCountUC();
		killer.addKillCountUC();
		
		_baseArena.broadcastToAll(new ExPVPMatchUserDie(_baseArena));
		
		if (player.getUCState() == Player.UC_STATE_POINT)
		{
			for (final UCPoint point : _baseArena.getPoints())
			{
				if (point.checkPlayer(player))
				{
					break;
				}
			}
		}
		
		if (_tower == null)
		{
			boolean flag = true;
			for (final Player member : getParty().getMembers())
			{
				if ((member != null) && !member.isDead())
				{
					flag = false;
				}
			}
			
			if (flag)
			{
				setStatus(FAIL);
				otherTeam.setStatus(WIN);
				_baseArena.runTaskNow();
			}
			return;
		}
		
		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				if (_tower == null)
				{
					return;
				}
				
				if (player.isDead())
				{
					resPlayer(player);
					player.teleToLocation(_x + Rnd.get(2, 50), _y + Rnd.get(10, 100), _z, true, player.getReflection());
					if (player.hasSummon())
					{
						final Summon summon = player.getSummon();
						summon.abortAttack();
						summon.abortCast();
						if (!summon.isDead())
						{
							summon.setCurrentHp(summon.getMaxHp());
							summon.setCurrentMp(summon.getMaxMp());
							summon.teleToLocation(_x + Rnd.get(2, 50), _y + Rnd.get(10, 100), _z, true, player.getReflection());
						}
					}
				}
			}
		}, Config.UC_RESS_TIME * 1000);
	}
	
	public void increaseKillCount()
	{
		_killCount++;
	}
	
	public static void resPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		player.restoreExp(100.0);
		player.doRevive();
		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
	}
	
	public void cleanUp()
	{
		if (getParty() != null)
		{
			getParty().setUCState(null);
			_party = null;
		}
		_party = null;
		_lastParty = null;
		_consecutiveWins = 0;
		setStatus(NOT_DECIDED);
		_killCount = 0;
	}
	
	public byte getStatus()
	{
		return _status;
	}
	
	public UCArena getBaseArena()
	{
		return _baseArena;
	}
	
	public void computeReward()
	{
		if ((_lastParty == null || _lastParty.getMemberCount() < 2) || (_lastParty != getOtherTeam().getParty()))
		{
			final List<UCReward> rewards = _baseArena.getRewards();
			double modifier = 1;
			switch (_consecutiveWins)
			{
				case 1 :
					modifier = 1.0;
					break;
				case 2 :
					modifier = 1.06;
					break;
				case 3 :
					modifier = 1.12;
					break;
				case 4 :
					modifier = 1.18;
					break;
				case 5 :
					modifier = 1.25;
					break;
				case 6 :
					modifier = 1.27;
					break;
				case 7 :
					modifier = 1.3;
					break;
				case 8 :
					modifier = 1.32;
					break;
				case 9 :
					modifier = 1.35;
					break;
				case 10 :
					modifier = 1.37;
					break;
				default :
					if (_consecutiveWins > 10)
					{
						modifier = 1.4;
					}
					break;
			}
			
			if (rewards == null || rewards.isEmpty())
			{
				return;
			}
			
			for (final Player member : getParty().getMembers())
			{
				if (member != null)
				{
					for (final UCReward reward : rewards)
					{
						if (reward.getId() == -100)
						{
							long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
							if ((member.getPcBangPoints() + amount) > Config.MAX_PC_BANG_POINTS)
							{
								amount = Config.MAX_PC_BANG_POINTS - member.getPcBangPoints();
							}
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
							sm.addNumber((int) amount);
							member.sendPacket(sm);
							member.setPcBangPoints((int) (member.getPcBangPoints() + amount));
							member.sendPacket(new ExPCCafePointInfo(member.getPcBangPoints(), (int) amount, true, false, 1));
						}
						else if (reward.getId() == -200)
						{
							if (member.getClan() != null)
							{
								final long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
								member.getClan().addReputationScore((int) amount, true);
							}
						}
						else if (reward.getId() == -300)
						{
							final long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
							member.setFame((int) (member.getFame() + amount));
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
							sm.addNumber((int) amount);
							member.sendPacket(sm);
							member.sendUserInfo();
						}
						else if (reward.getId() > 0)
						{
							final long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
							member.addItem("UC reward", reward.getId(), amount, null, true);
						}
					}
				}
			}
		}
	}
	
	public void setStatus(byte status)
	{
		_status = status;
		
		if (_status == WIN)
		{
			if (getIndex() == 0)
			{
				_baseArena.broadcastToAll(SystemMessage.getSystemMessage(SystemMessageId.THE_BLUE_TEAM_IS_VICTORIOUS));
			}
			else
			{
				_baseArena.broadcastToAll(SystemMessage.getSystemMessage(SystemMessageId.THE_RED_TEAM_IS_VICTORIOUS));
			}
		}
		
		switch (_status)
		{
			case NOT_DECIDED :
				break;
			case WIN :
				increaseConsecutiveWins();
				computeReward();
				deleteTower();
				break;
			case FAIL :
				deleteTower();
				break;
		}
	}
	
	public void broadcastToTeam(GameServerPacket packet)
	{
		final Party party = _party;
		if (party != null)
		{
			for (final Player member : party.getMembers())
			{
				if (member != null)
				{
					member.sendPacket(packet);
				}
			}
		}
	}
	
	public UCTeam getOtherTeam()
	{
		return _baseArena.getTeams()[getOtherTeamIndex()];
	}
	
	public int getOtherTeamIndex()
	{
		return _index == 0 ? 1 : 0;
	}
	
	public int getKillCount()
	{
		return _killCount;
	}
	
	public void setParty(Party party)
	{
		final Party oldParty = _party;
		_party = party;
		if (oldParty != null)
		{
			oldParty.setUCState(null);
		}
		
		if (_party != null)
		{
			_party.setUCState(this);
		}
	}
	
	public Party getParty()
	{
		return _party;
	}
	
	public int getIndex()
	{
		return _index;
	}
}