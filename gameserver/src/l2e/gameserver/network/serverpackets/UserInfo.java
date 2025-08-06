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

import l2e.gameserver.data.parser.ExperienceParser;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.instancemanager.CursedWeaponsManager;
import l2e.gameserver.instancemanager.TerritoryWarManager;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.Elementals;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.CubicInstance;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.matching.MatchingRoom;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.zone.type.FunPvpZone;

public final class UserInfo extends GameServerPacket
{
	private final Player _activeChar;
	private final String _name;
	private String _title;
	private int _relation;
	private int _airShipHelm;
	private int _abnormalEffectMask;
	private int _abnormalEffectMask2;
	private final boolean _partyRoom;
	
	private final int _runSpd, _walkSpd;
	private final int _swimRunSpd, _swimWalkSpd;
	private final int _flyRunSpd, _flyWalkSpd;
	private final double _moveMultiplier;
	private final int[] _visualSlots;
	private final boolean _isPreview;

	public UserInfo(Player character, int[] visualSlots)
	{
		_activeChar = character;
		_name = getName(_activeChar);
		_title = getTitle(_activeChar);
		if (_activeChar.isInvisible())
		{
			_title = "Invisible";
		}
		if (_activeChar.isPolymorphed() && !_activeChar.isInFightEvent())
		{
			final NpcTemplate polyObj = NpcsParser.getInstance().getTemplate(_activeChar.getPolyId());
			if (polyObj != null)
			{
				_title += " - " + polyObj.getName(null);
			}
		}
		_abnormalEffectMask = _activeChar.getAbnormalEffectMask();
		if (_activeChar.isInvisible())
		{
			_abnormalEffectMask |= AbnormalEffect.STEALTH.getMask();
		}
		
		_abnormalEffectMask2 = _activeChar.getAbnormalEffectMask2();
		if (_activeChar.isGM() && _activeChar.isOnlyInvul())
		{
			_abnormalEffectMask2 |= AbnormalEffect.S_INVINCIBLE.getMask();
		}
		
		_visualSlots = visualSlots != null ? visualSlots : character.getUserVisualSlots();
		_isPreview = visualSlots != null;
		_moveMultiplier = character.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(character.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(character.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(character.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(character.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = character.isFlying() ? _runSpd : 0;
		_flyWalkSpd = character.isFlying() ? _walkSpd : 0;
		_partyRoom = character.getMatchingRoom() != null && character.getMatchingRoom().getType() == MatchingRoom.PARTY_MATCHING && character.getMatchingRoom().getLeader() == character;
		final int _territoryId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(character);
		_relation = _activeChar.isClanLeader() ? 0x40 : 0;
		if (_activeChar.getSiegeState() == 1)
		{
			if (_territoryId == 0)
			{
				_relation |= 0x180;
			}
			else
			{
				_relation |= 0x1000;
			}
		}
		if (_activeChar.getSiegeState() == 2)
		{
			_relation |= 0x80;
		}
		
		if (_activeChar.isInAirShip() && _activeChar.getAirShip().isCaptain(_activeChar))
		{
			_airShipHelm = _activeChar.getAirShip().getHelmItemId();
		}
		else
		{
			_airShipHelm = 0;
		}
	}
	
	private String getName(Player player)
	{
		if (player.isInFightEvent())
		{
			return player.getFightEvent().getVisibleName(player, player.getName(null), true);
		}

		if (player.isInPvpFunZone())
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
			if (zone != null)
			{
				return zone.getVisibleName(player, player.getName(null), true);
			}
		}
		return player.getName(null);
	}

	private String getTitle(Player player)
	{
		if (player.isInFightEvent())
		{
			return player.getFightEvent().getVisibleTitle(player, player, player.getTitle(null), true);
		}

		if (player.isInPvpFunZone())
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
			if (zone != null)
			{
				return zone.getVisibleTitle(player, player, player.getTitle(null), true);
			}
		}
		return player.getTitle(null);
	}

	@Override
	protected final void writeImpl()
	{
		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
		writeD(_activeChar.getVehicle() != null ? _activeChar.getVehicle().getObjectId() : 0x00);
		writeD(_activeChar.getObjectId());
		writeS(_name);
		writeD(_activeChar.getRace().ordinal());
		writeD(_activeChar.getAppearance().getSex() ? 0x01 : 0x00);
		writeD(_activeChar.getBaseClass());
		writeD(_activeChar.getLevel());
		writeQ(_activeChar.getExp());
		writeF((float) (_activeChar.getExp() - ExperienceParser.getInstance().getExpForLevel(_activeChar.getLevel())) / (ExperienceParser.getInstance().getExpForLevel(_activeChar.getLevel() + 1) - ExperienceParser.getInstance().getExpForLevel(_activeChar.getLevel())));
		writeD(_activeChar.getSTR());
		writeD(_activeChar.getDEX());
		writeD(_activeChar.getCON());
		writeD(_activeChar.getINT());
		writeD(_activeChar.getWIT());
		writeD(_activeChar.getMEN());
		writeD((int) _activeChar.getMaxHp());
		writeD((int) _activeChar.getCurrentHp());
		writeD((int) _activeChar.getMaxMp());
		writeD((int) _activeChar.getCurrentMp());
		writeD(_activeChar.getSp());
		writeD(_activeChar.getCurrentLoad());
		writeD(_activeChar.getMaxLoad());
		writeD(_activeChar.getActiveWeaponItem() != null ? 40 : 20);
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_UNDER));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
		writeD(_airShipHelm == 0 ? _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND) : _airShipHelm);
		writeD(_airShipHelm == 0 ? _isPreview ? 0x00 : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND) : 0x00);
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CLOAK));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR2));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RBRACELET));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LBRACELET));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO1));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO2));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO3));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO4));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO5));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO6));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BELT));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_UNDER));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_REAR));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LEAR));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_NECK));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_RFINGER));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LFINGER));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_HEAD));
		writeD(_airShipHelm == 0 ? _visualSlots[0] : _airShipHelm);
		writeD(_airShipHelm == 0 ? _visualSlots[1] : _airShipHelm);
		writeD(_visualSlots[2]);
		writeD(_visualSlots[3]);
		writeD(_visualSlots[4]);
		writeD(_visualSlots[5]);
		writeD(_visualSlots[6]);
		writeD(_visualSlots[0]);
		writeD(_visualSlots[7]);
		writeD(_visualSlots[8]);
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_RBRACELET));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_LBRACELET));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO1));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO2));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO3));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO4));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO5));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO6));
		writeD(_activeChar.getInventory().getPaperdollItemDisplayId(Inventory.PAPERDOLL_BELT));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_UNDER));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_REAR));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LEAR));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_NECK));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RFINGER));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LFINGER));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HEAD));
		writeD(_airShipHelm == 0 ? _visualSlots[10] : _airShipHelm);
		writeD(_airShipHelm == 0 ? _isPreview ? 0x00 : _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LHAND) : 0x00);
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_GLOVES));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_CHEST));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LEGS));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_FEET));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_CLOAK));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR));
		writeD(_isPreview ? 0x00 : _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR2));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RBRACELET));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LBRACELET));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO1));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO2));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO3));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO4));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO5));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO6));
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_BELT));
		writeD(_activeChar.getInventory().getMaxTalismanCount());
		writeD(_activeChar.getInventory().getCloakStatus());
		writeD((int) _activeChar.getPAtk(null));
		writeD((int) _activeChar.getPAtkSpd());
		writeD((int) _activeChar.getPDef(null));
		writeD(_activeChar.getEvasionRate(null));
		writeD(_activeChar.getAccuracy());
		writeD((int) _activeChar.getCriticalHit(null, null));
		writeD((int) _activeChar.getMAtk(null, null));
		writeD((int) _activeChar.getMAtkSpd());
		writeD((int) _activeChar.getPAtkSpd());
		writeD((int) _activeChar.getMDef(null, null));
		writeD(_activeChar.getPvpFlag());
		writeD(_activeChar.getKarma());
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd);
		writeD(_swimWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(_moveMultiplier);
		writeF(_activeChar.getAttackSpeedMultiplier());
		writeF(_activeChar.getColRadius());
		writeF(_activeChar.getColHeight());
		writeD(_activeChar.getAppearance().getHairStyle());
		writeD(_activeChar.getAppearance().getHairColor());
		writeD(_activeChar.getAppearance().getFace());
		writeD(_activeChar.isGameMaster() ? 0x01 : 0x00);
		writeS(_title);
		writeD(_activeChar.getClanId());
		writeD(_activeChar.getClanCrestId());
		writeD(_activeChar.getAllyId());
		writeD(_activeChar.getAllyCrestId());
		writeD(_relation);
		writeC(_activeChar.getMountType().ordinal());
		writeC(_activeChar.getPrivateStoreType());
		writeC(_activeChar.hasDwarvenCraft() ? 0x01 : 0x00);
		writeD(_activeChar.getPkKills());
		writeD(_activeChar.getPvpKills());
		writeH(_activeChar.getCubicsSize());
		for (final CubicInstance c : _activeChar.getCubics().values())
		{
			writeH(c.getId());
		}
		writeC(_partyRoom ? 0x01 : 0x00);
		writeD(_abnormalEffectMask);
		writeC(_activeChar.isInWater() ? 0x01 : _activeChar.isFlyingMounted() ? 0x02 : 0x00);
		writeD(_activeChar.getClanPrivileges());
		writeH(_activeChar.getRecommendation().getRecomLeft());
		writeH(_activeChar.getRecommendation().getRecomHave());
		writeD(_activeChar.getMountNpcId() > 0 ? _activeChar.getMountNpcId() + 1000000 : 0x00);
		writeH(_activeChar.getInventoryLimit());
		writeD(_activeChar.getClassId().getId());
		writeD(0x00);
		writeD((int) _activeChar.getMaxCp());
		writeD((int) _activeChar.getCurrentCp());
		writeC(_activeChar.isMounted() || (_airShipHelm != 0) ? 0x00 : _visualSlots[9]);
		writeC(_activeChar.getTeam());
		writeD(_activeChar.getClanCrestLargeId());
		writeC(_activeChar.isNoble() ? 0x01 : 0x00);
		writeC(_activeChar.isHero() || (_activeChar.isGM() && _activeChar.getAccessLevel().allowHeroAura()) || _activeChar.getInventory().getHeroStatus() >= 1 ? 0x01 : 0x00);
		writeC(_activeChar.isFishing() ? 0x01 : 0x00);
		writeD(_activeChar.getFishx());
		writeD(_activeChar.getFishy());
		writeD(_activeChar.getFishz());
		writeD(_activeChar.isInFightEvent() ? _activeChar.getFightEvent().getVisibleNameColor(_activeChar, _activeChar.getAppearance().getNameColor(), true) : _activeChar.getAppearance().getNameColor());
		writeC(_activeChar.isRunning() ? 0x01 : 0x00);
		writeD(_activeChar.getPledgeClass());
		writeD(_activeChar.getPledgeType());
		writeD(_activeChar.getAppearance().getTitleColor());
		writeD(_activeChar.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquippedId()) : 0x00);
		writeD(_activeChar.getTransformationId());
		final byte attackAttribute = _activeChar.getAttackElement();
		writeH(attackAttribute);
		writeH(_activeChar.getAttackElementValue(attackAttribute));
		writeH(_activeChar.getDefenseElementValue(Elementals.FIRE));
		writeH(_activeChar.getDefenseElementValue(Elementals.WATER));
		writeH(_activeChar.getDefenseElementValue(Elementals.WIND));
		writeH(_activeChar.getDefenseElementValue(Elementals.EARTH));
		writeH(_activeChar.getDefenseElementValue(Elementals.HOLY));
		writeH(_activeChar.getDefenseElementValue(Elementals.DARK));
		writeD(_activeChar.getActiveAura() > 0 ? _activeChar.getActiveAura() : _activeChar.getAgathionId());
		writeD(_activeChar.getFame());
		writeD(_activeChar.isMinimapAllowed() ? 0x01 : 0x00);
		writeD(_activeChar.getVitalityPoints());
		writeD(_abnormalEffectMask2);
	}
}