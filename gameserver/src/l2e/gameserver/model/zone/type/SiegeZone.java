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

import l2e.gameserver.Config;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.instancemanager.CHSiegeManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.instancemanager.DoubleSessionManager;
import l2e.gameserver.instancemanager.FortManager;
import l2e.gameserver.instancemanager.FortSiegeManager;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.MountType;
import l2e.gameserver.model.TeleportWhereType;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.SiegeSummonInstance;
import l2e.gameserver.model.actor.instance.player.impl.TeleportTask;
import l2e.gameserver.model.entity.Fort;
import l2e.gameserver.model.entity.FortSiege;
import l2e.gameserver.model.entity.Siegable;
import l2e.gameserver.model.entity.clanhall.SiegableHall;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.zone.AbstractZoneSettings;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.model.zone.ZoneType;
import l2e.gameserver.network.SystemMessageId;

public class SiegeZone extends ZoneType
{
	private int _fortId = 0;
	private int _castleId = 0;
	private int _clanHallId = 0;
	private boolean _allotHwidsLimit = false;
	private int _hwidsLimit = 0;
	private int _zoneSessionId = 0;
	
	public SiegeZone(int id)
	{
		super(id);
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		setSettings(settings);
	}
	
	public final class Settings extends AbstractZoneSettings
	{
		private int _siegableId = -1;
		private Siegable _siege = null;
		private boolean _isActiveSiege = false;

		public Settings()
		{
		}

		public int getSiegeableId()
		{
			return _siegableId;
		}

		protected void setSiegeableId(int id)
		{
			_siegableId = id;
		}

		public Siegable getSiege()
		{
			return _siege;
		}

		public void setSiege(Siegable s)
		{
			_siege = s;
		}

		public boolean isActiveSiege()
		{
			return _isActiveSiege;
		}

		public void setActiveSiege(boolean val)
		{
			_isActiveSiege = val;
		}

