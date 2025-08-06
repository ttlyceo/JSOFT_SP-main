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
package l2e.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import l2e.commons.util.Rnd;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.player.impl.TeleportTask;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.SystemMessageId;

public class FunPvpZone extends ZoneType
{
	private boolean _isNoRestartZone = false;
	private boolean _isNoSummonFriendZone = false;
	private boolean _isNoLogoutZone = false;
	private boolean _isNoRevive = false;
	private boolean _isPvpEnabled = false;
	private boolean _allowPvpKills = false;
	private boolean _allowParty = true;
	private boolean _reviveNoblesse = false;
	private boolean _reviveHeal = false;
	private boolean _removeBuffs = false;
	private boolean _removePets = false;
	private boolean _giveNoblesse = false;
	private boolean _isPvpZone = false;
	private boolean _canUseCommunityBuffs = false;
	private boolean _canUseCommunityTp = false;
	private boolean _canAttackAllies = false;
	private boolean _allotHwidsLimit = false;
	private int _zoneSessionId = 0;
	private boolean _hideClanInfo = false;
	private boolean _hideTitles = false;
	private boolean _hideNames = false;
	private boolean _isEnableRelation = false;
	
	private int _hwidsLimit = 0;
	private int _radius = 100;
	private int _enchant = 0;
	private int _reviveDelay = 10;
	private int _flagDelay = 20;
	private int[][] _spawnLocs;
	private int _minLvl = 1;
	private int _maxLvl = 85;
	private int[] _zoneRewardId;
	private int[] _zoneRewardAmount;
	private int[] _zoneRewardChance;
	private final List<String> _items = new ArrayList<>();
	private final List<Integer> _skills = new ArrayList<>();
	private final List<String> _grades = new ArrayList<>();
	private final List<String> _classes = new ArrayList<>();
	private final List<Integer> _blockTransformations = new ArrayList<>();
	private final String[] _gradeNames =
	{
			"", "D", "C", "B", "A", "S", "S80", "S84"
	};
	
	private int[][] _fighterBuffs = null;
	private int[][] _mageBuffs  = null;
	
