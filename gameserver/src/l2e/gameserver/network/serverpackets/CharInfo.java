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

import l2e.gameserver.Config;
import l2e.gameserver.instancemanager.CursedWeaponsManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Decoy;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.CubicInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.matching.MatchingRoom;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.zone.type.FunPvpZone;

public class CharInfo extends GameServerPacket
{
	private final Player _activeChar;
	private final Player _player;
	private final String _name;
	private final String _title;
	private final Inventory _inv;
	private int _objId;
	private int _x, _y, _z, _heading;
	private final double _mAtkSpd, _pAtkSpd;
	private final int _runSpd;
	private final int _walkSpd;
	private final double _moveMultiplier;
	private int _vehicleId, _airShipHelm;
	private final boolean _isPartyRoomLeader;
	private final int[] _visualSlots;
	private final boolean _isHideClanInfo;
	private int _abnormalEffectMask;
	private int _abnormalEffectMask2;
	
	public CharInfo(Player cha, Player viewer)
	{
		_activeChar = cha;
		_visualSlots = cha.getCharVisualSlots(viewer);
		_player = viewer;
		_objId = _activeChar.getObjectId();
		_inv = _activeChar.getInventory();
		_name = getName(_activeChar, _player);
		_title = getTitle(_activeChar, _player);
		_isHideClanInfo = getHideClanInfo(_activeChar);
		if ((_activeChar.getVehicle() != null) && (_activeChar.getInVehiclePosition() != null))
		{
			_x = _activeChar.getInVehiclePosition().getX();
			_y = _activeChar.getInVehiclePosition().getY();
			_z = _activeChar.getInVehiclePosition().getZ();
			_vehicleId = _activeChar.getVehicle().getObjectId();
			if (_activeChar.isInAirShip() && _activeChar.getAirShip().isCaptain(_activeChar))
			{
				_airShipHelm = _activeChar.getAirShip().getHelmItemId();
			}
			else
			{
				_airShipHelm = 0;
			}
		}
		else
		{
			_x = _activeChar.getX();
			_y = _activeChar.getY();
			_z = _activeChar.getZ();
			_vehicleId = 0;
			_airShipHelm = 0;
		}
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = (int) _activeChar.getPAtkSpd();
		_moveMultiplier = _activeChar.getMovementSpeedMultiplier();
		_runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (_activeChar.getWalkSpeed() / _moveMultiplier);
		_invisible = cha.isInvisible();
		_isPartyRoomLeader = cha.getMatchingRoom() != null && cha.getMatchingRoom().getType() == MatchingRoom.PARTY_MATCHING && cha.getMatchingRoom().getLeader() == cha;
		_abnormalEffectMask = _activeChar.getAbnormalEffectMask();
		if ((_activeChar != null) && _invisible && _activeChar.canOverrideCond(PcCondOverride.SEE_ALL_PLAYERS))
		{
			_abnormalEffectMask |= AbnormalEffect.STEALTH.getMask();
		}
		_abnormalEffectMask2 = _activeChar.getAbnormalEffectMask2();
		if (_activeChar.isGM() && _activeChar.isOnlyInvul())
		{
			_abnormalEffectMask2 |= AbnormalEffect.S_INVINCIBLE.getMask();
		}
	}

	public CharInfo(Decoy decoy)
	{
		this(decoy.getActingPlayer(), decoy.getActingPlayer());
		_vehicleId = 0;
		_airShipHelm = 0;
		_objId = decoy.getObjectId();
		_x = decoy.getX();
		_y = decoy.getY();
		_z = decoy.getZ();
		_heading = decoy.getHeading();
	}