		@Override
		public void clear()
		{
			_siegableId = -1;
			_siege = null;
			_isActiveSiege = false;
		}
	}

	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			getSettings().setSiegeableId(Integer.parseInt(value));
			_castleId = Integer.parseInt(value);
		}
		else if (name.equals("fortId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			getSettings().setSiegeableId(Integer.parseInt(value));
			_fortId = Integer.parseInt(value);
		}
		else if (name.equals("clanHallId"))
		{
			if (getSettings().getSiegeableId() != -1)
			{
				throw new IllegalArgumentException("Siege object already defined!");
			}
			getSettings().setSiegeableId(Integer.parseInt(value));
			_clanHallId = Integer.parseInt(value);
			final SiegableHall hall = CHSiegeManager.getInstance().getConquerableHalls().get(getSettings().getSiegeableId());
			if (hall == null)
			{
				_log.warn("SiegeZone: Siegable clan hall with id " + value + " does not exist!");
			}
			else
			{
				hall.setSiegeZone(this);
			}
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (getSettings().isActiveSiege())
		{
			if (character.isPlayer())
			{
				final Player player = character.getActingPlayer();
				if (player == null)
				{
					return;
				}
				
				if (player.getFarmSystem().isAutofarming())
				{
					player.getFarmSystem().stopFarmTask(false);
				}
				
				if (_allotHwidsLimit && checkHWID(player))
				{
					return;
				}
				
				if (player.isRegisteredOnThisSiegeField(getSettings().getSiegeableId()))
				{
					player.setIsInSiege(true);
					if (getSettings().getSiege().giveFame() && (getSettings().getSiege().getFameFrequency() > 0))
					{
						player.startFameTask(getSettings().getSiege().getFameFrequency() * 1000, getSettings().getSiege().getFameAmount());
					}
				}

				character.sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
				if (!Config.ALLOW_WYVERN_DURING_SIEGE && (player.getMountType() == MountType.WYVERN))
				{
					player.sendPacket(SystemMessageId.AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_WYVERN);
					player.enteredNoLanding(5);
				}
				
				if (Config.ALLOW_BLOCK_TRANSFORMS_AT_SIEGE && player.getTransformationId() > 0)
				{
					if (Config.LIST_BLOCK_TRANSFORMS_AT_SIEGE.contains(player.getTransformationId()))
					{
						player.untransform();
					}
				}
			}
		}
	}

	@Override
	protected void onExit(Creature character)
	{
		if (getSettings().isActiveSiege())
		{
			if (character.isPlayer())
			{
				final Player player = character.getActingPlayer();
				if (player != null)
				{
					player.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
					if (player.getMountType() == MountType.WYVERN)
					{
						player.exitedNoLanding();
					}
					
					if (player.getPvpFlag() == 0)
					{
						player.startPvPFlag();
					}
					
					if (_allotHwidsLimit)
					{
						removeHWIDInfo(player);
					}
				}
			}
		}
		if (character.isPlayer())
		{
			final Player activeChar = character.getActingPlayer();
			activeChar.stopFameTask();
			activeChar.setIsInSiege(false);

			if ((getSettings().getSiege() instanceof FortSiege) && (activeChar.getInventory().getItemByItemId(9819) != null))
			{
				final Fort fort = FortManager.getInstance().getFortById(getSettings().getSiegeableId());
				if (fort != null)
				{
					FortSiegeManager.getInstance().dropCombatFlag(activeChar, fort.getId());
				}
				else
				{
					final int slot = activeChar.getInventory().getSlotFromItem(activeChar.getInventory().getItemByItemId(9819));
					activeChar.getInventory().unEquipItemInBodySlot(slot);
					activeChar.destroyItem("CombatFlag", activeChar.getInventory().getItemByItemId(9819), null, true);
				}
			}
		}

		if (character instanceof SiegeSummonInstance)
		{
			((SiegeSummonInstance) character).unSummon(((SiegeSummonInstance) character).getOwner());
		}
	}
	
	@Override
	public void onPlayerLogoutInside(Player player)
	{
		if (getSettings().isActiveSiege() && _allotHwidsLimit)
		{
			removeHWIDInfo(player);
		}
	}

	@Override
	public void onDieInside(Creature character)
	{
		if (getSettings().isActiveSiege())
		{
			if (character.isPlayer() && character.getActingPlayer().isRegisteredOnThisSiegeField(getSettings().getSiegeableId()))
			{
				int lvl = 1;
				final Effect e = character.getFirstEffect(5660);
				if (e != null)
				{
					lvl = Math.min(lvl + e.getSkill().getLevel(), 5);
				}

				final Skill skill = SkillsParser.getInstance().getInfo(5660, lvl);
				if (skill != null)
				{
					skill.getEffects(character, character, false);
				}
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

	public void updateZoneStatusForCharactersInside()
	{
		if (_allotHwidsLimit)
		{
			DoubleSessionManager.getInstance().clear(_zoneSessionId);
		}
		
		if (getSettings().isActiveSiege())
		{
			for (final Creature character : getCharactersInside())
			{
				if (character != null)
				{
					onEnter(character);
				}
			}
		}
		else
		{
			Player player;
			for (final Creature character : getCharactersInside())
			{
				if (character == null)
				{
					continue;
				}

				if (character.isPlayer())
				{
					player = character.getActingPlayer();
					if (player != null)
					{
						player.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
						player.stopFameTask();
						if (player.getMountType() == MountType.WYVERN)
						{
							player.exitedNoLanding();
						}
					}
				}
				
				if (character instanceof SiegeSummonInstance)
				{
					((SiegeSummonInstance) character).unSummon(((SiegeSummonInstance) character).getOwner());
				}
			}
		}
	}

	public void announceToPlayers(String message)
	{
		for (final Player player : getPlayersInside())
		{
			if (player != null)
			{
				player.sendMessage(message);
			}
		}
	}

	public int getSiegeObjectId()
	{
		return getSettings().getSiegeableId();
	}

	public boolean isActive()
	{
		return getSettings().isActiveSiege();
	}

	public void setIsActive(boolean val)
	{
		getSettings().setActiveSiege(val);
		if (val)
		{
			if (getCastleId() > 0)
			{
				final var castle = CastleManager.getInstance().getCastleById(getCastleId());
				if (castle != null && castle.getTemplate() != null)
				{
					_hwidsLimit = castle.getTemplate().getSiegeLimitPlayers();
					_allotHwidsLimit = _hwidsLimit > 0;
					if (_allotHwidsLimit)
					{
						_zoneSessionId = DoubleSessionManager.CASTLE_ID + getCastleId();
						DoubleSessionManager.getInstance().registerEvent(_zoneSessionId);
					}
				}
			}
			else if (getFortId() > 0)
			{
				_hwidsLimit = FortSiegeManager.getInstance().getFortHwidLimit();
				_allotHwidsLimit = _hwidsLimit > 0;
				if (_allotHwidsLimit)
				{
					_zoneSessionId = DoubleSessionManager.FORT_ID + getFortId();
					DoubleSessionManager.getInstance().registerEvent(_zoneSessionId);
				}
			}
			else if (getClanHallId() > 0)
			{
				_hwidsLimit = Config.CLAN_HALL_HWID_LIMIT;
				_allotHwidsLimit = _hwidsLimit > 0;
				if (_allotHwidsLimit)
				{
					_zoneSessionId = DoubleSessionManager.CLANHALL_ID + getClanHallId();
					DoubleSessionManager.getInstance().registerEvent(_zoneSessionId);
				}
			}
			addZoneId(ZoneId.PVP);
			addZoneId(ZoneId.SIEGE);
			addZoneId(ZoneId.NO_SUMMON_FRIEND);
		}
		else
		{
			getZoneId().clear();
			if (_allotHwidsLimit)
			{
				DoubleSessionManager.getInstance().clear(_zoneSessionId);
			}
		}
	}

	public void setSiegeInstance(Siegable siege)
	{
		getSettings().setSiege(siege);
	}

	public void banishForeigners(int owningClanId)
	{
		final TeleportWhereType type = TeleportWhereType.TOWN;
		for (final Player temp : getPlayersInside())
		{
			if (temp.getClanId() == owningClanId)
			{
				continue;
			}

			temp.teleToLocation(type, true, ReflectionManager.DEFAULT);
		}
	}
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	public int getFortId()
	{
		return _fortId;
	}
	
	public int getClanHallId()
	{
		return _clanHallId;
	}
	
	public boolean isAttackSameSiegeSide()
	{
		if (getCastleId() > 0)
		{
			final var castle = CastleManager.getInstance().getCastleById(getCastleId());
			if (castle != null && castle.getTemplate() != null)
			{
				return castle.getTemplate().canAttackSameSiegeSide();
			}
		}
		return true;
	}
	
	public void getCastlePvpReward(Player killer, Player target)
	{
		if (getCastleId() > 0)
		{
			final var castle = CastleManager.getInstance().getCastleById(getCastleId());
			if (castle != null && castle.getTemplate() != null)
			{
				castle.getTemplate().checkCastlePvpReward(killer, target);
			}
		}
	}
}