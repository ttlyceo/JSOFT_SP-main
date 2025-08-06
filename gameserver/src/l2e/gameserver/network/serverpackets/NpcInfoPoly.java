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

import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.instancemanager.ZoneManager;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.model.zone.type.FunPvpZone;
import l2e.gameserver.network.ServerPacketOpcodes;

public class NpcInfoPoly extends GameServerPacket
{
	@Override
	protected ServerPacketOpcodes getOpcodes()
	{
		return ServerPacketOpcodes.NpcInfo;
	}
	
	private final Player _activeChar;
	private final String _name, _title;
	private final int _objId;
	private int _x, _y, _z;
	private final int _heading;
	private final double _mAtkSpd, _pAtkSpd;
	private final int _runSpd;
	private final int _walkSpd;
	private final double _moveMultiplier;
	private final float _attackSpeedMultiplier;
	private final boolean _isAttackable;
	private final boolean _isHideClanInfo;
	
	public NpcInfoPoly(Player cha, Player viewer)
	{
		_activeChar = cha;
		_objId = cha.getObjectId();
		_name = getName(_activeChar);
		_title = getTitle(_activeChar);
		_isHideClanInfo = getHideClanInfo(_activeChar);
		if ((_activeChar.getVehicle() != null) && (_activeChar.getInVehiclePosition() != null))
		{
			_x = _activeChar.getInVehiclePosition().getX();
			_y = _activeChar.getInVehiclePosition().getY();
			_z = _activeChar.getInVehiclePosition().getZ();
		}
		else
		{
			_x = _activeChar.getX();
			_y = _activeChar.getY();
			_z = _activeChar.getZ();
		}
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = (int) _activeChar.getPAtkSpd();
		_moveMultiplier = _activeChar.getMovementSpeedMultiplier();
		_attackSpeedMultiplier = _activeChar.getAttackSpeedMultiplier();
		_runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (_activeChar.getWalkSpeed() / _moveMultiplier);
		_isAttackable = _activeChar.isAutoAttackable(viewer, false);
		_invisible = cha.isInvisible();
	}

	private String getName(Player player)
	{
		if (player.isInFightEvent())
		{
			return player.getFightEvent().getVisibleName(player, player.getAppearance().getVisibleName(), false);
		}

		if (player.isInPvpFunZone())
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
			if (zone != null)
			{
				return zone.getVisibleName(player, player.getAppearance().getVisibleName(), false);
			}
		}

		return player.getAppearance().getVisibleName();
	}

	private String getTitle(Player player)
	{
		if (player.isInFightEvent())
		{
			return player.getFightEvent().getVisibleTitle(player, player, player.getAppearance().getVisibleTitle(), false);
		}

		if (player.isInPvpFunZone())
		{
			final FunPvpZone zone = ZoneManager.getInstance().getZone(player, FunPvpZone.class);
			if (zone != null)
			{
				return zone.getVisibleTitle(player, player, player.getAppearance().getVisibleTitle(), false);
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

		final NpcTemplate template = NpcsParser.getInstance().getTemplate(_activeChar.getPolyId());
		if (template != null)
		{
			writeD(_objId);
			writeD(template.getId() + 1000000);
			writeD(_isAttackable ? 0x01 : 0x00);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(0x00);
			writeD((int) _mAtkSpd);
			writeD((int) _pAtkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeF(_moveMultiplier);
			writeF(_attackSpeedMultiplier);
			writeF(template.getfCollisionRadius());
			writeF(template.getfCollisionHeight());
			writeD(template.getRightHand());
			writeD(0x00);
			writeD(template.getLeftHand());
			writeC(1);
			writeC(_activeChar.isRunning() ? 0x01 : 0x00);
			writeC(_activeChar.isInCombat() ? 0x01 : 0x00);
			writeC(_activeChar.isAlikeDead() ? 0x01 : 0x00);
			writeC(!gmSeeInvis && _invisible ? 0x01 : 0x00);
			writeD(-1);
			writeS(_name);
			writeD(-1);
			writeS(gmSeeInvis ? "Invisible" : _title);
			writeD(_activeChar.getAppearance().getTitleColor());
			writeD(_activeChar.getPvpFlag());
			writeD(_activeChar.getKarma());
			writeD(gmSeeInvis ? (_activeChar.getAbnormalEffectMask() | AbnormalEffect.STEALTH.getMask()) : _activeChar.getAbnormalEffectMask());
			writeD(_isHideClanInfo ? 0x00 : _activeChar.getClanId());
			writeD(_isHideClanInfo ? 0x00 : _activeChar.getClanCrestId());
			writeD(_isHideClanInfo ? 0x00 : _activeChar.getAllyId());
			writeD(_isHideClanInfo ? 0x00 : _activeChar.getAllyCrestId());
			writeC(_activeChar.isFlying() ? 0x02 : 0x00);
			writeC(_activeChar.getTeam());
			writeF(template.getfCollisionRadius());
			writeF(template.getfCollisionHeight());
			writeD(_activeChar.getEnchantEffect());
			writeD(_activeChar.isFlying() ? 0x02 : 0x00);
			writeD(0x00);
			writeD(0x00);
			writeC(!template.isTargetable() ? 0x01 : 0x00);
			writeC(!template.isShowName() ? 0x01 : 0x00);
			writeC(_activeChar.getAbnormalEffectMask2());
			writeD(0x00);
		}
	}
}