	public FunPvpZone(int id)
	{
		super(id);
		addZoneId(ZoneId.FUN_PVP);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("enablePvPFlag"))
		{
			_isPvpEnabled = Boolean.parseBoolean(value);
		}
		else if (name.equals("enablePvPKills"))
		{
			_allowPvpKills = Boolean.parseBoolean(value);
		}
		else if (name.equals("enableParty"))
		{
			_allowParty = Boolean.parseBoolean(value);
		}
		else if (name.equals("isPvpZone"))
		{
			_isPvpZone = Boolean.parseBoolean(value);
			if (_isPvpZone)
			{
				addZoneId(ZoneId.PVP);
			}
		}
		else if (name.equals("canUseCommunityBuffs"))
		{
			_canUseCommunityBuffs = Boolean.parseBoolean(value);
		}
		else if (name.equals("canUseCommunityTeleports"))
		{
			_canUseCommunityTp = Boolean.parseBoolean(value);
		}
		else if (name.equals("canAttackAllies"))
		{
			_canAttackAllies = Boolean.parseBoolean(value);
		}
		else if (name.equals("spawnLocations"))
		{
			_spawnLocs = parseItemsList(value);
		}
		else if (name.equals("reviveDelay"))
		{
			_reviveDelay = Integer.parseInt(value);
			if (_reviveDelay != 0)
			{
				_isNoRevive = true;
			}
		}
		else if (name.equals("giveNoblesse"))
		{
			_giveNoblesse = Boolean.parseBoolean(value);
		}
		else if (name.equals("bannedItems"))
		{
			final String[] propertySplit = value.split(",");
			if (propertySplit.length != 0)
			{
				for (final String i : propertySplit)
				{
					_items.add(i);
				}
			}
		}
		else if (name.equals("bannedSkills"))
		{
			final String[] propertySplit = value.split(",");
			if (propertySplit.length != 0)
			{
				for (final String i : propertySplit)
				{
					_skills.add(Integer.parseInt(i));
				}
			}
		}
		else if (name.equals("bannedGrades"))
		{
			final String[] propertySplit = value.split(",");
			if (propertySplit.length != 0)
			{
				for (final String i : propertySplit)
				{
					if (i.equals("D") || i.equals("C") || i.equals("B") || i.equals("A") || i.equals("S") || i.equals("S80") || i.equals("S84"))
					{
						_grades.add(i);
					}
				}
			}
		}
		else if (name.equals("bannedClasses"))
		{
			final String[] propertySplit = value.split(",");
			if (propertySplit.length != 0)
			{
				for (final String i : propertySplit)
				{
					_classes.add(i);
				}
			}
		}
		else if (name.equals("blockTransformations"))
		{
			final String[] propertySplit = value.split(",");
			if (propertySplit.length != 0)
			{
				for (final String i : propertySplit)
				{
					_blockTransformations.add(Integer.valueOf(i));
				}
			}
		}
		else if (name.equals("respawnRadius"))
		{
			_radius = Integer.parseInt(value);
		}
		else if (name.equals("enchantLimit"))
		{
			_enchant = Integer.parseInt(value);
		}
		else if (name.equals("removeBuffs"))
		{
			_removeBuffs = Boolean.parseBoolean(value);
		}
		else if (name.equals("removePets"))
		{
			_removePets = Boolean.parseBoolean(value);
		}
		else if (name.equals("isNoRestartZone"))
		{
			_isNoRestartZone = Boolean.parseBoolean(value);
		}
		else if (name.equals("isNoSummonFriendZone"))
		{
			_isNoSummonFriendZone = Boolean.parseBoolean(value);
			if (_isNoSummonFriendZone)
			{
				addZoneId(ZoneId.NO_SUMMON_FRIEND);
			}
		}
		else if (name.equals("isNoLogoutZone"))
		{
			_isNoLogoutZone = Boolean.parseBoolean(value);
		}
		else if (name.equals("reviveNoblesse"))
		{
			_reviveNoblesse = Boolean.parseBoolean(value);
		}
		else if (name.equals("reviveHeal"))
		{
			_reviveHeal = Boolean.parseBoolean(value);
		}
		else if (name.equals("rewardItems"))
		{
			final String[] rewardId = value.trim().split(",");
			_zoneRewardId = new int[rewardId.length];
			try
			{
				int i = 0;
				for (final String id : rewardId)
				{
					_zoneRewardId[i++] = Integer.parseInt(id);
				}
			}
			catch (final NumberFormatException nfe)
			{
				_log.warn(nfe.getMessage(), nfe);
			}
		}
		else if (name.equals("rewardAmount"))
		{
			final String[] rewardCount = value.trim().split(",");
			_zoneRewardAmount = new int[rewardCount.length];
			try
			{
				int i = 0;
				for (final String count : rewardCount)
				{
					_zoneRewardAmount[i++] = Integer.parseInt(count);
				}
			}
			catch (final NumberFormatException nfe)
			{
				_log.warn(nfe.getMessage(), nfe);
			}
		}
		else if (name.equals("rewardChance"))
		{
			final String[] rewardChance = value.trim().split(",");
			_zoneRewardChance = new int[rewardChance.length];
			try
			{
				int i = 0;
				for (final String chance : rewardChance)
				{
					_zoneRewardChance[i++] = Integer.parseInt(chance);
				}
			}
			catch (final NumberFormatException nfe)
			{
				_log.warn(nfe.getMessage(), nfe);
			}
		}
		else if (name.equals("rewardLvls"))
		{
			final String[] propertySplit = value.split(",");
			if (propertySplit.length != 0)
			{
				_minLvl = Integer.parseInt(propertySplit[0]);
				_maxLvl = Integer.parseInt(propertySplit[1]);
			}
		}
		else if (name.equals("fighterBuffs"))
		{
			_fighterBuffs = parseBuffs(value);
		}
		else if (name.equals("mageBuffs"))
		{
			_mageBuffs = parseBuffs(value);
		}
		else if (name.equals("flagDelay"))
		{
			_flagDelay = Integer.parseInt(value);
		}
		else if (name.equals("hwidsLimit"))
		{
			_hwidsLimit = Integer.parseInt(value);
			_allotHwidsLimit = _hwidsLimit > 0;
			if (_allotHwidsLimit)
			{
				_zoneSessionId = DoubleSessionManager.FUNZONE_ID + getId();
				DoubleSessionManager.getInstance().registerEvent(_zoneSessionId);
				DoubleSessionManager.getInstance().clear(_zoneSessionId);
			}
		}
		else if (name.equals("isHideClanInfo"))
		{
			_hideClanInfo = Boolean.parseBoolean(value);
		}
		else if (name.equals("isHideTitles"))
		{
			_hideTitles = Boolean.parseBoolean(value);
		}
		else if (name.equals("isHideNames"))
		{
			_hideNames = Boolean.parseBoolean(value);
		}
		else if (name.equals("isEnableRelation"))
		{
			_isEnableRelation = Boolean.parseBoolean(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		if (character.getActingPlayer() != null && character.getActingPlayer().isInFightEvent() || character.isInPartyTournament() || /*character.checkInTournament() ||*/ character.getReflectionId() > 0)
		{
			return;
		}
		
		if (_allotHwidsLimit && character.isPlayer())
		{
			if (checkHWID(character.getActingPlayer()))
			{
				return;
			}
		}
		
		if (isPvpZone())
		{
			if (character.isPlayer())
			{
				if (!character.isInsideZone(ZoneId.PVP, this))
				{
					character.sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
				}
			}
		}

		if (character.isPlayer())
		{
			final Player activeChar = character.getActingPlayer();
			if (_classes != null && _classes.contains("" + activeChar.getClassId().getId()))
			{
				activeChar.getPersonalTasks().addTask(new TeleportTask(2000, null));
				activeChar.sendMessage("Your class is not allowed in the MultiFunction zone.");
				return;
			}
			
			checkTransFormations(activeChar);

			activeChar.setIsInPvpFunZone(true);

			if (!isPartyEnabled())
			{
				final Party party = activeChar.getParty();
				if (party != null)
				{
					party.removePartyMember(activeChar, Party.messageType.Expelled);
				}
			}
			
			for (final ItemInstance o : activeChar.getInventory().getItems())
			{
				if (o != null)
				{
					if (o.isEquipable() && o.isEquipped() && !checkItem(o))
					{
						final int slot = activeChar.getInventory().getSlotFromItem(o);
						activeChar.getInventory().unEquipItemInBodySlot(slot);
						activeChar.sendMessage(o.getName(activeChar.getLang()) + " unequiped because is not allowed inside this zone.");
					}
				}
			}
			clear(activeChar);
			buffPlayer(activeChar);
			if (_isPvpEnabled)
			{
				activeChar.stopPvpRegTask();
				activeChar.updatePvPFlag(1);
			}
			
			if (isPvpZone())
			{
				getPlayersInside().stream().filter(e -> e != null).forEach(e -> e.broadcastUserInfo(true));
			}
		}
	}
	
	@Override
	public void onPlayerLogoutInside(Player player)
	{
		if (player.isInFightEvent() || player.checkInTournament() || player.getReflectionId() > 0)
		{
			return;
		}
		if (_allotHwidsLimit)
		{
			removeHWIDInfo(player);
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		if (character.getActingPlayer() != null && character.getActingPlayer().isInFightEvent() || character.checkInTournament() || character.getReflectionId() > 0)
		{
			return;
		}
		
		if (isPvpZone())
		{
			if (character.isPlayer())
			{
				if (!character.isInsideZone(ZoneId.PVP, this))
				{
					character.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
				}
			}
		}

		if (character.isPlayer())
		{
			final Player activeChar = character.getActingPlayer();

			activeChar.setIsInPvpFunZone(activeChar.isInsideZoneExcluding(ZoneId.FUN_PVP, this));

			if (_isPvpEnabled && (_flagDelay > 0))
			{

				activeChar.setPvpFlagLasts(System.currentTimeMillis() + (_flagDelay * 1000L));
				activeChar.startPvPFlag();
			}
			
			if (isPvpZone())
			{
				getPlayersInside().stream().filter(e -> e != null).forEach(e -> e.broadcastRelationChanged());
				activeChar.broadcastUserInfo(true);
			}
		}
		
		if (_allotHwidsLimit && character.isPlayer())
		{
			removeHWIDInfo(character.getActingPlayer());
		}
	}
	
	@Override
	public void onDieInside(final Creature character)
	{
		if (character.getActingPlayer() != null && character.getActingPlayer().isInFightEvent() || character.checkInTournament() || character.getReflectionId() > 0)
		{
			return;
		}

		if (character.isPlayer())
		{
			final Player activeChar = character.getActingPlayer();
			if (_isNoRevive)
			{
				ThreadPoolManager.getInstance().schedule(new Runnable()
				{
					@Override
					public void run()
					{
						if (!isEnabled())
						{
							return;
						}

						activeChar.doRevive();
						heal(activeChar);
						if (_spawnLocs != null)
						{
							final int[] loc = _spawnLocs[Rnd.get(_spawnLocs.length)];
							activeChar.teleToLocation(loc[0] + Rnd.get(-_radius, _radius), loc[1] + Rnd.get(-_radius, _radius), loc[2], true, ReflectionManager.DEFAULT);
						}
					}
				}, _reviveDelay * 1000);
			}
		}
	}
	
	@Override
	public void onReviveInside(Creature character)
	{
		if (character.getActingPlayer() != null && character.getActingPlayer().isInFightEvent() || character.checkInTournament() || character.getReflectionId() > 0)
		{
			return;
		}

		if (character.isPlayer())
		{
			buffPlayer(character.getActingPlayer());
			if (_reviveNoblesse)
			{
				SkillsParser.getInstance().getInfo(1323, 1).getEffects(character.getActingPlayer(), character.getActingPlayer(), false);
			}
			if (_reviveHeal)
			{
				heal(character.getActingPlayer());
			}
		}
	}
	
	public boolean checkHWID(Player player)
	{
		if (!DoubleSessionManager.getInstance().tryAddPlayer(_zoneSessionId, player, _hwidsLimit))
		{
			player.getPersonalTasks().addTask(new TeleportTask(2000, null));
			return true;
		}
		return false;
	}
	
	private void removeHWIDInfo(Player player)
	{
		DoubleSessionManager.getInstance().removePlayer(_zoneSessionId, player);
	}
	
	private void clear(Player player)
	{
		if (player == null || player.isInFightEvent() || player.checkInTournament() || player.getReflectionId() > 0)
		{
			return;
		}
		
		if (_removeBuffs)
		{
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			if (_removePets)
			{
				final Summon pet = player.getSummon();
				if (pet != null)
				{
					pet.stopAllEffectsExceptThoseThatLastThroughDeath();
					pet.unSummon(player);
				}
			}
		}
		else
		{
			if (_removePets)
			{
				final Summon pet = player.getSummon();
				if (pet != null)
				{
					pet.unSummon(player);
				}
			}
		}
	}
	
	private void heal(Player activeChar)
	{
		activeChar.setCurrentHp(activeChar.getMaxHp());
		activeChar.setCurrentCp(activeChar.getMaxCp());
		activeChar.setCurrentMp(activeChar.getMaxMp());
	}
	
	public void givereward(Player killer, Player player)
	{
		if (killer == null || killer.isInFightEvent() || killer.checkInTournament() || killer.getReflectionId() > 0 || !isEnabled())
		{
			return;
		}
		
		if (killer.isInsideZone(ZoneId.FUN_PVP))
		{
			if (_zoneRewardChance != null)
			{
				if (killer.isInParty())
				{
					for (final Player pm : killer.getParty().getMembers())
					{
						if (pm != null)
						{
							if (!pm.isSameAddress(player) && pm.isInsideZone(ZoneId.FUN_PVP) && isValidRewardLvl(pm))
							{
								final int[] chance = _zoneRewardChance;
								for (int i = 0; i < chance.length; i++)
								{
									if (Rnd.chance(_zoneRewardChance[i]))
									{
										pm.addItem("Zone Reward", _zoneRewardId[i], _zoneRewardAmount[i], pm, true);
									}
								}
							}
						}
					}
				}
				else
				{
					if (isValidRewardLvl(killer))
					{
						final int[] chance = _zoneRewardChance;
						for (int i = 0; i < chance.length; i++)
						{
							if (Rnd.chance(_zoneRewardChance[i]))
							{
								killer.addItem("Zone Reward", _zoneRewardId[i], _zoneRewardAmount[i], killer, true);
							}
						}
					}
				}
			}
		}
	}
	
	public boolean checkSkill(Skill skill)
	{
		if (_skills != null && _skills.contains(skill.getId()))
		{
			return false;
		}
		return true;
	}
	
	public boolean checkItem(ItemInstance item)
	{
		if (!isEnabled())
		{
			return true;
		}
		final int o = item.getItem().getCrystalType();
		final int e = item.getEnchantLevel();
		
		if (_enchant != 0 && e >= _enchant)
		{
			return false;
		}
		
		if (_grades != null && _grades.contains(_gradeNames[o]))
		{
			return false;
		}
		
		if (_items != null && _items.contains("" + item.getId()))
		{
			return false;
		}
		return true;
	}
	
	private static int[][] parseItemsList(String line)
	{
		final String[] propertySplit = line.split(";");
		if (propertySplit.length == 0)
		{
			return null;
		}
		
		int i = 0;
		String[] valueSplit;
		final int[][] result = new int[propertySplit.length][];
		for (final String value : propertySplit)
		{
			valueSplit = value.split(",");
			if (valueSplit.length != 3)
			{
				return null;
			}
			
			result[i] = new int[3];
			try
			{
				result[i][0] = Integer.parseInt(valueSplit[0]);
			}
			catch (final NumberFormatException e)
			{
				return null;
			}
			try
			{
				result[i][1] = Integer.parseInt(valueSplit[1]);
			}
			catch (final NumberFormatException e)
			{
				return null;
			}
			try
			{
				result[i][2] = Integer.parseInt(valueSplit[2]);
			}
			catch (final NumberFormatException e)
			{
				return null;
			}
			i++;
		}
		return result;
	}
	
	private int[][] parseBuffs(String buffs)
	{
		if (buffs == null || buffs.isEmpty())
		{
			return null;
		}
		
		final StringTokenizer st = new StringTokenizer(buffs, ";");
		final int[][] realBuffs = new int[st.countTokens()][2];
		int index = 0;
		while (st.hasMoreTokens())
		{
			final String[] skillLevel = st.nextToken().split(",");
			final int[] realHourMin =
			{
			        Integer.parseInt(skillLevel[0]), Integer.parseInt(skillLevel[1])
			};
			realBuffs[index] = realHourMin;
			index++;
		}
		return realBuffs;
	}
	
	private void buffPlayer(Player player)
	{
		if (_giveNoblesse)
		{
			SkillsParser.getInstance().getInfo(1323, 1).getEffects(player, player, false);
		}
		
		if (_fighterBuffs != null && _mageBuffs != null)
		{
			int[][] buffs;
			if (player.isMageClass())
			{
				buffs = _mageBuffs;
			}
			else
			{
				buffs = _fighterBuffs;
			}
			
			giveBuffs(player, buffs, false);
			
			if (player.getSummon() != null)
			{
				giveBuffs(player, _fighterBuffs, true);
			}
		}
	}
	
	private boolean isValidRewardLvl(Player player)
	{
		if (player.getLevel() < _minLvl || player.getLevel() > _maxLvl)
		{
			return false;
		}
		return true;
	}
	
	private static void giveBuffs(final Player player, int[][] buffs, boolean petbuff)
	{
		Skill buff;
		for (final int[] buff1 : buffs)
		{
			buff = SkillsParser.getInstance().getInfo(buff1[0], buff1[1]);
			if (buff == null)
			{
				continue;
			}
			
			if (!petbuff)
			{
				buff.getEffects(player, player, false);
			}
			else
			{
				buff.getEffects(player, player.getSummon(), false);
			}
		}
	}
	
	public boolean isNoRestartZone()
	{
		if (!isEnabled())
		{
			return false;
		}
		return _isNoRestartZone;
	}
	
	public boolean isNoLogoutZone()
	{
		if (!isEnabled())
		{
			return false;
		}
		return _isNoLogoutZone;
	}
	
	public boolean canRevive()
	{
		if (!isEnabled())
		{
			return true;
		}
		return _isNoRevive;
	}

	public boolean canJoinParty(Player sender, Player receiver)
	{
		if (!receiver.isInPvpFunZone() && !sender.isInPvpFunZone())
		{
			return true;
		}
		return isPartyEnabled();
	}

	public boolean canAttackAllies()
	{
		if (!isEnabled())
		{
			return false;
		}
		return _canAttackAllies;
	}
	
	public boolean isPvpZone()
	{
		return _isPvpZone;
	}

	public boolean isPartyEnabled()
	{
		if (!isEnabled())
		{
			return true;
		}
		return _allowParty;
	}
	
	public boolean canUseCbBuffs()
	{
		if (!isEnabled())
		{
			return true;
		}
		return _canUseCommunityBuffs;
	}
	
	public boolean canUseCbTeleports()
	{
		if (!isEnabled())
		{
			return true;
		}
		return _canUseCommunityTp;
	}
	
	public boolean allowPvpKills()
	{
		return _allowPvpKills;
	}

	public boolean isHideClanInfo()
	{
		if (!isEnabled())
		{
			return false;
		}
		return _hideClanInfo;
	}

	public boolean isHideNames()
	{
		if (!isEnabled())
		{
			return false;
		}
		return _hideNames;
	}

	public boolean isHideTitles()
	{
		if (!isEnabled())
		{
			return false;
		}
		return _hideTitles;
	}

	public String getVisibleTitle(Player player, Player viewer, String currentTitle, boolean toMe)
	{
		if (isHideTitles() && !toMe)
		{
			return "";
		}
		return currentTitle;
	}

	public String getVisibleName(Player player, String currentName, boolean toMe)
	{
		if (isHideNames() && !toMe)
		{
			return "Player";
		}
		return currentName;
	}
	
	private void checkTransFormations(Player player)
	{
		final var blockList = _blockTransformations;
		if (blockList.isEmpty())
		{
			return;
		}
		
		if (player.getTransformationId() > 0)
		{
			if (blockList.contains(player.getTransformationId()) || blockList.contains(-1))
			{
				player.untransform();
			}
		}
	}
	
	public boolean isBlockTransfroms()
	{
		if (!isEnabled())
		{
			return false;
		}
		return !_blockTransformations.isEmpty();
	}
	
	public boolean isBlockedTransfrom(int id)
	{
		if (!isEnabled() || _blockTransformations.isEmpty())
		{
			return false;
		}
		return _blockTransformations.contains(id) || _blockTransformations.contains(-1);
	}
	
	public boolean isEnableRelation()
	{
		return _isEnableRelation;
	}
}