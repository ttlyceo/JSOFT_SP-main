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
import l2e.gameserver.instancemanager.TerritoryWarManager.Territory;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Attackable;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.instance.MonsterInstance;
import l2e.gameserver.model.actor.instance.TrapInstance;
import l2e.gameserver.model.entity.events.AbstractFightEvent;
import l2e.gameserver.model.skills.effects.AbnormalEffect;
import l2e.gameserver.network.ServerPacketOpcodes;
import l2e.gameserver.network.serverpackets.updatetype.NpcInfoType;

public abstract class NpcInfo extends AbstractMaskPacket<NpcInfoType>
{
	@Override
	protected ServerPacketOpcodes getOpcodes()
	{
		return ServerPacketOpcodes.NpcInfo;
	}
	
	protected int _x, _y, _z, _heading;
	protected int _idTemplate;
	protected boolean _isAttackable, _isSummoned;
	protected double _mAtkSpd, _pAtkSpd;
	protected int _runSpd;
	protected int _walkSpd;
	protected final int _swimRunSpd, _swimWalkSpd;
	protected final int _flyRunSpd, _flyWalkSpd;
	protected double _moveMultiplier;
	protected int _rhand, _lhand, _chest, _enchantEffect;
	protected double _collisionHeight, _collisionRadius;
	protected String _name = "";
	protected String _title = "";