	private String getName(Player player, Player viewer)
	{
		if (player.isInFightEvent())
		{
			return player.getFightEvent().getVisibleName(viewer != null ? viewer : player, player.getAppearance().getVisibleName(), false);
		}

		if (player.isInPvpFunZone())
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
			if (zone != null)
			{
				return zone.getVisibleName(viewer != null ? viewer : player, player.getAppearance().getVisibleName(), false);
			}
		}
		return player.getAppearance().getVisibleName();
	}

	private String getTitle(Player player, Player viewer)
	{
		if (viewer != null && viewer.isHideTitles())
		{
			return "";
		}
		
		if (player.isInFightEvent())
		{
			return player.getFightEvent().getVisibleTitle(player, viewer != null ? viewer : player, player.getAppearance().getVisibleTitle(), false);
		}

		if (player.isInPvpFunZone())
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
			if (zone != null)
			{
				return zone.getVisibleTitle(player, viewer != null ? viewer : player, player.getAppearance().getVisibleTitle(), false);
			}
		}
		return player.getAppearance().getVisibleTitle();
	}

	private boolean getHideClanInfo(Player player)
	{
		if (player.isInFightEvent())
		{
			return player.getFightEvent().isHideClanInfo();
		}

		if (player.isInPvpFunZone())
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
			if (zone != null)
			{
				return zone.isHideClanInfo();
			}
		}
		return false;
	}

	@Override
	protected final void writeImpl()
	{
		boolean gmSeeInvis = false;

		if (_invisible)
		{
			final Player activeChar = getClient().getActiveChar();
			if ((activeChar != null) && activeChar.canOverrideCond(PcCondOverride.SEE_ALL_PLAYERS))
			{
				gmSeeInvis = true;
			}
		}

		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_vehicleId);
		writeD(_objId);
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeS("Player");
			writeD(Config.OLY_ANTI_FEED_RACE);
			writeD(Config.OLY_ANTI_FEED_GENDER);
		}
		else
		{
			writeS(_name);
			writeD(_activeChar.getRace().ordinal());
			writeD(_activeChar.getAppearance().getSex() ? 0x01 : 0x00);
		}

		if (_activeChar.getClassIndex() == 0)
		{
			writeD(_activeChar.getClassId().getId());
		}
		else
		{
			writeD(_activeChar.getBaseClass());
		}
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_UNDER));
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_HEAD));
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeD(Config.OLY_ANTI_FEED_WEAPON_RIGHT);
		}
		else
		{
			writeD(_airShipHelm == 0 ? _visualSlots[0] : _airShipHelm);
		}
		
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeD(Config.OLY_ANTI_FEED_WEAPON_LEFT);
		}
		else
		{
			writeD(_airShipHelm == 0 ? _visualSlots[1] : _airShipHelm);
		}
			
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeD(Config.OLY_ANTI_FEED_GLOVES);
			writeD(Config.OLY_ANTI_FEED_CHEST);
			writeD(Config.OLY_ANTI_FEED_LEGS);
			writeD(Config.OLY_ANTI_FEED_FEET);
		}
		else
		{
			writeD(_visualSlots[2]);
			writeD(_visualSlots[3]);
			writeD(_visualSlots[4]);
			writeD(_visualSlots[5]);
		}
			
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeD(Config.OLY_ANTI_FEED_CLOAK);
		}
		else
		{
			writeD(_visualSlots[6]);
		}
			
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeD(Config.OLY_ANTI_FEED_RIGH_HAND_ARMOR);
		}
		else
		{
			writeD(_visualSlots[0]);
		}
			
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeD(Config.OLY_ANTI_FEED_HAIR_MISC_1);
		}
		else
		{
			writeD(_visualSlots[7]);
		}
		
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeD(Config.OLY_ANTI_FEED_HAIR_MISC_2);
		}
		else
		{
			writeD(_visualSlots[8]);
		}
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_RBRACELET));
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_LBRACELET));
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO1));
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO2));
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO3));
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO4));
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO5));
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO6));
		writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_BELT));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_UNDER));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_HEAD));
		writeD(_airShipHelm == 0 ? _visualSlots[10] : _airShipHelm);
		writeD(_airShipHelm == 0 ? _inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LHAND) : 0x00);
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_GLOVES));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_CHEST));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LEGS));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_FEET));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_CLOAK));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR2));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RBRACELET));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LBRACELET));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO1));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO2));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO3));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO4));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO5));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO6));
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_BELT));
		writeD(0x00);
		writeD(0x01);
		writeD(_activeChar.getPvpFlag());
		writeD(_activeChar.getKarma());
		writeD((int) _mAtkSpd);
		writeD((int) _pAtkSpd);
		writeD(0x00);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeF(_activeChar.getMovementSpeedMultiplier());
		writeF(_activeChar.getAttackSpeedMultiplier());
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeF(Config.OLY_ANTI_FEED_CLASS_RADIUS);
			writeF(Config.OLY_ANTI_FEED_CLASS_HEIGHT);
		}
		else
		{
			writeF(_activeChar.getColRadius());
			writeF(_activeChar.getColHeight());
		}
		writeD(_activeChar.getAppearance().getHairStyle());
		writeD(_activeChar.getAppearance().getHairColor());
		writeD(_activeChar.getAppearance().getFace());
		writeS(gmSeeInvis ? "Invisible" : _title);
		if (!_activeChar.isCursedWeaponEquipped())
		{
			writeD(_isHideClanInfo ? 0x00 : _activeChar.getClanId());
			writeD(_isHideClanInfo ? 0x00 : _activeChar.getClanCrestId());
			writeD(_isHideClanInfo ? 0x00 : _activeChar.getAllyId());
			writeD(_isHideClanInfo ? 0x00 : _activeChar.getAllyCrestId());
		}
		else
		{
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
		}
		writeC(_activeChar.isSitting() ? 0x00 : 0x01);
		writeC(_activeChar.isRunning() ? 0x01 : 0x00);
		writeC(_activeChar.isInCombat() ? 0x01 : 0x00);
		writeC(!_activeChar.isInOlympiadMode() && _activeChar.isAlikeDead() ? 0x01 : 0x00);
		writeC(!gmSeeInvis && _invisible ? 0x01 : 0x00);
		writeC(_activeChar.getMountType().ordinal());
		writeC(_activeChar.getPrivateStoreType());
		writeH(_activeChar.getCubicsSize());
		for (final CubicInstance c : _activeChar.getCubics().values())
		{
			writeH(c.getId());
		}
		writeC(_isPartyRoomLeader ? 0x01 : 0x00);
		writeD(_abnormalEffectMask);
		writeC(_activeChar.isInWater() ? 0x01 : _activeChar.isFlyingMounted() ? 0x02 : 0x00);
		if ((Config.ENABLE_OLY_FEED) && (_activeChar.isInOlympiadMode()))
		{
			writeH(Config.OLY_ANTI_FEED_PLAYER_HAVE_RECS);
		}
		else
		{
			writeH(_activeChar.getRecommendation().getRecomHave());
		}
		writeD(_activeChar.getMountNpcId() + 1000000);
		writeD(_activeChar.getClassId().getId());
		writeD(0x00);
		writeC(_activeChar.isMounted() || (_airShipHelm != 0) ? 0x00 : _visualSlots[9]);
		writeC(_activeChar.getTeam());
		writeD(_activeChar.getClanCrestLargeId());
		writeC(_activeChar.isNoble() ? 0x01 : 0x00);
		writeC(_activeChar.isHero() || (_activeChar.isGM() && _activeChar.getAccessLevel().allowHeroAura()) || _inv.getHeroStatus() >= 1 ? 0x01 : 0x00);
		writeC(_activeChar.isFishing() ? 0x01 : 0x00);
		writeD(_activeChar.getFishx());
		writeD(_activeChar.getFishy());
		writeD(_activeChar.getFishz());
		writeD(_activeChar.isInFightEvent() ? _activeChar.getFightEvent().getVisibleNameColor(_activeChar, _activeChar.getAppearance().getNameColor(), false) : _activeChar.getAppearance().getNameColor());
		writeD(_heading);
		writeD(_activeChar.getPledgeClass());
		writeD(_activeChar.getPledgeType());
		writeD(_activeChar.getAppearance().getTitleColor());
		writeD(_activeChar.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquippedId()) : 0x00);
		writeD(_activeChar.getClanId() > 0 ? _activeChar.getClan().getReputationScore() : 0x00);
		writeD(_activeChar.getTransformationId());
		writeD(_activeChar.getActiveAura() > 0 ? _activeChar.getActiveAura() : _activeChar.getAgathionId());
		writeD(0x01);
		writeD(_abnormalEffectMask2);
	}
}