	public NpcInfo(Creature cha)
	{
		_isSummoned = cha.isShowSummonAnimation();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_heading = cha.getHeading();
		_mAtkSpd = cha.getMAtkSpd();
		_pAtkSpd = cha.getPAtkSpd();
		_moveMultiplier = cha.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(cha.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(cha.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(cha.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(cha.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = cha.isFlying() ? _runSpd : 0;
		_flyWalkSpd = cha.isFlying() ? _walkSpd : 0;
	}
	
	public static class Info extends NpcInfo
	{
		private final Npc _npc;
		private int _clanCrest = 0;
		private int _allyCrest = 0;
		private int _allyId = 0;
		private int _clanId = 0;
		private int _displayEffect = 0;
		
		public Info(Npc cha, Creature attacker)
		{
			super(cha);
			_npc = cha;
			_idTemplate = cha.getTemplate().getIdTemplate();
			_rhand = cha.getRightHandItem();
			_lhand = cha.getLeftHandItem();
			_enchantEffect = (cha.getChampionTemplate() != null ? cha.getChampionTemplate().weaponEnchant : cha.getEnchantEffect());
			_collisionHeight = cha.getColHeight();
			_collisionRadius = cha.getColRadius();
			_isAttackable = cha.isAutoAttackable(attacker, false);
			
			if ((Config.SHOW_NPC_SERVER_NAME || cha.getTemplate().isCustom()) && attacker != null && attacker.isPlayer())
			{
				_name = cha.getTemplate().getName(attacker.getActingPlayer().getLang());
			}
			
			if (_npc.isInvisible())
			{
				_title = "Invisible";
			}
			else if (cha.getChampionTemplate() != null)
			{
				_title = cha.getChampionTemplate().title;
			}
			else
			{
				final var isInFightEvent = cha.getEvent(AbstractFightEvent.class) != null;
				if ((Config.SHOW_NPC_SERVER_TITLE || cha.getTemplate().isCustom() || isInFightEvent) && attacker != null && attacker.isPlayer())
				{
					_title = isInFightEvent ? cha.getTitle(attacker.getActingPlayer().getLang()) : cha.getTemplate().getTitle(attacker.getActingPlayer().getLang());
				}
			}
			
			if (Config.SHOW_NPC_LVL && (_npc instanceof MonsterInstance) && ((Attackable) _npc).canShowLevelInTitle())
			{
				String t = "Lv " + cha.getLevel() + (cha.getAggroRange() > 0 ? "*" : "");
				if (_title != null)
				{
					t += " " + _title;
				}
				_title = t;
			}
			
			final Territory territory = cha.getTerritory();
			if (territory != null)
			{
				if (Config.SHOW_CREST_WITHOUT_QUEST || (territory.getLordObjectId() != 0 && territory.getOwnerClan() != null))
				{
					final Clan clan = territory.getOwnerClan();
					if (clan != null)
					{
						if (Config.SHOW_CREST_WITHOUT_QUEST || (clan.getLeaderId() == territory.getLordObjectId()))
						{
							_clanCrest = clan.getCrestId();
							_clanId = clan.getId();
							_allyCrest = clan.getAllyCrestId();
							_allyId = clan.getAllyId();
						}
					}
				}
			}
			_displayEffect = cha.getDisplayEffect();
		}
		
		@Override
		protected void writeImpl()
		{
			writeD(_npc.getObjectId());
			writeD(_idTemplate + 1000000);
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
			writeD(_swimRunSpd);
			writeD(_swimWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeF(_moveMultiplier);
			writeF(_npc.getAttackSpeedMultiplier());
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_rhand);
			writeD(_chest);
			writeD(_lhand);
			writeC(0x01);
			writeC(_npc.isRunning() ? 0x01 : 0x00);
			writeC(_npc.isInCombat() ? 0x01 : 0x00);
			writeC(_npc.isAlikeDead() ? 0x01 : 0x00);
			writeC(_isSummoned ? 0x02 : 0x00);
			writeD(-1);
			writeS(_name);
			writeD(-1);
			writeS(_title);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(_npc.isInvisible() ? _npc.getAbnormalEffectMask() | AbnormalEffect.STEALTH.getMask() : _npc.getAbnormalEffectMask());
			writeD(_clanId);
			writeD(_clanCrest);
			writeD(_allyId);
			writeD(_allyCrest);
			writeC(_npc.isInWater(_npc) ? 0x01 : _npc.isFlying() ? 0x02 : 0x00);
			if (_npc.getChampionTemplate() != null)
			{
				if (_npc.getChampionTemplate().blueCircle)
				{
					writeC(0x01);
				}
				else if (_npc.getChampionTemplate().redCircle)
				{
					writeC(0x02);
				}
				else
				{
					writeC(0x00);
				}
			}
			else
			{
				writeC(_npc.getTeam());
			}
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_enchantEffect);
			writeD(_npc.isFlying() ? 0x01 : 0x00);
			writeD(0x00);
			writeD(_npc.getColorEffect());
			writeC(_npc.isTargetable() ? 0x01 : 0x00);
			writeC(_npc.isShowName() ? 0x01 : 0x00);
			writeD(_npc.getAbnormalEffectMask2());
			writeD(_displayEffect);
		}
		
		private final byte[] _masks = new byte[]
		{
		        (byte) 0x00, (byte) 0x0C, (byte) 0x0C, (byte) 0x00, (byte) 0x00
		};
		
		@Override
		protected byte[] getMasks()
		{
			return _masks;
		}
		
		@Override
		protected void onNewMaskAdded(NpcInfoType component)
		{
		}
	}
	
	public static class TrapInfo extends NpcInfo
	{
		private final TrapInstance _trap;
		
		public TrapInfo(TrapInstance cha, Creature attacker)
		{
			super(cha);
			
			_trap = cha;
			_idTemplate = cha.getTemplate().getIdTemplate();
			_isAttackable = cha.isAutoAttackable(attacker, false);
			_rhand = 0;
			_lhand = 0;
			_collisionHeight = _trap.getTemplate().getfCollisionHeight();
			_collisionRadius = _trap.getTemplate().getfCollisionRadius();
			_name = cha.getName(null);
			_title = cha.getOwner() != null ? cha.getOwner().getName(null) : "";
		}
		
		@Override
		protected void writeImpl()
		{
			writeD(_trap.getObjectId());
			writeD(_idTemplate + 1000000);
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
			writeF(_trap.getAttackSpeedMultiplier());
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_rhand);
			writeD(_chest);
			writeD(_lhand);
			writeC(0x01);
			writeC(0x01);
			writeC(_trap.isInCombat() ? 0x01 : 0x00);
			writeC(_trap.isAlikeDead() ? 0x01 : 0x00);
			writeC(_isSummoned ? 0x02 : 0x00);
			writeD(-1);
			writeS(_name);
			writeD(-1);
			writeS(_title);
			writeD(0x00);
			writeD(_trap.getPvpFlag());
			writeD(_trap.getKarma());
			writeD(_trap.isInvisible() ? _trap.getAbnormalEffectMask() | AbnormalEffect.STEALTH.getMask() : _trap.getAbnormalEffectMask());
			writeD(0x00);
			writeD(0x00);
			writeD(0000);
			writeD(0000);
			writeC(0000);
			writeC(_trap.getTeam());
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeC(0x01);
			writeC(0x01);
			writeD(0x00);
		}
		
		private final byte[] _masks = new byte[]
		{
		        (byte) 0x00, (byte) 0x0C, (byte) 0x0C, (byte) 0x00, (byte) 0x00
		};
		
		@Override
		protected byte[] getMasks()
		{
			return _masks;
		}
		
		@Override
		protected void onNewMaskAdded(NpcInfoType component)
		{
		}
	}
	
	public static class SummonInfo extends NpcInfo
	{
		private final Summon _summon;
		private int _form = 0;
		private int _val = 0;
		
		public SummonInfo(Summon cha, Creature attacker, int val)
		{
			super(cha);
			_summon = cha;
			_val = val;
			if (_summon.isShowSummonAnimation())
			{
				_val = 2;
			}
			
			final int npcId = cha.getTemplate().getId();
			
			if ((npcId == 16041) || (npcId == 16042))
			{
				if (cha.getLevel() > 69)
				{
					_form = 3;
				}
				else if (cha.getLevel() > 64)
				{
					_form = 2;
				}
				else if (cha.getLevel() > 59)
				{
					_form = 1;
				}
			}
			else if ((npcId == 16025) || (npcId == 16037))
			{
				if (cha.getLevel() > 69)
				{
					_form = 3;
				}
				else if (cha.getLevel() > 64)
				{
					_form = 2;
				}
				else if (cha.getLevel() > 59)
				{
					_form = 1;
				}
			}
			_isAttackable = cha.isAutoAttackable(attacker, false);
			_rhand = cha.getWeapon();
			_lhand = 0;
			_chest = cha.getArmor();
			_enchantEffect = cha.getTemplate().getEnchantEffect();
			_name = cha.getName(null);
			_title = cha.getOwner() != null ? ((!cha.getOwner().isOnline()) ? "" : cha.getOwner().getName(null)) : "";
			_idTemplate = cha.getTemplate().getIdTemplate();
			_collisionHeight = cha.getTemplate().getfCollisionHeight();
			_collisionRadius = cha.getTemplate().getfCollisionRadius();
			_invisible = cha.isInvisible();
		}
		
		@Override
		protected void writeImpl()
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
			writeD(_summon.getObjectId());
			writeD(_idTemplate + 1000000);
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
			writeD(_swimRunSpd);
			writeD(_swimWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeF(_moveMultiplier);
			writeF(_summon.getAttackSpeedMultiplier());
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_rhand);
			writeD(_chest);
			writeD(_lhand);
			writeC(0x01);
			writeC(0x01);
			writeC(_summon.isInCombat() ? 0x01 : 0x00);
			writeC(_summon.isAlikeDead() ? 0x01 : 0x00);
			writeC(_val);
			writeD(-1);
			writeS(_name);
			writeD(-1);
			writeS(_title);
			writeD(0x01);
			writeD(_summon.getPvpFlag());
			writeD(_summon.getKarma());
			writeD(gmSeeInvis ? _summon.getAbnormalEffectMask() | AbnormalEffect.STEALTH.getMask() : _summon.getAbnormalEffectMask());
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeC(0x00);
			writeC(_summon.getTeam());
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_enchantEffect);
			writeD(0x00);
			writeD(0x00);
			writeD(_form);
			writeC(0x01);
			writeC(0x01);
			writeD(_summon.getAbnormalEffectMask2());
		}
		
		private final byte[] _masks = new byte[]
		{
		        (byte) 0x00, (byte) 0x0C, (byte) 0x0C, (byte) 0x00, (byte) 0x00
		};
		
		@Override
		protected byte[] getMasks()
		{
			return _masks;
		}
		
		@Override
		protected void onNewMaskAdded(NpcInfoType component)
		{
		}
	}
}